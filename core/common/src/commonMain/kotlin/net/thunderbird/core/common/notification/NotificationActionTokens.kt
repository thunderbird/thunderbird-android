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

    fun <T> normalizeOrder(
        persistedTokens: List<String>,
        supportedActions: List<Pair<String, T>>,
    ): List<T> {
        val supportedByToken = supportedActions.toMap()
        val normalized = LinkedHashSet<T>()

        for (token in persistedTokens) {
            supportedByToken[token]?.let { normalized.add(it) }
        }

        for ((_, action) in supportedActions) {
            normalized.add(action)
        }

        return normalized.toList()
    }

    fun serializeOrder(tokens: List<String>): String = tokens.joinToString(separator = ",")
}
