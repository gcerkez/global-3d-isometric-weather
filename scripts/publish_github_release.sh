#!/usr/bin/env bash
# =============================================================================
# publish_github_release.sh
# -------------------------
# Pushes main & tag over SSH (git@github.com:gcerkez/global-3d-isometric-weather.git)
# and publishes/updates GitHub Release with the packaged .plasmoid, .apk, and .zip assets.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
GH_BIN="${HOME}/.local/bin/gh"
REPO_SLUG="gcerkez/global-3d-isometric-weather"
SSH_REMOTE="git@github.com:${REPO_SLUG}.git"
RELEASE_TAG="${1:-v0.0.3}"

if ! command -v "${GH_BIN}" >/dev/null 2>&1; then
    GH_BIN="gh"
fi

cd "${ROOT_DIR}"

echo "=== 1. Checking GitHub CLI Authentication ==="
if ! "${GH_BIN}" auth status >/dev/null 2>&1; then
    "${GH_BIN}" auth login --hostname github.com --git-protocol ssh --skip-ssh-key --web
fi

echo "=== 2. Pushing Code & Tag ${RELEASE_TAG} via SSH (${SSH_REMOTE}) ==="
git remote set-url origin "${SSH_REMOTE}"
git push -u origin main
git push origin "${RELEASE_TAG}" --force

echo "=== 3. Publishing GitHub Release ${RELEASE_TAG} with Widget Packages (.plasmoid, .apk, .zip) ==="
NOTES_FILE="dist/RELEASE_NOTES_${RELEASE_TAG}.md"
if [[ ! -f "${NOTES_FILE}" ]]; then
    NOTES_FILE="dist/RELEASE_NOTES_v0.0.2.md"
fi

if "${GH_BIN}" release view "${RELEASE_TAG}" --repo "${REPO_SLUG}" >/dev/null 2>&1; then
    "${GH_BIN}" release edit "${RELEASE_TAG}" \
        --repo "${REPO_SLUG}" \
        --title "${RELEASE_TAG} — Global 3D Isometric Weather Widget (KDE Plasma 6 / Android APK / Web)" \
        --notes-file "${NOTES_FILE}"
    "${GH_BIN}" release upload "${RELEASE_TAG}" \
        dist/global-3d-isometric-weather-${RELEASE_TAG}.apk \
        dist/global-3d-isometric-weather-${RELEASE_TAG}.plasmoid \
        dist/global-3d-isometric-weather-web-android-${RELEASE_TAG}.zip \
        dist/SHA256SUMS.txt \
        --clobber --repo "${REPO_SLUG}"
else
    "${GH_BIN}" release create "${RELEASE_TAG}" \
        dist/global-3d-isometric-weather-${RELEASE_TAG}.apk \
        dist/global-3d-isometric-weather-${RELEASE_TAG}.plasmoid \
        dist/global-3d-isometric-weather-web-android-${RELEASE_TAG}.zip \
        dist/SHA256SUMS.txt \
        --repo "${REPO_SLUG}" \
        --title "${RELEASE_TAG} — Global 3D Isometric Weather Widget (KDE Plasma 6 / Android APK / Web)" \
        --notes-file "${NOTES_FILE}"
fi

echo "✅ Published Release ${RELEASE_TAG} at https://github.com/${REPO_SLUG}/releases/tag/${RELEASE_TAG}"
