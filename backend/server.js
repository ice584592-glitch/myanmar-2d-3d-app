const express = require('express');
const http = require('http');
const { Server } = require('socket.io');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: { origin: process.env.CHAT_ALLOWED_ORIGIN || '*' },
  maxHttpBufferSize: 16 * 1024
});
const apiKey = process.env.SETLIVE_API_KEY;
const upstream = 'https://setlive.myanmarnode.com';
const recentMessages = [];
const MAX_MESSAGES = 100;
const MESSAGE_INTERVAL_MS = 1500;

if (!apiKey) {
  console.error('SETLIVE_API_KEY is required');
  process.exit(1);
}

app.get('/health', (_req, res) => res.json({ ok: true, chat: true }));

function isAllowed(path) {
  return ['/api/v1/twod-result-live', '/api/v1/twod-results', '/api/v1/threed-results'].includes(path) ||
    /^\\/api\\/v1\\/threed-results\\/\\d{4}-\\d{2}-\\d{2}$/.test(path);
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
}

io.on('connection', (socket) => {
  socket.emit('chat:history', recentMessages);
  socket.on('chat:send', (payload) => {
    const now = Date.now();
    if (socket.data.lastMessageAt && now - socket.data.lastMessageAt < MESSAGE_INTERVAL_MS) {
      socket.emit('chat:error', 'စာပို့တာ မြန်လွန်းနေပါတယ်။ ခဏစောင့်ပြီး ထပ်ပို့ပါ။');
      return;
    }
    const username = typeof payload?.username === 'string' ? payload.username.trim().slice(0, 24) : '';
    const text = typeof payload?.text === 'string' ? payload.text.trim().slice(0, 500) : '';
    if (!text) {
      socket.emit('chat:error', 'စာသားကို ထည့်ပေးပါ။');
      return;
    }
    socket.data.lastMessageAt = now;
    const message = {
      id: `${now}-${Math.random().toString(36).slice(2, 10)}`,
      username: username || 'ဧည့်သည်',
      text,
      timestamp: new Date(now).toISOString()
    };
    recentMessages.push(message);
    if (recentMessages.length > MAX_MESSAGES) recentMessages.shift();
    io.emit('chat:message', message);
  });
});

const port = Number(process.env.PORT) || 3000;
server.listen(port, () => console.log(`API and chat server listening on ${port}`));
