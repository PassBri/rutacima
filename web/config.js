/* Configuración de Rutaalacima Web para probarla en tu computador.
 * En GitHub Pages no se usa este archivo: tools/armar_web.sh lo genera con las variables del repositorio.
 * Usa los mismos valores de local.properties de la app (supabase.url y supabase.anonKey).
 * La clave "anon" es pública por diseño: la seguridad la ponen las reglas RLS de supabase/schema.sql.
 * Si se dejan vacíos, la web abre en modo demostración con una ruta de ejemplo. */
window.RUTACIMA = {
  supabaseUrl: "",        // https://TU-PROYECTO.supabase.co
  supabaseAnonKey: "",    // clave anon pública del proyecto
  // Carpeta con las guías y las 365 frases (las mismas de la app)
  contenido: "../app/src/main/assets/",
};
