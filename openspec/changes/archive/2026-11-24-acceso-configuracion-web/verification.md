# Verificación de C-12 (`acceso-configuracion-web`)

## Resultado

Implementación completa de C-12 en `frontend/` bajo Strict TDD, cumpliendo `TS-01` (TypeScript estricto sin `any`), política visual `off` y los contratos HTTP de C-03 y C-06.

## Evidencia ejecutada

- **Entorno**: Windows, Node.js `v24.20.0`, npm `12.2.3` (ejecutado con `npm_config_engine_strict=false` por diferencia menor frente a `24.22.3` en `package.json`, sin modificar `.npmrc` ni `engines`).
- **`npx --yes npm@12.2.3 run typecheck` (`tsc -b`)**: pasó sin errores.
- **`npx --yes npm@12.2.3 test` (`vitest run`)**: **8/8 suites, 32/32 pruebas pasaron**:
  - `src/types/odontologia.test.ts` (7 pruebas): type guards y validación de duración entera `15..240` múltiplo de 15.
  - `src/api/apiClient.test.ts` (4 pruebas): `checkSession`, `login` con CSRF y renovación tras `204`, descarte de token sin reenvío automático ante `403 CSRF_INVALID`, mapeo de `409 AGENDA_BUSY`, `500 TECHNICAL_FAILURE` y fallo de red.
  - `src/components/LoginForm.test.tsx` (3 pruebas): envío `204`, errores `401 INVALID_CREDENTIALS` y `503 ACCESS_NOT_CONFIGURED`, validación de campos vacíos.
  - `src/App.test.tsx` (4 pruebas): verificación inicial única sin polling, cierre de sesión, redirección ante `401 SESSION_REQUIRED`, mensajes visibles de `409 AGENDA_BUSY` y `500 TECHNICAL_FAILURE`.
  - `src/components/MotivosSection.test.tsx` (5 pruebas): listado, alta `201` con entero JSON, edición `200`, ausencia de botón de borrado, manejo de `404 MOTIVO_NOT_FOUND` y validación cliente.
  - `src/components/HorarioSection.test.tsx` (3 pruebas): carga y `PUT` único con UUID en franjas nuevas, conservación del borrador ante `409 CONFIGURATION_CONFLICT`, validación `horaInicio < horaFin`.
  - `src/utils/timezone.test.ts` (3 pruebas): zona explícita `America/Argentina/Mendoza` y conversión exacta a ISO-8601.
  - `src/components/BloqueosSection.test.tsx` (3 pruebas): listado, alta `201` en zona del consultorio, rechazo `409 CONFIGURATION_CONFLICT` y resincronización ante `404 BLOQUEO_NOT_FOUND`.
- **`npx --yes npm@12.2.3 run build`**: `vite build` completado en 291 ms (`dist/assets/index-*.js` 241.16 kB, `dist/assets/index-*.css` 2.77 kB).
- **`openspec validate acceso-configuracion-web --strict --no-interactive`**: `Change 'acceso-configuracion-web' is valid`.
- **`git diff --check`**: sin errores de formato.

## Verificación en vivo (Windows local, 2026-11-24)

- Se levantaron temporalmente PostgreSQL 18 (`127.0.0.1:5432`, base `odontologia_test`, migraciones Flyway hasta `v3`), Spring Boot (`127.0.0.1:8080`) y Vite (`127.0.0.1:5173`) sin modificar archivos del repositorio.
- Comprobaciones HTTP reales vía proxy Vite:
  - `GET http://127.0.0.1:5173/` → `200`
  - `GET http://127.0.0.1:5173/api/odontologo/sesion` → `401 {"codigo":"SESSION_REQUIRED"}`
  - `GET http://127.0.0.1:5173/api/odontologo/csrf` → `200` con `token` y `headerName: "X-CSRF-TOKEN"`
- Sebastián visualizó la pantalla de ingreso en `http://127.0.0.1:5173/`. Ambos servidores se detuvieron limpiamente al finalizar.

## Límites y verificación pendiente en entorno local Linux/WSL2

- Por requerimiento de C-03 (`DentistCredentialFile.java`), el archivo de credencial del odontólogo exige permisos POSIX (`PosixFileAttributes`), no soportados en Windows NTFS sin WSL2. Por ello, el recorrido autenticado completo (ingreso con credencial real, motivos, horario y bloqueos contra PostgreSQL) y la prueba automatizada con Playwright quedan pendientes para entorno Linux/WSL2 o la verificación integrada de C-16.
- Comandos para ejecutar el recorrido autenticado en Linux/WSL2:
  1. `python3 scripts/with-local-db.py ./backend/gradlew -p backend bootRun` (con `APP_DENTIST_CREDENTIAL_FILE`, `APP_TIME_ZONE=America/Argentina/Mendoza`, `APP_CONSULTORIO_NOMBRE`, `APP_ODONTOLOGO_NOMBRE`).
  2. `cd frontend && npx --yes npm@12.2.3 run dev` y abrir `http://127.0.0.1:5173` en `390×844` y `1440×900` probando teclado y flujos FL-01 / FL-02.
