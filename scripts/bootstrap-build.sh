#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLCHAIN_DIR="${SWINGFRAME_TOOLCHAIN_DIR:-$HOME/.cache/swingframe-toolchain}"
JDK_DIR="$TOOLCHAIN_DIR/jdk-17"
SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$TOOLCHAIN_DIR/android-sdk}}"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"

mkdir -p "$TOOLCHAIN_DIR"

if [[ ! -x "$JDK_DIR/bin/java" ]]; then
  echo "[1/4] Downloading a local Temurin JDK 17..."
  rm -rf "$JDK_DIR"
  mkdir -p "$JDK_DIR"
  curl --fail --location --retry 3 \
    "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse" \
    --output "$TOOLCHAIN_DIR/jdk17.tar.gz"
  tar -xzf "$TOOLCHAIN_DIR/jdk17.tar.gz" -C "$JDK_DIR" --strip-components=1
  rm -f "$TOOLCHAIN_DIR/jdk17.tar.gz"
else
  echo "[1/4] Reusing JDK 17 at $JDK_DIR"
fi
export JAVA_HOME="$JDK_DIR"
export PATH="$JAVA_HOME/bin:$PATH"

if [[ ! -x "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]]; then
  echo "[2/4] Downloading Android command-line tools..."
  rm -rf "$SDK_DIR/cmdline-tools"
  mkdir -p "$SDK_DIR/cmdline-tools/latest"
  curl --fail --location --retry 3 "$CMDLINE_TOOLS_URL" \
    --output "$TOOLCHAIN_DIR/android-commandline-tools.zip"
  TMP_TOOLS="$TOOLCHAIN_DIR/cmdline-tools-unpack"
  rm -rf "$TMP_TOOLS"
  mkdir -p "$TMP_TOOLS"
  unzip -q "$TOOLCHAIN_DIR/android-commandline-tools.zip" -d "$TMP_TOOLS"
  mv "$TMP_TOOLS/cmdline-tools/"* "$SDK_DIR/cmdline-tools/latest/"
  rm -rf "$TMP_TOOLS" "$TOOLCHAIN_DIR/android-commandline-tools.zip"
else
  echo "[2/4] Reusing Android command-line tools at $SDK_DIR"
fi
export ANDROID_SDK_ROOT="$SDK_DIR"
export ANDROID_HOME="$SDK_DIR"
export PATH="$SDK_DIR/platform-tools:$SDK_DIR/cmdline-tools/latest/bin:$PATH"

SDKMANAGER="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"
echo "[3/4] Installing Android API 36 build packages..."
yes | "$SDKMANAGER" --licenses >/dev/null || true
"$SDKMANAGER" "platform-tools" "platforms;android-36" "build-tools;36.0.0"

if [[ ! -x "$ROOT_DIR/gradlew" ]]; then
  echo "Gradle wrapper is missing. Recreate it with Gradle 8.13 before building." >&2
  exit 1
fi

echo "[4/4] Building and testing SwingFrame..."
cd "$ROOT_DIR"
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
mkdir -p artifacts
cp app/build/outputs/apk/debug/app-debug.apk artifacts/SwingFrame.apk

printf '\nAPK ready:\n  %s\n\nSHA-256:\n' "$ROOT_DIR/artifacts/SwingFrame.apk"
sha256sum artifacts/SwingFrame.apk
