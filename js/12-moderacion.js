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
    const [p, h, e, m, pl, mp] = await Promise.all(["moderacion_pendientes", "moderacion_historial", "errores_recientes", "metricas_comunidad", "planes_vigentes", "metricas_plan"].map(f => Store.nube.sb.rpc(f)));
    if (p.error) throw p.error;
    MOD.casos = p.data || []; MOD.hist = h.data || []; MOD.errores = e.data || []; MOD.metricas = m.data || null;
    MOD.planes = pl.data || []; MOD.metricasPlan = mp.data || null; MOD.error = false;
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

/** Planes: dar el Plan Cumbre a pilotos, instituciones o como regalo, y ver quiénes lo tienen. */
function vistaPlanes() {
  const mp = MOD.metricasPlan || {}, ps = MOD.planes || [];
  const ORIGEN = { regalo: "Regalo", institucion: "Institución", piloto: "Piloto" };
  return hoja(`<div class="met-cifras">
      <div class="met-cifra"><b>${Number(mp.interesados || 0).toLocaleString("es")}</b><span>Pidieron que les avisen</span><small>${Number(mp.interesados_30d || 0).toLocaleString("es")} en 30 días</small></div>
      <div class="met-cifra"><b>${Number(mp.cumbre_vigentes || 0).toLocaleString("es")}</b><span>Con el Plan Cumbre</span><small>regalos, pilotos e instituciones</small></div>
    </div><p class="suave" style="font-size:12px;margin-bottom:0">Quienes tocan "Avísame" muestran cuánta demanda hay antes de cobrar.</p>`) +
    hoja(`<h3 style="margin-top:0">Dar el Plan Cumbre</h3>
      <form class="form" data-form="darPlan">
        <label class="campo"><span>Usuario</span><div class="con-prefijo"><b>@</b><input name="usuario" required maxlength="30" autocomplete="off"></div></label>
        <div class="mod-dias" role="radiogroup" aria-label="Duración">${[[30, "1 mes"], [90, "3 meses"], [180, "6 meses"], [365, "1 año"]].map(([d, t], i) =>
          `<label class="pill-radio"><input type="radio" name="dias" value="${d}"${i === 3 ? " checked" : ""}> ${t}</label>`).join("")}</div>
        <div class="mod-dias" role="radiogroup" aria-label="Por qué">${Object.entries(ORIGEN).map(([k, t], i) =>
          `<label class="pill-radio"><input type="radio" name="origen" value="${k}"${i === 2 ? " checked" : ""}> ${t}</label>`).join("")}</div>
        <label class="campo"><span>Nota interna (opcional)</span><input name="nota" maxlength="300" placeholder="Ej.: Colegio piloto, grado 11"></label>
        <div class="botones"><button class="btn lleno" type="submit">Dar el plan</button></div>
      </form>`) +
    hoja(`<h3 style="margin-top:0">Planes vigentes</h3>${ps.length ? ps.map(p => `<div class="mod-hist"><div>@${esc(p.usuario)} · ${ORIGEN[p.origen] || p.origen} · hasta ${fechaMod(p.hasta)}</div>
      ${p.nota ? `<div class="suave">${esc(p.nota)}</div>` : ""}<button class="btn mini peligro" data-acc="quitarPlan" data-arg="${esc(p.usuario)}" style="margin-top:6px">Quitar</button></div>`).join("")
      : `<p class="vacio-mini">Nadie tiene el plan todavía.</p>`}`);
}

/** Métricas: cifras grandes y la participación semanal (12 semanas) en barras con su tabla. */
function vistaMetricas() {
  const m = MOD.metricas; if (!m) return hoja(`<p class="vacio-mini">Sin métricas todavía.</p>`);
  const n = v => Number(v || 0).toLocaleString("es");
  const ret = m.retencion?.base ? Math.round(100 * m.retencion.volvieron / m.retencion.base) + " %" : "—";
  const cifra = (valor, titulo, nota = "") => `<div class="met-cifra"><b>${valor}</b><span>${titulo}</span>${nota ? `<small>${nota}</small>` : ""}</div>`;
  const sem = m.semanas || [], max = Math.max(1, ...sem.map(x => x.activas)), W = 600, H = 160, paso = W / Math.max(1, sem.length), ancho = Math.max(4, paso - 2);
  const barras = sem.map((x, i) => {
    const alto = x.activas ? Math.max(4, Math.round((H - 20) * x.activas / max)) : 0, xx = i * paso + 1, y = H - alto;
    const etiqueta = `Semana del ${new Date(x.semana + "T12:00").toLocaleDateString("es", { day: "numeric", month: "short" })}: ${x.activas} activas, ${x.nuevas} nuevas, ${x.publicaciones} publicaciones`;
    return `<g class="met-barra" tabindex="0" aria-label="${etiqueta}"><title>${etiqueta}</title><rect class="met-hit" x="${i * paso}" y="0" width="${paso}" height="${H}"/>${alto ? `<path d="M${xx},${H} V${y + 4} q0,-4 4,-4 h${ancho - 8} q4,0 4,4 V${H} Z"/>` : ""}</g>`;
  }).join("");
  const tipos = Object.entries(m.publicaciones_30d || {}).sort((a, b) => b[1] - a[1]);
  const NOMBRE_TIPO = { LOGRO: "Logros", EVIDENCIA: "Evidencias", VISION: "Visión", META: "Metas", REFLEXION: "Reflexiones" };
  return hoja(`<div class="met-cifras">
      ${cifra(n(m.cuentas), "Cuentas", `${n(m.nuevas_30d)} nuevas en 30 días`)}
      ${cifra(n(m.activas_7d), "Activas esta semana", `${n(m.activas_30d)} en 30 días`)}
      ${cifra(ret, "Volvieron tras la 1.ª semana", m.retencion?.base ? `${n(m.retencion.volvieron)} de ${n(m.retencion.base)}` : "Aún sin cuentas de 2 semanas")}
      ${cifra(n(m.coach_ia_30d?.mensajes), "Mensajes al coach IA", `${n(m.coach_ia_30d?.personas)} personas · 30 días`)}
    </div>`) +
    hoja(`<h3 style="margin-top:0">Personas activas por semana</h3><p class="suave" style="margin-top:0">Publicaron, impulsaron, comentaron, escribieron o marcaron su día en una cordada.</p>
      <svg class="met-grafica" viewBox="0 0 ${W} ${H}" role="img" aria-label="Personas activas por semana, últimas 12 semanas"><line class="met-base" x1="0" x2="${W}" y1="${H}" y2="${H}"/>${barras}</svg>
      <div class="met-ejes"><span>hace 12 semanas</span><span>máx. ${n(max)}</span><span>esta semana</span></div>
      <details class="met-tabla"><summary>Ver como tabla</summary><table><thead><tr><th>Semana</th><th>Activas</th><th>Nuevas</th><th>Publicaciones</th></tr></thead><tbody>
        ${sem.map(x => `<tr><td>${new Date(x.semana + "T12:00").toLocaleDateString("es", { day: "numeric", month: "short" })}</td><td>${x.activas}</td><td>${x.nuevas}</td><td>${x.publicaciones}</td></tr>`).join("")}</tbody></table></details>`) +
    hoja(`<div class="met-cifras">
      ${cifra(n(m.cordadas_activas), "Cordadas activas", "con días marcados en 14 días")}
      ${cifra(n(m.web_vinculada), "Cuentas con la web")}
      ${cifra(n(m.reportes_pendientes), "Reportes sin revisar")}
      ${cifra(n(m.errores_7d), "Errores en 7 días")}
    </div>${tipos.length ? `<p class="suave" style="margin-bottom:0">Publicaciones en 30 días: ${tipos.map(([t, v]) => `${NOMBRE_TIPO[t] || t} ${v}`).join(" · ")}</p>` : ""}
    <p class="suave" style="font-size:12px;margin-bottom:0">Solo totales, calculados con lo que ya está en el servidor. La ruta de cada persona (tramos, milímetros, Brújula) vive en su teléfono y no se mide.</p>`);
}

function vistaErrores() {
  const es = MOD.errores || [];
  if (!es.length) return hoja(`<p class="vacio-mini">Sin errores en los últimos 30 días.</p>`);
  return es.map(e => hoja(`<div class="mod-cab"><span class="etiqueta">${e.origen === "web" ? "Web" : "App"}</span><span class="mod-n${e.veces >= 5 ? " alto" : ""}">${e.veces} ${e.veces === 1 ? "vez" : "veces"}</span></div>
    <div class="mod-autor" style="overflow-wrap:anywhere">${esc(e.firma)}</div>
    <p class="suave" style="font-size:13px;margin:4px 0">${(e.versiones || []).map(esc).join(", ") || "—"} · ${esc(e.equipo || "")} · último: ${fechaMod(e.ultimo)}</p>
    ${e.rastro ? `<details><summary>Rastro</summary><pre class="mod-rastro">${esc(e.rastro)}</pre></details>` : ""}`, "mod-caso")).join("");
}

DET["perfil.moderacion"] = () => {
  if (!MOD.soy) return cab(av(ic("mazo"), "#8A7B70"), "Moderación", "") + cuerpo(hoja(`<p>Esta sección es solo para moderadores.</p>`), "max-width:640px");
  if (MOD.casos === null) cargarModeracion();
  const n = MOD.casos?.length || 0, pest = MOD.pestana;
  const tab = (k, t) => `<button role="tab" aria-selected="${pest === k}" data-acc="pestanaMod" data-arg="${k}">${t}</button>`;
  const tabs = `<div class="mod-tabs" role="tablist">${tab("pendientes", `Pendientes (${n})`)}${tab("historial", "Historial")}${tab("metricas", "Métricas")}${tab("errores", `Errores${MOD.errores?.length ? ` (${MOD.errores.length})` : ""}`)}${tab("planes", "Planes")}
    <button class="btn mini" data-acc="recargarMod" style="margin-left:auto">Actualizar</button></div>`;
  let h;
  if (MOD.casos === null) h = `<p class="vacio-mini">Cargando…</p>`;
  else if (pest === "pendientes") h = n ? MOD.casos.map(tarjetaCaso).join("") : hoja(`<p class="vacio-mini">${ic("mazo")} No hay reportes por revisar. La comunidad está en calma.</p>`);
  else if (pest === "metricas") h = vistaMetricas();
  else if (pest === "errores") h = vistaErrores();
  else if (pest === "planes") h = vistaPlanes();
  else h = (MOD.hist || []).length ? hoja(MOD.hist.map(d => `<div class="mod-hist"><div>@${esc(d.moderador || "—")} · ${ACCION_MOD[d.accion] || d.accion}${!["suspender", "levantar"].includes(d.accion) && TIPO_MOD[d.tipo] ? ` · ${TIPO_MOD[d.tipo].toLowerCase()}` : ""}${d.usuario ? ` · @${esc(d.usuario)}` : ""}</div>
      ${d.nota ? `<div class="suave">${esc(d.nota)}</div>` : ""}<div class="suave" style="font-size:12px">${fechaMod(d.creado)}</div></div>`).join(""))
    : hoja(`<p class="vacio-mini">Todavía no hay decisiones.</p>`);
  return cab(av(ic("mazo"), "var(--burdeos)"), "Moderación", "Reportes, métricas y errores") +
    cuerpo(tabs + (MOD.error ? `<p class="suave" style="color:#B3261E">No se pudo completar. Revisa tu conexión e inténtalo de nuevo.</p>` : "") + h, "max-width:760px");
};

Object.assign(ACC, {
  pestanaMod(el, p) { MOD.pestana = p; pintarDetalle(); },
  recargarMod() { cargarModeracion(); },
  async quitarPlan(el, usuario) {
    if (!el.dataset.seguro) { el.dataset.seguro = "1"; el.textContent = "¿Quitar el plan?"; return; }
    const { error } = await Store.nube.sb.rpc("dar_plan", { p_usuario: usuario, p_dias: 0, p_origen: "regalo", p_nota: "" });
    toast(error ? "No se pudo quitar. Revisa tu conexión." : `Plan quitado a @${usuario}`); await cargarModeracion();
  },
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

/** Formulario "Dar el plan" (lo llama el manejador general de formularios). */
async function enviarDarPlan(f) {
  const d = new FormData(f), usuario = String(d.get("usuario") || "").trim().replace(/^@/, "").toLowerCase();
  if (!usuario) { toast("Escribe el usuario."); return; }
  const boton = f.querySelector("button[type=submit]"); if (boton) boton.disabled = true;
  const { error } = await Store.nube.sb.rpc("dar_plan", { p_usuario: usuario, p_dias: Number(d.get("dias")), p_origen: String(d.get("origen")), p_nota: String(d.get("nota") || "") });
  if (error) { console.error(error); toast(/no existe/.test(error.message || "") ? `No existe la cuenta @${usuario}` : "No se pudo dar el plan. Revisa tu conexión."); if (boton) boton.disabled = false; return; }
  toast(`Plan Cumbre dado a @${usuario}`); await cargarModeracion();
}
