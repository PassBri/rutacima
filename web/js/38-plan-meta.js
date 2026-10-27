"use strict";
/* Plan de acción de cada meta del año (igual que PlanMeta en la app): los pasos que quieras, cada uno con su casilla.
 * Se guarda como respuesta "plan:a:<id>" (libro "metas") con [{t, h}], así viaja igual entre la web y la app. */
const PlanMeta = {
  clave: id => `plan:a:${id}`,
  leer(id) {
    try { const v = JSON.parse(respuesta(this.clave(id)) || "[]"); return Array.isArray(v) ? v.filter(x => x && typeof x.t === "string").map(x => ({ t: x.t, h: !!x.h })) : []; }
    catch { return []; }
  },
  guardar(id, ps) { return responder("metas", this.clave(id), ps.length ? JSON.stringify(ps) : ""); },
};
window.PlanMeta = PlanMeta;

function planMetaHtml(m) {
  const ps = PlanMeta.leer(m.id), hechos = ps.filter(x => x.h).length;
  return hoja(`<div class="cab-lista-btn"><div class="etiqueta">Plan de acción${ps.length ? ` · ${hechos} de ${ps.length}` : ""}</div></div>
    ${ps.length ? `<div class="pm-barra" role="img" aria-label="${hechos} de ${ps.length} pasos hechos"><i style="width:${Math.round(hechos * 100 / ps.length)}%"></i></div>` : ""}
    <div class="sm">${ps.map((x, i) => `<div class="sm-fila${x.h ? " ganada" : ""}">
      <button class="sm-check" data-acc="planMetaHecho" data-arg="${m.id}|${i}" aria-pressed="${x.h}" aria-label="${x.h ? "Desmarcar" : "Marcar hecho"}: ${esc(x.t)}">${ic("check")}</button>
      <input class="sm-texto" value="${esc(x.t)}" data-acc-change="planMetaEditar" data-arg="${m.id}|${i}" aria-label="Paso ${i + 1} del plan" maxlength="200">
      <small class="sm-donde">${i + 1}</small>
      <button class="sm-quitar" data-acc="planMetaQuitar" data-arg="${m.id}|${i}" aria-label="Quitar: ${esc(x.t)}" title="Quitar">${ic("cerrar")}</button></div>`).join("")
      || `<p class="suave sm-vacio">Sin pasos todavía. Agrega los que necesites para lograr esta meta.</p>`}
    <form class="sm-nueva" data-form="planMetaNuevo" data-arg="${m.id}"><input name="texto" placeholder="Nuevo paso del plan" aria-label="Nuevo paso del plan" autocomplete="off" maxlength="200">
      <button class="btn mini" aria-label="Agregar paso">${ic("mas")} Agregar</button></form></div>`);
}
window.planMetaHtml = planMetaHtml;

const planCambiar = async (id, f) => { const ps = PlanMeta.leer(id); f(ps); await PlanMeta.guardar(id, ps); pintarDetalle(); };
ACC.planMetaHecho = (_, arg) => { const [id, i] = arg.split("|"); planCambiar(id, ps => { if (ps[i]) ps[i].h = !ps[i].h; }); };
ACC.planMetaQuitar = (_, arg) => { const [id, i] = arg.split("|"); planCambiar(id, ps => ps.splice(Number(i), 1)); };
ACC.planMetaEditar = (el, arg) => {
  const [id, i] = arg.split("|"), t = el.value.trim();
  planCambiar(id, ps => { if (!ps[i]) return; if (t) ps[i].t = t; else ps.splice(Number(i), 1); });
};
(window.FORMULARIOS ||= {}).planMetaNuevo = async (f, _d, texto) => {
  if (!texto) return;
  const id = f.dataset.arg;
  await planCambiar(id, ps => ps.push({ t: texto.slice(0, 200), h: false }));
  requestAnimationFrame(() => document.querySelector(`form[data-form="planMetaNuevo"][data-arg="${id}"] input`)?.focus());
};

/* Orden de mayor a menor: propósitos (Brújula general) → metas del año (Brújula del año) → metas del mes (las del año que
 * se trabajan ese mes) → acciones del día (cada una aporta a una de esas metas). Igual que la app. */
function focoMes(a, m) {
  const delMes = metasDelMes(a, m), anuales = Store.lista("meta_anio").filter(x => x.anio === a).sort((x, y) => (x.prioridad || "B").localeCompare(y.prioridad || "B") || x.id - y.id);
  const elegidas = anuales.filter(x => delMes.some(y => y.metaAnualId === x.id)).length;
  const fila = x => {
    const mm = delMes.find(y => y.metaAnualId === x.id), eje = ejeEfectivo("meta_anio", x);
    return `<div class="foco${mm ? " sel" : ""}"><label class="foco-cab"><input type="checkbox" data-acc-change="focoMes" data-arg="${a}|${m}|${x.id}" ${mm ? "checked" : ""}>
        <i class="eje-punto" style="background:${COLOR_EJE[eje] || "#7A4A6B"}" title="${esc(NOMBRE_EJE[eje] || "Confluencia")}"></i><span>${esc(x.titulo)}</span></label>
      ${mm ? `<button class="pct" data-ir="metas" data-arg="m:${mm.id}" title="Abrir la meta del mes">${pctTxt(avanceMetaMes(mm))}</button>` : `<button class="suave foco-ir" data-ir="metas" data-arg="a:${x.id}">Ver</button>`}</div>
      ${mm ? tiraDias(mm) : ""}`;
  };
  return hoja(`<div class="cab-lista-btn"><div class="etiqueta">¿Qué metas de ${a} trabajo en ${MESES[m - 1]}?${anuales.length ? ` · ${elegidas} de ${anuales.length}` : ""}</div></div>
    <p class="suave" style="margin:2px 0 10px">Marca las metas del año que trabajarás este mes. En cada día, tus acciones aportan a una de ellas y los días se marcan aquí.</p>
    ${anuales.map(fila).join("") || `<p class="suave" style="margin:0">Aún no hay metas de ${a}. Nacen de tu Brújula de ${a}.</p>
      <p style="margin:8px 0 0"><button class="btn mini" data-acc="abrirBrujulaAnio" data-arg="${a}">${ic("brujula")} Brújula de ${a}</button></p>`}`);
}
window.focoMes = focoMes;

ACC.focoMes = async (el, arg) => {
  const [a, m, id] = arg.split("|").map(Number), mm = metasDelMes(a, m).find(y => y.metaAnualId === id), x = Store.get("meta_anio", id);
  if (el.checked) {
    if (!mm && x) await Store.guardar("meta_mes", { id: nuevoId(), anio: a, mes: m, orden: metasDelMes(a, m).length, texto: x.titulo, dias: "", cumplida: false, metaAnualId: id, eje: null, indicador: "", objetivoDias: 20 });
  } else if (mm) {
    if (numeros(mm.dias).length && !el.dataset.seguro) {
      el.checked = true; el.dataset.seguro = "1";
      toast(`“${x?.titulo || mm.texto}” ya tiene ${numeros(mm.dias).length} días marcados este mes. Desmárcala otra vez para quitarla.`);
      setTimeout(() => { delete el.dataset.seguro; }, 6000);
      return;
    }
    await Store.borrar("meta_mes", mm.id);
  }
  pintarDetalle(); pintarLista();
};

/** Lo que aporta una acción del día: las metas del año escogidas para el mes (con el nombre de su meta del año). */
function opcionesAporte(ms, actual) {
  const nombre = x => Store.get("meta_anio", x.metaAnualId)?.titulo || x.texto;
  return `<option value="">Aporta a…</option>${ms.map(x => `<option value="${x.id}" ${String(x.id) === String(actual) ? "selected" : ""}>${esc(nombre(x))}</option>`).join("")}`;
}
window.opcionesAporte = opcionesAporte;

/* Una sola vez: las metas del mes sueltas (sin meta del año) eran de prueba y salen. */
async function limpiarMesSueltas() {
  if (respuesta("migra#mes-sueltas")) return;
  const anuales = new Set(Store.lista("meta_anio").map(x => x.id));
  for (const x of Store.lista("meta_mes")) if (x.metaAnualId == null || !anuales.has(x.metaAnualId)) await Store.borrar("meta_mes", x.id);
  await responder("migra", "migra#mes-sueltas", "1");
}
window.limpiarMesSueltas = limpiarMesSueltas;

/* Metas vive dentro de Mi ruta: propósitos arriba, cada año con sus metas, el mes con su foco y el día con sus acciones.
 * Los enlaces de siempre (ir("metas", …)) llegan a Mi ruta: p: propósito, a: meta del año → ma:, m: meta del mes → mm:. */
DET["ruta.p"] = arg => DET["metas.p"](arg);
DET["ruta.ma"] = arg => DET["metas.a"](arg);
DET["ruta.mm"] = arg => DET["metas.m"](arg);
DET["ruta.nuevo"] = () => DET["metas.nuevo"]();
const irSinMetas = ir;
ir = function (sec, sel = null, ...rest) {
  if (sec === "metas") {
    const [t, ...r] = String(sel ?? "").split(":"), id = r.join(":");
    const nuevo = sel == null ? null : t === "p" ? "p:" + id : t === "a" ? "ma:" + id : t === "m" ? "mm:" + id : sel;
    irSinMetas("ruta", nuevo, ...rest);
    if (sel == null) { estado.filtro = "prop"; pintarLista(); }
    return;
  }
  return irSinMetas(sec, sel, ...rest);
};
