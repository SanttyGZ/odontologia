package dev.odontologia;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoreSchemaTest {

    @Test
    void migratesEightTablesOnceAndDoesNotSeedRows() {
        try (var db = new TestDatabase()) {
            assertThat(db.flyway().migrate().migrationsExecuted).isEqualTo(1);
            assertThat(db.flyway().migrate().migrationsExecuted).isZero();
            List<String> tables = db.jdbc.queryForList("""
                    SELECT tablename FROM pg_tables
                    WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
                    """, String.class);
            assertThat(tables).containsExactlyInAnyOrder("consultorio", "odontologo", "agenda",
                    "motivo_consulta", "franja_semanal", "bloqueo_agenda", "paciente", "turno");
            tables.forEach(table -> assertThat(db.jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class))
                    .as(table).isZero());
            assertThat(db.jdbc.queryForList("""
                    SELECT indexname FROM pg_indexes WHERE schemaname = 'public'
                    """, String.class)).contains("franja_semanal_agenda_dia_inicio_idx",
                    "bloqueo_agenda_agenda_inicio_idx", "turno_agenda_inicio_idx", "turno_motivo_agenda_idx");
        }
    }

    @Test
    void rejectsUnknownSchemaAndMismatchedChecksumWithoutRepair() {
        try (var unknown = new TestDatabase(); var drift = new TestDatabase()) {
            unknown.jdbc.execute("CREATE TABLE ajena (id integer)");
            assertThatThrownBy(() -> unknown.flyway().migrate()).isInstanceOf(FlywayException.class);
            assertThat(unknown.jdbc.queryForObject("SELECT count(*) FROM ajena", Long.class)).isZero();
            drift.flyway().migrate();
            drift.jdbc.update("UPDATE flyway_schema_history SET checksum = 1 WHERE version = '1'");
            assertThatThrownBy(() -> drift.flyway().migrate()).isInstanceOf(FlywayException.class)
                    .hasMessageContaining("checksum");
            assertThat(drift.jdbc.queryForObject("SELECT checksum FROM flyway_schema_history WHERE version = '1'",
                    Integer.class)).isEqualTo(1);
        }
    }

    @Test
    void enforcesSingletonAndAvailabilityConstraints() {
        try (var db = new TestDatabase()) {
            Fixture f = fixture(db);
            rejects(() -> db.jdbc.update("INSERT INTO consultorio (id, nombre_visible, zona_horaria) VALUES (?, 'Otro', 'Etc/UTC')", UUID.randomUUID()));
            rejects(() -> db.jdbc.update("INSERT INTO odontologo VALUES (?, ?, 'Otro')", UUID.randomUUID(), f.consultorio()));
            rejects(() -> db.jdbc.update("INSERT INTO agenda VALUES (?, ?)", UUID.randomUUID(), f.odontologo()));
            rejects(() -> db.jdbc.update("INSERT INTO agenda VALUES (?, ?)", UUID.randomUUID(), UUID.randomUUID()));
            rejects(() -> db.jdbc.update("INSERT INTO motivo_consulta VALUES (?, ?, ' ', 30)", UUID.randomUUID(), f.agenda()));
            rejects(() -> db.jdbc.update("INSERT INTO motivo_consulta VALUES (?, ?, 'Prueba', 0)", UUID.randomUUID(), f.agenda()));
            rejects(() -> db.jdbc.update("INSERT INTO motivo_consulta VALUES (?, ?, NULL, 30)", UUID.randomUUID(), f.agenda()));
            rejects(() -> db.jdbc.update("INSERT INTO franja_semanal VALUES (?, ?, 0, TIME '08:00', TIME '09:00', 'ATENCION')", UUID.randomUUID(), f.agenda()));
            rejects(() -> db.jdbc.update("INSERT INTO franja_semanal VALUES (?, ?, 1, TIME '08:00', TIME '09:00', 'OTRO')", UUID.randomUUID(), f.agenda()));
            rejects(() -> db.jdbc.update("INSERT INTO franja_semanal VALUES (?, ?, 1, TIME '09:00', TIME '08:00', 'ATENCION')", UUID.randomUUID(), f.agenda()));
            rejects(() -> db.jdbc.update("INSERT INTO franja_semanal VALUES (?, ?, 1, TIME '09:00', TIME '24:00', 'ATENCION')", UUID.randomUUID(), f.agenda()));
            rejects(() -> db.jdbc.update("INSERT INTO bloqueo_agenda VALUES (?, ?, ?, ?, NULL)", UUID.randomUUID(), f.agenda(), START, START));
            assertThat(db.jdbc.update("INSERT INTO franja_semanal VALUES (?, ?, 1, TIME '08:00', TIME '09:00', 'ATENCION')", UUID.randomUUID(), f.agenda())).isOne();
        }
    }

    @Test
    void protectsTurnReferencesAndReservedDurationAndAllowsRepeatedPatientData() {
        try (var db = new TestDatabase()) {
            Fixture f = fixture(db);
            UUID appointment = UUID.randomUUID();
            assertThat(insertPatient(db.jdbc, UUID.randomUUID(), f.consultorio())).isOne();
            rejects(() -> db.jdbc.update("INSERT INTO paciente VALUES (?, ?, NULL, 'Demo', 'f@example.invalid', '001', '002')", UUID.randomUUID(), f.consultorio()));
            rejects(() -> insertPatient(db.jdbc, UUID.randomUUID(), UUID.randomUUID()));
            rejects(() -> insertTurn(db.jdbc, f, appointment, START, START.plusMinutes(29), "CONFIRMADO"));
            rejects(() -> insertTurn(db.jdbc, f, appointment, START, START.plusMinutes(30), "CANCELADO"));
            rejects(() -> insertTurn(db.jdbc, new Fixture(f.consultorio(), f.odontologo(), f.agenda(), UUID.randomUUID(), f.paciente()),
                    appointment, START, START.plusMinutes(30), "CONFIRMADO"));
            insertTurn(db.jdbc, f, appointment, START, START.plusMinutes(30), "CONFIRMADO");
            rejects(() -> db.jdbc.update("UPDATE turno SET revision = -1 WHERE id = ?", appointment));
            rejects(() -> db.jdbc.update("UPDATE turno SET cancelado_en = ? WHERE id = ?", START, appointment));
            rejects(() -> db.jdbc.update("DELETE FROM paciente WHERE id = ?", f.paciente()));
            rejects(() -> db.jdbc.update("DELETE FROM motivo_consulta WHERE id = ?", f.motivo()));
            db.jdbc.update("UPDATE motivo_consulta SET duracion_minutos = 45 WHERE id = ?", f.motivo());
            assertThat(db.jdbc.queryForObject("SELECT duracion_reservada_minutos FROM turno WHERE id = ?", Integer.class, appointment)).isEqualTo(30);
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM paciente", Long.class)).isEqualTo(2);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 15})
    void exclusionSerializesOverlappingInsertsEvenWithoutCoordinator(int offset) throws Exception {
        try (var db = new TestDatabase(); var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            Fixture f = fixture(db);
            var inserted = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var transaction = new TransactionTemplate(new JdbcTransactionManager(db.dataSource()));
            var a = threads.submit(() -> transaction.executeWithoutResult(status -> {
                insertTurn(db.jdbc, f, UUID.randomUUID(), START, START.plusMinutes(30), "CONFIRMADO");
                inserted.countDown();
                awaitCommitSignal(release);
            }));
            try {
                assertThat(inserted.await(4, TimeUnit.SECONDS)).isTrue();
                var b = threads.submit(() -> tryInsert(db.jdbc, f, START.plusMinutes(offset)));
                AgendaWriteCoordinatorTest.awaitLockWait(db);
                assertThat(b).isNotDone();
                release.countDown();
                a.get(8, TimeUnit.SECONDS);
                assertThat(b.get(8, TimeUnit.SECONDS)).isEqualTo("23P01");
            } finally {
                release.countDown();
            }
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM turno", Long.class)).isOne();
        }
    }

    @Test
    void allowsAdjacentIntervalsAndReleasesCancelledIntervals() {
        try (var db = new TestDatabase()) {
            Fixture f = fixture(db);
            UUID cancelled = UUID.randomUUID();
            insertTurn(db.jdbc, f, cancelled, START, START.plusMinutes(30), "CONFIRMADO");
            insertTurn(db.jdbc, f, UUID.randomUUID(), START.plusMinutes(30), START.plusMinutes(60), "CONFIRMADO");
            db.jdbc.update("UPDATE turno SET estado = 'CANCELADO', cancelado_en = ? WHERE id = ?", START, cancelled);
            insertTurn(db.jdbc, f, UUID.randomUUID(), START, START.plusMinutes(30), "CONFIRMADO");
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM turno WHERE estado = 'CONFIRMADO'", Long.class)).isEqualTo(2);
        }
    }

    static final OffsetDateTime START = OffsetDateTime.parse("2030-01-08T10:00:00.123456-03:00");
    static final String TEST_CONSULTORIO_NOMBRE = "Demo";
    static final String TEST_ODONTOLOGO_NOMBRE = "Profesional de prueba";

    static Fixture fixture(TestDatabase db) {
        db.flyway().migrate();
        var f = new Fixture(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        db.jdbc.update("INSERT INTO consultorio (id, nombre_visible, zona_horaria) VALUES (?, ?, 'Etc/UTC')",
                f.consultorio(), TEST_CONSULTORIO_NOMBRE);
        db.jdbc.update("INSERT INTO odontologo VALUES (?, ?, ?)", f.odontologo(), f.consultorio(),
                TEST_ODONTOLOGO_NOMBRE);
        db.jdbc.update("INSERT INTO agenda VALUES (?, ?)", f.agenda(), f.odontologo());
        db.jdbc.update("INSERT INTO motivo_consulta VALUES (?, ?, 'Motivo de prueba', 30)", f.motivo(), f.agenda());
        insertPatient(db.jdbc, f.paciente(), f.consultorio());
        return f;
    }

    static int insertPatient(JdbcTemplate jdbc, UUID id, UUID consultorio) {
        return jdbc.update("INSERT INTO paciente VALUES (?, ?, 'Paciente', 'Prueba', 'f@example.invalid', '001', '002')", id, consultorio);
    }

    static void insertTurn(JdbcTemplate jdbc, Fixture f, UUID id, OffsetDateTime start, OffsetDateTime end, String state) {
        jdbc.update("""
                INSERT INTO turno (id, agenda_id, paciente_id, motivo_id, inicio, fin,
                duracion_reservada_minutos, estado, revision, creado_en) VALUES (?, ?, ?, ?, ?, ?, 30, ?, 0, ?)
                """, id, f.agenda(), f.paciente(), f.motivo(), start, end, state, start.minusDays(1));
    }

    private static String tryInsert(JdbcTemplate jdbc, Fixture f, OffsetDateTime start) {
        try {
            insertTurn(jdbc, f, UUID.randomUUID(), start, start.plusMinutes(30), "CONFIRMADO");
            return "confirmed";
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMostSpecificCause() instanceof java.sql.SQLException sql) {
                return sql.getSQLState();
            }
            throw ex;
        }
    }

    private static void awaitCommitSignal(CountDownLatch release) {
        try {
            assertThat(release.await(4, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Prueba interrumpida", ex);
        }
    }

    private static void rejects(org.assertj.core.api.ThrowableAssert.ThrowingCallable statement) {
        assertThatThrownBy(statement).isInstanceOf(DataIntegrityViolationException.class);
    }

    record Fixture(UUID consultorio, UUID odontologo, UUID agenda, UUID motivo, UUID paciente) {
    }
}
