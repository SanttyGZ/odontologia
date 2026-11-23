package dev.odontologia.gestion;

import java.time.Instant;
import java.util.UUID;

public interface AvisosRecuperacion {
    void preparar(UUID evento, UUID turno, int revision, UUID credencial, String destinatario, Instant ahora);
}
