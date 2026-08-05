package com.devwithzachary.completelinuxinstaller.util

import android.content.Context
import org.json.JSONArray

object HotkeyManager {
    private const val PREFS_NAME = "terminal_prefs"
    const val KEY_CUSTOM_HOTKEYS = "custom_hotkeys"

    val DEFAULT_HOTKEYS = listOf(
        "Ctrl+C", "Ctrl+Z", "Ctrl+D", "Tab", "Esc",
        "▲", "▼", "◄", "►", "|", "~", "/", "-", "_",
        "clear", "htop", "uname -a", "df -h"
    )

    fun getHotkeys(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CUSTOM_HOTKEYS, null) ?: return DEFAULT_HOTKEYS
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            if (list.isEmpty()) DEFAULT_HOTKEYS else list
        } catch (_: Exception) {
            DEFAULT_HOTKEYS
        }
    }

    fun saveHotkeys(context: Context, hotkeys: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        hotkeys.forEach { jsonArray.put(it) }
        prefs.edit().putString(KEY_CUSTOM_HOTKEYS, jsonArray.toString()).apply()
    }

    fun resetHotkeys(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CUSTOM_HOTKEYS).apply()
        return DEFAULT_HOTKEYS
    }
}
