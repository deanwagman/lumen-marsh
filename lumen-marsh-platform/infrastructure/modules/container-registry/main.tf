variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

locals {
  name_prefix = "${var.project_name}-${var.environment}"
  # Images the demo compose file builds. Postgres and Caddy stay anonymous Hub images.
  repositories = toset([
    "venueops-api",
    "environmental-monitor",
    "park-flow-intelligence",
    "reliability-intelligence",
    "venueops-console",
    "lumen-marsh-app",
    "cutaway",
  ])
}

resource "aws_ecr_repository" "app" {
  for_each = local.repositories

  name                 = "${local.name_prefix}/${each.key}"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = {
    Component = "container-registry"
  }
}

output "repository_urls" {
  value = { for name, repo in aws_ecr_repository.app : name => repo.repository_url }
}

output "repository_arns" {
  value = [for repo in aws_ecr_repository.app : repo.arn]
}

output "registry_host" {
  value = split("/", aws_ecr_repository.app["venueops-api"].repository_url)[0]
}

output "image_prefix" {
  value = local.name_prefix
}
