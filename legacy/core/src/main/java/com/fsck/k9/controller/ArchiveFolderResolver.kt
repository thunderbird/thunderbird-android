package com.fsck.k9.controller

import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mailstore.LocalMessage
import java.util.Calendar
import java.util.Date
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.mail.folder.api.ArchiveGranularity
import net.thunderbird.feature.mail.folder.api.FOLDER_DEFAULT_PATH_DELIMITER
import com.fsck.k9.mail.FolderType as LegacyFolderType

internal class ArchiveFolderResolver(
    private val folderIdResolver: FolderIdResolver,
    private val folderCreator: ArchiveFolderCreator,
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
        val parentServerId = folderIdResolver.getFolderServerId(account, parentFolderId) ?: return null

        val delimiter = FOLDER_DEFAULT_PATH_DELIMITER
        val subfolderServerId = "$parentServerId$delimiter$subfolderName"

        folderIdResolver.getFolderId(account, subfolderServerId)?.let { return it }

        val folderInfo = FolderInfo(
            serverId = subfolderServerId,
            name = subfolderServerId,
            type = LegacyFolderType.ARCHIVE,
        )
        return folderCreator.createFolder(account, folderInfo)
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
