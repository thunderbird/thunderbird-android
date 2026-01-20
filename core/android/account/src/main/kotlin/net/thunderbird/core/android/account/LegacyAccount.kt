package net.thunderbird.core.android.account

import com.fsck.k9.mail.ServerSettings
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.NO_OPENPGP_KEY
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.Account
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.profile.ProfileDto
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.notification.NotificationSettings

/**
 * This class is used to store the account data in a way that is safe to pass between threads.
 */
data class LegacyAccount(
    val isSensitiveDebugLoggingEnabled: () -> Boolean = { false },

    // [Account]
    override val id: AccountId,

    // [BaseAccount]
    override val name: String?,
    override val email: String,

    // [AccountProfile]
    val profile: ProfileDto,

    // Uncategorized
    val deletePolicy: DeletePolicy = DeletePolicy.NEVER,
    val incomingServerSettings: ServerSettings,
    val outgoingServerSettings: ServerSettings,
    val oAuthState: String? = null,
    val alwaysBcc: String? = null,
    val automaticCheckIntervalMinutes: Int = 0,
    val displayCount: Int = 0,
    val isNotifyNewMail: Boolean = false,
    val folderNotifyNewMailMode: FolderMode = FolderMode.ALL,
    val isNotifySelfNewMail: Boolean = false,
    val isNotifyContactsMailOnly: Boolean = false,
    val isIgnoreChatMessages: Boolean = false,
    val legacyInboxFolder: String? = null,
    val importedDraftsFolder: String? = null,
    val importedSentFolder: String? = null,
    val importedTrashFolder: String? = null,
    val importedArchiveFolder: String? = null,
    val importedSpamFolder: String? = null,
    val inboxFolderId: Long? = null,
    val draftsFolderId: Long? = null,
    val sentFolderId: Long? = null,
    val trashFolderId: Long? = null,
    val archiveFolderId: Long? = null,
    val spamFolderId: Long? = null,
    val draftsFolderSelection: SpecialFolderSelection = SpecialFolderSelection.AUTOMATIC,
    val sentFolderSelection: SpecialFolderSelection = SpecialFolderSelection.AUTOMATIC,
    val trashFolderSelection: SpecialFolderSelection = SpecialFolderSelection.AUTOMATIC,
    val archiveFolderSelection: SpecialFolderSelection = SpecialFolderSelection.AUTOMATIC,
    val spamFolderSelection: SpecialFolderSelection = SpecialFolderSelection.AUTOMATIC,
    val importedAutoExpandFolder: String? = null,
    val autoExpandFolderId: Long? = null,
    val folderDisplayMode: FolderMode = FolderMode.NOT_SECOND_CLASS,
    val folderSyncMode: FolderMode = FolderMode.FIRST_CLASS,
    val folderPushMode: FolderMode = FolderMode.NONE,
    val accountNumber: Int = 0,
    val isNotifySync: Boolean = false,
    val sortType: SortType = SortType.SORT_DATE,
    val sortAscending: Map<SortType, Boolean> = emptyMap(),
    val showPictures: ShowPictures = ShowPictures.NEVER,
    val isSignatureBeforeQuotedText: Boolean = false,
    val expungePolicy: Expunge = Expunge.EXPUNGE_IMMEDIATELY,
    val maxPushFolders: Int = 0,
    val idleRefreshMinutes: Int = 0,
    val useCompression: Boolean = true,
    val isSendClientInfoEnabled: Boolean = true,
    val isSubscribedFoldersOnly: Boolean = false,
    val maximumPolledMessageAge: Int = 0,
    val maximumAutoDownloadMessageSize: Int = 0,
    val messageFormat: MessageFormat = MessageFormat.HTML,
    val isMessageFormatAuto: Boolean = false,
    val isMessageReadReceipt: Boolean = false,
    val quoteStyle: QuoteStyle = QuoteStyle.PREFIX,
    val quotePrefix: String? = null,
    val isDefaultQuotedTextShown: Boolean = false,
    val isReplyAfterQuote: Boolean = false,
    val isStripSignature: Boolean = false,
    val isSyncRemoteDeletions: Boolean = false,
    val openPgpProvider: String? = null,
    val openPgpKey: Long = 0,
    val autocryptPreferEncryptMutual: Boolean = false,
    val isOpenPgpHideSignOnly: Boolean = false,
    val isOpenPgpEncryptSubject: Boolean = false,
    val isOpenPgpEncryptAllDrafts: Boolean = false,
    val isMarkMessageAsReadOnView: Boolean = false,
    val isMarkMessageAsReadOnDelete: Boolean = false,
    val isAlwaysShowCcBcc: Boolean = false,
    val isRemoteSearchFullText: Boolean = false,
    val remoteSearchNumResults: Int = 0,
    val isUploadSentMessages: Boolean = false,
    val lastSyncTime: Long = 0,
    val lastFolderListRefreshTime: Long = 0,
    val isFinishedSetup: Boolean = false,
    val messagesNotificationChannelVersion: Int = 0,
    val isChangedVisibleLimits: Boolean = false,
    val lastSelectedFolderId: Long? = null,
    val identities: List<Identity>,
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val senderName: String? = identities[0].name,
    val signatureUse: Boolean = identities[0].signatureUse,
    val signature: String? = identities[0].signature,
    val shouldMigrateToOAuth: Boolean = false,
    val folderPathDelimiter: FolderPathDelimiter = "/",
) : Account, BaseAccount {

    override val uuid: String = id.toString()

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

    fun isIncomingServerPop3(): Boolean =
        incomingServerSettings.type == Protocols.POP3
}
