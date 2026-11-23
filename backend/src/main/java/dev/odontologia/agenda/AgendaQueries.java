package dev.odontologia.agenda;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AgendaQueries {

    private static final String TURNOS = """
            SELECT id, agenda_id, paciente_id, motivo_id, inicio, fin, duracion_reservada_minutos,
                   estado, revision, creado_en, cancelado_en
            FROM turno WHERE agenda_id = ?
            """;

    private final JdbcTemplate jdbc;

    public AgendaQueries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<MotivoConsulta> findMotivo(UUID agendaId, UUID motivoId) {
        return DataAccessUtils.optionalResult(jdbc.query("""
                SELECT id, agenda_id, nombre, duracion_minutos
                FROM motivo_consulta WHERE agenda_id = ? AND id = ?
                """, (row, index) -> new MotivoConsulta(row.getObject("id", UUID.class),
                        row.getObject("agenda_id", UUID.class), row.getString("nombre"), row.getInt("duracion_minutos")),
                agendaId, motivoId));
    }

    public List<FranjaSemanal> franjas(UUID agendaId) {
        return jdbc.query("""
                SELECT id, agenda_id, dia_semana, hora_inicio, hora_fin, tipo
                FROM franja_semanal WHERE agenda_id = ? ORDER BY dia_semana, hora_inicio, id
                """, (row, index) -> new FranjaSemanal(row.getObject("id", UUID.class),
                        row.getObject("agenda_id", UUID.class), DayOfWeek.of(row.getInt("dia_semana")),
                        row.getObject("hora_inicio", LocalTime.class), row.getObject("hora_fin", LocalTime.class),
                        FranjaSemanal.Tipo.valueOf(row.getString("tipo"))), agendaId);
    }

    public List<BloqueoAgenda> bloqueos(UUID agendaId, Instant inicio, Instant fin) {
        return jdbc.query("""
                SELECT id, agenda_id, inicio, fin, descripcion
                FROM bloqueo_agenda WHERE agenda_id = ? AND inicio < ? AND fin > ? ORDER BY inicio, id
                """, (row, index) -> new BloqueoAgenda(row.getObject("id", UUID.class),
                        row.getObject("agenda_id", UUID.class), instant(row, "inicio"), instant(row, "fin"),
                        row.getString("descripcion")), agendaId, atUtc(fin), atUtc(inicio));
    }

    public Optional<Turno> findTurno(UUID agendaId, UUID turnoId) {
        return DataAccessUtils.optionalResult(jdbc.query(TURNOS + "AND id = ?", AgendaQueries::readTurno, agendaId, turnoId));
    }

    public List<Turno> ocupados(UUID agendaId, Instant inicio, Instant fin) {
        return jdbc.query(TURNOS + "AND estado = 'CONFIRMADO' AND inicio < ? AND fin > ? ORDER BY inicio, id",
                AgendaQueries::readTurno, agendaId, atUtc(fin), atUtc(inicio));
    }

    private static Turno readTurno(ResultSet row, int index) throws SQLException {
        return new Turno(row.getObject("id", UUID.class), row.getObject("agenda_id", UUID.class),
                row.getObject("paciente_id", UUID.class), row.getObject("motivo_id", UUID.class),
                instant(row, "inicio"), instant(row, "fin"), row.getInt("duracion_reservada_minutos"),
                Turno.Estado.valueOf(row.getString("estado")), row.getInt("revision"), instant(row, "creado_en"),
                instant(row, "cancelado_en"));
    }

    private static Instant instant(ResultSet row, String column) throws SQLException {
        OffsetDateTime value = row.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static OffsetDateTime atUtc(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC);
    }
}
