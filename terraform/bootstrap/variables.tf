variable "aws_region" {
  description = "Region de AWS"
  type        = string
  default     = "us-east-1"
}

variable "bucket_name" {
  description = "Nombre unico global del bucket S3 para el estado remoto de Terraform"
  type        = string
  default     = "caribexperience-tf-state-los-parrilleros"
}

variable "dynamodb_table_name" {
  description = "Nombre de la tabla DynamoDB para control de concurrencia y bloqueo de estado"
  type        = string
  default     = "caribexperience-tf-locks"
}
