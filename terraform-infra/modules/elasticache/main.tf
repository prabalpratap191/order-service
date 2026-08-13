# ElastiCache Subnet Group
resource "aws_elasticache_subnet_group" "main" {
  name       = "${var.cluster_id}-subnet-group"
  subnet_ids = var.subnet_ids

  tags = merge(
    var.tags,
    {
      Name = "${var.cluster_id}-subnet-group"
    }
  )
}

# Security Group
resource "aws_security_group" "main" {
  name        = "${var.cluster_id}-sg"
  description = "Security group for ElastiCache Redis cluster"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = var.port
    to_port         = var.port
    protocol        = "tcp"
    security_groups = var.allowed_security_groups
    description     = "Allow Redis access from application"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound traffic"
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.cluster_id}-sg"
    }
  )
}

# ElastiCache Replication Group (for cluster mode)
resource "aws_elasticache_replication_group" "main" {
  count = var.num_cache_nodes > 1 || var.automatic_failover_enabled ? 1 : 0

  replication_group_id       = var.cluster_id
  replication_group_description = "Redis cluster for ${var.cluster_id}"
  
  engine               = "redis"
  engine_version       = var.engine_version
  node_type           = var.node_type
  num_cache_clusters  = var.num_cache_nodes
  parameter_group_name = var.parameter_group_name
  port                = var.port
  
  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.main.id]
  
  automatic_failover_enabled = var.automatic_failover_enabled
  multi_az_enabled          = var.multi_az_enabled
  
  at_rest_encryption_enabled = var.at_rest_encryption_enabled
  transit_encryption_enabled = var.transit_encryption_enabled
  auth_token                = var.transit_encryption_enabled ? random_password.auth_token[0].result : null
  
  snapshot_retention_limit = var.snapshot_retention_limit
  snapshot_window         = var.snapshot_window
  maintenance_window      = var.maintenance_window
  
  auto_minor_version_upgrade = var.auto_minor_version_upgrade
  
  notification_topic_arn = var.notification_topic_arn
  
  tags = var.tags
}

# ElastiCache Cluster (for single node)
resource "aws_elasticache_cluster" "main" {
  count = var.num_cache_nodes == 1 && !var.automatic_failover_enabled ? 1 : 0

  cluster_id           = var.cluster_id
  engine               = "redis"
  engine_version       = var.engine_version
  node_type           = var.node_type
  num_cache_nodes     = 1
  parameter_group_name = var.parameter_group_name
  port                = var.port
  
  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.main.id]
  
  snapshot_retention_limit = var.snapshot_retention_limit
  snapshot_window         = var.snapshot_window
  maintenance_window      = var.maintenance_window
  
  tags = var.tags
}

# Random auth token for Redis
resource "random_password" "auth_token" {
  count = var.transit_encryption_enabled ? 1 : 0

  length  = 32
  special = false
}

# Store auth token in Secrets Manager
resource "aws_secretsmanager_secret" "redis_auth" {
  count = var.transit_encryption_enabled ? 1 : 0

  name        = "${var.cluster_id}-auth-token"
  description = "Auth token for ${var.cluster_id}"

  tags = var.tags
}

resource "aws_secretsmanager_secret_version" "redis_auth" {
  count = var.transit_encryption_enabled ? 1 : 0

  secret_id = aws_secretsmanager_secret.redis_auth[0].id
  secret_string = jsonencode({
    auth_token = random_password.auth_token[0].result
    host       = var.num_cache_nodes > 1 || var.automatic_failover_enabled ? aws_elasticache_replication_group.main[0].primary_endpoint_address : aws_elasticache_cluster.main[0].cache_nodes[0].address
    port       = var.port
  })
}
