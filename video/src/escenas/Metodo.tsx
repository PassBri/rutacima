import {Easing, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {Apoyo, Papel, Titular, useEntrada} from '../comun';
import {C, EJES, FASES, TEXTO} from '../tema';

// Recorrido de las 7 fases sobre la montaña (coordenadas del SVG de 1000 × 860).
const PUNTOS: [number, number][] = [[110, 780], [300, 690], [215, 560], [430, 450], [600, 150], [700, 215], [880, 470]];
const LADO = ['der', 'der', 'izq', 'der', 'izq', 'der', 'izq'];

export const Metodo: React.FC = () => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const t = useEntrada(0.1), s = useEntrada(0.35);
	const avance = interpolate(frame, [0.5 * fps, 3.6 * fps], [0, PUNTOS.length - 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: Easing.bezier(0.45, 0, 0.25, 1)});
	const tramo = Math.floor(avance), resto = avance - tramo;
	const visibles = PUNTOS.slice(0, tramo + 1);
	if (tramo < PUNTOS.length - 1) {
		const [x0, y0] = PUNTOS[tramo], [x1, y1] = PUNTOS[tramo + 1];
		visibles.push([x0 + (x1 - x0) * resto, y0 + (y1 - y0) * resto]);
	}
	return (
		<Papel>
			<div style={{position: 'absolute', left: 140, top: 170, width: 700}}>
				<Titular style={t}>7 fases<br /><span style={{color: C.oro}}>×</span> 6 ejes</Titular>
				<Apoyo style={{marginTop: 28, ...s}}>Un método para llegar a tu Cumbre Personal, paso a paso.</Apoyo>
				<div style={{marginTop: 50, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 18}}>
					{EJES.map((e, i) => {
						const p = interpolate(frame, [(1.2 + i * 0.18) * fps, (1.7 + i * 0.18) * fps], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: Easing.bezier(0.16, 1, 0.3, 1)});
						return (
							<div key={e.nombre} style={{opacity: p, scale: String(0.85 + 0.15 * p), display: 'flex', alignItems: 'center', gap: 16, padding: '16px 24px',
								borderRadius: 999, backgroundColor: '#fff', boxShadow: '0 6px 16px rgba(122,36,24,0.10)', fontFamily: TEXTO, fontSize: 38, fontWeight: 500}}>
								<span style={{width: 26, height: 26, borderRadius: 13, backgroundColor: e.color}} />{e.nombre}
							</div>
						);
					})}
				</div>
			</div>
			<svg width={1000} height={860} viewBox="0 0 1000 860" style={{position: 'absolute', right: 60, top: 130}}>
				<path d="M0 860 L340 470 L430 520 L600 110 L760 330 L830 290 L1000 520 L1000 860 Z" fill={C.burdeosSuave} />
				<path d="M600 110 L545 240 L590 215 L640 250 L660 200 Z" fill="#FFFFFF" />
				<polyline points={visibles.map((p) => p.join(',')).join(' ')} fill="none" stroke={C.burdeos} strokeWidth={8} strokeDasharray="2 18" strokeLinecap="round" strokeLinejoin="round" />
				{PUNTOS.map(([x, y], i) => {
					const p = interpolate(avance, [i - 0.15, i + 0.2], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
					const cumbre = i === 4;
					return (
						<g key={FASES[i]} opacity={p}>
							<circle cx={x} cy={y} r={cumbre ? 34 : 28} fill={cumbre ? C.oro : C.burdeos} />
							<text x={x} y={y + 11} textAnchor="middle" fontFamily={TEXTO} fontWeight={700} fontSize={30} fill="#fff">{i + 1}</text>
							<text x={LADO[i] === 'der' ? x + 46 : x - 46} y={y + 12} textAnchor={LADO[i] === 'der' ? 'start' : 'end'}
								fontFamily={TEXTO} fontWeight={500} fontSize={36} fill={C.tinta}>{FASES[i]}</text>
						</g>
					);
				})}
			</svg>
		</Papel>
	);
};
