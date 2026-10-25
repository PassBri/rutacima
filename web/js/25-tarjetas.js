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

/* ---------- Girar la tarjeta ---------- */
(estado.giradas ||= new Set());
ACC.girarCasilla = (el, id) => {
  const t = el.closest(".casilla-v"), k = String(id), girar = !estado.giradas.has(k);
  girar ? estado.giradas.add(k) : estado.giradas.delete(k);
  if (!t) return pintarDetalle();
  t.classList.toggle("girada", girar);
  t.querySelector(".cara").inert = girar; t.querySelector(".dorso").inert = !girar;
  // El foco pasa a la cara visible, para el teclado y el lector de pantalla
  setTimeout(() => (girar ? t.querySelector(".casilla-apodo") : t.querySelector(".cara .casilla-girar"))?.focus({ preventScroll: true }), 60);
};
{ const base = ACC.pctCasilla; ACC.pctCasilla = (el, id) => { estado.giradas.add(String(id)); base(el, id); }; }

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
      const bajo = document.elementFromPoint(ev.clientX, ev.clientY)?.closest(".casilla-v.giro");
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
    const cara = e.target.closest?.(".casilla-v.giro .cara"); if (!cara || e.button > 0 || arr) return;
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

/* ---------- Visor a pantalla completa: deslizar hacia arriba para pasar de tarjeta ---------- */
ACC.verTarjetas = (el, id) => {
  const md = window.Mandala ? Mandala.datos() : null;
  const cs = casillasVision().filter(c => fotoDePublicacion(c.publicacionId));
  if (!cs.length) return;
  const origen = document.activeElement;
  const slide = c => {
    const k = String(c.id), foto = fotoDePublicacion(c.publicacionId), a = apodoDe("vision", k), pct = avanceCasilla(c, md).pct;
    return `<section class="visor-t-slide" data-casilla="${k}" style="--c:${COLOR_EJE[c.eje] || "#6B2A1A"}" aria-label="${esc(a || c.titulo || "Tarjeta")}">
      <img class="visor-t-fondo" src="${esc(foto)}" alt="" aria-hidden="true"><img class="visor-t-foto" src="${esc(foto)}" alt="">
      <div class="visor-t-pie">${a ? `<span class="visor-t-apodo">«${esc(a)}»</span>` : ""}
        <p class="visor-t-frase">${esc(c.afirmacion || "")}</p>
        <span class="visor-t-meta">${esc(c.titulo || "")}${c.titulo ? " · " : ""}${pct} %</span></div>
      <div class="visor-t-acciones"><button data-visor-editar="${k}" aria-label="Girar y editar esta tarjeta">${ic("girar")}<span>Editar</span></button></div>
    </section>`;
  };
  const v = document.createElement("div");
  v.className = "visor-t"; v.setAttribute("role", "dialog"); v.setAttribute("aria-modal", "true"); v.setAttribute("aria-label", "Tus tarjetas");
  v.innerHTML = `<div class="visor-t-lista" tabindex="-1">${cs.map(slide).join("")}</div>
    <button class="visor-t-cerrar" aria-label="Cerrar">${ic("cerrar")}</button><span class="visor-t-cuenta" aria-hidden="true"></span>`;
  document.body.appendChild(v); document.documentElement.classList.add("visor-abierto");
  const lista = v.querySelector(".visor-t-lista"), cuenta = v.querySelector(".visor-t-cuenta");
  const i0 = Math.max(0, cs.findIndex(c => String(c.id) === String(id)));
  lista.scrollTop = i0 * lista.clientHeight;
  const actual = () => Math.round(lista.scrollTop / Math.max(1, lista.clientHeight));
  const contar = () => { cuenta.textContent = `${actual() + 1} / ${cs.length}`; };
  contar(); lista.addEventListener("scroll", contar, { passive: true });
  lista.focus({ preventScroll: true });
  const cerrar = () => {
    removeEventListener("keydown", teclas);
    v.classList.add("saliendo"); document.documentElement.classList.remove("visor-abierto");
    setTimeout(() => v.remove(), matchMedia("(prefers-reduced-motion: reduce)").matches ? 0 : 160);
    origen?.focus?.({ preventScroll: true });
  };
  const ir = paso => lista.scrollTo({ top: (actual() + paso) * lista.clientHeight, behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth" });
  const teclas = e => {
    if (e.key === "Escape") { e.preventDefault(); cerrar(); }
    else if (e.key === "ArrowDown" || e.key === "PageDown" || e.key === " ") { e.preventDefault(); ir(1); }
    else if (e.key === "ArrowUp" || e.key === "PageUp") { e.preventDefault(); ir(-1); }
  };
  addEventListener("keydown", teclas);
  v.addEventListener("click", e => {
    if (e.target.closest(".visor-t-cerrar")) return cerrar();
    const ed = e.target.closest("[data-visor-editar]");
    if (ed) {
      const k = ed.dataset.visorEditar; cerrar();
      estado.giradas.add(k); pintarDetalle();
      requestAnimationFrame(() => { const t = document.querySelector(`.casilla-v[data-casilla="${k}"]`); t?.scrollIntoView({ block: "center" }); t?.querySelector(".casilla-apodo")?.focus({ preventScroll: true }); });
    }
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
