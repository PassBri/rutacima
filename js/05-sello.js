"use strict";
/* ======================================================================
 * Sello de cera que se rompe de verdad (misma receta que RoturaSello.kt: con la misma semilla,
 * el sello se parte igual en el teléfono y en el computador)
 * ====================================================================== */
/** java.util.Random, para que los trozos salgan idénticos a los de la app. */
class AzarJava {
  constructor(semilla) { this.s = (BigInt(semilla) ^ 0x5DEECE66Dn) & ((1n << 48n) - 1n); }
  next(bits) { this.s = (this.s * 0x5DEECE66Dn + 0xBn) & ((1n << 48n) - 1n); return Number(BigInt.asIntN(32, this.s >> BigInt(48 - bits))); }
  nextFloat() { return this.next(24) / (1 << 24); }
  nextBoolean() { return this.next(1) !== 0; }
  nextInt(n) {
    if ((n & -n) === n) return Number((BigInt(n) * BigInt(this.next(31))) >> 31n);
    let bits, val;
    do { bits = this.next(31); val = bits % n; } while (((bits - val + (n - 1)) | 0) < 0);
    return val;
  }
}
const RoturaSello = {
  FIN_GRIETAS: 0.2, GRAVEDAD: 2.2,
  generar(semilla, piezas = 6, migas = 18) {
    const r = new AzarJava(semilla), azar = (a, b) => a + r.nextFloat() * (b - a), PI2 = 2 * Math.PI;
    const impacto = { x: azar(-0.18, 0.18), y: azar(-0.18, 0.18) };
    const base = azar(0, PI2);
    const angulos = Array.from({ length: piezas }, (_, i) => base + (PI2 / piezas) * (i + azar(-0.32, 0.32))).sort((a, b) => a - b);
    const grieta = (ang, largo, desde, quiebres) => {
      const pts = [desde];
      for (let k = 1; k <= quiebres; k++) { const t = k / quiebres, a = ang + (k < quiebres ? azar(-0.16, 0.16) : 0); pts.push({ x: desde.x + Math.cos(a) * largo * t, y: desde.y + Math.sin(a) * largo * t }); }
      return pts;
    };
    const grietas = angulos.map(a => grieta(a, 1.7, impacto, 5));
    const finas = Array.from({ length: 4 }, () => {
      const g = grietas[r.nextInt(grietas.length)], desde = g[1 + r.nextInt(2)];
      return grieta(Math.atan2(desde.y - impacto.y, desde.x - impacto.x) + azar(-0.9, 0.9), azar(0.18, 0.38), desde, 3);
    });
    const fragmentos = angulos.map((a1, i) => {
      const a2 = i + 1 < angulos.length ? angulos[i + 1] : angulos[0] + PI2;
      const arco = Array.from({ length: 7 }, (_, k) => { const a = a1 + (a2 - a1) * k / 6; return { x: impacto.x + Math.cos(a) * 1.7, y: impacto.y + Math.sin(a) * 1.7 }; });
      const siguiente = grietas[(i + 1) % grietas.length];
      const contorno = [...grietas[i], ...arco, ...[...siguiente].reverse()];
      const recortar = q => { const d = Math.hypot(q.x, q.y); return d <= 0.95 ? q : { x: q.x / d * 0.95, y: q.y / d * 0.95 }; };
      const arcoCara = Array.from({ length: 15 }, (_, k) => { const a = a1 + (a2 - a1) * k / 14; return { x: impacto.x + Math.cos(a) * 1.7, y: impacto.y + Math.sin(a) * 1.7 }; });
      const cara = [...grietas[i], ...arcoCara, ...[...siguiente].reverse()].map(recortar);
      const filos = [grietas[i], siguiente].map(g => g.map(recortar));
      const medio = (a1 + a2) / 2, dir = { x: Math.cos(medio), y: Math.sin(medio) };
      const ancho = (a2 - a1) / (PI2 / piezas);
      const velocidad = azar(0.75, 1.15) / Math.min(1.6, Math.max(0.6, ancho));
      const giro = azar(70, 220) * (r.nextBoolean() ? 1 : -1);
      return { contorno, cara, filos, direccion: dir, velocidad, giro, volteo: azar(0.15, 0.55), pivote: { x: impacto.x + dir.x * 0.55, y: impacto.y + dir.y * 0.55 } };
    });
    const ms = Array.from({ length: migas }, () => {
      const a = azar(0, PI2), d = azar(0.05, 0.75);
      return { origen: { x: impacto.x + Math.cos(a) * d, y: impacto.y + Math.sin(a) * d }, direccion: { x: Math.cos(a), y: Math.sin(a) - 0.35 },
        velocidad: azar(0.6, 1.6), tamano: azar(0.025, 0.07), giro: azar(-360, 360) };
    });
    return { impacto, grietas, finas, fragmentos, migas: ms };
  },
  grietasEn: t => { const x = Math.min(1, Math.max(0, t / 0.2)); return 1 - (1 - x) * (1 - x); },
  vuelo: t => Math.min(1, Math.max(0, (t - 0.2) / 0.8)),
  temblor: t => { if (t <= 0 || t >= 0.2) return { x: 0, y: 0 }; const f = 1 - t / 0.2; return { x: Math.sin(t * 210) * 0.025 * f, y: Math.cos(t * 170) * 0.018 * f }; },
  suave: (a, b, x) => { const t = Math.min(1, Math.max(0, (x - a) / (b - a))); return t * t * (3 - 2 * t); },
  pose(f, u) {
    const salto = f.velocidad * (1 - (1 - u) * (1 - u)) * 0.8;
    return { dx: f.direccion.x * salto, dy: f.direccion.y * salto + 2.2 * u * u, grados: f.giro * u, escalaX: Math.cos(f.volteo * Math.PI * u),
      alfa: 1 - this.suave(0.62, 1, u), altura: Math.sin(Math.PI * Math.min(u, 0.5)) };
  },
  poseMiga(m, u) {
    const salto = m.velocidad * (1 - (1 - u) * (1 - u)) * 0.9;
    return { dx: m.direccion.x * salto, dy: m.direccion.y * salto + 2.2 * 1.2 * u * u, grados: m.giro * u, alfa: 1 - this.suave(0.5, 0.95, u) };
  },
  /** Crujido de la cera (mismas capas que en la app: chasquido, golpe sordo y crujidos). */
  sonido(muestreo, semilla = 7) {
    const r = new AzarJava(semilla), n = Math.floor(muestreo * 0.32), s = new Float32Array(n);
    const chasquido = (inicio, fuerza, caida) => {
      const i0 = Math.floor(inicio * muestreo); let previo = 0;
      for (let i = i0; i < n; i++) {
        const env = Math.exp(-((i - i0) / muestreo) / caida); if (env < 0.001) break;
        const ruido = r.nextFloat() * 2 - 1; s[i] += (ruido - previo * 0.85) * env * fuerza; previo = ruido;
      }
    };
    for (let i = 0; i < n; i++) { const t = i / muestreo; s[i] += Math.sin(2 * Math.PI * 165 * t) * Math.exp(-t / 0.028) * 0.35; }
    chasquido(0, 0.95, 0.004); chasquido(0.006, 0.55, 0.007);
    for (let k = 0; k < 9; k++) chasquido(0.02 + r.nextFloat() * 0.2, 0.12 + r.nextFloat() * 0.4, 0.0015 + r.nextFloat() * 0.003);
    let pico = 1e-6; for (const v of s) pico = Math.max(pico, Math.abs(v));
    for (let i = 0; i < n; i++) s[i] = s[i] / pico * 0.8 * (i > n - 600 ? (n - i) / 600 : 1);
    return s;
  },
};

const DURACION_ROTURA = 1500;
/**
 * Rompe el sello [img]: dibuja la animación en una capa fija sobre la página (así sigue aunque la
 * pantalla se redibuje al guardar) y llama a [alPartirse] cuando los trozos ya van volando.
 */
function romperSello(img, semilla, alPartirse) {
  const caja = img.getBoundingClientRect();
  const lado = caja.width, dpr = Math.min(2, devicePixelRatio || 1);
  const margen = lado * 2.2;   // espacio para que los trozos vuelen y caigan
  const cv = document.createElement("canvas");
  cv.className = "capa-sello";
  Object.assign(cv.style, { left: caja.left - margen + "px", top: caja.top - margen * 0.7 + "px", width: lado + margen * 2 + "px", height: lado + margen * 2.3 + "px" });
  cv.width = Math.round((lado + margen * 2) * dpr); cv.height = Math.round((lado + margen * 2.3) * dpr);
  document.body.appendChild(cv);
  img.style.visibility = "hidden";
  const ctx = cv.getContext("2d"), R = lado / 2, cx = margen + R, cy = margen * 0.7 + R;
  const rot = RoturaSello.generar(semilla);
  const reducido = matchMedia("(prefers-reduced-motion: reduce)").matches;
  sonarCrujido();
  if (navigator.vibrate) try { navigator.vibrate([12, 30, 8]); } catch {}
  const camino = (pts, X, Y) => { ctx.beginPath(); pts.forEach((p, i) => ctx[i ? "lineTo" : "moveTo"](X(p.x), Y(p.y))); };
  const trazoParcial = (pts, prop) => {
    // dibuja la grieta hasta [prop] de su largo
    const largos = pts.slice(1).map((p, i) => Math.hypot(p.x - pts[i].x, p.y - pts[i].y)), total = largos.reduce((a, b) => a + b, 0);
    let resto = total * prop; ctx.beginPath(); ctx.moveTo(pts[0].x * R, pts[0].y * R);
    for (let i = 0; i < largos.length && resto > 0; i++) {
      const f = Math.min(1, resto / largos[i]), a = pts[i], b = pts[i + 1];
      ctx.lineTo((a.x + (b.x - a.x) * f) * R, (a.y + (b.y - a.y) * f) * R); resto -= largos[i];
    }
    ctx.stroke();
  };
  const grietas = g => {
    ctx.save(); ctx.beginPath(); ctx.arc(0, 0, R * 0.96, 0, 2 * Math.PI); ctx.clip();
    ctx.lineCap = "round"; ctx.lineJoin = "round";
    for (const [color, ancho, dx] of [["rgba(255,205,185,.35)", R * 0.022, -R * 0.012], ["rgba(25,4,2,.9)", R * 0.03, 0]]) {
      ctx.strokeStyle = color; ctx.lineWidth = ancho; ctx.save(); ctx.translate(dx, dx);
      rot.grietas.forEach(p => trazoParcial(p, g));
      if (g > 0.45) { ctx.lineWidth = ancho * 0.55; rot.finas.forEach(p => trazoParcial(p, (g - 0.45) / 0.55)); }
      ctx.restore();
    }
    ctx.restore();
  };
  const inicio = performance.now();
  let avisado = false;
  const cuadro = () => {
    const t = reducido ? 1 : Math.min(1, (performance.now() - inicio) / DURACION_ROTURA);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0); ctx.clearRect(0, 0, cv.width, cv.height);
    ctx.translate(cx, cy);
    if (t < RoturaSello.FIN_GRIETAS) {
      const tb = RoturaSello.temblor(t);
      ctx.save(); ctx.translate(tb.x * R, tb.y * R);
      ctx.drawImage(img, -R, -R, lado, lado);
      grietas(RoturaSello.grietasEn(t));
      ctx.restore();
    } else {
      if (!avisado) { avisado = true; alPartirse?.(); }
      const u = RoturaSello.vuelo(t);
      for (const f of rot.fragmentos) {
        const p = RoturaSello.pose(f, u); if (p.alfa <= 0) continue;
        const X = x => (x - f.pivote.x) * R, Y = y => (y - f.pivote.y) * R;
        ctx.save();
        ctx.globalAlpha = p.alfa;
        // Sombra sobre el papel: más lejos y más suave mientras el trozo está en el aire
        ctx.save();
        ctx.translate((f.pivote.x + p.dx) * R + R * (0.04 + 0.12 * p.altura), (f.pivote.y + p.dy) * R + R * (0.07 + 0.18 * p.altura));
        ctx.rotate(p.grados * Math.PI / 180); ctx.scale(p.escalaX, 1);
        ctx.filter = `blur(${2 + 6 * p.altura}px)`; ctx.fillStyle = `rgba(70,18,10,${0.32 - 0.12 * p.altura})`; camino(f.cara, X, Y); ctx.fill();
        ctx.restore();
        // El trozo: canto oscuro (grosor de la cera) y encima la cara del sello
        ctx.translate((f.pivote.x + p.dx) * R, (f.pivote.y + p.dy) * R);
        ctx.rotate(p.grados * Math.PI / 180); ctx.scale(p.escalaX, 1);
        const canto = R * 0.05 * (0.4 + Math.abs(Math.sin(f.volteo * Math.PI * u)) * 1.4);
        camino(f.cara.map(q => ({ x: q.x, y: q.y + canto / R })), X, Y); ctx.fillStyle = "#360a05"; ctx.fill();   // canto: el grosor de la cera
        ctx.save(); camino(f.contorno, X, Y); ctx.clip();
        if (p.escalaX < 0) { camino(f.cara, X, Y); ctx.fillStyle = "#6a1a10"; ctx.fill(); }   // el revés de la cera, liso
        else ctx.drawImage(img, -f.pivote.x * R - R, -f.pivote.y * R - R, lado, lado);
        ctx.restore();
        // Filo de la fractura: oscuro con un brillo fino
        ctx.lineCap = "round"; ctx.lineJoin = "round";
        for (const [color, ancho] of [["rgba(20,3,1,.7)", R * 0.03], ["rgba(255,200,180,.28)", R * 0.01]]) { ctx.strokeStyle = color; ctx.lineWidth = ancho; f.filos.forEach(fl => { camino(fl, X, Y); ctx.stroke(); }); }
        ctx.restore();
      }
      for (const m of rot.migas) {
        const p = RoturaSello.poseMiga(m, u); if (p.alfa <= 0) continue;
        const tam = m.tamano * R;
        ctx.save(); ctx.globalAlpha = p.alfa; ctx.translate((m.origen.x + p.dx) * R, (m.origen.y + p.dy) * R); ctx.rotate(p.grados * Math.PI / 180);
        ctx.fillStyle = m.tamano > 0.05 ? "#5a140b" : "#8e2618";
        ctx.beginPath(); ctx.moveTo(-tam, -tam * 0.4); ctx.lineTo(tam * 0.3, -tam); ctx.lineTo(tam, tam * 0.2); ctx.lineTo(-tam * 0.2, tam); ctx.closePath(); ctx.fill();
        ctx.restore();
      }
    }
    if (t < 1) requestAnimationFrame(cuadro);
    else { cv.remove(); if (!avisado) alPartirse?.(); }
  };
  requestAnimationFrame(cuadro);
}
let audioSello = null;
function sonarCrujido() {
  try {
    const AC = window.AudioContext || window.webkitAudioContext; if (!AC) return;
    audioSello ||= new AC();
    const datos = RoturaSello.sonido(audioSello.sampleRate);
    const buf = audioSello.createBuffer(1, datos.length, audioSello.sampleRate); buf.copyToChannel(datos, 0);
    const src = audioSello.createBufferSource(), vol = audioSello.createGain();
    vol.gain.value = 0.55; src.buffer = buf; src.connect(vol).connect(audioSello.destination); src.start();
  } catch (e) { console.error(e); }
}
