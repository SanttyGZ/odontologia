# Spec Delta

## MODIFIED Requirements

### Requirement: CW-02 Manejo del ciclo de token CSRF y cierre de sesión
La interfaz SHALL obtener el token y el nombre del encabezado desde `GET /api/odontologo/csrf`, conservarlos en memoria y enviarlos en toda escritura interna y en el cierre. Ante `403 CSRF_INVALID` en una escritura interna o cierre, SHALL descartar el token, consultar `GET /api/odontologo/sesion` sin reenviar la escritura, volver al ingreso ante `401` o permitir reintento manual con token nuevo ante `200`.

#### Scenario: Cierre de sesión explícito
- **WHEN** el odontólogo acciona el cierre y `POST /api/odontologo/sesion/cierre` responde `204`
- **THEN** la interfaz descarta el token CSRF y los datos internos en memoria, vuelve al formulario de ingreso y no permite operar sin reingresar.

#### Scenario: Rechazo por token CSRF inválido o vencido
- **WHEN** una escritura o cierre responde `403 CSRF_INVALID`
- **THEN** la interfaz descarta el token actual, consulta `GET /api/odontologo/sesion` sin reenviar la escritura automáticamente y actúa según el estado de sesión vigente.

#### Scenario: Escritura rechazada con 403 CSRF_INVALID y sesión expirada
- **WHEN** una escritura interna recibe `403 CSRF_INVALID` y al consultar `GET /api/odontologo/sesion` el backend responde `401 SESSION_REQUIRED`
- **THEN** la interfaz descarta el token CSRF, no reenvía la escritura, limpia la vista interna y vuelve al formulario de ingreso con un mensaje claro de expiración.

#### Scenario: Escritura rechazada con 403 CSRF_INVALID y sesión activa
- **WHEN** una escritura interna recibe `403 CSRF_INVALID` y `GET /api/odontologo/sesion` responde `200` con `autenticado: true`
- **THEN** la interfaz descarta el token previo, no reenvía la escritura automáticamente, muestra el error al usuario y permite reintentar manualmente obteniendo un token nuevo.

#### Scenario: Pérdida o expiración de sesión durante el uso
- **WHEN** cualquier consulta o escritura interna responde `401 SESSION_REQUIRED`
- **THEN** la interfaz limpia el estado de sesión autenticada, vuelve al ingreso e informa que la sesión expiró sin dar por guardada la operación.

### Requirement: CW-04 Edición web del horario semanal y pausas
La interfaz autenticada SHALL consultar `GET /api/odontologo/horario`. Si la consulta falla, SHALL mostrar el error con reintento y bloquear edición y guardado hasta obtener una respuesta válida. Con respuesta válida, SHALL permitir editar franjas (`ATENCION`/`PAUSA`, días `1` a `7`) conservando la precisión original de horas no modificadas y guardar en un único `PUT /api/odontologo/horario`.

#### Scenario: Fallo en la consulta inicial de horario
- **WHEN** `GET /api/odontologo/horario` falla con `500 TECHNICAL_FAILURE` o error de red
- **THEN** la interfaz muestra el error y un botón de reintento, y bloquea la edición y el guardado de forma que jamás habilite un `PUT` con una lista vacía.

#### Scenario: Consulta exitosa sin franjas configuradas
- **WHEN** `GET /api/odontologo/horario` responde `200` con `franjas: []`
- **THEN** la interfaz habilita el formulario para agregar franjas y crear un horario.

#### Scenario: Preservación de precisión en horas no editadas
- **WHEN** `GET /api/odontologo/horario` devuelve franjas con segundos como `09:00:30`–`12:00:45` y el usuario guarda sin modificarlas o editando otra franja
- **THEN** el cuerpo de `PUT /api/odontologo/horario` conserva exactamente `09:00:30` y `12:00:45` sin recortarlos.

#### Scenario: Reemplazo válido del horario semanal
- **WHEN** el odontólogo guarda franjas válidas y `PUT /api/odontologo/horario` responde `200`
- **THEN** la interfaz actualiza el horario mostrado con las franjas devueltas por el backend y confirma el guardado.

#### Scenario: Conflicto con turnos confirmados o error de validación
- **WHEN** `PUT /api/odontologo/horario` responde `409 CONFIGURATION_CONFLICT` o `400 VALIDATION_ERROR`
- **THEN** la interfaz informa el conflicto o error de validación y conserva en pantalla la edición pendiente para que el odontólogo la corrija o descarte sin perder su trabajo.
