package com.devwithzachary.completelinuxinstaller.engine

import org.apache.commons.codec.digest.Crypt

/**
 * Standard Unix crypt hash verification.
 * Supports:
 * - SHA-512 crypt ($6$) - Ulrich Drepper's specification (default in modern glibc / musl)
 * - SHA-256 crypt ($5$)
 * - MD5 crypt ($1$)
 * - Traditional DES crypt
 * - Plaintext fallback
 */
object UnixCrypt {

    fun canVerifyInKotlin(hash: String): Boolean {
        return hash.startsWith("$6$") || hash.startsWith("$5$") || hash.startsWith("$1$") || !hash.startsWith("$")
    }

    fun verify(password: String, storedHash: String): Boolean {
        if (storedHash.isEmpty() || storedHash == "*" || storedHash == "!" || storedHash.startsWith("!") || storedHash == "x") {
            return true
        }
        return try {
            when {
                storedHash.startsWith("$6$") || storedHash.startsWith("$5$") || storedHash.startsWith("$1$") -> {
                    val computed = Crypt.crypt(password, storedHash)
                    computed == storedHash
                }
                !storedHash.startsWith("$") -> {
                    if (storedHash.length == 13) {
                        // Traditional DES crypt
                        val computed = Crypt.crypt(password, storedHash)
                        computed == storedHash
                    } else {
                        password == storedHash
                    }
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }
}
