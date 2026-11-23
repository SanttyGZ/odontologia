# Tasks

## 1. Revisión y preparación

- [x] 1.1 Registrar en [Revisión y aprobación](design.md#revisión-y-aprobación) las decisiones D-01 a D-04, la autorización explícita separada de código CRITICAL y la condición de evidencia aplicable antes de iniciar el código.
- [x] 1.2 Incorporar las dependencias de Spring Security administradas por el BOM y actualizar el bloqueo de Gradle. Comprobar que el backend y la configuración de pruebas resuelven dependencias en modo estricto.

## 2. Credencial local

- [x] 2.1 Implementar el alta y la recuperación local y documentar el procedimiento en el README. Comprobar sobre archivos de prueba la escritura atómica, el acceso restringido, la ausencia de contraseña en texto claro y el rechazo de la credencial anterior tras reemplazarla. DA-01.
- [x] 2.2 Cargar la credencial durante el arranque y documentar cómo aportar la ubicación del archivo. Comprobar los casos de archivo válido, ausente, ilegible e inválido, con el backend operativo y el ingreso rechazado de forma controlada cuando corresponda. DA-01.

## 3. API interna y sesión

- [x] 3.1 Aplicar la regla de autorización a `/api/odontologo/**`. Probar por HTTP con manejadores exclusivos de pruebas que una consulta o escritura sin sesión no llega a la operación, que una sesión válida sí permite continuar y que las familias de pacientes quedan fuera de la sesión interna. DA-03, CT-02.
- [x] 3.2 Implementar los endpoints de ingreso, estado y cierre con el contrato de errores del diseño. Comprobar la renovación del identificador, el rechazo del identificador anterior tras el ingreso y el cierre, el `401` de cierre con CSRF válido y sesión anónima, y la expiración acordada. DA-02, DA-05.
- [x] 3.3 Integrar el token y las validaciones CSRF, incluyendo sesión iniciada, anónima y cerrada. Comprobar token ausente, inválido, anterior y renovado, falta de efectos ante rechazo y ausencia de caché en las respuestas de sesión. DA-04.
- [x] 3.4 Aplicar los atributos y alcance de la cookie y acotar HTTP a loopback. Comprobar los encabezados de creación y retiro de cookie, el rechazo de configuración HTTP fuera de loopback y el flujo HTTP a través del proxy de Vite. DA-05.
- [x] 3.5 Revisar el contrato de C-12 y las reglas para C-06 y C-10 contra las pruebas HTTP. Dejar en el change las rutas, cuerpos de error y comportamiento de sesión y CSRF comprobados. DA-02, DA-03, DA-04.

## 4. Integración y revisión

- [x] 4.1 En PostgreSQL aislado, ejecutar el backend empaquetado sobre el esquema de C-02, reiniciar y verificar que se conserva la agenda, el identificador anterior se rechaza y la credencial local permite volver a ingresar. Repetir con rotación de credencial. DA-01, DA-05, FL-01.
- [x] 4.2 Ejecutar las comprobaciones unitarias, de integración y de empaquetado pertinentes. Registrar evidencia por requisito y obtener revisión independiente de los controles y del estado exacto probado. CT-02, CT-06. [Resultados](verification.md)

## Workflow follow-up

- Archivar con OpenSpec después de la verificación y actualizar C-03 en el roadmap con sus comprobaciones. Registrar la decisión de AT-03 en la KB cuando corresponda.
