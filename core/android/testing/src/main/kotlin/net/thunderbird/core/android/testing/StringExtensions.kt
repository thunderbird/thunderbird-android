package net.thunderbird.core.android.testing

fun String.removeNewlines(): String = replace("([\\r\\n])".toRegex(), "")
