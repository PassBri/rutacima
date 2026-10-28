/* Paso de hoy: una fila de chips, uno por meta del mes (punto de color y %). Un toque marca que hoy le diste un paso;
 * si la meta tiene sub-metas, primero se escoge a cuál, y eso suma la jornada de esa sub-meta del Método Cima.
 * Se recuerda qué sub-meta se marcó ("paso:hoy:<meta del mes>:<fecha>") para poder deshacerlo bien. Igual que la app. */
(() => {
  const clave = (id, fecha) => `paso:hoy:${id}:${fecha}`;
  const pasosMes = mm => {
    const c = casillaDeMes(mm); if (!c) return { c: null, ps: [] };
    const todos = [0, 1, 2, 3, 4, 5, 6, 7].filter(p => respuesta(Mandala.clave(c.id, p)).trim() && !MetodoCima.leer(c.id, p).cumplido);
    const el = numeros(respuesta(`mes:pasos:${mm.id}`)).filter(p => todos.includes(p));
    return { c, ps: el.length ? el : todos };
  };
  const corto = (t, n) => t.length > n ? t.slice(0, n - 1) + "…" : t;
  window.pasoDeHoy = (fecha, ms) => {
    const f = deIso(fecha), d = f.getDate(), futuro = f > hoyFecha();
    ms = ms.filter(x => x.metaAnualId != null);
    const hechos = ms.filter(x => numeros(x.dias).includes(d)).length, abierta = estado.pasoHoy && Store.get("meta_mes", estado.pasoHoy);
    const chip = x => {
      const si = numeros(x.dias).includes(d), a = Store.get("meta_anio", x.metaAnualId), eje = ejeEfectivo("meta_mes", x);
      return `<button class="ph-chip${si ? " si" : ""}" data-acc="pasoHoy" data-arg="${x.id}|${fecha}" aria-pressed="${si}" ${futuro ? "disabled" : ""} title="${esc(a?.titulo || x.texto)}">
        ${si ? ic("check") : `<i class="eje-punto" style="background:${COLOR_EJE[eje] || "#7A4A6B"}"></i>`}<span>${esc(corto(a?.titulo || x.texto, 28))}</span><small>${pctTxt(avanceMetaMes(x))}</small></button>`;
    };
    let subs = "";
    if (abierta && ms.some(x => x.id === abierta.id)) {
      const { c, ps } = pasosMes(abierta);
      subs = `<div class="ph-subs" role="group" aria-label="¿A qué sub-meta le diste el paso?"><small>¿A qué sub-meta le diste el paso?</small><div class="ph-fila">
        ${ps.map(p => `<button class="chip" data-acc="pasoHoySub" data-arg="${abierta.id}|${fecha}|${p}">${p + 1}. ${esc(corto(respuesta(Mandala.clave(c.id, p)).trim(), 34))} <span class="suave">+${Math.round(aportePaso(c.id, p) * 10) / 10} %</span></button>`).join("")}
        <button class="chip" data-acc="pasoHoySub" data-arg="${abierta.id}|${fecha}|">A la meta en general</button>
        <button class="chip" data-acc="pasoHoyCerrar" aria-label="Cancelar">${ic("cerrar")}</button></div></div>`;
    }
    return seccionDia("metas", cabSec("metas", "Paso de hoy", ms.length ? "Toca una meta si hoy le diste un paso." : "",
        ms.length ? `<span class="sec-badge">${hechos}/${ms.length}</span>` : "") +
      (ms.length ? `<div class="ph-fila">${ms.map(chip).join("")}</div>${subs}`
        : `<p class="suave" style="margin:8px 0 0">Aún no eliges qué metas trabajar este mes. <button class="btn mini oro" data-ir="ruta" data-arg="m:${fecha.slice(0, 7)}">Elegir en el mes</button></p>`));
  };
  const marcar = async (id, fecha, p) => {
    const mm = Store.get("meta_mes", id); if (!mm) return;
    estado.pasoHoy = null;
    marcarDiaMeta(id, fecha);
    if (p !== "" && p != null) { const c = casillaDeMes(mm); if (c) { await jornadaPaso(c.id, Number(p), fecha); await responder("metas", clave(id, fecha), `${c.id}|${p}`); } }
    toast("¡Paso de hoy marcado!"); pintarDetalle();
  };
  ACC.pasoHoy = async (_, arg) => {
    const [id, fecha] = arg.split("|"), mm = Store.get("meta_mes", id); if (!mm) return;
    const d = deIso(fecha).getDate();
    if (numeros(mm.dias).includes(d)) { // deshacer: quita el día y, si sumó una jornada, también esa jornada
      await Store.cambiar("meta_mes", id, { dias: numeros(mm.dias).filter(x => x !== d).join(",") });
      const [cid, p] = respuesta(clave(id, fecha)).split("|");
      if (cid && p !== undefined) { const x = MetodoCima.leer(Number(cid), Number(p));
        if (!x.cumplido) await responderBase("mandala", MetodoCima.claveJornadas(Number(cid), Number(p)), x.jornadas.filter(j => j !== fecha).join(","));
        await responder("metas", clave(id, fecha), ""); }
      estado.pasoHoy = null; pintarDetalle(); return;
    }
    if (pasosMes(mm).ps.length) { estado.pasoHoy = estado.pasoHoy === Number(id) ? null : Number(id); pintarDetalle(); return; }
    marcar(id, fecha, "");
  };
  ACC.pasoHoySub = (_, arg) => { const [id, fecha, p] = arg.split("|"); marcar(id, fecha, p); };
  ACC.pasoHoyCerrar = () => { estado.pasoHoy = null; pintarDetalle(); };
})();
