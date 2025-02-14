package net.discdd.k9.onboarding.util

import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.entity.AccountSyncOptions
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOption
import app.k9mail.feature.account.common.domain.entity.SpecialFolderSettings
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.folders.FolderServerId
import com.fsck.k9.mail.folders.RemoteFolder

object CreateAccountConstants {
    val INCOMING_SERVER_SETTINGS = ServerSettings(
        "imap",
        "imap.example.org",
        993,
        ConnectionSecurity.SSL_TLS_REQUIRED,
        AuthType.PLAIN,
        "username",
        "password",
        null,
    )

    val OUTGOING_SERVER_SETTINGS = ServerSettings(
        "smtp",
        "smtp.example.org",
        465,
        ConnectionSecurity.SSL_TLS_REQUIRED,
        AuthType.PLAIN,
        "username",
        "password",
        null,
    )

    val DISPLAY_OPTIONS = AccountDisplayOptions(
        accountName = "accountName",
        displayName = "displayName",
        emailSignature = "emailSignature",
    )

    val SYNC_OPTIONS = AccountSyncOptions(
        checkFrequencyInMinutes = 10,
        messageDisplayCount = 20,
        showNotification = true,
    )

    val SPECIAL_FOLDER_SETTINGS = SpecialFolderSettings(
        archiveSpecialFolderOption = SpecialFolderOption.Special(
            remoteFolder = RemoteFolder(FolderServerId("archive"), "archive", FolderType.ARCHIVE),
        ),
        draftsSpecialFolderOption = SpecialFolderOption.Special(
            remoteFolder = RemoteFolder(FolderServerId("drafts"), "drafts", FolderType.DRAFTS),
        ),
        sentSpecialFolderOption = SpecialFolderOption.Special(
            remoteFolder = RemoteFolder(FolderServerId("sent"), "sent", FolderType.SENT),
        ),
        spamSpecialFolderOption = SpecialFolderOption.Special(
            remoteFolder = RemoteFolder(FolderServerId("spam"), "spam", FolderType.SPAM),
        ),
        trashSpecialFolderOption = SpecialFolderOption.Special(
            remoteFolder = RemoteFolder(FolderServerId("trash"), "trash", FolderType.TRASH),
        ),
    )
}
