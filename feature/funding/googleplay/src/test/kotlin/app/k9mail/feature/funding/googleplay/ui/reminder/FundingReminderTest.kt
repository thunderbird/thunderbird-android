package app.k9mail.feature.funding.googleplay.ui.reminder

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import app.k9mail.core.testing.TestClock
import app.k9mail.feature.funding.api.FundingSettings
import app.k9mail.feature.funding.googleplay.ui.reminder.FundingReminderContract.Dialog
import app.k9mail.feature.funding.googleplay.ui.reminder.FundingReminderContract.ActivityLifecycleObserver
import app.k9mail.feature.funding.googleplay.ui.reminder.FundingReminderContract.FragmentLifecycleObserver
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.datetime.Instant
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class FundingReminderTest {

    @Test
    fun `should set reference timestamp when not set`() {
        val activity = createTestActivity()
        val settings = FakeFundingSettings(reminderReferenceTimestamp = REMINDER_REFERENCE_TIMESTAMP_UNSET)
        val fragmentObserver = FakeFragmentLifecycleObserver()
        val activityObserver = FakeActivityLifecycleObserver()
        val testSubject = createTestSubject(
            settings,
            fragmentObserver,
            activityObserver,
        )

        testSubject.registerReminder(activity) { }

        assertThat(settings.getReminderReferenceTimestamp()).isEqualTo(REMINDER_REFERENCE_TIMESTAMP)
        assertThat(fragmentObserver.isRegistered).isFalse()
        assertThat(activityObserver.isRegistered).isTrue()
    }

    @Test
    fun `should not set reference timestamp when already set`() {
        val activity = createTestActivity(
            installTime = 2000L,
        )
        val settings = FakeFundingSettings(reminderReferenceTimestamp = REMINDER_REFERENCE_TIMESTAMP)
        val fragmentObserver = FakeFragmentLifecycleObserver()
        val activityObserver = FakeActivityLifecycleObserver()
        val testSubject = createTestSubject(
            settings,
            fragmentObserver,
            activityObserver,
        )

        testSubject.registerReminder(activity) { }

        assertThat(settings.getReminderReferenceTimestamp()).isEqualTo(REMINDER_REFERENCE_TIMESTAMP)
        assertThat(fragmentObserver.isRegistered).isFalse()
        assertThat(activityObserver.isRegistered).isTrue()
    }

    @Test
    fun `should not register reminder when reminder was shown`() {
        val activity = createTestActivity()
        val settings = FakeFundingSettings(
            reminderReferenceTimestamp = REMINDER_REFERENCE_TIMESTAMP,
            reminderShownTimestamp = REMINDER_SHOWN_TIMESTAMP,
        )
        val fragmentObserver = FakeFragmentLifecycleObserver()
        val activityObserver = FakeActivityLifecycleObserver()
        val testSubject = createTestSubject(
            settings,
            fragmentObserver,
            activityObserver,
        )

        testSubject.registerReminder(activity) { }

        assertThat(fragmentObserver.isRegistered).isFalse()
        assertThat(activityObserver.isRegistered).isTrue()
    }

    @Test
    fun `should register reminder when reminder was not shown and conditions met`() {
        val activity = createTestActivity()
        val settings = FakeFundingSettings(
            reminderReferenceTimestamp = REMINDER_REFERENCE_TIMESTAMP,
            reminderShownTimestamp = REMINDER_SHOWN_TIMESTAMP_UNSET,
            activityCounterInMillis = FUNDING_REMINDER_MIN_ACTIVITY_MILLIS,
        )
        val currentTime = REMINDER_REFERENCE_TIMESTAMP + FUNDING_REMINDER_DELAY_MILLIS
        val fragmentObserver = FakeFragmentLifecycleObserver()
        val activityObserver = FakeActivityLifecycleObserver()
        var dialogShown = false
        val testSubject = createTestSubject(
            settings = settings,
            fragmentObserver = fragmentObserver,
            activityObserver = activityObserver,
            dialog = { dialogShown = true },
            clock = TestClock(Instant.fromEpochMilliseconds(currentTime)),
        )

        testSubject.registerReminder(activity) { }

        assertThat(dialogShown).isEqualTo(true)
        assertThat(fragmentObserver.isRegistered).isTrue()
        assertThat(activityObserver.isRegistered).isTrue()
        assertThat(settings.getReminderShownTimestamp()).isEqualTo(currentTime)
    }

    private fun createTestActivity(
        installTime: Long = INSTALL_TIME,
        packageManager: PackageManager = createPackageManager(installTime),
        testLifecycleOwner: LifecycleOwner = TestLifecycleOwner(),
    ): AppCompatActivity {
        val activity = mock<AppCompatActivity>()
        whenever(activity.supportFragmentManager).thenReturn(mock())
        whenever(activity.packageManager).thenReturn(packageManager)
        whenever(activity.packageName).thenReturn(PACKAGE_NAME)
        whenever(activity.lifecycle).thenReturn(testLifecycleOwner.lifecycle)
        return activity
    }

    private fun createPackageManager(
        installTime: Long,
        packageInfo: PackageInfo = createPackageInfo(installTime),
    ): PackageManager {
        val packageManager = mock<PackageManager>()
        whenever(packageManager.getPackageInfo(PACKAGE_NAME, 0)).thenReturn(packageInfo)
        return packageManager
    }

    private fun createPackageInfo(
        installTime: Long,
    ): PackageInfo = PackageInfo().apply {
        firstInstallTime = installTime
    }

    private fun createTestSubject(
        settings: FundingSettings,
        fragmentObserver: FragmentLifecycleObserver,
        activityObserver: ActivityLifecycleObserver,
        dialog: Dialog = Dialog { },
        clock: TestClock = TestClock(Instant.fromEpochMilliseconds(0)),
    ): FundingReminder {
        return FundingReminder(
            settings = settings,
            fragmentObserver = fragmentObserver,
            activityCounterObserver = activityObserver,
            dialog = dialog,
            clock = clock,
        )
    }

    private companion object {
        const val PACKAGE_NAME = "test.package.name"

        const val INSTALL_TIME = 1000L
        const val REMINDER_REFERENCE_TIMESTAMP_UNSET = 0L
        const val REMINDER_REFERENCE_TIMESTAMP = 1000L
        const val REMINDER_SHOWN_TIMESTAMP_UNSET = 0L
        const val REMINDER_SHOWN_TIMESTAMP = 1111L

        const val FUNDING_REMINDER_DELAY_MILLIS = 7 * 24 * 60 * 60 * 1000L
        const val FUNDING_REMINDER_MIN_ACTIVITY_MILLIS = 30 * 60 * 1000L
    }
}

