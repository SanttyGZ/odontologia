package dev.odontologia.disponibilidad;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import dev.odontologia.agenda.AgendaQueries;
import dev.odontologia.agenda.BloqueoAgenda;
import dev.odontologia.agenda.FranjaSemanal;
import dev.odontologia.agenda.MotivoConsulta;
import dev.odontologia.agenda.Turno;
import dev.odontologia.consultorio.ConsultorioQueries;
import dev.odontologia.consultorio.ConsultorioQueries.Configuracion;
import dev.odontologia.disponibilidad.AvailabilityRules.Slot;
import org.springframework.stereotype.Service;

@Service
public final class PublicAvailabilityService {

    private final AgendaQueries agendaQueries;
    private final ConsultorioQueries consultorioQueries;
    private final Clock clock;

    public PublicAvailabilityService(AgendaQueries agendaQueries, ConsultorioQueries consultorioQueries, Clock clock) {
        this.agendaQueries = agendaQueries;
        this.consultorioQueries = consultorioQueries;
        this.clock = clock;
    }

    public List<MotivoConsulta> motivos() {
        Configuracion config = configuration();
        return agendaQueries.motivos(config.agenda().id());
    }

    public DisponibilidadResult disponibilidad(UUID motivoId, LocalDate desde, LocalDate hasta) {
        if (motivoId == null || (desde != null && hasta != null && desde.isAfter(hasta))) {
            throw AvailabilityException.validation();
        }
        Configuracion config = configuration();
        UUID agendaId = config.agenda().id();
        MotivoConsulta motivo = agendaQueries.findMotivo(agendaId, motivoId)
                .orElseThrow(AvailabilityException::motivoNotFound);

        Instant now = clock.instant();
        ZoneId zone = config.consultorio().zonaHoraria();
        Instant windowStart = now.plus(24, ChronoUnit.HOURS);
        Instant windowEnd = now.atZone(zone).toLocalDate().plusDays(31).atStartOfDay(zone).toInstant();

        List<FranjaSemanal> schedule = agendaQueries.franjas(agendaId);
        List<BloqueoAgenda> blocks = windowStart.isBefore(windowEnd)
                ? agendaQueries.bloqueos(agendaId, windowStart, windowEnd) : List.of();
        List<Turno> occupied = windowStart.isBefore(windowEnd)
                ? agendaQueries.ocupados(agendaId, windowStart, windowEnd) : List.of();

        List<Slot> slots = AvailabilityRules.availableSlots(motivo.duracionMinutos(), schedule, blocks, occupied,
                desde, hasta, now, zone);
        return new DisponibilidadResult(motivo.id(), motivo.duracionMinutos(), zone, slots);
    }

    private Configuracion configuration() {
        return consultorioQueries.findConfiguration().orElseThrow(AvailabilityException::technicalFailure);
    }

    public record DisponibilidadResult(UUID motivoId, int duracionMinutos, ZoneId zonaHoraria, List<Slot> slots) {
    }
}
