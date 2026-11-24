package dev.odontologia.disponibilidad;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import dev.odontologia.disponibilidad.AvailabilityDtos.MotivoResponse;
import dev.odontologia.disponibilidad.PublicAvailabilityDtos.DisponibilidadResponse;
import dev.odontologia.disponibilidad.PublicAvailabilityDtos.HorarioDisponibleResponse;
import dev.odontologia.disponibilidad.PublicAvailabilityService.DisponibilidadResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public final class PublicAvailabilityController {

    private final PublicAvailabilityService service;

    public PublicAvailabilityController(PublicAvailabilityService service) {
        this.service = service;
    }

    @GetMapping("/motivos")
    public List<MotivoResponse> motivos() {
        return service.motivos().stream()
                .map(motivo -> new MotivoResponse(motivo.id(), motivo.nombre(), motivo.duracionMinutos()))
                .toList();
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadResponse disponibilidad(
            @RequestParam UUID motivoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        DisponibilidadResult result = service.disponibilidad(motivoId, desde, hasta);
        List<HorarioDisponibleResponse> horarios = result.slots().stream()
                .map(slot -> new HorarioDisponibleResponse(slot.inicio(), slot.fin()))
                .toList();
        return new DisponibilidadResponse(result.motivoId(), result.duracionMinutos(),
                result.zonaHoraria().getId(), horarios);
    }
}
