import json,sys
d=json.load(open(sys.argv[1]))
def show(b,ind='  '):
    t=b['type']
    if t=='callout':
        print(ind+'[CALLOUT]'); [show(x,ind+'   ') for x in b['blocks']]; return
    if t=='prompt': print(ind+f"?? PROMPT {b.get('number','')} {('['+b['title']+'] ') if b.get('title') else ''}{b['label'][:110]}")
    elif t=='scale': print(ind+f"## SCALE {b['label'][:100]}")
    elif t=='check': print(ind+f"[] {b['label'][:100]}")
    elif t=='choice': print(ind+f"() {b['label'][:60]} -> {b['options']}")
    elif t=='inputTable': print(ind+f"TT INPUT {b['headers']} rows={b['rows']} labels={b['rowLabels'][:8]}")
    elif t=='table': print(ind+f"TT {b['headers']} x{len(b['rows'])}")
    elif t=='heading': print(ind+f"H{b['level']} {b['text']}")
    else: print(ind+f"{t[:4]}: {b.get('text','')[:110]}")
for s in d['sections']:
    print('=== SECTION:',s['title'])
    for b in s['blocks']: show(b)
