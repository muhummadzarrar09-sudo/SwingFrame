# SwingFrame ProGuard / R8 rules (referenced from app/build.gradle.kts release build).
# NOTE: this file MUST exist — the release build fails if the referenced file is missing.

# ML Kit pose detection (accurate) is delivered via Google Play Services. Keep its classes
# and annotations so the model binding / native side is not stripped at release time.
-keep class com.google.mlkit.vision.pose.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.mlkit.common.** { *; }
-keepattributes *Annotation*

# Media3 / ExoPlayer ships its own consumer rules; no additional keeps required.
