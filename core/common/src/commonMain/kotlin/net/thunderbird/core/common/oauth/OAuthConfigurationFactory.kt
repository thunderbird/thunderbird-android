package net.thunderbird.core.common.oauth

fun interface OAuthConfigurationFactory {
    fun createConfigurations(): Map<List<String>, OAuthConfiguration>
}
