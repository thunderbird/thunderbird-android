package net.thunderbird.core.common.oauth

fun interface OAuthConfigurationProvider {
    fun getConfiguration(hostname: String): OAuthConfiguration?
}
