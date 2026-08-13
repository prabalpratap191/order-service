output "endpoint" {
  description = "Redis endpoint"
  value       = var.num_cache_nodes > 1 || var.automatic_failover_enabled ? aws_elasticache_replication_group.main[0].primary_endpoint_address : aws_elasticache_cluster.main[0].cache_nodes[0].address
}

output "port" {
  description = "Redis port"
  value       = var.port
}

output "security_group_id" {
  description = "ID of the security group"
  value       = aws_security_group.main.id
}

output "cluster_id" {
  description = "ID of the Redis cluster"
  value       = var.cluster_id
}

output "secret_arn" {
  description = "ARN of the Secrets Manager secret containing auth token"
  value       = var.transit_encryption_enabled ? aws_secretsmanager_secret.redis_auth[0].arn : ""
}
