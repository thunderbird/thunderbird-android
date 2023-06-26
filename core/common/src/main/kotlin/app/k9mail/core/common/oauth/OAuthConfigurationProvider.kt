package app.k9mail.core.common.oauth

fun interface OAuthConfigurationProvider {
    fun getConfiguration(hostname: String): OAuthConfiguration?
}
