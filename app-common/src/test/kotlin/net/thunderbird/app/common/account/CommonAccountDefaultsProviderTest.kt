package net.thunderbird.app.common.account

import app.k9mail.core.featureflag.FeatureFlagResult
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_MAXIMUM_AUTO_DOWNLOAD_MESSAGE_SIZE
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_MESSAGE_FORMAT
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_MESSAGE_FORMAT_AUTO
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_MESSAGE_READ_RECEIPT
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_QUOTED_TEXT_SHOWN
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_QUOTE_PREFIX
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_QUOTE_STYLE
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_REMOTE_SEARCH_NUM_RESULTS
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_REPLY_AFTER_QUOTE
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_RINGTONE_URI
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_SORT_ASCENDING
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_SORT_TYPE
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_STRIP_SIGNATURE
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.DEFAULT_SYNC_INTERVAL
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.NO_OPENPGP_KEY
import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.UNASSIGNED_ACCOUNT_NUMBER
import app.k9mail.legacy.account.Expunge
import app.k9mail.legacy.account.FolderMode
import app.k9mail.legacy.account.Identity
import app.k9mail.legacy.account.LegacyAccount
import app.k9mail.legacy.account.ShowPictures
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.K9
import net.thunderbird.core.mail.folder.api.SpecialFolderSelection
import net.thunderbird.core.preferences.Storage
import net.thunderbird.feature.notification.NotificationLight
import net.thunderbird.feature.notification.NotificationSettings
import net.thunderbird.feature.notification.NotificationVibration
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class CommonAccountDefaultsProviderTest {

    @Suppress("LongMethod")
    @Test
    fun `applyDefaults should return default values`() {
        // arrange
        val resourceProvider = mock<CoreResourceProvider> {
            on { defaultIdentityDescription() } doReturn "Default Identity"
        }
        val account = LegacyAccount(
            uuid = "test-uuid",
            isSensitiveDebugLoggingEnabled = { false },
        )
        val identities = listOf(
            Identity(
                signatureUse = false,
                signature = null,
                description = resourceProvider.defaultIdentityDescription(),
            ),
        )
        val notificationSettings = NotificationSettings(
            isRingEnabled = true,
            ringtone = DEFAULT_RINGTONE_URI,
            light = NotificationLight.Disabled,
            vibration = NotificationVibration.DEFAULT,
        )
        val testSubject = CommonAccountDefaultsProvider(
            resourceProvider = resourceProvider,
            featureFlagProvider = {
                FeatureFlagResult.Disabled
            },
        )

        // act
        testSubject.applyDefaults(account)

        // assert
        assertThat(account.automaticCheckIntervalMinutes).isEqualTo(DEFAULT_SYNC_INTERVAL)
        assertThat(account.idleRefreshMinutes).isEqualTo(24)
        assertThat(account.displayCount).isEqualTo(K9.DEFAULT_VISIBLE_LIMIT)
        assertThat(account.accountNumber).isEqualTo(UNASSIGNED_ACCOUNT_NUMBER)
        assertThat(account.isNotifyNewMail).isTrue()
        assertThat(account.folderNotifyNewMailMode).isEqualTo(FolderMode.ALL)
        assertThat(account.isNotifySync).isFalse()
        assertThat(account.isNotifySelfNewMail).isTrue()
        assertThat(account.isNotifyContactsMailOnly).isFalse()
        assertThat(account.isIgnoreChatMessages).isFalse()
        assertThat(account.messagesNotificationChannelVersion).isEqualTo(0)
        assertThat(account.folderDisplayMode).isEqualTo(FolderMode.NOT_SECOND_CLASS)
        assertThat(account.folderSyncMode).isEqualTo(FolderMode.FIRST_CLASS)
        assertThat(account.folderPushMode).isEqualTo(FolderMode.NONE)
        assertThat(account.sortType).isEqualTo(DEFAULT_SORT_TYPE)
        assertThat(account.isSortAscending(DEFAULT_SORT_TYPE)).isEqualTo(DEFAULT_SORT_ASCENDING)
        assertThat(account.showPictures).isEqualTo(ShowPictures.NEVER)
        assertThat(account.isSignatureBeforeQuotedText).isFalse()
        assertThat(account.expungePolicy).isEqualTo(Expunge.EXPUNGE_IMMEDIATELY)
        assertThat(account.importedAutoExpandFolder).isNull()
        assertThat(account.legacyInboxFolder).isNull()
        assertThat(account.maxPushFolders).isEqualTo(10)
        assertThat(account.isSubscribedFoldersOnly).isFalse()
        assertThat(account.maximumPolledMessageAge).isEqualTo(-1)
        assertThat(account.maximumAutoDownloadMessageSize).isEqualTo(DEFAULT_MAXIMUM_AUTO_DOWNLOAD_MESSAGE_SIZE)
        assertThat(account.messageFormat).isEqualTo(DEFAULT_MESSAGE_FORMAT)
        assertThat(account.isMessageFormatAuto).isEqualTo(DEFAULT_MESSAGE_FORMAT_AUTO)
        assertThat(account.isMessageReadReceipt).isEqualTo(DEFAULT_MESSAGE_READ_RECEIPT)
        assertThat(account.quoteStyle).isEqualTo(DEFAULT_QUOTE_STYLE)
        assertThat(account.quotePrefix).isEqualTo(DEFAULT_QUOTE_PREFIX)
        assertThat(account.isDefaultQuotedTextShown).isEqualTo(DEFAULT_QUOTED_TEXT_SHOWN)
        assertThat(account.isReplyAfterQuote).isEqualTo(DEFAULT_REPLY_AFTER_QUOTE)
        assertThat(account.isStripSignature).isEqualTo(DEFAULT_STRIP_SIGNATURE)
        assertThat(account.isSyncRemoteDeletions).isTrue()
        assertThat(account.openPgpKey).isEqualTo(NO_OPENPGP_KEY)
        assertThat(account.isRemoteSearchFullText).isFalse()
        assertThat(account.remoteSearchNumResults).isEqualTo(DEFAULT_REMOTE_SEARCH_NUM_RESULTS)
        assertThat(account.isUploadSentMessages).isTrue()
        assertThat(account.isMarkMessageAsReadOnView).isTrue()
        assertThat(account.isMarkMessageAsReadOnDelete).isTrue()
        assertThat(account.isAlwaysShowCcBcc).isFalse()
        assertThat(account.lastSyncTime).isEqualTo(0L)
        assertThat(account.lastFolderListRefreshTime).isEqualTo(0L)

        assertThat(account.archiveFolderId).isNull()
        assertThat(account.archiveFolderSelection).isEqualTo(SpecialFolderSelection.AUTOMATIC)
        assertThat(account.draftsFolderId).isNull()
        assertThat(account.draftsFolderSelection).isEqualTo(SpecialFolderSelection.AUTOMATIC)
        assertThat(account.sentFolderId).isNull()
        assertThat(account.sentFolderSelection).isEqualTo(SpecialFolderSelection.AUTOMATIC)
        assertThat(account.spamFolderId).isNull()
        assertThat(account.spamFolderSelection).isEqualTo(SpecialFolderSelection.AUTOMATIC)
        assertThat(account.trashFolderId).isNull()
        assertThat(account.trashFolderSelection).isEqualTo(SpecialFolderSelection.AUTOMATIC)
        assertThat(account.archiveFolderId).isNull()
        assertThat(account.archiveFolderSelection).isEqualTo(SpecialFolderSelection.AUTOMATIC)

        assertThat(account.identities).isEqualTo(identities)
        assertThat(account.notificationSettings).isEqualTo(notificationSettings)

        assertThat(account.isChangedVisibleLimits).isFalse()
    }

    @Test
    fun `applyOverwrites should return patched account when disabled`() {
        // arrange
        val resourceProvider = mock<CoreResourceProvider> {
            on { defaultIdentityDescription() } doReturn "Default Identity"
        }
        val account = LegacyAccount(
            uuid = "test-uuid",
            isSensitiveDebugLoggingEnabled = { false },
        )
        val storage = mock<Storage> {
            on { contains("${account.uuid}.notifyNewMail") } doReturn false
            on { getBoolean("${account.uuid}.notifyNewMail", false) } doReturn false
            on { getBoolean("${account.uuid}.notifySelfNewMail", false) } doReturn false
        }
        val testSubject = CommonAccountDefaultsProvider(
            resourceProvider = resourceProvider,
            featureFlagProvider = {
                FeatureFlagResult.Disabled
            },
        )

        // act
        testSubject.applyOverwrites(account, storage)

        // assert
        assertThat(account.isNotifyNewMail).isFalse()
        assertThat(account.isNotifySelfNewMail).isFalse()
    }

    @Test
    fun `applyOverwrites should return patched account when enabled`() {
        // arrange
        val resourceProvider = mock<CoreResourceProvider> {
            on { defaultIdentityDescription() } doReturn "Default Identity"
        }
        val account = LegacyAccount(
            uuid = "test-uuid",
            isSensitiveDebugLoggingEnabled = { false },
        )
        val storage = mock<Storage> {
            on { contains("${account.uuid}.notifyNewMail") } doReturn false
            on { getBoolean("${account.uuid}.notifyNewMail", false) } doReturn false
            on { getBoolean("${account.uuid}.notifySelfNewMail", false) } doReturn false
        }
        val testSubject = CommonAccountDefaultsProvider(
            resourceProvider = resourceProvider,
            featureFlagProvider = {
                FeatureFlagResult.Enabled
            },
        )

        // act
        testSubject.applyOverwrites(account, storage)

        // assert
        assertThat(account.isNotifyNewMail).isTrue()
        assertThat(account.isNotifySelfNewMail).isTrue()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `applyOverwrites updates account notification values from storage when storage contains isNotifyNewMail value`() {
        // arrange
        val resourceProvider = mock<CoreResourceProvider> {
            on { defaultIdentityDescription() } doReturn "Default Identity"
        }
        val account = LegacyAccount(
            uuid = "test-uuid",
            isSensitiveDebugLoggingEnabled = { false },
        )
        val storage = mock<Storage> {
            on { contains("${account.uuid}.notifyNewMail") } doReturn true
            on { getBoolean("${account.uuid}.notifyNewMail", false) } doReturn false
            on { getBoolean("${account.uuid}.notifySelfNewMail", false) } doReturn false
        }
        val testSubject = CommonAccountDefaultsProvider(
            resourceProvider = resourceProvider,
            featureFlagProvider = {
                FeatureFlagResult.Enabled
            },
        )

        // act
        testSubject.applyOverwrites(account, storage)

        // assert
        assertThat(account.isNotifyNewMail).isFalse()
        assertThat(account.isNotifySelfNewMail).isFalse()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `applyOverwrites updates account notification values from featureFlag values when storage does not contain isNotifyNewMail value`() {
        // arrange
        val resourceProvider = mock<CoreResourceProvider> {
            on { defaultIdentityDescription() } doReturn "Default Identity"
        }
        val account = LegacyAccount(
            uuid = "test-uuid",
            isSensitiveDebugLoggingEnabled = { false },
        )
        val storage = mock<Storage> {
            on { contains("${account.uuid}.notifyNewMail") } doReturn false
            on { getBoolean("${account.uuid}.notifyNewMail", false) } doReturn false
            on { getBoolean("${account.uuid}.notifySelfNewMail", false) } doReturn false
        }
        val testSubject = CommonAccountDefaultsProvider(
            resourceProvider = resourceProvider,
            featureFlagProvider = {
                FeatureFlagResult.Enabled
            },
        )

        // act
        testSubject.applyOverwrites(account, storage)

        // assert
        assertThat(account.isNotifyNewMail).isTrue()
        assertThat(account.isNotifySelfNewMail).isTrue()
    }
}
