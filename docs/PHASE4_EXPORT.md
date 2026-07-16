# Phase 4 — Local still and video export

Version: `0.5.0-export`

## Still export

- Exports the exact current source frame as PNG.
- Uses a dedicated full-resolution Media3 frame decode rather than the scaled preview bitmap.
- Supports clean source pixels or burned-in vector annotations.
- Carry-forward references can be rendered at reduced opacity.
- Saves through MediaStore to `Pictures/SwingFrame`.

## Video export

- Selects start and end frames from the real indexed timeline.
- Clips with source presentation timestamps.
- Supports 1×, 0.5×, and 0.25× output.
- 1× retains source audio when supported.
- Slow-motion exports are intentionally silent to avoid unsynchronized audio.
- Dynamic `CanvasOverlay` chooses the nearest indexed frame for every output timestamp.
- Per-frame annotations and optional carry-forward references are rendered at output resolution.
- Saves MP4 through MediaStore to `Movies/SwingFrame`.

## Rendering

- Preview and export geometry use the same normalized annotation model.
- Export stroke widths scale against output dimensions.
- Angles retain their degree labels.
- Plumb lines span the full output frame.
- Boxes, ellipses, freehand, lines, and angles are non-destructively composited.

## Reliability

- Transformer renders into an app-cache temporary MP4 first.
- Completed files are copied into MediaStore using `IS_PENDING`.
- Failed or cancelled exports delete temporary and partial MediaStore files.
- Progress is polled locally and shown as a percentage.
- Export can be cancelled.
- Media3 platform diagnostics are disabled.
- No storage permission or network permission is added.

## Known limits

- Slow-motion exports currently omit audio.
- Device encoder capability determines maximum practical resolution/HDR preservation.
- Full HDR color-validation remains part of the hardening phase.
- Output quality cannot be certified until the eventual device build/test pass.

## Eventual test plan

1. Export clean and annotated stills.
2. Verify source orientation and resolution.
3. Export a short 1× range with audio.
4. Export the same range at 0.5× and 0.25×.
5. Confirm each annotated frame appears at the correct output time.
6. Test carry-forward rendering.
7. Cancel midway and confirm no partial gallery item remains.
8. Test H.264/HEVC, portrait/landscape, VFR, 1080p, and 4K.
9. Verify outputs under Pictures/Movies SwingFrame albums.
10. Confirm the manifest still has no INTERNET or broad storage permission.
