package net.thunderbird.app.common.feature.applock

import android.app.Activity
import android.os.Build
import androidx.fragment.app.FragmentActivity
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.single
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.feature.applock.api.AppLockGate
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class AppLockActivityLifecycleCallbacksTest : RobolectricTest() {

    @Test
    fun `should not crash when factory is null`() {
        val testSubject = AppLockActivityLifecycleCallbacks(gateFactory = null)
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()

        testSubject.onActivityCreated(activity, null)
    }

    @Test
    fun `should create gate for FragmentActivity`() {
        val fakeFactory = FakeAppLockGateFactory()
        val testSubject = AppLockActivityLifecycleCallbacks(gateFactory = fakeFactory)
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()

        testSubject.onActivityCreated(activity, null)

        assertThat(fakeFactory.createdActivities).single().isEqualTo(activity)
    }

    @Test
    fun `should not create gate for plain Activity`() {
        val fakeFactory = FakeAppLockGateFactory()
        val testSubject = AppLockActivityLifecycleCallbacks(gateFactory = fakeFactory)
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()

        testSubject.onActivityCreated(activity, null)

        assertThat(fakeFactory.createdActivities).isEmpty()
    }

    private class FakeAppLockGateFactory : AppLockGate.Factory {
        val createdActivities = mutableListOf<FragmentActivity>()

        override fun create(activity: FragmentActivity): AppLockGate {
            createdActivities.add(activity)
            return FakeAppLockGate()
        }
    }

    private class FakeAppLockGate : AppLockGate
}
