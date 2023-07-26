package com.fsck.k9.activity.setup

@Deprecated("Could be removed with the legacy account setup UI")
object GoogleOAuthHelper {
    fun isGoogle(hostname: String): Boolean {
        return hostname.lowercase().endsWith(".gmail.com") ||
            hostname.lowercase().endsWith(".googlemail.com")
    }
}
