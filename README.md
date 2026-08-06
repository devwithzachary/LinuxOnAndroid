# Complete Linux Installer (Linux on Android)

[![Google Play Store](https://img.shields.io/badge/Google%20Play-Download-brightgreen.svg?logo=googleplay)](https://play.google.com/store/apps/details?id=com.devwithzachary.completelinuxinstaller)
[![Google Play Open Testing](https://img.shields.io/badge/Google%20Play-Open%20Beta-blue.svg?logo=googleplay)](https://play.google.com/apps/testing/com.devwithzachary.completelinuxinstaller)
[![Android MinSDK](https://img.shields.io/badge/Min%20SDK-24%20%28Android%207.0%2B%29-brightgreen.svg)](https://developer.android.com/about/versions/nougat)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![UI Framework](https://img.shields.io/badge/UI-Jetpack%20Compose%20Material3-purple.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-ARM64%20%7C%20x86__64%20%7C%20ARMv7-orange.svg)](#multi-architecture-support)
[![License](https://img.shields.io/badge/License-GPL--2.0--or--later-blue.svg)](LICENSE)

**Complete Linux Installer** is an open-source Android application designed to download, provision, and run full-featured Linux distributions (such as Ubuntu 26.04 LTS) natively on Android devices **without requiring root permissions**.

> [!TIP]
> 📲 **Now Live on the Google Play Store!**  
> **[Download on Google Play](https://play.google.com/store/apps/details?id=com.devwithzachary.completelinuxinstaller)** | **[Join Beta Testing Track](https://play.google.com/apps/testing/com.devwithzachary.completelinuxinstaller)**

Powered by a native **PRoot** engine, a JNI-backed **PTY pseudo-terminal**, and a modern **Jetpack Compose** interface, this app brings a true Linux development and desktop environment straight to your mobile device or tablet.

> [!WARNING]
> **Alpha Software Notice**: Complete Linux Installer is currently in **early alpha**. You may encounter bugs, unexpected behavior, or package edge cases. If you encounter any issues, please [raise an issue on GitHub](https://github.com/devwithzachary/LinuxOnAndroid/issues)!

---

## 🚀 Key Features

* **🔒 100% Rootless Operation**: Runs entirely in Android user-space using PRoot ptrace system call interception. No root access or bootloader unlocking required.
* **🖥️ Full Graphical Desktop Access (GUI)**: One-click installation of a complete **XFCE4 Desktop Environment** with TigerVNC and noVNC support for full windowed GUI desktop access right on your phone or tablet.
* **⚡ Interactive Native Terminal**: Built-in VT100/XTerm-compatible terminal emulator with full ANSI color support, buffer scrolling, and custom quick-action keys (Ctrl, Alt, Esc, Tab, Arrow navigation).
* **🛠️ Software Hub & One-Click Stacks**: Pre-configured software installers for common stacks:
  * **Desktop Environments**: XFCE4 Desktop, XFCE Terminal, TigerVNC Server, noVNC web interface.
  * **Python 3 Developer Stack**: Python 3, pip, venv, Git, C/C++ GCC build-essential, Neovim.
  * **Node.js Developer Stack**: Node.js, npm, Yarn, Git, C/C++ GCC build-essential, Neovim.
  * **Android Developer Tools**: OpenJDK 17, Android Platform Tools (adb, fastboot), Gradle, Git.
  * **Web & Database**: NGINX high-performance HTTP web server + SQLite3.
  * **Remote Access**: OpenSSH Server daemon setup for SSH remote terminal access from PC or LAN.
* **🌐 Multi-Architecture Support**: Automatic detection and support for **ARM64 (aarch64)**, **x86_64 (amd64)**, and **ARMv7 (armhf)** processor architectures.
* **📁 Storage & Device Binding**: Automatic mounting of Android SDCard/storage (`/sdcard`) and key system file descriptors (`/proc`, `/sys`, `/dev`).

---

## 🛠️ How It Works (Technical Architecture)

```
+------------------------------------------------------------------+
|                   Android UI Layer (Jetpack Compose)            |
|       WizardScreen  |  DashboardScreen  |  TerminalScreen        |
+------------------------------------------------------------------+
                                  |
                                  v
+------------------------------------------------------------------+
|                      Kotlin Engine Core                          |
|    RootfsManager    |   PRootEngine    |   TerminalBridge        |
+------------------------------------------------------------------+
            |                             |
            v                             v
+-----------------------+     +------------------------------------+
|  Native JNI Layer     |     |   PRoot Subsystem                  |
|  pty.cpp (Posix PTY)  |     |   libproot.so                      |
|  - posix_openpt()     |     |   - ptrace syscall interception    |
|  - grantpt/unlockpt   |     |   - Rootfs path isolation (-r)     |
|  - fork() & execve()  |     |   - Fake root user mapping (-0)    |
|  - Window resize      |     |   - Bind mounts (/dev, /proc, etc) |
+-----------------------+     +------------------------------------+
            |                             |
            +--------------+--------------+
                           |
                           v
+------------------------------------------------------------------+
|              Guest Linux Rootfs (Ubuntu / Debian)                |
|              /bin/bash, apt, dpkg, gcc, python, xfce4            |
+------------------------------------------------------------------+
```

### 1. PRoot Engine (`libproot.so`) & SELinux Hard-Link Emulation
PRoot uses the `ptrace` system call mechanism to bind system calls made by guest Linux binaries. It translates paths and file operations on-the-fly, creating the illusion that guest binaries are running with root privileges (`-0`) inside a standard Linux filesystem layout (`/`), even though everything resides inside the app's internal private storage directory (`context.filesDir`).

* **Link2Symlink (`PROOT_LINK2SYMLINK`) Support**: Android SELinux policies restrict native hard-link creation on internal storage for untrusted app UIDs. The app manages a dedicated `l2s` store (`$HOST_FILES/l2s`) bound into PRoot via `-b`, translating hard-link requests (`link`/`linkat`) into transparent symlinks for package managers like `dpkg` and `apt`.

### 2. Native PTY Bridge (`pty.cpp` & `PtyNative.kt`)
Interactive terminal applications (like `vim`, `htop`, `tmux`, `bash`) require a Unix pseudo-terminal (PTY) to handle window dimensions, signals (`SIGINT`, `SIGTSTP`), and line buffering. The native C++ layer (`pty.cpp`) allocates a POSIX PTY via `posix_openpt()`, configures window size (`TIOCSWINSZ`), and spawns the PRoot child process via `fork()` and `execve()`.

### 3. Rootfs Provisioning & APT Engine (`RootfsManager.kt`)
* Downloads minimal Linux rootfs tarballs (e.g., Ubuntu Base) directly from official mirrors.
* Extracts the rootfs using native system `tar` or an embedded fallback `Java TarExtractor`.
* Auto-configures essential network and system files:
  * `/etc/resolv.conf` (DNS configuration)
  * `/etc/apt/sources.list` (Arch-aware mirrors: `ports.ubuntu.com` for ARM64/ARMv7 vs `archive.ubuntu.com` for x86_64)
  * `/etc/apt/apt.conf.d/99linuxonandroid` (`APT::Sandbox::User "root"`, disabled HTTP pipelining & pdiffs for zero-hang network updates)
  * `/etc/hosts` and `/etc/environment`

---

## 📦 Bundled Native Binaries & Credits

This project relies on several key open-source native components. We gratefully acknowledge the authors and maintainers of these projects:

| Binary / Library | Description & Purpose | License / Source |
| :--- | :--- | :--- |
| **PRoot (`libproot.so`)** | User-space implementation of `chroot`, `mount --bind`, and root emulation using `ptrace`. | [PRoot Project](https://proot-me.github.io/) / [GPL-2.0](https://www.gnu.org/licenses/gpl-2.0.html) |
| **libandroid-shmem (`libandroid-shmem.so`)** | System V shared memory emulation library wrapper for Android's ashmem/memfd kernel interfaces. | [libandroid-shmem](https://github.com/termux/libandroid-shmem) |
| **talloc (`libtalloc.so`)** | Hierarchical pool-based memory allocator developed by the Samba project, required by PRoot. | [Samba talloc](https://talloc.samba.org/) / [LGPL-3.0](https://www.gnu.org/licenses/lgpl-3.0.html) |
| **Ubuntu Base** | Official minimal root filesystem tarballs provided by Canonical Ltd. | [Ubuntu Base Releases](https://cdimage.ubuntu.com/ubuntu-base/) / Canonical Ltd. |
| **Termux Project** | Architectural references and patches for running PRoot and PTY subprocesses on Android. | [Termux](https://github.com/termux) / [GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.html) |

---

## 🛠️ Building from Source

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1) or newer recommended.
* **JDK**: Java 17.
* **Android NDK**: Version 25 or higher (configured for C++ CMake compilation of `pty.cpp`).

### Build Steps

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/linuxonandroid.git
   cd linuxonandroid
   ```

2. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on connected device via ADB**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📖 Quick Usage Guide

### 1. First-Time Setup Wizard
Launch the application and follow the setup wizard:
1. Select your target architecture (ARM64, x86_64, or ARMv7).
2. Click **Download & Install RootFS**. The app will stream the download, unpack the filesystem, and configure system files automatically.

### 2. Accessing the Linux Terminal
* Navigate to the **Terminal** tab to open an interactive session.
* Use the top quick-toolbar to easily type special keys like `Ctrl`, `Esc`, `Tab`, `Alt`, and directional arrows.

### 3. Running a Graphical Desktop (XFCE4)
1. Go to the **Software Hub** tab.
2. Select **XFCE4 Desktop + VNC Server** and tap **Install**.
3. Once installed, launch the VNC server using the provided launch command:
   ```bash
   vncserver :1 -geometry 1280x720 -depth 24
   ```
4. Connect using any Android VNC viewer app (such as bVNC or VNC Viewer) at address `127.0.0.1:5901`.

---

## 🤝 Contributing

Contributions, bug reports, and feature requests are welcome! Feel free to check out the issues page or submit a pull request.

---

## 📄 License

This project is licensed under the **GNU General Public License v2.0 (GPL-2.0-or-later)** - see the [LICENSE](LICENSE) file for details. Included binaries (PRoot, talloc, libandroid-shmem) remain under their respective open-source licenses.
