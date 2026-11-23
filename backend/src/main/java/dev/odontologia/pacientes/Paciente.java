package dev.odontologia.pacientes;

import java.util.UUID;

public record Paciente(UUID id, UUID consultorioId, String nombre, String apellido, String email, String telefono,
                       String dni) {
}
