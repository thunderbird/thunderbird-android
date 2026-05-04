package net.thunderbird.cli.weblate.api

/**
 * Configuration for Weblate API
 *
 * @property baseUrl Base URL of the Weblate API
 * @property projectName Name of the Weblate project
 * @property defaultComponent Default component to use for translations
 * @property cacheEnabled Whether caching is enabled for API responses
 */
data class WeblateConfig(
    val baseUrl: String = "https://hosted.weblate.org/api/",
    val projectName: String = "thunderbird",
    val defaultComponent: String = "app-common",
    private val defaultHeaders: Map<String, String> = mapOf(
        "Accept" to "application/json",
        "Authorization" to "Token $PLACEHOLDER_TOKEN",
    ),
    val cacheEnabled: Boolean = false,
) {
    fun getDefaultHeaders(token: String): List<Pair<String, String>> =
        defaultHeaders.mapValues { it.value.replace(PLACEHOLDER_TOKEN, token) }
            .map { (key, value) -> key to value }

    private companion object {
        const val PLACEHOLDER_TOKEN = "{weblate_token}"
    }
}
