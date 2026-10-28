/* La meta del año en una sola pantalla (Material 3): lo esencial arriba en una grilla compacta y el resto en pestañas
 * (Sub-metas · Plan de acción · Seguimiento · Meses). Mismos campos y mismos datos que antes: nada se pierde. */
(() => {
  const TABS = [["sub", "Sub-metas"], ["plan", "Plan de acción"], ["seg", "Seguimiento"], ["mes", "Meses"]];
  const corto = s => s.replace(" · campamento de tu Brújula", "");
  DET["metas.a"] = arg => {
    const m = Store.get("meta_anio", arg); if (!m) return vacio();
    const c = cascada(), k = String(m.id), tab = estado.metaTab || "sub";
    const hijos = c.mensuales.filter(x => x.metaAnualId === m.id).sort((x, y) => x.anio - y.anio || x.mes - y.mes);
    const ps = PlanMeta.leer(m.id), subs = String(m.subMetas || "").split("\n").filter(s => s.trim()).length;
    const cuenta = { sub: subs, plan: ps.length, mes: hijos.length };
    const avance = hijos.length
      ? `<p class="suave mc-auto">Avance automático: <b>${pctTxt(c.nodoAnio(m))}</b>, el promedio de sus metas del mes.</p>`
      : `<div class="rango"><input type="range" min="0" max="100" step="5" value="${m.avance || 0}" ${bind("meta_anio", k, "avance", "num")} aria-label="Avance manual"><b>${m.avance || 0}%</b></div>`;
    const panel = {
      sub: () => window.subMetasHtml ? subMetasHtml("meta_anio", m) : area("meta_anio", k, "subMetas", m.subMetas),
      plan: () => window.planMetaHtml ? planMetaHtml(m, true) : "",
      seg: () => `<div class="mc-seg">${campo("Indicador", txt("meta_anio", k, "indicador", m.indicador, "¿Qué medirás?"))}
          ${campo("Próxima acción", txt("meta_anio", k, "proximaAccion", m.proximaAccion))}
          ${campo("Evidencia observable", area("meta_anio", k, "observable", m.observable, "¿Cómo sabrás que la lograste?"))}
          ${campo("Obstáculo", area("meta_anio", k, "obstaculo", m.obstaculo))}</div>`,
      mes: () => `<div class="cab-lista-btn"><span class="suave">Metas del mes que la alimentan</span><button class="btn mini oro" data-acc="nuevaMetaMes" data-arg="${iso(hoyFecha()).slice(0, 7)}|${m.id}">${ic("mas")} Nueva</button></div>
          ${hijos.map(x => `<div class="meta-fila"><div class="arriba"><button style="text-align:left" data-sel="m:${x.id}">${mayus(MESES[x.mes - 1])} ${x.anio} · ${esc(x.texto)}</button><span class="pct">${pctTxt(avanceMetaMes(x))}</span></div></div>`).join("") || `<p class="suave">Aún no la trabajas en ningún mes. Elígela en el mes desde Mi ruta.</p>`}`,
    }[tab]();
    return cab(av(pctTxt(c.nodoAnio(m)), COLOR_EJE[m.eje] || "var(--oro)"), esc(m.titulo), `Meta de ${m.anio}`,
      `<button class="btn mini peligro" data-acc="borrar" data-arg="meta_anio|${k}">${ic("borrar")} Borrar</button>`) + cuerpo(
      (window.franjaCampamento ? franjaCampamento("meta_anio", m) : "") +
      hoja(`<div class="mc-grid">
          <div class="mc-meta">${campo("Meta", txt("meta_anio", k, "titulo", m.titulo))}</div>
          <div class="mc-apodo">${typeof campoApodo === "function" ? campoApodo("a", k).replace(/placeholder="[^"]*"/, 'placeholder="Ej. Operación Fénix"') : ""}</div>
          ${campo("Año", `<input type="number" min="1900" max="2200" ${bind("meta_anio", k, "anio", "num")} value="${m.anio}">`)}
          ${campo("Propósito", sel("meta_anio", k, "propositoId", m.propositoId ?? "", [["", "Sin propósito"], ...c.props.map(p => [p.id, p.titulo])], "numNulo"))}
          ${campo("Campamento", sel("meta_anio", k, "eje", m.eje || "", opcionesEje.map(([v, n]) => [v, corto(n)]), "nulo"))}
          ${campo("Prioridad", sel("meta_anio", k, "prioridad", m.prioridad || "B", PRIORIDADES.map(x => [x, PRIORIDAD_NOMBRE[x]])))}
          ${campo("Estado", sel("meta_anio", k, "estado", m.estado || "NO_INICIADA", ESTADOS))}
          ${campo("Decisión", sel("meta_anio", k, "decision", m.decision || "CONTINUAR", DECISIONES))}
          <div class="mc-avance">${hijos.length ? "" : `<span class="mc-etq">Avance manual</span>`}${avance}</div>
        </div>`, "mc-hoja") +
      hoja(`<div class="mc-tabs" role="tablist" aria-label="Partes de la meta">${TABS.map(([id, n]) => `<button role="tab" data-acc="metaTab" data-arg="${id}" aria-selected="${tab === id}">${n}${cuenta[id] ? ` <small>${cuenta[id]}</small>` : ""}</button>`).join("")}</div>
        <div class="mc-panel" role="tabpanel">${panel}</div>`, "mc-hoja mc-tabhoja") +
      (window.deTuMochila ? `<details class="mc-mochila"><summary>${ic("lista")} Ideas: bancos de metas, indicadores y plantillas</summary>${deTuMochila()}</details>` : ""), "max-width:1100px;gap:12px");
  };
  ACC.metaTab = (_, id) => { estado.metaTab = id; pintarDetalle(); };
})();
