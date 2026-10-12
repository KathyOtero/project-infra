variable "environment" {
  type = string
}

variable "instance_profile_name" {
  type = string
}

variable "instance_type" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "project_name" {
  type = string
}

variable "security_group_id" {
  type = string
}

variable "student" {
  type = string
}

variable "target_group_arn" {
  type = string
}

variable "user_data" {
  type = string
}
