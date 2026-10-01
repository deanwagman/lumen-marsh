output "COGNITO_ISSUER_URI" {
  value = module.identity.issuer_uri
}

output "COGNITO_DOMAIN" {
  value = module.identity.cognito_domain
}

output "COGNITO_USER_POOL_ID" {
  value = module.identity.user_pool_id
}

output "CONSOLE_CLIENT_ID" {
  value = module.identity.console_client_id
}

output "MONITOR_SECRET_ARN" {
  description = "Secrets Manager ARN containing the Environmental Monitor client credentials."
  value       = module.identity.monitor_secret_arn
}

output "FLOW_SECRET_ARN" {
  description = "Secrets Manager ARN containing the Park Flow Intelligence client credentials."
  value       = module.identity.flow_secret_arn
}

output "RELIABILITY_SECRET_ARN" {
  description = "Secrets Manager ARN containing the Reliability Intelligence client credentials."
  value       = module.identity.reliability_secret_arn
}

output "aws_region" {
  value = var.aws_region
}
