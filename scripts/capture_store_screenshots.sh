#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
IMAGES_ROOT="$REPO_ROOT/fastlane/metadata/android/en-US/images"

if [ -x "$HOME/Android/Sdk/platform-tools/adb" ]; then
    ADB_BIN="$HOME/Android/Sdk/platform-tools/adb"
elif command -v adb >/dev/null 2>&1; then
    ADB_BIN="$(command -v adb)"
else
    echo "Error: adb not found in \$HOME/Android/Sdk/platform-tools or PATH" >&2
    exit 1
fi

adb_cmd() {
    local serial="${1:-}"
    shift
    if [ -n "$serial" ]; then
        "$ADB_BIN" -s "$serial" "$@"
    else
        "$ADB_BIN" "$@"
    fi
}

demo_on() {
    local serial="${1:-}"
    adb_cmd "$serial" shell settings put global sysui_demo_allowed 1
    adb_cmd "$serial" shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
    adb_cmd "$serial" shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200 >/dev/null
    adb_cmd "$serial" shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
    adb_cmd "$serial" shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e mobile show -e datatype none >/dev/null
    adb_cmd "$serial" shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null
    echo "Demo Mode enabled${serial:+ on $serial}."
}

demo_off() {
    local serial="${1:-}"
    adb_cmd "$serial" shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null || true
    echo "Demo Mode disabled${serial:+ on $serial}."
}

snap() {
    local bucket="${1:?Usage: snap <phone|tablet7|tablet10> <name> [serial]}"
    local name="${2:?Usage: snap <phone|tablet7|tablet10> <name> [serial]}"
    local serial="${3:-}"

    local dir_name
    case "$bucket" in
        phone) dir_name="phoneScreenshots" ;;
        tablet7) dir_name="sevenInchScreenshots" ;;
        tablet10) dir_name="tenInchScreenshots" ;;
        *)
            echo "Error: unknown bucket '$bucket' (expected phone, tablet7, or tablet10)" >&2
            exit 1
            ;;
    esac

    local out_dir="$IMAGES_ROOT/$dir_name"
    mkdir -p "$out_dir"
    local out_file="$out_dir/${name%.png}.png"

    adb_cmd "$serial" shell screencap -p /data/local/tmp/loa_snap.png
    adb_cmd "$serial" pull /data/local/tmp/loa_snap.png "$out_file" >/dev/null
    adb_cmd "$serial" shell rm -f /data/local/tmp/loa_snap.png

    python3 - "$out_file" <<'PY'
import os, struct, sys
path = sys.argv[1]
with open(path, "rb") as f:
    data = f.read(24)
w, h = struct.unpack(">II", data[16:24])
size_kb = os.path.getsize(path) / 1024.0
print(f"Captured {path} ({w}x{h}, {size_kb:.1f} KB)")
PY
}

set_tablet7() {
    local serial="${1:-}"
    adb_cmd "$serial" shell wm size 1200x1920
    adb_cmd "$serial" shell wm density 240
    adb_cmd "$serial" shell settings put system accelerometer_rotation 0
    adb_cmd "$serial" shell settings put system user_rotation 0
    echo "Configured 7-inch portrait display (1200x1920 @ 240dpi)${serial:+ on $serial}."
}

set_tablet10() {
    local serial="${1:-}"
    adb_cmd "$serial" shell wm size 2560x1600
    adb_cmd "$serial" shell wm density 320
    adb_cmd "$serial" shell settings put system accelerometer_rotation 0
    adb_cmd "$serial" shell settings put system user_rotation 1
    echo "Configured 10-inch landscape display (2560x1600 @ 320dpi)${serial:+ on $serial}."
}

reset_display() {
    local serial="${1:-}"
    adb_cmd "$serial" shell wm size reset
    adb_cmd "$serial" shell wm density reset
    adb_cmd "$serial" shell settings put system accelerometer_rotation 1
    echo "Reset display overrides${serial:+ on $serial}."
}

cmd="${1:-}"
case "$cmd" in
    demo-on)
        demo_on "${2:-}"
        ;;
    demo-off)
        demo_off "${2:-}"
        ;;
    snap)
        snap "${2:-}" "${3:-}" "${4:-}"
        ;;
    tablet7)
        set_tablet7 "${2:-}"
        ;;
    tablet10)
        set_tablet10 "${2:-}"
        ;;
    reset-display)
        reset_display "${2:-}"
        ;;
    *)
        echo "Usage: $0 {demo-on|demo-off|snap <phone|tablet7|tablet10> <name>|tablet7|tablet10|reset-display} [serial]" >&2
        exit 1
        ;;
esac
