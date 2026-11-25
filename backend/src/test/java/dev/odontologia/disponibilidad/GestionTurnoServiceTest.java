package dev.odontologia.disponibilidad;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import dev.odontologia.agenda.Agenda;
import dev.odontologia.agenda.AgendaQueries;
import dev.odontologia.agenda.AgendaWriteCoordinator;
import dev.odontologia.agenda.FranjaSemanal;
import dev.odontologia.agenda.MotivoConsulta;
import dev.odontologia.agenda.Turno;
import dev.odontologia.comunicaciones.PreparacionAvisos;
import dev.odontologia.consultorio.Consultorio;
import dev.odontologia.consultorio.ConsultorioQueries;
import dev.odontologia.consultorio.Odontologo;
import dev.odontologia.disponibilidad.GestionTurnoDtos.Cancelacion;
import dev.odontologia.disponibilidad.GestionTurnoDtos.ReprogramacionInput;
import dev.odontologia.gestion.CredencialesGestion;
import dev.odontologia.pacientes.Paciente;
import dev.odontologia.pacientes.PacienteQueries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GestionTurnoServiceTest {

    private static final UUID CONSULTORIO_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ODONTOLOGO_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID AGENDA_ID = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID TURNO_ID = UUID.fromString("20000000-0000-0000-0000-000000000004");
    private static final UUID PACIENTE_ID = UUID.fromString("20000000-0000-0000-0000-000000000005");
    private static final UUID MOTIVO_ID = UUID.fromString("20000000-0000-0000-0000-000000000006");
    private static final UUID CREDENCIAL_ID = UUID.fromString("20000000-0000-0000-0000-000000000007");
    private static final String TOKEN = "A".repeat(43);
    private static final Instant AHORA = Instant.parse("2030-01-07T12:00:00Z");
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Mendoza");

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AgendaQueries agendaQueries = mock(AgendaQueries.class);
    private final ConsultorioQueries consultorioQueries = mock(ConsultorioQueries.class);
    private final PacienteQueries pacienteQueries = mock(PacienteQueries.class);
    private final AgendaWriteCoordinator coordinador = mock(AgendaWriteCoordinator.class);
    private final CredencialesGestion credenciales = mock(CredencialesGestion.class);
    private final PreparacionAvisos avisos = mock(PreparacionAvisos.class);
    private final GestionTurnoService service = new GestionTurnoService(jdbc, agendaQueries, consultorioQueries,
            pacienteQueries, coordinador, credenciales, avisos, Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        var config = new ConsultorioQueries.Configuracion(
                new Consultorio(CONSULTORIO_ID, "Consultorio", ZONA),
                new Odontologo(ODONTOLOGO_ID, CONSULTORIO_ID, "Odontólogo"),
                new Agenda(AGENDA_ID, ODONTOLOGO_ID));
        when(consultorioQueries.findConfiguration()).thenReturn(Optional.of(config));
        when(pacienteQueries.find(PACIENTE_ID)).thenReturn(Optional.of(new Paciente(PACIENTE_ID, CONSULTORIO_ID,
                "Ana", "Perez", "ana@example.com", "2615551234", "30123456")));
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get())
                .when(coordinador).write(any(UUID.class), any());
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
    }

    @Test
    void cancelsAtTheInclusive24HourBoundaryAndPreparesOnePendingNotice() {
        Turno turno = turno(AHORA.plusSeconds(24 * 60 * 60), 5, Turno.Estado.CONFIRMADO);
        autorizar(turno, turno.revision());
        String etag = service.consultar(TOKEN).etag();

        Cancelacion result = service.cancelar(TOKEN, etag);

        assertThat(result.estado()).isEqualTo("CANCELADO");
        assertThat(result.notificacionEstado()).isEqualTo("PENDIENTE");
        verify(jdbc).update(contains("UPDATE turno SET estado = 'CANCELADO'"), any(Object[].class));
        verify(credenciales).revocar(TURNO_ID);
        verify(avisos).cambio(any(PreparacionAvisos.Evento.class), isNull(), isNull());
    }

    @Test
    void rejectsCancellationOneSecondInsideTheClosedWindowWithoutWrites() {
        Turno turno = turno(AHORA.plusSeconds(24 * 60 * 60 - 1), 5, Turno.Estado.CONFIRMADO);
        autorizar(turno, turno.revision());
        String etag = service.consultar(TOKEN).etag();

        assertThatThrownBy(() -> service.cancelar(TOKEN, etag))
                .isInstanceOfSatisfying(GestionTurnoException.class,
                        failure -> assertThat(failure.codigo())
                                .isEqualTo(GestionTurnoException.Codigo.MANAGEMENT_WINDOW_CLOSED));

        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verify(credenciales, never()).revocar(any(UUID.class));
        verify(avisos, never()).cambio(any(), any(), any());
    }

    @Test
    void requiresIfMatchAndRejectsAStaleTagBeforeWriting() {
        Turno turno = turno(AHORA.plusSeconds(48 * 60 * 60), 5, Turno.Estado.CONFIRMADO);
        autorizar(turno, turno.revision());

        assertThatThrownBy(() -> service.cancelar(TOKEN, null))
                .isInstanceOfSatisfying(GestionTurnoException.class,
                        failure -> assertThat(failure.codigo())
                                .isEqualTo(GestionTurnoException.Codigo.PRECONDITION_REQUIRED));

        String staleEtag = "\"stale-version\"";
        assertThatThrownBy(() -> service.cancelar(TOKEN, staleEtag))
                .isInstanceOfSatisfying(GestionTurnoException.class,
                        failure -> assertThat(failure.codigo())
                                .isEqualTo(GestionTurnoException.Codigo.TURN_VERSION_MISMATCH));

        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verify(credenciales, never()).revocar(any(UUID.class));
        verify(avisos, never()).cambio(any(), any(), any());
    }

    @Test
    void rejectsReprogrammingToTheSameIntervalAfterCalculatingCurrentDuration() {
        Turno turno = turno(AHORA.plusSeconds(48 * 60 * 60), 5, Turno.Estado.CONFIRMADO);
        autorizar(turno, turno.revision());
        String etag = service.consultar(TOKEN).etag();
        when(agendaQueries.findMotivo(AGENDA_ID, MOTIVO_ID))
                .thenReturn(Optional.of(new MotivoConsulta(MOTIVO_ID, AGENDA_ID, "Control", 30)));
        when(agendaQueries.franjas(AGENDA_ID)).thenReturn(List.of(new FranjaSemanal(
                UUID.randomUUID(), AGENDA_ID, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0),
                LocalTime.of(17, 0), FranjaSemanal.Tipo.ATENCION)));
        when(agendaQueries.bloqueos(eq(AGENDA_ID), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(agendaQueries.ocupados(eq(AGENDA_ID), any(Instant.class), any(Instant.class))).thenReturn(List.of());

        assertThatThrownBy(() -> service.reprogramar(TOKEN, etag, new ReprogramacionInput(turno.inicio())))
                .isInstanceOfSatisfying(GestionTurnoException.class,
                        failure -> assertThat(failure.codigo().name()).isEqualTo("SAME_INTERVAL"));

        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verify(avisos, never()).cambio(any(), any(), any());
    }

    @Test
    void permitsTheSameStartWhenCurrentMotifDurationProducesADifferentEnd() {
        Instant start = Instant.parse("2030-01-09T12:00:00Z");
        Turno turno = turno(start, 5, Turno.Estado.CONFIRMADO);
        autorizar(turno, turno.revision());
        String etag = service.consultar(TOKEN).etag();
        when(agendaQueries.findMotivo(AGENDA_ID, MOTIVO_ID))
                .thenReturn(Optional.of(new MotivoConsulta(MOTIVO_ID, AGENDA_ID, "Control", 45)));
        when(agendaQueries.franjas(AGENDA_ID)).thenReturn(List.of(new FranjaSemanal(
                UUID.randomUUID(), AGENDA_ID, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0),
                LocalTime.of(17, 0), FranjaSemanal.Tipo.ATENCION)));
        when(agendaQueries.bloqueos(eq(AGENDA_ID), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(agendaQueries.ocupados(eq(AGENDA_ID), any(Instant.class), any(Instant.class))).thenReturn(List.of());

        var result = service.reprogramar(TOKEN, etag, new ReprogramacionInput(start));

        assertThat(result.cuerpo().horaInicio()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.cuerpo().horaFin()).isEqualTo(LocalTime.of(9, 45));
    }

    @Test
    void hidesAnUnavailableCredentialAndRejectsAnUnavailableRevision() {
        when(credenciales.validar(TOKEN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancelar(TOKEN, "\"opaque\""))
                .isInstanceOfSatisfying(GestionTurnoException.class,
                        failure -> assertThat(failure.codigo())
                                .isEqualTo(GestionTurnoException.Codigo.LINK_NOT_AVAILABLE));

        Turno turno = turno(AHORA.plusSeconds(48 * 60 * 60), 5, Turno.Estado.CONFIRMADO);
        autorizar(turno, turno.revision() - 1);
        assertThatThrownBy(() -> service.cancelar(TOKEN, "\"opaque\""))
                .isInstanceOfSatisfying(GestionTurnoException.class,
                        failure -> assertThat(failure.codigo())
                                .isEqualTo(GestionTurnoException.Codigo.TURN_VERSION_MISMATCH));

        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void reprogramsWithCurrentMotifDurationOnTheInclusive30thLocalDayAndKeepsTurnCredential() {
        Instant currentStart = Instant.parse("2030-01-11T12:00:00Z");
        Turno turno = turno(currentStart, 5, Turno.Estado.CONFIRMADO);
        autorizar(turno, turno.revision());
        String etag = service.consultar(TOKEN).etag();
        Instant newStart = Instant.parse("2030-02-06T19:00:00Z"); // 16:00 local, 30th calendar day.
        when(agendaQueries.findMotivo(AGENDA_ID, MOTIVO_ID))
                .thenReturn(Optional.of(new MotivoConsulta(MOTIVO_ID, AGENDA_ID, "Control", 45)));
        when(agendaQueries.franjas(AGENDA_ID)).thenReturn(List.of(new FranjaSemanal(
                UUID.randomUUID(), AGENDA_ID, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0),
                LocalTime.of(17, 0), FranjaSemanal.Tipo.ATENCION)));
        when(agendaQueries.bloqueos(eq(AGENDA_ID), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(agendaQueries.ocupados(eq(AGENDA_ID), any(Instant.class), any(Instant.class))).thenReturn(List.of());

        var result = service.reprogramar(TOKEN, etag, new ReprogramacionInput(newStart));

        assertThat(result.cuerpo().turnoId()).isEqualTo(TURNO_ID);
        assertThat(result.cuerpo().fecha()).isEqualTo(java.time.LocalDate.of(2030, 2, 6));
        assertThat(result.cuerpo().horaInicio()).isEqualTo(LocalTime.of(16, 0));
        assertThat(result.cuerpo().horaFin()).isEqualTo(LocalTime.of(16, 45));
        assertThat(result.cuerpo().estado()).isEqualTo("CONFIRMADO");
        assertThat(result.cuerpo().notificacionEstado()).isEqualTo("PENDIENTE");
        assertThat(result.etag()).matches("\\\"[A-Za-z0-9_-]{43}\\\"");
        verify(jdbc).update(contains("duracion_reservada_minutos = ?"), any(Object[].class));
        verify(avisos).cambio(any(PreparacionAvisos.Evento.class), eq(newStart), eq(CREDENCIAL_ID));
    }

    @Test
    void rejectsReprogrammingAfterTheInclusive30DayWindowWithoutChangingTheCurrentTurn() {
        Turno turno = turno(Instant.parse("2030-01-11T12:00:00Z"), 5, Turno.Estado.CONFIRMADO);
        autorizar(turno, turno.revision());
        String etag = service.consultar(TOKEN).etag();
        Instant beyondWindow = Instant.parse("2030-02-07T12:00:00Z");
        when(agendaQueries.findMotivo(AGENDA_ID, MOTIVO_ID))
                .thenReturn(Optional.of(new MotivoConsulta(MOTIVO_ID, AGENDA_ID, "Control", 30)));
        when(agendaQueries.franjas(AGENDA_ID)).thenReturn(List.of());
        when(agendaQueries.bloqueos(eq(AGENDA_ID), any(Instant.class), any(Instant.class))).thenReturn(List.of());
        when(agendaQueries.ocupados(eq(AGENDA_ID), any(Instant.class), any(Instant.class))).thenReturn(List.of());

        assertThatThrownBy(() -> service.reprogramar(TOKEN, etag, new ReprogramacionInput(beyondWindow)))
                .isInstanceOfSatisfying(GestionTurnoException.class,
                        failure -> assertThat(failure.codigo())
                                .isEqualTo(GestionTurnoException.Codigo.SLOT_UNAVAILABLE));

        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verify(avisos, never()).cambio(any(), any(), any());
    }

    @Test
    void doesNotManageARevokedOrNonconfirmedTurn() {
        Turno canceled = turno(AHORA.plusSeconds(48 * 60 * 60), 5, Turno.Estado.CANCELADO);
        when(credenciales.validar(TOKEN)).thenReturn(Optional.of(new CredencialesGestion.TurnoAutorizado(
                TURNO_ID, canceled.inicio(), canceled.fin(), canceled.revision(), CREDENCIAL_ID, Optional.empty())));
        when(agendaQueries.findTurno(AGENDA_ID, TURNO_ID)).thenReturn(Optional.of(canceled));

        assertThatThrownBy(() -> service.cancelar(TOKEN, "\"opaque\""))
                .isInstanceOfSatisfying(GestionTurnoException.class,
                        failure -> assertThat(failure.codigo())
                                .isEqualTo(GestionTurnoException.Codigo.LINK_NOT_AVAILABLE));

        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verify(credenciales, never()).revocar(any(UUID.class));
    }

    private void autorizar(Turno turno, int revisionAutorizada) {
        when(credenciales.validar(TOKEN)).thenReturn(Optional.of(new CredencialesGestion.TurnoAutorizado(
                TURNO_ID, turno.inicio(), turno.fin(), revisionAutorizada, CREDENCIAL_ID, Optional.empty())));
        when(agendaQueries.findTurno(AGENDA_ID, TURNO_ID)).thenReturn(Optional.of(turno));
    }

    private static Turno turno(Instant inicio, int revision, Turno.Estado estado) {
        return new Turno(TURNO_ID, AGENDA_ID, PACIENTE_ID, MOTIVO_ID, inicio,
                inicio.plusSeconds(30 * 60), 30, estado, revision, AHORA.minusSeconds(7 * 24 * 60 * 60), null);
    }
}
