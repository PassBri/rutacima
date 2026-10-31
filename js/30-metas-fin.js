"use strict";
/* ======================================================================
 * Metas financieras (igual que la app, domain/model/MetasFinancieras.kt).
 * Lo que la persona quiere lograr con su dinero: fondo de emergencia, viaje, casa, estudio, vehículo,
 * retiro, una compra… Como las «metas» de YNAB, los «bolsillos» de los bancos o las «cajitas»: cada meta
 * tiene cuánto, para cuándo y cuánto llevas; la app dice cuánto aportar al mes y si vas a tiempo.
 * Se guardan en "fin#metas": [{ id, tipo, nombre, objetivo, fecha: "aaaa-mm" | "", inicial, creada: "aaaa-mm" }]
 * Lo ahorrado = lo que ya tenía (inicial) + los movimientos del libro que nombran la meta (meta = id):
 * un aporte es ahorro (o inversión) y un retiro es un ahorro negativo.
 * ====================================================================== */
const TIPOS_META = [
  ["emergencia", "Fondo de emergencia", "escudo", "emergencia", "De 3 a 6 meses de tus gastos fijos, para los imprevistos."],
  ["viaje", "Viaje", "avion", "meta", "El viaje que quieres hacer, sin deudas."],
  ["vivienda", "Casa o cuota inicial", "casa", "meta", "Tu casa propia o la cuota inicial."],
  ["educacion", "Educación", "escuela", "meta", "Un estudio para ti o para tu familia."],
  ["vehiculo", "Vehículo", "carro", "meta", "Carro, moto o bicicleta."],
  ["retiro", "Retiro", "playa", "retiro", "Tu pensión voluntaria o tu libertad financiera."],
  ["compra", "Una compra", "bolsa", "meta", "Algo que quieres comprar de contado."],
  ["otra", "Otra meta", "bandera", "meta", "Lo que tú quieras lograr."],
];
const tipoMeta = t => TIPOS_META.find(x => x[0] === t) || TIPOS_META.at(-1);
function metasFinDatos() { try { const l = JSON.parse(respuesta("fin#metas") || "[]"); return Array.isArray(l) ? l : []; } catch { return []; } }
const guardarMetasFin = l => responder(ASC.WB, "fin#metas", l.length ? JSON.stringify(l) : "");
const ymHoy = () => iso(hoyFecha()).slice(0, 7);
const mesesEntre = (a, b) => (Number(b.slice(0, 4)) - Number(a.slice(0, 4))) * 12 + Number(b.slice(5, 7)) - Number(a.slice(5, 7));
const nombreYm = ym => `${MESES[Number(ym.slice(5, 7)) - 1]} de ${ym.slice(0, 4)}`;

/** Los movimientos del libro que van a cada meta. */
function aportesMetas() {
  const r = {};
  diasConMovs(true).forEach(f => movsGuardados(f).forEach(x => { if (x.meta) (r[x.meta] ||= []).push([f, x]); }));
  return r;
}
/** Cada meta con lo ahorrado, lo que falta, el aporte al mes para llegar a tiempo y si va a tiempo. */
function metasFinHoy() {
  const ap = aportesMetas(), hoyYm = ymHoy();
  return metasFinDatos().map(m => {
    const movs = ap[m.id] || [], aportado = movs.reduce((a, [, x]) => a + (Number(x.m) || 0), 0);
    const ahorrado = Math.max(0, (Number(m.inicial) || 0) + aportado), objetivo = Number(m.objetivo) || 0;
    const falta = Math.max(0, objetivo - ahorrado), pct = objetivo ? Math.min(1, ahorrado / objetivo) : 0;
    const meses = m.fecha ? mesesEntre(hoyYm, m.fecha) + 1 : null;              // contando este mes
    const aporteMes = falta && meses ? falta / Math.max(1, meses) : 0;
    // Ritmo: lo aportado en los últimos 3 meses, por mes
    const desde = (() => { const d = hoyFecha(); d.setDate(1); d.setMonth(d.getMonth() - 2); return iso(d).slice(0, 7); })();
    const ritmo = movs.filter(([f]) => f.slice(0, 7) >= desde && f.slice(0, 7) <= hoyYm).reduce((a, [, x]) => a + (Number(x.m) || 0), 0) / 3;
    // Va a tiempo si lo ahorrado alcanza lo esperado para este punto del camino (desde que se creó)
    const total = m.fecha && m.creada ? Math.max(1, mesesEntre(m.creada, m.fecha) + 1) : null, pasados = m.creada ? Math.max(0, mesesEntre(m.creada, hoyYm)) : 0;
    const esperado = total ? objetivo * Math.min(1, pasados / total) : 0;
    const estado = pct >= 1 ? "lograda" : meses != null && meses <= 0 ? "vencida" : !m.fecha ? "sinfecha" : ahorrado + 0.5 >= esperado * 0.95 ? "atiempo" : "atrasada";
    const llegaEn = falta && ritmo > 0 ? Math.ceil(falta / ritmo) : null;
    return { ...m, ahorrado, objetivo, falta, pct, meses, aporteMes, ritmo, estado, llegaEn, nAportes: movs.length };
  });
}
/** Para la lista de Ascenso financiero y el balance. */
window.metasFinResumen = () => { const l = metasFinHoy(), obj = l.reduce((a, x) => a + x.objetivo, 0), aho = l.reduce((a, x) => a + Math.min(x.ahorrado, x.objetivo), 0);
  return { n: l.length, pct: obj ? aho / obj : 0, lista: l }; };
const EST_META = { lograda: ["¡Lograda!", "ok"], atiempo: ["Vas a tiempo", "ok"], atrasada: ["Atrasada", "medio"], vencida: ["Se pasó la fecha", "bajo"], sinfecha: ["Sin fecha", "na"] };

/** El fondo de emergencia sugerido: 6 meses de gastos fijos (mínimo 3). */
function fondoSugerido() { const r = saludDe(""); const fijos = (r.filas?.[3]?.[1] || 0) / (r.nMeses || 1); return { fijos, tres: Math.round(fijos * 3), seis: Math.round(fijos * 6) }; }

DET["finanzas.metasfin"] = () => {
  if (window.finBloqueado?.()) return pantallaBloqueo();
  const l = metasFinHoy(), obj = l.reduce((a, x) => a + x.objetivo, 0), aho = l.reduce((a, x) => a + Math.min(x.ahorrado, x.objetivo), 0);
  const mes = l.filter(x => x.estado !== "lograda").reduce((a, x) => a + x.aporteMes, 0), fs = fondoSugerido();
  const sinFondo = !l.some(x => x.tipo === "emergencia") && fs.fijos > 0;
  const tarjeta = x => { const t = tipoMeta(x.tipo), [et, ec] = EST_META[x.estado], L = 2 * Math.PI * 26;
    return `<article class="meta-fin e-${ec}">
      <header><span class="mf-ic">${ic(t[2])}</span><div class="mf-nombre"><input value="${esc(x.nombre)}" maxlength="60" data-acc-change="metaFinCampo" data-arg="${x.id}|nombre" aria-label="Nombre de la meta"><small>${t[1]}</small></div>
        <span class="mf-estado">${et}</span></header>
      <div class="mf-avance"><svg viewBox="0 0 64 64" class="mf-anillo" aria-hidden="true"><circle cx="32" cy="32" r="26"/>${x.pct > 0 ? `<circle cx="32" cy="32" r="26" class="v" style="stroke-dasharray:${L * x.pct} ${L}"/>` : ""}</svg>
        <div><b>${Math.round(x.pct * 100)} %</b><span>${plata(x.ahorrado)} de ${plata(x.objetivo)}</span><small>${x.falta ? `Faltan ${plata(x.falta)}` : "¡Completaste tu meta!"}</small></div></div>
      ${x.estado === "lograda" ? "" : `<div class="mf-plan">${x.fecha ? (x.meses > 0 ? `<div><span>Aporta al mes</span><b>${plata(Math.ceil(x.aporteMes))}</b><small>${x.meses} ${x.meses === 1 ? "mes" : "meses"} para ${nombreYm(x.fecha)}</small></div>` : `<div><span>La fecha ya pasó</span><b>${nombreYm(x.fecha)}</b><small>Ponle una fecha nueva</small></div>`) : `<div><span>Sin fecha</span><b>—</b><small>Ponle una fecha y te digo cuánto aportar</small></div>`}
        <div><span>Tu ritmo</span><b>${x.ritmo > 0 ? plata(Math.round(x.ritmo)) + "/mes" : "—"}</b><small>${x.llegaEn ? `A este ritmo la logras en ${nombreYm(ymMas(ymHoy(), x.llegaEn - 1))}` : "Aún sin aportes recientes"}</small></div></div>`}
      <div class="mf-campos"><label><span>Meta</span><input class="num" inputmode="decimal" value="${esc(numAsc(x.objetivo))}" data-acc-change="metaFinCampo" data-arg="${x.id}|objetivo" aria-label="Cuánto quieres reunir"></label>
        <label><span>Para cuándo</span><input type="month" value="${esc(x.fecha || "")}" min="${ymHoy()}" data-acc-change="metaFinCampo" data-arg="${x.id}|fecha" aria-label="Para cuándo"></label>
        <div class="mf-acciones"><button class="rb" data-acc="metaFinQuitar" data-arg="${x.id}" aria-label="Quitar la meta ${esc(x.nombre)}" title="Quitar">${ic("borrar")}</button>
          ${x.ahorrado > 0 ? `<button class="btn mini" data-acc="metaFinMover" data-arg="${x.id}|retirar">Retirar</button>` : ""}<button class="btn mini lleno" data-acc="metaFinMover" data-arg="${x.id}|aportar">${ic("mas")} Aportar</button></div></div>
    </article>`; };
  return cab(av(ic("alcancia"), "var(--ok)"), "Metas financieras", "Ascenso financiero · lo que quieres lograr con tu dinero", ojoAsc()) + cuerpo(
    migasAsc([["Balance general", "b"], ["Metas financieras", ""]]) +
    (l.length ? `<div class="asc-cifras"><div class="asc-cifra c-aho"><span>Ahorrado para tus metas</span><b>${plata(aho)}</b></div><div class="asc-cifra c-ing"><span>Lo que quieres reunir</span><b>${plata(obj)}</b></div>
      <div class="asc-cifra c-gas"><span>Te falta</span><b>${plata(Math.max(0, obj - aho))}</b></div><div class="asc-cifra c-disp"><span>Aporta al mes</span><b>${plata(Math.ceil(mes))}</b></div></div>` : "") +
    (sinFondo ? hoja(`<div class="mf-sugerencia">${ic("escudo")}<div><b>Empieza por tu fondo de emergencia</b><p class="suave">Tus gastos fijos son cerca de ${plata(Math.round(fs.fijos))} al mes. Lo ideal es tener de 3 a 6 meses guardados: entre ${plata(fs.tres)} y ${plata(fs.seis)}. Con él, un imprevisto no se vuelve deuda.</p></div>
      <button class="btn lleno" data-acc="metaFinFondo">${ic("mas")} Crear mi fondo</button></div>`) : "") +
    (l.length ? `<div class="metas-fin">${l.map(tarjeta).join("")}</div>` : hoja(`<div class="mf-vacio">${ic("alcancia", "i grande")}<h3>¿Qué quieres lograr con tu dinero?</h3><p class="suave">Una meta clara es la mitad del camino. Escoge una, di cuánto y para cuándo: te diré cuánto aportar cada mes y si vas a tiempo.</p></div>`)) +
    hoja(`<h3 style="margin:0 0 4px">Nueva meta</h3><p class="suave" style="margin:0 0 12px">Escoge el tipo y escribe cuánto quieres reunir. La fecha es opcional, pero con ella sabrás cuánto aportar al mes.</p>
      <div class="mf-tipos" role="radiogroup" aria-label="Tipo de meta">${TIPOS_META.map(([k, n, i, , d], j) => `<button type="button" role="radio" aria-checked="${(estado.metaTipo || "viaje") === k}" class="mf-tipo" data-acc="metaFinTipo" data-arg="${k}" aria-label="${n}">${ic(i)}</button>`).join("")}</div>
      <form class="asc-nuevo" data-form="metaFinNueva" style="margin-top:12px"><div class="asc-fila-nueva">
        <label class="campo"><span>Nombre</span><input name="texto" maxlength="60" autocomplete="off" placeholder="${esc(tipoMeta(estado.metaTipo || "viaje")[1])}"></label>
        <label class="campo"><span>Cuánto quieres reunir</span><input name="objetivo" inputmode="decimal" autocomplete="off" placeholder="${plata(0)}" required></label>
        <label class="campo"><span>Para cuándo</span><input name="fecha" type="month" min="${ymHoy()}"></label>
        <label class="campo"><span>Ya tengo ahorrado</span><input name="inicial" inputmode="decimal" autocomplete="off" placeholder="${plata(0)}"></label>
        <button class="btn lleno" type="submit">Crear meta</button></div></form>`) +
    `<p class="suave" style="font-size:13px">Cada aporte se anota hoy en tu libro como ahorro (e indica la meta); un retiro lo devuelve a tu disponible. En el día también puedes decir a qué meta va un ahorro que ya anotaste.</p>`, "max-width:980px");
};

Object.assign(ACC, {
  metaFinTipo(_, k) { estado.metaTipo = k; pintarDetalle(); document.querySelector('[data-form="metaFinNueva"] [name="objetivo"]')?.focus(); },
  metaFinFondo() { const fs = fondoSugerido(), l = metasFinDatos();
    l.push({ id: "g" + nuevoId(), tipo: "emergencia", nombre: "Fondo de emergencia", objetivo: fs.seis || fs.tres, fecha: ymMas(ymHoy(), 11), inicial: 0, creada: ymHoy() });
    guardarMetasFin(l); toast("Tu fondo de emergencia: 6 meses de gastos fijos en un año. Cámbialo cuando quieras."); pintarDetalle(); },
  metaFinCampo(el, arg) {
    const [id, k] = arg.split("|"), l = metasFinDatos(), x = l.find(y => y.id === id); if (!x) return;
    if (k === "objetivo") { const v = Math.abs(montoFin(el.value)); if (!v) { toast("Escribe cuánto quieres reunir"); pintarDetalle(); return; } x.objetivo = v; }
    else if (k === "nombre") { if (!el.value.trim()) { pintarDetalle(); return; } x.nombre = el.value.trim().slice(0, 60); }
    else if (k === "fecha") x.fecha = /^\d{4}-\d{2}$/.test(el.value) ? el.value : "";
    guardarMetasFin(l); pintarDetalle();
  },
  metaFinQuitar(_, id) { const l = metasFinDatos(), x = l.find(y => y.id === id); if (!x) return;
    guardarMetasFin(l.filter(y => y.id !== id)); toast(`Meta «${x.nombre}» quitada; sus aportes siguen en tu libro`, { accion: "Deshacer", alHacer: () => guardarMetasFin([...metasFinDatos(), x]) }); pintarDetalle(); },
  metaFinMover(_, arg) {
    const [id, que] = arg.split("|"), x = metasFinHoy().find(y => y.id === id); if (!x) return;
    const retirar = que === "retirar", sug = retirar ? x.ahorrado : (x.aporteMes ? Math.ceil(x.aporteMes) : 0);
    const v = ventanaCuenta(`<h3 style="margin-top:0">${retirar ? "Retirar de" : "Aportar a"} ${esc(x.nombre)}</h3>
      <p class="suave" style="margin-top:0">Llevas ${plata(x.ahorrado)} de ${plata(x.objetivo)}.${!retirar && x.aporteMes ? ` Para llegar a tiempo aporta ${plata(Math.ceil(x.aporteMes))} al mes.` : ""}</p>
      <form class="form" id="metaMov">${campo(retirar ? "Cuánto retiras" : "Cuánto aportas", `<input name="monto" inputmode="decimal" value="${sug ? esc(numAsc(sug)) : ""}" required>`)}
      <p class="suave" style="font-size:12.5px">${retirar ? "Se anota hoy en tu libro y vuelve a tu disponible." : "Se anota hoy en tu libro como ahorro para esta meta."}</p>
      <div class="botones"><button class="btn" type="button" data-cerrar>Cancelar</button><button class="btn lleno" type="submit">${retirar ? "Retirar" : "Aportar"}</button></div></form>`, w => {
      w.querySelector("[data-cerrar]").onclick = () => w.remove();
      w.querySelector("#metaMov").onsubmit = e => {
        e.preventDefault(); let m = Math.abs(montoFin(new FormData(e.target).get("monto"))); if (!m) return;
        if (retirar) m = Math.min(m, x.ahorrado);
        const t = tipoMeta(x.tipo), f = iso(hoyFecha());
        guardarMovs(f, [...movsGuardados(f), { id: "m" + nuevoId(), t: "aho", m: retirar ? -m : m, c: t[3], n: `${retirar ? "Retiro de" : "Aporte a"} ${x.nombre}`, meta: x.id }]);
        w.remove();
        const ya = x.ahorrado + (retirar ? -m : m);
        toast(retirar ? `Retiraste ${plata(m)} de ${x.nombre}` : ya >= x.objetivo ? `¡Lograste tu meta ${x.nombre}!` : `Aportaste ${plata(m)}: llevas ${Math.round(Math.min(1, ya / x.objetivo) * 100)} %`);
        pintarDetalle(); pintarLista();
      };
    });
  },
  /** En el día: a qué meta va un ahorro o una inversión ya anotados. */
  ascMetaDe(el, arg) { cambiarMov(arg, y => { if (el.value) y.meta = el.value; else delete y.meta; }); toast(el.value ? `Este ahorro va a ${metasFinDatos().find(m => m.id === el.value)?.nombre}` : "Sin meta"); pintarDetalle(); },
});
window.FORMULARIOS = Object.assign(window.FORMULARIOS || {}, {
  metaFinNueva(form, datos, texto) {
    const objetivo = Math.abs(montoFin(datos.get("objetivo"))); if (!objetivo) { toast("Escribe cuánto quieres reunir"); form.querySelector('[name="objetivo"]').focus(); return; }
    const tipo = estado.metaTipo || "viaje", fecha = String(datos.get("fecha") || "");
    const l = metasFinDatos(); l.push({ id: "g" + nuevoId(), tipo, nombre: (texto || tipoMeta(tipo)[1]).slice(0, 60), objetivo, fecha: /^\d{4}-\d{2}$/.test(fecha) ? fecha : "", inicial: Math.abs(montoFin(datos.get("inicial"))) || 0, creada: ymHoy() });
    guardarMetasFin(l); form.reset(); toast("Meta creada"); pintarDetalle(); pintarLista();
  },
});
