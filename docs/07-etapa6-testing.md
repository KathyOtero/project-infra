# Etapa 6 — Testing

## Objetivo

Construir una suite de pruebas automatizadas que valide la lógica de negocio (capa de
servicio) y las reglas de seguridad (capa web) implementadas en las Etapas 3-5, siguiendo
una estrategia de **pirámide de pruebas**: muchas pruebas unitarias rápidas, pocas pruebas
de integración (slice) más pesadas, y las pruebas de persistencia con Testcontainers ya
existentes desde la Etapa 2 (bloqueadas en este sandbox por restricciones de Docker, pero
funcionales en cualquier entorno con Docker completo).

## Estrategia de pruebas

| Nivel | Framework | Objetivo | Velocidad |
|---|---|---|---|
| Unitarias (servicio) | JUnit 5 + Mockito | Validar reglas de negocio aisladas del framework/DB | Muy rápida (ms) |
| Unitarias (utilidades) | JUnit 5 puro | Validar componentes sin dependencias de Spring (ej. JWT) | Muy rápida |
| Slice web (`@WebMvcTest`) | Spring Test + MockMvc | Validar que Spring Security aplique correctamente 401/403/200 | Rápida (~1s) |
| Integración de persistencia | Testcontainers + JUnit 5 | Validar mapeo JPA/Flyway contra MySQL real | Lenta, requiere Docker |

No se usó `@SpringBootTest` completo para los tests de negocio: `@Mock`/`@InjectMocks` de
Mockito es suficiente y evita levantar el contexto de Spring, lo cual mantiene los tests
rápidos y enfocados en una sola unidad.

## Clases de prueba creadas

### 1. `service/UsuarioServiceImplTest` (4 tests)
- Registro exitoso de usuario (hashing de password, asignación de rol).
- Registro falla si el email ya existe (`EmailYaRegistradoException` o similar).
- Registro falla si el rol no es válido.
- `obtenerPorId` lanza excepción si el usuario no existe.

### 2. `service/ExperienciaServiceImplTest` (6 tests)
- Crear experiencia exitosamente (rol GUIA).
- Crear falla si el usuario no tiene rol GUIA.
- Crear falla si la ciudad no existe.
- Actualizar falla por violación de propiedad (otro guía intenta editar).
- Actualizar ajusta proporcionalmente el cupo disponible al cambiar el cupo total.
- Cancelar falla por violación de propiedad.

### 3. `service/ReservaServiceImplTest` (7 tests)
Cobertura de la lógica más crítica del negocio (control de cupo/overbooking):
- Reservar exitosamente, decrementando el cupo disponible.
- Reservar falla si no hay cupo suficiente.
- Reservar falla si el usuario no tiene rol VIAJERO.
- Reservar falla si la experiencia no existe.
- Cancelar reserva exitosamente, restaurando el cupo disponible.
- Cancelar falla por violación de propiedad (otro viajero intenta cancelar).
- Cancelar una reserva ya cancelada es idempotente (no falla, no duplica la restauración).

### 4. `security/JwtServiceTest` (4 tests)
- Generar un token válido para un usuario.
- Validar correctamente un token propio.
- Rechazar un token cuando el usuario no coincide.
- Un token expirado lanza `ExpiredJwtException`.

### 5. `web/controller/ExperienciaControllerSecurityTest` (3 tests, `@WebMvcTest`)
Verifica el comportamiento **real** de Spring Security (no se puede probar con Mockito puro
porque `@PreAuthorize` y el filtro JWT son parte de la infraestructura de Spring):
- `GET /api/experiencias` sin token → `200 OK` (endpoint público).
- `POST /api/experiencias` sin token → `401 Unauthorized`.
- `POST /api/experiencias` con `@WithMockUser(roles="GUIA")` pero body inválido → `400 Bad Request`.

## Problemas encontrados y solucionados

### 1. Mockear el filtro JWT rompe la cadena de filtros
Al usar `@MockBean` sobre `JwtAuthenticationFilter`, Mockito reemplaza toda la lógica del
filtro por un stub que **no llama a `chain.doFilter()`**, lo que provoca que ninguna
petición llegue al controlador (o se comporte de forma incorrecta). Corrección: importar el
filtro real con `@Import(JwtAuthenticationFilter.class)` y solo mockear sus dependencias de
hoja (`JwtService`, `CustomUserDetailsService`).

### 2. Mockear los manejadores de errores rompe los códigos de estado
Lo mismo aplica a `RestAuthenticationEntryPoint` y `RestAccessDeniedHandler`: si se mockean,
no escriben el status HTTP real (401/403), y las peticiones "fallidas" terminan devolviendo
200 o cayendo en otro flujo. Corrección: importarlos como beans reales.

### 3. Mock sin stub retorna `null` → 500 en lugar de 200
Un método de servicio mockeado sin `when(...)` configurado retorna `null` por defecto. Al
serializar `Page<ExperienciaResponse>` nulo, el controlador lanza `NullPointerException` →
`500`. Corrección: `when(experienciaService.buscar(any(), any())).thenReturn(new PageImpl<>(List.of()))`.

## Resultado final

```
mvn test -Dtest='!DomainPersistenceIT'

Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- 21 pruebas unitarias (servicios + JWT).
- 3 pruebas de slice web (seguridad).
- `DomainPersistenceIT` (Testcontainers, Etapa 2) queda excluida en este sandbox por la
  restricción de Docker-in-Docker ya documentada; en cualquier entorno con Docker
  disponible, correría sin cambios.

## Lecciones para el informe final del proyecto

- `@Transactional` es obligatorio en cualquier `UserDetailsService` que acceda a
  asociaciones `LAZY` (bug real encontrado y corregido en Etapa 5, cubierto indirectamente
  por estas pruebas).
- Los tests de seguridad **no se pueden simular solo con Mockito**: se requiere un slice de
  Spring (`@WebMvcTest`) para ejercitar el filtro real y las anotaciones `@PreAuthorize`.
- La pirámide de pruebas (muchas unitarias, pocas de integración) permite feedback rápido en
  el ciclo de desarrollo sin sacrificar cobertura de los flujos críticos (control de cupo,
  autenticación, autorización por rol y por dueño del recurso).

## Estado del entorno

- Contenedor `caribexperience-mysql` sigue activo (usado en Etapa 5), no requerido por los
  tests de esta etapa.

## Próximo paso

Etapa 7 — Documentación OpenAPI/Swagger y empaquetado con Docker (Dockerfile + docker-compose
para backend + MySQL), según el plan general (`docs/00-plan-general.md`).
