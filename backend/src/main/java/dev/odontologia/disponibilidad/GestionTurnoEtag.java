package dev.odontologia.disponibilidad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import dev.odontologia.disponibilidad.GestionTurnoDtos.Consulta;

final class GestionTurnoEtag {

    private GestionTurnoEtag() {
    }

    static String generar(String turnoId, int revision, Consulta consulta) {
        String representacion = String.join("\u0000", "c09-v1", turnoId, Integer.toString(revision),
                consulta.fecha().toString(), consulta.horaInicio().toString(), consulta.horaFin().toString(),
                consulta.estado(), String.join(",", consulta.acciones()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(representacion.getBytes(StandardCharsets.UTF_8));
            return "\"" + Base64.getUrlEncoder().withoutPadding().encodeToString(digest) + "\"";
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 no está disponible", unavailable);
        }
    }
}
