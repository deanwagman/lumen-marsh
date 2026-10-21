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

output "identity_monitor_client_id" {
  value = try(module.identity[0].monitor_client_id, null)
}

output "identity_flow_client_id" {
  value = try(module.identity[0].flow_client_id, null)
}

output "identity_reliability_client_id" {
  value = try(module.identity[0].reliability_client_id, null)
}

output "identity_cognito_domain" {
  value = try(module.identity[0].cognito_domain, null)
}

output "identity_oauth_audience" {
  description = "Resource-server identifier used in scope names (venueops/). Not a JWT aud claim."
  value       = try(module.identity[0].oauth_audience, null)
}

output "identity_token_url" {
  value = try(module.identity[0].token_url, null)
}

output "identity_monitor_secret_arn" {
  value = try(module.identity[0].monitor_secret_arn, null)
}

output "identity_flow_secret_arn" {
  value = try(module.identity[0].flow_secret_arn, null)
}

output "identity_reliability_secret_arn" {
  value = try(module.identity[0].reliability_secret_arn, null)
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
  description = "Console, Environmental Monitor, Park Flow, and Reliability client ids."
  value = try(join(",", [
    module.identity[0].console_client_id,
    module.identity[0].monitor_client_id,
    module.identity[0].flow_client_id,
    module.identity[0].reliability_client_id,
  ]), null)
}

output "visit_public_ip" {
  description = "Public address for the visit DNS A record."
  value       = try(module.compute[0].public_ip, null)
}

output "visit_public_dns" {
  value = try(module.compute[0].public_dns, null)
}

output "visit_instance_id" {
  value = try(module.compute[0].instance_id, null)
}

output "ecr_registry_host" {
  value = try(module.container_registry[0].registry_host, null)
}

output "ecr_repository_urls" {
  value = try(module.container_registry[0].repository_urls, null)
}
