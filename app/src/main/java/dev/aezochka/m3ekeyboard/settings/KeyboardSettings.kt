package dev.aezochka.m3ekeyboard.settings

import android.content.Context

data class KeyboardPrefs(
    val haptics: Boolean = true,
    val keyHeightScale: Float = 1f,
    val dynamicColors: Boolean = true,
    val layout: String = "en",
)

object KeyboardSettings {
    private const val PREFS = "m3e_keyboard_prefs"

    fun read(context: Context): KeyboardPrefs {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return KeyboardPrefs(
            haptics = p.getBoolean("haptics", true),
            keyHeightScale = p.getFloat("key_height_scale", 1f),
            dynamicColors = p.getBoolean("dynamic_colors", true),
            layout = p.getString("layout", "en") ?: "en",
        )
    }

    fun write(context: Context, prefs: KeyboardPrefs) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("haptics", prefs.haptics)
            .putFloat("key_height_scale", prefs.keyHeightScale)
            .putBoolean("dynamic_colors", prefs.dynamicColors)
            .putString("layout", prefs.layout)
            .apply()
    }
}
