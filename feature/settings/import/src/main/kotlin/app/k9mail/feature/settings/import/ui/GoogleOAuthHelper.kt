package app.k9mail.feature.settings.import.ui

internal object GoogleOAuthHelper {
    fun isGoogle(hostname: String): Boolean {
        return hostname.lowercase().endsWith(".gmail.com") ||
            hostname.lowercase().endsWith(".googlemail.com")
    }
}
