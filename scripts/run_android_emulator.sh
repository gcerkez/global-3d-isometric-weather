#!/usr/bin/env bash
# =============================================================================
# run_android_emulator.sh
# -----------------------
# Launches an Android SDK Virtual Device (AVD / Emulator) without requiring
# a physical phone connected, installs the release APK, and opens the app
# for interactive visual testing.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

export JAVA_HOME="${JAVA_HOME:-${HOME}/.local/jdk17}"
export ANDROID_HOME="${ANDROID_HOME:-${HOME}/.local/android-sdk}"
export PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator:${PATH}"

APK_PATH="${ROOT_DIR}/dist/global-3d-isometric-weather-v0.0.2.apk"

echo "=== 1. Checking Android SDK & Emulator Tools ==="
if ! command -v emulator >/dev/null 2>&1; then
    echo "Android emulator binary not found in PATH."
    echo "You can install the Android Emulator via SDK Manager or IntelliJ / Android Studio:"
    echo "  sdkmanager 'emulator' 'system-images;android-34;google_apis;x86_64'"
    echo "Or in IntelliJ / Android Studio: Tools -> Device Manager -> Create Device"
fi

if [[ ! -f "${APK_PATH}" ]]; then
    echo "Building release APK..."
    "${ROOT_DIR}/gradlew" -p "${ROOT_DIR}/android" assembleRelease || gradle -p "${ROOT_DIR}/android" assembleRelease
    cp "${ROOT_DIR}/android/app/build/outputs/apk/release/app-release.apk" "${APK_PATH}"
fi

echo "=== 2. Checking Available Virtual Devices (AVDs) ==="
AVD_LIST=$(emulator -list-avds 2>/dev/null || true)

if [[ -z "${AVD_LIST}" ]]; then
    echo "No Android Virtual Devices (AVD) detected."
    echo ""
    echo "To create one from command line:"
    echo "  avdmanager create avd -n Pixel_Mock_Device -k 'system-images;android-34;google_apis;x86_64' --device 'pixel_7'"
    echo ""
    echo "Or in IntelliJ IDEA / Android Studio:"
    echo "  1. Open IntelliJ / Android Studio"
    echo "  2. Go to Tools -> Device Manager"
    echo "  3. Click 'Create Device' (e.g. Pixel 8 / Pixel 7)"
    echo "  4. Select System Image (API 34) and finish"
    echo "  5. Press 'Play' to pop up the virtual device window"
    echo ""
    echo "Running headless mock device UI tests instead..."
    gradle -p "${ROOT_DIR}/android" test
    exit 0
fi

SELECTED_AVD=$(echo "${AVD_LIST}" | head -n 1)
echo "Found AVD: ${SELECTED_AVD}"
echo "Launching ${SELECTED_AVD} emulator window..."

emulator -avd "${SELECTED_AVD}" -netdelay none -netspeed full &
EMULATOR_PID=$!

echo "Waiting for Android Virtual Device to boot..."
adb wait-for-device
while [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
    sleep 2
done

echo "=== 3. Installing & Launching on Virtual Device ==="
adb install -r "${APK_PATH}"
adb shell am start -n com.antigravity.yorktownweather/.MainActivity

echo "✅ App is running on the Android Emulator!"
