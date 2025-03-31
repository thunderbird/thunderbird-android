package app.k9mail.legacy.account

fun interface AccountDefaultsProvider {
    fun applyDefaults(account: LegacyAccount)

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

        const val NO_OPENPGP_KEY: Long = 0

        const val UNASSIGNED_ACCOUNT_NUMBER = -1
    }
}
