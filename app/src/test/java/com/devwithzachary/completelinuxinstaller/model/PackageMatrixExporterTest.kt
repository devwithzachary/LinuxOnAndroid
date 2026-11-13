package com.devwithzachary.completelinuxinstaller.model

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import java.io.File

class PackageMatrixExporterTest {

    @Test
    fun exportPackageMatrixIfRequested() {
        if (System.getenv("EXPORT_PACKAGE_MATRIX") != "true") return

        val presets = SoftwarePackage.getPresets()
        val distrosJson = JSONArray()

        for (distro in DistroCatalog.ALL_DISTROS) {
            val distroObj = JSONObject()
            distroObj.put("id", distro.id)
            distroObj.put("name", distro.name)
            distroObj.put("packageManager", distro.packageManager.name)
            distroObj.put("amd64Url", distro.getDownloadUrl(SystemArchitecture.X86_64) ?: "")
            distroObj.put("arm64Url", distro.getDownloadUrl(SystemArchitecture.ARM64) ?: "")
            distroObj.put(
                "firstLaunchScript",
                distro.buildFirstLaunchSetupScript("testpass", "user", "testpass", false)
            )

            val pkgsJson = JSONArray()
            for (pkg in presets) {
                val pkgObj = JSONObject()
                pkgObj.put("id", pkg.id)
                pkgObj.put("name", pkg.name)
                pkgObj.put(
                    "installCommand",
                    distro.getSoftwarePackageInstallCommand(pkg.id, 2222) ?: pkg.installCommand
                )
                val binsJson = JSONArray()
                for (bin in pkg.expectedBinaries) {
                    binsJson.put(bin)
                }
                pkgObj.put("expectedBinaries", binsJson)
                pkgsJson.put(pkgObj)
            }
            distroObj.put("packages", pkgsJson)
            distrosJson.put(distroObj)
        }

        val outFile = File("build/package_matrix.json")
        outFile.parentFile?.mkdirs()
        outFile.writeText(distrosJson.toString(2))
    }
}
