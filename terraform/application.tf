locals {
  prefix = "${var.project_name}-${var.environment}"
  deploy_config = {
    aws_region     = var.aws_region
    backend_image  = "${aws_ecr_repository.backend.repository_url}:${var.backend_release}"
    db_secret_arn  = aws_db_instance.mysql.master_user_secret[0].secret_arn
    jwt_secret_arn = aws_secretsmanager_secret.jwt.arn
    db_endpoint    = aws_db_instance.mysql.address
    log_group      = aws_cloudwatch_log_group.backend.name
    cors_origin    = "https://${var.domain_name != "" ? var.domain_name : aws_cloudfront_distribution.frontend.domain_name}"
  }
}

resource "aws_security_group" "database" {
  name_prefix = "${local.prefix}-db-"
  vpc_id      = module.network.main_id
  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.web_sg.id]
  }
}

resource "aws_db_subnet_group" "database" {
  name       = "${local.prefix}-db"
  subnet_ids = [module.network.private_a_id, module.network.private_b_id]
}

resource "aws_db_instance" "mysql" {
  identifier                  = local.prefix
  engine                      = "mysql"
  engine_version              = "8.0"
  instance_class              = "db.t3.micro"
  allocated_storage           = 20
  max_allocated_storage       = 100
  storage_type                = "gp3"
  storage_encrypted           = true
  db_name                     = "caribexperience"
  username                    = "caribe_admin"
  manage_master_user_password = true
  multi_az                    = true
  publicly_accessible         = false
  db_subnet_group_name        = aws_db_subnet_group.database.name
  vpc_security_group_ids      = [aws_security_group.database.id]
  backup_retention_period     = 7
  deletion_protection         = true
  skip_final_snapshot         = false
  final_snapshot_identifier   = "${local.prefix}-final"
  auto_minor_version_upgrade  = true
}

# Value initialized once by the deployment workflow, never stored in Terraform state.
resource "aws_secretsmanager_secret" "jwt" {
  name                    = "${local.prefix}/jwt"
  recovery_window_in_days = 7
}

resource "aws_ecr_repository" "backend" {
  name                 = "${local.prefix}-backend"
  image_tag_mutability = "IMMUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/${local.prefix}/backend"
  retention_in_days = 30
}

resource "aws_iam_role" "backend" {
  name = "${local.prefix}-backend"
  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [{ Effect = "Allow", Principal = { Service = "ec2.amazonaws.com" }, Action = "sts:AssumeRole" }]
  })
}

resource "aws_iam_instance_profile" "backend" {
  name = local.prefix
  role = aws_iam_role.backend.name
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.backend.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy" "backend" {
  role = aws_iam_role.backend.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      { Effect = "Allow", Action = ["secretsmanager:GetSecretValue"], Resource = [aws_secretsmanager_secret.jwt.arn, aws_db_instance.mysql.master_user_secret[0].secret_arn] },
      { Effect = "Allow", Action = ["ecr:GetAuthorizationToken"], Resource = "*" },
      { Effect = "Allow", Action = ["ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer", "ecr:BatchCheckLayerAvailability"], Resource = aws_ecr_repository.backend.arn },
      { Effect = "Allow", Action = ["s3:GetObject"], Resource = "${aws_s3_bucket.artifacts.arn}/releases/*" },
      { Effect = "Allow", Action = ["logs:CreateLogStream", "logs:PutLogEvents"], Resource = "${aws_cloudwatch_log_group.backend.arn}:*" }
    ]
  })
}

resource "aws_s3_bucket" "artifacts" {
  bucket_prefix = "${local.prefix}-artifacts-"
}
resource "aws_s3_bucket" "frontend" {
  bucket_prefix = "${local.prefix}-frontend-"
}
resource "aws_s3_bucket" "ssm" {
  bucket_prefix = "${local.prefix}-ssm-"
}

locals {
  buckets = { artifacts = aws_s3_bucket.artifacts.id, frontend = aws_s3_bucket.frontend.id, ssm = aws_s3_bucket.ssm.id }
}
resource "aws_s3_bucket_public_access_block" "application" {
  for_each                = local.buckets
  bucket                  = each.value
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
resource "aws_s3_bucket_server_side_encryption_configuration" "application" {
  for_each = local.buckets
  bucket   = each.value
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}
resource "aws_s3_bucket_versioning" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id
  versioning_configuration {
    status = "Enabled"
  }
}
resource "aws_s3_bucket_lifecycle_configuration" "ssm" {
  bucket = aws_s3_bucket.ssm.id
  rule {
    id     = "expire-transfers"
    status = "Enabled"
    filter { prefix = "" }
    expiration { days = 1 }
  }
}

resource "aws_cloudwatch_metric_alarm" "unhealthy" {
  alarm_name          = "${local.prefix}-unhealthy-backends"
  namespace           = "AWS/ApplicationELB"
  metric_name         = "UnHealthyHostCount"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 2
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  dimensions          = { LoadBalancer = aws_lb.web_alb.arn_suffix, TargetGroup = aws_lb_target_group.web_tg.arn_suffix }
}
