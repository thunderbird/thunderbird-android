package net.thunderbird.cli.translation.net

import org.http4k.client.OkHttp
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.format.Moshi.auto

class WeblateClient(
    private val client: HttpHandler = OkHttp(),
    private val config: WeblateConfig = WeblateConfig(),
) {
    fun loadLanguages(token: String): List<Language> {
        val request = Request(Method.GET, Uri.of(config.projectsLanguagesUrl()))
            .headers(config.getDefaultHeaders(token))

        val response = client(request)
        val lens = Body.auto<Array<Language>>().toLens()

        return lens(response).toList()
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
        val request = Request(Method.GET, Uri.of(config.componentsTranslationsUrl(page)))
            .headers(config.getDefaultHeaders(token))

        val response = client(request)
        val lens = Body.auto<TranslationResponse>().toLens()

        return lens(response)
    }

    private companion object {
        private fun WeblateConfig.projectsLanguagesUrl() =
            "${baseUrl}projects/$projectName/languages/"

        private fun WeblateConfig.componentsTranslationsUrl(page: Int) =
            "${baseUrl}components/$projectName/$defaultComponent/translations/?page=$page"
    }
}
