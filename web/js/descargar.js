/* Página de descargas: recomienda la versión del equipo que la abre, instala la app en el computador
 * (con el diálogo del navegador cuando existe) y muestra un QR para abrirla en el teléfono. */
"use strict";
(() => {
  const ua = navigator.userAgent;
  const ios = /iPhone|iPad|iPod/.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
  const android = /Android/.test(ua);
  const macSafari = navigator.platform === "MacIntel" && navigator.maxTouchPoints <= 1 && /Safari\//.test(ua) && !/Chrome|Chromium|Edg|OPR|Firefox/.test(ua);
  const instalada = matchMedia("(display-mode: standalone)").matches || navigator.standalone === true;
  const movil = ios || android;

  // La tarjeta del equipo que abre la página va primero y se marca como recomendada
  const propia = ios ? "iphone" : android ? "android" : "pc";
  const tarjeta = document.getElementById(propia);
  tarjeta.classList.add("recomendada");
  tarjeta.querySelector(".etiqueta").hidden = false;
  tarjeta.parentElement.prepend(tarjeta);
  if (movil) document.getElementById("qrTel").hidden = true;

  // Instalar en el computador
  let aviso = null;
  const boton = document.getElementById("instalarPc"), nota = document.getElementById("pcNota");
  const chromium = /Chrome|Chromium|Edg\//.test(ua) && !/OPR|Firefox/.test(ua);
  /** Ya está instalada: el botón la abre (Chrome y Edge la abren en su ventana) en vez de quedarse sin hacer nada. */
  const yaInstalada = () => {
    boton.innerHTML = "Abrir SuRoute"; boton.disabled = false; boton.dataset.abrir = "1";
    nota.innerHTML = "SuRoute ya está instalada en este computador. Ábrela desde el menú de inicio, el Dock o con <b>Abrir en la app</b> en la barra de direcciones.";
  };
  addEventListener("beforeinstallprompt", e => { e.preventDefault(); aviso = e; });
  addEventListener("appinstalled", () => { aviso = null; yaInstalada(); nota.textContent = "SuRoute quedó instalada. Búscala en el menú de inicio o en el Dock."; });
  if ("serviceWorker" in navigator && location.protocol === "https:") navigator.serviceWorker.register("sw.js").catch(() => {});
  if (instalada) yaInstalada();
  else navigator.getInstalledRelatedApps?.().then(apps => { if (apps?.length && !movil) yaInstalada(); }).catch(() => {});
  boton.addEventListener("click", async () => {
    if (boton.dataset.abrir) { location.href = "./"; return; }
    if (aviso) { aviso.prompt(); try { await aviso.userChoice; } catch {} aviso = null; return; }
    // Sin el diálogo del navegador: se explican los pasos según el navegador
    nota.innerHTML = macSafari
      ? "En la barra de menús de Safari abre <b>Archivo</b> y elige <b>Agregar al Dock</b> (macOS Sonoma o posterior)."
      : /Firefox/.test(ua)
        ? "Firefox no instala apps web. Abre <b>suroute.com</b> en <b>Chrome</b> o <b>Edge</b> y toca el ícono de instalar en la barra de direcciones."
        : movil
          ? "Esta opción es para el computador. En tu teléfono usa la tarjeta de Android o de iPhone."
          : chromium
            ? "Si en la barra de direcciones ves <b>Abrir en la app</b>, SuRoute ya está instalada: tócalo o búscala en el menú de inicio. " +
              "Si no, abre el menú <b>⋮</b> › <b>Transmitir, guardar y compartir</b> › <b>Instalar página como app</b>. <a href=\"./\">Abrir SuRoute Web</a>"
            : "Abre <b>suroute.com</b> en <b>Chrome</b> o <b>Edge</b> y toca el ícono de instalar en la barra de direcciones.";
    nota.classList.add("resaltada");
  });

  // QR para abrir esta página en el teléfono
  try {
    const q = qrcode(0, "H"); q.addData("https://suroute.com/descargar.html"); q.make();
    const img = new Image(); img.alt = ""; img.src = q.createDataURL(7, 0);
    document.getElementById("qr").append(img);
    const sello = new Image(); sello.src = "iconos/favicon.png"; sello.alt = ""; sello.className = "qr-sello";
    document.getElementById("qr").append(sello);
  } catch { document.getElementById("qrTel").hidden = true; }
})();
