output "vpc_id" {
  description = "ID de la VPC principal"
  value       = module.network.main_id
}

output "public_subnets" {
  description = "IDs de las subredes publicas"
  value       = [module.network.public_a_id, module.network.public_b_id]
}

output "private_subnets" {
  description = "IDs de las subredes privadas"
  value       = [module.network.private_a_id, module.network.private_b_id]
}

output "alb_dns_name" {
  description = "URL DNS publica del Application Load Balancer"
  value       = "http://${aws_lb.web_alb.dns_name}"
}

output "autoscaling_group_name" {
  description = "Nombre del Auto Scaling Group"
  value       = module.compute.asg.name
}

output "deploy_config" {
  value = local.deploy_config
}
output "artifacts_bucket" {
  value = aws_s3_bucket.artifacts.id
}
output "ssm_bucket" {
  value = aws_s3_bucket.ssm.id
}
output "frontend_bucket" {
  value = aws_s3_bucket.frontend.id
}
output "cloudfront_id" {
  value = aws_cloudfront_distribution.frontend.id
}
output "frontend_url" {
  value = "https://${var.domain_name != "" ? var.domain_name : aws_cloudfront_distribution.frontend.domain_name}"
}
output "ecr_repository_url" {
  value = aws_ecr_repository.backend.repository_url
}
output "target_group_arn" {
  value = aws_lb_target_group.web_tg.arn
}
output "backend_release" {
  value = var.backend_release
}
