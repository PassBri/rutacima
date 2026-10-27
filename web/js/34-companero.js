/* Compañero de Ascenso: vive en la Brújula, en el Campamento Base (No subo solo).
 * Herramientas vivas del workbook 11: ¿estás listo/a?, las 3 compatibilidades, red y green flags
 * y la revisión mensual de pareja. La guía completa sigue abierta para escribir todo el workbook.
 * Las respuestas usan las mismas claves que la app (comp#…), así se ven igual en los dos lados. */
const COMP = {
  WB: "companero",
  LISTO: [
    ["¿Busco pareja porque me siento incompleto/a sin alguien?", ["Sí", "No", "A veces"]],
    ["¿Siento que necesito una pareja para ser feliz?", ["Sí", "No", "A veces"]],
    ["¿Tengo claridad total sobre mi propia cumbre personal?", ["Sí", "No", "Más o menos"]],
    ["¿Estoy dispuesto/a a sacrificar mi cumbre por mantener una relación?", ["Sí", "No", "Depende de quién"]],
    ["¿Puedo ser completamente feliz estando solo/a?", ["Sí", "No", "No estoy seguro/a"]],
  ],
  CUMBRES: [["paralela", "Paralelas", "Cada uno tiene su montaña, pero comparten el valle. Ideal."],
    ["convergente", "Convergentes", "Sus cumbres se unen en un propósito mayor. Poderoso, pero pide mucha comunicación."],
    ["conflicto", "En conflicto", "Alcanzar tu cumbre impide la suya. Uno siempre sacrificará."]],
  FASES: [["Orientación", "Estar solo/a para ganar claridad", "Confundir pareja con propósito"],
    ["Preparación", "Conocer gente desde tu nueva identidad", "Distraerte del trabajo necesario"],
    ["Travesía", "Alguien que entienda tu ausencia", "Parejas que exigen atención constante"],
    ["Prueba", "Aquí se revelan las verdaderas", "Parejas que te abandonan en crisis"],
    ["Cumbre", "Cuidado: pueden enamorarse del logro", "Atraer gente por estatus, no por ti"],
    ["Descenso", "Excelente para una relación consciente", "La depresión post-cumbre afecta la relación"],
    ["Integración", "Construir vida juntos", "Acomodarse y dejar de crecer"]],
  RITMOS: ["Rápido", "Moderado", "Lento"],
  RED: ["Sabotea tu cumbre", "No tiene cumbre propia", "Está siempre en orientación", "Valores fundamentales incompatibles", "No puede celebrar tus victorias", "Comunicación tóxica"],
  GREEN: ["Tiene su propia cumbre clara", "Te empuja a ser mejor sin controlarte", "Celebra tus victorias", "Sostiene conversaciones difíciles", "Te da espacio cuando lo necesitas", "Está en crecimiento activo"],
  MES: [["fase", "¿En qué fase estoy en mi viaje?"], ["necesito", "¿Qué necesito de ti este mes?"], ["ofrezco", "¿Qué puedo ofrecerte este mes?"]],
};
const compK = k => "comp#" + k;
const compV = k => respuesta(compK(k));

function companeroHtml() {
  const d = Mandala.datos();
  const opcion = (k, v, t) => `<button type="button" class="chip" data-acc="compElegir" data-arg="${k}|${v}" aria-pressed="${compV(k) === String(v)}">${t}</button>`;
  // 1 · ¿Estás listo/a?
  const r = COMP.LISTO.map((_, i) => compV("listo#" + i));
  const completo = r.every(Boolean);
  const noListo = r[0] === "0" || r[1] === "0" || r[2] === "1" || r[3] === "0" || r[4] === "1";
  const listo = `<ol class="cp-preg">${COMP.LISTO.map(([p, ops], i) => `<li><span>${p}</span><span class="cp-ops">${ops.map((o, j) => opcion("listo#" + i, j, o)).join("")}</span></li>`).join("")}</ol>
    ${completo ? `<p class="cp-veredicto ${noListo ? "aun" : "si"}">${noListo
      ? "<b>Todavía no es el momento de buscar un compañero de ascenso.</b> No es que no merezcas amor: primero trabaja en tu relación contigo mismo/a. El amor desde la carencia atrae relaciones codependientes."
      : "<b>Estás caminando desde la plenitud.</b> Puedes elegir un compañero de ascenso desde la elección, no desde la necesidad."}</p>` : `<p class="suave cp-nota">Responde las 5 para ver tu lectura.</p>`}`;
  // 2 · Las 3 compatibilidades
  const fy = compV("fase_yo"), fp = compV("fase_pareja"), ry = compV("ritmo_yo"), rp = compV("ritmo_pareja");
  const faseInfo = f => f !== "" ? `<small class="cp-fase-info"><span>Mejor para: ${COMP.FASES[f][1]}</span><span>Riesgo: ${COMP.FASES[f][2]}</span></small>` : "";
  const sel = (k, lista, ph) => `<select data-acc-change="compSel" data-arg="${k}" aria-label="${ph}"><option value="">${ph}</option>${lista.map((t, i) => `<option value="${i}" ${compV(k) === String(i) ? "selected" : ""}>${t}</option>`).join("")}</select>`;
  const compat = `<div class="cp-compat">
      <div class="cp-bloque"><b>De cumbres</b>
        <div class="cp-dos"><label><small>Mi cumbre</small><span class="cp-mia">${esc(d.titulo || "Escribe tu cumbre en la capa 1")}</span></label>
        <label><small>Su cumbre</small><input type="text" data-resp="${COMP.WB}|${compK("su_cumbre")}" value="${esc(compV("su_cumbre"))}" placeholder="La cumbre de tu pareja"></label></div>
        <span class="cp-ops">${COMP.CUMBRES.map(([v, t]) => opcion("compat_cumbre", v, t)).join("")}</span>
        ${compV("compat_cumbre") ? `<p class="cp-nota">${COMP.CUMBRES.find(x => x[0] === compV("compat_cumbre"))[2]}</p>` : ""}</div>
      <div class="cp-bloque"><b>De fase</b>
        <div class="cp-dos"><label><small>Mi fase</small>${sel("fase_yo", COMP.FASES.map(x => x[0]), "Elige tu fase")}${faseInfo(fy)}</label>
        <label><small>Su fase</small>${sel("fase_pareja", COMP.FASES.map(x => x[0]), "Elige su fase")}${faseInfo(fp)}</label></div></div>
      <div class="cp-bloque"><b>De ritmo</b>
        <div class="cp-dos"><label><small>Mi ritmo</small><span class="cp-ops">${COMP.RITMOS.map((t, i) => opcion("ritmo_yo", i, t)).join("")}</span></label>
        <label><small>Su ritmo</small><span class="cp-ops">${COMP.RITMOS.map((t, i) => opcion("ritmo_pareja", i, t)).join("")}</span></label></div>
        ${ry !== "" && rp !== "" ? `<p class="cp-nota">${Math.abs(ry - rp) === 2 ? "Ritmos opuestos: uno siempre sentirá que el otro lo frena o lo presiona. Háblenlo." : ry === rp ? "Caminan al mismo ritmo." : "Ritmos cercanos: con acuerdos claros, se acompañan bien."}</p>` : ""}</div></div>`;
  // 3 · Red y green flags
  const marca = (k, t) => `<label class="cp-flag"><input type="checkbox" data-acc-change="compFlag" data-arg="${k}" ${compV(k) ? "checked" : ""}><span>${t}</span></label>`;
  const rojas = COMP.RED.filter((_, i) => compV("red#" + i)).length, verdes = COMP.GREEN.filter((_, i) => compV("green#" + i)).length;
  const flags = `<div class="cp-dos cp-flags"><div><b class="cp-rojo">Red flags · ${rojas}</b>${COMP.RED.map((t, i) => marca("red#" + i, t)).join("")}</div>
      <div><b class="cp-verde">Green flags · ${verdes}</b>${COMP.GREEN.map((t, i) => marca("green#" + i, t)).join("")}</div></div>
    <label class="cp-campo"><small>Pregunta crucial: ¿esta relación me acerca o me aleja de mi cumbre?</small><textarea rows="2" data-resp="${COMP.WB}|${compK("crucial")}">${esc(compV("crucial"))}</textarea></label>
    ${rojas >= 3 ? `<p class="cp-veredicto aun"><b>Marcaste ${rojas} red flags.</b> Si además la relación te aleja de tu cumbre, toca una conversación honesta, con tu pareja y contigo mismo/a.</p>` : ""}`;
  // 4 · Revisión mensual de pareja
  const ahora = new Date(), mes = `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, "0")}`;
  const revision = `<p class="suave cp-nota">Cada mes, compártanlo en voz alta. Esta es la de ${new Date().toLocaleDateString("es", { month: "long", year: "numeric" })}.</p>
    ${COMP.MES.map(([k, t]) => `<label class="cp-campo"><small>${t}</small><textarea rows="2" data-resp="${COMP.WB}|${compK("mes#" + mes + "#" + k)}">${esc(compV("mes#" + mes + "#" + k))}</textarea></label>`).join("")}`;
  const ab = estado.cpAbiertos ||= [true, false, false, false];
  const sec = (n, t, html) => `<details class="cp-sec" data-cp="${n - 1}"${ab[n - 1] ? " open" : ""}><summary><span class="cp-n">${n}</span><b>${t}</b>${ic("abajo")}</summary><div class="cp-cuerpo">${html}</div></details>`;
  return `<div class="cp">
    <p class="cp-intro">Dos personas completas caminando juntas, no dos mitades buscándose. No es una guía para conseguir pareja: es para elegir con quién compartir el ascenso sin soltar tu cumbre.</p>
    ${sec(1, "¿Estás listo/a?", listo)}${sec(2, "Las 3 compatibilidades", compat)}${sec(3, "Red flags y green flags", flags)}${sec(4, "Revisión mensual de pareja", revision)}
    <button class="btn" data-acc="compGuia">${ic("lecciones")} Abrir la guía completa (workbook 11)</button></div>`;
}

ACC.compElegir = (el, a) => {
  const [k, v] = a.split("|"); const actual = compV(k);
  responder(COMP.WB, compK(k), actual === v ? "" : v);
  repintarCompanero();
};
ACC.compSel = el => { responder(COMP.WB, compK(el.dataset.arg), el.value); repintarCompanero(); };
ACC.compFlag = el => { responder(COMP.WB, compK(el.dataset.arg), el.checked ? "1" : ""); repintarCompanero(); };
ACC.compGuia = () => ir("aprende", "wb:" + COMP.WB);
/** Abre el espacio desde Aprende, el Modelo o el Campamento Base. */
ACC.abrirCompanero = () => {
  estado.mdCajon = "companero";
  if (!(estado.sec === "ruta" && estado.sel === "vida")) ir("ruta", "vida");
  requestAnimationFrame(() => {
    document.querySelectorAll(".md-cajon").forEach(c => c.classList.toggle("abierto", c.dataset.cajon === "companero"));
    document.querySelectorAll(".md-riel-cajon").forEach(b => b.setAttribute("aria-pressed", String(b.dataset.arg === "companero")));
    if (innerWidth <= 700) document.querySelector('.md-cajon[data-cajon="companero"]')?.scrollIntoView({ block: "start" });
  });
};
/** Redibuja solo el cajón (sin perder el foco de la página) */
function repintarCompanero() {
  const c = document.querySelector('.md-cajon[data-cajon="companero"] .cp'); if (!c) return;
  const abiertos = [...c.querySelectorAll(".cp-sec")].map(s => s.open);
  const t = document.createElement("div"); t.innerHTML = companeroHtml(); const nuevo = t.firstElementChild;
  nuevo.querySelectorAll(".cp-sec").forEach((s, i) => { s.open = abiertos[i]; });
  c.replaceWith(nuevo);
}
window.companeroHtml = companeroHtml;

/* Las secciones abiertas se recuerdan aunque la página se redibuje al guardar una respuesta */
document.addEventListener("toggle", e => { const d = e.target; if (d.classList?.contains("cp-sec")) (estado.cpAbiertos ||= [true, false, false, false])[d.dataset.cp] = d.open; }, true);
