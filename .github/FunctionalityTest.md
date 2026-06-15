# Event Gateway Service Functionality Test

This document describes the functional test flow for the Event Gateway service and the internal Account Service it calls.

## Purpose

Verify that the Event Gateway can safely submit account transactions, handle duplicates, retrieve balances and account details, and report health status.

## Test Environment

- Account Service running on `http://localhost:8081`
- Event Gateway service should route internal transaction events to Account Service
- Use `X-Trace-Id` for distributed tracing
- Use H2 in-memory database for test cleanup

## Key Flows to Test

1. Submit a credit transaction through the gateway
2. Submit a debit transaction through the gateway
3. Submit the same `eventId` twice and verify idempotency
4. Verify account balance after multiple transactions
5. Verify `GET /accounts/{accountId}` transaction history is sorted by `eventTimestamp`
6. Verify invalid payloads return `400 Bad Request`
7. Verify missing account balance returns `404 Not Found`
8. Verify health endpoint and metrics availability

## Sample Request URLs

### Apply transaction

```
POST http://localhost:8081/accounts/{accountId}/transactions
```

- Path parameter: `accountId`
- Body JSON fields:
  - `eventId`
  - `type` (`CREDIT` or `DEBIT`)
  - `amount`
  - `currency`
  - `eventTimestamp`

### Get balance

```
GET http://localhost:8081/accounts/{accountId}/balance
```

- Path parameter: `accountId`

### Get account details

```
GET http://localhost:8081/accounts/{accountId}
```

- Path parameter: `accountId`

### Health

```
GET http://localhost:8081/health
```

## Sample Request Body

```json
{
  "eventId": "evt-1001",
  "type": "CREDIT",
  "amount": 150.00,
  "currency": "USD",
  "eventTimestamp": "2026-06-14T12:00:00Z"
}
```

## Functional Test Flow

### 1. Credit transaction

- Request:
  - `POST /accounts/acct-123/transactions`
  - Body with `type: CREDIT`, `amount: 150.00`
- Verify:
  - Response status `200`
  - Balance returned is `150.00`
  - `X-Trace-Id` returned in response header

### 2. Debit transaction

- Request:
  - `POST /accounts/acct-123/transactions`
  - Body with `type: DEBIT`, `amount: 50.00`
- Verify:
  - Response status `200`
  - Balance returned is `100.00`

### 3. Duplicate `eventId`

- Request the same transaction payload twice for the same `eventId`
- Verify:
  - first request applies balance change
  - second request returns the same balance without double-processing
  - account transaction history count does not increase for duplicate event

### 4. Balance calculation after multiple transactions

- Submit several transactions for the same account:
  - `CREDIT 200.00`
  - `DEBIT 75.00`
  - `CREDIT 50.00`
- Verify:
  - final balance equals `175.00`
  - internal service uses incremental balance updates, not Gateway-derived values

### 5. Out-of-order transaction history

- Submit a later `eventTimestamp` transaction first, then an earlier one
- Verify:
  - account details endpoint returns transactions sorted by `eventTimestamp`
  - balance remains correct based on all applied transactions

### 6. Invalid amount

- Request with `amount: 0.00` or missing amount
- Verify:
  - response status `400`
  - error code is `BAD_REQUEST` or `VALIDATION_FAILED`

### 7. Invalid type

- Request with `type: TRANSFER`
- Verify:
  - response status `400`
  - error code is `BAD_REQUEST`

### 8. Missing required fields

- Request with omitted `eventId`, `currency`, or `eventTimestamp`
- Verify:
  - response status `400`
  - error code is `VALIDATION_FAILED`

### 9. Missing account balance

- Request:
  - `GET /accounts/nonexistent-account/balance`
- Verify:
  - response status `404`
  - error code is `ACCOUNT_NOT_FOUND`

### 10. Health and metrics

- Request:
  - `GET /health`
- Verify:
  - response contains `status: UP`
  - response contains database status
- Also verify actuator metrics at `/actuator/metrics`

## Notes for Event Gateway Testers

- The Event Gateway should call the Account Service using REST only
- `eventId` is the idempotency key
- Account Service is the source of truth for current balance
- Transaction history is stored and returned for audit
- Use `X-Trace-Id` to correlate requests between Gateway and Account Service
