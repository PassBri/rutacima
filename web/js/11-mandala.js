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
  altura(cel, est = "VACIO") {
    const base = 5 - this.anillo(cel.f, cel.c);
    if (cel.tipo === "CUMBRE") return base + 0.8;
    if (cel.tipo === "CAMPAMENTO") return base + 0.3;
    return base * ({ VACIO: 0.6, ESCRITO: 0.8, HECHO: 1 })[est];
  },
  estado(id, p) {
    if (id == null || !respuesta(this.clave(id, p)).trim()) return "VACIO";
    return respuesta(this.claveHecho(id, p)) === "1" ? "HECHO" : "ESCRITO";
  },
  /** La casilla "cumbre" va al centro; las demás (por su orden) son los campamentos. */
  datos() {
    const cs = casillasVision(), cumbre = cs.find(c => c.origen === "cumbre") || null;
    const camps = cs.filter(c => c !== cumbre).slice(0, 8);
    const titulo = (cumbre?.afirmacion || perfil().cumbreFrase || "Mi cumbre").trim();
    const color = i => COLOR_EJE[camps[i]?.eje] || this.PALETA[i];
    let escritos = 0, hechos = 0;
    camps.forEach(c => { for (let p = 0; p < 8; p++) { const e = this.estado(c.id, p); if (e !== "VACIO") escritos++; if (e === "HECHO") hechos++; } });
    return { cumbre, camps, titulo, color, escritos, hechos };
  },
};
window.Mandala = Mandala;

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
      <textarea data-resp="mandala|${Mandala.clave(camp.id, cel.paso)}" placeholder="Paso ${cel.paso + 1}" aria-label="Paso ${cel.paso + 1} de ${esc(camp.titulo)}">${esc(texto)}</textarea>
      <label class="md-check"><input type="checkbox" data-acc-change="mandalaHecho" data-arg="${camp.id}|${cel.paso}" ${est === "HECHO" ? "checked" : ""} ${texto.trim() ? "" : "disabled"}> Cumplido</label></span>`;
  return `<span class="md-c md-paso md-${est.toLowerCase()}" style="--c:${color}">${esc(texto)}${est === "HECHO" ? "<i>✓</i>" : ""}</span>`;
}

function mandalaHtml() {
  const d = Mandala.datos(), b = estado.mandalaBloque ?? 4, en3d = !!estado.mandala3d;
  const intro = `<p style="margin-top:0">La cuadrícula 9×9 (Mandala Chart), el método con el que Shohei Ohtani planeó su carrera: tu cumbre al centro, tus 8 campamentos alrededor (las casillas de tu vision board) y 8 pasos para cada uno. 64 pasos hacia tu cima.</p>
    <b class="md-progreso">${d.escritos} de 64 pasos escritos · ${d.hechos} cumplidos</b>
    <div class="barra" style="margin:8px 0 12px"><i style="width:${d.hechos / 64 * 100}%"></i></div>
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
      cols.push({ cel, alto: Mandala.altura(cel, est), rgb: color });
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
  async mandalaHecho(el, arg) {
    const [id, p] = arg.split("|");
    await responder("mandala", Mandala.claveHecho(id, p), el.checked ? "1" : "");
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
    } catch { /* sin banco (por ejemplo en la vista previa): solo las acciones del propósito */ }
    const usados = new Set(actuales.filter(t => t.trim()).map(t => t.trim().toLowerCase()));
    const cola = sug.map(t => String(t).trim()).filter(t => t && !usados.has(t.toLowerCase()) && usados.add(t.toLowerCase()));
    let n = 0;
    for (let p = 0; p < 8; p++) if (!actuales[p].trim() && cola.length) { await responder("mandala", Mandala.clave(c.id, p), cola.shift()); n++; }
    toast(n ? `Sugerí ${n} ${n === 1 ? "paso" : "pasos"}. Cámbialos a tu medida.` : "No encontré más sugerencias para este campamento.");
    pintarDetalle();
  },
});
