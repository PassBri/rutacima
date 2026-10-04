"""Genera los catálogos (bancos) que la app usa al crear metas:
indicadores por eje, metas plantilla por área, acciones multi-eje y proyectos de confluencia.
Uso: python3 bancos.py <carpeta_txt_compacto> <salida>"""
import re, json, sys, os
src, out = sys.argv[1], sys.argv[2]
def leer(pat):
    f = [x for x in os.listdir(src) if x.startswith(pat)][0]
    return open(os.path.join(src, f), encoding='utf-8').read().splitlines()
COD = {'Voluntad': 'VOL', 'Maestría': 'MAE', 'Voz': 'VOZ', 'Valor': 'VAL', 'Evolución': 'EVO', 'Trascendencia': 'TRA'}
SKIP = re.compile(r'^(\d+ (indicadores|metas|acciones)|Espacio para|Tus (metas|acciones)|Haz clic|Activa \d)', re.I)

# Indicadores
ind, eje = [], None
for l in leer('BONO_1'):
    m = re.match(r'^## (\w+[áéíóú]?\w*) \((\w+)\)', l)
    if m: eje = m.group(2); continue
    if l.startswith('#'): eje = None if not l.startswith('## ') else eje; continue
    if eje and l.strip() and not SKIP.match(l) and not l.startswith('  T:'):
        if not l.strip().endswith('.'):
            ind.append({'eje': eje, 'texto': l.strip()})
# Metas
metas, area, cur = [], None, None
for l in leer('BONO_2'):
    if l.startswith('## '): area = l[3:].strip(); continue
    if l.startswith('# '): area = None; continue
    if not area or not l.strip() or SKIP.match(l): continue
    if l.startswith('Observable:'): cur['observable'] = l.split(':', 1)[1].strip()
    elif l.startswith('Timeframe:'): cur['plazo'] = l.split(':', 1)[1].strip()
    else:
        cur = {'area': area, 'meta': l.strip(), 'observable': '', 'plazo': ''}; metas.append(cur)
# Acciones
acc, ejes = [], None
for l in leer('BOOM_5'):
    m = re.match(r'^## Acciones (.+)$', l)
    if m: ejes = [x.strip() for x in m.group(1).split('+')]; continue
    if l.startswith('#'): ejes = None; continue
    m = re.match(r'^\d+\.\s+(.+)$', l)
    if ejes and m: acc.append({'ejes': ejes, 'texto': m.group(1).strip()})
# Proyectos de confluencia
proy, p = [], None
lines = leer('BONO_3')
for i, l in enumerate(lines):
    m = re.match(r'^## Proyecto \d+: (.+)$', l)
    if m:
        p = {'nombre': m.group(1).strip(), 'descripcion': '', 'ejes': {}}; proy.append(p); continue
    if l.startswith('# '): p = None; continue
    if not p: continue
    if l.startswith('Descripción:') and i + 1 < len(lines): p['descripcion'] = lines[i + 1].strip()
    m = re.match(r'^\s*T: \w+ \((\w+)\) \| (.+)$', l)
    if m and not m.group(2).startswith('N/A'): p['ejes'][m.group(1)] = m.group(2).strip()
os.makedirs(out, exist_ok=True)
for n, d in [('indicadores', ind), ('metas', metas), ('acciones', acc), ('proyectos', proy)]:
    json.dump(d, open(os.path.join(out, n + '.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    print(n, len(d))
