package dev.odontologia.comunicaciones;

import java.time.Instant;
import java.time.InstantSource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.odontologia.agenda.AgendaWriteCoordinator;
import dev.odontologia.gestion.CredencialesGestion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public final class AutorizacionDespacho {
    public record Intento(UUID id, UUID turno, int revision, String destinatario, UUID credencial,
            Instant inicio, Instant fin, String estado, List<String> tipos, boolean recuperacion) {}
    public enum Resultado { ACEPTADA_SMTP, TRANSITORIO, PERMANENTE, INCIERTO }

    private final JdbcTemplate jdbc;
    private final AgendaWriteCoordinator coordinador;
    private final CredencialesGestion credenciales;

    AutorizacionDespacho(JdbcTemplate jdbc, AgendaWriteCoordinator coordinador, CredencialesGestion credenciales) {
        this.jdbc = jdbc;
        this.coordinador = coordinador;
        this.credenciales = credenciales;
    }

    public Optional<Intento> autorizar(UUID agenda, UUID despacho, InstantSource reloj) {
        return coordinador.write(agenda, () -> autorizarEnTransaccion(agenda, despacho, reloj.instant()));
    }

    private Optional<Intento> autorizarEnTransaccion(UUID agenda, UUID despacho, Instant ahora) {
        UUID intento = UUID.randomUUID();
        var fecha = ahora.atOffset(ZoneOffset.UTC);
        return jdbc.query("""
                INSERT INTO intento_notificacion
                    (id, despacho_id, turno_id, revision, destinatario, credencial_id, autorizado_en,
                     inicio_turno, fin_turno, estado_turno, tipos, recuperacion)
                SELECT ?, n.despacho_id, n.turno_id, n.revision, n.destinatario, n.credencial_id, ?,
                    t.inicio, t.fin, t.estado,
                    ARRAY(SELECT g.tipo FROM intencion_notificacion g
                          WHERE g.despacho_id = n.despacho_id ORDER BY g.tipo),
                    n.recuperacion
                FROM intencion_notificacion n JOIN turno t ON t.id = n.turno_id
                WHERE n.despacho_id = ? AND t.agenda_id = ? AND n.revision = t.revision
                    AND (t.estado = 'CONFIRMADO' OR n.tipo = 'CAMBIO')
                    AND NOT EXISTS (
                        SELECT 1 FROM intencion_notificacion grupo
                        LEFT JOIN credencial_gestion c ON c.id = grupo.credencial_id
                        WHERE grupo.despacho_id = n.despacho_id
                          AND (NOT grupo.elegible OR grupo.programada_en > ? OR grupo.vence_en <= ?
                            OR grupo.turno_id <> n.turno_id OR grupo.revision <> n.revision
                            OR grupo.destinatario <> n.destinatario
                            OR grupo.credencial_id IS DISTINCT FROM n.credencial_id
                            OR (c.id IS NOT NULL AND (c.revocada_en IS NOT NULL OR c.turno_id <> n.turno_id)))
                    )
                    AND NOT EXISTS (
                        SELECT 1 FROM intento_notificacion i WHERE i.despacho_id = n.despacho_id
                            AND i.resultado IS DISTINCT FROM 'TRANSITORIO'
                    )
                    AND (
                        SELECT CASE count(*)
                            WHEN 0 THEN true
                            WHEN 1 THEN max(i.terminado_en) + interval '1 minute' <= ?
                            WHEN 2 THEN max(i.terminado_en) + interval '5 minutes' <= ?
                            ELSE false END
                        FROM intento_notificacion i WHERE i.despacho_id = n.despacho_id
                    )
                LIMIT 1
                RETURNING id, turno_id, revision, destinatario, credencial_id, inicio_turno, fin_turno,
                    estado_turno, array_to_string(tipos, ',') AS tipos, recuperacion
                """, (rs, row) -> new Intento(rs.getObject("id", UUID.class), rs.getObject("turno_id", UUID.class),
                    rs.getInt("revision"), rs.getString("destinatario"), rs.getObject("credencial_id", UUID.class),
                    rs.getObject("inicio_turno", OffsetDateTime.class).toInstant(),
                    rs.getObject("fin_turno", OffsetDateTime.class).toInstant(), rs.getString("estado_turno"),
                    List.of(rs.getString("tipos").split(",")), rs.getBoolean("recuperacion")),
                intento, fecha, despacho, agenda, fecha, fecha, fecha, fecha).stream().findFirst();
    }

    public Optional<String> recuperarEnlace(UUID intento) {
        return jdbc.query("""
                SELECT turno_id, credencial_id FROM intento_notificacion
                WHERE id = ? AND resultado IS NULL AND credencial_id IS NOT NULL
                """, (rs, row) -> new Referencia(rs.getObject("turno_id", UUID.class),
                    rs.getObject("credencial_id", UUID.class)), intento).stream().findFirst()
                .flatMap(r -> credenciales.recuperarParaIntentoAutorizado(r.credencial(), r.turno()));
    }

    private record Referencia(UUID turno, UUID credencial) {}

    public boolean registrar(UUID intento, Resultado resultado, Instant ahora) {
        var fecha = ahora.atOffset(ZoneOffset.UTC);
        return jdbc.update("""
                UPDATE intento_notificacion SET resultado = ?, terminado_en = ?
                WHERE id = ? AND resultado IS NULL AND autorizado_en <= ?
                """, resultado.name(), fecha, intento, fecha) == 1;
    }
}
