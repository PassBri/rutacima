"use strict";
/* ======================================================================
 * Bienestar en cada día, mes y año (igual que la app, ui/bienestar/BienestarDia.kt).
 * · El día: tu clima interior (emoción, intensidad, causas, herramientas y nota) y los seis hábitos de
 *   vida saludable con las metas de la OMS. Se controla todos los días, por eso vive en el día.
 * · El mes y el año: el acumulado. Días registrados, índice de vida saludable, clima de cada día,
 *   reparto de climas, emociones y causas que se repiten, promedios y totales de cada hábito.
 * Los datos son los mismos de Bienestar ("bien#aaaa-mm-dd" y el agua de la agenda). Es privado.
 * ====================================================================== */

/** Puntaje de un día (0 a 1) y cuántos hábitos se cumplieron; null si no hay nada anotado. */
function puntajeDia(f) {
  const p = pilaresDia(f), v = Object.values(p).filter(x => x != null);
  if (!v.length) return null;
  return { p, puntaje: v.reduce((a, x) => a + x, 0) / SALUD_PILARES.length, cumplidos: v.filter(x => x >= .99).length };
}

/** El acumulado de bienestar de una lista de fechas (iso). */
function statsBien(fechas) {
  const r = { dias: fechas.length, conClima: 0, conDatos: 0, clima: { sol: 0, tormenta: 0, niebla: 0, valle: 0 }, emo: {}, causas: {}, herr: 0,
    sueno: [], mov: 0, movDias: 0, fyv: [], agua: [], aire: 0, pant: 0, pantDias: 0, puntajes: [], metas: Object.fromEntries(SALUD_PILARES.map(([k]) => [k, 0])), intens: [] };
  fechas.forEach(f => {
    const b = bienDia(f), a = aguaDe(f), pd = puntajeDia(f);
    if (b.clima) { r.conClima++; r.clima[b.clima]++; if (b.int) r.intens.push(b.int); }
    if (b.emo) r.emo[b.emo] = (r.emo[b.emo] || 0) + 1;
    if (b.clima === "tormenta" || b.clima === "niebla") (b.causas || []).forEach(c => { r.causas[c] = (r.causas[c] || 0) + 1; });
    r.herr += (b.herr || []).length;
    if (b.sueno != null) r.sueno.push(b.sueno);
    if (b.mov != null) { r.mov += b.mov; r.movDias++; }
    if (b.fyv != null) r.fyv.push(b.fyv);
    if (a) r.agua.push(a);
    if (b.aire != null) r.aire += b.aire;
    if (b.pant != null) { r.pantDias++; if (b.pant) r.pant++; }
    if (pd) { r.conDatos++; r.puntajes.push(pd.puntaje); Object.entries(pd.p).forEach(([k, v]) => { if (v != null && v >= .99) r.metas[k]++; }); }
  });
  const prom = l => l.length ? l.reduce((a, x) => a + x, 0) / l.length : null;
  r.indice = r.puntajes.length ? Math.round(prom(r.puntajes) * 100) : null;
  r.suenoProm = prom(r.sueno); r.fyvProm = prom(r.fyv); r.aguaProm = prom(r.agua); r.intProm = prom(r.intens);
  r.registrados = fechas.filter(f => bienDia(f).clima || puntajeDia(f)).length;
  r.emoTop = Object.entries(r.emo).sort((a, b) => b[1] - a[1]).slice(0, 4);
  r.causasTop = Object.entries(r.causas).sort((a, b) => b[1] - a[1]).slice(0, 3);
  r.dominante = r.conClima ? Object.entries(r.clima).sort((a, b) => b[1] - a[1])[0][0] : null;
  return r;
}
const fechasMes = (a, m, hasta = hoyFecha()) => { const r = []; for (let d = 1; d <= diasEnMes(a, m); d++) { const f = new Date(a, m - 1, d); if (f <= hasta) r.push(iso(f)); } return r; };
const num1 = x => x == null ? "—" : x.toLocaleString("es", { maximumFractionDigits: 1 });

/* ---------- El día ---------- */
function bienHojaDia(fecha) {
  const f = deIso(fecha); if (f > hoyFecha()) return "";
  const b = bienDia(fecha), cl = b.clima ? CLIMAS[b.clima] : null, agua = aguaDe(fecha), pd = puntajeDia(fecha);
  const esHoy = fecha === iso(hoyFecha());
  const fuerte = (b.clima === "tormenta" || b.clima === "niebla") && (b.int || 0) >= 5;
  const paso = (k, v, max, st, suf, etq) => `<div class="bd-paso"><span>${etq}</span><div class="bd-ctrl"><button type="button" class="rb" data-acc="bienValor" data-arg="${k}|${Math.max(0, (v || 0) - st)}" aria-label="Menos ${etq.toLowerCase()}">−</button><b>${v ?? "—"}${v != null ? `<small> ${suf}</small>` : ""}</b><button type="button" class="rb" data-acc="bienValor" data-arg="${k}|${Math.min(max, (v || 0) + st)}" aria-label="Más ${etq.toLowerCase()}">+</button></div></div>`;
  const pil = pd?.p || {};
  const estadoP = k => pil[k] == null ? "" : pil[k] >= .99 ? " ok" : pil[k] >= .6 ? " medio" : " bajo";
  return `<section class="hoja bien-dia sec-dia" id="sd-bienestar" data-sd="bienestar" data-bien-f="${fecha}">
    ${cabSec("bienestar", "Bienestar del día", `${esHoy ? "Tu clima interior y tus hábitos de hoy." : "Tu clima interior y tus hábitos de este día."} Es privado.`,
      pd ? `<div class="bd-anillo" style="--p:${pd.cumplidos / 6}" role="img" aria-label="${pd.cumplidos} de 6 hábitos cumplidos"><b>${pd.cumplidos}</b><small>/6</small></div>` : "")}

    <h4 class="bd-sub">¿Cómo estuvo tu clima interior?</h4>
    <div class="bd-climas" role="radiogroup" aria-label="Clima interior">${Object.entries(CLIMAS).map(([k, c]) => `<button type="button" role="radio" class="bd-clima${b.clima === k ? " si" : ""}" style="--c:${c.c}" data-acc="bienClima" data-arg="${k}" aria-checked="${b.clima === k}">${climaSvg(k)}<span>${c.n}</span></button>`).join("")}</div>
    ${cl ? `<div class="bd-detalle">
      <div class="bien-chips" aria-label="Emoción">${cl.e.map(e => `<button type="button" class="chip${b.emo === e ? " si" : ""}" style="--c:${cl.c}" data-acc="bienEmo" data-arg="${e}" aria-pressed="${b.emo === e}">${e}</button>`).join("")}</div>
      <div class="bd-int"><span>Intensidad</span><div class="bien-int" role="group" aria-label="Intensidad">${[1, 2, 3, 4, 5].map(n => `<button type="button" class="${(b.int || 0) >= n ? "si" : ""}" style="--c:${cl.c}" data-acc="bienInt" data-arg="${n}" aria-label="Intensidad ${n} de 5">${n}</button>`).join("")}</div></div>
      <details class="bd-mas"${(b.causas || []).length || b.nota || (b.herr || []).length ? " open" : ""}><summary>Qué lo provocó, herramientas y nota${ic("abajo")}</summary>
        <div class="bien-chips" style="margin-top:10px" aria-label="Causas">${CAUSAS.map(c => `<button type="button" class="chip${(b.causas || []).includes(c) ? " si" : ""}" data-acc="bienCausa" data-arg="${c}" aria-pressed="${(b.causas || []).includes(c)}">${c}</button>`).join("")}</div>
        <div class="bd-herr">${HERRAMIENTAS.map(([k, n, d]) => `<button type="button" class="bd-h${(b.herr || []).includes(k) ? " si" : ""}" data-acc="${k === "caja" ? "bienRespirar" : "bienHerr"}" data-arg="${k}" aria-pressed="${(b.herr || []).includes(k)}" title="${esc(d)}">${(b.herr || []).includes(k) ? ic("check") : ""}${n}</button>`).join("")}</div>
        <label class="campo-m3"><span>Nota · solo para ti</span><textarea rows="3" maxlength="600" data-acc-change="bienNota" placeholder="Qué pasó, qué pensaste, qué necesitas…">${esc(b.nota || "")}</textarea></label>
      </details></div>` : ""}
    ${fuerte ? `<div class="bien-apoyo bd-apoyo">${ic("comentario")}<div><b>No tienes que pasar esto solo</b><p>Habla con alguien de confianza o con un profesional. En Colombia la <b>Línea 106</b> atiende gratis las 24 horas; si hay peligro inmediato, llama al <b>123</b>. En otros países: <a href="https://findahelpline.com" target="_blank" rel="noopener">findahelpline.com</a>.</p></div></div>` : ""}

    <div class="sec-div"></div>
    <h4 class="bd-sub">Vida saludable</h4>
    <div class="bd-habitos">
      <div class="bd-h-it${estadoP("sueno")}">${paso("sueno", b.sueno, 14, .5, "h", "Sueño")}<small>Meta 7 a 9 h</small></div>
      <div class="bd-h-it${estadoP("mov")}">${paso("mov", b.mov, 600, 10, "min", "Actividad física")}<small>Meta 30 min al día</small></div>
      <div class="bd-h-it${estadoP("fyv")}"><div class="bd-paso"><span>Frutas y verduras</span><div class="bien-porciones">${[1, 2, 3, 4, 5].map(n => `<button type="button" class="${(b.fyv || 0) >= n ? "si" : ""}" data-acc="bienValor" data-arg="fyv|${(b.fyv || 0) === n ? n - 1 : n}" aria-label="${n} porciones">${n}</button>`).join("")}</div></div><small>Meta 5 porciones</small></div>
      <div class="bd-h-it${estadoP("aire")}">${paso("aire", b.aire, 600, 10, "min", "Aire libre")}<small>Meta 20 min</small></div>
      <div class="bd-h-it${estadoP("pant")}"><label class="bd-paso bd-sw"><span>Sin pantallas al dormir</span><input type="checkbox" role="switch" ${b.pant ? "checked" : ""} data-acc-change="bienPant"></label><small>La última hora sin celular ni TV</small></div>
      ${(() => { const ag = Store.get("agenda", fecha) || {}; // Energía y agua viven aquí (antes estaban en el cierre)
        return `<div class="bd-h-it bd-ancho"><div class="bd-paso"><span>Energía</span></div>${energiaMontes(fecha, ag.energia || 0)}</div>
      <div class="bd-h-it bd-ancho${estadoP("agua")}"><div class="bd-paso"><span>Agua</span></div>${vasosDeAgua(fecha, ag.agua || 0)}</div>`; })()}
    </div>
    <div class="bd-pie"><button class="btn mini" data-acc="bienRespirar">${ic("bienestar")} Respirar un minuto</button><button class="btn mini" data-acc="abrirBien" data-arg="practica">${ic("aprende")} Práctica de la semana</button><button class="btn mini" data-acc="abrirBien" data-arg="clima">${ic("grafica")} Tus patrones</button></div>
  </section>`;
}

/* ---------- Gráficas pequeñas ---------- */
function barraClimas(c, total, alto = 12) {
  if (!total) return `<div class="bm-barra vacia" style="height:${alto}px"></div>`;
  return `<div class="bm-barra" style="height:${alto}px">${Object.entries(CLIMAS).filter(([k]) => c[k]).map(([k, x]) => `<i style="flex:${c[k]};background:${x.c}" title="${x.n}: ${c[k]}"></i>`).join("")}</div>`;
}
function kpi(v, etq, sub = "") { return `<div class="bm-kpi"><b>${v}</b><span>${etq}</span>${sub ? `<small>${sub}</small>` : ""}</div>`; }
function metasPilares(s, base) {
  return `<div class="bm-pilares">${SALUD_PILARES.map(([k, n]) => { const p = base ? Math.round(s.metas[k] / base * 100) : 0;
    return `<div><span>${n}</span><div class="barra"><i style="width:${p}%"></i></div><b>${p} %</b></div>`; }).join("")}</div>`;
}

/* ---------- El mes ---------- */
function bienResumenMes(a, m) {
  const hoy = hoyFecha(); if (new Date(a, m - 1, 1) > hoy) return "";
  const fechas = fechasMes(a, m), s = statsBien(fechas), ym = `${a}-${String(m).padStart(2, "0")}`;
  const ini = (new Date(a, m - 1, 1).getDay() + 6) % 7;
  let cal = ["L", "M", "X", "J", "V", "S", "D"].map(x => `<i class="bm-dsem">${x}</i>`).join("") + `<i class="bm-vacio"></i>`.repeat(ini);
  for (let d = 1; d <= diasEnMes(a, m); d++) {
    const f = iso(new Date(a, m - 1, d)), b = bienDia(f), c = b.clima ? CLIMAS[b.clima] : null, pd = new Date(a, m - 1, d) <= hoy ? puntajeDia(f) : null;
    cal += `<button class="bm-d${c ? " con" : ""}${f === iso(hoy) ? " hoy" : ""}" data-ir="ruta" data-arg="d:${f}" style="${c ? `--c:${c.c}` : ""}" aria-label="${d}${c ? `, ${c.n}${b.emo ? ", " + b.emo : ""}` : ""}${pd ? `, ${pd.cumplidos} de 6 hábitos` : ""}">
      ${c ? climaSvg(b.clima) : `<span>${d}</span>`}${pd ? `<em style="--p:${pd.cumplidos / 6}"></em>` : ""}</button>`;
  }
  const vacio = !s.registrados;
  return `<section class="hoja bien-mes" aria-labelledby="bmTit-${ym}">
    <div class="bd-cab"><span class="bd-ic">${ic("bienestar")}</span><div><h3 id="bmTit-${ym}">Bienestar del mes</h3><p class="suave">${vacio ? "Anota tu clima y tus hábitos cada día y aquí verás el acumulado." : `${s.registrados} de ${fechas.length} días registrados.`}</p></div>
      ${s.indice != null ? `<div class="bd-anillo grande" style="--p:${s.indice / 100}" role="img" aria-label="Índice de vida saludable ${s.indice} %"><b>${s.indice}</b><small>%</small></div>` : ""}</div>
    ${vacio ? "" : `<div class="bm-kpis">
      ${kpi(s.dominante ? climaSvg(s.dominante) : "—", "Clima que más se repitió", s.dominante ? CLIMAS[s.dominante].n : "")}
      ${kpi(num1(s.suenoProm) + (s.suenoProm != null ? " h" : ""), "Sueño promedio", `${s.metas.sueno} noches de 7 h o más`)}
      ${kpi(s.mov.toLocaleString("es") + " min", "Actividad física", `${num1(s.mov / Math.max(1, fechas.length) * 7)} min por semana`)}
      ${kpi(num1(s.fyvProm), "Porciones de frutas y verduras al día", `${s.metas.fyv} días con 5`)}
      ${kpi(num1(s.aguaProm), "Cantimploras al día", `${s.metas.agua} días con 8`)}
      ${kpi(s.aire.toLocaleString("es") + " min", "Aire libre y sol", `${s.pant} noches sin pantallas`)}
    </div>`}
    <h4 class="bd-sub">Cada día, su clima</h4>
    <div class="bm-cal">${cal}</div>
    ${s.conClima ? `<h4 class="bd-sub">Reparto de tu clima</h4>${barraClimas(s.clima, s.conClima, 14)}
      <div class="bien-ley">${Object.entries(CLIMAS).map(([k, c]) => `<span><i style="background:${c.c}"></i>${c.n} · ${s.clima[k]}</span>`).join("")}</div>
      ${s.emoTop.length ? `<p class="bm-txt"><b>Emociones que más sentiste:</b> ${s.emoTop.map(([e, n]) => `${e} (${n})`).join(", ")}.</p>` : ""}
      ${s.causasTop.length ? `<p class="bm-txt"><b>Lo que más te trajo tormenta o niebla:</b> ${s.causasTop.map(([c, n]) => `${c.toLowerCase()} (${n})`).join(", ")}.</p>` : ""}
      ${s.herr ? `<p class="bm-txt"><b>Herramientas que usaste:</b> ${s.herr} ${s.herr === 1 ? "vez" : "veces"}.</p>` : ""}` : ""}
    ${s.conDatos ? `<h4 class="bd-sub">Días que cumpliste cada meta</h4>${metasPilares(s, s.conDatos)}` : ""}
  </section>`;
}

/* ---------- El año ---------- */
function bienResumenAnio(a) {
  const hoy = hoyFecha(); if (a > hoy.getFullYear()) return "";
  const meses = []; for (let m = 1; m <= 12; m++) meses.push(new Date(a, m - 1, 1) <= hoy ? statsBien(fechasMes(a, m)) : null);
  const todas = []; for (let m = 1; m <= 12; m++) if (new Date(a, m - 1, 1) <= hoy) todas.push(...fechasMes(a, m));
  const s = statsBien(todas);
  if (!s.registrados) return "";
  const max = 100;
  return `<section class="hoja bien-anio" aria-labelledby="baTit-${a}">
    <div class="bd-cab"><span class="bd-ic">${ic("bienestar")}</span><div><h3 id="baTit-${a}">Bienestar de ${a}</h3><p class="suave">${s.registrados} días registrados. Toca un mes para verlo.</p></div>
      ${s.indice != null ? `<div class="bd-anillo grande" style="--p:${s.indice / 100}" role="img" aria-label="Índice de vida saludable del año ${s.indice} %"><b>${s.indice}</b><small>%</small></div>` : ""}</div>
    <div class="bm-kpis">
      ${kpi(s.dominante ? climaSvg(s.dominante) : "—", "Clima del año", s.dominante ? CLIMAS[s.dominante].n : "")}
      ${kpi(num1(s.suenoProm) + (s.suenoProm != null ? " h" : ""), "Sueño promedio", `${s.metas.sueno} noches de 7 h o más`)}
      ${kpi(Math.round(s.mov / 60).toLocaleString("es") + " h", "Actividad física en el año", `${s.mov.toLocaleString("es")} min`)}
      ${kpi(s.pant.toLocaleString("es"), "Noches sin pantallas", `${Math.round(s.aire / 60).toLocaleString("es")} h al aire libre`)}
    </div>
    <h4 class="bd-sub">Mes a mes</h4>
    <div class="ba-meses" role="list">${meses.map((x, i) => `<button role="listitem" class="ba-mes" data-ir="ruta" data-arg="m:${a}-${String(i + 1).padStart(2, "0")}" ${x ? "" : "disabled"} aria-label="${mayus(MESES[i])}${x?.indice != null ? `: índice ${x.indice} %` : ""}${x?.dominante ? `, clima ${CLIMAS[x.dominante].n}` : ""}">
      <span class="ba-col"><i style="height:${x?.indice != null ? Math.max(4, x.indice / max * 100) : 0}%"></i></span>
      <b>${x?.indice != null ? x.indice : ""}</b>${barraClimas(x?.clima || {}, x?.conClima || 0, 8)}<small>${MES_C[i]}</small></button>`).join("")}</div>
    <div class="bien-ley"><span><i style="background:var(--ok)"></i>Índice de vida saludable</span>${Object.entries(CLIMAS).map(([, c]) => `<span><i style="background:${c.c}"></i>${c.n}</span>`).join("")}</div>
    ${s.emoTop.length ? `<p class="bm-txt"><b>Emociones del año:</b> ${s.emoTop.map(([e, n]) => `${e} (${n})`).join(", ")}.</p>` : ""}
    ${s.causasTop.length ? `<p class="bm-txt"><b>Lo que más te trajo tormenta o niebla:</b> ${s.causasTop.map(([c, n]) => `${c.toLowerCase()} (${n})`).join(", ")}.</p>` : ""}
    ${s.conDatos ? `<h4 class="bd-sub">Días que cumpliste cada meta</h4>${metasPilares(s, s.conDatos)}` : ""}
  </section>`;
}

/* Índice del día: salta a cada sección con un desplazamiento suave */
Object.assign(ACC, {
  irSecDia(_, k) {
    const el = document.getElementById("sd-" + k); if (!el) return;
    el.scrollIntoView({ behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth", block: "start" });
    el.classList.add("sd-foco"); setTimeout(() => el.classList.remove("sd-foco"), 1200);
  },
});
