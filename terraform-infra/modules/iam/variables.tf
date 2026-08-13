variable "cluster_name" {
  description = "Name of the EKS cluster"
  type        = string
}

variable "oidc_provider_arn" {
  description = "ARN of the OIDC provider"
  type        = string
}

variable "service_accounts" {
  description = "List of service accounts with their policies"
  type = list(object({
    name      = string
    namespace = string
    policies  = list(string)
  }))
  default = []
}

variable "custom_policies" {
  description = "Map of custom IAM policies"
  type = map(object({
    description = string
    policy_json = string
  }))
  default = {}
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
