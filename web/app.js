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
const HORIZONTES = [5, 10, 15, 20];
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

/* ======================================================================
 * Navegación (igual que WhatsApp Web: riel, lista y detalle)
 * ====================================================================== */
const SECCIONES = [
  ["ruta", "Mi ruta"], ["hoy", "Hoy"], ["metas", "Metas"], ["comunidad", "Comunidad"],
  ["aprende", "Aprende"], ["coach", "Coach"], ["mensajes", "Mensajes"], ["frases", "Mis frases"], ["perfil", "Perfil"],
];
/** En el teléfono la barra es igual a la de la app: 5 secciones y el botón de publicar en medio.
 *  Metas, Coach y Mis frases se abren desde arriba (destello y menú) y desde Perfil. */
const SOLO_PC = new Set(["metas", "coach", "mensajes", "frases"]);
const estado = { sec: "ruta", sel: null, filtro: "todo", busqueda: "", wbSec: 0, feed: null, feedFiltro: "PARA_TI", post: null, wb: {}, vistaCom: (() => { try { return localStorage.getItem("rutacima-vista") || "lista"; } catch { return "lista"; } })() };

function pintarRiel() {
  const sellada = !abiertas(new Date().getFullYear()).has(fraseIndice(hoyFecha()));
  $("riel").innerHTML =
    `<img class="sello" src="${LOGO}" alt="Rutaalacima">` +
    SECCIONES.map(([k, n], i) => (i === 3 ? `<button class="rb rb-publicar" data-ir="comunidad" data-arg="publicar" aria-label="Publicar">${ic("publicar")}<span class="tip">Publicar · planta tu bandera</span></button>` : "") +
      `<button class="rb ${SOLO_PC.has(k) ? "solo-pc" : ""}" data-sec="${k}" aria-label="${n}" ${estado.sec === k ? 'aria-current="page"' : ""}>${ic(k)}<span class="tip">${n}</span>${(k === "frases" && sellada) || (k === "mensajes" && typeof MS !== "undefined" && noLeidosTotal()) ? '<span class="punto"></span>' : ""}</button>`).join("") +
    `<span class="esp"></span>
     <button class="rb solo-pc" data-acc="tema" aria-label="Tema claro u oscuro">${ic("tema")}<span class="tip">Tema claro u oscuro</span></button>
     <button class="rb solo-pc" data-acc="salir" aria-label="${Store.nube ? "Cerrar sesión en este computador" : "Salir de la demostración"}">${ic("salir")}<span class="tip">${Store.nube ? "Cerrar sesión en este computador" : "Salir de la demostración"}</span></button>`;
}
function ir(sec, sel = null) {
  estado.sec = sec; estado.sel = sel; estado.filtro = "todo"; estado.busqueda = "";
  $("app").classList.toggle("con-detalle", sel !== null);
  pintarTodo();
}
function elegir(sel) {
  estado.sel = sel;
  $("app").classList.toggle("con-detalle", sel !== null);
  pintarLista(); pintarDetalle();
}
function pintarTodo() { pintarRiel(); pintarLista(); pintarDetalle(); }

/* Si estás escribiendo, el detalle espera a que termines antes de redibujarse */
let detallePendiente = false;
function enEdicion() { const a = document.activeElement; return a && $("detalle").contains(a) && /INPUT|TEXTAREA|SELECT/.test(a.tagName) && a.type !== "checkbox" && a.type !== "range"; }
function refrescar() {
  pintarRiel(); pintarLista();
  if (enEdicion()) detallePendiente = true; else pintarDetalle();
}
document.addEventListener("focusout", () => setTimeout(() => { if (detallePendiente && !enEdicion()) { detallePendiente = false; pintarDetalle(); } }, 0));

/* ---------- Lista ---------- */
function cabLista(titulo, { chips = [], buscar = true, nuevo = null } = {}) {
  const atajos = `<span class="solo-movil atajos">${estado.sec !== "metas" ? `<button class="rb" data-sec="metas" aria-label="Metas">${ic("metas")}</button>` : ""}${estado.sec !== "frases" ? `<button class="rb" data-sec="frases" aria-label="Mis frases">${ic("frases")}</button>` : ""}${estado.sec === "comunidad" ? `<button class="rb" data-sec="mensajes" aria-label="Mensajes">${ic("mensajes")}</button>` : ""}<button class="rb" data-sec="coach" aria-label="Coach" style="color:var(--oro)">${ic("coach")}</button></span>`;
  return `<div class="lista-cab"><div class="cab-lista-btn"><h1>${titulo}</h1><span style="display:flex;gap:4px;align-items:center">${atajos}${nuevo ? `<button class="nuevo" data-sel="${nuevo[0]}" aria-label="${nuevo[1]}" title="${nuevo[1]}">${ic("mas")}</button>` : ""}</span></div>
    ${buscar ? `<label class="buscar">${ic("buscar")}<input id="busq" type="search" placeholder="Buscar" value="${esc(estado.busqueda)}" aria-label="Buscar"></label>` : ""}
    ${chips.length ? `<div class="chips">${chips.map(([k, n]) => `<button class="chip" data-filtro="${k}" aria-pressed="${estado.filtro === k}">${n}</button>`).join("")}</div>` : ""}</div>`;
}
const item = (sel, avatar, titulo, hora, sub, extra = "") =>
  `<button class="item" data-sel="${esc(sel)}" aria-selected="${estado.sel === sel}">${avatar}<span class="txt">
     <span class="t1"><span>${titulo}</span><span class="hora">${hora}</span></span>
     <span class="t2"><span>${sub}</span>${extra}</span></span></button>`;
const av = (txt, color) => `<span class="avatar" style="background:${color}">${txt}</span>`;
const avSello = () => `<img class="avatar" src="${LOGO}" alt="">`;
const coincide = s => !estado.busqueda || String(s).toLowerCase().includes(estado.busqueda.toLowerCase());
const grupo = t => `<div class="grupo">${t}</div>`;
const pctTxt = x => `${Math.round(x * 100)}%`;

function pintarLista() {
  const f = LISTAS[estado.sec];
  $("lista").innerHTML = f ? f() : "";
}
const LISTAS = {
  ruta() {
    const r = recordatorio(), p = perfil();
    let h = cabLista("Mi ruta", { chips: [["todo", "Toda la vida"], ["plan", "Años del plan"], ["metas", "Con metas"]] }) + `<div class="items">`;
    if (!r) return h + `<p class="vacio-mini">Agrega tu fecha de nacimiento en Perfil para ver tu vida año por año.</p><div class="botones" style="justify-content:center"><button class="btn lleno" data-ir="perfil" data-arg="perfil">Ir a mi perfil</button></div></div>`;
    const anioHoy = new Date().getFullYear(), nac = p.anioNacimiento, ultimo = nac + metaVida();
    const conMetas = new Map();
    Store.lista("meta_anio").forEach(m => conMetas.set(m.anio, (conMetas.get(m.anio) || 0) + 1));
    const finPlan = (p.anioInicioPlan || anioHoy) + Math.max(5, ...Store.lista("proposito").map(x => x.horizonte || 5)) - 1;
    if (estado.filtro === "todo" && coincide("camino vida")) h += item("vida", avSello(), `Camino hacia los ${metaVida()} años`, "hoy", `Día ${r.dia.toLocaleString("es")} de tu vida`, ic("pin", "i fijado"));
    let anios = [];
    for (let a = anioHoy; a <= ultimo; a++) anios.push(a);
    for (let a = anioHoy - 1; a >= nac; a--) anios.push(a);
    if (estado.filtro === "plan") anios = anios.filter(a => a >= (p.anioInicioPlan || anioHoy) && a <= finPlan).sort((x, y) => x - y);
    if (estado.filtro === "metas") anios = anios.filter(a => conMetas.has(a)).sort((x, y) => x - y);
    let g = "";
    for (const a of anios) {
      const gr = a === anioHoy ? "Este año" : a > anioHoy ? "Por vivir" : "Vividos";
      if (estado.filtro === "todo" && gr !== g) { h += grupo(gr); g = gr; }
      const metas = Store.lista("meta_anio").filter(m => m.anio === a);
      const sub = metas[0]?.titulo || (a < anioHoy ? "Año vivido" : a <= finPlan ? `Año ${a - (p.anioInicioPlan || anioHoy) + 1} de tu plan` : "Sin metas aún");
      if (!coincide(`${a} ${sub}`)) continue;
      const color = a < anioHoy ? "var(--burdeos)" : a === anioHoy ? "var(--oro)" : "#B8A99C";
      h += item("a:" + a, av(a - nac, color), `${a} · ${a - nac} años`, a === anioHoy ? "ahora" : "", esc(sub), metas.length ? `<span class="badge suave">${metas.length}</span>` : "");
    }
    return h + `</div>`;
  },
  hoy() {
    const hoy = hoyFecha(), a = hoy.getFullYear(), m = hoy.getMonth() + 1;
    let h = cabLista("Hoy", { buscar: false }) + `<div class="items">`;
    h += item("d:" + iso(hoy), av(hoy.getDate(), "var(--burdeos)"), "Tu día", "hoy", `${marcadosDe(iso(hoy)).size} de 18 hábitos · ${MESES[hoy.getMonth()]}`, ic("pin", "i fijado"));
    h += grupo(`Metas de ${MESES[m - 1]}`);
    const ms = metasDelMes(a, m);
    if (!ms.length) h += `<p class="vacio-mini">Aún no hay metas este mes.</p>`;
    ms.forEach(x => {
      const hechoHoy = numeros(x.dias).includes(hoy.getDate());
      h += item("mm:" + x.id, av(pctTxt(avanceMetaMes(x)), COLOR_EJE[x.eje] || "var(--burdeos)"), esc(x.texto), hechoHoy ? "✓ hoy" : "", NOMBRE_EJE[x.eje] || "Meta del mes");
    });
    h += grupo("Ayer y antes");
    for (let k = 1; k <= 6; k++) {
      const d = new Date(hoy); d.setDate(d.getDate() - k);
      h += item("d:" + iso(d), av(d.getDate(), diaCumplido(d) ? "var(--oro)" : "#B8A99C"), mayus(d.toLocaleDateString("es", { weekday: "long" })), `${d.getDate()} ${MES_C[d.getMonth()]}`, `${marcadosDe(iso(d)).size} hábitos`);
    }
    return h + `</div>`;
  },
  metas() {
    const c = cascada(), anioHoy = new Date().getFullYear();
    const anios = [...new Set(c.anuales.map(x => x.anio))].sort((x, y) => x - y);
    let h = cabLista("Metas", { chips: [["todo", "Todo"], ["prop", "Propósitos"], ["anio", `${anioHoy}`], ["mes", "Este mes"]], nuevo: ["nuevo", "Nueva meta"] }) + `<div class="items">`;
    const f = estado.filtro;
    if (f === "todo" || f === "prop") {
      h += grupo("Propósitos a largo plazo");
      const ps = c.props.filter(p => coincide(p.titulo)).sort((x, y) => (x.orden || 0) - (y.orden || 0) || x.id - y.id);
      if (!ps.length) h += `<p class="vacio-mini">Todavía no hay propósitos.</p>`;
      ps.forEach(p => { h += item("p:" + p.id, av(pctTxt(c.nodoProp(p)), COLOR_EJE[p.eje] || "var(--burdeos)"), esc(p.titulo), `${p.horizonte || 5} años`, `Prioridad ${p.prioridad || "B"}`); });
    }
    if (f === "todo" || f === "anio") {
      for (const a of (f === "anio" ? [anioHoy] : anios)) {
        const xs = c.anuales.filter(x => x.anio === a && coincide(x.titulo));
        if (!xs.length && f !== "anio") continue;
        h += grupo(`Metas de ${a}`);
        if (!xs.length) h += `<p class="vacio-mini">Sin metas para ${a}.</p>`;
        xs.forEach(x => { h += item("a:" + x.id, av(pctTxt(c.nodoAnio(x)), COLOR_EJE[x.eje] || "var(--oro)"), esc(x.titulo), x.prioridad || "", ESTADOS.find(e => e[0] === x.estado)?.[1] || ""); });
      }
    }
    if (f === "todo" || f === "mes") {
      const hoy = hoyFecha();
      const xs = metasDelMes(hoy.getFullYear(), hoy.getMonth() + 1).filter(x => coincide(x.texto));
      h += grupo(`Metas de ${MESES[hoy.getMonth()]}`);
      if (!xs.length) h += `<p class="vacio-mini">Sin metas este mes.</p>`;
      xs.forEach(x => { h += item("m:" + x.id, av(pctTxt(avanceMetaMes(x)), COLOR_EJE[x.eje] || "var(--burdeos)"), esc(x.texto), "", `${numeros(x.dias).length} de ${x.objetivoDias || 20} días`); });
    }
    return h + `</div>`;
  },
  comunidad() {
    let h = cabLista("Comunidad", { buscar: false, nuevo: ["publicar", "Publicar"] });
    h += `<div class="lista-cab" style="padding-top:0"><div class="segmentos" role="group" aria-label="Cómo ver la comunidad">${[["lista", "Lista"], ["cimas", "Cimas"]].map(([k, n]) => `<button data-vista="${k}" aria-pressed="${estado.vistaCom === k && estado.sel === "feed"}">${k === "cimas" ? ic("ruta") : ic("comunidad")} ${n}</button>`).join("")}</div>
      <div class="chips">${[["PARA_TI", "Para ti"], ["VISION", "Visión"], ["SIGUIENDO", "Siguiendo"]].map(([k, n]) => `<button class="chip" data-feed="${k}" aria-pressed="${estado.feedFiltro === k}">${n}</button>`).join("")}</div></div><div class="items">`;
    if (!estado.feed) { cargarFeed(); return h + `<p class="vacio-mini">Cargando…</p></div>`; }
    if (!estado.feed.length) h += `<p class="vacio-mini">${estado.feedFiltro === "SIGUIENDO" ? "Todavía no sigues a nadie." : "Aún no hay publicaciones."}</p>`;
    estado.feed.forEach(p => {
      const ini = (p.autorNombre || "?").split(" ").map(x => x[0]).join("").slice(0, 2).toUpperCase();
      h += item("post:" + p.id, av(esc(ini), COLOR_EJE[p.eje] || "var(--burdeos)"), esc(p.autorNombre), hace(p.creadoEn), esc(p.texto), `<span class="badge ${p.yoImpulse ? "" : "suave"}">${p.impulsos}</span>`);
    });
    return h + `</div>`;
  },
  aprende() {
    const CAT = { ruta: "Tu ruta", herramientas: "Herramientas", bonos: "Bonos", lecturas: "Lecturas", facilitador: "Facilitadores" };
    let h = cabLista("Aprende", { chips: [["todo", "Todo"], ["ruta", "Ruta"], ["herramientas", "Herramientas"], ["bonos", "Bonos"], ["lecturas", "Lecturas"]] }) + `<div class="items">`;
    if (!Contenido.indice) { Contenido.cargarIndice(); return h + `<p class="vacio-mini">Cargando las guías…</p></div>`; }
    const resp = Store.lista("respuesta");
    let g = "";
    [...Contenido.indice].sort((a, b) => Object.keys(CAT).indexOf(a.category) - Object.keys(CAT).indexOf(b.category) || a.order - b.order)
      .filter(x => (estado.filtro === "todo" || x.category === estado.filtro) && coincide(x.title + x.subtitle)).forEach(x => {
        if (x.category !== g) { h += grupo(CAT[x.category] || x.category); g = x.category; }
        const n = resp.filter(r => r.workbookId === x.id && !r.clave.includes("#")).length;
        h += item("wb:" + x.id, av(esc(x.title.trim()[0]), x.category === "bonos" ? "var(--oro)" : "var(--burdeos)"), esc(x.title), n ? `${n}/${x.inputs}` : "", esc(x.subtitle));
      });
    return h + `</div>`;
  },
  coach() {
    const ms = mensajesCoach(), u = ms[ms.length - 1];
    let h = cabLista("Coach", { buscar: false }) + `<div class="items">`;
    h += item("coach", av(ic("coach"), "var(--burdeos)"), "Coach Rutaalacima", u ? horaDe(u.creadoEn) : "", esc(u ? u.texto : "Pregúntame por tu ruta"));
    h += grupo("Atajos");
    [["Perdí la motivación", "Perdí la motivación"], ["Planea mi semana", "Planea mi semana"], ["¿Qué eje debo trabajar?", "¿Qué eje debo trabajar?"], ["Divide mi meta anual en pasos", "Divide mi meta anual en pasos"]].forEach(([t, q]) => {
      h += `<button class="item" data-pregunta="${esc(q)}">${av(ic("coach"), "var(--oro)")}<span class="txt"><span class="t1"><span>${t}</span></span><span class="t2"><span>Preguntar al coach</span></span></span></button>`;
    });
    return h + `</div>`;
  },
  frases() {
    let h = cabLista("Mis frases", { chips: [["todo", "Todas"], ["abiertas", "Abiertas"]] }) + `<div class="items">`;
    if (!Contenido.frases) { Contenido.cargarFrases(); return h + `<p class="vacio-mini">Cargando…</p></div>`; }
    const hoy = hoyFecha();
    for (let k = 0; k < 60; k++) {
      const d = new Date(hoy); d.setDate(d.getDate() - k);
      const abierta = abiertas(d.getFullYear()).has(fraseIndice(d));
      if (estado.filtro === "abiertas" && !abierta) continue;
      const fr = Contenido.frases[fraseIndice(d)];
      if (!coincide(abierta ? fr.t : "")) continue;
      h += item("f:" + iso(d), abierta ? av(ic("frases"), "var(--oro)") : avSello(), k === 0 ? "Frase de hoy" : `Día ${diaDelAnio(d) + 1}`, `${d.getDate()} ${MES_C[d.getMonth()]}`,
        abierta ? esc(fr.t) : `${ic("candado", "i fijado")} Sellada`, k === 0 && !abierta ? `<span class="badge">1</span>` : "");
    }
    return h + `</div>`;
  },
  perfil() {
    const p = perfil(), ult = ultimaEvaluacion();
    let h = cabLista("Perfil", { buscar: false }) + `<div class="items">`;
    h += item("perfil", av(esc((p.nombre || "Y")[0].toUpperCase()), "var(--burdeos)"), esc(p.nombre || "Tu perfil"), "", esc(Store.nube?.perfil?.username ? "@" + Store.nube.perfil.username : p.cumbreFrase || "Nombre, cumbre y fecha de nacimiento"));
    h += item("ejes", av(ic("comunidad"), "var(--oro)"), "Mis ejes", ult ? new Date(ult.fecha).toLocaleDateString("es") : "", ult ? "Última evaluación" : "Evalúa tus 6 ejes");
    const vs = Store.lista("vision");
    h += item("vision", av(ic("coach"), "var(--oro)"), "Mi vision board", vs.length ? `${vs.filter(v => v.publicacionId).length}/${vs.length}` : "", vs.length ? "Llénalo con tus fotos" : "Ármalo con IA y tus datos");
    h += item("publicaciones", av(ic("foto"), "var(--burdeos)"), "Mis publicaciones", "", "Tu diario de vida");
    h += grupo("Más");
    h += item("ir-metas", av(ic("metas"), "var(--burdeos)"), "Metas", "", "Propósitos, metas del año y del mes");
    h += item("ir-frases", av(ic("frases"), "var(--oro)"), "Mis frases", "", "Las frases que ya abriste");
    h += item("ir-coach", av(ic("coach"), "var(--burdeos)"), "Coach", "", "Pregúntale por tu ruta");
    h += item("ir-mensajes", av(ic("mensajes"), "var(--burdeos)"), "Mensajes", "", "Tus conversaciones 1 a 1");
    h += item("ir-coachvida", av(ic("cumbre"), "var(--oro)"), "Coach de vida", "", "Una persona que te acompaña");
    h += item("cuenta", av(ic("salir"), "#8A7B70"), Store.nube ? "Este computador" : "Demostración", "", Store.nube ? nombreNavegador() : "Ruta de ejemplo");
    return h + `</div>`;
  },
};

/* ======================================================================
 * Detalle
 * ====================================================================== */
const cab = (avatar, titulo, sub, acciones = "") =>
  `<header class="det-cab"><button class="rb volver" data-acc="volver" aria-label="Volver">${ic("volver")}</button>${avatar}<div class="txt"><h2>${titulo}</h2><p>${sub}</p></div>${acciones}</header>`;
const cuerpo = (html, ancho = "") => `<div class="det-cuerpo"><div class="ancho" style="gap:18px;${ancho}">${html}</div></div>`;
const hoja = (html, clase = "") => `<div class="hoja ${clase}">${html}</div>`;
const campo = (etq, control) => `<label class="campo"><span>${etq}</span>${control}</label>`;
/** Control enlazado a un campo de un documento: se guarda al salir del campo. */
const bind = (tipo, clave, campoN, conv = "") => `data-bind="${tipo}|${esc(clave)}|${campoN}|${conv}"`;
const txt = (tipo, clave, campoN, valor, ph = "") => `<input type="text" ${bind(tipo, clave, campoN)} value="${esc(valor)}" placeholder="${esc(ph)}">`;
const area = (tipo, clave, campoN, valor, ph = "") => `<textarea ${bind(tipo, clave, campoN)} placeholder="${esc(ph)}">${esc(valor)}</textarea>`;
const sel = (tipo, clave, campoN, valor, opciones, conv = "") =>
  `<select ${bind(tipo, clave, campoN, conv)}>${opciones.map(([v, n]) => `<option value="${esc(v)}" ${String(v) === String(valor ?? "") ? "selected" : ""}>${esc(n)}</option>`).join("")}</select>`;
const opcionesEje = [["", "Sin eje"], ...EJES.map(([c, n]) => [c, n])];

function vacio() {
  return `<div class="vacio"><div><img src="${LOGO}" alt="Sello de Rutaalacima"><h2>Rutaalacima Web</h2>
    <p>La misma app de tu teléfono, en pantalla grande. Elige un año de tu ruta, tu día de hoy o una de tus metas.</p>
    <p class="pie suave">${ic("candado")} ${Store.nube ? "Conectada a tu cuenta: lo que cambies aquí aparece en tu teléfono." : "Demostración: los cambios se quedan en esta pestaña."}</p></div></div>`;
}

function pintarDetalle() {
  const s = estado.sec, v = estado.sel;
  let h;
  if (v === null) h = s === "coach" ? (estado.sel = "coach", DET.coach()) : s === "mensajes" ? vacioMensajes() : vacio();
  else {
    const [t, ...resto] = String(v).split(":"); const arg = resto.join(":");
    const f = DET[`${s}.${t}`] || DET[t];
    h = f ? f(arg) : vacio();
  }
  const mismo = pintarDetalle.ultimo === `${s}|${v}|${estado.vistaCom}`;
  const antes = mismo ? [...$("detalle").querySelectorAll(".det-cuerpo, .cimas")].map(x => x.scrollTop) : [];
  $("detalle").innerHTML = h;
  pintarDetalle.ultimo = `${s}|${v}|${estado.vistaCom}`;
  $("detalle").querySelectorAll(".det-cuerpo, .cimas").forEach((x, i) => { if (antes[i] !== undefined) x.scrollTop = antes[i]; });
  const abajo = $("detalle").querySelector(".det-cuerpo.abajo"); if (abajo) abajo.scrollTop = abajo.scrollHeight;
  pintarAudio();
}

const DET = {
  /* ---------- Mi ruta: vida → año → mes → día ---------- */
  vida() {
    const r = recordatorio();
    if (!r) return vacio();
    const nac = r.nac.getFullYear(), anioHoy = new Date().getFullYear();
    const conMetas = new Set(Store.lista("meta_anio").map(m => m.anio));
    let puntos = "";
    for (let i = 0; i < metaVida(); i++) {
      const a = nac + i, cls = a < anioHoy ? "vivido" : a === anioHoy ? "actual" : "futuro";
      puntos += `<button class="pv ${cls}" data-sel="a:${a}" aria-label="${a}, ${i} años">${i % 10 === 0 ? i : ""}${conMetas.has(a) ? ic("bandera", "bandera") : ""}</button>`;
    }
    return cab(avSello(), `Camino hacia los ${metaVida()} años`, `${esc(perfil().nombre || "Tú")} · ${r.edad} años`) + cuerpo(
      hoja(`<div class="recordatorio"><div><div class="etiqueta">Recordatorio del día</div>
        <div style="display:flex;gap:28px;flex-wrap:wrap;margin-top:10px">
          <div><div class="cifra">${r.dia.toLocaleString("es")}</div><div class="suave">día de tu vida</div></div>
          <div><div class="cifra">${r.quedan.toLocaleString("es")}</div><div class="suave">días hasta los ${metaVida()}</div></div></div></div>
        <div style="text-align:right"><div class="cifra" style="color:var(--oro)">${Math.round(r.dia / r.total * 100)}%</div><div class="suave">del camino</div></div></div>
        <div class="barra" style="margin-top:14px"><i style="width:${Math.min(100, r.dia / r.total * 100)}%"></i></div>
        <p class="frase-vida">${r.frase}</p>`) +
      hoja(`<h3>Tu vida en puntos</h3><p class="suave" style="margin:0 0 16px">Un punto por año. Toca uno para abrirlo mes a mes. Las banderas marcan años con metas.</p>
        <div class="puntos">${puntos}</div>
        <div class="leyenda" style="margin-top:16px"><span><i style="background:var(--burdeos)"></i>Vivido</span><span><i style="background:var(--oro)"></i>Este año</span><span><i style="background:var(--hoja);border:1px solid var(--linea)"></i>Por vivir</span><span>${ic("bandera", "i fijado")}Año con metas</span></div>`));
  },
  "ruta.a"(arg) {
    const a = Number(arg), p = perfil(), edad = p.anioNacimiento ? a - p.anioNacimiento : null;
    const hoy = hoyFecha(), c = cascada();
    let meses = "";
    for (let m = 1; m <= 12; m++) {
      const n = diasEnMes(a, m), ini = (new Date(a, m - 1, 1).getDay() + 6) % 7;
      let dias = "", cuenta = 0, total = 0;
      for (let k = 0; k < ini; k++) dias += `<i class="dia vacio-d"></i>`;
      for (let d = 1; d <= n; d++) {
        const f = new Date(a, m - 1, d), pasado = f <= hoy, esHoy = f.getTime() === hoy.getTime();
        const ok = pasado && diaCumplido(f);
        if (pasado) { total++; if (ok) cuenta++; }
        dias += `<i class="dia ${ok ? "hecho" : ""} ${pasado && !esHoy ? "pasado" : ""} ${esHoy ? "hoy" : ""}" title="${d} de ${MESES[m - 1]}"></i>`;
      }
      const nm = metasDelMes(a, m).length;
      meses += `<button class="hoja mes clic" data-sel="m:${a}-${String(m).padStart(2, "0")}" style="text-align:left"><h4>${mayus(MESES[m - 1])} <span>${total ? cuenta + "/" + total : nm ? nm + " metas" : ""}</span></h4><div class="dias">${dias}</div></button>`;
    }
    const metas = c.anuales.filter(x => x.anio === a);
    return cab(av(edad ?? "·", a < hoy.getFullYear() ? "var(--burdeos)" : a === hoy.getFullYear() ? "var(--oro)" : "#B8A99C"),
      `${a}${edad !== null ? ` · ${edad} años` : ""}`, a < hoy.getFullYear() ? "Año vivido" : a === hoy.getFullYear() ? "Año en curso" : "Por vivir") + cuerpo(
      `<div class="migas"><button data-sel="vida">Mi vida</button> › <span>${a}</span></div>` +
      hoja(`<div class="cab-lista-btn"><div class="etiqueta">Metas del año</div><button class="btn mini oro" data-acc="nuevaMetaAnio" data-arg="${a}">${ic("mas")} Nueva meta</button></div>
        ${metas.length ? metas.map(m => `<div class="meta-fila"><div class="arriba"><button style="text-align:left" data-ir="metas" data-arg="a:${m.id}">${ic("bandera", "i fijado")} ${esc(m.titulo)}</button><span class="pct">${pctTxt(c.nodoAnio(m))}</span></div><div class="barra"><i style="width:${c.nodoAnio(m) * 100}%"></i></div></div>`).join("")
          : `<p class="suave" style="margin:8px 0 0">Todavía no hay metas para ${a}.</p>`}`) +
      `<div class="meses">${meses}</div>`);
  },
  "ruta.m"(arg) { return detMes(arg); },
  "ruta.d"(arg) { return detDia(arg); },
  "hoy.d"(arg) { return detDia(arg); },
  "hoy.mm"(arg) { return detMetaMes(Number(arg)); },

  /* ---------- Metas: propósitos, metas del año y del mes ---------- */
  "metas.nuevo"() {
    const anioHoy = new Date().getFullYear();
    return cab(av(ic("mas"), "var(--burdeos)"), "Nueva meta", "Elige el nivel de la cascada") + cuerpo(
      hoja(`<h3>Propósito a largo plazo</h3><p class="suave">Tu cumbre a 5, 10, 15 o 20 años. De aquí salen las metas de cada año.</p><button class="btn lleno" data-acc="nuevoProposito">Crear propósito</button>`) +
      hoja(`<h3>Meta del año</h3><p class="suave">Tu campamento base: lo que lograrás este año para acercarte al propósito.</p><button class="btn lleno" data-acc="nuevaMetaAnio" data-arg="${anioHoy}">Crear meta de ${anioHoy}</button>`) +
      hoja(`<h3>Meta del mes</h3><p class="suave">Lo que harás este mes. Cada día que la cumplas, el avance sube solo hasta tu propósito.</p><button class="btn lleno" data-acc="nuevaMetaMes" data-arg="${iso(hoyFecha()).slice(0, 7)}">Crear meta de ${MESES[hoyFecha().getMonth()]}</button>`), "max-width:640px");
  },
  "metas.p"(arg) {
    const p = Store.get("proposito", arg); if (!p) return vacio();
    const c = cascada(), k = String(p.id);
    const acciones = Store.lista("accion").filter(x => x.propositoId === p.id).sort((x, y) => (x.orden || 0) - (y.orden || 0) || x.id - y.id);
    const anuales = c.anuales.filter(x => x.propositoId === p.id);
    const auto = anuales.length > 0;
    return cab(av(pctTxt(c.nodoProp(p)), COLOR_EJE[p.eje] || "var(--burdeos)"), esc(p.titulo), `Propósito a ${p.horizonte || 5} años`,
      `<button class="btn mini peligro" data-acc="borrar" data-arg="proposito|${k}">${ic("borrar")} Borrar</button>`) + cuerpo(
      hoja(`<div class="form">${campo("Propósito", txt("proposito", k, "titulo", p.titulo))}
        <div class="form-2">${campo("Horizonte", sel("proposito", k, "horizonte", p.horizonte || 5, HORIZONTES.map(h => [h, `${h} años`]), "num"))}
          ${campo("Eje principal", sel("proposito", k, "eje", p.eje || "", opcionesEje, "nulo"))}
          ${campo("Prioridad", sel("proposito", k, "prioridad", p.prioridad || "B", PRIORIDADES.map(x => [x, PRIORIDAD_NOMBRE[x]])))}</div>
        ${auto ? `<p class="suave" style="margin:0">Avance automático: ${pctTxt(c.nodoProp(p))}, el promedio de sus metas anuales.</p>`
          : campo(`Avance manual`, `<div class="rango"><input type="range" min="0" max="100" step="5" value="${p.progreso || 0}" ${bind("proposito", k, "progreso", "num")}><b>${p.progreso || 0}%</b></div>`)}
        ${campo("Descripción", area("proposito", k, "descripcion", p.descripcion))}
        ${campo("Indicador de éxito", area("proposito", k, "indicadorExito", p.indicadorExito, "¿Cómo sabrás que llegaste?"))}
        ${campo("¿Por qué me importa?", area("proposito", k, "porQueImporta", p.porQueImporta))}
        ${campo("Metas específicas (una por línea)", area("proposito", k, "metasEspecificas", p.metasEspecificas))}
        ${campo("Visualización", area("proposito", k, "visualizacion", p.visualizacion))}</div>`) +
      hoja(`<div class="cab-lista-btn"><div class="etiqueta">Plan de acción</div></div>
        ${acciones.map(x => `<label class="check"><input type="checkbox" ${bind("accion", x.id, "hecha", "bool")} ${x.hecha ? "checked" : ""}><span>${esc(x.texto)}</span>
          <button class="btn mini" data-acc="borrar" data-arg="accion|${x.id}" aria-label="Borrar paso" style="margin-left:auto">${ic("borrar")}</button></label>`).join("") || `<p class="suave">Sin pasos todavía.</p>`}
        <form class="escribir" data-form="nuevaAccion" data-arg="${p.id}" style="background:none;border:0;padding:8px 0 0"><input name="texto" placeholder="Nuevo paso del plan" aria-label="Nuevo paso" autocomplete="off"><button class="enviar" aria-label="Agregar">${ic("mas")}</button></form>`) +
      hoja(`<div class="cab-lista-btn"><div class="etiqueta">Metas anuales de este propósito</div><button class="btn mini oro" data-acc="nuevaMetaAnio" data-arg="${new Date().getFullYear()}|${p.id}">${ic("mas")} Nueva</button></div>
        ${anuales.map(m => `<div class="meta-fila"><div class="arriba"><button style="text-align:left" data-sel="a:${m.id}">${m.anio} · ${esc(m.titulo)}</button><span class="pct">${pctTxt(c.nodoAnio(m))}</span></div></div>`).join("") || `<p class="suave">Conecta metas del año a este propósito para que el avance suba solo.</p>`}`));
  },
  "metas.a"(arg) {
    const m = Store.get("meta_anio", arg); if (!m) return vacio();
    const c = cascada(), k = String(m.id);
    const hijos = c.mensuales.filter(x => x.metaAnualId === m.id).sort((x, y) => x.anio - y.anio || x.mes - y.mes);
    return cab(av(pctTxt(c.nodoAnio(m)), COLOR_EJE[m.eje] || "var(--oro)"), esc(m.titulo), `Meta de ${m.anio}`,
      `<button class="btn mini peligro" data-acc="borrar" data-arg="meta_anio|${k}">${ic("borrar")} Borrar</button>`) + cuerpo(
      hoja(`<div class="form">${campo("Meta", txt("meta_anio", k, "titulo", m.titulo))}
        <div class="form-2">${campo("Año", `<input type="number" min="1900" max="2200" ${bind("meta_anio", k, "anio", "num")} value="${m.anio}">`)}
          ${campo("Propósito", sel("meta_anio", k, "propositoId", m.propositoId ?? "", [["", "Sin propósito"], ...c.props.map(p => [p.id, p.titulo])], "numNulo"))}
          ${campo("Eje", sel("meta_anio", k, "eje", m.eje || "", opcionesEje, "nulo"))}
          ${campo("Prioridad", sel("meta_anio", k, "prioridad", m.prioridad || "B", PRIORIDADES.map(x => [x, PRIORIDAD_NOMBRE[x]])))}
          ${campo("Estado", sel("meta_anio", k, "estado", m.estado || "NO_INICIADA", ESTADOS))}
          ${campo("Decisión", sel("meta_anio", k, "decision", m.decision || "CONTINUAR", DECISIONES))}</div>
        ${hijos.length ? `<p class="suave" style="margin:0">Avance automático: ${pctTxt(c.nodoAnio(m))}, el promedio de sus metas del mes.</p>`
          : campo("Avance manual", `<div class="rango"><input type="range" min="0" max="100" step="5" value="${m.avance || 0}" ${bind("meta_anio", k, "avance", "num")}><b>${m.avance || 0}%</b></div>`)}
        ${campo("Sub-metas (una por línea)", area("meta_anio", k, "subMetas", m.subMetas))}
        ${campo("Indicador", txt("meta_anio", k, "indicador", m.indicador))}
        ${campo("Evidencia observable", area("meta_anio", k, "observable", m.observable, "¿Cómo sabrás que la lograste?"))}
        ${campo("Obstáculo", area("meta_anio", k, "obstaculo", m.obstaculo))}
        ${campo("Próxima acción", txt("meta_anio", k, "proximaAccion", m.proximaAccion))}</div>`) +
      hoja(`<div class="cab-lista-btn"><div class="etiqueta">Metas del mes que la alimentan</div><button class="btn mini oro" data-acc="nuevaMetaMes" data-arg="${iso(hoyFecha()).slice(0, 7)}|${m.id}">${ic("mas")} Nueva</button></div>
        ${hijos.map(x => `<div class="meta-fila"><div class="arriba"><button style="text-align:left" data-sel="m:${x.id}">${mayus(MESES[x.mes - 1])} ${x.anio} · ${esc(x.texto)}</button><span class="pct">${pctTxt(avanceMetaMes(x))}</span></div></div>`).join("") || `<p class="suave">Sin metas del mes conectadas.</p>`}`));
  },
  "metas.m"(arg) { return detMetaMes(Number(arg)); },
};

function detMetaMes(id) {
  const m = Store.get("meta_mes", id); if (!m) return vacio();
  const k = String(m.id), c = cascada();
  return cab(av(pctTxt(avanceMetaMes(m)), COLOR_EJE[m.eje] || "var(--burdeos)"), esc(m.texto), `Meta de ${MESES[m.mes - 1]} ${m.anio}`,
    `<button class="btn mini peligro" data-acc="borrar" data-arg="meta_mes|${k}">${ic("borrar")} Borrar</button>`) + cuerpo(
    hoja(`<div class="etiqueta">Días cumplidos · ${numeros(m.dias).length} de ${m.objetivoDias || 20}</div>${tiraDias(m)}
      <label class="check" style="margin-top:10px"><input type="checkbox" ${bind("meta_mes", k, "cumplida", "bool")} ${m.cumplida ? "checked" : ""}><span>Meta cumplida</span></label>`) +
    hoja(`<div class="form">${campo("Meta", txt("meta_mes", k, "texto", m.texto))}
      <div class="form-2">${campo("Meta del año", sel("meta_mes", k, "metaAnualId", m.metaAnualId ?? "", [["", "Sin conectar"], ...c.anuales.map(a => [a.id, `${a.anio} · ${a.titulo}`])], "numNulo"))}
        ${campo("Eje", sel("meta_mes", k, "eje", m.eje || "", opcionesEje, "nulo"))}
        ${campo("Días objetivo", `<input type="number" min="1" max="31" ${bind("meta_mes", k, "objetivoDias", "num")} value="${m.objetivoDias || 20}">`)}</div>
      ${campo("Indicador", txt("meta_mes", k, "indicador", m.indicador))}</div>`), "max-width:720px");
}
function tiraDias(m) {
  const n = diasEnMes(m.anio, m.mes), hechos = new Set(numeros(m.dias)), hoy = hoyFecha();
  let h = `<div class="tira">`;
  for (let d = 1; d <= n; d++) {
    const f = new Date(m.anio, m.mes - 1, d), futuro = f > hoy;
    h += `<button class="${hechos.has(d) ? "si" : ""} ${f.getTime() === hoy.getTime() ? "hoy" : ""}" data-acc="diaMeta" data-arg="${m.id}|${d}" ${futuro ? "disabled" : ""} aria-pressed="${hechos.has(d)}" aria-label="${d} de ${MESES[m.mes - 1]}">${d}</button>`;
  }
  return h + `</div>`;
}
function detMes(arg) {
  const [a, m] = arg.split("-").map(Number), clave = arg;
  const ms = metasDelMes(a, m), b = Store.get("mes", clave) || {}, hoy = hoyFecha();
  const ini = (new Date(a, m - 1, 1).getDay() + 6) % 7;
  let cal = ["L", "M", "X", "J", "V", "S", "D"].map(x => `<i class="suave" style="font-size:11px;text-align:center;font-style:normal">${x}</i>`).join("");
  for (let k = 0; k < ini; k++) cal += `<i class="dia vacio-d"></i>`;
  for (let d = 1; d <= diasEnMes(a, m); d++) {
    const f = new Date(a, m - 1, d), pasado = f <= hoy, ok = pasado && diaCumplido(f), esHoy = f.getTime() === hoy.getTime();
    cal += `<button class="dia btn-dia ${ok ? "hecho" : ""} ${pasado && !esHoy ? "pasado" : ""} ${esHoy ? "hoy" : ""}" data-sel="d:${iso(f)}" aria-label="${d} de ${MESES[m - 1]}" style="font-size:11px;color:var(--tinta-suave)">${d}</button>`;
  }
  const campoMes = (n, etq) => campo(etq, `<textarea ${bind("mes", clave, n, "", `mes`)}>${esc(b[n] || "")}</textarea>`);
  return cab(av(MES_C[m - 1], "var(--burdeos)"), `${mayus(MESES[m - 1])} ${a}`, `${ms.length} metas este mes`) + cuerpo(
    `<div class="migas"><button data-sel="vida">Mi vida</button> › <button data-sel="a:${a}">${a}</button> › <span>${mayus(MESES[m - 1])}</span></div>` +
    hoja(`<div class="cab-lista-btn"><div class="etiqueta">Metas del mes</div><button class="btn mini oro" data-acc="nuevaMetaMes" data-arg="${clave}">${ic("mas")} Nueva meta</button></div>
      ${ms.map(x => `<div class="meta-fila"><div class="arriba"><button style="text-align:left" data-ir="metas" data-arg="m:${x.id}">${esc(x.texto)}</button><span class="pct">${pctTxt(avanceMetaMes(x))}</span></div>${tiraDias(x)}</div>`).join("") || `<p class="suave" style="margin:8px 0 0">Sin metas este mes.</p>`}`) +
    hoja(`<div class="etiqueta" style="margin-bottom:8px">Días del mes · toca uno para abrirlo</div><div class="dias" style="max-width:320px">${cal}</div>`) +
    hoja(`<h3>Balance del mes</h3><div class="form">${campoMes("comoEstuvo", "¿Cómo estuvo el mes?")}${campoMes("logros", "Logros")}${campoMes("desafios", "Desafíos")}
      <div class="form-2">${campoMes("agradecido", "Estoy agradecido por")}${campoMes("mejorar", "Puedo mejorar")}</div>${campoMes("objetivosProximo", "Objetivos del próximo mes")}${campoMes("notas", "Notas")}</div>`));
}
function detDia(fecha) {
  const f = deIso(fecha), hoy = hoyFecha(), esHoy = f.getTime() === hoy.getTime();
  const ag = Store.get("agenda", fecha) || {}, marc = marcadosDe(fecha), ms = metasDelMes(f.getFullYear(), f.getMonth() + 1);
  const c = cascada(), p = perfil();
  const campoAg = (n, etq, ph = "") => campo(etq, `<textarea ${bind("agenda", fecha, n, "", "agenda")} placeholder="${esc(ph)}">${esc(ag[n] || "")}</textarea>`);
  let anillos = "";
  if (esHoy) {
    const anioHoy = hoy.getFullYear();
    const props = c.props, anuales = c.anuales.filter(x => x.anio === anioHoy), mesH = metasDelMes(anioHoy, hoy.getMonth() + 1);
    const prom = xs => xs.length ? xs.reduce((s, x) => s + x, 0) / xs.length : 0;
    const anillo = (n, x, id = "") => `<div class="anillo"><div class="r" ${id ? `id="${id}"` : ""} style="--p:${Math.round(x * 100)}">${Math.round(x * 100)}%</div>${n}</div>`;
    anillos = hoja(`<div class="anillos">${anillo("Propósitos", prom(props.map(c.nodoProp)))}${anillo(`Año ${anioHoy}`, prom(anuales.map(c.nodoAnio)))}${anillo("Mes", prom(mesH.map(avanceMetaMes)))}${anillo("Hoy", marc.size / 18, "anilloHoy")}</div>`);
  }
  const habitos = EJES.map(([cod, nom]) => `<div class="hoja"><div class="etiqueta">${nom}</div>${HABITOS.filter(h => h[1] === cod).map(([id, , t]) =>
    `<label class="check"><input type="checkbox" data-acc-change="habito" data-arg="${fecha}|${id}" ${marc.has(id) ? "checked" : ""}><span>${esc(t)}</span></label>`).join("")}</div>`).join("");
  const sellada = esHoy && !abiertas(hoy.getFullYear()).has(fraseIndice(hoy));
  return cab(av(f.getDate(), "var(--burdeos)"), esHoy ? "Tu día" : mayus(f.toLocaleDateString("es", { weekday: "long" })), fechaLarga(f)) + cuerpo(
    `<div class="migas"><button data-ir="ruta" data-arg="vida">Mi vida</button> › <button data-ir="ruta" data-arg="a:${f.getFullYear()}">${f.getFullYear()}</button> › <button data-ir="ruta" data-arg="m:${fecha.slice(0, 7)}">${mayus(MESES[f.getMonth()])}</button> › <span>${f.getDate()}</span></div>` +
    (esHoy && p.cumbreFrase ? hoja(`<div class="etiqueta">Tu cumbre en una frase</div><p class="frase-vida" style="margin-top:6px">${esc(p.cumbreFrase)}</p>`) : "") +
    anillos +
    (sellada ? `<div class="aviso">${ic("frases")} Tu frase del día te espera sellada. <button class="btn" data-ir="frases" data-arg="f:${fecha}" style="margin-left:auto">Abrir</button></div>` : "") +
    hoja(`<div class="form"><div class="form-2">${campo("Intención del día", `<input type="text" ${bind("agenda", fecha, "intencion", "", "agenda")} value="${esc(ag.intencion || "")}">`)}
      ${campo("Prioridad #1", `<input type="text" ${bind("agenda", fecha, "prioridad", "", "agenda")} value="${esc(ag.prioridad || "")}">`)}</div></div>`) +
    hoja(`<div class="etiqueta">Metas del mes · marca si hoy diste el paso</div>
      ${ms.map(x => { const si = numeros(x.dias).includes(f.getDate()); return `<label class="check"><input type="checkbox" data-acc-change="diaMeta" data-arg="${x.id}|${f.getDate()}" ${si ? "checked" : ""} ${f > hoy ? "disabled" : ""}><span>${esc(x.texto)} <span class="suave">· ${pctTxt(avanceMetaMes(x))}</span></span></label>`; }).join("") || `<p class="suave" style="margin:8px 0 0">Sin metas este mes. <button class="btn mini oro" data-acc="nuevaMetaMes" data-arg="${fecha.slice(0, 7)}">Crear una</button></p>`}`) +
    `<div class="etiqueta">Checklist de hábitos · ${marc.size} de 18</div><div class="habitos">${habitos}</div>` +
    hoja(`<h3>Cierre del día</h3><div class="form">${campoAg("victorias", "Victorias de hoy")}${campoAg("aprendizaje", "Aprendizaje")}${campoAg("gratitud", "Gratitud")}
      <div class="form-2">${campo("Energía", `<div class="energia">${[1, 2, 3, 4, 5].map(n => `<button class="${(ag.energia || 0) >= n ? "si" : ""}" data-acc="energia" data-arg="${fecha}|${n}" aria-label="Energía ${n}"></button>`).join("")}</div>`)}
        ${campo("Vasos de agua", `<div class="rango"><input type="range" min="0" max="8" value="${ag.agua || 0}" ${bind("agenda", fecha, "agua", "num", "agenda")}><b>${ag.agua || 0}</b></div>`)}</div>
      ${campoAg("pendientes", "Pendientes")}${campoAg("notas", "Notas")}</div>`));
}

/* ---------- Comunidad ---------- */
const DEMO_POSTS = [
  ["demo-1", "Valentina R.", "LOGRO", "MAE", "¡Terminé mi primer curso de diseño! 30 días seguidos practicando 1 hora diaria.", "Dominar el diseño UX en 6 meses", 128, 3],
  ["demo-2", "Andrés M.", "VISION", "TRA", "Mi vision board 2027: una casa con jardín, un taller propio y tiempo para mi familia.", "", 96, 7],
  ["demo-3", "Lucía P.", "EVIDENCIA", "VOL", "Día 21 levantándome a las 5:00. La disciplina empieza a sentirse natural.", "Crear el hábito de madrugar", 74, 12],
  ["demo-4", "Mateo G.", "META", "VAL", "Mi meta del año: facturar el doble con mi emprendimiento sin trabajar más horas.", "Duplicar ingresos del emprendimiento", 51, 20],
  ["demo-5", "Sofía L.", "REFLEXION", "VOZ", "Hoy dije lo que pensaba en una reunión importante. Me temblaba la voz, pero lo hice.", "", 63, 26],
  ["demo-6", "Camilo T.", "LOGRO", "EVO", "Corrí mi primera media maratón. Hace un año no podía correr 2 km.", "Correr una media maratón", 142, 40],
].map(([id, n, tipo, eje, t, meta, imp, h]) => ({ id, autorId: id, autorNombre: n, autorUsuario: "", tipo, eje, texto: t, metaTitulo: meta, impulsos: imp, comentarios: 2, yoImpulse: false, creadoEn: Date.now() - h * 3600000, foto: "", propio: false, demo: true }));
const demoComentarios = {};
const misPostsDemo = [];

async function cargarFeed() {
  if (cargarFeed.activo) return; cargarFeed.activo = true;
  await null; // siempre después de dibujar la lista
  try {
    if (!Store.nube) {
      const todos = [...misPostsDemo, ...DEMO_POSTS].sort((a, b) => b.creadoEn - a.creadoEn);
      estado.feed = estado.feedFiltro === "VISION" ? todos.filter(p => p.tipo === "VISION") : estado.feedFiltro === "SIGUIENDO" ? misPostsDemo : todos;
    } else {
      const sb = Store.nube.sb, yo = Store.nube.dueno;
      let q = sb.from("posts").select("*,autor:profiles(username,nombre,avatar_url),impulsos:votes(count),comentarios:comments(count)").order("created_at", { ascending: false }).limit(40);
      if (estado.feedFiltro === "VISION") q = q.eq("tipo", "VISION");
      if (estado.feedFiltro === "SIGUIENDO") {
        const { data: f } = await sb.from("follows").select("followed_id").eq("follower_id", yo);
        const ids = (f || []).map(x => x.followed_id);
        if (!ids.length) { estado.feed = []; return; }
        q = q.in("user_id", ids);
      }
      const { data, error } = await q; if (error) throw error;
      const ids = data.map(p => p.id);
      const { data: v } = ids.length ? await sb.from("votes").select("post_id").eq("user_id", yo).in("post_id", ids) : { data: [] };
      const mios = new Set((v || []).map(x => x.post_id));
      estado.feed = data.map(p => ({
        id: p.id, autorId: p.user_id, autorNombre: p.autor?.nombre || p.autor?.username || "Senderista", autorUsuario: p.autor?.username || "",
        tipo: p.tipo, eje: p.eje, texto: p.texto, foto: p.image_url, metaTitulo: p.meta_titulo, visibilidad: p.visibilidad,
        impulsos: p.impulsos?.[0]?.count || 0, comentarios: p.comentarios?.[0]?.count || 0, yoImpulse: mios.has(p.id),
        creadoEn: Date.parse(p.created_at), propio: p.user_id === yo,
      }));
    }
  } catch (e) { console.error(e); estado.feed = []; toast("No se pudo cargar la comunidad."); }
  finally { cargarFeed.activo = false; if (estado.sec === "comunidad" || estado.sec === "perfil") { pintarLista(); if (!enEdicion()) pintarDetalle(); } }
}
function postal(p, mini = false) {
  const c = COLOR_EJE[p.eje] || "#6B2A1A", tipo = TIPOS_POST.find(t => t[0] === p.tipo)?.[1] || "";
  if (p.foto && !mini) return `<img class="foto" src="${esc(p.foto)}" alt="">`;
  return `<div class="postal" style="background:linear-gradient(160deg, ${c}, color-mix(in srgb, ${c} 55%, #1d120d))"><span class="tipo">${tipo}</span>
    <div class="q">${mini ? esc(p.texto) : "“" + esc(p.texto) + "”"}</div>${!mini && p.metaTitulo ? `<div class="m">${ic("bandera", "i fijado")} ${esc(p.metaTitulo)}${p.eje ? " · " + NOMBRE_EJE[p.eje] : ""}</div>` : ""}</div>`;
}
Object.assign(DET, {
  "comunidad.post"(id) {
    const p = (estado.feed || []).find(x => String(x.id) === id) || estado.post;
    if (!p) return vacio();
    estado.post = p;
    if (!p.comentariosLista) cargarComentarios(p);
    const ini = (p.autorNombre || "?").split(" ").map(x => x[0]).join("").slice(0, 2).toUpperCase();
    return cab(av(esc(ini), COLOR_EJE[p.eje] || "var(--burdeos)"), esc(p.autorNombre), `${p.autorUsuario ? "@" + esc(p.autorUsuario) + " · " : ""}${hace(p.creadoEn)}`,
      p.propio ? `<button class="btn mini peligro" data-acc="borrarPost" data-arg="${esc(p.id)}">${ic("borrar")} Borrar</button>`
        : `<span style="display:flex;gap:6px">${!p.demo || !Store.nube ? `<button class="btn mini" data-acc="escribirA" data-arg="${esc(p.autorId)}|${esc(p.autorNombre)}">${ic("mensajes")} Mensaje</button>` : ""}${!p.demo && Store.nube ? `<button class="btn mini" data-acc="seguir" data-arg="${esc(p.autorId)}">Seguir</button>` : ""}</span>`) +
      `<div class="det-cuerpo"><div class="ancho" style="gap:16px;max-width:640px">
        ${p.demo ? `<div class="aviso">Comunidad de ejemplo. ${Store.nube ? "" : "Vincula la web con tu cuenta para ver la comunidad real."}</div>` : ""}
        ${postal(p)}${p.foto ? `<p style="margin:0">${esc(p.texto)}</p>` : ""}
        <div class="acciones"><button class="btn ${p.yoImpulse ? "lleno" : "oro"}" data-acc="impulsar" aria-pressed="${p.yoImpulse}">${ic("impulso")} ${p.yoImpulse ? "Impulsado" : "Impulsar"} · ${p.impulsos}</button>
          <span class="suave">${(p.comentariosLista || []).length || p.comentarios} comentarios</span></div>
        <div class="chat" style="max-width:none">${(p.comentariosLista || []).map(c => `<div class="burbuja el"><b style="color:var(--oro);font-size:13px">${esc(c.autor)}</b><br>${esc(c.texto)}</div>`).join("")}</div>
      </div></div>
      <form class="escribir" data-form="comentar"><input name="texto" placeholder="Escribe un comentario" aria-label="Comentario" autocomplete="off"><button class="enviar" aria-label="Enviar">${ic("enviar")}</button></form>`;
  },
  "comunidad.feed"() {
    if (!estado.feed) { cargarFeed(); return cab(av(ic("comunidad"), "var(--burdeos)"), "Comunidad", "Cargando…") + cuerpo(""); }
    const ps = estado.feed;
    const autor = p => { const ini = (p.autorNombre || "?").split(" ").map(x => x[0]).join("").slice(0, 2).toUpperCase(); return av(esc(ini), COLOR_EJE[p.eje] || "var(--burdeos)"); };
    const vacioFeed = `<p class="vacio-mini">${estado.feedFiltro === "SIGUIENDO" ? "Todavía no sigues a nadie." : "Aún no hay publicaciones."}</p>`;
    if (estado.vistaCom === "cimas") {
      return cab(av(ic("ruta"), "var(--burdeos)"), "Cimas", "Desliza o usa las flechas para ver la siguiente cima") +
        `<div class="cimas" tabindex="0" aria-label="Cimas">${ps.length ? ps.map(p => `<article class="cima" data-cima="${esc(p.id)}">
          <div class="cima-fondo" style="--c:${COLOR_EJE[p.eje] || "#6B2A1A"}">${p.foto ? `<img src="${esc(p.foto)}" alt="">` : `<p class="cima-cita">“${esc(p.texto)}”</p>`}</div>
          <span class="cima-tipo">${TIPOS_POST.find(t => t[0] === p.tipo)?.[1] || ""}</span>
          <div class="cima-acciones">${autor(p)}
            <button data-acc="impulsarId" data-arg="${esc(p.id)}" aria-pressed="${p.yoImpulse}" aria-label="Impulsar">${ic("impulso")}<b>${p.impulsos}</b></button>
            <button data-ir="comunidad" data-arg="post:${esc(p.id)}" aria-label="Comentarios">${ic("comentario")}<b>${p.comentarios}</b></button></div>
          <div class="cima-pie"><span><b>${esc(p.autorNombre)}</b> · ${hace(p.creadoEn)}</span>${p.foto && p.texto ? `<p>${esc(p.texto)}</p>` : ""}
            ${p.metaTitulo ? `<span class="cima-meta">⛰ ${esc(p.metaTitulo)}${p.eje ? " · " + NOMBRE_EJE[p.eje] : ""}</span>` : ""}</div></article>`).join("") : vacioFeed}</div>`;
    }
    return cab(av(ic("comunidad"), "var(--burdeos)"), "Comunidad", "Tu muro, hacia abajo") + cuerpo(
      ps.length ? ps.map(p => `<article class="hoja post-muro">
        <div class="autor">${autor(p)}<div style="flex:1;min-width:0"><b>${esc(p.autorNombre)}</b><div class="suave" style="font-size:13px">${hace(p.creadoEn)} · ${TIPOS_POST.find(t => t[0] === p.tipo)?.[1] || ""}</div></div></div>
        <button data-ir="comunidad" data-arg="post:${esc(p.id)}" style="display:block;width:100%;text-align:left">${postal(p)}</button>
        ${p.foto && p.texto ? `<p style="margin:0">${esc(p.texto)}</p>` : ""}
        <div class="acciones"><button class="btn ${p.yoImpulse ? "lleno" : "oro"}" data-acc="impulsarId" data-arg="${esc(p.id)}" aria-pressed="${p.yoImpulse}">${ic("impulso")} ${p.impulsos}</button>
          <button class="btn" data-ir="comunidad" data-arg="post:${esc(p.id)}">${ic("comentario")} ${p.comentarios} comentarios</button></div></article>`).join("") : vacioFeed,
      "max-width:600px");
  },
  "comunidad.publicar"() {
    return cab(av(ic("publicar"), "var(--burdeos)"), "Publicar", "Planta tu bandera: comparte un paso de tu ascenso") + cuerpo(
      hoja(`<form class="form" data-form="publicar">
        ${campo("¿Qué quieres compartir?", `<textarea name="texto" required maxlength="2200" placeholder="Hoy logré…"></textarea>`)}
        <div class="form-2">${campo("Tipo", `<select name="tipo">${TIPOS_POST.map(([v, n]) => `<option value="${v}">${n}</option>`).join("")}</select>`)}
          ${campo("Eje", `<select name="eje">${opcionesEje.map(([v, n]) => `<option value="${v}">${n}</option>`).join("")}</select>`)}
          ${campo("Quién lo ve", `<select name="visibilidad">${VISIBILIDAD.map(([v, n]) => `<option value="${v}">${n}</option>`).join("")}</select>`)}</div>
        ${campo("Meta relacionada", `<select name="meta"><option value="">Ninguna</option>${Store.lista("meta_anio").map(m => `<option>${esc(m.titulo)}</option>`).join("")}</select>`)}
        ${Store.nube ? campo("Foto (opcional)", `<input type="file" name="foto" accept="image/*">`) : ""}
        <div class="botones"><button class="btn lleno" type="submit">Publicar</button></div></form>`), "max-width:640px");
  },
});
async function cargarComentarios(p) {
  await null;
  if (p.demo || !Store.nube) { p.comentariosLista = [{ autor: "Diana", texto: "¡Qué inspiración! Gracias por compartirlo." }, { autor: "Julián", texto: "Vamos, sigue así. Nos vemos en la cima." }, ...(demoComentarios[p.id] || [])]; }
  else {
    const { data } = await Store.nube.sb.from("comments").select("id,texto,created_at,autor:profiles(username,nombre)").eq("post_id", p.id).order("created_at");
    p.comentariosLista = (data || []).map(c => ({ autor: c.autor?.nombre || c.autor?.username || "", texto: c.texto }));
  }
  if (estado.sel === "post:" + p.id && !enEdicion()) pintarDetalle();
}

/* ---------- Aprende: las 24 guías, con las respuestas de tu teléfono ---------- */
const Contenido = {
  indice: null, frases: null, guias: {},
  async json(ruta) { const r = await fetch(CONFIG.contenido + ruta); if (!r.ok) throw new Error(ruta); return r.json(); },
  async cargarIndice() {
    if (this._i) return; this._i = true;
    try { this.indice = await this.json("content/index.json"); } catch { this.indice = window.RUTACIMA_INDICE || []; }
    if (estado.sec === "aprende") { pintarLista(); pintarDetalle(); }
  },
  async cargarFrases() {
    if (this._f) return; this._f = true;
    try { this.frases = await this.json("frases/es.json"); } catch { this.frases = window.RUTACIMA_FRASES || Array.from({ length: 365 }, () => ({ t: "Tu cumbre te espera.", libro: "Ruta a la Cima" })); }
    refrescar();
  },
  async guia(id) {
    if (this.guias[id] || this["_g" + id]) return this.guias[id];
    this["_g" + id] = true;
    try { this.guias[id] = await this.json(`content/${id}.json`); } catch { this.guias[id] = { id, title: id, sections: [] }; }
    if (estado.sel === "wb:" + id) pintarDetalle();
  },
};
const respuesta = clave => Store.get("respuesta", clave)?.valor || "";
function responder(wb, clave, valor) {
  if (!valor) return Store.borrar("respuesta", clave);
  return Store.guardar("respuesta", { clave, workbookId: wb, valor: String(valor), actualizadoEn: Date.now() }, clave);
}
function bloque(b, wb) {
  const md = t => esc(t).replace(/\*\*(.+?)\*\*/g, "<b>$1</b>");
  switch (b.type) {
    case "heading": { const n = Math.min(4, Math.max(2, b.level || 2) + 1); return `<h${n}>${md(b.text)}</h${n}>`; }
    case "paragraph": return `<p>${md(b.text)}</p>`;
    case "bullet": return `<p style="padding-left:18px;text-indent:-14px">${esc(b.mark || "•")} ${md(b.text)}</p>`;
    case "quote": return `<blockquote>${md(b.text)}</blockquote>`;
    case "callout": return `<div class="callout">${(b.blocks || []).map(x => bloque(x, wb)).join("")}</div>`;
    case "image": return "";
    case "table": return `<div class="tabla"><table>${b.headers?.length ? `<tr>${b.headers.map(x => `<th>${md(x)}</th>`).join("")}</tr>` : ""}${(b.rows || []).map(r => `<tr>${r.map(x => `<td>${md(x)}</td>`).join("")}</tr>`).join("")}</table></div>`;
    case "prompt": return `<div class="bloque-in">${b.title ? `<b>${b.number ? esc(b.number) + " " : ""}${md(b.title)}</b>` : ""}${b.label ? `<span class="suave">${md(b.label)}</span>` : ""}
      <textarea class="entrada" data-resp="${esc(wb)}|${esc(b.id)}" rows="3" placeholder="Escribe aquí">${esc(respuesta(b.id))}</textarea></div>`;
    case "scale": { const v = Number(respuesta(b.id)) || 0; let bs = ""; for (let i = b.min ?? 1; i <= (b.max ?? 10); i++) bs += `<button class="${v === i ? "si" : ""}" data-acc="escala" data-arg="${esc(wb)}|${esc(b.id)}|${i}" aria-pressed="${v === i}">${i}</button>`;
      return `<div class="bloque-in"><span>${md(b.label)}</span><div class="escala">${bs}</div></div>`; }
    case "check": return `<label class="check"><input type="checkbox" data-acc-change="checkResp" data-arg="${esc(wb)}|${esc(b.id)}" ${respuesta(b.id) ? "checked" : ""}><span>${md(b.label)}</span></label>`;
    case "choice": { const v = respuesta(b.id); return `<div class="bloque-in">${b.label ? `<span>${md(b.label)}</span>` : ""}<div class="opciones">${(b.options || []).map(o => `<button class="${v === o ? "si" : ""}" data-acc="opcion" data-arg="${esc(wb)}|${esc(b.id)}|${esc(o)}">${esc(o)}</button>`).join("")}</div></div>`; }
    case "inputTable": {
      const etiquetas = b.rowLabels || [], con = etiquetas.length > 0;
      const cols = con ? (b.headers || []).slice(1) : (b.headers || []);
      const filas = con ? etiquetas.length : (Number(respuesta(b.id + "#n")) || b.rows || 3);
      let h = `<div class="tabla"><table>${b.headers?.length ? `<tr>${b.headers.map(x => `<th>${md(x)}</th>`).join("")}</tr>` : ""}`;
      for (let r = 0; r < filas; r++) {
        h += `<tr>${con ? `<th>${md(etiquetas[r])}</th>` : ""}${cols.map((_, c) => `<td><input data-resp="${esc(wb)}|${esc(b.id)}#${r}_${c}" value="${esc(respuesta(`${b.id}#${r}_${c}`))}" aria-label="${esc(cols[c] || "")}"></td>`).join("")}</tr>`;
      }
      return h + `</table></div>${con ? "" : `<button class="btn mini" data-acc="filaMas" data-arg="${esc(wb)}|${esc(b.id)}|${filas + 1}">${ic("mas")} Fila</button>`}`;
    }
    default: return "";
  }
}
Object.assign(DET, {
  "aprende.wb"(id) {
    const g = Contenido.guias[id];
    const resumen = (Contenido.indice || []).find(x => x.id === id) || { title: id, subtitle: "" };
    Audio_.cargar();
    if (!g) { Contenido.guia(id); return cab(av(esc(resumen.title[0]), "var(--burdeos)"), esc(resumen.title), "Cargando…") + cuerpo(""); }
    const secs = g.sections || [];
    const guardada = Number(respuesta(`${id}#ultima`)) || 0;
    if (estado.wb[id] === undefined) estado.wb[id] = Math.min(guardada, Math.max(0, secs.length - 1));
    const i = estado.wb[id], s = secs[i];
    return cab(av(esc(g.title.trim()[0]), g.category === "bonos" ? "var(--oro)" : "var(--burdeos)"), esc(g.title), esc(g.subtitle || "")) + cuerpo(
      `<div class="tabs">${secs.map((x, k) => `<button data-acc="seccion" data-arg="${esc(id)}|${k}" aria-pressed="${k === i}">${esc(x.title || `Sección ${k + 1}`)}</button>`).join("")}</div>` +
      (s ? `<div id="audioBarra" data-cap="${esc(id)}|${i}">${htmlAudioBarra(id, i)}</div>` : "") +
      (s ? hoja(`<div class="wb">${s.title ? `<h2 style="margin-top:0">${esc(s.title)}</h2>` : ""}${(s.blocks || []).map(b => bloque(b, id)).join("")}</div>`) : `<p class="vacio-mini">Esta guía no se pudo cargar.</p>`) +
      `<div class="botones" style="justify-content:space-between"><button class="btn" data-acc="seccion" data-arg="${esc(id)}|${i - 1}" ${i <= 0 ? "disabled" : ""}>Anterior</button>
        <span class="suave">${i + 1} de ${secs.length}</span>
        <button class="btn lleno" data-acc="seccion" data-arg="${esc(id)}|${i + 1}" ${i >= secs.length - 1 ? "disabled" : ""}>Siguiente</button></div>`, "max-width:860px");
  },
});

/* ---------- Coach ---------- */
const mensajesCoach = () => Store.lista("coach").sort((a, b) => a.creadoEn - b.creadoEn || a.id - b.id);
let coachEscribiendo = false;
function contextoCoach() {
  const p = perfil(), e = ultimaEvaluacion(), c = cascada(), anio = new Date().getFullYear(), hoy = hoyFecha();
  const l = [];
  if (p.nombre) l.push(`Nombre: ${p.nombre}`);
  if (p.cumbreFrase) l.push(`Cumbre personal: ${p.cumbreFrase}`);
  const fase = FASES.findIndex(f => f[0] === p.faseActual); if (fase >= 0) l.push(`Fase actual: ${fase + 1}. ${FASES[fase][1]}`);
  if (e) l.push("Ejes (1-10): " + EJES.map(([, n, k]) => `${n} ${e[k]}`).join(", "));
  if (c.props.length) l.push("Propósitos a 5 años: " + c.props.map(x => `${x.titulo} (${Math.round(c.nodoProp(x) * 100)}%)`).join("; "));
  const an = c.anuales.filter(x => x.anio === anio); if (an.length) l.push(`Metas ${anio}: ` + an.map(x => `${x.titulo} (${Math.round(c.nodoAnio(x) * 100)}%)`).join("; "));
  const me = metasDelMes(anio, hoy.getMonth() + 1); if (me.length) l.push("Metas de este mes: " + me.map(x => `${x.texto} (${Math.round(avanceMetaMes(x) * 100)}%)`).join("; "));
  return l.join("\n");
}
function respuestaGuia(t) {
  t = t.toLowerCase(); const tiene = (...p) => p.some(x => t.includes(x));
  if (tiene("motiv", "ganas", "cansad", "fuego")) return "La motivación va y viene; los sistemas se quedan. Hoy no necesitas ganas, necesitas un paso de 2 minutos. Elige la acción más pequeña de tu meta del mes y hazla ahora. Luego márcala en Hoy: ver el avance enciende de nuevo el fuego.";
  if (tiene("rendir", "abandon", "dejar")) return "Antes de abandonar, abre el bono Anti-abandono: casi siempre no hay que dejar la montaña, sino cambiar la ruta. Pregúntate: ¿esta meta todavía me acerca a mi cumbre? Si la respuesta es sí, reduce el tamaño del paso, no la meta.";
  if (tiene("perdid", "confund", "no sé", "no se")) return "Perderse en la niebla es parte del viaje. Abre el Kit de Emergencia y elige el protocolo que describe cómo te sientes. Después vuelve a tu cumbre en una frase y decide solo el próximo paso.";
  const e = ultimaEvaluacion();
  if (!e) return "Para orientarte mejor, evalúa tus 6 ejes (Perfil → Mis ejes). Con eso sabré qué parte de tu montaña necesita más atención.";
  const [cod, nom, k] = EJES.reduce((m, x) => (e[x[2]] < e[m[2]] ? x : m));
  return `Tu eje más débil hoy es ${nom} (${e[k]}/10). Elige una acción pequeña para fortalecerlo esta semana y conviértela en una meta del mes en Metas → Nueva meta.`;
}
async function preguntar(texto) {
  if (estado.sec !== "coach") { estado.sec = "coach"; pintarRiel(); }
  estado.sel = "coach"; $("app").classList.add("con-detalle");
  await Store.guardar("coach", { id: nuevoId(), rol: "user", texto, creadoEn: Date.now() });
  coachEscribiendo = true; pintarLista(); pintarDetalle();
  let r = null;
  if (Store.nube) {
    try {
      const hist = mensajesCoach().slice(-16).map(m => ({ role: m.rol, content: m.texto }));
      const { data, error } = await Store.nube.sb.functions.invoke("coach", { body: { contexto: contextoCoach(), idioma: "es", mensajes: hist } });
      if (error) throw error;
      r = data?.texto || null;
    } catch (e) { console.warn("coach sin servidor", e); }
  } else await new Promise(ok => setTimeout(ok, 700));
  coachEscribiendo = false;
  await Store.guardar("coach", { id: nuevoId(), rol: "assistant", texto: r || respuestaGuia(texto), creadoEn: Date.now() });
}
DET.coach = () => {
  const ms = mensajesCoach();
  const sugs = ["Ayúdame a definir mi cumbre", "Divide mi meta anual en pasos", "Perdí la motivación", "Planea mi semana", "¿Qué eje debo trabajar?"];
  let ultimoDia = "";
  return cab(av(ic("coach"), "var(--burdeos)"), "Coach Rutaalacima", coachEscribiendo ? "escribiendo…" : "Conoce tus metas, tus ejes y tu avance",
    ms.length ? `<button class="btn mini" data-acc="borrarCoach">Nueva conversación</button>` : "") +
    `<div class="det-cuerpo abajo"><div class="chat">
      <div class="burbuja el">¡Hola! Soy tu coach de Rutaalacima. Conozco tus metas, tus ejes y tu avance. Cuéntame qué quieres lograr o en qué te sientes atascado y te ayudo a dar el siguiente paso.</div>
      ${ms.map(m => { const d = new Date(m.creadoEn).toLocaleDateString("es", { day: "numeric", month: "long" }); const sep = d !== ultimoDia ? `<span class="fecha-sep">${d}</span>` : ""; ultimoDia = d;
        return sep + `<div class="burbuja ${m.rol === "user" ? "yo" : "el"}">${esc(m.texto)}<span class="h">${horaDe(m.creadoEn)}</span></div>`; }).join("")}
      ${coachEscribiendo ? `<div class="burbuja el escribiendo">escribiendo…</div>` : ""}</div>
      ${ms.length < 2 ? `<div class="sugerencias">${sugs.map(s => `<button data-pregunta="${esc(s)}">${esc(s)}</button>`).join("")}</div>` : ""}</div>
    <form class="escribir" data-form="coach"><input name="texto" placeholder="Escribe un mensaje" aria-label="Mensaje" autocomplete="off"><button class="enviar" aria-label="Enviar">${ic("enviar")}</button></form>`;
};

/* ---------- Mis frases ---------- */
DET["frases.f"] = fecha => {
  if (!Contenido.frases) { Contenido.cargarFrases(); return vacio(); }
  const d = deIso(fecha), hoy = hoyFecha(), esHoy = d.getTime() === hoy.getTime();
  const abierta = abiertas(d.getFullYear()).has(fraseIndice(d)), fr = Contenido.frases[fraseIndice(d)];
  return cab(avSello(), esHoy ? "Frase de hoy" : `Día ${diaDelAnio(d) + 1} del año`, fechaLarga(d)) + cuerpo(
    `<div class="hoja sellada ${abierta ? "rota" : ""}" id="tarjetaFrase"><div class="etiqueta">Frase del día</div><img class="lacre" src="${LOGO}" alt="Sello de cera">
      <div id="contenidoFrase">${abierta ? `<p class="cita aparece">“${esc(fr.t)}”</p><p class="cita-libro aparece">${esc(fr.libro)}</p>`
        : esHoy ? `<p class="suave" style="margin:18px auto 16px;max-width:36ch">Cada día te espera una frase de los libros, sellada con cera. Este computador está vinculado a tu cuenta, así que el sello se rompe con un toque.</p><button class="btn lleno" data-acc="romper" data-arg="${fecha}">${ic("candado")} Romper el sello</button>`
        : `<p class="suave" style="margin-top:18px">Este sello no se rompió ese día. Las frases solo se abren el día que les toca.</p>`}</div></div>`, "max-width:640px");
};

/* ---------- Perfil ---------- */
const ultimaEvaluacion = () => Store.lista("ejes").sort((a, b) => b.fecha - a.fecha)[0] || null;
Object.assign(DET, {
  "perfil.perfil"() {
    const p = perfil(), r = recordatorio(), anioHoy = new Date().getFullYear();
    return cab(av(esc((p.nombre || "Y")[0].toUpperCase()), "var(--burdeos)"), esc(p.nombre || "Tu perfil"), r ? `${r.edad} años · camino hacia los ${metaVida()}` : "") + cuerpo(
      hoja(`<div class="form">${campo("Tu nombre", txt("perfil", 1, "nombre", p.nombre))}
        ${campo("Mi cumbre es… (en una frase)", area("perfil", 1, "cumbreFrase", p.cumbreFrase))}
        <div class="form-2">${campo("Año de nacimiento", `<input type="number" min="1900" max="${anioHoy}" ${bind("perfil", 1, "anioNacimiento", "numNulo")} value="${p.anioNacimiento ?? ""}">`)}
          ${campo("Mes de nacimiento", sel("perfil", 1, "mesNacimiento", p.mesNacimiento ?? "", [["", "—"], ...MESES.map((m, i) => [i + 1, mayus(m)])], "numNulo"))}
          ${campo("Fase actual", sel("perfil", 1, "faseActual", p.faseActual || "", [["", "—"], ...FASES], "nulo"))}
          ${campo("Año de inicio del plan", `<input type="number" min="2000" max="2100" ${bind("perfil", 1, "anioInicioPlan", "num")} value="${p.anioInicioPlan || anioHoy}">`)}</div>
        ${campo(`Meta de vida: ${metaVida()} años`, `<div class="rango"><input type="range" min="60" max="120" value="${metaVida()}" ${bind("perfil", 1, "esperanzaVida", "num")}><b>${metaVida()}</b></div>`)}</div>`), "max-width:720px");
  },
  "perfil.ejes"() {
    const e = ultimaEvaluacion(), hist = Store.lista("ejes").sort((a, b) => b.fecha - a.fecha);
    const val = estado.evalNueva ||= Object.fromEntries(EJES.map(([, , k]) => [k, e ? e[k] : 5]));
    return cab(av(ic("comunidad"), "var(--oro)"), "Mis ejes", e ? `Última evaluación: ${new Date(e.fecha).toLocaleDateString("es")}` : "Evalúa tus 6 ejes") + cuerpo(
      (e ? hoja(`<h3>Así están tus ejes</h3><div class="ejes">${EJES.map(([c, n, k]) => `<div class="eje"><span>${n}</span><div class="barra"><i style="width:${e[k] * 10}%;background:${COLOR_EJE[c]}"></i></div><b>${e[k]}</b></div>`).join("")}</div>`) : "") +
      hoja(`<h3>Nueva evaluación</h3><p class="suave" style="margin-top:0">Del 1 al 10, ¿cómo está hoy cada eje de tu cumbre?</p><div class="form">
        ${EJES.map(([c, n, k]) => campo(n, `<div class="rango"><input type="range" min="1" max="10" value="${val[k]}" data-acc-change="evalEje" data-arg="${k}" data-acc-input="evalEje"><b>${val[k]}</b></div>`)).join("")}
        <div class="botones"><button class="btn lleno" data-acc="guardarEval">Guardar evaluación</button></div></div>`) +
      (hist.length > 1 ? hoja(`<h3>Historial</h3>${hist.map(x => `<div class="meta-fila"><div class="arriba"><span>${new Date(x.fecha).toLocaleDateString("es")} · ${EJES.map(([, , k]) => x[k]).reduce((a, b) => a + b, 0)}/60</span><button class="btn mini" data-acc="borrar" data-arg="ejes|${x.id}">${ic("borrar")}</button></div></div>`).join("")}`) : ""), "max-width:720px");
  },
  "perfil.publicaciones"() {
    if (!estado.misPosts) cargarMisPosts();
    const ps = estado.misPosts || [];
    return cab(av(ic("foto"), "var(--burdeos)"), "Mis publicaciones", "Tu diario de vida", `<button class="btn mini oro" data-ir="comunidad" data-arg="publicar">${ic("mas")} Publicar</button>`) + cuerpo(
      ps.length ? `<div class="posts">${ps.map(p => `<button data-ir="comunidad" data-arg="post:${esc(p.id)}" style="text-align:left">${p.foto ? `<img class="foto" style="aspect-ratio:4/5" src="${esc(p.foto)}" alt="">` : postal(p, true)}</button>`).join("")}</div>`
        : `<p class="vacio-mini">${estado.misPosts ? "Todavía no has publicado." : "Cargando…"}</p>`);
  },
  "perfil.cuenta"() {
    return cab(av(ic("salir"), "#8A7B70"), Store.nube ? "Este computador" : "Demostración", Store.nube ? nombreNavegador() : "Ruta de ejemplo") + cuerpo(
      hoja(Store.nube
        ? `<h3>Conectado a tu cuenta</h3><p>Rutaalacima Web usa los mismos datos que tu teléfono${Store.nube.perfil?.username ? ` (@${esc(Store.nube.perfil.username)})` : ""}. Lo que cambies aquí aparece en la app, y lo que hagas en la app aparece aquí.</p>
           <p class="suave">Para desvincular este computador desde el teléfono: Perfil › Rutaalacima Web.</p><div class="botones"><button class="btn peligro" data-acc="salir">Cerrar sesión en este computador</button></div>`
        : `<h3>Estás en la demostración</h3><p>Es una ruta de ejemplo y los cambios se quedan en esta pestaña. Para usar tu ruta real, abre Rutaalacima Web con el servidor configurado y vincúlala desde la app: Perfil › Rutaalacima Web › Escanear código.</p>
           <div class="botones"><button class="btn" data-acc="salir">Volver a la pantalla de inicio</button></div>`), "max-width:640px");
  },
});
async function cargarMisPosts() {
  if (cargarMisPosts.activo) return; cargarMisPosts.activo = true;
  await null;
  try {
    if (!Store.nube) estado.misPosts = misPostsDemo;
    else {
      const { data } = await Store.nube.sb.from("posts").select("*").eq("user_id", Store.nube.dueno).order("created_at", { ascending: false }).limit(200);
      estado.misPosts = (data || []).map(p => ({ id: p.id, autorId: p.user_id, autorNombre: perfil().nombre || "Yo", tipo: p.tipo, eje: p.eje, texto: p.texto, foto: p.image_url, metaTitulo: p.meta_titulo, impulsos: 0, comentarios: 0, creadoEn: Date.parse(p.created_at), propio: true }));
    }
  } finally { cargarMisPosts.activo = false; if ((estado.sel === "publicaciones" || estado.sel === "vision") && !enEdicion()) pintarDetalle(); }
}


/* ---------- Vision board: la IA lo arma con tus datos, tú lo llenas con tus fotos ---------- */
const VISION_MAX = 9;
const VISION_EJES = {
  VOL: ["Sé lo que me mueve y voy por ello cada día.", "Algo que te encienda: un lugar, una persona o un símbolo de tu propósito."],
  MAE: ["Practico cada día hasta dominar mi oficio.", "Tú en tu práctica: tus herramientas, tu espacio de trabajo o de estudio."],
  VOZ: ["Comparto mi mensaje con mi voz auténtica.", "Tú hablando, enseñando o mostrando tu obra a otros."],
  VAL: ["Vivo con coherencia de lo que sé hacer.", "Lo que tu trabajo hace posible: tu casa, tu negocio, tu libertad."],
  EVO: ["Cada mes soy mejor que el anterior.", "Algo que estás aprendiendo o un lugar al que quieres llegar."],
  TRA: ["Mi cumbre ayuda a otros a subir.", "Las personas que impactas: tu familia, tus alumnos, tu comunidad."],
};
const VACIAS = new Set("el la los las un una unos unas de del al a en y o con por para mi mis tu tus su sus que se lo es son como más the and of to my sin sobre hasta desde cada todo toda".split(" "));
const palabrasClave = t => [...new Set(String(t).toLowerCase().split(/[^\p{L}\p{N}]+/u).filter(w => w.length > 2 && !VACIAS.has(w) && isNaN(Number(w))))].slice(0, 4).join(" ");
const urlIdeas = b => "https://www.pexels.com/search/" + encodeURIComponent(b || "mountain summit") + "/";
const INSTRUCCION_VISION = `Arma mi vision board con lo que sabes de mí (mi cumbre, mis propósitos, mis metas y mis ejes).
Responde SOLO con un arreglo JSON de 6 a ${VISION_MAX} casillas, sin texto antes ni después. Cada casilla:
{"titulo": "rótulo corto, máx. 4 palabras",
 "afirmacion": "frase en presente y primera persona, máx. 14 palabras, sobre algo concreto de mi vida",
 "eje": "VOL|MAE|VOZ|VAL|EVO|TRA o null",
 "sugerencia": "qué foto MÍA buscar o tomar para esta casilla (no imágenes genéricas)",
 "busqueda": "2 a 4 palabras para buscar ideas de imágenes"}
Cubre mi cumbre, cada propósito y los ejes más débiles. Escribe en mi idioma.`;
/** Igual que VisionBoard.proponer en la app. */
function proponerVision() {
  const p = perfil(), anio = new Date().getFullYear(), r = [], inicio = p.anioInicioPlan || anio;
  const sug = (eje, t) => `${VISION_EJES[eje]?.[1] || "Una foto tuya en un lugar o momento que represente tu cumbre."} Relacionada con: ${t}`;
  if ((p.cumbreFrase || "").trim()) r.push({ titulo: "Mi cumbre", afirmacion: p.cumbreFrase.trim(), eje: null, sugerencia: "Una foto tuya en un lugar o momento que represente tu cumbre.", busqueda: palabrasClave(p.cumbreFrase), origen: "cumbre" });
  Store.lista("proposito").slice(0, 3).forEach(x => r.push({ titulo: `Propósito · ${inicio + (x.horizonte || 5) - 1}`, afirmacion: x.titulo.trim(), eje: x.eje || null, sugerencia: sug(x.eje, x.titulo), busqueda: palabrasClave(x.titulo), origen: "proposito:" + x.id }));
  Store.lista("meta_anio").filter(x => x.anio === anio).slice(0, 3).forEach(x => r.push({ titulo: `Meta ${x.anio}`, afirmacion: x.titulo.trim(), eje: x.eje || null, sugerencia: sug(x.eje, x.titulo), busqueda: palabrasClave(x.titulo), origen: "meta:" + x.id }));
  const e = ultimaEvaluacion(), debil = e ? EJES.reduce((m, x) => (e[x[2]] < e[m[2]] ? x : m))[0] : null;
  const presentes = new Set(r.map(x => x.eje).filter(Boolean));
  EJES.map(x => x[0]).filter(c => !presentes.has(c)).sort((a, b) => (a === debil ? -1 : b === debil ? 1 : 0)).forEach(c => {
    if (r.length < VISION_MAX) r.push({ titulo: NOMBRE_EJE[c], afirmacion: VISION_EJES[c][0], eje: c, sugerencia: VISION_EJES[c][1], busqueda: palabrasClave(VISION_EJES[c][1]), origen: "eje:" + c });
  });
  return r.slice(0, VISION_MAX);
}
/** Igual que VisionBoard.desdeIa en la app. */
function visionDesdeIa(texto) {
  const i = texto.indexOf("["), f = texto.lastIndexOf("]");
  if (i < 0 || f <= i) return null;
  let arr; try { arr = JSON.parse(texto.slice(i, f + 1)); } catch { return null; }
  if (!Array.isArray(arr)) return null;
  const s = v => (typeof v === "string" && v.trim() && v.trim() !== "null") ? v.trim() : null;
  const r = arr.map(o => {
    if (!o || !s(o.afirmacion)) return null;
    const eje = s(o.eje)?.toUpperCase();
    return { titulo: (s(o.titulo) || s(o.afirmacion)).slice(0, 40), afirmacion: s(o.afirmacion).slice(0, 160), eje: NOMBRE_EJE[eje] ? eje : null,
      sugerencia: (s(o.sugerencia) || "").slice(0, 200), busqueda: (s(o.busqueda) || palabrasClave(o.afirmacion)).slice(0, 60), origen: "ia" };
  }).filter(Boolean).slice(0, VISION_MAX);
  return r.length >= 3 ? r : null;
}
const casillasVision = () => Store.lista("vision").sort((a, b) => (a.orden || 0) - (b.orden || 0) || a.id - b.id);
function fotoDePublicacion(id) {
  if (!id) return null;
  const p = (estado.misPosts || []).find(x => String(x.id) === String(id));
  return p?.foto || null;
}
let armandoVision = false, visionConIa = null;
async function armarVision() {
  armandoVision = true; pintarDetalle();
  let propuestas = null;
  if (Store.nube) {
    try {
      const { data } = await Store.nube.sb.functions.invoke("coach", { body: { contexto: contextoCoach(), idioma: "es", mensajes: [{ role: "user", content: INSTRUCCION_VISION }] } });
      propuestas = data?.texto ? visionDesdeIa(data.texto) : null;
    } catch (e) { console.warn(e); }
  }
  visionConIa = !!propuestas;
  propuestas ||= proponerVision();
  const actuales = casillasVision(), conFoto = actuales.filter(c => c.publicacionId);
  actuales.filter(c => !c.publicacionId).forEach(c => Store.borrar("vision", c.id));
  const cubiertas = new Set(conFoto.map(c => c.origen));
  let orden = Math.max(-1, ...conFoto.map(c => c.orden || 0)) + 1;
  propuestas.filter(p => p.origen === "ia" || !cubiertas.has(p.origen)).slice(0, Math.max(0, VISION_MAX - conFoto.length))
    .forEach(p => Store.guardar("vision", { id: nuevoId(), orden: orden++, publicacionId: null, ...p }));
  armandoVision = false; pintarDetalle();
}
async function ponerFotoVision(id, archivo) {
  const c = Store.get("vision", id); if (!c || !archivo) return;
  toast("Subiendo tu foto…");
  try {
    let postId;
    if (Store.nube) {
      const sb = Store.nube.sb; postId = crypto.randomUUID();
      const ruta = `${Store.nube.dueno}/${postId}.jpg`;
      const up = await sb.storage.from("media").upload(ruta, archivo, { upsert: true, contentType: archivo.type || "image/jpeg" });
      if (up.error) throw up.error;
      const url = sb.storage.from("media").getPublicUrl(ruta).data.publicUrl;
      const r = await sb.from("posts").insert({ id: postId, user_id: Store.nube.dueno, tipo: "VISION", texto: c.afirmacion, image_url: url, eje: c.eje || null, anio: new Date().getFullYear(), visibilidad: "PRIVADA", meta_titulo: c.titulo });
      if (r.error) throw r.error;
      (estado.misPosts ||= []).unshift({ id: postId, tipo: "VISION", eje: c.eje, texto: c.afirmacion, foto: url, metaTitulo: c.titulo, creadoEn: Date.now(), propio: true });
    } else {
      postId = "mio-" + nuevoId();
      misPostsDemo.unshift({ id: postId, autorNombre: perfil().nombre || "Yo", tipo: "VISION", eje: c.eje, texto: c.afirmacion, foto: URL.createObjectURL(archivo), metaTitulo: c.titulo, impulsos: 0, comentarios: 0, yoImpulse: false, creadoEn: Date.now(), propio: true });
      estado.misPosts = misPostsDemo;
    }
    await Store.cambiar("vision", id, { publicacionId: postId });
    toast("Foto agregada");
  } catch (e) { errorNube(e); }
}
DET["perfil.vision"] = () => {
  if (!estado.misPosts) cargarMisPosts();
  const cs = casillasVision(), conFoto = cs.filter(c => fotoDePublicacion(c.publicacionId)).length;
  const color = e => COLOR_EJE[e] || "#6B2A1A";
  const tarjeta = c => {
    const foto = fotoDePublicacion(c.publicacionId), k = String(c.id);
    return `<div class="hoja casilla-v" style="padding:0">
      <label class="casilla-foto" style="--c:${color(c.eje)}">
        <input type="file" accept="image/*" hidden data-foto-vision="${k}">
        ${foto ? `<img src="${esc(foto)}" alt=""><span class="casilla-txt">${esc(c.afirmacion)}</span>`
          : `<span class="casilla-af">${esc(c.afirmacion)}</span><span class="casilla-sug">${esc(c.sugerencia)}</span><span class="casilla-btn">${ic("foto")} Agregar mi foto</span>`}
      </label>
      <details class="casilla-pie"><summary><span>${esc(c.titulo)}</span>${foto ? "" : `<a href="${urlIdeas(c.busqueda)}" target="_blank" rel="noopener" class="btn mini">${ic("buscar")} Ideas</a>`}</summary>
        <div class="form" style="padding:12px">${campo("Frase", area("vision", k, "afirmacion", c.afirmacion))}${campo("Rótulo", txt("vision", k, "titulo", c.titulo))}
        ${campo("Qué foto buscar o tomar", area("vision", k, "sugerencia", c.sugerencia))}<button class="btn mini peligro" data-acc="borrar" data-arg="vision|${k}">${ic("borrar")} Quitar casilla</button></div></details></div>`;
  };
  return cab(av(ic("coach"), "var(--oro)"), "Mi vision board", cs.length ? `${conFoto} de ${cs.length} casillas con tu foto` : "Ármalo con IA y tus datos",
    cs.length ? `<button class="btn mini oro" data-acc="casillaNueva">${ic("mas")} Casilla</button>` : "") + cuerpo(
    hoja(`<p style="margin-top:0">La IA arma tu tablero con tu cumbre, tus propósitos y tus metas. Tú lo llenas con tus fotos: así cada imagen es de tu vida, no de un catálogo.</p>
      ${cs.length ? `<div class="barra" style="margin:10px 0"><i style="width:${conFoto / cs.length * 100}%"></i></div>` : ""}
      <div class="botones">${armandoVision ? `<span class="suave">Armando tu tablero…</span>`
        : `<button class="btn ${cs.length ? "" : "lleno"}" data-acc="armarVision">${ic("coach")} ${cs.length ? "Volver a proponer" : "Armar con IA"}</button>${cs.length ? `<span class="suave">Solo cambian las casillas que aún no tienen foto.</span>` : ""}`}</div>
      ${visionConIa === false ? `<p class="suave" style="margin-bottom:0">Lo armé con tus datos, sin conexión a la IA.</p>` : ""}`) +
    (cs.length ? `<div class="tablero">${cs.map(tarjeta).join("")}</div><p class="suave">Tus fotos se guardan como publicaciones de visión privadas: solo tú las ves hasta que decidas compartirlas.</p>` : ""));
};

/* ======================================================================
 * Acciones
 * ====================================================================== */
const BASES = {
  perfil: () => perfil(),
  agenda: c => ({ fecha: c }),
  mes: c => ({ clave: c, anio: Number(c.slice(0, 4)), mes: Number(c.slice(5, 7)) }),
  checklist: c => ({ fecha: c, marcados: "" }),
};
const alternar = (xs, x) => xs.includes(x) ? xs.filter(y => y !== x) : [...xs, x];

const ACC = {
  volver() { estado.sel = null; $("app").classList.remove("con-detalle"); pintarLista(); if (innerWidth > 900) pintarDetalle(); },
  tema() {
    const r = document.documentElement, oscuro = r.dataset.theme ? r.dataset.theme === "dark" : matchMedia("(prefers-color-scheme: dark)").matches;
    r.dataset.theme = oscuro ? "light" : "dark";
    try { localStorage.setItem("rutacima-tema", r.dataset.theme); } catch {}
  },
  async salir() {
    if (Store.nube) { mostrarCarga("Cerrando sesión…"); try { await Store.nube.desvincular(); } catch (e) { console.error(e); } location.reload(); }
    else { Store.datos = {}; iniciar(); }
  },
  borrar(el, arg) {
    if (!el.dataset.seguro) {
      el.dataset.seguro = "1"; const antes = el.innerHTML; el.innerHTML = "¿Seguro? Toca otra vez";
      setTimeout(() => { if (el.isConnected) { delete el.dataset.seguro; el.innerHTML = antes; } }, 4000);
      return;
    }
    const [tipo, clave] = arg.split("|");
    if (tipo === "proposito") Store.lista("accion").filter(a => String(a.propositoId) === clave).forEach(a => Store.borrar("accion", a.id));
    Store.borrar(tipo, clave);
    if (String(estado.sel).endsWith(":" + clave)) { estado.sel = null; $("app").classList.remove("con-detalle"); }
    toast("Borrado"); refrescar();
  },
  nuevoProposito() {
    const id = nuevoId();
    Store.guardar("proposito", { id, orden: Store.lista("proposito").length, titulo: "Nuevo propósito", metasEspecificas: "", prioridad: "B", eje: null, descripcion: "", indicadorExito: "", visualizacion: "", porQueImporta: "", impacto: "", reflexionFinal: "", progreso: 0, horizonte: 5, creadoEn: Date.now() });
    ir("metas", "p:" + id); enfocarPrimero();
  },
  nuevaMetaAnio(el, arg) {
    const [anio, prop] = arg.split("|"), id = nuevoId();
    Store.guardar("meta_anio", { id, anio: Number(anio), propositoId: prop ? Number(prop) : null, titulo: "Nueva meta", subMetas: "", decision: "CONTINUAR", prioridad: "B", avance: 0, estado: "NO_INICIADA", obstaculo: "", proximaAccion: "", eje: null, indicador: "", observable: "" });
    ir("metas", "a:" + id); enfocarPrimero();
  },
  nuevaMetaMes(el, arg) {
    const [ym, anual] = arg.split("|"), [a, m] = ym.split("-").map(Number), id = nuevoId();
    Store.guardar("meta_mes", { id, anio: a, mes: m, orden: metasDelMes(a, m).length, texto: "Nueva meta del mes", dias: "", cumplida: false, metaAnualId: anual ? Number(anual) : null, eje: null, indicador: "", objetivoDias: 20 });
    ir("metas", "m:" + id); enfocarPrimero();
  },
  diaMeta(el, arg) {
    const [id, d] = arg.split("|").map(Number), m = Store.get("meta_mes", id); if (!m) return;
    Store.cambiar("meta_mes", id, { dias: alternar(numeros(m.dias), d).sort((x, y) => x - y).join(",") });
  },
  habito(el, arg) {
    const [fecha, id] = arg.split("|");
    Store.cambiar("checklist", fecha, { marcados: alternar([...marcadosDe(fecha)], id).sort().join(",") }, BASES.checklist(fecha));
  },
  energia(el, arg) {
    const [fecha, n] = arg.split("|"); const ag = Store.get("agenda", fecha) || {};
    Store.cambiar("agenda", fecha, { energia: (ag.energia || 0) === Number(n) ? Number(n) - 1 : Number(n) }, BASES.agenda(fecha));
  },
  escala(el, arg) { const [wb, id, v] = arg.split("|"); responder(wb, id, respuesta(id) === v ? "" : v); },
  opcion(el, arg) { const [wb, id, ...o] = arg.split("|"); const v = o.join("|"); responder(wb, id, respuesta(id) === v ? "" : v); },
  checkResp(el, arg) { const [wb, id] = arg.split("|"); responder(wb, id, el.checked ? "1" : ""); },
  filaMas(el, arg) { const [wb, id, n] = arg.split("|"); responder(wb, id + "#n", n); },
  seccion(el, arg) {
    const [wb, k] = arg.split("|"); estado.wb[wb] = Number(k);
    responder(wb, `${wb}#ultima`, String(k)); pintarDetalle();
    $("detalle").querySelector(".det-cuerpo")?.scrollTo(0, 0);
  },
  async romper(el, fecha) {
    const d = deIso(fecha), anio = d.getFullYear();
    $("tarjetaFrase")?.classList.add("rota");
    await new Promise(ok => setTimeout(ok, 350));
    Store.guardar("frases", { dias: [...abiertas(anio), fraseIndice(d)].sort((x, y) => x - y) }, String(anio));
  },
  impulsarId(el, id) { const p = (estado.feed || []).find(x => String(x.id) === id); if (p) { estado.post = p; ACC.impulsar(); } },
  async impulsar() {
    const p = estado.post; if (!p) return;
    p.yoImpulse = !p.yoImpulse; p.impulsos += p.yoImpulse ? 1 : -1; refrescar();
    if (p.demo || !Store.nube) return;
    const sb = Store.nube.sb, yo = Store.nube.dueno;
    const r = p.yoImpulse ? await sb.from("votes").insert({ post_id: p.id, user_id: yo }) : await sb.from("votes").delete().eq("post_id", p.id).eq("user_id", yo);
    if (r.error) errorNube(r.error);
  },
  async seguir(el, autor) {
    const r = await Store.nube.sb.from("follows").upsert({ follower_id: Store.nube.dueno, followed_id: autor });
    if (r.error) errorNube(r.error); else { el.textContent = "Siguiendo"; el.disabled = true; }
  },
  async borrarPost(el, id) {
    if (!el.dataset.seguro) return ACC.borrar(el, ""), undefined;
    if (Store.nube) { const r = await Store.nube.sb.from("posts").delete().eq("id", id); if (r.error) return errorNube(r.error); }
    else misPostsDemo.splice(misPostsDemo.findIndex(p => p.id === id), 1);
    estado.feed = null; estado.misPosts = null; estado.sel = null; toast("Publicación borrada"); pintarTodo();
  },
  borrarCoach(el) {
    if (!el.dataset.seguro) { el.dataset.seguro = "1"; el.textContent = "¿Borrar la conversación?"; return; }
    mensajesCoach().forEach(m => Store.borrar("coach", m.id));
  },
  evalEje(el, k) { estado.evalNueva[k] = Number(el.value); el.nextElementSibling.textContent = el.value; },
  armarVision() { armarVision(); },
  casillaNueva() {
    const orden = Math.max(-1, ...casillasVision().map(c => c.orden || 0)) + 1;
    Store.guardar("vision", { id: nuevoId(), orden, titulo: "Nueva casilla", afirmacion: "Escribe aquí tu frase", eje: null, sugerencia: "", busqueda: "", origen: "manual", publicacionId: null });
  },
  guardarEval() {
    const v = estado.evalNueva;
    Store.guardar("ejes", { id: nuevoId(), fecha: Date.now(), ...v, origen: "web", nota: "" });
    estado.evalNueva = null; toast("Evaluación guardada"); pintarDetalle();
  },
};
function enfocarPrimero() { setTimeout(() => { const i = $("detalle").querySelector("input[type=text]"); if (i) { i.focus(); i.select(); } }, 30); }

/* Eventos (delegados) */
function clic(e) {
  const t = e.target.closest("button, [data-sel]"); if (!t || t.disabled) return;
  if (t.dataset.acc && !(t.matches("input"))) { e.preventDefault(); ACC[t.dataset.acc]?.(t, t.dataset.arg || ""); return; }
  if (t.dataset.ir) { ir(t.dataset.ir, t.dataset.arg || null); return; }
  if (t.dataset.pregunta) { preguntar(t.dataset.pregunta); return; }
  if (t.dataset.filtro) { estado.filtro = t.dataset.filtro; pintarLista(); return; }
  if (t.dataset.feed) { estado.feedFiltro = t.dataset.feed; estado.feed = null; pintarLista(); return; }
  if (t.dataset.vista) {
    estado.vistaCom = t.dataset.vista; try { localStorage.setItem("rutacima-vista", t.dataset.vista); } catch {}
    elegir("feed"); return;
  }
  if (t.dataset.sec) { ir(t.dataset.sec, innerWidth > 900 && (t.dataset.sec === "coach" || t.dataset.sec === "comunidad") ? (t.dataset.sec === "coach" ? "coach" : "feed") : null); return; }
  if (t.dataset.sel?.startsWith("ir-")) { const d = t.dataset.sel.slice(3); if (d === "coachvida") ir("mensajes", "coachvida"); else ir(d, d === "coach" ? "coach" : null); return; }
  if (t.dataset.sel !== undefined) elegir(t.dataset.sel);
}
function cambio(e) {
  const el = e.target;
  if (el.dataset.accChange) { ACC[el.dataset.accChange]?.(el, el.dataset.arg || ""); return; }
  if (el.dataset.fotoVision) { ponerFotoVision(el.dataset.fotoVision, el.files?.[0]); return; }
  if (el.dataset.resp) { const [wb, clave] = el.dataset.resp.split("|"); responder(wb, clave, el.value.trim() ? el.value : ""); return; }
  if (!el.dataset.bind) return;
  const [tipo, clave, nombre, conv] = el.dataset.bind.split("|");
  let v = el.type === "checkbox" ? el.checked : el.value;
  if (conv === "num") v = Number(v);
  else if (conv === "numNulo") v = v === "" ? null : Number(v);
  else if (conv === "nulo") v = v === "" ? null : v;
  else if (conv === "bool") v = !!el.checked;
  const extra = tipo === "vision" && nombre === "afirmacion" ? { busqueda: palabrasClave(v) } : {};
  Store.cambiar(tipo, clave, { [nombre]: v, ...extra }, BASES[tipo]?.(clave) || {});
}
function entrada(e) {
  const el = e.target;
  if (el.id === "busq") { estado.busqueda = el.value; pintarLista(); const b = $("busq"); b.focus(); b.setSelectionRange(b.value.length, b.value.length); return; }
  if (el.dataset.accInput) { ACC[el.dataset.accInput]?.(el, el.dataset.arg || ""); return; }
  if (el.type === "range" && el.nextElementSibling?.tagName === "B") el.nextElementSibling.textContent = el.value;
}
async function enviar(e) {
  const f = e.target.closest("form[data-form]"); if (!f) return;
  e.preventDefault();
  const datos = new FormData(f), texto = String(datos.get("texto") || "").trim();
  switch (f.dataset.form) {
    case "coach": if (texto) { f.reset(); preguntar(texto); } break;
    case "mensaje": await enviarMensaje(f, texto); break;
    case "postular": {
      const esp = String(datos.get("especialidad") || "").trim(); if (!texto || !esp) break;
      await hacer(() => Msj.postularme(texto, esp), "Postulación enviada"); MS.cv = null; cargarCoachVida();
    } break;
    case "nuevaAccion": if (texto) {
      const pid = Number(f.dataset.arg);
      await Store.guardar("accion", { id: nuevoId(), propositoId: pid, orden: Store.lista("accion").filter(a => a.propositoId === pid).length, texto, hecha: false });
    } break;
    case "comentar": {
      const p = estado.post; if (!texto || !p) break;
      f.reset();
      if (p.demo || !Store.nube) (demoComentarios[p.id] ||= []).push({ autor: perfil().nombre || "Yo", texto });
      else { const r = await Store.nube.sb.from("comments").insert({ post_id: p.id, user_id: Store.nube.dueno, texto }); if (r.error) { errorNube(r.error); break; } }
      p.comentariosLista = null; cargarComentarios(p);
    } break;
    case "publicar": {
      if (!texto) break;
      const boton = f.querySelector("button[type=submit]"); boton.disabled = true; boton.textContent = "Publicando…";
      const post = { tipo: datos.get("tipo"), eje: datos.get("eje") || null, visibilidad: datos.get("visibilidad"), meta_titulo: datos.get("meta") || "", texto, anio: new Date().getFullYear() };
      try {
        if (Store.nube) {
          const sb = Store.nube.sb, id = crypto.randomUUID(), foto = datos.get("foto");
          let url = "";
          if (foto && foto.size) {
            const ruta = `${Store.nube.dueno}/${id}.jpg`;
            const up = await sb.storage.from("media").upload(ruta, foto, { upsert: true, contentType: foto.type || "image/jpeg" });
            if (up.error) throw up.error;
            url = sb.storage.from("media").getPublicUrl(ruta).data.publicUrl;
          }
          const r = await sb.from("posts").insert({ id, user_id: Store.nube.dueno, image_url: url, ...post });
          if (r.error) throw r.error;
        } else {
          misPostsDemo.unshift({ id: "mio-" + nuevoId(), autorNombre: perfil().nombre || "Yo", tipo: post.tipo, eje: post.eje, texto, metaTitulo: post.meta_titulo, impulsos: 0, comentarios: 0, yoImpulse: false, creadoEn: Date.now(), propio: true, demo: false });
        }
        estado.feed = null; estado.misPosts = null; toast("Publicado"); ir("comunidad", null);
      } catch (err) { errorNube(err); boton.disabled = false; boton.textContent = "Publicar"; }
    } break;
  }
}

/* ======================================================================
 * Audiolibros: cada capítulo con la grabación del autor o, si no hay, con la voz del navegador
 * (igual que la app: Locucion.kt y ReproductorAudiolibro.kt)
 * ====================================================================== */
const Locucion = {
  limpiar: t => String(t || "").replace(/_{2,}/g, " ").replace(/\s*\/\s*(?=\s|$|\))/g, " ").replace(/\(\s*%?\s*:?\s*\)/g, " ")
    .replace(/\*\*(.+?)\*\*/g, "$1").replace(/\s*([,.;:])\s*(?=[,.;:)])/g, "$1").replace(/\s+/g, " ").trim().replace(/^[:\s]+|[:\s]+$/g, ""),
  /** El navegador corta los enunciados largos: fragmentos cortos, partidos por oraciones. */
  partir(t, max = 220) {
    if (t.length <= max) return [t];
    const partes = []; let actual = "";
    for (const o of t.split(/(?<=[.!?;:])\s+/)) {
      const piezas = o.length > max ? o.split(" ") : [o];
      for (const p of piezas) {
        if (actual && actual.length + p.length + 1 > max) { partes.push(actual); actual = ""; }
        actual = actual ? actual + " " + p : p;
      }
    }
    if (actual) partes.push(actual);
    return partes;
  },
  fragmentos(s) {
    const out = [];
    const agregar = t => { const l = this.limpiar(t); if (l.length > 1 && /[\p{L}\p{N}]/u.test(l)) out.push(...this.partir(l)); };
    const bloque = b => {
      switch (b.type) {
        case "heading": case "paragraph": case "bullet": case "quote": agregar(b.text); break;
        case "callout": (b.blocks || []).forEach(bloque); break;
        case "table":
          if (!(b.rows || []).length) agregar((b.headers || []).join(", "));
          (b.rows || []).forEach(f => agregar(f.map((v, i) => { const h = (b.headers?.[i] || "").trim(); return h && v.trim() && i > 0 ? `${h}: ${v}` : v; }).filter(x => x.trim()).join(". ")));
          break;
        case "prompt": agregar(b.title); agregar(b.label); break;
        case "scale": case "check": agregar(b.label); break;
        case "choice": agregar([b.label, (b.options || []).join(", ")].filter(x => x && x.trim()).join(": ")); break;
        case "inputTable": agregar((b.rowLabels || []).join(", ")); break;
      }
    };
    agregar(s.title);
    (s.blocks || []).forEach(bloque);
    return out;
  },
  minutos: fs => Math.max(1, Math.ceil(fs.reduce((n, f) => n + f.split(" ").length, 0) / 150)),
};

const Audio_ = {
  remotos: {},      // "guía/capítulo" → URL en el servidor
  incluidos: {},    // "guía/capítulo" → archivo publicado con la web (assets/audios)
  soyAutor: false, cargado: false,
  e: { activo: false, wb: "", guia: "", sec: 0, total: 0, titulo: "", fuente: "voz", sonando: false, cargando: false, avance: 0, velocidad: 1 },
  frags: [], frag: 0, gen: 0, audio: null, voz: null,

  fuente(wb, sec) { return this.remotos[`${wb}/${sec}`] || this.incluidos[`${wb}/${sec}`] || null; },
  async cargar(forzar = false) {
    if (this.cargado && !forzar) return; this.cargado = true;
    if (CONFIG.audiosIncluidos) try { const r = await fetch(CONFIG.contenido + "audios/indice.json"); if (r.ok) { const xs = await r.json(); xs.forEach(x => this.incluidos[`${x.guia}/${x.seccion}`] = CONFIG.contenido + "audios/" + x.archivo); } } catch {}
    if (Store.nube) {
      try {
        const { data } = await Store.nube.sb.from("audios").select("workbook_id,seccion,url").limit(2000);
        this.remotos = {}; (data || []).forEach(x => this.remotos[`${x.workbook_id}/${x.seccion}`] = x.url);
        const { data: autor } = await Store.nube.sb.rpc("es_autor"); this.soyAutor = autor === true;
      } catch (err) { console.error(err); }
    }
    if (estado.sec === "aprende") pintarAudio();
  },
  elegirVoz() {
    const vs = speechSynthesis.getVoices();
    for (const l of ["es-CO", "es-US", "es-419", "es-MX", "es-ES"]) { const v = vs.find(x => x.lang.replace("_", "-") === l); if (v) return v; }
    return vs.find(x => x.lang.toLowerCase().startsWith("es")) || null;
  },
  async reproducir(wb, sec) {
    const g = Contenido.guias[wb] || (await Contenido.guia(wb), Contenido.guias[wb]);
    const s = g?.sections?.[sec]; if (!s) return;
    this.parar();
    this.frags = Locucion.fragmentos(s); this.frag = 0;
    const f = this.fuente(wb, sec);
    Object.assign(this.e, { activo: true, wb, guia: g.title, sec, total: g.sections.length, titulo: s.title, fuente: f ? "autor" : "voz", sonando: true, cargando: true, avance: 0 });
    if (estado.sel === "wb:" + wb && estado.wb[wb] !== sec) { estado.wb[wb] = sec; pintarDetalle(); }
    if (f) this.sonarGrabacion(f); else this.hablar(0);
    pintarAudio();
  },
  hablar(desde) {
    if (!("speechSynthesis" in window)) { toast("Este navegador no puede leer en voz alta."); this.e.sonando = false; pintarAudio(); return; }
    if (!this.frags.length) return this.terminarCapitulo();
    this.frag = Math.max(0, Math.min(desde, this.frags.length - 1));
    const gen = ++this.gen;
    speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance(this.frags[this.frag]);
    this.voz ||= this.elegirVoz();
    if (this.voz) u.voice = this.voz; u.lang = this.voz?.lang || "es-CO"; u.rate = this.e.velocidad;
    u.onend = () => { if (gen !== this.gen || !this.e.sonando) return; this.frag + 1 >= this.frags.length ? this.terminarCapitulo() : this.hablar(this.frag + 1); };
    u.onerror = ev => {
      if (gen !== this.gen || ev.error === "interrupted" || ev.error === "canceled") return;
      console.error(ev.error); this.pausar();
      toast("Este navegador no pudo leer en voz alta. Prueba con Chrome o Edge, o revisa que tenga voces en español.");
    };
    try { speechSynthesis.speak(u); }
    catch (err) { console.error(err); this.pausar(); toast("Este navegador no pudo leer en voz alta."); return; }
    Object.assign(this.e, { cargando: false, sonando: true, avance: this.frag / this.frags.length });
    pintarAudio();
  },
  sonarGrabacion(url) {
    const a = new Audio(url); this.audio = a;
    a.playbackRate = this.e.velocidad; a.preservesPitch = true;
    a.oncanplay = () => { if (this.audio === a) { this.e.cargando = false; pintarAudio(); } };
    a.ontimeupdate = () => { if (this.audio === a && a.duration) { this.e.avance = a.currentTime / a.duration; pintarAvance(); } };
    a.onended = () => { if (this.audio === a) this.terminarCapitulo(); };
    a.onerror = () => { if (this.audio !== a) return; this.audio = null; toast("No se pudo reproducir la grabación; sigue la voz del navegador."); this.e.fuente = "voz"; this.hablar(0); };
    a.play().catch(() => { this.e.sonando = false; this.e.cargando = false; pintarAudio(); });
  },
  alternar() { this.e.sonando ? this.pausar() : this.reanudar(); },
  pausar() {
    if (!this.e.sonando) return;
    this.e.sonando = false;
    if (this.audio) this.audio.pause(); else { this.gen++; speechSynthesis.cancel(); }
    pintarAudio();
  },
  reanudar() {
    if (!this.e.activo || this.e.sonando) return;
    this.e.sonando = true;
    if (this.audio) this.audio.play().catch(console.error); else this.hablar(this.frag);
    pintarAudio();
  },
  saltar(d) {
    if (this.audio) { this.audio.currentTime = Math.max(0, Math.min(this.audio.duration || 0, this.audio.currentTime + d * 15)); return; }
    this.frag = Math.max(0, Math.min(this.frags.length - 1, this.frag + d));
    this.e.avance = this.frag / Math.max(1, this.frags.length);
    if (this.e.sonando) this.hablar(this.frag); else pintarAudio();
  },
  velocidad() {
    const vs = [0.75, 1, 1.25, 1.5, 2], i = vs.findIndex(v => v >= this.e.velocidad - 0.01);
    this.e.velocidad = vs[(i + 1) % vs.length];
    if (this.audio) this.audio.playbackRate = this.e.velocidad; else if (this.e.sonando) this.hablar(this.frag);
    pintarAudio();
  },
  terminarCapitulo() {
    if (this.e.sec + 1 < this.e.total) return this.reproducir(this.e.wb, this.e.sec + 1);
    this.parar(); Object.assign(this.e, { sonando: false, avance: 1 }); pintarAudio();
  },
  parar() {
    this.gen++;
    if ("speechSynthesis" in window) speechSynthesis.cancel();
    if (this.audio) { const a = this.audio; this.audio = null; a.pause(); a.src = ""; }
  },
  cerrar() { this.parar(); this.e = { ...this.e, activo: false, sonando: false, cargando: false, avance: 0 }; pintarAudio(); },

  /* Grabaciones del autor (solo quienes están en la tabla "autores") */
  async subir(wb, sec, archivo) {
    if (!archivo) return;
    if (archivo.size > 50 * 1024 * 1024) { toast("La grabación pesa más de 50 MB."); return; }
    const sb = Store.nube.sb, ext = (archivo.name.split(".").pop() || "mp3").toLowerCase().replace(/[^a-z0-9]/g, "") || "mp3";
    const ruta = `${wb}/${sec}-${Math.floor(Date.now() / 1000)}.${ext}`;
    toast("Subiendo tu grabación…");
    try {
      const up = await sb.storage.from("audios").upload(ruta, archivo, { upsert: true, contentType: archivo.type || "audio/mpeg" });
      if (up.error) throw up.error;
      const url = sb.storage.from("audios").getPublicUrl(ruta).data.publicUrl;
      const r = await sb.from("audios").upsert({ workbook_id: wb, seccion: sec, url, ruta });
      if (r.error) throw r.error;
      this.remotos[`${wb}/${sec}`] = url;
      toast("Listo: este capítulo ya se escucha con tu voz.");
      if (this.e.activo && this.e.wb === wb && this.e.sec === sec) this.reproducir(wb, sec); else pintarAudio();
    } catch (err) { console.error(err); toast("No se pudo subir la grabación. Revisa tu conexión."); }
  },
  async quitar(wb, sec) {
    try {
      const r = await Store.nube.sb.from("audios").delete().eq("workbook_id", wb).eq("seccion", sec);
      if (r.error) throw r.error;
      delete this.remotos[`${wb}/${sec}`]; toast("Grabación quitada: vuelve la voz del navegador."); pintarAudio();
    } catch (err) { console.error(err); toast("No se pudo quitar la grabación."); }
  },
};
if ("speechSynthesis" in window) speechSynthesis.onvoiceschanged = () => { Audio_.voz = null; };

const velTxt = v => String(v).replace(".", ",") + "×";
function htmlAudioBarra(wb, sec) {
  const e = Audio_.e, aqui = e.activo && e.wb === wb && e.sec === sec;
  const s = Contenido.guias[wb]?.sections?.[sec];
  const autor = aqui ? e.fuente === "autor" : !!Audio_.fuente(wb, sec);
  const fuente = `<span class="audio-fuente ${autor ? "autor" : ""}">${ic(autor ? "mic" : "audifonos", "i")} ${autor ? "Grabación del autor" : "Voz del navegador"}</span>`;
  const subir = Audio_.soyAutor && Store.nube ? `<label class="btn mini" title="Subir mi grabación de este capítulo">${ic("mic")} ${Audio_.remotos[`${wb}/${sec}`] ? "Reemplazar" : "Subir"} mi grabación
      <input type="file" accept="audio/*" hidden data-subir-audio="${esc(wb)}|${sec}"></label>${Audio_.remotos[`${wb}/${sec}`] ? `<button class="btn mini peligro" data-acc="quitarAudio" data-arg="${esc(wb)}|${sec}">Quitar</button>` : ""}` : "";
  if (!aqui) return `<div class="audio-barra"><button class="audio-play" data-acc="escucharCap" data-arg="${esc(wb)}|${sec}" aria-label="Escuchar este capítulo">${ic("audifonos")}</button>
    <button class="audio-txt" data-acc="escucharCap" data-arg="${esc(wb)}|${sec}"><b>Escuchar este capítulo</b> · ${s ? Locucion.minutos(Locucion.fragmentos(s)) : 1} min<br>${fuente}</button>${subir}</div>`;
  return `<div class="audio-barra sonando">
    <button class="rb" data-acc="audioSaltar" data-arg="-1" aria-label="Retroceder">${ic("atras")}</button>
    <button class="audio-play lleno" data-acc="audioAlternar" aria-label="${e.sonando ? "Pausar" : "Reanudar"}">${e.cargando ? "…" : ic(e.sonando ? "pausa" : "play")}</button>
    <button class="rb" data-acc="audioSaltar" data-arg="1" aria-label="Avanzar">${ic("adelante")}</button>
    <div class="audio-txt">${fuente}<div class="barra"><i id="audioAvance" style="width:${Math.round(e.avance * 100)}%"></i></div></div>
    <button class="btn mini" data-acc="audioVelocidad" aria-label="Velocidad">${velTxt(e.velocidad)}</button>
    <button class="rb" data-acc="audioCerrar" aria-label="Cerrar audiolibro">${ic("cerrar")}</button>${subir}</div>`;
}
/** Vuelve a dibujar solo el audio (no toca lo que estás escribiendo en la guía). */
function pintarAudio() {
  const b = $("audioBarra");
  if (b) { const [wb, sec] = b.dataset.cap.split("|"); b.innerHTML = htmlAudioBarra(wb, Number(sec)); }
  const e = Audio_.e, m = $("miniAudio");
  if (!m) return;
  const viendo = estado.sec === "aprende" && estado.sel === "wb:" + e.wb;
  m.hidden = !e.activo || viendo;
  if (!m.hidden) m.innerHTML = `<button class="mini-txt" data-ir="aprende" data-arg="wb:${esc(e.wb)}">${ic(e.fuente === "autor" ? "mic" : "audifonos")}<span><b>${esc(e.titulo)}</b><br><span class="suave">${esc(e.guia)}</span></span></button>
    <button class="rb" data-acc="audioAlternar" aria-label="${e.sonando ? "Pausar" : "Reanudar"}">${ic(e.sonando ? "pausa" : "play")}</button>
    <button class="rb" data-acc="audioCerrar" aria-label="Cerrar audiolibro">${ic("cerrar")}</button><div class="barra"><i id="miniAvance" style="width:${Math.round(e.avance * 100)}%"></i></div>`;
}
function pintarAvance() {
  const w = Math.round(Audio_.e.avance * 100) + "%";
  ["audioAvance", "miniAvance"].forEach(id => { const x = $(id); if (x) x.style.width = w; });
}
Object.assign(ACC, {
  escucharCap(el, arg) { const [wb, sec] = arg.split("|"); Audio_.reproducir(wb, Number(sec)); },
  audioAlternar() { Audio_.alternar(); },
  audioSaltar(el, d) { Audio_.saltar(Number(d)); },
  audioVelocidad() { Audio_.velocidad(); },
  audioCerrar() { Audio_.cerrar(); },
  quitarAudio(el, arg) {
    if (!el.dataset.seguro) { el.dataset.seguro = "1"; el.textContent = "¿Seguro?"; return; }
    const [wb, sec] = arg.split("|"); Audio_.quitar(wb, Number(sec));
  },
});
document.addEventListener("change", e => {
  const el = e.target; if (!el.dataset?.subirAudio) return;
  const [wb, sec] = el.dataset.subirAudio.split("|"); Audio_.subir(wb, Number(sec), el.files?.[0]); el.value = "";
});

/* ======================================================================
 * Mensajes 1 a 1 y coach de vida (las mismas reglas que la app: ver supabase/schema.sql)
 * ====================================================================== */
const iniciales = n => (n || "?").split(" ").map(x => x[0]).join("").slice(0, 2).toUpperCase();
const avPersona = (n, color = "var(--burdeos)") => av(esc(iniciales(n)), color);

/** Lo que ve un coach de quien acompaña (igual que ResumenCoach.kt). */
function resumenDe(filas, hoy = new Date()) {
  const de = t => filas.filter(f => f.tipo === t).map(f => f.datos || {});
  const doc = (t, c) => filas.find(f => f.tipo === t && f.clave === c)?.datos;
  const perfilX = de("perfil")[0] || {};
  const mensuales = de("meta_mes"), anuales = de("meta_anio");
  const avMes = m => m.cumplida ? 1 : Math.min(1, lista(m.dias).length / Math.max(1, m.objetivoDias || 20));
  const prom = xs => xs.reduce((a, b) => a + b, 0) / xs.length;
  const avAnio = a => { const h = mensuales.filter(m => m.metaAnualId != null && m.metaAnualId === a.id); return h.length ? prom(h.map(avMes)) : (a.avance || 0) / 100; };
  const propositos = de("proposito").map(p => {
    const h = anuales.filter(a => a.propositoId != null && a.propositoId === p.id);
    return { titulo: p.titulo, avance: Math.round((h.length ? prom(h.map(avAnio)) : (p.progreso || 0) / 100) * 100) };
  });
  const habitos7 = [6, 5, 4, 3, 2, 1, 0].map(k => { const d = new Date(hoy); d.setDate(d.getDate() - k); return lista(doc("checklist", iso(d))?.marcados).length; });
  const ult = de("ejes").sort((a, b) => (b.fecha || 0) - (a.fecha || 0))[0];
  const vision = de("vision");
  const a = hoy.getFullYear(), m = hoy.getMonth() + 1;
  return {
    nombre: perfilX.nombre || "", cumbre: perfilX.cumbreFrase || "", propositos,
    metasAnio: anuales.filter(x => x.anio === a).map(x => ({ titulo: x.titulo, avance: Math.round(avAnio(x) * 100) })),
    metasMes: mensuales.filter(x => x.anio === a && x.mes === m).map(x => ({ titulo: x.texto, avance: Math.round(avMes(x) * 100) })),
    habitos7, ejes: ult ? ["voluntad", "maestria", "voz", "valor", "evolucion", "trascendencia"].map(k => ult[k] || 0) : null,
    visionConFoto: vision.filter(v => v.publicacionId).length, visionTotal: vision.length,
  };
}

/* ---------- Datos de ejemplo (demostración) ---------- */
const DemoMsj = (() => {
  const h = x => Date.now() - x * 60000;
  const convs = [
    { id: "dc1", otroId: "coach-ana", otroNombre: "Ana Torres", otroUsuario: "anatorres", estado: "aceptada", laInicieYo: true, laBloqueeYo: false, esMiCoach: true, laAcompano: false },
    { id: "dc2", otroId: "demo-6", otroNombre: "Camilo T.", otroUsuario: "camilo", estado: "aceptada", laInicieYo: false, laBloqueeYo: false, esMiCoach: false, laAcompano: false },
    { id: "dc3", otroId: "demo-5", otroNombre: "Sofía L.", otroUsuario: "sofial", estado: "pendiente", laInicieYo: false, laBloqueeYo: false, esMiCoach: false, laAcompano: false },
    { id: "dc4", otroId: "demo-3", otroNombre: "Lucía P.", otroUsuario: "luciap", estado: "aceptada", laInicieYo: false, laBloqueeYo: false, esMiCoach: false, laAcompano: true },
  ];
  const msgs = {
    dc1: [[false, "¡Hola! Vi que esta semana marcaste 14 hábitos de 18 en promedio. ¿Cómo te sentiste?", 1500], [true, "Bien, pero el miércoles me costó mucho levantarme.", 1440],
      [false, "Es normal. Probemos dejar la ropa lista la noche anterior y revisamos el viernes.", 1430], [false, "Recuerda: tu meta del mes va en 60 %. Vas por buen camino.", 95]],
    dc2: [[false, "Gracias por el impulso a mi media maratón 🙌", 300], [true, "¡Te lo merecías! ¿Cuánto tiempo entrenaste?", 290], [false, "Diez meses, 4 días por semana. Empecé caminando.", 280]],
    dc3: [[false, "Hola, me inspiró tu publicación. ¿Puedo preguntarte cómo organizas tus metas del mes?", 40]],
    dc4: [[false, "Ya compartí mi avance contigo. ¿Lo revisamos el jueves?", 600], [true, "Claro, el jueves a las 7.", 590]],
  };
  const mensajes = {};
  Object.entries(msgs).forEach(([c, xs]) => { mensajes[c] = xs.map(([mio, texto, min], i) => ({ id: c + "-" + i, mio, texto, creadoEn: h(min), leido: mio || c !== "dc3" })); });
  mensajes.dc1[3].leido = false;
  const directorio = [
    { userId: "coach-ana", nombre: "Ana Torres", usuario: "anatorres", especialidad: "Hábitos y disciplina", bio: "Acompaño a personas que quieren sostener sus hábitos en el tiempo. Sesiones breves y semanales, con seguimiento de tu checklist.", acompanados: 12 },
    { userId: "coach-martin", nombre: "Martín Ruiz", usuario: "martinruiz", especialidad: "Carrera y propósito", bio: "Te ayudo a aterrizar tu cumbre en metas del año y del mes, y a decidir qué continuar, acelerar o pausar.", acompanados: 8 },
    { userId: "coach-elena", nombre: "Elena Gómez", usuario: "elenag", especialidad: "Finanzas personales", bio: "Para quienes tienen metas de ahorro, deudas o un emprendimiento. Trabajamos el eje Valor paso a paso.", acompanados: 5 },
  ];
  const acomps = [
    { id: "da1", rol: "usuario", otroId: "coach-ana", otroNombre: "Ana Torres", otroUsuario: "anatorres", estado: "activo", comparteAvance: true, especialidad: "Hábitos y disciplina" },
    { id: "da2", rol: "coach", otroId: "demo-3", otroNombre: "Lucía P.", otroUsuario: "luciap", estado: "activo", comparteAvance: true, especialidad: "" },
    { id: "da3", rol: "coach", otroId: "demo-1", otroNombre: "Valentina R.", otroUsuario: "valentina", estado: "solicitado", comparteAvance: false, especialidad: "" },
  ];
  const resumenLucia = {
    nombre: "Lucía P.", cumbre: "Ser una mujer disciplinada que vive de lo que ama", propositos: [{ titulo: "Salud y energía para crear", avance: 54 }, { titulo: "Mi propio estudio de diseño", avance: 31 }],
    metasAnio: [{ titulo: "Crear el hábito de madrugar", avance: 70 }, { titulo: "Conseguir 5 clientes propios", avance: 40 }],
    metasMes: [{ titulo: "Levantarme a las 5:00 20 días", avance: 75 }, { titulo: "Publicar 8 piezas de portafolio", avance: 50 }],
    habitos7: [11, 13, 9, 14, 15, 12, 10], ejes: [8, 6, 5, 6, 7, 7], visionConFoto: 5, visionTotal: 9,
  };
  return { convs, mensajes, directorio, acomps, resumenLucia, ficha: { bio: "", especialidad: "Disciplina", verificado: true } };
})();

const Msj = {
  get real() { return !!Store.nube; },
  async rpc(n, args) { const { data, error } = await Store.nube.sb.rpc(n, args); if (error) throw error; return data; },
  async conversaciones() {
    if (!this.real) return DemoMsj.convs.map(c => {
      const ms = DemoMsj.mensajes[c.id] || [], u = ms[ms.length - 1];
      return { ...c, ultimoTexto: u?.texto || "", ultimoEsMio: !!u?.mio, ultimoEn: u?.creadoEn || Date.now(), noLeidos: ms.filter(m => !m.mio && !m.leido).length, solicitudParaMi: c.estado === "pendiente" && !c.laInicieYo };
    }).filter(c => !(c.estado === "bloqueada" && !c.laBloqueeYo)).sort((a, b) => b.ultimoEn - a.ultimoEn);
    const yo = Store.nube.dueno;
    return (await this.rpc("mis_conversaciones") || []).map(o => ({
      id: o.id, otroId: o.otro, otroNombre: o.otro_nombre || o.otro_usuario || "Senderista", otroUsuario: o.otro_usuario || "", estado: o.estado,
      laInicieYo: o.iniciada_por === yo, laBloqueeYo: o.bloqueada_por === yo, ultimoTexto: o.ultimo_texto || "", ultimoEsMio: o.ultimo_autor === yo,
      ultimoEn: Date.parse(o.ultimo_en) || 0, noLeidos: o.no_leidos || 0, esMiCoach: !!o.es_coach, laAcompano: !!o.es_acompanado,
      solicitudParaMi: o.estado === "pendiente" && o.iniciada_por !== yo,
    }));
  },
  async mensajes(conv) {
    if (!this.real) return [...(DemoMsj.mensajes[conv] || [])];
    const { data, error } = await Store.nube.sb.from("mensajes").select("id,autor,texto,creado").eq("conversacion_id", conv).order("creado").limit(500);
    if (error) throw error;
    return data.map(m => ({ id: m.id, mio: m.autor === Store.nube.dueno, texto: m.texto, creadoEn: Date.parse(m.creado) }));
  },
  async enviar(conv, texto) {
    if (!this.real) {
      (DemoMsj.mensajes[conv] ||= []).push({ id: "m" + nuevoId(), mio: true, texto, creadoEn: Date.now(), leido: true });
      const c = DemoMsj.convs.find(x => x.id === conv);
      if (c && c.estado === "aceptada" && !c.laInicieYo && conv !== "dc3") setTimeout(() => {
        DemoMsj.mensajes[conv].push({ id: "m" + nuevoId(), mio: false, texto: c.esMiCoach ? "¡Gracias por contarme! Lo revisamos juntos en nuestra próxima conversación." : "¡Gracias! Seguimos subiendo 💪", creadoEn: Date.now(), leido: false });
        actualizarMensajes(true);
      }, 2500);
      return;
    }
    const { error } = await Store.nube.sb.from("mensajes").insert({ conversacion_id: conv, autor: Store.nube.dueno, texto: texto.slice(0, 2000) });
    if (error) throw error;
  },
  async abrirCon(otro, nombre = "") {
    if (!this.real) {
      let c = DemoMsj.convs.find(x => x.otroId === otro);
      if (!c) { c = { id: "dn" + nuevoId(), otroId: otro, otroNombre: nombre || "Senderista", otroUsuario: "", estado: DemoMsj.directorio.some(d => d.userId === otro) ? "aceptada" : "pendiente", laInicieYo: true, laBloqueeYo: false, esMiCoach: false, laAcompano: false }; DemoMsj.convs.push(c); }
      return c.id;
    }
    return this.rpc("abrir_conversacion", { otro });
  },
  async responder(conv, aceptar) {
    if (!this.real) { const c = DemoMsj.convs.find(x => x.id === conv); if (c) { c.estado = aceptar ? "aceptada" : "bloqueada"; c.laBloqueeYo = !aceptar; } return; }
    await this.rpc("responder_solicitud", { conv, aceptar });
  },
  async bloquear(conv, bloquear) {
    if (!this.real) { const c = DemoMsj.convs.find(x => x.id === conv); if (c) { c.estado = bloquear ? "bloqueada" : "aceptada"; c.laBloqueeYo = bloquear; } return; }
    await this.rpc("bloquear_conversacion", { conv, bloquear });
  },
  async marcarLeidos(conv) {
    if (!this.real) { (DemoMsj.mensajes[conv] || []).forEach(m => m.leido = true); return; }
    await this.rpc("marcar_leidos", { conv });
  },
  async reportar(otro, conv, motivo) {
    if (!this.real) return;
    const { error } = await Store.nube.sb.from("reportes").insert({ quien: Store.nube.dueno, a_quien: otro, conversacion_id: conv, motivo: motivo.slice(0, 1000) });
    if (error) throw error;
  },
  async directorio() {
    if (!this.real) return DemoMsj.directorio;
    return (await this.rpc("directorio_coaches") || []).map(o => ({ userId: o.user_id, nombre: o.nombre || o.usuario, usuario: o.usuario, bio: o.bio || "", especialidad: o.especialidad || "", acompanados: o.acompanados || 0 }));
  },
  async acompanamientos() {
    if (!this.real) return DemoMsj.acomps.filter(a => a.estado !== "terminado");
    return (await this.rpc("mis_acompanamientos") || []).map(o => ({ id: o.id, rol: o.rol, otroId: o.otro, otroNombre: o.otro_nombre || o.otro_usuario, otroUsuario: o.otro_usuario, estado: o.estado, comparteAvance: !!o.comparte_avance, especialidad: o.especialidad || "" }));
  },
  async solicitarCoach(coach) {
    if (!this.real) { const d = DemoMsj.directorio.find(x => x.userId === coach); DemoMsj.acomps.push({ id: "da" + nuevoId(), rol: "usuario", otroId: coach, otroNombre: d?.nombre || "", otroUsuario: d?.usuario || "", estado: "solicitado", comparteAvance: false, especialidad: d?.especialidad || "" }); return; }
    await this.rpc("solicitar_coach", { coach });
  },
  async responderAcomp(id, aceptar) {
    if (!this.real) { const a = DemoMsj.acomps.find(x => x.id === id); if (a) a.estado = aceptar ? "activo" : "terminado"; return; }
    await this.rpc("responder_acompanamiento", { acomp: id, aceptar });
  },
  async terminarAcomp(id) {
    if (!this.real) { const a = DemoMsj.acomps.find(x => x.id === id); if (a) a.estado = "terminado"; return; }
    await this.rpc("terminar_acompanamiento", { acomp: id });
  },
  async compartirAvance(id, si) {
    if (!this.real) { const a = DemoMsj.acomps.find(x => x.id === id); if (a) a.comparteAvance = si; return; }
    await this.rpc("compartir_avance", { acomp: id, si });
  },
  async miFicha() {
    if (!this.real) return DemoMsj.ficha;
    const { data } = await Store.nube.sb.from("coaches").select("bio,especialidad,verificado").eq("user_id", Store.nube.dueno).maybeSingle();
    return data || null;
  },
  async postularme(bio, especialidad) {
    if (!this.real) { DemoMsj.ficha = { bio, especialidad, verificado: false }; return; }
    await this.rpc("postularme_coach", { bio, especialidad });
  },
  async avanceDe(uid) {
    if (!this.real) return uid === "demo-3" ? DemoMsj.resumenLucia : resumenDe([]);
    const { data, error } = await Store.nube.sb.from("ruta_datos").select("tipo,clave,datos").eq("user_id", uid).limit(1000);
    if (error) throw error;
    return resumenDe(data);
  },
};

/* ---------- Estado y carga ---------- */
const MS = { convs: null, msgs: {}, cv: null, avance: {}, cargando: false };
const noLeidosTotal = () => (MS.convs || []).reduce((s, c) => s + c.noLeidos, 0);
const convActual = () => String(estado.sel || "").startsWith("c:") ? MS.convs?.find(c => c.id === estado.sel.slice(2)) : null;

async function cargarConvs() {
  if (MS.cargando) return; MS.cargando = true;
  await null;
  try { MS.convs = await Msj.conversaciones(); }
  catch (e) { console.error(e); MS.convs ||= []; }
  finally { MS.cargando = false; }
}
async function cargarCoachVida() {
  await null;
  try {
    const [acomps, dir, ficha] = await Promise.all([Msj.acompanamientos(), Msj.directorio(), Msj.miFicha()]);
    MS.cv = { acomps, dir, ficha };
  } catch (e) { console.error(e); MS.cv = { acomps: [], dir: [], ficha: null, error: true }; }
  if (estado.sec === "mensajes") { pintarLista(); if (!enEdicion()) pintarDetalle(); }
}
async function cargarAvance(uid) {
  await null;
  try { MS.avance[uid] = await Msj.avanceDe(uid); } catch (e) { console.error(e); MS.avance[uid] = { error: true }; }
  if (estado.sel === "acomp:" + uid) pintarDetalle();
}
/** Trae conversaciones y mensajes nuevos; el chat se actualiza sin tocar lo que estás escribiendo. */
async function actualizarMensajes(forzar = false) {
  if (!forzar && (document.hidden || estado.sec !== "mensajes")) return;
  await cargarConvs();
  const c = convActual();
  if (c) {
    try { MS.msgs[c.id] = await Msj.mensajes(c.id); } catch (e) { console.error(e); }
    if (c.noLeidos) { await Msj.marcarLeidos(c.id).catch(console.error); c.noLeidos = 0; }
  }
  pintarRiel();
  if (estado.sec !== "mensajes") return;
  pintarLista();
  const chat = $("chatMsj");
  if (chat && c) {
    const cuerpoChat = chat.closest(".det-cuerpo"), alFondo = cuerpoChat.scrollHeight - cuerpoChat.scrollTop - cuerpoChat.clientHeight < 80;
    chat.innerHTML = htmlMensajes(c.id);
    $("pieChat").innerHTML = htmlPieChat(c);
    if (alFondo) cuerpoChat.scrollTop = cuerpoChat.scrollHeight;
  } else if (!enEdicion()) pintarDetalle();
}
setInterval(() => actualizarMensajes(), 5000);
setInterval(() => { if (Store.nube && estado.sec !== "mensajes" && !document.hidden) cargarConvs().then(pintarRiel); }, 30000);

/* ---------- Lista ---------- */
LISTAS.mensajes = function () {
  let h = cabLista("Mensajes", { buscar: true }) + `<div class="items">`;
  const mio = MS.cv?.acomps.find(a => a.rol === "usuario");
  h += item("coachvida", av(ic("cumbre"), "var(--oro)"), "Coach de vida", "", mio ? `Tu coach: ${esc(mio.otroNombre)}` : "Una persona que te acompaña en tu ascenso");
  if (!MS.cv) cargarCoachVida();
  if (!MS.convs) { cargarConvs().then(() => { if (estado.sec === "mensajes") { pintarLista(); pintarRiel(); } }); return h + `<p class="vacio-mini">Cargando…</p></div>`; }
  const fila = c => item("c:" + c.id, avPersona(c.otroNombre, c.esMiCoach || c.laAcompano ? "var(--oro)" : "var(--burdeos)"),
    esc(c.otroNombre) + (c.esMiCoach ? ` <span class="pill">Tu coach</span>` : c.laAcompano ? ` <span class="pill">Acompañas</span>` : ""), c.ultimoEn ? hace(c.ultimoEn) : "",
    c.solicitudParaMi ? "Quiere escribirte" : c.estado === "bloqueada" ? "Conversación bloqueada" : !c.ultimoTexto ? "Sin mensajes todavía" : (c.ultimoEsMio ? "Tú: " : "") + esc(c.ultimoTexto),
    c.noLeidos ? `<span class="badge">${c.noLeidos}</span>` : "");
  const vis = MS.convs.filter(c => coincide(c.otroNombre + " " + c.ultimoTexto));
  const sol = vis.filter(c => c.solicitudParaMi), resto = vis.filter(c => !c.solicitudParaMi);
  if (sol.length) { h += grupo("Solicitudes"); sol.forEach(c => h += fila(c)); h += grupo("Conversaciones"); }
  resto.forEach(c => h += fila(c));
  if (!MS.convs.length) h += `<p class="vacio-mini">Aún no tienes conversaciones. Escríbele a alguien desde una publicación de la comunidad o pregúntale a un coach de vida.</p>`;
  return h + `</div>`;
};

/* ---------- Chat ---------- */
function htmlMensajes(id) {
  let ultimoDia = "";
  return (MS.msgs[id] || []).map(m => {
    const d = new Date(m.creadoEn).toLocaleDateString("es", { day: "numeric", month: "long" }); const sep = d !== ultimoDia ? `<span class="fecha-sep">${d}</span>` : ""; ultimoDia = d;
    return sep + `<div class="burbuja ${m.mio ? "yo" : "el"}">${esc(m.texto)}<span class="h">${horaDe(m.creadoEn)}</span></div>`;
  }).join("");
}
function htmlPieChat(c) {
  const ms = MS.msgs[c.id] || [];
  if (c.solicitudParaMi) return `<div class="aviso" style="margin:8px 12px;flex-wrap:wrap"><span style="flex:1;min-width:220px">${esc(c.otroNombre)} quiere escribirte. Si aceptas, podrán conversar; si rechazas, no sabrá nada más de ti.</span>
    <button class="btn mini lleno" data-acc="responderChat" data-arg="${esc(c.id)}|1">Aceptar</button><button class="btn mini" data-acc="responderChat" data-arg="${esc(c.id)}|0">Rechazar</button></div>`;
  if (c.estado === "bloqueada") return `<p class="suave" style="text-align:center;margin:12px">Conversación bloqueada${c.laBloqueeYo ? " · puedes desbloquearla arriba" : ""}.</p>`;
  if (c.estado === "pendiente" && c.laInicieYo && ms.length) return `<p class="suave" style="text-align:center;margin:12px">Tu mensaje llegó como solicitud. Podrás seguir escribiendo cuando la acepte.</p>`;
  return `<form class="escribir" data-form="mensaje" data-arg="${esc(c.id)}"><input name="texto" maxlength="2000" placeholder="${c.estado === "pendiente" ? "Preséntate en un mensaje" : "Escribe un mensaje"}" aria-label="Mensaje" autocomplete="off"><button class="enviar" aria-label="Enviar">${ic("enviar")}</button></form>`;
}
DET["mensajes.c"] = id => {
  const c = MS.convs?.find(x => x.id === id);
  if (!c) { if (!MS.convs) cargarConvs().then(() => pintarDetalle()); return vacioMensajes(); }
  if (!MS.msgs[id]) { MS.msgs[id] = []; actualizarMensajes(true); }
  const etiqueta = c.esMiCoach ? "Tu coach de vida" : c.laAcompano ? "Lo acompañas como coach" : c.otroUsuario ? "@" + esc(c.otroUsuario) : "";
  return cab(avPersona(c.otroNombre, c.esMiCoach || c.laAcompano ? "var(--oro)" : "var(--burdeos)"), esc(c.otroNombre), etiqueta,
    `<span style="display:flex;gap:6px">${c.laAcompano ? `<button class="btn mini" data-sel="acomp:${esc(c.otroId)}">Ver avance</button>` : ""}
      <button class="btn mini" data-acc="bloquearChat" data-arg="${esc(c.id)}|${c.estado === "bloqueada" && c.laBloqueeYo ? 0 : 1}">${c.estado === "bloqueada" && c.laBloqueeYo ? "Desbloquear" : "Bloquear"}</button>
      <button class="btn mini peligro" data-acc="reportarChat" data-arg="${esc(c.id)}">Reportar</button></span>`) +
    `<div class="det-cuerpo abajo"><div class="chat" id="chatMsj">${htmlMensajes(id)}</div></div><div id="pieChat">${htmlPieChat(c)}</div>`;
};
function vacioMensajes() {
  return `<div class="vacio"><div><img src="${LOGO}" alt=""><h2>Mensajes</h2>
    <p>Conversa con otras personas de la comunidad y con tu coach de vida. Si no se siguen mutuamente, el primer mensaje llega como solicitud y la otra persona decide si acepta.</p>
    <p class="pie suave">${ic("candado")} Puedes bloquear o reportar cualquier conversación.</p></div></div>`;
}

/* ---------- Coach de vida ---------- */
const barra = (pct) => `<div class="barra"><i style="width:${Math.max(0, Math.min(100, pct))}%"></i></div>`;
DET["mensajes.coachvida"] = () => {
  const cv = MS.cv;
  const titulo = cab(av(ic("cumbre"), "var(--oro)"), "Coach de vida", "Una persona real que te acompaña en tu ascenso");
  if (!cv) { cargarCoachVida(); return titulo + cuerpo(`<p class="suave">Cargando…</p>`); }
  const mio = cv.acomps.find(a => a.rol === "usuario"), acompanados = cv.acomps.filter(a => a.rol === "coach");
  let h = `<p style="margin:0">Conversa contigo, revisa tu avance si se lo permites y te ayuda a no rendirte.</p>`;
  if (!Store.nube) h += `<div class="aviso">Demostración: coaches y conversaciones de ejemplo.</div>`;
  if (mio) {
    h += `<div class="etiqueta">Mi coach</div>` + hoja(`<div class="autor" style="display:flex;gap:12px;align-items:center">${avPersona(mio.otroNombre, "var(--oro)")}<div><b>${esc(mio.otroNombre)}</b><div class="suave" style="font-size:13px">${esc(mio.especialidad)}</div></div></div>
      ${mio.estado === "solicitado" ? `<p class="suave">Esperando que acepte tu solicitud.</p>` : `
        <div class="botones" style="margin-top:12px"><button class="btn lleno" data-acc="escribirA" data-arg="${esc(mio.otroId)}|${esc(mio.otroNombre)}">${ic("mensajes")} Escribir</button></div>
        <label style="display:flex;align-items:flex-start;gap:12px;margin-top:14px;cursor:pointer"><input type="checkbox" style="width:20px;height:20px;margin-top:2px;flex:none;accent-color:var(--burdeos)" data-acc-change="compartirAvance" data-arg="${esc(mio.id)}" ${mio.comparteAvance ? "checked" : ""}>
          <span><b>Compartir mi avance</b><br><span class="suave" style="font-size:13px">Tu cumbre, propósitos, metas, hábitos de la semana, ejes y vision board. Nunca tus respuestas de los libros ni tus notas.</span></span></label>`}
      <div class="botones" style="margin-top:8px"><button class="btn mini peligro" data-acc="terminarAcomp" data-arg="${esc(mio.id)}">${mio.estado === "solicitado" ? "Cancelar solicitud" : "Terminar acompañamiento"}</button></div>`);
  }
  if (acompanados.length) {
    h += `<div class="etiqueta">Personas que acompañas</div>`;
    acompanados.forEach(a => {
      h += hoja(`<div style="display:flex;gap:12px;align-items:center">${avPersona(a.otroNombre)}<div style="flex:1"><b>${esc(a.otroNombre)}</b><div class="suave" style="font-size:13px">${a.estado === "solicitado" ? "Te pide que seas su coach" : a.comparteAvance ? "Comparte su avance contigo" : "Todavía no comparte su avance"}</div></div>
        ${a.estado === "solicitado" ? `<button class="btn mini lleno" data-acc="responderAcomp" data-arg="${esc(a.id)}|1">Aceptar</button><button class="btn mini" data-acc="responderAcomp" data-arg="${esc(a.id)}|0">Rechazar</button>`
          : `<button class="btn mini" data-sel="acomp:${esc(a.otroId)}">Ver avance</button><button class="btn mini" data-acc="escribirA" data-arg="${esc(a.otroId)}|${esc(a.otroNombre)}">${ic("mensajes")}</button>`}</div>`);
    });
  }
  if (!mio) {
    h += `<div class="etiqueta">Coaches verificados</div>`;
    if (!cv.dir.length) h += `<p class="suave">Aún no hay coaches verificados. Vuelve pronto.</p>`;
    cv.dir.forEach(f => {
      h += hoja(`<div style="display:flex;gap:12px;align-items:center">${avPersona(f.nombre, "var(--oro)")}<div style="flex:1"><b>${esc(f.nombre)}</b> <span class="pill">${esc(f.especialidad)}</span><div class="suave" style="font-size:13px">Acompaña a ${f.acompanados} personas</div></div></div>
        ${f.bio ? `<p style="margin:10px 0 0">${esc(f.bio)}</p>` : ""}
        <div class="botones" style="margin-top:10px"><button class="btn lleno" data-acc="solicitarCoach" data-arg="${esc(f.userId)}">Pedir que sea mi coach</button><button class="btn" data-acc="escribirA" data-arg="${esc(f.userId)}|${esc(f.nombre)}">Preguntar</button></div>`);
    });
  }
  h += `<div class="etiqueta">Ser coach de vida</div>`;
  const fi = cv.ficha;
  h += hoja(fi?.verificado ? `<p style="margin:0">Eres coach verificado. Las personas pueden pedirte que las acompañes desde el directorio.</p>`
    : fi ? `<p style="margin:0">Tu postulación está en revisión. Aparecerás en el directorio cuando el equipo de Rutaalacima la verifique.</p>`
    : `<form class="form" data-form="postular"><p style="margin:0">Si acompañas personas profesionalmente, postúlate. Tu ficha aparecerá en el directorio cuando el equipo de Rutaalacima la verifique.</p>
        ${campo("Especialidad", `<input type="text" name="especialidad" maxlength="120" required placeholder="Hábitos, carrera, finanzas…">`)}
        ${campo("Cómo acompañas", `<textarea name="texto" maxlength="1200" required></textarea>`)}
        <div class="botones"><button class="btn lleno" type="submit">Enviar postulación</button></div></form>`);
  return titulo + cuerpo(h, "max-width:680px");
};
DET["mensajes.acomp"] = uid => {
  const a = MS.cv?.acomps.find(x => x.otroId === uid), nombre = a?.otroNombre || "Avance";
  const r = MS.avance[uid];
  const titulo = cab(avPersona(nombre, "var(--oro)"), esc(nombre), "Lo que decidió compartir contigo",
    `<button class="btn mini" data-acc="escribirA" data-arg="${esc(uid)}|${esc(nombre)}">${ic("mensajes")} Escribir</button>`);
  if (!r) { cargarAvance(uid); return titulo + cuerpo(`<p class="suave">Cargando…</p>`); }
  if (r.error) return titulo + cuerpo(`<p class="suave">No se pudo cargar. Revisa tu conexión.</p>`);
  const bloqueX = (t, xs) => xs.length ? hoja(`<div class="etiqueta">${t}</div>` + xs.map(x => `<div style="margin-top:10px"><div style="display:flex;justify-content:space-between;gap:12px"><span>${esc(x.titulo)}</span><b>${x.avance}%</b></div>${barra(x.avance)}</div>`).join("")) : "";
  const prom = r.habitos7.reduce((s, n) => s + n, 0) / 7;
  let h = `<p class="suave" style="margin:0">Solo ves lo que esta persona decidió compartir contigo, y deja de verse si ella lo desactiva.</p>`;
  if (r.cumbre) h += hoja(`<div class="etiqueta">Su cumbre</div><p style="font-size:18px;margin:6px 0 0">${esc(r.cumbre)}</p>`);
  h += hoja(`<div class="etiqueta">Hábitos de los últimos 7 días</div>
    <div style="display:flex;gap:6px;align-items:flex-end;height:80px;margin-top:10px">${r.habitos7.map(n => `<span title="${n} de 18" style="flex:1;border-radius:6px 6px 0 0;height:${Math.max(4, n / 18 * 100)}%;background:${n >= 10 ? "var(--oro)" : "color-mix(in srgb, var(--burdeos) 50%, transparent)"}"></span>`).join("")}</div>
    <p class="suave" style="margin:6px 0 0;font-size:13px">Promedio: ${prom.toFixed(1)} de 18 hábitos al día</p>`);
  h += bloqueX("Propósitos", r.propositos) + bloqueX("Metas del año", r.metasAnio) + bloqueX("Metas del mes", r.metasMes);
  if (r.ejes) h += hoja(`<div class="etiqueta">Ejes</div>` + EJES.map(([c, n], i) => `<div style="display:grid;grid-template-columns:120px 1fr 24px;gap:10px;align-items:center;margin-top:8px"><span>${n}</span>${barra(r.ejes[i] * 10)}<b>${r.ejes[i]}</b></div>`).join(""));
  if (r.visionTotal) h += hoja(`<p style="margin:0">Vision board: ${r.visionConFoto} de ${r.visionTotal} casillas con su foto</p>`);
  return titulo + cuerpo(h, "max-width:680px");
};

/* ---------- Acciones ---------- */
async function hacer(f, ok = "") {
  try { await f(); if (ok) toast(ok); } catch (e) { console.error(e); toast(e?.message || "No se pudo completar. Revisa tu conexión."); }
}
Object.assign(ACC, {
  async escribirA(el, arg) {
    const [otro, nombre] = arg.split("|");
    await hacer(async () => {
      const id = await Msj.abrirCon(otro, nombre);
      await cargarConvs(); MS.msgs[id] = null;
      ir("mensajes", "c:" + id);
    });
  },
  async responderChat(el, arg) { const [id, si] = arg.split("|"); await hacer(() => Msj.responder(id, si === "1")); await actualizarMensajes(true); pintarDetalle(); },
  async bloquearChat(el, arg) { const [id, si] = arg.split("|"); await hacer(() => Msj.bloquear(id, si === "1"), si === "1" ? "Conversación bloqueada" : "Conversación desbloqueada"); await actualizarMensajes(true); pintarDetalle(); },
  async reportarChat(el, id) {
    const c = MS.convs?.find(x => x.id === id); if (!c) return;
    const motivo = prompt(`¿Qué pasó con ${c.otroNombre}?`); if (motivo === null) return;
    await hacer(() => Msj.reportar(c.otroId, c.id, motivo || "Sin detalle"), "Gracias. Revisaremos tu reporte.");
  },
  async solicitarCoach(el, id) { el.disabled = true; await hacer(() => Msj.solicitarCoach(id), "Solicitud enviada"); MS.cv = null; cargarCoachVida(); },
  async responderAcomp(el, arg) { const [id, si] = arg.split("|"); await hacer(() => Msj.responderAcomp(id, si === "1")); MS.cv = null; cargarCoachVida(); },
  async terminarAcomp(el, id) {
    if (!el.dataset.seguro) { el.dataset.seguro = "1"; el.textContent = "¿Seguro? Toca otra vez"; return; }
    await hacer(() => Msj.terminarAcomp(id)); MS.cv = null; cargarCoachVida();
  },
  async compartirAvance(el, id) { await hacer(() => Msj.compartirAvance(id, el.checked), el.checked ? "Tu coach ya puede ver tu avance" : "Dejaste de compartir tu avance"); MS.cv = null; cargarCoachVida(); },
});
async function enviarMensaje(f, texto) {
  const id = f.dataset.arg; if (!texto) return;
  f.reset(); f.querySelector("input")?.focus();
  (MS.msgs[id] ||= []).push({ id: "tmp" + nuevoId(), mio: true, texto, creadoEn: Date.now() });
  const chat = $("chatMsj"); if (chat) { chat.innerHTML = htmlMensajes(id); chat.closest(".det-cuerpo").scrollTop = 1e9; }
  try { await Msj.enviar(id, texto); } catch (e) { console.error(e); toast("No se envió el mensaje. Inténtalo de nuevo."); }
  await actualizarMensajes(true);
}

/* ======================================================================
 * Pantallas: vincular, cargando, app
 * ====================================================================== */
function mostrar(cual) {
  $("pantallaVincular").hidden = cual !== "vincular";
  $("pantallaCarga").hidden = cual !== "carga";
  $("pantallaApp").hidden = cual !== "app";
}
function mostrarCarga(t) { $("pantallaCarga").innerHTML = `<div><img src="${LOGO}" alt=""><p>${esc(t)}</p></div>`; mostrar("carga"); }

let sondeo = null, renovar = null;
async function mostrarVincular() {
  clearInterval(sondeo); clearInterval(renovar);
  const real = !!Store.nube;
  $("pantallaVincular").innerHTML = `<div class="marca"><img src="${LOGO}" alt="">Rutaalacima Web</div>
    <div class="tarjeta-v"><div><h1>Usa Rutaalacima en tu computador</h1>
      <ol class="pasos"><li>Abre <b>Rutaalacima</b> en tu teléfono.</li><li>Ve a <b>Perfil</b> y toca el ícono del computador (<b>Rutaalacima Web</b>).</li><li>Toca <b>Escanear código</b> y apunta tu teléfono a este código.</li></ol>
      <p class="suave" style="margin-top:22px;max-width:46ch">Es la misma app, con tu misma cuenta: lo que hagas aquí aparece en tu teléfono, y lo que hagas en el teléfono aparece aquí.</p></div>
    <div><div class="qr" id="qr"><img class="centro" src="${LOGO}" alt=""></div><div class="codigo-txt" id="codigoTxt">${real ? "· · · ·" : ""}</div>
      <p class="suave" style="text-align:center;font-size:13px;margin:8px 0 0" id="qrNota">${real ? "¿Sin cámara? Escribe este código en la app." : "Código de muestra: abre el proyecto en GitHub"}</p></div>
    <div class="v-pie">${real ? `<span class="suave">El código cambia cada pocos minutos. Este computador queda vinculado hasta que lo desvincules.</span>`
      : `<span><span class="pill">Demostración</span> <span class="suave">Este sitio todavía no tiene el servidor de Rutaalacima configurado. Prueba la app con una ruta de ejemplo.</span></span>`}
      <button class="btn ${real ? "" : "lleno"}" data-acc-v="demo">Ver la demostración</button></div></div>`;
  mostrar("vincular");
  if (!real) { pintarQR("https://github.com/PassBri/rutacima"); return; }
  const nuevo = async () => {
    try {
      const c = await Store.nube.crearCodigo();
      pintarQR(`rutacima://vincular?codigo=${c}`);
      $("codigoTxt").textContent = `${c.slice(0, 4)}-${c.slice(4)}`;
    } catch (e) { console.error(e); $("qrNota").textContent = "No se pudo crear el código. Revisa tu conexión y recarga la página."; }
  };
  await nuevo();
  renovar = setInterval(nuevo, 4.5 * 60000);
  sondeo = setInterval(async () => {
    try { if (await Store.nube.vinculo()) { clearInterval(sondeo); clearInterval(renovar); entrarReal(); } } catch {}
  }, 2500);
}
function pintarQR(texto) {
  const box = $("qr"); box.querySelector(".codigo")?.remove();
  try {
    const q = qrcode(0, "H"); q.addData(texto); q.make();
    const img = document.createElement("img"); img.className = "codigo"; img.alt = "Código QR para vincular"; img.src = q.createDataURL(8, 0); box.prepend(img);
  } catch { box.insertAdjacentHTML("afterbegin", `<p class="suave codigo">Código QR no disponible</p>`); }
}

async function entrarReal() {
  mostrarCarga("Cargando tu ruta…");
  try {
    const filas = await Store.nube.cargarTodo();
    Store.datos = {};
    filas.forEach(f => Store.poner(f.tipo, f.clave, f.datos));
    let t = null;
    Store.nube.escuchar(ev => {
      if (ev.eventType === "DELETE") Store.quitar(ev.old.tipo, ev.old.clave);
      else Store.poner(ev.new.tipo, ev.new.clave, ev.new.datos);
      clearTimeout(t); t = setTimeout(refrescar, 250);
    });
    // Respaldo por si se pierde algún aviso en vivo
    setInterval(async () => {
      try { const fs = await Store.nube.cargarTodo(); Store.datos = {}; fs.forEach(f => Store.poner(f.tipo, f.clave, f.datos)); refrescar(); } catch {}
    }, 120000);
    abrirApp();
    if (!filas.length) toast("Tu cuenta aún no tiene datos: abre la app en el teléfono para que se sincronice.");
  } catch (e) { console.error(e); mostrarCarga("No se pudo cargar tu ruta. Revisa tu conexión y recarga la página."); }
}
function entrarDemo() { Store.nube = null; Store.datos = {}; sembrarDemo(); abrirApp(); }
function abrirApp() {
  Store.oyentes.clear(); Store.oyentes.add(refrescar);
  mostrar("app");
  ir("ruta", innerWidth > 900 && recordatorio() ? "vida" : null);
}
document.addEventListener("click", e => { if (e.target.closest("[data-acc-v='demo']")) { clearInterval(sondeo); clearInterval(renovar); entrarDemo(); } });

/* Ruta de ejemplo para la demostración (con la misma forma de los datos reales) */
function sembrarDemo() {
  const hoy = hoyFecha(), A = hoy.getFullYear(), M = hoy.getMonth() + 1;
  const azar = (a, m, d) => { const x = Math.sin(a * 372 + m * 31 + d) * 10000; return x - Math.floor(x); };
  Store.poner("perfil", 1, { id: 1, nombre: "Laura", cumbreFrase: "Abrir mi escuela de montaña y vivir de enseñar a otros a subir.", faseActual: "PREPARACION", anioInicioPlan: A, onboardingCompleto: true, anioNacimiento: 1990, mesNacimiento: 3, esperanzaVida: 100 });
  Store.poner("proposito", 1, { id: 1, orden: 0, titulo: "Escuela de montaña propia", prioridad: "A", eje: "TRA", descripcion: "Una escuela donde aprender a subir sea aprender a vivir.", indicadorExito: "La escuela se sostiene sola con 3 grupos al año.", porQueImporta: "", metasEspecificas: "", visualizacion: "", impacto: "", reflexionFinal: "", progreso: 0, horizonte: 10, creadoEn: Date.now() });
  [["Certificarme como guía de montaña", "MAE", A, 1], ["Ahorrar el capital semilla de la escuela", "VAL", A, 1], ["Correr una media maratón", "VOL", A, null], ["Primer grupo de 12 estudiantes", "VOZ", A + 2, 1], ["Sede propia con equipo de alquiler", "VAL", A + 4, 1]]
    .forEach(([t, eje, anio, prop], i) => Store.poner("meta_anio", 10 + i, { id: 10 + i, anio, propositoId: prop, titulo: t, subMetas: "", decision: "CONTINUAR", prioridad: "A", avance: anio > A ? 0 : 20, estado: anio > A ? "NO_INICIADA" : "EN_CURSO", obstaculo: "", proximaAccion: "", eje, indicador: "", observable: "" }));
  [["Terminar el módulo 4 del curso de guía", "MAE", 10], ["Ahorrar 10 % del ingreso del mes", "VAL", 11], ["3 trotes por semana", "VOL", 12], ["Publicar una ruta guiada en la comunidad", "VOZ", null]].forEach(([t, eje, anual], i) => {
    const dias = []; for (let d = 1; d < hoy.getDate(); d++) if (azar(A, M + i, d) < 0.55) dias.push(d);
    Store.poner("meta_mes", 20 + i, { id: 20 + i, anio: A, mes: M, orden: i, texto: t, dias: dias.join(","), cumplida: false, metaAnualId: anual, eje, indicador: "", objetivoDias: 20 });
  });
  for (let k = 1; k < 420; k++) {
    const d = new Date(hoy); d.setDate(d.getDate() - k);
    if (azar(d.getFullYear(), d.getMonth(), d.getDate()) < 0.62) Store.poner("checklist", iso(d), { fecha: iso(d), marcados: HABITOS.filter((h, j) => azar(d.getDate(), j, d.getMonth()) < 0.5).map(h => h[0]).join(",") });
  }
  Store.poner("agenda", iso(hoy), { fecha: iso(hoy), intencion: "Subir con calma y constancia", prioridad: "Estudiar el módulo 4 (1 hora)" });
  Store.poner("ejes", 30, { id: 30, fecha: Date.now() - 20 * DIA_MS, voluntad: 8, maestria: 7, voz: 5, valor: 6, evolucion: 7, trascendencia: 6, origen: "rapida", nota: "" });
  const dias = []; for (let k = 1; k < 30; k++) { const d = new Date(hoy); d.setDate(d.getDate() - k); if (d.getFullYear() === A && azar(k, 3, 7) < 0.7) dias.push(fraseIndice(d)); }
  Store.poner("frases", A, { dias: dias.sort((x, y) => x - y) });
  misPostsDemo.length = 0;
  [["LOGRO", "VOL", "Primer mes completo trotando 3 veces por semana."], ["VISION", "TRA", "Una escuela donde aprender a subir sea aprender a vivir."], ["EVIDENCIA", "MAE", "Módulo 3 del curso de guía: aprobado."]]
    .forEach(([tipo, eje, t], i) => misPostsDemo.push({ id: "mio-" + i, autorNombre: "Laura", tipo, eje, texto: t, metaTitulo: "", impulsos: 12 - i * 3, comentarios: 1, yoImpulse: false, creadoEn: Date.now() - (i + 2) * 86400000, propio: true, demo: false }));
}

document.addEventListener("dblclick", e => {
  const c = e.target.closest("[data-cima]"); if (!c || c.closest("button")) return;
  const p = (estado.feed || []).find(x => String(x.id) === c.dataset.cima);
  if (p && !p.yoImpulse) { estado.post = p; ACC.impulsar(); }
  const destello = document.createElement("span"); destello.className = "destello"; destello.innerHTML = ic("impulso");
  const nueva = $("detalle").querySelector(`[data-cima="${CSS.escape(c.dataset.cima)}"]`) || c; nueva.appendChild(destello); setTimeout(() => destello.remove(), 700);
});
async function iniciar() {
  try { const t = localStorage.getItem("rutacima-tema"); if (t) document.documentElement.dataset.theme = t; } catch {}
  ["lista", "detalle", "riel"].forEach(id => { $(id).onclick = clic; });
  $("detalle").onchange = cambio; $("detalle").oninput = entrada; $("lista").oninput = entrada; $("detalle").onsubmit = enviar;
  Contenido.cargarFrases(); Contenido.cargarIndice();
  if (!MODO_REAL) { if (location.hash === "#demo") entrarDemo(); else mostrarVincular(); return; }
  mostrarCarga("Conectando…");
  try {
    Store.nube = new Nube();
    await Store.nube.sesion();
    if (await Store.nube.vinculo()) entrarReal(); else mostrarVincular();
  } catch (e) {
    console.error(e);
    Store.nube = null;
    mostrarCarga("No se pudo conectar con el servidor de Rutaalacima. Revisa que los inicios de sesión anónimos estén activados (ver README) y recarga.");
  }
}
iniciar();
