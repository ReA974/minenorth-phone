"""Icône de l'application « Déco » du téléphone (64x64, coins très arrondis, dégradé menthe) : canapé blanc arrondi avec coussin et lampe.
usage : python -I make_deco_icon.py <dossier de sortie>   -> deco.png   (nécessite Pillow)"""
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
    hi, lo = (92, 222, 176), (16, 130, 102)
    for y in range(N):
        t = y / (N - 1)
        gd.line([(0, y), (N, y)], fill=tuple(int(hi[i] + (lo[i] - hi[i]) * t) for i in range(3)) + (255,))
    mask = Image.new("L", (N, N), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, N - 1, N - 1], radius=px(15), fill=255)
    img.paste(grad, (0, 0), mask)
    d = ImageDraw.Draw(img)
    white = (255, 255, 255)
    mint = (24, 150, 116)
    # lampe sur pied (derrière, à droite)
    d.rectangle([px(47), px(18), px(48.6), px(40)], fill=white)
    d.rounded_rectangle([px(42), px(11), px(54), px(19)], radius=px(3), fill=white)
    # dossier et accoudoirs du canapé
    d.rounded_rectangle([px(12), px(26), px(44), px(40)], radius=px(6), fill=white)
    d.rounded_rectangle([px(7), px(32), px(16), px(48)], radius=px(4), fill=white)
    d.rounded_rectangle([px(40), px(32), px(49), px(48)], radius=px(4), fill=white)
    # assise
    d.rounded_rectangle([px(12), px(38), px(44), px(48)], radius=px(4), fill=white)
    d.line([(px(28), px(39)), (px(28), px(47))], fill=mint, width=px(1.4))
    # coussin
    d.rounded_rectangle([px(17), px(30), px(26), px(37)], radius=px(3), fill=(255, 200, 120))
    # pieds
    d.rounded_rectangle([px(10), px(48), px(13), px(52)], radius=px(1), fill=white)
    d.rounded_rectangle([px(43), px(48), px(46), px(52)], radius=px(1), fill=white)
    path = os.path.join(folder, "deco.png")
    img.resize((64, 64), Image.LANCZOS).save(path)
    print("icône écrite :", path)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else ".")
