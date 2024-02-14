package app.k9mail.feature.account.setup.domain.entity

import kotlinx.collections.immutable.toImmutableList

@Suppress("MagicNumber")
enum class EmailDisplayCount(
    val count: Int,
) {
    MESSAGES_10(10),
    MESSAGES_25(25),
    MESSAGES_50(50),
    MESSAGES_100(100),
    MESSAGES_250(250),
    MESSAGES_500(500),
    MESSAGES_1000(1000),
    ;

    companion object {
        val DEFAULT = MESSAGES_100
        fun all() = entries.toImmutableList()

        fun fromCount(count: Int): EmailDisplayCount {
            return all().find { it.count == count } ?: DEFAULT
        }
    }
}
