package dev.odontologia.comunicaciones;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

final class SondeoAvisos {
    private static final Logger LOGGER = LoggerFactory.getLogger(SondeoAvisos.class);

    private final ColaDespachos cola;
    private final AutorizacionDespacho autorizacion;
    private final EmisionAviso emision;
    private final Clock reloj;

    SondeoAvisos(ColaDespachos cola, AutorizacionDespacho autorizacion, EmisionAviso emision, Clock reloj) {
        this.cola = cola;
        this.autorizacion = autorizacion;
        this.emision = emision;
        this.reloj = reloj;
    }

    @Scheduled(fixedRate = 30_000)
    void ejecutar() {
        try {
            cola.procesar(reloj, pendiente -> autorizacion.autorizar(pendiente.agenda(), pendiente.despacho(), reloj)
                    .ifPresent(emision::procesar));
        } catch (RuntimeException fallo) {
            LOGGER.warn("C-11 no completó el sondeo de avisos. Revisar los resultados persistidos.");
        }
    }
}
