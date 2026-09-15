#!/usr/bin/env bash
# Defaults to building within the fdroid docker container, use scripts/build_release.sh if you dont need this.
exec "$(dirname "$0")/scripts/build_release_linux.sh" "$@"
