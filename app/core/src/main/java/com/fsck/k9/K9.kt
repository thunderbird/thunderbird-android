package com.fsck.k9

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import com.fsck.k9.Account.SortType
import com.fsck.k9.core.BuildConfig
import com.fsck.k9.mail.K9MailLib
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.preferences.StorageEditor
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import timber.log.Timber.DebugTree

object K9 : KoinComponent {
    private val preferences: Preferences by inject()


    /**
     * If this is enabled, various development settings will be enabled
     * It should NEVER be on for Market builds
     * Right now, it just governs strictmode
     */
    @JvmField
    val DEVELOPER_MODE = BuildConfig.DEBUG

    /**
     * If this is enabled there will be additional logging information sent to Log.d, including protocol dumps.
     * Controlled by Preferences at run-time
     */
    private var DEBUG = false

    /**
     * If this is enabled than logging that normally hides sensitive information like passwords will show that
     * information.
     */
    @JvmField
    var DEBUG_SENSITIVE = false


    private const val VERSION_MIGRATE_OPENPGP_TO_ACCOUNTS = 63

    /**
     * Name of the [SharedPreferences] file used to store the last known version of the
     * accounts' databases.
     *
     * See `UpgradeDatabases` for a detailed explanation of the database upgrade process.
     */
    private const val DATABASE_VERSION_CACHE = "database_version_cache"

    /**
     * Key used to store the last known database version of the accounts' databases.
     *
     * @see DATABASE_VERSION_CACHE
     */
    private const val KEY_LAST_ACCOUNT_DATABASE_VERSION = "last_account_database_version"

    /**
     * A reference to the [SharedPreferences] used for caching the last known database version.
     *
     * @see checkCachedDatabaseVersion
     * @see setDatabasesUpToDate
     */
    private var databaseVersionCache: SharedPreferences? = null

    /**
     * @see areDatabasesUpToDate
     */
    private var databasesUpToDate = false


    /**
     * Check if we already know whether all databases are using the current database schema.
     *
     * This method is only used for optimizations. If it returns `true` we can be certain that getting a [LocalStore]
     * instance won't trigger a schema upgrade.
     *
     * @return `true`, if we know that all databases are using the current database schema. `false`, otherwise.
     */
    @Synchronized
    @JvmStatic
    fun areDatabasesUpToDate(): Boolean {
        return databasesUpToDate
    }

    /**
     * Remember that all account databases are using the most recent database schema.
     *
     * @param save
     * Whether or not to write the current database version to the
     * `SharedPreferences` [.DATABASE_VERSION_CACHE].
     *
     * @see .areDatabasesUpToDate
     */
    @Synchronized
    @JvmStatic
    fun setDatabasesUpToDate(save: Boolean) {
        databasesUpToDate = true

        if (save) {
            val editor = databaseVersionCache!!.edit()
            editor.putInt(KEY_LAST_ACCOUNT_DATABASE_VERSION, LocalStore.getDbVersion())
            editor.apply()
        }
    }

    /**
     * Loads the last known database version of the accounts' databases from a `SharedPreference`.
     *
     * If the stored version matches [LocalStore.getDbVersion] we know that the databases are up to date.
     * Using `SharedPreferences` should be a lot faster than opening all SQLite databases to get the current database
     * version.
     *
     * See the class `UpgradeDatabases` for a detailed explanation of the database upgrade process.
     *
     * @see areDatabasesUpToDate
     */
    private fun checkCachedDatabaseVersion(context: Context) {
        databaseVersionCache = context.getSharedPreferences(DATABASE_VERSION_CACHE, Context.MODE_PRIVATE)

        val cachedVersion = databaseVersionCache!!.getInt(KEY_LAST_ACCOUNT_DATABASE_VERSION, 0)

        if (cachedVersion >= LocalStore.getDbVersion()) {
            setDatabasesUpToDate(false)
        }

        if (cachedVersion < VERSION_MIGRATE_OPENPGP_TO_ACCOUNTS) {
            migrateOpenPgpGlobalToAccountSettings()
        }
    }

    private fun migrateOpenPgpGlobalToAccountSettings() {
        val storage = preferences.storage

        val openPgpProvider = storage.getString("openPgpProvider", null)
        val openPgpSupportSignOnly = storage.getBoolean("openPgpSupportSignOnly", false)

        for (account in preferences.accounts) {
            account.openPgpProvider = openPgpProvider
            account.isOpenPgpHideSignOnly = !openPgpSupportSignOnly
            preferences.saveAccount(account)
        }

        preferences.createStorageEditor()
                .remove("openPgpProvider")
                .remove("openPgpSupportSignOnly")
                .commit()
    }


    @JvmStatic
    var k9Language = ""

    @JvmStatic
    var k9Theme = Theme.LIGHT
        set(theme) {
            if (theme != Theme.USE_GLOBAL) {
                field = theme
            }
        }

    @JvmStatic
    var k9MessageViewThemeSetting = Theme.USE_GLOBAL

    @JvmStatic
    var k9ComposerThemeSetting = Theme.USE_GLOBAL

    @JvmStatic
    var isFixedMessageViewTheme = true
        set(theme) {
            field = theme
            if (!theme && k9MessageViewThemeSetting == Theme.USE_GLOBAL) {
                k9MessageViewThemeSetting = k9Theme
            }
        }

    @JvmStatic
    val fontSizes = FontSizes()

    @JvmStatic
    var backgroundOps = BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC

    @JvmStatic
    var isShowAnimations = true

    private var confirmDelete = false
    private var confirmDiscardMessage = true
    private var confirmDeleteStarred = false
    private var confirmSpam = false
    private var confirmDeleteFromNotification = true
    private var confirmMarkAllRead = true

    @JvmStatic
    var notificationHideSubject = NotificationHideSubject.NEVER

    @JvmStatic
    var notificationQuickDeleteBehaviour = NotificationQuickDelete.NEVER

    @JvmStatic
    var lockScreenNotificationVisibility = LockScreenNotificationVisibility.MESSAGE_COUNT

    @JvmStatic
    var isShowMessageListCheckboxes = true

    @JvmStatic
    var isShowMessageListStars = true

    @JvmStatic
    var messageListPreviewLines = 2

    @JvmStatic
    var isShowCorrespondentNames = true

    @JvmStatic
    var isMessageListSenderAboveSubject = false

    @JvmStatic
    var isShowContactName = false

    @JvmStatic
    var isChangeContactNameColor = false

    @JvmStatic
    var contactNameColor = 0xff00008f.toInt()

    private var showContactPicture = true
    private var messageViewFixedWidthFont = false
    private var messageViewReturnToList = false
    private var messageViewShowNext = false
    var isGesturesEnabled = true

    @JvmStatic
    var isUseVolumeKeysForNavigation = false

    @JvmStatic
    var isUseVolumeKeysForListNavigation = false

    @JvmStatic
    var isStartInUnifiedInbox = false

    private var measureAccounts = true
    private var countSearchMessages = true

    @JvmStatic
    var isHideSpecialAccounts = false

    @JvmStatic
    var isAutoFitWidth: Boolean = false

    var quietTimeEnabled = false
    var isNotificationDuringQuietTimeEnabled = true
    var quietTimeStarts: String? = null
    var quietTimeEnds: String? = null
    private var wrapFolderNames = false
    private var hideUserAgent = false
    private var hideTimeZone = false

    @get:Synchronized
    @set:Synchronized
    @JvmStatic
    var sortType: SortType = Account.DEFAULT_SORT_TYPE
    private val sortAscending = mutableMapOf<SortType, Boolean>()

    private var useBackgroundAsUnreadIndicator = true

    @get:Synchronized
    @set:Synchronized
    @JvmStatic
    var isThreadedViewEnabled = true

    @get:Synchronized
    @set:Synchronized
    @JvmStatic
    var splitViewMode = SplitViewMode.NEVER

    var isColorizeMissingContactPictures = true

    @JvmStatic
    var isMessageViewArchiveActionVisible = false

    @JvmStatic
    var isMessageViewDeleteActionVisible = true

    @JvmStatic
    var isMessageViewMoveActionVisible = false

    @JvmStatic
    var isMessageViewCopyActionVisible = false

    @JvmStatic
    var isMessageViewSpamActionVisible = false

    @JvmStatic
    var pgpInlineDialogCounter: Int = 0

    @JvmStatic
    var pgpSignOnlyDialogCounter: Int = 0

    @JvmStatic
    val k9MessageViewTheme: Theme
        get() = if (k9MessageViewThemeSetting == Theme.USE_GLOBAL) k9Theme else k9MessageViewThemeSetting

    @JvmStatic
    val k9ComposerTheme: Theme
        get() = if (k9ComposerThemeSetting == Theme.USE_GLOBAL) k9Theme else k9ComposerThemeSetting

    val isQuietTime: Boolean
        get() {
            if (!quietTimeEnabled) {
                return false
            }

            val quietTimeChecker = QuietTimeChecker(Clock.INSTANCE, quietTimeStarts, quietTimeEnds)
            return quietTimeChecker.isQuietTime
        }

    @JvmStatic
    var isDebug: Boolean
        get() = DEBUG
        set(debug) {
            DEBUG = debug
            updateLoggingStatus()
        }

    @JvmStatic
    fun messageViewFixedWidthFont(): Boolean {
        return messageViewFixedWidthFont
    }

    fun setMessageViewFixedWidthFont(fixed: Boolean) {
        messageViewFixedWidthFont = fixed
    }

    @JvmStatic
    fun messageViewReturnToList(): Boolean {
        return messageViewReturnToList
    }

    fun setMessageViewReturnToList(messageViewReturnToList: Boolean) {
        K9.messageViewReturnToList = messageViewReturnToList
    }

    @JvmStatic
    fun messageViewShowNext(): Boolean {
        return messageViewShowNext
    }

    fun setMessageViewShowNext(messageViewShowNext: Boolean) {
        K9.messageViewShowNext = messageViewShowNext
    }

    @JvmStatic
    fun measureAccounts(): Boolean {
        return measureAccounts
    }

    fun setMeasureAccounts(measureAccounts: Boolean) {
        K9.measureAccounts = measureAccounts
    }

    @JvmStatic
    fun countSearchMessages(): Boolean {
        return countSearchMessages
    }

    fun setCountSearchMessages(countSearchMessages: Boolean) {
        K9.countSearchMessages = countSearchMessages
    }

    @JvmStatic
    fun confirmDelete(): Boolean {
        return confirmDelete
    }

    fun setConfirmDelete(confirm: Boolean) {
        confirmDelete = confirm
    }

    @JvmStatic
    fun confirmDeleteStarred(): Boolean {
        return confirmDeleteStarred
    }

    fun setConfirmDeleteStarred(confirm: Boolean) {
        confirmDeleteStarred = confirm
    }

    @JvmStatic
    fun confirmSpam(): Boolean {
        return confirmSpam
    }

    @JvmStatic
    fun confirmDiscardMessage(): Boolean {
        return confirmDiscardMessage
    }

    fun setConfirmSpam(confirm: Boolean) {
        confirmSpam = confirm
    }

    fun setConfirmDiscardMessage(confirm: Boolean) {
        confirmDiscardMessage = confirm
    }

    @JvmStatic
    fun confirmDeleteFromNotification(): Boolean {
        return confirmDeleteFromNotification
    }

    @JvmStatic
    fun setConfirmDeleteFromNotification(confirm: Boolean) {
        confirmDeleteFromNotification = confirm
    }

    @JvmStatic
    fun confirmMarkAllRead(): Boolean {
        return confirmMarkAllRead
    }

    fun setConfirmMarkAllRead(confirm: Boolean) {
        confirmMarkAllRead = confirm
    }

    @JvmStatic
    fun wrapFolderNames(): Boolean {
        return wrapFolderNames
    }

    fun setWrapFolderNames(state: Boolean) {
        wrapFolderNames = state
    }

    @JvmStatic
    fun hideUserAgent(): Boolean {
        return hideUserAgent
    }

    fun setHideUserAgent(state: Boolean) {
        hideUserAgent = state
    }

    @JvmStatic
    fun hideTimeZone(): Boolean {
        return hideTimeZone
    }

    fun setHideTimeZone(state: Boolean) {
        hideTimeZone = state
    }

    @Synchronized
    @JvmStatic
    fun isSortAscending(sortType: SortType): Boolean {
        if (sortAscending[sortType] == null) {
            sortAscending[sortType] = sortType.isDefaultAscending
        }
        return sortAscending[sortType]!!
    }

    @Synchronized
    @JvmStatic
    fun setSortAscending(sortType: SortType, sortAscending: Boolean) {
        K9.sortAscending[sortType] = sortAscending
    }

    @Synchronized
    @JvmStatic
    fun useBackgroundAsUnreadIndicator(): Boolean {
        return useBackgroundAsUnreadIndicator
    }

    @Synchronized
    fun setUseBackgroundAsUnreadIndicator(enabled: Boolean) {
        useBackgroundAsUnreadIndicator = enabled
    }

    @JvmStatic
    fun showContactPicture(): Boolean {
        return showContactPicture
    }

    fun setShowContactPicture(show: Boolean) {
        showContactPicture = show
    }


    fun init(context: Context) {
        K9MailLib.setDebugStatus(object : K9MailLib.DebugStatus {
            override fun enabled(): Boolean = DEBUG

            override fun debugSensitive(): Boolean = DEBUG_SENSITIVE
        })

        checkCachedDatabaseVersion(context)

        loadPrefs(preferences)
    }

    /**
     * Load preferences into our statics.
     *
     * If you're adding a preference here, odds are you'll need to add it to
     * [com.fsck.k9.preferences.GlobalSettings], too.
     *
     * @param prefs Preferences to load
     */
    @JvmStatic
    fun loadPrefs(prefs: Preferences) {
        val storage = prefs.storage
        isDebug = storage.getBoolean("enableDebugLogging", DEVELOPER_MODE)
        DEBUG_SENSITIVE = storage.getBoolean("enableSensitiveLogging", false)
        isShowAnimations = storage.getBoolean("animations", true)
        isGesturesEnabled = storage.getBoolean("gesturesEnabled", false)
        isUseVolumeKeysForNavigation = storage.getBoolean("useVolumeKeysForNavigation", false)
        isUseVolumeKeysForListNavigation = storage.getBoolean("useVolumeKeysForListNavigation", false)
        isStartInUnifiedInbox = storage.getBoolean("startIntegratedInbox", false)
        measureAccounts = storage.getBoolean("measureAccounts", true)
        countSearchMessages = storage.getBoolean("countSearchMessages", true)
        isHideSpecialAccounts = storage.getBoolean("hideSpecialAccounts", false)
        isMessageListSenderAboveSubject = storage.getBoolean("messageListSenderAboveSubject", false)
        isShowMessageListCheckboxes = storage.getBoolean("messageListCheckboxes", false)
        isShowMessageListStars = storage.getBoolean("messageListStars", true)
        messageListPreviewLines = storage.getInt("messageListPreviewLines", 2)

        isAutoFitWidth = storage.getBoolean("autofitWidth", true)

        quietTimeEnabled = storage.getBoolean("quietTimeEnabled", false)
        isNotificationDuringQuietTimeEnabled = storage.getBoolean("notificationDuringQuietTimeEnabled", true)
        quietTimeStarts = storage.getString("quietTimeStarts", "21:00")
        quietTimeEnds = storage.getString("quietTimeEnds", "7:00")

        isShowCorrespondentNames = storage.getBoolean("showCorrespondentNames", true)
        isShowContactName = storage.getBoolean("showContactName", false)
        showContactPicture = storage.getBoolean("showContactPicture", true)
        isChangeContactNameColor = storage.getBoolean("changeRegisteredNameColor", false)
        contactNameColor = storage.getInt("registeredNameColor", -0xffff71)
        messageViewFixedWidthFont = storage.getBoolean("messageViewFixedWidthFont", false)
        messageViewReturnToList = storage.getBoolean("messageViewReturnToList", false)
        messageViewShowNext = storage.getBoolean("messageViewShowNext", false)
        wrapFolderNames = storage.getBoolean("wrapFolderNames", false)
        hideUserAgent = storage.getBoolean("hideUserAgent", false)
        hideTimeZone = storage.getBoolean("hideTimeZone", false)

        confirmDelete = storage.getBoolean("confirmDelete", false)
        confirmDiscardMessage = storage.getBoolean("confirmDiscardMessage", true)
        confirmDeleteStarred = storage.getBoolean("confirmDeleteStarred", false)
        confirmSpam = storage.getBoolean("confirmSpam", false)
        confirmDeleteFromNotification = storage.getBoolean("confirmDeleteFromNotification", true)
        confirmMarkAllRead = storage.getBoolean("confirmMarkAllRead", true)

        sortType = try {
            val value = storage.getString("sortTypeEnum", Account.DEFAULT_SORT_TYPE.name)
            SortType.valueOf(value)
        } catch (e: Exception) {
            Account.DEFAULT_SORT_TYPE
        }

        val sortAscendingSetting = storage.getBoolean("sortAscending", Account.DEFAULT_SORT_ASCENDING)
        sortAscending[sortType] = sortAscendingSetting

        val notificationHideSubjectSetting = storage.getString("notificationHideSubject", null)
        notificationHideSubject = if (notificationHideSubjectSetting == null) {
            // If the "notificationHideSubject" setting couldn't be found, the app was probably
            // updated. Look for the old "keyguardPrivacy" setting and map it to the new enum.
            if (storage.getBoolean("keyguardPrivacy", false)) {
                NotificationHideSubject.WHEN_LOCKED
            } else {
                NotificationHideSubject.NEVER
            }
        } else {
            NotificationHideSubject.valueOf(notificationHideSubjectSetting)
        }

        val notificationQuickDelete = storage.getString("notificationQuickDelete", null)
        if (notificationQuickDelete != null) {
            notificationQuickDeleteBehaviour = NotificationQuickDelete.valueOf(notificationQuickDelete)
        }

        val lockScreenNotificationVisibilitySetting = storage.getString("lockScreenNotificationVisibility", null)
        if (lockScreenNotificationVisibilitySetting != null) {
            lockScreenNotificationVisibility =
                    LockScreenNotificationVisibility.valueOf(lockScreenNotificationVisibilitySetting)
        }

        val splitViewModeSetting = storage.getString("splitViewMode", null)
        if (splitViewModeSetting != null) {
            splitViewMode = SplitViewMode.valueOf(splitViewModeSetting)
        }

        useBackgroundAsUnreadIndicator = storage.getBoolean("useBackgroundAsUnreadIndicator", true)
        isThreadedViewEnabled = storage.getBoolean("threadedView", true)
        fontSizes.load(storage)

        backgroundOps = try {
            val settingValue = storage.getString("backgroundOperations", BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC.name)
            BACKGROUND_OPS.valueOf(settingValue)
        } catch (e: Exception) {
            BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC
        }

        isColorizeMissingContactPictures = storage.getBoolean("colorizeMissingContactPictures", true)

        isMessageViewArchiveActionVisible = storage.getBoolean("messageViewArchiveActionVisible", false)
        isMessageViewDeleteActionVisible = storage.getBoolean("messageViewDeleteActionVisible", true)
        isMessageViewMoveActionVisible = storage.getBoolean("messageViewMoveActionVisible", false)
        isMessageViewCopyActionVisible = storage.getBoolean("messageViewCopyActionVisible", false)
        isMessageViewSpamActionVisible = storage.getBoolean("messageViewSpamActionVisible", false)

        pgpInlineDialogCounter = storage.getInt("pgpInlineDialogCounter", 0)
        pgpSignOnlyDialogCounter = storage.getInt("pgpSignOnlyDialogCounter", 0)

        k9Language = storage.getString("language", "")

        var themeValue = storage.getInt("theme", Theme.LIGHT.ordinal)
        // We used to save the resource ID of the theme. So convert that to the new format if necessary.
        k9Theme = if (themeValue == Theme.DARK.ordinal || themeValue == android.R.style.Theme) {
            Theme.DARK
        } else {
            Theme.LIGHT
        }

        themeValue = storage.getInt("messageViewTheme", Theme.USE_GLOBAL.ordinal)
        k9MessageViewThemeSetting = Theme.values()[themeValue]
        themeValue = storage.getInt("messageComposeTheme", Theme.USE_GLOBAL.ordinal)
        k9ComposerThemeSetting = Theme.values()[themeValue]
        isFixedMessageViewTheme = storage.getBoolean("fixedMessageViewTheme", true)
    }

    @JvmStatic
    fun save(editor: StorageEditor) {
        editor.putBoolean("enableDebugLogging", DEBUG)
        editor.putBoolean("enableSensitiveLogging", DEBUG_SENSITIVE)
        editor.putString("backgroundOperations", K9.backgroundOps.name)
        editor.putBoolean("animations", isShowAnimations)
        editor.putBoolean("gesturesEnabled", isGesturesEnabled)
        editor.putBoolean("useVolumeKeysForNavigation", isUseVolumeKeysForNavigation)
        editor.putBoolean("useVolumeKeysForListNavigation", isUseVolumeKeysForListNavigation)
        editor.putBoolean("autofitWidth", isAutoFitWidth)
        editor.putBoolean("quietTimeEnabled", quietTimeEnabled)
        editor.putBoolean("notificationDuringQuietTimeEnabled", isNotificationDuringQuietTimeEnabled)
        editor.putString("quietTimeStarts", quietTimeStarts)
        editor.putString("quietTimeEnds", quietTimeEnds)

        editor.putBoolean("startIntegratedInbox", isStartInUnifiedInbox)
        editor.putBoolean("measureAccounts", measureAccounts)
        editor.putBoolean("countSearchMessages", countSearchMessages)
        editor.putBoolean("messageListSenderAboveSubject", isMessageListSenderAboveSubject)
        editor.putBoolean("hideSpecialAccounts", isHideSpecialAccounts)
        editor.putBoolean("messageListStars", isShowMessageListStars)
        editor.putInt("messageListPreviewLines", messageListPreviewLines)
        editor.putBoolean("messageListCheckboxes", isShowMessageListCheckboxes)
        editor.putBoolean("showCorrespondentNames", isShowCorrespondentNames)
        editor.putBoolean("showContactName", isShowContactName)
        editor.putBoolean("showContactPicture", showContactPicture)
        editor.putBoolean("changeRegisteredNameColor", isChangeContactNameColor)
        editor.putInt("registeredNameColor", contactNameColor)
        editor.putBoolean("messageViewFixedWidthFont", messageViewFixedWidthFont)
        editor.putBoolean("messageViewReturnToList", messageViewReturnToList)
        editor.putBoolean("messageViewShowNext", messageViewShowNext)
        editor.putBoolean("wrapFolderNames", wrapFolderNames)
        editor.putBoolean("hideUserAgent", hideUserAgent)
        editor.putBoolean("hideTimeZone", hideTimeZone)

        editor.putString("language", k9Language)
        editor.putInt("theme", k9Theme.ordinal)
        editor.putInt("messageViewTheme", k9MessageViewThemeSetting.ordinal)
        editor.putInt("messageComposeTheme", k9ComposerThemeSetting.ordinal)
        editor.putBoolean("fixedMessageViewTheme", isFixedMessageViewTheme)

        editor.putBoolean("confirmDelete", confirmDelete)
        editor.putBoolean("confirmDiscardMessage", confirmDiscardMessage)
        editor.putBoolean("confirmDeleteStarred", confirmDeleteStarred)
        editor.putBoolean("confirmSpam", confirmSpam)
        editor.putBoolean("confirmDeleteFromNotification", confirmDeleteFromNotification)
        editor.putBoolean("confirmMarkAllRead", confirmMarkAllRead)

        editor.putString("sortTypeEnum", sortType.name)
        editor.putBoolean("sortAscending", sortAscending[sortType] ?: false)

        editor.putString("notificationHideSubject", notificationHideSubject.toString())
        editor.putString("notificationQuickDelete", notificationQuickDeleteBehaviour.toString())
        editor.putString("lockScreenNotificationVisibility", lockScreenNotificationVisibility.toString())

        editor.putBoolean("useBackgroundAsUnreadIndicator", useBackgroundAsUnreadIndicator)
        editor.putBoolean("threadedView", isThreadedViewEnabled)
        editor.putString("splitViewMode", splitViewMode.name)
        editor.putBoolean("colorizeMissingContactPictures", isColorizeMissingContactPictures)

        editor.putBoolean("messageViewArchiveActionVisible", isMessageViewArchiveActionVisible)
        editor.putBoolean("messageViewDeleteActionVisible", isMessageViewDeleteActionVisible)
        editor.putBoolean("messageViewMoveActionVisible", isMessageViewMoveActionVisible)
        editor.putBoolean("messageViewCopyActionVisible", isMessageViewCopyActionVisible)
        editor.putBoolean("messageViewSpamActionVisible", isMessageViewSpamActionVisible)

        editor.putInt("pgpInlineDialogCounter", pgpInlineDialogCounter)
        editor.putInt("pgpSignOnlyDialogCounter", pgpSignOnlyDialogCounter)

        fontSizes.save(editor)
    }

    private fun updateLoggingStatus() {
        Timber.uprootAll()
        val enableDebugLogging = BuildConfig.DEBUG || DEBUG
        if (enableDebugLogging) {
            Timber.plant(DebugTree())
        }
    }

    @JvmStatic
    fun saveSettingsAsync() {
        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg voids: Void): Void? {
                val editor = preferences.createStorageEditor()
                save(editor)
                editor.commit()

                return null
            }
        }.execute()
    }


    const val LOCAL_UID_PREFIX = "K9LOCAL:"

    const val IDENTITY_HEADER = K9MailLib.IDENTITY_HEADER

    /**
     * Specifies how many messages will be shown in a folder by default. This number is set
     * on each new folder and can be incremented with "Load more messages..." by the
     * VISIBLE_LIMIT_INCREMENT
     */
    const val DEFAULT_VISIBLE_LIMIT = 25

    /**
     * The maximum size of an attachment we're willing to download (either View or Save)
     * Attachments that are base64 encoded (most) will be about 1.375x their actual size
     * so we should probably factor that in. A 5MB attachment will generally be around
     * 6.8MB downloaded but only 5MB saved.
     */
    const val MAX_ATTACHMENT_DOWNLOAD_SIZE = 128 * 1024 * 1024

    /**
     * How many times should K-9 try to deliver a message before giving up until the app is killed and restarted
     */
    const val MAX_SEND_ATTEMPTS = 5

    const val MANUAL_WAKE_LOCK_TIMEOUT = 120000
    const val PUSH_WAKE_LOCK_TIMEOUT = K9MailLib.PUSH_WAKE_LOCK_TIMEOUT
    const val MAIL_SERVICE_WAKE_LOCK_TIMEOUT = 60000
    const val BOOT_RECEIVER_WAKE_LOCK_TIMEOUT = 60000


    /**
     * Possible values for the different theme settings.
     *
     * **Important:**
     * Do not change the order of the items! The ordinal value (position) is used when saving the settings.
     */
    enum class Theme {
        LIGHT,
        DARK,
        USE_GLOBAL
    }

    enum class BACKGROUND_OPS {
        ALWAYS, NEVER, WHEN_CHECKED_AUTO_SYNC
    }

    /**
     * Controls when to hide the subject in the notification area.
     */
    enum class NotificationHideSubject {
        ALWAYS,
        WHEN_LOCKED,
        NEVER
    }

    /**
     * Controls behaviour of delete button in notifications.
     */
    enum class NotificationQuickDelete {
        ALWAYS,
        FOR_SINGLE_MSG,
        NEVER
    }

    enum class LockScreenNotificationVisibility {
        EVERYTHING,
        SENDERS,
        MESSAGE_COUNT,
        APP_NAME,
        NOTHING
    }

    /**
     * Controls when to use the message list split view.
     */
    enum class SplitViewMode {
        ALWAYS,
        NEVER,
        WHEN_IN_LANDSCAPE
    }

    object Intents {
        object Share {
            lateinit var EXTRA_FROM: String
        }

        internal fun init(packageName: String) {
            Share.EXTRA_FROM = "$packageName.intent.extra.SENDER"
        }
    }
}
