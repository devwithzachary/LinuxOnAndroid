# External Native Libraries Provenance & Auditing

This document details the origin, upstream revisions, licensing, and modifications of all vendored native source code located in `external/`.

These native libraries are built directly from source by `scripts/build_native_libs.sh` during development and F-Droid CI builds without network access, ensuring complete auditability and reproducible builds.

---

## 1. PRoot (`external/proot`)

* **Upstream Repository**: [https://github.com/termux/proot](https://github.com/termux/proot)
* **Recorded Upstream Commit**: `7266fb3e8516535682f5a9c8f3a7e70f6506eddb` (branch: `master`)
* **Upstream Authors**: Termux Team, Cédric Vincent, and contributors
* **License**: GNU General Public License v2.0 or later (GPL-2.0-or-later)
* **License File**: [`external/proot/COPYING`](external/proot/COPYING)
* **Target Output**: `libproot.so`, `libproot_loader.so`, `libproot_loader32.so`
* **Purpose**: User-space `ptrace`-based system call interception and rootless filesystem virtualization (chroot / mount simulation) on Android.
* **Modifications**: C source tree copied from upstream `src/`. Header include `<string.h>` added to `extension/ashmem_memfd/ashmem_memfd.c` for Android NDK Clang compatibility.

---

## 2. libandroid-shmem (`external/libandroid-shmem`)

* **Upstream Repository**: [https://github.com/termux/libandroid-shmem](https://github.com/termux/libandroid-shmem)
* **Recorded Upstream Commit**: `7f0bd7e25dbdd146265aff7c6a890029e374622d` (branch: `master`)
* **Upstream Authors**: Termux Team, Fredrik Fornwall
* **License**: BSD 2-Clause / MIT-style
* **License File**: [`external/libandroid-shmem/LICENSE`](external/libandroid-shmem/LICENSE)
* **Target Output**: `libandroid-shmem.so`
* **Purpose**: Translates System V Shared Memory (`shmget`, `shmat`, `shmdt`, `shmctl`) calls into Android `ashmem` / `memfd` file descriptors, enabling X11, VNC, and graphical apps to share memory buffers on Android.
* **Modifications**: None.

---

## 3. talloc (`external/talloc`)

* **Upstream Source**: Samba Project ([https://www.samba.org/ftp/talloc/](https://www.samba.org/ftp/talloc/) / [https://gitlab.com/samba-team/samba](https://gitlab.com/samba-team/samba))
* **Version**: `2.4.2`
* **Upstream Authors**: Andrew Tridgell, Stefan Metzmacher, and the Samba Team
* **License**: GNU Lesser General Public License v3.0 or later (LGPL-3.0-or-later)
* **License File**: [`external/talloc/LICENSE`](external/talloc/LICENSE)
* **Target Output**: `libtalloc.so`
* **Purpose**: Hierarchical reference-counted memory allocation library utilized internally by PRoot.
* **Vendoring Rationale**: Upstream talloc is developed within the multi-gigabyte Samba monorepo and employs a complex Waf build system requiring Python and host configuration checks. The core C sources (`talloc.c`, `talloc.h`, `config.h`, `replace.h`) are vendored directly so `scripts/build_native_libs.sh` can compile `libtalloc.so` cleanly with the Android NDK Clang toolchain in an isolated, offline environment.
