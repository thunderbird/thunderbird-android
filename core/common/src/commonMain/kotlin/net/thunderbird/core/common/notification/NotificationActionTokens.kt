package net.thunderbird.core.common.notification

/**
 * Token strings used to persist notification action ordering.
 */
object NotificationActionTokens {
    const val REPLY = "reply"
    const val MARK_AS_READ = "mark_as_read"
    const val DELETE = "delete"
    const val STAR = "star"
    const val ARCHIVE = "archive"
    const val SPAM = "spam"

    const val DEFAULT_ORDER = "$REPLY,$MARK_AS_READ,$DELETE,$STAR,$ARCHIVE,$SPAM"
}
