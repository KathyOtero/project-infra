# Validación local — 12 de octubre de 2026

Rama: `mejoras-andres`. Estado inicial limpio; se revisaron la propuesta
DOCX, el documento de arquitectura y la implementación existente.
No se ejecutó ninguna operación contra AWS.

| Verificación | Resultado |
|---|---|
| Terraform 1.9.8: formato y validate | Correcto en proyecto principal y bootstrap |
| Terraform con proveedor AWS simulado | Invariantes HA/seguridad y rechazo de dominio incompleto |
| TFLint 0.55.1 | Sin problemas |
| Ansible Core 2.18.6 / amazon.aws 10.1.2 | Sintaxis correcta en Linux mediante Docker |
| Prueba Python del launcher | Secretos fuera de argumentos y borrado del archivo temporal antes de arrancar |
| Actionlint 1.7.7 con ShellCheck | Workflows correctos |
| Maven verify / Java 17 | 24 pruebas unitarias/de seguridad y 1 integración MySQL correctas |
| Frontend Node 22 | Lint sin errores y build de producción correcto; 5 advertencias existentes |

La ejecución inicial de Testcontainers falló al usar una API Docker anterior
a la admitida por Docker Engine 29. Se añadió `api.version=1.44` en recursos
de prueba y se repitió `mvn verify` satisfactoriamente. Se configuró Maven
Failsafe para que las pruebas `*IT` se ejecuten realmente en `verify`.

La instalación npm reportó además dos vulnerabilidades de severidad alta
en las dependencias existentes. No se realizaron actualizaciones ajenas al
alcance de infraestructura. Revisarlas antes de una publicación real.

Quedan por verificar en un futuro entorno autorizado: IAM/OIDC,
certificados, conexiones SSM, ejecución/idempotencia del playbook en EC2,
rolling refresh bajo tráfico, fallos de AZ y failover RDS. Las pruebas
simuladas no sustituyen estas verificaciones.

Referencias técnicas consultadas:

- [Ansible: conexión SSM](https://docs.ansible.com/projects/ansible/latest/collections/amazon/aws/aws_ssm_connection.html)
- [Terraform: distribución CloudFront](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudfront_distribution.html)
- [Testcontainers: incompatibilidad de API con Docker 29](https://github.com/testcontainers/testcontainers-java/issues/11211)
