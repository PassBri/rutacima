import type React from 'react';
import {AbsoluteFill, Easing, Img, interpolate, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {C, TEXTO, TITULO} from './tema';

/** Hoja de papel blanco con dos pliegues suaves, como en la app. */
export const Papel: React.FC<{children?: React.ReactNode; oscuro?: boolean}> = ({children, oscuro}) => (
	<AbsoluteFill
		style={{
			backgroundColor: oscuro ? C.noche : C.fondo,
			backgroundImage: oscuro
				? 'radial-gradient(ellipse at 50% 40%, #3A1E16 0%, #1B1310 70%)'
				: 'radial-gradient(ellipse at 30% 20%, #FFFFFF 0%, #F7F3EE 55%, #EFE6DC 100%)',
			fontFamily: TEXTO,
			color: oscuro ? '#F1E8DF' : C.tinta,
		}}
	>
		{!oscuro && (
			<>
				<div style={{position: 'absolute', left: '50%', top: 0, bottom: 0, width: 2, background: 'linear-gradient(90deg, rgba(122,36,24,0.05), rgba(255,255,255,0.7))'}} />
				<div style={{position: 'absolute', top: '50%', left: 0, right: 0, height: 2, background: 'linear-gradient(180deg, rgba(122,36,24,0.05), rgba(255,255,255,0.7))'}} />
			</>
		)}
		{children}
	</AbsoluteFill>
);

const suave = Easing.bezier(0.16, 1, 0.3, 1);

/** Aparece subiendo y desvaneciéndose desde `desde` (en segundos). */
export const useEntrada = (desde: number, duracion = 0.8) => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const p = interpolate(frame, [desde * fps, (desde + duracion) * fps], [0, 1], {
		extrapolateLeft: 'clamp',
		extrapolateRight: 'clamp',
		easing: suave,
	});
	return {opacity: p, translate: `0px ${(1 - p) * 40}px`} as React.CSSProperties;
};

export const Rotulo: React.FC<{children: React.ReactNode; color?: string; style?: React.CSSProperties}> = ({children, color = C.oro, style}) => (
	<div style={{fontFamily: TEXTO, fontWeight: 700, fontSize: 30, letterSpacing: 6, textTransform: 'uppercase', color, ...style}}>{children}</div>
);

export const Titular: React.FC<{children: React.ReactNode; style?: React.CSSProperties; color?: string}> = ({children, style, color = C.burdeos}) => (
	<div style={{fontFamily: TITULO, fontWeight: 700, fontSize: 112, lineHeight: 1.05, color, textWrap: 'balance', ...style}}>{children}</div>
);

export const Apoyo: React.FC<{children: React.ReactNode; style?: React.CSSProperties}> = ({children, style}) => (
	<div style={{fontFamily: TEXTO, fontSize: 48, lineHeight: 1.35, color: C.suave, textWrap: 'balance', ...style}}>{children}</div>
);

/** Teléfono con una captura real de Rutaalacima. */
export const Telefono: React.FC<{src?: string; children?: React.ReactNode; alto?: number; style?: React.CSSProperties}> = ({src, children, alto = 880, style}) => {
	const ancho = (alto * 390) / 844;
	const borde = alto * 0.022;
	return (
		<div
			style={{
				width: ancho + borde * 2,
				height: alto + borde * 2,
				padding: borde,
				borderRadius: alto * 0.07,
				background: 'linear-gradient(145deg, #3B2A23, #140D0A)',
				boxShadow: '0 50px 90px rgba(122,36,24,0.28), 0 12px 24px rgba(122,36,24,0.18), inset 0 0 0 2px rgba(255,255,255,0.08)',
				...style,
			}}
		>
			<div style={{position: 'relative', width: ancho, height: alto, borderRadius: alto * 0.055, overflow: 'hidden', backgroundColor: '#fff'}}>
				{src ? <Img src={staticFile(src)} style={{width: '100%', height: '100%', objectFit: 'cover'}} /> : children}
			</div>
		</div>
	);
};

/** El sello de cera de Ruta a la Cima. */
export const Sello: React.FC<{tam: number; style?: React.CSSProperties}> = ({tam, style}) => (
	<Img src={staticFile('sello.png')} style={{width: tam, height: tam, filter: 'drop-shadow(0 24px 40px rgba(60,10,5,0.45))', ...style}} />
);
