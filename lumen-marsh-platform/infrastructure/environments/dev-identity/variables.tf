variable "aws_region" {
  type        = string
  description = "AWS region for the development Cognito pool."
  default     = "us-east-1"
}

variable "project_name" {
  type    = string
  default = "lumen-marsh"
}
