# Alerts Management

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Package Structure](#package-structure)
4. [Domain Model](#domain-model)
   - [Alert Entity](#alert-entity)
   - [AlertHistory Entity](#alerthistory-entity)
   - [AlertStatus Enum](#alertstatus-enum)
5. [Database Schema](#database-schema)
6. [DTOs](#dtos)
   - [CreateAlertRequest](#createalertrequest)
   - [AlertStatusUpdateRequest](#alertstatusupdaterequest)
   - [AlertResponse](#alertresponse)
   - [AlertHistoryResponse](#alerthistoryresponse)
7. [Repository Layer](#repository-layer)
8. [Service Layer](#service-layer)
9. [Controller Layer](#controller-layer)
10. [API Reference](#api-reference)
11. [Alert Lifecycle](#alert-lifecycle)
12. [Validation Rules](#validation-rules)
13. [Error Handling](#error-handling)
14. [Integration Points](#integration-points)

---

## Overview

The Alerts Management module tracks suspicious activity raised by monitoring rules and provides an operator workflow to investigate and close alerts.

It is responsible for:

- Creating alerts manually or from rule-engine triggers.
- Linking one alert to one or more triggering transactions.
- Tracking status transitions (`OPEN`, `ACKNOWLEDGED`, `INVESTIGATING`, `CLOSED`, `DISMISSED`).
- Persisting a complete status-change history for auditability.

Data access is implemented with Spring `JdbcClient` and explicit SQL statements.

---

## Architecture

```
HTTP Request
     |
     v
+-------------------+
| AlertController   |  <- REST endpoints
+---------+---------+
          |
          v
+-------------------+
| AlertService      |  <- business rules and transitions
+----+----------+---+
     |          |
     v          v
AlertRepository AlertHistoryRepository
     |
     v
   MySQL
(alerts, alert_transactions, alert_history)
```

Internal integration path:

```
RuleEngineService -> AlertService.createAlertForRuleTrigger(...)
TransactionService -> AlertService.resolveAlertsForTransactionDecision(...)
```

---

## Package Structure

```
com.hsbc.tms.alerts
|- controller
|  `- AlertController.java
|- dto
|  |- CreateAlertRequest.java
|  |- AlertStatusUpdateRequest.java
|  |- AlertResponse.java
|  `- AlertHistoryResponse.java
|- entity
|  |- Alert.java
|  `- AlertHistory.java
|- model
|  `- AlertStatus.java
|- repository
|  |- AlertRepository.java
|  |- JdbcAlertRepository.java
|  |- AlertHistoryRepository.java
|  `- JdbcAlertHistoryRepository.java
`- service
   |- AlertService.java
   `- AlertApiMapper.java
```

---

## Domain Model

### Alert Entity

**File:** `src/main/java/com/hsbc/tms/alerts/entity/Alert.java`

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | Auto-increment primary key |
| `ruleName` | `String` | Name of the rule that raised alert |
| `ruleType` | `RuleType` | Rule category (`AMOUNT_THRESHOLD`, etc.) |
| `severity` | `AlertSeverity` | Priority level (`HIGH`, `MEDIUM`, `LOW`) |
| `status` | `AlertStatus` | Workflow state |
| `message` | `String` | Human-readable explanation |
| `createdAt` | `Instant` | Created timestamp |
| `updatedAt` | `Instant` | Last update timestamp |
| `triggeringTransactionIds` | `List<UUID>` | IDs linked via join table |

### AlertHistory Entity

**File:** `src/main/java/com/hsbc/tms/alerts/entity/AlertHistory.java`

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | Auto-increment primary key |
| `alertId` | `Long` | Parent alert ID |
| `fromStatus` | `AlertStatus` | Previous status (nullable on creation) |
| `toStatus` | `AlertStatus` | New status |
| `note` | `String` | Transition note |
| `changedBy` | `String` | Operator/system actor |
| `createdAt` | `Instant` | Transition timestamp |

### AlertStatus Enum

**File:** `src/main/java/com/hsbc/tms/alerts/model/AlertStatus.java`

```
OPEN
ACKNOWLEDGED
INVESTIGATING
CLOSED
DISMISSED
```

---

## Database Schema

Defined in `src/main/resources/schema.sql`.

### `alerts`

```sql
CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(150) NOT NULL,
    rule_type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    INDEX idx_alerts_status (status),
    INDEX idx_alerts_severity (severity),
    INDEX idx_alerts_created_at (created_at)
);
```

### `alert_transactions`

```sql
CREATE TABLE IF NOT EXISTS alert_transactions (
    alert_id BIGINT NOT NULL,
    transaction_id CHAR(36) NOT NULL,
    PRIMARY KEY (alert_id, transaction_id),
    INDEX idx_alert_transactions_transaction_id (transaction_id)
);
```

### `alert_history`

```sql
CREATE TABLE IF NOT EXISTS alert_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id BIGINT NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    note VARCHAR(1000) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_alert_history_alert_id (alert_id),
    INDEX idx_alert_history_created_at (created_at)
);
```

---

## DTOs

### CreateAlertRequest

Used by `POST /api/v1/alerts`.

| Field | Type | Required | Constraints |
|---|---|---|---|
| `ruleName` | `String` | Yes | `@NotBlank` |
| `ruleType` | `RuleType` | Yes | `@NotNull` |
| `severity` | `AlertSeverity` | Yes | `@NotNull` |
| `message` | `String` | Yes | `@NotBlank`, max 1000 |
| `operatorId` | `String` | Yes | `@NotBlank`, max 100 |
| `note` | `String` | No | max 1000 |
| `triggeringTransactionIds` | `List<UUID>` | No | must reference existing transactions |

### AlertStatusUpdateRequest

Used by `PATCH /api/v1/alerts/{id}/status`.

| Field | Type | Required |
|---|---|---|
| `status` | `AlertStatus` | Yes |
| `operatorId` | `String` | Yes |
| `note` | `String` | No |

### AlertResponse

Returned by all alert APIs.

| Field | Type |
|---|---|
| `id` | `Long` |
| `ruleName` | `String` |
| `ruleType` | `RuleType` |
| `severity` | `AlertSeverity` |
| `status` | `AlertStatus` |
| `message` | `String` |
| `createdAt` | `Instant` |
| `updatedAt` | `Instant` |
| `triggeringTransactions` | `List<TransactionResponse>` |
| `history` | `List<AlertHistoryResponse>` |

### AlertHistoryResponse

| Field | Type |
|---|---|
| `id` | `Long` |
| `fromStatus` | `AlertStatus` |
| `toStatus` | `AlertStatus` |
| `note` | `String` |
| `changedBy` | `String` |
| `createdAt` | `Instant` |

---

## Repository Layer

### `AlertRepository`

**File:** `src/main/java/com/hsbc/tms/alerts/repository/AlertRepository.java`

Main responsibilities:

- Search by optional `status`, `severity`, and `activeOnly` filter.
- Save new and existing alerts.
- Manage join rows in `alert_transactions`.
- Fetch active alerts by triggering transaction ID.

### `JdbcAlertRepository`

**File:** `src/main/java/com/hsbc/tms/alerts/repository/JdbcAlertRepository.java`

Key behavior:

- `search(...)` builds dynamic SQL and orders by `created_at DESC, id DESC`.
- `save(...)` inserts when `id == null`, updates otherwise.
- `replaceTriggeringTransactions(...)` deletes existing links and re-inserts unique IDs.
- `findActiveByTriggeringTransactionId(...)` limits to active statuses from service.

### `AlertHistoryRepository` / `JdbcAlertHistoryRepository`

- Writes all status-change records to `alert_history`.
- Reads history ordered by `created_at ASC, id ASC`.

---

## Service Layer

**File:** `src/main/java/com/hsbc/tms/alerts/service/AlertService.java`

### Core operations

- `getAlerts(...)`: list alerts with optional filters.
- `createAlert(...)`: create manual alert and seed first history row.
- `getAlert(id)`: fetch a single alert.
- `getHistory(id)`: fetch timeline of status transitions.
- `updateStatus(id, request)`: validate and apply workflow transition.

### Integration operations

- `createAlertForRuleTrigger(...)`: used by rule engine when a rule fires.
- `resolveAlertsForTransactionDecision(...)`: closes/dismisses active alerts for a transaction when operator decides transaction outcome.

### Valid transitions

`VALID_TRANSITIONS` in service allows:

- `OPEN -> ACKNOWLEDGED | INVESTIGATING | CLOSED | DISMISSED`
- `ACKNOWLEDGED -> INVESTIGATING | CLOSED | DISMISSED`
- `INVESTIGATING -> CLOSED | DISMISSED`
- `CLOSED -> (no transitions)`
- `DISMISSED -> (no transitions)`

Invalid transitions throw `BadRequestException`.

---

## Controller Layer

**File:** `src/main/java/com/hsbc/tms/alerts/controller/AlertController.java`

Base path: `/api/v1/alerts`

Exposed endpoints:

- `POST /api/v1/alerts` - create alert.
- `GET /api/v1/alerts` - list alerts.
- `GET /api/v1/alerts/{id}` - get one alert.
- `GET /api/v1/alerts/{id}/history` - get status timeline.
- `PATCH /api/v1/alerts/{id}/status` - update status.

---

## API Reference

### `POST /api/v1/alerts`

Creates an alert and an initial history record (`toStatus = OPEN`).

**Response:** `201 Created` with `AlertResponse`.

### `GET /api/v1/alerts`

Query params:

- `status` (optional)
- `severity` (optional)
- `activeOnly` (default `false`)

**Response:** `200 OK` with `List<AlertResponse>`.

### `GET /api/v1/alerts/{id}`

Returns full alert view including triggering transaction payloads and history.

### `GET /api/v1/alerts/{id}/history`

Returns ordered status timeline.

### `PATCH /api/v1/alerts/{id}/status`

Applies one workflow transition and writes audit history row.

---

## Alert Lifecycle

```
OPEN
 |- acknowledge    -> ACKNOWLEDGED
 |- investigate    -> INVESTIGATING
 |- close          -> CLOSED
 `- dismiss        -> DISMISSED

ACKNOWLEDGED
 |- investigate    -> INVESTIGATING
 |- close          -> CLOSED
 `- dismiss        -> DISMISSED

INVESTIGATING
 |- close          -> CLOSED
 `- dismiss        -> DISMISSED
```

Terminal states:

- `CLOSED`
- `DISMISSED`

---

## Validation Rules

### Request validation

- Bean Validation annotations enforce required fields and max lengths.
- Transition update requires non-null `status` and non-blank `operatorId`.

### Business validation

- Alert must exist for read/update operations.
- Requested new status must differ from current status.
- Requested transition must be in `VALID_TRANSITIONS`.
- Triggering transaction IDs must exist before link rows are saved.

---

## Error Handling

Errors are handled by global exception infrastructure in `com.hsbc.tms.common.exception`.

Common alert module errors:

- `ResourceNotFoundException` (`404`) when alert ID is unknown.
- `BadRequestException` (`400`) for:
  - invalid transition,
  - duplicate same-state update,
  - unknown triggering transaction IDs.

---

## Integration Points

- `RuleEngineService` creates alerts automatically when any active rule triggers.
- `TransactionService` calls `resolveAlertsForTransactionDecision(...)` so related active alerts are closed or dismissed when a flagged transaction is approved or rejected.
- `AlertApiMapper` maps alert entities and history plus linked `TransactionResponse` details for API output.

---

*Last updated: August 6, 2026*

