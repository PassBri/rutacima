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

/** La barra de vida a la derecha (arriba en el teléfono), con todo: subir un nivel, dónde estás, Propósitos y Metas por año,
 *  y los mismos círculos del mapa de vida, uno debajo del otro: años; el año abierto, sus meses; el mes abierto, sus días. */
function rvBarra() {
  const p = perfil(), nac = p.anioNacimiento; if (!nac) return "";
  const hoy = hoyFecha(), aHoy = hoy.getFullYear(), mHoy = hoy.getMonth() + 1, ultimo = nac + metaVida() - 1, l = rvLugar();
  const v = String(estado.sel || ""), arriba = rvArriba();
  const conMetas = new Set(Store.lista("meta_anio").map(m => m.anio));
  const aro = (cls, dentro = "") => `<span class="pv ${cls}" aria-hidden="true">${PUNTO_MARCA}${dentro}</span>`;
  const donde = l.d ? `${l.d} ${MES_CORTO[l.m - 1]} ${l.a}` : l.m ? `${mayus(MESES[l.m - 1])} ${l.a}` : l.a ? `${l.a}` : v === "props" ? "Propósitos" : v === "conmetas" ? "Metas por año" : "Vida";
  let h = "";
  for (let a = nac; a <= ultimo; a++) {
    const abierto = l.a === a, cls = a < aHoy ? "vivido" : a === aHoy ? "actual" : "futuro";
    h += `<button class="rv-fila rv-anio${abierto ? " abierto" : ""}" data-sel="${abierto && !l.m ? "puntos" : `a:${a}`}" aria-expanded="${abierto}" aria-label="${a}, ${a - nac} años${conMetas.has(a) ? ", con metas" : ""}"${a === aHoy ? ' data-hoy="1"' : ""}>
      ${aro(cls + (conMetas.has(a) ? " meta" : ""), cls === "actual" ? `<b>${a - nac}</b>` : "")}<span class="rv-et">${a}<small>${a - nac} años</small></span></button>`;
    if (!abierto) continue;
    h += `<div class="rv-hijos" role="group" aria-label="Meses de ${a}">`;
    for (let m = 1; m <= 12; m++) {
      const mAb = l.m === m, n = metasDelMes(a, m).length;
      const cm = a < aHoy || (a === aHoy && m < mHoy) ? "vivido" : a === aHoy && m === mHoy ? "actual" : "futuro";
      h += `<button class="rv-fila rv-mes${mAb ? " abierto" : ""}" data-sel="${mAb && !l.d ? `a:${a}` : `m:${a}-${pad2(m)}`}" aria-expanded="${mAb}" aria-label="${MESES[m - 1]} de ${a}${n ? `, ${n} metas` : ""}">
        ${aro(cm + (n ? " meta" : ""))}<span class="rv-et">${mayus(MESES[m - 1])}${n ? `<small>${n} ${n === 1 ? "meta" : "metas"}</small>` : ""}</span></button>`;
      if (!mAb) continue;
      h += `<div class="rv-hijos rv-dias" role="group" aria-label="Días de ${MESES[m - 1]}">`;
      for (let d = 1; d <= diasEnMes(a, m); d++) {
        const f = new Date(a, m - 1, d), cd = f < hoy ? "vivido" : f.getTime() === hoy.getTime() ? "actual" : "futuro", hecho = f <= hoy && diaCumplido(f);
        h += `<button class="rv-fila rv-dia${l.d === d ? " abierto" : ""}" data-sel="d:${a}-${pad2(m)}-${pad2(d)}" aria-label="${d} de ${MESES[m - 1]}${hecho ? ", cumplido" : ""}">
          ${aro(cd + (hecho ? " meta" : ""), `<b>${d}</b>`)}<span class="rv-et">${["dom", "lun", "mar", "mié", "jue", "vie", "sáb"][f.getDay()]}</span></button>`;
      }
      h += `</div>`;
    }
    h += `</div>`;
  }
  return `<nav class="rv-barra" aria-label="Tu vida en años">
    <div class="rv-cab">
      <div class="rv-cab-fila"><button class="rv-atras" ${arriba ? `data-sel="${arriba}"` : "disabled"} aria-label="Subir un nivel (Esc)" title="Subir un nivel (Esc)">${ic("volver")}</button><b class="rv-donde" title="Dónde estás">${donde}</b></div>
      <div class="rv-cab-fila"><button class="rv-ic" data-sel="props" aria-pressed="${v === "props"}" aria-label="Propósitos" title="Propósitos">${ic("brujula")}</button><button class="rv-ic" data-sel="conmetas" aria-pressed="${v === "conmetas"}" aria-label="Metas por año" title="Metas por año">${ic("metas")}</button><button class="rv-ic" data-sel="puntos" aria-pressed="${v === "puntos"}" aria-label="Mapa de mi vida" title="Mapa de mi vida">${ic("puntos")}</button></div>
    </div>
    <div class="rv-lista">${h}</div></nav>`;
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
/* Pintar: en Mi ruta, el contenido al centro y la barra de vida (con todas sus opciones) a la derecha */
const pintarDetalleSinVida = pintarDetalle;
pintarDetalle = function (...args) {
  // La Brújula (Mi ruta › vida) tiene su propio riel: allí no va la barra de vida
  const enRuta = estado.sec === "ruta" && estado.sel !== "vida";
  if (enRuta && estado.sel == null) { estado.sel = "puntos"; document.getElementById("app")?.classList.add("con-detalle"); }
  const viejo = document.querySelector(".rv-lista"), scroll = viejo ? [viejo.scrollTop, viejo.scrollLeft] : null;
  const r = pintarDetalleSinVida.apply(this, args), det = $("detalle");
  document.getElementById("app")?.classList.toggle("ruta-vida", enRuta);
  det.classList.toggle("con-vida", enRuta);
  if (!enRuta) return r;
  const l = rvLugar(); if (l.a) estado.rvUltimo = l;
  det.insertAdjacentHTML("beforeend", rvBarra());
  const barra = det.querySelector(".rv-lista");
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
