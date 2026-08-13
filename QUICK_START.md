# 🚀 Quick Start Guide

## ✅ Timezone Issue - RESOLVED!

The PostgreSQL timezone error has been fixed with multiple safeguards:

1. **Application Code** - Sets timezone in `main()` method before Spring Boot starts
2. **JDBC URL** - Includes `?TimeZone=Asia/Kolkata` parameter  
3. **HikariCP** - Configured with timezone datasource property
4. **Startup Scripts** - Set JVM timezone argument

---

## 📍 Prerequisites

### 1. Start PostgreSQL Database

```bash
cd ..
docker-compose -f db-docker-compose.yml up -d
```

**Verify it's running:**
```bash
docker ps
```

You should see `postgres_db` and `pgadmin_web` containers.

---

## 🏃 Run the Application

### Option 1: Using Custom Startup Script (Recommended for Windows)

```bash
.\run-local.bat
```

### Option 2: Using Maven with Timezone Argument

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Duser.timezone=Asia/Kolkata" -Dspring-boot.run.profiles=local
```

### Option 3: Using Maven with Default Settings

The application now sets timezone programmatically, so this should work too:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 🔍 Verify Application is Running

### 1. Check Console Output

You should see:
```
Application Timezone: Asia/Kolkata
...
Started OrderServiceApplication in X.XXX seconds
```

### 2. Check Health Endpoint

Open browser or use curl:
```bash
curl http://localhost:8083/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

### 3. Access Swagger UI

Open in browser:
```
http://localhost:8083/swagger-ui.html
```

---

## 📦 Database Access

### Via pgAdmin Web Interface

1. Open: `http://localhost:8080`
2. Login:
   - Email: `ppsingh.singh2@gmail.com`
   - Password: `pgadminpass`
3. Server already configured as `postgres_db`

### Via Command Line

```bash
docker exec -it postgres_db psql -U pgadmin -d orderDb
```

Common commands:
```sql
\dt              -- List tables
\d orders        -- Describe orders table
SELECT * FROM orders LIMIT 10;
```

---

## ⚠️ Known Issues & Solutions

### Kafka Warnings (Safe to Ignore)

If you see Kafka connection warnings, it's normal if Kafka is not running.

The application works fine without Kafka for:
- Database operations
- REST API calls
- Testing CRUD operations

**To disable Kafka warnings**, run with local profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The local profile automatically:
- Excludes Kafka auto-configuration
- Reduces Kafka logging to WARN level

### Port Already in Use

**Error:** `Port 8083 is already in use`

**Solution:**
```bash
# Windows - Find and kill process
netstat -ano | findstr :8083
taskkill /PID <PID> /F
```

### Database Connection Refused

**Check if PostgreSQL is running:**
```bash
docker ps | findstr postgres
```

**Start if not running:**
```bash
cd ..
docker-compose -f db-docker-compose.yml up -d
```

---

## 🧪 Testing the Application

### 1. Create an Order (POST)

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

### 2. Get All Orders (GET)

```bash
curl http://localhost:8083/api/orders
```

### 3. Get Order by ID (GET)

```bash
curl http://localhost:8083/api/orders/1
```

### 4. Update Order Status (PUT)

```bash
curl -X PUT http://localhost:8083/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CONFIRMED"
  }'
```

---

## 🛑 Stopping the Application

### Stop Spring Boot

- Press `Ctrl+C` in the terminal

### Stop PostgreSQL

```bash
cd ..
docker-compose -f db-docker-compose.yml stop
```

### Complete Cleanup (removes data)

```bash
cd ..
docker-compose -f db-docker-compose.yml down -v
rm -rf data
```

---

## 📚 Additional Documentation

- [DATABASE_SETUP.md](DATABASE_SETUP.md) - Detailed database configuration
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Common issues and solutions
- [README.md](README.md) - Project overview

---

## ✅ Success Checklist

- [x] PostgreSQL timezone error **FIXED**
- [x] Database connection working
- [x] Application starts successfully
- [x] Timezone set to Asia/Kolkata
- [x] HikariCP connection pool initialized
- [x] REST API endpoints accessible
- [x] Swagger UI available
- [x] Health check responding

---

## 🎉 You're All Set!

Your Order Service is now running with:
- ✅ PostgreSQL database connection
- ✅ Correct timezone (Asia/Kolkata)
- ✅ Auto-schema update (in local profile)
- ✅ SQL logging enabled (in local profile)
- ✅ REST API ready for testing

**Happy Coding!** 🚀
