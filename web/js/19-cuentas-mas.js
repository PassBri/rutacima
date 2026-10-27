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
/* ---------- Pagos de cada deuda ----------
 * Un movimiento del libro paga una deuda cuando la nombra (d): los anotados con «Pagar», los que la persona
 * marca en el día o en «Tus pagos de deudas» y los fijos marcados (cada mes). El saldo de una deuda es el de
 * la fecha en que se anotó o se corrigió (desde): solo los pagos posteriores lo bajan, así los pagos viejos que
 * ya estaban descontados en el extracto no se restan dos veces. Su cuota, si no se escribió, es lo que se le
 * pagó el último mes con pagos anotados. */
function pagosDeDeudas() {
  const hoyI = iso(hoyFecha()), r = [];
  diasConMovs().forEach(f => { if (f <= hoyI) movsDe(f).forEach(x => { if (x.t === "gas" && !x.prog) r.push([f, x]); }); });
  return r;
}
const tsMov = x => Number(String(x.id || "").replace(/\D/g, "")) || 0;
const cuentaParaSaldo = (deuda, f, x) => !deuda.desde || f > deuda.desde || (f === deuda.desde && tsMov(x) > (Number(deuda.desdeT) || 0));
function pagadoDe(id, pagos = pagosDeDeudas()) {
  const deuda = deudasDatos().lista.find(y => y.id === id) || {};
  // Lo que baja el saldo: lo que fue a capital (cap) si se anotó; si no, todo el pago
  return pagos.reduce((s, [f, x]) => s + (x.d === id && cuentaParaSaldo(deuda, f, x) ? Number(x.cap ?? x.m) || 0 : 0), 0);
}
function cuotaVinculada(id, pagos = pagosDeDeudas()) {
  const porMes = {}; pagos.forEach(([f, x]) => { if (x.d === id) porMes[f.slice(0, 7)] = (porMes[f.slice(0, 7)] || 0) + (Number(x.m) || 0); });
  const ult = Object.keys(porMes).sort().at(-1); return ult ? porMes[ult] : 0;
}
const saldoHoy = (x, pagos) => Math.max(0, (Number(x.saldo) || 0) - pagadoDe(x.id, pagos));
function deudasHoy() {
  const pagos = pagosDeDeudas();
  return deudasDatos().lista.map(x => { const propio = Number(x.minimo) || 0, cuota = propio ? 0 : cuotaVinculada(x.id, pagos);
    const h = { ...x, minimoPropio: propio, minimo: propio || cuota, cuotaAuto: !propio && cuota > 0, pagado: pagadoDe(x.id, pagos), actual: saldoHoy(x, pagos),
      nPagos: pagos.filter(([, y]) => y.d === x.id).length };
    h.avance = avanceCredito(h, pagos); return h; });
}
/**
 * El avance de un crédito por cuotas: cuántas lleva (las que escribió la persona más los meses con pagos anotados
 * después), cuántas faltan y en qué mes termina, y de lo pagado cuánto fue a capital y cuánto a intereses.
 * El monto prestado, si no se escribió, sale de la cuota, la tasa y el número de cuotas (amortización francesa).
 */
function avanceCredito(x, pagos) {
  const n = Math.round(Number(x.cuotas) || 0); if (!n) return null;
  const meses = new Set(pagos.filter(([f, y]) => y.d === x.id && !y.ab && (!x.pagadasF || f > x.pagadasF || (f === x.pagadasF && tsMov(y) > (Number(x.pagadasT) || 0)))).map(([f]) => f.slice(0, 7)));
  const k = Math.min(n, Math.round(Number(x.pagadas) || 0) + meses.size), c = Number(x.minimo) || 0, r = (Number(x.tasa) || 0) / 100 / 12;
  const prestado = Number(x.prestado) || (c ? (r ? c * (1 - Math.pow(1 + r, -n)) / r : c * n) : 0);
  // Capital pagado: con el monto prestado escrito, lo prestado menos lo que se debe hoy; si es estimado, la tabla de amortización
  const saldoK = r ? prestado * Math.pow(1 + r, k) - c * (Math.pow(1 + r, k) - 1) / r : prestado - c * k;
  const pagadoTotal = k * c, capital = prestado ? Math.min(pagadoTotal, Math.max(0, prestado - (Number(x.prestado) ? x.actual : saldoK))) : 0, intereses = Math.max(0, pagadoTotal - capital);
  const faltan = n - k, d = new Date(); d.setDate(1); d.setMonth(d.getMonth() + faltan);
  return { n, k, faltan, fin: `${MESES[d.getMonth()]} de ${d.getFullYear()}`, pagadoTotal, capital, intereses, prestado, estimado: !Number(x.prestado) };
}
/** El saldo de hoy pasa a ser el punto de partida: los pagos de antes ya no lo vuelven a bajar. */
function fijarSaldo(x, v = saldoHoy(x)) { x.saldo = Math.max(0, v); x.desde = iso(hoyFecha()); x.desdeT = nuevoId(); }

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
    const intMes = {};
    ds.forEach(d => { if (d.b > 0.005) { const i = d.b * d.r; d.b += i; interes += i; intDe[d.id] = (intDe[d.id] || 0) + i; intMes[d.id] = i; } });
    const prev = Object.fromEntries(ds.map(d => [d.id, d.b]));
    const antes = mes === 1 ? Object.fromEntries(ds.map(d => [d.id, d.b])) : null;
    // Solo mínimos: cada deuda paga su cuota y nada más; con un método, lo que sobra rueda a la siguiente
    let bolsa = minimos ? 0 : Math.max(0, extra);
    ds.forEach(d => { if (d.b > 0.005) { const p = Math.min(d.min, d.b); d.b -= p; if (!minimos) bolsa += d.min - p; } else if (!minimos) bolsa += d.min; });
    for (const d of ds.filter(d => d.b > 0.005).sort(ordenar)) { if (bolsa <= 0.005) break; const p = Math.min(bolsa, d.b); d.b -= p; bolsa -= p; }
    if (antes) ds.forEach(d => { pago1[d.id] = antes[d.id] - d.b; });
    // Lo que recibe cada deuda este mes (para el calendario mes a mes), con su interés ya sumado
    if (mes <= 120) pagos.push({ por: Object.fromEntries(ds.map(d => [d.id, Math.max(0, (prev[d.id] ?? 0) - d.b)])), int: intMes, saldo: total() });
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
  // Fondo de emergencia: el ahorro de «Emergencia» más el efectivo y las cuentas declarados en Activos
  const liquidos = actual ? activosDatos().filter(x => x.tipo === "liquidez").reduce((a, x) => a + (Number(x.valor) || 0), 0) : 0;
  const emergencia = (hasta.cat["aho:emergencia"] || 0) + liquidos, fondo = mes(fijos) > 0 ? emergencia / mes(fijos) : null;
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
    ["Fondo de emergencia", r.fondo, v => num(v) + " meses", est(r.fondo, 3, 1), "Meses de gastos fijos que cubren tu ahorro de emergencia y tu efectivo y cuentas. Ideal: de 3 a 6."],
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
/** El nombre y el valor de un activo o pasivo nuevo. Si se escribieron al revés (el número en el nombre
 *  y el texto en el valor), se acomodan solos; si falta algo, se dice qué. */
function nombreYValor(nombre, valor, que) {
  const esNum = t => /\d/.test(t || "") && /^[\s$\d.,']+$/.test(t || "");
  nombre = String(nombre || "").trim(); valor = String(valor || "").trim();
  if (!esNum(valor) && esNum(nombre)) [nombre, valor] = [valor, nombre];
  const v = Math.abs(montoFin(valor));
  if (!nombre) toast(`Escribe el nombre del ${que}`);
  else if (!v) toast(`Escribe ${que === "activo" ? "su valor" : "su saldo"} en números (ejemplo: 50.000.000)`);
  return [nombre, v];
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
    if (k === "valor") fijarSaldo(x, Math.abs(montoFin(el.value)));
    else if (k === "nombre") { if (!el.value.trim()) { pintarDetalle(); return; } x.nombre = el.value.trim().slice(0, 60); }
    else x.tipo = el.value === "buena" ? "buena" : "mala";
    guardarDeudas(d);
  },
  patAdjuntos(_, clave) { abrirRecibos(clave); },
});
window.FORMULARIOS = Object.assign(window.FORMULARIOS || {}, {
  activoNuevo(form, datos, texto0) {
    const [texto, v] = nombreYValor(texto0, datos.get("valor"), "activo"); if (!texto || !v) return;
    guardarActivos([...activosDatos(), { id: "a" + nuevoId(), nombre: texto.slice(0, 60), tipo: datos.get("tipo") || "otro", valor: v }]); form.reset(); toast("Activo agregado"); pintarDetalle();
    setTimeout(() => document.querySelector('[data-form="activoNuevo"] [name="texto"]')?.focus(), 50);
  },
  pasivoNuevo(form, datos, texto0) {
    const [texto, saldo] = nombreYValor(texto0, datos.get("valor"), "pasivo"); if (!texto || !saldo) return;
    const d = deudasDatos(); d.lista.push({ id: "d" + nuevoId(), nombre: texto.slice(0, 60), saldo, tasa: 0, minimo: 0, tipo: datos.get("tipo") === "buena" ? "buena" : "mala", desde: iso(hoyFecha()), desdeT: nuevoId() });
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
/** Minutos sin uso tras los que el módulo se vuelve a cerrar (1, 5 o 15; por defecto 5). */
const finMinutos = () => [1, 5, 15].includes(Number(respuesta("fin#cierre"))) ? Number(respuesta("fin#cierre")) : 5;
const finBloqueado = () => (window.FINC?.activo() && !FINC.dek) || (bloqueoFin() && !(estado.ascAbierto && Date.now() - estado.ascAbierto < finMinutos() * 60 * 1000));
/** Cerrar el módulo también olvida la llave del cifrado. */
const finCerrar = () => { estado.ascAbierto = 0; window.FINC?.cerrar(); };
/* Cada toque o tecla dentro del módulo cuenta como uso; al pasar el tiempo sin uso, se cierra solo */
["pointerdown", "keydown"].forEach(t => document.addEventListener(t, () => { if (estado.sec === "finanzas" && estado.ascAbierto && !finBloqueado()) estado.ascAbierto = Date.now(); }, true));
setInterval(() => { if (bloqueoFin() && estado.ascAbierto && Date.now() - estado.ascAbierto >= finMinutos() * 60 * 1000) { finCerrar(); if (estado.sec === "finanzas") pintarDetalle(); } }, 10000);
/* Intentos: tras 5 PIN errados, una espera que crece (30 s, 1 min, 5 min, 15 min) y que sobrevive a recargar la página */
const FIN_ESPERAS = [30, 60, 300, 900];
const finIntentos = () => { try { return JSON.parse(localStorage.getItem("suroute-fin-intentos") || "{}"); } catch { return {}; } };
const guardarIntentos = o => { try { localStorage.setItem("suroute-fin-intentos", JSON.stringify(o)); } catch {} };
const finEspera = () => Math.max(0, (finIntentos().hasta || 0) - Date.now());
/* Passkey: abrir con la huella, el rostro o Windows Hello de este aparato (WebAuthn). La credencial vive en el aparato. */
const b64u = b => btoa(String.fromCharCode(...new Uint8Array(b))).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
const desdeB64u = t => Uint8Array.from(atob(t.replace(/-/g, "+").replace(/_/g, "/") + "===".slice((t.length + 3) % 4)), c => c.charCodeAt(0));
const passkeyId = () => { try { return localStorage.getItem("suroute-fin-passkey") || ""; } catch { return ""; } };
const passkeyPosible = () => !!window.PublicKeyCredential && !!navigator.credentials?.create && isSecureContext;
async function crearPasskey() {
  const cred = await navigator.credentials.create({ publicKey: {
    rp: { name: "SuRoute" }, challenge: crypto.getRandomValues(new Uint8Array(32)),
    user: { id: crypto.getRandomValues(new Uint8Array(16)), name: "Ascenso financiero", displayName: "Ascenso financiero · SuRoute" },
    pubKeyCredParams: [{ type: "public-key", alg: -7 }, { type: "public-key", alg: -257 }],
    authenticatorSelection: { authenticatorAttachment: "platform", userVerification: "required", residentKey: "discouraged" }, timeout: 60000 } });
  try { localStorage.setItem("suroute-fin-passkey", b64u(cred.rawId)); } catch {}
}
async function usarPasskey() {
  await navigator.credentials.get({ publicKey: { challenge: crypto.getRandomValues(new Uint8Array(32)), userVerification: "required", timeout: 60000,
    allowCredentials: [{ type: "public-key", id: desdeB64u(passkeyId()) }] } });
}
document.addEventListener("visibilitychange", () => { if (document.hidden) finCerrar(); });
function pantallaBloqueo() {
  if (estado.finPideCodigo && window.pantallaCodigo) return pantallaCodigo();
  const espera = finEspera();
  return cab(av(ic("candado"), "var(--ok)"), "Ascenso financiero", "Privado · bloqueado") + cuerpo(hoja(`<div class="bloqueo-fin">${ic("candado", "i grande")}
    <h3>Tus finanzas están protegidas</h3><p class="suave">${passkeyId() ? "Ábrelo con tu huella o rostro, o con tu PIN." : "Escribe tu PIN para abrir el libro de cuentas."} Se vuelve a cerrar tras ${finMinutos()} ${finMinutos() === 1 ? "minuto" : "minutos"} sin uso o al salir.</p>
    ${passkeyId() && passkeyPosible() ? `<button class="btn lleno" data-acc="ascPasskey" style="margin:0 auto 14px;display:flex">${ic("huella")} Abrir con huella o rostro</button>` : ""}
    <form data-form="ascPin" class="form" style="max-width:260px;margin:0 auto"><input name="texto" type="password" inputmode="numeric" autocomplete="off" maxlength="6" pattern="[0-9]{4,6}" aria-label="PIN" placeholder="PIN" ${espera ? "disabled" : ""} required style="text-align:center;letter-spacing:.5em;font-size:22px">
      <button class="btn lleno" type="submit" ${espera ? "disabled" : ""}>Abrir</button></form>
    ${espera ? `<p class="suave">Demasiados intentos. Espera ${espera > 90000 ? Math.ceil(espera / 60000) + " minutos" : Math.ceil(espera / 1000) + " segundos"}.</p>` : ""}</div>`), "max-width:560px");
}
function privacidadHoja() {
  const pub = publicaFin();
  return hoja(`<h3 style="margin:0 0 4px">Privacidad</h3><p class="suave" style="margin:0 0 10px">Tus cuentas son tuyas. Elige si son solo para ti o si quieres compartir tu avance (solo porcentajes, nunca montos).</p>
    <div class="seg" role="radiogroup" aria-label="Privacidad" style="grid-template-columns:1fr 1fr;max-width:420px"><button type="button" role="radio" aria-checked="${!pub}" class="seg-b t-gas" data-acc="ascVisibilidad" data-arg="privada">Privada</button><button type="button" role="radio" aria-checked="${pub}" class="seg-b t-ing" data-acc="ascVisibilidad" data-arg="publica">Pública</button></div>
    ${pub ? `<p class="suave" style="margin:12px 0 0">Puedes compartir tu salud financiera y tu avance contra las deudas en la comunidad. Los montos, los recibos y los movimientos siguen siendo privados.</p>`
      : `<div class="capas"><div class="capa">${ic("check")}<span><b>Solo tu cuenta</b><small>Nadie más puede leer tus movimientos: la base de datos solo los entrega a tu sesión.</small></span></div>
        <div class="capa">${ic("candado")}<span><b>Bloqueo con PIN</b><small>${bloqueoFin() ? `Activo: el módulo pide tu PIN y se cierra tras ${finMinutos()} ${finMinutos() === 1 ? "minuto" : "minutos"} sin uso o al salir. En la app se abre con tu huella o rostro.` : "Pide un PIN para abrir este módulo en la web (en la app, tu huella o rostro)."}</small></span>
          ${bloqueoFin() ? `<button class="btn mini" data-acc="ascQuitarPin">Quitar</button>` : `<button class="btn mini lleno" data-acc="ascPonerPin">Activar</button>`}</div>
        ${bloqueoFin() ? `<div class="capa">${ic("reloj")}<span><b>Se cierra solo</b><small>Tras estos minutos sin uso, o al cambiar de pestaña.</small></span>
          <span class="seg cierre-min" role="radiogroup" aria-label="Minutos sin uso">${[1, 5, 15].map(m => `<button type="button" role="radio" class="seg-b" aria-checked="${finMinutos() === m}" data-acc="ascCierre" data-arg="${m}">${m} min</button>`).join("")}</span></div>
        ${passkeyPosible() ? `<div class="capa">${ic("huella")}<span><b>Huella, rostro o Windows Hello</b><small>${passkeyId() ? "Activo en este aparato: abres sin escribir el PIN." : "Abre el módulo con el desbloqueo de este aparato, sin escribir el PIN. El PIN queda de respaldo."}</small></span>
          ${passkeyId() ? `<button class="btn mini" data-acc="ascPasskeyQuitar">Quitar</button>` : `<button class="btn mini lleno" data-acc="ascPasskeyCrear">Activar</button>`}</div>` : ""}` : ""}
        <div class="capa">${ic("escudo")}<span><b>Cifrado de extremo a extremo</b><small>${window.FINC?.activo() ? "Activo: tus montos se cifran en tu aparato antes de guardarse. Nadie más puede leerlos. Guarda tu código de recuperación de 12 palabras." : bloqueoFin() ? "Cifra tus montos en tu aparato con una llave que solo tú tienes, con un código de recuperación de 12 palabras." : "Activa primero el bloqueo con PIN para poder cifrar tus finanzas."}</small></span>
          ${window.FINC?.activo() ? `<button class="btn mini" data-acc="finCifradoQuitar">Quitar</button>` : bloqueoFin() ? `<button class="btn mini lleno" data-acc="finCifradoActivar">Activar</button>` : ""}</div>
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
      ${(() => { const n = {}; orden.forEach(x => { n[x.id] = x.avance ? x.avance.k : 0; });   // el número de cada cuota sigue desde las ya pagadas
        return filas.map((f, k) => `<tr><th scope="row">${fechaFin(k + 1)}</th>${orden.map(x => { const v = f.por[x.id] || 0, fin = plan.fin[x.id] === k + 1, extra = v - (Number(x.minimo) || 0) > 0.5;
          if (v > 0.5) n[x.id]++;
          const i = Math.min(v, f.int?.[x.id] || 0), cap = Math.max(0, v - i);
          return `<td class="num${fin ? " fin" : extra ? " extra" : ""}">${v > 0.5 ? `<b>${plata(Math.round(v))}${fin ? " ✓" : ""}</b><small class="cal-cuota">Cuota ${n[x.id]}${x.avance ? ` de ${x.avance.n}` : ""}</small><small class="cal-det">Capital ${plata(Math.round(cap))}</small><small class="cal-det">Interés ${plata(Math.round(i))}</small>` : "—"}</td>`; }).join("")}<td class="num">${plata(Math.round(f.saldo))}</td></tr>`).join(""); })()}
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
  return cab(av(ic("deuda"), "var(--ok)"), "Salir de deudas", "Ascenso financiero · tu plan para quedar libre", ojoAsc()) + cuerpo(
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
            <header>${i >= 0 ? `<span class="orden">${i + 1}</span>` : `<span class="orden ok">${ic("check")}</span>`}<div class="dt-nombre"><input class="dt-nombre-in" value="${esc(x.nombre)}" maxlength="60" data-acc-change="pasivoCampo" data-arg="${x.id}|nombre" aria-label="Nombre de la deuda" title="Toca para cambiar el nombre">
                <span class="dt-sub"><select data-acc-change="pasivoCampo" data-arg="${x.id}|tipo" aria-label="Tipo de deuda"><option value="mala" ${x.tipo !== "buena" ? "selected" : ""}>Mala · solo cuesta</option><option value="buena" ${x.tipo === "buena" ? "selected" : ""}>Buena · produce</option></select>${x.nPagos ? `<small>${x.nPagos} ${x.nPagos === 1 ? "pago anotado" : "pagos anotados"}</small>` : ""}</span></div>
              <div class="dt-saldo"><span>Debes hoy</span><b>${plata(x.actual)}</b></div></header>
            <div class="barra-deuda" title="Pagado ${plata(x.pagado)}"><i style="width:${x.saldo ? Math.min(100, x.pagado / x.saldo * 100) : 100}%"></i></div>
            ${x.actual > 0 ? `<div class="dt-plan"><div><span>Pagas al mes</span><b>${plata(Math.round(pagoMes))}</b>${extraMes > 0.5 ? `<small>mínimo ${plata(Number(x.minimo) || 0)} + ${plata(Math.round(extraMes))} extra</small>` : `<small>${x.cuotaAuto ? "su cuota (de tus pagos anotados)" : "su mínimo"}</small>`}</div>
              <div><span>La eliminas en</span><b>${fin ? fechaFin(fin) : "Sin fecha"}</b>${fin ? `<span class="linea-tiempo"><i style="width:${Math.max(4, ancho)}%"></i></span>` : ""}</div></div>
              ${impacto ? `<p class="dt-impacto">${ic("grafica")}<span>${impacto}</span></p>` : ""}` : `<p class="dt-impacto">${ic("check")}<span>Pagada. Su cuota ya trabaja para las demás.</span></p>`}
            ${x.avance ? `<div class="dt-cuotas"><div class="dt-cuotas-cab"><b>Cuota ${x.avance.k} de ${x.avance.n}</b><span>${x.avance.faltan ? `faltan ${x.avance.faltan} · terminas en ${x.avance.fin}` : "¡todas pagadas!"}</span></div>
              <span class="linea-tiempo"><i style="width:${Math.max(3, x.avance.k / x.avance.n * 100)}%"></i></span>
              ${x.minimo ? `<p>Has pagado <b>${plata(Math.round(x.avance.pagadoTotal))}</b>: ${plata(Math.round(x.avance.capital))} a capital y ${plata(Math.round(x.avance.intereses))} a intereses. Debes <b>${plata(x.actual)}</b>.${x.avance.estimado ? ` <small>Prestado estimado: ${plata(Math.round(x.avance.prestado))} (con la cuota, la tasa y el plazo).</small>` : ""}</p>` : `<p class="suave">Escribe la cuota para ver cuánto va a capital y cuánto a intereses.</p>`}</div>`
              : x.actual > 0 ? `<p class="suave dt-sin-cuotas">Escribe cuántas cuotas tiene el crédito y cuántas llevas para ver cuántas te faltan y cuánto has pagado a capital e intereses.</p>` : ""}
            <div class="dt-campos"><label><span>Debes hoy</span><input class="num" inputmode="decimal" value="${esc(numAsc(x.actual))}" data-acc-change="pasivoCampo" data-arg="${x.id}|valor" aria-label="Lo que debes hoy de ${esc(x.nombre)}" style="width:130px"></label><label><span>Interés anual</span><span class="dt-input"><input class="num" inputmode="decimal" value="${esc(numAsc(Number(x.tasa) || 0))}" data-acc-change="deudaCampo" data-arg="${x.id}|tasa" aria-label="Interés anual de ${esc(x.nombre)}"> %</span></label>
              <label><span>Pago mínimo</span><input class="num" inputmode="decimal" value="${x.minimoPropio ? esc(numAsc(x.minimoPropio)) : ""}" placeholder="${x.cuotaAuto ? esc(numAsc(x.minimo)) : "0"}" data-acc-change="deudaCampo" data-arg="${x.id}|minimo" aria-label="Pago mínimo de ${esc(x.nombre)}"></label>
              <label><span>N.º de cuotas</span><input class="num" inputmode="numeric" value="${x.cuotas ? esc(String(x.cuotas)) : ""}" placeholder="—" data-acc-change="deudaCampo" data-arg="${x.id}|cuotas" aria-label="Número de cuotas del crédito ${esc(x.nombre)}"></label>
              <label><span>Pagadas hoy</span><input class="num" inputmode="numeric" value="${x.avance ? esc(String(x.avance.k)) : x.pagadas ? esc(String(x.pagadas)) : ""}" placeholder="—" data-acc-change="deudaCampo" data-arg="${x.id}|pagadas" aria-label="Cuotas pagadas hasta hoy de ${esc(x.nombre)}"></label>
              <label><span>Monto prestado</span><input class="num" inputmode="decimal" value="${x.prestado ? esc(numAsc(x.prestado)) : ""}" placeholder="opcional" data-acc-change="deudaCampo" data-arg="${x.id}|prestado" aria-label="Monto prestado de ${esc(x.nombre)}"></label>
              <div class="dt-acciones">${d.metodo === "propio" ? `<button class="rb" data-acc="deudaMover" data-arg="${x.id}|-1" ${k ? "" : "disabled"} aria-label="Subir ${esc(x.nombre)}">${ic("arriba")}</button><button class="rb" data-acc="deudaMover" data-arg="${x.id}|1" ${k < lista.length - 1 ? "" : "disabled"} aria-label="Bajar ${esc(x.nombre)}">${ic("abajo")}</button>` : ""}<button class="rb" data-acc="deudaQuitar" data-arg="${x.id}" aria-label="Quitar ${esc(x.nombre)}">${ic("borrar")}</button>${x.actual > 0 ? `<button class="btn lleno" data-acc="deudaPagar" data-arg="${x.id}">Pagar</button>` : ""}</div></div>
          </article>`; }).join("")}</div>` : `<p class="suave">Anota cada deuda con lo que debes hoy, su interés anual y su pago mínimo. El plan se arma solo.</p>`}
      <form class="asc-nuevo" data-form="deudaNueva" style="margin-top:14px"><div class="asc-fila-nueva deuda-nueva">
        <label class="campo"><span>Deuda</span><input name="texto" maxlength="60" placeholder="Tarjeta de crédito" required></label>
        <label class="campo"><span>Debes hoy</span><input name="saldo" inputmode="decimal" placeholder="${plata(1000000)}" required></label>
        <label class="campo"><span>Interés anual %</span><input name="tasa" inputmode="decimal" placeholder="28"></label>
        <label class="campo"><span>Pago mínimo</span><input name="minimo" inputmode="decimal" placeholder="${plata(80000)}"></label>
        <label class="campo"><span>Tipo</span><select name="tipo"><option value="mala">Mala · solo cuesta</option><option value="buena">Buena · produce (negocio, estudio, vivienda)</option></select></label>
        <button class="btn lleno" type="submit">Agregar</button></div></form>`) +
    (lista.length ? pagosDeudasHoja(lista) : "") +
    `<p class="suave" style="font-size:13px">Cada pago se anota en el libro de cuentas de hoy como gasto en "Pago de deudas" y baja el saldo. Lee más en la guía <button class="enlace" data-acc="aprendeFinanzas">Cargas financieras</button>.</p>`, "max-width:980px");
};

/* ---------- Tus pagos de deudas: identificar a qué deuda va cada cuota anotada en el libro ---------- */
const normPago = t => String(t || "").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/\s+/g, " ").trim();
/** Los pagos agrupados: un fijo (todos sus meses) o un mismo concepto. Solo los de la categoría «Pago de deudas»
 *  o los que ya nombran una deuda. */
function gruposPagos() {
  const g = new Map();
  pagosDeDeudas().forEach(([f, x]) => {
    if (x.c !== "deudas" && !x.d) return;
    const k = x.fx ? "fx:" + x.fx : "n:" + normPago(x.n);
    const e = g.get(k) || { k, nombre: x.n || "Pago de deudas", n: 0, ultimo: 0, fecha: "", ds: new Set(), fijo: !!x.fx };
    e.n++; if (f >= e.fecha) { e.fecha = f; e.ultimo = Number(x.m) || 0; } e.ds.add(x.d || ""); g.set(k, e);
  });
  return [...g.values()].sort((a, b) => b.fecha.localeCompare(a.fecha));
}
/** La deuda que más se parece al concepto (solo si hay una sola que coincide). */
function deudaSugerida(nombre, lista) {
  const comunes = new Set(["libranza", "credito", "tarjeta", "banco", "pago", "cuota", "prestamo", "de", "del", "la", "el", "tc", "sa", "s.a."]);
  const pal = t => normPago(t).split(/[^a-z0-9]+/).filter(w => w.length > 2 && !comunes.has(w));
  const n = new Set(pal(nombre)), c = lista.filter(x => pal(x.nombre).some(w => n.has(w)));
  return c.length === 1 ? c[0].id : "";
}
function pagosDeudasHoja(lista) {
  const gs = gruposPagos(); if (!gs.length) return "";
  const sin = gs.filter(e => e.ds.has("")).length;
  return hoja(`<h3 style="margin:0 0 4px">Tus pagos de deudas</h3>
    <p class="suave" style="margin:0 0 10px">Las cuotas que anotas en el libro (categoría «Pago de deudas»). Dile a cada una qué deuda paga: así su cuota entra al plan y los pagos de ahora en adelante bajan lo que debes.${sin ? ` <b>${sin} ${sin === 1 ? "sin identificar" : "sin identificar"}.</b>` : ""}</p>
    <div class="asc-tabla"><table class="pagos-deudas"><thead><tr><th>Concepto</th><th class="num">Pagos</th><th class="num">Último</th><th>¿Qué deuda paga?</th></tr></thead><tbody>
      ${gs.map(e => { const actual = e.ds.size === 1 ? [...e.ds][0] : "", sug = actual ? "" : deudaSugerida(e.nombre, lista);
        return `<tr class="${actual ? "" : "sin-deuda"}"><th scope="row">${esc(e.nombre)}${e.fijo ? ` <span class="pildora">Cada mes</span>` : ""}</th><td class="num">${e.n}</td><td class="num">${plata(e.ultimo)}</td>
          <td><select data-acc-change="deudaVincular" data-arg="${esc(e.k)}" aria-label="Deuda que paga ${esc(e.nombre)}"><option value="">${e.ds.size > 1 ? "Varias…" : "Sin identificar"}</option>${lista.map(x => `<option value="${x.id}" ${actual === x.id ? "selected" : ""}>${esc(x.nombre)}${sug === x.id ? " (sugerida)" : ""}</option>`).join("")}</select></td></tr>`; }).join("")}
    </tbody></table></div>`);
}
/** Asigna una deuda a todos los pagos de un grupo (y al fijo, para los meses que vienen). */
function vincularPagos(k, idDeuda) {
  const d = deudasDatos(), deuda = d.lista.find(x => x.id === idDeuda);
  if (deuda && !deuda.desde) { fijarSaldo(deuda); guardarDeudas(d); }      // lo que debe hoy queda como punto de partida
  let n = 0;
  if (k.startsWith("fx:")) {
    const fx = k.slice(3), l = fijosDatos(), fijo = l.find(x => x.id === fx);
    if (fijo) { if (idDeuda) fijo.d = idDeuda; else delete fijo.d; guardarFijos(l); }
  }
  diasConMovs(true).forEach(f => {
    const xs = movsGuardados(f); let cambio = false;
    xs.forEach(x => { const es = k.startsWith("fx:") ? x.fx === k.slice(3) : (x.c === "deudas" || x.d) && "n:" + normPago(x.n) === k;
      if (es) { if (idDeuda) x.d = idDeuda; else delete x.d; cambio = true; n++; } });
    if (cambio) guardarMovs(f, xs);
  });
  return n;
}

/* ---------- Acciones ---------- */
Object.assign(ACC, {
  deudaVincular(el, k) {
    const id = el.value, nombre = deudasDatos().lista.find(x => x.id === id)?.nombre;
    vincularPagos(k, id);
    const c = id ? cuotaVinculada(id) : 0;
    toast(id ? `Estos pagos van a ${nombre}${c ? `: su cuota al mes queda en ${plata(c)}` : ""}` : "Pagos sin deuda asignada");
    pintarDetalle();
  },
  deudaMetodo(_, m) { const d = deudasDatos(); d.metodo = m; guardarDeudas(d); },
  deudasCal(el) { setTimeout(() => { estado.deudasCal = el.closest("details")?.open; }, 0); },
  deudasTodo() { estado.deudasTodo = !estado.deudasTodo; estado.deudasCal = true; pintarDetalle(); },
  deudaMover(_, arg) { const [id, s] = arg.split("|"), d = deudasDatos(), k = d.lista.findIndex(y => y.id === id), j = k + Number(s);
    if (k < 0 || j < 0 || j >= d.lista.length) return; [d.lista[k], d.lista[j]] = [d.lista[j], d.lista[k]]; guardarDeudas(d); },
  deudaExtra(el) { const d = deudasDatos(); d.extra = Math.abs(montoFin(el.value)); guardarDeudas(d); },
  deudaCampo(el, arg) { const [id, k] = arg.split("|"), d = deudasDatos(), x = d.lista.find(y => y.id === id); if (!x) return;
    x[k] = k === "cuotas" || k === "pagadas" ? Math.round(Math.abs(montoFin(el.value))) : Math.abs(montoFin(el.value));
    if (k === "pagadas") { x.pagadasF = iso(hoyFecha()); x.pagadasT = nuevoId(); }      // las de hoy en adelante se suman solas con los pagos anotados
    guardarDeudas(d); },
  deudaQuitar(_, id) { const d = deudasDatos(), x = d.lista.find(y => y.id === id); if (!x) return; d.lista = d.lista.filter(y => y.id !== id); guardarDeudas(d);
    toast(`${x.nombre} quitada`, { accion: "Deshacer", alHacer: () => { const e = deudasDatos(); e.lista.push(x); guardarDeudas(e); } }); },
  /** Pagar una deuda con las opciones de los bancos: la cuota (o pago mínimo), el pago total, un abono
   *  extraordinario a capital que reduce el plazo o la cuota, u otro valor. */
  deudaPagar(_, id) {
    const x = deudasHoy().find(y => y.id === id); if (!x) return;
    const r = (Number(x.tasa) || 0) / 100 / 12, interes = Math.round(x.actual * r), c = Number(x.minimo) || 0;
    const nResto = (B, cuota) => !cuota ? null : r ? (cuota <= B * r ? null : Math.ceil(-Math.log(1 - B * r / cuota) / Math.log(1 + r))) : Math.ceil(B / cuota);
    const cuotaPara = (B, n) => !n ? null : r ? B * r / (1 - Math.pow(1 + r, -n)) : B / n;
    // Las cuotas que faltan según el saldo, la cuota y la tasa (así el antes y el después se comparan igual)
    const restan = nResto(x.actual, c) ?? (x.avance ? x.avance.faltan : null);
    const opciones = [
      ["cuota", c ? "Cuota del mes o pago mínimo" : "Pago del mes", c ? `${plata(c)}${r ? ` · unos ${plata(interes)} van a intereses` : ""}` : "Escribe el valor"],
      ["abono-plazo", "Abono a capital · reducir el plazo", "Pagas la misma cuota y terminas antes"],
      ["abono-cuota", "Abono a capital · reducir la cuota", "Terminas en la misma fecha con una cuota menor"],
      ["total", "Pago total", `Cancelas la deuda: ${plata(x.actual)}`],
    ];
    const v = ventanaCuenta(`<h3 style="margin-top:0">Pagar ${esc(x.nombre)}</h3><p class="suave" style="margin-top:0">Debes hoy ${plata(x.actual)}${c ? ` · cuota ${plata(c)}` : ""}${restan ? ` · con esta cuota te faltan unas ${restan} ${restan === 1 ? "cuota" : "cuotas"}` : ""}. El pago se anota en tu libro de cuentas de hoy.</p>
      <form class="form" id="pagoDeuda"><div class="pago-opciones" role="radiogroup" aria-label="Tipo de pago">${opciones.map(([k, t, d], i) => `<label class="pago-op"><input type="radio" name="tipo" value="${k}" ${i === 0 ? "checked" : ""}><span><b>${t}</b><small>${d}</small></span></label>`).join("")}</div>
      ${campo("Monto del pago", `<input name="monto" inputmode="decimal" value="${esc(numAsc(Math.min(x.actual, c || x.actual)))}" required>`)}
      <p class="pago-efecto suave"></p>
      ${r ? "" : `<p class="suave" style="font-size:12.5px">Escribe el interés anual de esta deuda para ver cuánto va a capital y cuánto a intereses.</p>`}
      <div class="botones"><button class="btn" type="button" data-cerrar>Cancelar</button><button class="btn lleno" type="submit">Pagar</button></div></form>`, v => {
      const form = v.querySelector("#pagoDeuda"), monto = form.querySelector('[name="monto"]'), efecto = v.querySelector(".pago-efecto");
      const tipo = () => form.querySelector('[name="tipo"]:checked').value;
      const calcular = () => {
        const t = tipo(), m = Math.min(x.actual + interes, Math.abs(montoFin(monto.value)));
        const abono = t.startsWith("abono") || t === "total", cap = Math.min(x.actual, abono ? m : Math.max(0, m - interes)), B = x.actual - cap;
        let txt = "", cambios = {};
        if (!m) txt = "";
        else if (B <= 0.5) txt = `<b>Con este pago la deuda queda en $0.</b>`;
        else if (t === "abono-plazo") { const n2 = nResto(B, c); txt = n2 != null && restan ? `Debes ${plata(B)}. Con la misma cuota terminas <b>${Math.max(0, restan - n2)} ${restan - n2 === 1 ? "cuota" : "cuotas"} antes</b>: te quedan ${n2}.` : `Debes ${plata(B)}. Todo el abono va a capital.`; cambios = { plazo: n2 }; }
        else if (t === "abono-cuota") { const c2 = cuotaPara(B, restan); txt = c2 ? `Debes ${plata(B)}. Tu cuota baja de ${plata(c)} a <b>${plata(Math.round(c2))}</b> y terminas en la misma fecha.` : `Debes ${plata(B)}. Escribe la cuota y el número de cuotas para calcular la nueva cuota.`; cambios = { cuota: c2 }; }
        else txt = `${r ? `De este pago, unos ${plata(Math.min(m, interes))} van a intereses y ${plata(cap)} a capital. ` : ""}Quedas debiendo ${plata(B)}.`;
        efecto.innerHTML = txt; return { t, m, cap, B, cambios };
      };
      form.querySelectorAll('[name="tipo"]').forEach(i => i.onchange = () => {
        const t = tipo(); if (t === "total") monto.value = numAsc(x.actual); else if (t === "cuota") monto.value = numAsc(Math.min(x.actual, c || x.actual)); else { monto.value = ""; monto.focus(); }
        calcular();
      });
      monto.oninput = calcular; calcular();
      v.querySelector("[data-cerrar]").onclick = () => v.remove();
      form.onsubmit = e => {
        e.preventDefault(); const { t, m, cap, B, cambios } = calcular(); if (!m) return;
        const f = iso(hoyFecha()), ab = t === "abono-plazo" ? "plazo" : t === "abono-cuota" ? "cuota" : t === "total" ? "total" : "";
        const nombre = (t.startsWith("abono") ? "Abono a capital " : "Pago ") + x.nombre;
        guardarMovs(f, [...movsGuardados(f), { id: "m" + nuevoId(), t: "gas", m, c: "deudas", n: nombre, d: x.id, cap: Math.round(cap), ...(ab ? { ab } : {}) }]);
        // Lo que cambia en el crédito: la cuota nueva o el número de cuotas
        const d = deudasDatos(), dx = d.lista.find(y => y.id === id);
        if (dx && t === "abono-cuota" && cambios.cuota) dx.minimo = Math.round(cambios.cuota);
        if (dx && t === "abono-plazo" && cambios.plazo != null && x.avance && restan) dx.cuotas = Math.max(x.avance.k, x.avance.n - (restan - cambios.plazo));
        if (dx && (t === "abono-cuota" || t === "abono-plazo")) guardarDeudas(d);
        v.remove(); toast(B <= 0.5 ? `¡${x.nombre} quedó pagada!` : t === "abono-cuota" && cambios.cuota ? `Abono anotado: tu cuota nueva es ${plata(Math.round(cambios.cuota))}` : t === "abono-plazo" && cambios.plazo != null ? `Abono anotado: te quedan ${cambios.plazo} cuotas` : `Pago de ${plata(m)} anotado`);
      };
    });
  },
  ascVisibilidad(_, v) { responder(ASC.WB, "fin#visibilidad", v === "publica" ? "publica" : ""); toast(v === "publica" ? "Podrás compartir tu avance, sin montos" : "Tus finanzas son privadas"); },
  ascOcultar(el) { responder(ASC.WB, "fin#ocultar", el.checked ? "1" : ""); },
  ascQuitarPin() { if (finBloqueado()) return; if (window.FINC?.activo()) { toast("Primero quita el cifrado de extremo a extremo"); return; } responder(ASC.WB, "fin#bloqueo", ""); responder(ASC.WB, "fin#pin", ""); toast("Bloqueo quitado"); },
  ascCierre(_, m) { responder(ASC.WB, "fin#cierre", String(m)); estado.ascAbierto = Date.now(); toast(`Se cerrará tras ${m} ${m === "1" ? "minuto" : "minutos"} sin uso`); },
  async ascPasskeyCrear() {
    try { await crearPasskey(); toast("Listo: abre tus finanzas con tu huella o rostro"); pintarDetalle(); }
    catch (e) { if (e?.name !== "NotAllowedError") toast("Este aparato no permitió crear la llave. Sigue usando tu PIN."); }
  },
  ascPasskeyQuitar() { try { localStorage.removeItem("suroute-fin-passkey"); } catch {} toast("Huella quitada en este aparato"); pintarDetalle(); },
  async ascPasskey() {
    try {
      await usarPasskey(); guardarIntentos({});
      let abierta = true; try { abierta = await (window.finAbrirConAparato?.() ?? true); } catch { abierta = false; }
      if (!abierta) { toast("Esta vez escribe tu PIN: este aparato aún no guarda tu llave"); return; }
      estado.ascAbierto = Date.now(); pintarDetalle();
    }
    catch (e) { if (e?.name !== "NotAllowedError") toast("No se pudo usar la huella. Escribe tu PIN."); }
  },
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
    if (finEspera()) { pintarDetalle(); return; }
    if (await hashPin(texto, sal) === h) {
      guardarIntentos({});
      let abierta = true; try { abierta = await (window.finAbrirConPin?.(texto) ?? true); } catch { abierta = false; }
      if (!abierta) { estado.finPinPendiente = texto; estado.finPideCodigo = true; pintarDetalle(); return; }
      estado.ascAbierto = Date.now(); pintarDetalle(); return;
    }
    const o = finIntentos(); o.n = (o.n || 0) + 1;
    if (o.n >= 5) { const seg = FIN_ESPERAS[Math.min(o.nivel || 0, FIN_ESPERAS.length - 1)]; o.hasta = Date.now() + seg * 1000; o.nivel = (o.nivel || 0) + 1; o.n = 0; setTimeout(() => pintarDetalle(), seg * 1000 + 100); }
    guardarIntentos(o); toast("PIN incorrecto"); pintarDetalle();
  },
  deudaNueva(form, datos, texto) {
    const saldo = Math.abs(montoFin(datos.get("saldo"))); if (!texto || !saldo) { toast("Escribe el nombre y lo que debes"); return; }
    const d = deudasDatos(); d.lista.push({ id: "d" + nuevoId(), nombre: texto.slice(0, 60), saldo, tasa: Math.abs(montoFin(datos.get("tasa"))), minimo: Math.abs(montoFin(datos.get("minimo"))), tipo: datos.get("tipo") === "buena" ? "buena" : "mala", desde: iso(hoyFecha()), desdeT: nuevoId() });
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

/* ---------- Análisis financiero completo: un informe para descargar (PDF desde «Imprimir») ----------
 * Junta todo: resumen, Índice de Salud Financiera e indicadores con su meta, estado de cuentas, en qué se va
 * el dinero, activos y pasivos, cada deuda (cuotas, capital e intereses), el plan para salir de deudas y
 * recomendaciones según los indicadores que están por mejorar. Se arma en una página aparte lista para guardar. */
const RECOMIENDA = {
  "Porcentaje de flujo neto": "Te queda poco libre de lo que entra. Revisa la mochila amarilla (gastos variables) y fija un tope por categoría.",
  "Índice de liquidez": "Sale más de lo que entra. Antes de ahorrar más, corta gastos variables hasta que los ingresos cubran todo.",
  "Tasa de ahorro": "Págate primero: separa al menos el 10 % el día que te pagan, antes de gastar.",
  "Ahorro + inversión ÷ ingresos": "Apunta a la regla 80/20: el 20 % de cada ingreso para tu futuro (ahorro e inversión).",
  "Deuda ÷ ingreso (DTI)": "Tus cuotas pesan mucho sobre tu ingreso. No tomes deudas nuevas y usa el plan de Salir de deudas para cerrar primero las más caras.",
  "Relación gasto ÷ ingreso": "Tus gastos se llevan casi todo el ingreso. Busca bajar los fijos que se puedan renegociar (planes, seguros, servicios).",
  "Fondo de emergencia": "Arma un fondo de emergencia de 3 a 6 meses de gastos fijos en una cuenta aparte (categoría «Emergencia» o un activo «Efectivo y cuentas»).",
  "Índice de endeudamiento": "Debes una parte grande de lo que tienes. Prioriza bajar deudas malas antes de comprar a crédito.",
  "Ingresos pasivos ÷ ingresos": "Empieza a construir ingresos que no dependan de tu tiempo: rentas, dividendos, un negocio.",
  "Índice de libertad financiera": "Cada ingreso pasivo nuevo te acerca a que tus rentas paguen tu vida.",
  "Pasivos buenos ÷ pasivos": "La mayoría de tus deudas solo cuestan. Ciérralas primero; las buenas son las que producen.",
  "Patrimonio neto ÷ ingresos anuales": "Tu patrimonio aún es bajo frente a lo que ganas: cada peso que pasa a ahorro, inversión o a bajar deudas lo hace crecer.",
};
function informeFinanciero() {
  const oculto = estado.ascOculto; estado.ascOculto = false;
  try {
    const r = saludDe(""), dd = deudasDatos(), lista = deudasHoy(), plan = planDeudas(lista, dd.metodo, dd.extra), act = activosDatos();
    const led = sumar(diasConMovs().flatMap(movsDe)).construido, hoyD = hoyFecha(), meses = r.mesesLista || [];
    const nomMes = ym => `${MESES[Number(ym.slice(5, 7)) - 1]} ${ym.slice(0, 4)}`;
    const periodo = meses.length ? `${mayus(nomMes(meses[0]))} – ${nomMes(meses.at(-1))} · ${meses.length} ${meses.length === 1 ? "mes" : "meses"}` : "Sin movimientos";
    const fecha = hoyD.toLocaleDateString("es", { day: "numeric", month: "long", year: "numeric" });
    const EST = { ok: ["Bien", "#1E7B4A", "#E3F2E9"], medio: ["Atención", "#8A5A00", "#FBEFD8"], bajo: ["Por mejorar", "#B3261E", "#FBE4E2"], na: ["—", "#5A666B", "#EEF1F2"] };
    const pill = e => `<span class="pill" style="color:${EST[e][1]};background:${EST[e][2]}">${EST[e][0]}</span>`;
    const flujo = r.filas.at(-1)[1], egresos = r.s.ing - flujo;
    const nivelColor = r.isf == null ? "#5A666B" : r.isf >= 80 ? "#1E7B4A" : r.isf >= 50 ? "#2E7D52" : r.isf >= 20 ? "#B7791F" : "#B3261E";
    const L = 2 * Math.PI * 52, anillo = `<svg viewBox="0 0 120 120" class="anillo"><circle cx="60" cy="60" r="52" fill="none" stroke="#EEF1F2" stroke-width="12"/>
      <circle cx="60" cy="60" r="52" fill="none" stroke="${nivelColor}" stroke-width="12" stroke-linecap="round" stroke-dasharray="${L * (r.isf || 0) / 100} ${L}" transform="rotate(-90 60 60)"/>
      <text x="60" y="62" text-anchor="middle" class="anillo-n">${r.isf ?? "—"}</text><text x="60" y="82" text-anchor="middle" class="anillo-t">ISF %</text></svg>`;
    const porNombre = Object.fromEntries(r.ind.map(x => [x.nombre, x]));
    const buenos = r.ind.filter(x => x.estado === "ok"), malos = r.ind.filter(x => x.estado === "bajo" || x.estado === "medio").sort((a, b) => (a.estado === "bajo" ? 0 : 1) - (b.estado === "bajo" ? 0 : 1));
    const vivas = lista.filter(x => x.actual > 0);
    const claves = [
      buenos.length ? `<b>Fortaleza:</b> ${buenos.slice(0, 2).map(x => `${x.nombre.toLowerCase()} (${x.valor})`).join(" y ")}.` : "",
      malos.length ? `<b>Prioridad:</b> ${malos[0].nombre.toLowerCase()} (${malos[0].valor}, meta ${malos[0].metaTxt || "—"}).` : "<b>Todo en meta:</b> sigue así y sube tu inversión.",
      vivas.length ? (plan.meses ? `<b>Deudas:</b> debes ${plata(r.pasivos)}; con tu plan quedas libre en ${mesesDespues(plan.meses)}.` : `<b>Deudas:</b> con los pagos actuales la deuda no baja.`) : "<b>Deudas:</b> no tienes deudas activas.",
    ].filter(Boolean);
    // Mes a mes: ingresos y egresos (barras) y lo que quedó libre
    const mm = meses.map(ym => { const t = totalesDe(ym + "-"); return { ym, ing: t.ing, egr: t.gas + t.aho + t.inv, disp: t.disp }; });
    const tope = Math.max(1, ...mm.map(x => Math.max(x.ing, x.egr))), W = 640, H = 120, bw = Math.min(22, (W - 40) / Math.max(1, mm.length) / 2.6);
    const barras = mm.length ? `<svg viewBox="0 0 ${W} ${H + 24}" class="graf">${[0, .5, 1].map(f => `<line x1="0" x2="${W}" y1="${10 + (1 - f) * H}" y2="${10 + (1 - f) * H}" stroke="#EEF1F2"/>`).join("")}
      ${mm.map((x, i) => { const cx = 20 + (i + .5) * (W - 40) / mm.length, h1 = x.ing / tope * H, h2 = x.egr / tope * H;
        return `<rect x="${cx - bw - 1}" y="${10 + H - h1}" width="${bw}" height="${h1}" rx="3" fill="#3E5868"/><rect x="${cx + 1}" y="${10 + H - h2}" width="${bw}" height="${h2}" rx="3" fill="#C9AE84"/>
          <text x="${cx}" y="${H + 24}" text-anchor="middle" class="eje">${MES_C[Number(x.ym.slice(5, 7)) - 1]}</text>`; }).join("")}</svg>
      <div class="leyenda"><span><i style="background:#3E5868"></i>Ingresos</span><span><i style="background:#C9AE84"></i>Egresos (gastos, ahorro e inversión)</span></div>` : "";
    const cats = Object.entries(r.s.cat).filter(([k]) => k.startsWith("gas:")).sort((a, b) => b[1] - a[1]), totGas = cats.reduce((a, [, v]) => a + v, 0) || 1;
    const metodo = (METODOS_DEUDA.find(m => m[0] === dd.metodo) || [])[1] || dd.metodo;
    const nombre = esc(perfil()?.nombre || "");
    const logo = new URL("iconos/logo-horizontal-crema.svg", location.href).href, simbolo = new URL("iconos/simbolo.svg", location.href).href;
    let sec = 0; const h2 = t => `<h2><span>${String(++sec).padStart(2, "0")}</span>${t}</h2>`;
    const tabla = (cab, filas, cls = "") => `<table class="${cls}"><thead><tr>${cab.map((c, i) => `<th${i ? ' class="n"' : ""}>${c}</th>`).join("")}</tr></thead><tbody>${filas.join("")}</tbody></table>`;
    const tr = (cels, cls = "") => `<tr class="${cls}">${cels.map((c, i) => i ? `<td class="n">${c}</td>` : `<th>${c}</th>`).join("")}</tr>`;
    const css = `@import url("https://fonts.googleapis.com/css2?family=Roboto+Serif:opsz,wght@8..144,500;8..144,600&family=Roboto:wght@400;500;700&display=swap");
      *{box-sizing:border-box}body{font:13.5px/1.55 Roboto,system-ui,sans-serif;color:#1D2427;margin:0;background:#E6EAEC;-webkit-print-color-adjust:exact;print-color-adjust:exact}
      .doc{max-width:900px;margin:24px auto;background:#fff;border-radius:14px;overflow:hidden;box-shadow:0 10px 40px -12px rgb(0 0 0/.25)}
      .banda{background:#6B2A1A;color:#F3EDE0;padding:22px 44px;display:flex;align-items:center;justify-content:space-between}
      .banda img{height:46px}.banda .meta{text-align:right;font-size:12px;letter-spacing:.06em;text-transform:uppercase;opacity:.85}
      .portada{padding:30px 44px 8px;display:grid;grid-template-columns:1fr auto;gap:8px 24px;align-items:end;border-bottom:1px solid #DCE2E4}
      h1{font:600 30px/1.15 "Roboto Serif",Georgia,serif;margin:0;color:#1D2427}.sub{color:#5A666B;margin:6px 0 18px}
      .cuerpo{padding:8px 44px 36px}
      h2{display:flex;align-items:baseline;gap:12px;font:600 19px "Roboto Serif",Georgia,serif;color:#6B2A1A;margin:34px 0 12px;break-after:avoid}
      h2 span{font:500 12px Roboto,sans-serif;color:#86683A;letter-spacing:.1em}
      .resumen{display:grid;grid-template-columns:180px 1fr;gap:24px;align-items:center;margin-top:22px}
      .anillo{width:170px;height:170px}.anillo-n{font:600 34px "Roboto Serif",Georgia,serif;fill:#1D2427}.anillo-t{font:500 11px Roboto,sans-serif;fill:#5A666B;letter-spacing:.08em}
      .nivel{display:inline-block;padding:4px 12px;border-radius:999px;color:#fff;font-weight:700;font-size:13px}
      .kpis{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin-top:14px}.kpi{border-radius:10px;background:#F5F7F8;padding:10px 12px}
      .kpi span{display:block;font-size:11.5px;color:#5A666B}.kpi b{font-size:17px;font-variant-numeric:tabular-nums}
      .claves{margin:18px 0 0;padding:14px 18px;border-left:3px solid #86683A;background:#FBF8F2;border-radius:0 10px 10px 0}.claves p{margin:4px 0}
      .grupo{margin:14px 0 6px;font:600 12px Roboto,sans-serif;letter-spacing:.08em;text-transform:uppercase;color:#5A666B}
      table{width:100%;border-collapse:collapse;font-variant-numeric:tabular-nums}th,td{padding:8px 10px;border-bottom:1px solid #EEF1F2;text-align:left;vertical-align:top}
      thead th{font:500 11.5px Roboto,sans-serif;color:#5A666B;text-transform:uppercase;letter-spacing:.05em;border-bottom:1px solid #DCE2E4}
      .n{text-align:right;white-space:nowrap}table.ind{table-layout:fixed}table.ind th:nth-child(2),table.ind td:nth-child(2){width:96px}table.ind th:nth-child(3),table.ind td:nth-child(3){width:96px}table.ind th:nth-child(4),table.ind td:nth-child(4){width:120px}tr.total th,tr.total td{font-weight:700;background:#F5F7F8}
      td.ayuda,small{color:#5A666B;font-size:12px;font-weight:400}.pill{display:inline-block;padding:2px 10px;border-radius:999px;font-size:12px;font-weight:700;white-space:nowrap}
      .neg{color:#B3261E}.acciones-lista{counter-reset:a;display:grid;gap:8px;padding:0;margin:0;list-style:none}
      .acciones-lista li{counter-increment:a;display:grid;grid-template-columns:28px 1fr;gap:10px;padding:10px 12px;border:1px solid #EEF1F2;border-radius:10px;break-inside:avoid}
      .acciones-lista li::before{content:counter(a);width:28px;height:28px;border-radius:50%;background:#6B2A1A;color:#fff;display:grid;place-items:center;font-weight:700;font-size:13px}
      .graf{width:100%;height:auto}.eje{font:11px Roboto,sans-serif;fill:#5A666B}.leyenda{display:flex;gap:18px;font-size:12px;color:#5A666B;margin:4px 0 10px}.leyenda i{display:inline-block;width:10px;height:10px;border-radius:2px;margin-right:6px}
      .barra{height:8px;border-radius:4px;background:#EEF1F2;overflow:hidden;min-width:120px}.barra i{display:block;height:100%;border-radius:4px;background:#3E5868}
      .dos{display:grid;grid-template-columns:1fr 1fr;gap:20px}
      .deuda{border:1px solid #DCE2E4;border-radius:12px;padding:14px 16px;margin:10px 0;break-inside:avoid}.deuda header{display:flex;justify-content:space-between;gap:12px;align-items:baseline}
      .deuda header b{font-size:15px}.deuda .mini{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin:10px 0}.deuda .mini div{background:#F5F7F8;border-radius:8px;padding:8px 10px}
      .deuda .mini span{display:block;font-size:11px;color:#5A666B}.deuda .mini b{font-size:14px}.deuda p{margin:6px 0 0;font-size:12.5px}
      .nota{margin-top:30px;padding-top:12px;border-top:1px solid #DCE2E4;font-size:11.5px;color:#5A666B;display:flex;gap:10px;align-items:center}.nota img{width:20px;height:20px;opacity:.7}
      .barra-acc{position:sticky;top:0;z-index:2;display:flex;gap:8px;justify-content:center;padding:12px;background:#E6EAEC}
      .barra-acc button{font:500 14px Roboto,sans-serif;padding:10px 20px;border-radius:999px;border:0;background:#6B2A1A;color:#fff;cursor:pointer}.barra-acc button.sec{background:#fff;color:#6B2A1A}
      @media (max-width:700px){.banda,.portada,.cuerpo{padding-left:18px;padding-right:18px}.resumen{grid-template-columns:1fr}.kpis{grid-template-columns:1fr 1fr}.dos{grid-template-columns:1fr}.deuda .mini{grid-template-columns:1fr 1fr}}
      @media print{body{background:#fff}.doc{margin:0;max-width:none;border-radius:0;box-shadow:none}.barra-acc{display:none}@page{size:A4;margin:12mm 0 14mm}
        .banda{padding:16px 14mm}.portada,.cuerpo{padding-left:14mm;padding-right:14mm}h2{break-after:avoid}table,.claves{break-inside:auto}tr{break-inside:avoid}
        .resumen{grid-template-columns:150px 1fr}.anillo{width:140px;height:140px}.kpis{grid-template-columns:1fr 1fr}.kpi b{font-size:15px}.deuda .mini b{font-size:13px}}`;
    const grupos = GRUPOS_SALUD.map(([t, , nombres]) => { const xs = nombres.map(n => porNombre[n]).filter(Boolean); return !xs.length ? "" :
      `<div class="grupo">${t}</div>` + tabla(["Indicador", "Valor", "Meta", "Estado"], xs.map(x => tr([`${x.nombre}<br><small>${x.ayuda}</small>`, x.valor, x.metaTxt || "—", pill(x.estado)])), "ind"); }).join("");
    const html = `<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
      <title>Análisis financiero · SuRoute · ${iso(hoyD)}</title><style>${css}</style></head><body>
      <div class="barra-acc"><button onclick="print()">Guardar como PDF o imprimir</button><button class="sec" onclick="close()">Cerrar</button></div>
      <div class="doc">
      <div class="banda"><img src="${logo}" alt="SuRoute · Summit Route"><div class="meta">Análisis financiero<br>${fecha}</div></div>
      <div class="portada"><div><h1>Análisis financiero completo</h1><div class="sub">${nombre ? `<b>${nombre}</b> · ` : ""}${periodo}</div></div></div>
      <div class="cuerpo">
        <div class="resumen">${anillo}<div>${r.nivel ? `<span class="nivel" style="background:${nivelColor}">${r.nivel[0]}</span><p style="margin:8px 0 0">${r.nivel[1]}</p>` : `<p class="sub">Anota tus ingresos para calcular el índice.</p>`}
          <div class="kpis"><div class="kpi"><span>Ingresos</span><b>${plata(r.s.ing)}</b></div><div class="kpi"><span>Egresos</span><b>${plata(egresos)}</b></div>
            <div class="kpi"><span>Flujo neto</span><b class="${flujo < 0 ? "neg" : ""}">${plata(flujo)}</b></div><div class="kpi"><span>Patrimonio neto</span><b class="${r.neto < 0 ? "neg" : ""}">${plata(r.neto)}</b></div></div></div></div>
        <div class="claves">${claves.map(c => `<p>${c}</p>`).join("")}</div>
        ${h2("Salud financiera")}${grupos}
        ${h2("Qué hacer ahora")}${malos.length ? `<ol class="acciones-lista">${malos.map(x => `<li><div><b>${x.nombre}</b> · ${x.valor} ${pill(x.estado)}<br>${RECOMIENDA[x.nombre] || x.ayuda}</div></li>`).join("")}</ol>` : `<p>Todos tus indicadores están en su meta. Sigue igual y sube tu inversión.</p>`}
        ${h2("Estado de cuentas")}${tabla(["Concepto", `Promedio mensual`, `Total (${r.nMeses} ${r.nMeses === 1 ? "mes" : "meses"})`], [...r.filas.map(([n, v, f]) => tr([n, plata(Math.round(v / (r.nMeses || 1))), plata(v)], f ? "total" : "")), tr(["Cuotas de deuda al mes", plata(Math.round(r.deudaMes)), ""])])}
        ${mm.length > 1 ? h2("Mes a mes") + barras + tabla(["Mes", "Ingresos", "Egresos", "Libre"], mm.map(x => tr([mayus(nomMes(x.ym)), plata(x.ing), plata(x.egr), `<span class="${x.disp < 0 ? "neg" : ""}">${plata(x.disp)}</span>`]))) : ""}
        ${cats.length ? h2("En qué se va tu dinero") + tabla(["Categoría", "Total", "% del gasto", ""], cats.slice(0, 12).map(([k, v]) => { const [t, c] = k.split(":"), p = v / totGas * 100;
          return tr([esc(catDe(t, c)?.[1] || c), plata(v), Math.round(p) + " %", `<div class="barra"><i style="width:${Math.max(2, p)}%"></i></div>`]); })) : ""}
        ${h2("Activos y pasivos")}<div class="dos"><div>${tabla(["Lo que tienes", "Valor hoy"], [...act.map(x => tr([`${esc(x.nombre)}<br><small>${(TIPOS_ACTIVO.find(t => t[0] === x.tipo) || [, ""])[1]}</small>`, plata(Number(x.valor) || 0)])), tr(["Ahorro e inversión del libro<br><small>Se suma solo</small>", plata(led)]), tr(["Activos totales", plata(r.activos)], "total")])}</div>
          <div>${tabla(["Lo que debes", "Saldo hoy"], [...(lista.length ? lista.map(x => tr([`${esc(x.nombre)}<br><small>${x.tipo === "buena" ? "Buena · produce" : "Mala · solo cuesta"}</small>`, plata(x.actual)])) : [tr(["Sin deudas", ""])]), tr(["Pasivos totales", plata(r.pasivos)], "total"), tr(["Patrimonio neto", `<span class="${r.neto < 0 ? "neg" : ""}">${plata(r.neto)}</span>`], "total")])}</div></div>
        ${(() => { const ms = window.metasFinHoy ? metasFinHoy() : []; if (!ms.length) return "";
          const EST = { lograda: "¡Lograda!", atiempo: "Vas a tiempo", atrasada: "Atrasada", vencida: "Se pasó la fecha", sinfecha: "Sin fecha" };
          return h2("Tus metas financieras") + tabla(["Meta", "Llevas", "Meta", "Para cuándo", "Aporta al mes", "Estado"], ms.map(x => tr([`${esc(x.nombre)}<br><small>${tipoMeta(x.tipo)[1]}</small><div class="barra" style="margin-top:6px"><i style="width:${Math.max(2, x.pct * 100)}%;background:#1E7B4A"></i></div>`,
            `${plata(x.ahorrado)}<br><small>${Math.round(x.pct * 100)} %</small>`, plata(x.objetivo), x.fecha ? nombreYm(x.fecha) : "—", x.aporteMes ? plata(Math.ceil(x.aporteMes)) : "—", pill(x.estado === "lograda" || x.estado === "atiempo" ? "ok" : x.estado === "atrasada" ? "medio" : x.estado === "vencida" ? "bajo" : "na").replace(/>[^<]*</, `>${EST[x.estado]}<`)]))); })()}
        ${lista.length ? h2("Tus deudas") + `<p>Método <b>${metodo}</b>${dd.extra ? ` con ${plata(dd.extra)} extra al mes` : ""}. ${vivas.length ? (plan.meses ? `Quedas libre en <b>${mesesDespues(plan.meses)}</b> y pagas cerca de <b>${plata(Math.round(plan.interes))}</b> en intereses.` : "Con estos pagos la deuda no baja: sube el pago o renegocia la tasa.") : "¡Sin deudas!"}</p>` +
          lista.map(x => { const a = x.avance, fin = plan.fin[x.id];
            return `<div class="deuda"><header><b>${esc(x.nombre)}</b><small>${x.tipo === "buena" ? "Buena · produce" : "Mala · solo cuesta"} · interés ${Number(x.tasa) || 0} % anual</small></header>
              <div class="mini"><div><span>Debes hoy</span><b>${plata(x.actual)}</b></div><div><span>Cuota</span><b>${plata(x.minimo || 0)}</b></div>
                <div><span>Cuotas</span><b>${a ? `${a.k} de ${a.n}` : "—"}</b></div><div><span>${x.actual > 0 ? "Libre en" : "Estado"}</span><b>${x.actual > 0 ? (fin ? mesesDespues(fin) : "Sin fecha") : "Pagada"}</b></div></div>
              ${a ? `<div class="barra"><i style="width:${a.k / a.n * 100}%;background:#1E7B4A"></i></div>${x.minimo ? `<p>Has pagado <b>${plata(Math.round(a.pagadoTotal))}</b>: ${plata(Math.round(a.capital))} a capital y ${plata(Math.round(a.intereses))} a intereses${a.faltan ? `. Faltan ${a.faltan} cuotas (terminas en ${a.fin})` : ""}.${a.estimado ? ` <small>Prestado estimado: ${plata(Math.round(a.prestado))}.</small>` : ""}</p>` : ""}` : ""}</div>`; }).join("") : ""}
        <div class="nota"><img src="${simbolo}" alt="">Generado por SuRoute con los datos de tu libro de cuentas · método Ruta a la Cima. Este análisis es una guía para decidir; no reemplaza la asesoría de un profesional financiero.</div>
      </div></div></body></html>`;
    return html;
  } finally { estado.ascOculto = oculto; }
}
Object.assign(ACC, {
  ascInforme() {
    const html = informeFinanciero(), w = window.open("", "_blank");
    if (!w) {   // si el navegador bloquea la ventana, se descarga el archivo
      const a = Object.assign(document.createElement("a"), { href: URL.createObjectURL(new Blob([html], { type: "text/html" })), download: `analisis-financiero-${iso(hoyFecha())}.html` });
      a.click(); toast("Análisis descargado: ábrelo y elige «Guardar como PDF»"); return;
    }
    w.document.open(); w.document.write(html); w.document.close();
    toast("Análisis listo: usa «Guardar como PDF» para descargarlo");
  },
});
