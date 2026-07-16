# Phase 1 — Editorial viewer and scrub performance

Version: `0.2.0-editorial`

## Included

### Decode path

- Media3 exact-frame extraction now starts with the platform hardware codec selector.
- If the hardware codec rejects the source, the persistent decoder switches to Media3's software-preferred selector and retries.
- Exact seek parameters are requested explicitly.
- The decoder remains alive for the entire viewer session.

### Scrub scheduling

- Slider events no longer cancel and recreate decode work continuously.
- A conflated request channel retains only the latest uncached target while allowing the in-flight decode to finish cleanly.
- Cached frames commit immediately.
- After an exact frame resolves, SwingFrame prefetches a directional ±2-frame window.
- New user input always interrupts prefetch before the next neighbor decode.
- The playhead position is direct and no longer animates behind the user's finger.

### Timeline

- Added an independent 12-sample thumbnail filmstrip.
- Thumbnails are orientation aids and may use nearby keyframes; the amber playhead, frame number, and timestamp still address the exact indexed frame.
- Added rate-limited haptics while dragging.
- Added a compact resolving badge instead of blocking the frame with a large spinner.

### Editorial design

- Replaced lime with a warm amber interaction accent.
- Added near-black, off-white, and restrained neutral design tokens.
- Reworked Home into a minimal editorial masthead and source-first call to action.
- Reworked Source Review into a technical ledger.
- Reworked timeline analysis into a compact masthead, framed media stage, filmstrip, and smaller transport dock.
- Removed animated playhead lag and excessive rounded dashboard cards.

## Not included yet

- Drawing/annotation tools
- Persistent projects/recent sessions
- Manual shot tracer
- Local AI inference
- Export

## Build

From Windows PowerShell in the project root:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\swingframe-build.ps1
```

The final file is copied to `artifacts\SwingFrame.apk`.

## Device test checklist

1. Import a 1080p/30 H.264 clip.
2. Drag slowly across the filmstrip and confirm the frame number follows the finger immediately.
3. Step repeatedly forward and backward; adjacent frames should become instant after prefetch warms.
4. Jump from the beginning to the end and confirm the resolving badge appears without blocking gestures.
5. Test 1080p/60, 4K/30 HEVC, and a phone slow-motion clip.
6. Confirm VFR timestamp/frame values remain stable.
7. Pinch and pan, then double-tap to reset.
8. Play at 0.1×, 0.25×, 0.5×, and 1×.
9. Return to Source Review and import another clip.
10. Record phone model, source format, cold random-seek latency, warm adjacent-frame latency, and any decoder errors.

## Expected limitation

A cold random seek into an uncached inter-frame-compressed 4K video cannot be guaranteed to render in one display frame. Phase 1 minimizes perceived and repeated latency through hardware decoding, clean request coalescing, filmstrip orientation, and an exact-frame prefetch window. It does not display an approximate thumbnail as if it were the requested exact frame.
