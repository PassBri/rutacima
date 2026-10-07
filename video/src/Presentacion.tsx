import {Audio} from '@remotion/media';
import {linearTiming, TransitionSeries} from '@remotion/transitions';
import {fade} from '@remotion/transitions/fade';
import {slide} from '@remotion/transitions/slide';
import {interpolate, staticFile, useVideoConfig} from 'remotion';
import {Apertura} from './escenas/Apertura';
import {Cascada} from './escenas/Cascada';
import {Cierre} from './escenas/Cierre';
import {Comunidad} from './escenas/Comunidad';
import {FraseSellada} from './escenas/FraseSellada';
import {Metodo} from './escenas/Metodo';
import {Recorrido} from './escenas/Recorrido';
import {Vida} from './escenas/Vida';
import {Web} from './escenas/Web';
import './tema';

export const T = 15;

/** Video de presentación de Rutaalacima (55 s). */
export const Presentacion: React.FC = () => {
	const {fps, durationInFrames} = useVideoConfig();
	return (
		<>
			<TransitionSeries>
				<TransitionSeries.Sequence name="Apertura" durationInFrames={150} premountFor={fps}><Apertura /></TransitionSeries.Sequence>
				<TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: T})} />
				<TransitionSeries.Sequence name="Vida" durationInFrames={165} premountFor={fps}><Vida /></TransitionSeries.Sequence>
				<TransitionSeries.Transition presentation={slide({direction: 'from-bottom'})} timing={linearTiming({durationInFrames: T})} />
				<TransitionSeries.Sequence name="Método" durationInFrames={180} premountFor={fps}><Metodo /></TransitionSeries.Sequence>
				<TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: T})} />
				<TransitionSeries.Sequence name="Cascada" durationInFrames={165} premountFor={fps}><Cascada /></TransitionSeries.Sequence>
				<TransitionSeries.Transition presentation={slide({direction: 'from-right'})} timing={linearTiming({durationInFrames: T})} />
				<TransitionSeries.Sequence name="Recorrido" durationInFrames={390} premountFor={fps}><Recorrido /></TransitionSeries.Sequence>
				<TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: T})} />
				<TransitionSeries.Sequence name="Comunidad" durationInFrames={180} premountFor={fps}><Comunidad /></TransitionSeries.Sequence>
				<TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: T})} />
				<TransitionSeries.Sequence name="Frase sellada" durationInFrames={165} premountFor={fps}><FraseSellada /></TransitionSeries.Sequence>
				<TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: T})} />
				<TransitionSeries.Sequence name="Web" durationInFrames={165} premountFor={fps}><Web /></TransitionSeries.Sequence>
				<TransitionSeries.Transition presentation={fade()} timing={linearTiming({durationInFrames: T})} />
				<TransitionSeries.Sequence name="Cierre" durationInFrames={210} premountFor={fps}><Cierre /></TransitionSeries.Sequence>
			</TransitionSeries>
			<Audio src={staticFile('musica.mp3')} volume={(f) => interpolate(f, [durationInFrames - 2 * fps, durationInFrames], [0.8, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'})} />
		</>
	);
};
