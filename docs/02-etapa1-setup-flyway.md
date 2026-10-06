# Etapa 1 — Setup del Proyecto Spring Boot + Flyway

## 1. Qué se construyó

- Proyecto Maven `caribexperience-backend` con **Spring Boot 3.3.4** y **Java 17** (LTS, la combinación más estable y ampliamente documentada — se evitó Spring Boot 4.x por ser demasiado reciente para un proyecto de aprendizaje).
- Estructura de paquetes en **arquitectura por capas**, ya con todos los directorios listos para las siguientes etapas:
  ```
  com.caribexperience
   ├── config
   ├── domain
   ├── repository
   ├── service/interfaces
   ├── service/impl
   ├── web/controller
   ├── web/dto
   ├── web/mapper
   ├── security
   ├── exception
   └── common
  ```
- Configuración externa (`application.yml`) siguiendo **12-factor app**: nada de credenciales hardcodeadas, todo vía variables de entorno con valores por defecto para desarrollo local.
- **6 migraciones Flyway** (V1–V6) que implementan exactamente el modelo de la Etapa 0.
- `docker-compose.yml` para levantar MySQL localmente sin instalarlo en tu máquina.

## 2. Dependencias clave del `pom.xml`

| Dependencia | Para qué |
|---|---|
| `spring-boot-starter-web` | API REST |
| `spring-boot-starter-data-jpa` | Persistencia (Etapa 2) |
| `mysql-connector-j` | Driver JDBC de MySQL |
| `flyway-core` + `flyway-mysql` | Migraciones versionadas |
| `spring-boot-starter-validation` | Validación de DTOs (`@Valid`, `@NotNull`, etc.) |
| `spring-boot-starter-security` + `jjwt-*` | Autenticación JWT (Etapa 5) |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI automático (Etapa 7) |
| `mapstruct` | Mapeo Entidad ↔ DTO sin boilerplate (Etapa 3) |
| `lombok` | Reduce código repetitivo (getters/setters/constructores) |
| `spring-boot-starter-actuator` | Expone `/actuator/health` — **crítico para el health check del ALB en AWS** |
| `testcontainers` (mysql + junit-jupiter) | Tests de integración contra MySQL real, no un mock (Etapa 6) |

## 3. `application.yml` — decisiones de diseño

- **Toda credencial es una variable de entorno** (`${DB_URL:...}`, `${DB_PASSWORD:...}`) con default solo para desarrollo local. En AWS, estas variables se inyectarán vía Secrets Manager / variables de entorno del servicio, nunca se sube nada sensible al repo.
- **`ddl-auto: validate`**: Hibernate **nunca** crea ni modifica tablas. Flyway es la única fuente de verdad del esquema. Esto es una práctica profesional obligatoria — evita divergencias entre entornos.
- **`open-in-view: false`**: evita el anti-patrón "Open Session In View", que causa problemas de rendimiento y errores lazy-loading ocultos en producción.
- **HikariCP con pool pequeño (10 conexiones)**: pensado para cuando el Auto Scaling Group tenga varias instancias EC2 apuntando a la misma RDS — evita agotar las conexiones disponibles de la base de datos.
- **`forward-headers-strategy: framework`**: necesario porque en AWS la app corre detrás de un Application Load Balancer; sin esto, Spring no reconoce correctamente el protocolo/IP original del cliente.
- **Actuator con `health.probes.enabled`**: expone `/actuator/health/liveness` y `/actuator/health/readiness`, que se usarán como **health check target del ALB** y para el Auto Scaling Group (reemplaza instancias que fallan el check).

## 4. Migraciones Flyway aplicadas

| Versión | Descripción |
|---|---|
| V1 | Tablas catálogo: `roles`, `ciudades`, `categorias`, `estados_experiencia`, `estados_reserva` |
| V2 | Datos semilla de esos catálogos (incluye 7 ciudades de la Costa Caribe) |
| V3 | Tabla `usuarios` con FK a `roles` |
| V4 | Tabla `experiencias` con FKs a `usuarios`, `ciudades`, `categorias`, `estados_experiencia` |
| V5 | Tabla `reservas` con FKs a `experiencias`, `usuarios`, `estados_reserva` |
| V6 | Índices adicionales para patrones de consulta frecuentes |

**Regla Flyway respetada:** una vez que una migración `V*` se ejecuta, nunca se edita. Cualquier corrección futura se hace en una nueva versión (`V7__...`).

## 5. Verificación realizada (evidencia de que funciona)

1. `docker compose up -d` → MySQL 8.0 corriendo en `localhost:3306`.
2. `mvn compile` → compila sin errores.
3. `mvn spring-boot:run` → la app arranca, Flyway ejecuta automáticamente las 6 migraciones al boot:
   ```
   Migrating schema to version "1 - create catalog tables"
   ...
   Successfully applied 6 migrations to schema `caribexperience`, now at version v6
   Started CaribeXperienceApplication in 3.056 seconds
   ```
4. `curl http://localhost:8080/actuator/health` → `{"status":"UP", "groups":["liveness","readiness"]}`
5. Verificado en MySQL directamente: las 9 tablas existen (incluye `flyway_schema_history`) y los datos semilla están cargados (3 roles, 7 ciudades, etc.).

## 6. Cómo correrlo tú mismo

```bash
cd backend
docker compose up -d          # levanta MySQL
mvn spring-boot:run           # arranca la app (aplica migraciones automáticamente)
curl http://localhost:8080/actuator/health
```

Para detener todo:
```bash
docker compose down           # borra también el volumen si agregas -v
```

## 7. Nota de seguridad temporal

Al arrancar verás en el log una contraseña generada automáticamente por Spring Security (`Using generated security password: ...`). Es normal — Spring Security se auto-configura por defecto hasta que implementemos nuestra propia configuración en la **Etapa 5**. No se debe usar en producción.

---
**Siguiente paso:** aprobar para iniciar la **Etapa 2 — Capa de dominio (entidades JPA) y repositorios** (`03-etapa2-dominio-repositorios.md`).
