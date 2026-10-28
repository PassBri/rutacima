"use strict";
/* La cadena completa, de lo más grande a lo más pequeño (igual que la app):
 * 1. Propósitos (Brújula general) con su período: desde un año hasta otro.
 * 2. El año ES la Brújula de ese año: la cumbre, los 8 campamentos, cada uno con su meta, sus sub-metas y su plan de acción.
 * 3. El mes escoge qué metas trabaja, completas o solo algunas de sus sub-metas.
 * 4. El día: cada actividad aporta a una meta o a una sub-meta; al hacerla cuenta como una jornada del Método Cima
 *    y se ve cuánto aporta (una jornada = 1 / jornadas del paso; un paso = 1/8 de la meta). */

/* ---------- 1. Período del propósito ---------- */
const clavePeriodo = id => `periodo:p:${id}`;
function desdeProposito(p) { const v = Number(respuesta(clavePeriodo(p.id))); return v || (p.creadoEn ? new Date(p.creadoEn).getFullYear() : hoyFecha().getFullYear()); }
aniosDeCumbre = function (p) { const ini = desdeProposito(p); return { ini, fin: ini + (p.horizonte || 5) - 1 }; };
const selectorHorizonteBase = selectorHorizonte;
selectorHorizonte = function (tipo, clave, anios) {
  const p = tipo === "proposito" ? Store.get("proposito", clave) : null; if (!p) return selectorHorizonteBase(tipo, clave, anios);
  const desde = desdeProposito(p), hasta = desde + anios - 1, arg = `proposito|${esc(clave)}`;
  return `<div class="campo periodo" role="group" aria-label="Período del propósito"><span>Período</span>
    <div class="per-fila"><label>Desde <input type="number" inputmode="numeric" min="1900" max="2200" value="${desde}" data-acc-change="perDesde" data-arg="${esc(clave)}" aria-label="Año en que empieza"></label>
      <span aria-hidden="true">–</span><label>Hasta <input type="number" inputmode="numeric" min="1900" max="2250" value="${hasta}" data-acc-change="perHasta" data-arg="${esc(clave)}" aria-label="Año en que termina"></label>
      <b>${anios} años</b></div>
    <div class="atajos">${HORIZONTES.map(h => `<button type="button" class="chip" aria-pressed="${h === anios}" data-acc="horizonte" data-arg="${arg}|=${h}">${h} años</button>`).join("")}</div></div>`;
};
ACC.perDesde = async (el, id) => { const v = Math.round(Number(el.value)); if (v >= 1900 && v <= 2200) await responder("metas", clavePeriodo(id), String(v)); pintarDetalle(); };
ACC.perHasta = async (el, id) => {
  const p = Store.get("proposito", id); if (!p) return;
  const h = Math.min(HORIZONTE_MAX, Math.max(HORIZONTE_MIN, Math.round(Number(el.value)) - desdeProposito(p) + 1));
  await Store.cambiar("proposito", id, { horizonte: h }); pintarDetalle();
};

/* ---------- Jornadas de un paso (de cualquier Brújula, no solo la abierta) ---------- */
const reqPaso = (cid, p) => MetodoCima.leer(cid, p).req || MetodoCima.INICIAL;
/** Cuánto aporta una jornada de ese paso a su meta, en %. */
const aportePaso = (cid, p) => 100 / (reqPaso(cid, p) * 8);
async function jornadaPaso(cid, p, fecha) {
  const x = MetodoCima.leer(cid, p); if (x.cumplido) return;
  const req = x.req || MetodoCima.INICIAL, js = [...new Set([...x.jornadas, fecha])].sort();
  await responderBase("mandala", MetodoCima.claveJornadas(cid, p), js.join(","));
  await responderBase("mandala", MetodoCima.claveReq(cid, p), String(req));
  if (js.length >= req) { await responderBase("mandala", Mandala.claveHecho(cid, p), fecha); toast("¡Paso ganado! Tu meta subió un octavo."); }
}
/** La casilla (campamento) de una meta del mes, a través de su meta del año. */
const casillaDeMes = mm => { const a = mm && Store.get("meta_anio", mm.metaAnualId); return a ? Sincro.casillaDe("meta_anio", a) : null; };

/* ---------- 2. El año es su Brújula ---------- */
function brujulaDelAnio(a) {
  const cs = casillasDe(a);
  if (!cs.length) return hoja(`<h3 style="margin:0 0 4px">Brújula de ${a}</h3><p class="suave" style="margin:0 0 10px">Las metas de ${a} nacen de su Brújula: la cumbre del año y sus 8 campamentos, cada uno con sus sub-metas.</p>
    <div class="botones"><button class="btn lleno" data-acc="abrirBrujulaAnio" data-arg="${a}">${ic("brujula")} Crear la Brújula de ${a}</button>
    ${casillasDe(null).length ? `<button class="btn" data-acc="copiarGeneral" data-arg="${a}">${ic("copiar")} Copiar de la Brújula general</button>` : ""}</div>`);
  const { cumbre, camps } = Mandala.repartir(cs), hoyF = hoyFecha();
  const tarjeta = (c, i) => {
    const cod = Mandala.CAMPAMENTOS_FIJOS[i], color = Mandala.COLORES[cod] || COLOR_EJE[cod] || "#7A4A6B";
    if (!c) return `<div class="ba-camp vacio" style="--c:${color}"><small>${esc(Mandala.nombre(cod))}</small><p class="suave">Sin campamento en la Brújula de ${a}.</p></div>`;
    const m = Sincro.objetivo(a, cod), pasos = [0, 1, 2, 3, 4, 5, 6, 7].map(p => ({ p, t: respuesta(Mandala.clave(c.id, p)).trim(), x: MetodoCima.leer(c.id, p) }));
    const ganados = pasos.filter(x => x.x.cumplido).length, plan = m ? PlanMeta.leer(m.id) : [];
    return `<article class="ba-camp" style="--c:${color}">
      <header><small>${esc(Mandala.nombre(cod))}</small><span class="ba-pct">${ganados}/8</span></header>
      ${m ? `<button class="ba-titulo" data-sel="ma:${m.id}" title="Abrir la meta">${esc(m.titulo)}</button>` : `<b class="ba-titulo">${esc(mdNombre(c))}</b>`}
      <div class="ba-barra"><i style="width:${ganados / 8 * 100}%"></i></div>
      <ol class="ba-pasos">${pasos.map(({ p, t, x }) => t ? `<li class="${x.cumplido ? "ganado" : ""}"><button class="ba-check" data-acc="baGanado" data-arg="${c.id}|${p}" aria-pressed="${x.cumplido}" aria-label="${x.cumplido ? "Quitar ganado" : "Marcar ganado"}: ${esc(t)}">${ic("check")}</button><span>${esc(t)}</span>${!x.cumplido && x.jornadas.length ? `<small>${x.jornadas.length}/${x.req || MetodoCima.INICIAL}</small>` : ""}</li>` : "").join("")
        || `<li class="suave">Sin sub-metas todavía: escríbelas en la Brújula o en la meta.</li>`}</ol>
      ${m ? `<details class="ba-plan"${estado.baPlan === m.id ? " open" : ""}><summary data-acc="baPlan" data-arg="${m.id}">${ic("lista")} Plan de acción${plan.length ? ` · ${plan.filter(x => x.h).length} de ${plan.length}` : ""}</summary>${planMetaHtml(m, true)}</details>` : ""}
    </article>`;
  };
  return hoja(`<div class="ba-cab"><div><small>Brújula de ${a}</small><h3>${esc(cumbre?.afirmacion || cumbre?.titulo || `Mi cumbre de ${a}`)}</h3></div>
      <span class="ba-acc">${casillasDe(null).length ? `<button class="btn mini" data-acc="copiarGeneral" data-arg="${a}" title="Trae la cumbre, los campamentos y sus sub-metas de tu Brújula general">${ic("copiar")} Copiar de la general</button>` : ""}
      <button class="btn mini" data-acc="abrirBrujulaAnio" data-arg="${a}">${ic("brujula")} Abrir la Brújula</button></span></div>
    <p class="suave" style="margin:0 0 12px">La cumbre de ${a} y sus 8 campamentos: cada uno con su meta, sus sub-metas (los pasos) y su plan de acción.</p>
    <div class="ba-grid">${Mandala.CAMPAMENTOS_FIJOS.map((_, i) => tarjeta(camps[i], i)).join("")}</div>`, "ba-hoja");
}
window.brujulaDelAnio = brujulaDelAnio;
ACC.baPlan = (_, id) => { estado.baPlan = estado.baPlan === Number(id) ? null : Number(id); };
ACC.baGanado = async (_, arg) => {
  const [cid, n] = arg.split("|"), p = Number(n), x = MetodoCima.leer(Number(cid), p);
  if (x.cumplido) { await responderBase("mandala", Mandala.claveHecho(Number(cid), p), ""); const req = x.req || MetodoCima.INICIAL;
    if (x.jornadas.length >= req) await responderBase("mandala", MetodoCima.claveJornadas(Number(cid), p), [...x.jornadas].sort().slice(0, req - 1).join(",")); }
  else { await responderBase("mandala", Mandala.claveHecho(Number(cid), p), iso(new Date())); toast("¡Sub-meta ganada!"); }
  pintarDetalle();
};

/* ---------- 3. El mes: completas o solo algunas sub-metas ---------- */
const claveMesPasos = id => `mes:pasos:${id}`;
const pasosDelMes = mm => numeros(respuesta(claveMesPasos(mm.id)));
function subMetasMes(mm) {
  const c = casillaDeMes(mm); if (!c) return "";
  const elegidos = new Set(pasosDelMes(mm)), todos = [0, 1, 2, 3, 4, 5, 6, 7].filter(p => respuesta(Mandala.clave(c.id, p)).trim());
  if (!todos.length) return "";
  return `<div class="fm-pasos"><small>${elegidos.size ? `${elegidos.size} de ${todos.length} sub-metas este mes` : "Completa: todas sus sub-metas"} · toca para escoger solo algunas</small>
    <div class="fm-chips">${todos.map(p => { const t = respuesta(Mandala.clave(c.id, p)).trim(), g = MetodoCima.leer(c.id, p).cumplido;
      return `<button class="chip${g ? " ganado" : ""}" data-acc="mesPaso" data-arg="${mm.id}|${p}" aria-pressed="${elegidos.has(p)}" title="${esc(t)}">${g ? ic("check") : ""}${p + 1}. ${esc(t.length > 34 ? t.slice(0, 32) + "…" : t)}</button>`; }).join("")}</div></div>`;
}
ACC.mesPaso = async (_, arg) => {
  const [id, n] = arg.split("|"), mm = Store.get("meta_mes", id); if (!mm) return;
  const s = new Set(pasosDelMes(mm)), p = Number(n); s.has(p) ? s.delete(p) : s.add(p);
  await responder("metas", claveMesPasos(id), [...s].sort((x, y) => x - y).join(",")); pintarDetalle();
};
const focoMesBase = focoMes;
focoMes = function (a, m) {
  // Debajo de cada meta escogida, sus sub-metas para escoger solo algunas este mes
  let h = focoMesBase(a, m);
  for (const mm of metasDelMes(a, m)) {
    const extra = subMetasMes(mm); if (!extra) continue;
    h = h.replace(`data-ir="metas" data-arg="m:${mm.id}"`, `data-ir="metas" data-arg="m:${mm.id}" data-fm="${mm.id}"`);
    const marca = `data-fm="${mm.id}"`, i = h.indexOf(marca); if (i < 0) continue;
    const fin = h.indexOf("</div>", i) + 6;
    h = h.slice(0, fin) + extra + h.slice(fin);
  }
  return h;
};
window.focoMes = focoMes;

/* ---------- 4. El día: aporta a una meta o a una sub-meta, y cuánto ---------- */
opcionesAporte = function (ms, actual) {
  const act = String(actual ?? "");
  return `<option value="">Aporta a…</option>${ms.map(mm => {
    const a = Store.get("meta_anio", mm.metaAnualId), nombre = a?.titulo || mm.texto, c = casillaDeMes(mm);
    const elegidos = pasosDelMes(mm), pasos = c ? [0, 1, 2, 3, 4, 5, 6, 7].filter(p => respuesta(Mandala.clave(c.id, p)).trim() && (!elegidos.length || elegidos.includes(p))) : [];
    const op = (v, t) => `<option value="${v}" ${v === act ? "selected" : ""}>${esc(t)}</option>`;
    if (!pasos.length) return op(String(mm.id), nombre);
    return `<optgroup label="${esc(nombre)}">${op(String(mm.id), `${nombre} (toda la meta)`)}${pasos.map(p => {
      const t = respuesta(Mandala.clave(c.id, p)).trim(), g = MetodoCima.leer(c.id, p).cumplido;
      return op(`${mm.id}|${p}`, `${p + 1}. ${t.length > 40 ? t.slice(0, 38) + "…" : t}${g ? " ✓" : ` · +${Math.round(aportePaso(c.id, p) * 10) / 10} %`}`); }).join("")}</optgroup>`;
  }).join("")}`;
};
window.opcionesAporte = opcionesAporte;
const marcarDiaMetaBase = marcarDiaMeta;
marcarDiaMeta = function (v, fecha) {
  const [id, p] = String(v).split("|");
  marcarDiaMetaBase(id, fecha);
  if (p != null && p !== "") { const c = casillaDeMes(Store.get("meta_mes", id)); if (c) jornadaPaso(c.id, Number(p), fecha).then(() => pintarDetalle()); }
};
/** Lo que aportaron hoy las actividades y las cumbres hechas (por meta). */
function aporteDelDia(fecha) {
  const r = new Map(), sumar = v => {
    const [id, p] = String(v || "").split("|"); if (p == null || p === "") return;
    const mm = Store.get("meta_mes", id), c = casillaDeMes(mm); if (!c) return;
    const a = Store.get("meta_anio", mm.metaAnualId), k = a?.titulo || mm.texto; r.set(k, (r.get(k) || 0) + aportePaso(c.id, Number(p)));
  };
  for (let h = 0; h < 24; h++) if (respuesta(`dia#${fecha}#${h}#hecho`) === "1") sumar(respuesta(`dia#${fecha}#${h}#meta`));
  try { (topDe(fecha) || []).forEach(x => { if (x?.hecho) sumar(x.meta); }); } catch { /* sin cumbres */ }
  return r;
}
const actividadesDiaBase = actividadesDia;
actividadesDia = function (fecha, ag, ms) {
  const h = actividadesDiaBase(fecha, ag, ms), r = aporteDelDia(fecha); if (!r.size) return h;
  const total = [...r.values()].reduce((x, y) => x + y, 0);
  const res = `<div class="aporte-dia"><b>Hoy aportaste ${Math.round(total * 10) / 10} %</b>${[...r].map(([k, v]) => `<span>+${Math.round(v * 10) / 10} % ${esc(k)}</span>`).join("")}</div>`;
  return h.replace(`<div class="acts">`, res + `<div class="acts">`);
};

/** Metas del año que no son la meta de un campamento de su Brújula (por ejemplo, una segunda meta del mismo eje). */
function otrasMetasAnio(a, metas) {
  const ids = new Set(Mandala.CAMPAMENTOS_FIJOS.map(cod => Sincro.objetivo(a, cod)?.id).filter(Boolean)), otras = metas.filter(m => !ids.has(m.id));
  return hoja(`<div class="cab-lista-btn"><div class="etiqueta">${otras.length ? `Otras metas de ${a}` : `¿Otra meta para ${a}?`}</div><button class="btn mini oro" data-acc="nuevaMetaAnio" data-arg="${a}">${ic("mas")} Nueva meta</button></div>
    ${otras.map(m => `<div class="meta-fila"><div class="arriba"><button style="text-align:left" data-sel="ma:${m.id}">${esc(m.titulo)}</button></div></div>`).join("") || `<p class="suave" style="margin:6px 0 0">Las metas de ${a} son las de su Brújula. Aquí puedes sumar otras.</p>`}`);
}
window.otrasMetasAnio = otrasMetasAnio;

/* Copiar la Brújula general a la de un año: la cumbre, los 8 campamentos (frase, eje y foto) y sus sub-metas.
 * Lo ganado y las jornadas no se copian: el año empieza su propio camino. Si el año ya tiene algo escrito, se reemplaza. */
ACC.copiarGeneral = async (el, arg) => {
  const a = Number(arg), anio = casillasDe(a);
  if (anio.length && !el.dataset.seguro) {
    el.dataset.seguro = "1"; const antes = el.innerHTML; el.innerHTML = `¿Reemplazar la Brújula de ${a}? Toca otra vez`;
    setTimeout(() => { if (el.isConnected) { delete el.dataset.seguro; el.innerHTML = antes; } }, 5000);
    return;
  }
  const gen = Mandala.repartir(casillasDe(null)), dest = Mandala.repartir(anio);
  const base = c => (c.origen || "").replace(/^anio:\d{4}:/, "");
  const copiar = async (src, actual, origen) => {
    if (!src) return null;
    const datos = { titulo: src.titulo, afirmacion: src.afirmacion, eje: src.eje ?? null, sugerencia: src.sugerencia || "", busqueda: src.busqueda || "", publicacionId: src.publicacionId ?? null };
    const raw = actual ? Store.get("vision", actual.id) : null;
    const obj = raw ? { ...raw, ...datos } : { id: nuevoId(), orden: src.orden || 0, ...datos, origen: `anio:${a}:${origen || base(src) || "manual"}` };
    await Store.guardar("vision", obj);
    return obj;
  };
  await copiar(gen.cumbre, dest.cumbre, "cumbre");
  let n = 0;
  for (let i = 0; i < 8; i++) {
    const src = gen.camps[i]; if (!src) continue;
    const obj = await copiar(src, dest.camps[i], "campamento:" + Mandala.CAMPAMENTOS_FIJOS[i]); n++;
    for (let p = 0; p < 8; p++) await responderBase("mandala", Mandala.clave(obj.id, p), respuesta(Mandala.clave(src.id, p)).trim());
  }
  await Sincro.reconciliar(a);
  // Cada meta del año toma el nombre de su campamento copiado
  for (let i = 0; i < 8; i++) { const cod = Mandala.CAMPAMENTOS_FIJOS[i], src = gen.camps[i], m = src && Sincro.objetivo(a, cod);
    if (m && mdNombre(src) && m.titulo !== mdNombre(src)) await Store.guardar("meta_anio", { ...m, titulo: mdNombre(src).slice(0, 120) }); }
  toast(`Listo: la Brújula de ${a} tiene la cumbre y ${n} campamentos de tu Brújula general, con sus sub-metas.`);
  pintarDetalle(); pintarLista();
};
