package dev.odontologia.disponibilidad;

import java.time.Instant;
import java.util.UUID;

public final class PublicBookingDtos {

    private PublicBookingDtos() {
    }

    public record ReservaInput(UUID motivoId, Instant inicio, String nombre, String apellido,
                               String email, String telefono, String dni) {
    }

    public record ReservaResponse(UUID turnoId, UUID motivoId, Instant inicio, Instant fin,
                                  int duracionReservadaMinutos, String estado, String notificacionEstado) {
    }
}
