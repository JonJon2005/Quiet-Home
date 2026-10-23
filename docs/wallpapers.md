# Bundled landscape wallpapers

The app includes three local JPEGs in `assets/wallpapers/`, each **3840 × 2160**: `alpine.jpg`, `coast.jpg`, and `dunes.jpg`. All work offline. Images were created with the built-in image-generation tool, then converted to JPEG and resized for app packaging.

The image generator returned 1672 × 941 originals despite the 4K request. The shipped files are upscaled to 4K dimensions, not native 4K captures. Slight aspect normalization makes them exactly 16:9. The original generated compositions were preserved.

## Prompt set

Shared prompt for each image:

> Use case: photorealistic-natural. Asset type: bundled Android TV landscape wallpaper. Generate a single finished ultra-wide 16:9 wallpaper, exactly 3840x2160 pixels if supported, full bleed. [Scene below] Composition: expansive quiet sky in the upper third, landscape detail toward the right and lower half; subdued uncluttered left side for UI text. Premium understated Apple TV-like scenic wallpaper mood. No text, logos, borders, people, buildings, collages or UI. Output one image.

Scenes:

- **Alpine lake:** Alpine lake at blue hour, layered mountain silhouettes and mirror-still water, soft mist, deep navy and cool slate blue tones, subtle warm light on distant snowy peaks, tranquil photographic natural landscape.
- **Coastal dusk:** Dramatic sea cliffs above a calm ocean at dusk, soft peach horizon fading to indigo sky, muted teal water, coastal headlands receding into haze, tasteful cinematic landscape photography.
- **Desert dunes:** Minimal sweeping desert dunes at sunset, sculptural smooth sand ridges, warm amber highlights, deep mauve shadows, distant hazy mountains, calm atmospheric fine-art landscape photography.

## Rendering

Choose a scene in **Customize → Background**. The separate **Wallpaper darkening** slider changes only the background image, not panel brightness. Text stays light on photos with subtle shadows. Selecting OLED clears the photo to preserve the pure-black backdrop; choosing any gradient clears the photo and restores gradient mode.

The selected image decodes on a worker thread. A bounded bitmap cache avoids repeated decode work when revisiting a scene. At a 1920-pixel-wide UI the decoder uses a 1920 × 1080 bitmap; a 4K UI can use the full-size asset. The TV panel's physical resolution can differ from the Android UI resolution. Images center-crop to the viewport.

Liquid Glass refracts the actual static wallpaper through a shared bitmap shader. Dispersion is sampled near edges, and hardware layers cache material surfaces to keep navigation responsive. The app still has no network permission or background animation loop.
