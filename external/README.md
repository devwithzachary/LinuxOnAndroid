# External Native Libraries

This directory contains vendored native C source code compiled by `scripts/build_native_libs.sh` to generate the JNI shared libraries (`libproot.so`, `libproot_loader.so`, `libtalloc.so`, `libandroid-shmem.so`).

For full auditability, upstream commits, licenses, and architecture notes, see [PROVENANCE.md](PROVENANCE.md).

## Summary of Components

| Component | Source / Upstream | Version / Commit | License | Output |
| :--- | :--- | :--- | :--- | :--- |
| **proot** | `termux/proot` | `7266fb3e` | GPL-2.0-or-later | `libproot.so`, `libproot_loader*.so` |
| **libandroid-shmem** | `termux/libandroid-shmem` | `7f0bd7e2` | BSD-2-Clause / MIT | `libandroid-shmem.so` |
| **talloc** | Samba Project | `2.4.2` | LGPL-3.0-or-later | `libtalloc.so` |

## Building

```bash
# Build all native libraries for arm64-v8a, armeabi-v7a, and x86_64
./scripts/build_native_libs.sh
```
