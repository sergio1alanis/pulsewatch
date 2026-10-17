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
| `PULSEWATCH_WEBHOOK_URL` | Opcional. Receptor HTTPS para eventos de incidencia |

Al arrancar, Flyway aplica las migraciones pendientes (`V1`, `V2`, ...), y Hibernate valida el esquema. Configura un health check en `/actuator/health`.

## Límites antes de producción real

- El API no tiene autenticación, cuotas ni rate limiting. No expongas el registro de monitores públicamente hasta añadirlos.
- La política SSRF actual reduce el riesgo, pero no sustituye controles de egreso de red o una allowlist de destinos.
- Mantén una sola réplica mientras el scheduler no tenga bloqueo distribuido; varias réplicas podrían comprobar un mismo monitor simultáneamente.
- El webhook es best-effort y no reintenta errores. Para entrega confiable, añade una tabla outbox y reintentos.
- La instancia que ejecuta el scheduler debe permanecer activa. Si el proveedor suspende o duerme el servicio, se detendrán las comprobaciones automáticas.
- Verifica el costo del proveedor antes de crear recursos; esta configuración no provisiona bases de datos ni servicios.
