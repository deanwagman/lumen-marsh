variable "aws_region" {
  type        = string
  description = "AWS region for the inexpensive demo environment."
  default     = "us-east-1"
}

variable "enable_demo_resources" {
  type        = bool
  description = "When false (default), OpenTofu validates without creating AWS resources."
  default     = false
}

variable "project_name" {
  type        = string
  description = "Short project name used for tagging."
  default     = "lumen-marsh"
}

variable "demo_console_callback_urls" {
  type        = list(string)
  description = "Additional Cognito callback URLs for the AWS demo console origin."
  default     = []
}

variable "demo_console_logout_urls" {
  type        = list(string)
  description = "Additional Cognito logout URLs for the AWS demo console origin."
  default     = []
}
