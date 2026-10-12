# Infraestructura y operación de CaribeXperience

Estado: implementación en `mejoras-andres`, preparada para revisión local.
No se ha ejecutado `apply`, bootstrap, plan real ni despliegue en AWS.

## Correspondencia con la propuesta

| Propuesta | Implementación |
|---|---|
| Dos AZ y NAT por zona | Módulo `terraform/modules/network`, dos rutas privadas independientes |
| ALB único y backend privado | Un target group, health check `/actuator/health`, Nginx hacia Docker en localhost |
| ASG mínimo 2 / deseado 2 / máximo 4 | Módulo `terraform/modules/compute`, Launch Template versionado, IMDSv2, rolling refresh y CPU objetivo 70% |
| RDS MySQL Multi-AZ | Cifrado, credenciales administradas por RDS en Secrets Manager, backups de 7 días |
| Frontend S3/CloudFront | Bucket privado, OAC, SPA rewrite solo en comportamiento estático |
| API sin cache | Comportamiento `/api/*`, todos los métodos y política AllViewerExceptHostHeader para reenviar JWT y consultas |
| DNS/TLS | Route 53 y certificados ACM suministrados como variables; HTTPS de CloudFront funciona sin dominio |
| Secretos | JWT creado una sola vez fuera de Terraform; lectura en EC2 con rol IAM acotado |
| Observabilidad | Logs Docker en CloudWatch, alarma de backends no saludables, política de CPU |
| Ansible | Roles common/webserver/deploy, inventario por etiquetas y transporte SSM |
| CI/CD | Validación sin AWS, plan opcional de lectura y despliegue manual separado |

La propuesta DOCX menciona Java 21/React 18, pero el código existente usa
Java 17/React 19. Se mantiene el stack real. Se adopta el máximo de cuatro
instancias del DOCX y del documento de arquitectura, corrigiendo el seis
que figuraba en el README anterior.

## Validación sin desplegar

Requisitos: Terraform 1.9.8, TFLint 0.55.1, Python 3.11+, Ansible Core
2.18.6, Java 17/Maven, Node 22 y Docker 25+ para Testcontainers.
En Windows se pueden ejecutar las herramientas Linux en Docker.

```bash
terraform fmt -check -recursive terraform
terraform -chdir=terraform init -backend=false -input=false
terraform -chdir=terraform validate
terraform -chdir=terraform test
terraform -chdir=terraform/bootstrap init -backend=false -input=false
terraform -chdir=terraform/bootstrap validate
tflint --chdir=terraform --recursive
pip install ansible-core==2.18.6 boto3
ansible-galaxy collection install -r ansible/requirements.yml
ANSIBLE_CONFIG=ansible/ansible.cfg ansible-playbook -i localhost, -c local ansible/site.yml --syntax-check
python -m unittest discover -s tests -v
mvn -B -f backend/pom.xml verify
npm ci --prefix frontend
npm run lint --prefix frontend
npm run build --prefix frontend
```

`terraform test` usa `mock_provider "aws"`: verifica invariantes de
seguridad/HA sin credenciales ni recursos reales. Maven Failsafe ejecuta
`DomainPersistenceIT` en `verify`; `mvn test` por sí solo ejecuta las pruebas
unitarias. `docker-java.properties` fija API 1.44 para Docker Engine 29.

## Preparación para un futuro despliegue autorizado

1. Crear el backend remoto **una sola vez**, de forma explícita desde
   `terraform/bootstrap`. Si el bucket o la tabla ya existen, importarlos
   antes de aplicar. No recrearlos ni migrar el estado sin revisarlo.
   Proteger y conservar el estado local de bootstrap; contiene los recursos
   que soportan el estado del resto de la infraestructura.
2. Crear environments GitHub `aws-dev` y `aws-plan`. Configurar revisores
   requeridos en `aws-dev` y restringirlo a `main`; para `aws-plan`, también
   exigir aprobación antes de ejecutar código de PR con credenciales.
   Los nombres en YAML no crean por sí solos estas protecciones.
3. Configurar un proveedor OIDC de GitHub y roles distintos. Restringir el
   trust a la cuenta/repositorio, audiencia `sts.amazonaws.com` y subject
   `repo:OWNER/REPO:environment:aws-dev` (o `aws-plan`). Definir variables
   `AWS_DEPLOY_ROLE_ARN` y `AWS_PLAN_ROLE_ARN` en sus environments.
4. El rol de plan debe permitir describir recursos y leer el estado S3,
   sin escritura ni creación. `plan.yml` usa `-lock=false` y nunca ejecuta
   bootstrap. Activarlo solo con `ENABLE_AWS_PLAN=true`; por defecto no corre.
5. El rol de despliegue debe poder gestionar los tipos de recurso definidos,
   leer/escribir el estado y bloquearlo en DynamoDB, publicar ECR/S3,
   inicializar el secreto JWT, invalidar CloudFront y usar SSM. Limitar
   recursos por nombres, ARNs y etiquetas. Para Ansible necesita
   `ec2:DescribeInstances`, `ssm:StartSession/TerminateSession/ResumeSession`
   y `s3:GetObject/PutObject/DeleteObject` sobre el bucket temporal SSM,
   además de `s3:ListBucket/GetBucketLocation`.
6. Para TLS hasta el ALB, cargar las variables `alb_origin_domain`,
   `alb_certificate_arn`, `zone_id`; el certificado regional debe cubrir
   ese nombre. Para dominio del frontend, `domain_name` y
   `cloudfront_certificate_arn` (ACM us-east-1). Los certificados deben
   estar emitidos antes del despliegue. Sin estos valores, el navegador
   usa HTTPS CloudFront y CloudFront → ALB usa HTTP: adecuado para comprobar
   la plantilla, pero no completa el TLS de producción de la propuesta.
   Las variables pueden suministrarse como TF_VAR_* del environment.

No se necesitan claves AWS permanentes ni claves SSH en GitHub Secrets.

## Flujo de despliegue manual

`deploy.yml` solo admite ejecución manual desde main. Dentro de un único
grupo de concurrencia compartido con destroy:

1. Valida código y prueba backend/frontend antes de modificar recursos.
2. Prepara ECR, almacenes S3 y el contenedor del secreto JWT con un plan
   dirigido. Esta fase deliberada permite publicar artefactos **antes**
   de arrancar el primer ASG; después siempre ejecuta un plan completo.
3. Inicializa JWT solo si no existe valor; no lo cambia en cada release.
   Publica imagen con tag inmutable igual al SHA y bundle Ansible por SHA.
4. Guarda y aplica el plan completo con ese SHA en el Launch Template.
   Nuevas instancias descargan el bundle y ejecutan el playbook en local.
   Así la configuración también cubre crecimiento y reemplazos del ASG.
5. Espera capacidad sana y renovación de instancias; ejecuta Ansible por
   SSM con inventario no vacío. Publica estáticos antes de `index.html`,
   invalida caché y prueba un endpoint público a través de CloudFront.

El valor cero por defecto de `backend_release` es solo un marcador para
validación; no es un release publicable. Para ejecutar manualmente Terraform
fuera del workflow se debe publicar antes el bundle/imagen y suministrar un
SHA real. No aplicar la configuración con el marcador.

Los secretos nunca entran en tfvars, inventario ni user data. En el servidor
se recuperan al iniciar el servicio y se usa un archivo temporal root-only
en `/run`, que se elimina al crear el contenedor. Docker conserva las
variables en su configuración: el acceso root/Docker debe permanecer
restringido. La app usa actualmente el usuario administrador de RDS;
se recomienda una cuenta SQL dedicada antes de producción. La rotación de
contraseña exige reiniciar el servicio para recuperar el nuevo valor.

## Destrucción y migración

Los bloques `moved` conservan los recursos de red existentes al pasarlos al
módulo, y también preservan Launch Template, ASG y política de CPU al
extraer el módulo de cómputo. Revisar el plan real antes de aplicar: existen cambios funcionales
de NAT, plantilla, health check, imagen y tamaño del ASG.

`destroy.yml` requiere el texto `DESTROY caribexperience-dev` y el environment
protegido. Solo opera sobre recursos del estado, sin limpieza de toda la
cuenta y sin continuar ante errores. RDS tiene protección de borrado y
snapshot final; los buckets no vacíos también bloquean el borrado. Archivar
datos, desactivar conscientemente la protección de RDS y vaciar solo los
buckets del proyecto antes de autorizar destroy. El backend bootstrap
permanece protegido y separado. El antiguo script de limpieza general se
conserva como historial pero no se invoca desde ningún workflow.

## Límites de las pruebas locales

Las pruebas simuladas y de sintaxis no prueban permisos OIDC reales,
disponibilidad regional, certificados, registro SSM, failover RDS ni
idempotencia contra EC2. Esas verificaciones requieren un despliegue de
prueba posterior autorizado. No se ha hecho en esta sesión.
