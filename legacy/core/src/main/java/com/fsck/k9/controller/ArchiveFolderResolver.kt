package com.fsck.k9.controller

import app.k9mail.legacy.mailstore.CreateFolderInfo
import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.Preferences
import com.fsck.k9.mailstore.FolderSettingsProvider
import com.fsck.k9.mailstore.LocalMessage
import java.util.Calendar
import java.util.Date
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.feature.mail.folder.api.ArchiveGranularity
import net.thunderbird.feature.mail.folder.api.FOLDER_DEFAULT_PATH_DELIMITER
import com.fsck.k9.mail.FolderType as LegacyFolderType

internal class ArchiveFolderResolver(
    private val messageStoreManager: MessageStoreManager,
    private val preferences: Preferences,
) {

    fun resolveArchiveFolder(
        account: LegacyAccountDto,
        message: LocalMessage,
    ): Long? {
        val baseFolderId = account.archiveFolderId ?: return null

        return when (account.archiveGranularity) {
            ArchiveGranularity.SINGLE_ARCHIVE_FOLDER -> {
                baseFolderId
            }

            ArchiveGranularity.PER_YEAR_ARCHIVE_FOLDERS -> {
                val year = getYear(getMessageDate(message))
                findOrCreateSubfolder(account, baseFolderId, year.toString())
            }

            ArchiveGranularity.PER_MONTH_ARCHIVE_FOLDERS -> {
                val date = getMessageDate(message)
                val year = getYear(date)
                val month = String.format("%02d", getMonth(date))

                val yearFolderId = findOrCreateSubfolder(account, baseFolderId, year.toString())
                    ?: return baseFolderId

                findOrCreateSubfolder(account, yearFolderId, month)
            }
        }
    }

    private fun findOrCreateSubfolder(
        account: LegacyAccountDto,
        parentFolderId: Long,
        subfolderName: String,
    ): Long? {
        val messageStore = messageStoreManager.getMessageStore(account)

        val parentServerId = messageStore.getFolderServerId(parentFolderId) ?: return null

        val delimiter = FOLDER_DEFAULT_PATH_DELIMITER
        val subfolderServerId = "$parentServerId$delimiter$subfolderName"

        messageStore.getFolderId(subfolderServerId)?.let { return it }

        return try {
            val folderSettingsProvider = FolderSettingsProvider(preferences, account)
            val folderInfo = CreateFolderInfo(
                serverId = subfolderServerId,
                name = subfolderServerId,
                type = LegacyFolderType.ARCHIVE,
                settings = folderSettingsProvider.getFolderSettings(subfolderServerId),
            )
            val folderIds = messageStore.createFolders(listOf(folderInfo))
            folderIds.firstOrNull()
        } catch (e: Exception) {
            Log.e(e, "Failed to create archive subfolder: $subfolderServerId")
            null
        }
    }

    private fun getMessageDate(message: LocalMessage): Date {
        return message.internalDate ?: message.sentDate ?: Date()
    }

    private fun getYear(date: Date): Int {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar.get(Calendar.YEAR)
    }

    private fun getMonth(date: Date): Int {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar.get(Calendar.MONTH) + 1
    }
}
