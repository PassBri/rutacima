"use strict";
/* El cielo de la franja superior: el sol y la luna se mueven con la hora real, el cielo pasa del
 * amanecer al día, al atardecer y a la noche, y la luna muestra su fase de hoy. Las fórmulas son las
 * de SunCalc (Vladimir Agafonkin, BSD) reducidas a lo necesario. Sin ubicación exacta: la latitud es
 * la de Colombia y la longitud sale de la zona horaria del navegador (no se pide permiso de ubicación). */
const Cielo = (() => {
  const rad = Math.PI / 180, dia = 864e5, J1970 = 2440588, J2000 = 2451545, e = rad * 23.4397;
  const dias = d => d.valueOf() / dia - 0.5 + J1970 - J2000;
  const ascension = (l, b) => Math.atan2(Math.sin(l) * Math.cos(e) - Math.tan(b) * Math.sin(e), Math.cos(l));
  const declinacion = (l, b) => Math.asin(Math.sin(b) * Math.cos(e) + Math.cos(b) * Math.sin(e) * Math.sin(l));
  const azimut = (H, phi, dec) => Math.atan2(Math.sin(H), Math.cos(H) * Math.sin(phi) - Math.tan(dec) * Math.cos(phi));
  const altura = (H, phi, dec) => Math.asin(Math.sin(phi) * Math.sin(dec) + Math.cos(phi) * Math.cos(dec) * Math.cos(H));
  const sideral = (d, lw) => rad * (280.16 + 360.9856235 * d) - lw;
  const sol = d => {
    const M = rad * (357.5291 + 0.98560028 * d);
    const L = M + rad * (1.9148 * Math.sin(M) + 0.02 * Math.sin(2 * M) + 0.0003 * Math.sin(3 * M)) + rad * 102.9372 + Math.PI;
    return { dec: declinacion(L, 0), ra: ascension(L, 0) };
  };
  const luna = d => {
    const L = rad * (218.316 + 13.176396 * d), M = rad * (134.963 + 13.064993 * d), F = rad * (93.272 + 13.22935 * d);
    const l = L + rad * 6.289 * Math.sin(M), b = rad * 5.128 * Math.sin(F);
    return { ra: ascension(l, b), dec: declinacion(l, b), dist: 385001 - 20905 * Math.cos(M) };
  };
  const posicion = (cuerpo, fecha, lat, lng) => {
    const lw = rad * -lng, phi = rad * lat, d = dias(fecha), c = cuerpo(d), H = sideral(d, lw) - c.ra;
    return { az: azimut(H, phi, c.dec), alt: altura(H, phi, c.dec) };
  };
  const iluminacion = fecha => {
    const d = dias(fecha), s = sol(d), m = luna(d), sd = 149598000;
    const phi = Math.acos(Math.sin(s.dec) * Math.sin(m.dec) + Math.cos(s.dec) * Math.cos(m.dec) * Math.cos(s.ra - m.ra));
    const inc = Math.atan2(sd * Math.sin(phi), m.dist - sd * Math.cos(phi));
    const ang = Math.atan2(Math.cos(s.dec) * Math.sin(s.ra - m.ra), Math.sin(s.dec) * Math.cos(m.dec) - Math.cos(s.dec) * Math.sin(m.dec) * Math.cos(s.ra - m.ra));
    return { fraccion: (1 + Math.cos(inc)) / 2, fase: 0.5 + 0.5 * inc * (ang < 0 ? -1 : 1) / Math.PI };
  };
  const LAT = 7.1, LNG = -new Date().getTimezoneOffset() / 60 * 15;

  /* Planetas: elementos keplerianos aproximados de JPL (Standish), válidos de 1800 a 2050.
     a, e, I, L, ϖ, Ω y sus tasas por siglo. Basta para verlos en el lugar correcto del cielo. */
  const EL = {
    tierra: [1.00000261, .01671123, -.00001531, 100.46457166, 102.93768193, 0, .00000562, -.00004392, -.01294668, 35999.37244981, .32327364, 0],
    venus: [.72333566, .00677672, 3.39467605, 181.9790995, 131.60246718, 76.67984255, .0000039, -.00004107, -.0007889, 58517.81538729, .00268329, -.27769418],
    marte: [1.52371034, .0933941, 1.84969142, -4.55343205, -23.94362959, 49.55953891, .00001847, .00007882, -.00813131, 19140.30268499, .44441088, -.29257343],
    jupiter: [5.202887, .04838624, 1.30439695, 34.39644051, 14.72847983, 100.47390909, -.00011607, -.00013253, -.00183714, 3034.74612775, .21252668, .20469106],
    saturno: [9.53667594, .05386179, 2.48599187, 49.95424423, 92.59887831, 113.66242448, -.0012506, -.00050991, .00193609, 1222.49362201, -.41897216, -.28867794],
  };
  function helio(k, d) {
    const T = d / 36525, el = EL[k], v = i => el[i] + el[i + 6] * T;
    const a = v(0), ex = v(1), I = v(2) * rad, O = v(5) * rad, M = ((v(3) - v(4)) % 360) * rad, om = (v(4) - v(5)) * rad;
    let E = M + ex * Math.sin(M); for (let i = 0; i < 6; i++) E -= (E - ex * Math.sin(E) - M) / (1 - ex * Math.cos(E));
    const xp = a * (Math.cos(E) - ex), yp = a * Math.sqrt(1 - ex * ex) * Math.sin(E);
    const co = Math.cos(om), so = Math.sin(om), cO = Math.cos(O), sO = Math.sin(O), cI = Math.cos(I), sI = Math.sin(I);
    return [(co * cO - so * sO * cI) * xp + (-so * cO - co * sO * cI) * yp, (co * sO + so * cO * cI) * xp + (-so * sO + co * cO * cI) * yp, so * sI * xp + co * sI * yp];
  }
  const planeta = k => d => {
    const p = helio(k, d), t = helio("tierra", d), x = p[0] - t[0], y = p[1] - t[1], z = p[2] - t[2];
    const l = Math.atan2(y, x), b = Math.atan2(z, Math.hypot(x, y));
    return { ra: ascension(l, b), dec: declinacion(l, b) };
  };
  /** Cuándo se ve cada planeta: Venus y Júpiter brillan desde el crepúsculo; Marte y Saturno, ya de noche. */
  const PLANETAS = [["venus", "Venus", -3], ["jupiter", "Júpiter", -5], ["marte", "Marte", -8], ["saturno", "Saturno", -8]];

  /** Colores del cielo según la altura del sol (en grados): noche, alba y día, con bruma clara en el horizonte. */
  const mezcla = (a, b, t) => a.map((x, i) => Math.round(x + (b[i] - x) * t));
  const rgb = c => `rgb(${c.join(",")})`;
  const NOCHE = [[6, 10, 20], [16, 24, 40], [30, 40, 60]], ALBA = [[44, 58, 98], [126, 116, 140], [238, 172, 122]], DIA = [[78, 132, 186], [150, 190, 222], [222, 233, 238]];
  function colores(alt) {
    const mx = (A, B, t) => A.map((c, i) => mezcla(c, B[i], t));
    if (alt <= -10) return NOCHE;
    if (alt <= 2) return mx(NOCHE, ALBA, (alt + 10) / 12);
    if (alt <= 14) return mx(ALBA, DIA, (alt - 2) / 12);
    return DIA;
  }

  /** Forma iluminada de la luna (camino SVG centrado en 0,0) según su fase (0 nueva, .5 llena). */
  function faseLuna(fase, r) {
    const crece = fase < 0.5, rx = Math.abs(Math.cos(2 * Math.PI * fase)) * r;
    const lado = crece ? 1 : 0;                       // arco del borde iluminado: derecha al crecer
    const giba = (fase > 0.25 && fase < 0.75) ? lado : 1 - lado;
    return `M0 ${-r} A${r} ${r} 0 0 ${lado} 0 ${r} A${rx} ${r} 0 0 ${giba} 0 ${-r}Z`;
  }

  const estrellas = Array.from({ length: 70 }, (_, i) => ({ x: (i * 97.3) % 100, y: (i * 37.7) % 70, r: 0.5 + (i % 3) * 0.35, t: (i % 7) * 0.6 }));
  const hora = f => f.toLocaleTimeString("es", { hour: "2-digit", minute: "2-digit" });

  /* ---------- El cielo real: constelaciones, lluvias de meteoros y eventos (datos de tools/cielo) ----------
     Misma lógica que la app (Astronomia.kt / CieloEventos.kt). Nunca se inventa un evento: todo sale de
     CIELO_DATOS, calculado con astronomy-engine y el calendario de la IMO para Santander, Colombia. */
  const DATOS = typeof CIELO_DATOS !== "undefined" ? CIELO_DATOS : { estrellas: [], lineas: [], nombres: {}, lluvias: [], eventos: [] };
  const HORA_MS = 36e5, DIA_MS = 864e5;
  const fijo = (ra, dec) => () => ({ ra: ra * rad, dec: dec * rad });
  /** La lluvia activa en este instante: días al pico (Δ) y tasa por hora antes de corregir por altura y luna. */
  function lluviaDe(ahora) {
    const t = ahora.getTime(), y = ahora.getUTCFullYear();
    const fecha = (a, md) => { const [m, d] = md.split("-").map(Number); return Date.UTC(a, m - 1, d, 6); };
    for (const l of DATOS.lluvias) for (const a of [y - 1, y, y + 1]) {
      const pico = fecha(a, l.pico);
      let desde = fecha(a, l.desde), hasta = fecha(a, l.hasta);
      if (desde > pico) desde = fecha(a - 1, l.desde);
      if (hasta < pico) hasta = fecha(a + 1, l.hasta);
      if (t >= desde - DIA_MS / 4 && t <= hasta + DIA_MS * 0.75) {
        const delta = (t - pico) / DIA_MS;
        return { ...l, pico, delta, tasa: l.thz * Math.exp(-Math.abs(delta) / l.ancho) };
      }
    }
    return null;
  }
  /** El evento del cielo que toca contar ahora (uno solo, por prioridad), o null. */
  function eventoDe(ahora) {
    const t = ahora.getTime(), ms = x => Date.parse(x);
    const ev = DATOS.eventos;
    const eclipse = ev.find(e => e.tipo.startsWith("eclipse") && t >= ms(e.inicio) - DIA_MS && t <= ms(e.fin));
    if (eclipse) return eclipse;
    const ll = lluviaDe(ahora);
    if (ll && Math.abs(ll.delta) <= 3) return { tipo: "lluvia", lluvia: ll, max: new Date(ll.pico).toISOString() };
    const cerca = (tipo, h) => ev.filter(e => e.tipo === tipo && Math.abs(t - ms(e.max)) <= h * HORA_MS)
      .sort((a, b) => Math.abs(t - ms(a.max)) - Math.abs(t - ms(b.max)))[0];
    return cerca("conjuncion", 18) || cerca("superluna", 18) || cerca("estacion", 12) || null;
  }
  /** Cuánto cubre el eclipse en curso (0..1) y en qué fase va: la sombra crece hasta el máximo y luego se retira. */
  function cubrimiento(e, t) {
    const ms = x => Date.parse(x), max = ms(e.max);
    if (e.tipo === "eclipse_sol") {
      const a = ms(e.inicio), b = ms(e.fin);
      if (t < a || t > b) return 0;
      return e.oscurecimiento * (t <= max ? (t - a) / (max - a) : (b - t) / (b - max));
    }
    if (e.totalInicio && t >= ms(e.totalInicio) && t <= ms(e.totalFin)) return 1;
    if (e.parcialInicio && t >= ms(e.parcialInicio) && t <= ms(e.parcialFin)) {
      const pico = e.totalInicio ? (t < max ? ms(e.totalInicio) : ms(e.totalFin)) : max, borde = t < max ? ms(e.parcialInicio) : ms(e.parcialFin);
      return Math.max(0.05, (e.totalInicio ? 1 : e.oscurecimiento) * (t - borde) / (pico - borde));
    }
    const a = ms(e.inicio), b = ms(e.fin);
    if (t < a || t > b) return 0;
    return 0.12 * (t <= max ? (t - a) / (max - a) : (b - t) / (b - max));   // penumbra: apenas se apaga
  }
  const NOMBRE_ASTRO = { luna: "la Luna", venus: "Venus", marte: "Marte", jupiter: "Júpiter", saturno: "Saturno" };
  const CLASE_ECLIPSE = { total: "total", partial: "parcial", penumbral: "penumbral", annular: "anular" };
  const fechaCorta = t => new Date(t).toLocaleDateString("es", { day: "numeric", month: "long" });
  /** Una línea para el cielo y dos frases que lo explican con el lenguaje de la montaña. */
  function contar(e) {
    if (!e) return null;
    const h = x => hora(new Date(x)), cons = id => DATOS.nombres[id]?.es || id;
    switch (e.tipo) {
      case "eclipse_luna": return {
        linea: `Eclipse ${CLASE_ECLIPSE[e.clase]} de luna · ${fechaCorta(e.max)} · ${h(e.parcialInicio || e.inicio)}–${h(e.parcialFin || e.fin)}`,
        explica: e.clase === "total" ? "La Tierra se pone entre el sol y la luna y la cubre con su sombra: la luna se vuelve color cobre. Se ve a simple vista, sin ningún filtro."
          : e.clase === "partial" ? "La sombra de la Tierra muerde una parte de la luna. Se ve a simple vista, sin ningún filtro."
          : "La luna pasa por la penumbra de la Tierra y se oscurece apenas. Es sutil: mírala con calma y compárala con otras noches." };
      case "eclipse_sol": return {
        linea: `Eclipse ${CLASE_ECLIPSE[e.clase] || "parcial"} de sol · ${fechaCorta(e.max)} · ${h(e.inicio)}–${h(e.fin)}`,
        explica: `La luna pasa delante del sol y desde Santander tapa el ${Math.round(e.oscurecimiento * 100)} %. Nunca lo mires directo: usa gafas de eclipse certificadas (ISO 12312-2) o proyéctalo con un cartón perforado.` };
      case "lluvia": return {
        linea: `Lluvia de meteoros desde ${cons(e.lluvia.constelacion)} · pico el ${fechaCorta(e.lluvia.pico)}`,
        explica: `La Tierra cruza el polvo que dejó un cometa: cada estela es un grano que arde al entrar al aire. Busca un lugar oscuro y mira hacia ${cons(e.lluvia.constelacion)} después de medianoche.` };
      case "conjuncion": {
        const [a, b] = e.cuerpos.map(c => NOMBRE_ASTRO[c]);
        return { linea: `${a[0].toUpperCase() + a.slice(1)} y ${b}, juntos · ${String(e.sep).replace(".", ",")}°`,
          explica: `${a[0].toUpperCase() + a.slice(1)} y ${b} se ven casi juntos, a ${String(e.sep).replace(".", ",")}° (menos que un dedo con el brazo estirado). Es una alineación aparente: en realidad están a millones de kilómetros.` };
      }
      case "superluna": return { linea: `Superluna · ${e.km.toLocaleString("es")} km`,
        explica: `La luna llena coincide con su paso más cercano a la Tierra (${e.km.toLocaleString("es")} km): se ve un poco más grande y más brillante.` };
      case "estacion": return {
        linea: { equinoccio_marzo: "Equinoccio de marzo", equinoccio_septiembre: "Equinoccio de septiembre", solsticio_junio: "Solsticio de junio", solsticio_diciembre: "Solsticio de diciembre" }[e.clase],
        explica: e.clase.startsWith("equinoccio") ? "El sol cruza el ecuador del cielo: hoy el día y la noche duran casi lo mismo en todo el planeta. En Colombia, a mediodía, el sol pasa casi por encima de tu cabeza."
          : e.clase === "solsticio_junio" ? "El sol llega a su punto más al norte del año. En Colombia, a mediodía, tu sombra apunta hacia el sur." : "El sol llega a su punto más al sur del año. En Colombia, a mediodía, tu sombra apunta hacia el norte." };
    }
    return null;
  }

  /**
   * El cielo como un instrumento de altura: degradado real del cielo, estrellas, la línea del horizonte
   * con marcas cada 15°, el arco que recorre el sol hoy, el sol y la luna con su anillo y su altura,
   * los planetas visibles con su nombre, y abajo la cordillera saliendo de un mar de nubes.
   */
  function pintar(el, ahora = new Date()) {
    const s = posicion(sol, ahora, LAT, LNG), m = posicion(luna, ahora, LAT, LNG), il = iluminacion(ahora);
    const altS = s.alt / rad, altM = m.alt / rad;
    const [arriba, medio, abajo] = colores(altS);
    const noche = Math.max(0, Math.min(1, (-altS - 2) / 10));
    const dia = Math.max(0, Math.min(1, (altS + 4) / 14));
    const completo = !el.classList.contains("cielo-mini");
    // Este (azimut -90°) a la izquierda, oeste (+90°) a la derecha; la altura sube hacia arriba
    const hud = el.dataset.cielo === "hud";
    const H0 = hud ? 60 : 74;                          // el horizonte, en % de alto (más arriba si hay texto debajo)
    const uid = pintar.n = (pintar.n || 0) + 1;         // ids únicos: varios cielos en la misma página
    const x = az => 50 + Math.sin(az) * 46, y = alt => H0 - Math.max(-12, alt / rad) / 90 * (H0 - 16);
    const sx = x(s.az), sy = y(s.alt), mx = x(m.az), my = y(m.alt);
    // Arco del sol de hoy, cada 20 minutos
    const media = new Date(ahora); media.setHours(0, 0, 0, 0);
    const arco = [];
    for (let k = 0; k <= 72; k++) { const p = posicion(sol, new Date(media.getTime() + k * 1200000), LAT, LNG); if (p.alt / rad > -8) arco.push(`${x(p.az).toFixed(2)},${y(p.alt).toFixed(2)}`); }
    const marcas = [-75, -60, -45, -30, -15, 0, 15, 30, 45, 60, 75].map(a => { const xx = x(a * rad); return `<line x1="${xx}" y1="${H0}" x2="${xx}" y2="${H0 - (a % 45 === 0 ? 2.4 : 1.2)}"/>`; }).join("");
    const linea = `rgba(255,255,255,${0.22 + 0.2 * noche})`;
    const ev = eventoDe(ahora);
    const planetas = completo ? PLANETAS.map(([k, nombre, umbral]) => {
      const p = posicion(planeta(k), ahora, LAT, LNG), alt = p.alt / rad;
      if (alt < 2 || altS > umbral) return "";
      const px = x(p.az), py = y(p.alt), bajoLectura = hud && py < 34 && (px < 36 || (px > 50 && ev));
      return `<span class="astro planeta ${px > 78 ? "izq" : ""}" style="left:${px}%;top:${py}%" title="${nombre}"><i></i>${bajoLectura ? "" : `<b>${nombre}</b>`}</span>`;
    }).join("") : "";
    // Constelaciones reales (solo la mitad sur del cielo, la que se ve de frente) en píxeles, sin deformar
    const W = el.clientWidth, H = el.clientHeight, t = ahora.getTime();
    let constelaciones = "";
    if (completo && noche > 0.05 && W > 0) {
      const pX = az => W * (0.5 + Math.sin(az) * 0.46), pY = alt => H * (H0 - Math.max(-12, alt / rad) / 90 * (H0 - 16)) / 100;
      const ver = p => p.alt > 0.02 && Math.cos(p.az) > 0.05;
      const pos = (ra, dec) => posicion(fijo(ra, dec), ahora, LAT, LNG);
      let lin = "", est = "", nom = "";
      const amplio = W >= 560;                         // en el teléfono: solo las constelaciones mayores y sin nombres
      for (const c of DATOS.lineas) if (amplio || c.r === 1) for (const seg of c.l) {
        let prev = null;
        for (const [ra, dec] of seg) {
          const p = pos(ra, dec), ok = ver(p), q = [pX(p.az), pY(p.alt)];
          if (ok && prev) lin += `M${prev[0].toFixed(1)} ${prev[1].toFixed(1)}L${q[0].toFixed(1)} ${q[1].toFixed(1)}`;
          prev = ok ? q : null;
        }
      }
      for (const [ra, dec, mag] of DATOS.estrellas) {
        const p = pos(ra, dec); if (!ver(p)) continue;
        est += `<circle cx="${pX(p.az).toFixed(1)}" cy="${pY(p.alt).toFixed(1)}" r="${(0.5 + (4 - mag) * 0.32).toFixed(2)}"/>`;
      }
      if (amplio) for (const [id, n] of Object.entries(DATOS.nombres)) {
        const p = pos(n.pos[0], n.pos[1]); if (!ver(p) || p.alt / rad < 8) continue;
        const nx = pX(p.az), ny = pY(p.alt); if (hud && ny < H * 0.34 && (nx < W * 0.36 || nx > W * 0.5)) continue;
        nom += `<text x="${nx.toFixed(0)}" y="${ny.toFixed(0)}">${n.es}</text>`;
      }
      constelaciones = `<svg class="cielo-const" viewBox="0 0 ${W} ${H}" style="opacity:${noche.toFixed(2)}" aria-hidden="true"><path d="${lin}"/><g>${est}</g>${nom}</svg>`;
    }
    // Lluvia de meteoros: estelas que nacen del radiante, solo de noche y con el radiante sobre el horizonte
    let estelas = "";
    const ll = completo ? lluviaDe(ahora) : null;
    if (ll && altS < -10) {
      const r = posicion(fijo(ll.ra, ll.dec), ahora, LAT, LNG);
      const lunaArriba = altM > 0 ? il.fraccion : 0;
      const efectiva = ll.tasa * Math.max(0, Math.sin(r.alt)) * (1 - 0.6 * lunaArriba);
      if (efectiva >= 1) {
        const n = Math.max(1, Math.min(6, Math.round(efectiva / 8))), rx = x(r.az), ry = y(r.alt);
        for (let k = 0; k < n; k++) {
          const azar = (Math.sin(t / 60000 * 12.9898 + k * 78.233) * 43758.5453) % 1, a2 = Math.abs(azar), b2 = Math.abs((azar * 7.31) % 1);
          const sx0 = 8 + a2 * 84, sy0 = 8 + b2 * (H0 - 22), ang = Math.atan2(sy0 - ry, sx0 - rx) / rad;
          estelas += `<i class="estela" style="left:${sx0.toFixed(1)}%;top:${sy0.toFixed(1)}%;--ang:${ang.toFixed(0)}deg;animation-delay:${(k * 60 / n + b2 * 6).toFixed(1)}s"></i>`;
        }
      }
    }
    // Eclipse en curso: la luna se tiñe de cobre o la luna tapa al sol, en la fracción real de ese minuto
    const cub = ev && ev.tipo.startsWith("eclipse") ? cubrimiento(ev, t) : 0;
    const eclipseLuna = ev?.tipo === "eclipse_luna" && cub > 0 ? cub : 0, eclipseSol = ev?.tipo === "eclipse_sol" && cub > 0 ? cub : 0;
    const nube = `rgba(255,255,255,${(0.16 + 0.62 * dia).toFixed(2)})`;
    const monte = rgb(mezcla(mezcla(abajo, [20, 28, 40], 0.55), [6, 9, 14], noche * 0.6));
    el.innerHTML = `<svg viewBox="0 0 100 100" preserveAspectRatio="none" class="cielo-fondo"><defs>
        <linearGradient id="cieloG${uid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${rgb(arriba)}"/><stop offset=".62" stop-color="${rgb(medio)}"/><stop offset="1" stop-color="${rgb(abajo)}"/></linearGradient></defs>
        <rect width="100" height="100" fill="url(#cieloG${uid})"/></svg>
      ${constelaciones ? "" : `<svg viewBox="0 0 100 100" preserveAspectRatio="none" class="cielo-estrellas" style="opacity:${noche}">
        ${estrellas.map(e => `<circle cx="${e.x}" cy="${e.y}" r="${e.r / 4}" style="animation-delay:${e.t}s"/>`).join("")}</svg>`}
      ${completo ? `<svg viewBox="0 0 100 100" preserveAspectRatio="none" class="cielo-hud" style="stroke:${linea}">
        <line x1="2" y1="${H0}" x2="98" y2="${H0}"/>${marcas}
        ${arco.length > 1 ? `<polyline points="${arco.join(" ")}" class="cielo-arco"/>` : ""}</svg>` : ""}
      ${constelaciones}
      ${altS > -6 ? `<span class="astro sol ${sx > 78 ? "izq" : ""}" style="left:${sx}%;top:${sy}%;opacity:${Math.min(1, (altS + 6) / 6)}"><i></i>${eclipseSol ? `<i class="sombra-luna" style="--c:${eclipseSol.toFixed(3)}"></i>` : ""}${completo ? `<b>${Math.round(altS)}°</b>` : ""}</span>` : ""}
      ${altM > -3 && il.fraccion > 0.04 ? `<span class="astro luna ${mx > 78 ? "izq" : ""}" style="left:${mx}%;top:${my}%;opacity:${0.4 + 0.6 * noche};--eclipse:${eclipseLuna.toFixed(3)}"><svg viewBox="-12 -12 24 24">
          <circle r="10" class="luna-sombra"/><path d="${faseLuna(il.fase, 10)}" class="luna-luz"/></svg>${completo ? `<b>${Math.round(il.fraccion * 100)} %</b>` : ""}</span>` : ""}
      ${planetas}${estelas}
      ${eclipseSol > 0.6 ? `<span class="cielo-penumbra" style="opacity:${((eclipseSol - 0.6) * 1.4).toFixed(2)}"></span>` : ""}
      <svg viewBox="0 0 400 60" preserveAspectRatio="none" class="cielo-montes" style="height:${100 - H0 + 6}%">
        <path fill="${monte}" d="M0 60 L0 40 L30 30 L52 36 L84 18 L104 26 L128 14 L150 24 L176 8 L198 22 L226 16 L252 28 L280 12 L306 26 L334 20 L360 30 L384 22 L400 28 L400 60Z" opacity=".55"/>
        <path fill="${monte}" d="M0 60 L0 46 L40 34 L70 42 L112 24 L140 38 L170 30 L214 42 L246 28 L276 40 L318 30 L352 40 L400 34 L400 60Z"/>
        <path fill="${nube}" d="M0 60 L0 48 C30 44 52 50 80 47 C108 44 128 50 160 48 C196 46 214 52 248 49 C280 46 304 51 336 48 C364 46 384 50 400 47 L400 60Z"/>
      </svg>
      ${hud ? `<span class="cielo-lectura">${hora(ahora)}<span>Sol ${Math.round(altS)}° · Luna ${Math.round(il.fraccion * 100)} %</span></span>` : ""}
      ${hud ? `<button type="button" class="cielo-mirar" data-acc="mirarCielo" aria-label="Mirar el cielo" title="Mirar el cielo">${typeof ic === "function" ? ic("cielo360") : ""}</button>` : ""}
      ${hud && contar(ev) ? `<button type="button" class="cielo-evento" data-acc="eventoCielo" title="${contar(ev).linea}" aria-label="${contar(ev).linea}. Toca para saber más"><span>${W >= 560 ? contar(ev).linea : contar(ev).linea.split(" · ")[0]}</span></button>` : ""}`;
    el.title = `Sol ${Math.round(altS)}° · Luna ${Math.round(il.fraccion * 100)} % iluminada`;
  }

  /** Pinta la franja y cada ventana de cielo de la página ([data-cielo]). */
  /**
   * Una sola luz para todo el mundo: la del sol de este momento (de noche, la de la luna, más suave).
   * Las sombras de las hojas caen hacia el lado contrario del astro y se alargan cuando está bajo;
   * al amanecer y al atardecer el papel se calienta apenas (≤ 3 % hacia el oro).
   */
  function luz(ahora = new Date()) {
    const s = posicion(sol, ahora, LAT, LNG), m = posicion(luna, ahora, LAT, LNG);
    const altS = s.alt / rad, astro = altS > -4 ? s : (m.alt > 0 ? m : null);
    const r = document.documentElement.style;
    if (!astro) { r.setProperty("--luz-x", "0px"); r.setProperty("--luz-y", "0px"); r.setProperty("--calidez", "0%"); return; }
    const fuerza = astro === s ? 1 : 0.5, bajo = 1 - Math.max(0, Math.sin(astro.alt));
    r.setProperty("--luz-x", `${(-Math.sin(astro.az) * 3 * fuerza).toFixed(2)}px`);
    r.setProperty("--luz-y", `${(bajo * 3 * fuerza).toFixed(2)}px`);
    r.setProperty("--calidez", `${(Math.max(0, 1 - Math.abs(altS - 4) / 14) * 3).toFixed(2)}%`);
  }
  function pintarTodos() {
    luz();
    document.querySelectorAll(".franja.cielo, [data-cielo]").forEach(el => { el.classList.add("cielo"); pintar(el); });
  }
  /* ---------- Mirar el cielo: una ventana al cielo real según hacia dónde apuntas ----------
     En el teléfono y la tableta se usa la brújula y la inclinación del aparato: se ve la parte del cielo
     que está detrás de la pantalla. En el computador (sin sensores) se arrastra para mirar alrededor y la
     rueda acerca o aleja. Proyección estereográfica, como la de un planisferio; el horizonte siempre nivelado. */
  const CARDINALES = ["N", "NE", "E", "SE", "S", "SO", "O", "NO"];
  /** Rumbo (desde el norte, hacia el este) y altura a los que apunta la parte de atrás del aparato. */
  function apuntado(alfa, beta, gamma) {
    const a = alfa * rad, b = beta * rad, g = gamma * rad;
    const cA = Math.cos(a), sA = Math.sin(a), cB = Math.cos(b), sB = Math.sin(b), cG = Math.cos(g), sG = Math.sin(g);
    const e = -(cG * sA * sB + cA * sG), n = -(sA * sG - cA * cG * sB), u = -cB * cG;
    return { rumbo: (Math.atan2(e, n) / rad + 360) % 360, alt: Math.asin(Math.max(-1, Math.min(1, u))) / rad };
  }
  function abrirVentana() {
    if (document.getElementById("ventanaCielo")) return;
    const v = document.createElement("div");
    v.id = "ventanaCielo"; v.className = "ventana-cielo"; v.setAttribute("role", "dialog"); v.setAttribute("aria-modal", "true"); v.setAttribute("aria-label", "Mirar el cielo");
    v.innerHTML = `<canvas aria-hidden="true"></canvas>
      <div class="vc-arriba"><span class="vc-rumbo" aria-live="polite"></span>
        <button type="button" class="vc-cerrar" aria-label="Cerrar">${typeof ic === "function" ? ic("cerrar") : "×"}</button></div>
      <p class="vc-ayuda"></p>
      <button type="button" class="vc-brujula" hidden>Usar la brújula del teléfono</button>`;
    document.body.append(v);
    const lienzo = v.querySelector("canvas"), ctx = lienzo.getContext("2d"), ayuda = v.querySelector(".vc-ayuda"), rumboTxt = v.querySelector(".vc-rumbo");
    // Mirada inicial: hacia el sur, a 35° sobre el horizonte (la mitad del cielo que mejor se ve desde Colombia)
    const vista = { rumbo: 180, alt: 35, fov: innerWidth < 700 ? 70 : 90 }, meta = { ...vista };
    let sensores = false, cuadro = 0, cache = null, cacheMin = -1;
    const tactil = matchMedia("(pointer: coarse)").matches;
    ayuda.textContent = tactil ? "Mueve el teléfono para mirar el cielo." : "Arrastra para mirar alrededor · la rueda acerca y aleja.";

    // Sensores de orientación (Android: absoluta; iPhone y iPad: con la brújula, previo permiso)
    function alGirar(ev) {
      let alfa = ev.alpha;
      if (typeof ev.webkitCompassHeading === "number") alfa = 360 - ev.webkitCompassHeading;
      else if (!ev.absolute && ev.type !== "deviceorientationabsolute") return;
      if (alfa == null || ev.beta == null || ev.gamma == null) return;
      const p = apuntado(alfa, ev.beta, ev.gamma);
      if (!sensores) { sensores = true; ayuda.textContent = "Apunta con el teléfono: lo que ves es el cielo que está detrás de la pantalla."; v.querySelector(".vc-brujula").hidden = true; }
      meta.rumbo = p.rumbo; meta.alt = Math.max(-20, Math.min(89, p.alt));
    }
    const escuchar = () => { addEventListener("deviceorientationabsolute", alGirar); addEventListener("deviceorientation", alGirar); };
    if (typeof DeviceOrientationEvent !== "undefined" && typeof DeviceOrientationEvent.requestPermission === "function") {
      const b = v.querySelector(".vc-brujula"); b.hidden = false;
      b.onclick = async () => { try { if (await DeviceOrientationEvent.requestPermission() === "granted") escuchar(); else b.textContent = "Sin permiso: arrastra para mirar"; } catch { b.hidden = true; } };
    } else escuchar();

    // Arrastrar, teclado y rueda (siempre disponibles; con sensores, el aparato manda)
    let arrastre = null;
    lienzo.addEventListener("pointerdown", e => { arrastre = { x: e.clientX, y: e.clientY }; lienzo.setPointerCapture(e.pointerId); });
    lienzo.addEventListener("pointermove", e => {
      if (!arrastre || sensores) return;
      const k = vista.fov / lienzo.clientWidth;
      meta.rumbo = (meta.rumbo - (e.clientX - arrastre.x) * k + 360) % 360; meta.alt = Math.max(-20, Math.min(89, meta.alt + (e.clientY - arrastre.y) * k));
      arrastre = { x: e.clientX, y: e.clientY };
    });
    lienzo.addEventListener("pointerup", () => arrastre = null);
    lienzo.addEventListener("wheel", e => { e.preventDefault(); meta.fov = Math.max(35, Math.min(120, meta.fov * (e.deltaY > 0 ? 1.08 : 0.92))); }, { passive: false });
    const teclas = e => {
      if (e.key === "Escape") cerrar();
      const paso = { ArrowLeft: [-10, 0], ArrowRight: [10, 0], ArrowUp: [0, 6], ArrowDown: [0, -6] }[e.key];
      if (paso && !sensores) { meta.rumbo = (meta.rumbo + paso[0] + 360) % 360; meta.alt = Math.max(-20, Math.min(89, meta.alt + paso[1])); }
    };
    addEventListener("keydown", teclas);
    function cerrar() { cancelAnimationFrame(cuadro); removeEventListener("deviceorientationabsolute", alGirar); removeEventListener("deviceorientation", alGirar); removeEventListener("keydown", teclas); v.remove(); }
    v.querySelector(".vc-cerrar").onclick = cerrar;
    v.querySelector(".vc-cerrar").focus();

    const sinMovimiento = matchMedia("(prefers-reduced-motion: reduce)").matches;
    function datosDelMinuto(ahora) {
      const min = Math.floor(ahora.getTime() / 60000);
      if (cache && min === cacheMin) return cache;
      const s = posicion(sol, ahora, LAT, LNG), m = posicion(luna, ahora, LAT, LNG), il = iluminacion(ahora);
      const hz = p => ({ A: (p.az / rad + 180 + 360) % 360, h: p.alt / rad });
      const fijoHz = (ra, dec) => hz(posicion(fijo(ra, dec), ahora, LAT, LNG));
      const ev = eventoDe(ahora), cub = ev && ev.tipo.startsWith("eclipse") ? cubrimiento(ev, ahora.getTime()) : 0;
      const ll = lluviaDe(ahora);
      cache = {
        ahora, sol: hz(s), luna: hz(m), il, ev, cub,
        planetas: PLANETAS.map(([k, nombre]) => ({ nombre, ...hz(posicion(planeta(k), ahora, LAT, LNG)) })),
        estrellas: DATOS.estrellas.map(([ra, dec, mag]) => ({ ...fijoHz(ra, dec), mag })),
        lineas: DATOS.lineas.map(c => c.l.map(seg => seg.map(([ra, dec]) => fijoHz(ra, dec)))),
        nombres: Object.values(DATOS.nombres).map(n => ({ nombre: n.es, ...fijoHz(n.pos[0], n.pos[1]) })),
        lluvia: ll && s.alt / rad < -10 ? { ...ll, radiante: fijoHz(ll.ra, ll.dec) } : null,
        colores: colores(s.alt / rad),
      };
      cacheMin = min;
      return cache;
    }

    function pintarCuadro(tiempo) {
      cuadro = requestAnimationFrame(pintarCuadro);
      // Suavizado de la mirada (el giro más corto) para que el cielo no tiemble
      const dR = ((meta.rumbo - vista.rumbo + 540) % 360) - 180;
      vista.rumbo = (vista.rumbo + dR * 0.18 + 360) % 360; vista.alt += (meta.alt - vista.alt) * 0.18; vista.fov += (meta.fov - vista.fov) * 0.2;
      const dpr = Math.min(2, devicePixelRatio || 1), W = lienzo.clientWidth, H = lienzo.clientHeight;
      if (lienzo.width !== Math.round(W * dpr)) { lienzo.width = Math.round(W * dpr); lienzo.height = Math.round(H * dpr); }
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      const d = datosDelMinuto(new Date());
      const A0 = vista.rumbo * rad, h0 = vista.alt * rad;
      const f = [Math.cos(h0) * Math.sin(A0), Math.cos(h0) * Math.cos(A0), Math.sin(h0)];
      const r = [Math.cos(A0), -Math.sin(A0), 0], u = [-Math.sin(A0) * Math.sin(h0), -Math.cos(A0) * Math.sin(h0), Math.cos(h0)];
      const escala = (W / 2) / (2 * Math.tan(vista.fov * rad / 4));
      /** Pantalla (x, y) de un punto del cielo, o null si está detrás. */
      const proy = (A, h) => {
        const a = A * rad, e = h * rad, vx = Math.cos(e) * Math.sin(a), vy = Math.cos(e) * Math.cos(a), vz = Math.sin(e);
        const pz = vx * f[0] + vy * f[1] + vz * f[2]; if (pz < -0.2) return null;
        const k = 2 / (1 + pz);
        return [W / 2 + k * (vx * r[0] + vy * r[1]) * escala, H / 2 - k * (vx * u[0] + vy * u[1] + vz * u[2]) * escala];
      };
      const altS = d.sol.h, noche = Math.max(0, Math.min(1, (-altS - 2) / 10)), dia = Math.max(0, Math.min(1, (altS + 4) / 14));
      // Cielo: degradado por altura real (cenit arriba, bruma en el horizonte)
      const yHor = proy(vista.rumbo, 0)?.[1] ?? H, yCenit = proy(vista.rumbo, 89.9)?.[1] ?? -H;
      const g = ctx.createLinearGradient(0, Math.min(yCenit, yHor - 1), 0, yHor);
      g.addColorStop(0, rgb(d.colores[0])); g.addColorStop(0.62, rgb(d.colores[1])); g.addColorStop(1, rgb(d.colores[2]));
      ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
      if (d.cub > 0.6 && d.ev.tipo === "eclipse_sol") { ctx.fillStyle = `rgba(6,10,18,${Math.min(1, (d.cub - 0.6) * 1.4)})`; ctx.fillRect(0, 0, W, H); }
      // Constelaciones y estrellas (de noche)
      if (noche > 0.05) {
        ctx.strokeStyle = `rgba(255,255,255,${0.22 * noche})`; ctx.lineWidth = 1; ctx.beginPath();
        for (const c of d.lineas) for (const seg of c) {
          let prev = null;
          for (const p of seg) { const q = p.h > -1 ? proy(p.A, p.h) : null; if (q && prev) { ctx.moveTo(prev[0], prev[1]); ctx.lineTo(q[0], q[1]); } prev = q; }
        }
        ctx.stroke();
        ctx.fillStyle = `rgba(255,255,255,${noche})`;
        for (const s of d.estrellas) { if (s.h < -1) continue; const q = proy(s.A, s.h); if (!q) continue; ctx.beginPath(); ctx.arc(q[0], q[1], 0.6 + (4 - s.mag) * 0.4, 0, 7); ctx.fill(); }
        ctx.font = "400 10px Roboto, system-ui, sans-serif"; ctx.textAlign = "center"; ctx.fillStyle = `rgba(255,255,255,${0.55 * noche})`;
        for (const n of d.nombres) { if (n.h < 3) continue; const q = proy(n.A, n.h); if (q) ctx.fillText(n.nombre.toUpperCase().split("").join(" "), q[0], q[1]); }
      }
      ctx.textAlign = "left"; ctx.font = "400 11px Roboto, system-ui, sans-serif";
      const rotulo = (q, txt, radio) => { ctx.fillStyle = "rgba(255,255,255,.85)"; ctx.fillText(txt.toUpperCase(), q[0] + radio + 6, q[1] + 4); };
      // Planetas visibles
      for (const p of d.planetas) {
        if (p.h < 0) continue; const q = proy(p.A, p.h); if (!q) continue;
        ctx.fillStyle = "#fff"; ctx.beginPath(); ctx.arc(q[0], q[1], 2.6, 0, 7); ctx.fill();
        ctx.strokeStyle = "rgba(255,255,255,.35)"; ctx.beginPath(); ctx.arc(q[0], q[1], 9, 0, 7); ctx.stroke(); rotulo(q, p.nombre, 9);
      }
      // Luna (con su fase y, si hay eclipse, color cobre)
      const ql = d.luna.h > -2 ? proy(d.luna.A, d.luna.h) : null;
      if (ql) {
        const R = 14, ecl = d.ev?.tipo === "eclipse_luna" ? d.cub : 0;
        ctx.save(); ctx.translate(ql[0], ql[1]);
        ctx.fillStyle = "rgba(24,30,44,.6)"; ctx.beginPath(); ctx.arc(0, 0, R, 0, 7); ctx.fill();
        ctx.fillStyle = ecl ? `rgb(${mezcla([238, 241, 244], [168, 80, 44], ecl).join(",")})` : "#EEF1F4";
        ctx.fill(new Path2D(faseLuna(d.il.fase, R)));
        ctx.strokeStyle = "rgba(255,255,255,.32)"; ctx.beginPath(); ctx.arc(0, 0, R + 12, 0, 7); ctx.stroke(); ctx.restore();
        rotulo(ql, `Luna ${Math.round(d.il.fraccion * 100)} %`, R + 12);
      }
      // Sol (tapado en la fracción real si hay eclipse)
      const qs = altS > -4 ? proy(d.sol.A, altS) : null;
      if (qs) {
        const R = 16, halo = ctx.createRadialGradient(qs[0], qs[1], 0, qs[0], qs[1], 60);
        halo.addColorStop(0, "rgba(255,232,180,.55)"); halo.addColorStop(1, "rgba(255,232,180,0)");
        ctx.fillStyle = halo; ctx.beginPath(); ctx.arc(qs[0], qs[1], 60, 0, 7); ctx.fill();
        ctx.fillStyle = "#FFF4D8"; ctx.beginPath(); ctx.arc(qs[0], qs[1], R, 0, 7); ctx.fill();
        if (d.ev?.tipo === "eclipse_sol" && d.cub > 0) { ctx.fillStyle = "#0E1620"; ctx.beginPath(); ctx.arc(qs[0] + (1 - d.cub) * 2 * R, qs[1], R, 0, 7); ctx.fill(); }
        ctx.strokeStyle = "rgba(255,255,255,.45)"; ctx.beginPath(); ctx.arc(qs[0], qs[1], R + 14, 0, 7); ctx.stroke();
        rotulo(qs, `Sol ${Math.round(altS)}°`, R + 14);
      }
      // Lluvia de meteoros: estelas que salen del radiante
      if (d.lluvia && d.lluvia.radiante.h > 0 && !sinMovimiento) {
        const qr = proy(d.lluvia.radiante.A, d.lluvia.radiante.h);
        const lunaArriba = d.luna.h > 0 ? d.il.fraccion : 0, efectiva = d.lluvia.tasa * Math.sin(d.lluvia.radiante.h * rad) * (1 - 0.6 * lunaArriba);
        const n = efectiva >= 1 ? Math.max(1, Math.min(6, Math.round(efectiva / 8))) : 0;
        for (let k = 0; k < n && qr; k++) {
          const ciclo = 12000, t = ((tiempo + k * ciclo / n) % ciclo) / 700; if (t > 1) continue;
          const sem = Math.floor((tiempo + k * ciclo / n) / ciclo) * 7.13 + k, ang = (Math.sin(sem) * 43758.5453 % 1) * Math.PI * 2;
          const dist = 40 + Math.abs(Math.sin(sem * 3.1)) * 140, x0 = qr[0] + Math.cos(ang) * dist, y0 = qr[1] + Math.sin(ang) * dist;
          const x1 = x0 + Math.cos(ang) * 120 * t, y1 = y0 + Math.sin(ang) * 120 * t, alfa = t < 0.3 ? t / 0.3 : 1 - (t - 0.3) / 0.7;
          const gr = ctx.createLinearGradient(x0, y0, x1, y1); gr.addColorStop(0, "rgba(255,255,255,0)"); gr.addColorStop(1, `rgba(255,255,255,${alfa})`);
          ctx.strokeStyle = gr; ctx.lineWidth = 1.5; ctx.beginPath(); ctx.moveTo(x0, y0); ctx.lineTo(x1, y1); ctx.stroke(); ctx.lineWidth = 1;
        }
      }
      // Suelo: la cordillera alrededor (360°) y el mar de nubes, con perspectiva atmosférica
      const monte = mezcla(mezcla(d.colores[2], [20, 28, 40], 0.55), [6, 9, 14], noche * 0.6);
      for (const [capa, alto, alfa] of [[0, 6, 0.55], [1, 3.4, 1]]) {
        ctx.fillStyle = `rgba(${monte.join(",")},${alfa})`; ctx.beginPath(); let empezado = false;
        for (let A = vista.rumbo - vista.fov; A <= vista.rumbo + vista.fov; A += 1.5) {
          const cresta = alto * (0.55 + 0.45 * Math.sin(A * rad * (capa ? 9 : 6) + capa * 2) * Math.cos(A * rad * (capa ? 4 : 3) + 1));
          const q = proy((A + 360) % 360, cresta); if (!q) continue;
          if (!empezado) { ctx.moveTo(q[0], H + 10); empezado = true; } ctx.lineTo(q[0], q[1]);
        }
        ctx.lineTo(W + 10, H + 10); ctx.closePath(); ctx.fill();
      }
      // Bajo el horizonte: un mar de nubes claro junto a las cumbres que se apaga hacia el valle
      const qn = proy(vista.rumbo, 0.6);
      if (qn && qn[1] < H) {
        const suelo = ctx.createLinearGradient(0, qn[1], 0, Math.max(qn[1] + 1, H));
        suelo.addColorStop(0, `rgba(255,255,255,${(0.14 + 0.5 * dia).toFixed(2)})`);
        suelo.addColorStop(0.12, `rgba(${mezcla(monte, [255, 255, 255], 0.06 + 0.4 * dia).join(",")},.96)`);
        suelo.addColorStop(1, `rgba(${mezcla(monte, [0, 0, 0], 0.35).join(",")},1)`);
        ctx.fillStyle = suelo; ctx.fillRect(0, qn[1], W, H - qn[1]);
      }
      // Horizonte con marcas cada 15° y los puntos cardinales
      ctx.strokeStyle = `rgba(255,255,255,${0.3 + 0.2 * noche})`; ctx.fillStyle = "rgba(255,255,255,.85)"; ctx.textAlign = "center";
      ctx.font = "400 11px Roboto, system-ui, sans-serif";
      for (let A = 0; A < 360; A += 15) {
        const q = proy(A, 0); if (!q || q[0] < -20 || q[0] > W + 20) continue;
        const card = A % 45 === 0;
        ctx.beginPath(); ctx.moveTo(q[0], q[1]); ctx.lineTo(q[0], q[1] - (card ? 10 : 5)); ctx.stroke();
        if (card) ctx.fillText(CARDINALES[A / 45], q[0], q[1] - 16);
      }
      // Si el sol, la luna o el radiante de la lluvia no están a la vista, una flecha en el borde dice hacia dónde mirar
      const guia = (A, h, txt) => {
        const q = proy(A, h); if (q && q[0] > 30 && q[0] < W - 30 && q[1] > 70 && q[1] < H - 60) return;
        // Hacia dónde girar: diferencia de rumbo (izquierda/derecha) y de altura (arriba/abajo)
        const dA = ((A - vista.rumbo + 540) % 360) - 180, dH = h - vista.alt, ang = Math.atan2(-dH, dA);
        const mx = W / 2 - 34, my = H / 2 - 70, k = Math.min(mx / Math.abs(Math.cos(ang) || 1e-6), my / Math.abs(Math.sin(ang) || 1e-6));
        const x = W / 2 + Math.cos(ang) * k, y = H / 2 + 8 + Math.sin(ang) * k;
        ctx.save(); ctx.translate(x, y); ctx.rotate(ang); ctx.fillStyle = "rgba(255,255,255,.85)";
        ctx.beginPath(); ctx.moveTo(9, 0); ctx.lineTo(-5, -6); ctx.lineTo(-5, 6); ctx.closePath(); ctx.fill(); ctx.restore();
        ctx.textAlign = "center"; ctx.font = "400 10.5px Roboto, system-ui, sans-serif"; ctx.fillText(txt.toUpperCase(), x - Math.cos(ang) * 22, y - Math.sin(ang) * 18 + 4);
      };
      if (altS > -4) guia(d.sol.A, altS, "Sol");
      if (d.luna.h > -2) guia(d.luna.A, d.luna.h, "Luna");
      if (d.lluvia && d.lluvia.radiante.h > 0) guia(d.lluvia.radiante.A, d.lluvia.radiante.h, "Lluvia");
      rumboTxt.textContent = `${CARDINALES[Math.round(vista.rumbo / 45) % 8]} ${Math.round(vista.rumbo)}° · ${Math.round(vista.alt)}° de altura`;
    }
    cuadro = requestAnimationFrame(pintarCuadro);
  }

  function iniciar() {
    document.querySelector(".franja")?.classList.add("cielo");
    pintarTodos();
    setInterval(pintarTodos, 60000);
  }
  return { iniciar, pintarTodos, pintar, faseLuna, abrirVentana, apuntado, eventoDe, lluviaDe, cubrimiento, contar, iluminacion, posicion: (f, lat = LAT, lng = LNG) => ({ sol: posicion(sol, f, lat, lng), luna: posicion(luna, f, lat, lng), ...Object.fromEntries(PLANETAS.map(([k]) => [k, posicion(planeta(k), f, lat, lng)])) }) };
})();
window.Cielo = Cielo;
Cielo.iniciar();
