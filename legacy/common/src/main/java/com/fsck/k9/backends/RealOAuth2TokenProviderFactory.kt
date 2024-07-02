package com.fsck.k9.backends

import android.content.Context
import com.fsck.k9.mail.oauth.AuthStateStorage
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.oauth.OAuth2TokenProviderFactory

class RealOAuth2TokenProviderFactory(
    private val context: Context,
) : OAuth2TokenProviderFactory {
    override fun create(authStateStorage: AuthStateStorage): OAuth2TokenProvider {
        return RealOAuth2TokenProvider(context, authStateStorage)
    }
}
