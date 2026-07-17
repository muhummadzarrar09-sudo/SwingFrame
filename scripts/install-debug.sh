#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="$ROOT_DIR/artifacts/SwingFrame.apk"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb was not found. Add Android SDK platform-tools to PATH." >&2
  exit 1
fi
if [[ ! -f "$APK" ]]; then
  echo "Build the APK first with ./scripts/bootstrap-build.sh" >&2
  exit 1
fi

adb install -r "$APK"
echo "SwingFrame installed. Launch it from the phone's app drawer."
