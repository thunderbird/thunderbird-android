package net.thunderbird.app.common.activity

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.lifecycle.ProcessLifecycleOwner
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment

class DefaultActivityProviderTest : RobolectricTest() {

    private val application = RuntimeEnvironment.getApplication()
    private val logger = TestLogger()

    @Test
    fun `getCurrent returns null initially`() {
        // Arrange
        val testSubject = DefaultActivityProvider(
            application = application,
            logger = logger,
        )

        // Assert
        assertThat(testSubject.getCurrent()).isNull()
    }

    @Test
    fun `tracks current activity when in foreground and clears when backgrounded`() {
        // Arrange
        val testSubject = DefaultActivityProvider(
            application = application,
            logger = logger,
        )

        val controller = Robolectric.buildActivity(Activity::class.java)

        // Act: bring app/activity to foreground
        testSubject.onStart(ProcessLifecycleOwner.get())
        val activity = controller.create().start().resume().get()

        // Assert: provider returns the resumed activity
        assertThat(testSubject.getCurrent()).isEqualTo(activity)

        // Act: pause activity (still foreground process-wise)
        controller.pause()

        // Assert: returns null when activity is paused
        assertThat(testSubject.getCurrent()).isNull()

        // Act: resume activity
        controller.resume()
        assertThat(testSubject.getCurrent()).isEqualTo(activity)

        // Act: stop activity (app goes to background)
        controller.stop()
        // Manually move the process to background
        testSubject.onStop(ProcessLifecycleOwner.get())

        // Assert: returns null when app is in background
        assertThat(testSubject.getCurrent()).isNull()

        // Act: destroy activity
        controller.destroy()
        assertThat(testSubject.getCurrent()).isNull()
    }

    @Test
    fun `tracks multiple activities and logs transitions`() {
        // Arrange
        val testSubject = DefaultActivityProvider(
            application = application,
            logger = logger,
        )
        val controller1 = Robolectric.buildActivity(Activity::class.java)
        val controller2 = Robolectric.buildActivity(ComponentActivity::class.java)

        // Act: Start first activity and move process to foreground
        testSubject.onStart(ProcessLifecycleOwner.get())
        val activity1 = controller1.create().start().resume().get()

        // Assert: First activity is current
        assertThat(testSubject.getCurrent()).isEqualTo(activity1)
        assertThat(logger.events).contains(
            LogEvent(
                level = LogLevel.DEBUG,
                tag = "DefaultActivityProvider",
                message = "onActivityResumed: setting activity to Activity",
                timestamp = TestLogger.TIMESTAMP,
            ),
        )

        // Act: Start second activity
        val activity2 = controller2.create().start().resume().get()

        // Assert: Second activity is now current
        assertThat(testSubject.getCurrent()).isEqualTo(activity2)
        assertThat(logger.events).contains(
            LogEvent(
                level = LogLevel.DEBUG,
                tag = "DefaultActivityProvider",
                message = "onActivityResumed: setting activity to ComponentActivity",
                timestamp = TestLogger.TIMESTAMP,
            ),
        )

        // Act: Destroy second activity
        controller2.pause().stop().destroy()

        // Assert: Should clear current activity as it was the last resumed
        assertThat(testSubject.getCurrent()).isNull()
        assertThat(logger.events).contains(
            LogEvent(
                level = LogLevel.DEBUG,
                tag = "DefaultActivityProvider",
                message = "onActivityPaused: clearing current activity ComponentActivity",
                timestamp = TestLogger.TIMESTAMP,
            ),
        )
    }
}
