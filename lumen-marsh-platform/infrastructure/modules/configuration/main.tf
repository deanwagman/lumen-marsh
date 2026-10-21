terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
    random = {
      source = "hashicorp/random"
    }
  }
}

variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "demo_host" {
  type = string
}

variable "demo_origin" {
  type = string
}

variable "issuer_uri" {
  type = string
}

variable "allowed_client_ids" {
  type = string
}

variable "nws_user_agent" {
  type = string
}

variable "acme_email" {
  type      = string
  sensitive = true
}

variable "source_repository_url" {
  type = string
}

variable "source_revision" {
  type = string
}

variable "ecr_registry" {
  type = string
}

variable "image_prefix" {
  type = string
}

variable "image_tag" {
  type    = string
  default = "demo"
}

variable "lease_duration" {
  type    = string
  default = "PT10M"
}

variable "inactivity_timeout" {
  type    = string
  default = "PT3M"
}

variable "hard_maximum" {
  type    = string
  default = "PT15M"
}

variable "expiry_scan" {
  type    = string
  default = "PT15S"
}

variable "monitor_secret_arn" {
  type = string
}

variable "flow_secret_arn" {
  type = string
}

variable "reliability_secret_arn" {
  type = string
}

variable "monitor_secret_version_id" {
  type = string
}

variable "flow_secret_version_id" {
  type = string
}

variable "reliability_secret_version_id" {
  type = string
}

locals {
  name_prefix = "${var.project_name}-${var.environment}"
  prefix      = "/${var.project_name}/${var.environment}"
}

resource "random_password" "reset" {
  length  = 48
  special = false
}

resource "random_password" "venueops_db" {
  length  = 32
  special = false
}

resource "random_password" "environmental_db" {
  length  = 32
  special = false
}

resource "aws_secretsmanager_secret" "reset" {
  name                    = "${local.name_prefix}/visit/reset-secret"
  recovery_window_in_days = 7

  tags = {
    Component = "configuration"
  }
}

resource "aws_secretsmanager_secret_version" "reset" {
  secret_id     = aws_secretsmanager_secret.reset.id
  secret_string = random_password.reset.result
}

resource "aws_secretsmanager_secret" "venueops_db" {
  name                    = "${local.name_prefix}/visit/venueops-db"
  recovery_window_in_days = 7

  tags = {
    Component = "configuration"
  }
}

resource "aws_secretsmanager_secret_version" "venueops_db" {
  secret_id     = aws_secretsmanager_secret.venueops_db.id
  secret_string = random_password.venueops_db.result
}

resource "aws_secretsmanager_secret" "environmental_db" {
  name                    = "${local.name_prefix}/visit/environmental-db"
  recovery_window_in_days = 7

  tags = {
    Component = "configuration"
  }
}

resource "aws_secretsmanager_secret_version" "environmental_db" {
  secret_id     = aws_secretsmanager_secret.environmental_db.id
  secret_string = random_password.environmental_db.result
}

resource "aws_secretsmanager_secret" "acme" {
  name                    = "${local.name_prefix}/visit/acme-email"
  recovery_window_in_days = 7

  tags = {
    Component = "configuration"
  }
}

resource "aws_secretsmanager_secret_version" "acme" {
  secret_id     = aws_secretsmanager_secret.acme.id
  secret_string = var.acme_email
}

locals {
  parameters = {
    host                        = var.demo_host
    origin                      = var.demo_origin
    security-mode               = "OIDC"
    historical-seed             = "true"
    lease-duration              = var.lease_duration
    inactivity-timeout          = var.inactivity_timeout
    hard-maximum                = var.hard_maximum
    expiry-scan                 = var.expiry_scan
    issuer-uri                  = var.issuer_uri
    allowed-client-ids          = var.allowed_client_ids
    nws-user-agent              = var.nws_user_agent
    source-repository-url       = var.source_repository_url
    source-revision             = var.source_revision
    ecr-registry                = var.ecr_registry
    image-prefix                = var.image_prefix
    image-tag                   = var.image_tag
    reset-secret-arn            = aws_secretsmanager_secret.reset.arn
    venueops-db-secret-arn      = aws_secretsmanager_secret.venueops_db.arn
    environmental-db-secret-arn = aws_secretsmanager_secret.environmental_db.arn
    acme-secret-arn             = aws_secretsmanager_secret.acme.arn
    monitor-secret-arn          = var.monitor_secret_arn
    flow-secret-arn             = var.flow_secret_arn
    reliability-secret-arn      = var.reliability_secret_arn
  }
}

resource "aws_ssm_parameter" "setting" {
  for_each = local.parameters

  name  = "${local.prefix}/${each.key}"
  type  = "String"
  value = each.value

  tags = {
    Component = "configuration"
  }
}

output "parameter_prefix" {
  value = local.prefix
}

output "reset_secret_arn" {
  value = aws_secretsmanager_secret.reset.arn
}

output "venueops_db_secret_arn" {
  value = aws_secretsmanager_secret.venueops_db.arn
}

output "environmental_db_secret_arn" {
  value = aws_secretsmanager_secret.environmental_db.arn
}

output "acme_secret_arn" {
  value = aws_secretsmanager_secret.acme.arn
}

output "configuration_generation" {
  value = sha256(join("|", concat(
    [for name in sort(keys(aws_ssm_parameter.setting)) : "${name}=${aws_ssm_parameter.setting[name].version}"],
    [
      aws_secretsmanager_secret_version.reset.version_id,
      aws_secretsmanager_secret_version.venueops_db.version_id,
      aws_secretsmanager_secret_version.environmental_db.version_id,
      aws_secretsmanager_secret_version.acme.version_id,
      var.monitor_secret_version_id,
      var.flow_secret_version_id,
      var.reliability_secret_version_id,
    ],
  )))
}
