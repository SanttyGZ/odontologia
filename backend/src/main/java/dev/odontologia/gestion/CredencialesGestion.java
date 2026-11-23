package dev.odontologia.gestion;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public final class CredencialesGestion {
    public record TurnoAutorizado(UUID id, Instant inicio, Instant fin, int revision) {}

    private final JdbcTemplate jdbc;
    private final MaterialEnlace material;
    private final Clock clock;

    CredencialesGestion(JdbcTemplate jdbc, MaterialEnlace material, Clock clock) {
        this.jdbc = jdbc;
        this.material = material;
        this.clock = clock;
    }

    public UUID emitir(UUID turno) {
        exigirTransaccion();
        UUID id = UUID.randomUUID();
        MaterialEnlace.Cifrado cifrado = material.emitir(id, turno);
        int escritos = jdbc.update("""
                INSERT INTO credencial_gestion
                    (id, turno_id, verificador, material_cifrado, nonce, version_clave, emitida_en)
                SELECT ?, id, ?, ?, ?, ?, ?
                FROM turno WHERE id = ? AND estado = 'CONFIRMADO' AND fin > ?
                """, id, cifrado.verificador(), cifrado.contenido(), cifrado.nonce(), cifrado.version(),
                ahora(), turno, ahora());
        if (escritos != 1) {
            throw new IllegalStateException("Turno no disponible para emitir credencial");
        }
        return id;
    }

    public void revocar(UUID turno) {
        exigirTransaccion();
        jdbc.update("""
                UPDATE credencial_gestion SET revocada_en = greatest(emitida_en, ?)
                WHERE turno_id = ? AND revocada_en IS NULL
                """, ahora(), turno);
    }

    public Optional<TurnoAutorizado> validar(String secreto) {
        if (secreto == null || secreto.length() != 43) {
            return Optional.empty();
        }
        byte[] bytes;
        try {
            bytes = Base64.getUrlDecoder().decode(secreto);
        } catch (IllegalArgumentException invalid) {
            return Optional.empty();
        }
        if (bytes.length != 32 || !Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(secreto)) {
            return Optional.empty();
        }
        return jdbc.query("""
                SELECT t.id, t.inicio, t.fin, t.revision
                FROM credencial_gestion c JOIN turno t ON t.id = c.turno_id
                WHERE c.verificador = ? AND c.revocada_en IS NULL
                    AND t.estado = 'CONFIRMADO' AND t.fin > ?
                """, (rs, row) -> new TurnoAutorizado(rs.getObject("id", UUID.class),
                    instante(rs, "inicio"), instante(rs, "fin"), rs.getInt("revision")),
                MaterialEnlace.verificador(bytes), ahora()).stream().findFirst();
    }

    public Optional<String> recuperarParaComposicion(UUID referencia, UUID turno, int revision) {
        return recuperarMaterial(referencia, turno, revision);
    }

    /** El consumidor debe haber comprobado un intento durable todavía sin resultado. */
    public Optional<String> recuperarParaIntentoAutorizado(UUID referencia, UUID turno) {
        return recuperarMaterial(referencia, turno, null);
    }

    private Optional<String> recuperarMaterial(UUID referencia, UUID turno, Integer revision) {
        return jdbc.query("""
                SELECT c.verificador, c.material_cifrado, c.nonce, c.version_clave
                FROM credencial_gestion c JOIN turno t ON t.id = c.turno_id
                WHERE c.id = ? AND t.id = ? AND (CAST(? AS integer) IS NULL OR t.revision = ?)
                    AND c.revocada_en IS NULL AND t.estado = 'CONFIRMADO' AND t.fin > ?
                """, (rs, row) -> new MaterialEnlace.Cifrado(rs.getBytes("verificador"),
                    rs.getBytes("material_cifrado"), rs.getBytes("nonce"), rs.getString("version_clave")),
                referencia, turno, revision, revision, ahora()).stream().findFirst()
                .map(cifrado -> material.recuperar(referencia, turno, cifrado));
    }

    private OffsetDateTime ahora() {
        return clock.instant().atOffset(ZoneOffset.UTC);
    }

    private static Instant instante(ResultSet rs, String columna) throws SQLException {
        return rs.getObject(columna, OffsetDateTime.class).toInstant();
    }

    private static void exigirTransaccion() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("La gestión de credenciales requiere una escritura coordinada");
        }
    }
}
