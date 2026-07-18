# SwingFrame next-chat handoff

**Prepared:** July 18, 2026 (round 4 — compile fix + full-repo bug hunt)
**Repository:** `muhummadzarrar09-sudo/SwingFrame`
**Required branch:** `arena/019f6f8c-swingframe`
**Current status:** the owner's reported compile failure and 13 additional bugs are fixed in source, statically reviewed, and `git diff --check` clean — but **nothing has been compiled or run yet**. Arena has no JDK/Android SDK. The very next step is the owner running the Gradle gate from Windows.

## Instructions for the next coding agent

You are continuing a deep stabilization pass. Do **not** reset, discard, overwrite, or switch away from the existing branch. The working tree intentionally contains the previous agents' changes.

### Read these documents first, in order

1. [`BUG_HUNT_2026-07-18.md`](BUG_HUNT_2026-07-18.md) — what was just changed and why
2. [`ENGINEERING_AUDIT_2026-07-17.md`](ENGINEERING_AUDIT_2026-07-17.md) — the full audit and open blockers
3. [`DEVICE_SMOKE_CHECKLIST.md`](DEVICE_SMOKE_CHECKLIST.md)
4. [`TEST_MATRIX.md`](TEST_MATRIX.md)
5. [`PHASE4_1_HARDENING.md`](PHASE4_1_HARDENING.md)
6. This handoff document.

## What just happened (round 4 summary)

The owner ran `.\swingframe-build.ps1` and hit a Kotlin compiler error:

```
FrameViewerScreen.kt:215:17 'fun ColumnScope.AnimatedVisibility(...)' cannot be called
in this context with an implicit receiver. Use an explicit receiver if necessary.
```

That was fixed (explicit `this@Column` receiver for the badge, transition-state-driven plain `AnimatedVisibility` for the error overlay), along with a second identical latent error at the error-overlay call site. The owner then asked for a full bug hunt; 13 issues were fixed across viewer, home, export, playback, annotations, and build config, plus 5 new regression tests (`CarryForwardTest`). Full detail, severity grouping, and the deliberate no-fix list are in [`BUG_HUNT_2026-07-18.md`](BUG_HUNT_2026-07-18.md).

The highest-stakes fixes to sanity-check after the build passes:

- **Home empty-state crash on first launch** (weighted `Spacer` inside a vertically scrolling `Column`). Verify first-launch Home renders and scrolls.
- **`OverlayEffect` Guava `ImmutableList`** in `ExportManager.kt` — probable second compile error hiding behind the first.
- **Carry-forward parity** — preview, still export, and video export now share one rule (`annotation/CarryForward.kt`). Verify an annotated video export matches the preview ghosts.
- **Export stroke/text scale** — now density-equivalent (`minDim/360`). Burned-in annotations should look the same weight as on-screen.

## First response to the owner

After reading the documents and inspecting `git status`, ask the owner for the **complete, unedited Gradle console output**, including the first error and the final task summary.

Ask them to run from Windows PowerShell:

```powershell
cd D:\fun` projects` out` of` boredom\SwingFrame   # owner's actual path
.\swingframe-build.ps1 2>&1 | Tee-Object -FilePath .\swingframe-build-output.txt
```

The script runs `testDebugUnitTest → lintDebug → assembleDebug` and copies the APK to `artifacts\SwingFrame.apk` with a SHA-256. For a faster loop on compile-only failures:

```powershell
.\gradlew compileDebugKotlin --stacktrace
```

Request either the attached `swingframe-build-output.txt` or the entire output from the first `FAILURE:`/compiler error through the final lines. Do not diagnose from a screenshot of the last line — Gradle often reports the useful error hundreds of lines earlier.

Owner's known-good environment: JDK 17 (`Eclipse Adoptium jdk-17.0.19.10-hotspot`), Android SDK present, `compileSdk/targetSdk 36`, `minSdk 29`, Kotlin 2.3.21, AGP 8.13.2, Gradle 8.13 wrapper.

## Before changing code

Run:

```bash
git status --short --branch
git diff --check
git diff --stat
```

Confirm the branch is exactly `arena/019f6f8c-swingframe`. Do not run `git reset --hard`, `git clean`, checkout another branch, or regenerate the project. Preserve all existing edits and untracked audit/test/resource files.

## Path A — the Gradle build fails

### 1. Classify the earliest real failure

Fix the **first root error only**; later errors are usually cascades. Classify as: Kotlin compiler / AAPT resource / manifest merge / unit test / lint / R8 / toolchain / network.

### 2. Reproduce the narrowest failing task

```bash
./gradlew compileDebugKotlin --stacktrace
./gradlew testDebugUnitTest --stacktrace
./gradlew lintDebug --stacktrace
./gradlew assembleDebug --stacktrace
```

Arena cannot run these (no JDK/SDK). Use the owner's complete output as authoritative; make source fixes locally, run all static checks available here, and clearly state what the owner must rerun.

### 3. Fix root causes, not symptoms

For every fix:

- Preserve cancellation semantics; never swallow `CancellationException`.
- Do not catch fatal VM errors merely to keep the app alive.
- Keep the manifest free of `INTERNET` and broad-storage permissions.
- Preserve requested/resolved frame identity.
- Preserve project and annotation data across failures.
- Keep touch targets at least 48 dp.
- Do not reintroduce overlapping fixed-offset viewer controls.
- Keep preview / still-export / video-export rendering rules identical (see `CarryForward.kt` and the `AnnotationBitmapRenderer` scale comment).
- Add or update a regression test whenever logic can be tested outside a device.

### 4. Rerun the narrow task, then the complete gate

After the narrow task passes:

```bash
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Report exactly which tasks passed. Do not say "build fixed" if only compilation passed but tests/lint/R8 did not.

## Path B — the Gradle build succeeds

Treat the compiler gate as passed only if the output shows `testDebugUnitTest`, `lintDebug`, `assembleDebug` all succeeded, `BUILD SUCCESSFUL`, and `artifacts\SwingFrame.apk` exists with a printed SHA-256. Record the SHA-256 and build date in the test notes.

### Release/R8 gate

`swingframe-build.ps1` does **not** cover release. Once debug passes, also run:

```powershell
.\gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleRelease
```

R8 has never run against this codebase; expect reachable-code/keep-rule findings from Media3/Transformer and fix them before calling the build gate done.

### Install

```powershell
.\scripts\install-windows.ps1
```

If installation fails, collect `adb devices`, `adb install -r .\artifacts\SwingFrame.apk`, and `adb shell getprop ro.build.version.release / ro.product.manufacturer / ro.product.model`. Prefer `adb install -r` to preserve existing projects; only uninstall with the owner's explicit consent.

## Device test sequence after a successful build

Use [`DEVICE_SMOKE_CHECKLIST.md`](DEVICE_SMOKE_CHECKLIST.md) step by step and [`TEST_MATRIX.md`](TEST_MATRIX.md) for codec fixtures. Run gates in this order:

### Gate 1 — launch and navigation

1. Launch; **first-launch Home must render and scroll** (regression check for the empty-state fix).
2. Cancel the document picker.
3. Import a normal 1080p H.264 clip.
4. System Back at Source Review, Indexing, Viewer.
5. Cancel Indexing; prove Viewer does not reopen itself.

On crash: `adb logcat -c`, reproduce once, `adb logcat -d -v threadtime > .\swingframe-logcat.txt`. Diagnose the earliest `FATAL EXCEPTION` / MediaCodec / Transformer trace.

### Gate 2 — viewer layout

Default and large font/display sizes. Header, frame, tool bar, action bar, timeline never overlap; tool row scrolls; filename readable; menus open; Source Review and Export Setup scroll; TalkBack works. Screenshot Home, Source Review, Viewer, overflow menu, bookmark dialog, export setup.

### Gate 3 — frame correctness and races

Scrub beginning → end → middle while thumbnails load; confirm frame number, timestamp, bitmap, and annotations agree; step forward/back; change speed during playback; background during playback must return paused. **Play a clip end-to-end and confirm the video does not lag its audio/timestamps** (regression check for the wall-clock cadence fix). Any stale final frame is P0.

### Gate 4 — annotation persistence and geometry

All tools; non-axis-aligned angle on 16:9; **tap the angle tool without dragging and confirm no 0° ghost remains** (regression check); edge movement, undo/redo, carry, hide/show, clear, freehand; relaunch persistence; relink match/mismatch/mismatch-confirm. Existing projects may get a one-time fingerprint mismatch from the sampled-fingerprint upgrade — and note the known open issue: `LEGACY_UNKNOWN` (blank fingerprint) projects reopen **without** any validation prompt; fixing that needs a warn-and-confirm UX decision.

### Gate 5 — export

Short 1080p H.264 first: clean still, annotated still, 1× w/ audio, 0.5×, 0.25×, cancelled export. **Compare an annotated still and an annotated video against the preview frame: carried ghosts must appear identically on all three, and stroke/text weight must match on-screen proportions** (regression checks for the parity and scale fixes). Then portrait, HEVC, VFR, 4K, HDR. Long export is still process-bound — a known release blocker until foreground durable work lands.

### Gate 6 — memory and performance

Probe/index duration, first-frame latency, warm/cold seek, peak memory on repeated 4K seeks and annotated 4K still export, thermal behavior during video export (`adb shell dumpsys meminfo app.swingframe`).

## How to report device findings

```text
Build SHA-256:
Phone / Android version:
Source container / codec / resolution / FPS / rotation / HDR:
Exact steps:
Expected:
Actual:
Frequency:
Screenshot or recording:
Relevant logcat:
```

Do not fix vague reports by guessing. Reproduce or obtain enough evidence to identify the responsible layer.

## Definition of success for this checkpoint

1. Unit tests pass (including the new `CarryForwardTest`).
2. Lint passes with no release-blocking findings.
3. Debug APK assembles and installs.
4. Minified release assembly/R8 passes.
5. Home/import/viewer/export layouts have no clipping at target dimensions and large font.
6. Rapid seeks never display stale frame identity; playback holds wall-clock cadence.
7. Projects, annotations, bookmarks survive relaunch and relinking.
8. Required 1080p still/video exports pass timing/orientation checks, and burned-in annotations match the preview (carry + stroke weight).
9. No crash or silent data loss in the smoke sequence.
10. All results recorded in the device checklist/matrix.

Do **not** begin Compare, AI pose, tempo, or shot-tracing feature work before this checkpoint is closed.

## Work remaining after the checkpoint

1. `LEGACY_UNKNOWN` fingerprint warn-and-confirm flow (needs product decision).
2. Angle label edge-clipping placement rule.
3. Process-durable foreground video export.
4. Landscape/tablet/foldable viewer composition (portrait stays locked until tested).
5. Project-ID-based annotation storage migration.
6. In-app quarantine recovery/export for corrupt local JSON.
7. String-resource localization and plurals.
8. Freehand geometric simplification.
9. Instrumentation, screenshot, accessibility, benchmark, and memory suites.
10. Baseline profile and startup benchmark.
11. Signed release configuration, privacy policy, release checklist, store assets.
12. Promote to `1.0.0-rc1` only after the complete gate.

## Copy/paste prompt for a new chat

> Continue the SwingFrame stabilization work on the existing Arena branch `arena/019f6f8c-swingframe`. Do not reset or discard the working tree. First read `docs/BUG_HUNT_2026-07-18.md` and `docs/NEXT_CHAT_HANDOFF.md`, then `docs/ENGINEERING_AUDIT_2026-07-17.md`, `docs/DEVICE_SMOKE_CHECKLIST.md`, and `docs/TEST_MATRIX.md`. Inspect git status and the current diff. Ask me for the complete `swingframe-build-output.txt` from running `.\swingframe-build.ps1` in Windows PowerShell. If the build failed, identify and fix the earliest root compiler/test/lint/resource error only, add regression coverage, and tell me exactly what to rerun. If it succeeded, verify the success markers and SHA-256, run the release/R8 gate, then guide me through install, logcat collection, and the device smoke checklist — including the first-launch Home, playback cadence, no-ghost-angle-tap, and preview-vs-export carry/stroke parity regression checks — before adding any new features.
