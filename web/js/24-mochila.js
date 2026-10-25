/* SuRoute Web · Tu mochila: los bonos del método como herramientas del senderista (igual que en la app,
 * ui/mochila/MochilaScreen.kt). Se ordenan por el momento en que sirven y aparecen donde se usan:
 * al trazar metas, al planear los días, al cerrar el mes y cuando cuesta seguir. */
"use strict";

const MOCHILA = [
  ["Para trazar tus metas", "Ábrelas mientras escribes un propósito o una meta del año.", [
    ["bono_metas", "Ideas de metas para adaptar a tu vida"],
    ["bono_indicadores", "Cómo medir cada meta, eje por eje"],
    ["bono_confluencia", "Proyectos que mueven varios ejes a la vez"]]],
  ["Para planear tus días", "Cuando armes la semana o los pasos de una meta.", [
    ["bono_acciones", "Acciones que activan varios ejes en un solo paso"]]],
  ["Para cerrar el mes", "Veinte minutos al final de cada mes.", [
    ["bono_cierre", "El repaso del mes, punto por punto"],
    ["bono_evidencias", "Haz visible tu progreso"]]],
  ["Cuando cueste seguir", "Las caídas son parte del camino.", [
    ["bono_anti_abandono", "Protocolos para volver sin culpa"]]],
];
const MOCHILA_IDS = new Set(MOCHILA.flatMap(([, , xs]) => xs.map(([id]) => id)));
const MOCHILA_METAS = ["bono_metas", "bono_indicadores", "bono_confluencia"];

const tituloBono = id => (Contenido.indice || []).find(x => x.id === id)?.title || "";
function avanceBono(id) {
  const x = (Contenido.indice || []).find(x => x.id === id); if (!x?.inputs) return "";
  const n = Store.lista("respuesta").filter(r => r.workbookId === id && !r.clave.includes("#")).length;
  return n ? `${n}/${x.inputs}` : "";
}

LISTAS.mochila = function () {
  let h = cabLista("Tu mochila", { buscar: false }) + `<div class="items">`;
  if (!Contenido.indice) { Contenido.cargarIndice(); return h + `<p class="vacio-mini">Abriendo tu mochila…</p></div>`; }
  MOCHILA.forEach(([g, , xs]) => {
    h += grupo(g);
    xs.forEach(([id, d]) => { if (tituloBono(id)) h += item("wb:" + id, av(ic("mochila"), "var(--oro)"), esc(tituloBono(id)), avanceBono(id), esc(d)); });
  });
  return h + `</div>`;
};

DET["mochila.inicio"] = () => {
  if (!Contenido.indice) Contenido.cargarIndice();
  return cab(av(ic("mochila"), "var(--burdeos)"), "Tu mochila", "Las herramientas del senderista") + cuerpo(
    hoja(`<p style="margin:0">Todo senderista lleva lo necesario para el camino. Aquí están las herramientas del método:
      ábrelas mientras construyes tus metas, cuando cierres el mes o cuando te cueste seguir. Lo que escribas en ellas se guarda.</p>`) +
    MOCHILA.map(([g, d, xs]) => hoja(`<h3 style="margin-bottom:2px">${g}</h3><p class="suave" style="margin-top:0">${d}</p>
      <div class="mochila-lista">${xs.map(([id, u]) => `<button class="mochila-op" data-sel="wb:${id}">${av(ic("mochila"), "var(--oro)")}
        <span><b>${esc(tituloBono(id) || "…")}</b><small>${esc(u)}</small></span>${avanceBono(id) ? `<span class="badge suave">${avanceBono(id)}</span>` : ""}</button>`).join("")}</div>`)).join(""),
    "max-width:720px");
};
DET["mochila.wb"] = id => DET["aprende.wb"](id);

/** «De tu mochila»: las herramientas que sirven en este momento, a un toque (por ejemplo, al trazar una meta). */
function deTuMochila(ids = MOCHILA_METAS) {
  if (!Contenido.indice) { Contenido.cargarIndice(); return ""; }
  return `<div class="mochila-fila"><span>${ic("mochila")} De tu mochila</span>${ids.filter(tituloBono).map(id =>
    `<button class="chip" data-ir="mochila" data-arg="wb:${id}">${esc(tituloBono(id))}</button>`).join("")}</div>`;
}
window.deTuMochila = deTuMochila;
window.MOCHILA_IDS = MOCHILA_IDS;
