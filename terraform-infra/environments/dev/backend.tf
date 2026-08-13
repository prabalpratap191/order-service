terraform {
  backend "s3" {
    bucket         = "ecommerce-terraform-state-dev"
    key            = "dev/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "ecommerce-terraform-locks"
    
    # Uncomment after creating the S3 bucket and DynamoDB table manually
    # Or use the bootstrap script in scripts/setup-backend.sh
  }
}
