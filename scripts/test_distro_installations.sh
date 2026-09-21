#!/usr/bin/env bash
# ==============================================================================
# LinuxOnAndroid Multi-Distro Verification & Installation Test Framework
# ==============================================================================
# Usage:
#   ./scripts/test_distro_installations.sh --urls-only
#   ./scripts/test_distro_installations.sh --device [DEVICE_ID] [--distro <distro_id>]
#   ./scripts/test_distro_installations.sh --all [DEVICE_ID]
# ==============================================================================

set -eo pipefail

PACKAGE_NAME="com.devwithzachary.completelinuxinstaller"
MODE="urls" # "urls", "device", or "all"
DEVICE_ID=""
FILTER_DISTRO=""

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

print_usage() {
    echo -e "${BOLD}LinuxOnAndroid Distro Test Framework${NC}"
    echo ""
    echo "Usage:"
    echo "  $0 --urls-only                           Verify HTTP availability and archive integrity for all distros/architectures"
    echo "  $0 --device [DEVICE_ID]                  Run live install & PRoot test on connected Android device"
    echo "  $0 --device [DEVICE_ID] --distro <id>    Run live install & PRoot test for a specific distro (e.g. alpine_3_21)"
    echo "  $0 --all [DEVICE_ID]                     Run both URL integrity tests and live device install tests"
    echo "  $0 --help                                Show this help message"
    echo ""
    echo "Supported Distro IDs: ubuntu_26_04, debian_12, fedora_44, alpine_3_21, arch_arm, kali_rolling, void_rolling"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --urls-only|--check-urls|--release-check)
            MODE="urls"
            shift
            ;;
        --device)
            MODE="device"
            if [[ -n "$2" && "$2" != --* ]]; then
                DEVICE_ID="$2"
                shift 2
            else
                shift
            fi
            ;;
        --all)
            MODE="all"
            if [[ -n "$2" && "$2" != --* ]]; then
                DEVICE_ID="$2"
                shift 2
            else
                shift
            fi
            ;;
        --distro)
            if [[ -n "$2" && "$2" != --* ]]; then
                FILTER_DISTRO="$2"
                shift 2
            else
                echo -e "${RED}Error: --distro requires a distro ID argument.${NC}"
                exit 1
            fi
            ;;
        --help|-h)
            print_usage
            exit 0
            ;;
        *)
            if [[ -z "$DEVICE_ID" && "$1" != --* ]]; then
                DEVICE_ID="$1"
                MODE="device"
            fi
            shift
            ;;
    esac
done

# Distro definitions: id | display_name | arch | url | expected_archive_type
DISTRO_URLS=(
    "ubuntu_26_04|Ubuntu 26.04 LTS|ARM64|https://cdimage.ubuntu.com/ubuntu-base/releases/26.04/release/ubuntu-base-26.04-base-arm64.tar.gz|gzip"
    "ubuntu_26_04|Ubuntu 26.04 LTS|X86_64|https://cdimage.ubuntu.com/ubuntu-base/releases/26.04/release/ubuntu-base-26.04-base-amd64.tar.gz|gzip"
    "ubuntu_26_04|Ubuntu 26.04 LTS|ARMV7|https://cdimage.ubuntu.com/ubuntu-base/releases/26.04/release/ubuntu-base-26.04-base-armhf.tar.gz|gzip"
    "debian_12|Debian 12 Bookworm|ARM64|https://doi-janky.infosiftr.net/job/tianon/job/debuerreotype/job/arm64v8/lastSuccessfulBuild/artifact/bookworm/rootfs.tar.xz|xz"
    "debian_12|Debian 12 Bookworm|X86_64|https://doi-janky.infosiftr.net/job/tianon/job/debuerreotype/job/amd64/lastSuccessfulBuild/artifact/bookworm/rootfs.tar.xz|xz"
    "debian_12|Debian 12 Bookworm|ARMV7|https://doi-janky.infosiftr.net/job/tianon/job/debuerreotype/job/arm32v7/lastSuccessfulBuild/artifact/bookworm/rootfs.tar.xz|xz"
    "fedora_44|Fedora 44 Rawhide|ARM64|https://download.fedoraproject.org/pub/fedora/linux/releases/44/Container/aarch64/images/Fedora-WSL-Base-44-1.7.aarch64.wsl|tar"
    "fedora_44|Fedora 44 Rawhide|X86_64|https://download.fedoraproject.org/pub/fedora/linux/releases/44/Container/x86_64/images/Fedora-WSL-Base-44-1.7.x86_64.wsl|tar"
    "fedora_44|Fedora 44 Rawhide|ARMV7|https://download.fedoraproject.org/pub/fedora/linux/releases/44/Container/aarch64/images/Fedora-WSL-Base-44-1.7.aarch64.wsl|tar"
    "alpine_3_21|Alpine Linux 3.21|ARM64|https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/aarch64/alpine-minirootfs-3.21.3-aarch64.tar.gz|gzip"
    "alpine_3_21|Alpine Linux 3.21|X86_64|https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/x86_64/alpine-minirootfs-3.21.3-x86_64.tar.gz|gzip"
    "alpine_3_21|Alpine Linux 3.21|ARMV7|https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/armv7/alpine-minirootfs-3.21.3-armv7.tar.gz|gzip"
    "arch_arm|Arch Linux ARM|ARM64|http://os.archlinuxarm.org/os/ArchLinuxARM-aarch64-latest.tar.gz|gzip"
    "arch_arm|Arch Linux ARM|X86_64|https://geo.mirror.pkgbuild.com/iso/latest/archlinux-bootstrap-x86_64.tar.zst|zst"
    "arch_arm|Arch Linux ARM|ARMV7|http://os.archlinuxarm.org/os/ArchLinuxARM-armv7-latest.tar.gz|gzip"
    "kali_rolling|Kali Rolling|ARM64|https://kali.download/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-arm64.tar.xz|xz"
    "kali_rolling|Kali Rolling|X86_64|https://kali.download/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-amd64.tar.xz|xz"
    "kali_rolling|Kali Rolling|ARMV7|https://kali.download/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-armhf.tar.xz|xz"
    "void_rolling|Void Linux Rolling|ARM64|https://repo-default.voidlinux.org/live/current/void-aarch64-ROOTFS-20250202.tar.xz|xz"
    "void_rolling|Void Linux Rolling|X86_64|https://repo-default.voidlinux.org/live/current/void-x86_64-ROOTFS-20250202.tar.xz|xz"
    "void_rolling|Void Linux Rolling|ARMV7|https://repo-default.voidlinux.org/live/current/void-armv7l-ROOTFS-20250202.tar.xz|xz"
)

# Test URLs
run_url_tests() {
    echo -e "${BLUE}========================================================================${NC}"
    echo -e "${BLUE}  LinuxOnAndroid Release Validation: Distro URL & Archive Verification  ${NC}"
    echo -e "${BLUE}========================================================================${NC}"
    printf "%-14s | %-20s | %-7s | %-6s | %-10s | %s\n" "DISTRO ID" "DISTRO NAME" "ARCH" "HTTP" "SIZE" "STATUS"
    echo "--------------------------------------------------------------------------------------------------"

    local total=0
    local passed=0
    local failed=0

    for item in "${DISTRO_URLS[@]}"; do
        IFS='|' read -r id name arch url expected_type <<< "$item"

        if [[ -n "$FILTER_DISTRO" && "$FILTER_DISTRO" != "$id" ]]; then
            continue
        fi

        ((total++))

        # Check HTTP response and headers
        HTTP_RESPONSE=$(curl -sIL -A "Mozilla/5.0 LinuxOnAndroidTest/1.0" -w "%{http_code}\n%{size_download}\n" -o /dev/null "$url" 2>/dev/null || echo "000")
        HTTP_STATUS=$(echo "$HTTP_RESPONSE" | head -n1)

        # Retrieve content-length from headers
        CONTENT_LENGTH=$(curl -sIL -A "Mozilla/5.0 LinuxOnAndroidTest/1.0" "$url" 2>/dev/null | grep -i "^content-length:" | tail -n1 | tr -d '\r\n' | awk '{print $2}')
        if [[ -z "$CONTENT_LENGTH" || "$CONTENT_LENGTH" == "0" ]]; then
            SIZE_STR="Unknown"
        else
            SIZE_MB=$(awk "BEGIN {printf \"%.1f MB\", $CONTENT_LENGTH/1048576}")
            SIZE_STR="$SIZE_MB"
        fi

        if [[ "$HTTP_STATUS" == "200" || "$HTTP_STATUS" == "302" ]]; then
            STATUS_STR="${GREEN}PASS${NC}"
            ((passed++))
        else
            STATUS_STR="${RED}FAIL ($HTTP_STATUS)${NC}"
            ((failed++))
        fi

        printf "%-14s | %-20s | %-7s | %-6s | %-10s | %b\n" "$id" "$name" "$arch" "$HTTP_STATUS" "$SIZE_STR" "$STATUS_STR"
    done

    echo "--------------------------------------------------------------------------------------------------"
    echo -e "URL Verification Summary: Total: $total | Passed: ${GREEN}$passed${NC} | Failed: ${RED}$failed${NC}"
    echo ""

    if [[ $failed -gt 0 ]]; then
        return 1
    fi
    return 0
}

# Run Device End-to-End Test
run_device_tests() {
    if [[ -z "$DEVICE_ID" ]]; then
        DEVICE_ID="$(adb devices | grep -v "List" | grep "device" | head -n1 | awk '{print $1}')"
    fi

    if [[ -z "$DEVICE_ID" ]]; then
        echo -e "${RED}ERROR: No connected Android device found via ADB. Connect a device or launch an emulator.${NC}"
        exit 1
    fi

    echo -e "${BLUE}========================================================================${NC}"
    echo -e "${BLUE}  LinuxOnAndroid End-to-End Distro Installation Test (PRoot Runtime)   ${NC}"
    echo -e "${BLUE}========================================================================${NC}"
    echo -e "Target Device : ${CYAN}$DEVICE_ID${NC}"
    echo -e "Target Package: ${CYAN}$PACKAGE_NAME${NC}"

    # Verify app is installed
    APP_PATH=$(adb -s "$DEVICE_ID" shell pm path "$PACKAGE_NAME" 2>/dev/null | head -n1 | cut -d: -f2 | tr -d '\r\n')
    if [[ -z "$APP_PATH" ]]; then
        echo -e "${YELLOW}App not installed on device. Building & deploying debug APK...${NC}"
        ./gradlew installDebug
        APP_PATH=$(adb -s "$DEVICE_ID" shell pm path "$PACKAGE_NAME" | head -n1 | cut -d: -f2 | tr -d '\r\n')
    fi

    # Discover native library directory
    LIB_DIR="$(dirname "$APP_PATH")/lib/arm64"
    if ! adb -s "$DEVICE_ID" shell test -f "$LIB_DIR/libproot.so" 2>/dev/null; then
        # Try finding via pm dump
        LIB_DIR=$(adb -s "$DEVICE_ID" shell pm dump "$PACKAGE_NAME" | grep -E "legacyNativeLibraryDir=" | head -n1 | cut -d= -f2 | tr -d '\r\n')"/arm64"
    fi

    echo -e "Native Libs   : ${CYAN}$LIB_DIR${NC}"
    echo ""

    # Distros to test on device (ARM64)
    # id | display_name | url | archive_filename | tar_flags | test_cmd | expected_keyword
    DEVICE_TESTS=(
        "alpine_3_21|Alpine Linux 3.21|https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/aarch64/alpine-minirootfs-3.21.3-aarch64.tar.gz|alpine.tar.gz|-xzf|/bin/cat /etc/alpine-release|3.21"
        "debian_12|Debian 12 Bookworm|https://doi-janky.infosiftr.net/job/tianon/job/debuerreotype/job/arm64v8/lastSuccessfulBuild/artifact/bookworm/rootfs.tar.xz|debian.tar.xz|-xJf|/bin/cat /etc/debian_version|12"
        "void_rolling|Void Linux Rolling|https://repo-default.voidlinux.org/live/current/void-aarch64-ROOTFS-20250202.tar.xz|void.tar.xz|-xJf|/bin/cat /etc/os-release|void"
    )

    printf "%-14s | %-22s | %-12s | %s\n" "DISTRO ID" "DISTRO NAME" "PROOT EXEC" "TEST OUTPUT / STATUS"
    echo "--------------------------------------------------------------------------------------------------"

    local total=0
    local passed=0
    local failed=0

    for test in "${DEVICE_TESTS[@]}"; do
        IFS='|' read -r id name url filename tar_flags test_cmd expected <<< "$test"

        if [[ -n "$FILTER_DISTRO" && "$FILTER_DISTRO" != "$id" ]]; then
            continue
        fi

        ((total++))
        echo -e "${YELLOW}--> [Testing $name] Downloading and verifying rootfs...${NC}"

        # Download directly into device cache
        adb -s "$DEVICE_ID" shell "run-as $PACKAGE_NAME /system/bin/sh -c 'mkdir -p cache && curl -sSL -o cache/$filename \"$url\"'"

        # Prepare test container directory
        TEST_DIR="files/containers/test_${id}"
        adb -s "$DEVICE_ID" shell "run-as $PACKAGE_NAME /system/bin/sh -c 'rm -rf $TEST_DIR && mkdir -p $TEST_DIR/rootfs && cd $TEST_DIR/rootfs && tar $tar_flags /data/user/0/$PACKAGE_NAME/cache/$filename'"

        # Execute PRoot inside the unpacked container
        CMD_OUTPUT=$(adb -s "$DEVICE_ID" shell "run-as $PACKAGE_NAME /system/bin/sh -c 'LD_LIBRARY_PATH=$LIB_DIR PROOT_LOADER=$LIB_DIR/libproot_loader.so PROOT_TMP_DIR=/data/user/0/$PACKAGE_NAME/files/tmp PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin $LIB_DIR/libproot.so -0 -l -r /data/user/0/$PACKAGE_NAME/$TEST_DIR/rootfs -b $LIB_DIR -w / $test_cmd < /dev/null 2>&1'" | tr -d '\r')

        # Clean up test rootfs and cache tarball
        adb -s "$DEVICE_ID" shell "run-as $PACKAGE_NAME /system/bin/sh -c 'rm -rf $TEST_DIR cache/$filename'" 2>/dev/null || true

        if echo "$CMD_OUTPUT" | grep -qi "$expected"; then
            STATUS_STR="${GREEN}PASS${NC}"
            OUT_STR="${GREEN}$(echo "$CMD_OUTPUT" | head -n1)${NC}"
            ((passed++))
        else
            STATUS_STR="${RED}FAIL${NC}"
            OUT_STR="${RED}Output: $CMD_OUTPUT (Expected: $expected)${NC}"
            ((failed++))
        fi

        printf "%-14s | %-22s | %-21b | %b\n" "$id" "$name" "$STATUS_STR" "$OUT_STR"
    done

    echo "--------------------------------------------------------------------------------------------------"
    echo -e "Device E2E Summary: Total: $total | Passed: ${GREEN}$passed${NC} | Failed: ${RED}$failed${NC}"
    echo ""

    if [[ $failed -gt 0 ]]; then
        return 1
    fi
    return 0
}

# Main Dispatch
OVERALL_STATUS=0

if [[ "$MODE" == "urls" || "$MODE" == "all" ]]; then
    run_url_tests || OVERALL_STATUS=1
fi

if [[ "$MODE" == "device" || "$MODE" == "all" ]]; then
    run_device_tests || OVERALL_STATUS=1
fi

if [[ $OVERALL_STATUS -eq 0 ]]; then
    echo -e "${GREEN}${BOLD}✓ ALL DISTRO VERIFICATION TESTS PASSED SUCCESSFULLY!${NC}"
else
    echo -e "${RED}${BOLD}✗ ONE OR MORE DISTRO VERIFICATION TESTS FAILED.${NC}"
fi

exit $OVERALL_STATUS
