package dev.odontologia.disponibilidad;

import dev.odontologia.disponibilidad.PublicBookingDtos.ReservaInput;
import dev.odontologia.disponibilidad.PublicBookingDtos.ReservaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public final class PublicBookingController {

    private final PublicBookingService service;

    public PublicBookingController(PublicBookingService service) {
        this.service = service;
    }

    @PostMapping("/turnos")
    public ResponseEntity<ReservaResponse> reservar(@RequestBody(required = false) ReservaInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.reservar(input));
    }
}
