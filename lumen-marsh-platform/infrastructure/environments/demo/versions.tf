terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Local state only during bootstrap (Phase 9).
  # Move to an encrypted remote backend before shared apply.
  backend "local" {}
}
