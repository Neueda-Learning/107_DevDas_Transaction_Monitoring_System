# Rules Management

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Package Structure](#package-structure)
4. [Domain Model](#domain-model)
   - [MonitoringRule](#monitoringrule)
   - [RuleAuditHistory](#ruleaudithistory)
   - [RuleExecutionHistory](#ruleexecutionhistory)
   - [Enums](#enums)
5. [Database Schema](#database-schema)
6. [DTOs](#dtos)
7. [Repository Layer](#repository-layer)
8. [Service Layer](#service-layer)
9. [Controller Layer](#controller-layer)
10. [API Reference](#api-reference)
11. [Rule Evaluation Flow](#rule-evaluation-flow)
12. [Validation Rules](#validation-rules)
13. [Error Handling](#error-handling)
14. [Feature Request Workflow](#feature-request-workflow)

---

## Overview

The Rules Management module defines and evaluates monitoring rules used to detect suspicious transactions.

It covers:

- CRUD and activation/deactivation of monitoring rules.
- Rule configuration validation by rule type.
- Runtime evaluation against incoming transactions.
- Audit trail (`rule_audit_history`) and execution trail (`rule_execution_history`).
- Operator feature requests for future rule enhancements.

---

## Architecture

```
Rule Administration APIs
         |
         v
+-------------------+
| RuleController    |
+---------+---------+
          |
          v
+-------------------+          +------------------------+
| RuleService       | -------> | RuleValidationService  |
+---------+---------+          +------------------------+
          |
          v
+-------------------------------+
| MonitoringRuleRepository      |
| RuleAuditHistoryRepository    |
+---------------+---------------+
                |
                v
              MySQL
```

Runtime path during transaction creation:

```
TransactionService -> RuleEngineService
                      |- load active rules
                      |- dispatch evaluator by RuleType
                      |- persist rule_execution_history
                      `- create alert on trigger
```

---

## Package Structure

```
com.hsbc.tms.rules
|- controller
|  |- RuleController.java
|  `- FeatureRequestController.java
|- dto
|  |- RuleCreateRequest.java
|  |- RuleUpdateRequest.java
|  |- RuleStatusUpdateRequest.java
|  |- RuleResponse.java
|  |- RuleStatsResponse.java
|  |- RuleAuditHistoryResponse.java
|  |- FeatureRequestCreateRequest.java
|  |- FeatureRequestStatusUpdateRequest.java
|  `- FeatureRequestResponse.java
|- entity
|  |- MonitoringRule.java
|  |- RuleAuditHistory.java
|  |- RuleExecutionHistory.java
|  `- RuleFeatureRequest.java
|- model
|  |- RuleType.java
|  |- AlertSeverity.java
|  |- RuleAuditAction.java
|  |- RuleExecutionOutcome.java
|  `- FeatureRequestStatus.java
|- repository
|  |- MonitoringRuleRepository.java
|  |- JdbcMonitoringRuleRepository.java
|  |- RuleAuditHistoryRepository.java
|  |- JdbcRuleAuditHistoryRepository.java
|  |- RuleExecutionHistoryRepository.java
|  |- JdbcRuleExecutionHistoryRepository.java
|  |- RuleTransactionMetricsRepository.java
|  |- JdbcRuleTransactionMetricsRepository.java
|  |- RuleFeatureRequestRepository.java
|  `- JdbcRuleFeatureRequestRepository.java
`- service
   |- RuleService.java
   |- RuleValidationService.java
   |- RuleEngineService.java
   |- FeatureRequestService.java
   |- RuleApiMapper.java
   `- ruleengine/*Evaluator.java
```

---

## Domain Model

### MonitoringRule

**File:** `src/main/java/com/hsbc/tms/rules/entity/MonitoringRule.java`

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | Auto-increment key |
| `name` | `String` | Unique rule name |
| `type` | `RuleType` | Rule category |
| `severity` | `AlertSeverity` | Alert severity when triggered |
| `active` | `boolean` | Enable/disable flag |
| `amountThreshold` | `BigDecimal` | Used by `AMOUNT_THRESHOLD` and `DAILY_LIMIT` |
| `transactionCountThreshold` | `Integer` | Used by `VELOCITY` |
| `timeWindowMinutes` | `Integer` | Used by `VELOCITY` |
| `createdAt` | `Instant` | Creation time |

### RuleAuditHistory

Tracks administrative changes to rule definitions and status.

| Field | Type |
|---|---|
| `id` | `Long` |
| `ruleId` | `Long` |
| `action` | `RuleAuditAction` |
| `previousValues` | `String` |
| `newValues` | `String` |
| `changedAt` | `Instant` |
| `changedBy` | `String` |

### RuleExecutionHistory

Tracks runtime rule checks per transaction.

| Field | Type |
|---|---|
| `id` | `Long` |
| `executionId` | `UUID` |
| `ruleId` | `Long` |
| `transactionId` | `UUID` |
| `outcome` | `RuleExecutionOutcome` |
| `message` | `String` |
| `createdAt` | `Instant` |

### Enums

`RuleType`:

```
AMOUNT_THRESHOLD
VELOCITY
NEW_PAYEE
DAILY_LIMIT
```

`AlertSeverity`:

```
HIGH
MEDIUM
LOW
```

`RuleAuditAction`:

```
CREATED
UPDATED
ACTIVATED
DEACTIVATED
DELETED
```

`RuleExecutionOutcome`:

```
TRIGGERED
NOT_TRIGGERED
```

---

## Database Schema

Defined in `src/main/resources/schema.sql`.

### `monitoring_rules`

```sql
CREATE TABLE IF NOT EXISTS monitoring_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL,
    amount_threshold DECIMAL(19, 2),
    transaction_count_threshold INT,
    time_window_minutes INT,
    created_at TIMESTAMP(6) NOT NULL
);
```

### `rule_audit_history`

Stores each create/update/activate/deactivate/delete snapshot.

### `rule_execution_history`

Stores each runtime evaluation result, including not-triggered outcomes.

### `rule_feature_requests`

Stores operator-submitted enhancement requests for new rules.

---

## DTOs

### Rule Administration

- `RuleCreateRequest`: create rule configuration.
- `RuleUpdateRequest`: full update, including active flag.
- `RuleStatusUpdateRequest`: activate/deactivate only.
- `RuleResponse`: returned rule payload.
- `RuleAuditHistoryResponse`: audit timeline row.
- `RuleStatsResponse`: aggregate counts by status/type/severity.

### Feature Requests

- `FeatureRequestCreateRequest`: title, description, requestedBy.
- `FeatureRequestStatusUpdateRequest`: status + optional adminNote.
- `FeatureRequestResponse`: full feature request state.

---

## Repository Layer

### `MonitoringRuleRepository`

Provides:

- active rule retrieval for runtime evaluation,
- duplicate-name checks,
- CRUD/search,
- statistics and grouped counts.

### `JdbcMonitoringRuleRepository`

Highlights:

- Dynamic `search(active, type, severity)` query.
- `save(...)` inserts on null `id`, updates otherwise.
- Grouped counts are returned as pre-seeded enum maps with zero defaults.

### `RuleAuditHistoryRepository` and `RuleExecutionHistoryRepository`

- Persist and read audit / runtime history rows.
- Both JDBC implementations map timestamps to/from `Instant`.

### `RuleTransactionMetricsRepository`

Supports evaluator queries against `transactions`:

- velocity count in time window,
- historical account-payee count,
- daily sum by account,
- optional status-scoped lookups.

---

## Service Layer

### `RuleService`

**File:** `src/main/java/com/hsbc/tms/rules/service/RuleService.java`

Responsibilities:

- list/search rules,
- fetch rule stats,
- fetch rule audit history,
- create/update/status/deactivate rule,
- write audit records for each management action.

Notable behavior:

- Name uniqueness is enforced case-insensitively.
- `create` defaults `active` to `true` when omitted.
- `softDeleteRule` sets `active = false` and records `DELETED` action.

### `RuleValidationService`

Validates:

- common required fields (`name`, `type`, `severity`),
- type-specific thresholds:
  - `AMOUNT_THRESHOLD`, `DAILY_LIMIT`: positive `amountThreshold` required.
  - `VELOCITY`: positive `transactionCountThreshold` and `timeWindowMinutes` required.
  - `NEW_PAYEE`: no additional thresholds.

### `RuleEngineService`

Runtime evaluator orchestration:

1. Load active rules.
2. Dispatch to evaluator by `RuleType`.
3. Save execution history (`TRIGGERED` / `NOT_TRIGGERED`).
4. Call `AlertService.createAlertForRuleTrigger(...)` on triggers.
5. Return `true` if any rule triggered.

---

## Controller Layer

### `RuleController`

Base path: `/api/v1/rules`

Endpoints:

- `GET /api/v1/rules`
- `POST /api/v1/rules`
- `PUT /api/v1/rules/{id}`
- `PATCH /api/v1/rules/{id}/status`
- `DELETE /api/v1/rules/{id}`
- `GET /api/v1/rules/{id}/history`
- `GET /api/v1/rules/stats`

### `FeatureRequestController`

Base path: `/api/v1/rule-requests`

Endpoints:

- `GET /api/v1/rule-requests`
- `GET /api/v1/rule-requests/{id}`
- `POST /api/v1/rule-requests`
- `PATCH /api/v1/rule-requests/{id}/status`
- `PATCH /api/v1/rule-requests/{id}/withdraw`

---

## API Reference

### Rule Management APIs

- `GET /api/v1/rules`: optional filters `active`, `type`, `severity`.
- `POST /api/v1/rules`: creates and audits new rule (`CREATED`).
- `PUT /api/v1/rules/{id}`: full update and audit (`UPDATED`).
- `PATCH /api/v1/rules/{id}/status`: toggles active state and audits (`ACTIVATED` or `DEACTIVATED`).
- `DELETE /api/v1/rules/{id}`: soft delete (sets inactive) and audits (`DELETED`).
- `GET /api/v1/rules/{id}/history`: full audit timeline.
- `GET /api/v1/rules/stats`: total, active, inactive, grouped counts.

### Feature Request APIs

- `POST /api/v1/rule-requests`: submit operator request (`REQUESTED`).
- `PATCH /api/v1/rule-requests/{id}/status`: move request through status flow.
- `PATCH /api/v1/rule-requests/{id}/withdraw`: operator withdrawal.

---

## Rule Evaluation Flow

```
Incoming transaction
       |
       v
RuleEngineService.evaluate(transaction)
       |
       +--> load active rules
       |
       +--> for each rule: evaluator.evaluate(...)
       |      |
       |      +--> save rule_execution_history row
       |      `--> if triggered: create alert
       |
       `--> return violated=true if any rule triggered
```

Current evaluator behavior:

- `AMOUNT_THRESHOLD`: trigger when transaction amount exceeds configured threshold.
- `VELOCITY`: trigger when transaction count in window exceeds configured count.
- `NEW_PAYEE`: trigger for first account-to-payee transaction.
- `DAILY_LIMIT`: trigger when completed transaction daily sum exceeds configured limit.

---

## Validation Rules

- Request DTO annotations enforce basic schema constraints.
- Service-level validation enforces business semantics per `RuleType`.
- Invalid status updates (already active/inactive) raise `BadRequestException`.
- Duplicate rule names raise `ConflictException`.
- Unknown rule IDs raise `ResourceNotFoundException`.

---

## Error Handling

Typical statuses:

- `400 Bad Request`: invalid config, invalid status transition, invalid feature request status.
- `404 Not Found`: unknown rule or feature request ID.
- `409 Conflict`: duplicate rule name.

All exceptions are handled by global handlers in `com.hsbc.tms.common.exception`.

---

## Feature Request Workflow

`FeatureRequestStatus` values:

```
REQUESTED -> BANK_APPROVAL -> IMPLEMENTED
        \-> WITHDRAWN
```

Constraints from service logic:

- `IMPLEMENTED` and `WITHDRAWN` are terminal for admin status changes.
- Withdraw is blocked once implemented.

---

*Last updated: August 6, 2026*

