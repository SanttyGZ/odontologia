package dev.odontologia.disponibilidad;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class GestionTurnoDtos {

    private GestionTurnoDtos() {
    }

    public record Consulta(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                           String estado, List<String> acciones) {
        public Consulta {
            acciones = List.copyOf(acciones);
        }
    }

    public record Cancelacion(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                              String estado, String notificacionEstado) {
    }

    public record Reprogramacion(UUID turnoId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                                 String estado, String notificacionEstado) {
    }

    public record ReprogramacionInput(Instant nuevoInicio) {
    }

    public record ConsultaResultado(Consulta cuerpo, String etag) {
    }

    public record ReprogramacionResultado(Reprogramacion cuerpo, String etag) {
    }
}
