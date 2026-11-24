package dev.odontologia;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import dev.odontologia.agenda.AgendaQueries;
import dev.odontologia.agenda.AgendaWriteCoordinator;
import dev.odontologia.consultorio.ConsultorioQueries;
import dev.odontologia.disponibilidad.AvailabilityConfigurationService;
import dev.odontologia.disponibilidad.AvailabilityDtos.FranjaInput;
import dev.odontologia.disponibilidad.AvailabilityErrorAdvice;
import dev.odontologia.disponibilidad.PublicAvailabilityController;
import dev.odontologia.disponibilidad.PublicAvailabilityService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicAvailabilityIntegrationTest {

    @Test
    void returnsPublicMotivosAndComputesAvailabilityAgainstPostgresFixturesWithoutExposingPatients() throws Exception {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            // Set consultorio zone to America/Argentina/Mendoza (UTC-3)
            db.jdbc.update("UPDATE consultorio SET zona_horaria = ?", "America/Argentina/Mendoza");
            var configService = new AvailabilityConfigurationService(db.jdbc, new AgendaQueries(db.jdbc),
                    new ConsultorioQueries(db.jdbc),
                    new AgendaWriteCoordinator(db.jdbc, new JdbcTransactionManager(db.dataSource())));

            // CoreSchemaTest.START is 2030-01-08T13:15:00Z (Tuesday 10:15 - 10:45 in Mendoza UTC-3).
            // Configure Tuesday (day 2): 09:00-10:45 and contiguous 10:45-12:00, pause 11:15-11:30.
            configService.replaceHorario(List.of(
                    new FranjaInput(UUID.randomUUID(), 2, LocalTime.of(9, 0), LocalTime.of(10, 45), "ATENCION"),
                    new FranjaInput(UUID.randomUUID(), 2, LocalTime.of(10, 45), LocalTime.of(12, 0), "ATENCION"),
                    new FranjaInput(UUID.randomUUID(), 2, LocalTime.of(11, 15), LocalTime.of(11, 30), "PAUSA")));

            // Confirmed turn 10:15-10:45 Mendoza (13:15-13:45Z)
            OffsetDateTime confirmedStart = OffsetDateTime.parse("2030-01-08T13:15:00Z");
            CoreSchemaTest.insertTurn(db.jdbc, fixture, UUID.randomUUID(), confirmedStart,
                    confirmedStart.plusMinutes(30), "CONFIRMADO");
            // Cancelled turn 09:30-10:00 Mendoza (12:30-13:00Z) -> must NOT block availability
            db.jdbc.update("""
                    INSERT INTO turno (id, agenda_id, paciente_id, motivo_id, inicio, fin,
                                       duracion_reservada_minutos, estado, revision, creado_en, cancelado_en)
                    VALUES (?, ?, ?, ?, ?, ?, 30, 'CANCELADO', 1, ?, ?)
                    """, UUID.randomUUID(), fixture.agenda(), fixture.paciente(), fixture.motivo(),
                    confirmedStart.minusMinutes(45), confirmedStart.minusMinutes(15),
                    confirmedStart.minusDays(1), confirmedStart.minusHours(12));
            // Block 09:00-09:15 Mendoza (12:00-12:15Z) -> blocks 09:00-09:30 slot, leaves 09:15-09:45 free
            configService.createBloqueo(Instant.parse("2030-01-08T12:00:00Z"),
                    Instant.parse("2030-01-08T12:15:00Z"), "Mantenimiento");

            // Fixed clock at 2030-01-07T12:15:00Z (09:15 Mendoza, exactly 24h before 2030-01-08T09:15 Mendoza)
            Clock fixedClock = Clock.fixed(Instant.parse("2030-01-07T12:15:00Z"), ZoneOffset.UTC);
            var publicService = new PublicAvailabilityService(new AgendaQueries(db.jdbc),
                    new ConsultorioQueries(db.jdbc), fixedClock);
            MockMvc mvc = MockMvcBuilders.standaloneSetup(new PublicAvailabilityController(publicService))
                    .setControllerAdvice(new AvailabilityErrorAdvice())
                    .build();

            long turnsBefore = db.jdbc.queryForObject("SELECT count(*) FROM turno", Long.class);

            mvc.perform(get("/api/public/motivos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(fixture.motivo().toString()))
                    .andExpect(jsonPath("$[0].duracionMinutos").value(30))
                    .andExpect(jsonPath("$[0].agendaId").doesNotExist());

            // Expected Tuesday 2030-01-08 free 30m slots:
            // - 09:15-09:45 (12:15Z-12:45Z)
            // - 09:30-10:00 (12:30Z-13:00Z)
            // - 09:45-10:15 (12:45Z-13:15Z, contiguous to confirmed 10:15-10:45)
            // - 10:45-11:15 (13:45Z-14:15Z, contiguous to confirmed 10:15-10:45 and pause 11:15-11:30)
            // - 11:30-12:00 (14:30Z-15:00Z, contiguous to pause 11:15-11:30)
            mvc.perform(get("/api/public/disponibilidad")
                            .param("motivoId", fixture.motivo().toString())
                            .param("desde", "2030-01-08")
                            .param("hasta", "2030-01-08"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.motivoId").value(fixture.motivo().toString()))
                    .andExpect(jsonPath("$.duracionMinutos").value(30))
                    .andExpect(jsonPath("$.zonaHoraria").value("America/Argentina/Mendoza"))
                    .andExpect(jsonPath("$.horarios.length()").value(5))
                    .andExpect(jsonPath("$.horarios[0].inicio").value("2030-01-08T12:15:00Z"))
                    .andExpect(jsonPath("$.horarios[2].fin").value("2030-01-08T13:15:00Z"))
                    .andExpect(jsonPath("$.horarios[3].inicio").value("2030-01-08T13:45:00Z"))
                    .andExpect(jsonPath("$.horarios[4].inicio").value("2030-01-08T14:30:00Z"));

            // Empty day returns 200 OK with empty horarios
            mvc.perform(get("/api/public/disponibilidad")
                            .param("motivoId", fixture.motivo().toString())
                            .param("desde", "2030-01-09")
                            .param("hasta", "2030-01-09"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.horarios").isEmpty());

            // Non-existent motivo returns 404 MOTIVO_NOT_FOUND
            mvc.perform(get("/api/public/disponibilidad")
                            .param("motivoId", UUID.randomUUID().toString()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("MOTIVO_NOT_FOUND"));

            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM turno", Long.class)).isEqualTo(turnsBefore);
        }
    }
}
