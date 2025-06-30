package com.fsck.k9.mail.testing

fun String.crlf() = replace("\n", "\r\n")

fun String.removeLineBreaks() = replace(Regex("""\r|\n"""), "")
