import urllib.request
import os

url = "https://github.com/google/fonts/raw/main/ofl/playfairdisplay/static/PlayfairDisplay-Italic.ttf"
output_path = "app/src/main/res/font/playfair_italic.ttf"

print("Downloading font...")
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req) as response, open(output_path, 'wb') as out_file:
    out_file.write(response.read())

print(f"Downloaded {os.path.getsize(output_path)} bytes.")
