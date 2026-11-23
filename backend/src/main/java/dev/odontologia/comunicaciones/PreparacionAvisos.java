package dev.odontologia.comunicaciones;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public final class PreparacionAvisos {
    public record Evento(UUID id, UUID turno, int revision, String destinatario, Instant ocurrido) {}
    public static final class ConflictoEvento extends IllegalArgumentException {
        ConflictoEvento() {
            super("El evento ya existe con otros datos o su revisión no está vigente");
        }
    }

    private final JdbcTemplate jdbc;

    PreparacionAvisos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void reserva(Evento evento, Instant inicio, UUID credencial) {
        exigirCoordinacion();
        if (!registrarOrigen(evento, "RESERVA", inicio, credencial)) {
            return;
        }
        UUID confirmacion = UUID.randomUUID();
        Instant programada = inicio.minus(Duration.ofHours(24));
        insertar(evento, "CONFIRMACION", credencial, evento.ocurrido(), inicio, confirmacion);
        if (!programada.isBefore(evento.ocurrido())) {
            insertar(evento, "RECORDATORIO", credencial, programada, programada.plus(Duration.ofHours(2)),
                    programada.equals(evento.ocurrido()) ? confirmacion : UUID.randomUUID());
        }
    }

    public void cambio(Evento evento, Instant nuevoInicio, UUID credencial) {
        exigirCoordinacion();
        UUID referencia = nuevoInicio == null ? null : credencial;
        if (!registrarOrigen(evento, "CAMBIO", nuevoInicio, referencia)) {
            return;
        }
        jdbc.update("""
                UPDATE intencion_notificacion SET elegible = false
                WHERE turno_id = ? AND revision < ? AND elegible
                """, evento.turno(), evento.revision());
        insertar(evento, "CAMBIO", nuevoInicio == null ? null : credencial, evento.ocurrido(),
                evento.ocurrido().plus(Duration.ofHours(24)), UUID.randomUUID());
        if (nuevoInicio != null) {
            Instant programada = nuevoInicio.minus(Duration.ofHours(24));
            if (!programada.isBefore(evento.ocurrido())) {
                insertar(evento, "RECORDATORIO", credencial, programada,
                        programada.plus(Duration.ofHours(2)), UUID.randomUUID());
            }
        }
    }

    private boolean registrarOrigen(Evento evento, String clase, Instant inicio, UUID credencial) {
        var fecha = evento.ocurrido().atOffset(ZoneOffset.UTC);
        var nuevoInicio = inicio == null ? null : inicio.atOffset(ZoneOffset.UTC);
        int agregado = jdbc.update("""
                INSERT INTO origen_notificacion (id, turno_id, revision, clase, destinatario,
                    ocurrido_en, nuevo_inicio, credencial_id)
                SELECT ?, t.id, ?, ?, ?, ?, ?, ? FROM turno t
                WHERE t.id = ? AND t.revision = ?
                ON CONFLICT (id) DO NOTHING
                """, evento.id(), evento.revision(), clase, evento.destinatario(), fecha, nuevoInicio,
                credencial, evento.turno(), evento.revision());
        if (agregado == 1) {
            return true;
        }
        if (!Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM origen_notificacion
                    WHERE id = ? AND turno_id = ? AND revision = ? AND clase = ? AND destinatario = ?
                        AND ocurrido_en = ? AND nuevo_inicio IS NOT DISTINCT FROM CAST(? AS timestamptz)
                        AND credencial_id IS NOT DISTINCT FROM CAST(? AS uuid))
                """, Boolean.class, evento.id(), evento.turno(), evento.revision(), clase, evento.destinatario(),
                fecha, nuevoInicio, credencial))) {
            throw new ConflictoEvento();
        }
        return false;
    }

    private void insertar(Evento evento, String tipo, UUID credencial, Instant programada, Instant vence,
            UUID despacho) {
        jdbc.update("""
                INSERT INTO intencion_notificacion
                    (id, evento_id, turno_id, revision, tipo, destinatario, credencial_id,
                     programada_en, vence_en, despacho_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), evento.id(), evento.turno(), evento.revision(), tipo,
                evento.destinatario(), credencial, programada.atOffset(ZoneOffset.UTC),
                vence.atOffset(ZoneOffset.UTC), despacho);
    }

    private static void exigirCoordinacion() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("La preparación de avisos requiere una escritura coordinada");
        }
    }
}
