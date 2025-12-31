# Payment Service Architecture & Running Guide

## Architecture Overview

This is a **monolithic Spring Boot application** with a **multi-module Maven project structure**. It is **NOT** 8 separate services - it's **ONE application** with 7 library modules.

### Module Structure

```
paymentservice (parent)
├── paymentservice-common          [Library JAR] - Common DTOs, types, errors
├── paymentservice-storage         [Library JAR] - Database entities & repositories
├── paymentservice-connectors      [Library JAR] - Payment connector interfaces
├── paymentservice-routing         [Library JAR] - Routing algorithms
├── paymentservice-scheduler       [Library JAR] - Scheduler service
├── paymentservice-core            [Library JAR] - Core business logic
└── paymentservice-web             [MAIN APPLICATION] - REST API layer (EXECUTABLE)
```

## How Modules Connect

### Dependency Hierarchy

```
paymentservice-web (MAIN APP)
    ├── depends on → paymentservice-core
    │       ├── depends on → paymentservice-common
    │       ├── depends on → paymentservice-storage
    │       │       └── depends on → paymentservice-common
    │       ├── depends on → paymentservice-connectors
    │       └── depends on → paymentservice-routing
    └── depends on → paymentservice-scheduler
```

### How It Works

1. **Library Modules** (common, storage, connectors, routing, scheduler, core):
   - These are **library JARs** that contain reusable code
   - They are **NOT** standalone applications
   - They get packaged as dependencies into the main application

2. **Main Application** (paymentservice-web):
   - This is the **ONLY executable** Spring Boot application
   - Contains the `PaymentServiceApplication` main class
   - Has REST controllers, security configuration, and web layer
   - When built, it creates a **fat JAR** that includes ALL dependencies

3. **Build Process**:
   - Maven builds each module and creates JARs
   - The Spring Boot Maven plugin in `paymentservice-web` packages **ALL** dependencies into ONE executable JAR
   - The final JAR (`paymentservice-web-1.0.0-SNAPSHOT.jar`) contains everything needed to run

## Running the Application

### You Only Need to Run ONE JAR!

The main application JAR is located at:
```
Payment-service/paymentservice/paymentservice-web/target/paymentservice-web-1.0.0-SNAPSHOT.jar
```

### Step 1: Build All Modules

From the root `paymentservice` directory:

```powershell
cd Payment-service/paymentservice
mvn clean package spring-boot:repackage -DskipTests
```

This will:
1. Build all 7 library modules (common, storage, connectors, routing, scheduler, core)
2. Build the main application (web)
3. Package everything into ONE executable JAR in `paymentservice-web/target/`

### Step 2: Run the Application

```powershell
cd Payment-service/paymentservice/paymentservice-web/target
java "-Dspring.classformat.ignore=true" -jar paymentservice-web-1.0.0-SNAPSHOT.jar
```

**Note:** The `-Dspring.classformat.ignore=true` flag is needed for Java 25 compatibility.

### Step 3: Verify It's Running

- Health Check: `http://localhost:8080/api/health`
- Deep Health: `http://localhost:8080/api/health/ready`
- Actuator: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Module Details

### 1. paymentservice-common
- **Type:** Library module
- **Purpose:** Shared DTOs, enums, types, errors, validation
- **Packaging:** Regular JAR (not executable)
- **Contains:** Common classes used across all modules

### 2. paymentservice-storage
- **Type:** Library module
- **Purpose:** Database abstraction layer
- **Packaging:** Regular JAR (not executable)
- **Contains:** 
  - Entity classes (database models)
  - Repository interfaces (R2DBC repositories)
  - Database migration scripts
  - Database configuration

### 3. paymentservice-connectors
- **Type:** Library module
- **Purpose:** Payment connector interfaces and implementations
- **Packaging:** Regular JAR (not executable)
- **Contains:** Connector interfaces for different payment providers (Stripe, Adyen, etc.)

### 4. paymentservice-routing
- **Type:** Library module
- **Purpose:** Payment routing algorithms
- **Packaging:** Regular JAR (not executable)
- **Contains:** Routing service and algorithms

### 5. paymentservice-scheduler
- **Type:** Library module
- **Purpose:** Scheduled task processing
- **Packaging:** Regular JAR (not executable)
- **Contains:** Scheduler service implementation

### 6. paymentservice-core
- **Type:** Library module
- **Purpose:** Core business logic
- **Packaging:** Regular JAR (not executable)
- **Contains:** 
  - Payment processing logic
  - Business rules
  - Service implementations
  - Uses: storage, connectors, routing, common

### 7. paymentservice-web
- **Type:** **MAIN APPLICATION** (Executable)
- **Purpose:** REST API layer and web controllers
- **Packaging:** Executable Spring Boot JAR (fat JAR)
- **Contains:**
  - REST controllers
  - Security configuration
  - Web layer
  - Main application class (`PaymentServiceApplication`)
  - Uses: core, scheduler

## Why This Architecture?

### Benefits:
1. **Modularity:** Code is organized into logical modules
2. **Reusability:** Library modules can be reused
3. **Maintainability:** Clear separation of concerns
4. **Single Deployment:** One JAR to deploy and run
5. **Easier Testing:** Modules can be tested independently

### How It's Different from Microservices:
- **Microservices:** Multiple separate applications, each running independently
- **This Project:** One application with modular code organization

## Troubleshooting

### Issue: "No main manifest attribute"
**Solution:** Make sure you're running the JAR from `paymentservice-web/target/`, not from other modules.

### Issue: "ClassNotFoundException" or missing dependencies
**Solution:** Rebuild with `mvn clean package spring-boot:repackage` to ensure all dependencies are packaged.

### Issue: Port 8080 already in use
**Solution:** Use the `stop-app.ps1` script or change the port in `application.yml`:
```yaml
server:
  port: 8081  # Change to different port
```

## Quick Start Script

Create a `start-app.ps1` script:

```powershell
# Start Hyperswitch Payment Service
Write-Host "Starting Payment Service..." -ForegroundColor Green
cd Payment-service/paymentservice/paymentservice-web/target
java "-Dspring.classformat.ignore=true" -jar paymentservice-web-1.0.0-SNAPSHOT.jar
```

## Summary

- **Total Modules:** 7 library modules + 1 main application = 8 modules
- **Executable JARs:** Only 1 (paymentservice-web)
- **How to Run:** Just run the `paymentservice-web-1.0.0-SNAPSHOT.jar`
- **How They Connect:** Through Maven dependencies, all packaged into one fat JAR
- **No Separate Services:** Everything runs in one process

The application is a **monolithic Spring Boot application** with a **modular codebase** for better organization and maintainability.

