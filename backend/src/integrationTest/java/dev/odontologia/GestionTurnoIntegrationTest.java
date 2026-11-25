package dev.odontologia;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import dev.odontologia.agenda.AgendaQueries;
import dev.odontologia.agenda.AgendaWriteCoordinator;
import dev.odontologia.comunicaciones.PreparacionAvisos;
import dev.odontologia.consultorio.ConsultorioQueries;
import dev.odontologia.disponibilidad.AvailabilityConfigurationService;
import dev.odontologia.disponibilidad.AvailabilityDtos.FranjaInput;
import dev.odontologia.disponibilidad.GestionTurnoService;
import dev.odontologia.gestion.CredencialesGestion;
import dev.odontologia.gestion.CredencialesGestion.TurnoAutorizado;
import dev.odontologia.pacientes.PacienteQueries;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.support.JdbcTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GestionTurnoIntegrationTest {

    private static final UUID TURNO_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID CREDENCIAL_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final Instant AHORA = Instant.parse("2030-01-07T12:00:00Z");
    private static final Instant INICIO_ACTUAL = Instant.parse("2030-01-09T12:00:00Z");
    private static final Instant NUEVO_INICIO = Instant.parse("2030-01-10T12:00:00Z");
    private static final byte[] TOKEN_BYTES = new byte[32];
    private static final String TOKEN = Base64.getUrlEncoder().withoutPadding().encodeToString(TOKEN_BYTES);
    private static final byte[] VERIFICADOR = sha256(TOKEN_BYTES);

    @Test
    void cancellationAtomicallyRevokesCredentialAndPreparesPendingNotice() {
        try (var db = new TestDatabase()) {
            var harness = setup(db, true);
            String etag = harness.service().consultar(TOKEN).etag();

            var result = harness.service().cancelar(TOKEN, etag);

            assertThat(result.estado()).isEqualTo("CANCELADO");
            assertThat(result.notificacionEstado()).isEqualTo("PENDIENTE");
            assertThat(db.jdbc.queryForObject("SELECT estado FROM turno WHERE id = ?", String.class, TURNO_ID))
                    .isEqualTo("CANCELADO");
            assertThat(db.jdbc.queryForObject("SELECT revision FROM turno WHERE id = ?", Integer.class, TURNO_ID))
                    .isEqualTo(1);
            assertThat(db.jdbc.queryForObject("SELECT revocada_en IS NOT NULL FROM credencial_gestion WHERE id = ?",
                    Boolean.class, CREDENCIAL_ID)).isTrue();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion WHERE turno_id = ? AND tipo = 'CAMBIO' AND revision = 1 AND elegible",
                    Long.class, TURNO_ID)).isEqualTo(1);
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion WHERE turno_id = ? AND revision = 0 AND elegible",
                    Long.class, TURNO_ID)).isZero();
        }
    }

    @Test
    void cancellationRollsBackTurnCredentialAndNoticeChangesTogetherWhenNoticePreparationFails() {
        try (var db = new TestDatabase()) {
            var harness = setup(db, true);
            db.jdbc.execute("ALTER TABLE origen_notificacion ADD CONSTRAINT reject_cambio_for_test CHECK (clase <> 'CAMBIO')");
            String etag = harness.service().consultar(TOKEN).etag();

            assertThatThrownBy(() -> harness.service().cancelar(TOKEN, etag))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("TECHNICAL_FAILURE");

            assertThat(db.jdbc.queryForObject("SELECT estado FROM turno WHERE id = ?", String.class, TURNO_ID))
                    .isEqualTo("CONFIRMADO");
            assertThat(db.jdbc.queryForObject("SELECT revision FROM turno WHERE id = ?", Integer.class, TURNO_ID))
                    .isZero();
            assertThat(db.jdbc.queryForObject("SELECT revocada_en IS NULL FROM credencial_gestion WHERE id = ?",
                    Boolean.class, CREDENCIAL_ID)).isTrue();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion WHERE turno_id = ?",
                    Long.class, TURNO_ID)).isEqualTo(2);
            assertThat(db.jdbc.queryForObject("SELECT bool_and(elegible) FROM intencion_notificacion WHERE turno_id = ?",
                    Boolean.class, TURNO_ID)).isTrue();
        }
    }

    @Test
    void reprogrammingKeepsTurnIdentityAndCredentialAndReconcilesNotices() {
        try (var db = new TestDatabase()) {
            var harness = setup(db, true);
            String etag = harness.service().consultar(TOKEN).etag();

            var result = harness.service().reprogramar(TOKEN, etag,
                    new dev.odontologia.disponibilidad.GestionTurnoDtos.ReprogramacionInput(NUEVO_INICIO));

            assertThat(result.cuerpo().turnoId()).isEqualTo(TURNO_ID);
            assertThat(result.cuerpo().fecha().toString()).isEqualTo("2030-01-10");
            assertThat(result.cuerpo().horaInicio()).isEqualTo(LocalTime.of(12, 0));
            assertThat(result.cuerpo().horaFin()).isEqualTo(LocalTime.of(12, 30));
            assertThat(result.cuerpo().estado()).isEqualTo("CONFIRMADO");
            assertThat(result.cuerpo().notificacionEstado()).isEqualTo("PENDIENTE");
            assertThat(result.etag()).isNotEqualTo(etag);
            assertThat(db.jdbc.queryForObject("SELECT id FROM turno WHERE id = ?", UUID.class, TURNO_ID))
                    .isEqualTo(TURNO_ID);
            assertThat(db.jdbc.queryForObject("SELECT inicio FROM turno WHERE id = ?", OffsetDateTime.class, TURNO_ID)
                    .toInstant()).isEqualTo(NUEVO_INICIO);
            assertThat(db.jdbc.queryForObject("SELECT duracion_reservada_minutos FROM turno WHERE id = ?",
                    Integer.class, TURNO_ID)).isEqualTo(30);
            assertThat(db.jdbc.queryForObject("SELECT revocada_en IS NULL FROM credencial_gestion WHERE id = ?",
                    Boolean.class, CREDENCIAL_ID)).isTrue();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion WHERE turno_id = ? AND revision = 0 AND elegible",
                    Long.class, TURNO_ID)).isZero();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion WHERE turno_id = ? AND revision = 1 AND tipo = 'CAMBIO' AND elegible AND credencial_id = ?",
                    Long.class, TURNO_ID, CREDENCIAL_ID)).isEqualTo(1);
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion WHERE turno_id = ? AND revision = 1 AND tipo = 'RECORDATORIO' AND elegible AND credencial_id = ?",
                    Long.class, TURNO_ID, CREDENCIAL_ID)).isEqualTo(1);
        }
    }

    @Test
    void synchronizedRequestsWithTheSameEtagCommitAtMostOneReprogrammingAndOneSetOfNotices() throws Exception {
        try (var db = new TestDatabase(); var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            var harness = setup(db, true);
            String etag = harness.service().consultar(TOKEN).etag();
            var ready = new CountDownLatch(2);
            var start = new CountDownLatch(1);
            var first = threads.submit(() -> reprogramAtBarrier(harness.service(), etag, ready, start));
            var second = threads.submit(() -> reprogramAtBarrier(harness.service(), etag, ready, start));
            assertThat(ready.await(4, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(first.get(8, TimeUnit.SECONDS), second.get(8, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("CONFIRMED", "TURN_VERSION_MISMATCH");
            assertThat(db.jdbc.queryForObject("SELECT revision FROM turno WHERE id = ?", Integer.class, TURNO_ID))
                    .isEqualTo(1);
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion WHERE turno_id = ? AND revision = 1 AND tipo = 'CAMBIO'",
                    Long.class, TURNO_ID)).isOne();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion WHERE turno_id = ? AND revision = 1 AND tipo = 'RECORDATORIO'",
                    Long.class, TURNO_ID)).isOne();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM origen_notificacion WHERE turno_id = ? AND revision = 1",
                    Long.class, TURNO_ID)).isOne();
        }
    }

    private static String reprogramAtBarrier(GestionTurnoService service, String etag,
                                             CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        assertThat(start.await(4, TimeUnit.SECONDS)).isTrue();
        try {
            service.reprogramar(TOKEN, etag,
                    new dev.odontologia.disponibilidad.GestionTurnoDtos.ReprogramacionInput(NUEVO_INICIO));
            return "CONFIRMED";
        } catch (RuntimeException failure) {
            return failure.getMessage();
        }
    }

    private static Harness setup(TestDatabase db, boolean initialNotices) {
        var fixture = CoreSchemaTest.fixture(db);
        Instant finalInicio = INICIO_ACTUAL.plusSeconds(30 * 60);
        CoreSchemaTest.insertTurn(db.jdbc, fixture, TURNO_ID, INICIO_ACTUAL.atOffset(ZoneOffset.UTC),
                finalInicio.atOffset(ZoneOffset.UTC), "CONFIRMADO");
        db.jdbc.update("""
                INSERT INTO credencial_gestion (id, turno_id, verificador, material_cifrado, nonce,
                    version_clave, emitida_en) VALUES (?, ?, ?, ?, ?, 'test', ?)
                """, CREDENCIAL_ID, TURNO_ID, VERIFICADOR, new byte[48], new byte[12],
                AHORA.atOffset(ZoneOffset.UTC));

        var coordinator = new AgendaWriteCoordinator(db.jdbc, new JdbcTransactionManager(db.dataSource()));
        var agendaQueries = new AgendaQueries(db.jdbc);
        var consultorioQueries = new ConsultorioQueries(db.jdbc);
        var pacienteQueries = new PacienteQueries(db.jdbc);
        var avisos = new PreparacionAvisos(db.jdbc);
        var credenciales = mock(CredencialesGestion.class);
        when(credenciales.validar(TOKEN)).thenAnswer(invocation -> db.jdbc.query("""
                SELECT t.id, t.inicio, t.fin, t.revision, c.id AS referencia_credencial
                FROM credencial_gestion c JOIN turno t ON t.id = c.turno_id
                WHERE c.verificador = ? AND c.revocada_en IS NULL AND t.estado = 'CONFIRMADO'
                """, (rs, row) -> new TurnoAutorizado(rs.getObject("id", UUID.class),
                rs.getObject("inicio", OffsetDateTime.class).toInstant(),
                rs.getObject("fin", OffsetDateTime.class).toInstant(), rs.getInt("revision"),
                rs.getObject("referencia_credencial", UUID.class)), VERIFICADOR).stream().findFirst());
        doAnswer(invocation -> {
            db.jdbc.update("UPDATE credencial_gestion SET revocada_en = ? WHERE turno_id = ? AND revocada_en IS NULL",
                    AHORA.atOffset(ZoneOffset.UTC), invocation.getArgument(0));
            return null;
        }).when(credenciales).revocar(TURNO_ID);

        if (initialNotices) {
            coordinator.write(fixture.agenda(), () -> {
                avisos.reserva(new PreparacionAvisos.Evento(UUID.randomUUID(), TURNO_ID, 0,
                        "f@example.invalid", AHORA), INICIO_ACTUAL, CREDENCIAL_ID);
                return null;
            });
        }

        var availability = new AvailabilityConfigurationService(db.jdbc, agendaQueries, consultorioQueries,
                coordinator);
        availability.replaceHorario(List.of(
                new FranjaInput(UUID.randomUUID(), 3, LocalTime.of(9, 0), LocalTime.of(17, 0), "ATENCION"),
                new FranjaInput(UUID.randomUUID(), 4, LocalTime.of(9, 0), LocalTime.of(17, 0), "ATENCION")));
        var service = new GestionTurnoService(db.jdbc, agendaQueries, consultorioQueries, pacienteQueries,
                coordinator, credenciales, avisos, Clock.fixed(AHORA, ZoneOffset.UTC));
        return new Harness(fixture, service);
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (Exception unavailable) {
            throw new ExceptionInInitializerError(unavailable);
        }
    }

    private record Harness(CoreSchemaTest.Fixture fixture, GestionTurnoService service) {
    }
}
