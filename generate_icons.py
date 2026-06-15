#!/usr/bin/env python3
"""
Run this script once to generate a simple blue launcher icon.
Requires: pip install Pillow
Output: app/src/main/res/mipmap-hdpi/ic_launcher.png
"""
try:
    from PIL import Image, ImageDraw, ImageFont
    import os

    sizes = {
        "mipmap-mdpi":    48,
        "mipmap-hdpi":    72,
        "mipmap-xhdpi":   96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi":192,
    }

    base = os.path.dirname(os.path.abspath(__file__))
    res  = os.path.join(base, "app", "src", "main", "res")

    for folder, size in sizes.items():
        out_dir = os.path.join(res, folder)
        os.makedirs(out_dir, exist_ok=True)

        img  = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)

        # Blue circle background
        margin = size // 8
        draw.ellipse([margin, margin, size - margin, size - margin],
                     fill=(21, 101, 192, 255))

        # "CSE" text
        font_size = size // 4
        try:
            font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
                                      font_size)
        except Exception:
            font = ImageFont.load_default()

        text = "CSE"
        bbox = draw.textbbox((0, 0), text, font=font)
        tw = bbox[2] - bbox[0]
        th = bbox[3] - bbox[1]
        draw.text(((size - tw) / 2, (size - th) / 2 - bbox[1]),
                  text, fill="white", font=font)

        img.save(os.path.join(out_dir, "ic_launcher.png"))
        print(f"Created {folder}/ic_launcher.png ({size}×{size})")

    print("All icons generated successfully.")

except ImportError:
    print("Pillow not installed. Run:  pip install Pillow  then re-run this script.")
