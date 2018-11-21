package com.fsck.k9

import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.NetworkType
import com.fsck.k9.mail.filter.Base64
import com.fsck.k9.preferences.Storage
import com.fsck.k9.preferences.StorageEditor
import java.util.*

class AccountManager(
        private val preferences: Preferences,
        private val localKeyStoreManager: LocalKeyStoreManager
) {
    @Synchronized
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
            editor.putBoolean("$accountUuid.pushPollOnConnect", isPushPollOnConnect)
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
            editor.putBoolean("$accountUuid.subscribedFoldersOnly", isSubscribedFoldersOnly)
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
            editor.putBoolean("$accountUuid.messageReadReceipt", isMessageReadReceiptAlways)
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
            editor.putBoolean("$accountUuid.allowRemoteSearch", isAllowRemoteSearch)
            editor.putBoolean("$accountUuid.remoteSearchFullText", isRemoteSearchFullText)
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


    @Synchronized
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

    @Synchronized
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

    @Synchronized
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

}