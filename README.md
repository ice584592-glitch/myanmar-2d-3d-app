# Myanmar 2D/3D Android app

Myanmar 2D/3D results app built with Kotlin and Jetpack Compose.

## Features

- Live 2D results and 2D/3D history from the configured API.
- Latest 3D result, including the 3-digit result, full prize number, draw date, and draw schedule.
- Burmese-first dark UI with connection status, loading feedback, retry action, and empty/error states.
- Settings for live auto-refresh (15, 30, or 60 seconds), saved on the device.
- Live Chat using Socket.IO with chat history and messages broadcast to connected users. The sample backend keeps the latest 100 messages in memory; messages are cleared when the server restarts.
- The backend rate-limits chat messages and limits names/message lengths. The sample chat has no account/login or persistent storage; deploy it only with suitable moderation and access controls for your use case.
- Android internet and network-state permissions. Use HTTPS/WSS endpoints in production.
- The upstream results API key stays on the server in `SETLIVE_API_KEY`, not in the Android app.

## Configure and deploy

The API proxy in `backend/` also hosts Socket.IO chat. Deploy it on a Node.js host that supports long-lived WebSocket connections and configure the required `SETLIVE_API_KEY` environment variable. The service uses `PORT` when provided. Optionally set `CHAT_ALLOWED_ORIGIN` for browser clients.

Set these Gradle properties locally or in the build environment (do not commit secrets or private deployment settings):

```properties
MYANMAR_API_BASE_URL=https://your-api.example.com/
CHAT_SERVER_URL=https://your-api.example.com
```

The chat URL is the deployed server origin, not a path. Without `CHAT_SERVER_URL`, the Chat screen tells the user that chat is not configured. Configure HTTPS in production; the Android manifest does not enable cleartext traffic.

The backend proxy allowlists `/api/v1/threed-result-latest` in addition to the live and history endpoints. The 3D screen shows its `data` fields and the `schedule` fields returned by this endpoint. The upstream endpoint itself is `https://setlive.myanmarnode.com/api/v1/threed-result-latest`.

## Build

Build a debug APK with the repository's Android Gradle project and Java 17. The GitHub Actions workflow also builds the debug APK.
