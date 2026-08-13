# ECR Repositories
resource "aws_ecr_repository" "main" {
  for_each = toset(var.repositories)

  name                 = "${var.environment}/${each.value}"
  image_tag_mutability = var.image_tag_mutability

  image_scanning_configuration {
    scan_on_push = var.scan_on_push
  }

  encryption_configuration {
    encryption_type = var.encryption_type
    kms_key         = var.kms_key_arn != "" ? var.kms_key_arn : null
  }

  tags = merge(
    var.tags,
    {
      Name = each.value
    }
  )
}

# Lifecycle Policy
resource "aws_ecr_lifecycle_policy" "main" {
  for_each = toset(var.repositories)

  repository = aws_ecr_repository.main[each.key].name

  policy = jsonencode(var.lifecycle_policy)
}

# Repository Policy (if needed for cross-account access)
resource "aws_ecr_repository_policy" "main" {
  for_each = var.enable_cross_account_access ? toset(var.repositories) : []

  repository = aws_ecr_repository.main[each.key].name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowCrossAccountPull"
        Effect = "Allow"
        Principal = {
          AWS = var.allowed_account_ids
        }
        Action = [
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:BatchCheckLayerAvailability"
        ]
      }
    ]
  })
}
