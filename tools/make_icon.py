"""Generate the adaptive-icon foreground vector (serif wordmark) from a system font."""
from pathlib import Path

from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.boundsPen import BoundsPen

ROOT = Path(__file__).resolve().parents[1]
FONT = Path(r"C:\Windows\Fonts\georgiab.ttf")
TEXT = "Memos"
VIEW = 108.0
TARGET_WIDTH = 48.0  # units inside the 108 viewport; safe zone is 66

def glyph_run(font, text):
    glyph_set = font.getGlyphSet()
    cmap = font.getBestCmap()
    names = [cmap[ord(ch)] for ch in text]
    hmtx = font["hmtx"]
    total = sum(hmtx[n][0] for n in names)
    return glyph_set, names, total

def bbox(font, names):
    glyph_set = font.getGlyphSet()
    hmtx = font["hmtx"]
    x0 = y0 = 1e9
    x1 = y1 = -1e9
    offset = 0.0
    for n in names:
        pen = BoundsPen(glyph_set)
        glyph_set[n].draw(pen)
        if pen.bounds is not None:
            bx0, by0, bx1, by1 = pen.bounds
            x0 = min(x0, bx0 + offset)
            y0 = min(y0, by0)
            x1 = max(x1, bx1 + offset)
            y1 = max(y1, by1)
        offset += hmtx[n][0]
    return x0, y0, x1, y1

def build(font_path, text, target_width):
    font = TTFont(font_path)
    glyph_set, names, total = glyph_run(font, text)
    x0, y0, x1, y1 = bbox(font, names)
    scale = target_width / (x1 - x0)
    w = (x1 - x0) * scale
    h = (y1 - y0) * scale
    tx = (VIEW - w) / 2 - x0 * scale
    ty = (VIEW + h) / 2 + y0 * scale  # y flips: baseline sits so bbox is centered vertically
    parts = []
    pen_x = 0.0
    for n in names:
        pen = SVGPathPen(glyph_set, ntos=lambda v: ("%.2f" % v).rstrip("0").rstrip("."))
        tp = TransformPen(pen, (scale, 0, 0, -scale, tx + pen_x * scale, ty))
        glyph_set[n].draw(tp)
        parts.append(pen.getCommands())
        pen_x += font["hmtx"][n][0]
    return " ".join(p for p in parts if p), w, h

if __name__ == "__main__":
    path, w, h = build(FONT, TEXT, TARGET_WIDTH)
    out = ROOT / "app/src/main/res/drawable/ic_launcher_foreground.xml"
    xml = f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#000000"
        android:pathData="{path}" />
</vector>
'''
    with open(out, "w", encoding="utf-8") as f:
        f.write(xml)
    svg_out = ROOT / "tools/icon-preview.svg"
    with open(svg_out, "w", encoding="utf-8") as f:
        f.write(
            '<svg xmlns="http://www.w3.org/2000/svg" width="432" height="432" viewBox="0 0 108 108">'
            '<rect width="108" height="108" fill="#ffffff"/>'
            f'<path d="{path}" fill="#000000" fill-rule="nonzero"/>'
            "</svg>"
        )
    print("wrote", out, "wordmark %.1f x %.1f units" % (w, h), "path chars", len(path))
