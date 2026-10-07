#!/usr/bin/env bash
# Arma Rutaalacima Web listo para publicar (GitHub Pages o cualquier hosting estático).
#   bash tools/armar_web.sh _site
# Opcional: SUPABASE_URL y SUPABASE_ANON_KEY en el entorno para conectarla a tu servidor.
# Sin ellas, la web abre en modo demostración.
# Para probarla en tu computador:  python3 -m http.server -d _site 8000  → http://localhost:8000
set -euo pipefail
cd "$(dirname "$0")/.."
DESTINO="${1:-_site}"
VERSION="${GITHUB_SHA:-$(git rev-parse --short HEAD 2>/dev/null || date +%s)}"

rm -rf "$DESTINO"
mkdir -p "$DESTINO/assets"
cp web/index.html web/privacidad.html web/estilos.css web/sello.png "$DESTINO/"
cp -r web/js "$DESTINO/js"
# Video de presentación (página web/video.html)
cp web/video.html "$DESTINO/"
if [ -d diseno/video ]; then
  mkdir -p "$DESTINO/video"
  cp diseno/video/rutaalacima_presentacion.mp4 diseno/video/portada.jpg "$DESTINO/video/"
fi
# Las 24 guías y las 365 frases (las mismas de la app)
cp -r app/src/main/assets/content "$DESTINO/assets/content"
cp -r app/src/main/assets/frases "$DESTINO/assets/frases"
# Grabaciones del autor incluidas en la app (assets/audios/{guía}/{capítulo}.mp3), si las hay
if [ -d app/src/main/assets/audios ]; then
  cp -r app/src/main/assets/audios "$DESTINO/assets/audios"
  ( cd "$DESTINO/assets/audios" && find . -mindepth 2 -maxdepth 2 -type f \( -name '*.mp3' -o -name '*.m4a' -o -name '*.aac' -o -name '*.ogg' -o -name '*.opus' -o -name '*.wav' \) \
      | sed 's|^\./||' | python3 -c 'import sys,json; print(json.dumps([{"guia":l.split("/")[0],"seccion":int(l.split("/")[1].rsplit(".",1)[0]),"archivo":l} for l in sys.stdin.read().split() if l.split("/")[1].rsplit(".",1)[0].isdigit()]))' > indice.json )
fi

# Configuración del sitio publicado
cat > "$DESTINO/config.js" <<CONFIG
/* Generado por tools/armar_web.sh: no editar aquí, sino en las variables del repositorio. */
window.RUTACIMA = {
  supabaseUrl: "${SUPABASE_URL:-}",
  supabaseAnonKey: "${SUPABASE_ANON_KEY:-}",
  contenido: "assets/",
  audiosIncluidos: $( [ -f "$DESTINO/assets/audios/indice.json" ] && echo true || echo false ),
};
CONFIG

# Evita que el navegador use una versión vieja después de publicar
sed -i "s|src=\"js/\([0-9a-z-]*\)\.js\"|src=\"js/\1.js?v=$VERSION\"|g; s|href=\"estilos.css\"|href=\"estilos.css?v=$VERSION\"|; s|src=\"config.js\"|src=\"config.js?v=$VERSION\"|" "$DESTINO/index.html"

# GitHub Pages: servir los archivos tal cual (sin Jekyll)
touch "$DESTINO/.nojekyll"
echo "Rutaalacima Web lista en $DESTINO ($(du -sh "$DESTINO" | cut -f1))"
if [ -n "${SUPABASE_URL:-}" ]; then echo "Conectada a $SUPABASE_URL"; else echo "Sin servidor configurado: abrirá en modo demostración"; fi
