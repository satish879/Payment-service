# Health Checks Postman Collection

This Postman collection contains all the health check endpoints for the Hyperswitch Payment Service.

## Prerequisites

1. **Postman** installed on your machine
2. **Payment Service** running locally on port 8080
3. **Dependencies** (PostgreSQL, Redis) should be running if you want deep health checks to pass
4. **Authentication** (optional) - The service supports optional API key authentication

## Setup Instructions

### 1. Import the Collection

1. Open Postman
2. Click **Import** button (top left)
3. Select the file `Health_Checks.postman_collection.json`
4. The collection will appear in your Postman workspace

### 2. Configure Environment Variables

The collection uses a variable `base_url` which is set to `http://localhost:8080` by default.

To change the base URL:
1. Click on the collection name
2. Go to the **Variables** tab
3. Update the `base_url` value if your service runs on a different port or host

### 3. Authentication Configuration

The Payment Service supports optional authentication via API keys. **All health check endpoints are accessible without authentication** regardless of the authentication setting.

#### Authentication Disabled (Default)
- **Configuration:** `hyperswitch.security.enable-auth: false` in `application.yml`
- **Behavior:** All endpoints are accessible without authentication
- **Health Endpoints:** No authentication required ✅

#### Authentication Enabled
- **Configuration:** `hyperswitch.security.enable-auth: true` in `application.yml` or set `ENABLE_AUTH=true` environment variable
- **Behavior:** 
  - Health endpoints (`/api/health`, `/api/health/**`, `/actuator/**`) are accessible without authentication ✅
  - Other endpoints require API key authentication
- **API Key Authentication:** When enabled, other endpoints require an API key in the request headers

**Note:** Health check endpoints are always publicly accessible (no authentication required) even when authentication is enabled for the rest of the service.

## Available Endpoints

**Important:** All health check endpoints listed below are **publicly accessible** and do **NOT require authentication**, regardless of the `enable-auth` configuration setting.

### 1. Basic Health Check
- **Method:** `GET`
- **URL:** `{{base_url}}/api/health`
- **Description:** Simple health check that returns a static healthy status
- **Authentication:** Not required (always public)
- **Expected Response:**
  ```json
  {
    "status": "healthy",
    "service": "hyperswitch-payment-service"
  }
  ```

### 2. Deep Health Check
- **Method:** `GET`
- **URL:** `{{base_url}}/api/health/ready`
- **Description:** Comprehensive health check of all components (database, Redis, etc.)
- **Authentication:** Not required (always public)
- **Expected Response:** Detailed health status object with component statuses
  ```json
  {
    "database": true,
    "redis": true,
    "vault": true,
    "analytics": true,
    "opensearch": true,
    "outgoing_request": true,
    "grpc_health_check": {
      "dynamic_routing_service": true
    },
    "decision_engine": true,
    "unified_connector_service": true,
    "status": "healthy"
  }
  ```

### 3. Health Check (v2 API)
- **Method:** `GET`
- **URL:** `{{base_url}}/api/v2/health`
- **Description:** Health check using v2 API format
- **Authentication:** Not required (always public)
- **Expected Response:** Health status in v2 format (same as deep health check)

### 4. Deep Health Check (v2 API)
- **Method:** `GET`
- **URL:** `{{base_url}}/api/v2/health/ready`
- **Description:** Comprehensive health check using v2 API format
- **Authentication:** Not required (always public)
- **Expected Response:** Detailed health status in v2 format (same as `/api/health/ready`)

### 5. Spring Boot Actuator Health
- **Method:** `GET`
- **URL:** `{{base_url}}/actuator/health`
- **Description:** Spring Boot Actuator health endpoint with detailed component status
- **Authentication:** Not required (always public)
- **Expected Response:** Detailed health information from Spring Boot Actuator
  ```json
  {
    "status": "UP",
    "components": {
      "diskSpace": { "status": "UP" },
      "ping": { "status": "UP" },
      "r2dbc": { "status": "UP" },
      "redis": { "status": "UP" }
    }
  }
  ```

### 6. Spring Boot Actuator Info
- **Method:** `GET`
- **URL:** `{{base_url}}/actuator/info`
- **Description:** Application information endpoint
- **Authentication:** Not required (always public)
- **Expected Response:** Application info (if configured)

### 7. Spring Boot Actuator Metrics
- **Method:** `GET`
- **URL:** `{{base_url}}/actuator/metrics`
- **Description:** Lists all available metrics
- **Authentication:** Not required (always public)
- **Expected Response:** List of available metric names

## Running the Tests

### Option 1: Run Individual Requests
1. Expand the collection in Postman
2. Click on any request
3. Click **Send** button

### Option 2: Run Collection
1. Click on the collection name
2. Click **Run** button (top right)
3. Select all requests or specific ones
4. Click **Run Health Checks** button

## Expected Results

### When Service is Running and Healthy:
- All endpoints should return `200 OK` status
- Basic health check should return `{"status": "healthy"}`
- Deep health checks should show all components as healthy

### When Service is Not Running:
- All requests will fail with connection errors
- Check that the service is running on port 8080

### When Dependencies are Not Available:
- Basic health check (`/health`) will still work
- Deep health checks (`/health/ready`) may show some components as unhealthy
- Check that PostgreSQL and Redis are running

## Troubleshooting

### Connection Refused
- **Issue:** Cannot connect to the service
- **Solution:** 
  - Verify the service is running: `java -jar paymentservice-web-*.jar` or via IDE
  - Check the port in `application.yml` (default: 8080)
  - Verify `base_url` variable in Postman collection

### Deep Health Check Shows Unhealthy Components
- **Issue:** Database or Redis shows as unhealthy
- **Solution:**
  - Verify PostgreSQL is running: `docker ps` or check service status
  - Verify Redis is running: `redis-cli ping` should return `PONG`
  - Check connection strings in `application.yml`

### 404 Not Found
- **Issue:** Endpoint not found
- **Solution:**
  - Verify the service is fully started (check logs)
  - Check that the endpoint path is correct
  - Ensure no base path prefix is required (check `application.yml`)
  - Verify `spring.webflux.base-path` is not set in `application.yml` (should be removed)

### 401 Unauthorized (Non-Health Endpoints)
- **Issue:** Getting 401 Unauthorized for non-health endpoints
- **Solution:**
  - This is expected if authentication is enabled (`enable-auth: true`)
  - Health endpoints should still work without authentication
  - For other endpoints, provide the required API key in request headers
  - To disable authentication, set `hyperswitch.security.enable-auth: false` in `application.yml`

## Local Development Setup

To run the service locally:

1. **Start PostgreSQL:**
   ```bash
   docker run -d --name postgres -p 5432:5432 -e POSTGRES_PASSWORD=db_pass -e POSTGRES_USER=db_user -e POSTGRES_DB=hyperswitch_db postgres:15
   ```

2. **Start Redis:**
   ```bash
   docker run -d --name redis -p 6379:6379 redis:7-alpine
   ```

3. **Run the Application:**
   ```bash
   # Using Maven
   mvn spring-boot:run
   
   # Or using the JAR
   java -jar paymentservice/paymentservice-web/target/paymentservice-web-*.jar
   ```

4. **Verify Service is Running:**
   - Open browser: `http://localhost:8080/api/health`
   - Should return: `{"status": "healthy", "service": "hyperswitch-payment-service"}`

## Authentication Details

### Health Endpoints (Always Public)
All health check endpoints are **always accessible without authentication**, regardless of the `enable-auth` configuration:
- `/api/health`
- `/api/health/**` (including `/api/health/ready`)
- `/api/v2/health`
- `/api/v2/health/**` (including `/api/v2/health/ready`)
- `/actuator/**` (all Actuator endpoints)

### Enabling/Disabling Authentication

**Default Behavior (Authentication Disabled):**
```yaml
# application.yml
hyperswitch:
  security:
    enable-auth: false  # Default value
```

**Enable Authentication:**
```yaml
# application.yml
hyperswitch:
  security:
    enable-auth: true
```

Or via environment variable:
```bash
export ENABLE_AUTH=true
```

**When Authentication is Enabled:**
- Health endpoints remain publicly accessible (no auth required) ✅
- Other API endpoints require API key authentication
- Webhook endpoints (`/api/webhooks/**`) are also publicly accessible
- Swagger UI (`/swagger-ui.html`, `/api-docs/**`) is publicly accessible

### Security Configuration
The security configuration is defined in `SecurityConfig.java` and automatically:
- Permits all health endpoints without authentication
- Permits all Actuator endpoints without authentication
- Allows optional API key authentication for other endpoints when enabled

## Notes

- The basic health check (`/api/health`) does not require any dependencies and will always return healthy if the service is running
- Deep health checks (`/api/health/ready`) verify database, Redis, and other component connectivity
- Spring Boot Actuator endpoints provide additional monitoring capabilities
- **All health check endpoints are GET requests and do NOT require authentication** - they are always publicly accessible
- When authentication is enabled, only non-health endpoints require API key authentication

## Additional Resources

- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Postman Documentation](https://learning.postman.com/docs/getting-started/introduction/)
- [Implementation Summary](./IMPLEMENTATION_SUMMARY.md)

