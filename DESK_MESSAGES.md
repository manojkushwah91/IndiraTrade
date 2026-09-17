# Desk Message Interpretations

These are the four desk messages from the assessment, with investigation steps.

---

## Message 1: "The green tile is showing, but the DP file landed after the cut. Can we close it?"

**What this means**: A status indicator (green tile) shows everything is OK, but the DP position extract was received AFTER the cut time of the internal holdings snapshot.

**Investigation steps**:
1. Check the `cut_at` timestamp of the internal holdings snapshot
2. Check the `received_at` timestamp of the DP position extract
3. Compare: if DP arrived after the cut, the data may be stale

**Evidence needed**:
- Internal holdings snapshot: `cut_at = 2026-09-15T15:30:00+05:30`
- DP position extract: `received_at = 2026-09-15T16:00:00+05:30` (30 min late)

**Answer**: **NO, you cannot close it.** The green tile is misleading because it's comparing against stale data. The holdings snapshot shows a position at 3:30 PM, but the DP data is from 4:00 PM. We cannot confirm the position was the same at 3:30 PM. Mark as `STALE_CUT` and request fresh DP data aligned to the same cut time.

---

## Message 2: "This ISIN is on both screens, though the scrip label changed. Which account is short?"

**What this means**: The same ISIN appears in both internal holdings and DP positions, but the display symbol (scrip label) is different between the two sources. One account shows a shortage.

**Investigation steps**:
1. Find the ISIN in both sources
2. Note the different symbols (e.g., "RELIANCE" vs "RELIANCEIND")
3. Calculate delta: `internal_qty - dp_settled_qty`
4. Identify which side is short (negative delta = internal has less)

**Evidence needed**:
- Internal: ISIN `INE002A01018`, symbol `RELIANCE`, quantity = 100
- DP: ISIN `INE002A01018`, symbol `Reliance Industries`, settled_qty = 95

**Answer**: The **internal account is short by 5 shares**. The symbol difference is expected (NSE vs BSE naming conventions). Use ISIN as the canonical join key, not display symbol. The delta is `100 - 95 = +5` (internal has more), wait - that means internal has MORE. Let me recalculate: if internal shows 100 and DP shows 95, then `internal - dp = +5`, meaning internal claims 5 MORE than DP confirms. This is a `HOLDING_MISMATCH` case. The account that is "short" depends on which source is authoritative - DP is authoritative for settled holdings, so the **internal account is overstated by 5 shares**.

---

## Message 3: "A bank UTR repeats in the feed. The client says the money came once. Keep both lines?"

**What this means**: The same bank reference number (UTR - Unique Transaction Reference) appears twice in the bank confirmation file. Client claims only one transaction happened.

**Investigation steps**:
1. Find all rows with the duplicate UTR
2. Compare: same amount? Same direction? Same date?
3. Check cash ledger for corresponding entries

**Evidence needed**:
- Bank row 1: UTR=`UTR001234567`, amount=5000, direction=CREDIT, date=2026-09-14
- Bank row 2: UTR=`UTR001234567`, amount=5000, direction=CREDIT, date=2026-09-14
- Cash ledger: No matching entry for 5000 on 2026-09-14

**Answer**: **Keep both lines, but flag as `DUPLICATE_BANK_REF`.** Do NOT delete either row - that would destroy evidence. The duplicate reference is ambiguous. It could be:
- A genuine duplicate (bank error)
- Two separate deposits with same reference (unlikely but possible)
- A split transaction

The cash ledger shows no matching entry, so we cannot confirm either row. Mark as `UNMATCHED_IDENTITY` and require manual investigation with bank reconciliation. Do not automatically net against ledger entries.

---

## Message 4: "Yesterday's export was corrected this morning. The note on the old case must still be there"

**What this means**: A source file (e.g., DP position extract) was corrected/reissued. The old version had a case created against it. The investigator's note on that case must be preserved.

**Investigation steps**:
1. Check import history for the source
2. Find version 1 (original) and version 2 (corrected)
3. Verify the case still exists and notes are intact
4. Compare the two versions to see what changed

**Evidence needed**:
- Import v1: hash=`abc123`, received=2026-09-15T16:00:00
- Import v2: hash=`def456`, received=2026-09-16T09:00:00
- Case #5: Created against v1, has note "Checking with DP team"
- Diff: Line 3 changed from `settled_qty=95` to `settled_qty=100`

**Answer**: **Yes, the note must still be there.** Our system preserves:
- All versions of the source file (raw bytes stored)
- All case notes (immutable audit trail)
- The link between case and evidence version

The corrected file creates a new version. The old case remains with its notes. If the correction resolves the discrepancy, an investigator can transition the case to RESOLVED. If it creates a new discrepancy, a new case may be opened against the new version. Prior notes are NEVER deleted.

---

## How to Present These in Interview

> "For each desk message, I would first identify what evidence is needed, then check the system for that evidence. I would not guess or make assumptions. If the evidence is insufficient, I would say 'insufficient evidence' and request the missing data."

> "The key principle is: one source's silence is UNKNOWN, not zero. We never assume data we don't have."

> "For the corrected file scenario, our versioning system ensures full traceability. Every version is stored, every note is preserved, and the investigator can see exactly what changed."
