"""Vertical Heat Exchanger, 1:1. Origin = core cell centre at floor level; footprint x -1.5..1.5, z -1.5..1.5, y 0..3.
Lower nozzles (y=0.5) = turbine loop (copper flanges), upper nozzles (y=2.5) = pumped water side (teal flanges)."""
import math, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from ntmkit import Model
m = Model(seed=23)
G = 'Body'
N = Model.ngon

# stepped steel base plate
m.box(G, -1.5, 0, -1.5, 1.5, 0.125, 1.5, {'py': 'steel', 'ny': 'steel'}, default='plate_side')
m.box(G, -1.375, 0.125, -1.375, 1.375, 0.25, 1.375, {'py': 'steel', 'ny': 'steel'}, default='plate_side')

# main shell: octagonal column, flats face the four sides. y-axis profile centre is (z, x).
shell = N(0, 0, 1.05, 8)
m.prism(G, shell, 0.25, 2.75, 'y', side=('green', {'vents_t': 7}), caps=(False, True),
        cap_paint='dark',
        side_paints={i: ('green', {'vents_t': 7}) for i in range(8)})
# dome and gauge stub on top
m.prism(G, N(0, 0, 0.8, 8), 2.75, 2.93, 'y', side='dark', cap_paint='casing')
m.prism(G, N(0, 0, 0.3, 8), 2.93, 3.0, 'y', side='brass', cap_paint='gauge')

# rivet bands around the shell
for y0, y1 in ((0.58, 0.72), (1.45, 1.59), (2.32, 2.46)):
    m.prism(G, N(0, 0, 1.13, 8), y0, y1, 'y', side='casing', cap_paint=('casing', {'rivets': False}), rivets=False)

# orange heat stripes on the +z flat, upper section (flats are 0.8 wide; drawn as a thin plate)
m.box(G, -0.4, 1.7, 0.95, 0.4, 2.2, 1.0, {'pz': ('stripe_green')}, default='green')

# front gauge (+z)
m.box(G, -0.25, 0.95, 0.96, 0.25, 1.35, 1.04, {'pz': 'gauge'}, default='steel')

# copper riser tubes in the four corners, with brass collars
for sx in (-1, 1):
    for sz in (-1, 1):
        cz, cx = 1.18 * sz, 1.18 * sx
        m.prism(G, N(cz, cx, 0.13, 8), 0.25, 2.7, 'y', side='copper', cap_paint='brass')
        m.prism(G, N(cz, cx, 0.19, 8), 0.5, 0.62, 'y', side='brass', cap_paint='brass')
        m.prism(G, N(cz, cx, 0.19, 8), 2.4, 2.52, 'y', side='brass', cap_paint='brass')

# nozzles on both side faces at the two port heights
def nozzle_x(sign, y, flange, port):
    c0, c1 = (0.95, 1.5) if sign > 0 else (-1.5, -0.95)
    m.prism(G, N(y, 0.0, 0.22, 8), c0, c1, 'x', side='pipe', cap_paint=port)
    f0, f1 = (1.36, 1.5) if sign > 0 else (-1.5, -1.36)
    m.prism(G, N(y, 0.0, 0.3, 8), f0, f1, 'x', side=flange, cap_paint=port)
for sign in (1, -1):
    nozzle_x(sign, 0.5, 'copper', 'copper_port')
    nozzle_x(sign, 2.5, 'teal', 'teal_port')

W, H = m.pack(256)
print('atlas', W, H, 'faces', len(m.faces))
base = '/home/claude/out/src/main/resources/assets/hbm'
os.makedirs(base + '/models/machines', exist_ok=True)
os.makedirs(base + '/textures/models/machines', exist_ok=True)
open(base + '/models/machines/vertical_heat_exchanger.obj', 'w').write(m.obj('vertical_heat_exchanger'))
m.paint_atlas().save(base + '/textures/models/machines/vertical_heat_exchanger.png')
print('written')
