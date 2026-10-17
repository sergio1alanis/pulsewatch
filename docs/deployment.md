# Despliegue de PulseWatch

## Estado

El repositorio incluye un `Dockerfile` multi-stage para construir la aplicación con Java 21 y ejecutarla con una imagen JRE como usuario no root, además de `render.yaml` para un servicio Render Free conectado a Neon Free. El equipo local no necesita Docker. La configuración está lista, pero aún no se han creado los proyectos de Render/Neon ni desplegado la app: hace falta iniciar sesión y completar los datos de conexión directamente en los paneles de los proveedores.

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

## Alternativa gratuita: Render + Neon

El archivo [`render.yaml`](../render.yaml) define únicamente un servicio web Docker gratuito en Oregon. Al crear el Blueprint, Render genera `PULSEWATCH_API_KEY` y solicita `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`; introduce esos datos directamente en el formulario seguro del proveedor, nunca en el chat ni en Git.

1. Crea un proyecto **Neon Free** en AWS US West 2 (Oregon) y conserva el plan gratuito. La cuenta Free es permanente, sin tarjeta, con 1 GB de almacenamiento y cuota de cómputo; Neon suspende el cómputo tras inactividad. Revisa los límites actuales en [Neon Pricing](https://neon.com/pricing).
2. En Render, crea un Blueprint desde `sergio1alanis/pulsewatch`, rama `main`, y revisa que el servicio se mantenga en el plan `free`.
3. En el formulario inicial de Render, introduce las variables de conexión de Neon. Para `DB_URL`, usa formato JDBC: cambia el prefijo `postgresql://` de Neon por `jdbc:postgresql://` y conserva `sslmode=require` para exigir TLS. No cambies el plan ni habilites servicios adicionales de pago.
4. Espera el health check en `/actuator/health`. Swagger está en `/swagger-ui.html`; el API requiere la clave generada por Render.

Es un despliegue de demostración con costo de infraestructura esperado de **US$0** si se mantienen los planes Free y no se habilitan complementos pagos. Render duerme el servicio tras 15 minutos sin tráfico y puede tardar alrededor de un minuto en despertarlo; durante el sueño no corren los checks programados. Neon Free también limita las horas de cómputo y escala a cero; consulta [Render Free](https://render.com/docs/free) y [Render Pricing](https://render.com/pricing). No lo uses como monitor siempre activo ni para disponibilidad garantizada.

## Límites antes de producción real

- El API admite una API key compartida y un límite global de solicitudes por instancia. En producción configura `PULSEWATCH_API_KEY` y `PULSEWATCH_REQUIRE_API_KEY=true`; usa HTTPS. Esto no sustituye cuentas/roles por usuario ni cuotas por cliente.
- El rate limit vive en memoria y se reinicia con la aplicación; no coordina varias réplicas.
- El probe valida todos los resultados DNS y fija las IP aceptadas para esa conexión; conserva el hostname para TLS, bloquea rangos privados/especiales, desactiva proxies y no sigue redirecciones. Añade además controles de egreso en la red del proveedor: la aplicación puede conectar a cualquier IP pública por diseño.
- Mantén una sola réplica mientras el scheduler no tenga bloqueo distribuido; varias réplicas podrían comprobar un mismo monitor simultáneamente.
- El webhook es best-effort y no reintenta errores. Para entrega confiable, añade una tabla outbox y reintentos.
- La instancia que ejecuta el scheduler debe permanecer activa. Si el proveedor suspende o duerme el servicio, se detendrán las comprobaciones automáticas.
- Verifica el costo del proveedor antes de crear recursos; esta configuración no provisiona bases de datos ni servicios.
