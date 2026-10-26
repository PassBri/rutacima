"use strict";
/* ======================================================================
 * Climas: los íconos de clima de montaña (los mismos de la app, ui/components/ClimaIconos.kt) y la
 * reacción "¿Qué clima te deja?" de la comunidad, debajo del corazón.
 * · Cinco reacciones: sol (me ilumina), tormenta (me sacude), niebla (me identifica), valle (me da calma)
 *   y cumbre (me impulsa a subir).
 * · El botón muestra el clima que más dejó la comunidad dentro de un anillo de cielo: cada tramo del
 *   anillo es la parte de cada clima. Al tocarlo se abre el abanico de climas; tocar el tuyo lo quita.
 * · En la nube vive en la tabla post_climas (supabase/2026-10-28-climas.sql). Sin cuenta, en este navegador.
 * ====================================================================== */
/** Trazos de 48×48: [d, relleno] o [d, null, trazo, grosor]. */
const CLIMA_TRAZOS = {
  sol: [["M31 3v3M31 22v3M20 14h3M39 14h3M23.5 6.5l2 2M36.5 19.5l2 2M38.5 6.5l-2 2", null, "#E3A93B", 2.2],
    ["M25 14a6 6 0 1 0 12 0a6 6 0 1 0-12 0", "#E3A93B"], ["M3 43L18 21l8 11 5-6 14 17z", "#8A6A48"], ["M18 21l4.5 6.5-3-1-3 2-2.5-1.5z", "#F4EFE6"]],
  tormenta: [["M12 22a8 8 0 0 1 15-4 6 6 0 0 1 9 5 5 5 0 0 1-1 10H13a6 6 0 0 1-1-11z", "#6B7888"],
    ["M25 26l-4 8h5l-3 8 9-11h-5l3-5z", "#F0C043"], ["M14 37l-2 4M36 37l-2 4", null, "#8FB0D3", 2.2]],
  niebla: [["M6 36l13-18 8 10 5-5 10 13z", "#6F7B88"], ["M6 26h20M14 32h28M8 40h30", null, "#BCC4CC", 3]],
  valle: [["M18 26a6 6 0 1 0 12 0a6 6 0 1 0-12 0", "#E8B26A"], ["M2 30l12-12 10 12 10-12 12 12v14H2z", "#4E7B57"],
    ["M2 38c8-4 14 2 22-1s14-2 22 0v8H2z", "#8DB894"]],
  cumbre: [["M6 44c4-5 16-5 20 0z", "#8A6A48"], ["M16 6v36", null, "#E9E2D3", 3], ["M17 7h20l-5 6 5 6H17z", "#C0473A"]],
};
const REAC_CLIMA = [
  ["sol", "Me ilumina", "#E3A93B"], ["tormenta", "Me sacude", "#7E8FA3"], ["niebla", "Me identifica", "#B9C1C9"],
  ["valle", "Me da calma", "#6FA07A"], ["cumbre", "Me impulsa a subir", "#C0473A"],
];
function climaSvg(k, cls = "clima-ic") {
  return `<svg class="${cls}" viewBox="0 0 48 48" aria-hidden="true">${(CLIMA_TRAZOS[k] || []).map(([d, f, s, w]) =>
    `<path d="${d}" fill="${f || "none"}"${s ? ` stroke="${s}" stroke-width="${w}" stroke-linecap="round"` : ""}/>`).join("")}</svg>`;
}

/* ---------- Datos: conteos y mi clima ---------- */
const climasLocales = () => { try { return JSON.parse(localStorage.getItem("suroute-climas") || "{}"); } catch { return {}; } };
/** Conteos de ejemplo estables por publicación (sin cuenta): siempre los mismos para el mismo id. */
function climasDemo(id) {
  let h = 0; for (const ch of String(id)) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  const r = {}; REAC_CLIMA.forEach(([k], i) => { r[k] = (h >> (i * 3)) % (i === 0 || i === 4 ? 9 : 5); });
  return r;
}
/** Agrega climas y miClima a las publicaciones del muro (la nube o el ejemplo). */
async function cargarClimas(ps) {
  if (!ps?.length) return;
  const locales = climasLocales();
  if (!Store.nube) { ps.forEach(p => { p.climas = climasDemo(p.id); p.miClima = locales[p.id] || null; if (p.miClima) p.climas[p.miClima]++; }); return; }
  const ids = ps.filter(p => !p.demo && /^[0-9a-f-]{36}$/i.test(String(p.id))).map(p => p.id);
  ps.forEach(p => { p.climas = p.demo ? climasDemo(p.id) : {}; p.miClima = p.demo ? locales[p.id] || null : null; if (p.demo && p.miClima) p.climas[p.miClima]++; });
  if (!ids.length) return;
  try {
    const { data, error } = await Store.nube.sb.from("post_climas").select("post_id,user_id,clima").in("post_id", ids);
    if (error) throw error;
    const yo = Store.nube.dueno, por = new Map(ps.map(p => [String(p.id), p]));
    (data || []).forEach(f => { const p = por.get(String(f.post_id)); if (!p) return; p.climas[f.clima] = (p.climas[f.clima] || 0) + 1; if (f.user_id === yo) p.miClima = f.clima; });
  } catch (e) { console.warn("Climas: falta correr supabase/2026-10-28-climas.sql", e); }
}

/* ---------- El botón: anillo de cielo con el clima dominante ---------- */
function botonClima(p, lista = false) {
  const c = p.climas || {}, total = REAC_CLIMA.reduce((s, [k]) => s + (c[k] || 0), 0);
  const dom = p.miClima || (total ? REAC_CLIMA.reduce((a, b) => (c[b[0]] || 0) > (c[a[0]] || 0) ? b : a)[0] : null);
  let ac = 0;
  const aro = total ? `conic-gradient(${REAC_CLIMA.filter(([k]) => c[k]).map(([k, , col]) => { const a = ac; ac += c[k] / total * 360; return `${col} ${a}deg ${ac - 2}deg, transparent ${ac - 2}deg ${ac}deg`; }).join(", ")})`
    : "conic-gradient(rgb(255 255 255 / .35) 0 360deg)";
  const nombre = p.miClima ? REAC_CLIMA.find(r => r[0] === p.miClima)[1] : "¿Qué clima te deja?";
  return `<button class="clima-btn${lista ? " en-lista" : ""}${p.miClima ? " mio" : ""}" data-acc="abrirClimas" data-arg="${esc(p.id)}" aria-haspopup="true" aria-pressed="${!!p.miClima}" aria-label="${esc(nombre)}, ${total}">
    <span class="cb-circ"><span class="cb-aro" style="background:${aro}"></span>${dom ? climaSvg(dom) : climaSvg("sol", "clima-ic vacio")}</span><b>${total || (lista ? "Clima" : "")}</b></button>`;
}
globalThis.botonClima = botonClima;

function cerrarAbanico() { document.querySelector(".clima-abanico")?.remove(); document.removeEventListener("pointerdown", fueraAbanico, true); document.removeEventListener("keydown", escAbanico); }
function fueraAbanico(e) { if (!e.target.closest(".clima-abanico, .clima-btn")) cerrarAbanico(); }
function escAbanico(e) { if (e.key === "Escape") cerrarAbanico(); }
const postPorId = id => [...(estado.feed || []), ...(estado.misPosts || [])].find(x => String(x.id) === String(id)) || (String(estado.post?.id) === String(id) ? estado.post : null);

Object.assign(ACC, {
  abrirClimas(el, id) {
    const abierto = document.querySelector(".clima-abanico");
    if (abierto) { const mismo = abierto.dataset.post === id; cerrarAbanico(); if (mismo) return; }
    const p = postPorId(id); if (!p) return;
    const ab = document.createElement("div");
    ab.className = "clima-abanico" + (el.classList.contains("en-lista") ? " arriba" : ""); ab.dataset.post = id; ab.setAttribute("role", "menu"); ab.setAttribute("aria-label", "¿Qué clima te deja?");
    ab.innerHTML = REAC_CLIMA.map(([k, n], i) => `<button role="menuitemradio" aria-checked="${p.miClima === k}" class="${p.miClima === k ? "si" : ""}" style="--i:${i}" data-clima="${k}" title="${n}">${climaSvg(k)}<small>${n}</small></button>`).join("");
    ab.addEventListener("click", e => { const b = e.target.closest("[data-clima]"); if (b) { e.stopPropagation(); elegirClima(p, b.dataset.clima, el); } });
    el.after(ab);
    navigator.vibrate?.(8);
    setTimeout(() => { document.addEventListener("pointerdown", fueraAbanico, true); document.addEventListener("keydown", escAbanico); }, 0);
    ab.querySelector("button")?.focus({ preventScroll: true });
  },
});

async function elegirClima(p, k, boton) {
  const antes = p.miClima, nuevo = antes === k ? null : k;
  p.climas ||= {};
  if (antes) p.climas[antes] = Math.max(0, (p.climas[antes] || 0) - 1);
  if (nuevo) p.climas[nuevo] = (p.climas[nuevo] || 0) + 1;
  p.miClima = nuevo;
  cerrarAbanico();
  const lista = boton?.classList.contains("en-lista");
  if (boton?.isConnected) { boton.outerHTML = botonClima(p, lista); }
  const nb = document.querySelector(`.clima-btn[data-arg="${CSS.escape(String(p.id))}"]`);
  if (nuevo && nb) { nb.classList.add("recien"); setTimeout(() => nb.classList.remove("recien"), 700); navigator.vibrate?.(12); }
  if (!Store.nube || p.demo) {
    const l = climasLocales(); if (nuevo) l[p.id] = nuevo; else delete l[p.id];
    try { localStorage.setItem("suroute-climas", JSON.stringify(l)); } catch {}
    return;
  }
  try {
    const sb = Store.nube.sb, yo = Store.nube.dueno;
    if (nuevo) { const { error } = await sb.from("post_climas").upsert({ post_id: p.id, user_id: yo, clima: nuevo }); if (error) throw error; }
    else { const { error } = await sb.from("post_climas").delete().eq("post_id", p.id).eq("user_id", yo); if (error) throw error; }
  } catch (e) { console.error(e); toast("No se pudo guardar tu clima." + detalleError(e)); }
}
