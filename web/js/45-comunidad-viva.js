"use strict";
/* =====================================================================================
 * Comunidad viva: lo que hace distinta a SuRoute de TikTok, Instagram y Facebook.
 * Aquí no se premia la atención sino el avance real (igual que la app):
 *  1. Prueba de camino: cada publicación muestra el camino que hay detrás (racha y pasos ganados).
 *  2. Cordada que no deja caer: si alguien lleva 3 días sin marcar, sus compañeros le lanzan una cuerda.
 *  3. Lanzar una cuerda: comentar con una pregunta, un consejo o "yo también lo intento".
 *  4. Para ti por tramo: primero lo que te ayuda a subir (tu campamento más flojo y quien tiene camino).
 *  5. La expedición de la comunidad suma los días marcados en cordadas y retos (supabase).
 *  6. Compromisos con fecha: al vencer, el autor dice si lo logró o lo replantea, y todos ven el desenlace.
 *  7. Pocos números: el contador de impulsos solo lo ve el autor.
 *  8. Carta al yo futuro: una publicación sellada con lacre que se abre en la fecha elegida.
 * Las columnas nuevas están en supabase/2026-10-29-comunidad-viva.sql; sin ellas todo sigue funcionando.
 * ===================================================================================== */

/* ---------- 1. El camino de hoy: racha de días con hábitos y pasos ganados de la Brújula ---------- */
function caminoActual() {
  let racha = 0;
  const d = hoyFecha();
  if (!marcadosDe(iso(d)).size) d.setDate(d.getDate() - 1);   // hoy aún no marcas: la racha llega hasta ayer
  for (let k = 0; k < 3650; k++) { if (!marcadosDe(iso(d)).size) break; racha++; d.setDate(d.getDate() - 1); }
  let ganados = 0;
  try {
    if (window.Mandala && window.MetodoCima && typeof casillasDe === "function") {
      const ids = Mandala.repartir(casillasDe(null)).camps.map(c => c?.id ?? null);
      ganados = MetodoCima.pasosDe(ids).filter(x => x.cumplido).length;
    }
  } catch { ganados = 0; }
  return { racha, ganados };
}
window.caminoActual = caminoActual;

/** Las columnas nuevas de una fila de la base (o del objeto que se publica). */
window.extrasPost = q => ({ camino: q?.camino || null, compromiso: q?.compromiso || null, sello: q?.sello || null });

/** Lo que se guarda al publicar, además de lo de siempre. */
window.extrasPublicar = datos => {
  const r = {}, hoyI = iso(hoyFecha());
  const cam = caminoActual(); if (cam.racha || cam.ganados) r.camino = cam;
  const hasta = String(datos.get("hasta") || ""), abre = String(datos.get("abre") || "");
  if (/^\d{4}-\d\d-\d\d$/.test(hasta) && hasta > hoyI) r.compromiso = { hasta, estado: "ABIERTO" };
  if (/^\d{4}-\d\d-\d\d$/.test(abre) && abre > hoyI) r.sello = { abre };
  return r;
};

/* ---------- 8. Carta sellada ---------- */
const fechaCartaCv = f => deIso(f).toLocaleDateString("es", { day: "numeric", month: "long", year: "numeric" });
const diasHastaCv = f => Math.round((deIso(f) - hoyFecha()) / DIA_MS);
/** Sellada para quien no es el autor hasta la fecha en que se abre. */
window.estaSellada = p => !!(p?.sello?.abre && !p.propio && iso(hoyFecha()) < p.sello.abre);
const selloHtmlCv = p => {
  const n = diasHastaCv(p.sello.abre);
  return `<div class="carta-sellada" role="img" aria-label="Carta al yo futuro, sellada hasta el ${fechaCartaCv(p.sello.abre)}">
    <div class="cs-sobre"><span class="cs-solapa" aria-hidden="true"></span><img class="cs-lacre" src="sello.png" alt="" aria-hidden="true"></div>
    <b class="cs-titulo">Carta al yo futuro</b>
    <span class="cs-fecha">Se abre el ${fechaCartaCv(p.sello.abre)}${n > 0 ? ` · faltan ${n} ${n === 1 ? "día" : "días"}` : ""}</span>
    <span class="cs-ayuda">Guárdala en tu mochila para leerla cuando se abra.</span></div>`;
};
window.selladaCima = p => window.estaSellada(p) ? selloHtmlCv(p) : "";
// En el muro y en las listas, la postal también se sella
{ const base = postal; postal = (p, mini) => window.estaSellada(p) ? selloHtmlCv(p) : base(p, mini); }

/* ---------- 1, 6 y 8: chips del pie (camino, compromiso, sello propio) ---------- */
window.chipsCamino = p => {
  const ch = [], c = p.camino, k = p.compromiso, hoyI = iso(hoyFecha());
  if (c && (c.racha || c.ganados)) {
    if (c.racha) ch.push(`<span class="cv-chip cv-camino" title="Días seguidos marcando sus hábitos">${ic("ruta")}Día ${c.racha} de constancia</span>`);
    if (c.ganados) ch.push(`<span class="cv-chip cv-camino" title="Pasos ganados de los 64 de su Brújula">${ic("cima")}${c.ganados}/64 pasos</span>`);
  }
  if (k?.hasta) {
    if (k.estado === "LOGRADO") ch.push(`<span class="cv-chip cv-logrado">${ic("check")}Compromiso cumplido</span>`);
    else if (k.estado === "REPLANTEO") ch.push(`<span class="cv-chip">${ic("repetir")}Lo replanteó con honestidad</span>`);
    else if (hoyI <= k.hasta) { const n = diasHastaCv(k.hasta);
      ch.push(`<span class="cv-chip cv-compromiso" title="Se compromete hasta el ${fechaCartaCv(k.hasta)}">${ic("bandera")}Compromiso · ${n > 0 ? `faltan ${n} ${n === 1 ? "día" : "días"}` : "hoy vence"}</span>`); }
    else if (p.propio) ch.push(`<span class="cv-cierre"><span class="cv-chip cv-compromiso">${ic("bandera")}Venció tu compromiso: ¿cómo te fue?</span>
      <button class="cv-btn" data-acc="cerrarCompromiso" data-arg="${esc(p.id)}|LOGRADO">${ic("check")} Lo logré</button>
      <button class="cv-btn" data-acc="cerrarCompromiso" data-arg="${esc(p.id)}|REPLANTEO">${ic("repetir")} Lo replanteo</button></span>`);
    else ch.push(`<span class="cv-chip">${ic("reloj")}Venció · esperando su desenlace</span>`);
  }
  if (p.sello?.abre && p.propio && hoyI < p.sello.abre) ch.push(`<span class="cv-chip">${ic("candado")}Sellada para los demás hasta el ${fechaCartaCv(p.sello.abre)}</span>`);
  return ch.length ? `<div class="cv-chips">${ch.join("")}</div>` : "";
};

ACC.cerrarCompromiso = async (el, arg) => {
  const [id, estadoK] = arg.split("|");
  const todos = [...(estado.feed || []), ...(estado.misPosts || []), ...(estado.misRecientes || []), ...DEMO_POSTS, ...misPostsDemo];
  const ps = todos.filter(x => String(x.id) === id); if (!ps.length || !ps[0].compromiso) return;
  const nuevo = { ...ps[0].compromiso, estado: estadoK, cerradoEn: iso(hoyFecha()) };
  if (Store.nube && !ps[0].demo && !String(id).startsWith("mio-")) {
    const r = await Store.nube.sb.from("posts").update({ compromiso: nuevo }).eq("id", id);
    if (r.error) return errorNube(r.error);
  }
  ps.forEach(x => { x.compromiso = nuevo; });
  toast(estadoK === "LOGRADO" ? "¡Bandera en la cima! Tu comunidad verá que lo lograste." : "Replantear también es avanzar. Tu comunidad lo verá.");
  pintarDetalle(); pintarLista();
};

/* ---------- 6 y 8 en el formulario de publicar ---------- */
{
  const base = DET["comunidad.publicar"];
  DET["comunidad.publicar"] = (...a) => {
    const cam = caminoActual(), manana = (() => { const d = hoyFecha(); d.setDate(d.getDate() + 1); return iso(d); })();
    const camino = cam.racha || cam.ganados
      ? `<p class="cv-nota">${ic("ruta")} Tu publicación mostrará el camino que hay detrás: ${[cam.racha ? `día ${cam.racha} de constancia` : "", cam.ganados ? `${cam.ganados} de 64 pasos ganados` : ""].filter(Boolean).join(" · ")}.</p>`
      : `<p class="cv-nota">${ic("ruta")} Marca tus hábitos y gana pasos en tu Brújula: tus publicaciones mostrarán el camino que hay detrás.</p>`;
    const extra = `<details class="cv-mas"><summary>${ic("bandera")} Compromiso con fecha o carta al yo futuro</summary>
      <div class="form-2">${campo("Me comprometo hasta (opcional)", `<input type="date" name="hasta" min="${manana}">`)}
        ${campo("Sellar como carta al yo futuro: se abre el (opcional)", `<input type="date" name="abre" min="${manana}">`)}</div>
      <p class="suave" style="margin:0;font-size:13px">Al vencer tu compromiso dirás si lo lograste o lo replanteas, y tu comunidad verá el desenlace. Una carta sellada se ve con su lacre hasta el día que elijas.</p></details>`;
    return base(...a).replace(`<div class="botones"><button class="btn lleno" type="submit">Publicar</button>`, camino + extra + `<div class="botones"><button class="btn lleno" type="submit">Publicar</button>`);
  };
}

/* ---------- 3. Lanzar una cuerda en los comentarios ---------- */
const CUERDAS_CV = [["", "Comentar"], ["PREGUNTA", "Pregunta"], ["CONSEJO", "Consejo"], ["YO_TAMBIEN", "Yo también lo intento"]];
window.chipsCuerda = () => `<div class="cuerda-chips" role="radiogroup" aria-label="Lanza una cuerda">${CUERDAS_CV.map(([k, n]) =>
  `<button type="button" role="radio" aria-checked="${(estado.cuerda || "") === k}" data-acc="elegirCuerda" data-arg="${k}">${k ? ic("cordada") : ""}${n}</button>`).join("")}</div>`;
window.placeholderCuerda = () => ({ PREGUNTA: "¿Qué le quieres preguntar?", CONSEJO: "Un consejo que te sirvió…", YO_TAMBIEN: "Cuéntale cómo lo vas intentando tú…" })[estado.cuerda] || "";
window.etiquetaCuerda = c => c.cuerda ? `<span class="com-cuerda">${ic("cordada")}Lanzó una cuerda · ${CUERDAS_CV.find(x => x[0] === c.cuerda)?.[1] || ""}</span>` : "";
ACC.elegirCuerda = (el, k) => {
  const input = el.closest(".hoja-panel, #detalle")?.querySelector(".com-escribir input"), valor = input?.value || "";
  estado.cuerda = k || null;
  if (document.querySelector("#hojaInferior .com-lista") && estado.post) pintarHojaComentarios(estado.post); else pintarDetalle();
  requestAnimationFrame(() => { const i = document.querySelector("#hojaInferior .com-escribir input") || document.querySelector("#detalle .com-escribir input"); if (i) { i.value = valor; i.focus(); } });
};
// Los comentarios de ejemplo también muestran cuerdas
COM_DEMO[2].cuerda = "PREGUNTA";
COM_DEMO.push({ id: "d4", autor: "Camilo T.", texto: "A mí me sirvió dejar la ropa lista la noche anterior.", creadoEn: Date.now() - 36e5 * 2, likes: 3, cuerda: "CONSEJO" });

/* ---------- 4. Para ti por tramo: tu campamento más flojo y quien tiene camino detrás ---------- */
window.tramoMio = () => {
  try {
    const { camps } = Mandala.repartir(casillasDe(null)); let peor = null, min = 2;
    camps.forEach((c, i) => { if (!c) return; const cod = Mandala.CAMPAMENTOS_FIJOS[i]; if (!NOMBRE_EJE[cod]) return;
      const ps = [0, 1, 2, 3, 4, 5, 6, 7].map(p => MetodoCima.leer(c.id, p)), v = ps.filter(x => x.cumplido).length / 8;
      if (v < min) { min = v; peor = cod; } });
    return peor;
  } catch { return null; }
};
window.bonoTramo = p => {
  const t = window.tramoMio(), c = p.camino;
  return (t && p.eje === t ? 1.5 : 1) * (c ? 1 + Math.min(0.5, Math.log2(1 + (c.racha || 0) + (c.ganados || 0)) / 12) : 1) * (p.compromiso?.estado === "LOGRADO" ? 1.2 : 1);
};

/* ---------- 2. Cordada que no deja caer ---------- */
ACC.cuerdaCordada = (el, nombre) => {
  const i = document.querySelector('#detalle form[data-form="notaCordada"] input'); if (!i) return;
  i.value = `Te lanzo una cuerda, ${nombre.split(" ")[0]}: ¡hoy es un buen día para volver a marcar! `;
  i.scrollIntoView({ block: "center", behavior: pocoMovimiento?.() ? "auto" : "smooth" }); i.focus();
};

/* ---------- Ejemplos (demostración): camino, un compromiso y una carta sellada ---------- */
{
  const cam = { "demo-1": { racha: 30, ganados: 14 }, "demo-3": { racha: 21, ganados: 9 }, "demo-6": { racha: 64, ganados: 22 }, "demo-4": { racha: 5, ganados: 3 } };
  DEMO_POSTS.forEach(p => { if (cam[p.id]) p.camino = cam[p.id]; });
  const en = k => { const d = hoyFecha(); d.setDate(d.getDate() + k); return iso(d); };
  const m = DEMO_POSTS.find(p => p.id === "demo-4"); if (m) m.compromiso = { hasta: en(45), estado: "ABIERTO" };
  const l = DEMO_POSTS.find(p => p.id === "demo-6"); if (l) l.compromiso = { hasta: en(-3), estado: "LOGRADO" };
  const s = DEMO_POSTS.find(p => p.id === "demo-5"); if (s) s.sello = { abre: en(240) };
}
// En la cordada de ejemplo, Valentina lleva unos días sin marcar: así se ve la cuerda
try {
  const s = DemoCord.checkins["dk1|demo-1"];
  if (s) for (let k = 0; k <= 3; k++) { const d = hoyFecha(); d.setDate(d.getDate() - k); s.delete(iso(d)); }
} catch { /* sin cordadas de ejemplo */ }
