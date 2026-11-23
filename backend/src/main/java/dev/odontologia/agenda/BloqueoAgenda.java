package dev.odontologia.agenda;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record BloqueoAgenda(UUID id, UUID agendaId, Instant inicio, Instant fin, String descripcion) {

    public BloqueoAgenda {
        inicio = inicio.truncatedTo(ChronoUnit.MICROS);
        fin = fin.truncatedTo(ChronoUnit.MICROS);
    }
}
