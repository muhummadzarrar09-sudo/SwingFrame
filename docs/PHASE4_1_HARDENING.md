# Phase 4.1 — Core correctness and modularization

Version: `0.5.1-hardening`

## Delivered

### Frame identity

- Added separate requested and resolved frame identity.
- Uncached seeks clear the resolved bitmap instead of retaining pixels from another frame.
- Annotation geometry is rendered only when requested and resolved frame IDs match.
- Drawing tools and action controls are disabled while exact pixels are unresolved.

### Media architecture

- Added `MediaSessionController`.
- Decoder, cache, request conflation, prefetch, thumbnails, playback, and export suspension moved out of the ViewModel.
- Added explicit `pauseForExport` / `resumeAfterExport` resource ownership.

### Annotation architecture

- Added `AnnotationSession`.
- Per-frame vectors, undo/redo, carry-forward, persistence debounce, and snapshots moved out of the ViewModel.

### Geometry

- Hit testing now measures in display pixels with the real image aspect ratio.
- Whole-shape movement computes one safe delta from complete bounds.
- Boundary movement no longer shortens lines or deforms shapes.
- Added aspect-distance and rigid-translation tests.

### Persistence safety

- Video metadata now records size, last-modified time, and a SHA-256 lightweight source fingerprint.
- Project schema advanced to version 2 with backward-compatible optional fingerprint fields.
- Source relinking compares fingerprints.
- Mismatches require an explicit high-risk confirmation.
- Project deletion requires confirmation and explains that original media is retained.

### Export

- Added pure `ExportTimelineMapper` with timestamp tests.
- Slow-motion output time maps explicitly back to source time.
- End-frame clipping uses an exclusive real-frame boundary.
- Preview codec resources are released during still/video export and restored afterward.
- Export setup can use the outermost bookmarks as range boundaries.

## Controller boundaries

```text
media/MediaSessionController.kt
annotation/AnnotationSession.kt
project/ProjectStore.kt + ProjectCompatibility.kt
export/ExportManager.kt + ExportTimelineMapper.kt
```

The ViewModel remains the screen-level coordinator. Project mutation and export status can be extracted further after the first compiler/device checkpoint if needed, but media and annotation lifecycle ownership are no longer embedded in it.

## Runtime items requiring the authorized test build

- Compose/Media3 compiler compatibility
- Real codec resource behavior
- Preview/export orientation equality
- Slow-motion overlay timing on device
- Gesture arbitration under two-finger input
- OEM MediaStore behavior
- 4K/HDR memory and color behavior
- Background process survival during long exports

Long export is still tied to the active application process. Foreground WorkManager/service promotion remains a release-hardening deliverable after basic export behavior is proven on the target device.

## Authorized checkpoint

The owner explicitly requested a Phase 4.1 test checkpoint. This build is a development validation build, not the final `1.0.0-rc1` gate. Compiler/runtime findings must be fixed before Phase 5.

## Test focus

1. Scrub to an uncached frame and verify old pixels disappear before the new exact frame resolves.
2. Confirm no annotations appear over an unresolved frame.
3. Drag shapes against all four edges and verify dimensions remain unchanged.
4. Re-link to the same file and to a different file; verify mismatch confirmation.
5. Delete a project and verify confirmation appears.
6. Export a still while preview prefetch is active; verify preview returns afterward.
7. Export 1×/0.5×/0.25× ranges with annotation timing checks.
8. Use Address/Finish marks as export boundaries.
