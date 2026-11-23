package dev.odontologia;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresConnectivityTest {

    @Test
    void usesPublicSchemaEvenWhenJdbcSearchPathIncludesAnEarlierSchema() {
        try (var db = new TestDatabase()) {
            db.jdbc.execute("CREATE SCHEMA sombra");
            db.jdbc.execute("CREATE TABLE sombra.consultorio (irrelevante integer)");
            try (var app = start(db.url() + "?currentSchema=sombra,public", "Demo", "Etc/UTC")) {
                assertThat(db.jdbc.queryForObject("SELECT count(*) FROM public.consultorio", Long.class)).isOne();
                assertThat(db.jdbc.queryForObject("SELECT count(*) FROM sombra.consultorio", Long.class)).isZero();
            }
        }
    }

    @Test
    void startsAndPreservesPersistentDataWhenRestarted() throws Exception {
        try (var db = new TestDatabase()) {
            List<Map<String, Object>> before;
            try (var app = start(db, "Demo", "Etc/UTC")) {
                try (var connection = db.dataSource().getConnection(); var statement = connection.createStatement()) {
                    connection.setReadOnly(true);
                    assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
                    try (var result = statement.executeQuery("SELECT 1")) {
                        assertThat(result.next()).isTrue();
                        assertThat(result.getInt(1)).isOne();
                    }
                }
                before = configuration(db);
                assertThat(before).hasSize(1);
                assertThat(db.jdbc.queryForObject("SELECT count(*) FROM paciente", Long.class)).isZero();
                CoreSchemaTest.insertPatient(db.jdbc, UUID.randomUUID(), (UUID) before.getFirst().get("consultorio_id"));
            }
            try (var app = start(db, "Demo", "Etc/UTC")) {
                assertThat(configuration(db)).isEqualTo(before);
                assertThat(db.jdbc.queryForObject("SELECT count(*) FROM paciente", Long.class)).isOne();
                assertThat(db.jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Long.class)).isOne();
            }
            assertThatThrownBy(() -> start(db, "Otro", "Etc/UTC")).hasStackTraceContaining("nombre del consultorio");
            assertThatThrownBy(() -> start(db, "Demo", "America/Argentina/Mendoza")).hasStackTraceContaining("zona horaria");
            assertThat(configuration(db)).isEqualTo(before);
        }
    }

    @Test
    void concurrentlyInitializesOneConfiguration() throws Exception {
        try (var db = new TestDatabase(); var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            db.flyway().migrate();
            var ready = new CountDownLatch(2);
            var go = new CountDownLatch(1);
            java.util.concurrent.Callable<List<Map<String, Object>>> initialize = () -> {
                ready.countDown();
                if (!go.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("No se abrió la barrera de inicialización");
                }
                try (var context = start(concurrentApplication(), db.url(), "Demo", "Etc/UTC")) {
                    return configuration(db);
                }
            };
            var first = workers.submit(initialize);
            var second = workers.submit(initialize);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            assertThat(first.get(25, TimeUnit.SECONDS)).hasSize(1).isEqualTo(second.get(25, TimeUnit.SECONDS));
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM consultorio", Long.class)).isOne();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM odontologo", Long.class)).isOne();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM agenda", Long.class)).isOne();
        }
    }

    @Test
    void rollsBackPartialInitializationAndCompletesExistingConfiguration() {
        try (var db = new TestDatabase()) {
            db.flyway().migrate();
            db.jdbc.execute("ALTER TABLE agenda ADD CONSTRAINT fallo_fixture CHECK (false)");
            assertThatThrownBy(() -> start(db, "Demo", "Etc/UTC")).hasCauseInstanceOf(DataAccessException.class);
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM consultorio", Long.class)).isZero();
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM odontologo", Long.class)).isZero();
            db.jdbc.execute("ALTER TABLE agenda DROP CONSTRAINT fallo_fixture");
            UUID office = UUID.randomUUID();
            db.jdbc.update("INSERT INTO consultorio (id, nombre_visible, zona_horaria) VALUES (?, 'Demo', 'Etc/UTC')", office);
            try (var app = start(db, "Demo", "Etc/UTC")) {
                assertThat(configuration(db)).hasSize(1);
                assertThat(configuration(db).getFirst().get("consultorio_id")).isEqualTo(office);
            }
        }
    }

    private static List<Map<String, Object>> configuration(TestDatabase db) {
        return db.jdbc.queryForList("""
                SELECT c.id AS consultorio_id, c.nombre_visible AS consultorio, c.zona_horaria,
                       o.id AS odontologo_id, o.nombre_visible AS odontologo, a.id AS agenda_id
                FROM consultorio c JOIN odontologo o ON o.consultorio_id = c.id
                JOIN agenda a ON a.odontologo_id = o.id
                """);
    }

    private static ConfigurableApplicationContext start(TestDatabase db, String office, String zone) {
        return start(db.url(), office, zone);
    }

    private static ConfigurableApplicationContext start(String url, String office, String zone) {
        return start(new SpringApplicationBuilder(OdontologiaApplication.class).web(WebApplicationType.NONE).build(),
                url, office, zone);
    }

    private static SpringApplication concurrentApplication() {
        var application = new SpringApplicationBuilder(OdontologiaApplication.class).web(WebApplicationType.NONE).build();
        // Los dos contextos comparten el logger de la JVM. Evitar su reinicialización concurrente.
        application.setListeners(application.getListeners().stream()
                .filter(listener -> !(listener instanceof LoggingApplicationListener)).toList());
        return application;
    }

    private static ConfigurableApplicationContext start(SpringApplication application, String url, String office,
            String zone) {
        return application.run(
                "--spring.datasource.url=" + url,
                "--app.time-zone=" + zone,
                "--app.consultorio-nombre=" + office,
                "--app.odontologo-nombre=Profesional ficticio",
                "--spring.main.banner-mode=off");
    }
}
