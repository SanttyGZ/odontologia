package dev.odontologia.disponibilidad;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import dev.odontologia.agenda.BloqueoAgenda;
import dev.odontologia.agenda.FranjaSemanal;
import dev.odontologia.agenda.Turno;
import dev.odontologia.disponibilidad.AvailabilityDtos.FranjaInput;

final class AvailabilityRules {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{6,30}$");
    private static final Pattern DNI_PATTERN = Pattern.compile("^[A-Za-z0-9]{6,20}$");

    record Slot(Instant inicio, Instant fin) {
    }

    public record ValidatedPatient(String nombre, String apellido, String email, String telefono, String dni) {
    }

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

    static List<Slot> availableSlots(int durationMinutes, List<FranjaSemanal> schedule,
                                     List<BloqueoAgenda> blocks, List<Turno> confirmedTurns,
                                     LocalDate fromDate, LocalDate toDate, Instant now, ZoneId zone) {
        validateDuration(durationMinutes);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw AvailabilityException.validation();
        }
        Instant minStart = now.plus(24, ChronoUnit.HOURS);
        LocalDate minAllowedDate = minStart.atZone(zone).toLocalDate();
        LocalDate maxAllowedDate = now.atZone(zone).toLocalDate().plusDays(30);

        LocalDate effectiveFrom = (fromDate == null || fromDate.isBefore(minAllowedDate)) ? minAllowedDate : fromDate;
        LocalDate effectiveTo = (toDate == null || toDate.isAfter(maxAllowedDate)) ? maxAllowedDate : toDate;
        if (effectiveFrom.isAfter(effectiveTo)) {
            return List.of();
        }

        List<Slot> slots = new ArrayList<>();
        for (LocalDate date = effectiveFrom; !date.isAfter(effectiveTo); date = date.plusDays(1)) {
            List<FranjaSemanal> attention = day(schedule, date.getDayOfWeek(), FranjaSemanal.Tipo.ATENCION);
            List<FranjaSemanal> pauses = day(schedule, date.getDayOfWeek(), FranjaSemanal.Tipo.PAUSA);
            for (FranjaSemanal range : attention) {
                LocalDateTime rangeEnd = LocalDateTime.of(date, range.horaFin());
                LocalDateTime startLocal = LocalDateTime.of(date, range.horaInicio());
                for (LocalDateTime endLocal = startLocal.plusMinutes(durationMinutes);
                     !endLocal.isAfter(rangeEnd);
                     startLocal = startLocal.plusMinutes(15), endLocal = startLocal.plusMinutes(durationMinutes)) {
                    Instant startInstant = startLocal.atZone(zone).toInstant();
                    if (startInstant.isBefore(minStart)) {
                        continue;
                    }
                    LocalDateTime candidateStart = startLocal;
                    LocalDateTime candidateEnd = endLocal;
                    boolean hitsPause = pauses.stream().anyMatch(pause -> overlaps(candidateStart, candidateEnd,
                            LocalDateTime.of(candidateStart.toLocalDate(), pause.horaInicio()),
                            LocalDateTime.of(candidateStart.toLocalDate(), pause.horaFin())));
                    if (hitsPause) {
                        continue;
                    }
                    Instant endInstant = endLocal.atZone(zone).toInstant();
                    boolean hitsBlock = blocks.stream()
                            .anyMatch(block -> overlaps(startInstant, endInstant, block.inicio(), block.fin()));
                    if (hitsBlock) {
                        continue;
                    }
                    boolean hitsTurn = confirmedTurns.stream()
                            .filter(turn -> turn.estado() == Turno.Estado.CONFIRMADO)
                            .anyMatch(turn -> overlaps(startInstant, endInstant, turn.inicio(), turn.fin()));
                    if (!hitsTurn) {
                        slots.add(new Slot(startInstant, endInstant));
                    }
                }
            }
        }
        return List.copyOf(slots);
    }

    public static ValidatedPatient validatedPatient(String nombre, String apellido, String email,
                                                    String telefono, String dni) {
        String cleanNombre = cleanText(nombre, 100);
        String cleanApellido = cleanText(apellido, 100);
        String cleanEmail = cleanText(email, 254).toLowerCase(Locale.ROOT);
        String cleanTelefono = cleanText(telefono, 30);
        String cleanDni = cleanText(dni, 20);
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()
                || !PHONE_PATTERN.matcher(cleanTelefono).matches()
                || cleanTelefono.replaceAll("[^0-9]", "").length() < 6
                || !DNI_PATTERN.matcher(cleanDni).matches()) {
            throw AvailabilityException.validation();
        }
        return new ValidatedPatient(cleanNombre, cleanApellido, cleanEmail, cleanTelefono, cleanDni);
    }

    static boolean isSlotAvailable(Instant inicio, int durationMinutes, List<FranjaSemanal> schedule,
                                   List<BloqueoAgenda> blocks, List<Turno> confirmedTurns,
                                   Instant now, ZoneId zone) {
        if (inicio == null) {
            return false;
        }
        validateDuration(durationMinutes);
        Instant startInstant = inicio.truncatedTo(ChronoUnit.MICROS);
        if (!startInstant.equals(inicio) || startInstant.getEpochSecond() % 60 != 0) {
            return false;
        }
        Instant minStart = now.plus(24, ChronoUnit.HOURS);
        if (startInstant.isBefore(minStart)) {
            return false;
        }
        LocalDate maxAllowedDate = now.atZone(zone).toLocalDate().plusDays(30);
        var startZoned = startInstant.atZone(zone);
        if (startZoned.toLocalDate().isAfter(maxAllowedDate)) {
            return false;
        }
        LocalDateTime startLocal = startZoned.toLocalDateTime();
        LocalDateTime endLocal = startLocal.plusMinutes(durationMinutes);
        if (!startLocal.toLocalDate().equals(endLocal.toLocalDate())) {
            return false;
        }
        LocalDate date = startLocal.toLocalDate();
        List<FranjaSemanal> attention = day(schedule, date.getDayOfWeek(), FranjaSemanal.Tipo.ATENCION);
        boolean anchoredInOneAttention = attention.stream().anyMatch(range -> {
            LocalDateTime rangeStart = LocalDateTime.of(date, range.horaInicio());
            LocalDateTime rangeEnd = LocalDateTime.of(date, range.horaFin());
            if (startLocal.isBefore(rangeStart) || endLocal.isAfter(rangeEnd)) {
                return false;
            }
            long minutesFromStart = java.time.Duration.between(rangeStart, startLocal).toMinutes();
            return minutesFromStart % 15 == 0;
        });
        if (!anchoredInOneAttention) {
            return false;
        }
        List<FranjaSemanal> pauses = day(schedule, date.getDayOfWeek(), FranjaSemanal.Tipo.PAUSA);
        if (pauses.stream().anyMatch(pause -> overlaps(startLocal, endLocal,
                LocalDateTime.of(date, pause.horaInicio()), LocalDateTime.of(date, pause.horaFin())))) {
            return false;
        }
        Instant endInstant = endLocal.atZone(zone).toInstant();
        if (blocks.stream().anyMatch(block -> overlaps(startInstant, endInstant, block.inicio(), block.fin()))) {
            return false;
        }
        return confirmedTurns.stream()
                .filter(turn -> turn.estado() == Turno.Estado.CONFIRMADO)
                .noneMatch(turn -> overlaps(startInstant, endInstant, turn.inicio(), turn.fin()));
    }

    private static String cleanText(String value, int maxLength) {
        if (value == null) {
            throw AvailabilityException.validation();
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > maxLength) {
            throw AvailabilityException.validation();
        }
        return trimmed;
    }

    private static boolean overlaps(Instant aStart, Instant aEnd, Instant bStart, Instant bEnd) {
        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
    }

    private static boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd,
                                    LocalDateTime bStart, LocalDateTime bEnd) {
        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
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
