package dev.odontologia.disponibilidad;

import dev.odontologia.disponibilidad.GestionTurnoDtos.Cancelacion;
import dev.odontologia.disponibilidad.GestionTurnoDtos.Consulta;
import dev.odontologia.disponibilidad.GestionTurnoDtos.ConsultaResultado;
import dev.odontologia.disponibilidad.GestionTurnoDtos.Reprogramacion;
import dev.odontologia.disponibilidad.GestionTurnoDtos.ReprogramacionInput;
import dev.odontologia.disponibilidad.GestionTurnoDtos.ReprogramacionResultado;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gestion/turno")
public final class GestionTurnoController {

    static final String MANAGEMENT_TOKEN = "X-Management-Token";

    private final GestionTurnoService service;

    public GestionTurnoController(GestionTurnoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Consulta> consultar(@RequestHeader(name = MANAGEMENT_TOKEN, required = false) String secreto) {
        ConsultaResultado resultado = service.consultar(secreto);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.ETAG, resultado.etag()).body(resultado.cuerpo());
    }

    @PostMapping("/cancelacion")
    public ResponseEntity<Cancelacion> cancelar(
            @RequestHeader(name = MANAGEMENT_TOKEN, required = false) String secreto,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.cancelar(secreto, ifMatch));
    }

    @PostMapping("/reprogramacion")
    public ResponseEntity<Reprogramacion> reprogramar(
            @RequestHeader(name = MANAGEMENT_TOKEN, required = false) String secreto,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody(required = false) ReprogramacionInput input) {
        ReprogramacionResultado resultado = service.reprogramar(secreto, ifMatch, input);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.ETAG, resultado.etag()).body(resultado.cuerpo());
    }
}
