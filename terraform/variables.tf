variable "aws_region" {
  description = "Region de AWS"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Nombre del proyecto"
  type        = string
  default     = "caribexperience"
}

variable "environment" {
  description = "Entorno"
  type        = string
  default     = "dev"
}

variable "vpc_cidr" {
  description = "CIDR de la VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_a_cidr" {
  description = "CIDR Subred Publica A"
  type        = string
  default     = "10.0.1.0/24"
}

variable "public_subnet_b_cidr" {
  description = "CIDR Subred Publica B"
  type        = string
  default     = "10.0.2.0/24"
}

variable "private_subnet_a_cidr" {
  description = "CIDR Subred Privada A"
  type        = string
  default     = "10.0.10.0/24"
}

variable "private_subnet_b_cidr" {
  description = "CIDR Subred Privada B"
  type        = string
  default     = "10.0.20.0/24"
}

variable "instance_type" {
  description = "Tipo de instancia EC2"
  type        = string
  default     = "t3.micro"
}

variable "student" {
  type    = string
  default = "Los Parrilleros"
}

variable "backend_release" {
  description = "Immutable Git commit SHA published by the manual deployment workflow"
  type        = string
  default     = "0000000000000000000000000000000000000000"
  validation {
    condition     = can(regex("^[a-f0-9]{40}$", var.backend_release))
    error_message = "Use a full Git commit SHA."
  }
}

variable "domain_name" {
  type    = string
  default = ""
}

variable "zone_id" {
  type    = string
  default = ""
}

variable "cloudfront_certificate_arn" {
  description = "ACM certificate in us-east-1, required with domain_name"
  type        = string
  default     = ""
}

variable "alb_certificate_arn" {
  description = "Regional ACM certificate matching alb_origin_domain; required for HTTPS origin"
  type        = string
  default     = ""
}

variable "alb_origin_domain" {
  description = "DNS name within zone_id for the HTTPS ALB origin"
  type        = string
  default     = ""
}
