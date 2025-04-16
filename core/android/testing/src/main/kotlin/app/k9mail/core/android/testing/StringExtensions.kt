package app.k9mail.core.android.testing

fun String.removeNewlines(): String = replace("([\\r\\n])".toRegex(), "")
