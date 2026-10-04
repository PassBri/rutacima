"""Convierte los workbooks .docx de Ruta a la Cima en JSON estructurado para la app Android.

Uso: python3 convert.py <carpeta_docx> <carpeta_salida>
"""
import json, re, sys, os, glob
from docx import Document

W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'

MARKER = re.compile(r'^(escriba aqu[ií]|_{3,}|haz clic.*)$', re.I)
NUM_PROMPT = re.compile(r'^[☐◻]\s*(\d{1,2})\.?\s*(.*)$')
SCALE = re.compile(r'^(.*?)(?:_{0,}\s*)/\s*10\s*$')
SECTION_KW = re.compile(r'^(PASO|PARTE|PROTOCOLO|HERRAMIENTA|EXCAVACI[ÓO]N|FASE|EJE\b|PORTAL|CASO|ANEXO|BONO|S[ÍI]NTESIS|INTRODUCCI[ÓO]N|CIERRE|COMPROMISO|REFLEXI[ÓO]N FINAL|VISUALIZACI[ÓO]N|LEE ESTO|[ÍI]NDICE|BIENVENIDA)\b', re.I)


def ptext(p):
    parts = []
    for el in p.iter():
        if el.tag == W + 't':
            parts.append(el.text or '')
        elif el.tag in (W + 'br', W + 'cr'):
            parts.append('\n')
        elif el.tag == W + 'tab':
            parts.append(' ')
    return ''.join(parts).replace('\xa0', ' ').strip()


def pstyle(p, styles):
    ps = p.find(W + 'pPr/' + W + 'pStyle')
    if ps is None:
        return ''
    return styles.get(ps.get(W + 'val'), ps.get(W + 'val'))


def is_bullet(p):
    return p.find(W + 'pPr/' + W + 'numPr') is not None


def cell_texts(tc):
    """All paragraph texts of a cell (deep)."""
    return [ptext(p) for p in tc.iter(W + 'p')]


def cell_is_empty(tc):
    return all((not t) or MARKER.match(t) for t in cell_texts(tc))


def cell_plain(tc):
    return ' '.join(t for t in cell_texts(tc) if t and not MARKER.match(t)).strip()


def is_caps_heading(t):
    if len(t) > 70 or len(t) < 3 or t[0] in '☐◻✓•"“-':
        return False
    letters = [c for c in t if c.isalpha()]
    if len(letters) < 3:
        return False
    up = sum(1 for c in letters if c.isupper())
    return up / len(letters) > 0.85


class Converter:
    def __init__(self, wid, styles):
        self.wid = wid
        self.styles = styles
        self.n = 0
        self.stack = []

    def nid(self, kind):
        self.n += 1
        return f'{self.wid}.{kind}{self.n}'

    # ------------------------------------------------------------------ paragraphs
    def add_text(self, blocks, t, style='', bullet=False):
        if not t:
            return
        if '\n' in t:
            for line in t.split('\n'):
                self.add_text(blocks, line.strip(), style, bullet)
            return
        mscale = re.match(r'^(.+?):?\s+1\s+2\s+3\s+4\s+5\s+6\s+7\s+8\s+9\s+10', t)
        if mscale:
            blocks.append({'type': 'scale', 'id': self.nid('s'), 'label': mscale.group(1).strip(' :').title(), 'min': 1, 'max': 10})
            return
        if re.match(r'^(VOL|MAE|VOZ|VAL|EVO|TRA) \(.+\):$', t):
            blocks.append({'type': 'scale', 'id': self.nid('s'), 'label': t.rstrip(':'), 'min': 1, 'max': 10})
            return
        if MARKER.match(t):
            self.on_marker(blocks)
            return
        if re.fullmatch(r'\d{1,2}', t):  # números decorativos de portada de sección
            return
        if t in ('[]', 'rr') or 'Espacio para el texto' in t or re.match(r'^(Firma|FIRMA)\s*:', t):
            return
        if self.stack and re.match(r'^[A-ZÁÉÍÓÚÑ][A-ZÁÉÍÓÚÑ \-]{3,40}\s+[-–]\s+\S', t) and len(t) < 140:
            blocks.append({'type': 'check', 'id': self.nid('k'), 'label': t})
            return
        m = NUM_PROMPT.match(t)
        if m:
            num, label = m.group(1), m.group(2).strip()
            blocks.append({'type': 'prompt', 'id': self.nid('q'), 'number': num,
                           'label': label})
            return
        if t.count('☐') + t.count('◻') >= 2 and len(t) < 160:
            parts = [x.strip(' /') for x in re.split(r'[☐◻]', t)]
            label = parts[0]
            opts = [o for o in parts[1:] if o]
            # ☐ SÍ ☐ NO - Lo que estoy haciendo...
            last = opts[-1] if opts else ''
            mm = re.match(r'^(NO)\s*[-–]\s*(.+)$', last)
            if mm:
                opts[-1] = mm.group(1)
                label = (label + ' ' + mm.group(2)).strip()
            blocks.append({'type': 'choice', 'id': self.nid('c'), 'label': label, 'options': opts})
            return
        if t[0] in '☐◻':
            blocks.append({'type': 'check', 'id': self.nid('k'), 'label': t[1:].strip()})
            return
        ms = SCALE.match(t)
        if ms and len(t) < 160:
            label = ms.group(1).strip(' :_')
            if not label and blocks and blocks[-1]['type'] in ('paragraph', 'prompt', 'bullet'):
                prev = blocks.pop() if blocks[-1]['type'] != 'prompt' else blocks[-1]
                label = prev.get('label') or prev.get('text')
            blocks.append({'type': 'scale', 'id': self.nid('s'), 'label': label or 'Puntaje', 'min': 1, 'max': 10})
            return
        if style.startswith('Heading') or style == 'Title':
            lvl = int(style[-1]) if style[-1].isdigit() else 1
            t2 = t.lstrip('#•\t ').strip()
            blocks.append({'type': 'heading', 'level': min(lvl, 3), 'text': t2})
            return
        if t[0] in '"“' and len(t) < 260:
            blocks.append({'type': 'quote', 'text': t.strip('"“”')})
            return
        if t[0] in '✓✗•→-' or bullet:
            blocks.append({'type': 'bullet', 'text': t.lstrip('✓✗•→- \t').strip(), 'mark': t[0] if t[0] in '✓✗→' else '•'})
            return
        if is_caps_heading(t):
            blocks.append({'type': 'heading', 'level': 2, 'text': t})
            return
        blocks.append({'type': 'paragraph', 'text': t})

    def on_marker(self, blocks):
        if not blocks:
            for parent in reversed(self.stack):
                if parent is not blocks and parent:
                    blocks = parent
                    break
        if not blocks:
            blocks.append({'type': 'prompt', 'id': self.nid('q'), 'label': '', '_closed': True})
            return
        last = blocks[-1]
        if last['type'] == 'prompt' and not last.get('_closed'):
            last['_closed'] = True
            return
        if last['type'] in ('paragraph', 'bullet', 'heading', 'quote') or (last['type'] == 'prompt'):
            if last['type'] == 'prompt':
                # segundo campo consecutivo -> nuevo prompt sin etiqueta
                blocks.append({'type': 'prompt', 'id': self.nid('q'), 'label': '', '_closed': True})
                return
            if last['type'] == 'heading':
                blocks.append({'type': 'prompt', 'id': self.nid('q'), 'label': '', '_closed': True})
                return
            blocks.pop()
            title = None
            if last['text'].startswith('(') and blocks and blocks[-1]['type'] in ('paragraph', 'bullet'):
                title = blocks.pop()['text']
            elif blocks and blocks[-1]['type'] == 'paragraph' and len(blocks[-1]['text']) < 45 \
                    and not blocks[-1]['text'].endswith(('?', '.', ':')) and '?' in last['text']:
                title = blocks.pop()['text']
            pr = {'type': 'prompt', 'id': self.nid('q'), 'label': last['text'], '_closed': True}
            if title:
                pr['title'] = title
            blocks.append(pr)
        else:
            blocks.append({'type': 'prompt', 'id': self.nid('q'), 'label': '', '_closed': True})

    # ------------------------------------------------------------------ tables
    def table(self, tbl, blocks):
        rows = [[tc for tc in tr.iterchildren(W + 'tc')] for tr in tbl.iterchildren(W + 'tr')]
        rows = [r for r in rows if r]
        if not rows:
            return
        ncols = max(len(r) for r in rows)
        if all(cell_is_empty(c) for r in rows for c in r):
            self.on_marker(blocks)
            return
        has_nested = tbl.find('.//' + W + 'tc//' + W + 'tbl') is not None
        texts = [[cell_plain(c) for c in r] for r in rows]
        empties = [[cell_is_empty(c) for c in r] for r in rows]
        has_checkbox = any('☐' in x or '◻' in x for r in texts for x in r)
        has_numprompt = any(NUM_PROMPT.match(x) for r in texts for x in r)

        # 1) Tabla de lectura (todas las celdas con texto, sin anidados)
        if all(re.match(r'^(VOL|MAE|VOZ|VAL|EVO|TRA) \(.+\):$', x) for r in texts for x in r if x):
            for r in texts:
                for x in r:
                    if x:
                        self.add_text(blocks, x)
            return
        if ncols >= 2 and len(rows) >= 2 and not has_nested and not has_checkbox \
                and all(not e for r in empties for e in r) and not any('/10' in x for r in texts for x in r):
            blocks.append({'type': 'table', 'headers': texts[0], 'rows': texts[1:]})
            return
        # 2) Tabla de captura: encabezado con texto + filas con celdas vacías
        if ncols >= 2 and len(rows) >= 2 and not has_nested and not has_numprompt \
                and all(not e for e in empties[0]) and any(any(e) for e in empties[1:]):
            headers = texts[0]
            body = rows[1:]
            row_labels = []
            first_col_labels = all(not empties[i + 1][0] for i in range(len(body))) and len(body) > 0
            if first_col_labels:
                row_labels = [texts[i + 1][0] for i in range(len(body))]
            # filas tipo "Pregunta | ☐/vacío" => checks
            if ncols == 2 and first_col_labels and all(empties[i + 1][1] for i in range(len(body))) \
                    and not any('/10' in h for h in headers) and headers[1] == '':
                for lab in row_labels:
                    blocks.append({'type': 'check', 'id': self.nid('k'), 'label': lab})
                return
            score_col = [i for i, h in enumerate(headers) if re.search(r'1-10|PUNTAJE|PUNTUACI|Puntaje', h)]
            if row_labels and score_col and score_col[-1] == ncols - 1:
                for i in range(len(body)):
                    mid = [texts[i + 1][j] for j in range(1, ncols - 1)]
                    mid_empty = [empties[i + 1][j] for j in range(1, ncols - 1)]
                    lab = row_labels[i]
                    q = ' '.join(m for m in mid if m and '/10' not in m)
                    if any(mid_empty) or (q and q.startswith('¿Cómo está bloqueando')):
                        hint = q or (headers[1] if ncols > 2 else '')
                        blocks.append({'type': 'prompt', 'id': self.nid('q'), 'title': lab, 'label': hint})
                        blocks.append({'type': 'scale', 'id': self.nid('s'), 'label': lab, 'min': 1, 'max': 10})
                    else:
                        blocks.append({'type': 'scale', 'id': self.nid('s'), 'label': (lab + ' · ' + q) if q else lab, 'min': 1, 'max': 10})
                return
            blocks.append({'type': 'inputTable', 'id': self.nid('t'), 'headers': headers,
                           'rowLabels': row_labels, 'rows': max(len(body), 3) if not row_labels else len(body)})
            return
        # 3) Filas "texto | vacío" sin encabezado (patrones de autosabotaje)
        if ncols == 2 and not has_nested and len(rows) == 1 and not empties[0][0] and empties[0][1] \
                and not has_checkbox and len(texts[0][0]) < 160:
            blocks.append({'type': 'check', 'id': self.nid('k'), 'label': texts[0][0]})
            return
        # 4) Contenedor: procesar contenido; caja 1x1 => callout
        single = len(rows) == 1 and ncols == 1
        target = []
        self.stack.append(blocks)
        for r in rows:
            for c in r:
                self.container(c, target)
        self.stack.pop()
        if single and target and not any(b['type'] in ('prompt',) for b in target[1:2]) and \
                all(b['type'] in ('paragraph', 'bullet', 'quote', 'heading') for b in target) and len(target) > 1:
            blocks.append({'type': 'callout', 'blocks': target})
        elif single and len(target) == 1 and target[0]['type'] in ('paragraph',):
            txt = target[0]['text']
            if len(txt) < 50 and not txt.endswith('.'):
                blocks.append({'type': 'heading', 'level': 3, 'text': txt})
            else:
                blocks.append({'type': 'callout', 'blocks': target})
        else:
            blocks.extend(target)

    def container(self, el, blocks):
        for child in el.iterchildren():
            tag = child.tag.replace(W, '')
            if tag == 'p':
                self.add_text(blocks, ptext(child), pstyle(child, self.styles), is_bullet(child))
            elif tag == 'tbl':
                self.table(child, blocks)
            elif tag == 'sdt':
                c = child.find(W + 'sdtContent')
                if c is not None:
                    self.container(c, blocks)


def clean(blocks):
    out = []
    for b in blocks:
        b.pop('_closed', None)
        if b['type'] == 'callout':
            b['blocks'] = clean(b['blocks'])
        # deduplicar consecutivos idénticos (las tablas índice se repiten)
        if out and {k: v for k, v in out[-1].items() if k != 'id'} == {k: v for k, v in b.items() if k != 'id'}:
            continue
        out.append(b)
    return out


def sectionize(blocks, wid):
    sections = []
    cur = {'id': f'{wid}.sec0', 'title': 'Inicio', 'blocks': []}
    for b in blocks:
        start = b['type'] == 'heading' and (b['level'] == 1 or (b['level'] == 2 and SECTION_KW.match(b['text']) and len(b['text']) > 6))
        if start and cur['blocks']:
            sections.append(cur)
            cur = {'id': f'{wid}.sec{len(sections)}', 'title': b['text'].title() if b['text'].isupper() else b['text'], 'blocks': []}
            continue
        if start:
            cur['title'] = b['text'].title() if b['text'].isupper() else b['text']
            continue
        cur['blocks'].append(b)
    if cur['blocks']:
        sections.append(cur)
    # fusionar secciones muy pequeñas sin campos con la siguiente
    merged = []
    for s in sections:
        if merged and len(merged[-1]['blocks']) <= 1 and not any(x['type'] not in ('paragraph', 'quote') for x in merged[-1]['blocks']):
            prev = merged.pop()
            s['blocks'] = ([{'type': 'heading', 'level': 2, 'text': prev['title']}] if prev['title'] != 'Inicio' else []) + prev['blocks'] + s['blocks']
            if prev['title'] != 'Inicio' and s['title'] == 'Inicio':
                s['title'] = prev['title']
        merged.append(s)
    for i, s in enumerate(merged):
        s['id'] = f'{wid}.sec{i}'
    return merged


def convert(path, meta):
    doc = Document(path)
    styles = {s.style_id: s.name for s in doc.styles}
    c = Converter(meta['id'], styles)
    blocks = []
    c.container(doc.element.body, blocks)
    blocks = clean(blocks)
    secs = sectionize(blocks, meta['id'])
    data = dict(meta)
    data['sections'] = secs
    return data


# ------------------------------------------------------------------ catálogo
CATALOG = [
    # (patrón de archivo, id, título, subtítulo, categoría, orden)
    ('DESCUBRE_TU_CUMBRE', 'descubre', 'Descubre tu Cumbre Personal', 'Secuencia didáctica · 7 pasos', 'ruta', 1),
    ('LOS_6_EJES', 'seis_ejes', 'Los 6 Ejes de tu Cumbre', 'Workbook didáctico', 'ruta', 2),
    ('VIAJE_TRANSFORMATIVO', 'viaje', 'El Viaje Transformativo', 'Guía de reflexión para las 7 fases', 'ruta', 3),
    ('DIAGN_STICO', 'diagnostico', 'Diagnóstico Personal', 'Antes de ascender, conócete a ti mismo', 'ruta', 4),
    ('CONFLUENCIA', 'confluencia', 'Confluencia', 'Cómo integrar los 6 ejes', 'ruta', 5),
    ('PORTALES', 'portales', 'Portales y Transiciones', 'Rituales para cruzar de fase', 'ruta', 6),
    ('DEL_PROP', 'proposito_valor', 'Del Propósito al Valor', 'Monetización ética de tu cumbre', 'ruta', 7),
    ('CAMPAMENTO_BASE', 'campamento', 'Campamento Base', 'Mentores, accountability y red de apoyo', 'ruta', 8),
    ('DESDE_LA_CIMA', 'desde_cima', 'Desde la Cima', 'Celebrar, integrar y descender', 'ruta', 9),
    ('KIT_DE_EMERGENCIA', 'kit', 'Kit de Emergencia', '7 herramientas de uso diario', 'herramientas', 10),
    ('COMPA_ERO', 'companero', 'Compañero de Ascenso', 'Relaciones desde la plenitud', 'ruta', 11),
    ('GU_A_DE_CA', 'caidas', 'Guía de Caídas', 'Aprender a leer la montaña', 'ruta', 12),
    ('NIEBLA', 'niebla', 'Cuando te pierdes en la niebla', '8 protocolos de reorientación', 'herramientas', 13),
    ('MANUAL_PARA_FACILITADORES', 'facilitadores', 'Manual para Facilitadores', 'Talleres, dinámicas y certificación', 'facilitador', 14),
    ('BONO_1', 'bono_indicadores', 'Banco de Indicadores', 'Bono 1 · 150 indicadores por eje', 'bonos', 15),
    ('BONO_2', 'bono_metas', 'Banco de Metas', 'Bono 2 · 80+ metas para adaptar', 'bonos', 16),
    ('BONO_3', 'bono_confluencia', 'Plantillas de Confluencia', 'Bono 3 · 10 proyectos multi-eje', 'bonos', 17),
    ('BONO_4', 'bono_cierre', 'Checklist de Cierre Mensual', 'Bono 4 · 20-30 minutos al mes', 'bonos', 18),
]

if __name__ == '__main__':
    src, out = sys.argv[1], sys.argv[2]
    os.makedirs(out, exist_ok=True)
    index = []
    for pat, wid, title, sub, cat, order in CATALOG:
        files = [f for f in glob.glob(os.path.join(src, '*.docx')) if pat in os.path.basename(f)]
        if not files:
            print('FALTA', pat)
            continue
        meta = {'id': wid, 'title': title, 'subtitle': sub, 'category': cat, 'order': order}
        data = convert(files[0], meta)
        with open(os.path.join(out, wid + '.json'), 'w', encoding='utf-8') as fh:
            json.dump(data, fh, ensure_ascii=False, indent=1)
        def count(blocks):
            return sum(count(b['blocks']) if b['type'] == 'callout' else (1 if b['type'] in ('prompt', 'scale', 'check', 'choice', 'inputTable') else 0) for b in blocks)
        n_inputs = sum(count(s['blocks']) for s in data['sections'])
        index.append({**meta, 'sections': len(data['sections']), 'inputs': n_inputs})
        print(f'{wid:18s} secciones={len(data["sections"]):3d} campos={n_inputs}')
    # Workbooks curados a mano (no vienen de un .docx): se conservan en el índice.
    generados = {i['id'] for i in index}
    for f in sorted(glob.glob(os.path.join(out, '*.json'))):
        wid = os.path.basename(f)[:-5]
        if wid == 'index' or wid in generados:
            continue
        d = json.load(open(f, encoding='utf-8'))
        def count(blocks):
            return sum(count(b['blocks']) if b['type'] == 'callout' else (1 if b['type'] in ('prompt', 'scale', 'check', 'choice', 'inputTable') else 0) for b in blocks)
        index.append({k: d[k] for k in ('id', 'title', 'subtitle', 'category', 'order')} | {'sections': len(d['sections']), 'inputs': sum(count(s['blocks']) for s in d['sections'])})
        print(f'{wid:18s} (curado a mano)')
    index.sort(key=lambda i: i['order'])
    with open(os.path.join(out, 'index.json'), 'w', encoding='utf-8') as fh:
        json.dump(index, fh, ensure_ascii=False, indent=1)
