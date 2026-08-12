# Changelog

All notable changes to the LinuxOnAndroid project will be documented in this file.

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
