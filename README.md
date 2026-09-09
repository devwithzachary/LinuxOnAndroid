# Complete Linux Installer (Linux on Android)

[![Google Play Store](https://img.shields.io/badge/Google%20Play-Download-brightgreen.svg?logo=googleplay)](https://play.google.com/store/apps/details?id=com.devwithzachary.completelinuxinstaller)
[![Google Play Open Testing](https://img.shields.io/badge/Google%20Play-Open%20Beta-blue.svg?logo=googleplay)](https://play.google.com/apps/testing/com.devwithzachary.completelinuxinstaller)
[![Discord Community](https://img.shields.io/badge/Discord-Join%20Community-5865F2.svg?logo=discord&logoColor=white)](https://discord.gg/vJbBagx8JA)
[![Android MinSDK](https://img.shields.io/badge/Min%20SDK-23%20%28Android%206.0%2B%29-brightgreen.svg)](https://developer.android.com/about/versions/marshmallow)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![UI Framework](https://img.shields.io/badge/UI-Jetpack%20Compose%20Material3-purple.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-ARM64%20%7C%20x86__64%20%7C%20ARMv7-orange.svg)](#multi-architecture-support)
[![License](https://img.shields.io/badge/License-GPL--2.0--or--later-blue.svg)](LICENSE)

**Complete Linux Installer** is an open-source Android application designed to download, provision, and run full-featured Linux distributions (including Ubuntu 26.04 LTS, Debian 12, Alpine Linux, Arch Linux ARM, Kali Linux, and Void Linux) natively on Android devices **without requiring root permissions**.

> [!TIP]
> 📲 **Now Live on the Google Play Store!**  
> **[Download on Google Play](https://play.google.com/store/apps/details?id=com.devwithzachary.completelinuxinstaller)** | **[Join Beta Testing Track](https://play.google.com/apps/testing/com.devwithzachary.completelinuxinstaller)**

Powered by a native **PRoot** user-space engine, a multi-session **POSIX PTY terminal bridge**, and a modern **Jetpack Compose Material 3** interface, Complete Linux Installer transforms your Android phone or tablet into a portable Linux workstation, server host, desktop environment, and development platform.

> [!NOTE]
> **Active Development & Community Support**: Complete Linux Installer is actively developed and maintained. Join our [Discord Community](https://discord.gg/vJbBagx8JA) for announcements and discussion, or [submit an issue on GitHub](https://github.com/devwithzachary/LinuxOnAndroid/issues) if you discover any bugs!

---

## 🚀 Key Features

* **🔒 100% Rootless & Completely Isolated**: Runs entirely in Android user-space using PRoot ptrace system call interception. Zero root access, unlocked bootloaders, or Android system partition modifications required. Safe and sandboxed without touching your personal files or host OS.
* **🐧 Multi-Distribution Linux Catalog**: Install and switch between 6 distinct Linux distributions tailored for different performance profiles and use cases:
  * **Ubuntu 26.04 LTS**: Official base LTS environment with APT package management.
  * **Debian 12**: Ultra-stable lightweight alternative with vast software repositories.
  * **Alpine Linux 3.21**: Minimalist musl/busybox environment (~10MB rootfs) with instant boot times and tiny memory footprint.
  * **Arch Linux ARM**: Bleeding-edge rolling release environment powered by the `pacman` package manager.
  * **Kali Linux CLI Tools**: Specialized security auditing and penetration testing tools environment.
  * **Void Linux**: Independent general-purpose distribution with the blazing-fast `xbps` package manager.
* **📑 Concurrent Multi-Tab Terminal**: Run, switch, and manage multiple independent interactive terminal sessions simultaneously with isolated PTY subprocesses, custom tab titles, session indicators, and container-specific terminal sessions.
* **🎛️ Installed Containers Dashboard**: Manage multiple Linux distributions installed side-by-side on disk. Includes per-container storage tracking, live RAM and storage gauge dials, isolated process monitoring (`ps aux`), open TCP port listeners with 1-tap browser launcher, and 1-touch service quick-launchers (VNC, NGINX, SSH).
* **🖥️ Full Graphical Desktop Access (GUI)**: One-click installation of a complete **XFCE4 Desktop Environment** with TigerVNC support for full windowed desktop access directly on your phone, tablet, or external display.
* **🛠️ 1-Click Software Hub & Multi-Package Managers**: Tailored software presets and native package manager integration (`apt`, `apk`, `pacman`, `xbps`):
  * **Desktop Environments**: XFCE4 Desktop, XFCE Terminal, TigerVNC Server.
  * **Web & Database**: NGINX HTTP web server + SQLite3.
  * **Remote Access**: OpenSSH Server daemon for remote terminal logins from PC or laptop over LAN.
  * **Development Stacks**: Python 3 (pip, venv, GCC), Node.js (npm, Yarn, Neovim), and Android Developer Tools (OpenJDK 17, ADB, Gradle).
* **👋 First-Launch Onboarding & Welcome Guide**: Integrated introduction screen detailing container architecture, sandboxing safety, and multi-container capabilities before entering the setup wizard.
* **🗑️ Safe Container Deletion Feedback**: Dedicated full-screen deletion view with animated feedback, real-time step progress, and back-gesture protection during storage purging.
* **🛡️ Background Execution & WakeLock**: Android Foreground Service with CPU WakeLock keeping long compilation tasks, downloads, SSH sessions, and background web servers alive when the app is minimized.
* **💾 RootFS Backup, Restore & Incremental Upgrades**: 1-tap `.tar.gz` container backup export/import and schema upgrades without wiping user files.
* **🌐 Multi-Architecture Support**: Native architecture detection and rootfs support for **ARM64 (aarch64)**, **x86_64 (amd64)**, and **ARMv7 (armhf)**.
* **📁 Storage & Device Binding**: Automatic mounting of Android storage (`/sdcard`) and key system file descriptors (`/proc`, `/sys`, `/dev`).

---

## 🛠️ How It Works (Technical Architecture)

```
+-------------------------------------------------------------------------------+
|                      Android UI Layer (Jetpack Compose Material 3)            |
|  WelcomeScreen | DashboardScreen | ContainerDetail (3 Tabs) | TerminalScreen  |
|  (Onboarding)  | (Multi-Distro)  | Overview / Software / Set| Multi-Tab Strip |
+-------------------------------------------------------------------------------+
                                       |
                                       v
+-------------------------------------------------------------------------------+
|                            Kotlin Engine Core                                 |
|  ContainerManager | PRootEngine | TerminalManager & Session | SoftwareInstaller|
+-------------------------------------------------------------------------------+
             |                                             |
             v                                             v
+-----------------------------+           +-------------------------------------+
|      Native JNI Layer       |           |           PRoot Subsystem           |
|    pty.cpp (POSIX PTY)      |           |           libproot.so               |
|    - Concurrent PTYs        |           |           - ptrace syscall intercept|
|    - posix_openpt()         |           |           - Rootfs isolation (-r)   |
|    - TIOCSWINSZ resize      |           |           - Fake root mapping (-0)  |
|    - fork() & execve()      |           |           - Bind mounts (/sdcard)   |
+-----------------------------+           +-------------------------------------+
             |                                             |
             +----------------------+----------------------+
                                    |
                                    v
+-------------------------------------------------------------------------------+
|                Guest Linux Rootfs (Ubuntu / Debian / Alpine / Arch / etc)     |
|   apt / apk / pacman / xbps, shells (/bin/bash, /bin/sh), XFCE4, TigerVNC     |
|   Isolated per-container rootfs folders, process trees, and ports             |
+-------------------------------------------------------------------------------+
```

### 1. PRoot Engine (`libproot.so`) & SELinux Hard-Link Emulation
PRoot uses the `ptrace` system call mechanism to bind system calls made by guest Linux binaries. It translates paths and file operations on-the-fly, creating the illusion that guest binaries are running with root privileges (`-0`) inside a standard Linux filesystem layout (`/`), even though everything resides inside the app's internal private storage directory (`context.filesDir`).

* **Link2Symlink (`PROOT_LINK2SYMLINK`) Support**: Android SELinux policies restrict native hard-link creation on internal storage for untrusted app UIDs. The app manages a dedicated `l2s` store bound into PRoot via `-b`, translating hard-link requests (`link`/`linkat`) into transparent relative symlinks for package managers like `dpkg`, `apt`, `pacman`, and `xbps`.

### 2. Multi-Session Native PTY Bridge (`pty.cpp` & `TerminalSession.kt`)
Interactive terminal applications (like `vim`, `htop`, `tmux`, `bash`) require a Unix pseudo-terminal (PTY) to handle window dimensions, signals (`SIGINT`, `SIGTSTP`), and line buffering. The native C++ layer (`pty.cpp`) allocates a POSIX PTY per tab via `posix_openpt()`, configures window size (`TIOCSWINSZ`), and spawns PRoot child processes via `fork()` and `execve()`. Each tab runs an independent, isolated session that buffers output in the background.

### 3. Multi-Container Rootfs Provisioning & Management (`ContainerManager.kt`)
* Provisions and manages multiple independent container directories side-by-side.
* Extracts minimal distribution archives (Ubuntu, Debian, Alpine, Arch, Kali, Void) using native streaming extractors with hard-link translation.
* Automatically configures guest networking, public DNS resolvers (`/etc/resolv.conf`), hostname binding (`user@ContainerName`), and cross-distribution user provisioning (`/etc/passwd`, `/etc/group`, `/etc/shadow`).
* Accurately tracks per-container disk consumption across hard links (`du -sk`) and scopes process monitoring and active port detection strictly to each container.

---

## 📦 Upstream Distribution RootFS & Component Credits

Complete Linux Installer provisions environments using official root filesystem images directly from upstream Linux distribution project maintainers. We gratefully acknowledge the developers and maintainers of these projects:

| Linux Distribution | Maintainer / Project | Description | Source / Reference |
| :--- | :--- | :--- | :--- |
| **Ubuntu Base** | **Canonical Ltd.** | Official minimal base rootfs tarballs for Ubuntu Linux. | [Ubuntu Base Releases](https://cdimage.ubuntu.com/ubuntu-base/) |
| **Debian** | **Debian Project / debuerreotype** | Reproducible, official Debian container rootfs pipeline maintained by the Debian cloud team. | [debuerreotype Releases](https://github.com/debuerreotype/docker-debian-artifacts) / [Debian](https://www.debian.org/) |
| **Alpine Linux** | **Alpine Linux Development Team** | Official ultra-lightweight Alpine Linux minirootfs tarballs. | [Alpine Linux Releases](https://alpinelinux.org/downloads/) |
| **Arch Linux ARM** | **Arch Linux ARM Project** | Official rolling base rootfs images for ARM architectures. | [Arch Linux ARM](https://archlinuxarm.org/) |
| **Kali Linux** | **Offensive Security / Kali Linux Team** | Official NetHunter minimal rootfs images with Kali Linux repositories. | [Kali Linux](https://www.kali.org/) / [Kali NetHunter Images](https://kali.download/nethunter-images/) |
| **Void Linux** | **Void Linux Project** | Official independent rootfs tarballs featuring the XBPS package system. | [Void Linux Downloads](https://voidlinux.org/download/) |

### Open-Source Libraries & Subsystems

| Component | Description & Purpose | License / Upstream |
| :--- | :--- | :--- |
| **PRoot (`libproot.so`)** | User-space implementation of `chroot`, `mount --bind`, and root emulation using `ptrace`. | [PRoot Project](https://proot-me.github.io/) / [GPL-2.0](https://www.gnu.org/licenses/gpl-2.0.html) |
| **libandroid-shmem (`libandroid-shmem.so`)** | System V shared memory emulation wrapper for Android ashmem/memfd kernel interfaces. | [libandroid-shmem](https://github.com/termux/libandroid-shmem) |
| **talloc (`libtalloc.so`)** | Hierarchical pool-based memory allocator developed by the Samba project, required by PRoot. | [Samba talloc](https://talloc.samba.org/) / [LGPL-3.0](https://www.gnu.org/licenses/lgpl-3.0.html) |
| **Termux Project** | Architectural references for PRoot execution and PTY subprocess management on Android. | [Termux](https://github.com/termux) / [GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.html) |
| **JetBrains Mono** | Bundled monospace font used in the terminal renderer. | [JetBrains/JetBrainsMono](https://github.com/JetBrains/JetBrainsMono) / [SIL OFL 1.1](licenses/fonts/JetBrainsMono-OFL-1.1.txt) |
| **Ubuntu Mono** | Bundled monospace font option for the terminal. | [Ubuntu Font Family](https://design.ubuntu.com/font/) / [Ubuntu Font Licence](licenses/fonts/UbuntuFontLicence.txt) |

---

## 🛠️ Building from Source

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1) or newer recommended.
* **JDK**: Java 17.
* **Android NDK**: Version 25 or higher (configured for C++ CMake compilation of `pty.cpp`).

### Build Steps

1. **Clone the repository**:
   ```bash
   git clone https://github.com/devwithzachary/LinuxOnAndroid.git
   cd LinuxOnAndroid
   ```

2. **(Optional) Rebuild Native Libraries**:
   Pre-compiled binaries are maintained under `app/src/main/jniLibs/` for rapid daily builds. To recompile `libproot.so`, `libproot_loader.so`, `libproot_loader32.so`, `libtalloc.so`, and `libandroid-shmem.so` from source across all target architectures (`arm64-v8a`, `armeabi-v7a`, `x86_64`):
   ```bash
   ./scripts/build_native_libs.sh
   ```
   *Note: This script is also executed automatically by F-Droid's build pipeline to compile all native components strictly from source.*

3. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on connected device via ADB**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📖 Quick Usage Guide

### 1. First-Launch Welcome & Distro Setup Wizard
1. On first launch, the **Welcome Screen** introduces the container architecture, rootless virtualization, and multi-container capabilities.
2. Tap **Get Started** to enter the **Setup Wizard**.
3. Choose your distribution from the catalog (Ubuntu 26.04 LTS, Debian 12, Alpine Linux 3.21, Arch Linux ARM, Kali Linux CLI, or Void Linux) and select your target architecture.
4. Tap **Download & Install RootFS** to automatically stream, verify, unpack, and provision your initial container.

### 2. Concurrent Multi-Tab Terminal Workflows
* Navigate to the **Terminal** tab to interact with your environment.
* Tap the `+` button in the top tab strip to launch additional concurrent terminal sessions.
* Long-press any tab chip to give it a custom name (e.g. "Server", "Python", "Compiler").
* Use the pinned extra-keys toolbar for fast access to `Ctrl`, `Alt`, `Esc`, `Tab`, `Paste`, and directional navigation arrows.

### 3. Running Graphical Desktops (XFCE4) & Services
1. Tap into any container from the Dashboard and navigate to the **Software** tab.
2. Select **XFCE4 Desktop + VNC Server**, **NGINX Web Server**, or **OpenSSH Server** and tap **Install**.
3. Once installed, launch the service with a single tap directly from the **Overview** tab service card.
4. Connect using any Android VNC viewer (such as bVNC or VNC Viewer) at address `127.0.0.1:5901`.

### 4. Multi-Container Management & Backups
* **Install More Containers**: Add additional distinct Linux distributions anytime from the Dashboard.
* **Per-Container Isolation**: Each container runs isolated filesystem mounts, independent process trees (`ps aux`), and separate TCP port listeners.
* **Backup & Restore**: Export and import full `.tar.gz` container snapshots under **Settings > Backup & Restore**.
* **Safe Container Deletion**: Remove unwanted containers safely with real-time progress feedback and storage purging.

---

## 🤝 Contributing & AI Policy

Contributions, bug reports, and feature requests are welcome! Feel free to check out the issues page or submit a pull request.

Please review our **[AI Usage Policy](AI.md)** for guidelines on using AI coding assistants when contributing to this project. All code submitted must be thoroughly reviewed and personally owned by the human author; automated bot PRs are not accepted.

---

## 📄 License

This project is licensed under the **GNU General Public License v2.0 (GPL-2.0-or-later)** - see the [LICENSE](LICENSE) file for details. Included binaries (PRoot, talloc, libandroid-shmem) remain under their respective open-source licenses.
