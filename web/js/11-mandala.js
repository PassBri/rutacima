"use strict";
/* Mandala 9×9 dentro del vision board (la misma regla que Mandala.kt en la app).
 * Adaptación del Mandala Chart (Hiroaki Matsumura; método Harada, el que usó Shohei Ohtani):
 * la cumbre al centro, las 8 casillas del vision board como campamentos y 8 pasos por campamento.
 * Los pasos se guardan como respuestas "mandala#<casilla>#<n>" (y "#hecho"), así viajan con la app.
 * En 3D la cuadrícula es una montaña escalonada que crece al escribir y cumplir los pasos. */
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
  anillo: (f, c) => Math.max(Math.abs(f - 4), Math.abs(c - 4)),
  altura(cel, est = "VACIO", avance = 0) {
    const base = 5 - this.anillo(cel.f, cel.c);
    if (cel.tipo === "CUMBRE") return base + 0.8;
    if (cel.tipo === "CAMPAMENTO") return base + 0.3;
    return base * ({ VACIO: 0.6, ESCRITO: 0.7 + 0.25 * Math.min(1, Math.max(0, avance)), HECHO: 1 })[est];
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

/** Una celda: la cumbre, un campamento (con su foto) o un paso. */
function mdCelda(d, cel, grande) {
  if (cel.tipo === "CUMBRE") {
    const foto = mdFoto(d.cumbre);
    return `<span class="md-c md-cumbre${foto ? " con-foto" : ""}">${foto ? `<img src="${esc(foto)}" alt="">` : ""}<b>${esc(d.titulo)}</b></span>`;
  }
  const camp = d.camps[cel.camp], color = d.color(cel.camp);
  if (cel.tipo === "CAMPAMENTO") {
    const foto = mdFoto(camp);
    return `<span class="md-c md-camp${camp ? "" : " libre"}" style="--c:${color}">${foto ? `<img src="${esc(foto)}" alt="">` : ""}<b>${camp ? esc(camp.titulo) : "+"}</b></span>`;
  }
  const est = Mandala.estado(camp?.id, cel.paso), texto = camp ? respuesta(Mandala.clave(camp.id, cel.paso)) : "";
  if (grande && camp) return `<span class="md-c md-paso md-${est.toLowerCase()}" style="--c:${color}">
      <textarea data-resp="mandala|${Mandala.clave(camp.id, cel.paso)}" ${est === "HECHO" ? "readonly" : ""} placeholder="Paso ${cel.paso + 1}" aria-label="Paso ${cel.paso + 1} de ${esc(camp.titulo)}">${esc(texto)}</textarea>
      ${mdJornadas(d, cel, camp, texto)}</span>`;
  return `<span class="md-c md-paso md-${est.toLowerCase()}" style="--c:${color}">${esc(texto)}${est === "HECHO" ? "<i>✓</i>" : ""}</span>`;
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
  const d = Mandala.datos(), b = estado.mandalaBloque ?? 4, en3d = !!estado.mandala3d;
  const intro = `<p style="margin-top:0">La cuadrícula 9×9 (Mandala Chart), el método con el que Shohei Ohtani planeó su carrera: tu cumbre al centro, tus 8 campamentos alrededor (las casillas de tu vision board) y 8 pasos para cada uno. 64 pasos hacia tu cima.</p>
    <b class="md-progreso">${d.escritos} de 64 pasos escritos · ${d.hechos} ganados</b>
    <div class="barra" style="margin:8px 0 12px"><i style="width:${d.hechos / 64 * 100}%"></i></div>
    ${ritmoHtml(d.ritmo)}
    <div class="segmentos" role="group" aria-label="Vista de la mandala">
      <button class="chip" data-acc="mandala3d" data-arg="0" aria-pressed="${!en3d}">Cuadrícula</button>
      <button class="chip" data-acc="mandala3d" data-arg="1" aria-pressed="${en3d}">Montaña 3D</button></div>`;
  if (en3d) return hoja(intro + `<canvas id="montana" class="md-montana" width="900" height="900" role="img"
      aria-label="Montaña 3D de tu mandala: ${d.hechos} de 64 pasos cumplidos"></canvas>
      <p class="suave" style="margin-bottom:0">Arrastra para girar la montaña. Cada paso que escribes y cumples la hace crecer.</p>`);
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
  const titulo = camp < 0 ? d.titulo : cs ? cs.titulo : "Campamento libre";
  return hoja(intro + `<div class="md-grilla">${grilla}</div><p class="suave">Toca un bloque para abrirlo y escribir sus pasos.</p>`) +
    hoja(`<h3 style="margin-top:0">${esc(titulo)}</h3><div class="md-grande">${grande}</div>
      <div class="botones" style="margin-top:12px">${cs ? `<button class="btn" data-acc="mandalaSugerir" data-arg="${cs.id}">${ic("coach")} Sugerir pasos</button>` : ""}
      ${camp >= 0 && !cs ? `<button class="btn" data-acc="casillaNueva">${ic("mas")} Agregar campamento</button>` : ""}
      ${camp >= 0 ? `<button class="btn mini" data-acc="mandalaBloque" data-arg="4">Ver el centro</button>` : ""}</div>`);
}

/** La montaña: 81 columnas en perspectiva, dibujadas de atrás hacia adelante. Gira sola hasta que la arrastran. */
const Montana = {
  angulo: Math.PI / 5, inclinacion: 0.75, sola: true, cuadro: 0,
  montar() {
    const cv = document.getElementById("montana");
    if (!cv || cv.dataset.listo) return;
    cv.dataset.listo = "1";
    let arrastre = null;
    cv.addEventListener("pointerdown", e => { arrastre = [e.clientX, e.clientY]; this.sola = false; cv.setPointerCapture(e.pointerId); });
    cv.addEventListener("pointermove", e => {
      if (!arrastre) return;
      this.angulo += (e.clientX - arrastre[0]) * 0.01;
      this.inclinacion = Math.min(1.25, Math.max(0.25, this.inclinacion - (e.clientY - arrastre[1]) * 0.004));
      arrastre = [e.clientX, e.clientY]; this.dibujar(cv);
    });
    const fin = () => { arrastre = null; };
    cv.addEventListener("pointerup", fin); cv.addEventListener("pointercancel", fin);
    let antes = 0;
    const paso = t => {
      if (!document.body.contains(cv)) return;
      if (this.sola && antes && !matchMedia("(prefers-reduced-motion: reduce)").matches) this.angulo += (t - antes) / 1000 * 0.25;
      antes = t; this.dibujar(cv); this.cuadro = requestAnimationFrame(paso);
    };
    cancelAnimationFrame(this.cuadro); this.cuadro = requestAnimationFrame(paso);
  },
  dibujar(cv) {
    const g = cv.getContext("2d"), W = cv.width, H = cv.height, d = Mandala.datos();
    const vacio = "#E5D9C8";
    g.clearRect(0, 0, W, H);
    const fondo = g.createLinearGradient(0, 0, 0, H); fondo.addColorStop(0, "#DDE7F0"); fondo.addColorStop(1, "#F6EBDD");
    g.fillStyle = fondo; g.fillRect(0, 0, W, H);
    const s = W / 13.5, cx = W / 2, cy = H * 0.6, ca = Math.cos(this.angulo), sa = Math.sin(this.angulo);
    const st = Math.sin(this.inclinacion), ct = Math.cos(this.inclinacion), zE = 1.0, m = 0.47;
    const p = (x, y, z) => [cx + (x * ca - y * sa) * s, cy + (x * sa + y * ca) * s * st - z * zE * s * ct];
    const mezcla = (a, b, t) => { const h = x => [1, 3, 5].map(i => parseInt(x.slice(i, i + 2), 16)); const A = h(a), B = h(b);
      return `rgb(${A.map((v, i) => Math.round(v + (B[i] - v) * t)).join(",")})`; };
    const cara = (pts, color) => { g.beginPath(); pts.forEach(([x, y], i) => i ? g.lineTo(x, y) : g.moveTo(x, y)); g.closePath(); g.fillStyle = color; g.fill(); g.strokeStyle = "rgba(0,0,0,.13)"; g.lineWidth = 1; g.stroke(); };
    const hex = v => /^#[0-9a-f]{6}$/i.test(v) ? v : "#E5D9C8";
    const cols = [];
    for (let f = 0; f < 9; f++) for (let c = 0; c < 9; c++) {
      const cel = Mandala.celda(f, c), camp = d.camps[cel.camp];
      const est = cel.tipo === "PASO" ? Mandala.estado(camp?.id, cel.paso) : "HECHO";
      const col = d.color(cel.camp);
      const color = cel.tipo === "CUMBRE" ? "#FFF8EC" : cel.tipo === "CAMPAMENTO" ? (camp ? col : hex(vacio))
        : est === "VACIO" ? hex(vacio) : est === "ESCRITO" ? mezcla(hex(vacio), col, 0.35) : mezcla(col, "#C9973B", 0.25);
      cols.push({ cel, alto: Mandala.altura(cel, est, cel.tipo === "PASO" ? d.avance(cel.camp, cel.paso) : 0), rgb: color });
    }
    cols.sort((a, b) => ((a.cel.c - 4) * sa + (a.cel.f - 4) * ca) - ((b.cel.c - 4) * sa + (b.cel.f - 4) * ca));
    const oscuro = color => color.startsWith("rgb") ? color.replace("rgb(", "").replace(")", "").split(",").map(Number) : [1, 3, 5].map(i => parseInt(color.slice(i, i + 2), 16));
    cols.forEach(({ cel, alto, rgb }) => {
      const x = cel.c - 4, y = cel.f - 4;
      [[0, 1, [x - m, y + m], [x + m, y + m]], [0, -1, [x + m, y - m], [x - m, y - m]], [1, 0, [x + m, y + m], [x + m, y - m]], [-1, 0, [x - m, y - m], [x - m, y + m]]]
        .forEach(([nx, ny, a, b]) => {
          if (nx * sa + ny * ca <= 0) return;
          const luz = 0.55 + 0.25 * Math.max(-1, Math.min(1, nx * ca - ny * sa));
          cara([p(a[0], a[1], 0), p(b[0], b[1], 0), p(b[0], b[1], alto), p(a[0], a[1], alto)], `rgb(${oscuro(rgb).map(v => Math.round(v * luz)).join(",")})`);
        });
      cara([p(x - m, y - m, alto), p(x + m, y - m, alto), p(x + m, y + m, alto), p(x - m, y + m, alto)], rgb);
      if (cel.tipo === "CUMBRE") { const [px, py] = p(x, y, alto); g.beginPath(); g.arc(px, py, s * 0.18, 0, Math.PI * 2); g.fillStyle = "#C9973B"; g.fill(); }
    });
  },
};
window.Montana = Montana;

Object.assign(ACC, {
  visionVista(_, v) { estado.visionVista = v; pintarDetalle(); },
  mandala3d(_, v) { estado.mandala3d = v === "1"; pintarDetalle(); },
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
  /** Llena los pasos vacíos: primero las acciones del propósito de la casilla, luego el banco de acciones de su eje. */
  async mandalaSugerir(_, arg) {
    const c = Store.get("vision", Number(arg)) || Store.get("vision", arg); if (!c) return;
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
    toast(n ? `Sugerí ${n} ${n === 1 ? "paso" : "pasos"}. Cámbialos a tu medida.` : "No encontré más sugerencias para este campamento.");
    pintarDetalle();
  },
});
