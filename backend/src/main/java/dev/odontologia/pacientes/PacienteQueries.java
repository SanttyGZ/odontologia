package dev.odontologia.pacientes;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PacienteQueries {

    private final JdbcTemplate jdbc;

    public PacienteQueries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Paciente> find(UUID id) {
        return DataAccessUtils.optionalResult(jdbc.query("""
                SELECT id, consultorio_id, nombre, apellido, email, telefono, dni
                FROM paciente WHERE id = ?
                """, (row, index) -> new Paciente(row.getObject("id", UUID.class),
                        row.getObject("consultorio_id", UUID.class), row.getString("nombre"),
                        row.getString("apellido"), row.getString("email"), row.getString("telefono"),
                        row.getString("dni")), id));
    }
}
