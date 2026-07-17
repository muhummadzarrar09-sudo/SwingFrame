# SwingFrame device smoke checklist

Use this after every APK produced from the hardening branch. Record phone model, Android version, display/font size settings, free storage, build commit, and source fixture name.

## Build gate

```powershell
.\swingframe-build.ps1
```

The helper now runs unit tests, `lintDebug`, and debug assembly. Do not install the APK if any task fails. Save the full console output and `artifacts/SwingFrame.apk` SHA-256.

## UI layouts

Run once at default font size and once at the largest practical Android font/display size.

- Home scrolls without clipping on an empty install.
- Recent rows keep filenames readable; Relink and Delete do not trigger Open.
- Source Review scrolls; both bottom actions remain reachable.
- Viewer header shows Back, title, Export, and More without overlap.
- More contains Reset zoom when zoomed, Mark frame, Marked frames, Open another video, and Source information.
- The video is never covered by annotation tools, annotation actions, or the timeline.
- Annotation tools form one horizontal, scrollable row.
- Timeline labels do not collide at frame 1, the last frame, or a long timestamp.
- Playback speed opens from one compact selector and all four speeds remain selectable.
- Export setup scrolls at large font size and its Export/Cancel actions remain reachable.
- Errors are readable and dismissible without permanently blocking navigation.

## Navigation and lifecycle

- System Back from Probing returns Home.
- System Back from Source Review returns Home.
- Cancel during Indexing returns Source Review and does **not** reopen Viewer later.
- System Back from Viewer returns Source Review.
- Start playback, background the app, wait 10 seconds, and return: playback must be paused.
- Cancel the document picker from Home, Viewer, and Relink; the current screen/project must remain intact.

## Rapid-seek race test

1. Import a 10+ second 60 FPS source.
2. Scrub quickly: beginning → end → middle → beginning.
3. Stop on a frame with an obvious unique visual marker.
4. Confirm requested frame number, timestamp, bitmap, and annotations all agree.
5. Repeat while thumbnails are still loading.
6. Repeat immediately after changing playback speed.

No older request may replace the final requested frame.

## Annotation geometry

- On a 16:9 source, draw a non-axis-aligned angle and compare its value with a desktop geometry check.
- Tiny accidental line/box/circle drags below roughly 8 dp are rejected consistently across display densities.
- Drag every shape against all four edges; dimensions do not change.
- Zoom to 6× and pan aggressively; the frame cannot be lost permanently off-screen.
- Two-finger zoom cancels an active draft without creating a stray annotation.
- Draw a long freehand path and watch for lag or memory spikes; record the result (point simplification remains open work).
- Relaunch after edits and verify vectors, styles, bookmarks, and last frame.

## Project and relink safety

- Reopen an unchanged recent source: no mismatch warning.
- Replace source contents behind the same URI if the provider permits it: a mismatch warning must appear.
- Relink to a matching copy and verify annotations survive.
- Relink to a different clip and cancel; the original project remains intact.
- Force a mismatch and confirm; verify the project and annotations survive a relaunch.
- Attempt to relink one project to a URI already attached to another project; it must be rejected.
- Delete one of two projects that reference the same source (if this state exists from older data); remaining annotations must survive.
- Fill storage or use a fault-injection build and confirm save failures are shown rather than silently accepted.

## Export

For clean and annotated output, test portrait and landscape sources with rotation metadata 0/90/180/270 where fixtures are available.

- Current still has the expected source resolution, orientation, color, and exact visible frame.
- 1× video starts/ends on the requested range and retains audio when supported.
- 0.5× and 0.25× videos are silent as disclosed and annotations stay on the intended source frames.
- Cancel a video export; no partial gallery item remains.
- Background during a long export and record behavior. Process-durable foreground export is still an open release blocker.
- Verify files appear under Pictures/Movies `SwingFrame` and can be opened after reboot.
- Run with low free storage and confirm failure is clear and no pending gallery item remains.

## Accessibility

- TalkBack identifies every annotation tool and announces the selected tool.
- TalkBack can adjust the timeline with progress actions.
- Error messages are announced.
- Every primary control has a usable touch target.
- Test Switch Access or keyboard/D-pad if available; record unreachable controls.
- Verify text contrast in bright outdoor conditions.

## Performance notes to capture

- Cold import/probe time.
- Timestamp index time and indexed sample count.
- Cold first-frame latency.
- Warm adjacent-frame and cold random-seek latency.
- Peak memory during repeated 4K seeks.
- Peak memory during annotated 4K still export.
- Temperature/battery behavior during a 60-second export.
- Any codec fallback message or decode timeout.
