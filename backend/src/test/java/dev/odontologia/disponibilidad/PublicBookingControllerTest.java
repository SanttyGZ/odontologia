package dev.odontologia.disponibilidad;

import java.time.Instant;
import java.util.UUID;

import dev.odontologia.agenda.AgendaWriteException;
import dev.odontologia.disponibilidad.PublicBookingDtos.ReservaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicBookingControllerTest {

    private static final UUID TURNO = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID MOTIVO = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private PublicBookingService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(PublicBookingService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PublicBookingController(service))
                .setControllerAdvice(new AvailabilityErrorAdvice())
                .build();
    }

    @Test
    void postTurnosReturnsCreatedWithConfirmedAndPendingNotificationWithoutSecrets() throws Exception {
        Instant inicio = Instant.parse("2030-01-08T12:00:00Z");
        Instant fin = Instant.parse("2030-01-08T12:30:00Z");
        when(service.reservar(any())).thenReturn(
                new ReservaResponse(TURNO, MOTIVO, inicio, fin, 30, "CONFIRMADO", "PENDIENTE"));

        mvc.perform(post("/api/public/turnos").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "motivoId": "00000000-0000-0000-0000-000000000002",
                                  "inicio": "2030-01-08T12:00:00Z",
                                  "nombre": "Ana",
                                  "apellido": "Perez",
                                  "email": "ana@example.com",
                                  "telefono": "2615551234",
                                  "dni": "30123456"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.turnoId").value(TURNO.toString()))
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$.notificacionEstado").value("PENDIENTE"))
                .andExpect(jsonPath("$.credencialId").doesNotExist())
                .andExpect(jsonPath("$.secreto").doesNotExist());
    }

    @Test
    void mapsValidationNotFoundSlotUnavailableAndAgendaBusyErrors() throws Exception {
        doThrow(AvailabilityException.validation()).when(service).reservar(any());
        mvc.perform(post("/api/public/turnos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"));

        doThrow(AvailabilityException.motivoNotFound()).when(service).reservar(any());
        mvc.perform(post("/api/public/turnos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("MOTIVO_NOT_FOUND"));

        doThrow(AvailabilityException.slotUnavailable()).when(service).reservar(any());
        mvc.perform(post("/api/public/turnos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SLOT_UNAVAILABLE"));

        AgendaWriteException lockTimeout = mock(AgendaWriteException.class);
        when(lockTimeout.reason()).thenReturn(AgendaWriteException.Reason.LOCK_TIMEOUT);
        doThrow(lockTimeout).when(service).reservar(any());
        mvc.perform(post("/api/public/turnos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("AGENDA_BUSY"));
    }
}
