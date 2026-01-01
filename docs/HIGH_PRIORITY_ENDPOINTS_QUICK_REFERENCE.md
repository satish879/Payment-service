# High Priority Endpoints - Quick Reference

This document provides a quick reference guide for all high priority (critical) endpoints in the Hyperswitch Payment Service.

## Endpoint Summary

### Core Payment Operations (7 endpoints)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments` | Create payment intent |
| POST | `/api/payments/{id}/confirm` | Confirm payment |
| POST | `/api/payments/{id}/capture` | Capture payment |
| GET | `/api/payments/{id}` | Get payment details |
| POST | `/api/payments/{id}` | Update payment |
| POST | `/api/payments/{id}/cancel` | Cancel payment |
| GET | `/api/payments/{id}/client_secret` | Get client secret |

### Customer Management (5 endpoints)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/customers` | Create customer |
| GET | `/api/customers/{id}` | Get customer |
| POST | `/api/customers/{id}` | Update customer |
| DELETE | `/api/customers/{id}` | Delete customer |
| GET | `/api/customers` | List customers |

### Payment Method Management (6 endpoints)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payment_methods` | Create payment method |
| GET | `/api/payment_methods/{id}` | Get payment method |
| GET | `/api/customers/{id}/payment_methods` | List customer payment methods |
| POST | `/api/customers/{id}/payment_methods/{pm_id}/default` | Set default payment method |
| DELETE | `/api/payment_methods/{id}` | Delete payment method |
| GET | `/api/payment_methods/client_secret` | Get payment method by client secret |

### 3DS Authentication (3 endpoints)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments/{id}/3ds/challenge` | Initiate 3DS challenge |
| POST | `/api/payments/{id}/3ds/resume` | Resume after 3DS |
| POST | `/api/payments/{id}/3ds/callback` | Handle 3DS callback |

### Refunds (5 endpoints)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments/{id}/refund` | Create refund |
| GET | `/api/refunds/{id}` | Get refund |
| POST | `/api/refunds/list` | List refunds |
| GET | `/api/refunds/filter` | Get refund filters |
| POST | `/api/refunds/sync` | Sync refund status |

**Total: 26 high priority endpoints**

## Common Request Headers

All endpoints require:
- `Content-Type: application/json` (for POST/PUT requests)
- `X-Merchant-Id: {merchant_id}` (merchant identifier)

## Common Response Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request |
| 401 | Unauthorized |
| 404 | Not Found |
| 500 | Internal Server Error |

## Payment Status Flow

```
REQUIRES_CONFIRMATION → PROCESSING → SUCCEEDED
                              ↓
                         REQUIRES_CUSTOMER_ACTION (3DS)
                              ↓
                         PROCESSING → SUCCEEDED
                              ↓
                         FAILED / CANCELLED
```

## Typical Payment Flow

1. **Create Customer** → Get `customer_id`
2. **Create Payment Method** → Get `payment_method_id`
3. **Create Payment** → Get `payment_id`
4. **Confirm Payment** → Payment processed
5. **3DS Challenge** (if required) → Redirect customer
6. **3DS Resume** → Complete payment
7. **Capture Payment** (if manual capture)
8. **Create Refund** (if needed)

## Amount Format

All amounts are in **minor currency units**:
- USD: $10.00 = `1000`
- EUR: €10.00 = `1000`
- JPY: ¥10 = `10` (no decimal places)

## Currency Codes

Use ISO 4217 currency codes:
- `USD` - US Dollar
- `EUR` - Euro
- `GBP` - British Pound
- `JPY` - Japanese Yen
- `INR` - Indian Rupee

## Payment Method Types

- `CARD` - Credit/Debit Card
- `WALLET` - Digital Wallet
- `BANK_TRANSFER` - Bank Transfer
- `UPI` - Unified Payments Interface
- `NETBANKING` - Net Banking

## Common Request Patterns

### Create Payment
```json
{
  "amount": {
    "value": 1000,
    "currencyCode": "USD"
  },
  "merchantId": "merchant_123",
  "paymentMethod": "CARD",
  "customerId": "cust_123",
  "description": "Payment description",
  "captureMethod": "AUTOMATIC",
  "confirm": false
}
```

### Create Customer
```json
{
  "merchantId": "merchant_123",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "phone": "+1234567890"
}
```

### Create Payment Method
```json
{
  "customerId": "cust_123",
  "merchantId": "merchant_123",
  "paymentMethodType": "CARD",
  "paymentMethodData": {
    "cardNumber": "4242424242424242",
    "expiryMonth": 12,
    "expiryYear": 2025,
    "cvc": "123"
  }
}
```

### Create Refund
```json
{
  "amount": {
    "value": 500,
    "currencyCode": "USD"
  },
  "reason": "Customer requested refund"
}
```

## Error Response Format

```json
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Invalid payment amount",
    "details": {}
  }
}
```

## Testing Tips

1. **Use Postman Variables**: Set up collection variables for `merchant_id`, `customer_id`, `payment_id`, etc.
2. **Follow the Flow**: Use the typical payment flow sequence
3. **Check Status**: Always check payment status before performing operations
4. **Handle 3DS**: Be prepared to handle 3DS challenges for card payments
5. **Test Error Cases**: Test with invalid data to understand error responses

## Related Documentation

- [High Priority Endpoints Postman README](./HIGH_PRIORITY_ENDPOINTS_POSTMAN_README.md) - Detailed documentation
- [Health Checks Postman README](./HEALTH_CHECKS_POSTMAN_README.md) - Health check endpoints
- [Architecture and Running Guide](./ARCHITECTURE_AND_RUNNING.md) - Service architecture
- [Implementation Summary](../IMPLEMENTATION_SUMMARY.md) - Complete feature list

