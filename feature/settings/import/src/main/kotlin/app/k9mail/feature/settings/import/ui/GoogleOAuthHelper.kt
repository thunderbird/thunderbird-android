package app.k9mail.feature.settings.import.ui

@Deprecated("Could be removed when import uses the new oauth flow")
internal object GoogleOAuthHelper {
    fun isGoogle(hostname: String): Boolean {
        return hostname.lowercase().endsWith(".gmail.com") ||
            hostname.lowercase().endsWith(".googlemail.com")
    }
}
