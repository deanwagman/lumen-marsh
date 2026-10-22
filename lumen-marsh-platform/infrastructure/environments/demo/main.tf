# Demo resources stay uncreated while enable_demo_resources is false.
# CI validates this root with the remote backend disabled.

locals {
  environment = "demo"
  origin      = length(var.demo_host) > 0 ? "https://${var.demo_host}" : ""
}

resource "terraform_data" "visit_inputs" {
  count = var.enable_demo_resources ? 1 : 0

  input = var.demo_host

  lifecycle {
    precondition {
      condition     = length(var.demo_host) > 0 && length(var.demo_acme_email) > 0
      error_message = "demo_host and demo_acme_email are required when enable_demo_resources is true."
    }
  }
}

module "network" {
  source = "../../modules/network"
  count  = var.enable_demo_resources ? 1 : 0

  project_name = var.project_name
  environment  = local.environment

  depends_on = [terraform_data.visit_inputs]
}

module "identity" {
  source = "../../modules/identity"
  count  = var.enable_demo_resources ? 1 : 0

  project_name   = var.project_name
  environment    = local.environment
  aws_region     = var.aws_region
  user_pool_name = "lumen-marsh-demo"

  enable_machine_client = true
  console_callback_urls = ["${local.origin}/auth/callback"]
  console_logout_urls   = [local.origin]

  depends_on = [terraform_data.visit_inputs]
}

module "container_registry" {
  source = "../../modules/container-registry"
  count  = var.enable_demo_resources ? 1 : 0

  project_name = var.project_name
  environment  = local.environment

  depends_on = [terraform_data.visit_inputs]
}

module "static_site" {
  source = "../../modules/static-site"
  count  = var.enable_demo_resources ? 1 : 0

  project_name = var.project_name
  environment  = local.environment
}

module "configuration" {
  source = "../../modules/configuration"
  count  = var.enable_demo_resources ? 1 : 0

  project_name = var.project_name
  environment  = local.environment
  demo_host    = var.demo_host
  demo_origin  = local.origin
  issuer_uri   = module.identity[0].issuer_uri
  allowed_client_ids = join(",", [
    module.identity[0].console_client_id,
    module.identity[0].monitor_client_id,
    module.identity[0].flow_client_id,
    module.identity[0].reliability_client_id,
  ])
  nws_user_agent = (
    var.nws_user_agent != "" ? var.nws_user_agent : "LumenMarshVisit/1.0 (${var.demo_host})"
  )
  acme_email                    = var.demo_acme_email
  source_repository_url         = var.visit_source_repository_url
  source_revision               = var.visit_source_revision
  ecr_registry                  = module.container_registry[0].registry_host
  image_prefix                  = module.container_registry[0].image_prefix
  monitor_secret_arn            = module.identity[0].monitor_secret_arn
  flow_secret_arn               = module.identity[0].flow_secret_arn
  reliability_secret_arn        = module.identity[0].reliability_secret_arn
  monitor_secret_version_id     = module.identity[0].monitor_secret_version_id
  flow_secret_version_id        = module.identity[0].flow_secret_version_id
  reliability_secret_version_id = module.identity[0].reliability_secret_version_id

  depends_on = [terraform_data.visit_inputs]
}

module "compute" {
  source = "../../modules/compute"
  count  = var.enable_demo_resources ? 1 : 0

  project_name             = var.project_name
  environment              = local.environment
  aws_region               = var.aws_region
  subnet_id                = module.network[0].public_subnet_id
  security_group_id        = module.network[0].security_group_id
  parameter_prefix         = module.configuration[0].parameter_prefix
  configuration_generation = module.configuration[0].configuration_generation
  secret_arns = [
    module.configuration[0].reset_secret_arn,
    module.configuration[0].venueops_db_secret_arn,
    module.configuration[0].environmental_db_secret_arn,
    module.configuration[0].acme_secret_arn,
    module.identity[0].monitor_secret_arn,
    module.identity[0].flow_secret_arn,
    module.identity[0].reliability_secret_arn,
  ]
  ecr_repository_arns = module.container_registry[0].repository_arns

  depends_on = [terraform_data.visit_inputs]
}

module "observability" {
  source = "../../modules/observability"
  count  = var.enable_demo_resources ? 1 : 0

  project_name = var.project_name
  environment  = local.environment
}
