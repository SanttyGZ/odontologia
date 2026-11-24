package dev.odontologia.disponibilidad;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import dev.odontologia.agenda.FranjaSemanal;
import dev.odontologia.agenda.Turno;
import dev.odontologia.disponibilidad.AvailabilityDtos.FranjaInput;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvailabilityRulesTest {

    private static final UUID AGENDA = UUID.randomUUID();

    @Test
    void acceptsContiguousAttentionAndBreakInsideExactlyOneAttentionRange() {
        var schedule = AvailabilityRules.validatedSchedule(List.of(
                row(1, "09:10", "12:00", "ATENCION"),
                row(1, "12:00", "13:00", "ATENCION"),
                row(1, "10:00", "10:15", "PAUSA")), AGENDA);

        assertThat(schedule).hasSize(3);
        assertThat(schedule.getFirst().horaInicio()).isEqualTo(LocalTime.parse("09:10"));
    }

    @Test
    void rejectsDuplicateAndOverlappingAttentionRanges() {
        var first = row(1, "09:00", "10:00", "ATENCION");
        assertValidation(() -> AvailabilityRules.validatedSchedule(List.of(first,
                row(1, "09:00", "10:00", "ATENCION")), AGENDA));
        assertValidation(() -> AvailabilityRules.validatedSchedule(List.of(first,
                row(1, "09:30", "10:30", "ATENCION")), AGENDA));
    }

    @Test
    void rejectsBreaksOutsideAttentionOrOverlappingAnotherBreak() {
        assertValidation(() -> AvailabilityRules.validatedSchedule(List.of(
                row(1, "09:00", "10:00", "ATENCION"),
                row(1, "10:00", "10:15", "PAUSA")), AGENDA));
        assertValidation(() -> AvailabilityRules.validatedSchedule(List.of(
                row(1, "09:00", "12:00", "ATENCION"),
                row(1, "10:00", "10:30", "PAUSA"),
                row(1, "10:15", "10:45", "PAUSA")), AGENDA));
    }

    @Test
    void rejectsCrossDayAndInvalidRows() {
        assertValidation(() -> AvailabilityRules.validatedSchedule(List.of(
                row(1, "23:00", "01:00", "ATENCION")), AGENDA));
        assertValidation(() -> AvailabilityRules.validatedSchedule(List.of(
                new FranjaInput(UUID.randomUUID(), 8, LocalTime.NOON, LocalTime.MAX, "ATENCION")), AGENDA));
        assertValidation(() -> AvailabilityRules.validatedSchedule(List.of(
                new FranjaInput(UUID.randomUUID(), 1, LocalTime.NOON, LocalTime.MAX, "CERRADO")), AGENDA));
    }

    @Test
    void rejectsBlankMotivesAndNonPositiveOrMissingBlocks() {
        assertValidation(() -> AvailabilityRules.validateMotivo("  ", 30));
        assertValidation(() -> AvailabilityRules.validateMotivo("Consulta", null));
        AvailabilityRules.validateBlock(Instant.EPOCH, Instant.EPOCH.plusSeconds(1));
        assertValidation(() -> AvailabilityRules.validateBlock(null, Instant.EPOCH));
        assertValidation(() -> AvailabilityRules.validateBlock(Instant.EPOCH, Instant.EPOCH));
        assertValidation(() -> AvailabilityRules.validateBlock(Instant.EPOCH, Instant.EPOCH.plusNanos(999)));
        assertValidation(() -> AvailabilityRules.validateBlock(Instant.EPOCH.plusSeconds(1), Instant.EPOCH));
    }

    @Test
    void acceptsOnlyConfiguredDurationMultiplesInInclusiveRange() {
        for (int duration : List.of(15, 30, 45, 240)) {
            AvailabilityRules.validateDuration(duration);
        }
        for (int duration : List.of(0, 14, 25, 241)) {
            assertValidation(() -> AvailabilityRules.validateDuration(duration));
        }
    }

    @Test
    void treatsAdjacentAttentionAsContinuousButNeverAllowsAConfirmedTurnAcrossAPause() {
        var adjacent = AvailabilityRules.validatedSchedule(List.of(
                row(1, "09:00", "10:00", "ATENCION"),
                row(1, "10:00", "11:00", "ATENCION")), AGENDA);
        var crossingBoundary = turn("2030-01-07T09:30:00Z", "2030-01-07T10:30:00Z");
        assertThat(AvailabilityRules.coversConfirmedTurn(crossingBoundary, adjacent, ZoneId.of("Etc/UTC")))
                .isTrue();

        var withBreak = AvailabilityRules.validatedSchedule(List.of(
                row(1, "09:00", "11:00", "ATENCION"),
                row(1, "10:00", "10:15", "PAUSA")), AGENDA);
        assertThat(AvailabilityRules.coversConfirmedTurn(crossingBoundary, withBreak, ZoneId.of("Etc/UTC")))
                .isFalse();
        assertThat(AvailabilityRules.coversConfirmedTurn(turn("2030-01-07T10:00:00Z", "2030-01-07T10:30:00Z"),
                withBreak, ZoneId.of("Etc/UTC"))).isFalse();
    }

    @Test
    void doesNotTreatAnIntervalCrossingLocalMidnightAsCoveredByAWeeklyRange() {
        var schedule = AvailabilityRules.validatedSchedule(List.of(
                row(1, "23:00", "23:59:00", "ATENCION"),
                row(2, "00:00", "01:00", "ATENCION")), AGENDA);
        assertThat(AvailabilityRules.coversConfirmedTurn(turn("2030-01-07T23:45:00Z", "2030-01-08T00:15:00Z"),
                schedule, ZoneId.of("Etc/UTC"))).isFalse();
    }

    private static FranjaInput row(int day, String start, String end, String type) {
        return new FranjaInput(UUID.randomUUID(), day, LocalTime.parse(start), LocalTime.parse(end), type);
    }

    private static Turno turn(String start, String end) {
        Instant inicio = Instant.parse(start);
        return new Turno(UUID.randomUUID(), AGENDA, UUID.randomUUID(), UUID.randomUUID(), inicio,
                Instant.parse(end), (int) java.time.Duration.between(inicio, Instant.parse(end)).toMinutes(),
                Turno.Estado.CONFIRMADO, 0, inicio.minusSeconds(60), null);
    }

    private static void assertValidation(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(AvailabilityException.class,
                failure -> assertThat(failure.codigo()).isEqualTo("VALIDATION_ERROR"));
    }
}
