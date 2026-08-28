package com.devwithzachary.completelinuxinstaller.engine

import android.content.Context
import android.util.Log
import com.devwithzachary.completelinuxinstaller.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val publishedAt: String
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val release: GitHubRelease,
        val currentVersion: String,
        val isNewer: Boolean
    ) : UpdateCheckResult()

    data class UpToDate(val currentVersion: String) : UpdateCheckResult()

    data class Error(val message: String) : UpdateCheckResult()
}

class GitHubReleaseManager(private val context: Context) {

    companion object {
        private const val TAG = "GitHubReleaseManager"
        const val REPO_OWNER = "devwithzachary"
        const val REPO_NAME = "LinuxOnAndroid"
        const val API_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
        const val GITHUB_RELEASES_WEB_URL = "https://github.com/$REPO_OWNER/$REPO_NAME/releases"

        const val PREFS_NAME = "github_update_prefs"
        const val KEY_UPDATE_CHECK_ENABLED = "update_check_enabled"
        const val KEY_DISMISSED_TAG = "dismissed_tag"
        const val KEY_LAST_CHECK_TIME = "last_check_time"
        const val THROTTLE_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours

        /**
         * Compares two semantic version strings.
         * Returns positive number if v1 > v2, negative if v1 < v2, and 0 if v1 == v2.
         */
        fun compareVersions(version1: String, version2: String): Int {
            val v1Parts = parseSemVer(version1)
            val v2Parts = parseSemVer(version2)

            for (i in 0 until 3) {
                val diff = v1Parts[i].compareTo(v2Parts[i])
                if (diff != 0) return diff
            }
            return 0
        }

        /**
         * Parses a version string like "v1.4.0", "1.4.1", "2.0.0-rc1" into [major, minor, patch].
         */
        fun parseSemVer(versionStr: String): IntArray {
            val clean = versionStr.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore("-")
                .substringBefore("+")

            val parts = clean.split(".")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0

            return intArrayOf(major, minor, patch)
        }

        /**
         * Parses a GitHub release API JSON response string into a [GitHubRelease] object.
         */
        fun parseReleaseJson(jsonString: String): GitHubRelease {
            val json = JSONObject(jsonString)
            val tagName = json.optString("tag_name", "")
            val name = json.optString("name", tagName)
            val body = json.optString("body", "")
            val htmlUrl = json.optString("html_url", GITHUB_RELEASES_WEB_URL)
            val publishedAt = json.optString("published_at", "")

            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i)
                    if (asset != null) {
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            val downloadUrl = asset.optString("browser_download_url", "")
                            if (downloadUrl.isNotBlank()) {
                                apkUrl = downloadUrl
                                break
                            }
                        }
                    }
                }
            }

            return GitHubRelease(
                tagName = tagName,
                name = name,
                body = body,
                htmlUrl = htmlUrl,
                apkDownloadUrl = apkUrl ?: htmlUrl,
                publishedAt = publishedAt
            )
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isUpdateCheckEnabled(): Boolean {
        return prefs.getBoolean(KEY_UPDATE_CHECK_ENABLED, true)
    }

    fun setUpdateCheckEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_UPDATE_CHECK_ENABLED, enabled).apply()
    }

    fun getLastCheckTime(): Long {
        return prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
    }

    fun setLastCheckTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_CHECK_TIME, time).apply()
    }

    fun getDismissedTag(): String? {
        return prefs.getString(KEY_DISMISSED_TAG, null)
    }

    fun dismissRelease(tagName: String) {
        prefs.edit().putString(KEY_DISMISSED_TAG, tagName).apply()
    }

    fun clearDismissedRelease() {
        prefs.edit().remove(KEY_DISMISSED_TAG).apply()
    }

    fun isReleaseDismissed(tagName: String): Boolean {
        val dismissed = getDismissedTag()
        return dismissed != null && dismissed.equals(tagName, ignoreCase = true)
    }

    /**
     * Checks GitHub for the latest release.
     *
     * @param force If true, bypasses throttle check and dismissed release check.
     * @param currentVersion The active app version to compare against (defaults to BuildConfig.VERSION_NAME).
     */
    suspend fun checkForUpdates(
        force: Boolean = false,
        currentVersion: String = BuildConfig.VERSION_NAME
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!force && !isUpdateCheckEnabled()) {
            return@withContext UpdateCheckResult.UpToDate(currentVersion)
        }

        val lastCheck = getLastCheckTime()
        val now = System.currentTimeMillis()
        if (!force && (now - lastCheck) < THROTTLE_INTERVAL_MS) {
            // Throttled: don't bombard GitHub API on every cold start
            return@withContext UpdateCheckResult.UpToDate(currentVersion)
        }

        try {
            Log.d(TAG, "Checking GitHub API for latest release at: $API_URL")
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "LinuxOnAndroid-App/$currentVersion")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                connection.disconnect()

                setLastCheckTime(now)
                val release = parseReleaseJson(response)

                if (release.tagName.isBlank()) {
                    return@withContext UpdateCheckResult.UpToDate(currentVersion)
                }

                val comparison = compareVersions(release.tagName, currentVersion)
                if (comparison > 0) {
                    if (!force && isReleaseDismissed(release.tagName)) {
                        Log.d(TAG, "New release ${release.tagName} found but previously dismissed by user.")
                        return@withContext UpdateCheckResult.UpToDate(currentVersion)
                    }
                    Log.d(TAG, "Newer release found: ${release.tagName} (installed: $currentVersion)")
                    return@withContext UpdateCheckResult.UpdateAvailable(
                        release = release,
                        currentVersion = currentVersion,
                        isNewer = true
                    )
                } else {
                    Log.d(TAG, "Installed version $currentVersion is up to date with latest GitHub release ${release.tagName}")
                    return@withContext UpdateCheckResult.UpToDate(currentVersion)
                }
            } else if (responseCode == 404) {
                // No releases found yet
                connection.disconnect()
                return@withContext UpdateCheckResult.UpToDate(currentVersion)
            } else {
                connection.disconnect()
                val errorMsg = "GitHub API returned HTTP $responseCode (${connection.responseMessage})"
                Log.w(TAG, errorMsg)
                return@withContext UpdateCheckResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for GitHub updates", e)
            return@withContext UpdateCheckResult.Error(e.localizedMessage ?: "Network connection failed")
        }
    }
}
