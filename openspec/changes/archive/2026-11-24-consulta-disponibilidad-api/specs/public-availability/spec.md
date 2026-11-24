# Specification

## ADDED Requirements

### Requirement: PA-01 Consulta pública de motivos de consulta
La API pública SHALL exponer `GET /api/public/motivos` sin requerir autenticación ni cuenta de paciente, devolviendo únicamente `id`, `nombre` y `duracionMinutos` de los motivos configurados para la agenda del consultorio.

#### Scenario: Listado público de motivos
- **WHEN** un cliente consulta `GET /api/public/motivos`
- **THEN** la API responde `200 OK` con la lista ordenada de motivos (`id`, `nombre`, `duracionMinutos`) sin incluir `agendaId` ni datos internos.

### Requirement: PA-02 Consulta pública de disponibilidad por motivo y período
La API pública SHALL exponer `GET /api/public/disponibilidad` aceptando `motivoId` (UUID obligatorio) y parámetros opcionales `desde` y `hasta` (`YYYY-MM-DD`, inclusive en la zona horaria del consultorio). Cuando `desde` o `hasta` se omitan, SHALL utilizar los extremos efectivos de la ventana temporal aprobada (`PB-01`).

#### Scenario: Parámetros inválidos o motivo inexistente
- **WHEN** `motivoId` falta o no es UUID, `desde`/`hasta` tienen formato inválido o `desde > hasta`
- **THEN** la API responde `400 VALIDATION_ERROR`; si `motivoId` es UUID válido pero no existe en la agenda, responde `404 MOTIVO_NOT_FOUND`.

#### Scenario: Período sin disponibilidad o fuera de rango
- **WHEN** el período solicitado es válido pero no tiene horarios disponibles o queda fuera de la ventana de 24 horas a 30 días
- **THEN** la API responde `200 OK` con `horarios: []` sin inventar huecos ni devolver error.

### Requirement: PA-03 Cálculo de inicios y exclusión de indisponibilidad
Cada opción en `horarios` (`inicio`, `fin` en UTC ISO-8601) SHALL iniciar en pasos de 15 minutos anclados al comienzo de una franja `ATENCION` del día local, completar la duración vigente dentro de esa única franja (sin combinar contiguas), no intersectar `PAUSA`, `BloqueoAgenda` ni `Turno` `CONFIRMADO` (permitiendo intervalos contiguos), y cumplir `PB-01`: `inicio >= ahora + 24h` y `fechaLocal(inicio) <= fechaLocal(ahora) + 30 días`.

#### Scenario: Cálculo con franjas contiguas, pausas, bloqueos y turnos confirmados
- **WHEN** existen franjas `ATENCION` contiguas, pausas, bloqueos o turnos `CONFIRMADO` (incluidos turnos contiguos o parcialmente superpuestos)
- **THEN** la API excluye los inicios que cruzan el límite entre franjas contiguas o invaden pausas, bloqueos o turnos confirmados, y conserva los inicios contiguos libres que completan la duración del motivo.

#### Scenario: Bordes exactos de la ventana PB-01
- **WHEN** un inicio candidato coincide exactamente con `ahora + 24h` o cae en el día `fechaLocal(ahora) + 30 días`
- **THEN** se incluye si cumple las reglas de franja y ocupación; un inicio a `ahora + 24h - 1s` o en `fechaLocal(ahora) + 31 días` se excluye.

### Requirement: PA-04 Privacidad y ausencia de efectos secundarios
La consulta de disponibilidad SHALL ser de solo lectura y SHALL NOT reservar horarios ni exponer datos personales de pacientes ni detalles de citas ocupadas, bloqueos o pausas.

#### Scenario: Respuesta pública mínima
- **WHEN** la API responde `200 OK` en `GET /api/public/disponibilidad`
- **THEN** devuelve únicamente `motivoId`, `duracionMinutos`, `zonaHoraria` y la lista `horarios` con pares `inicio` y `fin`, sin modificar la base de datos.
