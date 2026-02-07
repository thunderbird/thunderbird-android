package app.k9mail.core.ui.compose.common.window

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric

class FoldableStateObserverTest : RobolectricTest() {

    private lateinit var activity: Activity
    private lateinit var testLogger: TestLogger
    private lateinit var lifecycleOwner: TestLifecycleOwner
    private lateinit var testSubject: FoldableStateObserver

    @Before
    fun setup() {
        // Arrange
        FoldableStateObserver.resetStateForTesting()
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        testLogger = TestLogger()
        lifecycleOwner = TestLifecycleOwner()
        testSubject = FoldableStateObserver(activity, testLogger)
    }

    @Test
    fun `initial state should be UNKNOWN`() {
        // Act
        val result = testSubject.currentState

        // Assert
        assertThat(result).isEqualTo(FoldableState.UNKNOWN)
    }

    @Test
    fun `state should remain UNKNOWN when observer is not started`() {
        // Arrange - observer not started

        // Act
        val result = testSubject.currentState

        // Assert
        assertThat(result).isEqualTo(FoldableState.UNKNOWN)
    }

    @Test
    fun `state should be UNKNOWN when no folding feature is present on non-foldable device`() {
        // Arrange
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        testSubject.onCreate(lifecycleOwner)
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        testSubject.onStart(lifecycleOwner)

        // Act
        val result = testSubject.currentState

        // Assert
        assertThat(result).isEqualTo(FoldableState.UNKNOWN)
    }

    @Test
    fun `observer should log when starting observation`() {
        // Arrange
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        testSubject.onCreate(lifecycleOwner)

        // Act
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        testSubject.onStart(lifecycleOwner)

        // Assert
        assertThat(testLogger.events).isNotEmpty()
    }

    @Test
    fun `observer should log when stopping observation`() {
        // Arrange
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        testSubject.onCreate(lifecycleOwner)
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        testSubject.onStart(lifecycleOwner)
        val eventsBeforeStop = testLogger.events.size

        // Act
        lifecycleOwner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        testSubject.onStop(lifecycleOwner)

        // Assert
        assertThat(testLogger.events.size).isEqualTo(eventsBeforeStop + 1)
    }

    @Test
    fun `foldableState flow should emit initial state`() {
        // Act
        val result = testSubject.foldableState.value

        // Assert
        assertThat(result).isEqualTo(FoldableState.UNKNOWN)
    }

    /**
     * Test lifecycle owner for testing lifecycle-aware components.
     */
    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)

        override val lifecycle: Lifecycle = registry
    }
}
