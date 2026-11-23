package dev.odontologia.agenda;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static dev.odontologia.agenda.AgendaWriteException.Reason;

@Component
public final class AgendaWriteCoordinator {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public AgendaWriteCoordinator(JdbcTemplate jdbc, PlatformTransactionManager transactions) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactions);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transaction.setTimeout(8);
    }

    /**
     * Ejecuta una escritura en la transacción raíz. El trabajo debe releer las
     * condiciones de la operación aquí, en la misma conexión, después del bloqueo.
     * Un rechazo debe propagarse como excepción para que se revierta toda la escritura.
     */
    public <T> T write(UUID agendaId, Supplier<T> work) {
        Objects.requireNonNull(agendaId, "agendaId");
        Objects.requireNonNull(work, "work");
        if (TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("La coordinación de agenda requiere iniciar la transacción raíz.");
        }

        try {
            return transaction.execute(status -> {
                jdbc.execute("SET LOCAL lock_timeout = '2s'");
                jdbc.execute("SET LOCAL statement_timeout = '4s'");
                if (!Boolean.TRUE.equals(jdbc.query("SELECT id FROM agenda WHERE id = ? FOR UPDATE",
                        (ResultSetExtractor<Boolean>) ResultSet::next, agendaId))) {
                    throw new AgendaWriteException(Reason.AGENDA_NOT_FOUND);
                }
                return work.get();
            });
        } catch (DataAccessException | TransactionException failure) {
            // Clasificar después del rollback. Los mensajes del driver pueden incluir datos de filas.
            throw new AgendaWriteException(classify(failure));
        }
    }

    private static Reason classify(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getSQLState() != null) {
                return switch (sql.getSQLState()) {
                    case "55P03" -> Reason.LOCK_TIMEOUT;
                    case "23P01" -> Reason.OVERLAP;
                    case "23502", "23503", "23505", "23514" -> Reason.INTEGRITY_CONFLICT;
                    case "40P01", "40001" -> Reason.CONCURRENT_CONFLICT;
                    default -> Reason.TECHNICAL_FAILURE;
                };
            }
        }
        return Reason.TECHNICAL_FAILURE;
    }
}
