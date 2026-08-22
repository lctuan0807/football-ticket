# Football Ticket

A ticket reservation backend for football matches — manage matches and their ticket types, and reserve/confirm/cancel tickets with concurrency-safe stock handling (verified under load with 150 concurrent requests against 30 units of stock, no overselling).

## Tech stack

- **Java 21**, **Spring Boot 4.1.0** (Maven, `./mvnw`)
- Spring Web, Spring Data JPA, Spring Security, JWT (`jjwt` 0.12.6)
- **PostgreSQL 16** — driver + Hibernate `ddl-auto: update` (no Flyway/Liquibase; schema is auto-managed)
- **Redis 7** + **Redisson** 4.6.1 — caching and distributed locking for reservations
- **Kafka** (KRaft mode, no Zookeeper) — messaging broker, via `spring-boot-starter-kafka`
- Lombok, ModelMapper, Hibernate Validator
- **API docs**: springdoc-openapi (Swagger UI) 3.1.0
- **Testing**: JUnit 5, Mockito, MockMvc (`@WebMvcTest`), AssertJ, `spring-security-test`, plus a full `@SpringBootTest` concurrency test
- **Load testing**: [k6](https://k6.io) scripts in `loadtest/`

## Prerequisites

- JDK 21
- Docker (for Postgres + Redis)
- Optional: [k6](https://k6.io) (`brew install k6`) and `redis-cli`, for the load-test scripts

## Getting started

1. **Start infrastructure** (Postgres on `5433`, Redis on `6389`):
   ```bash
   ./environment/start.sh
   ```
   This runs `docker-compose -f environment/docker-compose-dev.yml up -d`, creating:
   - Postgres 16, db `football_ticket`, user/pass `postgres`/`postgres`, seeded via `environment/postgres/data_init.sql` with 50 sample matches (Premier League, La Liga, Serie A, Bundesliga, Ligue 1, Champions League, ...) and their ticket types.
   - Redis 7.
   - Kafka (single-node, KRaft mode) on `localhost:9094`, plus a [Kafka UI](https://github.com/provectus/kafka-ui) at `http://localhost:8090` for browsing topics/messages.

2. **Configure environment variables** (optional for local dev — sensible defaults are baked into `application.yaml`):
   ```
   JWT_SECRET=<your-secret>
   JWT_EXPIRATION_MS=3600000
   JWT_REFRESH_EXPIRATION_MS=604800000
   ```
   See `environment/.env.example`.

3. **Run the app**:
   ```bash
   ./mvnw spring-boot:run
   ```
   The API serves on `http://localhost:8080` (no `server.port` override).

## Project structure

| Package | Contents |
|---|---|
| `common` | `ApiResponse` envelope, `GlobalExceptionHandler` |
| `config` | Redis/Redisson config, ModelMapper config, reservation schema init |
| `controller` | REST controllers (Auth, Match, TicketType, Reservation) |
| `dto` | Request/response DTOs, grouped per resource |
| `entity` | JPA entities (`MatchEntity`, `TicketTypeEntity`, `ReservationEntity`, `UserEntity`) |
| `enums` | `MatchStatusEnum`, `ReservationStatusEnum` |
| `exceptions` | Domain exceptions mapped to HTTP status codes |
| `repository` | Spring Data JPA repositories |
| `security` | JWT filter/service, Spring Security config |
| `service` | Business logic (+ `service/impl`, `service/cache`) |

## Authentication

JWT infrastructure is wired up (`POST /api/v1/auth/register`, `/login`, `/logout`, and a `JwtAuthenticationFilter`), but **auth is currently not enforced**: `SecurityConfig` permits all `/api/v1/**` requests without a token. Treat the login flow as available for testing/integration, not as an active gate yet.

## Messaging (Kafka)

A Kafka broker is available at `localhost:9094` (`spring-boot-starter-kafka`, config in `application.yaml`). `KafkaTopicConfig` declares a `reservation-place-topic` (3 partitions) on startup, but no producer or consumer is wired up yet — it's infrastructure ready for use, not an active event flow.

Inspect the broker via Kafka UI (`http://localhost:8090`) or its CLI (run inside the container, so it uses the internal listener port `9092` rather than the host-mapped `9094`):

```bash
docker exec football-ticket-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list
```

## API response format

All success responses are wrapped:
```json
{ "code": 200, "message": "Success", "data": { ... } }
```

Errors use the same envelope with `data: null`, mapped by a global exception handler:

| Status | Thrown by |
|---|---|
| 400 | Bean validation failures (invalid request body) |
| 401 | `InvalidCredentialsException` |
| 404 | `ResourceNotFoundException` |
| 409 | `ResourceAlreadyExistsException`, `InsufficientTicketException`, `InvalidReservationStateException`, `ReservationCreationFailedException` |

## API endpoints

All endpoints are under `/api/v1`. Interactive docs are available via Swagger UI at `http://localhost:8080/swagger-ui.html` (raw OpenAPI spec at `/v3/api-docs`) once the app is running — this section plus the curl examples below are also a quick reference.

### Auth — `/api/v1/auth`

| Method | Path | Body | Description |
|---|---|---|---|
| POST | `/register` | `{ username, password }` (password min 8 chars) | Register a new user |
| POST | `/login` | `{ username, password }` | Authenticate, returns `{ accessToken, tokenType, expiresIn, refreshToken }` |
| POST | `/refresh` | `{ refreshToken }` | Rotates the refresh token and issues a new access token; the old refresh token is invalidated |
| POST | `/logout` | `{ refreshToken }` (optional; `Authorization: Bearer <token>` header optional) | Blacklists the access token and revokes the refresh token, if provided |

### Matches — `/api/v1/matches`

| Method | Path | Body / Params | Description |
|---|---|---|---|
| POST | `/` | `{ competition, stage, season, homeTeam, awayTeam, kickoffAt, stadium }` (`kickoffAt` format `yyyy-MM-dd HH:mm:ss`) | Create a match |
| GET | `/` | `page`, `size`, `sort`, optional `status` | List matches (paginated) |
| GET | `/{id}` | — | Get a match |
| PUT | `/{id}` | same fields as create | Update a match |
| DELETE | `/{id}` | — | Delete a match |

`status` values: `SCHEDULED`, `LIVE`, `FINISHED`, `POSTPONED`, `CANCELLED`.

### Ticket types — `/api/v1/matches/{matchId}/ticket-types`

| Method | Path | Body | Description |
|---|---|---|---|
| POST | `/` | `{ name, description?, price, quantity }` | Create a ticket type for a match |
| GET | `/` | — | List ticket types for a match |
| GET | `/{id}` | — | Get a ticket type |
| PUT | `/{id}` | same fields as create | Update a ticket type |
| DELETE | `/{id}` | — | Delete a ticket type |

### Reservations

| Method | Path | Body | Description |
|---|---|---|---|
| POST | `/api/v1/matches/{matchId}/ticket-types/{ticketTypeId}/reservations` | `{ userId, quantity }` | Create a reservation (deducts available stock) |
| GET | `/api/v1/reservations/{id}` | — | Get a reservation |
| PATCH | `/api/v1/reservations/{id}/confirm` | — | Confirm a pending reservation |
| PATCH | `/api/v1/reservations/{id}/cancel` | — | Cancel a reservation |

`status` values: `PENDING`, `CONFIRMED`, `CANCELLED`, `EXPIRED`.

## Testing

### Automated tests

```bash
./mvnw test
```

Covers:
- **Unit tests** — service layer logic with Mockito (e.g. `MatchServiceImplTest`, `ReservationServiceImplTest`).
- **Controller-slice tests** — `@WebMvcTest` + MockMvc + `jsonPath` (e.g. `MatchControllerTest`, `ReservationControllerTest`).
- **Concurrency test** — `ReservationConcurrencyTest` is a full `@SpringBootTest` that fires 150 concurrent reservation requests against 30 units of stock to verify no overselling. It needs a real Postgres and Redis running, so start infra first:
  ```bash
  ./environment/start.sh
  ./mvnw test
  ```

### Manual testing (Swagger UI or curl)

With the app running, open `http://localhost:8080/swagger-ui.html` to browse and try out every endpoint interactively (no Postman collection exists, so this is the easiest way to explore). Alternatively, use the seeded data directly via curl — match id `1` is Arsenal vs Manchester United (Premier League, Matchday 1):

```bash
# List matches
curl http://localhost:8080/api/v1/matches

# Get match 1
curl http://localhost:8080/api/v1/matches/1

# List ticket types for match 1
curl http://localhost:8080/api/v1/matches/1/ticket-types

# Create a reservation (ticketTypeId from the previous call, e.g. 1)
curl -X POST http://localhost:8080/api/v1/matches/1/ticket-types/1/reservations \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "quantity": 2}'

# Confirm a reservation (id from the response above)
curl -X PATCH http://localhost:8080/api/v1/reservations/1/confirm

# Cancel a reservation
curl -X PATCH http://localhost:8080/api/v1/reservations/1/cancel
```

### Load / chaos testing (k6)

Scripts in `loadtest/` simulate cache penetration and cache stampede scenarios against `GET /api/v1/matches/{matchId}/ticket-types/{id}`. They read the app's log output to count cache misses, so start the app with `tee` first:

```bash
./mvnw spring-boot:run | tee /tmp/footballticket-app.log
```

Then, in another terminal:

```bash
# Cache penetration (sustained lookups of ticket-type ids that never exist)
MATCH_ID=1 VUS=50 DURATION=30s ./loadtest/run-penetration-test.sh

# Cache stampede (deletes a Redis key for an existing ticket type, then fires concurrent requests)
TICKET_TYPE_ID=1 MATCH_ID=1 VUS=200 ./loadtest/run-stampede-test.sh
```

Both accept `BASE_URL` (default `http://localhost:8080`) and `LOG_FILE` (default `/tmp/footballticket-app.log`); the stampede script also accepts `REDIS_HOST`/`REDIS_PORT` (defaults `localhost`/`6389`). Requires `k6` and `redis-cli` on `PATH`.
