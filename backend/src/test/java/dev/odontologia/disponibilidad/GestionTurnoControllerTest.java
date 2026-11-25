package dev.odontologia.disponibilidad;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import dev.odontologia.disponibilidad.GestionTurnoDtos.Consulta;
import dev.odontologia.disponibilidad.GestionTurnoDtos.ConsultaResultado;
import dev.odontologia.disponibilidad.GestionTurnoDtos.Cancelacion;
import dev.odontologia.disponibilidad.GestionTurnoDtos.Reprogramacion;
import dev.odontologia.disponibilidad.GestionTurnoDtos.ReprogramacionInput;
import dev.odontologia.disponibilidad.GestionTurnoDtos.ReprogramacionResultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GestionTurnoControllerTest {

    private static final UUID TURNO = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final String TOKEN = "abcdefghijklmnopabcdefghijklmnopabcdefghijklmnop";
    private static final String ETAG = "\"0123456789abcdefghijklmnopqrstuv\"";
    private static final String URL = "/api/gestion/turno";

    private GestionTurnoService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(GestionTurnoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new GestionTurnoController(service))
                .setControllerAdvice(new GestionTurnoErrorAdvice())
                .build();
    }

    @Test
    void getReturnsOnlyTheAuthorizedTurnFieldsAndOpaqueEtagWithoutCaching() throws Exception {
        when(service.consultar(TOKEN)).thenReturn(new ConsultaResultado(
                new Consulta(LocalDate.of(2030, 1, 8), LocalTime.of(9, 0), LocalTime.of(9, 30),
                        "CONFIRMADO", List.of("CANCELAR", "REPROGRAMAR")), ETAG));

        mvc.perform(get(URL).header("X-Management-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("ETag", ETAG))
                .andExpect(jsonPath("$.fecha").value("2030-01-08"))
                .andExpect(jsonPath("$.horaInicio").value("09:00:00"))
                .andExpect(jsonPath("$.horaFin").value("09:30:00"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$.acciones[0]").value("CANCELAR"))
                .andExpect(jsonPath("$.acciones[1]").value("REPROGRAMAR"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.turnoId").doesNotExist())
                .andExpect(jsonPath("$.revision").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.telefono").doesNotExist());

        verify(service).consultar(TOKEN);
    }

    @Test
    void reprogramacionReturnsSameTurnIdAndCurrentOpaqueEtag() throws Exception {
        when(service.reprogramar(any(), any(), any())).thenReturn(new ReprogramacionResultado(
                new Reprogramacion(TURNO, LocalDate.of(2030, 1, 9), LocalTime.of(10, 0), LocalTime.of(10, 30),
                        "CONFIRMADO", "PENDIENTE"), ETAG));

        mvc.perform(post(URL + "/reprogramacion")
                        .header("X-Management-Token", TOKEN)
                        .header("If-Match", ETAG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nuevoInicio":"2030-01-09T13:00:00Z"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("ETag", ETAG))
                .andExpect(jsonPath("$.turnoId").value(TURNO.toString()))
                .andExpect(jsonPath("$.fecha").value("2030-01-09"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$.notificacionEstado").value("PENDIENTE"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.secreto").doesNotExist());

        verify(service).reprogramar(TOKEN, ETAG, new ReprogramacionInput(
                java.time.Instant.parse("2030-01-09T13:00:00Z")));
    }

    @Test
    void errorResponsesAreGenericAndNotCached() throws Exception {
        doThrow(GestionTurnoException.linkNotAvailable()).when(service).consultar(null);

        mvc.perform(get(URL))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.codigo").value("LINK_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void preconditionMissingIs428AndNotCached() throws Exception {
        doThrow(GestionTurnoException.preconditionRequired()).when(service).cancelar(TOKEN, null);

        mvc.perform(post(URL + "/cancelacion").header("X-Management-Token", TOKEN))
                .andExpect(status().is(428))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.codigo").value("PRECONDITION_REQUIRED"));
    }

    @Test
    void cancellationReturnsPendingNotificationAndIsNotCached() throws Exception {
        when(service.cancelar(TOKEN, ETAG)).thenReturn(new Cancelacion(LocalDate.of(2030, 1, 8),
                LocalTime.of(9, 0), LocalTime.of(9, 30), "CANCELADO", "PENDIENTE"));

        mvc.perform(post(URL + "/cancelacion")
                        .header("X-Management-Token", TOKEN)
                        .header("If-Match", ETAG))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.notificacionEstado").value("PENDIENTE"))
                .andExpect(jsonPath("$.turnoId").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());

        verify(service).cancelar(TOKEN, ETAG);
    }

    @Test
    void slotConflictIs409GenericAndNotCached() throws Exception {
        doThrow(GestionTurnoException.slotUnavailable()).when(service).reprogramar(any(), any(), any());

        mvc.perform(post(URL + "/reprogramacion")
                        .header("X-Management-Token", TOKEN)
                        .header("If-Match", ETAG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoInicio\":\"2030-01-09T13:00:00Z\"}"))
                .andExpect(status().isConflict())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.codigo").value("SLOT_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.detalle").doesNotExist());
    }

    @Test
    void staleVersionIs412GenericAndNotCached() throws Exception {
        doThrow(GestionTurnoException.versionMismatch()).when(service).cancelar(TOKEN, ETAG);

        mvc.perform(post(URL + "/cancelacion")
                        .header("X-Management-Token", TOKEN)
                        .header("If-Match", ETAG))
                .andExpect(status().isPreconditionFailed())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.codigo").value("TURN_VERSION_MISMATCH"))
                .andExpect(jsonPath("$.message").doesNotExist());
    }
}
