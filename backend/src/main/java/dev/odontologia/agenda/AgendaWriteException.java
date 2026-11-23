package dev.odontologia.agenda;

public final class AgendaWriteException extends RuntimeException {

    public enum Reason {
        AGENDA_NOT_FOUND("La agenda no existe."),
        LOCK_TIMEOUT("Se agotó la espera por el bloqueo de escritura."),
        OVERLAP("El intervalo se superpone con otro turno confirmado."),
        INTEGRITY_CONFLICT("La escritura no cumple una restricción de integridad."),
        CONCURRENT_CONFLICT("La transacción fue rechazada por un conflicto concurrente."),
        TECHNICAL_FAILURE("No se pudo confirmar el resultado de la escritura.");

        private final String message;

        Reason(String message) {
            this.message = message;
        }
    }

    private final Reason reason;

    AgendaWriteException(Reason reason) {
        super(reason.message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
