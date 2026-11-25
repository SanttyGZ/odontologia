package dev.odontologia;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import dev.odontologia.agenda.AgendaQueries;
import dev.odontologia.agenda.DentistAgendaController;
import dev.odontologia.agenda.DentistAgendaService;
import dev.odontologia.consultorio.ConsultorioQueries;
import dev.odontologia.disponibilidad.AvailabilityErrorAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DentistAgendaIntegrationTest {

    @Test
    void queriesDailyAndWeeklyAgendaAndTurnDetailInConsultorioTimezone() throws Exception {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            db.jdbc.update("UPDATE consultorio SET zona_horaria = ?", "America/Argentina/Mendoza");

            // 1. Contiguous turn ending at 00:00 Mendoza on 2030-01-08 (2030-01-07 23:30-00:00 Mendoza):
            //    Must NOT appear in vista=DIA&fecha=2030-01-08, MUST appear in ISO week 2030-01-07..2030-01-13.
            UUID mondayLateTurn = UUID.randomUUID();
            OffsetDateTime mondayLateStart = OffsetDateTime.parse("2030-01-08T02:30:00Z");
            CoreSchemaTest.insertTurn(db.jdbc, fixture, mondayLateTurn, mondayLateStart,
                    mondayLateStart.plusMinutes(30), "CONFIRMADO");

            // 2. Confirmed turn on 2030-01-08 at 22:00 Mendoza (2030-01-09T01:00:00Z), 30m:
            //    Must appear in vista=DIA&fecha=2030-01-08 and NOT in vista=DIA&fecha=2030-01-09.
            UUID tuesdayNightTurn = UUID.randomUUID();
            OffsetDateTime tuesdayNightStart = OffsetDateTime.parse("2030-01-09T01:00:00Z");
            CoreSchemaTest.insertTurn(db.jdbc, fixture, tuesdayNightTurn, tuesdayNightStart,
                    tuesdayNightStart.plusMinutes(30), "CONFIRMADO");

            // 3. Same-start cancelled & rescheduled confirmed turns on 2030-01-10 10:00 Mendoza (13:00Z)
            //    to verify deterministic ordering (inicio ASC, id ASC) and revision/canceladoEn fields.
            UUID confirmedRescheduledId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID cancelledTurnId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            OffsetDateTime thursdayStart = OffsetDateTime.parse("2030-01-10T13:00:00Z");
            OffsetDateTime thursdayCreated = OffsetDateTime.parse("2030-01-09T10:00:00Z");
            OffsetDateTime thursdayCancelled = OffsetDateTime.parse("2030-01-09T15:00:00Z");
            db.jdbc.update("""
                    INSERT INTO turno (id, agenda_id, paciente_id, motivo_id, inicio, fin,
                                       duracion_reservada_minutos, estado, revision, creado_en, cancelado_en)
                    VALUES (?, ?, ?, ?, ?, ?, 30, 'CANCELADO', 1, ?, ?)
                    """, cancelledTurnId, fixture.agenda(), fixture.paciente(), fixture.motivo(),
                    thursdayStart, thursdayStart.plusMinutes(30), thursdayCreated, thursdayCancelled);
            db.jdbc.update("""
                    INSERT INTO turno (id, agenda_id, paciente_id, motivo_id, inicio, fin,
                                       duracion_reservada_minutos, estado, revision, creado_en)
                    VALUES (?, ?, ?, ?, ?, ?, 30, 'CONFIRMADO', 1, ?)
                    """, confirmedRescheduledId, fixture.agenda(), fixture.paciente(), fixture.motivo(),
                    thursdayStart, thursdayStart.plusMinutes(30), thursdayCreated);

            // Fixed clock: 2030-01-09T01:30:00Z = 2030-01-08 22:30 Mendoza
            Clock fixedClock = Clock.fixed(Instant.parse("2030-01-09T01:30:00Z"), ZoneOffset.UTC);
            var service = new DentistAgendaService(new AgendaQueries(db.jdbc), new ConsultorioQueries(db.jdbc),
                    fixedClock);
            MockMvc mvc = MockMvcBuilders.standaloneSetup(new DentistAgendaController(service))
                    .setControllerAdvice(new AvailabilityErrorAdvice())
                    .build();

            // Default vista=DIA and fecha from fixedClock (2030-01-08 Mendoza)
            mvc.perform(get("/api/odontologo/agenda"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.vista").value("DIA"))
                    .andExpect(jsonPath("$.desde").value("2030-01-08"))
                    .andExpect(jsonPath("$.hasta").value("2030-01-08"))
                    .andExpect(jsonPath("$.zonaHoraria").value("America/Argentina/Mendoza"))
                    .andExpect(jsonPath("$.turnos.length()").value(1))
                    .andExpect(jsonPath("$.turnos[0].id").value(tuesdayNightTurn.toString()))
                    .andExpect(jsonPath("$.turnos[0].estado").value("CONFIRMADO"))
                    .andExpect(jsonPath("$.turnos[0].pacienteNombre").value("Paciente"))
                    .andExpect(jsonPath("$.turnos[0].pacienteApellido").value("Prueba"));

            // Explicit vista=DIA&fecha=2030-01-08
            mvc.perform(get("/api/odontologo/agenda")
                            .param("vista", "DIA")
                            .param("fecha", "2030-01-08"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.turnos.length()").value(1))
                    .andExpect(jsonPath("$.turnos[0].id").value(tuesdayNightTurn.toString()));

            // Empty date (and verifies 22:00 Mendoza / 01:00Z turn is NOT included in 2030-01-09)
            mvc.perform(get("/api/odontologo/agenda")
                            .param("vista", "DIA")
                            .param("fecha", "2030-01-09"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.desde").value("2030-01-09"))
                    .andExpect(jsonPath("$.hasta").value("2030-01-09"))
                    .andExpect(jsonPath("$.turnos").isEmpty());

            // Weekly view on Saturday 2030-01-12 -> ISO week 2030-01-07..2030-01-13
            mvc.perform(get("/api/odontologo/agenda")
                            .param("vista", "SEMANA")
                            .param("fecha", "2030-01-12"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.vista").value("SEMANA"))
                    .andExpect(jsonPath("$.desde").value("2030-01-07"))
                    .andExpect(jsonPath("$.hasta").value("2030-01-13"))
                    .andExpect(jsonPath("$.zonaHoraria").value("America/Argentina/Mendoza"))
                    .andExpect(jsonPath("$.turnos.length()").value(4))
                    .andExpect(jsonPath("$.turnos[0].id").value(mondayLateTurn.toString()))
                    .andExpect(jsonPath("$.turnos[1].id").value(tuesdayNightTurn.toString()))
                    .andExpect(jsonPath("$.turnos[2].id").value(confirmedRescheduledId.toString()))
                    .andExpect(jsonPath("$.turnos[2].estado").value("CONFIRMADO"))
                    .andExpect(jsonPath("$.turnos[3].id").value(cancelledTurnId.toString()))
                    .andExpect(jsonPath("$.turnos[3].estado").value("CANCELADO"));

            // Turn detail for cancelled turn
            mvc.perform(get("/api/odontologo/turnos/{id}", cancelledTurnId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(cancelledTurnId.toString()))
                    .andExpect(jsonPath("$.motivoId").value(fixture.motivo().toString()))
                    .andExpect(jsonPath("$.motivoNombre").value("Motivo de prueba"))
                    .andExpect(jsonPath("$.duracionReservadaMinutos").value(30))
                    .andExpect(jsonPath("$.estado").value("CANCELADO"))
                    .andExpect(jsonPath("$.revision").value(1))
                    .andExpect(jsonPath("$.creadoEn").value("2030-01-09T10:00:00Z"))
                    .andExpect(jsonPath("$.canceladoEn").value("2030-01-09T15:00:00Z"))
                    .andExpect(jsonPath("$.paciente.nombre").value("Paciente"))
                    .andExpect(jsonPath("$.paciente.apellido").value("Prueba"))
                    .andExpect(jsonPath("$.paciente.email").value("f@example.invalid"))
                    .andExpect(jsonPath("$.paciente.telefono").value("001"))
                    .andExpect(jsonPath("$.paciente.dni").value("002"));

            // Turn detail for rescheduled confirmed turn
            mvc.perform(get("/api/odontologo/turnos/{id}", confirmedRescheduledId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(confirmedRescheduledId.toString()))
                    .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                    .andExpect(jsonPath("$.revision").value(1))
                    .andExpect(jsonPath("$.canceladoEn").value(nullValue()))
                    .andExpect(jsonPath("$.paciente.nombre").value("Paciente"))
                    .andExpect(jsonPath("$.paciente.apellido").value("Prueba"))
                    .andExpect(jsonPath("$.paciente.email").value("f@example.invalid"))
                    .andExpect(jsonPath("$.paciente.telefono").value("001"))
                    .andExpect(jsonPath("$.paciente.dni").value("002"));

            // Non-existent turn returns 404 TURNO_NOT_FOUND
            mvc.perform(get("/api/odontologo/turnos/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("TURNO_NOT_FOUND"));
        }
    }

    @Test
    void agendaRoutesRequireInternalSession() throws Exception {
        try (var db = new TestDatabase()) {
            CoreSchemaTest.fixture(db);
            try (var app = start(db)) {
                var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
                String base = "http://127.0.0.1:" + app.getWebServer().getPort();

                var agendaResponse = client.send(
                        HttpRequest.newBuilder(URI.create(base + "/api/odontologo/agenda"))
                                .timeout(Duration.ofSeconds(5))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
                assertThat(agendaResponse.statusCode()).isEqualTo(401);
                assertThat(agendaResponse.body()).isEqualTo("{\"codigo\":\"SESSION_REQUIRED\"}");

                var detailResponse = client.send(
                        HttpRequest.newBuilder(URI.create(base + "/api/odontologo/turnos/" + UUID.randomUUID()))
                                .timeout(Duration.ofSeconds(5))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
                assertThat(detailResponse.statusCode()).isEqualTo(401);
                assertThat(detailResponse.body()).isEqualTo("{\"codigo\":\"SESSION_REQUIRED\"}");
            }
        }
    }

    private static ServletWebServerApplicationContext start(TestDatabase db) {
        return (ServletWebServerApplicationContext) new SpringApplicationBuilder(OdontologiaApplication.class).run(
                "--spring.datasource.url=" + db.url(),
                "--server.port=0",
                "--server.address=127.0.0.1",
                "--app.base-url=http://127.0.0.1:5173",
                "--app.time-zone=Etc/UTC",
                "--app.consultorio-nombre=" + CoreSchemaTest.TEST_CONSULTORIO_NOMBRE,
                "--app.odontologo-nombre=" + CoreSchemaTest.TEST_ODONTOLOGO_NOMBRE,
                "--app.dentist-credential-file=",
                "--spring.main.banner-mode=off");
    }
}
