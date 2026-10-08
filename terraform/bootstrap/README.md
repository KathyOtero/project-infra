# Bootstrap de Estado Remoto (Terraform Remote State)

Este módulo crea los recursos base en AWS necesarios para almacenar y proteger el estado de Terraform de forma remota y colaborativa:

1. **Bucket S3 (`caribexperience-tf-state-los-parrilleros`)**:
   - Almacena el archivo `terraform.tfstate`.
   - **Versionamiento habilitado**: permite restaurar estados anteriores ante fallos o corrupciones.
   - **Cifrado SSE-AES256**: protege el contenido confidencial del estado en reposo.
   - **Bloqueo público total**: previene cualquier exposición a Internet.
   - **Prevent destroy**: evita borrados accidentales del estado.

2. **Tabla DynamoDB (`caribexperience-tf-locks`)**:
   - Gestiona el **bloqueo de estado (State Locking)** con la clave primaria `LockID`.
   - Evita condiciones de carrera si dos desarrolladores o dos pipelines de GitHub Actions ejecutan `terraform apply` simultáneamente.

---

## Cómo ejecutarlo (Solo una vez)

Si deseas aprovisionarlo manualmente con Terraform CLI:

```bash
cd terraform/bootstrap
terraform init
terraform apply
```

Una vez completado, el backend S3 en `terraform/main.tf` ya podrá utilizarse tanto localmente como en GitHub Actions.
