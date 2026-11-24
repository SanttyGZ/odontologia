# Proposal

## Why

La revisión de PR sobre C-12 (`acceso-configuracion-web`, ya archivado) detectó tres brechas en el frontend que pueden provocar pérdida silenciosa del horario configurado o manejo impreciso de pérdida de sesión ante `403 CSRF_INVALID`.

## What Changes

- **Horario semanal ante fallo de lectura**: si `GET /api/odontologo/horario` falla, mostrar el error, permitir reintentar la consulta y bloquear la edición y el guardado hasta obtener una respuesta válida; si la respuesta exitosa devuelve cero franjas, permitir crear un horario.
- **Precisión de horas no modificadas**: conservar los valores originales de `horaInicio` y `horaFin` (p. ej. `09:00:30`–`12:00:45`) cuando el usuario guarda sin editar o edita otra franja, sin truncarlos a `HH:mm`.
- **Pérdida de sesión ante `403 CSRF_INVALID`**: al recibir `403 CSRF_INVALID` en una escritura interna, descartar el token CSRF, no reenviar la escritura y consultar `GET /api/odontologo/sesion`. Si responde `401 SESSION_REQUIRED`, limpiar la vista interna y volver al ingreso con un mensaje claro; si la sesión sigue activa (`200`), mostrar el error y permitir un reintento manual con un token nuevo.

## Capabilities

### New Capabilities

<!-- Ninguna capacidad nueva. -->

### Modified Capabilities

- `dentist-configuration-web`: precisar en `CW-02` la verificación de sesión tras `403 CSRF_INVALID` sin reenvío automático y en `CW-04` el bloqueo ante fallo de `GET /api/odontologo/horario` y la preservación de precisión de horas no editadas.

## Impact

- **Código afectado**: únicamente `frontend/` (`src/components/HorarioSection.tsx`, `src/api/apiClient.ts`, `src/App.tsx` y sus pruebas Vitest/RTL).
- **Sin cambios en backend**: no se modifican endpoints, seguridad ni esquema PostgreSQL.
- **Gobernanza**: **CRITICAL** por modificar el manejo de sesión y CSRF en el cliente; requiere aprobación explícita de Sebastián antes de escribir código.
