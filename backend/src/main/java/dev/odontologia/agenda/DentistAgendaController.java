package dev.odontologia.agenda;

import java.time.LocalDate;
import java.util.UUID;

import dev.odontologia.agenda.DentistAgendaDtos.AgendaResponse;
import dev.odontologia.agenda.DentistAgendaDtos.TurnoDetalleResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/odontologo")
public final class DentistAgendaController {

    private final DentistAgendaService service;

    public DentistAgendaController(DentistAgendaService service) {
        this.service = service;
    }

    @GetMapping("/agenda")
    public AgendaResponse agenda(@RequestParam(required = false) String vista,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.agenda(vista, fecha);
    }

    @GetMapping("/turnos/{turnoId}")
    public TurnoDetalleResponse turnoDetalle(@PathVariable UUID turnoId) {
        return service.turnoDetalle(turnoId);
    }
}
