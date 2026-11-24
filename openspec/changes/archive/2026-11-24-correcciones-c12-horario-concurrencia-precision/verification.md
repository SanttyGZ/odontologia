# Verificación de `correcciones-c12-horario-concurrencia-precision`

## Resultado

Correcciones aplicadas en `frontend/src/components/HorarioSection.tsx` y `frontend/src/components/HorarioSection.test.tsx` bajo Strict TDD, conservando intactos sesión, CSRF y el contrato de C-06.

## Evidencia ejecutada

- **Entorno**: Windows, Node.js `v24.20.0`, npm `12.2.3` (ejecutado con `npm_config_engine_strict=false` por diferencia menor frente a `24.22.3` en `package.json`, sin modificar `.npmrc` ni `engines`).
- **`npx --yes npm@12.2.3 ci`**: instalación limpia de `90` paquetes, `0` vulnerabilidades.
- **`npx --yes npm@12.2.3 run typecheck` (`tsc -b`)**: pasó sin errores, cero `any` en `HorarioSection.tsx` y `HorarioSection.test.tsx`.
- **`npx --yes npm@12.2.3 test` (`vitest run`)**: **8/8 suites, 42/42 pruebas pasaron** (`HorarioSection.test.tsx` 9/9):
  - Consultas fuera de orden bajo `React.StrictMode` con promesas diferidas: respuesta tardía `08:00:00` se descarta y no sobrescribe `09:00:00` ni el `PUT`.
  - Desmontaje con consulta pendiente `401 SESSION_REQUIRED`: no invoca `onSessionExpired`.
  - Horas con microsegundos (`09:00:30.123456`–`12:00:45.654321`): los inputs muestran `09:00:30` y `12:00:45`, la UI indica `.123456` y `.654321` junto a cada campo, y el `PUT` conserva los valores exactos sin editar o al agregar otra franja.
  - Comparación en el mismo segundo con microsegundos distintos (`09:00:30.123456` < `09:00:30.654321` válido; invertido rechazado en cliente sin llamar `PUT`).
- **`npx --yes npm@12.2.3 run build`**: `vite build` completado en 274 ms (`dist/assets/index-B9hci3cF.js` 242.09 kB, `dist/assets/index-ar9EKZ4W.css` 2.77 kB).
- **`openspec validate correcciones-c12-horario-concurrencia-precision --strict --no-interactive`**: `Change 'correcciones-c12-horario-concurrencia-precision' is valid`.
- **`git diff --check`**: sin errores.

## Comprobaciones pendientes en Linux / Chromium

- La verificación en Chromium con el input nativo `<input type="time" step="1">` y el recorrido autenticado completo con Spring Boot + PostgreSQL temporal quedan pendientes para ejecución en Linux/WSL2 porque en este entorno Windows no hay Chrome instalado para Playwright MCP ni permisos POSIX para `DentistCredentialFile.java` (C-03).
