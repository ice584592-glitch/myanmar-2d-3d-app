## Deploy the API proxy on Cloudflare Workers

This is an alternative to the Render Blueprint and keeps the SetLive API key on the server. Never place the API key in the Android app or commit it to GitHub.

1. Create/sign in to a Cloudflare account and open **Workers & Pages**.
2. Create a Worker named `myanmar-2d-3d-api` and deploy the Worker code from `backend/cloudflare-worker.js` (or use Wrangler from the `backend` directory).
3. Set `SETLIVE_API_KEY` as an encrypted Worker secret. With Wrangler: `npx wrangler secret put SETLIVE_API_KEY`.
4. Copy the deployed Worker URL, such as `https://myanmar-2d-3d-api.<your-account>.workers.dev/`.
5. Set the Android Gradle property `MYANMAR_API_BASE_URL` to that URL (keep the trailing slash) and rebuild the app.
6. Check `https://<your-worker>.workers.dev/health`; a healthy deployment returns `{"ok":true}`.

The proxy currently allows the API paths already configured in `backend/server.js`: live 2D, 2D history, 3D history, latest 3D, and date-specific 3D history. An API route only works if it is supported by SetLive and permitted by your API plan. Check Cloudflare's current account and plan requirements before proceeding; they can vary by account and region.
