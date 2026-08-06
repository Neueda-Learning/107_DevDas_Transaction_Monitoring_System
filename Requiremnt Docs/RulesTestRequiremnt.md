# Rules Unit Test Requirements

## Scope
Unit tests for the `rules` module in `src/main/java/com/hsbc/tms/rules`.

## Planned Test Files and Function Coverage

### 1) `src/test/java/com/hsbc/tms/rules/service/RuleValidationServiceTest.java`
Functions to test:
- `validateForCreate(...)`
- `validateForUpdate(...)`

Coverage focus:
- Required fields (`name`, `type`, `severity`)
- Type-specific validation (`AMOUNT_THRESHOLD`, `VELOCITY`, `NEW_PAYEE`, `DAILY_LIMIT`)
- Positive threshold validation

### 2) `src/test/java/com/hsbc/tms/rules/service/RuleApiMapperTest.java`
Functions to test:
- `RuleApiMapper.toRuleResponse(...)`
- `RuleApiMapper.toRuleAuditHistoryResponse(...)`

Coverage focus:
- Field-to-field mapping correctness

### 3) `src/test/java/com/hsbc/tms/rules/service/RuleServiceTest.java`
Functions to test:
- `getRules(...)`
- `getRuleStats()`
- `getRuleHistory(...)`
- `createRule(...)`
- `updateRule(...)`
- `updateRuleStatus(...)`
- `softDeleteRule(...)`

Coverage focus:
- Success paths and mapping
- Not-found and conflict scenarios
- Audit history creation side effects
- Type-specific field updates during create/update

### 4) `src/test/java/com/hsbc/tms/rules/service/RuleEngineServiceTest.java`
Functions to test:
- `evaluate(...)`

Coverage focus:
- No active rules
- Missing evaluator for rule type
- Triggered and non-triggered evaluator outcomes
- Execution history persistence and alert creation behavior

### 5) `src/test/java/com/hsbc/tms/rules/service/ruleengine/AmountThresholdRuleEvaluatorTest.java`
Functions to test:
- `supportedType()`
- `evaluate(...)`

Coverage focus:
- Missing threshold config
- Triggered when amount exceeds threshold
- Not triggered when conditions are not met

### 6) `src/test/java/com/hsbc/tms/rules/service/ruleengine/VelocityRuleEvaluatorTest.java`
Functions to test:
- `supportedType()`
- `evaluate(...)`

Coverage focus:
- Missing velocity config
- Triggered when count exceeds threshold
- Not triggered when conditions are not met

### 7) `src/test/java/com/hsbc/tms/rules/service/ruleengine/NewPayeeRuleEvaluatorTest.java`
Functions to test:
- `supportedType()`
- `evaluate(...)`

Coverage focus:
- Triggered for first payee transaction
- Not triggered when payee already has prior transactions

### 8) `src/test/java/com/hsbc/tms/rules/service/ruleengine/DailyLimitRuleEvaluatorTest.java`
Functions to test:
- `supportedType()`
- `evaluate(...)`

Coverage focus:
- Missing amount threshold config
- Triggered when daily total exceeds limit
- Not triggered when conditions are not met

### 9) `src/test/java/com/hsbc/tms/rules/repository/JdbcMonitoringRuleRepositoryTest.java`
Functions to test:
- `findByActiveTrue()`
- `existsByNameIgnoreCase(...)`
- `existsByNameIgnoreCaseAndIdNot(...)`
- `count()`
- `countByActiveTrue()`
- `countByActiveFalse()`
- `countGroupedByType()`
- `countGroupedBySeverity()`
- `search(...)`
- `findById(...)`
- `existsById(...)`
- `save(...)`

Coverage focus:
- CRUD behavior and SQL filter correctness
- Grouped stats defaults and populated counts
- Insert/update path behavior in `save(...)`

### 10) `src/test/java/com/hsbc/tms/rules/repository/JdbcRuleAuditHistoryRepositoryTest.java`
Functions to test:
- `save(...)`
- `findByRuleIdOrderByChangedAtAsc(...)`

Coverage focus:
- Insert persistence
- Ordered retrieval and row mapping correctness

### 11) `src/test/java/com/hsbc/tms/rules/repository/JdbcRuleExecutionHistoryRepositoryTest.java`
Functions to test:
- `save(...)`
- `findByTransactionId(...)`

Coverage focus:
- Insert persistence
- Filtered retrieval and row mapping correctness

### 12) `src/test/java/com/hsbc/tms/rules/repository/JdbcRuleTransactionMetricsRepositoryTest.java`
Functions to test:
- `countByAccountIdAndTransactionTimeBetween(...)`
- `findByAccountIdAndTransactionTimeBetween(...)`
- `countByAccountIdAndPayeeIdAndTransactionTimeBefore(...)`
- `sumAmountByAccountAndTransactionTimeRange(...)`

Coverage focus:
- Time-range filtering
- Account/payee aggregation logic
- Transaction row mapping back to domain model

### 13) `src/test/java/com/hsbc/tms/config/DataSeederTest.java`
Functions to test:
- `seedRules(...)`

Coverage focus:
- Seeds 4 default rules only when repository is empty
- Does not seed when data already exists

## Execution Plan
1. Create test classes above.
2. Run rules-focused tests first.
3. Run full test suite.
4. Confirm all tests pass.

