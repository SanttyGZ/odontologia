# Design

## Context

El usuario aprobó D-01 a D-04 para AT-03 y la parte de acceso interno de PT-04. C-02 está archivado y ya inicializa un único odontólogo y su agenda. Spring Boot usa MVC y Vite reenvía `/api` desde `127.0.0.1:5173` al backend en `127.0.0.1:8080`.

Referencias. [AT-03](../../../../knowledge-base/10_preguntas_abiertas.md#preguntas-técnicas-abiertas-at) · [Inicialización de C-02](../../../../backend/src/main/java/dev/odontologia/consultorio/ConsultorioInitializer.java) · [Proxy Vite](../../../../frontend/vite.config.ts)

## Goals / Non-Goals

**Objetivo.** Dar a C-06 y C-10 un control de acceso común y a C-12 un contrato HTTP comprobable. Conservar los datos de negocio al habilitar o recuperar el acceso.

**Límite.** El trabajo de C-03 se concentra en la API y la operación local de la credencial. Las pantallas y la agenda siguen el alcance de la [propuesta](proposal.md). La autogestión del paciente se rige por el contrato de C-04.

## Decisions

### D-01. Alta y recuperación local

Usar una cuenta local con nombre de ingreso elegible por el operador y vinculada al odontólogo único de C-02. Usar un archivo privado fuera del repositorio con el nombre de ingreso y un verificador versionado generado con `PasswordEncoder` de Spring Security. El formato inicial usa PBKDF2 con HMAC-SHA-256, sal aleatoria de 16 bytes y 600 000 iteraciones. Un archivo separado permite rotar la credencial conservando el esquema y los datos de C-02.

La utilidad local de alta y recuperación pedirá la contraseña sin eco, confirmará el valor y escribirá el archivo de forma atómica con acceso restringido al usuario del proceso. La configuración versionada indicará cómo aportar la ubicación del archivo. Los registros y las respuestas omitirán contraseña y verificador. Si el archivo falta, es ilegible o tiene un formato inválido, el ingreso ofrecerá `503 ACCESS_NOT_CONFIGURED` y las rutas internas permanecerán protegidas. El operador recupera el acceso deteniendo el backend, reemplazando el archivo y reiniciando.

### D-02. Sesión en el backend

Usar la autenticación de formulario de Spring Security con respuestas de API y sesión en memoria. El ingreso exitoso renueva el identificador. El cierre invalida la sesión y retira la cookie. Se proponen 30 minutos de inactividad, medidos por actividad asociada a la sesión en el servidor. El reinicio invalida todas las sesiones. El cliente consultará el estado al entrar al flujo interno y cuando reciba un rechazo, evitando consultas periódicas que prolonguen la sesión.

La cookie será de sesión, `HttpOnly`, `SameSite=Strict`, sin `Domain` y con `Path=/api/odontologo`. Se retirará con el mismo nombre y ruta. El modo HTTP de la demo requiere una dirección de escucha y un origen de navegador en loopback. Si la aplicación usa HTTPS, la cookie tendrá `Secure`. La configuración de transporte público queda fuera de la entrega local.

### D-03. Autorización y contrato de C-12

Limitar la cadena de sesión interna a `/api/odontologo/**`. Exigir la identidad interna para toda ruta de esa familia salvo los métodos exactos de obtención de CSRF e ingreso. Las futuras rutas de C-06 y C-10 quedan cubiertas por esa regla. Las familias `/api/public/**` y `/api/gestion/**` mantienen sus contratos propios. Vite y el navegador usan rutas relativas para que la sesión y el token funcionen desde el mismo origen en desarrollo.

| Método y ruta | Entrada | Respuesta propuesta |
|---|---|---|
| `GET /api/odontologo/csrf` | Cookie si ya existe. | `200` con `token` y `headerName`. La respuesta lleva `Cache-Control: no-store`. |
| `POST /api/odontologo/sesion` | `username` y `password` en el cuerpo `application/x-www-form-urlencoded`, con encabezado CSRF. | `204` al ingresar, `400 INVALID_LOGIN_REQUEST` para formato inválido, `401 INVALID_CREDENTIALS` si falla la credencial, `403 CSRF_INVALID` si falla CSRF, `503 ACCESS_NOT_CONFIGURED` si la cuenta local no está operativa o `503 ACCESS_UNAVAILABLE` si la consulta del odontólogo no está disponible. |
| `GET /api/odontologo/sesion` | Cookie de sesión. | `200` con `autenticado: true` o `401 SESSION_REQUIRED`. |
| `POST /api/odontologo/sesion/cierre` | Cookie de sesión y encabezado CSRF. | `204` con invalidación de sesión y retiro de cookie. `403 CSRF_INVALID` si el token es inválido o falta. Con token válido y sin sesión autenticada, `401 SESSION_REQUIRED`. |

Los errores de acceso devuelven JSON con `codigo` y sin datos internos. Las lecturas internas sin sesión devuelven `401 SESSION_REQUIRED`. En escrituras, la comprobación de CSRF puede responder `403 CSRF_INVALID` antes de comprobar si existe una sesión autenticada. C-12 volverá al ingreso si la sesión expira y solicitará un token nuevo antes de una acción posterior. El cliente mostrará un fallo de escritura sin reenviar automáticamente la operación.

El cierre debe pasar por la comprobación de sesión autenticada antes de ejecutar los manejadores de invalidación. `LogoutFilter` se ejecuta antes de `AuthorizationFilter` en Spring Security. Para conservar el `401` del contrato, el endpoint de cierre se autorizará antes de invocar los manejadores de cierre de Spring Security. [Cierre de sesión en Spring Security](https://docs.spring.io/spring-security/reference/servlet/authentication/logout.html#customizing-logout-uris)

### D-04. Ciclo del token CSRF

Mantener la protección de Spring Security en ingreso, cierre y escrituras internas. Guardar el token esperado en la sesión y exponer a React el valor del token y el nombre del encabezado mediante el endpoint dedicado. El cliente conserva el token en memoria, lo envía en el encabezado indicado, lo descarta al cambiar la sesión y obtiene uno nuevo antes de la siguiente escritura. Aplicar `Cache-Control: no-store` también a las respuestas del contrato de sesión.

[Sesiones de Spring Security](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html) · [CSRF de Spring Security](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) · [Verificadores de contraseña](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)

## Risks / Trade-offs

- Reiniciar interrumpe el acceso del odontólogo → documentar que debe volver a ingresar y comprobar que su agenda se conserva.
- Cambiar el host entre `localhost` y `127.0.0.1` cambia el alcance de la cookie → usar el origen de loopback documentado durante cada recorrido y verificar el proxy de Vite con el mismo host.
- Una comprobación basada solo en rutas todavía inexistentes puede tomar un `404` como evidencia de protección → probar la regla con manejadores de lectura y escritura exclusivos del entorno de pruebas, además de los endpoints reales de sesión.

## Migration Plan

Generar la credencial y configurar su ruta para el primer ingreso. Probar que el backend de C-03 arranca sobre una base ya inicializada por C-02 con y sin credencial disponible. La rotación repite el procedimiento local con el backend detenido y conserva la base. Para retirar C-03 durante el desarrollo, volver a la versión de C-02 y retirar la configuración de credencial de la ejecución. La base conserva el esquema y los datos de C-02.

## UI Mockups

Decision: no (no UI)

## Revisión y aprobación

- El usuario aprobó D-01 a D-04 el 2026-11-22.
- El usuario autorizó explícitamente el código CRITICAL de C-03 el 2026-11-22.
- El usuario autorizó para C-03 la excepción al contrato de evidencia incompleto de `$sdd-orchestrator`. Se conservan pruebas reales, capturas redaccionadas y una revisión independiente de código y ejecución.

## Ronda de planificación

`{action: propose, step: planificación documental, signal: conserva las decisiones del borrador de esta conversación, direct}`

`{action: apply, step: implementación aprobada, signal: decisiones y autorizaciones de esta conversación, direct}`

`{action: verify, step: reproducción y revisión, signal: revisor independiente, delegated}`

`{action: archive, step: cierre del change, signal: 11 tareas completadas y revisión independiente, direct}`
