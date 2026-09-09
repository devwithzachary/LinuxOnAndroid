#!/usr/bin/env bash
set -e

# LinuxOnAndroid Native Libraries Build & Rebuild Script
# Builds libproot.so, libproot_loader.so, libproot_loader32.so, libtalloc.so, libandroid-shmem.so
# Supports: arm64-v8a, armeabi-v7a, x86_64
# Compatible with local macOS/Linux developer environments and F-Droid CI build servers.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
EXTERNAL_DIR="$PROJECT_ROOT/external"
JNI_LIBS_DIR="$PROJECT_ROOT/app/src/main/jniLibs"
BUILD_WORK_DIR="$PROJECT_ROOT/build/native_libs_work"

MIN_API=24

# -------------------------------------------------------------
# 1. Update / Fetch Latest Sources from Upstream (Optional)
# -------------------------------------------------------------
if [ "$1" = "--fetch-latest" ] || [ "$1" = "--update" ]; then
    echo "============================================================"
    echo "🔄 Updating external native sources from upstream"
    echo "============================================================"
    mkdir -p "$EXTERNAL_DIR"

    # libandroid-shmem
    echo "Fetching termux/libandroid-shmem..."
    TMP_SHMEM=$(mktemp -d)
    git clone --depth 1 https://github.com/termux/libandroid-shmem.git "$TMP_SHMEM"
    mkdir -p "$EXTERNAL_DIR/libandroid-shmem"
    cp "$TMP_SHMEM/shmem.c" "$EXTERNAL_DIR/libandroid-shmem/"
    cp "$TMP_SHMEM/shm.h" "$EXTERNAL_DIR/libandroid-shmem/"
    cp "$TMP_SHMEM/exports.txt" "$EXTERNAL_DIR/libandroid-shmem/" 2>/dev/null || true
    cp "$TMP_SHMEM/LICENSE"* "$EXTERNAL_DIR/libandroid-shmem/" 2>/dev/null || true
    rm -rf "$TMP_SHMEM"

    # termux proot
    echo "Fetching termux/proot..."
    TMP_PROOT=$(mktemp -d)
    git clone --depth 1 https://github.com/termux/proot.git "$TMP_PROOT"
    mkdir -p "$EXTERNAL_DIR/proot"
    cp -r "$TMP_PROOT/src/"* "$EXTERNAL_DIR/proot/"
    cp "$TMP_PROOT/COPYING" "$EXTERNAL_DIR/proot/" 2>/dev/null || true
    # Patch missing header in ashmem_memfd.c if needed
    if ! grep -q '<string.h>' "$EXTERNAL_DIR/proot/extension/ashmem_memfd/ashmem_memfd.c"; then
        sed -i.bak '1s/^/#include <string.h>\n/' "$EXTERNAL_DIR/proot/extension/ashmem_memfd/ashmem_memfd.c"
        rm -f "$EXTERNAL_DIR/proot/extension/ashmem_memfd/ashmem_memfd.c.bak"
    fi
    rm -rf "$TMP_PROOT"

    echo "✅ Upstream sources fetched successfully."
    shift || true
fi

# -------------------------------------------------------------
# 2. Locate Android NDK
# -------------------------------------------------------------
NDK_DIR=""

# Check argument passed directly (e.g. from F-Droid $$NDK$$)
if [ -n "$1" ] && [ -d "$1" ]; then
    NDK_DIR="$1"
fi

# Check environment variables
if [ -z "$NDK_DIR" ] && [ -n "$ANDROID_NDK_HOME" ] && [ -d "$ANDROID_NDK_HOME" ]; then
    NDK_DIR="$ANDROID_NDK_HOME"
fi

if [ -z "$NDK_DIR" ] && [ -n "$ANDROID_NDK_ROOT" ] && [ -d "$ANDROID_NDK_ROOT" ]; then
    NDK_DIR="$ANDROID_NDK_ROOT"
fi

if [ -z "$NDK_DIR" ] && [ -n "$ANDROID_NDK" ] && [ -d "$ANDROID_NDK" ]; then
    NDK_DIR="$ANDROID_NDK"
fi

# Check local.properties
if [ -z "$NDK_DIR" ] && [ -f "$PROJECT_ROOT/local.properties" ]; then
    PROP_NDK=$(grep -E '^ndk\.dir=' "$PROJECT_ROOT/local.properties" | cut -d= -f2- | tr -d '\r' || true)
    if [ -n "$PROP_NDK" ] && [ -d "$PROP_NDK" ]; then
        NDK_DIR="$PROP_NDK"
    fi

    if [ -z "$NDK_DIR" ]; then
        PROP_SDK=$(grep -E '^sdk\.dir=' "$PROJECT_ROOT/local.properties" | cut -d= -f2- | tr -d '\r' || true)
        if [ -n "$PROP_SDK" ] && [ -d "$PROP_SDK/ndk" ]; then
            LATEST_NDK=$(find "$PROP_SDK/ndk" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)
            if [ -n "$LATEST_NDK" ] && [ -d "$LATEST_NDK" ]; then
                NDK_DIR="$LATEST_NDK"
            fi
        fi
    fi
fi

# Standard system paths
if [ -z "$NDK_DIR" ]; then
    CANDIDATES=(
        "$HOME/Library/Android/sdk/ndk"
        "$HOME/Android/Sdk/ndk"
        "/opt/android-sdk/ndk"
        "/opt/android-ndk"
    )
    for c in "${CANDIDATES[@]}"; do
        if [ -d "$c" ]; then
            FOUND=$(find "$c" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)
            if [ -n "$FOUND" ] && [ -d "$FOUND" ]; then
                NDK_DIR="$FOUND"
                break
            fi
        fi
    done
fi

if [ -z "$NDK_DIR" ] || [ ! -d "$NDK_DIR" ]; then
    echo "❌ Error: Android NDK not found. Please set ANDROID_NDK_HOME or pass NDK path as argument." >&2
    exit 1
fi

echo "============================================================"
echo "🛠️  Building LinuxOnAndroid Native Libraries"
echo "============================================================"
echo "📁 Android NDK : $NDK_DIR"
echo "📁 Source Dir  : $EXTERNAL_DIR"
echo "📁 Target Dir  : $JNI_LIBS_DIR"
echo ""

# Find LLVM toolchain bin directory
PREBUILT_HOST_DIR=$(find "$NDK_DIR/toolchains/llvm/prebuilt" -mindepth 1 -maxdepth 1 -type d | head -n 1)
TOOLCHAIN_BIN="$PREBUILT_HOST_DIR/bin"
if [ -z "$PREBUILT_HOST_DIR" ] || [ ! -d "$TOOLCHAIN_BIN" ]; then
    echo "❌ Error: Could not locate LLVM toolchain bin in $NDK_DIR" >&2
    exit 1
fi

# Create tool wrapper directory to ensure readelf, objcopy, objdump, strip are accessible by standard names
WRAPPER_DIR="$BUILD_WORK_DIR/tool_wrappers"
mkdir -p "$WRAPPER_DIR"
ln -sf "$TOOLCHAIN_BIN/llvm-readelf" "$WRAPPER_DIR/readelf"
ln -sf "$TOOLCHAIN_BIN/llvm-objcopy" "$WRAPPER_DIR/objcopy"
ln -sf "$TOOLCHAIN_BIN/llvm-objdump" "$WRAPPER_DIR/objdump"
ln -sf "$TOOLCHAIN_BIN/llvm-strip" "$WRAPPER_DIR/strip"
ln -sf "$TOOLCHAIN_BIN/llvm-ar" "$WRAPPER_DIR/ar"

# Locate GNU make (system make or NDK prebuilt make)
MAKE_BIN=""
if command -v make >/dev/null 2>&1; then
    MAKE_BIN="$(command -v make)"
else
    NDK_MAKE=$(find "$NDK_DIR/prebuilt" -name "make" -type f 2>/dev/null | head -n 1)
    if [ -n "$NDK_MAKE" ] && [ -x "$NDK_MAKE" ]; then
        MAKE_BIN="$NDK_MAKE"
    fi
fi

if [ -n "$MAKE_BIN" ]; then
    ln -sf "$MAKE_BIN" "$WRAPPER_DIR/make"
else
    echo "❌ Error: 'make' not found in PATH or NDK prebuilt directory." >&2
    exit 1
fi

ORIG_PATH="$PATH"
export PATH="$WRAPPER_DIR:$TOOLCHAIN_BIN:$PATH"

STRIP="$TOOLCHAIN_BIN/llvm-strip"
OBJCOPY="$TOOLCHAIN_BIN/llvm-objcopy"
OBJDUMP="$TOOLCHAIN_BIN/llvm-objdump"

ABIS=("arm64-v8a" "armeabi-v7a" "x86_64")

for ABI in "${ABIS[@]}"; do
    echo "------------------------------------------------------------"
    echo "🏗️  Compiling for ABI: $ABI"
    echo "------------------------------------------------------------"

    ABI_WORK_DIR="$BUILD_WORK_DIR/$ABI"
    DEST_DIR="$JNI_LIBS_DIR/$ABI"
    mkdir -p "$ABI_WORK_DIR"
    mkdir -p "$DEST_DIR"

    case "$ABI" in
        "arm64-v8a")
            TARGET_TRIPLE="aarch64-linux-android"
            CC="$TOOLCHAIN_BIN/${TARGET_TRIPLE}${MIN_API}-clang"
            CC_32="$TOOLCHAIN_BIN/armv7a-linux-androideabi${MIN_API}-clang"
            LOADER_TEXT_ADDR="0x2000000000"
            HAS_LOADER32=1
            ;;
        "armeabi-v7a")
            TARGET_TRIPLE="armv7a-linux-androideabi"
            CC="$TOOLCHAIN_BIN/${TARGET_TRIPLE}${MIN_API}-clang"
            CC_32=""
            LOADER_TEXT_ADDR="0x20000000"
            HAS_LOADER32=0
            ;;
        "x86_64")
            TARGET_TRIPLE="x86_64-linux-android"
            CC="$TOOLCHAIN_BIN/${TARGET_TRIPLE}${MIN_API}-clang"
            CC_32="$TOOLCHAIN_BIN/i686-linux-android${MIN_API}-clang"
            LOADER_TEXT_ADDR="0x2000000000"
            HAS_LOADER32=1
            ;;
        *)
            echo "❌ Unknown ABI: $ABI" >&2
            exit 1
            ;;
    esac

    # 1. Build libandroid-shmem.so
    echo " -> Building libandroid-shmem.so..."
    $CC -fPIC -shared -std=c11 -O2 -Wall -Wextra \
        -include fcntl.h -D_PATH_TMP='"/tmp/"' \
        "$EXTERNAL_DIR/libandroid-shmem/shmem.c" \
        -o "$ABI_WORK_DIR/libandroid-shmem.so" \
        -llog -landroid

    # 2. Build libtalloc.so
    echo " -> Building libtalloc.so..."
    $CC -fPIC -shared -O2 -Wall -Wextra \
        -D__STDC_WANT_LIB_EXT1__=1 \
        -I "$EXTERNAL_DIR/talloc" \
        "$EXTERNAL_DIR/talloc/talloc.c" \
        -o "$ABI_WORK_DIR/libtalloc.so"

    # 3. Build Loader (libproot_loader.so)
    echo " -> Building libproot_loader.so..."
    $CC -I "$EXTERNAL_DIR/proot" -fPIC -ffreestanding -c "$EXTERNAL_DIR/proot/loader/loader.c" -o "$ABI_WORK_DIR/loader.o"
    $CC -I "$EXTERNAL_DIR/proot" -fPIC -ffreestanding -c "$EXTERNAL_DIR/proot/loader/assembly.S" -o "$ABI_WORK_DIR/assembly.o"
    $CC -static -nostdlib -Wl,--build-id=none,-Ttext=${LOADER_TEXT_ADDR},--rosegment,-z,noexecstack \
        "$ABI_WORK_DIR/loader.o" "$ABI_WORK_DIR/assembly.o" \
        -o "$ABI_WORK_DIR/libproot_loader.so"

    # 4. Build 32-bit Loader (libproot_loader32.so) if applicable
    if [ "$HAS_LOADER32" -eq 1 ]; then
        echo " -> Building libproot_loader32.so..."
        $CC_32 -I "$EXTERNAL_DIR/proot" -fPIC -ffreestanding -c "$EXTERNAL_DIR/proot/loader/loader.c" -o "$ABI_WORK_DIR/loader32.o"
        $CC_32 -I "$EXTERNAL_DIR/proot" -fPIC -ffreestanding -c "$EXTERNAL_DIR/proot/loader/assembly.S" -o "$ABI_WORK_DIR/assembly32.o"
        $CC_32 -static -nostdlib -Wl,--build-id=none,-Ttext=0x20000000,--rosegment,-z,noexecstack \
            "$ABI_WORK_DIR/loader32.o" "$ABI_WORK_DIR/assembly32.o" \
            -o "$ABI_WORK_DIR/libproot_loader32.so"
    fi

    # 5. Build proot -> libproot.so
    echo " -> Building libproot.so..."
    PROOT_SRC_COPY="$ABI_WORK_DIR/proot_src"
    rm -rf "$PROOT_SRC_COPY"
    mkdir -p "$PROOT_SRC_COPY"
    cp -R "$EXTERNAL_DIR/proot/." "$PROOT_SRC_COPY/"

    # Ensure clean state in copy
    find "$PROOT_SRC_COPY" -name "*.o" -o -name "*.d" -o -name "proot" -delete

    # Ensure sys/shm.h exists in libandroid-shmem for sysvipc_shm.c
    mkdir -p "$EXTERNAL_DIR/libandroid-shmem/sys"
    ln -sf ../shm.h "$EXTERNAL_DIR/libandroid-shmem/sys/shm.h"

    "$MAKE_BIN" -C "$PROOT_SRC_COPY" clean >/dev/null 2>&1 || true
    "$MAKE_BIN" -C "$PROOT_SRC_COPY" -j$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 4) \
        CC="$CC" \
        STRIP="$STRIP" \
        OBJCOPY="$OBJCOPY" \
        OBJDUMP="$OBJDUMP" \
        CPPFLAGS="-D_FILE_OFFSET_BITS=64 -D_GNU_SOURCE -I. -I./ -DPROOT_UNBUNDLE_LOADER='\"../libexec/proot\"' -DWITH_LIBANDROID_SHMEM -I$EXTERNAL_DIR/talloc -I$EXTERNAL_DIR/libandroid-shmem" \
        LDFLAGS="-L$ABI_WORK_DIR -ltalloc -landroid-shmem -Wl,-z,noexecstack" \
        PROOT_WITH_LIBANDROID_SHMEM=true \
        PROOT_UNBUNDLE_LOADER="../libexec/proot"

    cp "$PROOT_SRC_COPY/proot" "$ABI_WORK_DIR/libproot.so"

    # 6. Strip and copy to app/src/main/jniLibs/<abi>/
    echo " -> Stripping and installing binaries to $DEST_DIR..."
    TARGET_FILES=(
        "libandroid-shmem.so"
        "libtalloc.so"
        "libproot.so"
        "libproot_loader.so"
    )
    if [ "$HAS_LOADER32" -eq 1 ]; then
        TARGET_FILES+=("libproot_loader32.so")
    else
        # Remove any stale loader32 if it exists in 32-bit dir
        rm -f "$DEST_DIR/libproot_loader32.so"
    fi

    for f in "${TARGET_FILES[@]}"; do
        if [ ! -f "$ABI_WORK_DIR/$f" ]; then
            echo "❌ Error: Expected binary $ABI_WORK_DIR/$f was not created!" >&2
            exit 1
        fi
        $STRIP --strip-unneeded -o "$DEST_DIR/$f" "$ABI_WORK_DIR/$f"
        SIZE=$(ls -lh "$DEST_DIR/$f" | awk '{print $5}')
        echo "    📦 $f ($SIZE)"
    done

    echo "✅ $ABI compiled successfully."
    echo ""
done

# Cleanup temporary work directory
rm -rf "$BUILD_WORK_DIR"
export PATH="$ORIG_PATH"

echo "============================================================"
echo "🎉 Native Libraries Built and Installed Successfully!"
echo "============================================================"
ls -la "$JNI_LIBS_DIR"/*
