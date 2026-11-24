# Design

## Context

Ver [proposal.md](proposal.md) y [la spec de dentist-configuration-web](specs/dentist-configuration-web/spec.md). Ambos ajustes se limitan a `frontend/src/components/HorarioSection.tsx` y `frontend/src/components/HorarioSection.test.tsx`, conservando el contrato de C-06 y el comportamiento de sesión y CSRF.

## Goals / Non-Goals

**Goals:**
- Garantizar que solo la consulta `GET /api/odontologo/horario` vigente pueda actualizar `franjas`, `loadState`, `errorMessage` o disparar `onSessionExpired`.
- Evitar que Chromium vacíe `<input type="time">` cuando el backend devuelve microsegundos (`09:00:30.123456`), mostrando la fracción junto al campo, conservando el valor exacto en el `PUT` si no se edita y validando `inicio < fin` con precisión de microsegundos.

**Non-Goals:**
- Modificar `apiClient.ts`, `App.tsx`, `MotivosSection.tsx`, `BloqueosSection.tsx` ni el backend.

## Decisions

### D-01 — Control de vigencia de consultas en `HorarioSection.tsx`

Usar `latestRequestIdRef = useRef(0)` para identificar cada llamada de `loadHorario`:
- Al iniciar `loadHorario()`, incrementar y capturar `const requestId = ++latestRequestIdRef.current`.
- En el cleanup de `useEffect`, incrementar `latestRequestIdRef.current += 1` para invalidar consultas en vuelo tras desmontaje (incluido el doble montaje de `React.StrictMode`).
- Tras `await apiClient.getHorario()`, si `requestId !== latestRequestIdRef.current`, retornar de inmediato sin actualizar `franjas`, `loadState`, `errorMessage` ni invocar `onSessionExpired`.

### D-02 — Separación de valor visible y valor exacto para microsegundos

- El estado `franjas` conserva el string exacto recibido de la API (`horaInicio`, `horaFin`).
- Helper puro `toTimeInputValue(raw: string): string`: recorta la parte fraccionaria (`raw.split('.')[0]`) para que `<input type="time" step="1">` reciba `HH:mm:ss` (válido en Chromium y compatible con `required`).
- Helper puro `extractSubsecondFraction(raw: string): string | null`: si `raw` contiene `.` seguido de dígitos (ej. `.123456`), devuelve esa fracción para renderizarla como indicación visible junto al campo.
- Al disparar `onChange` en un campo de hora, solo ese campo (`horaInicio` o `horaFin`) pasa a tener el nuevo valor editado por el usuario; los demás campos y franjas conservan sus valores exactos en el `PUT`.
- Helper puro `normalizeTimeForCompare(raw: string): string`: normaliza `HH:mm` → `HH:mm:00.000000`, `HH:mm:ss` → `HH:mm:ss.000000` y `HH:mm:ss.f+` → 6 dígitos rellenados con `'0'` a la derecha, permitiendo comparar lexicográficamente dos horas incluso dentro del mismo segundo (`09:00:30.100000` < `09:00:30.200000`).

## Risks / Trade-offs

- [Si el usuario edita un campo que tenía microsegundos en Chromium, el `onChange` entrega `HH:mm:ss` sin la fracción previa] → Esperado: al editar explícitamente ese campo, el usuario define la nueva hora; mientras no lo edite, el `PUT` conserva el valor original con microsegundos.

## Migration Plan

Sin migraciones ni cambios de configuración. Solo se actualizan `HorarioSection.tsx` y `HorarioSection.test.tsx`.

## UI Mockups

Decision: no (project policy)
