"use strict";
/* =====================================================================================
 * Ascenso financiero con barra a la derecha (igual que Mi ruta y Evaluación, y que la app):
 *  - Arriba: el año con sus 12 meses (cada aro dice cómo cerró: verde si quedó disponible, burdeos si salió
 *    más de lo que entró; con presupuesto, semáforo de gasto), y el mes con sus días (puntos por tipo de movimiento).
 *  - En medio: los atajos (balance, cuentas fijas, deudas, metas, salud, gráfica, patrimonio e informe PDF).
 *  - La cordillera del patrimonio: cada año es un pico tan alto como lo construido en ahorro e inversión.
 *  - Abajo: «Así va el mes» y una frase financiera del día. En el teléfono, la barra se pliega arriba.
 * ===================================================================================== */
const FB_COLOR = { ing: "#2E6B3A", gas: "#8C2F1E", aho: "#2F5D8A", inv: "#6A4A8C", gen: "#A0522D" };
const FRASES_FIN = [
  "Primero págate a ti: lo que ahorras es la parte del sueldo que se queda contigo.",
  "Lo que no se mide no se mejora. Anotar es el primer paso del ascenso.",
  "No es cuánto ganas, es cuánto conservas y haces crecer.",
  "Un gasto pequeño repetido cada día es una deuda grande al final del año.",
  "El fondo de emergencia es la cuerda que te sostiene cuando el terreno cede.",
  "Gastar con intención también es una forma de ahorrar.",
  "La paciencia es el interés compuesto del carácter.",
  "Antes de comprar, pregúntate cuántas horas de tu vida cuesta.",
  "Tu presupuesto no te quita libertad: te dice a dónde se va.",
  "Dar también construye: la generosidad hace más rico el camino.",
  "Invierte primero en aprender: es el activo que nadie te quita.",
  "Cada peso con un propósito: así se sube sin perder el aire.",
];

/** Lo que muestra el centro: año, mes o día (los atajos recuerdan el último lugar). */
function fbLugar() {
  const v = String(estado.sel || ""), arg = v.split(":").slice(1).join(":");
  if (v.startsWith("a:") && /^\d{4}$/.test(arg)) return { a: Number(arg) };
  if (v.startsWith("m:") && /^\d{4}-\d\d$/.test(arg)) { const [a, m] = arg.split("-").map(Number); return { a, m }; }
  if (v.startsWith("d:") && /^\d{4}-\d\d-\d\d$/.test(arg)) { const [a, m, d] = arg.split("-").map(Number); return { a, m, d }; }
  return estado.fbUltimo || {};
}
const fbArriba = () => { const v = String(estado.sel || ""), l = fbLugar();
  return v.startsWith("d:") ? `m:${l.a}-${pad2(l.m)}` : v.startsWith("m:") ? `a:${l.a}` : v.startsWith("a:") ? "b" : v === "b" ? null : "b"; };
function fbVer() {
  const l = fbLugar(), hoy = hoyFecha();
  if (!estado.fbVer || estado.fbVerSel !== estado.sel) { estado.fbVer = { a: l.a || hoy.getFullYear(), m: l.m || (l.a && l.a !== hoy.getFullYear() ? 1 : hoy.getMonth() + 1) }; estado.fbVerSel = estado.sel; }
  return estado.fbVer;
}

/** Un solo recorrido por los días con movimientos: totales por día, por mes y por año (la barra se dibuja seguido). */
function fbDatos() {
  const dias = diasConMovs(), porDia = {}, porMes = {}, porAnio = {};
  for (const f of dias) {
    const movs = movsDe(f), s = sumar(movs), tipos = new Set(movs.filter(x => !x.prog).map(x => x.t));
    porDia[f] = { s, tipos };
    for (const [k, mapa] of [[f.slice(0, 7), porMes], [f.slice(0, 4), porAnio]]) {
      const t = mapa[k] ||= { ing: 0, gas: 0, aho: 0, inv: 0, gen: 0, n: 0 };
      ["ing", "gas", "aho", "inv", "gen"].forEach(x => { t[x] += s[x] || 0; }); t.n += s.n;
    }
  }
  [porMes, porAnio].forEach(m => Object.values(m).forEach(t => { t.disp = t.ing - t.gas - t.aho - t.inv - t.gen; t.construido = t.aho + t.inv; }));
  return { dias, porDia, porMes, porAnio };
}

/** Cómo cerró un periodo: bien (sobró, o quedó en cero pero ahorró o invirtió), justo (en cero sin ahorro) o mal (salió más). */
const fbEstado = t => t.disp < -0.5 ? "mal" : Math.abs(t.disp) < 0.5 ? (t.construido > 0 ? "bien" : "justo") : "bien";
/** Aro del mes: verde si quedó disponible, burdeos si no; con presupuesto, se llena con lo gastado (semáforo). */
function fbAro(t, ym, actual) {
  const pres = Math.abs(montoFin(respuesta(claveFinPresupuesto(ym))));
  let color = "var(--linea)", fr = 0, titulo = "Sin movimientos";
  if (t?.n) {
    if (pres) { const uso = t.gas / pres; fr = Math.min(1, uso); color = uso > 1 ? "var(--burdeos)" : uso > 0.8 ? "var(--oro)" : "var(--ok, #2E6B3A)";
      titulo = `Gastaste ${Math.round(uso * 100)} % del presupuesto`; }
    else { fr = t.ing > 0 ? Math.max(0.12, Math.min(1, Math.max(0, t.disp) / t.ing + (t.construido / t.ing))) : 1;
      // En cero: si ahorró o invirtió, todo el dinero tuvo destino (verde); si no, quedó apretado (dorado)
      const e = fbEstado(t); color = e === "bien" ? "var(--ok, #2E6B3A)" : e === "justo" ? "var(--oro)" : "var(--burdeos)";
      titulo = e === "mal" ? "Salió más de lo que entró" : Math.abs(t.disp) < 0.5 ? (e === "bien" ? "En cero: todo tuvo destino" : "En cero, sin ahorro") : "Cerró con disponible"; }
  }
  const r = 11, c = 2 * Math.PI * r;
  return { titulo, svg: `<svg class="fb-aro" viewBox="0 0 28 28" aria-hidden="true"><circle cx="14" cy="14" r="${r}" fill="none" stroke="var(--linea)" stroke-width="2.4"/>
    ${fr ? `<circle cx="14" cy="14" r="${r}" fill="none" stroke="${color}" stroke-width="2.6" stroke-linecap="round" stroke-dasharray="${(fr * c).toFixed(1)} ${c.toFixed(1)}" transform="rotate(-90 14 14)"/>` : ""}
    ${t?.n ? `<circle cx="14" cy="14" r="2.4" fill="${color}"/>` : ""}${actual ? `<circle cx="14" cy="14" r="${r + 2.6}" fill="none" stroke="var(--burdeos)" stroke-width="1" stroke-dasharray="2 2"/>` : ""}</svg>` };
}

/** La cordillera del patrimonio: un pico por año, alto según lo construido (acumulado) en ahorro e inversión. */
function fbCordillera(datos, aViendo) {
  const anios = Object.keys(datos.porAnio).map(Number).sort((x, y) => x - y); if (!anios.length) return "";
  let acum = 0; const picos = anios.map(a => { acum += Math.max(0, datos.porAnio[a].construido); return { a, v: acum, del: datos.porAnio[a].construido }; });
  const max = Math.max(...picos.map(p => p.v), 1), W = 236, H = 70, paso = W / (picos.length + 1);
  const pts = picos.map((p, i) => [paso * (i + 1), H - 8 - (p.v / max) * (H - 22)]);
  const d = `M0 ${H} ` + pts.map(([x, y], i) => `L${(x - paso / 2).toFixed(1)} ${(H - 6 - (i ? (pts[i - 1][1] < y ? 6 : 10) : 4)).toFixed(1)} L${x.toFixed(1)} ${y.toFixed(1)}`).join(" ") + ` L${W} ${H - 4} L${W} ${H} Z`;
  return `<section class="fb-cordillera" aria-label="La cordillera de tu patrimonio"><b class="rv-rec-t">Tu cordillera del patrimonio</b>
    <svg viewBox="0 0 ${W} ${H}" role="img" aria-label="Lo construido año a año en ahorro e inversión"><defs><linearGradient id="fbCg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="var(--burdeos)" stop-opacity=".55"/><stop offset="1" stop-color="var(--burdeos)" stop-opacity=".08"/></linearGradient></defs>
      <path d="${d}" fill="url(#fbCg)" stroke="var(--burdeos)" stroke-width="1.4" stroke-linejoin="round"/>
      ${pts.map(([x, y], i) => `<g class="fb-pico${picos[i].a === aViendo ? " viendo" : ""}" data-sel="a:${picos[i].a}" tabindex="0" role="button" aria-label="${picos[i].a}: ${plata(picos[i].v)} construidos en total"><title>${picos[i].a} · ${plata(picos[i].del)} ese año · ${plata(picos[i].v)} acumulado</title>
        <circle cx="${x.toFixed(1)}" cy="${y.toFixed(1)}" r="${picos[i].a === aViendo ? 4 : 3}"/><text x="${x.toFixed(1)}" y="${H - 1}" text-anchor="middle">${String(picos[i].a).slice(2)}</text></g>`).join("")}</svg>
    <small class="suave">${plata(acum)} construidos en total · toca un año</small></section>`;
}

/** Racha de registro: días seguidos anotando algo (hasta ayer si hoy aún no). */
function fbRacha(porDia) {
  const d = hoyFecha(); if (!porDia[iso(d)]) d.setDate(d.getDate() - 1);
  let n = 0; while (porDia[iso(d)] && n < 3650) { n++; d.setDate(d.getDate() - 1); }
  return n;
}

/** Tarjeta viva: así va el mes y la frase financiera del día. */
function fbTarjeta(datos) {
  const hoy = hoyFecha(), ym = iso(hoy).slice(0, 7), t = datos.porMes[ym] || { ing: 0, gas: 0, disp: 0, n: 0 };
  const pres = Math.abs(montoFin(respuesta(claveFinPresupuesto(ym)))), uso = pres ? t.gas / pres : null;
  const faltan = diasEnMes(hoy.getFullYear(), hoy.getMonth() + 1) - hoy.getDate();
  const frase = FRASES_FIN[Math.floor(hoy.getTime() / DIA_MS) % FRASES_FIN.length], racha = fbRacha(datos.porDia);
  return `<section class="rv-rec fb-tarjeta" aria-label="Así va el mes"><b class="rv-rec-t">Así va ${MESES[hoy.getMonth()]}</b>
    <div class="rv-rec-cifras"><div><b class="${t.disp < 0 ? "neg" : ""}">${plata(t.disp)}</b><small>disponible</small></div><div><b>${faltan}</b><small>${faltan === 1 ? "día para cerrar" : "días para cerrar"}</small></div></div>
    ${uso != null ? `<div class="rv-rec-barra fb-pres${uso > 1 ? " pasado" : uso > 0.8 ? " cerca" : ""}" role="img" aria-label="Gastaste ${Math.round(uso * 100)} % del presupuesto"><i style="width:${Math.min(100, uso * 100)}%"></i></div>
      <small class="rv-rec-pct">${Math.round(uso * 100)} % del presupuesto gastado</small>`
      : `<button class="fb-sin-pres" data-sel="m:${ym}">Ponle un presupuesto al mes</button>`}
    <small class="fb-racha">${ic("ruta")} ${racha ? (racha === 1 ? "1 día anotando" : `${racha} días seguidos anotando`) : "Anota hoy y empieza tu racha"}</small>
    <p class="rv-rec-frase">${frase}</p></section>`;
}

function fbBarra() {
  const hoy = hoyFecha(), aHoy = hoy.getFullYear(), mHoy = hoy.getMonth() + 1, l = fbLugar(), ver = fbVer(), a = ver.a, m = ver.m;
  const v = String(estado.sel || ""), arriba = fbArriba(), datos = fbDatos();
  const ATAJOS = [["b", "plata", "Balance general"], ["fijos", "repetir", "Mis cuentas fijas"], ["deudas", "deuda", "Deudas"], ["metasfin", "alcancia", "Metas financieras"],
    ["salud", "salud", "Salud financiera"], ["grafica", "grafica", "Gráfica del ascenso"], ["patrimonio", "balanza", "Activos y pasivos"]];
  const donde = l.d ? `${l.d} ${MES_CORTO[l.m - 1]} ${l.a}` : l.m ? `${mayus(MESES[l.m - 1])} ${l.a}` : l.a ? String(l.a) : ATAJOS.find(x => x[0] === v)?.[2] || "Ascenso financiero";
  let meses = "";
  for (let k = 1; k <= 12; k++) {
    const ym = `${a}-${pad2(k)}`, t = datos.porMes[ym], aro = fbAro(t, ym, a === aHoy && k === mHoy);
    meses += `<button class="rv-mes${l.a === a && l.m === k ? " abierto" : ""}${k === m ? " viendo" : ""}" data-sel="m:${ym}" aria-label="${MESES[k - 1]} de ${a}: ${aro.titulo}" title="${mayus(MESES[k - 1])} · ${aro.titulo}${t?.n ? ` · disponible ${plata(t.disp)}` : ""}">${aro.svg}<span>${MES_CORTO[k - 1]}</span></button>`;
  }
  const ini = (new Date(a, m - 1, 1).getDay() + 6) % 7;
  let dias = ["L", "M", "X", "J", "V", "S", "D"].map(x => `<b class="rv-sem" aria-hidden="true">${x}</b>`).join("") + `<i class="rv-vacio"></i>`.repeat(ini);
  for (let d = 1; d <= diasEnMes(a, m); d++) {
    const f = `${a}-${pad2(m)}-${pad2(d)}`, x = datos.porDia[f], esHoy = f === iso(hoy), tipos = x ? ["ing", "gas", "aho", "inv", "gen"].filter(t => x.tipos.has(t)) : [];
    dias += `<button class="rv-dia fb-dia${l.a === a && l.m === m && l.d === d ? " abierto" : ""}${esHoy ? " hoy" : ""}${x ? " anotado" : ""}" data-sel="d:${f}" aria-label="${d} de ${MESES[m - 1]}${x ? `: ${x.s.n} movimientos, disponible ${plata(x.s.disp)}` : ""}">
      <b>${d}</b>${tipos.length ? `<span class="fb-puntos">${tipos.map(t => `<i style="background:${FB_COLOR[t]}"></i>`).join("")}</span>` : ""}</button>`;
  }
  const nav = (sel, i, txt) => `<button class="rv-nav-item" data-sel="${sel}" aria-pressed="${v === sel}">${ic(i)}<span>${txt}</span></button>`;

  const panel = fbPanel();
  const btn = (attrs, i, t, on) => `<button class="fb-r" ${attrs}${on != null ? ` aria-pressed="${on}"` : ""} aria-label="${t}" data-tip="${t}">${ic(i)}</button>`;
  const riel = `<div class="fb-riel" role="toolbar" aria-label="Ascenso financiero" aria-orientation="vertical">
      ${btn(arriba ? `data-sel="${arriba}"` : "disabled", "volver", "Subir un nivel (Esc)")}
      ${btn('data-acc="fbPestana" data-arg="cal"', "calendario", "Calendario", panel === "cal")}
      ${btn('data-acc="fbPestana" data-arg="resumen"', "lista", "Resumen del mes", panel === "resumen")}
      <hr class="fb-sep" aria-hidden="true">
      ${ATAJOS.map(([sel, i, t]) => btn(`data-sel="${sel}"`, i, t, v === sel)).join("")}
      ${btn('data-acc="ascInforme"', "pdf", "Informe PDF")}
    </div>`;
  const cal = `<div class="rv-paso"><button class="rv-flecha" data-acc="fbAnio" data-arg="-1" aria-label="Año anterior">${ic("izq")}</button>
        <button class="rv-titulo${l.a === a && !l.m ? " abierto" : ""}" data-acc="fbMapa" aria-expanded="${!!estado.fbMapa}" title="Ver todos tus años">${a}<small>${datos.porAnio[a] ? `${plata(datos.porAnio[a].disp)} libre` : "sin movimientos"} ${ic(estado.fbMapa ? "arriba" : "abajo")}</small></button>
        <button class="rv-flecha" data-acc="fbAnio" data-arg="1" aria-label="Año siguiente" ${a >= aHoy + 1 ? "disabled" : ""}>${ic("der")}</button></div>
      ${estado.fbMapa ? fbAnios(datos, a) : ""}
      <div class="rv-meses" role="group" aria-label="Meses de ${a}">${meses}</div>
      <div class="rv-paso rv-paso-mes"><button class="rv-flecha" data-acc="fbMes" data-arg="-1" aria-label="Mes anterior">${ic("izq")}</button>
        <button class="rv-titulo${l.a === a && l.m === m && !l.d ? " abierto" : ""}" data-sel="m:${a}-${pad2(m)}" title="Abrir ${MESES[m - 1]}">${mayus(MESES[m - 1])}</button>
        <button class="rv-flecha" data-acc="fbMes" data-arg="1" aria-label="Mes siguiente">${ic("der")}</button></div>
      <div class="rv-cal" role="group" aria-label="Días de ${MESES[m - 1]} de ${a}">${dias}</div>
      <p class="fb-ley" aria-hidden="true">${[["ing", "Ingreso"], ["gas", "Gasto"], ["aho", "Ahorro"], ["inv", "Inversión"], ["gen", "Dar"]].map(([t, n]) => `<span><i style="background:${FB_COLOR[t]}"></i>${n}</span>`).join("")}</p>`;
  const hoja = panel ? `<div class="fb-hoja" role="region" aria-label="${panel === "cal" ? "Calendario" : "Resumen del mes"}">
      <div class="fb-hoja-cab"><b class="rv-donde">${panel === "cal" ? donde : "Resumen"}</b>
        ${panel === "cal" ? `<button class="rv-hoy" data-sel="d:${iso(hoy)}" title="Ir a hoy y registrar">Hoy</button>` : ""}
        <button class="rv-atras" data-acc="fbPestana" data-arg="${panel}" aria-label="Cerrar" title="Cerrar">${ic("cerrar")}</button></div>
      <div class="fb-panel">${panel === "cal" ? cal : fbTarjeta(datos) + fbCordillera(datos, a)}</div></div>` : "";
  return `<nav class="rv-barra fb-barra${panel ? " con-hoja" : ""}" aria-label="Ascenso financiero: año, mes y día">${riel}${hoja}</nav>`;
}
/** Qué se abre junto al riel: el calendario, el resumen o nada (se recuerda; en el teléfono empieza cerrado). */
function fbPanel() {
  if (estado.fbPanel == null) {
    let p = null; try { p = localStorage.getItem("suroute-fb-panel"); } catch {}
    estado.fbPanel = matchMedia("(max-width: 900px)").matches ? "" : (p === "" || p === "resumen" ? p : "cal");
  }
  return estado.fbPanel;
}
/** Todos tus años en miniatura (igual que Mi ruta): un aro por año de tu vida, una fila por década.
 *  El color dice cómo cerró: verde (sobró o todo tuvo destino), dorado (en cero sin ahorro), burdeos (salió más).
 *  Un toque mira ese año; otro toque lo abre. */
function fbAnios(datos, visto) {
  const nac = perfil().anioNacimiento, aHoy = hoyFecha().getFullYear();
  if (!nac) return fbAniosLista(datos, visto);
  let h = "";
  for (let i = 0; i < metaVida(); i++) {
    const y = nac + i, t = datos.porAnio[y], cls = y < aHoy ? "vivido" : y === aHoy ? "actual" : "futuro";
    const fin = t?.n ? " fin-" + fbEstado(t) : y <= aHoy ? " fin-sin" : "";
    if (i % 10 === 0) h += `${i ? "</div>" : ""}<div class="rv-dec"><small>${i}</small>`;
    h += `<button class="rv-mini${y === visto ? " viendo" : ""}" data-acc="fbAnioIr" data-arg="${y}" aria-label="${y}, ${i} años${t?.n ? `: disponible ${plata(t.disp)}` : ", sin movimientos"}" title="${y} · ${i} años${t?.n ? ` · disponible ${plata(t.disp)} · construido ${plata(t.construido)}` : " · sin movimientos"}">
      <span class="pv ${cls}${fin}" aria-hidden="true">${PUNTO_MARCA}</span></button>`;
  }
  return `<div class="rv-mapa fb-mapa" role="group" aria-label="Todos tus años">${h}</div>
    <p class="fb-mapa-ley"><span><i class="bien"></i>Sobró o todo tuvo destino</span><span><i class="justo"></i>En cero sin ahorro</span><span><i class="mal"></i>Salió más</span></p>
    <p class="rv-mapa-ayuda">Toca un año para verlo; tócalo otra vez para abrirlo. <button class="fb-link" data-sel="b">Ver el balance de todos</button></p></div>`;
}
/** Sin año de nacimiento: los años con movimientos, en tarjetas. */
function fbAniosLista(datos, visto) {
  const aHoy = hoyFecha().getFullYear(), conMovs = Object.keys(datos.porAnio).map(Number);
  const desde = Math.min(aHoy, ...conMovs), anios = [];
  for (let y = aHoy + 1; y >= desde; y--) anios.push(y);
  return `<div class="fb-anios" role="group" aria-label="Todos tus años">${anios.map(y => { const t = datos.porAnio[y];
    return `<button class="fb-anio${y === visto ? " viendo" : ""}${y === aHoy ? " actual" : ""}${t?.n ? " " + fbEstado(t) : ""}" data-acc="fbAnioIr" data-arg="${y}" aria-label="${y}${t?.n ? `: disponible ${plata(t.disp)}` : ", sin movimientos"}">
      <b>${y}</b><small>${t?.n ? plata(t.disp) : "—"}</small></button>`; }).join("")}
    <p class="rv-mapa-ayuda">Toca un año para verlo; tócalo otra vez para abrirlo. <button class="fb-link" data-sel="b">Ver el balance de todos</button></p></div>`;
}
ACC.fbPestana = (_, k) => { estado.fbPanel = fbPanel() === k ? "" : k; try { localStorage.setItem("suroute-fb-panel", estado.fbPanel); } catch {} fbRepintar(); };
ACC.fbMapa = () => { estado.fbMapa = !estado.fbMapa; fbRepintar(); };
ACC.fbAnioIr = (_, y) => { const ver = fbVer(), n = Number(y); if (ver.a === n) { estado.fbMapa = false; elegir("a:" + n); return; } ver.a = n; fbRepintar(); };
function fbRepintar() { const b = document.querySelector(".fb-barra"); if (b) b.outerHTML = fbBarra(); $("detalle")?.classList.toggle("fb-abierta", !!fbPanel()); }
ACC.fbAnio = (_, d) => { const ver = fbVer(); ver.a = Math.min(hoyFecha().getFullYear() + 1, ver.a + Number(d)); fbRepintar(); };
ACC.fbMes = (_, d) => { const ver = fbVer(); ver.m += Number(d); if (ver.m < 1) { ver.m = 12; ver.a--; } if (ver.m > 12) { ver.m = 1; ver.a++; } fbRepintar(); };
ACC.fbPlegar = () => { estado.fbAbierta = !estado.fbAbierta; fbRepintar(); };

/* Mis cuentas fijas, a un toque desde la barra */
DET["finanzas.fijos"] = () => cab(avAsc(), "Mis cuentas fijas", "Lo que se repite cada mes: arriendo, salario, internet…", ojoAsc()) +
  cuerpo((window.fijosHoja ? fijosHoja("") : hoja(`<p class="suave">Aún no hay cuentas fijas.</p>`)), "max-width:980px");

/* Pintar: en el Ascenso financiero, el contenido al centro y la barra a la derecha (arriba y plegable en el teléfono) */
{
  const base = pintarDetalle;
  pintarDetalle = function (...args) {
    const enFin = estado.sec === "finanzas" && !window.finBloqueado?.();
    if (enFin && estado.sel == null) { estado.sel = "m:" + iso(hoyFecha()).slice(0, 7); document.getElementById("app")?.classList.add("con-detalle"); }
    const r = base.apply(this, args), det = $("detalle");
    if (!enFin) { det.classList.remove("con-fin", "fb-abierta"); return r; }
    document.getElementById("app")?.classList.add("ruta-vida");
    det.classList.add("con-vida", "con-fin"); det.classList.toggle("fb-abierta", !!fbPanel());
    const l = fbLugar(); if (l.a && /^[amd]:/.test(String(estado.sel || ""))) estado.fbUltimo = l;
    det.insertAdjacentHTML("beforeend", fbBarra());
    return r;
  };
}
/* Teclado: Esc sube un nivel; en un día, ← → cambian de día (si no estás escribiendo) */
document.addEventListener("keydown", e => {
  if (estado.sec !== "finanzas" || window.finBloqueado?.() || /INPUT|TEXTAREA|SELECT/.test(document.activeElement?.tagName || "") || e.altKey || e.ctrlKey || e.metaKey || document.querySelector(".hoja-modal, dialog[open]")) return;
  if (e.key === "Escape") { const arriba = fbArriba(); if (arriba) { e.preventDefault(); elegir(arriba); } return; }
  const v = String(estado.sel || ""), paso = { ArrowLeft: -1, ArrowRight: 1 }[e.key];
  if (!paso || !v.startsWith("d:")) return;
  e.preventDefault(); const f = deIso(v.slice(2)); f.setDate(f.getDate() + paso); elegir("d:" + iso(f));
});
