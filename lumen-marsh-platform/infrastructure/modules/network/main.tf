variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

data "aws_availability_zones" "available" {
  state = "available"

  filter {
    name   = "opt-in-status"
    values = ["opt-in-not-required"]
  }

  filter {
    name   = "zone-type"
    values = ["availability-zone"]
  }
}

locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

resource "aws_vpc" "visit" {
  cidr_block           = "10.42.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name      = "${local.name_prefix}-visit"
    Component = "network"
  }
}

resource "aws_internet_gateway" "visit" {
  vpc_id = aws_vpc.visit.id

  tags = {
    Name      = "${local.name_prefix}-visit"
    Component = "network"
  }
}

resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.visit.id
  cidr_block              = "10.42.1.0/24"
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true

  tags = {
    Name      = "${local.name_prefix}-visit-public"
    Component = "network"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.visit.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.visit.id
  }

  tags = {
    Name      = "${local.name_prefix}-visit-public"
    Component = "network"
  }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

resource "aws_security_group" "visit" {
  name        = "${local.name_prefix}-visit"
  description = "HTTP and HTTPS for the visit host. Postgres and SSH stay closed."
  vpc_id      = aws_vpc.visit.id

  ingress {
    description = "HTTP for the certificate redirect"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS for the visit origin"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Pull images, obtain a certificate, and reach AWS APIs"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name      = "${local.name_prefix}-visit"
    Component = "network"
  }
}

resource "aws_default_security_group" "visit" {
  vpc_id = aws_vpc.visit.id

  tags = {
    Name      = "${local.name_prefix}-default-closed"
    Component = "network"
  }
}

output "vpc_id" {
  value = aws_vpc.visit.id
}

output "public_subnet_id" {
  value = aws_subnet.public.id
}

output "security_group_id" {
  value = aws_security_group.visit.id
}
