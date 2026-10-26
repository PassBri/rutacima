"use strict";
/* ======================================================================
 * Bienestar (igual que la app, domain/model/Bienestar.kt): inteligencia emocional y hábitos saludables.
 * · Clima interior: el «medidor del ánimo» de Yale (RULER) con el lenguaje de la montaña. Cuatro climas
 *   según la energía (alta o baja) y el agrado (agradable o desagradable), la emoción con su nombre, su
 *   intensidad, qué la provocó, una herramienta de regulación de un minuto y una nota.
 * · Una práctica por semana de los cinco componentes de Goleman.
 * · Vida saludable con las metas de la OMS: sueño de 7 a 9 h, 150 min de movimiento a la semana,
 *   5 porciones de frutas y verduras, agua (las cantimploras del día), aire libre y sin pantallas antes
 *   de dormir. Sin peso ni calorías. Índice de vida saludable de la semana y racha sin castigo.
 * Cada día se guarda en "bien#aaaa-mm-dd" (workbook "bienestar"). Es privado: nunca va a la comunidad.
 * ====================================================================== */
const BIEN_WB = "bienestar";
const CLIMAS = {
  sol: { n: "Sol en la cumbre", d: "Energía alta · agradable", c: "#E8A317", e: ["Alegre", "Entusiasmado", "Motivado", "Orgulloso", "Esperanzado", "Inspirado"] },
  tormenta: { n: "Tormenta", d: "Energía alta · desagradable", c: "#E0662A", e: ["Enojado", "Ansioso", "Frustrado", "Estresado", "Preocupado", "Irritado"] },
  niebla: { n: "Niebla", d: "Energía baja · desagradable", c: "#7A8896", e: ["Triste", "Cansado", "Desanimado", "Solo", "Aburrido", "Agotado"] },
  valle: { n: "Valle en calma", d: "Energía baja · agradable", c: "#3E8A5A", e: ["Tranquilo", "Agradecido", "Relajado", "Satisfecho", "En paz", "Seguro"] },
};
const CAUSAS = ["Trabajo", "Familia", "Pareja", "Amistades", "Salud", "Dinero", "Estudio", "Sueño", "Yo mismo", "Otra"];
const HERRAMIENTAS = [
  ["caja", "Respiración en caja", "Inhala 4, sostén 4, exhala 4, sostén 4. Un minuto baja la activación del cuerpo."],
  ["stop", "Pausa STOP", "Para. Toma aire. Observa qué sientes y piensas. Prosigue con lo que sí depende de ti."],
  ["mirada", "Otra mirada", "¿Qué le dirías a un amigo en tu lugar? ¿Qué otra forma hay de ver esto?"],
  ["mover", "Muévete 5 minutos", "Camina, estírate o sube escaleras: el cuerpo ayuda a cambiar el clima."],
  ["hablar", "Habla con alguien", "Cuéntaselo a una persona de confianza. Nombrarlo en voz alta lo hace más liviano."],
];
const GOLEMAN = [
  ["Autoconciencia", "Tres veces al día pregúntate: ¿qué siento y dónde lo siento en el cuerpo? Ponle nombre exacto."],
  ["Autorregulación", "Antes de responder algo que te molestó, haz una respiración en caja y espera un minuto."],
  ["Motivación", "Cada noche escribe un paso pequeño que diste hacia tu cumbre, aunque el día haya sido difícil."],
  ["Empatía", "En una conversación al día, escucha sin interrumpir y repite lo que entendiste antes de opinar."],
  ["Habilidades sociales", "Agradece o reconoce algo concreto a una persona cada día, en voz alta o por escrito."],
];
const SALUD_PILARES = [
  ["sueno", "Sueño", "7 a 9 h", "horas"], ["mov", "Actividad física", "150 min por semana", "min"], ["fyv", "Frutas y verduras", "5 porciones", "porciones"],
  ["agua", "Agua", "8 cantimploras", "cantimploras"], ["aire", "Aire libre y sol", "20 min", "min"], ["pant", "Sin pantallas al dormir", "1 h antes", ""],
];
const claveBien = f => `bien#${f}`;
function bienDia(f) { try { return JSON.parse(respuesta(claveBien(f)) || "{}") || {}; } catch { return {}; } }
function guardarBien(f, cambio) { const d = bienDia(f); cambio(d); responder(BIEN_WB, claveBien(f), Object.keys(d).length ? JSON.stringify(d) : ""); }
const aguaDe = f => Number((Store.get("agenda", f) || {}).agua) || 0;
const diasAtras = n => { const r = []; const d = hoyFecha(); for (let i = 0; i < n; i++) { const x = new Date(d); x.setDate(d.getDate() - i); r.push(iso(x)); } return r; };
const semanaIso = d => { const t = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate())); const dia = t.getUTCDay() || 7; t.setUTCDate(t.getUTCDate() + 4 - dia); return Math.ceil(((t - Date.UTC(t.getUTCFullYear(), 0, 1)) / 864e5 + 1) / 7); };

/** Cuánto se cumplió cada pilar en un día (0 a 1). */
function pilaresDia(f) {
  const b = bienDia(f);
  return { sueno: b.sueno != null ? Math.min(1, b.sueno / 7) * (b.sueno > 10 ? .8 : 1) : null, mov: b.mov != null ? Math.min(1, b.mov / 30) : null,
    fyv: b.fyv != null ? Math.min(1, b.fyv / 5) : null, agua: Math.min(1, aguaDe(f) / 8) || (b.sueno != null || b.mov != null ? 0 : null),
    aire: b.aire != null ? Math.min(1, b.aire / 20) : null, pant: b.pant != null ? (b.pant ? 1 : 0) : null };
}
/** Índice de vida saludable de los últimos 7 días (0 a 100) y la racha de días con 4 de 6 pilares cumplidos. */
function indiceSaludable() {
  const dias = diasAtras(7), pil = {};
  SALUD_PILARES.forEach(([k]) => { const v = dias.map(f => pilaresDia(f)[k]).filter(x => x != null); pil[k] = v.length ? v.reduce((a, x) => a + x, 0) / 7 : null; });
  const movSem = dias.reduce((a, f) => a + (Number(bienDia(f).mov) || 0), 0); pil.mov = movSem || dias.some(f => bienDia(f).mov != null) ? Math.min(1, movSem / 150) : null;
  const vals = Object.values(pil).filter(x => x != null), indice = vals.length ? Math.round(vals.reduce((a, x) => a + x, 0) / SALUD_PILARES.length * 100) : null;
  let racha = 0; for (const f of diasAtras(365)) { const p = pilaresDia(f), ok = Object.values(p).filter(x => x != null && x >= .99).length; if (ok >= 4) racha++; else if (f !== iso(hoyFecha())) break; }
  return { pil, indice, racha, movSem };
}
/** Los climas de los últimos días y lo que se repite. */
function patronesClima(n = 30) {
  const dias = diasAtras(n).map(f => [f, bienDia(f)]).filter(([, b]) => b.clima);
  const cuenta = { sol: 0, tormenta: 0, niebla: 0, valle: 0 }, emo = {}, causas = {};
  dias.forEach(([, b]) => { cuenta[b.clima]++; if (b.emo) emo[b.emo] = (emo[b.emo] || 0) + 1; if (b.clima === "tormenta" || b.clima === "niebla") (b.causas || []).forEach(c => { causas[c] = (causas[c] || 0) + 1; }); });
  const conSueno = dias.filter(([, b]) => b.sueno != null), bienDormido = conSueno.filter(([, b]) => b.sueno >= 7), agr = x => x.filter(([, b]) => b.clima === "sol" || b.clima === "valle").length;
  return { dias, cuenta, emo: Object.entries(emo).sort((a, b) => b[1] - a[1]).slice(0, 5), causas: Object.entries(causas).sort((a, b) => b[1] - a[1]).slice(0, 3),
    sueno: bienDormido.length >= 3 && conSueno.length - bienDormido.length >= 3 ? [Math.round(agr(bienDormido) / bienDormido.length * 100), Math.round(agr(conSueno.filter(([, b]) => b.sueno < 7)) / (conSueno.length - bienDormido.length) * 100)] : null };
}

LISTAS.bienestar = () => {
  const hoyI = iso(hoyFecha()), b = bienDia(hoyI), is = indiceSaludable(), [gn] = GOLEMAN[semanaIso(hoyFecha()) % 5];
  let h = cabLista("Bienestar", { buscar: false }) + `<div class="items">`;
  h += item("hoy", av(b.clima ? `<span class="bien-punto" style="background:${CLIMAS[b.clima].c}"></span>` : ic("bienestar"), "var(--ok)"), "Tu clima de hoy", "", b.clima ? `${CLIMAS[b.clima].n}${b.emo ? " · " + b.emo : ""}` : "¿Cómo está tu clima interior?");
  h += item("habitos", av(ic("hoy"), "var(--ok)"), "Vida saludable", "", is.indice != null ? `Índice ${is.indice} % · racha de ${is.racha} ${is.racha === 1 ? "día" : "días"}` : "Sueño, actividad física, alimentación y más");
  h += item("clima", av(ic("grafica"), "var(--ok)"), "Tu clima del mes", "", "Lo que se repite en tus emociones");
  h += item("respirar", av(ic("bienestar"), "var(--ok)"), "Respirar un minuto", "", "Respiración en caja guiada");
  h += item("practica", av(ic("aprende"), "var(--ok)"), "Práctica de la semana", "", gn);
  return h + `</div>`;
};

/* ---------- Tu clima de hoy ---------- */
DET["bienestar.hoy"] = () => {
  const f = iso(hoyFecha()), b = bienDia(f), cl = b.clima ? CLIMAS[b.clima] : null;
  const fuerte = (b.clima === "tormenta" || b.clima === "niebla") && (b.int || 0) >= 5;
  return cab(av(ic("bienestar"), "var(--ok)"), "Tu clima de hoy", "Bienestar · inteligencia emocional", "") + cuerpo(
    hoja(`<h3 style="margin:0 0 4px">¿Cómo está tu clima interior?</h3><p class="suave" style="margin:0 0 12px">Elige según tu energía y si lo que sientes es agradable o no. No hay climas buenos ni malos: todos dicen algo.</p>
      <div class="bien-medidor">${Object.entries(CLIMAS).map(([k, c]) => `<button type="button" class="bien-clima${b.clima === k ? " si" : ""}" style="--c:${c.c}" data-acc="bienClima" data-arg="${k}" aria-pressed="${b.clima === k}">
        <span class="bc-ic">${window.climaSvg ? climaSvg(k) : ""}</span><b>${c.n}</b><small>${c.d}</small></button>`).join("")}</div>`) +
    (cl ? hoja(`<h3 style="margin:0 0 4px">Ponle nombre</h3><p class="suave" style="margin:0 0 10px">Nombrar con precisión lo que sientes es el primer paso para manejarlo.</p>
      <div class="bien-chips">${cl.e.map(e => `<button type="button" class="chip${b.emo === e ? " si" : ""}" style="--c:${cl.c}" data-acc="bienEmo" data-arg="${e}" aria-pressed="${b.emo === e}">${e}</button>`).join("")}</div>
      <div class="campo" style="margin-top:14px"><span>¿Qué tan fuerte? ${b.int ? `<b>${b.int} de 5</b>` : ""}</span><div class="bien-int" role="group" aria-label="Intensidad">${[1, 2, 3, 4, 5].map(n => `<button type="button" class="${(b.int || 0) >= n ? "si" : ""}" style="--c:${cl.c}" data-acc="bienInt" data-arg="${n}" aria-label="Intensidad ${n} de 5">${n}</button>`).join("")}</div></div>
      <div class="campo" style="margin-top:14px"><span>¿Qué lo provocó?</span><div class="bien-chips">${CAUSAS.map(c => `<button type="button" class="chip${(b.causas || []).includes(c) ? " si" : ""}" data-acc="bienCausa" data-arg="${c}" aria-pressed="${(b.causas || []).includes(c)}">${c}</button>`).join("")}</div></div>
      ${campo("Una nota (solo para ti)", `<textarea rows="3" maxlength="600" data-acc-change="bienNota" placeholder="Qué pasó, qué pensaste, qué necesitas…">${esc(b.nota || "")}</textarea>`)}`) : "") +
    (fuerte ? hoja(`<div class="bien-apoyo">${ic("comentario")}<div><b>No tienes que pasar esto solo</b><p>Si sientes que el malestar te supera, habla hoy con alguien de confianza o con un profesional. En Colombia la <b>Línea 106</b> atiende gratis las 24 horas; si hay peligro inmediato, llama al <b>123</b>. En otros países busca tu línea en <a href="https://findahelpline.com" target="_blank" rel="noopener">findahelpline.com</a>.</p></div></div>`, "bien-apoyo-hoja") : "") +
    (cl ? hoja(`<h3 style="margin:0 0 4px">${b.clima === "sol" || b.clima === "valle" ? "Guárdalo" : "Un minuto para ti"}</h3><p class="suave" style="margin:0 0 10px">${b.clima === "sol" || b.clima === "valle" ? "Los días buenos también se aprenden: ¿qué los hizo así? Escríbelo en tu nota." : "Elige una herramienta y márcala si te sirvió."}</p>
      <div class="bien-herr">${HERRAMIENTAS.map(([k, n, d]) => `<button type="button" class="bien-h${(b.herr || []).includes(k) ? " si" : ""}" data-acc="${k === "caja" ? "bienRespirar" : "bienHerr"}" data-arg="${k}" aria-pressed="${(b.herr || []).includes(k)}"><b>${n}</b><small>${d}</small></button>`).join("")}</div>`) : "") +
    `<p class="suave" style="font-size:12.5px">${ic("candado")} Tu clima interior es privado: no se comparte en la comunidad. SuRoute acompaña, no reemplaza a un profesional de la salud mental.</p>`, "max-width:860px");
};

/* ---------- Vida saludable ---------- */
DET["bienestar.habitos"] = () => {
  const f = iso(hoyFecha()), b = bienDia(f), is = indiceSaludable(), agua = aguaDe(f);
  const paso = (k, v, min, max, st, suf) => `<div class="bien-paso"><button type="button" class="rb" data-acc="bienValor" data-arg="${k}|${Math.max(min, (v || 0) - st)}" aria-label="Menos">−</button><b>${v ?? "—"}${v != null ? " " + suf : ""}</b><button type="button" class="rb" data-acc="bienValor" data-arg="${k}|${Math.min(max, (v || 0) + st)}" aria-label="Más">+</button></div>`;
  const nivelP = v => v == null ? "na" : v >= .99 ? "ok" : v >= .6 ? "medio" : "bajo";
  return cab(av(ic("hoy"), "var(--ok)"), "Vida saludable", "Bienestar · tus hábitos de hoy", "") + cuerpo(
    hoja(`<div class="bien-indice"><div class="bi-n"><b>${is.indice ?? "—"}</b><span>${is.indice != null ? "%" : ""}</span></div><div><h3 style="margin:0">Índice de vida saludable</h3>
      <p class="suave" style="margin:2px 0 0">Los últimos 7 días, con las metas de la OMS. Racha: <b>${is.racha} ${is.racha === 1 ? "día" : "días"}</b> con 4 de 6 hábitos cumplidos. Un día flojo no borra tu camino.</p></div></div>
      <div class="bien-pilares">${SALUD_PILARES.map(([k, n, meta]) => `<div class="bp e-${nivelP(is.pil[k])}"><span>${n}</span><div class="barra"><i style="width:${Math.round((is.pil[k] || 0) * 100)}%"></i></div><small>${k === "mov" ? `${is.movSem} de 150 min esta semana` : `Meta: ${meta}`}</small></div>`).join("")}</div>`) +
    hoja(`<h3 style="margin:0 0 12px">Hoy</h3><div class="bien-hoy">
      <div class="bh"><span>Sueño anoche</span>${paso("sueno", b.sueno, 0, 14, .5, "h")}<small>Meta: 7 a 9 horas</small></div>
      <div class="bh"><span>Actividad física</span>${paso("mov", b.mov, 0, 600, 10, "min")}<small>Caminar cuenta. Meta: 150 min a la semana</small></div>
      <div class="bh"><span>Frutas y verduras</span><div class="bien-porciones">${[1, 2, 3, 4, 5, 6].map(n => `<button type="button" class="${(b.fyv || 0) >= n ? "si" : ""}" data-acc="bienValor" data-arg="fyv|${(b.fyv || 0) === n ? n - 1 : n}" aria-label="${n} porciones">${n}</button>`).join("")}</div><small>Meta: 5 porciones (un puño cada una)</small></div>
      <div class="bh"><span>Aire libre y sol</span>${paso("aire", b.aire, 0, 600, 10, "min")}<small>Meta: 20 minutos</small></div>
      <div class="bh"><span>Sin pantallas antes de dormir</span><label class="bien-si"><input type="checkbox" ${b.pant ? "checked" : ""} data-acc-change="bienPant"> La última hora sin celular ni TV</label><small>Ayuda a dormir mejor</small></div>
      <div class="bh bh-agua"><span>Agua</span>${vasosDeAgua(f, agua)}</div></div>`) +
    `<p class="suave" style="font-size:12.5px">Las metas siguen las recomendaciones de la Organización Mundial de la Salud para adultos. Si tienes una condición de salud, sigue lo que te indique tu médico.</p>`, "max-width:860px");
};

/* ---------- Tu clima del mes ---------- */
DET["bienestar.clima"] = () => {
  const p = patronesClima(30), total = p.dias.length;
  const dias = diasAtras(30).reverse();
  return cab(av(ic("grafica"), "var(--ok)"), "Tu clima del mes", "Bienestar · los últimos 30 días", "") + cuerpo(
    hoja(`<h3 style="margin:0 0 10px">Cada día, su clima</h3><div class="bien-cal">${dias.map(f => { const b = bienDia(f), c = b.clima ? CLIMAS[b.clima] : null;
      return `<span class="bc${c ? "" : " vacio"}" style="${c ? `--c:${c.c}` : ""}" title="${Number(f.slice(8))} ${MES_C[Number(f.slice(5, 7)) - 1]}${c ? ` · ${c.n}${b.emo ? " · " + b.emo : ""}` : ""}"><i>${Number(f.slice(8))}</i></span>`; }).join("")}</div>
      <div class="bien-ley">${Object.entries(CLIMAS).map(([, c]) => `<span><i style="background:${c.c}"></i>${c.n}</span>`).join("")}</div>`) +
    (total ? hoja(`<h3 style="margin:0 0 10px">Lo que se repite</h3>
      <div class="bien-reparto">${Object.entries(CLIMAS).map(([k, c]) => `<div><span>${c.n}</span><div class="barra"><i style="width:${Math.round(p.cuenta[k] / total * 100)}%;background:${c.c}"></i></div><b>${p.cuenta[k]}</b></div>`).join("")}</div>
      ${p.emo.length ? `<p><b>Tus emociones más frecuentes:</b> ${p.emo.map(([e, n]) => `${e} (${n})`).join(", ")}.</p>` : ""}
      ${p.causas.length ? `<p><b>Lo que más te trae tormenta o niebla:</b> ${p.causas.map(([c, n]) => `${c.toLowerCase()} (${n})`).join(", ")}. Saberlo te permite prepararte.</p>` : ""}
      ${p.sueno ? `<p><b>El sueño cuenta:</b> los días que dormiste 7 horas o más, tu clima fue agradable el ${p.sueno[0]} %; con menos sueño, el ${p.sueno[1]} %.</p>` : ""}`)
      : hoja(`<p class="suave" style="margin:0">Marca tu clima unos días y aquí verás lo que se repite: qué te trae tormenta, qué te devuelve la calma y cómo influye tu sueño.</p>`)), "max-width:860px");
};

/* ---------- Respirar un minuto (respiración en caja guiada) ---------- */
DET["bienestar.respirar"] = () => cab(av(ic("bienestar"), "var(--ok)"), "Respirar un minuto", "Bienestar · respiración en caja", "") + cuerpo(
  hoja(`<div class="bien-resp"><div class="br-caja" id="brCaja"><span class="br-punto"></span><b id="brFase">Toca empezar</b></div>
    <p class="suave">Inhala 4 segundos, sostén 4, exhala 4 y sostén 4. Cuatro vueltas son un minuto.</p>
    <div class="botones" style="justify-content:center"><button class="btn lleno" data-acc="bienRespirarEmpezar" id="brBoton">Empezar</button></div></div>`), "max-width:640px");

/* ---------- Práctica de la semana ---------- */
DET["bienestar.practica"] = () => { const k = semanaIso(hoyFecha()) % 5, [n, d] = GOLEMAN[k];
  return cab(av(ic("aprende"), "var(--ok)"), "Práctica de la semana", "Bienestar · los cinco componentes de la inteligencia emocional", "") + cuerpo(
    hoja(`<div class="bien-practica"><small>Esta semana</small><h3>${n}</h3><p>${d}</p></div>`) +
    hoja(`<h3 style="margin:0 0 10px">Los cinco componentes (Daniel Goleman)</h3><ol class="bien-goleman">${GOLEMAN.map(([gn, gd], i) => `<li class="${i === k ? "actual" : ""}"><b>${gn}</b><span>${gd}</span></li>`).join("")}</ol>
      <p class="suave" style="margin:10px 0 0">Cada semana cambia la práctica. Repetirla en días buenos y malos es lo que la vuelve hábito.</p>`), "max-width:860px"); };

let respTimer = null;
/** La fecha del bienestar que se edita: la del día abierto (tarjeta del día) o hoy. */
const fBien = el => el?.closest?.("[data-bien-f]")?.dataset.bienF || iso(hoyFecha());
const repintarBien = () => { pintarDetalle(); pintarLista(); };
Object.assign(ACC, {
  bienClima(el, k) { guardarBien(fBien(el), d => { if (d.clima !== k) { d.clima = k; delete d.emo; d.int = d.int || 3; } else if (el?.closest?.("[data-bien-f]")) { delete d.clima; delete d.emo; delete d.int; } d.t = Date.now(); }); repintarBien(); },
  bienEmo(el, e) { guardarBien(fBien(el), d => { d.emo = d.emo === e ? undefined : e; }); repintarBien(); },
  bienInt(el, n) { guardarBien(fBien(el), d => { d.int = Number(n); }); pintarDetalle(); },
  bienCausa(el, c) { guardarBien(fBien(el), d => { const l = new Set(d.causas || []); l.has(c) ? l.delete(c) : l.add(c); d.causas = [...l]; }); pintarDetalle(); },
  bienNota(el) { guardarBien(fBien(el), d => { d.nota = el.value.trim().slice(0, 600); }); },
  bienHerr(el, k) { guardarBien(fBien(el), d => { const l = new Set(d.herr || []); l.has(k) ? l.delete(k) : l.add(k); d.herr = [...l]; }); pintarDetalle(); },
  bienRespirar(el) { guardarBien(fBien(el), d => { const l = new Set(d.herr || []); l.add("caja"); d.herr = [...l]; }); if (estado.sec !== "bienestar") { ir("bienestar", "respirar"); } else elegir("respirar"); setTimeout(() => ACC.bienRespirarEmpezar(), 300); },
  bienValor(el, arg) { const [k, v] = arg.split("|"); guardarBien(fBien(el), d => { d[k] = Math.max(0, Number(v)); }); repintarBien(); },
  bienPant(el) { guardarBien(fBien(el), d => { d.pant = el.checked; }); repintarBien(); },
  bienRespirarEmpezar() {
    const caja = $("brCaja"), fase = $("brFase"), boton = $("brBoton"); if (!caja) return;
    clearInterval(respTimer); const fases = ["Inhala", "Sostén", "Exhala", "Sostén"]; let t = 0;
    const reducido = matchMedia("(prefers-reduced-motion: reduce)").matches;
    caja.classList.add("activa"); if (boton) boton.textContent = "Volver a empezar";
    const paso = () => {
      if (!document.body.contains(caja)) { clearInterval(respTimer); return; }
      if (t >= 64) { clearInterval(respTimer); fase.textContent = "Listo. ¿Cómo está tu clima ahora?"; caja.classList.remove("activa"); return; }
      const f = Math.floor(t / 4) % 4; fase.textContent = `${fases[f]} · ${4 - (t % 4)}`; caja.dataset.fase = f; if (!reducido) caja.style.setProperty("--lado", f); t++;
    };
    paso(); respTimer = setInterval(paso, 1000);
  },
});
