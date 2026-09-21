package com.devwithzachary.completelinuxinstaller.engine

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

object RootfsArchiveExtractor {
    private const val TAG = "RootfsArchiveExtractor"

    suspend fun extractArchive(
        archiveFile: File,
        targetDir: File,
        onProgress: (suspend (String, Int) -> Unit)? = null
    ) {
        val fileName = archiveFile.name.lowercase()
        val isXz = fileName.endsWith(".xz")
        extractArchiveInJava(archiveFile, targetDir, isXz = isXz, onProgress = onProgress)
        unwrapNestedRootfsIfNeeded(targetDir)
    }

    suspend fun extractTarGz(
        tarGzFile: File,
        targetDir: File,
        onProgress: (suspend (String, Int) -> Unit)? = null
    ) {
        extractArchive(tarGzFile, targetDir, onProgress)
    }

    private fun unwrapNestedRootfsIfNeeded(targetDir: File) {
        if (!targetDir.exists()) return
        val hasDirectRootfs = File(targetDir, "bin").exists() || File(targetDir, "usr").exists() || File(targetDir, "etc").exists()
        if (!hasDirectRootfs) {
            val children = targetDir.listFiles() ?: emptyArray()
            val singleDir = children.firstOrNull { it.isDirectory && (File(it, "bin").exists() || File(it, "usr").exists() || File(it, "etc").exists()) }
            if (singleDir != null) {
                Log.d(TAG, "Detected nested rootfs directory ${singleDir.name}, unwrapping into ${targetDir.absolutePath}...")
                val nestedItems = singleDir.listFiles() ?: emptyArray()
                for (item in nestedItems) {
                    val dest = File(targetDir, item.name)
                    if (dest.exists()) {
                        try { android.system.Os.remove(dest.absolutePath) } catch (_: Exception) { dest.delete() }
                    }
                    if (!item.renameTo(dest)) {
                        try {
                            item.copyRecursively(dest, overwrite = true)
                            item.deleteRecursively()
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed copying nested item ${item.name}", e)
                        }
                    }
                }
                singleDir.deleteRecursively()
            }
        }
    }

    private suspend fun extractArchiveInJava(
        archiveFile: File,
        targetDir: File,
        isXz: Boolean,
        onProgress: (suspend (String, Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val rawIn = BufferedInputStream(archiveFile.inputStream(), 65536)
        val tarIn: java.io.InputStream = if (isXz) {
            XZInputStream(rawIn)
        } else {
            GZIPInputStream(rawIn, 65536)
        }

        val buffer = ByteArray(512)
        var longName: String? = null
        var longLink: String? = null
        var extractedFiles = 0
        var lastUpdate = System.currentTimeMillis()

        tarIn.use { stream ->
            while (true) {
                var bytesRead = 0
                while (bytesRead < 512) {
                    val r = stream.read(buffer, bytesRead, 512 - bytesRead)
                    if (r == -1) break
                    bytesRead += r
                }
                if (bytesRead < 512) break

                var isEmpty = true
                for (i in 0 until 512) {
                    if (buffer[i] != 0.toByte()) {
                        isEmpty = false
                        break
                    }
                }
                if (isEmpty) break

                val rawName = String(buffer, 0, 100, Charsets.US_ASCII).trimEnd('\u0000', ' ')
                val mode = parseOctal(buffer, 100, 8)
                val size = parseOctal(buffer, 124, 12)
                val typeFlag = buffer[156].toInt().toChar()
                val rawLink = String(buffer, 157, 100, Charsets.US_ASCII).trimEnd('\u0000', ' ')
                val prefix = String(buffer, 345, 155, Charsets.US_ASCII).trimEnd('\u0000', ' ')

                // Handle GNU Long Name
                if (typeFlag == 'L') {
                    val nameBytes = ByteArray(size.toInt())
                    readFully(stream, nameBytes)
                    longName = String(nameBytes, Charsets.UTF_8).trimEnd('\u0000', ' ', '\n', '\r')
                    val remainder = (512 - (size % 512)) % 512
                    if (remainder > 0) skipBytes(stream, remainder)
                    continue
                }

                // Handle GNU Long Link
                if (typeFlag == 'K') {
                    val linkBytes = ByteArray(size.toInt())
                    readFully(stream, linkBytes)
                    longLink = String(linkBytes, Charsets.UTF_8).trimEnd('\u0000', ' ', '\n', '\r')
                    val remainder = (512 - (size % 512)) % 512
                    if (remainder > 0) skipBytes(stream, remainder)
                    continue
                }

                // Handle PAX Extended Headers
                if (typeFlag == 'x' || typeFlag == 'g') {
                    val paxBytes = ByteArray(size.toInt())
                    readFully(stream, paxBytes)
                    val paxString = String(paxBytes, Charsets.UTF_8)
                    for (paxLine in paxString.lines()) {
                        val spaceIdx = paxLine.indexOf(' ')
                        if (spaceIdx != -1) {
                            val eqIdx = paxLine.indexOf('=', spaceIdx)
                            if (eqIdx != -1) {
                                val key = paxLine.substring(spaceIdx + 1, eqIdx)
                                val value = paxLine.substring(eqIdx + 1)
                                if (key == "path") longName = value
                                else if (key == "linkpath") longLink = value
                            }
                        }
                    }
                    val remainder = (512 - (size % 512)) % 512
                    if (remainder > 0) skipBytes(stream, remainder)
                    continue
                }

                var entryName = longName ?: if (prefix.isNotEmpty()) "$prefix/$rawName" else rawName
                val finalLink = longLink ?: rawLink
                longName = null
                longLink = null

                if (entryName.isEmpty() || entryName == "." || entryName == "./") {
                    val remainder = (512 - (size % 512)) % 512
                    skipBytes(stream, size + remainder)
                    continue
                }

                if (entryName.startsWith("./")) {
                    entryName = entryName.substring(2)
                } else if (entryName.startsWith("/")) {
                    entryName = entryName.substring(1)
                }

                val destFile = File(targetDir, entryName)

                try {
                    when (typeFlag) {
                        '5' -> {
                            destFile.mkdirs()
                            val remainder = (512 - (size % 512)) % 512
                            if (remainder > 0) skipBytes(stream, remainder)
                        }

                        '0', '\u0000' -> {
                            destFile.parentFile?.mkdirs()
                            try {
                                android.system.Os.remove(destFile.absolutePath)
                            } catch (_: Exception) {
                                destFile.delete()
                            }
                            FileOutputStream(destFile).use { out ->
                                copyBytes(stream, out, size)
                            }
                            val isExec = (mode and 0x49L) != 0L || entryName.contains("bin/") || entryName.endsWith(".sh")
                            if (isExec) {
                                destFile.setExecutable(true, false)
                            }
                            val remainder = (512 - (size % 512)) % 512
                            if (remainder > 0) skipBytes(stream, remainder)
                        }

                        '1' -> {
                            // Hard Link
                            destFile.parentFile?.mkdirs()
                            val sourceFile = File(targetDir, finalLink.removePrefix("/"))
                            if (sourceFile.exists()) {
                                try {
                                    android.system.Os.remove(destFile.absolutePath)
                                } catch (_: Exception) {
                                    destFile.delete()
                                }
                                var linked = false
                                try {
                                    android.system.Os.link(sourceFile.absolutePath, destFile.absolutePath)
                                    linked = true
                                } catch (_: Exception) {
                                    // Hard links fail on Android due to SELinux restrictions.
                                    // Create a relative symlink first to avoid duplicating large multicall binaries (e.g. uutils/rust-coreutils)
                                    try {
                                        val parent = destFile.parentFile ?: targetDir
                                        val relPath = sourceFile.relativeTo(parent).path
                                        android.system.Os.symlink(relPath, destFile.absolutePath)
                                        linked = true
                                    } catch (_: Exception) {
                                        try {
                                            sourceFile.copyTo(destFile, overwrite = true)
                                        } catch (_: Exception) {}
                                    }
                                }
                                val isExec = (mode and 0x49L) != 0L || entryName.contains("bin/") || entryName.endsWith(".sh") || sourceFile.canExecute()
                                if (isExec) {
                                    destFile.setExecutable(true, false)
                                }
                                destFile.setReadable(true, false)
                            }
                            val remainder = (512 - (size % 512)) % 512
                            if (remainder > 0) skipBytes(stream, remainder)
                        }

                        '2' -> {
                            // Symbolic Link
                            destFile.parentFile?.mkdirs()
                            if (finalLink.isNotEmpty()) {
                                val isTopLevel = (destFile.parentFile?.absolutePath == targetDir.absolutePath)
                                val isAbsoluteRootfsPath =
                                    finalLink.startsWith("usr/") || finalLink.startsWith("etc/") || finalLink.startsWith("var/") || finalLink.startsWith("opt/")
                                val linkTarget = if (!isTopLevel && isAbsoluteRootfsPath) {
                                    "/$finalLink"
                                } else {
                                    finalLink
                                }
                                try {
                                    try {
                                        android.system.Os.remove(destFile.absolutePath)
                                    } catch (_: Exception) {
                                        destFile.delete()
                                    }
                                    android.system.Os.symlink(linkTarget, destFile.absolutePath)
                                } catch (e: Exception) {
                                    Log.w(TAG, "Symlink creation failed for ${destFile.name} -> $linkTarget: ${e.message}")
                                }
                            }
                            val remainder = (512 - (size % 512)) % 512
                            if (remainder > 0) skipBytes(stream, remainder)
                        }

                        else -> {
                            val remainder = (512 - (size % 512)) % 512
                            skipBytes(stream, size + remainder)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error extracting entry $entryName: ${e.message}")
                    val remainder = (512 - (size % 512)) % 512
                    skipBytes(stream, size + remainder)
                }

                extractedFiles++
                val now = System.currentTimeMillis()
                if (onProgress != null && (now - lastUpdate > 250 || extractedFiles % 500 == 0)) {
                    lastUpdate = now
                    val fName = entryName.substringAfterLast('/')
                    val percent = 50 + ((extractedFiles * 35) / 50000).coerceIn(0, 35)
                    onProgress("Extracting: $fName ($extractedFiles files)", percent)
                }
            }
        }
        Log.d(TAG, "Java archive extraction completed: $extractedFiles total files extracted to ${targetDir.absolutePath}")
    }

    private fun parseOctal(buffer: ByteArray, offset: Int, length: Int): Long {
        var result = 0L
        val end = offset + length
        for (i in offset until end) {
            val b = buffer[i].toInt() and 0xFF
            if (b == 0 || b == ' '.code) continue
            if (b in '0'.code..'7'.code) {
                result = (result shl 3) + (b - '0'.code)
            }
        }
        return result
    }

    private fun readFully(input: java.io.InputStream, buffer: ByteArray) {
        var read = 0
        while (read < buffer.size) {
            val r = input.read(buffer, read, buffer.size - read)
            if (r == -1) break
            read += r
        }
    }

    private fun copyBytes(input: java.io.InputStream, output: FileOutputStream, count: Long) {
        var remaining = count
        val buf = ByteArray(65536)
        while (remaining > 0) {
            val toRead = minOf(buf.size.toLong(), remaining).toInt()
            val r = input.read(buf, 0, toRead)
            if (r == -1) break
            output.write(buf, 0, r)
            remaining -= r
        }
    }

    private fun skipBytes(input: java.io.InputStream, count: Long) {
        var remaining = count
        val buf = ByteArray(65536)
        while (remaining > 0) {
            val toRead = minOf(buf.size.toLong(), remaining).toInt()
            val r = input.read(buf, 0, toRead)
            if (r == -1) break
            remaining -= r
        }
    }
}
