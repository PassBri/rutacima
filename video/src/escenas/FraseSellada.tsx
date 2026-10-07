import {AbsoluteFill, Easing, Img, interpolate, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {Papel, Rotulo, useEntrada} from '../comun';
import {TEXTO, TITULO} from '../tema';

// Grieta irregular de arriba abajo (en % del sello).
const GRIETA: [number, number][] = [[52, 0], [47, 14], [55, 27], [46, 41], [53, 55], [44, 68], [51, 82], [47, 100]];
const mitad = (lado: 'izq' | 'der') =>
	`polygon(${lado === 'izq' ? '0% 0%' : '100% 0%'}, ${GRIETA.map(([x, y]) => `${x}% ${y}%`).join(', ')}, ${lado === 'izq' ? '0% 100%' : '100% 100%'})`;

/** Cada día una frase sellada: el sello se agrieta, se parte y deja leer la frase. */
export const FraseSellada: React.FC = () => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const r = useEntrada(0.1);
	const T = 520;
	const grieta = interpolate(frame, [1.0 * fps, 1.5 * fps], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
	const rompe = interpolate(frame, [1.75 * fps, 2.7 * fps], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: Easing.bezier(0.3, 0, 0.2, 1)});
	const temblor = frame > 1.0 * fps && frame < 1.75 * fps ? Math.sin(frame * 2.3) * 4 : 0;
	const frase = interpolate(frame, [2.0 * fps, 2.9 * fps], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: Easing.bezier(0.16, 1, 0.3, 1)});
	const largo = GRIETA.slice(1).reduce((s, [x, y], i) => s + Math.hypot((x - GRIETA[i][0]) * T / 100, (y - GRIETA[i][1]) * T / 100), 0);
	return (
		<Papel oscuro>
			<AbsoluteFill style={{alignItems: 'center', paddingTop: 110}}>
				<Rotulo color="#E0B566" style={r}>Cada día, una frase sellada</Rotulo>
			</AbsoluteFill>
			<AbsoluteFill style={{alignItems: 'center', justifyContent: 'center'}}>
				<div style={{width: 1240, textAlign: 'center', opacity: frase, scale: String(0.94 + 0.06 * frase)}}>
					<div style={{fontFamily: TITULO, fontStyle: 'italic', fontWeight: 500, fontSize: 92, lineHeight: 1.25, color: '#F6EAD3', textWrap: 'balance'}}>
						“Pedir ayuda no es debilidad, es sabiduría.”
					</div>
					<div style={{fontFamily: TEXTO, fontSize: 40, color: '#BBA99C', marginTop: 40}}>Se abre con tu rostro, tu huella o tu clave.</div>
				</div>
			</AbsoluteFill>
			<AbsoluteFill style={{alignItems: 'center', justifyContent: 'center'}}>
				<div style={{position: 'relative', width: T, height: T, translate: `${temblor}px 0px`}}>
					{(['izq', 'der'] as const).map((lado) => {
						const s = lado === 'izq' ? -1 : 1;
						return (
							<Img key={lado} src={staticFile('sello.png')} style={{position: 'absolute', inset: 0, width: T, height: T, clipPath: mitad(lado),
								translate: `${s * rompe * 560}px ${rompe * rompe * 260}px`, rotate: `${s * rompe * 24}deg`, opacity: 1 - rompe * 0.9,
								filter: 'drop-shadow(0 30px 50px rgba(0,0,0,0.6))'}} />
						);
					})}
					<svg width={T} height={T} style={{position: 'absolute', inset: 0, opacity: 1 - rompe}}>
						<polyline points={GRIETA.map(([x, y]) => `${(x * T) / 100},${(y * T) / 100}`).join(' ')} fill="none" stroke="#1A0805" strokeWidth={7}
							strokeLinejoin="round" strokeDasharray={`${largo * grieta} ${largo}`} />
						<polyline points={GRIETA.map(([x, y]) => `${(x * T) / 100 + 3},${(y * T) / 100}`).join(' ')} fill="none" stroke="rgba(255,190,170,0.5)" strokeWidth={2}
							strokeDasharray={`${largo * grieta} ${largo}`} />
					</svg>
				</div>
			</AbsoluteFill>
		</Papel>
	);
};
