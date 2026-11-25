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
    private static final UUID CREDENCIAL_RECUPERADA_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID INTENTO_EXISTENTE_ID = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final Instant AHORA = Instant.parse("2030-01-07T12:00:00Z");
    private static final Instant INICIO_ACTUAL = Instant.parse("2030-01-09T12:00:00Z");
    private static final Instant NUEVO_INICIO = Instant.parse("2030-01-10T12:00:00Z");
    private static final byte[] TOKEN_BYTES = new byte[32];
    private static final String TOKEN = Base64.getUrlEncoder().withoutPadding().encodeToString(TOKEN_BYTES);
    private static final byte[] VERIFICADOR = sha256(TOKEN_BYTES);
    private static final byte[] TOKEN_RECUPERADO_BYTES = recoveredTokenBytes();
    private static final String TOKEN_RECUPERADO = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(TOKEN_RECUPERADO_BYTES);
    private static final byte[] VERIFICADOR_RECUPERADO = sha256(TOKEN_RECUPERADO_BYTES);
    private static final String EMAIL_RECUPERADO = "odontologo.nuevo@example.invalid";

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
    void sameIntervalReprogrammingRollsBackRevisionCredentialAndNoticeState() {
        try (var db = new TestDatabase()) {
            var harness = setup(db, true);
            String etag = harness.service().consultar(TOKEN).etag();

            assertThatThrownBy(() -> harness.service().reprogramar(TOKEN, etag,
                    new dev.odontologia.disponibilidad.GestionTurnoDtos.ReprogramacionInput(INICIO_ACTUAL)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("SAME_INTERVAL");

            assertThat(db.jdbc.queryForObject("SELECT revision FROM turno WHERE id = ?", Integer.class, TURNO_ID))
                    .isZero();
            assertThat(db.jdbc.queryForObject("SELECT inicio FROM turno WHERE id = ?", OffsetDateTime.class, TURNO_ID)
                    .toInstant()).isEqualTo(INICIO_ACTUAL);
            assertThat(db.jdbc.queryForObject("SELECT revocada_en IS NULL FROM credencial_gestion WHERE id = ?",
                    Boolean.class, CREDENCIAL_ID)).isTrue();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion WHERE turno_id = ?",
                    Long.class, TURNO_ID)).isEqualTo(2);
            assertThat(db.jdbc.queryForObject("SELECT bool_and(elegible) FROM intencion_notificacion WHERE turno_id = ?",
                    Boolean.class, TURNO_ID)).isTrue();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM origen_notificacion WHERE turno_id = ?",
                    Long.class, TURNO_ID)).isEqualTo(1);
        }
    }

    @Test
    void cancellationAfterRecoveryUsesConfirmedRecipientAndPreservesPatientAndSentHistory() {
        try (var db = new TestDatabase()) {
            var harness = setupRecovered(db);
            String etag = harness.service().consultar(TOKEN_RECUPERADO).etag();

            harness.service().cancelar(TOKEN_RECUPERADO, etag);

            assertThat(db.jdbc.queryForObject("SELECT email FROM paciente WHERE id = ?", String.class,
                    harness.fixture().paciente())).isEqualTo("f@example.invalid");
            assertThat(db.jdbc.queryForObject("""
                    SELECT count(*) FROM intencion_notificacion
                    WHERE turno_id = ? AND revision = 1 AND tipo = 'CAMBIO' AND destinatario = ?
                    """, Long.class, TURNO_ID, EMAIL_RECUPERADO)).isOne();
            assertThat(db.jdbc.queryForObject("""
                    SELECT count(*) FROM intencion_notificacion
                    WHERE turno_id = ? AND revision = 1 AND destinatario = ?
                    """, Long.class, TURNO_ID, "f@example.invalid")).isZero();
            assertExistingSendHistory(db);
        }
    }

    @Test
    void reprogrammingAfterRecoveryUsesConfirmedRecipientAndCurrentCredential() {
        try (var db = new TestDatabase()) {
            var harness = setupRecovered(db);
            String etag = harness.service().consultar(TOKEN_RECUPERADO).etag();

            harness.service().reprogramar(TOKEN_RECUPERADO, etag,
                    new dev.odontologia.disponibilidad.GestionTurnoDtos.ReprogramacionInput(NUEVO_INICIO));

            assertThat(db.jdbc.queryForObject("SELECT email FROM paciente WHERE id = ?", String.class,
                    harness.fixture().paciente())).isEqualTo("f@example.invalid");
            assertThat(db.jdbc.queryForObject("""
                    SELECT count(*) FROM intencion_notificacion
                    WHERE turno_id = ? AND revision = 1 AND tipo IN ('CAMBIO', 'RECORDATORIO')
                        AND destinatario = ? AND credencial_id = ?
                    """, Long.class, TURNO_ID, EMAIL_RECUPERADO, CREDENCIAL_RECUPERADA_ID)).isEqualTo(2);
            assertThat(db.jdbc.queryForObject("""
                    SELECT count(*) FROM intencion_notificacion
                    WHERE turno_id = ? AND revision = 1 AND destinatario = ?
                    """, Long.class, TURNO_ID, "f@example.invalid")).isZero();
            assertExistingSendHistory(db);
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
                SELECT t.id, t.inicio, t.fin, t.revision, c.id AS referencia_credencial,
                    (SELECT r.destinatario FROM recuperacion_enlace r
                        WHERE r.turno_id = t.id ORDER BY r.orden DESC LIMIT 1) AS destinatario_recuperacion
                FROM credencial_gestion c JOIN turno t ON t.id = c.turno_id
                WHERE c.verificador = ? AND c.revocada_en IS NULL AND t.estado = 'CONFIRMADO'
                """, (rs, row) -> new TurnoAutorizado(rs.getObject("id", UUID.class),
                rs.getObject("inicio", OffsetDateTime.class).toInstant(),
                rs.getObject("fin", OffsetDateTime.class).toInstant(), rs.getInt("revision"),
                rs.getObject("referencia_credencial", UUID.class),
                Optional.ofNullable(rs.getString("destinatario_recuperacion"))), VERIFICADOR).stream().findFirst());
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
        return new Harness(fixture, service, credenciales);
    }

    private static Harness setupRecovered(TestDatabase db) {
        var harness = setup(db, true);
        db.jdbc.update("UPDATE credencial_gestion SET revocada_en = ? WHERE id = ?",
                AHORA.atOffset(ZoneOffset.UTC), CREDENCIAL_ID);
        db.jdbc.update("""
                INSERT INTO credencial_gestion (id, turno_id, verificador, material_cifrado, nonce,
                    version_clave, emitida_en) VALUES (?, ?, ?, ?, ?, 'test', ?)
                """, CREDENCIAL_RECUPERADA_ID, TURNO_ID, VERIFICADOR_RECUPERADO,
                new byte[48], new byte[12], AHORA.atOffset(ZoneOffset.UTC));
        registrarRecuperacion(db, "odontologo.anterior@example.invalid", AHORA.minusSeconds(60));
        registrarRecuperacion(db, EMAIL_RECUPERADO, AHORA.minusSeconds(30));
        db.jdbc.update("""
                UPDATE intencion_notificacion SET destinatario = ?, credencial_id = ?
                WHERE turno_id = ? AND revision = 0 AND tipo = 'RECORDATORIO' AND elegible
                """, EMAIL_RECUPERADO, CREDENCIAL_RECUPERADA_ID, TURNO_ID);
        db.jdbc.update("""
                INSERT INTO intento_notificacion
                    (id, despacho_id, turno_id, revision, destinatario, credencial_id,
                     autorizado_en, inicio_turno, fin_turno, estado_turno, tipos, recuperacion,
                     resultado, terminado_en)
                VALUES (?, ?, ?, 0, 'f@example.invalid', ?, ?, ?, ?, 'CONFIRMADO',
                    ARRAY['CONFIRMACION'], false, 'ACEPTADA_SMTP', ?)
                """, INTENTO_EXISTENTE_ID, UUID.randomUUID(), TURNO_ID, CREDENCIAL_ID,
                AHORA.atOffset(ZoneOffset.UTC), INICIO_ACTUAL.atOffset(ZoneOffset.UTC),
                INICIO_ACTUAL.plusSeconds(30 * 60).atOffset(ZoneOffset.UTC), AHORA.plusSeconds(1).atOffset(ZoneOffset.UTC));
        when(harness.credenciales().validar(TOKEN_RECUPERADO)).thenAnswer(invocation -> db.jdbc.query("""
                SELECT t.id, t.inicio, t.fin, t.revision, c.id AS referencia_credencial,
                    (SELECT r.destinatario FROM recuperacion_enlace r
                        WHERE r.turno_id = t.id ORDER BY r.orden DESC LIMIT 1) AS destinatario_recuperacion
                FROM credencial_gestion c JOIN turno t ON t.id = c.turno_id
                WHERE c.verificador = ? AND c.revocada_en IS NULL AND t.estado = 'CONFIRMADO'
                """, (rs, row) -> new TurnoAutorizado(rs.getObject("id", UUID.class),
                rs.getObject("inicio", OffsetDateTime.class).toInstant(),
                rs.getObject("fin", OffsetDateTime.class).toInstant(), rs.getInt("revision"),
                rs.getObject("referencia_credencial", UUID.class),
                Optional.ofNullable(rs.getString("destinatario_recuperacion"))), VERIFICADOR_RECUPERADO).stream().findFirst());
        return harness;
    }

    private static void registrarRecuperacion(TestDatabase db, String email, Instant fecha) {
        db.jdbc.update("""
                INSERT INTO recuperacion_enlace (id, turno_id, odontologo, metodo, motivo, destinatario, realizada_en)
                VALUES (?, ?, 'Profesional de prueba', 'DOCUMENTO_PRESENCIAL', 'Recuperación de prueba', ?, ?)
                """, UUID.randomUUID(), TURNO_ID, email, fecha.atOffset(ZoneOffset.UTC));
    }

    private static void assertExistingSendHistory(TestDatabase db) {
        assertThat(db.jdbc.queryForObject("""
                SELECT count(*) FROM intento_notificacion
                WHERE id = ? AND turno_id = ? AND revision = 0 AND destinatario = 'f@example.invalid'
                    AND resultado = 'ACEPTADA_SMTP' AND credencial_id = ?
                """, Long.class, INTENTO_EXISTENTE_ID, TURNO_ID, CREDENCIAL_ID)).isOne();
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (Exception unavailable) {
            throw new ExceptionInInitializerError(unavailable);
        }
    }

    private static byte[] recoveredTokenBytes() {
        byte[] bytes = new byte[32];
        bytes[0] = 1;
        return bytes;
    }

    private record Harness(CoreSchemaTest.Fixture fixture, GestionTurnoService service,
            CredencialesGestion credenciales) {
    }
}
