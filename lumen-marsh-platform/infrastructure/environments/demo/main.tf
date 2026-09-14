# Bootstrap placeholder. No cloud resources are created while
# enable_demo_resources remains false (Phase 9 exit criteria).

locals {
  environment = "demo"
}

module "network" {
  source = "../../modules/network"
  count  = var.enable_demo_resources ? 1 : 0

  project_name = var.project_name
  environment  = local.environment
}

module "identity" {
  source = "../../modules/identity"
  count  = var.enable_demo_resources ? 1 : 0

  project_name = var.project_name
  environment  = local.environment
  aws_region   = var.aws_region

  # Future AWS demo console origins (CloudFront / ALB) append here when known.
  additional_console_callback_urls = var.demo_console_callback_urls
  additional_console_logout_urls   = var.demo_console_logout_urls
}

module "container_registry" {
  source = "../../modules/container-registry"
  count  = var.enable_demo_resources ? 1 : 0

  project_name = var.project_name
  environment  = local.environment
}

module "static_site" {
  source = "../../modules/static-site"
  count  = var.enable_demo_resources ? 1 : 0

  project_name = var.project_name
  environment  = local.environment
}

module "compute" {
  source = "../../modules/compute"
  count  = var.enable_demo_resources ? 1 : 0

  project_name = var.project_name
  environment  = local.environment
}

module "configuration" {
  source = "../../modules/configuration"
  count  = var.enable_demo_resources ? 1 : 0

  project_name = var.project_name
  environment  = local.environment
}

module "observability" {
  source = "../../modules/observability"
  count  = var.enable_demo_resources ? 1 : 0

  project_name = var.project_name
  environment  = local.environment
}
