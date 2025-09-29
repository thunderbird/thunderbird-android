package net.thunderbird.feature.account.storage.legacy

import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.FolderMode
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.MessageFormat
import net.thunderbird.core.android.account.QuoteStyle
import net.thunderbird.core.android.account.ShowPictures
import net.thunderbird.core.android.account.SortType
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.getEnumOrDefault
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.legacy.serializer.ServerSettingsDtoSerializer
import net.thunderbird.feature.mail.folder.api.FOLDER_DEFAULT_PATH_DELIMITER
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.notification.NotificationLight
import net.thunderbird.feature.notification.NotificationSettings
import net.thunderbird.feature.notification.NotificationVibration
import net.thunderbird.feature.notification.VibratePattern

class LegacyAccountStorageHandler(
    private val serverSettingsDtoSerializer: ServerSettingsDtoSerializer,
    private val profileDtoStorageHandler: ProfileDtoStorageHandler,
    private val logger: Logger,
) : AccountDtoStorageHandler {

    @Suppress("LongMethod", "MagicNumber")
    @Synchronized
    override fun load(data: LegacyAccountDto, storage: Storage) {
        val keyGen = AccountKeyGenerator(data.id)

        profileDtoStorageHandler.load(data, storage)

        with(data) {
            incomingServerSettings = serverSettingsDtoSerializer.deserialize(
                storage.getStringOrDefault(keyGen.create(INCOMING_SERVER_SETTINGS_KEY), ""),
            )
            outgoingServerSettings = serverSettingsDtoSerializer.deserialize(
                storage.getStringOrDefault(keyGen.create(OUTGOING_SERVER_SETTINGS_KEY), ""),
            )
            oAuthState = storage.getStringOrNull(keyGen.create("oAuthState"))
            alwaysBcc = storage.getStringOrNull(keyGen.create("alwaysBcc")) ?: alwaysBcc
            automaticCheckIntervalMinutes = storage.getInt(
                keyGen.create("automaticCheckIntervalMinutes"),
                AccountDefaultsProvider.Companion.DEFAULT_SYNC_INTERVAL,
            )
            idleRefreshMinutes = storage.getInt(keyGen.create("idleRefreshMinutes"), 24)
            displayCount = storage.getInt(
                keyGen.create("displayCount"),
                AccountDefaultsProvider.Companion.DEFAULT_VISIBLE_LIMIT,
            )
            if (displayCount < 0) {
                displayCount = AccountDefaultsProvider.Companion.DEFAULT_VISIBLE_LIMIT
            }
            isNotifyNewMail = storage.getBoolean(keyGen.create("notifyNewMail"), false)
            folderNotifyNewMailMode = getEnumStringPref<FolderMode>(
                storage,
                keyGen.create("folderNotifyNewMailMode"),
                FolderMode.ALL,
            )
            isNotifySelfNewMail = storage.getBoolean(keyGen.create("notifySelfNewMail"), true)
            isNotifyContactsMailOnly = storage.getBoolean(keyGen.create("notifyContactsMailOnly"), false)
            isIgnoreChatMessages = storage.getBoolean(keyGen.create("ignoreChatMessages"), false)
            isNotifySync = storage.getBoolean(keyGen.create("notifyMailCheck"), false)
            messagesNotificationChannelVersion = storage.getInt(keyGen.create("messagesNotificationChannelVersion"), 0)
            deletePolicy = DeletePolicy.Companion.fromInt(
                storage.getInt(
                    keyGen.create("deletePolicy"),
                    DeletePolicy.NEVER.setting,
                ),
            )
            legacyInboxFolder = storage.getStringOrNull(keyGen.create("inboxFolderName"))
            importedDraftsFolder = storage.getStringOrNull(keyGen.create("draftsFolderName"))
            importedSentFolder = storage.getStringOrNull(keyGen.create("sentFolderName"))
            importedTrashFolder = storage.getStringOrNull(keyGen.create("trashFolderName"))
            importedArchiveFolder = storage.getStringOrNull(keyGen.create("archiveFolderName"))
            importedSpamFolder = storage.getStringOrNull(keyGen.create("spamFolderName"))

            inboxFolderId = storage.getStringOrNull(keyGen.create("inboxFolderId"))?.toLongOrNull()

            val draftsFolderId = storage.getStringOrNull(keyGen.create("draftsFolderId"))?.toLongOrNull()
            val draftsFolderSelection = getEnumStringPref<SpecialFolderSelection>(
                storage,
                keyGen.create("draftsFolderSelection"),
                SpecialFolderSelection.AUTOMATIC,
            )
            setDraftsFolderId(draftsFolderId, draftsFolderSelection)

            val sentFolderId = storage.getStringOrNull(keyGen.create("sentFolderId"))?.toLongOrNull()
            val sentFolderSelection = getEnumStringPref<SpecialFolderSelection>(
                storage,
                keyGen.create("sentFolderSelection"),
                SpecialFolderSelection.AUTOMATIC,
            )
            setSentFolderId(sentFolderId, sentFolderSelection)

            val trashFolderId = storage.getStringOrNull(keyGen.create("trashFolderId"))?.toLongOrNull()
            val trashFolderSelection = getEnumStringPref<SpecialFolderSelection>(
                storage,
                keyGen.create("trashFolderSelection"),
                SpecialFolderSelection.AUTOMATIC,
            )
            setTrashFolderId(trashFolderId, trashFolderSelection)

            val archiveFolderId = storage.getStringOrNull(keyGen.create("archiveFolderId"))?.toLongOrNull()
            val archiveFolderSelection = getEnumStringPref<SpecialFolderSelection>(
                storage,
                keyGen.create("archiveFolderSelection"),
                SpecialFolderSelection.AUTOMATIC,
            )
            setArchiveFolderId(archiveFolderId, archiveFolderSelection)

            val spamFolderId = storage.getStringOrNull(keyGen.create("spamFolderId"))?.toLongOrNull()
            val spamFolderSelection = getEnumStringPref<SpecialFolderSelection>(
                storage,
                keyGen.create("spamFolderSelection"),
                SpecialFolderSelection.AUTOMATIC,
            )
            setSpamFolderId(spamFolderId, spamFolderSelection)

            autoExpandFolderId = storage.getStringOrNull(keyGen.create("autoExpandFolderId"))?.toLongOrNull()

            expungePolicy = getEnumStringPref(storage, keyGen.create("expungePolicy"), Expunge.EXPUNGE_IMMEDIATELY)
            isSyncRemoteDeletions = storage.getBoolean(keyGen.create("syncRemoteDeletions"), true)

            maxPushFolders = storage.getInt(keyGen.create("maxPushFolders"), 10)
            isSubscribedFoldersOnly = storage.getBoolean(keyGen.create("subscribedFoldersOnly"), false)
            maximumPolledMessageAge = storage.getInt(keyGen.create("maximumPolledMessageAge"), -1)
            maximumAutoDownloadMessageSize = storage.getInt(
                keyGen.create("maximumAutoDownloadMessageSize"),
                AccountDefaultsProvider.Companion.DEFAULT_MAXIMUM_AUTO_DOWNLOAD_MESSAGE_SIZE,
            )
            messageFormat = getEnumStringPref(
                storage,
                keyGen.create("messageFormat"),
                AccountDefaultsProvider.Companion.DEFAULT_MESSAGE_FORMAT,
            )
            val messageFormatAuto = storage.getBoolean(
                keyGen.create("messageFormatAuto"),
                AccountDefaultsProvider.Companion.DEFAULT_MESSAGE_FORMAT_AUTO,
            )
            if (messageFormatAuto && messageFormat == MessageFormat.TEXT) {
                messageFormat = MessageFormat.AUTO
            }
            isMessageReadReceipt = storage.getBoolean(
                keyGen.create("messageReadReceipt"),
                AccountDefaultsProvider.Companion.DEFAULT_MESSAGE_READ_RECEIPT,
            )
            quoteStyle = getEnumStringPref<QuoteStyle>(
                storage,
                keyGen.create("quoteStyle"),
                AccountDefaultsProvider.Companion.DEFAULT_QUOTE_STYLE,
            )
            quotePrefix = storage.getStringOrDefault(
                keyGen.create("quotePrefix"),
                AccountDefaultsProvider.Companion.DEFAULT_QUOTE_PREFIX,
            )
            isDefaultQuotedTextShown = storage.getBoolean(
                keyGen.create("defaultQuotedTextShown"),
                AccountDefaultsProvider.Companion.DEFAULT_QUOTED_TEXT_SHOWN,
            )
            isReplyAfterQuote = storage.getBoolean(
                keyGen.create("replyAfterQuote"),
                AccountDefaultsProvider.Companion.DEFAULT_REPLY_AFTER_QUOTE,
            )
            isStripSignature = storage.getBoolean(
                keyGen.create("stripSignature"),
                AccountDefaultsProvider.Companion.DEFAULT_STRIP_SIGNATURE,
            )
            useCompression = storage.getBoolean(keyGen.create("useCompression"), true)
            isSendClientInfoEnabled = storage.getBoolean(keyGen.create("sendClientInfo"), true)

            importedAutoExpandFolder = storage.getStringOrNull(keyGen.create("autoExpandFolderName"))

            accountNumber = storage.getInt(
                keyGen.create("accountNumber"),
                AccountDefaultsProvider.Companion.UNASSIGNED_ACCOUNT_NUMBER,
            )

            sortType = getEnumStringPref<SortType>(storage, keyGen.create("sortTypeEnum"), SortType.SORT_DATE)

            setSortAscending(sortType, storage.getBoolean(keyGen.create("sortAscending"), false))

            showPictures =
                getEnumStringPref<ShowPictures>(storage, keyGen.create("showPicturesEnum"), ShowPictures.NEVER)

            updateNotificationSettings {
                NotificationSettings(
                    isRingEnabled = storage.getBoolean(keyGen.create("ring"), true),
                    ringtone = storage.getStringOrDefault(
                        keyGen.create("ringtone"),
                        AccountDefaultsProvider.Companion.DEFAULT_RINGTONE_URI,
                    ),
                    light = getEnumStringPref(
                        storage,
                        keyGen.create("notificationLight"),
                        NotificationLight.Disabled,
                    ),
                    vibration = NotificationVibration(
                        isEnabled = storage.getBoolean(keyGen.create("vibrate"), false),
                        pattern = VibratePattern.Companion.deserialize(
                            storage.getInt(
                                keyGen.create("vibratePattern"),
                                0,
                            ),
                        ),
                        repeatCount = storage.getInt(keyGen.create("vibrateTimes"), 5),
                    ),
                )
            }

            folderDisplayMode =
                getEnumStringPref<FolderMode>(storage, keyGen.create("folderDisplayMode"), FolderMode.NOT_SECOND_CLASS)

            folderSyncMode =
                getEnumStringPref<FolderMode>(storage, keyGen.create("folderSyncMode"), FolderMode.FIRST_CLASS)

            folderPushMode = getEnumStringPref<FolderMode>(storage, keyGen.create("folderPushMode"), FolderMode.NONE)

            isSignatureBeforeQuotedText = storage.getBoolean(keyGen.create("signatureBeforeQuotedText"), false)
            replaceIdentities(loadIdentities(data.id, storage))

            openPgpProvider = storage.getStringOrDefault(keyGen.create("openPgpProvider"), "")
            openPgpKey = storage.getLong(keyGen.create("cryptoKey"), AccountDefaultsProvider.Companion.NO_OPENPGP_KEY)
            isOpenPgpHideSignOnly = storage.getBoolean(keyGen.create("openPgpHideSignOnly"), true)
            isOpenPgpEncryptSubject = storage.getBoolean(keyGen.create("openPgpEncryptSubject"), true)
            isOpenPgpEncryptAllDrafts = storage.getBoolean(keyGen.create("openPgpEncryptAllDrafts"), true)
            autocryptPreferEncryptMutual = storage.getBoolean(keyGen.create("autocryptMutualMode"), false)
            isRemoteSearchFullText = storage.getBoolean(keyGen.create("remoteSearchFullText"), false)
            remoteSearchNumResults =
                storage.getInt(
                    keyGen.create("remoteSearchNumResults"),
                    AccountDefaultsProvider.Companion.DEFAULT_REMOTE_SEARCH_NUM_RESULTS,
                )
            isUploadSentMessages = storage.getBoolean(keyGen.create("uploadSentMessages"), true)

            isMarkMessageAsReadOnView = storage.getBoolean(keyGen.create("markMessageAsReadOnView"), true)
            isMarkMessageAsReadOnDelete = storage.getBoolean(keyGen.create("markMessageAsReadOnDelete"), true)
            isAlwaysShowCcBcc = storage.getBoolean(keyGen.create("alwaysShowCcBcc"), false)
            lastSyncTime = storage.getLong(keyGen.create("lastSyncTime"), 0L)
            lastFolderListRefreshTime = storage.getLong(keyGen.create("lastFolderListRefreshTime"), 0L)

            shouldMigrateToOAuth = storage.getBoolean(keyGen.create("migrateToOAuth"), false)
            folderPathDelimiter = storage.getStringOrDefault(
                key = keyGen.create(FOLDER_PATH_DELIMITER_KEY),
                defValue = FOLDER_DEFAULT_PATH_DELIMITER,
            )

            val isFinishedSetup = storage.getBoolean(keyGen.create("isFinishedSetup"), true)
            if (isFinishedSetup) markSetupFinished()

            resetChangeMarkers()
        }
    }

    @Synchronized
    private fun loadIdentities(accountId: AccountId, storage: Storage): List<Identity> {
        val newIdentities = ArrayList<Identity>()
        var ident = 0
        var gotOne: Boolean
        val keyGen = AccountKeyGenerator(accountId)

        do {
            gotOne = false
            val name = storage.getStringOrNull(keyGen.create("$IDENTITY_NAME_KEY.$ident"))
            val email = storage.getStringOrNull(keyGen.create("$IDENTITY_EMAIL_KEY.$ident"))
            val signatureUse = storage.getBoolean(keyGen.create("signatureUse.$ident"), false)
            val signature = storage.getStringOrNull(keyGen.create("signature.$ident"))
            val description = storage.getStringOrNull(keyGen.create("$IDENTITY_DESCRIPTION_KEY.$ident"))
            val replyTo = storage.getStringOrNull(keyGen.create("replyTo.$ident"))
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
            val name = storage.getStringOrNull(keyGen.create("name"))
            val email = storage.getStringOrNull(keyGen.create("email"))
            val signatureUse = storage.getBoolean(keyGen.create("signatureUse"), false)
            val signature = storage.getStringOrNull(keyGen.create("signature"))
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
    override fun save(data: LegacyAccountDto, storage: Storage, editor: StorageEditor) {
        val keyGen = AccountKeyGenerator(data.id)

        profileDtoStorageHandler.save(data, storage, editor)

        if (!storage.getStringOrDefault("accountUuids", "").contains(data.uuid)) {
            var accountUuids = storage.getStringOrDefault("accountUuids", "")
            accountUuids += (if (accountUuids.isNotEmpty()) "," else "") + data.uuid
            editor.putString("accountUuids", accountUuids)
        }

        with(data) {
            editor.putString(
                keyGen.create(INCOMING_SERVER_SETTINGS_KEY),
                serverSettingsDtoSerializer.serialize(incomingServerSettings),
            )
            editor.putString(
                keyGen.create(OUTGOING_SERVER_SETTINGS_KEY),
                serverSettingsDtoSerializer.serialize(outgoingServerSettings),
            )
            editor.putString(keyGen.create("oAuthState"), oAuthState)
            editor.putString(keyGen.create("alwaysBcc"), alwaysBcc)
            editor.putInt(keyGen.create("automaticCheckIntervalMinutes"), automaticCheckIntervalMinutes)
            editor.putInt(keyGen.create("idleRefreshMinutes"), idleRefreshMinutes)
            editor.putInt(keyGen.create("displayCount"), displayCount)
            editor.putBoolean(keyGen.create("notifyNewMail"), isNotifyNewMail)
            editor.putString(keyGen.create("folderNotifyNewMailMode"), folderNotifyNewMailMode.name)
            editor.putBoolean(keyGen.create("notifySelfNewMail"), isNotifySelfNewMail)
            editor.putBoolean(keyGen.create("notifyContactsMailOnly"), isNotifyContactsMailOnly)
            editor.putBoolean(keyGen.create("ignoreChatMessages"), isIgnoreChatMessages)
            editor.putBoolean(keyGen.create("notifyMailCheck"), isNotifySync)
            editor.putInt(keyGen.create("messagesNotificationChannelVersion"), messagesNotificationChannelVersion)
            editor.putInt(keyGen.create("deletePolicy"), deletePolicy.setting)
            editor.putString(keyGen.create("inboxFolderName"), legacyInboxFolder)
            editor.putString(keyGen.create("draftsFolderName"), importedDraftsFolder)
            editor.putString(keyGen.create("sentFolderName"), importedSentFolder)
            editor.putString(keyGen.create("trashFolderName"), importedTrashFolder)
            editor.putString(keyGen.create("archiveFolderName"), importedArchiveFolder)
            editor.putString(keyGen.create("spamFolderName"), importedSpamFolder)
            editor.putString(keyGen.create("inboxFolderId"), inboxFolderId?.toString())
            editor.putString(keyGen.create("draftsFolderId"), draftsFolderId?.toString())
            editor.putString(keyGen.create("sentFolderId"), sentFolderId?.toString())
            editor.putString(keyGen.create("trashFolderId"), trashFolderId?.toString())
            editor.putString(keyGen.create("archiveFolderId"), archiveFolderId?.toString())
            editor.putString(keyGen.create("spamFolderId"), spamFolderId?.toString())
            editor.putString(keyGen.create("archiveFolderSelection"), archiveFolderSelection.name)
            editor.putString(keyGen.create("draftsFolderSelection"), draftsFolderSelection.name)
            editor.putString(keyGen.create("sentFolderSelection"), sentFolderSelection.name)
            editor.putString(keyGen.create("spamFolderSelection"), spamFolderSelection.name)
            editor.putString(keyGen.create("trashFolderSelection"), trashFolderSelection.name)
            editor.putString(keyGen.create("autoExpandFolderName"), importedAutoExpandFolder)
            editor.putString(keyGen.create("autoExpandFolderId"), autoExpandFolderId?.toString())
            editor.putInt(keyGen.create("accountNumber"), accountNumber)
            editor.putString(keyGen.create("sortTypeEnum"), sortType.name)
            editor.putBoolean(keyGen.create("sortAscending"), isSortAscending(sortType))
            editor.putString(keyGen.create("showPicturesEnum"), showPictures.name)
            editor.putString(keyGen.create("folderDisplayMode"), folderDisplayMode.name)
            editor.putString(keyGen.create("folderSyncMode"), folderSyncMode.name)
            editor.putString(keyGen.create("folderPushMode"), folderPushMode.name)
            editor.putBoolean(keyGen.create("signatureBeforeQuotedText"), isSignatureBeforeQuotedText)
            editor.putString(keyGen.create("expungePolicy"), expungePolicy.name)
            editor.putBoolean(keyGen.create("syncRemoteDeletions"), isSyncRemoteDeletions)
            editor.putInt(keyGen.create("maxPushFolders"), maxPushFolders)
            editor.putBoolean(keyGen.create("subscribedFoldersOnly"), isSubscribedFoldersOnly)
            editor.putInt(keyGen.create("maximumPolledMessageAge"), maximumPolledMessageAge)
            editor.putInt(keyGen.create("maximumAutoDownloadMessageSize"), maximumAutoDownloadMessageSize)
            val messageFormatAuto = if (MessageFormat.AUTO == messageFormat) {
                // saving MessageFormat.AUTO as is to the database will cause downgrades to crash on
                // startup, so we save as MessageFormat.TEXT instead with a separate flag for auto.
                editor.putString(keyGen.create("messageFormat"), MessageFormat.TEXT.name)
                true
            } else {
                editor.putString(keyGen.create("messageFormat"), messageFormat.name)
                false
            }
            editor.putBoolean(keyGen.create("messageFormatAuto"), messageFormatAuto)
            editor.putBoolean(keyGen.create("messageReadReceipt"), isMessageReadReceipt)
            editor.putString(keyGen.create("quoteStyle"), quoteStyle.name)
            editor.putString(keyGen.create("quotePrefix"), quotePrefix)
            editor.putBoolean(keyGen.create("defaultQuotedTextShown"), isDefaultQuotedTextShown)
            editor.putBoolean(keyGen.create("replyAfterQuote"), isReplyAfterQuote)
            editor.putBoolean(keyGen.create("stripSignature"), isStripSignature)
            editor.putLong(keyGen.create("cryptoKey"), openPgpKey)
            editor.putBoolean(keyGen.create("openPgpHideSignOnly"), isOpenPgpHideSignOnly)
            editor.putBoolean(keyGen.create("openPgpEncryptSubject"), isOpenPgpEncryptSubject)
            editor.putBoolean(keyGen.create("openPgpEncryptAllDrafts"), isOpenPgpEncryptAllDrafts)
            editor.putString(keyGen.create("openPgpProvider"), openPgpProvider)
            editor.putBoolean(keyGen.create("autocryptMutualMode"), autocryptPreferEncryptMutual)
            editor.putBoolean(keyGen.create("remoteSearchFullText"), isRemoteSearchFullText)
            editor.putInt(keyGen.create("remoteSearchNumResults"), remoteSearchNumResults)
            editor.putBoolean(keyGen.create("uploadSentMessages"), isUploadSentMessages)
            editor.putBoolean(keyGen.create("markMessageAsReadOnView"), isMarkMessageAsReadOnView)
            editor.putBoolean(keyGen.create("markMessageAsReadOnDelete"), isMarkMessageAsReadOnDelete)
            editor.putBoolean(keyGen.create("alwaysShowCcBcc"), isAlwaysShowCcBcc)

            editor.putBoolean(keyGen.create("vibrate"), notificationSettings.vibration.isEnabled)
            editor.putInt(keyGen.create("vibratePattern"), notificationSettings.vibration.pattern.serialize())
            editor.putInt(keyGen.create("vibrateTimes"), notificationSettings.vibration.repeatCount)
            editor.putBoolean(keyGen.create("ring"), notificationSettings.isRingEnabled)
            editor.putString(keyGen.create("ringtone"), notificationSettings.ringtone)
            editor.putString(keyGen.create("notificationLight"), notificationSettings.light.name)
            editor.putLong(keyGen.create("lastSyncTime"), lastSyncTime)
            editor.putLong(keyGen.create("lastFolderListRefreshTime"), lastFolderListRefreshTime)
            editor.putBoolean(keyGen.create("isFinishedSetup"), isFinishedSetup)
            editor.putBoolean(keyGen.create("useCompression"), useCompression)
            editor.putBoolean(keyGen.create("sendClientInfo"), isSendClientInfoEnabled)
            editor.putBoolean(keyGen.create("migrateToOAuth"), shouldMigrateToOAuth)
            editor.putString(keyGen.create(FOLDER_PATH_DELIMITER_KEY), folderPathDelimiter)
        }

        saveIdentities(data, storage, editor)
    }

    @Suppress("LongMethod")
    @Synchronized
    override fun delete(data: LegacyAccountDto, storage: Storage, editor: StorageEditor) {
        val keyGen = AccountKeyGenerator(data.id)
        val accountUuid = data.uuid

        profileDtoStorageHandler.delete(data, storage, editor)

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
            val accountUuids = newUuids.joinToString(",")
            editor.putString("accountUuids", accountUuids)
        }

        editor.remove(keyGen.create("oAuthState"))
        editor.remove(keyGen.create(INCOMING_SERVER_SETTINGS_KEY))
        editor.remove(keyGen.create(OUTGOING_SERVER_SETTINGS_KEY))
        editor.remove(keyGen.create("description"))
        editor.remove(keyGen.create("email"))
        editor.remove(keyGen.create("alwaysBcc"))
        editor.remove(keyGen.create("automaticCheckIntervalMinutes"))
        editor.remove(keyGen.create("idleRefreshMinutes"))
        editor.remove(keyGen.create("lastAutomaticCheckTime"))
        editor.remove(keyGen.create("notifyNewMail"))
        editor.remove(keyGen.create("notifySelfNewMail"))
        editor.remove(keyGen.create("ignoreChatMessages"))
        editor.remove(keyGen.create("messagesNotificationChannelVersion"))
        editor.remove(keyGen.create("deletePolicy"))
        editor.remove(keyGen.create("draftsFolderName"))
        editor.remove(keyGen.create("sentFolderName"))
        editor.remove(keyGen.create("trashFolderName"))
        editor.remove(keyGen.create("archiveFolderName"))
        editor.remove(keyGen.create("spamFolderName"))
        editor.remove(keyGen.create("archiveFolderSelection"))
        editor.remove(keyGen.create("draftsFolderSelection"))
        editor.remove(keyGen.create("sentFolderSelection"))
        editor.remove(keyGen.create("spamFolderSelection"))
        editor.remove(keyGen.create("trashFolderSelection"))
        editor.remove(keyGen.create("autoExpandFolderName"))
        editor.remove(keyGen.create("accountNumber"))
        editor.remove(keyGen.create("vibrate"))
        editor.remove(keyGen.create("vibratePattern"))
        editor.remove(keyGen.create("vibrateTimes"))
        editor.remove(keyGen.create("ring"))
        editor.remove(keyGen.create("ringtone"))
        editor.remove(keyGen.create("folderDisplayMode"))
        editor.remove(keyGen.create("folderSyncMode"))
        editor.remove(keyGen.create("folderPushMode"))
        editor.remove(keyGen.create("signatureBeforeQuotedText"))
        editor.remove(keyGen.create("expungePolicy"))
        editor.remove(keyGen.create("syncRemoteDeletions"))
        editor.remove(keyGen.create("maxPushFolders"))
        editor.remove(keyGen.create("notificationLight"))
        editor.remove(keyGen.create("subscribedFoldersOnly"))
        editor.remove(keyGen.create("maximumPolledMessageAge"))
        editor.remove(keyGen.create("maximumAutoDownloadMessageSize"))
        editor.remove(keyGen.create("messageFormatAuto"))
        editor.remove(keyGen.create("quoteStyle"))
        editor.remove(keyGen.create("quotePrefix"))
        editor.remove(keyGen.create("sortTypeEnum"))
        editor.remove(keyGen.create("sortAscending"))
        editor.remove(keyGen.create("showPicturesEnum"))
        editor.remove(keyGen.create("replyAfterQuote"))
        editor.remove(keyGen.create("stripSignature"))
        editor.remove(keyGen.create("cryptoApp")) // this is no longer set, but cleans up legacy values
        editor.remove(keyGen.create("cryptoAutoSignature"))
        editor.remove(keyGen.create("cryptoAutoEncrypt"))
        editor.remove(keyGen.create("cryptoApp"))
        editor.remove(keyGen.create("cryptoKey"))
        editor.remove(keyGen.create("cryptoSupportSignOnly"))
        editor.remove(keyGen.create("openPgpProvider"))
        editor.remove(keyGen.create("openPgpHideSignOnly"))
        editor.remove(keyGen.create("openPgpEncryptSubject"))
        editor.remove(keyGen.create("openPgpEncryptAllDrafts"))
        editor.remove(keyGen.create("autocryptMutualMode"))
        editor.remove(keyGen.create("enabled"))
        editor.remove(keyGen.create("markMessageAsReadOnView"))
        editor.remove(keyGen.create("markMessageAsReadOnDelete"))
        editor.remove(keyGen.create("alwaysShowCcBcc"))
        editor.remove(keyGen.create("remoteSearchFullText"))
        editor.remove(keyGen.create("remoteSearchNumResults"))
        editor.remove(keyGen.create("uploadSentMessages"))
        editor.remove(keyGen.create("defaultQuotedTextShown"))
        editor.remove(keyGen.create("displayCount"))
        editor.remove(keyGen.create("inboxFolderName"))
        editor.remove(keyGen.create("messageFormat"))
        editor.remove(keyGen.create("messageReadReceipt"))
        editor.remove(keyGen.create("notifyMailCheck"))
        editor.remove(keyGen.create("inboxFolderId"))
        editor.remove(keyGen.create("outboxFolderId"))
        editor.remove(keyGen.create("draftsFolderId"))
        editor.remove(keyGen.create("sentFolderId"))
        editor.remove(keyGen.create("trashFolderId"))
        editor.remove(keyGen.create("archiveFolderId"))
        editor.remove(keyGen.create("spamFolderId"))
        editor.remove(keyGen.create("autoExpandFolderId"))
        editor.remove(keyGen.create("lastSyncTime"))
        editor.remove(keyGen.create("lastFolderListRefreshTime"))
        editor.remove(keyGen.create("isFinishedSetup"))
        editor.remove(keyGen.create("useCompression"))
        editor.remove(keyGen.create("sendClientInfo"))
        editor.remove(keyGen.create("migrateToOAuth"))
        editor.remove(keyGen.create(FOLDER_PATH_DELIMITER_KEY))

        deleteIdentities(data, storage, editor)
        // TODO: Remove preference settings that may exist for individual folders in the account.
    }

    @Synchronized
    private fun saveIdentities(data: LegacyAccountDto, storage: Storage, editor: StorageEditor) {
        deleteIdentities(data, storage, editor)
        var ident = 0
        val keyGen = AccountKeyGenerator(data.id)

        with(data) {
            for (identity in identities) {
                editor.putString(keyGen.create("$IDENTITY_NAME_KEY.$ident"), identity.name)
                editor.putString(keyGen.create("$IDENTITY_EMAIL_KEY.$ident"), identity.email)
                editor.putBoolean(keyGen.create("signatureUse.$ident"), identity.signatureUse)
                editor.putString(keyGen.create("signature.$ident"), identity.signature)
                editor.putString(keyGen.create("$IDENTITY_DESCRIPTION_KEY.$ident"), identity.description)
                editor.putString(keyGen.create("replyTo.$ident"), identity.replyTo)
                ident++
            }
        }
    }

    @Synchronized
    private fun deleteIdentities(data: LegacyAccountDto, storage: Storage, editor: StorageEditor) {
        val keyGen = AccountKeyGenerator(data.id)

        var identityIndex = 0
        var gotOne: Boolean
        do {
            gotOne = false
            val email = storage.getStringOrNull(keyGen.create("$IDENTITY_EMAIL_KEY.$identityIndex"))
            if (email != null) {
                editor.remove(keyGen.create("$IDENTITY_NAME_KEY.$identityIndex"))
                editor.remove(keyGen.create("$IDENTITY_EMAIL_KEY.$identityIndex"))
                editor.remove(keyGen.create("signatureUse.$identityIndex"))
                editor.remove(keyGen.create("signature.$identityIndex"))
                editor.remove(keyGen.create("$IDENTITY_DESCRIPTION_KEY.$identityIndex"))
                editor.remove(keyGen.create("replyTo.$identityIndex"))
                gotOne = true
            }
            identityIndex++
        } while (gotOne)
    }

    private inline fun <reified T : Enum<T>> getEnumStringPref(storage: Storage, key: String, defaultEnum: T): T {
        return try {
            storage.getEnumOrDefault<T>(key, defaultEnum)
        } catch (ex: IllegalArgumentException) {
            logger.warn(throwable = ex) {
                "Unable to convert preference key [$key] to enum of type defaultEnum: $defaultEnum"
            }

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

        const val FOLDER_PATH_DELIMITER_KEY = "folderPathDelimiter"
    }
}
