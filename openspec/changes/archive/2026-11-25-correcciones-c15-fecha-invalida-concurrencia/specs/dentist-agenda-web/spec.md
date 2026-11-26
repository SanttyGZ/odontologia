# dentist-agenda-web Specification

## Purpose

Precisar en `AW-04` la invalidación de consultas de agenda en curso al borrar o invalidar la fecha en `dentist-agenda-web`.

## MODIFIED Requirements

### Requirement: AW-04 Distinción de agenda vacía, carga, fallos de consulta y expiración de sesión

La interfaz SHALL distinguir carga (`aria-busy`), agenda vacía (`turnos: []`), errores y `401 SESSION_REQUIRED`. Al cambiar fecha o vista (`DIA`/`SEMANA`), SHALL conservar el detalle solo si pertenece al resultado, cerrarlo si queda fuera, vacío o falla, invalidar respuestas demoradas al cambiar período o cerrar detalle, y SHALL invalidar consultas en vuelo si se borra o invalida la fecha, apagando carga y conservando el mensaje.

#### Scenario: Período sin turnos muestra estado vacío y no error

- **WHEN** `GET /api/odontologo/agenda` responde `200 OK` con `turnos: []`
- **THEN** la interfaz muestra un mensaje informativo de agenda vacía para el período seleccionado sin alertas de error y cierra cualquier detalle abierto

#### Scenario: Cambio de período mantiene o cierra el detalle según pertenencia

- **WHEN** el odontólogo tiene un detalle abierto y cambia fecha o alterna entre `DIA` y `SEMANA`
- **THEN** el detalle solo permanece visible si el turno seleccionado pertenece al nuevo resultado de `GET /api/odontologo/agenda`, cerrándose en caso contrario

#### Scenario: Respuesta demorada de detalle tras cambiar de período o cerrar manualmente no reabre el panel

- **WHEN** una consulta `GET /api/odontologo/turnos/{turnoId}` en vuelo resuelve después de cambiar de período o de accionar «Cerrar detalle»
- **THEN** la respuesta demorada se descarta y no reabre el panel de detalle ni pisa el estado vigente

#### Scenario: Fallo de consulta informa imposibilidad con reintento sin presentar datos previos no recuperados como actuales

- **WHEN** el odontólogo cambia de período o reintenta una consulta de agenda y el backend responde con error (`400`, `403`, `500`, `NETWORK_ERROR`, `INVALID_RESPONSE` o fecha inválida)
- **THEN** la interfaz limpia los turnos y el detalle previos para no mostrarlos como actuales, informa el error de consulta y ofrece un botón «Reintentar»

#### Scenario: Respuestas de consultas fuera de orden se descartan

- **WHEN** una primera solicitud de agenda o detalle tarda más que una segunda solicitud posterior y resuelve después
- **THEN** la interfaz ignora la respuesta obsoleta de la primera solicitud y conserva exclusivamente los datos de la última solicitud emitida

#### Scenario: Respuesta 401 SESSION_REQUIRED redirige al formulario de ingreso

- **WHEN** `GET /api/odontologo/agenda` o `GET /api/odontologo/turnos/{turnoId}` responde `401 SESSION_REQUIRED`
- **THEN** la interfaz ejecuta el manejador de sesión expirada de `C-12`, limpia el estado autenticado y vuelve a mostrar el formulario de ingreso

#### Scenario: Borrar o invalidar la fecha durante una consulta en curso descarta la respuesta pendiente

- **WHEN** hay una consulta `GET /api/odontologo/agenda` en curso (con o sin detalle abierto), el odontólogo borra o invalida la fecha antes de que termine y luego se resuelve esa solicitud pendiente
- **THEN** la interfaz descarta la respuesta pendiente, mantiene la lista vacía, el detalle cerrado, `loadingAgenda` desactivado y el mensaje de fecha inválida visible, dejando habilitado elegir otra fecha o reintentar
