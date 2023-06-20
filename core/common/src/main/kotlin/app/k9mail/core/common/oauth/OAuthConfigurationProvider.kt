package app.k9mail.core.common.oauth

interface OAuthConfigurationProvider {

    fun getConfiguration(hostname: String): OAuthConfiguration?

    fun isGoogle(hostname: String): Boolean
}
