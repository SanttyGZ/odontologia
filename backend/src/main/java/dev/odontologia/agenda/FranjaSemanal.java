package dev.odontologia.agenda;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record FranjaSemanal(UUID id, UUID agendaId, DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFin,
                           Tipo tipo) {

    public FranjaSemanal {
        horaInicio = horaInicio.truncatedTo(ChronoUnit.MICROS);
        horaFin = horaFin.truncatedTo(ChronoUnit.MICROS);
    }

    public enum Tipo {
        ATENCION, PAUSA
    }
}
