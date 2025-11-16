package com.fsck.k9.controller

import com.fsck.k9.mailstore.LocalMessage
import java.util.Calendar
import java.util.Date
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.mail.folder.api.ArchiveGranularity

/**
 * Resolves the destination folder ID for archiving messages based on account's archive granularity setting.
 *
 * TODO: Add folder creation/lookup logic for yearly and monthly subfolders
 */
internal class ArchiveFolderResolver {

    /**
     * Determines the target archive folder ID for a message based on account settings.
     *
     * @param account The account containing archive settings
     * @param message The message to archive (used to extract date for yearly/monthly routing)
     * @return The folder ID to archive to, or null if no archive folder configured
     */
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
                // TODO: Implement yearly folder resolution
                // Extract year from message date, find/create folder like "Archive/2025"
                baseFolderId
            }

            ArchiveGranularity.PER_MONTH_ARCHIVE_FOLDERS -> {
                // TODO: Implement monthly folder resolution
                // Extract year/month from message date, find/create folder like "Archive/2025/11"
                baseFolderId
            }
        }
    }

    /**
     * Extracts the date from a message for archive folder routing.
     * Prefers internalDate (when received), falls back to sentDate.
     */
    private fun getMessageDate(message: LocalMessage): Date {
        return message.internalDate ?: message.sentDate ?: Date()
    }

    /**
     * Extracts year from a date.
     */
    private fun getYear(date: Date): Int {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar.get(Calendar.YEAR)
    }

    /**
     * Extracts month from a date (1-12).
     */
    private fun getMonth(date: Date): Int {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
    }
}
