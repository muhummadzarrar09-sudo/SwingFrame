# SwingFrame: The Ultimate Free AI Golf Coach

An on-device Android application for golf swing analysis. No cloud processing, no backend
servers, no API fees. The app holds **no network permission** — your swings stay on your phone.

## What's Built (MVP — Phase 1 & 2)

- **Precision playback:** exact seeking, frame-rate-aware frame stepping, slow-mo speeds
  (0.1x–1x), and a custom scrubber built on Media3 (ExoPlayer).
- **On-device AI pose tracking:** ML Kit Pose Detection (Accurate) maps the 12 key joints
  (shoulders, elbows, wrists, hips, knees, ankles) fully offline, with a letterbox-aware
  skeleton overlay that stays aligned to the displayed video — including portrait footage.
- **AI diagnostics:** automatic swing-phase detection (Setup → Top → Impact), spine-angle /
  sway / tempo heuristics, a 0–100 swing score, and an honest "insufficient data" state when
  the video can't support a diagnosis (the app never invents a score).
- **Handedness-agnostic:** the analyzer self-calibrates the lead hand from the video, so it
  works for left- and right-handed golfers without a settings toggle.

## Roadmap (not yet shipped)

Auto-trim & clip export (Media3 Transformer), Ghost Mode overlay, Auto-Capture, Live Audio
Coach, progress dashboard with local persistence (Room). See `docs/architecture/FEATURES.md`.

## Tech Stack

- Android Native (Kotlin), Jetpack Compose UI
- Media3 (ExoPlayer) with `SeekParameters.EXACT` + `MediaMetadataRetriever` frame extraction
- ML Kit Pose Detection (Accurate) — on-device inference
- No Room/SQLite yet (planned for Phase 3 progress tracking)

## Privacy Notes

- The app declares no `INTERNET` permission and transmits nothing.
- The ML Kit pose model is delivered by Google Play Services (downloaded at install/first
  use on devices with Play Services; requires network at that moment, then runs fully
  offline). Bundling the model is a planned follow-up for fully-offline devices.

## Development

```bash
./gradlew testDebugUnitTest   # unit tests (analyzer, timeline, formatting)
./gradlew assembleDebug       # debug APK
./gradlew lintDebug           # Android lint
```

Windows: `.\swingframe-build.ps1` builds, installs, and tails logcat (see script header).
CI (`.github/workflows/ci.yml`) runs unit tests, debug build, and lint on every push/PR.
