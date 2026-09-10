#!/usr/bin/env bash
set -e

# LinuxOnAndroid Linux-Environment Release Build Script (for macOS & Linux)
# Builds release APK and AAB inside the official F-Droid Debian Trixie buildserver container
# to ensure 100% byte-for-byte reproducibility with F-Droid build servers.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

echo "============================================================"
echo "🐧 LinuxOnAndroid Reproducible Linux Release Build"
echo "============================================================"

# 1. Check for Docker
if ! command -v docker >/dev/null 2>&1; then
    echo "❌ Error: 'docker' command was not found on your system."
    echo ""
    echo "To run this lightweight Linux build on your Mac, please install either:"
    echo "  • Colima (Lightweight, CLI-only, open-source):"
    echo "      brew install colima docker"
    echo "      colima start --cpu 4 --memory 6 --rosetta"
    echo ""
    echo "  • OrbStack (Fast, native macOS app, minimal resource usage):"
    echo "      brew install --cask orbstack"
    echo ""
    echo "  • Docker Desktop:"
    echo "      brew install --cask docker"
    echo "============================================================"
    exit 1
fi

# Check if Docker daemon is responsive
if ! docker info >/dev/null 2>&1; then
    echo "❌ Error: Docker daemon is not running."
    echo ""
    echo "If using Colima, start it with:"
    echo "  colima start --cpu 4 --memory 6 --rosetta"
    echo "If using OrbStack or Docker Desktop, launch the application from Applications."
    echo "============================================================"
    exit 1
fi

# 2. Extract Version Info
VERSION_NAME=$(grep -E 'versionName\s*=' app/build.gradle.kts | head -n 1 | sed 's/.*"\(.*\)".*/\1/')
VERSION_CODE=$(grep -E 'versionCode\s*=' app/build.gradle.kts | head -n 1 | sed 's/[^0-9]*//g')

echo "📦 Target Version: $VERSION_NAME (code: $VERSION_CODE)"
echo "🐳 Build Container: registry.gitlab.com/fdroid/fdroidserver:buildserver-trixie"
echo ""

DOCKER_IMAGE="registry.gitlab.com/fdroid/fdroidserver:buildserver-trixie"

# Ensure release and host cache directories exist
mkdir -p "$PROJECT_ROOT/release"
mkdir -p "$HOME/.cache/android-sdk-linux/ndk"
mkdir -p "$HOME/.cache/android-sdk-linux/platforms"
mkdir -p "$HOME/.cache/android-sdk-linux/build-tools"
mkdir -p "$HOME/.cache/android-sdk-linux/cmake"

# Gradle cache volume mount for speed
GRADLE_CACHE_MOUNT=""
if [ -d "$HOME/.gradle/caches" ]; then
    GRADLE_CACHE_MOUNT="-v $HOME/.gradle/caches:/root/.gradle/caches"
fi

# 3. Preserve host jniLibs & local.properties so release builds do not dirty your git working tree
TMP_JNILIBS_BACKUP=""
if [ -d "$PROJECT_ROOT/app/src/main/jniLibs" ]; then
    TMP_JNILIBS_BACKUP=$(mktemp -d)
    cp -a "$PROJECT_ROOT/app/src/main/jniLibs/." "$TMP_JNILIBS_BACKUP/"
fi

restore_host_state() {
    echo "🔄 Restoring original host files to keep git working tree clean..."
    if [ -n "$TMP_JNILIBS_BACKUP" ] && [ -d "$TMP_JNILIBS_BACKUP" ]; then
        cp -a "$TMP_JNILIBS_BACKUP/." "$PROJECT_ROOT/app/src/main/jniLibs/"
        rm -rf "$TMP_JNILIBS_BACKUP"
    fi
    if [ -f "$PROJECT_ROOT/local.properties.host" ]; then
        mv "$PROJECT_ROOT/local.properties.host" "$PROJECT_ROOT/local.properties"
    fi
}
trap restore_host_state EXIT

# 4. Execute build in the F-Droid buildserver container
FDROID_APP_ID="com.devwithzachary.completelinuxinstaller"
FDROID_BUILD_DIR="/home/vagrant/build/$FDROID_APP_ID"

echo "🚀 Starting containerized build in Debian Trixie (linux/amd64)..."
echo "📁 Container Mount Path: $FDROID_BUILD_DIR"

docker run --rm \
    --platform linux/amd64 \
    -v "$PROJECT_ROOT":"$FDROID_BUILD_DIR" \
    -v "$HOME/.cache/android-sdk-linux/ndk":/opt/android-sdk/ndk \
    -v "$HOME/.cache/android-sdk-linux/platforms":/opt/android-sdk/platforms \
    -v "$HOME/.cache/android-sdk-linux/build-tools":/opt/android-sdk/build-tools \
    -v "$HOME/.cache/android-sdk-linux/cmake":/opt/android-sdk/cmake \
    $GRADLE_CACHE_MOUNT \
    -w "$FDROID_BUILD_DIR" \
    "$DOCKER_IMAGE" \
    bash -c '
        set -e
        echo "🔧 Installing build prerequisites (make, gawk)..."
        apt-get update -qq
        apt-get install -y -qq make gawk >/dev/null

        if [ ! -d "/opt/android-sdk/ndk/28.2.13676358" ]; then
            echo "⬇️  Installing Android NDK 28.2.13676358 (cached on host)..."
            yes | sdkmanager "ndk;28.2.13676358" >/dev/null
        fi

        if [ ! -d "/opt/android-sdk/cmake/3.22.1" ]; then
            echo "⬇️  Installing CMake 3.22.1 (cached on host)..."
            yes | sdkmanager "cmake;3.22.1" >/dev/null
        fi

        echo "🧹 Cleaning existing jniLibs..."
        rm -rf app/src/main/jniLibs/*

        echo "🏗️  Building native libraries with Linux NDK (28.2.13676358)..."
        ./scripts/build_native_libs.sh /opt/android-sdk/ndk/28.2.13676358

        echo "⚙️  Configuring local.properties for Linux build environment..."
        cp local.properties local.properties.host 2>/dev/null || true
        echo "sdk.dir=/opt/android-sdk" > local.properties
        echo "ndk.dir=/opt/android-sdk/ndk/28.2.13676358" >> local.properties

        echo "📦 Compiling Android release artifacts with Gradle..."
        ./gradlew clean bundleRelease assembleRelease --no-daemon --no-configuration-cache
    '

OUT_DIR="$PROJECT_ROOT/release"
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
echo "🎉 Linux Release Build Completed Successfully!"
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
