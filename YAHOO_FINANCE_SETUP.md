# Yahoo Finance integration

The Android app's Yahoo Finance screen uses the Cloudflare Worker route `/api/v1/yahoo/chart?symbol=AAPL`. The Worker fetches Yahoo Finance's chart API and returns its JSON response; no Yahoo or SetLive API key is embedded in the Android app.

## Configure the app

Set `MYANMAR_API_BASE_URL` to the **deployed Cloudflare Worker origin**, including a trailing slash, in the build environment's `gradle.properties` (or `~/.gradle/gradle.properties`):

```properties
MYANMAR_API_BASE_URL=https://YOUR-WORKER.YOUR-ACCOUNT.workers.dev/
```

Then build and install the app:

```bash
./gradlew assembleDebug
```

Before building, test the route in a browser or with curl, replacing the host with your deployed Worker:

```text
https://YOUR-WORKER.YOUR-ACCOUNT.workers.dev/api/v1/yahoo/chart?symbol=AAPL
```

The app starts on Yahoo Finance, defaults to `AAPL`, accepts a Yahoo symbol, and shows the latest market price and available daily closing prices for the last month. Use the button at the bottom to return to the Myanmar 2D/3D results screen.

Yahoo Finance data availability and terms are controlled by Yahoo. The Worker URL is not included in this repository because it is specific to the deployment account.
