# Transaction Monitoring System - Change Log

_Last updated: 2026-08-05_

This document tracks the major and minor changes completed in the `107_DevDas_Transaction_Monitoring_System` workspace.  
The changes are grouped branch-wise / workstream-wise so the team can track implementation history by module.

---

## 1. Transaction Management Workstream

### Major changes
- Preserved the existing transaction API flow and extended it without breaking the current endpoints.
- Implemented full transaction lifecycle support through `TransactionController` and `TransactionServiceImpl`.
- Added support for operator-driven review flows for suspicious transactions.
- Added rollback request / rollback approval / rollback rejection workflow.
- Added refund creation flow for approved rollbacks.

### Functional changes completed

#### API coverage
- `POST /api/v1/transactions` - create transaction.
- `GET /api/v1/transactions/{id}` - fetch transaction by id.
- `GET /api/v1/transactions` - filter and paginate transactions.
- `PATCH /api/v1/transactions/{id}/approve` - approve pending transaction.
- `PATCH /api/v1/transactions/{id}/reject` - reject pending transaction.
- `PATCH /api/v1/transactions/{id}/rollback/request` - request rollback for completed transaction.
- `PATCH /api/v1/transactions/{id}/rollback/approve` - approve rollback and create refund transaction.
- `PATCH /api/v1/transactions/{id}/rollback/reject` - reject rollback request.

#### Service-layer workflow changes
- Transaction creation now:
    - creates a new transaction record,
    - invokes rule evaluation,
    - automatically moves a `COMPLETED` transaction to `PENDING_APPROVAL` when a rule violation is triggered,
    - stores a review note indicating operator approval is required.
- Approve / reject flow now records:
    - `reviewedBy`,
    - `reviewedAt`,
    - `reviewNote`.
- Rollback request flow now records:
    - reason code,
    - reason detail,
    - requested by,
    - supporting reference,
    - requested timestamp.
- Rollback approval now:
    - marks the original transaction as `REFUNDED`,
    - creates a linked refund transaction,
    - stores `refundTransactionId`,
    - stores `refundedForTransactionId` on the generated refund.
- Rollback rejection now records reviewer details and review note.

### JDBC / repository changes
- Enhanced `JdbcTransactionRepository` to support live database compatibility.
- Added dynamic transaction-column detection using `DatabaseMetaData`.
- Repository now safely handles environments where optional transaction columns are missing.
- `SELECT` queries now include only the columns that actually exist in the live `transactions` table.
- `UPDATE` queries now update optional fields only if those columns exist.
- Row mapping now safely returns `null` for optional fields not present in the database.
- Added duplicate-key protection when inserting transactions.
- Added page-size capping logic to avoid accidental oversized fetches.

### Bug fixes completed
- Fixed the issue where transactions stored in the existing MySQL `coworking.transactions` table were not reflecting in the UI.
- Root cause fixed: repository was previously expecting optional columns that were not present in the user's live table.
- Fixed compatibility with the user's current transaction table structure:
    - `id`
    - `account_id`
    - `payee_id`
    - `amount`
    - `currency`
    - `type`
    - `status`
    - `transaction_time`
    - `description`
    - `created_at`
    - `updated_at`

### Minor improvements
- Added validation for invalid amount and time filter ranges.
- Added validation for supported sort fields and sort directions.
- Preserved DTO response shape expected by the frontend.

---

## 2. Rules Management Workstream

### Major changes
- Migrated the Rules Management feature set into this project using JDBC-based persistence.
- Implemented full backend support for rule creation, update, enable/disable, deletion, history, and statistics.
- Integrated rule execution into the transaction creation flow.
- Added seeded default rules so the system is usable immediately after startup.

### Rules module implemented

#### Rule types supported
- `AMOUNT_THRESHOLD`
- `VELOCITY`
- `NEW_PAYEE`
- `DAILY_LIMIT`

#### Rule APIs implemented
- `GET /api/v1/rules`
- `POST /api/v1/rules`
- `PUT /api/v1/rules/{id}`
- `PATCH /api/v1/rules/{id}/status`
- `DELETE /api/v1/rules/{id}`
- `GET /api/v1/rules/{id}/history`
- `GET /api/v1/rules/stats`

### Rule engine changes
- Implemented `RuleEngineService` to evaluate active rules against each created transaction.
- Added dedicated evaluator pattern for rule execution.
- Implemented rule evaluators for:
    - amount threshold detection,
    - velocity detection,
    - new payee detection,
    - daily limit detection.
- Added rule execution history persistence for auditability.

### Rule validation and business protections
- Added type-specific request validation via `RuleValidationService`.
- Added duplicate rule-name protection.
- Added explicit `409 Conflict` handling for duplicate rule names.
- Soft delete behavior implemented by deactivating the rule and recording audit history.
- Rule activation / deactivation now records audit actions.

### Persistence and audit support
- Added `monitoring_rules` table support.
- Added `rule_audit_history` table support.
- Added `rule_execution_history` table support.
- Implemented JDBC repositories for:
    - rules,
    - rule audit history,
    - rule execution history,
    - transaction metrics needed by velocity / daily-limit rules.

### Default seeded rules
- Seeded the following rules in `DataSeeder` when no rules exist:
    - `High Amount Threshold`
    - `Velocity 5 in 10 min`
    - `New Payee`
    - `Daily Limit`

### Minor improvements
- Added rule statistics grouped by type and severity.
- Added rule history snapshots for change tracking.
- Added rule-response mapping layer for stable API responses.

---

## 3. Alert Management Workstream

### Major changes
- Implemented alert creation, lookup, filtering, history, and workflow state transitions.
- Integrated alert creation directly with rule-trigger events.
- Linked alerts to the triggering transaction set.
- Integrated alert resolution with transaction decision workflow.

### Alert APIs implemented
- `POST /api/v1/alerts`
- `GET /api/v1/alerts`
- `GET /api/v1/alerts/{id}`
- `GET /api/v1/alerts/{id}/history`
- `PATCH /api/v1/alerts/{id}/status`

### Alert workflow changes
- Alerts are created automatically when a rule is triggered.
- Alert history is stored for all status changes.
- Supported alert states include:
    - `OPEN`
    - `ACKNOWLEDGED`
    - `INVESTIGATING`
    - `CLOSED`
    - `DISMISSED`
- Added transition validation so only valid alert state changes are accepted.
- When a pending transaction is approved or rejected, related active alerts are automatically resolved:
    - approved transaction -> alert can be closed,
    - rejected transaction -> alert can be dismissed.

### Persistence changes
- Added `alerts` table support.
- Added `alert_transactions` table support for many-to-many alert/transaction linkage.
- Added `alert_history` table support.
- Implemented JDBC-based repositories and service layer for alert retrieval and audit history.

### Minor improvements
- Added filtering by status, severity, and active-only mode.
- Added transaction details inside alert responses for frontend drill-down screens.
- Added history details inside alert response payloads for modal / detail rendering.

---

## 4. Frontend / Backend Integration Workstream

### Major changes
- Reviewed the SPA in `src/main/resources/static/index.html` against backend API routes.
- Ensured the frontend route usage matches the backend controller mappings.
- Verified dashboard, transactions, rules, alerts, and simulator features are calling valid backend endpoints.

### API integration checks completed
- Frontend and backend route parity verified for:
    - transactions list and detail,
    - transaction approval / rejection,
    - rollback request / approval / rejection,
    - rules CRUD and stats,
    - alerts list / detail / status update,
    - dashboard summary,
    - simulator generation.

### CORS / connectivity changes
- Added `WebCorsConfig` to support frontend-backend communication when running on different ports or origins.
- Enabled API CORS support for:
    - `GET`
    - `POST`
    - `PUT`
    - `PATCH`
    - `DELETE`
    - `OPTIONS`
- Added configurable origin support using property:
    - `tms.cors.allowed-origins`

### Frontend simulation integration change
- Updated the dashboard's sample-data generation action to use the backend simulator response.
- Frontend toast message now uses backend `createdCount` when available.

### Health / runtime verification completed
- Verified live API responses for:
    - `/api/v1/dashboard/summary`
    - `/api/v1/transactions`
    - `/api/v1/rules`
    - `/api/v1/rules/stats`
    - `/api/v1/alerts`
    - `/api/v1/simulator/generate`
- Verified CORS preflight handling through an `OPTIONS` request.

### Minor improvements
- Kept the frontend API base configurable through settings.
- Preserved the existing HTML UI without requiring a frontend rewrite.

---

## 5. Simulation Workstream

### Major changes
- Implemented simulation endpoint support for website testing.
- Improved the simulation behavior so generated data is useful for rule, alert, and dashboard testing.

### Simulation API implemented
- `POST /api/v1/simulator/generate`

### Simulation behavior changes
- Added realistic random transaction generation.
- Added mixed transaction statuses to make dashboard charts more meaningful:
    - `COMPLETED`
    - `PENDING`
    - `FAILED`
- Added burst transaction generation for a single account / payee pair to exercise velocity and alert logic.
- Added higher-risk amount generation for some simulated transactions to increase the chance of rule hits.
- Added varied time distribution for generated transactions.

### Simulation response contract
- Added explicit response DTO for simulator output.
- Simulator now returns:
    - requested `count`,
    - `createdCount`,
    - `createdTransactionIds`.

### Minor improvements
- Added integration coverage for successful simulator calls.
- Added validation coverage for invalid simulation requests.

---

## 6. Schema / Database Workstream

### Major changes
- Preserved compatibility with the user's existing MySQL `transactions` table.
- Added new supporting tables required for rules and alerts without forcing in-memory persistence.
- Continued using JDBC / MySQL-oriented persistence.

### Tables added / maintained in `schema.sql`
- `transactions`
- `monitoring_rules`
- `rule_audit_history`
- `rule_execution_history`
- `alerts`
- `alert_transactions`
- `alert_history`

### Important compatibility note
- The runtime repository logic was adjusted so the app can work even if the live MySQL `transactions` table does not yet contain all optional review / rollback / refund columns.
- This reduced the risk of runtime SQL failures in environments using an earlier version of the table.

---

## 7. Configuration Workstream

### Major changes
- Removed H2 console dependency from the main runtime configuration.
- Kept production-style datasource configuration externalized through environment variables.
- Added configurable server port support.

### Configuration changes in `application.properties`
- `server.port=${SERVER_PORT:8080}`
- `spring.datasource.url=${TMS_DB_URL:jdbc:mysql://localhost:3306/coworking}`
- `spring.datasource.username=${TMS_DB_USERNAME:root}`
- `spring.datasource.password=${TMS_DB_PASSWORD:n3u3da!}`
- `spring.datasource.driver-class-name=${TMS_DB_DRIVER:com.mysql.cj.jdbc.Driver}`
- `tms.cors.allowed-origins=${TMS_CORS_ALLOWED_ORIGINS:*}`
- Spring SQL init kept enabled for schema bootstrap.
- Swagger / OpenAPI endpoints kept enabled.

### Minor improvements
- Made it easier to run the project on alternate ports when 8080 is already in use.
- Kept runtime properties environment-driven for easier local / QA / demo setup.

---

## 8. Exception Handling / API Error Behavior

### Major changes
- Added `ConflictException` for duplicate or conflicting operations.
- Updated `GlobalExceptionHandler` to support:
    - `404 Not Found`
    - `400 Bad Request`
    - `409 Conflict`
    - validation errors
    - fallback `500 Internal Server Error`

### Minor improvements
- Validation responses now return structured error details.
- API error responses remain consistent across transactions, rules, alerts, and simulator workflows.

---

## 9. Testing Workstream

### Test coverage added / verified
- `TransactionControllerIntegrationTest`
    - create transaction,
    - fetch transaction by id,
    - validate invalid payload,
    - filter transaction list.
- `RuleControllerIntegrationTest`
    - create rule,
    - list / stats validation,
    - status / lifecycle coverage.
- `SimulationControllerIntegrationTest`
    - valid simulator request,
    - invalid simulator request.
- `TmsApplicationTests`
    - application context boot validation.

### Runtime verification completed
- Verified simulator call increases transaction count in the live application.
- Verified key API endpoints return `200` at runtime.
- Verified generated simulator payload shape from a running application.
- Verified CORS behavior from a different origin.

---

## 10. Summary of Major Outcomes

### Delivered
- Rules management migrated and integrated.
- Alert management implemented and linked to rule evaluation.
- Transaction workflows extended with review and rollback handling.
- Existing transaction data compatibility issue fixed.
- Frontend/backend integration stabilized.
- Simulation added and improved for website testing.
- Change tracking, audit history, and statistics implemented across key modules.

### Notes for future tracking
- If needed, this file can later be split into release entries such as:
    - `v1 - transaction baseline`
    - `v2 - rules management`
    - `v3 - alerts + integration`
    - `v4 - simulation + compatibility fixes`


