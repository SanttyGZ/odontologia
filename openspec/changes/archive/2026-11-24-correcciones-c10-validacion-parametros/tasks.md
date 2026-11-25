# Tasks

## 1. Validación estricta en DentistAgendaController (TDD)

- [x] 1.1 Agregar casos de regresión en `DentistAgendaControllerTest` para `fecha` (`""`, `" "`, `"2026-11-26Z"`, `"2026-11-26-03:00"`, `"2026-02-30"`, `"+10000-01-01"`, `"+999999999-12-31"` → `400 VALIDATION_ERROR` con `verifyNoInteractions(service)`, más `"2026-11-26"` válido en `DIA`/`SEMANA` y omisión de `fecha`) y para `turnoId` (`"1-1-1-1-1"` → `400 VALIDATION_ERROR` con `verifyNoInteractions(service)`, UUID mayúsculas/minúsculas → `200` existente y `404 TURNO_NOT_FOUND` inexistente).
- [x] 1.2 Implementar validación estricta de `fecha` (`YYYY-MM-DD` calendario estricto) y `turnoId` (UUID hexadecimal `8-4-4-4-12`) en `DentistAgendaController` rechazando con `AvailabilityException.validation()` antes de invocar a `DentistAgendaService`.

## 2. Verificación del backend e integración

- [x] 2.1 Ejecutar pruebas unitarias del backend (`DentistAgendaControllerTest`, `DentistAgendaServiceTest` y suite de regresión), `compileIntegrationTestJava`, `bootJar`, `openspec validate correcciones-c10-validacion-parametros --strict --no-interactive` y `git diff --check`; ejecutar `DentistAgendaIntegrationTest` contra PostgreSQL 17.12 si Docker Compose está disponible o documentar el límite en `verification.md`.
