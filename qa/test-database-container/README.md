# Database Migration Test Runner

This repository contains a helper script (`test.sh`) for running **database migration tests** against multiple database backends inside Docker containers.  
It automates container lifecycle management, waits for the database to be ready, runs a Maven build with the appropriate profile, and cleans up resources afterwards.

---

## Supported Databases

The script supports the following database types:

- **MySQL**
- **MariaDB**
- **PostgreSQL**
- **Oracle**
- **SQL Server**

---

## How It Works

When you run `./test.sh <db_type>`, the script:

1. **Validates input** – ensures the database type is one of the supported ones.  
2. **Exports configuration variables** – sets connection details (username, password, port, database name) as environment variables used by the application/tests.  
3. **Starts the database container** – via `docker compose -f ./docker-compose.yaml up -d <db_type>`.  
4. **Waits for readiness** – polls the database with simple queries until it responds (up to 30 retries, 2s apart).  
5. **Runs migrations/tests** – executes Maven build with appropriate profiles:  
   ```bash
   ../../mvnw -f ../pom.xml clean install -Prolling-update,mt-<db_type>
   ```
6. **Handles SQL Server specifics** – creates the test database if necessary.  
7. **Cleans up containers** – brings down any Docker services started for the run.  
8. **Exits with Maven’s result code** – ensuring CI/CD pipelines reflect success/failure correctly.

---

## Requirements

Before using this script, make sure you have:

- **Docker & Docker Compose** installed and running.  
- **Java & Maven Wrapper (`mvnw`)** available in the project (expected at `../../mvnw`).  
- **docker-compose.yaml** file with services named after the supported DB types (`mysql`, `mariadb`, `postgres`, `oracle`, `sqlserver`).  
- Sufficient permissions to run Docker commands.

---

## Usage

```bash
./test.sh <db_type>
```

Where `<db_type>` is one of:

- `mysql`
- `mariadb`
- `postgres`
- `oracle`
- `sqlserver`

### Example

```bash
./test.sh postgres
```

This will:

- Start the `postgres` service from `docker-compose.yaml`  
- Wait until it is ready to accept connections  
- Run migration tests with Maven using the `mt-postgres` profile  
- Tear down the container after the test  

---

## Environment Variables Used

The script automatically sets and exports connection variables depending on the chosen database:

| Database   | Username   | Password         | Port   | Database   |
|------------|-----------|------------------|--------|------------|
| MySQL      | root      | 123456           | 33060  | operaton   |
| MariaDB    | root      | 123456           | 33070  | operaton   |
| PostgreSQL | root      | 123456           | 54320  | operaton   |
| Oracle     | SYSTEM    | 123456           | 15210  | operaton   |
| SQL Server | sa        | P@ssw0rd123ABC   | 14330  | operaton   |

These are injected into the running tests automatically.

---

## Troubleshooting

- **Database not ready in time** – If the DB fails to initialize within ~1 minute, the script will abort. Check your `docker-compose.yaml` health checks or resource limits.  
- **Wrong service names** – The container names must match the DB type argument (`mysql`, `postgres`, etc.).  
- **Ports already in use** – Ensure the host ports (e.g., 54320 for Postgres) are available.  

---

✅ With this script, you can reliably test database migrations across multiple backends in a repeatable, containerized environment.
