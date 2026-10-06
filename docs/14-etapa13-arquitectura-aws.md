# Etapa 13 — Arquitectura AWS de Alta Disponibilidad

## Objetivo

Diseñar la arquitectura en AWS que llevaría a **CaribeXperience** a
producción con **Alta Disponibilidad**: sin puntos únicos de falla, capaz de
escalar horizontalmente y de recuperarse automáticamente ante la caída de un
componente. Este documento (junto al diagrama) es el entregable de la
primera entrega del proyecto de aula.

## Diagrama

Archivo editable: [`diagramas/arquitectura-aws-ha.drawio`](diagramas/arquitectura-aws-ha.drawio)

**Cómo abrirlo:**
1. Ir a [app.diagrams.net](https://app.diagrams.net) (draw.io, gratis, sin
   registro).
2. `File → Open From → Device` y seleccionar el archivo `.drawio`.
3. Para exportar a la entrega: `File → Export as → PNG` (o PDF).

El diagrama usa los **iconos oficiales de AWS** (librería `aws4` integrada
en draw.io) para: Route 53, CloudFront, S3, Application Load Balancer, NAT
Gateway, EC2, RDS, Secrets Manager y CloudWatch, organizados dentro de una
VPC con 2 Availability Zones, subredes públicas/privadas, y las relaciones
de tráfico entre todos los componentes.

---

## Explicación de cada componente del diagrama

### 1. Usuario → Route 53 (DNS)

**Qué es:** Route 53 es el servicio DNS de AWS. Traduce un dominio humano
(ej. `caribexperience.com`) a las direcciones IP reales de CloudFront/ALB.

**Por qué está aquí:** sin esto habría que darle a los usuarios una IP o un
nombre feo tipo `caribexp-alb-123456.us-east-1.elb.amazonaws.com`.

### 2. CloudFront + S3 (frontend estático)

- **S3** guarda los archivos que genera `npm run build` (el `dist/` de
  React: `index.html`, JS, CSS). Es almacenamiento de objetos, no un
  servidor — no hay ningún "EC2" que pueda caerse aquí.
- **CloudFront** es una CDN: replica esos archivos en decenas de
  ubicaciones ("edge locations") alrededor del mundo y sirve al usuario
  desde la más cercana.

**Por qué reemplaza al nginx en Docker de la Etapa 12:** el contenedor
nginx que armamos sirve genial para correr **localmente**, pero en AWS no
tiene sentido pagar/mantener un servidor 24/7 solo para archivos que no
cambian entre petición y petición. S3+CloudFront ya tiene disponibilidad
nativa de 99.99%+ (S3 replica internamente entre múltiples AZs sin que se
configure nada) y es más barato.

**Ruteo `/api/*` vs resto:** se configura en CloudFront un segundo *origin*
apuntando al ALB con un *behavior* que dice "todo lo que empiece con
`/api/` va al ALB, todo lo demás (rutas de React, JS, CSS) va a S3" — el
mismo patrón que resolvimos con el proxy de nginx en local, ahora a nivel
de CDN, y sin problemas de CORS porque el usuario solo ve un dominio.

### 3. VPC — la red privada del proyecto

- **Rango:** `10.0.0.0/16`.
- **2 Availability Zones** (mínimo para HA — con una sola AZ, esa AZ sería
  un punto único de falla).
- **Subredes públicas:** solo contienen el ALB y los NAT Gateways.
- **Subredes privadas:** contienen las instancias EC2 del backend y las
  instancias RDS — **sin ruta directa a Internet**. Nadie desde afuera
  puede conectarse directo al Spring Boot ni al MySQL, solo a través del
  ALB.

**Por qué 2 públicas + 2 privadas:** cada subred vive en una sola AZ (regla
de AWS); para tener redundancia entre AZs se necesita al menos un par
(pública + privada) por cada AZ usada.

### 4. NAT Gateway (uno por AZ)

Permite que las instancias en subred privada (el backend) inicien
conexiones **de salida** hacia Internet, sin permitir conexiones entrantes
hacia ellas. Se duplica (uno por AZ) para que, si cae la AZ del NAT-A, las
instancias de la AZ-B no se queden sin salida a Internet.

### 5. Application Load Balancer (ALB) — un único recurso, no uno por AZ

- Es **un solo ALB** (no uno por AZ). Un ALB es un recurso *regional*: AWS
  lo despliega con nodos redundantes en cada AZ internamente, sin que haya
  que crear ni administrar una instancia separada por zona. Crear dos ALBs
  sería redundante y además rompería el balanceo real, porque cada uno solo
  vería las instancias de su propia AZ en vez de repartir entre todas.
- Tiene **un solo target group**, que registra **ambas** instancias EC2
  (`EC2 #1` en AZ-a y `EC2 #2` en AZ-b) — así el balanceo es real entre las
  dos AZs, no una réplica aislada por zona.
- Recibe todo el tráfico HTTPS dirigido a `/api/*`.
- Termina el TLS/SSL (certificado HTTPS vive en el ALB; internamente habla
  HTTP plano con las instancias).
- Hace **health checks** contra `GET /actuator/health` (endpoint que ya
  expone el backend desde la Etapa 7 vía Spring Boot Actuator). Si una
  instancia deja de responder `200 OK`, el ALB deja de enviarle tráfico
  automáticamente, sin dejar de enviarle tráfico a la otra AZ.
- Reparte peticiones (round robin) entre todas las instancias sanas del
  target group, en cualquiera de las dos AZs.

> **Corrección (feedback del profesor):** la primera versión del diagrama
> dibujaba dos ALBs independientes, uno por AZ, cada uno con health check
> a una sola instancia. Eso no es una arquitectura HA válida — un ALB por
> AZ significa que si la instancia de esa AZ cae, ese ALB se queda sin
> nada que balancear (y sigue siendo un recurso extra sin propósito real,
> ya que el ALB nativo de AWS ya reparte entre AZs). Se corrigió a un único
> ALB con un target group que cubre las dos AZs.

### 6. Auto Scaling Group (ASG) + EC2

Mantiene siempre un número saludable de instancias EC2 corriendo el backend
(el mismo `Dockerfile` que ya construimos y probamos en la Etapa 7),
repartidas entre las AZs.

**Configuración propuesta:**
- Mínimo: **2** (una por AZ, mínimo real para HA de la capa de aplicación).
- Deseado: **2**.
- Máximo: **4** (permite escalar ante picos de tráfico).
- Política de escalado basada en CPU promedio (ej. > 70% agrega instancia,
  < 20% quita una).

Si una instancia muere, el ASG la detecta (vía health check), la termina y
lanza una nueva automáticamente a partir de una plantilla (*Launch
Template*), típicamente en minutos.

### 7. RDS MySQL Multi-AZ

Instancia MySQL administrada por AWS (Primary) con una segunda instancia
Standby en otra AZ, sincronizada de forma **síncrona** — cada
`INSERT`/`UPDATE` se confirma en ambas antes de responder "OK" al backend.

**Por qué es crítico en este proyecto:** `ReservaServiceImpl.reservar()`
(Etapa 11) usa `SELECT ... FOR UPDATE` para bloquear la fila de la
experiencia y evitar sobreventa de cupo — ese control de concurrencia
depende 100% de que la base de datos esté siempre disponible. Si MySQL cae
sin failover automático, todo el negocio se detiene (nadie puede reservar
ni publicar experiencias). RDS Multi-AZ resuelve ese riesgo con failover
automático (60-120 seg) sin intervención humana.

El backend siempre se conecta al **mismo endpoint DNS** de RDS; AWS resuelve
internamente ese nombre hacia la instancia que sea Primary en cada momento,
así que un failover no requiere cambiar ninguna variable de entorno.

### 8. Secrets Manager

Guarda de forma cifrada el `JWT_SECRET` (Etapa 5) y las credenciales de
RDS, en vez de tenerlas como texto plano en variables de entorno (como se
maneja hoy en `docker-compose.yml`, aceptable para desarrollo local, no para
producción). Cada instancia EC2 tiene un rol de IAM que le permite leer
(no ver en texto plano en ningún dashboard) esos secretos al arrancar.

### 9. CloudWatch

Centraliza logs (lo que hoy se ve con `docker logs
caribexperience-backend`) y métricas (CPU, memoria, latencia) de todas las
instancias, y permite configurar alarmas — ej. "si el CPU del ASG pasa 70%,
dispara el auto-scaling" o "si el health check falla 3 veces seguidas,
notificar".

---

## Flujo completo de una petición real: "un Viajero reserva una experiencia"

1. El navegador ya cargó el React (`dist/`) desde **CloudFront** (que a su
   vez lo trajo de **S3** la primera vez, y lo tiene cacheado).
2. El usuario hace clic en "Confirmar reserva" → el frontend llama a
   `POST /api/reservas` con el JWT en el header `Authorization`.
3. Como la ruta empieza con `/api/`, CloudFront la reenvía (sin cachear) al
   **ALB**.
4. El **ALB** (uno solo, target group con las dos AZs) elige una de las
   instancias sanas del **ASG** (ej. `EC2 #1`, AZ-a).
5. El filtro JWT de Spring Security (Etapa 5) valida el token sin consultar
   ninguna base de datos de sesiones — el backend es **stateless**, así que
   cualquier instancia puede atender cualquier petición.
6. `ReservaServiceImpl.reservar()` hace `SELECT ... FOR UPDATE` contra el
   endpoint de **RDS** (resuelve al Primary), valida cupo, inserta la
   reserva, hace commit.
7. La respuesta regresa: `EC2 #1` → ALB → CloudFront → navegador.

Si en el paso 6 el Primary de RDS hubiera fallado, RDS promueve el Standby
automáticamente. Si `EC2 #1` hubiera estado caída, el ALB simplemente habría
elegido `EC2 #2` en el paso 4.

---

## Justificación: por qué esta arquitectura es de Alta Disponibilidad

| Componente duplicado | Punto único de falla que elimina |
|---|---|
| 2 Availability Zones (no 1) | Falla de un centro de datos completo (energía, red, incendio) |
| ALB único, con target group multi-AZ | Falla del balanceador mismo (el ALB ya es redundante internamente entre AZs) |
| ASG con mínimo 2 instancias EC2 | Caída/reinicio/crash de una instancia individual |
| RDS Multi-AZ | Caída de la base de datos, mantenimiento de AWS, falla de disco |
| NAT Gateway por AZ | Pérdida de salida a Internet si cae la AZ del NAT |
| S3 (replicación interna nativa) | Servidor de archivos estáticos caído |
| CloudFront (red global de edge locations) | Sobrecarga de tráfico, alta latencia desde regiones lejanas |
| JWT (backend stateless) | Que una instancia "recuerde" sesiones y no se pueda repartir tráfico libremente entre instancias |

---

## Costos y Free Tier (contexto para la entrega académica)

Para esta primera entrega (diseño) no es necesario tener la arquitectura
desplegada 24/7. Recomendación:
- Presentar el diagrama completo (mostrando dominio del tema con Multi-AZ,
  2+ instancias, etc.).
- Si se pide una demo en vivo, desplegar temporalmente con lo mínimo del
  Free Tier (`t3.micro` para EC2/RDS, RDS **Single-AZ** durante la demo para
  no duplicar costo de BD), aclarando en el informe que en producción real
  se activaría Multi-AZ.
- Apagar/destruir los recursos después de cada demo.

## Próximos pasos posibles (fuera del alcance de esta entrega)

- Infrastructure as Code (Terraform o AWS CDK) para poder recrear/destruir
  el entorno de forma reproducible.
- Pipeline CI/CD (ej. GitHub Actions) que construya la imagen Docker y la
  publique en ECR, disparando un despliegue rolling en el ASG.
- Evolución futura hacia ECS Fargate o EKS (contenedores gestionados sin
  administrar servidores EC2 directamente).
