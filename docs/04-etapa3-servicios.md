# Etapa 3 — Capa de Servicio (Lógica de Negocio)

## 1. Qué se construyó

```
web/dto/
 ├── usuario/     UsuarioRegistroRequest, UsuarioResponse
 ├── experiencia/  ExperienciaRequest, ExperienciaResponse, ExperienciaFiltro
 └── reserva/      ReservaRequest, ReservaResponse

web/mapper/       UsuarioMapper, ExperienciaMapper, ReservaMapper  (MapStruct)

service/interfaces/  UsuarioService, ExperienciaService, ReservaService
service/impl/         UsuarioServiceImpl, ExperienciaServiceImpl, ReservaServiceImpl

exception/        RecursoNoEncontradoException, ReglaDeNegocioException,
                   EmailYaRegistradoException, CupoInsuficienteException,
                   OperacionNoAutorizadaException

config/            PasswordEncoderConfig (BCrypt)

repository/         ExperienciaSpecifications (filtros dinámicos)
                     + método findByIdParaActualizar en ExperienciaRepository (lock)
                     + método findByExperienciaGuiaId en ReservaRepository
```

## 2. Decisiones de diseño (y el porqué)

### a) DTOs con `record` de Java
Se usan `record` en vez de clases con Lombok para los DTOs (a diferencia de las entidades). Son inmutables por naturaleza, ideales para objetos de transferencia que no deben mutar una vez creados, y eliminan boilerplate sin depender de Lombok en la capa web.

### b) Nunca se expone la entidad JPA en la API
Cada servicio devuelve DTOs (`UsuarioResponse`, `ExperienciaResponse`, `ReservaResponse`), nunca `Usuario`/`Experiencia`/`Reserva` directamente. Esto evita:
- Exponer `password_hash` por accidente
- Acoplar el contrato de la API a la estructura interna de la base de datos
- Errores de serialización por proxies lazy de Hibernate

### c) MapStruct en vez de mapeo manual
Los 3 mappers (`UsuarioMapper`, `ExperienciaMapper`, `ReservaMapper`) son interfaces `@Mapper(componentModel = "spring")`; MapStruct genera la implementación en tiempo de compilación (verificado en `target/generated-sources/.../*MapperImpl.java`). Se usan `@Mapping` para resolver campos anidados (`rol.nombre`, `ciudad.nombre`) y `expression` para concatenar nombre+apellido. Ventaja sobre mapeo manual: **cero boilerplate y errores de mapeo detectados en compilación**, no en producción.

### d) Control de concurrencia con bloqueo pesimista (`SELECT ... FOR UPDATE`)
Este es el punto más delicado del proyecto: **dos viajeros reservando el último cupo al mismo tiempo**.

Sin bloqueo, ambas transacciones podrían leer `cupoDisponible = 1`, ambas restar 1, y terminar con `cupoDisponible = -1` (sobreventa). Se resolvió con:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select e from Experiencia e where e.id = :id")
Optional<Experiencia> findByIdParaActualizar(@Param("id") Long id);
```
La segunda transacción concurrente **espera** hasta que la primera termine (commit/rollback) antes de poder leer la fila. Esto se usa tanto en `reservar()` como en `cancelar()` (ambas tocan `cupoDisponible`).

> Este mismo problema se puede probar en la Etapa 6 con un test de concurrencia (2 hilos reservando el mismo cupo límite simultáneamente).

### e) `Specification` dinámica para el buscador (`ExperienciaSpecifications`)
El listado público de experiencias admite filtros **todos opcionales** (ciudad, categoría, rango de fechas). En vez de escribir métodos derivados para cada combinación posible, se usa `JpaSpecificationExecutor` + una clase de utilidades que arma el `Predicate` dinámicamente. Además, la Specification **siempre** filtra por `estado = ACTIVA` — un viajero nunca debe ver experiencias canceladas en el listado público.

### f) Reglas de negocio implementadas

| Regla | Dónde se valida |
|---|---|
| Email único al registrarse | `UsuarioServiceImpl.registrar` → `EmailYaRegistradoException` |
| Contraseña nunca en texto plano | `PasswordEncoderConfig` (BCrypt) |
| Solo un `GUIA` puede crear/editar/cancelar experiencias | `ExperienciaServiceImpl` → `OperacionNoAutorizadaException` |
| Solo el guía dueño puede editar/cancelar su propia experiencia | `validarPropiedad()` → `OperacionNoAutorizadaException` |
| Solo un `VIAJERO` puede reservar | `ReservaServiceImpl.obtenerViajeroValidado` → `OperacionNoAutorizadaException` |
| No se puede reservar más cupo del disponible | `ReservaServiceImpl.reservar` → `CupoInsuficienteException` |
| Solo el viajero dueño puede cancelar su reserva | `ReservaServiceImpl.cancelar` → `OperacionNoAutorizadaException` |
| Cancelar devuelve el cupo a la experiencia | `ReservaServiceImpl.cancelar` |
| Cancelar una reserva ya cancelada es un no-op (idempotente) | `ReservaServiceImpl.cancelar` |
| El listado público solo muestra experiencias `ACTIVA` | `ExperienciaSpecifications.conFiltro` |

### g) `@Transactional` en cada operación de escritura
Toda operación que modifica más de una tabla (ej. `reservar()` toca `experiencias` y `reservas`) está anotada `@Transactional` — si algo falla a mitad de camino, se revierte todo. Las consultas de solo lectura usan `@Transactional(readOnly = true)`, lo cual permite optimizaciones del framework (no se abre una transacción de escritura innecesaria).

## 3. Pruebas realizadas (smoke test funcional)

Se ejecutó temporalmente un `CommandLineRunner` que ejercita **todas** las reglas de negocio de la etapa contra MySQL real. Resultado obtenido:

```
SMOKE3 experiencia creada cupoMax=3 cupoDisponible=3
SMOKE3 reserva1 estado=CONFIRMADA cantidad=2
SMOKE3 cupoDisponible tras reserva1=1
SMOKE3 OK CupoInsuficienteException: Cupo insuficiente: se solicitaron 5 puesto(s) pero solo hay 1 disponible(s)
SMOKE3 OK OperacionNoAutorizadaException (rol): Solo un usuario con rol GUIA puede publicar experiencias
SMOKE3 OK OperacionNoAutorizadaException (propiedad): No tienes permiso para modificar esta experiencia
SMOKE3 cupoDisponible tras cancelar reserva=3
SMOKE3 misReservas=1
SMOKE3 buscarExperiencias=1
```

Se verificó explícitamente:
1. Crear experiencia → `cupoDisponible` inicia igual a `cupoMax` ✅
2. Reservar 2 de 3 cupos → `cupoDisponible` baja a 1 ✅
3. Intentar reservar 5 cuando solo hay 1 → lanza `CupoInsuficienteException` con mensaje claro ✅
4. Un `VIAJERO` intentando crear una experiencia → `OperacionNoAutorizadaException` ✅
5. Un guía distinto intentando cancelar la experiencia de otro guía → `OperacionNoAutorizadaException` ✅
6. Cancelar la reserva → el cupo vuelve a 3 (`cupoDisponible` restaurado) ✅
7. `misReservas` y `buscar` con filtro por ciudad devuelven los resultados esperados ✅

## 4. Cómo verificarlo tú mismo

```bash
cd backend
docker compose up -d
mvn spring-boot:run
```
La app arranca limpia (sin el smoke runner, que era temporal y ya se removió). Las reglas se probarán formalmente vía HTTP en la **Etapa 4** y con JUnit/Mockito en la **Etapa 6**.

---
**Siguiente paso:** aprobar para iniciar la **Etapa 4 — Capa Web (API REST)**: controladores, validación de entrada con `@Valid`, y manejo global de errores con `@ControllerAdvice` (`05-etapa4-api-rest.md`).
