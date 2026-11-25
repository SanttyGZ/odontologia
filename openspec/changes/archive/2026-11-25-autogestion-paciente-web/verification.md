# Verificación de C-14 — 2026-11-25

## Resultado

La implementación principal quedó aplicada dentro del contrato aprobado. El endpoint y el flujo web pasan las comprobaciones técnicas registradas abajo. PB-06 quedó resuelta para C-14 con contacto directo fuera de la app y el rechazo `409` conservado. OpenSpec archivó el change el 2026-11-25 como `2026-11-25-autogestion-paciente-web` y actualizó las especificaciones principales. La verificación de apertura de enlace desde email se dejó para C-16 cuando C-11 habilite ese flujo.

## API protegida de horarios

- `GET /api/gestion/turno/disponibilidad` deriva el motivo del turno autorizado, reutiliza C-07, no toma `motivoId` del cliente y responde solo `zonaHoraria`/`horarios` con `Cache-Control: no-store`.
- Las pruebas de controlador y servicio pasaron (Active Stack receipt `083727bbb9006a7dd4a6626602ab9787`). La integración contra PostgreSQL 17.12 pasó 1/1 (receipt `b6afbb68af03b191a3faaa13d1eb0967`); comprueba el motivo autorizado, ignora un `motivoId` hostil y no cambia turno ni avisos.
- **Límite de esa integración:** usa un doble de `CredencialesGestion`; no ejercita la verificación criptográfica real de C-04.
- `bootJar` pasó en Windows/Java 21 (receipt `2210f9dd4025ddc623dc9432ca38f63e`).

## Interfaz y transporte

- La ruta `/gestionar-turno` quita el fragmento con `history.replaceState`, mantiene el token solo en estado de React y lo envía en `X-Management-Token`. Una recarga sin el fragmento no recupera el secreto de `localStorage` ni `sessionStorage`.
- Las consultas usan `cache: no-store`; cancelación y reprogramación envían `If-Match`. La reprogramación solo envía el inicio exacto de un horario devuelto por la API protegida, sin `motivoId` del cliente.
- La cancelación requiere una segunda confirmación visible. El éxito comunica `CANCELADO`/`PENDIENTE` sin afirmar que el correo se recibió. Ante respuesta de escritura incierta, la página consulta el estado antes de comunicarlo como exitoso.
- Los errores `LINK_NOT_AVAILABLE`, `MANAGEMENT_WINDOW_CLOSED`, `SLOT_UNAVAILABLE`, `SAME_INTERVAL` y `TURN_VERSION_MISMATCH` se muestran sin distinguir causas privadas ni ofrecer excepciones manuales.
- Para `MANAGEMENT_WINDOW_CLOSED`, la interfaz indica contacto directo con el odontólogo por el canal habitual externo del consultorio, sin teléfono/email configurado o expuesto. Las pruebas verifican que tanto cancelación como reprogramación conservan el estado `Confirmado` y no ofrecen alta manual ni excepciones.
- Pruebas frontend finales: `npm test -- --maxWorkers=1`, Node 24.20.0/npm 11.19.0; 10 archivos, 69/69 pruebas aprobadas. Incluyen ambos flujos fuera de plazo: cancelación y reprogramación conservan el turno y muestran el contacto directo, sin datos de contacto ni excepciones.
- Build y TypeScript finales: `npm run build`; `tsc -b` y Vite build aprobados con el mismo toolchain.

## Revisión visual y teclado

La política visual del proyecto es `off`, así que no se generaron mockups. Se abrió la página con un mock API local de solo prueba, sin usar turnos ni credenciales reales, y se revisaron capturas de 390×844 y 1440×900. La medición móvil dio `innerWidth=390` y `documentElement.scrollWidth=390`, sin desborde horizontal. La prueba de teclado recorre con Tab las acciones y abre la confirmación con Enter.

- Captura móvil: [verification-mobile.png](verification-mobile.png).
- Captura escritorio: [verification-desktop.png](verification-desktop.png).

## Gates cerrados y límites

- PB-06 queda resuelta para C-14: ante `409 MANAGEMENT_WINDOW_CLOSED`, impedir la autogestión e indicar contacto directo con el odontólogo por el canal habitual externo del consultorio. El odontólogo es el responsable operativo; la app no publica teléfono/email ni ofrece alta manual o excepciones. Las tareas 1.1 y 4.4 están completas.
- C-11 todavía no habilita emails locales; no se pudo abrir un enlace real desde un mensaje. La tarea 5.1 se cierra registrando esa evidencia pendiente para el cierre conjunto C-16; la apertura del enlace y la comprobación de que su previsualización no modifica el turno aún no se ejecutaron.
- La validación local de membresía/asignación de equipo no pudo ejecutarse porque el entorno no tiene Python 3.8 o superior; queda como limitación administrativa no funcional.
- Antes del archivo, `openspec validate autogestion-paciente-web --strict` pasó: `Change 'autogestion-paciente-web' is valid`. Después del archivo, `openspec validate appointment-self-management --type spec --strict` y `openspec validate patient-self-management-web --type spec --strict` pasaron.
- `openspec validate --archived --strict` pasó en los 19 changes archivados, incluido C-14. `git diff --check` pasó sin errores.
- OpenSpec reportó sus cuatro artefactos de planificación y 13/13 tareas completas antes del archivo. Después del archivo no queda un change activo.
- `openspec validate --all --strict` reportó un fallo ajeno a C-14 en `spec/dentist-agenda-query`: falta un propósito no-placeholder y una requirement supera 500 caracteres. Las dos especificaciones de C-14 son válidas; el spec ajeno no se modificó para conservar el alcance.

## Regresiones del flujo web verificadas después del archivo — 2026-11-25

- **Prueba roja antes del arreglo:** `npm test -- --maxWorkers=1 src/components/GestionTurnoPage.test.tsx` ejecutó 15 pruebas y fallaron 4: segundo enlace en la misma pestaña, turno confirmado con `acciones: []` y las dos variantes de respuesta tardía del período anterior (éxito y error).
- Se agregaron pruebas con dos credenciales ficticias en la misma pestaña; un turno confirmado sin acciones; y consultas demoradas para el 23 y el 24 de enero. Comprueban limpieza de datos, horarios y confirmaciones anteriores, descarte de respuestas de turno obsoletas, indicador de carga vigente, ausencia de errores/horarios obsoletos y confirmación solo para los horarios actuales.
- **Pruebas verdes:** `npm test` pasó con 10 archivos y 73/73 pruebas. **Build:** `npm run build` pasó con `tsc -b` y Vite, usando Node 24.20.0/npm 11.19.0.
- Los cambios se limitaron a `GestionTurnoPage.tsx`, su prueba, el texto de error compartido del flujo web y esta evidencia. No se modificaron rutas, parámetros, cuerpos ni contratos de la API.
- Sigue pendiente abrir el enlace desde un correo recibido cuando C-11 habilite ese flujo; esta comprobación permanece asignada a C-16.
