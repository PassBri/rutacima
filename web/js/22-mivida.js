/* SuRoute Web · Mi vida como perfil: foto de portada, foto de perfil, nombre, edad, la cumbre y «Quién soy».
 * Igual que en la app (ui/ruta/MiVidaPerfil.kt). Con cuenta, las fotos van al bucket público "media"
 * (carpeta propia, perfil/avatar.jpg y perfil/portada.jpg) y la foto de perfil queda como avatar de la comunidad. */
"use strict";

const FOTOS_PERFIL = { avatar: { campo: "fotoPerfilUrl", local: "fotoPerfil", lado: 720 }, portada: { campo: "fotoPortadaUrl", local: "fotoPortada", lado: 1600 } };
const mv = { menu: null, editando: false, demo: {} };

/** Reduce la imagen (lado mayor [lado] px) a JPEG antes de subirla. */
async function reducirImagen(archivo, lado) {
  try {
    const bmp = await createImageBitmap(archivo);
    const e = Math.min(1, lado / Math.max(bmp.width, bmp.height));
    const c = document.createElement("canvas"); c.width = Math.round(bmp.width * e); c.height = Math.round(bmp.height * e);
    c.getContext("2d").drawImage(bmp, 0, 0, c.width, c.height);
    return await new Promise(r => c.toBlob(b => r(b || archivo), "image/jpeg", .85));
  } catch { return archivo; }
}

function fotoPerfilDe(tipo) {
  const f = FOTOS_PERFIL[tipo];
  return mv.demo[tipo] || perfil()[f.campo] || "";
}

async function ponerFotoPerfil(tipo, archivo) {
  const f = FOTOS_PERFIL[tipo]; if (!f || !archivo) return;
  mv.menu = null;
  if (!Store.nube) { mv.demo[tipo] = URL.createObjectURL(archivo); pintarDetalle(); return; }
  toast("Subiendo tu foto…");
  try {
    const sb = Store.nube.sb, ruta = `${Store.nube.dueno}/perfil/${tipo}.jpg`;
    const blob = await reducirImagen(archivo, f.lado);
    const up = await sb.storage.from("media").upload(ruta, blob, { upsert: true, contentType: "image/jpeg" });
    if (up.error) throw up.error;
    const url = sb.storage.from("media").getPublicUrl(ruta).data.publicUrl + "?v=" + Date.now();
    // La ruta local del teléfono se vacía para que allá también se vea la foto nueva
    await Store.cambiar("perfil", 1, { [f.campo]: url, [f.local]: "" }, perfil());
    if (tipo === "avatar") await sb.from("profiles").update({ avatar_url: url }).eq("id", Store.nube.dueno);
    toast("Listo.");
  } catch (e) { console.error(e); toast("No se pudo subir la foto." + detalleError(e)); }
}

async function quitarFotoPerfil(tipo) {
  const f = FOTOS_PERFIL[tipo]; mv.menu = null;
  if (!Store.nube) { delete mv.demo[tipo]; pintarDetalle(); return; }
  await Store.cambiar("perfil", 1, { [f.campo]: "", [f.local]: "" }, perfil());
  try {
    await Store.nube.sb.storage.from("media").remove([`${Store.nube.dueno}/perfil/${tipo}.jpg`]);
    if (tipo === "avatar") await Store.nube.sb.from("profiles").update({ avatar_url: "" }).eq("id", Store.nube.dueno);
  } catch (e) { console.error(e); }
}

/** Botón de foto: si ya hay una, abre un menú (cambiar o quitar); si no, abre el selector. */
function botonFoto(tipo, clase, contenido, etiqueta) {
  const hay = !!fotoPerfilDe(tipo), input = `<input type="file" accept="image/*" hidden data-foto-perfil="${tipo}">`;
  const menu = mv.menu === tipo ? `<div class="mv-menu" role="menu">
      <label class="mv-menu-i" role="menuitem">${ic("foto")}${etiqueta}${input}</label>
      <button class="mv-menu-i" role="menuitem" data-acc="mvQuitar" data-arg="${tipo}">${ic("borrar")}Quitar foto</button></div>` : "";
  return hay
    ? `<div class="mv-foto-wrap"><button class="${clase}" data-acc="mvMenu" data-arg="${tipo}" aria-label="${etiqueta}" aria-haspopup="menu" aria-expanded="${mv.menu === tipo}">${contenido}</button>${menu}</div>`
    : `<label class="${clase}" aria-label="${etiqueta}" title="${etiqueta}">${contenido}${input}</label>`;
}

/** La cabecera de Mi vida. */
function cabeceraVida() {
  const p = perfil(), r = recordatorio(), portada = fotoPerfilDe("portada"), foto = fotoPerfilDe("avatar");
  const fondo = portada ? `<img src="${esc(portada)}" alt="Foto de portada">` : (window.Arte ? Arte.cordillera("portada-" + (p.nombre || ""), null, { bandera: false }) : "");
  const inicial = esc((p.nombre || "·").trim().charAt(0).toUpperCase() || "·");
  const avatar = (foto ? `<img src="${esc(foto)}" alt="Foto de perfil">` : `<span class="mv-inicial">${inicial}</span>`) + `<span class="mv-cam" aria-hidden="true">${ic("foto")}</span>`;
  return `<section class="mv-cab">
    <div class="mv-portada">${fondo}${botonFoto("portada", "mv-btn-portada", ic("foto"), "Cambiar portada")}</div>
    <div class="mv-id">
      ${botonFoto("avatar", "mv-avatar", avatar, "Cambiar foto de perfil")}
      <div class="mv-nombre"><h1>${esc(p.nombre || "Mi vida")}</h1>${r ? `<p>${r.edad} años · día ${r.dia.toLocaleString("es")} de tu vida</p>` : ""}</div>
    </div>
    ${p.cumbreFrase ? `<p class="mv-cumbre">«${esc(p.cumbreFrase)}»</p>` : ""}
  </section>`;
}

/** «Quién soy»: la presentación en primera persona; se edita ahí mismo. */
function quienSoy() {
  const t = perfil().quienSoy || "";
  if (mv.editando) return hoja(`<div class="mv-qs-cab"><h3>Quién soy</h3></div>
    <textarea id="mvQuien" class="mv-qs-txt" rows="5" maxlength="1200" placeholder="Soy… Valoro… Me mueve…">${esc(t)}</textarea>
    <div class="botones" style="justify-content:flex-end;margin-top:10px"><button class="btn" data-acc="mvQuienCancelar">Cancelar</button><button class="btn lleno" data-acc="mvQuienGuardar">Guardar</button></div>`);
  return hoja(`<div class="mv-qs-cab"><h3>Quién soy</h3><button class="btn mini" data-acc="mvQuienEditar">${ic("editar")} Editar</button></div>
    ${t ? `<p class="mv-qs">${esc(t).replace(/\n/g, "<br>")}</p>`
        : `<button class="mv-qs-vacio" data-acc="mvQuienEditar">Cuéntate en pocas líneas: quién eres hoy, qué valoras y qué te mueve. Toca para escribir.</button>`}`);
}

Object.assign(ACC, {
  mvMenu(el, tipo) { mv.menu = mv.menu === tipo ? null : tipo; pintarDetalle(); },
  mvQuitar(el, tipo) { quitarFotoPerfil(tipo); },
  mvQuienEditar() { mv.editando = true; pintarDetalle(); setTimeout(() => $("mvQuien")?.focus(), 30); },
  mvQuienCancelar() { mv.editando = false; pintarDetalle(); },
  async mvQuienGuardar() { const v = ($("mvQuien")?.value || "").trim(); mv.editando = false; await Store.cambiar("perfil", 1, { quienSoy: v }, perfil()); },
});
window.cabeceraVida = cabeceraVida;
window.quienSoy = quienSoy;
window.ponerFotoPerfil = ponerFotoPerfil;
