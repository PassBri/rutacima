import {Easing, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {Apoyo, Papel, Rotulo, Titular, useEntrada} from '../comun';
import {C, TEXTO} from '../tema';

const ANILLOS = [
	{nombre: 'Propósito', r: 330, pct: 0.38, color: C.burdeos},
	{nombre: 'Año', r: 255, pct: 0.55, color: '#8E3B26'},
	{nombre: 'Mes', r: 180, pct: 0.68, color: C.oro},
	{nombre: 'Hoy', r: 105, pct: 0.78, color: '#3F7A4A'},
];

/** Anillos de la cascada: lo de hoy alimenta el mes, el año y el propósito. */
export const Cascada: React.FC = () => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const a = useEntrada(0.1), b = useEntrada(0.35), c = useEntrada(0.7);
	return (
		<Papel>
			<div style={{position: 'absolute', left: 140, top: 260, width: 820}}>
				<Rotulo style={a}>Metas en cascada</Rotulo>
				<Titular style={{marginTop: 24, ...b}}>Lo que haces hoy suma a tu cumbre</Titular>
				<Apoyo style={{marginTop: 30, ...c}}>Propósito → año → mes → hoy. El avance sube solo.</Apoyo>
			</div>
			<svg width={760} height={760} viewBox="-380 -380 760 760" style={{position: 'absolute', right: 170, top: 160, rotate: '-90deg'}}>
				{ANILLOS.map((an, i) => {
					const largo = 2 * Math.PI * an.r;
					// Se llena de adentro hacia afuera: primero hoy, al final el propósito.
					const inicio = (0.6 + (3 - i) * 0.35) * fps;
					const p = interpolate(frame, [inicio, inicio + 1.3 * fps], [0, an.pct], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: Easing.bezier(0.16, 1, 0.3, 1)});
					return (
						<g key={an.nombre}>
							<circle r={an.r} fill="none" stroke={C.burdeosSuave} strokeWidth={46} />
							<circle r={an.r} fill="none" stroke={an.color} strokeWidth={46} strokeLinecap="round" strokeDasharray={`${largo * p} ${largo}`} />
						</g>
					);
				})}
			</svg>
			<div style={{position: 'absolute', right: 170 + 380 + 34, top: 160, width: 0}}>
				{ANILLOS.map((an, i) => {
					const inicio = (0.6 + (3 - i) * 0.35 + 0.5) * fps;
					const o = interpolate(frame, [inicio, inicio + 0.5 * fps], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
					return (
						<div key={an.nombre} style={{position: 'absolute', top: 380 - an.r - 22, left: 0, translate: '-100% 0', whiteSpace: 'nowrap', opacity: o,
							fontFamily: TEXTO, fontWeight: 700, fontSize: 34, color: an.color === C.oro ? '#8A6A1F' : an.color}}>{an.nombre}</div>
					);
				})}
			</div>
		</Papel>
	);
};
