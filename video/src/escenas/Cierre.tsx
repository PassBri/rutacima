import {AbsoluteFill, Easing, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {Papel, Sello, useEntrada} from '../comun';
import {C, TEXTO, TITULO} from '../tema';

/** Cierre: el sello, la invitación y dónde conseguir la app. */
export const Cierre: React.FC = () => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const t = useEntrada(0.4), s = useEntrada(0.7), b = useEntrada(1.1), f = useEntrada(1.6);
	return (
		<Papel>
			<AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', flexDirection: 'column'}}>
				<Sello tam={260} style={{scale: interpolate(frame, [0, 0.8 * fps], [0.6, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: Easing.spring({damping: 16}), output: 'perceptual-scale'}),
					opacity: interpolate(frame, [0, 0.4 * fps], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'})}} />
				<div style={{fontFamily: TITULO, fontWeight: 700, fontSize: 130, color: C.burdeos, marginTop: 30, ...t}}>Tu cumbre te espera</div>
				<div style={{fontFamily: TEXTO, fontSize: 50, color: C.suave, marginTop: 14, ...s}}>Empieza hoy con un solo paso.</div>
				<div style={{display: 'flex', gap: 28, marginTop: 56, ...b}}>
					<div style={{padding: '22px 44px', borderRadius: 999, backgroundColor: C.burdeos, color: '#fff', fontFamily: TEXTO, fontWeight: 500, fontSize: 40}}>App Android en GitHub</div>
					<div style={{padding: '22px 44px', borderRadius: 999, backgroundColor: C.oroSuave, color: C.burdeos, fontFamily: TEXTO, fontWeight: 500, fontSize: 40}}>passbri.github.io/rutacima</div>
				</div>
			</AbsoluteFill>
			<AbsoluteFill style={{justifyContent: 'flex-end', alignItems: 'center', paddingBottom: 90}}>
				<div style={{fontFamily: TEXTO, fontSize: 32, letterSpacing: 5, textTransform: 'uppercase', color: C.suave, ...f}}>Serie Ruta a la Cima · Brian Gonzalo Suárez Acevedo</div>
			</AbsoluteFill>
		</Papel>
	);
};
