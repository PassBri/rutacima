# Video de presentación de Rutaalacima

El video ([`diseno/video/rutaalacima_presentacion.mp4`](../diseno/video/rutaalacima_presentacion.mp4), 55 s, 1920 × 1080)
está hecho con código, con [Remotion](https://www.remotion.dev) y su skill oficial para agentes
([remotion-dev/skills](https://github.com/remotion-dev/skills)). Así se puede cambiar un texto o una pantalla y volver a
generarlo igual.

## Escenas

| # | Escena | Archivo |
|---|---|---|
| 1 | El sello cae y aparece Rutaalacima | `src/escenas/Apertura.tsx` |
| 2 | Tu vida hasta los 120 años, en puntos | `src/escenas/Vida.tsx` |
| 3 | 7 fases × 6 ejes sobre la montaña | `src/escenas/Metodo.tsx` |
| 4 | Metas en cascada (anillos) | `src/escenas/Cascada.tsx` |
| 5 | Recorrido por la app: Mi ruta, Hoy, Metas, Aprende y Coach | `src/escenas/Recorrido.tsx` |
| 6 | Comunidad, cordadas y coach de vida | `src/escenas/Comunidad.tsx` |
| 7 | La frase sellada: el sello se parte | `src/escenas/FraseSellada.tsx` |
| 8 | Rutaalacima Web | `src/escenas/Web.tsx` |
| 9 | Cierre: «Tu cumbre te espera» | `src/escenas/Cierre.tsx` |

Las pantallas (`public/caps/`) son capturas reales de Rutaalacima Web en modo demostración. La música
(`public/musica.mp3`) es original, generada para este video.

## Editar y volver a generar

```bash
cd video
npm install
npx remotion studio                 # vista previa interactiva en el navegador
npx remotion render Presentacion out/rutaalacima.mp4 --codec=h264 --crf=20
```

Luego copia `out/rutaalacima.mp4` a `diseno/video/rutaalacima_presentacion.mp4`.
