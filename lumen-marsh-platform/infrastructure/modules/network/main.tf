variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

# Placeholder network module for Phase 9 bootstrap.
# Phase 10 will add VPC, subnet, IGW, and route table resources.
output "status" {
  value = "network-module-placeholder"
}
