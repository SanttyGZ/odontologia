# Tasks

No iniciar tareas hasta la aprobación explícita CRITICAL de Sebastián (y confirmación de D-05 y D-06 en `design.md`).

## 1. Herramientas de prueba (TDD)

- [x] 1.1 Incorporar en `frontend/package.json` las `devDependencies` aprobadas (`vitest`, `@testing-library/react`, `@testing-library/user-event`, `jsdom` compatibles con Vite 8.3.8 y React 19.3.1), el script `"test": "vitest run"` y configurar el entorno jsdom; verificar instalando con `npx --yes npm@12.2.3 install` y ejecutando una prueba mínima.

## 2. Cliente HTTP tipado, CSRF y guardas de tipo

- [x] 2.1 Escribir pruebas unitarias que fallen para las guardas de tipo (`unknown` → `CsrfResponse`, `SessionResponse`, `Motivo`, `Horario`, `Bloqueo`, `ErrorResponse` sin `any`) y el cliente `apiClient` (adjunta `headerName` CSRF en escrituras, descarta token en `403 CSRF_INVALID` o cambio de sesión, nunca reenvía escrituras automáticamente, propaga `401 SESSION_REQUIRED`, `409` y `500`/red); implementar ambos módulos hasta pasar `npm test` y `npm run typecheck`.

## 3. Flujo de sesión del odontólogo

- [x] 3.1 Escribir pruebas de componente que fallen para verificación inicial (`GET /api/odontologo/sesion` `200` vs `401 SESSION_REQUIRED` sin polling), formulario de ingreso (`POST /api/odontologo/sesion` `form-urlencoded` + CSRF, manejo de `204`, `400`, `401 INVALID_CREDENTIALS` y `503 ACCESS_NOT_CONFIGURED`/`ACCESS_UNAVAILABLE`), cierre (`POST /api/odontologo/sesion/cierre` con CSRF) y redirección al ingreso ante `401`; implementar los componentes y verificar con `npm test`.

## 4. Gestión de motivos de consulta

- [x] 4.1 Escribir pruebas de componente que fallen para listar, crear (`POST /api/odontologo/motivos`) y editar (`PUT /api/odontologo/motivos/{id}`) motivos sin botón de borrado, enviando `duracionMinutos` como entero JSON múltiplo de 15 entre 15 y 240 y manejando `400 VALIDATION_ERROR` y `404 MOTIVO_NOT_FOUND`; implementar la vista de motivos y verificar con `npm test`.

## 5. Gestión de horario semanal y pausas

- [x] 5.1 Escribir pruebas de componente que fallen para consultar (`GET /api/odontologo/horario`), editar franjas `ATENCION` y `PAUSA` (días 1 a 7, horas locales) y guardar todo el conjunto en un único `PUT /api/odontologo/horario`, conservando la edición en pantalla ante `400 VALIDATION_ERROR` o `409 CONFIGURATION_CONFLICT`; implementar la vista de horario y verificar con `npm test`.

## 6. Gestión de bloqueos de agenda

- [x] 6.1 Escribir pruebas unitarias para el helper de conversión de fecha/hora local en la zona configurada del consultorio (sin inferir zona del navegador) a `Instant` ISO-8601 y pruebas de componente que fallen para listar, crear (`POST /api/odontologo/bloqueos` con descripción opcional) y retirar (`DELETE /api/odontologo/bloqueos/{id}`) bloqueos, manejando `400`, `404 BLOQUEO_NOT_FOUND` y `409 CONFIGURATION_CONFLICT`; implementar helper y vista de bloqueos y verificar con `npm test`.

## 7. Estados operativos, estilos y accesibilidad

- [x] 7.1 Escribir pruebas que fallen para indicadores de carga, mensajes visibles de `409 AGENDA_BUSY` (reintento manual), `500 TECHNICAL_FAILURE` y error de red sin asumir guardado, etiquetas asociadas a controles y región `aria-live` para errores/confirmaciones; implementar estilos CSS responsive (390 px y 1440 px, sin desbordamiento horizontal) y verificar con `npm test`.

## 8. Verificación e integración final

- [x] 8.1 Ejecutar en `frontend/` `npx --yes npm@12.2.3 run typecheck`, `npx --yes npm@12.2.3 run build` y `npx --yes npm@12.2.3 test`, además de un recorrido en navegador real (390×844 y 1440×900, navegación por teclado y consola sin errores) contra el backend y PostgreSQL locales con `http://127.0.0.1:5173`. *(Verificado en Windows: `typecheck`, `test` 32/32, `build`, proxy `/api` en vivo y visualización de ingreso; el recorrido autenticado completo con credencial POSIX queda documentado en `verification.md` para Linux/WSL2 o C-16.)*
- [x] 8.2 Validar el change con `openspec validate acceso-configuracion-web --strict --no-interactive`, comprobar `git diff --check` y registrar la evidencia en `openspec/changes/acceso-configuracion-web/verification.md`.

## Workflow follow-up

- Archivar el change con `openspec archive acceso-configuracion-web` solo después de completar y verificar todas las tareas.
- Marcar C-12 en `CHANGES.md` únicamente tras archivar.
