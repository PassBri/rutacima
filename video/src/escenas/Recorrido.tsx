import {linearTiming, springTiming, TransitionSeries} from '@remotion/transitions';
import {fade} from '@remotion/transitions/fade';
import {slide} from '@remotion/transitions/slide';
import {AbsoluteFill, Img, staticFile, useVideoConfig} from 'remotion';
import {Apoyo, Papel, Rotulo, Telefono, Titular, useEntrada} from '../comun';

/** Una pantalla de la app dentro del teléfono. */
const Pantalla: React.FC<{src: string}> = ({src}) => (
	<AbsoluteFill><Img src={staticFile(src)} style={{width: '100%', height: '100%', objectFit: 'cover'}} /></AbsoluteFill>
);

/** El texto que acompaña cada pantalla. */
const Leyenda: React.FC<{rotulo: string; titulo: string; texto: string}> = ({rotulo, titulo, texto}) => {
	const a = useEntrada(0.15), b = useEntrada(0.3), c = useEntrada(0.45);
	return (
		<AbsoluteFill style={{justifyContent: 'center'}}>
			<Rotulo style={a}>{rotulo}</Rotulo>
			<Titular style={{fontSize: 104, marginTop: 20, ...b}}>{titulo}</Titular>
			<Apoyo style={{marginTop: 28, ...c}}>{texto}</Apoyo>
		</AbsoluteFill>
	);
};

const T = 15; // cuadros de cada transición

/** Recorrido por la app: el teléfono pasa de pantalla en pantalla y el texto cambia al mismo ritmo. */
export const Recorrido: React.FC = () => {
	const {fps} = useVideoConfig();
	return (
		<Papel>
			<div style={{position: 'absolute', left: 190, top: 70}}>
				<Telefono alto={900}>
					<TransitionSeries>
						<TransitionSeries.Sequence name="Mi ruta" durationInFrames={90} premountFor={fps}><Pantalla src="caps/m_vida.png" /></TransitionSeries.Sequence>
						<TransitionSeries.Transition presentation={slide({direction: 'from-right'})} timing={springTiming({config: {damping: 200}, durationInFrames: T})} />
						<TransitionSeries.Sequence name="Hoy" durationInFrames={90} premountFor={fps}><Pantalla src="caps/m_dia.png" /></TransitionSeries.Sequence>
						<TransitionSeries.Transition presentation={slide({direction: 'from-right'})} timing={springTiming({config: {damping: 200}, durationInFrames: T})} />
						<TransitionSeries.Sequence name="Metas" durationInFrames={90} premountFor={fps}><Pantalla src="caps/m_metas.png" /></TransitionSeries.Sequence>
						<TransitionSeries.Transition presentation={slide({direction: 'from-right'})} timing={springTiming({config: {damping: 200}, durationInFrames: T})} />
						<TransitionSeries.Sequence name="Aprende" durationInFrames={90} premountFor={fps}><Pantalla src="caps/m_guia.png" /></TransitionSeries.Sequence>
						<TransitionSeries.Transition presentation={slide({direction: 'from-right'})} timing={springTiming({config: {damping: 200}, durationInFrames: T})} />
						<TransitionSeries.Sequence name="Coach" durationInFrames={90} premountFor={fps}><Pantalla src="caps/m_coach.png" /></TransitionSeries.Sequence>
					</TransitionSeries>
				</Telefono>
			</div>
			<div style={{position: 'absolute', left: 820, top: 0, bottom: 0, width: 960}}>
				<TransitionSeries>
					<TransitionSeries.Sequence name="Texto Mi ruta" durationInFrames={90} premountFor={fps}>
						<Leyenda rotulo="Mi ruta" titulo="Tu vida, año por año" texto="Cada punto es un año. Las banderas marcan dónde están tus metas." />
					</TransitionSeries.Sequence>
					<TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: T})} />
					<TransitionSeries.Sequence name="Texto Hoy" durationInFrames={90} premountFor={fps}>
						<Leyenda rotulo="Hoy" titulo="Un paso cada día" texto="Tu prioridad, tus anillos de avance y 18 hábitos de los 6 ejes." />
					</TransitionSeries.Sequence>
					<TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: T})} />
					<TransitionSeries.Sequence name="Texto Metas" durationInFrames={90} premountFor={fps}>
						<Leyenda rotulo="Metas" titulo="Del sueño al plan" texto="Propósitos a los años que elijas, metas del año y del mes." />
					</TransitionSeries.Sequence>
					<TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: T})} />
					<TransitionSeries.Sequence name="Texto Aprende" durationInFrames={90} premountFor={fps}>
						<Leyenda rotulo="Aprende" titulo="24 guías y audiolibros" texto="Complétalas en la app, guarda tus respuestas o escúchalas mientras caminas." />
					</TransitionSeries.Sequence>
					<TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: T})} />
					<TransitionSeries.Sequence name="Texto Coach" durationInFrames={90} premountFor={fps}>
						<Leyenda rotulo="Coach con IA" titulo="Te conoce y te guía" texto="Sabe tu cumbre y tus metas, y te ayuda a dar el siguiente paso." />
					</TransitionSeries.Sequence>
				</TransitionSeries>
			</div>
		</Papel>
	);
};
