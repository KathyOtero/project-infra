# Etapa 9 — Autenticación en el Frontend

## Objetivo

Construir los formularios reales de login y registro, conectados al backend real
(`POST /api/auth/login`, `POST /api/usuarios/registro`), con validación en cliente,
manejo de errores del servidor, persistencia de sesión y redirección según el rol
del usuario autenticado.

## Componentes y utilidades creados

| Archivo | Propósito |
|---|---|
| `lib/schemas.ts` | Esquemas Zod (`loginSchema`, `registroSchema`), espejo de las validaciones `jakarta.validation` del backend |
| `lib/errors.ts` | `extraerMensajeError()` — normaliza errores de Axios usando la forma `ApiErrorResponse` del `GlobalExceptionHandler` |
| `components/FormInput.tsx` | Input reutilizable (label + mensaje de error), compatible con `react-hook-form` vía `forwardRef` |
| `components/SubmitButton.tsx` | Botón de submit con estado "Procesando..." mientras el formulario está enviando |
| `components/AlertaError.tsx` | Caja de error para mostrar mensajes generales del servidor |
| `pages/LoginPage.tsx` | Formulario real de login |
| `pages/RegistroPage.tsx` | Formulario real de registro (Guía/Viajero) |
| `routes/RutaProtegida.tsx` | (actualizado) ahora guarda la ruta de origen en el estado de navegación |

## Decisiones técnicas

### 1. Validación en dos capas (Zod en cliente + Bean Validation en servidor)
Los esquemas Zod (`lib/schemas.ts`) replican las restricciones que ya existen en
`UsuarioRegistroRequest`/`LoginRequest` del backend (`@NotBlank`, `@Email`,
`@Size(min=8)`, `@Pattern(GUIA|VIAJERO)`). El cliente da feedback instantáneo sin
esperar un roundtrip HTTP, pero **el backend nunca deja de validar** — es la
verdadera fuente de verdad; si alguien evade la validación del cliente (ej. con
DevTools), el servidor la rechaza igual.

### 2. Registro + auto-login encadenado
El endpoint `POST /api/usuarios/registro` solo crea el usuario y devuelve
`UsuarioResponse` (sin token). En vez de redirigir al usuario a `/login` para que
vuelva a escribir sus credenciales, `RegistroPage` llama automáticamente a
`login()` (contexto de auth) con el mismo email/password recién registrados,
dejando al usuario ya autenticado — mejor UX, análogo a como funcionan la mayoría
de apps modernas (Airbnb incluido).

### 3. Redirección post-login inteligente
- Si el usuario intentó acceder a una ruta protegida sin sesión (ej.
  `/guia/experiencias`), `RutaProtegida` lo manda a `/login` guardando el
  `pathname` original en `location.state.from`. Tras el login exitoso, se le
  regresa exactamente a donde intentaba ir.
- Si no hay un "origen" guardado (llegó directo a `/login`), se le redirige según
  su rol: `GUIA` → `/guia/experiencias`, `VIAJERO` → `/` (home pública).

### 4. Manejo de errores del servidor
`extraerMensajeError()` inspecciona la respuesta de error Axios:
- Si `errores` tiene contenido (violaciones de campo, ej. "El apellido es
  obligatorio"), se muestran esos mensajes.
- Si no, se usa el `message` general (ej. "Email o contrasena incorrectos" para
  un 401 de login fallido).
- Si la petición ni siquiera llegó a tener una respuesta con esa forma (error de
  red), se usa un mensaje de fallback genérico.

## Verificación end-to-end (navegador real con Playwright)

Se instaló Playwright + Chromium temporalmente para probar el flujo completo en un
navegador real (no solo llamadas curl a la API), contra el stack Docker levantado
en la Etapa 7 (`caribexperience-backend` + `caribexperience-mysql`) y el dev server
de Vite:

| Escenario | Resultado |
|---|---|
| Registro de un Viajero nuevo → auto-login → redirección a home | ✅ OK |
| Navbar refleja el nombre y el rol tras el login (muestra "Mis reservas") | ✅ OK |
| Logout → vuelve a mostrar "Iniciar sesion" / "Registrarme" | ✅ OK |
| Login manual con las credenciales recién creadas | ✅ OK |
| Login con contraseña incorrecta → mensaje de error visible, permanece en `/login` | ✅ OK |
| Acceso a `/guia/experiencias` sin sesión → redirige a `/login` | ✅ OK |

Los datos de prueba (`viajero_*@test.com`) fueron eliminados de la base de datos
al finalizar. Playwright/Chromium se desinstaló del entorno temporal tras la
verificación (no queda como dependencia del proyecto).

```bash
npx tsc -b      # 0 errores
npm run build   # build de produccion exitoso
```

## Próximo paso

**Etapa 10 — Módulo de Experiencias**: listado público con filtros (ciudad,
categoría, fecha), página de detalle, y el CRUD del panel de Guía
(`/guia/experiencias`) usando `experienciasApi` y `catalogosApi` ya creados en la
Etapa 8.
