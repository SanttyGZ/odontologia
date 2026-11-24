# dentist-configuration-web Specification

## Purpose
Proveer la interfaz web interna del odontólogo para autenticar su sesión y administrar motivos de consulta, horario semanal con pausas y bloqueos de agenda sobre la API local existente.

## Requirements

### Requirement: CW-01 Verificación inicial de sesión e ingreso del odontólogo
Al iniciar el flujo interno, la interfaz SHALL consultar `GET /api/odontologo/sesion` sin enviar credenciales predeterminadas ni consultas periódicas de renovación. Si no hay sesión activa, SHALL mostrar el formulario de ingreso con usuario y contraseña y enviar `POST /api/odontologo/sesion` con cuerpo `application/x-www-form-urlencoded` y encabezado CSRF válido.

#### Scenario: Sesión activa al cargar la interfaz
- **WHEN** `GET /api/odontologo/sesion` responde `200` con `autenticado: true`
- **THEN** la interfaz habilita las vistas internas de configuración y consulta los datos iniciales.

#### Scenario: Sin sesión activa al cargar la interfaz
- **WHEN** `GET /api/odontologo/sesion` responde `401 SESSION_REQUIRED`
- **THEN** la interfaz muestra únicamente el formulario de ingreso y no renderiza datos ni acciones de configuración.

#### Scenario: Ingreso exitoso
- **WHEN** el odontólogo envía credenciales válidas y `POST /api/odontologo/sesion` responde `204`
- **THEN** la interfaz descarta el token CSRF previo, obtiene uno nuevo para la sesión autenticada y accede a la configuración.

#### Scenario: Credenciales inválidas o solicitud malformada
- **WHEN** `POST /api/odontologo/sesion` responde `401 INVALID_CREDENTIALS` o `400 INVALID_LOGIN_REQUEST`
- **THEN** la interfaz muestra un mensaje de error claro sin conceder acceso y permite corregir los datos.

#### Scenario: Acceso interno no configurado o no disponible
- **WHEN** `POST /api/odontologo/sesion` responde `503 ACCESS_NOT_CONFIGURED` o `503 ACCESS_UNAVAILABLE`
- **THEN** la interfaz informa que el acceso interno no está disponible en este entorno y no intenta crear credenciales ni reintentar automáticamente.

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

### Requirement: CW-03 Gestión web de motivos de consulta
La interfaz autenticada SHALL permitir listar, crear y editar motivos mediante `GET /api/odontologo/motivos`, `POST /api/odontologo/motivos` y `PUT /api/odontologo/motivos/{motivoId}`. La duración SHALL enviarse como entero JSON múltiplo de 15 entre 15 y 240 minutos inclusive; no SHALL ofrecer borrado de motivos.

#### Scenario: Alta de motivo válido
- **WHEN** el odontólogo completa un nombre no vacío y una duración entera admitida y guarda
- **THEN** la interfaz envía `POST /api/odontologo/motivos`, muestra la respuesta `201` en la lista y conserva los demás motivos.

#### Scenario: Edición de motivo existente
- **WHEN** el odontólogo modifica el nombre o duración de un motivo y guarda
- **THEN** la interfaz envía `PUT /api/odontologo/motivos/{motivoId}` y actualiza el elemento solo tras recibir `200`.

#### Scenario: Error de validación o motivo inexistente
- **WHEN** el formulario contiene datos inválidos o la API responde `400 VALIDATION_ERROR` o `404 MOTIVO_NOT_FOUND`
- **THEN** la interfaz informa el rechazo sin modificar la lista persistida ni dar por guardado el cambio.

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

### Requirement: CW-05 Gestión web de bloqueos de agenda
La interfaz autenticada SHALL permitir listar bloqueos (`GET /api/odontologo/bloqueos`), crear un bloqueo (`POST /api/odontologo/bloqueos`) con inicio, fin y descripción opcional, y retirar un bloqueo (`DELETE /api/odontologo/bloqueos/{bloqueoId}`). Las fechas y horas ingresadas SHALL interpretarse en la zona del consultorio configurada, nunca desde la zona del navegador.

#### Scenario: Alta de bloqueo válido
- **WHEN** el odontólogo ingresa un intervalo válido con inicio anterior al fin y descripción opcional, y `POST /api/odontologo/bloqueos` responde `201`
- **THEN** la interfaz incorpora el nuevo bloqueo a la lista y limpia el formulario de alta.

#### Scenario: Bloqueo que superpone un turno confirmado
- **WHEN** `POST /api/odontologo/bloqueos` responde `409 CONFIGURATION_CONFLICT`
- **THEN** la interfaz informa que el bloqueo se superpone con un turno confirmado y no lo agrega a la lista.

#### Scenario: Retiro de bloqueo existente o inexistente
- **WHEN** el odontólogo retira un bloqueo y `DELETE /api/odontologo/bloqueos/{bloqueoId}` responde `204` o `404 BLOQUEO_NOT_FOUND`
- **THEN** ante `204` lo quita de la lista; ante `404` informa que ya no existía y sincroniza la lista desde el servidor.

### Requirement: CW-06 Estados operativos, errores y accesibilidad básica
La interfaz SHALL mostrar estado de carga durante cada solicitud, distinguir fallos de `409 AGENDA_BUSY` y `500 TECHNICAL_FAILURE` o de red sin dar por guardada la operación, permitir navegación completa con teclado con etiquetas asociadas a cada control y mantener legibilidad en pantallas pequeñas y grandes.

#### Scenario: Agenda ocupada o error técnico en una escritura
- **WHEN** una escritura recibe `409 AGENDA_BUSY`, `500 TECHNICAL_FAILURE` o un error de red
- **THEN** la interfaz informa el fallo de forma visible, sugiere reintentar manualmente en `AGENDA_BUSY` y nunca muestra la operación como guardada ni la reenvía automáticamente.

#### Scenario: Navegación por teclado y tamaños pequeño y grande
- **WHEN** el odontólogo recorre ingreso, motivos, horario y bloqueos usando teclado en anchos de 390 px y 1440 px
- **THEN** todos los controles interactivos son alcanzables y operables por teclado con etiquetas visibles y sin desbordamiento horizontal que oculte controles.
