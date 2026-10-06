# Etapa 4 — Capa Web (API REST)

## 1. Qué se construyó

```
web/controller/
 ├── UsuarioController.java       POST /api/usuarios/registro, GET /api/usuarios/{id}
 ├── ExperienciaController.java   CRUD + búsqueda de experiencias
 ├── ReservaController.java       Reservar, cancelar, mis reservas, reservas recibidas
 └── CatalogoController.java      GET /api/catalogos/ciudades, /categorias

web/dto/common/
 └── PaginaResponse.java          Envoltorio de paginación explícito

exception/
 ├── ErrorResponse.java           Estructura uniforme de error
 └── GlobalExceptionHandler.java  @RestControllerAdvice centralizado

config/
 └── SecurityConfig.java          Placeholder temporal (permitAll) — se reemplaza en Etapa 5
```

## 2. Endpoints expuestos

| Método | Endpoint | Descripción | Body/Params |
|---|---|---|---|
| POST | `/api/usuarios/registro` | Registrar usuario | `UsuarioRegistroRequest` |
| GET | `/api/usuarios/{id}` | Obtener usuario | — |
| POST | `/api/experiencias?guiaId=` | Crear experiencia | `ExperienciaRequest` |
| PUT | `/api/experiencias/{id}?guiaId=` | Actualizar experiencia | `ExperienciaRequest` |
| DELETE | `/api/experiencias/{id}?guiaId=` | Cancelar experiencia | — |
| GET | `/api/experiencias/{id}` | Ver detalle | — |
| GET | `/api/experiencias?ciudadId=&categoriaId=&fechaDesde=&fechaHasta=&page=&size=` | Listado público con filtros | — |
| GET | `/api/experiencias/guia/{guiaId}` | Mis experiencias (guía) | — |
| POST | `/api/reservas?viajeroId=` | Reservar | `ReservaRequest` |
| DELETE | `/api/reservas/{id}?viajeroId=` | Cancelar reserva | — |
| GET | `/api/reservas/viajero/{viajeroId}` | Mis reservas | — |
| GET | `/api/reservas/guia/{guiaId}` | Reservas recibidas | — |
| GET | `/api/catalogos/ciudades` | Catálogo de ciudades | — |
| GET | `/api/catalogos/categorias` | Catálogo de categorías | — |

## 3. Decisiones de diseño (y el porqué)

### a) `guiaId` / `viajeroId` como query param — **temporal, con nota explícita**
Todavía no existe autenticación (JWT llega en la Etapa 5). Se documentó explícitamente en el Javadoc de cada controlador que esto es un **placeholder inseguro**: cualquiera podría suplantar a otro usuario cambiando el query param. En la Etapa 5, estos parámetros se eliminan por completo y el usuario se obtiene del `SecurityContext` (token JWT validado), no de un parámetro que el cliente controla.

### b) `SecurityConfig` temporal con `permitAll()`
Spring Security se auto-configura apenas se agrega la dependencia (ya estaba en el `pom.xml` desde la Etapa 1, pensando en la Etapa 5). Sin una configuración propia, **bloquea todos los endpoints** con Basic Auth y una contraseña aleatoria en cada arranque — impidiendo probar la API que acabamos de construir. Se agregó una clase `SecurityConfig` mínima que abre todo (`permitAll`), con un comentario bien visible de que **se reemplaza por completo** en la Etapa 5.

### c) `PaginaResponse<T>` en vez de exponer `Page<T>` directo
Spring Data `Page` expone detalles internos (`Pageable`, `Sort`) que generan advertencias de Spring Boot si se exponen tal cual en una API pública. Se envuelve en un DTO propio con solo lo que el frontend necesita: `contenido`, `paginaActual`, `totalPaginas`, `totalElementos`, `esUltimaPagina`.

### d) `GlobalExceptionHandler` (`@RestControllerAdvice`)
Ningún controlador tiene `try/catch`. Todas las excepciones de negocio (de la Etapa 3) se traducen centralmente a códigos HTTP:

| Excepción | HTTP |
|---|---|
| `RecursoNoEncontradoException` | 404 Not Found |
| `OperacionNoAutorizadaException` | 403 Forbidden |
| `ReglaDeNegocioException` (y subclases: `EmailYaRegistradoException`, `CupoInsuficienteException`) | 409 Conflict |
| `MethodArgumentNotValidException` (`@Valid` fallido) | 400 Bad Request, con mapa `campo → mensaje` |
| `HttpMessageNotReadableException` (JSON malformado) | 400 Bad Request |
| Cualquier otra excepción no prevista | 500, **sin exponer el mensaje interno** (se registra en logs, no se filtra al cliente) |

### e) Validación de entrada con Bean Validation (`@Valid`)
Todos los `@RequestBody` usan las anotaciones ya definidas en los DTOs de la Etapa 3 (`@NotBlank`, `@Email`, `@Positive`, `@FutureOrPresent`, etc.). Spring las ejecuta automáticamente antes de llegar al controlador.

### f) `CatalogoController` sin capa de servicio
Ciudades/categorías son proyecciones de solo lectura sin ninguna regla de negocio — acceder directo al repositorio desde el controlador es aceptable aquí (evita una capa de servicio vacía). Si en el futuro se agrega lógica (ej. solo mostrar ciudades con experiencias activas), se movería a un `CatalogoService`.

## 4. Pruebas realizadas (end-to-end contra MySQL real)

Se probaron **14 escenarios** con `curl` contra la API completa:

```
✓ Registrar GUIA                                    -> 201 con UsuarioResponse
✓ Registrar VIAJERO                                 -> 201
✓ Email duplicado                                    -> 409 Conflict
✓ Rol inválido en registro                           -> 400 con {"rol":"El rol debe ser GUIA o VIAJERO"}
✓ Catálogos (ciudades, categorías)                   -> 200 con las 7 ciudades y 6 categorías sembradas
✓ Crear experiencia (guía)                           -> 201, cupoMax=3 cupoDisponible=3
✓ Buscar experiencias (listado público, filtro ciudad) -> 200, PaginaResponse con 1 resultado
✓ Reservar 2 cupos                                    -> 201 estado=CONFIRMADA
✓ Sobrereservar (pedir 5 cuando solo hay 1)           -> 409 "Cupo insuficiente..."
✓ Viajero intenta crear experiencia                   -> 403 "Solo un usuario con rol GUIA..."
✓ Mis reservas (viajero)                              -> 200 con la reserva
✓ Reservas recibidas (guía)                           -> 200 con la misma reserva
✓ Cancelar reserva                                    -> 204 No Content
✓ Cupo restaurado tras cancelar                       -> cupoDisponible vuelve a 3
```

Todos los escenarios se comportaron exactamente como lo definieron las reglas de negocio de la Etapa 3, ahora expuestas correctamente vía HTTP con los códigos de estado y mensajes de error apropiados.

## 5. Cómo probarlo tú mismo

```bash
cd backend
docker compose up -d
mvn spring-boot:run
```

Ejemplo rápido:
```bash
curl -X POST http://localhost:8080/api/usuarios/registro \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Carlos","apellido":"Perez","email":"guia1@test.co","password":"password123","rol":"GUIA"}'
```

---
**Siguiente paso:** aprobar para iniciar la **Etapa 5 — Seguridad (JWT + roles)**: login, filtro JWT, `@PreAuthorize` por rol, y **eliminar los query params `guiaId`/`viajeroId`** reemplazándolos por el usuario autenticado (`06-etapa5-seguridad.md`).
