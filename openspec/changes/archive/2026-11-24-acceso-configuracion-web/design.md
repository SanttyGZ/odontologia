# Design

## Context

Ver [proposal.md](proposal.md) para el alcance y [la spec de dentist-configuration-web](specs/dentist-configuration-web/spec.md) para los requisitos observables. El frontend actual en `frontend/` solo contiene Vite 8.3.8, React 19.3.1, TypeScript 7.0.2 estricto y proxy `/api` → `http://127.0.0.1:8080`. El backend ya implementa:

- **C-03 ([diseño C-03](../archive/2026-11-22-acceso-interno-odontologo/design.md) §D-03 y §D-04)**: sesión en memoria con cookie `HttpOnly` (`SameSite=Strict`, `Path=/api/odontologo`), token CSRF en `GET /api/odontologo/csrf`, ingreso en `POST /api/odontologo/sesion`, consulta en `GET /api/odontologo/sesion` y cierre en `POST /api/odontologo/sesion/cierre`.
- **C-06 ([diseño C-06](../archive/2026-11-24-configuracion-disponibilidad-api/design.md) §D-01)**: endpoints bajo `/api/odontologo/motivos`, `/api/odontologo/horario` y `/api/odontologo/bloqueos`, y el seguimiento [`correcciones-c06-configuracion-disponibilidad-api`](../correcciones-c06-configuracion-disponibilidad-api/design.md) que exige enteros JSON estrictos en `duracionMinutos` sin cambiar rutas ni payloads válidos.

La política visual del proyecto es `off` (AT-11), por lo que no hay sistema de diseño ni gate de mockups.

## Goals / Non-Goals

**Goals:**
- Consumir de forma segura y fiel los contratos HTTP de C-03 y C-06 sin modificar backend ni agregar dependencias pesadas innecesarias.
- Garantizar cumplimiento estricto de `TS-01` (cero `any`, validación en runtime con guardas de tipo sobre `unknown`).
- Mantener una interfaz clara, operable con teclado y legible en móvil (`390×844`) y escritorio (`1440×900`).

**Non-Goals:**
- Agregar router de SPA, bibliotecas de estado global o frameworks de componentes UI.
- Implementar la pantalla de agenda de turnos (C-15) ni rutas públicas de pacientes (C-13, C-14).
- Modificar políticas de seguridad, CORS, cookies o endpoints del backend.

## Decisions

### D-01 — Navegación interna por estado React sin router ni librerías de UI

Al tratarse de una única aplicación interna de configuración (ingreso + tres secciones: motivos, horario semanal y bloqueos), se gestiona la vista activa mediante estado local de React (`'motivos' | 'horario' | 'bloqueos'`) cuando hay sesión autenticada, y la pantalla de ingreso cuando no la hay. Las llamadas HTTP usan `fetch` nativo con rutas relativas `/api/odontologo/...` y `credentials: 'same-origin'` para compartir el mismo origen en loopback vía el proxy de Vite (`127.0.0.1:5173` → `127.0.0.1:8080`).

- *Alternativa descartada*: incorporar `react-router-dom` o librerías de estado/fetch (`react-query`, `axios`). Añaden dependencias y complejidad que no aportan valor para cuatro vistas acotadas.

### D-02 — Cliente HTTP único tipado con validación en runtime (`unknown` + type guards)

Centralizar todas las peticiones en un módulo `apiClient` que:
1. Adjunte el encabezado CSRF (`headerName` devuelto por `GET /api/odontologo/csrf`) en toda operación no `GET`.
2. Devuelva una unión discriminada `{ ok: true; data: T } | { ok: false; status: number; codigo: ApiErrorCode; message: string }` en lugar de lanzar excepciones opacas.
3. Valide toda respuesta JSON (`csrf`, `sesion`, `motivo`, `horario`, `bloqueo`, `error.codigo`) pasando de `unknown` a tipos concretos mediante type guards explícitos, cumpliendo `TS-01` sin usar `any` ni casts ciegos.

- *Alternativa descartada*: `as T` directo sobre `response.json()`. Incumple el espíritu de `TS-01` y ocultaría cambios de contrato del backend.

### D-03 — Ciclo de vida de sesión y CSRF fiel a C-03 D-04

Seguir exactamente el protocolo de C-03:
- Al montar la app: llamar `GET /api/odontologo/sesion`. Si devuelve `401 SESSION_REQUIRED`, mostrar el formulario de ingreso.
- **Sin polling**: no hacer peticiones periódicas para consultar o mantener viva la sesión (C-03 D-02).
- **CSRF en memoria**: obtener `{ token, headerName }` con `GET /api/odontologo/csrf` antes del ingreso o de la primera escritura si no existe en memoria. Descartar el token en memoria tras `POST /api/odontologo/sesion` (`204`), tras `POST /api/odontologo/sesion/cierre` (`204`) o ante cualquier `403 CSRF_INVALID` o `401 SESSION_REQUIRED`.
- **Nunca reenviar escrituras automáticamente**: si una escritura falla por `403 CSRF_INVALID`, `401 SESSION_REQUIRED`, `409` o `500`, informar al usuario y dejar que reintente de forma explícita. En `403 CSRF_INVALID`, refrescar el token en segundo plano o al reintentar para que el siguiente clic del usuario tenga token vigente.
- **Mismo host loopback**: documentar que la cookie `Path=/api/odontologo` exige operar en `127.0.0.1:5173` (no mezclar `localhost` y `127.0.0.1`).

### D-04 — Validación preventiva en cliente sin reemplazar la autoridad del backend

En formularios (nombre no vacío, duración entera múltiplo de 15 entre 15 y 240 mediante selector o control numérico restringido a enteros, `horaInicio < horaFin`, inicio < fin en bloqueos), el cliente valida antes de enviar para dar feedback inmediato, pero nunca asume que una respuesta exitosa local equivale a persistencia: siempre refleja lo devuelto por la API y conserva lo editado si la API responde `400` o `409 CONFIGURATION_CONFLICT`.

### D-05 — Zona horaria de los bloqueos en el consultorio (**Open Question para Sebastián**)

El backend de C-06 interpreta el horario semanal en la zona persistida del consultorio (`America/Argentina/Mendoza`) y recibe/devuelve los bloqueos (`inicio`, `fin`) como instantes ISO-8601 (`Instant`). Actualmente, ningún endpoint de C-03 ni C-06 expone la zona horaria del consultorio al frontend.

Regla del proyecto (`AGENTS.md` / C-01): **no inferir la zona del consultorio desde la zona de la computadora/navegador**. Para convertir fecha+hora local ingresada por el odontólogo en un `Instant` ISO-8601 exacto, existen dos opciones:
- **Opción A (recomendada en C-12, sin tocar backend)**: leer la zona configurada desde una variable de entorno de Vite (`VITE_APP_TIME_ZONE`, con valor explícito `America/Argentina/Mendoza` alineado al entorno local de `README.md`, sin usar `Intl` del sistema) y convertir mediante `Intl.DateTimeFormat` parametrizado con esa zona IANA explícita.
- **Opción B (requiere change de backend)**: agregar un endpoint de configuración o incluir `zonaHoraria` en `GET /api/odontologo/sesion` (requiere abrir cambio en backend, fuera del alcance de C-12).

Se deja como pregunta explícita a Sebastián antes de implementar la pantalla de bloqueos.

### D-06 — Herramientas de prueba TDD en frontend (**Decisión pendiente de aprobación**)

`frontend/package.json` hoy no incluye runner de pruebas. Para cumplir Strict TDD (pruebas unitarias y de componentes antes del código de producción) se propone incorporar como `devDependencies` en `frontend/package.json`:
- `vitest`, `@testing-library/react`, `@testing-library/user-event`, `jsdom` (fijando versiones compatibles con Vite `8.3.8` y React `19.3.1` al momento de aplicar) y el script `"test": "vitest run"`.
- Para la verificación E2E en navegador real contra backend + PostgreSQL, usar Playwright mediante la skill local `webapp-testing` sin agregar dependencias al bundle de producción.

Dado que `AGENTS.md` pide justificar cualquier librería nueva, se solicita confirmación de Sebastián para estas `devDependencies` de prueba.

### D-07 — Revisión del contrato HTTP de C-06 por el responsable de C-12

Se verificó directamente contra [AvailabilityDtos.java](../../../../backend/src/main/java/dev/odontologia/disponibilidad/AvailabilityDtos.java), [AvailabilityConfigurationController.java](../../../../backend/src/main/java/dev/odontologia/disponibilidad/AvailabilityConfigurationController.java) y [AvailabilityErrorAdvice.java](../../../../backend/src/main/java/dev/odontologia/disponibilidad/AvailabilityErrorAdvice.java):
- `GET /api/odontologo/motivos` → `[{ id: string, nombre: string, duracionMinutos: number }]`
- `POST /api/odontologo/motivos` (`{ nombre, duracionMinutos }` entero JSON) → `201` `MotivoResponse`
- `PUT /api/odontologo/motivos/{motivoId}` (`{ nombre, duracionMinutos }` entero JSON) → `200` `MotivoResponse` | `404 MOTIVO_NOT_FOUND`
- `GET /api/odontologo/horario` → `{ franjas: [{ id, diaSemana (1..7), horaInicio, horaFin, tipo ("ATENCION"|"PAUSA") }] }`
- `PUT /api/odontologo/horario` (`{ franjas }`) → `200` `HorarioResponse` | `409 CONFIGURATION_CONFLICT`
- `GET /api/odontologo/bloqueos` → `[{ id, inicio (ISO Instant), fin (ISO Instant), descripcion }]`
- `POST /api/odontologo/bloqueos` (`{ inicio, fin, descripcion }`) → `201` `BloqueoResponse` | `409 CONFIGURATION_CONFLICT`
- `DELETE /api/odontologo/bloqueos/{bloqueoId}` → `204` | `404 BLOQUEO_NOT_FOUND`
- Errores transversales: `400 VALIDATION_ERROR`, `401 SESSION_REQUIRED`, `403 CSRF_INVALID`, `409 AGENDA_BUSY`, `500 TECHNICAL_FAILURE`.
- El seguimiento [`correcciones-c06-configuracion-disponibilidad-api`](../correcciones-c06-configuracion-disponibilidad-api/design.md) endurece el rechazo de decimales en `duracionMinutos` y exige que cada turno confirmado quepa en una sola franja `ATENCION`, sin alterar rutas ni payloads válidos.

La aprobación de este diseño por Sebastián deja registrada la conformidad de C-12 con el contrato HTTP de C-06.

## Risks / Trade-offs

- [Compatibilidad de `vitest` / `@testing-library/react` con Vite 8.3.8 y React 19.3.1] → Verificar versiones compatibles al ejecutar `npm ci` / `npm install` en apply antes de escribir tests.
- [Conversión de fecha/hora local a `Instant` en zona `America/Argentina/Mendoza` sin librería de fechas] → Encapsular en un helper puro con pruebas unitarias dedicadas (incluyendo formato ISO-8601 con offset `-03:00` o `Z`).
- [Mezclar `localhost` y `127.0.0.1` rompe la cookie de sesión] → Documentar y usar exclusivamente `http://127.0.0.1:5173`.

## Migration Plan

No hay migraciones de base de datos ni cambios en backend. En desarrollo se ejecuta el frontend con Vite (`127.0.0.1:5173`) apuntando al backend local (`127.0.0.1:8080`). Para revertir el cambio basta revertir los archivos de `frontend/`.

## Open Questions

Ninguna abierta. El 2026-11-24 Sebastián confirmó:
1. **Zona horaria para bloqueos (D-05)**: **Opción A** aprobada (`VITE_APP_TIME_ZONE` con `America/Argentina/Mendoza` explícito en configuración de Vite, sin consultar la zona del navegador ni modificar backend).
2. **Dependencias de testing (D-06)**: aprobada la incorporación de `vitest`, `@testing-library/react`, `@testing-library/user-event` y `jsdom` como `devDependencies` en `frontend/package.json`.

## UI Mockups

Decision: no (project policy)

## Revisión y aprobación

- **Conformidad con contrato HTTP de C-06**: aprobada por Sebastián el 2026-11-24.
- **Aprobación de código CRITICAL (integración frontend de acceso/CSRF de C-03)**: **aprobada por Sebastián el 2026-11-24** («apruebo todos los cambios, sobre lo que preguntas del D-05 selecciono la recomendada, que seria la opcion A, no toques el backend, podes comenzar con apply de ciclo de openspec»).
