# Verificación

**Cambio:** `correcciones-c09-autogestion-turnos-api`  
**Fecha:** 2026-11-25

## Resultados

- Unitarias completas, `backend\gradlew.bat -p backend test --rerun-tasks`: 68/71 pasaron (recibo Active Stack `1dcf91a9921a3ab5693a9690bccb3f11`). Las tres fallidas son de `DentistCredentialFileTest`; Java sobre Windows no admite `posix:permissions` ni `Files.getPosixFilePermissions`. Las 19 unitarias focalizadas de `CredencialesGestionTest`, `GestionTurnoServiceTest` y `GestionTurnoControllerTest` pasaron (recibo `f5570e850bc33b69ba2848ef4492d342`).
- Integración completa, `backend\gradlew.bat -p backend integrationTest --rerun-tasks`: 38/52 pasaron (recibo `568229ff5def016ceedc5b235af2f8c1`). Las 14 fallidas dependen de archivos de credenciales protegidos con permisos POSIX: 5 operaciones de archivos de acceso y 9 casos de `MaterialEnlace`/recuperación. Ubuntu WSL está instalado, pero no tiene Java disponible en esta sesión.
- Integración C-09, `backend\gradlew.bat -p backend integrationTest --tests dev.odontologia.GestionTurnoIntegrationTest --rerun-tasks`: 7/7 pasaron contra PostgreSQL real 17.12 en un contenedor temporal, puerto `127.0.0.1:55433`, sin volumen. Incluye destinatario recuperado para cancelación y para `CAMBIO`/`RECORDATORIO`, conservación del email del paciente e historial enviado, credencial vigente en la reprogramación y rechazo `SAME_INTERVAL` sin cambios (recibo `f51b52f59a88c628e7c0bd7fe7ae48c9`). El contenedor se eliminó al terminar.
- `backend\gradlew.bat -p backend bootJar --rerun-tasks`: correcto (recibo `456ca935c3a1cbb286a2245de5210532`).
- `openspec validate correcciones-c09-autogestion-turnos-api --type change --strict --json`: correcto, 1/1 antes del archivo.
- `openspec archive correcciones-c09-autogestion-turnos-api --yes --json`: archivado como `2026-11-25-correcciones-c09-autogestion-turnos-api`; OpenSpec actualizó dos requisitos de `appointment-self-management`; 9/9 tareas completadas.
- `openspec validate --all --strict --json`: correcto, 12/12 especificaciones después del archivo.
- `openspec validate --archived --strict --json`: correcto, 14/14 changes archivados.
- `git diff --check`: correcto, sin errores de whitespace.

## Decisiones pendientes

- **C-14 / CRITICAL:** aprobar o rechazar antes de implementar el contrato candidato `GET /api/gestion/turno/disponibilidad?desde=YYYY-MM-DD&hasta=YYYY-MM-DD`, autenticado con `X-Management-Token` vigente. La API derivaría internamente el `motivoId` del turno autorizado, delegaría el cálculo en C-07 y respondería `200`, `Cache-Control: no-store` y `{ "zonaHoraria": "...", "horarios": [{"inicio":"...", "fin":"..."}] }`; no aceptaría ni expondría `motivoId` o datos adicionales del turno. No se implementó ruta ni se amplió `GET /api/gestion/turno`.
- **PB-06 / C-14:** pendiente acordar con el odontólogo el contacto operativo fuera del plazo. C-09 conserva `409 MANAGEMENT_WINDOW_CLOSED`; no se agregó canal, alta manual ni excepción.
- **C-10 / roadmap:** al iniciar, `CHANGES.md` mostraba C-10 como `[ ]` pendiente y C-12 como `[x]` completado/archivado. Se conservaron ambas filas sin cambios; la afirmación de cierres conjuntos de C-10 y C-12 no coincide con el estado local de C-10.
- La suite completa debe repetirse en un entorno Linux con Java 21 para verificar los casos POSIX de C-04; esta limitación no afecta los 7/7 casos de integración específicos de C-09.
