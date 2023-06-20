package app.k9mail.dev

import app.k9mail.core.common.oauth.OAuthProvider
import app.k9mail.core.common.oauth.OAuthProviderSettings
import com.fsck.k9.BuildConfig

fun createOAuthProviderSettings(): OAuthProviderSettings {
    return OAuthProviderSettings(
        applicationId = BuildConfig.APPLICATION_ID,
        clientIds = mapOf(
            OAuthProvider.AOL to
                "dj0yJmk9dUNqYXZhYWxOYkdRJmQ9WVdrOU1YQnZVRFZoY1ZrbWNHbzlNQT09JnM9Y29uc3VtZXJzZWNyZXQmc3Y9MCZ4PWIw",
            OAuthProvider.GMAIL to
                "262622259280-hhmh92rhklkg2k1tjil69epo0o9a12jm.apps.googleusercontent.com",
            OAuthProvider.MICROSOFT to
                "e647013a-ada4-4114-b419-e43d250f99c5",
            OAuthProvider.YAHOO to
                "dj0yJmk9aHNUb3d2MW5TQnpRJmQ9WVdrOWVYbHpaRWM0YkdnbWNHbzlNQT09JnM9Y29uc3VtZXJzZWNyZXQmc3Y9MCZ4PWIz",
        ),
        redirectUriIds = mapOf(
            OAuthProvider.MICROSOFT to
                "Dx8yUsuhyU3dYYba1aA16Wxu5eM%3D",
        ),
    )
}
