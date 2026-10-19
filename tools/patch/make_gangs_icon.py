"""Icône de l'application « Mon gang » du téléphone : trois silhouettes (le gang) sur fond violet, 64x64.
usage : python -I make_gangs_icon.py <sortie.png>   (nécessite Pillow)"""
import sys
from PIL import Image, ImageDraw

S = 8
N = 64 * S
HI, LO = (214, 96, 255), (140, 30, 190)
WHITE = (255, 255, 255)
GOLD = (255, 214, 64)


def px(v):
    return int(round(v * S))


img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
grad = Image.new("RGBA", (N, N))
gd = ImageDraw.Draw(grad)
for y in range(N):
    t = y / (N - 1)
    gd.line([(0, y), (N, y)], fill=tuple(int(HI[i] + (LO[i] - HI[i]) * t) for i in range(3)) + (255,))
mask = Image.new("L", (N, N), 0)
ImageDraw.Draw(mask).rounded_rectangle([0, 0, N - 1, N - 1], radius=px(15), fill=255)
img.paste(grad, (0, 0), mask)
d = ImageDraw.Draw(img)


def bust(cx, top, r, col):
    d.ellipse([px(cx - r), px(top), px(cx + r), px(top + 2 * r)], fill=col)
    d.pieslice([px(cx - r * 1.9), px(top + 2 * r + 1.2), px(cx + r * 1.9), px(top + 2 * r + 1.2 + r * 4.2)], 180, 360, fill=col)


bust(17, 24, 5.2, (235, 220, 245))
bust(47, 24, 5.2, (235, 220, 245))
bust(32, 14, 7, WHITE)
# couronne du chef
d.polygon([(px(25.5), px(12)), (px(25.5), px(6)), (px(29), px(9.5)), (px(32), px(5)), (px(35), px(9.5)), (px(38.5), px(6)), (px(38.5), px(12))], fill=GOLD)
img.resize((64, 64), Image.LANCZOS).save(sys.argv[1])
print("icône écrite :", sys.argv[1])
