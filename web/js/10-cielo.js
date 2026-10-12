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
    const planetas = completo ? PLANETAS.map(([k, nombre, umbral]) => {
      const p = posicion(planeta(k), ahora, LAT, LNG), alt = p.alt / rad;
      if (alt < 2 || altS > umbral) return "";
      const px = x(p.az), py = y(p.alt), bajoLectura = hud && px < 36 && py < 34;
      return `<span class="astro planeta ${px > 78 ? "izq" : ""}" style="left:${px}%;top:${py}%" title="${nombre}"><i></i>${bajoLectura ? "" : `<b>${nombre}</b>`}</span>`;
    }).join("") : "";
    const nube = `rgba(255,255,255,${(0.16 + 0.62 * dia).toFixed(2)})`;
    const monte = rgb(mezcla(mezcla(abajo, [20, 28, 40], 0.55), [6, 9, 14], noche * 0.6));
    el.innerHTML = `<svg viewBox="0 0 100 100" preserveAspectRatio="none" class="cielo-fondo"><defs>
        <linearGradient id="cieloG${uid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${rgb(arriba)}"/><stop offset=".62" stop-color="${rgb(medio)}"/><stop offset="1" stop-color="${rgb(abajo)}"/></linearGradient></defs>
        <rect width="100" height="100" fill="url(#cieloG${uid})"/></svg>
      <svg viewBox="0 0 100 100" preserveAspectRatio="none" class="cielo-estrellas" style="opacity:${noche}">
        ${estrellas.map(e => `<circle cx="${e.x}" cy="${e.y}" r="${e.r / 4}" style="animation-delay:${e.t}s"/>`).join("")}</svg>
      ${completo ? `<svg viewBox="0 0 100 100" preserveAspectRatio="none" class="cielo-hud" style="stroke:${linea}">
        <line x1="2" y1="${H0}" x2="98" y2="${H0}"/>${marcas}
        ${arco.length > 1 ? `<polyline points="${arco.join(" ")}" class="cielo-arco"/>` : ""}</svg>` : ""}
      ${altS > -6 ? `<span class="astro sol ${sx > 78 ? "izq" : ""}" style="left:${sx}%;top:${sy}%;opacity:${Math.min(1, (altS + 6) / 6)}"><i></i>${completo ? `<b>${Math.round(altS)}°</b>` : ""}</span>` : ""}
      ${altM > -3 && il.fraccion > 0.04 ? `<span class="astro luna ${mx > 78 ? "izq" : ""}" style="left:${mx}%;top:${my}%;opacity:${0.4 + 0.6 * noche}"><svg viewBox="-12 -12 24 24">
          <circle r="10" class="luna-sombra"/><path d="${faseLuna(il.fase, 10)}" class="luna-luz"/></svg>${completo ? `<b>${Math.round(il.fraccion * 100)} %</b>` : ""}</span>` : ""}
      ${planetas}
      <svg viewBox="0 0 400 60" preserveAspectRatio="none" class="cielo-montes" style="height:${100 - H0 + 6}%">
        <path fill="${monte}" d="M0 60 L0 40 L30 30 L52 36 L84 18 L104 26 L128 14 L150 24 L176 8 L198 22 L226 16 L252 28 L280 12 L306 26 L334 20 L360 30 L384 22 L400 28 L400 60Z" opacity=".55"/>
        <path fill="${monte}" d="M0 60 L0 46 L40 34 L70 42 L112 24 L140 38 L170 30 L214 42 L246 28 L276 40 L318 30 L352 40 L400 34 L400 60Z"/>
        <path fill="${nube}" d="M0 60 L0 48 C30 44 52 50 80 47 C108 44 128 50 160 48 C196 46 214 52 248 49 C280 46 304 51 336 48 C364 46 384 50 400 47 L400 60Z"/>
      </svg>
      ${hud ? `<span class="cielo-lectura">${hora(ahora)}<span>Sol ${Math.round(altS)}° · Luna ${Math.round(il.fraccion * 100)} %</span></span>` : ""}`;
    el.title = `Sol ${Math.round(altS)}° · Luna ${Math.round(il.fraccion * 100)} % iluminada`;
  }

  /** Pinta la franja y cada ventana de cielo de la página ([data-cielo]). */
  function pintarTodos() {
    document.querySelectorAll(".franja.cielo, [data-cielo]").forEach(el => { el.classList.add("cielo"); pintar(el); });
  }
  function iniciar() {
    document.querySelector(".franja")?.classList.add("cielo");
    pintarTodos();
    setInterval(pintarTodos, 60000);
  }
  return { iniciar, pintarTodos, pintar, faseLuna, iluminacion, posicion: (f, lat = LAT, lng = LNG) => ({ sol: posicion(sol, f, lat, lng), luna: posicion(luna, f, lat, lng), ...Object.fromEntries(PLANETAS.map(([k]) => [k, posicion(planeta(k), f, lat, lng)])) }) };
})();
window.Cielo = Cielo;
Cielo.iniciar();
