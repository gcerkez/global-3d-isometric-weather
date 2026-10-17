#!/usr/bin/env bash
# =============================================================================
# publish_github_release.sh
# -------------------------
# Creates the GitHub repository (gcerkez/yorktown-3d-isometric-weather),
# pushes main & tag v0.0.1 over SSH (git@github.com:...), and publishes
# GitHub Release v0.0.1 with the packaged .plasmoid and .zip widget assets.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
GH_BIN="${HOME}/.local/bin/gh"
REPO_SLUG="gcerkez/yorktown-3d-isometric-weather"
SSH_REMOTE="git@github.com:${REPO_SLUG}.git"

if ! command -v "${GH_BIN}" >/dev/null 2>&1; then
    GH_BIN="gh"
fi

cd "${ROOT_DIR}"

echo "=== 1. Checking GitHub CLI Authentication ==="
if ! "${GH_BIN}" auth status >/dev/null 2>&1; then
    echo "GitHub CLI is not yet authenticated with an API token."
    echo "Starting interactive SSH-backed GitHub authentication..."
    "${GH_BIN}" auth login --hostname github.com --git-protocol ssh --web
fi

echo "=== 2. Creating Repository ${REPO_SLUG} on GitHub (if not exists) ==="
if ! "${GH_BIN}" repo view "${REPO_SLUG}" >/dev/null 2>&1; then
    "${GH_BIN}" repo create "${REPO_SLUG}" \
        --public \
        --description "3D Isometric Real-Time & Generative Weather Widget for KDE Plasma 6 (Bazzite/Linux), Web/PWA, and Android (OnePlus 13)"
fi

echo "=== 3. Pushing Code & Tag v0.0.1 via SSH (${SSH_REMOTE}) ==="
if git remote get-url origin >/dev/null 2>&1; then
    git remote set-url origin "${SSH_REMOTE}"
else
    git remote add origin "${SSH_REMOTE}"
fi

git push -u origin main
git push origin v0.0.1 --force

echo "=== 4. Creating GitHub Release v0.0.1 with Widget Packages ==="
if "${GH_BIN}" release view v0.0.1 --repo "${REPO_SLUG}" >/dev/null 2>&1; then
    "${GH_BIN}" release upload v0.0.1 \
        dist/yorktown-3d-isometric-weather-v0.0.1.plasmoid \
        dist/yorktown-3d-isometric-weather-web-android-v0.0.1.zip \
        dist/SHA256SUMS.txt \
        --clobber --repo "${REPO_SLUG}"
else
    "${GH_BIN}" release create v0.0.1 \
        dist/yorktown-3d-isometric-weather-v0.0.1.plasmoid \
        dist/yorktown-3d-isometric-weather-web-android-v0.0.1.zip \
        dist/SHA256SUMS.txt \
        --repo "${REPO_SLUG}" \
        --title "v0.0.1 — Yorktown & Global 3D Isometric Weather Widget (KDE Plasma 6 / Web / Android)" \
        --notes-file dist/RELEASE_NOTES_v0.0.1.md
fi

echo "✅ Published Release v0.0.1 at https://github.com/${REPO_SLUG}/releases/tag/v0.0.1"
