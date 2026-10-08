output "s3_bucket_name" {
  description = "Nombre del bucket S3 creado"
  value       = aws_s3_bucket.terraform_state.id
}

output "dynamodb_table_name" {
  description = "Nombre de la tabla DynamoDB para state locking"
  value       = aws_dynamodb_table.terraform_locks.name
}

output "backend_config" {
  description = "Bloque de configuracion backend S3 para terraform/main.tf"
  value       = <<EOF
backend "s3" {
  bucket         = "${aws_s3_bucket.terraform_state.id}"
  key            = "dev/terraform.tfstate"
  region         = "${var.aws_region}"
  dynamodb_table = "${aws_dynamodb_table.terraform_locks.name}"
  encrypt        = true
}
EOF
}
