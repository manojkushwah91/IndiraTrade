# Sample Import with Expected Case IDs

## How to Run Sample Import

```bash
# Start the application
mvn spring-boot:run

# In another terminal, run the import
curl -X POST http://localhost:8080/api/imports
```

## Expected Results

### Import Summary
```json
{
  "importResults": [
    {"sourceName": "INTERNAL_HOLDINGS", "version": 1},
    {"sourceName": "DP_POSITION", "version": 1},
    {"sourceName": "CASH_LEDGER", "version": 1},
    {"sourceName": "BANK_CONFIRMATION", "version": 1},
    {"sourceName": "EXCHANGE_REFERENCE", "version": 1}
  ],
  "totalImports": 5,
  "errors": [],
  "status": "SUCCESS"
}
```

### Expected Cases Generated

| Case ID | Client | ISIN | Type | Severity | Description |
|---------|--------|------|------|----------|-------------|
| 1 | CLI001 | INFY | HOLDING_MISMATCH | MEDIUM | Internal=200, DP=180, Delta=20 |
| 2 | CLI002 | HDFCBANK | PENDING_DP_MOVEMENT | MEDIUM | DP has pending movement of 5 shares |
| 3 | CLI002 | N/A | MISSING_SOURCE | MEDIUM | No bank confirmation for some events |
| 4 | CLI002 | N/A | DUPLICATE_BANK_REF | HIGH | UTR002345678 appears twice |
| 5 | CLI001 | N/A | CASH_MISMATCH | HIGH | Posted cash ≠ bank credits |

### Verification Commands

```bash
# List all cases
curl -H "X-User-Id: USR001" http://localhost:8080/api/cases

# Get case details
curl -H "X-User-Id: USR001" http://localhost:8080/api/cases/1

# Check metrics
curl http://localhost:8080/api/cases/metrics

# Verify idempotency (run import again)
curl -X POST http://localhost:8080/api/imports
# Should return same import IDs, no new cases

# Check version comparison
curl "http://localhost:8080/api/versions/INTERNAL_HOLDINGS/compare?v1=1&v2=1"
# Should show identical (same version)
```

## Test Data Summary

### Internal Holdings (CLI001)
- RELIANCE: 100 shares (matches DP)
- INFY: 200 shares (DP shows 180 - MISMATCH)

### DP Positions
- CLI001 RELIANCE: 100 settled (matches)
- CLI002 HDFCBANK: 45 settled, 5 PENDING
- CLI003 TCS: 75 settled (matches)
- CLI001 INFY: 180 settled (internal shows 200 - MISMATCH)

### Cash Events
- CLI001: Posted 5000 - 2000 + 2500 = 5500 paise
- CLI002: Posted 7500, Pending 1000
- CLI003: Posted 3000 - 1500 = 1500 paise

### Bank Confirmations
- CLI001: Credit 5000, Debit 2000
- CLI002: Credit 7500, Credit 1000 (PENDING), DUPLICATE UTR
- CLI003: Credit 3000, Debit 1500

## Unresolved Data Quality Problems

1. **INFY Holding Mismatch**: Internal shows 200, DP shows 180. Delta of 20 shares unexplained.
2. **HDFCBANK Pending Movement**: 5 shares pending in DP. Not yet settled.
3. **Duplicate Bank UTR**: UTR002345678 appears twice for CLI002. Client says money came once.
4. **PENDING Cash Event**: CLI002 has 1000 paise pending. Not yet posted.
5. **Missing Bank Confirmation**: No bank confirmation for CLI004's cash ledger entry.

## Role-Based Access Test

```bash
# Support user - should see masked client IDs
curl -H "X-User-Id: USR002" http://localhost:8080/api/cases

# Investigator - full access to assigned clients
curl -H "X-User-Id: USR001" http://localhost:8080/api/cases

# Unauthorized - try to access unassigned client
curl -H "X-User-Id: USR002" http://localhost:8080/api/cases/3
# Should return 403 (CLI003 not in USR002's scope)
```
