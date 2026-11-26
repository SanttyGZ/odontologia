package dev.odontologia.comunicaciones;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static dev.odontologia.comunicaciones.AutorizacionDespacho.Resultado.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmisionAvisoTest {
    private final Instant ahora = Instant.parse("2027-01-05T12:30:00Z");
    private final AutorizacionDespacho autorizacion = mock(AutorizacionDespacho.class);
    private final EmisionAviso.Transporte transporte = mock(EmisionAviso.Transporte.class);
    private final EmisionAviso emision = new EmisionAviso(autorizacion,
            new ComposicionAvisos(URI.create("http://localhost:5173"), ZoneOffset.UTC),
            transporte, Clock.fixed(ahora, ZoneOffset.UTC));

    @Test
    void registraResultadoDevueltoPorElTransporte() {
        var intento = intento(GmailPruebas.BUZON);
        when(autorizacion.recuperarEnlace(intento.id())).thenReturn(Optional.of("ficticio"));
        when(transporte.enviar(any(), any())).thenReturn(TRANSITORIO);
        emision.procesar(intento);
        verify(transporte).enviar(eq(GmailPruebas.BUZON), any());
        verify(autorizacion).registrar(intento.id(), TRANSITORIO, ahora);
    }

    @Test
    void impideElEnvioFueraDelBuzonYCuandoFaltaElEnlaceRequerido() {
        var noAutorizado = intento("persona@example.test");
        var enlaceRevocado = intento(GmailPruebas.BUZON);
        when(autorizacion.recuperarEnlace(enlaceRevocado.id())).thenReturn(Optional.empty());
        emision.procesar(noAutorizado);
        emision.procesar(enlaceRevocado);
        verifyNoInteractions(transporte);
        verify(autorizacion).registrar(noAutorizado.id(), PERMANENTE, ahora);
        verify(autorizacion).registrar(enlaceRevocado.id(), PERMANENTE, ahora);
    }

    @Test
    void propagaFallosAjenosAlMaterialDeEnlace() {
        var intento = intento(GmailPruebas.BUZON);
        var fallo = new IllegalStateException("Fallo inesperado");
        when(autorizacion.recuperarEnlace(intento.id())).thenThrow(fallo);

        assertThatThrownBy(() -> emision.procesar(intento)).isSameAs(fallo);
        verifyNoInteractions(transporte);
        verify(autorizacion, never()).registrar(any(), any(), any());
    }

    private AutorizacionDespacho.Intento intento(String destinatario) {
        return new AutorizacionDespacho.Intento(UUID.randomUUID(), UUID.randomUUID(), 0, destinatario,
                UUID.randomUUID(), ahora.plusSeconds(3600), ahora.plusSeconds(7200), "CONFIRMADO",
                List.of("CONFIRMACION"), false);
    }
}
