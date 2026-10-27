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
