# Website

## Local Backend Setup

This workflow runs both PostgreSQL and the Spring Boot backend through Docker Compose.
You do not need to install PostgreSQL locally or run `mvn spring-boot:run` manually.

### Requirements

- Docker Desktop with Docker Compose
- Windows PowerShell

### 1. Create Local Environment File

```powershell
Copy-Item .env.example .env
```

Edit `.env` for local development. The default local database values are:

```text
DB_NAME=bautruc_ecommerce
DB_USERNAME=bautruc
DB_PASSWORD=bautruc_local
SPRING_PROFILES_ACTIVE=local
ALLOWED_ORIGINS=http://127.0.0.1:3000
```

For backend running inside Docker Compose, the backend container uses:

```text
jdbc:postgresql://postgres:5432/bautruc_ecommerce
```

Do not use `localhost:5433` from inside the backend container. `localhost:5433` is only for tools running on the host.

Set a real local JWT secret for secured flows:

```text
JWT_SECRET_BASE64=<replace-with-local-base64-secret>
```

JWT_SECRET_BASE64 is required for local startup and must be provided through the local .env file. Do not commit the .env file.

Google Login uses Google Identity Services ID token verification. Use the same OAuth Client ID for:

```text
Backend:  GOOGLE_CLIENT_ID
Frontend: VITE_GOOGLE_CLIENT_ID
```

No Google Client Secret is required for this flow.

### 2. Start Local Backend Stack

Normal startup:

```powershell
docker compose up -d
```

After source changes:

```powershell
docker compose up -d --build
```

Check service status:

```powershell
docker compose ps
```

Follow backend logs:

```powershell
docker compose logs -f backend
```

Expected startup behavior:

```text
PostgreSQL becomes healthy
Backend waits for PostgreSQL health
Flyway migrations apply/validate
Hibernate schema validation passes
Spring Boot starts on http://127.0.0.1:8080
```

### 3. Local URLs

Backend base:

```text
http://127.0.0.1:8080
```

Swagger UI:

```text
http://127.0.0.1:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://127.0.0.1:8080/v3/api-docs
```

Health:

```text
http://127.0.0.1:8080/actuator/health
```

CSRF token endpoint:

```text
http://127.0.0.1:8080/api/v1/auth/csrf
```

### 4. Verify Database

```powershell
docker compose exec postgres psql -U bautruc -d bautruc_ecommerce -c "SELECT 1;"
docker compose exec postgres psql -U bautruc -d bautruc_ecommerce -c "\dt"
docker compose exec postgres psql -U bautruc -d bautruc_ecommerce -c "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;"
```

### 5. Stop Or Reset

Stop and remove containers without deleting the named PostgreSQL volume:

```powershell
docker compose down
```

Full local database reset:

```powershell
docker compose down -v
docker compose up -d --build
```

Warning: `docker compose down -v` deletes the local PostgreSQL data volume and all local database data.
