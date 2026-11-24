# Proposal

## Why

La revisión de la PR #4 detectó dos fallos en `HorarioSection.tsx`: respuestas de `GET /api/odontologo/horario` que llegan fuera de orden pueden sobrescribir el resultado de «Reintentar», y horas con microsegundos persistidas por C-06 (como `09:00:30.123456`) hacen que `<input type="time">` quede vacío en Chromium y bloquee el guardado por `required`.

## What Changes

- **Consultas de horario fuera de orden**: identificar cada carga vigente y descartar cualquier respuesta de consultas anteriores o posterior al desmontaje del componente.
- **Horas con microsegundos en controles nativos**: separar el valor visible del control (`HH:mm:ss`) del valor exacto devuelto por la API, indicar la fracción (ej. `.123456`) junto al campo, conservar el valor exacto en el `PUT` mientras no se edite ese campo y validar los intervalos `inicio < fin` con precisión completa de microsegundos.

## Capabilities

### New Capabilities

<!-- Ninguna capacidad nueva. -->

### Modified Capabilities

- `dentist-configuration-web`: precisar en `CW-04` el descarte de respuestas de consultas anteriores/desmontadas y el tratamiento de horas con microsegundos en controles nativos y validación.

## Impact

- **Código afectado**: únicamente `frontend/src/components/HorarioSection.tsx` y `frontend/src/components/HorarioSection.test.tsx`.
- **Sin cambios en sesión, CSRF ni backend**: se conservan intactos `apiClient.ts`, `App.tsx`, el contrato de C-06 y el backend.
- **Gobernanza**: **MEDIO** (reglas de formulario y estado local de horario).
