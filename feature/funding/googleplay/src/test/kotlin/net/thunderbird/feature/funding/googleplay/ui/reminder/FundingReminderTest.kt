package net.thunderbird.feature.funding.googleplay.ui.reminder

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import net.thunderbird.core.android.common.activity.ActivityProvider
import net.thunderbird.core.testing.TestClock
import net.thunderbird.feature.funding.api.FundingSettings
import net.thunderbird.feature.funding.common.api.FundingReminderContract
import net.thunderbird.feature.funding.common.api.FundingReminderContract.Dialog
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FundingReminderTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val mainDispatcher = MainDispatcherHelper(UnconfinedTestDispatcher())

    @BeforeTest
    fun setUp() {
        mainDispatcher.setUp()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcher.tearDown()
    }

    @Test
    fun `should set reference timestamp when not set`() {
        val activity = createTestActivity()
        val activityProvider = FakeActivityProvider(activity)
        val settings = FakeFundingSettings(reminderReferenceTimestamp = REMINDER_REFERENCE_TIMESTAMP_UNSET)
        val fragmentObserver = FakeFragmentLifecycleObserver()
        val activityObserver = FakeActivityLifecycleObserver()
        val testSubject = createTestSubject(
            activityProvider = activityProvider,
            settings = settings,
            fragmentObserver = fragmentObserver,
            activityCounterObserver = activityObserver,
            scope = CoroutineScope(mainDispatcher.testDispatcher),
        )

        testSubject.registerReminder { }

        assertThat(settings.getReminderReferenceTimestamp()).isEqualTo(REMINDER_REFERENCE_TIMESTAMP)
        assertThat(fragmentObserver.isRegistered).isFalse()
        assertThat(activityObserver.isRegistered).isTrue()
    }

    @Test
    fun `should not set reference timestamp when already set`() {
        val activity = createTestActivity(
            installTime = 2000L,
        )
        val activityProvider = FakeActivityProvider(activity)
        val settings = FakeFundingSettings(reminderReferenceTimestamp = REMINDER_REFERENCE_TIMESTAMP)
        val fragmentObserver = FakeFragmentLifecycleObserver()
        val activityObserver = FakeActivityLifecycleObserver()
        val testSubject = createTestSubject(
            activityProvider = activityProvider,
            settings = settings,
            fragmentObserver = fragmentObserver,
            activityCounterObserver = activityObserver,
            scope = CoroutineScope(mainDispatcher.testDispatcher),
        )

        testSubject.registerReminder { }

        assertThat(settings.getReminderReferenceTimestamp()).isEqualTo(REMINDER_REFERENCE_TIMESTAMP)
        assertThat(fragmentObserver.isRegistered).isFalse()
        assertThat(activityObserver.isRegistered).isTrue()
    }

    @Test
    fun `should not register reminder when reminder was shown`() {
        val activity = createTestActivity()
        val activityProvider = FakeActivityProvider(activity)
        val settings = FakeFundingSettings(
            reminderReferenceTimestamp = REMINDER_REFERENCE_TIMESTAMP,
            reminderShownTimestamp = REMINDER_SHOWN_TIMESTAMP,
        )
        val fragmentObserver = FakeFragmentLifecycleObserver()
        val activityObserver = FakeActivityLifecycleObserver()
        val testSubject = createTestSubject(
            activityProvider = activityProvider,
            settings = settings,
            fragmentObserver = fragmentObserver,
            activityCounterObserver = activityObserver,
            scope = CoroutineScope(mainDispatcher.testDispatcher),
        )

        testSubject.registerReminder { }

        assertThat(fragmentObserver.isRegistered).isFalse()
        assertThat(activityObserver.isRegistered).isTrue()
    }

    /**
     * An incremented counter should be enough to prevent the reminder from displaying,
     *  even if it would otherwise display
     */
    @Test
    fun `should not register reminder when previously incremented`() {
        val activity = createTestActivity()
        val activityProvider = FakeActivityProvider(activity)
        val settings = FakeFundingSettings(
            reminderReferenceTimestamp = REMINDER_REFERENCE_TIMESTAMP,
            reminderShownTimestamp = REMINDER_SHOWN_TIMESTAMP_UNSET,
            activityCounterInMillis = FUNDING_REMINDER_MIN_ACTIVITY_MILLIS,
            lastReminderShownActivityAmount = LAST_REMINDER_SHOWN_UNSET,
            fundingReminderCount = REMINDER_COUNTER_FIRST_SHOWN,
        )
        val currentTime = REMINDER_REFERENCE_TIMESTAMP + FUNDING_REMINDER_DELAY_MILLIS
        val fragmentObserver = FakeFragmentLifecycleObserver()
        val activityObserver = FakeActivityLifecycleObserver()
        var dialogShown = false
        val testSubject = createTestSubject(
            activityProvider = activityProvider,
            settings = settings,
            fragmentObserver = fragmentObserver,
            activityCounterObserver = activityObserver,
            dialog = { dialogShown = true },
            clock = TestClock(Instant.fromEpochMilliseconds(currentTime)),
            scope = CoroutineScope(mainDispatcher.testDispatcher),
        )

        testSubject.registerReminder { }

        assertThat(dialogShown).isFalse()
        assertThat(fragmentObserver.isRegistered).isFalse()
        assertThat(settings.getReminderShownTimestamp()).isEqualTo(LAST_REMINDER_SHOWN_UNSET)
        assertThat(REMINDER_COUNTER_FIRST_SHOWN).isEqualTo(settings.getReminderShownCount())
    }

    /**
     * A value on the lastReminderShown timestamp should be enough to prevent the reminder from displaying,
     *  even if it would otherwise display
     */
    @Test
    fun `should not register reminder when lastReminderShown timestamp is not zero`() {
        val activity = createTestActivity()
        val activityProvider = FakeActivityProvider(activity)
        val settings = FakeFundingSettings(
            reminderReferenceTimestamp = REMINDER_REFERENCE_TIMESTAMP,
            reminderShownTimestamp = REMINDER_SHOWN_TIMESTAMP_UNSET,
            activityCounterInMillis = FUNDING_REMINDER_MIN_ACTIVITY_MILLIS,
            lastReminderShownActivityAmount = REMINDER_REFERENCE_TIMESTAMP,
            fundingReminderCount = 0,
        )
        val currentTime = REMINDER_REFERENCE_TIMESTAMP + FUNDING_REMINDER_DELAY_MILLIS
        val fragmentObserver = FakeFragmentLifecycleObserver()
        val activityObserver = FakeActivityLifecycleObserver()
        var dialogShown = false
        val testSubject = createTestSubject(
            activityProvider = activityProvider,
            settings = settings,
            fragmentObserver = fragmentObserver,
            activityCounterObserver = activityObserver,
            dialog = { dialogShown = true },
            clock = TestClock(Instant.fromEpochMilliseconds(currentTime)),
            scope = CoroutineScope(mainDispatcher.testDispatcher),
        )

        testSubject.registerReminder { }

        assertThat(dialogShown).isFalse()
        assertThat(fragmentObserver.isRegistered).isFalse()
        assertThat(settings.getReminderShownTimestamp()).isEqualTo(REMINDER_SHOWN_TIMESTAMP_UNSET)
        assertThat(settings.getLastReminderShownActivityAmount()).isEqualTo(REMINDER_REFERENCE_TIMESTAMP)
        assertThat(0).isEqualTo(settings.getReminderShownCount())
    }

    @Test
    fun `should register reminder when reminder was not shown and conditions met`() {
        val activity = createTestActivity()
        val activityProvider = FakeActivityProvider(activity)
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
            activityProvider = activityProvider,
            settings = settings,
            fragmentObserver = fragmentObserver,
            activityCounterObserver = activityObserver,
            dialog = { dialogShown = true },
            clock = TestClock(Instant.fromEpochMilliseconds(currentTime)),
            scope = CoroutineScope(mainDispatcher.testDispatcher),
        )

        // Should not be set until after register reminder called
        assertThat(settings.getReminderShownCount()).isEqualTo(REMINDER_COUNTER_UNSET)
        assertThat(settings.getLastReminderShownActivityAmount()).isEqualTo(REMINDER_SHOWN_TIMESTAMP_UNSET)

        // Test the reminder functionality
        testSubject.registerReminder { }

        assertThat(dialogShown).isEqualTo(true)
        assertThat(fragmentObserver.isRegistered).isTrue()
        assertThat(activityObserver.isRegistered).isTrue()
        assertThat(settings.getLastReminderShownActivityAmount()).isEqualTo(FUNDING_REMINDER_MIN_ACTIVITY_MILLIS)
        assertThat(settings.getReminderShownCount()).isEqualTo(REMINDER_COUNTER_FIRST_SHOWN)
        assertThat(settings.getReminderShownTimestamp()).isEqualTo(currentTime)
    }

    @Test
    fun `second reminder does not show if first has not set timestamp`() {
        val activity = createTestActivity()
        val activityProvider = FakeActivityProvider(activity)
        val settings = FakeFundingSettings(
            reminderReferenceTimestamp = REMINDER_REFERENCE_TIMESTAMP,
            reminderShownTimestamp = REMINDER_SHOWN_TIMESTAMP_UNSET,
            fundingReminderCount = REMINDER_COUNTER_FIRST_SHOWN, // Prevent first reminder from showing
            activityCounterInMillis = FUNDING_REMINDER_MIN_ACTIVITY_MILLIS,
        )
        val currentTime = REMINDER_REFERENCE_TIMESTAMP + FUNDING_REMINDER_DELAY_MILLIS
        val fragmentObserver = FakeFragmentLifecycleObserver()
        val activityObserver = FakeActivityLifecycleObserver()
        var dialogShown = false
        val testSubject = createTestSubject(
            activityProvider = activityProvider,
            settings = settings,
            fragmentObserver = fragmentObserver,
            activityCounterObserver = activityObserver,
            dialog = { dialogShown = true },
            clock = TestClock(Instant.fromEpochMilliseconds(currentTime)),
            scope = CoroutineScope(mainDispatcher.testDispatcher),
        )

        // Test the reminder functionality
        testSubject.registerReminder { }

        assertThat(dialogShown).isEqualTo(false)
        assertThat(fragmentObserver.isRegistered).isFalse()
        assertThat(settings.getLastReminderShownActivityAmount()).isEqualTo(REMINDER_SHOWN_TIMESTAMP_UNSET)
        assertThat(settings.getReminderShownCount()).isEqualTo(REMINDER_COUNTER_FIRST_SHOWN)
        assertThat(settings.getReminderShownTimestamp()).isEqualTo(REMINDER_SHOWN_TIMESTAMP_UNSET)
    }

    @Test
    fun `second reminder does not show if no reminders have been shown yet`() {
        val activity = createTestActivity()
        val activityProvider = FakeActivityProvider(activity)
        // Only the first reminder could show with these settings
        val settings = FakeFundingSettings(
            reminderReferenceTimestamp = REMINDER_REFERENCE_TIMESTAMP,
            reminderShownTimestamp = REMINDER_SHOWN_TIMESTAMP_UNSET,
            fundingReminderCount = REMINDER_COUNTER_UNSET,
            activityCounterInMillis = FUNDING_REMINDER_MIN_ACTIVITY_MILLIS,
        )
        val currentTime = REMINDER_REFERENCE_TIMESTAMP + FUNDING_REMINDER_DELAY_MILLIS
        val fragmentObserver = FakeFragmentLifecycleObserver()
        val activityObserver = FakeActivityLifecycleObserver()
        var dialogShown = false
        val testSubject = createTestSubject(
            activityProvider = activityProvider,
            settings = settings,
            fragmentObserver = fragmentObserver,
            activityCounterObserver = activityObserver,
            dialog = { dialogShown = true },
            clock = TestClock(Instant.fromEpochMilliseconds(currentTime)),
            scope = CoroutineScope(mainDispatcher.testDispatcher),
        )

        // Test the reminder functionality
        testSubject.registerReminder { }

        // Assert that only one donation reminder has displayed, the first one
        assertThat(dialogShown).isEqualTo(true)
        assertThat(fragmentObserver.isRegistered).isTrue()
        assertThat(activityObserver.isRegistered).isTrue()
        assertThat(settings.getReminderShownCount()).isEqualTo(REMINDER_COUNTER_FIRST_SHOWN)
        assertThat(settings.getLastReminderShownActivityAmount()).isEqualTo(FUNDING_REMINDER_MIN_ACTIVITY_MILLIS)
        assertThat(settings.getReminderShownTimestamp()).isEqualTo(currentTime)
    }

    @Test
    fun `second reminder shows and increments timestamp and counter after first and after set time`() {
        val activity = createTestActivity()
        val activityProvider = FakeActivityProvider(activity)
        val settings = FakeFundingSettings(
            reminderReferenceTimestamp = REMINDER_REFERENCE_TIMESTAMP,
            reminderShownTimestamp = REMINDER_SHOWN_TIMESTAMP,
            lastReminderShownActivityAmount = REMINDER_SHOWN_TIMESTAMP,
            fundingReminderCount = REMINDER_COUNTER_FIRST_SHOWN,
            activityCounterInMillis = SECOND_FUNDING_REMINDER_MIN_ACTIVITY_MILLIS,
        )
        val currentTime = REMINDER_REFERENCE_TIMESTAMP + SECOND_FUNDING_REMINDER_MIN_ACTIVITY_MILLIS
        val fragmentObserver = FakeFragmentLifecycleObserver()
        val activityObserver = FakeActivityLifecycleObserver()
        var dialogShown = false
        val testSubject = createTestSubject(
            activityProvider = activityProvider,
            settings = settings,
            fragmentObserver = fragmentObserver,
            activityCounterObserver = activityObserver,
            dialog = { dialogShown = true },
            clock = TestClock(Instant.fromEpochMilliseconds(currentTime)),
            scope = CoroutineScope(mainDispatcher.testDispatcher),
        )

        // Test the reminder functionality
        testSubject.registerReminder { }

        assertThat(dialogShown).isEqualTo(true)
        assertThat(fragmentObserver.isRegistered).isTrue()
        assertThat(activityObserver.isRegistered).isTrue()
        assertThat(settings.getReminderShownCount()).isEqualTo(REMINDER_COUNTER_SECOND_SHOWN)
        assertThat(settings.getReminderShownTimestamp()).isEqualTo(currentTime)
        assertThat(settings.getLastReminderShownActivityAmount()).isEqualTo(SECOND_FUNDING_REMINDER_MIN_ACTIVITY_MILLIS)
    }

    private fun createTestSubject(
        activityProvider: ActivityProvider,
        settings: FundingSettings,
        fragmentObserver: FundingReminderContract.FragmentLifecycleObserver,
        activityCounterObserver: FundingReminderContract.ActivityLifecycleObserver,
        dialog: Dialog = Dialog { },
        clock: TestClock = TestClock(Instant.fromEpochMilliseconds(0)),
        scope: CoroutineScope,
    ): FundingReminder {
        return FundingReminder(
            activityProvider = activityProvider,
            settings = settings,
            fragmentObserver = fragmentObserver,
            activityCounterObserver = activityCounterObserver,
            dialog = dialog,
            clock = clock,
            scope = scope,
        )
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

    private companion object {
        const val PACKAGE_NAME = "test.package.name"

        const val INSTALL_TIME = 1000L
        const val REMINDER_REFERENCE_TIMESTAMP_UNSET = 0L
        const val REMINDER_REFERENCE_TIMESTAMP = 1000L
        const val REMINDER_SHOWN_TIMESTAMP_UNSET = 0L
        const val REMINDER_SHOWN_TIMESTAMP = 1111L

        const val LAST_REMINDER_SHOWN_UNSET = 0L

        const val REMINDER_COUNTER_UNSET = 0
        const val REMINDER_COUNTER_FIRST_SHOWN = 1
        const val REMINDER_COUNTER_SECOND_SHOWN = 2

        const val FUNDING_REMINDER_DELAY_MILLIS = 7 * 24 * 60 * 60 * 1000L
        const val FUNDING_REMINDER_MIN_ACTIVITY_MILLIS = 30 * 60 * 1000L
        const val SECOND_FUNDING_REMINDER_MIN_ACTIVITY_MILLIS = FUNDING_REMINDER_MIN_ACTIVITY_MILLIS * 2
    }
}
