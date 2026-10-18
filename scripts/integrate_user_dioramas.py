#!/usr/bin/env python3
"""
integrate_user_dioramas.py
==========================
Processes and integrates the 5 high-fidelity 3D isometric diorama models
generated with quadrant-zoned architectural prompts:
  1. `virginia_beach`   (Coastal city with turquoise water cube, bridge spanning shore to shore, hotels & lighthouse)
  2. `colorado_springs` (Mountain plateau with visible geological rock strata, alpine chalets, stream & waterfall)
  3. `las_vegas`        (Golden Hour desert resort plaza with central fountain lagoon, luxury towers & palm trees)
  4. `orlando`          (Miniature fantasy theme park with fairytale castle, lagoon fountain & roller coaster)
  5. `atlanta`          (Downtown Atlanta with Piedmont Park pond, elevated transit rail line & skyscrapers)

Also updates generic fallbacks `coastal` and `alpine` with these upgraded renders.
Outputs Day, Sunset, and Night RGBA PNGs (1376x768) to web, Android assets, and KDE package.
"""

import os
from collections import deque
from PIL import Image, ImageFilter

ROOT_DIR = "/home/gcerkez/Documents/antigravity/gallant-pasteur"
BRAIN_USER_DIR = "/home/gcerkez/.gemini/antigravity/brain/9d13020f-0e3a-4e92-8852-af20399e39f1/.user_uploaded"

DEST_DIRS = [
    os.path.join(ROOT_DIR, "web/images"),
    os.path.join(ROOT_DIR, "android/app/src/main/assets/web/images"),
    os.path.join(ROOT_DIR, "package/contents/images"),
]

DIORAMAS = [
    {
        "keys": ["virginia_beach", "coastal"],
        "src": os.path.join(BRAIN_USER_DIR, "media_1792343235441.webp"),
        "clean_badge": False,
        "step_d": 6.0,
    },
    {
        "keys": ["colorado_springs", "alpine"],
        "src": os.path.join(BRAIN_USER_DIR, "media_1792343301237.webp"),
        "clean_badge": True,
        "step_d": 6.0,
    },
    {
        "keys": ["las_vegas"],
        "src": os.path.join(BRAIN_USER_DIR, "media_1792343493360.jpg"),
        "clean_badge": False,
        "step_d": 7.0,
    },
    {
        "keys": ["orlando"],
        "src": os.path.join(BRAIN_USER_DIR, "media_1792343560274.jpg"),
        "clean_badge": False,
        "step_d": 6.5,
    },
    {
        "keys": ["atlanta"],
        "src": os.path.join(BRAIN_USER_DIR, "media_1792343623228.jpg"),
        "clean_badge": False,
        "step_d": 6.5,
    },
]


def isolate_diorama_cube(src_path: str, clean_badge: bool = False, step_d: float = 6.5) -> Image.Image:
    """Isolates the isometric cube with clean smooth anti-aliased alpha edge and soft contact shadow."""
    im = Image.open(src_path).convert("RGB")
    w, h = im.size
    px = im.load()

    if clean_badge:
        # Inpaint bottom-right watermark pill using smooth background reference
        for y in range(580, h):
            ref = px[495, y]
            for x in range(500, w):
                px[x, y] = ref

    visited = [[False] * h for _ in range(w)]
    q = deque()

    # Seed all four borders
    for x in range(w):
        visited[x][0] = True
        q.append((x, 0))
        visited[x][h - 1] = True
        q.append((x, h - 1))
    for y in range(h):
        if not visited[0][y]:
            visited[0][y] = True
            q.append((0, y))
        if not visited[w - 1][y]:
            visited[w - 1][y] = True
            q.append((w - 1, y))

    # Neighbor gradient flood fill
    while q:
        cx, cy = q.popleft()
        cr, cg, cb = px[cx, cy]
        for nx, ny in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
            if 0 <= nx < w and 0 <= ny < h and not visited[nx][ny]:
                nr, ng, nb = px[nx, ny]
                d = ((nr - cr) ** 2 + (ng - cg) ** 2 + (nb - cb) ** 2) ** 0.5
                if d < step_d:
                    visited[nx][ny] = True
                    q.append((nx, ny))

    # Create foreground mask
    mask = Image.new("L", (w, h), 0)
    mpx = mask.load()
    for x in range(w):
        for y in range(h):
            if not visited[x][y]:
                mpx[x, y] = 255

    smooth_mask = mask.filter(ImageFilter.GaussianBlur(radius=1.2))
    sm_px = smooth_mask.load()

    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    opx = out.load()
    for x in range(w):
        for y in range(h):
            a = sm_px[x, y]
            if a > 12:
                opx[x, y] = (*px[x, y], a)

    # Add soft neutral contact shadow beneath the cube
    shadow_mask = mask.filter(ImageFilter.GaussianBlur(radius=14))
    sh_px = shadow_mask.load()
    shadow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    sopx = shadow.load()
    for x in range(w):
        for y in range(h - 10):
            sa = sh_px[x, y]
            if sa > 0:
                sopx[x, y + 10] = (15, 25, 38, int(sa * 0.30))
    out = Image.alpha_composite(shadow, out)

    bbox = out.getbbox()
    return out.crop(bbox)


def fit_to_frame(cropped: Image.Image, canvas_w: int = 1376, canvas_h: int = 768, target_h: int = 690) -> Image.Image:
    """Proportionally scales and centers the isolated diorama cube onto the standard 1376x768 frame."""
    cw, ch = cropped.size
    scale = target_h / ch
    tw = int(cw * scale)
    th = int(ch * scale)
    if tw > 1180:
        scale = 1180 / cw
        tw = int(cw * scale)
        th = int(ch * scale)

    resized = cropped.resize((tw, th), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    x0 = (canvas_w - tw) // 2
    y0 = (canvas_h - th) // 2 + 10
    canvas.alpha_composite(resized, (x0, y0))
    return canvas


def build_sunset_and_night_variants(day_img: Image.Image) -> tuple[Image.Image, Image.Image]:
    """Generates golden hour sunset and glowing-window starry night variants."""
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

            # Sunset: warm golden-amber lighting
            sr = min(255, int(r * 1.13 + 12))
            sg = min(255, int(g * 0.92))
            sb = min(255, int(b * 0.79))
            spx[x, y] = (sr, sg, sb, a)

            # Night: deep blue/indigo ambient with warm glowing windows and lamps
            lum = 0.299 * r + 0.587 * g + 0.114 * b
            if lum > 175 and r > 165 and g > 140:
                # Emissive warm golden window light
                nr = min(255, int(r * 1.10))
                ng = min(255, int(g * 0.94))
                nb = int(b * 0.55)
            else:
                nr = int(r * 0.33)
                ng = int(g * 0.43)
                nb = min(255, int(b * 0.69 + 14))
            npx[x, y] = (nr, ng, nb, a)

    return sunset, night


def main():
    print("Beginning integration of user 3D isometric dioramas...")
    for item in DIORAMAS:
        src = item["src"]
        clean_b = item["clean_badge"]
        step = item["step_d"]
        keys = item["keys"]

        print(f"\nProcessing {keys[0]} from {os.path.basename(src)}...")
        cropped = isolate_diorama_cube(src, clean_badge=clean_b, step_d=step)
        framed = fit_to_frame(cropped)
        sunset, night = build_sunset_and_night_variants(framed)

        for key in keys:
            for d in DEST_DIRS:
                os.makedirs(d, exist_ok=True)
                framed.save(os.path.join(d, f"{key}_island.png"), optimize=True)
                sunset.save(os.path.join(d, f"{key}_island_sunset.png"), optimize=True)
                night.save(os.path.join(d, f"{key}_island_night.png"), optimize=True)
            print(f"  ✓ Installed {key}_island[.png|_sunset.png|_night.png] to web, android, & package")

    print("\nAll new user dioramas successfully integrated across all platforms!")


if __name__ == "__main__":
    main()
