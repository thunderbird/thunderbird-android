package net.thunderbird.core.android.account

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import kotlin.test.Test
import net.thunderbird.account.fake.FakeAccountData.ACCOUNT_ID
import net.thunderbird.account.fake.FakeAccountProfileData.PROFILE_COLOR
import net.thunderbird.account.fake.FakeAccountProfileData.PROFILE_NAME
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.notification.NotificationSettings

class LegacyAccountWrapperTest {

    @Suppress("LongMethod")
    @Test
    fun `should set defaults`() {
        // arrange
        val expected = createAccountWrapper()

        // act
        val result = LegacyAccountWrapper(
            isSensitiveDebugLoggingEnabled = isSensitiveDebugLoggingEnabled,
            id = ACCOUNT_ID,
            name = PROFILE_NAME,
            email = email,
            profile = profile,
            incomingServerSettings = incomingServerSettings,
            outgoingServerSettings = outgoingServerSettings,
            identities = identities,
        )

        // assert
        assertThat(expected).isEqualTo(result)
    }

    private companion object {
        val isSensitiveDebugLoggingEnabled = { true }

        const val email = "demo@example.com"

        val avatar = AvatarDto(
            avatarType = AvatarTypeDto.MONOGRAM,
            avatarMonogram = null,
            avatarImageUri = null,
            avatarIconName = null,
        )

        val profile = ProfileDto(
            id = ACCOUNT_ID,
            name = PROFILE_NAME,
            color = PROFILE_COLOR,
            avatar = avatar,
        )

        val incomingServerSettings = ServerSettings(
            type = "imap",
            host = "imap.example.com",
            port = 993,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "test",
            password = "password",
            clientCertificateAlias = null,
        )

        val outgoingServerSettings = ServerSettings(
            type = "smtp",
            host = "smtp.example.com",
            port = 465,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "test",
            password = "password",
            clientCertificateAlias = null,
        )

        val identities = mutableListOf(
            Identity(
                email = "demo@example.com",
                name = "identityName",
                signatureUse = true,
                signature = "signature",
                description = "Demo User",
            ),
        )

        val notificationSettings = NotificationSettings()

        @Suppress("LongMethod")
        fun createAccountWrapper(): LegacyAccountWrapper {
            return LegacyAccountWrapper(
                isSensitiveDebugLoggingEnabled = isSensitiveDebugLoggingEnabled,

                // [Account]
                id = ACCOUNT_ID,

                // [BaseAccount]
                name = PROFILE_NAME,
                email = email,

                // [AccountProfile]
                profile = profile,

                // Uncategorized
                deletePolicy = DeletePolicy.NEVER,
                incomingServerSettings = incomingServerSettings,
                outgoingServerSettings = outgoingServerSettings,
                oAuthState = null,
                alwaysBcc = null,
                automaticCheckIntervalMinutes = 0,
                displayCount = 0,
                isNotifyNewMail = false,
                folderNotifyNewMailMode = FolderMode.ALL,
                isNotifySelfNewMail = false,
                isNotifyContactsMailOnly = false,
                isIgnoreChatMessages = false,
                legacyInboxFolder = null,
                importedDraftsFolder = null,
                importedSentFolder = null,
                importedTrashFolder = null,
                importedArchiveFolder = null,
                importedSpamFolder = null,
                inboxFolderId = null,
                outboxFolderId = null,
                draftsFolderId = null,
                sentFolderId = null,
                trashFolderId = null,
                archiveFolderId = null,
                spamFolderId = null,
                draftsFolderSelection = SpecialFolderSelection.AUTOMATIC,
                sentFolderSelection = SpecialFolderSelection.AUTOMATIC,
                trashFolderSelection = SpecialFolderSelection.AUTOMATIC,
                archiveFolderSelection = SpecialFolderSelection.AUTOMATIC,
                spamFolderSelection = SpecialFolderSelection.AUTOMATIC,
                importedAutoExpandFolder = null,
                autoExpandFolderId = null,
                folderDisplayMode = FolderMode.NOT_SECOND_CLASS,
                folderSyncMode = FolderMode.FIRST_CLASS,
                folderPushMode = FolderMode.NONE,
                accountNumber = 0,
                isNotifySync = false,
                sortType = SortType.SORT_DATE,
                sortAscending = emptyMap(),
                showPictures = ShowPictures.NEVER,
                isSignatureBeforeQuotedText = false,
                expungePolicy = Expunge.EXPUNGE_IMMEDIATELY,
                maxPushFolders = 0,
                idleRefreshMinutes = 0,
                useCompression = true,
                isSendClientInfoEnabled = true,
                isSubscribedFoldersOnly = false,
                maximumPolledMessageAge = 0,
                maximumAutoDownloadMessageSize = 0,
                messageFormat = MessageFormat.HTML,
                isMessageFormatAuto = false,
                isMessageReadReceipt = false,
                quoteStyle = QuoteStyle.PREFIX,
                quotePrefix = null,
                isDefaultQuotedTextShown = false,
                isReplyAfterQuote = false,
                isStripSignature = false,
                isSyncRemoteDeletions = false,
                openPgpProvider = null,
                openPgpKey = 0,
                autocryptPreferEncryptMutual = false,
                isOpenPgpHideSignOnly = false,
                isOpenPgpEncryptSubject = false,
                isOpenPgpEncryptAllDrafts = false,
                isMarkMessageAsReadOnView = false,
                isMarkMessageAsReadOnDelete = false,
                isAlwaysShowCcBcc = false,
                isRemoteSearchFullText = false,
                remoteSearchNumResults = 0,
                isUploadSentMessages = false,
                lastSyncTime = 0,
                lastFolderListRefreshTime = 0,
                isFinishedSetup = false,
                messagesNotificationChannelVersion = 0,
                isChangedVisibleLimits = false,
                lastSelectedFolderId = null,
                identities = identities,
                notificationSettings = notificationSettings,
                senderName = identities[0].name,
                signatureUse = identities[0].signatureUse,
                signature = identities[0].signature,
                shouldMigrateToOAuth = false,
            )
        }
    }
}
