# IndiraTrade Operations Console

A read-only operations console for an Indian stock broker's exception desk, built with Java Spring Boot.

## What This Project Does

This application helps operations staff identify client account exceptions by:
1. **Ingesting** reports from multiple sources (holdings, DP positions, cash ledger, bank confirmations)
2. **Normalizing** the data to a common format
3. **Identifying discrepancies** (holding mismatches, cash mismatches, pending DP movements)
4. **Tracking cases** through investigation workflow
5. **Enforcing role-based access** (Support, Investigator, Ops Lead, Auditor)

## Tech Stack

- **Backend**: Java 17 + Spring Boot 3.2
- **Database**: SQLite (portable, no setup needed)
- **Parsers**: OpenCSV, Jsoup (HTML), Jackson (JSON), Apache POI (XLSX)
- **Security**: Spring Security (demo mode)

## How to Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the JAR
java -jar target/operations-console-1.0.0-SNAPSHOT.jar
```

The app starts on `http://localhost:8080`

## Screenshots

| View | Image |
|------|-------|
| Investigator (full access + evidence) | `screenshots/01-investigator.png` |
| Support (masked client IDs) | `screenshots/02-support.png` |
| Ops Lead (can resolve cases) | `screenshots/03-opslead.png` |
| Auditor (read-only, all clients) | `screenshots/04-auditor.png` |
| Dashboard | `screenshots/05-dashboard.png` |

For the live demo, open `http://localhost:8080?user=USR001&case=1` to show a specific role and case.

## API Endpoints

| Method | Endpoint | Description | Role Required |
|--------|----------|-------------|---------------|
| POST | `/api/imports` | Import all synthetic data files | ops_lead |
| GET | `/api/imports/:id` | Get import details | any |
| GET | `/api/cases` | List cases with filters | any (scoped) |
| GET | `/api/cases/:id` | Get case detail with notes | any (scoped) |
| POST | `/api/cases/:id/notes` | Add investigation note | investigator |
| POST | `/api/cases/:id/transition` | Change case state | investigator/ops_lead |
| GET | `/api/cases/metrics` | Dashboard metrics | any |

## How to Test

```bash
# 1. Import all data and generate cases
curl -X POST http://localhost:8080/api/imports

# 2. List all cases (as investigator Priya)
curl -H "X-User-Id: USR001" http://localhost:8080/api/cases

# 3. Get case detail
curl -H "X-User-Id: USR001" http://localhost:8080/api/cases/1

# 4. Add a note
curl -X POST -H "Content-Type: application/json" -H "X-User-Id: USR001" \
  -d '{"note": "Checking with DP team", "evidenceVersion": "1"}' \
  http://localhost:8080/api/cases/1/notes

# 5. Transition case state
curl -X POST -H "Content-Type: application/json" -H "X-User-Id: USR001" \
  -d '{"newState": "INVESTIGATING", "reason": "Starting investigation"}' \
  http://localhost:8080/api/cases/1/transition

# 6. Try unauthorized access (Support user can't see CLI003)
curl -H "X-User-Id: USR002" http://localhost:8080/api/cases
```

## Key Design Decisions

See `decisions.md` for detailed architecture decisions, including:
- Data flow diagram
- Source authority hierarchy
- Cases that must stay UNKNOWN
- Technical choices

## Project Structure

```
src/main/java/com/indiratrading/
├── OperationsConsoleApplication.java  # Main entry point
├── config/
│   ├── DataSeeder.java                # Loads users from JSON
│   └── SecurityConfig.java            # Security configuration
├── model/
│   ├── Case.java                      # Case entity
│   ├── CaseNote.java                  # Audit notes
│   ├── ImportHistory.java             # Import tracking
│   ├── SourceVersion.java             # File versioning
│   └── User.java                      # User/role entity
├── repository/
│   ├── CaseRepository.java
│   ├── CaseNoteRepository.java
│   ├── ImportHistoryRepository.java
│   ├── SourceVersionRepository.java
│   └── UserRepository.java
├── service/
│   ├── ImportService.java             # Core import + case generation
│   ├── CaseService.java               # Case workflow management
│   └── UserService.java               # RBAC checks
├── controller/
│   ├── ImportController.java          # Import endpoints
│   └── CaseController.java            # Case endpoints
└── parser/
    ├── DataParser.java                 # Parser interface
    ├── CsvParser.java                  # CSV parsing
    ├── HtmlParser.java                 # HTML table parsing
    ├── JsonLineParser.java             # JSON lines parsing
    └── XlsxParser.java                 # Excel parsing
```

## Synthetic Data

The `src/main/resources/data/` folder contains:
- `access_scopes.json` - User roles and client permissions
- `internal_holdings_snapshot.csv` - Broker's holdings view
- `dp_position_extract.html` - Depository position extract
- `cash_ledger.jsonl` - Cash events (posted and pending)
- `bank_confirmation.csv` - Bank credit/debit references
- `exchange_reference.csv` - Symbol/ISIN mapping and prices
