# Tasks

## 1. Reapertura y concurrencia de la consulta

- [x] 1.1 Agregar regresiones para reabrir dos veces el mismo enlace y para una segunda apertura mientras la primera lectura está pendiente; verificar un segundo GET, limpieza del fragmento, datos distintos vigentes y descarte de la respuesta anterior con la misma credencial.
- [x] 1.2 Hacer que cada apertura reconocida dispare una lectura nueva por su generación de apertura; mantener la credencial solo en memoria y descartar respuestas obsoletas. Verificar primero que 1.1 falla antes del arreglo y luego que los dos casos y la cobertura C-14 existente pasan.

## 2. Compatibilidad de rutas y contratos

- [x] 2.1 Revisar el despacho existente de `App.tsx` y mantener pruebas del acceso interno, reserva pública y autogestión C-14; conservar las pruebas de transporte API público y privado. Verificar con `App.test.tsx` y `apiClient.test.ts`, sin cambiar contratos ni implementar C-15.
- [x] 2.2 Mantener sin cambios el estado y referencias de C-15 en `CHANGES.md`; registrar el seguimiento correctivo de C-14 sin presentarlo como cierre de C-15. Verificar el diff del roadmap.

## 3. Verificación y evidencia

- [x] 3.1 Ejecutar `npm test`, `npm run build`, validación estricta OpenSpec y `git diff --check`; guardar salidas redacted fuera del repositorio.
- [ ] 3.2 Ejecutar `backend\gradlew.bat -p backend test bootJar` según `README.md`; completar solo cuando ambas tareas terminen correctamente en un entorno habilitado.
- [ ] 3.3 Repetir en Chromium a 390×844 y 1440×900 con turno ficticio y credencial de prueba; registrar por separado el flujo local y la apertura desde correo recibido aún pendiente de C-11/C-16.

## Workflow follow-up

- Archivar este change cuando todas las tareas verificables estén completas y su evidencia quede registrada.
