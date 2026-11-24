# Verificación de `correcciones-c12-acceso-configuracion-web`

## Resultado

Correcciones de revisión de PR aplicadas sobre `frontend/` en la rama `sebastian/c12` bajo Strict TDD, sin modificar `backend/`.

## Evidencia ejecutada

- **Entorno**: Windows, Node.js `v24.20.0`, npm `12.2.3` (ejecutado con `npm_config_engine_strict=false` por diferencia menor frente a `24.22.3` en `package.json`, sin modificar `.npmrc` ni `engines`).
- **`npx --yes npm@12.2.3 ci`**: instalación limpia de `90` paquetes sin vulnerabilidades.
- **`npx --yes npm@12.2.3 run typecheck` (`tsc -b`)**: pasó sin errores, cero `any` en `frontend/src/`.
- **`npx --yes npm@12.2.3 test` (`vitest run`)**: **8/8 suites, 38/38 pruebas pasaron**:
  - `src/components/HorarioSection.test.tsx` (5 pruebas): fallo `GET 500` bloquea edición/guardado y habilita reintento sin permitir `PUT []`; `GET 200` con `[]` habilita crear horario; conservación de `09:00:30`–`12:00:45` en `PUT` sin editar y al editar otra franja.
  - `src/api/apiClient.test.ts` (5 pruebas): ante `403 CSRF_INVALID` descarta token, consulta `GET /api/odontologo/sesion` sin reenviar escritura; devuelve `401 SESSION_REQUIRED` si expiró o `403 CSRF_INVALID` si sigue activa para reintento manual con token nuevo.
  - `src/App.test.tsx` (7 pruebas): flujo completo en React para `403 CSRF_INVALID` + `GET /sesion -> 401` (limpia vista y vuelve al ingreso enviando la escritura una sola vez), `403 CSRF_INVALID` + `GET /sesion -> 200` (mantiene vista con error y permite reintento manual con token nuevo), y error visible al fallar `logout` con sesión activa.
  - Resto de suites (`odontologia.test.ts`, `LoginForm.test.tsx`, `MotivosSection.test.tsx`, `BloqueosSection.test.tsx`, `timezone.test.ts`): todas verdes.
- **`npx --yes npm@12.2.3 run build`**: `vite build` completado en 296 ms (`dist/assets/index-DuXa44Xh.js` 241.62 kB, `dist/assets/index-ar9EKZ4W.css` 2.77 kB).
- **`openspec validate correcciones-c12-acceso-configuracion-web --strict --no-interactive`**: `Change 'correcciones-c12-acceso-configuracion-web' is valid`.
- **`git diff --check`**: sin errores de formato.

## Comprobaciones pendientes en entorno Linux/WSL2

- La verificación en navegador real autenticado con Spring Boot + PostgreSQL de prueba (guardado normal de horario con segundos, fallo de lectura `GET /horario` y cierre de sesión desde otra pestaña provocando `403 CSRF_INVALID` + `401 SESSION_REQUIRED`) queda pendiente de ejecución en Linux/WSL2 o C-16 porque `DentistCredentialFile.java` (C-03) exige permisos POSIX no soportados en Windows NTFS sin WSL2.
