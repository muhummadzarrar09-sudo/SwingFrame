# SwingFrame Review Pass and Remaining Deliverables

**Review date:** July 14, 2026  
**Reviewed version:** `0.5.0-export`  
**Review type:** Static source/product/architecture review only  
**Build status:** Intentionally not compiled or packaged yet

---

## 1. Executive verdict

SwingFrame is functionally implemented through the local export phase, but it is **not ready for the first APK build**.

The feature surface currently covers the original v1 functional requirements:

- Import and metadata
- Actual timestamp indexing
- Frame-addressable filmstrip and stepping
- Playback speed control
- Annotation tools
- Per-frame persistence
- Undo/redo/delete/clear
- Bookmarks
- Still export
- Range video export
- Error paths

However, static review found correctness, lifecycle, geometry, and architecture work that should be completed before adding local AI or performing the first build. The first build should test a deliberately hardened candidate, not act as the compiler-driven design process.

---

## 2. Review scope

Reviewed:

- Android manifest and permissions
- Gradle/dependency declarations
- Media import/index/decode/cache pipeline
- Viewer state and scrub scheduling
- Annotation model, geometry, gestures, persistence, and history
- Project index, bookmarks, resume, relinking, and deletion
- Still/video export and MediaStore behavior
- UI state and screen wiring
- Phase documentation and roadmap

Not reviewed because no build/device run is authorized yet:

- Compiler compatibility
- Runtime layout at actual device dimensions
- MediaCodec behavior
- Media3 Transformer behavior on the target phone
- Gesture arbitration under real multi-touch
- Memory/thermal performance
- Gallery/MediaStore behavior on the target OEM
- HDR/color fidelity

---

## 3. Current strengths

### Offline and privacy

- No `INTERNET` permission.
- No Retrofit, OkHttp, HTTP client, account, analytics, billing, or backend code.
- Platform diagnostics are disabled for Media3 export.
- Source access uses the system document picker and persisted grants.

### Media

- Actual sample presentation timestamps are indexed.
- Hardware decoding is attempted first with software-preferred fallback.
- Scrub requests are conflated instead of cancel-thrashing every decoder request.
- Directional adjacent-frame prefetch exists.
- Thumbnail work is independent from exact-frame decoding.
- Playback does not intentionally skip indexed frames to catch up.

### Annotation system

- Shapes use normalized source coordinates.
- Structured immutable shape types exist.
- Measured angles use vector math.
- App-private vector persistence uses atomic writes.
- Per-frame undo/redo and carry-forward exist.
- Export uses the same vector model rather than taking UI screenshots.

### Projects and export

- Recent projects use stable IDs.
- Last frame and bookmarks persist locally.
- Source relinking migrates annotation storage.
- MediaStore uses `IS_PENDING` and deletes partial outputs on failure.
- Temporary MP4 output is cleaned on completion/error/cancellation.

### Source hygiene

- One workspace root folder.
- No ZIP/APK/AAB artifacts.
- No TODO/FIXME/placeholder markers in app source.
- Versions are pinned.

---

## 4. Functional completion matrix

| Requirement | Static status | Remaining gate |
|---|---|---|
| Import via Android picker | Implemented | Device permission test |
| Metadata preview | Implemented | Codec matrix |
| Actual frame timestamps | Implemented | VFR source comparison |
| Slider equals frame count | Implemented | UI/device validation |
| Perceived scrub latency | Architecture improved | Benchmark required |
| Exact one-frame step | Implemented | Pixel-by-pixel test |
| Adjustable playback | Implemented | Cadence/audio test |
| Line/angle/plumb/box/circle/freehand | Implemented | Gesture test |
| Per-frame annotation persistence | Implemented | Relaunch/migration test |
| Undo/redo/delete/clear | Implemented | Stress/history test |
| Frame bookmarks | Implemented | Persistence/jump test |
| Clean/annotated still export | Implemented | Resolution/orientation test |
| Annotated range video export | Implemented | Timestamp/encoder test |
| Corrupt/unsupported errors | Partially implemented | Fault-injection test |
| Compare mode | Not started | Phase 5 |
| Local pose/tempo/P-system | Not started | Phase 6 |
| Manual/assisted shot trace | Not started | Phase 7 |

---

## 5. Findings and severity

### P0 — must resolve before first build candidate

#### R-01: Requested frame can temporarily display stale pixels with new annotations

On an uncached seek, `currentFrameIndex` and `currentFrameAnnotations` update immediately while `currentBitmap` retains the previously resolved frame until decoding finishes.

**Risk:** geometry from frame B may appear over pixels from frame A. This violates analysis correctness even if a resolving badge is visible.

**Required fix:** track requested and resolved frame indices separately. Never render current-frame annotations unless the bitmap belongs to the same resolved frame. Either retain old pixels with old annotations/readout or show an explicit unresolved stage.

#### R-02: No compiler pass has occurred

The project has intentionally not been compiled after Phases 1–4.

**Risk:** API signature/import/type errors may exist, especially across Compose and Media3 unstable APIs.

**Required fix:** this remains deferred until all implementation phases are complete, but the final build gate must begin with compilation before device installation. No phase may be called runtime-verified before then.

#### R-03: Slow-motion annotation timestamp mapping is logically implemented but unverified

The video overlay maps adjusted output timestamps back to source timestamps after `SpeedChangeEffect`.

**Risk:** Media3 effect timestamp ordering could differ from the inferred ordering on some paths, causing annotations to drift in 0.25×/0.5× exports.

**Required fix:** isolate timestamp mapping in a tested component and create golden source/output timestamp fixtures before first device export.

### P1 — must resolve before AI phases

#### R-04: `SwingFrameViewModel` is a 900+ line orchestration monolith

Media, annotations, persistence, projects, bookmarks, playback, and export are coordinated in one class.

**Risk:** AI/compare additions will make lifecycle and cancellation defects difficult to reason about.

**Required fix:** split into session controllers/repositories before Phase 5.

#### R-05: UI state mixes domain state with heavyweight `Bitmap` instances

`SwingFrameUiState` contains full frame bitmaps, thumbnail bitmaps, projects, annotation lists, export state, and all editor controls.

**Risk:** broad recomposition and accidental state copying; difficult unit testing.

**Required fix:** separate media render state, project state, annotation editor state, and export state.

#### R-06: Still export can open a second full-resolution decoder while preview decoding remains active

**Risk:** low/mid-range devices may exhaust codec instances or memory.

**Required fix:** add a media-session export lock: pause prefetch, serialize source-frame decode, then resume.

#### R-07: Long video export is tied to ViewModel lifecycle

Export is cancellable but not hosted in a foreground worker/service.

**Risk:** backgrounding the app or process pressure may terminate a long 4K export.

**Required fix:** move finalized video export to WorkManager foreground execution or an explicit local export service before calling export reliable.

#### R-08: Annotation hit testing is not aspect-correct

Distance calculations use normalized X/Y as if both dimensions have equal physical scale.

**Risk:** selection tolerance differs on portrait versus landscape clips.

**Required fix:** geometry hit testing must accept source/display aspect or operate entirely in pixel space.

#### R-09: Boundary movement distorts shapes

Each point is clamped independently during translation.

**Risk:** dragging a line/box against a boundary can shorten or deform it.

**Required fix:** clamp the translation delta against the whole shape bounds before applying one rigid translation.

#### R-10: Rotation/color alignment is not validated between preview and export

**Risk:** annotations may rotate/mirror or offset if FrameExtractor preview orientation and Transformer source orientation differ.

**Required fix:** centralize source-to-display/export transforms and add 0/90/180/270-degree golden cases.

#### R-11: Project identity depends on URI without source fingerprint validation

Relinking preserves geometry even if the selected replacement is a different clip.

**Risk:** valid annotations silently bind to wrong pixels/timing.

**Required fix:** store and compare duration, dimensions, frame count, file size/last modified where available, and a lightweight source fingerprint. Require explicit confirmation for mismatch.

#### R-12: Project deletion is immediately destructive

Deleting a recent project also deletes its annotation file without confirmation or undo.

**Required fix:** confirmation dialog with project name and clear scope; optionally retain annotation file until cleanup.

### P2 — quality improvements before polished release

- Color control currently cycles a palette instead of opening a visible palette.
- Box/ellipse editing exposes two diagonal handles rather than four corners/edges.
- Freehand editing only exposes endpoints.
- Carry-forward chooses one nearest annotated frame rather than per-layer pinning.
- Bookmark range presets are not integrated into export setup.
- Export completion does not offer Open/Share actions.
- Recent projects do not have persisted thumbnails.
- Error logs are user-readable but there is no local diagnostic bundle.
- Accessibility semantics for the custom filmstrip and annotation controls need dedicated coverage.
- Output codec/HDR/resolution policy is implicit rather than selectable and explained.

---

## 6. Mandatory review-remediation phase

# Phase 4.1 — Core correctness and modularization

This is the next implementation phase. No new headline feature is added.

### Deliverable 4.1-A — Frame identity correctness

- Add `requestedFrameIndex` and `resolvedFrameIndex`.
- Couple bitmap and annotations to resolved identity.
- Prevent stale-frame annotation overlays.
- Keep filmstrip playhead responsive without misrepresenting decoded pixels.

**Exit criteria:** every rendered bitmap can be proven to match its displayed frame/annotation identity.

### Deliverable 4.1-B — Controller split

Create:

```text
media/MediaSessionController.kt
annotation/AnnotationSession.kt
project/ProjectRepository.kt
export/ExportController.kt
```

The ViewModel becomes orchestration and screen-state composition only.

**Exit criteria:** ViewModel is substantially reduced; media, annotation, project, and export logic have isolated unit-testable APIs.

### Deliverable 4.1-C — Geometry hardening

- Pixel/aspect-correct hit testing.
- Rigid boundary-safe translation.
- Shared source/display/export transform object.
- Rotation fixtures.

**Exit criteria:** geometry unit tests cover portrait, landscape, and boundary movement.

### Deliverable 4.1-D — Persistence safety

- Project source fingerprint.
- Relink compatibility check.
- Schema migration hooks.
- Delete confirmation.

**Exit criteria:** a mismatched replacement source cannot silently inherit vectors.

### Deliverable 4.1-E — Export hardening architecture

- Export lock around decoder/prefetch resources.
- Pure tested timestamp mapper.
- Foreground export worker/service design.
- Bookmark-to-range presets.
- Explicit output policy model.

**Exit criteria:** export lifecycle is independent from the viewer and all partial outputs have deterministic cleanup.

---

## 7. Remaining product phases

# Phase 5 — Compare workspace

### Deliverables

- Two local sources or two frames from one source.
- Side-by-side mode.
- Adjustable split-screen mode.
- Opacity overlay mode.
- Independent and synchronized scrubbing.
- Per-side frame/time readouts.
- Per-side zoom/pan with optional linked transforms.
- Annotation visibility by side.
- Saved compare session state.

### Exit criteria

- Sources with different FPS/VFR remain independently frame-accurate.
- Sync uses normalized progress or user-defined anchor bookmarks, never assumed equal frame counts.

# Phase 6 — Local pose, tempo, and swing phases

### Deliverables

- Fully local model runtime packaged in the app.
- Capture-quality gate: blur, exposure, shake, subject size.
- On-device pose landmarks.
- Hand path.
- Head movement.
- Backswing/downswing boundary suggestions.
- Tempo durations and ratio.
- Address/Top/Impact/Finish suggestions.
- P1–P10 candidate architecture with confidence.
- AI vectors remain editable and visually marked as suggestions.

### Exit criteria

- No model or media request leaves the device.
- Manual analysis remains fully usable when inference fails.
- Confidence and failure reasons are visible.

# Phase 7 — Manual and local-assisted shot/club tracing

### Deliverables

- Manual impact/apex/landing/curve trajectory editor.
- Progressive timestamp-based trace reveal.
- Clean/Glow/Dots render styles.
- Club-head and ball candidate model interfaces.
- ROI confirmation.
- Temporal candidate association and confidence gaps.
- AI proposal converts to the same editable manual vector model.
- Offline training workspace under `SwingFrame/ml-training/`.
- Packaged optimized model under app assets only after validation.

### Exit criteria

- Every clip can be traced manually.
- AI failure never blocks export.
- Distance is labeled manual/estimated and never presented as launch-monitor truth without calibration.

# Phase 8 — Final source hardening and build readiness

### Deliverables

- Dependency/API audit.
- ProGuard/R8 rules for Media3 and model runtimes.
- Local diagnostic log export.
- Full unit-test inventory.
- Instrumentation and screenshot test definitions.
- Baseline profile/benchmark modules.
- Memory budgets and cache policy documentation.
- All destructive actions confirmed.
- Accessibility semantics.
- No TODO/FIXME/placeholder markers.
- Version/migration policy.

### Exit criteria

All static P0/P1 findings are closed and the source satisfies the final build gate below.

---

## 8. Final first-build gate

The first APK build is authorized only when:

1. Phases 4.1, 5, 6, 7, and 8 are marked complete in source.
2. No P0/P1 review finding remains open.
3. The manifest still has no network or broad-storage permission.
4. All runtime models are packaged locally.
5. The build script points to pinned Gradle/JDK/SDK versions.
6. Unit and instrumentation test sources are present.
7. Export cleanup and project migrations are defined.
8. The version is promoted from development phase naming to `1.0.0-rc1`.

The first build sequence will then be:

```text
compile → unit tests → debug APK → install → smoke matrix → fix → release-candidate APK
```

Building earlier would be allowed only if the owner explicitly changes this gate for a narrow compiler-only checkpoint.

---

## 9. Phase 4.1 remediation result

Closed in source:

- R-01 requested/resolved frame identity and annotation lockout
- R-03 pure slow-motion timestamp mapper plus tests
- R-06 preview/export decoder resource lock
- R-08 aspect-correct hit testing
- R-09 rigid boundary-safe movement
- R-11 source fingerprint and relink mismatch confirmation
- R-12 destructive project-delete confirmation
- Media lifecycle extracted to `MediaSessionController`
- Annotation lifecycle extracted to `AnnotationSession`

Still requires the authorized compiler/device checkpoint:

- R-02 compilation/runtime validation
- R-07 foreground survival for long exports
- R-10 preview/export orientation verification

## 10. Clear next action

**Run the explicitly authorized compiler/device checkpoint for `0.5.1-hardening`, fix all observed issues, then proceed to Phase 5 Compare.**
