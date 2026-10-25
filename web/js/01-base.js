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
const LOGO = CONFIG.logo || "sello.png";

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
  finanzas: "M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z",
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
  hoy: "M19 3h-1V1h-2v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11zM7 10h5v5H7z",
  metas: "M14.4 6L14 4H5v17h2v-7h5.6l.4 2h7V6z",
  comunidad: "M12 1.8c.6 2.6 4.4 4.3 4.4 8.3 0 2.7-2 4.7-4.4 4.7S7.6 12.8 7.6 10.1c0-1.9 1-3.2 1.9-4.1.2 1.5 1 2.5 2 2.7-.5-2.2-.4-4.6.5-6.9zm0 7.4c-.9 1-1.6 1.9-1.6 3 0 1 .7 1.8 1.6 1.8s1.6-.8 1.6-1.8c0-1.1-.7-2-1.6-3zM3.4 17.4c-.3-.6 0-1.3.6-1.5l.2-.1 15.8 4.3c.6.2.9.8.8 1.4-.2.6-.8 1-1.4.8L4.1 18.1a1 1 0 0 1-.7-.7zM20.6 17.4c.3-.6 0-1.3-.6-1.5l-.2-.1L4 20.1c-.6.2-.9.8-.8 1.4.2.6.8 1 1.4.8l15.3-4.2c.3-.1.6-.4.7-.7z",
  personas: "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z",
  aprende: "M2 8.2c3.1-1 6.3-.6 9.2 1.3V21c-2.9-1.7-6.1-2-9.2-1zM22 8.2c-3.1-1-6.3-.6-9.2 1.3V21c2.9-1.7 6.1-2 9.2-1zM8.4 7.1 12 1.6l3.6 5.5c-1.2-.3-2.4-.2-3.6.5-1.2-.7-2.4-.8-3.6-.5z",
  coach: "M11.2 12.6h1.6V20h-1.6zM11.5 13.2C11.4 9.6 8.6 7 4.6 7c0 3.8 2.9 6.2 6.9 6.2zM12.5 11.6c0-4 3-7 7-7 0 4-3 7-7 7zM6 19.6c0-.6.4-1 1-1h10c.6 0 1 .4 1 1v1.4H6zM5.6 1.4l.6 1.6 1.6.6-1.6.6-.6 1.6L5 4.2l-1.6-.6L5 3z",
  ia: "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z",
  frases: "M6 17h3l2-4V7H5v6h3zm8 0h3l2-4V7h-6v6h3z",
  perfil: "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
  buscar: "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
  enviar: "M2.01 21L23 12 2.01 3 2 10l15 2-15 2z",
  volver: "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z",
  candado: "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z",
  bandera: "M14.4 6L14 4H5v17h2v-7h5.6l.4 2h7V6z",
  impulso: "M11 21h-1l1-7H7.5c-.58 0-.57-.32-.38-.66.19-.34.05-.08.07-.12C8.48 10.94 10.42 7.54 13 3h1l-1 7h3.5c.49 0 .56.33.47.51l-.07.15C12.96 17.55 11 21 11 21z",
  tema: "M20 8.69V4h-4.69L12 .69 8.69 4H4v4.69L.69 12 4 15.31V20h4.69L12 23.31 15.31 20H20v-4.69L23.31 12 20 8.69zM12 18c-.89 0-1.74-.2-2.5-.55C11.56 16.5 13 14.42 13 12s-1.44-4.5-3.5-5.45C10.26 6.2 11.11 6 12 6c3.31 0 6 2.69 6 6s-2.69 6-6 6z",
  salir: "M10.09 15.59L11.5 17l5-5-5-5-1.41 1.41L12.67 11H3v2h9.67l-2.58 2.59zM19 3H5c-1.11 0-2 .9-2 2v4h2V5h14v14H5v-4H3v4c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z",
  pin: "M16 9V4h1c.55 0 1-.45 1-1s-.45-1-1-1H7c-.55 0-1 .45-1 1s.45 1 1 1h1v5c0 1.66-1.34 3-3 3v2h5.97v7l1 1 1-1v-7H19v-2c-1.66 0-3-1.34-3-3z",
  mas: "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z",
  borrar: "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z",
  // Brújula (Material "explore", contorno): Publicar en la barra
  brujula: "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-5.5-2.5l7.51-3.49L17.5 6.5 9.99 9.99 6.5 17.5zm5.5-6.6c.61 0 1.1.49 1.1 1.1s-.49 1.1-1.1 1.1-1.1-.49-1.1-1.1.49-1.1 1.1-1.1z",
  publicar: "M1,21L8.5,10L11.5,14.2L14.6,9.6L23,21ZM14,2.4h1.25v7.4h-1.25zM15.25,2.4L20.4,4.2L15.25,6ZM4.2,2.4h1.6v2.4h2.4v1.6H5.8v2.4H4.2V6.4H1.8V4.8h2.4z",
  comentario: "M21.99 4c0-1.1-.89-2-1.99-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14l4 4-.01-18z",
  mensajes: "M20 2H4c-1.1 0-1.99.9-1.99 2L2 22l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 9h12v2H6V9zm8 5H6v-2h8v2zm4-6H6V6h12v2z",
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
const IC_PAR = new Set(["comunidad", "aprende", "ejes", "puntos"]);
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
}
