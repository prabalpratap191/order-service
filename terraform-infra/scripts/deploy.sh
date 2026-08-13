#!/bin/bash

# Script to deploy infrastructure using Terraform

set -e

ENVIRONMENT=${1:-dev}
ACTION=${2:-apply}

if [ "$ENVIRONMENT" != "dev" ] && [ "$ENVIRONMENT" != "staging" ] && [ "$ENVIRONMENT" != "prod" ]; then
    echo "Error: Invalid environment. Use 'dev', 'staging', or 'prod'"
    exit 1
fi

if [ "$ACTION" != "plan" ] && [ "$ACTION" != "apply" ] && [ "$ACTION" != "destroy" ]; then
    echo "Error: Invalid action. Use 'plan', 'apply', or 'destroy'"
    exit 1
fi

echo "========================================"
echo "Deploying to environment: ${ENVIRONMENT}"
echo "Action: ${ACTION}"
echo "========================================"

cd "../environments/${ENVIRONMENT}"

# Initialize Terraform
echo "Initializing Terraform..."
terraform init

# Validate configuration
echo "Validating Terraform configuration..."
terraform validate

# Run terraform command
if [ "$ACTION" = "plan" ]; then
    terraform plan -out=tfplan
elif [ "$ACTION" = "apply" ]; then
    terraform plan -out=tfplan
    echo ""
    read -p "Do you want to apply these changes? (yes/no): " confirm
    if [ "$confirm" = "yes" ]; then
        terraform apply tfplan
        rm tfplan
        
        echo ""
        echo "Deployment complete!"
        echo ""
        echo "To configure kubectl, run:"
        terraform output -raw configure_kubectl
        echo ""
    else
        echo "Apply cancelled."
        rm tfplan
    fi
elif [ "$ACTION" = "destroy" ]; then
    echo ""
    echo "WARNING: This will destroy all resources in ${ENVIRONMENT}!"
    read -p "Are you sure? Type 'yes' to confirm: " confirm
    if [ "$confirm" = "yes" ]; then
        terraform destroy
    else
        echo "Destroy cancelled."
    fi
fi
