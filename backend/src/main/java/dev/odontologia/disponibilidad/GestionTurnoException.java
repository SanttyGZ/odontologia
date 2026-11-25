package dev.odontologia.disponibilidad;

import org.springframework.http.HttpStatus;

final class GestionTurnoException extends RuntimeException {

    enum Codigo {
        LINK_NOT_AVAILABLE(HttpStatus.NOT_FOUND),
        MANAGEMENT_WINDOW_CLOSED(HttpStatus.CONFLICT),
        SLOT_UNAVAILABLE(HttpStatus.CONFLICT),
        SAME_INTERVAL(HttpStatus.CONFLICT),
        AGENDA_BUSY(HttpStatus.CONFLICT),
        TURN_VERSION_MISMATCH(HttpStatus.PRECONDITION_FAILED),
        PRECONDITION_REQUIRED(HttpStatus.PRECONDITION_REQUIRED),
        VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
        TECHNICAL_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR);

        private final HttpStatus status;

        Codigo(HttpStatus status) {
            this.status = status;
        }
    }

    private final Codigo codigo;

    GestionTurnoException(Codigo codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    Codigo codigo() {
        return codigo;
    }

    HttpStatus status() {
        return codigo.status;
    }

    static GestionTurnoException linkNotAvailable() {
        return new GestionTurnoException(Codigo.LINK_NOT_AVAILABLE);
    }

    static GestionTurnoException managementWindowClosed() {
        return new GestionTurnoException(Codigo.MANAGEMENT_WINDOW_CLOSED);
    }

    static GestionTurnoException slotUnavailable() {
        return new GestionTurnoException(Codigo.SLOT_UNAVAILABLE);
    }

    static GestionTurnoException sameInterval() {
        return new GestionTurnoException(Codigo.SAME_INTERVAL);
    }

    static GestionTurnoException agendaBusy() {
        return new GestionTurnoException(Codigo.AGENDA_BUSY);
    }

    static GestionTurnoException versionMismatch() {
        return new GestionTurnoException(Codigo.TURN_VERSION_MISMATCH);
    }

    static GestionTurnoException preconditionRequired() {
        return new GestionTurnoException(Codigo.PRECONDITION_REQUIRED);
    }

    static GestionTurnoException validation() {
        return new GestionTurnoException(Codigo.VALIDATION_ERROR);
    }

    static GestionTurnoException technicalFailure() {
        return new GestionTurnoException(Codigo.TECHNICAL_FAILURE);
    }
}
