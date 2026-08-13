# Getting Started with E-Commerce Microservices Infrastructure

Welcome! This guide will help you get up and running with the e-commerce microservices platform infrastructure.

## 📚 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Setup](#local-setup)
3. [Understanding the Structure](#understanding-the-structure)
4. [First Deployment](#first-deployment)
5. [Working with Services](#working-with-services)
6. [Common Tasks](#common-tasks)
7. [Troubleshooting](#troubleshooting)
8. [Best Practices](#best-practices)

## Prerequisites

### Required Tools

#### 1. AWS CLI
```bash
# Install AWS CLI
# Windows (using chocolatey)
choco install awscli

# macOS
brew install awscli

# Linux
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Configure AWS credentials
aws configure
```

#### 2. Terraform
```bash
# Windows
choco install terraform

# macOS
brew install terraform

# Linux
wget https://releases.hashicorp.com/terraform/1.5.0/terraform_1.5.0_linux_amd64.zip
unzip terraform_1.5.0_linux_amd64.zip
sudo mv terraform /usr/local/bin/

# Verify
terraform version
```

#### 3. kubectl
```bash
# Windows
choco install kubernetes-cli

# macOS
brew install kubectl

# Linux
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Verify
kubectl version --client
```

#### 4. Helm
```bash
# Windows
choco install kubernetes-helm

# macOS
brew install helm

# Linux
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Verify
helm version
```

#### 5. Docker
```bash
# Download from https://www.docker.com/products/docker-desktop

# Verify
docker version
```

### AWS Account Setup

1. **Create IAM User** with programmatic access
2. **Attach Policies**:
   - AmazonEC2FullAccess
   - AmazonEKSClusterPolicy
   - AmazonRDSFullAccess
   - AmazonElastiCacheFullAccess
   - AmazonMSKFullAccess
   - AmazonVPCFullAccess
   - IAMFullAccess
   - Or create custom policy with required permissions

3. **Configure AWS CLI**:
```bash
aws configure
# AWS Access Key ID: [your-access-key]
# AWS Secret Access Key: [your-secret-key]
# Default region: us-east-1
# Default output format: json
```

## Local Setup

### 1. Clone Repository
```bash
cd /path/to/your/projects
git clone <repository-url>
cd order-service
```

### 2. Understand the Project Structure
```
order-service/
├── src/                      # Java application code
├── Dockerfile                # Container image definition
├── pom.xml                   # Maven dependencies
└── terraform-infra/          # Infrastructure code
    ├── environments/         # Environment configs
    ├── modules/              # Reusable Terraform modules
    ├── helm-charts/          # Kubernetes deployments
    ├── jenkins/              # CI/CD pipeline
    └── scripts/              # Automation scripts
```

## Understanding the Structure

### Terraform Modules

| Module | Purpose | Key Resources |
|--------|---------|---------------|
| `vpc` | Networking | VPC, Subnets, NAT, IGW |
| `eks` | Kubernetes | EKS Cluster, Node Groups |
| `ecr` | Container Registry | ECR Repositories |
| `rds` | Database | PostgreSQL Instance |
| `elasticache` | Cache | Redis Cluster |
| `msk` | Messaging | Kafka Cluster |
| `iam` | Security | IAM Roles, Policies |

### Helm Chart Structure

```
helm-charts/order-service/
├── Chart.yaml          # Chart metadata
├── values.yaml         # Default configuration
└── templates/          # Kubernetes manifests
    ├── deployment.yaml  # Pod specification
    ├── service.yaml     # Internal service
    ├── ingress.yaml     # External access
    └── hpa.yaml         # Auto-scaling
```

## First Deployment

### Step 1: Setup Terraform Backend

```bash
cd terraform-infra/scripts

# Make scripts executable (Linux/Mac)
chmod +x *.sh

# Setup S3 and DynamoDB for Terraform state
./setup-backend.sh dev us-east-1
```

**What this does**:
- Creates S3 bucket for Terraform state
- Enables versioning and encryption
- Creates DynamoDB table for state locking

### Step 2: Configure Variables

```bash
cd ../environments/dev

# Copy example file
cp terraform.tfvars.example terraform.tfvars

# Edit with your settings
nano terraform.tfvars  # or use your preferred editor
```

**Key variables to set**:
```hcl
aws_region = "us-east-1"              # Your AWS region
project_name = "ecommerce"            # Project name
environment = "dev"                   # Environment
vpc_cidr = "10.0.0.0/16"             # VPC CIDR block
```

### Step 3: Deploy Infrastructure

```bash
# Initialize Terraform
terraform init

# Review what will be created
terraform plan

# Apply changes
terraform apply
# Type 'yes' when prompted
```

**Expected duration**: 15-25 minutes

**What gets created**:
- VPC with subnets
- EKS cluster
- RDS database
- ElastiCache Redis
- MSK Kafka
- ECR repositories
- IAM roles

### Step 4: Configure kubectl

```bash
cd ../../scripts

# Configure kubectl to access EKS
./configure-kubectl.sh dev us-east-1

# Verify connection
kubectl get nodes
```

You should see 2 nodes in "Ready" state.

### Step 5: Install Kubernetes Add-ons

#### AWS Load Balancer Controller
```bash
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Get cluster name from Terraform output
CLUSTER_NAME=$(cd ../environments/dev && terraform output -raw eks_cluster_name)

# Install controller
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=$CLUSTER_NAME
```

#### Metrics Server
```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

### Step 6: Build and Push Application

```bash
# Navigate to application root
cd ../../..

# Get ECR login credentials
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com

# Build application
mvn clean package -DskipTests

# Build Docker image
docker build -t order-service:latest .

# Tag for ECR
docker tag order-service:latest \
  $AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/dev/order-service:latest

# Push to ECR
docker push $AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/dev/order-service:latest
```

### Step 7: Deploy Application

```bash
cd terraform-infra/helm-charts

# Update values.yaml with ECR URL
ECR_URL="$AWS_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/dev/order-service"
sed -i "s|repository:.*|repository: $ECR_URL|" order-service/values.yaml

# Get database and other endpoints from Terraform
cd ../environments/dev
DB_ENDPOINT=$(terraform output -raw rds_endpoint)
REDIS_ENDPOINT=$(terraform output -raw redis_endpoint)
KAFKA_BROKERS=$(terraform output -raw msk_bootstrap_brokers)

# Update Helm values
cd ../../helm-charts/order-service
sed -i "s|host: \"\".*|host: \"$DB_ENDPOINT\"|" values.yaml
sed -i "s|host: \"\".*|host: \"$REDIS_ENDPOINT\"|" values.yaml
sed -i "s|bootstrapServers: \"\".*|bootstrapServers: \"$KAFKA_BROKERS\"|" values.yaml

# Create Kubernetes secrets
DB_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id ecommerce-dev-db-master-password \
  --query SecretString --output text | jq -r .password)

kubectl create secret generic order-service-secrets \
  --from-literal=DB_PASSWORD=$DB_PASSWORD

# Deploy with Helm
helm install order-service .
```

### Step 8: Verify Deployment

```bash
# Check pods
kubectl get pods

# Check service
kubectl get svc order-service

# Check ingress
kubectl get ingress order-service

# Get application URL
ALB_URL=$(kubectl get ingress order-service -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
echo "Application URL: http://$ALB_URL"

# Test health endpoint
curl http://$ALB_URL/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

## Working with Services

### View Logs
```bash
# Get pod name
POD=$(kubectl get pod -l app.kubernetes.io/name=order-service -o jsonpath='{.items[0].metadata.name}')

# View logs
kubectl logs $POD

# Follow logs
kubectl logs -f $POD
```

### Scale Service
```bash
# Manual scaling
kubectl scale deployment order-service --replicas=3

# Auto-scaling is configured via HPA
kubectl get hpa order-service
```

### Update Service
```bash
# After code changes and new image push
helm upgrade order-service ./order-service \
  --set image.tag=v1.0.1
```

### Rollback
```bash
# View history
helm history order-service

# Rollback to previous version
helm rollback order-service
```

## Common Tasks

### Access Database
```bash
# Port forward to RDS (through a pod)
kubectl run -it --rm psql --image=postgres:15 --restart=Never -- \
  psql -h $DB_ENDPOINT -U dbadmin -d ecommerce
```

### Access Redis
```bash
# Port forward to Redis
kubectl run -it --rm redis-cli --image=redis:7 --restart=Never -- \
  redis-cli -h $REDIS_ENDPOINT
```

### View Metrics
```bash
# Install Prometheus/Grafana
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack -n monitoring --create-namespace

# Access Grafana
kubectl port-forward -n monitoring svc/prometheus-grafana 3000:80
# Open http://localhost:3000 (admin/prom-operator)
```

### Update Infrastructure
```bash
cd terraform-infra/environments/dev

# Modify terraform.tfvars
nano terraform.tfvars

# Apply changes
terraform plan
terraform apply
```

## Troubleshooting

### Pods Not Starting
```bash
# Check pod status
kubectl describe pod <pod-name>

# Common issues:
# 1. Image pull errors - verify ECR URL and credentials
# 2. Resource limits - check node capacity
# 3. Failed health checks - verify application configuration
```

### Database Connection Issues
```bash
# Verify security groups
aws ec2 describe-security-groups --group-ids <sg-id>

# Test connectivity from pod
kubectl run -it --rm test --image=busybox --restart=Never -- \
  nc -zv $DB_ENDPOINT 5432
```

### Ingress Not Working
```bash
# Check ALB controller logs
kubectl logs -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller

# Verify ingress
kubectl describe ingress order-service
```

## Best Practices

### Development Workflow
1. Make code changes locally
2. Test with unit tests
3. Build Docker image
4. Push to ECR
5. Update Helm values
6. Deploy to dev environment
7. Verify functionality
8. Create pull request

### Security
- Never commit secrets to Git
- Use AWS Secrets Manager
- Enable MFA on AWS account
- Regularly rotate credentials
- Review security groups

### Cost Management
- Stop unused environments
- Use auto-scaling
- Monitor AWS Cost Explorer
- Set up billing alerts
- Clean up old resources

### Monitoring
- Set up CloudWatch alarms
- Monitor application metrics
- Review logs regularly
- Configure alerting

## Next Steps

1. ✓ Complete initial deployment
2. □ Deploy remaining microservices
3. □ Set up Jenkins pipeline
4. □ Configure monitoring dashboards
5. □ Implement backup procedures
6. □ Create runbooks for operations

## Additional Resources

- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [Helm Documentation](https://helm.sh/docs/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

## Getting Help

- Check [TROUBLESHOOTING.md](../TROUBLESHOOTING.md)
- Review [DEPLOYMENT.md](DEPLOYMENT.md)
- Ask in team Slack channel
- Create GitHub issue for bugs

---

**Welcome to the team! Happy coding! 🚀**
