"use strict";
/* ======================================================================
 * Acciones
 * ====================================================================== */
const BASES = {
  perfil: () => perfil(),
  agenda: c => ({ fecha: c }),
  mes: c => ({ clave: c, anio: Number(c.slice(0, 4)), mes: Number(c.slice(5, 7)) }),
  checklist: c => ({ fecha: c, marcados: "" }),
};
const alternar = (xs, x) => xs.includes(x) ? xs.filter(y => y !== x) : [...xs, x];
/** Marca un día en una meta del mes (sin desmarcarlo si ya estaba). */
function marcarDiaMeta(id, fecha) { const m = Store.get("meta_mes", id), d = deIso(fecha).getDate(); if (!m || numeros(m.dias).includes(d)) return;
  Store.cambiar("meta_mes", id, { dias: [...numeros(m.dias), d].sort((x, y) => x - y).join(",") }); }

const ACC = {
  /** Mirar el cielo: la ventana que sigue la orientación del teléfono (o se arrastra en el computador). */
  mirarCielo() { Cielo.abrirVentana(); },
  /** El evento del cielo de hoy, explicado en dos frases (eclipses, lluvias de meteoros, conjunciones…). */
  eventoCielo() {
    const c = Cielo.contar(Cielo.eventoDe(new Date())); if (!c) return;
    ventanaCuenta(`<h3 style="margin-top:0">${esc(c.linea.split(" · ")[0])}</h3><p>${esc(c.explica)}</p>
      <p class="suave" style="font-size:13px">${esc(c.linea.split(" · ").slice(1).join(" · "))}</p>
      <div class="botones"><button class="btn lleno" type="button" id="eventoListo">Entendido</button></div>`, v => { v.querySelector("#eventoListo").onclick = () => v.remove(); });
  },
  /** Quién ve un año del diario de vida (se guarda como respuesta, igual que en la app). */
  /** Cambia el @usuario público. Nunca el correo: la comunidad lo vería. */
  async cambiarUsuario(el) {
    const u = el.value.toLowerCase().replace(/[^a-z0-9._]/g, ""), antes = Store.nube?.perfil?.username || "";
    el.value = u;
    if (!Store.nube || u === antes) return;
    if (!/^[a-z0-9._]{3,30}$/.test(u)) { toast("El usuario debe tener de 3 a 30 letras minúsculas, números, punto o guion bajo."); el.value = antes; return; }
    if (pareceCorreo(u, Store.nube.correo)) { toast("No uses tu correo como usuario: toda la comunidad lo vería."); el.value = antes; return; }
    const { error } = await Store.nube.sb.from("profiles").update({ username: u }).eq("id", Store.nube.dueno);
    if (error) { el.value = antes; toast(/duplicate|unique/i.test(error.message) ? "Ese usuario ya existe. Prueba con otro." : "No se pudo cambiar el usuario." + detalleError(error)); return; }
    Store.nube.perfil.username = u; estado.feed = null; toast("Ahora eres @" + u); pintarLista?.();
  },
  /** Quita la foto de una casilla del vision board (la publicación privada se conserva en el diario). */
  async quitarFotoVision(_, id) { await Store.cambiar("vision", Number(id) || id, { publicacionId: null }); toast("Foto quitada de la casilla"); pintarDetalle(); },
  /** Abierta → En camino → Lograda. Al pasar a En camino se abre el ajuste del %. Se guarda como respuesta: viaja a la app. */
  estadoCasilla(_, id) {
    const c = Store.get("vision", Number(id)) || Store.get("vision", id); if (!c) return;
    const sig = { ABIERTA: "CAMINO", CAMINO: "LOGRADA", LOGRADA: "ABIERTA" }[avanceCasilla(c).estado];
    responder("vision", `vision:estado:${id}`, sig);
    if (sig === "CAMINO") { if (!(Number(respuesta(`vision:pct:${id}`)) > 0)) responder("vision", `vision:pct:${id}`, "25"); estado.pctCasilla = String(id); }
    if (sig === "ABIERTA") { responder("vision", `vision:pct:${id}`, ""); estado.pctCasilla = null; }
    if (sig === "LOGRADA") estado.pctCasilla = null;
    pintarDetalle();
  },
  pctCasilla(_, id) { estado.pctCasilla = estado.pctCasilla === String(id) ? null : String(id); pintarDetalle(); },
  pctCasillaVer(el) { const v = el.value; el.nextElementSibling.textContent = v + "%"; const a = el.closest(".casilla-v")?.querySelector(".pct-m3"); if (a) { a.style.setProperty("--p", v); a.lastChild.textContent = v + "%"; } },
  pctCasillaGuardar(el, id) {
    const v = Number(el.value);
    responder("vision", `vision:pct:${id}`, v ? String(v) : "");
    responder("vision", `vision:estado:${id}`, v >= 100 ? "LOGRADA" : v > 0 ? "CAMINO" : "ABIERTA");
    pintarDetalle();
  },
  albumAbrir(_, a) { estado.albumAbierto = a ? Number(a) : null; pintarDetalle(); },
  async diarioVis(_, arg) {
    const [a, vis] = arg.split("|");
    await responder("diario", `diario-${a}#visibilidad`, vis);
    if (Store.nube) try { await Store.nube.sb.from("diario_anios").upsert({ user_id: Store.nube.dueno, anio: Number(a), visibilidad: vis }); } catch (e) { console.error(e); }
    toast(vis === "PRIVADA" ? "Ahora solo tú ves este año." : "Listo: este año se ve en tu diario de vida.");
  },
  agua(_, arg) {
    const [fecha, n] = arg.split("|");
    Store.cambiar("agenda", fecha, { agua: Math.max(0, Math.min(8, Number(n))) }, BASES.agenda(fecha));
  },
  /** Tocar una publicación del muro la abre en Cimas, a pantalla completa, desde esa misma publicación. */
  verCima(_, id) {
    estado.vistaCom = "cimas"; elegir("feed");
    requestAnimationFrame(() => document.querySelector(`.cima[data-cima="${CSS.escape(id)}"]`)?.scrollIntoView({ block: "start" }));
  },
  horizonte(_, arg) {
    const [tipo, clave, d] = arg.split("|");
    const actual = Number(Store.get(tipo, clave)?.horizonte ?? 5);
    const nuevo = acotarHorizonte(d.startsWith("=") ? Number(d.slice(1)) : actual + Number(d));
    Store.cambiar(tipo, clave, { horizonte: nuevo }, BASES[tipo]?.(clave) || {});
  },
  volver() { estado.sel = null; $("app").classList.remove("con-detalle"); pintarLista(); if (innerWidth > 900) pintarDetalle(); },
  tema() {
    const r = document.documentElement, oscuro = r.dataset.theme ? r.dataset.theme === "dark" : matchMedia("(prefers-color-scheme: dark)").matches;
    r.dataset.theme = oscuro ? "light" : "dark";
    try { localStorage.setItem("rutacima-tema", r.dataset.theme); } catch {}
  },
  async salir() {
    if (Store.nube) { mostrarCarga("Cerrando sesión…"); try { await Store.nube.desvincular(); } catch (e) { console.error(e); } location.reload(); }
    else { Store.datos = {}; iniciar(); }
  },
  borrar(el, arg) {
    if (!el.dataset.seguro) {
      el.dataset.seguro = "1"; const antes = el.innerHTML; el.innerHTML = "¿Seguro? Toca otra vez";
      setTimeout(() => { if (el.isConnected) { delete el.dataset.seguro; el.innerHTML = antes; } }, 4000);
      return;
    }
    const [tipo, clave] = arg.split("|");
    if (tipo === "proposito") Store.lista("accion").filter(a => String(a.propositoId) === clave).forEach(a => Store.borrar("accion", a.id));
    Store.borrar(tipo, clave);
    if (String(estado.sel).endsWith(":" + clave)) { estado.sel = null; $("app").classList.remove("con-detalle"); }
    toast("Borrado"); refrescar();
  },
  nuevoProposito(el, eje) {
    const id = nuevoId();
    Store.guardar("proposito", { id, orden: Store.lista("proposito").length, titulo: "Nuevo propósito", metasEspecificas: "", prioridad: "B", eje: NOMBRE_EJE[eje] ? eje : null, descripcion: "", indicadorExito: "", visualizacion: "", porQueImporta: "", impacto: "", reflexionFinal: "", progreso: 0, horizonte: 5, creadoEn: Date.now() });
    ir("metas", "p:" + id); enfocarPrimero();
  },
  nuevaMetaAnio(el, arg) {
    const [anio, prop, eje] = arg.split("|"), id = nuevoId();
    Store.guardar("meta_anio", { id, anio: Number(anio), propositoId: prop ? Number(prop) : null, titulo: "Nueva meta", subMetas: "", decision: "CONTINUAR", prioridad: "B", avance: 0, estado: "NO_INICIADA", obstaculo: "", proximaAccion: "", eje: NOMBRE_EJE[eje] ? eje : null, indicador: "", observable: "" });
    ir("metas", "a:" + id); enfocarPrimero();
  },
  crearMetaCoach(el, id) {
    const msg = Store.get("coach", id); if (!msg) return;
    const [, a] = separarAccion(msg.texto); if (!a) return;
    const hoy = hoyFecha(), anio = hoy.getFullYear(), mes = hoy.getMonth() + 1;
    if (a.tipo === "meta_mes") Store.guardar("meta_mes", { id: nuevoId(), anio, mes, orden: metasDelMes(anio, mes).length, texto: a.texto, dias: "", cumplida: false, metaAnualId: null, eje: a.eje, indicador: "", objetivoDias: Math.min(a.dias, diasEnMes(anio, mes)) });
    else Store.guardar("meta_anio", { id: nuevoId(), anio, propositoId: null, titulo: a.texto, subMetas: "", decision: "CONTINUAR", prioridad: "B", avance: 0, estado: "NO_INICIADA", obstaculo: "", proximaAccion: "", eje: a.eje, indicador: "", observable: "" });
    ocultarLocal("coach-creadas", id); toast("Meta creada"); pintarDetalle();
  },
  nuevaMetaMes(el, arg) {
    const [ym, anual, eje] = arg.split("|"), [a, m] = ym.split("-").map(Number), id = nuevoId();
    Store.guardar("meta_mes", { id, anio: a, mes: m, orden: metasDelMes(a, m).length, texto: "Nueva meta del mes", dias: "", cumplida: false, metaAnualId: anual ? Number(anual) : null, eje: NOMBRE_EJE[eje] ? eje : null, indicador: "", objetivoDias: 20 });
    ir("metas", "m:" + id); enfocarPrimero();
  },
  diaMeta(el, arg) {
    const [id, d] = arg.split("|").map(Number), m = Store.get("meta_mes", id); if (!m) return;
    Store.cambiar("meta_mes", id, { dias: alternar(numeros(m.dias), d).sort((x, y) => x - y).join(",") });
  },
  /** Mueve la línea de vida una pantalla a la izquierda o a la derecha. */
  lvMover(el, d) { const c = el.closest(".lv")?.querySelector(".lv-cinta"); c?.scrollBy({ left: Number(d) * c.clientWidth * 0.8, behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth" }); },
  /** Un anhelo se vuelve meta de ese año (con el título ya escrito). */
  anheloAMeta(_, arg) {
    const i = arg.indexOf("|"), anio = Number(arg.slice(0, i)), titulo = arg.slice(i + 1).slice(0, 120), id = nuevoId();
    Store.guardar("meta_anio", { id, anio, propositoId: null, titulo, subMetas: "", decision: "CONTINUAR", prioridad: "B", avance: 0, estado: "NO_INICIADA", obstaculo: "", proximaAccion: "", eje: null, indicador: "", observable: "" });
    toast(`“${titulo}” ya es meta de ${anio}`); ir("metas", "a:" + id);
  },
  /** Desde una meta: abre Mi vida en el campamento que sostiene, con sus metas por año. */
  irCampamento(_, i) { estado.vidaCamp = Number(i); ir("ruta", "vida"); requestAnimationFrame(() => document.querySelector(".vv-panel")?.scrollIntoView({ block: "center" })); },
  /* Actividades del día: texto en el horario (igual que la app), meta del mes que mueve y si se hizo */
  actTexto(el, arg) { const [fecha, h] = arg.split("|"), ag = Store.get("agenda", fecha) || {}, hor = horarioDe(ag), t = el.value.trim().slice(0, 200);
    if (t) hor[h] = t; else delete hor[h]; Store.cambiar("agenda", fecha, { horario: JSON.stringify(hor) }, BASES.agenda(fecha)); },
  actMeta(el, arg) { const [fecha, h] = arg.split("|"); responder("dia", `dia#${fecha}#${h}#meta`, el.value);
    if (el.value && respuesta(`dia#${fecha}#${h}#hecho`) === "1") marcarDiaMeta(el.value, fecha); },
  actHecha(el, arg) { const [fecha, h] = arg.split("|"); responder("dia", `dia#${fecha}#${h}#hecho`, el.checked ? "1" : "");
    const meta = respuesta(`dia#${fecha}#${h}#meta`); if (el.checked && meta) { marcarDiaMeta(meta, fecha); toast("El día cuenta en tu meta del mes"); } },
  horasTodas(_, f) { estado.horasTodas = f || null; pintarDetalle(); },
  /* Plan del día: las 3 cumbres */
  topTexto(el, arg) { const [f, i] = arg.split("|"), xs = topDe(f); while (xs.length <= Number(i)) xs.push({ t: "" }); xs[i] = { ...xs[i], t: el.value.trim() }; guardarTop(f, xs); pintarDetalle(); },
  topMeta(el, arg) { const [f, i] = arg.split("|"), xs = topDe(f); if (!xs[i]) return; xs[i].meta = el.value; guardarTop(f, xs); if (xs[i].hecho && el.value) marcarDiaMeta(el.value, f); },
  topHecha(el, arg) { const [f, i] = arg.split("|"), xs = topDe(f); if (!xs[i]) return; xs[i].hecho = el.checked; guardarTop(f, xs);
    if (el.checked && xs[i].meta) { marcarDiaMeta(xs[i].meta, f); toast("¡Cumbre del día! Cuenta en tu meta del mes."); } else if (el.checked) toast("¡Cumbre del día!"); pintarDetalle(); },
  /** Lo pendiente de un día pasa al siguiente: las cumbres no hechas primero, y las actividades a su misma hora. */
  pasarPendientes(desde, hasta) {
    const p = pendientesDe(desde), xs = topDe(hasta);
    p.top.forEach(x => { if (xs.length < 3 && !xs.some(y => y.t === x.t)) xs.push({ t: x.t, meta: x.meta, hecho: false }); });
    guardarTop(hasta, xs);
    const ag = Store.get("agenda", hasta) || {}, hor = horarioDe(ag);
    p.acts.forEach(a => { if (!hor[a.h]) { hor[a.h] = a.t; if (a.meta) responder("dia", `dia#${hasta}#${a.h}#meta`, a.meta); } });
    Store.cambiar("agenda", hasta, { horario: JSON.stringify(hor) }, BASES.agenda(hasta));
    return p.top.length + p.acts.length;
  },
  traerPendientes(_, f) { const n = ACC.pasarPendientes(sumarDias(f, -1), f); responder("dia", `dia#${f}#traido`, "1"); toast(`${n} pendientes traídos de ayer`); pintarDetalle(); },
  traerNo(_, f) { responder("dia", `dia#${f}#traido`, "1"); pintarDetalle(); },
  pasarAManana(_, f) { const n = ACC.pasarPendientes(f, sumarDias(f, 1)); responder("dia", `dia#${sumarDias(f, 1)}#traido`, "1"); toast(`${n} pendientes pasan a mañana`); pintarDetalle(); },
  cartaEditar(_, a) { estado.cartaEditar = a ? Number(a) : null; pintarDetalle(); if (a) requestAnimationFrame(() => document.querySelector(".carta-area")?.focus()); },
  habEditar() { estado.habEditar = !estado.habEditar; pintarDetalle(); },
  habRenombrar(el, id) { const t = el.value.trim().slice(0, 120); if (!t) { el.value = habitos().find(h => h[0] === id)?.[2] || ""; return; } guardarHabitos(habitos().map(h => h[0] === id ? [h[0], h[1], t] : h)); },
  habQuitar(_, id) { const l = habitos(), h = l.find(x => x[0] === id); if (!h || l.filter(x => x[1] === h[1]).length <= 1) return; guardarHabitos(l.filter(x => x[0] !== id)); pintarDetalle(); },
  habAgregar(_, eje) {
    const inp = document.getElementById("habNuevo-" + eje), t = inp?.value.trim().slice(0, 120), l = habitos();
    if (!t || l.filter(h => h[1] === eje).length >= HABITOS_MAX) return;
    let id; do { id = eje + "-" + Date.now().toString(36) + l.length; } while (l.some(h => h[0] === id));
    const i = l.map(h => h[1]).lastIndexOf(eje), n = [...l]; n.splice(i + 1, 0, [id, eje, t]); guardarHabitos(n); pintarDetalle();
    requestAnimationFrame(() => document.getElementById("habNuevo-" + eje)?.focus());
  },
  habFabrica(_, v) { if (v !== "si") { estado.habFabrica = true; pintarDetalle(); return; } estado.habFabrica = false; guardarHabitos(HABITOS_BASE); pintarDetalle(); },
  habito(el, arg) {
    const [fecha, id] = arg.split("|"), nuevo = alternar([...marcadosDe(fecha)], id);
    // Al completar el eje abierto, se vuelve a seguir al siguiente que falta
    const hs = habitos(), eje = hs.find(h => h[0] === id)?.[1];
    if (el.checked && eje && hs.filter(h => h[1] === eje).every(h => nuevo.includes(h[0]))) { estado.ejeAbierto = null; navigator.vibrate?.(8); }
    Store.cambiar("checklist", fecha, { marcados: nuevo.sort().join(",") }, BASES.checklist(fecha));
  },
  energia(el, arg) {
    const [fecha, n] = arg.split("|"); const ag = Store.get("agenda", fecha) || {};
    Store.cambiar("agenda", fecha, { energia: (ag.energia || 0) === Number(n) ? Number(n) - 1 : Number(n) }, BASES.agenda(fecha));
  },
  escala(el, arg) { const [wb, id, v] = arg.split("|"); responder(wb, id, respuesta(id) === v ? "" : v); },
  opcion(el, arg) { const [wb, id, ...o] = arg.split("|"); const v = o.join("|"); responder(wb, id, respuesta(id) === v ? "" : v); },
  checkResp(el, arg) { const [wb, id] = arg.split("|"); responder(wb, id, el.checked ? "1" : ""); },
  filaMas(el, arg) { const [wb, id, n] = arg.split("|"); responder(wb, id + "#n", n); },
  seccion(el, arg) {
    const [wb, k] = arg.split("|"); estado.wb[wb] = Number(k);
    responder(wb, `${wb}#ultima`, String(k)); pintarDetalle();
    $("detalle").querySelector(".det-cuerpo")?.scrollTo(0, 0);
  },
  romper(el, fecha) {
    const d = deIso(fecha), anio = d.getFullYear();
    const guardar = () => Store.guardar("frases", { dias: [...abiertas(anio), fraseIndice(d)].sort((x, y) => x - y) }, String(anio));
    const img = $("tarjetaFrase")?.querySelector(".lacre");
    el.disabled = true;
    // Misma semilla que la app: el sello de hoy se parte igual en el teléfono y aquí
    if (img?.complete && img.naturalWidth) romperSello(img, anio * 1000 + fraseIndice(d), () => setTimeout(() => { $("tarjetaFrase")?.classList.add("rota"); setTimeout(guardar, 350); }, 220));
    else { $("tarjetaFrase")?.classList.add("rota"); setTimeout(guardar, 350); }
  },
  /** Compartir una publicación: la hoja del sistema en el teléfono; en el computador, se copia el texto. */
  async compartirPost(el, id) {
    const p = (estado.feed || []).find(x => String(x.id) === id); if (!p) return;
    const texto = [p.metaTitulo, p.texto, "— Rutaalacima"].filter(Boolean).join("\n\n");
    try { if (navigator.share) { await navigator.share({ text: texto }); return; } } catch { return; }
    try { await navigator.clipboard.writeText(texto); toast("Copiado: pégalo donde quieras compartirlo."); } catch {}
  },
  /** Comentarios de una cima en una hoja inferior: se lee y se escribe sin salir del feed. */
  async comentariosCima(el, id) {
    const p = (estado.feed || []).find(x => String(x.id) === id); if (!p) return;
    estado.post = p; pintarHojaComentarios(p, true);
    if (!p.comentariosLista) { await cargarComentarios(p); pintarHojaComentarios(p); }
  },
  /** Más de una cima: copiar el texto, reportar o bloquear (siempre a un toque). */
  masCima(el, id) {
    const p = (estado.feed || []).find(x => String(x.id) === id); if (!p) return;
    estado.post = p;
    abrirHoja(`<h3 class="hoja-titulo">Opciones</h3><div class="hoja-lista">
      <button data-acc="copiarCima" data-arg="${esc(p.id)}">${ic("copiar2")}<span>Copiar el texto</span></button>
      ${p.propio ? "" : `<button data-acc="reportarPost" data-arg="${esc(p.id)}">${ic("reportar")}<span>Reportar publicación</span></button>
      <button class="peligro" data-acc="bloquearAutor" data-arg="${esc(p.id)}">${ic("bloquear")}<span>Bloquear a ${esc(p.autorNombre)}</span></button>`}</div>`);
  },
  async copiarCima(el, id) {
    const p = (estado.feed || []).find(x => String(x.id) === id); if (!p) return;
    try { await navigator.clipboard.writeText([p.metaTitulo, p.texto].filter(Boolean).join("\n\n")); toast("Texto copiado."); } catch {}
    cerrarHoja();
  },
  /** "más" / "menos" en el texto de una cima */
  masTexto(el) { const pie = el.closest(".cima-pie"); const abierto = pie.classList.toggle("abierto"); el.textContent = abierto ? "menos" : "más"; el.setAttribute("aria-expanded", abierto); },
  impulsarId(el, id) { const p = (estado.feed || []).find(x => String(x.id) === id); if (p) { estado.post = p; ACC.impulsar(); } },
  async impulsar() {
    const p = estado.post; if (!p) return;
    p.yoImpulse = !p.yoImpulse; p.impulsos += p.yoImpulse ? 1 : -1; refrescar();
    if (p.demo || !Store.nube) return;
    const sb = Store.nube.sb, yo = Store.nube.dueno;
    const r = p.yoImpulse ? await sb.from("votes").insert({ post_id: p.id, user_id: yo }) : await sb.from("votes").delete().eq("post_id", p.id).eq("user_id", yo);
    if (r.error) errorNube(r.error);
  },
  async seguir(el, autor) {
    const r = await Store.nube.sb.from("follows").upsert({ follower_id: Store.nube.dueno, followed_id: autor });
    if (r.error) errorNube(r.error); else { el.textContent = "Siguiendo"; el.disabled = true; }
  },
  async reportarPost(el, id) {
    const p = (estado.feed || []).find(x => String(x.id) === id) || estado.post; if (!p) return;
    const motivo = prompt("¿Por qué reportas esta publicación? (spam, ofensiva, acoso, engaño…)\nDejarás de verla. Si varias personas la reportan, se oculta para todos hasta revisarla.");
    if (motivo === null) return;
    if (Store.nube && !p.demo) {
      const r = await Store.nube.sb.from("reportes").insert({ quien: Store.nube.dueno, a_quien: p.autorId, post_id: p.id, motivo: (motivo || "Sin detalle").slice(0, 1000) });
      if (r.error && r.error.code !== "23505") { errorNube(r.error); return; }
    }
    ocultarLocal("ocultos", p.id); cerrarHoja(); toast("Gracias por cuidar la comunidad: la revisaremos y ya no la verás."); estado.feed = null; ir("comunidad", estado.vistaCom === "cimas" ? "feed" : null);
  },
  async bloquearAutor(el, id) {
    const p = (estado.feed || []).find(x => String(x.id) === id) || estado.post; if (!p) return;
    if (!confirm(`¿Bloquear a ${p.autorNombre}? No verán sus publicaciones ni comentarios, no podrán escribirse y dejarán de seguirse. Esta persona no recibe ningún aviso.`)) return;
    if (Store.nube && !p.demo) {
      const r = await Store.nube.sb.from("bloqueos").upsert({ quien: Store.nube.dueno, a_quien: p.autorId });
      if (r.error) { errorNube(r.error); return; }
    }
    ocultarLocal("bloqueados", p.autorId); cerrarHoja(); toast(`Bloqueaste a ${p.autorNombre}. Ya no verás sus publicaciones.`); estado.feed = null; ir("comunidad", estado.vistaCom === "cimas" ? "feed" : null);
  },
  async eliminarCuenta() {
    const palabra = prompt("Esto borra para siempre tu cuenta y todo lo que está en el servidor. Lo guardado en tu teléfono se queda ahí.\n\nEscribe ELIMINAR para confirmar:");
    if (!palabra || palabra.trim().toUpperCase() !== "ELIMINAR") return;
    mostrarCarga("Eliminando tu cuenta…");
    const { error } = await Store.nube.sb.rpc("eliminar_mi_cuenta");
    if (error) { console.error(error); abrirApp(); toast("No se pudo eliminar la cuenta. Revisa tu conexión."); return; }
    try { await Store.nube.sb.auth.signOut(); } catch {}
    mostrarCarga("Tu cuenta fue eliminada."); setTimeout(() => location.reload(), 2500);
  },
  async borrarPost(el, id) {
    if (!el.dataset.seguro) return ACC.borrar(el, ""), undefined;
    if (Store.nube) { const r = await Store.nube.sb.from("posts").delete().eq("id", id); if (r.error) return errorNube(r.error); }
    else misPostsDemo.splice(misPostsDemo.findIndex(p => p.id === id), 1);
    estado.feed = null; estado.misPosts = null; estado.sel = null; toast("Publicación borrada"); pintarTodo();
  },
  borrarCoach(el) {
    if (!el.dataset.seguro) { el.dataset.seguro = "1"; el.textContent = "¿Borrar la conversación?"; return; }
    mensajesCoach().forEach(m => Store.borrar("coach", m.id));
  },
  evalEje(el, k) { estado.evalNueva[k] = Number(el.value); el.nextElementSibling.textContent = el.value; },
  armarVision() { armarVision(); },
  casillaNueva() {
    const orden = Math.max(-1, ...casillasVision().map(c => c.orden || 0)) + 1;
    Store.guardar("vision", { id: nuevoId(), orden, titulo: "Nueva casilla", afirmacion: "Escribe aquí tu frase", eje: null, sugerencia: "", busqueda: "", origen: "manual", publicacionId: null });
    // El nuevo campamento llega al 9×9 con sus 8 pasos sugeridos
    Promise.resolve(window.llenarCampamentosVacios?.()).then(() => pintarDetalle());
  },
  guardarEval() {
    const v = estado.evalNueva;
    Store.guardar("ejes", { id: nuevoId(), fecha: Date.now(), ...v, origen: "web", nota: "" });
    estado.evalNueva = null; toast("Evaluación guardada"); pintarDetalle();
  },
};
function enfocarPrimero() { setTimeout(() => { const i = $("detalle").querySelector("input[type=text]"); if (i) { i.focus(); i.select(); } }, 30); }

/* Eventos (delegados) */
function clic(e) {
  const t = e.target.closest("button, [data-sel]"); if (!t || t.disabled) return;
  if (t.dataset.acc && !(t.matches("input"))) { e.preventDefault(); ACC[t.dataset.acc]?.(t, t.dataset.arg || ""); return; }
  if (t.dataset.ir) { ir(t.dataset.ir, t.dataset.arg || null); return; }
  if (t.dataset.pregunta) { preguntar(t.dataset.pregunta); return; }
  if (t.dataset.filtro) { estado.filtro = t.dataset.filtro; pintarLista(); return; }
  if (t.dataset.feed) { estado.feedFiltro = t.dataset.feed; estado.feed = null; pintarLista(); if (estado.sel === "feed") pintarDetalle(); return; }
  if (t.dataset.vista) {
    // Volver de Cimas al muro, en la publicación que estabas viendo
    const actual = [...document.querySelectorAll(".cima")].find(c => { const r = c.getBoundingClientRect(); return r.top >= -r.height / 2 && r.top < innerHeight / 2; })?.dataset.cima;
    estado.vistaCom = t.dataset.vista; elegir("feed");
    if (actual) requestAnimationFrame(() => document.querySelector(`[data-acc="verCima"][data-arg="${CSS.escape(actual)}"]`)?.closest("article")?.scrollIntoView({ block: "center" }));
    return;
  }
  // La Comunidad se abre a pantalla completa (Cimas), en el teléfono y en el computador
  if (t.dataset.sec === "comunidad") { estado.vistaCom = "cimas"; ir("comunidad", "feed"); return; }
  if (t.dataset.sec) { ir(t.dataset.sec, innerWidth > 900 && t.dataset.sec === "coach" ? "coach" : null); return; }
  if (t.dataset.sel?.startsWith("ir-")) { const d = t.dataset.sel.slice(3); if (d === "coachvida") ir("mensajes", "coachvida"); else ir(d, d === "coach" ? "coach" : null); return; }
  if (t.dataset.sel !== undefined) elegir(t.dataset.sel);
}
function cambio(e) {
  const el = e.target;
  if (el.dataset.accChange) { ACC[el.dataset.accChange]?.(el, el.dataset.arg || ""); return; }
  if (el.dataset.fotoVision) { ponerFotoVision(el.dataset.fotoVision, el.files?.[0]); return; }
  if (el.dataset.fotoAnio) { fotoVisionAnio(el.dataset.fotoAnio, el.files?.[0]); el.value = ""; return; }
  if (el.dataset.resp) { const [wb, clave] = el.dataset.resp.split("|"); responder(wb, clave, el.value.trim() ? el.value : ""); return; }
  if (!el.dataset.bind) return;
  const [tipo, clave, nombre, conv] = el.dataset.bind.split("|");
  let v = el.type === "checkbox" ? el.checked : el.value;
  if (conv === "num") v = Number(v);
  else if (conv === "horiz") { v = acotarHorizonte(v); el.value = v; }
  else if (conv === "numNulo") v = v === "" ? null : Number(v);
  else if (conv === "nulo") v = v === "" ? null : v;
  else if (conv === "bool") v = !!el.checked;
  const extra = tipo === "vision" && nombre === "afirmacion" ? { busqueda: palabrasClave(v) } : {};
  Store.cambiar(tipo, clave, { [nombre]: v, ...extra }, BASES[tipo]?.(clave) || {});
}
function entrada(e) {
  const el = e.target;
  if (el.id === "busq") { estado.busqueda = el.value; pintarLista(); const b = $("busq"); b.focus(); b.setSelectionRange(b.value.length, b.value.length); return; }
  if (el.dataset.accInput) { ACC[el.dataset.accInput]?.(el, el.dataset.arg || ""); return; }
  if (el.type === "range" && el.nextElementSibling?.tagName === "B") el.nextElementSibling.textContent = el.value;
}
async function enviar(e) {
  const f = e.target.closest("form[data-form]"); if (!f) return;
  e.preventDefault();
  const datos = new FormData(f), texto = String(datos.get("texto") || "").trim();
  switch (f.dataset.form) {
    case "coach": if (texto) { f.reset(); preguntar(texto); } break;
    case "mensaje": await enviarMensaje(f, texto); break;
    case "suspender": await enviarSuspension(f); break;
    case "darPlan": await enviarDarPlan(f); break;
    case "crearCordada": case "unirseCordada": case "notaCordada": case "agendarSesion": await enviarCordadas(f, datos, texto); break;
    case "postular": {
      const esp = String(datos.get("especialidad") || "").trim(); if (!texto || !esp) break;
      await hacer(() => Msj.postularme(texto, esp), "Postulación enviada"); MS.cv = null; cargarCoachVida();
    } break;
    case "nuevaAccion": if (texto) {
      const pid = Number(f.dataset.arg);
      await Store.guardar("accion", { id: nuevoId(), propositoId: pid, orden: Store.lista("accion").filter(a => a.propositoId === pid).length, texto, hecha: false });
    } break;
    case "comentar": {
      const p = estado.post; if (!texto || !p) break;
      f.reset();
      if (p.demo || !Store.nube) (demoComentarios[p.id] ||= []).push({ autor: perfil().nombre || "Yo", texto });
      else { const r = await Store.nube.sb.from("comments").insert({ post_id: p.id, user_id: Store.nube.dueno, texto }); if (r.error) { errorNube(r.error); break; } }
      p.comentariosLista = null; await cargarComentarios(p); globalThis.pintarHojaComentarios?.(p);
    } break;
    case "publicar": {
      if (!texto) break;
      const boton = f.querySelector("button[type=submit]"); boton.disabled = true; boton.textContent = "Publicando…";
      const post = { tipo: datos.get("tipo"), eje: datos.get("eje") || null, visibilidad: datos.get("visibilidad"), meta_titulo: datos.get("meta") || "", texto, anio: new Date().getFullYear() };
      try {
        if (Store.nube) {
          const sb = Store.nube.sb, id = crypto.randomUUID(), foto = datos.get("foto");
          let url = "";
          if (foto && foto.size) {
            const ruta = `${Store.nube.dueno}/${id}.jpg`;
            const up = await sb.storage.from("media").upload(ruta, foto, { upsert: true, contentType: foto.type || "image/jpeg" });
            if (up.error) throw up.error;
            url = sb.storage.from("media").getPublicUrl(ruta).data.publicUrl;
          }
          const r = await sb.from("posts").insert({ id, user_id: Store.nube.dueno, image_url: url, ...post });
          if (r.error) throw r.error;
        } else {
          misPostsDemo.unshift({ id: "mio-" + nuevoId(), autorNombre: perfil().nombre || "Yo", tipo: post.tipo, eje: post.eje, texto, metaTitulo: post.meta_titulo, impulsos: 0, comentarios: 0, yoImpulse: false, creadoEn: Date.now(), propio: true, demo: false });
        }
        estado.feed = null; estado.misPosts = null; toast("Publicado"); ir("comunidad", null);
      } catch (err) { errorNube(err); boton.disabled = false; boton.textContent = "Publicar"; }
    } break;
  }
}

/* ---------- Hoja inferior (Material 3): se abre desde abajo, se cierra con el velo, Esc o arrastrando ---------- */
function abrirHoja(html) {
  cerrarHoja(true);
  const h = document.createElement("div");
  h.id = "hojaInferior"; h.className = "hoja-inferior"; h.setAttribute("role", "dialog"); h.setAttribute("aria-modal", "true");
  h.innerHTML = `<div class="hoja-velo" data-cerrar-hoja></div><section class="hoja-panel"><span class="hoja-asa" aria-hidden="true"></span>${html}</section>`;
  document.body.append(h);
  // La hoja vive fuera de la app: recibe los mismos clics y envíos que el detalle
  h.onclick = e => { if (e.target.closest("[data-cerrar-hoja]")) { cerrarHoja(); return; } clic(e); };
  h.onsubmit = enviar;
  h.addEventListener("keydown", e => { if (e.key === "Escape") cerrarHoja(); });
  // Arrastrar hacia abajo para cerrar
  const panel = h.querySelector(".hoja-panel"); let y0 = null;
  // Con el teclado abierto, la hoja sube y se apoya encima de él (lo que se escribe siempre se ve)
  const vv = window.visualViewport;
  if (vv) {
    const ajustar = () => { const tapado = Math.max(0, innerHeight - vv.height - vv.offsetTop); h.style.bottom = tapado + "px"; panel.style.maxHeight = `${Math.min(vv.height * 0.85, 640)}px`; };
    vv.addEventListener("resize", ajustar); vv.addEventListener("scroll", ajustar); ajustar();
    h._soltar = () => { vv.removeEventListener("resize", ajustar); vv.removeEventListener("scroll", ajustar); };
  }
  h.addEventListener("focusin", e => { if (e.target.matches("input, textarea")) setTimeout(() => e.target.scrollIntoView({ block: "nearest" }), 250); });
  panel.addEventListener("touchstart", e => { if (panel.scrollTop <= 0) y0 = e.touches[0].clientY; }, { passive: true });
  panel.addEventListener("touchmove", e => { if (y0 !== null) { const d = Math.max(0, e.touches[0].clientY - y0); panel.style.transform = `translateY(${d}px)`; } }, { passive: true });
  panel.addEventListener("touchend", e => { if (y0 === null) return; const d = e.changedTouches[0].clientY - y0; y0 = null; if (d > 90) cerrarHoja(); else panel.style.transform = ""; });
  (h.querySelector("input, textarea, button:not([data-cerrar-hoja])") || panel).focus?.();
  return h;
}
function cerrarHoja(inmediato = false) {
  const h = document.getElementById("hojaInferior"); if (!h) return;
  h._soltar?.();
  if (inmediato || matchMedia("(prefers-reduced-motion: reduce)").matches) { h.remove(); return; }
  h.classList.add("saliendo"); setTimeout(() => h.remove(), 220);
}
/** Pinta (o repinta) la hoja de comentarios de una publicación. */
function pintarHojaComentarios(p, abrir = false) {
  const lista = p.comentariosLista;
  const cuerpo = !lista ? `<p class="suave hoja-vacia">Cargando…</p>` : !lista.length ? `<p class="suave hoja-vacia">Aún no hay comentarios. Sé la primera persona en dar ánimo.</p>`
    : `<div class="hoja-comentarios">${lista.map(c => { const ini = (c.autor || "?").split(" ").map(x => x[0]).join("").slice(0, 2).toUpperCase();
        return `<div class="comentario"><span class="avatar">${esc(ini)}</span><div><b>${esc(c.autor)}</b><p>${esc(c.texto)}</p></div></div>`; }).join("")}</div>`;
  const html = `<h3 class="hoja-titulo">${p.comentarios || (lista || []).length} comentarios</h3>${cuerpo}
    <form class="escribir hoja-escribir" data-form="comentar"><input name="texto" placeholder="Escribe un comentario" aria-label="Comentario" autocomplete="off" maxlength="1000"><button class="enviar" aria-label="Enviar">${ic("enviar")}</button></form>`;
  const actual = document.getElementById("hojaInferior");
  if (abrir || !actual) { abrirHoja(html); return; }
  actual.querySelector(".hoja-panel").innerHTML = `<span class="hoja-asa" aria-hidden="true"></span>${html}`;
  actual.querySelector("input")?.focus();
}
globalThis.pintarHojaComentarios = pintarHojaComentarios;


/* ---------- Vision board: arrastrar las casillas para cambiar el orden (ratón, dedo o flechas) ---------- */
function reordenarCasillas(idOrigen, idDestino) {
  const cs = casillasVision(), de = cs.findIndex(c => String(c.id) === String(idOrigen)), a = cs.findIndex(c => String(c.id) === String(idDestino));
  if (de < 0 || a < 0 || de === a) return;
  const [m] = cs.splice(de, 1); cs.splice(a, 0, m);
  cs.forEach((c, i) => { if ((c.orden || 0) !== i) Store.cambiar("vision", c.id, { orden: i }); });
  pintarDetalle();
  requestAnimationFrame(() => document.querySelector(`[data-mover-casilla="${idOrigen}"]`)?.focus());
}
document.addEventListener("pointerdown", e => {
  const asa = e.target.closest?.("[data-mover-casilla]"); if (!asa || e.button > 0) return;
  e.preventDefault();
  const tarjeta = asa.closest(".casilla-v"), id = asa.dataset.moverCasilla, tablero = tarjeta.parentElement;
  const r = tarjeta.getBoundingClientRect(), dx = e.clientX - r.left, dy = e.clientY - r.top;
  const fantasma = tarjeta.cloneNode(true); fantasma.classList.add("casilla-fantasma");
  Object.assign(fantasma.style, { width: r.width + "px", height: r.height + "px", left: r.left + "px", top: r.top + "px" });
  document.body.appendChild(fantasma); tarjeta.classList.add("arrastrando");
  let destino = null;
  const mover = ev => {
    fantasma.style.left = ev.clientX - dx + "px"; fantasma.style.top = ev.clientY - dy + "px";
    fantasma.hidden = true; const t = document.elementFromPoint(ev.clientX, ev.clientY)?.closest(".casilla-v"); fantasma.hidden = false;
    const nuevo = t && t.parentElement === tablero && t !== tarjeta ? t : null;
    if (nuevo !== destino) { destino?.classList.remove("destino"); destino = nuevo; destino?.classList.add("destino"); }
    const caja = document.querySelector(".cuerpo") || document.scrollingElement;   // desplaza si se acerca al borde
    if (ev.clientY < 90) caja.scrollBy(0, -12); else if (ev.clientY > innerHeight - 60) caja.scrollBy(0, 12);
  };
  const soltar = () => {
    removeEventListener("pointermove", mover); removeEventListener("pointerup", soltar); removeEventListener("pointercancel", soltar);
    fantasma.remove(); tarjeta.classList.remove("arrastrando"); destino?.classList.remove("destino");
    if (destino) reordenarCasillas(id, destino.dataset.casilla);
  };
  addEventListener("pointermove", mover); addEventListener("pointerup", soltar); addEventListener("pointercancel", soltar);
});
document.addEventListener("keydown", e => {
  const asa = e.target.closest?.("[data-mover-casilla]"); if (!asa) return;
  const paso = { ArrowLeft: -1, ArrowUp: -1, ArrowRight: 1, ArrowDown: 1 }[e.key]; if (!paso) return;
  e.preventDefault();
  const cs = casillasVision(), i = cs.findIndex(c => String(c.id) === asa.dataset.moverCasilla), j = i + paso;
  if (j >= 0 && j < cs.length) reordenarCasillas(cs[i].id, cs[j].id);
});

/* ---------- Línea de vida: centrar el año que se mira y pasar de año con el teclado o deslizando ---------- */
{
  const base = pintarDetalle;
  pintarDetalle = function () {
    base.apply(this, arguments);
    const c = document.querySelector("#detalle .lv-cinta"), a = c?.querySelector(".actual") || c?.querySelector(".hoy");
    if (c && a) c.scrollLeft = a.offsetLeft - (c.clientWidth - a.offsetWidth) / 2;
  };
  const anioVisto = () => { const s = String(estado.sel || ""); return estado.sec === "ruta" && s.startsWith("a:") ? Number(s.slice(2)) : null; };
  const escribiendo = el => el && (el.closest("input, textarea, select, [contenteditable]") || el.closest(".lv-cinta, .album, .md-visor"));
  addEventListener("keydown", e => {
    const a = anioVisto(); if (a === null || escribiendo(e.target) || e.altKey || e.ctrlKey || e.metaKey) return;
    if (e.key === "ArrowLeft" || e.key === "ArrowRight") { e.preventDefault(); ir("ruta", "a:" + (a + (e.key === "ArrowRight" ? 1 : -1))); }
  });
  let toque = null;
  addEventListener("touchstart", e => { toque = anioVisto() !== null && !escribiendo(e.target) && e.target.closest?.("#detalle") ? { x: e.touches[0].clientX, y: e.touches[0].clientY } : null; }, { passive: true });
  addEventListener("touchend", e => {
    if (!toque) return; const t = e.changedTouches[0], dx = t.clientX - toque.x, dy = t.clientY - toque.y; toque = null;
    const a = anioVisto(); if (a === null || Math.abs(dx) < 70 || Math.abs(dx) < Math.abs(dy) * 2) return;
    ir("ruta", "a:" + (a + (dx < 0 ? 1 : -1)));
  }, { passive: true });
}
