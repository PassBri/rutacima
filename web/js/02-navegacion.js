"use strict";
/* ======================================================================
 * Navegación (igual que WhatsApp Web: riel, lista y detalle)
 * ====================================================================== */
const SECCIONES = [
  ["ruta", "Mi ruta"], ["hoy", "Hoy"], ["metas", "Metas"], ["comunidad", "Comunidad"],
  ["aprende", "Aprende"], ["coach", "Coach"], ["mensajes", "Mensajes"], ["frases", "Mis frases"], ["perfil", "Perfil"],
];
/** En el teléfono la barra es igual a la de la app: 5 secciones y el botón de publicar en medio.
 *  Metas, Coach y Mis frases se abren desde arriba (destello y menú) y desde Perfil. */
const SOLO_PC = new Set(["metas", "coach", "mensajes", "frases"]);
const estado = { sec: "ruta", sel: null, filtro: "todo", busqueda: "", wbSec: 0, feed: null, feedFiltro: "PARA_TI", post: null, wb: {}, vistaCom: "lista" };

function pintarRiel() {
  const sellada = !abiertas(new Date().getFullYear()).has(fraseIndice(hoyFecha()));
  $("riel").innerHTML =
    `<img class="sello" src="${LOGO}" alt="Rutaalacima">` +
    SECCIONES.map(([k, n], i) => (i === 3 ? `<button class="rb rb-publicar" data-ir="comunidad" data-arg="publicar" aria-label="Publicar">${ic("publicar")}<span class="tip">Publicar · planta tu bandera</span></button>` : "") +
      `<button class="rb ${SOLO_PC.has(k) ? "solo-pc" : ""}" data-sec="${k}" aria-label="${n}" ${estado.sec === k ? 'aria-current="page"' : ""}>${ic(k)}<span class="tip">${n}</span>${(k === "frases" && sellada) || (k === "mensajes" && typeof MS !== "undefined" && noLeidosTotal()) ? '<span class="punto"></span>' : ""}</button>`).join("") +
    `<span class="esp"></span>
     <button class="rb solo-pc" data-acc="tema" aria-label="Tema claro u oscuro">${ic("tema")}<span class="tip">Tema claro u oscuro</span></button>
     <button class="rb solo-pc" data-acc="salir" aria-label="${Store.nube ? "Cerrar sesión en este computador" : "Salir de la demostración"}">${ic("salir")}<span class="tip">${Store.nube ? "Cerrar sesión en este computador" : "Salir de la demostración"}</span></button>`;
}
function ir(sec, sel = null) {
  estado.sec = sec; estado.sel = sel; estado.filtro = "todo"; estado.busqueda = "";
  $("app").classList.toggle("con-detalle", sel !== null);
  pintarTodo();
}
function elegir(sel) {
  estado.sel = sel;
  $("app").classList.toggle("con-detalle", sel !== null);
  pintarLista(); pintarDetalle();
}
function pintarTodo() { pintarRiel(); pintarLista(); pintarDetalle(); }

/* Si estás escribiendo, el detalle espera a que termines antes de redibujarse */
let detallePendiente = false;
function enEdicion() { const a = document.activeElement; return a && $("detalle").contains(a) && /INPUT|TEXTAREA|SELECT/.test(a.tagName) && a.type !== "checkbox" && a.type !== "range"; }
function refrescar() {
  pintarRiel(); pintarLista();
  if (enEdicion()) detallePendiente = true; else pintarDetalle();
}
document.addEventListener("focusout", () => setTimeout(() => { if (detallePendiente && !enEdicion()) { detallePendiente = false; pintarDetalle(); } }, 0));

/* ---------- Lista ---------- */
function cabLista(titulo, { chips = [], buscar = true, nuevo = null } = {}) {
  const atajos = `<span class="solo-movil atajos">${estado.sec !== "metas" ? `<button class="rb" data-sec="metas" aria-label="Metas">${ic("metas")}</button>` : ""}${estado.sec !== "frases" ? `<button class="rb" data-sec="frases" aria-label="Mis frases">${ic("frases")}</button>` : ""}${estado.sec === "comunidad" ? `<button class="rb" data-sec="mensajes" aria-label="Mensajes">${ic("mensajes")}</button>` : ""}<button class="rb" data-sec="coach" aria-label="Coach" style="color:var(--oro)">${ic("coach")}</button></span>`;
  return `<div class="lista-cab"><div class="cab-lista-btn"><h1>${titulo}</h1><span style="display:flex;gap:4px;align-items:center">${atajos}${nuevo ? `<button class="nuevo" data-sel="${nuevo[0]}" aria-label="${nuevo[1]}" title="${nuevo[1]}">${ic("mas")}</button>` : ""}</span></div>
    ${buscar ? `<label class="buscar">${ic("buscar")}<input id="busq" type="search" placeholder="Buscar" value="${esc(estado.busqueda)}" aria-label="Buscar"></label>` : ""}
    ${chips.length ? `<div class="chips">${chips.map(([k, n]) => `<button class="chip" data-filtro="${k}" aria-pressed="${estado.filtro === k}">${n}</button>`).join("")}</div>` : ""}</div>`;
}
const item = (sel, avatar, titulo, hora, sub, extra = "") =>
  `<button class="item" data-sel="${esc(sel)}" aria-selected="${estado.sel === sel}">${avatar}<span class="txt">
     <span class="t1"><span>${titulo}</span><span class="hora">${hora}</span></span>
     <span class="t2"><span>${sub}</span>${extra}</span></span></button>`;
const av = (txt, color) => `<span class="avatar" style="background:${color}">${txt}</span>`;
const avSello = () => `<img class="avatar" src="${LOGO}" alt="">`;
const coincide = s => !estado.busqueda || String(s).toLowerCase().includes(estado.busqueda.toLowerCase());
const grupo = t => `<div class="grupo">${t}</div>`;
const pctTxt = x => `${Math.round(x * 100)}%`;

function pintarLista() {
  const f = LISTAS[estado.sec];
  $("lista").innerHTML = f ? f() : "";
}
const LISTAS = {
  ruta() {
    const r = recordatorio(), p = perfil();
    let h = cabLista("Mi ruta", { chips: [["todo", "Toda la vida"], ["plan", "Años del plan"], ["metas", "Con metas"]] }) + `<div class="items">`;
    if (!r) return h + `<p class="vacio-mini">Agrega tu fecha de nacimiento en Perfil para ver tu vida año por año.</p><div class="botones" style="justify-content:center"><button class="btn lleno" data-ir="perfil" data-arg="perfil">Ir a mi perfil</button></div></div>`;
    const anioHoy = new Date().getFullYear(), nac = p.anioNacimiento, ultimo = nac + metaVida();
    const conMetas = new Map();
    Store.lista("meta_anio").forEach(m => conMetas.set(m.anio, (conMetas.get(m.anio) || 0) + 1));
    const finPlan = (p.anioInicioPlan || anioHoy) + Math.max(5, ...Store.lista("proposito").map(x => x.horizonte || 5)) - 1;
    if (estado.filtro === "todo" && coincide("camino vida")) h += item("vida", avSello(), `Camino hacia los ${metaVida()} años`, "hoy", `Día ${r.dia.toLocaleString("es")} de tu vida`, ic("pin", "i fijado"));
    let anios = [];
    for (let a = anioHoy; a <= ultimo; a++) anios.push(a);
    for (let a = anioHoy - 1; a >= nac; a--) anios.push(a);
    if (estado.filtro === "plan") anios = anios.filter(a => a >= (p.anioInicioPlan || anioHoy) && a <= finPlan).sort((x, y) => x - y);
    if (estado.filtro === "metas") anios = anios.filter(a => conMetas.has(a)).sort((x, y) => x - y);
    let g = "";
    for (const a of anios) {
      const gr = a === anioHoy ? "Este año" : a > anioHoy ? "Por vivir" : "Vividos";
      if (estado.filtro === "todo" && gr !== g) { h += grupo(gr); g = gr; }
      const metas = Store.lista("meta_anio").filter(m => m.anio === a);
      const sub = metas[0]?.titulo || (a < anioHoy ? "Año vivido" : a <= finPlan ? `Año ${a - (p.anioInicioPlan || anioHoy) + 1} de tu plan` : "Sin metas aún");
      if (!coincide(`${a} ${sub}`)) continue;
      const color = a < anioHoy ? "var(--burdeos)" : a === anioHoy ? "var(--oro)" : "#B8A99C";
      h += item("a:" + a, av(a - nac, color), `${a} · ${a - nac} años`, a === anioHoy ? "ahora" : "", esc(sub), metas.length ? `<span class="badge suave">${metas.length}</span>` : "");
    }
    return h + `</div>`;
  },
  hoy() {
    const hoy = hoyFecha(), a = hoy.getFullYear(), m = hoy.getMonth() + 1;
    let h = cabLista("Hoy", { buscar: false }) + `<div class="items">`;
    h += item("d:" + iso(hoy), av(hoy.getDate(), "var(--burdeos)"), "Tu día", "hoy", `${marcadosDe(iso(hoy)).size} de 18 hábitos · ${MESES[hoy.getMonth()]}`, ic("pin", "i fijado"));
    h += grupo(`Metas de ${MESES[m - 1]}`);
    const ms = metasDelMes(a, m);
    if (!ms.length) h += `<p class="vacio-mini">Aún no hay metas este mes.</p>`;
    ms.forEach(x => {
      const hechoHoy = numeros(x.dias).includes(hoy.getDate());
      h += item("mm:" + x.id, av(pctTxt(avanceMetaMes(x)), COLOR_EJE[x.eje] || "var(--burdeos)"), esc(x.texto), hechoHoy ? "✓ hoy" : "", NOMBRE_EJE[x.eje] || "Meta del mes");
    });
    h += grupo("Ayer y antes");
    for (let k = 1; k <= 6; k++) {
      const d = new Date(hoy); d.setDate(d.getDate() - k);
      h += item("d:" + iso(d), av(d.getDate(), diaCumplido(d) ? "var(--oro)" : "#B8A99C"), mayus(d.toLocaleDateString("es", { weekday: "long" })), `${d.getDate()} ${MES_C[d.getMonth()]}`, `${marcadosDe(iso(d)).size} hábitos`);
    }
    return h + `</div>`;
  },
  metas() {
    const c = cascada(), anioHoy = new Date().getFullYear();
    const anios = [...new Set(c.anuales.map(x => x.anio))].sort((x, y) => x - y);
    let h = cabLista("Metas", { chips: [["todo", "Todo"], ["prop", "Propósitos"], ["anio", `${anioHoy}`], ["mes", "Este mes"]], nuevo: ["nuevo", "Nueva meta"] }) + `<div class="items">`;
    const f = estado.filtro;
    if (f === "todo" || f === "prop") {
      h += grupo("Propósitos a largo plazo");
      const ps = c.props.filter(p => coincide(p.titulo)).sort((x, y) => (x.orden || 0) - (y.orden || 0) || x.id - y.id);
      if (!ps.length) h += `<p class="vacio-mini">Todavía no hay propósitos.</p>`;
      ps.forEach(p => { h += item("p:" + p.id, av(pctTxt(c.nodoProp(p)), COLOR_EJE[p.eje] || "var(--burdeos)"), esc(p.titulo), `${p.horizonte || 5} años`, `Prioridad ${p.prioridad || "B"}`); });
    }
    if (f === "todo" || f === "anio") {
      for (const a of (f === "anio" ? [anioHoy] : anios)) {
        const xs = c.anuales.filter(x => x.anio === a && coincide(x.titulo));
        if (!xs.length && f !== "anio") continue;
        h += grupo(`Metas de ${a}`);
        if (!xs.length) h += `<p class="vacio-mini">Sin metas para ${a}.</p>`;
        xs.forEach(x => { h += item("a:" + x.id, av(pctTxt(c.nodoAnio(x)), COLOR_EJE[x.eje] || "var(--oro)"), esc(x.titulo), x.prioridad || "", ESTADOS.find(e => e[0] === x.estado)?.[1] || ""); });
      }
    }
    if (f === "todo" || f === "mes") {
      const hoy = hoyFecha();
      const xs = metasDelMes(hoy.getFullYear(), hoy.getMonth() + 1).filter(x => coincide(x.texto));
      h += grupo(`Metas de ${MESES[hoy.getMonth()]}`);
      if (!xs.length) h += `<p class="vacio-mini">Sin metas este mes.</p>`;
      xs.forEach(x => { h += item("m:" + x.id, av(pctTxt(avanceMetaMes(x)), COLOR_EJE[x.eje] || "var(--burdeos)"), esc(x.texto), "", `${numeros(x.dias).length} de ${x.objetivoDias || 20} días`); });
    }
    return h + `</div>`;
  },
  comunidad() {
    let h = cabLista("Comunidad", { buscar: false, nuevo: ["publicar", "Publicar"] });
    h += `<div class="lista-cab" style="padding-top:0">
      <div class="chips">${[["PARA_TI", "Para ti"], ["VISION", "Visión"], ["SIGUIENDO", "Siguiendo"]].map(([k, n]) => `<button class="chip" data-feed="${k}" aria-pressed="${estado.feedFiltro === k}">${n}</button>`).join("")}</div></div><div class="items">`;
    if (!estado.feed) { cargarFeed(); return h + `<p class="vacio-mini">Cargando…</p></div>`; }
    if (!estado.feed.length) h += `<p class="vacio-mini">${estado.feedFiltro === "SIGUIENDO" ? "Todavía no sigues a nadie." : "Aún no hay publicaciones."}</p>`;
    // En el celular el muro no está al lado: la expedición va arriba de la lista
    if (estado.feedFiltro === "PARA_TI" && estado.feed.length) h += `<div class="solo-movil exp-movil">${cumbreComunidad(estado.feed)}</div>`;
    estado.feed.forEach(p => {
      const ini = (p.autorNombre || "?").split(" ").map(x => x[0]).join("").slice(0, 2).toUpperCase();
      h += item((p.brujula && window.leerBrujula?.(p.brujula) ? "brujula:" : "cima:") + p.id, av(esc(ini), COLOR_EJE[p.eje] || "var(--burdeos)"), esc(p.autorNombre), hace(p.creadoEn), esc(p.texto), `<span class="badge ${p.yoImpulse ? "" : "suave"}">${p.impulsos}</span>`);
    });
    return h + `</div>`;
  },
  aprende() {
    const CAT = { ruta: "Tu ruta", herramientas: "Herramientas", bonos: "Bonos", lecturas: "Lecturas", facilitador: "Facilitadores" };
    let h = cabLista("Aprende", { chips: [["todo", "Todo"], ["ruta", "Ruta"], ["herramientas", "Herramientas"], ["bonos", "Bonos"], ["lecturas", "Lecturas"]] }) + `<div class="items">`;
    if (!Contenido.indice) { Contenido.cargarIndice(); return h + `<p class="vacio-mini">Cargando las guías…</p></div>`; }
    const resp = Store.lista("respuesta");
    let g = "";
    [...Contenido.indice].sort((a, b) => Object.keys(CAT).indexOf(a.category) - Object.keys(CAT).indexOf(b.category) || a.order - b.order)
      .filter(x => (estado.filtro === "todo" || x.category === estado.filtro) && coincide(x.title + x.subtitle)).forEach(x => {
        if (x.category !== g) { h += grupo(CAT[x.category] || x.category); g = x.category; }
        const n = resp.filter(r => r.workbookId === x.id && !r.clave.includes("#")).length;
        h += item("wb:" + x.id, av(esc(x.title.trim()[0]), x.category === "bonos" ? "var(--oro)" : "var(--burdeos)"), esc(x.title), n ? `${n}/${x.inputs}` : "", esc(x.subtitle));
      });
    return h + `</div>`;
  },
  coach() {
    const ms = mensajesCoach(), u = ms[ms.length - 1];
    let h = cabLista("Coach", { buscar: false }) + `<div class="items">`;
    h += item("coach", av(ic("coach"), "var(--burdeos)"), "Coach Rutaalacima", u ? horaDe(u.creadoEn) : "", esc(u ? separarAccion(u.texto)[0] : "Pregúntame por tu ruta"));
    h += grupo("Atajos");
    [["Perdí la motivación", "Perdí la motivación"], ["Planea mi semana", "Planea mi semana"], ["¿Qué eje debo trabajar?", "¿Qué eje debo trabajar?"], ["Divide mi meta anual en pasos", "Divide mi meta anual en pasos"]].forEach(([t, q]) => {
      h += `<button class="item" data-pregunta="${esc(q)}">${av(ic("coach"), "var(--oro)")}<span class="txt"><span class="t1"><span>${t}</span></span><span class="t2"><span>Preguntar al coach</span></span></span></button>`;
    });
    return h + `</div>`;
  },
  frases() {
    let h = cabLista("Mis frases", { chips: [["todo", "Todas"], ["abiertas", "Abiertas"]] }) + `<div class="items">`;
    if (!Contenido.frases) { Contenido.cargarFrases(); return h + `<p class="vacio-mini">Cargando…</p></div>`; }
    const hoy = hoyFecha();
    for (let k = 0; k < 60; k++) {
      const d = new Date(hoy); d.setDate(d.getDate() - k);
      const abierta = abiertas(d.getFullYear()).has(fraseIndice(d));
      if (estado.filtro === "abiertas" && !abierta) continue;
      const fr = Contenido.frases[fraseIndice(d)];
      if (!coincide(abierta ? fr.t : "")) continue;
      h += item("f:" + iso(d), abierta ? av(ic("frases"), "var(--oro)") : avSello(), k === 0 ? "Frase de hoy" : `Día ${diaDelAnio(d) + 1}`, `${d.getDate()} ${MES_C[d.getMonth()]}`,
        abierta ? esc(fr.t) : `${ic("candado", "i fijado")} Sellada`, k === 0 && !abierta ? `<span class="badge">1</span>` : "");
    }
    return h + `</div>`;
  },
  perfil() {
    const p = perfil(), ult = ultimaEvaluacion();
    let h = cabLista("Perfil", { buscar: false }) + `<div class="items">`;
    h += item("perfil", av(esc((p.nombre || "Y")[0].toUpperCase()), "var(--burdeos)"), esc(p.nombre || "Tu perfil"), "", esc(Store.nube?.perfil?.username ? "@" + Store.nube.perfil.username : p.cumbreFrase || "Nombre, cumbre y fecha de nacimiento"));
    h += grupo("Mi ascenso");
    h += item("ir-metas", av(ic("metas"), "var(--burdeos)"), "Metas", "", "Propósitos, metas del año y del mes");
    h += item("ejes", av(ic("comunidad"), "var(--oro)"), "Mis ejes", ult ? new Date(ult.fecha).toLocaleDateString("es") : "", ult ? "Última evaluación" : "Evalúa tus 6 ejes");
    const vs = Store.lista("vision");
    h += item("vision", av(ic("coach"), "var(--oro)"), "Mi vision board", vs.length ? `${vs.filter(v => v.publicacionId).length}/${vs.length}` : "", vs.length ? "Llénalo con tus fotos" : "Ármalo con IA y tus datos");
    h += item("publicaciones", av(ic("foto"), "var(--burdeos)"), "Mis publicaciones", "", "Tu diario de vida");
    h += item("ir-frases", av(ic("frases"), "var(--oro)"), "Mis frases", "", "Las frases que ya abriste");
    h += grupo("Personas");
    h += item("ir-mensajes", av(ic("mensajes"), "var(--burdeos)"), "Mensajes", "", "Tus conversaciones 1 a 1");
    h += item("ir-coachvida", av(ic("cumbre"), "var(--oro)"), "Coach de vida", "", "Una persona que te acompaña");
    h += item("ir-coach", av(ic("coach"), "var(--burdeos)"), "Coach con IA", "", "Pregúntale por tu ruta");
    h += grupo("Más");
    h += item("plan", av(ic("cumbre"), "var(--oro)"), "Plan Cumbre", "", globalThis.PLAN?.estado?.plan === "cumbre" ? "Tienes el Plan Cumbre" : "Qué incluye y cuánto cuesta");
    h += item("cuenta", av(ic("salir"), "#8A7B70"), Store.nube ? (Store.nube.directa ? "Mi cuenta" : "Este computador") : "Demostración", "", Store.nube ? (Store.nube.directa ? Store.nube.correo || nombreNavegador() : nombreNavegador()) + (globalThis.MOD?.suspension ? " · cuenta suspendida" : "") : "Ruta de ejemplo");
    // Moderación: solo para cuentas moderadoras (se comprueba una vez por sesión)
    const mod = globalThis.MOD;
    if (Store.nube && mod && mod.soy === null) comprobarModeracion();
    if (mod?.soy) h += item("moderacion", av(ic("mazo"), "var(--burdeos)"), "Moderación", mod.casos?.length ? String(mod.casos.length) : "", "Reportes de la comunidad");
    return h + `</div>`;
  },
};
