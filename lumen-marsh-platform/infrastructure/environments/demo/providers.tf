provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "lumen-marsh"
      Environment = "demo"
      ManagedBy   = "opentofu"
    }
  }
}
