# Watch-ER Backend

Cloud Run service for watcher CRUD, scheduled checks, Gemini strategy/synthesis, and push notification dispatch.

## Runtime Responsibilities

- Keep Gemini API keys server-side.
- Store watcher/check/notification records in Firestore.
- Schedule the next check with Cloud Tasks.
- Send push notifications with Firebase Cloud Messaging.
- Provide a dev in-memory store when `DEV_IN_MEMORY_STORE=true`.

## Main Endpoints

- `GET /healthz`
- `POST /v1/watchers`
- `GET /v1/watchers`
- `GET /v1/watchers/:id`
- `POST /v1/watchers/:id/pause`
- `POST /v1/watchers/:id/resume`
- `POST /v1/tasks/check`

## Deployment Notes

Create Cloud Run with these environment variables:

- `GEMINI_API_KEY`
- `GEMINI_STRATEGIST_MODEL`
- `GEMINI_SYNTHESIS_MODEL`
- `FIREBASE_PROJECT_ID`
- `PUBLIC_BASE_URL`

For production, set `DEV_IN_MEMORY_STORE=false` and provide service account permissions for Firestore, Cloud Tasks, and FCM.
