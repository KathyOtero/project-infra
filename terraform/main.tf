terraform {
  required_version = ">= 1.9.0, < 2.0.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket         = "caribexperience-tf-state-los-parrilleros"
    key            = "dev/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "caribexperience-tf-locks"
    encrypt        = true
  }
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
      Student     = var.student
    }
  }
}

# =========================================================
# 1. RED BASE (VPC & SUBREDES MULTI-AZ)
# =========================================================

module "network" {
  source                = "./modules/network"
  aws_region            = var.aws_region
  private_subnet_a_cidr = var.private_subnet_a_cidr
  private_subnet_b_cidr = var.private_subnet_b_cidr
  project_name          = var.project_name
  public_subnet_a_cidr  = var.public_subnet_a_cidr
  public_subnet_b_cidr  = var.public_subnet_b_cidr
  vpc_cidr              = var.vpc_cidr
}

# =========================================================
# 3. SEGURIDAD (SECURITY GROUPS)
# =========================================================

# SG del Balanceador (ALB)
resource "aws_security_group" "alb_sg" {
  name        = "${var.project_name}-alb-sg"
  description = "Permitir trafico HTTP al balanceador"
  vpc_id      = module.network.main_id

  ingress {
    description = "HTTP desde Internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  dynamic "ingress" {
    for_each = var.alb_certificate_arn == "" ? [] : [443]
    content {
      from_port   = ingress.value
      to_port     = ingress.value
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
    }
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-alb-sg"
  }
}

# SG de las Instancias EC2
resource "aws_security_group" "web_sg" {
  name        = "${var.project_name}-ec2-web-sg"
  description = "HTTP solo desde el ALB; administracion via SSM"
  vpc_id      = module.network.main_id

  ingress {
    description     = "HTTP solo desde el ALB"
    from_port       = 80
    to_port         = 80
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-ec2-web-sg"
  }
}

# =========================================================
# 4. BALANCEADOR DE CARGA (ALB)
# =========================================================

resource "aws_lb" "web_alb" {
  name               = "${var.project_name}-alb-v2"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = [module.network.public_a_id, module.network.public_b_id]

  tags = {
    Name = "${var.project_name}-alb-v2"
  }
}

resource "aws_lb_target_group" "web_tg" {
  name     = "${var.project_name}-tg-v3"
  port     = 80
  protocol = "HTTP"
  vpc_id   = module.network.main_id

  health_check {
    path                = "/actuator/health"
    protocol            = "HTTP"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 2
  }

  tags = {
    Name = "${var.project_name}-tg-v3"
  }
}

resource "aws_lb_listener" "web_listener" {
  load_balancer_arn = aws_lb.web_alb.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = var.alb_certificate_arn == "" ? "forward" : "redirect"
    target_group_arn = var.alb_certificate_arn == "" ? aws_lb_target_group.web_tg.arn : null
    dynamic "redirect" {
      for_each = var.alb_certificate_arn == "" ? [] : [1]
      content {
        port        = "443"
        protocol    = "HTTPS"
        status_code = "HTTP_301"
      }
    }
  }
}

# =========================================================
# 5. CÓMPUTO & AUTO SCALING GROUP (ASG EN SUBREDES PRIVADAS)
# =========================================================

module "compute" {
  source                = "./modules/compute"
  project_name          = var.project_name
  environment           = var.environment
  student               = var.student
  instance_type         = var.instance_type
  instance_profile_name = aws_iam_instance_profile.backend.name
  security_group_id     = aws_security_group.web_sg.id
  private_subnet_ids    = [module.network.private_a_id, module.network.private_b_id]
  target_group_arn      = aws_lb_target_group.web_tg.arn
  user_data = base64encode(templatefile("${path.module}/templates/bootstrap.sh.tftpl", {
    region  = var.aws_region
    bucket  = aws_s3_bucket.artifacts.id
    release = var.backend_release
    config  = jsonencode(local.deploy_config)
  }))
  depends_on = [module.network, aws_iam_role_policy.backend, aws_iam_role_policy_attachment.ssm]
}
