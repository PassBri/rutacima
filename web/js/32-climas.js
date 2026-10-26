"use strict";
/* ======================================================================
 * Climas: los íconos de clima de montaña (los mismos de la app, ui/components/ClimaIconos.kt) y la
 * reacción "¿Qué clima te deja?" de la comunidad, debajo del corazón.
 * · Cinco reacciones: sol (me ilumina), tormenta (me motiva), niebla (me identifica), valle (me da calma)
 *   y cumbre (me impulsa a subir).
 * · El botón muestra el clima que más dejó la comunidad dentro de un anillo de cielo: cada tramo del
 *   anillo es la parte de cada clima. Al tocarlo se abre el abanico de climas; tocar el tuyo lo quita.
 * · En la nube vive en la tabla post_climas (supabase/2026-10-28-climas.sql). Sin cuenta, en este navegador.
 * ====================================================================== */
/** Material 3: cada clima es un círculo de color con su símbolo de Material (Rounded, relleno)
 * en blanco. [forma de 48×48, color, símbolo de 960×960]. Iguales en la app (ui/components/ClimaIconos.kt). */
const CLIMA_FORMAS = {
  sol: ["M2 24a22 22 0 1 0 44 0a22 22 0 1 0-44 0", "#E8A317", "M458.5-778.63Q450-787.25 450-800v-90q0-12.75 8.68-21.38 8.67-8.62 21.5-8.62 12.82 0 21.32 8.62 8.5 8.63 8.5 21.38v90q0 12.75-8.68 21.37-8.67 8.63-21.5 8.63-12.82 0-21.32-8.63ZM685-685q-9-9-9-21.1 0-12.1 9-20.9l63-64q9.07-9 21.53-9 12.47 0 21.47 9 9 9 9 21t-9 21l-64 64q-9 9-21 9t-21-9Zm115 235q-12.75 0-21.37-8.68-8.63-8.67-8.63-21.5 0-12.82 8.63-21.32 8.62-8.5 21.37-8.5h90q12.75 0 21.38 8.68 8.62 8.67 8.62 21.5 0 12.82-8.62 21.32-8.63 8.5-21.38 8.5h-90ZM458.5-48.63Q450-57.25 450-70v-90q0-12.75 8.68-21.38 8.67-8.62 21.5-8.62 12.82 0 21.32 8.62 8.5 8.63 8.5 21.38v90q0 12.75-8.68 21.37-8.67 8.63-21.5 8.63-12.82 0-21.32-8.63ZM233-685l-64-63q-9-8.87-9-21.43 0-12.57 9.39-21.57 8.61-9 20.61-9t21 9l64 64q9 9 9 21t-9 21q-9.27 8-21.64 8-12.36 0-20.36-8Zm516 516-64-64q-9-8.8-9-20.9 0-12.1 9-21.1 8.25-8 20.63-8 12.37 0 21.37 8l65 63q9 9 8.63 21.6-.37 12.61-8.53 21.5-8.89 8.9-21.5 8.9-12.6 0-21.6-9ZM70-450q-12.75 0-21.37-8.68-8.63-8.67-8.63-21.5 0-12.82 8.63-21.32Q57.25-510 70-510h90q12.75 0 21.38 8.68 8.62 8.67 8.62 21.5 0 12.82-8.62 21.32-8.63 8.5-21.38 8.5H70Zm99 280.61q-9-8.61-9-20.61t9-21l64-64q8.25-8 20.63-8 12.37 0 21.37 8.25 9 9 9 21.37 0 12.38-9 21.38l-63 63q-8.87 9-21.43 9-12.57 0-21.57-9.39ZM310-310q-70-70-70-170t70-170q70-70 170-70t170 70q70 70 70 170t-70 170q-70 70-170 70t-170-70Z"],
  tormenta: ["M2 24a22 22 0 1 0 44 0a22 22 0 1 0-44 0", "#E0662A", "M360-360H217q-18 0-26.5-16t2.5-31l338-488q8-11 20-15t24 1q12 5 19 16t5 24l-39 309h176q19 0 27 17t-4 32L388-66q-8 10-20.5 13T344-55q-11-5-17.5-16T322-95l38-265Z"],
  niebla: ["M2 24a22 22 0 1 0 44 0a22 22 0 1 0-44 0", "#7A8896", "M720-205q-14 0-24.5-10.5T685-240q0-14 10.5-24.5T720-275q14 0 24.5 10.5T755-240q0 14-10.5 24.5T720-205ZM280-80q-14 0-24.5-10.5T245-115q0-14 10.5-24.5T280-150q14 0 24.5 10.5T315-115q0 14-10.5 24.5T280-80Zm-40-125q-14 0-24.5-10.5T205-240q0-14 10.5-24.5T240-275h360q14 0 24.5 10.5T635-240q0 14-10.5 24.5T600-205H240ZM400-80q-14 0-24.5-10.5T365-115q0-14 10.5-24.5T400-150h280q14 0 24.5 10.5T715-115q0 14-10.5 24.5T680-80H400ZM290-340q-87 0-148.5-61.5T80-550q0-79 56.5-141T277-759q32-56 84.5-88.5T480-880q91 0 152.5 57.5T708-680q79 4 125.5 53.5T880-510q0 70-49.5 120T710-340H290Z"],
  valle: ["M2 24a22 22 0 1 0 44 0a22 22 0 1 0-44 0", "#3E8A5A", "m76-288 180-240q5-6 11-9t13-3q7 0 13 3t11 9l162 216q5 6 11 9t13 3q19 0 27-16.5t-3-31.5L411-485q-6-8-6-18t6-18l125-167q5-6 11-9t13-3q7 0 13 3t11 9l300 400q11 15 3 31.5T860-240H100q-19 0-27-16.5t3-31.5Z"],
  cumbre: ["M2 24a22 22 0 1 0 44 0a22 22 0 1 0-44 0", "#C0473A", "M260-429v279q0 12.75-8.68 21.37-8.67 8.63-21.5 8.63-12.82 0-21.32-8.63-8.5-8.62-8.5-21.37v-620q0-12.75 8.63-21.38Q217.25-800 230-800h289q10.5 0 18.75 6.5T548-776l14 62h208q12.75 0 21.38 8.62Q800-696.75 800-684v310q0 12.75-8.62 21.37Q782.75-344 770-344H568q-10.5 0-18.75-6T539-367l-14-62H260Z"],
};
const REAC_CLIMA = [
  ["sol", "Me ilumina", "#E8A317"], ["tormenta", "Me motiva", "#E0662A"], ["niebla", "Me identifica", "#7A8896"],
  ["valle", "Me da calma", "#3E8A5A"], ["cumbre", "Me impulsa a subir", "#C0473A"],
];
function climaSvg(k, cls = "clima-ic") {
  const f = CLIMA_FORMAS[k]; if (!f) return "";
  return `<svg class="${cls}" viewBox="0 0 48 48" aria-hidden="true"><path d="${f[0]}" fill="${f[1]}"/><path transform="translate(11 37) scale(.02708)" d="${f[2]}" fill="#fff"/></svg>`;
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
    ab.innerHTML = REAC_CLIMA.map(([k, n], i) => `<button role="menuitemradio" aria-checked="${p.miClima === k}" class="${p.miClima === k ? "si" : ""}" style="--i:${i}" data-clima="${k}" title="${n}"><span class="ca-ic">${climaSvg(k)}<em class="ca-n">${p.climas?.[k] || 0}</em></span><small>${n}</small></button>`).join("");
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
