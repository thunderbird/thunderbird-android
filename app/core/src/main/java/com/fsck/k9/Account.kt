package com.fsck.k9


import android.content.Context
import com.fsck.k9.backend.api.SyncConfig.ExpungePolicy
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.NetworkType
import com.fsck.k9.mail.store.StoreConfig
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.mailstore.StorageManager.StorageProvider
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Account stores all of the settings for a single account defined by the user. It is able to save
 * and delete itself given a Preferences to work with. Each account is defined by a UUID.
 */
open class Account internal constructor(override val uuid: String) : BaseAccount, StoreConfig {
    @get:Synchronized
    @set:Synchronized
    var deletePolicy = DeletePolicy.NEVER

    @get:Synchronized
    @set:Synchronized
    lateinit var storeUri: String

    /**
     * Storage provider ID, used to locate and manage the underlying DB/file
     * storage
     */
    var localStorageProviderId: String? = null
        set(value) {
            if (field != value) {

                var successful = false
                try {
                    switchLocalStorage(value!!)
                    successful = true
                } catch (e: MessagingException) {
                    Timber.e(e, "Switching local storage provider from %s to %s failed.", localStorageProviderId, value)
                }

                // if migration to/from SD-card failed once, it will fail again.
                if (!successful) {
                    return
                }

                field = value
            }
        }

    @get:Synchronized
    @set:Synchronized
    lateinit var transportUri: String

    @get:Synchronized
    @set:Synchronized
    override var description: String? = null

    @get:Synchronized
    @set:Synchronized
    override var email: String
        get() = identities[0].email
        set(value) {
            identities[0].email = value
        }

    @get:Synchronized
    @set:Synchronized
    var alwaysBcc: String? = null

    var automaticCheckIntervalMinutes: Int = 0

    override var displayCount: Int = 0
        set(value) {
            field = if (value != -1) {
                value
            } else {
                K9.DEFAULT_VISIBLE_LIMIT
            }
            resetVisibleLimits()
        }

    @get:Synchronized
    @set:Synchronized
    var chipColor: Int = 0

    @get:Synchronized
    @set:Synchronized
    var latestOldMessageSeenTime: Long = 0

    @get:Synchronized
    @set:Synchronized
    var isNotifyNewMail: Boolean = false

    @get:Synchronized
    @set:Synchronized
    lateinit var folderNotifyNewMailMode: FolderMode

    @get:Synchronized
    @set:Synchronized
    var isNotifySelfNewMail: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var isNotifyContactsMailOnly: Boolean = false

    @get:Synchronized
    @set:Synchronized
    override var inboxFolder: String? = null

    @get:Synchronized
    @set:Synchronized
    override var draftsFolder: String? = null

    @get:Synchronized
    override val outboxFolder = OUTBOX

    @get:Synchronized
    var sentFolder: String? = null
        private set

    @get:Synchronized
    var trashFolder: String? = null
        private set

    @get:Synchronized
    var archiveFolder: String? = null
        private set

    @get:Synchronized
    var spamFolder: String? = null
        private set

    @get:Synchronized
    lateinit var draftsFolderSelection: SpecialFolderSelection
        private set

    @get:Synchronized
    lateinit var sentFolderSelection: SpecialFolderSelection
        private set

    @get:Synchronized
    lateinit var trashFolderSelection: SpecialFolderSelection
        private set

    @get:Synchronized
    lateinit var archiveFolderSelection: SpecialFolderSelection
        private set

    @get:Synchronized
    lateinit var spamFolderSelection: SpecialFolderSelection
        private set

    @get:Synchronized
    @set:Synchronized
    var autoExpandFolder: String? = null

    @get:Synchronized
    @set:Synchronized
    lateinit var folderDisplayMode: FolderMode

    @get:Synchronized
    @set:Synchronized
    lateinit var folderSyncMode: FolderMode

    @get:Synchronized
    @set:Synchronized
    lateinit var folderPushMode: FolderMode

    @get:Synchronized
    @set:Synchronized
    lateinit var folderTargetMode: FolderMode

    @get:Synchronized
    @set:Synchronized
    var accountNumber: Int = 0

    @get:Synchronized
    @set:Synchronized
    override var pushPollOnConnect: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var isShowOngoing: Boolean = false

    @get:Synchronized
    @set:Synchronized
    lateinit var sortType: SortType

    @get:Synchronized
    val sortAscending = HashMap<SortType, Boolean>()

    @get:Synchronized
    @set:Synchronized
    lateinit var showPictures: ShowPictures

    @get:Synchronized
    @set:Synchronized
    var isSignatureBeforeQuotedText: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var expungePolicy = Expunge.EXPUNGE_IMMEDIATELY

    @get:Synchronized
    @set:Synchronized
    var maxPushFolders: Int = 0

    @get:Synchronized
    @set:Synchronized
    override var idleRefreshMinutes: Int = 0

    @get:Synchronized
    @set:Synchronized
    var isGoToUnreadMessageSearch: Boolean = false

    @get:Synchronized
    val compressionMap = ConcurrentHashMap<NetworkType, Boolean>()

    @get:Synchronized
    @set:Synchronized
    lateinit var searchableFolders: Searchable

    @get:Synchronized
    @set:Synchronized
    override var subscribedFoldersOnly: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var maximumPolledMessageAge: Int = 0

    @get:Synchronized
    @set:Synchronized
    override var maximumAutoDownloadMessageSize: Int = 0

    // Tracks if we have sent a notification for this account for
    // current set of fetched messages
    /* Have we sent a new mail notification on this account */
    @get:Synchronized
    @set:Synchronized
    var isRingNotified: Boolean = false

    @get:Synchronized
    @set:Synchronized
    lateinit var messageFormat: MessageFormat

    @get:Synchronized
    @set:Synchronized
    var messageFormatAuto: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var messageReadReceiptAlways: Boolean = false

    @get:Synchronized
    @set:Synchronized
    lateinit var quoteStyle: QuoteStyle

    @get:Synchronized
    @set:Synchronized
    var quotePrefix: String? = null

    @get:Synchronized
    @set:Synchronized
    var isDefaultQuotedTextShown: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var isReplyAfterQuote: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var isStripSignature: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var isSyncRemoteDeletions: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var openPgpProvider: String? = null

    @get:Synchronized
    @set:Synchronized
    var openPgpKey: Long = NO_OPENPGP_KEY

    @get:Synchronized
    @set:Synchronized
    var autocryptPreferEncryptMutual: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var openPgpHideSignOnly: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var openPgpEncryptSubject: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var isMarkMessageAsReadOnView: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var isAlwaysShowCcBcc: Boolean = false

    @get:Synchronized
    @set:Synchronized
    override var allowRemoteSearch: Boolean = false
        get() = false// Temporarily disabled

    @get:Synchronized
    @set:Synchronized
    override var remoteSearchFullText: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var remoteSearchNumResults: Int = 0
        set(value) {
            field = if (value >= 0) value else 0
        }

    @get:Synchronized
    @set:Synchronized
    var isUploadSentMessages: Boolean = false


    /**
     * Indicates whether this account is enabled, i.e. ready for use, or not.
     * Right now newly imported accounts are disabled if the settings file didn't contain a
     * password for the incoming and/or outgoing server.
     */
    @get:Synchronized
    @set:Synchronized
    var isEnabled: Boolean = false

    /**
     * Name of the folder that was last selected for a copy or move operation.
     * Note: For now this value isn't persisted. So it will be reset when K-9 Mail is restarted.
     */
    @get:Synchronized
    @set:Synchronized
    var lastSelectedFolder: String? = null

    @get:Synchronized
    @set:Synchronized
    var identities: List<Identity> = listOf()

    @get:Synchronized
    val notificationSetting = NotificationSetting()

    val displayName: String
        get() = description ?: email

    var name: String
        @Synchronized get() = identities[0].name
        @Synchronized set(name) {
            identities[0].name = name
        }

    var signatureUse: Boolean
        @Synchronized get() = identities[0].signatureUse
        @Synchronized set(signatureUse) {
            identities[0].signatureUse = signatureUse
        }

    var signature: String
        @Synchronized get() = identities[0].signature
        @Synchronized set(signature) {
            identities[0].signature = signature
        }

    val localStore: LocalStore
        @Throws(MessagingException::class)
        get() {
            val context = DI.get(Context::class.java)
            return LocalStore.getInstance(this, context)
        }

    val earliestPollDate: Date?
        get() {
            val age = maximumPolledMessageAge
            if (age >= 0) {
                val now = Calendar.getInstance()
                now.set(Calendar.HOUR_OF_DAY, 0)
                now.set(Calendar.MINUTE, 0)
                now.set(Calendar.SECOND, 0)
                now.set(Calendar.MILLISECOND, 0)
                if (age < 28) {
                    now.add(Calendar.DATE, age * -1)
                } else
                    when (age) {
                        28 -> now.add(Calendar.MONTH, -1)
                        56 -> now.add(Calendar.MONTH, -2)
                        84 -> now.add(Calendar.MONTH, -3)
                        168 -> now.add(Calendar.MONTH, -6)
                        365 -> now.add(Calendar.YEAR, -1)
                    }

                return now.time
            }

            return null
        }

    val isOpenPgpProviderConfigured: Boolean
        get() = !openPgpProvider.isNullOrEmpty()

    enum class Expunge {
        EXPUNGE_IMMEDIATELY,
        EXPUNGE_MANUALLY,
        EXPUNGE_ON_POLL;

        fun toBackendExpungePolicy(): ExpungePolicy {
            return when (this) {
                EXPUNGE_IMMEDIATELY -> ExpungePolicy.IMMEDIATELY
                EXPUNGE_MANUALLY -> ExpungePolicy.MANUALLY
                EXPUNGE_ON_POLL -> ExpungePolicy.ON_POLL
            }
        }
    }

    enum class DeletePolicy constructor(val setting: Int) {
        NEVER(0),
        SEVEN_DAYS(1),
        ON_DELETE(2),
        MARK_AS_READ(3);

        companion object {
            fun fromInt(initialSetting: Int): DeletePolicy {
                for (policy in values()) {
                    if (policy.setting == initialSetting) {
                        return policy
                    }
                }
                throw IllegalArgumentException("DeletePolicy $initialSetting unknown")
            }
        }
    }

    enum class SortType constructor(val isDefaultAscending: Boolean) {
        SORT_DATE(false),
        SORT_ARRIVAL(false),
        SORT_SUBJECT(true),
        SORT_SENDER(true),
        SORT_UNREAD(true),
        SORT_FLAGGED(true),
        SORT_ATTACHMENT(true)
    }

    enum class FolderMode {
        NONE, ALL, FIRST_CLASS, FIRST_AND_SECOND_CLASS, NOT_SECOND_CLASS
    }

    enum class SpecialFolderSelection {
        AUTOMATIC,
        MANUAL
    }

    enum class ShowPictures {
        NEVER, ALWAYS, ONLY_FROM_CONTACTS
    }

    enum class Searchable {
        ALL, DISPLAYABLE, NONE
    }

    enum class QuoteStyle {
        PREFIX, HEADER
    }

    enum class MessageFormat {
        TEXT, HTML, AUTO
    }

    @Deprecated("Use AccountManager directly")
    fun save() {
        DI.get(AccountManager::class.java).save(this)
    }

    private fun resetVisibleLimits() {
        try {
            localStore.resetVisibleLimits(displayCount)
        } catch (e: MessagingException) {
            Timber.e(e, "Unable to reset visible limits")
        }

    }

    fun isSpecialFolder(folderServerId: String?): Boolean {
        return folderServerId != null && (folderServerId == inboxFolder ||
                folderServerId == trashFolder ||
                folderServerId == draftsFolder ||
                folderServerId == archiveFolder ||
                folderServerId == spamFolder ||
                folderServerId == outboxFolder ||
                folderServerId == sentFolder)
    }

    @Synchronized
    fun setDraftsFolder(name: String?, selection: SpecialFolderSelection) {
        draftsFolder = name
        draftsFolderSelection = selection
    }

    @Synchronized
    fun hasDraftsFolder(): Boolean {
        return draftsFolder != null
    }

    @Synchronized
    fun setSentFolder(name: String?, selection: SpecialFolderSelection) {
        sentFolder = name
        sentFolderSelection = selection
    }

    @Synchronized
    fun hasSentFolder(): Boolean {
        return sentFolder != null
    }

    @Synchronized
    fun setTrashFolder(name: String?, selection: SpecialFolderSelection) {
        trashFolder = name
        trashFolderSelection = selection
    }

    @Synchronized
    fun hasTrashFolder(): Boolean {
        return trashFolder != null
    }

    @Synchronized
    fun setArchiveFolder(archiveFolder: String?, selection: SpecialFolderSelection) {
        this.archiveFolder = archiveFolder
        archiveFolderSelection = selection
    }

    @Synchronized
    fun hasArchiveFolder(): Boolean {
        return archiveFolder != null
    }

    @Synchronized
    fun setSpamFolder(name: String?, selection: SpecialFolderSelection) {
        spamFolder = name
        spamFolderSelection = selection
    }

    @Synchronized
    fun hasSpamFolder(): Boolean {
        return spamFolder != null
    }

    @Synchronized
    fun isSortAscending(sortType: SortType): Boolean {
        return sortAscending[sortType] ?: sortType.isDefaultAscending
    }

    @Synchronized
    fun setSortAscending(sortType: SortType, sortAscending: Boolean) {
        this.sortAscending[sortType] = sortAscending
    }

    @Synchronized
    override fun toString(): String {
        return description ?: ""
    }

    @Synchronized
    fun setCompression(networkType: NetworkType, useCompression: Boolean) {
        compressionMap[networkType] = useCompression
    }

    @Synchronized
    override fun useCompression(networkType: NetworkType): Boolean {
        return compressionMap[networkType] ?: return true
    }

    override fun equals(other: Any?): Boolean {
        return (other as? Account)?.let { it.uuid == uuid } ?: super.equals(other)
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    @Synchronized
    fun getIdentity(i: Int): Identity {
        if (i < identities.size) {
            return identities[i]
        }
        throw IllegalArgumentException("Identity with index $i not found")
    }

    fun isAnIdentity(addrs: Array<Address>?): Boolean {
        if (addrs == null) {
            return false
        }
        for (addr in addrs) {
            if (findIdentity(addr) != null) {
                return true
            }
        }

        return false
    }

    fun isAnIdentity(addr: Address): Boolean {
        return findIdentity(addr) != null
    }

    @Synchronized
    fun findIdentity(addr: Address): Identity? {
        for (identity in identities) {
            val email = identity.email
            if (email != null && email.equals(addr.address, ignoreCase = true)) {
                return identity
            }
        }
        return null
    }

    override fun shouldHideHostname(): Boolean {
        return K9.hideHostnameWhenConnecting()
    }

    /**
     * Are we storing out localStore on the SD-card instead of the local device
     * memory?<br></br>
     * Only to be called during initial account-setup!<br></br>
     * Side-effect: changes [.localStorageProviderId].
     *
     * @param newStorageProviderId
     * Never `null`.
     * @throws MessagingException
     */
    @Throws(MessagingException::class)
    private fun switchLocalStorage(newStorageProviderId: String) {
        if (localStorageProviderId != newStorageProviderId) {
            localStore.switchLocalStorage(newStorageProviderId)
        }
    }

    /**
     * @return `true` if our [StorageProvider] is ready. (e.g.
     * card inserted)
     */
    fun isAvailable(context: Context): Boolean {
        val storageProviderIsInternalMemory = localStorageProviderId == null
        return storageProviderIsInternalMemory || StorageManager.getInstance(context).isReady(localStorageProviderId)
    }

    companion object {
        // Default value for the inbox folder (never changes for POP3 and IMAP)
        const val INBOX = "INBOX"

        // This local folder is used to store messages to be sent.
        const val OUTBOX = "K9MAIL_INTERNAL_OUTBOX"

        const val FALLBACK_ACCOUNT_COLOR = 0x0099CC

        val DEFAULT_MESSAGE_FORMAT = MessageFormat.HTML
        const val DEFAULT_MESSAGE_FORMAT_AUTO = false
        const val DEFAULT_MESSAGE_READ_RECEIPT = false
        val DEFAULT_QUOTE_STYLE = QuoteStyle.PREFIX
        const val DEFAULT_QUOTE_PREFIX = ">"
        const val DEFAULT_QUOTED_TEXT_SHOWN = true
        const val DEFAULT_REPLY_AFTER_QUOTE = false
        const val DEFAULT_STRIP_SIGNATURE = true
        const val DEFAULT_REMOTE_SEARCH_NUM_RESULTS = 25

        const val ACCOUNT_DESCRIPTION_KEY = "description"
        const val STORE_URI_KEY = "storeUri"
        const val TRANSPORT_URI_KEY = "transportUri"

        const val IDENTITY_NAME_KEY = "name"
        const val IDENTITY_EMAIL_KEY = "email"
        const val IDENTITY_DESCRIPTION_KEY = "description"

        val DEFAULT_SORT_TYPE = SortType.SORT_DATE
        const val DEFAULT_SORT_ASCENDING = false
        const val NO_OPENPGP_KEY: Long = 0
    }

}
