package dev.odontologia.disponibilidad;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import dev.odontologia.agenda.AgendaQueries;
import dev.odontologia.agenda.AgendaWriteCoordinator;
import dev.odontologia.agenda.AgendaWriteException;
import dev.odontologia.agenda.BloqueoAgenda;
import dev.odontologia.agenda.FranjaSemanal;
import dev.odontologia.agenda.MotivoConsulta;
import dev.odontologia.agenda.Turno;
import dev.odontologia.comunicaciones.PreparacionAvisos;
import dev.odontologia.consultorio.ConsultorioQueries;
import dev.odontologia.consultorio.ConsultorioQueries.Configuracion;
import dev.odontologia.disponibilidad.AvailabilityRules.ValidatedPatient;
import dev.odontologia.disponibilidad.PublicBookingDtos.ReservaInput;
import dev.odontologia.disponibilidad.PublicBookingDtos.ReservaResponse;
import dev.odontologia.gestion.CredencialesGestion;
import dev.odontologia.pacientes.Paciente;
import dev.odontologia.pacientes.PacienteQueries;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public final class PublicBookingService {

    private final JdbcTemplate jdbc;
    private final AgendaQueries agendaQueries;
    private final ConsultorioQueries consultorioQueries;
    private final PacienteQueries pacienteQueries;
    private final AgendaWriteCoordinator writeCoordinator;
    private final CredencialesGestion credencialesGestion;
    private final PreparacionAvisos preparacionAvisos;
    private final Clock clock;

    public PublicBookingService(JdbcTemplate jdbc, AgendaQueries agendaQueries,
                                ConsultorioQueries consultorioQueries, PacienteQueries pacienteQueries,
                                AgendaWriteCoordinator writeCoordinator, CredencialesGestion credencialesGestion,
                                PreparacionAvisos preparacionAvisos, Clock clock) {
        this.jdbc = jdbc;
        this.agendaQueries = agendaQueries;
        this.consultorioQueries = consultorioQueries;
        this.pacienteQueries = pacienteQueries;
        this.writeCoordinator = writeCoordinator;
        this.credencialesGestion = credencialesGestion;
        this.preparacionAvisos = preparacionAvisos;
        this.clock = clock;
    }

    public ReservaResponse reservar(ReservaInput input) {
        if (input == null || input.motivoId() == null || input.inicio() == null) {
            throw AvailabilityException.validation();
        }
        ValidatedPatient patient = AvailabilityRules.validatedPatient(
                input.nombre(), input.apellido(), input.email(), input.telefono(), input.dni());
        Instant inicio = input.inicio().truncatedTo(ChronoUnit.MICROS);
        if (!inicio.equals(input.inicio()) || inicio.getEpochSecond() % 60 != 0) {
            throw AvailabilityException.validation();
        }

        Configuracion beforeLock = configuration();
        try {
            return writeCoordinator.write(beforeLock.agenda().id(), () -> {
                Configuracion config = configuration();
                UUID agendaId = config.agenda().id();
                if (!agendaId.equals(beforeLock.agenda().id())) {
                    throw AvailabilityException.technicalFailure();
                }

                MotivoConsulta motivo = agendaQueries.findMotivo(agendaId, input.motivoId())
                        .orElseThrow(AvailabilityException::motivoNotFound);
                Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
                Instant fin = inicio.plus(motivo.duracionMinutos(), ChronoUnit.MINUTES);
                ZoneId zone = config.consultorio().zonaHoraria();

                List<FranjaSemanal> schedule = agendaQueries.franjas(agendaId);
                List<BloqueoAgenda> blocks = agendaQueries.bloqueos(agendaId, inicio, fin);
                List<Turno> occupied = agendaQueries.ocupados(agendaId, inicio, fin);

                if (!AvailabilityRules.isSlotAvailable(inicio, motivo.duracionMinutos(), schedule,
                        blocks, occupied, now, zone)) {
                    throw AvailabilityException.slotUnavailable();
                }

                Paciente persistedPatient = pacienteQueries
                        .findExact(config.consultorio().id(), patient.nombre(), patient.apellido(),
                                patient.email(), patient.telefono(), patient.dni())
                        .orElseGet(() -> pacienteQueries.insert(config.consultorio().id(),
                                patient.nombre(), patient.apellido(), patient.email(),
                                patient.telefono(), patient.dni()));

                UUID turnoId = UUID.randomUUID();
                jdbc.update("""
                        INSERT INTO turno (id, agenda_id, paciente_id, motivo_id, inicio, fin,
                                           duracion_reservada_minutos, estado, revision, creado_en)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'CONFIRMADO', 0, ?)
                        """, turnoId, agendaId, persistedPatient.id(), motivo.id(),
                        utc(inicio), utc(fin), motivo.duracionMinutos(), utc(now));

                UUID credencialId = credencialesGestion.emitir(turnoId);
                preparacionAvisos.reserva(
                        new PreparacionAvisos.Evento(UUID.randomUUID(), turnoId, 0, patient.email(), now),
                        inicio, credencialId);

                return new ReservaResponse(turnoId, motivo.id(), inicio, fin,
                        motivo.duracionMinutos(), "CONFIRMADO", "PENDIENTE");
            });
        } catch (AgendaWriteException writeFailure) {
            if (writeFailure.reason() == AgendaWriteException.Reason.OVERLAP
                    || writeFailure.reason() == AgendaWriteException.Reason.CONCURRENT_CONFLICT) {
                throw AvailabilityException.slotUnavailable();
            }
            throw writeFailure;
        }
    }

    private Configuracion configuration() {
        return consultorioQueries.findConfiguration().orElseThrow(AvailabilityException::technicalFailure);
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC);
    }
}
