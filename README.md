# SwingFrame

A personal, fully offline Android golf-analysis workspace. The current test-candidate source is **0.5.1-hardening**: exact-frame media, editorial performance, vector annotations, local projects/bookmarks, export, frame-identity safety, source fingerprints, and controller separation.

## Phase 1 included

- Kotlin + Jetpack Compose native app (`app.swingframe`)
- Android system document picker restricted to `video/*`
- Persisted read access to the selected document
- Pre-analysis metadata screen: duration, resolution, detected FPS, codec, frame estimate, and bitrate
- Actual video sample timestamp indexing with `MediaExtractor` (no assumed constant frame interval)
- Media3 1.10.1 `FrameExtractor` for asynchronous frame previews
- Bounded memory-aware LRU bitmap cache; 4K frames are scaled to a 1920 px preview edge
- Amber editorial Frame Viewer with a thumbnail filmstrip, exact playhead, hardware-first decoding, directional prefetch, per-frame stepping, timestamp/frame/FPS readouts, zoom/pan, and 0.1×–1× playback
- Straight line, measured angle, vertical/horizontal plumb, box, circle, and freehand vector tools
- Selection handles, move/edit, color, stroke width, hide/show, carry-forward, per-frame undo/redo, delete, and clear-frame
- App-private atomic JSON persistence for normalized per-frame vectors
- Recent local projects with last-frame resume and source re-linking
- Address/Top/Impact/Finish/custom bookmarks with timeline ticks and jump/delete controls
- Full-resolution clean/annotated PNG export and frame-range MP4 export with local progress/cancellation
- Clear corrupt/unsupported/decode error states
- No account, backend, analytics, ads, billing, or `INTERNET` permission

## Deliberately deferred

Comparison mode, local pose/tempo intelligence, assisted shot/club tracking, and an FFmpeg fallback remain later phases. The application continues to have no account, backend, ads, billing, or network permission.

## Toolchain

- Min SDK 29 (Android 10)
- Compile/target SDK 36
- Android Gradle Plugin 8.13.2
- Gradle 8.13
- JDK 17
- Kotlin + Compose compiler 2.3.21
- Compose BOM 2026.06.00
- Media3 1.10.1

## Build an APK

### Windows PowerShell (recommended for Muhammad)

Install Android Studio plus Android SDK Platform 36 and Build-Tools 36.0.0, then run:

```powershell
cd C:\Dev\SwingFrame
Set-ExecutionPolicy -Scope Process Bypass
.\swingframe-build.ps1
```

Install over USB after enabling USB debugging:

```powershell
.\scripts\install-windows.ps1
```

See [`docs/WINDOWS_BUILD.md`](docs/WINDOWS_BUILD.md) for manual commands and troubleshooting.

### Existing Android development setup

Set `ANDROID_SDK_ROOT` (or create `local.properties` with `sdk.dir=...`) and use JDK 17:

```bash
./gradlew testDebugUnitTest assembleDebug
mkdir -p artifacts
cp app/build/outputs/apk/debug/app-debug.apk artifacts/SwingFrame.apk
```

### Bootstrap from a Linux terminal

The helper downloads a local JDK 17 and Android command-line SDK into `~/.cache/swingframe-toolchain`, accepts SDK licenses, runs unit tests, builds, and copies the APK:

```bash
chmod +x scripts/*.sh
./scripts/bootstrap-build.sh
```

Output:

```text
artifacts/SwingFrame.apk
```

## Install on a phone

Enable **Developer options → USB debugging**, connect the phone, approve its RSA prompt, then:

```bash
./scripts/install-debug.sh
```

Or copy `artifacts/SwingFrame.apk` to the phone and open it to sideload. Android may ask you to allow installs from the app used to open the APK.

## Architecture notes

`VideoProbe` performs a fast metadata pass. `FrameIndexer` then records the source track's real presentation timestamps and detects variable timing. `SwingFrameViewModel` owns the session state, cache, frame requests, and playback loop. `Media3FrameDecoder` is kept on one application thread as required by `FrameExtractor`.

The UI never keeps every source frame as a full-resolution bitmap. The timeline is complete, but pixels are decoded on demand and cached around actual use. This avoids the multi-gigabyte memory cost of eagerly materializing 4K/high-FPS clips while retaining frame-addressable behavior.

## Device validation still required

“Any common format” cannot be honestly guaranteed without testing the target phone's hardware codecs. Phase 1 intentionally proves the standard Media3/platform path first. Test H.264, HEVC, MOV, WebM, MKV, 30/60/120/240 FPS, VFR, HDR, 1080p, and 4K clips. Failed real-world samples will define the FFmpeg fallback scope instead of adding a large native dependency blindly.
