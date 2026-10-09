/* Rutaalacima Web · Plan Cumbre (los cobros aún no están activos): qué incluye, precio de referencia
   y "Avísame cuando esté disponible". Igual que PlanScreen.kt y Planes.kt de la app. */
var PLAN = { estado: null, cargando: false, error: false };
const PLAN_PRECIO = { mes: CONFIG.planPrecioMes || "$14.900", anio: CONFIG.planPrecioAnio || "$119.000" };
// [texto, en el plan gratuito, esencial (nunca se cobra), detalle gratis, detalle Cumbre]
const PLAN_BENEFICIOS = [
  ["Guías, ejes, metas y hábitos", true, true],
  ["Brújula de la Cima y vision board", true, true],
  ["Comunidad, mensajes y cordadas", true, true],
  ["Rutaalacima Web, también en iPhone", true, true],
  ["Respaldo, exportar y eliminar tus datos", true, true],
  ["Coach con IA", true, false, "Algunos mensajes al día", "Muchos más mensajes al día"],
  ["Audiolibros completos", false, false],
  ["Cierre de año guiado", false, false],
];
const ORIGEN_PLAN = { regalo: "Es un regalo de Rutaalacima.", institucion: "Lo tienes por tu institución.", piloto: "Eres parte del programa piloto." };

async function cargarPlan() {
  if (PLAN.cargando) return; PLAN.cargando = true;
  try {
    if (!Store.nube) PLAN.estado = { plan: "gratis", avisame: false, demo: true };
    else { const { data, error } = await Store.nube.sb.rpc("mi_plan"); if (error) throw error; PLAN.estado = data || { plan: "gratis", avisame: false }; }
    PLAN.error = false;
  } catch (e) { console.error(e); PLAN.estado = { plan: "gratis", avisame: false }; PLAN.error = true; }
  finally { PLAN.cargando = false; if (estado.sel === "plan" && !enEdicion()) pintarTodo(); }
}

DET["perfil.plan"] = () => {
  if (!PLAN.estado) cargarPlan();
  const e = PLAN.estado || { plan: "gratis" }, cumbre = e.plan === "cumbre";
  const marca = (si, txt) => `<span class="plan-celda${si ? " si" : ""}" aria-label="${txt}">${si ? "✓" : "—"}</span>`;
  const filas = PLAN_BENEFICIOS.map(([t, gratis, esencial, dG, dC]) => `<div class="plan-fila"><div><b>${t}</b>
      ${esencial ? `<small>Esto nunca se cobra.</small>` : ""}${dG ? `<small>Gratis: ${dG} · Cumbre: ${dC}</small>` : ""}</div>
      ${marca(gratis || esencial, `Gratis: ${dG || (gratis || esencial ? "incluido" : "no incluido")}`)}${marca(true, `Cumbre: ${dC || "incluido"}`)}</div>`).join("");
  let accion;
  if (!PLAN.estado) accion = `<p class="suave">Cargando…</p>`;
  else if (cumbre) accion = "";
  else if (e.avisame) accion = `<p class="plan-ok">✓ Listo: te avisaremos cuando esté disponible.</p>`;
  else if (e.demo) accion = `<p class="suave">En la demostración no se guarda. Entra con tu cuenta para que te avisemos.</p>`;
  else accion = `<button class="btn lleno" data-acc="avisamePlan">Avísame cuando esté disponible</button>`;
  const hasta = cumbre && e.hasta ? new Date(e.hasta).toLocaleDateString("es", { day: "numeric", month: "long", year: "numeric" }) : "";
  return cab(av(ic("cumbre"), "var(--oro)"), "Plan Cumbre", "Qué incluye y cuánto cuesta") + cuerpo(
    hoja(`<div class="plan-intro">${ic("cumbre")}<h2>Plan Cumbre</h2><p>Todo el método sigue gratis. El Plan Cumbre es para quien quiere ir más lejos y, de paso, ayuda a sostener Rutaalacima.</p></div>`) +
    hoja(cumbre ? `<h3 style="margin:0;color:var(--burdeos)">Tienes el Plan Cumbre hasta el ${esc(hasta)}</h3>${ORIGEN_PLAN[e.origen] ? `<p class="suave" style="margin:4px 0 0">${ORIGEN_PLAN[e.origen]}</p>` : ""}`
      : `<h3 style="margin:0">Tu plan: Gratis</h3><p class="suave" style="margin:4px 0 0">Mientras tanto, todo sigue abierto para todos.</p>`) +
    hoja(`<div class="plan-fila plan-cab"><span></span><span class="plan-celda">Gratis</span><span class="plan-celda si">Cumbre</span></div>${filas}`) +
    hoja(`<p class="plan-precio">${esc(PLAN_PRECIO.mes)} al mes o ${esc(PLAN_PRECIO.anio)} al año</p>
      <p class="suave" style="text-align:center;margin-top:0">Precio de referencia en pesos colombianos. Todavía no se cobra.</p>
      <div class="botones" style="justify-content:center">${accion}</div>${PLAN.error ? `<p class="form-error" style="text-align:center">No se pudo guardar. Revisa tu conexión e inténtalo de nuevo.</p>` : ""}`),
    "max-width:640px");
};

Object.assign(ACC, {
  async avisamePlan(el) {
    if (!Store.nube) return;
    el.disabled = true;
    const { error } = await Store.nube.sb.rpc("avisame_plan");
    if (error) { console.error(error); PLAN.error = true; el.disabled = false; pintarDetalle(); return; }
    PLAN.estado = { ...PLAN.estado, avisame: true }; PLAN.error = false; pintarDetalle();
  },
});
