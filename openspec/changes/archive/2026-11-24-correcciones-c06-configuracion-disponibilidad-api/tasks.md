# Tasks

## 1. Decisiones y contrato C-06

- [x] 1.1 Registrar en la KB la confirmación explícita de Santiago del 2026-11-24 para PB-02, PB-03 y PB-05a/b, con el alcance confirmado y referencias al diseño/spec archivados; verificar que las reglas no cambien y que `openspec validate --strict --no-interactive` pase.
- [x] 1.2 Precisar en el delta `availability-configuration` el rechazo HTTP de duraciones JSON fraccionarias, la aceptación de franjas contiguas y el requisito de una sola franja para cada turno confirmado; verificar el delta con OpenSpec estricto.

## 2. Duraciones enteras en la API

- [x] 2.1 Agregar una regresión HTTP con servicio y PostgreSQL reales para POST y PUT: aceptar duraciones enteras admitidas; rechazar `30.5` y `"45"` con `400 VALIDATION_ERROR`; comprobar que POST no crea y PUT conserva el motivo previo; exigir un token JSON entero en el DTO/controlador y ejecutar las pruebas focalizadas.

## 3. Cobertura por una sola franja de atención

- [x] 3.1 Agregar pruebas de regla e integración HTTP/PostgreSQL para turnos dentro de una franja, turnos que cruzan franjas contiguas, respuesta `409 CONFIGURATION_CONFLICT` y conservación íntegra del horario y turno; corregir la validación y mantener la aceptación de franjas contiguas.

## 4. Concurrencia con configuración confirmada primero

- [x] 4.1 Reemplazar en la prueba configuración-primero el SQL directo por `AvailabilityConfigurationService.replaceHorario`; sincronizar los bloqueos PostgreSQL para demostrar que el fixture de reserva espera, relee la configuración confirmada y rechaza el intervalo sin insertar turno; conservar y ejecutar la carrera reserva-primero.

## 5. Verificación e integración

- [x] 5.1 Ejecutar `test`, `integrationTest` y `bootJar` mediante el Wrapper y PostgreSQL local, validar el change con `openspec validate --strict --no-interactive`, ejecutar `git diff --check` y registrar resultados y cualquier limitación; no subir ni fusionar la PR. (Verificado en Windows con JDK 21.0.13.1+2 y PostgreSQL 17.12: `bootJar`, todas las pruebas unitarias/integración/concurrencia de C-06 y `git diff --check` pasaron; los tests POSIX preexistentes de C-03/C-04 en `DentistCredentialFileTest`, `AvailabilityConfigurationSecurityTest`, `DentistAccessTest` y `RecuperacionIntencionesTest` requieren Linux/WSL por `PosixFilePermissions` en NTFS.)

## Workflow follow-up

- C-06 original permanece archivado. Archivar este seguimiento con OpenSpec solo cuando todas las tareas estén verificadas.
- Actualizar `CHANGES.md` después del archivo: conservar C-04/C-05 cerrados, reflejar la corrección C-06 y mantener C-07 pendiente.
- Solicitar autorización antes de hacer push o fusionar la PR.
