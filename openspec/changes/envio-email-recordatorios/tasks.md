# Tasks

## 1. Revisión HIGH y configuración local

- [x] 1.1 Registrar la aprobación HIGH y las decisiones de SMTP, remitente, destinatarios, operación y evidencia en el diseño. Confirmar la disponibilidad de contraseña de aplicación para Gmail y el acceso del titular a la sesión web del único buzón habilitado. Registrar la autorización de envíos reales antes de ejecutarlos.
- [x] 1.2 Incorporar Spring Mail y configuración local con TLS, tiempos finitos y emisor deshabilitado por defecto. Probar que el entorno de prueba bloquea destinos no autorizados y que los diagnósticos omiten secretos y enlaces privados. Documentar configuración sin valores sensibles.

## 2. Despacho y contenido

- [x] 2.1 Consumir despachos elegibles con sondeo y reloj controlables. Autorizar el intento durable con coordinación breve y ejecutar SMTP fuera de ella. Probar en PostgreSQL ambos órdenes de la carrera con cancelación/reprogramación, vencimiento durante la espera y exclusión de envíos simultáneos del mismo despacho.
- [x] 2.2 Componer y comprobar con transporte simulado confirmación, cancelación, reprogramación, recordatorio y despacho combinado al borde exacto de 24 horas. Verificar revisión y destinatario persistidos, referencia revocada, y el tratamiento aprobado del aviso de recuperación de C-04.

## 3. Resultados y recuperación

- [x] 3.1 Persistir aceptación SMTP, fallo transitorio, fallo permanente y resultado incierto. Probar con reloj controlado las esperas de 1 y 5 minutos, tres intentos máximos y ausencia de reintentos para vencidos, obsoletos, permanentes o inciertos.
- [x] 3.2 Verificar con reinicio y PostgreSQL avisos futuros, vigentes, vencidos y una interrupción tras posible aceptación. Documentar cómo identificar y revisar fallos persistidos sin reenviar automáticamente los inciertos.

## 4. Aceptación integrada

- [ ] 4.1 Ejecutar build y las pruebas de regresión de C-04/C-05/C-09. Con autorización, comprobar los cuatro casos en los buzones de prueba, abrir los enlaces aplicables desde el equipo local y usar reloj controlado para el recordatorio. Registrar evidencia redactada correlacionada con el intento y distinguir la copia recibida tanto del resultado SMTP como del mensaje en Enviados. Registrar los recorridos pendientes para C-16.
