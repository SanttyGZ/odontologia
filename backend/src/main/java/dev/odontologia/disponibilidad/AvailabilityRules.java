package dev.odontologia.disponibilidad;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import dev.odontologia.agenda.FranjaSemanal;
import dev.odontologia.agenda.Turno;
import dev.odontologia.disponibilidad.AvailabilityDtos.FranjaInput;

final class AvailabilityRules {

    private AvailabilityRules() {
    }

    static void validateMotivo(String nombre, Integer duracionMinutos) {
        if (nombre == null || nombre.isBlank() || duracionMinutos == null) {
            throw AvailabilityException.validation();
        }
        validateDuration(duracionMinutos);
    }

    static void validateBlock(Instant inicio, Instant fin) {
        if (inicio == null || fin == null
                || !inicio.truncatedTo(ChronoUnit.MICROS).isBefore(fin.truncatedTo(ChronoUnit.MICROS))) {
            throw AvailabilityException.validation();
        }
    }

    static void validateDuration(int duration) {
        if (duration < 15 || duration > 240 || duration % 15 != 0) {
            throw AvailabilityException.validation();
        }
    }

    static List<FranjaSemanal> validatedSchedule(List<FranjaInput> input, UUID agendaId) {
        if (input == null || agendaId == null) {
            throw AvailabilityException.validation();
        }
        Set<UUID> ids = new HashSet<>();
        List<FranjaSemanal> schedule = new ArrayList<>(input.size());
        for (FranjaInput row : input) {
            if (row == null || row.id() == null || row.diaSemana() == null || row.horaInicio() == null
                    || row.horaFin() == null || row.tipo() == null || !ids.add(row.id())) {
                throw AvailabilityException.validation();
            }
            DayOfWeek day;
            FranjaSemanal.Tipo type;
            try {
                day = DayOfWeek.of(row.diaSemana());
                type = FranjaSemanal.Tipo.valueOf(row.tipo());
            } catch (DateTimeException | IllegalArgumentException invalid) {
                throw AvailabilityException.validation();
            }
            FranjaSemanal range = new FranjaSemanal(row.id(), agendaId, day, row.horaInicio(), row.horaFin(), type);
            if (!range.horaInicio().isBefore(range.horaFin())) {
                throw AvailabilityException.validation();
            }
            schedule.add(range);
        }

        for (DayOfWeek day : DayOfWeek.values()) {
            List<FranjaSemanal> attention = day(schedule, day, FranjaSemanal.Tipo.ATENCION);
            List<FranjaSemanal> breaks = day(schedule, day, FranjaSemanal.Tipo.PAUSA);
            rejectOverlaps(attention);
            rejectOverlaps(breaks);
            for (FranjaSemanal pause : breaks) {
                long containers = attention.stream().filter(range -> contains(range, pause)).count();
                if (containers != 1) {
                    throw AvailabilityException.validation();
                }
            }
        }
        return List.copyOf(schedule);
    }

    static boolean coversConfirmedTurn(Turno turn, List<FranjaSemanal> schedule, ZoneId zone) {
        var start = turn.inicio().atZone(zone);
        var end = turn.fin().atZone(zone);
        LocalDate localDay = start.toLocalDate();
        if (!localDay.equals(end.toLocalDate())) {
            return false;
        }
        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();
        if (!startTime.isBefore(endTime)) {
            return false;
        }

        List<FranjaSemanal> daily = day(schedule, start.getDayOfWeek());
        boolean intersectsBreak = daily.stream()
                .filter(range -> range.tipo() == FranjaSemanal.Tipo.PAUSA)
                .anyMatch(pause -> pause.horaInicio().isBefore(endTime) && pause.horaFin().isAfter(startTime));
        if (intersectsBreak) {
            return false;
        }

        return daily.stream().filter(range -> range.tipo() == FranjaSemanal.Tipo.ATENCION)
                .anyMatch(range -> !startTime.isBefore(range.horaInicio()) && !endTime.isAfter(range.horaFin()));
    }

    private static List<FranjaSemanal> day(List<FranjaSemanal> schedule, DayOfWeek day) {
        return schedule.stream().filter(range -> range.diaSemana() == day).toList();
    }

    private static List<FranjaSemanal> day(List<FranjaSemanal> schedule, DayOfWeek day,
                                           FranjaSemanal.Tipo type) {
        return schedule.stream().filter(range -> range.diaSemana() == day && range.tipo() == type)
                .sorted(Comparator.comparing(FranjaSemanal::horaInicio)).toList();
    }

    private static void rejectOverlaps(List<FranjaSemanal> ranges) {
        for (int index = 1; index < ranges.size(); index++) {
            if (ranges.get(index).horaInicio().isBefore(ranges.get(index - 1).horaFin())) {
                throw AvailabilityException.validation();
            }
        }
    }

    private static boolean contains(FranjaSemanal outer, FranjaSemanal inner) {
        return !inner.horaInicio().isBefore(outer.horaInicio())
                && !inner.horaFin().isAfter(outer.horaFin());
    }
}
