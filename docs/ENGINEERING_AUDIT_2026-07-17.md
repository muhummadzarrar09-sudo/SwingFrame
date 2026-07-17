# SwingFrame engineering audit — 2026-07-17

## Verdict

SwingFrame is a credible **0.5.x prototype/test candidate**, not a finished product. The repository has a thoughtful offline-first direction and useful core abstractions, but the README's feature-complete tone is ahead of the evidence. The app has not passed a compiler, emulator, physical-device, codec, export, accessibility, or release build gate in this checkout.

This audit covers the tracked application source, build configuration, tests, scripts, manifest, and phase/review documentation. Static review cannot replace the device matrix required by `TEST_MATRIX.md`.

## What is solid

- No network or broad storage permission; media comes from the system document picker.
- Real media sample timestamps are indexed rather than assuming constant FPS.
- Requested frame identity is separated from resolved bitmap identity.
- Decode requests are conflated and adjacent frames are prefetched.
- Annotation geometry is normalized, structured, editable, and persisted atomically.
- Project and bookmark data is local and atomically written.
- Export uses MediaStore pending items and temporary-file cleanup.
- Media and annotation lifecycles have been moved out of the ViewModel.
- Existing pure geometry, timeline, compatibility, and formatting tests are useful.

## Corrections made in this pass

1. Restored executable bits on `gradlew` and Linux helper scripts. The documented `./gradlew` and `./scripts/bootstrap-build.sh` commands could not run from a clean checkout.
2. Bounded frame-index list preallocation so malicious/broken frame-count metadata cannot request an enormous allocation before scanning.
3. Preserved duplicate presentation timestamps. `distinct()` previously deleted real samples and silently shifted frame numbers, bookmarks, and annotation identities.
4. Added a regression test for duplicate-PTS ordering.
5. Preferred microsecond `MediaFormat` duration over retriever's millisecond-rounded value and now reject sources with unreadable dimensions.
6. Recycled the original full-size decode after creating a scaled preview, avoiding two retained pixel buffers per cold 4K decode.
7. Made playback speed changes take effect while playback is already running.
8. Serialized all project-index I/O, coordinated deletion with debounced saves, persisted the index before deleting vectors, and report/roll back index-write failures.
9. Made measured angles aspect-correct in both preview and export; normalized 16:9 geometry was previously measured as though it were square. Added a regression test.
10. Raised undersized editor, transport, bookmark, project, and error controls to at least 48 dp to match the documented accessibility target.
11. Removed unused Media3 imports.
12. Rebuilt the viewer as a non-overlapping vertical workspace: header, weighted media stage, horizontal scrollable tool bar, contextual actions, and timeline now own separate layout regions instead of floating over one another.
13. Reduced header crowding with a proper overflow menu and replaced four always-visible speed chips with one 48 dp speed menu.
14. Made Home's empty state, Source Review, and the long export setup responsive/scrollable for short screens and larger font scales.
15. Added TalkBack timeline progress actions, selected-tool semantics, error live-region announcements, Android system-back navigation, and lifecycle playback pause.
16. Fixed Indexing cancel: returning to Source Review now actually cancels the indexing coroutine instead of allowing it to reopen the viewer later.
17. Fixed a conflated-channel race where prefetch could consume an older seek and overwrite a newer seek request.
18. Existing projects now revalidate their source fingerprint when reopened, not only when manually relinked.
19. Made relinking copy-before-commit with rollback, prevented relinking onto another project's URI, and protected shared URI-keyed annotation files during deletion.
20. Serialized annotation-store I/O and surfaced annotation save failures instead of silently pretending they were persisted.
21. Corrected density-independent minimum-shape validation; the old code compared pixels directly with a dp constant.
22. Bounded zoom panning so the frame cannot be dragged permanently off-screen.
23. Added empty-timeline export validation, stale temporary-export cleanup, and verified MediaStore publish updates.
24. Changed every supported build helper to run unit tests and `lintDebug` before assembling the APK.
25. Fatal VM errors are no longer misclassified as a hardware-codec failure and retried through software decode.
26. Persisted document grants are released when their final project owner is deleted or relinked.
27. Bitmap caches now respond to Android memory pressure and unregister their callback with the ViewModel lifecycle.
28. Project/annotation JSON versions are validated; corrupt files are quarantined and surfaced instead of silently becoming empty data.
29. Orphan annotation files are cleaned only after a successful project-index load, preserving quarantined recovery data.
30. Source fingerprints now combine metadata with a bounded 256 KiB source-byte sample when the provider permits reading.
31. Freehand paths now have denser sampling and a hard point cap, and undo history has a global edited-frame budget.
32. Added adaptive, round, and monochrome-capable launcher resources.

## Release blockers

### P0 — no verified build

A build was attempted. The checkout initially failed because the wrapper was not executable. After fixing that, the bootstrap could not download JDK 17 because this sandbox's outbound TLS connection to Adoptium failed. There is no system JDK installed, so compilation, unit tests, lint, R8, and APK assembly remain unverified here.

**Gate:** run `./gradlew testDebugUnitTest lintDebug assembleDebug` with JDK 17 and Android SDK 36, then fix every compiler/lint finding before feature work.

### P0 — physical-device media correctness is unknown

The exact-frame claim depends on OEM codecs, `FrameExtractor`, timestamp precision, rotation handling, VFR behavior, and duplicate-PTS behavior. The repository's own device matrix is empty.

**Gate:** execute `TEST_MATRIX.md` on the target phone and retain the results. Compare neighboring decoded frames against a trusted desktop extraction for CFR, VFR, high-FPS, rotated, HDR, and duplicate-PTS fixtures.

### P0 — export correctness is unverified

Still orientation, Transformer overlay coordinates, end-frame clipping, speed-effect timestamp order, source audio retention, HDR behavior, cancellation, and OEM MediaStore behavior are all device-dependent.

**Gate:** golden-image/video fixtures for 0/90/180/270-degree sources and on-device exports at 1×, 0.5×, and 0.25×.

## High-priority engineering debt

### P1 — long exports are not durable

Video export is owned by `viewModelScope`. Background process death loses the job. Move release exports into a foreground WorkManager worker or foreground service with persisted request/progress state.

### P1 — persistence recovery remains incomplete

Save failures are now surfaced and corrupt JSON is quarantined instead of becoming silent empty data. Remaining work is an in-app quarantine restore/export flow plus fault-injection tests proving disk-full, cancellation, corrupt-record, and process-death behavior.

### P1 — source fingerprint is sampled, not full-file

The versioned SHA-256 fingerprint now combines metadata with the first 256 KiB of source bytes when the provider permits reading. This is substantially safer than metadata-only identity without hashing a multi-gigabyte clip, but two deliberately crafted or unusually similar files could still collide at the product-identity level. Device validation must also confirm stable reads across document providers.

### P1 — exact-frame identity has a platform limit

Preview decoding converts microsecond PTS to milliseconds because of the Media3 API in use. Duplicate PTS samples also cannot be uniquely requested by timestamp alone. The index now preserves frame identity, but the UI must not promise pixel-distinct decoding for containers that do not provide unique seekable timestamps. Record this as source capability and explain it to users.

### P1 — UI state is too broad

`SwingFrameUiState` mixes heavyweight bitmaps, project data, editor state, export state, and navigation stage. `SwingFrameViewModel` remains 700+ lines. Before Compare/AI work, split render/media state, project state, editor state, and export state, and inject repositories/controllers behind interfaces for tests.

### P1 — no instrumentation, screenshot, benchmark, or migration suite

Six small JVM test files are not enough for a media editor. Add:

- Compose navigation/dialog/accessibility tests.
- Annotation-store and project-store round-trip/corruption/migration tests.
- Cancellation and stale-session race tests.
- Golden annotation renderer tests at multiple sizes and rotations.
- Macrobenchmark/baseline profile for import, first frame, scrub, and startup.
- Memory tests for repeated random 4K seeks and still export.

### P1 — release engineering is incomplete

Adaptive launcher assets now exist. CI could not be published because the GitHub App lacks workflow permission. Release signing/configuration guidance, resource localization, crash diagnostics export, explicit migration dispatch, privacy policy, store metadata, baseline profiles, and a reproducible release checklist remain incomplete. R8 is enabled but has not been exercised.

## Product/documentation mismatch

- `README.md` presents many capabilities as included, while `PHASES.md` and `REVIEW_PASS_AND_DELIVERABLES.md` correctly call export unverified and the app a test candidate.
- Compare, pose/tempo, and tracing are roadmap phases, not current product functionality.
- The promise of “exact frame” needs a documented qualification for timestamp collisions and platform decoder limitations.
- “Full-resolution” and HDR preservation require device proof and memory limits.

Until the gates pass, user-facing wording should say **development test candidate** rather than imply production readiness.

## Deep-pass finding inventory

The second pass found additional issues beyond the original review. Items fixed above are intentionally retained in this inventory so regressions can be tested.

### Lifecycle, concurrency, and state

- **Fixed:** Indexing cancellation did not cancel `analysisJob`; the supposedly cancelled scan could later force navigation into Viewer.
- **Fixed:** prefetch consumed and re-sent requests from a conflated channel, allowing an older request to overwrite a newer seek.
- **Fixed:** playback could continue when the app moved to the background.
- **Open:** long export remains bound to `viewModelScope` and is not process-durable.
- **Open:** UI state still combines bitmap ownership, navigation, projects, annotations, and export progress.
- **Open:** many broad `Throwable`/`runCatching` paths still make cancellation and fatal/nonfatal policy hard to audit.
- **Open:** there is no stale-session token protecting every asynchronous probe/index/decode callback; cancellation currently provides most of that protection but should be tested under rapid source switching.

### Persistence and project identity

- **Fixed:** project and annotation stores now serialize I/O instead of allowing concurrent `AtomicFile` writers.
- **Fixed:** destructive relinking no longer deletes old vectors before project metadata commits.
- **Fixed:** reopening a persisted project validates its source fingerprint.
- **Fixed:** deletion does not remove a URI-keyed annotation file still referenced by another project.
- **Fixed:** JSON versions are validated and malformed/future data is quarantined rather than interpreted as an empty project.
- **Fixed:** persisted URI grants are released after their final project reference is removed.
- **Fixed:** regular orphan annotation files are cleaned after—and only after—a successful project-index load.
- **Fixed:** undo history now has both per-frame and global edited-frame limits.
- **Open:** annotation storage is still keyed by URI rather than project ID, which creates hidden coupling that is now guarded but not eliminated.
- **Open:** quarantine is visible through an error, but there is no in-app browser/restore action for quarantined JSON.
- **Open:** annotation and last-frame flushes launched during teardown are best-effort and are not a process-death guarantee.
- **Open:** the recent-project list itself has no retention limit.

### Media correctness and performance

- **Fixed:** duplicate PTS samples were deleted by `distinct()`, shifting all later frame identities.
- **Fixed:** decoder preallocation trusted unbounded frame-count metadata.
- **Fixed:** scaled previews retained the original full-resolution bitmap.
- **Fixed:** playback speed changes did not affect an active playback loop.
- **Open:** Media3 preview requests are millisecond-addressed; microsecond PTS and duplicate PTS cannot always identify a unique decoded sample.
- **Open:** the complete timeline is held as boxed `List<Long>` values; very long/high-FPS clips need a documented duration/frame/memory limit or a compact primitive index.
- **Fixed:** the preview cache now trims/clears itself through Android memory-pressure callbacks.
- **Open:** thumbnail and frame bitmap disposal still relies mainly on GC after session release.
- **Open:** HDR transfer, color space, tone mapping, rotation, and mirrored-source behavior are not golden-tested.
- **Open:** playback is a silent decoded-frame slideshow, not source audio/video playback; product wording should make this explicit.
- **Open:** cold random seeks remain codec/GOP dependent and need measured timeout/retry UX.

### Annotation correctness and scalability

- **Fixed:** angle values were geometrically wrong on non-square video.
- **Fixed:** minimum shape size mixed dp constants with raw pixels.
- **Mitigated:** freehand drawing still copies immutable preview lists, but denser sampling and a 1,500-point cap bound the previous unbounded quadratic pressure.
- **Open:** persisted freehand paths do not yet run a geometric simplification algorithm such as Ramer–Douglas–Peucker.
- **Open:** ellipse hit distance is a radial approximation, not a true nearest-edge distance for highly eccentric ellipses.
- **Open:** carry-forward searches both past and future frames; that is documented, but the term “carry-forward” normally implies past-only and may surprise users.
- **Open:** Clear Frame has no confirmation or short-lived undo snackbar beyond annotation history controls.
- **Open:** there are no renderer golden tests proving preview/export parity at rotation and output-size boundaries.

### UI and accessibility

- **Fixed:** the 400+ dp vertical tool rail, floating action strip, 194 dp timeline offset, and fixed 113 dp header padding competed for the same media area.
- **Fixed:** five header actions left almost no title space on a 360–390 dp phone.
- **Fixed:** the export dialog and Source Review could overflow on short screens or large font scales.
- **Fixed:** timeline scrubbing had no adjustable accessibility action and system Back bypassed in-app navigation.
- **Open:** portrait orientation is forced even though landscape video analysis is a primary use case.
- **Open:** the app has no width-class/tablet/foldable layout and no landscape composition.
- **Open:** almost all user-visible text is hardcoded; there are no string resources or localization/plural rules.
- **Open:** several 10 sp uppercase labels and muted colors need automated contrast and large-font checks.
- **Open:** color and stroke controls cycle through hidden values rather than presenting an explicit palette/chooser.
- **Open:** keyboard, D-pad, switch-access, and external-mouse workflows are untested.
- **Open:** error cards are visual overlays rather than a consistent snackbar/banner event system and can leave underlying controls interactive.
- **Fixed:** adaptive, round, and monochrome-capable launcher resources are present.
- **Open:** Android 12+ splash branding is still only the platform default and has no dedicated launch theme.

### Export and release engineering

- **Fixed:** stale cache exports now receive bounded cleanup and MediaStore publication is checked.
- **Open:** export has no foreground worker/service, resume record, disk-space preflight, or persisted cancellation state.
- **Open:** 4K annotated still export can require two full-resolution bitmaps simultaneously and needs a memory budget/fallback.
- **Open:** clip boundaries are converted to milliseconds, potentially rounding source-frame boundaries.
- **Open:** preview/export orientation and slow-motion overlay timestamps remain device-unverified.
- **Open:** the GitHub connection used for this branch cannot publish workflow files, so CI still needs to be added with an account that has `workflows` permission.
- **Open:** there is no signed release configuration/checklist, privacy-policy package, baseline profile, or reproducibility record.
- **Open:** the build helpers now run lint, but they still produce a debug APK; that is correct for device testing and must not be mistaken for a store-ready release.

## Recommended order of work

1. Establish CI with JDK 17/SDK 36: unit tests, lint, debug build, release build, and artifact retention.
2. Run the existing device matrix and create a checked-in results template per phone/build.
3. Fix compiler/runtime findings; add regression tests for each one.
4. Make persistence errors recoverable and visible.
5. Move long exports to foreground durable work and validate orientation/timing golden fixtures.
6. Add instrumentation, screenshot, accessibility, benchmark, and memory suites.
7. Split the ViewModel/UI state before adding Compare or AI.
8. Complete release assets, migration policy, diagnostics, signing docs, and privacy/store checklists.
9. Only then promote to `1.0.0-rc1`; Compare/AI should not block a focused, reliable frame-analysis v1 unless they are truly required for the product definition.

## Honest completion definition

SwingFrame is finished when a clean checkout builds reproducibly, all automated checks pass, the target-device matrix has recorded evidence, project data survives failures and upgrades, exports survive backgrounding and match golden orientation/timing, accessibility checks pass, and a signed release candidate completes a real user smoke test without data loss or misleading frame identity.
