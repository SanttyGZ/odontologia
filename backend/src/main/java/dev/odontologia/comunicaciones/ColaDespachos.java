package dev.odontologia.comunicaciones;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.InstantSource;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

public final class ColaDespachos {
    public record Pendiente(UUID agenda, UUID despacho) {}

    private final JdbcTemplate jdbc;

    public ColaDespachos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void procesar(InstantSource reloj, Consumer<Pendiente> procesador) {
        jdbc.execute((ConnectionCallback<Void>) conexion -> {
            if (!adquirirExclusion(conexion)) {
                return null;
            }
            try {
                procesarVigentes(reloj, procesador);
            } finally {
                liberarExclusion(conexion);
            }
            return null;
        });
    }

    // Exclusión del emisor por sesión. No toma el bloqueo de escritura de la agenda.
    private static boolean adquirirExclusion(Connection conexion) throws SQLException {
        try (var consulta = conexion.prepareStatement("SELECT pg_try_advisory_lock(110711, 11)");
                var resultado = consulta.executeQuery()) {
            return resultado.next() && resultado.getBoolean(1);
        }
    }

    private static void liberarExclusion(Connection conexion) throws SQLException {
        try (var consulta = conexion.prepareStatement("SELECT pg_advisory_unlock(110711, 11)")) {
            consulta.execute();
        } catch (SQLException fallo) {
            conexion.abort(Runnable::run);
            throw fallo;
        }
    }

    private void procesarVigentes(InstantSource reloj, Consumer<Pendiente> procesador) {
        var ahora = reloj.instant().atOffset(ZoneOffset.UTC);
        jdbc.update("""
                UPDATE intento_notificacion
                SET resultado = 'INCIERTO', terminado_en = GREATEST(autorizado_en, ?)
                WHERE resultado IS NULL AND lower(destinatario) = ?
                """, ahora, GmailPruebas.BUZON);
        var pendientes = jdbc.query("""
                WITH listos AS (
                    SELECT n.despacho_id, t.agenda_id, min(n.programada_en) AS prevista
                    FROM intencion_notificacion n
                    JOIN turno t ON t.id = n.turno_id
                    LEFT JOIN credencial_gestion c ON c.id = n.credencial_id
                    GROUP BY n.despacho_id, t.agenda_id
                    HAVING bool_and(n.elegible AND n.revision = t.revision
                        AND n.programada_en <= ? AND n.vence_en > ?
                        AND lower(n.destinatario) = ?
                        AND (t.estado = 'CONFIRMADO' OR n.tipo = 'CAMBIO')
                        AND (c.id IS NULL OR (c.revocada_en IS NULL AND c.turno_id = t.id)))
                )
                SELECT l.agenda_id, l.despacho_id
                FROM listos l
                CROSS JOIN LATERAL (
                    SELECT count(*) AS cantidad, max(terminado_en) AS ultimo,
                        bool_and(resultado IS NOT DISTINCT FROM 'TRANSITORIO') AS recuperable
                    FROM intento_notificacion
                    WHERE despacho_id = l.despacho_id
                ) historial
                WHERE historial.cantidad = 0
                   OR (historial.recuperable AND (
                          (historial.cantidad = 1 AND historial.ultimo + interval '1 minute' <= ?)
                       OR (historial.cantidad = 2 AND historial.ultimo + interval '5 minutes' <= ?)))
                ORDER BY l.prevista, l.despacho_id
                LIMIT 8
                """, (rs, fila) -> new Pendiente(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)),
                ahora, ahora, GmailPruebas.BUZON, ahora, ahora);
        pendientes.forEach(procesador);
    }
}
