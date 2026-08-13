# Quick Start Guide

This guide will help you quickly set up and deploy the e-commerce microservices infrastructure on AWS.

## Prerequisites

Ensure you have the following installed:
- AWS CLI (configured with credentials)
- Terraform >= 1.5.0
- kubectl >= 1.28
- Helm >= 3.12
- Docker
- Git

## Step 1: Set Up Terraform Backend

First, create the S3 bucket and DynamoDB table for Terraform state:

```bash
cd scripts
chmod +x setup-backend.sh
./setup-backend.sh dev us-east-1
```

## Step 2: Configure Variables

Copy and edit the variables file:

```bash
cd ../environments/dev
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your specific values
```

## Step 3: Initialize and Plan

```bash
terraform init
terraform plan
```

## Step 4: Deploy Infrastructure

```bash
# Using the helper script
cd ../../scripts
chmod +x deploy.sh
./deploy.sh dev apply

# Or manually
cd ../environments/dev
terraform apply
```

**Note:** This will create:
- VPC with public/private subnets
- EKS cluster with node groups
- RDS PostgreSQL database
- ElastiCache Redis cluster
- MSK (Kafka) cluster
- ECR repositories
- IAM roles and policies

## Step 5: Configure kubectl

```bash
cd ../../scripts
chmod +x configure-kubectl.sh
./configure-kubectl.sh dev us-east-1

# Verify
kubectl get nodes
```

## Step 6: Install Kubernetes Add-ons

### Install AWS Load Balancer Controller

```bash
# Add EKS chart repo
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Install AWS Load Balancer Controller
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=ecommerce-dev-eks \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

### Install Metrics Server

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

### Install Prometheus & Grafana (Optional)

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install prometheus prometheus-community/kube-prometheus-stack \
  -n monitoring --create-namespace
```

## Step 7: Generate Helm Charts for All Services

```bash
cd ../scripts
chmod +x generate-helm-charts.sh
./generate-helm-charts.sh
```

## Step 8: Build and Push Docker Images

For each microservice:

```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build and push (example for order-service)
cd ../../..  # Go to order-service root
docker build -t dev/order-service:latest .
docker tag dev/order-service:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/dev/order-service:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/dev/order-service:latest
```

## Step 9: Create Kubernetes Secrets

Create secrets for database and other services:

```bash
# Get RDS password from Secrets Manager
DB_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id ecommerce-dev-db-master-password \
  --query SecretString --output text | jq -r .password)

# Create database secret
kubectl create secret generic rds-credentials \
  --from-literal=username=dbadmin \
  --from-literal=password=$DB_PASSWORD \
  --from-literal=database=ecommerce

# Get Redis auth token (if encryption enabled)
REDIS_AUTH=$(aws secretsmanager get-secret-value \
  --secret-id ecommerce-dev-redis-auth-token \
  --query SecretString --output text | jq -r .auth_token)

kubectl create secret generic redis-credentials \
  --from-literal=auth-token=$REDIS_AUTH
```

## Step 10: Deploy Microservices

Deploy each microservice using Helm:

```bash
cd terraform-infra/helm-charts

# Update values with ECR URLs and endpoints
# Then deploy each service

helm install catalog-service ./catalog-service
helm install customer-service ./customer-service
helm install order-service ./order-service
helm install order-history-service ./order-history-service
helm install payment-service ./payment-service
helm install work-queue-service ./work-queue-service
```

## Step 11: Verify Deployments

```bash
# Check all pods
kubectl get pods -A

# Check services
kubectl get svc

# Check ingresses
kubectl get ingress

# Get Load Balancer URL
kubectl get ingress order-service -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
```

## Step 12: Set Up Jenkins (Optional)

1. Deploy Jenkins to Kubernetes or use existing Jenkins server
2. Install required plugins:
   - Kubernetes Plugin
   - Docker Pipeline
   - AWS Steps Plugin
   - Pipeline AWS Plugin

3. Configure credentials:
   - AWS credentials
   - Kubeconfig
   - SonarQube token (if using)

4. Create pipeline jobs from `jenkins/Jenkinsfile`

## Accessing Services

### Get Service URLs

```bash
kubectl get ingress
```

### Access Grafana (if installed)

```bash
kubectl port-forward -n monitoring svc/prometheus-grafana 3000:80
# Access at http://localhost:3000
# Default credentials: admin/prom-operator
```

## Cleanup

To destroy all resources:

```bash
# Delete Helm releases
helm uninstall catalog-service
helm uninstall customer-service
helm uninstall order-service
helm uninstall order-history-service
helm uninstall payment-service
helm uninstall work-queue-service

# Destroy infrastructure
cd scripts
./deploy.sh dev destroy
```

## Troubleshooting

### Pods not starting
```bash
kubectl describe pod <pod-name>
kubectl logs <pod-name>
```

### Ingress not working
```bash
kubectl describe ingress <ingress-name>
kubectl logs -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller
```

### Database connection issues
```bash
# Test from a pod
kubectl run -it --rm debug --image=postgres:15 --restart=Never -- \
  psql -h <rds-endpoint> -U dbadmin -d ecommerce
```

## Next Steps

1. Configure DNS for your domain
2. Set up SSL/TLS certificates
3. Configure monitoring and alerting
4. Set up backup and disaster recovery
5. Implement CI/CD pipelines
6. Configure auto-scaling policies
7. Set up centralized logging

## Support

For issues and questions, refer to:
- [README.md](README.md)
- [Terraform Documentation](https://www.terraform.io/docs)
- [EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Helm Documentation](https://helm.sh/docs/)
