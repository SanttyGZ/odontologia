package dev.odontologia.disponibilidad;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import dev.odontologia.agenda.MotivoConsulta;
import dev.odontologia.disponibilidad.AvailabilityRules.Slot;
import dev.odontologia.disponibilidad.PublicAvailabilityService.DisponibilidadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicAvailabilityControllerTest {

    private static final UUID AGENDA = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MOTIVO = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private PublicAvailabilityService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(PublicAvailabilityService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PublicAvailabilityController(service))
                .setControllerAdvice(new AvailabilityErrorAdvice())
                .build();
    }

    @Test
    void getPublicMotivosReturnsOnlyPublicFields() throws Exception {
        when(service.motivos()).thenReturn(List.of(new MotivoConsulta(MOTIVO, AGENDA, "Limpieza", 30)));

        mvc.perform(get("/api/public/motivos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(MOTIVO.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Limpieza"))
                .andExpect(jsonPath("$[0].duracionMinutos").value(30))
                .andExpect(jsonPath("$[0].agendaId").doesNotExist());
    }

    @Test
    void getPublicDisponibilidadReturnsMinimalSlotsAndHandlesValidationAndNotFound() throws Exception {
        Instant inicio = Instant.parse("2030-01-07T12:00:00Z");
        Instant fin = Instant.parse("2030-01-07T12:30:00Z");
        when(service.disponibilidad(MOTIVO, LocalDate.of(2030, 1, 7), LocalDate.of(2030, 1, 7)))
                .thenReturn(new DisponibilidadResult(MOTIVO, 30, ZoneId.of("America/Argentina/Mendoza"),
                        List.of(new Slot(inicio, fin))));

        mvc.perform(get("/api/public/disponibilidad")
                        .param("motivoId", MOTIVO.toString())
                        .param("desde", "2030-01-07")
                        .param("hasta", "2030-01-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivoId").value(MOTIVO.toString()))
                .andExpect(jsonPath("$.duracionMinutos").value(30))
                .andExpect(jsonPath("$.zonaHoraria").value("America/Argentina/Mendoza"))
                .andExpect(jsonPath("$.horarios[0].inicio").value("2030-01-07T12:00:00Z"))
                .andExpect(jsonPath("$.horarios[0].fin").value("2030-01-07T12:30:00Z"));

        when(service.disponibilidad(MOTIVO, LocalDate.of(2030, 1, 8), LocalDate.of(2030, 1, 7)))
                .thenThrow(AvailabilityException.validation());
        mvc.perform(get("/api/public/disponibilidad")
                        .param("motivoId", MOTIVO.toString())
                        .param("desde", "2030-01-08")
                        .param("hasta", "2030-01-07"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"));

        mvc.perform(get("/api/public/disponibilidad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"));

        when(service.disponibilidad(MOTIVO, null, null)).thenThrow(AvailabilityException.motivoNotFound());
        mvc.perform(get("/api/public/disponibilidad").param("motivoId", MOTIVO.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("MOTIVO_NOT_FOUND"));
    }
}
