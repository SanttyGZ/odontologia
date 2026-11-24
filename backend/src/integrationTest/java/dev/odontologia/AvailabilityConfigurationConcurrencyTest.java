package dev.odontologia;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import dev.odontologia.agenda.AgendaQueries;
import dev.odontologia.agenda.AgendaWriteCoordinator;
import dev.odontologia.agenda.FranjaSemanal;
import dev.odontologia.disponibilidad.AvailabilityConfigurationService;
import dev.odontologia.disponibilidad.AvailabilityDtos.FranjaInput;
import dev.odontologia.consultorio.ConsultorioQueries;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.support.JdbcTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityConfigurationConcurrencyTest {

    @Test
    void reservationCommittedFirstMakesTheFollowingConfigurationChangeFail() throws Exception {
        try (var db = new TestDatabase(); var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            var fixture = CoreSchemaTest.fixture(db);
            var service = service(db);
            var agendaQueries = new AgendaQueries(db.jdbc);
            var originalSchedule = service.replaceHorario(List.of(attention("12:00", "14:00")));
            var coordinator = coordinator(db);
            var inserted = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var reservation = threads.submit(() -> coordinator.write(fixture.agenda(), () -> {
                CoreSchemaTest.insertTurn(db.jdbc, fixture, UUID.randomUUID(), CoreSchemaTest.START,
                        CoreSchemaTest.START.plusMinutes(30), "CONFIRMADO");
                inserted.countDown();
                await(release);
                return null;
            }));
            try {
                assertThat(inserted.await(4, TimeUnit.SECONDS)).isTrue();
                var configuration = threads.submit(() -> {
                    try {
                        service.replaceHorario(List.of(attention("13:15", "14:00")));
                        return "SAVED";
                    } catch (dev.odontologia.disponibilidad.AvailabilityException failure) {
                        return failure.codigo();
                    }
                });
                AgendaWriteCoordinatorTest.awaitLockWait(db);
                assertThat(configuration).isNotDone();
                release.countDown();
                reservation.get(8, TimeUnit.SECONDS);
                assertThat(configuration.get(8, TimeUnit.SECONDS)).isEqualTo("CONFIGURATION_CONFLICT");
                assertThat(agendaQueries.franjas(fixture.agenda())).containsExactlyElementsOf(
                        originalSchedule);
                assertThat(db.jdbc.queryForObject("SELECT count(*) FROM turno WHERE estado = 'CONFIRMADO'",
                        Long.class)).isOne();
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void configurationCommittedFirstIsVisibleToTheFollowingReservationValidation() throws Exception {
        try (var db = new TestDatabase(); var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            var fixture = CoreSchemaTest.fixture(db);
            var service = service(db);
            var agendaQueries = new AgendaQueries(db.jdbc);
            service.replaceHorario(List.of(attention("12:00", "14:00")));
            var coordinator = coordinator(db);
            var configurationWritten = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var configuration = threads.submit(() -> coordinator.write(fixture.agenda(), () -> {
                db.jdbc.update("DELETE FROM franja_semanal WHERE agenda_id = ?", fixture.agenda());
                insertRange(db, fixture.agenda(), attention("13:15", "14:00"));
                configurationWritten.countDown();
                await(release);
                return null;
            }));
            try {
                assertThat(configurationWritten.await(4, TimeUnit.SECONDS)).isTrue();
                var reservation = threads.submit(() -> coordinator.write(fixture.agenda(), () -> {
                    var appointmentStart = CoreSchemaTest.START.toInstant().atZone(ZoneOffset.UTC);
                    var appointmentEnd = CoreSchemaTest.START.plusMinutes(30).toInstant().atZone(ZoneOffset.UTC);
                    boolean covered = agendaQueries.franjas(fixture.agenda()).stream()
                            .filter(range -> range.tipo() == FranjaSemanal.Tipo.ATENCION)
                            .anyMatch(range -> range.diaSemana() == appointmentStart.getDayOfWeek()
                                    && !appointmentStart.toLocalTime().isBefore(range.horaInicio())
                                    && !appointmentEnd.toLocalTime().isAfter(range.horaFin()));
                    if (!covered) {
                        return "OUTSIDE_AVAILABILITY";
                    }
                    CoreSchemaTest.insertTurn(db.jdbc, fixture, UUID.randomUUID(), CoreSchemaTest.START,
                            CoreSchemaTest.START.plusMinutes(30), "CONFIRMADO");
                    return "CONFIRMED";
                }));
                AgendaWriteCoordinatorTest.awaitLockWait(db);
                assertThat(reservation).isNotDone();
                release.countDown();
                configuration.get(8, TimeUnit.SECONDS);
                assertThat(reservation.get(8, TimeUnit.SECONDS)).isEqualTo("OUTSIDE_AVAILABILITY");
                assertThat(db.jdbc.queryForObject("SELECT count(*) FROM turno WHERE estado = 'CONFIRMADO'",
                        Long.class)).isZero();
            } finally {
                release.countDown();
            }
        }
    }

    private static AvailabilityConfigurationService service(TestDatabase db) {
        return new AvailabilityConfigurationService(db.jdbc, new AgendaQueries(db.jdbc),
                new ConsultorioQueries(db.jdbc), coordinator(db));
    }

    private static AgendaWriteCoordinator coordinator(TestDatabase db) {
        return new AgendaWriteCoordinator(db.jdbc, new JdbcTransactionManager(db.dataSource()));
    }

    private static FranjaInput attention(String start, String end) {
        return attention(2, start, end);
    }

    private static FranjaInput attention(int day, String start, String end) {
        return new FranjaInput(UUID.randomUUID(), day, LocalTime.parse(start), LocalTime.parse(end), "ATENCION");
    }

    private static void insertRange(TestDatabase db, UUID agendaId, FranjaInput range) {
        db.jdbc.update("""
                INSERT INTO franja_semanal (id, agenda_id, dia_semana, hora_inicio, hora_fin, tipo)
                VALUES (?, ?, ?, ?, ?, ?)
                """, range.id(), agendaId, DayOfWeek.of(range.diaSemana()).getValue(), range.horaInicio(),
                range.horaFin(), range.tipo());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(7, TimeUnit.SECONDS)) {
                throw new AssertionError("No se liberó la sincronización de las transacciones");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Prueba interrumpida", interrupted);
        }
    }
}