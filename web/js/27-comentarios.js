/* =====================================================================================
 * Comentarios al estilo de las redes de video corto: "N comentarios" arriba, cada
 * comentario con su persona, el tiempo, "Responder", sus respuestas plegadas y un
 * banderín de cumbre para dar ánimo. Guardar se hace con la mochila.
 * Si la base aún no tiene respuestas ni banderines de comentarios (supabase/
 * 2026-10-25-comentarios.sql), todo sigue funcionando como comentarios simples.
 * ===================================================================================== */

(estado.verResp ||= new Set());
const COM_DEMO = [{ id: "d1", autor: "Diana", texto: "¡Qué inspiración! Gracias por compartirlo.", creadoEn: Date.now() - 864e5, likes: 12 },
  { id: "d2", autor: "Julián", texto: "Vamos, sigue así. Nos vemos en la cima.", creadoEn: Date.now() - 36e5 * 5, likes: 4 },
  { id: "d3", autor: "Laura", texto: "¿Cuántos días te tomó?", creadoEn: Date.now() - 36e5 * 3, likes: 1, parentId: "d1" }];

globalThis.cargarComentarios = async function (p) {
  await null;
  if (p.demo || !Store.nube) {
    p.comentariosLista = [...COM_DEMO, ...(demoComentarios[p.id] || []).map((c, i) => ({ id: "m" + i, creadoEn: Date.now(), likes: 0, ...c }))]
      .map(c => ({ ...c, yoLike: !!estado.likesDemo?.has(c.id) }));
  } else {
    const sb = Store.nube.sb, yo = Store.nube.dueno;
    let r = await sb.from("comments").select("id,texto,created_at,user_id,parent_id,autor:profiles!comments_user_id_fkey(username,nombre,avatar_url),likes:comment_likes(count)").eq("post_id", p.id).order("created_at");
    const completo = !r.error;
    if (r.error) r = await sb.from("comments").select("id,texto,created_at,user_id,autor:profiles!comments_user_id_fkey(username,nombre,avatar_url)").eq("post_id", p.id).order("created_at");
    const lista = r.data || [];
    let mios = new Set();
    if (completo && lista.length) {
      const l = await sb.from("comment_likes").select("comment_id").eq("user_id", yo).in("comment_id", lista.map(c => c.id));
      if (!l.error) mios = new Set((l.data || []).map(x => x.comment_id));
    }
    estado.comCompletos = completo;
    p.comentariosLista = lista.map(c => ({ id: c.id, autor: c.autor?.nombre || c.autor?.username || "", usuario: c.autor?.username || "", avatar: c.autor?.avatar_url || "",
      texto: c.texto, creadoEn: Date.parse(c.created_at), parentId: c.parent_id || null, likes: c.likes?.[0]?.count || 0, yoLike: mios.has(c.id), propio: c.user_id === yo }));
  }
  p.comentarios = p.comentariosLista.length;
  if (estado.sel === "post:" + p.id && !enEdicion()) pintarDetalle();
};

const avatarCom = c => c.avatar ? `<img class="com-av" src="${esc(c.avatar)}" alt="">`
  : `<span class="com-av">${esc((c.autor || "?").split(" ").map(x => x[0]).join("").slice(0, 2).toUpperCase())}</span>`;
const filaCom = (c, hija = false) => `<div class="com${hija ? " hija" : ""}" data-com="${esc(c.id)}">${avatarCom(c)}
    <div class="com-cuerpo"><span class="com-quien">${esc(c.autor)}</span><p>${esc(c.texto)}</p>
      <div class="com-pie"><span>${hace(c.creadoEn)}</span><button data-acc="responderCom" data-arg="${esc(c.parentId || c.id)}|${esc(c.autor)}">Responder</button></div></div>
    <button class="com-ban" data-acc="banderaCom" data-arg="${esc(c.id)}" aria-pressed="${!!c.yoLike}" aria-label="Me gusta, ${c.likes || 0}">${ic("impulso")}<b>${c.likes || ""}</b></button></div>`;
/** La lista: comentarios principales y, debajo de cada uno, sus respuestas plegadas. */
function listaComentariosHtml(p) {
  const l = p.comentariosLista;
  if (!l) return `<p class="suave hoja-vacia">Cargando…</p>`;
  if (!l.length) return `<p class="suave hoja-vacia">Aún no hay comentarios. Sé la primera persona en dar ánimo.</p>`;
  const raiz = l.filter(c => !c.parentId || !l.some(x => x.id === c.parentId));
  return `<div class="coms">${raiz.map(c => {
    const hijas = l.filter(x => x.parentId === c.id), abiertas = estado.verResp.has(c.id);
    return filaCom(c) + (hijas.length ? `<div class="com-resp">${abiertas ? hijas.map(h => filaCom(h, true)).join("") : ""}
      <button class="com-ver" data-acc="verRespuestas" data-arg="${esc(c.id)}"><i></i>${abiertas ? "Ocultar respuestas" : `Ver ${hijas.length} ${hijas.length === 1 ? "respuesta" : "respuestas"}`} ${ic(abiertas ? "arriba" : "abajo")}</button></div>` : "");
  }).join("")}</div>`;
}
globalThis.listaComentariosHtml = listaComentariosHtml;
const escribirCom = () => { const r = estado.respondiendo, p = perfil();
  return `${r ? `<div class="com-respondiendo">Respondiendo a <b>${esc(r.autor)}</b><button data-acc="responderCom" data-arg="" aria-label="Cancelar respuesta">${ic("cerrar")}</button></div>` : ""}
    <form class="escribir hoja-escribir com-escribir" data-form="comentar">${(globalThis.mv?.demo?.avatar || p.fotoPerfilUrl) ? `<img class="com-av" src="${esc(globalThis.mv?.demo?.avatar || p.fotoPerfilUrl)}" alt="">` : `<span class="com-av">${esc((p.nombre || "Y")[0].toUpperCase())}</span>`}
      <input name="texto" placeholder="${r ? "Escribe tu respuesta…" : "Agregar comentario…"}" aria-label="Comentario" autocomplete="off" maxlength="1000"><button class="enviar" aria-label="Enviar">${ic("enviar")}</button></form>`; };

globalThis.pintarHojaComentarios = function (p, abrir = false) {
  const n = (p.comentariosLista || []).length || p.comentarios || 0;
  const html = `<div class="com-cab"><span></span><h3 class="hoja-titulo">${n} ${n === 1 ? "comentario" : "comentarios"}</h3><button data-cerrar-hoja aria-label="Cerrar">${ic("cerrar")}</button></div>
    <div class="com-lista">${listaComentariosHtml(p)}</div>${escribirCom()}`;
  const actual = document.getElementById("hojaInferior");
  if (abrir || !actual) { abrirHoja(html); return; }
  const lista = actual.querySelector(".com-lista"), alto = lista?.scrollTop || 0;
  actual.querySelector(".hoja-panel").innerHTML = `<span class="hoja-asa" aria-hidden="true"></span>${html}`;
  const nueva = actual.querySelector(".com-lista"); if (nueva) nueva.scrollTop = alto;
};
/** En la publicación abierta, la misma lista en lugar de las burbujas. */
{
  const base = DET["comunidad.post"];
  DET["comunidad.post"] = id => base(id).replace(/<div class="chat" style="max-width:none">[\s\S]*?<\/div>\s*<\/div><\/div>\s*<form class="escribir" data-form="comentar">[\s\S]*?<\/form>/,
    () => { const p = estado.post; return p ? `<div class="com-lista en-detalle">${listaComentariosHtml(p)}</div></div></div>${escribirCom()}` : ""; });
}
const repintarComs = () => { const p = estado.post; if (!p) return; if (document.querySelector("#hojaInferior .com-lista")) pintarHojaComentarios(p); else pintarDetalle(); };

ACC.verRespuestas = (el, id) => { estado.verResp.has(id) ? estado.verResp.delete(id) : estado.verResp.add(id); repintarComs(); };
ACC.responderCom = (el, arg) => {
  if (!arg) estado.respondiendo = null;
  else { const [id, autor] = arg.split("|"); estado.respondiendo = { id, autor }; estado.verResp.add(id); }
  repintarComs();
  requestAnimationFrame(() => { const i = document.querySelector("#hojaInferior .com-escribir input") || document.querySelector("#detalle .com-escribir input"); if (i) { i.focus(); if (estado.respondiendo && !i.value) i.value = "@" + estado.respondiendo.autor.split(" ")[0] + " "; } });
};
ACC.banderaCom = async (el, id) => {
  const p = estado.post, c = p?.comentariosLista?.find(x => String(x.id) === String(id)); if (!c) return;
  const poner = !c.yoLike; c.yoLike = poner; c.likes = Math.max(0, (c.likes || 0) + (poner ? 1 : -1));
  el.setAttribute("aria-pressed", poner); el.querySelector("b").textContent = c.likes || "";
  if (poner && !pocoMovimiento?.()) el.animate([{ transform: "scale(1)" }, { transform: "scale(1.25)" }, { transform: "scale(1)" }], { duration: 260, easing: "cubic-bezier(.2,0,0,1)" });
  if (p.demo || !Store.nube) { (estado.likesDemo ||= new Set())[poner ? "add" : "delete"](c.id); return; }
  const sb = Store.nube.sb, yo = Store.nube.dueno;
  const r = poner ? await sb.from("comment_likes").insert({ comment_id: c.id, user_id: yo }) : await sb.from("comment_likes").delete().eq("comment_id", c.id).eq("user_id", yo);
  if (r.error) { c.yoLike = !poner; c.likes += poner ? -1 : 1; el.setAttribute("aria-pressed", !poner); el.querySelector("b").textContent = c.likes || "";
    toast(estado.comCompletos === false ? "Falta activar los banderines de comentarios en la base de datos." : "No se pudo guardar." + (detalleError(r.error) || "")); }
};
/** Enviar una respuesta: va unida al comentario (si la base ya lo permite). */
document.addEventListener("submit", async e => {
  const f = e.target.closest?.('form[data-form="comentar"]'); if (!f || !estado.respondiendo) return;
  e.preventDefault(); e.stopImmediatePropagation();
  const p = estado.post, texto = f.texto.value.trim(); if (!texto || !p) return;
  const padre = estado.respondiendo.id; f.reset(); estado.respondiendo = null;
  if (p.demo || !Store.nube) (demoComentarios[p.id] ||= []).push({ autor: perfil().nombre || "Yo", texto, parentId: padre });
  else {
    const sb = Store.nube.sb, fila = { post_id: p.id, user_id: Store.nube.dueno, texto };
    let r = await sb.from("comments").insert({ ...fila, parent_id: padre });
    if (r.error) r = await sb.from("comments").insert(fila);   // sin respuestas en la base: queda como comentario
    if (r.error) return errorNube(r.error);
  }
  p.comentariosLista = null; await cargarComentarios(p); repintarComs();
}, true);

/* ---------- Guardar con la mochila (en Cimas y en el muro) ---------- */
{
  const base = ACC.guardarPost;
  ACC.guardarPost = (el, id) => {
    const enLinea = el?.closest?.(".cima-acciones, .acciones");
    base(el, id);
    if (enLinea) { const si = estaGuardada(id); el.setAttribute("aria-pressed", si); el.setAttribute("aria-label", si ? "Quitar de tu mochila" : "Guardar en tu mochila"); }
  };
}
const botonMochila = p => `<button data-acc="guardarPost" data-arg="${esc(p.id)}" aria-pressed="${estaGuardada(p.id)}" aria-label="${estaGuardada(p.id) ? "Quitar de tu mochila" : "Guardar en tu mochila"}" title="Guardar en tu mochila">${ic("morral")}</button>`;
globalThis.botonMochila = botonMochila;
