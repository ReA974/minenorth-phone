"""Icône de l'application « Deals » du téléphone (64x64, coins très arrondis, dégradé anthracite) : bulle de message blanche avec un euro rose.
usage : python -I make_deals_icon.py <dossier de sortie>   -> deals.png   (nécessite Pillow)"""
import os
import sys
from PIL import Image, ImageDraw, ImageFont

S = 8
N = 64 * S


def px(v):
    return int(round(v * S))


def main(folder):
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    grad = Image.new("RGBA", (N, N))
    gd = ImageDraw.Draw(grad)
    hi, lo = (70, 74, 96), (22, 22, 32)
    for y in range(N):
        t = y / (N - 1)
        gd.line([(0, y), (N, y)], fill=tuple(int(hi[i] + (lo[i] - hi[i]) * t) for i in range(3)) + (255,))
    mask = Image.new("L", (N, N), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, N - 1, N - 1], radius=px(15), fill=255)
    img.paste(grad, (0, 0), mask)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([px(10), px(12), px(54), px(44)], radius=px(10), fill=(255, 255, 255))
    d.polygon([(px(18), px(42)), (px(16), px(54)), (px(30), px(43))], fill=(255, 255, 255))
    pink = (200, 60, 240)
    # euro dessiné : arc + deux barres
    d.arc([px(22), px(17), px(44), px(39)], 40, 320, fill=pink, width=px(4))
    d.rectangle([px(19), px(24), px(34), px(26.5)], fill=pink)
    d.rectangle([px(19), px(29.5), px(34), px(32)], fill=pink)
    path = os.path.join(folder, "deals.png")
    img.resize((64, 64), Image.LANCZOS).save(path)
    print("icône écrite :", path)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else ".")
