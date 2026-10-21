variable "aws_region" {
  type        = string
  description = "AWS region for the visit host. Override this at apply time."
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

variable "demo_host" {
  type        = string
  description = "DNS name for the visit origin. Apply input; not a committed value."
  default     = ""

  validation {
    condition = (
      var.demo_host == "" ||
      can(regex("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$", var.demo_host))
    )
    error_message = "demo_host must be a DNS name such as visit.example.com, without a scheme or path."
  }
}

variable "demo_acme_email" {
  type        = string
  description = "ACME contact email for the visit certificate. Apply input; stored in Secrets Manager."
  default     = ""
  sensitive   = true

  validation {
    condition = (
      var.demo_acme_email == "" ||
      can(regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", var.demo_acme_email))
    )
    error_message = "demo_acme_email must be one email address."
  }
}

variable "nws_user_agent" {
  type        = string
  description = "Weather service contact string. Empty uses a host-based application name."
  default     = ""

  validation {
    condition = (
      var.nws_user_agent == "" ||
      (
        length(var.nws_user_agent) >= 10 &&
        can(regex(".*(@|\\(|https?://).*", var.nws_user_agent))
      )
    )
    error_message = "nws_user_agent must identify the application and include contact information."
  }
}

variable "visit_source_repository_url" {
  type        = string
  description = "Git repository the visit host clones for the demo compose stack."
  default     = "https://github.com/deanwagman/lumen-marsh.git"

  validation {
    condition     = can(regex("^https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+(\\.git)?$", var.visit_source_repository_url))
    error_message = "visit_source_repository_url must be an https GitHub repository URL."
  }
}

variable "visit_source_revision" {
  type        = string
  description = "Git branch, tag, or commit the visit host checks out."
  default     = "main"

  validation {
    condition     = can(regex("^[A-Za-z0-9._/-]+$", var.visit_source_revision))
    error_message = "visit_source_revision must contain only letters, numbers, and . _ / - characters."
  }
}
