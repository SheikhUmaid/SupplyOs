# SupplyChain OS: Backend

Spring Boot modular monolith with MySQL. The design is in [`docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md).

## Setup

Requires MySQL 8 and a JDK whose `javac` is on `PATH` (21 or newer). Maven downloads itself through `./mvnw`.

### Without root access to MySQL

`./local-db.sh start` runs a private MySQL on port 3307, creating its databases and user on first use. It needs no sudo and doesn't touch any other MySQL on the machine. Set `DB_PASSWORD`, `DB_URL` and `DB_TEST_URL` in `.env` first (see `.env.example`). Also available: `stop` and `status`.

### With an installed MySQL

Create the databases and user once:

```sql
CREATE DATABASE supplychain CHARACTER SET utf8mb4;
CREATE DATABASE supplychain_test CHARACTER SET utf8mb4;
CREATE USER 'supplychain'@'localhost' IDENTIFIED BY 'choose-a-password';
GRANT ALL PRIVILEGES ON supplychain.* TO 'supplychain'@'localhost';
GRANT ALL PRIVILEGES ON supplychain_test.* TO 'supplychain'@'localhost';
```

Copy `.env.example` to `.env` and fill it in. `.env` is ignored by git. The application refuses to start without `DB_PASSWORD` and a `JWT_SECRET` of at least 32 bytes.

## Commands

| Command | Purpose |
|---|---|
| `./mvnw spring-boot:run` | Start on port 8081. Flyway creates the schema on first start. |
| `./mvnw test` | Run all tests against the `supplychain_test` database |

API documentation is at <http://localhost:8081/swagger-ui.html> in the default `dev` profile, and is switched off in other profiles.
