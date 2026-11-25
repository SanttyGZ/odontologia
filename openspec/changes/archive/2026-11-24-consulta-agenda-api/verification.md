# Verificación de C-10 (`consulta-agenda-api`)

## Resultado

Implementación completa de C-10 (`US-006`, `FL-01`, gobernanza `BAJO`) bajo Strict TDD y `JAVA-01`, reutilizando sin modificaciones `DentistSecurityConfiguration` (`C-03`) y el esquema `V1__core_models.sql` (`C-02`).

## Evidencia ejecutada (Windows local, 2026-11-24)

- **Entorno**: Windows, Eclipse Temurin JDK `21.0.12.1+1`, Gradle `9.9.2`.
- **Pruebas unitarias ejecutadas** (`gradle.bat -p backend --no-daemon test ...`):
  - `dev.odontologia.agenda.DentistAgendaServiceTest`: **9/9 pasaron** (0 fallos, 0 omitidos).
  - `dev.odontologia.agenda.DentistAgendaControllerTest`: **8/8 pasaron** (0 fallos, 0 omitidos).
  - `dev.odontologia.agenda.AvailabilityErrorAdviceTest`: **1/1 pasó** (Safety Net).
  - Suites de regresión ejecutadas en la misma corrida (`AvailabilityConfigurationControllerTest`, `PublicAvailabilityControllerTest`, `PublicBookingControllerTest`, `AvailabilityRulesTest`, `LocalEnvironmentPostProcessorTest`): **todas pasaron** (`BUILD SUCCESSFUL`).
- **Compilación de integración (`compileIntegrationTestJava`)**: compiló sin errores, incluyendo `dev.odontologia.DentistAgendaIntegrationTest`.
- **Empaquetado (`bootJar`)**: `BUILD SUCCESSFUL`.
- **OpenSpec (`openspec validate consulta-agenda-api --strict --no-interactive`)**: `Change 'consulta-agenda-api' is valid`.
- **`git diff --check`**: sin errores de formato.
- **Integridad de gobernanza `BAJO`**: `git status -sb` confirma que no se modificó `DentistSecurityConfiguration`, `dev.odontologia.acceso` ni migraciones Flyway.

## Evidencia TDD

| Tarea | Archivo de prueba | Capa | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| 1.1–1.2 | `DentistAgendaServiceTest.java` | Unit | ✅ 1/1 | ✅ Falló compilación | ✅ Pasó | ✅ 9 casos | ✅ `requireConfiguration()` |
| 2.1–2.2 | `DentistAgendaControllerTest.java` | Controller | ✅ 1/1 | ✅ Falló compilación | ✅ Pasó | ✅ 8 casos | ✅ Limpio |

## Límites de entorno y verificación pendiente en Linux/WSL2 con Docker

- En esta máquina Windows no hay Docker Compose ni WSL2 disponibles en `127.0.0.1:5433`, por lo que `DentistAgendaIntegrationTest` quedó compilado (`compileIntegrationTestJava`) pero su ejecución contra bases efímeras PostgreSQL 17.12 (`TestDatabase`) queda pendiente para entorno con Docker Compose o para el cierre integrado en `C-16`:
  ```bash
  python3 scripts/with-local-db.py --integration ./backend/gradlew -p backend integrationTest --tests dev.odontologia.DentistAgendaIntegrationTest
  ```
