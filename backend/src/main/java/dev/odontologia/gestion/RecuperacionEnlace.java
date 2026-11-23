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
            String emailConfirmado) {
        if (!verificacion || metodo == null || motivo == null || motivo.isBlank() || motivo.length() > 120
                || emailConfirmado == null || !emailConfirmado.matches("[^\\s@]{1,64}@[^\\s@]{1,189}")) {
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
            int revision = jdbc.query("""
                    SELECT revision FROM turno
                    WHERE id = ? AND agenda_id = ? AND estado = 'CONFIRMADO' AND fin > ?
                    """, (rs, row) -> rs.getInt("revision"),
                    turno, agenda, fecha).stream().findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            UUID evento = UUID.randomUUID();
            credenciales.revocar(turno);
            UUID credencial = credenciales.emitir(turno);
            jdbc.update("""
                    INSERT INTO recuperacion_enlace (id, turno_id, odontologo, metodo, motivo, destinatario, realizada_en)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, evento, turno, odontologo, metodo.name(), motivo.strip(), emailConfirmado, fecha);
            preparador.preparar(evento, turno, revision, credencial, emailConfirmado, ahora);
            return evento;
        });
    }

}
