package com.fsck.k9


import android.content.Context
import com.fsck.k9.Preferences.getEnumStringPref
import com.fsck.k9.backend.api.SyncConfig.ExpungePolicy
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.NetworkType
import com.fsck.k9.mail.filter.Base64
import com.fsck.k9.mail.store.StoreConfig
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.mailstore.StorageManager.StorageProvider
import com.fsck.k9.preferences.Storage
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Account stores all of the settings for a single account defined by the user. It is able to save
 * and delete itself given a Preferences to work with. Each account is defined by a UUID.
 */
open class Account : BaseAccount, StoreConfig {
    override val uuid: String
        get() = accountUuid

    @get:Synchronized
    @set:Synchronized
    var deletePolicy = DeletePolicy.NEVER

    private val accountUuid: String
    @get:Synchronized
    @set:Synchronized
    lateinit var storeUri: String

    /**
     * Storage provider ID, used to locate and manage the underlying DB/file
     * storage
     */
    private var localStorageProviderId: String? = null
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

    private var automaticCheckIntervalMinutes: Int = 0

    private var displayCount: Int = 0

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

    private var inboxFolder: String? = null

    private var draftsFolder: String? = null

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

    lateinit var folderDisplayMode: FolderMode

    lateinit var folderSyncMode: FolderMode

    lateinit var folderPushMode: FolderMode

    @get:Synchronized
    @set:Synchronized
    lateinit var folderTargetMode: FolderMode

    @get:Synchronized
    @set:Synchronized
    var accountNumber: Int = 0

    private var pushPollOnConnect: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var isShowOngoing: Boolean = false

    @get:Synchronized
    @set:Synchronized
    lateinit var sortType: SortType

    private val sortAscending = HashMap<SortType, Boolean>()

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

    private var idleRefreshMinutes: Int = 0

    @get:Synchronized
    @set:Synchronized
    var isGoToUnreadMessageSearch: Boolean = false

    val compressionMap = ConcurrentHashMap<NetworkType, Boolean>()

    @get:Synchronized
    @set:Synchronized
    lateinit var searchableFolders: Searchable

    private var subscribedFoldersOnly: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var maximumPolledMessageAge: Int = 0

    private var maximumAutoDownloadMessageSize: Int = 0
    // Tracks if we have sent a notification for this account for
    // current set of fetched messages
    /* Have we sent a new mail notification on this account */
    var isRingNotified: Boolean = false

    lateinit var messageFormat: MessageFormat
    private var messageFormatAuto: Boolean = false
    @get:Synchronized
    var isMessageReadReceiptAlways: Boolean = false
        private set

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

    var openPgpKey: Long = 0

    var autocryptPreferEncryptMutual: Boolean = false

    var openPgpHideSignOnly: Boolean = false

    var openPgpEncryptSubject: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var isMarkMessageAsReadOnView: Boolean = false

    @get:Synchronized
    @set:Synchronized
    var isAlwaysShowCcBcc: Boolean = false

    private var allowRemoteSearch: Boolean = false

    private var remoteSearchFullText: Boolean = false

    var remoteSearchNumResults: Int = 0
        set(value) {
            field = if (value >= 0) value else 0
        }

    var isUploadSentMessages: Boolean = false


    /**
     * Indicates whether this account is enabled, i.e. ready for use, or not.
     *
     *
     *
     * Right now newly imported accounts are disabled if the settings file didn't contain a
     * password for the incoming and/or outgoing server.
     *
     */
    @get:Synchronized
    @set:Synchronized
    var isEnabled: Boolean = false

    /**
     * Name of the folder that was last selected for a copy or move operation.
     *
     * Note: For now this value isn't persisted. So it will be reset when
     * K-9 Mail is restarted.
     */
    @get:Synchronized
    @set:Synchronized
    var lastSelectedFolder: String? = null

    private lateinit var identities: MutableList<Identity>

    @get:Synchronized
    var notificationSetting = NotificationSetting()

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

        fun preferenceString(): String {
            return Integer.toString(setting)
        }

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

    constructor(context: Context, resourceProvider: CoreResourceProvider) {
        accountUuid = UUID.randomUUID().toString()
        localStorageProviderId = StorageManager.getInstance(context).defaultProviderId
        automaticCheckIntervalMinutes = -1
        idleRefreshMinutes = 24
        pushPollOnConnect = true
        displayCount = K9.DEFAULT_VISIBLE_LIMIT
        accountNumber = -1
        isNotifyNewMail = true
        folderNotifyNewMailMode = FolderMode.ALL
        isShowOngoing = true
        isNotifySelfNewMail = true
        isNotifyContactsMailOnly = false
        folderDisplayMode = FolderMode.NOT_SECOND_CLASS
        folderSyncMode = FolderMode.FIRST_CLASS
        folderPushMode = FolderMode.FIRST_CLASS
        folderTargetMode = FolderMode.NOT_SECOND_CLASS
        sortType = DEFAULT_SORT_TYPE
        sortAscending[DEFAULT_SORT_TYPE] = DEFAULT_SORT_ASCENDING
        showPictures = ShowPictures.NEVER
        isSignatureBeforeQuotedText = false
        expungePolicy = Expunge.EXPUNGE_IMMEDIATELY
        autoExpandFolder = INBOX
        inboxFolder = INBOX
        maxPushFolders = 10
        isGoToUnreadMessageSearch = false
        subscribedFoldersOnly = false
        maximumPolledMessageAge = -1
        maximumAutoDownloadMessageSize = 32768
        messageFormat = DEFAULT_MESSAGE_FORMAT
        messageFormatAuto = DEFAULT_MESSAGE_FORMAT_AUTO
        isMessageReadReceiptAlways = DEFAULT_MESSAGE_READ_RECEIPT
        quoteStyle = DEFAULT_QUOTE_STYLE
        quotePrefix = DEFAULT_QUOTE_PREFIX
        isDefaultQuotedTextShown = DEFAULT_QUOTED_TEXT_SHOWN
        isReplyAfterQuote = DEFAULT_REPLY_AFTER_QUOTE
        isStripSignature = DEFAULT_STRIP_SIGNATURE
        isSyncRemoteDeletions = true
        openPgpKey = NO_OPENPGP_KEY
        allowRemoteSearch = false
        remoteSearchFullText = false
        remoteSearchNumResults = DEFAULT_REMOTE_SEARCH_NUM_RESULTS
        isUploadSentMessages = true
        isEnabled = true
        isMarkMessageAsReadOnView = true
        isAlwaysShowCcBcc = false
        archiveFolder = null
        draftsFolder = null
        sentFolder = null
        spamFolder = null
        trashFolder = null
        archiveFolderSelection = SpecialFolderSelection.AUTOMATIC
        draftsFolderSelection = SpecialFolderSelection.AUTOMATIC
        sentFolderSelection = SpecialFolderSelection.AUTOMATIC
        spamFolderSelection = SpecialFolderSelection.AUTOMATIC
        trashFolderSelection = SpecialFolderSelection.AUTOMATIC

        searchableFolders = Searchable.ALL

        identities = ArrayList()

        val identity = Identity()
        identity.signatureUse = true
        identity.signature = resourceProvider.defaultSignature()
        identity.description = resourceProvider.defaultIdentityDescription()
        identities.add(identity)

        notificationSetting = NotificationSetting()
        notificationSetting.setVibrate(false)
        notificationSetting.vibratePattern = 0
        notificationSetting.vibrateTimes = 5
        notificationSetting.isRingEnabled = true
        notificationSetting.ringtone = "content://settings/system/notification_sound"
        notificationSetting.ledColor = chipColor
    }

    constructor(preferences: Preferences, uuid: String) {
        this.accountUuid = uuid
        loadAccount(preferences)
    }

    /**
     * Load stored settings for this account.
     */
    @Synchronized
    private fun loadAccount(preferences: Preferences) {

        val storage = preferences.storage
        val storageManager = DI.get(StorageManager::class.java)

        storeUri = Base64.decode(storage.getString("$accountUuid.storeUri", null))
        localStorageProviderId = storage.getString(
                "$accountUuid.localStorageProvider", storageManager.defaultProviderId)
        transportUri = Base64.decode(storage.getString("$accountUuid.transportUri", null))
        description = storage.getString("$accountUuid.description", null)
        alwaysBcc = storage.getString("$accountUuid.alwaysBcc", alwaysBcc)
        automaticCheckIntervalMinutes = storage.getInt("$accountUuid.automaticCheckIntervalMinutes", -1)
        idleRefreshMinutes = storage.getInt("$accountUuid.idleRefreshMinutes", 24)
        pushPollOnConnect = storage.getBoolean("$accountUuid.pushPollOnConnect", true)
        displayCount = storage.getInt("$accountUuid.displayCount", K9.DEFAULT_VISIBLE_LIMIT)
        if (displayCount < 0) {
            displayCount = K9.DEFAULT_VISIBLE_LIMIT
        }
        latestOldMessageSeenTime = storage.getLong("$accountUuid.latestOldMessageSeenTime", 0)
        isNotifyNewMail = storage.getBoolean("$accountUuid.notifyNewMail", false)

        folderNotifyNewMailMode = getEnumStringPref(storage, "$accountUuid.folderNotifyNewMailMode", FolderMode.ALL)
        isNotifySelfNewMail = storage.getBoolean("$accountUuid.notifySelfNewMail", true)
        isNotifyContactsMailOnly = storage.getBoolean("$accountUuid.notifyContactsMailOnly", false)
        isShowOngoing = storage.getBoolean("$accountUuid.notifyMailCheck", false)
        deletePolicy = DeletePolicy.fromInt(storage.getInt("$accountUuid.deletePolicy", DeletePolicy.NEVER.setting))
        inboxFolder = storage.getString("$accountUuid.inboxFolderName", INBOX)
        draftsFolder = storage.getString("$accountUuid.draftsFolderName", null)
        sentFolder = storage.getString("$accountUuid.sentFolderName", null)
        trashFolder = storage.getString("$accountUuid.trashFolderName", null)
        archiveFolder = storage.getString("$accountUuid.archiveFolderName", null)
        spamFolder = storage.getString("$accountUuid.spamFolderName", null)
        archiveFolderSelection = getEnumStringPref(storage, "$accountUuid.archiveFolderSelection",
                SpecialFolderSelection.AUTOMATIC)
        draftsFolderSelection = getEnumStringPref(storage, "$accountUuid.draftsFolderSelection",
                SpecialFolderSelection.AUTOMATIC)
        sentFolderSelection = getEnumStringPref(storage, "$accountUuid.sentFolderSelection",
                SpecialFolderSelection.AUTOMATIC)
        spamFolderSelection = getEnumStringPref(storage, "$accountUuid.spamFolderSelection",
                SpecialFolderSelection.AUTOMATIC)
        trashFolderSelection = getEnumStringPref(storage, "$accountUuid.trashFolderSelection",
                SpecialFolderSelection.AUTOMATIC)

        expungePolicy = getEnumStringPref(storage, "$accountUuid.expungePolicy", Expunge.EXPUNGE_IMMEDIATELY)
        isSyncRemoteDeletions = storage.getBoolean("$accountUuid.syncRemoteDeletions", true)

        maxPushFolders = storage.getInt("$accountUuid.maxPushFolders", 10)
        isGoToUnreadMessageSearch = storage.getBoolean("$accountUuid.goToUnreadMessageSearch", false)
        subscribedFoldersOnly = storage.getBoolean("$accountUuid.subscribedFoldersOnly", false)
        maximumPolledMessageAge = storage.getInt("$accountUuid.maximumPolledMessageAge", -1)
        maximumAutoDownloadMessageSize = storage.getInt("$accountUuid.maximumAutoDownloadMessageSize", 32768)
        messageFormat = getEnumStringPref(storage, "$accountUuid.messageFormat", DEFAULT_MESSAGE_FORMAT)
        messageFormatAuto = storage.getBoolean("$accountUuid.messageFormatAuto", DEFAULT_MESSAGE_FORMAT_AUTO)
        if (messageFormatAuto && messageFormat == MessageFormat.TEXT) {
            messageFormat = MessageFormat.AUTO
        }
        isMessageReadReceiptAlways = storage.getBoolean("$accountUuid.messageReadReceipt", DEFAULT_MESSAGE_READ_RECEIPT)
        quoteStyle = getEnumStringPref(storage, "$accountUuid.quoteStyle", DEFAULT_QUOTE_STYLE)
        quotePrefix = storage.getString("$accountUuid.quotePrefix", DEFAULT_QUOTE_PREFIX)
        isDefaultQuotedTextShown = storage.getBoolean("$accountUuid.defaultQuotedTextShown", DEFAULT_QUOTED_TEXT_SHOWN)
        isReplyAfterQuote = storage.getBoolean("$accountUuid.replyAfterQuote", DEFAULT_REPLY_AFTER_QUOTE)
        isStripSignature = storage.getBoolean("$accountUuid.stripSignature", DEFAULT_STRIP_SIGNATURE)
        for (type in NetworkType.values()) {
            val useCompression = storage.getBoolean("$accountUuid.useCompression.$type",
                    true)
            compressionMap[type] = useCompression
        }

        autoExpandFolder = storage.getString("$accountUuid.autoExpandFolderName", INBOX)

        accountNumber = storage.getInt("$accountUuid.accountNumber", 0)

        chipColor = storage.getInt("$accountUuid.chipColor", FALLBACK_ACCOUNT_COLOR)

        sortType = getEnumStringPref(storage, "$accountUuid.sortTypeEnum", SortType.SORT_DATE)

        sortAscending[sortType] = storage.getBoolean("$accountUuid.sortAscending", false)

        showPictures = getEnumStringPref(storage, "$accountUuid.showPicturesEnum", ShowPictures.NEVER)

        notificationSetting.setVibrate(storage.getBoolean("$accountUuid.vibrate", false))
        notificationSetting.vibratePattern = storage.getInt("$accountUuid.vibratePattern", 0)
        notificationSetting.vibrateTimes = storage.getInt("$accountUuid.vibrateTimes", 5)
        notificationSetting.isRingEnabled = storage.getBoolean("$accountUuid.ring", true)
        notificationSetting.ringtone = storage.getString("$accountUuid.ringtone",
                "content://settings/system/notification_sound")
        notificationSetting.setLed(storage.getBoolean("$accountUuid.led", true))
        notificationSetting.ledColor = storage.getInt("$accountUuid.ledColor", chipColor)

        folderDisplayMode = getEnumStringPref(storage, "$accountUuid.folderDisplayMode", FolderMode.NOT_SECOND_CLASS)

        folderSyncMode = getEnumStringPref(storage, "$accountUuid.folderSyncMode", FolderMode.FIRST_CLASS)

        folderPushMode = getEnumStringPref(storage, "$accountUuid.folderPushMode", FolderMode.FIRST_CLASS)

        folderTargetMode = getEnumStringPref(storage, "$accountUuid.folderTargetMode", FolderMode.NOT_SECOND_CLASS)

        searchableFolders = getEnumStringPref(storage, "$accountUuid.searchableFolders", Searchable.ALL)

        isSignatureBeforeQuotedText = storage.getBoolean("$accountUuid.signatureBeforeQuotedText", false)
        identities = loadIdentities(storage)

        openPgpProvider = storage.getString("$accountUuid.openPgpProvider", "")
        openPgpKey = storage.getLong("$accountUuid.cryptoKey", NO_OPENPGP_KEY)
        openPgpHideSignOnly = storage.getBoolean("$accountUuid.openPgpHideSignOnly", true)
        openPgpEncryptSubject = storage.getBoolean("$accountUuid.openPgpEncryptSubject", true)
        autocryptPreferEncryptMutual = storage.getBoolean("$accountUuid.autocryptMutualMode", false)
        allowRemoteSearch = storage.getBoolean("$accountUuid.allowRemoteSearch", false)
        remoteSearchFullText = storage.getBoolean("$accountUuid.remoteSearchFullText", false)
        remoteSearchNumResults = storage.getInt("$accountUuid.remoteSearchNumResults", DEFAULT_REMOTE_SEARCH_NUM_RESULTS)
        isUploadSentMessages = storage.getBoolean("$accountUuid.uploadSentMessages", true)

        isEnabled = storage.getBoolean("$accountUuid.enabled", true)
        isMarkMessageAsReadOnView = storage.getBoolean("$accountUuid.markMessageAsReadOnView", true)
        isAlwaysShowCcBcc = storage.getBoolean("$accountUuid.alwaysShowCcBcc", false)

        // Use email address as account description if necessary
        if (description == null) {
            description = email
        }
    }

    fun move(preferences: Preferences, moveUp: Boolean) {
        val uuids = preferences.storage.getString("accountUuids", "").split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val editor = preferences.storage.edit()
        val newUuids = arrayOfNulls<String>(uuids.size)
        if (moveUp) {
            for (i in uuids.indices) {
                if (i > 0 && uuids[i] == accountUuid) {
                    newUuids[i] = newUuids[i - 1]
                    newUuids[i - 1] = accountUuid
                } else {
                    newUuids[i] = uuids[i]
                }
            }
        } else {
            for (i in uuids.indices.reversed()) {
                if (i < uuids.size - 1 && uuids[i] == accountUuid) {
                    newUuids[i] = newUuids[i + 1]
                    newUuids[i + 1] = accountUuid
                } else {
                    newUuids[i] = uuids[i]
                }
            }
        }
        val accountUuids = Utility.combine(newUuids, ',')
        editor.putString("accountUuids", accountUuids)
        editor.commit()
        preferences.loadAccounts()
    }

    @Synchronized
    fun save() {
        DI.get(AccountManager::class.java).save(this)
    }

    private fun resetVisibleLimits() {
        try {
            localStore.resetVisibleLimits(getDisplayCount())
        } catch (e: MessagingException) {
            Timber.e(e, "Unable to reset visible limits")
        }

    }

    fun getLocalStorageProviderId(): String? {
        return localStorageProviderId
    }

    fun setLocalStorageProviderId(id: String) {

        if (localStorageProviderId != id) {

            var successful = false
            try {
                switchLocalStorage(id)
                successful = true
            } catch (e: MessagingException) {
                Timber.e(e, "Switching local storage provider from %s to %s failed.", localStorageProviderId, id)
            }

            // if migration to/from SD-card failed once, it will fail again.
            if (!successful) {
                return
            }

            localStorageProviderId = id
        }

    }

    /**
     * Returns -1 for never.
     */
    @Synchronized
    fun getAutomaticCheckIntervalMinutes(): Int {
        return automaticCheckIntervalMinutes
    }

    /**
     * @param automaticCheckIntervalMinutes or -1 for never.
     */
    @Synchronized
    fun setAutomaticCheckIntervalMinutes(automaticCheckIntervalMinutes: Int): Boolean {
        val oldInterval = this.automaticCheckIntervalMinutes
        this.automaticCheckIntervalMinutes = automaticCheckIntervalMinutes

        return oldInterval != automaticCheckIntervalMinutes
    }

    @Synchronized
    override fun getDisplayCount(): Int {
        return displayCount
    }

    @Synchronized
    fun setDisplayCount(displayCount: Int) {
        if (displayCount != -1) {
            this.displayCount = displayCount
        } else {
            this.displayCount = K9.DEFAULT_VISIBLE_LIMIT
        }
        resetVisibleLimits()
    }

    fun isSpecialFolder(folderServerId: String?): Boolean {
        return folderServerId != null && (folderServerId == getInboxFolder() ||
                folderServerId == trashFolder ||
                folderServerId == getDraftsFolder() ||
                folderServerId == archiveFolder ||
                folderServerId == spamFolder ||
                folderServerId == outboxFolder ||
                folderServerId == sentFolder)
    }

    @Synchronized
    override fun getDraftsFolder(): String? {
        return draftsFolder
    }

    @Synchronized
    fun setDraftsFolder(name: String?, selection: SpecialFolderSelection) {
        draftsFolder = name
        draftsFolderSelection = selection
    }

    /**
     * Checks if this account has a drafts folder set.
     * @return true if account has a drafts folder set.
     */
    @Synchronized
    fun hasDraftsFolder(): Boolean {
        return draftsFolder != null
    }

    @Synchronized
    fun setSentFolder(name: String?, selection: SpecialFolderSelection) {
        sentFolder = name
        sentFolderSelection = selection
    }

    /**
     * Checks if this account has a sent folder set.
     * @return true if account has a sent folder set.
     */
    @Synchronized
    fun hasSentFolder(): Boolean {
        return sentFolder != null
    }

    @Synchronized
    fun setTrashFolder(name: String?, selection: SpecialFolderSelection) {
        trashFolder = name
        trashFolderSelection = selection
    }

    /**
     * Checks if this account has a trash folder set.
     * @return true if account has a trash folder set.
     */
    @Synchronized
    fun hasTrashFolder(): Boolean {
        return trashFolder != null
    }

    @Synchronized
    fun setArchiveFolder(archiveFolder: String?, selection: SpecialFolderSelection) {
        this.archiveFolder = archiveFolder
        archiveFolderSelection = selection
    }

    /**
     * Checks if this account has an archive folder set.
     * @return true if account has an archive folder set.
     */
    @Synchronized
    fun hasArchiveFolder(): Boolean {
        return archiveFolder != null
    }

    @Synchronized
    fun setSpamFolder(name: String?, selection: SpecialFolderSelection) {
        spamFolder = name
        spamFolderSelection = selection
    }

    /**
     * Checks if this account has a spam folder set.
     * @return true if account has a spam folder set.
     */
    @Synchronized
    fun hasSpamFolder(): Boolean {
        return spamFolder != null
    }

    override fun getOutboxFolder(): String {
        return OUTBOX
    }

    @Synchronized
    fun setFolderDisplayMode(displayMode: FolderMode): Boolean {
        val oldDisplayMode = folderDisplayMode
        folderDisplayMode = displayMode
        return oldDisplayMode != displayMode
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
        return if (other is Account) {
            other.accountUuid == accountUuid
        } else super.equals(other)
    }

    override fun hashCode(): Int {
        return accountUuid.hashCode()
    }

    @Synchronized
    private fun loadIdentities(storage: Storage): MutableList<Identity> {
        val newIdentities = ArrayList<Identity>()
        var ident = 0
        var gotOne: Boolean
        do {
            gotOne = false
            val name = storage.getString("$accountUuid.$IDENTITY_NAME_KEY.$ident", null)
            val email = storage.getString("$accountUuid.$IDENTITY_EMAIL_KEY.$ident", null)
            val signatureUse = storage.getBoolean("$accountUuid.signatureUse.$ident", true)
            val signature = storage.getString("$accountUuid.signature.$ident", null)
            val description = storage.getString("$accountUuid.$IDENTITY_DESCRIPTION_KEY.$ident", null)
            val replyTo = storage.getString("$accountUuid.replyTo.$ident", null)
            if (email != null) {
                val identity = Identity()
                identity.name = name
                identity.email = email
                identity.signatureUse = signatureUse
                identity.signature = signature
                identity.description = description
                identity.replyTo = replyTo
                newIdentities.add(identity)
                gotOne = true
            }
            ident++
        } while (gotOne)

        if (newIdentities.isEmpty()) {
            val name = storage.getString("$accountUuid.name", null)
            val email = storage.getString("$accountUuid.email", null)
            val signatureUse = storage.getBoolean("$accountUuid.signatureUse", true)
            val signature = storage.getString("$accountUuid.signature", null)
            val identity = Identity()
            identity.name = name
            identity.email = email
            identity.signatureUse = signatureUse
            identity.signature = signature
            identity.description = email
            newIdentities.add(identity)
        }

        return newIdentities
    }

    @Synchronized
    fun getIdentities(): List<Identity> {
        return Collections.unmodifiableList(identities)
    }

    @Synchronized
    fun setIdentities(newIdentities: List<Identity>) {
        identities = ArrayList(newIdentities)
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

    @Synchronized
    override fun getIdleRefreshMinutes(): Int {
        return idleRefreshMinutes
    }

    override fun shouldHideHostname(): Boolean {
        return K9.hideHostnameWhenConnecting()
    }

    @Synchronized
    fun setIdleRefreshMinutes(idleRefreshMinutes: Int) {
        this.idleRefreshMinutes = idleRefreshMinutes
    }

    @Synchronized
    override fun isPushPollOnConnect(): Boolean {
        return pushPollOnConnect
    }

    @Synchronized
    fun setPushPollOnConnect(pushPollOnConnect: Boolean) {
        this.pushPollOnConnect = pushPollOnConnect
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

    @Synchronized
    override fun isSubscribedFoldersOnly(): Boolean {
        return subscribedFoldersOnly
    }

    @Synchronized
    fun setSubscribedFoldersOnly(subscribedFoldersOnly: Boolean) {
        this.subscribedFoldersOnly = subscribedFoldersOnly
    }

    @Synchronized
    override fun getMaximumAutoDownloadMessageSize(): Int {
        return maximumAutoDownloadMessageSize
    }

    @Synchronized
    fun setMaximumAutoDownloadMessageSize(maximumAutoDownloadMessageSize: Int) {
        this.maximumAutoDownloadMessageSize = maximumAutoDownloadMessageSize
    }

    @Synchronized
    fun setMessageReadReceipt(messageReadReceipt: Boolean) {
        this.isMessageReadReceiptAlways = messageReadReceipt
    }

    fun hasOpenPgpKey(): Boolean {
        return openPgpKey != NO_OPENPGP_KEY
    }

    override fun isAllowRemoteSearch(): Boolean {
        return allowRemoteSearch
    }

    fun setAllowRemoteSearch(`val`: Boolean) {
        allowRemoteSearch = `val`
    }

    override fun getInboxFolder(): String? {
        return inboxFolder
    }

    fun setInboxFolder(name: String?) {
        this.inboxFolder = name
    }

    /**
     * @return `true` if our [StorageProvider] is ready. (e.g.
     * card inserted)
     */
    fun isAvailable(context: Context): Boolean {
        val localStorageProviderId = getLocalStorageProviderId()
        val storageProviderIsInternalMemory = localStorageProviderId == null
        return storageProviderIsInternalMemory || StorageManager.getInstance(context).isReady(localStorageProviderId)
    }

    override fun isRemoteSearchFullText(): Boolean {
        return false   // Temporarily disabled
        //return remoteSearchFullText;
    }

    fun setRemoteSearchFullText(`val`: Boolean) {
        remoteSearchFullText = `val`
    }

    companion object {
        /**
         * Default value for the inbox folder (never changes for POP3 and IMAP)
         */
        private val INBOX = "INBOX"

        /**
         * This local folder is used to store messages to be sent.
         */
        const val OUTBOX = "K9MAIL_INTERNAL_OUTBOX"

        private val FALLBACK_ACCOUNT_COLOR = 0x0099CC

        val DEFAULT_MESSAGE_FORMAT = MessageFormat.HTML
        val DEFAULT_MESSAGE_FORMAT_AUTO = false
        val DEFAULT_MESSAGE_READ_RECEIPT = false
        val DEFAULT_QUOTE_STYLE = QuoteStyle.PREFIX
        val DEFAULT_QUOTE_PREFIX = ">"
        val DEFAULT_QUOTED_TEXT_SHOWN = true
        val DEFAULT_REPLY_AFTER_QUOTE = false
        val DEFAULT_STRIP_SIGNATURE = true
        val DEFAULT_REMOTE_SEARCH_NUM_RESULTS = 25

        val ACCOUNT_DESCRIPTION_KEY = "description"
        val STORE_URI_KEY = "storeUri"
        val TRANSPORT_URI_KEY = "transportUri"

        val IDENTITY_NAME_KEY = "name"
        val IDENTITY_EMAIL_KEY = "email"
        val IDENTITY_DESCRIPTION_KEY = "description"

        val DEFAULT_SORT_TYPE = SortType.SORT_DATE
        val DEFAULT_SORT_ASCENDING = false
        val NO_OPENPGP_KEY: Long = 0

        private fun findNewAccountNumber(accountNumbers: List<Int>): Int {
            var newAccountNumber = -1
            for (accountNumber in accountNumbers) {
                if (accountNumber > newAccountNumber + 1) {
                    break
                }
                newAccountNumber = accountNumber
            }
            newAccountNumber++
            return newAccountNumber
        }

        private fun getExistingAccountNumbers(preferences: Preferences): List<Int> {
            val accounts = preferences.accounts
            val accountNumbers = ArrayList<Int>(accounts.size)
            for (a in accounts) {
                accountNumbers.add(a.accountNumber)
            }
            return accountNumbers.sorted().toList()
        }

        fun generateAccountNumber(preferences: Preferences): Int {
            val accountNumbers = getExistingAccountNumbers(preferences)
            return findNewAccountNumber(accountNumbers)
        }
    }

}
