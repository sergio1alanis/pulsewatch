# PulseWatch

> Estado: MVP en desarrollo.

PulseWatch monitoreará la disponibilidad y el tiempo de respuesta de APIs y sitios HTTP/HTTPS configurados por el usuario.

## Objetivo del MVP

- Registrar endpoints con intervalo de revisión controlado.
- Ejecutar comprobaciones HTTP con timeout.
- Guardar resultados y duración de cada comprobación.
- Crear una incidencia cuando un endpoint pase de disponible a caído y cerrarla al recuperarse.
- Evitar alertas repetidas por una caída que continúa.

## Decisiones técnicas iniciales

- **Java 21 y Spring Boot:** construir una API backend con un stack habitual en equipos de desarrollo.
- **Monolito modular:** comenzar con una aplicación y separar responsabilidades por dominio; los microservicios añadirían operación y complejidad sin necesidad en este alcance.
- **PostgreSQL:** persistir monitores, resultados e incidencias.
- **Pruebas primero:** expresar cada regla con una prueba y observar que falla antes de implementar el comportamiento.
- **Intervalo entre 30 y 3600 segundos:** limitar la frecuencia inicial para no generar tráfico excesivo.
- **Solo HTTP/HTTPS y sin credenciales en la URL:** reducir destinos inválidos y evitar exponer secretos en logs o historial.
- **Timeouts y sin seguir redirecciones:** una URL no debe poder redirigir la comprobación a otra red o mantenerla abierta indefinidamente.

## Seguridad de los destinos

Antes de conectar, PulseWatch resuelve el host y rechaza IP privadas, loopback y rangos reservados; tampoco sigue redirecciones HTTP. El scheduler registra esos destinos como `BLOCKED` y espera el intervalo configurado antes de reintentarlos. Esto reduce el riesgo de SSRF, pero no elimina por completo DNS rebinding entre la validación y la conexión. Para producción se debe añadir control de egreso de red o una allowlist de destinos. `pulsewatch.probe.allow-loopback` solo se habilita en pruebas.

## Gestión de incidencias

- Una comprobación `DOWN` abre una incidencia si no hay otra abierta para el monitor.
- Las comprobaciones `DOWN` posteriores actualizan la última observación y no duplican la incidencia.
- La siguiente comprobación `UP` la resuelve; una caída posterior abre una incidencia nueva.
- `UNKNOWN` y `BLOCKED` no abren ni resuelven incidencias, porque no confirman una caída ni una recuperación.
- `GET /api/v1/monitors/{id}/incidents` devuelve hasta 100 incidencias, de la más reciente a la más antigua.
- El MVP registra incidencias y puede enviar un webhook JSON al abrir o resolver una incidencia.

## Notificaciones externas

`PULSEWATCH_WEBHOOK_URL` es opcional. Si no se configura, PulseWatch no envía notificaciones. Si se configura, envía un `POST` JSON después de confirmar la transacción, con `INCIDENT_OPENED` o `INCIDENT_RESOLVED` y los datos del monitor/incidente. Las caídas repetidas no generan envíos duplicados. Un error del webhook se registra y no revierte la incidencia; este MVP no reintenta entregas. Usa HTTPS para webhooks remotos y conserva la URL como secreto si incluye un token.

## Documentación interactiva de la API

Con PulseWatch ejecutándose, abre `http://localhost:8080/swagger-ui.html` para explorar y probar los endpoints desde el navegador. El documento OpenAPI en JSON está disponible en `http://localhost:8080/v3/api-docs`. Swagger UI es una herramienta para desarrolladores; un dashboard visual para usuarios finales sería una interfaz separada.

## PostgreSQL y Flyway

- **Servicio administrado:** un proveedor aloja PostgreSQL y se encarga del servidor, copias de seguridad y mantenimiento. La aplicación se conecta usando una URL y credenciales; no necesitas ejecutar la base de datos en tu computadora.
- **Migración Flyway:** un archivo SQL versionado que crea o modifica el esquema una sola vez. `V1__create_monitor_schema.sql` crea monitores e historial; `V2__create_monitor_incidents.sql` añade incidencias. Los cambios posteriores irán en `V3`, etc. Hibernate valida que las entidades coincidan con el esquema.

Para desarrollo local puedes usar PostgreSQL instalado directamente en tu sistema; Docker no es necesario.

## Desarrollo local

Requisitos: JDK 21 y Maven Wrapper. Las pruebas usan H2; para ejecutar la aplicación como servidor se necesita una base PostgreSQL configurada.

Para conectarte a la instancia local, crea en pgAdmin un usuario de aplicación llamado `pulsewatch_app` con contraseña y una base de datos `pulsewatch` propiedad de ese usuario. Guarda la contraseña en una variable de entorno, no en Git ni en el código:

```powershell
$env:DB_URL = "jdbc:postgresql://127.0.0.1:5432/pulsewatch"
$env:DB_USERNAME = "pulsewatch_app"
$env:DB_PASSWORD = "<tu contraseña local>"
```

Luego ejecuta la aplicación:

```powershell
.\mvnw.cmd spring-boot:run
```

Flyway aplicará la migración inicial al arrancar. `spring.jpa.hibernate.ddl-auto=validate` hará que Spring falle al inicio si el esquema de PostgreSQL no coincide con las entidades, en vez de modificarlo silenciosamente.

Para un servicio administrado, configura `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` con los valores que entregue el proveedor.


Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Bash/Git Bash:

```bash
./mvnw test
```

Si el wrapper no puede descargar Maven en Git Bash, configura `TMPDIR` con una ruta de Windows escribible y vuelve a intentarlo.

Las pruebas usan H2 en memoria para ejecutarse sin un servidor PostgreSQL local. H2 es solo una base de pruebas; el entorno de ejecución del proyecto se configurará con PostgreSQL.

## Progreso

- [x] Crear el esqueleto Spring Boot y comprobar el entorno de pruebas.
- [x] Validar el destino y el intervalo del monitor mediante pruebas unitarias.
- [x] Registrar monitores en `POST /api/v1/monitors` y comprobar su persistencia en pruebas H2.
- [x] Listar monitores por nombre con `GET /api/v1/monitors`.
- [x] Consultar un monitor por ID con `GET /api/v1/monitors/{id}`; devolver 404 si no existe.
- [x] Pausar/reactivar monitores con `PATCH /api/v1/monitors/{id}` y el campo `enabled`.
- [x] Editar nombre, endpoint e intervalo con `PUT /api/v1/monitors/{id}`, preservando pausa y estado observado.
- [x] Añadir migración Flyway `V1__create_monitor_schema.sql` para PostgreSQL.
- [x] Aplicar Flyway V1 y verificar la lectura de monitores en PostgreSQL local.
- [x] Aplicar y verificar Flyway V2 de incidencias contra PostgreSQL local.
- [x] Ejecutar comprobaciones HTTP manuales con timeout y guardar último resultado.
- [x] Conservar hasta 100 resultados recientes por monitor y consultarlos ordenados con `GET /api/v1/monitors/{id}/checks`.
- [x] Programar comprobaciones de monitores activos cuando vence su intervalo; revisión cada 5 s (inicio tras 10 s).
- [x] Abrir una incidencia en `DOWN`, evitar duplicados, resolver en `UP` y consultar historial con `GET /api/v1/monitors/{id}/incidents`.
- [x] Enviar notificaciones JSON por webhook al abrir/resolver incidencias; opción desactivada si no hay URL configurada.
- [x] Publicar documentación OpenAPI y Swagger UI para explorar los endpoints.
- [x] Ejecutar build y pruebas automáticamente con GitHub Actions en `push` y `pull_request` a `main`.
- [ ] Añadir reintentos/outbox, adaptar el webhook a un proveedor concreto y desplegar el servicio.
