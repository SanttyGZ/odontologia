package dev.odontologia.agenda;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.odontologia.agenda.DentistAgendaDtos.AgendaResponse;
import dev.odontologia.agenda.DentistAgendaDtos.AgendaTurnoResumenResponse;
import dev.odontologia.agenda.DentistAgendaDtos.PacienteBasicoResponse;
import dev.odontologia.agenda.DentistAgendaDtos.TurnoDetalleResponse;
import dev.odontologia.consultorio.Consultorio;
import dev.odontologia.consultorio.ConsultorioQueries;
import dev.odontologia.consultorio.ConsultorioQueries.Configuracion;
import dev.odontologia.consultorio.Odontologo;
import dev.odontologia.disponibilidad.AvailabilityException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DentistAgendaServiceTest {

    private static final ZoneId MENDOZA = ZoneId.of("America/Argentina/Mendoza");
    private static final UUID CONSULTORIO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ODONTOLOGO_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID AGENDA_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final AgendaQueries agendaQueries = mock(AgendaQueries.class);
    private final ConsultorioQueries consultorioQueries = mock(ConsultorioQueries.class);

    @Test
    void agendaDefaultsToDiaAndUsesConsultorioZoneAtUtcMidnightBoundary() {
        // 2030-01-08T01:30:00Z is 2030-01-07 22:30 in America/Argentina/Mendoza (UTC-3)
        Clock clock = Clock.fixed(Instant.parse("2030-01-08T01:30:00Z"), ZoneId.of("UTC"));
        DentistAgendaService service = new DentistAgendaService(agendaQueries, consultorioQueries, clock);
        when(consultorioQueries.findConfiguration()).thenReturn(Optional.of(configuration()));

        Instant expectedStart = Instant.parse("2030-01-07T03:00:00Z");
        Instant expectedEnd = Instant.parse("2030-01-08T03:00:00Z");
        List<AgendaTurnoResumenResponse> turnos = List.of(sampleResumen(UUID.randomUUID()));
        when(agendaQueries.findAgendaTurnos(AGENDA_ID, CONSULTORIO_ID, expectedStart, expectedEnd))
                .thenReturn(turnos);

        AgendaResponse response = service.agenda(null, null);

        assertThat(response.vista()).isEqualTo("DIA");
        assertThat(response.desde()).isEqualTo(LocalDate.of(2030, 1, 7));
        assertThat(response.hasta()).isEqualTo(LocalDate.of(2030, 1, 7));
        assertThat(response.zonaHoraria()).isEqualTo("America/Argentina/Mendoza");
        assertThat(response.turnos()).isEqualTo(turnos);
        verify(agendaQueries).findAgendaTurnos(AGENDA_ID, CONSULTORIO_ID, expectedStart, expectedEnd);
    }

    @Test
    void agendaDiaWithExplicitDateQueriesExactLocalDayBoundsInUtc() {
        Clock clock = Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"), ZoneId.of("UTC"));
        DentistAgendaService service = new DentistAgendaService(agendaQueries, consultorioQueries, clock);
        when(consultorioQueries.findConfiguration()).thenReturn(Optional.of(configuration()));

        LocalDate fecha = LocalDate.of(2030, 1, 8);
        Instant expectedStart = Instant.parse("2030-01-08T03:00:00Z");
        Instant expectedEnd = Instant.parse("2030-01-09T03:00:00Z");
        List<AgendaTurnoResumenResponse> turnos = List.of(sampleResumen(UUID.randomUUID()));
        when(agendaQueries.findAgendaTurnos(AGENDA_ID, CONSULTORIO_ID, expectedStart, expectedEnd))
                .thenReturn(turnos);

        AgendaResponse response = service.agenda("DIA", fecha);

        assertThat(response.vista()).isEqualTo("DIA");
        assertThat(response.desde()).isEqualTo(fecha);
        assertThat(response.hasta()).isEqualTo(fecha);
        assertThat(response.zonaHoraria()).isEqualTo("America/Argentina/Mendoza");
        assertThat(response.turnos()).isEqualTo(turnos);
        verify(agendaQueries).findAgendaTurnos(AGENDA_ID, CONSULTORIO_ID, expectedStart, expectedEnd);
    }

    @Test
    void agendaSemanaUsesIsoMondayToSundayForWednesdayAndSunday() {
        Clock clock = Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"), ZoneId.of("UTC"));
        DentistAgendaService service = new DentistAgendaService(agendaQueries, consultorioQueries, clock);
        when(consultorioQueries.findConfiguration()).thenReturn(Optional.of(configuration()));

        Instant expectedStart = Instant.parse("2030-01-07T03:00:00Z");
        Instant expectedEnd = Instant.parse("2030-01-14T03:00:00Z");
        List<AgendaTurnoResumenResponse> turnos = List.of(sampleResumen(UUID.randomUUID()));
        when(agendaQueries.findAgendaTurnos(AGENDA_ID, CONSULTORIO_ID, expectedStart, expectedEnd))
                .thenReturn(turnos);

        // Wednesday 2030-01-09 -> Monday 2030-01-07 .. Sunday 2030-01-13
        AgendaResponse wednesdayResponse = service.agenda("SEMANA", LocalDate.of(2030, 1, 9));
        assertThat(wednesdayResponse.vista()).isEqualTo("SEMANA");
        assertThat(wednesdayResponse.desde()).isEqualTo(LocalDate.of(2030, 1, 7));
        assertThat(wednesdayResponse.hasta()).isEqualTo(LocalDate.of(2030, 1, 13));
        assertThat(wednesdayResponse.turnos()).isEqualTo(turnos);

        // Sunday 2030-01-13 -> Monday 2030-01-07 .. Sunday 2030-01-13
        AgendaResponse sundayResponse = service.agenda("SEMANA", LocalDate.of(2030, 1, 13));
        assertThat(sundayResponse.vista()).isEqualTo("SEMANA");
        assertThat(sundayResponse.desde()).isEqualTo(LocalDate.of(2030, 1, 7));
        assertThat(sundayResponse.hasta()).isEqualTo(LocalDate.of(2030, 1, 13));
        assertThat(sundayResponse.turnos()).isEqualTo(turnos);
    }

    @Test
    void agendaRejectsInvalidVistaWithValidationError() {
        Clock clock = Clock.fixed(Instant.parse("2030-01-08T12:00:00Z"), ZoneId.of("UTC"));
        DentistAgendaService service = new DentistAgendaService(agendaQueries, consultorioQueries, clock);

        for (String invalidVista : List.of("MES", "", "dia")) {
            assertThatThrownBy(() -> service.agenda(invalidVista, LocalDate.of(2030, 1, 8)))
                    .isInstanceOf(AvailabilityException.class)
                    .satisfies(ex -> {
                        AvailabilityException ae = (AvailabilityException) ex;
                        assertThat(ae.codigo()).isEqualTo("VALIDATION_ERROR");
                        assertThat(ae.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }
        verifyNoInteractions(consultorioQueries, agendaQueries);
    }

    @Test
    void agendaThrowsTechnicalFailureWhenConfigurationIsMissing() {
        Clock clock = Clock.fixed(Instant.parse("2030-01-08T12:00:00Z"), ZoneId.of("UTC"));
        DentistAgendaService service = new DentistAgendaService(agendaQueries, consultorioQueries, clock);
        when(consultorioQueries.findConfiguration()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.agenda("DIA", LocalDate.of(2030, 1, 8)))
                .isInstanceOf(AvailabilityException.class)
                .satisfies(ex -> {
                    AvailabilityException ae = (AvailabilityException) ex;
                    assertThat(ae.codigo()).isEqualTo("TECHNICAL_FAILURE");
                    assertThat(ae.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                });
        verifyNoInteractions(agendaQueries);
    }

    @Test
    void turnoDetalleReturnsDetailWhenFound() {
        Clock clock = Clock.fixed(Instant.parse("2030-01-08T12:00:00Z"), ZoneId.of("UTC"));
        DentistAgendaService service = new DentistAgendaService(agendaQueries, consultorioQueries, clock);
        when(consultorioQueries.findConfiguration()).thenReturn(Optional.of(configuration()));

        UUID turnoId = UUID.randomUUID();
        TurnoDetalleResponse detalle = new TurnoDetalleResponse(
                turnoId,
                UUID.randomUUID(),
                "Limpieza",
                Instant.parse("2030-01-08T13:00:00Z"),
                Instant.parse("2030-01-08T13:30:00Z"),
                30,
                Turno.Estado.CONFIRMADO,
                1,
                Instant.parse("2030-01-05T10:00:00Z"),
                null,
                new PacienteBasicoResponse("Ana", "Gomez", "ana@example.com", "2615551234", "30123456"));
        when(agendaQueries.findTurnoDetalle(AGENDA_ID, CONSULTORIO_ID, turnoId))
                .thenReturn(Optional.of(detalle));

        TurnoDetalleResponse result = service.turnoDetalle(turnoId);

        assertThat(result).isEqualTo(detalle);
        verify(agendaQueries).findTurnoDetalle(AGENDA_ID, CONSULTORIO_ID, turnoId);
    }

    @Test
    void turnoDetalleRejectsNullTurnoIdWithValidationError() {
        Clock clock = Clock.fixed(Instant.parse("2030-01-08T12:00:00Z"), ZoneId.of("UTC"));
        DentistAgendaService service = new DentistAgendaService(agendaQueries, consultorioQueries, clock);

        assertThatThrownBy(() -> service.turnoDetalle(null))
                .isInstanceOf(AvailabilityException.class)
                .satisfies(ex -> {
                    AvailabilityException ae = (AvailabilityException) ex;
                    assertThat(ae.codigo()).isEqualTo("VALIDATION_ERROR");
                    assertThat(ae.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
        verifyNoInteractions(consultorioQueries, agendaQueries);
    }

    @Test
    void turnoDetalleThrowsTechnicalFailureWhenConfigurationIsMissing() {
        Clock clock = Clock.fixed(Instant.parse("2030-01-08T12:00:00Z"), ZoneId.of("UTC"));
        DentistAgendaService service = new DentistAgendaService(agendaQueries, consultorioQueries, clock);
        when(consultorioQueries.findConfiguration()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.turnoDetalle(UUID.randomUUID()))
                .isInstanceOf(AvailabilityException.class)
                .satisfies(ex -> {
                    AvailabilityException ae = (AvailabilityException) ex;
                    assertThat(ae.codigo()).isEqualTo("TECHNICAL_FAILURE");
                    assertThat(ae.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                });
        verifyNoInteractions(agendaQueries);
    }

    @Test
    void turnoDetalleThrowsTurnoNotFoundWhenMissing() {
        Clock clock = Clock.fixed(Instant.parse("2030-01-08T12:00:00Z"), ZoneId.of("UTC"));
        DentistAgendaService service = new DentistAgendaService(agendaQueries, consultorioQueries, clock);
        when(consultorioQueries.findConfiguration()).thenReturn(Optional.of(configuration()));

        UUID turnoId = UUID.randomUUID();
        when(agendaQueries.findTurnoDetalle(AGENDA_ID, CONSULTORIO_ID, turnoId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.turnoDetalle(turnoId))
                .isInstanceOf(AvailabilityException.class)
                .satisfies(ex -> {
                    AvailabilityException ae = (AvailabilityException) ex;
                    assertThat(ae.codigo()).isEqualTo("TURNO_NOT_FOUND");
                    assertThat(ae.status()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    private static Configuracion configuration() {
        return new Configuracion(
                new Consultorio(CONSULTORIO_ID, "Consultorio Central", MENDOZA),
                new Odontologo(ODONTOLOGO_ID, CONSULTORIO_ID, "Dra. Perez"),
                new Agenda(AGENDA_ID, ODONTOLOGO_ID));
    }

    private static AgendaTurnoResumenResponse sampleResumen(UUID turnoId) {
        return new AgendaTurnoResumenResponse(
                turnoId,
                UUID.randomUUID(),
                "Control",
                Instant.parse("2030-01-08T13:00:00Z"),
                Instant.parse("2030-01-08T13:30:00Z"),
                30,
                Turno.Estado.CONFIRMADO,
                "Juan",
                "Perez");
    }
}
