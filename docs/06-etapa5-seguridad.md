# Etapa 5 — Seguridad (JWT + Roles)

## 1. Qué se construyó

```
security/
 ├── UsuarioPrincipal.java            Adaptador Usuario -> UserDetails
 ├── CustomUserDetailsService.java    Carga el usuario por email para Spring Security
 ├── JwtService.java                  Generacion y validacion de tokens (jjwt)
 ├── JwtAuthenticationFilter.java     Filtro que valida el header Authorization en cada request
 ├── RestAuthenticationEntryPoint.java 401 JSON cuando falta autenticacion
 ├── RestAccessDeniedHandler.java     403 JSON cuando el rol no alcanza
 └── SecurityConfig.java              Filter chain, reglas por endpoint, CORS, @EnableMethodSecurity

web/dto/auth/
 ├── LoginRequest.java
 └── LoginResponse.java

web/controller/
 └── AuthController.java              POST /api/auth/login
```

Se eliminó `config/SecurityConfig.java` (el placeholder `permitAll` de la Etapa 4).

## 2. Flujo de autenticación

```
1. POST /api/usuarios/registro  → crea el usuario (rol GUIA o VIAJERO), password hasheado con BCrypt (ya existía desde Etapa 3)
2. POST /api/auth/login          → AuthenticationManager valida email+password vía CustomUserDetailsService + PasswordEncoder
                                  → si son correctos, JwtService firma un JWT (HMAC-SHA, expira en 24h) con claims: sub=email, id, rol, nombre
3. Cliente guarda el token y lo envía en cada request: Authorization: Bearer <token>
4. JwtAuthenticationFilter (una vez por request) valida la firma/expiración, reconstruye el UsuarioPrincipal
   y lo coloca en el SecurityContext antes de llegar al controlador
5. @PreAuthorize("hasRole('GUIA')") / hasRole('VIAJERO') en la capa de servicio verifica el rol
6. Los controladores obtienen el usuario autenticado con @AuthenticationPrincipal UsuarioPrincipal — ya NO existen
   los query params guiaId/viajeroId de la Etapa 4.
```

## 3. Decisiones de diseño (y el porqué)

### a) `UsuarioPrincipal` envuelve `Usuario` (no lo implementa directamente)
La entidad de dominio `Usuario` (capa de persistencia) no debe saber nada de Spring Security (acoplamiento indebido entre capas). `UsuarioPrincipal` es un adaptador construido a partir de `Usuario`, que además expone el rol como `ROLE_<NOMBRE>` (convención que exige Spring Security para que `hasRole("GUIA")` funcione).

### b) `@Transactional(readOnly = true)` en `CustomUserDetailsService.loadUserByUsername`
**Bug real encontrado y corregido durante las pruebas**: `Usuario.rol` es `@ManyToOne(FetchType.LAZY)` y `open-in-view: false` (buena práctica desde la Etapa 1). Sin una transacción abierta, `usuario.getRol().getNombre()` (llamado dentro del constructor de `UsuarioPrincipal`) lanzaba `LazyInitializationException`, envuelta por Spring Security en `InternalAuthenticationServiceException` → login fallaba con 401 incluso con credenciales correctas. Se corrigió añadiendo `@Transactional(readOnly = true)` al método, manteniendo la sesión de Hibernate abierta mientras se construye el principal.

### c) `JwtService` con `jjwt` 0.12.6 (API moderna, no deprecada)
Se usa la API `Jwts.builder()...claims()...signWith(key)` y `Jwts.parser().verifyWith(key)` (no la API antigua `setSigningKey`/`parseClaimsJws`, deprecada desde jjwt 0.12). La clave HMAC se deriva de `app.jwt.secret` (ya definido en `application.yml` desde la Etapa 1, pensando en esta etapa).

### d) `JwtAuthenticationFilter extends OncePerRequestFilter`
Garantiza ejecución exactamente una vez por request (importante con forwards/includes internos de Servlet). Si el token es inválido/expirado, **no propaga la excepción** hacia el `GlobalExceptionHandler` (los filtros corren antes de que Spring MVC entre en acción, el `@RestControllerAdvice` no aplica ahí) — en su lugar escribe directamente un JSON 401 con el mismo formato `ErrorResponse` del resto de la API, para que el frontend maneje errores de forma consistente sin importar en qué capa ocurrieron.

### e) `RestAuthenticationEntryPoint` vs `RestAccessDeniedHandler` — la distinción 401 vs 403
Es un error común confundir estos dos casos. Se implementaron ambos explícitamente:
- **401 Unauthorized** (`AuthenticationEntryPoint`): el request **no** trae un JWT válido — "no sé quién eres".
- **403 Forbidden** (`AccessDeniedHandler`): el request **sí** trae un JWT válido, pero el rol del usuario no cumple el `@PreAuthorize` — "sé quién eres, pero no puedes hacer esto".

Sin estos beans, Spring Security devuelve por defecto una redirección a una página de login HTML, rompiendo el contrato JSON de la API.

### f) Autorización en dos capas (defensa en profundidad)
1. `SecurityConfig.authorizeHttpRequests`: reglas gruesas por ruta HTTP (público vs autenticado).
2. `@PreAuthorize("hasRole('GUIA')")` / `hasRole('VIAJERO')` en los métodos de servicio (`ExperienciaServiceImpl`, `ReservaServiceImpl`): autorización fina por rol de negocio.
3. Validación de **propiedad** (ownership) ya existente desde la Etapa 3 (`validarPropiedad` en `ExperienciaServiceImpl`, chequeo de `viajero.getId()` en `ReservaServiceImpl.cancelar`): un GUIA no puede editar la experiencia de otro GUIA; un VIAJERO no puede cancelar la reserva de otro VIAJERO. Esto **no se puede resolver únicamente con roles** — requiere lógica de negocio, por eso vive en el servicio y no en `SecurityConfig`.

`@EnableMethodSecurity` habilita el procesamiento de `@PreAuthorize` a nivel de método.

### g) Reglas públicas explícitas en `SecurityConfig`
```java
"/api/auth/**"                                    → login público (obviamente)
POST "/api/usuarios/registro"                     → registro público
"/api/catalogos/**"                                → ciudades/categorías, sin necesidad de sesión
GET "/api/experiencias", "/api/experiencias/{id}"  → catálogo de experiencias navegable sin login (como Airbnb)
"/actuator/**"                                      → health checks del futuro Load Balancer (Etapa 13, AWS)
"/swagger-ui/**", "/v3/api-docs/**"                 → documentación OpenAPI (Etapa 7)
anyRequest()                                        → autenticado
```

### h) Sesión `STATELESS`
`SessionCreationPolicy.STATELESS`: el servidor no crea ni usa `HttpSession`. Cada request se autentica de forma independiente mediante su propio JWT. Es un requisito de diseño para la arquitectura de Alta Disponibilidad del proyecto (Etapa 13): con múltiples instancias EC2 detrás de un Load Balancer, no se puede depender de sesión pegajosa (sticky session) ni de replicación de sesión entre nodos.

### i) CORS configurable por variable de entorno
`CorsConfigurationSource` lee `app.cors.allowed-origins` (ya definido en `application.yml`, con default `http://localhost:5173` para el futuro Vite/React). En producción se cambia con la variable de entorno `CORS_ALLOWED_ORIGINS` sin tocar código.

### j) Cambios en los controladores
- `ExperienciaController` / `ReservaController`: los parámetros `guiaId`/`viajeroId` (query param, Etapa 4) se eliminaron por completo. Ahora se usa `@AuthenticationPrincipal UsuarioPrincipal principal` y `principal.getId()`.
- Se renombraron las rutas de "propio recurso" para que no dependan de un id en el path que el propio usuario ya no necesita pasar:
  - `GET /api/experiencias/guia/{guiaId}` → `GET /api/experiencias/mias`
  - `GET /api/reservas/viajero/{viajeroId}` → `GET /api/reservas/mias`
  - `GET /api/reservas/guia/{guiaId}` → `GET /api/reservas/recibidas`

## 4. Endpoints actualizados

| Método | Endpoint | Auth requerida | Rol |
|---|---|---|---|
| POST | `/api/auth/login` | No | — |
| POST | `/api/usuarios/registro` | No | — |
| GET | `/api/usuarios/{id}` | Sí | cualquiera |
| GET | `/api/catalogos/**` | No | — |
| GET | `/api/experiencias`, `/api/experiencias/{id}` | No | — |
| POST/PUT/DELETE | `/api/experiencias/**` | Sí | GUIA (+ dueño) |
| GET | `/api/experiencias/mias` | Sí | GUIA |
| POST | `/api/reservas` | Sí | VIAJERO |
| DELETE | `/api/reservas/{id}` | Sí | VIAJERO (+ dueño) |
| GET | `/api/reservas/mias` | Sí | VIAJERO |
| GET | `/api/reservas/recibidas` | Sí | GUIA |

## 5. Pruebas realizadas (end-to-end contra MySQL real)

```
✓ Registrar GUIA y VIAJERO                                    -> 201
✓ Login con credenciales correctas                              -> 200 con JWT
✓ Login con password incorrecto                                 -> 401
✓ Crear experiencia sin token                                    -> 401 "Se requiere autenticacion..."
✓ Crear experiencia con token de VIAJERO (rol incorrecto)        -> 403 "No tienes permisos..."
✓ Crear experiencia con token de GUIA (correcto)                 -> 201
✓ Token invalido/manipulado                                       -> 401 "Token invalido o expirado"
✓ Listado publico de experiencias sin token                      -> 200 (endpoint publico funciona)
✓ Reservar con token de GUIA (rol incorrecto)                    -> 403
✓ Reservar con token de VIAJERO (correcto)                       -> 201
✓ Sobrereservar (cupo insuficiente)                              -> 409 (regla de negocio sigue intacta)
✓ Mis reservas / Reservas recibidas (con @AuthenticationPrincipal) -> 200
✓ Viajero2 intenta cancelar la reserva de Viajero1 (ownership)   -> 403 "No tienes permiso para cancelar esta reserva"
✓ Viajero1 cancela su propia reserva                             -> 204
✓ Cupo restaurado correctamente tras cancelar                    -> 3/3
```

Durante las pruebas se encontró y corrigió el bug de `LazyInitializationException` descrito en el punto 3.b — quedó resuelto y verificado con una segunda ronda completa de pruebas.

## 6. Cómo probarlo tú mismo

```bash
cd backend
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

```bash
# 1. Registro
curl -X POST http://localhost:8080/api/usuarios/registro -H "Content-Type: application/json" \
  -d '{"nombre":"Carlos","apellido":"Perez","email":"guia1@test.co","password":"password123","rol":"GUIA"}'

# 2. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"guia1@test.co","password":"password123"}' | python3 -c "import json,sys; print(json.load(sys.stdin)['token'])")

# 3. Endpoint protegido
curl -X POST http://localhost:8080/api/experiencias -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"ciudadId":1,"categoriaId":1,"titulo":"Tour","descripcion":"Recorrido","precio":50,"cupoMax":3,"fecha":"2026-12-01","hora":"10:00:00"}'
```

---
**Siguiente paso:** aprobar para iniciar la **Etapa 6 — Testing** (unit tests con Mockito para la capa de servicio + tests de integración con Testcontainers para repositorios y controladores; en tu máquina local, sin la limitación de Docker del sandbox), documentada en `07-etapa6-testing.md`.
