# Proposal

## Why

C-02 dejó disponible el perfil del único odontólogo y su agenda. C-06 y C-10 necesitan una regla común de acceso en el backend antes de exponer configuración, turnos o datos de pacientes, y C-12 necesita un contrato para ingresar y cerrar sesión.

## What Changes

- Definir el alta inicial y la recuperación local de la única cuenta interna, el ingreso, el cierre, la expiración y el efecto del reinicio.
- Proteger las consultas y escrituras de `/api/odontologo/**` y definir las excepciones de ingreso y obtención de CSRF para el navegador local.
- Entregar a C-12 el contrato de sesión, CSRF y errores, y a C-06 y C-10 la regla de autorización de sus futuras rutas.
- Comprobar la parte de acceso interno de FL-01 y CT-02 mediante llamadas directas al backend. Las funciones de agenda, las pantallas y la credencial del paciente quedan en C-10, C-12, C-15 y C-04 según el roadmap.

## Capabilities

### New Capabilities

- `dentist-authentication` — Alta y recuperación local, ciclo de sesión y autorización de las API internas del odontólogo.

### Modified Capabilities

## Impact

El diseño afecta al backend Spring Boot, su configuración de desarrollo, el arranque con `bootJar` y las pruebas HTTP. Reutiliza el proxy `/api` de Vite y el odontólogo inicializado por C-02. El mecanismo de acceso se revisa en AT-03/PT-04 con gobernanza CRITICAL y la escritura de código requiere aprobación explícita separada.

Referencias del proyecto. [C-03](../../../../CHANGES.md#c-03-acceso-interno-odontologo) · [FL-01](../../../../knowledge-base/07_flujos_principales.md#fl-01--acceder-como-odontólogo-y-consultar-la-agenda) · [AT-03](../../../../knowledge-base/10_preguntas_abiertas.md#preguntas-técnicas-abiertas-at)
