package dev.odontologia.comunicaciones;

import java.time.Clock;

import static dev.odontologia.comunicaciones.AutorizacionDespacho.Resultado.PERMANENTE;

final class EmisionAviso {
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
            material = autorizacion.recuperarEnlace(intento.id()).orElse(null);
        }
        if (material == null && !"CANCELADO".equals(intento.estado())) {
            return PERMANENTE;
        }
        return transporte.enviar(intento.destinatario(), composicion.componer(intento, material));
    }
}
