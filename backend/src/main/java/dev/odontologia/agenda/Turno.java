package dev.odontologia.agenda;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record Turno(UUID id, UUID agendaId, UUID pacienteId, UUID motivoId, Instant inicio, Instant fin,
                    int duracionReservadaMinutos, Estado estado, int revision, Instant creadoEn,
                    Instant canceladoEn) {

    public Turno {
        inicio = inicio.truncatedTo(ChronoUnit.MICROS);
        fin = fin.truncatedTo(ChronoUnit.MICROS);
        creadoEn = creadoEn.truncatedTo(ChronoUnit.MICROS);
        if (canceladoEn != null) {
            canceladoEn = canceladoEn.truncatedTo(ChronoUnit.MICROS);
        }
    }

    public enum Estado {
        CONFIRMADO, CANCELADO
    }
}
