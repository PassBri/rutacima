/* =====================================================================================
 * Recuerdos del año: tus fotos de ese año pasan como una historia (reel) cuadrada,
 * con el mes en un sello de cera y tus palabras; al final, el resumen del año y
 * un mosaico listo para publicar en la comunidad o descargar.
 * ===================================================================================== */

/** Las fotos propias de un año, de la más antigua a la más reciente. */
function fotosDelAnio(a) {
  return (estado.misPosts || []).filter(p => p.foto && (p.anio ? Number(p.anio) === a : new Date(p.creadoEn).getFullYear() === a))
    .sort((x, y) => (x.creadoEn || 0) - (y.creadoEn || 0));
}
/** Botón para ver la historia del año; va dentro del Álbum del año (js/03-detalle.js albumAnio). */
const botonHistoriaAnio = a => fotosDelAnio(a).length ? `<button class="btn lleno mini" data-acc="reelAnio" data-arg="${a}">${ic("play")} Ver mis recuerdos</button>` : "";
window.botonHistoriaAnio = botonHistoriaAnio;

const MES_SELLO = ["ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"];
ACC.reelAnio = (el, arg) => {
  const a = Number(arg), fs = fotosDelAnio(a); if (!fs.length) return;
  const p = perfil(), c = cascada(), metas = c.anuales.filter(m => m.anio === a);
  const lograda = metas.filter(m => m.estado === "COMPLETADA" || c.nodoAnio(m) >= 100).length;
  const meses = new Set(fs.map(x => new Date(x.creadoEn).getMonth())).size;
  const quieto = matchMedia("(prefers-reduced-motion: reduce)").matches, DUR = 1700;   // flashback: rápido, pero se alcanza a ver
  const slides = [...fs.map((x, i) => {
    const f = new Date(x.creadoEn), frase = String(x.metaTitulo || x.texto || "").trim().slice(0, 140);
    return `<figure class="reel-slide" data-i="${i}"><img src="${esc(x.foto)}" alt="${altFoto(x)}" style="--o:${["50% 40%", "40% 55%", "60% 45%"][i % 3]}">
      <figcaption><span class="reel-sello" aria-label="${MESES[f.getMonth()]}">${MES_SELLO[f.getMonth()]}</span>${frase ? `<p>«${esc(frase)}»</p>` : ""}<small>${f.getDate()} de ${MESES[f.getMonth()]}</small></figcaption></figure>`;
  }), `<figure class="reel-slide reel-fin" data-i="${fs.length}">
      <div class="reel-mosaico">${fs.slice(0, 9).map(x => `<img src="${esc(x.foto)}" alt="">`).join("")}</div>
      <figcaption><small>Mi ${a} en SuRoute</small><h2>${esc(p.cumbreFrase || "Un año más cerca de mi cumbre")}</h2>
        <div class="reel-cifras"><span><b>${fs.length}</b>recuerdos</span><span><b>${meses}</b>${meses === 1 ? "mes" : "meses"}</span><span><b>${lograda}/${metas.length}</b>metas</span></div>
        <div class="reel-botones"><button class="btn lleno" data-reel="publicar">${ic("publicar")} Publicar en la comunidad</button>
          <button class="btn" data-reel="descargar">${ic("descargar")} Descargar</button><button class="btn" data-reel="repetir">${ic("repetir")} Repetir</button></div>
        <div class="reel-publicar" hidden><span>¿Quién lo ve?</span><button class="btn mini" data-reel="SEGUIDORES">Mis seguidores</button><button class="btn mini lleno" data-reel="PUBLICA">Todos</button></div>
      </figcaption></figure>`];
  const v = document.createElement("div");
  v.className = "reel"; v.setAttribute("role", "dialog"); v.setAttribute("aria-modal", "true"); v.setAttribute("aria-label", `Recuerdos de ${a}`);
  v.innerHTML = `<div class="reel-escena">
      <div class="reel-barras">${slides.map(() => `<i><b></b></i>`).join("")}</div>
      <div class="reel-cab"><span>${a}</span><span class="reel-ctrl"><button class="reel-x reel-pausa" data-reel="pausa" aria-label="Detener (espacio)" title="Detener (espacio)">${ic("pausa")}</button>
        <button class="reel-x" data-reel="cerrar" aria-label="Cerrar (Esc)">${ic("cerrar")}</button></span></div>
      <i class="reel-flash" aria-hidden="true"></i><i class="reel-grano" aria-hidden="true"></i>
      ${slides.join("")}
      <button class="reel-zona izq" data-reel="atras" aria-label="Anterior"></button><button class="reel-zona der" data-reel="siguiente" aria-label="Siguiente"></button>
    </div>`;
  document.body.appendChild(v); document.documentElement.classList.add("visor-abierto");
  const ss = [...v.querySelectorAll(".reel-slide")], bs = [...v.querySelectorAll(".reel-barras b")];
  let i = 0, t0 = 0, usado = 0, pausa = false, raf = 0;
  const mostrar = n => {
    i = Math.max(0, Math.min(ss.length - 1, n)); usado = 0; t0 = performance.now();
    ss.forEach((s, k) => s.classList.toggle("activa", k === i));
    if (!quieto) { const f = v.querySelector(".reel-flash"); f.classList.remove("on"); void f.offsetWidth; f.classList.add("on"); }
    bs.forEach((b, k) => b.style.transform = `scaleX(${k < i ? 1 : 0})`);
    v.querySelector(".reel-zona.izq").hidden = v.querySelector(".reel-zona.der").hidden = i === ss.length - 1;
  };
  const paso = ahora => {
    if (!pausa && !quieto && i < ss.length - 1) {
      const f = Math.min(1, (usado + ahora - t0) / DUR); bs[i].style.transform = `scaleX(${f})`;
      if (f >= 1) mostrar(i + 1);
    } else t0 = ahora - 0;
    raf = requestAnimationFrame(paso);
  };
  const pausar = si => { if (si === pausa) return; if (si) usado += performance.now() - t0; else t0 = performance.now(); pausa = si; v.classList.toggle("en-pausa", si);
    const bp = v.querySelector(".reel-pausa"); bp.innerHTML = ic(si ? "play" : "pausa"); bp.setAttribute("aria-label", si ? "Seguir (espacio)" : "Detener (espacio)"); };
  const cerrar = () => { cancelAnimationFrame(raf); removeEventListener("keydown", teclas); v.remove(); document.documentElement.classList.remove("visor-abierto"); el?.focus?.(); };
  const teclas = e => {
    if (e.key === "Escape") cerrar(); else if (e.key === "ArrowRight") mostrar(i + 1); else if (e.key === "ArrowLeft") mostrar(i - 1);
    else if (e.key === " ") { e.preventDefault(); pausar(!pausa); }
  };
  addEventListener("keydown", teclas);
  // Dejar presionado pausa, como en las historias
  let presion = 0;
  v.addEventListener("pointerdown", e => { if (e.target.closest(".reel-zona")) presion = setTimeout(() => pausar(true), 220); });
  v.addEventListener("pointerup", () => { clearTimeout(presion); if (pausa) setTimeout(() => pausar(false), 0); });
  v.addEventListener("click", async e => {
    const b = e.target.closest("[data-reel]"); if (!b) return;
    const q = b.dataset.reel;
    if (q === "cerrar") return cerrar();
    if (q === "pausa") return pausar(!pausa);
    if (q === "siguiente") return mostrar(i + 1);
    if (q === "atras") return mostrar(i - 1);
    if (q === "repetir") return mostrar(0);
    if (q === "publicar") { v.querySelector(".reel-publicar").hidden = false; return; }
    if (q === "descargar" || q === "PUBLICA" || q === "SEGUIDORES") {
      b.disabled = true;
      try {
        const blob = await mosaicoAnio(a, fs, p);
        if (q === "descargar") {
          const u = URL.createObjectURL(blob), l = Object.assign(document.createElement("a"), { href: u, download: `suroute-recuerdos-${a}.jpg` });
          document.body.appendChild(l); l.click(); l.remove(); setTimeout(() => URL.revokeObjectURL(u), 4000);
        } else {
          await publicarImagenVision(blob, q, `Mis recuerdos de ${a}${p.cumbreFrase ? " · " + p.cumbreFrase : ""}`, `Mis recuerdos de ${a}`, "LOGRO", null, null, a);
          toast(q === "PUBLICA" ? "Tus recuerdos ya están en la comunidad" : "Tus seguidores ya ven tus recuerdos");
          v.querySelector(".reel-publicar").hidden = true;
        }
      } catch (err) { console.error(err); toast("No se pudo preparar la imagen." + (detalleError(err) || "")); }
      b.disabled = false;
    }
  });
  mostrar(0); raf = requestAnimationFrame(paso);
  v.querySelector(".reel-x").focus({ preventScroll: true });
};

/** Mosaico cuadrado 1080×1080 con hasta 9 fotos del año, el año grande y la frase de la cumbre. */
async function mosaicoAnio(a, fs, p) {
  const N = 1080, cv = Object.assign(document.createElement("canvas"), { width: N, height: N }), x = cv.getContext("2d");
  x.fillStyle = "#1D120D"; x.fillRect(0, 0, N, N);
  const cargar = src => new Promise(res => { const im = new Image(); im.crossOrigin = "anonymous"; im.onload = () => res(im); im.onerror = () => res(null); im.src = src; });
  const ims = (await Promise.all(fs.slice(-9).map(f => cargar(f.foto)))).filter(Boolean);
  const n = ims.length <= 1 ? 1 : ims.length <= 4 ? 2 : 3, g = 8, lado = (N - g * (n + 1)) / n;
  ims.slice(0, n * n).forEach((im, k) => {
    const cx = g + (k % n) * (lado + g), cy = g + Math.floor(k / n) * (lado + g), s = Math.min(im.width, im.height);
    x.save(); x.beginPath(); x.roundRect(cx, cy, lado, lado, 18); x.clip();
    x.drawImage(im, (im.width - s) / 2, (im.height - s) / 2, s, s, cx, cy, lado, lado); x.restore();
  });
  const gr = x.createLinearGradient(0, N * .55, 0, N); gr.addColorStop(0, "rgba(29,18,13,0)"); gr.addColorStop(1, "rgba(29,18,13,.92)");
  x.fillStyle = gr; x.fillRect(0, N * .55, N, N * .45);
  x.fillStyle = "#F3EDE0"; x.font = "700 150px 'Roboto Serif', Georgia, serif"; x.textBaseline = "alphabetic"; x.fillText(String(a), 56, N - 150);
  x.font = "500 38px Roboto, system-ui, sans-serif"; x.fillStyle = "rgba(243,237,224,.9)";
  const frase = (p.cumbreFrase || "Un año más cerca de mi cumbre").slice(0, 70); x.fillText(frase, 60, N - 92);
  x.font = "600 26px Montserrat, Roboto, sans-serif"; x.fillStyle = "#C9973B"; x.fillText("SUROUTE · MIS RECUERDOS", 60, N - 46);
  return await new Promise((res, rej) => cv.toBlob(b => b ? res(b) : rej(new Error("imagen")), "image/jpeg", .9));
}
