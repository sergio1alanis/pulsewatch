# Despliegue de PulseWatch

## Estado

El repositorio incluye un `Dockerfile` multi-stage para construir la aplicación con Java 21 y ejecutarla con una imagen JRE como usuario no root. El equipo local no necesita Docker: un proveedor compatible puede construir la imagen desde el repositorio. Aún no se ha creado ni desplegado ningún recurso en la nube.

## Variables de entorno necesarias

Configura estos valores en el panel seguro del proveedor, no en el repositorio:

| Variable | Uso |
|---|---|
| `DB_URL` | JDBC URL de PostgreSQL administrado, por ejemplo `jdbc:postgresql://host:5432/pulsewatch` |
| `DB_USERNAME` | Usuario de aplicación de PostgreSQL |
| `DB_PASSWORD` | Contraseña del usuario de aplicación |
| `PORT` | Puerto HTTP indicado por la plataforma; si falta, la aplicación usa `8080` |
| `PULSEWATCH_API_KEY` | Clave secreta para autenticar el API mediante `X-API-Key` |
| `PULSEWATCH_REQUIRE_API_KEY` | Ponlo en `true` en producción; si falta la clave, la app no inicia |
| `PULSEWATCH_RATE_LIMIT_PER_MINUTE` | Límite global por instancia; predeterminado: `120` |
| `PULSEWATCH_WEBHOOK_URL` | Opcional. Receptor HTTPS para eventos de incidencia |

El `Dockerfile` activa `PULSEWATCH_REQUIRE_API_KEY=true`: el contenedor no arrancará hasta que configures `PULSEWATCH_API_KEY`. Genera una clave aleatoria localmente (por ejemplo, con un gestor de contraseñas) y guárdala solo en el almacén de secretos del proveedor. Los clientes deben enviarla en el encabezado `X-API-Key` sobre HTTPS.

Al arrancar, Flyway aplica las migraciones pendientes (`V1`, `V2`, ...), y Hibernate valida el esquema. Configura un health check en `/actuator/health`.

## Límites antes de producción real

- El API admite una API key compartida y un límite global de solicitudes por instancia. En producción configura `PULSEWATCH_API_KEY` y `PULSEWATCH_REQUIRE_API_KEY=true`; usa HTTPS. Esto no sustituye cuentas/roles por usuario ni cuotas por cliente.
- El rate limit vive en memoria y se reinicia con la aplicación; no coordina varias réplicas.
- La política SSRF actual reduce el riesgo, pero no sustituye controles de egreso de red o una allowlist de destinos.
- Mantén una sola réplica mientras el scheduler no tenga bloqueo distribuido; varias réplicas podrían comprobar un mismo monitor simultáneamente.
- El webhook es best-effort y no reintenta errores. Para entrega confiable, añade una tabla outbox y reintentos.
- La instancia que ejecuta el scheduler debe permanecer activa. Si el proveedor suspende o duerme el servicio, se detendrán las comprobaciones automáticas.
- Verifica el costo del proveedor antes de crear recursos; esta configuración no provisiona bases de datos ni servicios.
