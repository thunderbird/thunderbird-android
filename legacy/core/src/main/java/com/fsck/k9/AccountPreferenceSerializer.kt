package com.fsck.k9

import com.fsck.k9.helper.Utility
import com.fsck.k9.preferences.StorageEditor
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
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_STRIP_SIGNATURE
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.DEFAULT_SYNC_INTERVAL
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.NO_OPENPGP_KEY
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.UNASSIGNED_ACCOUNT_NUMBER
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.FolderMode
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.MessageFormat
import net.thunderbird.core.android.account.QuoteStyle
import net.thunderbird.core.android.account.ShowPictures
import net.thunderbird.core.android.account.SortType
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.preferences.Storage
import net.thunderbird.core.preferences.getEnumOrDefault
import net.thunderbird.feature.account.storage.legacy.ServerSettingsSerializer
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.notification.NotificationLight
import net.thunderbird.feature.notification.NotificationSettings
import net.thunderbird.feature.notification.NotificationVibration
import net.thunderbird.feature.notification.VibratePattern

class AccountPreferenceSerializer(
    private val serverSettingsSerializer: ServerSettingsSerializer,
) {

    @Suppress("LongMethod")
    @Synchronized
    fun loadAccount(account: LegacyAccount, storage: Storage) {
        val accountUuid = account.uuid
        with(account) {
            incomingServerSettings = serverSettingsSerializer.deserialize(
                storage.getStringOrDefault("$accountUuid.$INCOMING_SERVER_SETTINGS_KEY", ""),
            )
            outgoingServerSettings = serverSettingsSerializer.deserialize(
                storage.getStringOrDefault("$accountUuid.$OUTGOING_SERVER_SETTINGS_KEY", ""),
            )
            oAuthState = storage.getStringOrNull("$accountUuid.oAuthState")
            name = storage.getStringOrNull("$accountUuid.description")
            alwaysBcc = storage.getStringOrNull("$accountUuid.alwaysBcc") ?: alwaysBcc
            automaticCheckIntervalMinutes = storage.getInt(
                "" +
                    "$accountUuid.automaticCheckIntervalMinutes",
                DEFAULT_SYNC_INTERVAL,
            )
            idleRefreshMinutes = storage.getInt("$accountUuid.idleRefreshMinutes", 24)
            displayCount = storage.getInt("$accountUuid.displayCount", K9.DEFAULT_VISIBLE_LIMIT)
            if (displayCount < 0) {
                displayCount = K9.DEFAULT_VISIBLE_LIMIT
            }
            isNotifyNewMail = storage.getBoolean("$accountUuid.notifyNewMail", false)
            folderNotifyNewMailMode = getEnumStringPref<FolderMode>(
                storage,
                "$accountUuid.folderNotifyNewMailMode",
                FolderMode.ALL,
            )
            isNotifySelfNewMail = storage.getBoolean("$accountUuid.notifySelfNewMail", true)
            isNotifyContactsMailOnly = storage.getBoolean("$accountUuid.notifyContactsMailOnly", false)
            isIgnoreChatMessages = storage.getBoolean("$accountUuid.ignoreChatMessages", false)
            isNotifySync = storage.getBoolean("$accountUuid.notifyMailCheck", false)
            messagesNotificationChannelVersion = storage.getInt("$accountUuid.messagesNotificationChannelVersion", 0)
            deletePolicy = DeletePolicy.fromInt(storage.getInt("$accountUuid.deletePolicy", DeletePolicy.NEVER.setting))
            legacyInboxFolder = storage.getStringOrNull("$accountUuid.inboxFolderName")
            importedDraftsFolder = storage.getStringOrNull("$accountUuid.draftsFolderName")
            importedSentFolder = storage.getStringOrNull("$accountUuid.sentFolderName")
            importedTrashFolder = storage.getStringOrNull("$accountUuid.trashFolderName")
            importedArchiveFolder = storage.getStringOrNull("$accountUuid.archiveFolderName")
            importedSpamFolder = storage.getStringOrNull("$accountUuid.spamFolderName")

            inboxFolderId = storage.getStringOrNull("$accountUuid.inboxFolderId")?.toLongOrNull()
            outboxFolderId = storage.getStringOrNull("$accountUuid.outboxFolderId")?.toLongOrNull()

            val draftsFolderId = storage.getStringOrNull("$accountUuid.draftsFolderId")?.toLongOrNull()
            val draftsFolderSelection = getEnumStringPref<SpecialFolderSelection>(
                storage,
                "$accountUuid.draftsFolderSelection",
                SpecialFolderSelection.AUTOMATIC,
            )
            setDraftsFolderId(draftsFolderId, draftsFolderSelection)

            val sentFolderId = storage.getStringOrNull("$accountUuid.sentFolderId")?.toLongOrNull()
            val sentFolderSelection = getEnumStringPref<SpecialFolderSelection>(
                storage,
                "$accountUuid.sentFolderSelection",
                SpecialFolderSelection.AUTOMATIC,
            )
            setSentFolderId(sentFolderId, sentFolderSelection)

            val trashFolderId = storage.getStringOrNull("$accountUuid.trashFolderId")?.toLongOrNull()
            val trashFolderSelection = getEnumStringPref<SpecialFolderSelection>(
                storage,
                "$accountUuid.trashFolderSelection",
                SpecialFolderSelection.AUTOMATIC,
            )
            setTrashFolderId(trashFolderId, trashFolderSelection)

            val archiveFolderId = storage.getStringOrNull("$accountUuid.archiveFolderId")?.toLongOrNull()
            val archiveFolderSelection = getEnumStringPref<SpecialFolderSelection>(
                storage,
                "$accountUuid.archiveFolderSelection",
                SpecialFolderSelection.AUTOMATIC,
            )
            setArchiveFolderId(archiveFolderId, archiveFolderSelection)

            val spamFolderId = storage.getStringOrNull("$accountUuid.spamFolderId")?.toLongOrNull()
            val spamFolderSelection = getEnumStringPref<SpecialFolderSelection>(
                storage,
                "$accountUuid.spamFolderSelection",
                SpecialFolderSelection.AUTOMATIC,
            )
            setSpamFolderId(spamFolderId, spamFolderSelection)

            autoExpandFolderId = storage.getStringOrNull("$accountUuid.autoExpandFolderId")?.toLongOrNull()

            expungePolicy = getEnumStringPref(storage, "$accountUuid.expungePolicy", Expunge.EXPUNGE_IMMEDIATELY)
            isSyncRemoteDeletions = storage.getBoolean("$accountUuid.syncRemoteDeletions", true)

            maxPushFolders = storage.getInt("$accountUuid.maxPushFolders", 10)
            isSubscribedFoldersOnly = storage.getBoolean("$accountUuid.subscribedFoldersOnly", false)
            maximumPolledMessageAge = storage.getInt("$accountUuid.maximumPolledMessageAge", -1)
            maximumAutoDownloadMessageSize = storage.getInt(
                "$accountUuid.maximumAutoDownloadMessageSize",
                DEFAULT_MAXIMUM_AUTO_DOWNLOAD_MESSAGE_SIZE,
            )
            messageFormat = getEnumStringPref(storage, "$accountUuid.messageFormat", DEFAULT_MESSAGE_FORMAT)
            val messageFormatAuto = storage.getBoolean("$accountUuid.messageFormatAuto", DEFAULT_MESSAGE_FORMAT_AUTO)
            if (messageFormatAuto && messageFormat == MessageFormat.TEXT) {
                messageFormat = MessageFormat.AUTO
            }
            isMessageReadReceipt = storage.getBoolean("$accountUuid.messageReadReceipt", DEFAULT_MESSAGE_READ_RECEIPT)
            quoteStyle = getEnumStringPref<QuoteStyle>(storage, "$accountUuid.quoteStyle", DEFAULT_QUOTE_STYLE)
            quotePrefix = storage.getStringOrDefault("$accountUuid.quotePrefix", DEFAULT_QUOTE_PREFIX)
            isDefaultQuotedTextShown = storage.getBoolean(
                "$accountUuid.defaultQuotedTextShown",
                DEFAULT_QUOTED_TEXT_SHOWN,
            )
            isReplyAfterQuote = storage.getBoolean("$accountUuid.replyAfterQuote", DEFAULT_REPLY_AFTER_QUOTE)
            isStripSignature = storage.getBoolean("$accountUuid.stripSignature", DEFAULT_STRIP_SIGNATURE)
            useCompression = storage.getBoolean("$accountUuid.useCompression", true)
            isSendClientInfoEnabled = storage.getBoolean("$accountUuid.sendClientInfo", true)

            importedAutoExpandFolder = storage.getStringOrNull("$accountUuid.autoExpandFolderName")

            accountNumber = storage.getInt("$accountUuid.accountNumber", UNASSIGNED_ACCOUNT_NUMBER)

            chipColor = storage.getInt("$accountUuid.chipColor", FALLBACK_ACCOUNT_COLOR)

            sortType = getEnumStringPref<SortType>(storage, "$accountUuid.sortTypeEnum", SortType.SORT_DATE)

            setSortAscending(sortType, storage.getBoolean("$accountUuid.sortAscending", false))

            showPictures = getEnumStringPref<ShowPictures>(storage, "$accountUuid.showPicturesEnum", ShowPictures.NEVER)

            updateNotificationSettings {
                NotificationSettings(
                    isRingEnabled = storage.getBoolean("$accountUuid.ring", true),
                    ringtone = storage.getStringOrDefault("$accountUuid.ringtone", DEFAULT_RINGTONE_URI),
                    light = getEnumStringPref(
                        storage,
                        "$accountUuid.notificationLight",
                        NotificationLight.Disabled,
                    ),
                    vibration = NotificationVibration(
                        isEnabled = storage.getBoolean("$accountUuid.vibrate", false),
                        pattern = VibratePattern.deserialize(
                            storage.getInt(
                                "$accountUuid.vibratePattern",
                                0,
                            ),
                        ),
                        repeatCount = storage.getInt("$accountUuid.vibrateTimes", 5),
                    ),
                )
            }

            folderDisplayMode =
                getEnumStringPref<FolderMode>(storage, "$accountUuid.folderDisplayMode", FolderMode.NOT_SECOND_CLASS)

            folderSyncMode =
                getEnumStringPref<FolderMode>(storage, "$accountUuid.folderSyncMode", FolderMode.FIRST_CLASS)

            folderPushMode = getEnumStringPref<FolderMode>(storage, "$accountUuid.folderPushMode", FolderMode.NONE)

            isSignatureBeforeQuotedText = storage.getBoolean("$accountUuid.signatureBeforeQuotedText", false)
            replaceIdentities(loadIdentities(accountUuid, storage))

            openPgpProvider = storage.getStringOrDefault("$accountUuid.openPgpProvider", "")
            openPgpKey = storage.getLong("$accountUuid.cryptoKey", NO_OPENPGP_KEY)
            isOpenPgpHideSignOnly = storage.getBoolean("$accountUuid.openPgpHideSignOnly", true)
            isOpenPgpEncryptSubject = storage.getBoolean("$accountUuid.openPgpEncryptSubject", true)
            isOpenPgpEncryptAllDrafts = storage.getBoolean("$accountUuid.openPgpEncryptAllDrafts", true)
            autocryptPreferEncryptMutual = storage.getBoolean("$accountUuid.autocryptMutualMode", false)
            isRemoteSearchFullText = storage.getBoolean("$accountUuid.remoteSearchFullText", false)
            remoteSearchNumResults =
                storage.getInt("$accountUuid.remoteSearchNumResults", DEFAULT_REMOTE_SEARCH_NUM_RESULTS)
            isUploadSentMessages = storage.getBoolean("$accountUuid.uploadSentMessages", true)

            isMarkMessageAsReadOnView = storage.getBoolean("$accountUuid.markMessageAsReadOnView", true)
            isMarkMessageAsReadOnDelete = storage.getBoolean("$accountUuid.markMessageAsReadOnDelete", true)
            isAlwaysShowCcBcc = storage.getBoolean("$accountUuid.alwaysShowCcBcc", false)
            lastSyncTime = storage.getLong("$accountUuid.lastSyncTime", 0L)
            lastFolderListRefreshTime = storage.getLong("$accountUuid.lastFolderListRefreshTime", 0L)

            shouldMigrateToOAuth = storage.getBoolean("$accountUuid.migrateToOAuth", false)

            val isFinishedSetup = storage.getBoolean("$accountUuid.isFinishedSetup", true)
            if (isFinishedSetup) markSetupFinished()

            resetChangeMarkers()
        }
    }

    @Synchronized
    private fun loadIdentities(accountUuid: String, storage: Storage): List<Identity> {
        val newIdentities = ArrayList<Identity>()
        var ident = 0
        var gotOne: Boolean
        do {
            gotOne = false
            val name = storage.getStringOrNull("$accountUuid.$IDENTITY_NAME_KEY.$ident")
            val email = storage.getStringOrNull("$accountUuid.$IDENTITY_EMAIL_KEY.$ident")
            val signatureUse = storage.getBoolean("$accountUuid.signatureUse.$ident", false)
            val signature = storage.getStringOrNull("$accountUuid.signature.$ident")
            val description = storage.getStringOrNull("$accountUuid.$IDENTITY_DESCRIPTION_KEY.$ident")
            val replyTo = storage.getStringOrNull("$accountUuid.replyTo.$ident")
            if (email != null) {
                val identity = Identity(
                    name = name,
                    email = email,
                    signatureUse = signatureUse,
                    signature = signature,
                    description = description,
                    replyTo = replyTo,
                )
                newIdentities.add(identity)
                gotOne = true
            }
            ident++
        } while (gotOne)

        if (newIdentities.isEmpty()) {
            val name = storage.getStringOrNull("$accountUuid.name")
            val email = storage.getStringOrNull("$accountUuid.email")
            val signatureUse = storage.getBoolean("$accountUuid.signatureUse", false)
            val signature = storage.getStringOrNull("$accountUuid.signature")
            val identity = Identity(
                name = name,
                email = email,
                signatureUse = signatureUse,
                signature = signature,
                description = email,
            )
            newIdentities.add(identity)
        }

        return newIdentities
    }

    @Suppress("LongMethod")
    @Synchronized
    fun save(editor: StorageEditor, storage: Storage, account: LegacyAccount) {
        val accountUuid = account.uuid

        if (!storage.getStringOrDefault("accountUuids", "").contains(account.uuid)) {
            var accountUuids = storage.getStringOrDefault("accountUuids", "")
            accountUuids += (if (accountUuids.isNotEmpty()) "," else "") + account.uuid
            editor.putString("accountUuids", accountUuids)
        }

        with(account) {
            editor.putString(
                "$accountUuid.$INCOMING_SERVER_SETTINGS_KEY",
                serverSettingsSerializer.serialize(incomingServerSettings),
            )
            editor.putString(
                "$accountUuid.$OUTGOING_SERVER_SETTINGS_KEY",
                serverSettingsSerializer.serialize(outgoingServerSettings),
            )
            editor.putString("$accountUuid.oAuthState", oAuthState)
            editor.putString("$accountUuid.description", name)
            editor.putString("$accountUuid.alwaysBcc", alwaysBcc)
            editor.putInt("$accountUuid.automaticCheckIntervalMinutes", automaticCheckIntervalMinutes)
            editor.putInt("$accountUuid.idleRefreshMinutes", idleRefreshMinutes)
            editor.putInt("$accountUuid.displayCount", displayCount)
            editor.putBoolean("$accountUuid.notifyNewMail", isNotifyNewMail)
            editor.putString("$accountUuid.folderNotifyNewMailMode", folderNotifyNewMailMode.name)
            editor.putBoolean("$accountUuid.notifySelfNewMail", isNotifySelfNewMail)
            editor.putBoolean("$accountUuid.notifyContactsMailOnly", isNotifyContactsMailOnly)
            editor.putBoolean("$accountUuid.ignoreChatMessages", isIgnoreChatMessages)
            editor.putBoolean("$accountUuid.notifyMailCheck", isNotifySync)
            editor.putInt("$accountUuid.messagesNotificationChannelVersion", messagesNotificationChannelVersion)
            editor.putInt("$accountUuid.deletePolicy", deletePolicy.setting)
            editor.putString("$accountUuid.inboxFolderName", legacyInboxFolder)
            editor.putString("$accountUuid.draftsFolderName", importedDraftsFolder)
            editor.putString("$accountUuid.sentFolderName", importedSentFolder)
            editor.putString("$accountUuid.trashFolderName", importedTrashFolder)
            editor.putString("$accountUuid.archiveFolderName", importedArchiveFolder)
            editor.putString("$accountUuid.spamFolderName", importedSpamFolder)
            editor.putString("$accountUuid.inboxFolderId", inboxFolderId?.toString())
            editor.putString("$accountUuid.outboxFolderId", outboxFolderId?.toString())
            editor.putString("$accountUuid.draftsFolderId", draftsFolderId?.toString())
            editor.putString("$accountUuid.sentFolderId", sentFolderId?.toString())
            editor.putString("$accountUuid.trashFolderId", trashFolderId?.toString())
            editor.putString("$accountUuid.archiveFolderId", archiveFolderId?.toString())
            editor.putString("$accountUuid.spamFolderId", spamFolderId?.toString())
            editor.putString("$accountUuid.archiveFolderSelection", archiveFolderSelection.name)
            editor.putString("$accountUuid.draftsFolderSelection", draftsFolderSelection.name)
            editor.putString("$accountUuid.sentFolderSelection", sentFolderSelection.name)
            editor.putString("$accountUuid.spamFolderSelection", spamFolderSelection.name)
            editor.putString("$accountUuid.trashFolderSelection", trashFolderSelection.name)
            editor.putString("$accountUuid.autoExpandFolderName", importedAutoExpandFolder)
            editor.putString("$accountUuid.autoExpandFolderId", autoExpandFolderId?.toString())
            editor.putInt("$accountUuid.accountNumber", accountNumber)
            editor.putString("$accountUuid.sortTypeEnum", sortType.name)
            editor.putBoolean("$accountUuid.sortAscending", isSortAscending(sortType))
            editor.putString("$accountUuid.showPicturesEnum", showPictures.name)
            editor.putString("$accountUuid.folderDisplayMode", folderDisplayMode.name)
            editor.putString("$accountUuid.folderSyncMode", folderSyncMode.name)
            editor.putString("$accountUuid.folderPushMode", folderPushMode.name)
            editor.putBoolean("$accountUuid.signatureBeforeQuotedText", isSignatureBeforeQuotedText)
            editor.putString("$accountUuid.expungePolicy", expungePolicy.name)
            editor.putBoolean("$accountUuid.syncRemoteDeletions", isSyncRemoteDeletions)
            editor.putInt("$accountUuid.maxPushFolders", maxPushFolders)
            editor.putInt("$accountUuid.chipColor", chipColor)
            editor.putBoolean("$accountUuid.subscribedFoldersOnly", isSubscribedFoldersOnly)
            editor.putInt("$accountUuid.maximumPolledMessageAge", maximumPolledMessageAge)
            editor.putInt("$accountUuid.maximumAutoDownloadMessageSize", maximumAutoDownloadMessageSize)
            val messageFormatAuto = if (MessageFormat.AUTO == messageFormat) {
                // saving MessageFormat.AUTO as is to the database will cause downgrades to crash on
                // startup, so we save as MessageFormat.TEXT instead with a separate flag for auto.
                editor.putString("$accountUuid.messageFormat", MessageFormat.TEXT.name)
                true
            } else {
                editor.putString("$accountUuid.messageFormat", messageFormat.name)
                false
            }
            editor.putBoolean("$accountUuid.messageFormatAuto", messageFormatAuto)
            editor.putBoolean("$accountUuid.messageReadReceipt", isMessageReadReceipt)
            editor.putString("$accountUuid.quoteStyle", quoteStyle.name)
            editor.putString("$accountUuid.quotePrefix", quotePrefix)
            editor.putBoolean("$accountUuid.defaultQuotedTextShown", isDefaultQuotedTextShown)
            editor.putBoolean("$accountUuid.replyAfterQuote", isReplyAfterQuote)
            editor.putBoolean("$accountUuid.stripSignature", isStripSignature)
            editor.putLong("$accountUuid.cryptoKey", openPgpKey)
            editor.putBoolean("$accountUuid.openPgpHideSignOnly", isOpenPgpHideSignOnly)
            editor.putBoolean("$accountUuid.openPgpEncryptSubject", isOpenPgpEncryptSubject)
            editor.putBoolean("$accountUuid.openPgpEncryptAllDrafts", isOpenPgpEncryptAllDrafts)
            editor.putString("$accountUuid.openPgpProvider", openPgpProvider)
            editor.putBoolean("$accountUuid.autocryptMutualMode", autocryptPreferEncryptMutual)
            editor.putBoolean("$accountUuid.remoteSearchFullText", isRemoteSearchFullText)
            editor.putInt("$accountUuid.remoteSearchNumResults", remoteSearchNumResults)
            editor.putBoolean("$accountUuid.uploadSentMessages", isUploadSentMessages)
            editor.putBoolean("$accountUuid.markMessageAsReadOnView", isMarkMessageAsReadOnView)
            editor.putBoolean("$accountUuid.markMessageAsReadOnDelete", isMarkMessageAsReadOnDelete)
            editor.putBoolean("$accountUuid.alwaysShowCcBcc", isAlwaysShowCcBcc)

            editor.putBoolean("$accountUuid.vibrate", notificationSettings.vibration.isEnabled)
            editor.putInt("$accountUuid.vibratePattern", notificationSettings.vibration.pattern.serialize())
            editor.putInt("$accountUuid.vibrateTimes", notificationSettings.vibration.repeatCount)
            editor.putBoolean("$accountUuid.ring", notificationSettings.isRingEnabled)
            editor.putString("$accountUuid.ringtone", notificationSettings.ringtone)
            editor.putString("$accountUuid.notificationLight", notificationSettings.light.name)
            editor.putLong("$accountUuid.lastSyncTime", lastSyncTime)
            editor.putLong("$accountUuid.lastFolderListRefreshTime", lastFolderListRefreshTime)
            editor.putBoolean("$accountUuid.isFinishedSetup", isFinishedSetup)
            editor.putBoolean("$accountUuid.useCompression", useCompression)
            editor.putBoolean("$accountUuid.sendClientInfo", isSendClientInfoEnabled)
            editor.putBoolean("$accountUuid.migrateToOAuth", shouldMigrateToOAuth)
        }

        saveIdentities(account, storage, editor)
    }

    @Suppress("LongMethod")
    @Synchronized
    fun delete(editor: StorageEditor, storage: Storage, account: LegacyAccount) {
        val accountUuid = account.uuid

        // Get the list of account UUIDs
        val uuids = storage
            .getStringOrDefault("accountUuids", "")
            .split(",".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()

        // Create a list of all account UUIDs excluding this account
        val newUuids = ArrayList<String>(uuids.size)
        for (uuid in uuids) {
            if (uuid != accountUuid) {
                newUuids.add(uuid)
            }
        }

        // Only change the 'accountUuids' value if this account's UUID was listed before
        if (newUuids.size < uuids.size) {
            val accountUuids = Utility.combine(newUuids.toTypedArray(), ',')
            editor.putString("accountUuids", accountUuids)
        }

        editor.remove("$accountUuid.$INCOMING_SERVER_SETTINGS_KEY")
        editor.remove("$accountUuid.$OUTGOING_SERVER_SETTINGS_KEY")
        editor.remove("$accountUuid.oAuthState")
        editor.remove("$accountUuid.description")
        editor.remove("$accountUuid.name")
        editor.remove("$accountUuid.email")
        editor.remove("$accountUuid.alwaysBcc")
        editor.remove("$accountUuid.automaticCheckIntervalMinutes")
        editor.remove("$accountUuid.idleRefreshMinutes")
        editor.remove("$accountUuid.lastAutomaticCheckTime")
        editor.remove("$accountUuid.notifyNewMail")
        editor.remove("$accountUuid.notifySelfNewMail")
        editor.remove("$accountUuid.ignoreChatMessages")
        editor.remove("$accountUuid.messagesNotificationChannelVersion")
        editor.remove("$accountUuid.deletePolicy")
        editor.remove("$accountUuid.draftsFolderName")
        editor.remove("$accountUuid.sentFolderName")
        editor.remove("$accountUuid.trashFolderName")
        editor.remove("$accountUuid.archiveFolderName")
        editor.remove("$accountUuid.spamFolderName")
        editor.remove("$accountUuid.archiveFolderSelection")
        editor.remove("$accountUuid.draftsFolderSelection")
        editor.remove("$accountUuid.sentFolderSelection")
        editor.remove("$accountUuid.spamFolderSelection")
        editor.remove("$accountUuid.trashFolderSelection")
        editor.remove("$accountUuid.autoExpandFolderName")
        editor.remove("$accountUuid.accountNumber")
        editor.remove("$accountUuid.vibrate")
        editor.remove("$accountUuid.vibratePattern")
        editor.remove("$accountUuid.vibrateTimes")
        editor.remove("$accountUuid.ring")
        editor.remove("$accountUuid.ringtone")
        editor.remove("$accountUuid.folderDisplayMode")
        editor.remove("$accountUuid.folderSyncMode")
        editor.remove("$accountUuid.folderPushMode")
        editor.remove("$accountUuid.signatureBeforeQuotedText")
        editor.remove("$accountUuid.expungePolicy")
        editor.remove("$accountUuid.syncRemoteDeletions")
        editor.remove("$accountUuid.maxPushFolders")
        editor.remove("$accountUuid.chipColor")
        editor.remove("$accountUuid.notificationLight")
        editor.remove("$accountUuid.subscribedFoldersOnly")
        editor.remove("$accountUuid.maximumPolledMessageAge")
        editor.remove("$accountUuid.maximumAutoDownloadMessageSize")
        editor.remove("$accountUuid.messageFormatAuto")
        editor.remove("$accountUuid.quoteStyle")
        editor.remove("$accountUuid.quotePrefix")
        editor.remove("$accountUuid.sortTypeEnum")
        editor.remove("$accountUuid.sortAscending")
        editor.remove("$accountUuid.showPicturesEnum")
        editor.remove("$accountUuid.replyAfterQuote")
        editor.remove("$accountUuid.stripSignature")
        editor.remove("$accountUuid.cryptoApp") // this is no longer set, but cleans up legacy values
        editor.remove("$accountUuid.cryptoAutoSignature")
        editor.remove("$accountUuid.cryptoAutoEncrypt")
        editor.remove("$accountUuid.cryptoApp")
        editor.remove("$accountUuid.cryptoKey")
        editor.remove("$accountUuid.cryptoSupportSignOnly")
        editor.remove("$accountUuid.openPgpProvider")
        editor.remove("$accountUuid.openPgpHideSignOnly")
        editor.remove("$accountUuid.openPgpEncryptSubject")
        editor.remove("$accountUuid.openPgpEncryptAllDrafts")
        editor.remove("$accountUuid.autocryptMutualMode")
        editor.remove("$accountUuid.enabled")
        editor.remove("$accountUuid.markMessageAsReadOnView")
        editor.remove("$accountUuid.markMessageAsReadOnDelete")
        editor.remove("$accountUuid.alwaysShowCcBcc")
        editor.remove("$accountUuid.remoteSearchFullText")
        editor.remove("$accountUuid.remoteSearchNumResults")
        editor.remove("$accountUuid.uploadSentMessages")
        editor.remove("$accountUuid.defaultQuotedTextShown")
        editor.remove("$accountUuid.displayCount")
        editor.remove("$accountUuid.inboxFolderName")
        editor.remove("$accountUuid.messageFormat")
        editor.remove("$accountUuid.messageReadReceipt")
        editor.remove("$accountUuid.notifyMailCheck")
        editor.remove("$accountUuid.inboxFolderId")
        editor.remove("$accountUuid.outboxFolderId")
        editor.remove("$accountUuid.draftsFolderId")
        editor.remove("$accountUuid.sentFolderId")
        editor.remove("$accountUuid.trashFolderId")
        editor.remove("$accountUuid.archiveFolderId")
        editor.remove("$accountUuid.spamFolderId")
        editor.remove("$accountUuid.autoExpandFolderId")
        editor.remove("$accountUuid.lastSyncTime")
        editor.remove("$accountUuid.lastFolderListRefreshTime")
        editor.remove("$accountUuid.isFinishedSetup")
        editor.remove("$accountUuid.useCompression")
        editor.remove("$accountUuid.sendClientInfo")
        editor.remove("$accountUuid.migrateToOAuth")

        deleteIdentities(account, storage, editor)
        // TODO: Remove preference settings that may exist for individual folders in the account.
    }

    @Synchronized
    private fun saveIdentities(account: LegacyAccount, storage: Storage, editor: StorageEditor) {
        deleteIdentities(account, storage, editor)
        var ident = 0

        with(account) {
            for (identity in identities) {
                editor.putString("$uuid.$IDENTITY_NAME_KEY.$ident", identity.name)
                editor.putString("$uuid.$IDENTITY_EMAIL_KEY.$ident", identity.email)
                editor.putBoolean("$uuid.signatureUse.$ident", identity.signatureUse)
                editor.putString("$uuid.signature.$ident", identity.signature)
                editor.putString("$uuid.$IDENTITY_DESCRIPTION_KEY.$ident", identity.description)
                editor.putString("$uuid.replyTo.$ident", identity.replyTo)
                ident++
            }
        }
    }

    @Synchronized
    private fun deleteIdentities(account: LegacyAccount, storage: Storage, editor: StorageEditor) {
        val accountUuid = account.uuid

        var identityIndex = 0
        var gotOne: Boolean
        do {
            gotOne = false
            val email = storage.getStringOrNull("$accountUuid.$IDENTITY_EMAIL_KEY.$identityIndex")
            if (email != null) {
                editor.remove("$accountUuid.$IDENTITY_NAME_KEY.$identityIndex")
                editor.remove("$accountUuid.$IDENTITY_EMAIL_KEY.$identityIndex")
                editor.remove("$accountUuid.signatureUse.$identityIndex")
                editor.remove("$accountUuid.signature.$identityIndex")
                editor.remove("$accountUuid.$IDENTITY_DESCRIPTION_KEY.$identityIndex")
                editor.remove("$accountUuid.replyTo.$identityIndex")
                gotOne = true
            }
            identityIndex++
        } while (gotOne)
    }

    fun move(editor: StorageEditor, account: LegacyAccount, storage: Storage, newPosition: Int) {
        val accountUuids = storage.getStringOrDefault("accountUuids", "").split(",").filter { it.isNotEmpty() }
        val oldPosition = accountUuids.indexOf(account.uuid)
        if (oldPosition == -1 || oldPosition == newPosition) return

        val newAccountUuidsString = accountUuids.toMutableList()
            .apply {
                removeAt(oldPosition)
                add(newPosition, account.uuid)
            }
            .joinToString(separator = ",")

        editor.putString("accountUuids", newAccountUuidsString)
    }

    private inline fun <reified T : Enum<T>> getEnumStringPref(storage: Storage, key: String, defaultEnum: T): T {
        return try {
            storage.getEnumOrDefault<T>(key, defaultEnum)
        } catch (ex: IllegalArgumentException) {
            Log.w(
                ex,
                "Unable to convert preference key [%s] to enum of type %s",
                key,
                defaultEnum.declaringJavaClass,
            )

            defaultEnum
        }
    }

    companion object {
        const val ACCOUNT_DESCRIPTION_KEY = "description"
        const val INCOMING_SERVER_SETTINGS_KEY = "incomingServerSettings"
        const val OUTGOING_SERVER_SETTINGS_KEY = "outgoingServerSettings"

        const val IDENTITY_NAME_KEY = "name"
        const val IDENTITY_EMAIL_KEY = "email"
        const val IDENTITY_DESCRIPTION_KEY = "description"

        const val FALLBACK_ACCOUNT_COLOR = 0x0099CC
    }
}
