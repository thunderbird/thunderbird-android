package app.k9mail.feature.preview.config

import app.k9mail.core.common.oauth.OAuthProvider
import app.k9mail.core.common.oauth.OAuthProviderSettings
import app.k9mail.feature.preview.BuildConfig

fun createOAuthProviderSettings(): OAuthProviderSettings {
    return OAuthProviderSettings(
        applicationId = BuildConfig.APPLICATION_ID,
        clientIds = mapOf(
            OAuthProvider.AOL to
                "dj0yJmk9cHYydkJkTUxHcXlYJmQ9WVdrOWVHZHhVVXN4VVV3bWNHbzlNQT09JnM9Y29uc3VtZXJzZWNyZXQmc3Y9MCZ4PTdm",
            OAuthProvider.GMAIL to
                "262622259280-5qb3vtj68d5dtudmaif4g9vd3cpar8r3.apps.googleusercontent.com",
            OAuthProvider.MICROSOFT to
                "e647013a-ada4-4114-b419-e43d250f99c5",
            OAuthProvider.YAHOO to
                "dj0yJmk9ejRCRU1ybmZjQlVBJmQ9WVdrOVVrZEViak4xYmxZbWNHbzlNQT09JnM9Y29uc3VtZXJzZWNyZXQmc3Y9MCZ4PTZj",
        ),
        redirectUriIds = mapOf(
            OAuthProvider.MICROSOFT to
                "VZF2DYuLYAu4TurFd6usQB2JPts%3D",
        ),
    )
}
