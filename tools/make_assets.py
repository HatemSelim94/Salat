#!/usr/bin/env python3
"""Generate the Salat icon set (PNG) and the notification chime (WAV).

Icon: an original geometric mark — a deep-green rounded square with a thin
crescent formed by two offset circles and a small dot. No third-party art.
Chime: two soft sine tones with a gentle envelope, ~1.2 s.

Usage: python3 tools/make_assets.py <out-dir-for-icons> <out-wav>
"""
import math
import struct
import sys
import wave
from pathlib import Path

from PIL import Image, ImageDraw

BG = (47, 111, 94)        # #2f6f5e
FG = (251, 250, 247)      # #fbfaf7


def icon(size: int) -> Image.Image:
    s = 8  # supersample for smooth edges
    S = size * s
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = S * 0.22
    d.rounded_rectangle((0, 0, S - 1, S - 1), radius=r, fill=BG)

    # Crescent: big light circle minus an offset background circle.
    cx, cy = S * 0.50, S * 0.52
    R = S * 0.30
    d.ellipse((cx - R, cy - R, cx + R, cy + R), fill=FG)
    ox = cx + S * 0.11
    oy = cy - S * 0.06
    R2 = S * 0.27
    d.ellipse((ox - R2, oy - R2, ox + R2, oy + R2), fill=BG)

    # Small star-dot in the crescent's hollow.
    dx, dy, dr = S * 0.66, S * 0.34, S * 0.045
    d.ellipse((dx - dr, dy - dr, dx + dr, dy + dr), fill=FG)

    return img.resize((size, size), Image.LANCZOS)


def chime(path: Path, rate: int = 22050) -> None:
    """Rising three-note bell (C5 E5 G5), ~1.6 s. Deliberately unlike stock notification tones."""
    notes = [(523.25, 0.0), (659.25, 0.22), (783.99, 0.44)]
    dur = 1.6
    n = int(rate * dur)
    frames = bytearray()
    for i in range(n):
        t = i / rate
        v = 0.0
        for f, start in notes:
            if t < start:
                continue
            tt = t - start
            env = math.exp(-2.6 * tt)
            v += env * (0.6 * math.sin(2 * math.pi * f * tt)
                        + 0.25 * math.sin(2 * math.pi * 2 * f * tt) * math.exp(-5 * tt)
                        + 0.1 * math.sin(2 * math.pi * 3 * f * tt) * math.exp(-8 * tt))
        v *= 0.5
        frames += struct.pack("<h", int(max(-1, min(1, v)) * 32767))
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(bytes(frames))


def main() -> None:
    out_icons = Path(sys.argv[1])
    out_wav = Path(sys.argv[2])
    out_icons.mkdir(parents=True, exist_ok=True)
    for size in (16, 32, 48, 128, 256, 512):
        icon(size).save(out_icons / f"icon{size}.png", optimize=True)
    chime(out_wav)
    print("assets written to", out_icons, out_wav)


if __name__ == "__main__":
    main()
