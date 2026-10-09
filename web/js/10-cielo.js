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

  /** Colores del cielo según la altura del sol (en grados): noche, alba, día. */
  const mezcla = (a, b, t) => a.map((x, i) => Math.round(x + (b[i] - x) * t));
  const rgb = c => `rgb(${c.join(",")})`;
  const NOCHE = [[12, 18, 40], [36, 22, 40]], ALBA = [[64, 74, 128], [233, 138, 86]], DIA = [[118, 168, 214], [222, 206, 178]];
  function colores(alt) {
    if (alt <= -10) return NOCHE;
    if (alt <= 2) { const t = (alt + 10) / 12; return [mezcla(NOCHE[0], ALBA[0], t), mezcla(NOCHE[1], ALBA[1], t)]; }
    if (alt <= 14) { const t = (alt - 2) / 12; return [mezcla(ALBA[0], DIA[0], t), mezcla(ALBA[1], DIA[1], t)]; }
    return DIA;
  }

  /** Forma iluminada de la luna (camino SVG centrado en 0,0) según su fase (0 nueva, .5 llena). */
  function faseLuna(fase, r) {
    const crece = fase < 0.5, rx = Math.abs(Math.cos(2 * Math.PI * fase)) * r;
    const lado = crece ? 1 : 0;                       // arco del borde iluminado: derecha al crecer
    const giba = (fase > 0.25 && fase < 0.75) ? lado : 1 - lado;
    return `M0 ${-r} A${r} ${r} 0 0 ${lado} 0 ${r} A${rx} ${r} 0 0 ${giba} 0 ${-r}Z`;
  }

  const estrellas = Array.from({ length: 46 }, (_, i) => ({ x: (i * 97.3) % 100, y: (i * 37.7) % 62, r: 0.5 + (i % 3) * 0.35, t: (i % 7) * 0.6 }));

  function pintar(el, ahora = new Date()) {
    const s = posicion(sol, ahora, LAT, LNG), m = posicion(luna, ahora, LAT, LNG), il = iluminacion(ahora);
    const altS = s.alt / rad, altM = m.alt / rad;
    const [arriba, abajo] = colores(altS);
    const noche = Math.max(0, Math.min(1, (-altS - 2) / 10));
    // Este (azimut -90°) a la izquierda, oeste (+90°) a la derecha; la altura sube hacia arriba
    const x = az => 50 + Math.sin(az) * 46, y = alt => 92 - Math.max(-10, alt / rad) / 75 * 80;
    const sx = x(s.az), sy = y(s.alt), mx = x(m.az), my = y(m.alt);
    el.innerHTML = `<svg viewBox="0 0 100 100" preserveAspectRatio="none" class="cielo-fondo"><defs>
        <linearGradient id="cieloG" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${rgb(arriba)}"/><stop offset="1" stop-color="${rgb(abajo)}"/></linearGradient></defs>
        <rect width="100" height="100" fill="url(#cieloG)"/></svg>
      <svg viewBox="0 0 100 100" preserveAspectRatio="none" class="cielo-estrellas" style="opacity:${noche}">
        ${estrellas.map(e => `<circle cx="${e.x}" cy="${e.y}" r="${e.r / 4}" style="animation-delay:${e.t}s"/>`).join("")}</svg>
      ${altS > -6 ? `<div class="astro sol" style="left:${sx}%;top:${sy}%;opacity:${Math.min(1, (altS + 6) / 6)}"></div>` : ""}
      ${altM > -3 && il.fraccion > 0.04 ? `<svg class="astro luna" viewBox="-12 -12 24 24" style="left:${mx}%;top:${my}%;opacity:${0.35 + 0.65 * noche}">
          <circle r="10" class="luna-sombra"/><path d="${faseLuna(il.fase, 10)}" class="luna-luz"/></svg>` : ""}
      <svg viewBox="0 0 400 40" preserveAspectRatio="none" class="cielo-montes"><path d="M0 40 L0 26 L40 14 L70 22 L110 6 L150 20 L190 12 L230 24 L270 8 L310 18 L350 10 L400 22 L400 40Z"/></svg>`;
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
  return { iniciar, pintarTodos, pintar, faseLuna, iluminacion, posicion: (f, lat = LAT, lng = LNG) => ({ sol: posicion(sol, f, lat, lng), luna: posicion(luna, f, lat, lng) }) };
})();
window.Cielo = Cielo;
Cielo.iniciar();
