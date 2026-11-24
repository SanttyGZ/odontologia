package dev.odontologia.disponibilidad;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import dev.odontologia.agenda.FranjaSemanal;

public final class AvailabilityDtos {

    private AvailabilityDtos() {
    }

    public record MotivoInput(String nombre, Integer duracionMinutos) {
    }

    public record MotivoResponse(UUID id, String nombre, int duracionMinutos) {
    }

    public record HorarioInput(List<FranjaInput> franjas) {
    }

    public record FranjaInput(UUID id, Integer diaSemana, LocalTime horaInicio, LocalTime horaFin, String tipo) {
    }

    public record HorarioResponse(List<FranjaResponse> franjas) {
    }

    public record FranjaResponse(UUID id, int diaSemana, LocalTime horaInicio, LocalTime horaFin,
                                 FranjaSemanal.Tipo tipo) {
    }

    public record BloqueoInput(Instant inicio, Instant fin, String descripcion) {
    }

    public record BloqueoResponse(UUID id, Instant inicio, Instant fin, String descripcion) {
    }
}