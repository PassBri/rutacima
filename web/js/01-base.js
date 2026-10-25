/* SuRoute Web: la misma app del teléfono en el computador.
 *
 * Los datos son los mismos que guarda la app (cada fila de su base Room como un documento
 * JSON con los mismos campos) y viven en la tabla ruta_datos de Supabase. El teléfono y la web
 * leen y escriben ahí; la app del teléfono sincroniza con RutaWebRepository.
 *
 * Sin configuración (web/config.js vacío) la página funciona en modo demostración con una
 * ruta de ejemplo guardada solo en esta pestaña.
 */
"use strict";

const CONFIG = Object.assign({ supabaseUrl: "", supabaseAnonKey: "", contenido: "../app/src/main/assets/" }, window.RUTACIMA || {});
const MODO_REAL = /^https:\/\//.test(CONFIG.supabaseUrl) && !!CONFIG.supabaseAnonKey && !!window.supabase;

/* Reporte de errores anónimo: si algo falla en la web, se envía qué tipo de error fue y en qué línea,
   sin el mensaje (podría tener texto de la persona), sin cuenta y como máximo 5 por visita. */
(() => {
  if (!/^https:\/\//.test(CONFIG.supabaseUrl) || !CONFIG.supabaseAnonKey) return;
  const version = (document.currentScript?.src.match(/[?&]v=([^&]+)/) || [])[1] || "local";
  const vistos = new Set();
  const limpiar = pila => String(pila || "").split("\n").filter(l => /^\s*at |@/.test(l)).slice(0, 25)
    .map(l => l.trim().replace(/https?:\/\/[^/]+\//, "")).join("\n").slice(0, 6000);
  const enviar = (tipo, archivo, linea, pila) => {
    const firma = `${tipo || "Error"} en ${String(archivo || "?").split("/").pop().split("?")[0]}:${linea || 0}`.slice(0, 300);
    if (vistos.size >= 5 || vistos.has(firma)) return;
    vistos.add(firma);
    fetch(CONFIG.supabaseUrl.replace(/\/$/, "") + "/rest/v1/errores", {
      method: "POST", keepalive: true,
      headers: { apikey: CONFIG.supabaseAnonKey, Authorization: "Bearer " + CONFIG.supabaseAnonKey, "Content-Type": "application/json", Prefer: "return=minimal" },
      body: JSON.stringify({ origen: "web", version: version.slice(0, 40), sistema: navigator.userAgent.slice(0, 80), equipo: `${screen.width}×${screen.height}`, firma, rastro: limpiar(pila), cuando: new Date().toISOString() }),
    }).catch(() => {});
  };
  addEventListener("error", e => { if (e.error || e.message) enviar(e.error?.name || "Error", e.filename, e.lineno, e.error?.stack); });
  addEventListener("unhandledrejection", e => { const r = e.reason; if (r instanceof Error) enviar(r.name, (String(r.stack).match(/\/js\/[^:)\s]+/) || [""])[0], (String(r.stack).match(/:(\d+):\d+/) || [])[1], r.stack); });
})();
const LOGO = CONFIG.logo || "iconos/simbolo.svg";
/** El símbolo de SuRoute en un contenedor tonal de Material 3: la única marca de la app (reemplaza al sello de cera). */
const SIMBOLO_SVG = `<svg viewBox="0 0 100 100" aria-hidden="true"><path d="M41.45 85.53A38 38 0 1 1 58.55 85.53"/><circle cx="50" cy="86.5" r="6.5"/></svg>`;
const SIMBOLO = (cls = "", etiqueta = "") => `<span class="simbolo ${cls}"${etiqueta ? ` role="img" aria-label="${etiqueta}"` : ` aria-hidden="true"`}>${SIMBOLO_SVG}</span>`;
/* Logo definitivo de SuRoute (diseno/logo/): el círculo abierto al sur con el punto en el hueco, "el sur en el interior".
 * Va en línea y toma el color del texto (tinta en claro, crema en oscuro). El sello de cera queda para los rituales. */
const MARCA_SVG = "<path d=\"M41.45 85.53A38 38 0 1 1 58.55 85.53\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.7\"/><circle cx=\"50\" cy=\"86.50\" r=\"3.7\" fill=\"currentColor\"/>";
const MARCA_GRUESA_SVG = "<path d=\"M41.45 85.53A38 38 0 1 1 58.55 85.53\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"4.2\"/><circle cx=\"50\" cy=\"86.50\" r=\"5.0\" fill=\"currentColor\"/>";
const marca = (cls = "logo", gruesa = false) => `<svg class="${cls}" viewBox="0 0 100 100" role="img" aria-label="SuRoute">${gruesa ? MARCA_GRUESA_SVG : MARCA_SVG}</svg>`;


/* ======================================================================
 * Utilidades
 * ====================================================================== */
const $ = id => document.getElementById(id);
const esc = s => String(s ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const DIA_MS = 86400000;
const hoyFecha = () => { const d = new Date(); d.setHours(0, 0, 0, 0); return d; };
const iso = d => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
const deIso = s => { const [a, m, d] = s.split("-").map(Number); return new Date(a, m - 1, d); };
const diaDelAnio = d => Math.round((d - new Date(d.getFullYear(), 0, 1)) / DIA_MS); // 0..365
const MESES = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];
const MES_C = MESES.map(m => m.slice(0, 3));
const mayus = s => s ? s[0].toUpperCase() + s.slice(1) : s;
const diasEnMes = (a, m) => new Date(a, m, 0).getDate(); // m = 1..12
const fechaLarga = d => d.toLocaleDateString("es", { weekday: "long", day: "numeric", month: "long", year: "numeric" });
const horaDe = ms => new Date(ms).toLocaleTimeString("es", { hour: "2-digit", minute: "2-digit" });
const hace = ms => {
  const s = (Date.now() - ms) / 1000;
  if (s < 60) return "ahora";
  if (s < 3600) return `hace ${Math.floor(s / 60)} min`;
  if (s < 86400) return `hace ${Math.floor(s / 3600)} h`;
  return `hace ${Math.floor(s / 86400)} d`;
};
let ultimoId = 0;
/** Id nuevo para filas creadas en la web (milisegundos: no choca con los del teléfono). */
const nuevoId = () => { ultimoId = Math.max(Date.now(), ultimoId + 1); return ultimoId; };
const lista = s => String(s || "").split(",").map(x => x.trim()).filter(Boolean);
const numeros = s => lista(s).map(Number).filter(n => !isNaN(n));
function toast(t, { accion, alHacer } = {}) {
  const el = $("toast"); el.textContent = t; el.hidden = false;
  if (accion) { const b = document.createElement("button"); b.type = "button"; b.className = "toast-accion"; b.textContent = accion; b.onclick = () => { el.hidden = true; alHacer?.(); }; el.append(b); }
  clearTimeout(toast.t); toast.t = setTimeout(() => (el.hidden = true), Math.max(3200, t.length * 55));
}

/* Íconos de Material Design */
const P = {
  girar: "M12 6v3l4-4-4-4v3c-4.42 0-8 3.58-8 8 0 1.57.46 3.03 1.24 4.26L6.7 14.8c-.45-.83-.7-1.79-.7-2.8 0-3.31 2.69-6 6-6zm6.76 1.74L17.3 9.2c.44.84.7 1.79.7 2.8 0 3.31-2.69 6-6 6v-3l-4 4 4 4v-3c4.42 0 8-3.58 8-8 0-1.57-.46-3.03-1.24-4.26z",
  campana: "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z",
  ajustes: "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.48.48 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96a.48.48 0 0 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z",
  correo: "M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z",
  telefono: "M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z",
  pantallaCompleta: "M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z",
  ampliar: "M21 11V3h-8l3.29 3.29-10 10L3 13v8h8l-3.29-3.29 10-10z",
  guardado: "M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2zm0 15l-5-2.18L7 18V5h10v13z",
  guardadoLleno: "M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z",
  cuadricula: "M20 2H4c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM8 20H4v-4h4v4zm0-6H4v-4h4v4zm0-6H4V4h4v4zm6 12h-4v-4h4v4zm0-6h-4v-4h4v4zm0-6h-4V4h4v4zm6 12h-4v-4h4v4zm0-6h-4v-4h4v4zm0-6h-4V4h4v4z",
  personaMas: "M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
  ojo: "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z",
  ojoNo: "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z",
  adjunto: "M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5c0-1.38 1.12-2.5 2.5-2.5s2.5 1.12 2.5 2.5v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6H10v9.5c0 1.38 1.12 2.5 2.5 2.5s2.5-1.12 2.5-2.5V5c0-2.21-1.79-4-4-4S7 2.79 7 5v12.5c0 3.04 2.46 5.5 5.5 5.5s5.5-2.46 5.5-5.5V6h-1.5z",
  pdf: "M20 2H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-8.5 7.5c0 .83-.67 1.5-1.5 1.5H9v2H7.5V7H10c.83 0 1.5.67 1.5 1.5v1zm5 2c0 .83-.67 1.5-1.5 1.5h-2.5V7H15c.83 0 1.5.67 1.5 1.5v3zm4-3H19v1h1.5V11H19v2h-1.5V7h3v1.5zM9 9.5h1v-1H9v1zM4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm10 5.5h1v-3h-1v3z",
  deuda: "M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z",
  repetir: "M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z",
  pc: "M21 2H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h7v2H8v2h8v-2h-2v-2h7c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H3V4h18v12z",
  descargar: "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z",
  puntos: "M2.6 5a2.4 2.4 0 1 1 4.8 0a2.4 2.4 0 1 1 -4.8 0zM9.6 5a2.4 2.4 0 1 1 4.8 0a2.4 2.4 0 1 1 -4.8 0zM16.6 5a2.4 2.4 0 1 1 4.8 0a2.4 2.4 0 1 1 -4.8 0zM2.6 12a2.4 2.4 0 1 1 4.8 0a2.4 2.4 0 1 1 -4.8 0zM9.6 12a2.4 2.4 0 1 1 4.8 0a2.4 2.4 0 1 1 -4.8 0zM16.6 12a2.4 2.4 0 1 1 4.8 0a2.4 2.4 0 1 1 -4.8 0zM2.6 19a2.4 2.4 0 1 1 4.8 0a2.4 2.4 0 1 1 -4.8 0zM9.6 19a2.4 2.4 0 1 1 4.8 0a2.4 2.4 0 1 1 -4.8 0zM10.7 19a1.3 1.3 0 1 1 2.6 0a1.3 1.3 0 1 1 -2.6 0zM16.6 19a2.4 2.4 0 1 1 4.8 0a2.4 2.4 0 1 1 -4.8 0zM17.7 19a1.3 1.3 0 1 1 2.6 0a1.3 1.3 0 1 1 -2.6 0z",
  grafica: "M9 4H7v2H5v12h2v2h2v-2h2V6H9V4zm0 12H7V8h2v8zm10-8h-2V4h-2v4h-2v7h2v5h2v-5h2V8zm-2 5h-2v-3h2v3z",
  salud: "M15.11 12.45 14 10.24l-3.11 6.21c-.16.34-.51.55-.89.55s-.73-.21-.89-.55L7.38 13H2v5c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2v-5h-6c-.38 0-.73-.21-.89-.55zM20 4H4c-1.1 0-2 .9-2 2v5h6c.38 0 .73.21.89.55L10 13.76l3.11-6.21c.34-.68 1.45-.68 1.79 0L16.62 11H22V6c0-1.1-.9-2-2-2z",
  balanza: "M13 7.83c.85-.3 1.53-.98 1.83-1.83H18l-3 7c0 1.66 1.57 3 3.5 3s3.5-1.34 3.5-3l-3-7h2V4h-6.17c-.41-1.17-1.52-2-2.83-2s-2.42.83-2.83 2H3v2h2l-3 7c0 1.66 1.57 3 3.5 3S9 14.66 9 13L6 6h3.17c.3.85.98 1.53 1.83 1.83V19H2v2h20v-2h-9V7.83zM20.37 13h-3.74l1.87-4.36L20.37 13zm-13 0H3.63L5.5 8.64 7.37 13zM12 6c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1z",
  // Ascenso financiero: la cordillera con el sendero que sube y la moneda como sol
  finanzas: "M16.76 4.95L16.85 4.77L16.95 4.66L16.95 5.63L16.86 5.55L16.77 5.36L16.73 5.16ZM21.56 4.48L21.71 4.89L21.81 5.32L21.88 5.76L21.90 6.20L21.88 6.64L21.81 7.08L21.71 7.51L21.56 7.92L21.37 8.32L21.14 8.70L20.88 9.05L20.58 9.38L20.25 9.68L19.90 9.94L19.52 10.17L19.12 10.36L18.71 10.51L18.28 10.61L17.84 10.68L17.40 10.70L16.96 10.68L16.52 10.61L16.09 10.51L15.68 10.36L15.28 10.17L14.90 9.94L14.55 9.68L14.22 9.38L13.92 9.05L13.66 8.70L13.43 8.32L13.24 7.92L13.09 7.51L12.99 7.08L12.92 6.64L12.90 6.20L12.92 5.76L12.99 5.32L13.09 4.89L13.24 4.48L13.43 4.08L13.66 3.70L13.92 3.35L14.22 3.02L14.55 2.72L14.90 2.46L15.28 2.23L15.68 2.04L16.09 1.89L16.52 1.79L16.96 1.72L17.40 1.70L17.84 1.72L18.28 1.79L18.71 1.89L19.12 2.04L19.52 2.23L19.90 2.46L20.25 2.72L20.58 3.02L20.88 3.35L21.14 3.70L21.37 4.08ZM17.85 5.69L17.85 4.67L17.93 4.74L18.15 5.10L18.31 5.20L18.49 5.23L18.71 5.17L18.85 5.05L18.95 4.84L18.95 4.66L18.72 4.21L18.63 4.10L18.29 3.79L17.85 3.59L17.85 3.06L17.75 2.81L17.53 2.67L17.31 2.66L17.11 2.75L16.98 2.93L16.95 3.06L16.95 3.59L16.80 3.65L16.44 3.84L16.06 4.24L15.84 4.69L15.78 5.15L15.87 5.69L16.11 6.13L16.47 6.48L16.95 6.71L16.95 7.73L16.87 7.66L16.65 7.30L16.49 7.20L16.31 7.17L16.09 7.23L15.95 7.35L15.85 7.56L15.87 7.79L16.17 8.30L16.51 8.61L16.95 8.81L16.95 9.34L16.98 9.47L17.15 9.67L17.36 9.75L17.61 9.70L17.80 9.51L17.85 9.34L17.85 8.81L18.00 8.75L18.36 8.56L18.74 8.16L18.94 7.75L19.02 7.30L18.95 6.76L18.72 6.31L18.37 5.95ZM18.04 7.45L17.95 7.63L17.85 7.74L17.85 6.77L17.94 6.85L18.03 7.04L18.07 7.24ZM23.00 21.50L1.00 21.50L8.20 9.20L10.75 13.10L8.90 13.40L9.61 14.29L7.05 16.11L6.89 16.28L6.82 16.44L6.80 16.66L6.87 16.88L7.76 18.22L4.28 20.39L4.92 21.41L8.92 18.91L9.05 18.79L9.19 18.53L9.20 18.35L9.16 18.17L8.22 16.75L10.36 15.22L10.90 15.90L11.50 14.25L11.60 14.40L14.00 11.40Z",
  izq: "M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z",
  der: "M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z",
  editar: "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z",
  arrastrar: "M11 18c0 1.1-.9 2-2 2s-2-.9-2-2 .9-2 2-2 2 .9 2 2zm-2-8c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0-6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm6 4c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z",
  masH: "M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z",
  copiar2: "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z",
  reportar: "M15.73 3H8.27L3 8.27v7.46L8.27 21h7.46L21 15.73V8.27L15.73 3zM12 17.3c-.72 0-1.3-.58-1.3-1.3 0-.72.58-1.3 1.3-1.3.72 0 1.3.58 1.3 1.3 0 .72-.58 1.3-1.3 1.3zm1-4.3h-2V7h2v6z",
  bloquear: "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zM4 12c0-4.42 3.58-8 8-8 1.85 0 3.55.63 4.9 1.69L5.69 16.9C4.63 15.55 4 13.85 4 12zm8 8c-1.85 0-3.55-.63-4.9-1.69L18.31 7.1C19.37 8.45 20 10.15 20 12c0 4.42-3.58 8-8 8z",
  lista: "M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z",
  arriba: "M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z",
  compartir: "M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z",
  plata: "M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z",
  ejes: "M12.00 2.00L20.66 7.00L20.66 17.00L12.00 22.00L3.34 17.00L3.34 7.00ZM4.81 7.85L4.81 16.15L12.00 20.30L19.19 16.15L19.19 7.85L12.00 3.70ZM12.00 5.40L15.81 9.80L17.02 14.90L12.00 15.60L6.63 15.10L7.84 9.60Z",
  cima: "M1 21 9.6 8.6l3.2 4.4 2.3-3.1L23 21zM9 2h1.3v7.2H9zM10.3 2.2l5.4 1.9-5.4 1.9z",
  ruta: "M14 6l-3.75 5 2.85 3.8-1.6 1.2C9.81 13.75 7 10 7 10l-6 8h22L14 6z",
  // Hoy: huellas (el paso de hoy)
  hoy: "M8.5 2.4c2.3 0 3.8 2.3 3.8 5.6s-1.5 4.8-3.8 4.8S4.7 11.3 4.7 8s1.5-5.6 3.8-5.6zM8.5 13.8c1.8 0 2.9 1.1 2.9 2.7s-1.1 2.6-2.9 2.6-2.9-1-2.9-2.6 1.1-2.7 2.9-2.7zM15.5 5.4c2.3 0 3.8 2.3 3.8 5.6s-1.5 4.8-3.8 4.8-3.8-1.5-3.8-4.8 1.5-5.6 3.8-5.6zM15.5 16.8c1.8 0 2.9 1.1 2.9 2.7s-1.1 2.6-2.9 2.6-2.9-1-2.9-2.6 1.1-2.7 2.9-2.7z",
  // Metas: la baliza de orientación (el punto de control que buscas en la ruta)
  metas: "M3.00 3.40L14.60 3.40L20.60 6.20L20.60 17.40L14.60 15.00L9.65 15.00L9.65 22.00L8.05 22.00L8.05 15.00L3.00 15.00ZM4.50 4.90L13.10 13.50L13.10 4.90ZM16.00 6.67L19.20 14.13L19.20 7.09L16.00 5.60Z",
  // Comunidad: la fogata, limpia (llama con su núcleo y dos leños cruzados)
  comunidad: "M12 3c1 3 4.5 4.6 4.5 8.4A4.5 4.5 0 0 1 12 16a4.5 4.5 0 0 1-4.5-4.6c0-2 1-3.3 2-4.2.3 1.5 1 2.4 2 2.6C11 7.6 11 5.3 12 3z M12 10.2c-.9 1-1.5 1.8-1.5 2.8 0 .9.7 1.6 1.5 1.6s1.5-.7 1.5-1.6c0-1-.6-1.8-1.5-2.8z M19.70 18.00L19.95 17.88L20.20 17.61L20.32 17.35L20.34 16.99L20.23 16.63L20.06 16.41L19.74 16.21L19.47 16.15L12.00 17.78L4.81 16.17L4.44 16.16L4.01 16.35L3.70 16.80L3.68 17.35L3.98 17.82L4.39 18.03L7.63 18.75L4.22 19.53L3.80 19.89L3.66 20.24L3.66 20.51L3.77 20.87L4.01 21.15L4.35 21.32L4.71 21.34L12.00 19.72L19.19 21.33L19.56 21.34L20.06 21.09L20.33 20.61L20.29 20.06L20.02 19.68L19.61 19.47L16.37 18.75Z",
  personas: "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z",
  // Aprende: el mapa plegado con la ruta (aprender es leer el mapa)
  aprende: "M19.48 7.55L19.20 7.02L18.73 6.63L18.15 6.46L17.55 6.52L17.02 6.80L16.63 7.27L16.46 7.85L16.52 8.45L16.80 8.98L17.27 9.37L17.85 9.54L18.45 9.48L18.98 9.20L19.37 8.73L19.54 8.15ZM2.09 20.77L2.26 21.09L2.45 21.28L2.79 21.43L3.15 21.44L8.54 19.50L15.24 21.41L15.53 21.45L15.82 21.39L21.32 19.39L21.65 19.19L21.87 18.88L21.95 18.50L21.95 3.50L21.88 3.14L21.68 2.84L21.30 2.60L21.03 2.55L20.76 2.58L15.46 4.50L8.76 2.59L8.37 2.56L2.68 4.61L2.35 4.81L2.13 5.12L2.05 5.50L2.05 20.50ZM16.73 10.26L16.51 9.85L16.11 9.54L16.11 6.29L20.05 4.86L20.05 17.83L16.11 19.27L16.11 11.66L16.43 11.43L16.69 11.05L16.77 10.72ZM14.89 9.66L14.62 9.95L14.51 10.15L14.42 10.60L14.47 10.94L14.56 11.16L14.89 11.54L14.89 11.83L14.56 11.56L14.23 11.44L13.88 11.43L13.55 11.51L13.25 11.69L13.02 11.95L12.87 12.26L12.82 12.56L12.45 12.31L12.12 12.23L11.77 12.24L11.35 12.42L11.09 12.65L10.96 12.84L10.84 13.17L10.82 13.40L10.91 13.85L11.17 14.23L11.55 14.49L12.00 14.58L12.45 14.49L12.83 14.23L13.09 13.85L13.18 13.44L13.55 13.69L14.00 13.78L14.45 13.69L14.65 13.58L14.89 13.37L14.89 19.34L9.11 17.69L9.11 13.25L9.35 13.39L9.68 13.47L10.03 13.46L10.36 13.34L10.55 13.21L10.78 12.95L10.93 12.64L10.98 12.30L10.93 11.96L10.84 11.74L10.63 11.47L10.36 11.26L10.03 11.14L9.68 11.13L9.35 11.21L9.11 11.35L9.11 4.66L14.89 6.31ZM3.95 6.17L7.89 4.73L7.89 12.26L7.37 12.24L7.15 12.31L6.85 12.49L6.56 12.84L6.44 13.17L6.42 13.40L6.51 13.85L6.62 14.05L6.85 14.31L7.15 14.49L7.37 14.56L7.89 14.54L7.89 17.71L3.95 19.14ZM6.69 14.75L6.43 14.37L6.05 14.11L5.60 14.02L5.15 14.11L4.77 14.37L4.51 14.75L4.42 15.20L4.51 15.65L4.77 16.03L5.15 16.29L5.60 16.38L6.05 16.29L6.43 16.03L6.69 15.65L6.78 15.20Z",
  // Coach: un brote dentro de un diálogo (la conversación que te hace crecer)
  coach: "M4 4.5h16a1.5 1.5 0 0 1 1.5 1.5v10a1.5 1.5 0 0 1-1.5 1.5H10l-4.5 3.5v-3.5H4A1.5 1.5 0 0 1 2.5 16V6A1.5 1.5 0 0 1 4 4.5z M11.3 10.4h1.4V15h-1.4z M11.9 11.6c-.2-2-1.6-3.3-3.7-3.3 0 2.1 1.4 3.3 3.7 3.3z M12.1 10.5c0-2.1 1.4-3.4 3.7-3.4 0 2.1-1.4 3.4-3.7 3.4z",
  ia: "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z",
  // Mis frases: el piolet
  frases: "M7.2 21.6l7.3-15.9 1.6.8-7.3 15.9zM9.9 4.5c3-2 7.2-1.7 10 1.1l.6.6-3.4.4-.6-.6c-1.7-1.3-4-1.5-5.8-.5zM10.3 6.1L7.6 5.1l.5-1.5 2.9 1z",
  // Perfil: tú, con los hombros de montaña
  perfil: "M9.244 21.613A10 10 0 1 1 14.756 21.613L14.370 20.267A8.6 8.6 0 1 0 9.630 20.267ZM10.6 21.3a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0-2.8 0ZM12 5.8a2.9 2.9 0 1 0 0 5.8a2.9 2.9 0 1 0 0-5.8ZM6.9 17.3C7.6 14.6 9.6 13.2 12 13.2s4.4 1.4 5.1 4.1C15.8 18.4 14 19 12 19s-3.8-.6-5.1-1.7Z",
  // Mochila (herramientas): el mosquetón
  mochila: "M9.05 13.99L9.19 14.19L9.47 14.38L9.71 14.44L10.04 14.42L10.33 14.26L10.50 14.08L10.61 13.86L10.65 13.53L8.19 7.28L8.06 7.07L7.86 6.88L8.08 6.56L8.22 5.67L8.58 4.95L8.95 4.58L9.42 4.31L10.23 4.15L15.02 4.18L15.44 4.26L16.22 4.59L16.58 4.82L17.18 5.42L17.60 6.16L17.82 6.98L17.85 16.37L17.74 17.24L17.60 17.64L17.18 18.38L16.90 18.70L16.22 19.21L15.44 19.54L15.02 19.62L9.63 19.65L8.82 19.49L8.15 19.05L7.71 18.38L7.55 17.57L7.51 14.92L7.39 14.67L7.20 14.47L6.88 14.29L6.60 14.25L6.32 14.29L6.00 14.47L5.81 14.67L5.67 15.01L5.69 18.18L5.80 18.68L6.15 19.52L6.51 20.05L6.85 20.44L7.25 20.77L8.03 21.22L8.64 21.43L9.15 21.52L14.60 21.55L15.87 21.39L16.51 21.18L17.23 20.83L17.78 20.45L18.65 19.58L19.09 18.92L19.38 18.31L19.59 17.67L19.71 17.01L19.75 7.34L19.59 6.13L19.33 5.37L19.03 4.77L18.65 4.22L17.78 3.35L17.23 2.97L16.63 2.67L15.99 2.44L15.33 2.30L14.60 2.25L10.14 2.25L9.12 2.40L8.28 2.75L7.75 3.11L7.11 3.75L6.75 4.28L6.40 5.12L6.28 5.75L6.27 6.41L6.42 6.74L6.76 7.04L6.57 7.42L6.58 7.84Z",
  // Guardar en tu mochila: el morral
  morral: "M20 8v12c0 1.1-.9 2-2 2H6c-1.1 0-2-.9-2-2V8c0-1.86 1.28-3.41 3-3.86V2h3v2h4V2h3v2.14c1.72.45 3 2 3 3.86zM6 12v2h10v2h2v-4H6z",
  buscar: "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
  enviar: "M2.01 21L23 12 2.01 3 2 10l15 2-15 2z",
  volver: "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z",
  candado: "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z",
  bandera: "M3.00 3.40L14.60 3.40L20.60 6.20L20.60 17.40L14.60 15.00L9.65 15.00L9.65 22.00L8.05 22.00L8.05 15.00L3.00 15.00ZM4.50 4.90L13.10 13.50L13.10 4.90ZM16.00 6.67L19.20 14.13L19.20 7.09L16.00 5.60Z",
  // Impulso (me gusta): un corazón con la montaña adentro
  impulso: "M12 21.3S2.2 15.4 2.2 8.9C2.2 5.6 4.6 3.2 7.6 3.2c1.8 0 3.4.8 4.4 2.2 1-1.4 2.6-2.2 4.4-2.2 3 0 5.4 2.4 5.4 5.7 0 6.5-9.8 12.4-9.8 12.4zM7 14.2l3.5-4.6 2 2.6 1.4-1.8 3.1 3.8z",
  // Apariencia: medio sol, media luna sobre la montaña
  tema: "M13.55 1.57L13.60 1.80L13.60 3.60L13.57 3.77L13.46 3.98L13.28 4.13L13.06 4.20L12.83 4.17L12.67 4.10L12.54 3.98L12.41 3.72L12.40 1.80L12.43 1.63L12.50 1.47L12.62 1.34L12.77 1.25L12.94 1.20L13.17 1.23L13.33 1.30L13.46 1.42ZM17.75 4.03L17.62 4.22L16.22 5.62L16.08 5.73L15.86 5.80L15.63 5.77L15.42 5.66L15.27 5.48L15.21 5.32L15.20 5.14L15.25 4.97L15.34 4.82L16.78 3.38L16.92 3.27L17.08 3.21L17.26 3.20L17.43 3.25L17.58 3.34L17.70 3.47L17.79 3.68L17.80 3.86ZM15.00 7.40L15.25 8.01L15.33 8.34L15.40 9.00L15.33 9.66L15.25 9.99L15.00 10.60L14.63 11.16L14.12 11.65L13.79 11.60L13.49 11.65L13.31 11.73L13.14 11.85L12.76 12.31L12.33 12.38L12.00 12.40L12.00 13.31L11.79 13.59L11.10 13.51L10.66 13.40L10.24 13.25L9.83 13.06L9.44 12.82L9.08 12.56L8.75 12.25L8.44 11.92L8.18 11.56L7.94 11.17L7.75 10.76L7.60 10.34L7.49 9.90L7.42 9.45L7.40 9.00L7.42 8.55L7.49 8.10L7.60 7.66L7.75 7.24L7.94 6.83L8.18 6.44L8.44 6.08L8.75 5.75L9.08 5.44L9.44 5.18L9.83 4.94L10.24 4.75L10.66 4.60L11.10 4.49L11.55 4.42L11.96 4.40L11.51 4.86L11.27 5.18L10.90 5.87L10.68 6.62L10.60 7.40L10.68 8.18L10.90 8.93L11.27 9.62L11.51 9.94L12.00 10.44L12.00 5.60L12.66 5.67L12.99 5.75L13.30 5.86L13.89 6.17L14.40 6.60L14.63 6.84ZM19.68 9.53L19.40 9.60L17.28 9.59L17.07 9.50L16.90 9.33L16.81 9.12L16.81 8.88L16.87 8.72L16.98 8.58L17.12 8.47L17.28 8.41L19.52 8.41L19.68 8.47L19.82 8.58L19.93 8.72L19.99 8.88L20.00 9.06L19.95 9.23L19.86 9.38ZM23.00 22.00L1.00 22.00L7.60 13.40L10.60 16.80L13.80 12.60Z",
  salir: "M10.09 15.59L11.5 17l5-5-5-5-1.41 1.41L12.67 11H3v2h9.67l-2.58 2.59zM19 3H5c-1.11 0-2 .9-2 2v4h2V5h14v14H5v-4H3v4c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z",
  pin: "M16 9V4h1c.55 0 1-.45 1-1s-.45-1-1-1H7c-.55 0-1 .45-1 1s.45 1 1 1h1v5c0 1.66-1.34 3-3 3v2h5.97v7l1 1 1-1v-7H19v-2c-1.66 0-3-1.34-3-3z",
  mas: "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z",
  borrar: "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z",
  // Brújula (Material "explore", contorno): Publicar en la barra
  brujula: "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-5.5-2.5l7.51-3.49L17.5 6.5 9.99 9.99 6.5 17.5zm5.5-6.6c.61 0 1.1.49 1.1 1.1s-.49 1.1-1.1 1.1-1.1-.49-1.1-1.1.49-1.1 1.1-1.1z",
  publicar: "M1,21L8.5,10L11.5,14.2L14.6,9.6L23,21ZM14,2.4h1.25v7.4h-1.25zM15.25,2.4L20.4,4.2L15.25,6ZM4.2,2.4h1.6v2.4h2.4v1.6H5.8v2.4H4.2V6.4H1.8V4.8h2.4z",
  comentario: "M21.99 4c0-1.1-.89-2-1.99-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14l4 4-.01-18z",
  // Mensajes: la radio de montaña
  mensajes: "M14.20 1.60L16.00 1.60L16.00 6.50L16.74 6.51L17.01 6.56L17.26 6.67L17.59 6.91L17.76 7.12L17.89 7.36L17.97 7.63L18.00 7.90L18.00 20.60L17.97 20.87L17.89 21.14L17.68 21.49L17.38 21.76L17.14 21.89L16.74 21.99L7.40 22.00L7.13 21.97L6.86 21.89L6.51 21.68L6.24 21.38L6.11 21.14L6.01 20.74L6.00 7.90L6.03 7.63L6.11 7.36L6.32 7.01L6.62 6.74L6.99 6.56L7.26 6.51L14.20 6.50ZM15.40 9.50L15.38 9.35L15.29 9.18L15.14 9.06L14.95 9.00L9.10 9.00L8.86 9.06L8.68 9.22L8.60 9.45L8.60 12.70L8.66 12.94L8.75 13.05L8.86 13.14L9.10 13.20L14.90 13.20L15.14 13.14L15.32 12.98L15.40 12.75ZM15.11 16.74L15.26 16.69L15.43 16.55L15.51 16.41L15.55 16.20L15.51 15.99L15.43 15.85L15.26 15.71L15.05 15.65L8.95 15.65L8.74 15.71L8.61 15.81L8.49 15.99L8.45 16.15L8.47 16.36L8.57 16.55L8.74 16.69L8.95 16.75ZM15.11 19.34L15.26 19.29L15.43 19.15L15.51 19.01L15.55 18.80L15.51 18.59L15.39 18.41L15.26 18.31L15.05 18.25L8.95 18.25L8.74 18.31L8.61 18.41L8.49 18.59L8.45 18.75L8.47 18.96L8.57 19.15L8.74 19.29L8.95 19.35Z",
  cumbre: "M13.5 5.5c1.09 0 2-.9 2-2s-.91-2-2-2c-1.1 0-2 .9-2 2s.9 2 2 2zM17.5 10.78c-1.23-.37-2.22-1.17-2.8-2.18l-1-1.6c-.41-.65-1.11-1-1.84-1-.78 0-1.59.5-1.78 1.44S7 23 7 23h2.1l1.8-8 2.1 2v6h2v-7.5l-2.1-2 .6-3c1 1.15 2.41 2.01 4 2.34V23H19V9h-1.5v1.78zM7.43 13.13l-2.12-.41c-.54-.11-.9-.63-.79-1.17l.76-3.93c.21-1.08 1.26-1.79 2.34-1.58l1.16.23-1.35 6.86z",
  audifonos: "M12 1c-4.97 0-9 4.03-9 9v7c0 1.66 1.34 3 3 3h3v-8H5v-2c0-3.87 3.13-7 7-7s7 3.13 7 7v2h-4v8h3c1.66 0 3-1.34 3-3v-7c0-4.97-4.03-9-9-9z",
  mic: "M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z",
  play: "M8 5v14l11-7z",
  pausa: "M6 19h4V5H6v14zm8-14v14h4V5h-4z",
  atras: "M11 18V6l-8.5 6 8.5 6zm.5-6l8.5 6V6l-8.5 6z",
  adelante: "M4 18l8.5-6L4 6v12zm9-12v12l8.5-6L13 6z",
  cielo360: "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z",
  cerrar: "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z",
  cordada: "M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z",
  check: "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z",
  abajo: "M16.59 8.59L12 13.17 7.41 8.59 6 10l6 6 6-6z",
  copiar: "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z",
  foto: "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z",
};
/** Íconos propios dibujados con huecos (fogata, hacer fuego): se rellenan con la regla par-impar. */
const IC_PAR = new Set(["mochila", "impulso", "coach", "comunidad", "aprende", "ejes", "puntos"]);
const ic = (n, cls = "i") => `<svg class="${cls}" viewBox="0 0 24 24" aria-hidden="true"><path${IC_PAR.has(n) ? ' fill-rule="evenodd"' : ""} d="${P[n]}"/></svg>`;

/* ======================================================================
 * El método (igual que en la app)
 * ====================================================================== */
const EJES = [
  ["VOL", "Voluntad", "voluntad"], ["MAE", "Maestría", "maestria"], ["VOZ", "Voz", "voz"],
  ["VAL", "Valor", "valor"], ["EVO", "Evolución", "evolucion"], ["TRA", "Trascendencia", "trascendencia"],
];
const NOMBRE_EJE = Object.fromEntries(EJES.map(([c, n]) => [c, n]));
const COLOR_EJE = { VOL: "#8E3B26", MAE: "#5C4A8A", VOZ: "#2F6F7A", VAL: "#6B5530", EVO: "#3F7A4A", TRA: "#6B2A1A" };
const HABITOS_BASE = [
  ["VOL1", "VOL", "Revisé mi visión/propósito (1 min)"], ["VOL2", "VOL", "Hice algo hoy que me acerca a mi cumbre"], ["VOL3", "VOL", "Dije NO a algo que me aleja de mi cumbre"],
  ["MAE1", "MAE", "Practiqué/perfeccioné una habilidad clave (mín. 30 min)"], ["MAE2", "MAE", "Aprendí algo nuevo relacionado con mi cumbre"], ["MAE3", "MAE", "Salí de mi zona de confort hoy"],
  ["VOZ1", "VOZ", "Expresé mi opinión auténtica en al menos una situación"], ["VOZ2", "VOZ", "Compartí mi trabajo/mensaje de alguna forma"], ["VOZ3", "VOZ", "No me callé algo importante por miedo"],
  ["VAL1", "VAL", "Trabajé en algo que genera valor económico"], ["VAL2", "VAL", "Cobré/pedí lo que merezco sin culpa"], ["VAL3", "VAL", "Invertí en mi crecimiento (tiempo o dinero)"],
  ["EVO1", "EVO", "Reflexioné sobre algo que aprendí hoy (journaling 5 min)"], ["EVO2", "EVO", "Pedí feedback o perspectiva externa"], ["EVO3", "EVO", "Ajusté un hábito o estrategia que no funciona"],
  ["TRA1", "TRA", "Ayudé a alguien sin esperar nada a cambio"], ["TRA2", "TRA", "Conecté con algo mayor que yo (naturaleza, arte, espiritualidad)"], ["TRA3", "TRA", "Trabajé en mi legado o contribución al mundo"],
];
/* Hábitos propios: se guardan como respuesta "habitos#lista" (igual que en la app) y se sincronizan. t vacío = texto de fábrica. */
const HABITOS_CLAVE = "habitos#lista", HABITOS_MAX = 6;
function habitos() {
  try {
    const v = Store.get("respuesta", HABITOS_CLAVE)?.valor;
    if (v) {
      const l = JSON.parse(v).filter(h => h && h.id && NOMBRE_EJE[h.eje])
        .map(h => [String(h.id), h.eje, String(h.t || "").slice(0, 120) || HABITOS_BASE.find(b => b[0] === h.id)?.[2] || ""]);
      const res = EJES.flatMap(([c]) => { const x = l.filter(h => h[1] === c); return x.length ? x : HABITOS_BASE.filter(h => h[1] === c); });
      if (res.length) return res;
    }
  } catch { /* lista dañada: quedan los de fábrica */ }
  return HABITOS_BASE;
}
function guardarHabitos(l) {
  const iguales = l.length === HABITOS_BASE.length && l.every((h, i) => h[0] === HABITOS_BASE[i][0] && h[2] === HABITOS_BASE[i][2]);
  return responder("habitos", HABITOS_CLAVE, iguales ? "" : JSON.stringify(l.map(([id, eje, t]) => ({ id, eje, t: HABITOS_BASE.some(b => b[0] === id && b[2] === t) ? "" : t }))));
}
const FASES = [["ORIENTACION", "Orientación"], ["PREPARACION", "Preparación"], ["TRAVESIA", "Travesía"], ["ASCENSO", "Ascenso"], ["CULMINACION", "Culminación"], ["CONTEMPLACION", "Contemplación"], ["DESCENSO", "Descenso"]];
/** Horizonte de un propósito: la persona elige cualquier número de años en este rango; los atajos solo ayudan. */
const HORIZONTE_MIN = 2, HORIZONTE_MAX = 50;
const HORIZONTES = [3, 5, 10, 15, 20, 30];
const acotarHorizonte = n => Math.min(HORIZONTE_MAX, Math.max(HORIZONTE_MIN, Math.round(Number(n) || 5)));
const META_DEFECTO = 100, META_MAXIMA = 120;
const ESTADOS = [["NO_INICIADA", "No iniciada"], ["INICIADA", "Iniciada"], ["EN_PAUSA", "En pausa"], ["EN_CURSO", "En curso"], ["AVANZADA", "Avanzada"], ["CUMPLIDA", "Cumplida"]];
const PRIORIDADES = ["A", "B", "C", "D"];
const PRIORIDAD_NOMBRE = { A: "A · Máxima prioridad", B: "B · Importante", C: "C · Reactiva", D: "D · Observación" };
const DECISIONES = [["CONTINUAR", "Continuar"], ["ACELERAR", "Acelerar"], ["PAUSAR", "Pausar"], ["ELIMINAR", "Eliminar"]];
const TIPOS_POST = [["LOGRO", "Logro"], ["EVIDENCIA", "Evidencia"], ["VISION", "Visión"], ["META", "Meta"], ["REFLEXION", "Reflexión"]];
const VISIBILIDAD = [["PUBLICA", "Pública"], ["SEGUIDORES", "Seguidores"], ["PRIVADA", "Solo yo"]];
const FRASES_VIDA = [
  "Un día más no es un día menos: es una oportunidad nueva para subir.",
  "Hoy no tienes que llegar a la cima. Solo dar el siguiente paso.",
  "Vive este día como si fuera a quedar escrito en tu historia. Porque así será.",
  "Tu cuerpo te llevará hasta los 120 si lo cuidas hoy: muévete, hidrátate, descansa.",
  "Una larga vida se construye con días bien vividos, no con años contados.",
  "¿Qué recuerdo quieres que tenga este día dentro de 50 años?",
  "La disciplina de hoy es la libertad de tu yo de mañana.",
  "Llama, escribe o abraza a alguien que amas. Las relaciones también alargan la vida.",
  "Aprende algo nuevo hoy: una mente curiosa nunca envejece.",
  "No cuentes los días: haz que los días cuenten.",
  "Cada hábito que cumples hoy es un ladrillo de tus próximos 100 años.",
  "Agradece tres cosas antes de dormir. La gratitud es el combustible del ascenso.",
  "Si hoy caes, levántate hoy. El camino es largo y hay tiempo para volver a subir.",
  "Tu cumbre te espera. Este día es parte del camino.",
];

/** Clave de cada documento (igual que en RutaWebRepository.kt). */
const CLAVE = {
  perfil: o => String(o.id ?? 1), proposito: o => String(o.id), accion: o => String(o.id), meta_anio: o => String(o.id),
  meta_mes: o => String(o.id), balance: o => String(o.anio), agenda: o => o.fecha, mes: o => o.clave, checklist: o => o.fecha,
  ejes: o => String(o.id), respuesta: o => o.clave, coach: o => String(o.id), vision: o => String(o.id),
};

/* ======================================================================
 * Datos: la misma base que el teléfono
 * ====================================================================== */
const Store = {
  datos: {},          // tipo -> Map(clave -> objeto)
  oyentes: new Set(),
  nube: null,         // conexión real (Supabase) o null en demostración
  lista(tipo) { return [...(this.datos[tipo]?.values() || [])]; },
  get(tipo, clave) { return this.datos[tipo]?.get(String(clave)); },
  poner(tipo, clave, obj) { (this.datos[tipo] ||= new Map()).set(String(clave), obj); },
  quitar(tipo, clave) { this.datos[tipo]?.delete(String(clave)); },
  avisar() { this.oyentes.forEach(f => f()); },
  /** Guarda un documento completo (local al instante, en la nube después). */
  async guardar(tipo, obj, clave = (CLAVE[tipo] || (() => ""))(obj)) {
    this.poner(tipo, clave, obj); this.avisar();
    if (this.nube) try { await this.nube.guardar(tipo, clave, obj); } catch (e) { errorNube(e); }
  },
  /** Cambia algunos campos de un documento existente (o lo crea desde [base]). */
  async cambiar(tipo, clave, cambios, base = {}) {
    const actual = this.get(tipo, clave) || base;
    return this.guardar(tipo, { ...actual, ...cambios }, String(clave));
  },
  async borrar(tipo, clave) {
    this.quitar(tipo, clave); this.avisar();
    if (this.nube) try { await this.nube.borrar(tipo, String(clave)); } catch (e) { errorNube(e); }
  },
};
/** ¿El usuario es el correo (con o sin la @)? Un @usuario así deja el correo a la vista de todos. */
function pareceCorreo(u, correo = "") {
  const c = String(correo).toLowerCase(), local = c.split("@")[0].replace(/[^a-z0-9._]/g, "");
  return /(gmail|hotmail|outlook|yahoo|icloud|live)(\.|$)|\.(com|net|org|es|edu)$/.test(u) || (!!c && u === c.replace(/[^a-z0-9._]/g, "")) || (local.length >= 3 && u.startsWith(local) && u.length > local.length + 3 && /\./.test(u.slice(local.length)));
}
/** El motivo que da el servidor, corto y entre paréntesis, para poder diagnosticar sin abrir la consola. */
function detalleError(e) { const m = String(e?.message || e?.error_description || e?.msg || e || "").replace(/\s+/g, " ").slice(0, 140); return m ? ` (${m})` : ""; }
function errorNube(e) { console.error(e); toast("No se pudo guardar en tu cuenta." + (detalleError(e) || " Revisa tu conexión.")); }

/** Conexión con Supabase: sesión anónima del navegador, vinculada a tu cuenta desde la app. */
class Nube {
  constructor() {
    this.sb = window.supabase.createClient(CONFIG.supabaseUrl, CONFIG.supabaseAnonKey, { auth: { persistSession: true, storageKey: "rutacima-web" } });
    this.dueno = null; this.perfil = null; this.canal = null;
    /** true si se entró con correo y contraseña (sin teléfono): la cuenta es la sesión misma. */
    this.directa = false;
    // Enlace de "olvidé mi contraseña": al volver, pedir la nueva (ver 13-cuenta.js)
    this.sb.auth.onAuthStateChange(ev => { if (ev === "PASSWORD_RECOVERY") setTimeout(() => globalThis.pedirNuevaClave?.(), 300); });
  }
  async entrarConCorreo(email, clave) {
    const { data, error } = await this.sb.auth.signInWithPassword({ email, password: clave });
    if (error) throw error;
    return data.session;
  }
  /** Crea la cuenta; devuelve la sesión, o null si hay que confirmar el correo primero. */
  async registrarConCorreo(email, clave, usuario, nombre, versionLegal) {
    const { data, error } = await this.sb.auth.signUp({ email, password: clave,
      options: { data: { username: usuario, nombre, legal_version: versionLegal }, emailRedirectTo: location.origin + location.pathname } });
    if (error) throw error;
    return data.session;
  }
  async recuperarClave(email) {
    const { error } = await this.sb.auth.resetPasswordForEmail(email, { redirectTo: location.origin + location.pathname });
    if (error) throw error;
  }
  async cambiarClave(clave) {
    const { error } = await this.sb.auth.updateUser({ password: clave });
    if (error) throw error;
  }
  async sesion() {
    const { data } = await this.sb.auth.getSession();
    if (data.session) return data.session;
    const r = await this.sb.auth.signInAnonymously();
    if (r.error) throw r.error;
    return r.data.session;
  }
  async vinculo() {
    // Con correo y contraseña la cuenta es la sesión; si es anónima, la cuenta es la que la vinculó por QR
    const { data: s } = await this.sb.auth.getSession();
    const u = s.session?.user;
    if (u && !u.is_anonymous) { this.directa = true; this.dueno = u.id; this.correo = u.email || ""; return this.dueno; }
    this.directa = false;
    const { data, error } = await this.sb.rpc("mi_vinculo");
    if (error) throw error;
    this.dueno = data || null;
    return this.dueno;
  }
  async crearCodigo() {
    const { data, error } = await this.sb.rpc("crear_vinculo", { nombre: nombreNavegador() });
    if (error) throw error;
    return data;
  }
  async cargarTodo() {
    const filas = [];
    for (let desde = 0; ; desde += 1000) {
      const { data, error } = await this.sb.from("ruta_datos").select("tipo,clave,datos").eq("user_id", this.dueno)
        .order("tipo").order("clave").range(desde, desde + 999);
      if (error) throw error;
      filas.push(...data);
      if (data.length < 1000) break;
    }
    const { data: p } = await this.sb.from("profiles").select("username,nombre,avatar_url,cumbre").eq("id", this.dueno).maybeSingle();
    this.perfil = p || null;
    return filas;
  }
  async guardar(tipo, clave, datos) {
    const { error } = await this.sb.from("ruta_datos").upsert({ user_id: this.dueno, tipo, clave, datos, actualizado: new Date().toISOString() });
    if (error) throw error;
  }
  async borrar(tipo, clave) {
    const { error } = await this.sb.from("ruta_datos").delete().eq("user_id", this.dueno).eq("tipo", tipo).eq("clave", clave);
    if (error) throw error;
  }
  /** Cambios hechos en el teléfono, en vivo. */
  escuchar(cb) {
    this.canal = this.sb.channel("ruta-" + this.dueno)
      .on("postgres_changes", { event: "*", schema: "public", table: "ruta_datos", filter: `user_id=eq.${this.dueno}` }, cb)
      .subscribe();
  }
  async desvincular() {
    const s = await this.sb.auth.getSession();
    const uid = s.data.session?.user?.id;
    if (uid && !this.directa) await this.sb.from("dispositivos").delete().eq("web_uid", uid);
    if (this.canal) this.sb.removeChannel(this.canal);
    await this.sb.auth.signOut();
  }
}

function nombreNavegador() {
  const u = navigator.userAgent;
  const nav = /Edg\//.test(u) ? "Edge" : /OPR\//.test(u) ? "Opera" : /Chrome\//.test(u) ? "Chrome" : /Firefox\//.test(u) ? "Firefox" : /Safari\//.test(u) ? "Safari" : "Navegador";
  const so = /Windows/.test(u) ? "Windows" : /Mac OS X/.test(u) ? "Mac" : /Android/.test(u) ? "Android" : /CrOS/.test(u) ? "Chromebook" : /Linux/.test(u) ? "Linux" : /iPhone|iPad/.test(u) ? "iOS" : "";
  return so ? `${nav} en ${so}` : nav;
}

/* ======================================================================
 * Cálculos (los mismos de la app)
 * ====================================================================== */
const perfil = () => Store.get("perfil", 1) || { id: 1, nombre: "", cumbreFrase: "", anioInicioPlan: new Date().getFullYear(), onboardingCompleto: true };
const metaVida = () => Math.min(META_MAXIMA, Math.max(1, perfil().esperanzaVida || META_DEFECTO));
const avanceMetaMes = m => m.cumplida ? 1 : Math.min(1, numeros(m.dias).length / Math.max(1, m.objetivoDias || 20));
function cascada() {
  const props = Store.lista("proposito"), anuales = Store.lista("meta_anio"), mensuales = Store.lista("meta_mes");
  const nodoAnio = a => {
    const hijos = mensuales.filter(m => m.metaAnualId === a.id);
    return hijos.length ? hijos.map(avanceMetaMes).reduce((x, y) => x + y, 0) / hijos.length : (a.avance || 0) / 100;
  };
  const nodoProp = p => {
    const hijos = anuales.filter(a => a.propositoId === p.id);
    return hijos.length ? hijos.map(nodoAnio).reduce((x, y) => x + y, 0) / hijos.length : (p.progreso || 0) / 100;
  };
  return { nodoAnio, nodoProp, props, anuales, mensuales };
}
function recordatorio() {
  const p = perfil();
  if (!p.anioNacimiento) return null;
  const nac = new Date(p.anioNacimiento, (p.mesNacimiento || 1) - 1, 1);
  const fin = new Date(nac); fin.setFullYear(fin.getFullYear() + metaVida());
  const hoy = hoyFecha();
  const dia = Math.max(1, Math.floor((hoy - nac) / DIA_MS) + 1);
  const total = Math.floor((fin - nac) / DIA_MS);
  let edad = hoy.getFullYear() - nac.getFullYear(); if (hoy < new Date(hoy.getFullYear(), nac.getMonth(), 1)) edad--;
  return { dia, total, quedan: Math.max(0, total - dia), edad, nac, frase: FRASES_VIDA[Math.floor(Date.UTC(hoy.getFullYear(), hoy.getMonth(), hoy.getDate()) / DIA_MS) % FRASES_VIDA.length] };
}
const marcadosDe = fecha => new Set(lista(Store.get("checklist", fecha)?.marcados));
const metasDelMes = (a, m) => Store.lista("meta_mes").filter(x => x.anio === a && x.mes === m).sort((x, y) => (x.orden || 0) - (y.orden || 0) || x.id - y.id);
/** Un día "cumplido": marcaste hábitos o algún día de una meta del mes. */
function diaCumplido(f) {
  if (marcadosDe(iso(f)).size) return true;
  return metasDelMes(f.getFullYear(), f.getMonth() + 1).some(m => numeros(m.dias).includes(f.getDate()));
}
const fraseIndice = d => Math.min(364, Math.max(0, diaDelAnio(d)));
const abiertas = anio => new Set((Store.get("frases", anio)?.dias) || []);

/** Apariencia guardada en este navegador: papel BLANCO, ANTIGUO o PASTEL (los mismos de la app) y tema. */
function apariencia() {
  let papel = "blanco", tema = "sistema";
  try { papel = localStorage.getItem("rutacima-papel") || papel; tema = { light: "claro", dark: "oscuro" }[localStorage.getItem("rutacima-tema")] || tema; } catch {}
  return { papel, tema };
}
function aplicarApariencia(cambio = {}) {
  const a = { ...apariencia(), ...cambio }, r = document.documentElement;
  if (a.papel === "blanco") delete r.dataset.papel; else r.dataset.papel = a.papel;
  if (a.tema === "sistema") delete r.dataset.theme; else r.dataset.theme = a.tema === "oscuro" ? "dark" : "light";
  try {
    localStorage.setItem("rutacima-papel", a.papel);
    if (a.tema === "sistema") localStorage.removeItem("rutacima-tema"); else localStorage.setItem("rutacima-tema", r.dataset.theme);
  } catch {}
  /* La barra de estado del teléfono toma el color del papel elegido, no solo el del sistema. */
  requestAnimationFrame(() => {
    const c = getComputedStyle(r).getPropertyValue("--hoja-2").trim() || getComputedStyle(document.body).backgroundColor;
    document.querySelectorAll('meta[name="theme-color"]').forEach(m => {
      m.dataset.orig ??= m.content;
      /* Con el tema del sistema y papel blanco, cada esquema conserva su color propio. */
      m.setAttribute("content", a.tema === "sistema" && a.papel === "blanco" ? m.dataset.orig : c);
    });
  });
}

/* Filas que se desplazan de lado (secciones de una guía, filtros de fotos, chips largos): en el computador
 * se arrastran con el mouse y la rueda las mueve de lado, como en el teléfono con el dedo. */
(() => {
  const FILAS = ".tabs, .ef-filtros, .ef-chips, .lado";
  const fila = t => { const f = t?.closest?.(FILAS); return f && f.scrollWidth > f.clientWidth + 2 ? f : null; };
  let arr = null;
  document.addEventListener("pointerdown", e => {
    if (e.pointerType !== "mouse" || e.button !== 0) return;
    const f = fila(e.target); if (!f) return;
    arr = { f, x: e.clientX, izq: f.scrollLeft, movio: false };
  });
  document.addEventListener("pointermove", e => {
    if (!arr) return;
    const dx = e.clientX - arr.x;
    if (!arr.movio && Math.abs(dx) < 5) return;
    arr.movio = true; arr.f.classList.add("arrastrando");
    arr.f.scrollLeft = arr.izq - dx;
  });
  const soltar = () => {
    if (!arr) return;
    const { f, movio } = arr; arr = null; f.classList.remove("arrastrando");
    // Un arrastre no debe abrir la sección sobre la que se soltó
    if (movio) addEventListener("click", ev => { ev.stopPropagation(); ev.preventDefault(); }, { capture: true, once: true });
  };
  document.addEventListener("pointerup", soltar); document.addEventListener("pointercancel", soltar);
  document.addEventListener("wheel", e => {
    const f = fila(e.target); if (!f || Math.abs(e.deltaX) > Math.abs(e.deltaY)) return;
    const antes = f.scrollLeft; f.scrollLeft += e.deltaY;
    if (f.scrollLeft !== antes) e.preventDefault();
  }, { passive: false });
  // Bordes que se desvanecen cuando hay más a un lado
  const marcar = f => { f.classList.toggle("mas-izq", f.scrollLeft > 2); f.classList.toggle("mas-der", f.scrollLeft + f.clientWidth < f.scrollWidth - 2); };
  document.addEventListener("scroll", e => { if (e.target?.matches?.(FILAS)) marcar(e.target); }, true);
  new MutationObserver(() => document.querySelectorAll(FILAS).forEach(f => {
    marcar(f);
    // La sección abierta queda a la vista
    const act = f.querySelector('[aria-pressed="true"]');
    if (act && !f.dataset.centrada) { f.dataset.centrada = "1"; f.scrollLeft = act.offsetLeft - (f.clientWidth - act.offsetWidth) / 2; marcar(f); }
  })).observe(document.documentElement, { childList: true, subtree: true });
})();
