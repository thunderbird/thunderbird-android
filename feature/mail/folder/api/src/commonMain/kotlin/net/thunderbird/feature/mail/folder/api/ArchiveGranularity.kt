package net.thunderbird.feature.mail.folder.api

enum class ArchiveGranularity {
    SINGLE_ARCHIVE_FOLDER,
    PER_YEAR_ARCHIVE_FOLDERS,
    PER_MONTH_ARCHIVE_FOLDERS,
    ;

    companion object {
        /**
         * Default archive granularity for new accounts.
         * Matches Thunderbird Desktop default (value 1 = yearly).
         */
        val DEFAULT = PER_YEAR_ARCHIVE_FOLDERS

        /**
         * Default for existing accounts during migration.
         * Maintains backward compatibility with current single folder behavior.
         */
        val MIGRATION_DEFAULT = SINGLE_ARCHIVE_FOLDER
    }
}
