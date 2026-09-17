# Scale Plan: 50 Desks, Millions of Events

## Current Architecture (Single Instance)

```
[Web UI] → [Spring Boot] → [SQLite]
```

This works for demo. For production with 50 desks and millions of events:

---

## Production Architecture

```
                        ┌─────────────────┐
                        │   Load Balancer  │
                        │   (nginx/ALB)    │
                        └────────┬────────┘
                                 │
                ┌────────────────┼────────────────┐
                │                │                │
         ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
         │  App Node 1  │  │  App Node 2  │  │  App Node 3  │
         │  (8 cores)   │  │  (8 cores)   │  │  (8 cores)   │
         └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
                │                │                │
                └────────────────┼────────────────┘
                                 │
                        ┌────────▼────────┐
                        │  PostgreSQL DB   │
                        │  (Primary)       │
                        └────────┬────────┘
                                 │
                        ┌────────▼────────┐
                        │  PostgreSQL DB   │
                        │  (Replica)       │
                        └─────────────────┘
```

---

## Cross-Desk Data Leak Prevention

### Problem
50 desks, each handling different clients. Desk A should not see Desk B's client data.

### Solution: Row-Level Security (RLS)

```sql
-- PostgreSQL RLS policy
CREATE POLICY desk_isolation ON cases
    USING (desk_id = current_setting('app.current_desk_id'));
```

Each request includes desk ID:
```
X-Desk-Id: DESK001
X-User-Id: USR001
```

The application sets the session variable:
```sql
SET app.current_desk_id = 'DESK001';
```

Now Desk 001 queries only see their own cases.

---

## Queueing & Backpressure

### Problem
Millions of events arriving. System must not crash.

### Solution: Message Queue

```
[Source Files] → [SQS/RabbitMQ] → [Import Workers] → [Database]
```

- Import workers process one file at a time
- Backpressure: if queue > 10000, reject new imports with 503
- Retry: failed imports go to dead letter queue after 3 attempts

---

## Indexes for Performance

```sql
-- Critical indexes
CREATE INDEX idx_cases_client_id ON cases(client_id);
CREATE INDEX idx_cases_severity ON cases(severity);
CREATE INDEX idx_cases_state ON cases(state);
CREATE INDEX idx_cases_created_at ON cases(created_at);
CREATE INDEX idx_import_history_source ON import_history(source_name);
CREATE INDEX idx_case_notes_case_id ON case_notes(case_id);
CREATE INDEX idx_source_versions_source ON source_versions(source_name);

-- Composite index for filtered queries
CREATE INDEX idx_cases_filters ON cases(client_id, severity, state);
```

---

## Source Retention Policy

| Data Type | Retention | Reason |
|-----------|-----------|--------|
| Raw source files | 7 years | Regulatory requirement |
| Import history | 7 years | Audit trail |
| Case notes | Forever | Legal evidence |
| Cases | 7 years | Compliance |
| Metrics | 90 days | Operational |

---

## Failure Isolation

### Database Failure
- Primary goes down → Replica promoted automatically
- App nodes retry with exponential backoff
- Import queue pauses, resumes after failover

### Import Worker Failure
- Worker crashes → SQS message returns to queue
- Another worker picks it up
- Idempotent: same file = same import

### Memory Pressure
- Monitor JVM heap usage
- Alert at 80% usage
- Auto-restart at 90% usage

---

## Hardware Sizing (50 Desks)

| Component | Specification |
|-----------|---------------|
| App Nodes | 3x 8-core, 16GB RAM |
| Database | 1x 16-core, 64GB RAM, SSD |
| Database Replica | 1x 8-core, 32GB RAM |
| Load Balancer | nginx or AWS ALB |
| Message Queue | SQS or RabbitMQ cluster |

---

## Estimated Capacity

| Metric | Value |
|--------|-------|
| Concurrent users | 500 (50 desks × 10 users each) |
| Cases per day | ~1000 |
| Events per day | ~100,000 |
| Queries per second | ~500 |
| p95 latency target | <300ms |

---

## Monitoring & Alerting

| Metric | Alert Threshold |
|--------|-----------------|
| Import failure rate | >5% |
| Query p95 latency | >300ms |
| Database connections | >80% |
| Queue depth | >10,000 |
| Error rate | >1% |

---

## Migration Path from Demo to Production

1. Replace SQLite with PostgreSQL
2. Add connection pooling (HikariCP)
3. Enable Redis for session cache
4. Deploy behind load balancer
5. Enable HTTPS
6. Set up monitoring (Prometheus/Grafana)
7. Configure automated backups
8. Load test with production data volumes
