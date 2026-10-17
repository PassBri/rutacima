"use strict";
/* ======================================================================
 * Arte de Ruta a la Cima: la cordillera andina dibujada en código.
 * Misma matemática que la app (ui/components/Arte.kt): la misma semilla da el mismo paisaje.
 * Crestas fractales (desplazamiento del punto medio), 5 capas con perspectiva atmosférica, nieve
 * en las cumbres lejanas, bruma entre capas, el sol o la luna del momento y la bandera en la cima.
 * ====================================================================== */
const Arte = (() => {
  /** FNV-1a de 32 bits sobre las unidades UTF-16 del texto. */
  function semillaDe(texto) {
    let h = 0x811c9dc5;
    for (let i = 0; i < texto.length; i++) { h ^= texto.charCodeAt(i); h = Math.imul(h, 0x01000193); }
    return h >>> 0;
  }
  /** mulberry32: números entre 0 y 1, reproducibles. */
  function azar(semilla) {
    let a = semilla | 0;
    return () => {
      a = (a + 0x6D2B79F5) | 0;
      let t = Math.imul(a ^ (a >>> 15), a | 1);
      t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  }
  const BASE = [0.42, 0.52, 0.62, 0.72, 0.84], AMP = [0.20, 0.22, 0.24, 0.22, 0.16], N = 64;
  /** Las 5 crestas (de lejos a cerca), cada una con 65 alturas entre 0 y 1 (fracción del alto). */
  function crestas(texto) {
    const r = azar(semillaDe(texto));
    return BASE.map((base, k) => {
      const y = new Array(N + 1).fill(0);
      let amp = AMP[k];
      y[0] = base + (r() - 0.5) * amp; y[N] = base + (r() - 0.5) * amp;
      for (let paso = N; paso > 1; paso /= 2) {
        for (let i = 0; i < N; i += paso) y[i + paso / 2] = (y[i] + y[i + paso]) / 2 + (r() - 0.5) * amp;
        amp *= 0.55;
      }
      return y;
    }).concat([[r(), r()]]);                    // posición del astro (x, altura relativa)
  }
  const MOMENTOS = [
    { cielo: ["#2C3A62", "#EEAC7A"], lejos: "#9A8FA6", cerca: "#2A2F45", astro: "#FFE2A6" },
    { cielo: ["#4E84BA", "#DEE9EE"], lejos: "#A9BFCF", cerca: "#33485A", astro: "#FFF4D8" },
    { cielo: ["#3B2A4A", "#E8946A"], lejos: "#B07A78", cerca: "#2B1E2A", astro: "#FFC27A" },
    { cielo: ["#060A14", "#1E283C"], lejos: "#2A3550", cerca: "#0A0F1A", astro: "#EEF1F4", noche: true },
  ];
  const hex = c => [1, 3, 5].map(i => parseInt(c.slice(i, i + 2), 16));
  const mix = (a, b, t) => a.map((x, i) => Math.round(x + (b[i] - x) * t));
  /** Color de la capa k (0 lejos … 4 cerca): de la montaña lejana a la cercana, y la bruma del horizonte encima de lo lejano. */
  const colorCapa = (m, k) => { const t = k / 4; return mix(hex(m.cielo[1]), mix(hex(m.lejos), hex(m.cerca), Math.pow(t, 1.2)), 0.45 + 0.55 * t); };
  const rgbTxt = c => `rgb(${c.join(",")})`;
  const cache = new Map();
  /**
   * Paisaje en SVG (viewBox 400×240, llena su caja recortando). [momento] 0 amanecer, 1 día,
   * 2 atardecer, 3 noche; por defecto sale de la semilla. [detalle] false en tarjetas pequeñas.
   */
  function cordillera(texto, momento = null, { detalle = true, bandera = true } = {}) {
    const clave = `${texto}|${momento}|${detalle}|${bandera}`;
    if (cache.has(clave)) return cache.get(clave);
    const cs = crestas(String(texto)), astro = cs.pop();
    const m = MOMENTOS[(momento ?? semillaDe(String(texto)) % 4) % 4];
    const W = 400, H = 240, id = "a" + semillaDe(clave).toString(36);
    const camino = y => `M0 ${H}` + y.map((v, i) => `L${(i * W / N).toFixed(1)} ${(v * H).toFixed(1)}`).join("") + `L${W} ${H}Z`;
    let capas = "";
    cs.forEach((y, k) => {
      capas += `<path d="${camino(y)}" fill="${rgbTxt(colorCapa(m, k))}"/>`;
      // Nieve en las cumbres lejanas (capas 1 y 2): la cresta por encima de la línea de nieve
      if (detalle && (k === 1 || k === 2)) {
        const linea = (BASE[k] - AMP[k] * 0.3) * H;
        capas += `<clipPath id="${id}n${k}"><rect width="${W}" height="${linea.toFixed(1)}"/></clipPath><path d="${camino(y)}" fill="#F4F7F9" opacity="${m.noche ? 0.35 : 0.85}" clip-path="url(#${id}n${k})"/>`;
      }
      // Bruma entre capas
      if (detalle && (k === 2 || k === 3)) {
        const yb = BASE[k + 1] * H - 18;
        capas += `<rect y="${yb.toFixed(1)}" width="${W}" height="28" fill="url(#${id}b)"/>`;
      }
    });
    // Bandera en la cima más alta de la capa 3
    let flag = "";
    if (bandera) {
      const y3 = cs[3]; let im = 0; y3.forEach((v, i) => { if (v < y3[im]) im = i; });
      const x = im * W / N, y = y3[im] * H;
      if (x > W * 0.1 && x < W * 0.9) flag = `<line x1="${x.toFixed(1)}" y1="${y.toFixed(1)}" x2="${x.toFixed(1)}" y2="${(y - 22).toFixed(1)}" stroke="#F5EED9" stroke-width="1.4"/>
        <path d="M${x.toFixed(1)} ${(y - 22).toFixed(1)}l13 4-13 5z" fill="#B8862F" stroke="#6B2A1A" stroke-width=".7"/>`;
    }
    const ax = 40 + astro[0] * 320, ay = 30 + astro[1] * 50;
    let estrellas = "";
    if (m.noche && detalle) { const r = azar(semillaDe(texto + "*")); for (let i = 0; i < 40; i++) estrellas += `<circle cx="${(r() * W).toFixed(1)}" cy="${(r() * H * 0.5).toFixed(1)}" r="${(0.4 + r() * 0.7).toFixed(2)}" fill="#fff" opacity="${(0.4 + r() * 0.5).toFixed(2)}"/>`; }
    const svg = `<svg class="arte-cordillera" viewBox="0 0 ${W} ${H}" preserveAspectRatio="xMidYMax slice" aria-hidden="true" focusable="false"><defs>
      <linearGradient id="${id}c" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${m.cielo[0]}"/><stop offset=".75" stop-color="${m.cielo[1]}"/></linearGradient>
      <linearGradient id="${id}b" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${m.cielo[1]}" stop-opacity="0"/><stop offset=".5" stop-color="${m.cielo[1]}" stop-opacity=".55"/><stop offset="1" stop-color="${m.cielo[1]}" stop-opacity="0"/></linearGradient>
      <radialGradient id="${id}h"><stop offset="0" stop-color="${m.astro}" stop-opacity=".55"/><stop offset="1" stop-color="${m.astro}" stop-opacity="0"/></radialGradient></defs>
      <rect width="${W}" height="${H}" fill="url(#${id}c)"/>${estrellas}
      <circle cx="${ax.toFixed(1)}" cy="${ay.toFixed(1)}" r="34" fill="url(#${id}h)"/><circle cx="${ax.toFixed(1)}" cy="${ay.toFixed(1)}" r="${m.noche ? 9 : 12}" fill="${m.astro}"/>
      ${capas}${flag}</svg>`;
    cache.set(clave, svg);
    return svg;
  }
  return { cordillera, crestas, semillaDe, azar, colorCapa, MOMENTOS };
})();
window.Arte = Arte;
