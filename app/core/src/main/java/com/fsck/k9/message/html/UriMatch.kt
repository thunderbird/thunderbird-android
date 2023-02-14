package com.fsck.k9.message.html

data class UriMatch(
    val startIndex: Int,
    val endIndex: Int,
    val uri: CharSequence,
)
