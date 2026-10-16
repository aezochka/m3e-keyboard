package dev.aezochka.m3ekeyboard.settings

import android.content.Context

data class KeyboardPrefs(
    val haptics: Boolean = true,
    val sound: Boolean = false,
    val keyHeightScale: Float = 1f,
    val dynamicColors: Boolean = true,
    val accentKeys: Boolean = false,
    val layout: String = "en",
)

object KeyboardSettings {
    private const val PREFS = "m3e_keyboard_prefs"

    fun read(context: Context): KeyboardPrefs {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return KeyboardPrefs(
            haptics = p.getBoolean("haptics", true),
            sound = p.getBoolean("sound", false),
            keyHeightScale = p.getFloat("key_height_scale", 1f),
            dynamicColors = p.getBoolean("dynamic_colors", true),
            accentKeys = p.getBoolean("colored_keys", false),
            layout = p.getString("layout", "en") ?: "en",
        )
    }

    fun write(context: Context, prefs: KeyboardPrefs) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("haptics", prefs.haptics)
            .putBoolean("sound", prefs.sound)
            .putFloat("key_height_scale", prefs.keyHeightScale)
            .putBoolean("dynamic_colors", prefs.dynamicColors)
            .putBoolean("colored_keys", prefs.accentKeys)
            .putString("layout", prefs.layout)
            .apply()
    }
}
