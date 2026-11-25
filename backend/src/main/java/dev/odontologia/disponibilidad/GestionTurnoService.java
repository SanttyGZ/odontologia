package dev.odontologia.disponibilidad;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.odontologia.agenda.AgendaQueries;
import dev.odontologia.agenda.AgendaWriteCoordinator;
import dev.odontologia.agenda.AgendaWriteException;
import dev.odontologia.agenda.BloqueoAgenda;
import dev.odontologia.agenda.MotivoConsulta;
import dev.odontologia.agenda.Turno;
import dev.odontologia.comunicaciones.PreparacionAvisos;
import dev.odontologia.consultorio.ConsultorioQueries;
import dev.odontologia.consultorio.ConsultorioQueries.Configuracion;
import dev.odontologia.disponibilidad.GestionTurnoDtos.Cancelacion;
import dev.odontologia.disponibilidad.GestionTurnoDtos.Consulta;
import dev.odontologia.disponibilidad.GestionTurnoDtos.ConsultaResultado;
import dev.odontologia.disponibilidad.GestionTurnoDtos.Reprogramacion;
import dev.odontologia.disponibilidad.GestionTurnoDtos.ReprogramacionInput;
import dev.odontologia.disponibilidad.GestionTurnoDtos.ReprogramacionResultado;
import dev.odontologia.gestion.CredencialesGestion;
import dev.odontologia.gestion.CredencialesGestion.TurnoAutorizado;
import dev.odontologia.pacientes.PacienteQueries;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public final class GestionTurnoService {

    private static final List<String> ACCIONES_GESTIONABLES = List.of("CANCELAR", "REPROGRAMAR");
    private static final String NOTIFICACION_PENDIENTE = "PENDIENTE";

    private final JdbcTemplate jdbc;
    private final AgendaQueries agendaQueries;
    private final ConsultorioQueries consultorioQueries;
    private final PacienteQueries pacienteQueries;
    private final AgendaWriteCoordinator coordinador;
    private final CredencialesGestion credenciales;
    private final PreparacionAvisos preparacionAvisos;
    private final Clock clock;

    private record TurnoGestionado(Turno turno, UUID referenciaCredencial,
                                   Optional<String> destinatarioRecuperacion) {
    }

    public GestionTurnoService(JdbcTemplate jdbc, AgendaQueries agendaQueries,
                               ConsultorioQueries consultorioQueries, PacienteQueries pacienteQueries,
                               AgendaWriteCoordinator coordinador, CredencialesGestion credenciales,
                               PreparacionAvisos preparacionAvisos, Clock clock) {
        this.jdbc = jdbc;
        this.agendaQueries = agendaQueries;
        this.consultorioQueries = consultorioQueries;
        this.pacienteQueries = pacienteQueries;
        this.coordinador = coordinador;
        this.credenciales = credenciales;
        this.preparacionAvisos = preparacionAvisos;
        this.clock = clock;
    }

    public ConsultaResultado consultar(String secreto) {
        TurnoAutorizado autorizado = credenciales.validar(secreto)
                .orElseThrow(GestionTurnoException::linkNotAvailable);
        Configuracion config = configuracion();
        Turno turno = agendaQueries.findTurno(config.agenda().id(), autorizado.id())
                .filter(value -> value.estado() == Turno.Estado.CONFIRMADO)
                .orElseThrow(GestionTurnoException::linkNotAvailable);
        Instant ahora = ahora();
        Consulta consulta = consulta(turno.inicio(), turno.fin(), "CONFIRMADO",
                acciones(turno.inicio(), ahora), config.consultorio().zonaHoraria());
        return new ConsultaResultado(consulta,
                GestionTurnoEtag.generar(turno.id().toString(), turno.revision(), consulta));
    }

    public Cancelacion cancelar(String secreto, String ifMatch) {
        Configuracion antes = configuracion();
        try {
            return coordinador.write(antes.agenda().id(), () -> {
                Configuracion config = configuracion();
                exigirMismaAgenda(antes, config);
                TurnoGestionado gestionado = turnoAutorizado(secreto, config.agenda().id());
                Turno turno = gestionado.turno();
                Instant ahora = ahora();
                exigirPrecondicionPresente(ifMatch);
                exigirVentana(turno, ahora);
                exigirPrecondicion(ifMatch, turno, config.consultorio().zonaHoraria(), ahora);

                int nuevaRevision = Math.incrementExact(turno.revision());
                int escritos = jdbc.update("""
                        UPDATE turno SET estado = 'CANCELADO', cancelado_en = ?, revision = ?
                        WHERE id = ? AND agenda_id = ? AND estado = 'CONFIRMADO' AND revision = ?
                        """, utc(ahora), nuevaRevision, turno.id(), turno.agendaId(), turno.revision());
                if (escritos != 1) {
                    throw GestionTurnoException.versionMismatch();
                }

                credenciales.revocar(turno.id());
                prepararAviso(turno, nuevaRevision, ahora, null, null, gestionado.destinatarioRecuperacion());
                return cancelacion(turno.inicio(), turno.fin(), "CANCELADO", NOTIFICACION_PENDIENTE,
                        config.consultorio().zonaHoraria());
            });
        } catch (AgendaWriteException failure) {
            throw mapearFallo(failure, false);
        }
    }

    public ReprogramacionResultado reprogramar(String secreto, String ifMatch, ReprogramacionInput input) {
        if (input == null || input.nuevoInicio() == null) {
            throw GestionTurnoException.validation();
        }
        Configuracion antes = configuracion();
        try {
            return coordinador.write(antes.agenda().id(), () -> {
                Configuracion config = configuracion();
                exigirMismaAgenda(antes, config);
                TurnoGestionado gestionado = turnoAutorizado(secreto, config.agenda().id());
                Turno turno = gestionado.turno();
                Instant ahora = ahora();
                exigirPrecondicionPresente(ifMatch);
                exigirVentana(turno, ahora);
                exigirPrecondicion(ifMatch, turno, config.consultorio().zonaHoraria(), ahora);

                Instant nuevoInicio = input.nuevoInicio();
                MotivoConsulta motivo = agendaQueries.findMotivo(turno.agendaId(), turno.motivoId())
                        .orElseThrow(GestionTurnoException::technicalFailure);
                Instant nuevoFin;
                try {
                    nuevoFin = nuevoInicio.plus(motivo.duracionMinutos(), ChronoUnit.MINUTES);
                } catch (RuntimeException invalidRange) {
                    throw GestionTurnoException.slotUnavailable();
                }
                if (nuevoInicio.equals(turno.inicio()) && nuevoFin.equals(turno.fin())) {
                    throw GestionTurnoException.sameInterval();
                }
                List<BloqueoAgenda> bloqueos = agendaQueries.bloqueos(turno.agendaId(), nuevoInicio, nuevoFin);
                List<Turno> ocupados = agendaQueries.ocupados(turno.agendaId(), nuevoInicio, nuevoFin).stream()
                        .filter(other -> !other.id().equals(turno.id()))
                        .toList();
                if (!AvailabilityRules.isSlotAvailable(nuevoInicio, motivo.duracionMinutos(),
                        agendaQueries.franjas(turno.agendaId()), bloqueos, ocupados, ahora,
                        config.consultorio().zonaHoraria())) {
                    throw GestionTurnoException.slotUnavailable();
                }

                int nuevaRevision = Math.incrementExact(turno.revision());
                int escritos = jdbc.update("""
                        UPDATE turno SET inicio = ?, fin = ?, duracion_reservada_minutos = ?, revision = ?
                        WHERE id = ? AND agenda_id = ? AND estado = 'CONFIRMADO' AND revision = ?
                        """, utc(nuevoInicio), utc(nuevoFin), motivo.duracionMinutos(), nuevaRevision,
                        turno.id(), turno.agendaId(), turno.revision());
                if (escritos != 1) {
                    throw GestionTurnoException.versionMismatch();
                }

                prepararAviso(turno, nuevaRevision, ahora, nuevoInicio, gestionado.referenciaCredencial(),
                        gestionado.destinatarioRecuperacion());
                Consulta resultado = consulta(nuevoInicio, nuevoFin, "CONFIRMADO",
                        acciones(nuevoInicio, ahora), config.consultorio().zonaHoraria());
                String etag = GestionTurnoEtag.generar(turno.id().toString(), nuevaRevision, resultado);
                return new ReprogramacionResultado(reprogramacion(turno.id(), nuevoInicio, nuevoFin,
                        "CONFIRMADO", NOTIFICACION_PENDIENTE, config.consultorio().zonaHoraria()), etag);
            });
        } catch (AgendaWriteException failure) {
            throw mapearFallo(failure, true);
        }
    }

    private void prepararAviso(Turno turno, int revision, Instant ahora, Instant nuevoInicio, UUID credencialId,
                               Optional<String> destinatarioRecuperacion) {
        String email = destinatarioRecuperacion.orElseGet(() -> pacienteQueries.find(turno.pacienteId())
                .map(paciente -> paciente.email())
                .orElseThrow(GestionTurnoException::technicalFailure));
        preparacionAvisos.cambio(new PreparacionAvisos.Evento(UUID.randomUUID(), turno.id(), revision, email, ahora),
                nuevoInicio, credencialId);
    }

    private TurnoGestionado turnoAutorizado(String secreto, UUID agendaId) {
        TurnoAutorizado autorizado = credenciales.validar(secreto)
                .orElseThrow(GestionTurnoException::linkNotAvailable);
        Turno turno = agendaQueries.findTurno(agendaId, autorizado.id())
                .filter(value -> value.estado() == Turno.Estado.CONFIRMADO)
                .orElseThrow(GestionTurnoException::linkNotAvailable);
        if (turno.revision() != autorizado.revision()) {
            throw GestionTurnoException.versionMismatch();
        }
        return new TurnoGestionado(turno, autorizado.referenciaCredencial(), autorizado.destinatarioRecuperacion());
    }

    private static void exigirPrecondicionPresente(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw GestionTurnoException.preconditionRequired();
        }
    }

    private void exigirPrecondicion(String ifMatch, Turno turno, ZoneId zona, Instant ahora) {
        Consulta actual = consulta(turno.inicio(), turno.fin(), turno.estado().name(),
                acciones(turno.inicio(), ahora), zona);
        String etag = GestionTurnoEtag.generar(turno.id().toString(), turno.revision(), actual);
        if (!etag.equals(ifMatch.strip())) {
            throw GestionTurnoException.versionMismatch();
        }
    }

    private static void exigirVentana(Turno turno, Instant ahora) {
        if (turno.inicio().isBefore(ahora.plus(24, ChronoUnit.HOURS))) {
            throw GestionTurnoException.managementWindowClosed();
        }
    }

    private Configuracion configuracion() {
        return consultorioQueries.findConfiguration().orElseThrow(GestionTurnoException::technicalFailure);
    }

    private static void exigirMismaAgenda(Configuracion antes, Configuracion despues) {
        if (!antes.agenda().id().equals(despues.agenda().id())) {
            throw GestionTurnoException.technicalFailure();
        }
    }

    private Instant ahora() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private static Consulta consulta(Instant inicio, Instant fin, String estado, List<String> acciones, ZoneId zona) {
        var localInicio = inicio.atZone(zona);
        var localFin = fin.atZone(zona);
        return new Consulta(localInicio.toLocalDate(), localInicio.toLocalTime(), localFin.toLocalTime(),
                estado, acciones);
    }

    private static Cancelacion cancelacion(Instant inicio, Instant fin, String estado, String notificacion,
                                           ZoneId zona) {
        var localInicio = inicio.atZone(zona);
        var localFin = fin.atZone(zona);
        return new Cancelacion(localInicio.toLocalDate(), localInicio.toLocalTime(), localFin.toLocalTime(),
                estado, notificacion);
    }

    private static Reprogramacion reprogramacion(UUID turnoId, Instant inicio, Instant fin, String estado,
                                                 String notificacion, ZoneId zona) {
        var localInicio = inicio.atZone(zona);
        var localFin = fin.atZone(zona);
        return new Reprogramacion(turnoId, localInicio.toLocalDate(), localInicio.toLocalTime(),
                localFin.toLocalTime(), estado, notificacion);
    }

    private static List<String> acciones(Instant inicio, Instant ahora) {
        return inicio.isBefore(ahora.plus(24, ChronoUnit.HOURS)) ? List.of() : ACCIONES_GESTIONABLES;
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC);
    }

    private static GestionTurnoException mapearFallo(AgendaWriteException failure, boolean reprogramacion) {
        return switch (failure.reason()) {
            case LOCK_TIMEOUT -> GestionTurnoException.agendaBusy();
            case OVERLAP, CONCURRENT_CONFLICT -> reprogramacion
                    ? GestionTurnoException.slotUnavailable() : GestionTurnoException.technicalFailure();
            default -> GestionTurnoException.technicalFailure();
        };
    }
}
