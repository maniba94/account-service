# Account Service

Internal Spring Boot service for account balances and transaction history.

## Overview

- Java 17, Spring Boot 3.5.15
- Spring Web, Spring Data JPA, H2 in-memory database
- Validation, Actuator, JUnit 5
- Exposes internal REST APIs for account transaction processing, balance lookup, and account details
- Enforces idempotency with unique `eventId`
- Logs and propagates `X-Trace-Id`

## Build

From the project root:

```bash
./gradlew clean build
```

## Test

Run unit and integration tests:

```bash
./gradlew test
```

## Run

```bash
./gradlew bootRun
```

The service listens on port `8081`.

## Docker

Build the image:

```bash
docker build -t account-service .
```

Run the container:

```bash
docker run --rm -p 8081:8081 account-service
```

## API Endpoints

### Apply transaction

`POST /accounts/{accountId}/transactions`

Request body:

```json
{
  "eventId": "evt-123",
  "type": "CREDIT",
  "amount": 100.00,
  "currency": "USD",
  "eventTimestamp": "2025-01-01T10:00:00Z"
}
```

### Get balance

`GET /accounts/{accountId}/balance`

### Get account details

`GET /accounts/{accountId}`

### Health

`GET /accounts/health`

## Notes

- `X-Trace-Id` is read from requests and returned in responses.
- Transaction history is stored and returned sorted by `eventTimestamp`.
- Duplicate `eventId` submissions do not alter the account balance.
