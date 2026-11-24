package dev.odontologia.agenda;

import dev.odontologia.disponibilidad.AvailabilityErrorAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityErrorAdviceTest {

    @Test
    void mapsAgendaLockTimeoutToConflictWithoutLeakingDetails() {
        var response = new AvailabilityErrorAdvice().agendaWrite(
                new AgendaWriteException(AgendaWriteException.Reason.LOCK_TIMEOUT));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(new AvailabilityErrorAdvice.ErrorResponse("AGENDA_BUSY"));
    }
}
