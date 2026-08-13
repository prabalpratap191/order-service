#!/bin/bash

# Script to generate Helm charts for all microservices based on order-service template

set -e

BASE_CHART="../helm-charts/order-service"
HELM_CHARTS_DIR="../helm-charts"

# List of microservices
MICROSERVICES=(
    "catalog-service"
    "customer-service"
    "order-history-service"
    "payment-service"
    "work-queue-service"
)

echo "Generating Helm charts for all microservices..."
echo ""

for service in "${MICROSERVICES[@]}"; do
    echo "Creating Helm chart for: ${service}"
    
    # Create service directory
    mkdir -p "${HELM_CHARTS_DIR}/${service}/templates"
    
    # Copy and customize Chart.yaml
    cat > "${HELM_CHARTS_DIR}/${service}/Chart.yaml" <<EOF
apiVersion: v2
name: ${service}
description: A Helm chart for ${service} microservice
type: application
version: 1.0.0
appVersion: "1.0.0"

keywords:
  - microservices
  - ecommerce
  - ${service%%\-*}

maintainers:
  - name: DevOps Team
    email: devops@example.com
EOF
    
    # Copy values.yaml and replace service name
    sed "s/order-service/${service}/g" "${BASE_CHART}/values.yaml" > "${HELM_CHARTS_DIR}/${service}/values.yaml"
    
    # Copy template files and replace service name
    for template in "${BASE_CHART}/templates"/*.yaml "${BASE_CHART}/templates"/*.tpl; do
        if [ -f "$template" ]; then
            filename=$(basename "$template")
            sed "s/order-service/${service}/g" "$template" > "${HELM_CHARTS_DIR}/${service}/templates/${filename}"
        fi
    done
    
    echo "✓ Created ${service} chart"
    echo ""
done

echo "All Helm charts generated successfully!"
echo "Charts location: ${HELM_CHARTS_DIR}"
