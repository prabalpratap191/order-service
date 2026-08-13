# E-Commerce Microservices Infrastructure on AWS

This repository contains Terraform infrastructure as code (IaC) for deploying a microservices-based e-commerce platform on AWS using EKS (Elastic Kubernetes Service).

## 🏗️ Architecture Overview

### Microservices
- **catalog-service**: Product catalog management
- **customer-service**: Customer data and profiles
- **order-service**: Order processing and management
- **order-history-service**: Order history tracking
- **payment-service**: Payment processing
- **work_queue-service**: Background job processing

### AWS Infrastructure Components
- **EKS Cluster**: Managed Kubernetes cluster
- **VPC**: Isolated network with public/private subnets
- **RDS**: PostgreSQL/MySQL database
- **ElastiCache**: Redis for caching
- **MSK**: Managed Kafka for event streaming
- **Application Load Balancer**: Traffic distribution
- **ECR**: Docker container registry
- **S3**: Terraform state and artifact storage
- **CloudWatch**: Logging and monitoring
- **IAM**: Security and access control

## 📁 Project Structure

```
terraform-infra/
├── environments/
│   ├── dev/
│   ├── staging/
│   └── prod/
├── modules/
│   ├── vpc/
│   ├── eks/
│   ├── rds/
│   ├── elasticache/
│   ├── msk/
│   ├── ecr/
│   └── iam/
├── helm-charts/
│   ├── catalog-service/
│   ├── customer-service/
│   ├── order-service/
│   ├── order-history-service/
│   ├── payment-service/
│   └── work-queue-service/
├── jenkins/
│   ├── Jenkinsfile
│   └── pipelines/
├── scripts/
└── README.md
```

## 🚀 Prerequisites

- AWS CLI configured with appropriate credentials
- Terraform >= 1.5.0
- kubectl >= 1.28
- Helm >= 3.12
- Jenkins (for CI/CD)
- Docker

## 🔧 Setup Instructions

### 1. Initialize Terraform Backend

```bash
cd environments/dev
terraform init
```

### 2. Configure Variables

Copy and edit the variables file:
```bash
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your settings
```

### 3. Plan Infrastructure

```bash
terraform plan
```

### 4. Apply Infrastructure

```bash
terraform apply
```

### 5. Configure kubectl

```bash
aws eks update-kubeconfig --region <region> --name <cluster-name>
```

### 6. Deploy Microservices with Helm

```bash
cd ../../helm-charts
helm install catalog-service ./catalog-service
helm install customer-service ./customer-service
helm install order-service ./order-service
helm install order-history-service ./order-history-service
helm install payment-service ./payment-service
helm install work-queue-service ./work-queue-service
```

## 🔄 CI/CD Pipeline

The Jenkins pipeline automates:
1. Code checkout
2. Build Docker images
3. Push to ECR
4. Run tests
5. Deploy to EKS using Helm
6. Health checks

### Jenkins Setup

1. Install required plugins:
   - Kubernetes Plugin
   - Docker Pipeline
   - AWS Steps Plugin
   - Pipeline AWS Plugin

2. Configure credentials:
   - AWS credentials
   - Kubernetes config
   - Docker registry credentials

3. Create pipeline jobs from `jenkins/pipelines/`

## 🌍 Environments

### Development
- Single AZ deployment
- Smaller instance types
- Auto-scaling disabled

### Staging
- Multi-AZ deployment
- Production-like configuration
- Auto-scaling enabled

### Production
- Multi-AZ with high availability
- Production-grade instances
- Auto-scaling and monitoring
- Backup and disaster recovery

## 📊 Monitoring and Logging

- **CloudWatch**: Application and infrastructure logs
- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization
- **X-Ray**: Distributed tracing

## 🔐 Security Best Practices

- Private subnets for application workloads
- Security groups with least privilege
- Secrets stored in AWS Secrets Manager
- IAM roles for service accounts (IRSA)
- Network policies in Kubernetes
- Pod security policies

## 💰 Cost Optimization

- Right-sized EC2 instances
- Spot instances for non-critical workloads
- Auto-scaling based on metrics
- S3 lifecycle policies
- Reserved instances for production

## 🆘 Troubleshooting

Common issues and solutions:

### EKS Cluster Access Issues
```bash
aws eks update-kubeconfig --region <region> --name <cluster-name>
```

### Helm Deployment Failures
```bash
helm list -A
kubectl get pods -A
kubectl logs <pod-name>
```

### Terraform State Lock
```bash
terraform force-unlock <lock-id>
```

## 📝 Maintenance

### Update EKS Version
1. Update in terraform variables
2. Run `terraform plan` and `terraform apply`
3. Update node groups
4. Test deployments

### Backup Strategy
- RDS automated backups (7-day retention)
- EBS snapshots for persistent volumes
- Terraform state in S3 with versioning

## 🤝 Contributing

1. Create feature branch
2. Make changes
3. Test in dev environment
4. Create pull request
5. Deploy to staging for validation
6. Merge and deploy to production

## 📄 License

MIT License

## 📞 Support

For issues and questions, contact the DevOps team.
