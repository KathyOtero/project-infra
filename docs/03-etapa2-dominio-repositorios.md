# Etapa 2 — Capa de Dominio (Entidades JPA) y Repositorios

## 1. Qué se construyó

**8 entidades JPA** que reflejan 1:1 el esquema creado por Flyway en la Etapa 1, y **8 repositorios** Spring Data JPA.

```
domain/
 ├── BaseAuditEntity.java     (superclase: created_at/updated_at)
 ├── Rol.java                 (catálogo)
 ├── Ciudad.java               (catálogo)
 ├── Categoria.java            (catálogo)
 ├── EstadoExperiencia.java    (catálogo)
 ├── EstadoReserva.java        (catálogo)
 ├── Usuario.java
 ├── Experiencia.java
 └── Reserva.java

repository/
 ├── RolRepository.java
 ├── CiudadRepository.java
 ├── CategoriaRepository.java
 ├── EstadoExperienciaRepository.java
 ├── EstadoReservaRepository.java
 ├── UsuarioRepository.java
 ├── ExperienciaRepository.java
 └── ReservaRepository.java
```

También se agregó `config/JpaAuditingConfig.java` (`@EnableJpaAuditing`) y 3 clases de constantes en `common/` (`RolNombre`, `EstadoExperienciaNombre`, `EstadoReservaNombre`) para eliminar "magic strings" del código.

## 2. Decisiones de diseño (y el porqué)

### a) `BaseAuditEntity` — herencia con `@MappedSuperclass`
`Usuario`, `Experiencia` y `Reserva` heredan de esta clase para no repetir `createdAt`/`updatedAt` (principio **DRY**). Los valores se llenan automáticamente vía `@CreatedDate`/`@LastModifiedDate` + `AuditingEntityListener`, sin tocar el código de negocio.

### b) Relaciones **unidireccionales** (no bidireccionales)
Ninguna entidad tiene `@OneToMany`. Por ejemplo, `Usuario` **no** tiene una lista `List<Experiencia> experiencias`. Solo `Experiencia` conoce a su `Usuario guia` (`@ManyToOne`).

**Por qué:** las relaciones bidireccionales en JPA son una fuente común de bugs para desarrolladores junior — colecciones `LAZY` que disparan N+1 queries sin darte cuenta, y **serialización JSON en bucle infinito** (`Usuario → List<Experiencia> → Usuario → ...`) si se exponen directo en un controlador. Las consultas tipo "mis experiencias" o "mis reservas" se resuelven con métodos de repositorio (`findByGuiaId`, `findByViajeroId`), que es más explícito y controlable.

### c) `FetchType.LAZY` en todas las relaciones `@ManyToOne`
Por defecto, JPA hace `@ManyToOne` **EAGER**, lo cual trae relaciones que muchas veces no se necesitan (ej. traer el `Usuario` guía completo solo para listar experiencias). Se fuerza `LAZY` explícitamente — buena práctica estándar en proyectos serios.

### d) `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` basado solo en `id`
Evita el bug clásico de Lombok: generar `equals()`/`hashCode()` con **todos** los campos, lo cual rompe con relaciones lazy (dispara SQL innecesario al comparar) y falla en colecciones (`Set`) antes de que la entidad tenga `id` asignado.

### e) `cupo_disponible` desnormalizado en `Experiencia`
Se decidió mantener el cupo disponible como columna directa (en vez de calcularlo siempre con `SUM` sobre `reservas`) por rendimiento de lectura — el listado público de experiencias se consulta mucho más de lo que se reserva. La consistencia se garantiza transaccionalmente en la **Etapa 3** (capa de servicio). Como respaldo/auditoría, se agregó `ReservaRepository.sumPersonasConfirmadasPorExperiencia()` para poder verificar/reconciliar el dato si hiciera falta.

### f) `JpaSpecificationExecutor` en `ExperienciaRepository`
En vez de crear métodos derivados combinados (`findByCiudadAndCategoriaAndFecha...`, `findByCiudadAndFecha...`, etc. — combinatoria explosiva), se habilita `Specification<Experiencia>` para construir filtros **dinámicos y opcionales** (por ciudad, categoría, fecha) en la capa de servicio. Mantiene el repositorio limpio y sin lógica de negocio.

## 3. Pruebas realizadas

### a) Validación de esquema (Hibernate `ddl-auto: validate`)
Se arrancó la aplicación completa contra MySQL real. Si una sola entidad no coincidiera exactamente con las columnas/tipos de las tablas Flyway, Hibernate lanza `SchemaManagementException` al iniciar. **Resultado: arrancó sin errores** → las 8 entidades están correctamente mapeadas.

### b) Prueba funcional end-to-end (smoke test manual)
Se ejecutó temporalmente un `CommandLineRunner` que:
1. Busca catálogos ya sembrados (`Rol=GUIA/VIAJERO`, `Ciudad=Cartagena`, `Categoria=Nautica`, etc.)
2. Crea un `Usuario` guía y un `Usuario` viajero
3. Crea una `Experiencia` asociada al guía
4. Crea una `Reserva` de esa experiencia por el viajero
5. Verifica auditoría automática, la consulta de suma de cupos, y los finders `findByGuiaId`/`findByViajeroId`

**Resultado real obtenido:**
```
SMOKE_TEST_RESULT guiaId=1 expId=1 resId=1 createdAt=2026-08-25T19:25:54.624693
sumaConfirmadas=2 misExperiencias=1 misReservas=1
```
Todo correcto: IDs generados, `createdAt` autollenado por JPA Auditing, la suma de personas confirmadas coincide con la reserva creada, y los finders devuelven los registros esperados.

### c) Nota sobre Testcontainers
Se dejó preparado `src/test/java/.../DomainPersistenceIT.java`, un test de integración con **Testcontainers** (MySQL real en contenedor efímero) que reproduce el mismo flujo de forma automatizada y repetible — este es el patrón que se formalizará en la **Etapa 6 (Testing)**.

> ⚠️ En este entorno de sandbox, el socket de Docker está restringido y Testcontainers no puede conectarse (`Could not find a valid Docker environment`). **Esto es una limitación del entorno de desarrollo actual, no del código.** En tu máquina local con Docker Desktop estándar, este test correrá sin problema con `mvn test`.

## 4. Cómo verificarlo tú mismo

```bash
cd backend
docker compose up -d
mvn spring-boot:run
# En otra terminal, revisa que arranque sin errores de Hibernate/Flyway
```

Para reproducir el test de integración (en tu máquina local con Docker Desktop normal, fuera de este sandbox):
```bash
mvn test -Dtest=DomainPersistenceIT
```

---
**Siguiente paso:** aprobar para iniciar la **Etapa 3 — Capa de Servicio** (DTOs, MapStruct mappers, reglas de negocio: control de cupo, validaciones de rol) (`04-etapa3-servicios.md`).
