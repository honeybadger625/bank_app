Convert the existing Java Swing BankingSystem into a web-hosted application that supports user login, manual account setup and transactions, simulator-driven bulk transactions (embedded + external), and production deployment to a Red Hat Enterprise Linux 9 VM. Work from the repository root and deliver code, tests, Docker artifacts, and deployment instructions.

Goals

- Reuse existing domain logic (Bank, BankAccount, FileIO) via adapters so business rules remain unchanged.
- Provide a Spring Boot REST backend and a React SPA frontend.
- Support both embedded and external simulator modes; simulator must persist events to the same SQL store the API uses.
- Persist accounts and transaction events to SQL (Postgres for production; SQLite allowed for local dev).
- Provide production-ready deployment instructions for a Red Hat 9 VM (systemd and optional Docker).

Required stack (must use)

- Backend: Spring Boot (Java 17+), Spring Web, Spring Security (JWT), Spring Data (JDBC/JPA), Flyway for migrations.
- Frontend: React (Vite or CRA), Axios or fetch for API.
- DB: Postgres for production; SQLite for local/test.
- Simulator: Refactored simulator module to support `JdbcEventSink`, `ApiEventSink` and `CsvEventSink`.
- Build: Maven (multi-module) or Gradle multi-module (preferred Maven unless you specify otherwise).
- Container: Dockerfile for backend and frontend; `docker-compose.yml` for local dev (optional).

Concrete tasks (ordered)

1. Scaffold `backend/` Spring Boot module and `frontend/` React app in repo.
2. Implement `BankService` adapter that wraps `FileIO.bank` and is thread-safe (use `ReentrantReadWriteLock` or synchronized blocks).
3. Add authentication:
- `POST /auth/register`, `POST /auth/login` (JWT).
- Seed an admin user for simulator control.
4. Implement REST API endpoints:
- Accounts: `GET /accounts`, `POST /accounts`, `GET /accounts/{id}`, `POST /accounts/{id}/deposit`, `POST /accounts/{id}/withdraw`.
- Transactions: `GET /transactions?account=...&limit=..`
- Simulator control: `POST /simulate/start` (embedded), `GET /simulate/status/{runId}`
- Event ingest: `POST /events` (for external simulator)
- Health: `GET /health`
5. Refactor simulator:
- Make `TransactionGenerator` depend on `BankService` + an `EventSink` interface.
- Implement `JdbcEventSink` (writes to `simulation_transactions`), `ApiEventSink` (POSTs to `POST /events`), `CsvEventSink`.
- Provide `EmbeddedSimulatorService` (Spring `@Service`) that runs generator asynchronously.
- Keep CLI mode for external simulator: `simulator.SimulatorMain --api <url>` or `--db <path>`.
6. Persistence & migrations:
- Flyway migrations to create `users`, `simulation_transactions`, and optionally `accounts` (if migrating).
- `simulation_transactions` schema with columns matching existing CSV fields.
7. Frontend:
- Pages: Login/Register, Dashboard, Accounts, Account detail (transactions + manual entry), Simulator control page (start/monitor runs).
- Use JWT auth; store token securely (localStorage with guidance).
8. Tests:
- Unit tests for `BankService`, repository.
- Integration tests that start Spring context, run embedded simulation, assert DB rows and balance changes.
9. Packaging:
- Dockerfile for backend (fat JAR), Dockerfile for frontend (static build).
- `docker-compose.yml` for dev (backend + Postgres + frontend).
10. Deployment on Red Hat 9 (deliverables & scripts):
- Provide two deployment options and exact steps:
A. Systemd + JAR (recommended lightweight):
- Install OpenJDK 17: `sudo dnf install java-17-openjdk-devel`
- Create user `banking` and place JAR at `/opt/banking/app.jar`
- Create systemd unit `/etc/systemd/system/banking.service` that runs `ExecStart=/usr/bin/java -jar /opt/banking/app.jar --spring.datasource.url=jdbc:postgresql://db-host:5432/banking`
- Enable and start service: `sudo systemctl enable --now banking`
- Configure `firewall-cmd --add-port=8080/tcp --permanent; firewall-cmd --reload`
- SELinux: provide guidance to set file contexts or provide an SELinux policy module if necessary (or recommend running under Docker to avoid policy changes).
- Configure Nginx as reverse proxy (SSL): install nginx, create proxy config to forward 80/443 -> 8080, use certbot for TLS.
B. Docker (recommended for easier portability):
- Provide `Dockerfile` and `docker-compose.prod.yml` with Postgres and backend; show commands to build and run.
- Ensure Docker is installed on RHEL9 and SELinux-aware options used (e.g., `:z` mounts as needed).
- Provide an Ansible playbook (optional) or step-by-step commands to provision the VM: create `banking` user, install Java or Docker, install Postgres (or use managed DB), open firewall ports, configure Nginx, and deploy JAR/container.
11. Operational notes:
- Run as non-root user.
- Log to stdout for Docker; or configure `journalctl` when using systemd.
- Health check endpoints and graceful shutdown that flushes simulator and optionally calls `FileIO.Write()` if `persistBank` is enabled.
- Secret management: read DB credentials and JWT signing key from environment variables or systemd `EnvironmentFile`.
- Backups & ETL: recommend using Postgres logical backups or nightly snapshots. If still using SQLite in prod, always snapshot via backup API.
12. Documentation:
- Update `README.md` with development, build, test, and production deployment steps to RHEL9.
- Provide example `curl` commands and example systemd unit and `docker-compose` commands.

Acceptance criteria (automated + manual)

- `GET /health` returns 200.
- Auth flow works: register -> login -> get JWT -> call protected endpoint.
- Manual actions produce transactions:
- `POST /accounts` then `POST /accounts/{id}/deposit` creates a row in `simulation_transactions`.
- Embedded simulator:
- `POST /simulate/start` returns run id and 202; background job runs and inserts events into `simulation_transactions`.
- External simulator:
- `simulator.SimulatorMain --api http://host:8080` posts events and server persists them.
- Docker/systemd deployment instructions validated on RHEL9 VM (include commands to start, view logs, and open firewall).
- Deliverables committed and runnable locally and deployable to Red Hat 9 with the provided steps.

Deliverables (must commit to repo)

- `backend/` (Spring Boot) with controllers, `BankService` adapter, `EmbeddedSimulatorService`, Flyway migrations, tests, and `Dockerfile`.
- `frontend/` (React) with authentication and UI pages.
- `simulator/` updated with `JdbcEventSink`, `ApiEventSink`, CLI flags, and tests.
- `docker-compose.yml` (dev) and `docker-compose.prod.yml` or systemd example for production.
- `README.md` with dev & RHEL9 deploy instructions and example `systemd` unit.
- Optional: Ansible playbook to provision RHEL9 VM.

Run/build commands to include in README (exact)

- Build backend:
- `./mvnw -pl backend clean package`
- Build frontend:
- `cd frontend && npm ci && npm run build`
- Run locally (dev):
- `docker-compose up --build`
- Run on RHEL9 (systemd, basic)
- Copy `backend/target/backend.jar` to `/opt/banking/app.jar`.
- Create `/etc/systemd/system/banking.service`:
- ExecStart example: `/usr/bin/java -jar /opt/banking/app.jar --spring.datasource.url=jdbc:postgresql://localhost:5432/banking`
- `sudo systemctl daemon-reload && sudo systemctl enable --now banking`
- `sudo firewall-cmd --add-port=8080/tcp --permanent && sudo firewall-cmd --reload`
- Configure Nginx as reverse proxy and obtain TLS cert with certbot.
