import {Easing, interpolate, useCurrentFrame, useVideoConfig} from 'remotion';
import {Apoyo, Papel, Rotulo, Telefono, Titular, useEntrada} from '../comun';
import {C, TEXTO} from '../tema';

const PUNTOS = ['Comunidad para compartir logros', 'Cordadas: retos de 3 a 6 personas', 'Coach de vida que te acompaña'];

/** Tres teléfonos en abanico: comunidad, cordadas y mensajes. */
export const Comunidad: React.FC = () => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const a = useEntrada(0.1), b = useEntrada(0.3), c = useEntrada(1.8);
	const abanico = (desde: number) => interpolate(frame, [desde * fps, (desde + 0.9) * fps], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: Easing.bezier(0.16, 1, 0.3, 1)});
	const p1 = abanico(0.2), p2 = abanico(0.4), p3 = abanico(0.6);
	return (
		<Papel>
			<div style={{position: 'absolute', left: 140, top: 230, width: 720}}>
				<Rotulo style={a}>Comunidad</Rotulo>
				<Titular style={{marginTop: 24, ...b}}>No subes solo</Titular>
				<div style={{marginTop: 44, display: 'flex', flexDirection: 'column', gap: 26}}>
					{PUNTOS.map((t, i) => {
						const o = interpolate(frame, [(0.9 + i * 0.25) * fps, (1.4 + i * 0.25) * fps], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
						return (
							<div key={t} style={{opacity: o, translate: `${(1 - o) * -30}px 0px`, display: 'flex', alignItems: 'center', gap: 20, fontFamily: TEXTO, fontSize: 44, color: C.tinta}}>
								<span style={{width: 18, height: 18, borderRadius: 9, backgroundColor: C.oro, flex: 'none'}} />{t}
							</div>
						);
					})}
				</div>
				<Apoyo style={{marginTop: 40, fontSize: 38, ...c}}>Con reportes, bloqueos y límites contra el spam.</Apoyo>
			</div>
			<div style={{position: 'absolute', left: 900, top: 150, width: 960, height: 820}}>
				<Telefono src="caps/m_cordada.png" alto={720} style={{position: 'absolute', left: 30, top: 70, rotate: `${-9 * p1}deg`, opacity: p1, translate: `${(1 - p1) * 200}px 0px`}} />
				<Telefono src="caps/m_mensajes.png" alto={720} style={{position: 'absolute', left: 590, top: 70, rotate: `${9 * p3}deg`, opacity: p3, translate: `${(1 - p3) * -200}px 0px`}} />
				<Telefono src="caps/m_cimas.png" alto={780} style={{position: 'absolute', left: 300, top: 10, opacity: p2, translate: `0px ${(1 - p2) * 120}px`}} />
			</div>
		</Papel>
	);
};
