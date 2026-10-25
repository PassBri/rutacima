<p align="center"><img src="../diseno/sello_cera_suroute.png" alt="Sello de SuRoute" width="96"></p>

# SuRoute para desarrolladores

Cómo compilar la app, configurar el servidor y publicar la web. Para conocer la app, lee la [guía](GUIA.md).

## Instalar y ejecutar

**Requisitos:**

- Android Studio Ladybug (2024.2) o más reciente, que ya incluye JDK 17+.
- Android SDK 35.
- Un teléfono o emulador con Android 8.0 (API 26) o superior.

**Pasos:**

1. En Android Studio, **File › Open** y elige la carpeta del proyecto.
2. Espera la sincronización de Gradle. La primera vez descarga Gradle y las dependencias.
3. Elige un emulador o conecta tu teléfono con depuración USB, y pulsa **Run ▶**.

Para actualizar después: **Git › Pull** y **Run ▶**.

Sin configurar nada, la app funciona completa en el teléfono. La comunidad se muestra en modo demostración y el
coach en modo guía.

**Sin Android Studio:** cada cambio en `main` se compila en GitHub Actions
([`.github/workflows/android.yml`](.github/workflows/android.yml)). El APK queda en
[Releases › APK más reciente](https://github.com/PassBri/suroute/releases/tag/apk-reciente) y como artefacto de
cada ejecución.

**Versión para Google Play.** Para firmar el AAB de lanzamiento, crea en **Settings › Secrets and variables ›
Actions › Secrets**: `RUTACIMA_KEYSTORE_BASE64` (el `.jks` en base64), `RUTACIMA_KEYSTORE_PASSWORD`,
`RUTACIMA_KEY_ALIAS` y `RUTACIMA_KEY_PASSWORD`. En tu computador, lo mismo va en `keystore.properties`
(`storeFile`, `storePassword`, `keyAlias`, `keyPassword`), que no se sube. La versión de lanzamiento se reduce y
ofusca con R8.

## Servidor, coach IA y web

SuRoute usa [Supabase](https://supabase.com) para las cuentas, la comunidad, el coach y la web.

1. **Base de datos.** Crea un proyecto y, en **SQL Editor**, ejecuta completo [`supabase/schema.sql`](../supabase/schema.sql).
   Crea:
   - perfiles, publicaciones, impulsos, comentarios y seguidores;
   - el uso del coach;
   - la ruta compartida y los computadores vinculados;
   - las conversaciones, mensajes, reportes, coaches y acompañamientos;
   - los audiolibros: autores, grabaciones y el bucket `audios`;
   - reportes, bloqueos, límites contra el spam y `eliminar_mi_cuenta`;
   - las cordadas, las sesiones y las notas privadas de los coaches;
   - las reglas de seguridad y el bucket `media` para las fotos.
2. **Web.** En **Authentication › Sign In / Providers**, activa **Allow anonymous sign-ins**. La web entra como
   invitada hasta que la vinculas.
3. **Coach.** La clave de la inteligencia artificial queda en el servidor:
   ```bash
   supabase functions deploy coach
   supabase secrets set ANTHROPIC_API_KEY=sk-ant-...
   # opcionales
   supabase secrets set COACH_MODEL=claude-sonnet-5-5 COACH_DAILY_LIMIT=30
   ```
4. **App.** En `local.properties` (no se sube a GitHub):
   ```properties
   supabase.url=https://TU-PROYECTO.supabase.co
   supabase.anonKey=TU_ANON_KEY
   # opcional: dónde está publicada la web
   rutacima.webUrl=https://suroute.com/
   ```
5. **Web.** Pon la misma URL y la misma clave en [`web/config.js`](../web/config.js). La clave *anon* es pública por
   diseño; la seguridad la ponen las reglas del paso 1.
6. **Publicar la web.** Ya está todo listo para GitHub Pages ([`.github/workflows/web.yml`](.github/workflows/web.yml)):
   - Se publica en la rama `gh-pages`. Si la página no abre, revisa **Settings › Pages**: *Deploy from a branch*, rama `gh-pages`, carpeta `/ (root)`.
   - Para conectarla a tu servidor: **Settings › Secrets and variables › Actions › Variables** y crea
     `RUTACIMA_SUPABASE_URL` y `RUTACIMA_SUPABASE_ANON_KEY`. Sin ellas abre en modo demostración.
   - Cada vez que cambias algo en `web/`, la guías o las frases y lo subes a `main`, se publica sola en
     `https://suroute.com/`. También puedes publicarla a mano en **Actions › Publicar SuRoute Web › Run workflow**.
   - Para probarla antes en tu computador: `bash tools/armar_web.sh _site` y `python3 -m http.server -d _site 8000`.
7. **Verificar coaches.** Las postulaciones llegan a **Table Editor › coaches** con `verificado = false`. Revisa la
   ficha y cambia `verificado` a `true` para que aparezca en el directorio. Los reportes (de conversaciones y de
   publicaciones) llegan a **Table Editor › reportes**. Una publicación con 3 reportes queda con `oculto = true`;
   si la revisas y está bien, vuelve a ponerlo en `false`.
8. **Cuenta de autor (audiolibros).** Para subir tus grabaciones, agrega tu usuario en **Table Editor › autores**
   (`user_id` = tu id de **Authentication › Users**). Las grabaciones quedan en el bucket público `audios`.
9. **Limpieza (opcional).** Si activas la extensión **pg_cron** antes de ejecutar el esquema, las sesiones web
   invitadas que nunca se vincularon se borran solas cada noche.
10. **Probar.** Vuelve a compilar la app y crea tu cuenta en **Perfil › Ajustes**. La comunidad pasa a ser real y ya
   puedes vincular la web.

## Cómo está hecha

| Parte | Tecnología |
|---|---|
| App Android | Kotlin 2.0 · Jetpack Compose · Material 3 · Navigation |
| Datos en el teléfono | Room (SQLite), con migraciones para no perder datos al actualizar |
| Recordatorios y avisos | WorkManager |
| Audiolibros | TextToSpeech · MediaPlayer · servicio en primer plano |
| Widget | Jetpack Glance |
| Seguridad | BiometricPrompt (rostro, huella o bloqueo del teléfono) · PBKDF2 |
| Código QR | Escáner de Google Play Services (sin permiso de cámara para la app) |
| Servidor | Supabase: Auth, PostgreSQL con RLS, Storage, Realtime y Edge Functions (Deno) |
| Coach | Función `coach` en Supabase con la API de Claude |
| Web | HTML, CSS y JavaScript sin compilación · supabase-js |

```
app/src/main/
├── assets/content/          ← las 24 guías en JSON (generadas desde los .docx)
├── assets/bancos/           ← bancos de metas, indicadores, acciones y proyectos
├── assets/frases/           ← las 365 frases del día en 12 idiomas
├── java/com/rutaalacima/app/
│   ├── domain/model/        ← reglas del método: ejes, fases, vida, frases, prioridad, kit, sincronización
│   ├── data/content/        ← lectura de las guías y cálculo de resultados
│   ├── data/local/          ← Room: perfil, respuestas, ejes, checklist, planificador, publicaciones, coach
│   ├── data/Cascada.kt      ← avance automático día → mes → año → propósito
│   ├── data/remote/         ← cliente de Supabase
│   ├── data/social/         ← comunidad (con modo demostración)
│   ├── data/coach/          ← coach IA y modo guía
│   ├── data/frases/         ← frase del día y frases abiertas
│   ├── data/web/            ← SuRoute Web: vincular un computador y sincronizar
│   ├── seguridad/           ← sello: rostro, huella o clave
│   ├── data/audio/          ← audiolibros: grabaciones, voz del teléfono y reproductor
│   ├── notificaciones/      ← recordatorio diario y avisos de mensajes
│   ├── widget/              ← widget de la pantalla de inicio
│   └── ui/                  ← pantallas en Compose (ruta, hoy, metas, comunidad, aprende, perfil, web…)
└── res/values*/strings.xml  ← textos en 12 idiomas
supabase/                    ← schema.sql y la función "coach"
web/                         ← SuRoute Web: index.html, estilos.css, config.js y los documentos legales
web/js/                      ← la web por partes: 01-base … 09-arranque (se cargan en orden)
diseno/                      ← logo, vistas previas, las 365 frases y el video de presentación
video/                       ← el video de presentación hecho con Remotion (código de las escenas)
tools/                       ← conversores de los Word a JSON
```

## Contenido, pruebas y próximos pasos

**Actualizar las guías desde los Word:**

```bash
pip install python-docx
python3 tools/convert.py <carpeta_con_los_docx> app/src/main/assets/content
python3 tools/bancos.py
```

**Cómo lee los Word el conversor:**

- Detecta como campo para escribir todo lo que va seguido de "Escriba aquí", una caja vacía o una línea `____`.
- Toma los `/10` y `(1-10)` como escalas, que dan resultados.
- Toma los `☐ Sí ☐ No` como opciones.

**Pruebas:** `./gradlew test` (también en cada push) comprueba:

- las 24 guías y sus IDs;
- el cálculo de resultados de las evaluaciones;
- el calendario de vida;
- las 365 frases;
- el vision board;
- la sincronización con la web;
- las reglas del método;
- la locución de los audiolibros y la rotura del sello;
- los avisos, la primera semana, el regreso, las revisiones y la racha;
- las metas que propone el coach y la copia de seguridad;
- las pantallas principales con Robolectric (que abren sin errores).

**Próximos pasos:**

- SuRoute Web en los 12 idiomas (hoy está en español).
- Traducción del contenido de las guías.
- Exportar a PDF.
- Avisos al instante con notificaciones push (hoy se revisan cada 15 minutos).
- Modo facilitador para grupos.
- Publicación en Google Play (lista de pasos en [google-play.md](google-play.md)).
