# Verificación de C-15 (`agenda-odontologo-web`)

## Resultado

Implementación completa de `C-15` en `frontend/` bajo **Strict TDD**, cumpliendo `TS-01` (TypeScript estricto sin `any`), política visual `off`, reglas aplicables de `vercel-react-best-practices` (`async-parallel`, `rendering-conditional-render`) y el contrato HTTP verificado de `C-10` (`GET /api/odontologo/agenda` y `GET /api/odontologo/turnos/{turnoId}`).

## Evidencia ejecutada

- **Entorno**: Windows, Node.js `v24.22.3`, npm `12.2.3`.
- **`npm test` (`vitest run`)**: **10/10 archivos, 74/74 pruebas pasaron**:
  - `src/utils/timezone.test.ts` (7 pruebas): `isValidIsoCalendarDate` (incluyendo años bisiestos y rechazo de fechas inválidas/offsets), `shiftIsoCalendarDate` (aritmética `Date.UTC` sin deriva), `formatIsoCalendarDate` y formateo de instantes/rangos en `America/Argentina/Mendoza`.
  - `src/types/odontologia.test.ts` (10 pruebas): códigos `TURNO_NOT_FOUND` y `ACCESS_DENIED`, guardas runtime sobre `unknown` para `AgendaResponse`, `AgendaTurnoResumen`, `PacienteBasico` y `TurnoDetalleResponse` (`canceladoEn: string | null`, `revision >= 0`, rechazo de `estado: 'REPROGRAMADO'`).
  - `src/api/apiClient.test.ts` (10 pruebas): `getAgenda` (con y sin query params `vista`/`fecha`, sin solicitar `/api/odontologo/csrf`), `getTurnoDetalle`, y mapeo de `400 VALIDATION_ERROR`, `401 SESSION_REQUIRED`, `403 ACCESS_DENIED`, `404 TURNO_NOT_FOUND`, `500 TECHNICAL_FAILURE`, `INVALID_RESPONSE` y `NETWORK_ERROR`.
  - `src/components/AgendaSection.test.tsx` (10 pruebas): carga inicial `DIA` sin `fecha`, cambio `DIA` ↔ `SEMANA`, navegación `Anterior` / `Hoy` / `Siguiente` y selector de fecha `YYYY-MM-DD`, distinción agenda vacía vs. error con `Reintentar` y limpieza de turnos previos (`FL-01`), descarte de respuestas fuera de orden, detalle con los 5 datos básicos del paciente, `revision` sin estado `REPROGRAMADO` (`RN-RP-02`), `canceladoEn`, `404 TURNO_NOT_FOUND`, botón `Actualizar` en paralelo (`Promise.all`) cerrando detalle obsoleto, `401 SESSION_REQUIRED` → `onSessionExpired`, ausencia de controles de edición/operaciones manuales y navegación por teclado.
  - `src/App.test.tsx` (11 pruebas): pestaña `Agenda` (`#tab-agenda`), deshabilitación durante `sectionBusy || loggingOut`, integración de `AgendaSection`, redirección al login ante `401 SESSION_REQUIRED` y compatibilidad completa con `C-12` y `C-13`.
  - Suites preexistentes (`LoginForm.test.tsx` 3, `MotivosSection.test.tsx` 5, `HorarioSection.test.tsx` 9, `BloqueosSection.test.tsx` 3, `PublicBookingPage.test.tsx` 6): todas pasando.
- **`npm run typecheck` (`tsc -b`)**: pasó sin errores (`TS-01` cumplido, cero `any`).
- **`npm run build` (`vite build`)**: compilación exitosa en `frontend/dist/`.
- **`openspec validate agenda-odontologo-web --strict --no-interactive`**: `Change 'agenda-odontologo-web' is valid`.
- **`git diff --check`**: sin errores de formato ni espacios finales.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 / 1.2 | `frontend/src/utils/timezone.test.ts` | Unit | ✅ 4/4 | ✅ Failed (3) | ✅ Passed (7/7) | ✅ 18+ cases (bisiestos, límites de mes/año, UTC-3) | ✅ Clean |
| 1.1 / 1.2 | `frontend/src/types/odontologia.test.ts` | Unit | ✅ 8/8 | ✅ Failed (2) | ✅ Passed (10/10) | ✅ Valid + invalid variants (`DIA`/`SEMANA`, `CONFIRMADO`/`CANCELADO`, `revision`) | ✅ Clean |
| 1.1 / 1.2 | `frontend/src/api/apiClient.test.ts` | Unit | ✅ 7/7 | ✅ Failed (3) | ✅ Passed (10/10) | ✅ 9+ cases (GET sin/con params, sin CSRF, 400/401/403/404/500/NETWORK) | ✅ Clean |
| 2.1 / 2.2 | `frontend/src/components/AgendaSection.test.tsx` | Component (Vitest + RTL) | ✅ 62/62 (9 files) | ✅ Failed (missing `AgendaSection`) | ✅ 9/9 passed | ✅ 10 cases (`NETWORK_ERROR`, `404` en `Actualizar`, teclado, solo lectura) | ✅ `buildAgendaQuery` helper |
| 2.1 / 2.2 | `frontend/src/App.test.tsx` | Integration (Vitest + RTL) | ✅ 9/9 passed | ✅ 3 failed (`tab-agenda` / `initialTab`) | ✅ 11/11 passed | ✅ `tabAgenda.disabled`, `401`, `initialTab` | ✅ Clean (`typecheck` + `build` OK) |

## Límites y verificación pendiente en entorno local Linux/WSL2 o `C-16`

- **Navegador real con Playwright MCP**: al intentar ejecutar la verificación visual automatizada en `390×844` y `1440×900` contra Vite (`http://127.0.0.1:5173/`), Playwright MCP reportó que Google Chrome no está instalado en `C:\Users\sebad\AppData\Local\Google\Chrome\Application\chrome.exe`. La navegación por teclado y la ausencia de desbordamiento horizontal en `390×844` y `1440×900` quedan respaldadas por CSS responsive (`.agenda-layout`, `.agenda-toolbar`, `.agenda-period-bar`) y pruebas RTL, quedando la inspección visual en navegador real pendiente para entorno con Chrome/Chromium o `C-16`.
- **Recorrido autenticado completo contra backend + PostgreSQL 17.12**: igual que en `C-10` y `C-12`, `DentistCredentialFile.java` exige permisos POSIX (`PosixFileAttributes`) no soportados en Windows NTFS sin WSL2, por lo que el recorrido autenticado completo con datos reales de prueba y actualización tras `C-09` queda pendiente para Linux/WSL2 o `C-16`.
