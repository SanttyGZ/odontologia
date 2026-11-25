package dev.odontologia.comunicaciones;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComposicionAvisosTest {
    private final ComposicionAvisos composicion =
            new ComposicionAvisos(URI.create("http://127.0.0.1:5173"), ZoneId.of("America/Argentina/Mendoza"));

    @Test
    void componeConfirmacionYRecordatorioJuntosConReferenciaEnFragmento() {
        var aviso = composicion.componer(intento(List.of("CONFIRMACION", "RECORDATORIO"), "CONFIRMADO", false, true),
                "material-ficticio");
        assertThat(aviso.asunto()).contains("Confirmación y recordatorio");
        assertThat(aviso.texto()).contains("http://127.0.0.1:5173/gestion#material-ficticio", "09:30");
    }

    @Test
    void distingueCancelacionReprogramacionYRecuperacion() {
        assertThat(composicion.componer(intento(List.of("CAMBIO"), "CANCELADO", false, false), null).texto())
                .contains("cancelado").doesNotContain("/gestion");
        assertThat(composicion.componer(intento(List.of("CAMBIO"), "CONFIRMADO", false, true), "prueba").asunto())
                .contains("Reprogramación");
        assertThat(composicion.componer(intento(List.of("CONFIRMACION"), "CONFIRMADO", true, true), "prueba").asunto())
                .contains("Recuperación");
        assertThat(composicion.componer(intento(List.of("RECORDATORIO"), "CONFIRMADO", false, true), "prueba").asunto())
                .contains("Recordatorio");
    }

    private static AutorizacionDespacho.Intento intento(List<String> tipos, String estado,
            boolean recuperacion, boolean conCredencial) {
        Instant inicio = Instant.parse("2027-01-05T12:30:00Z");
        return new AutorizacionDespacho.Intento(UUID.randomUUID(), UUID.randomUUID(), 2, GmailPruebas.BUZON,
                conCredencial ? UUID.randomUUID() : null, inicio, inicio.plusSeconds(1800), estado, tipos, recuperacion);
    }
}

