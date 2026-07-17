# SwingFrame next-chat handoff

**Prepared:** July 17, 2026  
**Repository:** `muhummadzarrar09-sudo/SwingFrame`  
**Required branch:** `arena/019f6d62-swingframe`  
**Current status:** deep static hardening is implemented but has not yet passed a real JDK/Android Gradle build in Arena.

## Instructions for the next coding agent

You are continuing a deep stabilization pass. Do **not** reset, discard, overwrite, or switch away from the existing branch. The working tree intentionally contains the previous agent's changes.

### Read these documents first, in order

1. [`ENGINEERING_AUDIT_2026-07-17.md`](ENGINEERING_AUDIT_2026-07-17.md)
2. [`DEVICE_SMOKE_CHECKLIST.md`](DEVICE_SMOKE_CHECKLIST.md)
3. [`TEST_MATRIX.md`](TEST_MATRIX.md)
4. [`PHASE4_1_HARDENING.md`](PHASE4_1_HARDENING.md)
5. This handoff document.

The engineering audit is the primary source of truth. It records what was found, what was changed, what remains open, and which claims are not yet runtime-verified.

## First response to the owner

After reading the documents and inspecting `git status`, ask the owner for the **complete, unedited Gradle console output**, including the first error and the final task summary.

Ask them to run from Windows PowerShell:

```powershell
cd C:\path\to\SwingFrame
.\swingframe-build.ps1 2>&1 | Tee-Object -FilePath .\swingframe-build-output.txt
```

If they use the smaller helper instead:

```powershell
.\scripts\build-windows.ps1 2>&1 | Tee-Object -FilePath .\swingframe-build-output.txt
```

The scripts run:

```text
testDebugUnitTest → lintDebug → assembleDebug
```

Request either:

- The attached `swingframe-build-output.txt`, or
- The entire output pasted from the first `FAILURE:`/compiler error through the final lines.

Do not diagnose from a screenshot containing only the last line. Gradle frequently reports the useful Kotlin/AAPT error hundreds of lines earlier.

## Before changing code

Run:

```bash
git status --short --branch
git diff --check
git diff --stat
```

Confirm the branch is exactly:

```text
arena/019f6d62-swingframe
```

Do not run `git reset --hard`, `git clean`, checkout another branch, or regenerate the project. Preserve all existing edits and untracked audit/test/resource files.

## Path A — the Gradle build fails

### 1. Classify the earliest real failure

Ignore cascaded errors until the first root error is fixed. Classify it as one of:

- Kotlin compiler/type/API error
- Android resource/AAPT error
- Manifest merge error
- Unit-test failure
- Android Lint failure
- R8/ProGuard failure
- SDK/JDK/toolchain problem
- Dependency download/network problem

### 2. Reproduce the narrowest failing task

Examples:

```bash
./gradlew testDebugUnitTest --stacktrace
./gradlew lintDebug --stacktrace
./gradlew compileDebugKotlin --stacktrace
./gradlew processDebugResources --stacktrace
./gradlew assembleDebug --stacktrace
```

If Arena still has no JDK or network access, use the owner's complete output as authoritative. Make source fixes locally, run all available static checks, and clearly identify anything the owner must rerun.

### 3. Fix root causes, not symptoms

For every fix:

- Preserve cancellation semantics; never swallow `CancellationException`.
- Do not catch fatal VM errors merely to keep the app alive.
- Keep the manifest free of `INTERNET` and broad-storage permissions.
- Preserve requested/resolved frame identity.
- Preserve project and annotation data across failures.
- Keep touch targets at least 48 dp.
- Do not reintroduce overlapping fixed-offset viewer controls.
- Add or update a regression test whenever logic can be tested outside a device.

### 4. Rerun the narrow task, then the complete gate

After the narrow task passes:

```bash
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Then run:

```bash
git diff --check
```

Report exactly which tasks passed. Do not say “build fixed” if only compilation passed but tests/lint/R8 did not.

## Path B — the Gradle build succeeds

If the output contains all of the following, treat the compiler gate as passed:

- `testDebugUnitTest` succeeded
- `lintDebug` succeeded
- `assembleDebug` succeeded
- Final output says `BUILD SUCCESSFUL`
- `artifacts\SwingFrame.apk` exists and the script prints a SHA-256

Record the APK SHA-256 and build date in the test notes. Then proceed to device installation rather than adding features.

### Install

With USB debugging enabled:

```powershell
.\scripts\install-windows.ps1
```

If installation fails, collect:

```powershell
adb devices
adb install -r .\artifacts\SwingFrame.apk
adb shell getprop ro.build.version.release
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
```

For a clean-install migration check, first test `adb install -r` so existing project data is preserved. Only uninstall after the owner explicitly agrees that app-private test data may be erased.

## Device test sequence after a successful build

Use [`DEVICE_SMOKE_CHECKLIST.md`](DEVICE_SMOKE_CHECKLIST.md) as the step-by-step checklist and [`TEST_MATRIX.md`](TEST_MATRIX.md) for codec fixtures.

Run tests in this order:

### Gate 1 — launch and navigation

1. Launch the app.
2. Verify Home is not clipped.
3. Cancel the document picker.
4. Import a normal 1080p H.264 clip.
5. Press system Back at Source Review, Indexing, and Viewer.
6. Cancel Indexing and wait long enough to prove Viewer does not reopen itself.

If the app crashes, collect logs immediately:

```powershell
adb logcat -c
# reproduce once
adb logcat -d -v threadtime > .\swingframe-logcat.txt
```

Ask for `swingframe-logcat.txt`. Diagnose the earliest `FATAL EXCEPTION`, `AndroidRuntime`, MediaCodec, Transformer, or application stack trace.

### Gate 2 — viewer layout

Test default font size and a large font/display-size setting.

Verify:

- Header, frame, tool bar, action bar, and timeline never overlap.
- The horizontal tool row scrolls.
- The filename remains readable.
- More actions and speed menus open correctly.
- Source Review and Export Setup scroll.
- TalkBack can identify tools and adjust the timeline.

Capture screenshots at Home, Source Review, Viewer, overflow menu, bookmark dialog, and export setup. Fix clipping/overlap before media optimization.

### Gate 3 — frame correctness and races

Use a clip with obvious frame changes.

- Scrub beginning → end → middle → beginning while thumbnails load.
- Stop on an identifiable frame.
- Confirm frame number, timestamp, bitmap, and annotations agree.
- Step repeatedly forward/backward.
- Change speed during playback.
- Background the app during playback; it must return paused.

Any stale final frame is a P0 correctness bug. Collect a screen recording plus logcat.

### Gate 4 — annotation persistence and geometry

- Test all tools.
- Test a non-axis-aligned angle on 16:9 media.
- Test edge movement, undo/redo, carry, hide/show, clear, and freehand.
- Relaunch and confirm annotations/bookmarks/last frame survive.
- Test relink match, mismatch cancel, and mismatch confirmation.
- Existing projects may receive a one-time fingerprint mismatch because fingerprints were upgraded to sampled versioned values.

### Gate 5 — export

Start with short 1080p H.264 fixtures:

- Clean still
- Annotated still
- 1× video with audio
- 0.5× video
- 0.25× video
- Cancelled export

Verify frame range, annotation timing, orientation, gallery publication, and partial-file cleanup. Then test portrait rotation, HEVC, VFR, 4K, and HDR.

Long export is still process-bound. Background/process-death durability is a known release blocker; do not mark export production-ready until foreground durable work is implemented and tested.

### Gate 6 — memory and performance

Record:

- Probe/index duration
- First-frame latency
- Warm adjacent seek
- Cold random seek
- Peak memory during repeated 4K seeks
- Peak memory during annotated 4K still export
- Thermal behavior during video export

Use Android Studio Profiler or:

```powershell
adb shell dumpsys meminfo app.swingframe
```

## How to report device findings

For each issue, require:

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

This checkpoint is complete only when:

1. Unit tests pass.
2. Lint passes with no release-blocking findings.
3. Debug APK assembles and installs.
4. Minified release assembly/R8 passes in CI or locally.
5. Home/import/viewer/export layouts have no clipping at target dimensions and large font size.
6. Rapid seeks never display stale frame identity.
7. Projects, annotations, and bookmarks survive relaunch and relinking.
8. Required 1080p still/video exports pass timing/orientation checks.
9. No app crash or silent data loss remains in the smoke sequence.
10. All results are recorded in the device checklist/matrix.

Do **not** begin Compare, AI pose, tempo, or shot-tracing feature work before this checkpoint is closed.

## Work remaining after the checkpoint

Once build/device failures are fixed, continue release hardening in this order:

1. Process-durable foreground video export.
2. Landscape/tablet/foldable viewer composition; portrait remains intentionally locked until that layout is tested.
3. Project-ID-based annotation storage migration.
4. In-app quarantine recovery/export for corrupt local JSON.
5. String-resource localization and plural handling.
6. Freehand geometric simplification.
7. Instrumentation, screenshot, accessibility, benchmark, and memory suites.
8. Baseline profile and startup benchmark.
9. Signed release configuration, privacy policy, release checklist, and store assets.
10. Promote to `1.0.0-rc1` only after the complete gate.

## Copy/paste prompt for a new chat

Use this exact prompt when starting the next coding-agent chat:

> Continue the SwingFrame stabilization work on the existing Arena branch. Do not reset or discard the working tree. First read `docs/ENGINEERING_AUDIT_2026-07-17.md`, then `docs/NEXT_CHAT_HANDOFF.md`, `docs/DEVICE_SMOKE_CHECKLIST.md`, and `docs/TEST_MATRIX.md`. Inspect git status and the current diff. Ask me for the complete `swingframe-build-output.txt` from running `.\swingframe-build.ps1`. If the build failed, identify and fix the earliest root compiler/test/lint/resource error, add regression coverage, and tell me exactly what to rerun. If it succeeded, verify the success markers and SHA-256, then guide me through install, logcat collection, and the device smoke checklist before adding any new features.
