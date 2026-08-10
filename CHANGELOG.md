# Changelog

All notable changes to the LinuxOnAndroid project will be documented in this file.

## [1.1.1] - 2026-08-10

### 🛡️ Play Store Policy & Storage Compliance
- **Scoped Storage & SAF Compliance**: Full Play Store policy compliance with Android Scoped Storage (`getExternalFilesDir`) and Storage Access Framework (SAF) document pickers, removing `MANAGE_EXTERNAL_STORAGE` permission.

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
