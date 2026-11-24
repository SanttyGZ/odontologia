# Tasks

## 1. Validación de paciente y disponibilidad de slot (TDD)

- [x] 1.1 Agregar pruebas unitarias en `AvailabilityRulesTest` para validación de datos de paciente (`PB-04`) y validación de intervalo puntual de reserva (`isSlotAvailable`: pasos de 15 min, bordes `PB-01`, franja única, pausa, bloqueo, turno confirmado y contiguos).
- [x] 1.2 Implementar validación/normalización de paciente y `isSlotAvailable` en `AvailabilityRules`.

## 2. Controlador, servicio y persistencia de paciente (TDD)

- [x] 2.1 Agregar `PublicBookingControllerTest` cubriendo `POST /api/public/turnos` (`201 Created`, `400 VALIDATION_ERROR`, `404 MOTIVO_NOT_FOUND`, `409 SLOT_UNAVAILABLE`, `409 AGENDA_BUSY`, sin exponer secretos ni datos de otros pacientes).
- [x] 2.2 Implementar `PacienteQueries.findExact` / `insert`, `PublicBookingDtos`, `PublicBookingService`, `PublicBookingController` y registrar en `AvailabilityErrorAdvice` y `AvailabilityException`.

## 3. Integración PostgreSQL y concurrencia real

- [x] 3.1 Agregar `PublicBookingIntegrationTest` contra PostgreSQL 17.12 probando reserva confirmada end-to-end (`Turno` + `CredencialGestion` + `Origen/IntencionNotificacion`), política `PB-04` (reutilización exacta vs coincidencia parcial), rollback total ante fallo, y carreras sincronizadas (intervalos iguales, parcialmente solapados, y carrera con cambio de horario/bloqueo).
- [x] 3.2 Ejecutar `AvailabilityRulesTest`, `PublicBookingControllerTest`, `PublicBookingIntegrationTest`, `bootJar`, `openspec validate reserva-confirmada-api --strict --no-interactive` y `git diff --check`.
