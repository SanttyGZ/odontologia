# Verificación de C-06

## Resultado

C-06 `configuracion-disponibilidad-api` completado y archivado con OpenSpec. Las nueve tareas quedaron verificadas en OpenSpec.

## Evidencia

- Entorno: Ubuntu WSL2, Eclipse Temurin 21.0.13.1+2, Gradle Wrapper 9.9.2 y PostgreSQL 17.12 local de Compose.
- `python3 scripts/with-local-db.py --integration ./backend/gradlew -p backend test integrationTest bootJar --no-daemon`: `BUILD SUCCESSFUL`; la suite unitaria, la suite de integración con PostgreSQL real y bootJar terminaron sin fallos. Gradle informó que algunas tareas estaban `UP-TO-DATE`.
- La suite de integración incluye `DentistAccessTest`; pasó después de limitar `AvailabilityErrorAdvice` a `AvailabilityConfigurationController`, conservando el 404 de `/login` y el mapeo específico de errores C-06.
- Pruebas unitarias específicas de `AvailabilityErrorAdviceTest` y `AvailabilityConfigurationControllerTest`: pasaron.
- `openspec validate configuracion-disponibilidad-api --strict --no-interactive`: change válido.
- `git diff --check`: sin errores.
- La suite PostgreSQL cubrió conflictos, rollback, autorización/CSRF y las dos carreras de reserva/configuración.

## Límites y seguimiento

- PB-01 permanece pendiente para C-07/C-08/C-09.
- Los responsables de C-07 y C-12 todavía deben revisar el contrato HTTP antes de implementar sus consumidores. Esta coordinación no se registró como aprobación.
- El servicio local se detuvo después de la verificación y se conservó el volumen `odontologia-local_postgres-data`.
