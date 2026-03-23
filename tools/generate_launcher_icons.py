#!/usr/bin/env python3
"""Generate adaptive launcher layers + legacy mipmap PNGs from launcher_assets/. Run from repo root."""
from __future__ import annotations

from collections import deque
from pathlib import Path

from PIL import Image

PROJECT = Path(__file__).resolve().parent.parent
RES = PROJECT / "app/src/main/res"
LAUNCHER = PROJECT / "launcher_assets"
LAYER = 432  # 108dp @ xxxhdpi (4x)
# Center ~66dp of 108dp safe circle → scale mascot to fit inside
SAFE_SCALE = 0.66

SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# RGB exports often use opaque black instead of alpha; keep real pixels (darkest ~sum 17+).
_BLACK_KEY_SUM_MAX = 14


def key_black_to_transparent(img: Image.Image) -> Image.Image:
    """Turn near-black pixels transparent when the PNG is fully opaque (RGB export)."""
    rgba = img.convert("RGBA")
    a_ch = rgba.split()[3]
    if a_ch.getextrema()[0] < 255:
        return rgba  # already has real alpha — keep black eyes etc.
    px = rgba.load()
    w, h = rgba.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if r + g + b <= _BLACK_KEY_SUM_MAX:
                px[x, y] = (0, 0, 0, 0)
    return rgba


def strip_baked_checkerboard_neutral_gray(rgba: Image.Image) -> Image.Image:
    """Remove opaque neutral-gray tiles (editor 'transparency' checkerboard baked into RGB)."""
    px = rgba.load()
    w, h = rgba.size

    def is_neutral_gray(r: int, g: int, b: int) -> bool:
        if max(r, g, b) - min(r, g, b) > 14:
            return False
        avg = (r + g + b) / 3
        return 195 <= avg <= 235

    q: deque[tuple[int, int]] = deque()
    for x in range(w):
        for y in (0, h - 1):
            r, g, b, a = px[x, y]
            if a > 0 and is_neutral_gray(r, g, b):
                q.append((x, y))
    for y in range(1, h - 1):
        for x in (0, w - 1):
            r, g, b, a = px[x, y]
            if a > 0 and is_neutral_gray(r, g, b):
                q.append((x, y))

    seen: set[tuple[int, int]] = set()
    while q:
        x, y = q.popleft()
        if (x, y) in seen:
            continue
        seen.add((x, y))
        r, g, b, a = px[x, y]
        if a < 10 or not is_neutral_gray(r, g, b):
            continue
        px[x, y] = (0, 0, 0, 0)
        for dx, dy in ((0, 1), (0, -1), (1, 0), (-1, 0)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h:
                q.append((nx, ny))
    return rgba


def strip_baked_checkerboard(rgba: Image.Image) -> Image.Image:
    """Remove opaque neutral-gray tiles (checkerboard) connected to image edges."""
    return strip_baked_checkerboard_neutral_gray(rgba)


def load_foreground_source() -> Image.Image:
    raw = Image.open(LAUNCHER / "marshmallow_foreground.png")
    return key_black_to_transparent(raw)


def load_sleep_foreground_source() -> Image.Image:
    raw = Image.open(LAUNCHER / "marshmallow_sleep_foreground.png")
    return key_black_to_transparent(raw)


def load_feed_foreground_source() -> Image.Image:
    raw = Image.open(LAUNCHER / "marshmallow_feed_foreground.png")
    rgba = key_black_to_transparent(raw)
    return strip_baked_checkerboard(rgba)


def load_poop_foreground_source() -> Image.Image:
    raw = Image.open(LAUNCHER / "marshmallow_poop_foreground.png")
    rgba = key_black_to_transparent(raw)
    return strip_baked_checkerboard(rgba)


def load_pee_foreground_source() -> Image.Image:
    raw = Image.open(LAUNCHER / "marshmallow_pee_foreground.png")
    rgba = key_black_to_transparent(raw)
    return strip_baked_checkerboard(rgba)


def layer_background() -> Image.Image:
    bg = Image.open(LAUNCHER / "marshmallow_background.png").convert("RGBA")
    return bg.resize((LAYER, LAYER), Image.Resampling.LANCZOS)


def layer_foreground() -> Image.Image:
    fg = load_foreground_source()
    canvas = Image.new("RGBA", (LAYER, LAYER), (0, 0, 0, 0))
    max_w = int(LAYER * SAFE_SCALE)
    max_h = int(LAYER * SAFE_SCALE)
    fg.thumbnail((max_w, max_h), Image.Resampling.LANCZOS)
    x = (LAYER - fg.width) // 2
    y = (LAYER - fg.height) // 2
    canvas.paste(fg, (x, y), fg)
    return canvas


def composite_flat() -> Image.Image:
    bg = layer_background()
    fg = layer_foreground()
    return Image.alpha_composite(bg, fg)


def main() -> None:
    drawable = RES / "drawable"
    drawable.mkdir(parents=True, exist_ok=True)

    # Write matted source back so launcher_assets has real transparency in git
    matted = load_foreground_source()
    matted.save(LAUNCHER / "marshmallow_foreground.png")

    layer_background().save(drawable / "ic_launcher_background.png")
    layer_foreground().save(drawable / "ic_launcher_foreground.png")

    # In-app mascots — large bitmaps for sharp scaling on home cards (~96dp on xxxhdpi)
    app_size = 320
    raw = matted.copy()
    raw.thumbnail((app_size, app_size), Image.Resampling.LANCZOS)
    awake = Image.new("RGBA", (app_size, app_size), (0, 0, 0, 0))
    ax = (app_size - raw.width) // 2
    ay = (app_size - raw.height) // 2
    awake.paste(raw, (ax, ay), raw)
    awake.save(drawable / "marshmallow_awake.png")

    matted_sleep = load_sleep_foreground_source()
    matted_sleep.save(LAUNCHER / "marshmallow_sleep_foreground.png")
    sleep_raw = matted_sleep.copy()
    sleep_raw.thumbnail((app_size, app_size), Image.Resampling.LANCZOS)
    sleep = Image.new("RGBA", (app_size, app_size), (0, 0, 0, 0))
    sx = (app_size - sleep_raw.width) // 2
    sy = (app_size - sleep_raw.height) // 2
    sleep.paste(sleep_raw, (sx, sy), sleep_raw)
    sleep.save(drawable / "marshmallow_sleep.png")

    matted_feed = load_feed_foreground_source()
    matted_feed.save(LAUNCHER / "marshmallow_feed_foreground.png")
    feed_raw = matted_feed.copy()
    feed_raw.thumbnail((app_size, app_size), Image.Resampling.LANCZOS)
    feed = Image.new("RGBA", (app_size, app_size), (0, 0, 0, 0))
    fx = (app_size - feed_raw.width) // 2
    fy = (app_size - feed_raw.height) // 2
    feed.paste(feed_raw, (fx, fy), feed_raw)
    feed.save(drawable / "marshmallow_feed.png")

    matted_poop = load_poop_foreground_source()
    matted_poop.save(LAUNCHER / "marshmallow_poop_foreground.png")
    poop_raw = matted_poop.copy()
    poop_raw.thumbnail((app_size, app_size), Image.Resampling.LANCZOS)
    poop = Image.new("RGBA", (app_size, app_size), (0, 0, 0, 0))
    px_ = (app_size - poop_raw.width) // 2
    py_ = (app_size - poop_raw.height) // 2
    poop.paste(poop_raw, (px_, py_), poop_raw)
    poop.save(drawable / "marshmallow_poop.png")

    matted_pee = load_pee_foreground_source()
    matted_pee.save(LAUNCHER / "marshmallow_pee_foreground.png")
    pee_raw = matted_pee.copy()
    pee_raw.thumbnail((app_size, app_size), Image.Resampling.LANCZOS)
    pee = Image.new("RGBA", (app_size, app_size), (0, 0, 0, 0))
    pex = (app_size - pee_raw.width) // 2
    pey = (app_size - pee_raw.height) // 2
    pee.paste(pee_raw, (pex, pey), pee_raw)
    pee.save(drawable / "marshmallow_pee.png")

    flat = composite_flat()
    for folder, size in SIZES.items():
        d = RES / folder
        d.mkdir(parents=True, exist_ok=True)
        out = flat.resize((size, size), Image.Resampling.LANCZOS)
        out.save(d / "ic_launcher.png")
        out.save(d / "ic_launcher_round.png")


if __name__ == "__main__":
    main()
