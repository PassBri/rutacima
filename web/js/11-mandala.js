"use strict";
/* Mandala 9×9 dentro del vision board (la misma regla que Mandala.kt en la app).
 * Adaptación del Mandala Chart (Hiroaki Matsumura; método Harada, el que usó Shohei Ohtani):
 * la cumbre al centro, las 8 casillas del vision board como campamentos y 8 pasos por campamento.
 * Los pasos se guardan como respuestas "mandala#<casilla>#<n>" (y "#hecho"), así viajan con la app.
 * El bloque central del 9×9 ES el vision board: al armar el tablero se llenan los pasos, y el 9×9 se
 * puede compartir como imagen en la comunidad. */
const Mandala = {
  ANILLO: [0, 1, 2, 3, 5, 6, 7, 8],
  PALETA: ["#8E3B26", "#5C4A8A", "#2F6F7A", "#8A6A1F", "#3F7A4A", "#6B2A1A", "#7A4A6B", "#4A5E7A"],
  clave: (id, p) => `mandala#${id}#${p}`,
  claveHecho: (id, p) => `mandala#${id}#${p}#hecho`,
  celda(f, c) {
    const bloque = Math.floor(f / 3) * 3 + Math.floor(c / 3), dentro = (f % 3) * 3 + c % 3;
    if (bloque === 4 && dentro === 4) return { f, c, tipo: "CUMBRE", camp: -1, paso: -1 };
    if (bloque === 4) return { f, c, tipo: "CAMPAMENTO", camp: this.ANILLO.indexOf(dentro), paso: -1, copia: true };
    if (dentro === 4) return { f, c, tipo: "CAMPAMENTO", camp: this.ANILLO.indexOf(bloque), paso: -1 };
    return { f, c, tipo: "PASO", camp: this.ANILLO.indexOf(bloque), paso: this.ANILLO.indexOf(dentro) };
  },
  estado(id, p) {
    if (id == null || !respuesta(this.clave(id, p)).trim()) return "VACIO";
    return respuesta(this.claveHecho(id, p)).trim() ? "HECHO" : "ESCRITO";
  },
  /** La casilla "cumbre" va al centro; las demás (por su orden) son los campamentos. */
  datos() {
    const cs = casillasVision(), cumbre = cs.find(c => c.origen === "cumbre") || null;
    const camps = cs.filter(c => c !== cumbre).slice(0, 8);
    const titulo = (cumbre?.afirmacion || perfil().cumbreFrase || "Mi cumbre").trim();
    const color = i => COLOR_EJE[camps[i]?.eje] || this.PALETA[i];
    let escritos = 0, hechos = 0;
    camps.forEach(c => { for (let p = 0; p < 8; p++) { const e = this.estado(c.id, p); if (e !== "VACIO") escritos++; if (e === "HECHO") hechos++; } });
    const pasos = MetodoCima.pasosDe(camps.map(c => c.id)), ritmo = MetodoCima.calcular(pasos, new Date());
    const pasoCima = (i, p) => pasos[i * 8 + p];
    return { cumbre, camps, titulo, color, escritos, hechos, pasos, ritmo, pasoCima, avance: (i, p) => MetodoCima.avance(pasoCima(i, p), ritmo.dificultad) };
  },
};
window.Mandala = Mandala;

/* Método Cima 9×52 (igual que MetodoCima.kt): cada paso se gana con jornadas (días distintos en que
 * avanzaste); la montaña personal mide 8.848.000 mm; cada 4 tramos la dificultad (jornadas por paso)
 * se ajusta como Bitcoin según el ritmo que lleva a la cumbre el 31 de diciembre (tope 4×, mínimo 1,
 * máximo 30) y queda fija para cada paso desde su primera jornada. */
const MetodoCima = {
  TOTAL: 64, MM_POR_PASO: Math.floor(8848000 / 64), INICIAL: 3, MIN: 1, MAX: 30, AJUSTE: 4, CICLO: 4,
  claveJornadas: (id, p) => Mandala.clave(id, p) + "#jornadas",
  claveReq: (id, p) => Mandala.clave(id, p) + "#req",
  leer(id, p) {
    if (id == null) return { escrito: false, jornadas: [], req: null, cumplido: false, cumplidoEn: null };
    const hecho = respuesta(Mandala.claveHecho(id, p)).trim();
    return {
      escrito: !!respuesta(Mandala.clave(id, p)).trim(),
      jornadas: [...new Set(respuesta(this.claveJornadas(id, p)).split(",").map(x => x.trim()).filter(x => /^\d{4}-\d\d-\d\d$/.test(x)))],
      req: Number(respuesta(this.claveReq(id, p))) || null,
      cumplido: !!hecho, cumplidoEn: /^\d{4}-\d\d-\d\d$/.test(hecho) ? hecho : null,
    };
  },
  pasosDe(ids) { const r = []; for (let c = 0; c < 8; c++) for (let p = 0; p < 8; p++) r.push(this.leer(ids[c], p)); return r; },
  avance(p, d) { return p.cumplido ? 1 : !p.jornadas.length ? 0 : Math.min(0.99, p.jornadas.length / (p.req || d)); },
  dificultades(ganados, pendientes, t) {
    const r = []; let d = this.INICIAL, rest = pendientes, gc = 0, ec = 0;
    for (let n = 0; n <= t; n++) {
      if (n > 0) {
        const prev = ganados[n - 1] || 0;
        gc += prev; ec += rest / (52 - (n - 1)); rest = Math.max(0, rest - prev);
        if (n % this.CICLO === 0) {
          if (ec > 0) d = Math.min(this.MAX, Math.max(this.MIN, Math.min(d * this.AJUSTE, Math.max(Math.max(1, Math.floor(d / this.AJUSTE)), Math.round(d * gc / ec)))));
          gc = 0; ec = 0;
        }
      }
      r.push(d);
    }
    return r;
  },
  proximoAjuste(hoy) {
    const sig = (Math.floor(Expedicion.tramoDe(hoy) / this.CICLO) + 1) * this.CICLO;
    return sig >= 52 ? new Date(hoy.getFullYear() + 1, 0, 1) : new Date(hoy.getFullYear(), 0, sig * 7 + 1);
  },
  calcular(pasos, hoy) {
    const anio = hoy.getFullYear(), t = Expedicion.tramoDe(hoy);
    const antes = pasos.filter(p => p.cumplido && (!p.cumplidoEn || Number(p.cumplidoEn.slice(0, 4)) < anio)).length;
    const pend = this.TOTAL - antes, ganados = new Array(52).fill(0);
    pasos.forEach(p => { if (p.cumplido && p.cumplidoEn && Number(p.cumplidoEn.slice(0, 4)) === anio) { const [y, m, dd] = p.cumplidoEn.split("-").map(Number); ganados[Expedicion.tramoDe(new Date(y, m - 1, dd))]++; } });
    const d = this.dificultades(ganados, pend, t);
    let antesTramo = 0; for (let i = 0; i < t; i++) antesTramo += ganados[i];
    const ganAnio = antesTramo + ganados[t];
    const dia = (x) => Date.UTC(x.getFullYear(), x.getMonth(), x.getDate()) / 864e5;
    const largo = (anio % 4 === 0 && anio % 100 !== 0) || anio % 400 === 0 ? 366 : 365;
    const transcurrido = (dia(hoy) - dia(new Date(anio, 0, 1)) + 1) / largo;
    return {
      tramo: t, dificultad: d[t], diasAjuste: Math.round(dia(this.proximoAjuste(hoy)) - dia(hoy)),
      esperado: (pend - antesTramo) / (52 - t), ganadosTramo: ganados[t], ganadosAnio: ganAnio,
      restantes: Math.max(0, pend - ganAnio), adelanto: ganAnio - pend * transcurrido,
      mm: Math.min(8848000, pasos.reduce((s, p) => s + Math.floor(this.avance(p, d[t]) * this.MM_POR_PASO), 0)),
    };
  },
};
window.MetodoCima = MetodoCima;

/** Pasos de respaldo por eje, por si no se puede leer el banco de acciones. */
const PASOS_BASE = {
  VOL: ["Levantarme a la misma hora 5 días", "Terminar lo que empiezo antes de abrir algo nuevo", "Una tarea difícil antes del mediodía", "Revisar mi semana cada domingo", "Decir no a una distracción al día", "Cumplir mi racha de hábitos", "Anotar una victoria diaria", "Cerrar el día con el plan de mañana"],
  MAE: ["Leer 20 minutos al día", "Practicar mi habilidad clave 30 minutos", "Pedir retroalimentación a un experto", "Terminar un curso del área", "Enseñar lo que aprendí a alguien", "Estudiar un caso de éxito al mes", "Crear un proyecto de práctica", "Medir mi avance cada mes"],
  VOZ: ["Escribir lo que pienso antes de reuniones", "Hablar en público una vez al mes", "Pedir lo que necesito con claridad", "Publicar una reflexión por semana", "Practicar escucha activa", "Dar una opinión honesta con respeto", "Grabarme y mejorar mi forma de hablar", "Compartir mi historia con alguien"],
  VAL: ["Escribir mis 5 valores", "Revisar si mis decisiones los respetan", "Un acto de servicio por semana", "Agradecer a una persona cada día", "Cumplir mi palabra en lo pequeño", "Pasar tiempo sin pantallas con mi familia", "Donar tiempo o recursos al mes", "Reflexionar 10 minutos en silencio"],
  EVO: ["Dormir 7 horas", "Moverme 30 minutos al día", "Tomar 8 vasos de agua", "Comer verduras en cada comida", "Chequeo médico al año", "Aprender algo fuera de mi zona de confort", "Revisar mis finanzas cada mes", "Evaluar mis ejes cada trimestre"],
  TRA: ["Ayudar a una persona a subir su montaña", "Acompañar a alguien como mentor", "Crear algo útil para mi comunidad", "Sumarme a una causa", "Compartir mis recursos gratis", "Celebrar los logros de otros", "Dejar por escrito lo que aprendí", "Invitar a alguien a planear su ruta"],
  TODOS: ["Definir el primer paso concreto", "Ponerle fecha a este campamento", "Buscar a alguien que ya lo logró", "Dedicarle una hora a la semana", "Medir mi avance cada mes", "Quitar un obstáculo", "Pedir ayuda a tiempo", "Celebrar cada paso cumplido"],
};

const mdFoto = c => c ? fotoDePublicacion(c.publicacionId) : null;
/** Lo que se lee en un campamento: la frase de la casilla (el rótulo suele ser genérico, como "Meta 2026"). */
const mdNombre = c => (c.afirmacion || c.titulo || "").trim();

/** Una celda: la cumbre, un campamento (con su foto) o un paso. */
function mdCelda(d, cel, grande) {
  if (cel.tipo === "CUMBRE") {
    const foto = mdFoto(d.cumbre);
    return `<span class="md-c md-cumbre${foto ? " con-foto" : ""}">${foto ? `<img src="${esc(foto)}" alt="">` : ""}<b>${esc(d.titulo)}</b></span>`;
  }
  const camp = d.camps[cel.camp], color = d.color(cel.camp);
  if (cel.tipo === "CAMPAMENTO") {
    const foto = mdFoto(camp);
    return `<span class="md-c md-camp${camp ? "" : " libre"}" style="--c:${color}">${foto ? `<img src="${esc(foto)}" alt="">` : ""}<b>${camp ? esc(mdNombre(camp)) : "+"}</b></span>`;
  }
  const est = Mandala.estado(camp?.id, cel.paso), texto = camp ? respuesta(Mandala.clave(camp.id, cel.paso)) : "";
  if (grande && camp) return `<span class="md-c md-paso md-${est.toLowerCase()}" style="--c:${color}">
      <textarea data-resp="mandala|${Mandala.clave(camp.id, cel.paso)}" ${est === "HECHO" ? "readonly" : ""} placeholder="Paso ${cel.paso + 1}" aria-label="Paso ${cel.paso + 1} de ${esc(camp.titulo)}">${esc(texto)}</textarea>
      ${mdJornadas(d, cel, camp, texto)}</span>`;
  return `<span class="md-c md-paso md-${est.toLowerCase()}" style="--c:${color}"><span class="t">${esc(texto)}</span>${est === "HECHO" ? "<i>✓</i>" : ""}</span>`;
}

/** Jornadas de un paso y el botón para registrar la de hoy. */
function mdJornadas(d, cel, camp, texto) {
  const p = d.pasoCima(cel.camp, cel.paso);
  if (p.cumplido) return `<span class="md-ganado">✓ Paso ganado</span>`;
  const req = p.req || d.ritmo.dificultad, hoyListo = p.jornadas.includes(iso(new Date()));
  return `<span class="md-jornadas"><span>${p.jornadas.length} de ${req} ${req === 1 ? "jornada" : "jornadas"}</span><i style="--f:${p.jornadas.length / req}"></i>
    <button class="btn mini" data-acc="mandalaAvanzar" data-arg="${camp.id}|${cel.paso}" ${!texto.trim() || hoyListo ? "disabled" : ""}>${hoyListo ? "Hoy ya cuenta" : "Avancé hoy"}</button></span>`;
}

/** El ritmo del año del Método Cima 9×52. */
function ritmoHtml(r) {
  const nf = n => n.toLocaleString("es", { maximumFractionDigits: 1 });
  return `<div class="md-ritmo"><b class="md-ritmo-t">Método Cima 9×52</b>
    <b>Tu montaña: ${r.mm.toLocaleString("es")} de 8.848.000 mm</b>
    <span>Ahora un paso nuevo pide <b>${r.dificultad} ${r.dificultad === 1 ? "jornada" : "jornadas"}</b></span>
    <span class="suave">Tramo ${r.tramo + 1} de 52 · esta semana ${r.ganadosTramo} de ${nf(r.esperado)} pasos esperados · ajuste en ${r.diasAjuste} días</span>
    <b class="${r.adelanto >= 0 ? "md-adelante" : ""}">${r.adelanto >= 0 ? `Vas ${nf(r.adelanto)} pasos adelante del ritmo` : `Te faltan ${nf(-r.adelanto)} pasos para ir al ritmo`}</b>
    <small>Un paso no se marca: se gana con jornadas, días distintos en que avanzaste en él. Cada 4 tramos la dificultad se ajusta como en Bitcoin: si vas muy rápido, un paso nuevo pide más jornadas; si te frenas, pide menos para que vuelvas. Así llegas a la cumbre el 31 de diciembre.</small></div>`;
}

function mandalaHtml() {
  const d = Mandala.datos(), b = estado.mandalaBloque ?? 4;
  const compartir = estado.mdCompartir
    ? `<div class="md-compartir"><p>Se publica una imagen de tu 9×9 con tu cumbre, las fotos de tu vision board y tus pasos. ¿Quién la puede ver?</p>
        <div class="botones"><button class="btn" data-acc="compartirMandala" data-arg="SEGUIDORES">Mis seguidores</button>
        <button class="btn lleno" data-acc="compartirMandala" data-arg="PUBLICA">Todos</button><button class="btn mini" data-acc="compartirMandala" data-arg="">Cancelar</button></div></div>`
    : `<button class="btn lleno md-btn-compartir" data-acc="compartirMandala" data-arg="?" ${mdPublicando ? "disabled" : ""}>${ic("publicar")} ${mdPublicando ? "Preparando la imagen…" : "Compartir mi 9×9 en la comunidad"}</button>`;
  const intro = `<h3 style="margin-top:0">Tu 9×9</h3>
    <p>Tu vision board es el centro de tu 9×9: tu cumbre en medio y tus 8 casillas alrededor. Cada casilla es un campamento con 8 pasos; al armar el tablero, los pasos se llenan solos y tú los ajustas.</p>`;
  const pie = `<b class="md-progreso">${d.escritos} de 64 pasos escritos · ${d.hechos} ganados</b>
    <div class="barra" style="margin:8px 0 12px"><i style="width:${d.hechos / 64 * 100}%"></i></div>${compartir}`;
  const grilla = [0, 1, 2, 3, 4, 5, 6, 7, 8].map(bl => `<button class="md-bloque${bl === b ? " elegido" : ""}" data-acc="mandalaBloque" data-arg="${bl}" aria-label="Abrir bloque ${bl + 1}">${
    [0, 1, 2, 3, 4, 5, 6, 7, 8].map(k => mdCelda(d, Mandala.celda(Math.floor(bl / 3) * 3 + Math.floor(k / 3), (bl % 3) * 3 + k % 3), false)).join("")}</button>`).join("");
  const camp = Mandala.ANILLO.indexOf(b), cs = camp >= 0 ? d.camps[camp] : null;
  const grande = [0, 1, 2, 3, 4, 5, 6, 7, 8].map(k => {
    const cel = Mandala.celda(Math.floor(b / 3) * 3 + Math.floor(k / 3), (b % 3) * 3 + k % 3);
    const html = mdCelda(d, cel, true);
    if (cel.tipo === "CAMPAMENTO" && cel.copia) return d.camps[cel.camp] ? `<button data-acc="mandalaBloque" data-arg="${Mandala.ANILLO[cel.camp]}">${html}</button>` : `<button data-acc="casillaNueva">${html}</button>`;
    if (cel.tipo === "CAMPAMENTO") return `<button data-acc="mandalaBloque" data-arg="4">${html}</button>`;
    return `<div>${html}</div>`;
  }).join("");
  const titulo = camp < 0 ? d.titulo : cs ? mdNombre(cs) : "Campamento libre";
  return hoja(intro + `<div class="md-grilla">${grilla}</div>` + pie) + hoja(ritmoHtml(d.ritmo).replace('class="md-ritmo"', 'class="md-ritmo" style="margin:0"')) +
    hoja(`<p class="suave" style="margin-top:0">Toca un bloque del 9×9 para abrirlo y escribir sus pasos.</p><h3 style="margin-top:0">${esc(titulo)}</h3><div class="md-grande">${grande}</div>
      <div class="botones" style="margin-top:12px">${cs ? `<button class="btn" data-acc="mandalaSugerir" data-arg="${cs.id}">${ic("coach")} Sugerir pasos</button>` : ""}
      ${camp >= 0 && !cs ? `<button class="btn" data-acc="casillaNueva">${ic("mas")} Agregar campamento</button>` : ""}
      ${camp >= 0 ? `<button class="btn mini" data-acc="mandalaBloque" data-arg="4">Ver el centro</button>` : ""}</div>`);
}



Object.assign(ACC, {
  /** "?" pregunta quién la puede ver; "" cancela; SEGUIDORES o PUBLICA publica la imagen. */
  async compartirMandala(_, vis) {
    if (vis === "?" || vis === "") { estado.mdCompartir = vis === "?"; pintarDetalle(); return; }
    estado.mdCompartir = false; mdPublicando = true; pintarDetalle();
    try {
      const blob = await imagenMandala();
      await publicarImagenVision(blob, vis, Mandala.datos().titulo + "\n#MetodoCima9x52", "Mi 9×9 · Método Cima");
      toast("Tu 9×9 ya está en la comunidad");
    } catch (e) { console.error(e); toast("No se pudo compartir. Inténtalo de nuevo."); }
    mdPublicando = false; pintarDetalle();
  },
  mandalaBloque(_, v) { estado.mandalaBloque = Number(v); pintarDetalle(); },
  /** Registra la jornada de hoy (Método Cima 9×52): el paso se gana al juntar las jornadas que pide. */
  async mandalaAvanzar(_, arg) {
    const [id, p] = arg.split("|"), d = Mandala.datos(), paso = MetodoCima.leer(id, Number(p));
    if (paso.cumplido) return;
    const hoy = iso(new Date()), req = paso.req || d.ritmo.dificultad;
    const jornadas = [...new Set([...paso.jornadas, hoy])].sort();
    await responder("mandala", MetodoCima.claveJornadas(id, p), jornadas.join(","));
    await responder("mandala", MetodoCima.claveReq(id, p), String(req));
    if (jornadas.length >= req) { await responder("mandala", Mandala.claveHecho(id, p), hoy); toast("¡Paso ganado! Tu montaña subió " + MetodoCima.MM_POR_PASO.toLocaleString("es") + " mm."); }
    else toast(`Jornada registrada: ${jornadas.length} de ${req}.`);
    pintarDetalle();
  },
  /** Llena los pasos vacíos de un campamento con sugerencias. */
  async mandalaSugerir(_, arg) {
    const c = Store.get("vision", Number(arg)) || Store.get("vision", arg); if (!c) return;
    const n = await llenarPasos(c);
    toast(n ? `Sugerí ${n} ${n === 1 ? "paso" : "pasos"}. Cámbialos a tu medida.` : "No encontré más sugerencias para este campamento.");
    pintarDetalle();
  },
});

let mdPublicando = false;

/** Llena los pasos vacíos: primero las acciones del propósito de la casilla, luego el banco de acciones de su eje. */
async function llenarPasos(c) {
  const actuales = [0, 1, 2, 3, 4, 5, 6, 7].map(p => respuesta(Mandala.clave(c.id, p)));
  const sug = [];
  if (String(c.origen || "").startsWith("proposito:")) {
    const pid = String(c.origen).slice(10);
    Store.lista("accion").filter(a => String(a.propositoId) === pid).sort((a, b) => (a.orden || 0) - (b.orden || 0)).forEach(a => sug.push(a.texto));
  }
  try {
    const banco = await Contenido.json("bancos/acciones.json");
    banco.filter(a => !c.eje || (a.ejes || []).includes(c.eje)).map(a => a.texto).sort(() => Math.random() - 0.5).forEach(t => sug.push(t));
  } catch { /* sin banco (por ejemplo en la vista previa): pasos base del eje */ }
  (PASOS_BASE[c.eje] || PASOS_BASE.TODOS).forEach(t => sug.push(t));
  const usados = new Set(actuales.filter(t => t.trim()).map(t => t.trim().toLowerCase()));
  const cola = sug.map(t => String(t).trim()).filter(t => t && !usados.has(t.toLowerCase()) && usados.add(t.toLowerCase()));
  let n = 0;
  for (let p = 0; p < 8; p++) if (!actuales[p].trim() && cola.length) { await responder("mandala", Mandala.clave(c.id, p), cola.shift()); n++; }
  return n;
}

/** Al llenar el vision board se llena el 9×9: cada campamento sin ningún paso recibe 8 sugeridos. */
async function llenarCampamentosVacios() {
  const { camps } = Mandala.datos();
  for (const c of camps) if ([0, 1, 2, 3, 4, 5, 6, 7].every(p => !respuesta(Mandala.clave(c.id, p)).trim())) await llenarPasos(c);
}
window.llenarCampamentosVacios = llenarCampamentosVacios;

/** Imagen 1080×1350 del 9×9 (igual que MandalaImagen.kt en la app). */
async function imagenMandala() {
  const d = Mandala.datos(), W = 1080, H = 1350, cv = document.createElement("canvas");
  cv.width = W; cv.height = H;
  const g = cv.getContext("2d");
  const cargar = url => new Promise(res => { if (!url) return res(null); const im = new Image(); im.crossOrigin = "anonymous"; im.onload = () => res(im); im.onerror = () => res(null); im.src = url; });
  const fotos = new Map();
  for (const c of [d.cumbre, ...d.camps]) if (c) fotos.set(c.id, await cargar(mdFoto(c)));
  const SERIF = '"Roboto Serif", Georgia, serif', SANS = 'Roboto, system-ui, sans-serif';
  const texto = (s, x, y, ancho, tam, color, peso, lineas, centro = false, alto = null) => {
    if (!s) return;
    g.font = `${peso} ${tam}px ${peso === "700s" ? SERIF : SANS}`.replace("700s", "700"); g.fillStyle = color; g.textBaseline = "top";
    const palabras = [], filas = [];
    String(s).split(/\s+/).forEach(w => { while (w.length > 1 && g.measureText(w).width > ancho) { let k = w.length - 1; while (k > 1 && g.measureText(w.slice(0, k) + "-").width > ancho) k--; palabras.push(w.slice(0, k) + "-"); w = w.slice(k); } palabras.push(w); });
    let fila = "";
    for (const w of palabras) { const prueba = fila ? fila + " " + w : w; if (g.measureText(prueba).width > ancho && fila) { filas.push(fila); fila = w; } else fila = prueba; }
    if (fila) filas.push(fila);
    const vis = filas.slice(0, lineas);
    if (filas.length > lineas) { let u = vis[lineas - 1]; while (u && g.measureText(u + "…").width > ancho) u = u.slice(0, -1); vis[lineas - 1] = u + "…"; }
    const lh = tam * 1.15, top = alto != null ? y + (alto - vis.length * lh) / 2 : y;
    g.textAlign = centro ? "center" : "left";
    vis.forEach((f, i) => g.fillText(f, centro ? x + ancho / 2 : x, top + i * lh));
  };
  const redondo = (x, y, w, h, r) => { g.beginPath(); g.roundRect ? g.roundRect(x, y, w, h, r) : g.rect(x, y, w, h); };
  const mezcla = (a, b, t) => { const h = x => [1, 3, 5].map(i => parseInt(x.slice(i, i + 2), 16)); const A = h(a), B = h(b); return `rgb(${A.map((v, i) => Math.round(v + (B[i] - v) * t)).join(",")})`; };
  const foto = (im, x, y, s, velo) => { if (!im) return; const l = Math.min(im.width, im.height); g.save(); redondo(x, y, s, s, 14); g.clip(); g.drawImage(im, (im.width - l) / 2, (im.height - l) / 2, l, l, x, y, s, s); g.fillStyle = velo; g.fillRect(x, y, s, s); g.restore(); };
  g.fillStyle = "#F7F3EE"; g.fillRect(0, 0, W, H);
  texto("MI 9×9 · MÉTODO CIMA", 40, 40, 1000, 26, "#6B2A1A", 700, 1);
  g.font = `700 44px ${SERIF}`;
  texto(d.titulo, 40, 78, 1000, 44, "#2B1E18", "700s", 2);
  const x0 = 40, y0 = 190, lado = 1000, sb = 10, sc = 3, bl = (lado - 2 * sb) / 3, ce = (bl - 2 * sc) / 3;
  for (let f = 0; f < 9; f++) for (let c = 0; c < 9; c++) {
    const cel = Mandala.celda(f, c), x = x0 + Math.floor(c / 3) * (bl + sb) + (c % 3) * (ce + sc), y = y0 + Math.floor(f / 3) * (bl + sb) + (f % 3) * (ce + sc);
    if (cel.tipo === "CUMBRE") {
      const gr = g.createLinearGradient(0, y, 0, y + ce); gr.addColorStop(0, "#FFF8EC"); gr.addColorStop(1, "#C9973B");
      g.fillStyle = gr; redondo(x, y, ce, ce, 14); g.fill();
      const im = d.cumbre && fotos.get(d.cumbre.id); foto(im, x, y, ce, "rgba(0,0,0,.4)");
      texto(d.titulo, x + 6, y, ce - 12, 17, im ? "#fff" : "#3A1A10", 700, 5, true, ce);
    } else if (cel.tipo === "CAMPAMENTO") {
      const cp = d.camps[cel.camp], col = d.color(cel.camp);
      g.fillStyle = cp ? col : "#EDE4D9"; redondo(x, y, ce, ce, 14); g.fill();
      const im = cp && fotos.get(cp.id); foto(im, x, y, ce, col + "8C");
      texto(cp ? mdNombre(cp) : "", x + 6, y, ce - 12, 15, "#fff", 700, 5, true, ce);
    } else {
      const cp = d.camps[cel.camp], col = d.color(cel.camp), est = Mandala.estado(cp?.id, cel.paso);
      g.fillStyle = est === "VACIO" ? "#EDE4D9" : mezcla("#FFFFFF", col, est === "HECHO" ? 0.45 : 0.16); redondo(x, y, ce, ce, 10); g.fill();
      texto(cp ? respuesta(Mandala.clave(cp.id, cel.paso)) : "", x + 5, y, ce - 10, 14, "#2B1E18", 400, 5, true, ce);
      if (est === "HECHO") { g.fillStyle = col; g.beginPath(); g.arc(x + ce - 14, y + 14, 10, 0, Math.PI * 2); g.fill(); texto("✓", x + ce - 24, y + 6, 20, 15, "#fff", 700, 1, true); }
    }
  }
  const pie = y0 + lado + 14;
  texto(`${d.escritos} de 64 pasos escritos · ${d.hechos} ganados`, 40, pie, 1000, 30, "#2B1E18", 700, 1);
  texto(`Tu montaña: ${d.ritmo.mm.toLocaleString("es")} de 8.848.000 mm`, 40, pie + 44, 1000, 26, "#6B2A1A", 400, 1);
  g.fillStyle = "#6B2A1A"; g.fillRect(0, H - 64, W, 64);
  texto("Ruta a la Cima · Método Cima 9×52", 40, H - 50, 1000, 26, "#fff", "700s", 1, true);
  return await new Promise((res, rej) => cv.toBlob(b => b ? res(b) : rej(new Error("imagen")), "image/jpeg", 0.9));
}
window.imagenMandala = imagenMandala;

/** Publica una imagen como publicación de visión (en la nube o en el ejemplo). */
async function publicarImagenVision(blob, visibilidad, texto, metaTitulo) {
  if (Store.nube) {
    const sb = Store.nube.sb, postId = crypto.randomUUID(), ruta = `${Store.nube.dueno}/${postId}.jpg`;
    const up = await sb.storage.from("media").upload(ruta, blob, { upsert: true, contentType: "image/jpeg" });
    if (up.error) throw up.error;
    const url = sb.storage.from("media").getPublicUrl(ruta).data.publicUrl;
    const r = await sb.from("posts").insert({ id: postId, user_id: Store.nube.dueno, tipo: "VISION", texto, image_url: url, eje: null, anio: new Date().getFullYear(), visibilidad, meta_titulo: metaTitulo });
    if (r.error) throw r.error;
    (estado.misPosts ||= []).unshift({ id: postId, tipo: "VISION", texto, foto: url, metaTitulo, creadoEn: Date.now(), propio: true, visibilidad });
  } else {
    misPostsDemo.unshift({ id: "mio-" + nuevoId(), autorNombre: perfil().nombre || "Yo", tipo: "VISION", eje: null, texto, foto: URL.createObjectURL(blob), metaTitulo,
      impulsos: 0, comentarios: 0, yoImpulse: false, creadoEn: Date.now(), propio: true, visibilidad });
    estado.misPosts = misPostsDemo;
  }
  estado.feed = null;
}
