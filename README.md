# Ruta a la Cima · App Android

App Android (Kotlin + Jetpack Compose) del sistema **Ruta a la Cima**: el modelo
**7 Fases × 6 Ejes** para alcanzar tu Cumbre Personal, con todos los workbooks
interactivos, el Planificador 5 años y el Kit de Emergencia.

> Serie Ruta a la Cima · © 2026 Brian Gonzalo Suárez Acevedo. Todos los derechos reservados.

## Qué hace la app (v0.1)

| Pestaña | Contenido |
|---|---|
| **Inicio** | Cumbre en una frase, fase actual del viaje (con su portal), radar de los 6 ejes, checklist de hoy, "continúa donde quedaste" y recordatorio del día. |
| **Ruta** | Biblioteca con los 18 workbooks + el cierre del planificador, agrupados en La Ruta, Herramientas, Bonos y Facilitadores. Cada uno se diligencia en la app y se guarda solo. |
| **Plan 5 años** | 10 propósitos con prioridad ABCD, eje, indicador de éxito, visualización y plan de acción con fechas; metas anuales con matriz de decisión (Continuar/Acelerar/Pausar/Eliminar), semáforo de avance y fecha de revisión; balance anual. |
| **Ejes** | Evaluación 1–10 de Voluntad, Maestría, Voz, Valor, Evolución y Trascendencia, radar comparado con la evaluación anterior, interpretación sobre 60 e historial. |
| **Kit** | Acceso a los protocolos de "Cuando te pierdes en la niebla", checklist diario de 18 hábitos (3 por eje), tracker de 7 días, matriz de decisiones con puntaje, tarjetas de recordatorio y accesos a revisión mensual, plan semanal, reorientación y cierre mensual. |

Primera apertura: bienvenida → nombre y cumbre → fase actual → compromiso.

Todo se guarda **localmente** en el teléfono (Room/SQLite). No hay cuentas ni servidor todavía.

## Requisitos

- Android Studio Ladybug (2024.2) o más reciente (JDK 17+ incluido).
- Android SDK 35. Dispositivo o emulador con Android 8.0 (API 26) o superior.

## Abrir y ejecutar

1. Android Studio → **File › Open** → selecciona la carpeta `RutaALaCima`.
2. Espera la sincronización de Gradle (descarga Gradle 8.11.1 y las dependencias la primera vez).
3. Elige un emulador o conecta tu teléfono con depuración USB y pulsa **Run ▶**.

Desde terminal: `./gradlew assembleDebug` (APK en `app/build/outputs/apk/debug/`) y `./gradlew test` para las pruebas.

Si Android Studio sugiere actualizar AGP, Kotlin o la BOM de Compose, puedes aceptarlo; las versiones
fijadas en `gradle/libs.versions.toml` son estables y compatibles entre sí.

## Arquitectura

```
app/src/main/
├── assets/content/          ← workbooks en JSON (generados desde los .docx)
├── java/com/rutaalacima/app/
│   ├── domain/model/        ← Eje, Fase, Prioridad, Decisión, Semáforo, Kit (reglas del método)
│   ├── data/content/        ← modelo de bloques + lectura de assets (kotlinx.serialization)
│   ├── data/local/          ← Room: perfil, respuestas, evaluaciones, checklist, planificador
│   ├── data/Repositories.kt
│   ├── AppContainer.kt      ← inyección de dependencias manual
│   └── ui/                  ← Compose + Material 3 (home, library, workbook, planner, axes, kit, onboarding)
└── res/                     ← ícono adaptativo, sello de la marca, temas
tools/convert.py             ← conversor .docx → JSON
```

- **UI:** Jetpack Compose, Material 3, Navigation Compose, un ViewModel por pantalla.
- **Datos:** Room (KSP) para lo que escribe el usuario; JSON en `assets` para el contenido.
- **Motor de workbooks:** cada documento es una lista de secciones con bloques
  (`heading`, `paragraph`, `bullet`, `quote`, `callout`, `table`, `prompt`, `scale`, `check`,
  `choice`, `inputTable`). La pantalla de sección los dibuja y guarda cada respuesta por su `id`.
- **Identidad visual:** burdeos del sello de lacre, dorado de la franja, papel crema y serif
  para la lectura (tomados de los workbooks).

## Actualizar el contenido desde los Word

```bash
pip install python-docx
python3 tools/convert.py <carpeta_con_los_docx> app/src/main/assets/content
python3 tools/show.py app/src/main/assets/content/diagnostico.json   # revisar el resultado
```

El conversor detecta como campo editable todo lo que en el Word va seguido de "Escriba aquí",
una caja vacía o una línea `____`; los `☐ 01` como preguntas numeradas; los `/10` como escalas;
y los `☐ Sí ☐ No` como opciones. Los IDs de campo dependen del orden del documento: si cambias
mucho un Word ya publicado, las respuestas guardadas de ese workbook pueden quedar desalineadas.
`planificador_cierre.json` está hecho a mano y el conversor lo conserva.

## Pruebas

`./gradlew test` valida que los 19 JSON se lean, que no haya IDs repetidos, que los protocolos y
herramientas se encuentren, y las reglas del dominio (semáforo, matriz, checklist, plan de 5 años).

## Pendiente / próximos pasos

- Protocolos 6 ("Dudo de mi cumbre") y 7 ("Me autosaboteo") de *Cuando te pierdes en la niebla*:
  están en el índice del documento pero no tienen contenido; la app los muestra "En preparación".
- Exportar respuestas a PDF/Word y copia de seguridad (Drive o archivo).
- Recordatorios (revisión mensual el primer domingo, checklist diario) con WorkManager.
- Tipografías de marca (Google Fonts descargables) y vision board con imágenes.
- Cuenta y sincronización en la nube; modo facilitador para grupos.
