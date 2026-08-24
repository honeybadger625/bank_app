# Banking System

A banking application with a Spring Boot REST backend, a React dashboard, SQLite persistence, and transaction simulation tools.

## Current Application

The primary application consists of:

- **Backend:** Spring Boot 3.2.0 REST API in `backend/`
- **Frontend:** React 18 and Vite in `frontend/`
- **Database:** SQLite at `backend/db/banking.db`
- **Schema management:** Flyway migrations in `backend/src/main/resources/db/migration/`
- **Simulation:** Embedded simulator in the backend and a standalone simulator in `simulator/`
- **Containers:** Dockerfiles for the backend and frontend, orchestrated by `docker-compose.yml`

The backend targets **Java 25** and uses Maven. The frontend requires **Node.js 18 or newer** and npm.

## Features

- Create checking and savings accounts
- View account balances and account details
- Deposit funds and withdraw available funds
- View recent transaction history
- Start an embedded transaction simulation from the dashboard or API
- Poll an embedded simulation run for its status
- Ingest transaction events from an external simulator
- Check backend health

Authentication, JWT authorization, Postgres persistence, and production deployment automation are not currently implemented.

## Start With Docker Compose

Prerequisites:

- Docker Desktop or Docker Engine with the Compose plugin

From the repository root, build and start both services:

```bash
docker compose up --build
```

Open the application at:

- Frontend dashboard: http://localhost:3000
- Backend API: http://localhost:8080
- Health check: http://localhost:8080/health

The SQLite database is persisted through the `backend/db` volume. Stop the services with:

```bash
docker compose down
```

## Local Development

### Backend

Install Java 25 and Maven, then run these commands from `backend/`:

```bash
mvn clean package
mvn spring-boot:run
```

The backend listens on port `8080`. The Docker configuration stores the database at `/app/db/banking.db`. When running outside Docker, set a writable SQLite path if `/app/db` is not available:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:sqlite:./db/banking.db"
```

### Frontend

Install Node.js 18 or newer, then run these commands from `frontend/`:

```bash
npm install
npm run dev
```

The Vite development server normally runs on port `5173`. The frontend uses `VITE_API_BASE` when supplied; otherwise it calls `http://localhost:8080`.

For a production-style frontend build:

```bash
npm run build
npm run preview
```

The Docker image builds the frontend and serves its `dist/` directory with Nginx on container port `80`, mapped to host port `3000` by Compose.

## API

All request bodies use JSON.

### Health

```bash
curl http://localhost:8080/health
```

Returns `OK` with HTTP 200 when the backend is running.

### Accounts

List accounts:

```bash
curl http://localhost:8080/accounts
```

Create an account:

```bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{"ownerName":"Alex Morgan","accountType":"CHECKING"}'
```

Supported account types are `CHECKING` and `SAVINGS`. The response contains the generated account `id`. Use that value in the following requests:

```bash
curl http://localhost:8080/accounts/{id}

curl -X POST http://localhost:8080/accounts/{id}/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount":100.00}'

curl -X POST http://localhost:8080/accounts/{id}/withdraw \
  -H "Content-Type: application/json" \
  -d '{"amount":25.00}'
```

Deposits and withdrawals return HTTP 202 when accepted. Withdrawals fail when the amount is not positive or exceeds the available balance.

### Transactions

List the most recent transactions, optionally filtered by account:

```bash
curl "http://localhost:8080/transactions?limit=50"
curl "http://localhost:8080/transactions?account={id}&limit=50"
```

The API limits the requested result size to between 1 and 200 records.

### Embedded Simulator

Start a background simulation:

```bash
curl -X POST http://localhost:8080/simulate/start \
  -H "Content-Type: application/json" \
  -d '{"transactions":100,"minDelayMs":10,"maxDelayMs":50}'
```

The response contains a `runId` and HTTP 202. Poll its status with:

```bash
curl http://localhost:8080/simulate/status/{runId}
```

The dashboard polls this endpoint while a run is active and refreshes accounts and transactions when it finishes.

### External Event Ingestion

Post an event to the backend transaction table:

```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{"runId":"external-1","accountId":"{id}","type":"DEPOSIT","amount":50,"status":"SUCCESS"}'
```

## Standalone Simulator

The standalone simulator in `simulator/` is separate from the Spring Boot application. Compile the Java sources from the repository root:

```bash
javac -d bin @sources.txt
```

Run with CSV output:

```bash
java -cp bin simulator.SimulatorMain --transactions 100
```

Available simulator options include `--users`, `--transactions`, `--minDelayMs`, `--maxDelayMs`, `--api`, `--db`, and `--persistBank`.

The `--api` mode posts events to an API event endpoint. The `--db` mode requires the SQLite JDBC driver on the classpath:

```bash
java -cp "bin;lib/sqlite-jdbc.jar" simulator.SimulatorMain --db simulation_output/sim.db
```

Without `--api` or `--db`, events are written to `simulation_output/simulation_transactions.csv`.

## Legacy Java Applications

The original Java Swing banking application remains in `src/` and is independent of the Spring Boot and React application. The repository also contains a lightweight HTTP server in `web/`. These are legacy development tools and are not required to run the current web dashboard.

The lightweight server can be compiled with the source list and started with:

```bash
javac -d bin @sources.txt
java -cp bin web.WebServerMain
```

It accepts events at `POST /events` and starts an embedded simulation at `POST /simulate/start`. Use `--port` to change its port and `--db` to write through the SQLite event writer.

## Project Structure

```text
backend/       Spring Boot API, Flyway migrations, and SQLite database directory
frontend/      React/Vite dashboard
simulator/     Standalone simulator and event writers
src/           Legacy Swing banking application
web/           Legacy lightweight HTTP server
docker-compose.yml
```

## Limitations

- The local configuration uses SQLite rather than Postgres.
- API endpoints currently do not require authentication.
- The frontend expects the backend at `http://localhost:8080` unless `VITE_API_BASE` is set.
- The Docker Compose setup is intended for local development.