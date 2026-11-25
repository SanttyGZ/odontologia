package dev.odontologia.disponibilidad;

import org.springframework.http.HttpStatus;

public final class AvailabilityException extends RuntimeException {

    private final String codigo;
    private final HttpStatus status;

    public AvailabilityException(String codigo, HttpStatus status) {
        super(codigo);
        this.codigo = codigo;
        this.status = status;
    }

    public String codigo() {
        return codigo;
    }

    public HttpStatus status() {
        return status;
    }

    public static AvailabilityException validation() {
        return new AvailabilityException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    }

    static AvailabilityException configurationConflict() {
        return new AvailabilityException("CONFIGURATION_CONFLICT", HttpStatus.CONFLICT);
    }

    static AvailabilityException slotUnavailable() {
        return new AvailabilityException("SLOT_UNAVAILABLE", HttpStatus.CONFLICT);
    }

    static AvailabilityException motivoNotFound() {
        return new AvailabilityException("MOTIVO_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    static AvailabilityException bloqueoNotFound() {
        return new AvailabilityException("BLOQUEO_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public static AvailabilityException turnoNotFound() {
        return new AvailabilityException("TURNO_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public static AvailabilityException technicalFailure() {
        return new AvailabilityException("TECHNICAL_FAILURE", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}