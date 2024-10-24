package app.k9mail.feature.funding.googleplay.ui.reminder

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import app.k9mail.core.testing.TestClock
import app.k9mail.feature.funding.api.FundingSettings
import app.k9mail.feature.funding.googleplay.ui.reminder.FundingReminderContract.Dialog
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.datetime.Instant
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FundingReminderTest {

    @Test
    fun `should set reference timestamp when not set`() {
        val activity = createTestActivity()
        val settings = FakeFundingSettings(reminderReferenceTimestamp = REMINDER_REFERENCE_TIMESTAMP_UNSET)
        val testSubject = createTestSubject(
            settings,
        )

        testSubject.registerReminder(activity) { }

        assertThat(settings.getReminderReferenceTimestamp()).isEqualTo(REMINDER_REFERENCE_TIMESTAMP)
    }

    @Test
    fun `should not set reference timestamp when already set`() {
        val activity = createTestActivity(
            installTime = 2000L,
        )
        val settings = FakeFundingSettings(reminderReferenceTimestamp = REMINDER_REFERENCE_TIMESTAMP)
        val testSubject = createTestSubject(
            settings,
        )

        testSubject.registerReminder(activity) { }

        assertThat(settings.getReminderReferenceTimestamp()).isEqualTo(REMINDER_REFERENCE_TIMESTAMP)
    }

    @Test
    fun `should not register reminder when reminder was shown`() {
        val activity = createTestActivity()
        val settings = FakeFundingSettings(
            reminderReferenceTimestamp = REMINDER_REFERENCE_TIMESTAMP,
            reminderShownTimestamp = REMINDER_SHOWN_TIMESTAMP,
        )
        val testSubject = createTestSubject(settings)

        testSubject.registerReminder(activity) { }

        verifyNoInteractions(activity)
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
        var dialogShown = false
        val testSubject = createTestSubject(
            settings = settings,
            dialog = { _, _ -> dialogShown = true },
            clock = TestClock(Instant.fromEpochMilliseconds(currentTime)),
        )

        testSubject.registerReminder(activity) { }

        assertThat(dialogShown).isEqualTo(true)
    }

    private fun createTestActivity(
        installTime: Long = INSTALL_TIME,
        packageManager: PackageManager = createPackageManager(installTime),
        testLifecycleOwner: LifecycleOwner = TestLifecycleOwner(),
    ): AppCompatActivity {
        val activity = mock<AppCompatActivity>()
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
        dialog: Dialog = Dialog { _, _ -> },
        clock: TestClock = TestClock(Instant.fromEpochMilliseconds(0)),
    ): FundingReminder {
        return FundingReminder(
            settings = settings,
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

