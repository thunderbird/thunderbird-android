package net.thunderbird.core.android.account

enum class SortType(val isDefaultAscending: Boolean) {
    SORT_DATE(false),
    SORT_ARRIVAL(false),
    SORT_SUBJECT(true),
    SORT_SENDER(true),
    SORT_UNREAD(true),
    SORT_FLAGGED(true),
    SORT_ATTACHMENT(true),
    SORT_SIZE(false),
}
