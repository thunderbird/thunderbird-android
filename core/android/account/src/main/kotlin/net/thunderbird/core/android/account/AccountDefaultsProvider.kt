package net.thunderbird.core.android.account

import net.thunderbird.core.preference.storage.Storage

interface AccountDefaultsProvider {
    /**
     * Apply default values to the account.
     *
     * This method should only be called when creating a new account.
     */
    fun applyDefaults(account: LegacyAccountDto)

    /**
     * Apply any additional default values to the account.
     *
     * This method should be called when updating an existing account.
     */
    fun applyOverwrites(account: LegacyAccountDto, storage: Storage)

    companion object {
        const val DEFAULT_MAXIMUM_AUTO_DOWNLOAD_MESSAGE_SIZE = 131072

        @JvmStatic
        val DEFAULT_MESSAGE_FORMAT = MessageFormat.HTML

        const val DEFAULT_MESSAGE_FORMAT_AUTO = false
        const val DEFAULT_MESSAGE_READ_RECEIPT = false
        const val DEFAULT_QUOTED_TEXT_SHOWN = true
        const val DEFAULT_QUOTE_PREFIX = ">"

        @JvmStatic
        val DEFAULT_QUOTE_STYLE = QuoteStyle.PREFIX

        const val DEFAULT_REMOTE_SEARCH_NUM_RESULTS = 25
        const val DEFAULT_REPLY_AFTER_QUOTE = false
        const val DEFAULT_RINGTONE_URI = "content://settings/system/notification_sound"
        const val DEFAULT_SORT_ASCENDING = false

        @JvmStatic
        val DEFAULT_SORT_TYPE = SortType.SORT_DATE

        const val DEFAULT_STRIP_SIGNATURE = true

        const val DEFAULT_SYNC_INTERVAL = 60

        /**
         * Specifies how many messages will be shown in a folder by default. This number is set
         * on each new folder and can be incremented with "Load more messages..." by the
         * VISIBLE_LIMIT_INCREMENT
         */
        const val DEFAULT_VISIBLE_LIMIT = 25

        const val NO_OPENPGP_KEY: Long = 0

        const val UNASSIGNED_ACCOUNT_NUMBER = -1

        // TODO : Remove once storage is migrated to new format
        const val COLOR = 0x0099CC
    }
}
