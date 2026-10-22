variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "subnet_id" {
  type = string
}

variable "security_group_id" {
  type = string
}

variable "parameter_prefix" {
  type = string
}

variable "configuration_generation" {
  type = string
}

variable "secret_arns" {
  type = list(string)
}

variable "ecr_repository_arns" {
  type = list(string)
}

variable "instance_type" {
  type        = string
  description = "Burstable instance with at least 8 GB of memory."
  default     = "t3.large"

  validation {
    condition     = !can(regex("\\.(nano|micro|small|medium)$", var.instance_type))
    error_message = "Visit host instance type must provide at least 8 GB of memory. t3.large is the default."
  }
}

variable "root_volume_gib" {
  type    = number
  default = 40

  validation {
    condition     = var.root_volume_gib >= 30
    error_message = "Root volume must be at least 30 GiB so the demo images can be built or pulled."
  }
}

locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

data "aws_caller_identity" "current" {}

data "aws_ssm_parameter" "amazon_linux_2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

data "aws_iam_policy_document" "assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "visit" {
  name               = "${local.name_prefix}-visit"
  assume_role_policy = data.aws_iam_policy_document.assume.json

  tags = {
    Component = "compute"
  }
}

resource "aws_iam_role_policy_attachment" "session_manager" {
  role       = aws_iam_role.visit.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_iam_policy_document" "visit" {
  statement {
    sid = "ReadVisitSettings"
    actions = [
      "ssm:GetParameter",
      "ssm:GetParameters",
      "ssm:GetParametersByPath",
    ]
    resources = [
      "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter${var.parameter_prefix}",
      "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter${var.parameter_prefix}/*",
    ]
  }

  statement {
    sid       = "ReadVisitSecrets"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = var.secret_arns
  }

  statement {
    sid       = "EcrAuth"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "EcrPull"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:DescribeImages",
      "ecr:GetDownloadUrlForLayer",
    ]
    resources = var.ecr_repository_arns
  }
}

resource "aws_iam_role_policy" "visit" {
  name   = "${local.name_prefix}-visit"
  role   = aws_iam_role.visit.id
  policy = data.aws_iam_policy_document.visit.json
}

resource "aws_iam_instance_profile" "visit" {
  name = "${local.name_prefix}-visit"
  role = aws_iam_role.visit.name
}

resource "aws_instance" "visit" {
  ami                         = data.aws_ssm_parameter.amazon_linux_2023.value
  instance_type               = var.instance_type
  subnet_id                   = var.subnet_id
  vpc_security_group_ids      = [var.security_group_id]
  iam_instance_profile        = aws_iam_instance_profile.visit.name
  associate_public_ip_address = true
  user_data = templatefile("${path.module}/visit-host.sh.tftpl", {
    aws_region               = var.aws_region
    parameter_prefix         = var.parameter_prefix
    configuration_generation = var.configuration_generation
  })
  user_data_replace_on_change = true

  # Session Manager is the admin path. No SSH key is attached.
  root_block_device {
    volume_type = "gp3"
    volume_size = var.root_volume_gib
    encrypted   = true
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  dynamic "credit_specification" {
    for_each = startswith(var.instance_type, "t") ? [1] : []
    content {
      cpu_credits = "standard"
    }
  }

  tags = {
    Name      = "${local.name_prefix}-visit"
    Component = "compute"
  }

  depends_on = [aws_iam_role_policy.visit]
}

output "public_ip" {
  value = aws_instance.visit.public_ip
}

output "public_dns" {
  value = aws_instance.visit.public_dns
}

output "instance_id" {
  value = aws_instance.visit.id
}
