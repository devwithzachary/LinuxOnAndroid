package com.devwithzachary.completelinuxinstaller.model.software

import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory

val FirefoxBrowserRecipe = SoftwareRecipe(
    id = "firefox_browser",
    name = "Mozilla Firefox",
    description = "Desktop web browser (native package or snap-free Mozilla release)",
    category = SoftwareCategory.PRODUCTIVITY,
    iconName = "Public",
    expectedBinaries = listOf("usr/bin/firefox"),
    isBeta = true,
    packagesByManager = mapOf(
        PackageManagerType.APT to listOf("firefox-esr"),
        PackageManagerType.DNF to listOf("firefox"),
        PackageManagerType.APK to listOf("firefox"),
        PackageManagerType.PACMAN to listOf("firefox"),
        PackageManagerType.XBPS to listOf("firefox")
    ),
    customInstallCommands = mapOf(
        PackageManagerType.APT to "export DEBIAN_FRONTEND=noninteractive && apt-get update && (apt-get install -y --no-install-recommends firefox-esr || (apt-get install -y --no-install-recommends curl ca-certificates xz-utils bzip2 libdbus-glib-1-2 libxtst6 libnss3 libx11-xcb1 && (apt-get install -y --no-install-recommends libgtk-3-0t64 libasound2t64 || apt-get install -y --no-install-recommends libgtk-3-0 libasound2 || true) && ARCH=\$(uname -m); case \"\$ARCH\" in aarch64|arm64) FF_OS=\"linux64-aarch64\" ;; *) FF_OS=\"linux64\" ;; esac; curl -fsSL -L \"https://download.mozilla.org/?product=firefox-latest-ssl&os=\$FF_OS&lang=en-US\" -o /tmp/firefox.tar && rm -rf /opt/firefox && mkdir -p /opt/firefox && tar -xf /tmp/firefox.tar -C /opt && rm -f /tmp/firefox.tar && printf '#!/bin/sh\\nexport MOZ_DISABLE_CONTENT_SANDBOX=1\\nexport MOZ_DISABLE_GMP_SANDBOX=1\\nexport MOZ_DISABLE_RDD_SANDBOX=1\\nexport MOZ_DISABLE_SOCKET_PROCESS_SANDBOX=1\\nexport MOZ_DISABLE_UTILITY_SANDBOX=1\\nexport LIBGL_ALWAYS_SOFTWARE=1\\nexec /opt/firefox/firefox \"\$@\"\\n' > /usr/bin/firefox && chmod 755 /usr/bin/firefox && mkdir -p /usr/share/applications && printf '[Desktop Entry]\\nName=Firefox\\nComment=Browse the World Wide Web\\nExec=/usr/bin/firefox %%u\\nIcon=/opt/firefox/browser/chrome/icons/default/default128.png\\nTerminal=false\\nType=Application\\nCategories=Network;WebBrowser;\\n' > /usr/share/applications/firefox.desktop))"
    ),
    customUninstallCommands = mapOf(
        PackageManagerType.APT to "export DEBIAN_FRONTEND=noninteractive && (apt-get remove -y firefox-esr && apt-get autoremove -y) 2>/dev/null || true; rm -rf /opt/firefox /usr/bin/firefox /usr/share/applications/firefox.desktop"
    ),
    postInstallNotes = "Launch Firefox from XFCE Applications -> Internet or run 'firefox' inside XFCE."
)

