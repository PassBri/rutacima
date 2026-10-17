"use strict";
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
