# RutaCima · App Android

**RutaCima** es la app del método **Ruta a la Cima** (7 Fases × 6 Ejes): un planificador de metas en
cascada —**5 años → año → mes → hoy**— con coach de inteligencia artificial y una comunidad tipo red
social donde cada persona comparte con fotos sus logros, su vision board y el registro de cada año de su vida.

> Serie Ruta a la Cima · © 2026 Brian Gonzalo Suárez Acevedo. Todos los derechos reservados.

## Qué hace la app (v0.2)

| Pestaña | Contenido |
|---|---|
| **Hoy** | Tu cumbre en una frase, anillos de avance de la cascada (5 años, año, mes, hoy), prioridad #1, metas del mes para marcar el día con un toque, checklist de 18 hábitos, botones de rescate ("me perdí", "tuve un mal día", kit de emergencia), acceso al coach y a compartir avance. |
| **Metas** | **Cascada** con avance automático (lo que marcas cada día sube al mes, al año y al propósito de 5 años) y pestañas **Día, Mes, Año, 5 años y Balance** del planificador. El botón **Nueva meta** abre un asistente de 4 pasos que usa los bonos: banco de metas, banco de indicadores, banco de acciones y proyectos de confluencia, más "Mejorar con IA" (formato ORSE). |
| **Comunidad** | Feed estilo Instagram con fotos: logros, evidencias, visión, metas y reflexiones. Impulsos (votos), comentarios, seguir personas y compartir. Visibilidad pública, seguidores o solo yo. |
| **Aprende** | Las 24 guías organizadas por las etapas del ascenso (Conócete, Traza tu ruta, Camina, Supera obstáculos, Celebra la cima), bancos de herramientas, lecturas y facilitadores. Las evaluaciones ahora **generan resultados**: total, porcentaje, interpretación, barras por grupo, foco sugerido y radar de ejes que se puede guardar en "Mis Ejes". |
| **Perfil** | Publicaciones en cuadrícula, **Mi vida** (registro año por año), **Vision board** y **Ejes** (evaluación 1–10, radar comparado e historial). Ajustes: idioma, perfil y cuenta. |

Además: **Coach IA** (orienta con tus metas, ejes y avance reales; sin servidor funciona en modo guía),
**Kit de emergencia** (protocolos de la niebla, checklist, tracker de 7 días, matriz de decisiones, tarjetas)
y bienvenida con compromiso.

**Idiomas de la interfaz (12):** español, inglés, portugués, francés, alemán, italiano, chino, japonés,
coreano, árabe, hindi y ruso. Se cambian en Ajustes o desde los ajustes de idioma por app de Android 13+.
El contenido de las guías (workbooks) está en español.

**Diseño:** sin imágenes de los libros; ilustraciones vectoriales de montañas generadas en código
(`ui/components/Arte.kt`) e íconos Material.

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
│   ├── domain/model/        ← Eje, Fase, Prioridad, Decisión, Semáforo, Kit (reglas del método)
│   ├── data/content/        ← modelo de bloques, lectura de assets y cálculo de resultados
│   ├── data/local/          ← Room: perfil, respuestas, ejes, checklist, planificador, posts, coach
│   ├── data/Cascada.kt      ← avance automático día → mes → año → 5 años
│   ├── data/remote/         ← cliente Supabase (Auth, PostgREST, Storage, Functions)
│   ├── data/social/         ← comunidad (con modo demo) · data/coach/ ← coach IA
│   └── ui/                  ← Compose + Material 3: hoy, metas, comunidad, aprende, perfil, coach,
│                              workbook, planner, kit, axes, onboarding, i18n
├── res/values*/strings.xml  ← textos en 12 idiomas
supabase/                    ← schema.sql y función "coach" (Deno)
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

`./gradlew test` valida los 24 JSON, los IDs únicos, el cálculo de resultados de las evaluaciones
y las reglas del dominio.

## Próximos pasos

- Notificaciones (revisión mensual, checklist diario) con WorkManager.
- Traducción del contenido de las guías con IA.
- Exportar a PDF y copia de seguridad; modo facilitador para grupos.
- Dominio sugerido: rutacima.app.
