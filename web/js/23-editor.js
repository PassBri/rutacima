/* SuRoute Web · editor de fotos (igual que en la app, ui/components/EditorFoto.kt): recortar con formato,
 * acercar y mover (rueda, pellizco o arrastre), girar y seis filtros sobrios. Devuelve un JPEG (Blob) o null. */
"use strict";

const FORMATOS_FOTO = {
  perfil: [{ nombre: "Cuadrada", p: 1, circulo: true }],
  portada: [{ nombre: "Portada", p: 3 }],
  publicacion: [{ nombre: "Cuadrada", p: 1 }, { nombre: "Vertical 4:5", p: 4 / 5 }, { nombre: "Horizontal", p: 1.91 }, { nombre: "Original", p: null }],
};
/** Las mismas matrices de color que la app (4×5, desplazamientos en 0-255). */
const FILTROS_FOTO = [
  ["Original", null],
  ["Cálido", [1.08, 0, 0, 0, 8, 0, 1.02, 0, 0, 2, 0, 0, .92, 0, -6, 0, 0, 0, 1, 0]],
  ["Frío", [.94, 0, 0, 0, -4, 0, 1, 0, 0, 2, 0, 0, 1.08, 0, 8, 0, 0, 0, 1, 0]],
  ["Blanco y negro", [.3, .59, .11, 0, 0, .3, .59, .11, 0, 0, .3, .59, .11, 0, 0, 0, 0, 0, 1, 0]],
  ["Sepia", [.393, .769, .189, 0, 0, .349, .686, .168, 0, 0, .272, .534, .131, 0, 0, 0, 0, 0, 1, 0]],
  ["Contraste", [1.2, 0, 0, 0, -24, 0, 1.2, 0, 0, -24, 0, 0, 1.2, 0, -24, 0, 0, 0, 1, 0]],
];

/** Filtros SVG para la vista previa (el resultado final se calcula píxel a píxel con la misma matriz). */
function asegurarFiltrosSvg() {
  if (document.getElementById("filtrosFoto")) return;
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  svg.id = "filtrosFoto"; svg.setAttribute("aria-hidden", "true"); svg.style.cssText = "position:absolute;width:0;height:0";
  svg.innerHTML = FILTROS_FOTO.map(([, m], i) => m ? `<filter id="ff${i}" color-interpolation-filters="sRGB"><feColorMatrix type="matrix" values="${m.map((v, k) => k % 5 === 4 ? v / 255 : v).join(" ")}"/></filter>` : "").join("");
  document.body.append(svg);
}

function editarFoto(archivo, tipo = "publicacion", lado = 1440) {
  return new Promise(async resolver => {
    let img;
    try { img = await createImageBitmap(archivo, { imageOrientation: "from-image" }); } catch { toast("No se pudo abrir esa imagen."); return resolver(null); }
    asegurarFiltrosSvg();
    const formatos = FORMATOS_FOTO[tipo] || FORMATOS_FOTO.publicacion;
    let fuente = img, giro = 0, formato = 0, filtro = 0, zoom = 1, dx = 0, dy = 0;
    const v = document.createElement("div");
    v.className = "editor-foto"; v.setAttribute("role", "dialog"); v.setAttribute("aria-modal", "true"); v.setAttribute("aria-label", "Editar foto");
    v.innerHTML = `<div class="ef-barra"><button class="ef-ic" data-ef="cancelar" aria-label="Cancelar">${ic("cerrar")}</button><b>Editar foto</b>
        <button class="ef-ic" data-ef="girar" aria-label="Girar">${ic("repetir")}</button></div>
      <div class="ef-zona"><div class="ef-marco"><canvas></canvas><i class="ef-tercios" aria-hidden="true"></i></div></div>
      <input class="ef-zoom" type="range" min="1" max="5" step="0.01" value="1" aria-label="Acercar">
      ${formatos.length > 1 ? `<div class="ef-chips" role="radiogroup" aria-label="Formato">${formatos.map((f, i) => `<button data-ef-formato="${i}" aria-pressed="${i === 0}">${f.nombre}</button>`).join("")}</div>` : ""}
      <div class="ef-filtros" role="radiogroup" aria-label="Filtro">${FILTROS_FOTO.map(([n], i) => `<button data-ef-filtro="${i}" aria-pressed="${i === 0}"><canvas width="64" height="64"></canvas><span>${n}</span></button>`).join("")}</div>
      <button class="btn lleno ef-usar" data-ef="usar">Usar foto</button>`;
    document.body.append(v);
    const marco = v.querySelector(".ef-marco"), lienzo = marco.querySelector("canvas"), ctx = lienzo.getContext("2d");
    const escalaBase = () => Math.max(lienzo.width / fuente.width, lienzo.height / fuente.height);
    const limitar = () => {
      const s = escalaBase() * zoom, mx = Math.max(0, (fuente.width * s - lienzo.width) / 2), my = Math.max(0, (fuente.height * s - lienzo.height) / 2);
      dx = Math.min(mx, Math.max(-mx, dx)); dy = Math.min(my, Math.max(-my, dy));
    };
    const proporcion = () => formatos[formato].p ?? Math.min(1.91, Math.max(.8, fuente.width / fuente.height));
    const ajustarMarco = () => {
      const zona = v.querySelector(".ef-zona").getBoundingClientRect(), p = proporcion();
      let w = zona.width, h = w / p; if (h > zona.height) { h = zona.height; w = h * p; }
      const dpr = Math.min(2, devicePixelRatio || 1);
      marco.style.width = w + "px"; marco.style.height = h + "px"; marco.classList.toggle("circulo", !!formatos[formato].circulo);
      lienzo.width = Math.round(w * dpr); lienzo.height = Math.round(h * dpr); limitar(); dibujar();
    };
    const dibujar = () => {
      const s = escalaBase() * zoom;
      ctx.clearRect(0, 0, lienzo.width, lienzo.height);
      ctx.drawImage(fuente, (lienzo.width - fuente.width * s) / 2 + dx, (lienzo.height - fuente.height * s) / 2 + dy, fuente.width * s, fuente.height * s);
      lienzo.style.filter = FILTROS_FOTO[filtro][1] ? `url(#ff${filtro})` : "none";
    };
    const miniaturas = () => v.querySelectorAll(".ef-filtros canvas").forEach((c, i) => {
      const x = c.getContext("2d"), s = Math.max(64 / fuente.width, 64 / fuente.height);
      x.drawImage(fuente, (64 - fuente.width * s) / 2, (64 - fuente.height * s) / 2, fuente.width * s, fuente.height * s);
      c.style.filter = FILTROS_FOTO[i][1] ? `url(#ff${i})` : "none";
    });
    const girar = async () => {
      const c = new OffscreenCanvas(fuente.height, fuente.width), x = c.getContext("2d");
      x.translate(c.width, 0); x.rotate(Math.PI / 2); x.drawImage(fuente, 0, 0);
      fuente = await createImageBitmap(c); giro = (giro + 90) % 360; zoom = 1; dx = dy = 0;
      v.querySelector(".ef-zoom").value = 1; ajustarMarco(); miniaturas();
    };
    const cerrar = blob => { removeEventListener("resize", ajustarMarco); removeEventListener("keydown", tecla); v.remove(); resolver(blob); };
    const tecla = e => { if (e.key === "Escape") cerrar(null); };
    addEventListener("resize", ajustarMarco); addEventListener("keydown", tecla);

    // Mover con arrastre, acercar con la rueda o pellizcando
    const punteros = new Map(); let distancia = 0;
    lienzo.addEventListener("pointerdown", e => { lienzo.setPointerCapture(e.pointerId); punteros.set(e.pointerId, e); });
    lienzo.addEventListener("pointermove", e => {
      if (!punteros.has(e.pointerId)) return;
      const antes = punteros.get(e.pointerId); punteros.set(e.pointerId, e);
      const dpr = lienzo.width / lienzo.clientWidth;
      if (punteros.size === 1) { dx += (e.clientX - antes.clientX) * dpr; dy += (e.clientY - antes.clientY) * dpr; }
      else if (punteros.size === 2) {
        const [a, b] = [...punteros.values()], d = Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY);
        if (distancia) zoom = Math.min(5, Math.max(1, zoom * d / distancia)); distancia = d;
        v.querySelector(".ef-zoom").value = zoom;
      }
      limitar(); dibujar();
    });
    const soltar = e => { punteros.delete(e.pointerId); distancia = 0; };
    lienzo.addEventListener("pointerup", soltar); lienzo.addEventListener("pointercancel", soltar);
    lienzo.addEventListener("wheel", e => { e.preventDefault(); zoom = Math.min(5, Math.max(1, zoom * (e.deltaY < 0 ? 1.08 : 1 / 1.08))); v.querySelector(".ef-zoom").value = zoom; limitar(); dibujar(); }, { passive: false });
    v.querySelector(".ef-zoom").addEventListener("input", e => { zoom = Number(e.target.value); limitar(); dibujar(); });
    v.addEventListener("click", async e => {
      const b = e.target.closest("button"); if (!b) return;
      if (b.dataset.ef === "cancelar") return cerrar(null);
      if (b.dataset.ef === "girar") return girar();
      if (b.dataset.efFormato) { formato = Number(b.dataset.efFormato); zoom = 1; dx = dy = 0; v.querySelector(".ef-zoom").value = 1;
        v.querySelectorAll("[data-ef-formato]").forEach(x => x.setAttribute("aria-pressed", x === b)); ajustarMarco(); }
      if (b.dataset.efFiltro) { filtro = Number(b.dataset.efFiltro); v.querySelectorAll("[data-ef-filtro]").forEach(x => x.setAttribute("aria-pressed", x === b)); dibujar(); }
      if (b.dataset.ef === "usar") { b.disabled = true; cerrar(await exportar()); }
    });

    /** Lleva el encuadre a la foto real, aplica el filtro y entrega un JPEG con el lado mayor ≤ [lado]. */
    async function exportar() {
      const s = escalaBase() * zoom, izq = (lienzo.width - fuente.width * s) / 2 + dx, arr = (lienzo.height - fuente.height * s) / 2 + dy;
      const sx = -izq / s, sy = -arr / s, sw = lienzo.width / s, sh = lienzo.height / s;
      const k = Math.min(1, lado / Math.max(sw, sh)), w = Math.max(1, Math.round(sw * k)), h = Math.max(1, Math.round(sh * k));
      const c = document.createElement("canvas"); c.width = w; c.height = h;
      const x = c.getContext("2d"); x.imageSmoothingQuality = "high"; x.drawImage(fuente, sx, sy, sw, sh, 0, 0, w, h);
      const m = FILTROS_FOTO[filtro][1];
      if (m) {
        const d = x.getImageData(0, 0, w, h), p = d.data;
        for (let i = 0; i < p.length; i += 4) {
          const r = p[i], g = p[i + 1], bl = p[i + 2];
          p[i] = m[0] * r + m[1] * g + m[2] * bl + m[4];
          p[i + 1] = m[5] * r + m[6] * g + m[7] * bl + m[9];
          p[i + 2] = m[10] * r + m[11] * g + m[12] * bl + m[14];
        }
        x.putImageData(d, 0, 0);
      }
      return new Promise(r => c.toBlob(b => r(b), "image/jpeg", .88));
    }
    requestAnimationFrame(() => { ajustarMarco(); miniaturas(); v.querySelector('[data-ef="usar"]').focus(); });
  });
}
window.editarFoto = editarFoto;
