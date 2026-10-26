const cl = (x, a = 0, b = 1) => Math.max(a, Math.min(b, x));
const io = x => { x = cl(x); return x < .5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2; };
const seg = (t, a, b) => cl((t - a) / (b - a));
const ease = x => 1 - Math.pow(1 - cl(x), 3);
// Escenas (segundos) a la medida de la narración
window.ESCENAS = [
  { a: 0, b: 3.4 },
  { a: 3.4, b: 13.5, eti: "Summit Route", tit: "Tu vida es la montaña. Cada día es un paso.", sub: "La red social donde se aplaude el progreso, no la apariencia." },
  { a: 13.5, b: 21.4, eti: "Tu vida en años", tit: "Un símbolo por cada año", sub: "Los vividos en tinta, este año en oro y los años con metas con su punto encendido." },
  { a: 21.4, b: 31.8, eti: "Tu Brújula de la Cima", tit: "Una cumbre clara", sub: "Tu cumbre al centro, 8 campamentos y 64 pasos. Acércate, aléjate y gira cada tarjeta." },
  { a: 31.8, b: 39.4, eti: "Cimas", tit: "No subes solo", sub: "Los pasos reales de otras personas, a pantalla completa. Impulsa, comenta y únete a los retos." },
  { a: 39.4, b: 47.4, eti: "Ascenso financiero", tit: "Tu dinero, como una cordillera", sub: "Tu saldo, la cumbre de tu periodo y tu salud financiera mes a mes." },
  { a: 47.4, b: 56.1, eti: "Salir de deudas", tit: "Cinco métodos, un plan", sub: "Cuánto pagas este mes, cuándo eliminas cada deuda y cuántos puntos te devuelve." },
  { a: 56.1, b: 61.5 },
];
ESCENAS.introFin = 3.4; ESCENAS.outro = 56.1; ESCENAS.fin = 61.5;
const desliza = (t, a, b, desde, hasta) => `translateY(${desde + (hasta - desde) * io(seg(t, a, b))}%)`;
ESCENAS.pantallas = [
  { img: "hoy", a: 3.0, b: 13.3 },
  { img: "vida_larga", larga: true, a: 13.3, b: 21.2, scroll: t => 440 * io(seg(t, 14.4, 16.2)) },
  { img: "brujula0", a: 21.2, b: 24.2 },
  { img: "brujula1", a: 24.2, b: 26.9, entra: .25, mov: t => `scale(${1.25 - .25 * ease(seg(t, 24.2, 24.8))})` },
  { img: "brujula1g", a: 26.9, b: 29.2, entra: .05, z: 2, mov: t => `perspective(900px) rotateY(${(1 - ease(seg(t, 26.9, 27.45))) * 80}deg)` },
  { img: "brujula2", a: 29.2, b: 31.6, entra: .25, mov: t => `scale(${1.3 - .3 * ease(seg(t, 29.2, 29.9))})` },
  { img: "cimas1", a: 31.6, b: 35.6, mov: t => desliza(t, 35.0, 35.6, 0, -100) },
  { img: "cimas2", a: 35.0, b: 39.2, entra: .05, z: 2, mov: t => desliza(t, 35.0, 35.6, 100, 0) },
  { img: "grafica", a: 39.2, b: 47.2 },
  { img: "deudas_larga", larga: true, a: 47.2, b: 56.0, scroll: t => 1100 * io(seg(t, 49.6, 50.6)) + 656 * io(seg(t, 52.3, 53.3)) },
];
// El guía: tramos de movimiento en coordenadas de la app (390 × 844); tap = toca al llegar
const g = [];
const ir = (a, b, x0, y0, x1, y1, tap = false) => g.push({ a, b, x0, y0, x1, y1, tap });
ir(4.2, 5.3, 430, 760, 90, 80, true); ir(6.2, 7.2, 90, 80, 40, 200, true); ir(9.4, 10.4, 40, 200, 120, 488, true);
ir(14.2, 15.0, 120, 488, 200, 560); ir(16.4, 17.4, 200, 560, 258, 321, true);
ir(22.0, 23.1, 258, 321, 194, 400, true); ir(24.8, 25.8, 194, 400, 80, 300); ir(26.0, 26.85, 80, 300, 110, 380, true); ir(28.2, 29.1, 110, 380, 200, 420, true);
ir(32.4, 33.4, 200, 420, 200, 330); ir(36.4, 37.3, 200, 330, 346, 430, true);
ir(40.0, 41.0, 346, 430, 50, 640); ir(41.2, 42.0, 50, 640, 150, 605); ir(42.0, 42.8, 150, 605, 230, 540); ir(42.8, 43.6, 230, 540, 280, 466, true);
ir(48.3, 49.2, 280, 466, 195, 650, true); ir(50.8, 51.6, 195, 650, 328, 128, true); ir(53.6, 54.5, 328, 128, 178, 486, true);
ESCENAS.guia = g;
ESCENAS.guiaVisible = t => Math.min(seg(t, 3.9, 4.4), 1 - seg(t, 55.4, 55.9));
// Narración: cuándo empieza cada frase (para el audio)
ESCENAS.voz = [1.7, 3.9, 14.0, 21.9, 32.3, 39.9, 47.9, 58.4];
