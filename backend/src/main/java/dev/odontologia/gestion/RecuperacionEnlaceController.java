package dev.odontologia.gestion;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class RecuperacionEnlaceController {
    record Solicitud(String motivo, RecuperacionEnlace.Metodo metodo, boolean verificacionConfirmada,
            String emailCorregido) {}
    record Resultado(UUID referencia) {}

    private final RecuperacionEnlace recuperacion;

    RecuperacionEnlaceController(RecuperacionEnlace recuperacion) {
        this.recuperacion = recuperacion;
    }

    @PostMapping("/api/odontologo/turnos/{turno}/recuperacion-enlace")
    ResponseEntity<Resultado> recuperar(@PathVariable UUID turno, @RequestBody Solicitud solicitud,
            Principal odontologo) {
        UUID referencia = recuperacion.recuperar(turno, odontologo.getName(), solicitud.motivo(),
                solicitud.metodo(), solicitud.verificacionConfirmada(), solicitud.emailCorregido());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Resultado(referencia));
    }
}
