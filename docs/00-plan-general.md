# Plan General del Proyecto — CaribeXperience

> Plataforma de reserva de experiencias turísticas (estilo "Airbnb Experiences") enfocada en la Costa Caribe colombiana.
> Proyecto de aula: **Aplicación Web en Alta Disponibilidad** — Grupo LOS PARRILLEROS.

## 1. Objetivo del proyecto

Construir una aplicación web full-stack (backend en **Spring Boot**, frontend en **React**, base de datos **MySQL**) que permita:

- A un **Guía** publicar experiencias turísticas (tours, actividades).
- A un **Viajero** buscar, ver y reservar esas experiencias.

Y desplegarla en **AWS** con una arquitectura de **alta disponibilidad** (ALB + Auto Scaling Group + RDS MySQL Multi-AZ + S3/CloudFront).

Este repositorio contiene el desarrollo de la aplicación. La infraestructura de AWS se documentará por separado en `docs/06-arquitectura-aws.md` (etapa final).

## 2. Alcance del MVP

**Incluye:**
- Registro/login con roles: `GUIA`, `VIAJERO`
- CRUD de experiencias (solo el Guía dueño puede editar/eliminar las suyas)
- Listado público de experiencias con filtros (ciudad, categoría, fecha)
- Reserva de una experiencia por parte de un Viajero (con control de cupo)
- Historial de reservas (Viajero) y reservas recibidas (Guía)
- Autenticación con JWT

**No incluye (fuera de alcance, futuro):**
- Pasarela de pagos real
- Chat entre usuarios
- Reseñas/calificaciones (nice-to-have si sobra tiempo, se evaluará en etapa final)
- Recomendaciones automáticas / mapas interactivos

## 3. Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Java 21 + Spring Boot 3.x |
| Persistencia | Spring Data JPA + Hibernate |
| Migraciones DB | **Flyway** |
| Base de datos | MySQL 8 |
| Seguridad | Spring Security + JWT |
| Documentación API | springdoc-openapi (Swagger UI) |
| Testing | JUnit 5 + Mockito + Testcontainers |
| Frontend | React 18 + Vite + TypeScript |
| Estado/HTTP | React Query + Axios |
| Estilos | Tailwind CSS |
| Build/Empaquetado | Maven |
| Infraestructura (etapa final) | AWS (EC2, ALB, ASG, RDS Multi-AZ, S3, CloudFront) |

## 4. Arquitectura del backend

Se usará **arquitectura en capas (layered architecture)** dentro de un enfoque modular por dominio, aplicando principios **SOLID**, **Clean Code** y patrones de diseño estándar de Spring:

```
com.caribexperience
 ├── config          → configuración (seguridad, CORS, OpenAPI, beans)
 ├── domain           → entidades JPA (modelo de dominio)
 ├── repository       → interfaces Spring Data JPA (acceso a datos)
 ├── service           
 │    ├── interfaces  → contratos de servicio (abstracción)
 │    └── impl        → implementación de la lógica de negocio
 ├── web
 │    ├── controller  → controladores REST
 │    ├── dto         → Request/Response DTOs (no exponer entidades)
 │    └── mapper      → MapStruct: entidad ↔ DTO
 ├── security         → JWT, filtros, UserDetailsService
 ├── exception        → excepciones de negocio + manejador global (@ControllerAdvice)
 └── common           → utilidades, constantes, enums
```

**Patrones de diseño aplicados:**
- **DTO Pattern** — nunca exponer entidades JPA directamente en la API
- **Repository Pattern** — vía Spring Data JPA
- **Strategy/Interface segregation** — servicios definidos por interfaz, inyección de dependencias
- **Builder** — construcción de DTOs/entidades complejas
- **Global Exception Handler** — `@ControllerAdvice` centralizado
- **Mapper Pattern** — MapStruct para conversión entidad↔DTO (evita boilerplate y errores manuales)

## 5. Etapas del proyecto

> Cada etapa tendrá su propio archivo `.md` con explicación detallada, decisiones tomadas y cómo ejecutarla/probarla. **No se avanza a la siguiente etapa sin revisar y aprobar el plan de la actual.**

### Backend

| Etapa | Nombre | Contenido | Archivo |
|---|---|---|---|
| 0 | Diseño de base de datos | Modelo ER, relaciones, catálogo de tablas | `01-diseno-base-datos.md` |
| 1 | Setup del proyecto + Flyway | Estructura Maven, `application.yml`, conexión MySQL, migraciones V1 (esquema completo) | `02-etapa1-setup-flyway.md` |
| 2 | Capa de dominio y repositorios | Entidades JPA, relaciones `@OneToMany`/`@ManyToOne`, repositorios Spring Data | `03-etapa2-dominio-repositorios.md` |
| 3 | Capa de servicio (lógica de negocio) | Reglas: control de cupo, validación de roles, DTOs, mappers | `04-etapa3-servicios.md` |
| 4 | Capa web (REST API) | Controladores, validaciones (Bean Validation), manejo global de errores | `05-etapa4-api-rest.md` |
| 5 | Seguridad (JWT + roles) | Registro/login, filtros JWT, autorización por rol (`@PreAuthorize`) | `06-etapa5-seguridad.md` |
| 6 | Testing | Unit tests (servicios) + integración (Testcontainers + MySQL real) | `07-etapa6-testing.md` |
| 7 | Documentación API + Docker | Swagger UI, Dockerfile, docker-compose (app + MySQL) | `08-etapa7-docker-docs.md` |

### Frontend

| Etapa | Nombre | Contenido | Archivo |
|---|---|---|---|
| 8 | Setup React + estructura | Vite + TS, routing, cliente Axios, diseño de carpetas | `09-etapa8-setup-react.md` |
| 9 | Autenticación | Login/registro, contexto de auth, rutas protegidas por rol | `10-etapa9-auth-frontend.md` |
| 10 | Módulo Experiencias | Listado, detalle, filtros, CRUD (vista Guía) | `11-etapa10-experiencias-frontend.md` |
| 11 | Módulo Reservas | Reservar, mis reservas, reservas recibidas | `12-etapa11-reservas-frontend.md` |
| 12 | Pulido UI/UX + despliegue frontend | Responsive, loading/error states, build para S3/CloudFront | `13-etapa12-pulido-despliegue.md` |

### Infraestructura (fase final del curso)

| Etapa | Nombre | Archivo |
|---|---|---|
| 13 | Arquitectura AWS de Alta Disponibilidad | `14-arquitectura-aws.md` |

## 6. Flujo de trabajo por etapa

1. Te presento el **plan de la etapa** (qué se va a construir, por qué, decisiones técnicas).
2. Apruebas o pides ajustes.
3. Implemento la etapa completa (código + migraciones si aplica).
4. Genero el `.md` explicando lo construido.
5. Verificamos que compila/corre antes de pasar a la siguiente etapa.

## 7. Estructura de carpetas del proyecto

```
Proyecto Infra/
 ├── backend/          → proyecto Spring Boot (Maven)
 ├── frontend/          → proyecto React (Vite)
 ├── db/                → scripts SQL de referencia / diagramas
 └── docs/              → toda la documentación por etapas (este archivo y los siguientes)
```

---
**Estado actual:** Etapa 0 completada (diseño de BD). Pendiente aprobación para iniciar Etapa 1 (setup + Flyway).
