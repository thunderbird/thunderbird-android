package net.thunderbird.app.common.account

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_MAXIMUM_AUTO_DOWNLOAD_MESSAGE_SIZE
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_MESSAGE_FORMAT
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_MESSAGE_FORMAT_AUTO
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_MESSAGE_READ_RECEIPT
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_QUOTED_TEXT_SHOWN
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_QUOTE_PREFIX
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_QUOTE_STYLE
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_REMOTE_SEARCH_NUM_RESULTS
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_REPLY_AFTER_QUOTE
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_RINGTONE_URI
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_SORT_ASCENDING
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_SORT_TYPE
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_STRIP_SIGNATURE
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_SYNC_INTERVAL
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_VISIBLE_LIMIT
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.NO_OPENPGP_KEY
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.UNASSIGNED_ACCOUNT_NUMBER
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.FolderMode
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.ShowPictures
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.notification.NotificationLight
import net.thunderbird.feature.notification.NotificationSettings
import net.thunderbird.feature.notification.NotificationVibration
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class DefaultAccountDefaultsProviderTest {

    @Suppress("LongMethod")
    @Test
    fun `applyDefaults should return default values`() {
        // arrange
        val resourceProvider = mock<CoreResourceProvider> {
            on { defaultIdentityDescription() } doReturn "Default Identity"
        }
        val account = createFakeAccount()
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
        val testSubject = DefaultAccountDefaultsProvider(
            resourceProvider = resourceProvider,
            featureFlagProvider = {
                FeatureFlagResult.Disabled
            },
        )

        // act
        val defaultedAccount = testSubject.applyDefaults(account)

        // assert
        assertThat(defaultedAccount.automaticCheckIntervalMinutes).isEqualTo(DEFAULT_SYNC_INTERVAL)
        assertThat(defaultedAccount.idleRefreshMinutes).isEqualTo(24)
        assertThat(defaultedAccount.displayCount).isEqualTo(DEFAULT_VISIBLE_LIMIT)
        assertThat(defaultedAccount.accountNumber).isEqualTo(UNASSIGNED_ACCOUNT_NUMBER)
        assertThat(defaultedAccount.isNotifyNewMail).isTrue()
        assertThat(defaultedAccount.folderNotifyNewMailMode).isEqualTo(FolderMode.ALL)
        assertThat(defaultedAccount.isNotifySync).isFalse()
        assertThat(defaultedAccount.isNotifySelfNewMail).isTrue()
        assertThat(defaultedAccount.isNotifyContactsMailOnly).isFalse()
        assertThat(defaultedAccount.isIgnoreChatMessages).isFalse()
        assertThat(defaultedAccount.messagesNotificationChannelVersion).isEqualTo(0)
        assertThat(defaultedAccount.folderDisplayMode).isEqualTo(FolderMode.NOT_SECOND_CLASS)
        assertThat(defaultedAccount.folderSyncMode).isEqualTo(FolderMode.FIRST_CLASS)
        assertThat(defaultedAccount.folderPushMode).isEqualTo(FolderMode.NONE)
        assertThat(defaultedAccount.sortType).isEqualTo(DEFAULT_SORT_TYPE)
        assertThat(defaultedAccount.sortAscending[DEFAULT_SORT_TYPE]).isEqualTo(DEFAULT_SORT_ASCENDING)
        assertThat(defaultedAccount.showPictures).isEqualTo(ShowPictures.NEVER)
        assertThat(defaultedAccount.isSignatureBeforeQuotedText).isFalse()
        assertThat(defaultedAccount.expungePolicy).isEqualTo(Expunge.EXPUNGE_IMMEDIATELY)
        assertThat(defaultedAccount.importedAutoExpandFolder).isNull()
        assertThat(defaultedAccount.legacyInboxFolder).isNull()
        assertThat(defaultedAccount.maxPushFolders).isEqualTo(10)
        assertThat(defaultedAccount.isSubscribedFoldersOnly).isFalse()
        assertThat(defaultedAccount.maximumPolledMessageAge).isEqualTo(-1)
        assertThat(defaultedAccount.maximumAutoDownloadMessageSize)
            .isEqualTo(DEFAULT_MAXIMUM_AUTO_DOWNLOAD_MESSAGE_SIZE)
        assertThat(defaultedAccount.messageFormat).isEqualTo(DEFAULT_MESSAGE_FORMAT)
        assertThat(defaultedAccount.isMessageFormatAuto).isEqualTo(DEFAULT_MESSAGE_FORMAT_AUTO)
        assertThat(defaultedAccount.isMessageReadReceipt).isEqualTo(DEFAULT_MESSAGE_READ_RECEIPT)
        assertThat(defaultedAccount.quoteStyle).isEqualTo(DEFAULT_QUOTE_STYLE)
        assertThat(defaultedAccount.quotePrefix).isEqualTo(DEFAULT_QUOTE_PREFIX)
        assertThat(defaultedAccount.isDefaultQuotedTextShown).isEqualTo(DEFAULT_QUOTED_TEXT_SHOWN)
        assertThat(defaultedAccount.isReplyAfterQuote).isEqualTo(DEFAULT_REPLY_AFTER_QUOTE)
        assertThat(defaultedAccount.isStripSignature).isEqualTo(DEFAULT_STRIP_SIGNATURE)
        assertThat(defaultedAccount.isSyncRemoteDeletions).isTrue()
        assertThat(defaultedAccount.openPgpKey).isEqualTo(NO_OPENPGP_KEY)
        assertThat(defaultedAccount.isRemoteSearchFullText).isFalse()
        assertThat(defaultedAccount.remoteSearchNumResults).isEqualTo(DEFAULT_REMOTE_SEARCH_NUM_RESULTS)
        assertThat(defaultedAccount.isUploadSentMessages).isTrue()
        assertThat(defaultedAccount.isMarkMessageAsReadOnView).isTrue()
        assertThat(defaultedAccount.isMarkMessageAsReadOnDelete).isTrue()
        assertThat(defaultedAccount.isAlwaysShowCcBcc).isFalse()
        assertThat(defaultedAccount.lastSyncTime).isEqualTo(0L)
        assertThat(defaultedAccount.lastFolderListRefreshTime).isEqualTo(0L)

        assertThat(defaultedAccount.archiveFolderId).isNull()
        assertThat(defaultedAccount.archiveFolderSelection).isEqualTo(SpecialFolderSelection.AUTOMATIC)
        assertThat(defaultedAccount.draftsFolderId).isNull()
        assertThat(defaultedAccount.draftsFolderSelection).isEqualTo(SpecialFolderSelection.AUTOMATIC)
        assertThat(defaultedAccount.sentFolderId).isNull()
        assertThat(defaultedAccount.sentFolderSelection).isEqualTo(SpecialFolderSelection.AUTOMATIC)
        assertThat(defaultedAccount.spamFolderId).isNull()
        assertThat(defaultedAccount.spamFolderSelection).isEqualTo(SpecialFolderSelection.AUTOMATIC)
        assertThat(defaultedAccount.trashFolderId).isNull()
        assertThat(defaultedAccount.trashFolderSelection).isEqualTo(SpecialFolderSelection.AUTOMATIC)

        assertThat(defaultedAccount.identities).isEqualTo(identities)
        assertThat(defaultedAccount.notificationSettings).isEqualTo(notificationSettings)

        assertThat(defaultedAccount.isChangedVisibleLimits).isFalse()
    }

    @Test
    fun `applyOverwrites should return patched account when disabled`() {
        // arrange
        val resourceProvider = mock<CoreResourceProvider> {
            on { defaultIdentityDescription() } doReturn "Default Identity"
        }
        val account = createFakeAccount()
        val storage = mock<Storage> {
            on { contains("${account.id}.notifyNewMail") } doReturn false
            on { getBoolean("${account.id}.notifyNewMail", false) } doReturn false
            on { getBoolean("${account.id}.notifySelfNewMail", false) } doReturn false
        }
        val testSubject = DefaultAccountDefaultsProvider(
            resourceProvider = resourceProvider,
            featureFlagProvider = {
                FeatureFlagResult.Disabled
            },
        )

        // act
        val patchedAccount = testSubject.applyOverwrites(account, storage)

        // assert
        assertThat(patchedAccount.isNotifyNewMail).isFalse()
        assertThat(patchedAccount.isNotifySelfNewMail).isFalse()
    }

    @Test
    fun `applyOverwrites should return patched account when enabled`() {
        // arrange
        val resourceProvider = mock<CoreResourceProvider> {
            on { defaultIdentityDescription() } doReturn "Default Identity"
        }
        val account = createFakeAccount()
        val storage = mock<Storage> {
            on { contains("${account.id}.notifyNewMail") } doReturn false
            on { getBoolean("${account.id}.notifyNewMail", false) } doReturn false
            on { getBoolean("${account.id}.notifySelfNewMail", false) } doReturn false
        }
        val testSubject = DefaultAccountDefaultsProvider(
            resourceProvider = resourceProvider,
            featureFlagProvider = {
                FeatureFlagResult.Enabled
            },
        )

        // act
        val patchedAccount = testSubject.applyOverwrites(account, storage)

        // assert
        assertThat(patchedAccount.isNotifyNewMail).isTrue()
        assertThat(patchedAccount.isNotifySelfNewMail).isTrue()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `applyOverwrites updates account notification values from storage when storage contains isNotifyNewMail value`() {
        // arrange
        val resourceProvider = mock<CoreResourceProvider> {
            on { defaultIdentityDescription() } doReturn "Default Identity"
        }
        val account = createFakeAccount()
        val storage = mock<Storage> {
            on { contains("${account.id}.notifyNewMail") } doReturn true
            on { getBoolean("${account.id}.notifyNewMail", false) } doReturn false
            on { getBoolean("${account.id}.notifySelfNewMail", false) } doReturn false
        }
        val testSubject = DefaultAccountDefaultsProvider(
            resourceProvider = resourceProvider,
            featureFlagProvider = {
                FeatureFlagResult.Enabled
            },
        )

        // act
        val patchedAccount = testSubject.applyOverwrites(account, storage)

        // assert
        assertThat(patchedAccount.isNotifyNewMail).isFalse()
        assertThat(patchedAccount.isNotifySelfNewMail).isFalse()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `applyOverwrites updates account notification values from featureFlag values when storage does not contain isNotifyNewMail value`() {
        // arrange
        val resourceProvider = mock<CoreResourceProvider> {
            on { defaultIdentityDescription() } doReturn "Default Identity"
        }
        val account = createFakeAccount()
        val storage = mock<Storage> {
            on { contains("${account.id}.notifyNewMail") } doReturn false
            on { getBoolean("${account.id}.notifyNewMail", false) } doReturn false
            on { getBoolean("${account.id}.notifySelfNewMail", false) } doReturn false
        }
        val testSubject = DefaultAccountDefaultsProvider(
            resourceProvider = resourceProvider,
            featureFlagProvider = {
                FeatureFlagResult.Enabled
            },
        )

        // act
        val patchedAccount = testSubject.applyOverwrites(account, storage)

        // assert
        assertThat(patchedAccount.isNotifyNewMail).isTrue()
        assertThat(patchedAccount.isNotifySelfNewMail).isTrue()
    }

    private fun createFakeAccount(): LegacyAccount {
        val accountId = AccountIdFactory.create()
        return LegacyAccount(
            id = accountId,
            name = "name",
            email = "user@example.com",
            profile = ProfileDto(
                id = accountId,
                name = "name",
                color = -1,
                avatar = AvatarDto(
                    id = accountId,
                    avatarType = AvatarTypeDto.MONOGRAM,
                    avatarMonogram = "NA",
                    avatarImageUri = null,
                    avatarIconName = null,
                ),
            ),
            incomingServerSettings = ServerSettings(
                type = "imap",
                host = "host",
                port = 993,
                connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.PLAIN,
                username = "user",
                password = "pass",
                clientCertificateAlias = null,
            ),
            outgoingServerSettings = ServerSettings(
                type = "smtp",
                host = "host",
                port = 465,
                connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
                authenticationType = AuthType.PLAIN,
                username = "user",
                password = "pass",
                clientCertificateAlias = null,
            ),
            identities = listOf(
                Identity(
                    signatureUse = false,
                    signature = null,
                    description = "Default Identity",
                ),
            ),
        )
    }
}
