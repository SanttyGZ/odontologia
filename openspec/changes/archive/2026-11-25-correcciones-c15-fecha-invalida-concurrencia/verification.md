# Verificación de `correcciones-c15-fecha-invalida-concurrencia`

## Cambio aplicado

- **`frontend/src/components/AgendaSection.tsx`**:
  - En `handleDateInputChange(value)`, cuando `!isValidIsoCalendarDate(value)`, se incrementa `latestAgendaRequestIdRef.current += 1` y se ejecuta `setLoadingAgenda(false)` junto con `setAgenda(null)`, `clearDetalleState()` y `setAgendaError('Ingresá una fecha válida en formato AAAA-MM-DD.')`, invalidando cualquier consulta de agenda o detalle en vuelo y dejando la pantalla lista para elegir otra fecha o reintentar.
  - En `handleRefresh()`, se evalúan `isLatestAgenda` e `isLatestDetalle` inmediatamente tras comprobar `isMountedRef.current`, retornando de inmediato si ninguno está vigente y condicionando el manejo de `401 SESSION_REQUIRED` a cada solicitud vigente.
- **`frontend/src/components/AgendaSection.test.tsx`**:
  - Agregadas 3 pruebas de regresión/triangulación con respuestas demoradas (`fetchAgenda` en vuelo al navegar con «Siguiente», `handleRefresh` en vuelo al accionar «Actualizar», y respuesta tardía `401 SESSION_REQUIRED` de `handleRefresh` invalidada por fecha borrada), comprobando lista vacía, detalle cerrado, ausencia de `Cargando agenda…`, persistencia del mensaje de fecha inválida y recuperación al elegir otra fecha o accionar «Reintentar».

## Evidencia ejecutada

- **`npm test` (`vitest run`)**: **10/10 archivos, 84/84 tests pasaron** (`AgendaSection.test.tsx`: 20/20).
- **`npm run build` (`tsc -b && vite build`)**: **0 errores**, bundle generado en `frontend/dist/`.
- **`openspec validate correcciones-c15-fecha-invalida-concurrencia --strict --no-interactive`**: válido sin errores ni advertencias.
- **`git diff --check`**: sin errores.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 & 1.2 | `frontend/src/components/AgendaSection.test.tsx` | Component (Vitest + RTL) | ✅ 81/81 | ✅ 3 failed (17/20 passed) | ✅ 84/84 passed | ✅ 3 cases (`fetchAgenda`, `handleRefresh` + Reintentar, stale `401`) | ✅ Clean (`tsc -b` + `vite build` OK) |
