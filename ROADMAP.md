# Complete Linux Installer (LinuxOnAndroid) Roadmap

This document outlines the planned evolution of **Complete Linux Installer** from **v1.9.1** onward. Priorities are shaped by community feedback, GitHub issues, and maintaining 100% compatibility across all supported distributions (Ubuntu, Debian, Fedora, Alpine, Arch Linux ARM, Kali Linux, and Void Linux).

> **Note:** Version numbers and feature groupings are targets and may shift as issues or upstream distro changes arise.

---

## v1.9.0 - Software Center Foundation & Uninstall Support (Shipped)

The primary goal of **v1.9.0** is transforming the current 1-Click Software tab into a reliable, testable **Software Center** with full lifecycle management (install, verify, upgrade, launch, and uninstall).

### 1. Full Uninstall Support for All 1-Click Packages
- Add `uninstallCommand` (and package-manager purge/autoremove support) to `SoftwarePackage` and `DistroDefinition` across all 7 distributions (`apt`, `dnf`, `apk`, `pacman`, `xbps`).
- Stop any running service/daemon (e.g. `vncserver`, `ttyd`, `code-server`, `nginx`, `sshd`) safely before uninstalling.
- Add an **Uninstall** action with confirmation dialog to `SoftwareCard` (in `ContainerDetailScreen` -> **Software** tab), plus real-time uninstall logs and status reset (`NOT_INSTALLED`).
- Clean up generated wrappers, config stubs, and orphaned desktop/service artifacts where appropriate without touching user project files in `/home` or `/sdcard`.

### 2. Expanded Software Center Catalog & UX
- Refactor per-distro `when (pkgId)` command switches into a structured, declarative package recipe model so adding new apps across all 7 distros is consistent and easy to audit.
- Add search, category filtering, and install-status filtering (`All`, `Installed`, `Available`) in the Software Center UI.
- Expand the 1-Click catalog with high-demand packages across categories:
  - **Browsers & Internet**: Firefox / Chromium (with PRoot `--no-sandbox` wrapper), `curl`/`wget`/aria2
  - **Productivity & Office**: LibreOffice, GIMP, Mousepad / Geany
  - **Databases & Backend**: PostgreSQL, MariaDB / MySQL, Redis
  - **Developer Stacks**: Rust (`rustup`/cargo), Go, C/C++ (`clang`, `cmake`, `gdb`), Ruby, PHP
  - **CLI & System Utilities**: `fastfetch`, `htop` / `btop`, `tmux`, `zsh` + Oh My Zsh, `rclone`, `ffmpeg`
  - **Security & Networking** (especially for Kali / Ubuntu / Debian): `nmap`, `wireshark` (CLI `tshark`), `metasploit-framework` / `sqlmap` (where packaged)

### 3. Automated Multi-Distro Package Testing Matrix
- Replace the hardcoded Ubuntu-only `PKG_IDS` list in `scripts/test_1click_packages.sh` with a unified test suite driven by the app's package catalog.
- Test **every `(distro, package)` combination** end-to-end:
  1. Provision / verify base distro rootfs
  2. Run `installCommand` -> assert exit code `0` and verify `expectedBinaries` exist via `SoftwarePackage.isBinaryPresent`
  3. Run `launchCommand` smoke check (for service packages)
  4. Run `uninstallCommand` -> assert exit code `0` and verify target binaries are removed
- Add unit tests verifying every `DistroDefinition` provides valid install, uninstall, and verification metadata for every catalog package (no silent missing mappings).

---

## v1.9.1 - Botanical Green Theme & Stability Fixes (In Progress)

Focused on visual identity polish and resolving regressions reported on GitHub:

- **Botanical Green Classic Brand Theme**: Replaced the orange/espresso palette with Android Green primary, balanced sage secondary, warm Tux amber tertiary accents, and forest-charcoal dark surfaces, plus 4-tab navigation (`Dashboard`, `Terminal`, `Desktop`, `Settings` with `About` inside Settings) and clean Dashboard container subtitles.
- **Ubuntu 26.04 GNU Coreutils Fix ([#59](https://github.com/devwithzachary/LinuxOnAndroid/issues/59))**: Automatically switch Ubuntu 26.04 containers from Rust `uutils` coreutils to `coreutils-from-gnu` and heal `.l2s.coreutils.dpkg-new*` symlinks so `dpkg` and shell utilities run cleanly under PRoot.
- **VNC Session Logging & Diagnostics ([#61](https://github.com/devwithzachary/LinuxOnAndroid/issues/61), [#63](https://github.com/devwithzachary/LinuxOnAndroid/issues/63))**: Log `Xvnc` and `xstartup` output to `~/.vnc/vncserver_1.log` and attach the recent VNC log tail to Debug Reports.
- **Internal Storage & Environment Hardening ([#54](https://github.com/devwithzachary/LinuxOnAndroid/issues/54))**: Enforce `android:installLocation="internalOnly"` to prevent Adoptable Storage linker errors and strip inherited `LD_PRELOAD` before spawning `libproot.so`.

---

## v1.10.0 - Desktop, Graphics & Distribution Enhancements

Focused on closing the gap between mobile VNC and a true portable Linux workstation, while expanding distribution choices:

- **Bidirectional Clipboard Sync**: Sync Android clipboard <-> X11/VNC clipboard inside the built-in `VncClient` / `DesktopScreen`.
- **Audio Forwarding**: Optional PulseAudio / PipeWire audio streaming from the Linux container to Android (`AudioTrack` / Oboe).
- **Hardware-Accelerated Rendering (Experimental)**: VirGL (`virgl_test_server`) / Zink support for smoother 2D/3D rendering in XFCE and desktop apps on supported Adreno/Mali GPUs.
- **Additional Desktop Environments & Mobile Scaling Presets**: Lightweight alternatives alongside XFCE4, such as **LXQt**, **MATE**, or tiling WMs (**i3** / **Openbox**) for lower-RAM devices, plus phone-friendly XFCE4 panel/DPI presets and touch ergonomics (addressing [#65](https://github.com/devwithzachary/LinuxOnAndroid/issues/65)).
- **GNU FSDG-Compliant Distribution Support**: Add native support for FSF-endorsed libre distributions, starting with **Trisquel ARM64** (addressing [#64](https://github.com/devwithzachary/LinuxOnAndroid/issues/64)).
- **Desktop & PC Mode Polish**: Improved physical mouse/trackpad scroll & right-click handling, external monitor DPI presets, and customizable toolbar shortcuts.

---

## v1.11.0 - Terminal, Container & Workflow Power Features

Focused on developer ergonomics, container portability, and addressing long-standing enhancement requests:

- **Terminal Improvements**:
  - In-terminal text search (`Ctrl+F`) and clickable URLs/file paths
  - Split-pane terminal layouts on tablets and Android PC Mode
  - Custom terminal color schemes / theme presets and Nerd Font support
- **Container Lifecycle & Storage Tools**:
  - **Custom RootFS Import**: Import user-supplied `.tar.gz` / `.tar.xz` rootfs archives directly in the Setup Wizard.
  - **Container Cloning**: Duplicate an existing configured container before experimenting with risky upgrades.
  - **Service Auto-Start Profiles**: Optional per-container startup services (e.g., auto-start SSH or NGINX when launching a container).
  - **Disk Space Analyzer & Cache Cleaner**: One-tap cleanup of `apt`/`pacman`/`dnf`/`xbps` package caches to reclaim storage.
- **Init / Service Supervision**: Lightweight user-space service supervisor (addressing [#20](https://github.com/devwithzachary/LinuxOnAndroid/issues/20)) so daemons can be started, stopped, and monitored cleanly without full `systemd`.

---

## v1.12.0 - GPU Acceleration & Gaming Support

Focused on unlocking hardware-accelerated graphics and x86_64/Windows gaming workloads inside rootless PRoot containers:

- **Hardware GPU Acceleration (Turnip / Freedreno / Zink / VirGL)**:
  - Bundle or provision `virgl_test_server` (and Turnip Vulkan ICD / Zink OpenGL-over-Vulkan where supported on Adreno/Mali GPUs) so Linux desktop apps, browsers, and 3D workloads no longer rely solely on `LIBGL_ALWAYS_SOFTWARE=1`.
  - Expose GPU/DRI acceleration toggles per container in Desktop & Container Settings.
- **x86 / x86_64 Emulation on ARM64 (Box64 / FEX-Emu)**:
  - 1-click Software Center recipes and PRoot integration for **Box64** (and/or **FEX-Emu**) to run x86_64 Linux binaries and games on ARM64 Android devices.
- **Wine / Proton / DXVK Gaming Pipeline**:
  - Pre-configured **Wine + DXVK** 1-click Software Center profile paired with hardware acceleration and low-latency audio/display settings.
- **Controller & Gamepad Input**:
  - Route Android Bluetooth/USB gamepad events (`InputDevice` / `KeyEvent` / `MotionEvent`) into the container's desktop/X11 session.
- **Wayland / Mobile DE Exploration (Phosh / Plasma Mobile)**:
  - Evaluate Wayland-based mobile shells once Turnip / VirGL GPU acceleration is established in PRoot (tracking [#65](https://github.com/devwithzachary/LinuxOnAndroid/issues/65)).

---

## Ongoing & Long-Term Priorities

- **Android 15/16+ Compatibility**: Keep PRoot (`libproot.so`), SELinux shims, and 16 KB page-size native library support up to date with new Android releases.
- **Distro Base Updates**: Keep upstream rootfs URLs, GPG keys, and package recipes current as Ubuntu, Fedora, Alpine, Debian, Arch, Kali, and Void publish new releases.
- **Community & Localization**: Expand translations (building on existing community locales) and keep automated distro/package verification green before every release.
- **CI Automation**: Add a GitHub Actions CI workflow (`.github/workflows/ci.yml`) to run unit tests and lint checks on every PR and release branch.

