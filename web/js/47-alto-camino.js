"use strict";
/* ======================================================================
 * Alto en el camino (igual en la app): una vez al mes, 9 preguntas en tarjetas de papel, una por campamento y la
 * novena por tu cumbre; y cada noche, en el cierre del día, una sola pregunta. Las respuestas se guardan con su fecha
 * ("alto#<año-mes>#<n>" y "noche#<fecha>") para releerlas y ver cuánto has subido.
 * ====================================================================== */
const ALTO = {"intro": "A veces no hace falta cambiar de montaña. Basta con detenerte en el camino y hacerte las preguntas correctas.", "cierre": "Esto respondiste en este alto del camino. Vuelve a leerlo el próximo mes: ahí verás cuánto has subido.", "nombres": {"VOL": "Voluntad", "MAE": "Maestría", "VOZ": "Voz", "VAL": "Valor", "EVO": "Evolución", "TRA": "Trascendencia", "CON": "Confluencia", "CAM": "Campamento Base", "CUMBRE": "Tu cumbre"}, "preguntas": {"VOL": ["¿Qué paso pequeño llevas días esquivando, y qué pasaría si lo dieras mañana antes del desayuno?", "Si tu cuerpo pudiera escribirte una carta, ¿qué te agradecería y qué te pediría?", "¿En qué momento del día se te va la fuerza, y qué lo protegería?", "¿Qué promesa te hiciste y dejaste caer en silencio? ¿Vale la pena retomarla?", "Cuando nadie te ve, ¿quién eres en la subida?", "¿Qué hábito cargas en la mochila que pesa más de lo que aporta?"], "MAE": ["¿Qué te gustaría dominar tan bien que pudieras enseñarlo sin notas?", "¿Qué aprendiste este mes que tu yo de hace un año no sabía?", "Si hoy tuvieras un guía de montaña para tu oficio, ¿qué le preguntarías primero?", "¿En qué te da miedo volver a ser principiante?", "¿Qué error reciente te enseñó más que un acierto?", "¿A qué dedicarías una hora diaria si supieras que en cinco años serías de los mejores?"], "VOZ": ["¿Qué verdad tuya todavía no te atreves a decir en voz alta?", "Si tu mensaje cupiera en una sola frase grabada en una piedra de la cumbre, ¿cuál sería?", "¿Con quién te quedaste en silencio cuando querías hablar?", "¿Qué historia de tu vida podría darle ánimo a alguien que apenas empieza a subir?", "¿Dónde suenas como tú y dónde suenas como lo que esperan de ti?", "¿Qué conversación pendiente haría más liviana tu mochila?"], "VAL": ["Si tu dinero hablara, ¿diría que trabaja para tu cumbre o para tu ansiedad?", "¿Qué compra reciente te alejó de tu cima, y cuál te acercó?", "¿Cuánto vale una hora de tu vida, y en qué la estás cambiando?", "¿Qué valor das a los demás que todavía no te atreves a cobrar?", "Si mañana tuvieras seis meses de reserva, ¿qué decisión tomarías que hoy no tomas?", "¿Qué te enseñaron sobre el dinero que ya no quieres seguir creyendo?"], "EVO": ["¿Qué parte de ti de hace cinco años te sorprendería ver hoy?", "¿Qué versión de ti estás dejando atrás en este tramo del camino?", "¿Qué creencia sobre ti ya no te queda?", "Si este año fuera un capítulo de tu libro, ¿qué título le pondrías?", "¿Qué caída te hizo más fuerte de lo que admites?", "¿Qué te diría el niño o la niña que fuiste al ver cómo vives hoy?"], "TRA": ["¿Qué quieres que quede cuando ya no estés en la montaña?", "¿A quién le estás abriendo camino, aunque todavía no lo sepa?", "Si tu vida fuera un sendero que otros recorrerán, ¿qué señal dejarías en el tramo más difícil?", "¿Por qué causa te levantarías temprano sin que nadie te lo pida?", "¿Qué harías distinto este año si supieras que alguien te mira para aprender?", "¿Qué te agradecerán las personas dentro de veinte años?"], "CON": ["¿Qué actividad de tu vida mueve a la vez tu cuerpo, tu mente y tu propósito?", "¿Dónde se cruzan lo que amas, lo que sabes hacer y lo que el mundo necesita?", "¿Qué dos metas tuyas compiten entre sí, y cómo podrían caminar juntas?", "Si tuvieras que elegir un solo proyecto que una tus seis ejes, ¿cuál sería?", "¿Qué momento de esta semana te hizo sentir completo, sin piezas sueltas?", "¿Qué estás haciendo por separado que podrías hacer de una sola vez?"], "CAM": ["¿Quién sostiene la cuerda cuando resbalas?", "¿A quién le debes una llamada solo para decirle gracias?", "¿Con quién te gustaría hacer cordada este año?", "¿Qué relación te quita más oxígeno del que te da?", "¿Cómo está tu campamento base: tu casa, tu descanso, tu gente?", "¿A quién podrías acompañar tú en su subida esta semana?"], "CUMBRE": ["Si hoy llegaras a tu cumbre, ¿quién estaría contigo en la foto?", "¿Tu cumbre sigue siendo tuya, o es la que otros esperaban de ti?", "Si tu yo de dentro de diez años leyera tu día de hoy, ¿qué te agradecería?", "¿Qué harías mañana si supieras que el miedo no puede seguirte?", "¿A qué renunciarías para llegar a ser quien de verdad quieres ser?", "Si te quedara un solo año de camino, ¿seguirías subiendo esta misma montaña?"]}};
const ALTO_ORDEN = ["VOL", "MAE", "VOZ", "VAL", "EVO", "TRA", "CON", "CAM", "CUMBRE"];
const ALTO_WB = "alto";
/** Las 9 preguntas del mes: una de cada campamento (cambian cada mes) y la de tu cumbre. */
function altoPreguntas(ym) {
  const [a, m] = ym.split("-").map(Number), k = a * 12 + m;
  return ALTO_ORDEN.map((c, i) => { const xs = ALTO.preguntas[c]; return { c, n: ALTO.nombres[c], t: xs[(k + i) % xs.length] }; });
}
/** La pregunta de la noche: recorre todo el banco, una distinta cada día. */
function preguntaNoche(fecha) {
  const todas = ALTO_ORDEN.flatMap(c => ALTO.preguntas[c].map(t => ({ c, n: ALTO.nombres[c], t })));
  const dia = Math.floor(deIso(fecha).getTime() / DIA_MS);
  return todas[((dia * 7) % todas.length + todas.length) % todas.length];
}
const altoYm = () => iso(hoyFecha()).slice(0, 7);
const altoRespondidas = ym => ALTO_ORDEN.filter((_, i) => respuesta(`alto#${ym}#${i}`).trim()).length;

/* ---------- El ritual: tarjetas de papel a pantalla completa ---------- */
function altoTarjeta() {
  const s = estado.alto; if (!s) return "";
  const ps = altoPreguntas(s.ym), [a, m] = s.ym.split("-").map(Number), mes = `${mayus(MESES[m - 1])} ${a}`;
  let cuerpoT;
  if (s.i < 0) cuerpoT = `<div class="alto-centro"><span class="alto-marca">${ic("cima")}</span><h2 class="alto-tit">Alto en el camino</h2><p class="alto-q">${ALTO.intro}</p>
      <p class="alto-sub">9 preguntas · ${mes}</p><button class="btn lleno" data-acc="altoIr" data-arg="0">Empezar</button></div>`;
  else if (s.i >= ps.length) cuerpoT = `<div class="alto-resumen"><h2 class="alto-tit">Tu alto de ${MESES[m - 1]}</h2><p class="alto-sub">${ALTO.cierre}</p>
      ${ps.map((p, i) => { const r = respuesta(`alto#${s.ym}#${i}`).trim(); return `<div class="alto-r"><small>${esc(p.n)}</small><b>${esc(p.t)}</b><p>${r ? esc(r) : `<span class="suave">Sin responder</span>`}</p></div>`; }).join("")}
      <div class="botones"><button class="btn lleno" data-acc="altoCerrar">Guardar y cerrar</button><button class="btn" data-acc="altoIr" data-arg="0">Volver a empezar</button></div></div>`;
  else { const p = ps[s.i];
    cuerpoT = `<div class="alto-centro"><p class="alto-num">Pregunta ${s.i + 1} <span>de ${ps.length}</span></p><span class="alto-camp">${esc(p.n)}</span>
      <p class="alto-q">${esc(p.t)}</p>
      <textarea class="alto-resp" rows="4" maxlength="2000" placeholder="Escribe sin pensarlo mucho…" data-acc-input="altoResp" data-arg="${s.ym}|${s.i}" aria-label="Tu respuesta">${esc(respuesta(`alto#${s.ym}#${s.i}`))}</textarea></div>`; }
  const puntos = ps.map((_, i) => `<i class="${i === s.i ? "on" : respuesta(`alto#${s.ym}#${i}`).trim() ? "ok" : ""}"></i>`).join("");
  return `<div class="alto-velo" role="dialog" aria-modal="true" aria-label="Alto en el camino"><article class="alto-papel">
    <header class="alto-cab"><span>Alto en el camino · ${mes}</span><button class="rb" data-acc="altoCerrar" aria-label="Cerrar (Esc)" title="Cerrar (Esc)">${ic("cerrar")}</button></header>
    ${cuerpoT}
    ${s.i >= 0 && s.i < ps.length ? `<footer class="alto-pie"><button class="btn" data-acc="altoIr" data-arg="${s.i - 1}" ${s.i === 0 ? "disabled" : ""}>${ic("izq")} Anterior</button>
      <span class="alto-puntos" aria-hidden="true">${puntos}</span>
      <button class="btn lleno" data-acc="altoIr" data-arg="${s.i + 1}">${s.i === ps.length - 1 ? "Ver mis respuestas" : "Siguiente"} ${ic("der")}</button></footer>` : ""}
  </article></div>`;
}
function altoPintar() {
  document.querySelector(".alto-velo")?.remove();
  if (!estado.alto) return;
  document.body.insertAdjacentHTML("beforeend", altoTarjeta());
  const v = document.querySelector(".alto-velo"); v.onclick = e => { if (e.target === v) return ACC.altoCerrar(); clic(e); }; v.oninput = e => entrada(e);
  requestAnimationFrame(() => document.querySelector(".alto-resp")?.focus({ preventScroll: true }));
}
Object.assign(ACC, {
  altoAbrir(_, ym) { const y = ym || altoYm(); const n = altoRespondidas(y); estado.alto = { ym: y, i: n === 0 ? -1 : n >= 9 ? 9 : ALTO_ORDEN.findIndex((_, i) => !respuesta(`alto#${y}#${i}`).trim()) }; altoPintar(); },
  altoIr(_, i) { if (!estado.alto) return; estado.alto.i = Math.max(-1, Math.min(9, Number(i))); altoPintar(); },
  altoCerrar() { estado.alto = null; altoPintar(); if (estado.sec === "hoy" || estado.sec === "ruta" || estado.sec === "frases") pintarDetalle(); },
  altoResp(el, arg) { const [ym, i] = arg.split("|"); responder(ALTO_WB, `alto#${ym}#${i}`, el.value.slice(0, 2000)); },
  nocheResp(el, fecha) { responder(ALTO_WB, `noche#${fecha}`, el.value.slice(0, 2000)); },
});
document.addEventListener("keydown", e => {
  if (!estado.alto) return;
  if (e.key === "Escape") { e.preventDefault(); ACC.altoCerrar(); return; }
  if (document.activeElement?.tagName === "TEXTAREA") return;
  if (e.key === "ArrowRight") ACC.altoIr(null, estado.alto.i + 1); else if (e.key === "ArrowLeft") ACC.altoIr(null, estado.alto.i - 1);
});

/** La tarjeta que invita al alto del mes (en el cierre del día y en Mis frases). */
function altoInvitacion() {
  const ym = altoYm(), n = altoRespondidas(ym), [, m] = ym.split("-").map(Number);
  return `<button class="alto-inv" data-acc="altoAbrir" data-arg="${ym}"><span class="alto-inv-ic">${ic("cima")}</span>
    <span><b>Alto en el camino · ${MESES[m - 1]}</b><small>${n === 0 ? "9 preguntas para detenerte y mirar tu ruta" : n >= 9 ? "Completo · vuelve a leer tus respuestas" : `${n} de 9 respondidas · continúa`}</small></span>${ic("der")}</button>`;
}
/** La pregunta de la noche, al final del cierre del día. */
function preguntaNocheHtml(fecha) {
  const p = preguntaNoche(fecha);
  return `<div class="sec-div"></div><div class="noche-q"><span class="noche-et">${ic("frases")} La pregunta de la noche · ${esc(p.n)}</span>
    <p class="noche-t">${esc(p.t)}</p>
    <textarea rows="3" maxlength="2000" placeholder="Una línea basta. Queda solo para ti." data-acc-input="nocheResp" data-arg="${fecha}" aria-label="Tu respuesta a la pregunta de la noche">${esc(respuesta(`noche#${fecha}`))}</textarea></div>`;
}
{
  const base = cierreDia;
  cierreDia = function (fecha, ag, campoAg) {
    const h = base(fecha, ag, campoAg);
    // Al final de la sección del cierre: la pregunta de la noche y, debajo, el alto del mes
    return h.replace(/<\/section>\s*$/, preguntaNocheHtml(fecha) + `<div style="margin-top:12px">${altoInvitacion()}</div></section>`);
  };
}
{
  const base = DET["frases.f"];
  if (base) DET["frases.f"] = (...a) => base(...a).replace(/(<div class="det-cuerpo"><div class="ancho"[^>]*>)/, `$1${altoInvitacion()}`);
}
window.altoInvitacion = altoInvitacion;
