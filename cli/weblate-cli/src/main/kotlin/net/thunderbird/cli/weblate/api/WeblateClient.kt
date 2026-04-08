package net.thunderbird.cli.weblate.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class WeblateClient(
    private val client: HttpClient = createClient(),
    private val config: WeblateConfig = WeblateConfig(),
) {

    fun loadComponents(token: String): List<Component> {
        val components = mutableListOf<Component>()
        var page = 1
        var hasNextPage = true

        while (hasNextPage) {
            val componentPage = loadComponentPage(token, page)
            components.addAll(componentPage.results)

            hasNextPage = componentPage.next != null
            page++
        }

        return components
    }

    fun patchComponent(token: String, url: String, patch: ComponentPatch): Boolean {
        var success = false

        runBlocking {
            val response = client.patch(url) {
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Authorization, "Token $token")
                contentType(ContentType.Application.Json)
                setBody(patch)
            }

            success = response.status.value in SUCCESS
        }

        return success
    }

    private fun loadComponentPage(token: String, page: Int): ComponentResponse {
        val componentResponse: ComponentResponse

        runBlocking {
            componentResponse = client.get(config.componentsUrl(page)) {
                headers {
                    config.getDefaultHeaders(token).forEach { (key, value) -> append(key, value) }
                }
            }.body()
        }

        return componentResponse
    }

    private companion object {
        val SUCCESS = 200..299

        fun createClient(): HttpClient {
            return HttpClient(CIO) {
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.INFO
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

        private fun WeblateConfig.componentsUrl(page: Int) = "${baseUrl}projects/$projectName/components/?page=$page"
    }
}
