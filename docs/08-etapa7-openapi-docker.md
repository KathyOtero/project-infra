# Etapa 7 — Documentación OpenAPI/Swagger y Empaquetado con Docker

## Objetivo

Cerrar el ciclo de vida del backend para esta fase del proyecto:
1. Exponer documentación interactiva de la API (OpenAPI/Swagger) para que cualquier
   integrante del equipo (o el frontend en React) pueda explorar y probar los endpoints
   sin necesidad de Postman ni de leer el código.
2. Empaquetar el backend en una imagen Docker reproducible y levantar todo el stack
   (backend + MySQL) con un solo comando (`docker compose up`), preparando el terreno
   para el despliegue en AWS de la Etapa 13.

## 1. OpenAPI / Swagger

La dependencia `springdoc-openapi-starter-webmvc-ui` ya estaba en el `pom.xml` desde el
inicio del proyecto (Etapa 1), pero no tenía configuración explícita. Se agregó:

**`config/OpenApiConfig.java`**
- Metadatos de la API (título, descripción, versión, contacto del grupo).
- Esquema de seguridad `bearerAuth` (HTTP Bearer, formato JWT), lo que agrega el botón
  **"Authorize"** en la UI de Swagger: se pega el token obtenido en
  `POST /api/auth/login` y automáticamente se envía en el header `Authorization` de
  todas las peticiones que se prueben desde ahí.

Las rutas `/swagger-ui/**` y `/v3/api-docs/**` ya estaban permitidas (`permitAll`) desde
la configuración de seguridad de la Etapa 5, así que no fue necesario modificar
`SecurityConfig`.

**URLs expuestas:**
| Recurso | URL |
|---|---|
| UI interactiva | `http://localhost:8080/swagger-ui/index.html` |
| Contrato OpenAPI (JSON) | `http://localhost:8080/v3/api-docs` |

Verificado: 12 rutas documentadas automáticamente a partir de las anotaciones de Spring
MVC (`@RestController`, `@RequestMapping`, DTOs con Bean Validation), sin necesidad de
anotar manualmente cada endpoint con `@Operation`/`@ApiResponse` (aceptable para el
alcance de este proyecto académico; en un proyecto productivo se enriquecería con
esas anotaciones).

## 2. Dockerfile (multi-stage build)

Se creó `backend/Dockerfile` con dos etapas:

1. **Etapa `build`** (`maven:3.9-eclipse-temurin-17`): compila el proyecto con Maven
   dentro del propio contenedor, sin depender de tener Maven instalado en la máquina
   que construye la imagen (importante para reproducibilidad en CI/CD).
   - Se copia primero solo `pom.xml` y se ejecuta `dependency:go-offline` antes de
     copiar el código fuente, aprovechando el cache de capas de Docker: si las
     dependencias no cambian, esa capa no se reconstruye aunque cambie el código.
2. **Etapa `runtime`** (`eclipse-temurin:17-jre-jammy`): imagen final mínima, solo con
   el JRE (no el JDK) y el `.jar` ya compilado copiado desde la etapa `build`
   (`COPY --from=build`). Se usa la variante `jammy` (Ubuntu) en vez de `alpine` porque
   tiene mejor soporte multi-arquitectura (relevante porque el equipo desarrolla en
   Mac Apple Silicon/ARM64 y el despliegue en AWS EC2 será en `amd64`).

**Buenas prácticas aplicadas:**
- Usuario no-root (`caribe`) dentro del contenedor.
- `HEALTHCHECK` nativo de Docker apuntando a `/actuator/health`, compatible con el
  Load Balancer/orquestador que se usará en la Etapa 13 de alta disponibilidad en AWS.
- `.dockerignore` para no copiar `target/`, `.git/`, `docs/`, etc. al contexto de build.

## 3. docker-compose.yml

Se extendió el `docker-compose.yml` (que ya existía desde la Etapa 1 solo con MySQL)
agregando el servicio `backend`:

- `depends_on: mysql: condition: service_healthy` — el backend solo arranca cuando
  MySQL ya respondió su `healthcheck` (evita fallos de conexión en el arranque).
- Variables de entorno inyectadas: `DB_URL` apunta al hostname `mysql` (nombre del
  servicio en la red interna de docker-compose, no `localhost`), `JWT_SECRET` y
  `CORS_ALLOWED_ORIGINS` parametrizables vía variables de entorno del host con
  valores por defecto (`${JWT_SECRET:-...}`).
- Red dedicada `caribexperience_net` para aislar la comunicación interna
  backend↔mysql.
- Perfil activo `docker` (`SPRING_PROFILES_ACTIVE=docker`) para diferenciarlo del
  perfil `local` usado en desarrollo con IDE.

## 4. Verificación end-to-end

```bash
cd backend
docker compose up -d --build
```

Resultado:
- `caribexperience-mysql` → `healthy`
- `caribexperience-backend` → `healthy`
- Flyway aplicó las 6 migraciones automáticamente sobre el volumen limpio de MySQL al
  arrancar el contenedor (log confirmado: `Successfully applied 6 migrations ... now at
  version v6`).
- `GET /actuator/health` → `{"status":"UP"}`
- `GET /v3/api-docs` → 12 endpoints documentados, título/versión correctos.
- `GET /swagger-ui/index.html` → `200 OK`.
- `GET /api/experiencias` (público, sin token) → `200 OK`.
- Flujo completo de autenticación probado dentro del contenedor:
  registro (`POST /api/usuarios/registro`) → login (`POST /api/auth/login`, token JWT
  obtenido) → `GET /api/usuarios/{id}` con token → `200 OK` → mismo endpoint sin
  token → `401 Unauthorized`.
- Datos de prueba limpiados después de la verificación.

## Comandos de referencia para el equipo

```bash
# Levantar todo el stack (build + start)
docker compose up -d --build

# Ver logs del backend
docker compose logs -f backend

# Apagar todo (conserva el volumen de datos de MySQL)
docker compose down

# Apagar y borrar también los datos de MySQL
docker compose down -v
```

## Próximo paso

Con el backend completo, probado, documentado y contenedorizado, el proyecto pasa a la
etapa de **frontend en React** (Etapa 8 en adelante, según `docs/00-plan-general.md`):
inicialización del proyecto, cliente HTTP (Axios), pantallas de login/registro, listado
y detalle de experiencias, flujo de reserva, y panel del guía.
