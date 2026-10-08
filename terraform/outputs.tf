output "vpc_id" {
  description = "ID de la VPC principal"
  value       = aws_vpc.main.id
}

output "public_subnets" {
  description = "IDs de las subredes publicas"
  value       = [aws_subnet.public_a.id, aws_subnet.public_b.id]
}

output "private_subnets" {
  description = "IDs de las subredes privadas"
  value       = [aws_subnet.private_a.id, aws_subnet.private_b.id]
}

output "alb_dns_name" {
  description = "URL DNS publica del Application Load Balancer"
  value       = "http://${aws_lb.web_alb.dns_name}"
}

output "autoscaling_group_name" {
  description = "Nombre del Auto Scaling Group"
  value       = aws_autoscaling_group.web_asg.name
}