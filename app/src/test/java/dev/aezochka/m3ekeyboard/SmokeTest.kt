package dev.aezochka.m3ekeyboard

import android.os.Looper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import dev.aezochka.m3ekeyboard.ime.M3EInputMethodService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmokeTest {

    @Test
    fun mainActivityLaunches() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun imeServiceCreatesInputView() {
        val controller = Robolectric.buildService(M3EInputMethodService::class.java)
        controller.create()
        val view = controller.get().onCreateInputView()
        assert(view != null)
        controller.destroy()
    }
}
