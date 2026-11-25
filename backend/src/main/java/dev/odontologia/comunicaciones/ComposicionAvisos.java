package dev.odontologia.comunicaciones;

import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.web.util.UriComponentsBuilder;

final class ComposicionAvisos {
    record Aviso(String asunto, String texto) {
        @Override
        public String toString() {
            return "Aviso";
        }
    }

    private final URI baseUrl;
    private final DateTimeFormatter fecha;

    ComposicionAvisos(URI baseUrl, ZoneId zona) {
        this.baseUrl = baseUrl;
        this.fecha = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm", Locale.forLanguageTag("es-AR")).withZone(zona);
    }

    Aviso componer(AutorizacionDespacho.Intento intento, String material) {
        String tipo;
        String detalle;
        if (intento.recuperacion()) {
            tipo = "Recuperación de acceso";
            detalle = "Se recuperó el acceso a tu turno.";
        } else if (intento.tipos().contains("CAMBIO")) {
            boolean cancelado = "CANCELADO".equals(intento.estado());
            tipo = cancelado ? "Cancelación de turno" : "Reprogramación de turno";
            detalle = cancelado ? "Tu turno fue cancelado." : "Tu turno fue reprogramado.";
        } else if (intento.tipos().contains("CONFIRMACION")) {
            tipo = intento.tipos().contains("RECORDATORIO") ? "Confirmación y recordatorio" : "Confirmación de turno";
            detalle = "Tu turno está confirmado.";
        } else {
            tipo = "Recordatorio de turno";
            detalle = "Te recordamos tu próximo turno.";
        }

        String texto = detalle + "\nHorario del turno: " + fecha.format(intento.inicio())
                + " a " + fecha.format(intento.fin()) + " (" + fecha.getZone() + ").";
        if (material != null) {
            String enlace = UriComponentsBuilder.fromUri(baseUrl).pathSegment("gestion")
                    .fragment(material).build().encode().toUriString();
            texto += "\nGestioná el turno desde la aplicación local: " + enlace;
        }
        return new Aviso(tipo + " [C-11 " + intento.id() + "]", texto);
    }
}
