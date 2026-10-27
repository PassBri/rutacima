"use strict";
/* ======================================================================
 * Informe de la Brújula 1·9·81 (igual que InformeBrujula.kt en la app): la cumbre, el avance, el ritmo del
 * Método Cima 9×52, la imagen del 9×9, cada campamento con sus 8 pasos (estado y jornadas) y las metas
 * que lo sostienen. Se arma como página aparte con «Guardar como PDF».
 * ====================================================================== */
async function informeBrujula() {
  const d = Mandala.datos(), anio = estado.brujulaAnio || null, hoyD = new Date();
  const fecha = hoyD.toLocaleDateString("es", { day: "numeric", month: "long", year: "numeric" });
  const nombre = esc(perfil()?.nombre || ""), r = d.ritmo, pctC = Math.round(mdPct(d, -1));
  // El 9×9 en texto: sin colores ni imágenes, con líneas más gruesas entre los bloques de 3×3
  const celdaTxt = cel => cel.tipo === "CUMBRE" ? `<b>${esc(d.titulo)}</b>` : cel.tipo === "CAMPAMENTO"
    ? `<b>${d.camps[cel.camp] ? esc(mdNombre(d.camps[cel.camp])) : esc(Mandala.nombre(Mandala.CAMPAMENTOS_FIJOS[cel.camp]))}</b>`
    : (() => { const c = d.camps[cel.camp]; if (!c) return ""; const t = respuesta(Mandala.clave(c.id, cel.paso)).trim(); return t ? esc(t) + (Mandala.estado(c.id, cel.paso) === "HECHO" ? " ✓" : "") : ""; })();
  const grilla = `<table class="g9">${[0, 1, 2, 3, 4, 5, 6, 7, 8].map(f => `<tr>${[0, 1, 2, 3, 4, 5, 6, 7, 8].map(c => {
    const cel = Mandala.celda(f, c), cls = [c % 3 === 0 ? "bi" : "", f % 3 === 0 ? "bt" : "", c === 8 ? "bd" : "", f === 8 ? "bb" : "", cel.tipo !== "PASO" ? "cab" : ""].filter(Boolean).join(" ");
    return `<td class="${cls}">${celdaTxt(cel)}</td>`; }).join("")}</tr>`).join("")}</table>`;
  const logo = new URL("iconos/logo-horizontal-crema.svg", location.href).href, simbolo = new URL("iconos/simbolo.svg", location.href).href;
  let sec = 0; const h2 = t => `<h2><span>${String(++sec).padStart(2, "0")}</span>${t}</h2>`;
  const EST = { VACIO: ["Por escribir", "#5A666B", "#EEF1F2"], ESCRITO: ["En camino", "#8A5A00", "#FBEFD8"], HECHO: ["Ganado", "#1E7B4A", "#E3F2E9"] };
  const pill = e => `<span class="pill" style="color:${EST[e][1]};background:${EST[e][2]}">${EST[e][0]}</span>`;
  const anioMetas = anio || hoyD.getFullYear();
  const camps = d.camps.map((c, i) => {
    const cod = d.cod(i), col = d.color(i);
    const cab = `<header><span class="rumbo" style="background:${col}">${Mandala.RUMBOS[i]}</span><div><b>${c ? esc(mdNombre(c)) : "Campamento libre"}</b><small>${esc(Mandala.nombre(cod))}</small></div><span class="pct">${c ? Math.round(mdPct(d, i)) : 0} %</span></header>`;
    if (!c) return `<section class="camp">${cab}<p class="vacio">Aún no creas este campamento.</p></section>`;
    const filas = [0, 1, 2, 3, 4, 5, 6, 7].map(p => {
      const t = respuesta(Mandala.clave(c.id, p)).trim(), e = Mandala.estado(c.id, p), x = d.pasoCima(i, p), req = x.req || r.dificultad;
      const jor = e === "HECHO" ? (x.cumplidoEn ? new Date(x.cumplidoEn + "T12:00").toLocaleDateString("es", { day: "numeric", month: "short", year: "numeric" }) : "✓") : e === "ESCRITO" ? `${x.jornadas.length} de ${req}` : "—";
      return `<tr><td class="num">${p + 1}</td><td><small>${Mandala.FASES[p]}</small>${t ? esc(t) : `<span class="suave">—</span>`}</td><td class="n">${pill(e)}</td><td class="n">${jor}</td></tr>`;
    }).join("");
    const metas = cod === "CAM" ? [] : metasDeCampamento(i).filter(m => m.anio === anioMetas);
    return `<section class="camp">${cab}<table><thead><tr><th>#</th><th>Paso</th><th class="n">Estado</th><th class="n">Jornadas</th></tr></thead><tbody>${filas}</tbody></table>
      ${metas.length ? `<p class="metas"><b>Metas de ${anioMetas} que lo sostienen:</b> ${metas.map(m => esc(m.titulo)).join(" · ")}</p>` : ""}</section>`;
  }).join("");
  const css = `@import url("https://fonts.googleapis.com/css2?family=Roboto+Serif:opsz,wght@8..144,500;8..144,600&family=Roboto:wght@400;500;700&display=swap");
    *{box-sizing:border-box}body{font:13.5px/1.55 Roboto,system-ui,sans-serif;color:#1D2427;margin:0;background:#E6EAEC;-webkit-print-color-adjust:exact;print-color-adjust:exact}
    .doc{max-width:900px;margin:24px auto;background:#fff;border-radius:14px;overflow:hidden;box-shadow:0 10px 40px -12px rgb(0 0 0/.25)}
    .banda{background:#6B2A1A;color:#F3EDE0;padding:22px 44px;display:flex;align-items:center;justify-content:space-between}.banda img{height:46px}
    .banda .meta{text-align:right;font-size:12px;letter-spacing:.06em;text-transform:uppercase;opacity:.85}
    .portada{padding:30px 44px 18px;border-bottom:1px solid #DCE2E4}h1{font:600 30px/1.15 "Roboto Serif",Georgia,serif;margin:0}.sub{color:#5A666B;margin:6px 0 0}
    .cumbre{margin:18px 0 0;padding:16px 20px;border-radius:12px;background:#FBF8F2;border:1px solid #E8DCC4;font:600 19px/1.4 "Roboto Serif",Georgia,serif;color:#6B2A1A}
    .cumbre small{display:block;font:500 11px Roboto,sans-serif;letter-spacing:.1em;text-transform:uppercase;color:#86683A;margin-bottom:4px}
    .cuerpo{padding:8px 44px 36px}h2{display:flex;align-items:baseline;gap:12px;font:600 19px "Roboto Serif",Georgia,serif;color:#6B2A1A;margin:30px 0 12px;break-after:avoid}
    h2 span{font:500 12px Roboto,sans-serif;color:#86683A;letter-spacing:.1em}
    .kpis{display:grid;grid-template-columns:repeat(4,1fr);gap:10px}.kpi{border-radius:10px;background:#F5F7F8;padding:10px 12px}.kpi span{display:block;font-size:11.5px;color:#5A666B}.kpi b{font-size:17px;font-variant-numeric:tabular-nums}
    .barra{height:8px;border-radius:4px;background:#EEF1F2;overflow:hidden;margin:12px 0 4px}.barra i{display:block;height:100%;background:#C9973B;border-radius:4px}
    .ritmo{margin:12px 0 0;padding:12px 16px;border-left:3px solid #86683A;background:#FBF8F2;border-radius:0 10px 10px 0}.ritmo p{margin:3px 0}
    table.g9{table-layout:fixed;border-collapse:collapse;width:100%;break-inside:avoid}table.g9 td{border:1px solid #9AA3A7;padding:4px;height:64px;text-align:center;vertical-align:middle;font-size:9.5px;line-height:1.25;color:#1D2427;overflow:hidden;word-break:break-word}
    table.g9 td.bi{border-left:2.5px solid #1D2427}table.g9 td.bt{border-top:2.5px solid #1D2427}table.g9 td.bd{border-right:2.5px solid #1D2427}table.g9 td.bb{border-bottom:2.5px solid #1D2427}table.g9 td.cab{font-size:10px}
    .camp{border:1px solid #DCE2E4;border-radius:12px;padding:14px 16px;margin:12px 0;break-inside:avoid}
    .camp header{display:grid;grid-template-columns:auto 1fr auto;gap:12px;align-items:center;margin-bottom:8px}.camp header b{display:block;font:600 15.5px "Roboto Serif",Georgia,serif}.camp header small{color:#5A666B}
    .rumbo{min-width:34px;height:34px;padding:0 6px;border-radius:10px;color:#fff;display:grid;place-items:center;font-weight:700;font-size:12px}.pct{font:600 18px "Roboto Serif",Georgia,serif;font-variant-numeric:tabular-nums}
    table{width:100%;border-collapse:collapse}th,td{padding:6px 8px;border-bottom:1px solid #EEF1F2;text-align:left;vertical-align:top}thead th{font:500 11px Roboto,sans-serif;color:#5A666B;text-transform:uppercase;letter-spacing:.05em}
    td small{display:block;font-size:10.5px;color:#86683A;font-weight:700;letter-spacing:.04em;text-transform:uppercase}.num{width:26px;color:#5A666B;font-variant-numeric:tabular-nums}
    .n{text-align:right;white-space:nowrap}.pill{display:inline-block;padding:2px 10px;border-radius:999px;font-size:11.5px;font-weight:700}.suave,.vacio{color:#5A666B}.metas{margin:8px 0 0;font-size:12.5px}
    .nota{margin-top:28px;padding-top:12px;border-top:1px solid #DCE2E4;font-size:11.5px;color:#5A666B;display:flex;gap:10px;align-items:center}.nota img{width:20px;height:20px;opacity:.7}
    .barra-acc{position:sticky;top:0;z-index:2;display:flex;gap:8px;justify-content:center;padding:12px;background:#E6EAEC}
    .barra-acc button{font:500 14px Roboto,sans-serif;padding:10px 20px;border-radius:999px;border:0;background:#6B2A1A;color:#fff;cursor:pointer}.barra-acc button.sec{background:#fff;color:#6B2A1A}
    @media (max-width:700px){.banda,.portada,.cuerpo{padding-left:18px;padding-right:18px}.kpis{grid-template-columns:1fr 1fr}}
    @media print{body{background:#fff}.doc{margin:0;max-width:none;border-radius:0;box-shadow:none}.barra-acc{display:none}@page{size:A4;margin:12mm 0 14mm}
      .banda{padding:16px 14mm}.portada,.cuerpo{padding-left:14mm;padding-right:14mm}tr{break-inside:avoid}.kpis{grid-template-columns:repeat(4,1fr)}}`;
  return `<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Brújula 1·9·81 · ${anio || "General"}</title><style>${css}</style></head><body>
    <div class="barra-acc"><button onclick="print()">Guardar como PDF</button><button class="sec" onclick="close()">Cerrar</button></div>
    <div class="doc"><div class="banda"><img src="${logo}" alt="SuRoute"><div class="meta">Brújula 1·9·81<br>${anio ? "Año " + anio : "General · toda la vida"}</div></div>
    <div class="portada"><h1>Informe de mi Brújula</h1><p class="sub">${nombre ? nombre + " · " : ""}${fecha}</p>
      <div class="cumbre"><small>Mi cumbre</small>${esc(d.titulo)}</div></div>
    <div class="cuerpo">
      ${h2("Cómo voy")}<div class="kpis"><div class="kpi"><span>Avance de la cumbre</span><b>${pctC} %</b></div><div class="kpi"><span>Pasos escritos</span><b>${d.escritos} de 64</b></div>
        <div class="kpi"><span>Pasos ganados</span><b>${d.hechos} de 64</b></div><div class="kpi"><span>Fotos de evidencia</span><b>${d.evidencias}</b></div></div>
      <div class="barra"><i style="width:${pctC}%"></i></div>
      <div class="ritmo"><p><b>Método Cima 9×52 · Tu montaña: ${r.mm.toLocaleString("es")} de 8.848.000 mm</b></p>
        <p>Tramo ${r.tramo + 1} de 52 · un paso nuevo pide ${r.dificultad} ${r.dificultad === 1 ? "jornada" : "jornadas"} · ${r.adelanto >= 0 ? `vas ${r.adelanto.toLocaleString("es", { maximumFractionDigits: 1 })} pasos adelante del ritmo` : `te faltan ${(-r.adelanto).toLocaleString("es", { maximumFractionDigits: 1 })} pasos para ir al ritmo`}</p>
        ${d.confluencias ? `<p>${d.confluencias} ${d.confluencias === 1 ? "paso de confluencia" : "pasos de confluencia"}: acciones que movieron varios ejes.</p>` : ""}
        ${d.enNiebla ? `<p>${d.enNiebla} ${d.enNiebla === 1 ? "paso" : "pasos"} en la niebla (más de ${Travesia.NIEBLA_DIAS} días sin avanzar).</p>` : ""}</div>
      ${h2("Mi Brújula 9×9")}${grilla}
      ${h2("Campamentos y ruta")}${camps}
      <div class="nota"><img src="${simbolo}" alt="">Generado por SuRoute · Brújula 1·9·81 del Método Ruta a la Cima, inspirada en la Mandala Chart de Yasuo Matsumura (1979).</div>
    </div></div></body></html>`;
}
ACC.informeBrujula = async el => {
  const w = window.open("", "_blank");
  if (el) el.disabled = true;
  try {
    const html = await informeBrujula();
    if (!w) { const a = Object.assign(document.createElement("a"), { href: URL.createObjectURL(new Blob([html], { type: "text/html" })), download: `brujula-${iso(new Date())}.html` }); a.click(); toast("Informe descargado: ábrelo y elige «Guardar como PDF»"); return; }
    w.document.open(); w.document.write(html); w.document.close(); toast("Informe listo: usa «Guardar como PDF» para descargarlo");
  } catch { w?.close(); toast("No se pudo armar el informe"); }
  finally { if (el) el.disabled = false; }
};
