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

const ACC = {
  /** Quién ve un año del diario de vida (se guarda como respuesta, igual que en la app). */
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
  nuevoProposito() {
    const id = nuevoId();
    Store.guardar("proposito", { id, orden: Store.lista("proposito").length, titulo: "Nuevo propósito", metasEspecificas: "", prioridad: "B", eje: null, descripcion: "", indicadorExito: "", visualizacion: "", porQueImporta: "", impacto: "", reflexionFinal: "", progreso: 0, horizonte: 5, creadoEn: Date.now() });
    ir("metas", "p:" + id); enfocarPrimero();
  },
  nuevaMetaAnio(el, arg) {
    const [anio, prop] = arg.split("|"), id = nuevoId();
    Store.guardar("meta_anio", { id, anio: Number(anio), propositoId: prop ? Number(prop) : null, titulo: "Nueva meta", subMetas: "", decision: "CONTINUAR", prioridad: "B", avance: 0, estado: "NO_INICIADA", obstaculo: "", proximaAccion: "", eje: null, indicador: "", observable: "" });
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
    const [ym, anual] = arg.split("|"), [a, m] = ym.split("-").map(Number), id = nuevoId();
    Store.guardar("meta_mes", { id, anio: a, mes: m, orden: metasDelMes(a, m).length, texto: "Nueva meta del mes", dias: "", cumplida: false, metaAnualId: anual ? Number(anual) : null, eje: null, indicador: "", objetivoDias: 20 });
    ir("metas", "m:" + id); enfocarPrimero();
  },
  diaMeta(el, arg) {
    const [id, d] = arg.split("|").map(Number), m = Store.get("meta_mes", id); if (!m) return;
    Store.cambiar("meta_mes", id, { dias: alternar(numeros(m.dias), d).sort((x, y) => x - y).join(",") });
  },
  habito(el, arg) {
    const [fecha, id] = arg.split("|");
    Store.cambiar("checklist", fecha, { marcados: alternar([...marcadosDe(fecha)], id).sort().join(",") }, BASES.checklist(fecha));
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
    ocultarLocal("ocultos", p.id); toast("Gracias. Revisaremos tu reporte."); estado.feed = null; ir("comunidad", null);
  },
  async bloquearAutor(el, id) {
    const p = (estado.feed || []).find(x => String(x.id) === id) || estado.post; if (!p) return;
    if (!confirm(`¿Bloquear a ${p.autorNombre}? No verán sus publicaciones ni comentarios, no podrán escribirse y dejarán de seguirse. Esta persona no recibe ningún aviso.`)) return;
    if (Store.nube && !p.demo) {
      const r = await Store.nube.sb.from("bloqueos").upsert({ quien: Store.nube.dueno, a_quien: p.autorId });
      if (r.error) { errorNube(r.error); return; }
    }
    ocultarLocal("bloqueados", p.autorId); toast(`Bloqueaste a ${p.autorNombre}.`); estado.feed = null; ir("comunidad", null);
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
  if (t.dataset.feed) { estado.feedFiltro = t.dataset.feed; estado.feed = null; pintarLista(); return; }
  if (t.dataset.vista) {
    // Volver de Cimas al muro, en la publicación que estabas viendo
    const actual = [...document.querySelectorAll(".cima")].find(c => { const r = c.getBoundingClientRect(); return r.top >= -r.height / 2 && r.top < innerHeight / 2; })?.dataset.cima;
    estado.vistaCom = t.dataset.vista; elegir("feed");
    if (actual) requestAnimationFrame(() => document.querySelector(`[data-acc="verCima"][data-arg="${CSS.escape(actual)}"]`)?.closest("article")?.scrollIntoView({ block: "center" }));
    return;
  }
  if (t.dataset.sec) { ir(t.dataset.sec, innerWidth > 900 && (t.dataset.sec === "coach" || t.dataset.sec === "comunidad") ? (t.dataset.sec === "coach" ? "coach" : "feed") : null); return; }
  if (t.dataset.sel?.startsWith("ir-")) { const d = t.dataset.sel.slice(3); if (d === "coachvida") ir("mensajes", "coachvida"); else ir(d, d === "coach" ? "coach" : null); return; }
  if (t.dataset.sel !== undefined) elegir(t.dataset.sel);
}
function cambio(e) {
  const el = e.target;
  if (el.dataset.accChange) { ACC[el.dataset.accChange]?.(el, el.dataset.arg || ""); return; }
  if (el.dataset.fotoVision) { ponerFotoVision(el.dataset.fotoVision, el.files?.[0]); return; }
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
      p.comentariosLista = null; cargarComentarios(p);
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
