"use strict";
/* Mi ruta con la barra de vida (igual que la app): el contenido va en el centro y los puntos de vida en una barra
 * a la derecha (arriba en el teléfono). Clic en un año: aparecen sus meses; clic en un mes: aparecen sus días.
 * Se vuelve con las migas (← Vida › 2026 › Octubre › 27), con Esc o tocando otra vez el año o el mes abierto. */
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

function rvMigas() {
  const l = rvLugar(), v = String(estado.sel || ""), arriba = rvArriba();
  const parte = (sel, txt, actual) => actual ? `<b aria-current="page">${txt}</b>` : `<button data-sel="${sel}">${txt}</button>`;
  const ps = [parte("puntos", "Vida", v === "puntos")];
  if (l.a) ps.push(parte(`a:${l.a}`, l.a, v === `a:${l.a}`));
  if (l.m) ps.push(parte(`m:${l.a}-${pad2(l.m)}`, mayus(MESES[l.m - 1]), v === `m:${l.a}-${pad2(l.m)}`));
  if (l.d) ps.push(parte(`d:${l.a}-${pad2(l.m)}-${pad2(l.d)}`, l.d, v.startsWith("d:")));
  const extra = v === "props" ? "Propósitos" : v === "conmetas" ? "Metas por año" : /^p:/.test(v) ? "Propósito" : /^ma:/.test(v) ? "Meta del año" : /^mm:/.test(v) ? "Meta del mes" : "";
  return `<nav class="rv-migas" aria-label="Dónde estás">
    <button class="rv-atras" ${arriba ? `data-sel="${arriba}"` : "disabled"} aria-label="Subir un nivel (Esc)" title="Subir un nivel (Esc)">${ic("volver")}</button>
    <span class="rv-pasos">${ps.join(`<i aria-hidden="true">›</i>`)}${extra ? `<i aria-hidden="true">›</i><b>${extra}</b>` : ""}</span>
    <span class="rv-atajos"><button class="chip" data-sel="props" aria-pressed="${v === "props"}">${ic("brujula")} Propósitos</button><button class="chip" data-sel="conmetas" aria-pressed="${v === "conmetas"}">${ic("metas")} Metas por año</button></span></nav>`;
}

/** La barra de vida: un punto por año; el año abierto despliega sus meses, y el mes abierto sus días. */
function rvBarra() {
  const p = perfil(), nac = p.anioNacimiento; if (!nac) return "";
  const hoy = hoyFecha(), aHoy = hoy.getFullYear(), ultimo = nac + metaVida(), l = rvLugar();
  const conMetas = new Set(Store.lista("meta_anio").map(m => m.anio));
  let h = "";
  for (let a = nac; a <= ultimo; a++) {
    const abierto = l.a === a, cls = `${a < aHoy ? "vivido" : a === aHoy ? "hoy" : "futuro"}${conMetas.has(a) ? " con-metas" : ""}${abierto ? " abierto" : ""}`;
    h += `<button class="rv-anio ${cls}" data-sel="${abierto && !l.m ? "puntos" : `a:${a}`}" aria-expanded="${abierto}" aria-label="${a}, ${a - nac} años${conMetas.has(a) ? ", con metas" : ""}"${a === aHoy ? ' data-hoy="1"' : ""}>
      <i aria-hidden="true"></i><span>${a}</span><small>${a - nac}</small></button>`;
    if (!abierto) continue;
    h += `<div class="rv-meses" role="group" aria-label="Meses de ${a}">`;
    for (let m = 1; m <= 12; m++) {
      const mAb = l.m === m, esHoy = a === aHoy && m === hoy.getMonth() + 1, n = metasDelMes(a, m).length;
      h += `<button class="rv-mes${mAb ? " abierto" : ""}${esHoy ? " hoy" : ""}" data-sel="${mAb && !l.d ? `a:${a}` : `m:${a}-${pad2(m)}`}" aria-expanded="${mAb}" aria-label="${MESES[m - 1]} de ${a}${n ? `, ${n} metas` : ""}">${MES_CORTO[m - 1]}${n ? `<sup>${n}</sup>` : ""}</button>`;
      if (!mAb) continue;
      h += `<div class="rv-dias" role="group" aria-label="Días de ${MESES[m - 1]}">`;
      for (let d = 1; d <= diasEnMes(a, m); d++) {
        const f = new Date(a, m - 1, d), hecho = f <= hoy && diaCumplido(f), esHoyD = f.getTime() === hoy.getTime();
        h += `<button class="rv-dia${l.d === d ? " abierto" : ""}${hecho ? " hecho" : ""}${esHoyD ? " hoy" : ""}" data-sel="d:${a}-${pad2(m)}-${pad2(d)}" aria-label="${d} de ${MESES[m - 1]}">${d}</button>`;
      }
      h += `</div>`;
    }
    h += `</div>`;
  }
  return `<nav class="rv-barra" aria-label="Tu vida en años">${h}</nav>`;
}

/* Vistas del centro para lo que antes estaba en la lista de la izquierda */
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

let rvSalto = true, rvSelPrevio = null;
/* Pintar: en Mi ruta, el centro lleva las migas arriba y la barra de vida a la derecha */
const pintarDetalleSinVida = pintarDetalle;
pintarDetalle = function (...args) {
  const enRuta = estado.sec === "ruta";
  if (enRuta && estado.sel == null) { estado.sel = "puntos"; document.getElementById("app")?.classList.add("con-detalle"); }
  const viejo = document.querySelector(".rv-barra"), scroll = viejo ? [viejo.scrollTop, viejo.scrollLeft] : null;
  const r = pintarDetalleSinVida.apply(this, args), det = $("detalle");
  document.getElementById("app")?.classList.toggle("ruta-vida", enRuta);
  det.classList.toggle("con-vida", enRuta);
  if (!enRuta) return r;
  const l = rvLugar(); if (l.a) estado.rvUltimo = l;
  det.insertAdjacentHTML("afterbegin", rvMigas());
  det.insertAdjacentHTML("beforeend", rvBarra());
  const barra = det.querySelector(".rv-barra");
  if (barra) {
    const foco = barra.querySelector(".rv-dia.abierto, .rv-mes.abierto, .rv-anio.abierto") || barra.querySelector('[data-hoy="1"]');
    const mismo = rvSelPrevio === estado.sel; rvSelPrevio = estado.sel;
    if (scroll != null && (!rvSalto || mismo)) { barra.scrollTop = scroll[0]; barra.scrollLeft = scroll[1]; }
    else if (foco) {
      const rb = barra.getBoundingClientRect(), rf = foco.getBoundingClientRect(), vertical = barra.scrollHeight > barra.clientHeight + 4;
      if (vertical) barra.scrollTop += rf.top - rb.top - barra.clientHeight / 3; else barra.scrollLeft += rf.left - rb.left - barra.clientWidth / 4;
    }
    rvSalto = false;
  }
  return r;
};
/* Al elegir algo de otro lugar (no de la barra), la barra salta a ese año, mes o día */
document.addEventListener("click", e => { if (e.target.closest("[data-sel]") && !e.target.closest(".rv-barra")) rvSalto = true; }, true);

/* Esc sube un nivel (si no estás escribiendo) */
document.addEventListener("keydown", e => {
  if (e.key !== "Escape" || estado.sec !== "ruta" || document.querySelector(".md-visor, dialog[open], .hoja-modal")) return;
  if (/INPUT|TEXTAREA|SELECT/.test(document.activeElement?.tagName || "")) return;
  const arriba = rvArriba(); if (!arriba) return;
  e.preventDefault(); rvSalto = true; elegir(arriba);
});
