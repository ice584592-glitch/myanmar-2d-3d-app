const UPSTREAM = "https://setlive.myanmarnode.com";

function isAllowed(path) {
  return [
    "/api/v1/twod-result-live",
    "/api/v1/twod-results",
    "/api/v1/threed-results",
    "/api/v1/threed-result-latest",
  ].includes(path) || /^\/api\/v1\/threed-results\/\d{4}-\d{2}-\d{2}$/.test(path);
}

function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, OPTIONS",
    "Access-Control-Allow-Headers": "Accept, Content-Type",
  };
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const cors = corsHeaders();

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: cors });
    }

    if (url.pathname === "/health" && request.method === "GET") {
      return Response.json({ ok: true }, { headers: cors });
    }

    if (request.method !== "GET" || !url.pathname.startsWith("/api/v1/")) {
      return Response.json({ message: "Not found" }, { status: 404, headers: cors });
    }

    if (!isAllowed(url.pathname)) {
      return Response.json({ message: "Endpoint not allowed" }, { status: 404, headers: cors });
    }

    if (!env.SETLIVE_API_KEY) {
      return Response.json({ message: "Server API key is not configured" }, { status: 500, headers: cors });
    }

    const upstreamUrl = new URL(url.pathname + url.search, UPSTREAM);
    try {
      const upstreamResponse = await fetch(upstreamUrl, {
        headers: {
          "X-API-Key": env.SETLIVE_API_KEY,
          Accept: "application/json",
        },
        signal: AbortSignal.timeout(25000),
      });
      const headers = new Headers(cors);
      headers.set("Content-Type", upstreamResponse.headers.get("Content-Type") || "application/json");
      headers.set("Cache-Control", "no-store");
      return new Response(upstreamResponse.body, { status: upstreamResponse.status, headers });
    } catch (_error) {
      return Response.json({ message: "Upstream API unavailable" }, { status: 502, headers: cors });
    }
  },
};
