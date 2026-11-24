# Design

## Context

Ver [proposal.md](proposal.md) y [C-12 archivado](../archive/2026-11-24-acceso-configuracion-web/design.md). La revisión de PR sobre C-12 señaló tres correcciones estrictamente acotadas al frontend sin tocar el backend ni los contratos de C-03 y C-06.

## Goals / Non-Goals

**Goals:**
- Evitar que un fallo de lectura en `GET /api/odontologo/horario` permita sobrescribir el horario real con `PUT []`.
- Conservar la precisión de segundos (`HH:mm:ss`) recibida del backend en franjas no modificadas.
- Distinguir tras un `403 CSRF_INVALID` en escrituras internas si la sesión expiró (cerrada desde otra pestaña o por inactividad/reinicio) o si solo venció el token CSRF.

**Non-Goals:**
- Modificar endpoints, filtros de Spring Security o DTOs del backend.
- Cambiar el flujo de `login` (`POST /api/odontologo/sesion`), donde no existe sesión previa que consultar ante `403 CSRF_INVALID`.

## Decisions

### D-01 — Estado explícito de carga en `HorarioSection.tsx`

Reemplazar el booleano `loading` por un estado discriminado `'loading' | 'loaded' | 'error'` y extraer `loadHorario` para reutilizarlo en el montaje y en el botón **Reintentar**:
- En `'loading'`: muestra indicador de carga y no renderiza el formulario.
- En `'error'`: muestra el mensaje de error y el botón **Reintentar**; no renderiza el formulario ni los botones **Agregar franja** / **Guardar horario**, garantizando que un `GET 500` nunca habilite un `PUT` vacío.
- En `'loaded'`: habilita el formulario incluso cuando `franjas` es `[]`.

### D-02 — Preservación de precisión horaria (`HH:mm:ss`) en `HorarioSection.tsx`

- Eliminar el recorte `slice(0, 5)` al cargar desde `GET /api/odontologo/horario` y al armar el payload de `PUT /api/odontologo/horario`: cada franja conserva el string exacto recibido (ej. `"09:00:30"`).
- Usar `step="1"` en los `<input type="time">` para que el control soporte valores con segundos sin truncarlos en el DOM.
- Al editar una franja, actualizar únicamente el campo modificado (`horaInicio` o `horaFin`) con el valor del input, manteniendo intactos los demás campos y franjas.
- Para la validación `horaInicio < horaFin`, normalizar a `HH:mm:ss` (agregando `:00` si tiene longitud 5) solo para comparar, sin mutar el valor guardado en estado.

### D-03 — Verificación de sesión tras `403 CSRF_INVALID` en `apiClient.ts` y `App.tsx`

- En `apiClient.ts` (`mutateJson`, usado por todas las escrituras internas y `logout`): cuando `response.status === 403` y el código resuelto es `CSRF_INVALID`, descartar el token CSRF (`clearCsrfToken()`), **no reenviar la escritura**, y consultar `GET /api/odontologo/sesion`:
  - Si `checkSession()` devuelve `SESSION_REQUIRED` (`401`), devolver `SESSION_REQUIRED` para que `App` y todas las secciones invoquen `onSessionExpired(message)`, limpien la vista interna y vuelvan al ingreso con mensaje claro.
  - Si `checkSession()` devuelve `200` con `autenticado: true`, devolver `CSRF_INVALID` (`403`) para mostrar el error en pantalla y permitir el reintento manual (que obtendrá un token nuevo vía `ensureCsrfToken()`).
- En `App.tsx`: mantener un estado local de error visible dentro de la vista autenticada para errores de `handleLogout` cuando la sesión sigue activa (ej. `CSRF_INVALID`), y derivar a `handleSessionExpired(result.message)` cuando `logout` devuelva `SESSION_REQUIRED`.

## Risks / Trade-offs

- [Un navegador o jsdom con `<input type="time">` puede normalizar el valor mostrado si no tiene `step="1"`] → Usar `step="1"` en ambos inputs de hora y verificar en Vitest/jsdom que el payload enviado en `PUT` conserva `09:00:30` y `12:00:45`.
- [Doble llamada HTTP solo en el caso excepcional de `403 CSRF_INVALID`] → Aceptable; ocurre únicamente ante rechazo `403`, nunca en lecturas ni en escrituras exitosas, y garantiza no reenviar escrituras automáticamente.

## Migration Plan

Sin cambios de esquema, variables ni backend. Se aplica sobre la rama `sebastian/c12` y se archiva actualizando `openspec/specs/dentist-configuration-web/spec.md`.

## UI Mockups

Decision: no (project policy)

## Revisión y aprobación

- **Aprobación de código CRITICAL (cambios de sesión y CSRF en `apiClient.ts` y `App.tsx`)**: **aprobada por Sebastián el 2026-11-24** («esta todo bien, pasemos con el apply»).
