import {Easing, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {Apoyo, Papel, Rotulo, Titular, useEntrada} from '../comun';
import {C} from '../tema';

const EDAD = 36;
const METAS = [38, 40, 45];

/** 120 puntos, uno por año: los vividos se llenan, el actual brilla y las metas llevan bandera. */
export const Vida: React.FC = () => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const a = useEntrada(0.1), b = useEntrada(0.35), c = useEntrada(0.6);
	const lleno = interpolate(frame, [0.6 * fps, 2.6 * fps], [0, EDAD], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: Easing.bezier(0.4, 0, 0.2, 1)});
	const pulso = 1 + 0.12 * Math.sin((frame / fps) * Math.PI * 2) * (frame > 2.6 * fps ? 1 : 0);
	return (
		<Papel>
			<div style={{position: 'absolute', left: 140, top: 250, width: 760}}>
				<Rotulo style={a}>Tu vida en puntos</Rotulo>
				<Titular style={{marginTop: 24, ...b}}>Tu vida hasta los 120 años</Titular>
				<Apoyo style={{marginTop: 30, ...c}}>vista año por año y día por día.</Apoyo>
			</div>
			<div style={{position: 'absolute', right: 150, top: 120, display: 'grid', gridTemplateColumns: 'repeat(10, 60px)', gap: 14}}>
				{Array.from({length: 120}, (_, i) => {
					const vivido = i < Math.floor(lleno);
					const actual = i === EDAD && lleno >= EDAD - 0.01;
					const aparece = interpolate(frame, [i * 0.4, i * 0.4 + 8], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
					const bandera = METAS.includes(i) && frame > 2.8 * fps;
					return (
						<div key={i} style={{position: 'relative', width: 60, height: 60, borderRadius: 30, opacity: aparece,
							backgroundColor: actual ? C.oro : vivido ? C.burdeos : '#FFFFFF',
							border: vivido || actual ? 'none' : `2px solid ${C.burdeosSuave}`,
							scale: actual ? String(pulso) : '1',
							boxShadow: actual ? '0 0 0 8px rgba(184,134,47,0.25)' : vivido ? '0 3px 6px rgba(122,36,24,0.2)' : 'none'}}>
							{bandera && <div style={{position: 'absolute', top: -14, right: -4, width: 22, height: 16, backgroundColor: C.oro, clipPath: 'polygon(0 0,100% 30%,0 60%)', borderLeft: `3px solid ${C.oro}`}} />}
						</div>
					);
				})}
			</div>
		</Papel>
	);
};
