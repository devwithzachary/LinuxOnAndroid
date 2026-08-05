#!/usr/bin/env bash
# ==============================================================================
# LinuxOnAndroid 1-Click Package Automated Test Suite
# ==============================================================================
# Usage: ./scripts/test_1click_packages.sh [--install] [DEVICE_ID]
# Automates testing and verification of all 1-Click software packages
# ==============================================================================

set -eo pipefail

PACKAGE_NAME="com.devwithzachary.completelinuxinstaller"
DO_INSTALL=false
DEVICE_ID=""

for arg in "$@"; do
    if [ "$arg" == "--install" ]; then
        DO_INSTALL=true
    elif [ -z "$DEVICE_ID" ]; then
        DEVICE_ID="$arg"
    fi
done

if [ -z "$DEVICE_ID" ]; then
    DEVICE_ID="$(adb devices | grep -v "List" | grep "device" | head -n1 | awk '{print $1}')"
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

if [ -z "$DEVICE_ID" ]; then
    echo -e "${RED}ERROR: No ADB device detected. Please connect an Android device or start an emulator.${NC}"
    exit 1
fi

echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}  LinuxOnAndroid 1-Click Package Automated Test Suite${NC}"
echo -e "${BLUE}=====================================================${NC}"
echo -e "Device Target : ${YELLOW}$DEVICE_ID${NC}"
echo -e "Target Package: ${YELLOW}$PACKAGE_NAME${NC}"
echo -e "Auto Install  : ${YELLOW}$DO_INSTALL${NC}"
echo ""

# Find installed app lib path
APP_PATH=$(adb -s "$DEVICE_ID" shell pm path "$PACKAGE_NAME" | head -n1 | cut -d: -f2 | tr -d '\r\n')
if [ -z "$APP_PATH" ]; then
    echo -e "${YELLOW}App $PACKAGE_NAME not installed. Building and deploying debug APK...${NC}"
    ./gradlew assembleDebug
    adb -s "$DEVICE_ID" install -r app/build/outputs/apk/debug/app-debug.apk
    APP_PATH=$(adb -s "$DEVICE_ID" shell pm path "$PACKAGE_NAME" | head -n1 | cut -d: -f2 | tr -d '\r\n')
fi

LIB_DIR="$(dirname "$APP_PATH")/lib/arm64"
ROOTFS_DIR="/data/data/$PACKAGE_NAME/files/ubuntu_rootfs"

echo -e "Lib Directory : ${YELLOW}$LIB_DIR${NC}"
echo -e "RootFS Target : ${YELLOW}$ROOTFS_DIR${NC}"
echo ""

NONINT_EXPORT="export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; mkdir -p /usr/sbin /etc /var/lib/dbus 2>/dev/null; (grep -q ^messagebus: /etc/group || echo \"messagebus:x:101:\" >> /etc/group); (grep -q ^messagebus: /etc/passwd || echo \"messagebus:x:101:101:D-Bus Message System Daemon:/nonexistent:/bin/false\" >> /etc/passwd); (grep -q ^messagebus: /etc/shadow || echo \"messagebus:*:19700:0:99999:7:::\" >> /etc/shadow); (grep -q ^www-data: /etc/group || echo \"www-data:x:33:\" >> /etc/group); (grep -q ^www-data: /etc/passwd || echo \"www-data:x:33:33:www-data:/var/www:/usr/sbin/nologin\" >> /etc/passwd); (grep -q ^sshd: /etc/group || echo \"sshd:x:102:\" >> /etc/group); (grep -q ^sshd: /etc/passwd || echo \"sshd:x:102:102:Privilege-separated SSH:/run/sshd:/usr/sbin/nologin\" >> /etc/passwd); printf '#!/bin/sh\nexit 101\n' > /usr/sbin/policy-rc.d && chmod 755 /usr/sbin/policy-rc.d; if [ ! -f /bin/systemctl ] && [ ! -f /usr/bin/systemctl ]; then printf '#!/bin/sh\nexit 0\n' > /usr/bin/systemctl && chmod 755 /usr/bin/systemctl; fi; dbus-uuidgen --ensure 2>/dev/null || true; chmod 755 /usr /usr/local /usr/local/bin /usr/local/sbin /usr/bin /usr/sbin /bin /sbin /etc 2>/dev/null; chmod -R 777 /var/lib/dpkg /var/cache /tmp /var/tmp /.l2s 2>/dev/null; rm -rf /var/lib/dpkg/*-old /var/lib/dpkg/*-new /var/lib/dpkg/lock* /usr/bin/*.dpkg-new /usr/lib/*.dpkg-new 2>/dev/null; mkdir -p /etc/dpkg/dpkg.cfg.d && echo force-all > /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-unsafe-io >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-overwrite >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-confold >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-confdef >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid && echo force-depends >> /etc/dpkg/dpkg.cfg.d/00-linuxonandroid; mkdir -p /etc/apt/apt.conf.d && echo 'APT::Sandbox::User \"root\";' > /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::Pipeline-Depth \"0\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::http::No-Cache \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::PDiffs \"false\";' >> /etc/apt/apt.conf.d/99linuxonandroid && echo 'Acquire::ForceIPv4 \"true\";' >> /etc/apt/apt.conf.d/99linuxonandroid; export TMPDIR=/tmp && export TMP=/tmp && export DEBIAN_FRONTEND=noninteractive && export DEBIAN_PRIORITY=critical && export UCF_FORCE_CONFFOLD=1 && export NEEDRESTART_MODE=a"
DPKG_FLAGS="-o Dpkg::Options::=\"--force-all\" -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Use-Pty=0 -o APT::Sandbox::User=root -o Acquire::http::Pipeline-Depth=0 -o Acquire::PDiffs=false"

declare -a PKG_IDS=("xfce_desktop" "python_dev" "node_dev" "android_dev" "nginx_web" "openssh_server")
declare -a PKG_NAMES=("XFCE4 Desktop" "Python 3 Stack" "Node.js Stack" "Android Dev Tools" "NGINX Web Server" "OpenSSH Server")
declare -a PKG_CMDS=(
    "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS xfce4 xfce4-terminal dbus-x11 tigervnc-standalone-server tigervnc-xorg-extension tightvncserver novnc websockify curl ca-certificates && mkdir -p /root/.vnc && echo '#!/bin/sh\nunset SESSION_MANAGER\nunset DBUS_SESSION_BUS_ADDRESS\nexec startxfce4' > /root/.vnc/xstartup && chmod +x /root/.vnc/xstartup"
    "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS python3 python3-pip python3-venv git build-essential neovim curl wget ca-certificates"
    "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS nodejs npm yarnpkg git build-essential neovim curl wget ca-certificates"
    "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS openjdk-17-jdk-headless android-sdk-platform-tools gradle git curl wget unzip ca-certificates"
    "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS nginx sqlite3 curl ca-certificates"
    "$NONINT_EXPORT && dpkg --configure -a && apt-get update $DPKG_FLAGS && apt-get install -y $DPKG_FLAGS openssh-server && mkdir -p /run/sshd /etc/ssh/sshd_config.d && [ -f /etc/ssh/ssh_host_rsa_key ] || ssh-keygen -t rsa -f /etc/ssh/ssh_host_rsa_key -N \"\" && [ -f /etc/ssh/ssh_host_ecdsa_key ] || ssh-keygen -t ecdsa -f /etc/ssh/ssh_host_ecdsa_key -N \"\" && [ -f /etc/ssh/ssh_host_ed25519_key ] || ssh-keygen -t ed25519 -f /etc/ssh/ssh_host_ed25519_key -N \"\" && ssh-keygen -A 2>/dev/null || true && echo \"PermitRootLogin yes\" > /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"PasswordAuthentication yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"KbdInteractiveAuthentication yes\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"StrictModes no\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && echo \"Port 2222\" >> /etc/ssh/sshd_config.d/00-linuxonandroid.conf && chmod 600 /etc/ssh/ssh_host_*_key 2>/dev/null || true && chmod 755 /etc/ssh /run/sshd 2>/dev/null || true"
)
declare -a PKG_BINS=(
    "usr/bin/startxfce4 usr/bin/vncserver"
    "usr/bin/python3 usr/bin/pip3 usr/bin/git usr/bin/gcc usr/bin/nvim"
    "usr/bin/node usr/bin/npm usr/bin/git usr/bin/gcc usr/bin/nvim"
    "usr/bin/java usr/bin/adb usr/bin/gradle usr/bin/git"
    "usr/sbin/nginx usr/bin/sqlite3"
    "usr/sbin/sshd usr/bin/ssh-keygen"
)

TOTAL_COUNT=${#PKG_IDS[@]}
PASSED_COUNT=0
FAILED_COUNT=0

printf "%-18s | %-22s | %-10s | %s\n" "PACKAGE ID" "PACKAGE NAME" "STATUS" "EXPECTED BINARY VERIFICATION"
echo "---------------------------------------------------------------------------------------------------"

for i in "${!PKG_IDS[@]}"; do
    ID="${PKG_IDS[$i]}"
    NAME="${PKG_NAMES[$i]}"
    CMD="${PKG_CMDS[$i]}"
    BINS="${PKG_BINS[$i]}"

    if [ "$DO_INSTALL" = true ]; then
        echo -e "${BLUE}[+] Installing package $NAME ($ID)...${NC}"
        adb -s "$DEVICE_ID" shell run-as "$PACKAGE_NAME" /system/bin/sh -c \
            "LD_LIBRARY_PATH=$LIB_DIR PROOT_LOADER=$LIB_DIR/libproot_loader.so PROOT_TMP_DIR=/data/data/$PACKAGE_NAME/files/tmp PROOT_NO_SECCOMP=1 $LIB_DIR/libproot.so -0 -r $ROOTFS_DIR -b /data/data/$PACKAGE_NAME/files/fake_proc/stat:/proc/stat -b /proc -b /sys -b /dev /bin/bash -c \"$CMD\"" >/dev/null 2>&1 || true
    fi

    MISSING_BINS=()
    FOR_BINS=($BINS)
    for BIN in "${FOR_BINS[@]}"; do
        CHECK=$(adb -s "$DEVICE_ID" shell run-as "$PACKAGE_NAME" test -f "$ROOTFS_DIR/$BIN" && echo "YES" || echo "NO")
        if [ "$CHECK" != "YES" ]; then
            MISSING_BINS+=("$BIN")
        fi
    done

    if [ ${#MISSING_BINS[@]} -eq 0 ]; then
        STATUS_STR="${GREEN}PASS${NC}"
        BIN_STATUS="${GREEN}All ${#FOR_BINS[@]} binaries verified present${NC}"
        ((PASSED_COUNT++))
    else
        STATUS_STR="${RED}FAIL${NC}"
        BIN_STATUS="${RED}Missing: ${MISSING_BINS[*]}${NC}"
        ((FAILED_COUNT++))
    fi

    printf "%-18s | %-22s | %-19s | %b\n" "$ID" "$NAME" "$STATUS_STR" "$BIN_STATUS"
done

echo "---------------------------------------------------------------------------------------------------"
echo -e "Test Results Summary: Total: $TOTAL_COUNT | Passed: ${GREEN}$PASSED_COUNT${NC} | Failed: ${RED}$FAILED_COUNT${NC}"

if [ $FAILED_COUNT -gt 0 ]; then
    exit 1
fi
