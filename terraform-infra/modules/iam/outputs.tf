output "service_account_role_arns" {
  description = "Map of service account role ARNs"
  value = {
    for sa in var.service_accounts :
    "${sa.namespace}-${sa.name}" => aws_iam_role.service_account["${sa.namespace}-${sa.name}"].arn
  }
}

output "service_account_role_names" {
  description = "Map of service account role names"
  value = {
    for sa in var.service_accounts :
    "${sa.namespace}-${sa.name}" => aws_iam_role.service_account["${sa.namespace}-${sa.name}"].name
  }
}

output "custom_policy_arns" {
  description = "Map of custom policy ARNs"
  value       = { for k, v in aws_iam_policy.custom : k => v.arn }
}
