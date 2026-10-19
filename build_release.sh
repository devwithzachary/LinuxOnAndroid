#!/usr/bin/env bash
# Build release artifacts: uses containerized F-Droid build if docker is active, otherwise builds locally.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    exec "$SCRIPT_DIR/scripts/build_release_linux.sh" "$@"
else
    exec "$SCRIPT_DIR/scripts/build_release.sh" "$@"
fi
