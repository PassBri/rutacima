/*
 * Datos del cielo real de Ruta a la Cima (una sola fuente para la app y la web).
 *
 *   cd tools/cielo && npm install && npm run generar
 *
 * Escribe:
 *   web/js/09b-cielo-datos.js            (const CIELO_DATOS = {...})
 *   app/src/main/assets/cielo/datos.json (el mismo objeto)
 *
 * Fuentes:
 *   - Estrellas (magnitud ≤ 4) y líneas y nombres de constelaciones: d3-celestial de Olaf Frohn,
 *     licencia BSD-3-Clause (Copyright (c) 2015, Olaf Frohn). Catálogo Hipparcos.
 *   - Eclipses, conjunciones, superlunas, equinoccios y solsticios: astronomy-engine de Don Cross,
 *     licencia MIT, validado contra los datos de JPL y del catálogo de eclipses de la NASA (Espenak).
 *     Comprobado a mano: los eclipses penumbrales de luna del 20 de febrero y del 17 de agosto de 2027
 *     coinciden con science.nasa.gov/eclipses/future-eclipses.
 *   - Lluvias de meteoros: calendario de la Organización Internacional de Meteoros (IMO): radiante,
 *     tasa horaria cenital (THZ), pico y periodo de actividad. Las fechas se repiten cada año.
 *
 * Observador: Santander, Colombia (7,07° N; 73,1° O). Solo se guardan eventos visibles desde ahí.
 * Regla de oro: nunca inventar un evento. Si el cálculo dice que no se ve, no se guarda.
 */
"use strict";
const fs = require("fs");
const path = require("path");
const A = require("astronomy-engine");
const D = p => require(path.join(__dirname, "node_modules/d3-celestial/data", p));

const OBS = new A.Observer(7.07, -73.1, 1000);
const DESDE = new Date("2026-10-01T00:00:00Z"), HASTA = new Date("2036-12-31T23:59:59Z");
const raiz = path.join(__dirname, "..", "..");
const r1 = x => Math.round(x * 10) / 10, r2 = x => Math.round(x * 100) / 100, r3 = x => Math.round(x * 1000) / 1000;
const iso = t => (t.date || t).toISOString().replace(/\.\d+Z$/, "Z");

/* ---------- Estrellas y constelaciones ---------- */
const estrellas = D("stars.6.json").features.filter(f => f.properties.mag <= 4)
  .map(f => [r2(f.geometry.coordinates[0]), r2(f.geometry.coordinates[1]), r1(f.properties.mag)]);

const lineas = D("constellations.lines.json").features.filter(f => Number(f.properties.rank) <= 2)
  .map(f => ({ id: f.id, r: Number(f.properties.rank), l: f.geometry.coordinates.map(seg => seg.map(([ra, dec]) => [r2(ra), r2(dec)])) }));

// Nombres de las constelaciones principales y de los radiantes de las lluvias, en los 12 idiomas de la app.
// d3-celestial no trae portugués; se agrega aquí. En español se usan los nombres comunes del zodiaco.
const EXTRA = ["Cru", "Lyr", "Dra", "UMi", "Aqr"];
const PT = { And: "Andrômeda", Aql: "Águia", Ari: "Áries", Aur: "Cocheiro", Boo: "Boieiro", CMa: "Cão Maior", Car: "Quilha", Cas: "Cassiopeia", Cen: "Centauro",
  Cet: "Baleia", Cyg: "Cisne", Eri: "Erídano", Gem: "Gêmeos", Leo: "Leão", Ori: "Órion", Peg: "Pégaso", Per: "Perseu", Sgr: "Sagitário", Sco: "Escorpião",
  Tau: "Touro", UMa: "Ursa Maior", Vir: "Virgem", Cru: "Cruzeiro do Sul", Lyr: "Lira", Dra: "Dragão", UMi: "Ursa Menor", Aqr: "Aquário" };
const ES = { Ari: "Aries", Gem: "Géminis", Tau: "Tauro", Vir: "Virgo", Aur: "Auriga", Cru: "Cruz del Sur", Sco: "Escorpión" };
const nombres = {};
for (const f of D("constellations.json").features) {
  const p = f.properties;
  if (p.rank !== "1" && !EXTRA.includes(f.id)) continue;
  nombres[f.id] = { pos: f.geometry.coordinates, es: ES[f.id] || p.es, en: p.en, pt: PT[f.id], fr: p.fr, de: p.de, it: p.it, zh: p.zh, ja: p.ja, ko: p.ko, ar: p.ar, hi: p.hi, ru: p.ru };
  for (const [k, v] of Object.entries(nombres[f.id])) if (!v) throw new Error(`Falta el nombre ${k} de ${f.id}`);
}

/* ---------- Lluvias de meteoros (IMO) ---------- */
const lluvias = [
  { id: "QUA", constelacion: "Boo", ra: 230, dec: 49, thz: 110, pico: "01-03", desde: "12-28", hasta: "01-12", ancho: 0.6 },
  { id: "LYR", constelacion: "Lyr", ra: 271, dec: 34, thz: 18, pico: "04-22", desde: "04-14", hasta: "04-30", ancho: 1.5 },
  { id: "ETA", constelacion: "Aqr", ra: 338, dec: -1, thz: 50, pico: "05-06", desde: "04-19", hasta: "05-28", ancho: 4 },
  { id: "SDA", constelacion: "Aqr", ra: 340, dec: -16, thz: 25, pico: "07-30", desde: "07-12", hasta: "08-23", ancho: 4 },
  { id: "PER", constelacion: "Per", ra: 48, dec: 58, thz: 100, pico: "08-12", desde: "07-17", hasta: "08-24", ancho: 2.5 },
  { id: "DRA", constelacion: "Dra", ra: 262, dec: 54, thz: 10, pico: "10-08", desde: "10-06", hasta: "10-10", ancho: 1 },
  { id: "ORI", constelacion: "Ori", ra: 95, dec: 16, thz: 20, pico: "10-21", desde: "10-02", hasta: "11-07", ancho: 4 },
  { id: "LEO", constelacion: "Leo", ra: 152, dec: 22, thz: 15, pico: "11-17", desde: "11-06", hasta: "11-30", ancho: 2 },
  { id: "GEM", constelacion: "Gem", ra: 112, dec: 33, thz: 150, pico: "12-14", desde: "12-04", hasta: "12-20", ancho: 1.5 },
  { id: "URS", constelacion: "UMi", ra: 217, dec: 76, thz: 10, pico: "12-22", desde: "12-17", hasta: "12-26", ancho: 1 },
];

/* ---------- Eventos ---------- */
const eventos = [];
const altura = (cuerpo, t) => {
  const eq = A.Equator(cuerpo, t, OBS, true, true);
  return A.Horizon(t, OBS, eq.ra, eq.dec, "normal").altitude;
};
const enRango = t => t.date >= DESDE && t.date <= HASTA;

// Eclipses de luna: visibles si la luna está sobre el horizonte en algún momento de la fase principal
for (let t = A.MakeTime(DESDE); ;) {
  const e = A.SearchLunarEclipse(t);
  if (e.peak.date > HASTA) break;
  const sd = e.sd_partial > 0 ? e.sd_partial : e.sd_penum;
  let visible = false;
  for (let m = -sd; m <= sd; m += 5) if (altura(A.Body.Moon, e.peak.AddDays(m / 1440)) > 0) { visible = true; break; }
  if (visible) eventos.push({
    tipo: "eclipse_luna", clase: e.kind, max: iso(e.peak), oscurecimiento: r3(e.obscuration),
    inicio: iso(e.peak.AddDays(-e.sd_penum / 1440)), fin: iso(e.peak.AddDays(e.sd_penum / 1440)),
    ...(e.sd_partial > 0 ? { parcialInicio: iso(e.peak.AddDays(-e.sd_partial / 1440)), parcialFin: iso(e.peak.AddDays(e.sd_partial / 1440)) } : {}),
    ...(e.sd_total > 0 ? { totalInicio: iso(e.peak.AddDays(-e.sd_total / 1440)), totalFin: iso(e.peak.AddDays(e.sd_total / 1440)) } : {}),
  });
  t = e.peak.AddDays(10);
}

// Eclipses de sol vistos desde Santander: solo si el sol está sobre el horizonte en el máximo
for (let t = A.MakeTime(DESDE); ;) {
  const e = A.SearchLocalSolarEclipse(t, OBS);
  if (e.peak.time.date > HASTA) break;
  if (e.peak.altitude > 0 && e.obscuration > 0.01) eventos.push({
    tipo: "eclipse_sol", clase: e.kind, max: iso(e.peak.time), oscurecimiento: r3(e.obscuration),
    inicio: iso(e.partial_begin.time), fin: iso(e.partial_end.time),
  });
  t = e.peak.time.AddDays(10);
}

// Conjunciones: dos astros a menos de 2° (luna con un planeta, o dos planetas), lejos del sol y de noche
const CUERPOS = { luna: A.Body.Moon, venus: A.Body.Venus, marte: A.Body.Mars, jupiter: A.Body.Jupiter, saturno: A.Body.Saturn };
const pares = [["luna", "venus"], ["luna", "marte"], ["luna", "jupiter"], ["luna", "saturno"], ["venus", "marte"], ["venus", "jupiter"], ["venus", "saturno"], ["marte", "jupiter"], ["marte", "saturno"], ["jupiter", "saturno"]];
const sep = (a, b, t) => A.AngleBetween(A.GeoVector(CUERPOS[a], t, true), A.GeoVector(CUERPOS[b], t, true));
for (const [a, b] of pares) {
  const paso = a === "luna" ? 1 / 24 : 0.5;
  let prev = null, bajando = false;
  for (let t = A.MakeTime(DESDE); t.date <= HASTA; t = t.AddDays(paso)) {
    const s = sep(a, b, t);
    if (prev !== null) {
      if (s > prev && bajando && prev < 2) {
        const tm = t.AddDays(-paso);
        const elong = Math.min(A.Elongation(CUERPOS[b], tm).elongation, a === "luna" ? 180 : A.Elongation(CUERPOS[a], tm).elongation);
        // Visible: lejos del resplandor del sol y ambos sobre el horizonte en algún momento de esa noche
        let deNoche = false;
        for (let h = -12; h <= 12; h += 0.5) {
          const th = tm.AddDays(h / 24);
          if (altura(A.Body.Sun, th) < -8 && altura(CUERPOS[a], th) > 10 && altura(CUERPOS[b], th) > 10) { deNoche = true; break; }
        }
        if (elong > 15 && deNoche) eventos.push({ tipo: "conjuncion", cuerpos: [a, b], max: iso(tm), sep: r1(prev) });
      }
      bajando = s < prev;
    }
    prev = s;
  }
}

// Superlunas: luna llena con la luna a menos de 361.000 km
for (let t = A.MakeTime(DESDE); ;) {
  const f = A.SearchMoonPhase(180, t, 40);
  if (!f || f.date > HASTA) break;
  const km = A.GeoMoon(f).Length() * A.KM_PER_AU;
  if (km < 361000) eventos.push({ tipo: "superluna", max: iso(f), km: Math.round(km) });
  t = f.AddDays(20);
}

// Equinoccios y solsticios
for (let y = DESDE.getUTCFullYear(); y <= HASTA.getUTCFullYear(); y++) {
  const s = A.Seasons(y);
  for (const [clase, t] of [["equinoccio_marzo", s.mar_equinox], ["solsticio_junio", s.jun_solstice], ["equinoccio_septiembre", s.sep_equinox], ["solsticio_diciembre", s.dec_solstice]])
    if (enRango(t)) eventos.push({ tipo: "estacion", clase, max: iso(t) });
}

eventos.sort((x, y) => x.max.localeCompare(y.max));
const datos = {
  fuente: "Estrellas y constelaciones: d3-celestial (Olaf Frohn, BSD-3-Clause). Eventos: astronomy-engine (Don Cross, MIT). Lluvias: IMO.",
  observador: { lat: 7.07, lng: -73.1 }, desde: iso(DESDE), hasta: iso(HASTA),
  estrellas, lineas, nombres, lluvias, eventos,
};
const json = JSON.stringify(datos);
fs.writeFileSync(path.join(raiz, "web/js/09b-cielo-datos.js"),
  `"use strict";\n/* Generado por tools/cielo/generar.js: no editar a mano. ${datos.fuente} */\nconst CIELO_DATOS = ${json};\n`);
fs.mkdirSync(path.join(raiz, "app/src/main/assets/cielo"), { recursive: true });
fs.writeFileSync(path.join(raiz, "app/src/main/assets/cielo/datos.json"), json);
console.log(`${estrellas.length} estrellas, ${lineas.length} constelaciones, ${Object.keys(nombres).length} nombres, ${eventos.length} eventos (${(json.length / 1024).toFixed(0)} KB)`);
for (const e of eventos.filter(e => e.tipo !== "estacion" && e.tipo !== "superluna").slice(0, 60)) console.log(e.tipo, e.clase || e.cuerpos.join("+"), e.max, e.oscurecimiento ?? e.sep ?? "");
