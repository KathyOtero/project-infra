data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }
}

# Launch Template para el Auto Scaling Group
resource "aws_launch_template" "web_template" {
  name_prefix   = "${var.project_name}-template-"
  image_id      = data.aws_ami.amazon_linux_2023.id
  instance_type = var.instance_type
  iam_instance_profile {
    name = var.instance_profile_name
  }
  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }
  block_device_mappings {
    device_name = "/dev/xvda"
    ebs {
      volume_size = 20
      volume_type = "gp3"
      encrypted   = true
    }
  }

  network_interfaces {
    associate_public_ip_address = false
    security_groups             = [var.security_group_id]
  }

  user_data = var.user_data

  tag_specifications {
    resource_type = "instance"
    tags = {
      Name        = "${var.project_name}-asg-node"
      Project     = var.project_name
      Environment = var.environment
      Student     = var.student
      Role        = "backend"
    }
  }

  lifecycle {
    create_before_destroy = true
  }
}

# Auto Scaling Group (Desplegado en subredes privadas con nombre v2)
resource "aws_autoscaling_group" "web_asg" {
  name                = "${var.project_name}-asg-v2"
  vpc_zone_identifier = var.private_subnet_ids
  target_group_arns   = [var.target_group_arn]

  min_size         = 2
  desired_capacity = 2
  max_size         = 4

  health_check_type         = "ELB"
  health_check_grace_period = 900

  launch_template {
    id      = aws_launch_template.web_template.id
    version = aws_launch_template.web_template.latest_version
  }

  instance_refresh {
    strategy = "Rolling"
    preferences {
      min_healthy_percentage = 100
      max_healthy_percentage = 200
      instance_warmup        = 300
    }
  }
  tag {
    key                 = "Project"
    value               = var.project_name
    propagate_at_launch = true
  }
  tag {
    key                 = "Environment"
    value               = var.environment
    propagate_at_launch = true
  }
  tag {
    key                 = "Student"
    value               = var.student
    propagate_at_launch = true
  }

  lifecycle {
    create_before_destroy = true
    ignore_changes        = [desired_capacity]
  }
}

# Política de Escalado Automático basada en CPU (> 70%)
resource "aws_autoscaling_policy" "cpu_policy" {
  name                   = "${var.project_name}-cpu-policy"
  policy_type            = "TargetTrackingScaling"
  autoscaling_group_name = aws_autoscaling_group.web_asg.name

  target_tracking_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ASGAverageCPUUtilization"
    }
    target_value = 70.0
  }
}
