package com.fsck.k9.backend.gmailapi

import net.thunderbird.core.common.mail.Flag

internal object GmailSearchQuery {
    fun build(
        query: String?,
        requiredFlags: Set<Flag>?,
        forbiddenFlags: Set<Flag>?,
        performFullTextSearch: Boolean,
    ): String? {
        val terms = buildList {
            val cleanedQuery = query?.replace(Regex("[\\r\\n\\u0000-\\u001F]"), " ")?.trim()
            if (!cleanedQuery.isNullOrEmpty()) {
                val quotedQuery = "\"${cleanedQuery.replace("\\", "\\\\").replace("\"", "\\\"")}\""
                add(
                    if (performFullTextSearch) {
                        quotedQuery
                    } else {
                        listOf("subject", "from", "to", "cc", "bcc")
                            .joinToString(separator = " ", prefix = "{", postfix = "}") { "$it:$quotedQuery" }
                    },
                )
            }
            requiredFlags.orEmpty().forEach { add(flagTerm(it, forbidden = false)) }
            forbiddenFlags.orEmpty().forEach { add(flagTerm(it, forbidden = true)) }
        }
        return terms.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    private fun flagTerm(flag: Flag, forbidden: Boolean): String {
        val term = when (flag) {
            Flag.SEEN -> if (forbidden) "is:unread" else "is:read"
            Flag.FLAGGED -> if (forbidden) "-is:starred" else "is:starred"
            Flag.DRAFT -> if (forbidden) "-is:draft" else "is:draft"
            Flag.DELETED -> if (forbidden) "-in:trash" else "in:trash"
            else -> throw UnsupportedOperationException("Gmail remote search does not support the $flag flag")
        }
        return term
    }
}
