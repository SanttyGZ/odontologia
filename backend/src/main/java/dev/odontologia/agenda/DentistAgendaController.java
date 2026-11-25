package dev.odontologia.agenda;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.UUID;
import java.util.regex.Pattern;

import dev.odontologia.agenda.DentistAgendaDtos.AgendaResponse;
import dev.odontologia.agenda.DentistAgendaDtos.TurnoDetalleResponse;
import dev.odontologia.disponibilidad.AvailabilityException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/odontologo")
public final class DentistAgendaController {

    private static final Pattern FECHA_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final DateTimeFormatter FECHA_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final DentistAgendaService service;

    public DentistAgendaController(DentistAgendaService service) {
        this.service = service;
    }

    @GetMapping("/agenda")
    public AgendaResponse agenda(@RequestParam(required = false) String vista,
                                 @RequestParam(required = false) String fecha) {
        return service.agenda(vista, parseFecha(fecha));
    }

    @GetMapping("/turnos/{turnoId}")
    public TurnoDetalleResponse turnoDetalle(@PathVariable String turnoId) {
        return service.turnoDetalle(parseTurnoId(turnoId));
    }

    private static LocalDate parseFecha(String fecha) {
        if (fecha == null) {
            return null;
        }
        if (!FECHA_PATTERN.matcher(fecha).matches()) {
            throw AvailabilityException.validation();
        }
        try {
            return LocalDate.parse(fecha, FECHA_FORMAT);
        } catch (DateTimeParseException invalidDate) {
            throw AvailabilityException.validation();
        }
    }

    private static UUID parseTurnoId(String turnoId) {
        if (turnoId == null || !UUID_PATTERN.matcher(turnoId).matches()) {
            throw AvailabilityException.validation();
        }
        return UUID.fromString(turnoId);
    }
}
