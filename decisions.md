# Level 0: Desk Readback - Decisions

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           EXTERNAL SOURCES                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │   NSE All    │  │  BSE Bhav    │  │   SEBI       │  │   Bank       │   │
│  │   Reports    │  │  Copy        │  │   Circulars  │  │   Confirm    │   │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │
│         │                 │                 │                 │           │
│         └─────────────────┼─────────────────┼─────────────────┘           │
│                           │                 │                             │
└───────────────────────────┼─────────────────┼─────────────────────────────┘
                            │                 │
                            ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ADAPTER LAYER                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │ NSE Adapter  │  │ BSE Adapter  │  │ SEBI Adapter │  │ Manual Import│   │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │
│         │                 │                 │                 │           │
│         └─────────────────┼─────────────────┼─────────────────┘           │
│                           │                 │                             │
└───────────────────────────┼─────────────────┼─────────────────────────────┘
                            │                 │
                            ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    SYNTHETIC DATA INGESTION                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │ Internal     │  │ DP Position  │  │ Cash Ledger  │  │ Bank         │   │
│  │ Holdings     │  │ Extract      │  │ (JSON Lines) │  │ Confirmation │   │
│  │ (CSV)        │  │ (HTML)       │  │              │  │ (XLSX)       │   │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │
│         │                 │                 │                 │           │
│         └─────────────────┼─────────────────┼─────────────────┘           │
│                           │                 │                             │
└───────────────────────────┼─────────────────┼─────────────────────────────┘
                            │                 │
                            ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PARSER LAYER                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │ CSV Parser   │  │ HTML Parser  │  │ JSON Parser  │  │ XLSX Parser  │   │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │
│         │                 │                 │                 │           │
│         └─────────────────┼─────────────────┼─────────────────┘           │
│                           │                 │                             │
└───────────────────────────┼─────────────────┼─────────────────────────────┘
                            │                 │
                            ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    NORMALIZATION & MATCHING                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  • Normalize ISIN, client_id, position_type, cut_at                 │   │
│  │  • Convert paise to integer (no floating point)                     │   │
│  │  • Match holdings: (client_id, ISIN, position_type, cut_at)        │   │
│  │  • Track source versions and provenance                             │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CASE GENERATION                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │ Holding      │  │ Cash         │  │ DP Pending   │  │ Missing      │   │
│  │ Mismatch     │  │ Discrepancy  │  │ Movement     │  │ Source       │   │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │
│         │                 │                 │                 │           │
│         └─────────────────┼─────────────────┼─────────────────┘           │
│                           │                 │                             │
└───────────────────────────┼─────────────────┼─────────────────────────────┘
                            │                 │
                            ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    OPERATIONS CONSOLE                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  • Case List with filters (severity, state, client scope)          │   │
│  │  • Case Detail with evidence panel                                  │   │
│  │  • Role-based access (Support, Investigator, Ops Lead, Auditor)    │   │
│  │  • Audit trail for all changes                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Five Clarification Questions

1. **Data Cut Timing**: What is the expected cut time for daily reports? Should we normalize all sources to IST midnight, or is there a specific cutoff (e.g., 3:30 PM IST after market close)?

2. **ISIN Resolution**: When an ISIN appears with different display symbols across NSE and BSE, should we treat this as an exception or normalize it? The brief says "unmatched symbol is an exception" but ISIN is the join key.

3. **Bank Reference Matching**: The brief says "A bank reference is not a unique ledger event ID." How should we match bank entries to cash ledger events? By amount + date range? Manual review only?

4. **Pending DP Movements**: If a DP movement has `movement_state=PENDING` and the quantity matches a holding delta, should we auto-link them as evidence or require manual investigator review?

5. **Corrected File Handling**: When a corrected file arrives, should we automatically create a new version and diff, or require explicit operator action to trigger comparison?

---

## Source Authority Identification

| Source | Authority Level | Trust Model |
|--------|----------------|-------------|
| DP Position Extract | **Primary** (depository record) | External system, highest authority for holdings |
| Internal Holdings Snapshot | **Secondary** (broker's view) | Internal system, may have lag vs DP |
| Cash Ledger | **Primary** (posted entries) | Internal accounting, authoritative for cash |
| Bank Confirmation | **Supporting** (external validation) | Must be manually matched, not auto-netted |
| Exchange Reference | **Reference only** (prices/mapping) | Market data, not account entitlement |
| Access Scopes | **Configuration** (user/client mapping) | Admin-managed, controls RBAC |

---

## Two Cases That Must Stay UNKNOWN

### Case 1: MISSING_SOURCE - Late DP File
**Scenario**: The DP position extract arrives after the internal holdings snapshot cut time. The holdings snapshot shows a position, but DP data is stale.

**Why UNKNOWN**: We cannot determine if the DP position changed between the snapshot cut and the DP file receipt. The difference could be:
- Legitimate pending movement
- Data lag
- Actual discrepancy

**Action**: Mark as `MISSING_SOURCE` with `STALE_CUT` evidence. Require fresh DP data before resolution.

### Case 2: UNMATCHED_IDENTITY - Bank Reference Ambiguity
**Scenario**: A bank UTR (Unique Transaction Reference) appears twice in the feed with same reference but different amounts or dates. Client claims money came once.

**Why UNKNOWN**: Without additional evidence (bank statement, client confirmation), we cannot determine:
- Which entry is correct
- If both are legitimate (partial payments)
- If one is a duplicate

**Action**: Mark as `UNMATCHED_IDENTITY` with `CONFLICTING_EVIDENCE`. Require manual investigation with bank reconciliation.

---

## Key Technical Decisions

1. **Database**: SQLite for local demo (simple, no setup, portable)
2. **Build Tool**: Maven (standard Java ecosystem)
3. **Framework**: Spring Boot (fast setup, good for REST APIs)
4. **Paise Handling**: Always use `long` or `BigDecimal`, never `double`/`float`
5. **ISIN as Join Key**: Normalize to uppercase, trim whitespace
6. **Versioning**: Store file hash + timestamp for each import
7. **Audit Trail**: Immutable event log for all state changes

---

## Assumptions

1. All data is synthetic - no real broker data
2. Market hours are 9:15 AM to 3:30 PM IST
3. DP files arrive T+1 (next day after market close)
4. Bank confirmations are daily batch files
5. One client can have multiple positions (equity, derivatives)
6. Support users see masked client IDs (first 3 chars + asterisks)
