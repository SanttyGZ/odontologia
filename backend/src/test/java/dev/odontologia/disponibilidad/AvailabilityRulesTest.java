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
    void keepsAdjacentAttentionConfigurableButRequiresOneRangeToContainAConfirmedTurn() {
        var adjacent = AvailabilityRules.validatedSchedule(List.of(
                row(1, "09:00", "10:00", "ATENCION"),
                row(1, "10:00", "11:00", "ATENCION")), AGENDA);
        var crossingBoundary = turn("2030-01-07T09:30:00Z", "2030-01-07T10:30:00Z");
        assertThat(AvailabilityRules.coversConfirmedTurn(crossingBoundary, adjacent, ZoneId.of("Etc/UTC")))
                .isFalse();

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

    @Test
    void computesAnchored15MinuteSlotsExcludingContiguousBoundariesPausesBlocksAndConfirmedTurns() {
        ZoneId zone = ZoneId.of("America/Argentina/Mendoza");
        // Monday 2030-01-07 in Mendoza (UTC-3):
        // Range 1: 09:10 - 10:10 (45m slots -> only 09:10-09:55 and 09:25-10:10)
        // Range 2 (contiguous): 10:10 - 11:10 (does not combine with Range 1)
        // Pause in Range 2: 10:25 - 10:40
        var schedule = AvailabilityRules.validatedSchedule(List.of(
                row(1, "09:10", "10:10", "ATENCION"),
                row(1, "10:10", "11:10", "ATENCION"),
                row(1, "10:25", "10:40", "PAUSA")), AGENDA);
        // Confirmed turn 09:25-09:55 local (12:25-12:55Z) -> blocks 09:10-09:55 and 09:25-10:10
        var confirmed = List.of(turn("2030-01-07T12:25:00Z", "2030-01-07T12:55:00Z"));
        // Block 10:55-11:10 local (13:55-14:10Z) -> blocks 10:25-11:10
        var blocks = List.of(new dev.odontologia.agenda.BloqueoAgenda(UUID.randomUUID(), AGENDA,
                Instant.parse("2030-01-07T13:55:00Z"), Instant.parse("2030-01-07T14:10:00Z"), null));

        var slots = AvailabilityRules.availableSlots(45, schedule, blocks, confirmed,
                java.time.LocalDate.of(2030, 1, 7), java.time.LocalDate.of(2030, 1, 7),
                Instant.parse("2030-01-05T12:00:00Z"), zone);
        assertThat(slots).isEmpty();

        // With 30-min duration and no confirmed turn, 09:10-09:40, 09:25-09:55, 09:40-10:10 are free,
        // 09:55-10:25 is excluded (crosses 10:10 boundary), and 10:10-10:40 is excluded (pause 10:25-10:40).
        // 10:40-11:10 is excluded by block 10:55-11:10.
        var thirtyMinSlots = AvailabilityRules.availableSlots(30, schedule, blocks, List.of(),
                java.time.LocalDate.of(2030, 1, 7), java.time.LocalDate.of(2030, 1, 7),
                Instant.parse("2030-01-05T12:00:00Z"), zone);
        assertThat(thirtyMinSlots).containsExactly(
                new AvailabilityRules.Slot(Instant.parse("2030-01-07T12:10:00Z"), Instant.parse("2030-01-07T12:40:00Z")),
                new AvailabilityRules.Slot(Instant.parse("2030-01-07T12:25:00Z"), Instant.parse("2030-01-07T12:55:00Z")),
                new AvailabilityRules.Slot(Instant.parse("2030-01-07T12:40:00Z"), Instant.parse("2030-01-07T13:10:00Z")));
    }

    @Test
    void enforcesInclusive24HourLowerBoundAndInclusive30CalendarDayUpperBound() {
        ZoneId zone = ZoneId.of("America/Argentina/Mendoza");
        // now = 2030-01-07T12:00:00Z (09:00 local Monday).
        // 24h later = 2030-01-08T12:00:00Z (09:00 local Tuesday).
        // 30 calendar days later = 2030-02-06 (Wednesday).
        var schedule = AvailabilityRules.validatedSchedule(List.of(
                row(2, "08:45", "09:45", "ATENCION"),
                row(3, "08:45", "09:45", "ATENCION")), AGENDA);
        Instant now = Instant.parse("2030-01-07T12:00:00Z");

        var slots = AvailabilityRules.availableSlots(30, schedule, List.of(), List.of(),
                java.time.LocalDate.of(2030, 1, 8), java.time.LocalDate.of(2030, 2, 10), now, zone);

        // On 2030-01-08: 08:45 local (11:45Z) < now+24h -> excluded; 09:00 local (12:00Z) == now+24h -> included.
        assertThat(slots).first().isEqualTo(
                new AvailabilityRules.Slot(Instant.parse("2030-01-08T12:00:00Z"), Instant.parse("2030-01-08T12:30:00Z")));
        // Last day included must be 2030-02-06 (exact 30th calendar day, not 2030-02-12).
        assertThat(slots).last().isEqualTo(
                new AvailabilityRules.Slot(Instant.parse("2030-02-06T12:15:00Z"), Instant.parse("2030-02-06T12:45:00Z")));
    }

    @Test
    void validatesAndNormalizesPatientFieldsAndRejectsMissingOrInvalidFormats() {
        var patient = AvailabilityRules.validatedPatient(" Ana ", " Perez ", "ANA@Example.COM ", " +54 261-555-1234 ",
                " 30123456 ");
        assertThat(patient.nombre()).isEqualTo("Ana");
        assertThat(patient.apellido()).isEqualTo("Perez");
        assertThat(patient.email()).isEqualTo("ana@example.com");
        assertThat(patient.telefono()).isEqualTo("+54 261-555-1234");
        assertThat(patient.dni()).isEqualTo("30123456");

        assertValidation(() -> AvailabilityRules.validatedPatient("", "Perez", "a@b.com", "2615551234", "30123456"));
        assertValidation(() -> AvailabilityRules.validatedPatient("Ana", " ", "a@b.com", "2615551234", "30123456"));
        assertValidation(() -> AvailabilityRules.validatedPatient("Ana", "Perez", "invalido", "2615551234", "30123456"));
        assertValidation(() -> AvailabilityRules.validatedPatient("Ana", "Perez", "a@b.com", "123", "30123456"));
        assertValidation(() -> AvailabilityRules.validatedPatient("Ana", "Perez", "a@b.com", "2615551234", "12 345"));
    }

    @Test
    void checksExactPointSlotAvailabilityAgainstWindowAnchorContiguousRangesAndOccupation() {
        ZoneId zone = ZoneId.of("America/Argentina/Mendoza");
        Instant now = Instant.parse("2030-01-07T12:00:00Z"); // Monday 09:00 local
        var schedule = AvailabilityRules.validatedSchedule(List.of(
                row(2, "09:10", "10:10", "ATENCION"),
                row(2, "10:10", "11:10", "ATENCION"),
                row(2, "10:25", "10:40", "PAUSA")), AGENDA);
        var confirmed = List.of(turn("2030-01-08T12:25:00Z", "2030-01-08T12:55:00Z")); // 09:25-09:55 local

        // Exact anchored start 09:10-09:40 local (12:10Z) hits confirmed 09:25-09:55 -> false
        assertThat(AvailabilityRules.isSlotAvailable(Instant.parse("2030-01-08T12:10:00Z"), 30,
                schedule, List.of(), confirmed, now, zone)).isFalse();
        // Contiguous after confirmed: 09:55-10:25 local crosses 10:10 boundary -> false
        assertThat(AvailabilityRules.isSlotAvailable(Instant.parse("2030-01-08T12:55:00Z"), 30,
                schedule, List.of(), confirmed, now, zone)).isFalse();
        // Unanchored 09:15 local -> false
        assertThat(AvailabilityRules.isSlotAvailable(Instant.parse("2030-01-08T12:15:00Z"), 30,
                schedule, List.of(), List.of(), now, zone)).isFalse();
        // Valid free anchored slot 10:40-11:10 local (13:40Z) -> true
        assertThat(AvailabilityRules.isSlotAvailable(Instant.parse("2030-01-08T13:40:00Z"), 30,
                schedule, List.of(), confirmed, now, zone)).isTrue();

        // Off-grid seconds or microseconds on a minute-aligned range (09:10:00) -> false
        assertThat(AvailabilityRules.isSlotAvailable(Instant.parse("2030-01-08T13:40:30Z"), 30,
                schedule, List.of(), confirmed, now, zone)).isFalse();
        assertThat(AvailabilityRules.isSlotAvailable(Instant.parse("2030-01-08T13:40:00.123456Z"), 30,
                schedule, List.of(), confirmed, now, zone)).isFalse();

        // Range with seconds (09:00:30) and microseconds (10:00:00.250000) -> accepts exact grid only
        var preciseSchedule = AvailabilityRules.validatedSchedule(List.of(
                row(2, "09:00:30", "10:00:00", "ATENCION"),
                row(2, "10:00:00.250000", "11:00:00", "ATENCION")), AGENDA);
        assertThat(AvailabilityRules.isSlotAvailable(Instant.parse("2030-01-08T12:00:30Z"), 30,
                preciseSchedule, List.of(), List.of(), now, zone)).isTrue();
        assertThat(AvailabilityRules.isSlotAvailable(Instant.parse("2030-01-08T12:15:30Z"), 30,
                preciseSchedule, List.of(), List.of(), now, zone)).isTrue();
        assertThat(AvailabilityRules.isSlotAvailable(Instant.parse("2030-01-08T12:16:00Z"), 30,
                preciseSchedule, List.of(), List.of(), now, zone)).isFalse();
        assertThat(AvailabilityRules.isSlotAvailable(Instant.parse("2030-01-08T13:15:00.250000Z"), 30,
                preciseSchedule, List.of(), List.of(), now, zone)).isTrue();
        assertThat(AvailabilityRules.isSlotAvailable(Instant.parse("2030-01-08T13:15:00.123456Z"), 30,
                preciseSchedule, List.of(), List.of(), now, zone)).isFalse();
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
