package dev.odontologia.consultorio;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import dev.odontologia.agenda.Agenda;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConsultorioQueries {

    private final JdbcTemplate jdbc;

    public ConsultorioQueries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Configuracion> findConfiguration() {
        return DataAccessUtils.optionalResult(jdbc.query("""
                SELECT c.id AS consultorio_id, c.nombre_visible AS consultorio_nombre, c.zona_horaria,
                       o.id AS odontologo_id, o.nombre_visible AS odontologo_nombre, a.id AS agenda_id
                FROM consultorio c
                JOIN odontologo o ON o.consultorio_id = c.id
                JOIN agenda a ON a.odontologo_id = o.id
                WHERE c.unico
                """, (row, index) -> {
                    UUID consultorioId = row.getObject("consultorio_id", UUID.class);
                    UUID odontologoId = row.getObject("odontologo_id", UUID.class);
                    return new Configuracion(
                            new Consultorio(consultorioId, row.getString("consultorio_nombre"),
                                    ZoneId.of(row.getString("zona_horaria"))),
                            new Odontologo(odontologoId, consultorioId, row.getString("odontologo_nombre")),
                            new Agenda(row.getObject("agenda_id", UUID.class), odontologoId));
                }));
    }

    public record Configuracion(Consultorio consultorio, Odontologo odontologo, Agenda agenda) {
    }
}
