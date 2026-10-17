#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PKG_DIR="${SCRIPT_DIR}/package"
PLUGIN_ID="com.antigravity.yorktownweather"
TARGET_DIR="${HOME}/.local/share/plasma/plasmoids/${PLUGIN_ID}"

echo "Installing ${PLUGIN_ID} KDE Plasma 6 Widget..."

# Always remove existing user-space copy first so kpackagetool6 copies all new assets & QML files cleanly
rm -rf "${TARGET_DIR}"

if command -v kpackagetool6 >/dev/null 2>&1; then
    echo "Installing clean package via kpackagetool6..."
    kpackagetool6 --type Plasma/Applet --install "${PKG_DIR}"
else
    echo "kpackagetool6 not found; copying directly to ${TARGET_DIR}..."
    mkdir -p "${TARGET_DIR}"
    cp -r "${PKG_DIR}/"* "${TARGET_DIR}/"
fi

# Clear KDE Plasma 6 QML bytecode caches so changes take effect immediately
echo "Clearing KDE Plasma 6 QML caches..."
rm -rf "${HOME}/.cache/plasmashell/qmlcache/"* "${HOME}/.cache/plasmawindowed/qmlcache/"* 2>/dev/null || true

if command -v kbuildsycoca6 >/dev/null 2>&1; then
    kbuildsycoca6 --noincremental >/dev/null 2>&1 || true
fi

if systemctl --user is-active plasma-plasmashell.service >/dev/null 2>&1; then
    echo "Restarting plasma-plasmashell.service to reload desktop widget..."
    systemctl --user restart plasma-plasmashell.service || true
fi

# Package a distributable .plasmoid archive as well
PLASMOID_FILE="${SCRIPT_DIR}/global-3d-isometric-weather.plasmoid"
(cd "${PKG_DIR}" && python3 -m zipfile -c "${PLASMOID_FILE}" .)

echo ""
echo "✅ Installed & reloaded successfully!"
echo "   • Distribute archive: ${PLASMOID_FILE}"
echo "   • Add to KDE Desktop: Right-click desktop -> 'Enter Edit Mode' -> 'Add or Manage Widgets...' -> Search 'Global 3D Isometric Weather'"
echo "   • Preview in window : plasmawindowed ${PLUGIN_ID}"
