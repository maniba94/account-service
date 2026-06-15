## **Agent Instructions — Account Service** 

You are a senior Java Spring Boot developer building the `account-service` for a distributed Event Ledger system. 

## **Project Context** 

This is the internal Account Service. It is called by the public `event-gateway-service` . 

The Account Service is responsible for maintaining account balances, storing transaction history, protecting against duplicate transaction processing, and exposing account/balance information. 

Tech stack: 

- Java 17 
- Spring Boot 3.5.15 
- Gradle Groovy 
- Spring Web 
- Spring Data JPA
- H2 Database 
- Validation 
- Actuator 
- JUnit 5 

Base package: `com.maniba.eventledger.account` 

Do not implement Event Gateway API code in this project. This service must expose only internal accountrelated APIs. 

## **Global Engineering Rules** 

Follow these rules throughout all phases: 

1. Write clean, production-quality Spring Boot code. 
2. Use controller, service, repository, entity, dto, config, exception, filter, metrics, and util packages. 
3. Use constructor injection only. 
4. Use `BigDecimal` for all monetary values. 
5. Use `Instant` for event timestamps. 
6. Do not share database, memory, entity classes, or DTO classes with Event Gateway. 
7. Communicate only through REST APIs. 
8. Enforce idempotency using unique `eventId` in the transaction table. 
9. If the same `eventId` is submitted again, do not change balance again. 
10. Store transaction history sorted by `eventTimestamp` . 
11. Return meaningful HTTP status codes and error responses. 
12. Read and log `X-Trace-Id` from incoming requests. 
13. Expose health and metrics endpoints. 
14. Add tests for credit, debit, duplicate transaction, balance calculation, out-of-order events, and trace ID handling. 
15. Do not over-engineer with Kafka, async processing, or shared libraries. 

## **Phase 1 — Project Structure** 

Create the following package structure: 
```
com.maniba.eventledger.account
├── controller
├── service
├── repository
├── entity
├── dto
├── config
├── exception
├── filter
├── metrics
└── util
```
Verify the application starts successfully on port `8081` . 

## **Phase 2 — Application Configuration** 

Create/update `application.yml` . 
Required configuration: 

```
server:
   port:8081

spring:
   application:
      name:account-service
   datasource:
      url:jdbc:h2:mem:account_service_db
      driver-class-name:org.h2.Driver
      username:sa
      password:
   h2:
      console:
         enabled:true
   jpa:
      hibernate:
         ddl-auto:update
      show-sql:true
   management:
      endpoints:
         web:
            exposure:
               include:health,metrics
```

Do not configure any Event Gateway URL here. Account Service does not call Gateway. 

## **Phase 3 — Domain Model** 

Create the Account domain model. 

## **Account Entity** 

Create `Account` . 

Fields: 

```
id
accountId
currentBalance
currency
createdAt
updatedAt
```

Constraints: 

- `accountId` must be unique and required. 
- `currentBalance` must be required. 
- Use `BigDecimal` for balance. 
- Use `Instant` for timestamps. 

## **Transaction Entity** 
Create `AccountTransaction` . 

Do not name the entity just `Transaction` because `Transaction` can be confused with Spring/JPA transaction concepts. 

Fields: 

```
id
eventId
accountId
type
amount
currency
eventTimestamp
createdAt
```

Constraints: 
- `eventId` must be unique and required. 
- `accountId` must be required. 
- `type` must be required. 
- `amount` must be required. 
- Use `BigDecimal` for amount. 
- Use `Instant` for `eventTimestamp` . 
- Store type as string or enum with allowed values `CREDIT` and `DEBIT` . 

## **Phase 4 — DTOs** 

Create request and response DTOs. 

## **TransactionRequest** 

Fields: 

```
eventId
type
amount
currency
eventTimestamp
```

Validation: 

- `eventId` required 
- `type` required 
- `amount` must be greater than 0 
- `currency` required 
- `eventTimestamp` required 

## **BalanceResponse** 

Fields: 

```
accountId
balance
currency
```

## **TransactionResponse** 

Fields: 
```
eventId
type
amount
currency
eventTimestamp
```

## **AccountResponse** 

Fields: 
```
accountId
balance
currency
transactions
```

## **ErrorResponse** 

Fields: 
```
error
message
traceId
timestamp
```

## **Phase 5 — Repository Layer** 

Create `AccountRepository` . 
Required method: 
```
Optional<Account>findByAccountId(StringaccountId);
```

Create `AccountTransactionRepository` . 
Required methods: 
```
Optional<AccountTransaction>findByEventId(StringeventId);

List<AccountTransaction>findByAccountIdOrderByEventTimestampAsc(String
accountId);
```

## **Phase 6 — Trace ID Filter** 

Create a servlet filter that: 

1. Reads `X-Trace-Id` from incoming requests. 
2. If missing, generates a UUID. 
3. Stores it in MDC as `traceId` 
4. Adds it to the response header. 
5. Clears MDC after request completion. 

The service must log this trace ID in every important log statement. 

## **Phase 7 — Business Logic** 

Create `AccountService` . 
Implement: 

```
BalanceResponseapplyTransaction(StringaccountId,TransactionRequestrequest);

BalanceResponsegetBalance(StringaccountId);

AccountResponsegetAccount(StringaccountId);
```

## **applyTransaction flow** 

1. Validate transaction type is CREDIT or DEBIT.
2. Check if transaction with eventId already exists.
3. If duplicate:
   - Do not update account balance.
   - Return the current account balance.
4. Find account by accountId.
5. If account does not exist, create a new account with balance 0.
6. If type is CREDIT, add amount to balance.
7. If type is DEBIT, subtract amount from balance.
8. Save Account.
9. Save AccountTransaction. 
10. Return updated balance.

Important: 
- Use `@Transactional` . 
- Make duplicate handling safe. • Use the database unique constraint on `eventId` as the final protection. 
- Do not calculate balance from Gateway data. 
- Account Service is the source of truth for balance. 

## **Phase 8 — REST Controller** 
Create `AccountController` . 
Expose: 
```
POST /accounts/{accountId}/transactions
GET /accounts/{accountId}/balance
GET /accounts/{accountId}
GET /health
```

Behavior: 

## **POST /accounts/{accountId}/transactions** 
- Applies credit/debit transaction. 
- Returns updated balance. 
- Duplicate event returns current balance without changing it again.
- Invalid payload returns `400 Bad Request` . 

## **GET /accounts/{accountId}/balance** 
- Returns current balance. 
- If account does not exist, return `404 Not Found` . 

## **GET /accounts/{accountId}** 
- Returns account details and transaction history. 
- Transactions must be sorted by `eventTimestamp` ascending. 
- If account does not exist, return `404 Not Found` . 

## **GET /health** 
- Return service status and database status. 

## **Phase 9 — Exception Handling** 
Create a global exception handler. 
Handle: 
```
BadRequestException → 400
AccountNotFoundException → 404
MethodArgumentNotValidException → 400
DataIntegrityViolationException → safe duplicate/idempotency response if
applicable, otherwise 409
Generic Exception → 500
```

Error response format: 

```
{
"error":"ERROR_CODE",
"message":"Human-readable message",
"traceId":"trace-id-value",
"timestamp":"2026-06-14T00:00:00Z"
}
```

Do not expose stack traces in API responses. 

## **Phase 10 — Observability** 
Implement structured, meaningful logs for: 

```
transaction received
duplicate transaction detected
account created
balance updated
transaction stored
account lookup failed
```

Add Micrometer counters: 

```
transactions.applied.count
transactions.duplicate.count
transactions.failed.count
```

Expose metrics through Actuator: 

```
/actuator/metrics
```

Expose health through: 

```
/actuator/health
```

Also add custom: 

```
GET /health
```


## **Phase 11 — Data Correctness Rules** 

Enforce these correctness rules: 
1. Duplicate `eventId` must not update balance twice. 
2. Out-of-order event timestamps must not break balance calculation. 
3. Returned transaction history must be sorted by `eventTimestamp` . 
4. CREDIT increases balance. 
5. DEBIT decreases balance. 
6. Amount must be greater than zero. 
7. Missing fields must return validation errors. 
8. Invalid type must return bad request. 
9. Monetary calculations must never use `double` . 

## **Phase 12 — Tests** 

Create unit and integration tests. 
Minimum test coverage: 

```
POST credit transaction increases balance
POST debit transaction decreases balance
duplicate eventId does not double-process transaction
same account multiple transactions calculate correct balance
out-of-order transaction timestamps return sorted transaction history
invalid amount returns 400
invalid type returns 400
missing required fields return 400
GET balance for missing account returns 404
X-Trace-Id is returned in response header
```
Use: 
- JUnit 5 • Spring Boot Test 
- MockMvc 
- H2 

Do not require Event Gateway to be running for Account Service tests. 

## **Phase 13 — Docker Support** 

Create `Dockerfile` . 
Use Java 17 image. 
The service must run with: 

```
dockerbuild-taccount-service.
dockerrun-p8081:8081account-service
```

Dockerfile: 

```
FROMeclipse-temurin:17-jdk
WORKDIR/app
COPYbuild/libs/*.jarapp.jar
ENTRYPOINT["java","-jar","app.jar"]
```

## **Phase 14 — README Section** 
Add or update README for Account Service. 
Include: 
```
Service purpose
API endpoints
Request/response examples
How idempotency works
How balance is calculated
How out-of-order events are handled
How to run locally
How to run tests
How to run with Docker
```

Mention: 

```
Balance = Sum(CREDIT) - Sum(DEBIT)
```

Current balance may be maintained incrementally, but transaction history remains available for audit. 



## **Phase 15 — Final Quality Review** 

Before finishing, verify: 
1. Application starts on port 8081. 
2. H2 database is configured separately from Gateway. 
3. `POST /accounts/{accountId}/transactions` works. 
4. Duplicate `eventId` does not change balance twice. 
5. Credit and debit both work. 
6. Account details return sorted transaction history. 
7. Invalid request returns 400. 
8. Missing account returns 404. 
9. Trace ID appears in logs and response header. 
10. Metrics are exposed. 
11. Tests pass. 
12. No Event Gateway code exists in this service. 
13. No shared database or shared entity/DTO dependency is used. 

This service should be small, focused, internally reliable, and clearly responsible for account state. 