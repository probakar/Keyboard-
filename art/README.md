# Art sources

`launcher_source.png` is the master artwork for the launcher icon. The density-specific
`mipmap-*` PNGs under `app/src/main/res/` are generated from it:

```bash
for density in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  name="${density%%:*}"; size="${density##*:}"
  convert art/launcher_source.png -resize "${size}x${size}" \
    "app/src/main/res/mipmap-$name/ic_launcher.png"
done
```

The adaptive icon (Android 8+) uses `@drawable/ic_launcher_foreground` and
`@color/ic_launcher_background` instead, so this bitmap is only the legacy fallback.
