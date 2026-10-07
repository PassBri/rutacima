import {AbsoluteFill, Easing, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {Papel, Sello, useEntrada} from '../comun';
import {C, TEXTO, TITULO} from '../tema';

/** El sello de cera cae sobre el papel y aparece el nombre. */
export const Apertura: React.FC = () => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const titulo = useEntrada(1.1);
	const sub = useEntrada(1.6);
	return (
		<Papel>
			<AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 36}}>
				<div style={{position: 'relative', width: 400, height: 400}}>
					<svg width={400} height={400} style={{position: 'absolute', inset: 0, rotate: `${interpolate(frame, [0, 5 * fps], [0, 40])}deg`,
						opacity: interpolate(frame, [0.5 * fps, 1.2 * fps], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'})}}>
						<circle cx={200} cy={200} r={190} fill="none" stroke={C.oro} strokeWidth={6} strokeDasharray="2 16" strokeLinecap="round" />
					</svg>
					<Sello
						tam={330}
						style={{
							position: 'absolute', left: 35, top: 35,
							scale: interpolate(frame, [0, 0.7 * fps], [1.8, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: Easing.spring({damping: 14}), output: 'perceptual-scale'}),
							opacity: interpolate(frame, [0, 0.35 * fps], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'}),
						}}
					/>
				</div>
				<div style={{fontFamily: TITULO, fontWeight: 700, fontSize: 150, color: C.burdeos, letterSpacing: -2, ...titulo}}>Rutaalacima</div>
				<div style={{fontFamily: TEXTO, fontSize: 52, color: C.suave, marginTop: -20, ...sub}}>La app del método <b style={{color: C.tinta, fontWeight: 500}}>Ruta a la Cima</b></div>
			</AbsoluteFill>
		</Papel>
	);
};
