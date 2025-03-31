package app.k9mail.legacy.account

import app.k9mail.legacy.account.AccountDefaultsProvider.Companion.NO_OPENPGP_KEY
import app.k9mail.legacy.notification.NotificationSettings
import com.fsck.k9.mail.ServerSettings

/**
 * A immutable wrapper for the [LegacyAccount] class.
 *
 * This class is used to store the account data in a way that is safe to pass between threads.
 *
 * Use LegacyAccountWrapper.from(account) to create a wrapper from an account.
 * Use LegacyAccountWrapper.to(wrapper) to create an account from a wrapper.
 */
@Suppress("LongMethod")
data class LegacyAccountWrapper(
    override val uuid: String,
    override val name: String?,
    override val email: String,
    private val isSensitiveDebugLoggingEnabled: () -> Boolean = { false },
    val deletePolicy: DeletePolicy,
    val incomingServerSettings: ServerSettings,
    val outgoingServerSettings: ServerSettings,
    val oAuthState: String?,
    val alwaysBcc: String?,
    val automaticCheckIntervalMinutes: Int,
    val displayCount: Int,
    val chipColor: Int,
    val isNotifyNewMail: Boolean,
    val folderNotifyNewMailMode: FolderMode,
    val isNotifySelfNewMail: Boolean,
    val isNotifyContactsMailOnly: Boolean,
    val isIgnoreChatMessages: Boolean,
    val legacyInboxFolder: String?,
    val importedDraftsFolder: String?,
    val importedSentFolder: String?,
    val importedTrashFolder: String?,
    val importedArchiveFolder: String?,
    val importedSpamFolder: String?,
    val inboxFolderId: Long?,
    val outboxFolderId: Long?,
    val draftsFolderId: Long?,
    val sentFolderId: Long?,
    val trashFolderId: Long?,
    val archiveFolderId: Long?,
    val spamFolderId: Long?,
    val draftsFolderSelection: SpecialFolderSelection,
    val sentFolderSelection: SpecialFolderSelection,
    val trashFolderSelection: SpecialFolderSelection,
    val archiveFolderSelection: SpecialFolderSelection,
    val spamFolderSelection: SpecialFolderSelection,
    val importedAutoExpandFolder: String?,
    val autoExpandFolderId: Long?,
    val folderDisplayMode: FolderMode,
    val folderSyncMode: FolderMode,
    val folderPushMode: FolderMode,
    val accountNumber: Int,
    val isNotifySync: Boolean,
    val sortType: SortType,
    val sortAscending: Map<SortType, Boolean>,
    val showPictures: ShowPictures,
    val isSignatureBeforeQuotedText: Boolean,
    val expungePolicy: Expunge,
    val maxPushFolders: Int,
    val idleRefreshMinutes: Int,
    val useCompression: Boolean,
    val isSendClientInfoEnabled: Boolean,
    val isSubscribedFoldersOnly: Boolean,
    val maximumPolledMessageAge: Int,
    val maximumAutoDownloadMessageSize: Int,
    val messageFormat: MessageFormat,
    val isMessageFormatAuto: Boolean,
    val isMessageReadReceipt: Boolean,
    val quoteStyle: QuoteStyle,
    val quotePrefix: String?,
    val isDefaultQuotedTextShown: Boolean,
    val isReplyAfterQuote: Boolean,
    val isStripSignature: Boolean,
    val isSyncRemoteDeletions: Boolean,
    val openPgpProvider: String?,
    val openPgpKey: Long,
    val autocryptPreferEncryptMutual: Boolean,
    val isOpenPgpHideSignOnly: Boolean,
    val isOpenPgpEncryptSubject: Boolean,
    val isOpenPgpEncryptAllDrafts: Boolean,
    val isMarkMessageAsReadOnView: Boolean,
    val isMarkMessageAsReadOnDelete: Boolean,
    val isAlwaysShowCcBcc: Boolean,
    val isRemoteSearchFullText: Boolean,
    val remoteSearchNumResults: Int,
    val isUploadSentMessages: Boolean,
    val lastSyncTime: Long,
    val lastFolderListRefreshTime: Long,
    val isFinishedSetup: Boolean,
    val messagesNotificationChannelVersion: Int,
    val isChangedVisibleLimits: Boolean,
    val lastSelectedFolderId: Long?,
    val identities: List<Identity>,
    val notificationSettings: NotificationSettings,
    val displayName: String,
    val senderName: String?,
    val signatureUse: Boolean,
    val signature: String?,
    val shouldMigrateToOAuth: Boolean,
) : BaseAccount {

    fun hasDraftsFolder(): Boolean {
        return draftsFolderId != null
    }

    fun hasSentFolder(): Boolean {
        return sentFolderId != null
    }

    fun hasTrashFolder(): Boolean {
        return trashFolderId != null
    }

    fun hasArchiveFolder(): Boolean {
        return archiveFolderId != null
    }

    fun hasSpamFolder(): Boolean {
        return spamFolderId != null
    }

    fun isOpenPgpProviderConfigured(): Boolean {
        return openPgpProvider != null
    }

    fun hasOpenPgpKey(): Boolean {
        return openPgpKey != NO_OPENPGP_KEY
    }

    companion object {
        @Suppress("LongMethod")
        fun from(account: LegacyAccount): LegacyAccountWrapper {
            return LegacyAccountWrapper(
                uuid = account.uuid,
                isSensitiveDebugLoggingEnabled = account.isSensitiveDebugLoggingEnabled,
                name = account.displayName,
                identities = account.identities,
                email = account.email,
                deletePolicy = account.deletePolicy,
                incomingServerSettings = account.incomingServerSettings,
                outgoingServerSettings = account.outgoingServerSettings,
                oAuthState = account.oAuthState,
                alwaysBcc = account.alwaysBcc,
                automaticCheckIntervalMinutes = account.automaticCheckIntervalMinutes,
                displayCount = account.displayCount,
                chipColor = account.chipColor,
                isNotifyNewMail = account.isNotifyNewMail,
                folderNotifyNewMailMode = account.folderNotifyNewMailMode,
                isNotifySelfNewMail = account.isNotifySelfNewMail,
                isNotifyContactsMailOnly = account.isNotifyContactsMailOnly,
                isIgnoreChatMessages = account.isIgnoreChatMessages,
                legacyInboxFolder = account.legacyInboxFolder,
                importedDraftsFolder = account.importedDraftsFolder,
                importedSentFolder = account.importedSentFolder,
                importedTrashFolder = account.importedTrashFolder,
                importedArchiveFolder = account.importedArchiveFolder,
                importedSpamFolder = account.importedSpamFolder,
                inboxFolderId = account.inboxFolderId,
                outboxFolderId = account.outboxFolderId,
                draftsFolderId = account.draftsFolderId,
                sentFolderId = account.sentFolderId,
                trashFolderId = account.trashFolderId,
                archiveFolderId = account.archiveFolderId,
                spamFolderId = account.spamFolderId,
                draftsFolderSelection = account.draftsFolderSelection,
                sentFolderSelection = account.sentFolderSelection,
                trashFolderSelection = account.trashFolderSelection,
                archiveFolderSelection = account.archiveFolderSelection,
                spamFolderSelection = account.spamFolderSelection,
                importedAutoExpandFolder = account.importedAutoExpandFolder,
                autoExpandFolderId = account.autoExpandFolderId,
                folderDisplayMode = account.folderDisplayMode,
                folderSyncMode = account.folderSyncMode,
                folderPushMode = account.folderPushMode,
                accountNumber = account.accountNumber,
                isNotifySync = account.isNotifySync,
                sortType = account.sortType,
                sortAscending = account.sortAscending,
                showPictures = account.showPictures,
                isSignatureBeforeQuotedText = account.isSignatureBeforeQuotedText,
                expungePolicy = account.expungePolicy,
                maxPushFolders = account.maxPushFolders,
                idleRefreshMinutes = account.idleRefreshMinutes,
                useCompression = account.useCompression,
                isSendClientInfoEnabled = account.isSendClientInfoEnabled,
                isSubscribedFoldersOnly = account.isSubscribedFoldersOnly,
                maximumPolledMessageAge = account.maximumPolledMessageAge,
                maximumAutoDownloadMessageSize = account.maximumAutoDownloadMessageSize,
                messageFormat = account.messageFormat,
                isMessageFormatAuto = account.isMessageFormatAuto,
                isMessageReadReceipt = account.isMessageReadReceipt,
                quoteStyle = account.quoteStyle,
                quotePrefix = account.quotePrefix,
                isDefaultQuotedTextShown = account.isDefaultQuotedTextShown,
                isReplyAfterQuote = account.isReplyAfterQuote,
                isStripSignature = account.isStripSignature,
                isSyncRemoteDeletions = account.isSyncRemoteDeletions,
                openPgpProvider = account.openPgpProvider,
                openPgpKey = account.openPgpKey,
                autocryptPreferEncryptMutual = account.autocryptPreferEncryptMutual,
                isOpenPgpHideSignOnly = account.isOpenPgpHideSignOnly,
                isOpenPgpEncryptSubject = account.isOpenPgpEncryptSubject,
                isOpenPgpEncryptAllDrafts = account.isOpenPgpEncryptAllDrafts,
                isMarkMessageAsReadOnView = account.isMarkMessageAsReadOnView,
                isMarkMessageAsReadOnDelete = account.isMarkMessageAsReadOnDelete,
                isAlwaysShowCcBcc = account.isAlwaysShowCcBcc,
                isRemoteSearchFullText = account.isRemoteSearchFullText,
                remoteSearchNumResults = account.remoteSearchNumResults,
                isUploadSentMessages = account.isUploadSentMessages,
                lastSyncTime = account.lastSyncTime,
                lastFolderListRefreshTime = account.lastFolderListRefreshTime,
                isFinishedSetup = account.isFinishedSetup,
                messagesNotificationChannelVersion = account.messagesNotificationChannelVersion,
                isChangedVisibleLimits = account.isChangedVisibleLimits,
                lastSelectedFolderId = account.lastSelectedFolderId,
                notificationSettings = account.notificationSettings,
                displayName = account.displayName,
                senderName = account.senderName,
                signatureUse = account.signatureUse,
                signature = account.signature,
                shouldMigrateToOAuth = account.shouldMigrateToOAuth,
            )
        }

        @Suppress("LongMethod")
        fun to(wrapper: LegacyAccountWrapper): LegacyAccount {
            return LegacyAccount(
                uuid = wrapper.uuid,
                isSensitiveDebugLoggingEnabled = wrapper.isSensitiveDebugLoggingEnabled,
            ).apply {
                identities = wrapper.identities.toMutableList()
                name = wrapper.displayName
                email = wrapper.email
                deletePolicy = wrapper.deletePolicy
                incomingServerSettings = wrapper.incomingServerSettings
                outgoingServerSettings = wrapper.outgoingServerSettings
                oAuthState = wrapper.oAuthState
                alwaysBcc = wrapper.alwaysBcc
                automaticCheckIntervalMinutes = wrapper.automaticCheckIntervalMinutes
                displayCount = wrapper.displayCount
                chipColor = wrapper.chipColor
                isNotifyNewMail = wrapper.isNotifyNewMail
                folderNotifyNewMailMode = wrapper.folderNotifyNewMailMode
                isNotifySelfNewMail = wrapper.isNotifySelfNewMail
                isNotifyContactsMailOnly = wrapper.isNotifyContactsMailOnly
                isIgnoreChatMessages = wrapper.isIgnoreChatMessages
                legacyInboxFolder = wrapper.legacyInboxFolder
                importedDraftsFolder = wrapper.importedDraftsFolder
                importedSentFolder = wrapper.importedSentFolder
                importedTrashFolder = wrapper.importedTrashFolder
                importedArchiveFolder = wrapper.importedArchiveFolder
                importedSpamFolder = wrapper.importedSpamFolder
                inboxFolderId = wrapper.inboxFolderId
                outboxFolderId = wrapper.outboxFolderId
                draftsFolderId = wrapper.draftsFolderId
                sentFolderId = wrapper.sentFolderId
                trashFolderId = wrapper.trashFolderId
                archiveFolderId = wrapper.archiveFolderId
                spamFolderId = wrapper.spamFolderId
                draftsFolderSelection = wrapper.draftsFolderSelection
                sentFolderSelection = wrapper.sentFolderSelection
                trashFolderSelection = wrapper.trashFolderSelection
                archiveFolderSelection = wrapper.archiveFolderSelection
                spamFolderSelection = wrapper.spamFolderSelection
                importedAutoExpandFolder = wrapper.importedAutoExpandFolder
                autoExpandFolderId = wrapper.autoExpandFolderId
                folderDisplayMode = wrapper.folderDisplayMode
                folderSyncMode = wrapper.folderSyncMode
                folderPushMode = wrapper.folderPushMode
                accountNumber = wrapper.accountNumber
                isNotifySync = wrapper.isNotifySync
                sortType = wrapper.sortType
                sortAscending = wrapper.sortAscending.toMutableMap()
                showPictures = wrapper.showPictures
                isSignatureBeforeQuotedText = wrapper.isSignatureBeforeQuotedText
                expungePolicy = wrapper.expungePolicy
                maxPushFolders = wrapper.maxPushFolders
                idleRefreshMinutes = wrapper.idleRefreshMinutes
                useCompression = wrapper.useCompression
                isSendClientInfoEnabled = wrapper.isSendClientInfoEnabled
                isSubscribedFoldersOnly = wrapper.isSubscribedFoldersOnly
                maximumPolledMessageAge = wrapper.maximumPolledMessageAge
                maximumAutoDownloadMessageSize = wrapper.maximumAutoDownloadMessageSize
                messageFormat = wrapper.messageFormat
                isMessageFormatAuto = wrapper.isMessageFormatAuto
                isMessageReadReceipt = wrapper.isMessageReadReceipt
                quoteStyle = wrapper.quoteStyle
                quotePrefix = wrapper.quotePrefix
                isDefaultQuotedTextShown = wrapper.isDefaultQuotedTextShown
                isReplyAfterQuote = wrapper.isReplyAfterQuote
                isStripSignature = wrapper.isStripSignature
                isSyncRemoteDeletions = wrapper.isSyncRemoteDeletions
                openPgpProvider = wrapper.openPgpProvider
                openPgpKey = wrapper.openPgpKey
                autocryptPreferEncryptMutual = wrapper.autocryptPreferEncryptMutual
                isOpenPgpHideSignOnly = wrapper.isOpenPgpHideSignOnly
                isOpenPgpEncryptSubject = wrapper.isOpenPgpEncryptSubject
                isOpenPgpEncryptAllDrafts = wrapper.isOpenPgpEncryptAllDrafts
                isMarkMessageAsReadOnView = wrapper.isMarkMessageAsReadOnView
                isMarkMessageAsReadOnDelete = wrapper.isMarkMessageAsReadOnDelete
                isAlwaysShowCcBcc = wrapper.isAlwaysShowCcBcc
                isRemoteSearchFullText = wrapper.isRemoteSearchFullText
                remoteSearchNumResults = wrapper.remoteSearchNumResults
                isUploadSentMessages = wrapper.isUploadSentMessages
                lastSyncTime = wrapper.lastSyncTime
                lastFolderListRefreshTime = wrapper.lastFolderListRefreshTime
                isFinishedSetup = wrapper.isFinishedSetup
                messagesNotificationChannelVersion = wrapper.messagesNotificationChannelVersion
                isChangedVisibleLimits = wrapper.isChangedVisibleLimits
                lastSelectedFolderId = wrapper.lastSelectedFolderId
                notificationSettings = wrapper.notificationSettings
                senderName = wrapper.senderName
                signatureUse = wrapper.signatureUse
                signature = wrapper.signature
                shouldMigrateToOAuth = wrapper.shouldMigrateToOAuth
            }
        }
    }
}
