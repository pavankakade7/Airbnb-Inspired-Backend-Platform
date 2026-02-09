# SETUP_AND_COMMANDS.md

## Quick Setup & Commands Reference

Complete guide for setting up, building, running, and testing the AirBnB Backend Platform.

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Database Configuration](#database-configuration)
4. [Build Commands](#build-commands)
5. [Run Commands](#run-commands)
6. [Testing Commands](#testing-commands)
7. [Stripe Integration](#stripe-integration)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### System Requirements
- **OS**: Windows 10+, macOS 10.14+, or Linux (Ubuntu 18.04+)
- **Java**: JDK 21 or higher
- **Maven**: 3.8.0 or higher
- **PostgreSQL**: 12.0 or higher
- **Git**: Any recent version

### Verify Installations

```bash
# Check Java version
java -version
# Output should show Java 21+

# Check Maven version
mvn -version
# Output should show Maven 3.8.0+

# Check PostgreSQL version
psql --version
# Output should show PostgreSQL 12+
```

---

## Environment Setup

### 1. Clone Repository

```bash
# Using HTTPS
git clone https://github.com/your-username/airbnb-backend.git
cd airBnbApp

# Using SSH (if configured)
git clone git@github.com:your-username/airbnb-backend.git
cd airBnbApp
```

### 2. Create Environment Variables File

**Windows (PowerShell):**
```powershell
# Create .env file in project root
@"
DB_URL=jdbc:postgresql://localhost:5432/airbnb_db
DB_USERNAME=postgres
DB_PASSWORD=your_postgresql_password
JWT_SECRET=your_very_long_secret_key_minimum_32_chars_recommended_64_chars_ideal
FRONTEND_URL=http://localhost:3000
STRIPE_SECRET_KEY=sk_test_your_test_stripe_key_here
STRIPE_WEBHOOK_SECRET=whsec_your_test_webhook_secret_here
"@ | Out-File -Encoding UTF8 .env
```

**macOS/Linux (Bash/Zsh):**
```bash
# Create .env file in project root
cat > .env << EOF
DB_URL=jdbc:postgresql://localhost:5432/airbnb_db
DB_USERNAME=postgres
DB_PASSWORD=your_postgresql_password
JWT_SECRET=your_very_long_secret_key_minimum_32_chars_recommended_64_chars_ideal
FRONTEND_URL=http://localhost:3000
STRIPE_SECRET_KEY=sk_test_your_test_stripe_key_here
STRIPE_WEBHOOK_SECRET=whsec_your_test_webhook_secret_here
EOF

# Make it readable only by you
chmod 600 .env
```

### 3. Load Environment Variables

**PowerShell:**
```powershell
# Read .env file and set environment variables
$envFile = Get-Content .env
foreach ($line in $envFile) {
    if ($line -and -not $line.StartsWith("#")) {
        $key, $value = $line.Split("=", 2)
        [Environment]::SetEnvironmentVariable($key, $value, "User")
    }
}

# Verify
Get-ChildItem env: | Where-Object {$_.Name -like "*JWT*" -or $_.Name -like "*DB*"}
```

**Bash/Zsh:**
```bash
# Load environment variables
export $(cat .env | xargs)

# Or add to shell profile for persistence
echo "export $(cat .env | xargs)" >> ~/.bashrc
source ~/.bashrc

# Verify
echo $DB_URL
echo $JWT_SECRET
```

### 4. Enable Maven Environment Variables

**Windows (PowerShell):**
```powershell
# Set Maven environment variables
$Env:MAVEN_OPTS = "-Xmx1024m"
```

**Bash/Zsh:**
```bash
# Add to ~/.bashrc or ~/.zshrc
export MAVEN_OPTS="-Xmx1024m"
```

---

## Database Configuration

### 1. Download & Install PostgreSQL

**Windows:**
```powershell
# Using Chocolatey
choco install postgresql

# Or download from: https://www.postgresql.org/download/windows/
```

**macOS:**
```bash
# Using Homebrew
brew install postgresql@15

# Start PostgreSQL service
brew services start postgresql
```

**Linux (Ubuntu):**
```bash
# Update package manager
sudo apt update

# Install PostgreSQL
sudo apt install postgresql postgresql-contrib

# Start service
sudo systemctl start postgresql
```

### 2. Create Database

**Using psql (Interactive):**
```sql
-- Connect to PostgreSQL
-- Windows/macOS/Linux
psql -U postgres -h localhost

-- Once connected, create database
CREATE DATABASE airbnb_db;

-- Verify
\l

-- Connect to new database
\c airbnb_db

-- View tables (initially empty)
\dt
```

**Using Command Line:**
```bash
# Windows/macOS/Linux
createdb -U postgres -h localhost airbnb_db

# Verify
psql -U postgres -h localhost -l | grep airbnb_db
```

### 3. Test Database Connection

```bash
# Test connection
psql -U postgres -h localhost -d airbnb_db -c "SELECT 1;"

# Output should show:
#  ?column?
# ----------
#         1
```

---

## Build Commands

### 1. Clean Installation

```bash
# Remove build artifacts and Maven cache
mvn clean

# Re-download dependencies (fresh)
mvn clean dependency:resolve
```

### 2. Full Build

```bash
# Compile, test, and package
mvn clean package

# Output: target/airBnbApp-0.0.1-SNAPSHOT.jar

# Skip tests during build
mvn clean package -DskipTests
```

### 3. Compile Only

```bash
# Compile source code only
mvn compile

# Output: target/classes/
```

### 4. Download Dependencies

```bash
# Download all dependencies to ~/.m2
mvn dependency:resolve

# View dependency tree
mvn dependency:tree

# View dependency tree with filter
mvn dependency:tree -Dincludes=org.springframework*
```

### 5. Update Dependencies

```bash
# Check for updates
mvn versions:display-dependency-updates

# Check for plugin updates
mvn versions:display-plugin-updates

# Update to latest versions
mvn versions:use-latest-versions
```

---

## Run Commands

### 1. Run from IDE

**Using VS Code:**
```
1. Install "Extension Pack for Java"
2. Open AirBnbAppApplication.java
3. Click "Run" above main() method
4. Select "Run Java" from command palette
```

**Using IntelliJ IDEA:**
```
1. Open project
2. Right-click AirBnbAppApplication.java
3. Select "Run 'AirBnbAppApplication'"
4. Green arrow in gutter next to main()
```

**Using Eclipse:**
```
1. Import as Maven project
2. Right-click project → Run As → Maven build
3. Goals: spring-boot:run
```

### 2. Run from Terminal

```bash
# Using Maven plugin
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# Run with customs JVM arguments
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xms256m -Xmx1024m"
```

### 3. Run Pre-built JAR

```bash
# Build JAR first
mvn clean package

# Run JAR
java -jar target/airBnbApp-0.0.1-SNAPSHOT.jar

# Run with properties override
java -jar target/airBnbApp-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/airbnb_db \
  --spring.datasource.username=postgres \
  --spring.datasource.password=your_password
```

### 4. Run in Background

**Windows (PowerShell):**
```powershell
# Start in background
Start-Process -FilePath "java" -ArgumentList "-jar", "target/airBnbApp-0.0.1-SNAPSHOT.jar" -WindowStyle Hidden

# Or with logging
Start-Process -FilePath "java" -ArgumentList "-jar", "target/airBnbApp-0.0.1-SNAPSHOT.jar" -RedirectStandardOutput "logs/app.log" -RedirectStandardError "logs/error.log"

# View logs
tail -f logs/app.log
```

**Bash/Zsh:**
```bash
# Start in background
java -jar target/airBnbApp-0.0.1-SNAPSHOT.jar > logs/app.log 2>&1 &

# Get process ID
lsof -i :8080

# Kill process
kill -9 <PID>
```

### 5. Verify Application Started

```bash
# Test API endpoint
curl http://localhost:8080/api/v1/hotels/search

# Expected response
# {
#   "timeStamp": "...",
#   "data": [],
#   "error": null
# }

# Check Swagger UI
open http://localhost:8080/api/v1/swagger-ui.html
```

---

## Testing Commands

### 1. Run All Tests

```bash
# Run all tests
mvn test

# Run with specific surefire configuration
mvn test -DforkMode=once
```

### 2. Run Specific Test Class

```bash
# Run single test class
mvn test -Dtest=BookingServiceImplTest

# Run specific test method
mvn test -Dtest=BookingServiceImplTest#testInitialiseBooking
```

### 3. Run with Coverage

```bash
# Add JaCoCo plugin to pom.xml first, then:
mvn clean test jacoco:report

# Report location: target/site/jacoco/index.html

# Generate and view report
mvn clean test jacoco:report
start target/site/jacoco/index.html  # Windows
open target/site/jacoco/index.html   # macOS
```

### 4. Run Integration Tests

```bash
# Create separate test files with *IT suffix
# Run integration tests only
mvn verify

# Run unit tests + integration tests
mvn test integration-test

# Skip integration tests
mvn test -DskipITs
```

### 5. Debug Tests

```bash
# Run tests with debug mode
mvn -Dmaven.surefire.debug test

# Or in IDE:
# Right-click test → Debug As → JUnit Test
```

---

## Stripe Integration

### 1. Get Stripe Test Keys

```
1. Visit https://stripe.com/
2. Create account
3. Go to Dashboard → Developers → API Keys
4. Copy Test Secret Key (starts with sk_test_)
5. Copy Test Webhook Secret
```

### 2. Set Stripe Credentials

```bash
# Update .env file
STRIPE_SECRET_KEY=sk_test_your_actual_test_key_here
STRIPE_WEBHOOK_SECRET=whsec_your_actual_test_secret_here

# Load variables
export $(cat .env | xargs)  # macOS/Linux
Set-Item -Path Env:STRIPE_SECRET_KEY -Value "sk_test_..." # PowerShell
```

### 3. Test Stripe Integration Locally

**Install Stripe CLI:**

```bash
# Download from https://stripe.com/docs/stripe-cli
# Or use package manager

# macOS (Homebrew)
brew install stripe/stripe-cli/stripe

# Windows (Chocolatey)
choco install stripe

# Linux
# Download from above link
```

**Run Stripe CLI:**

```bash
# Login to Stripe account
stripe login

# Forward webhook events to local machine
stripe listen --forward-to localhost:8080/api/v1/webhook/stripe

# Get webhook signing secret (copy and add to .env)
# Output: whsec_...

# In another terminal, trigger test events
stripe trigger payment_intent.succeeded
```

### 4. Test Payment Flow

```bash
# Using test card numbers (Stripe documentation)
# 4242 4242 4242 4242 - Successful payment
# 4000 0000 0000 0002 - Card declined
# 4000 0002 5002 3010 - Requires authentication
```

---

## API Testing Commands

### 1. Authentication

```bash
# Sign up
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123!",
    "name": "Test User"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123!"
  }'

# Response includes: accessToken
```

### 2. Use Token in Requests

```bash
# Save token
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# Use in API calls
curl -X GET http://localhost:8080/api/v1/users/profile \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Hotel Management (Admin)

```bash
# Create hotel (requires HOTEL_MANAGER role)
curl -X POST http://localhost:8080/api/v1/admin/hotels \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Grand Hotel",
    "city": "Mumbai",
    "photos": ["photo1.jpg"],
    "amenities": ["WiFi", "Pool"],
    "contactInfo": {
      "address": "123 Main St",
      "phoneNumber": "+91-9999999999",
      "email": "contact@hotel.com",
      "location": "Downtown"
    }
  }'

# Get hotel details
curl -X GET http://localhost:8080/api/v1/admin/hotels/1 \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Booking Flow

```bash
# Search hotels (public)
curl "http://localhost:8080/api/v1/hotels/search?city=Mumbai&startDate=2024-02-15&endDate=2024-02-20&roomsCount=1"

# Initialize booking
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hotelId": 1,
    "roomId": 1,
    "checkInDate": "2024-02-15",
    "checkOutDate": "2024-02-20",
    "roomsCount": 1
  }'

# Response: bookingId = 100

# Add guests
curl -X PUT http://localhost:8080/api/v1/bookings/100/guests \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"guestIdList": [1, 2]}'

# Initiate payment
curl -X POST http://localhost:8080/api/v1/bookings/100/payments \
  -H "Authorization: Bearer $TOKEN"

# Response: sessionUrl for Stripe checkout
```

---

## Troubleshooting

### Issue: Maven Build Fails

```bash
# Problem: Dependency resolution failure

# Solutions:
1. Clear local Maven cache
   mvn clean -DremoveSnapshots

2. Update Maven
   mvn --version
   # Update to latest from https://maven.apache.org

3. Check internet connection
   ping repo1.maven.org

4. Use alternate Maven repository
   mvn clean package -Dmaven.repo.local=~/.m2/repository
```

### Issue: PostgreSQL Connection Failed

```bash
# Problem: "Connection refused"

# Troubleshooting steps:
1. Verify PostgreSQL is running
   # Windows
   Get-Service PostgreSQL* | Start-Service
   
   # macOS
   brew services start postgresql
   
   # Linux
   sudo systemctl start postgresql

2. Check connection details
   echo $DB_URL
   psql -U postgres -h localhost -d airbnb_db -c "SELECT 1;"

3. Check PostgreSQL port (default 5432)
   lsof -i :5432  # macOS/Linux
   netstat -an | findstr :5432  # Windows

4. Verify credentials
   # Update .env with correct username/password
```

### Issue: Port 8080 Already in Use

```bash
# Find process using port 8080
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# macOS/Linux
lsof -i :8080
kill -9 <PID>

# Or run on different port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### Issue: JWT Token Expired

```bash
# Error: "Invalid or expired token"

# Solution:
# 1. Get new token by logging in
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password"}'

# 2. Use new token in requests
# Token expiration defined in JWTService.java
# Default: 1 hour
```

### Issue: Stripe Webhook Not Received

```bash
# Problem: Webhook endpoint not receiving events

# Verify webhook secret
export STRIPE_WEBHOOK_SECRET="whsec_..."

# Check logs
tail -f target/logs/app.log | grep -i "webhook"

# Test locally with Stripe CLI
stripe listen --forward-to localhost:8080/api/v1/webhook/stripe

# In another terminal
stripe trigger payment_intent.succeeded

# Should see confirmation in first terminal
```

### Issue: 403 Unauthorized for Admin Endpoints

```bash
# Problem: Users can't access /admin/** routes

# Verify:
1. User has HOTEL_MANAGER role
   # Check in auth response or database

2. URL pattern matches exactly
   /admin/** requires role
   /api/v1/admin/** may need different pattern

3. Try with admin user:
   # Admin signup (if custom signup allows)
   # Or update user role in database
   UPDATE app_user SET roles = 'HOTEL_MANAGER' WHERE id = 1;
```

### Issue: Class Not Found Errors

```bash
# Problem: "javax.persistence.Entity not found"

# Solution: Java version mismatch
java -version  # Should show 21+

# Or: Missing dependencies
mvn clean dependency:resolve
mvn compile
```

---

## Performance Tuning

### JVM Parameters

```bash
# Recommended for development
-Xms256m -Xmx1024m -XX:+UseG1GC

# Recommended for production
-Xms2g -Xmx4g -XX:+UseG1GC -XX:+ParallelRefProcEnabled

# Run with parameters
java -Xms256m -Xmx1024m -jar target/airBnbApp-0.0.1-SNAPSHOT.jar
```

### Database Connection Pool

```properties
# application.properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
```

### Spring Boot Properties

```properties
# Logging level
logging.level.root=INFO
logging.level.com.pavanKakade.projects.airBnbApp=DEBUG

# JPA/Hibernate
spring.jpa.show-sql=false  # Don't print SQL in production
spring.jpa.properties.hibernate.jdbc.batch_size=20

# Server
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=10
```

---

## Useful Maven Commands

```bash
# Display Maven effective POM
mvn help:effective-pom

# Validate project
mvn validate

# Generate project info report
mvn site

# Display plugin help
mvn help:describe -Dplugin=spring-boot

# Execute specific phase
mvn clean
mvn compile
mvn test
mvn package
mvn install
mvn deploy
```

---

## IDE Configuration

### VS Code Settings

**Create `.vscode/settings.json`:**
```json
{
  "[java]": {
    "editor.defaultFormatter": "redhat.java",
    "editor.formatOnSave": true,
    "editor.codeActionsOnSave": {
      "source.organizeImports": true
    }
  },
  "maven.executable.path": "mvn"
}
```

### IntelliJ IDEA

```
1. File → Settings → Build, Execution, Deployment → Maven
2. Set Maven home directory
3. Configure JDK (Project Settings → Project → SDK → Java 21)
4. Enable Annotation Processing
   File → Settings → Build, Execution, Deployment → 
   Compiler → Annotation Processors → Enable
```

---

## CI/CD Pipeline (GitHub Actions Example)

**Create `.github/workflows/build.yml`:**
```yaml
name: Build & Test

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: airbnb_db
          POSTGRES_PASSWORD: postgres
    
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '21'
      
      - name: Build with Maven
        run: mvn clean package
      
      - name: Run Tests
        run: mvn test
```

---

## Quick Reference Checklist

```
✅ Java 21+ installed
✅ Maven 3.8+ installed
✅ PostgreSQL 12+ installed
✅ Git configured
✅ .env file created with credentials
✅ Database created (airbnb_db)
✅ Stripe test keys obtained
✅ Project cloned locally
✅ `mvn clean install` successful
✅ `mvn spring-boot:run` starts without errors
✅ Swagger UI accessible at http://localhost:8080/api/v1/swagger-ui.html
✅ Sample API call successful (GET /api/v1/hotels/search)
```

---

**Last Updated**: February 10, 2026  
**Status**: Production Ready
