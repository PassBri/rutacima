"use strict";
/* ======================================================================
 * Pantallas: vincular, cargando, app
 * ====================================================================== */
function mostrar(cual) {
  if ($("pantallaCarta")) $("pantallaCarta").hidden = cual !== "carta";
  $("pantallaVincular").hidden = cual !== "vincular";
  $("pantallaCarga").hidden = cual !== "carga";
  $("pantallaApp").hidden = cual !== "app";
}
function mostrarCarga(t) { $("pantallaCarga").innerHTML = `<div><img src="${LOGO}" alt=""><p>${esc(t)}</p></div>`; mostrar("carga"); }

let sondeo = null, renovar = null;
async function mostrarVincular() {
  clearInterval(sondeo); clearInterval(renovar);
  const real = !!Store.nube;
  $("pantallaVincular").innerHTML = `<div class="marca"><img src="${LOGO}" alt="">Rutaalacima Web</div>
    <div class="tarjeta-v"><div><h1>Usa Rutaalacima en tu computador</h1>
      <ol class="pasos"><li>Abre <b>Rutaalacima</b> en tu teléfono.</li><li>Ve a <b>Perfil</b> y toca el ícono del computador (<b>Rutaalacima Web</b>).</li><li>Toca <b>Escanear código</b> y apunta tu teléfono a este código.</li></ol>
      <p class="suave" style="margin-top:22px;max-width:46ch">Es la misma app, con tu misma cuenta: lo que hagas aquí aparece en tu teléfono, y lo que hagas en el teléfono aparece aquí.</p></div>
    <div><div class="qr" id="qr"><img class="centro" src="${LOGO}" alt=""><button class="qr-recargar" id="qrRecargar" hidden>${ic("ruta")}<span>Toca para generar un código nuevo</span></button></div>
      ${real ? `<div class="qr-tiempo" aria-hidden="true"><i id="qrBarra"></i></div>` : ""}<div class="codigo-txt" id="codigoTxt">${real ? "· · · ·" : ""}</div>
      <p class="suave" style="text-align:center;font-size:13px;margin:8px 0 0" id="qrNota">${real ? "¿Sin cámara? Escribe este código en la app." : "Código de muestra: abre el proyecto en GitHub"}</p></div>
    <div class="v-pie">${real ? `<span class="suave">El código cambia cada pocos minutos. Este computador queda vinculado hasta que lo desvincules.</span>`
      : `<span><span class="pill">Demostración</span> <span class="suave">Este sitio todavía no tiene el servidor de Rutaalacima configurado. Prueba la app con una ruta de ejemplo.</span></span>`}
      <button class="btn ${real ? "" : "lleno"}" data-acc-v="demo">Ver la demostración</button></div>
    <div class="v-extra"><button class="carta-releer" id="cartaReleer">${ic("candado")} Leer la carta de bienvenida</button>
    ${enlacesLegales()}</div></div>`;
  mostrar("vincular");
  $("cartaReleer").onclick = () => mostrarCarta(() => mostrarVincular());
  if (!real) { pintarQR("https://github.com/PassBri/rutacima"); return; }
  const nuevo = async () => {
    try {
      const c = await Store.nube.crearCodigo();
      pintarQR(`rutacima://vincular?codigo=${c}`);
      $("codigoTxt").textContent = `${c.slice(0, 4)}-${c.slice(4)}`;
    } catch (e) { console.error(e); $("qrNota").textContent = "No se pudo crear el código. Revisa tu conexión y recarga la página."; }
  };
  // Como en WhatsApp: el código cambia solo cada minuto (con una barra que se vacía) y, si nadie lo
  // escanea en 5 minutos, se detiene hasta que toques para pedir uno nuevo.
  const VIDA = 60, VUELTAS = 5;
  let quedan = VIDA, vueltas = 0;
  const pausar = () => {
    clearInterval(renovar); $("qr").classList.add("vencido"); $("qrRecargar").hidden = false;
    const barra = $("qrBarra"); if (barra) barra.style.transform = "scaleX(0)";
    $("codigoTxt").textContent = "· · · ·"; $("qrNota").textContent = "El código se detuvo para ahorrar datos.";
  };
  const reloj = () => {
    quedan--;
    const barra = $("qrBarra"); if (barra) barra.style.transform = `scaleX(${Math.max(0, quedan / VIDA)})`;
    if (quedan <= 0) { if (++vueltas >= VUELTAS) return pausar(); quedan = VIDA; nuevo(); }
  };
  $("qrRecargar").onclick = async () => {
    $("qr").classList.remove("vencido"); $("qrRecargar").hidden = true; $("qrNota").textContent = "¿Sin cámara? Escribe este código en la app.";
    vueltas = 0; quedan = VIDA; const barra = $("qrBarra"); if (barra) barra.style.transform = "scaleX(1)";
    await nuevo(); clearInterval(renovar); renovar = setInterval(reloj, 1000);
  };
  await nuevo();
  renovar = setInterval(reloj, 1000);
  sondeo = setInterval(async () => {
    try { if (await Store.nube.vinculo()) { clearInterval(sondeo); clearInterval(renovar); entrarReal(); } } catch {}
  }, 2500);
}
function pintarQR(texto) {
  const box = $("qr"); box.querySelector(".codigo")?.remove();
  try {
    const q = qrcode(0, "H"); q.addData(texto); q.make();
    const img = document.createElement("img"); img.className = "codigo"; img.alt = "Código QR para vincular"; img.src = q.createDataURL(8, 0); box.prepend(img);
  } catch { box.insertAdjacentHTML("afterbegin", `<p class="suave codigo">Código QR no disponible</p>`); }
}

async function entrarReal() {
  mostrarCarga("Cargando tu ruta…");
  try {
    const filas = await Store.nube.cargarTodo();
    Store.datos = {};
    filas.forEach(f => Store.poner(f.tipo, f.clave, f.datos));
    let t = null;
    Store.nube.escuchar(ev => {
      if (ev.eventType === "DELETE") Store.quitar(ev.old.tipo, ev.old.clave);
      else Store.poner(ev.new.tipo, ev.new.clave, ev.new.datos);
      clearTimeout(t); t = setTimeout(refrescar, 250);
    });
    // Respaldo por si se pierde algún aviso en vivo
    setInterval(async () => {
      try { const fs = await Store.nube.cargarTodo(); Store.datos = {}; fs.forEach(f => Store.poner(f.tipo, f.clave, f.datos)); refrescar(); } catch {}
    }, 120000);
    abrirApp();
    if (!filas.length) toast("Tu cuenta aún no tiene datos: abre la app en el teléfono para que se sincronice.");
  } catch (e) { console.error(e); mostrarCarga("No se pudo cargar tu ruta. Revisa tu conexión y recarga la página."); }
}
function entrarDemo() { Store.nube = null; Store.datos = {}; sembrarDemo(); abrirApp(); }
function abrirApp() {
  Store.oyentes.clear(); Store.oyentes.add(refrescar);
  mostrar("app");
  ir("ruta", innerWidth > 900 && recordatorio() ? "vida" : null);
}
document.addEventListener("click", e => { if (e.target.closest("[data-acc-v='demo']")) { clearInterval(sondeo); clearInterval(renovar); entrarDemo(); } });

/* Ruta de ejemplo para la demostración (con la misma forma de los datos reales) */
function sembrarDemo() {

  const hoy = hoyFecha(), A = hoy.getFullYear(), M = hoy.getMonth() + 1;
  const azar = (a, m, d) => { const x = Math.sin(a * 372 + m * 31 + d) * 10000; return x - Math.floor(x); };
  Store.poner("perfil", 1, { id: 1, nombre: "Laura", cumbreFrase: "Abrir mi escuela de montaña y vivir de enseñar a otros a subir.", faseActual: "PREPARACION", anioInicioPlan: A, onboardingCompleto: true, anioNacimiento: 1990, mesNacimiento: 3, esperanzaVida: 100 });
  Store.poner("proposito", 1, { id: 1, orden: 0, titulo: "Escuela de montaña propia", prioridad: "A", eje: "TRA", descripcion: "Una escuela donde aprender a subir sea aprender a vivir.", indicadorExito: "La escuela se sostiene sola con 3 grupos al año.", porQueImporta: "", metasEspecificas: "", visualizacion: "", impacto: "", reflexionFinal: "", progreso: 0, horizonte: 10, creadoEn: Date.now() });
  [["Certificarme como guía de montaña", "MAE", A, 1], ["Ahorrar el capital semilla de la escuela", "VAL", A, 1], ["Correr una media maratón", "VOL", A, null], ["Primer grupo de 12 estudiantes", "VOZ", A + 2, 1], ["Sede propia con equipo de alquiler", "VAL", A + 4, 1]]
    .forEach(([t, eje, anio, prop], i) => Store.poner("meta_anio", 10 + i, { id: 10 + i, anio, propositoId: prop, titulo: t, subMetas: "", decision: "CONTINUAR", prioridad: "A", avance: anio > A ? 0 : 20, estado: anio > A ? "NO_INICIADA" : "EN_CURSO", obstaculo: "", proximaAccion: "", eje, indicador: "", observable: "" }));
  [["Terminar el módulo 4 del curso de guía", "MAE", 10], ["Ahorrar 10 % del ingreso del mes", "VAL", 11], ["3 trotes por semana", "VOL", 12], ["Publicar una ruta guiada en la comunidad", "VOZ", null]].forEach(([t, eje, anual], i) => {
    const dias = []; for (let d = 1; d < hoy.getDate(); d++) if (azar(A, M + i, d) < 0.55) dias.push(d);
    Store.poner("meta_mes", 20 + i, { id: 20 + i, anio: A, mes: M, orden: i, texto: t, dias: dias.join(","), cumplida: false, metaAnualId: anual, eje, indicador: "", objetivoDias: 20 });
  });
  for (let k = 1; k < 420; k++) {
    const d = new Date(hoy); d.setDate(d.getDate() - k);
    if (azar(d.getFullYear(), d.getMonth(), d.getDate()) < 0.62) Store.poner("checklist", iso(d), { fecha: iso(d), marcados: HABITOS.filter((h, j) => azar(d.getDate(), j, d.getMonth()) < 0.5).map(h => h[0]).join(",") });
  }
  Store.poner("agenda", iso(hoy), { fecha: iso(hoy), intencion: "Subir con calma y constancia", prioridad: "Estudiar el módulo 4 (1 hora)" });
  Store.poner("ejes", 30, { id: 30, fecha: Date.now() - 20 * DIA_MS, voluntad: 8, maestria: 7, voz: 5, valor: 6, evolucion: 7, trascendencia: 6, origen: "rapida", nota: "" });
  const dias = []; for (let k = 1; k < 30; k++) { const d = new Date(hoy); d.setDate(d.getDate() - k); if (d.getFullYear() === A && azar(k, 3, 7) < 0.7) dias.push(fraseIndice(d)); }
  Store.poner("frases", A, { dias: dias.sort((x, y) => x - y) });
  misPostsDemo.length = 0;
  // Recuerdos de ejemplo para el diario de vida (álbum de cada año)
  misPostsDemo.push(
    { id: "rec-1", anio: 2012, tipo: "LOGRO", eje: "MAE", texto: "Me gradué de la universidad con toda mi familia en primera fila.", metaTitulo: "Graduación", impulsos: 0, comentarios: 0, creadoEn: Date.parse("2012-11-20"), propio: true, visibilidad: "PRIVADA" },
    { id: "rec-2", anio: 2012, tipo: "REFLEXION", eje: "VOL", texto: "Primer trabajo: aprendí que la constancia vale más que el talento.", metaTitulo: "", impulsos: 0, comentarios: 0, creadoEn: Date.parse("2012-03-02"), propio: true, visibilidad: "PRIVADA" },
    { id: "rec-3", anio: 2020, tipo: "LOGRO", eje: "TRA", texto: "Corrí mis primeros 10 km en la montaña.", metaTitulo: "Primera carrera de montaña", impulsos: 0, comentarios: 0, creadoEn: Date.parse("2020-08-09"), propio: true, visibilidad: "PRIVADA" });
  [["LOGRO", "VOL", "Primer mes completo trotando 3 veces por semana."], ["VISION", "TRA", "Una escuela donde aprender a subir sea aprender a vivir."], ["EVIDENCIA", "MAE", "Módulo 3 del curso de guía: aprobado."]]
    .forEach(([tipo, eje, t], i) => misPostsDemo.push({ id: "mio-" + i, autorNombre: "Laura", tipo, eje, texto: t, metaTitulo: "", impulsos: 12 - i * 3, comentarios: 1, yoImpulse: false, creadoEn: Date.now() - (i + 2) * 86400000, propio: true, demo: false }));
}

document.addEventListener("dblclick", e => {
  const c = e.target.closest("[data-cima]"); if (!c || c.closest("button")) return;
  const p = (estado.feed || []).find(x => String(x.id) === c.dataset.cima);
  if (p && !p.yoImpulse) { estado.post = p; ACC.impulsar(); }
  const destello = document.createElement("span"); destello.className = "destello"; destello.innerHTML = ic("impulso");
  const nueva = $("detalle").querySelector(`[data-cima="${CSS.escape(c.dataset.cima)}"]`) || c; nueva.appendChild(destello); setTimeout(() => destello.remove(), 700);
});
/* ======================================================================
 * Carta de bienvenida: una carta antigua en papel, doblada y cerrada con el sello de cera. Se rompe
 * el sello (con el mismo quiebre de la frase del día) y la carta se despliega. Sale la primera vez.
 * ====================================================================== */
const CARTA = {
  para: "Para ti, que decidiste subir",
  saludo: "Querido caminante:",
  parrafos: [
    "Si estás leyendo esto, ya diste el paso más difícil: decidiste subir.",
    "Ruta a la Cima no es una lista de tareas. Es un mapa para encontrar tu cumbre —eso que le da sentido a tu vida— y una brújula para no perderla cuando llegue la niebla.",
    "Aquí no se corre: se asciende. Un paso cada día, una jornada a la vez, con tu voluntad, tu maestría, tu voz, tu valor, tu evolución y la huella que dejarás en otros.",
    "Habrá caídas. Son parte del camino, no su final. Y no subirás solo: una cordada entera camina contigo.",
    "Guarda esta carta. El día que llegues arriba, vuelve a leerla.",
  ],
  cierre: "Nos vemos en la cima.", firma: "Brian Suárez", rol: "Fundador de Ruta a la Cima",
};
const cartaLeida = () => { try { return localStorage.getItem("rutacima-carta") === "1"; } catch { return false; } };
function mostrarCarta(alSeguir) {
  const el = $("pantallaCarta"); if (!el) return alSeguir();
  el.innerHTML = `<div class="carta-escena"><p class="carta-etiqueta">CARTA DE BIENVENIDA</p>
    <article class="carta" id="carta" aria-label="Carta de bienvenida a Ruta a la Cima">
      <p class="carta-para">${CARTA.para}</p>
      <div class="carta-doblez"><img class="carta-lacre" id="cartaSello" src="${LOGO}" alt="Sello de cera: tócalo para romperlo" role="button" tabindex="0"></div>
      <div class="carta-sellada" id="cartaSellada"><p>Rompe el sello para leer tu carta.</p><button class="btn lleno" id="cartaRomper">${ic("candado")} Romper el sello</button></div>
      <div class="carta-cuerpo" id="cartaCuerpo" hidden><h2>${CARTA.saludo}</h2>${CARTA.parrafos.map(t => `<p>${t}</p>`).join("")}
        <p class="carta-cierre">${CARTA.cierre}</p><div class="carta-firma"><span>${CARTA.firma}</span><small>${CARTA.rol}</small></div></div>
    </article>
    <button class="btn lleno carta-seguir" id="cartaSeguir" hidden>${ic("ruta")} Empezar mi ascenso</button></div>`;
  mostrar("carta");
  let rota = false;
  const romper = () => {
    if (rota) return; rota = true;
    const img = $("cartaSello");
    const abrir = () => {
      $("cartaSellada").hidden = true; $("cartaCuerpo").hidden = false; $("carta").classList.add("abierta");
      setTimeout(() => { $("cartaSeguir").hidden = false; $("cartaSeguir").focus(); }, 900);
    };
    if (img?.complete && img.naturalWidth && typeof romperSello === "function") romperSello(img, 1984, () => setTimeout(abrir, 200)); else abrir();
  };
  $("cartaRomper").onclick = romper; $("cartaSello").onclick = romper;
  $("cartaSello").onkeydown = e => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); romper(); } };
  $("cartaSeguir").onclick = () => { try { localStorage.setItem("rutacima-carta", "1"); } catch {} alSeguir(); };
}
window.mostrarCarta = mostrarCarta;

async function iniciar() {
  try { const t = localStorage.getItem("rutacima-tema"); if (t) document.documentElement.dataset.theme = t; } catch {}
  ["lista", "detalle", "riel"].forEach(id => { $(id).onclick = clic; });
  $("detalle").onchange = cambio; $("detalle").oninput = entrada; $("lista").oninput = entrada; $("detalle").onsubmit = enviar;
  Contenido.cargarFrases(); Contenido.cargarIndice();
  // La primera visita empieza con la carta sellada (no en la demostración directa ni si ya se leyó)
  if (!cartaLeida() && location.hash !== "#demo" && !iniciar.carta) { iniciar.carta = true; mostrarCarta(() => iniciar()); return; }
  if (!MODO_REAL) { if (location.hash === "#demo") entrarDemo(); else mostrarVincular(); return; }
  mostrarCarga("Conectando…");
  try {
    Store.nube = new Nube();
    await Store.nube.sesion();
    if (await Store.nube.vinculo()) entrarReal(); else mostrarVincular();
  } catch (e) {
    console.error(e);
    Store.nube = null;
    mostrarCarga("No se pudo conectar con el servidor de Rutaalacima. Revisa que los inicios de sesión anónimos estén activados (ver README) y recarga.");
  }
}
iniciar();
