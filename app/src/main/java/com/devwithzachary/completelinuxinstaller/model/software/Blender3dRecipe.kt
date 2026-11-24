package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

private const val BLENDER_WRAPPER_SETUP =
    "mkdir -p /usr/local/bin /usr/share/applications && " +
        "if /usr/bin/blender --help 2>/dev/null | grep -q -- --gpu-backend; then " +
        "printf '#!/bin/sh\\nexport LIBGL_ALWAYS_SOFTWARE=1\\nexport GALLIUM_DRIVER=llvmpipe\\nfor a in \"\$@\"; do case \"\$a\" in --gpu-backend|--help|-h|--version|-v|-b|--background) exec /usr/bin/blender \"\$@\" ;; esac; done\\nexec /usr/bin/blender --gpu-backend opengl \"\$@\"\\n' > /usr/local/bin/blender; " +
        "else " +
        "printf '#!/bin/sh\\nexport LIBGL_ALWAYS_SOFTWARE=1\\nexport GALLIUM_DRIVER=llvmpipe\\nexec /usr/bin/blender \"\$@\"\\n' > /usr/local/bin/blender; " +
        "fi && " +
        "chmod 755 /usr/local/bin/blender && " +
        "for f in /usr/share/applications/*blender*.desktop; do [ -f \"\$f\" ] && sed -i \"s|^Exec=/usr/bin/blender|Exec=/usr/local/bin/blender|g; s|^Exec=blender |Exec=/usr/local/bin/blender |g; s|^Exec=blender\$|Exec=/usr/local/bin/blender|g\" \"\$f\" || true; done"

val Blender3dRecipe = SoftwareRecipe(
    id = "blender_3d",
    name = "Blender",
    description = "3D modeling, animation, rendering, and compositing studio",
    category = SoftwareCategory.CREATIVE,
    iconName = "Palette",
    expectedBinaries = listOf("usr/bin/blender"),
    isBeta = true,
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("blender"),
        PackageManagerType.DNF to listOf("blender"),
        PackageManagerType.APK to listOf("blender"),
        PackageManagerType.PACMAN to listOf("blender"),
        PackageManagerType.XBPS to listOf("blender")
    ),
    customInstallCommands = mapOf(
        PackageManagerType.APT to "export DEBIAN_FRONTEND=noninteractive && apt-get update && apt-get install -y --no-install-recommends blender && $BLENDER_WRAPPER_SETUP",
        PackageManagerType.DNF to "dnf install -y blender && $BLENDER_WRAPPER_SETUP",
        PackageManagerType.APK to "apk update && apk add --no-cache blender && $BLENDER_WRAPPER_SETUP",
        PackageManagerType.PACMAN to "sed -i 's/^DownloadUser/#DownloadUser/; s/^#DisableSandbox/DisableSandbox/' /etc/pacman.conf 2>/dev/null || true && pacman -Syyu --noconfirm --overwrite \"*\" blender && $BLENDER_WRAPPER_SETUP",
        PackageManagerType.XBPS to "xbps-install -Suy && xbps-install -Sy blender && $BLENDER_WRAPPER_SETUP"
    ),
    preUninstallCommand = "rm -f /usr/local/bin/blender 2>/dev/null || true",
    postInstallNotes = "Open from XFCE Applications -> Graphics -> Blender (uses OpenGL software rendering in PRoot)."
)

