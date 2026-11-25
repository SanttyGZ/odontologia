# Tasks

## 1. Tipos, utilidades y cliente HTTP público (TDD)

- [x] 1.1 Agregar pruebas en `odontologia.test.ts`, `timezone.test.ts` y `apiClient.test.ts` para `SLOT_UNAVAILABLE`, type guards de disponibilidad/reserva pública, formateo de horarios en zona del consultorio y llamadas `getPublicMotivos`, `getPublicDisponibilidad` y `createPublicTurno` (sin usar `/api/odontologo/csrf`).
- [x] 1.2 Implementar los tipos, guardas, mensajes de error (`SLOT_UNAVAILABLE`), helper de formato en `timezone.ts` y métodos públicos en `apiClient.ts`.

## 2. Componente de reserva pública e integración en App (TDD)

- [x] 2.1 Crear `PublicBookingPage.test.tsx` y ampliar `App.test.tsx` cubriendo: acceso en `/reservar` sin `checkSession`, selección de motivo/fechas/horario conservando `inicio` exacto (incluidos segundos/microsegundos), validación de los 5 campos obligatorios, bloqueo de doble clic (`PB-08`), indisponibilidad vacía, fallo de consulta con reintento, `409 SLOT_UNAVAILABLE` recargando horarios, resultado incierto (`NETWORK_ERROR`), confirmación separando estado del turno y `notificacionEstado: "PENDIENTE"` (`PB-10`), y navegación por teclado.
- [x] 2.2 Implementar `PublicBookingPage.tsx`, integrar `/reservar` y enlaces de navegación en `App.tsx`, y añadir estilos responsivos en `styles.css`.

## 3. Verificación integral

- [x] 3.1 Ejecutar `vitest run`, `npm run typecheck`, `npm run build`, `openspec validate reserva-paciente-web --strict --no-interactive` y `git diff --check`.
