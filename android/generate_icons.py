import os
from PIL import Image

def process_icon():
    img_path = '/Users/pranayburra/.gemini/antigravity/brain/52519c5c-c1eb-4699-a1db-7a6918977b44/media__1782067151406.jpg'
    img = Image.open(img_path).convert("RGBA")

    # Get bounding box of non-transparent pixels
    bbox = img.getbbox()
    if bbox:
        img = img.crop(bbox)

    w, h = img.size
    
    # Make sure it's a square
    if w != h:
        size = max(w, h)
        square_img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        square_img.paste(img, ((size - w) // 2, (size - h) // 2))
        img = square_img

    # Sample the background color from near the top edge
    bg_pixel = img.getpixel((img.width // 2, 5))
    bg_color = f"#{bg_pixel[0]:02x}{bg_pixel[1]:02x}{bg_pixel[2]:02x}"

    res_dir = '/Users/pranayburra/Projects/Android/android/app/src/main/res'
    densities = {
        'mdpi': (48, 108),
        'hdpi': (72, 162),
        'xhdpi': (96, 216),
        'xxhdpi': (144, 324),
        'xxxhdpi': (192, 432)
    }

    for density, (legacy_size, adaptive_size) in densities.items():
        mipmap_dir = os.path.join(res_dir, f'mipmap-{density}')
        os.makedirs(mipmap_dir, exist_ok=True)
        
        # Legacy icon
        legacy_img = img.resize((legacy_size, legacy_size), Image.Resampling.LANCZOS)
        legacy_img.save(os.path.join(mipmap_dir, 'ic_launcher.png'))
        legacy_img.save(os.path.join(mipmap_dir, 'ic_launcher_round.png'))
        
        # Adaptive foreground
        adaptive_img = img.resize((adaptive_size, adaptive_size), Image.Resampling.LANCZOS)
        adaptive_img.save(os.path.join(mipmap_dir, 'ic_launcher_foreground.png'))

    # Write ic_launcher_background.xml
    drawable_dir = os.path.join(res_dir, 'drawable')
    os.makedirs(drawable_dir, exist_ok=True)
    with open(os.path.join(drawable_dir, 'ic_launcher_background.xml'), 'w') as f:
        f.write(f'<?xml version="1.0" encoding="utf-8"?>\n<color xmlns:android="http://schemas.android.com/apk/res/android" android:color="{bg_color}" />')

    # Write ic_launcher.xml and ic_launcher_round.xml
    anydpi_dir = os.path.join(res_dir, 'mipmap-anydpi-v26')
    os.makedirs(anydpi_dir, exist_ok=True)
    ic_launcher_xml = f'''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
'''
    with open(os.path.join(anydpi_dir, 'ic_launcher.xml'), 'w') as f:
        f.write(ic_launcher_xml)
    with open(os.path.join(anydpi_dir, 'ic_launcher_round.xml'), 'w') as f:
        f.write(ic_launcher_xml)
        
    print(f"Generated icons! Background sampled as: {bg_color}")

if __name__ == '__main__':
    process_icon()
