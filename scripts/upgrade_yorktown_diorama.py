#!/usr/bin/env python3
"""
upgrade_yorktown_diorama.py
===========================
Upgrades the Historic Yorktown, VA 3D isometric diorama (`yorktown_island*.png`)
using the exact same composite technique as the other 17 pre-generated renders:
- Starts from the 8k Virginia Coastal / Jamestown floating island base (with the
  rounded stratified geological sandstone cliff cross-section, lush pine & oak
  forests, sandy shoreline, and translucent turquoise cutaway York River block).
- Cleanly extracts and composites Yorktown's signature historical & modern landmarks
  from the Yorktown 3D source + open-source Enlightenment colonial assets:
  1. Colonial Brick Main Street (Custom House, Nelson House, Swan Tavern,
     cobblestone streets, and horse-drawn carriages) in the central town plateau
  2. Yorktown Victory Monument rising atop the upper-right grassy bluff
  3. 1781 Revolutionary War Earthwork Siege Redoubts & Bronze Artillery Cannons
  4. George P. Coleman Memorial Double-Swing Steel Truss Bridge (with transparent
     steel lattice trusses) spanning the translucent turquoise York River
  5. 18th-Century Three-Masted American Tall Ship & Schooners on the York River
"""

import os
from collections import deque
from PIL import Image, ImageFilter

from build_all_pregenerated_dioramas import (
    BRAIN_DIR,
    extract_isocity_tile,
    paste_scaled,
    save_theme_set,
)


def extract_clean_virginia_island_base(img_path: str) -> Image.Image:
    """
    Extracts the 8k Virginia Coastal / Jamestown floating island cutout with 100%
    removal of both the sky background and the studio floor cast shadow (where
    `b - g > 26 and b - r > 55`), preserving the rounded geological sandstone cliff
    and translucent turquoise cutaway water block.
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
        tol = 58 if y < int(h * 0.56) else 46
        if dist < tol:
            return True
        # Studio floor cast shadow has strong blue dominance over green (b - g > 26)
        # whereas translucent turquoise water has high green (b - g <= 18).
        if (b - g) > 26 and (b - r) > 52 and b > 125:
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

    # Erode 1px to strip the thin blue studio anti-aliasing fringe around the cliff/trees
    keep_mask = Image.new("L", (w, h), 0)
    kpx = keep_mask.load()
    for ux, uy in best_comp:
        kpx[ux, uy] = 255

    eroded_mask = keep_mask.filter(ImageFilter.MinFilter(3))
    smooth_mask = eroded_mask.filter(ImageFilter.GaussianBlur(radius=1.0))
    spx = smooth_mask.load()

    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    opx = out.load()
    for x in range(w):
        for y in range(h):
            a = spx[x, y]
            if a > 18:
                r, g, b, _ = px[x, y]
                opx[x, y] = (r, g, b, min(255, int(a * 1.05)))

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


def extract_yorktown_main_street(yt_src: Image.Image) -> Image.Image:
    """
    Extracts the Georgian red-brick Colonial Main Street block (Custom House,
    Nelson House, Swan Tavern, cobblestone street, trees, and horse-drawn carriages)
    from `yorktown_isometric_diorama_1792260180150.jpg` with an isometric diamond mask
    and soft feathered edges so it blends seamlessly onto the island plateau.
    """
    crop_box = (395, 360, 965, 675)
    sub = yt_src.crop(crop_box).convert("RGBA")
    w, h = sub.size
    spx = sub.load()

    mask = Image.new("L", (w, h), 0)
    mpx = mask.load()

    for x in range(w):
        for y in range(h):
            r, g, b, _ = spx[x, y]
            if b > r + 20 and b > g + 10:
                continue
            left_limit_y = 165 + (x - 15) * (302 - 165) / max(1.0, (285 - 15))
            right_limit_y = 302 - (x - 285) * (302 - 175) / max(1.0, (558 - 285))
            top_left_y = 165 - (x - 15) * (165 - 8) / max(1.0, (325 - 15))
            top_right_y = 8 + (x - 325) * (175 - 8) / max(1.0, (558 - 325))

            if x < 285 and y > left_limit_y:
                continue
            if x >= 285 and y > right_limit_y:
                continue
            if x < 325 and y < top_left_y:
                continue
            if x >= 325 and y < top_right_y:
                continue

            if y > 200 and r > 145 and g < 115 and b < 85 and (r - g) > 35 and (g - b) > 20:
                dist_to_bottom = (left_limit_y - y) if x < 285 else (right_limit_y - y)
                if dist_to_bottom < 22:
                    continue

            mpx[x, y] = 255

    smooth_mask = mask.filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(radius=4.5))
    sm_px = smooth_mask.load()

    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    opx = out.load()
    for x in range(w):
        for y in range(h):
            a = sm_px[x, y]
            if a > 8:
                r, g, b, _ = spx[x, y]
                nr = min(255, int(r * 0.98))
                ng = min(255, int(g * 0.97))
                nb = min(255, int(b * 0.95))
                opx[x, y] = (nr, ng, nb, a)

    return out


def extract_victory_monument_and_redoubts(yt_src: Image.Image) -> Image.Image:
    """
    Extracts the Yorktown Victory Monument (tall white column + pedestal + curved
    walkway + surrounding trees + 1781 earthen siege redoubts with bronze cannons)
    from `yorktown_isometric_diorama_1792260180150.jpg`, removing the sky background
    and lower brown cliff wall.
    """
    crop_box = (720, 40, 1255, 495)
    sub = yt_src.crop(crop_box).convert("RGBA")
    w, h = sub.size
    spx = sub.load()

    sky_r, sky_g, sky_b = spx[10, 10][:3]

    def is_sky_or_water(r, g, b):
        dist_sky = ((r - sky_r) ** 2 + (g - sky_g) ** 2 + (b - sky_b) ** 2) ** 0.5
        if dist_sky < 46:
            return True
        if b > r + 22 and b > g + 8 and r < 145:
            return True
        return False

    bg_vis = [[False] * h for _ in range(w)]
    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            r, g, b, _ = spx[x, y]
            if is_sky_or_water(r, g, b):
                bg_vis[x][y] = True
                q.append((x, y))
    for y in range(h):
        for x in (0, w - 1):
            if not bg_vis[x][y]:
                r, g, b, _ = spx[x, y]
                if is_sky_or_water(r, g, b):
                    bg_vis[x][y] = True
                    q.append((x, y))

    while q:
        cx, cy = q.popleft()
        for nx, ny in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
            if 0 <= nx < w and 0 <= ny < h and not bg_vis[nx][ny]:
                r, g, b, _ = spx[nx, ny]
                if is_sky_or_water(r, g, b):
                    bg_vis[nx][ny] = True
                    q.append((nx, ny))

    mask = Image.new("L", (w, h), 0)
    mpx = mask.load()

    for x in range(w):
        for y in range(h):
            if bg_vis[x][y]:
                continue
            r, g, b, _ = spx[x, y]
            bluff_edge_y = 445 - (x - 260) * (445 - 340) / max(1.0, (525 - 260))
            if x > 220 and y > bluff_edge_y:
                continue
            if y > 435:
                continue
            if x < 45 and y > 260:
                continue
            mpx[x, y] = 255

    eroded = mask.filter(ImageFilter.MinFilter(3))
    blurred_base = eroded.filter(ImageFilter.GaussianBlur(radius=3.5))
    sharp_top = eroded.filter(ImageFilter.GaussianBlur(radius=0.8))

    epx = sharp_top.load()
    bpx = blurred_base.load()

    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    opx = out.load()
    for x in range(w):
        for y in range(h):
            a = epx[x, y] if y < 220 else min(epx[x, y], bpx[x, y])
            if y > 380:
                a = int(a * max(0.0, (435 - y) / 55.0))
            if a > 12:
                r, g, b, _ = spx[x, y]
                opx[x, y] = (r, g, b, a)

    return out


def extract_coleman_bridge(yt_src: Image.Image) -> Image.Image:
    """
    Extracts the George P. Coleman Memorial Double-Swing Steel Truss Bridge
    and its concrete swing pier from `yorktown_isometric_diorama_1792260180150.jpg`,
    keying out all flat blue water pixels AND excluding the tall ship's top sails
    in the bottom-left corner of the crop (`x < 95 and y > 68`).
    """
    crop_box = (335, 195, 695, 385)
    sub = yt_src.crop(crop_box).convert("RGBA")
    w, h = sub.size
    spx = sub.load()

    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    opx = out.load()

    for x in range(w):
        for y in range(h):
            if x < 92 and y > 68:
                continue
            r, g, b, _ = spx[x, y]
            is_blue_water = (b > r + 14 and b > g + 4) or (b > 95 and r < 85 and g < 115)
            is_beach_sand = (x > 240 and y > 105 and r > 195 and g > 170 and b < 165 and (r - b) > 38)
            if is_blue_water or is_beach_sand:
                continue

            if b > max(r, g):
                b = int((r + g) * 0.52)
            opx[x, y] = (r, g, b, 250)

    bbox = out.getbbox()
    if bbox:
        out = out.crop(bbox)
    return out


def extract_yorktown_tall_ship(yt_src: Image.Image) -> Image.Image:
    """
    Extracts the 18th-century three-masted American tall ship (with US flag)
    from `yorktown_isometric_diorama_1792260180150.jpg`, removing the flat blue water.
    """
    crop_box = (240, 275, 405, 445)
    sub = yt_src.crop(crop_box).convert("RGBA")
    w, h = sub.size
    spx = sub.load()

    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    opx = out.load()

    for x in range(w):
        for y in range(h):
            r, g, b, _ = spx[x, y]
            if (b > r + 12 and b > g + 4) or (r < 75 and g < 95 and b > 80):
                continue
            opx[x, y] = (r, g, b, 255)

    bbox = out.getbbox()
    if bbox:
        out = out.crop(bbox)
    return out


def build_upgraded_yorktown_diorama() -> Image.Image:
    """
    Constructs the upgraded Historic Yorktown, VA 3D isometric diorama on the
    8k Jamestown / Virginia coastal floating island base (rounded geological
    sandstone cliff cross-section, lush trees, sandy York River shoreline, and
    translucent turquoise cutaway water block).
    """
    jamestown_base = extract_clean_virginia_island_base(
        f"{BRAIN_DIR}/diorama_jamestown_va_1792269964503.jpg"
    )
    yt_src = Image.open(
        f"{BRAIN_DIR}/yorktown_isometric_diorama_1792260180150.jpg"
    ).convert("RGBA").resize((1376, 768), Image.Resampling.LANCZOS)

    canvas = jamestown_base.copy()

    # 1. Cover the upper-left wooden palisade wall with Georgian Colonial brick halls
    #    and lush trees before placing the main Yorktown street & Victory Monument
    colonial_hall = extract_isocity_tile("enlightenment.png", 2, 0)
    custom_house = extract_isocity_tile("enlightenment.png", 0, 2)
    paste_scaled(canvas, colonial_hall, center_x=545, bottom_y=325, target_w=175)
    paste_scaled(canvas, custom_house, center_x=435, bottom_y=365, target_w=160)

    # 2. Extract the Yorktown Victory Monument + 1781 Siege Redoubts & Bronze Cannons
    #    and place them on the upper-right grassy bluff of the island
    monument_redoubts = extract_victory_monument_and_redoubts(yt_src)
    paste_scaled(canvas, monument_redoubts, center_x=805, bottom_y=400, target_w=440)

    # 3. Extract Yorktown's Georgian Red-Brick Colonial Main Street (Custom House,
    #    Nelson House, Swan Tavern, cobblestone street & horse-drawn carriages)
    #    and composite it over the central plateau
    main_street = extract_yorktown_main_street(yt_src)
    paste_scaled(canvas, main_street, center_x=598, bottom_y=475, target_w=475)

    # 4. Extract the George P. Coleman Memorial Double-Swing Steel Truss Bridge
    #    spanning across the translucent turquoise York River cutaway channel
    coleman_bridge = extract_coleman_bridge(yt_src)
    paste_scaled(canvas, coleman_bridge, center_x=490, bottom_y=545, target_w=255)

    # 5. Extract Yorktown's 3-masted American Tall Ship (with US flag) and sail it
    #    directly on the translucent turquoise York River surface at (705, 575),
    #    replacing the front-left ship so all vessels float naturally on the water surface!
    tall_ship = extract_yorktown_tall_ship(yt_src)
    paste_scaled(canvas, tall_ship, center_x=705, bottom_y=575, target_w=145)

    return canvas


def main():
    upgraded_day = build_upgraded_yorktown_diorama()
    save_theme_set("yorktown", upgraded_day)
    upgraded_day.save(os.path.join(BRAIN_DIR, "yorktown_island_upgraded.png"), optimize=True)
    print("Upgraded Historic Yorktown, VA diorama complete!")


if __name__ == "__main__":
    main()
