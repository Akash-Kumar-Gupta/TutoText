# TutoText

TutoText is a responsive web application for creating and managing speech transcripts. It provides transcript history and editing, a dashboard based on saved transcripts, manual transcript entry, and browser-based live transcription through the Gemini Live API.

## Features

- Dashboard with saved transcript count, total recorded duration, and recent transcripts. These values are calculated from the transcripts returned by the API; they are not hardcoded.
- Transcript history with search, details, title/text editing, and deletion.
- Manual transcript entry with title, language, and duration.
- Live microphone transcription with interim and finalized text, Hindi/English automatic detection, session controls, and saving finalized text.
- MySQL persistence for transcripts.

Live transcription requires a server-side Gemini API key. Hindi-English code-switching is best effort and depends on model output.

## Technology and architecture

- **Frontend:** Angular 21 standalone components, TypeScript, Bootstrap 5, and SCSS.
- **Backend:** Java 25, Spring Boot 4.0.9, Maven Wrapper, Spring MVC, validation, Spring Data JPA, and Hibernate.
- **Database:** MySQL.
- **Live speech:** Gemini Live API. The backend provisions a short-lived token; the permanent Gemini API key is not configured in Angular.

The Angular application calls the Spring Boot REST API. Backend controllers delegate to services and a Spring Data repository. The `database/schema.sql` script defines the MySQL schema, and Hibernate validates it at startup rather than creating or updating tables.

## Prerequisites

- Java 25.
- Node.js and npm versions supported by Angular 21.
- MySQL Server and a database user with permission to create/use `tutotext_db` and its tables.
- MySQL Workbench is optional; the schema can also be run with a MySQL command-line client.
- No globally installed Maven is needed; use the included Maven Wrapper.
- A Google AI Studio API key is needed only for live Gemini transcription.

## 1. Create the database

The checked-in [database/schema.sql](database/schema.sql) creates the `tutotext_db` database and `transcriptions` table if they do not already exist. It includes a `LONGTEXT` transcript column and timestamp fields.

### Using MySQL Workbench

1. Connect to your MySQL server.
2. Open `database/schema.sql` in Workbench.
3. Execute the script with the lightning-bolt button.
4. Refresh the Schemas panel and confirm `tutotext_db` and its `transcriptions` table are present.

This script is for initial schema creation, not a migration mechanism for evolving an existing database. Back up important data before making database changes.

## 2. Configure and run the backend

From a VS Code PowerShell terminal:

```powershell
Set-Location .\backend
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Edit `backend/.env` on your machine and replace the placeholders:

```properties
DB_URL=jdbc:mysql://localhost:3306/tutotext_db
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_local_mysql_password
GEMINI_API_KEY=your_google_ai_studio_api_key
```

Spring Boot imports the optional `.env` file when started from the `backend` directory. `DB_URL` defaults to `jdbc:mysql://localhost:3306/tutotext_db` if omitted. `DB_USERNAME` and `DB_PASSWORD` must match a MySQL account that can access the database. `GEMINI_API_KEY` is required for live session-token requests; transcript CRUD can be used without it.

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
| `POST` | `/api/transcriptions` | Create a transcript | `201 Created`, transcript response and `Location` header |
| `GET` | `/api/transcriptions` | List transcripts | `200 OK`, array of transcript responses |
| `GET` | `/api/transcriptions/{id}` | Get one transcript | `200 OK`, transcript response |
| `PUT` | `/api/transcriptions/{id}` | Replace a transcript's editable fields | `200 OK`, updated transcript response |
| `DELETE` | `/api/transcriptions/{id}` | Delete a transcript | `204 No Content` |
| `POST` | `/api/live-sessions/token` | Provision a short-lived Gemini Live session token | `200 OK`, token response with expiry and model |

Missing transcript records return `404 Not Found`. Invalid request bodies or non-positive IDs return `400 Bad Request`. Error responses use the backend's structured error shape with status, message, request path, timestamp, and validation details where applicable.

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

- The application has no user authentication or authorization. Do not expose transcript CRUD or live-session token provisioning publicly without adding appropriate access controls.
- Protect and rate-limit `/api/live-sessions/token` before public deployment to reduce API abuse and unexpected Gemini usage.
- Configure production secrets through a secret manager or environment variables. Never include a real `.env` file or API key in a deployment artifact or source-control commit.
- Use HTTPS for the frontend and API, restrict CORS to trusted origins, keep MySQL private, and apply backups and least-privilege database access.
- The Angular proxy configuration is for local development. Production requires a deliberate API base URL or reverse-proxy setup.
- A successful local build or test run is not a deployment. **TutoText is not documented here as deployed.**
- Live transcription depends on Gemini model availability, API access, network connectivity, usage limits, and browser microphone permission. Mixed-language recognition is not guaranteed.
