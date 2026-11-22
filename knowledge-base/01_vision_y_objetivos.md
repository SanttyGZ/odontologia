# Visión y objetivos

**Estado del documento**: aprobado por el usuario el 2026-11-18.
**Confirmación**: «Aprobar el documento 01 y continuar con el 02».
**Fecha**: 2026-11-18.
**Base**: [Discovery aprobado](../discovery/discovery.md) y [decisiones de entrega y stack aprobadas](../discovery/propuesta-tecnica-kb.md).

El alcance, los criterios de aceptación y el enfoque de medición de este documento fueron aprobados por el usuario. Los objetivos numéricos y los bordes identificados como pendientes siguen abiertos; no se presentan resultados ya obtenidos.

## Propósito del sistema

Reducir el trabajo de reservar, confirmar y reprogramar turnos de un odontólogo independiente mediante una agenda con reserva web y comunicaciones por email.

El paciente podrá elegir un motivo y un horario disponible, reservar y gestionar su turno mediante los enlaces recibidos por correo. El odontólogo administrará su disponibilidad y consultará los turnos y los datos básicos asociados. La hipótesis es que estos flujos reducirán la coordinación manual; todavía no se midió el problema ni se validó la solución con un odontólogo.

El objetivo comercial es desarrollar un SaaS para vender a consultorios en Argentina. La primera entrega se limita a una aplicación web funcional ejecutada localmente para **un único consultorio, un odontólogo y una agenda**. La operación de varios consultorios desde el mismo servicio será una decisión de una etapa posterior.

## Objetivos por actor

| Actor | Objetivo principal | Objetivos secundarios |
|---|---|---|
| Odontólogo | Disminuir la coordinación manual de los turnos. | Configurar motivos y duraciones, horarios semanales, pausas y bloqueos; consultar la agenda diaria y semanal y los datos básicos de pacientes. |
| Paciente | Reservar y gestionar un turno sin coordinación manual dentro de los plazos permitidos. | Consultar disponibilidad, recibir confirmación automática y avisos por email, cancelar o reprogramar mediante enlaces privados. |

El odontólogo es el único usuario interno. El paciente interactúa sin crear una cuenta ni verificar previamente su email. La primera versión no incorpora un usuario de recepción ni un rol funcional para administrar cadenas.

## Alcance de la primera versión

### Agenda y disponibilidad

- Una agenda del único odontólogo, con consulta diaria y semanal.
- Motivos de consulta y duraciones configuradas por el odontólogo.
- Horario semanal habitual con pausas y bloqueos por vacaciones o excepciones.
- Horarios ofrecidos según duración del motivo, disponibilidad y turnos existentes.
- Prevención de reservas superpuestas, incluidas solicitudes simultáneas.

### Reserva y autogestión

- Enlace web de reserva disponible en el entorno local de demostración.
- Reserva desde 24 horas de anticipación y hasta 30 días hacia adelante; la inclusión exacta de los límites sigue pendiente.
- Datos obligatorios: **nombre, apellido, email, teléfono y DNI**.
- Confirmación automática al reservar un horario disponible.
- Cancelación y reprogramación autónomas hasta 24 horas antes, mediante enlaces enviados por email; después de ese plazo se prevé contacto directo con el odontólogo.
- El nuevo horario de una reprogramación debe estar libre y cumplir las reglas de disponibilidad y anticipación.
- Acceso del odontólogo a su agenda y a los datos básicos asociados a los turnos; el diseño del acceso se documentará en los archivos de actores y arquitectura.

### Comunicaciones

- Email de confirmación inmediata, avisos de cambios y un recordatorio 24 horas antes.
- Correos reales dirigidos a destinatarios de prueba; proveedor y remitente pendientes de selección.
- Enlaces para gestionar el turno sin cuenta de paciente ni verificación previa del correo.
- Recordatorios dentro del backend, con estado de notificaciones persistido conforme al enfoque técnico aprobado. Reintentos, avisos vencidos y coincidencia de confirmación y recordatorio al reservar exactamente a 24 horas siguen pendientes de definición.

### Entorno y restricciones

- Ejecución local en la computadora de demostración, con PostgreSQL local.
- Plazo objetivo: una semana para la primera versión funcional. Las fechas de inicio y entrega no están acordadas.
- Stack aprobado: Java 21 Temurin, Spring Boot con Gradle Wrapper, React con TypeScript y Vite, CSS, PostgreSQL y SMTP mediante Spring Mail. Las versiones exactas restantes se fijarán al preparar el proyecto.
- Prioridad a tecnologías y servicios gratuitos cuando sea posible; no se definió un presupuesto mínimo ni un máximo absoluto de ARS 0.
- Para la demostración se utilizarán datos ficticios y direcciones de destinatarios de prueba, según la propuesta aprobada.

## Fuera de alcance de la primera versión

- Historia clínica, anamnesis, odontograma, periodontograma, radiografías y tratamientos.
- Cobros a pacientes, señas, caja, facturación e integraciones de pago.
- WhatsApp, chatbots, campañas y recuperación automatizada de pacientes.
- Recepción con acceso propio, múltiples profesionales, sillones, sedes u organizaciones.
- Lista de espera, reportes avanzados y sincronización con calendarios externos, que no son requisitos confirmados del MVP.
- Despliegue público, servicio continuo de reservas por internet y lanzamiento comercial dentro de esta entrega local.

La tarifa y el mecanismo de suscripción del futuro SaaS siguen pendientes. No se incorporan cobros recurrentes por inferencia del objetivo comercial. Las funcionalidades posteriores requerirán priorización explícita.

## Criterios de aceptación aprobados para la entrega local

Esta tabla traduce el alcance aprobado en comprobaciones observables. **Estado: aprobado por el usuario el 2026-11-18**, todavía sin ejecución ni evidencia de cumplimiento.

| Código | Comprobación acordada | Resultado observable esperado |
|---|---|---|
| CA-VIS-01 | Configurar motivos, duraciones, horario semanal, pausas y un bloqueo. | La disponibilidad ofrecida respeta la configuración y se puede consultar la agenda diaria y semanal. |
| CA-VIS-02 | Reservar con los cinco datos obligatorios en un horario válido. | El turno queda confirmado automáticamente y aparece en la agenda con los datos ingresados. |
| CA-VIS-03 | Solicitar dos reservas simultáneas que se superponen. | Se confirma como máximo una; la otra informa el conflicto sin crear un segundo turno superpuesto. |
| CA-VIS-04 | Cancelar y reprogramar desde los enlaces dentro del plazo permitido. | Se actualiza el turno, la disponibilidad refleja el cambio y se genera el aviso correspondiente. |
| CA-VIS-05 | Intentar autogestión fuera del plazo permitido. | Se impide el cambio autónomo conforme al límite que se acuerde; el procedimiento manual se documenta por separado. |
| CA-VIS-06 | Enviar confirmación, aviso de cambios y recordatorio a direcciones de prueba. | Se comprueba la recepción de cada tipo de mensaje y la apertura de los enlaces en el entorno local. La prueba de recepción no constituye una garantía general de entrega. |
| CA-VIS-07 | Reiniciar la aplicación y volver a consultar una reserva creada. | El turno y la configuración persistida permanecen disponibles; se comprueba persistencia, no restauración desde un respaldo. |

Para probar el recordatorio sin esperar un día completo se acordó utilizar un mecanismo controlado de prueba del tiempo. El mecanismo se diseñará después, conservará la regla real de 24 horas y se identificará como herramienta de prueba.

La aceptación completa depende de resolver los límites temporales, el caso de reserva exactamente a 24 horas, los cambios de configuración con turnos existentes, la identificación y corrección de pacientes y el tratamiento de fallas de correo. Este documento no decide esos bordes por omisión.

## Métricas de éxito

### Cumplimiento funcional de esta entrega

Registrar el resultado y evidencia de cada criterio de aceptación aprobado. El objetivo es comprobar todos los flujos incluidos en el alcance local y documentar cualquier pendiente. No existe todavía una aplicación sobre la cual afirmar cumplimiento.

### Validación posterior del producto

| Indicador propuesto | Cómo medirlo | Situación actual |
|---|---|---|
| Tiempo de coordinación por turno | Medir minutos invertidos en reservar, confirmar o reprogramar, antes y durante un piloto comparable. | Sin línea de base ni odontólogo disponible. |
| Proporción de gestiones autónomas | Dividir las reservas, cancelaciones y reprogramaciones resueltas por el paciente sin intervención entre el total de esas gestiones del período. | Sin medición; objetivos numéricos pendientes. |
| Intervenciones por problemas de comunicación | Registrar los casos que requieren contacto manual por correo incorrecto, enlace perdido o aviso no recibido. | Sin medición; procedimiento de registro pendiente. |

Las pruebas con datos ficticios comprueban el funcionamiento, pero no demuestran reducción de trabajo, demanda ni disposición a pagar. No se propone prometer una reducción porcentual sin datos de un piloto.

## Límites de la demostración y pendientes

Un correo real puede recibirse fuera de la computadora de demostración, pero un enlace `localhost` solo abre la aplicación en la computadora donde esta se ejecuta. La reserva y la autogestión se comprobarán desde ese entorno; probar desde otros dispositivos requiere definir una URL alcanzable.

El backend debe estar ejecutándose y tener conectividad para enviar avisos a tiempo. El tratamiento de notificaciones pendientes después de una interrupción todavía debe definirse. La entrega local no implica disponibilidad continua ni habilita el uso comercial con datos reales por sí sola.

Antes de implementar los flujos correspondientes se documentarán proveedor de email, acceso del odontólogo, gestión de enlaces privados, zona horaria y reglas pendientes. Se conserva la gobernanza del proyecto para aprobar código en los dominios que lo requieran.
