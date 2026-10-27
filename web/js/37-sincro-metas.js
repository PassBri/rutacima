"use strict";
/* Brújula ↔ Metas ↔ Mi ruta, sincronizadas (la misma regla que SincroBrujula.kt en la app).
 * - Brújula general: cada campamento ↔ un propósito de largo plazo de ese eje; sus 8 pasos ↔ sus "metas específicas".
 * - Brújula de un año: cada campamento ↔ la meta de ese año con ese eje; sus 8 pasos ↔ sus sub-metas.
 * - Confluencia va con lo que no tiene eje; Campamento Base no tiene metas (se queda solo en la Brújula).
 * - La Brújula manda: lo escrito en un paso pisa la sub-meta; una sub-meta en un paso vacío lo llena.
 * - Las sub-metas 1 a 8 son los pasos 1 a 8; desde la 9 se quedan solo en Metas.
 * - Si un campamento no tiene su meta (o propósito), se crea con el nombre del campamento: la Brújula llena Metas y Mi ruta. */
const Sincro = {
  CODS: ["CON", "TRA", "VAL", "EVO", "VOZ", "VOL", "MAE"],
  PASOS: 8,
  lineas: s => String(s || "").split("\n").map(x => x.trim()),
  unir(ls) { const r = [...ls]; while (r.length && !r[r.length - 1]) r.pop(); return r.join("\n"); },
  campo: anio => anio ? "subMetas" : "metasEspecificas",
  tipo: anio => anio ? "meta_anio" : "proposito",
  ejeDe: cod => cod === "CON" ? null : cod,
  /** La meta del año (o el propósito, en la general) que va con un campamento: el más antiguo con ese eje. */
  objetivo(anio, cod) {
    const eje = this.ejeDe(cod);
    return Store.lista(this.tipo(anio)).filter(o => (o.eje || null) === eje && (!anio || o.anio === anio)).sort((a, b) => a.id - b.id)[0] || null;
  },
  /** El campamento (casilla) de una meta o propósito, si su Brújula existe. */
  casillaDe(tipo, o) {
    if (!o || (tipo !== "meta_anio" && tipo !== "proposito")) return null;
    const anio = tipo === "meta_anio" ? o.anio : null, cod = o.eje || "CON";
    if (!this.CODS.includes(cod) || this.objetivo(anio, cod)?.id !== o.id) return null;
    return Mandala.repartir(casillasDe(anio)).camps[Mandala.CAMPAMENTOS_FIJOS.indexOf(cod)] || null;
  },
  /** El año y el campamento de una casilla (null si no va con metas). */
  dondeEsta(casillaId) {
    const v = Store.get("vision", casillaId); if (!v) return null;
    const anio = anioDeCasilla(v), { camps } = Mandala.repartir(casillasDe(anio));
    const i = camps.findIndex(c => c && String(c.id) === String(casillaId)), cod = Mandala.CAMPAMENTOS_FIJOS[i];
    return i >= 0 && this.CODS.includes(cod) ? { anio, cod, casilla: camps[i] } : null;
  },
  crear(anio, cod, c) {
    const titulo = (mdNombre(c) || Mandala.nombre(cod)).slice(0, 120), id = nuevoId(), eje = this.ejeDe(cod);
    if (anio) {
      const prop = this.objetivo(null, cod);
      return { id, anio, propositoId: prop?.id ?? null, titulo, subMetas: "", decision: "CONTINUAR", prioridad: "B", avance: 0, estado: "NO_INICIADA", obstaculo: "", proximaAccion: "", eje, indicador: "", observable: "" };
    }
    return { id, orden: Store.lista("proposito").length, titulo, metasEspecificas: "", prioridad: "B", eje, descripcion: "", indicadorExito: "", visualizacion: "", porQueImporta: "", impacto: "", reflexionFinal: "", progreso: 0, horizonte: 5, creadoEn: Date.now() };
  },
  /** Deja una Brújula y sus metas iguales (sin escrituras si ya lo están). */
  async reconciliar(anio) {
    const { camps } = Mandala.repartir(casillasDe(anio)), tipo = this.tipo(anio), campo = this.campo(anio);
    for (const cod of this.CODS) {
      const c = camps[Mandala.CAMPAMENTOS_FIJOS.indexOf(cod)]; if (!c) continue;
      let o = this.objetivo(anio, cod), nuevo = false;
      if (!o) { o = this.crear(anio, cod, c); nuevo = true; }
      const ls = this.lineas(o[campo]); while (ls.length < this.PASOS) ls.push("");
      for (let p = 0; p < this.PASOS; p++) {
        const t = respuesta(Mandala.clave(c.id, p)).trim();
        if (t) ls[p] = t; else if (ls[p]) await responderBase("mandala", Mandala.clave(c.id, p), ls[p]);
      }
      const txt = this.unir(ls);
      if (nuevo || txt !== this.unir(this.lineas(o[campo]))) await Store.guardar(tipo, { ...o, [campo]: txt });
    }
  },
  ocupada: false,
  async todo() {
    if (this.ocupada || !window.Mandala) return; this.ocupada = true;
    try {
      const anios = [null, ...new Set(Store.lista("vision").map(anioDeCasilla).filter(Boolean))];
      for (const a of anios) await this.reconciliar(a);
    } catch (e) { console.error(e); } finally { this.ocupada = false; }
  },
  /** Escribir (o borrar) una sub-meta: si es una de las 8 primeras de una meta con Brújula, también su paso. */
  async ponerLinea(tipo, id, i, texto) {
    const o = Store.get(tipo, id); if (!o) return;
    const campo = tipo === "meta_anio" ? "subMetas" : "metasEspecificas", ls = this.lineas(o[campo]);
    while (ls.length <= i) ls.push("");
    ls[i] = String(texto || "").trim();
    const c = this.casillaDe(tipo, o);
    if (!c && !ls[i]) ls.splice(i, 1);                 // sin Brújula, una sub-meta borrada sale de la lista
    else if (c && !ls[i] && i >= this.PASOS) ls.splice(i, 1);
    await Store.guardar(tipo, { ...o, [campo]: this.unir(ls) });
    if (c && i < this.PASOS) await responderBase("mandala", Mandala.clave(c.id, i), ls[i] || "");
  },
  /** Una sub-meta nueva: en la Brújula ocupa el primer paso vacío; si los 8 están llenos, va al final (solo en Metas). */
  async agregar(tipo, id, texto) {
    const o = Store.get(tipo, id), t = String(texto || "").trim(); if (!o || !t) return;
    const ls = this.lineas(o[tipo === "meta_anio" ? "subMetas" : "metasEspecificas"]), c = this.casillaDe(tipo, o);
    let i = c ? ls.slice(0, this.PASOS).findIndex(x => !x) : -1;
    if (c && i < 0 && ls.length < this.PASOS) i = ls.length;
    if (i < 0) i = Math.max(ls.length, c ? this.PASOS : 0);
    await this.ponerLinea(tipo, id, i, t);
  },
};
window.Sincro = Sincro;

/* Cuando se escribe un paso en la Brújula, su sub-meta cambia al instante. */
const responderBase = responder;
responder = function (wb, clave, valor) {
  const r = responderBase(wb, clave, valor), m = /^mandala#(\d+)#(\d)$/.exec(clave);
  if (m) {
    const donde = Sincro.dondeEsta(m[1]), p = Number(m[2]);
    if (donde) {
      const tipo = Sincro.tipo(donde.anio), campo = Sincro.campo(donde.anio);
      let o = Sincro.objetivo(donde.anio, donde.cod);
      if (!o && String(valor || "").trim()) o = Sincro.crear(donde.anio, donde.cod, donde.casilla);
      if (o) {
        const ls = Sincro.lineas(o[campo]); while (ls.length < Sincro.PASOS) ls.push("");
        ls[p] = String(valor || "").trim();
        const txt = Sincro.unir(ls);
        if (txt !== Sincro.unir(Sincro.lineas(o[campo])) || !Store.get(tipo, o.id)) Store.guardar(tipo, { ...o, [campo]: txt });
      }
    }
  }
  return r;
};

/* Al entrar a Metas, Mi ruta o la Brújula, todo queda al día. */
const irBase = ir;
ir = function (sec, ...rest) {
  if (["metas", "ruta", "brujula"].includes(sec)) Sincro.todo().then(() => { if (estado.sec === sec) { pintarLista(); pintarDetalle(); } });
  return irBase(sec, ...rest);
};

/** Sub-metas una por una: cada una con su número de paso, su casilla de ganado y quitar; abajo, agregar. */
function subMetasHtml(tipo, o) {
  const campo = tipo === "meta_anio" ? "subMetas" : "metasEspecificas", ls = Sincro.lineas(o[campo]), c = Sincro.casillaDe(tipo, o);
  const filas = ls.map((t, i) => {
    if (!t) return "";
    const enBrujula = c && i < Sincro.PASOS, ganado = enBrujula && MetodoCima.leer(c.id, i).cumplido;
    return `<div class="sm-fila${ganado ? " ganada" : ""}">
      ${enBrujula ? `<button class="sm-check" data-acc="mdGanado" data-arg="${c.id}|${i}" aria-pressed="${ganado}" aria-label="${ganado ? "Quitar ganado" : "Marcar ganado"}: ${esc(t)}" title="${ganado ? "Ganado · toca para quitarlo" : "Marcar como ganado"}">${ic("check")}</button>` : `<span class="sm-punto" aria-hidden="true"></span>`}
      <input class="sm-texto" value="${esc(t)}" data-acc-change="subMetaEditar" data-arg="${tipo}|${o.id}|${i}" aria-label="Sub-meta ${i + 1}" maxlength="160">
      <small class="sm-donde">${enBrujula ? `Paso ${i + 1}` : c ? "Solo en Metas" : ""}</small>
      <button class="sm-quitar" data-acc="subMetaQuitar" data-arg="${tipo}|${o.id}|${i}" aria-label="Quitar: ${esc(t)}" title="Quitar">${ic("cerrar")}</button></div>`;
  }).join("");
  const llenas = ls.filter(Boolean).length;
  return `<div class="sm">
    ${filas || `<p class="suave sm-vacio">Todavía no hay sub-metas.${c ? " Cada una que agregues ocupa un paso de su campamento en la Brújula." : ""}</p>`}
    <form class="sm-nueva" data-form="subMetaNueva" data-arg="${tipo}|${o.id}"><input name="texto" placeholder="Nueva sub-meta" aria-label="Nueva sub-meta" autocomplete="off" maxlength="160">
      <button class="btn mini" aria-label="Agregar sub-meta">${ic("mas")} Agregar</button></form>
    ${c ? `<p class="suave sm-nota">${ic("brujula")} Sincronizada con ${esc(Mandala.nombre(o.eje || "CON"))} en tu Brújula ${tipo === "meta_anio" ? "de " + o.anio : "general"}${llenas > Sincro.PASOS ? ": las que pasan de 8 se quedan solo aquí" : ""}.</p>` : ""}</div>`;
}
window.subMetasHtml = subMetasHtml;

ACC.subMetaEditar = (el, arg) => { const [tipo, id, i] = arg.split("|"); Sincro.ponerLinea(tipo, id, Number(i), el.value).then(() => pintarLista()); };
ACC.subMetaQuitar = async (el, arg) => { const [tipo, id, i] = arg.split("|"); await Sincro.ponerLinea(tipo, id, Number(i), ""); pintarDetalle(); pintarLista(); };
(window.FORMULARIOS ||= {}).subMetaNueva = async (f, _d, texto) => {
  if (!texto) return;
  const [tipo, id] = f.dataset.arg.split("|");
  await Sincro.agregar(tipo, id, texto); pintarDetalle(); pintarLista();
  requestAnimationFrame(() => document.querySelector(`form[data-form="subMetaNueva"][data-arg="${tipo}|${id}"] input`)?.focus());
};

/* Rehacer Metas desde la Brújula: guarda un respaldo de lo que había, lo quita y lo vuelve a crear con la Brújula. */
ACC.rehacerMetas = async el => {
  if (!el.dataset.seguro) {
    el.dataset.seguro = "1"; const antes = el.innerHTML; el.innerHTML = "¿Seguro? Se quitan tus metas y propósitos actuales. Toca otra vez";
    setTimeout(() => { if (el.isConnected) { delete el.dataset.seguro; el.innerHTML = antes; } }, 5000);
    return;
  }
  const props = Store.lista("proposito"), metas = Store.lista("meta_anio"), acciones = Store.lista("accion");
  const respaldo = { fecha: new Date().toISOString(), proposito: props, meta_anio: metas, accion: acciones };
  try {
    const a = document.createElement("a");
    a.href = URL.createObjectURL(new Blob([JSON.stringify(respaldo, null, 2)], { type: "application/json" }));
    a.download = `SuRoute_respaldo_metas_${iso(new Date())}.json`; a.click(); setTimeout(() => URL.revokeObjectURL(a.href), 4000);
  } catch { /* sin descarga, igual sigue */ }
  const quitadas = new Set(metas.map(m => m.id));
  for (const x of acciones) await Store.borrar("accion", x.id);
  for (const x of metas) await Store.borrar("meta_anio", x.id);
  for (const x of props) await Store.borrar("proposito", x.id);
  for (const x of Store.lista("meta_mes")) if (x.metaAnualId != null && quitadas.has(x.metaAnualId)) await Store.guardar("meta_mes", { ...x, metaAnualId: null });
  await Sincro.todo();
  estado.sel = null; toast(`Listo: ${Store.lista("proposito").length} propósitos y ${Store.lista("meta_anio").length} metas desde tu Brújula. Se descargó un respaldo de lo anterior.`);
  refrescar();
};
