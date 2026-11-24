# Tasks

## 1. Reglas de cálculo de disponibilidad (TDD)

- [x] 1.1 Agregar pruebas unitarias en `AvailabilityRulesTest` para cálculo de horarios disponibles: pasos de 15 minutos anclados al inicio de cada franja `ATENCION`, exclusión de huecos que cruzan franjas contiguas, exclusión de pausas/bloqueos/turnos confirmados manteniendo intervalos contiguos, y bordes exactos `PB-01` (24 h inclusivo y 30 días calendario inclusivo).
- [x] 1.2 Implementar la función pura de cálculo de disponibilidad en `AvailabilityRules`.

## 2. Endpoints públicos y servicio (TDD)

- [x] 2.1 Agregar pruebas de controlador (`PublicAvailabilityControllerTest`) para `GET /api/public/motivos` y `GET /api/public/disponibilidad` (respuesta mínima sin datos internos, `400 VALIDATION_ERROR` por parámetros inválidos y `404 MOTIVO_NOT_FOUND`).
- [x] 2.2 Implementar `PublicAvailabilityDtos`, `PublicAvailabilityService`, `PublicAvailabilityController` y registrar el controlador en `AvailabilityErrorAdvice`.

## 3. Integración PostgreSQL y verificación

- [x] 3.1 Agregar `PublicAvailabilityIntegrationTest` contra PostgreSQL real verificando motivos públicos, ocupación con fixtures de `Turno` y `BloqueoAgenda`, zona horaria del consultorio, bordes `PB-01` con `Clock` fijo, ausencia de datos personales y acceso público sin sesión.
- [x] 3.2 Ejecutar pruebas focalizadas (`AvailabilityRulesTest`, `PublicAvailabilityControllerTest`, `PublicAvailabilityIntegrationTest`), `bootJar`, `openspec validate consulta-disponibilidad-api --strict --no-interactive` y `git diff --check`.
