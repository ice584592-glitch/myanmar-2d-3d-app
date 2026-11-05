# Myanmar 2D/3D Android app

Myanmar 2D/3D results app built with Kotlin and Jetpack Compose.

## Features

- Live 2D results and 2D/3D history from the configured API proxy.
- 3D latest result with first-prize number, draw date, schedule, and paginated result history.
- Burmese-first dark UI with loading, error, and retry states.
- API key remains on the backend in `SETLIVE_API_KEY`; do not put it in the Android app or commit it to GitHub.

## Deploy the API proxy

The repository includes a Render Blueprint in `render.yaml`. On Render, create a Blueprint from this repository and set `SETLIVE_API_KEY` as a secret environment variable when prompted. The backend runs from `backend/` and exposes `/health` plus the allowlisted `/api/v1/` result endpoints. The API key must be obtained from the upstream API provider.

After deployment, set the deployed proxy base URL as the Gradle property `MYANMAR_API_BASE_URL`, including a trailing slash, for example:

```properties
MYANMAR_API_BASE_URL=https://your-backend.onrender.com/
```

The app calls the proxy at `api/v1/...`. Do not set this property to the upstream API URL, since the upstream API key must remain server-side.

## Build

Build the Android app with Java 17 and the repository's Gradle project. Set `MYANMAR_API_BASE_URL` in the local Gradle properties or build environment; the configured URL must be the deployed proxy origin with a trailing slash.
