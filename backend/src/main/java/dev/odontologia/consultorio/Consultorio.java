package dev.odontologia.consultorio;

import java.time.ZoneId;
import java.util.UUID;

public record Consultorio(UUID id, String nombreVisible, ZoneId zonaHoraria) {
}
