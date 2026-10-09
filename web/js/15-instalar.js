/* Rutaalacima Web · instalarla como app (PWA). En Android y en el computador, el navegador ofrece
   instalarla con un botón; en iPhone y iPad se hace desde Safari: Compartir › Agregar a pantalla de inicio.
   Se avisa una sola vez (se puede cerrar) y queda siempre en Perfil › Instalar la app. */
P.compartirIos = "M16 5l-1.42 1.42-1.59-1.59V16h-1.98V4.83L9.42 6.42 8 5l4-4 4 4zm4 5v11c0 1.1-.9 2-2 2H6c-1.11 0-2-.9-2-2V10c0-1.11.89-2 2-2h3v2H6v11h12V10h-3V8h3c1.1 0 2 .89 2 2z";
P.agregarInicio = "M19 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14zm-8-2h2v-4h4v-2h-4V7h-2v4H7v2h4z";

var INSTALAR = {
  aviso: null,                                   // evento beforeinstallprompt (Android / computador)
  instalada: () => matchMedia("(display-mode: standalone)").matches || navigator.standalone === true,
  ios: () => /iPhone|iPad|iPod/.test(navigator.userAgent) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1),
  /** Solo en el sitio publicado (no en vistas previas incrustadas ni archivos locales). */
  sitio: (location.protocol === "https:" || location.hostname === "localhost") && window.top === window,
  /** Safari en Mac (macOS Sonoma o posterior): se instala con Archivo › Agregar al Dock. */
  macSafari: () => navigator.platform === "MacIntel" && navigator.maxTouchPoints <= 1 &&
    /Safari\//.test(navigator.userAgent) && !/Chrome|Chromium|Edg|OPR|Firefox/.test(navigator.userAgent),
  disponible() { return this.sitio && !this.instalada() && (!!this.aviso || this.ios() || this.macSafari()); },
};

// El service worker hace que se pueda instalar y que abra rápido (no en vistas previas ni archivos locales)
if ("serviceWorker" in navigator && (location.protocol === "https:" || location.hostname === "localhost") && window.top === window) {
  addEventListener("load", () => navigator.serviceWorker.register("sw.js").catch(e => console.warn("sin service worker", e)));
}

addEventListener("beforeinstallprompt", e => { e.preventDefault(); INSTALAR.aviso = e; programarAvisoInstalar(); if (estado.sec === "perfil") pintarLista(); });
addEventListener("appinstalled", () => { INSTALAR.aviso = null; document.getElementById("avisoInstalar")?.remove(); toast("¡Listo! Rutaalacima quedó instalada."); });

/** Instalar: en Android/computador abre el diálogo del navegador; en iPhone muestra los pasos. */
async function instalarApp() {
  document.getElementById("avisoInstalar")?.remove();
  if (INSTALAR.aviso) {
    INSTALAR.aviso.prompt();
    try { await INSTALAR.aviso.userChoice; } catch {}
    INSTALAR.aviso = null;
    return;
  }
  if (INSTALAR.macSafari()) {
    ventanaCuenta(`<h3 style="margin-top:0">Instala Rutaalacima en tu Mac</h3>
      <ol class="pasos-instalar">
        <li><span class="paso-ic">${ic("agregarInicio")}</span><span>En la barra de menús de Safari, abre <b>Archivo</b> y elige <b>Agregar al Dock</b>.</span></li>
        <li><span class="paso-ic"><img src="iconos/apple-touch-icon.png" alt=""></span><span>Toca <b>Agregar</b>. Rutaalacima quedará en el Dock y en Aplicaciones, y se abrirá en su propia ventana.</span></li>
      </ol>
      <p class="suave" style="font-size:13px">Necesitas macOS Sonoma o posterior. Con Chrome o Edge también puedes instalarla desde el ícono de la barra de direcciones.</p>
      <div class="botones"><button class="btn lleno" type="button" id="instalarListo">Entendido</button></div>`, v => {
      v.querySelector("#instalarListo").onclick = () => v.remove();
    });
    return;
  }
  ventanaCuenta(`<h3 style="margin-top:0">Instala Rutaalacima en tu ${/iPad/.test(navigator.userAgent) ? "iPad" : "iPhone"}</h3>
    <ol class="pasos-instalar">
      <li><span class="paso-ic">${ic("compartirIos")}</span><span>Abre esta página en <b>Safari</b> y toca <b>Compartir</b> (abajo o arriba de la pantalla).</span></li>
      <li><span class="paso-ic">${ic("agregarInicio")}</span><span>Desliza y elige <b>Agregar a pantalla de inicio</b>.</span></li>
      <li><span class="paso-ic"><img src="iconos/apple-touch-icon.png" alt=""></span><span>Toca <b>Agregar</b>. Rutaalacima aparecerá con su sello entre tus apps y se abrirá a pantalla completa.</span></li>
    </ol>
    <p class="suave" style="font-size:13px">Tu cuenta y tu ruta son las mismas: entra con tu correo.</p>
    <div class="botones"><button class="btn lleno" type="button" id="instalarListo">Entendido</button></div>`, v => {
    v.querySelector("#instalarListo").onclick = () => v.remove();
  });
}

/** Aviso discreto, una sola vez, cuando ya se está usando la app. */
function programarAvisoInstalar() {
  clearTimeout(programarAvisoInstalar.t);
  programarAvisoInstalar.t = setTimeout(() => {
    let cerrado = false; try { cerrado = localStorage.getItem("rutacima-instalar") === "no"; } catch {}
    if (cerrado || !INSTALAR.disponible() || $("pantallaApp").hidden || document.getElementById("avisoInstalar")) return;
    const a = document.createElement("div");
    a.id = "avisoInstalar"; a.className = "aviso-instalar"; a.setAttribute("role", "region"); a.setAttribute("aria-label", "Instalar la app");
    a.innerHTML = `<img src="iconos/apple-touch-icon.png" alt=""><div><b>Instala Rutaalacima</b><span>Ábrela desde tu pantalla de inicio, como una app.</span></div>
      <button class="btn mini lleno" type="button" id="avisoInstalarSi">Instalar</button><button class="aviso-cerrar" type="button" aria-label="Cerrar" id="avisoInstalarNo">×</button>`;
    document.body.append(a);
    $("avisoInstalarSi").onclick = instalarApp;
    $("avisoInstalarNo").onclick = () => { try { localStorage.setItem("rutacima-instalar", "no"); } catch {} a.remove(); };
  }, 4000);
}
addEventListener("load", programarAvisoInstalar);
Object.assign(ACC, { instalarApp() { instalarApp(); } });
