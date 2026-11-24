package dev.odontologia.disponibilidad;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

import dev.odontologia.agenda.AgendaQueries;
import dev.odontologia.agenda.AgendaWriteCoordinator;
import dev.odontologia.agenda.BloqueoAgenda;
import dev.odontologia.agenda.FranjaSemanal;
import dev.odontologia.agenda.MotivoConsulta;
import dev.odontologia.agenda.Turno;
import dev.odontologia.consultorio.ConsultorioQueries;
import dev.odontologia.consultorio.ConsultorioQueries.Configuracion;
import dev.odontologia.disponibilidad.AvailabilityDtos.FranjaInput;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public final class AvailabilityConfigurationService {

    private final JdbcTemplate jdbc;
    private final AgendaQueries agendaQueries;
    private final ConsultorioQueries consultorioQueries;
    private final AgendaWriteCoordinator writeCoordinator;

    public AvailabilityConfigurationService(JdbcTemplate jdbc, AgendaQueries agendaQueries,
                                            ConsultorioQueries consultorioQueries,
                                            AgendaWriteCoordinator writeCoordinator) {
        this.jdbc = jdbc;
        this.agendaQueries = agendaQueries;
        this.consultorioQueries = consultorioQueries;
        this.writeCoordinator = writeCoordinator;
    }

    public List<MotivoConsulta> motivos() {
        Configuracion config = configuration();
        return agendaQueries.motivos(config.agenda().id());
    }

    public MotivoConsulta createMotivo(String nombre, Integer duracionMinutos) {
        AvailabilityRules.validateMotivo(nombre, duracionMinutos);
        UUID id = UUID.randomUUID();
        return write(config -> {
            jdbc.update("""
                    INSERT INTO motivo_consulta (id, agenda_id, nombre, duracion_minutos)
                    VALUES (?, ?, ?, ?)
                    """, id, config.agenda().id(), nombre, duracionMinutos);
            return new MotivoConsulta(id, config.agenda().id(), nombre, duracionMinutos);
        });
    }

    public MotivoConsulta updateMotivo(UUID motivoId, String nombre, Integer duracionMinutos) {
        AvailabilityRules.validateMotivo(nombre, duracionMinutos);
        if (motivoId == null) {
            throw AvailabilityException.validation();
        }
        return write(config -> {
            int updated = jdbc.update("""
                    UPDATE motivo_consulta SET nombre = ?, duracion_minutos = ?
                    WHERE agenda_id = ? AND id = ?
                    """, nombre, duracionMinutos, config.agenda().id(), motivoId);
            if (updated == 0) {
                throw AvailabilityException.motivoNotFound();
            }
            return new MotivoConsulta(motivoId, config.agenda().id(), nombre, duracionMinutos);
        });
    }

    public List<FranjaSemanal> horario() {
        Configuracion config = configuration();
        return agendaQueries.franjas(config.agenda().id());
    }

    public List<FranjaSemanal> replaceHorario(List<FranjaInput> input) {
        Configuracion initial = configuration();
        List<FranjaSemanal> candidate = AvailabilityRules.validatedSchedule(input, initial.agenda().id());
        return write(config -> {
            List<Turno> confirmed = agendaQueries.confirmados(config.agenda().id());
            for (Turno turn : confirmed) {
                if (!AvailabilityRules.coversConfirmedTurn(turn, candidate, config.consultorio().zonaHoraria())) {
                    throw AvailabilityException.configurationConflict();
                }
            }
            jdbc.update("DELETE FROM franja_semanal WHERE agenda_id = ?", config.agenda().id());
            if (!candidate.isEmpty()) {
                List<Object[]> rows = new ArrayList<>(candidate.size());
                for (FranjaSemanal range : candidate) {
                    rows.add(new Object[]{range.id(), range.agendaId(), range.diaSemana().getValue(),
                            range.horaInicio(), range.horaFin(), range.tipo().name()});
                }
                jdbc.batchUpdate("""
                        INSERT INTO franja_semanal (id, agenda_id, dia_semana, hora_inicio, hora_fin, tipo)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """, rows);
            }
            return agendaQueries.franjas(config.agenda().id());
        });
    }

    public List<BloqueoAgenda> bloqueos() {
        Configuracion config = configuration();
        return agendaQueries.bloqueos(config.agenda().id());
    }

    public BloqueoAgenda createBloqueo(java.time.Instant inicio, java.time.Instant fin, String descripcion) {
        AvailabilityRules.validateBlock(inicio, fin);
        UUID id = UUID.randomUUID();
        return write(config -> {
            BloqueoAgenda bloqueo = new BloqueoAgenda(id, config.agenda().id(), inicio, fin, descripcion);
            if (!agendaQueries.ocupados(config.agenda().id(), bloqueo.inicio(), bloqueo.fin()).isEmpty()) {
                throw AvailabilityException.configurationConflict();
            }
            jdbc.update("""
                    INSERT INTO bloqueo_agenda (id, agenda_id, inicio, fin, descripcion)
                    VALUES (?, ?, ?, ?, ?)
                    """, id, config.agenda().id(),
                    bloqueo.inicio().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC),
                    bloqueo.fin().truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC), descripcion);
            return bloqueo;
        });
    }

    public void deleteBloqueo(UUID bloqueoId) {
        if (bloqueoId == null) {
            throw AvailabilityException.validation();
        }
        write(config -> {
            int deleted = jdbc.update("DELETE FROM bloqueo_agenda WHERE agenda_id = ? AND id = ?",
                    config.agenda().id(), bloqueoId);
            if (deleted == 0) {
                throw AvailabilityException.bloqueoNotFound();
            }
            return null;
        });
    }

    private <T> T write(Function<Configuracion, T> work) {
        Configuracion beforeLock = configuration();
        return writeCoordinator.write(beforeLock.agenda().id(), () -> {
            Configuracion afterLock = configuration();
            if (!afterLock.agenda().id().equals(beforeLock.agenda().id())) {
                throw AvailabilityException.technicalFailure();
            }
            return work.apply(afterLock);
        });
    }

    private Configuracion configuration() {
        return consultorioQueries.findConfiguration().orElseThrow(AvailabilityException::technicalFailure);
    }
}