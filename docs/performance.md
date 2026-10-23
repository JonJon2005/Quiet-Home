# Navigation check: Liquid Glass development build

Measured locally on the same TCL 65QM6K / G08 / Android 14 TV with `adb shell dumpsys gfxinfo local.quiethome`, resetting statistics before each run. A short sequence of 24 left/right D-pad events, spaced by approximately 180 ms plus input command overhead, exercised the Home screen.

| Reported metric | Prior build | Glass development build |
| --- | ---: | ---: |
| Frames rendered | 246 | 168 |
| Median frame time | 9 ms | 5 ms |
| 95th percentile | 10 ms | 5 ms |
| 99th percentile | 32 ms | 5 ms |
| Median GPU time | 7 ms | 2 ms |
| Missed Vsync | 0 | 0 |
| High input latency count | 12 | 0 |
| Janky frames | 0 | 0 |
| Legacy janky frames | 6 | 0 |

This is a small, warm-navigation sample, not a general benchmark or end-to-end remote-latency measurement. The animation duration changed from 150 ms to 100 ms, so the rendered-frame counts differ. It excludes shader cold-start cost and does not measure TCL's HDMI tuning time. Both versions met the modern jank threshold in this sample.

The development build coalesces input callbacks, skips rebuilding identical input rows, caches surface backgrounds, and cancels/restarts short focus animations. Glass renders only within the tile/button bounds. There is no continuous animation loop, backdrop screenshot capture, bitmap copying, or blur pass.

## Glass approximation

`GlassDrawable` uses Android 13+ AGSL to analytically sample the launcher's known gradient through a rounded lens. Edge normals bend the sample coordinates; separate color offsets approximate edge dispersion. Directional rim lighting, Fresnel-like edge strength, an internal highlight, and a short focus-driven reflection supply depth. Text and icons remain sharp above the material.

This is an original glass emulation, not Apple's renderer. It refracts this app's gradient or bundled static wallpaper, not arbitrary apps, HDMI video, or system UI. OLED keeps the background black; Light uses reduced highlight strength. Older Android versions keep standard surfaces.

## Wallpaper navigation check

The same short D-pad sequence with Alpine Lake and photo-refraction glass initially reported a 26 ms median frame time and 20 janky frames. Adding cached hardware layers and limiting dispersion texture samples to the lens edges reduced this to a 14 ms median, 15 ms 95th percentile, 11 ms median GPU time, 0 high-input-latency events, and 0 janky frames across 168 rendered frames. This remains a short warm-navigation sample, not a guarantee across all scenes or workloads.
