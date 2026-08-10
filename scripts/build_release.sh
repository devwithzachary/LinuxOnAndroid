#!/usr/bin/env bash
set -e

# LinuxOnAndroid Unified Local Release Build & Deploy Script
# Builds both Google Play App Bundle (.aab) and F-Droid / GitHub Signed APK (.apk)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

echo "============================================================"
echo "🚀 Starting LinuxOnAndroid Unified Release Build"
echo "============================================================"

# Extract Version Name from app/build.gradle.kts
VERSION_NAME=$(grep -E 'versionName\s*=' app/build.gradle.kts | head -n 1 | sed 's/.*"\(.*\)".*/\1/')
VERSION_CODE=$(grep -E 'versionCode\s*=' app/build.gradle.kts | head -n 1 | sed 's/[^0-9]*//g')

if [ -z "$VERSION_NAME" ]; then
    VERSION_NAME="1.0.1"
fi

echo "📦 Target Version: $VERSION_NAME (code: $VERSION_CODE)"
echo ""

# Execute Gradle Clean Build for Release AAB and APK
./gradlew clean bundleRelease assembleRelease

OUT_DIR="$PROJECT_ROOT/release"
mkdir -p "$OUT_DIR"

AAB_SOURCE="$PROJECT_ROOT/app/build/outputs/bundle/release/app-release.aab"
APK_SOURCE="$PROJECT_ROOT/app/build/outputs/apk/release/app-release.apk"

AAB_TARGET="$OUT_DIR/LinuxOnAndroid-${VERSION_NAME}-playstore.aab"
APK_TARGET="$OUT_DIR/LinuxOnAndroid-${VERSION_NAME}-release.apk"

if [ -f "$AAB_SOURCE" ]; then
    cp "$AAB_SOURCE" "$AAB_TARGET"
fi

if [ -f "$APK_SOURCE" ]; then
    cp "$APK_SOURCE" "$APK_TARGET"
fi

echo ""
echo "============================================================"
echo "🎉 Build Completed Successfully!"
echo "============================================================"
echo "Output files ready in: $OUT_DIR"
echo ""

if [ -f "$AAB_TARGET" ]; then
    AAB_SIZE=$(ls -lh "$AAB_TARGET" | awk '{print $5}')
    AAB_SHA=$(shasum -a 256 "$AAB_TARGET" | awk '{print $1}')
    echo " 📱 [Play Store Bundle] : $AAB_TARGET"
    echo "    Size: $AAB_SIZE | SHA256: $AAB_SHA"
fi

echo ""

if [ -f "$APK_TARGET" ]; then
    APK_SIZE=$(ls -lh "$APK_TARGET" | awk '{print $5}')
    APK_SHA=$(shasum -a 256 "$APK_TARGET" | awk '{print $1}')
    echo " 🤖 [F-Droid / GitHub APK]: $APK_TARGET"
    echo "    Size: $APK_SIZE | SHA256: $APK_SHA"
fi

echo "============================================================"
