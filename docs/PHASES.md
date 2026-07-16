# SwingFrame delivery phases

Build policy: implementation phases first; first APK build only after the final source-hardening gate unless the owner explicitly authorizes a compiler checkpoint.

## Phase 1 — exact-frame media + editorial performance (complete)

- Native Android foundation
- Import and metadata
- Real timestamp index
- Hardware-first preview decode with fallback
- Filmstrip, prefetch, stepping, playback, zoom/pan
- Editorial amber UI

## Phase 2 — vector annotation engine (complete)

- Line, angle, vertical/horizontal plumb, box, ellipse, freehand
- Select/move/handles/style
- Per-frame undo/redo/delete/clear
- Carry-forward
- Atomic app-private vector persistence

## Phase 3 — local projects and bookmarks (complete)

- Recent local projects
- Last-frame resume
- Source relinking
- Address/Top/Impact/Finish/custom bookmarks
- Timeline ticks and bookmark jump/delete
- Atomic project index

## Phase 4 — still and video export (complete in source; unverified)

- Full-resolution clean/annotated PNG
- Frame-range MP4
- 1×/0.5×/0.25×
- Dynamic annotation overlay
- MediaStore output
- Progress/cancel/cleanup

## Phase 4.1 — core correctness and modularization (complete in source; test candidate)

- Requested/resolved frame identity
- Controller/repository split
- Aspect-correct rigid geometry
- Source fingerprint and safe relink
- Export lock, timestamp mapper, foreground lifecycle architecture

## Phase 5 — compare workspace (next after test checkpoint)

- Two-source/two-frame compare
- Side-by-side, split, overlay
- Independent/synchronized scrub
- Linked/unlinked transforms
- Saved compare state

## Phase 6 — local pose, tempo, and swing phases

- Local quality gate
- Pose landmarks
- Hand/head paths
- Tempo
- Address/Top/Impact/Finish suggestions
- P1–P10 candidate architecture
- Confidence/editable AI vectors

## Phase 7 — manual and local-assisted shot/club tracing

- Manual trajectory baseline
- Timestamp-based reveal
- Trace styles
- ROI and candidate tracks
- Local model interfaces/training workspace
- Editable AI proposal

## Phase 8 — final source hardening

- Close all P0/P1 findings
- Tests/benchmarks/accessibility
- Diagnostics/migrations/R8
- Memory and compatibility policy
- Promote to `1.0.0-rc1`

## First build and device validation

- Compile
- Unit tests
- Debug APK
- Install
- Codec/performance/export/gesture smoke matrix
- Fixes
- Release-candidate APK

See `REVIEW_PASS_AND_DELIVERABLES.md` for exact findings, deliverables, and exit criteria.
