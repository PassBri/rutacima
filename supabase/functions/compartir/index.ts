// SuRoute · Enlaces públicos con vista previa (Supabase Edge Function)
//
// Una publicación pública o una invitación se comparten con un enlace que se ve bonito en WhatsApp,
// Instagram, Facebook o X (imagen, título y texto), y que abre una página sencilla con el botón
// "Abrir en SuRoute".
//   ?p=<id de la publicación>     publicación (solo las PUBLICAS; las demás muestran una página genérica)
//   ?i=<usuario>                  invitación personal: al crear la cuenta, las dos personas quedan siguiéndose
// Despliegue (sin pedir sesión, porque quien abre el enlace aún no tiene cuenta):
//   supabase functions deploy compartir --no-verify-jwt
// Opcional (Secrets): SITIO = https://suroute.com

import { createClient } from "jsr:@supabase/supabase-js@2";

const SITIO = (Deno.env.get("SITIO") ?? "https://suroute.com").replace(/\/$/, "");
const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!);

const esc = (s: string) => String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]!));
const corto = (s: string, n: number) => (s.length > n ? s.slice(0, n - 1).trimEnd() + "…" : s);

function pagina(o: { titulo: string; texto: string; imagen?: string; destino: string; boton: string; autor?: string; url: string }) {
  const img = o.imagen || `${SITIO}/iconos/compartir.png`;
  return `<!doctype html><html lang="es"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(o.titulo)} · SuRoute</title>
<meta name="description" content="${esc(corto(o.texto, 200))}">
<meta property="og:site_name" content="SuRoute · Summit Route">
<meta property="og:type" content="article">
<meta property="og:title" content="${esc(o.titulo)}">
<meta property="og:description" content="${esc(corto(o.texto, 200))}">
<meta property="og:image" content="${esc(img)}">
<meta property="og:url" content="${esc(o.url)}">
<meta name="twitter:card" content="summary_large_image">
<meta name="theme-color" content="#6B2A1A">
<style>
  :root { color-scheme: light dark; --b: #6B2A1A; --p: #FBF8F3; --t: #2B1D17; --s: #6E625A; }
  @media (prefers-color-scheme: dark) { :root { --p: #1C1714; --t: #F3ECE4; --s: #B8ADA4; } }
  * { box-sizing: border-box; } body { margin: 0; min-height: 100dvh; display: grid; place-items: center; padding: 24px 16px;
    background: linear-gradient(#5B86B5, #C9D6E3); font: 16px/1.5 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; color: var(--t); }
  main { width: min(100%, 460px); background: var(--p); border-radius: 28px; overflow: hidden; box-shadow: 0 16px 48px -8px rgb(0 0 0 / .3); }
  img { display: block; width: 100%; aspect-ratio: 4 / 5; object-fit: cover; }
  .c { padding: 22px 24px 26px; display: grid; gap: 10px; }
  .a { font-size: 14px; color: var(--s); } h1 { margin: 0; font: 600 22px/1.3 Georgia, "Roboto Serif", serif; }
  p { margin: 0; white-space: pre-line; } a.b { margin-top: 8px; display: block; text-align: center; padding: 14px; border-radius: 999px; background: var(--b); color: #fff; font-weight: 600; text-decoration: none; }
  .m { text-align: center; font-size: 13px; color: var(--s); }
</style></head><body><main>
${o.imagen ? `<img src="${esc(o.imagen)}" alt="">` : ""}
<div class="c">${o.autor ? `<span class="a">${esc(o.autor)}</span>` : ""}<h1>${esc(o.titulo)}</h1>${o.texto ? `<p>${esc(corto(o.texto, 600))}</p>` : ""}
<a class="b" href="${esc(o.destino)}">${esc(o.boton)}</a><span class="m">SuRoute · Summit Route — tu vida es la montaña, cada día es un paso.</span></div>
</main></body></html>`;
}

Deno.serve(async (req) => {
  const u = new URL(req.url), id = u.searchParams.get("p"), invita = (u.searchParams.get("i") ?? "").toLowerCase().replace(/[^a-z0-9._]/g, "");
  let html: string;
  if (id && /^[0-9a-f-]{36}$/i.test(id)) {
    const { data: p } = await db.from("posts").select("texto,image_url,meta_titulo,visibilidad,autor:profiles!posts_user_id_fkey(nombre,username)").eq("id", id).maybeSingle();
    if (p && p.visibilidad === "PUBLICA") {
      // deno-lint-ignore no-explicit-any
      const a = (p as any).autor ?? {};
      html = pagina({ titulo: p.meta_titulo || `${a.nombre || a.username || "Alguien"} sube su montaña`, texto: p.texto, imagen: p.image_url || undefined,
        autor: `${a.nombre || ""}${a.username ? " · @" + a.username : ""}`, destino: `${SITIO}/#p=${id}`, boton: "Abrir en SuRoute", url: req.url });
    } else html = pagina({ titulo: "Una cima en SuRoute", texto: "Esta publicación es privada o ya no está disponible.", destino: `${SITIO}/`, boton: "Conocer SuRoute", url: req.url });
  } else if (invita) {
    const { data: x } = await db.from("profiles").select("nombre,username,cumbre,avatar_url").eq("username", invita).maybeSingle();
    const nombre = x?.nombre || x?.username || "Alguien";
    html = pagina({ titulo: `${nombre} te invita a subir con SuRoute`, texto: x?.cumbre ? `Su cumbre: ${x.cumbre}\n\nCrea tu cuenta y quedan siguiéndose para darse ánimo en cada paso.` : "Crea tu cuenta y quedan siguiéndose para darse ánimo en cada paso.",
      imagen: x?.avatar_url || undefined, destino: `${SITIO}/?invita=${encodeURIComponent(invita)}`, boton: "Crear mi cuenta", url: req.url });
  } else html = pagina({ titulo: "SuRoute · Summit Route", texto: "Tu vida es la montaña, cada día es un paso y la cumbre es tu propósito.", destino: `${SITIO}/`, boton: "Abrir SuRoute", url: req.url });
  return new Response(html, { headers: { "content-type": "text/html; charset=utf-8", "cache-control": "public, max-age=300" } });
});
