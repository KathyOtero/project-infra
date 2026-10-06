# Etapa 11 — Módulo de Reservas (Frontend)

## Objetivo

Conectar el flujo de negocio central de la aplicación (reservar un cupo en
una experiencia) que hasta la Etapa 10 estaba deshabilitado en la UI: botón
real de "Reservar" en el detalle, historial "Mis reservas" para el Viajero, y
"Reservas recibidas" para el Guía. El backend de este módulo ya existía
completo desde etapas anteriores (`ReservaController`, `ReservaServiceImpl`
con bloqueo pesimista de cupo); esta etapa fue puramente de frontend.

## Componentes y páginas creados

| Archivo | Propósito |
|---|---|
| `components/ReservaModal.tsx` | Modal simple (sin RHF, un solo campo numérico) para confirmar cantidad de personas y ver el total antes de reservar |
| `pages/ExperienciaDetallePage.tsx` | (modificado) botón "Reservar" real, ya no deshabilitado; recarga la experiencia tras reservar para reflejar el nuevo cupo |
| `pages/viajero/MisReservasPage.tsx` | Historial real del Viajero: lista sus reservas, botón "Cancelar" para las `CONFIRMADA` |
| `pages/guia/ReservasRecibidasPage.tsx` | Panel de solo lectura del Guía: reservas recibidas en todas sus experiencias, con nombre del viajero |
| `routes/router.tsx` / `components/MainLayout.tsx` | (modificados) nueva ruta protegida `/guia/reservas` y enlace de navegación |

## Decisiones técnicas

### 1. El backend ya resolvía la concurrencia — el frontend no necesita "adivinar" el cupo
`ReservaServiceImpl.reservar()` usa `findByIdParaActualizar` (`SELECT ... FOR
UPDATE`) para bloquear la fila de la experiencia durante la transacción,
evitando que dos reservas concurrentes sobrevendan el mismo cupo. El frontend
solo hace una validación *optimista* en el modal (no reservar más del cupo
que ya conoce del último `GET`) para dar feedback instantáneo, pero el
control real de la carrera vive en el backend — si dos usuarios reservan al
mismo tiempo justo en el límite, el segundo simplemente recibirá un error
`CupoInsuficienteException` desde el servidor, que `extraerMensajeError()`
muestra igual que cualquier otro error de validación.

### 2. Modal de reserva sin React Hook Form
A diferencia del modal de experiencias (varios campos, validación compleja),
el de reserva solo tiene un campo numérico (`cantidadPersonas`). Se usó
`useState` simple en vez de RHF+Zod para evitar la sobre-ingeniería de traer
una librería de formularios para un único input.

### 3. Estados de reserva y acciones permitidas por rol
`EstadoReservaNombre` define `PENDIENTE`, `CONFIRMADA`, `CANCELADA`. En la
práctica `reservar()` siempre crea la reserva ya en `CONFIRMADA` (no hay un
paso de aprobación manual por parte del guía en este MVP), así que el botón
"Cancelar" en `MisReservasPage` solo se muestra para `CONFIRMADA`. La
cancelación es exclusiva del viajero dueño de la reserva (verificado también
en el backend); por eso `ReservasRecibidasPage` (vista del guía) es de solo
lectura, sin ningún botón de acción.

### 4. Recarga tras mutaciones en vez de actualización optimista de estado
Tanto en el detalle (tras reservar) como en "Mis reservas" (tras cancelar), se
vuelve a pedir el recurso al backend en vez de actualizar el estado local a
mano. Es una decisión deliberada de simplicidad para un MVP académico: evita
que la UI muestre un cupo o estado "optimista" que luego no coincida con lo
que realmente persistió el servidor.

## Verificación

- `npx tsc -b` y `npm run build` — sin errores, 185 módulos.
- Prueba E2E real con Playwright + Chromium (dos contextos de navegador
  simultáneos, uno como Guía y otro como Viajero, para simular ambos roles
  interactuando con la misma experiencia):
  1. Guía se registra y publica una experiencia con cupo máximo 2.
  2. Viajero se registra, entra al detalle, reserva 1 persona → `POST
     /api/reservas` responde `201`, UI muestra confirmación.
  3. Tras recargar el detalle, el cupo disponible bajó de 2 a 1.
  4. La reserva aparece en "Mis reservas" del viajero con estado `CONFIRMADA`.
  5. La misma reserva aparece en "Reservas recibidas" del guía, con el nombre
     del viajero visible.
  6. El viajero cancela la reserva desde "Mis reservas" → `DELETE
     /api/reservas/{id}` exitoso, estado pasa a `CANCELADA` en la UI.
  7. Al volver al detalle, el cupo disponible se restauró a 2.
  - **3 ejecuciones consecutivas exitosas**, confirmando estabilidad (no
    quedó ningún bug de carrera de estado como el de la Etapa 10).
- Prueba adicional de validación de cupo: se creó una experiencia con
  `cupoMax=1` y se intentó reservar para 5 personas → el modal bloquea el
  envío y muestra el mensaje "Solo hay 1 cupos disponibles" sin llegar a
  golpear el backend.
- Datos de prueba (usuarios `*_reserva_*@test.com`, `*_cupo_*@test.com` y sus
  experiencias/reservas) eliminados de la base de datos al finalizar.

## Estado

Etapa 11 completada. El flujo de negocio principal de la aplicación (publicar
experiencia → descubrir → reservar → gestionar reserva) queda funcional de
punta a punta. Quedan pendientes, según el plan general:

- **Etapa 12** — pulido de UI/UX y preparación de despliegue del frontend.
- **Etapa 13** — diseño de arquitectura de Alta Disponibilidad en AWS (el
  entregable original de la asignación de la universidad).
