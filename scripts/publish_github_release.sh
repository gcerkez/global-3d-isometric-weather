#!/usr/bin/env bash
# =============================================================================
# publish_github_release.sh
# -------------------------
# Pushes main & tag v0.0.1 over SSH (git@github.com:gcerkez/global-3d-isometric-weather.git)
# and publishes/updates GitHub Release v0.0.1 with the packaged .plasmoid and .zip assets.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
GH_BIN="${HOME}/.local/bin/gh"
REPO_SLUG="gcerkez/global-3d-isometric-weather"
SSH_REMOTE="git@github.com:${REPO_SLUG}.git"

if ! command -v "${GH_BIN}" >/dev/null 2>&1; then
    GH_BIN="gh"
fi

cd "${ROOT_DIR}"

echo "=== 1. Checking GitHub CLI Authentication ==="
if ! "${GH_BIN}" auth status >/dev/null 2>&1; then
    "${GH_BIN}" auth login --hostname github.com --git-protocol ssh --skip-ssh-key --web
fi

echo "=== 2. Pushing Code & Tag v0.0.1 via SSH (${SSH_REMOTE}) ==="
git remote set-url origin "${SSH_REMOTE}"
git push -u origin main
git push origin v0.0.1 --force

echo "=== 3. Updating GitHub Release v0.0.1 with Widget Packages ==="
if "${GH_BIN}" release view v0.0.1 --repo "${REPO_SLUG}" >/dev/null 2>&1; then
    "${GH_BIN}" release edit v0.0.1 \
        --repo "${REPO_SLUG}" \
        --title "v0.0.1 — Global 3D Isometric Weather Widget (KDE Plasma 6 / Web / Android)" \
        --notes-file dist/RELEASE_NOTES_v0.0.1.md
    "${GH_BIN}" release upload v0.0.1 \
        dist/global-3d-isometric-weather-v0.0.1.plasmoid \
        dist/global-3d-isometric-weather-web-android-v0.0.1.zip \
        dist/SHA256SUMS.txt \
        --clobber --repo "${REPO_SLUG}"
else
    "${GH_BIN}" release create v0.0.1 \
        dist/global-3d-isometric-weather-v0.0.1.plasmoid \
        dist/global-3d-isometric-weather-web-android-v0.0.1.zip \
        dist/SHA256SUMS.txt \
        --repo "${REPO_SLUG}" \
        --title "v0.0.1 — Global 3D Isometric Weather Widget (KDE Plasma 6 / Web / Android)" \
        --notes-file dist/RELEASE_NOTES_v0.0.1.md
fi

echo "✅ Published Release v0.0.1 at https://github.com/${REPO_SLUG}/releases/tag/v0.0.1"
