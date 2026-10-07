import {Composition, Folder} from 'remotion';
import {Apertura} from './escenas/Apertura';
import {Cascada} from './escenas/Cascada';
import {Cierre} from './escenas/Cierre';
import {Comunidad} from './escenas/Comunidad';
import {FraseSellada} from './escenas/FraseSellada';
import {Metodo} from './escenas/Metodo';
import {Recorrido} from './escenas/Recorrido';
import {Vida} from './escenas/Vida';
import {Web} from './escenas/Web';
import {Presentacion} from './Presentacion';
import './tema';

const base = {width: 1920, height: 1080, fps: 30};

export const RemotionRoot: React.FC = () => (
	<>
		<Composition id="Presentacion" component={Presentacion} {...base} durationInFrames={1650} />
		<Folder name="Escenas">
			<Composition id="Apertura" component={Apertura} {...base} durationInFrames={150} />
			<Composition id="Vida" component={Vida} {...base} durationInFrames={165} />
			<Composition id="Metodo" component={Metodo} {...base} durationInFrames={180} />
			<Composition id="Cascada" component={Cascada} {...base} durationInFrames={165} />
			<Composition id="Recorrido" component={Recorrido} {...base} durationInFrames={390} />
			<Composition id="Comunidad" component={Comunidad} {...base} durationInFrames={180} />
			<Composition id="FraseSellada" component={FraseSellada} {...base} durationInFrames={165} />
			<Composition id="Web" component={Web} {...base} durationInFrames={165} />
			<Composition id="Cierre" component={Cierre} {...base} durationInFrames={210} />
		</Folder>
	</>
);
