# IAM Roles for Service Accounts (IRSA)
data "aws_iam_policy_document" "assume_role" {
  for_each = { for sa in var.service_accounts : "${sa.namespace}-${sa.name}" => sa }

  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [var.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${replace(var.oidc_provider_arn, "/^(.*provider/)/", "")}:sub"
      values   = ["system:serviceaccount:${each.value.namespace}:${each.value.name}"]
    }

    condition {
      test     = "StringEquals"
      variable = "${replace(var.oidc_provider_arn, "/^(.*provider/)/", "")}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "service_account" {
  for_each = { for sa in var.service_accounts : "${sa.namespace}-${sa.name}" => sa }

  name               = "${var.cluster_name}-${each.value.namespace}-${each.value.name}"
  assume_role_policy = data.aws_iam_policy_document.assume_role[each.key].json

  tags = merge(
    var.tags,
    {
      ServiceAccount = each.value.name
      Namespace      = each.value.namespace
    }
  )
}

resource "aws_iam_role_policy_attachment" "service_account" {
  for_each = merge([
    for sa in var.service_accounts : {
      for idx, policy_arn in sa.policies :
      "${sa.namespace}-${sa.name}-${idx}" => {
        role       = "${var.cluster_name}-${sa.namespace}-${sa.name}"
        policy_arn = policy_arn
      }
    }
  ]...)

  role       = aws_iam_role.service_account[each.value.role].name
  policy_arn = each.value.policy_arn
}

# Custom IAM Policies
resource "aws_iam_policy" "custom" {
  for_each = var.custom_policies

  name        = "${var.cluster_name}-${each.key}"
  description = each.value.description
  policy      = each.value.policy_json

  tags = var.tags
}
