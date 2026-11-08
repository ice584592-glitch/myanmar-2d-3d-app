const UPSTREAM = "https://setlive.myanmarnode.com";
const YAHOO_CHART = "https://query1.finance.yahoo.com/v8/finance/chart/";

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

function jsonError(message, status, cors) {
  return Response.json({ message }, { status, headers: cors });
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
      return jsonError("Not found", 404, cors);
    }

    // Public Yahoo Finance chart endpoint; does not require the SetLive API key.
    if (url.pathname === "/api/v1/yahoo/chart") {
      const symbol = url.searchParams.get("symbol") || "AAPL";
      if (!/^[A-Za-z0-9.^=_-]{1,32}$/.test(symbol)) {
        return jsonError("Invalid Yahoo Finance symbol", 400, cors);
      }

      const yahooUrl = new URL(encodeURIComponent(symbol), YAHOO_CHART);
      yahooUrl.searchParams.set("interval", "1d");
      yahooUrl.searchParams.set("range", "1mo");
      try {
        const yahooResponse = await fetch(yahooUrl, {
          headers: { "User-Agent": "Mozilla/5.0", Accept: "application/json" },
          signal: AbortSignal.timeout(25000),
        });
        const headers = new Headers(cors);
        headers.set("Content-Type", yahooResponse.headers.get("Content-Type") || "application/json");
        headers.set("Cache-Control", "no-store");
        return new Response(yahooResponse.body, { status: yahooResponse.status, headers });
      } catch (_error) {
        return jsonError("Yahoo Finance API unavailable", 502, cors);
      }
    }

    if (!isAllowed(url.pathname)) {
      return jsonError("Endpoint not allowed", 404, cors);
    }

    if (!env.SETLIVE_API_KEY) {
      return jsonError("Server API key is not configured", 500, cors);
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
      return jsonError("Upstream API unavailable", 502, cors);
    }
  },
};
