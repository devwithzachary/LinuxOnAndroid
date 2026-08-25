# Changelog

All notable changes to the LinuxOnAndroid project will be documented in this file.

## [1.3.0] - Unreleased

### 📱 Device Compatibility & Architecture
- **Expanded Android 6.0+ (API 23) Support**: Lowered minimum supported Android SDK to API 23 (Marshmallow), enabling support for legacy devices and tablets without sacrificing modern Jetpack Compose Material 3 features.

### 🔤 Terminal Font Engine & Character Spacing (Issue #31)
- **Bundled Monospace Font Assets**: Embedded authentic **JetBrains Mono** and **Ubuntu Mono** font families directly into APK resources, guaranteeing fixed-pitch character metrics across all Android devices.
- **Samsung One UI & Custom System Font Compatibility**: Fixed issue where custom system fonts (e.g. SamsungOne) replaced system monospace font mappings with proportional typefaces and caused large uneven character gaps.
- **Native Bold Font Rendering**: Replaced synthetic fake-bold text scaling with authentic bold font variants (`jetbrains_mono_bold`, `ubuntu_mono_bold`) for crystal-clear terminal text at any font size.
- **Curated Monospace Font Picker**: Streamlined Settings to dedicated developer monospace typefaces (JetBrains Mono, Ubuntu Mono, Monospace, CyberGlyphs).
- **Password Manager & Autocorrect Fix**: Configured developer URI input mode (`KeyboardType.Uri`) with `autoCorrectEnabled = false`, preventing 1Password, Bitwarden, KeePass, and Google Password Manager from triggering unwanted autofill popups while strictly suppressing software keyboard autocorrect (e.g. `ls` -> `L's`) on Gboard and Samsung Keyboard.
- **Permanent CTRL & ALT Modifier Keys**: Pinned persistent **CTRL** and **ALT** modifier toggle buttons at the start of the terminal hotkey bar with visual active latching, allowing users to intuitively send control and alt key combinations (e.g., tap CTRL then L for `Ctrl+L` clear screen, or ALT then B for backward word) with automatic unlatching upon keypress.

### 🐧 Linux Environment & Shell Variables
- **System-Wide `UBUNTU_CODENAME` & `VERSION_CODENAME`**: Configured and exported `UBUNTU_CODENAME` and `VERSION_CODENAME` across `/etc/os-release`, `/etc/environment`, `/etc/lsb-release`, and `/etc/profile.d/00-linuxonandroid-env.sh`, ensuring full compatibility with Docker installation scripts and 3rd-party apt repo sources.
- **PRoot Environment Export**: Injected `UBUNTU_CODENAME` and `VERSION_CODENAME` directly into the PRoot runtime environment for all container sessions and shell invocations.

### 🌐 Internationalization & Localization (PR #27)
- **German Language Support (Deutsch)**: Added complete German localization across all screens, setup wizard, settings, dialogs, and software hub (contributed by @bkodenkt via [PR #27](https://github.com/devwithzachary/LinuxOnAndroid/pull/27)).

### 💬 Community & Discord
- **Official Discord Community**: Added direct 1-tap invitation link to the LinuxOnAndroid Discord community in the About screen and project documentation.

### 🌟 Project Credits & Contributor Recognition
- **Community Credits Section**: Added dedicated Credits & Contributors card in the About screen celebrating code/translation contributors (PRs), bug hunters & feature pioneers (issues #7, #8, #9, #10, #11, #12, #13, #14, #16, #19, #21, #31), and Patreon backers with interactive GitHub profile and issue links.

### 🚀 Dynamic Splash Screen & Environment Initialization
- **Real-Time Loading Status**: Replaced cryptic "PRoot Environment v1.0" badge with dynamic detected architecture chip (`PRoot Container • ARM64/x86_64`) and real-time step-by-step progress status messages during environment pre-flight checks.

### 🛡️ Persistent Foreground Service & Resource Monitor
- **Background Execution & WakeLock**: Runs PRoot inside an Android Foreground Service holding a partial CPU `WakeLock`, preventing Android Doze and Phantom Process Killer from terminating long `apt upgrade` operations, C/C++/Rust compilations, SSH sessions, or background servers when the app is minimized.
- **Swipe & Recents Persistence (`stopWithTask="false"`)**: Container sessions and background daemons persist across task dismissal and app minimization with `onTaskRemoved` lifecycle handling.
- **Android 13+ Notification Permission (`POST_NOTIFICATIONS`)**: Added automatic runtime permission prompt on modern Android versions (API 33+) ensuring persistent service notifications and live resource status are visible in the status bar.
- **Live Service Status & Resource Monitoring**: Displays active container status (e.g., `SSH :2222 | VNC :5901 • RAM: 140MB`) in the persistent Android notification shade with quick actions to "Open Terminal" or "Stop Session".
- **Dedicated Background Execution Settings**: Configurable in its own dedicated card and filter category under Settings > Background Execution with instant start/stop lifecycle management and real-time protection summary.
- **Keep Screen On While Terminal Is Active (Issue #25)**: Added a screen keep-alive tied to the terminal view, so the display no longer times out mid-session while watching long builds, log output, or interactive SSH work. Configurable under Settings > Background Execution and enabled by default.


## [1.2.0] - 2026-08-14

### 🔄 RootFS Incremental Upgrades & Container Versioning
- **Container Version Tracking**: Automatically write and monitor container build versions in `/etc/linuxonandroid_version`.
- **1-Tap "RootFS Upgrade" Manager**: Apply incremental system improvements, PAM permit updates, APT sandboxing rules, and configuration fixes to existing containers without wiping data or resetting user environments.
- **Smart Upgrade Availability**: Identifies when an app build contains rootfs improvements vs when the container is already up to date.

### 📦 1-Click Package Upgrade & Manifest-Based Tracking
- **Authoritative Manifest Tracking**: 1-click packages now record install states and versions to `/etc/linuxonandroid_packages`, eliminating false-positive binary detections.
- **1-Click Software Upgrades**: Added 1-tap upgrade actions for installed software stacks directly in the Software & Packages Hub.
- **Balanced Hub Action Layout**: Streamlined card actions with equal 3-way distribution for Upgrade, Start/Stop, and View Logs.

### 🌐 Custom DNS Server Configuration
- **Network & DNS Settings**: Configure and persist custom `/etc/resolv.conf` nameservers directly from Settings.
- **Quick DNS Presets**: 1-tap presets for Google (8.8.8.8), Cloudflare (1.1.1.1), Quad9 (9.9.9.9), AdGuard (94.140.14.14), and OpenDNS (208.67.222.222), plus custom IP inputs.

### 🎛️ Settings UI Overhaul & Scannability
- **Collapsible Cards**: Settings cards are collapsed by default with unified card colors matching Dashboard and About screens.
- **Category Filter Tabs**: Quick-filter by `All`, `Container`, `Network & DNS`, `Terminal`, `Security`, and `Storage & Reset`.
- **Expand All / Collapse All**: 1-tap header toggle for rapid settings management.

### 🔤 Terminal Typography & CyberGlyphs
- **Horizontal Font Family Selector**: Converted font picker into a smooth horizontal scroll chip row, preventing button squishing.
- **Streamlined Monospace Fonts**: Default Monospace and JetBrains Mono (Bold).
- **Playful Fonts & CyberGlyphs**: Added Cursive, Casual, and high-performance, zero-allocation CyberGlyphs symbol typography.

### ⚡ Performance, Speed & Architecture Improvements
- **Asynchronous IO Refreshes**: Shifted all container filesystem inspection and status polling to background IO dispatchers, eliminating UI frame drops.
- **64KB High-Throughput Stream Buffers**: Quadrupled I/O throughput on flash storage for rootfs downloads, extractions, and backups.
- **Modernized Compose APIs**: Migrated deprecated `ClickableText` and `LocalClipboardManager` APIs.

## [1.1.1] - 2026-08-12

### 👤 Terminal User & Session Control
- **Default Session User Selector**: Added setting in Settings menu to select default terminal session user (Root or non-root User), defaulting to your created regular user account.
- **Custom User First-Launch Setup**: First-launch setup wizard now configures your custom user account directly on initial installation and cleans up generic default fallback users.

### 🔤 Terminal Customization & Hotkeys
- **Terminal Font Family & Size Selector**: Added font size adjustment slider and monospace font family selection (Roboto Mono, Fira Code, Source Code Pro, JetBrains Mono, Default Monospace).
- **Interactive Hotkey Editor & Reordering**: Edit, add, remove, and drag-and-drop reorder terminal quick-access hotkey buttons with support for special key combinations.

### 🔑 Standard Ubuntu Sudo & User Privilege Restoration
- **Native Interactive Ubuntu `sudo`**: Restored standard Ubuntu `/usr/bin/sudo` with user-space `setuid` syscall interception (`PROOT_FORCE_SETID=1`) and PAM permit configuration (`pam_permit.so`), supporting interactive prompts (`sudo apt upgrade`, `sudo nano`, `sudo su`).
- **Sudo Ownership & Setuid Safeguards**: Fixed package installer pre-flight hooks to prevent recursive `chmod 777` over `/usr` and `/etc`, automatically preserving `4755` setuid permissions and `0:0` root ownership for standard `sudo`.

### ⌨️ Terminal IME & Keyboard Enhancements
- **Soft Keyboard Autocorrect & Suggestion Disabling**: Configured terminal input as `Password`-style IME stream with `autoCorrectEnabled = false`, preventing software keyboards (Gboard, SwiftKey, etc.) from auto-correcting CLI commands like `usr` to `use`.
- **Composition Double Typing Fix**: Fixed Compose `BasicTextField` composition diff calculation to eliminate double-character inputs during soft keyboard typing.

### 🌐 Networking & Storage Improvements
- **SSH Service & Remote Login Configuration**: Fixed OpenSSH Server setup (`Port 2222`) with password authentication, pam_loginuid bypass, and SFTP subsystem support.
- **Scoped Storage & SAF Compliance**: Full Play Store policy compliance with Android Scoped Storage (`getExternalFilesDir`) and Storage Access Framework (SAF) document pickers.

## [1.1.0] - 2026-08-10

### 💾 RootFS Container Backup & Restore
- **1-Tap Export Container**: Create `.tar.gz` compressed rootfs backups of your entire Ubuntu environment directly to device storage.
- **1-Tap Import Container**: Restore existing container backup archives with 1-click under Settings.
- **Real-Time Extraction & Compression Progress Bar**: Visual progress tracking during container backup creation and extraction.

### 🎨 Terminal Color Theme Packs & Custom Palette Creator
- **5 Standard Color Theme Presets**: Added Dracula, Solarized Dark, Monokai, One Dark, and Cyberpunk ANSI palettes.
- **Custom Theme Creator**: Edit Foreground, Background, Cursor, Selection Highlight, and 16 individual ANSI colors with live terminal preview.
- **Hex Color Picker**: Edit color hex values with real-time preview and quick color swatches.
- **Theme Persistence**: Choice of theme and custom colors persist across app restarts.

### 📜 Terminal UX & Scrollback Enhancements
- **Scrollback History**: Full drag-scroll support up and down through terminal output buffer without triggering text selection.
- **Maximize Screen Space**: Streamlined screen headers to eliminate wasted vertical space and maximize terminal canvas.

### 📁 Storage & Mounting Improvements
- **Storage Permissions Request**: Explanatory permission cards in both the Setup Wizard and Settings menu with 1-tap grant buttons for `MANAGE_EXTERNAL_STORAGE` and `READ_EXTERNAL_STORAGE`.
- **Host-to-Guest Bind Mounts**: Mount host `/sdcard`, `/storage/emulated/0`, `/mnt/sdcard`, and `~/Downloads` automatically into the Linux container.

### 🚀 Build & Packaging Automation
- **Unified Release Build Script**: `./build_release.sh` builds both signed `.aab` for Google Play Console and signed `.apk` for GitHub Releases and F-Droid in one command.

---

## [1.0.0] - 2026-08-06
- Initial release of LinuxOnAndroid PRoot environment installer.
- Supported Ubuntu ARM64/x86_64 rootfs setups.
- Software Hub with 1-click packages (XFCE Desktop, Python, Node.js, Nginx, OpenSSH).
