/* El mes en dos tiempos: ELEGIR (un panel con las metas del año por campamento y sus sub-metas) y TRABAJAR (solo
 * tarjetas con lo elegido: la meta, su avance y sus sub-metas del mes). Los días ya están en el calendario de la barra.
 * Usa las mismas acciones de siempre (focoMes, mesPaso), así que nada de lo guardado cambia. Igual que la app. */
(() => {
  const pasosDe = mm => {
    const c = casillaDeMes(mm); if (!c) return { c: null, todos: [], vistos: [] };
    const todos = [0, 1, 2, 3, 4, 5, 6, 7].filter(p => respuesta(Mandala.clave(c.id, p)).trim());
    const elegidos = numeros(respuesta(`mes:pasos:${mm.id}`)).filter(p => todos.includes(p));
    return { c, todos, elegidos, vistos: elegidos.length ? elegidos : todos };
  };
  const punto = eje => `<i class="eje-punto" style="background:${COLOR_EJE[eje] || "#7A4A6B"}" title="${esc(NOMBRE_EJE[eje] || "Confluencia")}"></i>`;
  const corto = (t, n) => t.length > n ? t.slice(0, n - 1) + "…" : t;

  function tarjeta(mm) {
    const x = Store.get("meta_anio", mm.metaAnualId), eje = x ? ejeEfectivo("meta_anio", x) : null;
    const { c, vistos } = pasosDe(mm), dias = numeros(mm.dias).length;
    const pasos = vistos.map(p => {
      const t = respuesta(Mandala.clave(c.id, p)).trim(), e = MetodoCima.leer(c.id, p), req = e.req || MetodoCima.INICIAL;
      return `<li class="${e.cumplido ? "ganado" : ""}"><span class="mf-marca" aria-hidden="true">${e.cumplido ? ic("check") : ""}</span>
        <span class="mf-t">${p + 1}. ${esc(t)}</span><small>${e.cumplido ? "Ganada" : `${e.jornadas.length}/${req}`}</small></li>`;
    }).join("");
    return `<article class="mf-card">
      <header>${punto(eje)}<b>${esc(x?.titulo || mm.texto)}</b><button class="mf-pct" data-ir="metas" data-arg="m:${mm.id}" title="Abrir la meta del mes">${pctTxt(avanceMetaMes(mm))}</button></header>
      ${pasos ? `<ul class="mf-pasos">${pasos}</ul>` : `<p class="suave mf-vacio">Sin sub-metas todavía: escríbelas en la Brújula de ${mm.anio}.</p>`}
      <footer>${dias ? `${dias} ${dias === 1 ? "día" : "días"} con avance este mes` : "Aún sin días con avance este mes"}</footer>
    </article>`;
  }

  function panelElegir(a, m, anuales, delMes) {
    const grupos = new Map();
    for (const x of anuales) { const e = ejeEfectivo("meta_anio", x) || "CON"; if (!grupos.has(e)) grupos.set(e, []); grupos.get(e).push(x); }
    const orden = [...grupos.keys()].sort((p, q) => Mandala.CAMPAMENTOS_FIJOS.indexOf(p) - Mandala.CAMPAMENTOS_FIJOS.indexOf(q));
    const fila = x => {
      const mm = delMes.find(y => y.metaAnualId === x.id), info = mm ? pasosDe(mm) : null;
      return `<div class="mf-op${mm ? " sel" : ""}"><label><input type="checkbox" data-acc-change="focoMes" data-arg="${a}|${m}|${x.id}" ${mm ? "checked" : ""}><span>${esc(x.titulo)}</span></label>
        ${mm && info.todos.length ? `<div class="mf-subs"><small>${info.elegidos.length ? `${info.elegidos.length} de ${info.todos.length} sub-metas` : "Todas sus sub-metas"} · toca para escoger solo algunas</small>
          <div class="fm-chips">${info.todos.map(p => { const t = respuesta(Mandala.clave(info.c.id, p)).trim();
            return `<button class="chip" data-acc="mesPaso" data-arg="${mm.id}|${p}" aria-pressed="${info.elegidos.includes(p)}" title="${esc(t)}">${p + 1}. ${esc(corto(t, 34))}</button>`; }).join("")}</div></div>` : ""}</div>`;
    };
    return `<div class="mf-panel" role="region" aria-label="Elegir metas de ${MESES[m - 1]}">
      ${orden.map(e => `<section><h4>${punto(e)} ${esc(NOMBRE_EJE[e] || "Confluencia")}</h4>${grupos.get(e).map(fila).join("")}</section>`).join("")}
      <div class="mf-listo"><button class="btn oro" data-acc="mesElegir" data-arg="">${ic("check")} Listo</button></div></div>`;
  }

  focoMes = function (a, m) {
    const delMes = metasDelMes(a, m).filter(y => y.metaAnualId != null && Store.get("meta_anio", y.metaAnualId));
    const anuales = Store.lista("meta_anio").filter(x => x.anio === a).sort((x, y) => x.id - y.id);
    const eligiendo = estado.mesElegir === `${a}-${m}`, mes = MESES[m - 1];
    if (!anuales.length) return hoja(`<div class="etiqueta">Mis metas de ${mes}</div>
      <p class="suave" style="margin:6px 0 10px">Aún no hay metas de ${a}. Nacen de tu Brújula de ${a}.</p>
      <button class="btn mini" data-acc="abrirBrujulaAnio" data-arg="${a}">${ic("brujula")} Brújula de ${a}</button>`);
    const nSubs = delMes.reduce((s, mm) => s + pasosDe(mm).vistos.length, 0);
    const resumen = delMes.length
      ? `En ${mes} trabajas en <b>${delMes.length} ${delMes.length === 1 ? "meta" : "metas"}</b>${nSubs ? ` y <b>${nSubs} ${nSubs === 1 ? "sub-meta" : "sub-metas"}</b>` : ""} de ${a}.`
      : `Aún no eliges qué trabajar en ${mes}. Escoge unas pocas metas de ${a}: en cada día, tus acciones aportarán a ellas.`;
    return hoja(`<div class="cab-lista-btn"><div class="etiqueta">${eligiendo ? `Elige qué trabajar en ${mes}` : `Mis metas de ${mes}`}</div>
        ${eligiendo ? "" : `<button class="btn mini ${delMes.length ? "" : "oro"}" data-acc="mesElegir" data-arg="${a}-${m}">${ic(delMes.length ? "editar" : "mas")} ${delMes.length ? "Cambiar" : "Elegir"}</button>`}</div>
      <p class="suave" style="margin:4px 0 12px">${eligiendo ? `Marca las metas de ${a} y, si quieres, solo algunas de sus sub-metas.` : resumen}</p>
      ${eligiendo ? panelElegir(a, m, anuales, delMes) : `<div class="mf-lista">${delMes.map(tarjeta).join("")}</div>`}`, "mf-hoja");
  };
  window.focoMes = focoMes;
  ACC.mesElegir = (_, arg) => { estado.mesElegir = arg || null; pintarDetalle(); };
})();
