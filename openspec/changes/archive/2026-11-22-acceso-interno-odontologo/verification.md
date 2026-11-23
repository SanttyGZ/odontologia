# Comprobaciones de C-03 acceso-interno-odontologo

El usuario aprobó D-01 a D-04 y autorizó el código CRITICAL. La excepción para C-03 cubre el contrato incompleto de evidencia de `$sdd-orchestrator`. Se ejecutaron pruebas reales y un revisor independiente repitió las comprobaciones en una copia con las mismas entradas de código y construcción.

## Resultados

| Requisito | Comprobación y resultado |
|---|---|
| DA-01 | El archivo se escribe con acceso privado y un verificador con sal. Se rechazaron archivos ausentes, malformados, con permisos abiertos o enlaces simbólicos. Se comprobó el alta sobre una base C-02, el `503` de ingreso sin credencial y la rotación conservando el identificador persistido de la agenda. |
| DA-02 | Se comprobó ingreso con credencial válida, error uniforme ante credenciales incorrectas, rechazo de formatos inválidos, rotación de identificador y consulta de estado sin datos de agenda. |
| DA-03, CT-02 | Lecturas y escrituras directas contra manejadores de prueba quedan protegidas. Se comprobó la ausencia de efectos ante rechazo, la autorización de una sesión válida y el aislamiento del principal de C-03 respecto de las familias públicas y de autogestión. |
| DA-04 | Se comprobaron tokens ausentes, inválidos, anteriores y renovados en el ciclo de ingreso, escritura y cierre. La obtención del token devuelve `Cache-Control: no-store`. |
| DA-05, FL-01 | Se comprobaron cookie HTTP local y cookie `Secure` por HTTPS, rechazo de configuración HTTP fuera de loopback, cierre efectivo, `401` al cerrar una sesión anónima con CSRF válido y rechazo de identificadores antiguos. La prueba observó 1.800 segundos como valor efectivo en sesión y redujo el plazo en una sesión de prueba a un segundo para observar la expiración en el servidor real. |

Con Java 21 y PostgreSQL 17.12 de Compose, desde la raíz:

```bash
python3 scripts/with-local-db.py --integration ./backend/gradlew -p backend test integrationTest bootJar --rerun-tasks
openspec validate acceso-interno-odontologo --type change --strict --no-interactive
```

La suite obtuvo **26 pruebas unitarias y 21 de integración**, con cero fallos, errores y omisiones. El JAR se ejecutó sobre una base PostgreSQL efímera. Se aprovisionó y rotó la credencial mediante la terminal interactiva del JAR y se recorrió ingreso, cierre y reinicio por el proxy Vite, usando un cliente HTTP con almacén real de cookies. El backend rechazó identificadores previos y la credencial anterior. La agenda mantuvo su identificador. Los procesos de prueba se detuvieron y se eliminó la base efímera.

El caso heredado de arranque concurrente de C-02 reprodujo una carrera en la inicialización de Logback dentro de una JVM compartida. El ajuste se limitó a los dos contextos concurrentes de esa prueba, que volvieron a pasar iniciándose sobre la misma base real.

## Revisión independiente

El revisor corroboró en una copia nueva los 26 resultados unitarios, los 21 de integración y el build del JAR. La revisión cubrió archivos de credencial, configuración HTTP, alcance de filtros internos, sesión, CSRF, respuesta de cierre y cobertura de las pruebas. Con el mismo JAR, un certificado local de prueba y una base efímera, el revisor comprobó `Secure` antes y después del ingreso y al retirar la cookie tras el cierre. Las entradas de ejecución de ambas copias coincidieron con SHA-256 `cf0842b211012dcd38198686f2d7e6e8a770cf35e1441eca976cde3ce6ad2179`, sobre el listado ordenado de rutas y sumas de `backend`, `frontend`, `scripts`, `.env.example` y `compose.yaml`. Se conserva la salida redaccionada local bajo la excepción autorizada.

## TDD Cycle Evidence

| Tareas | Pruebas | Nivel | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| 2.1–2.2, 3.4 | `DentistCredentialFileTest`, `LocalEnvironmentPostProcessorTest` | Unitaria | Gradle confirmó la línea base existente desde su caché. | La compilación detectó la ausencia del componente. | Suite unitaria verde. | Sal, rotación, permisos, formatos, rutas en Git y loopback. | Suite final verde. |
| 3.1–3.3, 3.5, 4.1 | `DentistAccessTest` | HTTP y PostgreSQL | Se conservaron los tests de conectividad de C-02. | Tres escenarios HTTP fallaron antes de la cadena interna. | Los tres escenarios pasaron. | Variantes de credencial, cookies, CSRF, sesión y familias de rutas. | Tomcat 11 devuelve `Expires` al retirar la cookie. La prueba analiza la expiración efectiva y se ejecutó nuevamente. |
| 4.2 | `PostgresConnectivityTest` y suite completa | Integración | Dos arranques concurrentes sobre la misma base. | Se reprodujo el fallo del logger. | La suite completa pasó tras aislar el listener en esa prueba. | Reproducción independiente en copia nueva y comprobación TLS. | Suite final verde. |

La pantalla de ingreso queda en C-12. La consulta de agenda y la parte de autogestión de CT-02 se verifican con C-10 y C-04 respectivamente.
