"""Icônes des applications de métier du téléphone (64x64, coins très arrondis, dégradé vertical) :
Police (bleu, écusson + étoile), Secours (rouge, croix blanche), Mairie (vert, bâtiment à colonnes), Entreprise (or, mallette).
usage : python -I make_jobs_icons.py <dossier de sortie>   -> police.png secours.png mairie.png entreprise.png   (nécessite Pillow)"""
import os
import sys
from PIL import Image, ImageDraw

S = 8
N = 64 * S
WHITE = (255, 255, 255)


def px(v):
    return int(round(v * S))


def base(hi, lo):
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    grad = Image.new("RGBA", (N, N))
    gd = ImageDraw.Draw(grad)
    for y in range(N):
        t = y / (N - 1)
        gd.line([(0, y), (N, y)], fill=tuple(int(hi[i] + (lo[i] - hi[i]) * t) for i in range(3)) + (255,))
    mask = Image.new("L", (N, N), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, N - 1, N - 1], radius=px(15), fill=255)
    img.paste(grad, (0, 0), mask)
    return img, ImageDraw.Draw(img)


def save(img, folder, name):
    path = os.path.join(folder, name + ".png")
    img.resize((64, 64), Image.LANCZOS).save(path)
    print("icône écrite :", path)


def star(d, cx, cy, r, col):
    import math
    pts = []
    for i in range(10):
        a = -math.pi / 2 + i * math.pi / 5
        rr = r if i % 2 == 0 else r * 0.42
        pts.append((px(cx + rr * math.cos(a)), px(cy + rr * math.sin(a))))
    d.polygon(pts, fill=col)


def police(folder):
    img, d = base((92, 150, 255), (28, 76, 190))
    # écusson blanc
    d.polygon([(px(32), px(10)), (px(49), px(16)), (px(49), px(32)), (px(44), px(44)), (px(32), px(54)),
               (px(20), px(44)), (px(15), px(32)), (px(15), px(16))], fill=WHITE)
    d.polygon([(px(32), px(14)), (px(45.5), px(19)), (px(45.5), px(32)), (px(41.5), px(41.5)), (px(32), px(49.5)),
               (px(22.5), px(41.5)), (px(18.5), px(32)), (px(18.5), px(19))], fill=(40, 92, 210))
    star(d, 32, 31, 11, (255, 214, 64))
    save(img, folder, "police")


def secours(folder):
    img, d = base((255, 110, 110), (190, 28, 36))
    d.rounded_rectangle([px(25), px(11), px(39), px(53)], radius=px(3.5), fill=WHITE)
    d.rounded_rectangle([px(11), px(25), px(53), px(39)], radius=px(3.5), fill=WHITE)
    save(img, folder, "secours")


def mairie(folder):
    img, d = base((86, 210, 130), (28, 130, 72))
    # fronton, colonnes, socle
    d.polygon([(px(32), px(9)), (px(54), px(22)), (px(10), px(22))], fill=WHITE)
    d.ellipse([px(29.5), px(14.5), px(34.5), px(19.5)], fill=(40, 150, 86))
    for x in (14, 25, 36, 47):
        d.rectangle([px(x), px(26), px(x + 4), px(45)], fill=WHITE)
    d.rounded_rectangle([px(10), px(47), px(54), px(53)], radius=px(2), fill=WHITE)
    save(img, folder, "mairie")


def entreprise(folder):
    img, d = base((255, 214, 90), (214, 142, 14))
    brown = (96, 60, 12)
    # poignée
    d.rounded_rectangle([px(24), px(13), px(40), px(24)], radius=px(4), outline=WHITE, width=px(3.2))
    # corps de la mallette
    d.rounded_rectangle([px(10), px(21), px(54), px(51)], radius=px(5), fill=WHITE)
    d.rectangle([px(10), px(33), px(54), px(35)], fill=(235, 190, 70))
    d.rounded_rectangle([px(28), px(30), px(36), px(39)], radius=px(2), fill=brown)
    save(img, folder, "entreprise")


out = sys.argv[1]
os.makedirs(out, exist_ok=True)
for f in (police, secours, mairie, entreprise):
    f(out)
