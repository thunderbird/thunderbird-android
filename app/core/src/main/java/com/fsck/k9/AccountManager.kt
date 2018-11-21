package com.fsck.k9

import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.NetworkType
import com.fsck.k9.mail.filter.Base64
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.preferences.Storage
import com.fsck.k9.preferences.StorageEditor
import java.util.*

class AccountManager(
        private val preferences: Preferences,
        private val localKeyStoreManager: LocalKeyStoreManager,
        private val storageManager: StorageManager,
        private val resourceProvider: CoreResourceProvider
) {
    fun createAccountWithDefaults(): Account {
        val accountUuid = UUID.randomUUID().toString()
        val account = Account(accountUuid)

        return account.apply {
            storeUri = ""
            transportUri = ""
            localStorageProviderId = storageManager.defaultProviderId
            automaticCheckIntervalMinutes = -1
            idleRefreshMinutes = 24
            pushPollOnConnect = true
            displayCount = K9.DEFAULT_VISIBLE_LIMIT
            accountNumber = -1
            isNotifyNewMail = true
            folderNotifyNewMailMode = Account.FolderMode.ALL
            isShowOngoing = true
            isNotifySelfNewMail = true
            isNotifyContactsMailOnly = false
            folderDisplayMode = Account.FolderMode.NOT_SECOND_CLASS
            folderSyncMode = Account.FolderMode.FIRST_CLASS
            folderPushMode = Account.FolderMode.FIRST_CLASS
            folderTargetMode = Account.FolderMode.NOT_SECOND_CLASS
            sortType = Account.DEFAULT_SORT_TYPE
            sortAscending[Account.DEFAULT_SORT_TYPE] = Account.DEFAULT_SORT_ASCENDING
            showPictures = Account.ShowPictures.NEVER
            isSignatureBeforeQuotedText = false
            expungePolicy = Account.Expunge.EXPUNGE_IMMEDIATELY
            autoExpandFolder = Account.INBOX
            inboxFolder = Account.INBOX
            maxPushFolders = 10
            isGoToUnreadMessageSearch = false
            subscribedFoldersOnly = false
            maximumPolledMessageAge = -1
            maximumAutoDownloadMessageSize = 32768
            messageFormat = Account.DEFAULT_MESSAGE_FORMAT
            messageFormatAuto = Account.DEFAULT_MESSAGE_FORMAT_AUTO
            messageReadReceiptAlways = Account.DEFAULT_MESSAGE_READ_RECEIPT
            quoteStyle = Account.DEFAULT_QUOTE_STYLE
            quotePrefix = Account.DEFAULT_QUOTE_PREFIX
            isDefaultQuotedTextShown = Account.DEFAULT_QUOTED_TEXT_SHOWN
            isReplyAfterQuote = Account.DEFAULT_REPLY_AFTER_QUOTE
            isStripSignature = Account.DEFAULT_STRIP_SIGNATURE
            isSyncRemoteDeletions = true
            openPgpKey = Account.NO_OPENPGP_KEY
            allowRemoteSearch = false
            remoteSearchFullText = false
            remoteSearchNumResults = Account.DEFAULT_REMOTE_SEARCH_NUM_RESULTS
            isUploadSentMessages = true
            isEnabled = true
            isMarkMessageAsReadOnView = true
            isAlwaysShowCcBcc = false
            draftsFolder = null
            setArchiveFolder(null, Account.SpecialFolderSelection.AUTOMATIC)
            setDraftsFolder(null, Account.SpecialFolderSelection.AUTOMATIC)
            setSentFolder(null, Account.SpecialFolderSelection.AUTOMATIC)
            setSpamFolder(null, Account.SpecialFolderSelection.AUTOMATIC)
            setTrashFolder(null, Account.SpecialFolderSelection.AUTOMATIC)

            searchableFolders = Account.Searchable.ALL

            val identities = mutableListOf<Identity>()

            val identity = Identity()
            identity.signatureUse = true
            identity.signature = resourceProvider.defaultSignature()
            identity.description = resourceProvider.defaultIdentityDescription()
            identities.add(identity)

            notificationSetting.setVibrate(false)
            notificationSetting.vibratePattern = 0
            notificationSetting.vibrateTimes = 5
            notificationSetting.isRingEnabled = true
            notificationSetting.ringtone = "content://settings/system/notification_sound"
            notificationSetting.ledColor = chipColor
        }
    }

    fun loadAccount(accountUuid: String): Account {
        val account = Account(accountUuid)

        return account.apply {
            val storage = preferences.storage

            storeUri = Base64.decode(storage.getString("$uuid.storeUri", null))
            localStorageProviderId = storage.getString("$uuid.localStorageProvider", storageManager.defaultProviderId)
            transportUri = Base64.decode(storage.getString("$uuid.transportUri", null))
            description = storage.getString("$uuid.description", null)
            alwaysBcc = storage.getString("$uuid.alwaysBcc", alwaysBcc)
            automaticCheckIntervalMinutes = storage.getInt("$uuid.automaticCheckIntervalMinutes", -1)
            idleRefreshMinutes = storage.getInt("$uuid.idleRefreshMinutes", 24)
            pushPollOnConnect = storage.getBoolean("$uuid.pushPollOnConnect", true)
            displayCount = storage.getInt("$uuid.displayCount", K9.DEFAULT_VISIBLE_LIMIT)
            if (displayCount < 0) {
                displayCount = K9.DEFAULT_VISIBLE_LIMIT
            }
            latestOldMessageSeenTime = storage.getLong("$uuid.latestOldMessageSeenTime", 0)
            isNotifyNewMail = storage.getBoolean("$uuid.notifyNewMail", false)

            folderNotifyNewMailMode = Preferences.getEnumStringPref(storage, "$uuid.folderNotifyNewMailMode", Account.FolderMode.ALL)
            isNotifySelfNewMail = storage.getBoolean("$uuid.notifySelfNewMail", true)
            isNotifyContactsMailOnly = storage.getBoolean("$uuid.notifyContactsMailOnly", false)
            isShowOngoing = storage.getBoolean("$uuid.notifyMailCheck", false)
            deletePolicy = Account.DeletePolicy.fromInt(storage.getInt("$uuid.deletePolicy", Account.DeletePolicy.NEVER.setting))
            inboxFolder = storage.getString("$uuid.inboxFolderName", Account.INBOX)

            val draftsFolder = storage.getString("$uuid.draftsFolderName", null)
            val draftsFolderSelection = Preferences.getEnumStringPref(storage, "$uuid.draftsFolderSelection",
                    Account.SpecialFolderSelection.AUTOMATIC)
            setDraftsFolder(draftsFolder, draftsFolderSelection)

            val spamFolder = storage.getString("$uuid.spamFolderName", null)
            val spamFolderSelection = Preferences.getEnumStringPref(storage, "$uuid.spamFolderSelection",
                    Account.SpecialFolderSelection.AUTOMATIC)
            setSpamFolder(spamFolder, spamFolderSelection)

            val archiveFolder = storage.getString("$uuid.archiveFolderName", null)
            val archiveFolderSelection = Preferences.getEnumStringPref(storage, "$uuid.archiveFolderSelection",
                    Account.SpecialFolderSelection.AUTOMATIC)
            setArchiveFolder(archiveFolder, archiveFolderSelection)

            val sentFolder = storage.getString("$uuid.sentFolderName", null)
            val sentFolderSelection = Preferences.getEnumStringPref(storage, "$uuid.sentFolderSelection",
                    Account.SpecialFolderSelection.AUTOMATIC)
            setSentFolder(sentFolder, sentFolderSelection)

            val trashFolder = storage.getString("$uuid.trashFolderName", null)
            val trashFolderSelection = Preferences.getEnumStringPref(storage, "$uuid.trashFolderSelection",
                    Account.SpecialFolderSelection.AUTOMATIC)
            setTrashFolder(trashFolder, trashFolderSelection)

            expungePolicy = Preferences.getEnumStringPref(storage, "$uuid.expungePolicy", Account.Expunge.EXPUNGE_IMMEDIATELY)
            isSyncRemoteDeletions = storage.getBoolean("$uuid.syncRemoteDeletions", true)

            maxPushFolders = storage.getInt("$uuid.maxPushFolders", 10)
            isGoToUnreadMessageSearch = storage.getBoolean("$uuid.goToUnreadMessageSearch", false)
            subscribedFoldersOnly = storage.getBoolean("$uuid.subscribedFoldersOnly", false)
            maximumPolledMessageAge = storage.getInt("$uuid.maximumPolledMessageAge", -1)
            maximumAutoDownloadMessageSize = storage.getInt("$uuid.maximumAutoDownloadMessageSize", 32768)
            messageFormat = Preferences.getEnumStringPref(storage, "$uuid.messageFormat", Account.DEFAULT_MESSAGE_FORMAT)
            messageFormatAuto = storage.getBoolean("$uuid.messageFormatAuto", Account.DEFAULT_MESSAGE_FORMAT_AUTO)
            if (messageFormatAuto && messageFormat == Account.MessageFormat.TEXT) {
                messageFormat = Account.MessageFormat.AUTO
            }
            messageReadReceiptAlways = storage.getBoolean("$uuid.messageReadReceipt", Account.DEFAULT_MESSAGE_READ_RECEIPT)
            quoteStyle = Preferences.getEnumStringPref(storage, "$uuid.quoteStyle", Account.DEFAULT_QUOTE_STYLE)
            quotePrefix = storage.getString("$uuid.quotePrefix", Account.DEFAULT_QUOTE_PREFIX)
            isDefaultQuotedTextShown = storage.getBoolean("$uuid.defaultQuotedTextShown", Account.DEFAULT_QUOTED_TEXT_SHOWN)
            isReplyAfterQuote = storage.getBoolean("$uuid.replyAfterQuote", Account.DEFAULT_REPLY_AFTER_QUOTE)
            isStripSignature = storage.getBoolean("$uuid.stripSignature", Account.DEFAULT_STRIP_SIGNATURE)
            for (type in NetworkType.values()) {
                val useCompression = storage.getBoolean("$uuid.useCompression.$type",
                        true)
                compressionMap[type] = useCompression
            }

            autoExpandFolder = storage.getString("$uuid.autoExpandFolderName", Account.INBOX)

            accountNumber = storage.getInt("$uuid.accountNumber", 0)

            chipColor = storage.getInt("$uuid.chipColor", Account.FALLBACK_ACCOUNT_COLOR)

            sortType = Preferences.getEnumStringPref(storage, "$uuid.sortTypeEnum", Account.SortType.SORT_DATE)

            sortAscending[sortType] = storage.getBoolean("$uuid.sortAscending", false)

            showPictures = Preferences.getEnumStringPref(storage, "$uuid.showPicturesEnum", Account.ShowPictures.NEVER)

            notificationSetting.setVibrate(storage.getBoolean("$uuid.vibrate", false))
            notificationSetting.vibratePattern = storage.getInt("$uuid.vibratePattern", 0)
            notificationSetting.vibrateTimes = storage.getInt("$uuid.vibrateTimes", 5)
            notificationSetting.isRingEnabled = storage.getBoolean("$uuid.ring", true)
            notificationSetting.ringtone = storage.getString("$uuid.ringtone",
                    "content://settings/system/notification_sound")
            notificationSetting.setLed(storage.getBoolean("$uuid.led", true))
            notificationSetting.ledColor = storage.getInt("$uuid.ledColor", chipColor)

            folderDisplayMode = Preferences.getEnumStringPref(storage, "$uuid.folderDisplayMode", Account.FolderMode.NOT_SECOND_CLASS)

            folderSyncMode = Preferences.getEnumStringPref(storage, "$uuid.folderSyncMode", Account.FolderMode.FIRST_CLASS)

            folderPushMode = Preferences.getEnumStringPref(storage, "$uuid.folderPushMode", Account.FolderMode.FIRST_CLASS)

            folderTargetMode = Preferences.getEnumStringPref(storage, "$uuid.folderTargetMode", Account.FolderMode.NOT_SECOND_CLASS)

            searchableFolders = Preferences.getEnumStringPref(storage, "$uuid.searchableFolders", Account.Searchable.ALL)

            isSignatureBeforeQuotedText = storage.getBoolean("$uuid.signatureBeforeQuotedText", false)
            identities = loadIdentities(account, storage)

            openPgpProvider = storage.getString("$uuid.openPgpProvider", "")
            openPgpKey = storage.getLong("$uuid.cryptoKey", Account.NO_OPENPGP_KEY)
            openPgpHideSignOnly = storage.getBoolean("$uuid.openPgpHideSignOnly", true)
            openPgpEncryptSubject = storage.getBoolean("$uuid.openPgpEncryptSubject", true)
            autocryptPreferEncryptMutual = storage.getBoolean("$uuid.autocryptMutualMode", false)
            allowRemoteSearch = storage.getBoolean("$uuid.allowRemoteSearch", false)
            remoteSearchFullText = storage.getBoolean("$uuid.remoteSearchFullText", false)
            remoteSearchNumResults = storage.getInt("$uuid.remoteSearchNumResults", Account.DEFAULT_REMOTE_SEARCH_NUM_RESULTS)
            isUploadSentMessages = storage.getBoolean("$uuid.uploadSentMessages", true)

            isEnabled = storage.getBoolean("$uuid.enabled", true)
            isMarkMessageAsReadOnView = storage.getBoolean("$uuid.markMessageAsReadOnView", true)
            isAlwaysShowCcBcc = storage.getBoolean("$uuid.alwaysShowCcBcc", false)

            // Use email address as account description if necessary
            if (description == null) {
                description = email
            }
        }
    }

    fun save(account: Account) {
        val accountUuid = account.uuid

        val editor = preferences.storage.edit()

        if (!preferences.storage.getString("accountUuids", "").contains(accountUuid)) {
            /*
             * When the account is first created we assign it a unique account number. The
             * account number will be unique to that account for the lifetime of the account.
             * So, we get all the existing account numbers, sort them ascending, loop through
             * the list and check if the number is greater than 1 + the previous number. If so
             * we use the previous number + 1 as the account number. This refills gaps.
             * accountNumber starts as -1 on a newly created account. It must be -1 for this
             * algorithm to work.
             *
             * I bet there is a much smarter way to do this. Anyone like to suggest it?
             */
            val accounts = preferences.accounts
            val accountNumbers = IntArray(accounts.size)
            for (i in accounts.indices) {
                accountNumbers[i] = accounts[i].accountNumber
            }
            Arrays.sort(accountNumbers)
            for (accountNumber in accountNumbers) {
                if (accountNumber > account.accountNumber + 1) {
                    break
                }
                account.accountNumber = accountNumber
            }
            account.accountNumber += 1

            var accountUuids = preferences.storage.getString("accountUuids", "")
            accountUuids += (if (accountUuids.isNotEmpty()) "," else "") + accountUuid
            editor.putString("accountUuids", accountUuids)
        }

        with (account) {
            editor.putString("$accountUuid.storeUri", Base64.encode(storeUri))
            editor.putString("$accountUuid.localStorageProvider", localStorageProviderId)
            editor.putString("$accountUuid.transportUri", Base64.encode(transportUri))
            editor.putString("$accountUuid.description", description)
            editor.putString("$accountUuid.alwaysBcc", alwaysBcc)
            editor.putInt("$accountUuid.automaticCheckIntervalMinutes", automaticCheckIntervalMinutes)
            editor.putInt("$accountUuid.idleRefreshMinutes", idleRefreshMinutes)
            editor.putBoolean("$accountUuid.pushPollOnConnect", pushPollOnConnect)
            editor.putInt("$accountUuid.displayCount", displayCount)
            editor.putLong("$accountUuid.latestOldMessageSeenTime", latestOldMessageSeenTime)
            editor.putBoolean("$accountUuid.notifyNewMail", isNotifyNewMail)
            editor.putString("$accountUuid.folderNotifyNewMailMode", folderNotifyNewMailMode.name)
            editor.putBoolean("$accountUuid.notifySelfNewMail", isNotifySelfNewMail)
            editor.putBoolean("$accountUuid.notifyContactsMailOnly", isNotifyContactsMailOnly)
            editor.putBoolean("$accountUuid.notifyMailCheck", isShowOngoing)
            editor.putInt("$accountUuid.deletePolicy", deletePolicy.setting)
            editor.putString("$accountUuid.inboxFolderName", inboxFolder)
            editor.putString("$accountUuid.draftsFolderName", draftsFolder)
            editor.putString("$accountUuid.sentFolderName", sentFolder)
            editor.putString("$accountUuid.trashFolderName", trashFolder)
            editor.putString("$accountUuid.archiveFolderName", archiveFolder)
            editor.putString("$accountUuid.spamFolderName", spamFolder)
            editor.putString("$accountUuid.archiveFolderSelection", archiveFolderSelection.name)
            editor.putString("$accountUuid.draftsFolderSelection", draftsFolderSelection.name)
            editor.putString("$accountUuid.sentFolderSelection", sentFolderSelection.name)
            editor.putString("$accountUuid.spamFolderSelection", spamFolderSelection.name)
            editor.putString("$accountUuid.trashFolderSelection", trashFolderSelection.name)
            editor.putString("$accountUuid.autoExpandFolderName", autoExpandFolder)
            editor.putInt("$accountUuid.accountNumber", accountNumber)
            editor.putString("$accountUuid.sortTypeEnum", sortType.name)
            editor.putBoolean("$accountUuid.sortAscending", isSortAscending(sortType))
            editor.putString("$accountUuid.showPicturesEnum", showPictures.name)
            editor.putString("$accountUuid.folderDisplayMode", folderDisplayMode.name)
            editor.putString("$accountUuid.folderSyncMode", folderSyncMode.name)
            editor.putString("$accountUuid.folderPushMode", folderPushMode.name)
            editor.putString("$accountUuid.folderTargetMode", folderTargetMode.name)
            editor.putBoolean("$accountUuid.signatureBeforeQuotedText", isSignatureBeforeQuotedText)
            editor.putString("$accountUuid.expungePolicy", expungePolicy.name)
            editor.putBoolean("$accountUuid.syncRemoteDeletions", isSyncRemoteDeletions)
            editor.putInt("$accountUuid.maxPushFolders", maxPushFolders)
            editor.putString("$accountUuid.searchableFolders", searchableFolders.name)
            editor.putInt("$accountUuid.chipColor", chipColor)
            editor.putBoolean("$accountUuid.goToUnreadMessageSearch", isGoToUnreadMessageSearch)
            editor.putBoolean("$accountUuid.subscribedFoldersOnly", subscribedFoldersOnly)
            editor.putInt("$accountUuid.maximumPolledMessageAge", maximumPolledMessageAge)
            editor.putInt("$accountUuid.maximumAutoDownloadMessageSize", maximumAutoDownloadMessageSize)
            val messageFormatAuto = if (Account.MessageFormat.AUTO == messageFormat) {
                // saving MessageFormat.AUTO as is to the database will cause downgrades to crash on
                // startup, so we save as MessageFormat.TEXT instead with a separate flag for auto.
                editor.putString("$accountUuid.messageFormat", Account.MessageFormat.TEXT.name)
                true
            } else {
                editor.putString("$accountUuid.messageFormat", messageFormat.name)
                false
            }
            editor.putBoolean("$accountUuid.messageFormatAuto", messageFormatAuto)
            editor.putBoolean("$accountUuid.messageReadReceipt", messageReadReceiptAlways)
            editor.putString("$accountUuid.quoteStyle", quoteStyle.name)
            editor.putString("$accountUuid.quotePrefix", quotePrefix)
            editor.putBoolean("$accountUuid.defaultQuotedTextShown", isDefaultQuotedTextShown)
            editor.putBoolean("$accountUuid.replyAfterQuote", isReplyAfterQuote)
            editor.putBoolean("$accountUuid.stripSignature", isStripSignature)
            editor.putLong("$accountUuid.cryptoKey", openPgpKey)
            editor.putBoolean("$accountUuid.openPgpHideSignOnly", openPgpHideSignOnly)
            editor.putBoolean("$accountUuid.openPgpEncryptSubject", openPgpEncryptSubject)
            editor.putString("$accountUuid.openPgpProvider", openPgpProvider)
            editor.putBoolean("$accountUuid.autocryptMutualMode", autocryptPreferEncryptMutual)
            editor.putBoolean("$accountUuid.allowRemoteSearch", allowRemoteSearch)
            editor.putBoolean("$accountUuid.remoteSearchFullText", remoteSearchFullText)
            editor.putInt("$accountUuid.remoteSearchNumResults", remoteSearchNumResults)
            editor.putBoolean("$accountUuid.enabled", isEnabled)
            editor.putBoolean("$accountUuid.markMessageAsReadOnView", isMarkMessageAsReadOnView)
            editor.putBoolean("$accountUuid.alwaysShowCcBcc", isAlwaysShowCcBcc)

            editor.putBoolean("$accountUuid.vibrate", notificationSetting.isVibrateEnabled)
            editor.putInt("$accountUuid.vibratePattern", notificationSetting.vibratePattern)
            editor.putInt("$accountUuid.vibrateTimes", notificationSetting.vibrateTimes)
            editor.putBoolean("$accountUuid.ring", notificationSetting.isRingEnabled)
            editor.putString("$accountUuid.ringtone", notificationSetting.ringtone)
            editor.putBoolean("$accountUuid.led", notificationSetting.isLedEnabled)
            editor.putInt("$accountUuid.ledColor", notificationSetting.ledColor)

            for (type in NetworkType.values()) {
                val useCompression = compressionMap.get(type)
                if (useCompression != null) {
                    editor.putBoolean("$accountUuid.useCompression.$type", useCompression)
                }
            }
        }

        saveIdentities(account, preferences.storage, editor)

        editor.commit()
    }

    fun delete(account: Account) {
        localKeyStoreManager.deleteCertificates(account)

        val accountUuid = account.uuid

        // Get the list of account UUIDs
        val uuids = preferences.storage.getString("accountUuids", "").split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        // Create a list of all account UUIDs excluding this account
        val newUuids = ArrayList<String>(uuids.size)
        for (uuid in uuids) {
            if (uuid != accountUuid) {
                newUuids.add(uuid)
            }
        }

        val editor = preferences.storage.edit()

        // Only change the 'accountUuids' value if this account's UUID was listed before
        if (newUuids.size < uuids.size) {
            val accountUuids = Utility.combine(newUuids.toTypedArray(), ',')
            editor.putString("accountUuids", accountUuids)
        }

        editor.remove("$accountUuid.storeUri")
        editor.remove("$accountUuid.transportUri")
        editor.remove("$accountUuid.description")
        editor.remove("$accountUuid.name")
        editor.remove("$accountUuid.email")
        editor.remove("$accountUuid.alwaysBcc")
        editor.remove("$accountUuid.automaticCheckIntervalMinutes")
        editor.remove("$accountUuid.pushPollOnConnect")
        editor.remove("$accountUuid.idleRefreshMinutes")
        editor.remove("$accountUuid.lastAutomaticCheckTime")
        editor.remove("$accountUuid.latestOldMessageSeenTime")
        editor.remove("$accountUuid.notifyNewMail")
        editor.remove("$accountUuid.notifySelfNewMail")
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
        editor.remove("$accountUuid.folderTargetMode")
        editor.remove("$accountUuid.signatureBeforeQuotedText")
        editor.remove("$accountUuid.expungePolicy")
        editor.remove("$accountUuid.syncRemoteDeletions")
        editor.remove("$accountUuid.maxPushFolders")
        editor.remove("$accountUuid.searchableFolders")
        editor.remove("$accountUuid.chipColor")
        editor.remove("$accountUuid.led")
        editor.remove("$accountUuid.ledColor")
        editor.remove("$accountUuid.goToUnreadMessageSearch")
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
        editor.remove("$accountUuid.enabled")
        editor.remove("$accountUuid.markMessageAsReadOnView")
        editor.remove("$accountUuid.alwaysShowCcBcc")
        editor.remove("$accountUuid.allowRemoteSearch")
        editor.remove("$accountUuid.remoteSearchFullText")
        editor.remove("$accountUuid.remoteSearchNumResults")
        editor.remove("$accountUuid.uploadSentMessages")
        editor.remove("$accountUuid.defaultQuotedTextShown")
        editor.remove("$accountUuid.displayCount")
        editor.remove("$accountUuid.inboxFolderName")
        editor.remove("$accountUuid.localStorageProvider")
        editor.remove("$accountUuid.messageFormat")
        editor.remove("$accountUuid.messageReadReceipt")
        editor.remove("$accountUuid.notifyMailCheck")
        for (type in NetworkType.values()) {
            editor.remove(accountUuid + ".useCompression." + type.name)
        }
        deleteIdentities(account, preferences.storage, editor)
        // TODO: Remove preference settings that may exist for individual folders in the account.
        editor.commit()
    }

    private fun loadIdentities(account: Account, storage: Storage): MutableList<Identity> {
        val accountUuid = account.uuid
        val newIdentities = ArrayList<Identity>()
        var ident = 0
        var gotOne: Boolean
        do {
            gotOne = false
            val name = storage.getString("$accountUuid.${Account.IDENTITY_NAME_KEY}.$ident", null)
            val email = storage.getString("$accountUuid.${Account.IDENTITY_EMAIL_KEY}.$ident", null)
            val signatureUse = storage.getBoolean("$accountUuid.signatureUse.$ident", true)
            val signature = storage.getString("$accountUuid.signature.$ident", null)
            val description = storage.getString("$accountUuid.${Account.IDENTITY_DESCRIPTION_KEY}.$ident", null)
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

        return Collections.unmodifiableList(newIdentities)
    }

    private fun saveIdentities(account: Account, storage: Storage, editor: StorageEditor) {
        deleteIdentities(account, storage, editor)
        var ident = 0

        with (account) {
            for (identity in identities) {
                editor.putString("$uuid.${Account.IDENTITY_NAME_KEY}.$ident", identity.name)
                editor.putString("$uuid.${Account.IDENTITY_EMAIL_KEY}.$ident", identity.email)
                editor.putBoolean("$uuid.signatureUse.$ident", identity.signatureUse)
                editor.putString("$uuid.signature.$ident", identity.signature)
                editor.putString("$uuid.${Account.IDENTITY_DESCRIPTION_KEY}.$ident", identity.description)
                editor.putString("$uuid.replyTo.$ident", identity.replyTo)
                ident++
            }
        }
    }

    private fun deleteIdentities(account: Account, storage: Storage, editor: StorageEditor) {
        val accountUuid = account.uuid

        var ident = 0
        var gotOne: Boolean
        do {
            gotOne = false
            val email = storage.getString(accountUuid + "." + Account.IDENTITY_EMAIL_KEY + "." + ident, null)
            if (email != null) {
                editor.remove("$accountUuid.${Account.IDENTITY_NAME_KEY}.$ident")
                editor.remove("$accountUuid.${Account.IDENTITY_EMAIL_KEY}.$ident")
                editor.remove("$accountUuid.signatureUse.$ident")
                editor.remove("$accountUuid.signature.$ident")
                editor.remove("$accountUuid.${Account.IDENTITY_DESCRIPTION_KEY}.$ident")
                editor.remove("$accountUuid.replyTo.$ident")
                gotOne = true
            }
            ident++
        } while (gotOne)
    }

    fun move(account: Account, moveUp: Boolean) {
        val uuids = preferences.storage.getString("accountUuids", "").split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val editor = preferences.storage.edit()
        val newUuids = arrayOfNulls<String>(uuids.size)
        if (moveUp) {
            for (i in uuids.indices) {
                if (i > 0 && uuids[i] == account.uuid) {
                    newUuids[i] = newUuids[i - 1]
                    newUuids[i - 1] = account.uuid
                } else {
                    newUuids[i] = uuids[i]
                }
            }
        } else {
            for (i in uuids.indices.reversed()) {
                if (i < uuids.size - 1 && uuids[i] == account.uuid) {
                    newUuids[i] = newUuids[i + 1]
                    newUuids[i + 1] = account.uuid
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

    fun getNextFreeAccountNumber(): Int {
        val accountNumbers = getExistingAccountNumbers()
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

    private fun getExistingAccountNumbers(): List<Int> {
        return preferences.accounts.map { it.accountNumber }.sorted()
    }
}