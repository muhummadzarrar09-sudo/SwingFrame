# SwingFrame: The Ultimate Free AI Golf Coach

An on-device, completely free Android application for advanced golf swing analysis. Built to rival premium $50/month apps without ever sending data to the cloud or costing a dime in API fees.

## Core Philosophy
- **100% On-Device:** No cloud processing, no backend servers, no API costs.
- **Zero Subscriptions:** Everything is built directly into the app for free.
- **Privacy First:** Your terrible swings stay on your phone.

## MVP Features
1. **Precision Slow-Mo Engine:** Frame-by-frame scrubbable video playback.
2. **On-Device AI Tracking:** Uses local ML (TensorFlow Lite / ML Kit) to track joints and draw swing planes.
3. **Ghost Mode Overlay:** Compare swings side-by-side or overlaid.
4. **Auto-Capture:** Set the phone on a tripod, AI detects the swing and auto-clips the video.

## Tech Stack
- Android Native (Kotlin)
- UI: Jetpack Compose
- Video: Media3 (ExoPlayer) with custom frame extraction
- AI: Google ML Kit Pose Detection (Running 100% offline) / TFLite
- Database: Room (Local SQLite)
