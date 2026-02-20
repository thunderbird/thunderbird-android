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

    val DEFAULT_ORDER: List<String> = listOf(REPLY, MARK_AS_READ, DELETE, STAR, ARCHIVE, SPAM)

    fun parseOrder(raw: String): List<String> {
        return raw
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun serializeOrder(tokens: List<String>): String = tokens.joinToString(separator = ",")
}
