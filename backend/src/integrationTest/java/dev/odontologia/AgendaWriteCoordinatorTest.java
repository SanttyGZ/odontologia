package dev.odontologia;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import dev.odontologia.agenda.AgendaWriteCoordinator;
import dev.odontologia.agenda.AgendaWriteException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static dev.odontologia.CoreSchemaTest.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgendaWriteCoordinatorTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void waitsForAgendaAndRereadsOccupationAndConfigurationAfterCommitOrRollback(boolean commit) throws Exception {
        try (var db = new TestDatabase(); var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            var f = CoreSchemaTest.fixture(db);
            var coordinator = coordinator(db);
            var changed = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            UUID appointment = UUID.randomUUID();
            var a = threads.submit(() -> {
                try {
                    coordinator.write(f.agenda(), () -> {
                        db.jdbc.update("UPDATE motivo_consulta SET duracion_minutos = 45 WHERE id = ?", f.motivo());
                        CoreSchemaTest.insertTurn(db.jdbc, f, appointment, START, START.plusMinutes(30), "CONFIRMADO");
                        changed.countDown();
                        await(release);
                        if (!commit) {
                            throw new ExpectedRollback();
                        }
                        return null;
                    });
                } catch (ExpectedRollback expected) {
                    if (commit) {
                        throw expected;
                    }
                }
            });
            try {
                await(changed);
                assertThat(duration(db, f.motivo())).isEqualTo(30);
                var b = threads.submit(() -> coordinator.write(f.agenda(), () -> {
                    assertThat(db.jdbc.queryForObject("SHOW transaction_isolation", String.class)).isEqualTo("read committed");
                    assertThat(duration(db, f.motivo())).isEqualTo(commit ? 45 : 30);
                    return db.jdbc.queryForObject("SELECT count(*) FROM turno WHERE id = ?", Long.class, appointment);
                }));
                awaitLockWait(db);
                assertThat(b).isNotDone();
                release.countDown();
                a.get(8, TimeUnit.SECONDS);
                assertThat(b.get(8, TimeUnit.SECONDS)).isEqualTo(commit ? 1L : 0L);
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void boundsLockWaitAndReleasesTheTransactionAfterRollback() throws Exception {
        try (var db = new TestDatabase(); var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            var f = CoreSchemaTest.fixture(db);
            var coordinator = coordinator(db);
            var held = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var first = threads.submit(() -> coordinator.write(f.agenda(), () -> {
                held.countDown();
                await(release);
                return null;
            }));
            try {
                await(held);
                long start = System.nanoTime();
                assertThatThrownBy(() -> coordinator.write(f.agenda(), () -> {
                    throw new AssertionError("No debe ejecutar el trabajo sin adquirir la agenda");
                })).isInstanceOfSatisfying(AgendaWriteException.class,
                        ex -> assertThat(ex.reason()).isEqualTo(AgendaWriteException.Reason.LOCK_TIMEOUT));
                assertThat(Duration.ofNanos(System.nanoTime() - start))
                        .isBetween(Duration.ofSeconds(1), Duration.ofSeconds(5));
            } finally {
                release.countDown();
            }
            first.get(8, TimeUnit.SECONDS);
            assertThat(coordinator.write(f.agenda(), () -> duration(db, f.motivo()))).isEqualTo(30);
        }
    }

    @Test
    void rejectsMissingAgendaAndPreexistingTransaction() {
        try (var db = new TestDatabase()) {
            var f = CoreSchemaTest.fixture(db);
            var manager = new JdbcTransactionManager(db.dataSource());
            var coordinator = new AgendaWriteCoordinator(db.jdbc, manager);
            assertThatThrownBy(() -> coordinator.write(UUID.randomUUID(), () -> {
                throw new AssertionError("No debe ejecutar el trabajo para una agenda inexistente");
            })).isInstanceOfSatisfying(AgendaWriteException.class,
                    ex -> assertThat(ex.reason()).isEqualTo(AgendaWriteException.Reason.AGENDA_NOT_FOUND));
            new TransactionTemplate(manager).executeWithoutResult(status ->
                    assertThatThrownBy(() -> coordinator.write(f.agenda(), () -> null))
                            .isInstanceOf(IllegalStateException.class));
        }
    }

    @Test
    void conflictPreservesOriginalAndRollsBackOtherWritesTogether() {
        try (var db = new TestDatabase()) {
            var f = CoreSchemaTest.fixture(db);
            var coordinator = coordinator(db);
            var original = UUID.randomUUID();
            CoreSchemaTest.insertTurn(db.jdbc, f, original, START, START.plusMinutes(30), "CONFIRMADO");
            CoreSchemaTest.insertTurn(db.jdbc, f, UUID.randomUUID(), START.plusMinutes(30), START.plusMinutes(60), "CONFIRMADO");
            var before = db.jdbc.queryForMap("SELECT * FROM turno WHERE id = ?", original);

            assertThatThrownBy(() -> coordinator.write(f.agenda(), () -> {
                CoreSchemaTest.insertPatient(db.jdbc, UUID.randomUUID(), f.consultorio());
                db.jdbc.update("UPDATE turno SET inicio = ?, fin = ?, revision = revision + 1 WHERE id = ?",
                        START.plusMinutes(30), START.plusMinutes(60), original);
                return null;
            })).isInstanceOfSatisfying(AgendaWriteException.class,
                    ex -> assertThat(ex.reason()).isEqualTo(AgendaWriteException.Reason.OVERLAP));

            assertThat(db.jdbc.queryForMap("SELECT * FROM turno WHERE id = ?", original)).isEqualTo(before);
            assertThat(db.jdbc.queryForObject("SELECT count(*) FROM paciente", Long.class)).isOne();
        }
    }

    private static AgendaWriteCoordinator coordinator(TestDatabase db) {
        return new AgendaWriteCoordinator(db.jdbc, new JdbcTransactionManager(db.dataSource()));
    }

    private static int duration(TestDatabase db, UUID motivo) {
        return db.jdbc.queryForObject("SELECT duracion_minutos FROM motivo_consulta WHERE id = ?", Integer.class, motivo);
    }

    static void awaitLockWait(TestDatabase db) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1600);
        while (System.nanoTime() < deadline) {
            Boolean waiting = db.jdbc.queryForObject("""
                    SELECT EXISTS (SELECT 1 FROM pg_stat_activity WHERE datname = current_database()
                        AND wait_event_type = 'Lock')
                    """, Boolean.class);
            if (Boolean.TRUE.equals(waiting)) {
                return;
            }
            Thread.sleep(15);
        }
        throw new AssertionError("PostgreSQL no mostró una segunda conexión esperando un bloqueo");
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(7, TimeUnit.SECONDS)) {
                throw new AssertionError("No se completó la sincronización de las transacciones");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Prueba interrumpida", ex);
        }
    }

    private static final class ExpectedRollback extends RuntimeException {
    }
}
