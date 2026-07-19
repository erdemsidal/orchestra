# Job Orchestrator

An asynchronous job processing service, built in three phases: clean architecture, then scalability, then distributed and event-driven. Java 21, Spring Boot 3, PostgreSQL.

You submit a job, you get an ID back immediately, workers run it in the background, and you poll the ID to see how it went.

**Current status: Phase 1 is done.** The system is synchronous and single-service right now, and that's deliberate. Phase 3 is where it becomes distributed, and the contrast between the two is the whole point. See the [roadmap](#roadmap).

---

## Why this exists

I wanted to prove I can design an async job-processing system and defend every decision in it, not just get it working.

So this repo optimizes for something unusual: the reasoning is the deliverable. Every non-obvious choice has an [ADR](docs/adr/) explaining what I picked, what I gave up, and when I'd change my mind. The code is small. The thinking is the point.

It's not a product. Nobody's going to use it to send emails.

---

## How it works

A job moves through four states, and it can't cheat:

```
PENDING ──► RUNNING ──► DONE
                 └────► FAILED
```

Invalid transitions are impossible, not discouraged. `PENDING -> DONE` throws. `DONE -> RUNNING` throws. The `Job` object refuses to be put into an inconsistent state, so no caller can bypass the rules — not the controller, not a future worker, not me at 2am.

There is deliberately no endpoint to set a job's status. More on that below.

---

## Architecture

Hexagonal (ports and adapters). Three layers, dependencies point inward:

```
infrastructure/          Spring, JPA and HTTP live here
  web/                   JobController, request/response DTOs
  persistence/           JobEntity, JpaJobRepositoryAdapter
  JobBeanConfig          wires use cases into Spring
        |
        | depends on
        v
application/             Pure Java, no framework
  JobRepository          the port — an interface I own
  CreateJobService
  GetJobService
        |
        | depends on
        v
domain/                  Pure Java, no framework
  Job, JobStatus         the rules live here
```

`domain/` and `application/` contain zero Spring or JPA imports. The test for whether that's real: if I deleted Spring Boot, could I still test my business rules? Yes — 11 of the tests here run with no database, no Spring context and no Docker, in 0.14 seconds total.

Everything about jobs lives under `job/`. Package-by-feature, not `controller/` + `service/` + `entity/`. When Phase 3 adds a worker, it gets its own folder instead of smearing across five existing ones.

---

## Tech stack

| Layer | Choice | Why |
|-------|--------|-----|
| Language | Java 21 (LTS) | Records, virtual threads, pattern matching |
| Framework | Spring Boot 3.4 | Jakarta EE 10, mature ecosystem |
| Database | PostgreSQL 16 | Boring is good |
| Migrations | Flyway | Versioned SQL in git. `ddl-auto` is `validate`, so Hibernate never touches the schema |
| Build | Maven (wrapper) | `./mvnw` works anywhere, no global install |
| API docs | springdoc-openapi | Swagger UI at `/swagger-ui.html` |
| Testing | JUnit 5, AssertJ, Testcontainers | Real Postgres in tests, not H2 |
| Cache | Redis 7 | Wired but dormant, Phase 2 will use it |
| Containers | Docker Compose | Postgres and Redis in one command |

---

## Quick start

You need Java 21 and Docker.

```bash
git clone https://github.com/erdemsidal/orchestra.git
cd orchestra

# Postgres + Redis
docker compose up -d

# Flyway creates the schema on startup
./mvnw spring-boot:run
```

The app runs on `http://localhost:8080`. Swagger UI is at `http://localhost:8080/swagger-ui.html`.

Try it:

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{"type":"send-email"}'
# 201 {"id":"b4f43279-...","type":"send-email","status":"DONE"}
# The job runs synchronously inside the request (Phase 1), so the response
# already reflects the final state — DONE, or FAILED if the work threw.

curl http://localhost:8080/api/jobs/b4f43279-...
# 200 {"id":"b4f43279-...","type":"send-email","status":"DONE"}
```

---

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/jobs` | Create a job and run it synchronously. Returns 201 with the job ID and its final status (`DONE` or `FAILED`) |
| `GET` | `/api/jobs/{id}` | Get a job's current status. 404 if it doesn't exist |
| `GET` | `/api/health` | Health check |
| `GET` | `/actuator/health` | Actuator health |

Two endpoints. No `PUT`, no `DELETE`. That's not laziness, see below.

---

## Testing

```bash
./mvnw test
```

| Type | Count | Time | What it answers |
|------|-------|------|-----------------|
| Unit (domain + application) | 11 | 0.14s | Are my rules correct? |
| Integration (Testcontainers) | 4 | 21s | Are the pieces wired correctly? |

A 150x difference, and that's exactly why the pyramid has a wide base. If every rule needed a real Postgres, the suite would take minutes and nobody would run it.

The integration tests start a real PostgreSQL container through Testcontainers, so there's no manual `docker compose up` and CI behaves the same as my laptop. Not H2: it doesn't faithfully emulate Postgres's UUID type, indexes or SQL dialect, and "passed in tests, exploded in prod" starts right there.

---

## Performance: caching `GET /jobs/{id}`

Phase 2 rule: measure, change, measure again. Same k6 test both times — 50 virtual
users hammering `GET /jobs/{id}` for 30s. The only change between the two columns
is a `@Cacheable` annotation backed by Redis (5-minute TTL).

| Metric | No cache | With cache | Change |
|--------|----------|------------|--------|
| p50 | 11.74ms | 3.90ms | −67% |
| p95 | 19.51ms | 6.63ms | −66% (≈3× faster) |
| p99 | 29.19ms | 10.96ms | −62% |
| Throughput | 3,867 req/s | 11,192 req/s | ≈2.9× |

The endpoint is a single indexed primary-key read, so in isolation it's already
fast (~5ms). The win shows up **under load**: without the cache, concurrent
requests queue for one of Hikari's 20 DB connections and p95 climbs; the cache
serves them from Redis with no pool contention.

No cache invalidation is needed here, and that's deliberate — see
[ADR 0004](docs/adr/0004-cache-stratejisi.md). Because POST runs synchronously, a
job is already terminal (`DONE`/`FAILED`) by the time it can be fetched, so the
cached value never goes stale. Phase 3 makes jobs async — and that's exactly when
invalidation becomes a real problem.

Reproduce it: `docker compose up -d && ./mvnw spring-boot:run`, then
`k6 run load-tests/get-job.js` (comment out the `@Cacheable` for the baseline).

## Why these decisions?

This is the section I wish more repos had. Full reasoning lives in [`docs/adr/`](docs/adr/).

### Why a repository port instead of just using `JpaRepository`?

Not so I can swap Postgres for MongoDB. Nobody does that, and saying it in an interview should earn you a raised eyebrow.

The real reason is testing. `CreateJobService` has to save something. If it depends on Spring Data directly, testing it needs a real database: Docker, Testcontainers, several seconds, for every test. If it depends on `JobRepository` — an interface I own — I hand it a `HashMap`-backed fake and the test runs in a millisecond.

The second reason: `JpaRepository` is Spring's interface, not mine. Extend it and you inherit its whole world — `flush()`, lazy loading, detached entities, `LazyInitializationException`. My port has two methods and speaks in domain objects.

### Why is `JobEntity` a separate class from `Job`?

Because JPA's requirements would destroy the domain's guarantees. JPA needs a no-arg constructor, but `Job`'s constructor is exactly where "a new job is always PENDING" is enforced. JPA needs setters, but `Job` deliberately has none, because status only moves through `start()`, `markDone()` and `markFailed()`.

Merge them and you strip every protection to keep Hibernate happy. Kept apart, `JobEntity` obeys JPA and `Job` obeys the business. An adapter translates between them.

### Why no `PUT /jobs/{id}` or `DELETE`?

This is the one I'd defend hardest.

If I exposed `updateJob(status)`, a client could do this:

```json
PUT /api/jobs/abc-123
{ "status": "DONE" }
```

A PENDING job, marked done, having never run. Every state rule I wrote and every test protecting them, bypassed in one request.

Status isn't a field you set. It's the consequence of something happening. The user creates a job and reads it; the system advances it through the domain rules.

And `DELETE`? A job is a record — "this ran last Tuesday and failed". You don't delete history.

This is the difference between CRUD thinking ("I have a table, so I need four operations") and use-case thinking ("what can a user actually do here?"). The answer is two things, so there are two endpoints.

### Why return a DTO instead of the entity?

Return `JobEntity` and your database schema quietly becomes your API contract. Add a column, break every client. `JobResponse` makes the contract explicit: what's written there is what ships.

### Why is the state machine inside `Job` and not its own class?

Four states and an almost-linear flow. A dedicated `JobStateMachine` would be over-engineering today, and moving the rules out later is a small refactor if the states multiply. Start simple where changing your mind is cheap. The full reasoning, and the trigger for revisiting it, is in [ADR 0001](docs/adr/0001-durum-yonetimi.md).

### Why no `@Service` on the use cases?

So `application/` stays framework-free. `JobBeanConfig` registers them as beans instead. It costs one extra class and buys a layer with zero Spring imports, plus a single visible place where wiring happens.

### Why `VARCHAR` for status instead of an ordinal?

Store `0` and the day someone reorders the enum, every historical row silently means something else. `PENDING` is readable in `psql` and doesn't rot.

---

## Roadmap

**Phase 1 — Clean architecture. Done.**
Synchronous, single service. Hexagonal layers, Postgres and Flyway, unit and integration tests, Docker Compose.

**Phase 2 — Scalability.**
Redis caching, rate limiting, load testing with k6, metrics. The deliverable isn't "I added a cache", it's a measured before/after p95 latency table.

**Phase 3 — Distributed and event-driven.**
Split the worker out and put a queue (SQS) between them. Dead-letter queue, retry with exponential backoff, idempotency, eventual consistency. This is where Phase 1's synchronous design gets deliberately broken and replaced.

---

## Not included yet

Being honest about the gaps:

- **CI.** Deferred. The tests all pass locally (`./mvnw test`), but wiring up a pipeline that runs them on GitHub — including the Testcontainers integration tests, which need Docker — is a task for later. I'd rather no pipeline than a red one that lies about the state of the code.
- Authentication. Deliberately stripped — Phase 1 is about architecture, and auth would have been scope creep.
- Metrics, caching, rate limiting. Phase 2.
- The queue, workers, retries, DLQ. Phase 3.

---

## License

MIT.

## About

Built by Erdem Sidal as a study in async job-processing architecture, and in being able to explain it.
