# Tasks

## 1. Consultas de horario fuera de orden

- [x] 1.1 Escribir pruebas que fallen en `frontend/src/components/HorarioSection.test.tsx` con promesas diferidas (lectura inicial en vuelo con `08:00`, otra falla, «Reintentar» devuelve `09:00`, y luego resuelve la primera con `08:00`) y de desmontaje, comprobando que la pantalla y el `PUT` conservan `09:00`; implementar `latestRequestIdRef` en `frontend/src/components/HorarioSection.tsx` y pasar `npm test`.

## 2. Horas con microsegundos

- [x] 2.1 Escribir pruebas que fallen en `frontend/src/components/HorarioSection.test.tsx` para `09:00:30.123456`–`12:00:45.654321` (input muestra `09:00:30`, UI indica `.123456` junto al campo, guardar sin editar y editar otra franja conservan `09:00:30.123456` en `PUT`, y comparación válida/inválida de dos horas en el mismo segundo con microsegundos distintos); implementar los helpers y el renderizado en `frontend/src/components/HorarioSection.tsx` y pasar `npm test`.

## 3. Verificación e integración

- [x] 3.1 Ejecutar `npm ci`, `typecheck`, `vitest`, `build`, `openspec validate correcciones-c12-horario-concurrencia-precision --strict --no-interactive`, `git diff --check` y registrar en `openspec/changes/correcciones-c12-horario-concurrencia-precision/verification.md` los resultados reales y las comprobaciones pendientes de Chromium/Spring Boot/PostgreSQL en Linux. *(Verificado en Windows: `npm ci`, `typecheck`, 42/42 tests Vitest, `build`, `openspec validate` y `git diff --check` OK; verificación en Chromium y recorrido autenticado en Linux/WSL2 documentados como pendientes en `verification.md`.)*

## Workflow follow-up

- Archivar el change con `openspec archive correcciones-c12-horario-concurrencia-precision` tras verificar todas las tareas.
- Actualizar la nota de C-12 en `CHANGES.md` solo después del archivo.
