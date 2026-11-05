# Myanmar 2D/3D Android app

Myanmar 2D/3D results app built with Kotlin and Jetpack Compose.

## Features

- Live 2D results and 2D/3D history from the configured API.
- Burmese-first dark UI with connection status, loading feedback, retry action, and empty/error states.
- Android internet and network-state permissions. Cleartext HTTP is disabled; use an HTTPS API endpoint.
- The backend proxy keeps the upstream `SETLIVE_API_KEY` on the server rather than in the Android app.

## Configure the API

The Android app uses `MYANMAR_API_BASE_URL` as its Retrofit base URL. Set it to the HTTPS origin of your deployed API proxy (with a trailing slash), for example:

```properties
MYANMAR_API_BASE_URL=https://your-api.example.com/
```

You can put this in your local `gradle.properties` (do not commit secrets or private deployment configuration), or pass it to Gradle as a project property. The proxy in `backend/` requires the `SETLIVE_API_KEY` environment variable on the hosting service. The app cannot retrieve data until that proxy is deployed/reachable and its upstream API key is configured.

The proxy exposes `/health` and the allowlisted `/api/v1/...` result endpoints. Do not put the upstream API key in the Android app or commit it to the repository.

## Build

Build a debug APK with the repository's Android Gradle project and Java 17. The GitHub Actions workflow also builds the debug APK.
