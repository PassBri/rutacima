"use strict";
/* ======================================================================
 * Gráfica del ascenso (igual que la app, ui/finanzas/GraficaAscenso.kt).
 * Una gráfica de estilo bursátil para el balance general:
 *  · periodo: días, semanas, meses o años; rangos rápidos 1M, 3M, 6M, 1A, 5A y Todo
 *  · tipo: línea, área, barras o velas (las velas muestran el saldo acumulado: abrió, máximo, mínimo, cerró)
 *  · series: saldo acumulado, patrimonio construido, ingresos, gastos, ahorro, inversión y disponible
 *  · indicadores: media móvil y un panel de salud financiera en % (flujo neto, ahorro + inversión,
 *    gasto ÷ ingreso) con la meta del 20 %
 *  · se amplía con la rueda, pellizcando o con + y −; se mueve arrastrando o con las flechas;
 *    doble toque restablece. Crucero con lectura de cada periodo y vista de tabla.
 * Un solo eje de dinero (a la derecha, como en bolsa); los porcentajes van en su propio panel.
 * ====================================================================== */
const GR = { gran: "m", tipo: "linea", rango: "1A", series: ["saldo", "ing", "gas"], sma: false, salud: true, tabla: false, grande: false };
const GR_SERIES = [
  ["saldo", "Saldo acumulado", "--burdeos"], ["patr", "Patrimonio construido", "--oro"], ["ing", "Ingresos", "--v-fijos"],
  ["gas", "Gastos", "--v-variables"], ["aho", "Ahorro", "--v-ahorro"], ["inv", "Inversión", "--v-inversion"], ["gen", "Generosidad", "--v-generosidad"], ["disp", "Disponible", "--tinta"],
];
const GR_PCT = [["flujo", "Flujo neto", "--v-fijos"], ["constr", "Ahorro + inversión", "--v-ahorro"], ["gastoP", "Gasto ÷ ingreso", "--v-variables"]];
const GR_RANGOS = [["1M", "1M", "d", 31], ["3M", "3M", "d", 92], ["6M", "6M", "s", 26], ["1A", "1A", "m", 12], ["5A", "5A", "m", 60], ["todo", "Todo", "m", 0]];
const GR_GRAN = [["d", "Días"], ["s", "Semanas"], ["m", "Meses"], ["a", "Años"]];
const GR_TIPOS = [["linea", "Línea"], ["area", "Área"], ["barras", "Barras"], ["velas", "Velas"]];

/** Los periodos con sus sumas, el saldo acumulado (con apertura, máximo, mínimo y cierre) y los indicadores. */
function datosGrafica(gran) {
  const dias = diasConMovs(); if (!dias.length) return [];
  const porDia = new Map(dias.map(f => [f, sumar(movsDe(f))]));
  const lunes = d => { const x = new Date(d); x.setDate(x.getDate() - ((x.getDay() + 6) % 7)); return iso(x); };
  const clave = d => gran === "d" ? iso(d) : gran === "s" ? lunes(d) : gran === "m" ? iso(d).slice(0, 7) : String(d.getFullYear());
  const etq = (d, k) => gran === "d" ? `${d.getDate()} ${MES_C[d.getMonth()]}` : gran === "s" ? `sem ${Number(k.slice(8))} ${MES_C[Number(k.slice(5, 7)) - 1]}`
    : gran === "m" ? `${MES_C[d.getMonth()]} ${String(d.getFullYear()).slice(2)}` : k;
  const largo = (d, k) => gran === "d" ? `${d.getDate()} de ${MESES[d.getMonth()]} de ${d.getFullYear()}` : gran === "s" ? `Semana del ${Number(k.slice(8))} de ${MESES[Number(k.slice(5, 7)) - 1]} de ${k.slice(0, 4)}`
    : gran === "m" ? `${mayus(MESES[d.getMonth()])} de ${d.getFullYear()}` : k;
  const out = []; let saldo = 0, constr = 0, cur = null;
  const fin = hoyFecha(), ini = deIso(dias[0]) < fin ? deIso(dias[0]) : fin;
  for (let d = new Date(ini), n = 0; d <= fin && n < 20000; d.setDate(d.getDate() + 1), n++) {
    const k = clave(d), s = porDia.get(iso(d));
    if (!cur || cur.k !== k) { cur = { k, etq: etq(d, k), largo: largo(d, k), ing: 0, gas: 0, aho: 0, inv: 0, gen: 0, o: saldo, h: saldo, l: saldo, c: saldo }; out.push(cur); }
    if (s) { cur.ing += s.ing; cur.gas += s.gas; cur.aho += s.aho; cur.inv += s.inv; cur.gen += s.gen || 0; saldo += s.disp; constr += s.construido; }
    cur.h = Math.max(cur.h, saldo); cur.l = Math.min(cur.l, saldo); cur.c = cur.saldo = saldo; cur.patr = constr;
  }
  out.forEach(b => { b.disp = b.ing - b.gas - b.aho - b.inv - b.gen; b.flujo = b.ing ? b.disp / b.ing : null; b.constr = b.ing ? (b.aho + b.inv) / b.ing : null; b.gastoP = b.ing ? b.gas / b.ing : null; });
  return out;
}

/** La hoja con la barra de herramientas, el lienzo, la leyenda y la tabla. */
function graficaAscenso() {
  if (!diasConMovs().length) return "";
  const b = (acc, arg, txt, on, extra = "") => `<button class="gr-b" data-acc="${acc}" data-arg="${arg}" aria-pressed="${on}" ${extra}>${txt}</button>`;
  return hoja(`<div class="cab-lista-btn"><h3 style="margin:0">Gráfica del ascenso</h3><span class="suave gr-ayuda" style="font-size:13px">Amplía con la rueda o pellizcando · arrastra para moverte</span>
      <button class="btn mini ic-solo gr-grande-btn" data-acc="grGrande" aria-pressed="${GR.grande}" aria-label="${GR.grande ? "Salir de pantalla completa" : "Ver en pantalla completa para analizar"}" title="${GR.grande ? "Salir (Esc)" : "Pantalla completa"}">${ic(GR.grande ? "cerrar" : "pantallaCompleta")}</button></div>
    <div class="gr-barra" role="toolbar" aria-label="Opciones de la gráfica">
      <div class="gr-grupo" aria-label="Rango">${GR_RANGOS.map(([k, t]) => b("grRango", k, t, GR.rango === k)).join("")}</div>
      <div class="gr-grupo" aria-label="Periodo">${GR_GRAN.map(([k, t]) => b("grGran", k, t, GR.gran === k)).join("")}</div>
      <div class="gr-grupo" aria-label="Tipo de gráfica">${GR_TIPOS.map(([k, t]) => b("grTipo", k, t, GR.tipo === k)).join("")}</div>
      <div class="gr-grupo gr-zoom" aria-label="Acercar y alejar">${b("grZoom", "-1", "−", false, 'aria-label="Alejar"')}${b("grZoom", "1", "+", false, 'aria-label="Acercar"')}${b("grZoom", "0", "⟲", false, 'aria-label="Restablecer" title="Restablecer"')}</div>
    </div>
    <div class="gr-series" aria-label="Series">${GR_SERIES.map(([k, t, c]) => `<button class="gr-chip" data-acc="grSerie" data-arg="${k}" aria-pressed="${GR.series.includes(k)}" ${GR.tipo === "velas" && k === "saldo" ? "disabled" : ""}><i style="background:var(${c})"></i>${t}</button>`).join("")}
      <span class="gr-sep" aria-hidden="true"></span>
      <button class="gr-chip" data-acc="grSma" aria-pressed="${GR.sma}"><i class="gr-i-sma"></i>Media móvil</button>
      <button class="gr-chip" data-acc="grSalud" aria-pressed="${GR.salud}"><i class="gr-i-salud"></i>Salud financiera %</button></div>
    <div class="gr-kpis" id="grKpis" aria-live="polite"></div>
    <div class="gr-caja" id="grCaja"><canvas id="grLienzo" tabindex="0" role="img" aria-label="Gráfica del ascenso financiero"></canvas><div class="gr-tip" hidden></div></div>
    <div class="gr-pie"><span id="grRangoTxt" class="suave"></span><button class="enlace" data-acc="grTabla">${GR.tabla ? "Ocultar la tabla" : "Ver en tabla"}</button></div>
    <div id="grTabla"></div>`, "gr-hoja" + (GR.grande ? " gr-grande" : ""));
}
window.graficaAscenso = graficaAscenso;
/** En el balance: un acceso de una línea; la gráfica vive en su propia pantalla. */
window.graficaAcceso = () => diasConMovs().length ? `<button class="salud-enlace" data-sel="grafica">${av(ic("grafica"), "var(--ok)")}<span><b>Gráfica del ascenso</b><small>Amplía y recorre tus cuentas por días, semanas, meses o años · velas, barras y líneas</small></span>${ic("der")}</button>` : "";
DET["finanzas.grafica"] = () => {
  if (window.finBloqueado?.()) return pantallaBloqueo();
  const g = graficaAscenso();
  return cab(av(ic("grafica"), "var(--ok)"), "Gráfica del ascenso", "Ascenso financiero · como en la bolsa", ojoAsc()) + cuerpo(
    migasAsc([["Balance general", "b"], ["Gráfica del ascenso", ""]]) +
    (g || hoja(`<p class="suave" style="margin:0">Cuando anotes tus primeros movimientos, aquí verás cómo sube tu saldo día a día, mes a mes y año a año.</p><div class="botones" style="margin-top:12px"><button class="btn lleno" data-sel="d:${iso(hoyFecha())}">Registrar hoy</button></div>`)), "max-width:1100px");
};

/* ---------- Dibujo e interacción ---------- */
const grEstado = { datos: [], i0: 0, i1: 0, hover: -1, gran: null };
const grColor = v => getComputedStyle(document.documentElement).getPropertyValue(v).trim() || "#888";
const grCompacto = v => estado.ascOculto ? "•••" : new Intl.NumberFormat(ascLocale, { notation: "compact", maximumFractionDigits: 1 }).format(v);
const grPct = v => v == null ? "—" : Math.round(v * 100) + " %";

function grIniciar() {
  const cv = document.getElementById("grLienzo"); if (!cv || cv.dataset.listo) return;
  cv.dataset.listo = "1";
  const antes = { ...grEstado };
  grEstado.datos = datosGrafica(GR.gran);
  // si solo cambió el tipo, una serie o un indicador, se conserva la ventana ampliada
  if (!GR.rango && antes.gran === GR.gran && antes.datos.length === grEstado.datos.length && antes.i1 > antes.i0) { grEstado.i0 = antes.i0; grEstado.i1 = antes.i1; }
  else grAplicarRango(false);
  grEstado.gran = GR.gran;
  grEnlazar(cv); grDibujar(); grTabla();
}
/** Ventana visible según el rango elegido (los últimos N días, semanas, meses…). */
function grAplicarRango(redibujar = true) {
  const n = grEstado.datos.length, r = GR_RANGOS.find(x => x[0] === GR.rango);
  let cuantos = n;
  if (r && r[3]) { const dias = { d: 1, s: 7, m: 30.4, a: 365 }[GR.gran], meses = r[2] === "m" ? r[3] : r[3] * ({ d: 1, s: 7 }[r[2]] || 1) / 30.4; cuantos = Math.ceil(meses * 30.4 / dias); }
  grEstado.i1 = n - 1; grEstado.i0 = Math.max(0, n - Math.max(2, cuantos));
  if (redibujar) { grDibujar(); grTabla(); }
}
function grZoom(f, centro) {
  const { i0, i1, datos } = grEstado, n = datos.length, ancho = i1 - i0 + 1;
  const nuevo = Math.max(Math.min(5, n), Math.min(n, Math.round(ancho / f)));
  const c = centro ?? (i0 + i1) / 2, p = (c - i0) / Math.max(1, ancho - 1);
  let a = Math.round(c - p * (nuevo - 1)); a = Math.max(0, Math.min(n - nuevo, a));
  grEstado.i0 = a; grEstado.i1 = a + nuevo - 1; GR.rango = ""; grMarcarRango(); grDibujar(); grTabla();
}
function grMover(k) {
  const { i0, i1, datos } = grEstado, w = i1 - i0, a = Math.max(0, Math.min(datos.length - 1 - w, i0 + k));
  if (a === i0) return; grEstado.i0 = a; grEstado.i1 = a + w; GR.rango = ""; grMarcarRango(); grDibujar();
}
const grMarcarRango = () => document.querySelectorAll('[data-acc="grRango"]').forEach(b => b.setAttribute("aria-pressed", b.dataset.arg === GR.rango));

function grGeometria(cv) {
  const alto = GR.grande ? Math.max(380, innerHeight - (cv.getBoundingClientRect().top || 260) - 70) : 0;
  const W = cv.clientWidth, salud = GR.salud, H = GR.grande ? alto : salud ? 360 : 280, R = 64, L = 8, T = 10, xAlto = 22, pctAlto = salud ? (GR.grande ? Math.round(alto * 0.24) : 86) : 0, gap = salud ? 14 : 0;
  return { W, H, L, R, T, principal: [T, H - xAlto - pctAlto - gap], pct: salud ? [H - xAlto - pctAlto, H - xAlto] : null, xAlto };
}
function grEnlazar(cv) {
  const indice = x => { const g = grGeometria(cv), n = grEstado.i1 - grEstado.i0 + 1, paso = (g.W - g.L - g.R) / n; return grEstado.i0 + Math.max(0, Math.min(n - 1, Math.floor((x - g.L) / paso))); };
  const pasoPx = () => { const g = grGeometria(cv); return (g.W - g.L - g.R) / (grEstado.i1 - grEstado.i0 + 1); };
  cv.addEventListener("wheel", e => { e.preventDefault(); const r = cv.getBoundingClientRect(); grZoom(e.deltaY < 0 ? 1.25 : 0.8, indice(e.clientX - r.left)); }, { passive: false });
  const ptrs = new Map(); let arrastre = null, pellizco = null;
  cv.addEventListener("pointerdown", e => { cv.setPointerCapture(e.pointerId); ptrs.set(e.pointerId, e.clientX);
    if (ptrs.size === 1) arrastre = { x: e.clientX, i0: grEstado.i0 };
    if (ptrs.size === 2) { const [a, b] = [...ptrs.values()]; pellizco = { d: Math.abs(a - b), ancho: grEstado.i1 - grEstado.i0 + 1 }; arrastre = null; } });
  cv.addEventListener("pointermove", e => {
    const r = cv.getBoundingClientRect();
    if (ptrs.has(e.pointerId)) ptrs.set(e.pointerId, e.clientX);
    if (pellizco && ptrs.size === 2) { const [a, b] = [...ptrs.values()], d = Math.max(10, Math.abs(a - b)); const objetivo = pellizco.ancho * pellizco.d / d;
      grZoom((grEstado.i1 - grEstado.i0 + 1) / objetivo, indice((a + b) / 2 - r.left)); return; }
    if (arrastre) { const k = Math.round((arrastre.x - e.clientX) / pasoPx()); const w = grEstado.i1 - grEstado.i0;
      const a = Math.max(0, Math.min(grEstado.datos.length - 1 - w, arrastre.i0 + k)); if (a !== grEstado.i0) { grEstado.i0 = a; grEstado.i1 = a + w; GR.rango = ""; grMarcarRango(); } }
    grEstado.hover = indice(e.clientX - r.left); grDibujar();
  });
  const soltar = e => { ptrs.delete(e.pointerId); if (ptrs.size < 2) pellizco = null; if (!ptrs.size) { if (arrastre) grTabla(); arrastre = null; } };
  cv.addEventListener("pointerup", soltar); cv.addEventListener("pointercancel", soltar);
  cv.addEventListener("pointerleave", e => { if (e.pointerType === "mouse") { grEstado.hover = -1; grDibujar(); } });
  cv.addEventListener("dblclick", () => { GR.rango = "1A"; grMarcarRango(); grAplicarRango(); });
  cv.addEventListener("keydown", e => {
    const k = { ArrowLeft: () => grMover(-1), ArrowRight: () => grMover(1), "+": () => grZoom(1.25), "=": () => grZoom(1.25), "-": () => grZoom(0.8), Home: () => grMover(-1e6), End: () => grMover(1e6) }[e.key];
    if (k) { e.preventDefault(); k(); }
  });
  new ResizeObserver(() => grDibujar()).observe(cv);
}

function grDibujar() {
  const cv = document.getElementById("grLienzo"); if (!cv) return;
  const g = grGeometria(cv), dpr = window.devicePixelRatio || 1;
  cv.style.height = g.H + "px"; cv.width = Math.round(g.W * dpr); cv.height = Math.round(g.H * dpr);
  const x = cv.getContext("2d"); x.setTransform(dpr, 0, 0, dpr, 0, 0); x.clearRect(0, 0, g.W, g.H);
  const { datos, i0, i1 } = grEstado, vis = datos.slice(i0, i1 + 1), n = vis.length; if (!n) return;
  const tinta = grColor("--tinta"), suave = grColor("--tinta-suave"), linea = grColor("--linea"), fondo = grColor("--hoja");
  const fuente = "11px Roboto, system-ui, sans-serif";
  const pw = g.W - g.L - g.R, paso = pw / n, cx = i => g.L + (i + 0.5) * paso;
  const velas = GR.tipo === "velas", series = GR.series.filter(k => !(velas && k === "saldo"));
  // escala del dinero
  const vals = [0]; vis.forEach(b => { series.forEach(k => vals.push(b[k])); if (velas) vals.push(b.h, b.l); });
  let lo = Math.min(...vals), hi = Math.max(...vals); if (hi === lo) hi = lo + 1; const pad = (hi - lo) * 0.06; lo -= lo < 0 ? pad : 0; hi += pad;
  const [pt, pb] = g.principal, y = v => pt + (hi - v) / (hi - lo) * (pb - pt);
  // cuadrícula y eje derecho
  x.font = fuente; x.textBaseline = "middle"; x.lineWidth = 1;
  grTicks(lo, hi, 5).forEach(v => { x.strokeStyle = v === 0 ? suave : linea; x.globalAlpha = v === 0 ? 0.6 : 1; x.setLineDash(v === 0 ? [] : [2, 4]); x.beginPath(); x.moveTo(g.L, Math.round(y(v)) + 0.5); x.lineTo(g.W - g.R, Math.round(y(v)) + 0.5); x.stroke(); x.globalAlpha = 1;
    x.fillStyle = suave; x.textAlign = "left"; x.fillText(grCompacto(v), g.W - g.R + 8, y(v)); }); x.setLineDash([]); x.globalAlpha = 1;
  // barras agrupadas (o áreas y líneas)
  const flujos = series.filter(k => ["ing", "gas", "aho", "inv", "gen", "disp"].includes(k)), acumulados = series.filter(k => ["saldo", "patr"].includes(k));
  if (GR.tipo === "barras" && flujos.length) {
    const gw = Math.max(1, paso * 0.78), bw = Math.max(1, (gw - 2 * (flujos.length - 1)) / flujos.length);
    vis.forEach((b, i) => flujos.forEach((k, j) => { const v = b[k], x0 = cx(i) - gw / 2 + j * (bw + 2), ya = y(Math.max(0, v)), yb = y(Math.min(0, v));
      x.fillStyle = grColor(GR_SERIES.find(s => s[0] === k)[2]); grRect(x, x0, ya, bw, Math.max(1, yb - ya), Math.min(4, bw / 2), v >= 0); }));
  }
  const dibujarLinea = (k, area) => {
    const c = grColor(GR_SERIES.find(s => s[0] === k)[2]);
    x.beginPath(); vis.forEach((b, i) => i ? x.lineTo(cx(i), y(b[k])) : x.moveTo(cx(i), y(b[k])));
    if (area) { x.save(); x.lineTo(cx(n - 1), y(Math.max(lo, 0))); x.lineTo(cx(0), y(Math.max(lo, 0))); x.closePath();
      // La cordillera de tu dinero: degradado del color de la serie hacia el papel
      const gr = x.createLinearGradient(0, pt, 0, y(Math.max(lo, 0))); gr.addColorStop(0, c + "55"); gr.addColorStop(1, c + "05");
      x.fillStyle = /^#[0-9a-f]{6}$/i.test(c) ? gr : c; if (!/^#[0-9a-f]{6}$/i.test(c)) x.globalAlpha = 0.14; x.fill(); x.restore();
      x.beginPath(); vis.forEach((b, i) => i ? x.lineTo(cx(i), y(b[k])) : x.moveTo(cx(i), y(b[k]))); }
    x.strokeStyle = c; x.lineWidth = 2; x.lineJoin = "round"; x.stroke();
  };
  if (GR.tipo !== "barras") flujos.forEach(k => dibujarLinea(k, GR.tipo === "area"));
  acumulados.forEach(k => dibujarLinea(k, k === "saldo" && GR.tipo !== "barras"));
  // La cumbre del periodo: una bandera en el saldo más alto; y el punto de hoy con su halo
  if (series.includes("saldo") && n > 2 && !velas) {
    let im = 0; vis.forEach((b, i) => { if (b.saldo > vis[im].saldo) im = i; });
    const px = cx(im), py = y(vis[im].saldo), oro = grColor("--oro");
    if (vis[im].saldo > 0) {
      x.strokeStyle = oro; x.lineWidth = 1.5; x.beginPath(); x.moveTo(px, py - 4); x.lineTo(px, py - 24); x.stroke();
      x.fillStyle = oro; x.beginPath(); x.moveTo(px, py - 24); x.lineTo(px + 11, py - 20); x.lineTo(px, py - 16); x.closePath(); x.fill();
      x.font = "600 11px Roboto, system-ui, sans-serif"; x.textAlign = px > g.W - g.R - 90 ? "right" : "left"; x.textBaseline = "alphabetic"; x.fillStyle = tinta;
      x.fillText("Cumbre " + grCompacto(vis[im].saldo), px + (x.textAlign === "right" ? -6 : 14), Math.max(12, py - 18)); x.font = fuente;
    }
    if (i1 === datos.length - 1) { const hx = cx(n - 1), hy = y(vis[n - 1].saldo), c0 = grColor("--burdeos");
      x.globalAlpha = 0.18; x.fillStyle = c0; x.beginPath(); x.arc(hx, hy, 10, 0, 7); x.fill(); x.globalAlpha = 1;
      x.beginPath(); x.arc(hx, hy, 4.5, 0, 7); x.fillStyle = c0; x.fill(); x.lineWidth = 2; x.strokeStyle = fondo; x.stroke(); }
  }
  // velas del saldo acumulado: sube = hueca y verde, baja = llena y roja (se distinguen sin color)
  if (velas) {
    const sube = grColor("--st-ok"), baja = grColor("--st-bajo"), bw = Math.max(1, Math.min(18, paso * 0.66));
    vis.forEach((b, i) => { const up = b.c >= b.o, c = up ? sube : baja; x.strokeStyle = c; x.lineWidth = 1;
      x.beginPath(); x.moveTo(Math.round(cx(i)) + 0.5, y(b.h)); x.lineTo(Math.round(cx(i)) + 0.5, y(b.l)); x.stroke();
      const ya = y(Math.max(b.o, b.c)), alto = Math.max(1, Math.abs(y(b.o) - y(b.c)));
      if (up) { x.fillStyle = fondo; x.fillRect(cx(i) - bw / 2, ya, bw, alto); x.lineWidth = 1.5; x.strokeRect(cx(i) - bw / 2 + 0.75, ya + 0.75, Math.max(0, bw - 1.5), Math.max(0, alto - 1.5)); }
      else { x.fillStyle = c; x.fillRect(cx(i) - bw / 2, ya, bw, alto); } });
  }
  // media móvil (3 periodos) de la serie principal
  if (GR.sma) {
    const k = velas ? "c" : (acumulados[0] || flujos[0]); if (k) {
      x.beginPath(); let empezo = false;
      vis.forEach((b, i) => { const j = i0 + i; if (j < 2) return; const m = (datos[j][k] + datos[j - 1][k] + datos[j - 2][k]) / 3; if (!empezo) { x.moveTo(cx(i), y(m)); empezo = true; } else x.lineTo(cx(i), y(m)); });
      x.setLineDash([6, 4]); x.strokeStyle = suave; x.lineWidth = 1.5; x.stroke(); x.setLineDash([]);
    }
  }
  // panel de salud financiera en %
  if (g.pct) {
    const [qt, qb] = g.pct, pv = vis.flatMap(b => GR_PCT.map(([k]) => b[k]).filter(v => v != null));
    let plo = Math.min(0, ...pv), phi = Math.max(0.3, ...pv); plo = Math.max(plo, -1); phi = Math.min(phi, 1.5);
    const py = v => qt + (phi - Math.max(plo, Math.min(phi, v))) / (phi - plo) * (qb - qt);
    // Zona sana: desde el 20 % hacia arriba, la meta de ahorro + inversión
    x.fillStyle = grColor("--ok"); x.globalAlpha = 0.08; x.fillRect(g.L, py(phi), g.W - g.R - g.L, py(0.2) - py(phi)); x.globalAlpha = 1;
    x.fillStyle = suave; x.textAlign = "left"; x.fillText("Salud %", g.L, qt - 6);
    x.fillStyle = grColor("--ok"); x.textAlign = "right"; x.fillText("Zona sana", g.W - g.R - 6, py(0.2) - 7); x.textAlign = "left";
    [plo, 0, 0.2, phi].filter((v, i, a) => a.indexOf(v) === i).forEach(v => { x.strokeStyle = linea; x.lineWidth = 1; if (v === 0.2) { x.setLineDash([4, 4]); x.strokeStyle = suave; }
      x.beginPath(); x.moveTo(g.L, Math.round(py(v)) + 0.5); x.lineTo(g.W - g.R, Math.round(py(v)) + 0.5); x.stroke(); x.setLineDash([]);
      x.fillStyle = suave; x.fillText(Math.round(v * 100) + " %", g.W - g.R + 8, py(v)); });
    GR_PCT.forEach(([k, , c]) => { x.beginPath(); let abierto = false;
      vis.forEach((b, i) => { if (b[k] == null) { abierto = false; return; } abierto ? x.lineTo(cx(i), py(b[k])) : x.moveTo(cx(i), py(b[k])); abierto = true; });
      x.strokeStyle = grColor(c); x.lineWidth = 1.5; x.stroke(); });
  }
  // eje de tiempo
  x.fillStyle = suave; x.textAlign = "center"; x.textBaseline = "alphabetic";
  const cada = Math.max(1, Math.ceil(70 / paso));
  vis.forEach((b, i) => { if ((i0 + i) % cada === 0) x.fillText(b.etq, Math.max(24, Math.min(g.W - g.R - 24, cx(i))), g.H - 6); });
  // crucero y lectura
  const tip = document.querySelector("#grCaja .gr-tip"), h = grEstado.hover;
  if (h >= i0 && h <= i1) {
    const i = h - i0, b = datos[h]; x.strokeStyle = suave; x.lineWidth = 1; x.setLineDash([3, 3]); x.beginPath(); x.moveTo(Math.round(cx(i)) + 0.5, g.T); x.lineTo(Math.round(cx(i)) + 0.5, g.H - g.xAlto); x.stroke(); x.setLineDash([]);
    series.forEach(k => { if (GR.tipo === "barras" && flujos.includes(k)) return; x.beginPath(); x.arc(cx(i), y(b[k]), 4, 0, 7); x.fillStyle = grColor(GR_SERIES.find(s => s[0] === k)[2]); x.fill(); x.lineWidth = 2; x.strokeStyle = fondo; x.stroke(); });
    if (tip) {
      const filas = [...(velas ? [["Abrió", b.o], ["Máximo", b.h], ["Mínimo", b.l], ["Cerró", b.c]].map(([t, v]) => [t, plata(v), b.c >= b.o ? "--st-ok" : "--st-bajo"]) : []),
        ...series.map(k => { const s = GR_SERIES.find(z => z[0] === k); return [s[1], plata(b[k]), s[2]]; }),
        ...(GR.salud ? GR_PCT.map(([k, t, c]) => [t, grPct(b[k]), c]) : [])];
      tip.replaceChildren(Object.assign(document.createElement("b"), { textContent: b.largo }), ...filas.map(([t, v, c]) => {
        const d = document.createElement("div"), i = document.createElement("i"); i.style.background = `var(${c})`;
        d.append(i, Object.assign(document.createElement("span"), { textContent: t }), Object.assign(document.createElement("strong"), { textContent: v })); return d; }));
      tip.hidden = false; const izq = cx(i) > g.W / 2; tip.style.left = izq ? "" : (cx(i) + 14) + "px"; tip.style.right = izq ? (g.W - cx(i) + 14) + "px" : "";
    }
  } else if (tip) tip.hidden = true;
  const kp = document.getElementById("grKpis");
  if (kp) { const ini = vis[0].o, fin = vis[n - 1].saldo, dif = fin - ini, cumbre = vis.reduce((m, b) => b.h > m.h ? b : m, vis[0]);
    kp.innerHTML = `<div><span>Saldo al cierre</span><b>${plata(fin)}</b></div>
      <div class="${dif >= 0 ? "sube" : "baja"}"><span>Cambio en el periodo</span><b>${dif >= 0 ? "▲" : "▼"} ${plata(Math.abs(dif))}${ini > 0 ? ` <small>${dif >= 0 ? "+" : "−"}${Math.abs(Math.round(dif / ini * 100))} %</small>` : ""}</b></div>
      <div><span>Cumbre</span><b>${plata(cumbre.h)}</b><small>${esc(cumbre.largo)}</small></div>`; }
  const t = document.getElementById("grRangoTxt"); if (t) t.textContent = `${vis[0].largo} — ${vis[n - 1].largo} · ${n} ${({ d: "días", s: "semanas", m: "meses", a: "años" })[GR.gran]}`;
  cv.setAttribute("aria-label", `Gráfica del ascenso de ${vis[0].largo} a ${vis[n - 1].largo}. Saldo acumulado al final: ${plata(vis[n - 1].saldo)}. Usa las flechas para moverte y más o menos para ampliar.`);
}
/** Rectángulo con las esquinas de la punta redondeadas (la base queda recta). */
function grRect(x, x0, y0, w, h, r, arriba) {
  x.beginPath();
  if (arriba) { x.moveTo(x0, y0 + h); x.lineTo(x0, y0 + r); x.quadraticCurveTo(x0, y0, x0 + r, y0); x.lineTo(x0 + w - r, y0); x.quadraticCurveTo(x0 + w, y0, x0 + w, y0 + r); x.lineTo(x0 + w, y0 + h); }
  else { x.moveTo(x0, y0); x.lineTo(x0, y0 + h - r); x.quadraticCurveTo(x0, y0 + h, x0 + r, y0 + h); x.lineTo(x0 + w - r, y0 + h); x.quadraticCurveTo(x0 + w, y0 + h, x0 + w, y0 + h - r); x.lineTo(x0 + w, y0); }
  x.closePath(); x.fill();
}
function grTicks(lo, hi, n) {
  const paso0 = (hi - lo) / n, m = Math.pow(10, Math.floor(Math.log10(paso0))), paso = [1, 2, 2.5, 5, 10].map(k => k * m).find(k => k >= paso0) || paso0;
  const r = []; for (let v = Math.ceil(lo / paso) * paso; v <= hi; v += paso) r.push(Math.round(v / paso) * paso || 0); return r;
}
function grTabla() {
  const el = document.getElementById("grTabla"); if (!el) return;
  if (!GR.tabla) { el.innerHTML = ""; return; }
  const vis = grEstado.datos.slice(grEstado.i0, grEstado.i1 + 1).reverse();
  el.innerHTML = `<div class="asc-tabla" style="max-height:360px;overflow:auto"><table><thead><tr><th>Periodo</th><th class="num">Ingresos</th><th class="num">Gastos</th><th class="num">Ahorro</th><th class="num">Inversión</th><th class="num">Disponible</th><th class="num">Saldo acumulado</th><th class="num">Flujo neto</th><th class="num">Ahorro + inv.</th></tr></thead><tbody>
    ${vis.map(b => `<tr><th scope="row">${esc(b.largo)}</th>${["ing", "gas", "aho", "inv", "disp", "saldo"].map(k => `<td class="num${b[k] < 0 ? " neg" : ""}">${b[k] ? plata(b[k]) : "—"}</td>`).join("")}<td class="num">${grPct(b.flujo)}</td><td class="num">${grPct(b.constr)}</td></tr>`).join("")}</tbody></table></div>`;
}

const grRepintar = () => { const h = document.querySelector(".gr-hoja"); if (!h) return; const i0 = grEstado.i0, i1 = grEstado.i1; h.outerHTML = graficaAscenso(); return [i0, i1]; };
Object.assign(ACC, {
  grRango(_, k) { GR.rango = k; const r = GR_RANGOS.find(x => x[0] === k); GR.gran = r[2] === "s" ? "s" : r[2]; if (k === "todo") GR.gran = grEstado.datos.length > 0 && datosGrafica("m").length > 60 ? "a" : "m"; grRepintar(); },
  grGran(_, k) { GR.gran = k; GR.rango = GR.rango || "todo"; grRepintar(); },
  /** Pantalla completa para analizar: la gráfica ocupa toda la ventana; Esc o el botón la cierran. */
  grGrande() {
    GR.grande = !GR.grande; document.body.classList.toggle("gr-pantalla", GR.grande); const [i0, i1] = grRepintar() || [];
    if (i1 > i0) { grEstado.i0 = i0; grEstado.i1 = i1; requestAnimationFrame(() => { grDibujar(); grTabla(); }); }
    document.querySelector(".gr-hoja")?.focus?.();
  },
  grTipo(_, k) { GR.tipo = k; if (k === "velas" && !GR.series.includes("saldo")) GR.series.push("saldo"); grRepintar(); },
  grSerie(_, k) { GR.series = GR.series.includes(k) ? GR.series.filter(x => x !== k) : [...GR.series, k]; if (!GR.series.length) GR.series = ["saldo"]; grRepintar(); },
  grSma() { GR.sma = !GR.sma; grRepintar(); },
  grSalud() { GR.salud = !GR.salud; grRepintar(); },
  grTabla() { GR.tabla = !GR.tabla; grRepintar(); },
  grZoom(_, k) { if (k === "0") { GR.rango = "1A"; GR.gran = "m"; grRepintar(); } else grZoom(k === "1" ? 1.5 : 1 / 1.5); },
});
// Cada vez que se pinta el balance, el lienzo se prepara (y conserva la ventana si solo cambió una opción)
new MutationObserver(() => { const cv = document.getElementById("grLienzo"); if (cv && !cv.dataset.listo) grIniciar(); }).observe(document.body, { childList: true, subtree: true });

/* Esc sale de la pantalla completa; al cambiar el tamaño de la ventana se redibuja */
addEventListener("keydown", e => { if (e.key === "Escape" && GR.grande && document.querySelector(".gr-grande")) { e.preventDefault(); ACC.grGrande(); } });
addEventListener("resize", () => { if (document.getElementById("grLienzo")) grDibujar(); });
new MutationObserver(() => { if (GR.grande && !document.querySelector(".gr-grande")) { GR.grande = false; document.body.classList.remove("gr-pantalla"); } }).observe(document.body, { childList: true, subtree: true });
