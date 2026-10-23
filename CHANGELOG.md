# Changelog

## Unreleased — 0.2 development

- Three offline landscape wallpapers, bundled at 3840 × 2160 (upscaled generated artwork), with a separate wallpaper-darkening control.
- Larger clock at the bottom left and compact Settings/AirPlay/Customize controls at the top right.
- Wallpaper-aware glass refraction, background loading on a worker thread, and GPU surface caching.

- Optional Liquid Glass material in Customize: GPU-rendered gradient refraction, edge dispersion, directional rim highlights, internal caustics, and a focus-responsive sheen. Available on Android 13+; conventional surfaces remain available.
- Faster 100 ms focus transitions with cached backgrounds.
- Coalesced input callbacks and skipped unchanged tile rebuilds, preserving focus on return.
- Home returns from Customize to the input grid.
- Tested on TCL 65QM6K with Dark, Light, and OLED themes and HDMI-to-Home navigation. See `docs/performance.md` for the short on-device navigation comparison.

## v0.1

Initial release of Quiet Home for TCL G08 Google TVs, tested on a TCL 65QM6K running Android 14.

- HDMI tiles with device names, live connection status, and local renaming.
- Native TCL input switching, plus Settings and AirPlay shortcuts.
- Dark, OLED black, and Light themes.
- Eight gradient palettes with dark and light variants.
- TV backlight slider using TCL's display framework, with remote arrow controls.
- No internet permission, ads, analytics, accounts, or third-party app libraries.

TCL-specific brightness and launcher behavior can vary by firmware. Physical HDMI, CEC, AirPlay, and HomeKit checks are still required on each target TV.
