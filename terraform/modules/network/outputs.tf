output "main_id" {
  value = aws_vpc.main.id
}
output "public_a_id" {
  value = aws_subnet.public_a.id
}
output "public_b_id" {
  value = aws_subnet.public_b.id
}
output "private_a_id" {
  value = aws_subnet.private_a.id
}
output "private_b_id" {
  value = aws_subnet.private_b.id
}
output "gw_id" {
  value = aws_internet_gateway.gw.id
}
output "nat_eip_id" {
  value = aws_eip.nat_eip.id
}
output "nat_gw_id" {
  value = aws_nat_gateway.nat_gw.id
}
output "public_rt_id" {
  value = aws_route_table.public_rt.id
}
output "public_a_assoc_id" {
  value = aws_route_table_association.public_a_assoc.id
}
output "public_b_assoc_id" {
  value = aws_route_table_association.public_b_assoc.id
}
output "private_rt_id" {
  value = aws_route_table.private_rt.id
}
output "private_a_assoc_id" {
  value = aws_route_table_association.private_a_assoc.id
}
output "private_b_assoc_id" {
  value = aws_route_table_association.private_b_assoc.id
}
output "nat_b_eip_id" {
  value = aws_eip.nat_b.id
}
output "nat_b_id" {
  value = aws_nat_gateway.nat_b.id
}
output "private_b_rt_id" {
  value = aws_route_table.private_b_rt.id
}
output "nat_gateway_count" {
  value = length([aws_nat_gateway.nat_gw.id, aws_nat_gateway.nat_b.id])
}
output "private_subnets_public_ip" {
  value = [aws_subnet.private_a.map_public_ip_on_launch, aws_subnet.private_b.map_public_ip_on_launch]
}
