package dev.odontologia.comunicaciones;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import dev.odontologia.gestion.AvisosRecuperacion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
final class NotificacionesRecuperacion implements AvisosRecuperacion {
    private final JdbcTemplate jdbc;

    NotificacionesRecuperacion(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void preparar(UUID evento, UUID turno, int revision, UUID credencial, String destinatario, Instant ahora) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("La recuperación requiere una transacción coordinada");
        }
        var fecha = ahora.atOffset(ZoneOffset.UTC);
        jdbc.update("""
                UPDATE intencion_notificacion n
                SET credencial_id = ?, destinatario = ?, despacho_id = n.id
                FROM credencial_gestion c
                WHERE n.credencial_id = c.id AND c.revocada_en IS NOT NULL
                    AND n.turno_id = ? AND n.revision = ? AND n.tipo = 'RECORDATORIO'
                    AND n.elegible AND n.vence_en > ?
                    AND NOT EXISTS (SELECT 1 FROM intento_notificacion i WHERE i.despacho_id = n.despacho_id)
                """, credencial, destinatario, turno, revision, fecha);
        jdbc.update("""
                UPDATE intencion_notificacion n SET elegible = false
                FROM credencial_gestion c
                WHERE n.credencial_id = c.id AND c.revocada_en IS NOT NULL AND n.turno_id = ? AND n.elegible
                """, turno);
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO intencion_notificacion
                  (id, evento_id, turno_id, revision, tipo, destinatario, credencial_id,
                   programada_en, vence_en, despacho_id, recuperacion)
                SELECT ?, ?, t.id, ?, 'CONFIRMACION', ?, ?, ?, t.fin, ?, true
                FROM turno t WHERE t.id = ?
                """, id, evento, revision, destinatario, credencial, fecha, id, turno);
    }
}
