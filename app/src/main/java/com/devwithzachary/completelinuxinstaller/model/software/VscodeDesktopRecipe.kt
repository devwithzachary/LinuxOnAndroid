package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

private fun buildVscodeWrapperAndDesktopPatch(targetBin: String): String =
    "rm -f /usr/bin/code && " +
        "printf '#!/bin/sh\\nif [ \"\$(id -u)\" = \"0\" ]; then\\n  exec $targetBin --no-sandbox --disable-dev-shm-usage --disable-gpu --user-data-dir=/root/.vscode \"\$@\"\\nelse\\n  exec $targetBin --no-sandbox --disable-dev-shm-usage --disable-gpu \"\$@\"\\nfi\\n' > /usr/bin/code && " +
        "chmod 755 /usr/bin/code && " +
        "sed -i 's|Exec=/usr/share/code/code|Exec=/usr/bin/code|g; s|Exec=/opt/vscode/code|Exec=/usr/bin/code|g' /usr/share/applications/code*.desktop /usr/share/applications/*VSCode*.desktop 2>/dev/null || true"

val VscodeDesktopRecipe = SoftwareRecipe(
    id = "vscode_desktop",
    name = "VS Code (Desktop)",
    description = "Official Visual Studio Code desktop IDE with PRoot sandbox bypass",
    category = SoftwareCategory.DEVELOPMENT,
    iconName = "Code",
    expectedBinaries = listOf("usr/bin/code"),
    unsupportedDistros = setOf("alpine_3_24"),
    isBeta = true,
    customInstallCommands = mapOf(
        PackageManagerType.APT to "export DEBIAN_FRONTEND=noninteractive && apt-get update && apt-get install -y --no-install-recommends curl ca-certificates && ARCH=\$(uname -m); case \"\$ARCH\" in aarch64|arm64) VS_OS=\"linux-deb-arm64\" ;; *) VS_OS=\"linux-deb-x64\" ;; esac; curl -fsSL -L \"https://update.code.visualstudio.com/latest/\$VS_OS/stable\" -o /tmp/vscode.deb && apt-get install -y --no-install-recommends /tmp/vscode.deb && rm -f /tmp/vscode.deb /etc/apt/sources.list.d/vscode* && ${buildVscodeWrapperAndDesktopPatch("/usr/share/code/bin/code")}",
        PackageManagerType.DNF to "dnf install -y curl ca-certificates && ARCH=\$(uname -m); case \"\$ARCH\" in aarch64|arm64) VS_OS=\"linux-rpm-arm64\" ;; *) VS_OS=\"linux-rpm-x64\" ;; esac; curl -fsSL -L \"https://update.code.visualstudio.com/latest/\$VS_OS/stable\" -o /tmp/vscode.rpm && dnf install -y /tmp/vscode.rpm && rm -f /tmp/vscode.rpm && ${buildVscodeWrapperAndDesktopPatch("/usr/share/code/bin/code")}",
        PackageManagerType.PACMAN to "sed -i 's/^DownloadUser/#DownloadUser/; s/^#DisableSandbox/DisableSandbox/' /etc/pacman.conf 2>/dev/null || true && pacman -Syyu --noconfirm --overwrite \"*\" curl ca-certificates tar gzip nss nspr alsa-lib gtk3 libxss libxtst at-spi2-core libcups libdrm mesa libxkbcommon pango cairo gdk-pixbuf2 && ARCH=\$(uname -m); case \"\$ARCH\" in aarch64|arm64) VS_OS=\"linux-arm64\" ;; *) VS_OS=\"linux-x64\" ;; esac; curl -fsSL -L \"https://update.code.visualstudio.com/latest/\$VS_OS/stable\" -o /tmp/vscode.tar.gz && rm -rf /opt/vscode && mkdir -p /opt/vscode /usr/share/applications && tar -xzf /tmp/vscode.tar.gz -C /opt/vscode --strip-components=1 && rm -f /tmp/vscode.tar.gz && ${buildVscodeWrapperAndDesktopPatch("/opt/vscode/bin/code")} && printf '[Desktop Entry]\\nName=Visual Studio Code\\nComment=Code Editing. Redefined.\\nExec=/usr/bin/code %%F\\nIcon=/opt/vscode/resources/app/resources/linux/code.png\\nType=Application\\nCategories=Development;IDE;\\n' > /usr/share/applications/code.desktop",
        PackageManagerType.XBPS to "xbps-install -Suy && xbps-install -Sy curl ca-certificates tar gzip glib nss nspr alsa-lib gtk+3 libXScrnSaver libXtst at-spi2-core cups libdrm libGL libgbm libxkbcommon pango cairo gdk-pixbuf && ARCH=\$(uname -m); case \"\$ARCH\" in aarch64|arm64) VS_OS=\"linux-arm64\" ;; *) VS_OS=\"linux-x64\" ;; esac; curl -fsSL -L \"https://update.code.visualstudio.com/latest/\$VS_OS/stable\" -o /tmp/vscode.tar.gz && rm -rf /opt/vscode && mkdir -p /opt/vscode /usr/share/applications && tar -xzf /tmp/vscode.tar.gz -C /opt/vscode --strip-components=1 && rm -f /tmp/vscode.tar.gz && ${buildVscodeWrapperAndDesktopPatch("/opt/vscode/bin/code")} && printf '[Desktop Entry]\\nName=Visual Studio Code\\nComment=Code Editing. Redefined.\\nExec=/usr/bin/code %%F\\nIcon=/opt/vscode/resources/app/resources/linux/code.png\\nType=Application\\nCategories=Development;IDE;\\n' > /usr/share/applications/code.desktop"
    ),
    customUninstallCommands = mapOf(
        PackageManagerType.APT to "export DEBIAN_FRONTEND=noninteractive && apt-get remove -y code && apt-get autoremove -y; rm -f /usr/bin/code",
        PackageManagerType.DNF to "dnf remove -y code; rm -f /usr/bin/code",
        PackageManagerType.PACMAN to "rm -rf /opt/vscode /usr/bin/code /usr/share/applications/code.desktop",
        PackageManagerType.XBPS to "rm -rf /opt/vscode /usr/bin/code /usr/share/applications/code.desktop"
    ),
    postInstallNotes = "Launch from the XFCE Applications menu or run 'code' in a terminal (PRoot sandbox bypass is configured automatically)."
)

