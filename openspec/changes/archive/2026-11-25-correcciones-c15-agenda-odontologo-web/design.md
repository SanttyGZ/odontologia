# Design

## Context

Durante la revisión de `C-15` (`agenda-odontologo-web`) se identificaron tres comportamientos defectuosos en el frontend (`AgendaSection.tsx` y `styles.css`) y un conflicto pendiente en `CHANGES.md` al integrar `upstream/main`:
- El detalle de turno permanece visible o puede reabrirse por respuestas tardías cuando el usuario cambia de período o cierra manualmente.
- Textos largos sin espacios (nombres, apellidos de 100 caracteres o motivos largos) provocan desbordamiento horizontal en la lista de agenda.
- El campo «Revisión» se omite cuando `revision === 0`.
- `CHANGES.md` necesita integrar `upstream/main` preservando el cierre de `C-09` y registrando la verificación real de `C-15`.

## Goals / Non-Goals

- **Goals**:
  - Garantizar que el detalle de turno sea siempre consistente con el período vigente y que las respuestas tardías de `getTurnoDetalle` se descarten.
  - Mostrar siempre el campo «Revisión» en el detalle (incluido `0`).
  - Evitar todo desbordamiento horizontal (`scrollWidth <= innerWidth`) en `390×844` y `1440×900` con textos largos sin espacios.
  - Verificar en Chromium real y resolver el conflicto de `CHANGES.md` preservando `C-09`.
- **Non-Goals**:
  - Modificar backend, seguridad, esquema PostgreSQL, `C-12` o `C-13`.
  - Implementar edición de pacientes o acciones manuales pendientes (`PB-06`).

## Decisions

### D-01 — Sincronización del detalle con el período e invalidación de respuestas demoradas

Incrementar `latestDetalleRequestIdRef.current += 1` dentro de `clearDetalleState()` para invalidar toda promesa de `getTurnoDetalle` o `handleRefresh` en vuelo tras cierre manual, error o cambio de período; mantener `selectedTurnoIdRef` sincronizado para que `fetchAgenda()` verifique al recibir `200 OK` si el turno abierto sigue en `result.data.turnos` (si no está o la lista es `[]`, llamar `clearDetalleState()`); ante fecha inválida en `handleDateInputChange` o fallo de consulta, limpiar también el detalle previo; conservar el refresco paralelo de `handleRefresh()` cuando el turno sigue dentro del período.

### D-02 — Visibilidad permanente del campo «Revisión»

Eliminar la condición `turnoDetalle.revision > 0` en `AgendaSection.tsx` para renderizar siempre `<dt>Revisión</dt><dd>{turnoDetalle.revision}</dd>`, mostrando `0` en turnos recién reservados y el valor incrementado en turnos reprogramados bajo estado `Confirmado`.

### D-03 — Contención de ancho y corte de palabras largas en la lista de agenda

Aplicar `min-width: 0; max-width: 100%; overflow-wrap: anywhere; word-break: break-word;` en `.item-row`, `.agenda-turno-btn`, `.agenda-turno-header`, `.agenda-turno-meta` y sus `span`/`strong` dentro de `frontend/src/styles.css`, evitando que un apellido de 100 caracteres sin espacios fuerce scroll horizontal.

### D-04 — Verificación real en Chromium y resolución de conflicto en CHANGES.md

Ejecutar la verificación visual y de teclado a `390×844` y `1440×900` usando Microsoft Edge Chromium (`C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe`) contra Vite local (`127.0.0.1:5173`) con apellido de 100 caracteres sin espacios (detalle abierto y cerrado), cambio de período, agenda vacía y navegación por teclado; integrar `upstream/main` en `sebastian/c15` resolviendo el conflicto de `CHANGES.md` con preservación íntegra de `C-09`.

## Risks / Trade-offs

- El uso de `overflow-wrap: anywhere; word-break: break-word;` puede cortar palabras largas sin espacios en cualquier carácter, lo cual es deseable para evitar scroll horizontal en móviles (`390×844`), manteniendo legibilidad normal en textos con espacios.
- La sincronización con `selectedTurnoIdRef` y `latestDetalleRequestIdRef` requiere cubrir en pruebas unitarias las carreras entre respuestas tardías y cambios de período para evitar regresiones.

## Open Questions

Ninguna.

## UI Mockups

Decision: no (project policy)
