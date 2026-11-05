const express = require('express');
const app = express();
const apiKey = process.env.SETLIVE_API_KEY;
const upstream = 'https://setlive.myanmarnode.com';

if (!apiKey) {
  console.error('SETLIVE_API_KEY is required');
  process.exit(1);
}

app.get('/health', (_req, res) => res.json({ ok: true }));

function isAllowed(path) {
  return [
    '/api/v1/twod-result-live',
    '/api/v1/twod-results',
    '/api/v1/threed-results',
    '/api/v1/threed-result-latest'
  ].includes(path) || /^\/api\/v1\/threed-results\/\d{4}-\d{2}-\d{2}$/.test(path);
}

app.get('/api/v1/*', async (req, res) => {
  if (!isAllowed(req.path)) return res.status(404).json({ message: 'Endpoint not allowed' });
  const url = new URL(upstream + req.path);
  for (const [key, value] of Object.entries(req.query)) {
    if (typeof value === 'string') url.searchParams.set(key, value);
  }
  try {
    const response = await fetch(url, {
      headers: { 'X-API-Key': apiKey, Accept: 'application/json' },
      signal: AbortSignal.timeout(30000)
    });
    res.status(response.status);
    res.set('Content-Type', response.headers.get('content-type') || 'application/json');
    res.set('Cache-Control', 'no-store');
    res.send(Buffer.from(await response.arrayBuffer()));
  } catch (_error) {
    res.status(502).json({ message: 'Upstream API unavailable' });
  }
});

app.listen(process.env.PORT || 3000, () => console.log('API proxy running'));
