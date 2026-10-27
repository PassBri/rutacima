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

/** Avance de la cumbre (i = -1) o de un campamento: promedio de sus 8 pasos, contando las jornadas de cada uno. */
function mdPct(d, i) {
  const pasos = k => d.camps[k] ? [0, 1, 2, 3, 4, 5, 6, 7].reduce((s, p) => s + d.avance(k, p), 0) / 8 * 100 : 0;
  // Lo que la persona marcó a mano (estado Lograda o su %) manda si es mayor que lo que dicen los pasos
  const conManual = (c, base) => !c ? base : respuesta(`vision:estado:${c.id}`) === "LOGRADA" ? 100 : Math.max(base, Number(respuesta(`vision:pct:${c.id}`)) || 0);
  // Las metas de este año que sostienen el campamento (mismo eje) también lo hacen subir: Mi ruta, Metas y el vision board cuentan lo mismo
  const conMetas = (k, base) => { const v = metasPct(k); return v === null ? base : Math.max(base, v); };
  if (i >= 0) return Math.round(conManual(d.camps[i], conMetas(i, pasos(i))));
  const prom = [0, 1, 2, 3, 4, 5, 6, 7].reduce((s, k) => s + conManual(d.camps[k], conMetas(k, pasos(k))), 0) / 8;
  return Math.round(conManual(d.cumbre, prom));
}
/** Promedio de avance de las metas de este año de un campamento (null si no tiene). */
function metasPct(i) {
  const h = hoyFecha(), a = h.getFullYear(), mes = h.getMonth() + 1, c = cascada();
  const v = [...metasDeCampamento(i).filter(m => m.anio === a).map(m => c.nodoAnio(m)),
    ...mensualesDeCampamento(i).filter(m => m.anio === a && m.mes === mes && m.metaAnualId == null).map(avanceMetaMes)];
  return v.length ? v.reduce((s, x) => s + x, 0) / v.length * 100 : null;
}
/** Índice (0..7) del campamento que sostiene una meta de cualquier nivel (sin eje en la cadena: Confluencia). */
const campDe = (tipo, o) => Mandala.CAMPAMENTOS_FIJOS.indexOf(ejeEfectivo(tipo, o) || "CON");
const campDeMeta = m => campDe("meta_anio", m);
window.campDe = campDe;
/** Franja que dice qué campamento sostiene una meta o propósito, y lo abre en Mi vida. */
function franjaCampamento(tipo, o) {
  if (!window.Mandala || !casillasVision().length) return "";
  const md = Mandala.datos(), i = campDe(tipo, o); if (i < 0) return "";
  const camp = md.camps[i], heredado = !o.eje && ejeEfectivo(tipo, o) ? " (heredado de su meta superior)" : "";
  return `<button class="meta-camp" style="--c:${md.color(i)}" data-acc="irCampamento" data-arg="${i}"><i aria-hidden="true"></i>
    <span>Sostiene <b>${esc(Mandala.nombre(Mandala.CAMPAMENTOS_FIJOS[i]))}</b>${camp ? ` · ${esc(mdNombre(camp))}` : ""} en tu Brújula${heredado}</span>${ic("abajo")}</button>`;
}
window.franjaCampamento = franjaCampamento;
window.campDeMeta = campDeMeta;
/** Indicador de avance de Material 3: anillo pequeño con el porcentaje, en la esquina de la tarjeta. */
const mdAnillo = (pct, clase = "") => `<span class="pct-m3 ${clase}${pct >= 100 ? " lleno" : ""}" style="--p:${pct}" role="img" aria-label="Avance ${pct} %" title="Avance ${pct} %"><i aria-hidden="true"></i>${pct}%</span>`;
/** En el 9×9 completo las casillas son pequeñas: el avance va como barra fina en el borde inferior. */
const mdBarraMini = pct => `<span class="pct-linea" title="Avance ${pct} %" aria-label="Avance ${pct} %"><i style="width:${pct}%"></i></span>`;
window.mdPct = mdPct; window.mdAnillo = mdAnillo;

/** El nombre del bloque abierto, editable en su lugar: la cumbre (casilla central o frase del perfil) o el campamento. */
function mdTituloEditable(d, camp, cs, titulo) {
  const estilo = 'class="md-titulo-edit" aria-label="Nombre de este bloque"';
  if (camp >= 0) return cs ? `<input type="text" ${estilo} ${bind("vision", String(cs.id), "afirmacion")} value="${esc(mdNombre(cs))}" placeholder="Nombre del campamento">` : `<h3 style="margin:0">${esc(titulo)}</h3>`;
  return d.cumbre ? `<input type="text" ${estilo} ${bind("vision", String(d.cumbre.id), "afirmacion")} value="${esc(d.titulo)}" placeholder="Mi cumbre en una frase">`
    : `<input type="text" ${estilo} ${bind("perfil", 1, "cumbreFrase")} value="${esc(d.titulo)}" placeholder="Mi cumbre en una frase">`;
}

/** Barra lineal de Material 3 con el avance del bloque abierto y cómo se calcula. */
function mdBarraAvance(d, camp) {
  const pct = mdPct(d, camp);
  return `<div class="avance-m3"><div class="avance-m3-fila"><span>Avance ${camp < 0 ? "de tu cumbre" : "del campamento"}</span><b>${pct}%</b></div>
    <div class="avance-m3-barra" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${pct}"><i style="width:${pct}%"></i></div>
    <details class="avance-m3-ayuda"><summary>¿Cómo se calcula?</summary>
      <p>${camp < 0 ? "Es el promedio de tus 8 campamentos." : "Es el promedio de sus 8 pasos."} Cada paso suma según sus jornadas: si pide 3 y llevas 1, va en 33 %; al ganarlo, cuenta completo. Lo mueves con <b>Avancé hoy</b> en cada paso de abajo.</p></details></div>`;
}

/** Una celda: la cumbre, un campamento (con su foto) o un paso. */
function mdCelda(d, cel, grande) {
  if (cel.tipo === "CUMBRE") {
    const foto = mdFoto(d.cumbre);
    return `<span class="md-c md-cumbre${foto ? " con-foto" : ""}">${foto ? `<img src="${esc(foto)}" alt="">` : ""}<b>${esc(d.titulo)}</b>${mdBarraMini(mdPct(d, -1))}</span>`;
  }
  const camp = d.camps[cel.camp], color = d.color(cel.camp);
  if (cel.tipo === "CAMPAMENTO") {
    const foto = mdFoto(camp);
    return `<span class="md-c md-camp${camp ? "" : " libre"}" style="--c:${color}">${foto ? `<img src="${esc(foto)}" alt="">` : ""}<b>${camp ? esc(mdNombre(camp)) : "+ " + Mandala.nombre(Mandala.CAMPAMENTOS_FIJOS[cel.camp])}</b>${camp ? mdBarraMini(mdPct(d, cel.camp)) : ""}</span>`;
  }
  const est = Mandala.estado(camp?.id, cel.paso), texto = camp ? respuesta(Mandala.clave(camp.id, cel.paso)) : "";
  const evid = mdFotoPaso(camp, cel.paso), fase = `<small class="md-fase">${cel.paso + 1} · ${Mandala.FASES[cel.paso]}</small>`;
  if (grande && camp) return `<span class="md-c md-paso md-${est.toLowerCase()}${evid ? " con-evid" : ""}" style="--c:${color}">
      ${evid ? `<img src="${esc(evid)}" alt="Evidencia">` : ""}${fase}
      <textarea data-resp="mandala|${Mandala.clave(camp.id, cel.paso)}" placeholder="Paso ${cel.paso + 1}" aria-label="Paso ${cel.paso + 1} de ${esc(camp.titulo)}">${esc(texto)}</textarea>
      ${mdJornadas(d, cel, camp, texto)}${d.niebla(cel.camp, cel.paso) ? `<span class="md-niebla grande">☁</span>` : ""}${d.enlaces(cel.camp, cel.paso).length ? `<span class="md-enlace">⇄ ${d.enlaces(cel.camp, cel.paso).map(x => Mandala.nombre(x)).join(", ")}</span>` : ""}</span>`;
  const extra = (d.niebla(cel.camp, cel.paso) ? `<span class="md-niebla" title="Niebla: más de ${Travesia.NIEBLA_DIAS} días sin avanzar">☁</span>` : "") +
    (d.enlaces(cel.camp, cel.paso).length ? `<span class="md-enlace" title="Paso de confluencia">⇄</span>` : "");
  const t81 = camp ? `<textarea class="md-t81" data-resp="mandala|${Mandala.clave(camp.id, cel.paso)}" placeholder="${cel.paso + 1}" aria-label="Paso ${cel.paso + 1} de ${esc(mdNombre(camp))}">${esc(texto)}</textarea>` : "";
  return `<span class="md-c md-paso md-${est.toLowerCase()}${evid ? " con-evid" : ""}" style="--c:${color}"${texto ? ` title="${esc(texto)}"` : ""}>${evid ? `<img src="${esc(evid)}" alt="">` : ""}<span class="t">${esc(texto)}</span>${t81}${est === "HECHO" ? "<i>✓</i>" : ""}${extra}</span>`;
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
    ${evid ? `<img class="md-evid" src="${esc(evid)}" alt="${paso.cumplido ? "Foto de evidencia" : "Foto del paso"}">` : ""}
    <label class="btn">${ic("foto")} ${paso.cumplido ? (evid ? "Cambiar foto de evidencia" : "Agregar foto de evidencia") : (evid ? "Cambiar la foto de este paso" : "Agregar una foto a este paso")}<input type="file" accept="image/*" hidden data-acc-change="mdEvidencia" data-arg="${c.id}|${n}"></label>
    ${paso.cumplido ? `
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

function mandalaHtml(tableroHtml = "", rielArriba = "", rielAbajo = "", planear = "") {
  const d = Mandala.datos(), b = estado.mandalaBloque ?? 4;
  const compartir = estado.mdCompartir
    ? `<div class="md-compartir"><p>Se publica tu Brújula para que la comunidad la explore (1 · 9 · 81), con una imagen para descargar. Elige qué sale y quién la ve:</p>
        <label class="md-op"><input type="checkbox" id="mdConPasos" checked> Mis 64 pasos</label>
        <label class="md-op"><input type="checkbox" id="mdConEvid"> Mis fotos de evidencia</label>
        <div class="botones"><button class="btn" data-acc="compartirMandala" data-arg="SEGUIDORES">Mis seguidores</button>
        <button class="btn lleno" data-acc="compartirMandala" data-arg="PUBLICA">Todos</button><button class="btn mini" data-acc="compartirMandala" data-arg="">Cancelar</button></div></div>`
    : `<button class="btn lleno md-btn-compartir" data-acc="compartirMandala" data-arg="?" ${mdPublicando ? "disabled" : ""}>${ic("publicar")} ${mdPublicando ? "Preparando la imagen…" : "Compartir mi 9×9 en la comunidad"}</button>`;
  const intro = `<h3 style="margin-top:0">Brújula de la Cima 9×9</h3>
    <p>Tus campamentos son el centro: tu cumbre en medio y 8 campamentos fijos alrededor, como una rosa de los vientos (tus 6 ejes, Confluencia y Campamento Base). Cada campamento es un viaje de 8 pasos por las fases del Viaje Transformativo. Al ganar un paso, su foto de evidencia reemplaza a la visión: tu 9×9 pasa de soñado a vivido.</p>`;
  const pie = `<b class="md-progreso">${d.escritos} de 64 pasos escritos · ${d.hechos} ${d.hechos === 1 ? "ganado" : "ganados"}</b>
    <div class="barra" style="margin:8px 0 6px"><i style="width:${d.hechos / 64 * 100}%"></i></div>
    <p class="suave" style="margin:0">${d.evidencias} ${d.evidencias === 1 ? "foto" : "fotos"} de evidencia: lo vivido que ya reemplazó a lo soñado</p>
    ${d.confluencias ? `<p class="md-conf">${d.confluencias} ${d.confluencias === 1 ? "paso" : "pasos"} de confluencia: una acción que movió varios ejes</p>` : ""}
    ${d.enNiebla ? `<p class="md-nieblas">${d.enNiebla} ${d.enNiebla === 1 ? "paso" : "pasos"} en la niebla (más de ${Travesia.NIEBLA_DIAS} días sin avanzar)</p>` : ""}
    <div style="height:12px"></div>${compartir}`;
  /* Cada bloque es un <div>: en modo Edición se escribe en sus casillas; en los otros modos, tocarlo lo abre (js/25-tarjetas.js). */
  const grilla = [0, 1, 2, 3, 4, 5, 6, 7, 8].map(bl => `<div class="md-bloque${bl === b ? " elegido" : ""}" data-bloque="${bl}" role="button" tabindex="0" aria-label="Abrir bloque ${bl + 1}">${
    [0, 1, 2, 3, 4, 5, 6, 7, 8].map(k => mdCelda(d, Mandala.celda(Math.floor(bl / 3) * 3 + Math.floor(k / 3), (bl % 3) * 3 + k % 3), false)).join("")}</div>`).join("");
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
  const svg = "";   // las confluencias se marcan con ⇄ en cada paso; las líneas cruzaban toda la grilla
  // De lo más grande a lo más pequeño, en una sola página: Cumbre (1) → Vision board (9) → Brújula 9×9 (81)
  const fotoC = mdFoto(d.cumbre), pctC = mdPct(d, -1);
  const nav = `<nav class="md-secciones" aria-label="Secciones de tu Brújula">${rielArriba ? `<span class="md-riel-top">${rielArriba}</span><span class="md-riel-sep" aria-hidden="true"></span>` : ""}
    ${[[1, "Cumbre"], [9, "Campamentos"], [81, "Brújula 9×9"]].map(([n, t]) => `<button role="tab" data-acc="mdIr" data-arg="md-s${n}" data-capa-btn="${n}" aria-selected="${mdCapaActual() === n}" aria-label="${t}"><b>${n}</b><span>${t}</span></button>`).join("")}
    <span class="md-nav-sep"></span>
    <span class="modo-tarj" role="radiogroup" aria-label="Cómo ver tus tarjetas">${[["imagen", "foto", "Imagen"], ["texto", "frases", "Texto"], ["edicion", "editar", "Edición"]].map(([v, i, n]) =>
      `<button role="radio" aria-checked="${(estado.modoTarjetas || "imagen") === v}" data-acc="modoTarjetas" data-arg="${v}" title="Modo ${n.toLowerCase()}">${ic(i)}<span>${n}</span></button>`).join("")}</span>
    <span class="md-riel-sep" aria-hidden="true"></span>
    <button class="md-riel-btn md-girar-todas" data-acc="mdGirarCapa" aria-label="Girar las tarjetas" title="Girar las tarjetas">${ic("girar")}</button>
    <details class="md-riel-info msc-info"><summary class="md-riel-btn" aria-label="Qué es" title="Qué es">${ic("info")}</summary><p id="mdRielInfo">${MD_CAPA_INFO[mdCapaActual()]}</p></details>
    ${rielAbajo ? `<span class="md-riel-top">${rielAbajo}</span>` : ""}
    <span class="md-riel-top"><span class="md-riel-sep" aria-hidden="true"></span>${[["modelo", "modelo", "Modelo · Brújula 1·9·81"], ["ritmo", "ritmo", "Tu ritmo · Método Cima 9×52"], ["cierre", "lecciones", "Cierre del año"], ...(planear ? [["planear", "campamento", "Planear por campamento"]] : [])].map(([k, i, t]) =>
      `<button class="md-riel-btn md-riel-cajon" data-acc="mdCajon" data-arg="${k}" aria-pressed="${estado.mdCajon === k}" aria-label="${t}" title="${t}">${ic(i)}</button>`).join("")}</span>
    ${typeof tamBotones === "function" ? `<span class="md-riel-top md-riel-tam">${tamBotones()}</span>` : ""}
    <button class="md-ver-grande" data-acc="mdVisorCapa" aria-label="Pantalla completa" title="Pantalla completa: de la cumbre al 9×9 y de vuelta, con + y −">${ic("pantallaCompleta")}</button></nav>`;
  /* Cabecera compacta: el título como etiqueta arriba a la izquierda y, a la derecha, una fila de botones flotantes
     (girar, pantalla completa e información). La explicación vive en el botón de información, para dar todo el espacio a la brújula. */
  const cabSec = (id, n, t, sub, extra = "") => `<div class="md-sec-cab" id="${id}"><div class="msc-tit"><span class="md-sec-n">${n}</span><h3>${t}</h3></div>
    <div class="msc-fab">${extra}<details class="msc-info"><summary class="md-visor-ic" aria-label="Qué es ${esc(t)}" title="Qué es">${ic("info")}</summary><p>${sub}</p></details><button class="md-visor-ic" data-acc="mdVisor" data-arg="${{ 1: 0, 9: 1, 81: 2 }[n]}" aria-label="Ver ${t} en grande" title="Ver en grande (+ y − para moverte entre la cumbre, los campamentos y el 9×9)">${ic("pantallaCompleta")}</button></div></div>`;
  const s1 = hoja((estado.brujulaAnio ? cabSec("md-s1", "1", `Mi cumbre de ${estado.brujulaAnio}`, "Lo más grande de este año. Todo lo de abajo la sostiene.") : cabSec("md-s1", "1", "Mi cumbre personal", "Lo más grande: hacia dónde va tu vida. Todo lo de abajo la sostiene.")) +
    rosaBrujula(d.cumbre ? casillaVisionHtml(d.cumbre, d, false, { clase: "cumbre-tarjeta", fija: true }) : `<div class="md-nivel1 md-papel" style="pointer-events:none">${fotoC ? `<span class="md-impresion"><img src="${esc(fotoC)}" alt=""></span>` : `<span class="md-pico" aria-hidden="true">▲</span>`}<small>MI CUMBRE</small>${mdTituloEditable(d, -1, null, d.titulo)}</div>`, "rb-1") +
    `<div class="md-pie-capa">${mdBarraAvance(d, -1)}<p class="md-mm">Tu montaña: <b>${d.ritmo.mm.toLocaleString("es")}</b> de 8.848.000 mm</p></div>`);
  /* El vision board ES la rosa: la cumbre al centro y los 8 campamentos alrededor, cada uno una tarjeta que se gira y se edita ahí mismo. */
  const rosa = [0, 1, 2, 3, 4, 5, 6, 7, 8].map(k => {
    const cel = Mandala.celda(3 + Math.floor(k / 3), 3 + k % 3);
    if (cel.tipo === "CUMBRE") return d.cumbre ? casillaVisionHtml(d.cumbre, d, false, { compacta: true, fija: true, clase: "centro" })
      : `<button class="md-tc" data-acc="mdIr" data-arg="md-s1"><b>${esc(d.titulo)}</b></button>`;
    const c = d.camps[cel.camp], cod = Mandala.CAMPAMENTOS_FIJOS[cel.camp];
    const pasos = `<button class="cara-pasos" data-acc="mandalaBloque" data-arg="${Mandala.ANILLO[cel.camp]}" aria-label="Abrir los 8 pasos de ${esc(Mandala.nombre(cod))} en la Brújula" title="Sus 8 pasos">${Mandala.RUMBOS[cel.camp]}</button>`;
    return c ? casillaVisionHtml(c, d, false, { compacta: true, fija: true, sobre: pasos })
      : `<button class="tarjeta-libre" style="--c:${d.color(cel.camp)}" data-acc="crearCampamento" data-arg="${cod}">${ic("mas")}<small>${Mandala.RUMBOS[cel.camp]}</small><b>${esc(Mandala.nombre(cod))}</b></button>`;
  }).join("");
  const s9 = hoja(cabSec("md-s9", "9", "Campamentos", "Tu cumbre al centro y 8 campamentos alrededor. Imagen muestra tus fotos, Texto tus frases y Edición te deja escribir en cada tarjeta. La letra del rumbo abre sus 8 pasos.",
    `<button class="md-visor-ic md-girar-todas" data-acc="girarTodasVision" aria-label="Girar todas las tarjetas" title="Girar todas las tarjetas">${ic("girar")}</button>`) +
    rosaBrujula(`<div class="tablero-9">${rosa}</div>`, "rb-9") + (tableroHtml || "") +
    `<div class="md-pie-capa">${mdBarraAvance(d, -1)}<p class="md-mm">${d.camps.filter(Boolean).length} de 8 campamentos · tu montaña: <b>${d.ritmo.mm.toLocaleString("es")}</b> de 8.848.000 mm</p></div>`);
  const camposBloque = camp >= 0 ? (cs ? `<div class="md-bloque-cab" style="--c:${d.color(camp)}">${mdAnillo(mdPct(d, camp), "")}<div><small>${esc(Mandala.nombre(cod))} · rumbo ${Mandala.RUMBOS[camp]}</small><h4>${esc(mdNombre(cs))}</h4></div>
      <button class="btn mini" data-acc="editarTarjeta" data-arg="${cs.id}">${ic("foto")} Foto, frase y estado</button></div>` : `<h4>Campamento libre · ${esc(Mandala.nombre(cod))}</h4>`)
    : `<div class="md-bloque-cab">${mdAnillo(pctC, "")}<div><small>Centro · tu cumbre y sus 8 campamentos</small><h4>${esc(d.titulo)}</h4></div></div>`;
  const editorBloque = `<div id="md-bloque"></div><div class="md-bloque-barra"><button class="btn mini" data-acc="md81" data-arg="">${ic("volver")} Los 81</button>
      <span class="md-minimapa" role="group" aria-label="Cambiar de bloque">${[0, 1, 2, 3, 4, 5, 6, 7, 8].map(bl => `<button class="md-mini${bl === b ? " elegido" : ""}" data-acc="mandalaBloque" data-arg="${bl}" aria-label="Bloque ${bl + 1}" style="--c:${bl === 4 ? "var(--oro)" : d.color(Mandala.ANILLO.indexOf(bl))}"></button>`).join("")}</span></div>
    ${camposBloque}${camp >= 0 && !cs ? "" : mdBarraAvance(d, camp)}<div class="md-grande">${grande}</div>${mdPanelPaso(d)}
    <div class="botones" style="margin-top:12px">${cs ? `<button class="btn" data-acc="mandalaSugerir" data-arg="${cs.id}">${ic("ia")} Sugerir pasos</button>` : ""}
    ${camp >= 0 && !cs ? `<button class="btn" data-acc="crearCampamento" data-arg="${cod}">${ic("mas")} Agregar campamento</button>` : ""}
    ${camp >= 0 ? `<button class="btn mini" data-acc="mandalaBloque" data-arg="4">Ver el centro</button>` : ""}</div>`;
  /* Al tocar un bloque, se abre ahí mismo en lugar de los 81 (sin bajar); "Los 81" vuelve a la grilla. */
  const s81 = hoja(cabSec("md-s81", "81", "Brújula 9×9", "Lo más pequeño: los 64 pasos. En Edición escribes directo en cada casilla; en Imagen o Texto, toca un bloque para abrirlo con sus jornadas.",
    !estado.md81 ? `<button class="md-visor-ic md-girar-todas" data-acc="md81Girar" aria-pressed="${!!estado.md81Girada}" aria-label="${estado.md81Girada ? "Girar al texto" : "Girar a las imágenes"}" title="Girar la tarjeta">${ic("girar")}</button>` : "") +
    (estado.md81 ? editorBloque : (
      /* La tarjeta del 9×9 gira completa en cualquier modo: adelante el texto (o la edición), detrás las imágenes */
      rosaBrujula(`<div class="md-grilla-caja md-flip${estado.md81Girada ? " girada" : ""}"><div class="md-flip-in">
          <div class="md-cara cara-texto" aria-hidden="${!!estado.md81Girada}"><div class="md-grilla">${grilla}</div></div>
          <div class="md-cara cara-imagen" aria-hidden="${!estado.md81Girada}"><div class="md-grilla">${grilla}</div></div></div>
</div>`, "rb-81", "md-81-hijo")) + `
      <div class="md-pie-capa"><p class="md-leyenda"><span><i style="--n:7%"></i>Por escribir</span><span><i style="--n:30%"></i>Escrito</span><span><i style="--n:100%"></i>Ganado ✓</span></p>` + pie + `</div>`));
  // Visor emergente a pantalla completa: Cumbre (1) ⇄ Vision board (9) ⇄ Brújula 9×9 (81), en los dos sentidos
  let visor = "";
  if (estado.mdVisor) {
    const nivel = estado.mdNivel ?? 0, anim = estado.mdAnim ? ` md-anim-${estado.mdAnim}` : "";
    estado.mdAnim = null; setTimeout(() => { estado.mdVolteo = null; }, 0);
    const puntos = i => { const g = [0, 1, 2, 3, 4, 5, 6, 7].filter(q => Mandala.estado(d.camps[i]?.id, q) === "HECHO").length;
      return `<span class="md-puntos">${[0, 1, 2, 3, 4, 5, 6, 7].map(q => `<i class="${q < g ? "si" : ""}"></i>`).join("")}</span>`; };
    // Girar: cada tarjeta tiene su botón y la barra gira todas; el reverso muestra lo que hay detrás de la foto
    const giradas = estado.mdGiradas ||= new Set(), girada = k => giradas.has(k) !== !!estado.mdTodas;
    const volteo = k => estado.mdVolteo === k || estado.mdVolteo === "todas" ? " md-volteo" : "";
    const botonGiro = (k, nombre) => `<button class="md-girar" data-acc="mdGirar" data-arg="${k}" aria-pressed="${girada(k)}" aria-label="Girar ${esc(nombre)}" title="Girar">${ic("girar")}</button>`;
    const pasosDe = i => { const c0 = d.camps[i]; return c0 ? [0, 1, 2, 3, 4, 5, 6, 7].map(q => { const t = respuesta(Mandala.clave(c0.id, q)), h = Mandala.estado(c0.id, q) === "HECHO";
      return `<li class="${h ? "si" : ""}">${h ? "✓ " : ""}${t ? esc(t) : `<span class="suave">Paso ${q + 1}</span>`}</li>`; }).join("") : ""; };
    const vista = nivel === 0
      ? `<div class="md-giro md-giro-1">${girada("c") ? `<button class="md-nivel1 md-reverso${anim}${volteo("c")}" data-acc="mdNivel" data-arg="1" aria-label="Acercar: tus campamentos">
          <small>TUS 8 CAMPAMENTOS</small><b>${esc(d.titulo)}</b><hr><ol class="md-rev-lista">${[0, 1, 2, 3, 4, 5, 6, 7].map(i => d.camps[i]
            ? `<li><span>${esc(mdNombre(d.camps[i]))}</span><i>${Math.round(mdPct(d, i))} %</i></li>` : `<li class="suave"><span>${esc(Mandala.nombre(Mandala.CAMPAMENTOS_FIJOS[i]))}</span><i>—</i></li>`).join("")}</ol></button>`
        : `<button class="md-nivel1${anim}${volteo("c")}" data-acc="mdNivel" data-arg="1" aria-label="Acercar: tus campamentos">
          ${fotoC ? `<span class="md-impresion"><img src="${esc(fotoC)}" alt=""></span>` : `<span class="md-pico" aria-hidden="true">▲</span>`}
          <small>MI CUMBRE</small><b>${esc(d.titulo)}</b><hr><em>Tu montaña: ${d.ritmo.mm.toLocaleString("es")} de 8.848.000 mm</em>${mdAnillo(pctC, "centro")}</button>`}${botonGiro("c", "tu cumbre")}</div>`
      : nivel === 1
        ? `<div class="md-nivel9${anim}">${[0, 1, 2, 3, 4, 5, 6, 7, 8].map(k => {
            const cel = Mandala.celda(3 + Math.floor(k / 3), 3 + k % 3);
            if (cel.tipo === "CUMBRE") { const g = girada("t4");
              return `<div class="md-giro"><button class="md-tc${fotoC && !g ? " con-foto" : ""}${g ? " md-reverso" : ""}${volteo("t4")}" data-acc="mdNivel" data-arg="2" aria-label="Acercar: el 9×9 completo">
              ${fotoC && !g ? `<img src="${esc(fotoC)}" alt="">` : ""}${g ? `<small>MI CUMBRE</small>` : ""}<b>${esc(d.titulo)}</b>${g ? `<em class="md-rev-dato">${Math.round(pctC)} % · ${d.ritmo.mm.toLocaleString("es")} mm</em>` : mdAnillo(pctC, "esquina")}</button>${botonGiro("t4", "la cumbre")}</div>`; }
            const c = d.camps[cel.camp], cod = Mandala.CAMPAMENTOS_FIJOS[cel.camp], foto = mdFoto(c), k0 = "t" + cel.camp, g = c && girada(k0);
            return `<div class="md-giro"><button class="md-tk${foto && !g ? " con-foto" : ""}${g ? " md-reverso" : ""}${volteo(k0)}" style="--c:${d.color(cel.camp)}" data-acc="${c ? "mdCampamento" : "crearCampamento"}" data-arg="${c ? cel.camp : cod}" aria-label="Acercar: ${esc(Mandala.nombre(cod))}">
              ${g ? `<small>${esc(mdNombre(c))}</small><ol class="md-rev-pasos">${pasosDe(cel.camp)}</ol>`
                : `${foto ? `<img src="${esc(foto)}" alt="">` : ""}${c ? mdAnillo(mdPct(d, cel.camp), "esquina") : ""}<small>${Mandala.RUMBOS[cel.camp]} · ${esc(Mandala.nombre(cod))}</small><b>${c ? esc(mdNombre(c)) : "+ Agregar"}</b>${puntos(cel.camp)}`}</button>${c ? botonGiro(k0, mdNombre(c)) : ""}</div>`;
          }).join("")}</div>`
        : `<div class="md-grilla-caja${anim}${estado.mdTodas ? " md-sin-fotos" : ""}${volteo("todas")}"><div class="md-grilla">${grilla}</div></div>`;
    const ayuda = ["Toca tu cumbre para acercarte a tus campamentos.",
      "Toca la cumbre para ver el 9×9, o un campamento para ir directo a su bloque. Para alejarte: la flecha, las migas, el signo − o Esc.",
      "Toca un bloque para editarlo; el centro te aleja a los campamentos. Para alejarte: la flecha, las migas, el signo − o Esc."][nivel];
    const migas = ["Cumbre", "Campamentos", "9×9"].map((t, i) => (i ? `<span class="md-sep" aria-hidden="true">›</span>` : "") +
      (i === nivel ? `<b aria-current="step">${t}</b>` : `<button data-acc="mdNivel" data-arg="${i}">${t}</button>`)).join("");
    visor = `<div class="md-visor" role="dialog" aria-modal="true" aria-label="Tu Brújula en grande">
      <div class="md-visor-barra">
        <button class="md-visor-ic" data-acc="mdNivel" data-arg="${nivel - 1}" ${nivel ? "" : "disabled"} aria-label="Alejar">${ic("volver")}</button>
        <span class="md-migas">${migas}</span><span class="md-num">${["1", "9", "81"][nivel]}</span><span style="flex:1"></span>
        <button class="md-visor-ic" data-acc="mdNivel" data-arg="${nivel - 1}" ${nivel ? "" : "disabled"} aria-label="Alejar (−)" title="Alejar (−)">−</button>
        <button class="md-visor-ic" data-acc="mdNivel" data-arg="${nivel + 1}" ${nivel < 2 ? "" : "disabled"} aria-label="Acercar (+)" title="Acercar (+)">+</button>
        <button class="md-visor-ic" data-acc="mdGirar" data-arg="todas" aria-pressed="${!!estado.mdTodas}" aria-label="${estado.mdTodas ? "Volver a las fotos" : "Girar todas las tarjetas"}" title="${estado.mdTodas ? "Volver a las fotos" : "Girar todas"}">${ic("girar")}</button>
        <button class="btn mini" data-acc="mdEditarAbajo">${ic("editar")} Editar</button>
        <button class="md-visor-ic" data-acc="mdPantalla" aria-label="Pantalla completa" title="Pantalla completa (F)">${ic("pantallaCompleta")}</button>
        <button class="md-visor-ic" data-acc="mdVisor" data-arg="" aria-label="Cerrar (Esc)" title="Cerrar (Esc)">${ic("cerrar")}</button></div>
      <div class="md-visor-lienzo md-papel">${vista}</div>
      <p class="md-visor-ayuda">${ayuda}</p></div>`;
  }
  montarVisor(visor);
  /* 1 → 9 → 81 en una sola página: una capa a la vez en el mismo lugar, como acercar la brújula (sin hacer scroll) */
  const nv = mdCapaActual();
  const capa = (n, html) => `<section class="md-capa${n === nv ? " activa" : ""}" data-capa="${n}"${n === nv ? "" : " hidden"}>${html}</section>`;
  /* Todos los botones van en un riel a la derecha (en el computador): más espacio para la cumbre, el vision board y el 9×9 */
  /* Ritmo, cierre del año y planear por campamento viven en cajones que se abren desde el riel de la derecha:
     la vista queda limpia para la cumbre, el vision board y el 9×9 (en el teléfono se ven debajo, como antes) */
  const cajon = (k, html) => `<div class="md-cajon${estado.mdCajon === k ? " abierto" : ""}" data-cajon="${k}">${html}</div>`;
  const cajones = cajon("modelo", hoja(`<div class="md-cajon-cab md-cab-modelo"><span class="sec-ic">${ic("modelo")}</span><div><b>Modelo</b><span class="suave">Brújula 1·9·81</span></div><button class="md-cajon-x" data-acc="mdCajon" data-arg="modelo" aria-label="Cerrar">${ic("cerrar")}</button></div>${modeloHtml()}`, "md-extra md-extra-modelo")) +
    cajon("ritmo", hoja(`<div class="md-cajon-cab"><span class="sec-ic">${ic("ritmo")}</span><div><b>Tu ritmo · Método Cima 9×52</b><span class="suave">${d.ritmo.mm.toLocaleString("es")} de 8.848.000 mm</span></div><button class="md-cajon-x" data-acc="mdCajon" data-arg="ritmo" aria-label="Cerrar">${ic("cerrar")}</button></div>${ritmoHtml(d.ritmo)}`, "md-extra")) +
    cajon("cierre", hoja(`<div class="md-cajon-cab"><span class="sec-ic">${ic("lecciones")}</span><div><b>Cierre del año</b><span class="suave">Desde la Cima</span></div><button class="md-cajon-x" data-acc="mdCajon" data-arg="cierre" aria-label="Cerrar">${ic("cerrar")}</button></div>${cierreHtml(d)}`, "md-extra md-extra-cierre")) +
    (planear ? cajon("planear", `<div class="md-cajon-planear"><button class="md-cajon-x flota" data-acc="mdCajon" data-arg="planear" aria-label="Cerrar">${ic("cerrar")}</button>${planear}</div>`) : "");
  return `<div class="md-escena">${nav}<div class="md-pila">${capa(1, s1)}${capa(9, s9)}${capa(81, s81)}</div><div class="md-cajones">${cajones}</div></div>`;
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
    } catch (e) { console.error(e); toast("No se pudo compartir." + (detalleError(e) || " Inténtalo de nuevo.")); }
    mdPublicando = false; pintarDetalle();
  },
  mandalaBloque(el, v) {
    if (estado.mdVisor && el?.closest?.(".md-visor")) {
      if (Number(v) === 4 && el.classList.contains("md-bloque")) { ACC.mdNivel(null, "1"); return; }
      estado.mdVisor = false; if (document.fullscreenElement) document.exitFullscreen?.();
      estado.mandalaBloque = Number(v); estado.mdPaso = null; estado.md81 = true; mdCapaGuardar(81); pintarDetalle();
      document.getElementById("md-bloque")?.scrollIntoView({ behavior: "auto", block: "start" });   // quieta debajo del visor que se desvanece
      return;
    }
    const yaAbierto = estado.md81; estado.mandalaBloque = Number(v); estado.mdPaso = null; estado.md81 = true; mdCapaGuardar(81); pintarDetalle();
    // Desde la grilla de 81 se queda en su sitio; desde el vision board sube la Brújula hasta la vista
    if (el && !el.closest(".md-grilla, .md-bloque-barra, .md-grande") && !yaAbierto || el?.closest?.(".tablero-9"))
      requestAnimationFrame(() => document.getElementById("md-s81")?.scrollIntoView({ behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth", block: "start" }));
  },
  /** Navegación por secciones: Cumbre (1) · Vision board (9) · Brújula 9×9 (81). */
  /** Desde el bloque abierto, sube a la tarjeta de esa casilla en el vision board. */
  mdIrCasilla(_, id) { if (document.querySelector(".md-pila")) mdCapa(9); const t = document.querySelector(`.casilla-v[data-casilla="${id}"]:not(.en-bloque)`); t?.scrollIntoView({ behavior: "smooth", block: "center" }); t?.classList.add("resalta"); setTimeout(() => t?.classList.remove("resalta"), 1600); },
  mdIr(_, id) { const n = Number(String(id).replace("md-s", "")); if ([1, 9, 81].includes(n) && document.querySelector(".md-pila")) return mdCapa(n);
    document.getElementById(id)?.scrollIntoView({ behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth", block: "start" }); },
  /** Cambia de nivel de zoom; ignora el segundo clic de un doble clic para no saltarse un nivel. */
  /** Gira una tarjeta del visor (c, t0…t7, t4) o todas a la vez. */
  mdGirar(_, k) {
    if (k === "todas") estado.mdTodas = !estado.mdTodas;
    else { const s = estado.mdGiradas ||= new Set(); s.has(k) ? s.delete(k) : s.add(k); }
    estado.mdVolteo = k; navigator.vibrate?.(8); pintarDetalle();
  },
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
  /** Abre (con el nivel) o cierra el visor emergente. */
  mdVisor(_, v) {
    if (v === "" || v === undefined) { estado.mdVisor = false; if (document.fullscreenElement) document.exitFullscreen?.(); pintarDetalle(); return; }
    estado.mdVisor = true; estado.mdNivel = Number(v); estado.mdAnim = "acercar"; estado.mdNivelEn = 0; pintarDetalle();
    requestAnimationFrame(() => document.querySelector(".md-visor-lienzo button, .md-visor .md-visor-ic:not([disabled])")?.focus());
  },
  mdPantalla() { const v = document.querySelector(".md-visor"); if (!v) return; document.fullscreenElement ? document.exitFullscreen?.() : (document.documentElement.requestFullscreen?.() || null); },
  /** Cierra el visor y baja al editor de lo que se estaba viendo. */
  mdEditarAbajo() {
    const n = estado.mdNivel ?? 0; estado.mdVisor = false; if (document.fullscreenElement) document.exitFullscreen?.();
    if (n < 2) { pintarDetalle(); const d = Mandala.datos(); const c = n === 0 ? d.cumbre : d.camps.find(Boolean) || d.cumbre;
      if (c) return ACC.editarTarjeta(null, c.id); document.getElementById(n === 0 ? "md-s1" : "md-s9")?.scrollIntoView({ block: "start" }); return; }
    estado.md81 = true; pintarDetalle();
    // La página ya queda en su lugar mientras el visor se desvanece: sin saltos ni desplazamientos a la vista
    document.getElementById(n === 0 ? "md-s1" : n === 1 ? "md-s9" : "md-bloque")?.scrollIntoView({ behavior: "auto", block: "start" });
  },
  md81(_, v) { estado.md81 = !!v; estado.mdPaso = null; pintarDetalle(); },
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
      await Store.guardar("vision", conAnio({ id: nuevoId(), orden: ORIGENES_VISION().indexOf(origen), publicacionId: null, ...p }));
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
    } catch (e) { console.error(e); toast("No se pudo guardar la foto." + detalleError(e)); }
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
  texto("SuRoute · Método Cima 9×52", 40, H - 50, 1000, 26, "#fff", "700s", 1, true);
  return await new Promise((res, rej) => cv.toBlob(b => b ? res(b) : rej(new Error("imagen")), "image/jpeg", 0.9));
}
window.imagenMandala = imagenMandala;

/** Publica una imagen como publicación de visión (en la nube o en el ejemplo). */
async function publicarImagenVision(blob, visibilidad, texto, metaTitulo, tipo = "VISION", eje = null, brujula = null, anio = new Date().getFullYear()) {
  let id;
  if (Store.nube) {
    const sb = Store.nube.sb, postId = crypto.randomUUID(), ruta = `${Store.nube.dueno}/${postId}.jpg`; id = postId;
    const up = await sb.storage.from("media").upload(ruta, blob, { upsert: true, contentType: "image/jpeg" });
    if (up.error) throw up.error;
    const url = sb.storage.from("media").getPublicUrl(ruta).data.publicUrl;
    const fila = { id: postId, user_id: Store.nube.dueno, tipo, texto, image_url: url, eje, anio, visibilidad, meta_titulo: metaTitulo };
    if (brujula) fila.brujula = brujula;
    const r = await sb.from("posts").insert(fila);
    if (r.error) throw r.error;
    (estado.misPosts ||= []).unshift({ id: postId, anio, tipo, eje, texto, foto: url, metaTitulo, creadoEn: Date.now(), propio: true, visibilidad, brujula });
  } else {
    id = "mio-" + nuevoId();
    misPostsDemo.unshift({ id, autorNombre: perfil().nombre || "Yo", anio, tipo, eje, texto, foto: URL.createObjectURL(blob), metaTitulo,
      impulsos: 0, comentarios: 0, yoImpulse: false, creadoEn: Date.now(), propio: true, visibilidad, brujula });
    estado.misPosts = misPostsDemo;
  }
  estado.feed = null;
  return id;
}

// Esc aleja un nivel de la Brújula (cuando está abierta y no se está escribiendo)
document.addEventListener("keydown", e => {
  if (document.querySelector(".md-visor") && !e.target.closest("input, textarea")) {
    const n = estado.mdNivel ?? 0;
    if (e.key === "Escape" && !document.fullscreenElement) { e.preventDefault(); n > 0 ? ACC.mdNivel(null, String(n - 1)) : ACC.mdVisor(null, ""); return; }
    if (e.key === "-" || e.key === "_") { e.preventDefault(); if (n > 0) ACC.mdNivel(null, String(n - 1)); return; }
    if (e.key === "+" || e.key === "=") { e.preventDefault(); if (n < 2) ACC.mdNivel(null, String(n + 1)); return; }
    if (e.key === "f" || e.key === "F") { e.preventDefault(); ACC.mdPantalla(); return; }
  }
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
  const migas = ["Cumbre", "Campamentos", "9×9"].map((t, i) => (i ? `<span class="md-sep">›</span>` : "") +
    (i === nivel ? `<b aria-current="step">${t}</b>` : `<button data-acc="vbNivel" data-arg="${i}" class="${i > nivel ? "adelante" : ""}">${t}</button>`)).join("");
  const grande = nivel === 2 && b.conPasos !== false ? hoja(`<h3 style="margin-top:0">${bloque === 4 ? esc(b.cumbre) : esc(camp(Mandala.CAMPAMENTOS_FIJOS[Mandala.ANILLO.indexOf(bloque)])?.t || "")}</h3>
      <div class="md-grande">${[0, 1, 2, 3, 4, 5, 6, 7, 8].map(q => `<div>${celda(Math.floor(bloque / 3) * 3 + Math.floor(q / 3), (bloque % 3) * 3 + q % 3).replace('class="md-c', 'class="md-c md-leer')}</div>`).join("")}</div>`)
    : nivel === 2 ? hoja(`<p class="suave" style="margin:0">Esta persona compartió su cumbre y sus campamentos, sin el texto de sus pasos.</p>`) : "";
  return cab(av(ic("ruta"), "var(--burdeos)"), `Brújula de ${esc(p.autorNombre || "la comunidad")}`, b.fecha ? `Así iba el ${esc(b.fecha)}` : "Brújula de la Cima") + cuerpo(
    hoja(`<nav class="md-barra"><button class="md-atras" data-acc="vbNivel" data-arg="${nivel - 1}" ${nivel ? "" : "hidden"} aria-label="Alejar">${ic("volver")}</button>
      <span class="md-migas">${migas}</span><span class="md-num">${["1", "9", "81"][nivel]}</span></nav>
      <div class="md-zoom md-papel">${vista}</div>`) + grande + siguenPublicaciones(id), "max-width:720px");
};
/** Debajo de una Brújula compartida la comunidad continúa: las publicaciones que siguen en el muro. */
function siguenPublicaciones(id) {
  const ps = (estado.feed || []).filter(x => !x.oculto), i = ps.findIndex(x => String(x.id) === String(id));
  const resto = [...ps.slice(i + 1), ...ps.slice(0, Math.max(0, i))];
  if (!resto.length) return "";
  return `<section class="vb-siguen" aria-label="Más publicaciones"><h3>Sigue explorando</h3>${resto.map(x => `<article class="hoja vb-pub">
      <span class="vb-quien"><b>${esc(x.autorNombre || "")}</b>${x.autorUsuario ? " @" + esc(x.autorUsuario) : ""} · ${hace(x.creadoEn)}</span>
      ${x.brujula && leerBrujula(x.brujula) ? brujulaMuro(x) : `<button data-ir="comunidad" data-arg="post:${esc(x.id)}" style="text-align:left;padding:0;border:0;background:none">${postal(x)}</button>${window.botonRecuerdosPost ? botonRecuerdosPost(x) : ""}`}
      ${x.texto && !x.brujula ? `<p style="margin:0">${esc(String(x.texto).slice(0, 220))}</p>` : ""}</article>`).join("")}</section>`;
}
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


/* El visor vive en el <body>, por encima de la barra lateral y del panel; se vacía al salir de la Brújula. */
function montarVisor(html) {
  let raiz = document.getElementById("visorRaiz");
  if (!raiz) {
    raiz = document.createElement("div"); raiz.id = "visorRaiz"; document.body.appendChild(raiz);
    raiz.onclick = e => clic(e); raiz.onchange = e => cambio(e);
  }
  const actual = raiz.querySelector(".md-visor");
  if (!html) {
    if (actual) raiz.innerHTML = "";   // corte limpio: la página ya está en su lugar, no se mezclan las dos capas
    return;
  }
  if (actual) {
    // Cambio de nivel: solo cambia el contenido; el fondo del visor no se vuelve a animar (no se ve lo de atrás)
    const t = document.createElement("template"); t.innerHTML = html;
    const nuevo = t.content.querySelector(".md-visor");
    if (nuevo && actual.innerHTML !== nuevo.innerHTML) actual.innerHTML = nuevo.innerHTML;
    return;
  }
  raiz.innerHTML = html;
}
const pintarDetalleBase = pintarDetalle;
pintarDetalle = function () {
  pintarDetalleBase.apply(this, arguments);
  if (!document.querySelector(".md-secciones")) { if (estado.mdVisor) estado.mdVisor = false; montarVisor(""); }
};

/* ======================================================================
 * Mi vida: el vision board es el eje. Cumbre al centro, 8 campamentos alrededor, y cada uno muestra
 * las metas que lo sostienen y en qué años. Tocar un campamento abre su planeación: metas por año,
 * agregar una meta para ese campamento y fotos de visión. Debajo, los años vividos y por vivir.
 * ====================================================================== */
/*
 * Largo, mediano y corto plazo en el vision board. Cada nivel se une a un campamento por su eje; si no
 * tiene eje, hereda el de arriba: la meta del mes el de su meta del año, y la meta del año el de su propósito.
 * Sin eje en toda la cadena: Confluencia (un proyecto que mueve varios ejes).
 */
function ejeEfectivo(tipo, o) {
  if (!o) return null;
  if (tipo === "proposito") return o.eje || null;
  if (tipo === "meta_anio") return o.eje || ejeEfectivo("proposito", o.propositoId != null ? Store.get("proposito", o.propositoId) : null);
  if (tipo === "meta_mes") return o.eje || ejeEfectivo("meta_anio", o.metaAnualId != null ? Store.get("meta_anio", o.metaAnualId) : null);
  return null;
}
const deCamp = (tipo, i) => o => { const cod = Mandala.CAMPAMENTOS_FIJOS[i], e = ejeEfectivo(tipo, o); return cod === "CAM" ? false : cod === "CON" ? !e : e === cod; };
/** Propósitos (largo plazo) de un campamento. */
const propositosDeCampamento = i => Store.lista("proposito").filter(deCamp("proposito", i));
/** Metas del año (mediano plazo) de un campamento. */
function metasDeCampamento(i) { return Store.lista("meta_anio").filter(deCamp("meta_anio", i)).sort((a, b) => a.anio - b.anio); }
/** Metas del mes (corto plazo) de un campamento. */
const mensualesDeCampamento = i => Store.lista("meta_mes").filter(deCamp("meta_mes", i)).sort((a, b) => a.anio - b.anio || a.mes - b.mes);
window.ejeEfectivo = ejeEfectivo;
function visionVida() {
  const d = Mandala.datos(), hoy = hoyFecha().getFullYear(), sel = estado.vidaCamp ?? null;
  const fotoC = mdFoto(d.cumbre), pctC = mdPct(d, -1);
  if (!casillasVision().length) return "";
  const anios = ms => [...new Set(ms.map(m => m.anio))];
  let panel = "";
  if (sel !== null && sel >= 0 && sel < 8) {
    const c = d.camps[sel], cod = Mandala.CAMPAMENTOS_FIJOS[sel], ms = metasDeCampamento(sel), c0 = cascada();
    const props = propositosDeCampamento(sel), h0 = hoyFecha(), mesHoy = h0.getMonth() + 1;
    const mens = mensualesDeCampamento(sel).filter(m => m.anio > hoy || (m.anio === hoy && m.mes >= mesHoy)).slice(0, 6);
    const largo = `<div class="vv-nivel"><h5>Largo plazo · propósitos</h5>${props.map(p => `<button class="vv-meta" data-ir="metas" data-arg="p:${p.id}"><span>${esc(p.titulo)} · ${p.horizonte || 5} años</span><span class="pct">${pctTxt(c0.nodoProp(p))}</span><span class="barra"><i style="width:${c0.nodoProp(p) * 100}%"></i></span></button>`).join("") || `<p class="suave">Sin propósitos todavía.</p>`}
        <button class="btn mini oro" data-acc="nuevoProposito" data-arg="${cod === "CON" ? "" : cod}">${ic("mas")} Propósito</button></div>`;
    const corto = `<div class="vv-nivel"><h5>Corto plazo · metas del mes</h5>${mens.map(m => `<button class="vv-meta" data-ir="metas" data-arg="m:${m.id}"><span>${MESES[m.mes - 1]} ${m.anio} · ${esc(m.texto)}</span><span class="pct">${pctTxt(avanceMetaMes(m))}</span><span class="barra"><i style="width:${avanceMetaMes(m) * 100}%"></i></span></button>`).join("") || `<p class="suave">Sin metas del mes todavía.</p>`}
        <button class="btn mini oro" data-acc="nuevaMetaMes" data-arg="${hoy}-${String(mesHoy).padStart(2, "0")}||${cod === "CON" ? "" : cod}">${ic("mas")} Meta de ${MESES[mesHoy - 1]}</button></div>`;
    const porAnio = anios(ms).map(a => `<div class="vv-anio"><button class="vv-anio-n" data-ir="ruta" data-arg="a:${a}">${a}${a === hoy ? " · este año" : a < hoy ? "" : " · por vivir"}</button>
        ${ms.filter(m => m.anio === a).map(m => `<button class="vv-meta" data-ir="metas" data-arg="a:${m.id}"><span>${esc(m.titulo)}</span><span class="pct">${pctTxt(c0.nodoAnio(m))}</span><span class="barra"><i style="width:${c0.nodoAnio(m) * 100}%"></i></span></button>`).join("")}</div>`).join("");
    const fotos = (estado.misPosts || []).filter(p => p.tipo === "VISION" && p.eje === cod && p.foto).slice(0, 6);
    panel = `<div class="vv-panel" style="--c:${d.color(sel)}"><div class="vv-panel-cab"><div><small>${esc(Mandala.nombre(cod))} · rumbo ${Mandala.RUMBOS[sel]}</small><h4>${c ? esc(mdNombre(c)) : "Campamento libre"}</h4></div>
        <button class="md-visor-ic" data-acc="vidaCamp" data-arg="" aria-label="Cerrar">${ic("cerrar")}</button></div>
      ${cod === "CAM" ? `<p class="suave">Tu campamento base son las personas que suben contigo: tu cordada, tu mentor y tu coach de vida.</p><div class="botones"><button class="btn" data-ir="mensajes" data-arg="cordadas">Mis cordadas</button><button class="btn" data-ir="mensajes" data-arg="coachvida">Mi coach de vida</button></div>`
        : `${largo}<div class="vv-nivel"><h5>Mediano plazo · metas del año</h5>${porAnio || `<p class="suave">Sin metas del año todavía. ¿En qué año la quieres lograr?</p>`}
        <div class="botones">${[hoy, hoy + 1, hoy + 2].map(a => `<button class="btn mini oro" data-acc="nuevaMetaAnio" data-arg="${a}||${cod === "CON" ? "" : cod}">${ic("mas")} Meta para ${a}</button>`).join("")}</div></div>${corto}`}
      ${fotos.length ? `<div class="album album-tira" style="margin-top:12px">${fotos.map(p => `<button class="recuerdo" data-ir="comunidad" data-arg="post:${esc(p.id)}"><img src="${esc(p.foto)}" alt="" loading="lazy"><span>${esc(p.metaTitulo || "")}</span></button>`).join("")}</div>` : ""}
      <div class="botones" style="margin-top:10px">${c ? `<button class="btn mini" data-acc="vidaBrujula" data-arg="${Mandala.ANILLO[sel]}">${ic("ruta")} Sus 8 pasos en la Brújula</button>` : `<button class="btn mini" data-acc="crearCampamento" data-arg="${cod}">${ic("mas")} Crear este campamento</button>`}</div></div>`;
  }
  const chips = [0, 1, 2, 3, 4, 5, 6, 7].map(i => {
    const cod = Mandala.CAMPAMENTOS_FIJOS[i], ms = [...propositosDeCampamento(i), ...metasDeCampamento(i), ...mensualesDeCampamento(i)];
    return `<button class="vv-chip${sel === i ? " elegido" : ""}" style="--c:${d.color(i)}" data-acc="vidaCamp" data-arg="${i}" aria-pressed="${sel === i}">
      <i aria-hidden="true"></i>${esc(Mandala.nombre(cod))}<span>${cod === "CAM" ? "" : ms.length}</span></button>`;
  }).join("");
  return hoja(`<div class="vv-cab"><div><h3>Planear por campamento</h3><p class="suave">Las metas que sostienen cada campamento de tu Brújula, año por año. Elige uno para verlas o crear una nueva.</p></div>
      <button class="btn mini lleno" data-acc="compartirVision" ${mdPublicando ? "disabled" : ""}>${ic("publicar")} ${mdPublicando ? "Preparando…" : "Compartir mis campamentos"}</button></div>
    <div class="vv-chips">${chips}</div>${panel}`);
}
Object.assign(ACC, {
  vidaCamp(_, i) { estado.vidaCamp = i === "" || Number(i) === estado.vidaCamp ? null : Number(i); if (!estado.misPosts) cargarMisPosts(); pintarDetalle();
    if (estado.vidaCamp !== null) requestAnimationFrame(() => document.querySelector(".vv-panel")?.scrollIntoView({ behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth", block: "nearest" })); },
  /** Comparte el vision board (la imagen de la Brújula con su tablero) en la pestaña Visión de la comunidad. */
  vidaBrujula(_, b) { estado.mandalaBloque = Number(b); estado.mdPaso = null; if (!document.getElementById("md-bloque")) ir("perfil", "vision"); else pintarDetalle(); requestAnimationFrame(() => document.getElementById("md-bloque")?.scrollIntoView({ block: "start" })); },
  compartirVision() { estado.mdCompartir = true; if (!document.querySelector(".md-secciones")) ir("perfil", "vision"); else pintarDetalle(); requestAnimationFrame(() => document.querySelector(".md-compartir")?.scrollIntoView({ block: "center" })); },
});
window.visionVida = visionVida;

/* Girar la tarjeta del 9×9 sin redibujar (la animación se ve completa) */
ACC.md81Girar = el => {
  estado.md81Girada = !estado.md81Girada;
  const caja = document.querySelector(".rb-81 .md-flip, #md-s81 ~ .md-flip"); if (!caja) return pintarDetalle();
  caja.classList.toggle("girada", estado.md81Girada);
  caja.querySelector(".cara-texto")?.setAttribute("aria-hidden", String(estado.md81Girada));
  caja.querySelector(".cara-imagen")?.setAttribute("aria-hidden", String(!estado.md81Girada));
  el.setAttribute("aria-pressed", String(estado.md81Girada));
  el.setAttribute("aria-label", estado.md81Girada ? "Ver el texto" : "Ver las imágenes");
  el.setAttribute("aria-label", estado.md81Girada ? "Girar al texto" : "Girar a las imágenes");
};

/* Vision board: el ícono de girar de arriba gira todas las tarjetas, en cascada (una tras otra) */
ACC.girarTodasVision = (el, sel) => {
  const ts = [...document.querySelectorAll(typeof sel === "string" && sel ? sel : ".rosa-brujula .tablero-9 .casilla-v.giro, #md-s9 ~ .tablero-9 .casilla-v.giro")]; if (!ts.length) return;
  const atras = ts.some(t => !t.classList.contains("girada"));
  ts.forEach((t, i) => {
    const k = t.dataset.casilla, enModo = estado.modoTarjetas !== "imagen";
    if (k) { if (atras !== enModo) estado.giradas.add(k); else estado.giradas.delete(k); }
    const giro = t.querySelector(".giro-in") || t; giro.style.setProperty("--retraso", pocoMovimiento() ? "0ms" : `${Math.min(i, 10) * 45}ms`);
    ladoTarjeta(t, atras);
  });
  el.setAttribute("aria-pressed", String(atras));
};

/** La brújula minimalista (Material 3): sin esfera, solo los cuatro rumbos N · E · S · O alrededor de la tarjeta. */
function rosaBrujula(tablero, clase = "", _ = "") {
  const rumbos = [["N", "n"], ["E", "e"], ["S", "s"], ["O", "o"]];
  return `<div class="rosa-brujula rb-min ${clase}">
    ${rumbos.map(([t, k]) => `<span class="rb-rumbo rb-${k}${t === "N" ? " norte" : ""}" aria-hidden="true">${t}</span>`).join("")}
    ${tablero}</div>`;
}

/* ---------- El modelo: Brújula 1·9·81 ---------- */
const MODELO_REFS = [
  "Harada, T., y Shibayama, K. (2017). <i>Ichiryū no tasseiryoku: Harada mesoddo «Ōpun Uindō 64»</i> [La capacidad de logro de primer nivel: el método Harada «Open Window 64»]. Forest Shuppan.",
  "Imaizumi, H. (1988). <i>Chō memogaku nyūmon: Mandarāto no gihō</i> [Introducción a la ciencia de las notas: la técnica del Mandal-Art]. Nihon Jitsugyō Shuppansha.",
  "Mandala Chart Kyōkai. (s. f.). <i>Mandara chāto to wa</i> [Qué es la Mandala Chart]. https://mandalachart.jp/wp/mandalachart",
  "Matsumura, Y. (2006). <i>Mandara shikō de yume wa kanarazu kanau!</i> [¡Con el pensamiento mandala los sueños sí se cumplen!]. Forest Shuppan.",
  "Matsumura, Y. (2007). <i>Zukai mandara chāto</i> [La Mandala Chart ilustrada]. Seishun Shuppansha.",
];
function modeloHtml() {
  // Diagrama: 81 casillas, el 3×3 del centro (9) y la cumbre (1) en oro
  let celdas = "";
  for (let f = 0; f < 9; f++) for (let c = 0; c < 9; c++) {
    const enNueve = f >= 3 && f < 6 && c >= 3 && c < 6, centro = f === 4 && c === 4;
    celdas += `<rect x="${c * 11 + 1}" y="${f * 11 + 1}" width="9.4" height="9.4" rx="2" class="${centro ? "mo-1" : enNueve ? "mo-9" : (f % 3 === 1 && c % 3 === 1) ? "mo-c" : "mo-81"}"/>`;
  }
  const escalas = [
    ["1", "La cumbre", "Tu propósito en una frase y una foto. Es el norte de todo lo demás."],
    ["9", "Los 8 campamentos", "Alrededor de la cumbre, 8 rumbos que la sostienen: Confluencia, Trascendencia, Valor, Evolución, Voz, Campamento base, Voluntad y Maestría. Juntos forman la capa Campamentos."],
    ["81", "Los 64 pasos", "Cada campamento se abre en 8 pasos concretos. La cumbre, los 8 campamentos y los 64 pasos forman la Brújula 9×9."],
  ];
  const camino = [
    ["Soñar", "Escribe tu cumbre y ponle una foto."],
    ["Orientar", "Nombra tus 8 campamentos: los rumbos de tu vida."],
    ["Desglosar", "Escribe los 64 pasos, 8 por campamento."],
    ["Caminar", "Cada día avanzas en un paso. Un paso no se marca: se gana con jornadas, días distintos en que avanzaste."],
    ["Medir", "Método Cima 9×52: el año son 52 tramos y tu montaña mide 8.848.000 mm, como el Everest. Cada 4 tramos la dificultad se ajusta para que llegues el 31 de diciembre."],
    ["Cuidarte", "El bienestar de cada día (energía, actividad física, respiración) se suma en el mes y en el año."],
    ["Evidenciar", "Una foto por paso: lo vivido va reemplazando a lo soñado."],
    ["Cerrar", "Al terminar el año escribes 7 lecciones, las guardas en tu diario de vida y empiezas una nueva montaña."],
  ];
  return `<div class="mo">
    <div class="mo-cab"><svg class="mo-dia" viewBox="0 0 100 100" aria-hidden="true">${celdas}</svg>
      <div><h3>Brújula 1·9·81</h3><p>Tu vida es una montaña. La cumbre es hacia dónde vas, la brújula es cómo no perderte y cada día es un paso.</p></div></div>
    <h4>Tres escalas, una sola brújula</h4>
    <ol class="mo-escalas">${escalas.map(([n, t, d]) => `<li><b class="mo-n">${n}</b><div><b>${t}</b><span>${d}</span></div></li>`).join("")}</ol>
    <h4>El camino del año</h4>
    <ol class="mo-camino">${camino.map(([t, d], i) => `<li><span class="mo-i">${i + 1}</span><div><b>${t}</b><span>${d}</span></div></li>`).join("")}</ol>
    <h4>En el tiempo</h4>
    <p>Hay una Brújula General para toda tu vida y una para cada año, desde el año después de tu nacimiento hasta tu meta de vida. Elige el año en el riel y verás la cumbre de ese año.</p>
    <h4>No subes solo</h4>
    <p>Comparte tu 9×9 en la comunidad, sube en cordada y deja que otros te impulsen con su clima.</p>
    <h4>Origen</h4>
    <p>La Brújula 1·9·81 se inspira en la Mandala Chart que Yasuo Matsumura creó en Japón en 1979 (Matsumura, 2007). Llevó la estructura de 3×3 del mandala budista a la planeación de la vida y del trabajo: una meta en el centro y ocho áreas alrededor (salud, trabajo, economía, familia, sociedad, carácter, aprendizaje y ocio), para vivir con equilibrio (Mandala Chart Kyōkai, s. f.). Hiroaki Imaizumi la convirtió en el Mandal-Art para generar ideas (Imaizumi, 1988) y Takashi Harada en la hoja Open Window 64 para alcanzar metas, la que usó Shohei Ohtani (Harada y Shibayama, 2017).</p>
    <p>El Método Ruta a la Cima la vuelve una brújula de montaña: una cumbre, 8 campamentos y 64 pasos que se ganan con jornadas, medidos con el Método Cima 9×52 y con una foto de evidencia por paso.</p>
    <h4>Referencias</h4>
    <ul class="mo-refs">${MODELO_REFS.map(r => `<li>${r}</li>`).join("")}</ul></div>`;
}

/* ---------- Capas 1 · 9 · 81 en una sola página ---------- */
function mdCapaActual() {
  if (!estado.mdCapa) { try { estado.mdCapa = Number(localStorage.getItem("suroute-md-capa")) || 9; } catch { estado.mdCapa = 9; } }
  return [1, 9, 81].includes(estado.mdCapa) ? estado.mdCapa : 9;
}
function mdCapaGuardar(n) { estado.mdCapa = n; try { localStorage.setItem("suroute-md-capa", String(n)); } catch {} }
/** Cambia de capa sin redibujar: la nueva entra como si se acercara (más adentro) o se alejara (más afuera) la brújula. */
function mdCapa(n) {
  const antes = mdCapaActual(); mdCapaGuardar(n);
  const pila = document.querySelector(".md-pila"); if (!pila) return pintarDetalle();
  pila.querySelectorAll(".md-capa").forEach(c => { const si = Number(c.dataset.capa) === n; c.hidden = !si; c.classList.toggle("activa", si);
    c.classList.remove("entra-dentro", "entra-fuera"); if (si && antes !== n && !pocoMovimiento?.()) { void c.offsetWidth; c.classList.add(n > antes ? "entra-dentro" : "entra-fuera"); } });
  document.querySelectorAll("[data-capa-btn]").forEach(b => b.setAttribute("aria-selected", String(Number(b.dataset.capaBtn) === n)));
  const info = document.getElementById("mdRielInfo"); if (info) info.textContent = MD_CAPA_INFO[n];
  // Que la capa quede a la vista bajo la barra (sin desplazar si ya lo está)
  const r = pila.getBoundingClientRect(); if (r.top < 0 || r.top > innerHeight * .4) pila.scrollIntoView({ block: "start", behavior: "auto" });
  navigator.vibrate?.(6);
}
window.mdCapa = mdCapa;
/* Teclado: + acerca (1 → 9 → 81) y − aleja, cuando no se está escribiendo */
document.addEventListener("keydown", e => {
  if (!document.querySelector(".md-pila") || estado.mdVisor || /INPUT|TEXTAREA|SELECT/.test(document.activeElement?.tagName || "") || e.ctrlKey || e.metaKey) return;
  const orden = [1, 9, 81], i = orden.indexOf(mdCapaActual());
  if ((e.key === "+" || e.key === "=") && i < 2) { e.preventDefault(); mdCapa(orden[i + 1]); }
  if ((e.key === "-" || e.key === "_") && i > 0) { e.preventDefault(); mdCapa(orden[i - 1]); }
});

/* El riel de la derecha actúa sobre la capa que se ve: girar sus tarjetas y abrirla en pantalla completa */
const MD_CAPA_INFO = {
  1: "Tu cumbre: lo más grande, hacia dónde va tu vida. Todo lo de abajo la sostiene.",
  9: "Campamentos: tu cumbre al centro y 8 campamentos alrededor, como una brújula. Imagen muestra tus fotos, Texto tus frases y Edición te deja escribir en cada tarjeta. La letra del rumbo abre sus 8 pasos.",
  81: "La Brújula 9×9: los 64 pasos. Gírala para ver tus imágenes; en Edición escribes directo en cada casilla y en Imagen o Texto, tocar un bloque lo abre con sus jornadas.",
};
ACC.mdGirarCapa = el => {
  const n = mdCapaActual();
  if (n === 81) return ACC.md81Girar(el);
  if (n === 1) return ACC.girarTodasVision(el, '.md-capa[data-capa="1"] .casilla-v.giro');
  ACC.girarTodasVision(el);
};
ACC.mdVisorCapa = el => ACC.mdVisor(el, String({ 1: 0, 9: 1, 81: 2 }[mdCapaActual()]));

/* Cajones del riel: se abren y cierran sin redibujar (uno a la vez) */
ACC.mdCajon = (el, k) => {
  estado.mdCajon = estado.mdCajon === k ? null : k;
  document.querySelectorAll(".md-cajon").forEach(c => c.classList.toggle("abierto", c.dataset.cajon === estado.mdCajon));
  document.querySelectorAll(".md-riel-cajon").forEach(b => b.setAttribute("aria-pressed", String(b.dataset.arg === estado.mdCajon)));
};
document.addEventListener("keydown", e => { if (e.key === "Escape" && estado.mdCajon && document.querySelector(".md-cajon.abierto")) ACC.mdCajon(null, estado.mdCajon); });
