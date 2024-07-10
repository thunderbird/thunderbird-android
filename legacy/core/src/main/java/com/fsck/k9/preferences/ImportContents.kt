package com.fsck.k9.preferences

/**
 * Class to list the contents of an import file/stream.
 *
 * @see SettingsImporter.getImportStreamContents
 */
data class ImportContents(
    /**
     * True, if the import file contains global settings.
     */
    val globalSettings: Boolean,

    /**
     * The list of accounts found in the import file.
     */
    val accounts: List<AccountDescription>,
)
