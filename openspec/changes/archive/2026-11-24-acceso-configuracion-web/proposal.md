# Proposal

## Why

El backend ya cuenta con el acceso interno del odontólogo (C-03) y la API de configuración de disponibilidad (C-06), pero `frontend/` solo tiene un punto de entrada vacío. Sin esta interfaz, el odontólogo no puede iniciar sesión ni configurar motivos, horario semanal, pausas y bloqueos para habilitar US-001, US-002, US-003 y la entrada a FL-01.

## What Changes

- Implementar en React y TypeScript estricto la pantalla de ingreso y cierre de sesión del odontólogo consumiendo `GET /api/odontologo/csrf`, `POST /api/odontologo/sesion`, `GET /api/odontologo/sesion` y `POST /api/odontologo/sesion/cierre`.
- Incorporar un cliente HTTP tipado con manejo de token CSRF en memoria, verificación inicial de sesión al entrar al flujo interno y redirección al ingreso cuando la sesión expire o falte (`401 SESSION_REQUIRED`).
- Implementar las vistas de configuración de disponibilidad para el odontólogo autenticado:
  - **Motivos de consulta**: listar, crear y editar motivos con nombre y duración entera (múltiplos de 15 entre 15 y 240 minutos), sin borrado.
  - **Horario semanal y pausas**: consultar y editar el conjunto completo de franjas (`ATENCION` y `PAUSA`, días 1 a 7) y enviarlo en una sola operación `PUT`.
  - **Bloqueos**: listar, crear (con inicio, fin y descripción opcional) y retirar bloqueos.
- Manejar explícitamente estados de carga, errores de validación (`400`), recursos inexistentes (`404`), conflictos con turnos confirmados (`409 CONFIGURATION_CONFLICT`), contención de escritura (`409 AGENDA_BUSY`), fallos de servidor o red (`500` / error de transporte) y pérdida de sesión (`401`), sin asumir éxito ni reenviar escrituras automáticamente.
- **Fuera de alcance**: agenda de turnos (C-15), reserva o autogestión de pacientes (C-13, C-14), creación o modificación de endpoints o reglas de seguridad en el backend, incorporación de librerías de router, estado global o componentes visuales, y definición de marca o sistema visual (política `off`).

## Capabilities

### New Capabilities

- `dentist-configuration-web`: Interfaz web interna del odontólogo para ingresar, cerrar sesión y administrar motivos, horario semanal con pausas y bloqueos sobre la API existente, con manejo explícito de CSRF, sesión y errores.

### Modified Capabilities

<!-- Ninguna capacidad existente cambia sus requisitos. -->

## Impact

- **Código afectado**: únicamente `frontend/` (código de aplicación, estilos CSS propios y pruebas unitarias/de componentes) y `frontend/package.json` / `frontend/package-lock.json` si se aprueban las dependencias de desarrollo para pruebas.
- **APIs consumidas**: endpoints existentes de C-03 (`/api/odontologo/csrf`, `/api/odontologo/sesion`, `/api/odontologo/sesion/cierre`) y C-06 (`/api/odontologo/motivos`, `/api/odontologo/horario`, `/api/odontologo/bloqueos`), sin cambios en backend ni esquema PostgreSQL.
- **Gobernanza**: **CRITICAL** por integrar el ciclo de sesión y CSRF en el navegador; requiere revisión del contrato HTTP de C-06 y aprobación explícita de Sebastián antes de escribir código en `frontend/`.
