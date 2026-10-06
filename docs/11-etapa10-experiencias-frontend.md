# Etapa 10 — Módulo de Experiencias (Frontend)

## Objetivo

Construir la funcionalidad central de la aplicación: listado público de
experiencias con filtros y paginación, vista de detalle, y el panel CRUD que
usa el rol **GUIA** para publicar, editar y cancelar sus propias experiencias.

## Componentes y páginas creados

| Archivo | Propósito |
|---|---|
| `lib/useCatalogos.ts` | Hook que carga una sola vez `ciudades` y `categorias` (catálogos de solo lectura) |
| `components/ExperienciaCard.tsx` | Tarjeta reutilizable de experiencia, con slot opcional `acciones` para el panel del guía |
| `components/Paginador.tsx` | Control de paginación reutilizable |
| `components/ExperienciaFormModal.tsx` | Modal compartido de creación/edición (React Hook Form + Zod) |
| `pages/HomePage.tsx` | Listado público con filtros (ciudad, categoría, fecha desde) y paginación |
| `pages/ExperienciaDetallePage.tsx` | Vista de detalle: info del guía, precio, cupo; botón "Reservar" deshabilitado (llega en Etapa 11) |
| `pages/guia/MisExperienciasPage.tsx` | Panel CRUD del guía: crear, editar, cancelar (con confirmación) |
| `lib/schemas.ts` | (extendido) `experienciaSchema` con `z.coerce.number()` para campos numéricos de inputs HTML |

## Decisiones técnicas

### 1. `ciudadId`/`categoriaId` expuestos directamente en el DTO del backend
Al construir el modal de edición se necesitaba pre-seleccionar la ciudad y
categoría de la experiencia existente en los `<select>`. El DTO original
(`ExperienciaResponse`) solo exponía los nombres como texto (`ciudad: "Cartagena"`),
mientras que el catálogo de ciudades devuelve el nombre combinado con el
departamento (`"Cartagena - Bolivar"`) — un *string mismatch* que hacía
imposible mapear de forma confiable el nombre de vuelta a un ID.

En vez de parchar esto en el frontend con lógica frágil de comparación de
strings, se corrigió en el origen: se agregaron los campos `ciudadId` y
`categoriaId` directamente a `ExperienciaResponse` y a `ExperienciaMapper`
(mapeados desde las asociaciones `Ciudad`/`Categoria` de la entidad). Esto
obligó a actualizar dos pruebas unitarias existentes que construían el DTO por
argumentos posicionales (`ExperienciaServiceImplTest`), pero deja el contrato
de la API más correcto y explícito de forma permanente.

### 2. `useForm` con tres genéricos para manejar la coerción de Zod
`experienciaSchema` usa `z.coerce.number()` en `precio`/`cupoMax` porque los
inputs HTML siempre entregan strings. Esto crea una diferencia entre el tipo de
*entrada* del formulario (string) y el tipo de *salida* ya validado (number).
`useForm<ExperienciaFormData>()` a secas producía un conflicto de tipos con
`zodResolver`; la solución es pasar los tres parámetros genéricos de
`useForm`:

```ts
useForm<z.input<typeof experienciaSchema>, unknown, ExperienciaFormData>({
  resolver: zodResolver(experienciaSchema),
});
```

(entrada cruda del formulario, contexto, salida transformada).

### 3. Bug de estado: `reset()` sobrescribiendo la edición del usuario

**Síntoma:** al editar una experiencia, el envío ocasionalmente mandaba al
backend el título/valor **original**, no el que el usuario acababa de escribir
— reproducible de forma intermitente en pruebas E2E con Playwright.

**Diagnóstico:** el `useEffect` que llama `reset(...)` para precargar el
formulario de edición tenía como dependencias
`[experienciaExistente, ciudades, categorias, reset]`. `ciudades`/`categorias`
provienen de `useCatalogos()`, cuyo `fetch` es asíncrono; cuando ese fetch
resolvía *después* de que el usuario ya había empezado a editar el campo,
React volvía a ejecutar el efecto (porque el array de categorías/ciudades
cambió de referencia) y `reset()` reescribía el formulario con los valores
originales justo antes del submit — una condición de carrera confirmada
capturando el cuerpo real de la petición `PUT` con
`page.waitForRequest()` en Playwright.

**Corrección:** ahora que `ciudadId`/`categoriaId` llegan directamente en
`experienciaExistente` (ver punto 1), el efecto de precarga ya no necesita los
arreglos de catálogos para nada — solo se usan para renderizar las opciones
del `<select>`, no para poblar el `reset()`. Se redujo la dependencia a
`[experienciaExistente?.id, reset]`, eliminando la re-ejecución espuria:

```ts
useEffect(() => {
  if (experienciaExistente) {
    reset({ ...valores desde experienciaExistente... });
  }
}, [experienciaExistente?.id, reset]);
```

## Verificación

- `npx tsc -b` y `npm run build` — sin errores, 182 módulos.
- Suite de backend (`mvn test -Dtest='!DomainPersistenceIT'`) — 24/24 tests OK
  tras el cambio de DTO.
- Imagen Docker del backend reconstruida (`docker compose up -d --build backend`)
  para incluir `ciudadId`/`categoriaId`; contenedor healthy.
- Prueba E2E real con Playwright + Chromium (temporal, en `/tmp/pw-test`,
  eliminado al finalizar):
  1. Registro como GUIA → redirige a `/guia/experiencias`.
  2. Crear experiencia → aparece en el panel del guía.
  3. Aparece en el listado público (`HomePage`) y en el detalle con el precio
     correcto y datos del guía.
  4. Editar título/otros campos → el cambio se refleja correctamente (bug de
     carrera corregido; **3 ejecuciones consecutivas exitosas** tras el fix,
     confirmando que no era casualidad de timing).
  5. Cancelar experiencia (con diálogo de confirmación) → estado cambia a
     "Cancelada" en la UI.
- Datos de prueba (usuarios `*@test.com` y sus experiencias) eliminados de la
  base de datos al finalizar.

## Estado

Etapa 10 completada. Listo para continuar con **Etapa 11 — Módulo de
Reservas** (reservar una experiencia, "Mis reservas" del viajero, "Reservas
recibidas" del guía), que también activará el botón "Reservar" actualmente
deshabilitado en `ExperienciaDetallePage`.
