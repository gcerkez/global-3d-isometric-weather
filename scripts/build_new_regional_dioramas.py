#!/usr/bin/env python3
"""
build_new_regional_dioramas.py
==============================
Generates 10 new regional and landmark 3D isometric dioramas requested by the user:
  1. `virginia_beach`    — Virginia Beach Oceanfront, Boardwalk, Amusement Pier & Cape Henry Lighthouse
  2. `chesapeake_bay`    — Chesapeake Bay Bridge-Tunnel, Salt Marshes, Oyster Boats & Pine Headland
  3. `myrtle_beach`      — Myrtle Beach Grand Strand, SkyWheel Ferris Wheel, Pier & Turquoise Surf
  4. `orlando`           — Orlando Theme Parks, Fairytale Castle Spire, Roller Coaster & Lake Eola
  5. `atlanta`           — Atlanta Peachtree Skyline, Modern Glass Towers, Centennial Olympic Park & Capitol
  6. `denver`            — Denver Mile High Skyline, Historic Union Station, Rockies Foothills & Capitol Dome
  7. `colorado_springs`  — Colorado Springs, Garden of the Gods Red Sandstone Spires & Mountain Alpine Base
  8. `texas`             — Texas State Capitol Pink Granite Dome, San Antonio River Walk Bridge & Oil Derrick
  9. `arizona`           — Arizona Sedona Red Rocks, Adobe Mission, Desert Oasis Pool & Sonoran Cacti
  10. `las_vegas`        — Las Vegas Strip, Neon Mega-Resort Towers, High Roller Wheel & Bellagio Fountains

Each diorama is built upon our high-res translucent turquoise cutaway water/earth floating island
bases combined with clean 3D isometric architectural models from `amilich/isometric-city`.
Saves Day, Sunset, and Night RGBA PNGs (1376x768) into web, Android assets, and KDE package.
"""

import os
from collections import deque
from PIL import Image, ImageFilter

ROOT_DIR = "/home/gcerkez/Documents/antigravity/gallant-pasteur"
ISOCITY_DIR = "/tmp/isocity_assets"
OUT_DIRS = [
    os.path.join(ROOT_DIR, "package/contents/images"),
    os.path.join(ROOT_DIR, "web/images"),
    os.path.join(ROOT_DIR, "android/app/src/main/assets/web/images"),
]


def extract_isocity_tile(sheet_name: str, col: int, row: int, cols: int = 5, rows: int = 6) -> Image.Image:
    """Extracts and de-spills an isometric tile from sprite sheets in ISOCITY_DIR."""
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
    """Pastes an RGBA isometric tile onto canvas anchored by its bottom-center point."""
    w, h = tile.size
    scale = target_w / float(w)
    nw = max(1, int(w * scale))
    nh = max(1, int(h * scale))
    resized = tile.resize((nw, nh), Image.Resampling.LANCZOS)
    x0 = int(center_x - nw // 2)
    y0 = int(bottom_y - nh)
    canvas.alpha_composite(resized, (x0, y0))


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
    print(f"✓ Saved {key}_island[.png|_sunset.png|_night.png]")


def main():
    print("Loading base diorama islands...")
    # Load pristine base islands from web/images
    base_coastal = Image.open(f"{ROOT_DIR}/web/images/coastal_island.png").convert("RGBA")
    base_miami = Image.open(f"{ROOT_DIR}/web/images/miami_caribbean_island.png").convert("RGBA")
    base_canyon = Image.open(f"{ROOT_DIR}/web/images/grand_canyon_island.png").convert("RGBA")
    base_seattle = Image.open(f"{ROOT_DIR}/web/images/seattle_pnw_island.png").convert("RGBA")
    base_dc = Image.open(f"{ROOT_DIR}/web/images/washington_dc_island.png").convert("RGBA")
    base_alpine = Image.open(f"{ROOT_DIR}/web/images/alpine_island.png").convert("RGBA")
    base_chicago = Image.open(f"{ROOT_DIR}/web/images/chicago_great_lakes_island.png").convert("RGBA")

    print("Extracting 3D architectural landmark tiles...")
    # Tiles from modern.png
    skyscraper_glass = extract_isocity_tile("modern.png", 0, 0)
    corp_tower = extract_isocity_tile("modern.png", 1, 0)
    amusement_park = extract_isocity_tile("modern.png", 4, 5)   # Ferris wheel, roller coaster & carousel
    capitol_dome = extract_isocity_tile("modern.png", 2, 5)     # Classical dome
    fountain_plaza = extract_isocity_tile("modern.png", 0, 1)   # Square fountain plaza
    park_pond = extract_isocity_tile("modern.png", 1, 1)        # Scenic pond with gazebo
    luxury_resort = extract_isocity_tile("modern.png", 2, 3)    # Villa with swimming pool
    stadium = extract_isocity_tile("modern.png", 3, 2)          # Arena stadium

    # Tiles from enlightenment.png
    stone_bridge = extract_isocity_tile("enlightenment.png", 1, 5)
    manor_house = extract_isocity_tile("enlightenment.png", 0, 0)
    colonial_hall = extract_isocity_tile("enlightenment.png", 1, 0)
    harbor_pier = extract_isocity_tile("enlightenment.png", 4, 2)  # Lighthouse, pier & tall ship
    formal_garden = extract_isocity_tile("enlightenment.png", 0, 1)

    # Tiles from classics.png
    pharos_lighthouse = extract_isocity_tile("classics.png", 4, 2)
    sacred_torch_pool = extract_isocity_tile("classics.png", 2, 2) # Marble reflecting pool with torches
    adobe_mission = extract_isocity_tile("classics.png", 0, 5)     # Mission temple
    rock_quarry = extract_isocity_tile("classics.png", 2, 4)       # Terraced red-rock cliffs
    harbor_cranes = extract_isocity_tile("classics.png", 4, 4)     # Industrial wharf & boats
    colosseum = extract_isocity_tile("classics.png", 3, 2)

    # Tiles from industrial.png
    oil_derrick = extract_isocity_tile("industrial.png", 4, 2)     # Oil pump jacks & refinery
    union_station = extract_isocity_tile("industrial.png", 0, 5)   # Historic train depot with steam engine
    city_hall = extract_isocity_tile("industrial.png", 2, 5)       # Victorian clock tower / hall
    water_tower = extract_isocity_tile("industrial.png", 1, 2)

    # Tiles from sprites_red_water_new_harry.png
    castle_spire = extract_isocity_tile("sprites_red_water_new_harry.png", 0, 0)

    print("Compositing 10 new regional landmark dioramas...")

    # 1. VIRGINIA BEACH, VA (Boardwalk, Amusement Pier, Luxury Resort & Cape Henry Lighthouse)
    vb = base_miami.copy()
    paste_scaled(vb, luxury_resort, 430, 420, 210)
    paste_scaled(vb, amusement_park, 680, 435, 240)
    paste_scaled(vb, fountain_plaza, 540, 505, 175)
    paste_scaled(vb, pharos_lighthouse, 925, 460, 175)
    save_theme_set("virginia_beach", vb)

    # 2. CHESAPEAKE BAY, VA (Bay Bridge-Tunnel, Oyster Wharfs, Lighthouse & Pine Headland)
    cb = base_coastal.copy()
    paste_scaled(cb, stone_bridge, 520, 395, 245)
    paste_scaled(cb, harbor_cranes, 785, 435, 215)
    paste_scaled(cb, colonial_hall, 425, 465, 185)
    paste_scaled(cb, harbor_pier, 955, 485, 185)
    save_theme_set("chesapeake_bay", cb)

    # 3. MYRTLE BEACH, SC (The Grand Strand, SkyWheel, Ocean Blvd High-Rise & Beach Pier)
    mb = base_miami.copy()
    paste_scaled(mb, skyscraper_glass, 410, 395, 185)
    paste_scaled(mb, amusement_park, 670, 430, 245)
    paste_scaled(mb, luxury_resort, 885, 440, 195)
    paste_scaled(mb, harbor_pier, 530, 520, 180)
    save_theme_set("myrtle_beach", mb)

    # 4. ORLANDO, FL (Theme Parks, Fairytale Castle Spire, Roller Coaster & Lake Eola)
    orl = base_miami.copy()
    paste_scaled(orl, castle_spire, 470, 385, 210)
    paste_scaled(orl, amusement_park, 715, 425, 240)
    paste_scaled(orl, park_pond, 530, 505, 195)
    paste_scaled(orl, luxury_resort, 890, 460, 190)
    save_theme_set("orlando", orl)

    # 5. ATLANTA, GA (Peachtree Skyline, Modern Glass Towers, Centennial Park & Georgia Capitol)
    atl = base_chicago.copy()
    paste_scaled(atl, skyscraper_glass, 440, 375, 205)
    paste_scaled(atl, corp_tower, 680, 365, 220)
    paste_scaled(atl, capitol_dome, 560, 465, 215)
    paste_scaled(atl, fountain_plaza, 820, 470, 185)
    paste_scaled(atl, stadium, 350, 480, 180)
    save_theme_set("atlanta", atl)

    # 6. DENVER, CO (Mile High Skyline, Historic Union Station, Rockies Foothills & Capitol Dome)
    den = base_seattle.copy()
    paste_scaled(den, skyscraper_glass, 450, 370, 195)
    paste_scaled(den, capitol_dome, 690, 380, 215)
    paste_scaled(den, union_station, 520, 475, 210)
    paste_scaled(den, park_pond, 810, 480, 190)
    save_theme_set("denver", den)

    # 7. COLORADO SPRINGS, CO (Garden of the Gods Sandstone Spires, Pikes Peak Railway & Alpine Resort)
    cs = base_alpine.copy()
    paste_scaled(cs, rock_quarry, 500, 365, 230)
    paste_scaled(cs, union_station, 740, 405, 210)
    paste_scaled(cs, manor_house, 430, 475, 195)
    paste_scaled(cs, stone_bridge, 680, 495, 190)
    save_theme_set("colorado_springs", cs)

    # 8. TEXAS (Austin Capitol Pink Granite Dome, San Antonio River Walk Bridge & Oil Derrick)
    tx = base_dc.copy()
    paste_scaled(tx, capitol_dome, 520, 370, 240)
    paste_scaled(tx, stone_bridge, 730, 435, 225)
    paste_scaled(tx, oil_derrick, 350, 455, 190)
    paste_scaled(tx, adobe_mission, 890, 445, 185)
    paste_scaled(tx, colonial_hall, 580, 495, 180)
    save_theme_set("texas", tx)

    # 9. ARIZONA (Sedona Red Rocks, Desert Oasis Sacred Pool, Adobe Mission & Sonoran Cacti)
    az = base_canyon.copy()
    paste_scaled(az, rock_quarry, 490, 335, 230)
    paste_scaled(az, sacred_torch_pool, 690, 395, 215)
    paste_scaled(az, adobe_mission, 380, 445, 195)
    paste_scaled(az, corp_tower, 880, 415, 185)
    save_theme_set("arizona", az)

    # 10. LAS VEGAS, NV (The Strip, Neon Towers, High Roller Wheel & Bellagio Fountains)
    lv = base_canyon.copy()
    paste_scaled(lv, skyscraper_glass, 410, 345, 205)
    paste_scaled(lv, amusement_park, 660, 375, 235)
    paste_scaled(lv, sacred_torch_pool, 530, 465, 225)
    paste_scaled(lv, colosseum, 850, 435, 215)
    paste_scaled(lv, luxury_resort, 360, 490, 180)
    save_theme_set("las_vegas", lv)

    print("All 10 new regional landmark dioramas successfully generated and saved!")


if __name__ == "__main__":
    main()
