"""Icône de l'application « Boutiques » du téléphone (64x64, coins très arrondis, dégradé framboise) : sac de courses blanc avec anses, rabat et losange.
usage : python -I make_boutiques_icon.py <dossier de sortie>   -> boutiques.png   (nécessite Pillow)"""
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
    hi, lo = (246, 108, 150), (176, 30, 82)
    for y in range(N):
        t = y / (N - 1)
        gd.line([(0, y), (N, y)], fill=tuple(int(hi[i] + (lo[i] - hi[i]) * t) for i in range(3)) + (255,))
    mask = Image.new("L", (N, N), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, N - 1, N - 1], radius=px(15), fill=255)
    img.paste(grad, (0, 0), mask)
    d = ImageDraw.Draw(img)
    white = (255, 255, 255)
    # anses (derrière le sac)
    d.arc([px(21), px(11), px(43), px(33)], 180, 360, fill=white, width=px(3.5))
    # sac
    d.rounded_rectangle([px(14), px(22), px(50), px(54)], radius=px(7), fill=white)
    # rabat du sac (bande framboise) et petit losange décoratif
    berry = (196, 44, 98)
    d.rectangle([px(14), px(29), px(50), px(31.5)], fill=berry)
    d.polygon([(px(32), px(37)), (px(37), px(42)), (px(32), px(47)), (px(27), px(42))], fill=berry)
    path = os.path.join(folder, "boutiques.png")
    img.resize((64, 64), Image.LANCZOS).save(path)
    print("icône écrite :", path)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else ".")
