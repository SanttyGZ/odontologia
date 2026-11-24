package dev.odontologia.disponibilidad;

import dev.odontologia.agenda.AgendaWriteException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {AvailabilityConfigurationController.class, PublicAvailabilityController.class,
        PublicBookingController.class})
public final class AvailabilityErrorAdvice {

    @ExceptionHandler(AvailabilityException.class)
    public ResponseEntity<ErrorResponse> availability(AvailabilityException failure) {
        return response(failure.status(), failure.codigo());
    }

    @ExceptionHandler(AgendaWriteException.class)
    public ResponseEntity<ErrorResponse> agendaWrite(AgendaWriteException failure) {
        if (failure.reason() == AgendaWriteException.Reason.LOCK_TIMEOUT) {
            return response(HttpStatus.CONFLICT, "AGENDA_BUSY");
        }
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "TECHNICAL_FAILURE");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> invalidRequest(Exception failure) {
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> persistenceFailure(DataAccessException failure) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "TECHNICAL_FAILURE");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpectedFailure(Exception failure) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "TECHNICAL_FAILURE");
    }

    private static ResponseEntity<ErrorResponse> response(HttpStatus status, String code) {
        return ResponseEntity.status(status).body(new ErrorResponse(code));
    }

    public record ErrorResponse(String codigo) {
    }
}