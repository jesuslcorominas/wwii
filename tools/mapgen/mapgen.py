"""Map generator: builds a hex map in the format of docs/formato-mapa.md from Natural Earth
geography (public domain) and a theatre definition module (territories, 1939 borders, cities,
straits...).

Usage (from the repo root):
    python3 -I tools/mapgen/mapgen.py <natural_earth_dir> data/maps/europe-1939.json
    python3 -I tools/mapgen/mapgen.py <natural_earth_dir> data/maps/europe-1939.json --reference <png>

Only the Python standard library is needed. --reference also draws a PNG of the real geography
under the grid, plus its calibration for the map editor; that part needs Pillow.

See docs/mapa-europa-1939.md.
"""

import json
import math
import os
import sys
from collections import deque

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import europe1939 as theatre  # noqa: E402

# ------------------------------------------------------------------ projection (decision D5)
EARTH_R = 6371.0
LAT1, LAT2 = math.radians(38), math.radians(62)
LAT0, LON0 = math.radians(50), math.radians(18)
N_CONE = math.log(math.cos(LAT1) / math.cos(LAT2)) / math.log(
    math.tan(math.pi / 4 + LAT2 / 2) / math.tan(math.pi / 4 + LAT1 / 2)
)
F_CONE = math.cos(LAT1) * math.tan(math.pi / 4 + LAT1 / 2) ** N_CONE / N_CONE
RHO0 = EARTH_R * F_CONE / math.tan(math.pi / 4 + LAT0 / 2) ** N_CONE


def project(lon, lat):
    """Lambert conformal conic, km; y grows to the north."""
    rho = EARTH_R * F_CONE / math.tan(math.pi / 4 + math.radians(lat) / 2) ** N_CONE
    theta = N_CONE * (math.radians(lon) - LON0)
    return rho * math.sin(theta), RHO0 - rho * math.cos(theta)


def unproject(x, y):
    dy = RHO0 - y
    rho = math.copysign(math.hypot(x, dy), N_CONE)
    theta = math.atan2(x, dy)
    lat = 2 * math.atan((EARTH_R * F_CONE / rho) ** (1 / N_CONE)) - math.pi / 2
    return math.degrees(LON0 + theta / N_CONE), math.degrees(lat)


# ------------------------------------------------------------------ grid (decision D5)
HEX_W = 110.0  # km, flat side to flat side
HEX_R = HEX_W / math.sqrt(3)  # centre to corner
ROW_H = 1.5 * HEX_R
X_MIN, Y_MAX = -2460.0, 2420.0
COLS, ROWS = 42, 49


def offset_center(i, j):
    return X_MIN + HEX_W / 2 + (i + 0.5 * (j % 2)) * HEX_W, Y_MAX - HEX_R - j * ROW_H


def offset_to_axial(i, j):
    return i - (j - (j & 1)) // 2, j


def axial_center(q, r):
    return X_MIN + HEX_W / 2 + (q + r / 2) * HEX_W, Y_MAX - HEX_R - r * ROW_H


def hex_at(x, y):
    """Axial coordinates of the hex that contains the point (km)."""
    fr = (Y_MAX - HEX_R - y) / ROW_H
    fq = (x - X_MIN - HEX_W / 2) / HEX_W - fr / 2
    fs = -fq - fr
    q, r, s = round(fq), round(fr), round(fs)
    dq, dr, ds = abs(q - fq), abs(r - fr), abs(s - fs)
    if dq > dr and dq > ds:
        q = -r - s
    elif dr > ds:
        r = -q - s
    return q, r


DIRS = [(1, 0), (1, -1), (0, -1), (-1, 0), (-1, 1), (0, 1)]


def neighbors(c):
    return [(c[0] + dq, c[1] + dr) for dq, dr in DIRS]


def hex_distance(a, b):
    dq, dr = a[0] - b[0], a[1] - b[1]
    return max(abs(dq), abs(dr), abs(dq + dr))


def hex_corners(q, r):
    cx, cy = axial_center(q, r)
    return [
        (cx + HEX_R * math.cos(math.radians(a)), cy + HEX_R * math.sin(math.radians(a)))
        for a in (90, 30, -30, -90, -150, 150)
    ]


SAMPLE_STEP = 11.0  # km between samples: about 85 samples per hex


def sample_offsets():
    out = []
    n = int(HEX_W / SAMPLE_STEP)
    for a in range(-n, n + 1):
        for b in range(-n, n + 1):
            dx, dy = a * SAMPLE_STEP + SAMPLE_STEP / 2, b * SAMPLE_STEP + SAMPLE_STEP / 2
            if abs(dx) <= HEX_W / 2 and abs(dy) <= HEX_R - abs(dx) / math.sqrt(3):
                out.append((dx, dy))
    return out


OFFSETS = sample_offsets()

# ------------------------------------------------------------------ geometry


class Shape:
    """Polygons (outer rings and holes, even-odd) in km, with edges bucketed by y."""

    BUCKET = 20.0

    def __init__(self, rings):
        self.rings = [r for r in rings if len(r) >= 3]
        xs = [p[0] for r in self.rings for p in r]
        ys = [p[1] for r in self.rings for p in r]
        self.bbox = (min(xs), min(ys), max(xs), max(ys)) if xs else (0, 0, -1, -1)
        self.buckets = {}
        for ring in self.rings:
            for k in range(len(ring)):
                x1, y1 = ring[k]
                x2, y2 = ring[(k + 1) % len(ring)]
                if y1 == y2:
                    continue
                lo, hi = int(math.floor(min(y1, y2) / self.BUCKET)), int(math.floor(max(y1, y2) / self.BUCKET))
                for b in range(lo, hi + 1):
                    self.buckets.setdefault(b, []).append((x1, y1, x2, y2))

    def contains(self, x, y):
        a, b, c, d = self.bbox
        if not (a <= x <= c and b <= y <= d):
            return False
        inside = False
        for x1, y1, x2, y2 in self.buckets.get(int(math.floor(y / self.BUCKET)), ()):
            if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
                inside = not inside
        return inside

    def area(self):
        total = 0.0
        for ring in self.rings:
            s = 0.0
            for k in range(len(ring)):
                x1, y1 = ring[k]
                x2, y2 = ring[(k + 1) % len(ring)]
                s += x1 * y2 - x2 * y1
            total += s / 2
        return abs(total)


def lonlat_shape(points):
    return Shape([[project(lo, la) for lo, la in points]])


IN_LON, IN_LAT = (-32.0, 62.0), (20.0, 78.0)


def near_theatre(ring):
    return any(IN_LON[0] < lo < IN_LON[1] and IN_LAT[0] < la < IN_LAT[1] for lo, la in ring)


def polygons_of(geom):
    t, c = geom["type"], geom["coordinates"]
    if t == "Polygon":
        return [c]
    if t == "MultiPolygon":
        return c
    return []


def lines_of(geom):
    t, c = geom["type"], geom["coordinates"]
    if t == "LineString":
        return [c]
    if t == "MultiLineString":
        return c
    return []


def load_features(ne_dir, name):
    path = os.path.join(ne_dir, name + ".geojson")
    if not os.path.exists(path):
        sys.exit(f"Missing {path}. See docs/mapa-europa-1939.md (Natural Earth {theatre.NATURAL_EARTH_VERSION}).")
    with open(path, encoding="utf-8") as f:
        return json.load(f)["features"]


def projected_polygons(features, keep=lambda props: True):
    out = []
    for f in features:
        if not keep(f["properties"]):
            continue
        for poly in polygons_of(f["geometry"]):
            if near_theatre(poly[0]):
                out.append((f["properties"], [[project(lo, la) for lo, la in ring] for ring in poly]))
    return out


# ------------------------------------------------------------------ generator


class Generator:
    def __init__(self, ne_dir):
        self.ne_dir = ne_dir
        self.warnings = []
        self.errors = []
        self.load()

    def warn(self, text):
        self.warnings.append(text)

    def error(self, text):
        self.errors.append(text)

    # ---- inputs
    def load(self):
        land = projected_polygons(load_features(self.ne_dir, "ne_50m_land"))
        self.land_shapes = [Shape(rings) for _, rings in land]
        lakes = projected_polygons(
            load_features(self.ne_dir, "ne_50m_lakes"),
            lambda p: p.get("name") in theatre.LAKES or p.get("name_en") in theatre.LAKES,
        )
        self.lake_shapes = [Shape(rings) for _, rings in lakes]
        self.countries = []
        for props, rings in projected_polygons(load_features(self.ne_dir, "ne_50m_admin_0_countries")):
            self.countries.append((props["ADM0_A3"], Shape(rings)))
        self.overrides = [(tid, lonlat_shape(pts)) for tid, pts in theatre.BORDERS_1939]
        ranges = projected_polygons(
            load_features(self.ne_dir, "ne_10m_geography_regions_polys"),
            lambda p: p.get("NAME") in theatre.MOUNTAIN_RANGES,
        )
        self.range_shapes = [Shape(rings) for _, rings in ranges]
        self.extra_mountains = [lonlat_shape(pts) for pts in theatre.EXTRA_MOUNTAINS]
        self.river_lines = []
        for f in load_features(self.ne_dir, "ne_10m_rivers_lake_centerlines"):
            p = f["properties"]
            if p.get("featurecla") != "River" or p.get("name_en") not in theatre.RIVERS:
                continue
            for line in lines_of(f["geometry"]):
                if near_theatre(line):
                    self.river_lines.append((p["name_en"], [project(lo, la) for lo, la in line]))
        missing = set(theatre.RIVERS) - {n for n, _ in self.river_lines}
        if missing:
            self.warn(f"rivers not found in Natural Earth: {sorted(missing)}")

    # ---- territory at a point
    def country_at(self, x, y):
        for a3, shape in self.countries:
            if shape.contains(x, y):
                return a3
        return None

    def territory_at(self, x, y):
        """Territory id, None when the point is off the board, or '?' when unknown."""
        tid = None
        for t, shape in self.overrides:
            if shape.contains(x, y):
                tid = t
                break
        if tid is None:
            a3 = self.country_at(x, y)
            if a3 is None:
                return "?"
            tid = theatre.COUNTRY_TO_TERRITORY.get(a3, "?")
            if tid == "?":
                self.warn(f"country {a3} has no territory")
        if tid is not None and tid in theatre.SOUTH_CUT:
            lon, lat = unproject(x, y)
            if lat < theatre.SOUTH_CUT[tid]:
                return None
        return tid

    # ---- main steps
    def classify(self):
        """Terrain, territory and mountain of every hex of the frame, by sampling."""
        self.cells = {}
        for j in range(ROWS):
            for i in range(COLS):
                q, r = offset_to_axial(i, j)
                cx, cy = offset_center(i, j)
                n = len(OFFSETS)
                land = lake = 0
                votes = {}
                mountain = 0
                island_hits = {}
                for dx, dy in OFFSETS:
                    x, y = cx + dx, cy + dy
                    if any(s.contains(x, y) for s in self.lake_shapes):
                        lake += 1
                        continue
                    hit = None
                    for k, s in enumerate(self.land_shapes):
                        if s.contains(x, y):
                            hit = k
                            break
                    if hit is None:
                        continue
                    land += 1
                    island_hits[hit] = island_hits.get(hit, 0) + 1
                    t = self.territory_at(x, y)
                    votes[t] = votes.get(t, 0) + 1
                    if any(s.contains(x, y) for s in self.range_shapes) or any(
                        s.contains(x, y) for s in self.extra_mountains
                    ):
                        mountain += 1
                self.cells[(q, r)] = {
                    "land": land / n,
                    "lake": lake / n,
                    "votes": votes,
                    "mountain": mountain / max(land, 1),
                    "islands": {k: v / n for k, v in island_hits.items()},
                }

    def decide_terrain(self):
        self.hexes = {}
        island_sizes = {k: s.area() for k, s in enumerate(self.land_shapes)}
        small_islands = {
            k for k, a in island_sizes.items() if theatre.ISLAND_MIN_KM2 <= a <= theatre.ISLAND_MAX_KM2
        }
        best_hex = {}
        for c, cell in self.cells.items():
            for k, frac in cell["islands"].items():
                if k in small_islands and frac > best_hex.get(k, (None, 0))[1]:
                    best_hex[k] = (c, frac)
        forced_island_hexes = {c for c, _ in best_hex.values()}
        for c, cell in self.cells.items():
            water = 1 - cell["land"] - cell["lake"]
            island_frac = max((f for k, f in cell["islands"].items() if k in small_islands), default=0)
            is_land_like = water < 0.5 or island_frac >= theatre.ISLAND_HEX_SHARE or c in forced_island_hexes
            if not is_land_like:
                terrain = "sea"
            elif cell["lake"] > cell["land"]:
                terrain = "lake"
            else:
                terrain = "land"
            h = {"terrain": terrain}
            if terrain == "land":
                territory = self.majority_territory(c, cell)
                if territory is None:
                    continue  # off the board
                h["territory"] = territory
                if cell["mountain"] >= theatre.MOUNTAIN_SHARE:
                    h["mountain"] = True
            self.hexes[c] = h

    def majority_territory(self, c, cell):
        votes = dict(cell["votes"])
        unknown = votes.pop("?", 0)
        if not votes:
            if unknown:
                x, y = axial_center(*c)
                t = self.nearest_territory(x, y)
                return t
            return None
        best = sorted(votes.items(), key=lambda kv: (-kv[1], kv[0] or ""))[0][0]
        return best

    def nearest_territory(self, x, y):
        for radius in range(10, 200, 10):
            for a in range(0, 360, 30):
                t = self.territory_at(x + radius * math.cos(math.radians(a)), y + radius * math.sin(math.radians(a)))
                if t != "?":
                    return t
        return None

    def point_hex(self, lon, lat):
        return hex_at(*project(lon, lat))

    def apply_forced(self):
        for name, lon, lat, values in theatre.FORCED_HEXES:
            c = self.point_hex(lon, lat)
            self.apply_override(c, values, name)
        for (q, r), values in theatre.HEX_OVERRIDES.items():
            self.apply_override((q, r), values, f"({q}, {r})")

    def apply_override(self, c, values, label):
        if values is None:
            self.hexes.pop(c, None)
            return
        h = dict(self.hexes.get(c, {"terrain": "sea"}))
        h.update(values)
        if h["terrain"] != "land":
            for k in ("territory", "mountain", "city", "airport"):
                h.pop(k, None)
        elif "territory" not in h:
            sys.exit(f"forced land hex {label} {c} needs a territory")
        self.hexes[c] = h

    def build_straits(self):
        self.straits = {}
        for name, sea, shore_a, shore_b in theatre.STRAITS:
            s, a, b = tuple(sea), tuple(shore_a), tuple(shore_b)
            self.hexes[s] = {"terrain": "sea"}
            for shore in (a, b):
                if self.hexes.get(shore, {}).get("terrain") != "land":
                    self.error(f"strait {name}: shore {shore} is not land")
                if hex_distance(shore, s) != 1:
                    self.error(f"strait {name}: shore {shore} not next to {s}")
            self.straits[s] = (name, a, b)
            self.hexes[s]["strait"] = [list(a), list(b)]

    def drop_isolated_sea(self):
        """Sea that does not reach the open ocean (Red Sea, Persian Gulf) leaves the board."""
        seas = {c for c, h in self.hexes.items() if h["terrain"] == "sea"}
        corner = offset_to_axial(0, 0)
        if corner not in seas:
            sys.exit("expected open sea in the north-west corner")
        reached = {corner}
        todo = deque([corner])
        while todo:
            c = todo.popleft()
            for n in neighbors(c):
                if n in seas and n not in reached:
                    reached.add(n)
                    todo.append(n)
        for c in seas - reached:
            del self.hexes[c]

    def place_cities(self):
        self.city_hexes = {}
        for city in theatre.CITIES:
            name, lon, lat, kind, territory = city[:5]
            options = city[5] if len(city) > 5 else {}
            c = self.find_city_hex(name, project(lon, lat), territory)
            if c is None:
                if not options.get("optional"):
                    self.error(f"no free hex of {territory} for city {name}")
                continue
            h = self.hexes[c]
            h["city"] = {"name": name} if kind == "city" else {"name": name, "kind": kind}
            self.city_hexes[name] = c
        for name, lon, lat in theatre.AIRPORTS:
            c = hex_at(*project(lon, lat))
            h = self.hexes.get(c)
            if h is None or h["terrain"] != "land" or "city" in h:
                self.error(f"airport {name} at {c} is not on free land")
                continue
            h["airport"] = True

    def find_city_hex(self, name, point, territory):
        x, y = point
        best = None
        for c, h in self.hexes.items():
            if h["terrain"] != "land" or h.get("territory") != territory or "city" in h:
                continue
            cx, cy = axial_center(*c)
            d = math.hypot(cx - x, cy - y)
            if d <= theatre.CITY_MAX_SHIFT_KM and (best is None or (d, c) < best):
                best = (d, c)
        if best is None:
            return None
        if best[0] > HEX_R:
            self.warn(f"city {name} ({territory}) moved {best[0]:.0f} km to {best[1]}")
        return best[1]

    # ---- rivers along hex sides
    def build_rivers(self):
        def vkey(p):
            return (round(p[0], 1), round(p[1], 1))

        def nearest_vertex(x, y):
            q, r = hex_at(x, y)
            return min((vkey(v) for v in hex_corners(q, r)), key=lambda v: (math.hypot(v[0] - x, v[1] - y), v))

        vertex_set = set()
        for c in self.hexes:
            for v in hex_corners(*c):
                vertex_set.add(vkey(v))

        def vneighbors(v):
            out = []
            for a in range(30, 390, 60):
                w = vkey((v[0] + HEX_R * math.cos(math.radians(a)), v[1] + HEX_R * math.sin(math.radians(a))))
                for cand in (w, (round(w[0] + 0.1, 1), w[1]), (round(w[0] - 0.1, 1), w[1]),
                             (w[0], round(w[1] + 0.1, 1)), (w[0], round(w[1] - 0.1, 1))):
                    if cand in vertex_set:
                        out.append(cand)
                        break
            return out

        def path(a, b):
            if a == b:
                return [a]
            prev = {a: None}
            todo = deque([a])
            while todo:
                v = todo.popleft()
                if v == b:
                    break
                for w in sorted(vneighbors(v)):
                    if w not in prev and math.dist(w, a) < 4 * HEX_R:
                        prev[w] = v
                        todo.append(w)
            if b not in prev:
                return [a, b]
            out = [b]
            while prev[out[-1]] is not None:
                out.append(prev[out[-1]])
            return out[::-1]

        def side_hexes(v1, v2):
            mx, my = (v1[0] + v2[0]) / 2, (v1[1] + v2[1]) / 2
            dx, dy = v2[0] - v1[0], v2[1] - v1[1]
            length = math.hypot(dx, dy)
            px, py = -dy / length, dx / length
            a = hex_at(mx + px * HEX_W / 4, my + py * HEX_W / 4)
            b = hex_at(mx - px * HEX_W / 4, my - py * HEX_W / 4)
            return a, b

        sides = set()
        for _, line in self.river_lines:
            verts = []
            for k in range(len(line) - 1):
                (x1, y1), (x2, y2) = line[k], line[k + 1]
                steps = max(1, int(math.hypot(x2 - x1, y2 - y1) / 5))
                for s in range(steps):
                    t = s / steps
                    v = nearest_vertex(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t)
                    if not verts or verts[-1] != v:
                        verts.append(v)
            v = nearest_vertex(*line[-1])
            if not verts or verts[-1] != v:
                verts.append(v)
            full = [verts[0]]
            for v in verts[1:]:
                full.extend(path(full[-1], v)[1:])
            for v1, v2 in zip(full, full[1:]):
                if abs(math.dist(v1, v2) - HEX_R) > 1:
                    continue
                a, b = side_hexes(v1, v2)
                ha, hb = self.hexes.get(a), self.hexes.get(b)
                if ha and hb and ha["terrain"] == "land" and hb["terrain"] == "land":
                    sides.add(tuple(sorted((a, b))))
        for a, b in theatre.EXTRA_RIVER_SIDES:
            sides.add(tuple(sorted((tuple(a), tuple(b)))))
        for a, b in theatre.REMOVED_RIVER_SIDES:
            sides.discard(tuple(sorted((tuple(a), tuple(b)))))
        self.rivers = sorted(s for s in sides if self.hexes.get(s[0], {}).get("terrain") == "land"
                             and self.hexes.get(s[1], {}).get("terrain") == "land")

    # ---- territories
    def territories(self):
        used = {h.get("territory") for h in self.hexes.values()} - {None}
        out = []
        for tid, nation, kind, name in theatre.TERRITORIES:
            if tid not in used:
                self.warn(f"territory {tid} has no hexes")
            t = {"id": tid, "nation": nation, "kind": kind, "name": name}
            if not self.connected(tid):
                t["discontiguous"] = True
            out.append(t)
        unknown = used - {t[0] for t in theatre.TERRITORIES}
        if unknown:
            sys.exit(f"hexes with undeclared territories: {sorted(unknown)}")
        return out

    def connected(self, tid):
        cells = {c for c, h in self.hexes.items() if h.get("territory") == tid}
        if not cells:
            return True
        links = {c: [n for n in neighbors(c) if n in cells] for c in cells}
        for _, a, b in self.straits.values():
            if a in cells and b in cells:
                links[a].append(b)
                links[b].append(a)
        start = min(cells)
        seen = {start}
        todo = [start]
        while todo:
            c = todo.pop()
            for n in links[c]:
                if n not in seen:
                    seen.add(n)
                    todo.append(n)
        return len(seen) == len(cells)

    # ---- output
    def document(self):
        hexes = []
        for (q, r) in sorted(self.hexes, key=lambda c: (c[1], c[0])):
            h = self.hexes[(q, r)]
            out = {"q": q, "r": r, "terrain": h["terrain"]}
            for k in ("territory", "mountain", "city", "airport", "strait"):
                if k in h and h[k] not in (None, False):
                    out[k] = h[k]
            hexes.append(out)
        return {
            "formatVersion": 1,
            "id": theatre.MAP_ID,
            "name": theatre.MAP_NAME,
            "territories": self.territories(),
            "hexes": hexes,
            "rivers": [{"a": list(a), "b": list(b)} for a, b in self.rivers],
        }

    def run(self):
        self.classify()
        self.decide_terrain()
        self.apply_forced()
        self.build_straits()
        self.drop_isolated_sea()
        self.place_cities()
        self.build_rivers()
        return self.document()


# ------------------------------------------------------------------ reference image for the editor

PX_PER_KM = 0.5


def to_px(x, y):
    return (x - X_MIN) * PX_PER_KM, (Y_MAX - y) * PX_PER_KM


def write_reference(gen, png_path, map_path):
    try:
        from PIL import Image, ImageDraw
    except ImportError:
        sys.exit("--reference needs Pillow (pip install pillow)")
    width = int((COLS * HEX_W + HEX_W / 2) * PX_PER_KM) + 1
    height = int(((ROWS - 1) * ROW_H + 2 * HEX_R) * PX_PER_KM) + 1
    img = Image.new("RGB", (width, height), (196, 216, 232))
    draw = ImageDraw.Draw(img)
    for shape in gen.land_shapes:
        draw.polygon([to_px(*p) for p in shape.rings[0]], fill=(236, 230, 214))
        for hole in shape.rings[1:]:
            draw.polygon([to_px(*p) for p in hole], fill=(196, 216, 232))
    for shape in gen.lake_shapes:
        for ring in shape.rings:
            draw.polygon([to_px(*p) for p in ring], fill=(170, 200, 225))
    for _, shape in gen.overrides:
        draw.line([to_px(*p) for p in shape.rings[0] + shape.rings[0][:1]], fill=(200, 60, 60), width=1)
    for _, shape in gen.countries:
        for ring in shape.rings:
            draw.line([to_px(*p) for p in ring + ring[:1]], fill=(120, 110, 100), width=1)
    for _, line in gen.river_lines:
        draw.line([to_px(*p) for p in line], fill=(60, 110, 190), width=2)
    img.save(png_path)
    ox, oy = to_px(*axial_center(0, 0))
    calibration = {
        "imagePath": os.path.abspath(png_path),
        "originX": ox,
        "originY": oy,
        "hexWidth": HEX_W * PX_PER_KM,
        "hexHeight": 2 * HEX_R * PX_PER_KM,
    }
    sidecar = map_path[: -len(".json")] + ".editor.json"
    with open(sidecar, "w", encoding="utf-8") as f:
        json.dump(calibration, f, indent=4)
        f.write("\n")
    print(f"reference image {png_path}, calibration {sidecar}")


def main(argv):
    if len(argv) < 3:
        sys.exit(__doc__)
    ne_dir, out = argv[1], argv[2]
    gen = Generator(ne_dir)
    doc = gen.run()
    text = json.dumps(doc, indent=2, ensure_ascii=False) + "\n"
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    with open(out, "w", encoding="utf-8") as f:
        f.write(text)
    land = sum(1 for h in doc["hexes"] if h["terrain"] == "land")
    print(f"{out}: {len(doc['hexes'])} hexes ({land} land), {len(doc['territories'])} territories, "
          f"{sum(1 for h in doc['hexes'] if 'city' in h)} cities, {len(doc['rivers'])} river sides")
    for w in gen.warnings:
        print("warning:", w)
    for e in gen.errors:
        print("error:", e)
    if "--reference" in argv:
        write_reference(gen, argv[argv.index("--reference") + 1], out)
    if gen.errors:
        sys.exit(1)


if __name__ == "__main__":
    main(sys.argv)
