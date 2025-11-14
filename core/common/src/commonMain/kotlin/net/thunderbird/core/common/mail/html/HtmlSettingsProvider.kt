package net.thunderbird.core.common.mail.html

fun interface HtmlSettingsProvider {
    fun create(): HtmlSettings
}
