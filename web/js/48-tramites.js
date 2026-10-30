"use strict";
/* ======================================================================
 * Trámites y plazos (igual en la app): un trámite de la alcaldía de 45 días hábiles, un proceso, o una cuota que se paga
 * cada mes. Se une el día de inicio con el día final, se ve en cada día del camino (día 12 de 45, quedan 33) y avisa
 * antes de vencer. Los plazos en días hábiles saltan sábados, domingos y festivos de Colombia (Ley Emiliani), y empiezan
 * a contar el día siguiente al inicio, como los términos de un trámite.
 * Se guarda en el libro "tramites": "tramite#<id>" → JSON {id, n, tipo, ini, dias, habiles, fin, avisos, mensual, nota, listo}.
 * ====================================================================== */
const TR_WB = "tramites";
const TR_TIPOS = { tramite: ["Trámite", "reloj"], pago: ["Pago o cuota", "plata"], proceso: ["Proceso", "balanza"] };

/* ---------- Festivos de Colombia ---------- */
const trFestCache = {};
function festivosCO(a) {
  if (trFestCache[a]) return trFestCache[a];
  const s = new Set(), f = (m, d) => new Date(a, m - 1, d);
  const lunes = d => { const x = new Date(d); while (x.getDay() !== 1) x.setDate(x.getDate() + 1); return x; };
  // Pascua (algoritmo de Meeus)
  const A = a % 19, B = Math.floor(a / 100), C = a % 100, D = Math.floor(B / 4), E = B % 4, F = Math.floor((B + 8) / 25), G = Math.floor((B - F + 1) / 3),
    H = (19 * A + B - D - G + 15) % 30, I = Math.floor(C / 4), K = C % 4, L = (32 + 2 * E + 2 * I - H - K) % 7, M = Math.floor((A + 11 * H + 22 * L) / 451),
    mes = Math.floor((H + L - 7 * M + 114) / 31), dia = ((H + L - 7 * M + 114) % 31) + 1, pascua = new Date(a, mes - 1, dia);
  const mas = n => { const x = new Date(pascua); x.setDate(x.getDate() + n); return x; };
  [f(1, 1), f(5, 1), f(7, 20), f(8, 7), f(12, 8), f(12, 25), mas(-3), mas(-2)].forEach(d => s.add(iso(d)));
  [f(1, 6), f(3, 19), f(6, 29), f(8, 15), f(10, 12), f(11, 1), f(11, 11), mas(39), mas(60), mas(68)].forEach(d => s.add(iso(lunes(d))));
  return trFestCache[a] = s;
}
const esHabil = d => d.getDay() !== 0 && d.getDay() !== 6 && !festivosCO(d.getFullYear()).has(iso(d));
/** El día final: N días hábiles (desde el día siguiente) o N días calendario. */
function trFin(ini, dias, habiles) {
  const d = deIso(ini); if (!dias) return ini;
  if (!habiles) { d.setDate(d.getDate() + Number(dias)); return iso(d); }
  let n = 0; while (n < dias) { d.setDate(d.getDate() + 1); if (esHabil(d)) n++; }
  return iso(d);
}
/** Cuántos días (hábiles o calendario) hay entre dos fechas, sin contar la primera. */
function trContar(desde, hasta, habiles) {
  const a = deIso(desde), b = deIso(hasta); if (b <= a) return 0;
  if (!habiles) return Math.round((b - a) / DIA_MS);
  let n = 0; const d = new Date(a); while (d < b) { d.setDate(d.getDate() + 1); if (esHabil(d)) n++; } return n;
}

/* ---------- Datos ---------- */
function tramites() {
  return Store.lista("respuesta").filter(r => r.clave.startsWith("tramite#") && r.valor)
    .map(r => { try { return JSON.parse(r.valor); } catch { return null; } }).filter(Boolean).sort((x, y) => (x.fin || x.ini).localeCompare(y.fin || y.ini));
}
const trGuardar = t => responder(TR_WB, "tramite#" + t.id, t ? JSON.stringify(t) : "");
/** La fecha de pago de una cuota mensual en un mes dado (el mismo día; si el mes es corto, su último día). */
const trPagoEn = (t, a, m) => { const d = Math.min(Number(t.ini.slice(8)), diasEnMes(a, m)); return `${a}-${pad2(m)}-${pad2(d)}`; };
/** Qué pasa con un trámite en una fecha: inicio, fin, en curso o pago (o nada). */
function trEnDia(t, f) {
  if (t.mensual) { if (f < t.ini) return null; const [a, m] = f.split("-").map(Number); return trPagoEn(t, a, m) === f ? "pago" : null; }
  if (f < t.ini || f > t.fin) return null;
  return f === t.ini ? (f === t.fin ? "fin" : "inicio") : f === t.fin ? "fin" : "curso";
}
/** El próximo vencimiento desde hoy (para avisos). */
function trProximo(t, hoyIso) {
  if (t.listo) return null;
  if (!t.mensual) return t.fin >= hoyIso ? t.fin : t.fin;
  const [a, m] = hoyIso.split("-").map(Number); let p = trPagoEn(t, a, m);
  if (p < hoyIso) { const d = new Date(a, m, 1); p = trPagoEn(t, d.getFullYear(), d.getMonth() + 1); }
  return p < t.ini ? t.ini : p;
}
function trEstado(t, f) {
  const hoyIso = iso(hoyFecha()), u = t.habiles ? "hábiles" : "";
  if (t.listo) return { txt: "Listo", cls: "ok" };
  if (t.mensual) { const p = trProximo(t, hoyIso), q = trContar(hoyIso, p, false);
    return { txt: p === hoyIso ? "Se paga hoy" : `Próximo pago: ${deIso(p).getDate()} ${MES_CORTO[deIso(p).getMonth()]} · en ${q} ${q === 1 ? "día" : "días"}`, cls: q <= 3 ? "pronto" : "" }; }
  if (hoyIso > t.fin) return { txt: `Venció el ${deIso(t.fin).getDate()} ${MES_CORTO[deIso(t.fin).getMonth()]}`, cls: "vencido" };
  const total = t.dias || trContar(t.ini, t.fin, t.habiles), ya = Math.min(total, trContar(t.ini, f, t.habiles)), quedan = trContar(f < t.ini ? t.ini : f, t.fin, t.habiles);
  const rol = trEnDia(t, f);
  return { txt: rol === "inicio" ? `Empieza hoy · ${total} días ${u}` : rol === "fin" ? "Vence hoy" : `Día ${ya} de ${total} ${u} · quedan ${quedan}`, cls: quedan <= 3 ? "pronto" : "", pct: total ? ya / total : 0 };
}

/* ---------- En cada día: la sección «Trámites y plazos» ---------- */
function tramitesDia(fecha) {
  const xs = tramites().filter(t => trEnDia(t, fecha) || (fecha === iso(hoyFecha()) && !t.listo && trAvisoHoy(t)));
  const fila = t => { const e = trEstado(t, fecha), [tn, ti] = TR_TIPOS[t.tipo] || TR_TIPOS.tramite;
    return `<div class="tr-fila${t.listo ? " listo" : ""}"><span class="tr-ic">${ic(ti)}</span>
      <div class="tr-tx"><b>${esc(t.n)}</b><small class="tr-est ${e.cls}">${e.txt}</small>
        ${!t.mensual && e.pct != null && !t.listo ? `<span class="tr-barra" role="img" aria-label="${Math.round(e.pct * 100)} % del plazo"><i style="width:${(e.pct * 100).toFixed(1)}%"></i></span>` : ""}
        <small class="tr-fechas">${t.mensual ? `Cada mes, el día ${Number(t.ini.slice(8))}` : `${deIso(t.ini).getDate()} ${MES_CORTO[deIso(t.ini).getMonth()]} → ${deIso(t.fin).getDate()} ${MES_CORTO[deIso(t.fin).getMonth()]} ${deIso(t.fin).getFullYear()}`}${t.nota ? " · " + esc(t.nota) : ""}</small></div>
      <button class="rb" data-acc="trEditar" data-arg="${esc(t.id)}" aria-label="Editar ${esc(t.n)}" title="Editar">${ic("editar")}</button>
      ${t.mensual ? "" : `<button class="rb" data-acc="trListo" data-arg="${esc(t.id)}" aria-pressed="${!!t.listo}" aria-label="${t.listo ? "Marcar como pendiente" : "Marcar como listo"}" title="${t.listo ? "Pendiente" : "Listo"}">${ic("check")}</button>`}</div>`; };
  return seccionDia("tramites", cabSec("reloj", "Trámites y plazos", "Un trámite, un proceso o una cuota: desde su día de inicio hasta su día final, con avisos antes de vencer.",
      `<button class="btn mini" data-acc="trNuevo" data-arg="${fecha}">${ic("mas")} Nuevo</button>`) +
    (xs.length ? `<div class="tr-lista">${xs.map(fila).join("")}</div>` : `<p class="suave" style="margin:4px 0 0">Nada en curso este día. Agrega un trámite, un proceso o una cuota para no perder el plazo.</p>`));
}
/** ¿Toca avisar hoy? Según los días antes que eligió la persona (y el mismo día). */
function trAvisoHoy(t) {
  const hoyIso = iso(hoyFecha()), p = trProximo(t, hoyIso); if (!p || p < hoyIso) return false;
  const q = trContar(hoyIso, p, false); return q === 0 || (t.avisos || []).includes(q);
}

/* ---------- Formulario (hoja inferior) ---------- */
function trForm(t) {
  const n = !t.id, tipo = t.tipo || "tramite", modo = t.mensual ? "mensual" : (t.id && !t.dias) ? "fecha" : "dias";
  const av = new Set(t.avisos || [1, 3, 7]);
  return `<form class="form tr-form" data-form="tramite" data-arg="${esc(t.id || "")}">
    <h3 style="margin:0">${n ? "Nuevo trámite o plazo" : "Editar"}</h3>
    ${campo("Nombre", `<input name="n" required maxlength="120" value="${esc(t.n || "")}" placeholder="Licencia de construcción, cuota del carro…">`)}
    <div class="chips tr-tipos" role="radiogroup" aria-label="Tipo">${Object.entries(TR_TIPOS).map(([k, [nn, i]]) => `<label class="chip"><input type="radio" name="tipo" value="${k}" ${k === tipo ? "checked" : ""}>${ic(i)}${nn}</label>`).join("")}</div>
    ${campo("Día de inicio", `<input type="date" name="ini" required value="${esc(t.ini || iso(hoyFecha()))}">`)}
    <div class="chips" role="radiogroup" aria-label="Cómo termina">${[["dias", "Dura N días"], ["fecha", "Hasta una fecha"], ["mensual", "Se repite cada mes"]].map(([k, nn]) => `<label class="chip"><input type="radio" name="modo" value="${k}" ${k === modo ? "checked" : ""}>${nn}</label>`).join("")}</div>
    <div class="tr-dur">${campo("Días", `<input type="number" name="dias" min="1" max="3650" inputmode="numeric" value="${esc(t.dias || 45)}">`)}
      <label class="tr-hab"><input type="checkbox" name="habiles" ${t.habiles !== false ? "checked" : ""}><span>Días hábiles (sin fines de semana ni festivos de Colombia)</span></label></div>
    <div class="tr-hasta">${campo("Día final", `<input type="date" name="fin" value="${esc(t.fin || "")}">`)}</div>
    <p class="tr-calc suave" aria-live="polite"></p>
    <div><span class="campo-et">Avisarme antes</span><div class="chips">${[[1, "1 día"], [3, "3 días"], [7, "1 semana"], [15, "15 días"]].map(([d, nn]) => `<label class="chip"><input type="checkbox" name="av" value="${d}" ${av.has(d) ? "checked" : ""}>${nn}</label>`).join("")}</div></div>
    ${campo("Nota (opcional)", `<input name="nota" maxlength="200" value="${esc(t.nota || "")}" placeholder="Radicado 2026-123, ventanilla 4…">`)}
    <div class="botones"><button class="btn lleno" type="submit">Guardar</button>${n ? "" : `<button class="btn peligro" type="button" data-acc="trBorrar" data-arg="${esc(t.id)}">Borrar</button>`}</div></form>`;
}
function trActualizarForm(f) {
  const modo = f.modo.value; f.querySelector(".tr-dur").hidden = modo !== "dias"; f.querySelector(".tr-hasta").hidden = modo !== "fecha";
  const c = f.querySelector(".tr-calc"); if (!c || !f.ini.value) return;
  if (modo === "dias" && Number(f.dias.value) > 0) { const fin = trFin(f.ini.value, Number(f.dias.value), f.habiles.checked), d = deIso(fin);
    c.textContent = `Termina el ${["domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado"][d.getDay()]} ${d.getDate()} de ${MESES[d.getMonth()]} de ${d.getFullYear()}.`; }
  else if (modo === "mensual") c.textContent = `Se paga cada mes el día ${Number(f.ini.value.slice(8))}.`;
  else c.textContent = "";
}
Object.assign(ACC, {
  trNuevo(_, fecha) { abrirHoja(trForm({ ini: fecha || iso(hoyFecha()) })); const f = document.querySelector(".tr-form"); f.oninput = () => trActualizarForm(f); trActualizarForm(f); },
  trEditar(_, id) { const t = tramites().find(x => String(x.id) === String(id)); if (!t) return; abrirHoja(trForm(t)); const f = document.querySelector(".tr-form"); f.oninput = () => trActualizarForm(f); trActualizarForm(f); },
  trListo(_, id) { const t = tramites().find(x => String(x.id) === String(id)); if (!t) return; t.listo = !t.listo; trGuardar(t); toast(t.listo ? "¡Listo! Un pendiente menos en la mochila" : "Vuelve a estar pendiente"); pintarDetalle(); },
  trBorrar(_, id) { if (!confirm("¿Borrar este trámite?")) return; responder(TR_WB, "tramite#" + id, ""); cerrarHoja(); toast("Trámite borrado"); pintarDetalle(); },
});
window.FORMULARIOS = Object.assign(window.FORMULARIOS || {}, {
  tramite(f, datos) {
    const modo = datos.get("modo"), ini = String(datos.get("ini")), habiles = !!datos.get("habiles"), dias = modo === "dias" ? Math.max(1, Number(datos.get("dias")) || 1) : null;
    const id = f.dataset.arg || String(Date.now());
    const fin = modo === "dias" ? trFin(ini, dias, habiles) : modo === "fecha" ? (String(datos.get("fin") || "") || ini) : ini;
    if (fin < ini) { toast("El día final no puede ser antes del inicio"); return; }
    const prev = tramites().find(x => String(x.id) === id) || {};
    trGuardar({ id, n: String(datos.get("n")).trim(), tipo: String(datos.get("tipo") || "tramite"), ini, dias, habiles: modo === "dias" ? habiles : false, fin,
      avisos: datos.getAll("av").map(Number), mensual: modo === "mensual", nota: String(datos.get("nota") || "").trim(), listo: !!prev.listo });
    cerrarHoja(); toast("Guardado: lo verás en cada día de su plazo"); pintarDetalle();
  },
});

/* ---------- En los días: la sección, y en el calendario la línea que une inicio y fin ---------- */
{
  const base = actividadesDia;
  actividadesDia = function (fecha, ag, ms) { return base(fecha, ag, ms) + tramitesDia(fecha); };
}
function trMarcarCal(html) {
  const xs = tramites().filter(t => !t.listo); if (!xs.length) return html;
  return html.replace(/<button class="rv-dia([^"]*)" data-sel="d:(\d{4}-\d\d-\d\d)"/g, (m, cls, f) => {
    const roles = xs.map(t => trEnDia(t, f)).filter(Boolean); if (!roles.length) return m;
    const c = [...new Set(roles.map(r => "tr-" + r))].join(" ");
    return `<button class="rv-dia${cls} ${c}" data-sel="d:${f}"`;
  });
}
if (typeof rvBarra === "function") { const base = rvBarra; rvBarra = function (...a) { return trMarcarCal(base(...a)); }; }
window.tramites = tramites; window.festivosCO = festivosCO; window.trFin = trFin;
