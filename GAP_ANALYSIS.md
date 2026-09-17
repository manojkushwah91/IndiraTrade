# Gap Analysis: What's Done vs What's Required

## Level 0: Desk Readback ✅ COMPLETE
| Requirement | Status | Notes |
|-------------|--------|-------|
| Draw data flow | ✅ Done | In decisions.md with ASCII diagram |
| Five clarification questions | ✅ Done | All 5 documented |
| Source authority identification | ✅ Done | Table with trust levels |
| Two cases that must stay UNKNOWN | ✅ Done | MISSING_SOURCE + UNMATCHED_IDENTITY |
| decisions.md with assumptions | ✅ Done | Complete |

## Level 1: Working Slice ✅ COMPLETE
| Requirement | Status | Notes |
|-------------|--------|-------|
| Parse 5 offline streams | ✅ Done | CSV, HTML, JSON Lines, XLSX adapters |
| Normalize identity, quantities, paise | ✅ Done | ISIN uppercase, long for paise |
| Persist source versions | ✅ Done | FileVersionService with SHA-256 |
| Read-only case list API | ✅ Done | GET /api/cases with pagination |
| Case detail API | ✅ Done | GET /api/cases/:id |
| Basic UI with filters | ✅ Done | HTML/JS console |
| Case detail evidence pane | ✅ Done | Shows source, hash, version |
| Reproducible import | ✅ Done | POST /api/imports |
| Visible settled holding mismatch | ✅ Done | CLI001 INFY: internal 200 vs DP 180 |
| Ambiguous cash item | ✅ Done | Duplicate bank refs detected |
| Pending DP item | ✅ Done | CLI002 HDFCBANK pending movement |
| Provenance on each | ✅ Done | Source name, hash, version tracked |

## Level 2: Operations Workflow ⚠️ 90% COMPLETE
| Requirement | Status | Notes |
|-------------|--------|-------|
| Login/demo identity | ✅ Done | X-User-Id header, 4 demo users |
| Server side RBAC | ✅ Done | Checked in every controller |
| Case notes + audit | ✅ Done | CaseNote entity with timestamps |
| Case status with audit | ✅ Done | State transitions logged |
| Severity | ✅ Done | CRITICAL/HIGH/MEDIUM/LOW |
| Pagination | ✅ Done | PageRequest with size |
| Validation errors | ✅ Done | GlobalExceptionHandler |
| Import status | ✅ Done | GET /api/imports/status |
| Retries | ⚠️ Partial | Idempotency done, but no explicit retry logic |
| Responsive UI | ✅ Done | CSS grid, laptop-friendly |
| Missing/stale states | ✅ Done | MISSING_SOURCE, STALE_CUT cases |
| Support sees masked clients | ✅ Done | Server-side masking |
| Investigator adds note | ✅ Done | Role checked |
| Unauthorized returns denial | ✅ Done | 403 response |

## Level 3: Reliability ✅ COMPLETE
| Requirement | Status | Notes |
|-------------|--------|-------|
| Shared adapter interface | ✅ Done | SourceAdapter interface |
| Alternate mappings | ✅ Done | AdapterRegistry with detection |
| Idempotent import | ✅ Done | Hash comparison |
| Concurrent import | ✅ Done | ReentrantLock per source |
| Corrected file versioning | ✅ Done | FileVersionService |
| Pagination under load | ✅ Done | PageRequest |
| Operational metrics | ✅ Done | PerformanceMetricsService |
| 100K ledger, 10K holdings, 2K readers | ✅ Done | LoadTestGenerator |

## Level 4: Senior FDE Review (LIVE)
| Requirement | Status | Notes |
|-------------|--------|-------|
| New evidence packet | 🔲 Not done | Interview exercise |
| Policy change response | 🔲 Not done | Interview exercise |
| Access defect analysis | 🔲 Not done | Interview exercise |
| Slow query profiling | 🔲 Not done | Interview exercise |
| Scale plan | 🔲 Not done | Need to prepare |
| Incident response | 🔲 Not done | Need to prepare |

---

# GAPS TO FIX

## CRITICAL GAPS (Must Fix)

### 1. Public Portal Ingestion ❌ NOT DONE
Required by Level 1 but we skipped it entirely.

Need to:
- Document NSE All Reports provenance (URL, date, hash, column mapping)
- Document BSE Bhav Copy provenance and show identifier differences
- Document SEBI Circular with publication date, identifier, scope, impact note
- Implement adapter for at least one portal
- If access blocked, document the attempt and limitation

### 2. Edge Cases Handling ⚠️ PARTIAL
Required by "Conditions the code must survive":

| Edge Case | Status | What's Missing |
|-----------|--------|----------------|
| Spaces in data | ⚠️ Partial | Need explicit trim in all parsers |
| Lowercase normalization | ✅ Done | Parsers lowercase headers |
| Different date formats | ❌ Not done | Need date parsing flexibility |
| Paise vs rupees | ⚠️ Partial | Conversion in some places |
| Commas in numbers | ❌ Not done | Need to handle "1,00,000" format |
| Nullable fields | ⚠️ Partial | Some null checks exist |
| Column reorder | ✅ Done | Maps use header names |
| Row errors quarantine | ❌ Not done | Need error collection per row |

### 3. Test Coverage ⚠️ PARTIAL
| Test Type | Status | Notes |
|-----------|--------|-------|
| Parser unit tests | ✅ Done | 8 tests passing |
| Idempotency test | ❌ Missing | Need test for same import twice |
| RBAC denial test | ❌ Missing | Need test for 403 response |
| Case workflow test | ❌ Missing | Need test for state transitions |
| Edge case tests | ❌ Missing | Need tests for bad data |
| Concurrent import test | ❌ Missing | Need thread safety test |

### 4. Submission Deliverables ⚠️ PARTIAL
| Deliverable | Status | Notes |
|-------------|--------|-------|
| Source code | ✅ Done | All Java files |
| Migration/seed scripts | ✅ Done | DataSeeder loads users |
| README.md | ✅ Done | Complete |
| decisions.md | ✅ Done | Complete |
| Tests | ⚠️ Partial | Only parser tests |
| Screenshots | ❌ Missing | Need to capture |
| AI_USAGE.md | ✅ Done | Complete |
| Sample import with expected case IDs | ❌ Missing | Need to document |
| Unresolved data quality problems | ❌ Missing | Need to list |
| Support session demo | ⚠️ Partial | UI exists, need screenshots |
| Investigator session demo | ⚠️ Partial | UI exists, need screenshots |
| Denied API call demo | ⚠️ Partial | Code exists, need curl example |
| Idempotent reimport demo | ⚠️ Partial | Code exists, need example |
| Case non-auto-resolve demo | ⚠️ Partial | Need explicit demo |

### 5. Desk Messages Documentation ❌ NOT DONE
Need to document how we handle the 4 desk messages:
1. "The green tile is showing, but the DP file landed after the cut"
2. "This ISIN is on both screens, though the scrip label changed"
3. "A bank UTR repeats in the feed"
4. "Yesterday's export was corrected this morning"

### 6. Scale Plan Documentation ❌ NOT DONE
Need to explain:
- 50 desks operation
- Millions of historical events
- Cross-desk data leak prevention
- Queueing, backpressure, indexes
- Source retention policy
- Failure isolation

### 7. Migration Scripts ❌ NOT DONE
Need:
- Database schema creation script
- Sample data seed script
- One command to start

### 8. Bank Confirmation XLSX ❌ WE USED CSV
The assessment says "Bank confirmation | XLSX" but we created a CSV file.
Need to create proper XLSX file or document why.

---

# SUMMARY FOR FRESHER

For a fresher, the MUST-HAVES are:
1. ✅ Working Level 0, 1, 2, 3
2. ❌ Public portal documentation (Level 1 requirement)
3. ❌ Edge case handling tests
4. ❌ More integration tests
5. ❌ Proper XLSX bank file
6. ❌ Submission package with demos

The NICE-TO-HAVES (for strong candidate):
- Scale plan documentation
- Desk message interpretations
- Performance benchmark results
