package dev.odontologia.agenda;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class DentistAgendaDtos {

    private DentistAgendaDtos() {
    }

    public record AgendaResponse(String vista, LocalDate desde, LocalDate hasta, String zonaHoraria,
                                 List<AgendaTurnoResumenResponse> turnos) {
    }

    public record AgendaTurnoResumenResponse(UUID id, UUID motivoId, String motivoNombre, Instant inicio, Instant fin,
                                             int duracionReservadaMinutos, Turno.Estado estado, String pacienteNombre,
                                             String pacienteApellido) {
    }

    public record TurnoDetalleResponse(UUID id, UUID motivoId, String motivoNombre, Instant inicio, Instant fin,
                                       int duracionReservadaMinutos, Turno.Estado estado, int revision,
                                       Instant creadoEn, Instant canceladoEn, PacienteBasicoResponse paciente) {
    }

    public record PacienteBasicoResponse(String nombre, String apellido, String email, String telefono, String dni) {
    }
}
