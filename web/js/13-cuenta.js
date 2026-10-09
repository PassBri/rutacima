/* Rutaalacima Web · Cuenta con correo y contraseña (sin teléfono Android, por ejemplo en iPhone),
   recuperar la contraseña y la autorización de tratamiento de datos (Ley 1581 de 2012). */
const LEGAL_VERSION = "1.0"; // igual a Legal.VERSION de la app
const BASE_LEGAL = /github\.io$|^localhost|^127\./.test(location.hostname) ? "" : "https://passbri.github.io/rutacima/";
const enlaceLegal = (archivo, texto) => `<a href="${BASE_LEGAL}${archivo}" target="_blank" rel="noopener">${texto}</a>`;

/** Las dos casillas que exige la autorización (las mismas de la app). */
function casillasLegales() {
  return `<label class="casilla"><input type="checkbox" name="acepto" required> <span>Acepto los ${enlaceLegal("terminos.html", "Términos y condiciones")} y la ${enlaceLegal("privacidad.html", "Política de privacidad")}, y ${enlaceLegal("autorizacion.html", "autorizo el tratamiento de mis datos")}, incluidos los sensibles que decida dar.</span></label>
    <label class="casilla"><input type="checkbox" name="edad" required> <span>Tengo 18 años o más, o tengo entre 14 y 17 y cuento con la autorización de mi representante legal.</span></label>`;
}

const ERRORES_CUENTA = [
  [/invalid login credentials/i, "El correo o la contraseña no coinciden."],
  [/email not confirmed/i, "Confirma tu correo: te enviamos un enlace al crear la cuenta."],
  [/already registered|already been registered/i, "Ya hay una cuenta con ese correo. Entra con tu contraseña."],
  [/password should be at least|weak password/i, "La contraseña debe tener al menos 6 caracteres."],
  [/rate limit|too many/i, "Demasiados intentos. Espera unos minutos y vuelve a intentarlo."],
  [/database error saving new user/i, "Ese usuario ya existe. Prueba con otro."],
  [/signups not allowed|signup is disabled/i, "Por ahora no se pueden crear cuentas desde la web."],
];
const mensajeCuenta = e => (ERRORES_CUENTA.find(([r]) => r.test(e?.message || "")) || [, "No se pudo completar. Revisa tu conexión e inténtalo de nuevo."])[1];

/** Pantalla de entrar o crear cuenta con correo, en la misma hoja de la pantalla de inicio. */
function mostrarCorreo(modo = "entrar") {
  clearInterval(sondeo); clearInterval(renovar);
  const crear = modo === "crear", olvido = modo === "olvido";
  $("pantallaVincular").innerHTML = `<div class="marca"><img src="${LOGO}" alt="">Rutaalacima Web</div>
    <div class="tarjeta-v tarjeta-correo"><div>
      <h1>${crear ? "Crea tu cuenta" : olvido ? "Recupera tu contraseña" : "Entra con tu correo"}</h1>
      <p class="suave" style="margin-top:-12px">${crear ? "Tu ruta queda guardada en tu cuenta y la abres desde cualquier navegador, también en iPhone."
        : olvido ? "Te enviamos un enlace para crear una contraseña nueva." : "Usa el mismo correo y contraseña de tu cuenta de Rutaalacima."}</p>
      <form class="form form-correo" id="formCorreo" novalidate>
        ${crear ? `<label class="campo"><span>Usuario</span><div class="con-prefijo"><b>@</b><input name="usuario" autocomplete="username" minlength="3" maxlength="30" pattern="[a-z0-9._]{3,30}" required></div><small class="suave">De 3 a 30: letras minúsculas, números, punto o guion bajo.</small></label>
        <label class="campo"><span>Tu nombre</span><input name="nombre" autocomplete="name" maxlength="60"></label>` : ""}
        <label class="campo"><span>Correo</span><input name="correo" type="email" autocomplete="email" required></label>
        ${olvido ? "" : `<label class="campo"><span>Contraseña</span><input name="clave" type="password" autocomplete="${crear ? "new-password" : "current-password"}" minlength="6" required></label>`}
        ${crear ? casillasLegales() : ""}
        <p class="form-error" id="correoError" role="alert" aria-live="assertive"></p>
        <div class="botones"><button class="btn lleno" type="submit">${crear ? "Crear mi cuenta" : olvido ? "Enviar enlace" : "Entrar"}</button>
          ${!crear && !olvido ? `<button class="btn" type="button" data-modo="olvido">¿Olvidaste tu contraseña?</button>` : ""}</div>
      </form>
      <p class="suave v-cambio">${crear ? `¿Ya tienes cuenta? <button class="enlace" type="button" data-modo="entrar">Entra</button>` : `¿Aún no tienes cuenta? <button class="enlace" type="button" data-modo="crear">Créala</button>`}
        · <button class="enlace" type="button" data-modo="volver">Volver al código QR</button></p>
    </div></div>`;
  mostrar("vincular");
  const f = $("formCorreo"), err = $("correoError");
  $("pantallaVincular").querySelectorAll("[data-modo]").forEach(b => b.onclick = () => b.dataset.modo === "volver" ? mostrarVincular() : mostrarCorreo(b.dataset.modo));
  if (crear) f.usuario.oninput = () => { f.usuario.value = f.usuario.value.toLowerCase().replace(/[^a-z0-9._]/g, ""); };
  f.querySelector("input")?.focus();
  f.onsubmit = async e => {
    e.preventDefault(); err.textContent = "";
    const correo = f.correo.value.trim(), clave = f.clave?.value || "";
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(correo)) { err.textContent = "Escribe un correo válido."; return f.correo.focus(); }
    if (!olvido && clave.length < 6) { err.textContent = "La contraseña debe tener al menos 6 caracteres."; return f.clave.focus(); }
    if (crear) {
      if (!/^[a-z0-9._]{3,30}$/.test(f.usuario.value)) { err.textContent = "El usuario debe tener de 3 a 30 letras minúsculas, números, punto o guion bajo."; return f.usuario.focus(); }
      if (!f.acepto.checked || !f.edad.checked) { err.textContent = "Para crear la cuenta debes aceptar los documentos y confirmar tu edad."; return; }
    }
    const boton = f.querySelector("button[type=submit]"); boton.disabled = true;
    try {
      if (olvido) {
        await Store.nube.recuperarClave(correo);
        f.innerHTML = `<p>Si hay una cuenta con <b>${esc(correo)}</b>, te llegará un correo con el enlace. Ábrelo en este navegador.</p>`;
        return;
      }
      if (crear) {
        const { data: ya } = await Store.nube.sb.from("profiles").select("id").eq("username", f.usuario.value).maybeSingle();
        if (ya) { err.textContent = "Ese usuario ya existe. Prueba con otro."; boton.disabled = false; return f.usuario.focus(); }
        const sesion = await Store.nube.registrarConCorreo(correo, clave, f.usuario.value, f.nombre.value.trim(), LEGAL_VERSION);
        if (!sesion) {
          f.innerHTML = `<p>¡Listo! Te enviamos un correo a <b>${esc(correo)}</b>. Ábrelo para confirmar tu cuenta y luego entra aquí con tu contraseña.</p>`;
          return;
        }
      } else await Store.nube.entrarConCorreo(correo, clave);
      if (await Store.nube.vinculo()) entrarReal(); else throw new Error("sin cuenta");
    } catch (e2) { console.error(e2); err.textContent = mensajeCuenta(e2); boton.disabled = false; }
  };
}

/** Ventana sobre la app (autorización pendiente o contraseña nueva). */
function ventanaCuenta(html, alMontar) {
  document.getElementById("ventanaCuenta")?.remove();
  const v = document.createElement("div");
  v.id = "ventanaCuenta"; v.className = "ventana-cuenta"; v.setAttribute("role", "dialog"); v.setAttribute("aria-modal", "true");
  v.innerHTML = `<div class="hoja ventana-hoja">${html}</div>`;
  document.body.append(v);
  alMontar(v);
  v.querySelector("input, button")?.focus();
  return v;
}

/** Cuentas que aún no aceptan la autorización vigente: se pide una vez (también la guarda la app). */
async function revisarAutorizacion() {
  if (!Store.nube?.dueno) return;
  try {
    const { data, error } = await Store.nube.sb.from("consentimientos").select("version").eq("version", LEGAL_VERSION).limit(1);
    if (error || data?.length) return; // sin tabla todavía o ya aceptada
  } catch { return; }
  ventanaCuenta(`<h3 style="margin-top:0">Tus datos, con tu permiso</h3>
    <p>Publicamos los documentos legales de Rutaalacima. Para seguir usando tu cuenta, léelos y acepta la autorización. ${enlaceLegal("legal.html", "Leer los documentos")}</p>
    <form class="form" id="formAutorizacion">${casillasLegales()}<p class="form-error" id="autorizacionError" role="alert"></p>
      <div class="botones"><button class="btn lleno" type="submit">Acepto y autorizo</button><button class="btn" type="button" id="autorizacionLuego">Ahora no</button></div></form>`, v => {
    const f = v.querySelector("#formAutorizacion");
    v.querySelector("#autorizacionLuego").onclick = () => v.remove();
    f.onsubmit = async e => {
      e.preventDefault();
      if (!f.acepto.checked || !f.edad.checked) { v.querySelector("#autorizacionError").textContent = "Marca las dos casillas para continuar."; return; }
      const { error } = await Store.nube.sb.rpc("aceptar_legal", { p_version: LEGAL_VERSION, p_origen: "web" });
      if (error) { console.error(error); v.querySelector("#autorizacionError").textContent = "No se pudo guardar. Revisa tu conexión e inténtalo de nuevo."; return; }
      v.remove(); toast("¡Gracias! Quedó guardada tu autorización.");
    };
  });
}

/** Al volver del enlace de "olvidé mi contraseña" (o desde Perfil › Mi cuenta). */
function pedirNuevaClave() {
  ventanaCuenta(`<h3 style="margin-top:0">Crea una contraseña nueva</h3>
    <form class="form" id="formClave"><label class="campo"><span>Contraseña nueva</span><input name="clave" type="password" autocomplete="new-password" minlength="6" required></label>
      <label class="campo"><span>Repítela</span><input name="otra" type="password" autocomplete="new-password" minlength="6" required></label>
      <p class="form-error" id="claveError" role="alert"></p>
      <div class="botones"><button class="btn lleno" type="submit">Guardar</button><button class="btn" type="button" id="claveCancelar">Cancelar</button></div></form>`, v => {
    const f = v.querySelector("#formClave"), err = v.querySelector("#claveError");
    v.querySelector("#claveCancelar").onclick = () => v.remove();
    f.onsubmit = async e => {
      e.preventDefault();
      if (f.clave.value.length < 6) { err.textContent = "Debe tener al menos 6 caracteres."; return; }
      if (f.clave.value !== f.otra.value) { err.textContent = "Las dos contraseñas no coinciden."; return; }
      try { await Store.nube.cambiarClave(f.clave.value); v.remove(); toast("Contraseña actualizada"); }
      catch (e2) { console.error(e2); err.textContent = mensajeCuenta(e2); }
    };
  });
}

Object.assign(ACC, { cambiarClaveWeb() { pedirNuevaClave(); } });
document.addEventListener("keydown", e => { if (e.key === "Escape") document.getElementById("ventanaCuenta")?.remove(); });
