package app.k9mail.core.common.oauth

fun interface OAuthConfigurationFactory {
    fun createConfigurations(): Map<List<String>, OAuthConfiguration>
}
