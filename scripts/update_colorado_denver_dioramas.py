#!/usr/bin/env python3
"""
update_colorado_denver_dioramas.py
==================================
Isolates and installs the upgraded 3D isometric dioramas for:
  - Denver, CO (Mile High City, State Capitol, Union Station & Rockies)
  - Colorado Springs, CO (Pikes Peak, Garden of the Gods & Broadmoor)

Generates:
  - <theme>_island.png (Daylight)
  - <theme>_island_sunset.png (Golden Hour Sunset)
  - <theme>_island_night.png (Starry Night with glowing windows)

Outputs to:
  - package/contents/images/
  - web/images/
  - android/app/src/main/assets/web/images/
"""

import os
from collections import deque
from PIL import Image, ImageFilter

ROOT_DIR = "/home/gcerkez/Documents/antigravity/gallant-pasteur"
BRAIN_DIR = "/home/gcerkez/.gemini/antigravity/brain/a0d74981-204d-450c-a888-3e9e7e6616fc"

DEST_DIRS = [
    os.path.join(ROOT_DIR, "package/contents/images"),
    os.path.join(ROOT_DIR, "web/images"),
    os.path.join(ROOT_DIR, "android/app/src/main/assets/web/images"),
]

DIORAMAS = [
    {
        "keys": ["denver"],
        "src": os.path.join(BRAIN_DIR, "denver_diorama_1792510595129.jpg"),
        "step_d": 6.5,
        "clean_sky_box": (0, 0, 300, 270),
    },
    {
        "keys": ["colorado_springs"],
        "src": os.path.join(BRAIN_DIR, "colorado_diorama_1792510626940.jpg"),
        "step_d": 6.5,
        "clean_sky_box": None,
    },
]


def isolate_diorama_cube(src_path: str, step_d: float = 6.5, clean_sky_box: tuple = None) -> Image.Image:
    """Isolates the isometric cube from solid background with smooth anti-aliased alpha edge and soft contact shadow."""
    im = Image.open(src_path).convert("RGB")
    w, h = im.size
    px = im.load()

    bg_ref = px[0, 0]
    if clean_sky_box:
        x1, y1, x2, y2 = clean_sky_box
        for x in range(x1, min(w, x2)):
            for y in range(y1, min(h, y2)):
                px[x, y] = bg_ref

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

    # Flood fill background
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

    # Filter out detached small blobs (e.g. isolated floating sky clouds)
    fg_visited = [[False] * h for _ in range(w)]
    components = []
    for x in range(w):
        for y in range(h):
            if not visited[x][y] and not fg_visited[x][y]:
                comp = []
                cq = deque([(x, y)])
                fg_visited[x][y] = True
                while cq:
                    fx, fy = cq.popleft()
                    comp.append((fx, fy))
                    for nx, ny in ((fx - 1, fy), (fx + 1, fy), (fx, fy - 1), (fx, fy + 1)):
                        if 0 <= nx < w and 0 <= ny < h and not visited[nx][ny] and not fg_visited[nx][ny]:
                            fg_visited[nx][ny] = True
                            cq.append((nx, ny))
                components.append(comp)

    # The diorama is by far the largest component (hundreds of thousands of pixels).
    # Discard any small components (< 8000 pixels) which represent floating clouds.
    if components:
        max_size = max(len(c) for c in components)
        for c in components:
            if len(c) < max(5000, int(max_size * 0.05)):
                for fx, fy in c:
                    visited[fx][fy] = True

    # Foreground mask
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

    # Soft neutral contact shadow beneath the floating cube
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
                # Emissive warm golden window / architectural light
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
    for item in DIORAMAS:
        keys = item["keys"]
        src = item["src"]
        step_d = item["step_d"]

        print(f"\nProcessing {keys} from {src}...")
        cropped = isolate_diorama_cube(src, step_d=step_d, clean_sky_box=item.get("clean_sky_box"))
        day_canvas = fit_to_frame(cropped)
        sunset_canvas, night_canvas = build_sunset_and_night_variants(day_canvas)

        for dest_dir in DEST_DIRS:
            os.makedirs(dest_dir, exist_ok=True)
            for k in keys:
                day_path = os.path.join(dest_dir, f"{k}_island.png")
                sunset_path = os.path.join(dest_dir, f"{k}_island_sunset.png")
                night_path = os.path.join(dest_dir, f"{k}_island_night.png")

                day_canvas.save(day_path, "PNG", optimize=True)
                sunset_canvas.save(sunset_path, "PNG", optimize=True)
                night_canvas.save(night_path, "PNG", optimize=True)
                print(f"  -> Saved {day_path}")
                print(f"  -> Saved {sunset_path}")
                print(f"  -> Saved {night_path}")

    print("\nSuccessfully updated Denver and Colorado Springs dioramas!")


if __name__ == "__main__":
    main()
