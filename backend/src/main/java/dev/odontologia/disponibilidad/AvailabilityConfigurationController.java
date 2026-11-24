package dev.odontologia.disponibilidad;

import java.util.List;
import java.util.UUID;

import dev.odontologia.agenda.BloqueoAgenda;
import dev.odontologia.agenda.FranjaSemanal;
import dev.odontologia.agenda.MotivoConsulta;
import dev.odontologia.disponibilidad.AvailabilityDtos.BloqueoInput;
import dev.odontologia.disponibilidad.AvailabilityDtos.BloqueoResponse;
import dev.odontologia.disponibilidad.AvailabilityDtos.FranjaResponse;
import dev.odontologia.disponibilidad.AvailabilityDtos.HorarioInput;
import dev.odontologia.disponibilidad.AvailabilityDtos.HorarioResponse;
import dev.odontologia.disponibilidad.AvailabilityDtos.MotivoInput;
import dev.odontologia.disponibilidad.AvailabilityDtos.MotivoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/odontologo")
public final class AvailabilityConfigurationController {

    private final AvailabilityConfigurationService service;

    public AvailabilityConfigurationController(AvailabilityConfigurationService service) {
        this.service = service;
    }

    @GetMapping("/motivos")
    public List<MotivoResponse> motivos() {
        return service.motivos().stream().map(AvailabilityConfigurationController::motivoResponse).toList();
    }

    @PostMapping("/motivos")
    public ResponseEntity<MotivoResponse> createMotivo(@RequestBody(required = false) MotivoInput input) {
        MotivoResponse response = motivoResponse(service.createMotivo(
                input == null ? null : input.nombre(), duration(input)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/motivos/{motivoId}")
    public MotivoResponse updateMotivo(@PathVariable UUID motivoId,
                                       @RequestBody(required = false) MotivoInput input) {
        return motivoResponse(service.updateMotivo(motivoId,
                input == null ? null : input.nombre(), duration(input)));
    }

    @GetMapping("/horario")
    public HorarioResponse horario() {
        return horarioResponse(service.horario());
    }

    @PutMapping("/horario")
    public HorarioResponse replaceHorario(@RequestBody(required = false) HorarioInput input) {
        return horarioResponse(service.replaceHorario(input == null ? null : input.franjas()));
    }

    @GetMapping("/bloqueos")
    public List<BloqueoResponse> bloqueos() {
        return service.bloqueos().stream().map(AvailabilityConfigurationController::bloqueoResponse).toList();
    }

    @PostMapping("/bloqueos")
    public ResponseEntity<BloqueoResponse> createBloqueo(@RequestBody(required = false) BloqueoInput input) {
        BloqueoResponse response = bloqueoResponse(service.createBloqueo(
                input == null ? null : input.inicio(), input == null ? null : input.fin(),
                input == null ? null : input.descripcion()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/bloqueos/{bloqueoId}")
    public ResponseEntity<Void> deleteBloqueo(@PathVariable UUID bloqueoId) {
        service.deleteBloqueo(bloqueoId);
        return ResponseEntity.noContent().build();
    }

    private static MotivoResponse motivoResponse(MotivoConsulta value) {
        return new MotivoResponse(value.id(), value.nombre(), value.duracionMinutos());
    }

    private static Integer duration(MotivoInput input) {
        if (input == null || input.duracionMinutos() == null) {
            return null;
        }
        try {
            return input.duracionMinutos().intValueExact();
        } catch (ArithmeticException invalidInteger) {
            throw AvailabilityException.validation();
        }
    }

    private static HorarioResponse horarioResponse(List<FranjaSemanal> ranges) {
        return new HorarioResponse(ranges.stream().map(range -> new FranjaResponse(range.id(),
                range.diaSemana().getValue(), range.horaInicio(), range.horaFin(), range.tipo())).toList());
    }

    private static BloqueoResponse bloqueoResponse(BloqueoAgenda value) {
        return new BloqueoResponse(value.id(), value.inicio(), value.fin(), value.descripcion());
    }
}
