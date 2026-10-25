"use strict";
/* ======================================================================
 * Detalle
 * ====================================================================== */
const cab = (avatar, titulo, sub, acciones = "") =>
  `<header class="det-cab"><button class="rb volver" data-acc="volver" aria-label="Volver">${ic("volver")}</button>${avatar}<div class="txt"><h2>${titulo}</h2><p>${sub}</p></div>${acciones}</header>`;
const cuerpo = (html, ancho = "") => `<div class="det-cuerpo"><div class="ancho" style="gap:18px;${ancho}">${html}</div></div>`;
/** Texto alternativo de una foto de publicación (lectores de pantalla). */
const altFoto = p => esc(`Foto de ${p.autorNombre || "la comunidad"}${(p.metaTitulo || p.texto) ? ": " + String(p.metaTitulo || p.texto).slice(0, 120) : ""}`);
const hoja = (html, clase = "") => `<div class="hoja ${clase}">${html}</div>`;
const campo = (etq, control) => `<label class="campo"><span>${etq}</span>${control}</label>`;
/** Control enlazado a un campo de un documento: se guarda al salir del campo. */
const bind = (tipo, clave, campoN, conv = "") => `data-bind="${tipo}|${esc(clave)}|${campoN}|${conv}"`;
const txt = (tipo, clave, campoN, valor, ph = "") => `<input type="text" ${bind(tipo, clave, campoN)} value="${esc(valor)}" placeholder="${esc(ph)}">`;
const area = (tipo, clave, campoN, valor, ph = "") => `<textarea ${bind(tipo, clave, campoN)} placeholder="${esc(ph)}">${esc(valor)}</textarea>`;
const sel = (tipo, clave, campoN, valor, opciones, conv = "") =>
  `<select ${bind(tipo, clave, campoN, conv)}>${opciones.map(([v, n]) => `<option value="${esc(v)}" ${String(v) === String(valor ?? "") ? "selected" : ""}>${esc(n)}</option>`).join("")}</select>`;
/** Horizonte en años: se escribe el número, se ajusta con − y + o se toca un atajo. */
function selectorHorizonte(tipo, clave, anios) {
  const arg = `${tipo}|${esc(clave)}`;
  return `<div class="campo horizonte" role="group" aria-labelledby="horiz-${esc(clave)}"><span id="horiz-${esc(clave)}">Horizonte</span>
    <div class="pasos-num">
      <button type="button" class="btn mini" data-acc="horizonte" data-arg="${arg}|-1" aria-label="Un año menos" ${anios <= HORIZONTE_MIN ? "disabled" : ""}>−</button>
      <input type="number" inputmode="numeric" min="${HORIZONTE_MIN}" max="${HORIZONTE_MAX}" step="1" value="${anios}" aria-label="Años del propósito" ${bind(tipo, clave, "horizonte", "horiz")}>
      <button type="button" class="btn mini" data-acc="horizonte" data-arg="${arg}|1" aria-label="Un año más" ${anios >= HORIZONTE_MAX ? "disabled" : ""}>+</button>
      <b>años</b>
    </div>
    <div class="atajos">${HORIZONTES.map(h => `<button type="button" class="chip" aria-pressed="${h === anios}" data-acc="horizonte" data-arg="${arg}|=${h}">${h}</button>`).join("")}</div>
  </div>`;
}
/** Ocho vasos que se llenan con una ola al tocarlos (250 ml cada uno); más y menos para no apuntar al vaso. */
function vasosDeAgua(fecha, n) {
  const vaso = i => `<button class="vaso${i <= n ? " lleno" : ""}" data-acc="agua" data-arg="${fecha}|${i === n ? i - 1 : i}" aria-label="Vaso ${i}" aria-pressed="${i <= n}">
    <svg viewBox="0 0 36 50" aria-hidden="true"><defs><clipPath id="v-${fecha}-${i}"><path d="M2 3h32l-4 44H6z"/></clipPath></defs>
      <path d="M2 3h32l-4 44H6z" class="vaso-fondo"/><g clip-path="url(#v-${fecha}-${i})"><g class="vaso-agua"><path class="ola" d="M-36 8 q9 -5 18 0 t18 0 t18 0 t18 0 t18 0 V60 H-36z"/></g></g>
      <path d="M2 3h32l-4 44H6z" class="vaso-borde"/></svg></button>`;
  const litros = (n * 0.25).toLocaleString("es", { maximumFractionDigits: 2 });
  return `<div class="vasos" role="group" aria-label="Vasos de agua">${[1, 2, 3, 4, 5, 6, 7, 8].map(vaso).join("")}</div>
    <div class="vasos-pie"><b>${n} de 8 vasos · ${litros} L</b>${n >= 8 ? `<span>¡Meta de agua cumplida!</span>` : ""}
      <button class="btn mini" data-acc="agua" data-arg="${fecha}|${n - 1}" ${n <= 0 ? "disabled" : ""} aria-label="Un vaso menos">−</button>
      <button class="btn mini" data-acc="agua" data-arg="${fecha}|${n + 1}" ${n >= 8 ? "disabled" : ""} aria-label="Un vaso más">+</button></div>`;
}
/** La expedición de la comunidad (igual que Expedicion.kt en la app): una montaña de 8.848.000 mm por año
 * para todos. Cada semana es un tramo y su dificultad se ajusta con los puntos del tramo anterior, como
 * Bitcoin: la dificultad no cambia más de 4 veces por tramo y ningún tramo sube más de 4/52 de la montaña. */
const Expedicion = {
  ALTURA: 8848000, TRAMOS: 52, PUB: 10, IMP: 1, MIN: 100000, AJ: 4,
  tramoDe(f) { const doy = Math.round((Date.UTC(f.getFullYear(), f.getMonth(), f.getDate()) - Date.UTC(f.getFullYear(), 0, 1)) / 864e5) + 1; return Math.min(Math.floor((doy - 1) / 7), this.TRAMOS - 1); },
  mmPorPunto(d) { return this.ALTURA / (Math.max(d, 1) * this.TRAMOS); },
  dificultades(p) {
    const r = []; let d = this.MIN;
    p.forEach((_, i) => { if (i) d = Math.max(this.MIN, Math.min(Math.max(p[i - 1], Math.floor(d / this.AJ)), d * this.AJ)); r.push(d); });
    return r;
  },
  calcular(puntos, misPuntos, hoy = new Date()) {
    const t = this.tramoDe(hoy), p = Array.from({ length: t + 1 }, (_, i) => puntos[i] || 0), d = this.dificultades(p);
    const tope = this.ALTURA * this.AJ / this.TRAMOS;
    const mm = Math.min(this.ALTURA, p.reduce((s, x, i) => s + Math.min(x * this.mmPorPunto(d[i]), tope), 0));
    const fin = t === this.TRAMOS - 1 ? new Date(hoy.getFullYear() + 1, 0, 1) : new Date(hoy.getFullYear(), 0, (t + 1) * 7 + 1);
    const porPunto = this.mmPorPunto(d[t]);
    return { mm, porPunto, dificultad: d[t], tramo: t, cumbre: mm >= this.ALTURA, fraccion: Math.min(1, mm / this.ALTURA),
      diasAjuste: Math.round((Date.UTC(fin.getFullYear(), fin.getMonth(), fin.getDate()) - Date.UTC(hoy.getFullYear(), hoy.getMonth(), hoy.getDate())) / 864e5),
      miAporte: misPuntos * porPunto };
  },
  porTramo(ps, anio) {
    const r = new Array(this.TRAMOS).fill(0);
    ps.forEach(p => { const f = new Date(p.creadoEn); if (f.getFullYear() === anio) r[this.tramoDe(f)] += this.PUB + (p.impulsos || 0) * this.IMP; });
    return r;
  },
};
window.Expedicion = Expedicion;
/** Milímetros con los decimales que hacen falta: con millones de personas un aporte vale fracciones. */
const formatoMm = v => v.toLocaleString("es", { maximumFractionDigits: v >= 1000 ? 0 : v >= 1 ? 2 : v >= 0.001 ? 3 : 6 });
function cumbreComunidad(ps) {
  const hoy = new Date(), t = Expedicion.tramoDe(hoy);
  const puntos = estado.tramos || Expedicion.porTramo(ps, hoy.getFullYear());
  const deEste = ps.filter(p => { const f = new Date(p.creadoEn); return f.getFullYear() === hoy.getFullYear() && Expedicion.tramoDe(f) === t; });
  const mios = deEste.filter(p => p.propio).length * Expedicion.PUB + deEste.filter(p => p.yoImpulse).length * Expedicion.IMP;
  const e = Expedicion.calcular(puntos, mios, hoy), f = e.fraccion;
  const cresta = [[0, 92], [12, 78], [22, 82], [34, 58], [44, 64], [56, 40], [64, 46], [76, 18], [82, 8]].map(([x, y]) => [x * 2.4, y]);
  const d = cresta.map(([x, y], i) => `${i ? "L" : "M"}${x} ${y}`).join(" ");
  const nf = n => n.toLocaleString("es");
  return `<section class="cumbre-com" aria-label="La expedición de la comunidad: ${nf(Math.floor(e.mm))} milímetros de ${nf(Expedicion.ALTURA)}">
    <h3>La expedición de la comunidad</h3>
    <p>${e.cumbre ? "¡Cumbre! La comunidad coronó los 8.848.000 mm de este año." : "Una sola montaña de 8.848.000 mm para todos este año. Cuantas más personas suben, más difícil es cada milímetro."}</p>
    <svg viewBox="0 0 240 100" aria-hidden="true"><rect width="240" height="100" rx="14" class="cc-cielo"/>
      <path d="M0 100 ${d.slice(1)} L240 50 L240 100Z" class="cc-roca"/>
      <path d="${d}" class="cc-sendero"/><path d="${d}" class="cc-hecho" pathLength="1" style="stroke-dasharray:${f} 1"/>
    </svg>
    <div class="cc-cifra"><b>${nf(Math.floor(e.mm))}</b> / ${nf(Expedicion.ALTURA)} mm <span class="cc-pct">${(f * 100).toLocaleString("es", { maximumFractionDigits: 2 })} %</span></div>
    <div class="cc-datos">Hoy una publicación vale <b>${formatoMm(e.porPunto * Expedicion.PUB)} mm</b> y un impulso <b>${formatoMm(e.porPunto * Expedicion.IMP)} mm</b></div>
    <div class="cc-datos">Tramo ${e.tramo + 1} de 52 · la dificultad se ajusta en ${e.diasAjuste} ${e.diasAjuste === 1 ? "día" : "días"}${e.miAporte > 0 ? ` · <b>Tu aporte en este tramo: ${formatoMm(e.miAporte)} mm</b>` : ""}</div></section>`;
}
/** Álbum del año en el diario de vida: quién lo ve, metas cumplidas y los recuerdos en mosaico. */
function albumAnio(a, metas) {
  if (!estado.misPosts) cargarMisPosts();
  const ps = (estado.misPosts || []).filter(p => (p.anio ?? new Date(p.creadoEn).getFullYear()) === a);
  const vis = respuesta(`diario-${a}#visibilidad`) || "PRIVADA";
  const opciones = [["PRIVADA", "Solo yo"], ["SEGUIDORES", "Seguidores"], ["PUBLICA", "Público"]];
  const nota = { PRIVADA: "Solo tú ves este año.", SEGUIDORES: "Tus seguidores pueden ver este año en tu diario.", PUBLICA: "Cualquier persona de la comunidad puede ver este año en tu diario." }[vis];
  const cumplidas = metas.filter(m => m.estado === "CUMPLIDA");
  return hoja(`<div class="album-cab"><h3>Álbum de ${a}</h3>
      <div class="segmentos" role="group" aria-label="Quién puede ver este año">${opciones.map(([k, n]) => `<button data-acc="diarioVis" data-arg="${a}|${k}" aria-pressed="${vis === k}">${n}</button>`).join("")}</div></div>
    <p class="suave" style="margin:6px 0 12px">${nota}</p>
    ${cumplidas.length ? `<div class="chips" style="margin-bottom:12px">${cumplidas.map(m => `<span class="chip" aria-pressed="true">🏆 ${esc(m.titulo)}</span>`).join("")}</div>` : ""}
    ${ps.length ? recuerdosAnio(a, ps)
      : `<p class="suave" style="margin:0">${estado.misPosts ? "Todavía no hay recuerdos de este año. Regístralos desde la app." : "Cargando recuerdos…"}</p>`}`);
}
/** Recuerdos de un año: una tira de 6 que no tapa los meses; "Ver todo" abre el mosaico completo. */
function recuerdosAnio(a, ps) {
  const abierto = estado.albumAbierto === a, vistos = abierto ? ps : ps.slice(0, 6);
  const tarjeta = p => `<button class="recuerdo" data-ir="comunidad" data-arg="post:${esc(p.id)}">
        ${p.foto ? `<img src="${esc(p.foto)}" alt="" loading="lazy">` : ""}<span>${p.metaTitulo ? `<b>${esc(p.metaTitulo)}</b>` : ""}${p.texto ? (p.foto ? esc(p.texto) : `<i>“${esc(p.texto)}”</i>`) : ""}</span></button>`;
  return `<div class="${abierto ? "album" : "album album-tira"}">${vistos.map(tarjeta).join("")}</div>` +
    (ps.length > 6 ? `<button class="btn mini" style="margin-top:10px" data-acc="albumAbrir" data-arg="${abierto ? "" : a}">${abierto ? "Ver menos" : `Ver todo (${ps.length})`}</button>` : "");
}
/** Un año por vivir: su visión. Fotos de lo que quieres ver cumplido ese año, privadas hasta que las compartas. */
function visionAnio(a) {
  if (!estado.misPosts) cargarMisPosts();
  const ps = (estado.misPosts || []).filter(p => p.tipo === "VISION" && (p.anio ?? new Date(p.creadoEn).getFullYear()) === a);
  const cs = casillasVision(), conFoto = cs.filter(c => fotoDePublicacion(c.publicacionId)).length;
  return hoja(`<div class="album-cab"><h3>Visión de ${a}</h3>
      <label class="btn mini oro">${ic("foto")} Agregar una foto<input type="file" accept="image/*" hidden data-foto-anio="${a}"></label></div>
    <p class="suave" style="margin:6px 0 12px">Pon aquí lo que quieres ver cumplido en ${a}. Solo tú lo ves hasta que decidas compartirlo.</p>
    ${ps.length ? recuerdosAnio(a, ps) : `<p class="suave" style="margin:0">${estado.misPosts ? "Todavía no hay fotos para este año." : "Cargando…"}</p>`}
    <p style="margin:12px 0 0"><button class="btn mini" data-ir="perfil" data-arg="vision">${ic("ia")} Mi vision board${cs.length ? ` · ${conFoto}/${cs.length}` : ""}</button></p>`);
}
async function fotoVisionAnio(a, archivo) {
  if (!archivo) return;
  toast("Subiendo tu foto…");
  try { await publicarImagenVision(archivo, "PRIVADA", "", `Visión ${a}`, "VISION", null, null, Number(a)); toast("Foto agregada a tu visión de " + a); pintarDetalle(); }
  catch (e) { errorNube(e); }
}
window.fotoVisionAnio = fotoVisionAnio;
const opcionesEje = [["", "Sin eje"], ...EJES.map(([c, n]) => [c, n])];

/** El momento del día para el arte (0 amanecer, 1 día, 2 atardecer, 3 noche), según la hora local. */
const momentoAhora = () => { const h = new Date().getHours(); return h >= 5 && h < 8 ? 0 : h >= 8 && h < 17 ? 1 : h >= 17 && h < 19 ? 2 : 3; };
function vacio() {
  return `<div class="vacio"><div class="vacio-arte" aria-hidden="true">${window.Arte ? Arte.cordillera("rutaalacima-bienvenida", momentoAhora()) : ""}</div><div>${marca()}<h2>SuRoute Web</h2>
    <p>La misma app de tu teléfono, en pantalla grande. Elige un año de tu ruta, tu día de hoy o una de tus metas.</p>
    <p class="pie suave">${ic("candado")} ${Store.nube ? "Conectada a tu cuenta: lo que cambies aquí aparece en tu teléfono." : "Demostración: los cambios se quedan en esta pestaña."}</p></div></div>`;
}

function pintarDetalle() {
  const s = estado.sec, v = estado.sel;
  let h;
  if (v === null) h = s === "coach" ? (estado.sel = "coach", DET.coach()) : s === "finanzas" ? (estado.sel = "b", DET["finanzas.b"]()) : s === "mensajes" ? vacioMensajes() : s === "mochila" ? (estado.sel = "inicio", DET["mochila.inicio"]()) : vacio();
  else {
    const [t, ...resto] = String(v).split(":"); const arg = resto.join(":");
    const f = DET[`${s}.${t}`] || DET[t];
    h = f ? f(arg) : vacio();
  }
  const mismo = pintarDetalle.ultimo === `${s}|${v}|${estado.vistaCom}`;
  const antes = mismo ? [...$("detalle").querySelectorAll(".det-cuerpo, .cimas")].map(x => x.scrollTop) : [];
  $("detalle").innerHTML = h;
  pintarDetalle.ultimo = `${s}|${v}|${estado.vistaCom}`;
  $("detalle").querySelectorAll(".det-cuerpo, .cimas").forEach((x, i) => { if (antes[i] !== undefined) x.scrollTop = antes[i]; });
  const abajo = $("detalle").querySelector(".det-cuerpo.abajo"); if (abajo) abajo.scrollTop = abajo.scrollHeight;
  pintarAudio();
  window.Cielo?.pintarTodos();
  $("app").classList.toggle("inmersivo", estado.sec === "comunidad" && estado.sel === "feed" && estado.vistaCom === "cimas");
  prepararCimas();
}
/** Cimas: el cielo de cada publicación se pinta al verla; doble toque impulsa y cae un sello de lacre. */
function prepararCimas() {
  const cont = $("detalle").querySelector(".cimas"); if (!cont) return;
  const io = new IntersectionObserver(es => es.forEach(e => {
    if (!e.isIntersecting) return;
    const el = e.target; io.unobserve(el); window.Cielo?.pintar(el, new Date(Number(el.dataset.cieloPost)));
  }), { root: cont, rootMargin: "100% 0px" });
  cont.querySelectorAll("[data-cielo-post]").forEach(el => io.observe(el));
  if (cont.dataset.listo) return; cont.dataset.listo = "1";
  cont.addEventListener("dblclick", e => {
    const c = e.target.closest(".cima"); if (!c || e.target.closest("button")) return;
    const p = (estado.feed || []).find(x => String(x.id) === c.dataset.cima);
    // El sello va fuera de la cima: la cima se redibuja al impulsar y el sello debe terminar de caer
    const sello = document.createElement("img"); sello.src = LOGO; sello.alt = ""; sello.className = "cima-sello";
    sello.style.left = e.clientX + "px"; sello.style.top = e.clientY + "px"; document.body.append(sello);
    sello.addEventListener("animationend", () => sello.remove()); setTimeout(() => sello.remove(), 1500);
    navigator.vibrate?.(12);
    if (p && !p.yoImpulse) { estado.post = p; ACC.impulsar(); }
  });
  // Teclado: flechas arriba y abajo pasan de cima en cima
  cont.addEventListener("keydown", e => {
    if (e.key !== "ArrowDown" && e.key !== "ArrowUp" && e.key !== "j" && e.key !== "k") return;
    e.preventDefault(); cont.scrollBy({ top: (e.key === "ArrowDown" || e.key === "j" ? 1 : -1) * cont.clientHeight, behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth" });
  });
}

/** Los años alrededor de hoy, en fila: metas por campamento (puntos de color), recuerdos o fotos de visión. */
function aniosVida(r) {
  if (!estado.misPosts) cargarMisPosts();
  const hoy = hoyFecha().getFullYear(), nac = r.nac.getFullYear(), metas = Store.lista("meta_anio");
  const desde = Math.max(nac, hoy - 2), hasta = Math.min(nac + metaVida(), hoy + 6);
  const filas = [];
  for (let a = desde; a <= hasta; a++) {
    const ms = metas.filter(m => m.anio === a), ps = (estado.misPosts || []).filter(p => (p.anio ?? new Date(p.creadoEn).getFullYear()) === a);
    const fotos = ps.filter(p => p.foto).length, vision = ps.filter(p => p.tipo === "VISION").length;
    const ejes = [...new Set(ms.map(m => m.eje || "CON"))];
    const sub = [ms.length ? `${ms.length} ${ms.length === 1 ? "meta" : "metas"}` : a > hoy ? "Sin metas aún" : "",
      a > hoy ? (vision ? `${vision} ${vision === 1 ? "foto" : "fotos"} de visión` : "") : (fotos ? `${fotos} ${fotos === 1 ? "recuerdo" : "recuerdos"}` : "")].filter(Boolean).join(" · ");
    filas.push(`<button class="va-fila${a === hoy ? " hoy" : a < hoy ? " vivido" : ""}" data-ir="ruta" data-arg="a:${a}">
      <span class="va-anio">${a}</span><span class="va-edad">${a - nac} años</span>
      <span class="va-txt">${a === hoy ? "<b>Este año</b> · " : ""}${sub || "&nbsp;"}</span>
      <span class="va-ejes" aria-hidden="true">${ejes.map(e => `<i style="background:${COLOR_EJE[e] || Mandala.COLORES?.CON || "#7A4A6B"}"></i>`).join("")}</span>${ic("abajo")}</button>`);
  }
  return hoja(`<div class="cab-lista-btn"><h3 style="margin:0">Tus años</h3><button class="btn mini oro" data-acc="nuevaMetaAnio" data-arg="${hoy}">${ic("mas")} Nueva meta</button></div>
    <p class="suave" style="margin:4px 0 10px">Cada año con sus metas (los colores son sus campamentos), sus recuerdos o su visión.</p><div class="va-lista">${filas.join("")}</div>`);
}


/* ======================================================================
 * Mi vida como registro de toda la vida: cada año guarda su palabra, sus anhelos y sueños, sus metas,
 * lo cumplido, sus recuerdos (o su visión, si está por vivir) y una carta a ese yo. Se pasa de un año a
 * otro con la línea de vida, las flechas, el teclado o deslizando el dedo.
 * Todo se guarda como respuestas "vida#<año>#…": viaja a la app y a las copias sin tablas nuevas.
 * ====================================================================== */
const claveVida = (a, k) => `vida#${a}#${k}`;
const anhelosDe = a => respuesta(claveVida(a, "anhelos")).split("\n").map(x => x.trim()).filter(Boolean);
/** Lo cumplido de un año: metas logradas y publicaciones de logro. */
function cumplidosDe(a) {
  const metas = Store.lista("meta_anio").filter(m => m.anio === a && (m.estado === "CUMPLIDA" || Number(m.avance) >= 100));
  const logros = (estado.misPosts || []).filter(p => p.tipo === "LOGRO" && (p.anio ?? new Date(p.creadoEn).getFullYear()) === a);
  return { metas, logros };
}
/** La línea de vida: una cinta de años de punta a punta, centrada en el que se mira. */
function lineaVida(actual) {
  const r = recordatorio(); if (!r) return "";
  const nac = r.nac.getFullYear(), fin = nac + metaVida(), hoy = hoyFecha().getFullYear(), metas = Store.lista("meta_anio");
  const fotos = new Map(); (estado.misPosts || []).forEach(p => { const a = p.anio ?? new Date(p.creadoEn).getFullYear(); if (p.foto && !fotos.has(a)) fotos.set(a, p.foto); });
  let h = "";
  for (let a = nac; a <= fin; a++) {
    const ms = metas.filter(m => m.anio === a), ejes = [...new Set(ms.map(m => m.eje || "CON"))].slice(0, 4);
    const palabra = respuesta(claveVida(a, "palabra")), carta = respuesta(claveVida(a, "carta")), foto = fotos.get(a);
    h += `<button class="lv-anio${a === hoy ? " hoy" : a < hoy ? " vivido" : ""}${a === actual ? " actual" : ""}" data-ir="ruta" data-arg="a:${a}" ${a === actual ? 'aria-current="true"' : ""}
      aria-label="${a}, ${a - nac} años${palabra ? ", " + esc(palabra) : ""}${ms.length ? `, ${ms.length} metas` : ""}">
      ${foto ? `<img src="${esc(foto)}" alt="" loading="lazy">` : ""}<b>${a}</b><small>${a - nac} años</small>
      ${palabra ? `<em>${esc(palabra)}</em>` : ""}<span class="lv-ejes">${ejes.map(e => `<i style="background:${COLOR_EJE[e] || "#7A4A6B"}"></i>`).join("")}${carta && a > hoy ? `<i class="lv-sello" title="Carta sellada"></i>` : ""}</span></button>`;
  }
  return `<div class="lv" role="navigation" aria-label="Tu línea de vida"><button class="lv-flecha" data-acc="lvMover" data-arg="-1" aria-label="Años anteriores">${ic("abajo")}</button>
    <div class="lv-cinta" data-lv="${actual}">${h}</div><button class="lv-flecha der" data-acc="lvMover" data-arg="1" aria-label="Años siguientes">${ic("abajo")}</button></div>`;
}
/** "Este día en otros años": lo que registraste un día como hoy. */
function esteDia() {
  const hoy = hoyFecha(), ps = (estado.misPosts || []).filter(p => { const f = new Date(p.creadoEn); return f.getMonth() === hoy.getMonth() && f.getDate() === hoy.getDate() && f.getFullYear() < hoy.getFullYear(); });
  if (!ps.length) return "";
  return hoja(`<h3 style="margin:0 0 4px">Este día en otros años</h3><p class="suave" style="margin:0 0 10px">Lo que viviste un ${hoy.getDate()} de ${MESES[hoy.getMonth()]}.</p>
    <div class="album album-tira">${ps.slice(0, 6).map(p => `<button class="recuerdo" data-ir="comunidad" data-arg="post:${esc(p.id)}">${p.foto ? `<img src="${esc(p.foto)}" alt="" loading="lazy">` : ""}<span><b>${new Date(p.creadoEn).getFullYear()}</b>${esc(p.metaTitulo || p.texto || "")}</span></button>`).join("")}</div>`);
}
/** El corazón del año: su palabra y sus anhelos y sueños (por vivir) o lo que anhelabas (vivido). */
function corazonAnio(a) {
  const hoy = hoyFecha().getFullYear(), futuro = a > hoy, edad = perfil().anioNacimiento ? a - perfil().anioNacimiento : null;
  const titulo = futuro ? `Lo que sueñas para ${a}` : a === hoy ? `Tu ${a}` : `Lo que fue ${a}`;
  const ayuda = futuro ? "Escribe tus anhelos y sueños para ese año, uno por línea. Después conviértelos en metas." : a === hoy ? "La palabra que guía tu año y lo que anhelas, uno por línea." : `Cómo recuerdas tu ${a}${edad !== null ? `, a los ${edad} años` : ""}: una palabra y lo que anhelabas o viviste.`;
  const anh = anhelosDe(a);
  return hoja(`<h3 style="margin:0 0 4px">${titulo}</h3><p class="suave" style="margin:0 0 12px">${ayuda}</p>
    <div class="form">${campo("Palabra del año", `<input type="text" maxlength="40" data-resp="vida|${claveVida(a, "palabra")}" value="${esc(respuesta(claveVida(a, "palabra")))}" placeholder="${futuro || a === hoy ? "Libertad, Familia, Cumbre…" : "La palabra de ese año"}">`)}
    ${campo(futuro ? "Anhelos y sueños" : "Anhelos", `<textarea rows="4" data-resp="vida|${claveVida(a, "anhelos")}" placeholder="${futuro ? "Conocer la Patagonia\nAbrir mi escuela\nCorrer mi primera maratón" : a === hoy ? "Lo que anhelas este año, uno por línea" : "Lo que anhelabas ese año"}">${esc(respuesta(claveVida(a, "anhelos")))}</textarea>`)}</div>
    ${anh.length && a >= hoy ? `<div class="chips" style="margin-top:10px">${anh.slice(0, 8).map(t => { const ya = Store.lista("meta_anio").find(m => m.anio === a && m.titulo.trim().toLowerCase() === t.toLowerCase());
        return ya ? `<button class="chip" aria-pressed="true" data-ir="metas" data-arg="a:${ya.id}" title="Ya es meta de ${a}">${ic("check")} ${esc(t)}</button>`
          : `<button class="chip" data-acc="anheloAMeta" data-arg="${a}|${esc(t)}" title="Convertir en meta de ${a}">${ic("mas")} ${esc(t)}</button>`; }).join("")}</div>
      <p class="suave" style="margin:6px 0 0;font-size:13px">Toca un anhelo para convertirlo en meta de ${a}.</p>` : ""}`);
}
/** Lo cumplido del año: metas logradas y logros publicados. */
function cumplidosAnio(a) {
  const { metas, logros } = cumplidosDe(a);
  if (!metas.length && !logros.length) return "";
  return hoja(`<h3 style="margin:0 0 10px">Cumplidos de ${a}</h3>
    ${metas.map(m => `<button class="cumplido" data-ir="metas" data-arg="a:${m.id}">${ic("bandera", "i fijado")}<span>${esc(m.titulo)}</span></button>`).join("")}
    ${logros.length ? `<div class="album album-tira" style="margin-top:10px">${logros.slice(0, 6).map(p => `<button class="recuerdo" data-ir="comunidad" data-arg="post:${esc(p.id)}">${p.foto ? `<img src="${esc(p.foto)}" alt="" loading="lazy">` : ""}<span>${esc(p.metaTitulo || p.texto || "")}</span></button>`).join("")}</div>` : ""}`);
}
/** Carta a tu yo de ese año: si el año está por venir, queda sellada con lacre hasta el 1 de enero. */
function cartaAnio(a) {
  const hoy = hoyFecha().getFullYear(), texto = respuesta(claveVida(a, "carta")), futuro = a > hoy, editar = estado.cartaEditar === a;
  if (futuro && texto && !editar) return hoja(`<div class="carta-sellada"><img src="${LOGO}" alt="Sello de lacre"><div><h3 style="margin:0">Carta a tu yo de ${a}</h3>
      <p class="suave" style="margin:4px 0 10px">Sellada. Se abre el 1 de enero de ${a}.</p><button class="btn mini" data-acc="cartaEditar" data-arg="${a}">Reescribirla</button></div></div>`);
  if (!futuro && texto && !editar) return hoja(`<h3 style="margin:0 0 6px">${a === hoy ? "La carta que te escribiste para este año" : `Tu carta de ${a}`}</h3>
      <p class="carta-texto">${esc(texto).replace(/\n/g, "<br>")}</p><button class="btn mini" data-acc="cartaEditar" data-arg="${a}">Editar</button>`);
  if (!futuro && !texto && a < hoy) return "";
  return hoja(`<h3 style="margin:0 0 4px">Carta a tu yo de ${a}</h3><p class="suave" style="margin:0 0 10px">${futuro ? `Escríbele a la persona que serás en ${a}. Se sella con lacre y se abre el 1 de enero de ese año.` : "Lo que quieres decirte este año."}</p>
    <textarea rows="6" class="carta-area" data-resp="vida|${claveVida(a, "carta")}" placeholder="Querido yo de ${a}…">${esc(texto)}</textarea>
    <div class="botones" style="margin-top:8px"><button class="btn mini lleno" data-acc="cartaEditar" data-arg="">${futuro ? "Sellar la carta" : "Listo"}</button></div>`);
}


/* ======================================================================
 * La cascada completa: cumbres personales (propósitos, que abarcan varios años) → años → meses → días →
 * actividades del día. Cada nivel muestra de dónde viene y qué lo sostiene abajo.
 * ====================================================================== */
/** Los años que abarca una cumbre personal: desde que se creó (o su primera meta) hasta su horizonte. */
function aniosDeCumbre(p) {
  const metas = Store.lista("meta_anio").filter(m => m.propositoId === p.id).map(m => m.anio);
  const ini = Math.min(p.creadoEn ? new Date(p.creadoEn).getFullYear() : hoyFecha().getFullYear(), ...metas);
  const fin = Math.max(ini + (p.horizonte || 5) - 1, ...metas);
  return { ini, fin };
}
const cumbresDelAnio = a => Store.lista("proposito").filter(p => { const r = aniosDeCumbre(p); return a >= r.ini && a <= r.fin; });
/** "↑ meta del año · cumbre": de dónde viene una meta del mes o del año. */
function cadena(tipo, o) {
  const partes = [];
  if (tipo === "meta_mes" && o.metaAnualId != null) { const a = Store.get("meta_anio", o.metaAnualId); if (a) { partes.push(`<button data-ir="metas" data-arg="a:${a.id}">${esc(a.titulo)}</button>`); o = a; tipo = "meta_anio"; } }
  if (tipo === "meta_anio" && o.propositoId != null) { const p = Store.get("proposito", o.propositoId); if (p) partes.push(`<button data-ir="metas" data-arg="p:${p.id}">${ic("ruta")} ${esc(p.titulo)}</button>`); }
  return partes.length ? `<span class="cadena">↑ ${partes.join(" · ")}</span>` : "";
}
/** La cinta de años de una cumbre personal, con las metas de cada año. */
function cintaCumbre(p) {
  const { ini, fin } = aniosDeCumbre(p), hoy = hoyFecha().getFullYear(), metas = Store.lista("meta_anio").filter(m => m.propositoId === p.id);
  let h = "";
  for (let a = ini; a <= fin; a++) {
    const n = metas.filter(m => m.anio === a).length;
    h += `<button class="cc-anio${a === hoy ? " hoy" : a < hoy ? " vivido" : ""}" data-ir="ruta" data-arg="a:${a}"><b>${a}</b><small>${n ? `${n} ${n === 1 ? "meta" : "metas"}` : "sin metas"}</small></button>`;
  }
  return `<div class="cc-cinta" aria-label="Años de esta cumbre">${h}</div>`;
}
/** Año: las cumbres personales en curso, en qué año de su camino van y sus metas de este año. */
function cumbresAnio(a) {
  const ps = cumbresDelAnio(a); if (!ps.length) return "";
  const c = cascada();
  return hoja(`<h3 style="margin:0 0 4px">Cumbres personales en ${a}</h3><p class="suave" style="margin:0 0 10px">Tus propósitos de largo plazo que pasan por este año, y lo que les aportas en ${a}.</p>
    ${ps.map(p => { const r = aniosDeCumbre(p), ms = c.anuales.filter(m => m.propositoId === p.id && m.anio === a);
      return `<div class="cumbre-anio"><button class="cumbre-anio-cab" data-ir="metas" data-arg="p:${p.id}"><i class="eje-punto" style="background:${COLOR_EJE[p.eje] || "#7A4A6B"}"></i>
          <span><b>${esc(p.titulo)}</b><small>Año ${a - r.ini + 1} de ${r.fin - r.ini + 1} · ${r.ini}–${r.fin}</small></span><span class="pct">${pctTxt(c.nodoProp(p))}</span></button>
        ${ms.map(m => `<button class="cumbre-anio-meta" data-ir="metas" data-arg="a:${m.id}"><span>${esc(m.titulo)}</span><span class="pct">${pctTxt(c.nodoAnio(m))}</span></button>`).join("") || `<p class="suave" style="margin:4px 0">Aún no tiene metas para ${a}.</p>`}
        <button class="btn mini oro" data-acc="nuevaMetaAnio" data-arg="${a}|${p.id}|${p.eje || ""}">${ic("mas")} Meta de ${a} para esta cumbre</button></div>`; }).join("")}`);
}
/** Mes: metas del año que todavía no tienen un paso este mes (para bajar la meta grande a este mes). */
function sinPasoEsteMes(a, m) {
  const delMes = metasDelMes(a, m), anuales = Store.lista("meta_anio").filter(x => x.anio === a && x.estado !== "CUMPLIDA" && !delMes.some(y => y.metaAnualId === x.id));
  if (!anuales.length) return "";
  return `<div class="sin-paso"><p class="suave" style="margin:10px 0 6px">Metas de ${a} sin paso en ${MESES[m - 1]}:</p>
    ${anuales.map(x => `<button class="chip" data-acc="nuevaMetaMes" data-arg="${a}-${String(m).padStart(2, "0")}|${x.id}">${ic("mas")} ${esc(x.titulo)}</button>`).join("")}</div>`;
}
/** Actividades del día por hora (el mismo horario de la app), cada una puede ir a una meta del mes: al hacerla, el día cuenta en esa meta. */
const horarioDe = ag => { try { return JSON.parse(ag.horario || "{}") || {}; } catch { return {}; } };
function actividadesDia(fecha, ag, ms) {
  // Las 24 horas, de 4:00 a 3:00 (el mismo orden que la app): el día empieza al madrugar, no a medianoche
  const hor = horarioDe(ag), horas = [...Array(24).keys()].map(i => (i + 4) % 24);
  const f = deIso(fecha), hoyF = hoyFecha();
  const fila = h => {
    const t = hor[h] || "", meta = respuesta(`dia#${fecha}#${h}#meta`), hecho = respuesta(`dia#${fecha}#${h}#hecho`) === "1";
    const ahora = fecha === iso(hoyF) && new Date().getHours() === Number(h);
    return `<div class="act${hecho ? " hecha" : ""}${ahora ? " ahora" : ""}"><span class="act-h">${ahora ? "Ahora" : String(h).padStart(2, "0") + ":00"}</span>
      <input type="checkbox" data-acc-change="actHecha" data-arg="${fecha}|${h}" ${hecho ? "checked" : ""} ${!t ? "disabled" : ""} aria-label="Hecha">
      <input type="text" class="act-t" value="${esc(t)}" placeholder="—" data-acc-change="actTexto" data-arg="${fecha}|${h}" aria-label="Actividad de las ${h}:00">
      ${t ? `<select data-acc-change="actMeta" data-arg="${fecha}|${h}" aria-label="Meta del mes que mueve"><option value="">Sin meta</option>${ms.map(x => `<option value="${x.id}" ${String(x.id) === meta ? "selected" : ""}>${esc(x.texto)}</option>`).join("")}</select>` : `<span></span>`}</div>`;
  };
  return hoja(`<h3 style="margin:0">Actividades del día</h3>
    <p class="suave" style="margin:4px 0 10px">Escribe lo que harás a cada hora y elige a qué meta del mes aporta: al marcarla hecha, el día cuenta en esa meta.</p>
    <div class="acts">${horas.map(fila).join("")}</div>`);
}


/* ======================================================================
 * Planificador diario (igual en la app): ritual de la mañana con las 3 cumbres de hoy (método Ivy Lee:
 * pocas, en orden, empezando por la primera), bloques por hora con la hora actual marcada, cierre del día
 * que deja escritas las 3 de mañana y pasa lo pendiente. Las 3 se guardan como respuesta "dia#<fecha>#top"
 * (JSON [{t, meta, hecho}]); la primera también va a agenda.prioridad, el campo que ya tiene la app.
 * ====================================================================== */
const topDe = fecha => { try { const v = JSON.parse(respuesta(`dia#${fecha}#top`) || "[]"); return Array.isArray(v) ? v.slice(0, 3) : []; } catch { return []; } };
function guardarTop(fecha, xs) {
  const l = xs.slice(0, 3).map(x => ({ t: String(x.t || "").slice(0, 160), meta: x.meta ? String(x.meta) : "", hecho: !!x.hecho })).filter(x => x.t);
  responder("dia", `dia#${fecha}#top`, l.length ? JSON.stringify(l) : "");
  Store.cambiar("agenda", fecha, { prioridad: l[0]?.t || "" }, BASES.agenda(fecha));
}
const sumarDias = (fecha, n) => { const d = deIso(fecha); d.setDate(d.getDate() + n); return iso(d); };
/** Lo que quedó sin hacer un día: sus cumbres no hechas y sus actividades con texto no hechas. */
function pendientesDe(fecha) {
  const top = topDe(fecha).filter(x => !x.hecho), hor = horarioDe(Store.get("agenda", fecha) || {});
  const acts = Object.entries(hor).filter(([h]) => respuesta(`dia#${fecha}#${h}#hecho`) !== "1").map(([h, t]) => ({ h, t, meta: respuesta(`dia#${fecha}#${h}#meta`) }));
  return { top, acts };
}
function filaTop(fecha, i, x, ms, editable) {
  return `<div class="top${x?.hecho ? " hecha" : ""}"><span class="top-n">${i + 1}</span>
    <input type="checkbox" data-acc-change="topHecha" data-arg="${fecha}|${i}" ${x?.hecho ? "checked" : ""} ${!x?.t || !editable ? "disabled" : ""} aria-label="Hecha">
    <input type="text" class="top-t" value="${esc(x?.t || "")}" placeholder="${["Lo más importante", "Lo segundo", "Lo tercero"][i]}" maxlength="160" data-acc-change="topTexto" data-arg="${fecha}|${i}" aria-label="Cumbre ${i + 1} del día">
    ${x?.t ? `<select data-acc-change="topMeta" data-arg="${fecha}|${i}" aria-label="Meta del mes que mueve"><option value="">Sin meta</option>${ms.map(m => `<option value="${m.id}" ${String(m.id) === x.meta ? "selected" : ""}>${esc(m.texto)}</option>`).join("")}</select>` : "<span></span>"}</div>`;
}
/** El ritual de la mañana: intención y las 3 cumbres de hoy. */
function planDelDia(fecha, ag, ms) {
  const top = topDe(fecha), hoyF = iso(hoyFecha()), ayer = sumarDias(fecha, -1), p = pendientesDe(ayer);
  const traer = fecha === hoyF && (p.top.length || p.acts.length) && !respuesta(`dia#${fecha}#traido`);
  return hoja(`<h3 style="margin:0 0 4px">Plan del día</h3><p class="suave" style="margin:0 0 12px">Pocas cosas, en orden: empieza por la 1 y no pases a la siguiente hasta terminarla. Únelas a una meta del mes para que cuenten allá.</p>
    ${campo("Intención del día", `<input type="text" ${bind("agenda", fecha, "intencion", "", "agenda")} value="${esc(ag.intencion || "")}" placeholder="Hoy quiero…">`)}
    <div class="etiqueta" style="margin:12px 0 6px">Las 3 cumbres de hoy</div>
    <div class="tops">${[0, 1, 2].map(i => filaTop(fecha, i, top[i], ms, true)).join("")}</div>
    ${traer ? `<div class="traer"><span>Ayer quedaron ${p.top.length + p.acts.length} pendientes.</span><button class="btn mini oro" data-acc="traerPendientes" data-arg="${fecha}">Traerlos a hoy</button><button class="btn mini" data-acc="traerNo" data-arg="${fecha}">Soltarlos</button></div>` : ""}`);
}
/** El cierre: lo que salió, y las 3 cumbres de mañana escritas antes de dormir (Ivy Lee). */
function cierreDia(fecha, ag, campoAg) {
  const man = sumarDias(fecha, 1), topM = topDe(man), d = deIso(man), msM = metasDelMes(d.getFullYear(), d.getMonth() + 1), p = pendientesDe(fecha);
  return hoja(`<h3>Cierre del día</h3><div class="form">${campoAg("victorias", "Victorias de hoy")}${campoAg("aprendizaje", "Aprendizaje")}${campoAg("gratitud", "Gratitud")}
      <div class="form-2">${campo("Energía", `<div class="energia">${[1, 2, 3, 4, 5].map(n => `<button class="${(ag.energia || 0) >= n ? "si" : ""}" data-acc="energia" data-arg="${fecha}|${n}" aria-label="Energía ${n}"></button>`).join("")}</div>`)}
        <div class="campo"><span>Vasos de agua</span>${vasosDeAgua(fecha, ag.agua || 0)}</div></div>
      ${campoAg("notas", "Notas")}</div>
    <div class="etiqueta" style="margin:16px 0 6px">Mañana · tus 3 cumbres</div>
    <p class="suave" style="margin:0 0 8px;font-size:13px">Escríbelas antes de dormir: mañana empiezas sin decidir.</p>
    <div class="tops">${[0, 1, 2].map(i => filaTop(man, i, topM[i], msM, false)).join("")}</div>
    ${p.top.length || p.acts.length ? `<div class="botones" style="margin-top:8px"><button class="btn mini" data-acc="pasarAManana" data-arg="${fecha}">Pasar ${p.top.length + p.acts.length} pendientes a mañana</button></div>` : ""}`);
}

const DET = {
  /* ---------- Mi ruta: vida → año → mes → día ---------- */
  /* ---------- Mi ruta: Cumbre y Brújula (la visión de toda la vida) ---------- */
  vida() {
    return cab(av(ic("cima"), "var(--burdeos)"), "Cumbre y Brújula", "Tu cumbre, tu vision board y los 81 pasos del 9×9") + cuerpo(
      visionCompleta() + (window.visionVida ? visionVida() : ""));
  },
  /* ---------- Mi ruta: Mapa de mi vida (el tiempo): recordatorio del día y un punto por año ---------- */
  puntos() {
    const r = recordatorio();
    if (!r) return vacio();
    const nac = r.nac.getFullYear(), anioHoy = new Date().getFullYear();
    const conMetas = new Set(Store.lista("meta_anio").map(m => m.anio));
    // Cada fila es una década: a la izquierda, el año en que empieza; el punto dorado es este año
    let puntos = "";
    for (let i = 0; i < metaVida(); i++) {
      const a = nac + i, cls = a < anioHoy ? "vivido" : a === anioHoy ? "actual" : "futuro";
      if (i % 10 === 0) puntos += `<span class="pv-fila${anioHoy >= a && anioHoy < a + 10 ? " esta" : ""}" aria-hidden="true">${a}</span>`;
      puntos += `<button class="pv ${cls}${conMetas.has(a) ? " meta" : ""}" data-sel="a:${a}" aria-label="${a}, ${i} años${conMetas.has(a) ? ", con metas" : ""}" title="${a} · ${i} años${conMetas.has(a) ? " · con metas" : ""}"></button>`;
    }
    return cab(av(ic("puntos"), "var(--burdeos)"), "Mapa de mi vida", `${r.edad} de ${metaVida()} años · un punto por año`) + cuerpo(
      hoja(`<div class="cielo-mini" data-cielo aria-hidden="true"></div><div class="recordatorio"><div><div class="etiqueta">Recordatorio del día</div>
        <div style="display:flex;gap:28px;flex-wrap:wrap;margin-top:10px">
          <div><div class="cifra">${r.dia.toLocaleString("es")}</div><div class="suave">día de tu vida</div></div>
          <div><div class="cifra">${r.quedan.toLocaleString("es")}</div><div class="suave">días hasta los ${metaVida()}</div></div></div></div>
        <div style="text-align:right"><div class="cifra" style="color:var(--oro)">${Math.round(r.dia / r.total * 100)}%</div><div class="suave">del camino</div></div></div>
        <div class="barra" style="margin-top:14px"><i style="width:${Math.min(100, r.dia / r.total * 100)}%"></i></div>
        <p class="frase-vida">${r.frase}</p>`) + esteDia() +
      hoja(`<p class="suave" style="margin:0 0 16px">Un punto por año. Toca uno para abrirlo mes a mes. El anillo que gira marca los años con metas.</p>
        <div class="puntos">${puntos}</div>
        <p class="pv-hoy"><i></i>${anioHoy} · ${r.edad} años</p>
        <p class="suave" style="font-size:13px;margin:4px 0 0">Cada fila es una década: a la izquierda, el año en que empieza.</p>
        <div class="leyenda" style="margin-top:16px"><span><i style="background:var(--burdeos)"></i>Vivido</span><span><i style="background:var(--oro)"></i>Este año</span><span><i style="background:var(--hoja);border:1px solid var(--linea)"></i>Por vivir</span><span><i class="ley-meta" aria-hidden="true"></i>Año con metas</span></div>`));
  },
  "ruta.a"(arg) {
    const a = Number(arg), p = perfil(), edad = p.anioNacimiento ? a - p.anioNacimiento : null;
    const hoy = hoyFecha(), c = cascada();
    let meses = "";
    for (let m = 1; m <= 12; m++) {
      const n = diasEnMes(a, m), ini = (new Date(a, m - 1, 1).getDay() + 6) % 7;
      let dias = "", cuenta = 0, total = 0;
      for (let k = 0; k < ini; k++) dias += `<i class="dia vacio-d"></i>`;
      for (let d = 1; d <= n; d++) {
        const f = new Date(a, m - 1, d), pasado = f <= hoy, esHoy = f.getTime() === hoy.getTime();
        const ok = pasado && diaCumplido(f);
        if (pasado) { total++; if (ok) cuenta++; }
        dias += `<i class="dia ${ok ? "hecho" : ""} ${pasado && !esHoy ? "pasado" : ""} ${esHoy ? "hoy" : ""}" title="${d} de ${MESES[m - 1]}"></i>`;
      }
      const nm = metasDelMes(a, m).length;
      meses += `<button class="hoja mes clic" data-sel="m:${a}-${String(m).padStart(2, "0")}" style="text-align:left"><h4>${mayus(MESES[m - 1])} <span>${total ? cuenta + "/" + total : nm ? nm + " metas" : ""}</span></h4><div class="dias">${dias}</div></button>`;
    }
    const metas = c.anuales.filter(x => x.anio === a);
    return cab(av(edad ?? "·", a < hoy.getFullYear() ? "var(--burdeos)" : a === hoy.getFullYear() ? "var(--oro)" : "#B8A99C"),
      `${a}${edad !== null ? ` · ${edad} años` : ""}`, a < hoy.getFullYear() ? "Año vivido" : a === hoy.getFullYear() ? "Año en curso" : "Por vivir") + cuerpo(
      `<div class="migas"><button data-sel="puntos">Mapa de mi vida</button> › <span>${a}</span></div>` + lineaVida(a) +
      `<div class="anio-nav"><button class="btn mini" data-ir="ruta" data-arg="a:${a - 1}" aria-label="Año anterior: ${a - 1}">‹ ${a - 1}</button>
        <span class="suave">Desliza o usa ← → para cambiar de año</span><button class="btn mini" data-ir="ruta" data-arg="a:${a + 1}" aria-label="Año siguiente: ${a + 1}">${a + 1} ›</button></div>` +
      corazonAnio(a) + cumbresAnio(a) +
      hoja(`<div class="cab-lista-btn"><div class="etiqueta">Metas del año</div><button class="btn mini oro" data-acc="nuevaMetaAnio" data-arg="${a}">${ic("mas")} Nueva meta</button></div>
        ${metas.length ? metas.map(m => `<div class="meta-fila"><div class="arriba"><button style="text-align:left" data-ir="metas" data-arg="a:${m.id}"><i class="eje-punto" style="background:${COLOR_EJE[ejeEfectivo("meta_anio", m)] || "#7A4A6B"}" title="${esc(NOMBRE_EJE[ejeEfectivo("meta_anio", m)] || "Confluencia")}"></i> ${esc(m.titulo)}${(() => { const n = c.mensuales.filter(x => x.metaAnualId === m.id).length; return n ? ` <span class="suave" style="font-size:12px">· ${n} ${n === 1 ? "mes" : "meses"}</span>` : ""; })()}</button><span class="pct">${pctTxt(c.nodoAnio(m))}</span></div><div class="barra"><i style="width:${c.nodoAnio(m) * 100}%"></i></div></div>`).join("")
          : `<p class="suave" style="margin:8px 0 0">Todavía no hay metas para ${a}.</p>`}`) +
      (a <= hoy.getFullYear() ? albumAnio(a, metas) : visionAnio(a)) + cumplidosAnio(a) + cartaAnio(a) + `<div class="meses">${meses}</div>`);
  },
  "ruta.m"(arg) { return detMes(arg); },
  "ruta.d"(arg) { return detDia(arg); },
  "hoy.d"(arg) { return detDia(arg); },
  "hoy.mm"(arg) { return detMetaMes(Number(arg)); },

  /* ---------- Metas: propósitos, metas del año y del mes ---------- */
  "metas.nuevo"() {
    const anioHoy = new Date().getFullYear();
    return cab(av(ic("mas"), "var(--burdeos)"), "Nueva meta", "Elige el nivel de la cascada") + cuerpo(
      (window.deTuMochila ? deTuMochila() : "") + hoja(`<h3>Propósito a largo plazo</h3><p class="suave">Tu cumbre a los años que tú elijas. De aquí salen las metas de cada año.</p><button class="btn lleno" data-acc="nuevoProposito">Crear propósito</button>`) +
      hoja(`<h3>Meta del año</h3><p class="suave">Tu campamento base: lo que lograrás este año para acercarte al propósito.</p><button class="btn lleno" data-acc="nuevaMetaAnio" data-arg="${anioHoy}">Crear meta de ${anioHoy}</button>`) +
      hoja(`<h3>Meta del mes</h3><p class="suave">Lo que harás este mes. Cada día que la cumplas, el avance sube solo hasta tu propósito.</p><button class="btn lleno" data-acc="nuevaMetaMes" data-arg="${iso(hoyFecha()).slice(0, 7)}">Crear meta de ${MESES[hoyFecha().getMonth()]}</button>`), "max-width:640px");
  },
  "metas.p"(arg) {
    const p = Store.get("proposito", arg); if (!p) return vacio();
    const c = cascada(), k = String(p.id);
    const acciones = Store.lista("accion").filter(x => x.propositoId === p.id).sort((x, y) => (x.orden || 0) - (y.orden || 0) || x.id - y.id);
    const anuales = c.anuales.filter(x => x.propositoId === p.id);
    const auto = anuales.length > 0;
    return cab(av(pctTxt(c.nodoProp(p)), COLOR_EJE[p.eje] || "var(--burdeos)"), esc(p.titulo), `Propósito a ${p.horizonte || 5} años`,
      `<button class="btn mini peligro" data-acc="borrar" data-arg="proposito|${k}">${ic("borrar")} Borrar</button>`) + cuerpo(
      (window.deTuMochila ? deTuMochila() : "") + (window.franjaCampamento ? franjaCampamento("proposito", p) : "") +
      hoja(`<h3 style="margin:0 0 4px">Los años de esta cumbre</h3><p class="suave" style="margin:0 0 8px">${aniosDeCumbre(p).ini}–${aniosDeCumbre(p).fin}: toca un año para ver sus metas, meses y días.</p>${cintaCumbre(p)}`) +
      hoja(`<div class="form">${campo("Propósito", txt("proposito", k, "titulo", p.titulo))}
        ${selectorHorizonte("proposito", k, p.horizonte || 5)}
        <div class="form-2">          ${campo("Eje · campamento del vision board", sel("proposito", k, "eje", p.eje || "", opcionesEje, "nulo"))}
          ${campo("Prioridad", sel("proposito", k, "prioridad", p.prioridad || "B", PRIORIDADES.map(x => [x, PRIORIDAD_NOMBRE[x]])))}</div>
        ${auto ? `<p class="suave" style="margin:0">Avance automático: ${pctTxt(c.nodoProp(p))}, el promedio de sus metas anuales.</p>`
          : campo(`Avance manual`, `<div class="rango"><input type="range" min="0" max="100" step="5" value="${p.progreso || 0}" ${bind("proposito", k, "progreso", "num")}><b>${p.progreso || 0}%</b></div>`)}
        ${campo("Descripción", area("proposito", k, "descripcion", p.descripcion))}
        ${campo("Indicador de éxito", area("proposito", k, "indicadorExito", p.indicadorExito, "¿Cómo sabrás que llegaste?"))}
        ${campo("¿Por qué me importa?", area("proposito", k, "porQueImporta", p.porQueImporta))}
        ${campo("Metas específicas (una por línea)", area("proposito", k, "metasEspecificas", p.metasEspecificas))}
        ${campo("Visualización", area("proposito", k, "visualizacion", p.visualizacion))}</div>`) +
      hoja(`<div class="cab-lista-btn"><div class="etiqueta">Plan de acción</div></div>
        ${acciones.map(x => `<label class="check"><input type="checkbox" ${bind("accion", x.id, "hecha", "bool")} ${x.hecha ? "checked" : ""}><span>${esc(x.texto)}</span>
          <button class="btn mini" data-acc="borrar" data-arg="accion|${x.id}" aria-label="Borrar paso" style="margin-left:auto">${ic("borrar")}</button></label>`).join("") || `<p class="suave">Sin pasos todavía.</p>`}
        <form class="escribir" data-form="nuevaAccion" data-arg="${p.id}" style="background:none;border:0;padding:8px 0 0"><input name="texto" placeholder="Nuevo paso del plan" aria-label="Nuevo paso" autocomplete="off"><button class="enviar" aria-label="Agregar">${ic("mas")}</button></form>`) +
      hoja(`<div class="cab-lista-btn"><div class="etiqueta">Metas anuales de este propósito</div><button class="btn mini oro" data-acc="nuevaMetaAnio" data-arg="${new Date().getFullYear()}|${p.id}">${ic("mas")} Nueva</button></div>
        ${anuales.map(m => `<div class="meta-fila"><div class="arriba"><button style="text-align:left" data-sel="a:${m.id}">${m.anio} · ${esc(m.titulo)}</button><span class="pct">${pctTxt(c.nodoAnio(m))}</span></div></div>`).join("") || `<p class="suave">Conecta metas del año a este propósito para que el avance suba solo.</p>`}`));
  },
  "metas.a"(arg) {
    const m = Store.get("meta_anio", arg); if (!m) return vacio();
    const c = cascada(), k = String(m.id);
    const hijos = c.mensuales.filter(x => x.metaAnualId === m.id).sort((x, y) => x.anio - y.anio || x.mes - y.mes);
    const enlace = window.franjaCampamento ? franjaCampamento("meta_anio", m) : "";
    return cab(av(pctTxt(c.nodoAnio(m)), COLOR_EJE[m.eje] || "var(--oro)"), esc(m.titulo), `Meta de ${m.anio}`,
      `<button class="btn mini peligro" data-acc="borrar" data-arg="meta_anio|${k}">${ic("borrar")} Borrar</button>`) + cuerpo(
      (window.deTuMochila ? deTuMochila() : "") +
      enlace + hoja(`<div class="form">${campo("Meta", txt("meta_anio", k, "titulo", m.titulo))}
        <div class="form-2">${campo("Año", `<input type="number" min="1900" max="2200" ${bind("meta_anio", k, "anio", "num")} value="${m.anio}">`)}
          ${campo("Propósito", sel("meta_anio", k, "propositoId", m.propositoId ?? "", [["", "Sin propósito"], ...c.props.map(p => [p.id, p.titulo])], "numNulo"))}
          ${campo("Eje · campamento del vision board", sel("meta_anio", k, "eje", m.eje || "", opcionesEje, "nulo"))}
          ${campo("Prioridad", sel("meta_anio", k, "prioridad", m.prioridad || "B", PRIORIDADES.map(x => [x, PRIORIDAD_NOMBRE[x]])))}
          ${campo("Estado", sel("meta_anio", k, "estado", m.estado || "NO_INICIADA", ESTADOS))}
          ${campo("Decisión", sel("meta_anio", k, "decision", m.decision || "CONTINUAR", DECISIONES))}</div>
        ${hijos.length ? `<p class="suave" style="margin:0">Avance automático: ${pctTxt(c.nodoAnio(m))}, el promedio de sus metas del mes.</p>`
          : campo("Avance manual", `<div class="rango"><input type="range" min="0" max="100" step="5" value="${m.avance || 0}" ${bind("meta_anio", k, "avance", "num")}><b>${m.avance || 0}%</b></div>`)}
        ${campo("Sub-metas (una por línea)", area("meta_anio", k, "subMetas", m.subMetas))}
        ${campo("Indicador", txt("meta_anio", k, "indicador", m.indicador))}
        ${campo("Evidencia observable", area("meta_anio", k, "observable", m.observable, "¿Cómo sabrás que la lograste?"))}
        ${campo("Obstáculo", area("meta_anio", k, "obstaculo", m.obstaculo))}
        ${campo("Próxima acción", txt("meta_anio", k, "proximaAccion", m.proximaAccion))}</div>`) +
      hoja(`<div class="cab-lista-btn"><div class="etiqueta">Metas del mes que la alimentan</div><button class="btn mini oro" data-acc="nuevaMetaMes" data-arg="${iso(hoyFecha()).slice(0, 7)}|${m.id}">${ic("mas")} Nueva</button></div>
        ${hijos.map(x => `<div class="meta-fila"><div class="arriba"><button style="text-align:left" data-sel="m:${x.id}">${mayus(MESES[x.mes - 1])} ${x.anio} · ${esc(x.texto)}</button><span class="pct">${pctTxt(avanceMetaMes(x))}</span></div></div>`).join("") || `<p class="suave">Sin metas del mes conectadas.</p>`}`));
  },
  "metas.m"(arg) { return detMetaMes(Number(arg)); },
};

function detMetaMes(id) {
  const m = Store.get("meta_mes", id); if (!m) return vacio();
  const k = String(m.id), c = cascada();
  return cab(av(pctTxt(avanceMetaMes(m)), COLOR_EJE[m.eje] || "var(--burdeos)"), esc(m.texto), `Meta de ${MESES[m.mes - 1]} ${m.anio}`,
    `<button class="btn mini peligro" data-acc="borrar" data-arg="meta_mes|${k}">${ic("borrar")} Borrar</button>`) + cuerpo(
    (window.franjaCampamento ? franjaCampamento("meta_mes", m) : "") + hoja(`<div class="etiqueta">Días cumplidos · ${numeros(m.dias).length} de ${m.objetivoDias || 20}</div>${tiraDias(m)}
      <label class="check" style="margin-top:10px"><input type="checkbox" ${bind("meta_mes", k, "cumplida", "bool")} ${m.cumplida ? "checked" : ""}><span>Meta cumplida</span></label>`) +
    hoja(`<div class="form">${campo("Meta", txt("meta_mes", k, "texto", m.texto))}
      <div class="form-2">${campo("Meta del año", sel("meta_mes", k, "metaAnualId", m.metaAnualId ?? "", [["", "Sin conectar"], ...c.anuales.map(a => [a.id, `${a.anio} · ${a.titulo}`])], "numNulo"))}
        ${campo("Eje · campamento del vision board", sel("meta_mes", k, "eje", m.eje || "", [["", ejeEfectivo("meta_mes", { ...m, eje: null }) ? `El de su meta del año (${NOMBRE_EJE[ejeEfectivo("meta_mes", { ...m, eje: null })]})` : "Sin eje"], ...opcionesEje.slice(1)], "nulo"))}
        ${campo("Días objetivo", `<input type="number" min="1" max="31" ${bind("meta_mes", k, "objetivoDias", "num")} value="${m.objetivoDias || 20}">`)}</div>
      ${campo("Indicador", txt("meta_mes", k, "indicador", m.indicador))}</div>`), "max-width:720px");
}
function tiraDias(m) {
  const n = diasEnMes(m.anio, m.mes), hechos = new Set(numeros(m.dias)), hoy = hoyFecha();
  let h = `<div class="tira">`;
  for (let d = 1; d <= n; d++) {
    const f = new Date(m.anio, m.mes - 1, d), futuro = f > hoy;
    h += `<button class="${hechos.has(d) ? "si" : ""} ${f.getTime() === hoy.getTime() ? "hoy" : ""}" data-acc="diaMeta" data-arg="${m.id}|${d}" ${futuro ? "disabled" : ""} aria-pressed="${hechos.has(d)}" aria-label="${d} de ${MESES[m.mes - 1]}">${d}</button>`;
  }
  return h + `</div>`;
}
function detMes(arg) {
  const [a, m] = arg.split("-").map(Number), clave = arg;
  const ms = metasDelMes(a, m), b = Store.get("mes", clave) || {}, hoy = hoyFecha();
  const ini = (new Date(a, m - 1, 1).getDay() + 6) % 7;
  let cal = ["L", "M", "X", "J", "V", "S", "D"].map(x => `<i class="suave" style="font-size:11px;text-align:center;font-style:normal">${x}</i>`).join("");
  for (let k = 0; k < ini; k++) cal += `<i class="dia vacio-d"></i>`;
  for (let d = 1; d <= diasEnMes(a, m); d++) {
    const f = new Date(a, m - 1, d), pasado = f <= hoy, ok = pasado && diaCumplido(f), esHoy = f.getTime() === hoy.getTime();
    cal += `<button class="dia btn-dia ${ok ? "hecho" : ""} ${pasado && !esHoy ? "pasado" : ""} ${esHoy ? "hoy" : ""}" data-sel="d:${iso(f)}" aria-label="${d} de ${MESES[m - 1]}" style="font-size:11px">${d}</button>`;
  }
  const campoMes = (n, etq) => campo(etq, `<textarea ${bind("mes", clave, n, "", `mes`)}>${esc(b[n] || "")}</textarea>`);
  return cab(av(MES_C[m - 1], "var(--burdeos)"), `${mayus(MESES[m - 1])} ${a}`, `${ms.length} metas este mes`) + cuerpo(
    `<div class="migas"><button data-sel="puntos">Mapa de mi vida</button> › <button data-sel="a:${a}">${a}</button> › <span>${mayus(MESES[m - 1])}</span></div>` +
    hoja(`<div class="cab-lista-btn"><div class="etiqueta">Metas del mes</div><button class="btn mini oro" data-acc="nuevaMetaMes" data-arg="${clave}">${ic("mas")} Nueva meta</button></div>
      ${ms.map(x => `<div class="meta-fila"><div class="arriba"><button style="text-align:left" data-ir="metas" data-arg="m:${x.id}"><i class="eje-punto" style="background:${COLOR_EJE[ejeEfectivo("meta_mes", x)] || "#7A4A6B"}" title="${esc(NOMBRE_EJE[ejeEfectivo("meta_mes", x)] || "Confluencia")}"></i> ${esc(x.texto)}</button>${cadena("meta_mes", x)}<span class="pct">${pctTxt(avanceMetaMes(x))}</span></div>${tiraDias(x)}</div>`).join("") || `<p class="suave" style="margin:8px 0 0">Sin metas este mes.</p>`}${sinPasoEsteMes(a, m)}`) +
    hoja(`<div class="etiqueta" style="margin-bottom:8px">Días del mes · toca uno para abrirlo</div><div class="dias" style="max-width:320px">${cal}</div>`) +
    hoja(`<h3>Balance del mes</h3><div class="form">${campoMes("comoEstuvo", "¿Cómo estuvo el mes?")}${campoMes("logros", "Logros")}${campoMes("desafios", "Desafíos")}
      <div class="form-2">${campoMes("agradecido", "Estoy agradecido por")}${campoMes("mejorar", "Puedo mejorar")}</div>${campoMes("objetivosProximo", "Objetivos del próximo mes")}${campoMes("notas", "Notas")}</div>`) +
    (window.ascResumenMes ? ascResumenMes(a, m) : ""));
}
/** Los hábitos del día, un eje abierto a la vez (el primero que falta). Al completar uno se abre el siguiente. Se editan aquí mismo. */
function habitosDelDia(fecha, marc) {
  const HS = habitos(), total = HS.length, n = HS.filter(h => marc.has(h[0])).length;
  const editar = estado.habEditar;
  const cabeza = `<div class="hab-cab"><h3 id="habTit">${editar ? "Tus hábitos" : "Checklist de hábitos"}</h3>${editar ? "" : `<span class="hab-n">${n} / ${total}</span>`}
    <button class="btn mini" data-acc="habEditar" aria-pressed="${!!editar}">${editar ? ic("check") + " Listo" : ic("editar") + " Editar"}</button></div>`;
  if (editar) return `<section class="hoja hab" aria-labelledby="habTit">${cabeza}
    <p class="suave hab-lectura">Escríbelos a tu manera. Cada eje lleva de 1 a ${HABITOS_MAX} hábitos. Los días que ya marcaste se conservan.</p>
    ${EJES.map(([cod, nom]) => { const hs = HS.filter(h => h[1] === cod);
      return `<div class="hab-edit"><h4>${nom}</h4>${hs.map(([id, , t]) => `<div class="hab-fila"><input type="text" value="${esc(t)}" maxlength="120" data-acc-change="habRenombrar" data-arg="${esc(id)}" aria-label="Hábito de ${nom}">
        <button class="md-visor-ic" data-acc="habQuitar" data-arg="${esc(id)}" ${hs.length <= 1 ? "disabled" : ""} aria-label="Quitar hábito" title="Quitar">${ic("borrar")}</button></div>`).join("")}
        ${hs.length < HABITOS_MAX ? `<div class="hab-fila"><input type="text" id="habNuevo-${cod}" maxlength="120" placeholder="Nuevo hábito de ${nom.toLowerCase()}" aria-label="Nuevo hábito de ${nom}" onkeydown="if(event.key==='Enter'){event.preventDefault();ACC.habAgregar(null,'${cod}')}">
        <button class="md-visor-ic" data-acc="habAgregar" data-arg="${cod}" aria-label="Agregar hábito" title="Agregar">${ic("mas")}</button></div>` : ""}</div>`; }).join("")}
    <div class="botones" style="margin-top:12px">${estado.habFabrica ? `<button class="btn mini peligro" data-acc="habFabrica" data-arg="si">Sí, volver a los 18</button><button class="btn mini" data-acc="habFabrica" data-arg="no">Cancelar</button>`
      : `<button class="btn mini" data-acc="habFabrica">Volver a los de fábrica</button>`}</div></section>`;
  const pendiente = EJES.find(([cod]) => HS.some(h => h[1] === cod && !marc.has(h[0])))?.[0] || null;
  const elegido = estado.ejeAbierto?.fecha === fecha ? estado.ejeAbierto.eje : undefined;
  const visible = elegido !== undefined ? elegido : pendiente;
  const r = n / Math.max(1, total);
  const lectura = r >= 10 / 18 ? "Vas en buen camino." : r >= 8 / 18 ? "Avanzando. Busca llegar a dos tercios." : n > 0 ? "Menos de la mitad: revisa qué está pasando." : "Marca los hábitos que cumpliste hoy.";
  return `<section class="hoja hab" aria-labelledby="habTit">${cabeza}
    <div class="hab-linea" role="progressbar" aria-label="Hábitos marcados" aria-valuemin="0" aria-valuemax="${total}" aria-valuenow="${n}"><i style="--p:${r.toFixed(3)}"></i></div>
    <p class="suave hab-lectura">${lectura}</p>
    ${EJES.map(([cod, nom]) => { const hs = HS.filter(h => h[1] === cod), k = hs.filter(h => marc.has(h[0])).length;
      return `<details class="hab-eje" data-eje="${cod}" data-fecha="${fecha}" ${visible === cod ? "open" : ""}><summary><span>${nom}</span>${k === hs.length ? `<span class="hab-ok" aria-label="completo">${ic("check")}</span>` : `<span class="hab-k">${k} / ${hs.length}</span>`}<span class="hab-flecha">${ic("abajo")}</span></summary>
        ${hs.map(([id, , t]) => `<label class="check"><input type="checkbox" data-acc-change="habito" data-arg="${fecha}|${esc(id)}" ${marc.has(id) ? "checked" : ""}><span>${esc(t)}</span></label>`).join("")}</details>`; }).join("")}
  </section>`;
}
/* Recordar qué eje abrió la persona (el detalle se redibuja al marcar) */
document.addEventListener("toggle", e => {
  const d = e.target; if (!d.classList?.contains("hab-eje")) return;
  if (d.open) estado.ejeAbierto = { fecha: d.dataset.fecha, eje: d.dataset.eje };
  else if (estado.ejeAbierto?.eje === d.dataset.eje) estado.ejeAbierto = { fecha: d.dataset.fecha, eje: null };
}, true);
function detDia(fecha) {
  const f = deIso(fecha), hoy = hoyFecha(), esHoy = f.getTime() === hoy.getTime();
  const ag = Store.get("agenda", fecha) || {}, marc = marcadosDe(fecha), ms = metasDelMes(f.getFullYear(), f.getMonth() + 1);
  const c = cascada(), p = perfil();
  const campoAg = (n, etq, ph = "") => campo(etq, `<textarea ${bind("agenda", fecha, n, "", "agenda")} placeholder="${esc(ph)}">${esc(ag[n] || "")}</textarea>`);
  const habitos = habitosDelDia(fecha, marc);
  const sellada = esHoy && !abiertas(hoy.getFullYear()).has(fraseIndice(hoy));
  return cab(av(f.getDate(), "var(--burdeos)"), esHoy ? "Tu día" : mayus(f.toLocaleDateString("es", { weekday: "long" })), fechaLarga(f)) + cuerpo(
    `<div class="migas"><button data-ir="ruta" data-arg="puntos">Mapa de mi vida</button> › <button data-ir="ruta" data-arg="a:${f.getFullYear()}">${f.getFullYear()}</button> › <button data-ir="ruta" data-arg="m:${fecha.slice(0, 7)}">${mayus(MESES[f.getMonth()])}</button> › <span>${f.getDate()}</span></div>` +
    (esHoy ? `<div class="cielo-dia"><div class="cielo" data-cielo="hud"></div>${p.cumbreFrase ? `<button type="button" class="cielo-cumbre" data-ir="ruta" data-arg="vida" aria-label="Tu cumbre: ${esc(p.cumbreFrase)}. Ver tu ruta"><span class="frase-vida">${esc(p.cumbreFrase)}</span></button>` : ""}</div>` : "") +
    (sellada ? `<div class="aviso">${ic("frases")} Tu frase del día te espera sellada. <button class="btn" data-ir="frases" data-arg="f:${fecha}" style="margin-left:auto">Abrir</button></div>` : "") +
    planDelDia(fecha, ag, ms) +
    actividadesDia(fecha, ag, ms) + (esHoy && window.cordadasHoy ? cordadasHoy() : "") +
    hoja(`<h3>Metas del mes</h3><p class="suave ayuda-h">Marca si hoy diste el paso.</p>
      ${ms.map(x => { const si = numeros(x.dias).includes(f.getDate()); return `<label class="check"><input type="checkbox" data-acc-change="diaMeta" data-arg="${x.id}|${f.getDate()}" ${si ? "checked" : ""} ${f > hoy ? "disabled" : ""}><span><i class="eje-punto" style="background:${COLOR_EJE[ejeEfectivo("meta_mes", x)] || "#7A4A6B"}"></i>${esc(x.texto)} <span class="suave">· ${pctTxt(avanceMetaMes(x))}</span>${cadena("meta_mes", x)}</span></label>`; }).join("") || `<p class="suave" style="margin:8px 0 0">Aún no tienes metas este mes: una meta pequeña que marques cada día es el mejor primer paso. <button class="btn mini oro" data-acc="nuevaMetaMes" data-arg="${fecha.slice(0, 7)}">Crear una</button></p>`}`) +
    habitos +
    cierreDia(fecha, ag, campoAg) +
    (window.ascResumenDia ? ascResumenDia(fecha) : ""));
}

/* ---------- Comunidad ---------- */
const DEMO_POSTS = [
  ["demo-1", "Valentina R.", "LOGRO", "MAE", "¡Terminé mi primer curso de diseño! 30 días seguidos practicando 1 hora diaria.", "Dominar el diseño UX en 6 meses", 128, 3],
  ["demo-2", "Andrés M.", "VISION", "TRA", "Mi vision board 2027: una casa con jardín, un taller propio y tiempo para mi familia.", "", 96, 7],
  ["demo-3", "Lucía P.", "EVIDENCIA", "VOL", "Día 21 levantándome a las 5:00. La disciplina empieza a sentirse natural.", "Crear el hábito de madrugar", 74, 12],
  ["demo-4", "Mateo G.", "META", "VAL", "Mi meta del año: facturar el doble con mi emprendimiento sin trabajar más horas.", "Duplicar ingresos del emprendimiento", 51, 20],
  ["demo-5", "Sofía L.", "REFLEXION", "VOZ", "Hoy dije lo que pensaba en una reunión importante. Me temblaba la voz, pero lo hice.", "", 63, 26],
  ["demo-6", "Camilo T.", "LOGRO", "EVO", "Corrí mi primera media maratón. Hace un año no podía correr 2 km.", "Correr una media maratón", 142, 40],
].map(([id, n, tipo, eje, t, meta, imp, h]) => ({ id, autorId: id, autorNombre: n, autorUsuario: "", tipo, eje, texto: t, metaTitulo: meta, impulsos: imp, comentarios: 2, yoImpulse: false, creadoEn: Date.now() - h * 3600000, foto: "", propio: false, demo: true }));
const demoComentarios = {};
const misPostsDemo = [];

/** Lo que reportaste o a quien bloqueaste deja de verse aquí al instante (el servidor también lo filtra). */
const listaLocal = k => { try { return new Set(JSON.parse(localStorage.getItem("rutacima-" + k) || "[]")); } catch { return new Set(); } };
function ocultarLocal(k, v) { const s = listaLocal(k); s.add(String(v)); try { localStorage.setItem("rutacima-" + k, JSON.stringify([...s])); } catch {} }
async function cargarFeed() {
  if (cargarFeed.activo) return; cargarFeed.activo = true;
  await null; // siempre después de dibujar la lista
  try {
    if (!Store.nube) {
      const todos = [...misPostsDemo.filter(p => p.visibilidad !== "PRIVADA"), ...DEMO_POSTS].sort((a, b) => b.creadoEn - a.creadoEn);
      estado.feed = estado.feedFiltro === "VISION" ? todos.filter(p => p.tipo === "VISION") : estado.feedFiltro === "SIGUIENDO" ? misPostsDemo : todos;
    } else {
      const sb = Store.nube.sb, yo = Store.nube.dueno;
      let q = sb.from("posts").select("*,autor:profiles!posts_user_id_fkey(username,nombre,avatar_url),impulsos:votes(count),comentarios:comments(count)").order("created_at", { ascending: false }).limit(40);
      if (estado.feedFiltro === "VISION") q = q.eq("tipo", "VISION");
      if (estado.feedFiltro === "SIGUIENDO") {
        const { data: f } = await sb.from("follows").select("followed_id").eq("follower_id", yo);
        const ids = (f || []).map(x => x.followed_id);
        if (!ids.length) { estado.feed = []; return; }
        q = q.in("user_id", ids);
      }
      const { data, error } = await q; if (error) throw error;
      const ids = data.map(p => p.id);
      const { data: v } = ids.length ? await sb.from("votes").select("post_id").eq("user_id", yo).in("post_id", ids) : { data: [] };
      const mios = new Set((v || []).map(x => x.post_id));
      try {
        const { data: ex } = await sb.rpc("expedicion", { p_anio: new Date().getFullYear() });
        estado.tramos = ex ? ex.reduce((r, x) => (r[Math.min(51, Math.max(0, x.semana))] += Number(x.puntos) || 0, r), new Array(52).fill(0)) : null;
      } catch { estado.tramos = null; }
      estado.feed = data.map(p => ({
        id: p.id, autorId: p.user_id, autorNombre: p.autor?.nombre || p.autor?.username || "Senderista", autorUsuario: p.autor?.username || "",
        tipo: p.tipo, eje: p.eje, texto: p.texto, foto: p.image_url, metaTitulo: p.meta_titulo, visibilidad: p.visibilidad,
        impulsos: p.impulsos?.[0]?.count || 0, comentarios: p.comentarios?.[0]?.count || 0, yoImpulse: mios.has(p.id),
        brujula: p.brujula || null, creadoEn: Date.parse(p.created_at), propio: p.user_id === yo,
      }));
    }
  } catch (e) { console.error(e); estado.feed = []; toast("No se pudo cargar la comunidad." + detalleError(e)); }
  finally {
    const ocultos = listaLocal("ocultos"), bloqueados = listaLocal("bloqueados");
    if (estado.feed) estado.feed = estado.feed.filter(p => !ocultos.has(String(p.id)) && !bloqueados.has(String(p.autorId)));
    cargarFeed.activo = false; if (estado.sec === "comunidad" || estado.sec === "perfil") { pintarLista(); if (!enEdicion()) pintarDetalle(); } }
}
function postal(p, mini = false) {
  const tipo = TIPOS_POST.find(t => t[0] === p.tipo)?.[1] || "";
  if (p.foto && !mini) return `<img class="foto" src="${esc(p.foto)}" alt="${altFoto(p)}">`;
  // Sin foto: la cordillera de esa publicación (siempre la misma para el mismo post) con la frase encima
  return `<div class="postal">${window.Arte ? Arte.cordillera(String(p.id), null, { detalle: !mini }) : ""}<span class="tipo">${tipo}</span>
    <div class="q">${mini ? esc(p.texto) : "“" + esc(p.texto) + "”"}</div>${!mini && p.metaTitulo ? `<div class="m">${ic("bandera", "i fijado")} ${esc(p.metaTitulo)}${p.eje ? " · " + NOMBRE_EJE[p.eje] : ""}</div>` : ""}</div>`;
}
Object.assign(DET, {
  "comunidad.post"(id) {
    const p = [...(estado.feed || []), ...(estado.misPosts || []), ...(estado.misRecientes || [])].find(x => String(x.id) === id) || (String(estado.post?.id) === id ? estado.post : null);
    if (!p) return vacio();
    estado.post = p;
    if (!p.comentariosLista) cargarComentarios(p);
    const ini = (p.autorNombre || "?").split(" ").map(x => x[0]).join("").slice(0, 2).toUpperCase();
    return cab(av(esc(ini), COLOR_EJE[p.eje] || "var(--burdeos)"), esc(p.autorNombre), `${p.autorUsuario ? "@" + esc(p.autorUsuario) + " · " : ""}${hace(p.creadoEn)}`,
      p.propio ? `<button class="btn mini peligro" data-acc="borrarPost" data-arg="${esc(p.id)}">${ic("borrar")} Borrar</button>`
        : `<span style="display:flex;gap:6px;flex-wrap:wrap;justify-content:flex-end">${!p.demo || !Store.nube ? `<button class="btn mini" data-acc="escribirA" data-arg="${esc(p.autorId)}|${esc(p.autorNombre)}">${ic("mensajes")} Mensaje</button>` : ""}${!p.demo && Store.nube ? `<button class="btn mini" data-acc="seguir" data-arg="${esc(p.autorId)}">Seguir</button>` : ""}<button class="btn mini" data-acc="reportarPost" data-arg="${esc(p.id)}" title="Reportar publicación">Reportar</button><button class="btn mini peligro" data-acc="bloquearAutor" data-arg="${esc(p.id)}" title="Bloquear a esta persona">Bloquear</button></span>`) +
      `<div class="det-cuerpo"><div class="ancho" style="gap:16px;max-width:640px">
        ${p.demo ? `<div class="aviso">Comunidad de ejemplo. ${Store.nube ? "" : "Vincula la web con tu cuenta para ver la comunidad real."}</div>` : ""}
        ${postal(p)}${p.foto ? `<p style="margin:0">${esc(p.texto)}</p>` : ""}
        <div class="acciones"><button class="btn ${p.yoImpulse ? "lleno" : "oro"}" data-acc="impulsar" aria-pressed="${p.yoImpulse}">${ic("impulso")} ${p.yoImpulse ? "Impulsado" : "Impulsar"} · ${p.impulsos}</button>
          <span class="suave">${(p.comentariosLista || []).length || p.comentarios} comentarios</span></div>
        <div class="chat" style="max-width:none">${(p.comentariosLista || []).map(c => `<div class="burbuja el"><b style="color:var(--oro);font-size:13px">${esc(c.autor)}</b><br>${esc(c.texto)}</div>`).join("")}</div>
      </div></div>
      <form class="escribir" data-form="comentar"><input name="texto" placeholder="Escribe un comentario" aria-label="Comentario" autocomplete="off"><button class="enviar" aria-label="Enviar">${ic("enviar")}</button></form>`;
  },
  /** Una publicación de la lista abre Cimas desde ella (en el teléfono y en el computador). */
  "comunidad.cima"(id) {
    estado.vistaCom = "cimas";
    requestAnimationFrame(() => document.querySelector(`.cima[data-cima="${CSS.escape(id)}"]`)?.scrollIntoView({ block: "start" }));
    return DET["comunidad.feed"]();
  },
  "comunidad.feed"() {
    const p0Demo = ps => ps.some(p => p.demo) && !Store.nube ? `<p class="cimas-demo">Comunidad de ejemplo</p>` : "";
    if (!estado.feed) { cargarFeed(); return cab(av(ic("comunidad"), "var(--burdeos)"), "Comunidad", "Cargando…") + cuerpo(""); }
    const ps = estado.feed;
    const autor = p => { const ini = (p.autorNombre || "?").split(" ").map(x => x[0]).join("").slice(0, 2).toUpperCase(); return av(esc(ini), COLOR_EJE[p.eje] || "var(--burdeos)"); };
    const vacioFeed = `<p class="vacio-mini">${estado.feedFiltro === "SIGUIENDO" ? "Todavía no sigues a nadie." : "Aún no hay publicaciones."}</p>`;
    if (estado.vistaCom === "cimas") {
      // Cimas: la comunidad a pantalla completa, como los videos cortos, en el mundo de Ruta a la Cima.
      // Arriba: el muro y las pestañas. Derecha: autor y acciones. Abajo: autor, texto y meta.
      const pestanas = [["SIGUIENDO", "Siguiendo"], ["PARA_TI", "Para ti"], ["VISION", "Visión"]];
      const num = n => n >= 10000 ? `${Math.floor(n / 1000)} mil` : n >= 1000 ? `${(n / 1000).toFixed(1).replace(".", ",")} mil` : String(n);
      return `<div class="cimas-cab">
          <button class="cimas-ic" data-vista="lista" aria-label="Ver el muro" title="Ver el muro">${ic("lista")}</button>
          <nav class="cimas-pestanas" aria-label="Qué ver">${pestanas.map(([k, n]) => `<button data-feed="${k}" aria-pressed="${estado.feedFiltro === k}">${n}</button>`).join("")}</nav>
          <button class="cimas-ic" data-sec="mensajes" aria-label="Mensajes" title="Mensajes">${ic("mensajes")}</button></div>
        ${p0Demo(ps)}
        <div class="cimas" tabindex="0" aria-label="Cimas: desliza para ver la siguiente">${ps.length ? ps.map((p, i) => `<article class="cima" data-cima="${esc(p.id)}" aria-label="Publicación de ${esc(p.autorNombre)}">
          <div class="cima-fondo">${p.foto ? `<img src="${esc(p.foto)}" alt="${altFoto(p)}" loading="lazy">`
            : `<div class="cielo cima-cielo" data-cielo-post="${p.creadoEn}" aria-hidden="true"></div><p class="cima-cita">“${esc(p.texto)}”</p>`}</div>
          <div class="cima-acciones">
            <button class="cima-autor" data-acc="escribirA" data-arg="${esc(p.autorId)}|${esc(p.autorNombre)}" aria-label="Escribir a ${esc(p.autorNombre)}">${autor(p)}</button>
            <button data-acc="impulsarId" data-arg="${esc(p.id)}" aria-pressed="${p.yoImpulse}" aria-label="Impulsar, ${p.impulsos}">${ic("impulso")}<b>${num(p.impulsos)}</b></button>
            <button data-acc="comentariosCima" data-arg="${esc(p.id)}" aria-label="Comentarios, ${p.comentarios}">${ic("comentario")}<b>${num(p.comentarios)}</b></button>
            <button data-acc="compartirPost" data-arg="${esc(p.id)}" aria-label="Compartir">${ic("compartir")}</button>
            <button data-acc="masCima" data-arg="${esc(p.id)}" aria-label="Más opciones">${ic("masH")}</button></div>
          <div class="cima-pie"><b class="cima-quien">${p.autorUsuario ? "@" + esc(p.autorUsuario) : esc(p.autorNombre)}</b>
            <span class="cima-dato">${[TIPOS_POST.find(t => t[0] === p.tipo)?.[1], NOMBRE_EJE[p.eje], hace(p.creadoEn)].filter(Boolean).join(" · ")}</span>
            ${p.foto && p.texto ? `<p class="cima-texto">${esc(p.texto)}</p>${p.texto.length > 90 ? `<button class="cima-mas" data-acc="masTexto" aria-expanded="false">más</button>` : ""}` : p.foto ? "" : `<span class="cima-dato suave-c">Publicado bajo este cielo</span>`}
            ${p.metaTitulo ? `<span class="cima-meta">${ic("ruta")}${esc(p.metaTitulo)}</span>` : ""}
            ${i === 0 ? `<span class="cima-ayuda">${ic("arriba")}Desliza hacia arriba para ver la siguiente cima</span>` : ""}</div></article>`).join("") +
          // El feed tiene final: la invitación es volver a la propia ruta
          `<article class="cima cima-fin" aria-label="Final del feed"><div class="cima-fondo">${window.Arte ? Arte.cordillera("fin-del-feed", 3) : ""}</div>
            <div class="cima-fin-texto">${marca()}<h2>Ya viste lo nuevo</h2><p>La montaña sigue ahí mañana. Ahora, da tu propio paso de hoy.</p>
            <button class="btn lleno" data-ir="hoy" data-arg="d:${iso(hoyFecha())}">Dar el paso de hoy</button></div></article>`
          : `<div class="cimas-vacio"><p>${estado.feedFiltro === "SIGUIENDO" ? "Todavía no sigues a nadie." : "Aún no hay publicaciones."}</p><button class="btn lleno" data-ir="comunidad" data-arg="publicar">Publicar</button></div>`}</div>`;
    }
    return cab(av(ic("comunidad"), "var(--burdeos)"), "Comunidad", "Tu muro, hacia abajo") + cuerpo(
      (ps.length && estado.feedFiltro === "PARA_TI" ? cumbreComunidad(ps) : "") + (ps.length ? ps.map(p => `<article class="hoja post-muro">
        <div class="autor">${autor(p)}<div style="flex:1;min-width:0"><b>${esc(p.autorNombre)}</b><div class="suave" style="font-size:13px">${hace(p.creadoEn)} · ${TIPOS_POST.find(t => t[0] === p.tipo)?.[1] || ""}</div></div></div>
        ${p.brujula && window.leerBrujula?.(p.brujula) ? brujulaMuro(p)
          : `<button data-acc="verCima" data-arg="${esc(p.id)}" aria-label="Ver en Cimas, a pantalla completa" style="display:block;width:100%;text-align:left">${postal(p)}</button>
        ${p.foto && p.texto ? `<p style="margin:0">${esc(p.texto)}</p>` : ""}`}
        <div class="acciones"><button class="btn ${p.yoImpulse ? "lleno" : "oro"}" data-acc="impulsarId" data-arg="${esc(p.id)}" aria-pressed="${p.yoImpulse}">${ic("impulso")} ${p.impulsos}</button>
          <button class="btn" data-ir="comunidad" data-arg="post:${esc(p.id)}">${ic("comentario")} ${p.comentarios} comentarios</button></div></article>`).join("") : vacioFeed),
      "max-width:600px");
  },
  "comunidad.publicar"() {
    return cab(av(ic("publicar"), "var(--burdeos)"), "Publicar", "Planta tu bandera: comparte un paso de tu ascenso") + cuerpo(
      hoja(`<form class="form" data-form="publicar">
        ${campo("¿Qué quieres compartir?", `<textarea name="texto" required maxlength="2200" placeholder="Hoy logré…">${esc(estado.borradorPost || "")}</textarea>`)}${(estado.borradorPost = "", "")}
        <div class="form-2">${campo("Tipo", `<select name="tipo">${TIPOS_POST.map(([v, n]) => `<option value="${v}">${n}</option>`).join("")}</select>`)}
          ${campo("Eje", `<select name="eje">${opcionesEje.map(([v, n]) => `<option value="${v}">${n}</option>`).join("")}</select>`)}
          ${campo("Quién lo ve", `<select name="visibilidad">${VISIBILIDAD.map(([v, n]) => `<option value="${v}">${n}</option>`).join("")}</select>`)}</div>
        ${campo("Meta relacionada", `<select name="meta"><option value="">Ninguna</option>${Store.lista("meta_anio").map(m => `<option>${esc(m.titulo)}</option>`).join("")}</select>`)}
        ${Store.nube ? campo("Foto (opcional)", `<input type="file" name="foto" accept="image/*">`) : ""}
        <div class="botones"><button class="btn lleno" type="submit">Publicar</button></div></form>`), "max-width:640px");
  },
});
async function cargarComentarios(p) {
  await null;
  if (p.demo || !Store.nube) { p.comentariosLista = [{ autor: "Diana", texto: "¡Qué inspiración! Gracias por compartirlo." }, { autor: "Julián", texto: "Vamos, sigue así. Nos vemos en la cima." }, ...(demoComentarios[p.id] || [])]; }
  else {
    const { data } = await Store.nube.sb.from("comments").select("id,texto,created_at,autor:profiles!comments_user_id_fkey(username,nombre)").eq("post_id", p.id).order("created_at");
    p.comentariosLista = (data || []).map(c => ({ autor: c.autor?.nombre || c.autor?.username || "", texto: c.texto }));
  }
  if (estado.sel === "post:" + p.id && !enEdicion()) pintarDetalle();
}

/* ---------- Aprende: las 24 guías, con las respuestas de tu teléfono ---------- */
const Contenido = {
  indice: null, frases: null, guias: {},
  async json(ruta) { const r = await fetch(CONFIG.contenido + ruta); if (!r.ok) throw new Error(ruta); return r.json(); },
  async cargarIndice() {
    if (this._i) return; this._i = true;
    try { this.indice = await this.json("content/index.json"); } catch { this.indice = window.RUTACIMA_INDICE || []; }
    if (estado.sec === "aprende") { pintarLista(); pintarDetalle(); }
  },
  async cargarFrases() {
    if (this._f) return; this._f = true;
    try { this.frases = await this.json("frases/es.json"); } catch { this.frases = window.RUTACIMA_FRASES || Array.from({ length: 365 }, () => ({ t: "Tu cumbre te espera.", libro: "Ruta a la Cima" })); }
    refrescar();
  },
  async guia(id) {
    if (this.guias[id] || this["_g" + id]) return this.guias[id];
    this["_g" + id] = true;
    try { this.guias[id] = await this.json(`content/${id}.json`); } catch { this.guias[id] = { id, title: id, sections: [] }; }
    if (estado.sel === "wb:" + id) pintarDetalle();
  },
};
const respuesta = clave => Store.get("respuesta", clave)?.valor || "";
function responder(wb, clave, valor) {
  if (!valor) return Store.borrar("respuesta", clave);
  return Store.guardar("respuesta", { clave, workbookId: wb, valor: String(valor), actualizadoEn: Date.now() }, clave);
}
function bloque(b, wb) {
  const md = t => esc(t).replace(/\*\*(.+?)\*\*/g, "<b>$1</b>");
  switch (b.type) {
    case "heading": { const n = Math.min(4, Math.max(2, b.level || 2) + 1); return `<h${n}>${md(b.text)}</h${n}>`; }
    case "paragraph": return `<p>${md(b.text)}</p>`;
    case "bullet": return `<p style="padding-left:18px;text-indent:-14px">${esc(b.mark || "•")} ${md(b.text)}</p>`;
    case "quote": return `<blockquote>${md(b.text)}</blockquote>`;
    case "callout": return `<div class="callout">${(b.blocks || []).map(x => bloque(x, wb)).join("")}</div>`;
    case "image": return "";
    case "table": return `<div class="tabla"><table>${b.headers?.length ? `<tr>${b.headers.map(x => `<th>${md(x)}</th>`).join("")}</tr>` : ""}${(b.rows || []).map(r => `<tr>${r.map(x => `<td>${md(x)}</td>`).join("")}</tr>`).join("")}</table></div>`;
    case "prompt": return `<div class="bloque-in">${b.title ? `<b>${b.number ? esc(b.number) + " " : ""}${md(b.title)}</b>` : ""}${b.label ? `<span class="suave">${md(b.label)}</span>` : ""}
      <textarea class="entrada" data-resp="${esc(wb)}|${esc(b.id)}" rows="3" placeholder="Escribe aquí">${esc(respuesta(b.id))}</textarea></div>`;
    case "scale": { const v = Number(respuesta(b.id)) || 0; let bs = ""; for (let i = b.min ?? 1; i <= (b.max ?? 10); i++) bs += `<button class="${v === i ? "si" : ""}" data-acc="escala" data-arg="${esc(wb)}|${esc(b.id)}|${i}" aria-pressed="${v === i}">${i}</button>`;
      return `<div class="bloque-in"><span>${md(b.label)}</span><div class="escala">${bs}</div></div>`; }
    case "check": return `<label class="check"><input type="checkbox" data-acc-change="checkResp" data-arg="${esc(wb)}|${esc(b.id)}" ${respuesta(b.id) ? "checked" : ""}><span>${md(b.label)}</span></label>`;
    case "choice": { const v = respuesta(b.id); return `<div class="bloque-in">${b.label ? `<span>${md(b.label)}</span>` : ""}<div class="opciones">${(b.options || []).map(o => `<button class="${v === o ? "si" : ""}" data-acc="opcion" data-arg="${esc(wb)}|${esc(b.id)}|${esc(o)}">${esc(o)}</button>`).join("")}</div></div>`; }
    case "inputTable": {
      const etiquetas = b.rowLabels || [], con = etiquetas.length > 0;
      const cols = con ? (b.headers || []).slice(1) : (b.headers || []);
      const filas = con ? etiquetas.length : (Number(respuesta(b.id + "#n")) || b.rows || 3);
      let h = `<div class="tabla"><table>${b.headers?.length ? `<tr>${b.headers.map(x => `<th>${md(x)}</th>`).join("")}</tr>` : ""}`;
      for (let r = 0; r < filas; r++) {
        h += `<tr>${con ? `<th>${md(etiquetas[r])}</th>` : ""}${cols.map((_, c) => `<td><input data-resp="${esc(wb)}|${esc(b.id)}#${r}_${c}" value="${esc(respuesta(`${b.id}#${r}_${c}`))}" aria-label="${esc(cols[c] || "")}"></td>`).join("")}</tr>`;
      }
      return h + `</table></div>${con ? "" : `<button class="btn mini" data-acc="filaMas" data-arg="${esc(wb)}|${esc(b.id)}|${filas + 1}">${ic("mas")} Fila</button>`}`;
    }
    default: return "";
  }
}
Object.assign(DET, {
  "aprende.wb"(id) {
    const g = Contenido.guias[id];
    const resumen = (Contenido.indice || []).find(x => x.id === id) || { title: id, subtitle: "" };
    Audio_.cargar();
    if (!g) { Contenido.guia(id); return cab(av(esc(resumen.title[0]), "var(--burdeos)"), esc(resumen.title), "Cargando…") + cuerpo(""); }
    const secs = g.sections || [];
    const guardada = Number(respuesta(`${id}#ultima`)) || 0;
    if (estado.wb[id] === undefined) estado.wb[id] = Math.min(guardada, Math.max(0, secs.length - 1));
    const i = estado.wb[id], s = secs[i];
    return cab(av(esc(g.title.trim()[0]), g.category === "bonos" ? "var(--oro)" : "var(--burdeos)"), esc(g.title), esc(g.subtitle || "")) + cuerpo(
      `<div class="arte-cabecera" aria-hidden="true">${window.Arte ? Arte.cordillera(id, null, { bandera: g.category !== "bonos" }) : ""}</div>` +
      `<div class="tabs">${secs.map((x, k) => `<button data-acc="seccion" data-arg="${esc(id)}|${k}" aria-pressed="${k === i}">${esc(x.title || `Sección ${k + 1}`)}</button>`).join("")}</div>` +
      (s ? `<div id="audioBarra" data-cap="${esc(id)}|${i}">${htmlAudioBarra(id, i)}</div>` : "") +
      (s ? hoja(`<div class="wb">${s.title ? `<h2 style="margin-top:0">${esc(s.title)}</h2>` : ""}${(s.blocks || []).map(b => bloque(b, id)).join("")}</div>`) : `<p class="vacio-mini">Esta guía no se pudo cargar.</p>`) +
      `<div class="botones" style="justify-content:space-between"><button class="btn" data-acc="seccion" data-arg="${esc(id)}|${i - 1}" ${i <= 0 ? "disabled" : ""}>Anterior</button>
        <span class="suave">${i + 1} de ${secs.length}</span>
        <button class="btn lleno" data-acc="seccion" data-arg="${esc(id)}|${i + 1}" ${i >= secs.length - 1 ? "disabled" : ""}>Siguiente</button></div>`, "max-width:860px");
  },
});

/* ---------- Coach ---------- */
const mensajesCoach = () => Store.lista("coach").sort((a, b) => a.creadoEn - b.creadoEn || a.id - b.id);
let coachEscribiendo = false;
function contextoCoach() {
  const p = perfil(), e = ultimaEvaluacion(), c = cascada(), anio = new Date().getFullYear(), hoy = hoyFecha();
  const l = [];
  if (p.nombre) l.push(`Nombre: ${p.nombre}`);
  if (p.cumbreFrase) l.push(`Cumbre personal: ${p.cumbreFrase}`);
  const fase = FASES.findIndex(f => f[0] === p.faseActual); if (fase >= 0) l.push(`Fase actual: ${fase + 1}. ${FASES[fase][1]}`);
  if (e) l.push("Ejes (1-10): " + EJES.map(([, n, k]) => `${n} ${e[k]}`).join(", "));
  if (c.props.length) l.push("Propósitos a 5 años: " + c.props.map(x => `${x.titulo} (${Math.round(c.nodoProp(x) * 100)}%)`).join("; "));
  const an = c.anuales.filter(x => x.anio === anio); if (an.length) l.push(`Metas ${anio}: ` + an.map(x => `${x.titulo} (${Math.round(c.nodoAnio(x) * 100)}%)`).join("; "));
  const me = metasDelMes(anio, hoy.getMonth() + 1); if (me.length) l.push("Metas de este mes: " + me.map(x => `${x.texto} (${Math.round(avanceMetaMes(x) * 100)}%)`).join("; "));
  return l.join("\n");
}
function respuestaGuia(t) {
  t = t.toLowerCase(); const tiene = (...p) => p.some(x => t.includes(x));
  if (tiene("motiv", "ganas", "cansad", "fuego")) return "La motivación va y viene; los sistemas se quedan. Hoy no necesitas ganas, necesitas un paso de 2 minutos. Elige la acción más pequeña de tu meta del mes y hazla ahora. Luego márcala en Hoy: ver el avance enciende de nuevo el fuego.";
  if (tiene("rendir", "abandon", "dejar")) return "Antes de abandonar, abre el bono Anti-abandono: casi siempre no hay que dejar la montaña, sino cambiar la ruta. Pregúntate: ¿esta meta todavía me acerca a mi cumbre? Si la respuesta es sí, reduce el tamaño del paso, no la meta.";
  if (tiene("perdid", "confund", "no sé", "no se")) return "Perderse en la niebla es parte del viaje. Abre el Kit de Emergencia y elige el protocolo que describe cómo te sientes. Después vuelve a tu cumbre en una frase y decide solo el próximo paso.";
  const e = ultimaEvaluacion();
  if (!e) return "Para orientarte mejor, evalúa tus 6 ejes (Perfil → Mis ejes). Con eso sabré qué parte de tu montaña necesita más atención.";
  const [cod, nom, k] = EJES.reduce((m, x) => (e[x[2]] < e[m[2]] ? x : m));
  return `Tu eje más débil hoy es ${nom} (${e[k]}/10). Elige una acción pequeña para fortalecerlo esta semana y conviértela en una meta del mes en Metas → Nueva meta.`;
}
let coachLimite = false;
async function preguntar(texto) {
  if (estado.sec !== "coach") { estado.sec = "coach"; pintarRiel(); }
  estado.sel = "coach"; $("app").classList.add("con-detalle");
  await Store.guardar("coach", { id: nuevoId(), rol: "user", texto, creadoEn: Date.now() });
  coachEscribiendo = true; pintarLista(); pintarDetalle();
  let r = null;
  if (Store.nube) {
    try {
      const hist = mensajesCoach().slice(-16).map(m => ({ role: m.rol, content: m.texto }));
      const { data, error } = await Store.nube.sb.functions.invoke("coach", { body: { contexto: contextoCoach(), idioma: "es", mensajes: hist } });
      if (error) throw error;
      r = data?.texto || null;
      // Límite diario del plan gratuito: se ofrece conocer el Plan Cumbre
      coachLimite = !!data?.limite && !data?.cumbre;
    } catch (e) { console.warn("coach sin servidor", e); }
  } else await new Promise(ok => setTimeout(ok, 700));
  coachEscribiendo = false;
  await Store.guardar("coach", { id: nuevoId(), rol: "assistant", texto: r || respuestaGuia(texto), creadoEn: Date.now() });
}
DET.coach = () => {
  const ms = mensajesCoach();
  const sugs = ["Ayúdame a definir mi cumbre", "Divide mi meta anual en pasos", "Perdí la motivación", "Planea mi semana", "¿Qué eje debo trabajar?"];
  let ultimoDia = "";
  return cab(av(ic("coach"), "var(--burdeos)"), "Coach SuRoute", coachEscribiendo ? "escribiendo…" : "Conoce tus metas, tus ejes y tu avance",
    ms.length ? `<button class="btn mini" data-acc="borrarCoach">Nueva conversación</button>` : "") +
    `<div class="det-cuerpo abajo"><div class="chat">
      <div class="burbuja el">¡Hola! Soy tu coach de SuRoute. Conozco tus metas, tus ejes y tu avance. Cuéntame qué quieres lograr o en qué te sientes atascado y te ayudo a dar el siguiente paso.</div>
      ${ms.map(m => { const d = new Date(m.creadoEn).toLocaleDateString("es", { day: "numeric", month: "long" }); const sep = d !== ultimoDia ? `<span class="fecha-sep">${d}</span>` : ""; ultimoDia = d;
        if (m.rol === "user") return sep + `<div class="burbuja yo">${esc(m.texto)}<span class="h">${horaDe(m.creadoEn)}</span></div>`;
        const [texto, accion] = separarAccion(m.texto), creada = listaLocal("coach-creadas").has(String(m.id));
        return sep + `<div class="burbuja el">${esc(texto)}<span class="h">${horaDe(m.creadoEn)}</span></div>` + (accion ? `<div class="accion-coach">
          <div class="etiqueta">${accion.tipo === "meta_mes" ? "Meta del mes propuesta" : "Meta del año propuesta"}</div><p>${esc(accion.texto)}</p>
          ${creada ? `<span class="suave">✓ Meta creada</span>` : `<button class="btn mini lleno" data-acc="crearMetaCoach" data-arg="${esc(String(m.id))}">Crear esta meta</button>`}</div>` : ""); }).join("")}
      ${coachEscribiendo ? `<div class="burbuja el escribiendo">escribiendo…</div>` : ""}
      ${coachLimite ? `<button class="btn mini oro coach-plan" data-ir="perfil" data-arg="plan">${ic("cima")} ¿Quieres conversar más? Conoce el Plan Cumbre</button>` : ""}</div>
      ${ms.length < 2 ? `<div class="sugerencias">${sugs.map(s => `<button data-pregunta="${esc(s)}">${esc(s)}</button>`).join("")}</div>` : ""}</div>
    <form class="escribir" data-form="coach"><input name="texto" placeholder="Escribe un mensaje" aria-label="Mensaje" autocomplete="off"><button class="enviar" aria-label="Enviar">${ic("enviar")}</button></form>`;
};

/** La meta que propone el coach al final de su respuesta (igual que AccionCoach.kt). */
function separarAccion(t) {
  const m = /\[\[ACCION([\s\S]*?)]]/.exec(t || "");
  if (!m) return [String(t || "").trim(), null];
  const limpio = t.replace(/\[\[ACCION[\s\S]*?]]/g, "").trim();
  try {
    const o = JSON.parse(m[1].slice(m[1].indexOf("{"), m[1].lastIndexOf("}") + 1));
    if (o.tipo !== "meta_mes" && o.tipo !== "meta_anio") return [limpio, null];
    const texto = String(o.texto || o.titulo || "").trim().slice(0, 300); if (!texto) return [limpio, null];
    const eje = String(o.eje || "").toUpperCase();
    return [limpio, { tipo: o.tipo, texto, eje: COLOR_EJE[eje] ? eje : null, dias: Math.min(31, Math.max(1, Number(o.dias) || 20)) }];
  } catch { return [limpio, null]; }
}
/* ---------- Mis frases ---------- */
DET["frases.f"] = fecha => {
  if (!Contenido.frases) { Contenido.cargarFrases(); return vacio(); }
  const d = deIso(fecha), hoy = hoyFecha(), esHoy = d.getTime() === hoy.getTime();
  const abierta = abiertas(d.getFullYear()).has(fraseIndice(d)), fr = Contenido.frases[fraseIndice(d)];
  return cab(avSello(), esHoy ? "Frase de hoy" : `Día ${diaDelAnio(d) + 1} del año`, fechaLarga(d)) + cuerpo(
    `<div class="hoja sellada ${abierta ? "rota" : ""}" id="tarjetaFrase"><div class="etiqueta">Frase del día</div><img class="lacre" src="${LOGO}" alt="Sello de cera">
      <div id="contenidoFrase">${abierta ? `<p class="cita aparece">“${esc(fr.t)}”</p><p class="cita-libro aparece">${esc(fr.libro)}</p>`
        : esHoy ? `<p class="suave" style="margin:18px auto 16px;max-width:36ch">Cada día te espera una frase de los libros, sellada con cera. Este computador está vinculado a tu cuenta, así que el sello se rompe con un toque.</p><button class="btn lleno" data-acc="romper" data-arg="${fecha}">${ic("candado")} Romper el sello</button>`
        : `<p class="suave" style="margin-top:18px">Este sello no se rompió ese día. Las frases solo se abren el día que les toca.</p>`}</div></div>`, "max-width:640px");
};

/* ---------- Perfil ---------- */
const ultimaEvaluacion = () => Store.lista("ejes").sort((a, b) => b.fecha - a.fecha)[0] || null;
Object.assign(DET, {
  "perfil.perfil"() {
    const p = perfil(), r = recordatorio(), anioHoy = new Date().getFullYear();
    return cab(av(ic("perfil"), "var(--burdeos)"), "Mi perfil", "Quién soy, mis fotos y mis datos") + cuerpo(
      (window.cabeceraVida ? cabeceraVida() : "") + (window.quienSoy ? quienSoy() : "") +
      hoja(`<h3 style="margin:0 0 12px">Mis datos</h3><div class="form">${campo("Tu nombre", txt("perfil", 1, "nombre", p.nombre))}
        ${Store.nube?.perfil ? campo("Tu usuario (lo ve la comunidad)", `<div class="con-prefijo"><b>@</b><input name="usuarioPerfil" autocomplete="off" spellcheck="false" maxlength="30" value="${esc(Store.nube.perfil.username || "")}" data-acc-change="cambiarUsuario"></div>`) : ""}
        ${campo("Mi cumbre es… (en una frase)", area("perfil", 1, "cumbreFrase", p.cumbreFrase))}
        <div class="form-2">${campo("Año de nacimiento", `<input type="number" min="1900" max="${anioHoy}" ${bind("perfil", 1, "anioNacimiento", "numNulo")} value="${p.anioNacimiento ?? ""}">`)}
          ${campo("Mes de nacimiento", sel("perfil", 1, "mesNacimiento", p.mesNacimiento ?? "", [["", "—"], ...MESES.map((m, i) => [i + 1, mayus(m)])], "numNulo"))}
          ${campo("Fase actual", sel("perfil", 1, "faseActual", p.faseActual || "", [["", "—"], ...FASES], "nulo"))}
          ${campo("Año de inicio del plan", `<input type="number" min="2000" max="2100" ${bind("perfil", 1, "anioInicioPlan", "num")} value="${p.anioInicioPlan || anioHoy}">`)}</div>
        ${campo(`Meta de vida: ${metaVida()} años`, `<div class="rango"><input type="range" min="60" max="120" value="${metaVida()}" ${bind("perfil", 1, "esperanzaVida", "num")}><b>${metaVida()}</b></div>`)}</div>`), "max-width:720px");
  },
  "perfil.ejes"() {
    const e = ultimaEvaluacion(), hist = Store.lista("ejes").sort((a, b) => b.fecha - a.fecha);
    const val = estado.evalNueva ||= Object.fromEntries(EJES.map(([, , k]) => [k, e ? e[k] : 5]));
    return cab(av(ic("comunidad"), "var(--burdeos)"), "Mis ejes", e ? `Última evaluación: ${new Date(e.fecha).toLocaleDateString("es")}` : "Evalúa tus 6 ejes") + cuerpo(
      (e ? hoja(`<h3>Así están tus ejes</h3><div class="ejes">${EJES.map(([c, n, k]) => `<div class="eje"><span>${n}</span><div class="barra"><i style="width:${e[k] * 10}%;background:${COLOR_EJE[c]}"></i></div><b>${e[k]}</b></div>`).join("")}</div>`) : "") +
      hoja(`<h3>Nueva evaluación</h3><p class="suave" style="margin-top:0">Del 1 al 10, ¿cómo está hoy cada eje de tu cumbre?</p><div class="form">
        ${EJES.map(([c, n, k]) => campo(n, `<div class="rango"><input type="range" min="1" max="10" value="${val[k]}" data-acc-change="evalEje" data-arg="${k}" data-acc-input="evalEje"><b>${val[k]}</b></div>`)).join("")}
        <div class="botones"><button class="btn lleno" data-acc="guardarEval">Guardar evaluación</button></div></div>`) +
      (hist.length > 1 ? hoja(`<h3>Historial</h3>${hist.map(x => `<div class="meta-fila"><div class="arriba"><span>${new Date(x.fecha).toLocaleDateString("es")} · ${EJES.map(([, , k]) => x[k]).reduce((a, b) => a + b, 0)}/60</span><button class="btn mini" data-acc="borrar" data-arg="ejes|${x.id}">${ic("borrar")}</button></div></div>`).join("")}`) : ""), "max-width:720px");
  },
  "perfil.publicaciones"() {
    if (!estado.misPosts) cargarMisPosts();
    const ps = estado.misPosts || [];
    return cab(av(ic("foto"), "var(--burdeos)"), "Mis publicaciones", "Tu diario de vida", `<button class="btn mini oro" data-ir="comunidad" data-arg="publicar">${ic("mas")} Publicar</button>`) + cuerpo(
      ps.length ? `<div class="posts">${ps.map(p => `<button data-ir="comunidad" data-arg="post:${esc(p.id)}" style="text-align:left">${p.foto ? `<img class="foto" style="aspect-ratio:4/5" src="${esc(p.foto)}" alt="${altFoto(p)}">` : postal(p, true)}</button>`).join("")}</div>`
        : `<p class="vacio-mini">${estado.misPosts ? "Todavía no has publicado." : "Cargando…"}</p>`);
  },
  "perfil.apariencia"() {
    const a = apariencia();
    const papeles = [["blanco", "Blanco", "Limpio y luminoso"], ["antiguo", "Antiguo", "Papel envejecido, tinta café"], ["pastel", "Pastel", "Café con leche y caramelo"]];
    const temas = [["claro", "Claro"], ["oscuro", "Oscuro"], ["sistema", "Según el sistema"]];
    return cab(av(ic("tema"), "var(--burdeos)"), "Apariencia", "El papel y la luz de SuRoute en este navegador") + cuerpo(
      hoja(`<h3>Papel</h3><p class="suave" style="margin-top:0">El mismo estilo que eliges en la app (Perfil › Ajustes). Se usa con el tema claro.</p>
        <div class="papeles" role="radiogroup" aria-label="Papel">${papeles.map(([v, n, d]) =>
          `<button class="papel-op" role="radio" data-acc="papel" data-arg="${v}" aria-checked="${a.papel === v}"><i class="muestra muestra-${v}" aria-hidden="true"><b></b></i><span><b>${n}</b><small>${d}</small></span></button>`).join("")}</div>`) +
      hoja(`<h3>Tema</h3><div class="segmentos" style="grid-template-columns:repeat(3,1fr)" role="radiogroup" aria-label="Tema">${temas.map(([v, n]) =>
          `<button role="radio" data-acc="temaModo" data-arg="${v}" aria-checked="${a.tema === v}" aria-pressed="${a.tema === v}">${n}</button>`).join("")}</div>`), "max-width:640px");
  },
  "perfil.cuenta"() {
    return cab(av(ic("salir"), "#8A7B70"), Store.nube ? (Store.nube.directa ? "Mi cuenta" : "Este computador") : "Demostración", Store.nube ? (Store.nube.directa ? Store.nube.correo || "" : nombreNavegador()) : "Ruta de ejemplo") + cuerpo(
      hoja(Store.nube
        ? (Store.nube.directa
          ? `<h3>Conectado con tu correo</h3><p>Entraste como ${esc(Store.nube.correo || "")}${Store.nube.perfil?.username ? ` (@${esc(Store.nube.perfil.username)})` : ""}. Tu ruta se guarda en tu cuenta: ábrela desde cualquier navegador, en el computador o en el teléfono, entrando con tu correo.</p>
             <div class="botones"><button class="btn" data-acc="cambiarClaveWeb">Cambiar contraseña</button><button class="btn peligro" data-acc="salir">Cerrar sesión</button></div>`
          : `<h3>Conectado a tu cuenta</h3><p>SuRoute Web usa los mismos datos que tu teléfono${Store.nube.perfil?.username ? ` (@${esc(Store.nube.perfil.username)})` : ""}. Lo que cambies aquí aparece en la app, y lo que hagas en la app aparece aquí.</p>
           <p class="suave">Para desvincular este computador desde el teléfono: Perfil › SuRoute Web.</p><div class="botones"><button class="btn peligro" data-acc="salir">Cerrar sesión en este computador</button></div>`)
          + `<h3 style="margin-top:28px">Eliminar mi cuenta</h3><p class="suave">Se borran para siempre tu perfil, publicaciones, comentarios, mensajes, tu ruta en la nube y los computadores vinculados. Lo guardado en tu teléfono se queda ahí.</p>
           <div class="botones"><button class="btn peligro" data-acc="eliminarCuenta">Eliminar mi cuenta</button></div>`
        : `<h3>Estás en la demostración</h3><p>Es una ruta de ejemplo y los cambios se quedan en esta pestaña. Para usar tu ruta real, abre SuRoute Web con el servidor configurado y vincúlala desde la app: Perfil › SuRoute Web › Escanear código.</p>
           <div class="botones"><button class="btn" data-acc="salir">Volver a la pantalla de inicio</button></div>`) +
        (globalThis.avisoSuspension?.() || "") + enlacesLegales(), "max-width:640px");
  },
});
/** Documentos legales (Ley 1581 de 2012): se abren en otra pestaña. */
const DOCS_LEGALES = [["privacidad.html", "Privacidad"], ["terminos.html", "Términos"], ["aviso-privacidad.html", "Aviso de privacidad"],
  ["autorizacion.html", "Autorización de datos"], ["normas.html", "Normas de la comunidad"]];
function enlacesLegales() {
  // Fuera del sitio publicado (por ejemplo, en una vista previa) los documentos se abren desde GitHub Pages
  const base = /github\.io$|(^|\.)suroute\.com$|^localhost|^127\./.test(location.hostname) ? "" : "https://suroute.com/";
  return `<nav class="legales" aria-label="Documentos legales">${DOCS_LEGALES.map(([a, t]) => `<a href="${base}${a}" target="_blank" rel="noopener">${t}</a>`).join("")}</nav>`;
}
async function cargarMisPosts() {
  if (cargarMisPosts.activo) return; cargarMisPosts.activo = true;
  await null;
  try {
    if (!Store.nube) estado.misPosts = misPostsDemo;
    else {
      const { data, error } = await Store.nube.sb.from("posts").select("*").eq("user_id", Store.nube.dueno).order("created_at", { ascending: false }).limit(200);
      if (error) { console.error(error); estado.misPosts ||= []; toast("No se pudieron cargar tus publicaciones." + detalleError(error)); return; }
      estado.misPosts = (data || []).map(p => ({ id: p.id, anio: p.anio, autorId: p.user_id, autorNombre: perfil().nombre || "Yo", tipo: p.tipo, eje: p.eje, texto: p.texto, foto: p.image_url, metaTitulo: p.meta_titulo, visibilidad: p.visibilidad, impulsos: 0, comentarios: 0, creadoEn: Date.parse(p.created_at), propio: true }));
      // Lo recién publicado se ve aunque el servidor tarde en devolverlo
      const ids = new Set(estado.misPosts.map(p => p.id));
      estado.misPosts.unshift(...(estado.misRecientes || []).filter(p => !ids.has(p.id)));
    }
  } finally { cargarMisPosts.activo = false; if ((estado.sel === "publicaciones" || estado.sel === "vision" || estado.sel === "vida" || String(estado.sel).startsWith("a:")) && !enEdicion()) pintarDetalle(); }
}


/* ---------- Vision board: la IA lo arma con tus datos, tú lo llenas con tus fotos ---------- */
const VISION_MAX = 9;
const VISION_EJES = {
  VOL: ["Sé lo que me mueve y voy por ello cada día.", "Algo que te encienda: un lugar, una persona o un símbolo de tu propósito."],
  MAE: ["Practico cada día hasta dominar mi oficio.", "Tú en tu práctica: tus herramientas, tu espacio de trabajo o de estudio."],
  VOZ: ["Comparto mi mensaje con mi voz auténtica.", "Tú hablando, enseñando o mostrando tu obra a otros."],
  VAL: ["Vivo con coherencia de lo que sé hacer.", "Lo que tu trabajo hace posible: tu casa, tu negocio, tu libertad."],
  EVO: ["Cada mes soy mejor que el anterior.", "Algo que estás aprendiendo o un lugar al que quieres llegar."],
  TRA: ["Mi cumbre ayuda a otros a subir.", "Las personas que impactas: tu familia, tus alumnos, tu comunidad."],
};
const VACIAS = new Set("el la los las un una unos unas de del al a en y o con por para mi mis tu tus su sus que se lo es son como más the and of to my sin sobre hasta desde cada todo toda".split(" "));
const palabrasClave = t => [...new Set(String(t).toLowerCase().split(/[^\p{L}\p{N}]+/u).filter(w => w.length > 2 && !VACIAS.has(w) && isNaN(Number(w))))].slice(0, 4).join(" ");
const urlIdeas = b => "https://www.pexels.com/search/" + encodeURIComponent(b || "mountain summit") + "/";
const INSTRUCCION_VISION = `Arma mi vision board con lo que sabes de mí (mi cumbre, mis propósitos, mis metas y mis ejes).
Son exactamente ${VISION_MAX} casillas, una por cada campamento de mi Brújula de la Cima: CUMBRE (mi cumbre
personal), VOL, MAE, VOZ, VAL, EVO, TRA (mis 6 ejes), CON (Confluencia: un proyecto que activa varios
ejes) y CAM (Campamento Base: mentor, cordada y red de apoyo).
Responde SOLO con un arreglo JSON, sin texto antes ni después. Cada casilla:
{"campamento": "CUMBRE|VOL|MAE|VOZ|VAL|EVO|TRA|CON|CAM",
 "titulo": "rótulo corto, máx. 4 palabras",
 "afirmacion": "frase en presente y primera persona, máx. 14 palabras, sobre algo concreto de mi vida",
 "eje": "VOL|MAE|VOZ|VAL|EVO|TRA o null",
 "sugerencia": "qué foto MÍA buscar o tomar para esta casilla (no imágenes genéricas)",
 "busqueda": "2 a 4 palabras para buscar ideas de imágenes"}
Usa mis propósitos y metas en el campamento de su eje. Escribe en mi idioma.`;
/** Confluencia y Campamento Base (los dos campamentos que no son ejes). */
const VISION_CAMP = {
  CON: ["Confluencia", "Tengo un proyecto que une mis seis ejes en una sola montaña.", "Una foto tuya trabajando en el proyecto que mezcla lo que sabes, lo que dices y lo que aportas."],
  CAM: ["Campamento Base", "No subo solo: mi mentor, mi cordada y mi red me sostienen.", "Una foto con tu mentor, tu grupo o las personas que te acompañan a subir."],
};
const ORIGENES_VISION = () => ["cumbre", ...Mandala.CAMPAMENTOS_FIJOS.map(c => "campamento:" + c)];
/** Igual que VisionBoard.proponer en la app: la cumbre y los 8 campamentos fijos de la Brújula de la Cima. */
function proponerVision() {
  const p = perfil(), anio = new Date().getFullYear(), r = [];
  const cumbre = (p.cumbreFrase || "").trim();
  r.push({ titulo: "Mi cumbre", afirmacion: cumbre || "Escribe aquí tu cumbre personal", eje: null, sugerencia: "Una foto tuya en un lugar o momento que represente tu cumbre.", busqueda: palabrasClave(cumbre || "cumbre montaña"), origen: "cumbre" });
  const props = Store.lista("proposito"), metas = Store.lista("meta_anio").filter(x => x.anio === anio);
  Mandala.CAMPAMENTOS_FIJOS.forEach(cod => {
    const [nombre, af, foto] = VISION_CAMP[cod] || [NOMBRE_EJE[cod], VISION_EJES[cod][0], VISION_EJES[cod][1]];
    const propio = (cod === "CON" ? props.find(x => !x.eje)?.titulo : cod === "CAM" ? null
      : (props.find(x => x.eje === cod)?.titulo || metas.find(x => x.eje === cod)?.titulo))?.trim() || null;
    r.push({ titulo: nombre, afirmacion: propio || af, eje: VISION_CAMP[cod] ? null : cod, sugerencia: propio ? `${foto} Relacionada con: ${propio}` : foto,
      busqueda: palabrasClave(propio || foto), origen: "campamento:" + cod });
  });
  return r;
}
/** Igual que VisionBoard.desdeIa en la app. */
function visionDesdeIa(texto) {
  const i = texto.indexOf("["), f = texto.lastIndexOf("]");
  if (i < 0 || f <= i) return null;
  let arr; try { arr = JSON.parse(texto.slice(i, f + 1)); } catch { return null; }
  if (!Array.isArray(arr)) return null;
  const s = v => (typeof v === "string" && v.trim() && v.trim() !== "null") ? v.trim() : null;
  const r = arr.map(o => {
    if (!o || !s(o.afirmacion)) return null;
    const eje0 = s(o.eje)?.toUpperCase(), eje = NOMBRE_EJE[eje0] ? eje0 : null, camp = s(o.campamento)?.toUpperCase();
    const origen = camp === "CUMBRE" ? "cumbre" : Mandala.CAMPAMENTOS_FIJOS.includes(camp) ? "campamento:" + camp : eje ? "campamento:" + eje : "ia";
    return { titulo: (s(o.titulo) || s(o.afirmacion)).slice(0, 40), afirmacion: s(o.afirmacion).slice(0, 160), eje: eje || (NOMBRE_EJE[camp] ? camp : null),
      sugerencia: (s(o.sugerencia) || "").slice(0, 200), busqueda: (s(o.busqueda) || palabrasClave(o.afirmacion)).slice(0, 60), origen };
  }).filter(Boolean).slice(0, VISION_MAX);
  return r.length >= 3 ? r : null;
}
const casillasVision = () => Store.lista("vision").sort((a, b) => (a.orden || 0) - (b.orden || 0) || a.id - b.id);
function fotoDePublicacion(id) {
  if (!id) return null;
  const p = (estado.misPosts || []).find(x => String(x.id) === String(id));
  return p?.foto || null;
}
let armandoVision = false, visionConIa = null;
async function armarVision() {
  armandoVision = true; pintarDetalle();
  let propuestas = null;
  if (Store.nube) {
    try {
      const { data } = await Store.nube.sb.functions.invoke("coach", { body: { contexto: contextoCoach(), idioma: "es", mensajes: [{ role: "user", content: INSTRUCCION_VISION }] } });
      propuestas = data?.texto ? visionDesdeIa(data.texto) : null;
    } catch (e) { console.warn(e); }
  }
  visionConIa = !!propuestas;
  const local = proponerVision(), actuales = casillasVision();
  const { cumbre, camps } = Mandala.repartir(actuales), ubicadas = [cumbre, ...camps];
  // Las 9 casillas se actualizan en su lugar (conservan su id y sus pasos); las que tienen foto no cambian
  ORIGENES_VISION().forEach((origen, orden) => {
    const p = propuestas?.find(x => x.origen === origen) || local.find(x => x.origen === origen); if (!p) return;
    const ex = ubicadas[orden];
    if (!ex) Store.guardar("vision", { id: nuevoId(), orden, publicacionId: null, ...p });
    else if (ex.publicacionId) Store.cambiar("vision", ex.id, { origen });
    else Store.cambiar("vision", ex.id, { titulo: p.titulo, afirmacion: p.afirmacion, eje: p.eje, sugerencia: p.sugerencia, busqueda: p.busqueda, origen });
  });
  const usadas = new Set(ubicadas.filter(Boolean).map(c => c.id));
  actuales.filter(c => !usadas.has(c.id) && !c.publicacionId).forEach(c => Store.borrar("vision", c.id));
  await window.llenarCampamentosVacios?.();
  armandoVision = false; pintarDetalle();
}
async function ponerFotoVision(id, archivo) {
  const c = Store.get("vision", id); if (!c || !archivo) return;
  toast("Subiendo tu foto…");
  try {
    let postId;
    if (Store.nube) {
      const sb = Store.nube.sb; postId = crypto.randomUUID();
      const ruta = `${Store.nube.dueno}/${postId}.jpg`;
      const up = await sb.storage.from("media").upload(ruta, archivo, { upsert: true, contentType: archivo.type || "image/jpeg" });
      if (up.error) throw up.error;
      const url = sb.storage.from("media").getPublicUrl(ruta).data.publicUrl;
      const r = await sb.from("posts").insert({ id: postId, user_id: Store.nube.dueno, tipo: "VISION", texto: c.afirmacion, image_url: url, eje: c.eje || null, anio: new Date().getFullYear(), visibilidad: "PRIVADA", meta_titulo: c.titulo });
      if (r.error) throw r.error;
      (estado.misPosts ||= []).unshift({ id: postId, tipo: "VISION", eje: c.eje, texto: c.afirmacion, foto: url, metaTitulo: c.titulo, creadoEn: Date.now(), propio: true });
    } else {
      postId = "mio-" + nuevoId();
      misPostsDemo.unshift({ id: postId, autorNombre: perfil().nombre || "Yo", tipo: "VISION", eje: c.eje, texto: c.afirmacion, foto: URL.createObjectURL(archivo), metaTitulo: c.titulo, impulsos: 0, comentarios: 0, yoImpulse: false, creadoEn: Date.now(), propio: true });
      estado.misPosts = misPostsDemo;
    }
    await Store.cambiar("vision", id, { publicacionId: postId });
    toast("Foto agregada");
  } catch (e) { errorNube(e); }
}
/* ---------- Avance de cada casilla: estado (Abierta, En camino, Lograda) y porcentaje ---------- */
const ESTADOS_CASILLA = [["ABIERTA", "Abierta"], ["CAMINO", "En camino"], ["LOGRADA", "Lograda"]];
/** % de una casilla: si es la cumbre o un campamento del 9×9, sale de sus pasos ganados; si no, de su estado. */
function avanceCasilla(c, d = window.Mandala ? Mandala.datos() : null) {
  const manual = respuesta(`vision:estado:${c.id}`), pctMano = Number(respuesta(`vision:pct:${c.id}`)) || 0;
  if (manual === "LOGRADA") return { pct: 100, estado: "LOGRADA" };
  let pct = null;
  if (d) {
    const i = d.camps.findIndex(x => x && x.id === c.id);
    if (i >= 0) pct = mdPct(d, i);
    else if (d.cumbre && d.cumbre.id === c.id) pct = mdPct(d, -1);
  }
  if (pct === null) pct = pctMano;
  const estado = pct >= 100 ? "LOGRADA" : manual === "ABIERTA" && pct === 0 ? "ABIERTA" : pct > 0 || manual === "CAMINO" ? "CAMINO" : "ABIERTA";
  return { pct: estado === "LOGRADA" ? 100 : pct, estado };
}

/** Tarjeta de una casilla del vision board (también la usa el 9×9 para la cumbre y los campamentos). */
function casillaVisionHtml(c, md = window.Mandala ? Mandala.datos() : null, enBloque = false) {
  const color = e => COLOR_EJE[e] || "#6B2A1A";
    const foto = fotoDePublicacion(c.publicacionId), k = String(c.id);
    const av = avanceCasilla(c, md), est = ESTADOS_CASILLA.find(x => x[0] === av.estado)[1];
    const edicion = `<div class="casilla-edicion">
        <textarea class="casilla-frase" rows="2" aria-label="Frase de la casilla" placeholder="Escribe la frase de esta casilla" ${bind("vision", k, "afirmacion")}>${esc(c.afirmacion)}</textarea>
        <input class="casilla-rotulo" aria-label="Rótulo" placeholder="Rótulo" ${bind("vision", k, "titulo")} value="${esc(c.titulo)}">
        <div class="casilla-fila">
          <button class="casilla-estado" data-acc="estadoCasilla" data-arg="${k}" title="Toca para cambiar el estado">${est}</button><span style="flex:1"></span>
          ${foto ? `<button class="btn mini" data-acc="quitarFotoVision" data-arg="${k}" title="Quitar la foto">${ic("borrar")}</button>`
            : `<a href="${urlIdeas(c.busqueda)}" target="_blank" rel="noopener" class="btn mini">${ic("buscar")} Ideas</a>`}
        </div>
        ${estado.pctCasilla === k ? `<div class="casilla-ajuste"><label for="pct-${k}">Avance</label>
          <input id="pct-${k}" type="range" min="0" max="100" step="5" value="${av.pct}" data-acc-input="pctCasillaVer" data-acc-change="pctCasillaGuardar" data-arg="${k}"><b>${av.pct}%</b>
          <small>${md && (md.camps.some(x => x && String(x.id) === k) || String(md.cumbre?.id) === k) ? "Nunca baja de lo que llevas en el 9×9." : ""}</small></div>` : ""}
        ${(() => { const i = md ? md.camps.findIndex(x => x && x.id === c.id) : -1; if (i < 0 || Mandala.CAMPAMENTOS_FIJOS[i] === "CAM") return "";
          const h0 = hoyFecha(), hoy = h0.getFullYear(), mes = h0.getMonth() + 1, c0 = cascada();
          const ps = propositosDeCampamento(i), ms = metasDeCampamento(i), mm = mensualesDeCampamento(i);
          const fila = (niv, arg, txt, pct) => `<button data-ir="metas" data-arg="${arg}"><span><small>${niv}</small> ${txt}</span><span class="pct">${pctTxt(pct)}</span></button>`;
          const filas = [...ps.slice(0, 2).map(p => fila("Largo", "p:" + p.id, esc(p.titulo), c0.nodoProp(p))),
            ...ms.filter(m => m.anio >= hoy).slice(0, 2).map(m => fila(String(m.anio), "a:" + m.id, esc(m.titulo), c0.nodoAnio(m))),
            ...mm.filter(m => m.anio === hoy && m.mes === mes).slice(0, 2).map(m => fila(MESES[m.mes - 1].slice(0, 3), "m:" + m.id, esc(m.texto), avanceMetaMes(m)))];
          return `<div class="casilla-metas"><div class="casilla-metas-cab"><b>Lo que la sostiene</b><span>${ps.length} largo · ${ms.length} año · ${mm.length} mes</span></div>
            ${filas.join("")}
            <button class="btn mini oro" data-acc="nuevaMetaAnio" data-arg="${hoy}||${Mandala.CAMPAMENTOS_FIJOS[i] === "CON" ? "" : Mandala.CAMPAMENTOS_FIJOS[i]}">${ic("mas")} Meta para ${hoy}</button></div>`; })()}
        <details class="casilla-mas"><summary>Más opciones</summary>
          ${campo("Qué foto buscar o tomar", area("vision", k, "sugerencia", c.sugerencia))}
          <button class="btn mini peligro" data-acc="borrar" data-arg="vision|${k}">${ic("borrar")} Quitar casilla</button></details>
      </div>`;
    const pct = `<button class="casilla-pct-btn" data-acc="pctCasilla" data-arg="${k}" title="Ajustar el avance" aria-label="Avance ${av.pct} %. Toca para ajustarlo">${mdAnillo(av.pct, "esquina")}</button>`;
    if (enBloque) return `<div class="hoja casilla-v en-bloque est-${av.estado.toLowerCase()}" data-casilla="${k}" style="padding:0;--c:${color(c.eje)}">${pct}<label class="casilla-foto" style="--c:${color(c.eje)}" title="${foto ? "Cambiar la foto" : "Agregar tu foto"}">
        <input type="file" accept="image/*" hidden data-foto-vision="${k}">
        ${foto ? `<img src="${esc(foto)}" alt=""><span class="casilla-cambiar">${ic("foto")} Cambiar foto</span>`
          : `<span class="casilla-sug">${esc(c.sugerencia)}</span><span class="casilla-btn">${ic("foto")} Agregar mi foto</span>`}
      </label>${edicion}</div>`;
    /* La tarjeta tiene dos caras, como una carta: al frente la foto limpia; al girarla, la frase y lo que se edita.
       Se mueve dejándola presionada (o arrastrando con el mouse) y la foto se abre a pantalla completa. */
    const girada = estado.giradas?.has(k), apodo = apodoDe("vision", k), nombre = apodo || c.titulo || "";
    return `<div class="hoja casilla-v giro${girada ? " girada" : ""} est-${av.estado.toLowerCase()}" data-casilla="${k}" style="--c:${color(c.eje)}">
      <div class="giro-in">
        <div class="cara" data-mover-casilla="${k}" tabindex="0" role="group" aria-label="${esc(nombre || "Tarjeta")}. Déjala presionada para moverla, o usa las flechas"${girada ? " inert" : ""}>
          ${foto ? `<button class="cara-foto" data-acc="verTarjetas" data-arg="${k}" aria-label="Ver en pantalla completa"><img src="${esc(foto)}" alt="" draggable="false"></button>`
            : `<label class="cara-foto vacia" title="Agregar tu foto"><input type="file" accept="image/*" hidden data-foto-vision="${k}">
               <span class="casilla-sug">${esc(c.sugerencia)}</span><span class="casilla-btn">${ic("foto")} Agregar mi foto</span></label>`}
          ${pct}
          <div class="cara-pie">${nombre ? `<span class="cara-apodo">${apodo ? "«" + esc(apodo) + "»" : esc(nombre)}</span>` : "<span></span>"}
            <button class="casilla-girar" data-acc="girarCasilla" data-arg="${k}" aria-label="Girar la tarjeta para ver y editar la frase" title="Girar">${ic("girar")}</button></div>
        </div>
        <div class="dorso"${girada ? "" : " inert"}>
          <div class="dorso-cab"><span>${foto ? `<label class="btn mini" title="Cambiar la foto"><input type="file" accept="image/*" hidden data-foto-vision="${k}">${ic("foto")} Foto</label>` : ""}</span>
            <button class="casilla-girar" data-acc="girarCasilla" data-arg="${k}" aria-label="Volver a la foto" title="Volver a la foto">${ic("girar")}</button></div>
          <input class="casilla-apodo" maxlength="32" aria-label="Apodo de la tarjeta" placeholder="Apodo (opcional)" data-resp="apodo|apodo:vision:${k}" value="${esc(apodo)}">
          ${edicion}
        </div>
      </div></div>`;
  }
window.casillaVisionHtml = casillaVisionHtml;

DET["perfil.vision"] = () => {
  if (!estado.misPosts) cargarMisPosts();
  const cs = casillasVision(), conFoto = cs.filter(c => fotoDePublicacion(c.publicacionId)).length;
  const md = window.Mandala && cs.length ? Mandala.datos() : null;
  const color = e => COLOR_EJE[e] || "#6B2A1A";
  const tarjeta = c => casillaVisionHtml(c, md);


  return cab(av(ic("ia"), "var(--burdeos)"), "Mi cumbre y mi Brújula", cs.length ? `${conFoto} de ${cs.length} casillas con tu foto` : "Ármalo con IA y tus datos",
    cs.length ? `<button class="btn mini oro" data-acc="casillaNueva">${ic("mas")} Casilla</button>` : "") + cuerpo(visionCompleta());
};
/** El vision board completo (cumbre 1 · vision board 9 · Brújula 81): el mismo en Perfil y en Mi vida. */
function visionCompleta() {
  if (!estado.misPosts) cargarMisPosts();
  const cs = casillasVision(), conFoto = cs.filter(c => fotoDePublicacion(c.publicacionId)).length;
  const md = window.Mandala && cs.length ? Mandala.datos() : null;
  const tarjeta = c => casillaVisionHtml(c, md);
  return (
    hoja(`<p style="margin-top:0">La IA arma tu tablero con tu cumbre, tus propósitos y tus metas. Tú lo llenas con tus fotos: así cada imagen es de tu vida, no de un catálogo.</p>
      <details class="avance-m3-ayuda"><summary>¿De dónde sale el % de cada casilla?</summary>
        <p>Toca la etiqueta para pasar de <b>Abierta</b> a <b>En camino</b> (empieza en 25 %) y a <b>Lograda</b> (100 %). Toca el % de la esquina para ajustarlo con la barra. En la cumbre y los 8 campamentos también cuentan los pasos del 9×9 (<b>Avancé hoy</b>): el % nunca baja de lo que llevas allí, y la cumbre es el promedio de sus campamentos.</p></details>
      ${cs.length ? `<div class="barra" style="margin:10px 0"><i style="width:${conFoto / cs.length * 100}%"></i></div>` : ""}
      <div class="botones">${armandoVision ? `<span class="suave">Armando tu tablero…</span>`
        : `<button class="btn ${cs.length ? "" : "lleno"}" data-acc="armarVision">${ic("ia")} ${cs.length ? "Volver a proponer" : "Armar con IA"}</button>${cs.length ? `<span class="suave">Solo cambian las casillas que aún no tienen foto.</span>` : ""}`}</div>
      ${visionConIa === false ? `<p class="suave" style="margin-bottom:0">Lo armé con tus datos, sin conexión a la IA.</p>` : ""}`) +
    (cs.length ? mandalaHtml(`<div class="tablero">${cs.filter(c => c.id !== md?.cumbre?.id).map(tarjeta).join("")}</div><p class="suave" style="margin-bottom:0">Deja presionada una tarjeta para moverla y gírala para editar su frase. Tus fotos son publicaciones de visión privadas: solo tú las ves hasta que decidas compartirlas.</p>`) : ""));
}
