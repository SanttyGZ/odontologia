package dev.odontologia.agenda;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import dev.odontologia.agenda.DentistAgendaDtos.AgendaResponse;
import dev.odontologia.agenda.DentistAgendaDtos.TurnoDetalleResponse;
import dev.odontologia.consultorio.ConsultorioQueries;
import dev.odontologia.consultorio.ConsultorioQueries.Configuracion;
import dev.odontologia.disponibilidad.AvailabilityException;
import org.springframework.stereotype.Service;

@Service
public class DentistAgendaService {

    private final AgendaQueries agendaQueries;
    private final ConsultorioQueries consultorioQueries;
    private final Clock clock;

    public DentistAgendaService(AgendaQueries agendaQueries, ConsultorioQueries consultorioQueries, Clock clock) {
        this.agendaQueries = agendaQueries;
        this.consultorioQueries = consultorioQueries;
        this.clock = clock;
    }

    public AgendaResponse agenda(String vista, LocalDate fecha) {
        String normalizedVista = vista == null ? "DIA" : vista;
        if (!"DIA".equals(normalizedVista) && !"SEMANA".equals(normalizedVista)) {
            throw AvailabilityException.validation();
        }
        Configuracion config = requireConfiguration();
        ZoneId zone = config.consultorio().zonaHoraria();
        LocalDate referenceDate = fecha != null ? fecha : LocalDate.ofInstant(clock.instant(), zone);
        LocalDate desde = "SEMANA".equals(normalizedVista)
                ? referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : referenceDate;
        LocalDate hasta = "SEMANA".equals(normalizedVista) ? desde.plusDays(6) : desde;
        Instant periodoInicio = desde.atStartOfDay(zone).toInstant();
        Instant periodoFin = hasta.plusDays(1).atStartOfDay(zone).toInstant();
        return new AgendaResponse(
                normalizedVista,
                desde,
                hasta,
                zone.getId(),
                agendaQueries.findAgendaTurnos(config.agenda().id(), config.consultorio().id(),
                        periodoInicio, periodoFin));
    }

    public TurnoDetalleResponse turnoDetalle(UUID turnoId) {
        if (turnoId == null) {
            throw AvailabilityException.validation();
        }
        Configuracion config = requireConfiguration();
        return agendaQueries.findTurnoDetalle(config.agenda().id(), config.consultorio().id(), turnoId)
                .orElseThrow(AvailabilityException::turnoNotFound);
    }

    private Configuracion requireConfiguration() {
        return consultorioQueries.findConfiguration()
                .orElseThrow(AvailabilityException::technicalFailure);
    }
}
