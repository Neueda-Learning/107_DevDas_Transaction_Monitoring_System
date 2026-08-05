# Customer Meeting 2 - Transaction Management Requirements Update

## Overview

Based on the feedback received during Customer Meeting 2, the Transaction Management module requires UI improvements, better transaction status visibility, enhanced rollback tracking, and additional verification details to support operator decision-making.

---

# Changes Requested

## 1. Move Generate Simulation Transaction

### Current Behavior
- Generate Simulation Transaction option is available on the main frontend page.

### Required Change
- Remove the simulation transaction generation option from the main dashboard.
- Create a separate tab/page specifically for transaction simulation.

### New UI Structure

Tabs:
- Dashboard
- Transaction History
- Pending Transactions
- Transaction Simulation
- Transaction Details

---

# 2. Enhance UI Navigation

### Current Behavior
- Multiple actions are available as clickable buttons/components.

### Required Change
- Improve UI organization by using tabs instead of multiple clickable sections.
- Keep related functionalities grouped together.

### Expected UI

Example:

```
Transaction Management

| Dashboard | Transactions | Pending | Simulation | Reports |
--------------------------------------------------

Content changes based on selected tab.
```

Benefits:
- Cleaner user experience
- Easier navigation
- Better scalability for future features

---

# 3. Transaction Rollback Reason Tracking

## Current Issue
- Rollback transactions do not provide sufficient information.

## Required Change

Whenever a transaction is rolled back:

Store:
- Rollback status
- Rollback timestamp
- Reason for rollback
- User/operator who triggered rollback

Example:

```
Transaction ID: TXN1001

Status: ROLLED_BACK

Reason:
"Transaction exceeded daily transaction limit"

Rollback By:
Operator123

Timestamp:
2026-08-05 10:30 UTC
```

---

# 4. Pending Transaction Reason Visibility

## Current Issue
- Transactions are shown as pending without explaining why.

## Required Change

For every pending transaction, display:

- Pending reason
- Rule that caused the transaction to enter pending state
- Rule details
- Violation information

Example:

```
Transaction ID: TXN2001

Status:
PENDING

Reason:
Suspicious transaction pattern detected

Rule Violated:
High Amount Transaction Rule

Rule Details:
Transaction amount exceeded €10,000 limit
```

---

# 5. Transaction Status Improvement

## Current Issue
- Not all transactions should automatically move into PENDING state.

## Required Change

Transaction lifecycle should correctly identify status:

```
CREATED
   |
   |
   +----> COMPLETED
   |
   |
   +----> FAILED
   |
   |
   +----> PENDING (Only when rules require review)
```

Only transactions requiring investigation should enter PENDING state.

Examples:

COMPLETED:
- Normal transaction
- No rule violations

PENDING:
- Rule violation detected
- Additional verification required

FAILED:
- Processing failure
- Invalid transaction details

---

# 6. Email and Mobile Number Verification

## Requirement

Add customer verification details to help operators approve suspicious transactions.

Store:

- Customer email
- Customer mobile number
- Email verification status
- Mobile verification status

Example:

```
Customer Details

Email:
customer@test.com

Email Verified:
YES

Mobile:
+353XXXXXXXXX

Mobile Verified:
YES
```

---

# Operator Approval Support

Operators should have enough information before approving/rejecting transactions.

Transaction review screen should display:

```
Transaction Information

Transaction ID
Account Details
Amount
Currency
Transaction Type
Timestamp

Verification Details

Email Verified: YES
Mobile Verified: YES

Rule Violation

Rule:
Velocity Rule

Reason:
Multiple transactions detected within short period

Decision:

[Approve]

[Reject]

[Rollback]
```

---

# Minor Fixes

Additional improvements:

- Fix UI inconsistencies.
- Improve transaction details display.
- Improve error messages.
- Validate mandatory transaction fields.
- Ensure timestamps are stored in UTC.
- Improve filtering and search experience.

---

# Updated Transaction Management Scope

## Core Features

- Create transactions
- View transaction history
- Search and filter transactions
- Update transaction status
- Transaction dashboard
- Rule engine integration
- Pending transaction management
- Rollback tracking
- Customer verification details
- Operator approval workflow

---

# API Changes Required

New/Updated APIs:

## Transaction Creation

```
POST /transactions
```

Creates a new transaction.

---

## Transaction History

```
GET /transactions
```

Supports filtering:

- Transaction ID
- Account
- Amount
- Date range
- Status

---

## Update Transaction Status

```
PUT /transactions/{id}/status
```

Updates transaction lifecycle status.

---

## Rollback Transaction

```
POST /transactions/{id}/rollback
```

Request:

```json
{
  "reason": "Suspicious activity detected"
}
```

---

## Pending Transaction Details

```
GET /transactions/{id}/pending-details
```

Returns:

- Pending reason
- Violated rule
- Verification information

---

# Final Expected Outcome

The Transaction Management module should provide:

- Cleaner tab-based UI
- Better transaction visibility
- Clear pending reasons
- Complete rollback audit trail
- Customer verification information
- Accurate transaction status handling
- Better support for operator decisions