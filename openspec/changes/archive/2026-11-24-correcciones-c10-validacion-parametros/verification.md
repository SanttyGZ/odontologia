# Verificación de `correcciones-c10-validacion-parametros`

## Qué cambió

- **`backend/src/main/java/dev/odontologia/agenda/DentistAgendaController.java`**:
  - `GET /api/odontologo/agenda`: `fecha` pasa a `@RequestParam(required = false) String fecha`; si es `null` (omitida), conserva el default del día actual en `consultorio.zonaHoraria` vía `Clock`. Si está presente, `parseFecha(String)` exige el patrón exacto `^\d{4}-\d{2}-\d{2}$` y calendario válido con `DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT)`, lanzando `AvailabilityException.validation()` (`400 {"codigo":"VALIDATION_ERROR"}`) antes de invocar a `DentistAgendaService`.
  - `GET /api/odontologo/turnos/{turnoId}`: `turnoId` pasa a `@PathVariable String turnoId`; `parseTurnoId(String)` exige formato hexadecimal estricto `8-4-4-4-12` (`^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$`), lanzando `AvailabilityException.validation()` (`400 {"codigo":"VALIDATION_ERROR"}`) antes de invocar al servicio y preservando `200 OK` / `404 TURNO_NOT_FOUND` para UUID válidos.
- **`backend/src/test/java/dev/odontologia/agenda/DentistAgendaControllerTest.java`**:
  - Regresiones parametrizadas para `fecha` (`""`, `" "`, `"2026-11-26Z"`, `"2026-11-26-03:00"`, `"2026-02-30"`, `"+10000-01-01"`, `"+999999999-12-31"`) en `DIA` y `SEMANA` con `verifyNoInteractions(service)`.
  - Regresión para `fecha=2026-11-26` en `DIA` y `SEMANA` (`200 OK`).
  - Regresiones parametrizadas para `turnoId` (`"1-1-1-1-1"`, `"no-es-uuid"`, longitud incorrecta, carácter no hexadecimal) con `verifyNoInteractions(service)`.
  - Regresión para UUID en mayúsculas (`200 OK`) y minúsculas (`404 {"codigo":"TURNO_NOT_FOUND"}`).

## Evidencia ejecutada (Windows, 2026-11-24)

- **Suite unitaria completa (`gradle.bat -p backend --no-daemon test`)**: 81 pruebas ejecutadas, 3 fallos preexistentes en `DentistCredentialFileTest` (`UnsupportedOperationException` por permisos POSIX no soportados en Windows NTFS; sin cambios en C-03).
- **Suite unitaria filtrada sin tests POSIX** (`--tests dev.odontologia.agenda.* --tests dev.odontologia.disponibilidad.* --tests dev.odontologia.config.*`): **78/78 pasaron** (`DentistAgendaControllerTest` **21/21**, `DentistAgendaServiceTest` **9/9**, `AvailabilityErrorAdviceTest` **1/1**, más `AvailabilityConfigurationControllerTest`, `AvailabilityRulesTest`, `PublicAvailabilityControllerTest`, `PublicBookingControllerTest`, `LocalEnvironmentPostProcessorTest`).
- **`compileIntegrationTestJava` y `bootJar`**: **`BUILD SUCCESSFUL`**.
- **`openspec validate correcciones-c10-validacion-parametros --strict --no-interactive`**: **`Change 'correcciones-c10-validacion-parametros' is valid`**.
- **`git diff --check`**: sin errores de formato.

## Evidencia TDD

| Tarea | Archivo de prueba | Capa | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| 1.1–1.2 | `DentistAgendaControllerTest.java` | Controller | ✅ 8/8 | ✅ 8/21 fallaron (`1-1-1-1-1`, `""`, `" "`, `Z`, `-03:00`, `+10000`, `+999999999`, UUID corto) | ✅ 21/21 pasaron | ✅ 13 casos nuevos | ✅ Limpio |

## Límite de integración PostgreSQL 17.12

- `Test-NetConnection -ComputerName 127.0.0.1 -Port 5433 -InformationLevel Quiet` devolvió `False` (en este host Windows no hay Docker Compose ni WSL2 ejecutando PostgreSQL 17.12 en `127.0.0.1:5433`).
- `DentistAgendaIntegrationTest` compiló sin errores (`compileIntegrationTestJava`); su ejecución real contra PostgreSQL 17.12 queda pendiente para entorno Linux/WSL2 con Docker Compose:
  ```bash
  python3 scripts/with-local-db.py --integration ./backend/gradlew -p backend integrationTest --tests dev.odontologia.DentistAgendaIntegrationTest
  ```
