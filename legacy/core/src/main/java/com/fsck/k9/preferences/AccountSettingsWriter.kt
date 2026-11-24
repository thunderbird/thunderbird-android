package com.fsck.k9.preferences

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.eygraber.uri.toKmpUri
import com.fsck.k9.Core
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator
import java.io.File
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.UNASSIGNED_ACCOUNT_NUMBER
import net.thunderbird.core.android.account.DeletePolicy
import net.thunderbird.core.android.account.Expunge
import net.thunderbird.core.android.account.FolderMode
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.android.account.MessageFormat
import net.thunderbird.core.android.account.QuoteStyle
import net.thunderbird.core.android.account.ShowPictures
import net.thunderbird.core.android.account.SortType
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StorageProvider
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.avatar.AvatarImageRepository
import net.thunderbird.feature.account.storage.legacy.serializer.ServerSettingsDtoSerializer
import net.thunderbird.feature.account.storage.profile.AvatarDto
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.account.storage.profile.ProfileDto
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.notification.NotificationLight
import net.thunderbird.feature.notification.NotificationSettings
import net.thunderbird.feature.notification.NotificationVibration
import net.thunderbird.feature.notification.VibratePattern
import net.thunderbird.legacy.logging.Log.logger

private const val TAG = "AccountSettingsWriter"

/**
 * Saves imported account settings through the account manager and defers folder settings until folder creation.
 *
 * Used during settings import and account creation workflows to:
 * - Generate unique account identifiers and account display names
 * - Save account, server, and identity settings through the account manager
 * - Queue folder settings for folders that may not yet exist
 * - Assign unique notification channel versions
 * - Initialize special local folders for newly created/imported accounts
 */
internal class AccountSettingsWriter(
    private val accountManager: LegacyAccountManager,
    private val storageProvider: StorageProvider,
    private val localFoldersCreator: SpecialLocalFoldersCreator,
    private val clock: Clock,
    private val generalSettingsManager: GeneralSettingsManager,
    serverSettingsDtoSerializer: ServerSettingsDtoSerializer,
    private val avatarImageRepository: AvatarImageRepository,
    private val context: Context,
) {
    private val folderSettingsWriter = FolderSettingsWriter(generalSettingsManager)
    private val serverSettingsWriter = ServerSettingsWriter(serverSettingsDtoSerializer, generalSettingsManager)

    suspend fun write(account: ValidatedSettings.Account): Pair<AccountDescription, AccountDescription> {
        val originalAccountName = requireNotNull(account.name)
        val originalAccountId = AccountIdFactory.of(account.uuid)
        val originalAccount = AccountDescription(originalAccountName, originalAccountId.toString())

        val accountId = getUniqueAccountId(originalAccountId)
        val accountName = getUniqueAccountName(originalAccountName)
        val writtenAccount = AccountDescription(accountName, accountId.toString())

        val avatarImageUri = account.decodeAvatarImage(accountId)
        val importedAccount = createAccount(account, accountId, accountName, avatarImageUri)
        // A failed folder-settings commit must not leave an account behind that a retry would duplicate.
        writeFolders(accountId, account.folders)
        accountManager.create(importedAccount)
        localFoldersCreator.createSpecialLocalFolders(accountId)
        Core.setServicesEnabled(context)

        return originalAccount to writtenAccount
    }

    private inline fun <reified T : Enum<T>> InternalSettingsMap.enum(key: String, default: T): T =
        when (val value = this[key]) {
            null -> default
            is String -> enumValueOf(value)
            is T -> value
            else -> error("Invalid enum setting: $key")
        }

    @Suppress("LongMethod")
    private fun createAccount(
        account: ValidatedSettings.Account,
        accountId: AccountId,
        name: String,
        avatarImageUri: String?,
    ): LegacyAccount {
        val settings = account.settings
        fun bool(key: String): Boolean = settings[key] as? Boolean
            ?: (AccountSettingsDescriptions.SETTINGS[key]?.lastEntry()?.value?.defaultValue as Boolean)
        fun int(key: String): Int = settings[key] as? Int
            ?: (AccountSettingsDescriptions.SETTINGS[key]?.lastEntry()?.value?.defaultValue as Int)
        fun string(key: String) = settings[key] as? String

        val identities = account.identities.map { identity ->
            Identity(
                name = identity.name,
                email = identity.email,
                description = identity.description,
                signature = identity.settings["signature"] as? String,
                signatureUse = identity.settings["signatureUse"] as? Boolean ?: false,
                signatureIsHtml = identity.settings["signatureIsHtml"] as? Boolean ?: false,
                replyTo = identity.settings["replyTo"] as? String,
            )
        }
        val avatar = AvatarDto(
            accountId = accountId,
            avatarType = settings.enum("avatarType", AvatarTypeDto.MONOGRAM),
            avatarMonogram = string("avatarMonogram"),
            avatarImageUri = avatarImageUri ?: string("avatarImageUri"),
            avatarIconName = string("avatarIconName"),
        )
        val sortType = settings.enum("sortTypeEnum", SortType.SORT_DATE)
        val messageFormat = settings.enum("messageFormat", MessageFormat.HTML)
        val notificationSettings = NotificationSettings(
            isRingEnabled = bool("ring"),
            ringtone = string("ringtone"),
            light = settings.enum("notificationLight", NotificationLight.Disabled),
            vibration = NotificationVibration(
                isEnabled = bool("vibrate"),
                pattern = VibratePattern.deserialize(int("vibratePattern")),
                repeatCount = int("vibrateTimes"),
            ),
        )
        // A restored UUID must not reuse a previously existing notification channel ID.
        val channelVersion = clock.now().epochSeconds.toInt()
        return LegacyAccount(
            id = accountId,
            name = name,
            email = identities.first().email.orEmpty(),
            profile = ProfileDto(accountId, name, int("chipColor"), avatar),
            incomingServerSettings = serverSettingsWriter.createServerSettings(account.incoming),
            outgoingServerSettings = serverSettingsWriter.createServerSettings(account.outgoing),
            identities = identities,
            senderName = identities.first().name,
            alwaysBcc = string("alwaysBcc"),
            automaticCheckIntervalMinutes = int("automaticCheckIntervalMinutes"),
            displayCount = int("displayCount"),
            isNotifyNewMail = bool("notifyNewMail"),
            folderNotifyNewMailMode = settings.enum("folderNotifyNewMailMode", FolderMode.ALL),
            isNotifySelfNewMail = bool("notifySelfNewMail"),
            isNotifyContactsMailOnly = bool("notifyContactsMailOnly"),
            isIgnoreChatMessages = bool("ignoreChatMessages"),
            isNotifySync = bool("notifyMailCheck"),
            deletePolicy = DeletePolicy.fromInt(int("deletePolicy")),
            legacyInboxFolder = string("inboxFolderName"),
            importedDraftsFolder = string("draftsFolderName"),
            importedSentFolder = string("sentFolderName"),
            importedTrashFolder = string("trashFolderName"),
            importedArchiveFolder = string("archiveFolderName"),
            importedSpamFolder = string("spamFolderName"),
            importedAutoExpandFolder = string("autoExpandFolderName"),
            archiveFolderSelection = settings.enum("archiveFolderSelection", SpecialFolderSelection.AUTOMATIC),
            draftsFolderSelection = settings.enum("draftsFolderSelection", SpecialFolderSelection.AUTOMATIC),
            sentFolderSelection = settings.enum("sentFolderSelection", SpecialFolderSelection.AUTOMATIC),
            spamFolderSelection = settings.enum("spamFolderSelection", SpecialFolderSelection.AUTOMATIC),
            trashFolderSelection = settings.enum("trashFolderSelection", SpecialFolderSelection.AUTOMATIC),
            sortType = sortType,
            sortAscending = mapOf(sortType to bool("sortAscending")),
            showPictures = settings.enum("showPicturesEnum", ShowPictures.NEVER),
            // The account manager assigns the number when saving, using the same rule as for new accounts
            accountNumber = UNASSIGNED_ACCOUNT_NUMBER,
            messagesNotificationChannelVersion = channelVersion,
            isFinishedSetup = true,
            notificationSettings = notificationSettings,
            folderDisplayMode = settings.enum("folderDisplayMode", FolderMode.NOT_SECOND_CLASS),
            folderSyncMode = settings.enum("folderSyncMode", FolderMode.FIRST_CLASS),
            folderPushMode = settings.enum("folderPushMode", FolderMode.NONE),
            isSignatureBeforeQuotedText = bool("signatureBeforeQuotedText"),
            expungePolicy = settings.enum("expungePolicy", Expunge.EXPUNGE_IMMEDIATELY),
            isSyncRemoteDeletions = bool("syncRemoteDeletions"),
            maxPushFolders = int("maxPushFolders"),
            idleRefreshMinutes = int("idleRefreshMinutes"),
            isSubscribedFoldersOnly = bool("subscribedFoldersOnly"),
            maximumPolledMessageAge = int("maximumPolledMessageAge"),
            maximumAutoDownloadMessageSize = int("maximumAutoDownloadMessageSize"),
            messageFormat = messageFormat,
            isMessageFormatAuto = messageFormat == MessageFormat.AUTO,
            isMessageReadReceipt = bool("messageReadReceipt"),
            quoteStyle = settings.enum("quoteStyle", QuoteStyle.PREFIX),
            quotePrefix = string("quotePrefix"),
            isDefaultQuotedTextShown = bool("defaultQuotedTextShown"),
            isReplyAfterQuote = bool("replyAfterQuote"),
            isStripSignature = bool("stripSignature"),
            useCompression = bool("useCompression"),
            isSendClientInfoEnabled = bool("sendClientInfo"),
            isOpenPgpHideSignOnly = bool("openPgpHideSignOnly"),
            isOpenPgpEncryptSubject = bool("openPgpEncryptSubject"),
            isOpenPgpEncryptAllDrafts = bool("openPgpEncryptAllDrafts"),
            autocryptPreferEncryptMutual = bool("autocryptMutualMode"),
            isRemoteSearchFullText = bool("remoteSearchFullText"),
            remoteSearchNumResults = int("remoteSearchNumResults"),
            isUploadSentMessages = bool("uploadSentMessages"),
            isMarkMessageAsReadOnView = bool("markMessageAsReadOnView"),
            isMarkMessageAsReadOnDelete = bool("markMessageAsReadOnDelete"),
            isAlwaysShowCcBcc = bool("alwaysShowCcBcc"),
            folderPathDelimiter = string("folderPathDelimiter") ?: "/",
        )
    }

    private fun writeFolders(accountId: AccountId, folders: List<ValidatedSettings.Folder>) {
        if (folders.isEmpty()) return
        val editor = storageProvider.createStorageEditor()
        for (folder in folders) {
            folderSettingsWriter.write(editor, accountId.toString(), folder)
        }
        check(editor.commit()) { "Failed to save pending folder settings" }
    }

    private fun getUniqueAccountId(accountId: AccountId): AccountId {
        val existingAccount = accountManager.findById(accountId)
        return if (existingAccount != null) {
            AccountIdFactory.create()
        } else {
            accountId
        }
    }

    private fun getUniqueAccountName(accountName: String): String {
        val accounts = accountManager.findAll()
        if (!isAccountNameUsed(accountName, accounts)) {
            return accountName
        }

        // Account name is already in use. So generate a new one by appending " (x)", where x is the first
        // number >= 1 that results in an unused account name.
        for (i in 1..accounts.size) {
            val newAccountName = "$accountName ($i)"
            if (!isAccountNameUsed(newAccountName, accounts)) {
                return newAccountName
            }
        }

        error("Unexpected exit")
    }

    private fun isAccountNameUsed(name: String?, accounts: List<LegacyAccount>): Boolean {
        return accounts.any { it.profile.name == name }
    }

    private suspend fun ValidatedSettings.Account.decodeAvatarImage(accountId: AccountId): String? {
        val base64 = avatarImage
        return base64?.let { base64 ->
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val tempFile = File(context.cacheDir, "avatar_import_$accountId")
            try {
                tempFile.writeBytes(bytes)
                val tempUri = Uri.fromFile(tempFile)
                val newUri = avatarImageRepository.update(accountId, tempUri.toKmpUri())
                newUri.toString()
            } catch (e: IOException) {
                logger.error(tag = TAG, throwable = e) { "Failed to decode avatar image." }
                null
            } finally {
                tempFile.delete()
            }
        }
    }
}
