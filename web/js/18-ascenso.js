"use strict";
/* ======================================================================
 * Ascenso financiero (igual que en la app, domain/model/Ascenso.kt).
 * Un libro de cuentas que sube y baja: balance general → año → mes → día, y del día al balance.
 * Cada día guarda sus movimientos en la respuesta "fin#mov#<aaaa-mm-dd>" (workbook "finanzas") como JSON:
 *   [{ id, t: "ing"|"gas"|"aho"|"inv", m: monto, c: categoría, n: concepto }]
 * Los gastos se reparten en las tres mochilas del libro Cima financiera: verde (lo esencial, ~50 %),
 * amarilla (gastos variables, ~30 %) y roja (ahorro e inversión en tu futuro, ≥ 20 %).
 * Los montos del control financiero de antes (agenda.ingresos…) se suman como movimientos del día.
 * ====================================================================== */

const ASC = {
  WB: "finanzas",
  TIPOS: [["ing", "Ingresos", "Ingreso"], ["gas", "Gastos", "Gasto"], ["aho", "Ahorro", "Ahorro"], ["inv", "Inversión", "Inversión"]],
  /** Categorías por tipo: [código, nombre, mochila] (la mochila solo aplica a gastos; ahorro e inversión son la roja). */
  CAT: {
    ing: [["trabajo", "Trabajo o salario"], ["negocio", "Negocio"], ["ventas", "Ventas"], ["honorarios", "Honorarios"], ["pasivo", "Ingreso pasivo"], ["otro", "Otro ingreso"]],
    gas: [["alimentacion", "Alimentación", "verde"], ["vivienda", "Vivienda", "verde"], ["servicios", "Servicios básicos", "verde"], ["transporte", "Transporte", "verde"],
      ["salud", "Salud", "verde"], ["deudas", "Pago de deudas", "verde"], ["entretenimiento", "Entretenimiento", "amarilla"], ["restaurantes", "Restaurantes", "amarilla"],
      ["ropa", "Ropa y accesorios", "amarilla"], ["suscripciones", "Suscripciones", "amarilla"], ["hormiga", "Gastos hormiga", "amarilla"], ["otro", "Otro gasto", "amarilla"]],
    aho: [["emergencia", "Fondo de emergencia"], ["meta", "Ahorro para una meta"], ["retiro", "Retiro"], ["otro", "Otro ahorro"]],
    inv: [["educacion", "Educación"], ["herramientas", "Herramientas de trabajo"], ["fondos", "Fondos y acciones"], ["negocio", "Mi negocio"], ["inmuebles", "Bienes raíces"], ["otro", "Otra inversión"]],
  },
  MOCHILAS: [["verde", "Mochila verde", "Lo esencial", 0.5], ["amarilla", "Mochila amarilla", "Gastos variables", 0.3], ["roja", "Mochila roja", "Ahorro e inversión", 0.2]],
  /** Moneda por país y, si no se sabe el país, por idioma (los 12 de la app). */
  POR_PAIS: { CO: "COP", MX: "MXN", AR: "ARS", CL: "CLP", PE: "PEN", EC: "USD", VE: "VES", BO: "BOB", PY: "PYG", UY: "UYU", GT: "GTQ", CR: "CRC", DO: "DOP", PA: "USD", SV: "USD", HN: "HNL",
    NI: "NIO", CU: "CUP", PR: "USD", ES: "EUR", US: "USD", CA: "CAD", GB: "GBP", AU: "AUD", BR: "BRL", PT: "EUR", FR: "EUR", BE: "EUR", CH: "CHF", DE: "EUR", AT: "EUR", IT: "EUR",
    CN: "CNY", TW: "TWD", HK: "HKD", JP: "JPY", KR: "KRW", SA: "SAR", AE: "AED", EG: "EGP", MA: "MAD", IN: "INR", RU: "RUB" },
  POR_IDIOMA: { es: "COP", en: "USD", pt: "BRL", fr: "EUR", de: "EUR", it: "EUR", zh: "CNY", ja: "JPY", ko: "KRW", ar: "SAR", hi: "INR", ru: "RUB" },
  MONEDAS: ["COP", "USD", "EUR", "MXN", "ARS", "CLP", "PEN", "BOB", "PYG", "UYU", "VES", "GTQ", "CRC", "DOP", "HNL", "NIO", "BRL", "CAD", "GBP", "CHF", "AUD", "CNY", "JPY", "KRW", "INR", "RUB", "SAR", "AED", "EGP", "MAD"],
};
const claveMov = f => `fin#mov#${f}`;
/** El idioma y país del navegador, limpio ("es-CO"); si no se puede leer, español de Colombia. */
const ascLocale = (() => { try { return Intl.getCanonicalLocales(String(navigator.language || "").split(/[@_.]/)[0].replace("_", "-"))[0] || "es-CO"; } catch { return "es-CO"; } })();

/** Moneda elegida o la de tu país e idioma. */
function ascMoneda() {
  const m = respuesta("fin#moneda"); if (m) return m;
  const [idioma, pais] = ascLocale.split("-");
  return ASC.POR_PAIS[(pais || "").toUpperCase()] || ASC.POR_IDIOMA[idioma] || "USD";
}
/** "$ 1.250.000", "US$ 12,50", "1.234,50 €"… en el idioma del navegador; sin decimales si el monto es entero. */
function plata(v, { signo = false } = {}) {
  if (estado.ascOculto) return "•••••";               // montos ocultos (botón del ojo), por ejemplo en un lugar público
  const entero = Math.abs(v % 1) < 0.005;
  let s;
  try { s = new Intl.NumberFormat(ascLocale, { style: "currency", currency: ascMoneda(), minimumFractionDigits: entero ? 0 : 2, maximumFractionDigits: entero ? 0 : 2 }).format(v); }
  catch { s = fmtFin(v); }
  return signo && v > 0 ? "+" + s : s;
}
/** El número sin símbolo, con los separadores de tu idioma (para editar en la tabla). */
const numAsc = v => estado.ascOculto ? "•••••" : new Intl.NumberFormat(ascLocale, { maximumFractionDigits: 2 }).format(v);
/* Con los montos ocultos (el ojo), los campos de cifras se ven con puntos y no se pueden cambiar hasta mostrarlos */
document.addEventListener("focusin", e => {
  const i = e.target; if (!estado.ascOculto || !i.matches?.("input") || !String(i.value).includes("•")) return;
  i.readOnly = true; toast("Toca el ojo para mostrar los montos y poder cambiarlos");
});
document.addEventListener("change", e => { if (estado.ascOculto && String(e.target.value || "").includes("•")) e.stopImmediatePropagation(); }, true);
const nombreMoneda = c => { try { return new Intl.DisplayNames([ascLocale], { type: "currency" }).of(c); } catch { return c; } };

/** Movimientos guardados de un día. */
function movsGuardados(f) { try { const v = JSON.parse(respuesta(claveMov(f)) || "[]"); return Array.isArray(v) ? v.filter(x => x && ASC.CAT[x.t] && Number.isFinite(Number(x.m))) : []; } catch { return []; } }
/** Los del día con los montos del control financiero de antes (se muestran como movimientos y se pueden editar). */
function movsDe(f) {
  const ag = Store.get("agenda", f) || {}, viejos = [["ingresos", "ing", "otro"], ["gastos", "gas", "otro"], ["ahorro", "aho", "otro"], ["inversion", "inv", "otro"]]
    .map(([k, t, c]) => ({ id: "ag-" + k, t, c, m: Math.abs(montoFin(ag[k])), n: "Registro del día", ag: k })).filter(x => x.m);
  const g = movsGuardados(f), fijos = (window.fijosEn?.(f) || []).filter(v => !g.some(y => y.fx === v.fx));   // los fijos del mes que no se cambiaron a mano
  const futuro = f > iso(hoyFecha());
  const todos = [...viejos, ...fijos, ...g.map(y => futuro && y.fx ? { ...y, prog: true } : y)];   // un fijo cambiado en un mes que no llega: programado
  return ordenDia(f, todos);
}
/** El orden que eligió la persona para las filas del día (se arrastran con el asa). Los fijos se ubican
 *  por su fijo, así un fijo cambiado ese mes conserva su lugar; lo nuevo va al final. */
const claveOrden = f => `fin#orden#${f}`;
const llaveOrden = x => x.fx ? "fx:" + x.fx : x.id;
function ordenDia(f, xs) {
  let o; try { o = JSON.parse(respuesta(claveOrden(f)) || "[]"); } catch { o = []; }
  if (!Array.isArray(o) || !o.length) return xs;
  const pos = new Map(o.map((k, i) => [k, i]));
  return xs.map((x, i) => [x, pos.has(llaveOrden(x)) ? pos.get(llaveOrden(x)) : 1e6 + i]).sort((a, b) => a[1] - b[1]).map(a => a[0]);
}
function guardarOrdenDia(f, ids) {
  const xs = movsDe(f), por = new Map(xs.map(x => [x.id, x]));
  responder(ASC.WB, claveOrden(f), JSON.stringify(ids.map(id => por.get(id)).filter(Boolean).map(llaveOrden)));
}
function guardarMovs(f, xs) {
  const l = xs.filter(x => !x.ag && !x.virtual).map(({ id, t, m, c, n, r, d, fx, cap, ab, meta }) => ({ id, t, m: Math.round(Number(m) * 100) / 100, c, n: String(n || "").slice(0, 120), ...(r?.length ? { r } : {}), ...(d ? { d } : {}), ...(fx ? { fx } : {}),
    ...(cap != null ? { cap: Math.round(Number(cap) * 100) / 100 } : {}), ...(ab ? { ab } : {}), ...(meta ? { meta } : {}) }));   // meta: la meta financiera a la que va; cap: lo que bajó la deuda; ab: abono a capital
  return responder(ASC.WB, claveMov(f), l.length ? JSON.stringify(l) : "");
}
const catDe = (t, c) => (window.categorias ? categorias(t) : ASC.CAT[t] || []).find(x => x[0] === c) || ASC.CAT[t]?.at(-1);
const mochilaDe = x => x.t === "gas" ? (catDe("gas", x.c)?.[2] || "amarilla") : x.t === "aho" || x.t === "inv" ? "roja" : null;

/** Suma de movimientos: totales por tipo, por mochila y por categoría. */
function sumar(movs) {
  const s = { ing: 0, gas: 0, aho: 0, inv: 0, n: 0, mochila: { verde: 0, amarilla: 0, roja: 0 }, cat: {} };
  movs.forEach(x => {
    if (x.prog) return;                                  // programado: cuenta cuando llegue su día
    const m = Number(x.m) || 0; s[x.t] += m; s.n++;
    const mo = mochilaDe(x); if (mo) s.mochila[mo] += m;
    const k = x.t + ":" + x.c; s.cat[k] = (s.cat[k] || 0) + m;
  });
  s.disp = s.ing - s.gas - s.aho - s.inv;          // lo que queda libre
  s.construido = s.aho + s.inv;                    // lo que suma a tu patrimonio
  s.tasa = s.ing > 0 ? Math.min(1, Math.max(0, s.construido / s.ing)) : null;
  return s;
}
/** Todos los días con movimientos (de la respuesta o del control de antes). */
function diasConMovs(programados = false) {
  const set = new Set();
  Store.lista("respuesta").forEach(r => { if (r.clave.startsWith("fin#mov#") && r.valor) set.add(r.clave.slice(8)); });
  Store.lista("agenda").forEach(a => { if (a.fecha && ["ingresos", "gastos", "ahorro", "inversion"].some(k => montoFin(a[k]))) set.add(a.fecha); });
  (window.fechasFijos?.(programados) || []).forEach(f => set.add(f));
  return [...set].filter(f => /^\d{4}-\d{2}-\d{2}$/.test(f)).sort();
}
const totalesDe = pref => sumar(diasConMovs().filter(f => f.startsWith(pref)).flatMap(movsDe));

/* ---------- Piezas visuales ---------- */
const COL = [["ing", "Ingresos"], ["gas", "Gastos"], ["aho", "Ahorro"], ["inv", "Inversión"], ["disp", "Disponible"]];
const celda = (v, k) => `<td class="num${k === "disp" && v < 0 ? " neg" : ""}${!v ? " cero" : ""}">${v ? plata(v) : "—"}</td>`;
/** Tabla tipo hoja de cálculo: una fila por periodo (toca para bajar), acumulado que sube y fila de totales. */
function tablaPeriodos(filas, etqCol, total) {
  let acum = 0;
  return `<div class="asc-tabla"><table><thead><tr><th>${etqCol}</th>${COL.map(([, t]) => `<th class="num">${t}</th>`).join("")}<th class="num">Acumulado</th></tr></thead><tbody>
    ${filas.map(({ etq, sel, s, actual }) => { acum += s.disp;
      return `<tr class="${actual ? "actual" : ""}${s.n ? "" : " vacia"}" data-sel="${sel}" tabindex="0" role="link" aria-label="Abrir ${esc(etq)}"><th scope="row">${esc(etq)}</th>${COL.map(([k]) => celda(s[k], k)).join("")}<td class="num acum${acum < 0 ? " neg" : ""}">${s.n || acum ? plata(acum) : "—"}</td></tr>`; }).join("")}
    </tbody><tfoot><tr><th scope="row">Total</th>${COL.map(([k]) => celda(total[k], k)).join("")}<td class="num"></td></tr></tfoot></table></div>`;
}
/** Las cuatro cifras de un periodo y lo disponible. */
function cifras(s) {
  return `<div class="asc-cifras">${COL.map(([k, t]) => `<div class="asc-cifra c-${k}"><span>${t}</span><b class="${k === "disp" && s.disp < 0 ? "neg" : ""}">${plata(s[k])}</b></div>`).join("")}</div>`;
}
/** Las tres mochilas: cuánto pesa cada una frente a lo que entró, y la referencia 50/30/20. */
function mochilas(s) {
  const base = s.ing || (s.mochila.verde + s.mochila.amarilla + s.mochila.roja) || 1;
  return hoja(`<h3 style="margin:0 0 4px">Tus tres mochilas</h3><p class="suave" style="margin:0 0 12px">Del libro Cima financiera: lo esencial cerca del 50 %, lo variable cerca del 30 % y al menos el 20 % para tu futuro.</p>
    <div class="moch-barra" role="img" aria-label="${ASC.MOCHILAS.map(([k, n]) => `${n}: ${Math.round(s.mochila[k] / base * 100)} %`).join(", ")}">${ASC.MOCHILAS.map(([k]) => `<i class="m-${k}" style="flex:${s.mochila[k] || 0}"></i>`).join("")}${s.ing > s.gas + s.construido ? `<i class="m-libre" style="flex:${s.ing - s.gas - s.construido}"></i>` : ""}</div>
    <div class="moch-filas">${ASC.MOCHILAS.map(([k, n, d, ideal]) => { const p = s.mochila[k] / base, bien = k === "roja" ? p >= ideal : p <= ideal + 0.05;
      return `<div class="moch"><i class="m-${k}"></i><span><b>${n}</b><small>${d} · ideal ${k === "roja" ? "≥ " : "≈ "}${Math.round(ideal * 100)} %</small></span><span class="num">${plata(s.mochila[k])}</span><span class="num ${s.ing ? (bien ? "ok" : "alerta") : ""}">${s.ing ? Math.round(p * 100) + " %" : "—"}</span></div>`; }).join("")}</div>`);
}
/** En qué se fue el dinero: categorías de mayor a menor, con barras a escala. */
function porCategoria(s) {
  const l = Object.entries(s.cat).filter(([k]) => !k.startsWith("ing:")).sort((a, b) => b[1] - a[1]).slice(0, 8);
  if (!l.length) return hoja(`<div class="cab-lista-btn"><h3 style="margin:0">¿A dónde fue tu dinero?</h3><button class="btn mini" data-acc="ascCrearCat" data-arg="gas">${ic("mas")} Crear categoría</button></div><p class="suave" style="margin:8px 0 0">Cuando anotes gastos verás aquí en qué se van. Crea tus propias categorías: Mascotas, Gimnasio, Regalos…</p>`);
  const max = l[0][1];
  return hoja(`<div class="cab-lista-btn" style="margin-bottom:10px"><h3 style="margin:0">¿A dónde fue tu dinero?</h3><button class="btn mini" data-acc="ascCrearCat" data-arg="gas">${ic("mas")} Crear categoría</button></div><div class="asc-cats">${l.map(([k, v]) => { const [t, c] = k.split(":"), cat = catDe(t, c);
    return `<div class="asc-cat"><span>${esc(cat?.[1] || c)}</span><span class="barra-c"><i class="b-${t}" style="width:${(v / max * 100).toFixed(1)}%"></i></span><span class="num">${plata(v)}</span></div>`; }).join("")}</div>`);
}
/** Barras de ingresos y gastos por periodo (a escala, con su valor al pasar). */
function grafica(filas) {
  const max = Math.max(1, ...filas.flatMap(x => [x.s.ing, x.s.gas + x.s.aho + x.s.inv]));
  if (!filas.some(x => x.s.n)) return "";
  const W = 100 / filas.length;
  return `<div class="asc-graf" role="img" aria-label="Ingresos y salidas por periodo">${filas.map(x => `<button class="asc-col" data-sel="${x.sel}" style="width:${W}%" title="${esc(x.etq)}: entró ${plata(x.s.ing)}, salió ${plata(x.s.gas + x.s.aho + x.s.inv)}">
      <span class="pila"><i class="b-ing" style="height:${(x.s.ing / max * 100).toFixed(1)}%"></i><i class="b-gas" style="height:${((x.s.gas + x.s.aho + x.s.inv) / max * 100).toFixed(1)}%"></i></span><small>${esc(x.corto)}</small></button>`).join("")}</div>
    <p class="asc-ley"><span><i class="b-ing"></i>Entró</span><span><i class="b-gas"></i>Salió (gastos, ahorro e inversión)</span></p>`;
}
/** Migas de pan: siempre se puede subir al balance o bajar al día. */
function migasAsc(partes) {
  return `<nav class="migas" aria-label="Dónde estás">${partes.map(([t, sel], i) => i < partes.length - 1 ? `<button data-sel="${sel}">${esc(t)}</button> › ` : `<span>${esc(t)}</span>`).join("")}</nav>`;
}
const navPeriodo = (antes, despues, etq) => `<div class="asc-nav"><button class="rb" data-sel="${antes}" aria-label="Anterior">${ic("izq")}</button><b>${esc(etq)}</b><button class="rb" data-sel="${despues}" aria-label="Siguiente">${ic("der")}</button></div>`;
const avAsc = () => av(ic("finanzas"), "var(--ok)");
const ymDe = (a, m) => `${a}-${String(m).padStart(2, "0")}`;

/* ---------- Pantallas ---------- */
/* ---------- Deshacer y rehacer en Ascenso financiero ----------
 * Cada cambio del libro (movimientos, fijos, deudas, activos, categorías…) guarda cómo estaba antes.
 * Los cambios hechos casi a la vez (un mismo clic) son un solo paso. Se recuerdan los últimos 60 en
 * este navegador, así se puede volver atrás aunque el aviso de «Deshacer» ya se haya ido. */
const HIST = { atras: [], adelante: [], aplicando: false, ultimo: 0 };
try { const h = JSON.parse(localStorage.getItem("suroute-fin-hist") || "{}"); HIST.atras = h.atras || []; HIST.adelante = h.adelante || []; } catch {}
const guardarHist = () => { try { localStorage.setItem("suroute-fin-hist", JSON.stringify({ atras: HIST.atras.slice(-60), adelante: HIST.adelante.slice(-60) })); } catch {} };
{
  const base = responder;
  responder = function (wb, clave, valor) {
    if (wb === ASC.WB && String(clave).startsWith("fin#") && !HIST.aplicando && clave !== "fin#pin" && clave !== "fin#bloqueo") {
      const antes = respuesta(clave) || "", despues = valor ? String(valor) : "";
      if (antes !== despues) {
        const ahora = Date.now(), paso = HIST.atras.at(-1);
        if (paso && ahora - HIST.ultimo < 600) { if (!paso.c.some(x => x.k === clave)) paso.c.push({ k: clave, a: antes }); }
        else HIST.atras.push({ t: ahora, c: [{ k: clave, a: antes }] });
        HIST.ultimo = ahora; HIST.adelante = []; if (HIST.atras.length > 60) HIST.atras.shift(); guardarHist();
      }
    }
    return base.apply(this, arguments);
  };
}
const queCambio = paso => { const k = paso.c.map(x => x.k);
  return k.some(x => x === "fin#deudas") ? "deudas" : k.some(x => x === "fin#activos") ? "activos" : k.some(x => x === "fin#fijos") ? "fijos" : k.some(x => x.startsWith("fin#mov#")) ? "movimientos" : "cambio"; };
function moverHist(desde, hacia, nombre) {
  const paso = desde.pop(); if (!paso) { toast(nombre === "deshacer" ? "No hay nada para deshacer" : "No hay nada para rehacer"); return; }
  HIST.aplicando = true;
  const inverso = { t: Date.now(), c: paso.c.map(x => ({ k: x.k, a: respuesta(x.k) || "" })) };
  try { [...paso.c].reverse().forEach(x => responder(ASC.WB, x.k, x.a)); } finally { HIST.aplicando = false; }
  hacia.push(inverso); guardarHist();
  toast(`${nombre === "deshacer" ? "Deshecho" : "Rehecho"}: ${queCambio(paso)}`); pintarDetalle(); pintarLista();
}
Object.assign(ACC, { ascDeshacer() { moverHist(HIST.atras, HIST.adelante, "deshacer"); }, ascRehacer() { moverHist(HIST.adelante, HIST.atras, "rehacer"); } });
document.addEventListener("keydown", e => {
  if (estado.sec !== "finanzas" || !(e.ctrlKey || e.metaKey) || e.key.toLowerCase() !== "z" || e.target.closest?.("input, textarea, select, [contenteditable]")) return;
  e.preventDefault(); e.shiftKey ? ACC.ascRehacer() : ACC.ascDeshacer();
});
const histAsc = () => `<button class="rb" data-acc="ascDeshacer" ${HIST.atras.length ? "" : "disabled"} aria-label="Deshacer el último cambio" title="Deshacer el último cambio (Ctrl+Z)">${ic("deshacer")}</button><button class="rb" data-acc="ascRehacer" ${HIST.adelante.length ? "" : "disabled"} aria-label="Rehacer" title="Rehacer (Ctrl+Shift+Z)">${ic("rehacer")}</button>`;
const ojoAsc = () => histAsc() + `<button class="rb" data-acc="ascInforme" aria-label="Descargar el análisis financiero completo" title="Descargar el análisis financiero completo">${ic("descargar")}</button><button class="rb" data-acc="ascOjo" aria-label="${estado.ascOculto ? "Mostrar montos" : "Ocultar montos"}" title="${estado.ascOculto ? "Mostrar montos" : "Ocultar montos"}">${ic(estado.ascOculto ? "ojoNo" : "ojo")}</button>`;
DET["finanzas.b"] = () => {
  if (window.finBloqueado?.()) return pantallaBloqueo();
  const dias = diasConMovs(), hoyA = new Date().getFullYear();
  const anios = [...new Set([...dias.map(f => Number(f.slice(0, 4))), hoyA])].sort();
  const filas = anios.map(a => ({ etq: String(a), corto: String(a), sel: "a:" + a, s: totalesDe(a + "-"), actual: a === hoyA }));
  const t = sumar(dias.flatMap(movsDe));
  return cab(avAsc(), "Ascenso financiero", "Balance general · todos tus años", ojoAsc()) + cuerpo(
    migasAsc([["Balance general", "b"]]) +
    (t.n ? "" : `<div class="aviso">${ic("finanzas")} Empieza por hoy: anota lo que entró y salió. Cada día suma al mes, cada mes al año y cada año a este balance.<button class="btn lleno" data-sel="d:${iso(hoyFecha())}" style="margin-left:auto">Registrar hoy</button></div>`) +
    cifras(t) + (window.graficaAcceso?.() || "") +
    hoja(`<div class="cab-lista-btn"><h3 style="margin:0">Año por año</h3><span class="suave" style="font-size:13px">Toca un año para ver sus meses</span></div>${grafica(filas)}${tablaPeriodos(filas, "Año", t)}
      <p class="asc-pie">Patrimonio construido: <b>${plata(t.construido)}</b> en ahorro e inversión${t.tasa != null ? ` · ${Math.round(t.tasa * 100)} % de todo lo que entró` : ""}.</p>`) +
    saludResumen("") + deudasResumen() + fijosHoja("") + mochilas(t) + porCategoria(t) + privacidadHoja() + monedaHoja(), "max-width:980px");
};
DET["finanzas.a"] = arg => {
  if (window.finBloqueado?.()) return pantallaBloqueo();
  const a = Number(arg) || new Date().getFullYear(), hoy = hoyFecha();
  const filas = [...Array(12).keys()].map(i => ({ etq: mayus(MESES[i]), corto: MES_C[i], sel: "m:" + ymDe(a, i + 1), s: totalesDe(ymDe(a, i + 1) + "-"), actual: a === hoy.getFullYear() && i === hoy.getMonth() }));
  const t = totalesDe(a + "-");
  return cab(avAsc(), String(a), "Ascenso financiero · el año mes a mes", ojoAsc()) + cuerpo(
    migasAsc([["Balance general", "b"], [String(a), ""]]) + navPeriodo("a:" + (a - 1), "a:" + (a + 1), String(a)) + cifras(t) +
    hoja(`<div class="cab-lista-btn"><h3 style="margin:0">Mes a mes</h3><span class="suave" style="font-size:13px">Toca un mes para ver sus días</span></div>${grafica(filas)}${tablaPeriodos(filas, "Mes", t)}`) +
    saludResumen(a + "-") + mochilas(t) + porCategoria(t), "max-width:980px");
};
DET["finanzas.m"] = arg => {
  if (window.finBloqueado?.()) return pantallaBloqueo();
  const [a, m] = arg.split("-").map(Number), ym = ymDe(a, m), hoyI = iso(hoyFecha());
  const dias = new Set(diasConMovs(true).filter(f => f.startsWith(ym)));
  if (hoyI.startsWith(ym)) dias.add(hoyI);
  const filas = [...dias].sort().map(f => { const d = deIso(f); return { etq: `${d.getDate()} · ${d.toLocaleDateString("es", { weekday: "short" })}`, corto: String(d.getDate()), sel: "d:" + f, s: sumar(movsDe(f)), actual: f === hoyI }; });
  const t = totalesDe(ym + "-"), pres = Math.abs(montoFin(respuesta(claveFinPresupuesto(ym)))), uso = pres ? t.gas / pres : null;
  const ant = m === 1 ? ymDe(a - 1, 12) : ymDe(a, m - 1), sig = m === 12 ? ymDe(a + 1, 1) : ymDe(a, m + 1);
  return cab(avAsc(), `${mayus(MESES[m - 1])} ${a}`, "Ascenso financiero · el mes día a día", ojoAsc()) + cuerpo(
    migasAsc([["Balance general", "b"], [String(a), "a:" + a], [mayus(MESES[m - 1]), ""]]) + navPeriodo("m:" + ant, "m:" + sig, `${mayus(MESES[m - 1])} ${a}`) + cifras(t) +
    hoja(`<h3 style="margin:0 0 8px">Presupuesto de gastos</h3>${campo("Lo máximo que quieres gastar este mes", `<input type="text" inputmode="decimal" data-resp="${FIN_WB}|${claveFinPresupuesto(ym)}" value="${esc(respuesta(claveFinPresupuesto(ym)))}" placeholder="Ej. ${plata(1500000)}">`)}
      ${uso != null ? `<div class="fin-pres"><div class="barra"><i style="width:${Math.min(100, uso * 100)}%;${uso > 1 ? "background:var(--peligro, #B3261E)" : ""}"></i></div><span class="suave">Gastaste ${plata(t.gas)} de ${plata(pres)}${uso > 1 ? " · te pasaste " + plata(t.gas - pres) : " · te quedan " + plata(pres - t.gas)}</span></div>` : ""}`) +
    hoja(`<div class="cab-lista-btn"><h3 style="margin:0">Día a día</h3><span class="botones-mini">${movsPropiosMes(ym).length ? `<button class="btn mini" data-acc="ascDuplicarMes" data-arg="${ym}" title="Copia los movimientos de ${MESES[m - 1]} al mismo día de ${MESES[m % 12]}; los fijos ya se repiten solos">${ic("copiar")} Duplicar al mes siguiente</button>` : ""}<button class="btn mini lleno" data-sel="d:${hoyI.startsWith(ym) ? hoyI : ym + "-01"}">${ic("mas")} Registrar</button></span></div>
      ${filas.length ? grafica(filas) + tablaPeriodos(filas, "Día", t) : `<p class="suave">Aún no hay movimientos en ${MESES[m - 1]}.</p>`}`) +
    fijosHoja(ym) + mochilas(t) + porCategoria(t), "max-width:980px");
};
DET["finanzas.d"] = f => {
  if (window.finBloqueado?.()) return pantallaBloqueo();
  const d = deIso(f), a = d.getFullYear(), m = d.getMonth() + 1, ym = ymDe(a, m), movs = movsDe(f), s = sumar(movs), mes = totalesDe(ym + "-");
  const tipo = estado.ascTipo || "gas", deudasLista = window.deudasDatos ? deudasDatos().lista : [], metasLista = window.metasFinDatos ? metasFinDatos() : [];
  const opc = t => categorias(t).map(([c, n]) => `<option value="${c}" ${c === estado.ascCatNueva ? "selected" : ""}>${esc(n)}</option>`).join("");
  return cab(avAsc(), `${d.getDate()} de ${MESES[m - 1]}`, mayus(d.toLocaleDateString("es", { weekday: "long" })) + ` · ${a}`, ojoAsc()) + cuerpo(
    migasAsc([["Balance general", "b"], [String(a), "a:" + a], [mayus(MESES[m - 1]), "m:" + ym], [String(d.getDate()), ""]]) +
    navPeriodo("d:" + sumarDias(f, -1), "d:" + sumarDias(f, 1), `${d.getDate()} ${MES_C[m - 1]} ${a}`) +
    hoja(`<form class="asc-nuevo" data-form="ascMov" data-arg="${f}">
        <div class="seg" role="radiogroup" aria-label="Tipo de movimiento">${ASC.TIPOS.map(([k, , u]) => `<button type="button" role="radio" aria-checked="${tipo === k}" class="seg-b t-${k}" data-acc="ascTipo" data-arg="${k}">${u}</button>`).join("")}</div>
        <div class="asc-fila-nueva">
          <label class="campo"><span>Monto</span><input name="monto" inputmode="decimal" autocomplete="off" placeholder="${plata(0)}" required aria-label="Monto"></label>
          <label class="campo"><span>Categoría <button type="button" class="enlace" data-acc="ascCrearCat" data-arg="${tipo}">+ Nueva</button></span><select name="cat">${opc(tipo)}</select></label>
          <label class="campo"><span>Concepto</span><input name="texto" maxlength="120" autocomplete="off" placeholder="${{ ing: "Pago de la quincena", gas: "Mercado de la semana", aho: "Aporte al fondo", inv: "Curso de finanzas" }[tipo]}"></label>
          <button class="btn lleno" type="submit">Agregar</button>
        </div>
        <label class="asc-repite"><input type="checkbox" name="repite"> ${ic("repetir")} Se repite cada mes el día ${d.getDate()} <span class="suave">(arriendo, salario, internet…)</span></label></form>
      <div class="asc-tabla"><table class="asc-movs"><thead><tr><th class="asa-col"><span class="oculto">Ordenar</span></th><th>Tipo</th><th>Categoría</th><th>Concepto</th><th class="num">Monto</th><th class="cada-mes" title="Se repite cada mes">${ic("repetir")}<span class="oculto">Se repite cada mes</span></th><th><span class="oculto">Recibos y quitar</span></th></tr></thead><tbody>
        ${movs.length ? movs.map(x => `<tr class="t-${x.t}${x.prog ? " prog" : ""}" data-mov="${esc(x.id)}"><td class="asa-col">${movs.length > 1 ? `<button type="button" class="asc-asa" data-mover-mov="${esc(x.id)}" data-dia="${f}" aria-label="Arrastrar para cambiar el orden (o usa las flechas)" title="Arrastrar para cambiar el orden">${ic("arrastrar")}</button>` : ""}</td><td><span class="pildora t-${x.t}">${ASC.TIPOS.find(y => y[0] === x.t)[2]}</span></td>
          <td><select data-acc-change="ascCat" data-arg="${f}|${x.id}" aria-label="Categoría">${categorias(x.t).map(([c, n]) => `<option value="${c}" ${c === x.c ? "selected" : ""}>${esc(n)}</option>`).join("")}</select></td>
          <td><input value="${esc(x.n || "")}" data-acc-change="ascNota" data-arg="${f}|${x.id}" maxlength="120" placeholder="Concepto" aria-label="Concepto">${x.t === "gas" && (x.c === "deudas" || x.d) && !x.ag && deudasLista.length ? `<select class="paga-deuda${x.d ? "" : " sin"}" data-acc-change="ascDeudaDe" data-arg="${f}|${x.id}" aria-label="Deuda que paga"><option value="">¿Qué deuda paga?</option>${deudasLista.map(y => `<option value="${y.id}" ${x.d === y.id ? "selected" : ""}>Paga: ${esc(y.nombre)}</option>`).join("")}</select>` : ""}${(x.t === "aho" || x.t === "inv") && !x.ag && metasLista.length ? `<select class="paga-deuda${x.meta ? "" : " sin"}" data-acc-change="ascMetaDe" data-arg="${f}|${x.id}" aria-label="Meta financiera"><option value="">¿Para qué meta?</option>${metasLista.map(y => `<option value="${y.id}" ${x.meta === y.id ? "selected" : ""}>Meta: ${esc(y.nombre)}</option>`).join("")}</select>` : ""}</td>
          <td class="num"> <input class="num" inputmode="decimal" value="${esc(numAsc(x.m))}" data-acc-change="ascMonto" data-arg="${f}|${x.id}" aria-label="Monto"></td>
          <td class="cada-mes">${x.ag ? "" : `<input type="checkbox" ${x.fx ? "checked" : ""} data-acc-change="ascRepite" data-arg="${f}|${x.id}" aria-label="Se repite cada mes el día ${d.getDate()}${x.prog ? " · programado: cuenta el día que llegue" : ""}" title="${x.prog ? "Programado: cuenta el día que llegue" : `Se repite cada mes el día ${d.getDate()}`}">`}</td>
          <td class="acc-mov">${x.ag ? "" : `<button class="rb${x.r?.length ? " con" : ""}" data-acc="ascRecibos" data-arg="${f}|${x.id}" aria-label="Recibos (${x.r?.length || 0})" title="Recibos">${ic("adjunto")}${x.r?.length ? `<small>${x.r.length}</small>` : ""}</button>`}${`<button class="rb" data-acc="ascDuplicar" data-arg="${f}|${x.id}" aria-label="Duplicar al mes siguiente" title="Duplicar al mes siguiente">${ic("copiar")}</button>`}<button class="rb" data-acc="ascBorrar" data-arg="${f}|${x.id}" aria-label="Quitar este movimiento">${ic("borrar")}</button></td></tr>`).join("")
          : `<tr><td colspan="7" class="suave" style="padding:14px">Aún no hay movimientos este día. Anota el primero arriba: el monto y su categoría bastan.</td></tr>`}
      </tbody>${movs.length ? `<tfoot><tr><th colspan="4" scope="row">Disponible del día</th><td class="num ${s.disp < 0 ? "neg" : ""}">${plata(s.disp)}</td><td></td><td></td></tr></tfoot>` : ""}</table></div>`) +
    cifras(s) +
    `<button class="hoja asc-sube" data-sel="m:${ym}"><span>Así va ${MESES[m - 1]}</span><b class="${mes.disp < 0 ? "neg" : ""}">${plata(mes.disp)} disponible</b><small>Entró ${plata(mes.ing)} · salió ${plata(mes.gas + mes.aho + mes.inv)}</small>${ic("der")}</button>`, "max-width:980px");
};
/** La moneda en la que llevas tus cuentas. */
function monedaHoja() {
  const actual = ascMoneda();
  return hoja(`<h3 style="margin:0 0 4px">Moneda</h3><p class="suave" style="margin:0 0 10px">Se elige según tu país e idioma. Cámbiala si llevas tus cuentas en otra.</p>
    <select data-acc-change="ascMonedaElegir" aria-label="Moneda">${[...new Set([actual, ...ASC.MONEDAS])].map(c => `<option value="${c}" ${c === actual ? "selected" : ""}>${c} · ${esc(nombreMoneda(c))}</option>`).join("")}</select>`);
}

/* ---------- Lista ---------- */
LISTAS.finanzas = () => {
  if (window.finBloqueado?.()) return cabLista("Ascenso financiero", { buscar: false }) + `<div class="items">${item("b", av(ic("candado"), "var(--ok)"), "Privado", "", "Abre el libro con tu PIN")}</div>`;
  const hoyI = iso(hoyFecha()), ym = hoyI.slice(0, 7), a = new Date().getFullYear(), hoyS = sumar(movsDe(hoyI)), mesS = totalesDe(ym + "-");
  const anios = [...new Set([...diasConMovs().map(f => Number(f.slice(0, 4))), a])].sort((x, y) => y - x);
  let h = cabLista("Ascenso financiero", { buscar: false }) + `<div class="items">`;
  h += item("d:" + hoyI, av(hoyFecha().getDate(), "var(--ok)"), "Hoy", "", hoyS.n ? `${hoyS.n} ${hoyS.n === 1 ? "movimiento" : "movimientos"} · ${plata(hoyS.disp)}` : "Anota lo que entró y salió");
  h += item("m:" + ym, av(MES_C[hoyFecha().getMonth()], "var(--ok)"), mayus(MESES[hoyFecha().getMonth()]), "", `${plata(mesS.disp)} disponible`);
  h += item("b", av(ic("plata"), "var(--ok)"), "Balance general", "", `Todos tus años · ${plata(sumar(diasConMovs().flatMap(movsDe)).construido)} construidos`);
  const sf = window.saludDe ? saludDe("") : null;
  h += item("grafica", av(ic("grafica"), "var(--ok)"), "Gráfica del ascenso", "", "Velas, barras y líneas por días, meses o años");
  h += item("salud", av(ic("salud"), "var(--ok)"), "Salud financiera", "", sf?.isf != null ? `ISF ${sf.isf} % · ${sf.nivel[0]}` : "Tus indicadores, mes a mes");
  const dd = window.deudasHoy ? deudasHoy() : [], debe = dd.reduce((a, x) => a + x.actual, 0);
  h += item("deudas", av(ic("deuda"), "var(--ok)"), "Salir de deudas", "", dd.length ? (debe > 0 ? `Debes ${plata(debe)} · ${dd.filter(x => x.actual > 0).length} por pagar` : "¡Sin deudas!") : "Tu plan con avalancha o bola de nieve");
  h += item("patrimonio", av(ic("balanza"), "var(--ok)"), "Activos y pasivos", "", "Tu patrimonio neto");
  if (window.metasFinResumen) { const mr = metasFinResumen(); h += item("metasfin", av(ic("alcancia"), "var(--ok)"), "Metas financieras", "", mr.n ? `${mr.n} ${mr.n === 1 ? "meta" : "metas"} · ${Math.round(mr.pct * 100)} % del total` : "Lo que quieres lograr con tu dinero"); }
  h += grupo("Años");
  anios.forEach(y => { const s = totalesDe(y + "-"); h += item("a:" + y, av(String(y).slice(2), "var(--ok)"), String(y), "", s.n ? `Entró ${plata(s.ing)} · ${plata(s.disp)} libre` : "Sin movimientos"); });
  h += grupo("Aprende");
  h += `<button class="item" data-acc="aprendeFinanzas">${av(ic("aprende"), "var(--burdeos)")}<span class="txt"><span class="t1"><span>Inteligencia financiera</span></span><span class="t2"><span>7 guías: de la base al ascenso</span></span></span></button>`;
  return h + `</div>`;
};

/* ---------- Acciones ---------- */
function cambiarMov(arg, cambio) {
  const [f, id] = arg.split("|"), xs = movsDe(f), x = xs.find(y => y.id === id); if (!x) return;
  if (x.ag) { Store.cambiar("agenda", f, { [x.ag]: "" }, BASES.agenda(f)); x.id = "m" + nuevoId(); delete x.ag; }   // pasa al libro de cuentas
  if (x.virtual) { x.id = "m" + nuevoId(); delete x.virtual; }                                                  // un fijo cambiado solo este mes
  cambio(x); guardarMovs(f, xs.filter(y => (!y.ag && !y.virtual) || y === x));
  return x;
}
window.cambiarMov = cambiarMov;
/** Para deshacer una duplicación: lo guardado del libro (días y fijos) antes de copiar. */
const respuestas2 = () => { const r = { [FIJOS]: respuesta(FIJOS) }; diasConMovs().forEach(f => { r[claveMov(f)] = respuesta(claveMov(f)); }); return r; };
function restaurar2(antes) {
  diasConMovs().forEach(f => { if (!(claveMov(f) in antes) && respuesta(claveMov(f))) responder(ASC.WB, claveMov(f), ""); });
  Object.entries(antes).forEach(([k, v]) => { if (respuesta(k) !== (v || "")) responder(ASC.WB, k, v || ""); });
  pintarDetalle();
}
/** El mismo día del mes siguiente (o su último día, si ese mes es más corto). */
function diaMesSiguiente(f) {
  const a = Number(f.slice(0, 4)), m = Number(f.slice(5, 7)), d = Number(f.slice(8)), a2 = m === 12 ? a + 1 : a, m2 = m === 12 ? 1 : m + 1;
  return `${ymDe(a2, m2)}-${String(Math.min(d, new Date(a2, m2, 0).getDate())).padStart(2, "0")}`;
}
/** Todos los movimientos del mes, en orden: los anotados, los fijos (también los programados) y los del registro de antes. */
function movsPropiosMes(ym) {
  const n = new Date(Number(ym.slice(0, 4)), Number(ym.slice(5, 7)), 0).getDate(), r = [];
  for (let d = 1; d <= n; d++) { const f = `${ym}-${String(d).padStart(2, "0")}`; movsDe(f).forEach(x => r.push([f, x])); }
  return r;
}
const copiaMov = x => ({ id: "m" + nuevoId(), t: x.t, m: x.m, c: x.c, n: x.n || "" });
/** Copia un movimiento al mismo día del mes siguiente. Un fijo no se duplica: su fila del mes siguiente
 *  queda igual a la de este mes (monto, categoría y concepto). Devuelve [fecha, "nuevo" | "igual"]. */
function duplicarUno(f, x) {
  const fijo = x.fx && fijosDatos().find(y => y.id === x.fx), ym2 = ymMas(f.slice(0, 7), 1);
  if (fijo && ym2 >= fijo.desde && (!fijo.hasta || ym2 <= fijo.hasta)) {
    const g = fechaFijo(fijo, ym2), ya = movsDe(g).find(y => y.fx === x.fx);
    if (ya && mismoMov(ya, x)) return [g, "igual"];
    if ((fijo.saltar || []).includes(ym2)) saltarFijo(x.fx, ym2, false);       // si ese mes se había quitado, vuelve
    const iguales = Number(x.m) === Number(fijo.m) && x.c === fijo.c && (x.n || "") === (fijo.n || "");
    const otros = movsGuardados(g).filter(y => y.fx !== x.fx);
    guardarMovs(g, iguales ? otros : [...otros, { ...copiaMov(x), fx: x.fx }]);   // igual al fijo: basta con el fijo
    return [g, "nuevo"];
  }
  const g = diaMesSiguiente(f), ya = movsGuardados(g);
  if (ya.some(y => mismoMov(x, y))) return [g, "igual"];
  guardarMovs(g, [...ya, copiaMov(x)]);
  return [g, "nuevo"];
}
const mismoMov = (a, b) => a.t === b.t && Number(a.m) === Number(b.m) && a.c === b.c && (a.n || "") === (b.n || "");
Object.assign(ACC, {
  ascOjo() { estado.ascOculto = !estado.ascOculto; pintarDetalle(); pintarLista(); },
  aprendeFinanzas() { ir("aprende"); estado.filtro = "finanzas"; pintarLista(); },
  ascTipo(_, t) { estado.ascTipo = t; pintarDetalle(); document.querySelector('.asc-nuevo [name="monto"]')?.focus(); },
  ascCat(el, arg) { cambiarMov(arg, x => { x.c = el.value; }); },
  /** Marca qué deuda paga este movimiento (si es un fijo, también en los demás meses). */
  ascDeudaDe(el, arg) {
    const [f, id] = arg.split("|"), x = movsDe(f).find(y => y.id === id); if (!x) return;
    if (x.fx) vincularPagos("fx:" + x.fx, el.value); else { const dd = deudasDatos(), deuda = dd.lista.find(y => y.id === el.value); if (deuda && !deuda.desde) { fijarSaldo(deuda); guardarDeudas(dd); } cambiarMov(arg, y => { if (el.value) y.d = el.value; else delete y.d; }); }
    toast(el.value ? `Pago de ${deudasDatos().lista.find(y => y.id === el.value)?.nombre}${x.fx ? " (cada mes)" : ""}` : "Sin deuda asignada"); pintarDetalle();
  },
  ascNota(el, arg) { cambiarMov(arg, x => { x.n = el.value.trim(); }); },
  ascRepite(el, arg) {
    const [f, id] = arg.split("|"), x = movsDe(f).find(y => y.id === id), dia = Number(f.slice(8)); if (!x) return;
    if (x.ag) { el.checked = false; return; }
    if (el.checked) { repetirMov(f, x, true); toast(`Se repite cada mes el día ${dia}: aparece en los demás meses y puedes cambiarlo en cualquiera`); }
    else { repetirMov(f, x, false); toast("Ya no se repite desde el próximo mes; este mes queda anotado"); }
    pintarDetalle();
  },
  ascMonto(el, arg) { const v = Math.abs(montoFin(el.value)); if (!v) { toast("Escribe un monto mayor que cero"); pintarDetalle(); return; } cambiarMov(arg, x => { x.m = v; }); },
  ascBorrar(_, arg) {
    const [f, id] = arg.split("|"), xs = movsDe(f), x = xs.find(y => y.id === id); if (!x) return;
    if (x.ag) Store.cambiar("agenda", f, { [x.ag]: "" }, BASES.agenda(f));
    else if (!x.virtual) guardarMovs(f, xs.filter(y => y.id !== id));
    if (x.fx) saltarFijo(x.fx, f.slice(0, 7));          // un fijo quitado: este mes no se cobra
    toast(x.fx ? "Quitado solo este mes; sigue fijo los demás" : "Movimiento quitado", { accion: "Deshacer", alHacer: () => {
      if (x.fx) saltarFijo(x.fx, f.slice(0, 7), false);
      if (x.ag) Store.cambiar("agenda", f, { [x.ag]: String(x.m) }, BASES.agenda(f)); else if (!x.virtual) guardarMovs(f, [...movsGuardados(f), x]); } });
  },
  ascDuplicar(_, arg) {
    const [f, id] = arg.split("|"), x = movsDe(f).find(y => y.id === id); if (!x) return;
    const antes = respuestas2(), [g, r] = duplicarUno(f, x), dg = deIso(g);
    toast(r === "igual" ? `Ya estaba igual el ${dg.getDate()} de ${MESES[dg.getMonth()]}` : `Copiado al ${dg.getDate()} de ${MESES[dg.getMonth()]}`,
      r === "igual" ? { accion: "Ver", alHacer: () => elegir("d:" + g) } : { accion: "Deshacer", alHacer: () => restaurar2(antes) });
    pintarDetalle();
  },
  ascDuplicarMes(_, ym) {
    const antes = respuestas2(); let nuevos = 0, iguales = 0;
    movsPropiosMes(ym).forEach(([f, x]) => { const [, r] = duplicarUno(f, x); if (r === "nuevo") nuevos++; else iguales++; });
    const m2 = MESES[Number(ym.slice(5, 7)) % 12];
    if (!nuevos) { toast(`Todo ya estaba igual en ${m2}`); return; }
    toast(`Copiados a ${m2}: ${nuevos} ${nuevos === 1 ? "movimiento" : "movimientos"}${iguales ? ` (${iguales} ya estaban)` : ""}`, { accion: "Deshacer", alHacer: () => restaurar2(antes) });
    pintarDetalle();
  },
  ascMonedaElegir(el) { responder(ASC.WB, "fin#moneda", el.value); toast(`Tus cuentas quedan en ${nombreMoneda(el.value)}`); },
});
window.FORMULARIOS = Object.assign(window.FORMULARIOS || {}, {
  ascMov(form, datos, texto) {
    const f = form.dataset.arg, m = Math.abs(montoFin(datos.get("monto")));
    if (!m) { toast("Escribe el monto"); form.querySelector('[name="monto"]').focus(); return; }
    const t = estado.ascTipo || "gas";
    if (datos.get("repite")) {                          // fijo: aparece solo cada mes en este día
      const l = fijosDatos(); l.push({ id: "f" + nuevoId(), t, m, c: datos.get("cat") || "otro", n: texto, dia: Number(f.slice(8)), desde: f.slice(0, 7), hasta: "", saltar: [] });
      guardarFijos(l); form.reset(); toast(`Quedó fijo: ${plata(m)} cada mes el día ${Number(f.slice(8))}`);
    } else { guardarMovs(f, [...movsGuardados(f), { id: "m" + nuevoId(), t, m, c: datos.get("cat") || "otro", n: texto }]);
      form.reset(); toast(`${ASC.TIPOS.find(y => y[0] === t)[2]} de ${plata(m)} anotado`); }
    setTimeout(() => document.querySelector('.asc-nuevo [name="monto"]')?.focus(), 50);
  },
});

/* La tabla se abre con Enter igual que con un clic (como una hoja de cálculo) */
document.addEventListener("keydown", e => { if (e.key === "Enter" && e.target.matches?.(".asc-tabla tr[data-sel]")) { e.preventDefault(); e.target.click(); } });

/** Para el día (Hoy) y el balance del mes: un resumen que lleva al módulo. */
window.ascResumenDia = function (f) {
  const movs = movsDe(f), s = sumar(movs), plata = window.plataFuera || globalThis.plata;
  return `<section class="hoja sec-dia" id="sd-finanzas" data-sd="finanzas">` + cabSec("finanzas", "Control financiero", "Lo que entró y salió hoy.", `<button class="btn mini" data-ir="finanzas" data-arg="d:${f}">${movs.length ? "Ver y anotar" : "Anotar"}</button>`) + (`
    ${movs.length ? `<div class="asc-mini">${movs.slice(0, 5).map(x => `<span class="pildora t-${x.t}">${ASC.TIPOS.find(y => y[0] === x.t)[2]}</span><span>${esc(x.n || catDe(x.t, x.c)?.[1] || "")}</span><span class="num">${plata(x.m)}</span>`).join("")}</div>
      <p class="fin-libre">Disponible hoy <b class="${s.disp < 0 ? "neg" : ""}">${plata(s.disp)}</b></p>` : `<p class="suave" style="margin:0">Anota lo que entró y salió hoy. En Ascenso financiero se suma al mes, al año y a tu balance general.</p>`}`) + `</section>`;
};
window.ascResumenMes = function (a, m) {
  const ym = ymDe(a, m), t = totalesDe(ym + "-");
  if (window.ocultarFuera?.()) return hoja(`<div class="cab-lista-btn"><h3 style="margin:0">Finanzas del mes</h3><button class="btn mini" data-ir="finanzas" data-arg="m:${ym}">${ic("finanzas")} Abrir</button></div><p class="suave" style="margin:6px 0 0">Tus montos están ocultos. Ábrelas en Ascenso financiero.</p>`);
  return hoja(`<div class="cab-lista-btn"><h3 style="margin:0">Finanzas del mes</h3><button class="btn mini" data-ir="finanzas" data-arg="m:${ym}">${ic("finanzas")} Abrir</button></div>
    ${t.n ? cifras(t) + (t.tasa != null ? `<p class="fin-libre">Ahorro e inversión: <b>${Math.round(t.tasa * 100)} %</b> de lo que entró</p>` : "") : `<p class="suave" style="margin:6px 0 0">Aún no hay movimientos en ${MESES[m - 1]}.</p>`}`);
};


/* ---------- Ordenar las filas del día: arrastrar el asa (mouse o dedo) o con las flechas del teclado ---------- */
{
  let arr = null;
  const filas = tb => [...tb.querySelectorAll("tr[data-mov]")];   // los de un día, los activos o los pasivos
  document.addEventListener("pointerdown", e => {
    const asa = e.target.closest?.(".asc-asa"); if (!asa || e.button > 0) return;
    const tr = asa.closest("tr"), tb = tr.parentElement;
    e.preventDefault(); asa.setPointerCapture?.(e.pointerId);
    arr = { asa, tr, tb, f: asa.dataset.orden || "dia:" + asa.dataset.dia, y0: e.clientY, antes: filas(tb).map(x => x.dataset.mov).join() };
    tr.classList.add("arrastrando"); document.body.classList.add("ordenando");
  });
  document.addEventListener("pointermove", e => {
    if (!arr) return;
    const { tr, tb } = arr;
    tr.style.transform = `translateY(${e.clientY - arr.y0}px)`;
    // Cuando el centro pasa sobre otra fila, la fila se mueve ahí y las demás se apartan
    const otras = filas(tb).filter(x => x !== tr);
    const destino = otras.find(x => { const r = x.getBoundingClientRect(); return e.clientY > r.top && e.clientY < r.bottom; });
    if (!destino) return;
    const r = destino.getBoundingClientRect(), antesDe = e.clientY < r.top + r.height / 2;
    const ref = antesDe ? destino : destino.nextElementSibling;
    if (ref === tr || (ref && ref.previousElementSibling === tr && !antesDe)) return;
    const top0 = tr.getBoundingClientRect().top;
    tb.insertBefore(tr, ref);
    arr.y0 += tr.getBoundingClientRect().top - top0;
    tr.style.transform = `translateY(${e.clientY - arr.y0}px)`;
  });
  const soltar = () => {
    if (!arr) return;
    const { tr, tb, f, antes } = arr; arr = null;
    tr.style.transform = ""; tr.classList.remove("arrastrando"); document.body.classList.remove("ordenando");
    const ids = filas(tb).map(x => x.dataset.mov);
    if (ids.join() !== antes) guardarOrden(f, ids);
  };
  document.addEventListener("pointerup", soltar); document.addEventListener("pointercancel", soltar);
  document.addEventListener("keydown", e => {
    const asa = e.target.closest?.(".asc-asa"); if (!asa || e.target !== asa) return;
    const paso = { ArrowUp: -1, ArrowDown: 1 }[e.key]; if (!paso) return;
    e.preventDefault();
    const tb = asa.closest("tbody"), ids = filas(tb).map(x => x.dataset.mov), i = ids.indexOf(asa.dataset.moverMov), j = i + paso;
    if (j < 0 || j >= ids.length) return;
    [ids[i], ids[j]] = [ids[j], ids[i]]; guardarOrden(asa.dataset.orden || "dia:" + asa.dataset.dia, ids);
    requestAnimationFrame(() => document.querySelector(`.asc-asa[data-mover-mov="${CSS.escape(ids[j])}"]`)?.focus());
  });
}
