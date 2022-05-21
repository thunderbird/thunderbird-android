package com.fsck.k9.auth

import com.fsck.k9.BuildConfig
import com.fsck.k9.activity.setup.OAuthCredentials

class K9OAuthCredentials : OAuthCredentials {
    override val gmailClientId: String
        get() = BuildConfig.OAUTH_GMAIL_CLIENT_ID
}
