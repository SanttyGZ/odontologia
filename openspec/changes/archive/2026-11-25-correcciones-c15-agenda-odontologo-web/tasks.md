# Tasks

## 1. Correcciones en AgendaSection y CSS (Strict TDD)

- [x] 1.1 Escribir pruebas de regresión (RED) en `frontend/src/components/AgendaSection.test.tsx` para: cierre de detalle al cambiar fecha o alternar `Día`/`Semana` si el turno queda fuera del período (y conservación si sigue dentro), cierre cuando el período queda vacío o falla la consulta, respuesta demorada de `getTurnoDetalle` que NO reabre el detalle tras cambiar de período o cerrarlo manualmente, conservación de «Actualizar» para un turno vigente en el período, y visibilidad permanente de «Revisión» con `revision: 0` (recién reservado) y `revision > 0` (reprogramado con estado «Confirmado»).
- [x] 1.2 Implementar (GREEN + REFACTOR) las correcciones en `frontend/src/components/AgendaSection.tsx` y las reglas de corte de línea / contención horizontal en `frontend/src/styles.css` hasta pasar `npm test`, `npm run typecheck` y `npm run build`.

## 2. Verificación en Chromium (390×844 y 1440×900) y resolución de CHANGES.md

- [x] 2.1 Verificar en navegador Chromium real (`390×844` y `1440×900`) sobre Vite local: apellido de 100 caracteres sin espacios con detalle abierto y cerrado (`scrollWidth <= innerWidth`), cambio de período, agenda vacía, campo «Revisión» (`0` y `> 0`) y navegación por teclado, sin errores de consola.
- [x] 2.2 Integrar `upstream/main` en `sebastian/c15`, resolver el conflicto de `CHANGES.md` preservando el cierre de `C-09` y demás actualizaciones vigentes, documentar la evidencia real en `openspec/changes/correcciones-c15-agenda-odontologo-web/verification.md` y validar `openspec validate correcciones-c15-agenda-odontologo-web --strict --no-interactive` y `git diff --check`.
