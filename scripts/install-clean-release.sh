#!/usr/bin/env bash
set -euo pipefail

PACKAGE_NAME="${PACKAGE_NAME:-com.hindrax.ss}"
APK_PATH="${1:-app/build/outputs/apk/release/app-release.apk}"

if ! command -v adb >/dev/null 2>&1; then
    echo "ERROR: adb is not installed or not in PATH."
    exit 1
fi

if [ ! -f "$APK_PATH" ]; then
    echo "ERROR: APK not found: $APK_PATH"
    echo "Build it first with: ./gradlew assembleRelease"
    exit 1
fi

devices="$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
device_count="$(printf "%s\n" "$devices" | sed '/^$/d' | wc -l | tr -d ' ')"

if [ "$device_count" -eq 0 ]; then
    echo "ERROR: no authorized Android device found."
    echo "Connect the tablet, enable USB debugging, and accept the RSA prompt."
    exit 1
fi

if [ "$device_count" -gt 1 ]; then
    echo "ERROR: more than one Android device is connected."
    echo "Run with: ANDROID_SERIAL=<serial> $0 $APK_PATH"
    adb devices
    exit 1
fi

serial="${ANDROID_SERIAL:-$devices}"

echo "[1/3] Device: $serial"
echo "[2/3] Removing previous $PACKAGE_NAME installation if present..."
if adb -s "$serial" shell pm path "$PACKAGE_NAME" >/dev/null 2>&1; then
    adb -s "$serial" uninstall "$PACKAGE_NAME" || {
        echo "ERROR: uninstall failed. Remove Hindrax manually from Android Settings and retry."
        exit 1
    }
else
    echo "Package is not installed."
fi

echo "[3/3] Installing $APK_PATH..."
adb -s "$serial" install -r "$APK_PATH"
echo "Install completed."
