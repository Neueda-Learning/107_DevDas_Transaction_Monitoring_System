# User Stories

## Scope Basis
These stories are derived from `C:\Users\Administrator\Downloads\transaction_monitoring.md` and prioritize core API and alert lifecycle requirements first, then dashboard and advanced enhancements.

## Standard Story Format
Use this format for all new stories:

```markdown
### US-XXX: <Short title>
- **Role:** As a <role>
- **Goal:** I want <capability>
- **Benefit:** So that <business value>
- **Priority:** P0 | P1 | P2
- **Dependencies:** <story IDs or N/A>
- **Acceptance Criteria:**
  - Given ... When ... Then ...
  - Given ... When ... Then ...
- **Notes:** <optional>
```

## Initial Backlog

### US-001: Record a transaction via API
- **Role:** As an operator
- **Goal:** I want to submit transactions into the system
- **Benefit:** So that transactions can be monitored by rules
- **Priority:** P0
- **Dependencies:** N/A
- **Acceptance Criteria:**
  - Given a valid transaction payload, when I call the create transaction endpoint, then the transaction is persisted with a unique ID and timestamp.
  - Given a transaction is recorded, when rule evaluation is triggered, then the transaction is available for rule checks.

### US-002: View and search transactions
- **Role:** As an operator
- **Goal:** I want to list transactions with filtering/search
- **Benefit:** So that I can investigate suspicious activity quickly
- **Priority:** P1
- **Dependencies:** US-001
- **Acceptance Criteria:**
  - Given stored transactions, when I query with date/account/amount filters, then only matching records are returned.
  - Given a transaction ID search term, when I run search, then matching transactions are returned.

### US-003: Trigger alerts for amount threshold rule
- **Role:** As an operator
- **Goal:** I want an alert when a single transaction exceeds a configured threshold
- **Benefit:** So that large transactions are reviewed promptly
- **Priority:** P0
- **Dependencies:** US-001
- **Acceptance Criteria:**
  - Given an active amount-threshold rule, when a transaction amount exceeds threshold, then an alert is created with status `OPEN`.
  - Given a transaction below threshold, when evaluated, then no threshold alert is generated.

### US-004: Trigger alerts for velocity rule
- **Role:** As an operator
- **Goal:** I want an alert when too many transactions occur in a time window
- **Benefit:** So that burst activity can be flagged
- **Priority:** P0
- **Dependencies:** US-001
- **Acceptance Criteria:**
  - Given an active velocity rule (N in T), when the account exceeds N transactions within T, then an alert is created with status `OPEN`.
  - Given activity outside the configured time window, when evaluated, then no velocity alert is generated.

### US-005: Trigger alerts for new payee rule
- **Role:** As an operator
- **Goal:** I want an alert on first-time payees
- **Benefit:** So that potentially risky counterparties are reviewed
- **Priority:** P0
- **Dependencies:** US-001
- **Acceptance Criteria:**
  - Given no historical transaction to a payee for an account, when a transaction to that payee is submitted, then a new-payee alert is created.
  - Given the payee was used before for that account, when another transaction is submitted, then no new-payee alert is created.

### US-006: Generate alert records with required metadata
- **Role:** As an operator
- **Goal:** I want each alert to include traceable details
- **Benefit:** So that I can understand why the alert exists
- **Priority:** P0
- **Dependencies:** US-003, US-004, US-005
- **Acceptance Criteria:**
  - Given an alert is generated, when saved, then it includes rule type/name, related transaction references, created timestamp, and current status.
  - Given an alert list request, when retrieved, then alerts expose enough data for sorting and filtering.

### US-007: View active alerts
- **Role:** As an operator
- **Goal:** I want to list active alerts
- **Benefit:** So that I can prioritize investigations
- **Priority:** P1
- **Dependencies:** US-006
- **Acceptance Criteria:**
  - Given alerts exist, when I request active alerts, then statuses excluding `CLOSED` and `DISMISSED` are returned.
  - Given filters by status/severity/date, when applied, then the list reflects selected criteria.

### US-008: View alert details and triggering transactions
- **Role:** As an operator
- **Goal:** I want to inspect full details of a single alert
- **Benefit:** So that I can determine next lifecycle action
- **Priority:** P1
- **Dependencies:** US-006
- **Acceptance Criteria:**
  - Given an alert ID, when I request details, then I see status, rule context, timestamps, and related transactions.
  - Given status updates exist, when details are retrieved, then history entries are included in chronological order.

### US-009: Acknowledge alert (OPEN -> ACKNOWLEDGED)
- **Role:** As an operator
- **Goal:** I want to acknowledge new alerts
- **Benefit:** So that review ownership is explicit
- **Priority:** P0
- **Dependencies:** US-006
- **Acceptance Criteria:**
  - Given an alert in `OPEN`, when acknowledge is requested, then status changes to `ACKNOWLEDGED` and timestamp is stored.
  - Given an alert in any non-OPEN status, when acknowledge is requested, then the API rejects the invalid transition.

### US-010: Mark alert as investigating (ACKNOWLEDGED -> INVESTIGATING)
- **Role:** As an operator
- **Goal:** I want to mark active investigation state
- **Benefit:** So that workflow reflects current progress
- **Priority:** P0
- **Dependencies:** US-009
- **Acceptance Criteria:**
  - Given an alert in `ACKNOWLEDGED`, when investigating is requested, then status changes to `INVESTIGATING` with timestamp.
  - Given an alert in any other status, when investigating is requested, then transition rules are enforced.

### US-011: Close alert (INVESTIGATING -> CLOSED)
- **Role:** As an operator
- **Goal:** I want to close completed investigations
- **Benefit:** So that active queue stays current
- **Priority:** P0
- **Dependencies:** US-010
- **Acceptance Criteria:**
  - Given an alert in `INVESTIGATING`, when close is requested, then status changes to `CLOSED` and closure timestamp is stored.
  - Given closure notes are required by implementation policy, when missing, then request is rejected with a validation message.

### US-012: Dismiss alert as false positive
- **Role:** As an operator
- **Goal:** I want to dismiss non-actionable alerts
- **Benefit:** So that false positives do not consume investigation time
- **Priority:** P0
- **Dependencies:** US-009
- **Acceptance Criteria:**
  - Given an alert in `ACKNOWLEDGED` or `INVESTIGATING`, when dismiss is requested, then status changes to `DISMISSED` with timestamp.
  - Given an alert in disallowed states, when dismiss is requested, then transition is rejected.

### US-013: Preserve full alert audit trail
- **Role:** As an operator
- **Goal:** I want each status/action to be auditable
- **Benefit:** So that alert handling is traceable end-to-end
- **Priority:** P0
- **Dependencies:** US-009, US-010, US-011, US-012
- **Acceptance Criteria:**
  - Given any lifecycle action, when performed, then action type, old/new status, timestamp, and optional note are persisted.
  - Given alert history is requested, when returned, then all lifecycle events are visible in order.

### US-014: Manage monitoring rules
- **Role:** As an operator
- **Goal:** I want to create, update, activate, and deactivate rules
- **Benefit:** So that monitoring behavior can be adjusted without code changes
- **Priority:** P1
- **Dependencies:** US-003, US-004, US-005
- **Acceptance Criteria:**
  - Given rule management endpoints, when I create/update a rule, then validation for required parameters is enforced by rule type.
  - Given a rule is inactive, when transactions are evaluated, then that rule does not trigger alerts.

### US-015: API usage documentation
- **Role:** As a developer/operator
- **Goal:** I want API documentation for transactions, rules, and alerts
- **Benefit:** So that the system is easy to understand and demo
- **Priority:** P1
- **Dependencies:** US-001 to US-014
- **Acceptance Criteria:**
  - Given the project repository, when onboarding, then endpoint contracts and example payloads are documented.
  - Given OpenAPI/Swagger is enabled, when accessed, then endpoints and schemas are visible.

## Advanced Stories (If Time Allows)

### US-016: Daily cumulative limit rule
- **Role:** As an operator
- **Goal:** I want alerts when account daily total exceeds a limit
- **Benefit:** So that unusual cumulative outflows are detected
- **Priority:** P2
- **Dependencies:** US-001, US-006
- **Acceptance Criteria:**
  - Given daily total for an account exceeds the configured value, when evaluated, then an alert is generated.
  - Given daily total is below the configured value, when evaluated, then no daily-limit alert is generated.

### US-017: Alert deduplication/grouping
- **Role:** As an operator
- **Goal:** I want similar alerts grouped
- **Benefit:** So that alert fatigue is reduced
- **Priority:** P2
- **Dependencies:** US-006
- **Acceptance Criteria:**
  - Given repeated similar triggers in a short period, when generated, then alerts are grouped per configured policy.
  - Given grouped alerts, when viewing details, then linked underlying transactions are accessible.

### US-018: Real-time dashboard updates
- **Role:** As an operator
- **Goal:** I want alert and transaction views to update automatically
- **Benefit:** So that I can react quickly without manual refresh
- **Priority:** P2
- **Dependencies:** US-007, US-008
- **Acceptance Criteria:**
  - Given new alerts arrive, when the dashboard is open, then active alert views update in near real time.
  - Given connection interruptions, when recovered, then the client can resynchronize current state.

