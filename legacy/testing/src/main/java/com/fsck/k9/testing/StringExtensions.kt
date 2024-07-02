package com.fsck.k9.testing

fun String.removeNewlines(): String = replace("([\\r\\n])".toRegex(), "")
