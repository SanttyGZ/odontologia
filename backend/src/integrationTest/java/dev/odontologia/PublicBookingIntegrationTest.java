package dev.odontologia;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
import dev.odontologia.disponibilidad.AvailabilityErrorAdvice;
import dev.odontologia.disponibilidad.AvailabilityException;
import dev.odontologia.disponibilidad.PublicAvailabilityController;
import dev.odontologia.disponibilidad.PublicAvailabilityService;
import dev.odontologia.disponibilidad.PublicBookingController;
import dev.odontologia.disponibilidad.PublicBookingDtos.ReservaInput;
import dev.odontologia.disponibilidad.PublicBookingService;
import dev.odontologia.gestion.CredencialesGestion;
import dev.odontologia.pacientes.PacienteQueries;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicBookingIntegrationTest {

    @Test
    void confirmsBookingWithNotificationsAndAppliesExactPatientReusePolicy() throws Exception {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            db.jdbc.update("UPDATE consultorio SET zona_horaria = ?", "America/Argentina/Mendoza");
            var coordinator = new AgendaWriteCoordinator(db.jdbc, new JdbcTransactionManager(db.dataSource()));
            var configService = new AvailabilityConfigurationService(db.jdbc, new AgendaQueries(db.jdbc),
                    new ConsultorioQueries(db.jdbc), coordinator);
            configService.replaceHorario(List.of(
                    new FranjaInput(UUID.randomUUID(), 2, LocalTime.of(9, 0), LocalTime.of(12, 0), "ATENCION")));

            Clock clock = Clock.fixed(Instant.parse("2030-01-07T12:00:00Z"), ZoneOffset.UTC);
            var bookingService = bookingService(db, coordinator, clock);
            MockMvc mvc = MockMvcBuilders.standaloneSetup(new PublicBookingController(bookingService))
                    .setControllerAdvice(new AvailabilityErrorAdvice())
                    .build();

            long patientsBefore = db.jdbc.queryForObject("SELECT count(*) FROM paciente", Long.class);

            // 1. First booking creates patient, confirmed turn, credential, and CONFIRMACION + RECORDATORIO intents
            mvc.perform(post("/api/public/turnos").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "motivoId": "%s",
                                      "inicio": "2030-01-08T12:00:00Z",
                                      "nombre": "Ana",
                                      "apellido": "Perez",
                                      "email": "ANA@example.com",
                                      "telefono": "2615551234",
                                      "dni": "30123456"
                                    }
                                    """.formatted(fixture.motivo())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                    .andExpect(jsonPath("$.notificacionEstado").value("PENDIENTE"))
                    .andExpect(jsonPath("$.duracionReservadaMinutos").value(30))
                    .andExpect(jsonPath("$.credencialId").doesNotExist());

            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM paciente", Long.class)).isEqualTo(patientsBefore + 1);
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion", Long.class)).isEqualTo(2L);

            // 2. Exact 5-field match reuses existing patient for another slot
            mvc.perform(post("/api/public/turnos").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "motivoId": "%s",
                                      "inicio": "2030-01-08T12:30:00Z",
                                      "nombre": "Ana",
                                      "apellido": "Perez",
                                      "email": "ana@example.com",
                                      "telefono": "2615551234",
                                      "dni": "30123456"
                                    }
                                    """.formatted(fixture.motivo())))
                    .andExpect(status().isCreated());
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM paciente", Long.class)).isEqualTo(patientsBefore + 1);

            // 3. Same DNI/email but different telefono creates a new patient without modifying the first
            mvc.perform(post("/api/public/turnos").contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "motivoId": "%s",
                                      "inicio": "2030-01-08T13:00:00Z",
                                      "nombre": "Ana",
                                      "apellido": "Perez",
                                      "email": "ana@example.com",
                                      "telefono": "2615559999",
                                      "dni": "30123456"
                                    }
                                    """.formatted(fixture.motivo())))
                    .andExpect(status().isCreated());
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM paciente", Long.class)).isEqualTo(patientsBefore + 2);
        }
    }

    @Test
    void concurrentOverlappingBookingsAllowAtMostOneConfirmedTurnAndRollbackLoser() throws Exception {
        try (var db = new TestDatabase(); var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            var fixture = CoreSchemaTest.fixture(db);
            db.jdbc.update("UPDATE consultorio SET zona_horaria = ?", "America/Argentina/Mendoza");
            var coordinator = new AgendaWriteCoordinator(db.jdbc, new JdbcTransactionManager(db.dataSource()));
            var configService = new AvailabilityConfigurationService(db.jdbc, new AgendaQueries(db.jdbc),
                    new ConsultorioQueries(db.jdbc), coordinator);
            configService.replaceHorario(List.of(
                    new FranjaInput(UUID.randomUUID(), 2, LocalTime.of(9, 0), LocalTime.of(12, 0), "ATENCION")));

            Clock clock = Clock.fixed(Instant.parse("2030-01-07T12:00:00Z"), ZoneOffset.UTC);
            var bookingService = bookingService(db, coordinator, clock);
            var startBarrier = new CountDownLatch(1);

            // Partially overlapping intervals: 09:00-09:30 vs 09:15-09:45 (both 30m)
            var first = threads.submit(() -> {
                startBarrier.await(4, TimeUnit.SECONDS);
                return attempt(bookingService, new ReservaInput(fixture.motivo(),
                        Instant.parse("2030-01-08T12:00:00Z"), "Uno", "Test", "uno@example.com", "2615550001", "11111111"));
            });
            var second = threads.submit(() -> {
                startBarrier.await(4, TimeUnit.SECONDS);
                return attempt(bookingService, new ReservaInput(fixture.motivo(),
                        Instant.parse("2030-01-08T12:15:00Z"), "Dos", "Test", "dos@example.com", "2615550002", "22222222"));
            });
            startBarrier.countDown();

            List<String> outcomes = List.of(first.get(8, TimeUnit.SECONDS), second.get(8, TimeUnit.SECONDS));
            assertThat(outcomes).containsExactlyInAnyOrder("CONFIRMED", "SLOT_UNAVAILABLE");
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM turno WHERE estado = 'CONFIRMADO'", Long.class))
                    .isOne();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion", Long.class))
                    .isEqualTo(2L);
        }
    }

    @Test
    void blockCommittedFirstCausesPendingBookingToRejectWithSlotUnavailable() throws Exception {
        try (var db = new TestDatabase(); var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            var fixture = CoreSchemaTest.fixture(db);
            db.jdbc.update("UPDATE consultorio SET zona_horaria = ?", "America/Argentina/Mendoza");
            var coordinator = new AgendaWriteCoordinator(db.jdbc, new JdbcTransactionManager(db.dataSource()));
            var configService = new AvailabilityConfigurationService(db.jdbc, new AgendaQueries(db.jdbc),
                    new ConsultorioQueries(db.jdbc), coordinator);
            configService.replaceHorario(List.of(
                    new FranjaInput(UUID.randomUUID(), 2, LocalTime.of(9, 0), LocalTime.of(12, 0), "ATENCION")));

            Clock clock = Clock.fixed(Instant.parse("2030-01-07T12:00:00Z"), ZoneOffset.UTC);
            var bookingService = bookingService(db, coordinator, clock);
            var blockerAcquired = new CountDownLatch(1);
            var releaseBlocker = new CountDownLatch(1);

            var blocker = threads.submit(() -> coordinator.write(fixture.agenda(), () -> {
                blockerAcquired.countDown();
                try {
                    if (!releaseBlocker.await(7, TimeUnit.SECONDS)) {
                        throw new AssertionError("No se liberó el bloqueo de prueba");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("Prueba interrumpida", interrupted);
                }
                return null;
            }));
            assertThat(blockerAcquired.await(4, TimeUnit.SECONDS)).isTrue();

            var blockFuture = threads.submit(() -> configService.createBloqueo(
                    Instant.parse("2030-01-08T12:00:00Z"), Instant.parse("2030-01-08T12:30:00Z"), "Bloqueo"));
            AgendaWriteCoordinatorTest.awaitLockWait(db);

            var bookingFuture = threads.submit(() -> attempt(bookingService, new ReservaInput(fixture.motivo(),
                    Instant.parse("2030-01-08T12:00:00Z"), "Ana", "Perez", "ana@example.com", "2615551234", "30123456")));
            Thread.sleep(150);
            releaseBlocker.countDown();

            blocker.get(8, TimeUnit.SECONDS);
            blockFuture.get(8, TimeUnit.SECONDS);
            assertThat(bookingFuture.get(8, TimeUnit.SECONDS)).isEqualTo("SLOT_UNAVAILABLE");
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM turno WHERE estado = 'CONFIRMADO'", Long.class))
                    .isZero();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion", Long.class)).isZero();
        }
    }

    @Test
    void getAvailabilityAndPostBookingShareExactGridAcrossMinuteSecondAndMicrosecondRanges() throws Exception {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            db.jdbc.update("UPDATE consultorio SET zona_horaria = ?", "America/Argentina/Mendoza");
            var coordinator = new AgendaWriteCoordinator(db.jdbc, new JdbcTransactionManager(db.dataSource()));
            var configService = new AvailabilityConfigurationService(db.jdbc, new AgendaQueries(db.jdbc),
                    new ConsultorioQueries(db.jdbc), coordinator);
            Clock clock = Clock.fixed(Instant.parse("2030-01-07T11:00:00Z"), ZoneOffset.UTC);
            var publicAvailabilityService = new PublicAvailabilityService(new AgendaQueries(db.jdbc),
                    new ConsultorioQueries(db.jdbc), clock);
            var bookingService = bookingService(db, coordinator, clock);
            MockMvc mvc = MockMvcBuilders.standaloneSetup(
                            new PublicAvailabilityController(publicAvailabilityService),
                            new PublicBookingController(bookingService))
                    .setControllerAdvice(new AvailabilityErrorAdvice())
                    .build();

            // 1. Exact-minute range (09:00:00 - 10:00:00 local)
            configService.replaceHorario(List.of(
                    new FranjaInput(UUID.randomUUID(), 2, LocalTime.of(9, 0, 0), LocalTime.of(10, 0, 0), "ATENCION")));
            assertGetToPostConfirmedAndOffGridRejected(mvc, fixture.motivo(), db,
                    "2030-01-08T12:16:00Z", "2030-01-08T12:15:00.123456Z");

            // 2. Second-aligned range (09:00:30 - 10:00:30 local on Wednesday day 3)
            configService.replaceHorario(List.of(
                    new FranjaInput(UUID.randomUUID(), 2, LocalTime.of(9, 0, 0), LocalTime.of(10, 0, 0), "ATENCION"),
                    new FranjaInput(UUID.randomUUID(), 3, LocalTime.of(9, 0, 30), LocalTime.of(10, 0, 30), "ATENCION")));
            assertGetToPostConfirmedAndOffGridRejectedForDate(mvc, fixture.motivo(), db,
                    "2030-01-09", "2030-01-09T12:16:00Z", "2030-01-09T12:15:00Z");

            // 3. Microsecond-aligned range (09:00:00.250000 - 10:00:00.250000 local on Thursday day 4)
            configService.replaceHorario(List.of(
                    new FranjaInput(UUID.randomUUID(), 2, LocalTime.of(9, 0, 0), LocalTime.of(10, 0, 0), "ATENCION"),
                    new FranjaInput(UUID.randomUUID(), 3, LocalTime.of(9, 0, 30), LocalTime.of(10, 0, 30), "ATENCION"),
                    new FranjaInput(UUID.randomUUID(), 4, LocalTime.of(9, 0, 0, 250_000_000),
                            LocalTime.of(10, 0, 0, 250_000_000), "ATENCION")));
            assertGetToPostConfirmedAndOffGridRejectedForDate(mvc, fixture.motivo(), db,
                    "2030-01-10", "2030-01-10T12:15:00Z", "2030-01-10T12:15:00.123456Z");
        }
    }

    private static void assertGetToPostConfirmedAndOffGridRejected(MockMvc mvc, UUID motivoId, TestDatabase db,
                                                                   String offGridOne, String offGridTwo) throws Exception {
        assertGetToPostConfirmedAndOffGridRejectedForDate(mvc, motivoId, db, "2030-01-08", offGridOne, offGridTwo);
    }

    private static void assertGetToPostConfirmedAndOffGridRejectedForDate(MockMvc mvc, UUID motivoId, TestDatabase db,
                                                                          String date, String offGridOne,
                                                                          String offGridTwo) throws Exception {
        String getJson = mvc.perform(get("/api/public/disponibilidad")
                        .param("motivoId", motivoId.toString())
                        .param("desde", date)
                        .param("hasta", date))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String firstInicio = com.jayway.jsonpath.JsonPath.read(getJson, "$.horarios[0].inicio");

        long turnsBefore = db.jdbc.queryForObject("SELECT count(*) FROM turno", Long.class);
        long intentsBefore = db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion", Long.class);

        mvc.perform(post("/api/public/turnos").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload(motivoId, firstInicio)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));

        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM turno", Long.class)).isEqualTo(turnsBefore + 1);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion", Long.class))
                .isEqualTo(intentsBefore + 2);

        for (String invalidInicio : List.of(offGridOne, offGridTwo)) {
            mvc.perform(post("/api/public/turnos").contentType(MediaType.APPLICATION_JSON)
                            .content(bookingPayload(motivoId, invalidInicio)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("SLOT_UNAVAILABLE"));
        }

        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM turno", Long.class)).isEqualTo(turnsBefore + 1);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM intencion_notificacion", Long.class))
                .isEqualTo(intentsBefore + 2);
    }

    private static String bookingPayload(UUID motivoId, String inicio) {
        return """
                {
                  "motivoId": "%s",
                  "inicio": "%s",
                  "nombre": "Ana",
                  "apellido": "Perez",
                  "email": "ana@example.com",
                  "telefono": "2615551234",
                  "dni": "30123456"
                }
                """.formatted(motivoId, inicio);
    }

    private static String attempt(PublicBookingService service, ReservaInput input) {
        try {
            service.reservar(input);
            return "CONFIRMED";
        } catch (AvailabilityException failure) {
            return failure.codigo();
        }
    }

    private static PublicBookingService bookingService(TestDatabase db, AgendaWriteCoordinator coordinator, Clock clock) {
        var credenciales = mock(CredencialesGestion.class);
        when(credenciales.emitir(any())).thenAnswer(invocation -> {
            UUID turnoId = invocation.getArgument(0);
            UUID credencialId = UUID.randomUUID();
            byte[] verificador = new byte[32];
            java.nio.ByteBuffer.wrap(verificador)
                    .putLong(credencialId.getMostSignificantBits())
                    .putLong(credencialId.getLeastSignificantBits());
            db.jdbc.update("""
                    INSERT INTO credencial_gestion (id, turno_id, verificador, material_cifrado, nonce, version_clave, emitida_en)
                    VALUES (?, ?, ?, ?, ?, 'v1', ?)
                    """, credencialId, turnoId, verificador, new byte[48], new byte[12],
                    OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
            return credencialId;
        });
        return new PublicBookingService(db.jdbc, new AgendaQueries(db.jdbc), new ConsultorioQueries(db.jdbc),
                new PacienteQueries(db.jdbc), coordinator, credenciales, new PreparacionAvisos(db.jdbc), clock);
    }
}
