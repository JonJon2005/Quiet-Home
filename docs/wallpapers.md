# Bundled landscape photographs

The app includes three approved real photographs, bundled as **3840 × 2160** JPEGs in `assets/wallpapers/`. Each was center-cropped to 16:9 and downsampled from the full-resolution original, with no upscaling or generative changes. All work offline.

## Sources and credits

| App name / asset | Photographer / source | Original dimensions | License |
| --- | --- | --- | --- |
| Lake McDonald / `alpine.jpg` | Glacier National Park / National Park Service | 6016 × 4000 | U.S. public domain, federal government work |
| McWay Falls / `coast.jpg` | adrian / aows, via Unsplash and Wikimedia Commons | 6000 × 4000 | CC0 1.0; published on Unsplash before its June 2017 license change |
| Great Sand Dunes / `dunes.jpg` | Patrick Myers / National Park Service | 6000 × 4000 | U.S. public domain, federal government work |

### Lake McDonald, Montana

[Source and licensing](https://commons.wikimedia.org/wiki/File:Lake_McDonald_with_Mt_Cannon,_Mt_Brown,_Little_Matterhorn,_Mt_Edwards,_and_Gunsight_Mountain_(12091853594).jpg). Taken 20 January 2014. Warm evening light on the mountains reflected in Lake McDonald.

### McWay Falls, California

[Source and licensing](https://commons.wikimedia.org/wiki/File:Photograph_of_the_ocean_from_the_cliff_in_McWay_Falls.jpg). Photographer: [adrian / aows](https://unsplash.com/@aows). Original title: Live postcard. [CC0 1.0 dedication](https://creativecommons.org/publicdomain/zero/1.0/).

### Great Sand Dunes, Colorado

[Source and licensing](https://commons.wikimedia.org/wiki/File:Clearing_Storm_over_Dunes_(52390197290).jpg). Title: Clearing Storm over Dunes. Photograph by NPS/Patrick Myers, taken 1 September 2019. A late-summer thunderstorm clears at sunset.

The historical asset filenames are retained so existing wallpaper selections continue to work. These photographs replace the generated artwork in the earlier development build.

## Rendering

Choose a scene in **Customize → Background**. The **Tint wallpaper** slider changes the background image, while **Tint glass** separately darkens glass tiles and buttons (0–85%). Glass tint updates shader surfaces live without rebuilding the page; it preserves the edge highlights, saves independently, and is disabled when Liquid Glass is off. Neither control changes panel brightness. Text stays light on photos with subtle shadows. Selecting OLED clears the photo to preserve the pure-black backdrop; choosing any gradient clears the photo and restores gradient mode.

The selected image decodes on a worker thread. A bounded bitmap cache avoids repeated decode work when revisiting a scene. At a 1920-pixel-wide UI the decoder uses a 1920 × 1080 bitmap; a 4K UI can use the full-size asset. The TV panel's physical resolution can differ from the Android UI resolution. Images center-crop to the viewport.

Liquid Glass refracts the actual static wallpaper through a shared bitmap shader. Dispersion is sampled near edges, and hardware layers cache material surfaces to keep navigation responsive. The app still has no network permission or background animation loop.
