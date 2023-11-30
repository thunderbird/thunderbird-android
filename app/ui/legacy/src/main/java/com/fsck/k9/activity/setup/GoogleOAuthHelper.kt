package com.fsck.k9.activity.setup

@Deprecated("Could be removed when import uses the new oauth flow")
object GoogleOAuthHelper {
    fun isGoogle(hostname: String): Boolean {
        return hostname.lowercase().endsWith(".gmail.com") ||
            hostname.lowercase().endsWith(".googlemail.com")
    }
}
