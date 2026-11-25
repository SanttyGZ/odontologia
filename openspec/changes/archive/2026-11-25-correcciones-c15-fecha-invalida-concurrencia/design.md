# Design

## Context

En `frontend/src/components/AgendaSection.tsx`, las consultas de agenda (`fetchAgenda` y `handleRefresh`) utilizan `latestAgendaRequestIdRef` (y `latestDetalleRequestIdRef` para el detalle) para descartar respuestas fuera de orden. Sin embargo, cuando el odontólogo borra o ingresa una fecha inválida en `#agenda-fecha` mientras una solicitud de agenda está en vuelo, `handleDateInputChange` limpia `agenda`, el detalle y setea `agendaError`, pero no avanza `latestAgendaRequestIdRef.current` ni desactiva `loadingAgenda`. Como resultado, la promesa en vuelo sigue siendo considerada vigente al resolverse y vuelve a poblar `agenda`, además de mantener el indicador de carga activo hasta que esa promesa termine.

## Goals / Non-Goals

- **Goals**:
  - Invalidar inmediatamente toda consulta de agenda en curso cuando `handleDateInputChange` recibe una fecha vacía o inválida (`!isValidIsoCalendarDate(value)`).
  - Asegurar que `loadingAgenda` se apague inmediatamente al invalidar la fecha para que los controles (`#agenda-fecha`, «Reintentar», «Hoy», «Actualizar») queden operativos sin espera.
  - Garantizar en `handleRefresh` un corte temprano cuando tanto la solicitud de agenda como la de detalle quedaron obsoletas (`!isLatestAgenda && !isLatestDetalle`).
  - Cubrir el escenario con una prueba de regresión en `AgendaSection.test.tsx`.
- **Non-Goals**:
  - Modificar endpoints del backend, contratos de API, seguridad, estilos o componentes fuera de `AgendaSection`.

## Decisions

### D-01 — Invalidación de consulta de agenda en curso y liberación de loadingAgenda ante fecha inválida

En `handleDateInputChange(value)` cuando `!isValidIsoCalendarDate(value)`, incrementar `latestAgendaRequestIdRef.current += 1` y llamar `setLoadingAgenda(false)` junto a `setAgenda(null)`, `clearDetalleState()` y `setAgendaError('Ingresá una fecha válida en formato AAAA-MM-DD.')`. En `handleRefresh()`, verificar `isLatestAgenda` e `isLatestDetalle` antes de aplicar cualquier cambio de estado post-`await` para que una recarga en vuelo también quede completamente invalidada si se borra la fecha mientras corre. Con `loadingAgenda === false`, los controles (`#agenda-fecha`, «Reintentar», «Hoy», «Actualizar») quedan inmediatamente listos para recuperar la vista.

## Risks / Trade-offs

- **Bajo**: incrementar `latestAgendaRequestIdRef.current` y apagar `loadingAgenda` en `handleDateInputChange` no cancela la petición HTTP subyacente con `AbortController`, pero garantiza que su respuesta se descarte de forma determinística sin efectos secundarios en el estado de React ni en el flujo de errores.

## Open Questions

Ninguna.

## UI Mockups

Decision: no (project policy)
