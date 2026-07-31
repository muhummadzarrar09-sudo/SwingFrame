# Phase 2: Wiring the AI Core to the Video Engine

Now that we have both the **Video Engine** (Phase 1) and the **AI Core** (Phase 1.5) built, Phase 2 is where we make them talk to each other. Here is the exact blueprint for how we will map the `OnDevicePoseAnalyzer` to the `VideoPlayerViewModel` without lagging the user's phone.

## 1. Frame Extraction (The Bridge)
ExoPlayer plays video efficiently, but ML Kit requires raw `Bitmap` images.
When the user pauses the video or scrubs to a specific millisecond, we will use Android's `MediaMetadataRetriever` (or `PixelCopy` API for Android 13+) to rip the exact visible frame off the screen and turn it into a 2D Bitmap.

## 2. Expanding the MVI State
We will update our `VideoPlayerState` to hold the AI's findings.

```kotlin
data class VideoPlayerState(
    // ... existing video properties ...
    val isAiEnabled: Boolean = false,
    val isAiAnalyzing: Boolean = false,
    val currentSkeleton: SwingSkeleton? = null,
    val currentSpineAngle: Float? = null
)
```

## 3. Asynchronous Analysis (Zero Lag)
Machine Learning math is heavy. If we run it on the Main UI thread, the app will freeze. 
When the user scrubs to a new frame, we will fire a Kotlin Coroutine on the `Dispatchers.Default` background thread:

1. Intent: `AnalyzeCurrentFrame` is triggered.
2. The UI instantly updates `isAiAnalyzing = true` (maybe showing a cool futuristic loading glow).
3. The background thread grabs the `Bitmap`.
4. `OnDevicePoseAnalyzer.analyzeFrame(bitmap)` processes the skeleton.
5. The background thread pushes the result back to the Main thread.
6. The MVI State updates `currentSkeleton` and `isAiAnalyzing = false`.

## 4. UI Layering (The Cyber Overlay)
In `VideoPlayerScreen.kt`, we simply use a Jetpack Compose `Box` to stack the UI layers exactly like Photoshop:

```kotlin
Box {
    // Layer 1: The Raw Video
    AndroidView(factory = { PlayerView(...) })
    
    // Layer 2: The AI Bones (Only draws if currentSkeleton != null)
    if (state.isAiEnabled) {
        SkeletonOverlay(skeleton = state.currentSkeleton)
    }
}
```

## 5. Auto-Flaw Detection (Future Extension)
Once this loop is running 60 times a second, we can monitor the `currentSpineAngle`. If `currentSpineAngle` at "Address" (Frame 0) is 45 degrees, but at "Impact" it shifts to 60 degrees, the app immediately flags: **"ERROR: EARLY EXTENSION DETECTED."**
