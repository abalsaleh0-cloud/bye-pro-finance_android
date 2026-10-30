package com.example

import android.os.Looper
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivityLaunchTest {

    @Test
    fun mainActivityCreatesContentRootWithoutStartupCrash() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        shadowOf(Looper.getMainLooper()).idle()

        val content = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        assertNotNull(activity)
        assertFalse(activity.isFinishing)
        assertNotNull(content)
        assertTrue("Compose content root must be attached", content.childCount > 0)

        controller.pause().stop().destroy()
    }
}
