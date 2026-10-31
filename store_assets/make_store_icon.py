"""Play ストア掲載用アイコン(512x512)を、アプリ内アダプティブアイコンと同じデザインで書き出す。
使い方: python store_assets/make_store_icon.py app/src/main/res/drawable-nodpi store_assets/play_store_icon_512.png
"""
import sys
from PIL import Image, ImageDraw, ImageFilter

res_dir, out_path = sys.argv[1], sys.argv[2]
OUT = 512
BIG = 2048
U = BIG / 108.0


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def cubic(p0, p1, p2, p3, n=64):
    pts = []
    for i in range(n + 1):
        t = i / n
        mt = 1 - t
        x = mt**3 * p0[0] + 3 * mt * mt * t * p1[0] + 3 * mt * t * t * p2[0] + t**3 * p3[0]
        y = mt**3 * p0[1] + 3 * mt * mt * t * p1[1] + 3 * mt * t * t * p2[1] + t**3 * p3[1]
        pts.append((x * U, y * U))
    return pts


# 空→若草→草原のグラデーション
img = Image.new('RGBA', (BIG, BIG))
top, mid, bot = hexc('#8FD5FF'), hexc('#E6F7D2'), hexc('#B8E58F')
d = ImageDraw.Draw(img)
for y in range(BIG):
    t = y / (BIG - 1)
    c = lerp(top, mid, t / 0.5) if t < 0.5 else lerp(mid, bot, (t - 0.5) / 0.5)
    d.line([(0, y), (BIG, y)], fill=c)

# 右上からの日差し(放射状にぼかした光)
# 色は日差しの色で固定し、不透明度だけをぼかす(透明な黒が混ざって灰色ににじむのを防ぐ)。
cx, cy, r = 100 * U, 8 * U, 46 * U
mask = Image.new('L', (BIG, BIG), 0)
ImageDraw.Draw(mask).ellipse([cx - r * 0.6, cy - r * 0.6, cx + r * 0.6, cy + r * 0.6], fill=190)
mask = mask.filter(ImageFilter.GaussianBlur(r * 0.35))
sun = Image.new('RGBA', (BIG, BIG), (255, 246, 200, 255))
sun.putalpha(mask)
img = Image.alpha_composite(img, sun)

# 奥の丘・手前の丘
d = ImageDraw.Draw(img)
hill1 = cubic((0, 80), (26, 70), (52, 76), (108, 70)) + [(108 * U, BIG), (0, BIG)]
hill2 = cubic((0, 92), (34, 84), (70, 90), (108, 84)) + [(108 * U, BIG), (0, BIG)]
d.polygon(hill1, fill=hexc('#A6DB7E'))
d.polygon(hill2, fill=hexc('#7DC257'))

img = img.resize((OUT, OUT), Image.LANCZOS)
S = OUT / 108.0


def paste(name, left, top, size, shadow=True):
    src = Image.open(f'{res_dir}/{name}.png').convert('RGBA')
    px = round(size * S)
    sprite = src.resize((px, px), Image.LANCZOS)
    pos = (round(left * S), round(top * S))
    if shadow:
        # 地面に落ちるやわらかい影で、3Dの立体感を強める。
        sh = Image.new('RGBA', img.size, (0, 0, 0, 0))
        alpha = sprite.split()[3].point(lambda a: int(a * 0.28))
        tint = Image.new('RGBA', sprite.size, (74, 45, 25, 255))
        tint.putalpha(alpha)
        sh.paste(tint, (pos[0], pos[1] + round(2.2 * S)), tint)
        sh = sh.filter(ImageFilter.GaussianBlur(2.2 * S))
        img.alpha_composite(sh)
    img.alpha_composite(sprite, pos)


# ストアの角丸アイコンは円形マスクより見える範囲が広いので、アプリ内より少し大きく置く。
paste('animal_fox', 23, 36, 62)
paste('icon_sparkling_heart', 39, 13, 30)

img.convert('RGB').save(out_path, 'PNG', optimize=True)
print('saved', out_path, img.size)
