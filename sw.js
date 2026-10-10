/* Rutaalacima Web · service worker: permite instalarla como app (también en iPhone) y abre rápido.
   - Páginas: primero la red; sin conexión, la última copia.
   - Estilos, scripts, íconos, guías y frases: la copia guardada al instante y se actualiza por detrás.
   - Nunca toca Supabase (cuenta, datos, coach) ni los audios. */
const VERSION = "rutacima-f8fa816b1c65"; // tools/armar_web.sh lo cambia en cada publicación
const BASE = ["./", "index.html", "estilos.css", "config.js", "sello.png", "manifest.webmanifest",
  "iconos/icono-192.png", "iconos/icono-512.png", "iconos/apple-touch-icon.png"];
const CDN = ["cdn.jsdelivr.net", "cdnjs.cloudflare.com", "fonts.googleapis.com", "fonts.gstatic.com"];

self.addEventListener("install", e => {
  e.waitUntil(caches.open(VERSION).then(c => Promise.all(BASE.map(u => c.add(u).catch(() => {})))).then(() => self.skipWaiting()));
});

self.addEventListener("activate", e => {
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== VERSION).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});

self.addEventListener("fetch", e => {
  const req = e.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  const propio = url.origin === self.location.origin;
  if (!propio && !CDN.includes(url.hostname)) return;          // Supabase y demás: directo a la red
  if (url.pathname.includes("/assets/audios/") || req.headers.has("range")) return;
  if (req.mode === "navigate") {
    e.respondWith(fetch(req).then(r => { const copia = r.clone(); caches.open(VERSION).then(c => c.put("index.html", copia)); return r; })
      .catch(() => caches.match("index.html").then(r => r || caches.match("./"))));
    return;
  }
  e.respondWith(caches.open(VERSION).then(async c => {
    const guardada = await c.match(req);
    const red = fetch(req).then(r => { if (r.ok || r.type === "opaque") c.put(req, r.clone()); return r; }).catch(() => guardada);
    return guardada || red;
  }));
});
