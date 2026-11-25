# Tasks

## 1. Regresión e invalidación de consulta pendiente ante fecha inválida (Strict TDD)

- [x] 1.1 Agregar en `frontend/src/components/AgendaSection.test.tsx` la prueba de regresión (RED) con respuesta demorada: abrir una consulta en curso (y detalle previo), borrar la fecha antes de que resuelva, resolver la solicitud pendiente y verificar que la lista siga vacía, el detalle quede cerrado, se mantenga el mensaje de fecha inválida, `Cargando agenda…` no quede activo y la pantalla permita seleccionar otra fecha o reintentar.
- [x] 1.2 Implementar (GREEN + REFACTOR) en `frontend/src/components/AgendaSection.tsx` la invalidación de `latestAgendaRequestIdRef` y limpieza de `loadingAgenda` al recibir una fecha inválida (y guarda de vigencia en `handleRefresh`), ejecutando `npm test` y `npm run build` en `frontend/`.

## 2. Verificación y validación del change

- [x] 2.1 Registrar evidencia en `openspec/changes/correcciones-c15-fecha-invalida-concurrencia/verification.md` y validar con `openspec validate correcciones-c15-fecha-invalida-concurrencia --strict --no-interactive` y `git diff --check`.
