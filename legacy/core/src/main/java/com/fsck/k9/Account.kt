package com.fsck.k9

import app.k9mail.legacy.account.BaseAccount
import app.k9mail.legacy.account.Identity
import app.k9mail.legacy.notification.NotificationSettings
import com.fsck.k9.backend.api.SyncConfig.ExpungePolicy
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.ServerSettings
import java.util.Calendar
import java.util.Date

/**
 * Account stores all of the settings for a single account defined by the user. Each account is defined by a UUID.
 */
class Account(override val uuid: String) : BaseAccount {
    @get:Synchronized
    @set:Synchronized
    var deletePolicy = DeletePolicy.NEVER

    @get:Synchronized
    @set:Synchronized
    private var internalIncomingServerSettings: ServerSettings? = null

    @get:Synchronized
    @set:Synchronized
    private var internalOutgoingServerSettings: ServerSettings? = null

    var incomingServerSettings: ServerSettings
        get() = internalIncomingServerSettings ?: error("Incoming server settings not set yet")
        set(value) {
            internalIncomingServerSettings = value
        }

    var outgoingServerSettings: ServerSettings
        get() = internalOutgoingServerSettings ?: error("Outgoing server settings not set yet")
        set(value) {
            internalOutgoingServerSettings = value
        }

    @get:Synchronized
    @set:Synchronized
    var oAuthState: String? = null

    /**
     * Storage provider ID, used to locate and manage the underlying DB/file storage.
     */
    @get:Synchronized
    @set:Synchronized
    var localStorageProviderId: String? = null

    @get:Synchronized
    @set:Synchronized
    override var name: String? = null
        set(value) {
            field = value?.takeIf { it.isNotEmpty() }
        }

    @get:Synchronized
    @set:Synchronized
    var alwaysBcc: String? = null

    /**
     * -1 for never.
     */
    @get:Synchronized
    @set:Synchronized
    var automaticCheckIntervalMinutes = 0

    @get:Synchronized
    @set:Synchronized
    var displayCount = 0
        set(value) {
            if (field != value) {
                field = value.takeIf { it != -1 } ?: K9.DEFAULT_VISIBLE_LIMIT
                isChangedVisibleLimits = true
            }
        }

    @get:Synchronized
    @set:Synchronized
    var chipColor = 0

    @get:Synchronized
    @set:Synchronized
    var isNotifyNewMail = false

    @get:Synchronized
    @set:Synchronized
    var folderNotifyNewMailMode = FolderMode.ALL

    @get:Synchronized
    @set:Synchronized
    var isNotifySelfNewMail = false

    @get:Synchronized
    @set:Synchronized
    var isNotifyContactsMailOnly = false

    @get:Synchronized
    @set:Synchronized
    var isIgnoreChatMessages = false

    @get:Synchronized
    @set:Synchronized
    var legacyInboxFolder: String? = null

    @get:Synchronized
    @set:Synchronized
    var importedDraftsFolder: String? = null

    @get:Synchronized
    @set:Synchronized
    var importedSentFolder: String? = null

    @get:Synchronized
    @set:Synchronized
    var importedTrashFolder: String? = null

    @get:Synchronized
    @set:Synchronized
    var importedArchiveFolder: String? = null

    @get:Synchronized
    @set:Synchronized
    var importedSpamFolder: String? = null

    @get:Synchronized
    @set:Synchronized
    var inboxFolderId: Long? = null

    @get:Synchronized
    @set:Synchronized
    var outboxFolderId: Long? = null

    @get:Synchronized
    @set:Synchronized
    var draftsFolderId: Long? = null

    @get:Synchronized
    @set:Synchronized
    var sentFolderId: Long? = null

    @get:Synchronized
    @set:Synchronized
    var trashFolderId: Long? = null

    @get:Synchronized
    @set:Synchronized
    var archiveFolderId: Long? = null

    @get:Synchronized
    @set:Synchronized
    var spamFolderId: Long? = null

    @get:Synchronized
    var draftsFolderSelection = SpecialFolderSelection.AUTOMATIC

    @get:Synchronized
    var sentFolderSelection = SpecialFolderSelection.AUTOMATIC

    @get:Synchronized
    var trashFolderSelection = SpecialFolderSelection.AUTOMATIC

    @get:Synchronized
    var archiveFolderSelection = SpecialFolderSelection.AUTOMATIC

    @get:Synchronized
    var spamFolderSelection = SpecialFolderSelection.AUTOMATIC

    @get:Synchronized
    @set:Synchronized
    var importedAutoExpandFolder: String? = null

    @get:Synchronized
    @set:Synchronized
    var autoExpandFolderId: Long? = null

    @get:Synchronized
    @set:Synchronized
    var folderDisplayMode = FolderMode.NOT_SECOND_CLASS

    @get:Synchronized
    @set:Synchronized
    var folderSyncMode = FolderMode.FIRST_CLASS

    @get:Synchronized
    @set:Synchronized
    var folderPushMode = FolderMode.NONE

    @get:Synchronized
    @set:Synchronized
    var accountNumber = 0

    @get:Synchronized
    @set:Synchronized
    var isNotifySync = false

    @get:Synchronized
    @set:Synchronized
    var sortType: SortType = SortType.SORT_DATE

    private val sortAscending: MutableMap<SortType, Boolean> = mutableMapOf()

    @get:Synchronized
    @set:Synchronized
    var showPictures = ShowPictures.NEVER

    @get:Synchronized
    @set:Synchronized
    var isSignatureBeforeQuotedText = false

    @get:Synchronized
    @set:Synchronized
    var expungePolicy = Expunge.EXPUNGE_IMMEDIATELY

    @get:Synchronized
    @set:Synchronized
    var maxPushFolders = 0

    @get:Synchronized
    @set:Synchronized
    var idleRefreshMinutes = 0

    @get:JvmName("useCompression")
    @get:Synchronized
    @set:Synchronized
    var useCompression = true

    @get:Synchronized
    @set:Synchronized
    var isSendClientInfoEnabled = true

    @get:Synchronized
    @set:Synchronized
    var isSubscribedFoldersOnly = false

    @get:Synchronized
    @set:Synchronized
    var maximumPolledMessageAge = 0

    @get:Synchronized
    @set:Synchronized
    var maximumAutoDownloadMessageSize = 0

    @get:Synchronized
    @set:Synchronized
    var messageFormat = MessageFormat.HTML

    @get:Synchronized
    @set:Synchronized
    var isMessageFormatAuto = false

    @get:Synchronized
    @set:Synchronized
    var isMessageReadReceipt = false

    @get:Synchronized
    @set:Synchronized
    var quoteStyle = QuoteStyle.PREFIX

    @get:Synchronized
    @set:Synchronized
    var quotePrefix: String? = null

    @get:Synchronized
    @set:Synchronized
    var isDefaultQuotedTextShown = false

    @get:Synchronized
    @set:Synchronized
    var isReplyAfterQuote = false

    @get:Synchronized
    @set:Synchronized
    var isStripSignature = false

    @get:Synchronized
    @set:Synchronized
    var isSyncRemoteDeletions = false

    @get:Synchronized
    @set:Synchronized
    var openPgpProvider: String? = null
        set(value) {
            field = value?.takeIf { it.isNotEmpty() }
        }

    @get:Synchronized
    @set:Synchronized
    var openPgpKey: Long = 0

    @get:Synchronized
    @set:Synchronized
    var autocryptPreferEncryptMutual = false

    @get:Synchronized
    @set:Synchronized
    var isOpenPgpHideSignOnly = false

    @get:Synchronized
    @set:Synchronized
    var isOpenPgpEncryptSubject = false

    @get:Synchronized
    @set:Synchronized
    var isOpenPgpEncryptAllDrafts = false

    @get:Synchronized
    @set:Synchronized
    var isMarkMessageAsReadOnView = false

    @get:Synchronized
    @set:Synchronized
    var isMarkMessageAsReadOnDelete = false

    @get:Synchronized
    @set:Synchronized
    var isAlwaysShowCcBcc = false

    // Temporarily disabled
    @get:Synchronized
    @set:Synchronized
    var isRemoteSearchFullText = false
        get() = false

    @get:Synchronized
    @set:Synchronized
    var remoteSearchNumResults = 0
        set(value) {
            field = value.coerceAtLeast(0)
        }

    @get:Synchronized
    @set:Synchronized
    var isUploadSentMessages = false

    @get:Synchronized
    @set:Synchronized
    var lastSyncTime: Long = 0

    @get:Synchronized
    @set:Synchronized
    var lastFolderListRefreshTime: Long = 0

    @get:Synchronized
    var isFinishedSetup = false
        private set

    @get:Synchronized
    @set:Synchronized
    var messagesNotificationChannelVersion = 0

    @get:Synchronized
    @set:Synchronized
    var isChangedVisibleLimits = false
        private set

    /**
     * Database ID of the folder that was last selected for a copy or move operation.
     *
     * Note: For now this value isn't persisted. So it will be reset when K-9 Mail is restarted.
     */
    @get:Synchronized
    var lastSelectedFolderId: Long? = null
        private set

    @get:Synchronized
    @set:Synchronized
    var identities: MutableList<Identity> = mutableListOf()
        set(value) {
            field = value.toMutableList()
        }

    @get:Synchronized
    var notificationSettings = NotificationSettings()
        private set

    val displayName: String
        get() = name ?: email

    @get:Synchronized
    @set:Synchronized
    override var email: String
        get() = identities[0].email!!
        set(email) {
            val newIdentity = identities[0].withEmail(email)
            identities[0] = newIdentity
        }

    @get:Synchronized
    @set:Synchronized
    var senderName: String?
        get() = identities[0].name
        set(name) {
            val newIdentity = identities[0].withName(name)
            identities[0] = newIdentity
        }

    @get:Synchronized
    @set:Synchronized
    var signatureUse: Boolean
        get() = identities[0].signatureUse
        set(signatureUse) {
            val newIdentity = identities[0].withSignatureUse(signatureUse)
            identities[0] = newIdentity
        }

    @get:Synchronized
    @set:Synchronized
    var signature: String?
        get() = identities[0].signature
        set(signature) {
            val newIdentity = identities[0].withSignature(signature)
            identities[0] = newIdentity
        }

    @get:JvmName("shouldMigrateToOAuth")
    @get:Synchronized
    @set:Synchronized
    var shouldMigrateToOAuth = false

    /**
     * @param automaticCheckIntervalMinutes or -1 for never.
     */
    @Synchronized
    fun updateAutomaticCheckIntervalMinutes(automaticCheckIntervalMinutes: Int): Boolean {
        val oldInterval = this.automaticCheckIntervalMinutes
        this.automaticCheckIntervalMinutes = automaticCheckIntervalMinutes

        return oldInterval != automaticCheckIntervalMinutes
    }

    @Synchronized
    fun setDraftsFolderId(folderId: Long?, selection: SpecialFolderSelection) {
        draftsFolderId = folderId
        draftsFolderSelection = selection
    }

    @Synchronized
    fun hasDraftsFolder(): Boolean {
        return draftsFolderId != null
    }

    @Synchronized
    fun setSentFolderId(folderId: Long?, selection: SpecialFolderSelection) {
        sentFolderId = folderId
        sentFolderSelection = selection
    }

    @Synchronized
    fun hasSentFolder(): Boolean {
        return sentFolderId != null
    }

    @Synchronized
    fun setTrashFolderId(folderId: Long?, selection: SpecialFolderSelection) {
        trashFolderId = folderId
        trashFolderSelection = selection
    }

    @Synchronized
    fun hasTrashFolder(): Boolean {
        return trashFolderId != null
    }

    @Synchronized
    fun setArchiveFolderId(folderId: Long?, selection: SpecialFolderSelection) {
        archiveFolderId = folderId
        archiveFolderSelection = selection
    }

    @Synchronized
    fun hasArchiveFolder(): Boolean {
        return archiveFolderId != null
    }

    @Synchronized
    fun setSpamFolderId(folderId: Long?, selection: SpecialFolderSelection) {
        spamFolderId = folderId
        spamFolderSelection = selection
    }

    @Synchronized
    fun hasSpamFolder(): Boolean {
        return spamFolderId != null
    }

    @Synchronized
    fun updateFolderSyncMode(syncMode: FolderMode): Boolean {
        val oldSyncMode = folderSyncMode
        folderSyncMode = syncMode

        return (oldSyncMode == FolderMode.NONE && syncMode != FolderMode.NONE) ||
            (oldSyncMode != FolderMode.NONE && syncMode == FolderMode.NONE)
    }

    @Synchronized
    fun isSortAscending(sortType: SortType): Boolean {
        return sortAscending.getOrPut(sortType) { sortType.isDefaultAscending }
    }

    @Synchronized
    fun setSortAscending(sortType: SortType, sortAscending: Boolean) {
        this.sortAscending[sortType] = sortAscending
    }

    @Synchronized
    fun replaceIdentities(identities: List<Identity>) {
        this.identities = identities.toMutableList()
    }

    @Synchronized
    fun getIdentity(index: Int): Identity {
        if (index !in identities.indices) error("Identity with index $index not found")

        return identities[index]
    }

    fun isAnIdentity(addresses: Array<Address>?): Boolean {
        if (addresses == null) return false

        return addresses.any { address -> isAnIdentity(address) }
    }

    fun isAnIdentity(address: Address): Boolean {
        return findIdentity(address) != null
    }

    @Synchronized
    fun findIdentity(address: Address): Identity? {
        return identities.find { identity ->
            identity.email.equals(address.address, ignoreCase = true)
        }
    }

    val earliestPollDate: Date?
        get() {
            val age = maximumPolledMessageAge.takeIf { it >= 0 } ?: return null

            val now = Calendar.getInstance()
            now[Calendar.HOUR_OF_DAY] = 0
            now[Calendar.MINUTE] = 0
            now[Calendar.SECOND] = 0
            now[Calendar.MILLISECOND] = 0

            if (age < 28) {
                now.add(Calendar.DATE, age * -1)
            } else {
                when (age) {
                    28 -> now.add(Calendar.MONTH, -1)
                    56 -> now.add(Calendar.MONTH, -2)
                    84 -> now.add(Calendar.MONTH, -3)
                    168 -> now.add(Calendar.MONTH, -6)
                    365 -> now.add(Calendar.YEAR, -1)
                }
            }

            return now.time
        }

    val isOpenPgpProviderConfigured: Boolean
        get() = openPgpProvider != null

    @Synchronized
    fun hasOpenPgpKey(): Boolean {
        return openPgpKey != NO_OPENPGP_KEY
    }

    @Synchronized
    fun setLastSelectedFolderId(folderId: Long) {
        lastSelectedFolderId = folderId
    }

    @Synchronized
    fun resetChangeMarkers() {
        isChangedVisibleLimits = false
    }

    @Synchronized
    fun markSetupFinished() {
        isFinishedSetup = true
    }

    @Synchronized
    fun updateNotificationSettings(block: (oldNotificationSettings: NotificationSettings) -> NotificationSettings) {
        notificationSettings = block(notificationSettings)
    }

    override fun toString(): String {
        return if (K9.isSensitiveDebugLoggingEnabled) displayName else uuid
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Account) {
            other.uuid == uuid
        } else {
            super.equals(other)
        }
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    enum class FolderMode {
        NONE,
        ALL,
        FIRST_CLASS,
        FIRST_AND_SECOND_CLASS,
        NOT_SECOND_CLASS,
    }

    enum class SpecialFolderSelection {
        AUTOMATIC,
        MANUAL,
    }

    enum class ShowPictures {
        NEVER,
        ALWAYS,
        ONLY_FROM_CONTACTS,
    }

    enum class QuoteStyle {
        PREFIX,
        HEADER,
    }

    enum class MessageFormat {
        TEXT,
        HTML,
        AUTO,
    }

    enum class Expunge {
        EXPUNGE_IMMEDIATELY,
        EXPUNGE_MANUALLY,
        EXPUNGE_ON_POLL,
        ;

        fun toBackendExpungePolicy(): ExpungePolicy = when (this) {
            EXPUNGE_IMMEDIATELY -> ExpungePolicy.IMMEDIATELY
            EXPUNGE_MANUALLY -> ExpungePolicy.MANUALLY
            EXPUNGE_ON_POLL -> ExpungePolicy.ON_POLL
        }
    }

    enum class DeletePolicy(@JvmField val setting: Int) {
        NEVER(0),
        SEVEN_DAYS(1),
        ON_DELETE(2),
        MARK_AS_READ(3),
        ;

        companion object {
            fun fromInt(initialSetting: Int): DeletePolicy {
                return entries.find { it.setting == initialSetting } ?: error("DeletePolicy $initialSetting unknown")
            }
        }
    }

    enum class SortType(val isDefaultAscending: Boolean) {
        SORT_DATE(false),
        SORT_ARRIVAL(false),
        SORT_SUBJECT(true),
        SORT_SENDER(true),
        SORT_UNREAD(true),
        SORT_FLAGGED(true),
        SORT_ATTACHMENT(true),
    }

    companion object {
        /**
         * Fixed name of outbox - not actually displayed.
         */
        const val OUTBOX_NAME = "Outbox"

        @JvmField
        val DEFAULT_SORT_TYPE = SortType.SORT_DATE
        const val DEFAULT_SORT_ASCENDING = false
        const val NO_OPENPGP_KEY: Long = 0
        const val UNASSIGNED_ACCOUNT_NUMBER = -1
        const val INTERVAL_MINUTES_NEVER = -1
        const val DEFAULT_SYNC_INTERVAL = 60
    }
}
