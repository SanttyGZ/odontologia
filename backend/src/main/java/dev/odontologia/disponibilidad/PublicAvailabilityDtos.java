package dev.odontologia.disponibilidad;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PublicAvailabilityDtos {

    private PublicAvailabilityDtos() {
    }

    public record HorarioDisponibleResponse(Instant inicio, Instant fin) {
    }

    public record DisponibilidadResponse(UUID motivoId, int duracionMinutos, String zonaHoraria,
                                         List<HorarioDisponibleResponse> horarios) {
    }
}
