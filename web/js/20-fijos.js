"use strict";
/* ======================================================================
 * Movimientos fijos (igual que la app, domain/model/Fijos.kt).
 * Un gasto, ingreso, ahorro o inversión que se repite cada mes el mismo día: arriendo, salario,
 * internet, el aporte al fondo… Se guardan en "fin#fijos" (workbook "finanzas"):
 *   [{ id, t, m, c, n, dia, desde: "aaaa-mm", hasta: "aaaa-mm" | "", saltar: ["aaaa-mm"] }]
 * Cada mes aparecen solos en su día (sin pasar de hoy), sin escribirlos de nuevo.
 * · Si cambias uno en un día, ese mes queda como movimiento propio (con fx = id del fijo).
 * · Si lo quitas de un día, ese mes se salta.
 * · Si cambias el monto del fijo, rige desde este mes; los meses anteriores quedan como estaban.
 * ====================================================================== */
const FIJOS = "fin#fijos";
function fijosDatos() {
  try { const v = JSON.parse(respuesta(FIJOS) || "[]"); return Array.isArray(v) ? v.filter(x => x && ASC.CAT[x.t] && Number(x.m) > 0 && /^\d{4}-\d{2}$/.test(x.desde || "")) : []; } catch { return []; }
}
const guardarFijos = l => responder(ASC.WB, FIJOS, l.length ? JSON.stringify(l) : "");
const ymMas = (ym, k) => { const [a, m] = ym.split("-").map(Number), d = new Date(a, m - 1 + k, 1); return ymDe(d.getFullYear(), d.getMonth() + 1); };
const diasDelMes = ym => { const [a, m] = ym.split("-").map(Number); return new Date(a, m, 0).getDate(); };
/** El día del fijo en ese mes (el 31 cae el último día de los meses cortos). */
const fechaFijo = (x, ym) => `${ym}-${String(Math.min(Number(x.dia) || 1, diasDelMes(ym))).padStart(2, "0")}`;
const activoEn = (x, ym) => ym >= x.desde && (!x.hasta || ym <= x.hasta) && !(x.saltar || []).includes(ym);

/** Los fijos que caen ese día (como movimientos virtuales). */
window.fijosEn = f => {
  if (f > iso(hoyFecha())) return [];
  const ym = f.slice(0, 7);
  return fijosDatos().filter(x => activoEn(x, ym) && fechaFijo(x, ym) === f)
    .map(x => ({ id: "fx-" + x.id, t: x.t, m: Number(x.m), c: x.c, n: x.n || "", fx: x.id, virtual: true }));
};
/** Todas las fechas en que aparece algún fijo, hasta hoy. */
window.fechasFijos = () => {
  const hoyI = iso(hoyFecha()), hoyYm = hoyI.slice(0, 7), r = [];
  fijosDatos().forEach(x => {
    const fin = x.hasta && x.hasta < hoyYm ? x.hasta : hoyYm;
    for (let ym = x.desde, n = 0; ym <= fin && n < 600; ym = ymMas(ym, 1), n++) { if (!activoEn(x, ym)) continue; const f = fechaFijo(x, ym); if (f <= hoyI) r.push(f); }
  });
  return r;
};
/** Al quitar un movimiento que vino de un fijo: ese mes se salta. */
window.saltarFijo = (fx, ym, si = true) => {
  const l = fijosDatos(), x = l.find(y => y.id === fx); if (!x) return;
  x.saltar = si ? [...new Set([...(x.saltar || []), ym])] : (x.saltar || []).filter(y => y !== ym);
  guardarFijos(l);
};

/** La hoja de los fijos: qué se repite, cuánto, qué día; se edita aquí y rige desde este mes. */
function fijosHoja(ym) {
  const hoyYm = iso(hoyFecha()).slice(0, 7), base = ym || hoyYm;
  const l = fijosDatos().filter(x => (!x.hasta || x.hasta >= base) && x.desde <= (ym ? ym : "9999-12"));
  const totalT = t => l.filter(x => x.t === t).reduce((a, x) => a + Number(x.m), 0);
  return hoja(`<div class="cab-lista-btn"><h3 style="margin:0">${ic("repetir")} Fijos de cada mes</h3><span class="suave" style="font-size:13px">Aparecen solos cada mes en su día</span></div>
    ${l.length ? `<div class="asc-tabla"><table class="asc-movs asc-fijos"><thead><tr><th>Tipo</th><th>Concepto</th><th>Día</th><th class="num">Monto</th><th><span class="oculto">Dejar de repetir</span></th></tr></thead><tbody>
      ${l.map(x => `<tr class="t-${x.t}"><td><span class="pildora t-${x.t}">${ASC.TIPOS.find(y => y[0] === x.t)[2]}</span></td>
        <td><input value="${esc(x.n || catDe(x.t, x.c)?.[1] || "")}" data-acc-change="fijoNota" data-arg="${x.id}" maxlength="120" aria-label="Concepto"><small class="suave">${esc(catDe(x.t, x.c)?.[1] || "")} · desde ${mesCorto(x.desde)}</small></td>
        <td><input class="num" type="number" min="1" max="31" value="${x.dia}" data-acc-change="fijoDia" data-arg="${x.id}" aria-label="Día del mes" style="width:64px"></td>
        <td class="num"><input class="num" inputmode="decimal" value="${esc(numAsc(Number(x.m)))}" data-acc-change="fijoMonto" data-arg="${x.id}" aria-label="Monto (rige desde este mes)"></td>
        <td class="acc-mov"><button class="rb" data-acc="fijoParar" data-arg="${x.id}" aria-label="Dejar de repetir" title="Dejar de repetir">${ic("cerrar")}</button></td></tr>`).join("")}
      </tbody><tfoot><tr><th colspan="3" scope="row">Cada mes</th><td class="num">${["ing", "gas", "aho", "inv"].filter(t => totalT(t)).map(t => `<span class="pildora t-${t}">${plata(totalT(t))}</span>`).join(" ")}</td><td></td></tr></tfoot></table></div>
      <p class="suave" style="margin:8px 0 0;font-size:13px">Si cambias el monto, rige desde este mes. Para cambiar solo un mes, edítalo en su día.</p>`
    : `<p class="suave" style="margin:6px 0 0">¿Pagas arriendo, internet o recibes un salario fijo? Al anotarlo en un día marca <b>Se repite cada mes</b> y aparecerá solo cada mes, sin volver a escribirlo.</p>`}`);
}
const mesCorto = ym => `${MES_C[Number(ym.slice(5)) - 1]} ${ym.slice(0, 4)}`;
window.fijosHoja = fijosHoja;

Object.assign(ACC, {
  fijoNota(el, id) { const l = fijosDatos(), x = l.find(y => y.id === id); if (!x) return; x.n = el.value.trim(); guardarFijos(l); },
  fijoDia(el, id) { const l = fijosDatos(), x = l.find(y => y.id === id), d = Math.round(Number(el.value)); if (!x || !(d >= 1 && d <= 31)) { pintarDetalle(); return; } x.dia = d; guardarFijos(l); toast(`Ahora cae el día ${d} de cada mes`); },
  fijoMonto(el, id) {
    const v = Math.abs(montoFin(el.value)), l = fijosDatos(), x = l.find(y => y.id === id); if (!x) return;
    if (!v) { toast("Escribe un monto mayor que cero"); pintarDetalle(); return; }
    const hoyYm = iso(hoyFecha()).slice(0, 7);
    if (x.desde >= hoyYm) x.m = v;
    else {                                              // los meses pasados quedan con el monto de antes
      const nuevo = { ...x, id: "f" + nuevoId(), m: v, desde: hoyYm, saltar: (x.saltar || []).filter(y => y >= hoyYm) };
      x.hasta = ymMas(hoyYm, -1); x.saltar = (x.saltar || []).filter(y => y < hoyYm); l.push(nuevo);
      const f = fechaFijo(x, hoyYm), xs = movsGuardados(f);  // si este mes ya se había cambiado a mano, sigue enlazado
      if (xs.some(y => y.fx === x.id)) guardarMovs(f, xs.map(y => y.fx === x.id ? { ...y, fx: nuevo.id } : y));
    }
    guardarFijos(l); toast(`Desde ${MESES[Number(hoyYm.slice(5)) - 1]}: ${plata(v)} cada mes`);
  },
  fijoParar(_, id) {
    const l = fijosDatos(), x = l.find(y => y.id === id); if (!x) return;
    const antes = { ...x }, hoyYm = iso(hoyFecha()).slice(0, 7);
    if (x.desde > hoyYm) l.splice(l.indexOf(x), 1); else x.hasta = hoyYm;
    guardarFijos(l);
    toast("Ya no se repite desde el próximo mes", { accion: "Deshacer", alHacer: () => { const m = fijosDatos().filter(y => y.id !== id); guardarFijos([...m, antes]); } });
  },
});
