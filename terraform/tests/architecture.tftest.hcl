mock_provider "aws" {
  mock_data "aws_ami" {
    defaults = { id = "ami-0123456789abcdef0" }
  }
  mock_resource "aws_db_instance" {
    defaults = {
      master_user_secret = [{ secret_arn = "arn:aws:secretsmanager:us-east-1:123456789012:secret:database", secret_status = "active", kms_key_id = "arn:aws:kms:us-east-1:123456789012:key/example" }]
    }
  }
}

run "ha_architecture" {
  command = plan
  assert {
    condition     = module.network.nat_gateway_count == 2 && alltrue([for enabled in module.network.private_subnets_public_ip : !enabled])
    error_message = "Each AZ needs a NAT and backend subnets must not assign public IPs."
  }
  assert {
    condition     = aws_db_instance.mysql.multi_az && !aws_db_instance.mysql.publicly_accessible && aws_db_instance.mysql.storage_encrypted
    error_message = "Database must be private, encrypted and Multi-AZ."
  }
  assert {
    condition     = module.compute.asg.min_size == 2 && module.compute.asg.max_size == 4
    error_message = "Proposal requires minimum two and maximum four backends."
  }
  assert {
    condition     = aws_lb_target_group.web_tg.health_check[0].path == "/actuator/health"
    error_message = "ALB must check application health."
  }
  assert {
    condition     = module.compute.launch_template.metadata_options[0].http_tokens == "required" && !module.compute.launch_template.network_interfaces[0].associate_public_ip_address
    error_message = "EC2 must require IMDSv2 and have no public IP."
  }
  assert {
    condition     = alltrue([for rule in aws_security_group.web_sg.ingress : rule.from_port != 22])
    error_message = "SSM administration must not expose SSH."
  }
  assert {
    condition     = aws_cloudfront_distribution.frontend.ordered_cache_behavior[0].cache_policy_id == "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"
    error_message = "Authenticated API responses must never be cached."
  }
}

run "reject_incomplete_custom_domain" {
  command = plan
  variables {
    domain_name = "caribexperience.example.org"
  }
  expect_failures = [aws_cloudfront_distribution.frontend]
}
