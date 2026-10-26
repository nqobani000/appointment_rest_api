# Appointment REST API

Spring Boot (Java 17) modular monolith backed by PostgreSQL, with Flyway migrations.

## Running with Docker

Prerequisites: [Docker](https://docs.docker.com/get-docker/) with Compose v2.

1. Create your env file and set a real password:
   ```bash
   cp .env.example .env
   ```
2. Build and start the app and database:
   ```bash
   docker compose up --build
   ```
   The `db` service (PostgreSQL 16) has a healthcheck; the `app` service only starts once the database is healthy, after which Flyway applies migrations automatically.
3. The API is available at <http://localhost:8080>.

Useful commands:

```bash
docker compose up -d --build   # run in the background
docker compose logs -f app     # follow app logs
docker compose down            # stop containers (data is kept)
docker compose down -v         # stop and delete the database volume
```

### Configuration

| Variable      | Purpose                              |
|---------------|--------------------------------------|
| `DB_USERNAME` | Postgres user (set in `.env`)        |
| `DB_PASSWORD` | Postgres password (set in `.env`)    |

The database name is `bank_appointments`. `DB_HOST` is set to `db` by Compose.

## Running locally without Docker

Start a Postgres instance with a `bank_appointments` database, then (the instance must have the **PostGIS** extension installed, otherwise migration V2 fails with `extension "postgis" is not available`; the simplest option is `docker compose up -d db`, which publishes port 5432, with `DB_HOST=localhost`):

```bash
export DB_HOST=localhost DB_USERNAME=<user> DB_PASSWORD=<password>
./gradlew bootRun
```

## CI

- `ci-build.yml`: builds and tests against a Postgres service container.
- `ci-docker.yml`: builds the Docker image on PRs and pushes it to Docker Hub on `master` (requires `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` secrets).
