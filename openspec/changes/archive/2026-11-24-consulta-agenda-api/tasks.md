# Tasks

## 1. Consultas y servicio de agenda interna (TDD)

- [x] 1.1 Agregar pruebas unitarias para `DentistAgendaService` verificando cálculo de período diario y semanal (lunes a domingo ISO) en `America/Argentina/Mendoza` con `Clock` fijo, valores por defecto al omitir `vista` o `fecha`, rechazo de `vista` inválida (`400 VALIDATION_ERROR`), error `404 TURNO_NOT_FOUND` cuando el turno no existe en la agenda y `500 TECHNICAL_FAILURE` si falta la configuración del consultorio.
- [x] 1.2 Implementar proyecciones y consultas SQL `JOIN` sin N+1 en `AgendaQueries`, DTOs en `DentistAgendaDtos`, lógica de lectura en `DentistAgendaService` y `AvailabilityException.turnoNotFound()`.

## 2. Controlador HTTP y contrato de errores (TDD)

- [x] 2.1 Agregar pruebas de controlador (`DentistAgendaControllerTest`) para `GET /api/odontologo/agenda` y `GET /api/odontologo/turnos/{turnoId}` comprobando serialización JSON exacta (incluidos turnos `CONFIRMADO` y `CANCELADO`, agenda vacía `200 OK` con `turnos: []`, los 5 datos básicos del paciente en detalle y ausencia de campos internos), así como respuestas `400 VALIDATION_ERROR`, `404 TURNO_NOT_FOUND` y `500 TECHNICAL_FAILURE`.
- [x] 2.2 Implementar `DentistAgendaController` en `/api/odontologo` y registrarlo en `AvailabilityErrorAdvice`.

## 3. Integración con PostgreSQL, seguridad C-03 y verificación

- [x] 3.1 Agregar pruebas de integración (`DentistAgendaIntegrationTest` contra PostgreSQL real y comprobación de seguridad sin sesión sobre `/api/odontologo/agenda` y `/api/odontologo/turnos/{id}`) verificando bordes horarios en `America/Argentina/Mendoza`, ordenamiento determinístico, estados `CONFIRMADO`/`CANCELADO`, detalle de paciente y rechazo `401 SESSION_REQUIRED` en llamadas directas no autenticadas sin modificar `DentistSecurityConfiguration`.
- [x] 3.2 Ejecutar pruebas unitarias y de integración aplicables, `bootJar`, `openspec validate consulta-agenda-api --strict --no-interactive` y `git diff --check`, registrando `verification.md` antes de archivar.
