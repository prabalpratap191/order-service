# Deployment Guide

Comprehensive guide for deploying the e-commerce microservices platform on AWS.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        AWS Cloud                                 │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    VPC (10.0.0.0/16)                      │  │
│  │                                                            │  │
│  │  ┌──────────────┐        ┌──────────────┐                │  │
│  │  │ Public Subnet│        │ Public Subnet│                │  │
│  │  │  10.0.1.0/24 │        │  10.0.2.0/24 │                │  │
│  │  │              │        │              │                │  │
│  │  │  NAT Gateway │        │   (AZ-b)     │                │  │
│  │  │    (AZ-a)    │        │              │                │  │
│  │  └──────┬───────┘        └──────────────┘                │  │
│  │         │                                                 │  │
│  │  ┌──────▼────────┐       ┌──────────────┐                │  │
│  │  │Private Subnet │       │Private Subnet│                │  │
│  │  │  10.0.10.0/24 │       │ 10.0.11.0/24 │                │  │
│  │  │               │       │              │                │  │
│  │  │ ┌───────────┐ │       │ ┌──────────┐ │                │  │
│  │  │ │EKS Worker │ │       │ │EKS Worker│ │                │  │
│  │  │ │   Nodes   │ │       │ │  Nodes   │ │                │  │
│  │  │ └───────────┘ │       │ └──────────┘ │                │  │
│  │  │               │       │              │                │  │
│  │  │ ┌───────────┐ │       │ ┌──────────┐ │                │  │
│  │  │ │    RDS    │ │       │ │   MSK    │ │                │  │
│  │  │ │ PostgreSQL│ │       │ │  Kafka   │ │                │  │
│  │  │ └───────────┘ │       │ └──────────┘ │                │  │
│  │  │               │       │              │                │  │
│  │  │ ┌───────────┐ │       │              │                │  │
│  │  │ │ElastiCache│ │       │              │                │  │
│  │  │ │   Redis   │ │       │              │                │  │
│  │  │ └───────────┘ │       │              │                │  │
│  │  └───────────────┘       └──────────────┘                │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌────────────┐  ┌────────────┐  ┌──────────────┐              │
│  │    ECR     │  │    ALB     │  │ Secrets Mgr  │              │
│  └────────────┘  └────────────┘  └──────────────┘              │
└─────────────────────────────────────────────────────────────────┘
```

## Microservices Architecture

### Services

1. **catalog-service**: Product catalog and inventory management
2. **customer-service**: Customer profiles and authentication
3. **order-service**: Order creation and management
4. **order-history-service**: Historical order data and analytics
5. **payment-service**: Payment processing and transactions
6. **work-queue-service**: Background job processing

### Communication Patterns

- **Synchronous**: REST APIs via Application Load Balancer
- **Asynchronous**: Event-driven via Apache Kafka (MSK)
- **Caching**: Redis (ElastiCache) for session and data caching
- **Database**: PostgreSQL (RDS) with separate schemas per service

## Infrastructure Components

### Networking
- **VPC**: Isolated network with CIDR 10.0.0.0/16
- **Subnets**: Public (for ALB) and Private (for workloads)
- **NAT Gateway**: Outbound internet access for private subnets
- **Security Groups**: Fine-grained network access control

### Compute
- **EKS**: Managed Kubernetes service (v1.28)
- **Node Groups**: Auto-scaling EC2 instances (t3.medium)
- **Fargate**: Optional serverless compute for specific workloads

### Storage & Database
- **RDS PostgreSQL 15.4**: Multi-AZ for production
- **ElastiCache Redis 7.0**: Cluster mode for HA
- **MSK Kafka 3.5.1**: Multi-broker setup
- **EBS**: Persistent volumes for stateful workloads

### Container Registry
- **ECR**: Private Docker image repositories per service
- **Image Scanning**: Automated vulnerability scanning
- **Lifecycle Policies**: Automatic image cleanup

### Security
- **IAM Roles**: IRSA (IAM Roles for Service Accounts)
- **Secrets Manager**: Database credentials and API keys
- **KMS**: Encryption keys for data at rest
- **Security Groups**: Network-level security
- **Network Policies**: Pod-level security in Kubernetes

### Observability
- **CloudWatch**: Logs and metrics
- **Prometheus**: Metrics collection from pods
- **Grafana**: Metrics visualization
- **X-Ray**: Distributed tracing (optional)

## Deployment Strategies

### Blue-Green Deployment

```yaml
# Deploy new version (green)
helm upgrade order-service ./order-service \
  --set image.tag=v2.0.0 \
  --reuse-values

# Traffic is automatically routed to new pods
# Kubernetes performs rolling update

# Rollback if needed
helm rollback order-service
```

### Canary Deployment

Using Flagger or manual weight-based routing:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: order-service-canary
spec:
  # Route 10% traffic to canary
  ...
```

### Rolling Update (Default)

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1
    maxUnavailable: 0
```

## Environment Configuration

### Development (dev)
- Single AZ deployment
- t3.micro/small instance types
- Single NAT Gateway
- Reduced backup retention
- Auto-scaling disabled
- Cost-optimized configuration

### Staging (staging)
- Multi-AZ deployment
- Production-like instance types
- Full monitoring and logging
- Auto-scaling enabled
- Testing ground for production changes

### Production (prod)
- Multi-AZ with high availability
- Production-grade instances
- Enhanced monitoring
- Auto-scaling with conservative policies
- Deletion protection enabled
- Extended backup retention
- Automated disaster recovery

## CI/CD Pipeline

### Pipeline Stages

1. **Checkout**: Clone repository
2. **Build**: Compile Java application with Maven
3. **Unit Tests**: Run JUnit tests
4. **Code Quality**: SonarQube analysis
5. **Build Image**: Create Docker image
6. **Security Scan**: Trivy vulnerability scan
7. **Push to ECR**: Upload image to registry
8. **Deploy**: Helm upgrade on EKS
9. **Health Check**: Verify deployment
10. **Integration Tests**: E2E testing

### Jenkins Configuration

```groovy
// Example pipeline trigger
triggers {
    githubPush()
    pollSCM('H/5 * * * *')  // Poll every 5 minutes
}

// Multi-branch pipeline for feature branches
```

### GitOps with ArgoCD (Alternative)

```bash
# Install ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd -f \
  https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Create application
argocd app create order-service \
  --repo https://github.com/org/repo \
  --path helm-charts/order-service \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace default
```

## Scaling Strategies

### Horizontal Pod Autoscaling

```yaml
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80
```

### Cluster Autoscaling

EKS Cluster Autoscaler automatically adjusts node count:

```bash
# Nodes scale based on pod resource requests
min_size: 2
max_size: 10
```

### Database Scaling

- **Vertical**: Change instance class
- **Read Replicas**: For read-heavy workloads
- **Connection Pooling**: PgBouncer or application-level

## Monitoring & Alerting

### Key Metrics

1. **Application Metrics**
   - Request rate
   - Error rate
   - Response time (p50, p95, p99)
   - JVM metrics (heap, GC)

2. **Infrastructure Metrics**
   - CPU/Memory utilization
   - Network throughput
   - Disk I/O
   - Pod count

3. **Business Metrics**
   - Orders per minute
   - Payment success rate
   - Cart abandonment rate

### Alerting Rules

```yaml
# Example Prometheus alert
- alert: HighErrorRate
  expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
  for: 5m
  annotations:
    summary: "High error rate detected"
```

## Disaster Recovery

### Backup Strategy

1. **Database**: Automated daily backups (7-day retention)
2. **Configuration**: GitOps repository
3. **Terraform State**: S3 versioning enabled
4. **Persistent Volumes**: EBS snapshots

### Recovery Procedures

```bash
# Restore database from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier new-instance \
  --db-snapshot-identifier snapshot-id

# Restore infrastructure from Terraform
terraform apply

# Redeploy applications
helm install <service> ./helm-charts/<service>
```

## Security Best Practices

### Network Security
- Private subnets for workloads
- Security groups with least privilege
- Network policies in Kubernetes
- WAF for public endpoints (optional)

### Access Control
- RBAC in Kubernetes
- IAM policies with least privilege
- Service accounts with IRSA
- Audit logging enabled

### Secrets Management
- AWS Secrets Manager for credentials
- External Secrets Operator (optional)
- Encrypted environment variables
- No secrets in code or Git

### Compliance
- Encryption at rest (RDS, EBS, S3)
- Encryption in transit (TLS)
- VPC Flow Logs
- CloudTrail logging

## Cost Optimization

### Strategies

1. **Right-sizing**: Monitor and adjust instance types
2. **Spot Instances**: Use for non-critical workloads
3. **Reserved Instances**: 1-year or 3-year for production
4. **Auto-scaling**: Scale down during off-peak
5. **Storage Lifecycle**: Clean up old ECR images, logs
6. **Resource Quotas**: Prevent resource sprawl

### Cost Breakdown (Estimated)

- **EKS Cluster**: $73/month
- **EC2 Nodes**: $60-240/month (2-10 t3.medium)
- **RDS**: $15-100/month (db.t3.micro to db.t3.large)
- **ElastiCache**: $12-50/month
- **MSK**: $150-300/month
- **Data Transfer**: Variable
- **Other Services**: $20-50/month

**Total Dev**: ~$350-500/month  
**Total Prod**: ~$800-1500/month

## Maintenance

### Regular Tasks

1. **Weekly**
   - Review monitoring dashboards
   - Check for security patches
   - Review cost reports

2. **Monthly**
   - Update dependencies
   - Review and update auto-scaling policies
   - Disaster recovery drill
   - Review access logs

3. **Quarterly**
   - EKS version upgrades
   - Database engine upgrades
   - Security audit
   - Capacity planning review

## Troubleshooting Guide

See [TROUBLESHOOTING.md](../TROUBLESHOOTING.md) for detailed troubleshooting steps.

## Additional Resources

- [AWS EKS Best Practices](https://aws.github.io/aws-eks-best-practices/)
- [Kubernetes Production Best Practices](https://learnk8s.io/production-best-practices)
- [Terraform Best Practices](https://www.terraform-best-practices.com/)
- [Helm Best Practices](https://helm.sh/docs/chart_best_practices/)
