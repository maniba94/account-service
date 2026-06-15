# Account Service

Internal Spring Boot service for account balances and transaction history.

## Architecture Overview

The `account-service` is an internal microservice in an event ledger ecosystem. It is called by a separate public `event-gateway-service` that accepts client transaction events and forwards them to this service.

- `event-gateway-service` (external/public): receives client transaction requests, validates them, and routes them to `account-service`.
- `account-service` (internal): stores account balances and transaction history, protects against duplicate processing using `eventId`, and exposes account/balance APIs for internal consumption.

The two services interact over REST. The gateway forwards transaction events to `account-service` and propagates `X-Trace-Id` for distributed tracing.

## Prerequisites

- Java 17 installed
- Docker installed for containerized execution (optional)
- Git installed to clone the repository
- No manual dependency installation is required; Gradle wrapper downloads dependencies automatically

## Setup

From the project root:

```bash
./gradlew clean build
```

This command downloads dependencies, compiles the code, and runs the build lifecycle.

## Running the Account Service

### Manual

From the project root:

```bash
./gradlew bootRun
```

The service starts on port `8081` by default.

### Docker

Build the image:

```bash
docker build -t account-service .
```

Run the container:

```bash
docker run --rm -p 8081:8081 account-service
```

## Running Both Services

Because this repository contains only the internal `account-service`, start the `event-gateway-service` from its own repository or environment. Then run the `account-service` on `http://localhost:8081`.

A sample `docker-compose.yml` for local integration could look like:

```yaml
version: '3.9'
services:
  account-service:
    build: .
    ports:
      - '8081:8081'

  event-gateway-service:
    image: event-gateway-service
    ports:
      - '8080:8080'
    environment:
      ACCOUNT_SERVICE_URL: http://account-service:8081
```

> Note: the actual `event-gateway-service` Docker image and configuration are managed in the gateway repository.

## Tests

Run unit and integration tests with Gradle:

```bash
./gradlew test
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

## Resiliency Pattern

The service uses idempotent transaction processing as its primary resiliency pattern. By enforcing unique `eventId` values and storing processed transaction history, duplicate submissions do not change account balances and the service remains consistent under retry scenarios.

Additional resilience features include:

- `X-Trace-Id` propagation for request correlation and observability
- internal REST contract stability between the gateway and account service
- a health endpoint for readiness and liveness checks

## Notes

- `X-Trace-Id` is read from requests and returned in responses.
- Transaction history is stored and returned sorted by `eventTimestamp`.
- Duplicate `eventId` submissions do not alter the account balance.
