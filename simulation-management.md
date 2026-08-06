# Simulation Management

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Package Structure](#package-structure)
4. [DTOs](#dtos)
   - [SimulationRequest](#simulationrequest)
   - [SimulationResponse](#simulationresponse)
5. [Service Behavior](#service-behavior)
6. [Controller Layer](#controller-layer)
7. [API Reference](#api-reference)
8. [Generated Data Patterns](#generated-data-patterns)
9. [Validation Rules](#validation-rules)
10. [Integration Points](#integration-points)
11. [Operational Notes](#operational-notes)

---

## Overview

The Simulation Management module generates synthetic transaction traffic for demos, QA scenarios, and rule-engine behavior checks.

It is designed to:

- Generate a requested number of transactions through the same `TransactionService` used by production APIs.
- Produce a mix of random and burst patterns so rules such as velocity, threshold, and new payee can be exercised.
- Return created transaction IDs for quick follow-up queries.

This module does not own dedicated persistence tables. It writes to the `transactions` table through transaction APIs/services.

---

## Architecture

```
HTTP Request
     |
     v
+-----------------------+
| SimulationController  |
+-----------+-----------+
            |
            v
+-----------------------+
| SimulationService     |
+-----------+-----------+
            |
            v
+-----------------------+
| TransactionService    |
+-----------+-----------+
            |
            v
   Transaction + Rule + Alert flow
```

Because generated data goes through `TransactionService.createTransaction(...)`, every generated transaction also passes rule evaluation and alerting logic.

---

## Package Structure

```
com.hsbc.tms.simulation
|- controller
|  `- SimulationController.java
|- dto
|  |- SimulationRequest.java
|  `- SimulationResponse.java
`- service
   `- SimulationService.java
```

---

## DTOs

### SimulationRequest

**File:** `src/main/java/com/hsbc/tms/simulation/dto/SimulationRequest.java`

| Field | Type | Required | Constraints |
|---|---|---|---|
| `count` | `int` | Yes | `@Min(1)`, `@Max(200)` |

### SimulationResponse

**File:** `src/main/java/com/hsbc/tms/simulation/dto/SimulationResponse.java`

| Field | Type | Description |
|---|---|---|
| `count` | `int` | Requested number |
| `createdCount` | `int` | Number successfully created |
| `createdTransactionIds` | `List<UUID>` | IDs returned by transaction service |

---

## Service Behavior

**File:** `src/main/java/com/hsbc/tms/simulation/service/SimulationService.java`

### `generate(int count)`

Current generation split:

- `randomCount = count / 2`
- `burstCount = count - randomCount`

This intentionally blends background traffic with high-risk burst traffic.

### Random transaction generator

`buildRandomTransaction(...)` uses:

- random account from fixed list (`ACC-1001`..`ACC-1007`),
- random payee from fixed list (`PAY-200`..`PAY-207`),
- random amount in approximately `[10, 20000]`,
- random type (`DEBIT`/`CREDIT`),
- status biased toward `COMPLETED` with occasional `FAILED`,
- transaction time within last 24 hours.

### Burst transaction generator

`buildBurstTransaction(...)` uses:

- fixed burst account: `ACC-1001`,
- synthetic new payee: `PAY-NEW-<random-3-digits>`,
- high-risk amount in approximately `[12000, 40000]`,
- status fixed to `COMPLETED`,
- transaction time within last 15 minutes.

This pattern is useful to trigger:

- amount threshold,
- velocity,
- new payee,
- daily limit.

---

## Controller Layer

**File:** `src/main/java/com/hsbc/tms/simulation/controller/SimulationController.java`

Base path: `/api/v1/simulator`

Endpoint:

- `POST /api/v1/simulator/generate`

Request body: `SimulationRequest`

Response body: `SimulationResponse`

---

## API Reference

### `POST /api/v1/simulator/generate`

Generates synthetic transactions and returns IDs.

**Example request:**

```json
{
  "count": 20
}
```

**Example response shape:**

```json
{
  "count": 20,
  "createdCount": 20,
  "createdTransactionIds": [
    "0f498f7b-ccf4-4f49-8df2-f715f7fece57",
    "84121f5e-9985-4a74-b620-f294e0df6e06"
  ]
}
```

---

## Generated Data Patterns

- **Random half**: broad noise for general search/filter testing.
- **Burst half**: concentrated suspicious behavior for alert workflow testing.
- **Currency**: currently always `USD`.
- **Descriptions**:
  - random: `Simulated transaction <n>`
  - burst: `Simulation burst transaction <n>`

---

## Validation Rules

- `count` must be between `1` and `200` (inclusive).
- Invalid request payloads are rejected by Bean Validation.

---

## Integration Points

- Calls `TransactionService.createTransaction(...)` for each generated row.
- Automatically triggers downstream rule evaluation via `RuleEngineService`.
- Automatically creates alerts when rules are triggered.

This makes simulation useful for end-to-end checks without bypassing business logic.

---

## Operational Notes

- Generation uses Java `Random`; output is intentionally non-deterministic.
- There is no rollback wrapper around the full batch, so partial creation can occur if one request fails mid-run.
- For deterministic tests, consider introducing seeded random support in a future enhancement.

---

*Last updated: August 6, 2026*

