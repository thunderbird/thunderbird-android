package app.k9mail.feature.funding.googleplay.ui.reminder

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.testing.TestClock
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import org.junit.Rule

@OptIn(ExperimentalTime::class)
class ActivityLifecycleObserverTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should add lifecycle observer when register is called`() {
        val settings = FakeFundingSettings()
        val observer = ActivityLifecycleObserver(settings)
        val owner = TestLifecycleOwner()

        observer.register(owner.lifecycle) {}

        assertThat(owner.lifecycle.observerCount).isEqualTo(1)
    }

    @Test
    fun `should remove lifecycle observer when unregister is called`() {
        val settings = FakeFundingSettings()
        val observer = ActivityLifecycleObserver(settings)
        val owner = TestLifecycleOwner()

        observer.register(owner.lifecycle) {}
        observer.unregister(owner.lifecycle)

        assertThat(owner.lifecycle.observerCount).isEqualTo(0)
    }

    @Test
    fun `should update activity counter on pause`() = runTest {
        val settings = FakeFundingSettings(
            activityCounterInMillis = 0L,
        )
        val startTime = Instant.fromEpochMilliseconds(1000L)
        val clock = TestClock(startTime)
        val observer = ActivityLifecycleObserver(settings, clock)
        val owner = TestLifecycleOwner()

        observer.register(owner.lifecycle) {}

        owner.setCurrentState(Lifecycle.State.RESUMED)
        clock.changeTimeTo(Instant.fromEpochMilliseconds(2000L))
        owner.setCurrentState(Lifecycle.State.STARTED)

        assertThat(settings.getActivityCounterInMillis()).isEqualTo(1000L)
    }

    @Test
    fun `should call onDestroy when lifecycle is onDestroyed`() = runTest {
        val settings = FakeFundingSettings()
        val observer = ActivityLifecycleObserver(settings)
        val owner = TestLifecycleOwner()
        var onDestroyCalled = false

        observer.register(owner.lifecycle) {
            onDestroyCalled = true
        }

        owner.setCurrentState(Lifecycle.State.DESTROYED)

        assertThat(onDestroyCalled).isEqualTo(true)
    }
}
