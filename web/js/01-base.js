/* Rutaalacima Web: la misma app del teléfono en el computador.
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
function toast(t) {
  const el = $("toast"); el.textContent = t; el.hidden = false;
  clearTimeout(toast.t); toast.t = setTimeout(() => (el.hidden = true), 3200);
}

/* Íconos de Material Design */
const P = {
  ruta: "M14 6l-3.75 5 2.85 3.8-1.6 1.2C9.81 13.75 7 10 7 10l-6 8h22L14 6z",
  hoy: "M19 3h-1V1h-2v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11zM7 10h5v5H7z",
  metas: "M14.4 6L14 4H5v17h2v-7h5.6l.4 2h7V6z",
  comunidad: "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z",
  aprende: "M18 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 4h5v8l-2.5-1.5L6 12V4z",
  coach: "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z",
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
  cerrar: "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z",
  cordada: "M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z",
  check: "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z",
  copiar: "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z",
  foto: "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z",
};
const ic = (n, cls = "i") => `<svg class="${cls}" viewBox="0 0 24 24" aria-hidden="true"><path d="${P[n]}"/></svg>`;

/* ======================================================================
 * El método (igual que en la app)
 * ====================================================================== */
const EJES = [
  ["VOL", "Voluntad", "voluntad"], ["MAE", "Maestría", "maestria"], ["VOZ", "Voz", "voz"],
  ["VAL", "Valor", "valor"], ["EVO", "Evolución", "evolucion"], ["TRA", "Trascendencia", "trascendencia"],
];
const NOMBRE_EJE = Object.fromEntries(EJES.map(([c, n]) => [c, n]));
const COLOR_EJE = { VOL: "#8E3B26", MAE: "#5C4A8A", VOZ: "#2F6F7A", VAL: "#8A6A1F", EVO: "#3F7A4A", TRA: "#6B2A1A" };
const HABITOS = [
  ["VOL1", "VOL", "Revisé mi visión/propósito (1 min)"], ["VOL2", "VOL", "Hice algo hoy que me acerca a mi cumbre"], ["VOL3", "VOL", "Dije NO a algo que me aleja de mi cumbre"],
  ["MAE1", "MAE", "Practiqué/perfeccioné una habilidad clave (mín. 30 min)"], ["MAE2", "MAE", "Aprendí algo nuevo relacionado con mi cumbre"], ["MAE3", "MAE", "Salí de mi zona de confort hoy"],
  ["VOZ1", "VOZ", "Expresé mi opinión auténtica en al menos una situación"], ["VOZ2", "VOZ", "Compartí mi trabajo/mensaje de alguna forma"], ["VOZ3", "VOZ", "No me callé algo importante por miedo"],
  ["VAL1", "VAL", "Trabajé en algo que genera valor económico"], ["VAL2", "VAL", "Cobré/pedí lo que merezco sin culpa"], ["VAL3", "VAL", "Invertí en mi crecimiento (tiempo o dinero)"],
  ["EVO1", "EVO", "Reflexioné sobre algo que aprendí hoy (journaling 5 min)"], ["EVO2", "EVO", "Pedí feedback o perspectiva externa"], ["EVO3", "EVO", "Ajusté un hábito o estrategia que no funciona"],
  ["TRA1", "TRA", "Ayudé a alguien sin esperar nada a cambio"], ["TRA2", "TRA", "Conecté con algo mayor que yo (naturaleza, arte, espiritualidad)"], ["TRA3", "TRA", "Trabajé en mi legado o contribución al mundo"],
];
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
function errorNube(e) { console.error(e); toast("No se pudo guardar en tu cuenta. Revisa tu conexión."); }

/** Conexión con Supabase: sesión anónima del navegador, vinculada a tu cuenta desde la app. */
class Nube {
  constructor() {
    this.sb = window.supabase.createClient(CONFIG.supabaseUrl, CONFIG.supabaseAnonKey, { auth: { persistSession: true, storageKey: "rutacima-web" } });
    this.dueno = null; this.perfil = null; this.canal = null;
  }
  async sesion() {
    const { data } = await this.sb.auth.getSession();
    if (data.session) return data.session;
    const r = await this.sb.auth.signInAnonymously();
    if (r.error) throw r.error;
    return r.data.session;
  }
  async vinculo() {
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
    if (uid) await this.sb.from("dispositivos").delete().eq("web_uid", uid);
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
