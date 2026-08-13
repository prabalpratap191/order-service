variable "cluster_name" {
  description = "Name of the MSK cluster"
  type        = string
}

variable "kafka_version" {
  description = "Kafka version"
  type        = string
  default     = "3.5.1"
}

variable "number_of_nodes" {
  description = "Number of broker nodes"
  type        = number
  default     = 3
}

variable "broker_instance_type" {
  description = "Instance type for brokers"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "subnet_ids" {
  description = "List of subnet IDs for brokers"
  type        = list(string)
}

variable "allowed_security_groups" {
  description = "List of security group IDs allowed to access MSK"
  type        = list(string)
}

variable "ebs_volume_size" {
  description = "Size of EBS volume for each broker in GB"
  type        = number
  default     = 100
}

variable "provisioned_throughput_enabled" {
  description = "Enable provisioned throughput"
  type        = bool
  default     = false
}

variable "volume_throughput" {
  description = "Throughput in MiB/s"
  type        = number
  default     = 250
}

variable "encryption_in_transit_client_broker" {
  description = "Encryption setting for client-broker communication"
  type        = string
  default     = "TLS"
}

variable "encryption_in_transit_in_cluster" {
  description = "Enable encryption for inter-broker communication"
  type        = bool
  default     = true
}

variable "kms_key_arn" {
  description = "ARN of KMS key for encryption at rest"
  type        = string
  default     = ""
}

variable "enhanced_monitoring" {
  description = "Enhanced monitoring level"
  type        = string
  default     = "DEFAULT"
}

variable "enable_jmx_exporter" {
  description = "Enable JMX exporter for Prometheus"
  type        = bool
  default     = true
}

variable "enable_node_exporter" {
  description = "Enable node exporter for Prometheus"
  type        = bool
  default     = true
}

variable "log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 7
}

variable "server_properties" {
  description = "Server properties for Kafka configuration"
  type        = string
  default     = <<-EOT
    auto.create.topics.enable=true
    default.replication.factor=3
    min.insync.replicas=2
    num.io.threads=8
    num.network.threads=5
    num.partitions=1
    num.replica.fetchers=2
    replica.lag.time.max.ms=30000
    socket.receive.buffer.bytes=102400
    socket.request.max.bytes=104857600
    socket.send.buffer.bytes=102400
    unclean.leader.election.enable=true
    zookeeper.session.timeout.ms=18000
  EOT
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
