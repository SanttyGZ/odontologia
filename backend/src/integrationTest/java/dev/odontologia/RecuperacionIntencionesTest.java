package dev.odontologia;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import dev.odontologia.agenda.AgendaQueries;
import dev.odontologia.agenda.AgendaWriteCoordinator;
import dev.odontologia.comunicaciones.AutorizacionDespacho;
import dev.odontologia.comunicaciones.PreparacionAvisos;
import dev.odontologia.comunicaciones.SondeoAvisosFixture;
import dev.odontologia.consultorio.ConsultorioQueries;
import dev.odontologia.disponibilidad.AvailabilityDtos.FranjaInput;
import dev.odontologia.disponibilidad.AvailabilityConfigurationService;
import dev.odontologia.disponibilidad.PublicBookingDtos.ReservaInput;
import dev.odontologia.disponibilidad.PublicBookingService;
import dev.odontologia.gestion.AvisosRecuperacion;
import dev.odontologia.gestion.CredencialesGestion;
import dev.odontologia.pacientes.PacienteQueries;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

import static dev.odontologia.CoreSchemaTest.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecuperacionIntencionesTest {
    @TempDir Path directory;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void elSondeoDistingueComposicionFallidaDeInterrupcionTrasSmtp(boolean interrumpirSmtp) throws Exception {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            Path keys = claves();
            var ahora = Instant.parse("2030-01-07T10:00:00Z");
            var reloj = Clock.fixed(ahora, ZoneOffset.UTC);
            try (var app = iniciar(db, keys)) {
                var coordinador = app.getBean(AgendaWriteCoordinator.class);
                var jdbc = app.getBean(JdbcTemplate.class);
                app.getBean(AvailabilityConfigurationService.class)
                        .replaceHorario(List.of(new FranjaInput(UUID.randomUUID(), 2,
                                LocalTime.of(9, 0), LocalTime.of(12, 0), "ATENCION")));
                var reservas = new PublicBookingService(jdbc, app.getBean(AgendaQueries.class),
                        app.getBean(ConsultorioQueries.class), app.getBean(PacienteQueries.class),
                        coordinador, app.getBean(CredencialesGestion.class), app.getBean(PreparacionAvisos.class), reloj);
                var turno = reservas.reservar(new ReservaInput(fixture.motivo(), Instant.parse("2030-01-08T11:00:00Z"),
                        "Ana", "Prueba", "grupo16desarrollosoftware3k10@gmail.com", "2615551234", "30123456"));
                var fueraDeAlcance = keys.resolveSibling("clave-no-disponible.properties");
                if (!interrumpirSmtp) {
                    Files.move(keys, fueraDeAlcance);
                }
                var llamadas = new AtomicInteger();
                var autorizacion = app.getBean(AutorizacionDespacho.class);
                SondeoAvisosFixture.sondeo(db.jdbc, autorizacion, reloj, () -> {
                    llamadas.incrementAndGet();
                    return false;
                }).run();
                var primero = resultado(db, turno.turnoId());
                if (!interrumpirSmtp) {
                    Files.move(fueraDeAlcance, keys);
                }
                var despues = Clock.offset(reloj, Duration.ofMinutes(6));
                SondeoAvisosFixture.sondeo(db.jdbc, autorizacion, despues, () -> {
                    llamadas.incrementAndGet();
                    return true;
                }).run();
                var segundo = resultado(db, turno.turnoId());
                assertThat(primero.credencial()).isNotNull();
                if (interrumpirSmtp) {
                    assertThat(llamadas).hasValue(1);
                    assertThat(primero.estado()).isNull();
                    assertThat(primero.terminado()).isNull();
                    assertThat(segundo.estado()).isEqualTo("INCIERTO");
                    assertThat(segundo.terminado()).isEqualTo(despues.instant().atOffset(ZoneOffset.UTC));
                } else {
                    assertThat(llamadas).hasValue(0);
                    assertThat(java.util.Arrays.asList(primero.estado(), segundo.estado()))
                            .containsExactly("PERMANENTE", "PERMANENTE");
                    assertThat(primero.terminado()).isEqualTo(ahora.atOffset(ZoneOffset.UTC));
                    assertThat(segundo).isEqualTo(primero);
                }
            }
        }
    }

    private record ResultadoSondeo(String estado, OffsetDateTime terminado, UUID credencial) {}

    private static ResultadoSondeo resultado(TestDatabase db, UUID turno) {
        return db.jdbc.queryForObject("""
                SELECT resultado, terminado_en, credencial_id FROM intento_notificacion WHERE turno_id = ?
                """, (rs, fila) -> new ResultadoSondeo(rs.getString("resultado"),
                rs.getObject("terminado_en", OffsetDateTime.class), rs.getObject("credencial_id", UUID.class)), turno);
    }

    @Test
    void reconstruyeDespuesDeReiniciarYConservaLaValidacionSinClave() throws Exception {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            Path keys = claves();
            var inicio = START.plusYears(10);
            UUID turno = UUID.randomUUID();
            UUID referencia;
            String secreto;
            CoreSchemaTest.insertTurn(db.jdbc, fixture, turno, inicio, inicio.plusMinutes(30), "CONFIRMADO");
            try (var app = iniciar(db, keys)) {
                var credenciales = app.getBean(CredencialesGestion.class);
                referencia = app.getBean(AgendaWriteCoordinator.class).write(fixture.agenda(),
                        () -> credenciales.emitir(turno));
                secreto = credenciales.recuperarParaComposicion(referencia, turno, 0).orElseThrow();
                assertThat(credenciales.recuperarParaComposicion(referencia, UUID.randomUUID(), 0)).isEmpty();
            }
            try (var app = iniciar(db, keys)) {
                var credenciales = app.getBean(CredencialesGestion.class);
                assertThat(credenciales.recuperarParaComposicion(referencia, turno, 0)).contains(secreto);
                Files.setPosixFilePermissions(keys, PosixFilePermissions.fromString("rw-r--r--"));
                assertThatThrownBy(() -> credenciales.recuperarParaComposicion(referencia, turno, 0))
                        .isInstanceOf(IllegalStateException.class).hasMessage("Material de enlace no disponible");
                Files.delete(keys);
                assertThatThrownBy(() -> credenciales.recuperarParaComposicion(referencia, turno, 0))
                        .isInstanceOf(IllegalStateException.class).hasMessage("Material de enlace no disponible");
                assertThat(credenciales.validar(secreto).map(CredencialesGestion.TurnoAutorizado::id)).contains(turno);
            }
        }
    }

    @Test
    void preparaDespachoCombinadoUnaVezYRespetaDemorasDeReintento() throws Exception {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            try (var app = iniciar(db, claves())) {
                var inicio = START.plusYears(10);
                var ahora = inicio.minusDays(1).toInstant();
                UUID turno = UUID.randomUUID();
                CoreSchemaTest.insertTurn(db.jdbc, fixture, turno, inicio, inicio.plusMinutes(30), "CONFIRMADO");
                var coordinador = app.getBean(AgendaWriteCoordinator.class);
                var preparacion = app.getBean(PreparacionAvisos.class);
                UUID referencia = coordinador.write(fixture.agenda(),
                        () -> app.getBean(CredencialesGestion.class).emitir(turno));
                var evento = new PreparacionAvisos.Evento(UUID.randomUUID(), turno, 0, "paciente@example.test", ahora);
                coordinador.write(fixture.agenda(), () -> {
                    preparacion.reserva(evento, inicio.toInstant(), referencia);
                    preparacion.reserva(evento, inicio.toInstant(), referencia);
                    return null;
                });
                assertThat(db.jdbc.queryForList("""
                        SELECT tipo FROM intencion_notificacion WHERE evento_id = ?
                        """, String.class, evento.id())).containsExactlyInAnyOrder("CONFIRMACION", "RECORDATORIO");
                UUID despacho = db.jdbc.queryForObject("""
                        SELECT DISTINCT despacho_id FROM intencion_notificacion WHERE evento_id = ?
                        """, UUID.class, evento.id());
                var autorizacion = app.getBean(AutorizacionDespacho.class);
                var primero = autorizacion.autorizar(fixture.agenda(), despacho, InstantSource.fixed(ahora)).orElseThrow();
                assertThat(autorizacion.registrar(primero.id(), AutorizacionDespacho.Resultado.TRANSITORIO, ahora)).isTrue();
                assertThat(autorizacion.autorizar(fixture.agenda(), despacho, InstantSource.fixed(ahora.plusSeconds(59)))).isEmpty();
                var segundo = autorizacion.autorizar(fixture.agenda(), despacho, InstantSource.fixed(ahora.plusSeconds(60))).orElseThrow();
                assertThat(autorizacion.registrar(segundo.id(), AutorizacionDespacho.Resultado.TRANSITORIO,
                        ahora.plusSeconds(60))).isTrue();
                assertThat(autorizacion.autorizar(fixture.agenda(), despacho, InstantSource.fixed(ahora.plusSeconds(359)))).isEmpty();
                var tercero = autorizacion.autorizar(fixture.agenda(), despacho, InstantSource.fixed(ahora.plusSeconds(360))).orElseThrow();
                assertThat(autorizacion.registrar(tercero.id(), AutorizacionDespacho.Resultado.INCIERTO,
                        ahora.plusSeconds(360))).isTrue();
                assertThat(autorizacion.autorizar(fixture.agenda(), despacho, InstantSource.fixed(ahora.plusSeconds(720)))).isEmpty();
                assertThatThrownBy(() -> coordinador.write(fixture.agenda(), () -> {
                    var distinto = new PreparacionAvisos.Evento(evento.id(), turno, 0,
                            "otro@example.test", ahora);
                    preparacion.cambio(distinto, null, null);
                    return null;
                })).isInstanceOf(PreparacionAvisos.ConflictoEvento.class);
                assertThat(db.jdbc.queryForObject("""
                        SELECT count(*) FROM intencion_notificacion WHERE despacho_id = ? AND elegible
                        """, Long.class, despacho)).isEqualTo(2L);
            }
        }
    }

    @Test
    void conservaContenidoAutorizadoTrasReprogramarYBloqueaInciertoYVencido() throws Exception {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            try (var app = iniciar(db, claves())) {
                var coordinador = app.getBean(AgendaWriteCoordinator.class);
                var credenciales = app.getBean(CredencialesGestion.class);
                var avisos = app.getBean(PreparacionAvisos.class);
                var despachos = app.getBean(AutorizacionDespacho.class);
                var jdbc = app.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
                var inicio = START.plusYears(10);
                var ahora = inicio.minusDays(2).toInstant();
                UUID turno = UUID.randomUUID();
                var evento = new PreparacionAvisos.Evento(UUID.randomUUID(), turno, 0, "paciente@example.test", ahora);
                UUID referencia = coordinador.write(fixture.agenda(), () -> {
                    CoreSchemaTest.insertTurn(jdbc, fixture, turno, inicio, inicio.plusMinutes(30), "CONFIRMADO");
                    UUID nueva = credenciales.emitir(turno);
                    avisos.reserva(evento, inicio.toInstant(), nueva);
                    return nueva;
                });
                UUID confirmacion = db.jdbc.queryForObject("""
                        SELECT despacho_id FROM intencion_notificacion WHERE evento_id = ? AND tipo = 'CONFIRMACION'
                        """, UUID.class, evento.id());
                UUID recordatorio = db.jdbc.queryForObject("""
                        SELECT despacho_id FROM intencion_notificacion WHERE evento_id = ? AND tipo = 'RECORDATORIO'
                        """, UUID.class, evento.id());
                assertThat(despachos.autorizar(fixture.agenda(), recordatorio,
                        InstantSource.fixed(inicio.minusDays(1).plusHours(2).toInstant()))).isEmpty();
                var intento = despachos.autorizar(fixture.agenda(), confirmacion, InstantSource.fixed(ahora)).orElseThrow();
                var nuevo = inicio.plusHours(2);
                coordinador.write(fixture.agenda(), () -> {
                    jdbc.update("UPDATE turno SET inicio = ?, fin = ?, revision = 1 WHERE id = ?",
                            nuevo, nuevo.plusMinutes(30), turno);
                    avisos.cambio(new PreparacionAvisos.Evento(UUID.randomUUID(), turno, 1,
                            "paciente@example.test", ahora.plusSeconds(1)), nuevo.toInstant(), referencia);
                    return null;
                });
                assertThat(intento.inicio()).isEqualTo(inicio.toInstant());
                assertThat(intento.tipos()).containsExactly("CONFIRMACION");
                assertThat(despachos.recuperarEnlace(intento.id())).isPresent();
                assertThat(despachos.registrar(intento.id(), AutorizacionDespacho.Resultado.ACEPTADA_SMTP,
                        ahora.plusSeconds(2))).isTrue();
                assertThat(despachos.recuperarEnlace(intento.id())).isEmpty();
                assertThat(despachos.autorizar(fixture.agenda(), confirmacion, InstantSource.fixed(ahora.plusSeconds(120)))).isEmpty();
                UUID cambio = db.jdbc.queryForObject("""
                        SELECT despacho_id FROM intencion_notificacion WHERE turno_id = ? AND tipo = 'CAMBIO'
                        """, UUID.class, turno);
                var intentoCambio = despachos.autorizar(fixture.agenda(), cambio, InstantSource.fixed(ahora.plusSeconds(2))).orElseThrow();
                assertThat(despachos.registrar(intentoCambio.id(), AutorizacionDespacho.Resultado.INCIERTO,
                        ahora.plusSeconds(3))).isTrue();
                assertThat(despachos.autorizar(fixture.agenda(), cambio, InstantSource.fixed(ahora.plusSeconds(600)))).isEmpty();
                assertThat(db.jdbc.queryForObject("""
                        SELECT count(*) FROM intento_notificacion WHERE despacho_id = ?
                        """, Long.class, cambio)).isOne();
            }
        }
    }

    @Test
    void cancelaRevocaElEnlaceEImpideComponerUnIntentoYaAutorizado() throws Exception {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            try (var app = iniciar(db, claves())) {
                var coordinador = app.getBean(AgendaWriteCoordinator.class);
                var credenciales = app.getBean(CredencialesGestion.class);
                var avisos = app.getBean(PreparacionAvisos.class);
                var despachos = app.getBean(AutorizacionDespacho.class);
                var jdbc = app.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
                var inicio = START.plusYears(10);
                var ahora = inicio.minusDays(2).toInstant();
                UUID turno = UUID.randomUUID();
                UUID otroTurno = UUID.randomUUID();
                String destinatario = "paciente@example.test";
                CoreSchemaTest.insertTurn(db.jdbc, fixture, otroTurno, inicio.plusHours(2),
                        inicio.plusHours(2).plusMinutes(30), "CONFIRMADO");
                UUID eventoReserva = UUID.randomUUID();
                UUID referencia = coordinador.write(fixture.agenda(), () -> {
                    CoreSchemaTest.insertTurn(jdbc, fixture, turno, inicio, inicio.plusMinutes(30), "CONFIRMADO");
                    UUID credencial = credenciales.emitir(turno);
                    avisos.reserva(new PreparacionAvisos.Evento(eventoReserva, turno, 0, destinatario, ahora),
                            inicio.toInstant(), credencial);
                    return credencial;
                });
                var secreto = credenciales.recuperarParaComposicion(referencia, turno, 0).orElseThrow();
                assertThat(credenciales.validar(secreto).map(CredencialesGestion.TurnoAutorizado::id)).contains(turno);
                assertThat(credenciales.validar(secreto).map(CredencialesGestion.TurnoAutorizado::id))
                        .hasValueSatisfying(id -> assertThat(id).isNotEqualTo(otroTurno));
                assertThat(credenciales.validar(destinatario)).isEmpty();
                assertThat(credenciales.validar(turno.toString())).isEmpty();
                assertThat(credenciales.validar("002")).isEmpty();
                UUID confirmacion = db.jdbc.queryForObject("""
                        SELECT despacho_id FROM intencion_notificacion WHERE evento_id = ? AND tipo = 'CONFIRMACION'
                        """, UUID.class, eventoReserva);
                UUID recordatorio = db.jdbc.queryForObject("""
                        SELECT despacho_id FROM intencion_notificacion WHERE evento_id = ? AND tipo = 'RECORDATORIO'
                        """, UUID.class, eventoReserva);
                var intento = despachos.autorizar(fixture.agenda(), confirmacion, InstantSource.fixed(ahora)).orElseThrow();
                var cancelado = inicio.minusDays(2).plusSeconds(1);
                UUID eventoCancelacion = UUID.randomUUID();
                coordinador.write(fixture.agenda(), () -> {
                    jdbc.update("""
                            UPDATE turno SET estado = 'CANCELADO', cancelado_en = ?, revision = 1 WHERE id = ?
                            """, cancelado, turno);
                    credenciales.revocar(turno);
                    avisos.cambio(new PreparacionAvisos.Evento(eventoCancelacion, turno, 1, destinatario,
                            cancelado.toInstant()), null, null);
                    return null;
                });
                assertThat(credenciales.validar(secreto)).isEmpty();
                assertThat(despachos.recuperarEnlace(intento.id())).isEmpty();
                assertThat(despachos.registrar(intento.id(), AutorizacionDespacho.Resultado.PERMANENTE,
                        cancelado.toInstant())).isTrue();
                assertThat(despachos.autorizar(fixture.agenda(), recordatorio, InstantSource.fixed(inicio.minusDays(1).toInstant())))
                        .isEmpty();
                UUID cambio = db.jdbc.queryForObject("""
                        SELECT despacho_id FROM intencion_notificacion
                        WHERE evento_id = ? AND tipo = 'CAMBIO' AND credencial_id IS NULL AND revision = 1
                        """, UUID.class, eventoCancelacion);
                var intentoCambio = despachos.autorizar(fixture.agenda(), cambio, InstantSource.fixed(cancelado.toInstant())).orElseThrow();
                assertThat(intentoCambio.estado()).isEqualTo("CANCELADO");
                assertThat(intentoCambio.inicio()).isEqualTo(inicio.toInstant());
                assertThat(intentoCambio.destinatario()).isEqualTo(destinatario);
                assertThat(intentoCambio.credencial()).isNull();
            }
        }
    }

    @Test
    void revierteReservaCredencialEIntencionesEnLaMismaTransaccion() throws Exception {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            try (var app = iniciar(db, claves())) {
                var inicio = START.plusYears(10);
                UUID turno = UUID.randomUUID();
                var coordinador = app.getBean(AgendaWriteCoordinator.class);
                var credenciales = app.getBean(CredencialesGestion.class);
                var avisos = app.getBean(PreparacionAvisos.class);
                assertThatThrownBy(() -> coordinador.write(fixture.agenda(), () -> {
                    CoreSchemaTest.insertTurn(app.getBean(org.springframework.jdbc.core.JdbcTemplate.class),
                            fixture, turno, inicio, inicio.plusMinutes(30), "CONFIRMADO");
                    UUID referencia = credenciales.emitir(turno);
                    var evento = new PreparacionAvisos.Evento(UUID.randomUUID(), turno, 0,
                            "paciente@example.test", inicio.minusDays(2).toInstant());
                    avisos.reserva(evento, inicio.toInstant(), referencia);
                    throw new FalloSimulado();
                })).isInstanceOf(FalloSimulado.class);
                assertThat(db.jdbc.queryForObject("SELECT count(*) FROM turno WHERE id = ?", Long.class, turno))
                        .isZero();
                assertThat(db.jdbc.queryForObject("SELECT count(*) FROM credencial_gestion", Long.class)).isZero();
                assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion", Long.class)).isZero();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"recuperacion", "autorizacion", "incierto", "rollback"})
    void conservaRecordatorioOIntentoSegunOrden(String caso) throws Exception {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            try (var app = iniciar(db, claves());
                    var threads = Executors.newVirtualThreadPerTaskExecutor()) {
                var coordinator = app.getBean(AgendaWriteCoordinator.class);
                var credenciales = app.getBean(CredencialesGestion.class);
                var avisos = app.getBean(AvisosRecuperacion.class);
                var despachos = app.getBean(AutorizacionDespacho.class);
                UUID turno = UUID.randomUUID();
                UUID recordatorio = UUID.randomUUID();
                UUID evento = UUID.randomUUID();
                var inicio = START.plusYears(10);
                var programada = inicio.minusDays(1);
                CoreSchemaTest.insertTurn(db.jdbc, fixture, turno, inicio, inicio.plusMinutes(30), "CONFIRMADO");
                UUID anterior = coordinator.write(fixture.agenda(), () -> credenciales.emitir(turno));
                String secretoAnterior = credenciales.recuperarParaComposicion(anterior, turno, 0).orElseThrow();
                db.jdbc.update("""
                        INSERT INTO intencion_notificacion
                            (id, evento_id, turno_id, revision, tipo, destinatario, credencial_id,
                             programada_en, vence_en, despacho_id)
                        VALUES (?, ?, ?, 0, 'RECORDATORIO', 'anterior@example.test', ?, ?, ?, ?)
                        """, recordatorio, UUID.randomUUID(), turno, anterior, programada, programada.plusHours(2),
                        recordatorio);
                var hora = programada.plusMinutes(1).toInstant();
                CountDownLatch dentro = new CountDownLatch(1);
                CountDownLatch liberar = new CountDownLatch(1);
                if (caso.equals("rollback")) {
                    assertThatThrownBy(() -> coordinator.write(fixture.agenda(), () -> {
                        recuperar(credenciales, avisos, evento, turno, hora);
                        throw new FalloSimulado();
                    })).isInstanceOf(FalloSimulado.class);
                    assertThat(credenciales.validar(secretoAnterior)).isPresent();
                } else {
                    boolean recuperacionPrimero = caso.equals("recuperacion");
                    var primero = threads.submit(() -> coordinator.write(fixture.agenda(), () -> {
                        if (recuperacionPrimero) {
                            recuperar(credenciales, avisos, evento, turno, hora);
                        } else {
                            autorizar(db, recordatorio, hora);
                            if (caso.equals("incierto")) {
                                db.jdbc.update("""
                                        UPDATE intento_notificacion SET resultado = 'INCIERTO', terminado_en = ?
                                        WHERE despacho_id = ?
                                        """, hora.atOffset(ZoneOffset.UTC), recordatorio);
                            }
                        }
                        dentro.countDown();
                        esperar(liberar);
                        return null;
                    }));
                    try {
                        esperar(dentro);
                        var segundo = threads.submit(() -> {
                            if (recuperacionPrimero) {
                                var intento = despachos.autorizar(fixture.agenda(), recordatorio, InstantSource.fixed(hora)).orElseThrow();
                                assertThat(intento.destinatario()).isEqualTo("nuevo@example.test");
                                assertThat(intento.credencial()).isNotEqualTo(anterior);
                            } else {
                                coordinator.write(fixture.agenda(), () -> {
                                    recuperar(credenciales, avisos, evento, turno, hora);
                                    return null;
                                });
                            }
                            return null;
                        });
                        liberar.countDown();
                        primero.get(8, TimeUnit.SECONDS);
                        segundo.get(8, TimeUnit.SECONDS);
                    } finally {
                        liberar.countDown();
                    }
                    assertThat(credenciales.validar(secretoAnterior)).isEmpty();
                }
                var fila = db.jdbc.queryForMap("SELECT * FROM intencion_notificacion WHERE id = ?", recordatorio);
                assertThat(db.jdbc.queryForObject("""
                        SELECT programada_en = ? FROM intencion_notificacion WHERE id = ?
                        """, Boolean.class, programada, recordatorio)).isTrue();
                assertThat(fila.get("destinatario")).isEqualTo(caso.equals("recuperacion")
                        ? "nuevo@example.test" : "anterior@example.test");
                assertThat(fila.get("elegible")).isEqualTo(caso.equals("recuperacion") || caso.equals("rollback"));
                assertThat(fila.get("credencial_id").equals(anterior)).isEqualTo(!caso.equals("recuperacion"));
                assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion WHERE recuperacion",
                        Integer.class)).isEqualTo(caso.equals("rollback") ? 0 : 1);
                assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intento_notificacion", Integer.class))
                        .isEqualTo(caso.equals("rollback") ? 0 : 1);
                assertThat(db.jdbc.queryForObject("SELECT count(*) FROM credencial_gestion WHERE turno_id = ?",
                        Integer.class, turno)).isEqualTo(caso.equals("rollback") ? 1 : 2);
                if (caso.equals("incierto")) {
                    assertThat(db.jdbc.queryForObject("""
                            SELECT resultado FROM intento_notificacion WHERE despacho_id = ?
                            """, String.class, recordatorio)).isEqualTo("INCIERTO");
                }
            }
        }
    }

    @Test
    void noPersisteIntentoSiElAvisoVenceMientrasEsperaLaAgenda() throws Exception {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            try (var app = iniciar(db, claves()); var threads = Executors.newVirtualThreadPerTaskExecutor()) {
                var coordinador = app.getBean(AgendaWriteCoordinator.class);
                var despachos = app.getBean(AutorizacionDespacho.class);
                UUID turno = UUID.randomUUID();
                UUID despacho = UUID.randomUUID();
                var inicio = START.plusYears(10);
                var vence = inicio.minusHours(22);
                CoreSchemaTest.insertTurn(db.jdbc, fixture, turno, inicio, inicio.plusMinutes(30), "CONFIRMADO");
                db.jdbc.update("""
                        INSERT INTO intencion_notificacion
                            (id, evento_id, turno_id, revision, tipo, destinatario, programada_en, vence_en, despacho_id)
                        VALUES (?, ?, ?, 0, 'RECORDATORIO', 'paciente@example.test', ?, ?, ?)
                        """, despacho, UUID.randomUUID(), turno, inicio.minusDays(1), vence, despacho);

                var hora = new AtomicReference<>(vence.minusSeconds(1).toInstant());
                var bloqueada = new CountDownLatch(1);
                var liberar = new CountDownLatch(1);
                var bloqueo = threads.submit(() -> coordinador.write(fixture.agenda(), () -> {
                    bloqueada.countDown();
                    esperar(liberar);
                    return null;
                }));
                try {
                    esperar(bloqueada);
                    var intento = threads.submit(() -> despachos.autorizar(fixture.agenda(), despacho, hora::get));
                    AgendaWriteCoordinatorTest.awaitLockWait(db);
                    hora.set(vence.toInstant());
                    liberar.countDown();
                    bloqueo.get(8, TimeUnit.SECONDS);
                    assertThat(intento.get(8, TimeUnit.SECONDS)).isEmpty();
                } finally {
                    liberar.countDown();
                }
                assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intento_notificacion WHERE despacho_id = ?",
                        Long.class, despacho)).isZero();
            }
        }
    }

    private Path claves() throws Exception {
        Path keys = Files.createTempFile(directory, "gestion-", ".properties");
        byte[] testKey = new byte[32];
        new java.security.SecureRandom().nextBytes(testKey);
        Files.writeString(keys, "activa=prueba\nclave.prueba=" + Base64.getEncoder().encodeToString(testKey));
        return keys;
    }

    private static org.springframework.context.ConfigurableApplicationContext iniciar(TestDatabase db, Path keys) {
        return new SpringApplicationBuilder(OdontologiaApplication.class).web(WebApplicationType.NONE)
                .run("--spring.datasource.url=" + db.url(), "--app.base-url=http://127.0.0.1:5173",
                        "--app.time-zone=Etc/UTC", "--app.consultorio-nombre=Demo",
                        "--app.odontologo-nombre=Profesional de prueba", "--app.gestion-key-file=" + keys);
    }

    private static void autorizar(TestDatabase db, UUID despacho, Instant hora) {
        db.jdbc.update("""
                INSERT INTO intento_notificacion
                    (id, despacho_id, turno_id, revision, destinatario, credencial_id, autorizado_en,
                     inicio_turno, fin_turno, estado_turno, tipos, recuperacion)
                SELECT ?, n.despacho_id, n.turno_id, n.revision, n.destinatario, n.credencial_id, ?,
                    t.inicio, t.fin, t.estado, ARRAY[n.tipo], n.recuperacion
                FROM intencion_notificacion n JOIN turno t ON t.id = n.turno_id WHERE n.despacho_id = ?
                """, UUID.randomUUID(), hora.atOffset(ZoneOffset.UTC), despacho);
    }

    private static void recuperar(CredencialesGestion credenciales, AvisosRecuperacion avisos,
            UUID evento, UUID turno, Instant hora) {
        credenciales.revocar(turno);
        avisos.preparar(evento, turno, 0, credenciales.emitir(turno), "nuevo@example.test", hora);
    }

    private static void esperar(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError(failure);
        }
    }

    private static final class FalloSimulado extends RuntimeException {}
}
