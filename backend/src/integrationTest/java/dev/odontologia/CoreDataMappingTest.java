package dev.odontologia;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.UUID;

import dev.odontologia.agenda.AgendaQueries;
import dev.odontologia.agenda.BloqueoAgenda;
import dev.odontologia.agenda.FranjaSemanal;
import dev.odontologia.agenda.Turno;
import dev.odontologia.config.TimeConfiguration;
import dev.odontologia.consultorio.ConsultorioQueries;
import dev.odontologia.pacientes.PacienteQueries;
import org.junit.jupiter.api.Test;

import static dev.odontologia.CoreSchemaTest.START;
import static org.assertj.core.api.Assertions.assertThat;

class CoreDataMappingTest {

    @Test
    void readsTypedRowsKeepingLocalHoursAndInstantsAcrossJvmZones() {
        try (var db = new TestDatabase()) {
            var f = CoreSchemaTest.fixture(db);
            UUID appointment = UUID.randomUUID();
            UUID weekly = UUID.randomUUID();
            UUID block = UUID.randomUUID();
            db.jdbc.update("INSERT INTO franja_semanal VALUES (?, ?, 2, TIME '08:15:00.123456', TIME '11:30', 'ATENCION')",
                    weekly, f.agenda());
            db.jdbc.update("INSERT INTO bloqueo_agenda VALUES (?, ?, ?, ?, NULL)",
                    block, f.agenda(), START, START.plusMinutes(10));
            CoreSchemaTest.insertTurn(db.jdbc, f, appointment, START, START.plusMinutes(30), "CONFIRMADO");
            db.jdbc.update("UPDATE motivo_consulta SET duracion_minutos = 45 WHERE id = ?", f.motivo());

            TimeZone original = TimeZone.getDefault();
            try {
                for (String jvmZone : new String[] {"Asia/Tokyo", "America/Argentina/Mendoza"}) {
                    TimeZone.setDefault(TimeZone.getTimeZone(jvmZone));
                    var agenda = new AgendaQueries(db.jdbc);
                    var office = new ConsultorioQueries(db.jdbc).findConfiguration().orElseThrow();
                    var patient = new PacienteQueries(db.jdbc).find(f.paciente()).orElseThrow();
                    assertThat(office.consultorio().zonaHoraria()).isEqualTo(ZoneId.of("Etc/UTC"));
                    assertThat(office.odontologo().id()).isEqualTo(f.odontologo());
                    assertThat(office.agenda().id()).isEqualTo(f.agenda());
                    assertThat(agenda.findMotivo(f.agenda(), f.motivo()).orElseThrow().duracionMinutos()).isEqualTo(45);
                    assertThat(agenda.franjas(f.agenda())).containsExactly(new FranjaSemanal(weekly, f.agenda(),
                            DayOfWeek.TUESDAY, LocalTime.parse("08:15:00.123456"), LocalTime.parse("11:30"),
                            FranjaSemanal.Tipo.ATENCION));
                    assertThat(agenda.bloqueos(f.agenda(), START.toInstant(), START.plusMinutes(30).toInstant()))
                            .containsExactly(new BloqueoAgenda(block, f.agenda(), START.toInstant(),
                                    START.plusMinutes(10).toInstant(), null));
                    var turn = agenda.findTurno(f.agenda(), appointment).orElseThrow();
                    assertThat(turn.inicio()).isEqualTo(START.toInstant());
                    assertThat(turn.fin()).isEqualTo(START.plusMinutes(30).toInstant());
                    assertThat(turn.creadoEn()).isEqualTo(START.minusDays(1).toInstant());
                    assertThat(turn.canceladoEn()).isNull();
                    assertThat(turn.estado()).isEqualTo(Turno.Estado.CONFIRMADO);
                    assertThat(turn.duracionReservadaMinutos()).isEqualTo(30);
                    assertThat(turn.pacienteId()).isEqualTo(patient.id());
                    assertThat(patient.dni()).isEqualTo("002");
                    assertThat(agenda.ocupados(f.agenda(), START.plusMinutes(30).toInstant(), START.plusHours(1).toInstant())).isEmpty();
                    assertThat(agenda.ocupados(f.agenda(), START.plusMinutes(29).toInstant(), START.plusHours(1).toInstant()))
                            .containsExactly(turn);
                }
            } finally {
                TimeZone.setDefault(original);
            }
        }
    }

    @Test
    void clockCanBeSubstitutedAndDomainUsesMicrosecondPrecision() {
        Instant fixed = Instant.parse("2030-01-08T13:00:00.123456789Z");
        Clock testClock = Clock.fixed(fixed, ZoneOffset.UTC);
        assertThat(new TimeConfiguration().clock().getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(testClock.instant()).isEqualTo(fixed);
        assertThat(new BloqueoAgenda(UUID.randomUUID(), UUID.randomUUID(), testClock.instant(),
                testClock.instant().plusSeconds(60), null).inicio())
                .isEqualTo(Instant.parse("2030-01-08T13:00:00.123456Z"));
    }
}
