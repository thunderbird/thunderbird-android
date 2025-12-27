package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import java.util.Calendar
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.display.DisplaySettings
import net.thunderbird.core.preference.network.NetworkSettings
import net.thunderbird.core.preference.notification.NotificationPreference
import net.thunderbird.core.preference.privacy.PrivacySettings
import net.thunderbird.core.testing.TestClock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

private val TIMESTAMP = 0L

@OptIn(ExperimentalTime::class)
class SummaryNotificationDataCreatorTest {
    private val account = createAccount()
    private val testClock = TestClock()
    private var generalSettings = GeneralSettings(
        display = DisplaySettings(),
        network = NetworkSettings(),
        notification = NotificationPreference(
            quietTimeStarts = "23:00",
            quietTimeEnds = "00:00",
        ),
        privacy = PrivacySettings(),
        platformConfigProvider = FakePlatformConfigProvider(),
    )
    private val notificationDataCreator = SummaryNotificationDataCreator(
        singleMessageNotificationDataCreator = SingleMessageNotificationDataCreator(
            interactionPreferences = mock {
                on { getConfig() } doAnswer { generalSettings.interaction }
            },
            generalSettingsManager = mock {
                on { getConfig() } doAnswer { generalSettings }
            },
        ),
        generalSettingsManager = mock {
            on { getConfig() } doAnswer { generalSettings }
        },
    )

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<Clock> { testClock }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        setQuietTime(false)
    }

    @Test
    fun `single new message`() {
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = false,
        )

        assertThat(result).isInstanceOf<SummarySingleNotificationData>()
    }

    @Test
    fun `single notification during quiet time`() {
        setClockTo("23:01")
        setQuietTime(true)
        val notificationData = createNotificationData()

        val result = SummaryNotificationDataCreator(
            singleMessageNotificationDataCreator = SingleMessageNotificationDataCreator(
                interactionPreferences = mock {
                    on { getConfig() } doReturn generalSettings.interaction
                },
                generalSettingsManager = mock {
                    on { getConfig() } doReturn generalSettings
                },
            ),
            generalSettingsManager = mock {
                on { getConfig() } doReturn generalSettings.copy(
                    notification = generalSettings.notification.copy(isQuietTimeEnabled = true),
                )
            },
        ).createSummaryNotificationData(
            notificationData,
            silent = false,
        )

        val summaryNotificationData = result as SummarySingleNotificationData
        assertThat(summaryNotificationData.singleNotificationData.isSilent).isTrue()
    }

    @Test
    fun `single notification with quiet time disabled`() {
        setClockTo("23:01")
        setQuietTime(false)
        val notificationData = createNotificationData()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = false,
        )

        val summaryNotificationData = result as SummarySingleNotificationData
        assertThat(summaryNotificationData.singleNotificationData.isSilent).isFalse()
    }

    @Test
    fun `inbox-style notification during quiet time`() {
        setClockTo("23:01")
        setQuietTime(true)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = SummaryNotificationDataCreator(
            singleMessageNotificationDataCreator = SingleMessageNotificationDataCreator(
                interactionPreferences = mock {
                    on { getConfig() } doReturn generalSettings.interaction
                },
                generalSettingsManager = mock {
                    on { getConfig() } doReturn generalSettings
                },
            ),
            generalSettingsManager = mock {
                on { getConfig() } doReturn generalSettings.copy(
                    notification = generalSettings.notification.copy(isQuietTimeEnabled = true),
                )
            },
        ).createSummaryNotificationData(
            notificationData,
            silent = false,
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.isSilent).isTrue()
    }

    @Test
    fun `inbox-style notification with quiet time disabled`() {
        setQuietTime(false)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = false,
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.isSilent).isFalse()
    }

    @Test
    fun `inbox-style base properties`() {
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true,
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.notificationId).isEqualTo(
            NotificationIds.getNewMailSummaryNotificationId(account),
        )
        assertThat(summaryNotificationData.isSilent).isTrue()
        assertThat(summaryNotificationData.timestamp).isEqualTo(TIMESTAMP)
    }

    @Test
    fun `default actions`() {
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true,
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.actions).contains(SummaryNotificationAction.MarkAsRead)
        assertThat(summaryNotificationData.wearActions).contains(SummaryWearNotificationAction.MarkAsRead)
    }

    @Test
    fun `always show delete action without confirmation`() {
        setSummaryDeleteActionEnabled(true)
        setConfirmDeleteFromNotification(false)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true,
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.actions).contains(SummaryNotificationAction.Delete)
        assertThat(summaryNotificationData.wearActions).contains(SummaryWearNotificationAction.Delete)
    }

    @Test
    fun `always show delete action with confirmation`() {
        setSummaryDeleteActionEnabled(true)
        setConfirmDeleteFromNotification(true)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true,
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.actions).contains(SummaryNotificationAction.Delete)
        assertThat(summaryNotificationData.wearActions).doesNotContain(SummaryWearNotificationAction.Delete)
    }

    @Test
    fun `hide delete action when disabled`() {
        setSummaryDeleteActionEnabled(false)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true,
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.actions).doesNotContain(SummaryNotificationAction.Delete)
        assertThat(summaryNotificationData.wearActions).doesNotContain(SummaryWearNotificationAction.Delete)
    }

    @Test
    fun `archive action with archive folder`() {
        account.archiveFolderId = 1
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true,
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.wearActions).contains(SummaryWearNotificationAction.Archive)
    }

    @Test
    fun `archive action without archive folder`() {
        account.archiveFolderId = null
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = notificationDataCreator.createSummaryNotificationData(
            notificationData,
            silent = true,
        )

        val summaryNotificationData = result as SummaryInboxNotificationData
        assertThat(summaryNotificationData.wearActions).doesNotContain(SummaryWearNotificationAction.Archive)
    }

    private fun setQuietTime(quietTime: Boolean) {
        generalSettings = generalSettings.copy(
            notification = generalSettings.notification.copy(isQuietTimeEnabled = quietTime),
        )
    }

    private fun setSummaryDeleteActionEnabled(enabled: Boolean) {
        generalSettings = generalSettings.copy(
            notification = generalSettings.notification.copy(
                isSummaryDeleteActionEnabled = enabled,
            ),
        )
    }

    private fun setConfirmDeleteFromNotification(confirm: Boolean) {
        generalSettings = generalSettings.copy(
            interaction = generalSettings.interaction.copy(
                isConfirmDeleteFromNotification = confirm,
            ),
        )
    }

    private fun createAccount(): LegacyAccountDto {
        return LegacyAccountDto("00000000-0000-0000-0000-000000000000").apply {
            accountNumber = 42
        }
    }

    private fun createNotificationContent() = NotificationContent(
        messageReference = MessageReference("irrelevant", 1, "irrelevant"),
        sender = "irrelevant",
        subject = "irrelevant",
        preview = "irrelevant",
        summary = "irrelevant",
    )

    private fun createNotificationData(
        contentList: List<NotificationContent> = listOf(createNotificationContent()),
    ): NotificationData {
        val activeNotifications = contentList.mapIndexed { index, content ->
            NotificationHolder(notificationId = index, TIMESTAMP, content)
        }

        return NotificationData(account, activeNotifications, inactiveNotifications = emptyList())
    }

    private fun createNotificationDataWithMultipleMessages(times: Int = 2): NotificationData {
        val contentList = buildList {
            repeat(times) {
                add(createNotificationContent())
            }
        }
        return createNotificationData(contentList)
    }

    private fun setClockTo(time: String) {
        val (hourOfDay, minute) = time.split(':').map { it.toInt() }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        calendar.set(Calendar.MINUTE, minute)

        testClock.changeTimeTo(Instant.fromEpochMilliseconds(calendar.timeInMillis))
    }
}
