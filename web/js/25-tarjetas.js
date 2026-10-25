/* =====================================================================================
 * Tarjetas del vision board y perfil al estilo de las redes de video corto.
 *  · Cada tarjeta tiene dos caras: la foto limpia al frente y la frase al girarla.
 *  · Se mueve dejándola presionada (con el mouse basta arrastrarla); las demás se apartan.
 *  · La foto se abre a pantalla completa y se pasa de una a otra deslizando hacia arriba.
 *  · Cada meta y cada tarjeta puede tener un apodo («Operación Fénix»).
 *  · Mis publicaciones: cabecera con seguidores y pestañas Publicaciones · Privadas · Guardadas.
 * ===================================================================================== */

/* ---------- Apodos: se guardan como respuestas, así viajan entre la app y la web ---------- */
const apodoDe = (tipo, id) => respuesta(`apodo:${tipo}:${id}`).trim();
/** «Apodo» · resto, para la línea secundaria de las listas. */
const conApodo = (tipo, id, resto = "") => { const a = apodoDe(tipo, id); return a ? `«${esc(a)}»${resto ? " · " + resto : ""}` : resto; };
const campoApodo = (tipo, id) => campo("Apodo (opcional)",
  `<input type="text" maxlength="32" data-resp="apodo|apodo:${tipo}:${id}" value="${esc(apodoDe(tipo, id))}" placeholder="Un nombre corto que te inspire · ej. Operación Fénix">`);
for (const [det, tipo] of [["metas.p", "p"], ["metas.a", "a"], ["metas.m", "m"]]) {
  const base = DET[det];
  DET[det] = arg => base(arg).replace(`<div class="form">`, `<div class="form">${campoApodo(tipo, arg)}`);
}

/* ---------- Modo Imagen / Texto y girar cada tarjeta ---------- */
(estado.giradas ||= new Set());
try { estado.modoTarjetas = ["texto", "edicion"].includes(localStorage.getItem("rutacima-modo-tarjetas")) ? localStorage.getItem("rutacima-modo-tarjetas") : "imagen"; } catch { estado.modoTarjetas = "imagen"; }
const pocoMovimiento = () => matchMedia("(prefers-reduced-motion: reduce)").matches;
/** Pone una tarjeta de un lado u otro sin volver a pintar la pantalla (así se ve el giro). */
function ladoTarjeta(t, atras) {
  t.classList.toggle("girada", atras);
  const cara = t.querySelector(":scope > .giro-in > .cara, :scope > .cara"), dorso = t.querySelector(":scope > .giro-in > .dorso, :scope > .dorso");
  if (cara) cara.inert = atras; if (dorso) dorso.inert = !atras;
}
/** Imagen: todas muestran la foto. Texto: todas muestran lo que se edita. Giran en cascada, una tras otra. */
ACC.modoTarjetas = (el, v) => {
  const antes = estado.modoTarjetas; if (antes === v) return;
  estado.modoTarjetas = v;
  try { localStorage.setItem("rutacima-modo-tarjetas", v); } catch {}
  document.documentElement.dataset.modoTarjetas = v;
  document.querySelectorAll(".modo-tarj [role=radio]").forEach(b => b.setAttribute("aria-checked", b.dataset.arg === v));
  // En Edición el 9×9 se escribe en la grilla: si había un bloque abierto, vuelve a la grilla de 81
  if (v === "edicion" && estado.md81 && document.getElementById("md-s81")) { estado.md81 = false; pintarDetalle(); }
  // Entre Texto y Edición solo cambia lo que muestra el dorso; con Imagen de por medio, las tarjetas se giran
  if ((antes === "imagen") === (v === "imagen")) return;
  estado.giradas.clear();
  const ts = [...document.querySelectorAll("#detalle .casilla-v.giro, .visor-t-slide")];
  ts.forEach((t, i) => {
    const giro = t.querySelector(".giro-in") || t; giro.style.setProperty("--retraso", pocoMovimiento() ? "0ms" : `${Math.min(i, 10) * 20}ms`);
    ladoTarjeta(t, v !== "imagen");
  });
};
document.documentElement.dataset.modoTarjetas = estado.modoTarjetas;
ACC.girarCasilla = (el, id) => {
  const t = el.closest(".casilla-v, .visor-t-slide"), k = String(id);
  estado.giradas.has(k) ? estado.giradas.delete(k) : estado.giradas.add(k);
  if (!t) return pintarDetalle();
  const atras = !t.classList.contains("girada");
  ladoTarjeta(t, atras);
  // El foco pasa a la cara visible, para el teclado y el lector de pantalla
  setTimeout(() => (atras ? t.querySelector(".dorso textarea, .dorso input") : t.querySelector(".cara .casilla-girar"))?.focus({ preventScroll: true }), 60);
};
{ const base = ACC.pctCasilla; ACC.pctCasilla = (el, id) => { if (el?.closest?.(".tablero-9, .cumbre-tarjeta, .tablero")) return ACC.editarTarjeta(el, id); base(el, id); }; }

/* ---------- Mover: dejar presionada (dedo) o arrastrar (mouse); las demás se apartan ---------- */
{
  let arr = null;
  const PRESION = 380, TOLERANCIA = 8;
  const tarjetasDe = t => [...t.parentElement.children].filter(x => x.classList.contains("casilla-v"));
  /** Anima de su lugar viejo al nuevo las tarjetas que se corrieron (técnica FLIP). */
  const deslizar = (lista, antes) => lista.forEach(x => {
    const a = antes.get(x), d = x.getBoundingClientRect(); if (!a) return;
    const dx = a.left - d.left, dy = a.top - d.top; if (!dx && !dy) return;
    x.animate([{ transform: `translate(${dx}px,${dy}px)` }, { transform: "none" }], { duration: 220, easing: "cubic-bezier(.2,0,0,1)" });
  });
  const empezar = () => {
    const { t, x0, y0 } = arr; arr.activo = true;
    const r = t.getBoundingClientRect(); arr.dx = x0 - r.left; arr.dy = y0 - r.top; arr.base = r;
    t.classList.add("levantada"); navigator.vibrate?.(8);
    document.documentElement.classList.add("moviendo-tarjeta");
  };
  const colocar = ev => {
    const { t } = arr, r = t.getBoundingClientRect();
    const actual = new DOMMatrix(getComputedStyle(t).transform);   // dónde está sin la traslación actual
    const ox = r.left - actual.m41, oy = r.top - actual.m42;
    t.style.transform = `translate(${ev.clientX - arr.dx - ox}px,${ev.clientY - arr.dy - oy}px) scale(1.04)`;
  };
  const mover = ev => {
    if (!arr) return;
    if (!arr.activo) {
      if (Math.hypot(ev.clientX - arr.x0, ev.clientY - arr.y0) <= TOLERANCIA) return;
      if (arr.raton) { clearTimeout(arr.timer); empezar(); } else return terminar();   // con el dedo, moverse antes es desplazar la página
    }
    const { t } = arr;
    colocar(ev);
    if (performance.now() - (arr.cambio || 0) > 180) {   // espera a que las demás terminen de apartarse
      t.style.pointerEvents = "none";
      const bajo = document.elementFromPoint(ev.clientX, ev.clientY)?.closest(".tablero .casilla-v.giro");
      t.style.pointerEvents = "";
      if (bajo && bajo !== t && bajo.parentElement === t.parentElement) {
        const lista = tarjetasDe(t), antes = new Map(lista.map(x => [x, x.getBoundingClientRect()]));
        const i = lista.indexOf(t), j = lista.indexOf(bajo);
        t.parentElement.insertBefore(t, i < j ? bajo.nextSibling : bajo);
        deslizar(lista.filter(x => x !== t), antes);
        arr.cambio = performance.now();
        colocar(ev);
      }
    }
    const caja = document.querySelector("#detalle .det-cuerpo") || document.scrollingElement;   // desplaza cerca del borde
    if (ev.clientY < 90) caja.scrollBy(0, -10); else if (ev.clientY > innerHeight - 70) caja.scrollBy(0, 10);
  };
  const terminar = () => {
    if (!arr) return;
    const { t, activo, timer } = arr; clearTimeout(timer); arr = null;
    removeEventListener("pointermove", mover); removeEventListener("pointerup", terminar); removeEventListener("pointercancel", terminar);
    document.documentElement.classList.remove("moviendo-tarjeta");
    if (!activo) return;
    // Un clic justo después de soltar no debe abrir la foto ni el selector de archivos
    const tragar = e => { e.stopPropagation(); e.preventDefault(); };
    addEventListener("click", tragar, { capture: true, once: true });
    setTimeout(() => removeEventListener("click", tragar, true), 350);
    const desde = t.getBoundingClientRect();
    t.classList.remove("levantada"); t.style.transform = "";
    const hasta = t.getBoundingClientRect();
    t.animate([{ transform: `translate(${desde.left - hasta.left}px,${desde.top - hasta.top}px) scale(1.04)` }, { transform: "none" }], { duration: 200, easing: "cubic-bezier(.2,0,0,1)" });
    // Guarda el nuevo orden: la cumbre sigue primero
    const ids = tarjetasDe(t).map(x => x.dataset.casilla), cs = casillasVision();
    const orden = [...cs.filter(c => !ids.includes(String(c.id))).map(c => String(c.id)), ...ids];
    orden.forEach((id, i) => { const c = cs.find(x => String(x.id) === id); if (c && (c.orden || 0) !== i) Store.cambiar("vision", c.id, { orden: i }); });
  };
  document.addEventListener("pointerdown", e => {
    const cara = e.target.closest?.(".tablero .casilla-v.giro .cara"); if (!cara || e.button > 0 || arr) return;
    if (e.target.closest(".casilla-pct-btn, .casilla-girar")) return;
    const t = cara.closest(".casilla-v"), raton = e.pointerType === "mouse";
    arr = { t, x0: e.clientX, y0: e.clientY, raton, activo: false };
    if (!raton) arr.timer = setTimeout(() => arr && empezar(), PRESION);
    addEventListener("pointermove", mover); addEventListener("pointerup", terminar); addEventListener("pointercancel", terminar);
  });
  // Mientras se mueve con el dedo, la página no se desplaza
  document.addEventListener("touchmove", e => { if (arr?.activo) e.preventDefault(); }, { passive: false });
  // Sin menú de "guardar imagen" al dejar presionada la foto
  document.addEventListener("contextmenu", e => { if (e.target.closest?.(".casilla-v.giro .cara")) e.preventDefault(); });
}

/* ---------- Ver en grande: todas las tarjetas a pantalla completa; se desliza, se gira y se edita ahí ---------- */
/** Orden del visor: la cumbre, los 8 campamentos en el orden de la rosa y luego las demás. */
function tarjetasEnOrden() {
  const cs = casillasVision(), md = window.Mandala && cs.length ? Mandala.datos() : null;
  const primero = [md?.cumbre, ...(md ? [0, 1, 2, 3, 4, 5, 6, 7].map(i => md.camps[i]) : [])].filter(Boolean);
  return { md, lista: [...primero, ...cs.filter(c => !primero.includes(c))] };
}
function slideTarjeta(c, md, atras) {
  const k = String(c.id), foto = fotoDePublicacion(c.publicacionId), a = apodoDe("vision", k), pct = avanceCasilla(c, md).pct;
  return `<section class="visor-t-slide${atras ? " girada" : ""}" data-casilla="${k}" style="--c:${COLOR_EJE[c.eje] || "#6B2A1A"}" aria-label="${esc(a || c.titulo || "Tarjeta")}">
    <div class="cara"${atras ? " inert" : ""}>
      ${foto ? `<img class="visor-t-foto" src="${esc(foto)}" alt="" loading="lazy" decoding="async">`
        : `<label class="visor-t-sinfoto"><input type="file" accept="image/*" hidden data-foto-vision="${k}">${ic("foto")}<span>Agregar mi foto</span><small>${esc(c.sugerencia || "")}</small></label>`}
      <div class="visor-t-pie">${a ? `<span class="visor-t-apodo">«${esc(a)}»</span>` : ""}
        <p class="visor-t-frase">${esc(c.afirmacion || "")}</p>
        <span class="visor-t-meta">${esc(c.titulo || "")}${c.titulo ? " · " : ""}${pct} %</span></div>
      <div class="visor-t-acciones"><button data-acc="girarCasilla" data-arg="${k}" aria-label="Girar y editar esta tarjeta">${ic("girar")}<span>Editar</span></button></div>
    </div>
    <div class="dorso"${atras ? "" : " inert"}>
      <div class="visor-t-lectura">${a ? `<span class="visor-t-apodo">«${esc(a)}»</span>` : ""}<p>${esc(c.afirmacion || c.titulo || "")}</p>
        <span class="visor-t-meta">${esc(c.titulo || "")}${c.titulo ? " · " : ""}${pct} %</span>
        <div class="visor-t-acciones-lect"><button data-acc="girarCasilla" data-arg="${k}">${ic("girar")}<span>Ver la foto</span></button>
          <button data-acc="modoTarjetas" data-arg="edicion">${ic("editar")}<span>Editar</span></button></div></div>
      <div class="visor-t-papel">
      <div class="dorso-cab"><span>${foto ? `<label class="btn mini"><input type="file" accept="image/*" hidden data-foto-vision="${k}">${ic("foto")} Cambiar foto</label>` : ""}</span>
        <button class="casilla-girar" data-acc="girarCasilla" data-arg="${k}" aria-label="Ver la foto" title="Ver la foto">${ic("girar")}</button></div>
      <input class="casilla-apodo" maxlength="32" aria-label="Apodo de la tarjeta" placeholder="Apodo (opcional)" data-resp="apodo|apodo:vision:${k}" value="${esc(a)}">
      ${edicionCasillaHtml(c, md)}
    </div></div>
  </section>`;
}
ACC.editarTarjeta = (el, id) => ACC.verTarjetas(el, id, true);
ACC.verTarjetas = (el, id, editar = false) => {
  const { md, lista } = tarjetasEnOrden(); if (!lista.length) return;
  const origen = document.activeElement, texto = estado.modoTarjetas !== "imagen";
  if (editar && estado.modoTarjetas !== "edicion") ACC.modoTarjetas(null, "edicion");
  const atras = c => editar && String(c.id) === String(id) ? true : (estado.modoTarjetas !== "imagen") !== estado.giradas.has(String(c.id));
  const v = document.createElement("div");
  v.className = "visor-t"; v.setAttribute("role", "dialog"); v.setAttribute("aria-modal", "true"); v.setAttribute("aria-label", "Tus tarjetas en grande");
  v.innerHTML = `<div class="visor-t-lista" tabindex="-1">${lista.map(c => slideTarjeta(c, md, atras(c))).join("")}</div>
    <div class="visor-t-barra"><button class="visor-t-cerrar" aria-label="Cerrar (Esc)">${ic("cerrar")}</button>
      <span class="modo-tarj oscuro" role="radiogroup" aria-label="Cómo ver tus tarjetas">${[["imagen", "foto", "Imagen"], ["texto", "frases", "Texto"], ["edicion", "editar", "Edición"]].map(([m, i, n]) =>
        `<button role="radio" aria-checked="${estado.modoTarjetas === m}" data-acc="modoTarjetas" data-arg="${m}">${ic(i)}<span>${n}</span></button>`).join("")}</span>
      <span class="visor-t-cuenta" aria-hidden="true"></span></div>`;
  document.body.appendChild(v); document.documentElement.classList.add("visor-abierto");
  const lst = v.querySelector(".visor-t-lista"), cuenta = v.querySelector(".visor-t-cuenta");
  const i0 = Math.max(0, lista.findIndex(c => String(c.id) === String(id)));
  lst.scrollTop = i0 * lst.clientHeight;
  const actual = () => Math.round(lst.scrollTop / Math.max(1, lst.clientHeight));
  const contar = () => { cuenta.textContent = `${actual() + 1} / ${lista.length}`; };
  contar(); lst.addEventListener("scroll", contar, { passive: true });
  lst.focus({ preventScroll: true });
  const cerrar = () => {
    removeEventListener("keydown", teclas);
    v.classList.add("saliendo"); document.documentElement.classList.remove("visor-abierto");
    setTimeout(() => v.remove(), pocoMovimiento() ? 0 : 160);
    if (!enEdicion()) pintarDetalle();   // la página muestra lo que se editó en grande
    origen?.focus?.({ preventScroll: true });
  };
  const ir = paso => lst.scrollTo({ top: (actual() + paso) * lst.clientHeight, behavior: pocoMovimiento() ? "auto" : "smooth" });
  const teclas = e => {
    if (e.target.closest?.("input, textarea, select")) { if (e.key === "Escape") e.target.blur(); return; }
    if (e.key === "Escape") { e.preventDefault(); cerrar(); }
    else if (e.key === "ArrowDown" || e.key === "PageDown") { e.preventDefault(); ir(1); }
    else if (e.key === "ArrowUp" || e.key === "PageUp") { e.preventDefault(); ir(-1); }
  };
  addEventListener("keydown", teclas);
  v.onchange = e => cambio(e); v.oninput = e => entrada(e);
  v.addEventListener("click", e => {
    if (e.target.closest(".visor-t-cerrar")) return cerrar();
    if (e.target.closest("button, [data-sel]")) clic(e);
    if (e.target.closest("[data-ir], [data-sel]")) { cerrar(); return; }   // ir a una meta: se cierra y la página navega
    const acc = e.target.closest("[data-acc]")?.dataset.acc;
    // Lo que cambia el estado o el avance se vuelve a pintar dentro de la tarjeta, sin cerrar el visor
    if (acc && !["girarCasilla", "modoTarjetas"].includes(acc)) setTimeout(() => {
      const sl = e.target.isConnected ? e.target.closest(".visor-t-slide") : v.querySelectorAll(".visor-t-slide")[actual()];
      const c = sl && casillasVision().find(x => String(x.id) === sl.dataset.casilla); if (!c) return;
      sl.querySelector(".dorso .casilla-edicion")?.replaceWith(Object.assign(document.createElement("div"), { innerHTML: edicionCasillaHtml(c, Mandala.datos()) }).firstElementChild);
    }, 30);
  });
};

/* ---------- Guardadas: las publicaciones que una persona guarda (en este navegador, como en la app) ---------- */
const GUARDADAS_KEY = "rutacima-guardadas";
const guardadas = () => { try { return JSON.parse(localStorage.getItem(GUARDADAS_KEY) || "[]"); } catch { return []; } };
const estaGuardada = id => guardadas().some(p => String(p.id) === String(id));
ACC.guardarPost = (el, id) => {
  const p = [...(estado.feed || []), ...(estado.misPosts || [])].find(x => String(x.id) === String(id));
  let g = guardadas(); const ya = g.some(x => String(x.id) === String(id));
  g = ya ? g.filter(x => String(x.id) !== String(id))
    : [{ id: p?.id ?? id, foto: p?.foto || null, texto: p?.texto || "", tipo: p?.tipo || "", eje: p?.eje || null, metaTitulo: p?.metaTitulo || "", autorNombre: p?.autorNombre || "", impulsos: p?.impulsos || 0, creadoEn: p?.creadoEn || Date.now() }, ...g].slice(0, 300);
  try { localStorage.setItem(GUARDADAS_KEY, JSON.stringify(g)); } catch {}
  cerrarHoja(); toast(ya ? "Quitada de Guardadas" : "Guardada · la encuentras en tu perfil");
};
{
  const base = ACC.masCima;
  ACC.masCima = (el, id) => {
    base(el, id);
    const lista = document.querySelector("#hojaInferior .hoja-lista"); if (!lista) return;
    const ya = estaGuardada(id);
    lista.insertAdjacentHTML("afterbegin", `<button data-acc="guardarPost" data-arg="${esc(id)}">${ic(ya ? "guardadoLleno" : "guardado")}<span>${ya ? "Quitar de Guardadas" : "Guardar"}</span></button>`);
  };
}

/* ---------- Mis publicaciones con cabecera de perfil: seguidores, siguiendo e impulsos ---------- */
const cifra = n => n == null ? "–" : n >= 1e6 ? (n / 1e6).toFixed(1).replace(".0", "") + " M" : n >= 1e4 ? Math.round(n / 1e3) + " mil" : n.toLocaleString("es");
async function cargarCifrasPerfil() {
  if (cargarCifrasPerfil.activo) return; cargarCifrasPerfil.activo = true;
  try {
    if (!Store.nube) { estado.cifras = { siguiendo: 48, seguidores: 126, impulsos: 1340 }; return; }
    const sb = Store.nube.sb, yo = Store.nube.dueno;
    const n = q => q.then(r => r.count ?? 0, () => null);
    const ids = (estado.misPosts || []).map(p => p.id).filter(Boolean).slice(0, 200);
    const [siguiendo, seguidores, impulsos] = await Promise.all([
      n(sb.from("follows").select("followed_id", { count: "exact", head: true }).eq("follower_id", yo)),
      n(sb.from("follows").select("follower_id", { count: "exact", head: true }).eq("followed_id", yo)),
      ids.length ? n(sb.from("votes").select("post_id", { count: "exact", head: true }).in("post_id", ids)) : Promise.resolve(0)]);
    estado.cifras = { siguiendo, seguidores, impulsos };
  } finally { cargarCifrasPerfil.activo = false; if (estado.sel === "publicaciones" && !enEdicion()) pintarDetalle(); }
}
ACC.pestanaPerfil = (el, v) => { estado.pestanaPerfil = v; pintarDetalle(); };
ACC.invitarAmigos = async () => {
  const u = Store.nube?.perfil?.username, url = "https://suroute.com/descargar.html";
  const texto = `Estoy subiendo a mi cumbre en SuRoute${u ? ` (@${u})` : ""}. Sígueme y subamos juntos:`;
  try { if (navigator.share) return await navigator.share({ title: "SuRoute", text: texto, url }); } catch { return; }
  try { await navigator.clipboard.writeText(`${texto} ${url}`); toast("Invitación copiada: pégala en tu chat"); } catch { toast(url); }
};
DET["perfil.publicaciones"] = () => {
  if (!estado.misPosts) cargarMisPosts();
  if (estado.misPosts && !estado.cifras) cargarCifrasPerfil();
  const p = perfil(), u = Store.nube?.perfil?.username, c = estado.cifras || {};
  const foto = globalThis.mv?.demo?.avatar || p.fotoPerfilUrl;
  const pest = estado.pestanaPerfil || "pub", todos = estado.misPosts || [];
  const lista = pest === "guardadas" ? guardadas() : pest === "privadas" ? todos.filter(x => x.visibilidad === "PRIVADA") : todos.filter(x => x.visibilidad !== "PRIVADA");
  const celda = x => `<button class="tk-celda" data-ir="comunidad" data-arg="post:${esc(x.id)}" aria-label="${altFoto(x)}">
      ${x.foto ? `<img src="${esc(x.foto)}" alt="" loading="lazy">` : `<span class="tk-texto" style="--c:${COLOR_EJE[x.eje] || "var(--burdeos)"}">${esc(String(x.metaTitulo || x.texto || "").slice(0, 90))}</span>`}
      ${pest === "privadas" ? `<span class="tk-n">${ic("candado")}</span>` : x.impulsos ? `<span class="tk-n">${ic("impulso")} ${cifra(x.impulsos)}</span>` : ""}</button>`;
  const vacio = { pub: "Aún no has publicado. Tu primera foto abre tu diario de vida.", privadas: "Lo que publiques solo para ti aparece aquí.", guardadas: "Toca ⋯ en una publicación de la comunidad y elige Guardar." }[pest];
  const tab = (v, icono, nombre) => `<button role="tab" aria-selected="${pest === v}" data-acc="pestanaPerfil" data-arg="${v}" aria-label="${nombre}" title="${nombre}">${ic(icono)}</button>`;
  return cab(av(ic("foto"), "var(--burdeos)"), "Mis publicaciones", u ? "@" + esc(u) : "Tu diario de vida", `<button class="btn mini oro" data-ir="comunidad" data-arg="publicar">${ic("mas")} Publicar</button>`) + cuerpo(
    `<section class="tk-perfil">
      <span class="tk-avatar">${foto ? `<img src="${esc(foto)}" alt="">` : esc((p.nombre || "Y")[0].toUpperCase())}</span>
      <h2>${esc(p.nombre || "Tu nombre")}</h2>${u ? `<p class="tk-usuario">@${esc(u)}</p>` : ""}
      <div class="tk-cifras">
        <span><b>${cifra(c.siguiendo)}</b><small>Siguiendo</small></span>
        <span><b>${cifra(c.seguidores)}</b><small>Seguidores</small></span>
        <span><b>${cifra(c.impulsos)}</b><small>Impulsos</small></span></div>
      <div class="tk-botones"><button class="btn" data-sel="perfil">${ic("editar")} Editar perfil</button>
        <button class="btn" data-acc="invitarAmigos" aria-label="Invitar amigos" title="Invitar amigos">${ic("personaMas")}</button></div>
      ${p.cumbreFrase ? `<p class="tk-bio">${esc(p.cumbreFrase)}</p>` : ""}
    </section>
    <div class="tk-pestanas" role="tablist" aria-label="Qué ver">${tab("pub", "cuadricula", "Publicaciones")}${tab("privadas", "candado", "Privadas")}${tab("guardadas", "guardado", "Guardadas")}</div>
    ${!estado.misPosts && pest !== "guardadas" ? `<p class="vacio-mini">Cargando…</p>` : lista.length ? `<div class="tk-rejilla">${lista.map(celda).join("")}</div>` : `<p class="vacio-mini">${vacio}</p>`}`, "max-width:720px");
};

/* ---------- Brújula 9×9: en Edición se escribe en la grilla; en Imagen o Texto, tocar un bloque lo abre ---------- */
{
  const abrir = (e, bl) => { if (estado.modoTarjetas === "edicion" && !bl.closest(".md-visor")) return; ACC.mandalaBloque(bl, bl.dataset.bloque); };
  document.addEventListener("click", e => {
    const bl = e.target.closest?.(".md-grilla .md-bloque[data-bloque]"); if (!bl || e.target.closest("textarea")) return;
    abrir(e, bl);
  });
  document.addEventListener("keydown", e => {
    const bl = e.target.closest?.(".md-grilla .md-bloque[data-bloque]"); if (!bl || e.target !== bl || (e.key !== "Enter" && e.key !== " ")) return;
    e.preventDefault(); abrir(e, bl);
  });
}
