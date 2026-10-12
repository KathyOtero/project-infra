output "asg" {
  value = aws_autoscaling_group.web_asg
}
output "launch_template" {
  value = aws_launch_template.web_template
}
