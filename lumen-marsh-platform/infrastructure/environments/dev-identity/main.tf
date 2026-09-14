# Development Cognito only. Does not create network, compute, or other demo resources.

module "identity" {
  source = "../../modules/identity"

  project_name   = var.project_name
  environment    = "dev"
  aws_region     = var.aws_region
  user_pool_name = "lumen-marsh-dev"

  enable_machine_client = true
}
