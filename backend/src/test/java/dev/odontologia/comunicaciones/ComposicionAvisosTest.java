package dev.odontologia.comunicaciones;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ComposicionAvisosTest {
    private final ComposicionAvisos composicion =
            new ComposicionAvisos(URI.create("http://127.0.0.1:5173"), ZoneId.of("America/Argentina/Mendoza"));

    @ParameterizedTest
    @CsvSource({
        "CONFIRMACION, false, Confirmación de turno",
        "CAMBIO, false, Reprogramación de turno",
        "RECORDATORIO, false, Recordatorio de turno",
        "CONFIRMACION+RECORDATORIO, false, Confirmación y recordatorio",
        "CONFIRMACION, true, Recuperación de acceso"
    })
    void componeUnAvisoConLaUrlCompleta(String tipos, boolean recuperacion, String asunto) {
        var aviso = composicion.componer(intento(List.of(tipos.split("\\+")), "CONFIRMADO", recuperacion, true),
                "credencial-ficticia");
        assertThat(aviso.asunto()).startsWith(asunto + " [C-11 ");
        assertThat(aviso.texto()).contains("09:30")
                .containsOnlyOnce("http://127.0.0.1:5173/gestionar-turno#credencial-ficticia")
                .endsWith("http://127.0.0.1:5173/gestionar-turno#credencial-ficticia");
    }

    @Test
    void usaElHostYPuertoDeLaUrlBase() {
        var composicionLocal = new ComposicionAvisos(
                URI.create("http://localhost:4180"), ZoneId.of("America/Argentina/Mendoza"));
        var aviso = composicionLocal.componer(intento(List.of("CONFIRMACION"), "CONFIRMADO", false, true),
                "credencial-ficticia");
        assertThat(aviso.texto()).endsWith("http://localhost:4180/gestionar-turno#credencial-ficticia");
    }

    @Test
    void omiteElEnlaceSinMaterialDeGestion() {
        assertThat(composicion.componer(intento(List.of("CAMBIO"), "CANCELADO", false, false), null).texto())
                .contains("cancelado").doesNotContain("http://");
        assertThat(composicion.componer(intento(List.of("CONFIRMACION"), "CONFIRMADO", false, false), null).texto())
                .contains("confirmado").doesNotContain("http://");
    }

    private static AutorizacionDespacho.Intento intento(List<String> tipos, String estado,
            boolean recuperacion, boolean conCredencial) {
        Instant inicio = Instant.parse("2027-01-05T12:30:00Z");
        return new AutorizacionDespacho.Intento(UUID.randomUUID(), UUID.randomUUID(), 2, GmailPruebas.BUZON,
                conCredencial ? UUID.randomUUID() : null, inicio, inicio.plusSeconds(1800), estado, tipos, recuperacion);
    }
}

