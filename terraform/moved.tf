moved {
  from = aws_vpc.main
  to   = module.network.aws_vpc.main
}
moved {
  from = aws_subnet.public_a
  to   = module.network.aws_subnet.public_a
}
moved {
  from = aws_subnet.public_b
  to   = module.network.aws_subnet.public_b
}
moved {
  from = aws_subnet.private_a
  to   = module.network.aws_subnet.private_a
}
moved {
  from = aws_subnet.private_b
  to   = module.network.aws_subnet.private_b
}
moved {
  from = aws_internet_gateway.gw
  to   = module.network.aws_internet_gateway.gw
}
moved {
  from = aws_eip.nat_eip
  to   = module.network.aws_eip.nat_eip
}
moved {
  from = aws_nat_gateway.nat_gw
  to   = module.network.aws_nat_gateway.nat_gw
}
moved {
  from = aws_route_table.public_rt
  to   = module.network.aws_route_table.public_rt
}
moved {
  from = aws_route_table_association.public_a_assoc
  to   = module.network.aws_route_table_association.public_a_assoc
}
moved {
  from = aws_route_table_association.public_b_assoc
  to   = module.network.aws_route_table_association.public_b_assoc
}
moved {
  from = aws_route_table.private_rt
  to   = module.network.aws_route_table.private_rt
}
moved {
  from = aws_route_table_association.private_a_assoc
  to   = module.network.aws_route_table_association.private_a_assoc
}
moved {
  from = aws_route_table_association.private_b_assoc
  to   = module.network.aws_route_table_association.private_b_assoc
}
moved {
  from = aws_eip.nat_b
  to   = module.network.aws_eip.nat_b
}
moved {
  from = aws_nat_gateway.nat_b
  to   = module.network.aws_nat_gateway.nat_b
}
moved {
  from = aws_route_table.private_b_rt
  to   = module.network.aws_route_table.private_b_rt
}
moved {
  from = aws_launch_template.web_template
  to   = module.compute.aws_launch_template.web_template
}
moved {
  from = aws_autoscaling_group.web_asg
  to   = module.compute.aws_autoscaling_group.web_asg
}
moved {
  from = aws_autoscaling_policy.cpu_policy
  to   = module.compute.aws_autoscaling_policy.cpu_policy
}
