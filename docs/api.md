# Watch-ER API Contract

Base URL is configured by the Android app. All authenticated requests should include:

```http
Authorization: Bearer <firebase-id-token>
```

For local development, the backend may accept `x-dev-user-id`.

## Preview Strategy

```http
POST /v1/strategies/preview
Content-Type: application/json
```

```json
{
  "intent": "Tell me when xenova publishes MobileCLIP2-S2-int8 on HuggingFace"
}
```

Response:

```json
{
  "strategy": {
    "title": "xenova/MobileCLIP2-S2-int8",
    "frequency": "15m",
    "fallbackStrategy": "If the API fails, check the public model page and search results.",
    "sources": [],
    "verificationRules": []
  }
}
```

## Create Watcher

```http
POST /v1/watchers
Content-Type: application/json
```

```json
{
  "intent": "Tell me when xenova publishes MobileCLIP2-S2-int8 on HuggingFace",
  "mode": "oneshot",
  "confidenceThreshold": 90,
  "quietHours": {
    "start": "23:00",
    "end": "07:00"
  },
  "strategy": {}
}
```

Response:

```json
{
  "watcher": {
    "id": "watcher_123",
    "intent": "Tell me when xenova publishes MobileCLIP2-S2-int8 on HuggingFace",
    "mode": "oneshot",
    "status": "active",
    "confidenceThreshold": 90,
    "frequency": "15m",
    "strategy": {
      "title": "xenova/MobileCLIP2-S2-int8",
      "sources": [
        {
          "id": "hf_api",
          "type": "api",
          "url": "https://huggingface.co/api/models?author=xenova",
          "priority": 1
        }
      ],
      "verificationRules": [
        "Model name must exactly match MobileCLIP2-S2-int8"
      ]
    },
    "nextCheckAt": "2026-05-25T12:45:00.000Z"
  }
}
```

## List Watchers

```http
GET /v1/watchers
```

Response:

```json
{
  "watchers": []
}
```

## Run Watcher Check

Cloud Tasks calls this endpoint. It is not called by Android directly.

```http
POST /v1/tasks/check
Content-Type: application/json
```

```json
{
  "watcherId": "watcher_123"
}
```

Response:

```json
{
  "check": {
    "id": "check_123",
    "watcherId": "watcher_123",
    "synthesis": {
      "isTrue": false,
      "confidence": 34,
      "reasoning": "The model page was not found in the checked sources.",
      "evidence": [],
      "nextCheckAt": "2026-05-25T13:00:00.000Z"
    },
    "notificationSent": false
  }
}
```

## Public Types

- `WatcherMode`: `oneshot | persistent`
- `WatcherStatus`: `active | checking | paused | completed | error`
- `SourceType`: `url | api | rss | search`
- `NotificationChannel`: `push`

The Android app should treat unknown fields as forward-compatible additions.
