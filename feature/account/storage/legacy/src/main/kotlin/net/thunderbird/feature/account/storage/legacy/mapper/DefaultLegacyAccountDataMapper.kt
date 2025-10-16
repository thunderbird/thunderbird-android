package net.thunderbird.feature.account.storage.legacy.mapper

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.account.storage.profile.ProfileDto

internal class DefaultLegacyAccountDataMapper : LegacyAccountDataMapper {

    @Suppress("LongMethod")
    override fun toDomain(dto: LegacyAccountDto): LegacyAccount {
        return LegacyAccount(
            isSensitiveDebugLoggingEnabled = dto.isSensitiveDebugLoggingEnabled,

            // Account
            id = dto.id,

            // BaseAccount
            name = dto.name,

            // AccountProfile
            profile = toProfileDto(dto),

            // Uncategorized
            identities = dto.identities,
            email = dto.email,
            deletePolicy = dto.deletePolicy,
            incomingServerSettings = dto.incomingServerSettings,
            outgoingServerSettings = dto.outgoingServerSettings,
            oAuthState = dto.oAuthState,
            alwaysBcc = dto.alwaysBcc,
            automaticCheckIntervalMinutes = dto.automaticCheckIntervalMinutes,
            displayCount = dto.displayCount,
            isNotifyNewMail = dto.isNotifyNewMail,
            folderNotifyNewMailMode = dto.folderNotifyNewMailMode,
            isNotifySelfNewMail = dto.isNotifySelfNewMail,
            isNotifyContactsMailOnly = dto.isNotifyContactsMailOnly,
            isIgnoreChatMessages = dto.isIgnoreChatMessages,
            legacyInboxFolder = dto.legacyInboxFolder,
            importedDraftsFolder = dto.importedDraftsFolder,
            importedSentFolder = dto.importedSentFolder,
            importedTrashFolder = dto.importedTrashFolder,
            importedArchiveFolder = dto.importedArchiveFolder,
            importedSpamFolder = dto.importedSpamFolder,
            inboxFolderId = dto.inboxFolderId,
            draftsFolderId = dto.draftsFolderId,
            sentFolderId = dto.sentFolderId,
            trashFolderId = dto.trashFolderId,
            archiveFolderId = dto.archiveFolderId,
            spamFolderId = dto.spamFolderId,
            draftsFolderSelection = dto.draftsFolderSelection,
            sentFolderSelection = dto.sentFolderSelection,
            trashFolderSelection = dto.trashFolderSelection,
            archiveFolderSelection = dto.archiveFolderSelection,
            spamFolderSelection = dto.spamFolderSelection,
            importedAutoExpandFolder = dto.importedAutoExpandFolder,
            autoExpandFolderId = dto.autoExpandFolderId,
            folderDisplayMode = dto.folderDisplayMode,
            folderSyncMode = dto.folderSyncMode,
            folderPushMode = dto.folderPushMode,
            accountNumber = dto.accountNumber,
            isNotifySync = dto.isNotifySync,
            sortType = dto.sortType,
            sortAscending = dto.sortAscending,
            showPictures = dto.showPictures,
            isSignatureBeforeQuotedText = dto.isSignatureBeforeQuotedText,
            expungePolicy = dto.expungePolicy,
            maxPushFolders = dto.maxPushFolders,
            idleRefreshMinutes = dto.idleRefreshMinutes,
            useCompression = dto.useCompression,
            isSendClientInfoEnabled = dto.isSendClientInfoEnabled,
            isSubscribedFoldersOnly = dto.isSubscribedFoldersOnly,
            maximumPolledMessageAge = dto.maximumPolledMessageAge,
            maximumAutoDownloadMessageSize = dto.maximumAutoDownloadMessageSize,
            messageFormat = dto.messageFormat,
            isMessageFormatAuto = dto.isMessageFormatAuto,
            isMessageReadReceipt = dto.isMessageReadReceipt,
            quoteStyle = dto.quoteStyle,
            quotePrefix = dto.quotePrefix,
            isDefaultQuotedTextShown = dto.isDefaultQuotedTextShown,
            isReplyAfterQuote = dto.isReplyAfterQuote,
            isStripSignature = dto.isStripSignature,
            isSyncRemoteDeletions = dto.isSyncRemoteDeletions,
            openPgpProvider = dto.openPgpProvider,
            openPgpKey = dto.openPgpKey,
            autocryptPreferEncryptMutual = dto.autocryptPreferEncryptMutual,
            isOpenPgpHideSignOnly = dto.isOpenPgpHideSignOnly,
            isOpenPgpEncryptSubject = dto.isOpenPgpEncryptSubject,
            isOpenPgpEncryptAllDrafts = dto.isOpenPgpEncryptAllDrafts,
            isMarkMessageAsReadOnView = dto.isMarkMessageAsReadOnView,
            isMarkMessageAsReadOnDelete = dto.isMarkMessageAsReadOnDelete,
            isAlwaysShowCcBcc = dto.isAlwaysShowCcBcc,
            isRemoteSearchFullText = dto.isRemoteSearchFullText,
            remoteSearchNumResults = dto.remoteSearchNumResults,
            isUploadSentMessages = dto.isUploadSentMessages,
            lastSyncTime = dto.lastSyncTime,
            lastFolderListRefreshTime = dto.lastFolderListRefreshTime,
            isFinishedSetup = dto.isFinishedSetup,
            messagesNotificationChannelVersion = dto.messagesNotificationChannelVersion,
            isChangedVisibleLimits = dto.isChangedVisibleLimits,
            lastSelectedFolderId = dto.lastSelectedFolderId,
            notificationSettings = dto.notificationSettings,
            senderName = dto.senderName,
            signatureUse = dto.signatureUse,
            signature = dto.signature,
            shouldMigrateToOAuth = dto.shouldMigrateToOAuth,
            folderPathDelimiter = dto.folderPathDelimiter,
        )
    }

    private fun toProfileDto(dto: LegacyAccountDto): ProfileDto {
        return ProfileDto(
            id = dto.id,
            name = dto.displayName,
            color = dto.chipColor,
            avatar = dto.avatar,
        )
    }

    @Suppress("LongMethod")
    override fun toDto(domain: LegacyAccount): LegacyAccountDto {
        return LegacyAccountDto(
            uuid = domain.uuid,
            isSensitiveDebugLoggingEnabled = domain.isSensitiveDebugLoggingEnabled,
        ).apply {
            identities = domain.identities.toMutableList()
            email = domain.email

            // [AccountProfile]
            fromProfileDto(domain.profile, this)

            // Uncategorized
            deletePolicy = domain.deletePolicy
            incomingServerSettings = domain.incomingServerSettings
            outgoingServerSettings = domain.outgoingServerSettings
            oAuthState = domain.oAuthState
            alwaysBcc = domain.alwaysBcc
            automaticCheckIntervalMinutes = domain.automaticCheckIntervalMinutes
            displayCount = domain.displayCount
            isNotifyNewMail = domain.isNotifyNewMail
            folderNotifyNewMailMode = domain.folderNotifyNewMailMode
            isNotifySelfNewMail = domain.isNotifySelfNewMail
            isNotifyContactsMailOnly = domain.isNotifyContactsMailOnly
            isIgnoreChatMessages = domain.isIgnoreChatMessages
            legacyInboxFolder = domain.legacyInboxFolder
            importedDraftsFolder = domain.importedDraftsFolder
            importedSentFolder = domain.importedSentFolder
            importedTrashFolder = domain.importedTrashFolder
            importedArchiveFolder = domain.importedArchiveFolder
            importedSpamFolder = domain.importedSpamFolder
            inboxFolderId = domain.inboxFolderId
            draftsFolderId = domain.draftsFolderId
            sentFolderId = domain.sentFolderId
            trashFolderId = domain.trashFolderId
            archiveFolderId = domain.archiveFolderId
            spamFolderId = domain.spamFolderId
            draftsFolderSelection = domain.draftsFolderSelection
            sentFolderSelection = domain.sentFolderSelection
            trashFolderSelection = domain.trashFolderSelection
            archiveFolderSelection = domain.archiveFolderSelection
            spamFolderSelection = domain.spamFolderSelection
            importedAutoExpandFolder = domain.importedAutoExpandFolder
            autoExpandFolderId = domain.autoExpandFolderId
            folderDisplayMode = domain.folderDisplayMode
            folderSyncMode = domain.folderSyncMode
            folderPushMode = domain.folderPushMode
            accountNumber = domain.accountNumber
            isNotifySync = domain.isNotifySync
            sortType = domain.sortType
            sortAscending = domain.sortAscending.toMutableMap()
            showPictures = domain.showPictures
            isSignatureBeforeQuotedText = domain.isSignatureBeforeQuotedText
            expungePolicy = domain.expungePolicy
            maxPushFolders = domain.maxPushFolders
            idleRefreshMinutes = domain.idleRefreshMinutes
            useCompression = domain.useCompression
            isSendClientInfoEnabled = domain.isSendClientInfoEnabled
            isSubscribedFoldersOnly = domain.isSubscribedFoldersOnly
            maximumPolledMessageAge = domain.maximumPolledMessageAge
            maximumAutoDownloadMessageSize = domain.maximumAutoDownloadMessageSize
            messageFormat = domain.messageFormat
            isMessageFormatAuto = domain.isMessageFormatAuto
            isMessageReadReceipt = domain.isMessageReadReceipt
            quoteStyle = domain.quoteStyle
            quotePrefix = domain.quotePrefix
            isDefaultQuotedTextShown = domain.isDefaultQuotedTextShown
            isReplyAfterQuote = domain.isReplyAfterQuote
            isStripSignature = domain.isStripSignature
            isSyncRemoteDeletions = domain.isSyncRemoteDeletions
            openPgpProvider = domain.openPgpProvider
            openPgpKey = domain.openPgpKey
            autocryptPreferEncryptMutual = domain.autocryptPreferEncryptMutual
            isOpenPgpHideSignOnly = domain.isOpenPgpHideSignOnly
            isOpenPgpEncryptSubject = domain.isOpenPgpEncryptSubject
            isOpenPgpEncryptAllDrafts = domain.isOpenPgpEncryptAllDrafts
            isMarkMessageAsReadOnView = domain.isMarkMessageAsReadOnView
            isMarkMessageAsReadOnDelete = domain.isMarkMessageAsReadOnDelete
            isAlwaysShowCcBcc = domain.isAlwaysShowCcBcc
            isRemoteSearchFullText = domain.isRemoteSearchFullText
            remoteSearchNumResults = domain.remoteSearchNumResults
            isUploadSentMessages = domain.isUploadSentMessages
            lastSyncTime = domain.lastSyncTime
            lastFolderListRefreshTime = domain.lastFolderListRefreshTime
            isFinishedSetup = domain.isFinishedSetup
            messagesNotificationChannelVersion = domain.messagesNotificationChannelVersion
            isChangedVisibleLimits = domain.isChangedVisibleLimits
            lastSelectedFolderId = domain.lastSelectedFolderId
            notificationSettings = domain.notificationSettings
            senderName = domain.senderName
            signatureUse = domain.signatureUse
            signature = domain.signature
            shouldMigrateToOAuth = domain.shouldMigrateToOAuth
            folderPathDelimiter = domain.folderPathDelimiter
        }
    }

    private fun fromProfileDto(dto: ProfileDto, account: LegacyAccountDto) {
        account.name = dto.name
        account.chipColor = dto.color
        account.avatar = dto.avatar
    }
}
