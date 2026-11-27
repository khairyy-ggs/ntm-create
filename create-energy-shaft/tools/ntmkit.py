"""ntmkit: build NTM-style OBJ models + painted texture atlas at exactly 16 px per block (1:1).

Coordinates are block units, Y up. Faces are packed into one atlas; every face gets a pixel
rectangle sized length*16 so the texel density matches the mod's machine bodies (median 16 px/block).
OBJ output follows the mod's dialect: `o` groups, triangulated `f v/vt/vn`, flat normals, V up.
"""
import math, random
from PIL import Image, ImageDraw, ImageFilter

PPB = 16

def hexc(s):
    s = s.lstrip('#')
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)

# palette sampled from NTM's turbine sheets
C = dict(
    green=hexc('4d6d3f'), green_d=hexc('466339'), vent=hexc('374e2c'), vent_dd=hexc('232f1d'),
    casing=hexc('4e574a'), casing_d=hexc('3d443a'), steel=hexc('5e5e5e'), steel_l=hexc('686868'),
    steel_d=hexc('565656'), orange=hexc('c57245'), orange_d=hexc('9a5232'), yellow=hexc('ffd800'),
    black=hexc('1c1c1c'), rivet=hexc('8d9489'), pipe=hexc('5b6358'), port=hexc('2b2d2b'),
    brass=hexc('c9a24b'), brass_d=hexc('8f6c2c'), brass_l=hexc('ecd27a'), copper=hexc('b4673a'),
    copper_d=hexc('7e4526'), glass=hexc('6fa3b8'), teal=hexc('3f8f86'), teal_d=hexc('2c635d'),
)

def shade(c, d):
    return tuple(max(0, min(255, c[i] + d)) for i in range(3)) + (255,)

# ------------------------------------------------------------------ painters
def _noise(w, h, base, amp, rnd, blur=0.6):
    im = Image.new('RGBA', (w, h), base)
    px = im.load()
    n = Image.new('L', (w, h))
    npx = n.load()
    for y in range(h):
        for x in range(w):
            npx[x, y] = 128 + rnd.randint(-amp * 6, amp * 6)
    if blur:
        n = n.filter(ImageFilter.GaussianBlur(blur))
    npx = n.load()
    for y in range(h):
        for x in range(w):
            d = (npx[x, y] - 128) // 6
            px[x, y] = shade(base, d)
    return im

def p_steel(w, h, rnd, **k):
    im = _noise(w, h, C['steel'], 4, rnd, 0.8)
    return im

def p_plate_side(w, h, rnd, **k):
    im = _noise(w, h, C['steel_d'], 3, rnd, 0.6)
    d = ImageDraw.Draw(im)
    d.line([(0, 0), (w - 1, 0)], fill=C['steel_l'])
    return im

def p_green(w, h, rnd, vents=0, stripes=0, hazard=None, rivets=False, border=True, vents_t=0, **k):
    im = _noise(w, h, C['green'], 1, rnd, 0.5)
    d = ImageDraw.Draw(im)
    if border and w > 3 and h > 3:
        d.rectangle([0, 0, w - 1, h - 1], outline=C['green_d'])
    if vents_t:
        # vents for faces whose long axis is vertical: slits run across the face, i.e. up the image columns
        t = Image.new('RGBA', (h, w), (0, 0, 0, 0))
        td = ImageDraw.Draw(t)
        slot_w = max(2, h - 6); step = (w - 6) / vents_t
        for i in range(vents_t):
            y = int(3 + i * step + step / 2 - 1); x0 = (h - slot_w) // 2
            td.rectangle([x0, y - 1, x0 + slot_w - 1, y + 2], fill=C['vent'])
            td.rectangle([x0 + 1, y, x0 + slot_w - 2, y + 1], fill=C['vent_dd'])
        im.alpha_composite(t.transpose(Image.TRANSPOSE))
    if vents:
        slot_w = max(2, w - 6)
        band = h - 6
        step = band / vents
        for i in range(vents):
            y = int(3 + i * step + step / 2 - 1)
            x0 = (w - slot_w) // 2
            d.rectangle([x0, y - 1, x0 + slot_w - 1, y + 2], fill=C['vent'])
            d.rectangle([x0 + 1, y, x0 + slot_w - 2, y + 1], fill=C['vent_dd'])
    for i in range(stripes):
        y = 2 + i * 2 + (i // 3) * 3
        d.line([(1, y), (w - 2, y)], fill=C['orange'])
    if rivets:
        for (x, y) in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
            d.rectangle([x, y, x + 1, y + 1], fill=C['rivet'])
    if hazard:
        hx, hy, hs = hazard
        _hazard_square(d, hx, hy, hs)
    return im

def _hazard_square(d, x, y, s):
    d.rectangle([x, y, x + s - 1, y + s - 1], fill=C['black'], outline=C['black'])
    for i in range(s - 2):
        yy = y + 1 + i
        span = max(0, (i // 2))
        d.line([(x + s // 2 - span, yy), (x + s // 2 + span, yy)], fill=C['yellow'])
    d.line([(x + s // 2, y + s - 3), (x + s // 2, y + s - 3)], fill=C['black'])

def p_casing(w, h, rnd, rivets=True, **k):
    im = _noise(w, h, C['casing'], 2, rnd, 0.6)
    d = ImageDraw.Draw(im)
    if w > 3 and h > 3:
        d.rectangle([0, 0, w - 1, h - 1], outline=C['casing_d'])
    if rivets and w >= 6 and h >= 6:
        for (x, y) in ((1, 1), (w - 3, 1), (1, h - 3), (w - 3, h - 3)):
            d.rectangle([x, y, x + 1, y + 1], fill=C['rivet'])
    return im

def p_dark(w, h, rnd, **k):
    return _noise(w, h, C['casing_d'], 2, rnd, 0.6)

def p_brass(w, h, rnd, ribs=0, **k):
    im = _noise(w, h, C['brass'], 5, rnd, 0.5)
    d = ImageDraw.Draw(im)
    if w > 2 and h > 2:
        d.rectangle([0, 0, w - 1, h - 1], outline=C['brass_d'])
        d.line([(1, 1), (w - 2, 1)], fill=C['brass_l'])
    for i in range(ribs):
        x = (i + 1) * w // (ribs + 1)
        d.line([(x, 1), (x, h - 2)], fill=C['brass_d'])
    return im

def p_brass_d(w, h, rnd, **k):
    return _noise(w, h, C['brass_d'], 4, rnd, 0.5)

def p_pipe(w, h, rnd, **k):
    im = _noise(w, h, C['pipe'], 3, rnd, 0.6)
    d = ImageDraw.Draw(im)
    d.line([(0, 0), (w - 1, 0)], fill=shade(C['pipe'], 10))
    return im

def p_port(w, h, rnd, rim=None, **k):
    """Square pipe port: framed dark opening, like the nozzle ends on NTM machines."""
    im = _noise(w, h, rim or C['steel_d'], 3, rnd, 0.5)
    d = ImageDraw.Draw(im)
    i0 = max(1, w // 4)
    d.rectangle([i0, i0, w - 1 - i0, h - 1 - i0], fill=C['port'])
    d.rectangle([i0 + 1, i0 + 1, w - 2 - i0, h - 2 - i0], fill=shade(C['port'], -12))
    d.rectangle([0, 0, w - 1, h - 1], outline=shade(rim or C['steel_d'], 14))
    return im

def p_socket(w, h, rnd, **k):
    """Shaft socket face: brass frame around a dark square hole."""
    im = _noise(w, h, C['brass_d'], 3, rnd, 0.5)
    d = ImageDraw.Draw(im)
    i0 = max(1, w // 3)
    d.rectangle([i0, i0, w - 1 - i0, h - 1 - i0], fill=C['port'])
    d.rectangle([0, 0, w - 1, h - 1], outline=C['brass'])
    return im

def p_disc(w, h, rnd, marker=False, **k):
    im = _noise(w, h, C['brass_d'], 4, rnd, 0.5)
    d = ImageDraw.Draw(im)
    if marker:
        cx, cy = w // 2, h // 2
        d.rectangle([cx - 1, 1, cx, 3], fill=C['orange'])
        d.rectangle([cx - 1, h - 4, cx, h - 2], fill=C['orange'])
    return im

def p_stripe_green(w, h, rnd, **k):
    return p_green(w, h, rnd, stripes=3, **k)

def p_copper(w, h, rnd, **k):
    im = _noise(w, h, C['copper'], 4, rnd, 0.5)
    d = ImageDraw.Draw(im)
    d.line([(0, 0), (w - 1, 0)], fill=shade(C['copper'], 18))
    d.line([(0, h - 1), (w - 1, h - 1)], fill=C['copper_d'])
    return im

def p_gauge(w, h, rnd, **k):
    im = _noise(w, h, C['steel_d'], 3, rnd, 0.4)
    d = ImageDraw.Draw(im)
    i0 = max(1, w // 5)
    d.rectangle([i0, i0, w - 1 - i0, h - 1 - i0], fill=hexc('1d2a27'))
    d.line([(w // 2, h - 1 - i0), (w // 2 + 1, i0 + 1)], fill=C['orange'])
    d.rectangle([0, 0, w - 1, h - 1], outline=C['yellow'])
    return im

def p_teal(w, h, rnd, **k):
    im = _noise(w, h, C['teal'], 3, rnd, 0.5)
    d = ImageDraw.Draw(im)
    if w > 2 and h > 2:
        d.rectangle([0, 0, w - 1, h - 1], outline=C['teal_d'])
    return im

def p_copper_port(w, h, rnd, **k):
    return p_port(w, h, rnd, rim=C['copper_d'])

def p_teal_port(w, h, rnd, **k):
    return p_port(w, h, rnd, rim=C['teal_d'])

PAINTERS = {k[2:]: v for k, v in dict(globals()).items() if k.startswith('p_') and callable(v)}

# ------------------------------------------------------------------ geometry
class Face:
    def __init__(self, group, verts, w, h, paint, kw, kind, flip=False):
        self.group, self.verts, self.w, self.h = group, verts, max(1, w), max(1, h)
        self.paint, self.kw, self.kind, self.flip = paint, kw, kind, flip
        self.rx = self.ry = 0

def _sub(a, b): return (a[0] - b[0], a[1] - b[1], a[2] - b[2])
def _len(a): return math.sqrt(a[0] ** 2 + a[1] ** 2 + a[2] ** 2)
def _cross(a, b): return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])

class Model:
    def __init__(self, seed=7):
        self.faces = []
        self.rnd = random.Random(seed)
        self.groups = []

    def _group(self, g):
        if g not in self.groups: self.groups.append(g)

    def quad(self, group, p0, p1, p2, p3, paint='steel', **kw):
        """p0->p1 is the u edge (left to right), p0->p3 the v edge (bottom to top)."""
        self._group(group)
        w = round(_len(_sub(p1, p0)) * PPB)
        h = round(_len(_sub(p3, p0)) * PPB)
        self.faces.append(Face(group, [p0, p1, p2, p3], w, h, paint, kw, 'quad'))

    def poly(self, group, verts, project, paint='steel', **kw):
        """Cap polygon (CCW from outside). project(v)->(u,v) in block units; scaled to px."""
        self._group(group)
        pts = [project(v) for v in verts]
        us = [p[0] for p in pts]; vs = [p[1] for p in pts]
        w = round((max(us) - min(us)) * PPB); h = round((max(vs) - min(vs)) * PPB)
        f = Face(group, verts, w, h, paint, kw, 'poly')
        f.pts = pts; f.umin = min(us); f.vmin = min(vs)
        self.faces.append(f)

    # ---- primitives
    def box(self, group, x0, y0, z0, x1, y1, z1, paints=None, default='steel', **kw):
        P = paints or {}
        def pt(n): return P.get(n, default)
        A, B = (x0, y0, z0), (x1, y1, z1)
        # +z, -z, +x, -x, +y, -y
        self.quad(group, (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1), pt('pz'), **P.get('kw_pz', kw))
        self.quad(group, (x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0), pt('nz'), **P.get('kw_nz', kw))
        self.quad(group, (x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1), pt('px'), **P.get('kw_px', kw))
        self.quad(group, (x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0), pt('nx'), **P.get('kw_nx', kw))
        self.quad(group, (x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0), pt('py'), **P.get('kw_py', kw))
        self.quad(group, (x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1), pt('ny'), **P.get('kw_ny', kw))

    @staticmethod
    def rprofile(cx, cy, w, h, c):
        """Rectangle with cut corners, CCW seen from +z, starting at the lower right chamfer end."""
        hw, hh = w / 2, h / 2
        return [(cx + hw, cy - hh + c), (cx + hw, cy + hh - c), (cx + hw - c, cy + hh), (cx - hw + c, cy + hh),
                (cx - hw, cy + hh - c), (cx - hw, cy - hh + c), (cx - hw + c, cy - hh), (cx + hw - c, cy - hh)]

    @staticmethod
    def ngon(cx, cy, r, n, rot=None):
        rot = math.pi / n if rot is None else rot
        return [(cx + r * math.cos(rot + 2 * math.pi * i / n), cy + r * math.sin(rot + 2 * math.pi * i / n)) for i in range(n)]

    @staticmethod
    def _map(axis):
        # local (a,b,c) -> world; cyclic so winding is preserved. c is the extrusion axis.
        if axis == 'z': return lambda a, b, c: (a, b, c)
        if axis == 'x': return lambda a, b, c: (c, a, b)
        if axis == 'y': return lambda a, b, c: (b, c, a)
        raise ValueError(axis)

    def prism(self, group, profile, c0, c1, axis='z', side='casing', caps=(True, True), cap_paint=None,
              side_paints=None, **kw):
        """Extrude a CCW profile between c0 and c1 along axis. Side faces get `side` (or side_paints[i])."""
        M = self._map(axis)
        n = len(profile)
        for i in range(n):
            a, b = profile[i], profile[(i + 1) % n]
            sp = (side_paints or {}).get(i, side)
            name, skw = (sp if isinstance(sp, tuple) else (sp, kw))
            # u runs along the extrusion axis so vents and stripes run along the machine's length
            self.quad(group, M(b[0], b[1], c0), M(b[0], b[1], c1), M(a[0], a[1], c1), M(a[0], a[1], c0), name, **skw)
        cp = cap_paint or side
        cname, ckw = (cp if isinstance(cp, tuple) else (cp, kw))
        if caps[1]:   # +c end
            v = [M(p[0], p[1], c1) for p in profile]
            self.poly(group, v, lambda q, M=M: self._proj(q, axis, +1), cname, **ckw)
        if caps[0]:   # -c end, reversed
            v = [M(p[0], p[1], c0) for p in reversed(profile)]
            self.poly(group, v, lambda q, M=M: self._proj(q, axis, -1), cname, **ckw)

    @staticmethod
    def _proj(q, axis, s):
        if axis == 'z': return (q[0] * s, q[1])
        if axis == 'x': return (q[2] * s * -1, q[1])
        return (q[0], q[2] * s * -1)

    def ring(self, group, outer, inner, c0, c1, axis='z', paint='brass', inner_paint='dark', **kw):
        """Annular ring between two equal-length profiles: outer wall, inner wall (facing in) and both faces."""
        M = self._map(axis)
        n = len(outer)
        for i in range(n):
            a, b = outer[i], outer[(i + 1) % n]
            self.quad(group, M(b[0], b[1], c0), M(b[0], b[1], c1), M(a[0], a[1], c1), M(a[0], a[1], c0), paint, **kw)
            a, b = inner[i], inner[(i + 1) % n]
            self.quad(group, M(a[0], a[1], c0), M(a[0], a[1], c1), M(b[0], b[1], c1), M(b[0], b[1], c0), inner_paint)
            ao, bo = outer[i], outer[(i + 1) % n]
            ai, bi = inner[i], inner[(i + 1) % n]
            self.quad(group, M(ao[0], ao[1], c1), M(bo[0], bo[1], c1), M(bi[0], bi[1], c1), M(ai[0], ai[1], c1), paint, **kw)
            self.quad(group, M(bo[0], bo[1], c0), M(ao[0], ao[1], c0), M(ai[0], ai[1], c0), M(bi[0], bi[1], c0), paint, **kw)

    def box_rotz(self, group, cx, cy, hx, hy, z0, z1, ang, paint='brass_d', **kw):
        """Box centred at (cx,cy) with half sizes hx,hy, rotated by ang (rad) about the z axis through (cx,cy)."""
        ca, sa = math.cos(ang), math.sin(ang)
        def r(x, y): return (cx + x * ca - y * sa, cy + x * sa + y * ca)
        prof = [r(hx, -hy), r(hx, hy), r(-hx, hy), r(-hx, -hy)]
        self.prism(group, prof, z0, z1, 'z', paint, **kw)

    # ---- atlas
    def pack(self, width=128):
        order = sorted(self.faces, key=lambda f: (-f.h, -f.w))
        x = y = rowh = 0
        for f in order:
            if x + f.w > width:
                x, y, rowh = 0, y + rowh, 0
            f.rx, f.ry = x, y
            x += f.w
            rowh = max(rowh, f.h)
        self.W, self.H = width, y + rowh
        return self.W, self.H

    def paint_atlas(self):
        atlas = Image.new('RGBA', (self.W, self.H), (255, 0, 255, 0))
        for f in self.faces:
            fn = PAINTERS[f.paint]
            img = fn(f.w, f.h, self.rnd, **f.kw)
            if f.kind == 'poly':
                mask = Image.new('L', (f.w, f.h), 0)
                md = ImageDraw.Draw(mask)
                poly = [((p[0] - f.umin) * PPB, f.h - (p[1] - f.vmin) * PPB) for p in f.pts]
                md.polygon(poly, fill=255)
                img.putalpha(mask)
            atlas.alpha_composite(img, (f.rx, f.ry))
        return atlas

    def obj(self, name='model', shift=(0.0, 0.0, 0.0)):
        lines = [f'# ntmkit {name}: 1:1, {PPB} px per block, atlas {self.W}x{self.H}']
        vi = ti = ni = 0
        for g in self.groups:
            lines.append(f'o {g}')
            for f in [q for q in self.faces if q.group == g]:
                vs = f.verts
                n = _cross(_sub(vs[1], vs[0]), _sub(vs[2], vs[0]))
                ln = _len(n) or 1.0
                n = (n[0] / ln, n[1] / ln, n[2] / ln)
                for v in vs: lines.append('v %.6f %.6f %.6f' % (v[0] + shift[0], v[1] + shift[1], v[2] + shift[2]))
                uvs = []
                if f.kind == 'quad':
                    cs = [(0, f.h), (f.w, f.h), (f.w, 0), (0, 0)]
                    for (px, py) in cs: uvs.append(((f.rx + px) / self.W, 1 - (f.ry + py) / self.H))
                else:
                    for p in f.pts:
                        px = (p[0] - f.umin) * PPB; py = f.h - (p[1] - f.vmin) * PPB
                        uvs.append(((f.rx + px) / self.W, 1 - (f.ry + py) / self.H))
                for u, v in uvs: lines.append('vt %.6f %.6f' % (min(1.0, max(0.0, u)), min(1.0, max(0.0, v))))
                lines.append('vn %.6f %.6f %.6f' % n)
                vi0, ti0, ni0 = vi, ti, ni
                vi += len(vs); ti += len(vs); ni += 1
                for k in range(1, len(vs) - 1):
                    lines.append('f %d/%d/%d %d/%d/%d %d/%d/%d' % (
                        vi0 + 1, ti0 + 1, ni0 + 1, vi0 + k + 1, ti0 + k + 1, ni0 + 1, vi0 + k + 2, ti0 + k + 2, ni0 + 1))
        return '\n'.join(lines) + '\n'
