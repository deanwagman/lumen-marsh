provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "lumen-marsh"
      Environment = "dev"
      ManagedBy   = "opentofu"
      Component   = "identity"
    }
  }
}
