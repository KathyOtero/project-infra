# Etapa 12 — Pulido de UI/UX y Despliegue del Frontend

## Objetivo

Cerrar los detalles de experiencia de usuario que quedaron pendientes de las
etapas funcionales (Etapas 8-11) y preparar el frontend para ejecutarse como
un contenedor de producción dentro del mismo `docker-compose.yml` que ya
orquesta el backend y MySQL, sirviendo los archivos estáticos con nginx en
vez del servidor de desarrollo de Vite.

## Cambios de UI/UX

| Cambio | Archivo(s) |
|---|---|
| Componente `Spinner` reutilizable, reemplaza los `<p>Cargando...</p>` de texto plano en 5 páginas | `components/Spinner.tsx` |
| Página 404 real para rutas no reconocidas | `pages/NotFoundPage.tsx`, `routes/router.tsx` (ruta comodín `path: '*'`) |
| Navbar responsive con menú hamburguesa en móvil (`< md`), que se cierra automáticamente al navegar | `components/MainLayout.tsx` |

## Despliegue del frontend en Docker

### 1. Build multi-stage con nginx
Igual que el backend (Etapa 7), el `Dockerfile` del frontend usa dos etapas:
compila con `node:22-alpine` (`npm ci && npm run build`) y luego copia
**solo** el resultado estático (`dist/`) a una imagen `nginx:1.27-alpine`
mucho más liviana — la imagen final no incluye Node, npm ni `node_modules`.

### 2. `nginx.conf`: SPA fallback + proxy a la API
Dos problemas que resuelve la configuración de nginx:

- **SPA fallback**: `try_files $uri $uri/ /index.html;` — sin esto, refrescar
  el navegador en una ruta como `/experiencias/5` daría un 404 real de nginx,
  porque ese archivo no existe en disco; react-router-dom solo puede tomar el
  control *después* de que `index.html` cargue.
- **Proxy de `/api/`**: en desarrollo, Vite proxya `/api` a `localhost:8080`
  (Etapa 8). En producción no hay Vite corriendo, así que nginx cumple el
  mismo rol reenviando `/api/` al servicio `backend` de docker-compose por su
  nombre de red interna (`http://backend:8080/api/`). El frontend en el
  navegador sigue llamando a rutas relativas (`/api/...`), sin necesidad de
  configurar CORS entre el navegador y el backend (todo pasa por el mismo
  origen `:8082`).

### 3. Bug de healthcheck: `localhost` resuelve a IPv6 en Alpine
El primer `HEALTHCHECK` usaba `wget -qO- http://localhost/`, y el contenedor
quedó permanentemente `unhealthy` a pesar de que nginx respondía
correctamente (confirmado con `curl` externo). Diagnóstico: dentro de Alpine,
`localhost` resuelve primero a `::1` (IPv6), pero nginx en la imagen base
solo escucha en `0.0.0.0:80` (IPv4) — `netstat` dentro del contenedor lo
confirmó. Solución: apuntar el healthcheck a `http://127.0.0.1/` explícito,
evitando la resolución DNS ambigua.

### 4. Extensión de `docker-compose.yml`
Se agregó el servicio `frontend` (build desde `../frontend`, `depends_on:
backend` con `condition: service_healthy`), expuesto en el puerto `8082`
(el `8081` ya estaba tomado por otro proyecto en esta máquina). También se
amplió `CORS_ALLOWED_ORIGINS` del backend para incluir ese nuevo origen,
aunque en producción el navegador nunca cruza orígenes gracias al proxy de
nginx — se deja por si se accede al backend directamente en `:8080` durante
pruebas.

## Verificación

- `npx tsc -b` — sin errores tras los cambios de UI.
- `docker compose up -d --build` (desde `backend/`, construye ambas
  imágenes) → los 3 contenedores (`mysql`, `backend`, `frontend`) quedan
  `healthy`.
- Prueba de humo con Playwright contra el stack 100% dockerizado
  (`http://localhost:8082`, sin Vite ni proxy de desarrollo):
  1. La home carga y muestra el listado de experiencias.
  2. Registro + auto-login funciona correctamente cruzando nginx → backend.
  3. Navegar directamente a una ruta profunda (`/viajero/reservas`, sin pasar
     por la home primero) responde `200` y renderiza la página — confirma
     que el SPA fallback de nginx funciona.
- Verificado manualmente con `curl` que `GET /api/catalogos/ciudades` a
  través de `:8082` (proxy nginx) devuelve la misma respuesta que golpear el
  backend directo en `:8080`.
- Datos de prueba (usuario `smoke_docker_*@test.com`) eliminados de la base
  de datos al finalizar.

## Estado

Etapa 12 completada. La aplicación completa (frontend + backend + base de
datos) ahora se levanta con un solo comando (`docker compose up -d --build`
desde `backend/`) y queda accesible en `http://localhost:8082`, un paso
necesario antes de diseñar la arquitectura de despliegue en AWS.

Queda pendiente, según el plan general:

- **Etapa 13** — diseño de arquitectura de Alta Disponibilidad en AWS (el
  entregable original de la asignación de la universidad): balanceador de
  carga, múltiples instancias del backend, RDS Multi-AZ para MySQL,
  distribución del frontend estático (ej. S3 + CloudFront), y el diagrama de
  arquitectura correspondiente.
