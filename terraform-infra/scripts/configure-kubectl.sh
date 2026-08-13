#!/bin/bash

# Script to configure kubectl for EKS cluster

set -e

ENVIRONMENT=${1:-dev}
REGION=${2:-us-east-1}
CLUSTER_NAME="ecommerce-${ENVIRONMENT}-eks"

echo "Configuring kubectl for cluster: ${CLUSTER_NAME}"
echo "Region: ${REGION}"

# Update kubeconfig
aws eks update-kubeconfig --region "${REGION}" --name "${CLUSTER_NAME}"

echo ""
echo "kubectl configured successfully!"
echo ""

# Verify connection
echo "Verifying connection to cluster..."
kubectl cluster-info

echo ""
echo "Current context:"
kubectl config current-context

echo ""
echo "Nodes in cluster:"
kubectl get nodes
