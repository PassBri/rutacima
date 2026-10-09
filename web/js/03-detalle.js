"use strict";
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
/** Horizonte en años: se escribe el número, se ajusta con − y + o se toca un atajo. */
function selectorHorizonte(tipo, clave, anios) {
  const arg = `${tipo}|${esc(clave)}`;
  return `<div class="campo horizonte" role="group" aria-labelledby="horiz-${esc(clave)}"><span id="horiz-${esc(clave)}">Horizonte</span>
    <div class="pasos-num">
      <button type="button" class="btn mini" data-acc="horizonte" data-arg="${arg}|-1" aria-label="Un año menos" ${anios <= HORIZONTE_MIN ? "disabled" : ""}>−</button>
      <input type="number" inputmode="numeric" min="${HORIZONTE_MIN}" max="${HORIZONTE_MAX}" step="1" value="${anios}" aria-label="Años del propósito" ${bind(tipo, clave, "horizonte", "horiz")}>
      <button type="button" class="btn mini" data-acc="horizonte" data-arg="${arg}|1" aria-label="Un año más" ${anios >= HORIZONTE_MAX ? "disabled" : ""}>+</button>
      <b>años</b>
    </div>
    <div class="atajos">${HORIZONTES.map(h => `<button type="button" class="chip" aria-pressed="${h === anios}" data-acc="horizonte" data-arg="${arg}|=${h}">${h}</button>`).join("")}</div>
  </div>`;
}
/** Ocho vasos que se llenan con una ola al tocarlos (250 ml cada uno); más y menos para no apuntar al vaso. */
function vasosDeAgua(fecha, n) {
  const vaso = i => `<button class="vaso${i <= n ? " lleno" : ""}" data-acc="agua" data-arg="${fecha}|${i === n ? i - 1 : i}" aria-label="Vaso ${i}" aria-pressed="${i <= n}">
    <svg viewBox="0 0 36 50" aria-hidden="true"><defs><clipPath id="v-${fecha}-${i}"><path d="M2 3h32l-4 44H6z"/></clipPath></defs>
      <path d="M2 3h32l-4 44H6z" class="vaso-fondo"/><g clip-path="url(#v-${fecha}-${i})"><g class="vaso-agua"><path class="ola" d="M-36 8 q9 -5 18 0 t18 0 t18 0 t18 0 t18 0 V60 H-36z"/></g></g>
      <path d="M2 3h32l-4 44H6z" class="vaso-borde"/></svg></button>`;
  const litros = (n * 0.25).toLocaleString("es", { maximumFractionDigits: 2 });
  return `<div class="vasos" role="group" aria-label="Vasos de agua">${[1, 2, 3, 4, 5, 6, 7, 8].map(vaso).join("")}</div>
    <div class="vasos-pie"><b>${n} de 8 vasos · ${litros} L</b>${n >= 8 ? `<span>¡Meta de agua cumplida!</span>` : ""}
      <button class="btn mini" data-acc="agua" data-arg="${fecha}|${n - 1}" ${n <= 0 ? "disabled" : ""} aria-label="Un vaso menos">−</button>
      <button class="btn mini" data-acc="agua" data-arg="${fecha}|${n + 1}" ${n >= 8 ? "disabled" : ""} aria-label="Un vaso más">+</button></div>`;
}
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
    // Cada fila es una década: a la izquierda, el año en que empieza; el punto dorado es este año
    let puntos = "";
    for (let i = 0; i < metaVida(); i++) {
      const a = nac + i, cls = a < anioHoy ? "vivido" : a === anioHoy ? "actual" : "futuro";
      if (i % 10 === 0) puntos += `<span class="pv-fila${anioHoy >= a && anioHoy < a + 10 ? " esta" : ""}" aria-hidden="true">${a}</span>`;
      puntos += `<button class="pv ${cls}" data-sel="a:${a}" aria-label="${a}, ${i} años" title="${a} · ${i} años">${conMetas.has(a) ? ic("bandera", "bandera") : ""}</button>`;
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
        <p class="pv-hoy"><i></i>${anioHoy} · ${r.edad} años</p>
        <p class="suave" style="font-size:13px;margin:4px 0 0">Cada fila es una década: a la izquierda, el año en que empieza.</p>
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
      hoja(`<h3>Propósito a largo plazo</h3><p class="suave">Tu cumbre a los años que tú elijas. De aquí salen las metas de cada año.</p><button class="btn lleno" data-acc="nuevoProposito">Crear propósito</button>`) +
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
        ${selectorHorizonte("proposito", k, p.horizonte || 5)}
        <div class="form-2">          ${campo("Eje principal", sel("proposito", k, "eje", p.eje || "", opcionesEje, "nulo"))}
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
        <div class="campo"><span>Vasos de agua</span>${vasosDeAgua(fecha, ag.agua || 0)}</div></div>
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

/** Lo que reportaste o a quien bloqueaste deja de verse aquí al instante (el servidor también lo filtra). */
const listaLocal = k => { try { return new Set(JSON.parse(localStorage.getItem("rutacima-" + k) || "[]")); } catch { return new Set(); } };
function ocultarLocal(k, v) { const s = listaLocal(k); s.add(String(v)); try { localStorage.setItem("rutacima-" + k, JSON.stringify([...s])); } catch {} }
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
  finally {
    const ocultos = listaLocal("ocultos"), bloqueados = listaLocal("bloqueados");
    if (estado.feed) estado.feed = estado.feed.filter(p => !ocultos.has(String(p.id)) && !bloqueados.has(String(p.autorId)));
    cargarFeed.activo = false; if (estado.sec === "comunidad" || estado.sec === "perfil") { pintarLista(); if (!enEdicion()) pintarDetalle(); } }
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
        : `<span style="display:flex;gap:6px;flex-wrap:wrap;justify-content:flex-end">${!p.demo || !Store.nube ? `<button class="btn mini" data-acc="escribirA" data-arg="${esc(p.autorId)}|${esc(p.autorNombre)}">${ic("mensajes")} Mensaje</button>` : ""}${!p.demo && Store.nube ? `<button class="btn mini" data-acc="seguir" data-arg="${esc(p.autorId)}">Seguir</button>` : ""}<button class="btn mini" data-acc="reportarPost" data-arg="${esc(p.id)}" title="Reportar publicación">Reportar</button><button class="btn mini peligro" data-acc="bloquearAutor" data-arg="${esc(p.id)}" title="Bloquear a esta persona">Bloquear</button></span>`) +
      `<div class="det-cuerpo"><div class="ancho" style="gap:16px;max-width:640px">
        ${p.demo ? `<div class="aviso">Comunidad de ejemplo. ${Store.nube ? "" : "Vincula la web con tu cuenta para ver la comunidad real."}</div>` : ""}
        ${postal(p)}${p.foto ? `<p style="margin:0">${esc(p.texto)}</p>` : ""}
        <div class="acciones"><button class="btn ${p.yoImpulse ? "lleno" : "oro"}" data-acc="impulsar" aria-pressed="${p.yoImpulse}">${ic("impulso")} ${p.yoImpulse ? "Impulsado" : "Impulsar"} · ${p.impulsos}</button>
          <span class="suave">${(p.comentariosLista || []).length || p.comentarios} comentarios</span></div>
        <div class="chat" style="max-width:none">${(p.comentariosLista || []).map(c => `<div class="burbuja el"><b style="color:var(--oro);font-size:13px">${esc(c.autor)}</b><br>${esc(c.texto)}</div>`).join("")}</div>
      </div></div>
      <form class="escribir" data-form="comentar"><input name="texto" placeholder="Escribe un comentario" aria-label="Comentario" autocomplete="off"><button class="enviar" aria-label="Enviar">${ic("enviar")}</button></form>`;
  },
  /** Una publicación de la lista abre Cimas desde ella (en el teléfono y en el computador). */
  "comunidad.cima"(id) {
    estado.vistaCom = "cimas";
    requestAnimationFrame(() => document.querySelector(`.cima[data-cima="${CSS.escape(id)}"]`)?.scrollIntoView({ block: "start" }));
    return DET["comunidad.feed"]();
  },
  "comunidad.feed"() {
    if (!estado.feed) { cargarFeed(); return cab(av(ic("comunidad"), "var(--burdeos)"), "Comunidad", "Cargando…") + cuerpo(""); }
    const ps = estado.feed;
    const autor = p => { const ini = (p.autorNombre || "?").split(" ").map(x => x[0]).join("").slice(0, 2).toUpperCase(); return av(esc(ini), COLOR_EJE[p.eje] || "var(--burdeos)"); };
    const vacioFeed = `<p class="vacio-mini">${estado.feedFiltro === "SIGUIENDO" ? "Todavía no sigues a nadie." : "Aún no hay publicaciones."}</p>`;
    if (estado.vistaCom === "cimas") {
      // Cimas es la vista inmersiva del mismo muro: se entra tocando una publicación y se vuelve con este botón
      return cab(av(ic("ruta"), "var(--burdeos)"), "Cimas", "Desliza para ver la siguiente",
        `<button class="btn mini" data-vista="lista">${ic("comunidad")} Volver al muro</button>`) +
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
        <button data-acc="verCima" data-arg="${esc(p.id)}" aria-label="Ver en Cimas, a pantalla completa" style="display:block;width:100%;text-align:left">${postal(p)}</button>
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
        if (m.rol === "user") return sep + `<div class="burbuja yo">${esc(m.texto)}<span class="h">${horaDe(m.creadoEn)}</span></div>`;
        const [texto, accion] = separarAccion(m.texto), creada = listaLocal("coach-creadas").has(String(m.id));
        return sep + `<div class="burbuja el">${esc(texto)}<span class="h">${horaDe(m.creadoEn)}</span></div>` + (accion ? `<div class="accion-coach">
          <div class="etiqueta">${accion.tipo === "meta_mes" ? "Meta del mes propuesta" : "Meta del año propuesta"}</div><p>${esc(accion.texto)}</p>
          ${creada ? `<span class="suave">✓ Meta creada</span>` : `<button class="btn mini lleno" data-acc="crearMetaCoach" data-arg="${esc(String(m.id))}">Crear esta meta</button>`}</div>` : ""); }).join("")}
      ${coachEscribiendo ? `<div class="burbuja el escribiendo">escribiendo…</div>` : ""}</div>
      ${ms.length < 2 ? `<div class="sugerencias">${sugs.map(s => `<button data-pregunta="${esc(s)}">${esc(s)}</button>`).join("")}</div>` : ""}</div>
    <form class="escribir" data-form="coach"><input name="texto" placeholder="Escribe un mensaje" aria-label="Mensaje" autocomplete="off"><button class="enviar" aria-label="Enviar">${ic("enviar")}</button></form>`;
};

/** La meta que propone el coach al final de su respuesta (igual que AccionCoach.kt). */
function separarAccion(t) {
  const m = /\[\[ACCION([\s\S]*?)]]/.exec(t || "");
  if (!m) return [String(t || "").trim(), null];
  const limpio = t.replace(/\[\[ACCION[\s\S]*?]]/g, "").trim();
  try {
    const o = JSON.parse(m[1].slice(m[1].indexOf("{"), m[1].lastIndexOf("}") + 1));
    if (o.tipo !== "meta_mes" && o.tipo !== "meta_anio") return [limpio, null];
    const texto = String(o.texto || o.titulo || "").trim().slice(0, 300); if (!texto) return [limpio, null];
    const eje = String(o.eje || "").toUpperCase();
    return [limpio, { tipo: o.tipo, texto, eje: COLOR_EJE[eje] ? eje : null, dias: Math.min(31, Math.max(1, Number(o.dias) || 20)) }];
  } catch { return [limpio, null]; }
}
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
          + `<h3 style="margin-top:28px">Eliminar mi cuenta</h3><p class="suave">Se borran para siempre tu perfil, publicaciones, comentarios, mensajes, tu ruta en la nube y los computadores vinculados. Lo guardado en tu teléfono se queda ahí.</p>
           <div class="botones"><button class="btn peligro" data-acc="eliminarCuenta">Eliminar mi cuenta</button></div>`
        : `<h3>Estás en la demostración</h3><p>Es una ruta de ejemplo y los cambios se quedan en esta pestaña. Para usar tu ruta real, abre Rutaalacima Web con el servidor configurado y vincúlala desde la app: Perfil › Rutaalacima Web › Escanear código.</p>
           <div class="botones"><button class="btn" data-acc="salir">Volver a la pantalla de inicio</button></div>`) +
        `<p class="suave" style="text-align:center"><a href="privacidad.html" target="_blank" rel="noopener">Política de privacidad</a></p>`, "max-width:640px");
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
