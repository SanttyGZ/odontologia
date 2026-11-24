package dev.odontologia.disponibilidad;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import dev.odontologia.agenda.BloqueoAgenda;
import dev.odontologia.agenda.FranjaSemanal;
import dev.odontologia.agenda.MotivoConsulta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvailabilityConfigurationControllerTest {

    private static final UUID AGENDA = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MOTIVO = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID BLOQUEO = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private AvailabilityConfigurationService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(AvailabilityConfigurationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AvailabilityConfigurationController(service))
                .setControllerAdvice(new AvailabilityErrorAdvice())
                .build();
    }

    @Test
    void getMotivosReturnsOnlyThePublicConfigurationFields() throws Exception {
        when(service.motivos()).thenReturn(List.of(new MotivoConsulta(MOTIVO, AGENDA, "Consulta", 30)));

        mvc.perform(get("/api/odontologo/motivos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(MOTIVO.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Consulta"))
                .andExpect(jsonPath("$[0].duracionMinutos").value(30))
                .andExpect(jsonPath("$[0].agendaId").doesNotExist());
    }

    @Test
    void createAndUpdateMotivoExposeTheSpecifiedStatuses() throws Exception {
        when(service.createMotivo("Consulta", 30)).thenReturn(new MotivoConsulta(MOTIVO, AGENDA, "Consulta", 30));
        when(service.updateMotivo(MOTIVO, "Consulta general", 45))
                .thenReturn(new MotivoConsulta(MOTIVO, AGENDA, "Consulta general", 45));

        mvc.perform(post("/api/odontologo/motivos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Consulta\",\"duracionMinutos\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracionMinutos").value(30));
        mvc.perform(put("/api/odontologo/motivos/{id}", MOTIVO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Consulta general\",\"duracionMinutos\":45}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Consulta general"));
    }

    @Test
    void decimalDurationsAreRejectedByBothMotivoRoutesWithoutCallingTheService() throws Exception {
        mvc.perform(post("/api/odontologo/motivos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Consulta\",\"duracionMinutos\":30.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"));
        mvc.perform(put("/api/odontologo/motivos/{id}", MOTIVO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Consulta\",\"duracionMinutos\":30.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"));

        verify(service, org.mockito.Mockito.never()).createMotivo(anyString(), any());
        verify(service, org.mockito.Mockito.never()).updateMotivo(any(), anyString(), any());
    }

    @Test
    void stringDurationsAreRejectedByBothMotivoRoutesWithoutCallingTheService() throws Exception {
        mvc.perform(post("/api/odontologo/motivos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Consulta\",\"duracionMinutos\":\"45\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"));
        mvc.perform(put("/api/odontologo/motivos/{id}", MOTIVO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Consulta\",\"duracionMinutos\":\"45\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"));

        verify(service, org.mockito.Mockito.never()).createMotivo(anyString(), any());
        verify(service, org.mockito.Mockito.never()).updateMotivo(any(), anyString(), any());
    }

    @Test
    void validationAndMissingResourceErrorsUseOnlyTheCommonCodeField() throws Exception {
        when(service.createMotivo(anyString(), any())).thenThrow(AvailabilityException.validation());
        when(service.updateMotivo(any(), anyString(), any())).thenThrow(AvailabilityException.motivoNotFound());

        mvc.perform(post("/api/odontologo/motivos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" \",\"duracionMinutos\":25}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").doesNotExist());
        mvc.perform(put("/api/odontologo/motivos/{id}", MOTIVO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Consulta\",\"duracionMinutos\":30}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("MOTIVO_NOT_FOUND"));
    }

    @Test
    void scheduleAndBlockRoutesUseTheSpecifiedResourceShapes() throws Exception {
        var range = new FranjaSemanal(UUID.randomUUID(), AGENDA, java.time.DayOfWeek.MONDAY,
                LocalTime.parse("09:10"), LocalTime.parse("12:00"), FranjaSemanal.Tipo.ATENCION);
        when(service.horario()).thenReturn(List.of(range));
        when(service.replaceHorario(any())).thenReturn(List.of(range));
        when(service.bloqueos()).thenReturn(List.of(new BloqueoAgenda(BLOQUEO, AGENDA,
                Instant.parse("2030-01-07T12:00:00Z"), Instant.parse("2030-01-07T13:00:00Z"), null)));
        when(service.createBloqueo(any(), any(), any())).thenReturn(new BloqueoAgenda(BLOQUEO, AGENDA,
                Instant.parse("2030-01-07T12:00:00Z"), Instant.parse("2030-01-07T13:00:00Z"), null));

        mvc.perform(get("/api/odontologo/horario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.franjas[0].diaSemana").value(1))
                .andExpect(jsonPath("$.franjas[0].horaInicio").value("09:10:00"));
        mvc.perform(put("/api/odontologo/horario").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"franjas\":[{\"id\":\"00000000-0000-0000-0000-000000000004\",\"diaSemana\":1,\"horaInicio\":\"09:10:00\",\"horaFin\":\"12:00:00\",\"tipo\":\"ATENCION\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.franjas[0].diaSemana").value(1));
        mvc.perform(get("/api/odontologo/bloqueos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(BLOQUEO.toString()));
        mvc.perform(post("/api/odontologo/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inicio\":\"2030-01-07T12:00:00Z\",\"fin\":\"2030-01-07T13:00:00Z\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fin").value("2030-01-07T13:00:00Z"));
        mvc.perform(delete("/api/odontologo/bloqueos/{id}", BLOQUEO))
                .andExpect(status().isNoContent());
        verify(service).deleteBloqueo(BLOQUEO);
    }

    @Test
    void configurationAndPersistenceErrorsExposeOnlyTheCommonCode() throws Exception {
        when(service.replaceHorario(any())).thenThrow(AvailabilityException.configurationConflict());
        mvc.perform(put("/api/odontologo/horario").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"franjas\":[]}"))
                .andExpect(status().isConflict())
                .andExpect(content().json("{\"codigo\":\"CONFIGURATION_CONFLICT\"}", true));

        doThrow(AvailabilityException.bloqueoNotFound()).when(service).deleteBloqueo(BLOQUEO);
        mvc.perform(delete("/api/odontologo/bloqueos/{id}", BLOQUEO))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"codigo\":\"BLOQUEO_NOT_FOUND\"}", true));

        when(service.motivos()).thenThrow(new DataAccessResourceFailureException("detalle de fila confidencial"));
        mvc.perform(get("/api/odontologo/motivos"))
                .andExpect(status().is5xxServerError())
                .andExpect(content().json("{\"codigo\":\"TECHNICAL_FAILURE\"}", true));
    }

    @Test
    void invalidUuidIsNormalizedToValidationError() throws Exception {
        mvc.perform(delete("/api/odontologo/bloqueos/no-es-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"));
    }
}
