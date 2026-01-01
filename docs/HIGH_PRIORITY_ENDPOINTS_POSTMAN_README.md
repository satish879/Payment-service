# High Priority Endpoints Postman Collection

This Postman collection contains all the **high priority (critical)** endpoints for the Hyperswitch Payment Service. These endpoints are essential for core payment processing operations.

## Prerequisites

1. **Postman** installed on your machine
2. **Payment Service** running locally on port 8080
3. **Dependencies** (PostgreSQL, Redis) should be running
4. **Authentication** (optional) - The service supports optional API key authentication

## Setup Instructions

### 1. Import the Collection

1. Open Postman
2. Click **Import** button (top left)
3. Select the file `High_Priority_Endpoints.postman_collection.json`
4. The collection will appear in your Postman workspace

### 2. Configure Environment Variables

The collection uses several variables which are set with default values:

| Variable | Default Value | Description |
|----------|---------------|-------------|
| `base_url` | `http://localhost:8080` | Base URL of the payment service |
| `merchant_id` | `merchant_123` | Merchant identifier |
| `customer_id` | `cust_123` | Customer identifier |
| `payment_id` | `pay_123` | Payment identifier |
| `payment_method_id` | `pm_123` | Payment method identifier |
| `refund_id` | `ref_123` | Refund identifier |
| `client_secret` | `client_secret_123` | Client secret for payment method |

To update these variables:
1. Click on the collection name
2. Go to the **Variables** tab
3. Update the values as needed

### 3. Authentication Configuration

The Payment Service supports optional authentication via API keys. When authentication is enabled, you need to provide the API key in the `X-Merchant-Id` header (which is already included in all requests).

#### Authentication Disabled (Default)
- **Configuration:** `hyperswitch.security.enable-auth: false` in `application.yml`
- **Behavior:** All endpoints are accessible without authentication
- **Headers:** `X-Merchant-Id` header is still sent but not validated

#### Authentication Enabled
- **Configuration:** `hyperswitch.security.enable-auth: true` in `application.yml` or set `ENABLE_AUTH=true` environment variable
- **Behavior:** Endpoints require API key authentication
- **API Key:** Should be provided in the `X-Merchant-Id` header (already configured in collection)

## Available Endpoints

### 1. Core Payment Operations

#### 1.1 Create Payment
- **Method:** `POST`
- **URL:** `{{base_url}}/api/payments`
- **Description:** Creates a new payment intent with the specified amount, currency, and payment method. The payment will be in 'requires_confirmation' status until confirmed.
- **Request Body:**
  ```json
  {
    "amount": {
      "value": 1000,
      "currencyCode": "USD"
    },
    "merchantId": "merchant_123",
    "paymentMethod": "CARD",
    "customerId": "cust_123",
    "description": "Payment for order #12345",
    "captureMethod": "AUTOMATIC",
    "confirm": false,
    "metadata": {}
  }
  ```
- **Response:** Payment intent with payment ID and client secret

#### 1.2 Confirm Payment
- **Method:** `POST`
- **URL:** `{{base_url}}/api/payments/{{payment_id}}/confirm`
- **Description:** Confirms a payment intent and processes the payment through the selected connector.
- **Request Body:**
  ```json
  {
    "paymentMethodId": "pm_123",
    "returnUrl": "https://example.com/return"
  }
  ```
- **Response:** Payment status and details

#### 1.3 Capture Payment
- **Method:** `POST`
- **URL:** `{{base_url}}/api/payments/{{payment_id}}/capture`
- **Description:** Captures a previously authorized payment. Supports full and partial capture.
- **Request Body:**
  ```json
  {
    "amount": {
      "value": 1000,
      "currencyCode": "USD"
    }
  }
  ```
- **Response:** Capture status and details

#### 1.4 Get Payment
- **Method:** `GET`
- **URL:** `{{base_url}}/api/payments/{{payment_id}}`
- **Description:** Retrieves payment details by payment ID.
- **Response:** Complete payment details including status, amount, and metadata

#### 1.5 Update Payment
- **Method:** `POST`
- **URL:** `{{base_url}}/api/payments/{{payment_id}}`
- **Description:** Updates payment details including amount, description, and metadata.
- **Request Body:**
  ```json
  {
    "amount": {
      "value": 1500,
      "currencyCode": "USD"
    },
    "description": "Updated payment description",
    "metadata": {
      "order_id": "order_123"
    }
  }
  ```
- **Response:** Updated payment details

#### 1.6 Cancel Payment
- **Method:** `POST`
- **URL:** `{{base_url}}/api/payments/{{payment_id}}/cancel`
- **Description:** Cancels a payment that is in a cancellable state.
- **Request Body:**
  ```json
  {
    "cancellationReason": "Customer requested cancellation"
  }
  ```
- **Response:** Cancellation status

#### 1.7 Get Client Secret
- **Method:** `GET`
- **URL:** `{{base_url}}/api/payments/{{payment_id}}/client_secret`
- **Description:** Retrieves the client secret for a payment intent.
- **Response:** Client secret string

### 2. Customer Management

#### 2.1 Create Customer
- **Method:** `POST`
- **URL:** `{{base_url}}/api/customers`
- **Description:** Creates a new customer with the specified details.
- **Request Body:**
  ```json
  {
    "merchantId": "merchant_123",
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+1234567890",
    "metadata": {}
  }
  ```
- **Response:** Customer details with customer ID

#### 2.2 Get Customer
- **Method:** `GET`
- **URL:** `{{base_url}}/api/customers/{{customer_id}}`
- **Description:** Retrieves customer details by customer ID.
- **Response:** Complete customer details

#### 2.3 Update Customer
- **Method:** `POST`
- **URL:** `{{base_url}}/api/customers/{{customer_id}}`
- **Description:** Updates customer details including name, email, phone, and metadata.
- **Request Body:**
  ```json
  {
    "name": "Jane Doe",
    "email": "jane.doe@example.com",
    "phone": "+1234567890",
    "metadata": {
      "preferences": "email"
    }
  }
  ```
- **Response:** Updated customer details

#### 2.4 Delete Customer
- **Method:** `DELETE`
- **URL:** `{{base_url}}/api/customers/{{customer_id}}`
- **Description:** Deletes a customer by customer ID.
- **Response:** Deletion confirmation

#### 2.5 List Customers
- **Method:** `GET`
- **URL:** `{{base_url}}/api/customers?limit=10&offset=0`
- **Description:** Lists customers with pagination support.
- **Query Parameters:**
  - `limit` (optional): Number of customers to return (default: 10)
  - `offset` (optional): Number of customers to skip (default: 0)
- **Response:** List of customers with pagination metadata

### 3. Payment Method Management

#### 3.1 Create Payment Method
- **Method:** `POST`
- **URL:** `{{base_url}}/api/payment_methods`
- **Description:** Creates a new payment method for a customer. Supports various payment method types including cards, wallets, and bank accounts.
- **Request Body:**
  ```json
  {
    "customerId": "cust_123",
    "merchantId": "merchant_123",
    "paymentMethodType": "CARD",
    "paymentMethodData": {
      "cardNumber": "4242424242424242",
      "expiryMonth": 12,
      "expiryYear": 2025,
      "cvc": "123",
      "cardholderName": "John Doe"
    }
  }
  ```
- **Response:** Payment method details with payment method ID

#### 3.2 Get Payment Method
- **Method:** `GET`
- **URL:** `{{base_url}}/api/payment_methods/{{payment_method_id}}`
- **Description:** Retrieves payment method details by payment method ID.
- **Response:** Complete payment method details

#### 3.3 List Customer Payment Methods
- **Method:** `GET`
- **URL:** `{{base_url}}/api/customers/{{customer_id}}/payment_methods`
- **Description:** Lists all payment methods for a specific customer.
- **Response:** List of payment methods for the customer

#### 3.4 Set Default Payment Method
- **Method:** `POST`
- **URL:** `{{base_url}}/api/customers/{{customer_id}}/payment_methods/{{payment_method_id}}/default`
- **Description:** Sets a payment method as the default for a customer.
- **Response:** Success confirmation

#### 3.5 Delete Payment Method
- **Method:** `DELETE`
- **URL:** `{{base_url}}/api/payment_methods/{{payment_method_id}}`
- **Description:** Deletes a payment method by payment method ID.
- **Response:** Deletion confirmation

#### 3.6 Get Payment Method by Client Secret
- **Method:** `GET`
- **URL:** `{{base_url}}/api/payment_methods/client_secret?client_secret={{client_secret}}`
- **Description:** Retrieves a payment method using its client secret.
- **Query Parameters:**
  - `client_secret` (required): Client secret for the payment method
- **Response:** Payment method details

### 4. 3DS Authentication

#### 4.1 3DS Challenge
- **Method:** `POST`
- **URL:** `{{base_url}}/api/payments/{{payment_id}}/3ds/challenge`
- **Description:** Initiates a 3DS challenge for payment authentication. Returns a redirect URL for customer authentication.
- **Request Body:**
  ```json
  {
    "returnUrl": "https://example.com/return"
  }
  ```
- **Response:** Redirect URL and authentication ID

#### 4.2 3DS Resume
- **Method:** `POST`
- **URL:** `{{base_url}}/api/payments/{{payment_id}}/3ds/resume`
- **Description:** Resumes a payment after 3DS authentication is completed.
- **Request Body:**
  ```json
  {
    "authenticationId": "auth_123"
  }
  ```
- **Response:** Payment status after authentication

#### 4.3 3DS Callback
- **Method:** `POST`
- **URL:** `{{base_url}}/api/payments/{{payment_id}}/3ds/callback`
- **Description:** Handles 3DS authentication callback from the payment provider.
- **Request Body:**
  ```json
  {
    "authenticationId": "auth_123",
    "status": "SUCCEEDED"
  }
  ```
- **Response:** Payment status update

### 5. Refunds

#### 5.1 Create Refund
- **Method:** `POST`
- **URL:** `{{base_url}}/api/payments/{{payment_id}}/refund`
- **Description:** Creates a refund for a payment. Supports full and partial refunds.
- **Request Body:**
  ```json
  {
    "amount": {
      "value": 500,
      "currencyCode": "USD"
    },
    "reason": "Customer requested refund",
    "metadata": {}
  }
  ```
- **Response:** Refund details with refund ID

#### 5.2 Get Refund
- **Method:** `GET`
- **URL:** `{{base_url}}/api/refunds/{{refund_id}}`
- **Description:** Retrieves refund details by refund ID.
- **Response:** Complete refund details

#### 5.3 List Refunds
- **Method:** `POST`
- **URL:** `{{base_url}}/api/refunds/list`
- **Description:** Lists refunds with filtering by status, connector, currency, time range, and amount. Supports pagination.
- **Request Body:**
  ```json
  {
    "limit": 10,
    "offset": 0,
    "status": "SUCCEEDED",
    "connector": "stripe",
    "currency": "USD"
  }
  ```
- **Response:** List of refunds with pagination metadata

#### 5.4 Get Refund Filters
- **Method:** `GET`
- **URL:** `{{base_url}}/api/refunds/filter`
- **Description:** Returns available filter options for refunds (connectors, currencies, statuses).
- **Response:** Available filter options

#### 5.5 Sync Refund
- **Method:** `POST`
- **URL:** `{{base_url}}/api/refunds/sync`
- **Description:** Syncs refund status with the connector. Use forceSync=true to force a sync even if recently synced.
- **Request Body:**
  ```json
  {
    "forceSync": false
  }
  ```
- **Response:** Refund sync status

## Running the Tests

### Option 1: Run Individual Requests
1. Expand the collection in Postman
2. Navigate to the desired folder (e.g., "Core Payment Operations")
3. Click on any request
4. Update variables if needed (payment_id, customer_id, etc.)
5. Click **Send** button

### Option 2: Run Collection
1. Click on the collection name
2. Click **Run** button (top right)
3. Select all requests or specific ones
4. Click **Run High Priority Endpoints** button

## Typical Payment Flow

Here's a typical payment flow using these endpoints:

1. **Create Customer** (`POST /api/customers`)
   - Create a customer record
   - Save the `customer_id` from the response

2. **Create Payment Method** (`POST /api/payment_methods`)
   - Create a payment method for the customer
   - Save the `payment_method_id` from the response

3. **Create Payment** (`POST /api/payments`)
   - Create a payment intent
   - Save the `payment_id` from the response

4. **Confirm Payment** (`POST /api/payments/{payment_id}/confirm`)
   - Confirm and process the payment
   - If 3DS is required, you'll get a redirect URL

5. **3DS Challenge** (if required) (`POST /api/payments/{payment_id}/3ds/challenge`)
   - Initiate 3DS authentication
   - Redirect customer to the returned URL

6. **3DS Resume** (after authentication) (`POST /api/payments/{payment_id}/3ds/resume`)
   - Resume payment after 3DS completion

7. **Capture Payment** (if manual capture) (`POST /api/payments/{payment_id}/capture`)
   - Capture the authorized payment

8. **Create Refund** (if needed) (`POST /api/payments/{payment_id}/refund`)
   - Create a refund for the payment

## Expected Results

### Successful Responses
- **200 OK:** Request successful
- **201 Created:** Resource created successfully
- Response body contains the requested data

### Error Responses
- **400 Bad Request:** Invalid request data
- **401 Unauthorized:** Authentication required (if auth is enabled)
- **404 Not Found:** Resource not found
- **500 Internal Server Error:** Server error

## Troubleshooting

### Connection Refused
- **Issue:** Cannot connect to the service
- **Solution:** 
  - Verify the service is running: Check `http://localhost:8080/api/health`
  - Check the port in `application.yml` (default: 8080)
  - Verify `base_url` variable in Postman collection

### 401 Unauthorized
- **Issue:** Getting 401 Unauthorized
- **Solution:**
  - Check if authentication is enabled (`hyperswitch.security.enable-auth: true`)
  - Verify `X-Merchant-Id` header is set correctly
  - Ensure the merchant ID exists and is valid

### 400 Bad Request
- **Issue:** Invalid request data
- **Solution:**
  - Check request body format (must be valid JSON)
  - Verify required fields are present
  - Check data types match the expected format
  - Review error message in response body for details

### 404 Not Found
- **Issue:** Resource not found
- **Solution:**
  - Verify the resource ID exists (payment_id, customer_id, etc.)
  - Check the endpoint URL is correct
  - Ensure the resource belongs to the specified merchant

### Payment Status Issues
- **Issue:** Payment cannot be confirmed/captured/cancelled
- **Solution:**
  - Check payment status - some operations are only allowed in specific states
  - Verify payment is in the correct state for the operation
  - Review payment status in the response

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
   cd Payment-service/paymentservice
   mvn clean package spring-boot:repackage -DskipTests
   cd paymentservice-web/target
   java "-Dspring.classformat.ignore=true" -jar paymentservice-web-1.0.0-SNAPSHOT.jar
   ```

4. **Verify Service is Running:**
   - Open browser: `http://localhost:8080/api/health`
   - Should return: `{"status": "healthy", "service": "hyperswitch-payment-service"}`

## Authentication Details

### When Authentication is Disabled (Default)
- All endpoints are accessible without authentication
- `X-Merchant-Id` header is still sent but not validated
- Configuration: `hyperswitch.security.enable-auth: false` in `application.yml`

### When Authentication is Enabled
- Endpoints require API key authentication
- `X-Merchant-Id` header must contain a valid API key
- Configuration: `hyperswitch.security.enable-auth: true` in `application.yml` or `ENABLE_AUTH=true` environment variable

## Notes

- All endpoints use JSON for request/response bodies
- Amount values are in minor currency units (e.g., $10.00 = 1000 for USD)
- Payment statuses: `REQUIRES_CONFIRMATION`, `REQUIRES_CUSTOMER_ACTION`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `CANCELLED`
- Refund statuses: `PENDING`, `SUCCEEDED`, `FAILED`
- All timestamps are in ISO 8601 format
- Metadata fields support custom key-value pairs

## Additional Resources

- [Architecture and Running Guide](./ARCHITECTURE_AND_RUNNING.md)
- [Health Checks Postman Collection](./HEALTH_CHECKS_POSTMAN_README.md)
- [Implementation Summary](../IMPLEMENTATION_SUMMARY.md)
- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Postman Documentation](https://learning.postman.com/docs/getting-started/introduction/)

