"""Create Energy Shaft, 1:1. Designed with the core cell at the origin (footprint x -1.5..1.5, z -3.5..1.5, +Z = front); the OBJ is written
shifted by +1 in z so it is centred on its own midpoint, exactly like NTM's turbine.obj (z -2.5..2.5); the renderer shifts it back by -1."""
import math, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from ntmkit import Model, PPB

m = Model(seed=11)
R = Model.rprofile
G = 'Body'

# --- stepped steel base plate
m.box(G, -1.5, 0, -3.5, 1.5, 0.125, 1.5, {'py': 'steel', 'ny': 'steel'}, default='plate_side')
m.box(G, -1.375, 0.125, -3.375, 1.375, 0.25, 1.375, {'py': 'steel', 'ny': 'steel'}, default='plate_side')

# --- rear: power conditioner (green, vented, orange stripes, hazard decal)
rear = R(0, 1.0, 2.0, 1.5, 0.25)
m.prism(G, rear, -3.375, -1.5, 'z', side='green', caps=(True, False), cap_paint=('green', {'rivets': True}),
        side_paints={0: ('green', {'vents': 4}),
                     4: ('green', {'vents': 4, 'hazard': (20, 4, 8)}),
                     2: ('green', {'stripes': 6})})

# --- middle: stabiliser ring housing with rivet bands
ring = R(0, 1.075, 2.5, 1.65, 0.5)
m.prism(G, ring, -1.5, 0.25, 'z', side='casing', caps=(False, True), cap_paint=('casing', {'rivets': False}))
band = R(0, 1.075, 2.62, 1.77, 0.5)
for z0, z1 in ((-1.5, -1.36), (-0.78, -0.64), (0.11, 0.25)):
    m.prism(G, band, z0, z1, 'z', side='casing', cap_paint=('casing', {'rivets': False}), rivets=False)

# --- front bell, brass trim and the collar the Shaft Output plugs into
bell = R(0, 0.95, 1.9, 1.4, 0.4)
m.prism(G, bell, 0.25, 1.0, 'z', side='dark', caps=(False, False))
trim = R(0, 0.95, 2.0, 1.5, 0.4)
m.prism(G, trim, 0.99, 1.07, 'z', side='brass', cap_paint='dark')
m.ring(G, Model.ngon(0, 0.5, 0.5, 8), Model.ngon(0, 0.5, 0.36, 8), 1.08, 1.5, 'z', paint='brass', inner_paint='dark')

# --- port nozzles (power / water / steam / hot water), grey octagons with a square opening like NTM
def nozzle_x(sign):
    c0, c1 = (0.95, 1.5) if sign > 0 else (-1.5, -0.95)
    m.prism(G, Model.ngon(0.5, 0.0, 0.25, 8), c0, c1, 'x', side='pipe', cap_paint='port')
    f0, f1 = (1.36, 1.5) if sign > 0 else (-1.5, -1.36)
    m.prism(G, Model.ngon(0.5, 0.0, 0.33, 8), f0, f1, 'x', side='steel', cap_paint=('port', {'rim': None}))
nozzle_x(+1); nozzle_x(-1)
m.prism(G, Model.ngon(0, 0.5, 0.25, 8), -3.5, -3.2, 'z', side='pipe', cap_paint='port')
m.prism(G, Model.ngon(0, 0.5, 0.33, 8), -3.5, -3.36, 'z', side='steel', cap_paint='port')

# --- capacitor stack and stability gauge on top of the conditioner
for x in (-0.45, 0.45):
    m.prism(G, Model.ngon(-2.55, x, 0.2, 8), 1.75, 1.97, 'y', side='dark', cap_paint='brass')
m.box(G, -0.2, 1.75, -1.95, 0.2, 1.8, -1.65, {'py': 'gauge'}, default='steel')

# --- rotor: spinning flywheel inside the collar (group Rotor, spun about z through (0, 0.5))
Rg = 'Rotor'
m.prism(Rg, Model.ngon(0, 0.5, 0.34, 8), 1.2, 1.32, 'z', side='brass_d', cap_paint=('disc', {'marker': True}))
for k in range(4):
    m.box_rotz(Rg, 0, 0.5, 0.31, 0.035, 1.32, 1.37, k * math.pi / 4, paint='brass')
m.prism(Rg, Model.ngon(0, 0.5, 0.14, 8), 1.1, 1.5, 'z', side='brass', cap_paint='socket')

W, H = m.pack(256)
print('atlas', W, H, 'faces', len(m.faces))
base = '/home/claude/out/src/main/resources/assets/hbm'
os.makedirs(base + '/models/machines', exist_ok=True)
os.makedirs(base + '/textures/models/machines', exist_ok=True)
open(base + '/models/machines/shaft_turbine.obj', 'w').write(m.obj('shaft_turbine', shift=(0.0, 0.0, 1.0)))
m.paint_atlas().save(base + '/textures/models/machines/shaft_turbine.png')
print('written')
