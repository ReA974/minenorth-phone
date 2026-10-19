"""Icône de l'application Livraison du téléphone : scooter + sac de livraison, 3 couleurs (orange, blanc, brun foncé), 64x64.
usage : python -I make_livraison_icon.py <sortie.png>   (nécessite Pillow)"""
import sys
from PIL import Image, ImageDraw

S = 8                      # sur-échantillonnage
N = 64 * S
ORANGE_HI, ORANGE_LO = (255, 154, 60), (232, 89, 12)
WHITE = (255, 255, 255)
BROWN = (74, 42, 24)


def px(v):
    return int(round(v * S))


img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
# fond : dégradé vertical orange, coins très arrondis
grad = Image.new("RGBA", (N, N))
gd = ImageDraw.Draw(grad)
for y in range(N):
    t = y / (N - 1)
    gd.line([(0, y), (N, y)], fill=tuple(int(ORANGE_HI[i] + (ORANGE_LO[i] - ORANGE_HI[i]) * t) for i in range(3)) + (255,))
mask = Image.new("L", (N, N), 0)
ImageDraw.Draw(mask).rounded_rectangle([0, 0, N - 1, N - 1], radius=px(15), fill=255)
img.paste(grad, (0, 0), mask)

d = ImageDraw.Draw(img)
# sac / caisson de livraison (brun, liseré blanc) à l'arrière
d.rounded_rectangle([px(9), px(15), px(30), px(34)], radius=px(3), fill=BROWN)
d.rounded_rectangle([px(11.5), px(17.5), px(27.5), px(31.5)], radius=px(2), outline=WHITE, width=px(1.4))
d.line([(px(11.5), px(24)), (px(27.5), px(24))], fill=WHITE, width=px(1.4))
# scooter (blanc) : plancher, garde-boue, jambe de force, guidon
d.rounded_rectangle([px(15), px(34), px(46), px(40)], radius=px(3), fill=WHITE)            # selle + plancher
d.polygon([(px(40), px(34)), (px(46), px(34)), (px(49), px(22)), (px(44), px(22))], fill=WHITE)  # colonne de direction
d.rounded_rectangle([px(42), px(19), px(54), px(23.5)], radius=px(2), fill=WHITE)           # guidon
d.polygon([(px(46), px(40)), (px(52), px(40)), (px(54), px(47)), (px(47), px(47))], fill=WHITE)   # garde-boue avant
# roues (brun, moyeu blanc)
for cx in (19, 49):
    d.ellipse([px(cx - 7), px(40), px(cx + 7), px(54)], fill=BROWN, outline=WHITE, width=px(1.6))
    d.ellipse([px(cx - 2.4), px(44.6), px(cx + 2.4), px(49.4)], fill=WHITE)

out = img.resize((64, 64), Image.LANCZOS)
out.save(sys.argv[1])
print("icône écrite :", sys.argv[1])
