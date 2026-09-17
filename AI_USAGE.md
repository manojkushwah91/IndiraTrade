# AI Usage Declaration

## What AI Assisted With

### Project Structure & Configuration
- Generated initial Maven pom.xml with Spring Boot dependencies
- Created application.properties configuration
- Designed package structure (model, repository, service, controller, parser)

### Data Modeling
- Designed entity classes (Case, CaseNote, ImportHistory, SourceVersion, User)
- Created repository interfaces with custom query methods
- Designed database schema relationships

### Parser Implementation
- Generated CSV parser using OpenCSV library
- Generated HTML table parser using Jsoup
- Generated JSON lines parser using Jackson
- Generated XLSX parser using Apache POI

### Service Layer
- Drafted ImportService with data ingestion logic
- Drafted CaseService with workflow state machine
- Drafted UserService for RBAC checks

### API Design
- Designed REST controller endpoints matching assessment requirements
- Created request/response DTOs

## What I Verified Myself

### Data Integrity
- Verified paise arithmetic uses long/BigDecimal (no floating point)
- Verified ISIN normalization (uppercase, trim)
- Verified cut_at timestamp handling

### Security
- Verified RBAC checks in controllers (client scope filtering)
- Verified role-based access for case transitions
- Verified audit trail logging in CaseNote

### Business Rules
- Verified holding mismatch calculation: `internal_qty - dp_settled_qty`
- Verified pending DP movement handling (separate case, not auto-resolved)
- Verified cash event summing (POSTED only, not PENDING)
- Verified duplicate bank reference detection

### Edge Cases
- Verified MISSING_SOURCE handling when DP data absent
- Verified UNMATCHED_IDENTITY for ambiguous bank references
- Verified idempotent imports (same file hash = no duplicate)

## What I Did NOT Use AI For

- Business domain rules (directly from assessment brief)
- Severity classification logic (from domain rules section)
- Case state machine transitions (from assessment requirements)
- Test data generation (manually created synthetic data)

## Limitations & Known Issues

1. SQLite doesn't support concurrent writes well - would need PostgreSQL for production
2. Security is simplified for demo - no real authentication
3. File versioning stores raw bytes in DB - would use object storage in production
4. No pagination optimization for 100K+ rows - would need cursor-based pagination
