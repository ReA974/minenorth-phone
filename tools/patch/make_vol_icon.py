"""Icône de l'application « Vols » du téléphone (64x64, coins très arrondis, dégradé acier) : voiture blanche barrée d'un cadenas ouvert rouge.
usage : python -I make_vol_icon.py <dossier de sortie>   -> vol.png   (nécessite Pillow)"""
import os
import sys
from PIL import Image, ImageDraw

S = 8
N = 64 * S


def px(v):
    return int(round(v * S))


def main(folder):
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    grad = Image.new("RGBA", (N, N))
    gd = ImageDraw.Draw(grad)
    hi, lo = (92, 128, 178), (30, 46, 78)
    for y in range(N):
        t = y / (N - 1)
        gd.line([(0, y), (N, y)], fill=tuple(int(hi[i] + (lo[i] - hi[i]) * t) for i in range(3)) + (255,))
    mask = Image.new("L", (N, N), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, N - 1, N - 1], radius=px(15), fill=255)
    img.paste(grad, (0, 0), mask)
    d = ImageDraw.Draw(img)
    white, steel = (255, 255, 255), (40, 60, 96)
    # voiture de profil
    d.rounded_rectangle([px(8), px(30), px(56), px(44)], radius=px(5), fill=white)
    d.polygon([(px(18), px(30)), (px(24), px(21)), (px(40), px(21)), (px(47), px(30))], fill=white)
    d.polygon([(px(21), px(29)), (px(25.5), px(23.5)), (px(31), px(23.5)), (px(31), px(29))], fill=steel)
    d.polygon([(px(34), px(29)), (px(34), px(23.5)), (px(39), px(23.5)), (px(44), px(29))], fill=steel)
    for cx in (19, 45):
        d.ellipse([px(cx - 5), px(39), px(cx + 5), px(49)], fill=steel)
        d.ellipse([px(cx - 2.5), px(41.5), px(cx + 2.5), px(46.5)], fill=white)
    # cadenas ouvert rouge
    red = (232, 52, 72)
    d.arc([px(42), px(6), px(54), px(18)], 180, 330, fill=red, width=px(2.6))
    d.rounded_rectangle([px(40), px(13), px(56), px(26)], radius=px(3), fill=red)
    d.ellipse([px(46.5), px(17), px(49.5), px(20)], fill=white)
    path = os.path.join(folder, "vol.png")
    img.resize((64, 64), Image.LANCZOS).save(path)
    print("icône écrite :", path)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else ".")
