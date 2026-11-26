package dev.odontologia.comunicaciones;

import java.time.Clock;

import dev.odontologia.comunicaciones.ColaDespachos.Pendiente;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConfiguracionCorreoTest {
    @Test
    void emisorDeshabilitadoPorDefecto() {
        new ApplicationContextRunner().withUserConfiguration(ConfiguracionCorreo.class).run(context ->
                assertThat(context).hasNotFailed().doesNotHaveBean(SondeoAvisos.class));
    }

    @Test
    void sondeoAutorizaAntesDeEmitir() {
        var cola = mock(ColaDespachos.class);
        var autorizacion = mock(AutorizacionDespacho.class);
        var emision = mock(EmisionAviso.class);
        var reloj = Clock.systemUTC();
        var pendiente = new Pendiente(UUID.randomUUID(), UUID.randomUUID());
        var otro = new Pendiente(UUID.randomUUID(), UUID.randomUUID());
        var intento = mock(AutorizacionDespacho.Intento.class);
        doAnswer(invocacion -> {
            Consumer<Pendiente> procesador = invocacion.getArgument(1);
            procesador.accept(pendiente);
            procesador.accept(otro);
            return null;
        }).when(cola).procesar(eq(reloj), any());
        when(autorizacion.autorizar(pendiente.agenda(), pendiente.despacho(), reloj)).thenReturn(Optional.of(intento));

        new SondeoAvisos(cola, autorizacion, emision, reloj).ejecutar();

        var orden = inOrder(autorizacion, emision);
        orden.verify(autorizacion).autorizar(pendiente.agenda(), pendiente.despacho(), reloj);
        orden.verify(emision).procesar(intento);
        verifyNoMoreInteractions(emision);
    }
}
