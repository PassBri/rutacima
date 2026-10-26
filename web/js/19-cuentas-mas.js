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

/* ---------- Deudas: salir con un método (libro 3, Cargas financieras). Igual que Deudas.kt en la app ---------- */
/** [clave, nombre, idea en una línea, cómo ordena] */
const METODOS_DEUDA = [
  ["avalancha", "Avalancha", "Primero la de interés más alto: pagas menos intereses en total."],
  ["bola", "Bola de nieve", "Primero la más pequeña: victorias rápidas que te mantienen en el camino."],
  ["flujo", "Flujo de caja", "Primero la que libera más cuota por cada peso que pagas (saldo ÷ mínimo): respiras antes cada mes."],
  ["hibrido", "Híbrido", "Primero las que puedes cerrar en 3 meses o menos; después, avalancha."],
  ["propio", "Mi orden", "Tú decides el orden con las flechas: por ejemplo, la que más te pesa."],
];
function deudasDatos() {
  try { const d = JSON.parse(respuesta("fin#deudas") || "{}"); return { metodo: METODOS_DEUDA.some(m => m[0] === d.metodo) ? d.metodo : "avalancha", extra: Number(d.extra) || 0, lista: Array.isArray(d.lista) ? d.lista : [] }; }
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
  const ds = lista.filter(x => x.actual > 0.005).map((x, k) => ({ id: x.id, b: x.actual, r: (Number(x.tasa) || 0) / 100 / 12, min: Number(x.minimo) || 0, fin: 0, k }));
  const minimos = metodo === "minimos", total = () => ds.reduce((a, d) => a + Math.max(0, d.b), 0);
  const ordenar = {
    avalancha: (a, b) => b.r - a.r || a.b - b.b,
    bola: (a, b) => a.b - b.b,
    flujo: (a, b) => (a.min > 0 ? a.b / a.min : Infinity) - (b.min > 0 ? b.b / b.min : Infinity) || b.r - a.r,
    hibrido: (a, b) => { const q = d => d.b <= 3 * (d.min + Math.max(0, extra)); return (q(b) - q(a)) || (q(a) ? a.b - b.b : b.r - a.r || a.b - b.b); },
    propio: (a, b) => a.k - b.k,
  }[metodo] || ((a, b) => b.r - a.r);
  let mes = 0, interes = 0; const serie = [total()], pago1 = {}, intDe = {}, pagos = [];
  while (ds.some(d => d.b > 0.005) && mes < 600) {
    mes++;
    ds.forEach(d => { if (d.b > 0.005) { const i = d.b * d.r; d.b += i; interes += i; intDe[d.id] = (intDe[d.id] || 0) + i; } });
    const prev = Object.fromEntries(ds.map(d => [d.id, d.b]));
    const antes = mes === 1 ? Object.fromEntries(ds.map(d => [d.id, d.b])) : null;
    // Solo mínimos: cada deuda paga su cuota y nada más; con un método, lo que sobra rueda a la siguiente
    let bolsa = minimos ? 0 : Math.max(0, extra);
    ds.forEach(d => { if (d.b > 0.005) { const p = Math.min(d.min, d.b); d.b -= p; if (!minimos) bolsa += d.min - p; } else if (!minimos) bolsa += d.min; });
    for (const d of ds.filter(d => d.b > 0.005).sort(ordenar)) { if (bolsa <= 0.005) break; const p = Math.min(bolsa, d.b); d.b -= p; bolsa -= p; }
    if (antes) ds.forEach(d => { pago1[d.id] = antes[d.id] - d.b; });
    // Lo que recibe cada deuda este mes (para el calendario mes a mes), con su interés ya sumado
    if (mes <= 120) pagos.push({ por: Object.fromEntries(ds.map(d => [d.id, Math.max(0, (prev[d.id] ?? 0) - d.b)])), saldo: total() });
    ds.forEach(d => { if (d.b <= 0.005 && !d.fin) d.fin = mes; });
    serie.push(total());
  }
  const libre = !ds.some(d => d.b > 0.005);
  return { meses: libre ? mes : null, interes, serie, pago1, intDe, pagos, fin: Object.fromEntries(ds.map(d => [d.id, d.fin || null])) };
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
function saludDe(pref, { sinDeudas = false } = {}) {
  const dias = diasConMovs();
  let rango;
  if (pref) rango = dias.filter(f => f.startsWith(pref));
  else { const meses = [...new Set(dias.map(f => f.slice(0, 7)))].sort().slice(-12); rango = dias.filter(f => meses.includes(f.slice(0, 7))); }
  const s = sumar(rango.flatMap(movsDe)), nMeses = Math.max(1, new Set(rango.map(f => f.slice(0, 7))).size), mes = v => v / nMeses;
  const corte = pref ? pref.slice(0, 4) + "-12-31" : "9999-12-31", hasta = sumar(dias.filter(f => f <= corte).flatMap(movsDe));
  const actual = !pref || pref.startsWith(String(new Date().getFullYear()));
  const pasivoIng = s.cat["ing:pasivo"] || 0, ingAct = s.ing - pasivoIng;
  // Sin deudas: no hay saldo ni cuotas, y lo que hoy se va en pagos de deuda queda libre
  const pagosDeuda = sinDeudas ? (s.cat["gas:deudas"] || 0) : 0;
  const fijos = s.mochila.verde, variables = s.mochila.amarilla, gastos = s.gas - pagosDeuda, egresos = s.gas + s.aho + s.inv - pagosDeuda;
  const deudas = actual && !sinDeudas ? deudasHoy() : [], pasivos = deudas.reduce((a, x) => a + x.actual, 0), buenas = deudas.filter(x => x.tipo === "buena").reduce((a, x) => a + x.actual, 0);
  // Cuotas de deuda al mes: lo que de verdad se paga (pagos anotados en «Pago de deudas»), o las cuotas mínimas si son más
  const deudaMes = sinDeudas ? 0 : Math.max(deudas.filter(x => x.actual > 0).reduce((a, x) => a + (Number(x.minimo) || 0), 0), mes(s.cat["gas:deudas"] || 0));
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
  const cab = `<header class="med-cab"><b>${x.nombre}</b>${chipEst(x.estado)}</header><div class="med-val">${x.valor}</div>`;
  if (x.max == null || !Number.isFinite(x.v)) return `<article class="med e-${x.estado}">${cab}<p class="med-ayuda">${x.ayuda}</p></article>`;
  const p = v => Math.max(0, Math.min(100, v / x.max * 100)), meta = p(x.meta), meta2 = x.meta2 ? p(x.meta2) : null;
  // La zona buena del riel: a la derecha de la meta si más es mejor, a la izquierda si menos es mejor
  const zona = x.alto ? `left:${meta}%;right:${meta2 != null ? 100 - meta2 : 0}%` : `left:0;right:${100 - meta}%`;
  return `<article class="med e-${x.estado}">${cab}
    <div class="med-riel" role="img" aria-label="${esc(x.nombre)}: ${esc(x.valor)}; meta ${esc(x.metaTxt)}; ${EST_SALUD[x.estado][0]}"><span class="med-zona" style="${zona}"></span><span class="med-fill" style="width:${p(x.v)}%"></span>
      <span class="med-meta" style="left:${meta}%"></span>${meta2 != null ? `<span class="med-meta" style="left:${meta2}%"></span>` : ""}${x.v > x.max ? `<span class="med-mas" aria-hidden="true">›</span>` : ""}</div>
    <p class="med-ayuda">${x.ayuda}</p><p class="med-metatxt">Meta: ${x.metaTxt}</p></article>`;
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
/** Los indicadores, agrupados como se leen: el flujo del mes, el futuro, las deudas y la libertad. */
const GRUPOS_SALUD = [
  ["Tu flujo de dinero", "plata", ["Porcentaje de flujo neto", "Índice de liquidez", "Relación gasto ÷ ingreso"]],
  ["Construir tu futuro", "metas", ["Tasa de ahorro", "Ahorro + inversión ÷ ingresos", "Fondo de emergencia"]],
  ["Deudas", "deuda", ["Deuda ÷ ingreso (DTI)", "Índice de endeudamiento", "Pasivos buenos ÷ pasivos"]],
  ["Libertad y patrimonio", "cima", ["Ingresos pasivos ÷ ingresos", "Índice de libertad financiera", "Patrimonio neto ÷ ingresos anuales"]],
];
function saludHoja(pref) {
  const r = saludDe(pref);
  if (!r.s.n) return "";
  const periodo = pref ? `en ${pref.slice(0, 4)}` : "en tus últimos 12 meses", actual = !pref || pref.startsWith(String(new Date().getFullYear()));
  const m = v => plata(Math.round(v / r.nMeses));
  const cuenta = e => r.ind.filter(x => x.estado === e).length;
  const nivelK = r.isf == null ? "" : r.isf >= 80 ? "excelente" : r.isf >= 50 ? "buena" : r.isf >= 20 ? "moderada" : "riesgo";
  const usados = new Set();
  const grupos = GRUPOS_SALUD.map(([t, icono, nombres]) => {
    const xs = nombres.map(n => r.ind.find(x => x.nombre === n)).filter(Boolean); xs.forEach(x => usados.add(x));
    return xs.length ? `<section class="sal-grupo"><h4>${ic(icono)}${t}</h4><div class="sal-inds">${xs.map(medidor).join("")}</div></section>` : "";
  }).join("");
  const otros = r.ind.filter(x => !usados.has(x));
  return `<section class="sal-hero niv-${nivelK}">
      <div class="cab-lista-btn sal-hero-cab"><h3>Cómo está tu salud financiera</h3>${publicaFin() ? `<button class="btn mini tonal" data-acc="ascCompartir" data-arg="${pref}">${ic("publicar")} Compartir mi avance</button>` : ""}</div>
      ${r.isf != null ? `<div class="salud-cab"><div class="salud-anillo" style="--p:${r.isf}" role="img" aria-label="Índice de Salud Financiera ${r.isf} %"><b>${r.isf}</b><small>ISF %</small></div>
        <div class="salud-nivel"><span class="sal-nivel-chip">${r.nivel[0]}</span><p class="sal-nivel-txt">${r.nivel[1]}</p>
          <div class="isf-escala" aria-label="Escala del índice">${[["Riesgo", 0, 20], ["Moderada", 20, 50], ["Buena", 50, 80], ["Excelente", 80, 100]].map(([n, a, b]) => `<span class="${r.isf >= a && (r.isf < b || b === 100) ? "aqui" : ""}" style="flex:${b - a}">${n}</span>`).join("")}<i style="left:${r.isf}%"></i></div>
          <p class="suave sal-periodo">Índice de Salud Financiera ${periodo}.</p>
          <div class="sal-resumen">${[["ok", "en meta"], ["medio", "en atención"], ["bajo", "por mejorar"]].filter(([e]) => cuenta(e)).map(([e, t]) => `<span class="sal-cuenta e-${e}"><i aria-hidden="true">${EST_SALUD[e][1]}</i><b>${cuenta(e)}</b> ${t}</span>`).join("")}</div></div></div>` : ""}
    </section>
    <section class="sal-card">${barraPeso(r)}</section>
    ${grupos}${otros.length ? `<section class="sal-grupo"><h4>${ic("salud")}Otros</h4><div class="sal-inds">${otros.map(medidor).join("")}</div></section>` : ""}
    ${actual && (r.activos || r.pasivos) ? `<section class="sal-card">${balanceBarras(r)}</section>` : ""}
    ${(() => { const l = lineaMeses(pref, r); return l ? `<section class="sal-card">${l}</section>` : ""; })()}
    <section class="sal-card"><details class="salud-detalle"><summary>Ver el estado de cuentas en cifras</summary>
    <div class="asc-tabla"><table class="salud-tabla"><thead><tr><th>Concepto</th><th class="num">Promedio mensual (${r.nMeses} ${r.nMeses === 1 ? "mes" : "meses"})</th><th class="num">${pref ? "Total " + pref.slice(0, 4) : `Total (${r.nMeses} ${r.nMeses === 1 ? "mes" : "meses"})`}</th></tr></thead><tbody>
      ${r.filas.map(([n, v, fuerte]) => `<tr class="${fuerte ? "fuerte" : ""}"><th scope="row">${n}</th><td class="num${v < 0 ? " neg" : ""}">${m(v)}</td><td class="num${v < 0 ? " neg" : ""}">${plata(v)}</td></tr>`).join("")}
      <tr><th scope="row">Cuotas de deuda al mes</th><td class="num">${plata(r.deudaMes)}</td><td class="num"></td></tr>
      ${actual ? `<tr class="fuerte"><th scope="row">Activos totales</th><td class="num"></td><td class="num">${plata(r.activos)}</td></tr>
      <tr><th scope="row">Pasivos totales</th><td class="num"></td><td class="num">${plata(r.pasivos)}</td></tr>
      <tr class="fuerte"><th scope="row">Patrimonio neto</th><td class="num"></td><td class="num${r.neto < 0 ? " neg" : ""}">${plata(r.neto)}</td></tr>` : ""}
    </tbody></table></div></details></section>`;
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
const TIPOS_PASIVO = [["mala", "Mala (solo cuesta)"], ["buena", "Buena (produce)"]];
/** Una fila editable como las del día: asa para ordenar, campos que se cambian ahí mismo, documentos y quitar. */
function filaPatrimonio(dest, x, n, tipos, valor, acc) {
  return `<tr data-mov="${esc(x.id)}"><td class="asa-col">${n > 1 ? `<button type="button" class="asc-asa" data-mover-mov="${esc(x.id)}" data-orden="${dest}" aria-label="Arrastrar para cambiar el orden (o usa las flechas)" title="Arrastrar para cambiar el orden">${ic("arrastrar")}</button>` : ""}</td>
    <td><input value="${esc(x.nombre)}" maxlength="60" data-acc-change="${acc}" data-arg="${x.id}|nombre" aria-label="Nombre"></td>
    <td><select data-acc-change="${acc}" data-arg="${x.id}|tipo" aria-label="Tipo">${tipos.map(([k, t]) => `<option value="${k}" ${k === x.tipo ? "selected" : ""}>${t}</option>`).join("")}</select></td>
    <td class="num"><input class="num" inputmode="decimal" value="${esc(numAsc(valor))}" data-acc-change="${acc}" data-arg="${x.id}|valor" aria-label="Valor de ${esc(x.nombre)}"></td>
    <td class="acc-mov"><button class="rb${x.r?.length ? " con" : ""}" data-acc="patAdjuntos" data-arg="${dest === "activos" ? "act" : "deu"}:${x.id}" aria-label="Documentos (${x.r?.length || 0})" title="Documentos">${ic("adjunto")}${x.r?.length ? `<small>${x.r.length}</small>` : ""}</button><button class="rb" data-acc="${dest === "activos" ? "activoQuitar" : "deudaQuitar"}" data-arg="${x.id}" aria-label="Quitar ${esc(x.nombre)}">${ic("borrar")}</button></td></tr>`;
}
const formPatrimonio = (form, nombre, ph, tipos) => `<form class="asc-nuevo" data-form="${form}" style="margin-top:12px"><div class="asc-fila-nueva">
  <label class="campo"><span>${nombre}</span><input name="texto" maxlength="60" autocomplete="off" placeholder="${ph}" required></label>
  <label class="campo"><span>Tipo</span><select name="tipo">${tipos.map(([k, n]) => `<option value="${k}">${n}</option>`).join("")}</select></label>
  <label class="campo"><span>${form === "activoNuevo" ? "Valor hoy" : "Saldo hoy"}</span><input name="valor" inputmode="decimal" autocomplete="off" placeholder="${plata(0)}" required></label>
  <button class="btn lleno" type="submit">Agregar</button></div></form>`;
DET["finanzas.patrimonio"] = () => {
  if (finBloqueado()) return pantallaBloqueo();
  const l = activosDatos(), led = sumar(diasConMovs().flatMap(movsDe)).construido, deudas = deudasHoy();
  const act = l.reduce((a, x) => a + (Number(x.valor) || 0), 0) + led, pas = deudas.reduce((a, x) => a + x.actual, 0);
  const cabeza = (n, v) => `<thead><tr><th class="asa-col"><span class="oculto">Ordenar</span></th><th>${n}</th><th>Tipo</th><th class="num">${v}</th><th><span class="oculto">Documentos y quitar</span></th></tr></thead>`;
  return cab(av(ic("balanza"), "var(--ok)"), "Activos y pasivos", "Ascenso financiero · tu patrimonio neto", ojoAsc()) + cuerpo(
    migasAsc([["Balance general", "b"], ["Activos y pasivos", ""]]) +
    `<div class="asc-cifras"><div class="asc-cifra c-ing"><span>Activos totales</span><b>${plata(act)}</b></div><div class="asc-cifra c-gas"><span>Pasivos totales</span><b>${plata(pas)}</b></div><div class="asc-cifra c-disp"><span>Patrimonio neto</span><b class="${act - pas < 0 ? "neg" : ""}">${plata(act - pas)}</b></div></div>` +
    hoja(`<h3 style="margin:0 0 4px">Lo que tienes</h3><p class="suave" style="margin:0 0 10px">Un activo pone dinero en tu bolsillo o guarda valor: cuentas, inversiones, tu casa, tu negocio. Escribe su valor de hoy.</p>
      ${formPatrimonio("activoNuevo", "Activo", "Casa, carro, cuenta de ahorros…", TIPOS_ACTIVO)}
      <div class="asc-tabla" style="margin-top:14px"><table class="asc-movs asc-pat">${cabeza("Activo", "Valor hoy")}<tbody>
        ${l.map(x => filaPatrimonio("activos", x, l.length, TIPOS_ACTIVO, Number(x.valor) || 0, "activoCampo")).join("")}
        <tr class="fija"><td></td><th scope="row" class="pat-nom">Ahorro e inversión del libro de cuentas</th><td class="suave">Se suma solo</td><td class="num">${plata(led)}</td><td></td></tr>
      </tbody><tfoot><tr><th colspan="3" scope="row">Activos totales</th><td class="num">${plata(act)}</td><td></td></tr></tfoot></table></div>`) +
    hoja(`<div class="cab-lista-btn"><h3 style="margin:0">Lo que debes</h3><button class="btn mini" data-sel="deudas">${ic("deuda")} Salir de deudas</button></div>
      <p class="suave" style="margin:4px 0 10px">Una deuda buena produce (negocio, estudio, vivienda); una mala solo cuesta. La tasa y la cuota mínima se completan en Salir de deudas.</p>
      ${formPatrimonio("pasivoNuevo", "Pasivo", "Tarjeta, crédito de libranza, préstamo…", TIPOS_PASIVO)}
      ${deudas.length ? `<div class="asc-tabla" style="margin-top:14px"><table class="asc-movs asc-pat">${cabeza("Pasivo", "Saldo hoy")}<tbody>
        ${deudas.map(x => filaPatrimonio("deudas", x, deudas.length, TIPOS_PASIVO, x.actual, "pasivoCampo")).join("")}
      </tbody><tfoot><tr><th colspan="3" scope="row">Pasivos totales</th><td class="num">${plata(pas)}</td><td></td></tr></tfoot></table></div>`
        : `<p class="suave" style="margin:12px 0 0">Sin deudas anotadas.</p>`}`), "max-width:980px");
};
/** Guarda el orden que la persona eligió al arrastrar: los de un día, los activos o los pasivos. */
window.guardarOrden = function (dest, ids) {
  if (dest.startsWith("dia:")) return guardarOrdenDia(dest.slice(4), ids);
  const pos = new Map(ids.map((id, i) => [id, i])), ord = l => [...l].sort((a, b) => (pos.get(a.id) ?? 1e6) - (pos.get(b.id) ?? 1e6));
  if (dest === "activos") guardarActivos(ord(activosDatos()));
  else if (dest === "deudas") { const d = deudasDatos(); d.lista = ord(d.lista); guardarDeudas(d); }
};
Object.assign(ACC, {
  activoCampo(el, arg) {
    const [id, k] = arg.split("|"), l = activosDatos(), x = l.find(y => y.id === id); if (!x) return;
    if (k === "valor") { const v = Math.abs(montoFin(el.value)); if (!v) { toast("Escribe un valor mayor que cero"); pintarDetalle(); return; } x.valor = v; }
    else if (k === "nombre") { if (!el.value.trim()) { pintarDetalle(); return; } x.nombre = el.value.trim().slice(0, 60); }
    else x.tipo = el.value;
    guardarActivos(l);
  },
  activoQuitar(_, id) { const l = activosDatos(), x = l.find(y => y.id === id); guardarActivos(l.filter(y => y.id !== id)); if (x) toast(`${x.nombre} quitado`, { accion: "Deshacer", alHacer: () => guardarActivos([...activosDatos(), x]) }); },
  /** El saldo de hoy es lo que se debía menos lo pagado: al cambiarlo se ajusta lo que se debía. */
  pasivoCampo(el, arg) {
    const [id, k] = arg.split("|"), d = deudasDatos(), x = d.lista.find(y => y.id === id); if (!x) return;
    if (k === "valor") { const v = Math.abs(montoFin(el.value)); x.saldo = v + pagadoDe(id); }
    else if (k === "nombre") { if (!el.value.trim()) { pintarDetalle(); return; } x.nombre = el.value.trim().slice(0, 60); }
    else x.tipo = el.value === "buena" ? "buena" : "mala";
    guardarDeudas(d);
  },
  patAdjuntos(_, clave) { abrirRecibos(clave); },
});
window.FORMULARIOS = Object.assign(window.FORMULARIOS || {}, {
  activoNuevo(form, datos, texto) {
    const v = Math.abs(montoFin(datos.get("valor"))); if (!texto || !v) { toast("Escribe el activo y su valor"); return; }
    guardarActivos([...activosDatos(), { id: "a" + nuevoId(), nombre: texto.slice(0, 60), tipo: datos.get("tipo") || "otro", valor: v }]); form.reset(); toast("Activo agregado"); pintarDetalle();
    setTimeout(() => document.querySelector('[data-form="activoNuevo"] [name="texto"]')?.focus(), 50);
  },
  pasivoNuevo(form, datos, texto) {
    const saldo = Math.abs(montoFin(datos.get("valor"))); if (!texto || !saldo) { toast("Escribe el pasivo y su saldo"); return; }
    const d = deudasDatos(); d.lista.push({ id: "d" + nuevoId(), nombre: texto.slice(0, 60), saldo, tasa: 0, minimo: 0, tipo: datos.get("tipo") === "buena" ? "buena" : "mala" });
    guardarDeudas(d); form.reset(); toast("Pasivo agregado: completa su tasa y cuota en Salir de deudas"); pintarDetalle();
    setTimeout(() => document.querySelector('[data-form="pasivoNuevo"] [name="texto"]')?.focus(), 50);
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
/** Dónde se guardan los adjuntos: un movimiento del día ("mov:fecha|id"), un activo ("act:id") o un pasivo ("deu:id"). */
function adjuntoDe(clave) {
  const [k, resto] = [clave.slice(0, 3), clave.slice(4)];
  if (k === "act") return { x: () => activosDatos().find(y => y.id === resto), sub: x => `${esc(x.nombre)} · ${plata(Number(x.valor) || 0)}`, titulo: "Documentos",
    cambiar: fn => { const l = activosDatos(), y = l.find(z => z.id === resto); if (y) { fn(y); guardarActivos(l); } } };
  if (k === "deu") return { x: () => deudasHoy().find(y => y.id === resto), sub: x => `${esc(x.nombre)} · ${plata(x.actual)}`, titulo: "Documentos",
    cambiar: fn => { const d = deudasDatos(), y = d.lista.find(z => z.id === resto); if (y) { fn(y); guardarDeudas(d); } } };
  const [fecha, id] = resto.split("|");
  return { x: () => movsDe(fecha).find(y => y.id === id), sub: x => `${esc(x.n || catDe(x.t, x.c)?.[1] || "")} · ${plata(x.m)}`, titulo: "Recibos", cambiar: fn => cambiarMov(resto, fn) };
}
async function subirRecibo(clave, archivo) {
  if (!/^image\/|application\/pdf/.test(archivo.type)) { toast("Solo fotos o PDF"); return; }
  if (archivo.size > 15e6) { toast("El archivo pesa más de 15 MB"); return; }
  const f = await prepararArchivo(archivo), id = "r" + nuevoId(), ext = f.type === "application/pdf" ? "pdf" : (f.type.split("/")[1] || "jpg").replace("jpeg", "jpg");
  let ruta;
  if (Store.nube) {
    ruta = `${Store.nube.dueno}/${id}.${ext}`;
    const up = await Store.nube.sb.storage.from("recibos").upload(ruta, f, { contentType: f.type, upsert: false });
    if (up.error) { errorNube(up.error); return; }
  } else { ruta = `local:${id}.${ext}`; await IDB.poner(ruta, f); }
  adjuntoDe(clave).cambiar(x => { x.r = [...(x.r || []), { id, nom: String(archivo.name || "recibo").slice(0, 80), tipo: f.type, ruta }]; });
  toast("Archivo guardado");
}
async function urlRecibo(r) {
  if (r.ruta.startsWith("local:")) { const b = await IDB.leer(r.ruta); return b ? URL.createObjectURL(b) : null; }
  if (!Store.nube) return null;
  const { data, error } = await Store.nube.sb.storage.from("recibos").createSignedUrl(r.ruta, 600);
  if (error) { errorNube(error); return null; }
  return data.signedUrl;
}
async function abrirRecibos(clave) {
  const o = adjuntoDe(clave), x = o.x(); if (!x) return;
  const rs = x.r || [];
  const v = ventanaCuenta(`<h3 style="margin-top:0">${o.titulo}</h3><p class="suave" style="margin-top:0">${o.sub(x)}</p>
    <div class="recibos">${rs.map(r => `<div class="recibo" data-ruta="${esc(r.ruta)}"><span class="recibo-vista">${r.tipo === "application/pdf" ? ic("pdf") : ic("foto")}</span><span class="recibo-nom">${esc(r.nom)}</span>
      <button class="btn mini" data-ver="${esc(r.id)}">Ver</button><button class="rb" data-quitar="${esc(r.id)}" aria-label="Quitar ${esc(r.nom)}">${ic("borrar")}</button></div>`).join("") || `<p class="suave">${o.titulo === "Recibos" ? "Aún no hay recibos. Agrega la foto de la factura o el PDF para tener todo a mano." : "Aún no hay documentos. Agrega la escritura, el extracto o el contrato para tener todo a mano."}</p>`}</div>
    <label class="btn lleno" style="display:inline-flex;gap:6px;align-items:center;cursor:pointer">${ic("adjunto")} Agregar foto o PDF<input type="file" accept="image/*,application/pdf" capture="environment" hidden></label>
    <p class="suave" style="font-size:12.5px">${Store.nube ? "Se guardan en tu carpeta privada; solo tú puedes abrirlos." : "En la demostración se guardan solo en este navegador."}</p>
    <div class="botones"><button class="btn" type="button" data-cerrar>Listo</button></div>`, v => {
    v.querySelector("[data-cerrar]").onclick = () => v.remove();
    v.querySelector("input[type=file]").onchange = async e => { const a = e.target.files?.[0]; if (!a) return; e.target.closest("label").classList.add("cargando"); await subirRecibo(clave, a); v.remove(); abrirRecibos(clave); };
    v.querySelectorAll("[data-ver]").forEach(b => b.onclick = async () => {
      const r = rs.find(y => y.id === b.dataset.ver), url = r && await urlRecibo(r); if (!url) { toast("No se pudo abrir el archivo"); return; }
      const fila = b.closest(".recibo"); fila.querySelector(".recibo-vista").innerHTML = r.tipo === "application/pdf" ? ic("pdf") : `<img src="${url}" alt="${esc(r.nom)}">`;
      window.open(url, "_blank", "noopener");
    });
    v.querySelectorAll("[data-quitar]").forEach(b => b.onclick = async () => {
      const r = rs.find(y => y.id === b.dataset.quitar); if (!r) return;
      o.cambiar(y => { y.r = (y.r || []).filter(z => z.id !== r.id); });
      if (r.ruta.startsWith("local:")) IDB.quitar(r.ruta); else Store.nube?.sb.storage.from("recibos").remove([r.ruta]);
      toast("Archivo quitado"); v.remove(); abrirRecibos(clave);
    });
  });
}

/* ---------- Pantalla: salir de deudas ---------- */
/** Medidor semicircular de Material 3 para el Índice de Salud Financiera (0 a 100). */
function medidorIsf(v, etiqueta, tono) {
  const a = Math.max(0, Math.min(100, v ?? 0)) / 100, L = Math.PI * 52;
  return `<figure class="isf-medidor ${tono}"><svg viewBox="0 0 120 70" aria-hidden="true"><path class="pista" d="M8 64a52 52 0 0 1 104 0"/>
    <path class="valor" d="M8 64a52 52 0 0 1 104 0" style="stroke-dasharray:${L * a} ${L}"/></svg>
    <b>${v == null ? "—" : v}</b><figcaption>${etiqueta}</figcaption></figure>`;
}
/** Así baja tu deuda: el método elegido (área) frente a pagar solo los mínimos (línea punteada), con cada deuda cerrada marcada. */
function graficaDeudas(plan, base, lista) {
  const n = Math.max(plan.serie.length, Math.min(base.serie.length, plan.serie.length * 2.2 | 0, 120)), alto = Math.max(...plan.serie, ...base.serie.slice(0, n), 1);
  const W = 640, H = 200, x = k => 8 + k / Math.max(1, n - 1) * (W - 16), y = v => 14 + (1 - v / alto) * (H - 40);
  const linea = s => s.slice(0, n).map((v, k) => `${k ? "L" : "M"}${x(k).toFixed(1)} ${y(v).toFixed(1)}`).join("");
  const area = linea(plan.serie) + `L${x(plan.serie.length - 1).toFixed(1)} ${H - 26}L${x(0)} ${H - 26}Z`;
  const hitos = lista.filter(d => plan.fin[d.id]).map(d => { const k = plan.fin[d.id]; return `<g class="hito"><circle cx="${x(k).toFixed(1)}" cy="${y(plan.serie[k] || 0).toFixed(1)}" r="5"/><title>${esc(d.nombre)}: pagada en ${mesesDespues(k)}</title></g>`; }).join("");
  const marcas = [0, Math.round((n - 1) / 2), n - 1].map(k => `<text x="${x(k).toFixed(1)}" y="${H - 8}" text-anchor="${k === 0 ? "start" : k === n - 1 ? "end" : "middle"}">${k === 0 ? "Hoy" : MES_C[(new Date().getMonth() + k) % 12] + " " + String(new Date(new Date().getFullYear(), new Date().getMonth() + k, 1).getFullYear()).slice(2)}</text>`).join("");
  return `<figure class="deuda-grafica"><svg viewBox="0 0 ${W} ${H}" role="img" aria-label="Tu deuda baja de ${plata(alto)} a cero en ${plan.meses || "—"} meses; con solo mínimos tardarías ${base.meses || "más de 50 años"}">
    <defs><linearGradient id="dg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="var(--burdeos)" stop-opacity=".32"/><stop offset="1" stop-color="var(--burdeos)" stop-opacity=".03"/></linearGradient></defs>
    <line class="eje" x1="8" x2="${W - 8}" y1="${H - 26}" y2="${H - 26}"/>
    <path class="base" d="${linea(base.serie)}"/><path class="area" d="${area}" fill="url(#dg)"/><path class="linea" d="${linea(plan.serie)}"/>${hitos}${marcas}
    <text class="tope" x="${W - 8}" y="${y(alto) - 2}" text-anchor="end">${plata(Math.round(alto))}</text></svg>
    <figcaption><span><i class="ley-metodo"></i>Tu método</span><span><i class="ley-base"></i>Solo mínimos</span><span><i class="ley-hito"></i>Deuda pagada</span></figcaption></figure>`;
}
/**
 * Así se aplica el método: el orden en que atacas tus deudas (y por qué), qué pagas este mes a cada una
 * y el calendario mes a mes con lo que recibe cada deuda y lo que debes al final de cada mes.
 */
function aplicarMetodo(d, plan, vivas, fechaFin) {
  const nombre = (METODOS_DEUDA.find(m => m[0] === d.metodo) || [])[1] || "";
  const porque = x => { const t = Number(x.tasa) || 0, mn = Number(x.minimo) || 0;
    return { avalancha: `${t.toLocaleString(ascLocale)} % de interés`, bola: `saldo de ${plata(x.actual)}`,
      flujo: mn ? `${(x.actual / mn).toLocaleString(ascLocale, { maximumFractionDigits: 1 })} meses de cuota (saldo ÷ mínimo)` : "sin mínimo",
      hibrido: x.actual <= 3 * (mn + d.extra) ? "se cierra en 3 meses o menos" : `${t.toLocaleString(ascLocale)} % de interés`, propio: "tu orden" }[d.metodo] || ""; };
  const orden = vivas.slice().sort((a, b) => (plan.fin[a.id] || 1e9) - (plan.fin[b.id] || 1e9));
  const mes1 = plan.pagos[0]?.por || {}, foco = orden.find(x => (mes1[x.id] || 0) > (Number(x.minimo) || 0) + 0.5);
  const totalMes = orden.reduce((a, x) => a + (mes1[x.id] || 0), 0);
  const filas = plan.pagos.slice(0, Math.min(plan.pagos.length, estado.deudasTodo ? 120 : 12));
  const corto = s => s.length > 14 ? s.slice(0, 13) + "…" : s;
  return hoja(`<h3 style="margin:0 0 4px">Así se aplica ${esc(nombre)}</h3>
    <p class="suave" style="margin:0 0 12px">Pagas el mínimo de todas y lo extra va a una sola. Cuando esa termina, todo lo que le pagabas pasa a la siguiente: la bola crece.</p>
    <h4 class="sub-h">El orden de ataque</h4>
    <ol class="ataque">${orden.map((x, i) => `<li><span class="orden">${i + 1}</span><div><b>${esc(x.nombre)}</b><small>${porque(x)} · la eliminas en ${fechaFin(plan.fin[x.id])}</small></div></li>`).join("")}</ol>
    <h4 class="sub-h">Este mes pagas así</h4>
    <div class="este-mes">${orden.map(x => { const v = mes1[x.id] || 0, extra = v - (Number(x.minimo) || 0) > 0.5;
      return `<div class="${extra ? "foco" : ""}"><span>${esc(x.nombre)}${extra ? ` <i>Recibe el extra</i>` : ""}</span><b>${plata(Math.round(v))}</b></div>`; }).join("")}
      <div class="total"><span>Total del mes</span><b>${plata(Math.round(totalMes))}</b></div></div>
    ${foco ? `<p class="suave" style="margin:8px 0 0;font-size:13px">Todo lo que puedes pagar de más va a <b>${esc(foco.nombre)}</b>; a las demás, solo su mínimo.</p>` : ""}
    <details class="calendario-deudas"${estado.deudasCal ? " open" : ""}><summary data-acc="deudasCal">Mes a mes: qué recibe cada deuda</summary>
      <div class="asc-tabla"><table><thead><tr><th>Mes</th>${orden.map(x => `<th class="num" title="${esc(x.nombre)}">${esc(corto(x.nombre))}</th>`).join("")}<th class="num">Debes al final</th></tr></thead><tbody>
      ${filas.map((f, k) => `<tr><th scope="row">${fechaFin(k + 1)}</th>${orden.map(x => { const v = f.por[x.id] || 0, fin = plan.fin[x.id] === k + 1, extra = v - (Number(x.minimo) || 0) > 0.5;
        return `<td class="num${fin ? " fin" : extra ? " extra" : ""}">${v > 0.5 ? plata(Math.round(v)) : "—"}${fin ? " ✓" : ""}</td>`; }).join("")}<td class="num">${plata(Math.round(f.saldo))}</td></tr>`).join("")}
      </tbody></table></div>
      ${plan.pagos.length > 12 ? `<button class="btn mini" data-acc="deudasTodo">${estado.deudasTodo ? "Ver solo 12 meses" : `Ver los ${Math.min(plan.pagos.length, 120)} meses`}</button>` : ""}
      <p class="suave" style="font-size:12.5px;margin:8px 0 0"><span class="ley-extra"></span> recibe el extra · ✓ la eliminas ese mes</p></details>`);
}
DET["finanzas.deudas"] = () => {
  if (finBloqueado()) return pantallaBloqueo();
  const d = deudasDatos(), lista = deudasHoy(), total = lista.reduce((a, x) => a + x.actual, 0), pagado = lista.reduce((a, x) => a + x.pagado, 0);
  const vivas = lista.filter(x => x.actual > 0);
  const planes = Object.fromEntries(METODOS_DEUDA.map(([m]) => [m, planDeudas(lista, m, d.extra)]));
  const plan = planes[d.metodo], base = planDeudas(lista, "minimos", 0);
  const conFecha = METODOS_DEUDA.filter(([m]) => planes[m].meses);
  const masRapido = conFecha.length ? Math.min(...conFecha.map(([m]) => planes[m].meses)) : null, menosInteres = conFecha.length ? Math.min(...conFecha.map(([m]) => planes[m].interes)) : null;
  const orden = vivas.slice().sort((a, b) => (plan.fin[a.id] || 1e9) - (plan.fin[b.id] || 1e9));
  const fechaFin = k => MES_C[(new Date().getMonth() + k) % 12] + " " + new Date(new Date().getFullYear(), new Date().getMonth() + k, 1).getFullYear();
  const duracion = k => k == null ? "Sin fecha" : k < 12 ? `${k} ${k === 1 ? "mes" : "meses"}` : `${Math.floor(k / 12)} ${k < 24 ? "año" : "años"}${k % 12 ? ` y ${k % 12} m` : ""}`;
  // Efecto de las deudas en la salud financiera: el índice hoy y el mismo cálculo sin deudas
  const hoyS = saludDe(""), sinS = saludDe("", { sinDeudas: true });
  const pts = hoyS.isf != null && sinS.isf != null ? sinS.isf - hoyS.isf : null;
  const pctF = v => v == null || !Number.isFinite(v) ? "—" : Math.round(v * 100) + " %";
  const efecto = !lista.length ? "" : hoja(`<h3 style="margin:0 0 4px">Lo que tus deudas le hacen a tu salud financiera</h3>
    ${hoyS.isf == null ? `<p class="suave" style="margin:0">Anota tus ingresos del mes en Hoy y aquí verás cuántos puntos te quitan tus deudas.</p>` : `
    <p class="suave" style="margin:0 0 12px">${pts > 0 ? `Hoy te quitan <b>${pts} ${pts === 1 ? "punto" : "puntos"}</b> del Índice de Salud Financiera. Cada deuda que cierras te los va devolviendo.` : "Tus deudas casi no pesan en tu salud financiera. Sigue así."}</p>
    <div class="isf-par">${medidorIsf(hoyS.isf, "Hoy, con tus deudas", hoyS.nivel?.[2] || "medio")}<span class="isf-flecha" aria-hidden="true">${ic("derecha")}</span>${medidorIsf(sinS.isf, `Libre de deudas${plan.meses ? ` · ${fechaFin(plan.meses)}` : ""}`, "ok")}</div>
    <div class="asc-tabla" style="margin-top:12px"><table><thead><tr><th>Indicador</th><th class="num">Hoy</th><th class="num">Libre de deudas</th></tr></thead><tbody>
      <tr><th scope="row">Deuda ÷ ingreso (cuotas)</th><td class="num">${pctF(hoyS.dti)}</td><td class="num">${pctF(sinS.dti)}</td></tr>
      <tr><th scope="row">Índice de endeudamiento</th><td class="num">${Number.isFinite(hoyS.endeuda) ? pctF(hoyS.endeuda) : "Sin activos"}</td><td class="num">${pctF(sinS.endeuda)}</td></tr>
      <tr><th scope="row">Cuotas de deuda al mes</th><td class="num">${plata(Math.round(hoyS.deudaMes))}</td><td class="num">${plata(0)}</td></tr>
      <tr><th scope="row">Patrimonio neto</th><td class="num">${plata(Math.round(hoyS.neto))}</td><td class="num">${plata(Math.round(sinS.neto))}</td></tr></tbody></table></div>`}`);
  return cab(av(ic("deuda"), "var(--ok)"), "Salir de deudas", "Ascenso financiero · tu plan para quedar libre") + cuerpo(
    migasAsc([["Balance general", "b"], ["Salir de deudas", ""]]) +
    (lista.length ? `<div class="asc-cifras"><div class="asc-cifra c-gas"><span>Debes hoy</span><b>${plata(total)}</b></div><div class="asc-cifra c-ing"><span>Ya pagaste</span><b>${plata(pagado)}</b></div>
      <div class="asc-cifra c-disp"><span>Libre de deudas</span><b>${total === 0 ? "¡Ya!" : plan.meses ? fechaFin(plan.meses) : "Sin fecha"}</b></div></div>` : "") +
    efecto +
    hoja(`<h3 style="margin:0 0 4px">Elige tu método</h3><p class="suave" style="margin:0 0 12px">Del libro Cargas financieras: paga el mínimo en todas y pon todo lo extra en una sola. Cuando la terminas, su cuota pasa a la siguiente.</p>
      ${campo("¿Cuánto más puedes pagar cada mes, además de los mínimos?", `<input type="text" inputmode="decimal" value="${d.extra ? esc(numAsc(d.extra)) : ""}" placeholder="${plata(0)}" data-acc-change="deudaExtra">`)}
      <div class="metodos-deuda" role="radiogroup" aria-label="Método para salir de deudas">${METODOS_DEUDA.map(([m, nombre, idea]) => { const p = planes[m], sel = d.metodo === m;
        const sellos = vivas.length && p.meses ? [p.meses === masRapido ? "Más rápido" : "", Math.abs(p.interes - menosInteres) < 1 ? "Menos intereses" : ""].filter(Boolean) : [];
        return `<button type="button" role="radio" aria-checked="${sel}" class="metodo-deuda" data-acc="deudaMetodo" data-arg="${m}">
          <span class="md-cab"><b>${nombre}</b>${sel ? ic("check") : ""}</span><span class="md-idea">${idea}</span>
          ${vivas.length ? `<span class="md-res">${p.meses ? `<b>${duracion(p.meses)}</b> · ${plata(Math.round(p.interes))} en intereses` : "La deuda no baja"}</span>` : ""}
          ${sellos.length ? `<span class="md-sellos">${sellos.map(t => `<i>${t}</i>`).join("")}</span>` : ""}</button>`; }).join("")}</div>
      ${vivas.length ? `<div class="plan-deudas">${plan.meses ? `<p><b>Quedas libre en ${mesesDespues(plan.meses)}</b> y pagas cerca de ${plata(Math.round(plan.interes))} en intereses.</p>
          ${base.meses == null || base.meses > plan.meses ? `<p class="suave">Pagando solo los mínimos ${base.meses ? `tardarías ${duracion(base.meses)} y pagarías ${plata(Math.round(base.interes))}` : "la deuda no se acabaría"}: con tu método ${base.meses ? `ahorras <b>${plata(Math.round(base.interes - plan.interes))}</b> y <b>${duracion(base.meses - plan.meses)}</b>` : "sí sales"}.</p>` : ""}`
        : `<p class="neg"><b>Con estos pagos la deuda no baja:</b> los intereses son mayores que los mínimos. Sube el pago extra o renegocia la tasa.</p>`}</div>
        ${plan.meses ? graficaDeudas(plan, base, vivas) : ""}` : ""}`) +
    (vivas.length && plan.meses ? aplicarMetodo(d, plan, vivas, fechaFin) : "") +
    hoja(`<div class="cab-lista-btn"><h3 style="margin:0">Tus deudas</h3>${d.metodo === "propio" ? `<span class="suave" style="font-size:13px">Ordénalas con las flechas</span>` : ""}</div>
      ${lista.length ? `<div class="deudas-tarjetas">${lista.map((x, k) => { const i = orden.indexOf(x), fin = plan.fin[x.id], ancho = plan.meses && fin ? fin / plan.meses * 100 : 0;
          // Impacto: esta deuda pagando solo su mínimo frente a lo que le toca con tu método
          const sola = x.actual > 0 ? planDeudas([x], "minimos", 0) : null, pagoMes = plan.pago1?.[x.id] || 0, extraMes = Math.max(0, pagoMes - (Number(x.minimo) || 0));
          const ahorroInt = sola && fin ? Math.max(0, sola.interes - (plan.intDe?.[x.id] || 0)) : 0, mesesAntes = sola && fin && sola.meses != null ? sola.meses - fin : 0;
          const impacto = !sola || !fin ? "" : sola.meses == null ? `Con solo su mínimo nunca la terminarías: con tu plan sí.`
            : mesesAntes > 0 ? `<b>${duracion(mesesAntes)} antes</b> y <b>${plata(Math.round(ahorroInt))}</b> menos en intereses que pagando solo su mínimo.` : `Pagando su mínimo termina en ${duracion(sola.meses)}.`;
          return `<article class="deuda-t${x.actual <= 0 ? " pagada" : ""}">
            <header>${i >= 0 ? `<span class="orden">${i + 1}</span>` : `<span class="orden ok">${ic("check")}</span>`}<div class="dt-nombre"><b>${esc(x.nombre)}</b>${x.tipo === "buena" ? `<small>Buena · produce</small>` : ""}</div>
              <div class="dt-saldo"><span>Debes hoy</span><b>${plata(x.actual)}</b></div></header>
            <div class="barra-deuda" title="Pagado ${plata(x.pagado)}"><i style="width:${x.saldo ? Math.min(100, x.pagado / x.saldo * 100) : 100}%"></i></div>
            ${x.actual > 0 ? `<div class="dt-plan"><div><span>Pagas al mes</span><b>${plata(Math.round(pagoMes))}</b>${extraMes > 0.5 ? `<small>mínimo ${plata(Number(x.minimo) || 0)} + ${plata(Math.round(extraMes))} extra</small>` : `<small>su mínimo</small>`}</div>
              <div><span>La eliminas en</span><b>${fin ? fechaFin(fin) : "Sin fecha"}</b>${fin ? `<span class="linea-tiempo"><i style="width:${Math.max(4, ancho)}%"></i></span>` : ""}</div></div>
              ${impacto ? `<p class="dt-impacto">${ic("grafica")}<span>${impacto}</span></p>` : ""}` : `<p class="dt-impacto">${ic("check")}<span>Pagada. Su cuota ya trabaja para las demás.</span></p>`}
            <div class="dt-campos"><label><span>Interés anual</span><span class="dt-input"><input class="num" inputmode="decimal" value="${esc(numAsc(Number(x.tasa) || 0))}" data-acc-change="deudaCampo" data-arg="${x.id}|tasa" aria-label="Interés anual de ${esc(x.nombre)}"> %</span></label>
              <label><span>Pago mínimo</span><input class="num" inputmode="decimal" value="${esc(numAsc(Number(x.minimo) || 0))}" data-acc-change="deudaCampo" data-arg="${x.id}|minimo" aria-label="Pago mínimo de ${esc(x.nombre)}"></label>
              <div class="dt-acciones">${d.metodo === "propio" ? `<button class="rb" data-acc="deudaMover" data-arg="${x.id}|-1" ${k ? "" : "disabled"} aria-label="Subir ${esc(x.nombre)}">${ic("arriba")}</button><button class="rb" data-acc="deudaMover" data-arg="${x.id}|1" ${k < lista.length - 1 ? "" : "disabled"} aria-label="Bajar ${esc(x.nombre)}">${ic("abajo")}</button>` : ""}<button class="rb" data-acc="deudaQuitar" data-arg="${x.id}" aria-label="Quitar ${esc(x.nombre)}">${ic("borrar")}</button>${x.actual > 0 ? `<button class="btn lleno" data-acc="deudaPagar" data-arg="${x.id}">Pagar</button>` : ""}</div></div>
          </article>`; }).join("")}</div>` : `<p class="suave">Anota cada deuda con lo que debes hoy, su interés anual y su pago mínimo. El plan se arma solo.</p>`}
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
  deudasCal(el) { setTimeout(() => { estado.deudasCal = el.closest("details")?.open; }, 0); },
  deudasTodo() { estado.deudasTodo = !estado.deudasTodo; estado.deudasCal = true; pintarDetalle(); },
  deudaMover(_, arg) { const [id, s] = arg.split("|"), d = deudasDatos(), k = d.lista.findIndex(y => y.id === id), j = k + Number(s);
    if (k < 0 || j < 0 || j >= d.lista.length) return; [d.lista[k], d.lista[j]] = [d.lista[j], d.lista[k]]; guardarDeudas(d); },
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
  ascRecibos(_, arg) {
    const [f, id] = arg.split("|"), x = movsDe(f).find(y => y.id === id);
    // Un fijo del mes pasa a ser movimiento propio de ese mes para poder llevar su recibo
    if (x?.virtual) { const propio = cambiarMov(arg, () => {}); if (propio) return abrirRecibos(`mov:${f}|${propio.id}`); }
    abrirRecibos("mov:" + arg);
  },
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
