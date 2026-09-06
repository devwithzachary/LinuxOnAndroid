package com.devwithzachary.completelinuxinstaller.engine

import android.content.Context
import android.util.Log
import com.devwithzachary.completelinuxinstaller.BuildConfig
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.model.DistroCatalog
import com.devwithzachary.completelinuxinstaller.model.PackageManagerType
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ContainerManager(private val context: Context) {

    companion object {
        private const val TAG = "ContainerManager"
        private const val PREFS_NAME = "containers_prefs"
        private const val KEY_CONTAINERS_JSON = "installed_containers_json"
        private const val KEY_DEFAULT_CONTAINER_ID = "default_container_id"
        const val DEFAULT_CONTAINER_ID = "ubuntu_default"

        fun isRealRootfs(dir: File): Boolean {
            if (!dir.exists() || !dir.isDirectory) return false
            val binSh = File(dir, "bin/sh")
            val binBash = File(dir, "bin/bash")
            val binAsh = File(dir, "bin/ash")
            val usrBinSh = File(dir, "usr/bin/sh")
            val usrBinBash = File(dir, "usr/bin/bash")
            val osRelease = File(dir, "etc/os-release")
            val sbinApk = File(dir, "sbin/apk")
            val usrBinPacman = File(dir, "usr/bin/pacman")
            val usrBinDnf = File(dir, "usr/bin/dnf")
            val hasShell = binSh.exists() || binBash.exists() || binAsh.exists() || usrBinSh.exists() || usrBinBash.exists()
            val hasDistroMarker = osRelease.exists() || sbinApk.exists() || usrBinPacman.exists() || usrBinDnf.exists() || File(dir, "usr/bin").exists()
            return hasShell && hasDistroMarker
        }

        fun formatContainerHostname(containerName: String): String {
            val tokens = containerName.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotEmpty() }
            val result = tokens.joinToString("") { token ->
                token.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            return result.ifEmpty { "localhost" }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val filesDir: File get() = context.filesDir
    val containersBaseDir: File get() = File(filesDir, "containers").apply { if (!exists()) mkdirs() }
    val legacyUbuntuRootfsDir: File get() = File(filesDir, "ubuntu_rootfs")

    private val _containers = MutableStateFlow<List<ContainerInstance>>(emptyList())
    val containers: StateFlow<List<ContainerInstance>> = _containers.asStateFlow()

    private val _defaultContainerId = MutableStateFlow(loadDefaultContainerId())
    val defaultContainerId: StateFlow<String> = _defaultContainerId.asStateFlow()

    init {
        loadAndMigrateContainers()
    }

    private fun loadDefaultContainerId(): String {
        return prefs.getString(KEY_DEFAULT_CONTAINER_ID, DEFAULT_CONTAINER_ID) ?: DEFAULT_CONTAINER_ID
    }

    fun loadAndMigrateContainers() {
        val loaded = loadContainersFromPrefs().toMutableList()

        // Automatic legacy migration:
        // If legacy ubuntu_rootfs exists and contains a real valid installation with an executable shell, register it
        if (legacyUbuntuRootfsDir.exists() && legacyUbuntuRootfsDir.isDirectory) {
            val isRealLegacy = isRealRootfs(legacyUbuntuRootfsDir)
            val alreadyRegistered = loaded.any { it.id == DEFAULT_CONTAINER_ID || it.rootDirPath == legacyUbuntuRootfsDir.absolutePath }
            if (isRealLegacy && !alreadyRegistered) {
                Log.d(TAG, "Discovered legacy rootfs at ${legacyUbuntuRootfsDir.absolutePath}. Registering as $DEFAULT_CONTAINER_ID...")
                val legacyContainer = ContainerInstance(
                    id = DEFAULT_CONTAINER_ID,
                    name = "Ubuntu 26.04",
                    distroId = DistroCatalog.UBUNTU_26_04.id,
                    distroName = DistroCatalog.UBUNTU_26_04.name,
                    rootDirPath = legacyUbuntuRootfsDir.absolutePath,
                    installedAt = System.currentTimeMillis(),
                    isDefault = true,
                    storageUsedMb = calculateFastDiskUsageMb(legacyUbuntuRootfsDir),
                    defaultUser = "ubuntu",
                    defaultShell = "/bin/bash",
                    packageManager = PackageManagerType.APT,
                    colorHex = DistroCatalog.UBUNTU_26_04.colorHex
                )
                loaded.add(0, legacyContainer)
                saveContainersToPrefs(loaded)
            }
        }

        // Clean up any phantom uninstalled containers (e.g. empty directories without shells)
        // and populate real storage sizes if previously 0
        var needsSave = false
        val validContainers = loaded.filter { container ->
            isRealRootfs(container.rootDir)
        }.map { container ->
            // If the container is a real rootfs but is missing the version marker, stamp it with the current app build version
            val versionFile = File(container.rootDir, RootfsMigrationManager.VERSION_FILE_PATH)
            if (!versionFile.exists()) {
                RootfsMigrationManager.writeVersion(
                    container.rootDir,
                    RootfsVersionInfo(
                        versionCode = BuildConfig.VERSION_CODE,
                        versionName = BuildConfig.VERSION_NAME,
                        installedAt = container.installedAt,
                        lastUpgradedAt = System.currentTimeMillis()
                    )
                )
            }
            if (container.storageUsedMb <= 0L) {
                val computed = calculateFastDiskUsageMb(container.rootDir)
                if (computed > 0L) {
                    needsSave = true
                    container.copy(storageUsedMb = computed)
                } else {
                    container
                }
            } else {
                container
            }
        }

        if (validContainers.size != loaded.size || needsSave) {
            Log.d(TAG, "Updating ${validContainers.size} containers in prefs with storage sizes.")
            saveContainersToPrefs(validContainers)
        } else {
            _containers.value = validContainers
        }

        if (_containers.value.isNotEmpty() && _containers.value.none { it.id == _defaultContainerId.value }) {
            val firstId = _containers.value.first().id
            setDefaultContainer(firstId)
        }

        // Clean up any orphaned container directories on disk (from aborted/crashed installs or previous uninstalls)
        try {
            val registeredContainerDirPaths = _containers.value.mapNotNull {
                val root = File(it.rootDirPath)
                if (root.parentFile?.parentFile == containersBaseDir) root.parentFile?.absolutePath else root.absolutePath
            }.toSet()

            val onDiskDirs = containersBaseDir.listFiles() ?: emptyArray()
            for (dir in onDiskDirs) {
                if (dir.isDirectory && !registeredContainerDirPaths.contains(dir.absolutePath)) {
                    Log.d(TAG, "Purging orphaned container directory on disk: ${dir.name}")
                    try {
                        val chmodBin = if (File("/system/bin/chmod").exists()) "/system/bin/chmod" else "chmod"
                        ProcessBuilder(chmodBin, "-R", "777", dir.absolutePath).start().waitFor()
                    } catch (_: Exception) {}
                    dir.deleteRecursively()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning orphaned container directories", e)
        }
    }

    private fun loadContainersFromPrefs(): List<ContainerInstance> {
        val jsonStr = prefs.getString(KEY_CONTAINERS_JSON, null) ?: return emptyList()
        val list = mutableListOf<ContainerInstance>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val distroId = obj.getString("distroId")
                val distroName = obj.optString("distroName", DistroCatalog.getById(distroId).name)
                val rootDirPath = obj.getString("rootDirPath")
                val installedAt = obj.optLong("installedAt", System.currentTimeMillis())
                val isDefault = obj.optBoolean("isDefault", false)
                val storageUsedMb = obj.optLong("storageUsedMb", 0L)
                val defaultUser = obj.optString("defaultUser", "root")
                val defaultShell = obj.optString("defaultShell", "/bin/bash")
                val pmName = obj.optString("packageManager", "APT")
                val packageManager = try { PackageManagerType.valueOf(pmName) } catch (_: Exception) { PackageManagerType.APT }
                val colorHex = obj.optLong("colorHex", DistroCatalog.getById(distroId).colorHex)

                list.add(
                    ContainerInstance(
                        id = id,
                        name = name,
                        distroId = distroId,
                        distroName = distroName,
                        rootDirPath = rootDirPath,
                        installedAt = installedAt,
                        isDefault = isDefault,
                        storageUsedMb = storageUsedMb,
                        defaultUser = defaultUser,
                        defaultShell = defaultShell,
                        packageManager = packageManager,
                        colorHex = colorHex
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing containers JSON", e)
        }
        return list
    }

    private fun saveContainersToPrefs(list: List<ContainerInstance>) {
        try {
            val array = JSONArray()
            val currentDefault = _defaultContainerId.value
            for (c in list) {
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("name", c.name)
                obj.put("distroId", c.distroId)
                obj.put("distroName", c.distroName)
                obj.put("rootDirPath", c.rootDirPath)
                obj.put("installedAt", c.installedAt)
                obj.put("isDefault", c.id == currentDefault)
                obj.put("storageUsedMb", c.storageUsedMb)
                obj.put("defaultUser", c.defaultUser)
                obj.put("defaultShell", c.defaultShell)
                obj.put("packageManager", c.packageManager.name)
                obj.put("colorHex", c.colorHex)
                array.put(obj)
            }
            prefs.edit().putString(KEY_CONTAINERS_JSON, array.toString()).apply()
            _containers.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Error saving containers to prefs", e)
        }
    }

    fun getAllContainers(): List<ContainerInstance> = _containers.value

    fun getDefaultContainer(): ContainerInstance? {
        val list = _containers.value
        val defaultId = _defaultContainerId.value
        return list.find { it.id == defaultId } ?: list.firstOrNull()
    }

    fun getContainer(id: String): ContainerInstance? {
        return _containers.value.find { it.id == id }
    }

    fun setDefaultContainer(id: String) {
        prefs.edit().putString(KEY_DEFAULT_CONTAINER_ID, id).apply()
        _defaultContainerId.value = id
        val updated = _containers.value.map { it.copy(isDefault = it.id == id) }
        saveContainersToPrefs(updated)
    }

    fun getContainerRootfsDir(containerId: String): File {
        val existing = getContainer(containerId)
        if (existing != null) {
            return File(existing.rootDirPath)
        }
        if (containerId == DEFAULT_CONTAINER_ID && legacyUbuntuRootfsDir.exists()) {
            return legacyUbuntuRootfsDir
        }
        return File(File(containersBaseDir, containerId), "rootfs")
    }

    fun createContainerEntry(
        id: String,
        name: String,
        distroId: String,
        defaultUser: String = "root",
        defaultShell: String? = null
    ): ContainerInstance {
        val distro = DistroCatalog.getById(distroId)
        val containerDir = File(containersBaseDir, id)
        val rootfsDir = File(containerDir, "rootfs").apply { mkdirs() }
        val effectiveShell = defaultShell ?: distro.defaultShell

        val newInstance = ContainerInstance(
            id = id,
            name = name.ifBlank { distro.name },
            distroId = distroId,
            distroName = distro.name,
            rootDirPath = rootfsDir.absolutePath,
            installedAt = System.currentTimeMillis(),
            isDefault = _containers.value.isEmpty(),
            storageUsedMb = 0L,
            defaultUser = defaultUser,
            defaultShell = effectiveShell,
            packageManager = distro.packageManager,
            colorHex = distro.colorHex
        )

        val updated = _containers.value.toMutableList()
        updated.removeAll { it.id == id }
        updated.add(newInstance)
        saveContainersToPrefs(updated)

        if (_containers.value.size == 1 || _defaultContainerId.value.isEmpty()) {
            setDefaultContainer(id)
        }

        return newInstance
    }

    fun updateContainer(container: ContainerInstance) {
        val updated = _containers.value.map { if (it.id == container.id) container else it }
        saveContainersToPrefs(updated)
    }

    fun setContainerDefaultUser(containerId: String, username: String) {
        val target = getContainer(containerId) ?: return
        val updated = target.copy(defaultUser = username)
        updateContainer(updated)
    }

    suspend fun deleteContainer(id: String): Boolean = withContext(Dispatchers.IO) {
        val container = getContainer(id) ?: return@withContext false
        try {
            val rootDir = File(container.rootDirPath)
            val containerFolder = if (rootDir.parentFile?.parentFile == containersBaseDir) {
                rootDir.parentFile
            } else {
                rootDir
            }

            // Ensure full write permissions before recursive deletion
            if (containerFolder != null && containerFolder.exists()) {
                try {
                    val chmodBin = if (File("/system/bin/chmod").exists()) "/system/bin/chmod" else "chmod"
                    ProcessBuilder(chmodBin, "-R", "777", containerFolder.absolutePath).start().waitFor()
                } catch (_: Exception) {}
                containerFolder.deleteRecursively()
            }

            val updated = _containers.value.filter { it.id != id }
            saveContainersToPrefs(updated)

            if (_defaultContainerId.value == id) {
                val nextDefault = updated.firstOrNull()?.id ?: DEFAULT_CONTAINER_ID
                setDefaultContainer(nextDefault)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting container $id", e)
            false
        }
    }

    fun getContainers(): List<ContainerInstance> = _containers.value

    fun calculateFastDiskUsageMb(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory) return 0L
        try {
            val duBin = if (File("/system/bin/du").exists()) "/system/bin/du" else "du"
            val pb = ProcessBuilder(duBin, "-sk", dir.absolutePath)
            pb.redirectErrorStream(false)
            val proc = pb.start()
            val lines = proc.inputStream.bufferedReader().readLines()
            proc.waitFor()
            for (line in lines.reversed()) {
                val tokens = line.trim().split("\\s+".toRegex())
                val kb = tokens.firstOrNull()?.toLongOrNull()
                if (kb != null && kb > 0) {
                    return (kb / 1024L).coerceAtLeast(1L)
                }
            }
        } catch (_: Exception) {}
        return 0L
    }

    suspend fun refreshContainerStorage(rootfsManager: RootfsManager) = withContext(Dispatchers.IO) {
        val currentContainers = _containers.value
        if (currentContainers.isEmpty()) return@withContext

        var hasChanges = false
        val updated = currentContainers.map { container ->
            val dir = File(container.rootDirPath)
            if (dir.exists() && dir.isDirectory) {
                val sizeMb = rootfsManager.getStorageUsedMbForDir(dir)
                if (sizeMb != container.storageUsedMb && sizeMb > 0L) {
                    hasChanges = true
                    container.copy(storageUsedMb = sizeMb)
                } else {
                    container
                }
            } else {
                if (container.storageUsedMb != 0L) {
                    hasChanges = true
                    container.copy(storageUsedMb = 0L)
                } else {
                    container
                }
            }
        }
        if (hasChanges) {
            saveContainersToPrefs(updated)
        }
    }
}
