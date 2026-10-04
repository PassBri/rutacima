# RutaCima

**Tu vida hasta los 120 años, vista año por año y día por día.**

RutaCima es la app del método **Ruta a la Cima** (7 Fases × 6 Ejes). Muestra tu vida como un camino de
puntos —un punto por año, hasta 120— y baja en cascada a meses, semanas y días, para que cada cosa que
haces hoy se vea en tu propósito a 5, 10, 15 o 20 años. Incluye un coach con inteligencia artificial,
365 frases de los libros selladas con cera y una comunidad para compartir el ascenso.

> Serie Ruta a la Cima · © 2026 Brian Gonzalo Suárez Acevedo. Todos los derechos reservados.

<p align="center">
  <img src="diseno/RutaCima_papel_blanco.png" alt="Pantallas de RutaCima en papel blanco" width="860">
</p>

## Qué hace (v0.4)

| Sección | Contenido |
|---|---|
| **Mi ruta** | "Camino hacia los 100 años" (o la meta que elijas, de 60 a 120): un punto por año, con banderas en los años que tienen metas. Al tocar un año se abre en cascada: **año → mes → semana → día**. Recordatorio del día con el día de vida, los días que quedan y una frase. Botón flotante para saltar a hoy, esta semana, este mes o este año. |
| **Hoy** | Tu cumbre en una frase, anillos de avance (propósito, año, mes y hoy), prioridad #1, metas del mes, checklist de 18 hábitos por eje y botones de rescate ("me perdí", "tuve un mal día", kit de emergencia). |
| **Metas** | Planificador en cascada con avance automático: lo que marcas cada día sube al mes, al año y al propósito. Asistente de 4 pasos con los bancos de metas, indicadores, acciones y proyectos de confluencia. |
| **Comunidad** | Bitácora con postales 4:3 (o cita destacada si no hay foto): logros, evidencias, visión, metas y reflexiones. Impulsos, comentarios, seguir personas y elegir quién ve cada publicación. |
| **Aprende** | 24 guías por etapas del ascenso, bancos de herramientas y lecturas. Las evaluaciones dan resultados: puntaje, interpretación, barras por grupo y radar de ejes. |
| **Perfil** | Publicaciones a dos columnas (4:5), **Mi vida**, **Mis frases**, vision board y **Ejes** con historial. |

Además:

- **Frase del día sellada:** 365 frases tomadas de los libros, una por día. Se abre rompiendo el sello con el
  rostro o la huella (lo verifica el teléfono; la app nunca ve tu cara) o con una clave propia.
- **Coach IA** que conoce tus metas, ejes y avance. Sin servidor funciona en modo guía.
- **Recordatorio diario** a la hora que elijas y **Kit de emergencia** (protocolos de la niebla, tracker de 7 días, matriz de decisiones).
- **12 idiomas:** español, inglés, portugués, francés, alemán, italiano, chino, japonés, coreano, árabe, hindi y ruso.

## Diseño

- **Papel blanco** por defecto: hojas blancas sobre un fondo hueso, sombras burdeos (el color del sello) y grano suave.
  En Ajustes también están **Papel antiguo** y **Pastel marrón**. Todo sigue Material Design 3 (`ui/theme/Papel.kt`).
- **Barra inferior compacta:** solo íconos. Al apoyar el dedo aparece el nombre de la pestaña; si deslizas el dedo
  por la barra, el nombre lo sigue y al soltar se abre esa pestaña. Con TalkBack cada ícono se anuncia con su nombre.
- La barra superior muestra el sello y el nombre de la pantalla en la que estás.
- Los años, meses, semanas y días son botones flotantes que se hunden al tocarlos.
- **Logo:** el sello de cera de Ruta a la Cima (original del autor) en el ícono, el arranque, la ventana de inicio y la barra.
  Archivos en alta resolución y vistas previas en `diseno/`.

## RutaCima Web

`web/` es **la misma app en el computador**, al estilo de WhatsApp Web, con la misma cuenta y los mismos datos
que el teléfono: Mi ruta (vida → año → mes → día), Hoy (hábitos, metas del mes, cierre del día), Metas
(propósitos, metas del año y del mes, con la misma cascada de avance), Comunidad (publicar, impulsar, comentar,
seguir), Aprende (las 24 guías, con tus respuestas), Coach, Mis frases y Perfil (datos, meta de vida y ejes).
Lo que cambias en un lado aparece en el otro.

**Cómo se conectan.** En la app: **Perfil › ícono del computador (RutaCima Web) › Escanear código**, y apuntas
al código QR que muestra la web (o escribes sus 8 letras). Desde ahí el computador queda vinculado a tu cuenta
hasta que lo desvincules desde el teléfono o cierres la sesión en la web.

- La app guarda todo en el teléfono (Room) y, mientras haya un computador vinculado, sincroniza cada cambio con la
  tabla `ruta_datos` de Supabase (`data/web/RutaWebRepository.kt`): al abrir la app, cada minuto mientras está
  abierta y 3 segundos después de cada cambio. Las reglas de quién gana están en `domain/model/Sincronia.kt`
  (con pruebas): si los dos lados cambiaron lo mismo, se unen los días y hábitos marcados.
- La web escribe directo en `ruta_datos` y recibe en vivo lo que llega del teléfono (Supabase Realtime).
- Sin `web/config.js` configurado, la web abre en **modo demostración** con una ruta de ejemplo.

<p align="center">
  <img src="diseno/web_mi_ruta.png" alt="RutaCima Web: Mi ruta" width="720">
</p>

### Poner la web en marcha

1. En Supabase, vuelve a ejecutar `supabase/schema.sql` completo (agrega `ruta_datos`, `dispositivos`, `vinculos`
   y las reglas para que el computador vinculado actúe como tu cuenta).
2. **Authentication › Sign In / Providers › Allow anonymous sign-ins**: actívalo (la web entra como invitado
   hasta que la vinculas).
3. Vuelve a desplegar el coach: `supabase functions deploy coach` (ahora también responde a la web).
4. En `web/config.js` pon la misma URL y clave anon de `local.properties`.
5. Publica la carpeta con **GitHub Pages** (Settings › Pages › rama `main`): queda en
   `https://passbri.github.io/rutacima/web/`. Si usas otra dirección, ponla en `local.properties` como
   `rutacima.webUrl=` para que el botón "Abrir RutaCima Web" de la app apunte ahí.

## Diseño con IA (skill "impeccable")

El proyecto incluye la skill de diseño [impeccable](https://github.com/pbakaus/impeccable) (Apache 2.0)
en `.claude/skills/impeccable`. Si abres el proyecto con Claude Code, usa por ejemplo
`/impeccable critique perfil` o `/impeccable polish comunidad`.

## Requisitos

- Android Studio Ladybug (2024.2) o más reciente (JDK 17+ incluido).
- Android SDK 35. Dispositivo o emulador con Android 8.0 (API 26) o superior.

## Abrir y ejecutar

1. Android Studio → **File › Open** → selecciona la carpeta del proyecto.
2. Espera la sincronización de Gradle (la primera vez descarga Gradle 8.11.1 y las dependencias).
3. Elige un emulador o conecta tu teléfono con depuración USB y pulsa **Run ▶**.

Sin configurar nada, la app funciona completa en el teléfono y la comunidad se muestra en **modo demostración**.

## Servidor (Supabase) y coach IA

1. Crea un proyecto en [supabase.com](https://supabase.com).
2. En **SQL Editor** ejecuta `supabase/schema.sql` (tablas de perfiles, publicaciones, impulsos,
   comentarios, seguidores y uso de IA; reglas de seguridad RLS; bucket público `media` para fotos).
3. Despliega el coach (la clave de Anthropic queda en el servidor, nunca en la app):
   ```bash
   supabase functions deploy coach
   supabase secrets set ANTHROPIC_API_KEY=sk-ant-...
   # opcionales
   supabase secrets set COACH_MODEL=claude-sonnet-5-5 COACH_DAILY_LIMIT=30
   ```
4. En `local.properties` (no se sube a GitHub) agrega:
   ```properties
   supabase.url=https://TU-PROYECTO.supabase.co
   supabase.anonKey=TU_ANON_KEY
   ```
5. Vuelve a compilar. En **Perfil › Ajustes** crea tu cuenta y la comunidad pasa a ser real.

## Arquitectura

```
app/src/main/
├── assets/content/          ← workbooks en JSON (generados desde los .docx)
├── assets/bancos/           ← bancos de metas, indicadores, acciones y proyectos
├── java/com/rutaalacima/app/
│   ├── domain/model/        ← Eje, Fase, Vida (120 años), Frases, Prioridad, Kit (reglas del método)
│   ├── data/content/        ← modelo de bloques, lectura de assets y cálculo de resultados
│   ├── data/local/          ← Room: perfil, respuestas, ejes, checklist, planificador, posts, coach
│   ├── data/Cascada.kt      ← avance automático día → mes → año → 5 años
│   ├── data/remote/         ← cliente Supabase (Auth, PostgREST, Storage, Functions)
│   ├── data/social/         ← comunidad (con modo demo) · data/coach/ ← coach IA
│   ├── data/web/            ← RutaCima Web: vincular un computador y sincronizar la ruta
│   └── ui/                  ← Compose + Material 3: hoy, metas, comunidad, aprende, perfil, coach,
│                              workbook, planner, kit, axes, onboarding, i18n
├── res/values*/strings.xml  ← textos en 12 idiomas
supabase/                    ← schema.sql y función "coach" (Deno)
web/                         ← RutaCima Web: index.html, app.js, estilos.css y config.js
tools/convert.py             ← conversor .docx → JSON · tools/bancos.py ← bancos
```

## Actualizar el contenido desde los Word

```bash
pip install python-docx
python3 tools/convert.py <carpeta_con_los_docx> app/src/main/assets/content
python3 tools/bancos.py
```

El conversor detecta como campo editable todo lo que va seguido de "Escriba aquí", una caja vacía o
una línea `____`; los `/10` y `(1-10)` como escalas (con resultados); y los `☐ Sí ☐ No` como opciones.
Los IDs de campo dependen del orden del documento. `planificador_cierre.json` está hecho a mano.

## Pruebas

`./gradlew test` valida los 24 JSON, los IDs únicos, el cálculo de resultados de las evaluaciones,
el calendario de vida, las 365 frases, la sincronización con la web y las reglas del dominio.

## Próximos pasos

- RutaCima Web en los 12 idiomas (hoy está en español).
- Revisión mensual con WorkManager.
- Traducción del contenido de las guías con IA.
- Exportar a PDF y copia de seguridad; modo facilitador para grupos.
- Dominio sugerido: rutacima.app.
