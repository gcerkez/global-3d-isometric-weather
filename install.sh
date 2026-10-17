#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PKG_DIR="${SCRIPT_DIR}/package"
PLUGIN_ID="com.antigravity.yorktownweather"

echo "Installing ${PLUGIN_ID} KDE Plasma 6 Widget..."

if command -v kpackagetool6 >/dev/null 2>&1; then
    if kpackagetool6 --type Plasma/Applet --show "${PLUGIN_ID}" >/dev/null 2>&1; then
        echo "Updating existing installation via kpackagetool6..."
        kpackagetool6 --type Plasma/Applet --upgrade "${PKG_DIR}"
    else
        echo "Installing new package via kpackagetool6..."
        kpackagetool6 --type Plasma/Applet --install "${PKG_DIR}"
    fi
else
    TARGET_DIR="${HOME}/.local/share/plasma/plasmoids/${PLUGIN_ID}"
    echo "kpackagetool6 not found; copying directly to ${TARGET_DIR}..."
    mkdir -p "${TARGET_DIR}"
    cp -r "${PKG_DIR}/"* "${TARGET_DIR}/"
fi

# Package a distributable .plasmoid archive as well
PLASMOID_FILE="${SCRIPT_DIR}/yorktown-3d-isometric-weather.plasmoid"
(cd "${PKG_DIR}" && python3 -m zipfile -c "${PLASMOID_FILE}" .)

echo ""
echo "✅ Installed successfully!"
echo "   • Distribute archive: ${PLASMOID_FILE}"
echo "   • Add to KDE Desktop: Right-click desktop -> 'Enter Edit Mode' -> 'Add or Manage Widgets...' -> Search 'Yorktown 3D Isometric Weather'"
echo "   • Preview in window : plasmawindowed ${PLUGIN_ID}"
