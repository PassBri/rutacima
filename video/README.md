# Video de presentación de SuRoute

## Versión actual (v3): `diseno/video/suroute_presentacion.mp4`

61 s, 1280 × 720, 30 fps. Las pantallas son capturas reales de la web en modo demostración
(`v3/pantallas/`). `v3/escena.html` dibuja cada cuadro a partir del tiempo y `v3/config.js` define
las escenas, el recorrido del símbolo guía y cuándo entra cada frase de la voz. La voz es `v3/voz/narracion.mp3` (generada por Brian), cortada por frases. La música es la de
`public/musica.mp3`, alargada sin cambiar el tono.

```bash
node v3/render.js "$PWD/v3"           # cuadros a 30 fps en v3/cuadros/
python3 v3/sonido.py <carpeta>        # mezcla: voz, música, ambiente y efectos; luego loudnorm -16 LUFS
ffmpeg -framerate 30 -i v3/cuadros/f%05d.jpg -i mezcla_n.wav -c:v libx264 -crf 20 -pix_fmt yuv420p -c:a aac suroute_presentacion.mp4
```

## Versión anterior (Remotion)

El video ([`diseno/video/rutaalacima_presentacion.mp4`](../diseno/video/rutaalacima_presentacion.mp4), 55 s, 1920 × 1080)
está hecho con código, con [Remotion](https://www.remotion.dev) y su skill oficial para agentes
([remotion-dev/skills](https://github.com/remotion-dev/skills)). Así se puede cambiar un texto o una pantalla y volver a
generarlo igual.

## Escenas

| # | Escena | Archivo |
|---|---|---|
| 1 | El sello cae y aparece SuRoute | `src/escenas/Apertura.tsx` |
| 2 | Tu vida hasta los 120 años, en puntos | `src/escenas/Vida.tsx` |
| 3 | 7 fases × 6 ejes sobre la montaña | `src/escenas/Metodo.tsx` |
| 4 | Metas en cascada (anillos) | `src/escenas/Cascada.tsx` |
| 5 | Recorrido por la app: Mi ruta, Hoy, Metas, Aprende y Coach | `src/escenas/Recorrido.tsx` |
| 6 | Comunidad, cordadas y coach de vida | `src/escenas/Comunidad.tsx` |
| 7 | La frase sellada: el sello se parte | `src/escenas/FraseSellada.tsx` |
| 8 | SuRoute Web | `src/escenas/Web.tsx` |
| 9 | Cierre: «Tu cumbre te espera» | `src/escenas/Cierre.tsx` |

Las pantallas (`public/caps/`) son capturas reales de SuRoute Web en modo demostración. La música
(`public/musica.mp3`) es original, generada para este video.

## Editar y volver a generar

```bash
cd video
npm install
npx remotion studio                 # vista previa interactiva en el navegador
npx remotion render Presentacion out/rutaalacima.mp4 --codec=h264 --crf=20
```

Luego copia `out/rutaalacima.mp4` a `diseno/video/rutaalacima_presentacion.mp4`.
