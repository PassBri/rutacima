"use strict";
/* ======================================================================
 * Ascenso financiero, segunda parte (igual que en la app, domain/model/Cuentas.kt y Deudas.kt):
 * categorías propias, salud financiera, salir de deudas, privacidad con capas de seguridad y recibos.
 * Todo se guarda como respuestas del workbook "finanzas":
 *   fin#cats        → { ing|gas|aho|inv: [{ c, n, mo }] }   categorías creadas por la persona
 *   fin#deudas      → { metodo: "avalancha"|"bola", extra, lista: [{ id, nombre, saldo, tasa, minimo }] }
 *   fin#visibilidad → "privada" (por defecto) | "publica"
 *   fin#bloqueo     → "1" si el módulo se abre con PIN (web) o huella/rostro (app)
 *   fin#pin         → "sal.hash" (PBKDF2-SHA256) del PIN de la web
 *   fin#ocultar     → "1" oculta los montos fuera del módulo (Hoy, planificador, Mi ruta)
 * Los recibos van en cada movimiento (r: [{ id, nom, tipo, ruta }]) y el archivo en el depósito privado
 * "recibos" (carpeta de la cuenta, enlaces temporales); sin cuenta en la nube, en este navegador.
 * ====================================================================== */

/* ---------- Categorías propias ---------- */
function catsPropias() { try { return JSON.parse(respuesta("fin#cats") || "{}") || {}; } catch { return {}; } }
/** Las categorías de un tipo: las de siempre (sin "otro") + las propias + "otro" al final. */
function categorias(t) {
  const base = ASC.CAT[t], otro = base.find(x => x[0] === "otro");
  const propias = (catsPropias()[t] || []).map(x => [x.c, x.n, t === "gas" ? (x.mo || "amarilla") : undefined]);
  return [...base.filter(x => x[0] !== "otro"), ...propias, otro];
}

function crearCategoria(t, nombre, mochila) {
  const n = String(nombre || "").trim().slice(0, 40); if (!n) return null;
  const c = "u_" + n.toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/[^a-z0-9]+/g, "_").replace(/^_|_$/g, "").slice(0, 24);
  const todas = catsPropias(); todas[t] = todas[t] || [];
  if (!categorias(t).some(x => x[0] === c)) todas[t].push({ c, n, ...(t === "gas" ? { mo: mochila === "verde" ? "verde" : "amarilla" } : {}) });
  responder(ASC.WB, "fin#cats", JSON.stringify(todas));
  return c;
}

/* ---------- Deudas: salir con avalancha o bola de nieve (libro 3, Cargas financieras) ---------- */
function deudasDatos() {
  try { const d = JSON.parse(respuesta("fin#deudas") || "{}"); return { metodo: d.metodo === "bola" ? "bola" : "avalancha", extra: Number(d.extra) || 0, lista: Array.isArray(d.lista) ? d.lista : [] }; }
  catch { return { metodo: "avalancha", extra: 0, lista: [] }; }
}
const guardarDeudas = d => responder(ASC.WB, "fin#deudas", d.lista.length || d.extra ? JSON.stringify(d) : "");
/** Lo pagado a una deuda: los movimientos del libro que la nombran (d). */
function pagadoDe(id) { let s = 0; diasConMovs().forEach(f => movsGuardados(f).forEach(x => { if (x.d === id) s += Number(x.m) || 0; })); return s; }
const saldoHoy = x => Math.max(0, (Number(x.saldo) || 0) - pagadoDe(x.id));
function deudasHoy() { return deudasDatos().lista.map(x => ({ ...x, pagado: pagadoDe(x.id), actual: saldoHoy(x) })); }

/**
 * El plan mes a mes: cada deuda suma su interés, se pagan los mínimos y todo lo que sobra (el extra y
 * los mínimos de las deudas ya pagadas) va a la deuda elegida por el método. Igual que Deudas.plan en la app.
 */
function planDeudas(lista, metodo, extra) {
  const ds = lista.filter(x => x.actual > 0.005).map(x => ({ id: x.id, b: x.actual, r: (Number(x.tasa) || 0) / 100 / 12, min: Number(x.minimo) || 0, fin: 0 }));
  let mes = 0, interes = 0;
  while (ds.some(d => d.b > 0.005) && mes < 600) {
    mes++;
    ds.forEach(d => { if (d.b > 0.005) { const i = d.b * d.r; d.b += i; interes += i; } });
    let bolsa = Math.max(0, extra);
    ds.forEach(d => { if (d.b > 0.005) { const p = Math.min(d.min, d.b); d.b -= p; bolsa += d.min - p; } else bolsa += d.min; });
    const orden = ds.filter(d => d.b > 0.005).sort(metodo === "bola" ? (a, b) => a.b - b.b : (a, b) => b.r - a.r || a.b - b.b);
    for (const d of orden) { if (bolsa <= 0.005) break; const p = Math.min(bolsa, d.b); d.b -= p; bolsa -= p; }
    ds.forEach(d => { if (d.b <= 0.005 && !d.fin) d.fin = mes; });
  }
  const libre = !ds.some(d => d.b > 0.005);
  return { meses: libre ? mes : null, interes, fin: Object.fromEntries(ds.map(d => [d.id, d.fin || null])) };
}
const mesesDespues = n => { const d = new Date(); d.setDate(1); d.setMonth(d.getMonth() + n); return `${MESES[d.getMonth()]} de ${d.getFullYear()}`; };

/* ---------- Patrimonio: activos que la persona declara (casa, carro, cuentas…) ---------- */
const TIPOS_ACTIVO = [["liquidez", "Efectivo y cuentas"], ["inversion", "Inversiones"], ["bienes", "Bienes (casa, carro, terreno)"], ["negocio", "Negocio"], ["otro", "Otro"]];
function activosDatos() { try { const l = JSON.parse(respuesta("fin#activos") || "[]"); return Array.isArray(l) ? l : []; } catch { return []; } }
const guardarActivos = l => responder(ASC.WB, "fin#activos", l.length ? JSON.stringify(l) : "");

/** Escala de cada indicador para su barra: hasta dónde llega, la meta (línea) y si es mejor más alto o más bajo. */
const ESCALAS = {
  "Porcentaje de flujo neto": { max: 0.4, meta: 0.2, alto: true, metaTxt: "20 %" }, "Índice de liquidez": { max: 2, meta: 1.2, alto: true, metaTxt: "1,2" },
  "Tasa de ahorro": { max: 0.3, meta: 0.1, alto: true, metaTxt: "10 %" }, "Ahorro + inversión ÷ ingresos": { max: 0.4, meta: 0.2, alto: true, metaTxt: "20 %" },
  "Deuda ÷ ingreso (DTI)": { max: 0.6, meta: 0.3, alto: false, metaTxt: "30 %" }, "Relación gasto ÷ ingreso": { max: 1.2, meta: 0.7, alto: false, metaTxt: "70 %" },
  "Fondo de emergencia": { max: 12, meta: 3, meta2: 6, alto: true, metaTxt: "3 a 6 meses" }, "Índice de endeudamiento": { max: 1, meta: 0.3, alto: false, metaTxt: "30 %" },
  "Ingresos pasivos ÷ ingresos": { max: 0.5, meta: 0.2, alto: true, metaTxt: "20 %" }, "Índice de libertad financiera": { max: 1.2, meta: 1, alto: true, metaTxt: "1" },
  "Pasivos buenos ÷ pasivos": { max: 1, meta: 0.5, alto: true, metaTxt: "50 %" }, "Patrimonio neto ÷ ingresos anuales": { max: 5, meta: 1, alto: true, metaTxt: "1 año" },
};
/* ---------- Salud financiera: el estado de cuentas de la persona y su índice (ISF) ---------- */
/**
 * Indicadores de un periodo ("" = los últimos 12 meses con movimientos; "2026-" = ese año), en promedio mensual:
 * ingresos activos y pasivos, gastos fijos (mochila verde) y variables (amarilla), ahorro, inversión,
 * flujo neto, liquidez, tasa de ahorro, deuda/ingreso (DTI), gasto/ingreso, activos, pasivos, patrimonio,
 * libertad financiera y el Índice de Salud Financiera. Igual que Salud.de en la app.
 */
function saludDe(pref) {
  const dias = diasConMovs();
  let rango;
  if (pref) rango = dias.filter(f => f.startsWith(pref));
  else { const meses = [...new Set(dias.map(f => f.slice(0, 7)))].sort().slice(-12); rango = dias.filter(f => meses.includes(f.slice(0, 7))); }
  const s = sumar(rango.flatMap(movsDe)), nMeses = Math.max(1, new Set(rango.map(f => f.slice(0, 7))).size), mes = v => v / nMeses;
  const corte = pref ? pref.slice(0, 4) + "-12-31" : "9999-12-31", hasta = sumar(dias.filter(f => f <= corte).flatMap(movsDe));
  const actual = !pref || pref.startsWith(String(new Date().getFullYear()));
  const pasivoIng = s.cat["ing:pasivo"] || 0, ingAct = s.ing - pasivoIng;
  const fijos = s.mochila.verde, variables = s.mochila.amarilla, gastos = s.gas, egresos = s.gas + s.aho + s.inv;
  const deudas = actual ? deudasHoy() : [], pasivos = deudas.reduce((a, x) => a + x.actual, 0), buenas = deudas.filter(x => x.tipo === "buena").reduce((a, x) => a + x.actual, 0);
  const deudaMes = deudas.filter(x => x.actual > 0).reduce((a, x) => a + (Number(x.minimo) || 0), 0) || mes(s.cat["gas:deudas"] || 0);
  const declarados = actual ? activosDatos().reduce((a, x) => a + (Number(x.valor) || 0), 0) : 0;
  const activos = declarados + hasta.construido, neto = activos - pasivos;
  const emergencia = hasta.cat["aho:emergencia"] || 0, fondo = mes(fijos) > 0 ? emergencia / mes(fijos) : null;
  const r = {
    nMeses, s, activos, declarados, pasivos, neto, deudaMes, mesesLista: [...new Set(rango.map(f => f.slice(0, 7)))].sort(),
    filas: [["Ingresos activos (trabajo, negocio, ventas)", ingAct], ["Ingresos pasivos (rentas, intereses)", pasivoIng], ["Ingresos totales", s.ing, true],
      ["Gastos fijos (mochila verde)", fijos], ["Gastos variables (mochila amarilla)", variables], ["Ahorro", s.aho], ["Inversión", s.inv], ["Egresos totales", egresos, true],
      ["Flujo de efectivo neto", s.ing - egresos, true]],
    flujo: s.ing > 0 ? (s.ing - egresos) / s.ing : null, liquidez: egresos > 0 ? s.ing / egresos : null,
    tasaAhorro: s.ing > 0 ? s.aho / s.ing : null, tasaConstruye: s.ing > 0 ? s.construido / s.ing : null,
    dti: s.ing > 0 ? mes(s.ing) && deudaMes / mes(s.ing) : null, gastoIng: s.ing > 0 ? gastos / s.ing : null,
    pasivosRatio: s.ing > 0 ? pasivoIng / s.ing : null, buenasRatio: pasivos > 0 ? buenas / pasivos : null,
    endeuda: actual ? (pasivos === 0 ? 0 : activos > 0 ? pasivos / activos : Infinity) : null,
    libertad: gastos > 0 ? pasivoIng / gastos : null, netoIng: actual && s.ing > 0 ? neto / (mes(s.ing) * 12) : null, fondo,
  };
  // Índice de Salud Financiera: cada indicador puntúa de 0 a 1 y se promedia con su peso
  const lin = (v, malo, bueno) => v == null || !Number.isFinite(v) ? (v === Infinity ? (bueno > malo ? 1 : 0) : null) : Math.max(0, Math.min(1, (v - malo) / (bueno - malo)));
  const partes = [[lin(r.flujo, 0, 0.2), 2], [lin(r.tasaConstruye, 0, 0.2), 2], [lin(r.dti, 0.5, 0.2), 1.5], [lin(r.gastoIng, 1, 0.7), 1.5], [lin(r.liquidez, 1, 1.2), 1],
    [lin(r.fondo, 0, 6), 1.5], [r.endeuda == null ? null : lin(r.endeuda, 1, 0.3), 1.5], [actual ? (neto >= 0 ? 1 : 0) : null, 1], [lin(r.libertad, 0, 1), 0.5]].filter(([v]) => v != null);
  r.isf = partes.length && s.ing > 0 ? Math.round(partes.reduce((a, [v, w]) => a + v * w, 0) / partes.reduce((a, [, w]) => a + w, 0) * 100) : null;
  r.nivel = r.isf == null ? null : r.isf >= 80 ? ["Excelente", "Finanzas sólidas: buen patrimonio, ahorro e inversión.", "ok"] : r.isf >= 50 ? ["Buena", "Manejo adecuado de tus activos y de tu flujo de dinero.", "ok"]
    : r.isf >= 20 ? ["Moderada", "Requiere mejoras en ahorro o inversión.", "medio"] : ["En riesgo", "Pasivos altos o activos bajos: empieza por el plan de deudas y el fondo de emergencia.", "bajo"];
  const pct = v => (v * 100).toLocaleString(ascLocale, { maximumFractionDigits: 1 }) + " %", num = v => v.toLocaleString(ascLocale, { maximumFractionDigits: 2 });
  const est = (v, ok, medio, menor = false) => v == null ? "na" : menor ? (v <= ok ? "ok" : v <= medio ? "medio" : "bajo") : (v >= ok ? "ok" : v >= medio ? "medio" : "bajo");
  r.ind = [
    ["Porcentaje de flujo neto", r.flujo, pct, est(r.flujo, 0.2, 0), "Lo que queda libre de cada peso que entra. Ideal: 20 % o más."],
    ["Índice de liquidez", r.liquidez, num, est(r.liquidez, 1.2, 1), "Ingresos ÷ egresos. Por encima de 1 cubres todo lo que sale."],
    ["Tasa de ahorro", r.tasaAhorro, pct, est(r.tasaAhorro, 0.1, 0.05), "Ahorro ÷ ingresos."],
    ["Ahorro + inversión ÷ ingresos", r.tasaConstruye, pct, est(r.tasaConstruye, 0.2, 0.1), "Regla 80/20: al menos el 20 % para tu futuro (mochila roja)."],
    ["Deuda ÷ ingreso (DTI)", r.dti, pct, est(r.dti, 0.3, 0.4, true), "Cuotas mensuales de deuda ÷ ingreso mensual. Ideal: menos del 30 %."],
    ["Relación gasto ÷ ingreso", r.gastoIng, pct, est(r.gastoIng, 0.7, 0.9, true), "Gastos fijos y variables ÷ ingresos."],
    ["Fondo de emergencia", r.fondo, v => num(v) + " meses", est(r.fondo, 3, 1), "Meses de gastos fijos cubiertos. Ideal: de 3 a 6."],
    ["Índice de endeudamiento", r.endeuda, v => Number.isFinite(v) ? pct(v) : "Sin activos", est(r.endeuda, 0.3, 0.5, true), "Pasivos ÷ activos. Menos del 30 %: óptimo; más del 50 %: riesgo alto."],
    ["Ingresos pasivos ÷ ingresos", r.pasivosRatio, pct, est(r.pasivosRatio, 0.2, 0.05), "Qué parte de tu dinero llega sin tu tiempo."],
    ["Índice de libertad financiera", r.libertad, num, est(r.libertad, 1, 0.25), "Ingresos pasivos ÷ gastos. En 1, tus rentas pagan tu vida."],
    ["Pasivos buenos ÷ pasivos", r.buenasRatio, pct, r.buenasRatio == null ? "na" : r.buenasRatio >= 0.5 ? "ok" : "medio", "Deudas que producen (negocio, estudio, vivienda) frente a las que solo cuestan."],
    ["Patrimonio neto ÷ ingresos anuales", r.netoIng, num, est(r.netoIng, 1, 0.3), "Cuántos años de ingresos tienes construidos."],
  ].filter(x => x[1] != null).map(([nombre, v, f, estado, ayuda]) => ({ nombre, valor: f(v), v, estado, ayuda, ...(ESCALAS[nombre] || {}) }));
  return r;
}
/* Gráficos de la salud financiera (mismos colores y formas que la app):
 * · cada peso que entra: barra 100 % apilada (fijos, variables, ahorro, inversión, libre)
 * · indicadores: medidores con la meta marcada y el estado con ícono y palabra (nunca solo color)
 * · lo que tienes y lo que debes: dos barras en la misma escala
 * · mes a mes: flujo neto % y ahorro + inversión % con la meta del 20 % (crucero y lectura al pasar)
 * La tabla del estado de cuentas queda a un toque, para leer las cifras exactas. */
const EST_SALUD = { ok: ["Bien", "✓"], medio: ["Atención", "!"], bajo: ["Por mejorar", "✕"], na: ["", ""] };
const pctT = v => Math.round(v * 100) + " %";
function barraPeso(r) {
  const s = r.s; if (!(s.ing > 0)) return "";
  const libre = s.ing - s.gas - s.aho - s.inv;
  const partes = [["fijos", "Gastos fijos", s.mochila.verde], ["variables", "Gastos variables", s.mochila.amarilla + Math.max(0, s.gas - s.mochila.verde - s.mochila.amarilla)],
    ["ahorro", "Ahorro", s.aho], ["inversion", "Inversión", s.inv], ["libre", libre >= 0 ? "Libre" : "Déficit", Math.abs(libre)]].filter(x => x[2] > 0);
  const base = Math.max(s.ing, s.gas + s.aho + s.inv);
  return `<figure class="viz"><figcaption>¿Qué haces con cada peso que entra?</figcaption>
    <div class="peso-barra" role="img" aria-label="${partes.map(([, n, v]) => `${n} ${pctT(v / s.ing)}`).join(", ")}">${partes.map(([k, n, v]) =>
      `<span class="seg-${k}" style="flex:${v / base}" tabindex="0" data-tip="${esc(n)}|${pctT(v / s.ing)} de lo que entra|${plata(Math.round(v / r.nMeses))} al mes"></span>`).join("")}</div>
    ${libre < 0 ? `<p class="peso-aviso">✕ Sale más de lo que entra: ${plata(Math.round(-libre / r.nMeses))} al mes de déficit.</p>` : ""}
    <ul class="peso-ley">${partes.map(([k, n, v]) => `<li><i class="seg-${k}"></i><span>${n}</span><b>${pctT(v / s.ing)}</b><small>${plata(Math.round(v / r.nMeses))}/mes</small></li>`).join("")}</ul></figure>`;
}
function medidor(x) {
  if (x.max == null || !Number.isFinite(x.v)) return `<div class="med"><div class="med-cab"><b>${x.nombre}</b><span class="med-val">${x.valor}</span>${chipEst(x.estado)}</div><p class="med-ayuda">${x.ayuda}</p></div>`;
  const p = v => Math.max(0, Math.min(100, v / x.max * 100)), meta = p(x.meta), meta2 = x.meta2 ? p(x.meta2) : null;
  // La zona buena del riel: a la derecha de la meta si más es mejor, a la izquierda si menos es mejor
  const zona = x.alto ? `left:${meta}%;right:${meta2 != null ? 100 - meta2 : 0}%` : `left:0;right:${100 - meta}%`;
  return `<div class="med e-${x.estado}"><div class="med-cab"><b>${x.nombre}</b><span class="med-val">${x.valor}</span>${chipEst(x.estado)}</div>
    <div class="med-riel" role="img" aria-label="${esc(x.nombre)}: ${esc(x.valor)}; meta ${esc(x.metaTxt)}; ${EST_SALUD[x.estado][0]}"><span class="med-zona" style="${zona}"></span><span class="med-fill" style="width:${p(x.v)}%"></span>
      <span class="med-meta" style="left:${meta}%"></span>${meta2 != null ? `<span class="med-meta" style="left:${meta2}%"></span>` : ""}${x.v > x.max ? `<span class="med-mas" aria-hidden="true">›</span>` : ""}</div>
    <p class="med-ayuda">${x.ayuda} <span class="med-metatxt">Meta: ${x.metaTxt}</span></p></div>`;
}
const chipEst = e => e === "na" ? "" : `<span class="chip-est e-${e}"><i aria-hidden="true">${EST_SALUD[e][1]}</i>${EST_SALUD[e][0]}</span>`;
function balanceBarras(r) {
  if (!r.activos && !r.pasivos) return "";
  const max = Math.max(r.activos, r.pasivos, 1);
  return `<figure class="viz"><figcaption>Lo que tienes y lo que debes</figcaption>
    <div class="bal-fila"><span>Activos</span><div class="bal-riel"><i class="bal-act" style="width:${(r.activos / max * 100).toFixed(1)}%"></i></div><b>${plata(r.activos)}</b></div>
    <div class="bal-fila"><span>Pasivos</span><div class="bal-riel"><i class="bal-pas" style="width:${(r.pasivos / max * 100).toFixed(1)}%"></i></div><b>${plata(r.pasivos)}</b></div>
    <p class="bal-neto">Patrimonio neto <b class="${r.neto < 0 ? "neg" : ""}">${plata(r.neto)}</b>${r.activos > 0 ? ` · debes ${pctT(r.pasivos / r.activos)} de lo que tienes` : ""}</p></figure>`;
}
/** Línea mes a mes: % de flujo neto y % de ahorro + inversión, con la meta del 20 % punteada. */
function lineaMeses(pref, r) {
  const ms = r.mesesLista; if (ms.length < 2) return "";
  const datos = ms.map(m => { const s = sumar(diasConMovs().filter(f => f.startsWith(m)).flatMap(movsDe)); return { m, f: s.ing > 0 ? (s.ing - s.gas - s.aho - s.inv) / s.ing : null, c: s.ing > 0 ? (s.aho + s.inv) / s.ing : null }; });
  const vals = datos.flatMap(d => [d.f, d.c]).filter(v => v != null);
  const lo = Math.min(0, ...vals), hi = Math.max(0.3, ...vals), W = 600, H = 180, L = 36, R = 8, T = 10, B = 24;
  const x = i => L + i * (W - L - R) / (datos.length - 1), y = v => T + (hi - v) / (hi - lo) * (H - T - B);
  const camino = k => { let d = "", abierto = false; datos.forEach((p, i) => { if (p[k] == null) { abierto = false; return; } d += `${abierto ? "L" : "M"}${x(i).toFixed(1)},${y(p[k]).toFixed(1)}`; abierto = true; }); return d; };
  const ticks = [lo, 0, 0.2, hi].filter((v, i, a) => a.indexOf(v) === i && v >= lo && v <= hi);
  const etq = m => MES_C[Number(m.slice(5)) - 1] + (pref ? "" : " " + m.slice(2, 4));
  return `<figure class="viz linea-meses" data-puntos='${JSON.stringify(datos.map((d, i) => ({ x: x(i) / W, m: mayus(MESES[Number(d.m.slice(5)) - 1]) + " " + d.m.slice(0, 4), f: d.f == null ? "—" : pctT(d.f), c: d.c == null ? "—" : pctT(d.c) })))}'>
    <figcaption>Mes a mes</figcaption>
    <ul class="ley-lineas"><li><i class="ln-flujo"></i>Flujo neto</li><li><i class="ln-construye"></i>Ahorro + inversión</li><li><i class="ln-meta"></i>Meta 20 %</li></ul>
    <div class="lm-caja"><svg viewBox="0 0 ${W} ${H}" preserveAspectRatio="none" role="img" aria-label="Flujo neto y ahorro más inversión por mes">
      ${ticks.map(v => `<line class="lm-grid" x1="${L}" x2="${W - R}" y1="${y(v)}" y2="${y(v)}"/>`).join("")}
      <line class="lm-meta" x1="${L}" x2="${W - R}" y1="${y(0.2)}" y2="${y(0.2)}"/>
      <path class="lm-flujo" d="${camino("f")}"/><path class="lm-construye" d="${camino("c")}"/>
      ${datos.map((p, i) => (p.f != null ? `<circle class="lm-p-flujo" cx="${x(i)}" cy="${y(p.f)}" r="3.5"/>` : "") + (p.c != null ? `<circle class="lm-p-construye" cx="${x(i)}" cy="${y(p.c)}" r="3.5"/>` : "")).join("")}
      <line class="lm-cruz" x1="0" x2="0" y1="${T}" y2="${H - B}" hidden/></svg>
      <div class="lm-ejes">${ticks.map(v => `<span style="top:${(y(v) / H * 100).toFixed(1)}%">${pctT(v)}</span>`).join("")}</div>
      <div class="lm-meses">${datos.map((d, i) => `<span style="left:${(x(i) / W * 100).toFixed(1)}%">${etq(d.m)}</span>`).join("")}</div>
      <div class="lm-tip" hidden></div></div></figure>`;
}
function saludHoja(pref) {
  const r = saludDe(pref);
  if (!r.s.n) return "";
  const periodo = pref ? `en ${pref.slice(0, 4)}` : "en tus últimos 12 meses", actual = !pref || pref.startsWith(String(new Date().getFullYear()));
  const m = v => plata(Math.round(v / r.nMeses));
  return hoja(`<div class="cab-lista-btn"><h3 style="margin:0">Cómo está tu salud financiera</h3>${publicaFin() ? `<button class="btn mini" data-acc="ascCompartir" data-arg="${pref}">${ic("publicar")} Compartir mi avance</button>` : ""}</div>
    ${r.isf != null ? `<div class="salud-cab"><div class="salud-anillo" style="--p:${r.isf}" role="img" aria-label="Índice de Salud Financiera ${r.isf} %"><b>${r.isf}</b><small>ISF %</small></div>
      <div class="salud-nivel"><b class="salud-etq e-${r.nivel[2]}">${r.nivel[0]}</b><p class="suave" style="margin:2px 0 8px">${r.nivel[1]}</p>
        <div class="isf-escala" aria-label="Escala del índice">${[["Riesgo", 0, 20], ["Moderada", 20, 50], ["Buena", 50, 80], ["Excelente", 80, 100]].map(([n, a, b]) => `<span class="${r.isf >= a && (r.isf < b || b === 100) ? "aqui" : ""}" style="flex:${b - a}">${n}</span>`).join("")}<i style="left:${r.isf}%"></i></div>
        <p class="suave" style="margin:6px 0 0;font-size:12.5px">Índice de Salud Financiera ${periodo}.</p></div></div>` : ""}
    ${barraPeso(r)}
    <div class="medidores">${r.ind.map(medidor).join("")}</div>
    ${actual ? balanceBarras(r) : ""}
    ${lineaMeses(pref, r)}
    <details class="salud-detalle"><summary>Ver el estado de cuentas en cifras</summary>
    <div class="asc-tabla"><table class="salud-tabla"><thead><tr><th>Concepto</th><th class="num">Mensual (promedio)</th><th class="num">${pref ? "En " + pref.slice(0, 4) : "12 meses"}</th></tr></thead><tbody>
      ${r.filas.map(([n, v, fuerte]) => `<tr class="${fuerte ? "fuerte" : ""}"><th scope="row">${n}</th><td class="num${v < 0 ? " neg" : ""}">${m(v)}</td><td class="num${v < 0 ? " neg" : ""}">${plata(v)}</td></tr>`).join("")}
      <tr><th scope="row">Cuotas de deuda al mes</th><td class="num">${plata(r.deudaMes)}</td><td class="num"></td></tr>
      ${actual ? `<tr class="fuerte"><th scope="row">Activos totales</th><td class="num"></td><td class="num">${plata(r.activos)}</td></tr>
      <tr><th scope="row">Pasivos totales</th><td class="num"></td><td class="num">${plata(r.pasivos)}</td></tr>
      <tr class="fuerte"><th scope="row">Patrimonio neto</th><td class="num"></td><td class="num${r.neto < 0 ? " neg" : ""}">${plata(r.neto)}</td></tr>` : ""}
    </tbody></table></div></details>`);
}
/* Resumen de una línea en el balance y en el año: la salud completa vive en su propia pantalla */
function saludResumen(pref) {
  const r = saludDe(pref); if (r.isf == null) return "";
  const a = pref ? pref.slice(0, 4) : "";
  return `<button class="salud-enlace" data-sel="salud${a ? ":" + a : ""}">${av(ic("salud"), "var(--ok)")}<span><b>Salud financiera${a ? " de " + a : ""}: ${r.isf} % · ${r.nivel[0]}</b><small>${r.ind.filter(x => x.estado === "ok").length} de ${r.ind.filter(x => x.estado !== "na").length} indicadores en meta · ver gráficos</small></span>${ic("der")}</button>`;
}
/* ---------- Pantalla: salud financiera ---------- */
DET["finanzas.salud"] = arg => {
  if (finBloqueado()) return pantallaBloqueo();
  const hoyA = new Date().getFullYear(), a = Number(arg) || null;
  const anios = [...new Set([...diasConMovs().map(f => Number(f.slice(0, 4))), hoyA])].sort((x, y) => y - x);
  const chips = `<div class="chips salud-periodos" aria-label="Periodo">${[["salud", "Últimos 12 meses", !a], ...anios.map(y => ["salud:" + y, String(y), a === y])]
    .map(([sel, t, on]) => `<button class="chip" aria-pressed="${on}" data-sel="${sel}">${t}</button>`).join("")}</div>`;
  const cuerpoS = saludHoja(a ? a + "-" : "") || hoja(`<p class="suave" style="margin:0">Aún no hay movimientos ${a ? "en " + a : ""} para medir tu salud financiera. Anota lo que entra y sale y aquí verás tus indicadores.</p><div class="botones" style="margin-top:12px"><button class="btn lleno" data-sel="d:${iso(hoyFecha())}">Registrar hoy</button></div>`);
  return cab(av(ic("salud"), "var(--ok)"), "Salud financiera", "Ascenso financiero · tus indicadores", ojoAsc()) + cuerpo(
    migasAsc([["Balance general", "b"], ["Salud financiera", ""]]) + chips + cuerpoS +
    `<div class="botones" style="margin-top:4px"><button class="btn mini" data-sel="patrimonio">${ic("balanza")} Activos y pasivos</button><button class="btn mini" data-sel="deudas">${ic("deuda")} Salir de deudas</button></div>`, "max-width:980px");
};
/* Lectura al pasar: el segmento o el mes bajo el puntero (también con el teclado en los segmentos) */
(() => {
  let tip = null;
  const mostrar = (el, x, y) => {
    const [a, b, c] = el.dataset.tip.split("|");
    tip ||= Object.assign(document.createElement("div"), { className: "viz-tip", role: "tooltip" });
    tip.replaceChildren(...[[b, "strong"], [a, "span"], [c, "small"]].map(([t, tag]) => Object.assign(document.createElement(tag), { textContent: t })));
    document.body.append(tip); tip.style.left = Math.min(innerWidth - 180, x + 12) + "px"; tip.style.top = (y - 56) + "px";
  };
  document.addEventListener("pointermove", e => {
    const seg = e.target.closest?.(".peso-barra [data-tip]");
    if (seg) mostrar(seg, e.clientX, e.clientY); else tip?.remove();
    const fig = e.target.closest?.(".linea-meses .lm-caja");
    document.querySelectorAll(".linea-meses").forEach(f => { if (!fig || !f.contains(fig)) { f.querySelector(".lm-tip").hidden = true; f.querySelector(".lm-cruz").setAttribute("hidden", ""); } });
    if (!fig) return;
    const f = fig.closest(".linea-meses"), pts = JSON.parse(f.dataset.puntos), rc = fig.getBoundingClientRect(), fx = (e.clientX - rc.left) / rc.width;
    const p = pts.reduce((a, b) => Math.abs(b.x - fx) < Math.abs(a.x - fx) ? b : a);
    const cruz = f.querySelector(".lm-cruz"); cruz.removeAttribute("hidden"); cruz.setAttribute("x1", p.x * 600); cruz.setAttribute("x2", p.x * 600);
    const t = f.querySelector(".lm-tip"); t.hidden = false; t.style.left = `min(calc(${(p.x * 100).toFixed(1)}% + 10px), calc(100% - 170px))`;
    t.replaceChildren(Object.assign(document.createElement("b"), { textContent: p.m }),
      ...[["ln-flujo", p.f, "Flujo neto"], ["ln-construye", p.c, "Ahorro + inversión"]].map(([k, v, n]) => { const d = document.createElement("div"); d.append(Object.assign(document.createElement("i"), { className: k }), Object.assign(document.createElement("strong"), { textContent: v }), Object.assign(document.createElement("span"), { textContent: n })); return d; }));
  });
  document.addEventListener("focusin", e => { const seg = e.target.closest?.(".peso-barra [data-tip]"); if (seg) { const r = seg.getBoundingClientRect(); mostrar(seg, r.left, r.top); } });
  document.addEventListener("focusout", () => tip?.remove());
})();

/* ---------- Pantalla: patrimonio (activos y pasivos) ---------- */
DET["finanzas.patrimonio"] = () => {
  if (finBloqueado()) return pantallaBloqueo();
  const l = activosDatos(), led = sumar(diasConMovs().flatMap(movsDe)).construido, deudas = deudasHoy();
  const act = l.reduce((a, x) => a + (Number(x.valor) || 0), 0) + led, pas = deudas.reduce((a, x) => a + x.actual, 0);
  return cab(av(ic("balanza"), "var(--ok)"), "Activos y pasivos", "Ascenso financiero · tu patrimonio neto", ojoAsc()) + cuerpo(
    migasAsc([["Balance general", "b"], ["Activos y pasivos", ""]]) +
    `<div class="asc-cifras"><div class="asc-cifra c-ing"><span>Activos totales</span><b>${plata(act)}</b></div><div class="asc-cifra c-gas"><span>Pasivos totales</span><b>${plata(pas)}</b></div><div class="asc-cifra c-disp"><span>Patrimonio neto</span><b class="${act - pas < 0 ? "neg" : ""}">${plata(act - pas)}</b></div></div>` +
    hoja(`<h3 style="margin:0 0 4px">Lo que tienes</h3><p class="suave" style="margin:0 0 10px">Un activo pone dinero en tu bolsillo o guarda valor: cuentas, inversiones, tu casa, tu negocio. Escribe su valor de hoy.</p>
      <div class="asc-tabla"><table><thead><tr><th>Activo</th><th>Tipo</th><th class="num">Valor hoy</th><th></th></tr></thead><tbody>
        <tr><th scope="row">Ahorro e inversión del libro de cuentas</th><td class="suave">Se suma solo</td><td class="num">${plata(led)}</td><td></td></tr>
        ${l.map(x => `<tr><th scope="row"><input value="${esc(x.nombre)}" data-acc-change="activoCampo" data-arg="${x.id}|nombre" aria-label="Nombre"></th>
          <td><select data-acc-change="activoCampo" data-arg="${x.id}|tipo" aria-label="Tipo">${TIPOS_ACTIVO.map(([k, n]) => `<option value="${k}" ${k === x.tipo ? "selected" : ""}>${n}</option>`).join("")}</select></td>
          <td class="num"><input class="num" inputmode="decimal" value="${esc(numAsc(Number(x.valor) || 0))}" data-acc-change="activoCampo" data-arg="${x.id}|valor" aria-label="Valor de ${esc(x.nombre)}"></td>
          <td><button class="rb" data-acc="activoQuitar" data-arg="${x.id}" aria-label="Quitar ${esc(x.nombre)}">${ic("borrar")}</button></td></tr>`).join("")}
      </tbody></table></div>
      <form class="asc-nuevo" data-form="activoNuevo" style="margin-top:12px"><div class="asc-fila-nueva">
        <label class="campo"><span>Activo</span><input name="texto" maxlength="60" placeholder="Casa, carro, cuenta de ahorros…" required></label>
        <label class="campo"><span>Tipo</span><select name="tipo">${TIPOS_ACTIVO.map(([k, n]) => `<option value="${k}">${n}</option>`).join("")}</select></label>
        <label class="campo"><span>Valor hoy</span><input name="valor" inputmode="decimal" placeholder="${plata(0)}" required></label>
        <button class="btn lleno" type="submit">Agregar</button></div></form>`) +
    hoja(`<div class="cab-lista-btn"><h3 style="margin:0">Lo que debes</h3><button class="btn mini" data-sel="deudas">${ic("deuda")} Salir de deudas</button></div>
      ${deudas.length ? `<div class="asc-tabla"><table><thead><tr><th>Pasivo</th><th>Tipo</th><th class="num">Saldo hoy</th></tr></thead><tbody>${deudas.map(x => `<tr><th scope="row">${esc(x.nombre)}</th><td>${x.tipo === "buena" ? "Buena (produce)" : "Mala (solo cuesta)"}</td><td class="num">${plata(x.actual)}</td></tr>`).join("")}</tbody></table></div>`
        : `<p class="suave" style="margin:8px 0 0">Sin deudas anotadas.</p>`}`), "max-width:980px");
};
Object.assign(ACC, {
  activoCampo(el, arg) { const [id, k] = arg.split("|"), l = activosDatos(), x = l.find(y => y.id === id); if (!x) return; x[k] = k === "valor" ? Math.abs(montoFin(el.value)) : el.value.trim().slice(0, 60); guardarActivos(l); },
  activoQuitar(_, id) { const l = activosDatos(), x = l.find(y => y.id === id); guardarActivos(l.filter(y => y.id !== id)); if (x) toast(`${x.nombre} quitado`, { accion: "Deshacer", alHacer: () => guardarActivos([...activosDatos(), x]) }); },
});
window.FORMULARIOS = Object.assign(window.FORMULARIOS || {}, {
  activoNuevo(form, datos, texto) {
    const v = Math.abs(montoFin(datos.get("valor"))); if (!texto || !v) { toast("Escribe el activo y su valor"); return; }
    guardarActivos([...activosDatos(), { id: "a" + nuevoId(), nombre: texto.slice(0, 60), tipo: datos.get("tipo") || "otro", valor: v }]); form.reset(); toast("Activo agregado");
  },
});

/* ---------- Privacidad y capas de seguridad ---------- */
const publicaFin = () => respuesta("fin#visibilidad") === "publica";
const bloqueoFin = () => respuesta("fin#bloqueo") === "1" && !!respuesta("fin#pin");
const ocultarFuera = () => !publicaFin() && respuesta("fin#ocultar") === "1";
/** Fuera del módulo, los montos se ocultan si así lo eligió la persona. */
window.plataFuera = v => ocultarFuera() ? "•••••" : plata(v);
async function hashPin(pin, sal) {
  const enc = new TextEncoder(), clave = await crypto.subtle.importKey("raw", enc.encode(pin), "PBKDF2", false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits({ name: "PBKDF2", hash: "SHA-256", salt: enc.encode(sal), iterations: 120000 }, clave, 256);
  return [...new Uint8Array(bits)].map(b => b.toString(16).padStart(2, "0")).join("");
}
const finBloqueado = () => bloqueoFin() && !(estado.ascAbierto && Date.now() - estado.ascAbierto < 5 * 60 * 1000);
document.addEventListener("visibilitychange", () => { if (document.hidden) estado.ascAbierto = 0; });
function pantallaBloqueo() {
  const espera = Math.max(0, (estado.ascEspera || 0) - Date.now());
  return cab(av(ic("candado"), "var(--ok)"), "Ascenso financiero", "Privado · bloqueado") + cuerpo(hoja(`<div class="bloqueo-fin">${ic("candado", "i grande")}
    <h3>Tus finanzas están protegidas</h3><p class="suave">Escribe tu PIN para abrir el libro de cuentas. Se vuelve a cerrar a los 5 minutos o al salir.</p>
    <form data-form="ascPin" class="form" style="max-width:260px;margin:0 auto"><input name="texto" type="password" inputmode="numeric" autocomplete="off" maxlength="6" pattern="[0-9]{4,6}" aria-label="PIN" placeholder="PIN" ${espera ? "disabled" : ""} required style="text-align:center;letter-spacing:.5em;font-size:22px">
      <button class="btn lleno" type="submit" ${espera ? "disabled" : ""}>Abrir</button></form>
    ${espera ? `<p class="suave">Demasiados intentos. Espera ${Math.ceil(espera / 1000)} segundos.</p>` : ""}</div>`), "max-width:560px");
}
function privacidadHoja() {
  const pub = publicaFin();
  return hoja(`<h3 style="margin:0 0 4px">Privacidad</h3><p class="suave" style="margin:0 0 10px">Tus cuentas son tuyas. Elige si son solo para ti o si quieres compartir tu avance (solo porcentajes, nunca montos).</p>
    <div class="seg" role="radiogroup" aria-label="Privacidad" style="grid-template-columns:1fr 1fr;max-width:420px"><button type="button" role="radio" aria-checked="${!pub}" class="seg-b t-gas" data-acc="ascVisibilidad" data-arg="privada">Privada</button><button type="button" role="radio" aria-checked="${pub}" class="seg-b t-ing" data-acc="ascVisibilidad" data-arg="publica">Pública</button></div>
    ${pub ? `<p class="suave" style="margin:12px 0 0">Puedes compartir tu salud financiera y tu avance contra las deudas en la comunidad. Los montos, los recibos y los movimientos siguen siendo privados.</p>`
      : `<div class="capas"><div class="capa">${ic("check")}<span><b>Solo tu cuenta</b><small>Nadie más puede leer tus movimientos: la base de datos solo los entrega a tu sesión.</small></span></div>
        <div class="capa">${ic("candado")}<span><b>Bloqueo con PIN</b><small>${bloqueoFin() ? "Activo: el módulo pide tu PIN y se cierra a los 5 minutos o al salir. En la app se abre con tu huella o rostro." : "Pide un PIN para abrir este módulo en la web (en la app, tu huella o rostro)."}</small></span>
          ${bloqueoFin() ? `<button class="btn mini" data-acc="ascQuitarPin">Quitar</button>` : `<button class="btn mini lleno" data-acc="ascPonerPin">Activar</button>`}</div>
        <div class="capa">${ic("ojo")}<span><b>Ocultar montos fuera del módulo</b><small>En Hoy, el planificador y Mi ruta verás ••••• en vez de cifras.</small></span><input type="checkbox" data-acc-change="ascOcultar" ${respuesta("fin#ocultar") === "1" ? "checked" : ""} aria-label="Ocultar montos fuera del módulo"></div>
        <div class="capa">${ic("adjunto")}<span><b>Recibos en un archivo privado</b><small>Tus fotos y PDF se guardan en una carpeta solo tuya y se abren con enlaces que vencen en minutos.</small></span></div></div>`}`);
}

/* ---------- Recibos (fotos y PDF) ---------- */
const IDB = {
  db: null,
  abrir() { return this.db ||= new Promise((ok, mal) => { const r = indexedDB.open("rutacima-recibos", 1); r.onupgradeneeded = () => r.result.createObjectStore("archivos"); r.onsuccess = () => ok(r.result); r.onerror = () => mal(r.error); }); },
  async poner(k, v) { const db = await this.abrir(); return new Promise((ok, mal) => { const t = db.transaction("archivos", "readwrite"); t.objectStore("archivos").put(v, k); t.oncomplete = ok; t.onerror = () => mal(t.error); }); },
  async leer(k) { const db = await this.abrir(); return new Promise((ok, mal) => { const q = db.transaction("archivos").objectStore("archivos").get(k); q.onsuccess = () => ok(q.result); q.onerror = () => mal(q.error); }); },
  async quitar(k) { const db = await this.abrir(); return new Promise(ok => { const t = db.transaction("archivos", "readwrite"); t.objectStore("archivos").delete(k); t.oncomplete = ok; t.onerror = ok; }); },
};
/** Las fotos grandes se reducen (2000 px, JPEG) para que el recibo se lea bien y pese poco. */
async function prepararArchivo(f) {
  if (!f.type.startsWith("image/") || f.type === "image/gif") return f;
  try {
    const img = await createImageBitmap(f), k = Math.min(1, 2000 / Math.max(img.width, img.height));
    if (k === 1 && f.size < 1.5e6) return f;
    const c = document.createElement("canvas"); c.width = Math.round(img.width * k); c.height = Math.round(img.height * k);
    c.getContext("2d").drawImage(img, 0, 0, c.width, c.height);
    const b = await new Promise(ok => c.toBlob(ok, "image/jpeg", 0.85));
    return new File([b], f.name.replace(/\.\w+$/, "") + ".jpg", { type: "image/jpeg" });
  } catch { return f; }
}
async function subirRecibo(fecha, movId, archivo) {
  if (!/^image\/|application\/pdf/.test(archivo.type)) { toast("Solo fotos o PDF"); return; }
  if (archivo.size > 15e6) { toast("El archivo pesa más de 15 MB"); return; }
  const f = await prepararArchivo(archivo), id = "r" + nuevoId(), ext = f.type === "application/pdf" ? "pdf" : (f.type.split("/")[1] || "jpg").replace("jpeg", "jpg");
  let ruta;
  if (Store.nube) {
    ruta = `${Store.nube.dueno}/${id}.${ext}`;
    const up = await Store.nube.sb.storage.from("recibos").upload(ruta, f, { contentType: f.type, upsert: false });
    if (up.error) { errorNube(up.error); return; }
  } else { ruta = `local:${id}.${ext}`; await IDB.poner(ruta, f); }
  cambiarMov(`${fecha}|${movId}`, x => { x.r = [...(x.r || []), { id, nom: String(archivo.name || "recibo").slice(0, 80), tipo: f.type, ruta }]; });
  toast("Recibo guardado");
}
async function urlRecibo(r) {
  if (r.ruta.startsWith("local:")) { const b = await IDB.leer(r.ruta); return b ? URL.createObjectURL(b) : null; }
  if (!Store.nube) return null;
  const { data, error } = await Store.nube.sb.storage.from("recibos").createSignedUrl(r.ruta, 600);
  if (error) { errorNube(error); return null; }
  return data.signedUrl;
}
async function abrirRecibos(fecha, movId) {
  const x = movsDe(fecha).find(y => y.id === movId); if (!x) return;
  const rs = x.r || [];
  const v = ventanaCuenta(`<h3 style="margin-top:0">Recibos</h3><p class="suave" style="margin-top:0">${esc(x.n || catDe(x.t, x.c)?.[1] || "")} · ${plata(x.m)}</p>
    <div class="recibos">${rs.map(r => `<div class="recibo" data-ruta="${esc(r.ruta)}"><span class="recibo-vista">${r.tipo === "application/pdf" ? ic("pdf") : ic("foto")}</span><span class="recibo-nom">${esc(r.nom)}</span>
      <button class="btn mini" data-ver="${esc(r.id)}">Ver</button><button class="rb" data-quitar="${esc(r.id)}" aria-label="Quitar ${esc(r.nom)}">${ic("borrar")}</button></div>`).join("") || `<p class="suave">Aún no hay recibos. Agrega la foto de la factura o el PDF para tener todo a mano.</p>`}</div>
    <label class="btn lleno" style="display:inline-flex;gap:6px;align-items:center;cursor:pointer">${ic("adjunto")} Agregar foto o PDF<input type="file" accept="image/*,application/pdf" capture="environment" hidden></label>
    <p class="suave" style="font-size:12.5px">${Store.nube ? "Se guardan en tu carpeta privada; solo tú puedes abrirlos." : "En la demostración se guardan solo en este navegador."}</p>
    <div class="botones"><button class="btn" type="button" data-cerrar>Listo</button></div>`, v => {
    v.querySelector("[data-cerrar]").onclick = () => v.remove();
    v.querySelector("input[type=file]").onchange = async e => { const a = e.target.files?.[0]; if (!a) return; e.target.closest("label").classList.add("cargando"); await subirRecibo(fecha, movId, a); v.remove(); abrirRecibos(fecha, movId); };
    v.querySelectorAll("[data-ver]").forEach(b => b.onclick = async () => {
      const r = rs.find(y => y.id === b.dataset.ver), url = r && await urlRecibo(r); if (!url) { toast("No se pudo abrir el recibo"); return; }
      const fila = b.closest(".recibo"); fila.querySelector(".recibo-vista").innerHTML = r.tipo === "application/pdf" ? ic("pdf") : `<img src="${url}" alt="${esc(r.nom)}">`;
      window.open(url, "_blank", "noopener");
    });
    v.querySelectorAll("[data-quitar]").forEach(b => b.onclick = async () => {
      const r = rs.find(y => y.id === b.dataset.quitar); if (!r) return;
      cambiarMov(`${fecha}|${movId}`, y => { y.r = (y.r || []).filter(z => z.id !== r.id); });
      if (r.ruta.startsWith("local:")) IDB.quitar(r.ruta); else Store.nube?.sb.storage.from("recibos").remove([r.ruta]);
      toast("Recibo quitado"); v.remove(); abrirRecibos(fecha, movId);
    });
  });
}

/* ---------- Pantalla: salir de deudas ---------- */
DET["finanzas.deudas"] = () => {
  if (finBloqueado()) return pantallaBloqueo();
  const d = deudasDatos(), lista = deudasHoy(), total = lista.reduce((a, x) => a + x.actual, 0), pagado = lista.reduce((a, x) => a + x.pagado, 0);
  const plan = planDeudas(lista, d.metodo, d.extra), otro = planDeudas(lista, d.metodo === "bola" ? "avalancha" : "bola", d.extra);
  const orden = lista.filter(x => x.actual > 0).sort((a, b) => (plan.fin[a.id] || 1e9) - (plan.fin[b.id] || 1e9));
  const NOM = { avalancha: "Avalancha", bola: "Bola de nieve" };
  return cab(av(ic("deuda"), "var(--ok)"), "Salir de deudas", "Ascenso financiero · tu plan para quedar libre") + cuerpo(
    migasAsc([["Balance general", "b"], ["Salir de deudas", ""]]) +
    (lista.length ? `<div class="asc-cifras"><div class="asc-cifra c-gas"><span>Debes hoy</span><b>${plata(total)}</b></div><div class="asc-cifra c-ing"><span>Ya pagaste</span><b>${plata(pagado)}</b></div>
      <div class="asc-cifra c-disp"><span>Libre de deudas</span><b>${total === 0 ? "¡Ya!" : plan.meses ? `${plan.meses} ${plan.meses === 1 ? "mes" : "meses"}` : "Sin fecha"}</b></div></div>` : "") +
    hoja(`<h3 style="margin:0 0 4px">Tu método</h3><p class="suave" style="margin:0 0 10px">Del libro Cargas financieras: paga el mínimo en todas y pon todo lo extra en una sola. Cuando la terminas, su cuota pasa a la siguiente.</p>
      <div class="seg" role="radiogroup" aria-label="Método" style="grid-template-columns:1fr 1fr">${["avalancha", "bola"].map(m => `<button type="button" role="radio" aria-checked="${d.metodo === m}" class="seg-b t-gas" data-acc="deudaMetodo" data-arg="${m}">${NOM[m]}</button>`).join("")}</div>
      <p class="suave" style="margin:10px 0">${d.metodo === "bola" ? "<b>Bola de nieve:</b> primero la deuda más pequeña. Victorias rápidas que te mantienen en el camino." : "<b>Avalancha:</b> primero la de interés más alto. Pagas menos intereses en total."}</p>
      ${campo("¿Cuánto más puedes pagar cada mes, además de los mínimos?", `<input type="text" inputmode="decimal" value="${d.extra ? esc(numAsc(d.extra)) : ""}" placeholder="${plata(0)}" data-acc-change="deudaExtra">`)}
      ${lista.some(x => x.actual > 0) ? `<div class="plan-deudas">${plan.meses ? `<p><b>Quedas libre en ${mesesDespues(plan.meses)}</b> (${plan.meses} ${plan.meses === 1 ? "mes" : "meses"}) y pagas cerca de ${plata(Math.round(plan.interes))} en intereses.</p>`
        : `<p class="neg"><b>Con estos pagos la deuda no baja:</b> los intereses son mayores que los mínimos. Sube el pago extra o renegocia la tasa.</p>`}
        ${otro.meses && plan.meses && (otro.meses !== plan.meses || Math.abs(otro.interes - plan.interes) > 1) ? `<p class="suave">Con ${NOM[d.metodo === "bola" ? "avalancha" : "bola"]}: ${otro.meses} meses y ${plata(Math.round(otro.interes))} en intereses.</p>` : ""}</div>` : ""}`) +
    hoja(`<div class="cab-lista-btn"><h3 style="margin:0">Tus deudas</h3></div>
      ${lista.length ? `<div class="asc-tabla"><table class="asc-deudas"><thead><tr><th>Deuda</th><th class="num">Saldo hoy</th><th class="num">Interés anual</th><th class="num">Mínimo</th><th class="num">Termina</th><th></th></tr></thead><tbody>
        ${lista.map(x => { const i = orden.indexOf(x), fin = plan.fin[x.id];
          return `<tr><th scope="row">${i >= 0 ? `<span class="orden">${i + 1}</span>` : `<span class="orden ok">${ic("check")}</span>`}${esc(x.nombre)}${x.tipo === "buena" ? ` <small class="suave">· buena</small>` : ""}<div class="barra-deuda"><i style="width:${x.saldo ? Math.min(100, x.pagado / x.saldo * 100) : 100}%"></i></div></th>
            <td class="num">${plata(x.actual)}</td><td class="num"><input class="num" inputmode="decimal" value="${esc(numAsc(Number(x.tasa) || 0))}" data-acc-change="deudaCampo" data-arg="${x.id}|tasa" aria-label="Interés anual de ${esc(x.nombre)}"> %</td>
            <td class="num"><input class="num" inputmode="decimal" value="${esc(numAsc(Number(x.minimo) || 0))}" data-acc-change="deudaCampo" data-arg="${x.id}|minimo" aria-label="Pago mínimo de ${esc(x.nombre)}"></td>
            <td class="num">${x.actual <= 0 ? "Pagada" : fin ? MES_C[(new Date().getMonth() + fin) % 12] + " " + (new Date(new Date().getFullYear(), new Date().getMonth() + fin, 1).getFullYear()) : "—"}</td>
            <td class="acciones-deuda">${x.actual > 0 ? `<button class="btn mini lleno" data-acc="deudaPagar" data-arg="${x.id}">Pagar</button>` : ""}<button class="rb" data-acc="deudaQuitar" data-arg="${x.id}" aria-label="Quitar ${esc(x.nombre)}">${ic("borrar")}</button></td></tr>`; }).join("")}
      </tbody></table></div>` : `<p class="suave">Anota cada deuda con lo que debes hoy, su interés anual y su pago mínimo. El plan se arma solo.</p>`}
      <form class="asc-nuevo" data-form="deudaNueva" style="margin-top:14px"><div class="asc-fila-nueva deuda-nueva">
        <label class="campo"><span>Deuda</span><input name="texto" maxlength="60" placeholder="Tarjeta de crédito" required></label>
        <label class="campo"><span>Debes hoy</span><input name="saldo" inputmode="decimal" placeholder="${plata(1000000)}" required></label>
        <label class="campo"><span>Interés anual %</span><input name="tasa" inputmode="decimal" placeholder="28"></label>
        <label class="campo"><span>Pago mínimo</span><input name="minimo" inputmode="decimal" placeholder="${plata(80000)}"></label>
        <label class="campo"><span>Tipo</span><select name="tipo"><option value="mala">Mala · solo cuesta</option><option value="buena">Buena · produce (negocio, estudio, vivienda)</option></select></label>
        <button class="btn lleno" type="submit">Agregar</button></div></form>`) +
    `<p class="suave" style="font-size:13px">Cada pago se anota en el libro de cuentas de hoy como gasto en "Pago de deudas" y baja el saldo. Lee más en la guía <button class="enlace" data-acc="aprendeFinanzas">Cargas financieras</button>.</p>`, "max-width:980px");
};

/* ---------- Acciones ---------- */
Object.assign(ACC, {
  deudaMetodo(_, m) { const d = deudasDatos(); d.metodo = m; guardarDeudas(d); },
  deudaExtra(el) { const d = deudasDatos(); d.extra = Math.abs(montoFin(el.value)); guardarDeudas(d); },
  deudaCampo(el, arg) { const [id, k] = arg.split("|"), d = deudasDatos(), x = d.lista.find(y => y.id === id); if (!x) return; x[k] = Math.abs(montoFin(el.value)); guardarDeudas(d); },
  deudaQuitar(_, id) { const d = deudasDatos(), x = d.lista.find(y => y.id === id); if (!x) return; d.lista = d.lista.filter(y => y.id !== id); guardarDeudas(d);
    toast(`${x.nombre} quitada`, { accion: "Deshacer", alHacer: () => { const e = deudasDatos(); e.lista.push(x); guardarDeudas(e); } }); },
  deudaPagar(_, id) {
    const x = deudasHoy().find(y => y.id === id); if (!x) return;
    const v = ventanaCuenta(`<h3 style="margin-top:0">Pagar ${esc(x.nombre)}</h3><p class="suave">Debes hoy ${plata(x.actual)}. El pago se anota en tu libro de cuentas de hoy.</p>
      <form class="form" id="pagoDeuda">${campo("Monto del pago", `<input name="monto" inputmode="decimal" value="${esc(numAsc(Math.min(x.actual, Number(x.minimo) || x.actual)))}" required>`)}
      <div class="botones"><button class="btn" type="button" data-cerrar>Cancelar</button><button class="btn lleno" type="submit">Pagar</button></div></form>`, v => {
      v.querySelector("[data-cerrar]").onclick = () => v.remove();
      v.querySelector("#pagoDeuda").onsubmit = e => {
        e.preventDefault(); const m = Math.min(x.actual, Math.abs(montoFin(new FormData(e.target).get("monto")))); if (!m) return;
        const f = iso(hoyFecha()); guardarMovs(f, [...movsGuardados(f), { id: "m" + nuevoId(), t: "gas", m, c: "deudas", n: "Pago " + x.nombre, d: x.id }]);
        v.remove(); toast(m >= x.actual - 0.005 ? `¡${x.nombre} quedó pagada!` : `Pago de ${plata(m)} anotado`);
      };
    });
  },
  ascVisibilidad(_, v) { responder(ASC.WB, "fin#visibilidad", v === "publica" ? "publica" : ""); toast(v === "publica" ? "Podrás compartir tu avance, sin montos" : "Tus finanzas son privadas"); },
  ascOcultar(el) { responder(ASC.WB, "fin#ocultar", el.checked ? "1" : ""); },
  ascQuitarPin() { if (finBloqueado()) return; responder(ASC.WB, "fin#bloqueo", ""); responder(ASC.WB, "fin#pin", ""); toast("Bloqueo quitado"); },
  ascPonerPin() {
    const v = ventanaCuenta(`<h3 style="margin-top:0">Crea tu PIN</h3><p class="suave">De 4 a 6 números. Lo pedirá este módulo en la web; en la app se abre con tu huella o rostro.</p>
      <form class="form" id="nuevoPin">${campo("PIN", `<input name="a" type="password" inputmode="numeric" pattern="[0-9]{4,6}" maxlength="6" required autocomplete="new-password">`)}${campo("Repite el PIN", `<input name="b" type="password" inputmode="numeric" pattern="[0-9]{4,6}" maxlength="6" required autocomplete="new-password">`)}
      <div class="botones"><button class="btn" type="button" data-cerrar>Cancelar</button><button class="btn lleno" type="submit">Activar</button></div></form>`, v => {
      v.querySelector("[data-cerrar]").onclick = () => v.remove();
      v.querySelector("#nuevoPin").onsubmit = async e => {
        e.preventDefault(); const d = new FormData(e.target), a = String(d.get("a")), b = String(d.get("b"));
        if (a !== b) { toast("Los PIN no coinciden"); return; }
        const sal = [...crypto.getRandomValues(new Uint8Array(12))].map(x => x.toString(16).padStart(2, "0")).join("");
        responder(ASC.WB, "fin#pin", sal + "." + await hashPin(a, sal)); responder(ASC.WB, "fin#bloqueo", "1");
        estado.ascAbierto = Date.now(); v.remove(); toast("Bloqueo activado");
      };
    });
  },
  ascCompartir(_, pref) {
    const r = saludDe(pref || ""), dd = deudasHoy(), d = deudasDatos(), plan = planDeudas(dd, d.metodo, d.extra);
    const lineas = [r.isf != null ? `Mi Índice de Salud Financiera${pref ? " en " + pref.slice(0, 4) : ""}: ${r.isf} % (${r.nivel[0]}).` : "",
      ...r.ind.filter(x => /flujo neto|Ahorro \+ inversión|Fondo de emergencia|libertad/.test(x.nombre)).map(x => `${x.nombre}: ${x.valor}`),
      dd.length ? (dd.every(x => x.actual <= 0) ? "¡Salí de todas mis deudas!" : plan.meses ? `Saldré de mis deudas en ${plan.meses} meses con el método ${d.metodo === "bola" ? "bola de nieve" : "avalancha"}.` : "") : ""].filter(Boolean);
    estado.borradorPost = lineas.join("\n") + "\n#AscensoFinanciero"; ir("comunidad", "publicar");
  },
  ascCrearCat(_, t) {
    const tipo = ASC.CAT[t] ? t : "gas";
    const v = ventanaCuenta(`<h3 style="margin-top:0">Nueva categoría</h3><form class="form" id="nuevaCat">
      ${campo("Tipo", `<select name="t">${ASC.TIPOS.map(([k, , u]) => `<option value="${k}" ${k === tipo ? "selected" : ""}>${u}</option>`).join("")}</select>`)}
      ${campo("Nombre", `<input name="n" maxlength="40" required placeholder="Mascotas, Gimnasio, Regalos…">`)}
      <div class="campo" data-solo-gasto><span>Mochila</span><select name="mo"><option value="verde">Verde · lo esencial</option><option value="amarilla" selected>Amarilla · gastos variables</option></select></div>
      <div class="botones"><button class="btn" type="button" data-cerrar>Cancelar</button><button class="btn lleno" type="submit">Crear</button></div></form>`, v => {
      const sel = v.querySelector("[name=t]"), mo = v.querySelector("[data-solo-gasto]"); const ver = () => { mo.hidden = sel.value !== "gas"; }; sel.onchange = ver; ver();
      v.querySelector("[data-cerrar]").onclick = () => v.remove();
      v.querySelector("#nuevaCat").onsubmit = e => {
        e.preventDefault(); const d = new FormData(e.target), c = crearCategoria(d.get("t"), d.get("n"), d.get("mo"));
        if (!c) return; v.remove(); estado.ascTipo = d.get("t"); estado.ascCatNueva = c; toast(`Categoría “${String(d.get("n")).trim()}” creada`);
      };
    });
  },
  ascRecibos(_, arg) { const [f, id] = arg.split("|"); abrirRecibos(f, id); },
});
window.FORMULARIOS = Object.assign(window.FORMULARIOS || {}, {
  async ascPin(form, datos, texto) {
    const [sal, h] = respuesta("fin#pin").split(".");
    if (await hashPin(texto, sal) === h) { estado.ascAbierto = Date.now(); estado.ascIntentos = 0; pintarDetalle(); return; }
    estado.ascIntentos = (estado.ascIntentos || 0) + 1;
    if (estado.ascIntentos >= 5) { estado.ascEspera = Date.now() + 30000; estado.ascIntentos = 0; setTimeout(() => pintarDetalle(), 30100); }
    toast("PIN incorrecto"); pintarDetalle();
  },
  deudaNueva(form, datos, texto) {
    const saldo = Math.abs(montoFin(datos.get("saldo"))); if (!texto || !saldo) { toast("Escribe el nombre y lo que debes"); return; }
    const d = deudasDatos(); d.lista.push({ id: "d" + nuevoId(), nombre: texto.slice(0, 60), saldo, tasa: Math.abs(montoFin(datos.get("tasa"))), minimo: Math.abs(montoFin(datos.get("minimo"))), tipo: datos.get("tipo") === "buena" ? "buena" : "mala" });
    guardarDeudas(d); form.reset(); toast("Deuda agregada al plan");
  },
});

/** En el balance: cuánto debes y cuándo quedas libre, con el acceso al plan. */
function deudasResumen() {
  const lista = deudasHoy(), d = deudasDatos();
  if (!lista.length) return hoja(`<div class="cab-lista-btn"><h3 style="margin:0">Salir de deudas</h3><button class="btn mini lleno" data-sel="deudas">${ic("mas")} Armar mi plan</button></div><p class="suave" style="margin:8px 0 0">Anota tus deudas y elige avalancha o bola de nieve: verás el mes en que quedas libre.</p>`);
  const debe = lista.reduce((a, x) => a + x.actual, 0), plan = planDeudas(lista, d.metodo, d.extra);
  return `<button class="hoja asc-sube" data-sel="deudas"><span>Salir de deudas · ${d.metodo === "bola" ? "bola de nieve" : "avalancha"}</span><b class="${debe > 0 ? "" : "ok"}">${debe > 0 ? `Debes ${plata(debe)}` : "¡Sin deudas!"}</b><small>${debe > 0 ? (plan.meses ? `Quedas libre en ${mesesDespues(plan.meses)}` : "Con estos pagos la deuda no baja: revisa tu plan") : "Todas tus deudas están pagadas"}</small>${ic("der")}</button>`;
}
window.finBloqueado = finBloqueado; window.ocultarFuera = ocultarFuera;

window.saludDe = saludDe;
