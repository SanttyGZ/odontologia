package dev.odontologia.gestion;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import dev.odontologia.agenda.AgendaWriteCoordinator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
final class RecuperacionEnlace {
    enum Metodo { DOCUMENTO_PRESENCIAL, LLAMADA_A_NUMERO_VERIFICADO }

    private final JdbcTemplate jdbc;
    private final AgendaWriteCoordinator coordinador;
    private final CredencialesGestion credenciales;
    private final ObjectProvider<AvisosRecuperacion> avisos;
    private final Clock clock;

    RecuperacionEnlace(JdbcTemplate jdbc, AgendaWriteCoordinator coordinador,
            CredencialesGestion credenciales, ObjectProvider<AvisosRecuperacion> avisos, Clock clock) {
        this.jdbc = jdbc;
        this.coordinador = coordinador;
        this.credenciales = credenciales;
        this.avisos = avisos;
        this.clock = clock;
    }

    UUID recuperar(UUID turno, String odontologo, String motivo, Metodo metodo, boolean verificacion,
            String emailCorregido) {
        if (!verificacion || metodo == null || motivo == null || motivo.isBlank() || motivo.length() > 120
                || (emailCorregido != null && !emailCorregido.matches("[^\\s@]{1,64}@[^\\s@]{1,189}"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        AvisosRecuperacion preparador = avisos.getIfAvailable();
        if (preparador == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
        UUID agenda = jdbc.query("SELECT agenda_id FROM turno WHERE id = ?",
                (rs, row) -> rs.getObject(1, UUID.class), turno).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return coordinador.write(agenda, () -> {
            var ahora = clock.instant();
            var fecha = ahora.atOffset(ZoneOffset.UTC);
            var destino = jdbc.query("""
                    SELECT t.revision, coalesce((
                        SELECT r.destinatario FROM recuperacion_enlace r
                        WHERE r.turno_id = t.id ORDER BY r.orden DESC LIMIT 1
                    ), p.email) AS email
                    FROM turno t JOIN paciente p ON p.id = t.paciente_id
                    WHERE t.id = ? AND t.agenda_id = ? AND t.estado = 'CONFIRMADO' AND t.fin > ?
                    """, (rs, row) -> new Destino(rs.getInt("revision"), rs.getString("email")),
                    turno, agenda, fecha).stream().findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            UUID evento = UUID.randomUUID();
            String destinatario = emailCorregido == null ? destino.email() : emailCorregido;
            credenciales.revocar(turno);
            UUID credencial = credenciales.emitir(turno);
            jdbc.update("""
                    INSERT INTO recuperacion_enlace (id, turno_id, odontologo, metodo, motivo, destinatario, realizada_en)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, evento, turno, odontologo, metodo.name(), motivo.strip(), destinatario, fecha);
            preparador.preparar(evento, turno, destino.revision(), credencial, destinatario, ahora);
            return evento;
        });
    }

    private record Destino(int revision, String email) {}
}
