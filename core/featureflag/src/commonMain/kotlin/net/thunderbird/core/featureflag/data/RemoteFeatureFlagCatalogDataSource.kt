package net.thunderbird.core.featureflag.data

import com.eygraber.uri.Uri
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.io.decodeFromSource
import net.thunderbird.core.featureflag.data.RemoteCatalogException.Code
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigStore
import net.thunderbird.core.featureflag.data.configstore.RemoteCatalogCacheMetadata
import net.thunderbird.core.featureflag.data.configstore.safeUpdate
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.file.FileSystemManager
import net.thunderbird.core.logging.Logger

@OptIn(ExperimentalSerializationApi::class)
class RemoteFeatureFlagCatalogDataSource(
    private val url: String,
    private val cacheFileUri: Uri,
    private val logger: Logger,
    private val configStore: FeatureFlagConfigStore,
    private val fileSystemManager: FileSystemManager,
    private val json: Json,
    private val httpClient: HttpClient = HttpClient(engineFactory = createHttpClientEngine()) {
        install(ContentNegotiation) {
            json(json)
        }
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FeatureFlagCatalogDataSource {
    private val catalog = MutableStateFlow<FeatureFlagCatalog?>(null)

    override fun observe(): Flow<FeatureFlagCatalog> = catalog.mapNotNull { it }

    override suspend fun load(): FeatureFlagCatalog? {
        return catalog.value ?: try {
            val config = configStore.config.first()
            val remoteCatalogConfig = config.remoteCatalogConfig
            if (!remoteCatalogConfig.enabled) {
                throw RemoteCatalogException(code = Code.RemoteCatalogUserDisabled)
            }

            val cacheMetadata = httpClient.fetchCacheMetadata(url)
            val result = if (remoteCatalogConfig.cacheMetadata != cacheMetadata) {
                downloadAndCache(url, cacheMetadata)
            } else {
                readFromCache()
            }
            catalog.update { result }
            result
        } catch (e: RemoteCatalogException) {
            when (e.code) {
                Code.RemoteCatalogUserDisabled -> logger.debug(throwable = e) {
                    "$LOG_PREFIX Skipping remote catalog; user disabled."
                }

                Code.CantWriteCacheFile -> logger.debug(throwable = e) { "$LOG_PREFIX Failed to write cache file." }

                Code.CantReadCacheFile -> logger.debug(throwable = e) { "$LOG_PREFIX Failed to read cache file." }
            }
            null
        } catch (e: JsonConvertException) {
            logger.error(throwable = e) { "$LOG_PREFIX Failed to convert JSON to FeatureFlagCatalog" }
            null
        } catch (e: SerializationException) {
            logger.error(throwable = e) { "$LOG_PREFIX Failed to convert JSON to FeatureFlagCatalog" }
            null
        } catch (e: IOException) {
            logger.error(throwable = e) { "$LOG_PREFIX Failed to fetch Feature Flag Remote Catalog" }
            null
        }
    }

    /**
     * Issues a HEAD request to [url] and extracts the [RemoteCatalogCacheMetadata] from the
     * response headers.
     *
     * @throws RemoteCatalogException when the response status is not a success.
     */
    private suspend fun HttpClient.fetchCacheMetadata(url: String): RemoteCatalogCacheMetadata? {
        logger.verbose { "$LOG_PREFIX Fetching cache metadata" }
        val response = head(url)
        return if (response.status == HttpStatusCode.OK) {
            logger.verbose { "$LOG_PREFIX Cache read with success. Response = $response" }
            RemoteCatalogCacheMetadata(
                eTag = response.headers["ETag"],
                lastModified = response.headers["Last-Modified"],
                contentLength = response.headers["Content-Length"]?.toLongOrNull(),
            )
        } else {
            logger.warn {
                "$LOG_PREFIX HEAD request to '$url' returned status ${response.status}"
            }
            null
        }
    }

    private suspend fun downloadAndCache(url: String, cacheMetadata: RemoteCatalogCacheMetadata?): FeatureFlagCatalog =
        withContext(ioDispatcher) {
            logger.verbose { "$LOG_PREFIX Starting Remote Feature Flag catalog download" }
            val response = httpClient.get(urlString = url)

            val channel = response.bodyAsChannel()
            val contentLength = response.contentLength()?.toInt() ?: 0
            val buffer = Buffer()
            val chunk = ByteArray(size = 4_096) // 4 KB
            var totalRead = 0
            while (true) {
                val read = channel.readAvailable(buffer = chunk)
                if (read < 0) break
                buffer.write(source = chunk, startIndex = 0, endIndex = read)
                totalRead += read
                val progress = if (contentLength == 0) 0.0 else totalRead / contentLength.toDouble()
                logger.debug { "$LOG_PREFIX Downloading $progress%" }
            }

            val fileBuffer = buffer.copy()
            logger.verbose { "$LOG_PREFIX Caching Remote Feature Flag catalog at $cacheFileUri" }
            val sink = fileSystemManager.openSink(uri = cacheFileUri)
            if (sink == null) {
                logger.warn {
                    "$LOG_PREFIX Can't write the remote catalog cache. Sink could not be opened."
                }
                throw RemoteCatalogException(code = Code.CantWriteCacheFile)
            }
            sink.buffered().use { sink ->
                sink.write(fileBuffer, fileBuffer.size)
            }

            logger.verbose { "$LOG_PREFIX Caching remote catalog metadata" }
            configStore.safeUpdate { config ->
                config.copy(
                    remoteCatalogConfig = config.remoteCatalogConfig.copy(
                        cacheMetadata = cacheMetadata,
                    ),
                )
            }

            val catalog = json.decodeFromSource<FeatureFlagCatalog>(buffer)
            catalog
        }

    private suspend fun readFromCache(): FeatureFlagCatalog = withContext(ioDispatcher) {
        logger.verbose { "$LOG_PREFIX Reading Remote Feature Flag catalog from cache at $cacheFileUri" }
        val source = fileSystemManager.openSource(cacheFileUri)
        if (source == null) {
            logger.warn { "$LOG_PREFIX Can't read cached remote feature flag catalog." }
            throw RemoteCatalogException(code = Code.CantReadCacheFile)
        }
        json.decodeFromSource<FeatureFlagCatalog>(source.buffered())
    }

    companion object {
        private const val LOG_PREFIX = "[feature-flag][remote-data-sourece]"
    }
}

expect fun createHttpClientEngine(): HttpClientEngineFactory<HttpClientEngineConfig>
