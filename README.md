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

`web/index.html` es la versión para computador, al estilo de WhatsApp Web: pantalla de vinculación con código QR,
riel de íconos (el nombre aparece al pasar el cursor), lista a la izquierda y detalle a la derecha. Incluye Mi ruta,
Hoy, Comunidad, Aprende, Coach, Mis frases y Perfil, con tema claro y oscuro, y se adapta al teléfono.
Por ahora es una **demostración con una ruta de ejemplo**; la vinculación real con el teléfono llega con el servidor.
Se abre con doble clic o se puede publicar con GitHub Pages (carpeta `/web`).

<p align="center">
  <img src="diseno/web_mi_ruta.png" alt="RutaCima Web: Mi ruta" width="720">
</p>

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
│   └── ui/                  ← Compose + Material 3: hoy, metas, comunidad, aprende, perfil, coach,
│                              workbook, planner, kit, axes, onboarding, i18n
├── res/values*/strings.xml  ← textos en 12 idiomas
supabase/                    ← schema.sql y función "coach" (Deno)
web/index.html               ← RutaCima Web (versión para computador)
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
el calendario de vida, las 365 frases y las reglas del dominio.

## Próximos pasos

- Vincular RutaCima Web con el teléfono (código QR + Supabase).
- Revisión mensual con WorkManager.
- Traducción del contenido de las guías con IA.
- Exportar a PDF y copia de seguridad; modo facilitador para grupos.
- Dominio sugerido: rutacima.app.
