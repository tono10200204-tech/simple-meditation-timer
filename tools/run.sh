#!/bin/bash
# Build the app and run it, either on the emulator or on a phone plugged in
# over USB. Android Studio is not needed for any of this.
#
#   tools/run.sh              build, install, launch on whatever is connected
#   tools/run.sh emulator     start the emulator first, then the same
#
set -euo pipefail

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

cd "$(dirname "$0")/../android"

if [ "${1:-}" = "emulator" ]; then
  if ! adb devices | grep -q emulator; then
    echo "Starting the emulator…"
    emulator -avd smt36 -gpu swiftshader_indirect >/dev/null 2>&1 &
    adb wait-for-device
    until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
  fi
fi

if ! adb devices | grep -qE "device$"; then
  echo "No device. Plug in a phone with USB debugging on, or run: tools/run.sh emulator"
  exit 1
fi

./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.shl.meditation/.MainActivity
