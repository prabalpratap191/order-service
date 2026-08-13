terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
  }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Environment = var.environment
      Project     = var.project_name
      ManagedBy   = "Terraform"
    }
  }
}

provider "kubernetes" {
  host                   = module.eks.cluster_endpoint
  cluster_ca_certificate = base64decode(module.eks.cluster_ca_certificate)
  
  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "aws"
    args = [
      "eks",
      "get-token",
      "--cluster-name",
      module.eks.cluster_name
    ]
  }
}

provider "helm" {
  kubernetes {
    host                   = module.eks.cluster_endpoint
    cluster_ca_certificate = base64decode(module.eks.cluster_ca_certificate)
    
    exec {
      api_version = "client.authentication.k8s.io/v1beta1"
      command     = "aws"
      args = [
        "eks",
        "get-token",
        "--cluster-name",
        module.eks.cluster_name
      ]
    }
  }
}

# Data sources
data "aws_caller_identity" "current" {}
data "aws_availability_zones" "available" {
  state = "available"
}

# Local variables
locals {
  cluster_name = "${var.project_name}-${var.environment}-eks"
  
  common_tags = {
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
  
  microservices = [
    "catalog-service",
    "customer-service",
    "order-service",
    "order-history-service",
    "payment-service",
    "work-queue-service"
  ]
}

# VPC Module
module "vpc" {
  source = "../../modules/vpc"
  
  environment         = var.environment
  project_name        = var.project_name
  vpc_cidr            = var.vpc_cidr
  availability_zones  = var.availability_zones
  public_subnet_cidrs = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  
  enable_nat_gateway   = var.enable_nat_gateway
  single_nat_gateway   = var.single_nat_gateway
  enable_dns_hostnames = true
  enable_dns_support   = true
  
  tags = local.common_tags
}

# EKS Module
module "eks" {
  source = "../../modules/eks"
  
  cluster_name    = local.cluster_name
  cluster_version = var.eks_cluster_version
  
  vpc_id          = module.vpc.vpc_id
  subnet_ids      = module.vpc.private_subnet_ids
  
  node_groups = var.eks_node_groups
  
  enable_irsa                     = true
  enable_cluster_autoscaler       = var.enable_cluster_autoscaler
  enable_aws_load_balancer_controller = true
  
  tags = local.common_tags
}

# ECR Repositories
module "ecr" {
  source = "../../modules/ecr"
  
  repositories = local.microservices
  environment  = var.environment
  
  image_tag_mutability = "MUTABLE"
  scan_on_push         = true
  
  lifecycle_policy = {
    rules = [
      {
        rulePriority = 1
        description  = "Keep last 30 images"
        selection = {
          tagStatus     = "any"
          countType     = "imageCountMoreThan"
          countNumber   = 30
        }
        action = {
          type = "expire"
        }
      }
    ]
  }
  
  tags = local.common_tags
}

# RDS PostgreSQL
module "rds" {
  source = "../../modules/rds"
  
  identifier     = "${var.project_name}-${var.environment}-db"
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = var.rds_instance_class
  
  allocated_storage     = var.rds_allocated_storage
  max_allocated_storage = var.rds_max_allocated_storage
  storage_encrypted     = true
  
  db_name  = var.db_name
  username = var.db_username
  port     = 5432
  
  vpc_id                 = module.vpc.vpc_id
  subnet_ids             = module.vpc.private_subnet_ids
  allowed_security_groups = [module.eks.cluster_security_group_id]
  
  backup_retention_period = var.environment == "prod" ? 7 : 3
  backup_window          = "03:00-04:00"
  maintenance_window     = "Mon:04:00-Mon:05:00"
  
  multi_az               = var.environment == "prod" ? true : false
  deletion_protection    = var.environment == "prod" ? true : false
  skip_final_snapshot    = var.environment != "prod"
  
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  
  tags = local.common_tags
}

# ElastiCache Redis
module "elasticache" {
  source = "../../modules/elasticache"
  
  cluster_id           = "${var.project_name}-${var.environment}-redis"
  engine_version       = "7.0"
  node_type            = var.redis_node_type
  num_cache_nodes      = var.environment == "prod" ? 2 : 1
  parameter_group_name = "default.redis7"
  
  vpc_id                  = module.vpc.vpc_id
  subnet_ids              = module.vpc.private_subnet_ids
  allowed_security_groups = [module.eks.cluster_security_group_id]
  
  port                    = 6379
  automatic_failover_enabled = var.environment == "prod" ? true : false
  
  snapshot_retention_limit = var.environment == "prod" ? 5 : 1
  snapshot_window         = "03:00-05:00"
  
  tags = local.common_tags
}

# MSK (Managed Streaming for Kafka)
module "msk" {
  source = "../../modules/msk"
  
  cluster_name    = "${var.project_name}-${var.environment}-kafka"
  kafka_version   = "3.5.1"
  number_of_nodes = var.environment == "prod" ? 3 : 2
  
  broker_instance_type = var.msk_instance_type
  
  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnet_ids
  
  allowed_security_groups = [module.eks.cluster_security_group_id]
  
  ebs_volume_size = var.msk_ebs_volume_size
  
  encryption_in_transit_client_broker = "TLS"
  encryption_in_transit_in_cluster    = true
  
  enhanced_monitoring = var.environment == "prod" ? "PER_TOPIC_PER_PARTITION" : "DEFAULT"
  
  tags = local.common_tags
}

# IAM Roles for Service Accounts (IRSA)
module "iam" {
  source = "../../modules/iam"
  
  cluster_name        = module.eks.cluster_name
  oidc_provider_arn   = module.eks.oidc_provider_arn
  
  service_accounts = [
    {
      name      = "catalog-service-sa"
      namespace = "default"
      policies  = ["arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess"]
    },
    {
      name      = "order-service-sa"
      namespace = "default"
      policies  = ["arn:aws:iam::aws:policy/AmazonSQSFullAccess"]
    },
    {
      name      = "payment-service-sa"
      namespace = "default"
      policies  = ["arn:aws:iam::aws:policy/AmazonSQSFullAccess"]
    }
  ]
  
  tags = local.common_tags
}
