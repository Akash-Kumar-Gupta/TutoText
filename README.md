# TutoText

TutoText is a responsive web application for creating and managing speech transcripts. It provides transcript history and editing, a dashboard based on saved transcripts, manual transcript entry, and browser-based live transcription through the Gemini Live API.

## Features

- Dashboard with saved transcript count, total recorded duration, and recent transcripts. These values are calculated from the transcripts returned by the API; they are not hardcoded.
- Transcript history with search, details, title/text editing, and deletion.
- Manual transcript entry with title, language, and duration.
- Live microphone transcription with interim and finalized text, Hindi/English automatic detection, session controls, and saving finalized text.
- Account registration and login with BCrypt password hashing and expiring JWT access tokens.
- MySQL persistence for transcripts.

Live transcription requires a server-side Gemini API key. Hindi-English code-switching is best effort and depends on model output.

## Technology and architecture

- **Frontend:** Angular 21 standalone components, TypeScript, Bootstrap 5, and SCSS.
- **Backend:** Java 25, Spring Boot 4.0.9, Maven Wrapper, Spring MVC, Spring Security, JWT resource-server validation, BCrypt, validation, Spring Data JPA, and Hibernate.
- **Database:** MySQL.
- **Live speech:** Gemini Live API. The backend provisions a short-lived token; the permanent Gemini API key is not configured in Angular.

The Angular application calls the Spring Boot REST API with an in-memory bearer token. Backend controllers delegate to services and Spring Data repositories. The `database/schema.sql` script defines the MySQL schema, and Hibernate validates it at startup rather than creating or updating tables.

## Prerequisites

- Java 25.
- Node.js and npm versions supported by Angular 21.
- MySQL Server and a database user with permission to create/use `tutotext_db` and its tables.
- MySQL Workbench is optional; the schema can also be run with a MySQL command-line client.
- No globally installed Maven is needed; use the included Maven Wrapper.
- A Google AI Studio API key is needed only for live Gemini transcription.
- A base64-encoded random JWT signing secret of at least 32 bytes is required to start the backend.

## 1. Create the database

The checked-in [database/schema.sql](database/schema.sql) creates the `tutotext_db` database, `app_users` table, and `transcriptions` table with a required owner foreign key. It includes a `LONGTEXT` transcript column and timestamp fields.

### Using MySQL Workbench

1. Connect to your MySQL server.
2. Open `database/schema.sql` in Workbench.
3. Execute the script with the lightning-bolt button.
4. Refresh the Schemas panel and confirm `tutotext_db`, `app_users`, and `transcriptions` are present.

This script is for initial schema creation, not a migration mechanism for evolving an existing database. Back up important data before making database changes.

### Existing installations

For a database created before account registration, first apply `database/migrations/001_create_app_users.sql` if `app_users` does not already exist. For existing transcript data, use this staged procedure; it preserves records and deliberately does not choose an owner:

1. Back up the database and put the existing application into maintenance mode so no old backend instance can serve transcript requests during migration.
2. Ensure `app_users` exists. If migrating from before account authentication, apply `database/migrations/001_create_app_users.sql`, temporarily start the Phase 1 backend in a restricted maintenance environment, and register the intended account. Stop every backend instance before continuing. For databases that already have accounts, select the intended account now.
3. Apply `database/migrations/002_add_nullable_transcription_owner.sql`. Existing rows keep `owner_id = NULL`; the foreign key and lookup index are added. Keep the application unavailable while rows are unowned.
4. Review accounts and unowned transcript IDs before assigning anything:

   ```sql
   SELECT id, email FROM app_users ORDER BY id;
   SELECT id, title, created_at FROM transcriptions WHERE owner_id IS NULL ORDER BY id;
   ```

5. Assign ownership only after choosing the correct account. To assign all remaining unowned records to one explicitly selected account, replace the example email with that account's actual email:

   ```sql
   UPDATE transcriptions AS t
   JOIN app_users AS u ON u.email = 'chosen-owner@example.com'
   SET t.owner_id = u.id
   WHERE t.owner_id IS NULL;
   ```

   If transcripts belong to different people, assign them individually or in reviewed groups using an explicit mapping. Do not start the updated backend while legacy rows remain unowned.
6. Confirm no rows remain unowned:

   ```sql
   SELECT COUNT(*) AS unowned_count FROM transcriptions WHERE owner_id IS NULL;
   ```

   Continue only when the count is zero. Then apply `database/migrations/003_require_transcription_owner.sql`, which enforces `NOT NULL`. Start the backend after this migration; Hibernate validates the schema and rejects the nullable intermediate state.

7. Apply `database/migrations/003_require_transcription_owner.sql`, then start only the updated backend and verify Hibernate schema validation succeeds before removing maintenance mode.

These steps do not delete transcripts or assign them arbitrarily. The updated backend cannot start against the nullable intermediate schema, and the old unscoped backend must remain stopped while records have no owner. Do not reopen access until every existing transcript has an intentional owner and the required constraint has been applied. Keep a backup before applying schema changes.

## 2. Configure and run the backend

From a VS Code PowerShell terminal:

```powershell
Set-Location .\backend
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Generate a fresh JWT secret locally without printing it:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$secret = [Convert]::ToBase64String($bytes)
$content = Get-Content .env -Raw
$content = $content -replace '(?m)^JWT_SECRET=.*$', "JWT_SECRET=$secret"
[System.IO.File]::WriteAllText((Join-Path $PWD '.env'), $content, (New-Object System.Text.UTF8Encoding -ArgumentList $false))
Remove-Variable bytes, secret, content
```

Then edit the remaining placeholders in `backend/.env`:

```properties
DB_URL=jdbc:mysql://localhost:3306/tutotext_db
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_local_mysql_password
GEMINI_API_KEY=your_google_ai_studio_api_key
JWT_SECRET=generated_by_the_command_above
JWT_EXPIRATION_MS=900000
APP_CORS_ALLOWED_ORIGINS=
RATE_LIMIT_REGISTRATION_IP_CAPACITY=5
RATE_LIMIT_REGISTRATION_EMAIL_CAPACITY=3
RATE_LIMIT_REGISTRATION_WINDOW_SECONDS=3600
RATE_LIMIT_LOGIN_IP_CAPACITY=20
RATE_LIMIT_LOGIN_EMAIL_CAPACITY=10
RATE_LIMIT_LOGIN_WINDOW_SECONDS=900
RATE_LIMIT_LIVE_TOKEN_IP_CAPACITY=10
RATE_LIMIT_LIVE_TOKEN_ACCOUNT_CAPACITY=5
RATE_LIMIT_LIVE_TOKEN_WINDOW_SECONDS=600
RATE_LIMIT_BUCKET_RETENTION_SECONDS=7200
AUTH_REQUEST_MAX_BYTES=16384
TRANSCRIPTION_REQUEST_MAX_BYTES=17825792
OTHER_API_REQUEST_MAX_BYTES=65536
```

Spring Boot imports the optional `.env` file when started from the `backend` directory. `DB_URL` defaults to `jdbc:mysql://localhost:3306/tutotext_db` if omitted. `DB_USERNAME` and `DB_PASSWORD` must match a MySQL account that can access the database. `GEMINI_API_KEY` is required for live session-token requests; transcript CRUD and account auth can be used without it. `JWT_SECRET` is required; the backend refuses missing, invalid Base64, or fewer-than-32-byte signing secrets. `JWT_EXPIRATION_MS` defaults to 900,000 milliseconds (15 minutes) and is limited to one minute through one day. `APP_CORS_ALLOWED_ORIGINS` is an optional comma-separated exact-origin allowlist for a separately hosted frontend; leave empty for same-origin deployments.

Keep actual credentials only in the ignored local `.env` file or in your deployment platform's secret/environment-variable manager. Do not paste credentials into source files, Angular configuration, logs, screenshots, or Git.

Start Spring Boot from the `backend` directory:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend listens on `http://localhost:8080` by default. It uses `spring.jpa.hibernate.ddl-auto=validate`, so the database schema must exist and match the entity mapping before startup.

## 3. Install and run the frontend

In another terminal:

```powershell
Set-Location .\frontend
npm ci
npm start
```

Open `http://localhost:4200`. The Angular development proxy forwards `/api` requests to `http://localhost:8080`; this proxy is for local development. The frontend API base URL is `/api` in `frontend/src/app/environments/environment.ts`.

## REST API

All paths below are implemented by the Spring Boot backend. Transcription request bodies use JSON fields `title`, `transcriptText`, `language`, and `durationSeconds`. Titles are limited to 150 characters, language labels to 30 characters, transcript text to 4,000,000 characters, and duration must be a non-negative integer.

| Method | Path | Purpose | Successful response |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register with email and password (12–72 characters; maximum 72 UTF-8 bytes) | `201 Created`, bearer token response |
| `POST` | `/api/auth/login` | Authenticate with email and password | `200 OK`, bearer token response |
| `POST` | `/api/transcriptions` | Create a transcript | `201 Created`, transcript response and `Location` header |
| `GET` | `/api/transcriptions` | List transcripts | `200 OK`, array of transcript responses |
| `GET` | `/api/transcriptions/{id}` | Get one transcript | `200 OK`, transcript response |
| `PUT` | `/api/transcriptions/{id}` | Replace a transcript's editable fields | `200 OK`, updated transcript response |
| `DELETE` | `/api/transcriptions/{id}` | Delete a transcript | `204 No Content` |
| `POST` | `/api/live-sessions/token` | Provision a short-lived Gemini Live session token | `200 OK`, token response with expiry and model |

Registration and login are public. All other backend routes require `Authorization: Bearer <accessToken>`. Missing transcript records return `404 Not Found`; duplicate registration returns `409 Conflict`; invalid credentials and missing/invalid/expired JWTs return `401 Unauthorized`; invalid request bodies or non-positive IDs return `400 Bad Request`. Error responses use the backend's structured error shape with status, message, request path, timestamp, and validation details where applicable.

The Angular application keeps the JWT in memory only, not in local or session storage. This reduces persistence if the browser is closed, but a page refresh clears the session and requires the user to sign in again. Any successful same-origin script execution (such as XSS) can still access an in-memory token; use a strong content security policy and prevent script injection.

### Rate limits and request-body limits

The backend uses Bucket4j in-memory token buckets. Defaults are:

| Operation | Per client IP | Per account identity | Refill window |
|---|---:|---:|---:|
| Registration | 5 requests | 3 requests per normalized email | 1 hour |
| Login, including failed attempts | 20 requests | 10 requests per normalized email | 15 minutes |
| Gemini live-token issuance | 10 requests | 5 requests per authenticated account | 10 minutes |

Exhausted limits return `429 Too Many Requests`, a generic error body, and `Retry-After`. Client IP uses the servlet connection's `remoteAddr`; `Forwarded` and `X-Forwarded-For` request headers are not trusted. When deployed behind a reverse proxy, prevent direct client access to the application and configure the proxy to handle client identity safely. Until explicit trusted-proxy support is implemented, all users arriving through one proxy may share its IP budget.

The limiter is process-local: counters reset when the application restarts, are not shared across instances, and the same user may receive a separate budget on each instance. It is an abuse-control baseline, not a distributed enforcement boundary. Multi-instance production deployments should use a shared Redis-backed limiter or an equivalent centralized gateway/WAF policy, monitor 429 rates, and tune budgets to expected traffic. Account-based limits supplement IP limits, so rotating email addresses does not bypass the IP budget. Account identifiers are normalized and hashed for in-memory limiter keys; credentials, tokens, and transcript content are not included in limiter keys or diagnostics.

Request bodies are capped before JSON parsing: authentication requests at 16 KiB, transcription create/update requests at 17 MiB, and other `/api` request bodies at 64 KiB. The transcription cap allows the existing 4,000,000-character contract when encoded as UTF-8 (including four-byte Unicode characters), with room for JSON fields. Oversized bodies return `413 Payload Too Large`. Limits are configurable with `AUTH_REQUEST_MAX_BYTES`, `TRANSCRIPTION_REQUEST_MAX_BYTES`, and `OTHER_API_REQUEST_MAX_BYTES`; do not lower the transcription cap below 16 MiB if the existing maximum transcript size must remain usable.

The dashboard currently derives its metrics from `GET /api/transcriptions`; there is no separate dashboard-stats endpoint.

The live-session endpoint returns an ephemeral token to the browser for the Live API WebSocket. Do not log, persist, or share that token. The permanent `GEMINI_API_KEY` must remain server-side.

## Tests and production build

Run frontend tests:

```powershell
Set-Location .\frontend
npm test -- --watch=false
```

Build the frontend for production:

```powershell
npm run build
```

Run backend tests:

```powershell
Set-Location .\backend
.\mvnw.cmd test
```

Backend tests use the configured H2 test database and do not require local MySQL credentials.

## Current limitations and deployment security

- Transcript operations are scoped to the authenticated account, and a guessed ID belonging to another account returns the same 404 as a missing record. Existing installations must complete the documented owner-assignment migration before starting this version.
- The in-memory rate limiter does not coordinate across application instances and resets on restart. Use a shared limiter or gateway policy for distributed production enforcement.
- Duplicate registration currently returns `409 Conflict` with an account-exists message to preserve the immediate registration-and-login workflow. This reveals whether an email is registered; use email verification and a non-enumerating signup response if that disclosure is unacceptable.
- Add account recovery, email verification, and a token refresh/revocation strategy before production use. Access tokens expire and currently require signing in again.
- Configure production secrets through a secret manager or environment variables. Never include a real `.env` file or API key in a deployment artifact or source-control commit.
- Use HTTPS for the frontend and API, set `APP_CORS_ALLOWED_ORIGINS` to exact trusted origins when cross-origin deployment is needed, keep MySQL private, and apply backups and least-privilege database access. CORS is not an authentication mechanism.
- The Angular proxy configuration is for local development. Production requires a deliberate API base URL or reverse-proxy setup.
- A successful local build or test run is not a deployment. **TutoText is not documented here as deployed.**
- Live transcription depends on Gemini model availability, API access, network connectivity, usage limits, and browser microphone permission. Mixed-language recognition is not guaranteed.
