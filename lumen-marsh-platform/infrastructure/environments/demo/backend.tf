# State bucket and lock table are created by infrastructure/bootstrap.
# CI initializes with -backend=false and does not apply.
# Bucket, table, and region must match the bootstrap that was applied.
terraform {
  backend "s3" {
    bucket         = "lumen-marsh-tofu-state"
    key            = "demo/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "lumen-marsh-tofu-locks"
  }
}
