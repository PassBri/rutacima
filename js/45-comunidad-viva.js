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

/* ---------- Cómo funciona la comunidad (junto a la lupa) ---------- */
const GUIA_COMUNIDAD = [
  ["comunidad", "Para ti, Visión y Siguiendo", "Para ti muestra primero lo que te ayuda a subir: lo de tu campamento más flojo y lo de quien tiene camino detrás. Visión, solo tableros de sueños. Siguiendo, las personas que sigues."],
  ["arriba", "Desliza para la siguiente cima", "Cada publicación ocupa la pantalla. El muro tiene final: cuando ves todo lo nuevo, te invita a dar tu propio paso."],
  ["impulso", "Impulsar", "Toca el corazón o da doble toque sobre la publicación. El número de impulsos solo lo ve quien publicó: aquí no se compite por cifras."],
  ["sol", "¿Qué clima te deja?", "Deja cómo te hizo sentir: sol, tormenta, niebla, valle o cumbre. El anillo de colores muestra el clima que dejó la comunidad."],
  ["cordada", "Lanzar una cuerda", "Al comentar, elige Pregunta, Consejo o Yo también lo intento. Una cuerda ayuda más que un aplauso."],
  ["morral", "Mochila", "Guarda la publicación para volver a ella. También sirve para guardar una carta sellada y leerla el día que se abra."],
  ["ruta", "Prueba de camino", "Cada publicación muestra el camino que hay detrás: los días seguidos de constancia y los pasos ganados de la Brújula."],
  ["bandera", "Compromiso con fecha", "Al publicar puedes comprometerte hasta una fecha. Cuando vence, dices si lo lograste o lo replanteas, y tu comunidad ve el desenlace."],
  ["candado", "Carta al yo futuro", "Sella una publicación hasta una fecha: los demás verán el sobre con su lacre y la leerán cuando se abra."],
  ["personas", "Cordadas", "Retos de 7 a 90 días con hasta 6 personas. Si alguien lleva 3 días sin marcar, sus compañeros le lanzan una cuerda."],
  ["cima", "La expedición de la comunidad", "Una montaña de 8.848.000 mm que subimos entre todos: suman las publicaciones, los impulsos y cada día marcado en cordadas y retos."],
  ["masH", "Más opciones", "Junto al nombre: copiar el enlace, silenciar, reportar o bloquear. Tu seguridad siempre está a un toque."],
];
ACC.guiaComunidad = () => abrirHoja(`<div class="com-cab"><span></span><h3 class="hoja-titulo">Cómo funciona la comunidad</h3><button data-cerrar-hoja aria-label="Cerrar">${ic("cerrar")}</button></div>
  <p class="suave guia-intro">En SuRoute no se premia la atención sino el avance real. Así se usa cada opción:</p>
  <ul class="guia-com">${GUIA_COMUNIDAD.map(([i, t, d]) => `<li><span class="guia-ic">${i === "sol" && typeof climaSvg === "function" ? climaSvg("sol") : ic(i)}</span><div><b>${t}</b><p>${d}</p></div></li>`).join("")}</ul>`);
{
  const boton = cls => `<button class="${cls}" data-acc="guiaComunidad" aria-label="Cómo funciona la comunidad" title="Cómo funciona la comunidad">${ic("ayuda")}</button>`;
  const feed = DET["comunidad.feed"];
  DET["comunidad.feed"] = (...a) => feed(...a).replace(/(<button class="cimas-ic" data-ir="comunidad" data-arg="buscar")/, boton("cimas-ic") + "$1");
  const lista = LISTAS.comunidad;
  LISTAS.comunidad = (...a) => lista(...a).replace(/(<button class="rb" data-ir="comunidad" data-arg="buscar")/, boton("rb") + "$1");
}

/* =====================================================================================
 * Moverse de lado (lo nuevo de SuRoute frente al deslizar hacia abajo de las redes):
 *  A. Deslizar a la izquierda: «Detrás de la cima», el camino de esa persona.
 *  C. Deslizar a la derecha: «Me lo llevo», se guarda en la mochila y se impulsa. Nunca se rechaza a nadie.
 *  B. Senderos: arriba, la franja de ejes (Todos, Voluntad, Maestría…); se toca o se desliza de lado para cambiar de sendero.
 * ===================================================================================== */
const SENDEROS = [[null, "Todos"], ...EJES.map(([c, n]) => [c, n])];

// B. Senderos: la franja bajo las pestañas de Cimas
{
  const feed = DET["comunidad.feed"];
  DET["comunidad.feed"] = (...a) => {
    const h = feed(...a);
    if (estado.vistaCom !== "cimas" || !estado.feed) return h;
    const franja = `<nav class="senderos" aria-label="Senderos: desliza de lado para cambiar">${SENDEROS.map(([c, n]) =>
      `<button data-acc="sendero" data-arg="${c || ""}" aria-pressed="${(estado.feedEje || null) === c}"${c ? ` style="--c:${COLOR_EJE[c]}"` : ""}>${c ? `<i></i>` : ""}${n}</button>`).join("")}</nav>`;
    return h.replace(/(<div class="cimas-cab">[\s\S]*?<\/div>)/, `$1${franja}`);
  };
}
ACC.sendero = (el, c) => {
  estado.feedEje = c || null; estado.feed = null; estado.feedN = 0;
  cargarFeed();
  requestAnimationFrame(() => document.querySelector(".senderos [aria-pressed=true]")?.scrollIntoView({ inline: "center", block: "nearest" }));
};
const cambiarSendero = paso => {
  const i = SENDEROS.findIndex(([c]) => c === (estado.feedEje || null)), j = (i + paso + SENDEROS.length) % SENDEROS.length;
  ACC.sendero(null, SENDEROS[j][0] || "");
  toast(`Sendero: ${SENDEROS[j][1]}`);
};
// En el ejemplo, los senderos también filtran
{
  const base = cargarFeed;
  cargarFeed = async (...a) => { await base(...a); if (!Store.nube && estado.feedEje && estado.feed) { estado.feed = estado.feed.filter(p => p.eje === estado.feedEje); pintarDetalle(); } };
}

// A. Detrás de la cima: el camino de la persona, sus publicaciones del mismo sendero y su compromiso
function detrasDeLaCima(p) {
  const otras = (estado.feed || []).filter(x => x.autorId === p.autorId && x.id !== p.id).slice(0, 6), c = p.camino, k = p.compromiso;
  const fila = (i, t, d) => `<li><span class="guia-ic">${ic(i)}</span><div><b>${t}</b>${d ? `<p>${d}</p>` : ""}</div></li>`;
  abrirHoja(`<div class="com-cab"><span></span><h3 class="hoja-titulo">Detrás de la cima</h3><button data-cerrar-hoja aria-label="Cerrar">${ic("cerrar")}</button></div>
    <div class="detras-quien">${av(esc((p.autorNombre || "?").split(" ").map(x => x[0]).join("").slice(0, 2).toUpperCase()), COLOR_EJE[p.eje] || "var(--burdeos)")}
      <div><b>${esc(p.autorNombre)}</b><span class="suave">${p.eje ? "Sendero " + esc(NOMBRE_EJE[p.eje]) : "Confluencia"}${p.metaTitulo ? " · " + esc(p.metaTitulo) : ""}</span></div></div>
    <ul class="guia-com">
      ${fila("ruta", c?.racha ? `Día ${c.racha} de constancia` : "Sin racha registrada", c?.racha ? "Días seguidos marcando sus hábitos cuando publicó." : "Esta publicación no trae su racha.")}
      ${fila("cima", c?.ganados ? `${c.ganados} de 64 pasos ganados` : "Pasos de la Brújula", c?.ganados ? "Lo que ya conquistó de su Brújula de la Cima." : "Aún no comparte pasos ganados.")}
      ${k?.hasta ? fila("bandera", k.estado === "LOGRADO" ? "Cumplió su compromiso" : k.estado === "REPLANTEO" ? "Replanteó su compromiso" : `Compromiso hasta el ${deIso(k.hasta).toLocaleDateString("es", { day: "numeric", month: "long" })}`, "Lo dijo en público y la comunidad ve el desenlace.") : ""}
    </ul>
    ${otras.length ? `<div class="etiqueta">Otras cimas de ${esc(p.autorNombre.split(" ")[0])}</div><div class="detras-otras">${otras.map(x => `<button data-acc="verCima" data-arg="${esc(x.id)}" data-cerrar-hoja>${postal(x, true)}</button>`).join("")}</div>` : ""}
    <div class="botones" style="margin:14px 0 18px">${!p.demo || !Store.nube ? `<button class="btn" data-acc="escribirA" data-arg="${esc(p.autorId)}|${esc(p.autorNombre)}" data-cerrar-hoja>${ic("mensajes")} Escribirle</button>` : ""}
      ${Store.nube && !p.demo ? `<button class="btn lleno" data-ir="comunidad" data-arg="persona:${esc(p.autorId)}" data-cerrar-hoja>${ic("perfil")} Ver su perfil</button>` : ""}</div>`);
}

// C. Me lo llevo: a la mochila y con impulso
function meLoLlevo(p, el) {
  if (!estaGuardada(p.id)) ACC.guardarPost(el?.querySelector('[data-acc="guardarPost"]') || null, String(p.id));
  if (!p.yoImpulse) ACC.impulsarId(null, String(p.id));
  toast("Te lo llevas: quedó en tu mochila y le diste impulso.");
}

// El gesto: arrastrar de lado sobre la cima (lo vertical sigue siendo para pasar a la siguiente)
{
  let g = null;
  const cimaDe = el => el.closest?.(".cima[data-cima]");
  document.addEventListener("pointerdown", e => {
    const c = cimaDe(e.target); if (!c || e.target.closest("button, a, input, textarea, .cima-acciones, .clima-abanico")) { g = null; return; }
    g = { c, x: e.clientX, y: e.clientY, eje: null };
  }, { passive: true });
  document.addEventListener("pointermove", e => {
    if (!g) return; const dx = e.clientX - g.x, dy = e.clientY - g.y;
    if (!g.eje && Math.hypot(dx, dy) > 12) g.eje = Math.abs(dx) > Math.abs(dy) * 1.3 ? "x" : "y";
    if (g.eje !== "x") return;
    g.c.classList.add("deslizando"); g.c.style.setProperty("--dx", `${dx}px`);
    const lado = dx > 0 ? "llevar" : "detras"; g.c.style.setProperty("--pista", Math.min(1, Math.abs(dx) / 110).toFixed(2));
    let pista = g.c.querySelector(":scope > .cima-pista");
    if (!pista) { pista = document.createElement("span"); pista.setAttribute("aria-hidden", "true"); g.c.append(pista); }
    pista.className = "cima-pista " + lado; pista.textContent = lado === "llevar" ? "Me lo llevo" : "Detrás de la cima";
  }, { passive: true });
  const soltar = e => {
    if (!g) return; const { c } = g, dx = e.clientX - g.x, fue = g.eje === "x"; g = null;
    c.classList.remove("deslizando"); c.style.removeProperty("--dx"); c.style.removeProperty("--pista"); c.querySelector(":scope > .cima-pista")?.remove();
    if (!fue || Math.abs(dx) < 90) return;
    const p = (estado.feed || []).find(x => String(x.id) === c.dataset.cima); if (!p) return;
    navigator.vibrate?.(10);
    if (dx > 0) meLoLlevo(p, c); else detrasDeLaCima(p);
  };
  document.addEventListener("pointerup", soltar); document.addEventListener("pointercancel", () => { if (g) { g.c.classList.remove("deslizando"); g.c.style.removeProperty("--dx"); g.c.querySelector(":scope > .cima-pista")?.remove(); g = null; } });
  // B. Deslizar de lado sobre la cabecera o los senderos cambia de sendero
  let s = null;
  document.addEventListener("pointerdown", e => { s = e.target.closest?.(".cimas-cab, .senderos") ? { x: e.clientX, y: e.clientY } : null; }, { passive: true });
  document.addEventListener("pointerup", e => {
    if (!s) return; const dx = e.clientX - s.x, dy = e.clientY - s.y; s = null;
    if (Math.abs(dx) > 70 && Math.abs(dx) > Math.abs(dy) * 1.5 && !e.target.closest?.(".senderos")) cambiarSendero(dx < 0 ? 1 : -1);
  });
  // Teclado (computador): ← detrás de la cima, → me lo llevo, sobre la cima visible
  document.addEventListener("keydown", e => {
    if (estado.sec !== "comunidad" || estado.vistaCom !== "cimas" || e.target.closest?.("input, textarea, select, [contenteditable]") || document.getElementById("hojaInferior")) return;
    if (e.key !== "ArrowLeft" && e.key !== "ArrowRight") return;
    const caja = document.querySelector(".cimas"); if (!caja) return;
    const c = [...caja.querySelectorAll(".cima[data-cima]")].find(x => Math.abs(x.getBoundingClientRect().top - caja.getBoundingClientRect().top) < 40); if (!c) return;
    const p = (estado.feed || []).find(x => String(x.id) === c.dataset.cima); if (!p) return;
    e.preventDefault(); e.key === "ArrowRight" ? meLoLlevo(p, c) : detrasDeLaCima(p);
  });
}

// La guía también explica los nuevos movimientos
GUIA_COMUNIDAD.splice(2, 0,
  ["izq", "Desliza a la izquierda: detrás de la cima", "Ves el camino de esa persona: su constancia, sus pasos de la Brújula, su compromiso y sus otras cimas. En el computador, flecha ←."],
  ["morral", "Desliza a la derecha: me lo llevo", "La publicación queda en tu mochila y le das impulso. Aquí nunca se rechaza a nadie. En el computador, flecha →."],
  ["ejes", "Senderos", "Arriba eliges un sendero (Voluntad, Maestría, Voz, Valor, Evolución, Trascendencia) o deslizas de lado sobre la cabecera para cambiarlo."]);
