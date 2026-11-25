package dev.odontologia.agenda;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import dev.odontologia.agenda.DentistAgendaDtos.AgendaResponse;
import dev.odontologia.agenda.DentistAgendaDtos.AgendaTurnoResumenResponse;
import dev.odontologia.agenda.DentistAgendaDtos.PacienteBasicoResponse;
import dev.odontologia.agenda.DentistAgendaDtos.TurnoDetalleResponse;
import dev.odontologia.disponibilidad.AvailabilityErrorAdvice;
import dev.odontologia.disponibilidad.AvailabilityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DentistAgendaControllerTest {

    private static final UUID TURNO_CONFIRMADO_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID TURNO_CANCELADO_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID MOTIVO_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private DentistAgendaService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(DentistAgendaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new DentistAgendaController(service))
                .setControllerAdvice(new AvailabilityErrorAdvice())
                .build();
    }

    @Test
    void getAgendaWithoutParamsReturnsDefaultDayViewAndTurnos() throws Exception {
        var confirmado = new AgendaTurnoResumenResponse(
                TURNO_CONFIRMADO_ID,
                MOTIVO_ID,
                "Limpieza",
                Instant.parse("2030-01-09T13:00:00Z"),
                Instant.parse("2030-01-09T13:30:00Z"),
                30,
                Turno.Estado.CONFIRMADO,
                "Ana",
                "Perez");
        var cancelado = new AgendaTurnoResumenResponse(
                TURNO_CANCELADO_ID,
                MOTIVO_ID,
                "Control",
                Instant.parse("2030-01-09T14:00:00Z"),
                Instant.parse("2030-01-09T14:30:00Z"),
                30,
                Turno.Estado.CANCELADO,
                "Luis",
                "Gomez");
        when(service.agenda(null, null)).thenReturn(new AgendaResponse(
                "DIA",
                LocalDate.of(2030, 1, 9),
                LocalDate.of(2030, 1, 9),
                "America/Argentina/Mendoza",
                List.of(confirmado, cancelado)));

        mvc.perform(get("/api/odontologo/agenda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vista").value("DIA"))
                .andExpect(jsonPath("$.desde").value("2030-01-09"))
                .andExpect(jsonPath("$.hasta").value("2030-01-09"))
                .andExpect(jsonPath("$.zonaHoraria").value("America/Argentina/Mendoza"))
                .andExpect(jsonPath("$.agendaId").doesNotExist())
                .andExpect(jsonPath("$.consultorioId").doesNotExist())
                .andExpect(jsonPath("$.turnos.length()").value(2))
                .andExpect(jsonPath("$.turnos[0].id").value(TURNO_CONFIRMADO_ID.toString()))
                .andExpect(jsonPath("$.turnos[0].motivoId").value(MOTIVO_ID.toString()))
                .andExpect(jsonPath("$.turnos[0].motivoNombre").value("Limpieza"))
                .andExpect(jsonPath("$.turnos[0].inicio").value("2030-01-09T13:00:00Z"))
                .andExpect(jsonPath("$.turnos[0].fin").value("2030-01-09T13:30:00Z"))
                .andExpect(jsonPath("$.turnos[0].duracionReservadaMinutos").value(30))
                .andExpect(jsonPath("$.turnos[0].estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$.turnos[0].pacienteNombre").value("Ana"))
                .andExpect(jsonPath("$.turnos[0].pacienteApellido").value("Perez"))
                .andExpect(jsonPath("$.turnos[0].agendaId").doesNotExist())
                .andExpect(jsonPath("$.turnos[0].consultorioId").doesNotExist())
                .andExpect(jsonPath("$.turnos[1].id").value(TURNO_CANCELADO_ID.toString()))
                .andExpect(jsonPath("$.turnos[1].estado").value("CANCELADO"))
                .andExpect(jsonPath("$.turnos[1].agendaId").doesNotExist())
                .andExpect(jsonPath("$.turnos[1].consultorioId").doesNotExist());

        verify(service).agenda(null, null);
    }

    @Test
    void getAgendaWithWeekAndDateReturnsExactRangeAndEmptyListWhenNoTurnos() throws Exception {
        LocalDate fecha = LocalDate.of(2030, 1, 9);
        when(service.agenda("SEMANA", fecha)).thenReturn(new AgendaResponse(
                "SEMANA",
                LocalDate.of(2030, 1, 7),
                LocalDate.of(2030, 1, 13),
                "America/Argentina/Mendoza",
                List.of()));

        mvc.perform(get("/api/odontologo/agenda")
                        .param("vista", "SEMANA")
                        .param("fecha", "2030-01-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vista").value("SEMANA"))
                .andExpect(jsonPath("$.desde").value("2030-01-07"))
                .andExpect(jsonPath("$.hasta").value("2030-01-13"))
                .andExpect(jsonPath("$.zonaHoraria").value("America/Argentina/Mendoza"))
                .andExpect(jsonPath("$.turnos").isArray())
                .andExpect(jsonPath("$.turnos").isEmpty())
                .andExpect(jsonPath("$.agendaId").doesNotExist())
                .andExpect(jsonPath("$.consultorioId").doesNotExist());

        verify(service).agenda("SEMANA", fecha);
    }

    @Test
    void getTurnoDetalleReturnsExactDetailWithoutInternalOrClinicalFields() throws Exception {
        Instant inicio = Instant.parse("2030-01-09T13:00:00Z");
        Instant fin = Instant.parse("2030-01-09T13:30:00Z");
        Instant creadoEn = Instant.parse("2030-01-01T10:00:00Z");
        Instant canceladoEn = Instant.parse("2030-01-05T11:00:00Z");
        when(service.turnoDetalle(TURNO_CANCELADO_ID)).thenReturn(new TurnoDetalleResponse(
                TURNO_CANCELADO_ID,
                MOTIVO_ID,
                "Limpieza",
                inicio,
                fin,
                30,
                Turno.Estado.CANCELADO,
                2,
                creadoEn,
                canceladoEn,
                new PacienteBasicoResponse("Ana", "Perez", "ana@example.com", "+5492610000000", "30111222")));

        mvc.perform(get("/api/odontologo/turnos/{turnoId}", TURNO_CANCELADO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TURNO_CANCELADO_ID.toString()))
                .andExpect(jsonPath("$.motivoId").value(MOTIVO_ID.toString()))
                .andExpect(jsonPath("$.motivoNombre").value("Limpieza"))
                .andExpect(jsonPath("$.inicio").value("2030-01-09T13:00:00Z"))
                .andExpect(jsonPath("$.fin").value("2030-01-09T13:30:00Z"))
                .andExpect(jsonPath("$.duracionReservadaMinutos").value(30))
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.revision").value(2))
                .andExpect(jsonPath("$.creadoEn").value("2030-01-01T10:00:00Z"))
                .andExpect(jsonPath("$.canceladoEn").value("2030-01-05T11:00:00Z"))
                .andExpect(jsonPath("$.paciente.nombre").value("Ana"))
                .andExpect(jsonPath("$.paciente.apellido").value("Perez"))
                .andExpect(jsonPath("$.paciente.email").value("ana@example.com"))
                .andExpect(jsonPath("$.paciente.telefono").value("+5492610000000"))
                .andExpect(jsonPath("$.paciente.dni").value("30111222"))
                .andExpect(jsonPath("$.agendaId").doesNotExist())
                .andExpect(jsonPath("$.consultorioId").doesNotExist())
                .andExpect(jsonPath("$.notas").doesNotExist())
                .andExpect(jsonPath("$.historiaClinica").doesNotExist())
                .andExpect(jsonPath("$.diagnostico").doesNotExist())
                .andExpect(jsonPath("$.odontograma").doesNotExist());

        verify(service).turnoDetalle(TURNO_CANCELADO_ID);
    }

    @Test
    void getAgendaWithMalformedDateReturnsValidationErrorWithoutCallingService() throws Exception {
        mvc.perform(get("/api/odontologo/agenda").param("fecha", "no-es-fecha"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"codigo\":\"VALIDATION_ERROR\"}", true));

        verifyNoInteractions(service);
    }

    @Test
    void getTurnoDetalleWithNonUuidReturnsValidationErrorWithoutCallingService() throws Exception {
        mvc.perform(get("/api/odontologo/turnos/{turnoId}", "no-es-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"codigo\":\"VALIDATION_ERROR\"}", true));

        verifyNoInteractions(service);
    }

    @Test
    void getAgendaWithInvalidVistaMapsValidationExceptionToBadRequest() throws Exception {
        when(service.agenda("MES", null)).thenThrow(AvailabilityException.validation());

        mvc.perform(get("/api/odontologo/agenda").param("vista", "MES"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"codigo\":\"VALIDATION_ERROR\"}", true));
    }

    @Test
    void getTurnoDetalleWhenNotFoundMapsToNotFound() throws Exception {
        when(service.turnoDetalle(TURNO_CONFIRMADO_ID)).thenThrow(AvailabilityException.turnoNotFound());

        mvc.perform(get("/api/odontologo/turnos/{turnoId}", TURNO_CONFIRMADO_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"codigo\":\"TURNO_NOT_FOUND\"}", true));
    }

    @Test
    void persistenceAndTechnicalFailuresMapToInternalServerErrorWithoutLeakingDetails() throws Exception {
        when(service.agenda(null, null)).thenThrow(new DataAccessResourceFailureException("jdbc-secret-detail"));

        mvc.perform(get("/api/odontologo/agenda"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"codigo\":\"TECHNICAL_FAILURE\"}", true));

        when(service.turnoDetalle(TURNO_CONFIRMADO_ID)).thenThrow(AvailabilityException.technicalFailure());

        mvc.perform(get("/api/odontologo/turnos/{turnoId}", TURNO_CONFIRMADO_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"codigo\":\"TECHNICAL_FAILURE\"}", true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "2026-11-26Z",
            "2026-11-26-03:00",
            "2026-02-30",
            "+10000-01-01",
            "+999999999-12-31"
    })
    void getAgendaRejectsInvalidFechaBeforeCallingService(String fecha) throws Exception {
        mvc.perform(get("/api/odontologo/agenda").param("fecha", fecha))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"codigo\":\"VALIDATION_ERROR\"}", true));

        mvc.perform(get("/api/odontologo/agenda").param("vista", "SEMANA").param("fecha", fecha))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"codigo\":\"VALIDATION_ERROR\"}", true));

        verifyNoInteractions(service);
    }

    @Test
    void getAgendaAcceptsStrictIsoDateInDayAndWeek() throws Exception {
        LocalDate fecha = LocalDate.of(2026, 11, 26);
        when(service.agenda("DIA", fecha)).thenReturn(new AgendaResponse(
                "DIA", fecha, fecha, "America/Argentina/Mendoza", List.of()));
        when(service.agenda("SEMANA", fecha)).thenReturn(new AgendaResponse(
                "SEMANA", LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 29),
                "America/Argentina/Mendoza", List.of()));

        mvc.perform(get("/api/odontologo/agenda").param("vista", "DIA").param("fecha", "2026-11-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vista").value("DIA"))
                .andExpect(jsonPath("$.desde").value("2026-11-26"))
                .andExpect(jsonPath("$.hasta").value("2026-11-26"));

        mvc.perform(get("/api/odontologo/agenda").param("vista", "SEMANA").param("fecha", "2026-11-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vista").value("SEMANA"))
                .andExpect(jsonPath("$.desde").value("2026-11-23"))
                .andExpect(jsonPath("$.hasta").value("2026-11-29"));

        verify(service).agenda("DIA", fecha);
        verify(service).agenda("SEMANA", fecha);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1-1-1-1-1",
            "no-es-uuid",
            "00000000-0000-0000-0000-00000000010",
            "g0000000-0000-0000-0000-000000000101"
    })
    void getTurnoDetalleRejectsMalformedUuidBeforeCallingService(String id) throws Exception {
        mvc.perform(get("/api/odontologo/turnos/{turnoId}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"codigo\":\"VALIDATION_ERROR\"}", true));

        verifyNoInteractions(service);
    }

    @Test
    void getTurnoDetalleAcceptsUppercaseAndLowercaseUuid() throws Exception {
        String upper = "ABCDEF01-2345-6789-ABCD-EF0123456789";
        UUID upperUuid = UUID.fromString(upper);
        String missingLower = "abcdef01-2345-6789-abcd-ef0123456780";
        UUID missingUuid = UUID.fromString(missingLower);

        when(service.turnoDetalle(upperUuid)).thenReturn(new TurnoDetalleResponse(
                upperUuid,
                MOTIVO_ID,
                "Limpieza",
                Instant.parse("2030-01-09T13:00:00Z"),
                Instant.parse("2030-01-09T13:30:00Z"),
                30,
                Turno.Estado.CONFIRMADO,
                0,
                Instant.parse("2030-01-01T10:00:00Z"),
                null,
                new PacienteBasicoResponse("Ana", "Perez", "ana@example.com", "+5492610000000", "30111222")));
        when(service.turnoDetalle(missingUuid)).thenThrow(AvailabilityException.turnoNotFound());

        mvc.perform(get("/api/odontologo/turnos/{turnoId}", upper))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(upperUuid.toString()));

        mvc.perform(get("/api/odontologo/turnos/{turnoId}", missingLower))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"codigo\":\"TURNO_NOT_FOUND\"}", true));

        verify(service).turnoDetalle(upperUuid);
        verify(service).turnoDetalle(missingUuid);
    }
}
