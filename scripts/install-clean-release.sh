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
echo "[2/3] Removing previous $PACKAGE_NAME installation from all visible Android users..."
users="$(adb -s "$serial" shell pm list users | sed -n 's/.*UserInfo{\([0-9][0-9]*\):.*/\1/p' | tr -d '\r')"
if [ -z "$users" ]; then
    users="0"
fi

for user_id in $users; do
    echo "Checking user $user_id..."
    if adb -s "$serial" shell pm list packages --user "$user_id" "$PACKAGE_NAME" | tr -d '\r' | grep -q "^package:$PACKAGE_NAME$"; then
        adb -s "$serial" shell pm uninstall --user "$user_id" "$PACKAGE_NAME" >/dev/null || true
    fi
done

if adb -s "$serial" shell pm list packages -u "$PACKAGE_NAME" | tr -d '\r' | grep -q "^package:$PACKAGE_NAME$"; then
    echo "Package still has an installed or uninstalled residue. Trying full uninstall..."
    adb -s "$serial" uninstall "$PACKAGE_NAME" >/dev/null || true
fi

if adb -s "$serial" shell pm list packages -u "$PACKAGE_NAME" | tr -d '\r' | grep -q "^package:$PACKAGE_NAME$"; then
    echo "ERROR: $PACKAGE_NAME still exists on the device:"
    adb -s "$serial" shell pm list packages -u "$PACKAGE_NAME" | tr -d '\r'
    echo "Remove Hindrax from every Android user/profile, work profile, secure folder, or app clone, then retry."
    exit 1
fi

echo "[3/3] Installing $APK_PATH..."
adb -s "$serial" install -r "$APK_PATH"
echo "Install completed."
