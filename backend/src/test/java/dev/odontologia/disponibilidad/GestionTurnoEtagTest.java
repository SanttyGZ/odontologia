package dev.odontologia.disponibilidad;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import dev.odontologia.disponibilidad.GestionTurnoDtos.Consulta;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GestionTurnoEtagTest {

    @Test
    void createsAStableOpaqueTagThatChangesWithRevisionAndAvailableActions() {
        Consulta full = new Consulta(LocalDate.of(2030, 1, 8), LocalTime.of(9, 0), LocalTime.of(9, 30),
                "CONFIRMADO", List.of("CANCELAR", "REPROGRAMAR"));
        Consulta closedWindow = new Consulta(full.fecha(), full.horaInicio(), full.horaFin(), full.estado(), List.of());

        String current = GestionTurnoEtag.generar("10000000-0000-0000-0000-000000000001", 7, full);

        assertThat(current).isEqualTo(GestionTurnoEtag.generar(
                "10000000-0000-0000-0000-000000000001", 7, full));
        assertThat(current).matches("\"[A-Za-z0-9_-]{43}\"");
        assertThat(current).isNotEqualTo("\"7\"")
                .isNotEqualTo(GestionTurnoEtag.generar(
                        "10000000-0000-0000-0000-000000000001", 8, full))
                .isNotEqualTo(GestionTurnoEtag.generar(
                        "10000000-0000-0000-0000-000000000001", 7, closedWindow));
    }
}
