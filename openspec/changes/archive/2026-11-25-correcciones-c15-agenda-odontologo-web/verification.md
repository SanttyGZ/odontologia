# Verificación de `correcciones-c15-agenda-odontologo-web`

## Correcciones aplicadas

1. **Cierre de detalle obsoleto e invalidación de respuestas demoradas (`AgendaSection.tsx`)**:
   - `clearDetalleState()` ahora incrementa `latestDetalleRequestIdRef.current += 1` y limpia `selectedTurnoIdRef.current = null`, impidiendo que una consulta `getTurnoDetalle` o `handleRefresh` demorada vuelva a abrir el panel tras cambiar de período o accionar «Cerrar detalle».
   - `fetchAgenda()` verifica al recibir `200 OK` si `selectedTurnoIdRef.current` sigue incluido en `result.data.turnos`; si queda fuera del nuevo período o el período viene vacío (`[]`), invoca `clearDetalleState()`.
   - Ante error de consulta de agenda o fecha inválida en `#agenda-fecha`, se limpian los turnos y el detalle previos.
   - Se conserva el refresco paralelo de «Actualizar» (`handleRefresh`) cuando el turno sigue dentro del período.
2. **Desbordamiento horizontal en lista y detalle (`styles.css`)**:
   - Se agregó `min-width: 0; max-width: 100%;` y `overflow-wrap: anywhere; word-break: break-word;` en `.item-list`, `.item-row`, `.agenda-turno-btn`, `.agenda-turno-header`, `.agenda-turno-meta` y `.agenda-detail-list dd`.
3. **Visibilidad permanente del campo «Revisión» (`AgendaSection.tsx`)**:
   - Se eliminó la condición `turnoDetalle.revision > 0`, mostrando siempre `Revisión: {turnoDetalle.revision}` (incluido `0` en turnos recién reservados y `> 0` en turnos reprogramados bajo estado `Confirmado`).
4. **Resolución de `CHANGES.md` (`upstream/main`)**:
   - Se integró `upstream/main` en `sebastian/c15`, preservando intacto el cierre de `C-09` y actualizando el estado y evidencia real de `C-15`.

## Evidencia ejecutada

- **`npm test` (`vitest run`)**: **10/10 archivos, 81/81 pruebas pasaron** (`AgendaSection.test.tsx`: 17/17 pruebas, incluyendo 7 nuevas regresiones para detalle obsoleto al cambiar de período/vista/agenda vacía/fallo, respuestas demoradas tras cierre manual o cambio de período, conservación en `Actualizar` y `Revisión: 0`).
- **`npm run typecheck` (`tsc -b`)**: **0 errores** (`TS-01` estricto sin `any`).
- **`npm run build` (`vite build`)**: compilación exitosa en `frontend/dist/`.
- **Chromium real (`msedge.exe` headless contra Vite `http://127.0.0.1:5173/`, `**/api/odontologo/**` interceptado)**:
  - `1440×900` con apellido de 100 caracteres sin espacios y motivo largo sin espacios:
    - Detalle cerrado: `scrollWidth: 1440 <= innerWidth: 1440`, apellido partido en 4 líneas (`clientHeight: 96`, `lineHeight: 24`, `rects: 4`).
    - Detalle abierto: `scrollWidth: 1440 <= innerWidth: 1440`, `Revisión: 0` + `Confirmado` en Turno 1, `Revisión: 2` + `Confirmado` sin `Reprogramado` en Turno 2.
  - `390×844` con detalle abierto y cerrado: `scrollWidth: 390 <= innerWidth: 390` en ambos estados.
  - Cambio de período: mantiene el detalle al cambiar a `SEMANA` cuando el turno pertenece a la semana y cierra el detalle automáticamente al pasar a un período con agenda vacía (`No hay turnos registrados para este período.`).
  - Navegación por teclado (`Tab` + `Enter`) para pestaña `Agenda`, selección de turno y cierre de detalle; `consoleErrors: []`, `pageErrors: []`.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | `frontend/src/components/AgendaSection.test.tsx` | Component (Vitest + RTL) | ✅ 74/74 | ✅ 6 failing | ✅ 81/81 | ✅ 7 regression cases | ✅ Clean |
| 1.2 | `frontend/src/components/AgendaSection.tsx`, `styles.css` | Component + Styles | ✅ 74/74 | ✅ Confirmed by 1.1 | ✅ 81/81 + `tsc -b` + `vite build` | ✅ 17 `AgendaSection` tests | ✅ Clean |

## Qué queda pendiente de verificar

- El recorrido autenticado completo contra el backend Spring Boot + PostgreSQL 17.12 con archivo de credencial POSIX (`DentistCredentialFile.java`) en entorno Linux/WSL2 o `C-16`.
