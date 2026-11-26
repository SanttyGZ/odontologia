package dev.odontologia;

import java.time.InstantSource;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

import dev.odontologia.comunicaciones.ColaDespachos;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ColaDespachosIntegrationTest {
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2027-01-05T10:00:00Z");
    private static final String BUZON = "grupo16desarrollosoftware3k10@gmail.com";

    @Test
    void seleccionaDespachosVigentesYExcluyeOtroEmisor() {
        try (var db = new TestDatabase()) {
            var fuente = CoreSchemaTest.fixture(db);
            var vigente = aviso(db, fuente, AHORA, AHORA.plusHours(1), BUZON);
            aviso(db, fuente, AHORA.plusMinutes(1), AHORA.plusHours(1), BUZON);
            aviso(db, fuente, AHORA.minusHours(2), AHORA, BUZON);
            aviso(db, fuente, AHORA, AHORA.plusHours(1), "otro@example.test");
            var cola = new ColaDespachos(db.jdbc);
            var despachos = new ArrayList<UUID>();
            var competidor = new ArrayList<UUID>();

            cola.procesar(InstantSource.fixed(AHORA.toInstant()), pendiente -> {
                despachos.add(pendiente.despacho());
                cola.procesar(InstantSource.fixed(AHORA.toInstant()), otro -> competidor.add(otro.despacho()));
            });
            assertThat(despachos).containsExactly(vigente);
            assertThat(competidor).isEmpty();
        }
    }

    @Test
    void recuperaResultadoInciertoSinVolverADespachar() {
        try (var db = new TestDatabase()) {
            var fuente = CoreSchemaTest.fixture(db);
            var despacho = aviso(db, fuente, AHORA, AHORA.plusHours(1), BUZON);
            db.jdbc.update("""
                    INSERT INTO intento_notificacion
                        (id, despacho_id, turno_id, revision, destinatario, autorizado_en,
                         inicio_turno, fin_turno, estado_turno, tipos, recuperacion)
                    SELECT ?, n.despacho_id, n.turno_id, n.revision, n.destinatario, ?, t.inicio,
                           t.fin, t.estado, ARRAY[n.tipo], false
                    FROM intencion_notificacion n JOIN turno t ON t.id = n.turno_id
                    WHERE n.despacho_id = ?
                    """, UUID.randomUUID(), AHORA.minusMinutes(1), despacho);
            var despachos = new ArrayList<UUID>();
            new ColaDespachos(db.jdbc).procesar(InstantSource.fixed(AHORA.toInstant()),
                    pendiente -> despachos.add(pendiente.despacho()));

            assertThat(despachos).isEmpty();
            assertThat(db.jdbc.queryForObject(
                    "SELECT resultado FROM intento_notificacion WHERE despacho_id = ?", String.class, despacho))
                    .isEqualTo("INCIERTO");
        }
    }

    private static UUID aviso(TestDatabase db, CoreSchemaTest.Fixture fuente, OffsetDateTime programada,
            OffsetDateTime vence, String destinatario) {
        UUID turno = UUID.randomUUID();
        UUID despacho = UUID.randomUUID();
        long existentes = db.jdbc.queryForObject("SELECT count(*) FROM turno", Long.class);
        var inicio = AHORA.plusDays(2).plusHours(existentes);
        CoreSchemaTest.insertTurn(db.jdbc, fuente, turno, inicio, inicio.plusMinutes(30), "CONFIRMADO");
        db.jdbc.update("""
                INSERT INTO intencion_notificacion
                    (id, evento_id, turno_id, revision, tipo, destinatario, programada_en, vence_en, despacho_id)
                VALUES (?, ?, ?, 0, 'CAMBIO', ?, ?, ?, ?)
                """, UUID.randomUUID(), UUID.randomUUID(), turno, destinatario, programada, vence, despacho);
        return despacho;
    }
}
