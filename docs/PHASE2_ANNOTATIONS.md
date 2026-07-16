# Phase 2 — Vector annotation engine

Version: `0.3.0-annotations`

## Included

### Tools

- Select/move
- Straight line
- Two-arm measured angle
- Vertical plumb
- Horizontal plumb
- Box
- Circle/ellipse
- Freehand

### Editing

- Selected-shape handles
- Endpoint/vertex/corner editing
- Whole-shape movement
- Six-color rotating palette
- 2/3/5/8 dp stroke widths
- Per-frame delete
- Clear current frame
- Overlay visibility toggle
- Carry-forward view across the nearest annotated frame within ±3 frames

### History

- Undo and redo are independent per frame.
- Gesture previews do not flood history; only the completed action is committed.
- Up to 50 snapshots are retained for each edited frame.

### Persistence

- Shapes are lightweight vectors in normalized source-video coordinates.
- Data is saved as app-private JSON under `files/annotation-projects`.
- Writes use Android `AtomicFile` so a failed write does not replace the previous valid file.
- Projects are keyed by a SHA-256 hash of the source URI.
- No network or storage permission is introduced.

## Interaction guide

### Line, box, circle, freehand

Select the tool and drag on the video. Release to commit.

### Angle

1. Drag the first arm from the intended vertex and release.
2. Drag the second arm; the original vertex remains fixed.
3. Release to commit. The measured interior angle appears in degrees.

### Plumb

Select vertical or horizontal plumb and tap/drag the desired axis position.

### Select/edit

1. Choose Select.
2. Tap a shape.
3. Drag the body to move it.
4. Drag amber handles to edit endpoints, vertices, or diagonal corners.
5. Use the bottom action strip for color, width, delete, undo, or redo.

### Zoom/pan

Use two fingers over the media stage. The header reset-view action restores the viewport; when overlays are hidden, double-tap also resets it. A second pointer cancels an in-progress drawing draft rather than committing accidental geometry.

## Carry-forward behavior

When Carry is enabled, the closest annotated neighboring frame within three frames is rendered beneath the current frame at reduced opacity. Current-frame annotations remain full opacity and editable; carried annotations are reference-only.

## Not included yet

- Shot trajectory layer
- Local AI
- Video/still export
- Side-by-side compare
- Full layer reorder/rename UI

## Build

```powershell
cd "D:\fun projects out of boredom\SwingFrame"
Set-ExecutionPolicy -Scope Process Bypass
.\swingframe-build.ps1
```

The output is `artifacts\SwingFrame.apk`.

## Device test checklist

1. Draw every tool on frame 1.
2. Scrub away and back; confirm geometry persists.
3. Relaunch the source and confirm app-private JSON reloads.
4. Draw a 90-degree angle and verify the label.
5. Select and move a line; edit both endpoints.
6. Undo and redo additions, moves, color changes, and deletes.
7. Clear a frame and undo the clear.
8. Enable Carry and step ±1–3 frames.
9. Hide/show the overlay.
10. Zoom, pan, and draw while checking source alignment.
11. Test portrait and landscape source videos.
12. Verify fast filmstrip scrubbing still behaves as in Phase 1.
