/* ======================================================================
 * Control financiero (igual que en la app, domain/model/Finanzas.kt).
 * El día guarda ingresos, gastos, ahorro e inversión como texto libre en su agenda ("$ 50.000", "12,5").
 * El mes los suma en su balance, con el presupuesto de gastos en la respuesta "fin#<aaaa-mm>#presupuesto".
 * ====================================================================== */
const FIN_WB = "finanzas";
const claveFinPresupuesto = ym => `fin#${ym}#presupuesto`;
/** "$ 1.250.000" = 1250000, "12,5" = 12.5, "1,250" = 1250, "-3.000" = -3000. */
function montoFin(t) {
  if (!t || !String(t).trim()) return 0;
  const neg = String(t).trim().startsWith("-"), s = String(t).replace(/[^\d.,]/g, "");
  if (!/\d/.test(s)) return 0;
  const ult = Math.max(s.lastIndexOf("."), s.lastIndexOf(",")), cola = s.length - ult - 1;
  const v = ult >= 0 && cola >= 1 && cola <= 2 ? Number((s.slice(0, ult).replace(/\D/g, "") || "0") + "." + s.slice(ult + 1)) : Number(s.replace(/\D/g, ""));
  return neg ? -v : v;
}
const fmtFin = v => v.toLocaleString("es-CO", { maximumFractionDigits: v % 1 ? 2 : 0 });
const CAMPOS_FIN = [["ingresos", "Ingresos"], ["gastos", "Gastos"], ["ahorro", "Ahorro"], ["inversion", "Inversión"]];
function finDia(ag) {
  const d = Object.fromEntries(CAMPOS_FIN.map(([k]) => [k, montoFin(ag?.[k])]));
  d.disponible = d.ingresos - d.gastos - d.ahorro - d.inversion;
  d.vacio = CAMPOS_FIN.every(([k]) => !d[k]);
  return d;
}
/** Día: los cuatro montos y lo que queda libre. */
function finanzasDia(fecha, ag) {
  const d = finDia(ag);
  return hoja(`<h3 style="margin:0 0 4px">Control financiero</h3><p class="suave" style="margin:0 0 12px">Anota lo que entró y salió hoy. El mes lo suma en su balance.</p>
    <div class="fin-campos">${CAMPOS_FIN.map(([k, t]) => campo(t, `<input type="text" inputmode="decimal" autocomplete="off" ${bind("agenda", fecha, k)} value="${esc(ag?.[k] || "")}" placeholder="0">`)).join("")}</div>
    ${d.vacio ? "" : `<p class="fin-libre">Disponible hoy <b class="${d.disponible < 0 ? "neg" : ""}">${fmtFin(d.disponible)}</b></p>`}
    <button class="btn mini" data-ir="ruta" data-arg="m:${fecha.slice(0, 7)}" style="margin-top:8px">Ver el mes</button>`);
}
/** Mes: totales, presupuesto, tasa de ahorro y gastos por día (barras a escala). */
function finanzasMes(a, m) {
  const ym = `${a}-${String(m).padStart(2, "0")}`, dias = Store.lista("agenda").filter(x => x.fecha?.startsWith(ym));
  const porDia = {}; let n = 0; const t = { ingresos: 0, gastos: 0, ahorro: 0, inversion: 0 };
  dias.forEach(x => { const d = finDia(x); if (d.vacio) return; n++; CAMPOS_FIN.forEach(([k]) => t[k] += d[k]); if (d.gastos) porDia[Number(x.fecha.slice(8, 10))] = d.gastos; });
  const disp = t.ingresos - t.gastos - t.ahorro - t.inversion, pres = Math.abs(montoFin(respuesta(claveFinPresupuesto(ym))));
  const uso = pres > 0 ? t.gastos / pres : null, tasa = t.ingresos > 0 ? Math.min(1, Math.max(0, (t.ahorro + t.inversion) / t.ingresos)) : null;
  const campoPres = campo("Presupuesto de gastos del mes", `<input type="text" inputmode="decimal" data-resp="${FIN_WB}|${claveFinPresupuesto(ym)}" value="${esc(respuesta(claveFinPresupuesto(ym)))}" placeholder="Ej. 1.500.000">`);
  if (!n) return hoja(`<h3 style="margin:0 0 4px">Finanzas del mes</h3><p class="suave" style="margin:0 0 12px">Aún no hay movimientos en ${MESES[m - 1]}. Anota ingresos y gastos en el control financiero de cada día y aquí verás el mes completo.</p>${campoPres}`);
  const dm = diasEnMes(a, m), max = Math.max(...Object.values(porDia), pres > 0 ? pres / dm : 0, 1), W = 300, H = 72, bw = W / dm;
  const prom = pres > 0 ? pres / dm : 0;
  const barras = Object.entries(porDia).map(([d, v]) => { const h = Math.max(2, v / max * (H - 4)); return `<rect x="${(d - 1) * bw + 1}" y="${H - h}" width="${Math.max(1, bw - 2)}" height="${h}" rx="1.5" fill="var(--burdeos)"><title>${d} de ${MESES[m - 1]}: ${fmtFin(v)}</title></rect>`; }).join("");
  const linea = prom ? `<line x1="0" x2="${W}" y1="${H - prom / max * (H - 4)}" y2="${H - prom / max * (H - 4)}" stroke="var(--tinta-suave)" stroke-dasharray="3 3" stroke-width="1"/>` : "";
  return hoja(`<h3 style="margin:0 0 4px">Finanzas del mes</h3><p class="suave" style="margin:0 0 12px">${n} ${n === 1 ? "día" : "días"} con registro en ${MESES[m - 1]}.</p>
    <dl class="fin-tot">${CAMPOS_FIN.map(([k, tx]) => `<div><dt>${tx}</dt><dd>${fmtFin(t[k])}</dd></div>`).join("")}<div class="fin-disp"><dt>Disponible</dt><dd class="${disp < 0 ? "neg" : ""}">${fmtFin(disp)}</dd></div></dl>
    ${tasa != null ? `<p class="fin-libre">Ahorro e inversión: <b>${Math.round(tasa * 100)} %</b> de lo que entró</p>` : ""}
    ${campoPres}
    ${uso != null ? `<div class="fin-pres"><div class="barra"><i style="width:${Math.min(100, uso * 100)}%;${uso > 1 ? "background:var(--peligro, #B3261E)" : ""}"></i></div><span class="suave">Gastaste ${fmtFin(t.gastos)} de ${fmtFin(pres)}${uso > 1 ? " · te pasaste " + fmtFin(t.gastos - pres) : ""}</span></div>` : ""}
    ${Object.keys(porDia).length ? `<div class="etiqueta" style="margin:14px 0 6px">Gastos por día${prom ? " · la línea es el presupuesto diario" : ""}</div>
      <svg class="fin-graf" viewBox="0 0 ${W} ${H + 12}" role="img" aria-label="Gastos por día de ${MESES[m - 1]}">${linea}${barras}
        <text x="0" y="${H + 11}" font-size="8" fill="var(--tinta-suave)">1</text><text x="${W}" y="${H + 11}" font-size="8" fill="var(--tinta-suave)" text-anchor="end">${dm}</text></svg>` : ""}`);
}
window.finanzasDia = finanzasDia; window.finanzasMes = finanzasMes;
