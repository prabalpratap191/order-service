output "endpoint" {
  description = "Connection endpoint"
  value       = aws_db_instance.main.endpoint
}

output "address" {
  description = "Address of the database"
  value       = aws_db_instance.main.address
}

output "port" {
  description = "Port of the database"
  value       = aws_db_instance.main.port
}

output "database_name" {
  description = "Name of the database"
  value       = aws_db_instance.main.db_name
}

output "username" {
  description = "Master username"
  value       = var.username
  sensitive   = true
}

output "security_group_id" {
  description = "ID of the security group"
  value       = aws_security_group.main.id
}

output "secret_arn" {
  description = "ARN of the Secrets Manager secret containing credentials"
  value       = aws_secretsmanager_secret.db_password.arn
}

output "instance_id" {
  description = "ID of the RDS instance"
  value       = aws_db_instance.main.id
}
