package dev.odontologia.comunicaciones;

import java.net.URI;
import java.time.Clock;
import java.util.function.BooleanSupplier;
import org.springframework.jdbc.core.JdbcTemplate;

public final class SondeoAvisosFixture {
    public static Runnable sondeo(JdbcTemplate jdbc, AutorizacionDespacho autorizacion,
            Clock reloj, BooleanSupplier acepta) {
        var composicion = new ComposicionAvisos(URI.create("http://127.0.0.1:5173"), reloj.getZone());
        var emision = new EmisionAviso(autorizacion, composicion, (destinatario, aviso) -> {
            if (acepta.getAsBoolean()) {
                return AutorizacionDespacho.Resultado.ACEPTADA_SMTP;
            }
            throw new IllegalStateException("Interrupción simulada después de aceptar el mensaje");
        }, reloj);
        return new SondeoAvisos(new ColaDespachos(jdbc), autorizacion, emision, reloj)::ejecutar;
    }
}
