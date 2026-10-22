variable "aws_region" {
  type        = string
  description = "Region for the state bucket and lock table. Must match the demo backend region."
  default     = "us-east-1"
}

variable "project_name" {
  type        = string
  description = "Short project name used for tagging."
  default     = "lumen-marsh"
}

variable "state_bucket_name" {
  type        = string
  description = "Globally unique name for the encrypted demo state bucket. Change the demo backend to match if this default is taken."
  default     = "lumen-marsh-tofu-state"
}

variable "lock_table_name" {
  type        = string
  description = "DynamoDB table that locks demo state. Change the demo backend to match if this name changes."
  default     = "lumen-marsh-tofu-locks"
}
