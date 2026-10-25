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

/* ---------- 2. Muro continuo y "Para ti" ordenado ---------- */
/** A quiénes sigo (una vez por sesión; se actualiza al seguir o dejar de seguir). */
async function seguidosRed() {
  if (RED.siguiendo) return RED.siguiendo;
  if (!Store.nube) { RED.siguiendo = listaLocal("sigo-demo"); return RED.siguiendo; }
  const { data } = await Store.nube.sb.from("follows").select("followed_id").eq("follower_id", Store.nube.dueno);
  RED.siguiendo = new Set((data || []).map(x => x.followed_id)); return RED.siguiendo;
}
window.seguidosRed = seguidosRed;
/** Mis ejes para la comunidad (los elijo en los primeros pasos; viajan con la app). */
const misEjesRed = () => new Set(String(respuesta("red:ejes") || "").split(",").filter(Boolean));
/**
 * "Para ti": lo reciente con apoyo, de quienes sigo y de mis ejes, con variedad de personas.
 * Puntaje = apoyo (impulsos y comentarios) que se apaga con las horas × bonos (sigo, mi eje, foto).
 */
async function ordenarParaTi(ps) {
  const sigo = await seguidosRed(), ejes = misEjesRed(), ahora = Date.now();
  const puntaje = p => {
    const h = Math.max(0, (ahora - p.creadoEn) / 36e5);
    return (1 + Math.log2(1 + (p.impulsos || 0)) * 1.5 + Math.log2(1 + (p.comentarios || 0)) * 2) / Math.pow(h + 2, 0.75)
      * (sigo.has(p.autorId) ? 1.8 : 1) * (p.eje && ejes.has(p.eje) ? 1.4 : 1) * (p.foto ? 1.15 : 1) * (p.propio ? 0.6 : 1);
  };
  const resto = [...ps].sort((a, b) => puntaje(b) - puntaje(a)), r = [];
  // Variedad: no más de una publicación seguida de la misma persona si hay otras
  while (resto.length) { const i = resto.findIndex(p => r.at(-1)?.autorId !== p.autorId); r.push(resto.splice(i < 0 ? 0 : i, 1)[0]); }
  return r;
}
window.ordenarParaTi = ordenarParaTi;
{
  // Al llegar al final se cargan más (en Cimas, en el muro y en la lista)
  const feed = DET["comunidad.feed"];
  DET["comunidad.feed"] = (...a) => {
    let h = feed(...a);
    if (estado.feedMas) {
      h = h.replace(/<article class="cima cima-fin"[\s\S]*?<\/article>/, `<article class="cima cima-cargando" data-mas-feed aria-label="Cargando más"><div class="cima-fondo"></div><span class="cargando-mas">${ic("cima")} Cargando más cimas…</span></article>`);
      if (estado.vistaCom !== "cimas") h = h.replace(/(<\/div><\/div>)$/, `<div class="mas-feed" data-mas-feed>${ic("cima")} Cargando más…</div>$1`);
    }
    return h;
  };
  const lista = LISTAS.comunidad;
  LISTAS.comunidad = (...a) => { const h = lista(...a); return estado.feedMas ? h.replace(/<\/div>$/, `<div class="mas-feed" data-mas-feed>Cargando más…</div></div>`) : h; };
  const vigia = new IntersectionObserver(es => es.forEach(e => { if (e.isIntersecting) { vigia.unobserve(e.target); cargarFeed(true); } }), { rootMargin: "600px 0px" });
  new MutationObserver(() => document.querySelectorAll("[data-mas-feed]:not([data-visto])").forEach(el => { el.dataset.visto = "1"; vigia.observe(el); }))
    .observe(document.body, { childList: true, subtree: true });
  // Cambiar de pestaña (Para ti, Visión, Siguiendo) empieza de nuevo
  document.addEventListener("click", e => { if (e.target.closest?.("[data-feed]")) { estado.feedN = 0; estado.feedMas = false; } }, true);
}

/* ---------- 3. Buscar personas y perfiles públicos ---------- */
const demoPersonas = () => DEMO_POSTS.filter((p, i, a) => a.findIndex(x => x.autorNombre === p.autorNombre) === i)
  .map(p => ({ id: p.autorId, nombre: p.autorNombre, usuario: p.autorNombre.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().replace(/[^a-z]+/g, ".").replace(/\.$/, ""), avatar: "", cumbre: p.metaTitulo || "", bio: "" }));
const filaPersonaHtml = x => `<div class="persona-fila"><button class="persona-abrir" data-ir="comunidad" data-arg="persona:${esc(x.id)}">${avRed(x, "aviso-av")}
    <span><b>${esc(x.nombre)}</b><small>@${esc(x.usuario)}${x.cumbre ? ` · ${esc(x.cumbre)}` : ""}</small></span></button>${botonSeguir(x.id)}</div>`;
const botonSeguir = id => { const si = RED.siguiendo?.has(id); return `<button class="btn mini ${si ? "" : "lleno"}" data-acc="seguirPersona" data-arg="${esc(id)}" aria-pressed="${!!si}">${si ? "Siguiendo" : "Seguir"}</button>`; };
window.botonSeguir = botonSeguir;

async function buscarPersonas(t) {
  t = t.trim().replace(/^@/, "").replace(/[,()*%]/g, " ").trim();
  if (t.length < 2) { RED.resultados = null; return pintarResultados(); }
  RED.buscando = t;
  let r;
  if (!Store.nube) r = demoPersonas().filter(x => (x.nombre + " " + x.usuario).toLowerCase().includes(t.toLowerCase()));
  else {
    const { data } = await Store.nube.sb.from("profiles").select("id,username,nombre,avatar_url,cumbre").or(`username.ilike.*${t}*,nombre.ilike.*${t}*`).limit(25);
    r = (data || []).filter(x => x.id !== Store.nube.dueno).map(x => ({ id: x.id, nombre: x.nombre || x.username, usuario: x.username, avatar: x.avatar_url, cumbre: x.cumbre }));
  }
  if (RED.buscando !== t) return;
  RED.resultados = r; pintarResultados();
}
/** A quién seguir: quienes publican seguido en mis ejes (y si no alcanza, los más activos), sin los que ya sigo. */
async function sugeridasRed(cuantas = 8) {
  const sigo = await seguidosRed();
  if (!Store.nube) return demoPersonas().filter(x => !sigo.has(x.id)).slice(0, cuantas);
  const ejes = [...misEjesRed()];
  let q = Store.nube.sb.from("posts").select("user_id,autor:profiles!posts_user_id_fkey(id,username,nombre,avatar_url,cumbre)").eq("visibilidad", "PUBLICA").order("created_at", { ascending: false }).limit(200);
  if (ejes.length) q = q.in("eje", ejes);
  const { data } = await q, cuenta = {};
  (data || []).forEach(x => cuenta[x.user_id] = (cuenta[x.user_id] || 0) + 1);
  const vistas = new Set([Store.nube.dueno, ...sigo]);
  return (data || []).map(x => x.autor).filter(a => a && !vistas.has(a.id) && vistas.add(a.id))
    .sort((a, b) => cuenta[b.id] - cuenta[a.id]).slice(0, cuantas)
    .map(a => ({ id: a.id, nombre: a.nombre || a.username, usuario: a.username, avatar: a.avatar_url, cumbre: a.cumbre }));
}
window.sugeridasRed = sugeridasRed;
function pintarResultados() {
  const caja = document.getElementById("resultadosPersonas"); if (!caja) return;
  caja.innerHTML = RED.resultados ? (RED.resultados.length ? RED.resultados.map(filaPersonaHtml).join("") : `<p class="suave">No encontramos a nadie con ese nombre.</p>`)
    : RED.sugeridas?.length ? `<h3 class="aviso-tramo">A quién seguir</h3>${RED.sugeridas.map(filaPersonaHtml).join("")}` : "";
}
DET["comunidad.buscar"] = () => {
  if (!RED.sugeridas) seguidosRed().then(() => sugeridasRed(10)).then(s => { RED.sugeridas = s; pintarResultados(); });
  requestAnimationFrame(() => { const i = document.getElementById("buscarPersonas"); if (i && document.activeElement !== i) i.focus({ preventScroll: true }); pintarResultados(); });
  return cab(av(ic("buscar"), "var(--burdeos)"), "Buscar personas", "Por nombre o @usuario") + cuerpo(
    `<label class="buscador-red">${ic("buscar")}<input id="buscarPersonas" type="search" placeholder="Nombre o @usuario" autocomplete="off" autocapitalize="none" spellcheck="false" enterkeyhint="search"></label>
     <div id="resultadosPersonas" class="personas"></div>`, "max-width:640px");
};
let esperaBusqueda = null;
document.addEventListener("input", e => { if (e.target.id !== "buscarPersonas") return; clearTimeout(esperaBusqueda); esperaBusqueda = setTimeout(() => buscarPersonas(e.target.value), 280); });

/** Perfil público: quién es, sus cifras, su cumbre y lo que comparte. */
async function cargarPersona(id) {
  (RED.personas ||= {})[id] = { cargando: true };
  await seguidosRed();
  let r;
  if (!Store.nube) {
    const x = demoPersonas().find(p => p.id === id) || { id, nombre: "Senderista", usuario: "senderista" };
    r = { ...x, seguidores: 24, siguiendo: 31, meSigue: false, posts: DEMO_POSTS.filter(p => p.autorId === id) };
  } else {
    const sb = Store.nube.sb, yo = Store.nube.dueno;
    const [p, seg, sig, me, ps] = await Promise.all([
      sb.from("profiles").select("id,username,nombre,avatar_url,bio,cumbre").eq("id", id).maybeSingle(),
      sb.from("follows").select("*", { count: "exact", head: true }).eq("followed_id", id),
      sb.from("follows").select("*", { count: "exact", head: true }).eq("follower_id", id),
      sb.from("follows").select("followed_id").eq("follower_id", id).eq("followed_id", yo),
      sb.from("posts").select("id,tipo,texto,image_url,eje,meta_titulo,created_at,brujula,impulsos:votes(count),comentarios:comments(count)").eq("user_id", id).order("created_at", { ascending: false }).limit(60),
    ]);
    if (!p.data) { RED.personas[id] = { noExiste: true }; return pintarDetalle(); }
    const x = p.data;
    r = { id, nombre: x.nombre || x.username, usuario: x.username, avatar: x.avatar_url, bio: x.bio, cumbre: x.cumbre, seguidores: seg.count || 0, siguiendo: sig.count || 0,
      meSigue: !!me.data?.length, propio: id === yo,
      posts: (ps.data || []).map(q => ({ id: q.id, autorId: id, autorNombre: x.nombre || x.username, autorUsuario: x.username, tipo: q.tipo, texto: q.texto, foto: q.image_url, eje: q.eje,
        metaTitulo: q.meta_titulo, brujula: q.brujula, creadoEn: Date.parse(q.created_at), impulsos: q.impulsos?.[0]?.count || 0, comentarios: q.comentarios?.[0]?.count || 0 })) };
  }
  RED.personas[id] = r;
  // Sus publicaciones también abren en Cimas: se suman al muro cargado si no estaban
  (estado.feed ||= []); r.posts.forEach(p => { if (!estado.feed.some(x => x.id === p.id)) estado.feed.push(p); });
  if (estado.sel === "persona:" + id) pintarDetalle();
}
DET["comunidad.persona"] = id => {
  const p = RED.personas?.[id];
  if (!p) { cargarPersona(id); return cab(av(ic("perfil"), "var(--burdeos)"), "Perfil", "Cargando…") + cuerpo(""); }
  if (p.cargando) return cab(av(ic("perfil"), "var(--burdeos)"), "Perfil", "Cargando…") + cuerpo("");
  if (p.noExiste) return cab(av(ic("perfil"), "var(--burdeos)"), "Perfil", "") + cuerpo(hoja(`<p>Esta persona ya no está en la comunidad.</p>`));
  const cifra = (n, t) => `<span><b>${n.toLocaleString("es")}</b><small>${t}</small></span>`;
  return cab(avRed(p), esc(p.nombre), "@" + esc(p.usuario)) + cuerpo(
    `<section class="perfil-pub">${avRed(p, "perfil-pub-av")}<h2>${esc(p.nombre)}</h2>${p.meSigue && !p.propio ? `<span class="te-sigue">Te sigue</span>` : ""}
      <div class="perfil-cifras">${cifra(p.posts.length, "Publicaciones")}${cifra(p.seguidores, "Seguidores")}${cifra(p.siguiendo, "Siguiendo")}</div>
      ${p.bio ? `<p class="perfil-bio">${esc(p.bio)}</p>` : ""}${p.cumbre ? `<p class="perfil-cumbre">${ic("cima")} ${esc(p.cumbre)}</p>` : ""}
      ${p.propio ? "" : `<div class="botones">${botonSeguir(p.id).replace("btn mini", "btn")}<button class="btn" data-acc="escribirA" data-arg="${esc(p.id)}|${esc(p.nombre)}">${ic("mensajes")} Mensaje</button></div>`}</section>
    ${p.posts.length ? `<div class="perfil-grilla">${p.posts.map(q => `<button class="perfil-celda" data-ir="comunidad" data-arg="cima:${esc(q.id)}" aria-label="${esc((q.texto || "").slice(0, 60))}">
        ${q.foto ? `<img src="${esc(q.foto)}" alt="" loading="lazy">` : `<span>${esc((q.texto || "").slice(0, 90))}</span>`}<small>${ic("impulso")} ${q.impulsos}</small></button>`).join("")}</div>`
      : `<p class="suave" style="text-align:center">Todavía no comparte publicaciones.</p>`}`, "max-width:720px");
};
ACC.seguirPersona = async (el, id) => {
  await seguidosRed(); const si = !RED.siguiendo.has(id);
  si ? RED.siguiendo.add(id) : RED.siguiendo.delete(id);
  document.querySelectorAll(`[data-acc="seguirPersona"][data-arg="${CSS.escape(id)}"]`).forEach(b => { b.textContent = si ? "Siguiendo" : "Seguir"; b.classList.toggle("lleno", !si); b.setAttribute("aria-pressed", si); });
  const pp = RED.personas?.[id]; if (pp && !pp.cargando) pp.seguidores += si ? 1 : -1;
  if (!Store.nube) { try { localStorage.setItem("rutacima-sigo-demo", JSON.stringify([...RED.siguiendo])); } catch { /* sin almacenamiento */ } return; }
  const sb = Store.nube.sb, yo = Store.nube.dueno;
  const r = si ? await sb.from("follows").upsert({ follower_id: yo, followed_id: id }) : await sb.from("follows").delete().eq("follower_id", yo).eq("followed_id", id);
  if (r.error) errorNube(r.error);
};
{
  // La persona de una publicación abre su perfil (en Cimas, en el muro y en la publicación)
  const feed = DET["comunidad.feed"];
  DET["comunidad.feed"] = (...a) => feed(...a)
    .replace(/<button class="cima-autor" data-acc="escribirA" data-arg="([^"|]*)\|[^"]*"/g, '<button class="cima-autor" data-ir="comunidad" data-arg="persona:$1"')
    .replace(/(<button class="cimas-ic" data-sec="mensajes")/, `<button class="cimas-ic" data-ir="comunidad" data-arg="buscar" aria-label="Buscar personas" title="Buscar personas">${ic("buscar")}</button>$1`);
  const lista = LISTAS.comunidad;
  LISTAS.comunidad = (...a) => lista(...a).replace(/(<button class="rb campana)/, `<button class="rb" data-ir="comunidad" data-arg="buscar" aria-label="Buscar personas" title="Buscar personas">${ic("buscar")}</button>$1`);
}

/* ---------- 4. Primeros pasos en Comunidad ---------- */
const primerosPasosHechos = () => !!respuesta("red:inicio");
{
  // La primera vez que se entra a Comunidad: elegir ejes y seguir a algunas personas
  const ir0 = ir;
  globalThis.ir = (sec, sel = null) => {
    if (sec === "comunidad" && (sel === "feed" || sel === null) && !primerosPasosHechos() && Store.lista("respuesta").length) sel = "bienvenida";
    return ir0(sec, sel);
  };
}
DET["comunidad.bienvenida"] = () => {
  const ejes = estado.ejesRed ||= misEjesRed();
  if (!RED.bienvenida) { RED.bienvenida = []; seguidosRed().then(() => sugeridasRed(6)).then(s => { RED.bienvenida = s; if (estado.sel === "bienvenida") pintarDetalle(); }); }
  const sigo = RED.bienvenida.filter(x => RED.siguiendo?.has(x.id)).length;
  return cab(av(ic("comunidad"), "var(--burdeos)"), "Bienvenido a la comunidad", "Dos pasos para llenar tu muro") + cuerpo(
    hoja(`<h3 class="pp-t"><span>1</span>Elige los ejes que más te mueven</h3>
      <div class="pp-ejes">${EJES.map(([c, n]) => `<button class="pp-eje" data-acc="ejeRed" data-arg="${c}" aria-pressed="${ejes.has(c)}" style="--c:${COLOR_EJE[c]}">${ejes.has(c) ? ic("check") : ""}${esc(n)}</button>`).join("")}</div>`) +
    hoja(`<h3 class="pp-t"><span>2</span>Sigue a algunas personas${sigo ? ` <small>· sigues a ${sigo}</small>` : ""}</h3>
      <div class="personas">${RED.bienvenida.length ? RED.bienvenida.map(filaPersonaHtml).join("") : `<p class="suave">Buscando personas que suben en tus ejes…</p>`}</div>`) +
    `<div class="botones pp-botones"><button class="btn lleno" data-acc="terminarBienvenida" data-arg="1">${ic("check")} Listo, ver mi muro</button><button class="btn" data-acc="terminarBienvenida" data-arg="">Ahora no</button></div>`, "max-width:640px");
};
Object.assign(ACC, {
  ejeRed(_, c) {
    const s = estado.ejesRed ||= misEjesRed(); s.has(c) ? s.delete(c) : s.add(c);
    RED.bienvenida = null; pintarDetalle();   // las sugerencias siguen a los ejes elegidos
    responder("red", "red:ejes", [...s].join(","));
  },
  terminarBienvenida() {
    responder("red", "red:inicio", "1");
    if (estado.ejesRed) responder("red", "red:ejes", [...estado.ejesRed].join(","));
    estado.feed = null; estado.feedN = 0; estado.vistaCom = "cimas"; ir("comunidad", "feed");
  },
});
