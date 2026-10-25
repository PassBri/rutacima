"use strict";
/* =====================================================================================
 * Red social de SuRoute: avisos (la campana), muro continuo con "Para ti" ordenado,
 * buscador de personas, perfiles públicos y primeros pasos en Comunidad.
 * Necesita supabase/2026-10-26-red-social.sql; sin esa parte de la base, todo sigue funcionando
 * como antes (la campana queda vacía).
 * ===================================================================================== */

const RED = { avisos: null, noLeidos: 0, cargando: false, prefs: null };
window.RED = RED;
const inicialesRed = n => (n || "?").split(" ").filter(Boolean).map(x => x[0]).join("").slice(0, 2).toUpperCase();
const avRed = (p, clase = "avatar") => p?.avatar ? `<img class="${clase}" src="${esc(p.avatar)}" alt="">` : `<span class="${clase}" style="--c:var(--burdeos)">${esc(inicialesRed(p?.nombre || p?.usuario))}</span>`;

/* ---------- 1. Avisos ---------- */
const TIPOS_AVISO = {
  IMPULSO: ["impulso", "le dio impulso a tu publicación", "les dieron impulso a tu publicación"],
  COMENTARIO: ["comentario", "comentó tu publicación", "comentaron tu publicación"],
  RESPUESTA: ["comentario", "respondió tu comentario", "respondieron tu comentario"],
  SEGUIDOR: ["personaMas", "empezó a seguirte", "empezaron a seguirte"],
  LIKE_COMENTARIO: ["impulso", "le dio impulso a tu comentario", "les dieron impulso a tu comentario"],
  MENCION: ["mensajes", "te mencionó", "te mencionaron"],
  RETO: ["metas", "se unió a tu reto", "se unieron a tu reto"],
  COACH: ["coach", "te escribió como coach", "te escribieron como coach"],
  SISTEMA: ["cima", "", ""],
};
const NOMBRE_AVISO = { IMPULSO: "Impulsos a tus publicaciones", COMENTARIO: "Comentarios", RESPUESTA: "Respuestas a tus comentarios", SEGUIDOR: "Nuevos seguidores",
  LIKE_COMENTARIO: "Impulsos a tus comentarios", MENCION: "Menciones", RETO: "Retos" };
const AVISOS_DEMO = () => [
  { id: 1, tipo: "IMPULSO", actor: { nombre: "Diana", usuario: "diana" }, postId: "d1", texto: "", leida: false, creadoEn: Date.now() - 6e5 },
  { id: 2, tipo: "IMPULSO", actor: { nombre: "Julián", usuario: "julian" }, postId: "d1", texto: "", leida: false, creadoEn: Date.now() - 9e5 },
  { id: 3, tipo: "COMENTARIO", actor: { nombre: "Laura", usuario: "laura" }, postId: "d1", texto: "¡Qué inspiración! ¿Cuántos días te tomó?", leida: false, creadoEn: Date.now() - 36e5 },
  { id: 4, tipo: "SEGUIDOR", actor: { nombre: "Andrés", usuario: "andres", id: "demo-andres" }, texto: "", leida: true, creadoEn: Date.now() - 864e5 },
];

async function cargarAvisos(forzar = false) {
  if (RED.cargando || (RED.avisos && !forzar)) return;
  RED.cargando = true;
  try {
    let lista;
    if (!Store.nube) lista = RED.avisos || AVISOS_DEMO();
    else {
      const { data, error } = await Store.nube.sb.from("notificaciones")
        .select("id,tipo,texto,leida,created_at,post_id,comment_id,actor:profiles!notificaciones_actor_id_fkey(id,username,nombre,avatar_url),post:posts(image_url,texto)")
        .order("created_at", { ascending: false }).limit(80);
      if (error) { RED.avisos = RED.avisos || []; return; }
      lista = data.map(n => ({ id: n.id, tipo: n.tipo, texto: n.texto, leida: n.leida, creadoEn: Date.parse(n.created_at), postId: n.post_id, commentId: n.comment_id,
        actor: n.actor ? { id: n.actor.id, nombre: n.actor.nombre || n.actor.username, usuario: n.actor.username, avatar: n.actor.avatar_url } : null,
        foto: n.post?.image_url || "", postTexto: n.post?.texto || "" }));
    }
    const antes = RED.noLeidos, nuevos = lista.filter(n => !n.leida && !(RED.avisos || []).some(x => x.id === n.id));
    RED.avisos = lista; RED.noLeidos = lista.filter(n => !n.leida).length;
    if (RED.noLeidos !== antes) pintarCampanas();
    if (forzar && nuevos.length && document.hidden) avisarNavegador(nuevos);
    if (estado.sel === "avisos") pintarDetalle();
  } finally { RED.cargando = false; }
}
window.cargarAvisos = cargarAvisos;

/** Agrupa los avisos parecidos: "Diana y 3 personas más le dieron impulso a tu publicación". */
function agruparAvisos(lista) {
  const grupos = [];
  for (const n of lista) {
    const juntable = ["IMPULSO", "LIKE_COMENTARIO", "SEGUIDOR"].includes(n.tipo);
    const g = juntable && grupos.find(x => x.tipo === n.tipo && x.postId === n.postId && x.commentId === n.commentId && x.leida === n.leida && Math.abs(x.creadoEn - n.creadoEn) < 864e5 * 2);
    if (g) g.actores.push(n.actor); else grupos.push({ ...n, actores: [n.actor] });
  }
  return grupos;
}
function fraseAviso(g) {
  const [, uno, varios] = TIPOS_AVISO[g.tipo] || TIPOS_AVISO.SISTEMA, a = g.actores.filter(Boolean);
  const nombre = x => `<b>${esc(x?.nombre || "Alguien")}</b>`;
  if (g.tipo === "SISTEMA") return esc(g.texto);
  if (a.length <= 1) return `${nombre(a[0])} ${uno}`;
  if (a.length === 2) return `${nombre(a[0])} y ${nombre(a[1])} ${varios}`;
  return `${nombre(a[0])} y ${a.length - 1} personas más ${varios}`;
}
function avisoHtml(g) {
  const [icono] = TIPOS_AVISO[g.tipo] || TIPOS_AVISO.SISTEMA;
  const destino = g.tipo === "SEGUIDOR" ? `data-ir="comunidad" data-arg="persona:${esc(g.actores[0]?.id || "")}"` : g.postId ? `data-acc="abrirAviso" data-arg="${esc(g.postId)}"` : "";
  const caras = g.actores.filter(Boolean).slice(0, 2);
  return `<button class="aviso-fila${g.leida ? "" : " nuevo"}" ${destino}>
    <span class="aviso-caras${caras.length > 1 ? " dos" : ""}">${caras.map(x => avRed(x, "aviso-av")).join("")}<span class="aviso-tipo t-${g.tipo.toLowerCase()}">${ic(icono)}</span></span>
    <span class="aviso-txt"><span>${fraseAviso(g)}${g.texto && ["COMENTARIO", "RESPUESTA", "MENCION"].includes(g.tipo) ? `: <q>${esc(g.texto)}</q>` : ""}</span><small>${hace(g.creadoEn)}</small></span>
    ${g.foto ? `<img class="aviso-foto" src="${esc(g.foto)}" alt="" loading="lazy">` : g.tipo === "SEGUIDOR" && g.actores[0]?.id && !(RED.siguiendo?.has(g.actores[0].id)) ? `<span class="btn mini lleno" data-acc="seguirPersona" data-arg="${esc(g.actores[0].id)}">Seguir</span>` : ""}</button>`;
}
DET["comunidad.avisos"] = () => {
  if (!RED.avisos) cargarAvisos();
  if (RED.noLeidos) marcarAvisosLeidos();
  const grupos = agruparAvisos(RED.avisos || []), hoy = Date.now();
  const tramo = g => hoy - g.creadoEn < 864e5 ? "Hoy" : hoy - g.creadoEn < 864e5 * 7 ? "Esta semana" : "Antes";
  let h = "", ultimo = "";
  grupos.forEach(g => { const t = tramo(g); if (t !== ultimo) { h += `<h3 class="aviso-tramo">${t}</h3>`; ultimo = t; } h += avisoHtml(g); });
  const permiso = "Notification" in window && Notification.permission === "default" && Store.nube;
  return cab(av(ic("campana"), "var(--burdeos)"), "Avisos", "Tu actividad en la comunidad",
    `<button class="btn mini" data-acc="prefsAvisos" aria-expanded="${!!estado.prefsAvisos}">${ic("ajustes")} Qué recibir</button>`) + cuerpo(
    (estado.prefsAvisos ? prefsAvisosHtml() : "") +
    (permiso ? `<div class="aviso-permiso">${ic("campana")}<span>Recibe los avisos en este navegador aunque estés en otra pestaña.</span><button class="btn mini lleno" data-acc="permisoAvisos">Activar</button></div>` : "") +
    (!RED.avisos ? `<p class="suave">Cargando…</p>` : grupos.length ? `<div class="avisos">${h}</div>`
      : `<div class="vacio-avisos">${ic("campana")}<h3>Aún no tienes avisos</h3><p class="suave">Cuando alguien impulse, comente o empiece a seguir tu ascenso, lo verás aquí.</p>
          <button class="btn lleno" data-ir="comunidad" data-arg="publicar">${ic("publicar")} Publicar un paso</button></div>`), "max-width:640px");
};
async function marcarAvisosLeidos() {
  (RED.avisos || []).forEach(n => n.leida = true); RED.noLeidos = 0; pintarCampanas();
  if (Store.nube) await Store.nube.sb.from("notificaciones").update({ leida: true }).eq("leida", false);
}
function prefsAvisosHtml() {
  const p = RED.prefs || {};
  return hoja(`<h3 style="margin:0 0 4px">Qué avisos recibir</h3><p class="suave" style="margin:0 0 10px;font-size:14px">Los mensajes y solicitudes siempre llegan.</p>
    <div class="aviso-prefs">${Object.entries(NOMBRE_AVISO).map(([k, n]) => `<label class="interruptor"><span>${n}</span><input type="checkbox" role="switch" data-pref-aviso="${k}" ${p[k] === false ? "" : "checked"}></label>`).join("")}</div>`);
}
Object.assign(ACC, {
  abrirAviso(_, postId) { ir("comunidad", "cima:" + postId); },
  prefsAvisos() { estado.prefsAvisos = !estado.prefsAvisos; if (estado.prefsAvisos && !RED.prefs) cargarPrefsAvisos(); pintarDetalle(); },
  async permisoAvisos() { try { await Notification.requestPermission(); } catch { /* sin permiso */ } pintarDetalle(); },
});
async function cargarPrefsAvisos() {
  RED.prefs = {};
  if (!Store.nube) return;
  const { data } = await Store.nube.sb.from("profiles").select("avisos").eq("id", Store.nube.dueno).maybeSingle();
  RED.prefs = data?.avisos || {}; if (estado.sel === "avisos") pintarDetalle();
}
document.addEventListener("change", async e => {
  const t = e.target.closest?.("[data-pref-aviso]"); if (!t) return;
  RED.prefs = { ...(RED.prefs || {}), [t.dataset.prefAviso]: t.checked };
  if (Store.nube) { const r = await Store.nube.sb.rpc("mis_avisos", { p_avisos: RED.prefs }); if (r.error) toast("No se pudo guardar." + (detalleError(r.error) || "")); }
});
function avisarNavegador(nuevos) {
  if (!("Notification" in window) || Notification.permission !== "granted") return;
  const g = agruparAvisos(nuevos)[0]; if (!g) return;
  const txt = fraseAviso(g).replace(/<[^>]+>/g, "");
  try { const n = new Notification("SuRoute", { body: nuevos.length > 1 ? `${txt} y ${nuevos.length - 1} avisos más` : txt, icon: "iconos/icono-192.png", tag: "suroute-avisos" });
    n.onclick = () => { window.focus(); ir("comunidad", "avisos"); n.close(); }; } catch { /* algunos navegadores no lo permiten */ }
}
/** La campana: en el riel del computador, en la lista de Comunidad y arriba de Cimas. */
const campana = (clase = "rb") => `<button class="${clase} campana" data-ir="comunidad" data-arg="avisos" aria-label="Avisos${RED.noLeidos ? `, ${RED.noLeidos} nuevos` : ""}" title="Avisos">${clase === "rb" ? `<span class="rb-ic">${ic("campana")}</span><span class="tip">Avisos</span>` : ic("campana")}${RED.noLeidos ? `<span class="campana-n">${RED.noLeidos > 99 ? "99+" : RED.noLeidos}</span>` : ""}</button>`;
window.campana = campana;
function puntoComunidad() {
  const b = $("riel")?.querySelector('[data-sec="comunidad"]'); if (!b) return;
  const p = b.querySelector(".punto-avisos");
  if (RED.noLeidos && !p) b.insertAdjacentHTML("beforeend", `<span class="punto punto-avisos"></span>`); else if (!RED.noLeidos) p?.remove();
}
function pintarCampanas() {
  puntoComunidad();
  document.querySelectorAll(".campana").forEach(b => {
    const n = b.querySelector(".campana-n");
    if (!RED.noLeidos) n?.remove();
    else if (n) n.textContent = RED.noLeidos > 99 ? "99+" : RED.noLeidos;
    else b.insertAdjacentHTML("beforeend", `<span class="campana-n">${RED.noLeidos > 99 ? "99+" : RED.noLeidos}</span>`);
    b.setAttribute("aria-label", `Avisos${RED.noLeidos ? `, ${RED.noLeidos} nuevos` : ""}`);
  });
}
{
  // En el riel no va otra campana (sería un control repetido): Comunidad lleva un punto cuando hay avisos nuevos
  const riel = pintarRiel;
  globalThis.pintarRiel = function () { riel(); puntoComunidad(); };
  // En la lista de Comunidad (teléfono y computador), junto al botón de publicar
  const lista = LISTAS.comunidad;
  LISTAS.comunidad = (...a) => lista(...a).replace(/(<span style="display:flex;gap:4px;align-items:center">)/, `$1${campana("rb campana-lista")}`);
  // Arriba de Cimas, junto a Mensajes
  const feed = DET["comunidad.feed"];
  DET["comunidad.feed"] = (...a) => feed(...a).replace(/(<button class="cimas-ic" data-sec="mensajes")/, `${campana("cimas-ic")}$1`);
}
// Revisa los avisos al entrar y cada minuto mientras la página está abierta
setTimeout(() => cargarAvisos(), 1500);
setInterval(() => { if (Store.nube) cargarAvisos(true); }, 60_000);
document.addEventListener("visibilitychange", () => { if (!document.hidden && Store.nube) cargarAvisos(true); });
