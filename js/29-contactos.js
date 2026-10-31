/* =====================================================================
   Agregar contactos (Mensajes)
   1. Buscar por nombre o @usuario.
   2. Contactos del teléfono: el navegador (Chrome de Android) deja elegir contactos; se calculan huellas
      SHA-256 aquí mismo y solo esas huellas viajan al servidor (supabase/2026-10-27-contactos.sql).
   3. Otras redes: ninguna permite importar contactos, así que se comparte el enlace de invitación.
   4. Que mis contactos me encuentren (opcional).
   ===================================================================== */
const CT = { resultados: null, encontrados: null, invitar: [], encontrable: null, buscando: "" };
const hayAgenda = () => "contacts" in navigator && "ContactsManager" in window;
const normTel = t => { const d = String(t || "").replace(/\D/g, ""); return d.length >= 7 ? d.slice(-10) : ""; };
const normCorreo = c => String(c || "").trim().toLowerCase();
async function huella(tipo, v) {
  const b = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(`suroute|${tipo}|${v}`));
  return [...new Uint8Array(b)].map(x => x.toString(16).padStart(2, "0")).join("");
}
const textoInvitacion = () => {
  const u = Store.nube?.perfil?.username;
  return { texto: `Estoy subiendo a mi cumbre en SuRoute${u ? ` (@${u})` : ""}. Únete y subamos juntos:`, url: window.enlaceInvitacion?.(u) || "https://suroute.com/descargar.html" };
};

async function cargarEncontrable() {
  if (!Store.nube) { CT.encontrable = false; return; }
  try { CT.encontrable = !!(await Msj.rpc("soy_encontrable")); } catch { CT.encontrable = false; }
  if (estado.sel === "contactos") pintarDetalle();
}

const filaContacto = x => `<div class="persona-fila"><button class="persona-abrir" data-ir="comunidad" data-arg="persona:${esc(x.id)}">${avRed(x, "aviso-av")}
    <span><b>${esc(x.nombre)}</b><small>@${esc(x.usuario)}${x.agenda ? ` · ${esc(x.agenda)}` : x.cumbre ? ` · ${esc(x.cumbre)}` : ""}</small></span></button>
    <button class="btn mini lleno" data-acc="escribirA" data-arg="${esc(x.id)}|${esc(x.nombre)}">${ic("mensajes")} Mensaje</button></div>`;

function pintarContactos() {
  const caja = document.getElementById("resultadosContacto");
  if (caja) caja.innerHTML = CT.resultados == null ? "" : CT.resultados.length ? CT.resultados.map(filaContacto).join("") : `<p class="suave">No encontramos a nadie con ese nombre. Invítalo con tu enlace.</p>`;
}
async function buscarContacto(t) {
  t = t.trim().replace(/^@/, "").replace(/[,()*%]/g, " ").trim();
  if (t.length < 2) { CT.resultados = null; return pintarContactos(); }
  CT.buscando = t;
  let r;
  if (!Store.nube) r = demoPersonas().filter(x => (x.nombre + " " + x.usuario).toLowerCase().includes(t.toLowerCase()));
  else {
    const { data } = await Store.nube.sb.from("profiles").select("id,username,nombre,avatar_url,cumbre").or(`username.ilike.*${t}*,nombre.ilike.*${t}*`).limit(25);
    r = (data || []).filter(x => x.id !== Store.nube.dueno).map(x => ({ id: x.id, nombre: x.nombre || x.username, usuario: x.username, avatar: x.avatar_url, cumbre: x.cumbre }));
  }
  if (CT.buscando !== t) return;
  CT.resultados = ordenarPorUsuario(r, t); pintarContactos();
}
let esperaContacto = null;
document.addEventListener("input", e => { if (e.target.id !== "buscarContacto") return; clearTimeout(esperaContacto); esperaContacto = setTimeout(() => buscarContacto(e.target.value), 280); });

DET["mensajes.contactos"] = () => {
  if (CT.encontrable == null) cargarEncontrable();
  const { texto, url } = textoInvitacion(), msg = encodeURIComponent(`${texto} ${url}`);
  const red = (nombre, href) => `<a class="chip" href="${href}" target="_blank" rel="noopener">${nombre}</a>`;
  const miU = Store.nube?.perfil?.username;
  let h = !Store.nube ? "" : usuarioPendiente()
    ? `<div class="aviso" style="margin:0 0 12px;flex-wrap:wrap"><span style="flex:1;min-width:220px">Elige tu @usuario: es tu nombre único y así te encuentran para escribirte.</span><button class="btn mini lleno" data-acc="elegirUsuario">Elegir mi @usuario</button></div>`
    : `<p class="suave" style="margin:0 0 12px">Te encuentran como <b style="color:var(--burdeos)">@${esc(miU)}</b>. Compártelo para que te escriban.</p>`;
  h += `<label class="buscador-red">${ic("buscar")}<input id="buscarContacto" type="search" placeholder="Busca por @usuario o nombre" autocomplete="off" autocapitalize="none" spellcheck="false" enterkeyhint="search" aria-label="Buscar en SuRoute"></label>
    <div id="resultadosContacto" class="personas"></div>`;

  // Contactos del teléfono
  let agenda = `<div class="etiqueta ct-et">${ic("telefono")} Contactos de tu teléfono</div>`;
  if (CT.encontrados) {
    agenda += CT.encontrados.length ? `<p class="suave" style="margin:0 0 6px">${CT.encontrados.length === 1 ? "1 contacto ya está" : `${CT.encontrados.length} contactos ya están`} en SuRoute.</p><div class="personas">${CT.encontrados.map(filaContacto).join("")}</div>`
      : `<p class="suave" style="margin:0 0 6px">Ninguno de los contactos que elegiste está todavía en SuRoute (o no activó que lo encuentren).</p>`;
    if (CT.invitar.length) agenda += `<div class="etiqueta" style="margin-top:12px">Invítalos</div><div class="personas">${CT.invitar.slice(0, 50).map(c => `<div class="persona-fila"><span class="persona-abrir">${avPersona(c.nombre)}<span><b>${esc(c.nombre)}</b><small>${esc(c.tel || c.correo)}</small></span></span>
        <a class="btn mini" href="${c.tel ? `sms:${esc(c.tel)}?body=${msg}` : `mailto:${esc(c.correo)}?subject=${encodeURIComponent("Subamos juntos en SuRoute")}&body=${msg}`}">Invitar</a></div>`).join("")}</div>`;
  }
  agenda += hayAgenda()
    ? `<div class="botones" style="margin-top:10px"><button class="btn ${CT.encontrados ? "" : "lleno"}" data-acc="elegirContactos">${ic("personaMas")} ${CT.encontrados ? "Elegir otros contactos" : "Elegir contactos"}</button></div>
       <p class="suave" style="font-size:13px;margin:8px 0 0">Tú eliges a quién revisar. Los números y correos no salen de tu teléfono: solo se envía una huella cifrada para compararla.</p>`
    : `<p class="suave" style="margin:0">Este navegador no deja leer la agenda. Hazlo desde la app de Android o desde Chrome en un Android; aquí puedes buscar por nombre o invitar con tu enlace.</p>`;
  h += hoja(agenda);

  // Otras redes
  h += hoja(`<div class="etiqueta ct-et">${ic("compartir")} Desde otras redes</div>
    <p class="suave" style="margin:0 0 10px">WhatsApp, Instagram, Facebook, TikTok y X no dejan importar contactos. Comparte tu enlace: quien entre con él te verá al instante y podrá seguirte y escribirte.</p>
    <div class="chips" style="flex-wrap:wrap">
      ${red("WhatsApp", `https://wa.me/?text=${msg}`)}${red("Telegram", `https://t.me/share/url?url=${encodeURIComponent(url)}&text=${encodeURIComponent(texto)}`)}
      ${red("Facebook", `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`)}${red("X", `https://x.com/intent/post?text=${msg}`)}
      ${red("Correo", `mailto:?subject=${encodeURIComponent("Subamos juntos en SuRoute")}&body=${msg}`)}
      ${navigator.share ? `<button class="chip" data-acc="invitarAmigos">Instagram y más…</button>` : ""}
      <button class="chip" data-acc="copiarInvitacionRed">${ic("copiar")} Copiar enlace</button></div>`);

  // Encontrable
  h += hoja(`<div class="etiqueta ct-et">${ic("ojo")} Que mis contactos me encuentren</div>` + (!Store.nube
    ? `<p class="suave" style="margin:0">Con tu cuenta, las personas que te tengan en su agenda podrán encontrarte por tu correo o tu teléfono.</p>`
    : `<form data-form="encontrable" style="display:grid;gap:10px">
        <label style="display:flex;gap:12px;align-items:flex-start;cursor:pointer"><input type="checkbox" name="activo" style="width:20px;height:20px;margin-top:2px;flex:none;accent-color:var(--burdeos)" ${CT.encontrable ? "checked" : ""}>
          <span>Por el correo de mi cuenta y, si lo escribo, por mi teléfono.<br><span class="suave" style="font-size:13px">Se guarda solo una huella cifrada. Puedes apagarlo cuando quieras.</span></span></label>
        <input name="tel" type="tel" inputmode="tel" autocomplete="tel" placeholder="Tu número (opcional)" aria-label="Tu número de teléfono" style="font-size:16px">
        <div class="botones"><button class="btn lleno">Guardar</button></div></form>`));

  return cab(av(ic("personaMas"), "var(--burdeos)"), "Agregar contactos", "Tu gente, a tu lado en el ascenso") + cuerpo(h, "max-width:680px");
};

Object.assign(ACC, {
  async elegirContactos() {
    let lista;
    try { lista = await navigator.contacts.select(["name", "tel", "email"], { multiple: true }); } catch { return; }
    if (!lista?.length) return;
    const contactos = lista.map(c => ({ nombre: c.name?.[0] || c.tel?.[0] || c.email?.[0] || "Contacto", tel: c.tel?.[0] || "", correo: c.email?.[0] || "", tels: (c.tel || []).map(normTel).filter(Boolean), correos: (c.email || []).map(normCorreo).filter(Boolean) }));
    const porHuella = {};
    for (const c of contactos) {
      for (const t of c.tels) porHuella[await huella("tel", t)] = c;
      for (const m of c.correos) porHuella[await huella("correo", m)] = c;
    }
    let filas = [];
    if (Store.nube) { try { filas = await Msj.rpc("encontrar_contactos", { hashes: Object.keys(porHuella).slice(0, 3000) }) || []; } catch (e) { console.error(e); toast("No se pudo revisar ahora. Revisa tu conexión."); return; } }
    else filas = demoPersonas().slice(0, Math.min(2, contactos.length)).map((p, i) => ({ hash: Object.keys(porHuella)[i], id: p.id, username: p.usuario, nombre: p.nombre, cumbre: p.cumbre }));
    const ya = new Set();
    CT.encontrados = filas.map(f => { const c = porHuella[f.hash]; if (c) ya.add(c); return { id: f.id, nombre: f.nombre || f.username, usuario: f.username, avatar: f.avatar_url, cumbre: f.cumbre, agenda: c?.nombre }; });
    CT.invitar = contactos.filter(c => !ya.has(c) && (c.tel || c.correo));
    pintarDetalle();
  },
  elegirUsuario() { revisarUsuario.hecho = false; revisarUsuario(); },
  async copiarInvitacionRed() {
    const { texto, url } = textoInvitacion();
    toast(await copiarTexto(`${texto} ${url}`) ? "Enlace copiado: pégalo en Instagram, TikTok o donde quieras" : url);
  },
});

async function enviarEncontrable(f, datos) {
  const activo = !!datos.get("activo"), tel = normTel(datos.get("tel"));
  await hacer(async () => {
    await Msj.rpc("hacerme_encontrable", { activo, tel_hash: activo && tel ? await huella("tel", tel) : null });
    CT.encontrable = activo;
  }, activo ? "Listo: tus contactos podrán encontrarte" : "Ya no aparecerás al buscar en agendas");
  pintarDetalle();
}

/* ---------- @usuario: único y personal, para que te encuentren ---------- */
const usuarioPendiente = () => { const u = Store.nube?.perfil?.username || ""; return !!Store.nube?.dueno && (!u || /^senderista_/.test(u) || pareceCorreo(u, Store.nube.correo)); };
/** Si el @usuario es el correo o el automático, se pide uno propio (una vez por sesión). */
async function revisarUsuario() {
  if (!usuarioPendiente() || revisarUsuario.hecho) return;
  if (document.getElementById("ventanaCuenta")) return setTimeout(revisarUsuario, 1500);
  revisarUsuario.hecho = true;
  const actual = Store.nube.perfil?.username || "";
  ventanaCuenta(`<h3 style="margin-top:0">Elige tu @usuario</h3>
    <p>Es tu nombre único en SuRoute: así te encuentran tus amigos y te escriben.${pareceCorreo(actual, Store.nube.correo) ? " Ahora mismo se parece a tu correo y toda la comunidad lo vería." : ""}</p>
    <form class="form" id="formUsuario"><label class="buscador-red" style="height:52px">@<input name="u" autocomplete="username" autocapitalize="none" spellcheck="false" maxlength="30" placeholder="brian.suarez" aria-label="Tu @usuario"></label>
      <p class="suave" id="usuarioEstado" style="font-size:13px;min-height:18px;margin:6px 4px">De 3 a 30 letras minúsculas, números, punto o guion bajo.</p>
      <div class="botones"><button class="btn lleno" type="submit">Guardar</button><button class="btn" type="button" id="usuarioLuego">Ahora no</button></div></form>`, v => {
    const f = v.querySelector("#formUsuario"), est = v.querySelector("#usuarioEstado"); let espera;
    v.querySelector("#usuarioLuego").onclick = () => v.remove();
    f.u.oninput = () => {
      f.u.value = f.u.value.toLowerCase().replace(/[^a-z0-9._]/g, ""); clearTimeout(espera);
      const u = f.u.value;
      if (!/^[a-z0-9._]{3,30}$/.test(u)) { est.textContent = "De 3 a 30 letras minúsculas, números, punto o guion bajo."; return; }
      if (pareceCorreo(u, Store.nube.correo)) { est.textContent = "No uses tu correo: elige un nombre corto."; return; }
      espera = setTimeout(async () => {
        const { data } = await Store.nube.sb.rpc("usuario_disponible", { u }).catch(() => ({}));
        if (f.u.value === u) est.textContent = data === false ? `@${u} ya existe. Prueba con otro.` : data ? `@${u} está libre.` : "";
      }, 350);
    };
    f.onsubmit = async e => { e.preventDefault(); await ACC.cambiarUsuario(f.u); if (Store.nube.perfil?.username === f.u.value) { v.remove(); pintarDetalle(); } };
  });
}
globalThis.revisarUsuario = revisarUsuario;
/** Primero quien coincide exacto con el @usuario, luego quien empieza igual. */
const ordenarPorUsuario = (lista, t) => { t = t.toLowerCase(); const p = x => x.usuario === t ? 0 : x.usuario?.startsWith(t) ? 1 : 2; return [...lista].sort((a, b) => p(a) - p(b)); };
