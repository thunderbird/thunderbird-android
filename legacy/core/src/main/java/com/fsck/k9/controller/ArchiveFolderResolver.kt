package com.fsck.k9.controller

import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mailstore.LocalMessage
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.mail.folder.api.ArchiveGranularity
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
                val year = message.messageDate.year
                findOrCreateSubfolder(account, baseFolderId, year.toString()) ?: baseFolderId
            }

            ArchiveGranularity.PER_MONTH_ARCHIVE_FOLDERS -> {
                val date = message.messageDate
                val year = date.year
                val month = String.format(Locale.ROOT, "%02d", date.monthNumber)

                findOrCreateSubfolder(account, baseFolderId, year.toString())?.let { yearFolderId ->
                    findOrCreateSubfolder(account, yearFolderId, month)
                } ?: baseFolderId
            }
        }
    }

    private fun findOrCreateSubfolder(
        account: LegacyAccountDto,
        parentFolderId: Long,
        subfolderName: String,
    ): Long? {
        val parentServerId = folderIdResolver.getFolderServerId(account, parentFolderId) ?: return null

        val delimiter = account.folderPathDelimiter
        val subfolderServerId = "$parentServerId$delimiter$subfolderName"

        val existingId = folderIdResolver.getFolderId(account, subfolderServerId)
        return if (existingId != null) {
            existingId
        } else {
            val folderInfo = FolderInfo(
                serverId = subfolderServerId,
                name = subfolderServerId,
                type = LegacyFolderType.ARCHIVE,
            )
            folderCreator.createFolder(account, folderInfo)
        }
    }

    @OptIn(ExperimentalTime::class)
    private val LocalMessage.messageDate: LocalDate
        get() {
            val epochMillis = (internalDate ?: sentDate)?.time
            val timeZone = TimeZone.currentSystemDefault()
            val instant = epochMillis?.let { kotlin.time.Instant.fromEpochMilliseconds(it) }
                ?: Clock.System.now()
            return instant.toLocalDateTime(timeZone).date
        }
}
