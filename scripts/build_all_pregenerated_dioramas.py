#!/usr/bin/env python3
"""
build_all_pregenerated_dioramas.py
==================================
Processes all 18 pre-generated 3D isometric regional & landmark dioramas:
  1. Direct 8k 16:9 AI-rendered floating island dioramas with translucent turquoise
     cutaway water blocks (`jamestown`, `washington_dc`, `new_york`, `san_francisco`,
     `chicago_great_lakes`, `grand_canyon`, `new_orleans`, `seattle_pnw`, `miami_caribbean`,
     plus existing `yorktown`, `coastal`, `modern`, `alpine`).
  2. Global UNESCO / World Capital composite dioramas (`rome_mediterranean`,
     `tokyo_east_asia`, `london_uk`, `paris_europe`, `cairo_giza`) built on the
     Jamestown / Washington DC / Grand Canyon translucent turquoise cutaway water
     island bases combined with chroma-keyed 3D isometric landmark models from
     `amilich/isometric-city`.
"""

import os
from collections import deque
from PIL import Image, ImageFilter

BRAIN_DIR = "/home/gcerkez/.gemini/antigravity/brain/9d13020f-0e3a-4e92-8852-af20399e39f1"
ISOCITY_DIR = "/tmp/isocity_assets"
ROOT_DIR = "/home/gcerkez/Documents/antigravity/gallant-pasteur"
OUT_DIRS = [
    os.path.join(ROOT_DIR, "package/contents/images"),
    os.path.join(ROOT_DIR, "web/images"),
    os.path.join(ROOT_DIR, "android/app/src/main/assets/web/images"),
]

DIRECT_SOURCES = {
    "jamestown": f"{BRAIN_DIR}/diorama_jamestown_va_1792269964503.jpg",
    "washington_dc": f"{BRAIN_DIR}/diorama_washington_dc_1792269973993.jpg",
    "new_york": f"{BRAIN_DIR}/diorama_new_york_1792269983091.jpg",
    "san_francisco": f"{BRAIN_DIR}/diorama_san_francisco_1792269991266.jpg",
    "chicago_great_lakes": f"{BRAIN_DIR}/diorama_chicago_great_lakes_1792269999997.jpg",
    "grand_canyon": f"{BRAIN_DIR}/diorama_grand_canyon_1792270038049.jpg",
    "new_orleans": f"{BRAIN_DIR}/diorama_new_orleans_1792270046690.jpg",
    "seattle_pnw": f"{BRAIN_DIR}/diorama_seattle_pnw_1792270054949.jpg",
    "miami_caribbean": f"{BRAIN_DIR}/diorama_miami_caribbean_1792270062874.jpg",
}


def extract_clean_island_cutout(img_path: str) -> Image.Image:
    """
    Removes both the studio sky background and the blue floor cast-shadow via border
    flood-fill + largest connected-component filtering, returning a crisp 1376x768
    RGBA cutout with a uniform neutral drop shadow.
    """
    src = Image.open(img_path).convert("RGBA").resize((1376, 768), Image.Resampling.LANCZOS)
    w, h = src.size
    px = src.load()

    border_samples = []
    for x in range(0, w, 8):
        border_samples.append(px[x, 0][:3])
    for y in range(0, h // 2, 8):
        border_samples.append(px[0, y][:3])
        border_samples.append(px[w - 1, y][:3])

    bg_r = sum(c[0] for c in border_samples) / len(border_samples)
    bg_g = sum(c[1] for c in border_samples) / len(border_samples)
    bg_b = sum(c[2] for c in border_samples) / len(border_samples)

    def is_studio_bg_or_cast_shadow(x, y, r, g, b):
        dist = ((r - bg_r) ** 2 + (g - bg_g) ** 2 + (b - bg_b) ** 2) ** 0.5
        tol = 56 if y < int(h * 0.56) else 44
        if dist < tol:
            return True
        # Also match the soft blue cast shadow on the studio floor (high blue dominance vs red/green)
        if b > 165 and (b - r) > 58 and (b - g) > 22 and r > 70 and g > 125:
            # Protect turquoise water on the right/front of the island (turquoise has high green: b - g < 22)
            return True
        return False

    visited = [[False] * h for _ in range(w)]
    q = deque()

    for x in range(w):
        for y in (0, h - 1):
            r, g, b, _ = px[x, y]
            if is_studio_bg_or_cast_shadow(x, y, r, g, b):
                visited[x][y] = True
                q.append((x, y))
    for y in range(h):
        for x in (0, w - 1):
            if not visited[x][y]:
                r, g, b, _ = px[x, y]
                if is_studio_bg_or_cast_shadow(x, y, r, g, b):
                    visited[x][y] = True
                    q.append((x, y))

    while q:
        cx, cy = q.popleft()
        for nx, ny in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
            if 0 <= nx < w and 0 <= ny < h and not visited[nx][ny]:
                r, g, b, _ = px[nx, ny]
                if is_studio_bg_or_cast_shadow(nx, ny, r, g, b):
                    visited[nx][ny] = True
                    q.append((nx, ny))

    # Keep only the largest foreground connected component (strips floating sky clouds)
    fg_visited = [[False] * h for _ in range(w)]
    best_comp = []
    for sx in range(0, w, 2):
        for sy in range(0, h, 2):
            if not visited[sx][sy] and not fg_visited[sx][sy]:
                comp = []
                fq = deque([(sx, sy)])
                fg_visited[sx][sy] = True
                while fq:
                    ux, uy = fq.popleft()
                    comp.append((ux, uy))
                    for vx, vy in ((ux - 1, uy), (ux + 1, uy), (ux, uy - 1), (ux, uy + 1)):
                        if 0 <= vx < w and 0 <= vy < h and not visited[vx][vy] and not fg_visited[vx][vy]:
                            fg_visited[vx][vy] = True
                            fq.append((vx, vy))
                if len(comp) > len(best_comp):
                    best_comp = comp

    keep_mask = Image.new("L", (w, h), 0)
    kpx = keep_mask.load()
    for ux, uy in best_comp:
        kpx[ux, uy] = 255

    smooth_mask = keep_mask.filter(ImageFilter.GaussianBlur(radius=1.1))
    spx = smooth_mask.load()

    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    opx = out.load()
    for x in range(w):
        for y in range(h):
            a = spx[x, y]
            if a > 16:
                r, g, b, _ = px[x, y]
                opx[x, y] = (r, g, b, min(255, int(a * 1.05)))

    # Add soft neutral drop shadow beneath the floating island
    shadow_alpha = smooth_mask.filter(ImageFilter.GaussianBlur(radius=18))
    shadow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    sh_px = shadow.load()
    sa_px = shadow_alpha.load()
    for x in range(w):
        for y in range(h - 14):
            sa = sa_px[x, y]
            if sa > 0:
                sh_px[x, y + 14] = (10, 22, 36, int(sa * 0.30))

    return Image.alpha_composite(shadow, out)


def extract_isocity_tile(sheet_name: str, col: int, row: int, cols: int = 5, rows: int = 6) -> Image.Image:
    """
    Extracts a single isometric building/landmark tile from an `amilich/isometric-city`
    sprite sheet, flood-fills the `#ff0000` background from the tile borders, and
    neutralizes any red edge spill so there is zero red outline.
    """
    sheet_path = os.path.join(ISOCITY_DIR, sheet_name)
    sheet = Image.open(sheet_path).convert("RGBA")
    sw, sh = sheet.size
    tw = sw // cols
    th = sh // rows
    x0 = col * tw
    y0 = row * th
    tile = sheet.crop((x0 + 4, y0 + 4, x0 + tw - 4, y0 + th - 4))
    w, h = tile.size
    tpx = tile.load()

    def is_red_bg(r, g, b):
        return r > 150 and g < 75 and b < 75 and (r - max(g, b)) > 85

    bg_vis = [[False] * h for _ in range(w)]
    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            r, g, b, _ = tpx[x, y]
            if is_red_bg(r, g, b):
                bg_vis[x][y] = True
                q.append((x, y))
    for y in range(h):
        for x in (0, w - 1):
            if not bg_vis[x][y]:
                r, g, b, _ = tpx[x, y]
                if is_red_bg(r, g, b):
                    bg_vis[x][y] = True
                    q.append((x, y))

    while q:
        cx, cy = q.popleft()
        for nx, ny in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
            if 0 <= nx < w and 0 <= ny < h and not bg_vis[nx][ny]:
                r, g, b, _ = tpx[nx, ny]
                if is_red_bg(r, g, b):
                    bg_vis[nx][ny] = True
                    q.append((nx, ny))

    # Erode 2px inward from background and de-spill red halo on border pixels
    mask = Image.new("L", (w, h), 0)
    mpx = mask.load()
    for x in range(2, w - 2):
        for y in range(2, h - 2):
            if not bg_vis[x][y]:
                near_bg = False
                for dx in (-2, -1, 0, 1, 2):
                    for dy in (-2, -1, 0, 1, 2):
                        if bg_vis[x + dx][y + dy]:
                            near_bg = True
                            break
                    if near_bg:
                        break
                r, g, b, a = tpx[x, y]
                if near_bg:
                    # Clamp red channel to neutral stone/foliage tone to eliminate red fringe
                    nr = min(r, max(g, b) + 12)
                    tpx[x, y] = (nr, g, b, 210)
                    mpx[x, y] = 180
                else:
                    mpx[x, y] = 255

    smooth_m = mask.filter(ImageFilter.GaussianBlur(radius=0.9))
    sm_px = smooth_m.load()
    for x in range(w):
        for y in range(h):
            r, g, b, _ = tpx[x, y]
            a = sm_px[x, y]
            tpx[x, y] = (r, g, b, a if a > 32 else 0)

    bbox = tile.getbbox()
    if bbox:
        tile = tile.crop(bbox)
    return tile


def paste_scaled(canvas: Image.Image, tile: Image.Image, center_x: int, bottom_y: int, target_w: int):
    """Pastes an RGBA isometric tile onto `canvas` anchored by its bottom-center point."""
    w, h = tile.size
    scale = target_w / float(w)
    nw = max(1, int(w * scale))
    nh = max(1, int(h * scale))
    resized = tile.resize((nw, nh), Image.Resampling.LANCZOS)
    x0 = int(center_x - nw // 2)
    y0 = int(bottom_y - nh)
    canvas.alpha_composite(resized, (x0, y0))


def build_composite_global_dioramas(
    jamestown_day: Image.Image,
    washington_dc_day: Image.Image,
    grand_canyon_day: Image.Image,
) -> dict[str, Image.Image]:
    """
    Builds 5 global UNESCO / World Capital 3D isometric dioramas on top of the
    Jamestown, Washington DC, and Grand Canyon translucent turquoise cutaway water
    island bases using de-spilled 8k isometric landmark models from `amilich/isometric-city`.
    """
    composites = {}

    # 1. ROME, ATHENS & MEDITERRANEAN (Colosseum, Parthenon, Roman Aqueduct, Pantheon Basilica, Triumphal Arch & Pharos Lighthouse)
    rome = jamestown_day.copy()
    colosseum = extract_isocity_tile("classics.png", 3, 2)
    parthenon = extract_isocity_tile("classics.png", 1, 0)
    aqueduct = extract_isocity_tile("classics.png", 2, 0)
    basilica = extract_isocity_tile("classics.png", 0, 3)
    arch = extract_isocity_tile("classics.png", 1, 5)
    pharos = extract_isocity_tile("classics.png", 4, 2)
    villa = extract_isocity_tile("classics.png", 1, 3)

    paste_scaled(rome, aqueduct, 545, 345, 220)
    paste_scaled(rome, parthenon, 705, 355, 210)
    paste_scaled(rome, basilica, 480, 445, 215)
    paste_scaled(rome, colosseum, 665, 470, 250)
    paste_scaled(rome, villa, 845, 420, 185)
    paste_scaled(rome, arch, 560, 520, 145)
    paste_scaled(rome, pharos, 985, 470, 165)
    composites["rome_mediterranean"] = rome

    # 2. TOKYO, KYOTO, BEIJING & EAST ASIA (Pagoda, Forbidden City Imperial Hall, Koi Bridge Garden & Turquoise Water)
    east_asia = jamestown_day.copy()
    pagoda = extract_isocity_tile("sprites_red_water_new_china.png", 0, 0)
    pavilion = extract_isocity_tile("sprites_red_water_new_china.png", 1, 0)
    imperial_gate = extract_isocity_tile("sprites_red_water_new_china.png", 3, 0)
    imperial_hall = extract_isocity_tile("sprites_red_water_new_china.png", 2, 3)
    koi_garden = extract_isocity_tile("sprites_red_water_new_china.png", 1, 1)
    lantern_plaza = extract_isocity_tile("sprites_red_water_new_china.png", 4, 5)
    junk_dock = extract_isocity_tile("sprites_red_water_new_china.png", 1, 5)

    paste_scaled(east_asia, imperial_gate, 650, 340, 220)
    paste_scaled(east_asia, pagoda, 500, 435, 195)
    paste_scaled(east_asia, imperial_hall, 675, 455, 235)
    paste_scaled(east_asia, pavilion, 845, 415, 190)
    paste_scaled(east_asia, koi_garden, 565, 515, 195)
    paste_scaled(east_asia, lantern_plaza, 760, 505, 175)
    paste_scaled(east_asia, junk_dock, 935, 485, 185)
    composites["tokyo_east_asia"] = east_asia

    # 3. LONDON & BRITISH ISLES (Big Ben Parliament Clock Tower, Tower of London Keep, Westminster Abbey, Thames Bridge & Red Phone Box)
    london = jamestown_day.copy()
    tower_keep = extract_isocity_tile("medeival.png", 0, 0)
    abbey = extract_isocity_tile("medeival.png", 3, 5)
    parliament_clock = extract_isocity_tile("industrial.png", 2, 5)
    thames_bridge = extract_isocity_tile("enlightenment.png", 1, 5)
    red_bus = extract_isocity_tile("sprites_red_water_new_harry.png", 4, 2)
    phone_box = extract_isocity_tile("sprites_red_water_new_harry.png", 1, 5)
    ferris_eye = extract_isocity_tile("industrial.png", 4, 5)

    paste_scaled(london, tower_keep, 535, 365, 215)
    paste_scaled(london, abbey, 725, 375, 215)
    paste_scaled(london, parliament_clock, 635, 475, 235)
    paste_scaled(london, ferris_eye, 855, 435, 195)
    paste_scaled(london, phone_box, 485, 475, 165)
    paste_scaled(london, red_bus, 775, 500, 155)
    paste_scaled(london, thames_bridge, 555, 545, 190)
    composites["london_uk"] = london

    # 4. PARIS & WESTERN EUROPE (Built on the Cherry-Blossom & Turquoise River Island Base + Notre-Dame, Louvre Palace, Arc de Triomphe, Spire Tower & Seine Bridge)
    paris = washington_dc_day.copy()
    palace = extract_isocity_tile("enlightenment.png", 4, 0)
    fountain_garden = extract_isocity_tile("enlightenment.png", 0, 1)
    seine_bridge = extract_isocity_tile("enlightenment.png", 1, 5)
    notre_dame = extract_isocity_tile("medeival.png", 3, 5)
    arc_triomphe = extract_isocity_tile("classics.png", 1, 5)
    gothic_spire = extract_isocity_tile("sprites_red_water_new_harry.png", 0, 0)
    carousel = extract_isocity_tile("enlightenment.png", 4, 5)

    paste_scaled(paris, palace, 420, 395, 240)
    paste_scaled(paris, gothic_spire, 690, 415, 215)
    paste_scaled(paris, notre_dame, 945, 365, 220)
    paste_scaled(paris, fountain_garden, 545, 465, 195)
    paste_scaled(paris, arc_triomphe, 315, 485, 165)
    paste_scaled(paris, carousel, 825, 430, 165)
    paste_scaled(paris, seine_bridge, 1055, 495, 190)
    composites["paris_europe"] = paris

    # 5. CAIRO, ANCIENT WONDERS & NILE RIVER (Built on the Stratified Sandstone & Turquoise River Canyon Base + Sacred Torch Temple, Pharos of Alexandria, Stone Citadel & Chariot Temple)
    cairo = grand_canyon_day.copy()
    sacred_pool = extract_isocity_tile("classics.png", 2, 2)
    chariot_temple = extract_isocity_tile("classics.png", 0, 5)
    citadel_fort = extract_isocity_tile("classics.png", 3, 1)
    quarry_ruins = extract_isocity_tile("classics.png", 2, 4)

    paste_scaled(cairo, citadel_fort, 675, 285, 210)
    paste_scaled(cairo, sacred_pool, 615, 385, 215)
    paste_scaled(cairo, chariot_temple, 425, 455, 200)
    paste_scaled(cairo, quarry_ruins, 945, 465, 195)
    paste_scaled(cairo, pharos, 755, 535, 175)
    composites["cairo_giza"] = cairo

    return composites


def build_sunset_and_night_variants(day_img: Image.Image) -> tuple[Image.Image, Image.Image]:
    """Generates Golden Hour Sunset and Starry Night (with warm glowing windows) RGBA variants."""
    w, h = day_img.size
    dpx = day_img.load()

    sunset = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    night = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    spx = sunset.load()
    npx = night.load()

    for x in range(w):
        for y in range(h):
            r, g, b, a = dpx[x, y]
            if a == 0:
                continue

            sr = min(255, int(r * 1.13 + 12))
            sg = min(255, int(g * 0.92))
            sb = min(255, int(b * 0.79))
            spx[x, y] = (sr, sg, sb, a)

            lum = 0.299 * r + 0.587 * g + 0.114 * b
            if lum > 185 and r > 175 and g > 145:
                nr = min(255, int(r * 1.08))
                ng = min(255, int(g * 0.94))
                nb = int(b * 0.58)
            else:
                nr = int(r * 0.34)
                ng = int(g * 0.44)
                nb = min(255, int(b * 0.68 + 14))
            npx[x, y] = (nr, ng, nb, a)

    return sunset, night


def save_theme_set(key: str, day_img: Image.Image):
    """Saves Day, Sunset, and Night PNGs for `key` into all 3 platform image directories."""
    sunset_img, night_img = build_sunset_and_night_variants(day_img)
    for d in OUT_DIRS:
        os.makedirs(d, exist_ok=True)
        day_img.save(os.path.join(d, f"{key}_island.png"), optimize=True)
        sunset_img.save(os.path.join(d, f"{key}_island_sunset.png"), optimize=True)
        night_img.save(os.path.join(d, f"{key}_island_night.png"), optimize=True)
    print(f"Saved {key}_island[_sunset|_night].png")


def main():
    extracted_days = {}
    for key, src_path in DIRECT_SOURCES.items():
        day_cutout = extract_clean_island_cutout(src_path)
        extracted_days[key] = day_cutout
        save_theme_set(key, day_cutout)

    composites = build_composite_global_dioramas(
        extracted_days["jamestown"],
        extracted_days["washington_dc"],
        extracted_days["grand_canyon"],
    )
    for key, comp_day in composites.items():
        save_theme_set(key, comp_day)


if __name__ == "__main__":
    main()
