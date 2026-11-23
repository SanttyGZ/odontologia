package dev.odontologia.consultorio;

import java.util.UUID;

import dev.odontologia.config.AppProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@DependsOnDatabaseInitialization
final class ConsultorioInitializer implements InitializingBean {

    private final JdbcTemplate jdbc;
    private final AppProperties configuration;
    private final TransactionTemplate transaction;

    ConsultorioInitializer(JdbcTemplate jdbc, AppProperties configuration, PlatformTransactionManager transactions) {
        this.jdbc = jdbc;
        this.configuration = configuration;
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Override
    public void afterPropertiesSet() {
        transaction.executeWithoutResult(status -> initialize());
    }

    private void initialize() {
        String zone = configuration.timeZone().getId();
        jdbc.update("""
                INSERT INTO consultorio (id, nombre_visible, zona_horaria)
                VALUES (?, ?, ?) ON CONFLICT (unico) DO NOTHING
                """, UUID.randomUUID(), configuration.consultorioNombre(), zone);
        var office = jdbc.queryForMap("""
                SELECT id, nombre_visible, zona_horaria FROM consultorio WHERE unico FOR UPDATE
                """);
        requireMatch(configuration.consultorioNombre(), office.get("nombre_visible"), "nombre del consultorio");
        requireMatch(zone, office.get("zona_horaria"), "zona horaria del consultorio");
        UUID officeId = (UUID) office.get("id");

        jdbc.update("""
                INSERT INTO odontologo (id, consultorio_id, nombre_visible)
                VALUES (?, ?, ?) ON CONFLICT (consultorio_id) DO NOTHING
                """, UUID.randomUUID(), officeId, configuration.odontologoNombre());
        var professional = jdbc.queryForMap("""
                SELECT id, nombre_visible FROM odontologo WHERE consultorio_id = ?
                """, officeId);
        requireMatch(configuration.odontologoNombre(), professional.get("nombre_visible"), "nombre del odontólogo");

        jdbc.update("""
                INSERT INTO agenda (id, odontologo_id) VALUES (?, ?) ON CONFLICT (odontologo_id) DO NOTHING
                """, UUID.randomUUID(), professional.get("id"));
    }

    private static void requireMatch(String expected, Object actual, String field) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException("La configuración local no coincide con el " + field + " persistido.");
        }
    }
}
