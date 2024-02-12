package net.thunderbird.cli.translation.net

/**
 * Configuration for Weblate API
 *
 * @property baseUrl Base URL of the Weblate API
 * @property projectName Name of the Weblate project
 * @property defaultComponent Default component to use for translations
 */
data class WeblateConfig(
    val baseUrl: String = "https://hosted.weblate.org/api/",
    val projectName: String = "tb-android",
    val defaultComponent: String = "app-strings",
    private val defaultHeaders: Map<String, String> = mapOf(
        "Accept" to "application/json",
        "Authorization" to "Token $PLACEHOLDER_TOKEN",
    ),
) {
    fun getDefaultHeaders(token: String): List<Pair<String, String>> =
        defaultHeaders.mapValues { it.value.replace(PLACEHOLDER_TOKEN, token) }
            .map { (key, value) -> key to value }

    private companion object {
        const val PLACEHOLDER_TOKEN = "{weblate_token}"
    }
}
