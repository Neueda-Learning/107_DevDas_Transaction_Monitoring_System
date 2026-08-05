# Transaction Management

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Package Structure](#package-structure)
4. [Domain Model](#domain-model)
   - [Transaction Entity](#transaction-entity)
   - [TransactionStatus Enum](#transactionstatus-enum)
   - [TransactionType Enum](#transactiontype-enum)
5. [Database Schema](#database-schema)
6. [DTOs](#dtos)
   - [CreateTransactionRequest](#createtransactionrequest)
   - [TransactionResponse](#transactionresponse)
   - [TransactionDecisionRequest](#transactiondecisionrequest)
   - [TransactionRollbackRequest](#transactionrollbackrequest)
   - [TransactionRollbackDecisionRequest](#transactionrollbackdecisionrequest)
   - [TransactionFilterRequest](#transactionfilterrequest)
7. [Repository Layer](#repository-layer)
   - [TransactionRepository Interface](#transactionrepository-interface)
   - [JdbcTransactionRepository Implementation](#jdbctransactionrepository-implementation)
8. [Service Layer](#service-layer)
   - [TransactionService Interface](#transactionservice-interface)
   - [TransactionServiceImpl](#transactionserviceimpl)
9. [Controller Layer](#controller-layer)
   - [TransactionController](#transactioncontroller)
10. [API Reference](#api-reference)
    - [POST /api/v1/transactions](#post-apiv1transactions)
    - [GET /api/v1/transactions](#get-apiv1transactions)
    - [GET /api/v1/transactions/{id}](#get-apiv1transactionsid)
    - [PATCH /api/v1/transactions/{id}/approve](#patch-apiv1transactionsidapprove)
    - [PATCH /api/v1/transactions/{id}/reject](#patch-apiv1transactionsidreject)
    - [PATCH /api/v1/transactions/{id}/rollback/request](#patch-apiv1transactionsidrollbackrequest)
    - [PATCH /api/v1/transactions/{id}/rollback/approve](#patch-apiv1transactionsidrollbackapprove)
    - [PATCH /api/v1/transactions/{id}/rollback/reject](#patch-apiv1transactionsidrollbackreject)
11. [Transaction Lifecycle](#transaction-lifecycle)
12. [Validation Rules](#validation-rules)
13. [Error Handling](#error-handling)
14. [Integration with Rule Engine](#integration-with-rule-engine)

---

## Overview

The Transaction Management module is the core component of the Transaction Monitoring System (TMS). It is responsible for:

- **Recording** financial transactions submitted via REST API.
- **Evaluating** each transaction immediately against all active monitoring rules (via the Rule Engine).
- **Managing** the full lifecycle of a transaction from creation through approval, rejection, rollback, and refund.
- **Providing** paginated, filtered search over all stored transactions.

All transaction data is persisted to a MySQL database using Spring's `JdbcClient` directly — no ORM is used. This keeps queries explicit, transparent, and easy to optimize.

---

## Architecture

```
HTTP Request
     │
     ▼
┌─────────────────────┐
│  TransactionController  │  ← REST layer (@RestController)
└──────────┬──────────┘
           │ delegates to
           ▼
┌─────────────────────┐
│  TransactionService     │  ← Business logic interface
│  (TransactionServiceImpl)  │
└──────────┬──────────┘
           │ uses
     ┌─────┴──────┐
     │            │
     ▼            ▼
TransactionRepo  RuleEngineService + AlertService
(JdbcClient)     (evaluate rules, create alerts)
     │
     ▼
  MySQL DB
  (transactions table)
```

**Key design decisions:**
- **Controller** only handles HTTP concerns: routing, request parsing, and response wrapping.
- **Service** owns all business logic: transaction creation, lifecycle transitions, validation.
- **Repository** owns all SQL: insert, update, select, and dynamic filtering.
- **Rule engine** is called synchronously within the same transaction on every creation.

---

## Package Structure

```
com.hsbc.tms.transaction
├── controller
│   └── TransactionController.java       ← REST endpoints
├── dto
│   ├── CreateTransactionRequest.java    ← Inbound: create a transaction
│   ├── TransactionDecisionRequest.java  ← Inbound: approve / reject
│   ├── TransactionFilterRequest.java    ← Internal: filter params
│   ├── TransactionResponse.java         ← Outbound: all API responses
│   ├── TransactionRollbackDecisionRequest.java  ← Inbound: approve/reject rollback
│   └── TransactionRollbackRequest.java  ← Inbound: request a rollback
├── model
│   ├── Transaction.java                 ← Domain entity (plain Java)
│   ├── TransactionStatus.java           ← Status enum
│   └── TransactionType.java             ← Type enum (DEBIT / CREDIT)
├── repository
│   ├── TransactionRepository.java       ← Repository interface
│   └── JdbcTransactionRepository.java   ← JDBC implementation
└── service
    ├── TransactionService.java          ← Service interface
    └── TransactionServiceImpl.java      ← Service implementation
```

---

## Domain Model

### Transaction Entity

**File:** `com.hsbc.tms.transaction.model.Transaction`

The `Transaction` class is a plain Java object (no JPA annotations). All field mapping is done explicitly in `JdbcTransactionRepository`.

| Field | Type | Description |
|---|---|---|
| `id` | `UUID` | Unique identifier, generated on creation |
| `accountId` | `String` | The source account ID (max 50 chars) |
| `payeeId` | `String` | The destination payee ID (max 50 chars) |
| `amount` | `BigDecimal` | Transaction amount (precision 19, scale 2) |
| `currency` | `String` | 3-letter ISO 4217 currency code (e.g. `USD`) |
| `type` | `TransactionType` | `DEBIT` or `CREDIT` |
| `status` | `TransactionStatus` | Current lifecycle status |
| `transactionTime` | `Instant` | When the transaction occurred (UTC) |
| `description` | `String` | Optional free-text note (max 255 chars) |
| `createdAt` | `Instant` | Record creation timestamp (UTC) |
| `updatedAt` | `Instant` | Last modification timestamp (UTC) |
| **Review fields** | | Set when a pending transaction is approved/rejected |
| `reviewedBy` | `String` | Operator ID who reviewed the transaction |
| `reviewedAt` | `Instant` | When the review was performed |
| `reviewNote` | `String` | Operator note on review decision |
| **Rollback fields** | | Set when a rollback is requested |
| `rollbackReasonCode` | `String` | Normalised uppercase reason code (e.g. `CUSTOMER_REQUEST`) |
| `rollbackReasonDetail` | `String` | Detailed reason description |
| `rollbackRequestedBy` | `String` | Operator ID who requested the rollback |
| `rollbackRequestedAt` | `Instant` | Timestamp of rollback request |
| `rollbackSupportingReference` | `String` | Optional case / ticket reference |
| `rollbackReviewedBy` | `String` | Operator ID who reviewed the rollback |
| `rollbackReviewedAt` | `Instant` | Timestamp of rollback review |
| `rollbackReviewNote` | `String` | Note explaining the rollback decision |
| **Refund fields** | | Set when a rollback is approved and refund issued |
| `refundedAt` | `Instant` | When the refund was issued |
| `refundTransactionId` | `UUID` | ID of the system-generated refund transaction |
| `refundedForTransactionId` | `UUID` | (On the refund record) links back to the original transaction |

---

### TransactionStatus Enum

**File:** `com.hsbc.tms.transaction.model.TransactionStatus`

```
COMPLETED           → Transaction processed normally
PENDING             → Awaiting external processing (not rule-flagged)
FAILED              → Transaction failed
PENDING_APPROVAL    → Rule violation detected; awaiting operator review
REJECTED            → Operator rejected the transaction
ROLLBACK_REQUESTED  → Operator requested a rollback of a completed transaction
ROLLBACK_REJECTED   → Rollback request was rejected by another operator
REFUNDED            → Rollback approved; refund transaction issued
```

---

### TransactionType Enum

**File:** `com.hsbc.tms.transaction.model.TransactionType`

```
DEBIT   → Money leaving the account
CREDIT  → Money entering the account
```

---

## Database Schema

**Table: `transactions`** (defined in `src/main/resources/schema.sql`)

```sql
CREATE TABLE IF NOT EXISTS transactions (
    id                          CHAR(36) PRIMARY KEY,
    account_id                  VARCHAR(50)     NOT NULL,
    payee_id                    VARCHAR(50)     NOT NULL,
    amount                      DECIMAL(19, 2)  NOT NULL,
    currency                    VARCHAR(3)      NOT NULL,
    type                        VARCHAR(20)     NOT NULL,
    status                      VARCHAR(20)     NOT NULL,
    transaction_time            TIMESTAMP(6)    NOT NULL,
    description                 VARCHAR(255),
    created_at                  TIMESTAMP(6)    NOT NULL,
    updated_at                  TIMESTAMP(6)    NOT NULL,
    -- Review / approval
    reviewed_by                 VARCHAR(100),
    reviewed_at                 TIMESTAMP(6),
    review_note                 VARCHAR(1000),
    -- Rollback request
    rollback_reason_code        VARCHAR(50),
    rollback_reason_detail      VARCHAR(1000),
    rollback_requested_by       VARCHAR(100),
    rollback_requested_at       TIMESTAMP(6),
    rollback_supporting_reference VARCHAR(100),
    -- Rollback review
    rollback_reviewed_by        VARCHAR(100),
    rollback_reviewed_at        TIMESTAMP(6),
    rollback_review_note        VARCHAR(1000),
    -- Refund linkage
    refunded_at                 TIMESTAMP(6),
    refund_transaction_id       CHAR(36),
    refunded_for_transaction_id CHAR(36),
    -- Indexes
    INDEX idx_transactions_account_id (account_id),
    INDEX idx_transactions_payee_id (payee_id),
    INDEX idx_transactions_status (status),
    INDEX idx_transactions_transaction_time (transaction_time)
);
```

**Notes:**
- `id` is stored as `CHAR(36)` (UUID string format) for MySQL compatibility.
- All timestamps use `TIMESTAMP(6)` (microsecond precision, UTC).
- Indexes on `account_id`, `payee_id`, `status`, and `transaction_time` support velocity checks and filtered searches.

---

## DTOs

### CreateTransactionRequest

Used as the request body for `POST /api/v1/transactions`.

| Field | Type | Required | Constraints |
|---|---|---|---|
| `accountId` | `String` | ✅ | Not blank, max 50 chars |
| `payeeId` | `String` | ✅ | Not blank, max 50 chars |
| `amount` | `BigDecimal` | ✅ | Minimum 0.01 |
| `currency` | `String` | ✅ | Exactly 3 uppercase letters (e.g. `USD`) |
| `type` | `TransactionType` | ✅ | `DEBIT` or `CREDIT` |
| `status` | `TransactionStatus` | ✅ | Valid status value |
| `transactionTime` | `Instant` | ✅ | ISO-8601 UTC timestamp |
| `description` | `String` | ❌ | Max 255 chars |

**Example request body:**
```json
{
  "accountId": "ACC-1001",
  "payeeId": "PAY-2200",
  "amount": 15000.00,
  "currency": "USD",
  "type": "DEBIT",
  "status": "COMPLETED",
  "transactionTime": "2026-08-05T09:00:00Z",
  "description": "Wire transfer to supplier"
}
```

---

### TransactionResponse

Returned by all transaction endpoints. Contains the full state of the transaction.

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | System-generated unique ID |
| `accountId` | `String` | |
| `payeeId` | `String` | |
| `amount` | `BigDecimal` | |
| `currency` | `String` | |
| `type` | `TransactionType` | |
| `status` | `TransactionStatus` | Current status |
| `transactionTime` | `Instant` | |
| `description` | `String` | |
| `createdAt` | `Instant` | |
| `updatedAt` | `Instant` | |
| `reviewedBy` | `String` | Populated after approve/reject |
| `reviewedAt` | `Instant` | |
| `reviewNote` | `String` | |
| `rollbackReasonCode` | `String` | Populated after rollback request |
| `rollbackReasonDetail` | `String` | |
| `rollbackRequestedBy` | `String` | |
| `rollbackRequestedAt` | `Instant` | |
| `rollbackSupportingReference` | `String` | |
| `rollbackReviewedBy` | `String` | Populated after rollback decision |
| `rollbackReviewedAt` | `Instant` | |
| `rollbackReviewNote` | `String` | |
| `refundedAt` | `Instant` | Populated when refund issued |
| `refundTransactionId` | `UUID` | ID of the generated refund transaction |
| `refundedForTransactionId` | `UUID` | On refund records: ID of original transaction |

---

### TransactionDecisionRequest

Used for `PATCH /api/v1/transactions/{id}/approve` and `PATCH /api/v1/transactions/{id}/reject`.

```java
public record TransactionDecisionRequest(
    @NotBlank String operatorId,   // Required: who is making the decision
    String note                    // Optional: reason or context
)
```

**Example:**
```json
{
  "operatorId": "operator-01",
  "note": "Verified with customer and approved"
}
```

---

### TransactionRollbackRequest

Used for `PATCH /api/v1/transactions/{id}/rollback/request`.

```java
public record TransactionRollbackRequest(
    @NotBlank String reasonCode,           // Required: e.g. CUSTOMER_REQUEST
    @NotBlank String reasonDetail,         // Required: full explanation
    @NotBlank String requestedBy,          // Required: operator/customer ID
    String supportingReference             // Optional: case or ticket number
)
```

**Example:**
```json
{
  "reasonCode": "DUPLICATE_PAYMENT",
  "reasonDetail": "Payment was submitted twice due to a system error",
  "requestedBy": "operator-01",
  "supportingReference": "CASE-10452"
}
```

---

### TransactionRollbackDecisionRequest

Used for `PATCH /api/v1/transactions/{id}/rollback/approve` and `PATCH /api/v1/transactions/{id}/rollback/reject`.

```java
public record TransactionRollbackDecisionRequest(
    @NotBlank String operatorId,   // Required: who is making the rollback decision
    String note                    // Optional: reason or context
)
```

**Example:**
```json
{
  "operatorId": "operator-02",
  "note": "Confirmed duplicate. Issuing refund."
}
```

---

### TransactionFilterRequest

Used internally by the service. Populated from query parameters in `GET /api/v1/transactions`.

| Field | Type | Description |
|---|---|---|
| `accountId` | `String` | Exact match on account ID |
| `payeeId` | `String` | Exact match on payee ID |
| `status` | `TransactionStatus` | Exact status match |
| `type` | `TransactionType` | `DEBIT` or `CREDIT` |
| `minAmount` | `BigDecimal` | Lower bound on amount (inclusive) |
| `maxAmount` | `BigDecimal` | Upper bound on amount (inclusive) |
| `fromTime` | `Instant` | Start of transaction time range (inclusive) |
| `toTime` | `Instant` | End of transaction time range (inclusive) |

---

## Repository Layer

### TransactionRepository Interface

**File:** `com.hsbc.tms.transaction.repository.TransactionRepository`

Defines the contract for all database operations:

```java
public interface TransactionRepository {

    // Insert a new transaction
    Transaction save(Transaction transaction);

    // Update all fields of an existing transaction
    Transaction update(Transaction transaction);

    // Fetch by primary key; returns empty if not found
    Optional<Transaction> findById(UUID id);

    // Paginated, filtered search
    Page<Transaction> findByFilter(TransactionFilterRequest filter, Pageable pageable);
}
```

---

### JdbcTransactionRepository Implementation

**File:** `com.hsbc.tms.transaction.repository.JdbcTransactionRepository`

The concrete implementation uses Spring's `JdbcClient` (available since Spring Boot 3.2) with named parameters for all SQL.

#### `save(Transaction)`
Executes an `INSERT INTO transactions` with all core fields (id through updated_at). Optional/nullable fields (review, rollback, refund) are not included in the initial insert to keep it simple.

#### `update(Transaction)`
Executes a full `UPDATE transactions SET ... WHERE id = :id`. Every column — including all review, rollback, and refund fields — is updated in a single statement. This ensures the row always reflects the full current state.

#### `findById(UUID)`
Executes a `SELECT` with a `WHERE id = :id` clause. UUIDs are stored as `CHAR(36)` strings, so the parameter is bound as `id.toString()`. Returns `Optional.empty()` if no row matches.

#### `findByFilter(TransactionFilterRequest, Pageable)`
Builds a dynamic SQL query using a `StringBuilder`. Each filter field is only appended if non-null/non-blank, preventing unnecessary conditions and index misses.

Steps:
1. Build the `WHERE` clause dynamically.
2. Execute a `COUNT(*)` query with the same `WHERE` clause to get the total.
3. Execute the data query with `ORDER BY`, `LIMIT`, and `OFFSET` for pagination.
4. Return a Spring Data `PageImpl` with content, total count, and pageable metadata.

**Supported sort columns** (validated in the service):

| `sortBy` value | SQL column |
|---|---|
| `transactionTime` | `transaction_time` |
| `amount` | `amount` |
| `createdAt` | `created_at` |
| `updatedAt` | `updated_at` |
| `accountId` | `account_id` |
| `status` | `status` |

#### `TransactionRowMapper`
A private static inner class implementing `RowMapper<Transaction>`. Maps every `ResultSet` column to the corresponding `Transaction` field:
- UUIDs are read as `String` and parsed with `UUID.fromString()`.
- Timestamps are read as `java.sql.Timestamp` and converted to `Instant` via `.toInstant()`.
- Nullable columns are null-checked before conversion.

---

## Service Layer

### TransactionService Interface

**File:** `com.hsbc.tms.transaction.service.TransactionService`

Defines all business operations:

```java
public interface TransactionService {
    TransactionResponse createTransaction(CreateTransactionRequest request);
    TransactionResponse getTransactionById(UUID id);
    PagedResponse<TransactionResponse> findTransactions(TransactionFilterRequest filter, int page, int size, String sortBy, String sortDir);
    TransactionResponse approve(UUID id, TransactionDecisionRequest request);
    TransactionResponse reject(UUID id, TransactionDecisionRequest request);
    TransactionResponse requestRollback(UUID id, TransactionRollbackRequest request);
    TransactionResponse approveRollback(UUID id, TransactionRollbackDecisionRequest request);
    TransactionResponse rejectRollback(UUID id, TransactionRollbackDecisionRequest request);
}
```

---

### TransactionServiceImpl

**File:** `com.hsbc.tms.transaction.service.TransactionServiceImpl`

Dependencies injected via constructor:
- `TransactionRepository` — persistence
- `RuleEngineService` — rule evaluation on creation
- `AlertService` — alert resolution on approval/rejection

#### `createTransaction(CreateTransactionRequest)`

1. Build a `Transaction` object from the request fields.
2. Assign a new random `UUID` as the ID.
3. Set `createdAt` and `updatedAt` to `Instant.now()`.
4. Call `transactionRepository.save(transaction)`.
5. Call `ruleEngineService.evaluate(saved)` — runs all active monitoring rules.
6. If any rule is violated **and** the transaction status is `COMPLETED`:
   - Change status to `PENDING_APPROVAL`.
   - Set `reviewNote` to `"Rule violation detected. Operator approval required."`.
   - Call `transactionRepository.update(saved)` to persist the status change.
7. Return a `TransactionResponse` DTO mapped from the saved entity.

#### `getTransactionById(UUID)`
Looks up the transaction by ID. Throws `ResourceNotFoundException` (HTTP 404) if not found.

#### `findTransactions(TransactionFilterRequest, int, int, String, String)`
Validates ranges and sort parameters, then delegates to the repository's paginated filter query. Wraps the result in a `PagedResponse<TransactionResponse>`.

**Sort validation:** Only these values are allowed for `sortBy`: `amount`, `transactionTime`, `createdAt`, `updatedAt`, `accountId`, `status`. Any other value throws a `BadRequestException`.

**Range validation:**
- `minAmount` cannot be greater than `maxAmount`.
- `fromTime` cannot be after `toTime`.

#### `approve(UUID, TransactionDecisionRequest)`
- Requires status to be `PENDING_APPROVAL`. Throws `BadRequestException` otherwise.
- Sets status to `COMPLETED`.
- Records `reviewedBy`, `reviewedAt`, and `reviewNote`.
- Persists the updated transaction.
- Calls `alertService.resolveAlertsForTransactionDecision(...)` to close related alerts.

#### `reject(UUID, TransactionDecisionRequest)`
- Requires status to be `PENDING_APPROVAL`. Throws `BadRequestException` otherwise.
- Sets status to `REJECTED`.
- Records review audit fields.
- Calls `alertService.resolveAlertsForTransactionDecision(...)`.

#### `requestRollback(UUID, TransactionRollbackRequest)`
- Requires status to be `COMPLETED`. Throws `BadRequestException` otherwise.
- Sets status to `ROLLBACK_REQUESTED`.
- Records reason code (normalised to uppercase), reason detail, requested by, and optional supporting reference.

#### `approveRollback(UUID, TransactionRollbackDecisionRequest)`
- Requires status to be `ROLLBACK_REQUESTED`. Throws `BadRequestException` otherwise.
- Sets original transaction status to `REFUNDED`.
- Creates a **new refund transaction** (a credit with a negated amount) linked to the original.
- Saves the refund transaction; stores its ID on the original as `refundTransactionId`.
- Updates and returns the original transaction.

#### `rejectRollback(UUID, TransactionRollbackDecisionRequest)`
- Requires status to be `ROLLBACK_REQUESTED`. Throws `BadRequestException` otherwise.
- Sets status to `ROLLBACK_REJECTED`.
- Records rollback review audit fields.

---

## Controller Layer

### TransactionController

**File:** `com.hsbc.tms.transaction.controller.TransactionController`

- Annotated with `@RestController`, `@RequestMapping("/api/v1/transactions")`, and `@Validated`.
- Tagged in Swagger/OpenAPI as `"Transactions"`.
- All endpoints delegate immediately to `TransactionService`.
- Validation of `@RequestBody` is enforced via `@Valid`.
- Pagination constraints are enforced via `@Min` / `@Max` on `page` and `size` parameters.

---

## API Reference

### `POST /api/v1/transactions`

**Summary:** Record a new transaction and evaluate it against active monitoring rules.

**Request body:** `CreateTransactionRequest` (JSON)

**Responses:**

| Status | Description |
|---|---|
| `201 Created` | Transaction recorded successfully. Returns `TransactionResponse`. |
| `400 Bad Request` | Validation failed (missing required fields, invalid currency format, etc.). |

**Notes:**
- If a rule violation is detected and the status is `COMPLETED`, the status is automatically changed to `PENDING_APPROVAL` before the response is returned.
- The response always reflects the final persisted state.

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "ACC-1001",
    "payeeId": "PAY-2200",
    "amount": 15000.00,
    "currency": "USD",
    "type": "DEBIT",
    "status": "COMPLETED",
    "transactionTime": "2026-08-05T09:00:00Z",
    "description": "Wire transfer"
  }'
```

---

### `GET /api/v1/transactions`

**Summary:** Search transactions with optional filters and pagination.

**Query parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `accountId` | `String` | — | Filter by exact account ID |
| `payeeId` | `String` | — | Filter by exact payee ID |
| `status` | `TransactionStatus` | — | Filter by status |
| `type` | `TransactionType` | — | Filter by type (`DEBIT` / `CREDIT`) |
| `minAmount` | `BigDecimal` | — | Minimum amount (inclusive) |
| `maxAmount` | `BigDecimal` | — | Maximum amount (inclusive) |
| `fromTime` | `Instant` | — | Start of time range (ISO-8601) |
| `toTime` | `Instant` | — | End of time range (ISO-8601) |
| `page` | `int` | `0` | Zero-based page number |
| `size` | `int` | `20` | Page size (1–100) |
| `sortBy` | `String` | `transactionTime` | Sort field |
| `sortDir` | `String` | `desc` | Sort direction (`asc` / `desc`) |

**Responses:**

| Status | Description |
|---|---|
| `200 OK` | Returns `PagedResponse<TransactionResponse>` with pagination metadata. |
| `400 Bad Request` | Invalid filter combination or unsupported sort field. |

**Example:**
```bash
curl "http://localhost:8080/api/v1/transactions?accountId=ACC-1001&status=PENDING_APPROVAL&page=0&size=10"
```

**Response shape:**
```json
{
  "content": [ { ... } ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

---

### `GET /api/v1/transactions/{id}`

**Summary:** Retrieve a single transaction by UUID.

**Path variable:** `id` — UUID of the transaction.

**Responses:**

| Status | Description |
|---|---|
| `200 OK` | Returns `TransactionResponse`. |
| `404 Not Found` | No transaction exists with the given ID. |

---

### `PATCH /api/v1/transactions/{id}/approve`

**Summary:** Approve a transaction that is in `PENDING_APPROVAL` status.

**Request body:** `TransactionDecisionRequest`

**What happens:**
- Status changes from `PENDING_APPROVAL` → `COMPLETED`.
- `reviewedBy`, `reviewedAt`, and `reviewNote` are set.
- Related monitoring alerts are resolved.

**Responses:**

| Status | Description |
|---|---|
| `200 OK` | Returns updated `TransactionResponse`. |
| `400 Bad Request` | Transaction is not in `PENDING_APPROVAL` status. |
| `404 Not Found` | Transaction not found. |

**Example:**
```bash
curl -X PATCH http://localhost:8080/api/v1/transactions/{id}/approve \
  -H "Content-Type: application/json" \
  -d '{ "operatorId": "operator-01", "note": "Verified with customer" }'
```

---

### `PATCH /api/v1/transactions/{id}/reject`

**Summary:** Reject a transaction that is in `PENDING_APPROVAL` status.

**Request body:** `TransactionDecisionRequest`

**What happens:**
- Status changes from `PENDING_APPROVAL` → `REJECTED`.
- Review fields are recorded.
- Related monitoring alerts are resolved.

**Responses:**

| Status | Description |
|---|---|
| `200 OK` | Returns updated `TransactionResponse`. |
| `400 Bad Request` | Transaction is not in `PENDING_APPROVAL` status. |
| `404 Not Found` | Transaction not found. |

---

### `PATCH /api/v1/transactions/{id}/rollback/request`

**Summary:** Request a rollback for a `COMPLETED` transaction.

**Request body:** `TransactionRollbackRequest`

**What happens:**
- Status changes from `COMPLETED` → `ROLLBACK_REQUESTED`.
- Reason code is normalised to uppercase.
- All rollback request fields are recorded.

**Responses:**

| Status | Description |
|---|---|
| `200 OK` | Returns updated `TransactionResponse`. |
| `400 Bad Request` | Transaction is not in `COMPLETED` status. |
| `404 Not Found` | Transaction not found. |

---

### `PATCH /api/v1/transactions/{id}/rollback/approve`

**Summary:** Approve a rollback request and automatically issue a refund transaction.

**Request body:** `TransactionRollbackDecisionRequest`

**What happens:**
1. Status changes from `ROLLBACK_REQUESTED` → `REFUNDED`.
2. A **new refund transaction** is created with:
   - Same `accountId`, `payeeId`, `currency`, and `type` as the original.
   - `amount` = original amount **negated** (negative value).
   - `status` = `COMPLETED`.
   - `description` = `"Refund for <original-id>"`.
   - `refundedForTransactionId` = original transaction ID.
3. The original transaction's `refundTransactionId` is set to the new refund ID.

**Responses:**

| Status | Description |
|---|---|
| `200 OK` | Returns updated original `TransactionResponse` with `refundTransactionId` populated. |
| `400 Bad Request` | Transaction is not in `ROLLBACK_REQUESTED` status. |
| `404 Not Found` | Transaction not found. |

---

### `PATCH /api/v1/transactions/{id}/rollback/reject`

**Summary:** Reject a rollback request.

**Request body:** `TransactionRollbackDecisionRequest`

**What happens:**
- Status changes from `ROLLBACK_REQUESTED` → `ROLLBACK_REJECTED`.
- Rollback review fields are recorded.
- No refund transaction is created.

**Responses:**

| Status | Description |
|---|---|
| `200 OK` | Returns updated `TransactionResponse`. |
| `400 Bad Request` | Transaction is not in `ROLLBACK_REQUESTED` status. |
| `404 Not Found` | Transaction not found. |

---

## Transaction Lifecycle

```
                     ┌──────────────────────────────────┐
                     │         POST /transactions         │
                     │  (createTransaction)               │
                     └────────────────┬─────────────────┘
                                      │
                              Rule evaluation
                             (ruleEngineService)
                                      │
                    ┌─────────────────┴─────────────────┐
                    │  No rule violation?               │  Rule violation?
                    ▼                                   ▼
               COMPLETED / PENDING / FAILED    PENDING_APPROVAL
                    │                                   │
                    │                     ┌─────────────┴──────────────┐
                    │                     │  approve        │  reject   │
                    │                     ▼                 ▼           │
                    │                COMPLETED          REJECTED        │
                    │                    │                               │
                    │         ┌──────────┘                               │
                    │         │  rollback/request                        │
                    │         ▼                                          │
                    │   ROLLBACK_REQUESTED                               │
                    │         │                                          │
                    │  ┌──────┴──────┐                                  │
                    │  │ approve     │  reject                           │
                    │  ▼            ▼                                   │
                    │ REFUNDED  ROLLBACK_REJECTED                       │
                    │  + new refund transaction created                  │
                    └───────────────────────────────────────────────────┘
```

**Valid transitions summary:**

| From | Action | To |
|---|---|---|
| Any status | `POST /transactions` | `COMPLETED` / `PENDING` / `FAILED` / `PENDING_APPROVAL` |
| `PENDING_APPROVAL` | `approve` | `COMPLETED` |
| `PENDING_APPROVAL` | `reject` | `REJECTED` |
| `COMPLETED` | `rollback/request` | `ROLLBACK_REQUESTED` |
| `ROLLBACK_REQUESTED` | `rollback/approve` | `REFUNDED` |
| `ROLLBACK_REQUESTED` | `rollback/reject` | `ROLLBACK_REJECTED` |

---

## Validation Rules

### Request-level validation (Jakarta Bean Validation)

| Field | Rule |
|---|---|
| `accountId` | Not blank, max 50 chars |
| `payeeId` | Not blank, max 50 chars |
| `amount` | Not null, minimum `0.01` |
| `currency` | Not blank, must match regex `^[A-Z]{3}$` |
| `type` | Not null, must be a valid `TransactionType` |
| `status` | Not null, must be a valid `TransactionStatus` |
| `transactionTime` | Not null |
| `description` | Optional, max 255 chars |

### Business-level validation (in service)

| Rule | Error |
|---|---|
| `minAmount > maxAmount` in filter | `400` — "minAmount cannot be greater than maxAmount" |
| `fromTime` is after `toTime` | `400` — "fromTime cannot be after toTime" |
| Unsupported `sortBy` value | `400` — "Unsupported sortBy field: …" |
| `sortDir` is not `asc` or `desc` | `400` — "sortDir must be either 'asc' or 'desc'" |
| Approve when not `PENDING_APPROVAL` | `400` — "Only pending transactions can be approved" |
| Reject when not `PENDING_APPROVAL` | `400` — "Only pending transactions can be rejected" |
| Rollback request when not `COMPLETED` | `400` — "Rollback can only be requested for completed transactions" |
| Approve rollback when not `ROLLBACK_REQUESTED` | `400` — "Only rollback-requested transactions can be approved for refund" |
| Reject rollback when not `ROLLBACK_REQUESTED` | `400` — "Only rollback-requested transactions can be rejected" |
| Transaction not found | `404` — "Transaction not found for id: …" |

---

## Error Handling

All errors are handled by the global `GlobalExceptionHandler` (`com.hsbc.tms.common.exception.GlobalExceptionHandler`).

**Standard error response shape:**
```json
{
  "timestamp": "2026-08-05T09:15:30.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": [
    "accountId: must not be blank",
    "currency: currency must be a 3-letter ISO code"
  ]
}
```

| Exception | HTTP Status |
|---|---|
| `ResourceNotFoundException` | `404 Not Found` |
| `BadRequestException` | `400 Bad Request` |
| `MethodArgumentNotValidException` | `400 Bad Request` (with field-level `details`) |
| `ConstraintViolationException` | `400 Bad Request` |
| Any unexpected `Exception` | `500 Internal Server Error` |

---

## Integration with Rule Engine

When `createTransaction` is called, after saving the transaction, `ruleEngineService.evaluate(transaction)` is called synchronously.

The rule engine checks all active monitoring rules against the transaction (amount threshold, velocity, new payee, daily limit). If any rule is violated, `evaluate()` returns `true`.

**What the service does on violation:**
```
if (violated && saved.getStatus() == COMPLETED) {
    saved.setStatus(PENDING_APPROVAL);
    saved.setReviewNote("Rule violation detected. Operator approval required.");
    saved = transactionRepository.update(saved);
}
```

This means:
- Only `COMPLETED` transactions are held for review. `PENDING` or `FAILED` transactions that trigger rules are **not** auto-escalated.
- The alert is generated by the rule engine service, not by the transaction service.
- When the operator later approves or rejects the transaction, `alertService.resolveAlertsForTransactionDecision(...)` is called to close any related open alerts.

---

*Last updated: August 5, 2026*

