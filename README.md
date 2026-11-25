# SnipText

Universal offline screen-text grabber for Android 8.0+ (Oreo and above).

## How it works
1. **Press Volume Up + Volume Down** simultaneously anywhere on your phone.
2. **Drag across any area** on the frozen screen (images, videos, games, or locked apps).
3. **Copy instantly** — on-device OCR extracts the text so you can edit, copy, or auto-copy it in one tap.

## Highlights
- **Volume Up + Down Chord Trigger** via `AccessibilityService` (consumes the combo without changing your volume).
- **Hybrid Screen Capture**: Native `takeScreenshot()` on Android 11+ and `MediaProjection` fallback on Android 8.0–10.
- **Offline ML Kit OCR**: Fast, private, on-device text recognition (~4 MB APK).
- **Smart Copy Tools**: Auto-Copy on release, Join Line Breaks, Quick Settings Tile, and Recent Grabs history.
- **Built-in Updater**: Checks GitHub Releases for newer builds and shows release notes.
