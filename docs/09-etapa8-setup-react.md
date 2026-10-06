# Etapa 8 — Setup del Frontend (React + Vite + TypeScript)

## Objetivo

Inicializar el proyecto de frontend con las bases arquitectónicas necesarias para
construir, en las siguientes etapas, la autenticación (9), el módulo de experiencias
(10) y el módulo de reservas (11): estructura de carpetas, cliente HTTP tipado,
enrutamiento con rutas protegidas por rol, y estilos.

## Stack elegido

| Herramienta | Uso |
|---|---|
| **Vite + React + TypeScript** | Bundler y plantilla base (rápido, estándar de industria actual) |
| **React Router v6 (`createBrowserRouter`)** | Enrutamiento declarativo con rutas anidadas y guards |
| **Axios** | Cliente HTTP con interceptores (para adjuntar el JWT automáticamente) |
| **Tailwind CSS v4** (`@tailwindcss/vite`) | Utility-first CSS, sin necesidad de escribir CSS a mano por componente |
| **React Hook Form + Zod** | Manejo de formularios y validación (se usarán a partir de la Etapa 9: login/registro/CRUD) |

## Estructura de carpetas

```
frontend/src/
├── api/            # Clientes HTTP por dominio (uno por controller del backend)
│   ├── httpClient.ts       # Instancia Axios central + interceptores JWT
│   ├── authApi.ts
│   ├── experienciasApi.ts
│   ├── reservasApi.ts
│   └── catalogosApi.ts
├── auth/
│   ├── AuthContext.tsx     # Context + hook useAuth() (estado global de sesion)
│   └── tokenStorage.ts     # Persistencia de sesion en localStorage
├── components/
│   └── MainLayout.tsx      # Layout con navbar (Outlet de React Router)
├── pages/          # Una carpeta/archivo por pantalla (placeholders por ahora)
│   ├── HomePage.tsx
│   ├── LoginPage.tsx
│   ├── RegistroPage.tsx
│   ├── ExperienciaDetallePage.tsx
│   ├── guia/MisExperienciasPage.tsx
│   └── viajero/MisReservasPage.tsx
├── routes/
│   ├── router.tsx          # Definicion de todas las rutas
│   └── RutaProtegida.tsx   # Guard: exige sesion y, opcionalmente, un rol
├── types/
│   └── api.ts              # Tipos TS espejo exacto de los DTOs del backend
├── main.tsx
└── index.css               # Import de Tailwind + paleta de colores custom
```

Esta organización **por dominio dentro de `api/`** (no por tipo de archivo) refleja
la misma separación que tiene el backend por controlador (`AuthController`,
`ExperienciaController`, `ReservaController`, `CatalogoController`), facilitando
encontrar el código relacionado con cada feature del backend.

## Decisiones técnicas clave

### 1. Tipos TypeScript espejo de los DTOs Java
El archivo `types/api.ts` define interfaces TS que replican **campo por campo** los
`record` de `web/dto/**` del backend (`ExperienciaResponse`, `ReservaRequest`,
`PaginaResponse<T>`, `ApiErrorResponse`, etc.). Esto da autocompletado y chequeo de
tipos en tiempo de compilación al consumir la API, y documenta el contrato sin
depender de generar el cliente automáticamente (fuera de alcance para este proyecto,
aunque se podría automatizar con `openapi-typescript` a futuro usando el
`/v3/api-docs` de la Etapa 7).

### 2. Proxy de Vite en desarrollo (`vite.config.ts`)
```ts
server: {
  proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } }
}
```
El frontend en desarrollo llama a rutas relativas (`/api/experiencias`), y Vite las
reenvía internamente al backend en `localhost:8080`. Esto evita configurar CORS
distinto para desarrollo vs. producción y elimina errores de CORS mientras se
itera. En producción (build estático desplegado en S3/CloudFront, Etapa 12), se
usará la variable `VITE_API_BASE_URL` para apuntar directo al dominio del backend/ALB.

### 3. Cliente Axios con interceptores (`api/httpClient.ts`)
- **Request interceptor**: si existe un token guardado, lo adjunta como
  `Authorization: Bearer <token>` en cada petición saliente — el resto del código
  nunca necesita manipular el header manualmente.
- **Response interceptor**: si el backend responde `401` (token vencido/inválido),
  limpia la sesión local automáticamente, preparando el camino para redirigir a
  `/login` en la Etapa 9.

### 4. `AuthContext` + `tokenStorage`
Se adelantó la base de autenticación (Context API + localStorage) porque el
enrutamiento con `RutaProtegida` la necesita desde ya para poder definir rutas por
rol (`GUIA` vs `VIAJERO`). El **formulario real de login/registro y su conexión con
la UI se construirán en la Etapa 9**; por ahora `LoginPage`/`RegistroPage` son
placeholders.

### 5. Rutas protegidas por rol
```tsx
{ element: <RutaProtegida rolRequerido="GUIA" />, children: [...] }
```
`RutaProtegida` es un componente guard reutilizable: sin sesión → redirige a
`/login`; con sesión pero rol incorrecto → redirige a `/`. Refleja en el frontend
la misma separación de roles que ya existe en el backend (`@PreAuthorize`).

### 6. Tailwind CSS v4
Se usó la nueva integración vía plugin de Vite (`@tailwindcss/vite`), sin necesidad
de `tailwind.config.js` ni PostCSS manual — solo un `@import "tailwindcss";` en
`index.css` y un bloque `@theme` para la paleta de colores personalizada
(`caribe-blue`, `caribe-sand`, `caribe-coral`), evocando el tema Caribe del proyecto.

## Verificación realizada

```bash
cd frontend
npx tsc -b        # 0 errores de tipos
npm run build     # build de produccion exitoso, sin warnings
npm run dev        # servidor de desarrollo en :5173
```

Con el backend Dockerizado (Etapa 7) corriendo en `:8080`:
- `GET http://localhost:5173/` → `200 OK` (Home placeholder).
- `GET http://localhost:5173/login` → `200 OK` (ruta de React Router funcionando).
- `GET http://localhost:5173/api/catalogos/ciudades` → `200 OK`, devuelve las 7
  ciudades de la Costa Caribe sembradas en la migración `V2__seed_catalog_data.sql`
  — **confirma que el proxy de Vite conecta correctamente con el backend real**.

## Próximo paso

**Etapa 9 — Autenticación en el frontend**: construir los formularios reales de
login y registro (React Hook Form + Zod), conectar `AuthContext` con la UI,
manejo de errores de validación devueltos por el backend (`ApiErrorResponse`), y
redirecciones post-login según el rol del usuario.
