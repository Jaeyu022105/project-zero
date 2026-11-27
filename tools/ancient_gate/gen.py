import json, os, sys, glob
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from ring import ring, SIZES
root = sys.argv[1]
A = os.path.join(root, 'src/main/resources/assets/projectzero')
J = os.path.join(root, 'src/main/java/com/projectzero/gates')
MAT = {
 'stone':        ('minecraft:block/stone_bricks', 'minecraft:block/chiseled_stone_bricks', 'minecraft:block/cracked_stone_bricks'),
 'mossy':        ('minecraft:block/mossy_stone_bricks', 'minecraft:block/chiseled_stone_bricks', 'minecraft:block/cracked_stone_bricks'),
 'sandstone':    ('minecraft:block/cut_sandstone', 'minecraft:block/chiseled_sandstone', 'minecraft:block/sandstone'),
 'red_sandstone':('minecraft:block/cut_red_sandstone', 'minecraft:block/chiseled_red_sandstone', 'minecraft:block/red_sandstone'),
 'mud':          ('minecraft:block/mud_bricks', 'minecraft:block/chiseled_tuff_bricks', 'minecraft:block/packed_mud'),
 'deepslate':    ('minecraft:block/deepslate_bricks', 'minecraft:block/chiseled_deepslate', 'minecraft:block/cracked_deepslate_bricks'),
}

def runs(m):
    out = []
    for j in range(4):
        i = 0
        while i < 4:
            if m >> (j * 4 + i) & 1:
                s = i
                while i < 4 and m >> (j * 4 + i) & 1:
                    i += 1
                out.append([s * 4, j * 4, i * 4, j * 4 + 4])
            else:
                i += 1
    merged = []
    for b in out:
        for mb in merged:
            if mb[0] == b[0] and mb[2] == b[2] and mb[3] == b[1]:
                mb[3] = b[3]
                break
        else:
            merged.append(list(b))
    return merged

pieces = {}
table = {}
for size, (pw, ph) in SIZES.items():
    table[size] = []
    for u, h, m, key in ring(pw, ph):
        name = ('k_' if key else 'm_') + format(m, '04x')
        pieces[name] = (runs(m), (3, 13) if key else (4, 12), key)
        table[size].append((u, h, name))
PLATES = {'plate_high': 4, 'plate_low': 2}
pieces['rubble'] = ([[2, 0, 13, 6], [5, 6, 10, 9]], (3, 12), False)
ALL = ['north', 'south', 'east', 'west', 'up', 'down']

def glow(lit):
    return {'forge_data': {'block_light': 15, 'sky_light': 15}} if lit else {}

def zrange(name, i, z0, z1):
    return (5, 10) if name == 'rubble' and i == 1 else (z0, z1)

def model(name, mat, lit):
    base, keytex, cracked = MAT[mat]
    if name.startswith('plate_'):
        top = PLATES[name]
        stone = base if lit else cracked
        runes = 'projectzero:block/ancient_runes'
        els = [{'from': [0, 0, 0], 'to': [16, top, 16], 'faces': {f: {'texture': '#stone'} for f in ALL}}]
    elif name == 'seal':
        stone = base
        runes = 'projectzero:block/ancient_seal'
        els = [{'from': [0, 0, 0], 'to': [16, 1, 16], 'faces': {f: {'texture': '#stone'} for f in ALL}},
               {'from': [0, 0, 0], 'to': [16, 1.05, 16], 'shade': False,
                'faces': {'up': dict({'texture': '#runes', 'tintindex': 0}, **glow(lit))}}]
    else:
        boxes, (z0, z1), key = pieces[name]
        stone = keytex if key else (base if lit or name == 'rubble' else cracked)
        runes = 'projectzero:block/ancient_sigil' if key else 'projectzero:block/ancient_runes'
        els = []
        for i, b in enumerate(boxes):
            za, zb = zrange(name, i, z0, z1)
            els.append({'from': [b[0], b[1], za], 'to': [b[2], b[3], zb], 'faces': {f: {'texture': '#stone'} for f in ALL}})
        for i, b in enumerate(boxes):
            if name == 'rubble' and i == 1:
                continue
            e = 0.02
            els.append({'from': [b[0], b[1], z0 - e], 'to': [b[2], b[3], z1 + e], 'shade': False,
                        'faces': {f: dict({'texture': '#runes', 'tintindex': 0}, **glow(lit)) for f in ('north', 'south')}})
    return {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout', 'ambientocclusion': False,
            'textures': {'stone': stone, 'particle': stone, 'runes': runes}, 'elements': els}

mdir = os.path.join(A, 'models/block/ancient_gate_stone')
os.makedirs(mdir, exist_ok=True)
for f in glob.glob(mdir + '/*.json'):
    os.remove(f)
names = ['rubble', 'seal', 'plate_high', 'plate_low'] + sorted(n for n in pieces if n != 'rubble')
variants = {}
for n in names:
    for mat in MAT:
        for lit in (True, False):
            mn = f'{n}_{mat}_{"lit" if lit else "dark"}'
            with open(os.path.join(mdir, mn + '.json'), 'w') as fh:
                json.dump(model(n, mat, lit), fh, indent=1)
            for axis in ('x', 'z'):
                v = {'model': f'projectzero:block/ancient_gate_stone/{mn}'}
                if axis == 'z' and n != 'seal' and not n.startswith('plate_'):
                    v['y'] = 90
                variants[f'axis={axis},lit={str(lit).lower()},material={mat},piece={n}'] = v
with open(os.path.join(A, 'blockstates/ancient_gate_stone.json'), 'w') as fh:
    json.dump({'variants': variants}, fh, indent=1)

def jbox(n):
    if n == 'seal':
        return 'new double[][] {{0, 0, 0, 16, 1, 16}}'
    if n.startswith('plate_'):
        return 'new double[][] {{0, 0, 0, 16, %d, 16}}' % PLATES[n]
    boxes, (z0, z1), _ = pieces[n]
    bs = []
    for i, b in enumerate(boxes):
        za, zb = zrange(n, i, z0, z1)
        bs.append('{%g, %g, %g, %g, %g, %g}' % (b[0], b[1], za, b[2], b[3], zb))
    return 'new double[][] {' + ', '.join(bs) + '}'

lines = []
for n in names:
    kind = 'RUBBLE' if n == 'rubble' else 'SEAL' if n == 'seal' else 'PLATE' if n.startswith('plate_') else 'KEYSTONE' if n.startswith('k_') else 'RING'
    lines.append(f'    {n.upper()}(Kind.{kind}, {jbox(n)})')
tab = []
for size, cells in table.items():
    body = ',\n'.join(f'                new Cell({u}, {h}, {n.upper()})' for u, h, n in cells)
    tab.append(f'        RING.put(GateSize.{size}, List.of(\n{body}));')

template = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'AncientStonePiece.java.tmpl')).read()
src = template.replace('@@PIECES@@', ',\n'.join(lines)).replace('@@RING@@', '\n'.join(tab))
with open(os.path.join(J, 'AncientStonePiece.java'), 'w', newline='\n') as fh:
    fh.write(src)
print(len(names), 'pieces', len(variants), 'variants')
