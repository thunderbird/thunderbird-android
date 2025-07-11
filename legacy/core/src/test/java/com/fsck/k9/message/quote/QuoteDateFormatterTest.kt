package com.fsck.k9.message.quote

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.BackgroundSync
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.SubTheme
import net.thunderbird.core.preference.notification.NotificationPreference
import net.thunderbird.core.preference.privacy.PrivacySettings
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class QuoteDateFormatterTest {
    private lateinit var originalLocale: Locale
    private var originalTimeZone: TimeZone? = null
    private val generalSettingsManager: GeneralSettingsManager = mock()
    private val fakeGeneralSettings = GeneralSettings(
        backgroundSync = BackgroundSync.NEVER,
        showRecentChanges = false,
        appTheme = AppTheme.FOLLOW_SYSTEM,
        messageViewTheme = SubTheme.USE_GLOBAL,
        messageComposeTheme = SubTheme.USE_GLOBAL,
        fixedMessageViewTheme = false,
        isAutoFitWidth = false,
        isThreadedViewEnabled = false,
        isUseMessageViewFixedWidthFont = false,
        isShowContactPicture = false,
        isMessageListSenderAboveSubject = false,
        isChangeContactNameColor = false,
        isColorizeMissingContactPictures = false,
        shouldShowSetupArchiveFolderDialog = false,
        isShowContactName = false,
        isShowUnifiedInbox = false,
        isShowStarredCount = false,
        isShowComposeButtonOnMessageList = false,
        isUseBackgroundAsUnreadIndicator = false,
        isShowCorrespondentNames = false,
        isShowAnimations = false,
        isShowMessageListStars = false,
        notification = NotificationPreference(),
        privacy = PrivacySettings(),

    )
    private val quoteDateFormatter = QuoteDateFormatter(
        generalSettingsManager = generalSettingsManager,
    )

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+02:00"))
        whenever(generalSettingsManager.getSettings()) doReturn fakeGeneralSettings
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun hideTimeZoneEnabled_UsLocale() {
        whenever(generalSettingsManager.getSettings()) doReturn
            fakeGeneralSettings.copy(privacy = fakeGeneralSettings.privacy.copy(isHideTimeZone = true))
        Locale.setDefault(Locale.US)

        val formattedDate = quoteDateFormatter.format("2020-09-19T20:00:00+00:00".toDate())

        assertThat(formattedDate.normalizeDate()).isEqualTo("September 19, 2020, 8:00:00 PM UTC")
    }

    @Test
    fun hideTimeZoneEnabled_GermanyLocale() {
        whenever(generalSettingsManager.getSettings()) doReturn
            fakeGeneralSettings.copy(privacy = fakeGeneralSettings.privacy.copy(isHideTimeZone = true))
        Locale.setDefault(Locale.GERMANY)

        val formattedDate = quoteDateFormatter.format("2020-09-19T20:00:00+00:00".toDate())

        assertThat(formattedDate.normalizeDate()).isEqualTo("19. September 2020, 20:00:00 UTC")
    }

    @Test
    fun hideTimeZoneDisabled_UsLocale() {
        Locale.setDefault(Locale.US)

        val formattedDate = quoteDateFormatter.format("2020-09-19T20:00:00+00:00".toDate())

        assertThat(formattedDate.normalizeDate()).isEqualTo("September 19, 2020, 10:00:00 PM GMT+02:00")
    }

    @Test
    fun hideTimeZoneDisabled_GermanyLocale() {
        Locale.setDefault(Locale.GERMANY)

        val formattedDate = quoteDateFormatter.format("2020-09-19T20:00:00+00:00".toDate())

        assertThat(formattedDate.normalizeDate()).isEqualTo("19. September 2020, 22:00:00 GMT+02:00")
    }

    private fun String.toDate() = Date(ZonedDateTime.parse(this).toEpochSecond() * 1000L)

    // QuoteDateFormatter uses java.text.DateFormat internally. Depending on the JDK/JRE version the output is
    // different. We normalize the output here so the tests don't depend on a specific JDK version.
    private fun String.normalizeDate(): String {
        return this
            .replace(" at", ",")
            .replace(" um", ",")
            .replace("\u202F", " ")
    }
}
