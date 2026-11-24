package dev.odontologia;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import dev.odontologia.agenda.AgendaQueries;
import dev.odontologia.agenda.AgendaWriteCoordinator;
import dev.odontologia.agenda.BloqueoAgenda;
import dev.odontologia.agenda.FranjaSemanal;
import dev.odontologia.agenda.MotivoConsulta;
import dev.odontologia.disponibilidad.AvailabilityConfigurationService;
import dev.odontologia.disponibilidad.AvailabilityDtos.FranjaInput;
import dev.odontologia.disponibilidad.AvailabilityException;
import dev.odontologia.consultorio.ConsultorioQueries;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvailabilityConfigurationIntegrationTest {

    @Test
    void rejectsSchedulePauseAndPartialBlockConflictsWithoutChangingConfirmedTurns() {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            var service = service(db);
            var originalSchedule = List.of(attention(2, "12:00", "14:00"));
            service.replaceHorario(originalSchedule);
            CoreSchemaTest.insertTurn(db.jdbc, fixture, UUID.randomUUID(), CoreSchemaTest.START,
                    CoreSchemaTest.START.plusMinutes(30), "CONFIRMADO");
            var turnBefore = db.jdbc.queryForMap("SELECT * FROM turno WHERE agenda_id = ?", fixture.agenda());
            var scheduleBefore = new AgendaQueries(db.jdbc).franjas(fixture.agenda());

            assertThatThrownBy(() -> service.replaceHorario(List.of(attention(2, "13:15", "14:00"))))
                    .isInstanceOfSatisfying(AvailabilityException.class,
                            failure -> assertThat(failure.codigo()).isEqualTo("CONFIGURATION_CONFLICT"));
            assertThatThrownBy(() -> service.replaceHorario(List.of(
                    attention(2, "12:00", "14:00"), pause(2, "13:15", "13:20"))))
                    .isInstanceOfSatisfying(AvailabilityException.class,
                            failure -> assertThat(failure.codigo()).isEqualTo("CONFIGURATION_CONFLICT"));
            assertThatThrownBy(() -> service.createBloqueo(CoreSchemaTest.START.toInstant().plusSeconds(900),
                    CoreSchemaTest.START.toInstant().plusSeconds(2700), null))
                    .isInstanceOfSatisfying(AvailabilityException.class,
                            failure -> assertThat(failure.codigo()).isEqualTo("CONFIGURATION_CONFLICT"));

            assertThat(new AgendaQueries(db.jdbc).franjas(fixture.agenda())).containsExactlyElementsOf(scheduleBefore);
            assertThat(db.jdbc.queryForMap("SELECT * FROM turno WHERE agenda_id = ?", fixture.agenda()))
                    .isEqualTo(turnBefore);
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM bloqueo_agenda", Long.class)).isZero();

            service.updateMotivo(fixture.motivo(), "Motivo actualizado", 45);
            var turnAfter = db.jdbc.queryForMap("SELECT * FROM turno WHERE agenda_id = ?", fixture.agenda());
            assertThat(turnAfter.get("inicio")).isEqualTo(turnBefore.get("inicio"));
            assertThat(turnAfter.get("fin")).isEqualTo(turnBefore.get("fin"));
            assertThat(turnAfter.get("duracion_reservada_minutos")).isEqualTo(30);
            assertThat(db.jdbc.queryForObject("SELECT duracion_minutos FROM motivo_consulta WHERE id = ?",
                    Integer.class, fixture.motivo())).isEqualTo(45);
        }
    }

    @Test
    void invalidWeeklyReplacementPreservesTheEntirePersistedSchedule() {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            var service = service(db);
            service.replaceHorario(List.of(attention(2, "09:00", "12:00")));
            var before = new AgendaQueries(db.jdbc).franjas(fixture.agenda());

            assertThatThrownBy(() -> service.replaceHorario(List.of(
                    attention(2, "09:00", "12:00"), attention(2, "11:00", "13:00"))))
                    .isInstanceOfSatisfying(AvailabilityException.class,
                            failure -> assertThat(failure.codigo()).isEqualTo("VALIDATION_ERROR"));

            assertThat(new AgendaQueries(db.jdbc).franjas(fixture.agenda())).containsExactlyElementsOf(before);
        }
    }

    @Test
    void blockRemovalDeletesOnlyTheRequestedBlock() {
        try (var db = new TestDatabase()) {
            var fixture = CoreSchemaTest.fixture(db);
            var service = service(db);
            Instant start = CoreSchemaTest.START.toInstant().plusSeconds(86_400);
            BloqueoAgenda first = service.createBloqueo(start, start.plusSeconds(900), "primero");
            BloqueoAgenda second = service.createBloqueo(start.plusSeconds(1_800), start.plusSeconds(2_700), null);
            CoreSchemaTest.insertTurn(db.jdbc, fixture, UUID.randomUUID(), CoreSchemaTest.START,
                    CoreSchemaTest.START.plusMinutes(30), "CONFIRMADO");
            var turnBefore = db.jdbc.queryForMap("SELECT * FROM turno WHERE agenda_id = ?", fixture.agenda());

            service.deleteBloqueo(first.id());

            assertThat(service.bloqueos()).containsExactly(second);
            assertThat(db.jdbc.queryForMap("SELECT * FROM turno WHERE agenda_id = ?", fixture.agenda()))
                    .isEqualTo(turnBefore);
            assertThatThrownBy(() -> service.deleteBloqueo(first.id()))
                    .isInstanceOfSatisfying(AvailabilityException.class,
                            failure -> assertThat(failure.codigo()).isEqualTo("BLOQUEO_NOT_FOUND"));
        }
    }

    private static AvailabilityConfigurationService service(TestDatabase db) {
        return new AvailabilityConfigurationService(db.jdbc, new AgendaQueries(db.jdbc),
                new ConsultorioQueries(db.jdbc), new AgendaWriteCoordinator(db.jdbc,
                        new JdbcTransactionManager(db.dataSource())));
    }

    private static FranjaInput attention(int day, String start, String end) {
        return new FranjaInput(UUID.randomUUID(), day, LocalTime.parse(start), LocalTime.parse(end), "ATENCION");
    }

    private static FranjaInput pause(int day, String start, String end) {
        return new FranjaInput(UUID.randomUUID(), day, LocalTime.parse(start), LocalTime.parse(end), "PAUSA");
    }
}
