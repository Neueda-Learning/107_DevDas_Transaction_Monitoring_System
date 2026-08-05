# Transaction Monitoring System

Spring Boot backend for transaction monitoring, rule evaluation, alert handling, dashboard aggregation, and simulation-driven testing.

---

## Checklist

- Overview and local run instructions
- Swagger / OpenAPI access
- Shared API conventions
- Detailed endpoint documentation for:
    - Transactions
    - Rules
    - Alerts
    - Dashboard
    - Simulator
- Request / response model reference
- Error response format

---

## 1. Overview

This application provides:

- transaction creation and search APIs,
- transaction review and rollback workflow,
- monitoring rule management,
- automatic rule evaluation during transaction creation,
- alert creation and alert state transitions,
- dashboard summary endpoints,
- sample transaction generation for website / UI testing.

### Main API base

- Base path: `/api/v1`
- OpenAPI JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui.html`
- Static UI: `/`

### OpenAPI metadata

- Title: `Transaction Monitoring API`
- Version: `v1`

---

## 2. Local Run Instructions

### Runtime properties

The application reads runtime configuration from `src/main/resources/application.properties`.

Default runtime values:

- `SERVER_PORT=8080`
- `TMS_DB_URL=jdbc:mysql://localhost:3306/coworking`
- `TMS_DB_USERNAME=root`
- `TMS_DB_PASSWORD=n3u3da!`
- `TMS_DB_DRIVER=com.mysql.cj.jdbc.Driver`
- `TMS_CORS_ALLOWED_ORIGINS=*`

### Start the application

```powershell
cd "C:\Users\Administrator\Documents\107_DevDas_Transaction_Monitoring_System"
.\mvnw.cmd spring-boot:run
```

### Start on a different port

```powershell
cd "C:\Users\Administrator\Documents\107_DevDas_Transaction_Monitoring_System"
$env:SERVER_PORT="8091"
.\mvnw.cmd spring-boot:run
```

### Start with explicit CORS origins

```powershell
cd "C:\Users\Administrator\Documents\107_DevDas_Transaction_Monitoring_System"
$env:TMS_CORS_ALLOWED_ORIGINS="http://localhost:3000,http://localhost:5173"
.\mvnw.cmd spring-boot:run
```

### Run tests

```powershell
cd "C:\Users\Administrator\Documents\107_DevDas_Transaction_Monitoring_System"
.\mvnw.cmd test
```

### Test profile

The automated tests use `src/test/resources/application-test.properties` with in-memory H2 configured in MySQL compatibility mode.

---

## 3. Shared API Conventions

### Content type

- Request: `application/json`
- Response: `application/json`

### Pagination

Transaction list API returns a paged response with:

- `content`
- `page`
- `size`
- `totalElements`
- `totalPages`
- `first`
- `last`

### Common enums

#### Transaction types

- `DEBIT`
- `CREDIT`

#### Transaction statuses

- `COMPLETED`
- `PENDING`
- `FAILED`
- `PENDING_APPROVAL`
- `REJECTED`
- `ROLLBACK_REQUESTED`
- `ROLLBACK_REJECTED`
- `REFUNDED`

#### Rule types

- `AMOUNT_THRESHOLD`
- `VELOCITY`
- `NEW_PAYEE`
- `DAILY_LIMIT`

#### Alert severity

- `HIGH`
- `MEDIUM`
- `LOW`

#### Alert statuses

- `OPEN`
- `ACKNOWLEDGED`
- `INVESTIGATING`
- `CLOSED`
- `DISMISSED`

---

## 4. Transactions API

Base path: `/api/v1/transactions`

### 4.1 Create transaction

- **Method:** `POST`
- **Path:** `/api/v1/transactions`
- **Purpose:** Creates a transaction and automatically triggers rule evaluation.

#### Request body

| Field | Type | Required | Notes |
|---|---|---:|---|
| `accountId` | string | yes | max 50 chars |
| `payeeId` | string | yes | max 50 chars |
| `amount` | decimal | yes | must be `>= 0.01` |
| `currency` | string | yes | must match `^[A-Z]{3}$` |
| `type` | enum | yes | `DEBIT` or `CREDIT` |
| `status` | enum | yes | initial status |
| `transactionTime` | ISO timestamp | yes | example: `2026-08-05T08:00:00Z` |
| `description` | string | no | max 255 chars |

#### Example request

```json
{
  "accountId": "ACC-1001",
  "payeeId": "PAY-200",
  "amount": 1250.00,
  "currency": "USD",
  "type": "DEBIT",
  "status": "COMPLETED",
  "transactionTime": "2026-08-05T08:00:00Z",
  "description": "Grocery payment"
}
```

#### Notes

- If an active rule is triggered, the service may move the transaction from `COMPLETED` to `PENDING_APPROVAL`.
- Related alerts may be created automatically by the rule engine.

---

### 4.2 Get transaction by ID

- **Method:** `GET`
- **Path:** `/api/v1/transactions/{id}`

#### Path parameters

| Name | Type | Required | Notes |
|---|---|---:|---|
| `id` | UUID | yes | transaction id |

#### Response fields

| Field | Type |
|---|---|
| `id` | UUID |
| `accountId` | string |
| `payeeId` | string |
| `amount` | decimal |
| `currency` | string |
| `type` | enum |
| `status` | enum |
| `transactionTime` | timestamp |
| `description` | string/null |
| `createdAt` | timestamp |
| `updatedAt` | timestamp |
| `reviewedBy` | string/null |
| `reviewedAt` | timestamp/null |
| `reviewNote` | string/null |
| `rollbackReasonCode` | string/null |
| `rollbackReasonDetail` | string/null |
| `rollbackRequestedBy` | string/null |
| `rollbackRequestedAt` | timestamp/null |
| `rollbackSupportingReference` | string/null |
| `rollbackReviewedBy` | string/null |
| `rollbackReviewedAt` | timestamp/null |
| `rollbackReviewNote` | string/null |
| `refundedAt` | timestamp/null |
| `refundTransactionId` | UUID/null |
| `refundedForTransactionId` | UUID/null |

---

### 4.3 Search transactions

- **Method:** `GET`
- **Path:** `/api/v1/transactions`

#### Query parameters

| Name | Type | Required | Notes |
|---|---|---:|---|
| `accountId` | string | no | exact match |
| `payeeId` | string | no | exact match |
| `status` | enum | no | transaction status |
| `type` | enum | no | `DEBIT` or `CREDIT` |
| `minAmount` | decimal | no | inclusive |
| `maxAmount` | decimal | no | inclusive |
| `fromTime` | timestamp | no | inclusive |
| `toTime` | timestamp | no | inclusive |
| `page` | integer | no | default `0` |
| `size` | integer | no | default `20`, controller max `100` |
| `sortBy` | string | no | `amount`, `transactionTime`, `createdAt`, `updatedAt`, `accountId`, `status` |
| `sortDir` | string | no | `asc` or `desc` |

#### Example request

```text
GET /api/v1/transactions?accountId=ACC-1001&status=COMPLETED&page=0&size=20&sortBy=transactionTime&sortDir=desc
```

#### Example response shape

```json
{
  "content": [
	{
	  "id": "11111111-1111-4111-8111-000000000001",
	  "accountId": "ACC-1001",
	  "payeeId": "PAY-200",
	  "amount": 1250.00,
	  "currency": "USD",
	  "type": "DEBIT",
	  "status": "COMPLETED",
	  "transactionTime": "2026-08-05T08:00:00Z"
	}
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

### 4.4 Approve pending transaction

- **Method:** `PATCH`
- **Path:** `/api/v1/transactions/{id}/approve`
- **Purpose:** Approves a `PENDING_APPROVAL` transaction.

#### Request body

```json
{
  "operatorId": "analyst-01",
  "note": "Reviewed and approved"
}
```

#### Behavior

- Allowed only when transaction status is `PENDING_APPROVAL`.
- Marks the transaction as `COMPLETED`.
- Stores review metadata.
- Related active alerts may be closed by workflow.

---

### 4.5 Reject pending transaction

- **Method:** `PATCH`
- **Path:** `/api/v1/transactions/{id}/reject`

#### Request body

```json
{
  "operatorId": "analyst-01",
  "note": "Rejected due to suspicious activity"
}
```

#### Behavior

- Allowed only when transaction status is `PENDING_APPROVAL`.
- Marks the transaction as `REJECTED`.
- Related active alerts may be dismissed by workflow.

---

### 4.6 Request rollback

- **Method:** `PATCH`
- **Path:** `/api/v1/transactions/{id}/rollback/request`

#### Request body

```json
{
  "reasonCode": "CUSTOMER_REQUEST",
  "reasonDetail": "Customer reported incorrect payment",
  "requestedBy": "operator-01",
  "supportingReference": "CASE-2042"
}
```

#### Behavior

- Allowed only when transaction status is `COMPLETED`.
- Marks the transaction as `ROLLBACK_REQUESTED`.

---

### 4.7 Approve rollback

- **Method:** `PATCH`
- **Path:** `/api/v1/transactions/{id}/rollback/approve`

#### Request body

```json
{
  "operatorId": "supervisor-01",
  "note": "Rollback approved"
}
```

#### Behavior

- Allowed only when status is `ROLLBACK_REQUESTED`.
- Marks original transaction as `REFUNDED`.
- Creates a refund transaction.
- Links both records using refund fields.

---

### 4.8 Reject rollback

- **Method:** `PATCH`
- **Path:** `/api/v1/transactions/{id}/rollback/reject`

#### Request body

```json
{
  "operatorId": "supervisor-01",
  "note": "Insufficient evidence for rollback"
}
```

#### Behavior

- Allowed only when status is `ROLLBACK_REQUESTED`.
- Marks original transaction as `ROLLBACK_REJECTED`.

---

## 5. Rules API

Base path: `/api/v1/rules`

### 5.1 List rules

- **Method:** `GET`
- **Path:** `/api/v1/rules`

#### Query parameters

| Name | Type | Required | Notes |
|---|---|---:|---|
| `active` | boolean | no | filter by active state |
| `type` | enum | no | rule type |
| `severity` | enum | no | alert severity |

#### Response item

```json
{
  "id": 1,
  "name": "High Amount Threshold",
  "type": "AMOUNT_THRESHOLD",
  "severity": "HIGH",
  "active": true,
  "amountThreshold": 10000.00,
  "transactionCountThreshold": null,
  "timeWindowMinutes": null
}
```

---

### 5.2 Create rule

- **Method:** `POST`
- **Path:** `/api/v1/rules`

#### Request body

| Field | Type | Required | Notes |
|---|---|---:|---|
| `name` | string | yes | unique rule name |
| `type` | enum | yes | rule type |
| `severity` | enum | yes | alert severity |
| `active` | boolean | no | defaults to `true` when omitted |
| `amountThreshold` | decimal | conditional | required for `AMOUNT_THRESHOLD` and `DAILY_LIMIT` |
| `transactionCountThreshold` | integer | conditional | required for `VELOCITY` |
| `timeWindowMinutes` | integer | conditional | required for `VELOCITY` |

#### Example: amount threshold rule

```json
{
  "name": "High Value Transaction Rule",
  "type": "AMOUNT_THRESHOLD",
  "severity": "HIGH",
  "active": true,
  "amountThreshold": 10000.00
}
```

#### Example: velocity rule

```json
{
  "name": "Velocity 5 in 10 min",
  "type": "VELOCITY",
  "severity": "MEDIUM",
  "active": true,
  "transactionCountThreshold": 5,
  "timeWindowMinutes": 10
}
```

---

### 5.3 Update rule

- **Method:** `PUT`
- **Path:** `/api/v1/rules/{id}`

#### Notes

- Full update.
- `active` is required.
- Type-specific threshold rules still apply.

---

### 5.4 Activate or deactivate rule

- **Method:** `PATCH`
- **Path:** `/api/v1/rules/{id}/status`

#### Request body

```json
{
  "active": false
}
```

---

### 5.5 Soft delete rule

- **Method:** `DELETE`
- **Path:** `/api/v1/rules/{id}`

#### Behavior

- The rule is soft-deleted by being deactivated and audit-trailed.

---

### 5.6 Get rule audit history

- **Method:** `GET`
- **Path:** `/api/v1/rules/{id}/history`

#### Response item

| Field | Type |
|---|---|
| `id` | long |
| `ruleId` | long |
| `action` | enum |
| `previousValues` | string |
| `newValues` | string |
| `changedAt` | timestamp |
| `changedBy` | string |

---

### 5.7 Get rule statistics

- **Method:** `GET`
- **Path:** `/api/v1/rules/stats`

#### Example response

```json
{
  "totalRules": 4,
  "activeRules": 4,
  "inactiveRules": 0,
  "rulesByType": {
	"AMOUNT_THRESHOLD": 1,
	"VELOCITY": 1,
	"NEW_PAYEE": 1,
	"DAILY_LIMIT": 1
  },
  "rulesBySeverity": {
	"HIGH": 2,
	"MEDIUM": 2
  }
}
```

---

## 6. Alerts API

Base path: `/api/v1/alerts`

### 6.1 Create alert

- **Method:** `POST`
- **Path:** `/api/v1/alerts`

#### Request body

| Field | Type | Required | Notes |
|---|---|---:|---|
| `ruleName` | string | yes | source or associated rule name |
| `ruleType` | enum | yes | rule type |
| `severity` | enum | yes | alert severity |
| `message` | string | yes | max 1000 chars |
| `operatorId` | string | yes | creator / actor |
| `note` | string | no | initial history note |
| `triggeringTransactionIds` | UUID array | no | linked transaction ids |

#### Example request

```json
{
  "ruleName": "High Value Transaction Rule",
  "ruleType": "AMOUNT_THRESHOLD",
  "severity": "HIGH",
  "message": "Transaction amount exceeded threshold",
  "operatorId": "analyst-01",
  "note": "Created manually from investigation queue",
  "triggeringTransactionIds": [
	"11111111-1111-4111-8111-000000000001"
  ]
}
```

---

### 6.2 List alerts

- **Method:** `GET`
- **Path:** `/api/v1/alerts`

#### Query parameters

| Name | Type | Required | Notes |
|---|---|---:|---|
| `status` | enum | no | alert status |
| `severity` | enum | no | alert severity |
| `activeOnly` | boolean | no | default `false` |

---

### 6.3 Get alert by ID

- **Method:** `GET`
- **Path:** `/api/v1/alerts/{id}`

#### Response fields

| Field | Type |
|---|---|
| `id` | long |
| `ruleName` | string |
| `ruleType` | enum |
| `severity` | enum |
| `status` | enum |
| `message` | string |
| `createdAt` | timestamp |
| `updatedAt` | timestamp |
| `triggeringTransactions` | array of `TransactionResponse` |
| `history` | array of `AlertHistoryResponse` |

---

### 6.4 Get alert history

- **Method:** `GET`
- **Path:** `/api/v1/alerts/{id}/history`

#### Response item

```json
{
  "id": 1,
  "fromStatus": "OPEN",
  "toStatus": "ACKNOWLEDGED",
  "note": "Accepted for investigation",
  "changedBy": "analyst-01",
  "createdAt": "2026-08-05T09:00:00Z"
}
```

---

### 6.5 Update alert status

- **Method:** `PATCH`
- **Path:** `/api/v1/alerts/{id}/status`

#### Request body

```json
{
  "status": "INVESTIGATING",
  "operatorId": "analyst-01",
  "note": "Collecting additional evidence"
}
```

#### Transition rules

- `OPEN` -> `ACKNOWLEDGED`, `INVESTIGATING`, `CLOSED`, `DISMISSED`
- `ACKNOWLEDGED` -> `INVESTIGATING`, `CLOSED`, `DISMISSED`
- `INVESTIGATING` -> `CLOSED`, `DISMISSED`
- `CLOSED` -> no further transitions
- `DISMISSED` -> no further transitions

---

## 7. Dashboard API

Base path: `/api/v1/dashboard`

### 7.1 Summary counters

- **Method:** `GET`
- **Path:** `/api/v1/dashboard/summary`

#### Response fields

| Field | Type | Notes |
|---|---|---|
| `totalTransactions` | long | count of transactions |
| `totalAlerts` | long | count of alerts |
| `activeAlerts` | long | `OPEN`, `ACKNOWLEDGED`, `INVESTIGATING` |
| `openAlerts` | long | `OPEN` only |

#### Example response

```json
{
  "totalTransactions": 32,
  "totalAlerts": 8,
  "activeAlerts": 4,
  "openAlerts": 2
}
```

---

## 8. Simulator API

Base path: `/api/v1/simulator`

### 8.1 Generate sample transactions

- **Method:** `POST`
- **Path:** `/api/v1/simulator/generate`
- **Purpose:** Creates random sample transactions for UI, dashboard, rules, and alerts testing.

#### Request body

| Field | Type | Required | Notes |
|---|---|---:|---|
| `count` | integer | yes | min `1`, max `200` |

#### Example request

```json
{
  "count": 10
}
```

#### Example response

```json
{
  "count": 10,
  "createdCount": 10,
  "createdTransactionIds": [
	"951e63e3-671d-4a00-942b-4551cc0f5873",
	"8fefd484-c3c6-4dee-a8fb-c563c3f663be"
  ]
}
```

#### Simulation behavior

- Generates mixed statuses for better dashboard coverage.
- Generates burst traffic for realistic rule testing.
- Generates higher-value transactions to exercise threshold-based monitoring.

---

## 9. Error Response Format

All handled API errors follow the shared `ApiErrorResponse` shape.

### Response fields

| Field | Type |
|---|---|
| `timestamp` | timestamp |
| `status` | integer |
| `error` | string |
| `message` | string |
| `details` | string array |

### Example validation error

```json
{
  "timestamp": "2026-08-05T08:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": [
	"accountId: must not be blank",
	"currency: currency must be a 3-letter ISO code"
  ]
}
```

### Status codes used

- `200 OK`
- `201 Created`
- `400 Bad Request`
- `404 Not Found`
- `409 Conflict`
- `500 Internal Server Error`

---

## 10. Notes for Frontend Integration

- The static UI under `src/main/resources/static/index.html` is already aligned to `/api/v1`.
- If the frontend is hosted separately, set `TMS_CORS_ALLOWED_ORIGINS` to the frontend origin(s).
- The UI health check uses the API and expects the backend to be reachable.
- The simulator is wired into the dashboard so sample data can be generated directly from the website.

---

## 11. Seeded Rules Available on Fresh Startup

If the rule table is empty, the app seeds default rules:

- `High Amount Threshold`
- `Velocity 5 in 10 min`
- `New Payee`
- `Daily Limit`

These help the simulator and dashboard become useful immediately after first run.



