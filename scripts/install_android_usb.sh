#!/usr/bin/env bash
# =============================================================================
# install_android_usb.sh
# ----------------------
# Installs the signed Global 3D Isometric Weather Android APK
# (dist/global-3d-isometric-weather-v0.0.1.apk) onto a USB-connected Android
# phone (e.g., OnePlus 13) via ADB and launches the interactive app/configurator.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
APK_PATH="${ROOT_DIR}/dist/global-3d-isometric-weather-v0.0.1.apk"
ADB_BIN="${HOME}/.local/android-sdk/platform-tools/adb"

if ! command -v "${ADB_BIN}" >/dev/null 2>&1; then
    ADB_BIN="adb"
fi

echo "=== Checking USB-Connected Android Devices via ADB ==="
"${ADB_BIN}" start-server
DEVICES_OUT="$("${ADB_BIN}" devices | tail -n +2 | sed '/^[[:space:]]*$/d')"

if [ -z "${DEVICES_OUT}" ]; then
    echo "⚠️  No Android device detected over USB yet."
    echo "   1. Connect your OnePlus 13 via USB cable."
    echo "   2. On your phone: Settings -> About device -> Version -> Tap 'Build number' 7 times."
    echo "   3. Go to Settings -> Additional settings -> Developer options -> Enable 'USB debugging'."
    echo "   4. Tap 'Allow' on the 'Allow USB debugging?' prompt on your phone screen."
    exit 1
fi

echo "Found device(s):"
echo "${DEVICES_OUT}"

if echo "${DEVICES_OUT}" | grep -q "unauthorized"; then
    echo "⚠️  Device is connected but 'unauthorized'. Please unlock your phone and tap 'Allow' on the USB debugging popup!"
    exit 1
fi

echo "=== Installing ${APK_PATH} onto your phone ==="
"${ADB_BIN}" install -r "${APK_PATH}"

echo "=== Launching Global 3D Isometric Weather on your phone ==="
"${ADB_BIN}" shell am start -n com.antigravity.yorktownweather/.MainActivity

echo "✅ Installed & launched! Long-press your OnePlus 13 Homescreen -> Widgets -> Global 3D Isometric Weather to add the widget."
