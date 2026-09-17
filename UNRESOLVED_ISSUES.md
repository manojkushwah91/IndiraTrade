# Unresolved Data Quality Problems

These are known issues in the synthetic data that require investigation.

---

## 1. INFY Holding Mismatch (HIGH)

**Client**: CLI001
**ISIN**: INE031A01021 (INFY)
**Internal Quantity**: 200 shares
**DP Settled Quantity**: 180 shares
**Delta**: +20 shares

**Impact**: Internal view shows 20 MORE shares than DP confirms. This is a confirmed financial discrepancy.

**Possible Causes**:
- Shares purchased but not yet settled in DP
- Shares transferred out but not reflected in internal
- Data entry error in one of the systems

**Required Evidence**:
- Trade blotter for CLI001 INFY transactions
- DP settlement history
- Client account statement

**Resolution**: Cannot resolve without additional evidence. Case remains OPEN.

---

## 2. HDFCBANK Pending DP Movement (MEDIUM)

**Client**: CLI002
**ISIN**: INE049A01021 (HDFCBANK)
**DP Settled**: 45 shares
**DP Pending**: 5 shares
**Movement State**: PENDING

**Impact**: 5 shares are in transit. The pending movement must NOT silently become a settled holding.

**Required Evidence**:
- DP movement confirmation
- Settlement date
- Transfer details

**Resolution**: Wait for DP movement to settle. Do not close case until movement_state changes to SETTLED.

---

## 3. Duplicate Bank UTR (HIGH)

**Client**: CLI002
**UTR**: UTR002345678
**Occurrences**: 2
**Amount**: 7500 each
**Direction**: CREDIT

**Client Statement**: "Money came once"

**Impact**: Ambiguous bank reference. Could be:
- Bank error (duplicate entry)
- Two separate deposits with same reference
- Split transaction

**Required Evidence**:
- Bank statement from client
- Bank reconciliation report
- Original NEFT/RTGS confirmation

**Resolution**: Mark as `UNMATCHED_IDENTITY`. Keep both lines. Do not delete evidence.

---

## 4. PENDING Cash Event (MEDIUM)

**Client**: CLI002
**Event ID**: EVT004
**Amount**: 100000 paise (1000 rupees)
**State**: PENDING
**Description**: "NEFT pending confirmation"

**Impact**: This event is NOT posted cash. Cannot be used for cash balance calculations until state changes to POSTED.

**Required Evidence**:
- Bank confirmation of NEFT status
- Updated cash ledger with POSTED state

**Resolution**: Track event. When state changes to POSTED, update cash calculations.

---

## 5. Missing Bank Confirmation (MEDIUM)

**Client**: CLI004
**Cash Ledger Entry**: EVT007, CREDIT, 400000 paise
**Bank Confirmation**: None found

**Impact**: Cannot verify cash balance without bank confirmation. One source's silence is UNKNOWN, not zero.

**Required Evidence**:
- Bank confirmation for CLI004
- Client bank statement

**Resolution**: Request bank confirmation. Mark as `MISSING_SOURCE`.

---

## 6. Exchange Symbol Differences

**Issue**: NSE and BSE use different display symbols for same ISIN.

| ISIN | NSE Symbol | BSE Symbol |
|------|------------|------------|
| INE002A01018 | RELIANCE | RELIANCE |
| INE049A01021 | HDFCBANK | HDFCBANK |

**Impact**: Cannot join by display symbol. Must use ISIN as canonical key.

**Resolution**: Implemented in code - all parsers normalize ISIN to uppercase.

---

## 7. Date Format Inconsistency

**Issue**: Different sources may use different date formats.

| Source | Format |
|--------|--------|
| Internal Holdings | ISO 8601: 2026-09-15T15:30:00+05:30 |
| DP Position | ISO 8601: 2026-09-15T15:30:00+05:30 |
| Cash Ledger | ISO 8601: 2026-09-15T15:30:00+05:30 |
| Bank Confirmation | Simple: 2026-09-15 |

**Impact**: Need flexible date parsing.

**Resolution**: Implemented `DataNormalizer.parseFlexibleDate()` that tries multiple formats.

---

## 8. Paise vs Rupees

**Issue**: Cash ledger uses paise (integers), bank confirmation uses rupees (decimals).

| Source | Unit | Example |
|--------|------|---------|
| Cash Ledger | Paise | 500000 |
| Bank Confirmation | Rupees | 5000.00 |

**Impact**: Need to convert when comparing.

**Resolution**: Implemented `DataNormalizer.parsePaise()` that handles both formats.

---

## Recommendations for Production

1. **Automated Reconciliation**: Run daily reconciliation at T+1
2. **Alert Thresholds**: Set alerts for mismatches > 0.5% of holdings
3. **Audit Trail**: Keep all versions for 7 years (regulatory requirement)
4. **Backup**: Daily backup of all case data and notes
5. **Monitoring**: Track import success/failure rates
