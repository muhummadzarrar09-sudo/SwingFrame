# ShaShot Research → SwingFrame Product Direction

**Research date:** July 14, 2026  
**Purpose:** Competitive and technical research before changing SwingFrame  
**Status:** Research only — no implementation changes are authorized by this document

---

## Executive verdict

Yes: ShaShot is a useful reference for SwingFrame, especially its media-first editor, contextual bottom controls, filmstrip, trajectory styling, and distinction between shot, club, hand, body, tempo, and swing-phase analysis.

No: SwingFrame should not become a direct clone. ShaShot is primarily a social golf-video effects and AI-tracing product with accounts, ads, subscriptions, cloud processing, score overlays, and sharing. SwingFrame's strongest position remains a private, offline analysis instrument.

The recommended product expression is:

> **SwingFrame is an editorial golf-analysis studio: exact frames, precise geometry, assisted trajectories, and no cloud dependency.**

The design should borrow ShaShot's editor grammar while becoming quieter, more legible, more reliable, and more analysis-oriented:

- Media is the dominant surface.
- Controls appear only when relevant.
- A filmstrip and exact playhead make time tangible.
- AI creates an editable proposal, never an irreversible result.
- Original video timing and quality are preserved.
- Every automatic result includes confidence and manual correction.
- No account, ads, points, subscription, community, or network requirement.

---

## 0. Research method and confidence labels

### Sources inspected

1. UnderparLab's official product website and trace workflow.[1](https://www.underparlab.com/)
2. Current Google Play listing, screenshots, data-safety declaration, reviews, and developer responses.[5](https://play.google.com/store/apps/details?id=com.underparlab.shashot&hl=en_US)
3. Current Apple App Store listing, screenshots, version history, reviews, app size, and purchases.[4](https://apps.apple.com/us/app/shashot-ai-golf-shot-tracker/id1595050744)
4. UnderparLab's published privacy policy, including video collection and AWS processing/storage.[6](https://underparlab.notion.site/Privacy-terms-b6758a65b6e74f89949483b642c01a36)
5. A secondary screenshot/feature catalog used only to cross-check UI labels.[3](https://mwm.ai/apps/shashot-ai-golf-shot-tracer/1595050744)
6. Historical Google Play copy exposing older feature behavior and product scope.[1](https://play.google.com/store/apps/details?id=com.underparlab.shashot&gl=KR)

### Confidence labels

- **Verified:** stated by the developer, listed in official release notes, or directly visible in official screenshots.
- **Observed:** visible in screenshots but behavior cannot be fully tested from the image alone.
- **Inferred:** technically likely from visible behavior, privacy terms, reviews, and normal video-processing architecture; not confirmed source code.
- **Recommended:** original direction proposed for SwingFrame.

### Important limit

ShaShot's source code and proprietary model are not public. No claim in this report about its exact framework, neural-network architecture, model weights, database schema, or internal API should be treated as fact. We can reconstruct product behavior and likely system boundaries, not proprietary implementation.

---

# 1. Product model

## 1.1 What ShaShot actually is

ShaShot currently spans two jobs:

1. **Golf content creation**
   - Add a TV-style ball-flight tracer.
   - Add club, hand, body, and ghost/print effects.
   - Add filters, distance labels, scoreboards, watermarks, color, shadow, and stylized paths.
   - Export and share a polished video or reel.

2. **Swing analysis**
   - Inspect club-head trajectory.
   - Measure backswing and downswing tempo.
   - Visualize pose/body movement.
   - Review P1–P10 swing phases.
   - Compare swings.
   - View 3D/motion representations.

The current official listing describes automatic ball tracing, one-tap sharing, club-head trace, tempo, club path/3D visualization, body trace, and P-system analysis.[4](https://apps.apple.com/us/app/shashot-ai-golf-shot-tracker/id1595050744)

## 1.2 Current market state

As of July 2026:

- Google Play shows 100K+ downloads, ads, in-app purchases, and an approximately 2.6/5 rating.[5](https://play.google.com/store/apps/details?id=com.underparlab.shashot&hl=en_US)
- The App Store version history shows version 1.0.59, with Real-Time Tracer introduced in version 1.0.53 and prior releases adding 4K trajectory encoding, swing comparison, 3D motion, score bars, putting lines, tempo, and trajectory controls.[4](https://apps.apple.com/us/app/shashot-ai-golf-shot-tracker/id1595050744)
- The App Store lists a roughly 223 MB iOS package, subscriptions, consumable "GolfBall" credits, and advertising-related data collection.[4](https://apps.apple.com/us/app/shashot-ai-golf-shot-tracker/id1595050744)

This matters because the feature breadth is attractive, but the product has accumulated monetization, account, server, reliability, and discoverability costs that SwingFrame does not need.

## 1.3 Likely top-level user flow

**Verified/observed reconstruction:**

1. Record or import video.
2. Choose a mode: Shot Trace, Head/Club Trace, Tempo, Swing Analysis, Swing Print, Hand Path, etc.
3. Trim or identify the relevant swing window.
4. Wait for automatic analysis.
5. Enter a mode-specific editor.
6. Step through frames and refine the result.
7. Style the path/effect.
8. Add optional scoreboard/watermark/distance.
9. Export/share.

This mode-first flow keeps individual editors focused, but it can fragment the project: features like scoreboards have historically been unavailable in some modes, which users complained about.[5](https://play.google.com/store/apps/details?id=com.underparlab.shashot&hl=en_US)

**SwingFrame recommendation:** use one project and one layer stack. "Shot Trace," "Swing Geometry," "Pose," and "Score Overlay" should be layer types inside the same editor, not separate incompatible workflows.

---

# 2. Complete feature inventory

The table distinguishes current/verified features from historical or secondary features.

| Area | Feature | Status | SwingFrame decision |
|---|---|---:|---|
| Capture | Record with phone camera | Verified | Later; gallery import remains first |
| Import | Use existing landscape/portrait video | Verified | Keep |
| Shot | Automatic ball-flight tracing | Verified | Add only after manual tracer |
| Shot | Real-time tracer during recording | Verified, added 2026 | Defer; very high risk |
| Shot | Manual trajectory correction | Verified | Essential |
| Shot | Landing-point control | Observed | Add |
| Shot | Highest-height/apex control | Observed | Add |
| Shot | Curve control | Observed | Add |
| Shot | Depth/perspective adjustment | Release notes | Add as visual style, not fake physics |
| Shot | Target point | Release notes/screenshots | Optional |
| Shot | Distance/flying-distance overlay | Observed | Manual label unless calibrated |
| Shot | Thickness control | Release notes/screenshots | Add |
| Shot | Opacity | Release notes | Add |
| Shot | Shadow | Verified | Add later |
| Shot | Color and gradients | Verified | Add |
| Shot | Path filters: Normal/Fancy/Shiny/Follow/Laser/etc. | Observed | Start with Clean/Glow/Dots only |
| Shot | Bubble/comet-style filters | Historical release notes | Not analysis-critical |
| Club | Automatic club-head trace | Verified | Add after pose/impact foundation |
| Club | Club path visualization | Verified | Add |
| Club | Club trace filters: comet/laser/etc. | Observed | Keep one clean analysis style first |
| Club | 3D plane view | Verified | Later; label assumptions clearly |
| Hands | Hand-path trace | Verified | Strong Phase 3 candidate |
| Body | Body outline/trace | Verified | Add through pose landmarks |
| Swing Print | Head Print | Observed | Optional visual analysis layer |
| Swing Print | Head Ghost | Observed | Useful for head movement |
| Swing Print | Club Print | Observed | Similar to accumulated club path |
| Swing Print | Club Follow | Observed | Similar to progressive trail |
| Swing Print | Lightshaft | Observed | Cosmetic; low priority |
| Analysis | P1–P10 phase review | Verified | Add after reliable phase detection |
| Analysis | Swing-plane lines | Verified | Already compatible with SwingFrame annotations |
| Analysis | Tempo analysis | Verified | Add; high value, relatively tractable |
| Analysis | Backswing/downswing ratio | Verified | Add |
| Practice | Tempo sound/practice mode | Verified | Optional personal training tool |
| Analysis | 3D swing trajectory/motion | Verified | Long-term; monocular limits apply |
| Compare | User-vs-user/pro or session comparison | Release notes | Fits existing roadmap Phase 2 |
| Putting | Putting-line tracking | Release notes | Separate future feature |
| Timeline | Thumbnail filmstrip | Observed | Add |
| Timeline | Vertical playhead | Observed | Add |
| Timeline | Timestamp capsule | Observed | Add exact timestamp + frame index |
| Timeline | Previous/next frame controls | Observed | Already required |
| Editing | Contextual horizontal tool rail | Observed | Add |
| Editing | Tool-specific bottom sheets | Observed | Add |
| Editing | Confirm checkmark / close | Observed | Use Apply/Cancel transaction model |
| Overlay | Score bar/scoreboard | Verified | Optional export layer |
| Overlay | Club labels | Release notes | Optional |
| Overlay | Watermark | Observed | No watermark by default |
| Video | Slow-motion effects | Historical | Playback already required; export later |
| Video | Impact sounds | Historical | Reject for analysis core |
| Video | Video/color filters | Verified | Minimal correction only, not filter library |
| Export | Video export | Verified | Add |
| Export | Up to 4K trajectory encoding | Release notes | Eventually; 1080p reliable first |
| Export | Social/reel formats | Verified | Optional presets |
| Sharing | One-tap social sharing | Verified | System share sheet only |
| Library | Save to golf notes/session history | Website | Add local sessions later |
| Social | Community, messages, anonymous questions | Historical | Reject |
| Account | Login/profile | Verified by reviews/privacy | Reject |
| Monetization | Ads/subscriptions/credits | Verified | Reject |

---

# 3. Components

## 3.1 ShaShot component inventory

### App shell

- Near-black full-screen editor background.
- Compact top row with mode/editor title.
- Close icon at the upper-right.
- Export action in analysis modes.
- Minimal navigation chrome during editing.

### Media stage

- Large portrait media viewport with strongly rounded corners.
- Trace drawn directly over the frame.
- Draggable circular control handles on trajectory points.
- Optional floating canvas controls, such as pan and layer visibility.
- Video remains the visual focus; most editor chrome sits below it.

### Timeline

- Horizontal thumbnail filmstrip.
- Thin, high-contrast vertical playhead.
- Timestamp pill attached to the playhead.
- Explicit Previous Frame / Next Frame labels.
- Likely tap and drag scrubbing.

### Tool rail

- Horizontally scrolling row of icon-plus-label tools.
- Selected item uses a bright mint/green accent.
- Disabled or premium items become low-contrast or carry a crown badge.
- A right chevron indicates additional tools beyond the viewport.

### Property panels

- Contextual bottom panel rather than a permanent dashboard.
- Color palette shown as circular swatches.
- Filter picker shown as thumbnail circles/cards.
- Selection grids for P1–P10 and Swing Print variants.
- Confirmation checkmark at panel edge.
- Large cards for mode/filter choices in older screens.

### Analysis overlays

- Tempo card with two durations and a ratio.
- Body silhouette/outline.
- Swing-plane lines.
- P-position cards.
- Score bar with player/course/hole/par/shot/club fields.
- Distance and target labels.

## 3.2 Components SwingFrame should build

1. **Editorial Masthead**
   - Small uppercase product label.
   - Large clip/session title.
   - Secondary metadata line.
   - Close/back and export actions.

2. **Media Stage**
   - Edge-to-edge image/video.
   - Optional 16–24 dp corner radius in portrait.
   - Zoom/pan transform shared by all vector layers.
   - Layer visibility and reset-view controls.

3. **Exact Filmstrip**
   - Thumbnail samples that do not pretend every thumbnail is every frame.
   - Exact playhead mapped to the real timestamp index.
   - Frame number and timestamp capsule.
   - Bookmark/P-position ticks.

4. **Transport Bar**
   - Previous frame, play/pause, next frame.
   - Speed selector.
   - Optional loop-range toggle.

5. **Context Tool Rail**
   - Draw, Measure, Trace, Pose, Layers.
   - Icon plus short label; no mystery icon-only tools for primary actions.

6. **Property Sheet**
   - Appears after a layer or tool is selected.
   - Color, thickness, opacity, behavior, keyframes.
   - Apply/cancel semantics for destructive or multi-step edits.

7. **Layer Stack**
   - Annotation layer.
   - Shot trajectory layer.
   - Club/hand track layer.
   - Pose/body layer.
   - Score/text overlay.
   - Each independently visible, lockable, reorderable, and exportable.

8. **AI Review Panel**
   - Shows confidence and warnings.
   - Offers Accept, Refine, or Redetect.
   - Never hides the source frame or forces acceptance.

---

# 4. Design analysis

## 4.1 ShaShot's visual language

The current editor is simpler than its promotional materials:

- **Base:** near-black background.
- **Primary text:** white.
- **Action accent:** saturated mint/emerald.
- **Historic/marketing accent:** violet-to-blue gradient.
- **Trajectory color:** red, purple, white, gradient, or user-selected.
- **Shape language:** large corner radii, circular swatches, rounded rectangular panels.
- **Typography:** bold geometric/grotesk sans with plain supporting labels.
- **Hierarchy:** large media, small title, low-contrast editor labels, one bright selected action.

Promotional screens are louder—large all-caps branding, gradients, glow, and cinematic photography. The actual editor is more restrained and is the better reference.

## 4.2 What "minimalistic and editorial" should mean for SwingFrame

SwingFrame should not use "minimal" to mean missing labels or hidden functionality. It should mean disciplined hierarchy.

### Recommended palette

| Token | Value | Use |
|---|---:|---|
| Canvas | `#080A09` | Full-screen editor background |
| Ink | `#F3F4EF` | Primary text |
| Paper | `#E9E7DE` | Optional light editorial cards/export sheets |
| Surface 1 | `#111512` | Tool rails and sheets |
| Surface 2 | `#1A201C` | Selected/elevated surfaces |
| Rule | `#2A312C` | Borders/dividers |
| Signal Mint | `#27E0A3` | Active tool, playhead, confirmation |
| Intelligence Violet | `#7A62FF` | AI suggestions only |
| Measure Amber | `#F3B94E` | Angle/measurement labels |
| Destructive | `#FF7067` | Delete/errors |

Use mint for interaction and violet only for AI-generated content. This makes provenance visible: manual versus suggested.

### Typography

- Use one variable grotesk family where possible; Android system sans is acceptable initially.
- Large editorial titles: 30–40 sp, medium/semibold, tight tracking.
- Numeric readouts: tabular numerals.
- Section labels: 10–12 sp uppercase with increased tracking.
- Tool labels: 11–13 sp; never below accessible readability.
- Avoid excessive all-caps in operational controls.

### Layout rules

- 20–24 dp page margin.
- 8 dp base spacing unit.
- 12–16 dp compact control radius.
- 20–28 dp media/panel radius.
- 48 dp minimum touch targets.
- One primary accent per screen.
- No ornamental glass blur unless it improves media contrast.
- Avoid permanent borders around every object; use spacing first.

## 4.3 Editorial screen structure

### Home / Session Index

- Masthead: `SWINGFRAME / LOCAL ANALYSIS`.
- Large statement: `Your swing, frame by frame.`
- One confident Import action.
- Recent sessions as full-width editorial rows: thumbnail, date, clip name, duration, frame count.
- No bento-grid overload.

### Mode chooser after import

- **Frame Lab** — exact stepping and manual geometry.
- **Shot Trace** — ball-flight trajectory.
- **Swing Motion** — pose, club/hand path, tempo.

These are views of one project, not separate data silos.

### Editor

- Top: compact title and export.
- Middle: media stage.
- Bottom: exact filmstrip and transport.
- Lowest contextual area: tool rail or selected-layer properties, never both at full height simultaneously.

---

# 5. Elements, interactions, animation, and logic

## 5.1 Scrubber and filmstrip

### Logic

- Slider position maps to `frameIndex`, not assumed FPS.
- `frameIndex` maps to actual presentation timestamp.
- Previous/next always change exactly one indexed frame.
- Filmstrip thumbnails are sampled for orientation, while the playhead addresses every frame.
- Scrubbing requests are coalesced; only the latest request commits to UI.
- Adjacent frames are prefetched after the active frame is shown.

### Interaction

- Touch down enlarges thumb from 18 dp to 24 dp.
- Dragging shows a floating capsule with `F 087` and `00:02.900`.
- Haptic tick on each frame at slow drag; rate-limit haptics during fast drag.
- Bookmark/P-phase markers snap only when the user slows near them.
- Long press opens a magnified frame preview or bookmark action.

### Motion

- Thumb scale: spring, approximately 180–220 ms perceived settling.
- Timestamp capsule: 100–140 ms fade/scale.
- Do **not** crossfade between video frames; crossfades destroy frame-analysis clarity.
- Tool panel may animate, but frame replacement must be immediate.

## 5.2 Tool selection

- Selected tool gets mint fill or mint underline—not three simultaneous effects.
- Icon can use a subtle 0.96 → 1.04 → 1.0 spring.
- Property sheet enters from bottom in 220–280 ms.
- Back first exits the active gesture/tool, then closes the sheet, then exits the editor.

## 5.3 Drawing logic

Use an explicit state machine:

`Idle → Creating → Previewing → Committed → Selected → EditingHandle/Moving → Committed`

Every committed transition becomes an undoable command. Pointer moves during a drag update a temporary preview and do not flood the undo stack.

## 5.4 Trajectory editing

A shot trajectory should expose:

- Impact/start point.
- Apex point.
- Landing/exit point.
- Curvature/bend control.
- Start and end times.
- Reveal speed/easing.
- Stroke width, taper, color/gradient, opacity, glow/shadow.

Handles should be visible only while the layer is selected. A two-finger gesture manipulates the viewport; one finger manipulates the selected handle.

## 5.5 Trace animation

- Progressive reveal tied to video timestamp, not a generic fixed-duration animation.
- Optional after-image/follow trail decays over a configurable frame window.
- Glow rendered as a secondary wider low-alpha stroke.
- Dot/bubble mode samples the same path at timestamp-derived positions.
- Motion continues correctly under variable frame rate.

## 5.6 Loading and AI states

Avoid a vague spinner ending at 99%.

Use named stages:

1. Preparing video.
2. Checking capture quality.
3. Finding golfer and ball.
4. Tracking launch.
5. Fitting trajectory.
6. Preparing editable result.

If a stage fails, preserve completed work and move directly to manual setup.

---

# 6. AI feature analysis

## 6.1 ShaShot AI features

### Ball-flight trace

The official site says its vision pipeline analyzes video frame-by-frame and traces ball, club-head, and hand paths; it also states the trajectory can be tweaked by gesture.[1](https://www.underparlab.com/)

### Club-head tracking

Tracks the club head over the swing and renders a path/effect. Multiple visual filters and a 3D-plane representation are offered.

### Hand-path tracking

Tracks hands/grip movement and creates a path overlay.

### Body trace / pose

Produces a body outline or pose representation for posture review.

### P-system phase analysis

Maps the swing into P1–P10 key positions. This likely combines pose/club features with a temporal phase classifier, but the exact model is not public.

### Tempo analysis

Finds backswing and downswing boundaries, calculates durations, and displays a ratio such as 3:1. Tempo practice adds timed audio cues.

### Swing Print

Creates accumulated or ghosted motion effects: Head Print, Head Ghost, Club Print, Club Follow.

### 3D motion/swing trajectory

Offers a rotatable or multi-angle representation. With monocular phone video this should be understood as an estimated visualization, not guaranteed metric 3D launch-monitor data.

### Real-time tracing

Release notes introduced Real-Time Tracer in May 2026.[1](https://apps.apple.com/us/app/shashot-ai-golf-shot-tracker/id1595050744)

## 6.2 AI features recommended for SwingFrame

### Tier 1 — deterministic intelligence

- Capture-quality checks: brightness, blur, shake, subject size, ball visibility.
- Auto-trim suggestion around detected motion/impact.
- Tempo boundary suggestions.
- Pose landmarks using an on-device model.

These offer value even before custom model training.

### Tier 2 — assisted analysis

- Address/top/impact/finish suggestions.
- P1–P10 candidate frames with confidence.
- Spine/shoulder/hip line suggestions.
- Head-movement box generated from pose/head landmarks.
- Hand path from wrist landmarks.

### Tier 3 — difficult object tracking

- Club-head path.
- Ball-flight candidate tracking.
- Impact point and launch direction.

These require specialized small-object and temporal models plus strong manual fallback.

### Tier 4 — research/stretch

- Real-time shot tracing.
- Monocular 3D swing reconstruction.
- Coaching recommendations.

Do not claim coaching correctness without a validation set and domain review.

## 6.3 AI UX principle

AI output must be a vector layer with provenance:

- Purple = suggested/unreviewed.
- Mint = user-confirmed.
- Confidence shown per segment, not only one overall percentage.
- Low-confidence gaps are dashed.
- User can add, move, or delete keyframes.
- A failed model opens manual mode with detected impact/ROI retained.

---

# 7. How ShaShot likely performs shot tracing

## 7.1 What is verified

- The developer says the vision pipeline analyzes frames and tracks the ball frame-by-frame.[1](https://www.underparlab.com/)
- Historical official copy mentions AI ball-position and impact-position detection.[1](https://play.google.com/store/apps/details?id=com.underparlab.shashot&gl=KR)
- Current editing UI exposes manual landing point, apex/highest height, curve, depth, opacity, thickness, target, distance, color, shadow, and filters.[4](https://apps.apple.com/us/app/shashot-ai-golf-shot-tracker/id1595050744)
- The developer recommends non-slow-motion 1080p at 30–60 FPS for ball tracing; slow-motion is recommended for head trace/Swing Print instead.[1](https://play.google.com/store/apps/details?id=com.underparlab.shashot&hl=en_GB&gl=US)
- User reports and developer responses mention server congestion and processing stuck near completion, indicating at least part of the normal pipeline is server-backed.[2](https://play.google.com/store/apps/details?id=com.underparlab.shashot&amp%3Bhl=en)

## 7.2 Most plausible pipeline — inferred, not proprietary fact

1. **Ingest and trim**
   - Decode video and normalize orientation.
   - Restrict duration to a short window.
   - Possibly transcode to a standard analysis resolution/frame rate.

2. **Scene/subject localization**
   - Detect golfer and ball address region.
   - Estimate camera angle and usable sky/fairway region.

3. **Impact detection**
   - Use club/pose motion, audio transient, or rapid ball-region change to estimate impact frame.

4. **Small-object candidate detection**
   - Detect bright/moving ball candidates in a region of interest.
   - Use multi-scale crops because the ball may occupy only a few pixels.

5. **Temporal association**
   - Link candidates frame-to-frame using motion direction, acceleration bounds, appearance, and confidence.
   - Reject background highlights, birds, clouds, and compression artifacts.

6. **Trajectory fit**
   - Smooth sparse/noisy points.
   - Fill occluded or invisible portions.
   - Fit a screen-space ballistic/parabolic or cubic Bézier path.

7. **Manual correction**
   - Expose start, apex, end, curve, and visual depth.
   - User corrects a plausible path when raw tracking is incomplete.

8. **Styling and temporal reveal**
   - Render a clean parametric curve, not raw jittering detections.
   - Reveal it over time with taper, gradient, glow, shadow, follow, dots, or other filters.

9. **Video compositing/export**
   - Composite trajectory, labels, scoreboard, and watermark.
   - Encode to the selected format/quality.

This hybrid model explains both the "automatic" result and the extensive manual controls. It is also the right product pattern: reliable manual geometry with AI assistance is more useful than a black box that fails without recovery.

## 7.3 Recommended SwingFrame tracing pipeline

### Stage A: reliable manual tracer first

- User selects impact frame and start point.
- User selects apex frame/point.
- User selects landing or exit frame/point.
- Optional curvature control point.
- Fit a cubic Bézier or constrained ballistic screen-space curve.
- Preview progressive reveal.
- Save as editable vector data.

This creates value immediately and supplies correction data for later AI.

### Stage B: assisted detection

1. Quality gate.
2. User confirms a ball/impact ROI.
3. Temporal differencing generates candidates.
4. Tiny-object model scores candidates.
5. Optical flow/Kalman prediction links frames.
6. RANSAC or constrained curve fit rejects outliers.
7. Confidence-coded editable keyframes are returned.

### Stage C: camera motion

- Estimate global camera motion from background features.
- Stabilize detections in a virtual reference frame.
- Render back into source-frame coordinates.
- Moving-camera support should not ship until stationary-camera tracking is solid.

### Stage D: truthful distance

Do not infer yards from a 2D curve without calibration. Distance can be:

- User-entered.
- Imported from launch-monitor/range data.
- Estimated only after camera calibration and known scale, with an explicit `Estimated` label.

---

# 8. Architecture

## 8.1 ShaShot architecture signals

### Verified

- Accounts, purchases, ads, data deletion, and network-dependent login exist.[4](https://apps.apple.com/us/app/shashot-ai-golf-shot-tracker/id1595050744)
- Google Play says personal information, messages, photos/videos, and other data may be collected or shared.[5](https://play.google.com/store/apps/details?id=com.underparlab.shashot&hl=en_US)
- The privacy policy states generated videos and usage data may be collected and names AWS for data processing/storage.[6](https://underparlab.notion.site/Privacy-terms-b6758a65b6e74f89949483b642c01a36)
- Developer responses mention server congestion affecting processing.[2](https://play.google.com/store/apps/details?id=com.underparlab.shashot&amp%3Bhl=en)
- Release notes mention HDR and 4K encoding fixes, indicating substantial client-side media processing even if inference is remote.[4](https://apps.apple.com/us/app/shashot-ai-golf-shot-tracker/id1595050744)

### Inferred hybrid system

- Mobile capture/import and editor.
- Local thumbnailing, timeline, vector adjustment, and preview compositing.
- Upload or API job for at least some tracing operations.
- Cloud processing/storage through AWS.
- Local or cloud inference depending on feature/device.
- Local video encoding or a mixed export path.
- Backend for accounts, subscriptions/credits, community/messages, job status, and saved results.

The exact language, framework, model runtime, and service layout remain unknown.

## 8.2 Recommended SwingFrame modular architecture

```text
:app
:core:design-system
:core:model
:core:media
:core:geometry
:core:persistence
:feature:library
:feature:import
:feature:viewer
:feature:annotations
:feature:trace
:feature:pose
:feature:compare
:feature:export
:ml:quality
:ml:pose
:ml:ball
:native:vision          (optional OpenCV/NDK later)
:benchmark
```

### Responsibilities

- **design-system:** color, type, motion, icons, sheets, tool rail, scrubber.
- **model:** immutable project/layer/track data.
- **media:** metadata, frame index, Media3 decoder, cache, playback.
- **geometry:** normalized coordinates, Bézier fit, angle math, transforms.
- **persistence:** Room metadata plus JSON/protobuf vector payloads.
- **viewer:** stage, timeline, zoom/pan, transport.
- **annotations:** shape tools, selection, undo/redo.
- **trace:** manual and assisted trajectory editor/renderer.
- **pose:** landmarks, phases, tempo, body/hand path.
- **export:** still/video rendering and MediaStore output.
- **ML modules:** optional and independently replaceable.

## 8.3 Core data model

```text
AnalysisProject
  id
  sourceUri
  sourceFingerprint
  metadata
  frameTimeline
  layers[]
  bookmarks[]
  viewportState
  exportPresets[]

AnalysisLayer
  id
  type
  name
  visible
  locked
  zIndex
  source = MANUAL | AI_SUGGESTED | AI_CONFIRMED
  payload

ShotTraceLayer
  startFrame
  endFrame
  keyframes[]
  fittedCurve
  revealProfile
  renderStyle
  distanceLabel?
  confidenceSegments[]

TrackKeyframe
  frameIndex
  timestampUs
  normalizedPoint(x, y)
  confidence
  userConfirmed

PoseFrame
  frameIndex
  landmarks[]
  phaseCandidate
  phaseConfidence
```

All geometry should be stored in normalized source-video coordinates, never viewport pixels.

## 8.4 Threading and performance

- Main thread: Compose state and gestures only.
- Media dispatcher: Media3 frame requests, serialized as required.
- Vision dispatcher: CPU/GPU/NNAPI inference.
- Geometry dispatcher: track fitting and smoothing.
- Export: foreground WorkManager job with cancellation and progress.
- Keep only a bounded window of display-sized frames in memory.
- Keep source-quality decoding separate from preview-quality decoding.
- Coalesce scrub requests and discard stale results by generation token.

## 8.5 Rendering

- Compose Canvas for interactive vector preview.
- A common renderer interface shared by preview and export.
- Export renderer operates at source/output resolution using normalized geometry.
- Avoid taking screenshots of the Compose UI for export.
- Use Media3 effects/Transformer where practical; use an offscreen GL/Canvas path for complex overlays.

---

# 9. Engineering and product practices

## 9.1 Manual-first, AI-assisted

Every AI feature must have a complete manual path. This is the main lesson from ShaShot's review history: automatic detection is condition-dependent, and users become frustrated when failure means restarting or repeatedly trimming.

## 9.2 Quality gate before analysis

Before expensive tracing, report:

- Subject too small.
- Ball region not visible.
- Motion blur too high.
- Exposure too dark/bright.
- Camera shake too high.
- Clip too long.
- Unsupported slow-motion mode for ball tracing.

Give corrective guidance before processing, not after failure.

## 9.3 Preserve source quality

- Never overwrite the source.
- Preview can be 1080p-scaled.
- Export defaults to source aspect and source frame timing.
- Clearly show when 4K/HDR/60 FPS cannot be preserved.
- Test rotation, color space, HDR tone mapping, VFR, 120/240 FPS, and audio sync.

Quality degradation is a recurring complaint in ShaShot reviews.[1](https://apps.apple.com/us/app/shashot-ai-golf-shot-tracker/id1595050744)

## 9.4 Transparent progress

- Named processing stages.
- Elapsed time.
- Cancel button.
- No fake smooth progress.
- Recoverable intermediate output.
- Actionable error with the exact unsupported codec/model condition.

## 9.5 Confidence and provenance

- Suggested tracks are visually distinct.
- Confidence is segment-level.
- Manual edits are never overwritten by rerunning AI without confirmation.
- Keep original AI result for compare/undo.

## 9.6 Offline/privacy

SwingFrame should retain no `INTERNET` permission in the core product.

- On-device models only.
- App-private project storage.
- System picker for media access.
- MediaStore for exports.
- No account identity.
- Optional model packs can be sideloaded with the APK if necessary.

## 9.7 Accessibility

- 48 dp touch targets.
- Tool labels in addition to icons.
- Color is never the only AI/manual distinction.
- High-contrast handle outlines over any video.
- TalkBack descriptions and adjustable scrubbing actions.
- Reduced-motion mode disables glow pulses and spring overshoot.
- Left/right handedness is not inferred from visual design alone.

## 9.8 Testing

### Golden rendering tests

- Same trace layer at 720p, 1080p, 4K.
- Portrait/landscape and 90/270-degree rotation.
- SDR/HDR-tone-mapped preview.
- Different output aspect ratios.

### Timeline tests

- CFR and VFR.
- 24/30/60/120/240 FPS.
- Duplicate PTS and odd containers.
- Exact one-frame stepping.

### Vision tests

Dataset dimensions:

- Behind/down-the-line and face-on.
- Bright sky, cloudy sky, trees, range net, indoor simulator.
- White/yellow balls.
- 30/60 FPS.
- Camera shake and zoom.
- Partial occlusion.
- Ball lost after 2/5/10 frames.

Measure precision, recall, track continuity, endpoint error, and required manual corrections—not only "AI accuracy."

---

# 10. What SwingFrame should copy, reinterpret, and reject

## Copy as a product pattern

- Media-dominant editor.
- Filmstrip plus exact playhead.
- Contextual tool rail.
- Mode-specific property sheets.
- Immediate visual preview.
- Editable automatic result.
- Separate ball, club, hand, body, tempo, and phase layers.

## Reinterpret

- Replace neon-heavy marketing with an editorial analysis identity.
- Replace separate feature silos with one project/layer stack.
- Replace premium crowns with feature provenance/confidence.
- Replace generic timestamp-only readout with exact frame and PTS.
- Replace numerous cosmetic filters with a small deliberate set.
- Replace hidden automatic assumptions with quality and confidence UI.

## Reject

- Login requirement.
- Ads.
- Credits/consumables.
- Subscription state.
- Community/messages.
- Cloud-only processing.
- Watermarks by default.
- Fake distance claims.
- Automatic analysis with no usable correction path.
- Forced clip recreation after failures.

---

# 11. Proposed SwingFrame information architecture

## Home

- Import video.
- Recent local sessions.
- Session metadata and last-opened frame.

## Project

One project, three workspaces:

### Frame Lab

- Exact frame viewer.
- Lines, angles, plumb, shapes, freehand.
- Bookmarks/P positions.
- Compare.

### Trace Lab

- Manual/AI-assisted ball trajectory.
- Club-head and hand paths.
- Keyframe correction.
- Trace style.

### Motion Lab

- Pose/body overlay.
- Tempo.
- P1–P10 candidates.
- Head movement and swing plane.

## Export

- Still/current frame.
- Selected range.
- Full clip.
- Clean/selected/all layers.
- Original/1080p/social aspect.

---

# 12. Development roadmap after research approval

## Phase A — Editorial redesign without feature expansion

- Create design tokens and typography.
- Redesign Home, Import Preview, Loading, and Frame Viewer.
- Replace thick dashboard with a media-first filmstrip and contextual editor rail.
- Preserve current import/index/decode behavior.
- Add no AI yet.

**Exit:** existing videos import and scrub exactly as before; new design is stable at 390×844 and on the actual Android phone.

## Phase B — Annotation engine and layer model

- Normalized geometry.
- Structured drawing tools.
- Selection/edit handles.
- Undo/redo command model.
- Layer stack and persistence.

**Exit:** annotations survive scrubbing, zoom, relaunch, and export-resolution changes.

## Phase C — Manual shot tracer

- Impact/apex/landing setup.
- Curve fitting.
- Progressive reveal.
- Clean, Glow, and Dots styles.
- Start/end timing.
- Keyframe correction.

**Exit:** every normal golf shot can receive a convincing tracer without AI.

## Phase D — Export and score/text overlays

- Source-quality still/video export.
- Optional score bar and distance text.
- 1080p/30–60 FPS first.
- 4K after memory/thermal validation.

## Phase E — Pose, hand path, and tempo

- On-device pose.
- Auto-trim suggestion.
- Tempo ratio.
- Address/top/impact/finish suggestions.
- Hand path and head movement.

## Phase F — Assisted club/ball tracking

- Capture quality gate.
- ROI confirmation.
- Tiny-object candidate detector.
- Temporal tracking and curve fit.
- Confidence editing.

## Phase G — Comparison and advanced motion

- Side-by-side/overlay compare.
- P1–P10.
- Club-head refinement.
- 3D research prototype.

## Explicit defer

Real-time ball tracing should not be attempted until offline post-capture tracking reaches a measured quality target on the actual phone and test dataset.

---

# 13. Acceptance principles for the redesign

1. The frame is always larger and more important than the controls.
2. A user can always identify the active frame, active tool, and active layer.
3. Frame stepping never skips or assumes constant FPS.
4. Opening a tool never permanently hides the timeline.
5. Automatic results are editable vectors.
6. AI failure takes the user to manual mode, not a dead end.
7. No network is required.
8. No source media is overwritten.
9. Export quality limitations are shown before rendering.
10. The visual style is recognizably SwingFrame, not a ShaShot clone.

---

# 14. Recommended direction in one sentence

**Build ShaShot's focused media editor and assisted trajectory workflow, remove its cloud/monetization/social complexity, combine everything into a precise layer-based frame lab, and express it through a quieter editorial design.**

---

# 15. Product decisions — locked July 14, 2026

1. **Primary identity:** analysis-only for the current product. Creator features become a later add-on after the analysis infrastructure is excellent.
2. **First development milestone:** editorial redesign, scrubber/decode-latency improvement, and the full annotation engine.
3. **Tracing strategy:** local AI should create the normal result; manual tracing is the correction/fallback path. Until the local model is trained and validated, manual tracing remains the shippable baseline rather than a fake automatic feature.
4. **Runtime boundary:** entirely local. No inference API, cloud processing, account, or network dependency. Model training happens offline on a workstation; optimized inference models are packaged with the app.
5. **Visual accent:** warm editorial amber, supported by near-black, off-white, and restrained neutral surfaces.
6. **Score/social overlays:** optional export layers later, not part of the analysis foundation.
