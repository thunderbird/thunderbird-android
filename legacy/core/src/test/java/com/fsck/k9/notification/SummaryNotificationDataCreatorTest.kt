package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.fsck.k9.K9
import kotlinx.datetime.Clock
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.BackgroundSync
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.SubTheme
import net.thunderbird.core.preference.privacy.PrivacySettings
import net.thunderbird.core.testing.TestClock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

private val TIMESTAMP = 0L

class SummaryNotificationDataCreatorTest {
    private val account = createAccount()
    private var generalSettings = GeneralSettings(
        backgroundSync = BackgroundSync.ALWAYS,
        showRecentChanges = true,
        appTheme = AppTheme.DARK,
        messageComposeTheme = SubTheme.DARK,
        isShowCorrespondentNames = true,
        fixedMessageViewTheme = true,
        messageViewTheme = SubTheme.DARK,
        isShowStarredCount = false,
        isShowUnifiedInbox = false,
        isShowMessageListStars = false,
        isShowAnimations = false,
        shouldShowSetupArchiveFolderDialog = false,
        isMessageListSenderAboveSubject = false,
        isShowContactName = false,
        isShowContactPicture = false,
        isChangeContactNameColor = false,
        isColorizeMissingContactPictures = false,
        isUseBackgroundAsUnreadIndicator = false,
        isShowComposeButtonOnMessageList = false,
        isThreadedViewEnabled = false,
        isUseMessageViewFixedWidthFont = false,
        isAutoFitWidth = false,
        isQuietTime = false,
        quietTimeStarts = "0:00",
        quietTimeEnds = "23:59",
        isQuietTimeEnabled = false,
        privacy = PrivacySettings(isHideTimeZone = false),
    )
    private val notificationDataCreator = SummaryNotificationDataCreator(
        singleMessageNotificationDataCreator = SingleMessageNotificationDataCreator(),
        generalSettingsManager = mock {
            on { getSettings() } doReturn generalSettings
        },
    )

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<Clock> { TestClock() }
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
        setQuietTime(true)
        val notificationData = createNotificationData()

        val result = SummaryNotificationDataCreator(
            singleMessageNotificationDataCreator = SingleMessageNotificationDataCreator(),
            generalSettingsManager = mock {
                on { getSettings() } doReturn generalSettings.copy(isQuietTime = true, isQuietTimeEnabled = true)
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
        setQuietTime(true)
        val notificationData = createNotificationDataWithMultipleMessages()

        val result = SummaryNotificationDataCreator(
            singleMessageNotificationDataCreator = SingleMessageNotificationDataCreator(),
            generalSettingsManager = mock {
                on { getSettings() } doReturn generalSettings.copy(isQuietTime = true, isQuietTimeEnabled = true)
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
        setDeleteAction(K9.NotificationQuickDelete.ALWAYS)
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
        setDeleteAction(K9.NotificationQuickDelete.ALWAYS)
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
    fun `show delete action for single notification without confirmation`() {
        setDeleteAction(K9.NotificationQuickDelete.FOR_SINGLE_MSG)
        setConfirmDeleteFromNotification(false)
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
    fun `never show delete action`() {
        setDeleteAction(K9.NotificationQuickDelete.NEVER)
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
        generalSettings = generalSettings.copy(isQuietTimeEnabled = quietTime)
    }

    private fun setDeleteAction(mode: K9.NotificationQuickDelete) {
        K9.notificationQuickDeleteBehaviour = mode
    }

    private fun setConfirmDeleteFromNotification(confirm: Boolean) {
        K9.isConfirmDeleteFromNotification = confirm
    }

    private fun createAccount(): LegacyAccount {
        return LegacyAccount("00000000-0000-0000-0000-000000000000").apply {
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
}
