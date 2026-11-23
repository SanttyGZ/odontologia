# dentist-authentication Specification

## Purpose
Permitir al único odontólogo ingresar y operar su agenda desde el entorno local. Proteger el acceso a los recursos internos en el backend durante todo el ciclo de la sesión.

## Requirements

### Requirement: DA-01 Alta y recuperación de la cuenta interna

El sistema SHALL admitir una única cuenta vinculada al odontólogo existente. Un operador con acceso local SHALL poder crear o reemplazar la credencial fuera de la API HTTP. El sistema SHALL conservar solo un verificador de contraseña y SHALL rechazar el ingreso si la credencial no está operativa.

#### Scenario: Alta en una instalación inicializada
- **WHEN** el operador configura una credencial local válida para el odontólogo inicializado
- **THEN** esa cuenta puede ingresar y el alta conserva el odontólogo y la agenda existentes.

#### Scenario: Credencial ausente o no válida
- **WHEN** la cuenta todavía no fue configurada o el archivo de verificación no se puede usar
- **THEN** el ingreso informa `503 ACCESS_NOT_CONFIGURED` sin crear una sesión autenticada, usar una credencial predeterminada o divulgar el contenido del archivo.

#### Scenario: Recuperación local
- **WHEN** el operador detiene el backend, reemplaza el verificador y vuelve a iniciar
- **THEN** la credencial nueva permite ingresar, la anterior se rechaza, las sesiones anteriores quedan inválidas y la agenda conserva su estado.

### Requirement: DA-02 Ingreso y consulta de sesión

La API interna SHALL validar la credencial del odontólogo y asociar la identidad a una sesión de servidor. El ingreso SHALL renovar el identificador de sesión y la consulta de sesión SHALL devolver únicamente el estado de acceso.

#### Scenario: Ingreso válido
- **WHEN** el navegador envía las credenciales correctas y un token CSRF válido a `POST /api/odontologo/sesion`
- **THEN** la API responde `204`, renueva el identificador de sesión y `GET /api/odontologo/sesion` devuelve `200` con `autenticado: true`.

#### Scenario: Ingreso rechazado
- **WHEN** una solicitud de ingreso con CSRF válido presenta un usuario desconocido o una contraseña incorrecta
- **THEN** la API devuelve `401 INVALID_CREDENTIALS` en ambos casos y no concede acceso.

#### Scenario: Formato de ingreso inválido
- **WHEN** una solicitud de ingreso con CSRF válido incluye parámetros en la URL o no usa un cuerpo de formulario
- **THEN** la API devuelve `400 INVALID_LOGIN_REQUEST` y no crea una sesión autenticada.

#### Scenario: Consulta sin sesión válida
- **WHEN** se consulta `GET /api/odontologo/sesion` sin una sesión interna válida
- **THEN** la API devuelve `401 SESSION_REQUIRED` sin datos internos ni redirección.

### Requirement: DA-03 Autorización de las API internas

El backend SHALL exigir una sesión válida del odontólogo para leer o modificar recursos de `/api/odontologo/**`. Solo `GET /api/odontologo/csrf` y `POST /api/odontologo/sesion` SHALL ser accesibles antes del ingreso para completar el flujo. La autenticación interna SHALL quedar acotada a la familia del odontólogo.

#### Scenario: Llamada directa sin sesión
- **WHEN** un cliente sin sesión intenta consultar o modificar un recurso interno
- **THEN** el backend rechaza la solicitud sin devolver datos internos ni efectuar cambios. Una lectura interna devuelve `401 SESSION_REQUIRED`.

#### Scenario: Llamada interna autorizada
- **WHEN** una solicitud lleva una sesión válida y cumple las comprobaciones aplicables al método
- **THEN** el control de acceso deja continuar al manejador de la operación.

#### Scenario: Familias de rutas de pacientes
- **WHEN** los cambios que exponen rutas públicas o de autogestión las integran
- **THEN** la reserva pública no exige sesión interna y una sesión de odontólogo no sustituye la credencial limitada al turno que requiere la autogestión.

### Requirement: DA-04 Protección CSRF del acceso y las escrituras

El backend SHALL exigir un token CSRF válido para el ingreso, el cierre y las escrituras internas. El navegador SHALL poder obtener un token ligado a su sesión con una respuesta sin caché y renovarlo tras ingresar o cerrar sesión.

#### Scenario: Operación de escritura sin un token válido
- **WHEN** se invoca el ingreso, el cierre o una escritura interna sin el token vigente
- **THEN** se devuelve `403 CSRF_INVALID` y no se autentica, invalida ni modifica el estado de la operación solicitada.

#### Scenario: Token renovado al cambiar la sesión
- **WHEN** el navegador completa el ingreso o el cierre
- **THEN** puede obtener un token nuevo en `GET /api/odontologo/csrf` y el token anterior no autoriza una escritura posterior.

### Requirement: DA-05 Fin de sesión y transporte local

El backend SHALL terminar la sesión con cierre explícito, 30 minutos de inactividad o reinicio. SHALL transportar el identificador mediante una cookie de sesión inaccesible a JavaScript. El uso de HTTP para el acceso interno SHALL limitarse a loopback.

#### Scenario: Cierre explícito
- **WHEN** el odontólogo envía `POST /api/odontologo/sesion/cierre` con sesión y CSRF válidos
- **THEN** el backend responde `204`, invalida la sesión, ordena retirar su cookie y rechaza su identificador anterior.

#### Scenario: Cierre con CSRF válido y sin sesión autenticada
- **WHEN** el navegador envía `POST /api/odontologo/sesion/cierre` con un token CSRF válido de una sesión anónima
- **THEN** el backend devuelve `401 SESSION_REQUIRED` y esa sesión no adquiere acceso interno.

#### Scenario: Expiración por inactividad
- **WHEN** transcurren 30 minutos sin actividad en la sesión
- **THEN** la siguiente consulta interna requiere volver a ingresar.

#### Scenario: Reinicio del backend
- **WHEN** el backend se reinicia sobre la misma instalación
- **THEN** el identificador de sesión anterior no autentica y la credencial local sigue habilitando un nuevo ingreso.

#### Scenario: Configuración HTTP fuera de loopback
- **WHEN** se intenta habilitar el acceso interno por HTTP con una dirección de escucha o un origen de navegador fuera de loopback
- **THEN** esa configuración se rechaza y el acceso interno no queda expuesto por HTTP.
