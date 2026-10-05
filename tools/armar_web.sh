#!/usr/bin/env bash
# Arma RutaCima Web listo para publicar (GitHub Pages o cualquier hosting estático).
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
cp web/index.html web/app.js web/estilos.css web/sello.png "$DESTINO/"
# Las 24 guías y las 365 frases (las mismas de la app)
cp -r app/src/main/assets/content "$DESTINO/assets/content"
cp -r app/src/main/assets/frases "$DESTINO/assets/frases"

# Configuración del sitio publicado
cat > "$DESTINO/config.js" <<CONFIG
/* Generado por tools/armar_web.sh: no editar aquí, sino en las variables del repositorio. */
window.RUTACIMA = {
  supabaseUrl: "${SUPABASE_URL:-}",
  supabaseAnonKey: "${SUPABASE_ANON_KEY:-}",
  contenido: "assets/",
};
CONFIG

# Evita que el navegador use una versión vieja después de publicar
sed -i "s|src=\"app.js\"|src=\"app.js?v=$VERSION\"|; s|href=\"estilos.css\"|href=\"estilos.css?v=$VERSION\"|; s|src=\"config.js\"|src=\"config.js?v=$VERSION\"|" "$DESTINO/index.html"

# GitHub Pages: servir los archivos tal cual (sin Jekyll)
touch "$DESTINO/.nojekyll"
echo "RutaCima Web lista en $DESTINO ($(du -sh "$DESTINO" | cut -f1))"
if [ -n "${SUPABASE_URL:-}" ]; then echo "Conectada a $SUPABASE_URL"; else echo "Sin servidor configurado: abrirá en modo demostración"; fi
