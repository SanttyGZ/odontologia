package dev.odontologia;

import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

final class TestDatabase implements AutoCloseable {

    private static final String BASE = "jdbc:postgresql://127.0.0.1:5433/";
    private final String name = "odontologia_c02_" + UUID.randomUUID().toString().replace("-", "");
    private final String user = System.getenv("SPRING_DATASOURCE_USERNAME");
    private final String password = System.getenv("SPRING_DATASOURCE_PASSWORD");
    private final JdbcTemplate admin;
    private final DriverManagerDataSource dataSource;
    final JdbcTemplate jdbc;

    TestDatabase() {
        if (!(BASE + "odontologia_test").equals(System.getenv("SPRING_DATASOURCE_URL"))
                || !"true".equals(System.getenv("ODONTOLOGIA_INTEGRATION_DATABASES"))
                || user == null || password == null) {
            throw new IllegalStateException("Usar scripts/with-local-db.py --integration para habilitar bases efímeras en el PostgreSQL de Compose.");
        }
        admin = new JdbcTemplate(connect(BASE + "odontologia_test"));
        dataSource = connect(url());
        jdbc = new JdbcTemplate(dataSource);
        admin.execute("CREATE DATABASE " + name + " TEMPLATE template0");
    }

    String url() {
        return BASE + name;
    }

    DriverManagerDataSource dataSource() {
        return dataSource;
    }

    Flyway flyway() {
        return Flyway.configure().dataSource(dataSource).cleanDisabled(true).load();
    }

    private DriverManagerDataSource connect(String url) {
        return new DriverManagerDataSource(url + "?connectTimeout=5&socketTimeout=5", user, password);
    }

    @Override
    public void close() {
        admin.execute("DROP DATABASE " + name);
    }
}
