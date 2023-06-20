package com.fsck.k9.activity.setup

object GoogleOAuthHelper {
    fun isGoogle(hostname: String): Boolean {
        return hostname.lowercase().endsWith(".gmail.com") ||
            hostname.lowercase().endsWith(".googlemail.com")
    }
}
