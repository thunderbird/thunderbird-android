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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class WeblateClient(
    private val token: String,
    private val config: WeblateConfig = WeblateConfig(),
    private val logLevel: LogLevel = LogLevel.INFO,
    private val client: HttpClient = createClient(logLevel),
) {

    fun loadComponents(): List<Component> {
        if (config.cacheEnabled) {
            try {
                val cached = readComponentsFromCache()
                if (cached.isNotEmpty()) {
                    return cached
                }
            } catch (_: Exception) {
            }
        }

        val components = mutableListOf<Component>()
        var page = 1
        var hasNextPage = true

        while (hasNextPage) {
            val componentPage = loadComponentPage(page)
            components.addAll(componentPage.results)

            hasNextPage = componentPage.next != null
            page++
        }

        if (config.cacheEnabled) {
            try {
                writeComponentsToCache(components)
            } catch (_: Exception) {
            }
        }

        return components
    }

    fun patchComponent(url: String, patch: ComponentPatch): Boolean {
        return runBlocking {
            val response = client.patch(url) {
                header(HttpHeaders.Authorization, "Token $token")
                contentType(ContentType.Application.Json)
                setBody(patch)
            }

            response.status.value in SUCCESS
        }
    }

    fun createComponent(create: ComponentCreate): Boolean {
        return runBlocking {
            val url = "${config.baseUrl}projects/${config.projectName}/components/"
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Token $token")
                contentType(ContentType.Application.Json)
                setBody(create)
            }

            response.status.value in SUCCESS
        }
    }

    private fun loadComponentPage(page: Int): ComponentResponse {
        return runBlocking {
            client.get(config.componentsUrl(page)) {
                headers {
                    config.getDefaultHeaders(token).forEach { (key, value) -> append(key, value) }
                }
            }.body()
        }
    }

    private fun readComponentsFromCache(): List<Component> {
        val cacheFile = File(CACHE_FILE_PATH)
        if (cacheFile.exists()) {
            return json.decodeFromString<List<Component>>(cacheFile.readText())
        }
        return emptyList()
    }

    private fun writeComponentsToCache(components: List<Component>) {
        if (components.isEmpty()) return

        val cacheFile = File(CACHE_FILE_PATH)
        cacheFile.parentFile.mkdirs()
        cacheFile.writeText(json.encodeToString(components))
    }

    private companion object {
        val SUCCESS = 200..299

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }

        fun createClient(logLevel: LogLevel): HttpClient {
            return HttpClient(CIO) {
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = logLevel
                }
                install(ContentNegotiation) {
                    json(json)
                }
            }
        }

        private fun WeblateConfig.componentsUrl(page: Int) = "${baseUrl}projects/$projectName/components/?page=$page"

        private const val CACHE_FILE_PATH = "cli/weblate-cli/weblate-components-cache.json"
    }
}
