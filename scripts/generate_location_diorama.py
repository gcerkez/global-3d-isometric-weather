#!/usr/bin/env python3
"""
generate_location_diorama.py
----------------------------
Generative AI 3D Isometric City Diorama Builder.

Given any city or town name (or latitude/longitude), this script:
1. Geocodes the city via Open-Meteo's free Geocoding API.
2. Queries Wikipedia's GeoSearch API to discover the city's real-world
   historical & architectural landmarks.
3. Synthesizes a 3D isometric miniature city diorama image using either:
   - Google Gemini (`GEMINI_API_KEY` / `GOOGLE_API_KEY` environment variable), OR
   - Free zero-key Flux image generation endpoint.
4. Automatically extracts a clean transparent-background PNG cutout
   (`<slug>_island.png`) plus color-graded `<slug>_island_sunset.png` and
   `<slug>_island_night.png` layers for the KDE Plasma 6 & Web/Android widget.
"""

import argparse
import base64
import json
import math
import os
import re
import sys
import urllib.parse
import urllib.request
from collections import deque
from pathlib import Path


def slugify(name: str) -> str:
    """Converts a location name into a filesystem-safe lowercase slug."""
    return re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_") or "custom_city"


def geocode_city(query: str) -> dict:
    """Resolves a city name to coordinates and administrative metadata via Open-Meteo."""
    url = (
        "https://geocoding-api.open-meteo.com/v1/search?name="
        + urllib.parse.quote(query)
        + "&count=1&language=en&format=json"
    )
    req = urllib.request.Request(url, headers={"User-Agent": "IsometricWeatherWidget/0.0.1"})
    with urllib.request.urlopen(req, timeout=12) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    results = data.get("results") or []
    if not results:
        raise ValueError(f"Could not geocode location: {query}")
    r = results[0]
    admin = r.get("admin1", "")
    country = r.get("country", "")
    display = f"{r['name']}, {admin}" if admin else f"{r['name']}, {country}"
    return {
        "name": r["name"],
        "display_name": display,
        "latitude": float(r["latitude"]),
        "longitude": float(r["longitude"]),
    }


def fetch_wikipedia_landmarks(lat: float, lon: float, limit: int = 5) -> list[dict]:
    """Fetches nearby cultural and architectural landmarks from Wikipedia GeoSearch."""
    url = (
        "https://en.wikipedia.org/w/api.php?action=query&generator=geosearch"
        f"&ggscoord={lat}|{lon}&ggsradius=10000&ggslimit={limit}"
        "&prop=extracts|coordinates&exintro=1&explaintext=1&exsentences=2&format=json"
    )
    req = urllib.request.Request(url, headers={"User-Agent": "IsometricWeatherWidget/0.0.1"})
    try:
        with urllib.request.urlopen(req, timeout=12) as resp:
            data = json.loads(resp.read().decode("utf-8"))
        pages = data.get("query", {}).get("pages", {})
        landmarks = []
        for p in pages.values():
            if "title" in p:
                landmarks.append({
                    "title": p["title"],
                    "extract": (p.get("extract") or "").strip(),
                })
        return landmarks
    except Exception as exc:
        print(f"Warning: Wikipedia GeoSearch lookup failed ({exc}); using generic city prompt.")
        return []


def build_diorama_prompt(display_name: str, landmarks: list[dict]) -> str:
    """Constructs a structured 3D isometric city diorama prompt for image synthesis."""
    lm_names = ", ".join(l["title"] for l in landmarks[:4]) if landmarks else "local landmarks and waterfront"
    return (
        f"Create a cute, detailed 3D isometric city diorama of {display_name} "
        f"featuring its iconic landmarks ({lm_names}) in a miniature 3D architectural model style. "
        "Place water or river features on the left side of the cross-section slab, historic and "
        "characteristic local architecture in the center plaza, and rising terrain or prominent "
        "monuments on the right side. The entire diorama sits on a clean brown earth and blue water "
        "cross-section slab casting a soft drop shadow on a completely clean, solid light-blue "
        "background (#a8dbf8) with NO text, NO weather UI box, NO watermarks, and NO floating clouds."
    )


def generate_image_bytes(prompt: str, api_key: str | None = None) -> bytes:
    """
    Synthesizes the isometric diorama image via Google Gemini API (if `api_key` is provided)
    or falls back to the zero-key Pollinations Flux generator.
    """
    key = api_key or os.environ.get("GEMINI_API_KEY") or os.environ.get("GOOGLE_API_KEY")
    if key:
        model = os.environ.get("GOOGLE_MODEL", "gemini-2.5-flash-image")
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}"
        payload = json.dumps({
            "contents": [{"parts": [{"text": prompt}]}]
        }).encode("utf-8")
        req = urllib.request.Request(
            url,
            data=payload,
            headers={"Content-Type": "application/json", "User-Agent": "IsometricWeatherWidget/0.0.1"},
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=60) as resp:
            result = json.loads(resp.read().decode("utf-8"))
        for cand in result.get("candidates", []):
            for part in cand.get("content", {}).get("parts", []):
                inline = part.get("inlineData")
                if inline and "data" in inline:
                    return base64.b64decode(inline["data"])

    # Zero-key fallback via Pollinations Flux model
    encoded = urllib.parse.quote(prompt)
    flux_url = f"https://image.pollinations.ai/prompt/{encoded}?width=1376&height=768&nologo=true&model=flux"
    req = urllib.request.Request(flux_url, headers={"User-Agent": "IsometricWeatherWidget/0.0.1"})
    with urllib.request.urlopen(req, timeout=90) as resp:
        return resp.read()


def process_and_extract_layers(raw_bytes: bytes, out_dir: Path, prefix: str) -> None:
    """
    Extracts a transparent-background cutout PNG from the synthesized diorama image
    and produces matching golden-hour sunset and moonlit night layers.
    """
    from PySide6.QtCore import Qt
    from PySide6.QtGui import QColor, QImage

    img = QImage.fromData(raw_bytes).convertToFormat(QImage.Format_ARGB32)
    w, h = img.width(), img.height()

    # Sample corner background color
    c0 = img.pixelColor(10, 10)
    bg_r, bg_g, bg_b = c0.red(), c0.green(), c0.blue()

    def color_dist_bg(r: int, g: int, b: int) -> float:
        return math.sqrt((r - bg_r) ** 2 + (g - bg_g) ** 2 + (b - bg_b) ** 2)

    visited = bytearray(w * h)
    bg_type = bytearray(w * h)
    q = deque()

    for x in range(w):
        for y in (0, h - 1):
            idx = y * w + x
            visited[idx] = 1
            q.append((x, y))
    for y in range(h):
        for x in (0, w - 1):
            idx = y * w + x
            if not visited[idx]:
                visited[idx] = 1
                q.append((x, y))

    while q:
        cx, cy = q.popleft()
        idx = cy * w + cx
        c = img.pixelColor(cx, cy)
        r, g, b = c.red(), c.green(), c.blue()
        if color_dist_bg(r, g, b) < 28.0:
            bg_type[idx] = 1
        elif cx > int(w * 0.5) and cy > int(h * 0.5) and b > r + 28 and g > r + 10 and b < 245:
            bg_type[idx] = 2
        else:
            continue

        for nx, ny in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
            if 0 <= nx < w and 0 <= ny < h:
                nidx = ny * w + nx
                if not visited[nidx]:
                    visited[nidx] = 1
                    q.append((nx, ny))

    day_cut = QImage(w, h, QImage.Format_ARGB32)
    sunset_cut = QImage(w, h, QImage.Format_ARGB32)
    night_cut = QImage(w, h, QImage.Format_ARGB32)
    day_cut.fill(Qt.transparent)
    sunset_cut.fill(Qt.transparent)
    night_cut.fill(Qt.transparent)

    for y in range(h):
        for x in range(w):
            idx = y * w + x
            c = img.pixelColor(x, y)
            r, g, b = c.red(), c.green(), c.blue()
            t = bg_type[idx]
            if t == 1:
                continue
            elif t == 2:
                darkness = max(0.0, min(1.0, (246.0 - b) / 115.0))
                alpha = int(max(0, min(140, darkness * 130)))
                sh_col = QColor(10, 18, 32, alpha)
                day_cut.setPixelColor(x, y, sh_col)
                sunset_cut.setPixelColor(x, y, sh_col)
                night_cut.setPixelColor(x, y, sh_col)
            else:
                day_cut.setPixelColor(x, y, QColor(r, g, b, 255))
                lum = 0.299 * r + 0.587 * g + 0.114 * b
                nr = int(max(0, min(255, r * 0.34 + lum * 0.08)))
                ng = int(max(0, min(255, g * 0.44 + lum * 0.14)))
                nb = int(max(0, min(255, b * 0.62 + lum * 0.24 + 14)))
                night_cut.setPixelColor(x, y, QColor(nr, ng, nb, 255))

                sr = int(max(0, min(255, r * 1.08 + 18)))
                sg = int(max(0, min(255, g * 0.88 + 4)))
                sb = int(max(0, min(255, b * 0.76)))
                sunset_cut.setPixelColor(x, y, QColor(sr, sg, sb, 255))

    out_dir.mkdir(parents=True, exist_ok=True)
    day_cut.save(str(out_dir / f"{prefix}_island.png"))
    sunset_cut.save(str(out_dir / f"{prefix}_island_sunset.png"))
    night_cut.save(str(out_dir / f"{prefix}_island_night.png"))
    print(f"✅ Saved {prefix}_island.png, {prefix}_island_sunset.png, and {prefix}_island_night.png in {out_dir}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate a 3D isometric city diorama for any location.")
    parser.add_argument("city", help="City or town name (e.g., 'Yorktown, VA', 'San Francisco, CA', 'Kyoto')")
    parser.add_argument("--api-key", help="Optional Google Gemini API key (uses zero-key Flux fallback if omitted)")
    parser.add_argument(
        "--out-dir",
        default=str(Path(__file__).resolve().parent.parent / "package" / "contents" / "images"),
        help="Output directory for generated diorama PNG layers",
    )
    args = parser.parse_args()

    geo = geocode_city(args.city)
    print(f"📍 Geocoded: {geo['display_name']} ({geo['latitude']:.4f}, {geo['longitude']:.4f})")
    landmarks = fetch_wikipedia_landmarks(geo["latitude"], geo["longitude"])
    if landmarks:
        print("🏛  Discovered Landmarks:", ", ".join(l["title"] for l in landmarks))

    prompt = build_diorama_prompt(geo["display_name"], landmarks)
    print(f"🎨 Prompt: {prompt}\n⏳ Generating isometric diorama...")
    raw_bytes = generate_image_bytes(prompt, args.api_key)
    slug = slugify(geo["name"])
    process_and_extract_layers(raw_bytes, Path(args.out_dir), slug)
    return 0


if __name__ == "__main__":
    sys.exit(main())
