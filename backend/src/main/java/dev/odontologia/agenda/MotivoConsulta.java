package dev.odontologia.agenda;

import java.util.UUID;

public record MotivoConsulta(UUID id, UUID agendaId, String nombre, int duracionMinutos) {
}
