package dev.aezochka.m3ekeyboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.aezochka.m3ekeyboard.ui.M3EKeyboardTheme
import dev.aezochka.m3ekeyboard.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            M3EKeyboardTheme(dynamic = true) {
                SettingsScreen()
            }
        }
    }
}
