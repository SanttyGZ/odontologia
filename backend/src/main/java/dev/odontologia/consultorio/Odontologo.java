package dev.odontologia.consultorio;

import java.util.UUID;

public record Odontologo(UUID id, UUID consultorioId, String nombreVisible) {
}
