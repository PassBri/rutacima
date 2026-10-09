/* Rutaalacima Web · Moderación: revisar reportes, mantener, ocultar o eliminar, suspender cuentas.
   Solo aparece para las cuentas nombradas en la tabla moderadores (ver supabase/schema.sql).
   También muestra a quien tiene la cuenta suspendida hasta cuándo y por qué. */
// var (no const): la lista del perfil puede pintarse antes de que cargue este archivo
var MOD = { soy: null, casos: null, hist: null, pestana: "pendientes", suspension: undefined, ocupado: null };
P.mazo = "M1 21h12v2H1v-2zM5.245 8.07l2.83-2.827 14.14 14.142-2.828 2.828L5.245 8.07zM12.317 1l5.657 5.656-2.83 2.83-5.654-5.66L12.317 1zM3.825 9.485l5.657 5.657-2.828 2.828-5.657-5.657 2.828-2.828z";

const TIPO_MOD = { post: "Publicación", comentario: "Comentario", conversacion: "Conversación", persona: "Persona" };
const ACCION_MOD = { restaurar: "Mantuvo visible", ocultar: "Ocultó", eliminar: "Eliminó", descartar: "Descartó un reporte", suspender: "Suspendió", levantar: "Levantó la suspensión" };
const fechaMod = s => { const t = Date.parse(s); return isNaN(t) ? "" : new Date(t).toLocaleString("es", { dateStyle: "medium", timeStyle: "short" }); };

/** ¿Esta cuenta modera? Se pregunta una vez por sesión y repinta la lista del perfil si sí. */
async function comprobarModeracion() {
  if (!Store.nube || MOD.soy !== null) return;
  MOD.soy = false;
  try {
    const [{ data: soy }, { data: susp }] = await Promise.all([Store.nube.sb.rpc("es_moderador"), Store.nube.sb.rpc("mi_suspension")]);
    MOD.soy = soy === true; MOD.suspension = susp || null;
    if (MOD.soy) cargarModeracion();
    if ((MOD.soy || MOD.suspension) && estado.sec === "perfil" && !enEdicion()) pintarTodo();
  } catch (e) { console.error(e); }
}

async function cargarModeracion() {
  if (cargarModeracion.activo) return; cargarModeracion.activo = true;
  try {
    const [p, h] = await Promise.all([Store.nube.sb.rpc("moderacion_pendientes"), Store.nube.sb.rpc("moderacion_historial")]);
    if (p.error) throw p.error;
    MOD.casos = p.data || []; MOD.hist = h.data || []; MOD.error = false;
  } catch (e) { console.error(e); MOD.error = true; MOD.casos = MOD.casos || []; MOD.hist = MOD.hist || []; }
  finally { cargarModeracion.activo = false; if (estado.sel === "moderacion" && !enEdicion()) pintarTodo(); }
}

/** Aviso de suspensión (perfil › este computador). */
function avisoSuspension() {
  const s = MOD.suspension; if (!s) return "";
  const hasta = s.permanente ? "" : fechaMod(s.hasta);
  return hoja(`<h3 style="color:#A3321F">${ic("mazo")} Tu cuenta está suspendida</h3>
    <p>${hasta ? `Hasta el ${esc(hasta)}. Mientras tanto puedes seguir usando tu ruta, pero no publicar, comentar ni escribir mensajes.` : "De forma permanente. Puedes seguir usando tu ruta, pero no participar en la comunidad."}</p>
    ${s.motivo ? `<p><b>Motivo:</b> ${esc(s.motivo)}</p>` : ""}
    <p class="suave">Si crees que es un error, escríbenos: lo revisamos. <a href="${/github\.io$|^localhost|^127\./.test(location.hostname) ? "" : "https://passbri.github.io/rutacima/"}privacidad.html#procedimiento" target="_blank" rel="noopener">Pedir revisión</a></p>`, "mod-suspension");
}

function tarjetaCaso(c) {
  const contenido = c.tipo === "post" || c.tipo === "comentario", a = c.autor || {}, k = `${c.tipo}|${c.objetivo}`;
  const ocupado = MOD.ocupado === c.objetivo;
  const extras = [a.sanciones > 0 ? `Sanciones previas: ${a.sanciones}` : "", a.suspendido_hasta ? "Cuenta suspendida" : ""].filter(Boolean).join(" · ");
  return hoja(`<div class="mod-cab"><span class="etiqueta">${TIPO_MOD[c.tipo] || c.tipo}</span><span class="mod-n${c.reportes >= 3 ? " alto" : ""}">Reportes: ${c.reportes}</span></div>
    ${a.usuario ? `<div class="mod-autor">@${esc(a.usuario)}${a.nombre ? ` · ${esc(a.nombre)}` : ""}</div>${extras ? `<div class="mod-alerta">${extras}</div>` : ""}` : ""}
    ${c.imagen ? `<img class="mod-img" src="${esc(c.imagen)}" alt="Imagen reportada" loading="lazy">` : ""}
    ${c.texto ? `<p class="mod-texto">${esc(c.texto)}</p>` : ""}
    ${(c.mensajes || []).length ? `<p class="suave" style="margin-bottom:2px">Últimos mensajes de esta persona en la conversación:</p>${c.mensajes.slice(-8).filter(m => m.texto).map(m => `<p class="mod-msj">“${esc(m.texto)}”</p>`).join("")}` : ""}
    ${contenido ? `<p class="suave" style="font-size:13px">${c.oculto ? "Oculto para la comunidad" : "Visible"}</p>` : ""}
    <ul class="mod-motivos">${(c.motivos?.length ? c.motivos : ["Sin motivo escrito"]).slice(0, 5).map(m => `<li>${esc(m)}</li>`).join("")}</ul>
    <p class="suave" style="font-size:12px;margin:0">${fechaMod(c.ultimo)}</p>
    <div class="botones mod-acciones">${ocupado ? `<span class="suave">Guardando…</span>` : `
      <button class="btn mini" data-acc="moderar" data-arg="${esc(k)}|restaurar">${contenido ? "Mantener visible" : "Descartar reporte"}</button>
      ${contenido && !c.oculto ? `<button class="btn mini" data-acc="moderar" data-arg="${esc(k)}|ocultar">Ocultar</button>` : ""}
      ${contenido ? `<button class="btn mini peligro" data-acc="moderar" data-arg="${esc(k)}|eliminar">Eliminar</button>` : ""}
      ${a.id ? `<button class="btn mini peligro" data-acc="abrirSuspender" data-arg="${esc(c.objetivo)}">${a.suspendido_hasta ? "Levantar suspensión" : "Suspender cuenta"}</button>` : ""}`}
    </div>
    ${MOD.suspender === c.objetivo && a.id ? formSuspender(c) : ""}`, "mod-caso");
}

function formSuspender(c) {
  const levantar = !!c.autor.suspendido_hasta;
  return `<form class="form mod-form" data-form="suspender" data-arg="${esc(c.autor.id)}">
    ${levantar ? `<p>¿Levantar la suspensión de @${esc(c.autor.usuario)}?</p>` : `
    <div class="mod-dias" role="radiogroup" aria-label="Duración">${[[1, "1 día"], [7, "7 días"], [30, "30 días"], [-1, "Permanente"]].map(([d, t], i) =>
      `<label class="pill-radio"><input type="radio" name="dias" value="${d}"${i === 1 ? " checked" : ""}> ${t}</label>`).join("")}</div>
    <label class="campo"><span>Motivo (lo verá la persona)</span><textarea name="motivo" rows="2" maxlength="500" required></textarea></label>`}
    <input type="hidden" name="levantar" value="${levantar ? 1 : 0}">
    <div class="botones"><button class="btn lleno" type="submit">Confirmar</button><button class="btn" type="button" data-acc="abrirSuspender" data-arg="">Cancelar</button></div></form>`;
}

DET["perfil.moderacion"] = () => {
  if (!MOD.soy) return cab(av(ic("mazo"), "#8A7B70"), "Moderación", "") + cuerpo(hoja(`<p>Esta sección es solo para moderadores.</p>`), "max-width:640px");
  if (MOD.casos === null) cargarModeracion();
  const n = MOD.casos?.length || 0, pend = MOD.pestana === "pendientes";
  const tabs = `<div class="mod-tabs" role="tablist"><button role="tab" aria-selected="${pend}" data-acc="pestanaMod" data-arg="pendientes">Pendientes (${n})</button>
    <button role="tab" aria-selected="${!pend}" data-acc="pestanaMod" data-arg="historial">Historial</button>
    <button class="btn mini" data-acc="recargarMod" style="margin-left:auto">Actualizar</button></div>`;
  let h;
  if (MOD.casos === null) h = `<p class="vacio-mini">Cargando…</p>`;
  else if (pend) h = n ? MOD.casos.map(tarjetaCaso).join("") : hoja(`<p class="vacio-mini">${ic("mazo")} No hay reportes por revisar. La comunidad está en calma.</p>`);
  else h = (MOD.hist || []).length ? hoja(MOD.hist.map(d => `<div class="mod-hist"><div>@${esc(d.moderador || "—")} · ${ACCION_MOD[d.accion] || d.accion}${!["suspender", "levantar"].includes(d.accion) && TIPO_MOD[d.tipo] ? ` · ${TIPO_MOD[d.tipo].toLowerCase()}` : ""}${d.usuario ? ` · @${esc(d.usuario)}` : ""}</div>
      ${d.nota ? `<div class="suave">${esc(d.nota)}</div>` : ""}<div class="suave" style="font-size:12px">${fechaMod(d.creado)}</div></div>`).join(""))
    : hoja(`<p class="vacio-mini">Todavía no hay decisiones.</p>`);
  return cab(av(ic("mazo"), "var(--burdeos)"), "Moderación", "Reportes de la comunidad") +
    cuerpo(tabs + (MOD.error ? `<p class="suave" style="color:#B3261E">No se pudo completar. Revisa tu conexión e inténtalo de nuevo.</p>` : "") + h, "max-width:760px");
};

Object.assign(ACC, {
  pestanaMod(el, p) { MOD.pestana = p; pintarDetalle(); },
  recargarMod() { cargarModeracion(); },
  abrirSuspender(el, id) { MOD.suspender = id || null; pintarDetalle(); },
  async moderar(el, arg) {
    const [tipo, objetivo, accion] = arg.split("|");
    if (accion === "eliminar" && !el.dataset.seguro) { el.dataset.seguro = "1"; el.textContent = "¿Eliminar para siempre?"; return; }
    if (MOD.ocupado) return;
    MOD.ocupado = objetivo; pintarDetalle();
    try {
      const { error } = await Store.nube.sb.rpc("moderar", { p_tipo: tipo, p_objetivo: objetivo, p_accion: accion, p_nota: "" });
      if (error) throw error;
      toast({ restaurar: tipo === "post" || tipo === "comentario" ? "Se mantiene visible" : "Reporte descartado", ocultar: "Ocultado", eliminar: "Eliminado" }[accion]);
    } catch (e) { console.error(e); toast("No se pudo completar. Revisa tu conexión e inténtalo de nuevo."); }
    MOD.ocupado = null; await cargarModeracion();
  },
});

/** Formulario de suspensión (lo llama el manejador general de formularios). */
async function enviarSuspension(f) {
  const datos = new FormData(f), levantar = datos.get("levantar") === "1";
  const motivo = String(datos.get("motivo") || "").trim();
  if (!levantar && !motivo) { toast("Escribe el motivo: la persona lo verá."); return; }
  const boton = f.querySelector("button[type=submit]"); if (boton) boton.disabled = true;
  try {
    const { error } = await Store.nube.sb.rpc("suspender", { p_usuario: f.dataset.arg, p_dias: levantar ? 0 : Number(datos.get("dias")), p_motivo: motivo });
    if (error) throw error;
    toast(levantar ? "Suspensión levantada" : "Cuenta suspendida"); MOD.suspender = null; await cargarModeracion();
  } catch (e) { console.error(e); toast(e?.message || "No se pudo completar."); if (boton) boton.disabled = false; }
}
