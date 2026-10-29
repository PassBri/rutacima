"use strict";
/* Mi ruta con la barra de vida (igual que la app): el contenido va en el centro y los puntos de vida en una barra
 * a la derecha (arriba en el teléfono). Clic en un año: aparecen sus meses; clic en un mes: aparecen sus días.
 * Se vuelve con la flecha de la barra, con Esc o tocando otra vez el año o el mes abierto. */
const MES_CORTO = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];
const pad2 = n => String(n).padStart(2, "0");

/** Dónde está la persona: año, mes y día abiertos según lo que muestra el centro. */
function rvLugar() {
  const v = String(estado.sel || ""), [t, arg = ""] = [v.split(":")[0], v.split(":").slice(1).join(":")];
  if (t === "a" && /^\d{4}$/.test(arg)) return { a: Number(arg) };
  if (t === "m" && /^\d{4}-\d\d$/.test(arg)) { const [a, m] = arg.split("-").map(Number); return { a, m }; }
  if (t === "d" && /^\d{4}-\d\d-\d\d$/.test(arg)) { const [a, m, d] = arg.split("-").map(Number); return { a, m, d }; }
  return estado.rvUltimo && !["puntos", "props", "conmetas", ""].includes(v) ? estado.rvUltimo : {};
}

/** Un nivel arriba: día → mes → año → vida. */
function rvArriba() {
  const l = rvLugar(), v = String(estado.sel || "");
  if (v.startsWith("d:")) return `m:${l.a}-${pad2(l.m)}`;
  if (v.startsWith("m:")) return `a:${l.a}`;
  if (v.startsWith("a:")) return "puntos";
  if (/^(p|ma|mm):/.test(v)) return estado.rvUltimo?.d ? `d:${estado.rvUltimo.a}-${pad2(estado.rvUltimo.m)}-${pad2(estado.rvUltimo.d)}` : estado.rvUltimo?.m ? `m:${estado.rvUltimo.a}-${pad2(estado.rvUltimo.m)}` : estado.rvUltimo?.a ? `a:${estado.rvUltimo.a}` : "puntos";
  return null;
}

/** La barra de vida como selector de fecha (Material 3): año, meses y calendario del mes a la vista, siempre al día con
 *  lo que muestra el centro. ‹ › cambian el año o el mes que miras sin abrirlo; tocar un mes o un día lo abre al centro. */
function rvVer() {
  const l = rvLugar(), hoy = hoyFecha();
  if (!estado.rvVer || estado.rvVerSel !== estado.sel) { estado.rvVer = { a: l.a || hoy.getFullYear(), m: l.m || (l.a && l.a !== hoy.getFullYear() ? 1 : hoy.getMonth() + 1) }; estado.rvVerSel = estado.sel; }
  return estado.rvVer;
}
function rvBarra() {
  const p = perfil(), nac = p.anioNacimiento; if (!nac) return "";
  const hoy = hoyFecha(), aHoy = hoy.getFullYear(), mHoy = hoy.getMonth() + 1, l = rvLugar(), ver = rvVer();
  const v = String(estado.sel || ""), arriba = rvArriba(), a = ver.a, m = ver.m, edad = a - nac;
  const aro = (cls, dentro = "") => `<span class="pv ${cls}" aria-hidden="true">${PUNTO_MARCA}${dentro}</span>`;
  const ev = estado.sec === "evaluacion";
  const donde = (ev ? "Evaluar · " : "") + (l.d ? `${l.d} ${MES_CORTO[l.m - 1]} ${l.a}` : l.m ? `${mayus(MESES[l.m - 1])} ${l.a}` : l.a ? `${l.a}` : v === "props" ? "Propósitos" : v === "conmetas" ? "Metas por año" : "Mi vida en años");
  const isoHoy = `d:${aHoy}-${pad2(mHoy)}-${pad2(hoy.getDate())}`;
  // Meses: 4 × 3 aros
  let meses = "";
  for (let k = 1; k <= 12; k++) {
    const n = metasDelMes(a, k).length, cm = a < aHoy || (a === aHoy && k < mHoy) ? "vivido" : a === aHoy && k === mHoy ? "actual" : "futuro";
    const abierto = l.a === a && l.m === k, viendo = k === m;
    meses += `<button class="rv-mes${abierto ? " abierto" : ""}${viendo ? " viendo" : ""}" data-sel="m:${a}-${pad2(k)}" aria-label="${MESES[k - 1]} de ${a}${n ? `, ${n} metas` : ""}" title="${mayus(MESES[k - 1])}${n ? ` · ${n} ${n === 1 ? "meta" : "metas"}` : ""}">
      ${aro(cm + (n ? " meta" : ""))}<span>${MES_CORTO[k - 1]}</span>${n ? `<i class="rv-n">${n}</i>` : ""}</button>`;
  }
  // Calendario del mes que se mira (semanas de lunes a domingo)
  const ini = (new Date(a, m - 1, 1).getDay() + 6) % 7;
  let dias = ["L", "M", "X", "J", "V", "S", "D"].map(x => `<b class="rv-sem" aria-hidden="true">${x}</b>`).join("") + `<i class="rv-vacio"></i>`.repeat(ini);
  for (let d = 1; d <= diasEnMes(a, m); d++) {
    const f = new Date(a, m - 1, d), cd = f < hoy ? "vivido" : f.getTime() === hoy.getTime() ? "actual" : "futuro", hecho = f <= hoy && diaCumplido(f);
    dias += `<button class="rv-dia${l.a === a && l.m === m && l.d === d ? " abierto" : ""}" data-sel="d:${a}-${pad2(m)}-${pad2(d)}" aria-label="${d} de ${MESES[m - 1]} de ${a}${hecho ? ", cumplido" : ""}">${aro(cd + (hecho ? " meta" : ""), `<b>${d}</b>`)}</button>`;
  }
  const nav = (sel, ic0, txt, actual) => `<button class="rv-nav-item" data-sel="${sel}" aria-pressed="${actual}">${ic(ic0)}<span>${txt}</span></button>`;
  const enBrujula = estado.sec === "ruta" && estado.sel === "vida";
  const titulo = enBrujula ? "Calendario de tu vida" : donde;
  return `<nav class="rv-barra${enBrujula ? " rv-en-brujula" : ""}" aria-label="Tu vida: año, mes y día">
    <div class="rv-cab">${enBrujula ? "" : `<button class="rv-atras" ${arriba ? `data-sel="${arriba}"` : "disabled"} aria-label="Subir un nivel (Esc)" title="Subir un nivel (Esc)">${ic("volver")}</button>`}
      <b class="rv-donde">${titulo}</b><button class="rv-hoy" data-sel="${isoHoy}" title="Ir a hoy">Hoy</button>
      ${enBrujula ? `<button class="rv-atras" data-acc="rvBrujulaPanel" aria-label="Cerrar el calendario" title="Cerrar el calendario">${ic("cerrar")}</button>`
        : `<button class="rv-plegar solo-movil" data-acc="rvPlegar" aria-expanded="${!!estado.rvAbierta}" aria-label="${estado.rvAbierta ? "Ocultar el calendario" : "Mostrar el calendario"}">${ic(estado.rvAbierta ? "arriba" : "abajo")}</button>`}</div>
    <div class="rv-cuerpo${estado.rvAbierta || enBrujula ? " abierta" : ""}">
      <label class="rv-buscar">${ic("buscar")}<input type="search" placeholder="Buscar metas y propósitos" value="${esc(estado.rvBusca || "")}" data-acc-input="rvBuscar" aria-label="Buscar metas y propósitos en Mi ruta"></label>
      <div class="rv-resultados" aria-live="polite">${rvResultados()}</div>
      <div class="rv-paso"><button class="rv-flecha" data-acc="rvAnio" data-arg="-1" aria-label="Año anterior" ${a <= nac ? "disabled" : ""}>${ic("izq")}</button>
        <button class="rv-titulo${l.a === a && !l.m ? " abierto" : ""}" data-acc="rvMapa" aria-expanded="${!!estado.rvMapa}" title="Elegir otro año">${a}<small>${edad} años ${ic(estado.rvMapa ? "arriba" : "abajo")}</small></button>
        <button class="rv-flecha" data-acc="rvAnio" data-arg="1" aria-label="Año siguiente" ${a >= nac + metaVida() - 1 ? "disabled" : ""}>${ic("der")}</button></div>
      ${estado.rvMapa ? rvMiniMapa(nac, a) : ""}
      <div class="rv-meses" role="group" aria-label="Meses de ${a}">${meses}</div>
      <div class="rv-paso rv-paso-mes"><button class="rv-flecha" data-acc="rvMes" data-arg="-1" aria-label="Mes anterior">${ic("izq")}</button>
        <button class="rv-titulo${l.a === a && l.m === m && !l.d ? " abierto" : ""}" data-sel="m:${a}-${pad2(m)}" title="Abrir ${MESES[m - 1]}">${mayus(MESES[m - 1])}</button>
        <button class="rv-flecha" data-acc="rvMes" data-arg="1" aria-label="Mes siguiente">${ic("der")}</button></div>
      <div class="rv-cal" role="group" aria-label="Días de ${MESES[m - 1]} de ${a}">${dias}</div>
      <div class="rv-navs">${nav("puntos", "vida", "Mi vida en años", v === "puntos")}${nav("props", "brujula", "Propósitos", v === "props")}${nav("conmetas", "metas", "Metas por año", v === "conmetas")}</div>
      ${rvRecordatorio()}
    </div></nav>`;
}
/** Lo que se busca en Mi ruta: propósitos, metas del año y metas del mes que contienen el texto (hasta 8). */
function rvResultados() {
  const q = String(estado.rvBusca || "").trim(); if (q.length < 2) return "";
  const n = t => String(t || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase(), nq = n(q);
  const xs = [...Store.lista("proposito").map(x => ["p:" + x.id, x.titulo, "Propósito"]),
    ...Store.lista("meta_anio").map(x => ["ma:" + x.id, x.titulo, `Meta de ${x.anio}`]),
    ...Store.lista("meta_mes").map(x => ["mm:" + x.id, x.titulo || x.texto, x.anio && x.mes ? `Meta de ${MESES[x.mes - 1]} ${x.anio}` : "Meta del mes"])]
    .filter(([, t]) => t && n(t).includes(nq)).slice(0, 8);
  return xs.length ? xs.map(([sel, t, tipo]) => `<button class="rv-res" data-sel="${sel}"><b>${esc(t)}</b><small>${tipo}</small></button>`).join("")
    : `<p class="rv-res-nada">Nada con «${esc(q)}».</p>`;
}
ACC.rvBuscar = el => { estado.rvBusca = el.value; const r = document.querySelector(".rv-resultados"); if (r) r.innerHTML = rvResultados(); };
/** En la Brújula, el calendario de la vida se abre y se cierra desde el riel (se recuerda). */
ACC.rvBrujulaPanel = () => { estado.rvBrujula = !estado.rvBrujula; try { localStorage.setItem("suroute-rv-brujula", estado.rvBrujula ? "1" : ""); } catch {} pintarDetalle(); };
if (estado.rvBrujula == null) { try { estado.rvBrujula = localStorage.getItem("suroute-rv-brujula") === "1"; } catch { estado.rvBrujula = false; } }

/** Mini mapa de vida: los mismos aros, una fila por década. Un toque mira ese año; otro toque al mismo año lo abre. */
function rvMiniMapa(nac, visto) {
  const aHoy = hoyFecha().getFullYear(), conMetas = new Set(Store.lista("meta_anio").map(m => m.anio));
  let h = "";
  for (let i = 0; i < metaVida(); i++) {
    const y = nac + i, cls = y < aHoy ? "vivido" : y === aHoy ? "actual" : "futuro";
    if (i % 10 === 0) h += `${i ? "</div>" : ""}<div class="rv-dec"><small>${i}</small>`;
    h += `<button class="rv-mini${y === visto ? " viendo" : ""}" data-acc="rvAnioIr" data-arg="${y}" aria-label="${y}, ${i} años${conMetas.has(y) ? ", con metas" : ""}" title="${y} · ${i} años${conMetas.has(y) ? " · con metas" : ""}">
      <span class="pv ${cls}${conMetas.has(y) ? " meta" : ""}" aria-hidden="true">${PUNTO_MARCA}</span></button>`;
  }
  return `<div class="rv-mapa" role="group" aria-label="Elige un año de tu vida">${h}</div><p class="rv-mapa-ayuda">Toca un año para verlo; tócalo otra vez para abrirlo.</p></div>`;
}
/** El recordatorio del día, compacto, al final de la barra (en el computador): así el centro deja ver tu vida en años completa. */
function rvRecordatorio() {
  const r = recordatorio(); if (!r) return "";
  const pct = Math.round(r.dia / r.total * 100);
  return `<section class="rv-rec" aria-label="Recordatorio del día"><b class="rv-rec-t">Recordatorio del día</b>
    <div class="rv-rec-cifras"><div><b>${r.dia.toLocaleString("es")}</b><small>día de tu vida</small></div><div><b>${r.quedan.toLocaleString("es")}</b><small>días hasta los ${metaVida()}</small></div></div>
    <div class="rv-rec-barra" role="img" aria-label="${pct} % del camino"><i style="width:${Math.min(100, pct)}%"></i></div><small class="rv-rec-pct">${pct} % del camino</small>
    <p class="rv-rec-frase">${r.frase}</p></section>`;
}
/* Mirar otro año o mes sin abrirlo: solo se redibuja la barra */
function rvRepintar() { const b = document.querySelector(".rv-barra"); if (b) b.outerHTML = rvBarra(); }
ACC.rvAnio = (_, d) => { const ver = rvVer(), nac = perfil().anioNacimiento; ver.a = Math.min(nac + metaVida() - 1, Math.max(nac, ver.a + Number(d))); rvRepintar(); };
ACC.rvMes = (_, d) => { const ver = rvVer(); ver.m += Number(d); if (ver.m < 1) { ver.m = 12; ver.a--; } if (ver.m > 12) { ver.m = 1; ver.a++; } rvRepintar(); };
ACC.rvPlegar = () => { estado.rvAbierta = !estado.rvAbierta; rvRepintar(); };
ACC.rvMapa = () => { estado.rvMapa = !estado.rvMapa; rvRepintar(); };
ACC.rvAnioIr = (_, y) => {
  const ver = rvVer(), n = Number(y);
  if (ver.a === n) { estado.rvMapa = false; elegir("a:" + n); return; }
  ver.a = n; rvRepintar();
};

DET["ruta.props"] = () => {
  const c = cascada(), ps = c.props.slice().sort((x, y) => (x.prioridad || "B").localeCompare(y.prioridad || "B") || (x.orden || 0) - (y.orden || 0) || x.id - y.id);
  return cab(av(ic("brujula"), "var(--burdeos)"), "Propósitos", "Varios años · nacen de tu Brújula general") + cuerpo(
    (window.Sincro && Store.lista("vision").length ? `<div class="sm-rehacer"><span class="suave">Tus propósitos y metas salen de tu Brújula</span><button class="btn mini" data-acc="rehacerMetas">${ic("brujula")} Rehacer desde la Brújula</button></div>` : "") +
    hoja(`<div class="items rv-items">${ps.map(x => item("p:" + x.id, av(pctTxt(c.nodoProp(x)), COLOR_EJE[x.eje] || "var(--burdeos)"), esc(x.titulo), `${x.horizonte || 5} años`, apodoSeguro("p", x.id, (NOMBRE_EJE[x.eje] || "Confluencia") + ` · Prioridad ${x.prioridad || "B"}`))).join("")
      || `<p class="suave">Todavía no hay propósitos. Nacen de tu Brújula general.</p>`}</div>`), "max-width:760px");
};
DET["ruta.conmetas"] = () => {
  const c = cascada(), anios = [...new Set(c.anuales.map(x => x.anio))].sort((x, y) => x - y);
  return cab(av(ic("metas"), "var(--oro)"), "Metas por año", "Cada año con sus metas · nacen de la Brújula de ese año") + cuerpo(
    anios.map(a => hoja(`<div class="cab-lista-btn"><button class="etiqueta rv-anio-link" data-sel="a:${a}">${a}</button></div><div class="items rv-items">
      ${c.anuales.filter(x => x.anio === a).sort((x, y) => (x.prioridad || "B").localeCompare(y.prioridad || "B") || x.id - y.id)
        .map(m => item("ma:" + m.id, av(pctTxt(c.nodoAnio(m)), COLOR_EJE[ejeEfectivo("meta_anio", m)] || "var(--oro)"), esc(m.titulo), m.prioridad || "", apodoSeguro("a", m.id, ESTADOS.find(e => e[0] === m.estado)?.[1] || ""))).join("")}</div>`)).join("")
    || hoja(`<p class="suave" style="margin:0">Todavía no hay metas. Nacen de la Brújula de cada año.</p>`), "max-width:760px");
};

/* Pintar: en Mi ruta, el contenido al centro y la barra de vida (con todas sus opciones) a la derecha */
const pintarDetalleSinVida = pintarDetalle;
pintarDetalle = function (...args) {
  // La Brújula (Mi ruta › vida) tiene su propio riel: allí no va la barra de vida
  // Evaluación usa la misma barra: se elige el propósito, el año, el mes o el día que se quiere evaluar
  const enRuta = (estado.sec === "ruta" && estado.sel !== "vida") || estado.sec === "evaluacion";
  // En la Brújula la columna de Mi ruta no se muestra: el calendario de la vida se abre desde el riel
  const enBrujula = estado.sec === "ruta" && estado.sel === "vida", conPanel = enBrujula && !!estado.rvBrujula;
  if (enRuta && estado.sel == null) { estado.sel = estado.sec === "evaluacion" ? "m:" + iso(hoyFecha()).slice(0, 7) : "puntos"; document.getElementById("app")?.classList.add("con-detalle"); }
  const r = pintarDetalleSinVida.apply(this, args), det = $("detalle");
  document.getElementById("app")?.classList.toggle("ruta-vida", enRuta);
  document.getElementById("app")?.classList.toggle("ruta-brujula", enBrujula);
  det.classList.toggle("con-vida", enRuta || conPanel);
  if (!enRuta && !conPanel) return r;
  const l = rvLugar(); if (l.a) estado.rvUltimo = l;
  det.insertAdjacentHTML("beforeend", rvBarra());
  return r;
};

/* Teclado: Esc sube un nivel; en un día, ← → cambian de día y ↑ ↓ de semana (si no estás escribiendo) */
document.addEventListener("keydown", e => {
  if ((estado.sec !== "ruta" && estado.sec !== "evaluacion") || estado.sel === "vida" || document.querySelector(".md-visor, dialog[open], .hoja-modal")) return;
  if (/INPUT|TEXTAREA|SELECT/.test(document.activeElement?.tagName || "") || e.altKey || e.ctrlKey || e.metaKey) return;
  if (e.key === "Escape") { const arriba = rvArriba(); if (arriba) { e.preventDefault(); elegir(arriba); } return; }
  const paso = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 }[e.key], v = String(estado.sel || "");
  if (!paso || !v.startsWith("d:")) return;
  e.preventDefault();
  const f = deIso(v.slice(2)); f.setDate(f.getDate() + paso); elegir("d:" + iso(f));
});
