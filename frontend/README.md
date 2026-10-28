# TutoText frontend

Angular 21 standalone application with a responsive dashboard, transcript history, manual transcript entry/editing, and browser-based live transcription.

## Run locally

1. Start the Spring Boot backend on `http://localhost:8080`.
2. From this directory, run:

   ```powershell
   npm install
   npm start
   ```
3. Open `http://localhost:4200`. The Angular development-server proxy forwards `/api` requests to the backend.

The frontend API base URL is configured in `src/app/environments/environment.ts`. For a deployment where the frontend and API have different origins, set `apiBaseUrl` to the backend API base URL and configure backend CORS or a reverse proxy.

## Live transcription setup

Live transcription uses Google's `gemini-3.5-transcribe-live` model. The backend provisions a single-use, short-lived token using `GEMINI_API_KEY`; the permanent key stays server-side and is never sent to Angular. The browser sends mono 16 kHz PCM audio to Google's documented Live API WebSocket. Hindi and English are automatically detected; Hindi-English code-switching is best effort, not a separate guaranteed language mode. Sessions stop before Google's 10-minute session limit.

See Google's [Live transcription guide](https://ai.google.dev/gemini-api/docs/live-api/live-transcribe), [WebSocket setup](https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket), [ephemeral token guide](https://ai.google.dev/gemini-api/docs/ephemeral-tokens), and [session management documentation](https://ai.google.dev/gemini-api/docs/live-api/session-management) for current API details.

Set the key in the same PowerShell window used to run the backend, then start Spring Boot from `backend/`:

```powershell
$env:GEMINI_API_KEY = '<YOUR_GOOGLE_AI_STUDIO_API_KEY>'
.\mvnw.cmd spring-boot:run
```

Replace the placeholder locally; do not put a real key in source files, Angular configuration, chat, logs, or Git. Keep the environment variable set in the backend process's terminal session. Without it, live transcription returns a clear server configuration error; manual transcript CRUD remains available. The live token endpoint should be protected and rate-limited before public deployment.

## Build and test

```powershell
npm run build
npm test -- --watch=false
```
