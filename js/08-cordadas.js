"use strict";
/* ======================================================================
 * Cordadas (retos de N días en grupo) y sesiones con el coach de vida
 * (las mismas reglas que la app: ver "Cordadas" y "Coach de vida: sesiones" en supabase/schema.sql)
 * ====================================================================== */
const DURACIONES_CORDADA = [7, 21, 30, 60, 90];
const LETRAS_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
/** Día del reto en que vamos (1 = el día de inicio; pasa de N cuando el reto terminó). */
const diaCordada = c => Math.round((hoyFecha() - deIso(c.inicio)) / DIA_MS) + 1;
const cordadaTerminada = c => diaCordada(c) > c.dias;
const etapaCordada = c => cordadaTerminada(c) ? "Reto terminado" : `Día ${Math.max(1, diaCordada(c))} de ${c.dias}`;
const invitacionCordada = c => `Únete a mi cordada en Rutaalacima: «${c.reto}». Código: ${c.codigo}`;
/** Fechas del reto desde el inicio hasta hoy (o hasta el último día, si ya terminó). */
function diasCordada(c) {
  const xs = [], d = deIso(c.inicio), fin = Math.min(Math.max(1, diaCordada(c)), c.dias);
  for (let k = 0; k < fin; k++) { xs.push(iso(d)); d.setDate(d.getDate() + 1); }
  return xs;
}

/* ---------- Datos de ejemplo (demostración) ---------- */
const DemoCord = (() => {
  const haceDias = k => { const d = hoyFecha(); d.setDate(d.getDate() - k); return iso(d); };
  const azar = (a, b) => { const x = Math.sin(a * 91 + b * 7.3) * 10000; return x - Math.floor(x); };
  const persona = (userId, nombre, usuario) => ({ userId, nombre, usuario });
  const cordadas = [
    { id: "dk1", nombre: "Madrugadores", reto: "Levantarnos a las 5:00 y escribir 20 minutos", eje: "VOL", inicio: haceDias(11), dias: 30, codigo: "MADR5K",
      miembros: [persona("yo", "", ""), persona("demo-3", "Lucía P.", "luciap"), persona("demo-6", "Camilo T.", "camilo"), persona("demo-5", "Sofía L.", "sofial"), persona("demo-1", "Valentina R.", "valentina")] },
    { id: "dk2", nombre: "Lectores de montaña", reto: "Leer 15 páginas de un libro que nos haga crecer", eje: "MAE", inicio: haceDias(26), dias: 21, codigo: "LEE21X",
      miembros: [persona("demo-6", "Camilo T.", "camilo"), persona("yo", "", ""), persona("demo-2", "Andrés M.", "andresm"), persona("demo-4", "Mateo G.", "mateog")] },
  ];
  // Una cordada a la que puedes unirte con el código CUMBRE
  const ajenas = [{ id: "dk3", nombre: "Cumbre de diciembre", reto: "Caminar 8.000 pasos cada día", eje: "EVO", inicio: haceDias(3), dias: 60, codigo: "CUMBRE",
    miembros: [persona("demo-5", "Sofía L.", "sofial"), persona("demo-2", "Andrés M.", "andresm"), persona("demo-4", "Mateo G.", "mateog")] }];
  const checkins = {};
  [...cordadas, ...ajenas].forEach((c, i) => c.miembros.forEach((m, j) => {
    const s = checkins[c.id + "|" + m.userId] = new Set();
    diasCordada(c).forEach((d, k) => { if (d !== iso(hoyFecha()) && azar(i * 10 + j, k) < (m.userId === "yo" ? 0.75 : 0.45 + j * 0.1)) s.add(d); });
    if (m.userId !== "yo" && j % 2) s.add(iso(hoyFecha()));
  }));
  const min = x => Date.now() - x * 60000;
  const notas = {
    dk1: [{ id: "dn1", userId: "demo-3", texto: "¡Día 10! Hoy me costó, pero escribí mis 20 minutos con café en mano ☕", creada: min(95) },
      { id: "dn2", userId: "yo", texto: "Vamos, que ya llevamos un tercio del reto. Mañana a las 5:00 nos vemos.", creada: min(60 * 20) },
      { id: "dn3", userId: "demo-6", texto: "Truco: dejar el cuaderno abierto sobre la mesa la noche anterior.", creada: min(60 * 30) }],
    dk2: [{ id: "dn4", userId: "demo-2", texto: "Terminé «El hombre en busca de sentido». Gracias por empujarme, cordada.", creada: min(60 * 26) }],
  };
  const sesiones = {
    da1: [{ id: "ds1", dias: 2, hora: 19, minutos: 45, tema: "Revisar mi semana de hábitos", enlace: "https://meet.google.com/abc-defg-hij" },
      { id: "ds2", dias: 9, hora: 18, minutos: 60, tema: "Metas del próximo mes", enlace: "" }],
    da2: [{ id: "ds3", dias: 1, hora: 7, minutos: 30, tema: "Avance del portafolio", enlace: "https://meet.google.com/lpz-qrst-uvw" }],
  };
  Object.values(sesiones).forEach(xs => xs.forEach(s => { const d = hoyFecha(); d.setDate(d.getDate() + s.dias); d.setHours(s.hora); s.inicio = d.getTime(); delete s.dias; delete s.hora; }));
  const notasCoach = { da2: "Lucía quiere madrugar para crear antes del trabajo. Acordamos: alarma lejos de la cama y ropa lista.\nRevisar el jueves: ¿cuántos días se levantó a las 5:00?" };
  return { cordadas, ajenas, checkins, notas, sesiones, notasCoach };
})();

const Cord = {
  get real() { return !!Store.nube; },
  get yo() { return this.real ? Store.nube.dueno : "yo"; },
  rpc(n, args) { return Msj.rpc(n, args); },
  async mias() {
    if (!this.real) {
      const hoy = iso(hoyFecha());
      return DemoCord.cordadas.map(c => {
        const dias = m => DemoCord.checkins[c.id + "|" + m.userId] || new Set();
        return { id: c.id, nombre: c.nombre, reto: c.reto, eje: c.eje, inicio: c.inicio, dias: c.dias, codigo: c.codigo, miembros: c.miembros.length,
          misDias: dias({ userId: "yo" }).size, diasGrupo: c.miembros.reduce((s, m) => s + dias(m).size, 0), marqueHoy: dias({ userId: "yo" }).has(hoy) };
      }).sort((a, b) => cordadaTerminada(a) - cordadaTerminada(b) || b.inicio.localeCompare(a.inicio));
    }
    return (await this.rpc("mis_cordadas") || []).map(o => ({ id: o.id, nombre: o.nombre, reto: o.reto, eje: o.eje, inicio: o.inicio, dias: o.dias, codigo: o.codigo,
      miembros: o.miembros || 0, misDias: o.mis_dias || 0, diasGrupo: o.dias_grupo || 0, marqueHoy: !!o.marque_hoy }));
  },
  async crear({ nombre, reto, eje, dias }) {
    if (!this.real) {
      let codigo = ""; for (let i = 0; i < 6; i++) codigo += LETRAS_CODIGO[Math.floor(Math.random() * LETRAS_CODIGO.length)];
      const c = { id: "dk" + nuevoId(), nombre, reto, eje, inicio: iso(hoyFecha()), dias, codigo, miembros: [{ userId: "yo", nombre: "", usuario: "" }] };
      DemoCord.cordadas.unshift(c); DemoCord.notas[c.id] = [];
      return c.id;
    }
    const r = await this.rpc("crear_cordada", { nombre, reto, eje: eje || null, dias, inicio: iso(hoyFecha()) });
    return (Array.isArray(r) ? r[0] : r)?.id;
  },
  async unirse(codigo) {
    const limpio = codigo.toUpperCase().replace(/[^A-Z0-9]/g, "");
    if (!this.real) {
      if (DemoCord.cordadas.some(c => c.codigo === limpio)) return DemoCord.cordadas.find(c => c.codigo === limpio).id;
      const i = DemoCord.ajenas.findIndex(c => c.codigo === limpio);
      if (i < 0) throw new Error("Ese código no es de ninguna cordada");
      const [c] = DemoCord.ajenas.splice(i, 1);
      c.miembros.push({ userId: "yo", nombre: "", usuario: "" }); DemoCord.cordadas.unshift(c); DemoCord.notas[c.id] ||= [];
      return c.id;
    }
    return this.rpc("unirse_cordada", { codigo: limpio });
  },
  async salir(id) {
    if (!this.real) {
      const c = DemoCord.cordadas.find(x => x.id === id); if (!c) return;
      c.miembros = c.miembros.filter(m => m.userId !== "yo"); delete DemoCord.checkins[id + "|yo"];
      DemoCord.cordadas.splice(DemoCord.cordadas.indexOf(c), 1);
      return;
    }
    await this.rpc("salir_cordada", { c: id });
  },
  async detalle(id) {
    if (!this.real) {
      const c = DemoCord.cordadas.find(x => x.id === id); if (!c) return [];
      return c.miembros.map(m => ({ ...m, nombre: m.userId === "yo" ? (perfil().nombre || "Tú") : m.nombre, dias: new Set(DemoCord.checkins[id + "|" + m.userId] || []), soyYo: m.userId === "yo" }));
    }
    return (await this.rpc("cordada_detalle", { c: id }) || []).map(o => ({ userId: o.user_id, nombre: o.nombre || o.usuario || "Senderista", usuario: o.usuario || "", avatar: o.avatar || "", dias: new Set(o.dias || []), soyYo: !!o.soy_yo }));
  },
  /** Marca (o desmarca) tu día de hoy. */
  async marcar(id, si) {
    const dia = iso(hoyFecha());
    if (!this.real) { const s = DemoCord.checkins[id + "|yo"] ||= new Set(); si ? s.add(dia) : s.delete(dia); return; }
    const t = Store.nube.sb.from("cordada_checkins");
    const { error } = si ? await t.insert({ cordada_id: id, user_id: this.yo, dia }) : await t.delete().eq("cordada_id", id).eq("user_id", this.yo).eq("dia", dia);
    if (error && error.code !== "23505") throw error;
  },
  async notas(id) {
    if (!this.real) return (DemoCord.notas[id] || []).map(n => {
      const m = DemoCord.cordadas.find(c => c.id === id)?.miembros.find(x => x.userId === n.userId);
      return { ...n, autor: n.userId === "yo" ? (perfil().nombre || "Tú") : m?.nombre || "Senderista", mia: n.userId === "yo" };
    }).sort((a, b) => b.creada - a.creada);
    const { data, error } = await Store.nube.sb.from("cordada_notas").select("id,user_id,texto,creada,autor:profiles(nombre,username)")
      .eq("cordada_id", id).order("creada", { ascending: false }).limit(100);
    if (error) throw error;
    return data.map(n => ({ id: n.id, userId: n.user_id, texto: n.texto, creada: Date.parse(n.creada), autor: n.autor?.nombre || n.autor?.username || "Senderista", mia: n.user_id === this.yo }));
  },
  async escribir(id, texto) {
    if (!this.real) { (DemoCord.notas[id] ||= []).push({ id: "dn" + nuevoId(), userId: "yo", texto, creada: Date.now() }); return; }
    const { error } = await Store.nube.sb.from("cordada_notas").insert({ cordada_id: id, user_id: this.yo, texto: texto.slice(0, 500) });
    if (error) throw error;
  },
  async borrarNota(cordada, id) {
    if (!this.real) { DemoCord.notas[cordada] = (DemoCord.notas[cordada] || []).filter(n => n.id !== id); return; }
    const { error } = await Store.nube.sb.from("cordada_notas").delete().eq("id", id);
    if (error) throw error;
  },
  /* Sesiones con el coach y notas privadas del coach */
  async sesiones(acomp) {
    const vigente = s => s.inicio + s.minutos * 60000 > Date.now();
    if (!this.real) return (DemoCord.sesiones[acomp] || []).filter(vigente).sort((a, b) => a.inicio - b.inicio);
    const desde = new Date(Date.now() - 3 * 3600000).toISOString();
    const { data, error } = await Store.nube.sb.from("sesiones_coach").select("id,inicio,minutos,enlace,tema").eq("acomp_id", acomp).gte("inicio", desde).order("inicio").limit(50);
    if (error) throw error;
    return data.map(s => ({ id: s.id, inicio: Date.parse(s.inicio), minutos: s.minutos || 45, enlace: s.enlace || "", tema: s.tema || "" })).filter(vigente);
  },
  async agendar(acomp, { inicio, minutos, tema, enlace }) {
    if (!this.real) { (DemoCord.sesiones[acomp] ||= []).push({ id: "ds" + nuevoId(), inicio: inicio.getTime(), minutos, tema, enlace }); return; }
    const { error } = await Store.nube.sb.from("sesiones_coach").insert({ acomp_id: acomp, inicio: inicio.toISOString(), minutos, tema: tema.slice(0, 200), enlace, creada_por: this.yo });
    if (error) throw error;
  },
  async cancelarSesion(acomp, id) {
    if (!this.real) { DemoCord.sesiones[acomp] = (DemoCord.sesiones[acomp] || []).filter(s => s.id !== id); return; }
    const { error } = await Store.nube.sb.from("sesiones_coach").delete().eq("id", id);
    if (error) throw error;
  },
  async notasCoach(acomp) {
    if (!this.real) return DemoCord.notasCoach[acomp] || "";
    const { data, error } = await Store.nube.sb.from("notas_coach").select("texto").eq("acomp_id", acomp).maybeSingle();
    if (error) throw error;
    return data?.texto || "";
  },
  async guardarNotasCoach(acomp, texto) {
    if (!this.real) { DemoCord.notasCoach[acomp] = texto; return; }
    const { error } = await Store.nube.sb.from("notas_coach").upsert({ acomp_id: acomp, texto: texto.slice(0, 8000), actualizada: new Date().toISOString() });
    if (error) throw error;
  },
};

/* ---------- Estado y carga ---------- */
const CK = { lista: null, listaEn: 0, det: {}, ses: {}, notasCoach: {} };
const repintarMensajes = (sel = null) => {
  if (estado.sec !== "mensajes") return;
  pintarLista();
  if (sel === null || estado.sel === sel) { if (enEdicion()) detallePendiente = true; else pintarDetalle(); }
};
async function cargarCordadas() {
  if (CK.cargandoLista) return; CK.cargandoLista = true;
  await null;
  try { CK.lista = await Cord.mias(); } catch (e) { console.error(e); CK.lista ||= []; CK.errorLista = true; }
  finally { CK.cargandoLista = false; CK.listaEn = Date.now(); }
  repintarMensajes();
}
/** Trae compañeros y muro; si ya hay una carga en curso, carga otra vez al terminar (para ver lo recién escrito). */
function cargarCordada(id) {
  const d = CK.det[id] ||= {};
  if (d.cargando) { d.otra = true; return d.cargando; }
  return d.cargando = (async () => {
    await null;
    do {
      d.otra = false;
      try { const [miembros, notas] = await Promise.all([Cord.detalle(id), Cord.notas(id)]); Object.assign(d, { miembros, notas, error: false }); }
      catch (e) { console.error(e); d.error = !d.miembros; }
    } while (d.otra);
    d.cargando = null; d.en = Date.now();
    repintarMensajes("cordada:" + id);
  })();
}
async function cargarSesiones(acomp) {
  CK.ses[acomp] ||= null; if (CK["cs" + acomp]) return; CK["cs" + acomp] = true;
  await null;
  try {
    const [xs, notas] = await Promise.all([Cord.sesiones(acomp), MS.cv?.acomps.some(a => a.id === acomp && a.rol === "coach") ? Cord.notasCoach(acomp) : null]);
    CK.ses[acomp] = xs; if (notas !== null && CK.notasCoach[acomp] === undefined) CK.notasCoach[acomp] = notas;
  } catch (e) { console.error(e); CK.ses[acomp] = { error: true }; }
  finally { CK["cs" + acomp] = false; }
  repintarMensajes();
}

/* ---------- Pantallas de cordadas ---------- */
const avCordada = () => av(ic("cordada"), "var(--burdeos)");
const botonMarcar = c => cordadaTerminada(c) ? "" :
  `<button class="marcar" data-acc="marcarCordada" data-arg="${esc(c.id)}" aria-pressed="${c.marqueHoy}" aria-label="${c.marqueHoy ? "Hoy ya cumpliste (toca para desmarcar)" : "Marcar mi día de hoy"}" title="${c.marqueHoy ? "Hoy ya cumpliste" : "Marcar mi día de hoy"}">${ic("check")}</button>`;
const pillsCordada = c => `<span class="pill">${etapaCordada(c)}</span>${c.eje ? ` <span class="pill" style="background:color-mix(in srgb, ${COLOR_EJE[c.eje]} 18%, transparent)">${esc(NOMBRE_EJE[c.eje] || c.eje)}</span>` : ""}`;
const resumenCordada = c => `Tú: ${c.misDias} ${c.misDias === 1 ? "día" : "días"} · ${c.miembros} ${c.miembros === 1 ? "persona" : "personas"} · ${c.diasGrupo} ${c.diasGrupo === 1 ? "día" : "días"} en total`;

DET["mensajes.cordadas"] = () => {
  const titulo = cab(avCordada(), "Cordadas", "Retos de varios días con 3 a 6 personas");
  if (!CK.lista || Date.now() - CK.listaEn > 30000) cargarCordadas();
  let h = `<p style="margin:0">Una cordada es un grupo de 3 a 6 personas que suben atadas a la misma cuerda: un reto de varios días, cada quien marca su día y se dan ánimo.</p>`;
  if (!Store.nube) h += `<div class="aviso">Demostración: cordadas de ejemplo. Prueba el código CUMBRE para unirte a otra.</div>`;
  h += `<div class="etiqueta">Mis cordadas</div>`;
  if (!CK.lista) h += `<p class="suave">Cargando…</p>`;
  else if (!CK.lista.length) h += `<p class="suave">${CK.errorLista ? "No se pudieron cargar tus cordadas. Revisa tu conexión." : "Aún no estás en ninguna cordada. Crea una e invita a 2 a 5 personas, o únete con el código que te compartieron."}</p>`;
  else CK.lista.forEach(c => {
    h += hoja(`<div class="cord-card" data-sel="cordada:${esc(c.id)}">
      <div class="cord-top"><div><h3>${esc(c.nombre)}</h3><p class="suave">${esc(c.reto)}</p></div>${botonMarcar(c)}</div>
      <div>${pillsCordada(c)}</div>${barra(c.misDias / c.dias * 100)}
      <p class="suave" style="margin:0;font-size:13px">${resumenCordada(c)}</p></div>`);
  });
  h += `<div class="etiqueta">Nueva cordada</div>` + hoja(`<form class="form" data-form="crearCordada">
      ${campo("Nombre de la cordada", `<input type="text" name="nombre" minlength="2" maxlength="60" required placeholder="Madrugadores">`)}
      ${campo("El reto", `<textarea name="reto" minlength="3" maxlength="200" required placeholder="Ej.: levantarnos a las 5:00 y escribir 20 minutos"></textarea>`)}
      <div class="campo"><span>Duración</span><div class="chips">${DURACIONES_CORDADA.map(n => `<label class="chip-radio"><input type="radio" name="dias" value="${n}" ${n === 30 ? "checked" : ""}><span>${n} días</span></label>`).join("")}</div></div>
      ${campo("Eje (opcional)", `<select name="eje">${opcionesEje.map(([v, n]) => `<option value="${v}">${esc(n)}</option>`).join("")}</select>`)}
      <div class="botones"><button class="btn lleno" type="submit">${ic("mas")} Crear cordada</button></div></form>`);
  h += `<div class="etiqueta">Unirme con un código</div>` + hoja(`<form class="form-codigo" data-form="unirseCordada">
      <input class="entrada" name="codigo" maxlength="7" required autocomplete="off" autocapitalize="characters" spellcheck="false" placeholder="Código de 6 letras" aria-label="Código de 6 letras">
      <button class="btn lleno" type="submit">Entrar</button></form>`);
  return titulo + cuerpo(h, "max-width:680px");
};

DET["mensajes.cordada"] = id => {
  const c = CK.lista?.find(x => String(x.id) === id);
  const volverA = html => html.replace('data-acc="volver"', 'data-sel="cordadas"');
  if (!c) {
    if (!CK.lista || !CK.cargandoLista && Date.now() - CK.listaEn > 3000) cargarCordadas();
    return volverA(cab(avCordada(), "Cordada", "")) + cuerpo(`<p class="suave">${CK.lista ? "Esta cordada ya no está entre las tuyas." : "Cargando…"}</p>`, "max-width:680px");
  }
  const d = CK.det[id];
  if (!d || (!d.cargando && Date.now() - (d.en || 0) > 30000)) cargarCordada(id);
  const titulo = volverA(cab(avCordada(), esc(c.nombre), `${etapaCordada(c)} · ${c.miembros} ${c.miembros === 1 ? "persona" : "personas"}`));
  let h = hoja(`<div class="etiqueta">El reto</div><p class="cord-reto">${esc(c.reto)}</p><div>${pillsCordada(c)}</div>
    ${cordadaTerminada(c) ? "" : `<div class="botones" style="margin-top:14px"><button class="btn ${c.marqueHoy ? "oro" : "lleno"}" data-acc="marcarCordada" data-arg="${esc(c.id)}" aria-pressed="${c.marqueHoy}">${ic("check")} ${c.marqueHoy ? "Hoy ya cumpliste" : "Marcar mi día de hoy"}</button></div>`}
    <div style="margin-top:14px">${barra(c.misDias / c.dias * 100)}</div><p class="suave" style="margin:6px 0 0;font-size:13px">${resumenCordada(c)}</p>`);
  if (!d?.miembros) h += `<p class="suave">${d?.error ? "No se pudo cargar la cordada. Revisa tu conexión." : "Cargando…"}</p>`;
  else {
    const fechas = diasCordada(c), hoy = iso(hoyFecha());
    h += hoja(`<div class="etiqueta">Compañeros de cordada (${d.miembros.length})</div><div class="cord-miembros">` + d.miembros.map(m =>
      `<div class="cord-miembro">${m.avatar && /^https:\/\//.test(m.avatar) ? `<img class="avatar" src="${esc(m.avatar)}" alt="">` : avPersona(m.nombre, m.soyYo ? "var(--oro)" : "var(--burdeos)")}
        <div class="nom"><b>${esc(m.nombre)}${m.soyYo ? ` <span class="pill">Tú</span>` : ""}</b><span class="suave">${m.dias.size} de ${c.dias}</span></div>
        <div class="cord-dias" role="img" aria-label="${esc(m.nombre)}: ${m.dias.size} de ${fechas.length} días marcados">${fechas.slice().reverse().map(f => `<i class="${m.dias.has(f) ? "si" : ""}${f === hoy ? " hoy" : ""}" title="${deIso(f).toLocaleDateString("es", { day: "numeric", month: "short" })}"></i>`).join("")}</div></div>`).join("") + `</div>`);
  }
  if (!cordadaTerminada(c)) h += hoja(`<div class="etiqueta">Invita con este código (máximo 6 personas):</div>
    <div class="cord-codigo">${esc(c.codigo)}</div>
    <p class="suave" style="margin:0 0 10px;font-size:13px">${esc(invitacionCordada(c))}</p>
    <div class="botones"><button class="btn" data-acc="copiarInvitacion" data-arg="${esc(c.id)}">${ic("copiar")} Copiar invitación</button></div>`);
  h += hoja(`<div class="etiqueta">Muro de la cordada</div>
    <form class="form-codigo" data-form="notaCordada" data-arg="${esc(c.id)}" style="margin-top:10px"><input class="entrada" name="texto" maxlength="500" required autocomplete="off" placeholder="Escribe algo para tu cordada" aria-label="Escribe algo para tu cordada"><button class="btn lleno" type="submit" aria-label="Enviar">${ic("enviar")}</button></form>
    ${!d?.notas ? "" : !d.notas.length ? `<p class="suave" style="margin:12px 0 0">Nadie ha escrito todavía. Un «¡vamos!» a tiempo ayuda más de lo que crees.</p>`
      : `<div class="cord-muro">${d.notas.map(n => `<div class="nota-cord"><div class="nota-cab"><b>${esc(n.autor)}</b><span class="suave">${hace(n.creada)}</span>${n.mia ? `<button class="btn mini" data-acc="borrarNotaCordada" data-arg="${esc(c.id)}|${esc(n.id)}" aria-label="Borrar mi nota">${ic("borrar")}</button>` : ""}</div><p>${esc(n.texto)}</p></div>`).join("")}</div>`}`);
  h += `<div class="botones"><button class="btn peligro" data-acc="salirCordada" data-arg="${esc(c.id)}">${ic("salir")} Salir de la cordada</button></div>`;
  return titulo + cuerpo(h, "max-width:680px");
};

/* ---------- Sesiones con el coach ---------- */
const fechaSesion = ms => mayus(new Date(ms).toLocaleString("es", { weekday: "long", day: "numeric", month: "long", hour: "2-digit", minute: "2-digit" }));
function htmlSesiones(acomp) {
  if (CK.ses[acomp] === undefined) cargarSesiones(acomp);
  const xs = CK.ses[acomp];
  let h = `<div class="etiqueta">Sesiones</div>`;
  if (!xs) h += `<p class="suave" style="margin:6px 0 0">Cargando…</p>`;
  else if (xs.error) h += `<p class="suave" style="margin:6px 0 0">No se pudieron cargar las sesiones. Revisa tu conexión.</p>`;
  else if (!xs.length) h += `<p class="suave" style="margin:6px 0 0">No hay sesiones agendadas.</p>`;
  else h += xs.map(s => `<div class="sesion">${av(ic("hoy"), "var(--oro)")}<div class="ses-txt"><b>${esc(fechaSesion(s.inicio))}</b><span class="suave">${s.minutos} min${s.tema ? " · " + esc(s.tema) : ""}</span></div>
      <span class="botones">${s.enlace && /^https:\/\//.test(s.enlace) ? `<a class="btn mini lleno" href="${esc(s.enlace)}" target="_blank" rel="noopener noreferrer">Unirme</a>` : ""}<button class="btn mini" data-acc="cancelarSesion" data-arg="${esc(acomp)}|${esc(s.id)}">Cancelar</button></span></div>`).join("");
  const dias = Array.from({ length: 14 }, (_, k) => { const d = hoyFecha(); d.setDate(d.getDate() + k); return [iso(d), k === 0 ? "Hoy" : k === 1 ? "Mañana" : mayus(d.toLocaleDateString("es", { weekday: "short", day: "numeric", month: "short" }))]; });
  const horas = Array.from({ length: 16 }, (_, k) => String(k + 6).padStart(2, "0") + ":00");
  const op = (xs, def) => xs.map(([v, n]) => `<option value="${v}" ${String(v) === String(def) ? "selected" : ""}>${esc(n)}</option>`).join("");
  h += `<form class="form ses-form" data-form="agendarSesion" data-arg="${esc(acomp)}"><b>Agendar sesión</b><div class="form-2">
      ${campo("Día", `<select name="dia">${op(dias, dias[1][0])}</select>`)}
      ${campo("Hora", `<select name="hora">${op(horas.map(x => [x, x]), "19:00")}</select>`)}
      ${campo("Duración", `<select name="minutos">${op([30, 45, 60, 90].map(n => [n, n + " min"]), 45)}</select>`)}</div>
      ${campo("Tema (opcional)", `<input type="text" name="tema" maxlength="200" placeholder="Qué quieren revisar">`)}
      ${campo("Enlace de videollamada (opcional)", `<input type="url" name="enlace" maxlength="500" placeholder="https://meet.google.com/…" inputmode="url">`)}
      <div class="botones"><button class="btn lleno" type="submit">${ic("hoy")} Agendar sesión</button></div></form>`;
  return h;
}
function htmlNotasCoach(acomp) {
  const v = CK.notasCoach[acomp];
  return `<div class="etiqueta">Mis notas privadas</div><p class="suave" style="margin:4px 0 8px;font-size:13px">Solo tú las ves: lo que acuerdan, avances y qué revisar en la próxima sesión.</p>
    ${v === undefined ? `<p class="suave">Cargando…</p>` : `<label class="campo"><textarea rows="6" maxlength="8000" data-acc-input="notasCoach" data-arg="${esc(acomp)}" aria-label="Mis notas privadas">${esc(v)}</textarea></label>
    <p class="suave notas-estado" id="notasEstado" aria-live="polite"></p>`}`;
}

/* ---------- Acciones ---------- */
async function copiarTexto(t) {
  try { await navigator.clipboard.writeText(t); return true; }
  catch {
    const a = document.createElement("textarea"); a.value = t; a.style.cssText = "position:fixed;opacity:0"; document.body.append(a); a.select();
    let ok = false; try { ok = document.execCommand("copy"); } catch {} a.remove(); return ok;
  }
}
const guardarNotasCoach = {};
Object.assign(ACC, {
  async marcarCordada(el, id) {
    const c = CK.lista?.find(x => String(x.id) === id); if (!c || el.disabled) return;
    const si = !c.marqueHoy, dia = iso(hoyFecha()), yo = CK.det[id]?.miembros?.find(m => m.soyYo);
    el.disabled = true;
    // Al instante en pantalla; si el servidor falla, se vuelve a cargar
    c.marqueHoy = si; c.misDias += si ? 1 : -1; c.diasGrupo += si ? 1 : -1; if (yo) si ? yo.dias.add(dia) : yo.dias.delete(dia);
    repintarMensajes();
    try { await Cord.marcar(id, si); if (si) toast("¡Día marcado! Tu cordada lo verá."); }
    catch (e) { console.error(e); toast(e?.message || "No se pudo marcar tu día. Revisa tu conexión."); CK.listaEn = 0; cargarCordadas(); cargarCordada(id); }
  },
  async copiarInvitacion(el, id) {
    const c = CK.lista?.find(x => String(x.id) === id); if (!c) return;
    toast(await copiarTexto(invitacionCordada(c)) ? "Invitación copiada: pégala donde quieras" : `Código: ${c.codigo}`);
  },
  async borrarNotaCordada(el, arg) {
    if (!el.dataset.seguro) { el.dataset.seguro = "1"; el.textContent = "¿Borrar?"; return; }
    const [c, id] = arg.split("|");
    await hacer(() => Cord.borrarNota(c, id), "Nota borrada"); cargarCordada(c);
  },
  async salirCordada(el, id) {
    if (!el.dataset.seguro) { el.dataset.seguro = "1"; el.textContent = "¿Seguro? Toca otra vez"; return; }
    el.disabled = true;
    await hacer(async () => { await Cord.salir(id); delete CK.det[id]; CK.lista = CK.lista?.filter(x => String(x.id) !== id) || null; elegir("cordadas"); }, "Saliste de la cordada");
    CK.listaEn = 0; cargarCordadas();
  },
  async cancelarSesion(el, arg) {
    if (!el.dataset.seguro) { el.dataset.seguro = "1"; el.textContent = "¿Cancelar la sesión?"; return; }
    const [acomp, id] = arg.split("|");
    await hacer(() => Cord.cancelarSesion(acomp, id), "Sesión cancelada"); delete CK.ses[acomp]; cargarSesiones(acomp);
  },
  notasCoach(el, acomp) {
    CK.notasCoach[acomp] = el.value;
    const nota = $("notasEstado"); if (nota) nota.textContent = "Guardando…";
    clearTimeout(guardarNotasCoach[acomp]);
    guardarNotasCoach[acomp] = setTimeout(async () => {
      try { await Cord.guardarNotasCoach(acomp, CK.notasCoach[acomp]); const n = $("notasEstado"); if (n) n.textContent = "Guardado " + horaDe(Date.now()); }
      catch (e) { console.error(e); const n = $("notasEstado"); if (n) n.textContent = "No se pudo guardar. Revisa tu conexión."; }
    }, 800);
  },
});
/** Formularios de cordadas y sesiones (los llama enviar()). */
async function enviarCordadas(f, datos, texto) {
  const boton = f.querySelector("button[type=submit]");
  const ocupado = si => { if (boton) boton.disabled = si; };
  switch (f.dataset.form) {
    case "crearCordada": {
      const nombre = String(datos.get("nombre") || "").trim(), reto = String(datos.get("reto") || "").trim();
      if (nombre.length < 2 || reto.length < 3) { toast("Escribe el nombre y el reto de la cordada"); return; }
      ocupado(true);
      try {
        const id = await Cord.crear({ nombre, reto, eje: datos.get("eje") || null, dias: Number(datos.get("dias")) || 30 });
        CK.lista = null; await cargarCordadas(); toast("Cordada creada. Invita a tu gente con el código.");
        if (id) elegir("cordada:" + id);
      } catch (e) { console.error(e); toast(e?.message || "No se pudo crear la cordada. Revisa tu conexión."); ocupado(false); }
    } return;
    case "unirseCordada": {
      const codigo = String(datos.get("codigo") || "").toUpperCase().replace(/[^A-Z0-9]/g, "");
      if (codigo.length !== 6) { toast("El código tiene 6 letras"); return; }
      ocupado(true);
      try { const id = await Cord.unirse(codigo); CK.lista = null; await cargarCordadas(); toast("¡Ya eres parte de la cordada!"); if (id) elegir("cordada:" + id); }
      catch (e) { console.error(e); toast(e?.message || "No se pudo entrar. Revisa el código."); ocupado(false); }
    } return;
    case "notaCordada": {
      if (!texto) return;
      const id = f.dataset.arg; ocupado(true);
      try {
        await Cord.escribir(id, texto); f.reset(); await cargarCordada(id);
        if (estado.sel === "cordada:" + id) { pintarDetalle(); $("detalle").querySelector("form[data-form=notaCordada] input")?.focus(); }
      }
      catch (e) { console.error(e); toast(e?.message || "No se publicó tu nota. Inténtalo de nuevo."); }
      ocupado(false);
    } return;
    case "agendarSesion": {
      const acomp = f.dataset.arg, enlace = String(datos.get("enlace") || "").trim(), tema = String(datos.get("tema") || "").trim();
      const [h, m] = String(datos.get("hora") || "19:00").split(":").map(Number), inicio = deIso(String(datos.get("dia")));
      inicio.setHours(h, m || 0, 0, 0);
      if (enlace && !/^https:\/\/[^\s]+\.[^\s]+/.test(enlace)) { toast("El enlace debe empezar por https://"); f.querySelector("[name=enlace]")?.focus(); return; }
      if (inicio.getTime() <= Date.now()) { toast("Elige una hora que aún no haya pasado"); return; }
      ocupado(true);
      try { await Cord.agendar(acomp, { inicio, minutos: Number(datos.get("minutos")) || 45, tema, enlace }); f.reset(); toast("Sesión agendada: " + fechaSesion(inicio)); delete CK.ses[acomp]; await cargarSesiones(acomp); }
      catch (e) { console.error(e); toast(e?.message || "No se pudo agendar. Revisa tu conexión."); ocupado(false); }
    } return;
  }
}
