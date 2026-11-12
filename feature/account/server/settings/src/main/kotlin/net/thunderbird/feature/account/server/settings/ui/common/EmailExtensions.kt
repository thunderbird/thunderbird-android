package net.thunderbird.feature.account.server.settings.ui.common

fun String.toInvalidEmailDomain() = ".${this.substringAfter("@")}"
