@file:JvmName("CrLfConverter")

package com.fsck.k9.helper

fun String?.toLf() = this?.replace("\r\n", "\n")

fun CharSequence?.toLf() = this?.toString()?.replace("\r\n", "\n")

fun CharSequence?.toCrLf() = this?.toString()?.replace("\n", "\r\n")
