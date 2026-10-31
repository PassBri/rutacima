"use strict";
/* ======================================================================
 * Ascenso financiero · cifrado de extremo a extremo (igual que la app, seguridad/FinCifrado.kt).
 * - Una llave de datos (AES-GCM 256) cifra en el aparato cada respuesta "fin#…" (salvo los ajustes).
 *   Valor guardado: "e1:" + base64(iv de 12 bytes + texto cifrado). Ni la base de datos ni un
 *   administrador pueden leer los montos.
 * - En la nube solo va la llave envuelta con el código de recuperación de 12 palabras (fin#llave):
 *   { v: 1, s: sal, r: base64(iv + llave cifrada) }, con PBKDF2-SHA256 de 200.000 vueltas.
 * - En cada aparato la llave se guarda envuelta con el PIN (localStorage) y con una llave del navegador
 *   que no se puede exportar (IndexedDB), para abrir con huella o rostro. Al cerrar el módulo, se olvida.
 * ====================================================================== */
const FIN_PALABRAS = "agua aire alba alce alma alto amigo ancla anillo arbol arco arena arroz astro atlas aura avena ave azul bahia balsa banco barco barro base baya bello beso bosque bota brasa brazo brisa brote buho cabra cacao cafe caja cala calma cama camino campo canoa canto capa cara carta casa cedro cerro cielo cima cinta circo ciudad clave clima cobre cofre cola color cometa concha copa coral cuarzo cuerda cueva cumbre dalia danza dedo delta diente disco dulce duna eco eje elfo enero espiga estrella faro fiesta flor foco fruta fuego fuente gacela gaita gallo garza gato globo gota grano grillo gruta guante haba hada hielo hierba higo hoja horno huella huerto idea isla jade jarra jazmin joya juego junco lago lana lapiz laurel leche lento leon libro lima limon lince lino llama llave lluvia lobo loma luna luz madera maiz mango manto mapa mar marea mesa miel mina mirlo monte mora mundo musgo nido niebla nieve noche norte nube nuez ola oliva onda oro oso pacto paja palma pan papel pasto paz pecho perla pez piedra pino pista plata playa pluma polen pomelo poncho prado puente puerto pulpo puma queso radio raiz rama rana rayo red reloj rio roble roca rosa rueda ruta sabana sal salto selva semilla senda sierra sol sombra sonido suelo sur taza techo tejo tela tierra tigre tinta toro torre trigo trueno tucan uva valle vapor vela verano viento vino violin volcan yegua yerba yeso zorro abeja acero alero arpa asado bambu bisonte bolsa borde cancion cascada castor cisne cobija colina".split(" ");
const FIN_META = new Set(["fin#pin", "fin#bloqueo", "fin#ocultar", "fin#cierre", "fin#visibilidad", "fin#moneda", "fin#llave", "fin#cifrado"]);
const FINC = {
  dek: null, claro: new Map(), pendiente: new Map(),
  activo: () => respuesta("fin#cifrado") === "1" && !!Store.get("respuesta", "fin#llave")?.valor,
  cifrable: k => typeof k === "string" && k.startsWith("fin#") && !FIN_META.has(k),
  cerrar() { this.dek = null; this.claro.clear(); this.pendiente.clear(); },
};
window.FINC = FINC;
const fb64 = b => btoa(String.fromCharCode(...new Uint8Array(b)));
const fdeb64 = t => Uint8Array.from(atob(t), c => c.charCodeAt(0));
const unir = (a, b) => { const r = new Uint8Array(a.length + b.byteLength); r.set(a); r.set(new Uint8Array(b), a.length); return r; };
async function kekDe(texto, sal, vueltas) {
  const base = await crypto.subtle.importKey("raw", new TextEncoder().encode(texto), "PBKDF2", false, ["deriveKey"]);
  return crypto.subtle.deriveKey({ name: "PBKDF2", hash: "SHA-256", salt: sal, iterations: vueltas }, base, { name: "AES-GCM", length: 256 }, false, ["encrypt", "decrypt"]);
}
async function cifrarCon(llave, bytes) { const iv = crypto.getRandomValues(new Uint8Array(12)); return fb64(unir(iv, await crypto.subtle.encrypt({ name: "AES-GCM", iv }, llave, bytes))); }
async function descifrarCon(llave, b64) { const t = fdeb64(b64); return new Uint8Array(await crypto.subtle.decrypt({ name: "AES-GCM", iv: t.slice(0, 12) }, llave, t.slice(12))); }
const dekImportar = raw => crypto.subtle.importKey("raw", raw, "AES-GCM", true, ["encrypt", "decrypt"]);
/** "Pino  Río …" → "pino rio …" (sin tildes, minúsculas, un espacio). */
const normalizarCodigo = t => String(t || "").toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/[^a-z\s]/g, " ").trim().split(/\s+/).join(" ");
function nuevoCodigo() { return [...crypto.getRandomValues(new Uint8Array(12))].map(b => FIN_PALABRAS[b]).join(" "); }

/* Lectura y escritura de valores */
async function finCifrarValor(v) { return "e1:" + await cifrarCon(FINC.dek, new TextEncoder().encode(v)); }
async function finDescifrarValor(v) { return new TextDecoder().decode(await descifrarCon(FINC.dek, v.slice(3))); }
let finRepintar = 0;
/** respuesta() pasa por aquí: devuelve el texto claro si el módulo está abierto; si no, nada. */
window.finLeer = (clave, v) => {
  if (FINC.pendiente.has(clave)) return FINC.pendiente.get(clave);
  if (!v || !v.startsWith("e1:")) return v;
  if (FINC.claro.has(v)) return FINC.claro.get(v);
  if (FINC.dek) finDescifrarValor(v).then(c => { FINC.claro.set(v, c); clearTimeout(finRepintar); finRepintar = setTimeout(() => pintarDetalle(), 30); }).catch(() => FINC.claro.set(v, ""));
  return "";
};
{
  const base = responder;
  responder = function (wb, clave, valor) {
    if (!FINC.cifrable(clave) || !FINC.activo() || !valor) return base.apply(this, arguments);
    if (!FINC.dek) { toast("Abre tus finanzas para guardar este cambio"); return; }
    const texto = String(valor); FINC.pendiente.set(clave, texto);
    finCifrarValor(texto).then(e => { FINC.claro.set(e, texto); FINC.pendiente.delete(clave); base.call(this, wb, clave, e); });
  };
}
/** Descifra de una vez todo lo guardado (al abrir), para que el módulo se vea completo al instante. */
async function finPrecargar() {
  const lista = Store.lista("respuesta").filter(r => FINC.cifrable(r.clave) && String(r.valor || "").startsWith("e1:"));
  await Promise.all(lista.map(async r => { try { FINC.claro.set(r.valor, await finDescifrarValor(r.valor)); } catch { FINC.claro.set(r.valor, ""); } }));
}

/* La llave en este aparato: con el PIN y con una llave del navegador (para la huella o el rostro) */
const IDBF = {
  abrir() { return this.db ||= new Promise((ok, mal) => { const r = indexedDB.open("suroute-fin", 1); r.onupgradeneeded = () => r.result.createObjectStore("k"); r.onsuccess = () => ok(r.result); r.onerror = () => mal(r.error); }); },
  async poner(k, v) { const db = await this.abrir(); return new Promise((ok, mal) => { const t = db.transaction("k", "readwrite"); t.objectStore("k").put(v, k); t.oncomplete = ok; t.onerror = () => mal(t.error); }); },
  async leer(k) { const db = await this.abrir(); return new Promise(ok => { const q = db.transaction("k").objectStore("k").get(k); q.onsuccess = () => ok(q.result); q.onerror = () => ok(null); }); },
  async borrar(k) { const db = await this.abrir(); return new Promise(ok => { const t = db.transaction("k", "readwrite"); t.objectStore("k").delete(k); t.oncomplete = ok; t.onerror = ok; }); },
};
async function guardarLlaveLocal(pin) {
  const raw = new Uint8Array(await crypto.subtle.exportKey("raw", FINC.dek)), sal = crypto.getRandomValues(new Uint8Array(16));
  try { localStorage.setItem("suroute-fin-dek", JSON.stringify({ s: fb64(sal), p: await cifrarCon(await kekDe(pin, sal, 310000), raw) })); } catch {}
  try { const envoltura = await crypto.subtle.generateKey({ name: "AES-GCM", length: 256 }, false, ["encrypt", "decrypt"]);
    await IDBF.poner("envoltura", envoltura); await IDBF.poner("dek", await cifrarCon(envoltura, raw)); } catch {}
}
function borrarLlaveLocal() { try { localStorage.removeItem("suroute-fin-dek"); } catch {} IDBF.borrar("dek").catch(() => {}); IDBF.borrar("envoltura").catch(() => {}); }
const hayLlaveLocal = () => { try { return !!localStorage.getItem("suroute-fin-dek"); } catch { return false; } };
/** Abre con el PIN: true si había llave en este aparato; false si falta el código de recuperación. */
async function finAbrirConPin(pin) {
  if (!FINC.activo()) return true;
  let local; try { local = JSON.parse(localStorage.getItem("suroute-fin-dek") || "null"); } catch {}
  if (!local) return false;
  FINC.dek = await dekImportar(await descifrarCon(await kekDe(pin, fdeb64(local.s), 310000), local.p));
  await finPrecargar(); return true;
}
async function finAbrirConAparato() {
  if (!FINC.activo()) return true;
  const env = await IDBF.leer("envoltura"), dek = await IDBF.leer("dek"); if (!env || !dek) return false;
  FINC.dek = await dekImportar(await descifrarCon(env, dek)); await finPrecargar(); return true;
}
async function finAbrirConCodigo(codigo, pin) {
  const l = JSON.parse(Store.get("respuesta", "fin#llave").valor);
  const raw = await descifrarCon(await kekDe(normalizarCodigo(codigo), fdeb64(l.s), 200000), l.r);
  FINC.dek = await dekImportar(raw); await guardarLlaveLocal(pin); await finPrecargar();
}

/* Activar y quitar el cifrado */
async function finActivarCifrado(codigo, pin) {
  FINC.dek = await crypto.subtle.generateKey({ name: "AES-GCM", length: 256 }, true, ["encrypt", "decrypt"]);
  const raw = new Uint8Array(await crypto.subtle.exportKey("raw", FINC.dek)), sal = crypto.getRandomValues(new Uint8Array(16));
  const llave = JSON.stringify({ v: 1, s: fb64(sal), r: await cifrarCon(await kekDe(normalizarCodigo(codigo), sal, 200000), raw) });
  // Primero se cifra lo que ya existe, luego se anuncia el cifrado (así nada queda a medias si se corta)
  for (const r of Store.lista("respuesta").filter(r => FINC.cifrable(r.clave) && r.valor && !String(r.valor).startsWith("e1:"))) {
    const e = await finCifrarValor(String(r.valor)); FINC.claro.set(e, String(r.valor));
    Store.guardar("respuesta", { ...r, valor: e, actualizadoEn: Date.now() }, r.clave);
  }
  Store.guardar("respuesta", { clave: "fin#llave", workbookId: ASC.WB, valor: llave, actualizadoEn: Date.now() }, "fin#llave");
  Store.guardar("respuesta", { clave: "fin#cifrado", workbookId: ASC.WB, valor: "1", actualizadoEn: Date.now() }, "fin#cifrado");
  await guardarLlaveLocal(pin);
}
async function finQuitarCifrado() {
  if (!FINC.dek) return;
  for (const r of Store.lista("respuesta").filter(r => FINC.cifrable(r.clave) && String(r.valor || "").startsWith("e1:"))) {
    let c = ""; try { c = await finDescifrarValor(r.valor); } catch {}
    if (c) Store.guardar("respuesta", { ...r, valor: c, actualizadoEn: Date.now() }, r.clave);
  }
  Store.borrar("respuesta", "fin#cifrado"); Store.borrar("respuesta", "fin#llave"); borrarLlaveLocal(); FINC.cerrar();
}

/* Pantallas: activar (con el código para guardar en papel) y pedir el código en un aparato nuevo */
function ventanaCifrado(html, alAbrir) { return ventanaCuenta(html, alAbrir); }
ACC.finCifradoActivar = () => {
  if (!bloqueoFin()) { toast("Primero activa el bloqueo con PIN"); return; }
  const codigo = nuevoCodigo(), pal = codigo.split(" "), p1 = 2 + Math.floor(Math.random() * 4), p2 = 7 + Math.floor(Math.random() * 4);
  ventanaCifrado(`<h3 style="margin-top:0">Cifrado de extremo a extremo</h3>
    <p class="suave">Tus montos se cifrarán en este aparato antes de guardarse. Nadie más podrá leerlos, ni siquiera SuRoute.</p>
    <p><b>Este es tu código de recuperación.</b> Escríbelo en papel y guárdalo en un lugar seguro: si olvidas tu PIN o cambias de aparato, solo con él podrás abrir tus finanzas. <b>Sin él, los datos no se pueden recuperar.</b></p>
    <ol class="cod-rec">${pal.map(p => `<li>${p}</li>`).join("")}</ol>
    <button class="btn mini" type="button" data-copiar>${ic("copiar")} Copiar</button>
    <form class="form" id="finCif" style="margin-top:14px">
      ${campo(`Escribe la palabra ${p1 + 1}`, '<input name="a" required autocomplete="off" autocapitalize="none">')}
      ${campo(`Escribe la palabra ${p2 + 1}`, '<input name="b" required autocomplete="off" autocapitalize="none">')}
      ${campo("Tu PIN", '<input name="pin" type="password" inputmode="numeric" pattern="[0-9]{4,6}" maxlength="6" required autocomplete="off">')}
      <div class="botones"><button class="btn" type="button" data-cerrar>Cancelar</button><button class="btn lleno" type="submit">Cifrar mis finanzas</button></div></form>`, v => {
    v.querySelector("[data-cerrar]").onclick = () => v.remove();
    v.querySelector("[data-copiar]").onclick = () => { navigator.clipboard?.writeText(codigo); toast("Código copiado: pégalo en un lugar seguro y luego bórralo del portapapeles"); };
    v.querySelector("#finCif").onsubmit = async e => {
      e.preventDefault(); const d = new FormData(e.target);
      if (normalizarCodigo(d.get("a")) !== pal[p1] || normalizarCodigo(d.get("b")) !== pal[p2]) { toast("Las palabras no coinciden: revisa tu papel"); return; }
      const [sal, h] = respuesta("fin#pin").split("."); if (await hashPin(String(d.get("pin")), sal) !== h) { toast("PIN incorrecto"); return; }
      const b = e.target.querySelector("[type=submit]"); b.disabled = true; b.textContent = "Cifrando…";
      try { await finActivarCifrado(codigo, String(d.get("pin"))); v.remove(); estado.ascAbierto = Date.now(); toast("Listo: tus finanzas están cifradas de extremo a extremo"); pintarDetalle(); }
      catch { b.disabled = false; b.textContent = "Cifrar mis finanzas"; toast("No se pudo cifrar. Intenta de nuevo."); }
    };
  });
};
ACC.finCifradoQuitar = () => {
  if (!FINC.dek) { toast("Abre tus finanzas primero"); return; }
  ventanaCifrado(`<h3 style="margin-top:0">¿Quitar el cifrado?</h3><p class="suave">Tus finanzas se guardarán otra vez sin cifrar (siguen siendo privadas de tu cuenta). Tu código de recuperación dejará de servir.</p>
    <div class="botones"><button class="btn" type="button" data-cerrar>Cancelar</button><button class="btn lleno" type="button" data-si>Quitar el cifrado</button></div>`, v => {
    v.querySelector("[data-cerrar]").onclick = () => v.remove();
    v.querySelector("[data-si]").onclick = async () => { await finQuitarCifrado(); estado.ascAbierto = Date.now(); v.remove(); toast("Cifrado quitado"); pintarDetalle(); };
  });
};
/** En un aparato nuevo, después del PIN se pide una vez el código de recuperación. */
function pantallaCodigo() {
  return cab(av(ic("candado"), "var(--ok)"), "Ascenso financiero", "Cifrado · aparato nuevo") + cuerpo(hoja(`<div class="bloqueo-fin">${ic("candado", "i grande")}
    <h3>Este aparato aún no tiene tu llave</h3><p class="suave">Tus finanzas están cifradas. Escribe una sola vez tu código de recuperación de 12 palabras; después bastará tu PIN o tu huella.</p>
    <form data-form="finCodigo" class="form" style="max-width:420px;margin:0 auto"><textarea name="texto" rows="3" required autocomplete="off" autocapitalize="none" placeholder="pino rio luna …" aria-label="Código de recuperación"></textarea>
      <button class="btn lleno" type="submit">Abrir mis finanzas</button></form>
    ${Store.nube?.sb ? `<p class="suave" style="margin:14px 0 6px">¿Tienes tu teléfono con SuRoute a mano?</p><button class="btn" data-acc="finAprobarQR" style="margin:0 auto;display:flex">${ic("telefono")} Aprobar con mi teléfono</button>` : ""}</div>`), "max-width:560px");
}
window.FORMULARIOS = Object.assign(window.FORMULARIOS || {}, {
  async finCodigo(form, datos, texto) {
    try { await finAbrirConCodigo(texto, estado.finPinPendiente || ""); estado.finPinPendiente = null; estado.finPideCodigo = false; estado.ascAbierto = Date.now(); toast("Listo: este aparato ya tiene tu llave"); pintarLista(); pintarDetalle(); }
    catch { toast("Ese código no abre tus finanzas: revisa las 12 palabras"); }
  },
});
Object.assign(window, { finAbrirConPin, finAbrirConAparato, pantallaCodigo, hayLlaveLocal });

/* ======================================================================
 * Aprobar con el teléfono (QR): la web crea un par de llaves ECDH P-256 y muestra
 * rutacima://finanzas?i=<id>&k=<llave pública>. El teléfono, tras la huella, deja en fin_aprobaciones
 * un mensaje cifrado solo para esta web: la llave de datos (si hay cifrado) o un "ok". Vence en 5 minutos.
 * ====================================================================== */
const fb64u = b => fb64(b).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
ACC.finAprobarQR = async () => {
  const sb = Store.nube?.sb; if (!sb) { toast("Entra con tu cuenta para aprobar con el teléfono"); return; }
  const par = await crypto.subtle.generateKey({ name: "ECDH", namedCurve: "P-256" }, false, ["deriveBits"]);
  const pub = fb64u(await crypto.subtle.exportKey("raw", par.publicKey)), id = fb64u(crypto.getRandomValues(new Uint8Array(18)));
  const enlace = `rutacima://finanzas?i=${id}&k=${pub}`;
  let img = ""; try { const q = qrcode(0, "M"); q.addData(enlace); q.make(); img = q.createDataURL(6, 2); } catch {}
  let vivo = true;
  const v = ventanaCifrado(`<h3 style="margin-top:0">Aprobar con tu teléfono</h3>
    <p class="suave">Escanea este código con la cámara de tu teléfono. La app de SuRoute te pedirá tu huella o tu rostro y tus finanzas se abrirán aquí.</p>
    <div class="qr-fin">${img ? `<img src="${img}" alt="Código QR para aprobar con el teléfono">` : "<p>Código QR no disponible</p>"}</div>
    <p class="suave qr-estado" aria-live="polite">Esperando a tu teléfono… vence en 5 minutos.</p>
    <div class="botones"><button class="btn" type="button" data-cerrar>Cancelar</button></div>`, w => { w.querySelector("[data-cerrar]").onclick = () => { vivo = false; w.remove(); }; });
  const fin = Date.now() + 5 * 60 * 1000;
  while (vivo && Date.now() < fin && document.body.contains(v)) {
    await new Promise(r => setTimeout(r, 2000));
    const { data } = await sb.from("fin_aprobaciones").select("datos").eq("id", id).maybeSingle();
    if (!data?.datos) continue;
    vivo = false;
    try {
      const m = JSON.parse(data.datos);
      const suya = await crypto.subtle.importKey("raw", fdeb64(m.p), { name: "ECDH", namedCurve: "P-256" }, false, []);
      const secreto = await crypto.subtle.deriveBits({ name: "ECDH", public: suya }, par.privateKey, 256);
      const llave = await crypto.subtle.importKey("raw", await crypto.subtle.digest("SHA-256", secreto), "AES-GCM", false, ["decrypt"]);
      const c = JSON.parse(new TextDecoder().decode(await descifrarCon(llave, m.c)));
      if (c.t === "dek") { FINC.dek = await dekImportar(fdeb64(c.k)); await finPrecargar(); }
      else if (FINC.activo()) throw new Error("sin llave");
      estado.finPideCodigo = false; estado.ascAbierto = Date.now(); guardarIntentos?.({});
      toast("Aprobado desde tu teléfono"); v.remove(); pintarLista(); pintarDetalle();
    } catch { v.querySelector(".qr-estado").textContent = "No se pudo abrir con esa aprobación. Intenta de nuevo."; }
    sb.from("fin_aprobaciones").delete().eq("id", id).then(() => {}, () => {});
  }
  if (vivo && document.body.contains(v)) v.querySelector(".qr-estado").textContent = "El código venció. Ciérralo y genera uno nuevo.";
};
