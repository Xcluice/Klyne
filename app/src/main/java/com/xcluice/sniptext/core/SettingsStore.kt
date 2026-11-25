package com.xcluice.sniptext.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class GrabHistoryItem(
    val id: Long,
    val text: String,
    val timestamp: Long
)

data class SnipSettings(
    val autoCopy: Boolean = false,
    val joinLineBreaks: Boolean = true,
    val instantGrab: Boolean = true,
    val hapticEnabled: Boolean = true,
    val chordWindowMs: Int = 220
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("sniptext_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): SnipSettings = SnipSettings(
        autoCopy = prefs.getBoolean(KEY_AUTO_COPY, false),
        joinLineBreaks = prefs.getBoolean(KEY_JOIN_LINES, true),
        instantGrab = prefs.getBoolean(KEY_INSTANT_GRAB, true),
        hapticEnabled = prefs.getBoolean(KEY_HAPTIC, true),
        chordWindowMs = prefs.getInt(KEY_CHORD_MS, 220).coerceIn(120, 400)
    )

    fun saveSettings(settings: SnipSettings) {
        prefs.edit()
            .putBoolean(KEY_AUTO_COPY, settings.autoCopy)
            .putBoolean(KEY_JOIN_LINES, settings.joinLineBreaks)
            .putBoolean(KEY_INSTANT_GRAB, settings.instantGrab)
            .putBoolean(KEY_HAPTIC, settings.hapticEnabled)
            .putInt(KEY_CHORD_MS, settings.chordWindowMs)
            .apply()
    }

    fun loadHistory(): List<GrabHistoryItem> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        GrabHistoryItem(
                            id = obj.getLong("id"),
                            text = obj.getString("text"),
                            timestamp = obj.getLong("timestamp")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addHistory(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val current = loadHistory().filterNot { it.text == trimmed }.toMutableList()
        current.add(0, GrabHistoryItem(System.nanoTime(), trimmed, System.currentTimeMillis()))
        val capped = current.take(25)
        saveHistoryList(capped)
    }

    fun deleteHistory(id: Long) {
        saveHistoryList(loadHistory().filterNot { it.id == id })
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun saveHistoryList(items: List<GrabHistoryItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("timestamp", item.timestamp)
                }
            )
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    companion object {
        private const val KEY_AUTO_COPY = "auto_copy"
        private const val KEY_JOIN_LINES = "join_lines"
        private const val KEY_INSTANT_GRAB = "instant_grab"
        private const val KEY_HAPTIC = "haptic"
        private const val KEY_CHORD_MS = "chord_ms"
        private const val KEY_HISTORY = "grab_history"
    }
}
