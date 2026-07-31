# Feature Roadmap & Manifest

## Phase 1: The Core Foundation (Playback & Analysis)
*   **Precision Slow-Mo Engine:** Frame-by-frame scrubbable video playback with ultra-fine control to pinpoint the exact moment of impact.
*   **On-Device Pose Tracking:** Offline AI (ML Kit) maps 33 body joints in real-time to visualize spine angle, hip rotation, and shoulder tilt without the internet.
*   **Swing Plane Visualizer:** Draws customizable, persistent lines (shaft plane, elbow plane) over your video to instantly spot "over the top" or "under plane" flaws.
*   **Auto-Trim & Clip:** The app intelligently crops your raw 2-minute driving range video down to just the 3-second window containing the actual swing.

## Phase 2: Hyper-Specialized Coaching (The "Greedy" Stuff)
*   **Ghost Mode Overlay:** Stack two swings semi-transparently on top of each other (You vs. Past You, or You vs. Pro) to spot exact sequence differences.
*   **Auto-Flaw Detection (The Swing Score):** Local logic grades 5 checkpoints (Setup, Top, Impact) and automatically calls out your biggest error (e.g., Early Extension, Sway).
*   **Hands-Free Auto-Capture:** Put the phone on a tripod; the camera watches for you to hit a ball and automatically records/saves the swing without you touching the screen.
*   **Live Audio Coach:** Uses text-to-speech through your AirPods to instantly announce your swing tempo (e.g., "Tempo 2.5 to 1") immediately after impact so you don't have to look at the screen.

## Phase 3: Long-Term Improvement Tracking
*   **The Daily Rx (Drill Recommender):** If you exhibit the same flaw (e.g., "Chicken Wing") repeatedly, the app suggests a specific drill to practice the next day via local notification.
*   **Progress Dashboard:** A local SQLite database tracks your Swing Scores and flaw frequencies over weeks and months to visualize your actual improvement curve.
*   **Offline Video Library:** All swings are organized, tagged by club type, and stored securely on your local device storage, completely independent of cloud photos.
