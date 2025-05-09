package app.k9mail.legacy.account

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import net.thunderbird.core.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.notification.NotificationSettings

class LegacyAccountWrapperTest {

    @Test
    fun `from account should return wrapper`() {
        // arrange
        val account = createAccount()
        val expected = createAccountWrapper()

        // act
        val result = LegacyAccountWrapper.from(account)

        // assert
        assertThat(result).isEqualTo(expected)
    }

    @Suppress("LongMethod")
    @Test
    fun `to wrapper should return account`() {
        // arrange
        val wrapper = createAccountWrapper()

        // act
        val result = LegacyAccountWrapper.to(wrapper)

        // assert
        assertThat(result.uuid).isEqualTo("uuid")
        assertThat(result.isSensitiveDebugLoggingEnabled).isEqualTo(defaultIsSensitiveDebugLoggingEnabled)
        assertThat(result.identities).isEqualTo(defaultIdentities)
        assertThat(result.name).isEqualTo("displayName")
        assertThat(result.email).isEqualTo("demo@example.com")
        assertThat(result.deletePolicy).isEqualTo(DeletePolicy.SEVEN_DAYS)
        assertThat(result.incomingServerSettings).isEqualTo(defaultIncomingServerSettings)
        assertThat(result.outgoingServerSettings).isEqualTo(defaultOutgoingServerSettings)
        assertThat(result.oAuthState).isEqualTo("oAuthState")
        assertThat(result.alwaysBcc).isEqualTo("alwaysBcc")
        assertThat(result.automaticCheckIntervalMinutes).isEqualTo(60)
        assertThat(result.displayCount).isEqualTo(10)
        assertThat(result.chipColor).isEqualTo(0xFFFF0000.toInt())
        assertThat(result.isNotifyNewMail).isEqualTo(true)
        assertThat(result.folderNotifyNewMailMode).isEqualTo(FolderMode.FIRST_AND_SECOND_CLASS)
        assertThat(result.isNotifySelfNewMail).isEqualTo(true)
        assertThat(result.isNotifyContactsMailOnly).isEqualTo(true)
        assertThat(result.isIgnoreChatMessages).isEqualTo(true)
        assertThat(result.legacyInboxFolder).isEqualTo("legacyInboxFolder")
        assertThat(result.importedDraftsFolder).isEqualTo("importedDraftsFolder")
        assertThat(result.importedSentFolder).isEqualTo("importedSentFolder")
        assertThat(result.importedTrashFolder).isEqualTo("importedTrashFolder")
        assertThat(result.importedArchiveFolder).isEqualTo("importedArchiveFolder")
        assertThat(result.importedSpamFolder).isEqualTo("importedSpamFolder")
        assertThat(result.inboxFolderId).isEqualTo(1)
        assertThat(result.outboxFolderId).isEqualTo(2)
        assertThat(result.draftsFolderId).isEqualTo(3)
        assertThat(result.sentFolderId).isEqualTo(4)
        assertThat(result.trashFolderId).isEqualTo(5)
        assertThat(result.archiveFolderId).isEqualTo(6)
        assertThat(result.spamFolderId).isEqualTo(7)
        assertThat(result.draftsFolderSelection).isEqualTo(SpecialFolderSelection.MANUAL)
        assertThat(result.sentFolderSelection).isEqualTo(SpecialFolderSelection.MANUAL)
        assertThat(result.trashFolderSelection).isEqualTo(SpecialFolderSelection.MANUAL)
        assertThat(result.archiveFolderSelection).isEqualTo(SpecialFolderSelection.MANUAL)
        assertThat(result.spamFolderSelection).isEqualTo(SpecialFolderSelection.MANUAL)
        assertThat(result.importedAutoExpandFolder).isEqualTo("importedAutoExpandFolder")
        assertThat(result.autoExpandFolderId).isEqualTo(8)
        assertThat(result.folderDisplayMode).isEqualTo(FolderMode.FIRST_AND_SECOND_CLASS)
        assertThat(result.folderSyncMode).isEqualTo(FolderMode.FIRST_AND_SECOND_CLASS)
        assertThat(result.folderPushMode).isEqualTo(FolderMode.FIRST_AND_SECOND_CLASS)
        assertThat(result.accountNumber).isEqualTo(11)
        assertThat(result.isNotifySync).isEqualTo(true)
        assertThat(result.sortType).isEqualTo(SortType.SORT_SUBJECT)
        assertThat(result.sortAscending).isEqualTo(
            mutableMapOf(
                SortType.SORT_SUBJECT to false,
            ),
        )
        assertThat(result.showPictures).isEqualTo(ShowPictures.ALWAYS)
        assertThat(result.isSignatureBeforeQuotedText).isEqualTo(true)
        assertThat(result.expungePolicy).isEqualTo(Expunge.EXPUNGE_MANUALLY)
        assertThat(result.maxPushFolders).isEqualTo(12)
        assertThat(result.idleRefreshMinutes).isEqualTo(13)
        assertThat(result.useCompression).isEqualTo(false)
        assertThat(result.isSendClientInfoEnabled).isEqualTo(false)
        assertThat(result.isSubscribedFoldersOnly).isEqualTo(false)
        assertThat(result.maximumPolledMessageAge).isEqualTo(14)
        assertThat(result.maximumAutoDownloadMessageSize).isEqualTo(15)
        assertThat(result.messageFormat).isEqualTo(MessageFormat.TEXT)
        assertThat(result.isMessageFormatAuto).isEqualTo(true)
        assertThat(result.isMessageReadReceipt).isEqualTo(true)
        assertThat(result.quoteStyle).isEqualTo(QuoteStyle.HEADER)
        assertThat(result.quotePrefix).isEqualTo("quotePrefix")
        assertThat(result.isDefaultQuotedTextShown).isEqualTo(true)
        assertThat(result.isReplyAfterQuote).isEqualTo(true)
        assertThat(result.isStripSignature).isEqualTo(true)
        assertThat(result.isSyncRemoteDeletions).isEqualTo(true)
        assertThat(result.openPgpProvider).isEqualTo("openPgpProvider")
        assertThat(result.openPgpKey).isEqualTo(16)
        assertThat(result.autocryptPreferEncryptMutual).isEqualTo(true)
        assertThat(result.isOpenPgpHideSignOnly).isEqualTo(true)
        assertThat(result.isOpenPgpEncryptSubject).isEqualTo(true)
        assertThat(result.isOpenPgpEncryptAllDrafts).isEqualTo(true)
        assertThat(result.isMarkMessageAsReadOnView).isEqualTo(true)
        assertThat(result.isMarkMessageAsReadOnDelete).isEqualTo(true)
        assertThat(result.isAlwaysShowCcBcc).isEqualTo(true)
        assertThat(result.isRemoteSearchFullText).isEqualTo(false)
        assertThat(result.remoteSearchNumResults).isEqualTo(17)
        assertThat(result.isUploadSentMessages).isEqualTo(true)
        assertThat(result.lastSyncTime).isEqualTo(18)
        assertThat(result.lastFolderListRefreshTime).isEqualTo(19)
        assertThat(result.isFinishedSetup).isEqualTo(true)
        assertThat(result.messagesNotificationChannelVersion).isEqualTo(20)
        assertThat(result.isChangedVisibleLimits).isEqualTo(true)
        assertThat(result.lastSelectedFolderId).isEqualTo(21)
        assertThat(result.notificationSettings).isEqualTo(defaultNotificationSettings)
        assertThat(result.senderName).isEqualTo(defaultIdentities[0].name)
        assertThat(result.signatureUse).isEqualTo(defaultIdentities[0].signatureUse)
        assertThat(result.signature).isEqualTo(defaultIdentities[0].signature)
        assertThat(result.shouldMigrateToOAuth).isEqualTo(true)
    }

    private companion object {
        val defaultIsSensitiveDebugLoggingEnabled = { true }

        val defaultIncomingServerSettings = ServerSettings(
            type = "imap",
            host = "imap.example.com",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "test",
            password = "password",
            clientCertificateAlias = null,
        )

        val defaultOutgoingServerSettings = ServerSettings(
            type = "smtp",
            host = "smtp.example.com",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "test",
            password = "password",
            clientCertificateAlias = null,
        )

        val defaultIdentities = mutableListOf(
            Identity(
                email = "demo@example.com",
                name = "identityName",
                signatureUse = true,
                signature = "signature",
                description = "Demo User",
            ),
        )

        val defaultNotificationSettings = NotificationSettings()

        @Suppress("LongMethod")
        fun createAccount(): LegacyAccount {
            return LegacyAccount(
                uuid = "uuid",
                isSensitiveDebugLoggingEnabled = defaultIsSensitiveDebugLoggingEnabled,
            ).apply {
                identities = defaultIdentities
                name = "displayName"
                email = "demo@example.com"
                deletePolicy = DeletePolicy.SEVEN_DAYS
                incomingServerSettings = defaultIncomingServerSettings
                outgoingServerSettings = defaultOutgoingServerSettings
                oAuthState = "oAuthState"
                alwaysBcc = "alwaysBcc"
                automaticCheckIntervalMinutes = 60
                displayCount = 10
                chipColor = 0xFFFF0000.toInt()
                isNotifyNewMail = true
                folderNotifyNewMailMode = FolderMode.FIRST_AND_SECOND_CLASS
                isNotifySelfNewMail = true
                isNotifyContactsMailOnly = true
                isIgnoreChatMessages = true
                legacyInboxFolder = "legacyInboxFolder"
                importedDraftsFolder = "importedDraftsFolder"
                importedSentFolder = "importedSentFolder"
                importedTrashFolder = "importedTrashFolder"
                importedArchiveFolder = "importedArchiveFolder"
                importedSpamFolder = "importedSpamFolder"
                inboxFolderId = 1
                outboxFolderId = 2
                draftsFolderId = 3
                sentFolderId = 4
                trashFolderId = 5
                archiveFolderId = 6
                spamFolderId = 7
                draftsFolderSelection = SpecialFolderSelection.MANUAL
                sentFolderSelection = SpecialFolderSelection.MANUAL
                trashFolderSelection = SpecialFolderSelection.MANUAL
                archiveFolderSelection = SpecialFolderSelection.MANUAL
                spamFolderSelection = SpecialFolderSelection.MANUAL
                importedAutoExpandFolder = "importedAutoExpandFolder"
                autoExpandFolderId = 8
                folderDisplayMode = FolderMode.FIRST_AND_SECOND_CLASS
                folderSyncMode = FolderMode.FIRST_AND_SECOND_CLASS
                folderPushMode = FolderMode.FIRST_AND_SECOND_CLASS
                accountNumber = 11
                isNotifySync = true
                sortType = SortType.SORT_SUBJECT
                sortAscending = mutableMapOf(
                    SortType.SORT_SUBJECT to false,
                )
                showPictures = ShowPictures.ALWAYS
                isSignatureBeforeQuotedText = true
                expungePolicy = Expunge.EXPUNGE_MANUALLY
                maxPushFolders = 12
                idleRefreshMinutes = 13
                useCompression = false
                isSendClientInfoEnabled = false
                isSubscribedFoldersOnly = false
                maximumPolledMessageAge = 14
                maximumAutoDownloadMessageSize = 15
                messageFormat = MessageFormat.TEXT
                isMessageFormatAuto = true
                isMessageReadReceipt = true
                quoteStyle = QuoteStyle.HEADER
                quotePrefix = "quotePrefix"
                isDefaultQuotedTextShown = true
                isReplyAfterQuote = true
                isStripSignature = true
                isSyncRemoteDeletions = true
                openPgpProvider = "openPgpProvider"
                openPgpKey = 16
                autocryptPreferEncryptMutual = true
                isOpenPgpHideSignOnly = true
                isOpenPgpEncryptSubject = true
                isOpenPgpEncryptAllDrafts = true
                isMarkMessageAsReadOnView = true
                isMarkMessageAsReadOnDelete = true
                isAlwaysShowCcBcc = true
                isRemoteSearchFullText = false
                remoteSearchNumResults = 17
                isUploadSentMessages = true
                lastSyncTime = 18
                lastFolderListRefreshTime = 19
                isFinishedSetup = true
                messagesNotificationChannelVersion = 20
                isChangedVisibleLimits = true
                lastSelectedFolderId = 21
                notificationSettings = defaultNotificationSettings
                senderName = defaultIdentities[0].name
                signatureUse = defaultIdentities[0].signatureUse
                signature = defaultIdentities[0].signature
                shouldMigrateToOAuth = true
            }
        }

        @Suppress("LongMethod")
        fun createAccountWrapper(): LegacyAccountWrapper {
            return LegacyAccountWrapper(
                uuid = "uuid",
                isSensitiveDebugLoggingEnabled = defaultIsSensitiveDebugLoggingEnabled,
                name = "displayName",
                email = "demo@example.com",
                deletePolicy = DeletePolicy.SEVEN_DAYS,
                incomingServerSettings = defaultIncomingServerSettings,
                outgoingServerSettings = defaultOutgoingServerSettings,
                oAuthState = "oAuthState",
                alwaysBcc = "alwaysBcc",
                automaticCheckIntervalMinutes = 60,
                displayCount = 10,
                chipColor = 0xFFFF0000.toInt(),
                isNotifyNewMail = true,
                folderNotifyNewMailMode = FolderMode.FIRST_AND_SECOND_CLASS,
                isNotifySelfNewMail = true,
                isNotifyContactsMailOnly = true,
                isIgnoreChatMessages = true,
                legacyInboxFolder = "legacyInboxFolder",
                importedDraftsFolder = "importedDraftsFolder",
                importedSentFolder = "importedSentFolder",
                importedTrashFolder = "importedTrashFolder",
                importedArchiveFolder = "importedArchiveFolder",
                importedSpamFolder = "importedSpamFolder",
                inboxFolderId = 1,
                outboxFolderId = 2,
                draftsFolderId = 3,
                sentFolderId = 4,
                trashFolderId = 5,
                archiveFolderId = 6,
                spamFolderId = 7,
                draftsFolderSelection = SpecialFolderSelection.MANUAL,
                sentFolderSelection = SpecialFolderSelection.MANUAL,
                trashFolderSelection = SpecialFolderSelection.MANUAL,
                archiveFolderSelection = SpecialFolderSelection.MANUAL,
                spamFolderSelection = SpecialFolderSelection.MANUAL,
                importedAutoExpandFolder = "importedAutoExpandFolder",
                autoExpandFolderId = 8,
                folderDisplayMode = FolderMode.FIRST_AND_SECOND_CLASS,
                folderSyncMode = FolderMode.FIRST_AND_SECOND_CLASS,
                folderPushMode = FolderMode.FIRST_AND_SECOND_CLASS,
                accountNumber = 11,
                isNotifySync = true,
                sortType = SortType.SORT_SUBJECT,
                sortAscending = mutableMapOf(
                    SortType.SORT_SUBJECT to false,
                ),
                showPictures = ShowPictures.ALWAYS,
                isSignatureBeforeQuotedText = true,
                expungePolicy = Expunge.EXPUNGE_MANUALLY,
                maxPushFolders = 12,
                idleRefreshMinutes = 13,
                useCompression = false,
                isSendClientInfoEnabled = false,
                isSubscribedFoldersOnly = false,
                maximumPolledMessageAge = 14,
                maximumAutoDownloadMessageSize = 15,
                messageFormat = MessageFormat.TEXT,
                isMessageFormatAuto = true,
                isMessageReadReceipt = true,
                quoteStyle = QuoteStyle.HEADER,
                quotePrefix = "quotePrefix",
                isDefaultQuotedTextShown = true,
                isReplyAfterQuote = true,
                isStripSignature = true,
                isSyncRemoteDeletions = true,
                openPgpProvider = "openPgpProvider",
                openPgpKey = 16,
                autocryptPreferEncryptMutual = true,
                isOpenPgpHideSignOnly = true,
                isOpenPgpEncryptSubject = true,
                isOpenPgpEncryptAllDrafts = true,
                isMarkMessageAsReadOnView = true,
                isMarkMessageAsReadOnDelete = true,
                isAlwaysShowCcBcc = true,
                isRemoteSearchFullText = false,
                remoteSearchNumResults = 17,
                isUploadSentMessages = true,
                lastSyncTime = 18,
                lastFolderListRefreshTime = 19,
                isFinishedSetup = true,
                messagesNotificationChannelVersion = 20,
                isChangedVisibleLimits = true,
                lastSelectedFolderId = 21,
                identities = defaultIdentities,
                notificationSettings = defaultNotificationSettings,
                senderName = defaultIdentities[0].name,
                signatureUse = defaultIdentities[0].signatureUse,
                signature = defaultIdentities[0].signature,
                shouldMigrateToOAuth = true,
                displayName = "displayName",
            )
        }
    }
}
