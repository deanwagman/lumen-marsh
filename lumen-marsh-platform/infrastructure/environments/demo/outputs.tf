output "bootstrap_status" {
  description = "Confirms whether demo AWS modules are armed."
  value       = var.enable_demo_resources ? "resources-enabled" : "validate-only"
}

output "aws_region" {
  value = var.aws_region
}

output "identity_issuer_uri" {
  description = "Cognito issuer URI when demo resources are enabled."
  value       = try(module.identity[0].issuer_uri, null)
}

output "identity_user_pool_id" {
  value = try(module.identity[0].user_pool_id, null)
}

output "identity_console_client_id" {
  value = try(module.identity[0].console_client_id, null)
}

output "identity_cognito_domain" {
  value = try(module.identity[0].cognito_domain, null)
}

output "identity_oauth_audience" {
  description = "Resource-server identifier used in scope names (venueops/). Not a JWT aud claim."
  value       = try(module.identity[0].oauth_audience, null)
}

output "COGNITO_ISSUER_URI" {
  value = try(module.identity[0].issuer_uri, null)
}

output "COGNITO_DOMAIN" {
  value = try(module.identity[0].cognito_domain, null)
}

output "COGNITO_USER_POOL_ID" {
  value = try(module.identity[0].user_pool_id, null)
}

output "CONSOLE_CLIENT_ID" {
  value = try(module.identity[0].console_client_id, null)
}

output "VENUEOPS_AUDIENCE" {
  description = "Resource-server identifier used in scope names. VenueOps validates client_id, not aud."
  value       = try(module.identity[0].oauth_audience, null)
}

output "VENUEOPS_ALLOWED_CLIENT_IDS" {
  description = "Console client_id for VenueOps JWT validation. Append the monitor client when that path is enabled."
  value       = try(module.identity[0].console_client_id, null)
}

output "identity_monitor_secret_arn" {
  value = try(module.identity[0].monitor_secret_arn, null)
}

output "identity_token_url" {
  value = try(module.identity[0].token_url, null)
}
