package dev.odontologia.gestion;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CredencialesGestionTest {

    @Test
    void validaYDevuelveLaReferenciaOpacaSeparadaDelIdentificadorDelTurno() throws Exception {
        UUID turno = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID referencia = UUID.fromString("20000000-0000-0000-0000-000000000002");
        Instant inicio = Instant.parse("2030-02-01T15:00:00Z");
        Instant fin = inicio.plusSeconds(1800);
        Instant ahora = Instant.parse("2030-01-01T00:00:00Z");
        ResultSet fila = mock(ResultSet.class);
        when(fila.getObject("id", UUID.class)).thenReturn(turno);
        when(fila.getObject("referencia_credencial", UUID.class)).thenReturn(referencia);
        when(fila.getObject("inicio", OffsetDateTime.class)).thenReturn(inicio.atOffset(ZoneOffset.UTC));
        when(fila.getObject("fin", OffsetDateTime.class)).thenReturn(fin.atOffset(ZoneOffset.UTC));
        when(fila.getInt("revision")).thenReturn(3);
        when(fila.getString("destinatario_recuperacion")).thenReturn("odontologo.confirmado@example.invalid");
        String[] consulta = new String[1];

        JdbcTemplate jdbc = new JdbcTemplate() {
            @Override
            public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
                consulta[0] = sql;
                try {
                    return List.of(rowMapper.mapRow(fila, 0));
                } catch (SQLException failure) {
                    throw new IllegalStateException(failure);
                }
            }
        };
        var credenciales = new CredencialesGestion(jdbc, new MaterialEnlace(""),
                Clock.fixed(ahora, ZoneOffset.UTC));
        String secreto = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);

        var autorizado = credenciales.validar(secreto).orElseThrow();

        assertThat(autorizado.id()).isEqualTo(turno);
        assertThat(autorizado.referenciaCredencial()).isEqualTo(referencia);
        assertThat(autorizado.referenciaCredencial()).isNotEqualTo(autorizado.id());
        assertThat(autorizado.destinatarioRecuperacion())
                .isEqualTo(Optional.of("odontologo.confirmado@example.invalid"));
        assertThat(consulta[0]).contains("FROM recuperacion_enlace r", "ORDER BY r.orden DESC LIMIT 1");
    }

    @Test
    void noConsultaLaBaseParaUnSecretoInvalido() {
        JdbcTemplate jdbc = new JdbcTemplate() {
            @Override
            public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
                throw new AssertionError("Un secreto inválido no debe consultar credenciales");
            }
        };
        var credenciales = new CredencialesGestion(jdbc, new MaterialEnlace(""), Clock.systemUTC());

        assertThat(credenciales.validar("no-es-un-secreto-valido")).isEmpty();
    }
}
