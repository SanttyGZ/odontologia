# Tasks

No iniciar tareas hasta la aprobación explícita CRITICAL de Sebastián sobre `design.md`.

## 1. Horario semanal ante fallo de lectura

- [x] 1.1 Escribir pruebas que fallen en `frontend/src/components/HorarioSection.test.tsx` comprobando que si `GET /api/odontologo/horario` responde `500 TECHNICAL_FAILURE` se muestra el error y botón Reintentar, se bloquean edición y guardado (jamás habilita `PUT []`), y que si `GET` responde `200` con `[]` sí habilita crear un horario; implementar el estado `'loading' | 'loaded' | 'error'` en `frontend/src/components/HorarioSection.tsx` y pasar `npm test`.

## 2. Precisión del horario

- [x] 2.1 Escribir pruebas que fallen en `frontend/src/components/HorarioSection.test.tsx` verificando que ante `09:00:30`–`12:00:45`, guardar sin editar conserva ambos valores en el `PUT`, y editar otra franja tampoco los recorta; ajustar `frontend/src/components/HorarioSection.tsx` conservando los valores originales sin truncar y pasar `npm test`.

## 3. Pérdida de sesión tras `403 CSRF_INVALID`

- [x] 3.1 Escribir pruebas que fallen en `frontend/src/api/apiClient.test.ts` y `frontend/src/App.test.tsx` para ambos casos ante `403 CSRF_INVALID` en escritura interna: (a) `GET /api/odontologo/sesion` responde `401 SESSION_REQUIRED` → descarta token, no reenvía escritura, limpia vista interna y vuelve al ingreso con mensaje claro; (b) `GET /api/odontologo/sesion` responde `200` → descarta token, no reenvía escritura, muestra error y permite reintento manual con token nuevo; verificar que cada acción envía la escritura una sola vez; implementar en `frontend/src/api/apiClient.ts` y `frontend/src/App.tsx` y pasar `npm test`.

## 4. Verificación e integración

- [x] 4.1 Ejecutar instalación limpia (`npm ci`), `typecheck`, `vitest`, `build`, `openspec validate correcciones-c12-acceso-configuracion-web --strict --no-interactive`, `git diff --check` y registrar resultados reales y cualquier comprobación pendiente en `openspec/changes/correcciones-c12-acceso-configuracion-web/verification.md`. *(Verificado en Windows: `npm ci`, `typecheck`, 38/38 tests Vitest, `build`, `openspec validate` y `git diff --check` OK; verificación en navegador autenticado pendiente en Linux/WSL2 — ver `verification.md`.)*

## Workflow follow-up

- Archivar el change con `openspec archive correcciones-c12-acceso-configuracion-web` tras completar y verificar todas las tareas.
- Actualizar la nota de C-12 en `CHANGES.md` solo después del archivo.
