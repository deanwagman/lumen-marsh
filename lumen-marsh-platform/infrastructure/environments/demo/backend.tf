# Remote backend placeholder. Keep unused during Phase 9 local bootstrap.
# Before CI/shared apply, configure an encrypted S3 (or compatible) backend
# with versioning, locking, and restricted IAM access.
#
# terraform {
#   backend "s3" {
#     bucket         = "lumen-marsh-tofu-state"
#     key            = "demo/terraform.tfstate"
#     region         = "us-east-1"
#     encrypt        = true
#     dynamodb_table = "lumen-marsh-tofu-locks"
#   }
# }
