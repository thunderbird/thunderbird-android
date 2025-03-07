package net.thunderbird.cli.translation.net

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class WeblateClient(
    private val client: HttpClient = createClient(),
    private val config: WeblateConfig = WeblateConfig(),
) {
    fun loadLanguages(token: String): List<Language> {
        val languages: List<Language>

        runBlocking {
            languages = client.get(config.projectsLanguagesUrl()) {
                headers {
                    config.getDefaultHeaders(token).forEach { (key, value) -> append(key, value) }
                }
            }.body()
        }

        return languages
    }

    fun loadTranslations(token: String): List<Translation> {
        val translations = mutableListOf<Translation>()
        var page = 1
        var hasNextPage = true

        while (hasNextPage) {
            val translationPage = loadTranslationPage(token, page)
            translations.addAll(translationPage.results)

            hasNextPage = translationPage.next != null
            page++
        }

        return translations
    }

    private fun loadTranslationPage(token: String, page: Int): TranslationResponse {
        val translationResponse: TranslationResponse

        runBlocking {
            translationResponse = client.get(config.componentsTranslationsUrl(page)) {
                headers {
                    config.getDefaultHeaders(token).forEach { (key, value) -> append(key, value) }
                }
            }.body()
        }

        return translationResponse
    }

    private companion object {
        fun createClient(): HttpClient {
            return HttpClient(CIO) {
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.NONE
                }
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                        },
                    )
                }
            }
        }

        private fun WeblateConfig.projectsLanguagesUrl() =
            "${baseUrl}projects/$projectName/languages/"

        private fun WeblateConfig.componentsTranslationsUrl(page: Int) =
            "${baseUrl}components/$projectName/$defaultComponent/translations/?page=$page"
    }
}
