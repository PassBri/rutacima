import {Easing, Img, interpolate, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {Apoyo, Papel, Rotulo, Telefono, Titular, useEntrada} from '../comun';
import {C} from '../tema';

/** La misma app en el computador, vinculada con un código QR. */
export const Web: React.FC = () => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const a = useEntrada(0.1), b = useEntrada(0.3), c = useEntrada(0.55);
	const ventana = interpolate(frame, [0.2 * fps, 1.2 * fps], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: Easing.bezier(0.16, 1, 0.3, 1)});
	const tel = interpolate(frame, [0.9 * fps, 1.8 * fps], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: Easing.bezier(0.16, 1, 0.3, 1)});
	return (
		<Papel>
			<div style={{position: 'absolute', left: 140, top: 250, width: 640}}>
				<Rotulo style={a}>Rutaalacima Web</Rotulo>
				<Titular style={{marginTop: 24, fontSize: 88, ...b}}>La misma app en tu computador</Titular>
				<Apoyo style={{marginTop: 30, ...c}}>Escanea un código y tus datos aparecen en los dos lados.</Apoyo>
			</div>
			<div style={{position: 'absolute', left: 860, top: 170, width: 960, borderRadius: 18, overflow: 'hidden', backgroundColor: '#fff', opacity: ventana,
				translate: `${(1 - ventana) * 80}px 0px`, boxShadow: '0 50px 100px rgba(122,36,24,0.25), 0 10px 24px rgba(122,36,24,0.12)'}}>
				<div style={{height: 44, backgroundColor: C.burdeos, display: 'flex', alignItems: 'center', gap: 10, paddingLeft: 20}}>
					{['#F0B3A1', '#E0B566', '#BBA99C'].map((col) => <span key={col} style={{width: 14, height: 14, borderRadius: 7, backgroundColor: col}} />)}
				</div>
				<Img src={staticFile('caps/d_ruta.png')} style={{width: 960, display: 'block', scale: String(interpolate(frame, [0, 5.5 * fps], [1, 1.05])), transformOrigin: '30% 30%'}} />
			</div>
			<Telefono src="caps/m_ruta.png" alto={560} style={{position: 'absolute', left: 800, top: 470, opacity: tel, translate: `0px ${(1 - tel) * 100}px`}} />
		</Papel>
	);
};
