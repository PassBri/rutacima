import {loadFont} from '@remotion/fonts';
import {staticFile} from 'remotion';

// Colores de Rutaalacima: papel, tinta y el burdeos del sello de cera.
export const C = {
	fondo: '#F7F3EE',
	hoja: '#FFFFFF',
	tinta: '#2B1E18',
	suave: '#6B5B52',
	linea: '#E8DED4',
	burdeos: '#6B2A1A',
	burdeosSuave: '#F3E3DC',
	oro: '#B8862F',
	oroSuave: '#F6EAD3',
	noche: '#1B1310',
};

export const EJES = [
	{nombre: 'Voluntad', color: '#8E3B26'},
	{nombre: 'Maestría', color: '#5C4A8A'},
	{nombre: 'Voz', color: '#2F6F7A'},
	{nombre: 'Valor', color: '#8A6A1F'},
	{nombre: 'Evolución', color: '#3F7A4A'},
	{nombre: 'Trascendencia', color: '#6B2A1A'},
];

export const FASES = ['Orientación', 'Preparación', 'Travesía', 'Ascenso', 'Culminación', 'Contemplación', 'Descenso'];

export const TITULO = '"Roboto Serif", Georgia, serif';
export const TEXTO = 'Roboto, system-ui, sans-serif';

const fuentes: [string, string, string, string?][] = [
	['Roboto Serif', 'fonts/roboto-serif-latin-500-normal.woff2', '500'],
	['Roboto Serif', 'fonts/roboto-serif-latin-700-normal.woff2', '700'],
	['Roboto Serif', 'fonts/roboto-serif-latin-500-italic.woff2', '500', 'italic'],
	['Roboto', 'fonts/roboto-latin-400-normal.woff2', '400'],
	['Roboto', 'fonts/roboto-latin-500-normal.woff2', '500'],
	['Roboto', 'fonts/roboto-latin-700-normal.woff2', '700'],
];
fuentes.forEach(([family, url, weight, style]) => {
	loadFont({family, url: staticFile(url), weight, style: style ?? 'normal'});
});
