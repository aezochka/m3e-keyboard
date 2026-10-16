package dev.aezochka.m3ekeyboard

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.aezochka.m3ekeyboard.ui.CrashReportScreen
import dev.aezochka.m3ekeyboard.ui.M3EKeyboardTheme
import dev.aezochka.m3ekeyboard.ui.SettingsScreen
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashHandler()
        var crash by mutableStateOf(readLastCrash())
        setContent {
            M3EKeyboardTheme(dynamic = true) {
                val current = crash
                if (current != null) {
                    CrashReportScreen(
                        trace = current,
                        onDismiss = {
                            crashFile().delete()
                            crash = null
                        },
                    )
                } else {
                    SettingsScreen()
                }
            }
        }
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                crashFile().writeText(Log.getStackTraceString(throwable))
            } catch (_: Exception) {
                // never mask the original crash
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun crashFile(): File = File(filesDir, "last_crash.txt")

    private fun readLastCrash(): String? = try {
        val file = crashFile()
        if (file.exists()) file.readText() else null
    } catch (_: Exception) {
        null
    }
}
