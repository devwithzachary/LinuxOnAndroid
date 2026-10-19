package com.devwithzachary.completelinuxinstaller.model

import android.content.Context
import com.devwithzachary.completelinuxinstaller.engine.ContainerManager
import java.io.File

data class ContainerInstance(
    val id: String,
    val name: String,
    val distroId: String,
    val distroName: String,
    val rootDirPath: String,
    val installedAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false,
    val storageUsedMb: Long = 0L,
    val defaultUser: String = "root",
    val defaultShell: String = "/bin/bash",
    val packageManager: PackageManagerType = PackageManagerType.APT,
    val colorHex: Long = 0xFFE95420,
    val externalFolderName: String = "",
    val userPassword: String = "user",
    val webTerminalLanAccess: Boolean = false,
    val codeServerLanAccess: Boolean = false,
    val codeServerPassword: String = "",
    val vncLanAccess: Boolean = false,
    val vncPassword: String = ""
) {
    val username: String get() = defaultUser
    val rootDir: File get() = File(rootDirPath)

    val effectiveVncPassword: String
        get() = vncPassword.ifBlank { userPassword.ifBlank { "user" } }

    fun getExternalDirectory(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: File("/sdcard/Android/data/${context.packageName}/files")
        val folder = externalFolderName.ifBlank { "container" }
        return File(base, folder).apply { if (!exists()) mkdirs() }
    }

    fun getExternalDisplayPath(context: Context): String {
        val dir = getExternalDirectory(context)
        return dir.absolutePath.replace("/storage/emulated/0", "/sdcard")
    }

    val isInstalled: Boolean
        get() = ContainerManager.isRealRootfs(rootDir)

    val hasRootPassword: Boolean
        get() = com.devwithzachary.completelinuxinstaller.engine.RootfsManager.hasRootPassword(rootDir)
}
