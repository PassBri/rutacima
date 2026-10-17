"use strict";
/* ======================================================================
 * Mensajes 1 a 1 y coach de vida (las mismas reglas que la app: ver supabase/schema.sql)
 * ====================================================================== */
const iniciales = n => (n || "?").split(" ").map(x => x[0]).join("").slice(0, 2).toUpperCase();
const avPersona = (n, color = "var(--burdeos)") => av(esc(iniciales(n)), color);

/** Lo que ve un coach de quien acompaña (igual que ResumenCoach.kt). */
function resumenDe(filas, hoy = new Date()) {
  const de = t => filas.filter(f => f.tipo === t).map(f => f.datos || {});
  const doc = (t, c) => filas.find(f => f.tipo === t && f.clave === c)?.datos;
  const perfilX = de("perfil")[0] || {};
  const mensuales = de("meta_mes"), anuales = de("meta_anio");
  const avMes = m => m.cumplida ? 1 : Math.min(1, lista(m.dias).length / Math.max(1, m.objetivoDias || 20));
  const prom = xs => xs.reduce((a, b) => a + b, 0) / xs.length;
  const avAnio = a => { const h = mensuales.filter(m => m.metaAnualId != null && m.metaAnualId === a.id); return h.length ? prom(h.map(avMes)) : (a.avance || 0) / 100; };
  const propositos = de("proposito").map(p => {
    const h = anuales.filter(a => a.propositoId != null && a.propositoId === p.id);
    return { titulo: p.titulo, avance: Math.round((h.length ? prom(h.map(avAnio)) : (p.progreso || 0) / 100) * 100) };
  });
  const habitos7 = [6, 5, 4, 3, 2, 1, 0].map(k => { const d = new Date(hoy); d.setDate(d.getDate() - k); return lista(doc("checklist", iso(d))?.marcados).length; });
  const ult = de("ejes").sort((a, b) => (b.fecha || 0) - (a.fecha || 0))[0];
  const vision = de("vision");
  const a = hoy.getFullYear(), m = hoy.getMonth() + 1;
  return {
    nombre: perfilX.nombre || "", cumbre: perfilX.cumbreFrase || "", propositos,
    metasAnio: anuales.filter(x => x.anio === a).map(x => ({ titulo: x.titulo, avance: Math.round(avAnio(x) * 100) })),
    metasMes: mensuales.filter(x => x.anio === a && x.mes === m).map(x => ({ titulo: x.texto, avance: Math.round(avMes(x) * 100) })),
    habitos7, ejes: ult ? ["voluntad", "maestria", "voz", "valor", "evolucion", "trascendencia"].map(k => ult[k] || 0) : null,
    visionConFoto: vision.filter(v => v.publicacionId).length, visionTotal: vision.length,
  };
}

/* ---------- Datos de ejemplo (demostración) ---------- */
const DemoMsj = (() => {
  const h = x => Date.now() - x * 60000;
  const convs = [
    { id: "dc1", otroId: "coach-ana", otroNombre: "Ana Torres", otroUsuario: "anatorres", estado: "aceptada", laInicieYo: true, laBloqueeYo: false, esMiCoach: true, laAcompano: false },
    { id: "dc2", otroId: "demo-6", otroNombre: "Camilo T.", otroUsuario: "camilo", estado: "aceptada", laInicieYo: false, laBloqueeYo: false, esMiCoach: false, laAcompano: false },
    { id: "dc3", otroId: "demo-5", otroNombre: "Sofía L.", otroUsuario: "sofial", estado: "pendiente", laInicieYo: false, laBloqueeYo: false, esMiCoach: false, laAcompano: false },
    { id: "dc4", otroId: "demo-3", otroNombre: "Lucía P.", otroUsuario: "luciap", estado: "aceptada", laInicieYo: false, laBloqueeYo: false, esMiCoach: false, laAcompano: true },
  ];
  const msgs = {
    dc1: [[false, "¡Hola! Vi que esta semana marcaste 14 hábitos de 18 en promedio. ¿Cómo te sentiste?", 1500], [true, "Bien, pero el miércoles me costó mucho levantarme.", 1440],
      [false, "Es normal. Probemos dejar la ropa lista la noche anterior y revisamos el viernes.", 1430], [false, "Recuerda: tu meta del mes va en 60 %. Vas por buen camino.", 95]],
    dc2: [[false, "Gracias por el impulso a mi media maratón 🙌", 300], [true, "¡Te lo merecías! ¿Cuánto tiempo entrenaste?", 290], [false, "Diez meses, 4 días por semana. Empecé caminando.", 280]],
    dc3: [[false, "Hola, me inspiró tu publicación. ¿Puedo preguntarte cómo organizas tus metas del mes?", 40]],
    dc4: [[false, "Ya compartí mi avance contigo. ¿Lo revisamos el jueves?", 600], [true, "Claro, el jueves a las 7.", 590]],
  };
  const mensajes = {};
  Object.entries(msgs).forEach(([c, xs]) => { mensajes[c] = xs.map(([mio, texto, min], i) => ({ id: c + "-" + i, mio, texto, creadoEn: h(min), leido: mio || c !== "dc3" })); });
  mensajes.dc1[3].leido = false;
  const directorio = [
    { userId: "coach-ana", nombre: "Ana Torres", usuario: "anatorres", especialidad: "Hábitos y disciplina", bio: "Acompaño a personas que quieren sostener sus hábitos en el tiempo. Sesiones breves y semanales, con seguimiento de tu checklist.", acompanados: 12 },
    { userId: "coach-martin", nombre: "Martín Ruiz", usuario: "martinruiz", especialidad: "Carrera y propósito", bio: "Te ayudo a aterrizar tu cumbre en metas del año y del mes, y a decidir qué continuar, acelerar o pausar.", acompanados: 8 },
    { userId: "coach-elena", nombre: "Elena Gómez", usuario: "elenag", especialidad: "Finanzas personales", bio: "Para quienes tienen metas de ahorro, deudas o un emprendimiento. Trabajamos el eje Valor paso a paso.", acompanados: 5 },
  ];
  const acomps = [
    { id: "da1", rol: "usuario", otroId: "coach-ana", otroNombre: "Ana Torres", otroUsuario: "anatorres", estado: "activo", comparteAvance: true, especialidad: "Hábitos y disciplina" },
    { id: "da2", rol: "coach", otroId: "demo-3", otroNombre: "Lucía P.", otroUsuario: "luciap", estado: "activo", comparteAvance: true, especialidad: "" },
    { id: "da3", rol: "coach", otroId: "demo-1", otroNombre: "Valentina R.", otroUsuario: "valentina", estado: "solicitado", comparteAvance: false, especialidad: "" },
  ];
  const resumenLucia = {
    nombre: "Lucía P.", cumbre: "Ser una mujer disciplinada que vive de lo que ama", propositos: [{ titulo: "Salud y energía para crear", avance: 54 }, { titulo: "Mi propio estudio de diseño", avance: 31 }],
    metasAnio: [{ titulo: "Crear el hábito de madrugar", avance: 70 }, { titulo: "Conseguir 5 clientes propios", avance: 40 }],
    metasMes: [{ titulo: "Levantarme a las 5:00 20 días", avance: 75 }, { titulo: "Publicar 8 piezas de portafolio", avance: 50 }],
    habitos7: [11, 13, 9, 14, 15, 12, 10], ejes: [8, 6, 5, 6, 7, 7], visionConFoto: 5, visionTotal: 9,
  };
  return { convs, mensajes, directorio, acomps, resumenLucia, ficha: { bio: "", especialidad: "Disciplina", verificado: true } };
})();

const Msj = {
  get real() { return !!Store.nube; },
  async rpc(n, args) { const { data, error } = await Store.nube.sb.rpc(n, args); if (error) throw error; return data; },
  async conversaciones() {
    if (!this.real) return DemoMsj.convs.map(c => {
      const ms = DemoMsj.mensajes[c.id] || [], u = ms[ms.length - 1];
      return { ...c, ultimoTexto: u?.texto || "", ultimoEsMio: !!u?.mio, ultimoEn: u?.creadoEn || Date.now(), noLeidos: ms.filter(m => !m.mio && !m.leido).length, solicitudParaMi: c.estado === "pendiente" && !c.laInicieYo };
    }).filter(c => !(c.estado === "bloqueada" && !c.laBloqueeYo)).sort((a, b) => b.ultimoEn - a.ultimoEn);
    const yo = Store.nube.dueno;
    return (await this.rpc("mis_conversaciones") || []).map(o => ({
      id: o.id, otroId: o.otro, otroNombre: o.otro_nombre || o.otro_usuario || "Senderista", otroUsuario: o.otro_usuario || "", estado: o.estado,
      laInicieYo: o.iniciada_por === yo, laBloqueeYo: o.bloqueada_por === yo, ultimoTexto: o.ultimo_texto || "", ultimoEsMio: o.ultimo_autor === yo,
      ultimoEn: Date.parse(o.ultimo_en) || 0, noLeidos: o.no_leidos || 0, esMiCoach: !!o.es_coach, laAcompano: !!o.es_acompanado,
      solicitudParaMi: o.estado === "pendiente" && o.iniciada_por !== yo,
    }));
  },
  async mensajes(conv) {
    if (!this.real) return [...(DemoMsj.mensajes[conv] || [])];
    const { data, error } = await Store.nube.sb.from("mensajes").select("id,autor,texto,creado").eq("conversacion_id", conv).order("creado").limit(500);
    if (error) throw error;
    return data.map(m => ({ id: m.id, mio: m.autor === Store.nube.dueno, texto: m.texto, creadoEn: Date.parse(m.creado) }));
  },
  async enviar(conv, texto) {
    if (!this.real) {
      (DemoMsj.mensajes[conv] ||= []).push({ id: "m" + nuevoId(), mio: true, texto, creadoEn: Date.now(), leido: true });
      const c = DemoMsj.convs.find(x => x.id === conv);
      if (c && c.estado === "aceptada" && !c.laInicieYo && conv !== "dc3") setTimeout(() => {
        DemoMsj.mensajes[conv].push({ id: "m" + nuevoId(), mio: false, texto: c.esMiCoach ? "¡Gracias por contarme! Lo revisamos juntos en nuestra próxima conversación." : "¡Gracias! Seguimos subiendo 💪", creadoEn: Date.now(), leido: false });
        actualizarMensajes(true);
      }, 2500);
      return;
    }
    const { error } = await Store.nube.sb.from("mensajes").insert({ conversacion_id: conv, autor: Store.nube.dueno, texto: texto.slice(0, 2000) });
    if (error) throw error;
  },
  async abrirCon(otro, nombre = "") {
    if (!this.real) {
      let c = DemoMsj.convs.find(x => x.otroId === otro);
      if (!c) { c = { id: "dn" + nuevoId(), otroId: otro, otroNombre: nombre || "Senderista", otroUsuario: "", estado: DemoMsj.directorio.some(d => d.userId === otro) ? "aceptada" : "pendiente", laInicieYo: true, laBloqueeYo: false, esMiCoach: false, laAcompano: false }; DemoMsj.convs.push(c); }
      return c.id;
    }
    return this.rpc("abrir_conversacion", { otro });
  },
  async responder(conv, aceptar) {
    if (!this.real) { const c = DemoMsj.convs.find(x => x.id === conv); if (c) { c.estado = aceptar ? "aceptada" : "bloqueada"; c.laBloqueeYo = !aceptar; } return; }
    await this.rpc("responder_solicitud", { conv, aceptar });
  },
  async bloquear(conv, bloquear) {
    if (!this.real) { const c = DemoMsj.convs.find(x => x.id === conv); if (c) { c.estado = bloquear ? "bloqueada" : "aceptada"; c.laBloqueeYo = bloquear; } return; }
    await this.rpc("bloquear_conversacion", { conv, bloquear });
  },
  async marcarLeidos(conv) {
    if (!this.real) { (DemoMsj.mensajes[conv] || []).forEach(m => m.leido = true); return; }
    await this.rpc("marcar_leidos", { conv });
  },
  async reportar(otro, conv, motivo) {
    if (!this.real) return;
    const { error } = await Store.nube.sb.from("reportes").insert({ quien: Store.nube.dueno, a_quien: otro, conversacion_id: conv, motivo: motivo.slice(0, 1000) });
    if (error) throw error;
  },
  async directorio() {
    if (!this.real) return DemoMsj.directorio;
    return (await this.rpc("directorio_coaches") || []).map(o => ({ userId: o.user_id, nombre: o.nombre || o.usuario, usuario: o.usuario, bio: o.bio || "", especialidad: o.especialidad || "", acompanados: o.acompanados || 0 }));
  },
  async acompanamientos() {
    if (!this.real) return DemoMsj.acomps.filter(a => a.estado !== "terminado");
    return (await this.rpc("mis_acompanamientos") || []).map(o => ({ id: o.id, rol: o.rol, otroId: o.otro, otroNombre: o.otro_nombre || o.otro_usuario, otroUsuario: o.otro_usuario, estado: o.estado, comparteAvance: !!o.comparte_avance, especialidad: o.especialidad || "" }));
  },
  async solicitarCoach(coach) {
    if (!this.real) { const d = DemoMsj.directorio.find(x => x.userId === coach); DemoMsj.acomps.push({ id: "da" + nuevoId(), rol: "usuario", otroId: coach, otroNombre: d?.nombre || "", otroUsuario: d?.usuario || "", estado: "solicitado", comparteAvance: false, especialidad: d?.especialidad || "" }); return; }
    await this.rpc("solicitar_coach", { coach });
  },
  async responderAcomp(id, aceptar) {
    if (!this.real) { const a = DemoMsj.acomps.find(x => x.id === id); if (a) a.estado = aceptar ? "activo" : "terminado"; return; }
    await this.rpc("responder_acompanamiento", { acomp: id, aceptar });
  },
  async terminarAcomp(id) {
    if (!this.real) { const a = DemoMsj.acomps.find(x => x.id === id); if (a) a.estado = "terminado"; return; }
    await this.rpc("terminar_acompanamiento", { acomp: id });
  },
  async compartirAvance(id, si) {
    if (!this.real) { const a = DemoMsj.acomps.find(x => x.id === id); if (a) a.comparteAvance = si; return; }
    await this.rpc("compartir_avance", { acomp: id, si });
  },
  async miFicha() {
    if (!this.real) return DemoMsj.ficha;
    const { data } = await Store.nube.sb.from("coaches").select("bio,especialidad,verificado").eq("user_id", Store.nube.dueno).maybeSingle();
    return data || null;
  },
  async postularme(bio, especialidad) {
    if (!this.real) { DemoMsj.ficha = { bio, especialidad, verificado: false }; return; }
    await this.rpc("postularme_coach", { bio, especialidad });
  },
  async avanceDe(uid) {
    if (!this.real) return uid === "demo-3" ? DemoMsj.resumenLucia : resumenDe([]);
    const { data, error } = await Store.nube.sb.from("ruta_datos").select("tipo,clave,datos").eq("user_id", uid).limit(1000);
    if (error) throw error;
    return resumenDe(data);
  },
};

/* ---------- Estado y carga ---------- */
const MS = { convs: null, msgs: {}, cv: null, avance: {}, cargando: false };
const noLeidosTotal = () => (MS.convs || []).reduce((s, c) => s + c.noLeidos, 0);
const convActual = () => String(estado.sel || "").startsWith("c:") ? MS.convs?.find(c => c.id === estado.sel.slice(2)) : null;

async function cargarConvs() {
  if (MS.cargando) return; MS.cargando = true;
  await null;
  try { MS.convs = await Msj.conversaciones(); }
  catch (e) { console.error(e); MS.convs ||= []; }
  finally { MS.cargando = false; }
}
async function cargarCoachVida() {
  await null;
  try {
    const [acomps, dir, ficha] = await Promise.all([Msj.acompanamientos(), Msj.directorio(), Msj.miFicha()]);
    MS.cv = { acomps, dir, ficha };
  } catch (e) { console.error(e); MS.cv = { acomps: [], dir: [], ficha: null, error: true }; }
  if (estado.sec === "mensajes") { pintarLista(); if (!enEdicion()) pintarDetalle(); }
}
async function cargarAvance(uid) {
  await null;
  try { MS.avance[uid] = await Msj.avanceDe(uid); } catch (e) { console.error(e); MS.avance[uid] = { error: true }; }
  if (estado.sel === "acomp:" + uid) pintarDetalle();
}
/** Trae conversaciones y mensajes nuevos; el chat se actualiza sin tocar lo que estás escribiendo. */
async function actualizarMensajes(forzar = false) {
  if (!forzar && (document.hidden || estado.sec !== "mensajes")) return;
  await cargarConvs();
  const c = convActual();
  if (c) {
    try { MS.msgs[c.id] = await Msj.mensajes(c.id); } catch (e) { console.error(e); }
    if (c.noLeidos) { await Msj.marcarLeidos(c.id).catch(console.error); c.noLeidos = 0; }
  }
  pintarRiel();
  if (estado.sec !== "mensajes") return;
  pintarLista();
  const chat = $("chatMsj");
  if (chat && c) {
    const cuerpoChat = chat.closest(".det-cuerpo"), alFondo = cuerpoChat.scrollHeight - cuerpoChat.scrollTop - cuerpoChat.clientHeight < 80;
    chat.innerHTML = htmlMensajes(c.id);
    $("pieChat").innerHTML = htmlPieChat(c);
    if (alFondo) cuerpoChat.scrollTop = cuerpoChat.scrollHeight;
  } else if (!enEdicion() && !/^(cordadas|cordada|coachvida|acomp)(:|$)/.test(String(estado.sel || ""))) pintarDetalle();
}
setInterval(() => actualizarMensajes(), 5000);
setInterval(() => { if (Store.nube && estado.sec !== "mensajes" && !document.hidden) cargarConvs().then(pintarRiel); }, 30000);

/* ---------- Lista ---------- */
LISTAS.mensajes = function () {
  let h = cabLista("Mensajes", { buscar: true }) + `<div class="items">`;
  const mio = MS.cv?.acomps.find(a => a.rol === "usuario");
  h += item("coachvida", av(ic("cumbre"), "var(--oro)"), "Coach de vida", "", mio ? `Tu coach: ${esc(mio.otroNombre)}` : "Una persona que te acompaña en tu ascenso");
  const activas = (CK.lista || []).filter(c => !cordadaTerminada(c));
  h += item("cordadas", avCordada(), "Cordadas", "", activas.length ? `${activas.length} ${activas.length === 1 ? "reto activo" : "retos activos"}${activas.some(c => !c.marqueHoy) ? " · marca tu día" : ""}` : "Retos de varios días en grupo",
    activas.some(c => !c.marqueHoy) ? `<span class="badge">${activas.filter(c => !c.marqueHoy).length}</span>` : "");
  if (!CK.lista) cargarCordadas();
  if (!MS.cv) cargarCoachVida();
  if (!MS.convs) { cargarConvs().then(() => { if (estado.sec === "mensajes") { pintarLista(); pintarRiel(); } }); return h + `<p class="vacio-mini">Cargando…</p></div>`; }
  const fila = c => item("c:" + c.id, avPersona(c.otroNombre, c.esMiCoach || c.laAcompano ? "var(--oro)" : "var(--burdeos)"),
    esc(c.otroNombre) + (c.esMiCoach ? ` <span class="pill">Tu coach</span>` : c.laAcompano ? ` <span class="pill">Acompañas</span>` : ""), c.ultimoEn ? hace(c.ultimoEn) : "",
    c.solicitudParaMi ? "Quiere escribirte" : c.estado === "bloqueada" ? "Conversación bloqueada" : !c.ultimoTexto ? "Sin mensajes todavía" : (c.ultimoEsMio ? "Tú: " : "") + esc(c.ultimoTexto),
    c.noLeidos ? `<span class="badge">${c.noLeidos}</span>` : "");
  const vis = MS.convs.filter(c => coincide(c.otroNombre + " " + c.ultimoTexto));
  const sol = vis.filter(c => c.solicitudParaMi), resto = vis.filter(c => !c.solicitudParaMi);
  if (sol.length) { h += grupo("Solicitudes"); sol.forEach(c => h += fila(c)); h += grupo("Conversaciones"); }
  resto.forEach(c => h += fila(c));
  if (!MS.convs.length) h += `<p class="vacio-mini">Aún no tienes conversaciones. Escríbele a alguien desde una publicación de la comunidad o pregúntale a un coach de vida.</p>`;
  return h + `</div>`;
};

/* ---------- Chat ---------- */
function htmlMensajes(id) {
  let ultimoDia = "";
  return (MS.msgs[id] || []).map(m => {
    const d = new Date(m.creadoEn).toLocaleDateString("es", { day: "numeric", month: "long" }); const sep = d !== ultimoDia ? `<span class="fecha-sep">${d}</span>` : ""; ultimoDia = d;
    return sep + `<div class="burbuja ${m.mio ? "yo" : "el"}">${esc(m.texto)}<span class="h">${horaDe(m.creadoEn)}</span></div>`;
  }).join("");
}
function htmlPieChat(c) {
  const ms = MS.msgs[c.id] || [];
  if (c.solicitudParaMi) return `<div class="aviso" style="margin:8px 12px;flex-wrap:wrap"><span style="flex:1;min-width:220px">${esc(c.otroNombre)} quiere escribirte. Si aceptas, podrán conversar; si rechazas, no sabrá nada más de ti.</span>
    <button class="btn mini lleno" data-acc="responderChat" data-arg="${esc(c.id)}|1">Aceptar</button><button class="btn mini" data-acc="responderChat" data-arg="${esc(c.id)}|0">Rechazar</button></div>`;
  if (c.estado === "bloqueada") return `<p class="suave" style="text-align:center;margin:12px">Conversación bloqueada${c.laBloqueeYo ? " · puedes desbloquearla arriba" : ""}.</p>`;
  if (c.estado === "pendiente" && c.laInicieYo && ms.length) return `<p class="suave" style="text-align:center;margin:12px">Tu mensaje llegó como solicitud. Podrás seguir escribiendo cuando la acepte.</p>`;
  return `<form class="escribir" data-form="mensaje" data-arg="${esc(c.id)}"><input name="texto" maxlength="2000" placeholder="${c.estado === "pendiente" ? "Preséntate en un mensaje" : "Escribe un mensaje"}" aria-label="Mensaje" autocomplete="off"><button class="enviar" aria-label="Enviar">${ic("enviar")}</button></form>`;
}
DET["mensajes.c"] = id => {
  const c = MS.convs?.find(x => x.id === id);
  if (!c) { if (!MS.convs) cargarConvs().then(() => pintarDetalle()); return vacioMensajes(); }
  if (!MS.msgs[id]) { MS.msgs[id] = []; actualizarMensajes(true); }
  const etiqueta = c.esMiCoach ? "Tu coach de vida" : c.laAcompano ? "Lo acompañas como coach" : c.otroUsuario ? "@" + esc(c.otroUsuario) : "";
  return cab(avPersona(c.otroNombre, c.esMiCoach || c.laAcompano ? "var(--oro)" : "var(--burdeos)"), esc(c.otroNombre), etiqueta,
    `<span style="display:flex;gap:6px">${c.laAcompano ? `<button class="btn mini" data-sel="acomp:${esc(c.otroId)}">Ver avance</button>` : ""}
      <button class="btn mini" data-acc="bloquearChat" data-arg="${esc(c.id)}|${c.estado === "bloqueada" && c.laBloqueeYo ? 0 : 1}">${c.estado === "bloqueada" && c.laBloqueeYo ? "Desbloquear" : "Bloquear"}</button>
      <button class="btn mini peligro" data-acc="reportarChat" data-arg="${esc(c.id)}">Reportar</button></span>`) +
    `<div class="det-cuerpo abajo"><div class="chat" id="chatMsj">${htmlMensajes(id)}</div></div><div id="pieChat">${htmlPieChat(c)}</div>`;
};
function vacioMensajes() {
  return `<div class="vacio"><div><img src="${LOGO}" alt=""><h2>Mensajes</h2>
    <p>Conversa con otras personas de la comunidad y con tu coach de vida. Si no se siguen mutuamente, el primer mensaje llega como solicitud y la otra persona decide si acepta.</p>
    <p class="pie suave">${ic("candado")} Puedes bloquear o reportar cualquier conversación.</p></div></div>`;
}

/* ---------- Coach de vida ---------- */
const barra = (pct) => `<div class="barra"><i style="width:${Math.max(0, Math.min(100, pct))}%"></i></div>`;
DET["mensajes.coachvida"] = () => {
  const cv = MS.cv;
  const titulo = cab(av(ic("cumbre"), "var(--oro)"), "Coach de vida", "Una persona real que te acompaña en tu ascenso");
  if (!cv) { cargarCoachVida(); return titulo + cuerpo(`<p class="suave">Cargando…</p>`); }
  const mio = cv.acomps.find(a => a.rol === "usuario"), acompanados = cv.acomps.filter(a => a.rol === "coach");
  let h = `<p style="margin:0">Conversa contigo, revisa tu avance si se lo permites y te ayuda a no rendirte.</p>`;
  if (!Store.nube) h += `<div class="aviso">Demostración: coaches y conversaciones de ejemplo.</div>`;
  if (mio) {
    h += `<div class="etiqueta">Mi coach</div>` + hoja(`<div class="autor" style="display:flex;gap:12px;align-items:center">${avPersona(mio.otroNombre, "var(--oro)")}<div><b>${esc(mio.otroNombre)}</b><div class="suave" style="font-size:13px">${esc(mio.especialidad)}</div></div></div>
      ${mio.estado === "solicitado" ? `<p class="suave">Esperando que acepte tu solicitud.</p>` : `
        <div class="botones" style="margin-top:12px"><button class="btn lleno" data-acc="escribirA" data-arg="${esc(mio.otroId)}|${esc(mio.otroNombre)}">${ic("mensajes")} Escribir</button></div>
        <label style="display:flex;align-items:flex-start;gap:12px;margin-top:14px;cursor:pointer"><input type="checkbox" style="width:20px;height:20px;margin-top:2px;flex:none;accent-color:var(--burdeos)" data-acc-change="compartirAvance" data-arg="${esc(mio.id)}" ${mio.comparteAvance ? "checked" : ""}>
          <span><b>Compartir mi avance</b><br><span class="suave" style="font-size:13px">Tu cumbre, propósitos, metas, hábitos de la semana, ejes y vision board. Nunca tus respuestas de los libros ni tus notas.</span></span></label>
        ${mio.estado === "activo" ? `<div class="ses-bloque">${htmlSesiones(mio.id)}</div>` : ""}`}
      <div class="botones" style="margin-top:8px"><button class="btn mini peligro" data-acc="terminarAcomp" data-arg="${esc(mio.id)}">${mio.estado === "solicitado" ? "Cancelar solicitud" : "Terminar acompañamiento"}</button></div>`);
  }
  if (acompanados.length) {
    h += `<div class="etiqueta">Personas que acompañas</div>`;
    acompanados.forEach(a => {
      h += hoja(`<div style="display:flex;gap:12px;align-items:center">${avPersona(a.otroNombre)}<div style="flex:1"><b>${esc(a.otroNombre)}</b><div class="suave" style="font-size:13px">${a.estado === "solicitado" ? "Te pide que seas su coach" : a.comparteAvance ? "Comparte su avance contigo" : "Todavía no comparte su avance"}</div></div>
        ${a.estado === "solicitado" ? `<button class="btn mini lleno" data-acc="responderAcomp" data-arg="${esc(a.id)}|1">Aceptar</button><button class="btn mini" data-acc="responderAcomp" data-arg="${esc(a.id)}|0">Rechazar</button>`
          : `<button class="btn mini" data-sel="acomp:${esc(a.otroId)}">Ver avance</button><button class="btn mini" data-acc="escribirA" data-arg="${esc(a.otroId)}|${esc(a.otroNombre)}">${ic("mensajes")}</button>`}</div>`);
    });
  }
  if (!mio) {
    h += `<div class="etiqueta">Coaches verificados</div>`;
    if (!cv.dir.length) h += `<p class="suave">Aún no hay coaches verificados. Vuelve pronto.</p>`;
    cv.dir.forEach(f => {
      h += hoja(`<div style="display:flex;gap:12px;align-items:center">${avPersona(f.nombre, "var(--oro)")}<div style="flex:1"><b>${esc(f.nombre)}</b> <span class="pill">${esc(f.especialidad)}</span><div class="suave" style="font-size:13px">Acompaña a ${f.acompanados} personas</div></div></div>
        ${f.bio ? `<p style="margin:10px 0 0">${esc(f.bio)}</p>` : ""}
        <div class="botones" style="margin-top:10px"><button class="btn lleno" data-acc="solicitarCoach" data-arg="${esc(f.userId)}">Pedir que sea mi coach</button><button class="btn" data-acc="escribirA" data-arg="${esc(f.userId)}|${esc(f.nombre)}">Preguntar</button></div>`);
    });
  }
  h += `<div class="etiqueta">Ser coach de vida</div>`;
  const fi = cv.ficha;
  h += hoja(fi?.verificado ? `<p style="margin:0">Eres coach verificado. Las personas pueden pedirte que las acompañes desde el directorio.</p>`
    : fi ? `<p style="margin:0">Tu postulación está en revisión. Aparecerás en el directorio cuando el equipo de Rutaalacima la verifique.</p>`
    : `<form class="form" data-form="postular"><p style="margin:0">Si acompañas personas profesionalmente, postúlate. Tu ficha aparecerá en el directorio cuando el equipo de Rutaalacima la verifique.</p>
        ${campo("Especialidad", `<input type="text" name="especialidad" maxlength="120" required placeholder="Hábitos, carrera, finanzas…">`)}
        ${campo("Cómo acompañas", `<textarea name="texto" maxlength="1200" required></textarea>`)}
        <div class="botones"><button class="btn lleno" type="submit">Enviar postulación</button></div></form>`);
  return titulo + cuerpo(h, "max-width:680px");
};
DET["mensajes.acomp"] = uid => {
  const a = MS.cv?.acomps.find(x => x.otroId === uid), nombre = a?.otroNombre || "Avance";
  const r = MS.avance[uid];
  if (!MS.cv) cargarCoachVida();
  const yoCoach = MS.cv?.acomps.find(x => x.rol === "coach" && x.otroId === uid && x.estado === "activo");
  const deCoach = yoCoach ? hoja(htmlSesiones(yoCoach.id)) + hoja(htmlNotasCoach(yoCoach.id)) : "";
  const titulo = cab(avPersona(nombre, "var(--oro)"), esc(nombre), "Lo que decidió compartir contigo",
    `<button class="btn mini" data-acc="escribirA" data-arg="${esc(uid)}|${esc(nombre)}">${ic("mensajes")} Escribir</button>`);
  if (!r) { cargarAvance(uid); return titulo + cuerpo(deCoach + `<p class="suave">Cargando su avance…</p>`, "max-width:680px"); }
  if (r.error) return titulo + cuerpo(deCoach + `<p class="suave">No se pudo cargar su avance. Revisa tu conexión.</p>`, "max-width:680px");
  const bloqueX = (t, xs) => xs.length ? hoja(`<div class="etiqueta">${t}</div>` + xs.map(x => `<div style="margin-top:10px"><div style="display:flex;justify-content:space-between;gap:12px"><span>${esc(x.titulo)}</span><b>${x.avance}%</b></div>${barra(x.avance)}</div>`).join("")) : "";
  const prom = r.habitos7.reduce((s, n) => s + n, 0) / 7;
  let h = `<p class="suave" style="margin:0">Solo ves lo que esta persona decidió compartir contigo, y deja de verse si ella lo desactiva.</p>`;
  if (r.cumbre) h += hoja(`<div class="etiqueta">Su cumbre</div><p style="font-size:18px;margin:6px 0 0">${esc(r.cumbre)}</p>`);
  h += hoja(`<div class="etiqueta">Hábitos de los últimos 7 días</div>
    <div style="display:flex;gap:6px;align-items:flex-end;height:80px;margin-top:10px">${r.habitos7.map(n => `<span title="${n} de 18" style="flex:1;border-radius:6px 6px 0 0;height:${Math.max(4, n / 18 * 100)}%;background:${n >= 10 ? "var(--oro)" : "color-mix(in srgb, var(--burdeos) 50%, transparent)"}"></span>`).join("")}</div>
    <p class="suave" style="margin:6px 0 0;font-size:13px">Promedio: ${prom.toFixed(1)} de 18 hábitos al día</p>`);
  h += bloqueX("Propósitos", r.propositos) + bloqueX("Metas del año", r.metasAnio) + bloqueX("Metas del mes", r.metasMes);
  if (r.ejes) h += hoja(`<div class="etiqueta">Ejes</div>` + EJES.map(([c, n], i) => `<div style="display:grid;grid-template-columns:120px 1fr 24px;gap:10px;align-items:center;margin-top:8px"><span>${n}</span>${barra(r.ejes[i] * 10)}<b>${r.ejes[i]}</b></div>`).join(""));
  if (r.visionTotal) h += hoja(`<p style="margin:0">Vision board: ${r.visionConFoto} de ${r.visionTotal} casillas con su foto</p>`);
  return titulo + cuerpo(h + deCoach, "max-width:680px");
};

/* ---------- Acciones ---------- */
async function hacer(f, ok = "") {
  try { await f(); if (ok) toast(ok); } catch (e) { console.error(e); toast(e?.message || "No se pudo completar. Revisa tu conexión."); }
}
Object.assign(ACC, {
  async escribirA(el, arg) {
    const [otro, nombre] = arg.split("|");
    await hacer(async () => {
      const id = await Msj.abrirCon(otro, nombre);
      await cargarConvs(); MS.msgs[id] = null;
      ir("mensajes", "c:" + id);
    });
  },
  async responderChat(el, arg) { const [id, si] = arg.split("|"); await hacer(() => Msj.responder(id, si === "1")); await actualizarMensajes(true); pintarDetalle(); },
  async bloquearChat(el, arg) { const [id, si] = arg.split("|"); await hacer(() => Msj.bloquear(id, si === "1"), si === "1" ? "Conversación bloqueada" : "Conversación desbloqueada"); await actualizarMensajes(true); pintarDetalle(); },
  async reportarChat(el, id) {
    const c = MS.convs?.find(x => x.id === id); if (!c) return;
    const motivo = prompt(`¿Qué pasó con ${c.otroNombre}?`); if (motivo === null) return;
    await hacer(() => Msj.reportar(c.otroId, c.id, motivo || "Sin detalle"), "Gracias. Revisaremos tu reporte.");
  },
  async solicitarCoach(el, id) { el.disabled = true; await hacer(() => Msj.solicitarCoach(id), "Solicitud enviada"); MS.cv = null; cargarCoachVida(); },
  async responderAcomp(el, arg) { const [id, si] = arg.split("|"); await hacer(() => Msj.responderAcomp(id, si === "1")); MS.cv = null; cargarCoachVida(); },
  async terminarAcomp(el, id) {
    if (!el.dataset.seguro) { el.dataset.seguro = "1"; el.textContent = "¿Seguro? Toca otra vez"; return; }
    await hacer(() => Msj.terminarAcomp(id)); MS.cv = null; cargarCoachVida();
  },
  async compartirAvance(el, id) { await hacer(() => Msj.compartirAvance(id, el.checked), el.checked ? "Tu coach ya puede ver tu avance" : "Dejaste de compartir tu avance"); MS.cv = null; cargarCoachVida(); },
});
async function enviarMensaje(f, texto) {
  const id = f.dataset.arg; if (!texto) return;
  f.reset(); f.querySelector("input")?.focus();
  (MS.msgs[id] ||= []).push({ id: "tmp" + nuevoId(), mio: true, texto, creadoEn: Date.now() });
  const chat = $("chatMsj"); if (chat) { chat.innerHTML = htmlMensajes(id); chat.closest(".det-cuerpo").scrollTop = 1e9; }
  try { await Msj.enviar(id, texto); } catch (e) { console.error(e); toast("No se envió el mensaje. Inténtalo de nuevo."); }
  await actualizarMensajes(true);
}
