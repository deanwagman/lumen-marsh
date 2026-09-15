# Cognito identity for the VenueOps console (development first).
# Do not store user passwords in this module. Treat OpenTofu state as sensitive
# if a machine-client secret is later enabled.

variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "user_pool_name" {
  type        = string
  default     = "lumen-marsh-dev"
  description = "Cognito user pool display name."
}

variable "console_callback_urls" {
  type = list(string)
  default = [
    "http://localhost:5173/auth/callback",
    "http://127.0.0.1:5173/auth/callback",
  ]
}

variable "console_logout_urls" {
  type = list(string)
  default = [
    "http://localhost:5173",
    "http://127.0.0.1:5173",
  ]
}

variable "additional_console_callback_urls" {
  type    = list(string)
  default = []
}

variable "additional_console_logout_urls" {
  type    = list(string)
  default = []
}

variable "enable_machine_client" {
  type        = bool
  default     = false
  description = "When true, also provision Environmental Monitor and reliability-integration client-credentials apps. Leave false until those paths are in scope."
}

data "aws_caller_identity" "current" {}

locals {
  name_prefix = "${var.project_name}-${var.environment}"
  # Scope prefix only. Cognito access tokens identify the app with client_id, not aud=venueops.
  resource_server_identifier = "venueops"
  # Hosted-UI domain prefixes are globally unique. Include the account so apply is repeatable.
  hosted_domain_prefix = "${var.user_pool_name}-${substr(data.aws_caller_identity.current.account_id, -8, 8)}"
  scopes = concat(
    [
      { name = "operator.read", description = "Operator reads" },
      { name = "attractions.command", description = "Attraction commands" },
      { name = "incidents.command", description = "Incident commands" },
      { name = "advisories.publish", description = "Supervisor guest advisory publishing" },
      { name = "weather-recommendations.review", description = "Operator weather recommendation review" },
      { name = "maintenance.read", description = "Operator maintenance reads" },
      { name = "maintenance.command", description = "Operator maintenance commands" },
      { name = "maintenance.inspect", description = "Supervisor maintenance inspection" },
    ],
    var.enable_machine_client ? [
      { name = "weather-recommendations.write", description = "Environmental Monitor ingest" },
      { name = "reliability.write", description = "Reliability recommendation ingest" },
    ] : [],
  )
  console_scopes = [
    "${local.resource_server_identifier}/operator.read",
    "${local.resource_server_identifier}/attractions.command",
    "${local.resource_server_identifier}/incidents.command",
    "${local.resource_server_identifier}/advisories.publish",
    "${local.resource_server_identifier}/weather-recommendations.review",
    "${local.resource_server_identifier}/maintenance.read",
    "${local.resource_server_identifier}/maintenance.command",
    "${local.resource_server_identifier}/maintenance.inspect",
  ]
  monitor_scopes = [
    "${local.resource_server_identifier}/weather-recommendations.write",
  ]
  reliability_scopes = [
    "${local.resource_server_identifier}/reliability.write",
  ]
  callback_urls = concat(var.console_callback_urls, var.additional_console_callback_urls)
  logout_urls   = concat(var.console_logout_urls, var.additional_console_logout_urls)
}

resource "aws_cognito_user_pool" "venueops" {
  name = var.user_pool_name

  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

  password_policy {
    minimum_length    = 12
    require_lowercase = true
    require_numbers   = true
    require_symbols   = true
    require_uppercase = true
  }

  admin_create_user_config {
    allow_admin_create_user_only = true
  }

  tags = {
    Project     = var.project_name
    Environment = var.environment
    Component   = "identity"
  }
}

resource "aws_cognito_user_pool_domain" "venueops" {
  domain       = local.hosted_domain_prefix
  user_pool_id = aws_cognito_user_pool.venueops.id
}

resource "aws_cognito_resource_server" "venueops" {
  identifier   = local.resource_server_identifier
  name         = "VenueOps API"
  user_pool_id = aws_cognito_user_pool.venueops.id

  dynamic "scope" {
    for_each = local.scopes
    content {
      scope_name        = scope.value.name
      scope_description = scope.value.description
    }
  }
}

resource "aws_cognito_user_group" "operators" {
  name         = "operators"
  user_pool_id = aws_cognito_user_pool.venueops.id
  description  = "Day-to-day Control Tower operators"
  precedence   = 20
}

resource "aws_cognito_user_group" "supervisors" {
  name         = "supervisors"
  user_pool_id = aws_cognito_user_pool.venueops.id
  description  = "Elevated Control Tower supervisors"
  precedence   = 10
}

resource "aws_cognito_user_pool_client" "console" {
  name         = "venueops-console"
  user_pool_id = aws_cognito_user_pool.venueops.id

  generate_secret                      = false
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes = concat(
    ["openid", "email", "profile"],
    local.console_scopes,
  )
  supported_identity_providers = ["COGNITO"]
  callback_urls                = local.callback_urls
  logout_urls                  = local.logout_urls
  explicit_auth_flows = [
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_SRP_AUTH",
  ]

  prevent_user_existence_errors = "ENABLED"
  enable_token_revocation       = true
  access_token_validity         = 60
  id_token_validity             = 60
  refresh_token_validity        = 30

  token_validity_units {
    access_token  = "minutes"
    id_token      = "minutes"
    refresh_token = "days"
  }

  depends_on = [aws_cognito_resource_server.venueops]
}

# Machine client is omitted until enable_machine_client is true.
resource "aws_cognito_user_pool_client" "environmental_monitor" {
  count = var.enable_machine_client ? 1 : 0

  name         = "environmental-monitor"
  user_pool_id = aws_cognito_user_pool.venueops.id

  generate_secret                      = true
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["client_credentials"]
  allowed_oauth_scopes                 = local.monitor_scopes
  supported_identity_providers         = ["COGNITO"]
  explicit_auth_flows                  = []

  prevent_user_existence_errors = "ENABLED"
  enable_token_revocation       = true
  access_token_validity         = 60

  token_validity_units {
    access_token = "minutes"
  }

  depends_on = [aws_cognito_resource_server.venueops]
}

resource "aws_secretsmanager_secret" "monitor_client" {
  count = var.enable_machine_client ? 1 : 0
  name  = "${local.name_prefix}/environmental-monitor/oidc"

  tags = {
    Project     = var.project_name
    Environment = var.environment
    Component   = "identity"
  }
}

resource "aws_secretsmanager_secret_version" "monitor_client" {
  count     = var.enable_machine_client ? 1 : 0
  secret_id = aws_secretsmanager_secret.monitor_client[0].id
  secret_string = jsonencode({
    client_id     = aws_cognito_user_pool_client.environmental_monitor[0].id
    client_secret = aws_cognito_user_pool_client.environmental_monitor[0].client_secret
    token_url     = "https://${aws_cognito_user_pool_domain.venueops.domain}.auth.${var.aws_region}.amazoncognito.com/oauth2/token"
    scope         = join(" ", local.monitor_scopes)
  })
}

resource "aws_cognito_user_pool_client" "reliability" {
  count = var.enable_machine_client ? 1 : 0

  name         = "reliability-integration"
  user_pool_id = aws_cognito_user_pool.venueops.id

  generate_secret                      = true
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["client_credentials"]
  allowed_oauth_scopes                 = local.reliability_scopes
  supported_identity_providers         = ["COGNITO"]
  explicit_auth_flows                  = []

  prevent_user_existence_errors = "ENABLED"
  enable_token_revocation       = true
  access_token_validity         = 60

  token_validity_units {
    access_token = "minutes"
  }

  depends_on = [aws_cognito_resource_server.venueops]
}

resource "aws_secretsmanager_secret" "reliability_client" {
  count = var.enable_machine_client ? 1 : 0
  name  = "${local.name_prefix}/reliability-integration/oidc"

  tags = {
    Project     = var.project_name
    Environment = var.environment
    Component   = "identity"
  }
}

resource "aws_secretsmanager_secret_version" "reliability_client" {
  count     = var.enable_machine_client ? 1 : 0
  secret_id = aws_secretsmanager_secret.reliability_client[0].id
  secret_string = jsonencode({
    client_id     = aws_cognito_user_pool_client.reliability[0].id
    client_secret = aws_cognito_user_pool_client.reliability[0].client_secret
    token_url     = "https://${aws_cognito_user_pool_domain.venueops.domain}.auth.${var.aws_region}.amazoncognito.com/oauth2/token"
    scope         = join(" ", local.reliability_scopes)
  })
}

output "issuer_uri" {
  description = "OIDC issuer URL for VenueOps JWT validation"
  value       = "https://cognito-idp.${var.aws_region}.amazonaws.com/${aws_cognito_user_pool.venueops.id}"
}

output "user_pool_id" {
  value = aws_cognito_user_pool.venueops.id
}

output "console_client_id" {
  value = aws_cognito_user_pool_client.console.id
}

output "cognito_domain" {
  value = "https://${aws_cognito_user_pool_domain.venueops.domain}.auth.${var.aws_region}.amazoncognito.com"
}

output "oauth_audience" {
  description = "Resource-server identifier used in scope names (venueops/...). Not a JWT aud claim."
  value       = local.resource_server_identifier
}

output "monitor_secret_arn" {
  description = "Secrets Manager ARN for the machine client, if enabled."
  value       = try(aws_secretsmanager_secret.monitor_client[0].arn, null)
}

output "reliability_secret_arn" {
  description = "Secrets Manager ARN for the reliability machine client, if enabled."
  value       = try(aws_secretsmanager_secret.reliability_client[0].arn, null)
}

output "token_url" {
  value = "https://${aws_cognito_user_pool_domain.venueops.domain}.auth.${var.aws_region}.amazoncognito.com/oauth2/token"
}

output "status" {
  value = "cognito-identity-provisioned"
}
