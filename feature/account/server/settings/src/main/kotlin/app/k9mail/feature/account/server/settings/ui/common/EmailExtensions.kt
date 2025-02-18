package app.k9mail.feature.account.server.settings.ui.common

fun String.toInvalidEmailDomain() = ".${this.substringAfter("@")}"
