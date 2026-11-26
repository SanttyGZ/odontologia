package dev.odontologia.comunicaciones;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.odontologia.comunicaciones.AutorizacionDespacho.Resultado.PERMANENTE;

final class EmisionAviso {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmisionAviso.class);

    interface Transporte {
        AutorizacionDespacho.Resultado enviar(String destinatario, ComposicionAvisos.Aviso aviso);
    }

    private final AutorizacionDespacho autorizacion;
    private final ComposicionAvisos composicion;
    private final Transporte transporte;
    private final Clock reloj;

    EmisionAviso(AutorizacionDespacho autorizacion, ComposicionAvisos composicion, Transporte transporte, Clock reloj) {
        this.autorizacion = autorizacion;
        this.composicion = composicion;
        this.transporte = transporte;
        this.reloj = reloj;
    }

    void procesar(AutorizacionDespacho.Intento intento) {
        var resultado = emitir(intento);
        autorizacion.registrar(intento.id(), resultado, reloj.instant());
    }

    private AutorizacionDespacho.Resultado emitir(AutorizacionDespacho.Intento intento) {
        if (!GmailPruebas.permite(intento.destinatario())) {
            return PERMANENTE;
        }
        String material = null;
        if (intento.credencial() != null) {
            try {
                material = autorizacion.recuperarEnlace(intento.id()).orElse(null);
            } catch (IllegalStateException fallo) {
                // C-04 expone este diagnóstico sin secretos para material irrecuperable.
                if (!"Material de enlace no disponible".equals(fallo.getMessage())) {
                    throw fallo;
                }
                LOGGER.warn("C-11 no pudo componer el aviso: material de enlace no disponible.");
                return PERMANENTE;
            }
        }
        if (material == null && !"CANCELADO".equals(intento.estado())) {
            return PERMANENTE;
        }
        return transporte.enviar(intento.destinatario(), composicion.componer(intento, material));
    }
}
