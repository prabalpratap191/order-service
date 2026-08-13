# Order Service

Spring Boot 3.x microservice for Order Management, migrated from legacy Jakarta EE Order Management Module.

## 🚀 Quick Start

### Start Everything (Recommended)
```bash
# Windows
.\start-all.bat

# Linux/Mac
./start-all.sh
```

This starts PostgreSQL, Kafka, and the Order Service in one command!

### Alternative: Manual Start
```bash
# 1. Start infrastructure
cd ..
docker-compose -f full-stack-docker-compose.yml up -d

# 2. Run application
cd order-service
.\run-local.bat
```

## 📚 Documentation

- **[QUICK_START.md](QUICK_START.md)** - Get started in 5 minutes
- **[DATABASE_SETUP.md](DATABASE_SETUP.md)** - PostgreSQL setup and configuration
- **[KAFKA_SETUP.md](KAFKA_SETUP.md)** - Kafka setup with Docker Compose
- **[KAFKA_QUICK_REFERENCE.md](KAFKA_QUICK_REFERENCE.md)** - Kafka command cheat sheet
- **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Common issues and solutions
- **[TIMEZONE_FIX_SUMMARY.md](TIMEZONE_FIX_SUMMARY.md)** - Timezone configuration details

## Legacy Mapping
- Legacy Module: Order Management
- Legacy Classes: CreateOrderCommand, OrderManagementFlowCommandExecutor, OrderVO
- Legacy Tables: ORDER, ORDER_ITEM

## Tech Stack
- Java 17
- Spring Boot 3.2.5
- PostgreSQL (via Docker)
- Apache Kafka (via Docker)
- Docker Compose

## 🔗 Service URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Order Service | http://localhost:8083 | - |
| Swagger UI | http://localhost:8083/swagger-ui.html | - |
| Health Check | http://localhost:8083/actuator/health | - |
| Kafka UI | http://localhost:8090 | - |
| pgAdmin | http://localhost:8080 | ppsingh.singh2@gmail.com / pgadminpass |
| PostgreSQL | localhost:5432 | pgadmin / pgadminpass |

## Build & Run

### Development (with Kafka)
```bash
mvn clean package
mvn spring-boot:run -Dspring-boot.run.profiles=local-with-kafka
```

### Development (without Kafka)
```bash
mvn clean package
.\run-local.bat
```

### Production Build
```bash
mvn clean package
java -jar target/order-service-1.0.0.jar
```

## Docker
```bash
docker build -t order-service .
docker run -p 8083:8083 order-service
```

## API Endpoints
- POST /api/v1/orders - Create a new order
- GET /api/v1/orders/{orderId} - Get order by ID
- GET /api/v1/orders/customer/{customerId} - Get orders by customer
- PUT /api/v1/orders/{orderId}/status - Update order status
- POST /api/v1/orders/{orderId}/cancel - Cancel an order

## Kafka Topics
- **order.created** - Published when a new order is created
- **order.updated** - Published when an order status is updated

## 🧪 Example: Create Order

```bash
curl -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST001",
    "items": [
      {
        "productId": "PROD001",
        "quantity": 2,
        "price": 29.99
      }
    ]
  }'
```

Then check Kafka UI (http://localhost:8090) to see the event!"# order-service" 
