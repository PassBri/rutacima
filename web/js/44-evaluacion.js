"use strict";
/* ======================================================================
 * Evaluación: mirar hacia atrás y medir (Mi ruta es para planear hacia adelante).
 * · El mes: puntaje (metas + hábitos + bienestar), cumplimiento de cada meta y sus sub-metas ganadas,
 *   hábitos por eje, bienestar del mes, balance escrito y un resumen de finanzas.
 * · El año: puntaje mes a mes, la Brújula del año (8 campamentos), metas del año, bienestar, balance anual
 *   escrito (el mismo de la app: tabla "balance") y finanzas del año.
 * · Propósitos: cada uno con su período, su avance, las metas de cada año que lo alimentan y una reflexión.
 * Los datos no cambian de lugar: el balance del mes sigue en "mes", el anual en "balance".
 * Informes PDF (para imprimir o "Guardar como PDF"): de un día, de varios días, del mes y del año.
 * Igual que la app (ui/evaluacion/Evaluacion.kt).
 * ====================================================================== */
(() => {
  // La sección en el riel (solo en computador; en el teléfono se llega desde Mi ruta con «Evaluar»)
  if (!SECCIONES.some(([k]) => k === "evaluacion")) SECCIONES.splice(SECCIONES.findIndex(([k]) => k === "brujula") + 1, 0, ["evaluacion", "Evaluación"]);
  SOLO_PC.add("evaluacion");
  P.evaluacion = "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z";
  BASES.balance = c => ({ anio: Number(c) });

  const dos = n => String(n).padStart(2, "0");
  const ymDe = (a, m) => `${a}-${dos(m)}`;
  const pctN = x => x == null ? "—" : Math.round(x * 100) + " %";
  const prom = l => { const v = l.filter(x => x != null); return v.length ? v.reduce((s, x) => s + x, 0) / v.length : null; };
  const fechasHasta = (a, m) => { const r = [], hoy = hoyFecha(); for (let d = 1; d <= diasEnMes(a, m); d++) { const f = new Date(a, m - 1, d); if (f <= hoy) r.push(iso(f)); } return r; };
  const finOculto = () => (typeof finBloqueado === "function" && finBloqueado()) || (typeof ocultarFuera === "function" && ocultarFuera());

  /* ---------- Cálculos ---------- */
  /** Sub-metas (pasos de la Brújula) de una meta del mes: las que se trabajan y las ganadas en ese mes. */
  function pasosMesEval(mm) {
    const c = typeof casillaDeMes === "function" ? casillaDeMes(mm) : null; if (!c) return { total: 0, ganadas: 0, lista: [] };
    const todos = [0, 1, 2, 3, 4, 5, 6, 7].filter(p => respuesta(Mandala.clave(c.id, p)).trim());
    const el = numeros(respuesta(`mes:pasos:${mm.id}`)).filter(p => todos.includes(p)), vis = el.length ? el : todos, ym = ymDe(mm.anio, mm.mes);
    const lista = vis.map(p => { const x = MetodoCima.leer(c.id, p), hecho = respuesta(Mandala.claveHecho(c.id, p));
      return { p, t: respuesta(Mandala.clave(c.id, p)).trim(), ganada: !!x.cumplido, esteMes: String(hecho).startsWith(ym), jornadas: x.jornadas.filter(j => j.startsWith(ym)).length, req: x.req || MetodoCima.INICIAL }; });
    return { total: lista.length, ganadas: lista.filter(x => x.esteMes).length, lista };
  }
  /** Hábitos: cuántos se marcaron sobre los posibles, en total y por eje. */
  function habitosEval(fechas) {
    const HS = habitos(), porEje = Object.fromEntries(EJES.map(([c]) => [c, { si: 0, de: 0 }]));
    let si = 0;
    fechas.forEach(f => { const m = marcadosDe(f); HS.forEach(([id, eje]) => { if (!porEje[eje]) return; porEje[eje].de++; if (m.has(id)) { porEje[eje].si++; si++; } }); });
    const de = HS.length * fechas.length;
    return { p: de ? si / de : null, si, de, porEje, diasCumplidos: fechas.filter(f => diaCumplido(deIso(f))).length };
  }
  function evalMes(a, m) {
    const fechas = fechasHasta(a, m), ms = metasDelMes(a, m).filter(x => x.metaAnualId != null && Store.get("meta_anio", x.metaAnualId));
    const metas = ms.map(mm => ({ mm, anual: Store.get("meta_anio", mm.metaAnualId), p: avanceMetaMes(mm), dias: numeros(mm.dias).length, pasos: pasosMesEval(mm) }));
    const hab = habitosEval(fechas), bien = fechas.length && typeof statsBien === "function" ? statsBien(fechas) : null;
    const pMetas = prom(metas.map(x => x.p)), pBien = bien?.indice != null ? bien.indice / 100 : null;
    return { a, m, fechas, metas, hab, bien, pMetas, pHab: hab.p, pBien, puntaje: prom([pMetas, hab.p, pBien]) };
  }
  function evalAnio(a) {
    const hoy = hoyFecha(), meses = [];
    for (let m = 1; m <= 12; m++) meses.push(new Date(a, m - 1, 1) <= hoy ? evalMes(a, m) : null);
    const c = cascada(), anuales = c.anuales.filter(x => x.anio === a);
    const camps = Mandala.CAMPAMENTOS_FIJOS.map(cod => {
      const meta = Sincro.objetivo(a, cod), cas = meta ? Sincro.casillaDe("meta_anio", meta) : null;
      const pasos = cas ? [0, 1, 2, 3, 4, 5, 6, 7].filter(p => respuesta(Mandala.clave(cas.id, p)).trim()) : [];
      return { cod, meta, p: meta ? c.nodoAnio(meta) : null, total: pasos.length, ganadas: pasos.filter(p => MetodoCima.leer(cas.id, p).cumplido).length };
    });
    const pMetas = prom(anuales.map(x => c.nodoAnio(x)));
    const todas = meses.filter(Boolean).flatMap(x => x.fechas), hab = habitosEval(todas);
    const bien = todas.length && typeof statsBien === "function" ? statsBien(todas) : null, pBien = bien?.indice != null ? bien.indice / 100 : null;
    return { a, meses, anuales, camps, pMetas, pHab: hab.p, hab, bien, pBien, puntaje: prom([pMetas, hab.p, pBien]), c };
  }

  /* ---------- Piezas visuales ---------- */
  const anillo = (p, etq) => `<div class="ev-anillo" style="--p:${p ?? 0}" role="img" aria-label="${etq}: ${pctN(p)}"><b>${p == null ? "—" : Math.round(p * 100)}</b><small>${p == null ? "" : "%"}</small></div>`;
  const pista = (p, color = "var(--burdeos)") => `<span class="ev-pista" aria-hidden="true"><em style="width:${Math.max(0, Math.min(100, (p || 0) * 100)).toFixed(1)}%;background:${color}"></em></span>`;
  const kpiEv = (etq, p, sub) => `<div class="ev-kpi"><span>${etq}</span><b>${pctN(p)}</b>${pista(p)}<small>${sub}</small></div>`;
  const botonInforme = sel => `<button class="btn mini" data-acc="informeRuta" data-arg="${sel}">${ic("descargar")} Descargar PDF</button>`;
  const lectura = p => p == null ? "Aún no hay datos para medir." : p >= .8 ? "Un ascenso firme: sostén el ritmo." : p >= .6 ? "Buen paso: ajusta lo que quedó corto." : p >= .4 ? "A medio camino: elige menos y cúmplelo." : "Mes de niebla: vuelve a lo esencial, sin culpa.";

  function finanzasHoja(pref, titulo, sel) {
    if (typeof totalesDe !== "function") return "";
    if (finOculto()) return hoja(`<div class="cab-lista-btn"><h3 style="margin:0">${titulo}</h3><button class="btn mini" data-ir="finanzas" data-arg="${sel}">${ic("candado")} Abrir</button></div><p class="suave" style="margin:6px 0 0">Tus finanzas están protegidas u ocultas. Ábrelas en Ascenso financiero.</p>`);
    const t = totalesDe(pref);
    return hoja(`<div class="cab-lista-btn"><h3 style="margin:0">${titulo}</h3><button class="btn mini" data-ir="finanzas" data-arg="${sel}">${ic("finanzas")} Ver detalle</button></div>
      ${t.n ? `<div class="ev-fin"><div><span>Entró</span><b>${plata(t.ing)}</b></div><div><span>Salió</span><b>${plata(t.gas + t.aho + t.inv + (t.gen || 0))}</b></div><div><span>Disponible</span><b class="${t.disp < 0 ? "neg" : ""}">${plata(t.disp)}</b></div><div><span>Ahorro e inversión</span><b>${t.tasa != null ? Math.round(t.tasa * 100) + " %" : "—"}</b></div></div>`
        : `<p class="suave" style="margin:6px 0 0">Sin movimientos en este período.</p>`}`);
  }

  /* ---------- El mes ---------- */
  function balanceMesHoja(a, m) {
    const clave = ymDe(a, m), b = Store.get("mes", clave) || {};
    const cm = (n, etq) => campo(etq, `<textarea ${bind("mes", clave, n)}>${esc(b[n] || "")}</textarea>`);
    return hoja(`<h3>Balance de ${MESES[m - 1]}</h3><div class="form ev-form">${cm("comoEstuvo", "¿Cómo estuvo el mes?")}${cm("logros", "Logros")}${cm("desafios", "Desafíos")}
      ${cm("agradecido", "Estoy agradecido por")}${cm("mejorar", "Puedo mejorar")}${cm("objetivosProximo", "Objetivos del próximo mes")}${cm("notas", "Notas")}</div>`);
  }
  DET["evaluacion.m"] = arg => {
    const [a, m] = String(arg || ymDe(hoyFecha().getFullYear(), hoyFecha().getMonth() + 1)).split("-").map(Number), e = evalMes(a, m);
    const metasH = e.metas.length ? e.metas.map(x => `<li><i class="eje-punto" style="background:${COLOR_EJE[ejeEfectivo("meta_mes", x.mm)] || "#7A4A6B"}"></i>
        <button class="ev-n" data-ir="ruta" data-arg="m:${ymDe(a, m)}">${esc(x.anual?.titulo || x.mm.texto)}</button><b>${pctN(x.p)}</b>
        ${pista(x.p, COLOR_EJE[ejeEfectivo("meta_mes", x.mm)] || "var(--burdeos)")}
        <small>${x.dias} ${x.dias === 1 ? "día" : "días"} con avance${x.pasos.total ? ` · ${x.pasos.ganadas} de ${x.pasos.total} sub-metas ganadas este mes` : ""}</small></li>`).join("")
      : `<p class="suave">No elegiste metas para ${MESES[m - 1]}. <button class="btn mini" data-ir="ruta" data-arg="m:${ymDe(a, m)}">Elegir en Mi ruta</button></p>`;
    const ejes = EJES.map(([c, n]) => { const x = e.hab.porEje[c]; return x?.de ? `<li><span>${n}</span>${pista(x.si / x.de, COLOR_EJE[c])}<b>${pctN(x.si / x.de)}</b></li>` : ""; }).join("");
    return cab(av(MES_C[m - 1], "var(--burdeos)"), `Evaluación de ${MESES[m - 1]} ${a}`, `${e.fechas.length} días vividos del mes`, botonInforme(`m:${ymDe(a, m)}`)) + cuerpo(
      hoja(`<div class="ev-resumen">${anillo(e.puntaje, "Puntaje del mes")}<div><h3>Puntaje del mes</h3><p class="suave">${lectura(e.puntaje)}</p>
        <p class="ev-nota">Es el promedio de tus metas, tus hábitos y tu bienestar.</p></div></div>
        <div class="ev-kpis">${kpiEv("Metas", e.pMetas, `${e.metas.length} metas este mes`)}${kpiEv("Hábitos", e.pHab, `${e.hab.diasCumplidos} días cumplidos`)}${kpiEv("Bienestar", e.pBien, e.bien?.registrados ? `${e.bien.registrados} días registrados` : "Sin registros")}</div>`) +
      hoja(`<h3>Cumplimiento de metas</h3><ul class="ev-lista">${metasH}</ul>`) +
      (ejes ? hoja(`<h3>Hábitos por eje</h3><ul class="ev-ejes">${ejes}</ul>`) : "") +
      (window.bienResumenMes ? bienResumenMes(a, m) : "") +
      balanceMesHoja(a, m) +
      finanzasHoja(ymDe(a, m) + "-", `Finanzas de ${MESES[m - 1]}`, `m:${ymDe(a, m)}`), "max-width:980px");
  };

  /* ---------- El año ---------- */
  function balanceAnioHoja(a) {
    const b = Store.get("balance", String(a)) || {};
    const cb = (n, etq) => campo(etq, `<textarea ${bind("balance", String(a), n)}>${esc(b[n] || "")}</textarea>`);
    return hoja(`<h3>Balance de ${a}</h3><div class="form ev-form">${cb("comoMeSenti", "¿Cómo me sentí este año?")}${cb("positivo", "Lo positivo")}${cb("negativo", "Lo que no salió bien")}
      ${cb("loMejor", "Lo mejor del año")}${cb("objetivosProximo", `Objetivos para ${a + 1}`)}</div>`);
  }
  DET["evaluacion.a"] = arg => {
    const a = Number(arg) || hoyFecha().getFullYear(), e = evalAnio(a);
    const barras = e.meses.map((x, i) => `<button class="ev-mes" data-sel="m:${ymDe(a, i + 1)}" ${x ? "" : "disabled"} aria-label="${mayus(MESES[i])}: ${pctN(x?.puntaje)}">
      <span class="ev-col"><i style="height:${x?.puntaje != null ? Math.max(4, x.puntaje * 100) : 0}%"></i></span><b>${x?.puntaje != null ? Math.round(x.puntaje * 100) : ""}</b><small>${MES_C[i]}</small></button>`).join("");
    const camps = e.camps.filter(x => x.meta).map(x => `<li><i class="eje-punto" style="background:${COLOR_EJE[x.cod] || "#7A4A6B"}"></i><button class="ev-n" data-ir="ruta" data-arg="ma:${x.meta.id}">${esc(x.meta.titulo)}</button><b>${pctN(x.p)}</b>
      ${pista(x.p, COLOR_EJE[x.cod] || "var(--burdeos)")}<small>${esc(NOMBRE_EJE[x.cod] || "Confluencia")}${x.total ? ` · ${x.ganadas} de ${x.total} sub-metas ganadas` : ""}</small></li>`).join("");
    const ids = new Set(e.camps.map(x => x.meta?.id).filter(Boolean)), otras = e.anuales.filter(x => !ids.has(x.id));
    return cab(av(String(a).slice(2), "var(--burdeos)"), `Evaluación de ${a}`, "Tu año: metas, hábitos, bienestar y balance", botonInforme(`a:${a}`)) + cuerpo(
      hoja(`<div class="ev-resumen">${anillo(e.puntaje, "Puntaje del año")}<div><h3>Puntaje de ${a}</h3><p class="suave">${lectura(e.puntaje)}</p><p class="ev-nota">Toca un mes para ver su evaluación.</p></div></div>
        <div class="ev-kpis">${kpiEv("Metas del año", e.pMetas, `${e.anuales.length} metas`)}${kpiEv("Hábitos", e.pHab, `${e.hab.diasCumplidos} días cumplidos`)}${kpiEv("Bienestar", e.pBien, e.bien?.registrados ? `${e.bien.registrados} días registrados` : "Sin registros")}</div>
        <h4 class="bd-sub">Mes a mes</h4><div class="ev-meses">${barras}</div>`) +
      hoja(`<h3>Brújula de ${a}</h3>${camps ? `<ul class="ev-lista">${camps}</ul>` : `<p class="suave">Aún no hay Brújula de ${a}. <button class="btn mini" data-ir="ruta" data-arg="a:${a}">Crearla en Mi ruta</button></p>`}
        ${otras.length ? `<h4 class="bd-sub">Otras metas de ${a}</h4><ul class="ev-lista">${otras.map(x => `<li><i class="eje-punto" style="background:#B8A99C"></i><button class="ev-n" data-ir="ruta" data-arg="ma:${x.id}">${esc(x.titulo)}</button><b>${pctN(e.c.nodoAnio(x))}</b>${pista(e.c.nodoAnio(x))}</li>`).join("")}</ul>` : ""}`) +
      (window.bienResumenAnio ? bienResumenAnio(a) : "") +
      balanceAnioHoja(a) +
      finanzasHoja(`${a}-`, `Finanzas de ${a}`, `a:${a}`), "max-width:980px");
  };

  /* ---------- Propósitos ---------- */
  DET["evaluacion.p"] = () => {
    const c = cascada(), ps = c.props;
    const fila = p => {
      const { ini, fin } = typeof aniosDeCumbre === "function" ? aniosDeCumbre(p) : { ini: hoyFecha().getFullYear(), fin: hoyFecha().getFullYear() + (p.horizonte || 5) - 1 };
      const anios = [], hoyA = hoyFecha().getFullYear(); for (let y = ini; y <= fin; y++) anios.push(y);
      const pa = c.nodoProp(p), transcurrido = Math.max(0, Math.min(1, (hoyA - ini + 1) / Math.max(1, fin - ini + 1)));
      const k = `eval:p:${p.id}`;
      return hoja(`<div class="ev-prop-cab"><i class="eje-punto" style="background:${COLOR_EJE[p.eje] || "var(--burdeos)"}"></i><div><h3>${esc(p.titulo)}</h3>
          <p class="suave">${ini}–${fin} · ${esc(NOMBRE_EJE[p.eje] || "Confluencia")} · ${Math.round(transcurrido * 100)} % del tiempo recorrido</p></div>${anillo(pa, "Avance del propósito")}</div>
        <p class="ev-nota">${pa >= transcurrido ? "Vas al ritmo o por delante del tiempo." : "Vas detrás del tiempo: revisa qué meta del año lo acerca más."}</p>
        <div class="ev-anios">${anios.map(y => { const ms = c.anuales.filter(x => x.anio === y && x.propositoId === p.id), py = prom(ms.map(x => c.nodoAnio(x)));
          return `<button class="ev-anio${y === hoyA ? " actual" : ""}" data-sel="a:${y}" aria-label="${y}: ${ms.length} metas, ${pctN(py)}"><b>${y}</b>${pista(py)}<small>${ms.length ? `${ms.length} ${ms.length === 1 ? "meta" : "metas"} · ${pctN(py)}` : "Sin metas"}</small></button>`; }).join("")}</div>
        <div class="form ev-form dos">${campo("Qué está funcionando", `<textarea data-resp="metas|${k}:bien">${esc(respuesta(k + ":bien"))}</textarea>`)}${campo("Qué voy a ajustar", `<textarea data-resp="metas|${k}:ajustar">${esc(respuesta(k + ":ajustar"))}</textarea>`)}</div>`);
    };
    return cab(av(ic("brujula"), "var(--burdeos)"), "Balance de propósitos", "Tus cumbres de varios años: cuánto llevas y cuánto tiempo ha pasado") + cuerpo(
      (ps.length ? ps.map(fila).join("") : hoja(`<p class="suave" style="margin:0">Aún no tienes propósitos. Nacen de tu Brújula general. <button class="btn mini" data-sel="vida" data-ir="ruta" data-arg="vida">Abrir la Brújula</button></p>`)), "max-width:980px");
  };

  /* ---------- La lista ---------- */
  LISTAS.evaluacion = () => {
    const hoy = hoyFecha(), a = hoy.getFullYear(), m = hoy.getMonth() + 1;
    const fila = (sel, avatar, t, sub) => item(sel, avatar, t, "", sub);
    let h = cabLista("Evaluación", { buscar: false }) + `<div class="items">`;
    const em = evalMes(a, m), ea = evalAnio(a);
    h += fila(`m:${ymDe(a, m)}`, av(pctN(em.puntaje).replace(" %", ""), "var(--burdeos)"), "Este mes", `${mayus(MESES[m - 1])} · puntaje ${pctN(em.puntaje)}`);
    h += fila(`a:${a}`, av(pctN(ea.puntaje).replace(" %", ""), "var(--burdeos)"), "Este año", `${a} · puntaje ${pctN(ea.puntaje)}`);
    h += fila("p", av(ic("brujula"), "var(--burdeos)"), "Propósitos", (n => `${n} ${n === 1 ? "propósito" : "propósitos"}`)(cascada().props.length));
    h += grupo("Meses anteriores");
    for (let k = 1; k <= 12; k++) { const d = new Date(a, m - 1 - k, 1), y = d.getFullYear(), mm = d.getMonth() + 1;
      h += fila(`m:${ymDe(y, mm)}`, av(MES_C[mm - 1], "#B8A99C"), `${mayus(MESES[mm - 1])} ${y}`, "Evaluación del mes"); }
    h += grupo("Años anteriores");
    const desde = Math.min(a - 1, ...Store.lista("meta_anio").map(x => x.anio).filter(x => x < a));
    for (let y = a - 1; y >= Math.max(desde, a - 10); y--) h += fila(`a:${y}`, av(String(y).slice(2), "#B8A99C"), String(y), "Evaluación del año");
    return h + `</div>`;
  };

  /* ---------- Informes PDF (se abren listos para «Guardar como PDF») ---------- */
  const CSS_INF = `*{box-sizing:border-box}body{font:14px/1.5 Roboto,system-ui,sans-serif;color:#2B2522;margin:0;background:#fff}
    .pag{max-width:820px;margin:0 auto;padding:36px 40px}h1{font:700 28px/1.2 'Roboto Serif',Georgia,serif;color:#6B2A1A;margin:0}h2{font:600 18px/1.3 'Roboto Serif',Georgia,serif;color:#6B2A1A;margin:28px 0 10px;padding-bottom:6px;border-bottom:1px solid #E6DDD3}
    h3{font-size:15px;margin:16px 0 6px}.sub{color:#7A6E66;margin:4px 0 0}.marca{font:600 11px/1 Montserrat,Roboto,sans-serif;letter-spacing:.12em;color:#B08A3E;margin-bottom:10px}
    .kpis{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin:14px 0}.kpis div{border:1px solid #E6DDD3;border-radius:12px;padding:10px 12px}.kpis span{display:block;color:#7A6E66;font-size:12px}.kpis b{font-size:20px}
    table{width:100%;border-collapse:collapse;margin:6px 0 10px}th,td{text-align:left;padding:7px 8px;border-bottom:1px solid #EFE8E0;vertical-align:top}th{font-size:12px;color:#7A6E66;font-weight:600}td.n{text-align:right;white-space:nowrap}
    .barra{height:6px;background:#EFE8E0;border-radius:6px;overflow:hidden;min-width:80px}.barra i{display:block;height:100%;background:#6B2A1A}
    .txt{white-space:pre-wrap;margin:0 0 8px}.dia{page-break-inside:avoid;border-top:2px solid #6B2A1A;margin-top:26px;padding-top:6px}.vacio{color:#9A8F87;font-style:italic}
    .hecho{color:#2F6B4A;font-weight:600}.pie{margin-top:36px;color:#9A8F87;font-size:12px}@media print{.pag{padding:0}@page{margin:16mm}}`;
  const barraInf = p => `<div class="barra"><i style="width:${Math.max(0, Math.min(100, (p || 0) * 100))}%"></i></div>`;
  const txtInf = (etq, v) => v && String(v).trim() ? `<h3>${etq}</h3><p class="txt">${esc(v)}</p>` : "";
  const docInf = (titulo, sub, cuerpoH) => `<!doctype html><html lang="es"><head><meta charset="utf-8"><title>${esc(titulo)} · SuRoute</title><style>${CSS_INF}</style></head>
    <body><div class="pag"><div class="marca">SUROUTE · RUTA A LA CIMA</div><h1>${esc(titulo)}</h1><p class="sub">${esc(sub)}</p>${cuerpoH}
    <p class="pie">Generado el ${hoyFecha().toLocaleDateString("es", { day: "numeric", month: "long", year: "numeric" })} con tus datos de SuRoute. Es privado: compártelo solo si quieres.</p></div>
    <script>addEventListener("load",()=>setTimeout(()=>print(),350))<\/script></body></html>`;
  function abrirInforme(html, nombre) {
    const w = window.open("", "_blank");
    if (!w) { const a = Object.assign(document.createElement("a"), { href: URL.createObjectURL(new Blob([html], { type: "text/html" })), download: nombre + ".html" }); a.click(); toast("Informe descargado: ábrelo y elige «Guardar como PDF»"); return; }
    w.document.open(); w.document.write(html); w.document.close(); toast("Informe listo: elige «Guardar como PDF»");
  }
  /** Un día completo: intención, 3 cumbres, actividades, hábitos, bienestar y cierre. */
  function diaInf(f) {
    const ag = Store.get("agenda", f) || {}, d = deIso(f), top = typeof topDe === "function" ? topDe(f) : [], hor = typeof horarioDe === "function" ? horarioDe(ag) : {};
    const marc = marcadosDe(f), HS = habitos(), b = typeof bienDia === "function" ? bienDia(f) : {};
    const nombreMeta = v => { const [id, p] = String(v || "").split("|"); const mm = Store.get("meta_mes", id); if (!mm) return ""; const a = Store.get("meta_anio", mm.metaAnualId);
      return (a?.titulo || mm.texto) + (p != null && p !== "" ? ` · sub-meta ${Number(p) + 1}` : ""); };
    const acts = Object.entries(hor).filter(([, t]) => t).sort((x, y) => ((Number(x[0]) + 20) % 24) - ((Number(y[0]) + 20) % 24));
    const cl = b.clima && typeof CLIMAS !== "undefined" ? CLIMAS[b.clima]?.n : "";
    return `<section class="dia"><h2 style="border:0;margin-top:0">${mayus(d.toLocaleDateString("es", { weekday: "long", day: "numeric", month: "long", year: "numeric" }))}</h2>
      ${ag.intencion ? `<p><b>Intención:</b> ${esc(ag.intencion)}</p>` : ""}
      <h3>Las 3 cumbres</h3>${top.length ? `<table>${top.map((x, i) => `<tr><td>${i + 1}. ${esc(x.t)}</td><td class="n ${x.hecho ? "hecho" : ""}">${x.hecho ? "Hecha" : "Pendiente"}</td></tr>`).join("")}</table>` : `<p class="vacio">Sin cumbres anotadas.</p>`}
      <h3>Actividades</h3>${acts.length ? `<table><tr><th>Hora</th><th>Actividad</th><th>Aporta a</th><th>Estado</th></tr>${acts.map(([h, t]) => `<tr><td>${dos(h)}:00</td><td>${esc(t)}</td><td>${esc(nombreMeta(respuesta(`dia#${f}#${h}#meta`)))}</td><td class="n ${respuesta(`dia#${f}#${h}#hecho`) === "1" ? "hecho" : ""}">${respuesta(`dia#${f}#${h}#hecho`) === "1" ? "Hecha" : "—"}</td></tr>`).join("")}</table>` : `<p class="vacio">Sin actividades anotadas.</p>`}
      <h3>Hábitos · ${marc.size} de ${HS.length}</h3>${marc.size ? `<p>${HS.filter(([id]) => marc.has(id)).map(([, , t]) => esc(t)).join(" · ")}</p>` : `<p class="vacio">Ninguno marcado.</p>`}
      <h3>Bienestar</h3><p>${[cl && `Clima: ${cl}${b.emo ? ` (${esc(b.emo)})` : ""}`, b.sueno != null && `Sueño: ${b.sueno} h`, b.mov != null && `Actividad física: ${b.mov} min`, b.fyv != null && `Frutas y verduras: ${b.fyv}`, `Agua: ${Number(ag.agua) || 0} de 8`, ag.energia && `Energía: ${ag.energia} de 5`].filter(Boolean).join(" · ")}</p>
      ${txtInf("Victorias", ag.victorias)}${txtInf("Aprendizaje", ag.aprendizaje)}${txtInf("Gratitud", ag.gratitud)}${txtInf("Notas", ag.notas)}</section>`;
  }
  function mesInf(a, m) {
    const e = evalMes(a, m), b = Store.get("mes", ymDe(a, m)) || {};
    const t = !finOculto() && typeof totalesDe === "function" ? totalesDe(ymDe(a, m) + "-") : null;
    return `<div class="kpis"><div><span>Puntaje</span><b>${pctN(e.puntaje)}</b></div><div><span>Metas</span><b>${pctN(e.pMetas)}</b></div><div><span>Hábitos</span><b>${pctN(e.pHab)}</b></div><div><span>Bienestar</span><b>${pctN(e.pBien)}</b></div></div>
      <h2>Cumplimiento de metas</h2>${e.metas.length ? `<table><tr><th>Meta</th><th></th><th class="n">Avance</th><th class="n">Días</th><th class="n">Sub-metas ganadas</th></tr>${e.metas.map(x => `<tr><td>${esc(x.anual?.titulo || x.mm.texto)}</td><td>${barraInf(x.p)}</td><td class="n">${pctN(x.p)}</td><td class="n">${x.dias}</td><td class="n">${x.pasos.total ? `${x.pasos.ganadas} de ${x.pasos.total}` : "—"}</td></tr>`).join("")}</table>` : `<p class="vacio">Sin metas elegidas este mes.</p>`}
      <h2>Hábitos por eje</h2><table>${EJES.map(([c, n]) => { const x = e.hab.porEje[c]; return x?.de ? `<tr><td>${n}</td><td>${barraInf(x.si / x.de)}</td><td class="n">${pctN(x.si / x.de)}</td></tr>` : ""; }).join("")}</table>
      ${e.bien?.registrados ? `<h2>Bienestar</h2><p>${e.bien.registrados} días registrados · índice de vida saludable ${e.bien.indice ?? "—"} %${e.bien.suenoProm != null ? ` · sueño promedio ${e.bien.suenoProm.toFixed(1)} h` : ""}${e.bien.emoTop?.length ? ` · emociones: ${e.bien.emoTop.map(([x]) => x).join(", ")}` : ""}.</p>` : ""}
      <h2>Balance del mes</h2>${["comoEstuvo:¿Cómo estuvo el mes?", "logros:Logros", "desafios:Desafíos", "agradecido:Agradecido por", "mejorar:Puedo mejorar", "objetivosProximo:Objetivos del próximo mes", "notas:Notas"].map(x => { const [k, n] = x.split(":"); return txtInf(n, b[k]); }).join("") || `<p class="vacio">Sin balance escrito.</p>`}
      ${t && t.n ? `<h2>Finanzas</h2><div class="kpis"><div><span>Entró</span><b>${plata(t.ing)}</b></div><div><span>Salió</span><b>${plata(t.gas + t.aho + t.inv + (t.gen || 0))}</b></div><div><span>Disponible</span><b>${plata(t.disp)}</b></div><div><span>Ahorro e inversión</span><b>${t.tasa != null ? Math.round(t.tasa * 100) + " %" : "—"}</b></div></div>` : ""}`;
  }
  function anioInf(a) {
    const e = evalAnio(a), b = Store.get("balance", String(a)) || {}, t = !finOculto() && typeof totalesDe === "function" ? totalesDe(`${a}-`) : null;
    return `<div class="kpis"><div><span>Puntaje</span><b>${pctN(e.puntaje)}</b></div><div><span>Metas del año</span><b>${pctN(e.pMetas)}</b></div><div><span>Hábitos</span><b>${pctN(e.pHab)}</b></div><div><span>Bienestar</span><b>${pctN(e.pBien)}</b></div></div>
      <h2>Mes a mes</h2><table><tr><th>Mes</th><th></th><th class="n">Puntaje</th><th class="n">Metas</th><th class="n">Hábitos</th><th class="n">Bienestar</th></tr>${e.meses.map((x, i) => x ? `<tr><td>${mayus(MESES[i])}</td><td>${barraInf(x.puntaje)}</td><td class="n">${pctN(x.puntaje)}</td><td class="n">${pctN(x.pMetas)}</td><td class="n">${pctN(x.pHab)}</td><td class="n">${pctN(x.pBien)}</td></tr>` : "").join("")}</table>
      <h2>Brújula de ${a}</h2>${e.camps.some(x => x.meta) ? `<table><tr><th>Campamento</th><th>Meta</th><th></th><th class="n">Avance</th><th class="n">Sub-metas</th></tr>${e.camps.filter(x => x.meta).map(x => `<tr><td>${esc(NOMBRE_EJE[x.cod] || "Confluencia")}</td><td>${esc(x.meta.titulo)}</td><td>${barraInf(x.p)}</td><td class="n">${pctN(x.p)}</td><td class="n">${x.total ? `${x.ganadas} de ${x.total}` : "—"}</td></tr>`).join("")}</table>` : `<p class="vacio">Sin Brújula de este año.</p>`}
      ${e.bien?.registrados ? `<h2>Bienestar del año</h2><p>${e.bien.registrados} días registrados · índice de vida saludable ${e.bien.indice ?? "—"} %.</p>` : ""}
      <h2>Balance del año</h2>${["comoMeSenti:¿Cómo me sentí?", "positivo:Lo positivo", "negativo:Lo que no salió bien", "loMejor:Lo mejor del año", "objetivosProximo:Objetivos del próximo año"].map(x => { const [k, n] = x.split(":"); return txtInf(n, b[k]); }).join("") || `<p class="vacio">Sin balance escrito.</p>`}
      ${t && t.n ? `<h2>Finanzas del año</h2><div class="kpis"><div><span>Entró</span><b>${plata(t.ing)}</b></div><div><span>Salió</span><b>${plata(t.gas + t.aho + t.inv + (t.gen || 0))}</b></div><div><span>Disponible</span><b>${plata(t.disp)}</b></div><div><span>Ahorro e inversión</span><b>${t.tasa != null ? Math.round(t.tasa * 100) + " %" : "—"}</b></div></div>` : ""}`;
  }
  ACC.informeRuta = (_, arg) => {
    const [t, v] = String(arg).split(":");
    if (t === "a") return abrirInforme(docInf(`Informe de ${v}`, "Tu año en Ruta a la Cima", anioInf(Number(v))), `informe-${v}`);
    if (t === "m") { const [a, m] = v.split("-").map(Number); return abrirInforme(docInf(`Informe de ${MESES[m - 1]} ${a}`, "Tu mes en Ruta a la Cima", mesInf(a, m) + `<h2>Día a día</h2>` + fechasHasta(a, m).map(diaInf).join("")), `informe-${v}`); }
    // Días: uno solo o un rango (se elige en un cuadro pequeño)
    dialogoDias(v || iso(hoyFecha()));
  };
  function dialogoDias(f) {
    document.getElementById("dlgInf")?.remove();
    const d = document.createElement("dialog"); d.id = "dlgInf"; d.className = "dlg-inf";
    d.innerHTML = `<form method="dialog"><h3>Informe de días</h3><p class="suave">Elige un día o un rango. Se abre listo para «Guardar como PDF».</p>
      <div class="form-2">${campo("Desde", `<input type="date" name="desde" value="${f}" max="${iso(hoyFecha())}" required>`)}${campo("Hasta", `<input type="date" name="hasta" value="${f}" max="${iso(hoyFecha())}" required>`)}</div>
      <div class="botones"><button class="btn" value="cancelar" type="submit">Cancelar</button><button class="btn lleno" value="ok" type="submit">${ic("descargar")} Descargar</button></div></form>`;
    document.body.append(d); d.showModal();
    d.addEventListener("close", () => {
      if (d.returnValue === "ok") {
        const fd = new FormData(d.querySelector("form")); let a = String(fd.get("desde")), b = String(fd.get("hasta")); if (a > b) [a, b] = [b, a];
        const dias = []; for (let x = deIso(a); x <= deIso(b) && dias.length < 92; x.setDate(x.getDate() + 1)) dias.push(iso(x));
        abrirInforme(docInf(dias.length === 1 ? "Informe del día" : `Informe de ${dias.length} días`, dias.length === 1 ? mayus(deIso(a).toLocaleDateString("es", { weekday: "long", day: "numeric", month: "long", year: "numeric" })) : `Del ${deIso(a).toLocaleDateString("es")} al ${deIso(b).toLocaleDateString("es")}`, dias.map(diaInf).join("")), `informe-${a}${a !== b ? "_" + b : ""}`);
      }
      d.remove();
    });
  }
})();
