"use strict";
/* Mandala 9×9 dentro del vision board (la misma regla que Mandala.kt en la app).
 * Adaptación del Mandala Chart (Hiroaki Matsumura; método Harada, el que usó Shohei Ohtani):
 * la cumbre al centro, las 8 casillas del vision board como campamentos y 8 pasos por campamento.
 * Los pasos se guardan como respuestas "mandala#<casilla>#<n>" (y "#hecho"), así viajan con la app.
 * El bloque central del 9×9 ES el vision board: al armar el tablero se llenan los pasos, y el 9×9 se
 * puede compartir como imagen en la comunidad. */
const Mandala = {
  ANILLO: [0, 1, 2, 3, 5, 6, 7, 8],
  /** Brújula de la Cima: 8 campamentos fijos como rosa de los vientos (6 ejes, Confluencia y Campamento Base). */
  CAMPAMENTOS_FIJOS: ["CON", "TRA", "VAL", "EVO", "VOZ", "CAM", "VOL", "MAE"],
  RUMBOS: ["NO", "N", "NE", "O", "E", "SO", "S", "SE"],
  /** Los pasos van en sentido del reloj desde arriba a la izquierda. */
  RELOJ: [0, 1, 2, 5, 8, 7, 6, 3],
  /** Las 7 fases del Viaje Transformativo más el Legado (Desde la Cima). */
  FASES: ["Orientación", "Preparación", "Travesía", "Ascenso", "Cima", "Contemplación", "Descenso", "Legado"],
  COLORES: { CON: "#7A4A6B", CAM: "#4A5E7A" },
  NOMBRES: { CON: "Confluencia", CAM: "Campamento Base" },
  nombre(cod) { return this.NOMBRES[cod] || NOMBRE_EJE[cod] || cod; },
  clave: (id, p) => `mandala#${id}#${p}`,
  claveHecho: (id, p) => `mandala#${id}#${p}#hecho`,
  claveFoto: (id, p) => `mandala#${id}#${p}#foto`,
  claveSoltar: (id, p) => `mandala#${id}#${p}#soltar`,
  claveLlevar: (id, p) => `mandala#${id}#${p}#llevar`,
  /** Igual que Mandala.repartir en la app: cumbre al centro y cada campamento fijo en su rumbo. */
  repartir(cs) {
    const cumbre = cs.find(c => c.origen === "cumbre") || null, libres = cs.filter(c => c !== cumbre);
    const tomar = f => { const c = libres.find(f); if (c) libres.splice(libres.indexOf(c), 1); return c || null; };
    const camps = this.CAMPAMENTOS_FIJOS.map(cod => tomar(c => c.origen === "campamento:" + cod));
    this.CAMPAMENTOS_FIJOS.forEach((cod, i) => { if (!camps[i]) camps[i] = tomar(c => c.origen === "eje:" + cod) || tomar(c => c.eje === cod); });
    return { cumbre, camps };
  },
  celda(f, c) {
    const bloque = Math.floor(f / 3) * 3 + Math.floor(c / 3), dentro = (f % 3) * 3 + c % 3;
    if (bloque === 4 && dentro === 4) return { f, c, tipo: "CUMBRE", camp: -1, paso: -1 };
    if (bloque === 4) return { f, c, tipo: "CAMPAMENTO", camp: this.ANILLO.indexOf(dentro), paso: -1, copia: true };
    if (dentro === 4) return { f, c, tipo: "CAMPAMENTO", camp: this.ANILLO.indexOf(bloque), paso: -1 };
    return { f, c, tipo: "PASO", camp: this.ANILLO.indexOf(bloque), paso: this.RELOJ.indexOf(dentro) };
  },
  estado(id, p) {
    if (id == null || !respuesta(this.clave(id, p)).trim()) return "VACIO";
    return respuesta(this.claveHecho(id, p)).trim() ? "HECHO" : "ESCRITO";
  },
  datos() {
    const { cumbre, camps } = this.repartir(casillasVision());
    const titulo = (cumbre?.afirmacion || perfil().cumbreFrase || "Mi cumbre").trim();
    const color = i => this.COLORES[this.CAMPAMENTOS_FIJOS[i]] || COLOR_EJE[this.CAMPAMENTOS_FIJOS[i]];
    let escritos = 0, hechos = 0, evidencias = 0;
    camps.forEach(c => { if (c) for (let p = 0; p < 8; p++) if (mdFotoPaso(c, p)) evidencias++; });
    camps.filter(Boolean).forEach(c => { for (let p = 0; p < 8; p++) { const e = this.estado(c.id, p); if (e !== "VACIO") escritos++; if (e === "HECHO") hechos++; } });
    const pasos = MetodoCima.pasosDe(camps.map(c => c?.id)), ritmo = MetodoCima.calcular(pasos, new Date());
    const pasoCima = (i, p) => pasos[i * 8 + p];
    const cod = i => this.CAMPAMENTOS_FIJOS[i];
    const enlaces = (i, p) => camps[i] ? Travesia.enlaces(respuesta(Travesia.claveEnlaces(camps[i].id, p)), cod(i)) : [];
    const niebla = (i, p) => Travesia.enNiebla(pasos[i * 8 + p]);
    let confluencias = 0, enNiebla = 0;
    for (let i = 0; i < 8; i++) for (let p = 0; p < 8; p++) {
      if (pasos[i * 8 + p].cumplido && enlaces(i, p).length) confluencias++;
      if (niebla(i, p)) enNiebla++;
    }
    return { cumbre, camps, titulo, color, escritos, hechos, evidencias, confluencias, enNiebla, enlaces, niebla, cod, pasos, ritmo, pasoCima, avance: (i, p) => MetodoCima.avance(pasoCima(i, p), ritmo.dificultad) };
  },
};
window.Mandala = Mandala;

/* Travesía (igual que Travesia.kt): niebla, caídas, confluencia y cierre del año. */
const Travesia = {
  NIEBLA_DIAS: 14,
  ENLACES_AUTO: 2,
  CAIDAS: { FIN: "financiera", EMO: "emocional", DEC: "de decisión", CAR: "de carácter", IDE: "de identidad", CIR: "circunstancial" },
  claveCaida: (id, p) => `mandala#${id}#${p}#caida`,
  claveEnlaces: (id, p) => `mandala#${id}#${p}#enlaces`,
  claveLeccion: (a, n) => `brujula-${a}#leccion${n}`,
  claveCierre: a => `brujula-${a}#cierre`,
  dias: f => { const [y, m, d] = f.split("-").map(Number); return Date.UTC(y, m - 1, d) / 864e5; },
  diasQuieto(p, hoy = new Date()) {
    if (p.cumplido || !p.jornadas.length) return null;
    return Math.round(Date.UTC(hoy.getFullYear(), hoy.getMonth(), hoy.getDate()) / 864e5 - Math.max(...p.jornadas.map(this.dias)));
  },
  enNiebla(p, hoy) { const q = this.diasQuieto(p, hoy); return q != null && q >= this.NIEBLA_DIAS; },
  caida(v) { if (!v) return null; const [t, f] = v.split("|"); return this.CAIDAS[t] ? { tipo: t, fecha: f || null } : null; },
  enlaces(v, propio) { return [...new Set(String(v || "").split(",").map(x => x.trim().toUpperCase()).filter(x => Mandala.CAMPAMENTOS_FIJOS.includes(x) && x !== propio))]; },
  enlacesDeAccion(ejes, propio) { return [...new Set((ejes || []).map(x => x.toUpperCase()).filter(x => Mandala.CAMPAMENTOS_FIJOS.includes(x) && x !== propio))].join(","); },
  clavesALiberar(camps) {
    const r = [];
    camps.forEach(c => { if (c) for (let p = 0; p < 8; p++) if (respuesta(Mandala.claveHecho(c.id, p)).trim())
      [Mandala.clave(c.id, p), Mandala.claveHecho(c.id, p), MetodoCima.claveJornadas(c.id, p), MetodoCima.claveReq(c.id, p), Mandala.claveFoto(c.id, p),
        Mandala.claveSoltar(c.id, p), Mandala.claveLlevar(c.id, p), this.claveCaida(c.id, p), this.claveEnlaces(c.id, p)].forEach(k => { if (respuesta(k)) r.push(k); }); });
    return r;
  },
};
window.Travesia = Travesia;

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
  CON: ["Diseñar un proyecto que una tres de mis ejes", "Grabar un podcast sobre lo que aprendo", "Crear un curso con mi habilidad clave", "Escribir una guía que ayude a otros", "Lanzar una comunidad pequeña", "Unir mi trabajo y mi propósito en un servicio", "Presentar mi proyecto a alguien que pueda apoyarlo", "Medir cómo el proyecto mueve cada eje"],
  CAM: ["Elegir a mi mentor y pedirle una conversación", "Formar mi grupo de rendición de cuentas", "Reunirme con mi cordada cada semana", "Pedir retroalimentación honesta", "Ser mentor de alguien que empieza", "Agradecer a quien me sostiene", "Compartir mi avance sin victimizarme", "Cuidar mis tres círculos de apoyo"],
  VOL: ["Levantarme a la misma hora 5 días", "Terminar lo que empiezo antes de abrir algo nuevo", "Una tarea difícil antes del mediodía", "Revisar mi semana cada domingo", "Decir no a una distracción al día", "Cumplir mi racha de hábitos", "Anotar una victoria diaria", "Cerrar el día con el plan de mañana"],
  MAE: ["Leer 20 minutos al día", "Practicar mi habilidad clave 30 minutos", "Pedir retroalimentación a un experto", "Terminar un curso del área", "Enseñar lo que aprendí a alguien", "Estudiar un caso de éxito al mes", "Crear un proyecto de práctica", "Medir mi avance cada mes"],
  VOZ: ["Escribir lo que pienso antes de reuniones", "Hablar en público una vez al mes", "Pedir lo que necesito con claridad", "Publicar una reflexión por semana", "Practicar escucha activa", "Dar una opinión honesta con respeto", "Grabarme y mejorar mi forma de hablar", "Compartir mi historia con alguien"],
  VAL: ["Escribir mis 5 valores", "Revisar si mis decisiones los respetan", "Un acto de servicio por semana", "Agradecer a una persona cada día", "Cumplir mi palabra en lo pequeño", "Pasar tiempo sin pantallas con mi familia", "Donar tiempo o recursos al mes", "Reflexionar 10 minutos en silencio"],
  EVO: ["Dormir 7 horas", "Moverme 30 minutos al día", "Tomar 8 vasos de agua", "Comer verduras en cada comida", "Chequeo médico al año", "Aprender algo fuera de mi zona de confort", "Revisar mis finanzas cada mes", "Evaluar mis ejes cada trimestre"],
  TRA: ["Ayudar a una persona a subir su montaña", "Acompañar a alguien como mentor", "Crear algo útil para mi comunidad", "Sumarme a una causa", "Compartir mis recursos gratis", "Celebrar los logros de otros", "Dejar por escrito lo que aprendí", "Invitar a alguien a planear su ruta"],
  TODOS: ["Definir el primer paso concreto", "Ponerle fecha a este campamento", "Buscar a alguien que ya lo logró", "Dedicarle una hora a la semana", "Medir mi avance cada mes", "Quitar un obstáculo", "Pedir ayuda a tiempo", "Celebrar cada paso cumplido"],
};

const mdFoto = c => c ? fotoDePublicacion(c.publicacionId) : null;
/** Foto de evidencia de un paso ganado (Kit de Evidencias). */
const mdFotoPaso = (c, p) => { const id = c && respuesta(Mandala.claveFoto(c.id, p)); return id ? fotoDePublicacion(id) : null; };
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
    return `<span class="md-c md-camp${camp ? "" : " libre"}" style="--c:${color}">${foto ? `<img src="${esc(foto)}" alt="">` : ""}<b>${camp ? esc(mdNombre(camp)) : "+ " + Mandala.nombre(Mandala.CAMPAMENTOS_FIJOS[cel.camp])}</b></span>`;
  }
  const est = Mandala.estado(camp?.id, cel.paso), texto = camp ? respuesta(Mandala.clave(camp.id, cel.paso)) : "";
  const evid = mdFotoPaso(camp, cel.paso), fase = `<small class="md-fase">${cel.paso + 1} · ${Mandala.FASES[cel.paso]}</small>`;
  if (grande && camp) return `<span class="md-c md-paso md-${est.toLowerCase()}${evid ? " con-evid" : ""}" style="--c:${color}">
      ${evid ? `<img src="${esc(evid)}" alt="Evidencia">` : ""}${fase}
      <textarea data-resp="mandala|${Mandala.clave(camp.id, cel.paso)}" ${est === "HECHO" ? "readonly" : ""} placeholder="Paso ${cel.paso + 1}" aria-label="Paso ${cel.paso + 1} de ${esc(camp.titulo)}">${esc(texto)}</textarea>
      ${mdJornadas(d, cel, camp, texto)}${d.niebla(cel.camp, cel.paso) ? `<span class="md-niebla grande">☁</span>` : ""}${d.enlaces(cel.camp, cel.paso).length ? `<span class="md-enlace">⇄ ${d.enlaces(cel.camp, cel.paso).map(x => Mandala.nombre(x)).join(", ")}</span>` : ""}</span>`;
  const extra = (d.niebla(cel.camp, cel.paso) ? `<span class="md-niebla" title="Niebla: más de ${Travesia.NIEBLA_DIAS} días sin avanzar">☁</span>` : "") +
    (d.enlaces(cel.camp, cel.paso).length ? `<span class="md-enlace" title="Paso de confluencia">⇄</span>` : "");
  return `<span class="md-c md-paso md-${est.toLowerCase()}${evid ? " con-evid" : ""}" style="--c:${color}">${evid ? `<img src="${esc(evid)}" alt="">` : ""}<span class="t">${esc(texto)}</span>${est === "HECHO" ? "<i>✓</i>" : ""}${extra}</span>`;
}

/** Jornadas de un paso y el botón para registrar la de hoy. */
function mdJornadas(d, cel, camp, texto) {
  const p = d.pasoCima(cel.camp, cel.paso);
  if (p.cumplido) return `<span class="md-ganado">✓ Paso ganado</span>
    <button class="btn mini" data-acc="mdPaso" data-arg="${camp.id}|${cel.paso}">${mdFotoPaso(camp, cel.paso) ? "Evidencia y portal" : "📷 Evidencia y portal"}</button>`;
  const req = p.req || d.ritmo.dificultad, hoyListo = p.jornadas.includes(iso(new Date()));
  return `<span class="md-jornadas"><span>${p.jornadas.length} de ${req} ${req === 1 ? "jornada" : "jornadas"}</span><i style="--f:${p.jornadas.length / req}"></i>
    <button class="btn mini" data-acc="mandalaAvanzar" data-arg="${camp.id}|${cel.paso}" ${!texto.trim() || hoyListo ? "disabled" : ""}>${hoyListo ? "Hoy ya cuenta" : "Avancé hoy"}</button>
    <button class="md-mas" data-acc="mdPaso" data-arg="${camp.id}|${cel.paso}">Detalles</button></span>`;
}

/** Panel del paso ganado: foto de evidencia (Kit de Evidencias) y portal (qué suelto, qué llevo). */
function mdPanelPaso(d) {
  if (!estado.mdPaso) return "";
  const [id, p] = estado.mdPaso.split("|"), i = d.camps.findIndex(x => x && String(x.id) === id), c = d.camps[i];
  if (!c) return "";
  const n = Number(p), paso = d.pasoCima(i, n), evid = mdFotoPaso(c, n), cod = d.cod(i), en = d.enlaces(i, n);
  const caida = Travesia.caida(respuesta(Travesia.claveCaida(c.id, n))), quieto = Travesia.diasQuieto(paso);
  const req = paso.req || d.ritmo.dificultad;
  const niebla = paso.cumplido ? "" : caida
    ? `<div class="md-aviso">Reconociste una caída ${Travesia.CAIDAS[caida.tipo]}. No pierdes tus jornadas: este paso ahora pide ${req} para que vuelvas.
        <button class="btn mini" data-ir="aprende" data-arg="wb:bono_anti_abandono">Sistema Anti-Abandono</button></div>`
    : d.niebla(i, n) ? `<div class="md-aviso">Llevas ${quieto} días sin avanzar en este paso: entraste en la niebla. Perderse es parte del viaje; elige un protocolo para reorientarte o reconoce si te caíste.
        <div class="botones"><button class="btn mini" data-ir="aprende" data-arg="wb:niebla">Protocolos de la niebla</button>
        <button class="btn mini" data-acc="mdCaidaVer" data-arg="">Me caí</button></div>
        ${estado.mdCaidaVer ? `<div class="botones">${Object.entries(Travesia.CAIDAS).map(([k, v]) => `<button class="btn mini" data-acc="mdCaida" data-arg="${c.id}|${n}|${k}">${v}</button>`).join("")}
          <button class="btn mini" data-ir="aprende" data-arg="wb:caidas">Guía de Caídas</button></div>` : ""}</div>` : "";
  const chips = Mandala.CAMPAMENTOS_FIJOS.filter(x => x !== cod).map(x => `<button class="chip" data-acc="mdEnlace" data-arg="${c.id}|${n}|${x}" aria-pressed="${en.includes(x)}">${Mandala.nombre(x)}</button>`).join("");
  return `<div class="md-panel"><b>Paso ${n + 1} · ${Mandala.FASES[n]}</b><p style="margin:4px 0 8px">${esc(respuesta(Mandala.clave(c.id, n)))}</p>
    ${niebla}
    <h4>Este paso también activa:</h4><div class="chips">${chips}</div>
    ${paso.cumplido ? `${evid ? `<img class="md-evid" src="${esc(evid)}" alt="Foto de evidencia">` : ""}
    <label class="btn">${ic("foto")} ${evid ? "Cambiar foto de evidencia" : "Agregar foto de evidencia"}<input type="file" accept="image/*" hidden data-acc-change="mdEvidencia" data-arg="${c.id}|${n}"></label>
    <h4>Portal: antes del siguiente paso</h4>
    ${campo("Qué suelto", `<textarea data-resp="mandala|${Mandala.claveSoltar(c.id, n)}">${esc(respuesta(Mandala.claveSoltar(c.id, n)))}</textarea>`)}
    ${campo("Qué llevo", `<textarea data-resp="mandala|${Mandala.claveLlevar(c.id, n)}">${esc(respuesta(Mandala.claveLlevar(c.id, n)))}</textarea>`)}` : ""}
    <button class="btn mini" data-acc="mdPaso" data-arg="">Cerrar</button></div>`;
}

/** Cierre del año (Desde la Cima): 7 lecciones, la Brújula al diario de vida y una nueva montaña. */
function cierreHtml(d) {
  const a = new Date().getFullYear(), hecho = respuesta(Travesia.claveCierre(a));
  const lecciones = [0, 1, 2, 3, 4, 5, 6].map(n => campo(`Lección ${n + 1}`, `<textarea data-resp="mandala|${Travesia.claveLeccion(a, n)}">${esc(respuesta(Travesia.claveLeccion(a, n)))}</textarea>`)).join("");
  return `<div class="md-cierre"><h3 style="margin:0">Cierre del año ${a} · Desde la Cima</h3>
    <p>${hecho ? `Tu Brújula de ${a} ya está en tu diario de vida.` : "Celebra, integra y desciende con conciencia: escribe las 7 lecciones de tu año, guarda tu Brújula en el diario de vida y empieza una nueva montaña."}</p>
    ${estado.mdCierre ? `<div class="form">${lecciones}</div>
      <div class="botones"><button class="btn mini" data-ir="aprende" data-arg="wb:desde_cima">Abrir Desde la Cima</button>
      <button class="btn lleno" data-acc="mdCerrarAnio" ${mdPublicando ? "disabled" : ""}>${mdPublicando ? "Preparando la imagen…" : "Guardar mi año en el diario de vida"}</button>
      ${estado.mdNueva ? `<span class="md-aviso">Los pasos ganados se liberan (siguen en tu diario de vida con sus evidencias) y se llenan con pasos nuevos. Los que van a medio camino se quedan con sus jornadas.
        <button class="btn mini" data-acc="mdNuevaMontana" data-arg="si">Empezar</button><button class="btn mini" data-acc="mdNuevaMontana" data-arg="no">Cancelar</button></span>`
        : `<button class="btn" data-acc="mdNuevaMontana" data-arg="?">Empezar una nueva montaña</button>`}</div>`
      : `<button class="btn" data-acc="mdCierreVer">Escribir mis 7 lecciones</button>`}</div>`;
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
    ? `<div class="md-compartir"><p>Se publica tu Brújula para que la comunidad la explore (1 · 9 · 81), con una imagen para descargar. Elige qué sale y quién la ve:</p>
        <label class="md-op"><input type="checkbox" id="mdConPasos" checked> Mis 64 pasos</label>
        <label class="md-op"><input type="checkbox" id="mdConEvid"> Mis fotos de evidencia</label>
        <div class="botones"><button class="btn" data-acc="compartirMandala" data-arg="SEGUIDORES">Mis seguidores</button>
        <button class="btn lleno" data-acc="compartirMandala" data-arg="PUBLICA">Todos</button><button class="btn mini" data-acc="compartirMandala" data-arg="">Cancelar</button></div></div>`
    : `<button class="btn lleno md-btn-compartir" data-acc="compartirMandala" data-arg="?" ${mdPublicando ? "disabled" : ""}>${ic("publicar")} ${mdPublicando ? "Preparando la imagen…" : "Compartir mi 9×9 en la comunidad"}</button>`;
  const intro = `<h3 style="margin-top:0">Brújula de la Cima 9×9</h3>
    <p>Tu vision board es el centro: tu cumbre en medio y 8 campamentos fijos alrededor, como una rosa de los vientos (tus 6 ejes, Confluencia y Campamento Base). Cada campamento es un viaje de 8 pasos por las fases del Viaje Transformativo. Al ganar un paso, su foto de evidencia reemplaza a la visión: tu 9×9 pasa de soñado a vivido.</p>`;
  const pie = `<b class="md-progreso">${d.escritos} de 64 pasos escritos · ${d.hechos} ${d.hechos === 1 ? "ganado" : "ganados"}</b>
    <div class="barra" style="margin:8px 0 6px"><i style="width:${d.hechos / 64 * 100}%"></i></div>
    <p class="suave" style="margin:0">${d.evidencias} ${d.evidencias === 1 ? "foto" : "fotos"} de evidencia: lo vivido que ya reemplazó a lo soñado</p>
    ${d.confluencias ? `<p class="md-conf">${d.confluencias} ${d.confluencias === 1 ? "paso" : "pasos"} de confluencia: una acción que movió varios ejes</p>` : ""}
    ${d.enNiebla ? `<p class="md-nieblas">${d.enNiebla} ${d.enNiebla === 1 ? "paso" : "pasos"} en la niebla (más de ${Travesia.NIEBLA_DIAS} días sin avanzar)</p>` : ""}
    <div style="height:12px"></div>${compartir}`;
  const grilla = [0, 1, 2, 3, 4, 5, 6, 7, 8].map(bl => `<button class="md-bloque${bl === b ? " elegido" : ""}" data-acc="mandalaBloque" data-arg="${bl}" aria-label="Abrir bloque ${bl + 1}">${
    [0, 1, 2, 3, 4, 5, 6, 7, 8].map(k => mdCelda(d, Mandala.celda(Math.floor(bl / 3) * 3 + Math.floor(k / 3), (bl % 3) * 3 + k % 3), false)).join("")}</button>`).join("");
  const camp = Mandala.ANILLO.indexOf(b), cs = camp >= 0 ? d.camps[camp] : null;
  const grande = [0, 1, 2, 3, 4, 5, 6, 7, 8].map(k => {
    const cel = Mandala.celda(Math.floor(b / 3) * 3 + Math.floor(k / 3), (b % 3) * 3 + k % 3);
    const html = mdCelda(d, cel, true);
    if (cel.tipo === "CAMPAMENTO" && cel.copia) return d.camps[cel.camp] ? `<button data-acc="mandalaBloque" data-arg="${Mandala.ANILLO[cel.camp]}">${html}</button>` : `<button data-acc="crearCampamento" data-arg="${Mandala.CAMPAMENTOS_FIJOS[cel.camp]}">${html}</button>`;
    if (cel.tipo === "CAMPAMENTO") return `<button data-acc="mandalaBloque" data-arg="4">${html}</button>`;
    return `<div>${html}</div>`;
  }).join("");
  const cod = camp >= 0 ? Mandala.CAMPAMENTOS_FIJOS[camp] : null;
  const titulo = camp < 0 ? d.titulo : cs ? mdNombre(cs) : "Campamento libre";
  const rumbo = cod ? `<p class="md-rumbo" style="--c:${d.color(camp)}">Campamento ${esc(Mandala.nombre(cod))} · rumbo ${Mandala.RUMBOS[camp]}</p>` : "";
  // Líneas de confluencia: del paso al centro de cada campamento que también activa
  const pos = (f, c) => [(c + 0.5) / 9 * 100, (f + 0.5) / 9 * 100];
  let lineas = "";
  for (let f = 0; f < 9; f++) for (let c = 0; c < 9; c++) {
    const cel = Mandala.celda(f, c); if (cel.tipo !== "PASO") continue;
    const ganado = Mandala.estado(d.camps[cel.camp]?.id, cel.paso) === "HECHO";
    if (!ganado && !(estado.mdPaso === `${d.camps[cel.camp]?.id}|${cel.paso}`)) continue;   // solo las ganadas (y la del paso abierto)
    d.enlaces(cel.camp, cel.paso).forEach(x => {
      const bl = Mandala.ANILLO[Mandala.CAMPAMENTOS_FIJOS.indexOf(x)], [x1, y1] = pos(f, c), [x2, y2] = pos(Math.floor(bl / 3) * 3 + 1, (bl % 3) * 3 + 1);
      lineas += `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" class="${ganado ? "ganada" : ""}"/>`;
    });
  }
  const svg = lineas ? `<svg class="md-lineas" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">${lineas}</svg>` : "";
  // Zoom de 3 niveles (Material: container transform): cumbre (1) → vision board (9) → Brújula completa (81)
  const nivel = estado.mdNivel ?? 0, anim = estado.mdAnim ? ` md-anim-${estado.mdAnim}` : "";
  estado.mdAnim = null;
  const fotoC = mdFoto(d.cumbre);
  const puntos = i => { const g = [0, 1, 2, 3, 4, 5, 6, 7].filter(q => Mandala.estado(d.camps[i]?.id, q) === "HECHO").length;
    return `<span class="md-puntos">${[0, 1, 2, 3, 4, 5, 6, 7].map(q => `<i class="${q < g ? "si" : ""}"></i>`).join("")}</span>`; };
  const vista = nivel === 0
    ? `<button class="md-nivel1${anim}" data-acc="mdNivel" data-arg="1" aria-label="Abrir tu vision board">
        ${fotoC ? `<span class="md-impresion"><img src="${esc(fotoC)}" alt=""></span>` : `<span class="md-pico" aria-hidden="true">▲</span>`}
        <small>MI CUMBRE</small><b>${esc(d.titulo)}</b><hr><em>Tu montaña: ${d.ritmo.mm.toLocaleString("es")} de 8.848.000 mm</em></button>`
    : nivel === 1
      ? `<div class="md-nivel9${anim}">${[0, 1, 2, 3, 4, 5, 6, 7, 8].map(k => {
          const cel = Mandala.celda(3 + Math.floor(k / 3), 3 + k % 3);
          if (cel.tipo === "CUMBRE") return `<button class="md-tc${fotoC ? " con-foto" : ""}" data-acc="mdNivel" data-arg="2" aria-label="Ver el 9×9 completo">
            ${fotoC ? `<img src="${esc(fotoC)}" alt="">` : ""}<b>${esc(d.titulo)}</b></button>`;
          const c = d.camps[cel.camp], cod = Mandala.CAMPAMENTOS_FIJOS[cel.camp], foto = mdFoto(c);
          return `<button class="md-tk${foto ? " con-foto" : ""}" style="--c:${d.color(cel.camp)}" data-acc="${c ? "mdCampamento" : "crearCampamento"}" data-arg="${c ? cel.camp : cod}" aria-label="Abrir ${esc(Mandala.nombre(cod))}">
            ${foto ? `<img src="${esc(foto)}" alt="">` : ""}<small>${Mandala.RUMBOS[cel.camp]} · ${esc(Mandala.nombre(cod))}</small><b>${c ? esc(mdNombre(c)) : "+ Agregar"}</b>${puntos(cel.camp)}</button>`;
        }).join("")}</div>`
      : `<div class="md-grilla-caja${anim}"><div class="md-grilla">${grilla}</div>${svg}</div>`;
  const ayuda = ["Tu cumbre personal. Toca o haz doble clic para abrir tu vision board.",
    "Tu vision board: la cumbre y sus 8 campamentos. Toca la cumbre para ver el 9×9 o un campamento para abrir sus pasos. Para volver: la flecha, las migas o la tecla Esc.",
    "Tu Brújula completa: 81 casillas. Toca un bloque para abrirlo abajo. Para volver: la flecha, las migas o la tecla Esc."][nivel];
  const migas = ["Cumbre", "Vision board", "9×9"].map((t, i) => (i ? `<span class="md-sep" aria-hidden="true">›</span>` : "") +
    (i === nivel ? `<b aria-current="step">${t}</b>` : `<button data-acc="mdNivel" data-arg="${i}" class="${i > nivel ? "adelante" : ""}">${t}</button>`)).join("");
  const selector = `<nav class="md-barra" aria-label="Niveles de la Brújula">
    <button class="md-atras" data-acc="mdNivel" data-arg="${nivel - 1}" ${nivel ? "" : "hidden"} aria-label="Alejar (Esc)">${ic("volver")}</button>
    <span class="md-migas">${migas}</span><span class="md-num">${["1", "9", "81"][nivel]}</span></nav>`;
  return hoja(intro + selector + `<div class="md-zoom md-papel">${vista}</div><p class="suave md-ayuda">${ayuda}</p>` + pie) + hoja(ritmoHtml(d.ritmo).replace('class="md-ritmo"', 'class="md-ritmo" style="margin:0"')) +
    hoja(`<p class="suave" style="margin-top:0">Toca un bloque del 9×9 para abrirlo y escribir sus pasos.</p><h3 style="margin:0">${esc(titulo)}</h3>${rumbo}<div class="md-grande">${grande}</div>${mdPanelPaso(d)}
      <div class="botones" style="margin-top:12px">${cs ? `<button class="btn" data-acc="mandalaSugerir" data-arg="${cs.id}">${ic("coach")} Sugerir pasos</button>` : ""}
      ${camp >= 0 && !cs ? `<button class="btn" data-acc="crearCampamento" data-arg="${cod}">${ic("mas")} Agregar campamento</button>` : ""}
      ${camp >= 0 ? `<button class="btn mini" data-acc="mandalaBloque" data-arg="4">Ver el centro</button>` : ""}</div>`) + hoja(cierreHtml(d));
}



Object.assign(ACC, {
  /** "?" pregunta quién la puede ver; "" cancela; SEGUIDORES o PUBLICA publica la imagen. */
  async compartirMandala(_, vis) {
    if (vis === "?" || vis === "") { estado.mdCompartir = vis === "?"; pintarDetalle(); return; }
    const conPasos = $("mdConPasos")?.checked ?? true, conEvid = $("mdConEvid")?.checked ?? false;
    estado.mdCompartir = false; mdPublicando = true; pintarDetalle();
    try {
      const blob = await imagenMandala();
      const inst = await instantaneaBrujula(conPasos, conEvid);
      await publicarImagenVision(blob, vis, Mandala.datos().titulo + "\n#MetodoCima9x52", "Mi Brújula de la Cima 9×9", "VISION", null, inst);
      toast("Tu Brújula ya está en la comunidad: la pueden explorar.");
    } catch (e) { console.error(e); toast("No se pudo compartir. Inténtalo de nuevo."); }
    mdPublicando = false; pintarDetalle();
  },
  mandalaBloque(el, v) {
    // En el 81, el segundo toque sobre el centro (ya elegido) vuelve a la cumbre
    if (Number(v) === 4 && (estado.mandalaBloque ?? 4) === 4 && el?.classList?.contains("md-bloque") && Date.now() - (estado.mdNivelEn || 0) > 400) {
      estado.mdAnim = "alejar"; estado.mdNivel = 0; estado.mdNivelEn = Date.now(); pintarDetalle(); return;
    }
    estado.mandalaBloque = Number(v); estado.mdPaso = null; pintarDetalle();
  },
  /** Cambia de nivel de zoom; ignora el segundo clic de un doble clic para no saltarse un nivel. */
  mdNivel(_, v) {
    const n = Math.max(0, Math.min(2, Number(v))), ahora = Date.now(), actual = estado.mdNivel ?? 0;
    if (n === actual || ahora - (estado.mdNivelEn || 0) < 400) return;
    estado.mdAnim = n > actual ? "acercar" : "alejar"; estado.mdNivel = n; estado.mdNivelEn = ahora; pintarDetalle();
  },
  mdCampamento(_, i) {
    if (Date.now() - (estado.mdNivelEn || 0) < 400) return;
    estado.mandalaBloque = Mandala.ANILLO[Number(i)]; estado.mdPaso = null;
    estado.mdAnim = "acercar"; estado.mdNivel = 2; estado.mdNivelEn = Date.now(); pintarDetalle();
  },
  mdPaso(_, v) { estado.mdPaso = v || null; estado.mdCaidaVer = false; pintarDetalle(); },
  mdCaidaVer() { estado.mdCaidaVer = !estado.mdCaidaVer; pintarDetalle(); },
  /** Reconoce una caída: el paso conserva sus jornadas y pide como máximo la dificultad actual. */
  async mdCaida(_, arg) {
    const [id, p, tipo] = arg.split("|"), d = Mandala.datos(), paso = MetodoCima.leer(id, Number(p));
    const req = Math.min(paso.req || d.ritmo.dificultad, d.ritmo.dificultad);
    await responder("mandala", Travesia.claveCaida(id, p), `${tipo}|${iso(new Date())}`);
    await responder("mandala", MetodoCima.claveReq(id, p), String(req));
    estado.mdCaidaVer = false; toast("Caerse es parte del camino. Este paso ahora te pide menos para que vuelvas."); pintarDetalle();
  },
  async mdEnlace(_, arg) {
    const [id, p, cod] = arg.split("|"), c = casillasVision().find(x => String(x.id) === id); if (!c) return;
    const propio = String(c.origen || "").replace("campamento:", "");
    const actual = Travesia.enlaces(respuesta(Travesia.claveEnlaces(id, p)), propio);
    const nuevo = actual.includes(cod) ? actual.filter(x => x !== cod) : [...actual, cod];
    await responder("mandala", Travesia.claveEnlaces(id, p), nuevo.join(",")); pintarDetalle();
  },
  mdCierreVer() { estado.mdCierre = true; pintarDetalle(); },
  /** Guarda la Brújula del año en el diario de vida (reflexión privada con las 7 lecciones) y el resumen. */
  async mdCerrarAnio() {
    const a = new Date().getFullYear(), d = Mandala.datos();
    mdPublicando = true; pintarDetalle();
    try {
      const blob = await imagenMandala();
      const lecciones = [0, 1, 2, 3, 4, 5, 6].map(n => respuesta(Travesia.claveLeccion(a, n)).trim()).map((l, n) => l ? `${n + 1}. ${l}` : "").filter(Boolean);
      await publicarImagenVision(blob, "PRIVADA", [`Mi Brújula de la Cima ${a} · Las 7 lecciones de mi año`, ...lecciones].join("\n"), `Mi Brújula de la Cima ${a}`, "REFLEXION", null);
      await responder("mandala", Travesia.claveCierre(a), [d.hechos, d.escritos, d.ritmo.mm, d.evidencias, d.confluencias].join("|"));
      toast(`Tu Brújula de ${a} ya está en tu diario de vida.`);
    } catch (e) { console.error(e); toast("No se pudo guardar. Inténtalo de nuevo."); }
    mdPublicando = false; pintarDetalle();
  },
  /** Nueva montaña: libera los pasos ganados y llena los vacíos con pasos nuevos. */
  async mdNuevaMontana(_, v) {
    if (v === "?" || v === "no") { estado.mdNueva = v === "?"; pintarDetalle(); return; }
    const { camps } = Mandala.datos();
    for (const k of Travesia.clavesALiberar(camps)) await responder("mandala", k, "");
    estado.mdNueva = false;
    for (const c of camps) if (c) await llenarPasos(c);
    toast("Nueva montaña: tus pasos ganados siguen en tu diario de vida."); pintarDetalle();
  },
  /** Crea la casilla de un campamento fijo que falta y le sugiere sus 8 pasos. */
  async crearCampamento(_, cod) {
    const origen = "campamento:" + cod;
    if (!casillasVision().some(c => c.origen === origen)) {
      const p = proponerVision().find(x => x.origen === origen); if (!p) return;
      await Store.guardar("vision", { id: nuevoId(), orden: ORIGENES_VISION().indexOf(origen), publicacionId: null, ...p });
    }
    await llenarCampamentosVacios(); pintarDetalle();
  },
  /** Kit de Evidencias: la foto real del paso ganado (publicación privada que también llega al diario de vida). */
  async mdEvidencia(el, arg) {
    const archivo = el.files?.[0]; if (!archivo) return;
    const [id, p] = arg.split("|"), c = casillasVision().find(x => String(x.id) === id); if (!c) return;
    toast("Guardando tu evidencia…");
    try {
      const postId = await publicarImagenVision(archivo, "PRIVADA", respuesta(Mandala.clave(c.id, p)), (c.afirmacion || "").slice(0, 80), "EVIDENCIA", c.eje || null);
      await responder("mandala", Mandala.claveFoto(c.id, p), postId);
      toast("Evidencia guardada: lo vivido ya reemplaza a lo soñado.");
    } catch (e) { console.error(e); toast("No se pudo guardar la foto."); }
    pintarDetalle();
  },
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
  const cod = String(c.origen || "").startsWith("campamento:") ? c.origen.slice(11) : c.eje;
  const actuales = [0, 1, 2, 3, 4, 5, 6, 7].map(p => respuesta(Mandala.clave(c.id, p)));
  const sug = [], ejesDe = new Map();
  // Acciones de los propósitos del eje (en Confluencia, los propósitos sin eje)
  const props = Store.lista("proposito").filter(x => cod === "CON" ? !x.eje : cod === "CAM" ? false : x.eje === cod);
  if (String(c.origen || "").startsWith("proposito:")) props.push(...Store.lista("proposito").filter(x => String(x.id) === c.origen.slice(10)));
  props.forEach(pr => Store.lista("accion").filter(a => a.propositoId === pr.id && !a.hecha).sort((a, b) => (a.orden || 0) - (b.orden || 0)).forEach(a => sug.push(a.texto)));
  try {
    const banco = await Contenido.json("bancos/acciones.json");
    banco.forEach(a => ejesDe.set(String(a.texto).trim().toLowerCase(), a.ejes || []));
    const filtro = cod === "CON" ? a => (a.ejes || []).length >= 3
      : cod === "CAM" ? a => /mentor|grupo|cordada|comunidad|red |amig|familia|acompa|equipo/i.test(a.texto)
      : a => (a.ejes || []).includes(cod);
    banco.filter(filtro).sort(() => Math.random() - 0.5).forEach(a => sug.push(a.texto));
  } catch { /* sin banco (por ejemplo en la vista previa): pasos base del eje */ }
  (PASOS_BASE[cod] || PASOS_BASE.TODOS).forEach(t => sug.push(t));
  const usados = new Set(actuales.filter(t => t.trim()).map(t => t.trim().toLowerCase()));
  const cola = sug.map(t => String(t).trim()).filter(t => t && !usados.has(t.toLowerCase()) && usados.add(t.toLowerCase()));
  let n = 0, enlazados = [0, 1, 2, 3, 4, 5, 6, 7].filter(p => respuesta(Travesia.claveEnlaces(c.id, p))).length;
  for (let p = 0; p < 8; p++) if (!actuales[p].trim() && cola.length) {
    const t = cola.shift();
    await responder("mandala", Mandala.clave(c.id, p), t);
    // Solo las acciones que activan 3 ejes o más quedan como pasos de confluencia (hasta 2 por campamento)
    const ejes = ejesDe.get(t.toLowerCase()) || [];
    if (ejes.length >= 3 && enlazados < Travesia.ENLACES_AUTO) { await responder("mandala", Travesia.claveEnlaces(c.id, p), Travesia.enlacesDeAccion(ejes, cod)); enlazados++; }
    n++;
  }
  return n;
}

/** Al llenar el vision board se llena el 9×9: cada campamento sin ningún paso recibe 8 sugeridos. */
async function llenarCampamentosVacios() {
  const { camps } = Mandala.datos();
  for (const c of camps) if (c && [0, 1, 2, 3, 4, 5, 6, 7].every(p => !respuesta(Mandala.clave(c.id, p)).trim())) await llenarPasos(c);
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
  const evidencias = new Map();
  for (const c of d.camps) if (c) for (let p = 0; p < 8; p++) { const u = mdFotoPaso(c, p); if (u) evidencias.set(c.id + "|" + p, await cargar(u)); }
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
  texto("MI BRÚJULA DE LA CIMA 9×9", 40, 40, 1000, 26, "#6B2A1A", 700, 1);
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
      const ev = cp && evidencias.get(cp.id + "|" + cel.paso); foto(ev, x, y, ce, "rgba(0,0,0,.45)");
      texto(cp ? respuesta(Mandala.clave(cp.id, cel.paso)) : "", x + 5, y, ce - 10, 14, ev ? "#fff" : "#2B1E18", ev ? 700 : 400, 5, true, ce);
      if (est === "HECHO") { g.fillStyle = col; g.beginPath(); g.arc(x + ce - 14, y + 14, 10, 0, Math.PI * 2); g.fill(); texto("✓", x + ce - 24, y + 6, 20, 15, "#fff", 700, 1, true); }
    }
  }
  const pie = y0 + lado + 14;
  texto(`${d.escritos} de 64 pasos escritos · ${d.hechos} ${d.hechos === 1 ? "ganado" : "ganados"}`, 40, pie, 1000, 30, "#2B1E18", 700, 1);
  texto(`Tu montaña: ${d.ritmo.mm.toLocaleString("es")} de 8.848.000 mm`, 40, pie + 44, 1000, 26, "#6B2A1A", 400, 1);
  g.fillStyle = "#6B2A1A"; g.fillRect(0, H - 64, W, 64);
  texto("Ruta a la Cima · Método Cima 9×52", 40, H - 50, 1000, 26, "#fff", "700s", 1, true);
  return await new Promise((res, rej) => cv.toBlob(b => b ? res(b) : rej(new Error("imagen")), "image/jpeg", 0.9));
}
window.imagenMandala = imagenMandala;

/** Publica una imagen como publicación de visión (en la nube o en el ejemplo). */
async function publicarImagenVision(blob, visibilidad, texto, metaTitulo, tipo = "VISION", eje = null, brujula = null) {
  let id;
  if (Store.nube) {
    const sb = Store.nube.sb, postId = crypto.randomUUID(), ruta = `${Store.nube.dueno}/${postId}.jpg`; id = postId;
    const up = await sb.storage.from("media").upload(ruta, blob, { upsert: true, contentType: "image/jpeg" });
    if (up.error) throw up.error;
    const url = sb.storage.from("media").getPublicUrl(ruta).data.publicUrl;
    const fila = { id: postId, user_id: Store.nube.dueno, tipo, texto, image_url: url, eje, anio: new Date().getFullYear(), visibilidad, meta_titulo: metaTitulo };
    if (brujula) fila.brujula = brujula;
    const r = await sb.from("posts").insert(fila);
    if (r.error) throw r.error;
    (estado.misPosts ||= []).unshift({ id: postId, tipo, eje, texto, foto: url, metaTitulo, creadoEn: Date.now(), propio: true, visibilidad, brujula });
  } else {
    id = "mio-" + nuevoId();
    misPostsDemo.unshift({ id, autorNombre: perfil().nombre || "Yo", tipo, eje, texto, foto: URL.createObjectURL(blob), metaTitulo,
      impulsos: 0, comentarios: 0, yoImpulse: false, creadoEn: Date.now(), propio: true, visibilidad, brujula });
    estado.misPosts = misPostsDemo;
  }
  estado.feed = null;
  return id;
}

// Esc aleja un nivel de la Brújula (cuando está abierta y no se está escribiendo)
document.addEventListener("keydown", e => {
  if (e.key !== "Escape" || !document.querySelector(".md-zoom") || e.target.closest("input, textarea")) return;
  if (String(estado.sel || "").startsWith("brujula:")) { if ((estado.vbNivel ?? 0) > 0) { e.preventDefault(); ACC.vbNivel(null, String(estado.vbNivel - 1)); } return; }
  if ((estado.mdNivel ?? 0) > 0) { e.preventDefault(); ACC.mdNivel(null, String(estado.mdNivel - 1)); }
});

/* ======================================================================
 * Brújula compartida (igual que BrujulaCompartida.kt): la instantánea viaja con la publicación y la
 * comunidad la explora con el zoom 1 · 9 · 81 en solo lectura.
 * ====================================================================== */
/** Foto para la comunidad: con cuenta, la sube y devuelve su dirección pública. */
async function fotoParaCompartir(url, nombre) {
  if (!url) return "";
  if (!Store.nube || /^https?:/.test(url) && !url.startsWith("blob:")) return url;
  try {
    const b = await (await fetch(url)).blob(), ruta = `${Store.nube.dueno}/brujula/${Date.now()}-${nombre}.jpg`;
    const up = await Store.nube.sb.storage.from("media").upload(ruta, b, { upsert: true, contentType: b.type || "image/jpeg" });
    if (up.error) throw up.error;
    return Store.nube.sb.storage.from("media").getPublicUrl(ruta).data.publicUrl;
  } catch (e) { console.warn(e); return ""; }
}
async function instantaneaBrujula(conPasos, conEvid) {
  const d = Mandala.datos(), camps = [];
  for (let i = 0; i < 8; i++) {
    const c = d.camps[i]; if (!c) continue;
    const pasos = [];
    for (let p = 0; p < 8; p++) {
      const e = Mandala.estado(c.id, p);
      pasos.push({ t: conPasos ? respuesta(Mandala.clave(c.id, p)) : "", e: e === "HECHO" ? "H" : e === "ESCRITO" ? "E" : "V",
        f: conEvid ? await fotoParaCompartir(mdFotoPaso(c, p), `e${i}-${p}`) : "" });
    }
    camps.push({ c: Mandala.CAMPAMENTOS_FIJOS[i], t: mdNombre(c), f: await fotoParaCompartir(mdFoto(c), "c" + i), p: pasos });
  }
  return { v: 1, cumbre: d.titulo, fotoCumbre: await fotoParaCompartir(mdFoto(d.cumbre), "cumbre"), camps, mm: d.ritmo.mm,
    ganados: d.hechos, escritos: d.escritos, evidencias: conEvid ? d.evidencias : 0, fecha: iso(new Date()), conPasos };
}
const leerBrujula = b => { try { const o = typeof b === "string" ? JSON.parse(b) : b; return o && o.v >= 1 && Array.isArray(o.camps) ? o : null; } catch { return null; } };
window.leerBrujula = leerBrujula;

/** En el muro: la cumbre en papel con las cintas de avance de cada campamento. */
function brujulaMuro(p) {
  const b = leerBrujula(p.brujula); if (!b) return "";
  const cintas = Mandala.CAMPAMENTOS_FIJOS.map((cod, i) => { const c = b.camps.find(x => x.c === cod), g = c ? c.p.filter(x => x.e === "H").length : 0;
    return `<i style="--c:${Mandala.COLORES[cod] || COLOR_EJE[cod]};opacity:${.25 + .75 * g / 8}"></i>`; }).join("");
  return `<button class="bm-carta" data-ir="comunidad" data-arg="brujula:${esc(p.id)}" aria-label="Explorar la Brújula de ${esc(p.autorNombre || "")}">
    ${b.fotoCumbre ? `<span class="md-impresion"><img src="${esc(b.fotoCumbre)}" alt=""></span>` : `<span class="md-pico" aria-hidden="true">▲</span>`}
    <small>BRÚJULA DE LA CIMA 9×9</small><b>${esc(b.cumbre)}</b><span class="bm-cintas">${cintas}</span>
    <em>${b.ganados} ${b.ganados === 1 ? "paso ganado" : "pasos ganados"} · ${(b.mm || 0).toLocaleString("es")} mm</em><span class="btn lleno bm-explorar">Explorar su Brújula</span></button>`;
}

/** Vista de solo lectura de una Brújula compartida, con el mismo zoom 1 · 9 · 81. */
DET["comunidad.brujula"] = id => {
  const p = (estado.feed || []).concat(estado.misPosts || [], misPostsDemo || []).find(x => String(x.id) === String(id));
  const b = p && leerBrujula(p.brujula);
  if (estado.vbId !== id) { estado.vbId = id; estado.vbNivel = 0; estado.vbBloque = 4; }
  if (!b) return cab(avSello(), "Brújula", "No se encontró") + cuerpo(hoja(`<p>Esta Brújula ya no está disponible.</p>`));
  const nivel = estado.vbNivel ?? 0, bloque = estado.vbBloque ?? 4, anim = estado.vbAnim ? ` md-anim-${estado.vbAnim}` : ""; estado.vbAnim = null;
  const camp = cod => b.camps.find(x => x.c === cod);
  const color = i => Mandala.COLORES[Mandala.CAMPAMENTOS_FIJOS[i]] || COLOR_EJE[Mandala.CAMPAMENTOS_FIJOS[i]];
  const celda = (f, c) => {
    const cel = Mandala.celda(f, c);
    if (cel.tipo === "CUMBRE") return `<span class="md-c md-cumbre${b.fotoCumbre ? " con-foto" : ""}">${b.fotoCumbre ? `<img src="${esc(b.fotoCumbre)}" alt="">` : ""}<b>${esc(b.cumbre)}</b></span>`;
    const cod = Mandala.CAMPAMENTOS_FIJOS[cel.camp], k = camp(cod);
    if (cel.tipo === "CAMPAMENTO") return `<span class="md-c md-camp${k ? "" : " libre"}" style="--c:${color(cel.camp)}">${k?.f ? `<img src="${esc(k.f)}" alt="">` : ""}<b>${k ? esc(k.t) : ""}</b></span>`;
    const ps = k?.p?.[cel.paso] || { t: "", e: "V" }, est = ps.e === "H" ? "hecho" : ps.e === "E" ? "escrito" : "vacio";
    return `<span class="md-c md-paso md-${est}${ps.f ? " con-evid" : ""}" style="--c:${color(cel.camp)}">${ps.f ? `<img src="${esc(ps.f)}" alt="">` : ""}<span class="t">${esc(ps.t)}</span>${ps.e === "H" ? "<i>✓</i>" : ""}</span>`;
  };
  const vista = nivel === 0
    ? `<button class="md-nivel1${anim}" data-acc="vbNivel" data-arg="1">${b.fotoCumbre ? `<span class="md-impresion"><img src="${esc(b.fotoCumbre)}" alt=""></span>` : `<span class="md-pico">▲</span>`}
        <small>SU CUMBRE</small><b>${esc(b.cumbre)}</b><hr><em>${(b.mm || 0).toLocaleString("es")} de 8.848.000 mm</em></button>`
    : nivel === 1
      ? `<div class="md-nivel9${anim}">${[0, 1, 2, 3, 4, 5, 6, 7, 8].map(q => {
          const cel = Mandala.celda(3 + Math.floor(q / 3), 3 + q % 3);
          if (cel.tipo === "CUMBRE") return `<button class="md-tc${b.fotoCumbre ? " con-foto" : ""}" data-acc="vbNivel" data-arg="2">${b.fotoCumbre ? `<img src="${esc(b.fotoCumbre)}" alt="">` : ""}<b>${esc(b.cumbre)}</b></button>`;
          const cod = Mandala.CAMPAMENTOS_FIJOS[cel.camp], k = camp(cod), g = k ? k.p.filter(x => x.e === "H").length : 0;
          return `<button class="md-tk${k?.f ? " con-foto" : ""}" style="--c:${color(cel.camp)}" data-acc="vbCampamento" data-arg="${cel.camp}">${k?.f ? `<img src="${esc(k.f)}" alt="">` : ""}
            <small>${Mandala.RUMBOS[cel.camp]} · ${esc(Mandala.nombre(cod))}</small><b>${k ? esc(k.t) : "—"}</b><span class="md-puntos">${[0, 1, 2, 3, 4, 5, 6, 7].map(x => `<i class="${x < g ? "si" : ""}"></i>`).join("")}</span></button>`;
        }).join("")}</div>`
      : `<div class="md-grilla-caja${anim}"><div class="md-grilla">${[0, 1, 2, 3, 4, 5, 6, 7, 8].map(bl => `<button class="md-bloque${bl === bloque ? " elegido" : ""}" data-acc="vbBloque" data-arg="${bl}">${
          [0, 1, 2, 3, 4, 5, 6, 7, 8].map(q => celda(Math.floor(bl / 3) * 3 + Math.floor(q / 3), (bl % 3) * 3 + q % 3)).join("")}</button>`).join("")}</div></div>`;
  const migas = ["Cumbre", "Vision board", "9×9"].map((t, i) => (i ? `<span class="md-sep">›</span>` : "") +
    (i === nivel ? `<b aria-current="step">${t}</b>` : `<button data-acc="vbNivel" data-arg="${i}" class="${i > nivel ? "adelante" : ""}">${t}</button>`)).join("");
  const grande = nivel === 2 && b.conPasos !== false ? hoja(`<h3 style="margin-top:0">${bloque === 4 ? esc(b.cumbre) : esc(camp(Mandala.CAMPAMENTOS_FIJOS[Mandala.ANILLO.indexOf(bloque)])?.t || "")}</h3>
      <div class="md-grande">${[0, 1, 2, 3, 4, 5, 6, 7, 8].map(q => `<div>${celda(Math.floor(bloque / 3) * 3 + Math.floor(q / 3), (bloque % 3) * 3 + q % 3).replace('class="md-c', 'class="md-c md-leer')}</div>`).join("")}</div>`)
    : nivel === 2 ? hoja(`<p class="suave" style="margin:0">Esta persona compartió su cumbre y sus campamentos, sin el texto de sus pasos.</p>`) : "";
  return cab(av(ic("ruta"), "var(--burdeos)"), `Brújula de ${esc(p.autorNombre || "la comunidad")}`, b.fecha ? `Así iba el ${esc(b.fecha)}` : "Brújula de la Cima") + cuerpo(
    hoja(`<nav class="md-barra"><button class="md-atras" data-acc="vbNivel" data-arg="${nivel - 1}" ${nivel ? "" : "hidden"} aria-label="Alejar">${ic("volver")}</button>
      <span class="md-migas">${migas}</span><span class="md-num">${["1", "9", "81"][nivel]}</span></nav>
      <div class="md-zoom md-papel">${vista}</div>`) + grande, "max-width:720px");
};
Object.assign(ACC, {
  vbNivel(_, v) { const n = Math.max(0, Math.min(2, Number(v))), a = estado.vbNivel ?? 0; if (n === a || Date.now() - (estado.vbEn || 0) < 400) return;
    estado.vbAnim = n > a ? "acercar" : "alejar"; estado.vbNivel = n; estado.vbEn = Date.now(); pintarDetalle(); },
  vbCampamento(_, i) { if (Date.now() - (estado.vbEn || 0) < 400) return; estado.vbBloque = Mandala.ANILLO[Number(i)]; estado.vbAnim = "acercar"; estado.vbNivel = 2; estado.vbEn = Date.now(); pintarDetalle(); },
  vbBloque(_, v) { estado.vbBloque = Number(v); pintarDetalle(); },
});

// Ejemplo en el muro de la demostración: una Brújula compartida para explorar
(() => {
  const P = (ts, ganados) => ts.map((t, i) => ({ t, e: i < ganados ? "H" : "E" }));
  const camps = [
    ["CON", "Mi podcast une lo que sé, lo que digo y lo que doy", ["Elegir el nombre", "Grabar el piloto", "Invitar a 3 mentores", "Publicar 4 episodios", "Medir quién escucha", "Escuchar sin juzgar", "Abrir temporada 2", "Enseñar a otros a grabar"], 3],
    ["TRA", "Formo a 20 jóvenes como guías de montaña", ["Hablar con el colegio", "Diseñar el taller", "Primera salida", "Diez salidas seguras", "Graduación", "Escribir lo aprendido", "Dejar el taller a otro guía", "Becas para el siguiente grupo"], 2],
    ["VAL", "Vivo de enseñar lo que amo", ["Precio justo", "Primer cliente", "Diez clientes", "Ahorro de 3 meses", "Renunciar con plan", "Revisar el año", "Bajar el ritmo en diciembre", "Fondo para becas"], 1],
    ["EVO", "Cada mes soy mejor que el anterior", ["Evaluar mis ejes", "Un libro al mes", "Curso de primeros auxilios", "Inglés técnico", "Mentoría mensual", "Diario de aprendizajes", "Descanso real", "Enseñar lo aprendido"], 4],
    ["VOZ", "Comparto mi mensaje con mi voz auténtica", ["Escribir mi historia", "Primera charla", "Video corto semanal", "Charla en un colegio", "Entrevista en radio", "Escuchar la crítica", "Libro de bolsillo", "Taller de oratoria"], 2],
    ["CAM", "No subo solo: mi cordada me sostiene", ["Elegir mentor", "Formar mi cordada", "Reunión semanal", "Pedir ayuda a tiempo", "Celebrar con ellos", "Agradecer", "Ser mentor de alguien", "Cuidar mis círculos"], 5],
    ["VOL", "Corro mi primera media maratón", ["Correr 5 km", "Plan de 12 semanas", "Madrugar 4 días", "Correr 10 km", "Carrera de prueba", "Recuperarme bien", "Media maratón", "Acompañar a un novato"], 4],
    ["MAE", "Me certifico como guía de alta montaña", ["Curso básico", "Rescate en roca", "Tres nevados", "Práctica con un guía", "Examen", "Revisar errores", "Licencia", "Formar a otros"], 3],
  ];
  const b = { v: 1, cumbre: "Abrir una escuela de montaña en mi pueblo", fotoCumbre: "", fecha: iso(new Date()), conPasos: true,
    camps: camps.map(([c, t, ps, g]) => ({ c, t, f: "", p: P(ps, g) })) };
  b.ganados = camps.reduce((s, x) => s + x[3], 0); b.escritos = 64; b.mm = b.ganados * MetodoCima.MM_POR_PASO;
  DEMO_POSTS.unshift({ id: "demo-brujula", autorId: "demo-brujula", autorNombre: "Mateo G.", autorUsuario: "", tipo: "VISION", eje: null,
    texto: "Mi Brújula de la Cima a mitad de año.", metaTitulo: "Mi Brújula de la Cima 9×9", impulsos: 64, comentarios: 5, yoImpulse: false,
    creadoEn: Date.now() - 2 * 3600000, foto: "", propio: false, demo: true, brujula: b });
})();
