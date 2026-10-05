package net.thunderbird.core.featureflag.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import com.eygraber.uri.Uri
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigData
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigStore
import net.thunderbird.core.featureflag.data.configstore.RemoteCatalogConfig
import net.thunderbird.core.featureflag.model.AppVariantOverridesRawType
import net.thunderbird.core.featureflag.model.BaseAppVariantOverrides
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.featureflag.model.FlagRegistryOverride
import net.thunderbird.core.featureflag.serialization.FlagRegistryOverrideSerializer
import net.thunderbird.core.file.FileSystemManager
import net.thunderbird.core.file.WriteMode
import net.thunderbird.core.logging.testing.TestLogger

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteFeatureFlagCatalogDataSourceTest {

    @Test
    fun `observe should load the remote catalog when it is enabled after starting disabled`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val configStore = FakeFeatureFlagConfigStore(remoteCatalogEnabled = false)
            val engine = FakeRemoteCatalogEngine()
            val testSubject = createTestSubject(configStore = configStore, engine = engine)

            testSubject.observe().test {
                assertThat(awaitItem()).isNull()

                // Act
                configStore.setRemoteCatalogEnabled(enabled = true)

                // Assert
                assertThat(awaitItem()).isNotNull().prop(FeatureFlagCatalog::version).isEqualTo(CATALOG_VERSION)
                assertThat(engine.downloadCount).isEqualTo(1)
            }
        }

    @Test
    fun `observe should keep emitting the cached catalog when the remote catalog is disabled`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val configStore = FakeFeatureFlagConfigStore(remoteCatalogEnabled = true)
            val engine = FakeRemoteCatalogEngine()
            val testSubject = createTestSubject(configStore = configStore, engine = engine)

            testSubject.observe().test {
                assertThat(awaitItem()).isNotNull()

                // Act
                configStore.setRemoteCatalogEnabled(enabled = false)

                // Assert
                assertThat(awaitItem()).isNotNull().prop(FeatureFlagCatalog::version).isEqualTo(CATALOG_VERSION)
                assertThat(engine.downloadCount).isEqualTo(1)
            }
        }

    @Test
    fun `observe should emit the cached catalog without downloading again when re-enabled`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val configStore = FakeFeatureFlagConfigStore(remoteCatalogEnabled = true)
            val engine = FakeRemoteCatalogEngine()
            val testSubject = createTestSubject(configStore = configStore, engine = engine)
            testSubject.load()

            testSubject.observe().test {
                assertThat(awaitItem()).isNotNull()
                configStore.setRemoteCatalogEnabled(enabled = false)
                assertThat(awaitItem()).isNotNull()

                // Act
                configStore.setRemoteCatalogEnabled(enabled = true)

                // Assert
                assertThat(awaitItem()).isNotNull().prop(FeatureFlagCatalog::version).isEqualTo(CATALOG_VERSION)
                assertThat(engine.downloadCount).isEqualTo(1)
            }
        }

    @Test
    fun `observe and load should download the catalog only once when running concurrently`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val engine = FakeRemoteCatalogEngine()
            val testSubject = createTestSubject(
                configStore = FakeFeatureFlagConfigStore(remoteCatalogEnabled = true),
                engine = engine,
            )

            // Act
            backgroundScope.launch { testSubject.observe().collect {} }
            val catalog = testSubject.load()

            // Assert
            assertThat(catalog).isNotNull()
            assertThat(engine.downloadCount).isEqualTo(1)
        }

    private fun TestScope.createTestSubject(
        configStore: FeatureFlagConfigStore,
        engine: FakeRemoteCatalogEngine,
    ): RemoteFeatureFlagCatalogDataSource {
        val json = Json {
            serializersModule = SerializersModule {
                contextual(
                    kClass = FlagRegistryOverride::class,
                    serializer = FlagRegistryOverrideSerializer(
                        k9Factory = { wrapper -> FakeRemoteAppVariantOverrides(wrapper) },
                        thunderbirdFactory = { wrapper -> FakeRemoteAppVariantOverrides(wrapper) },
                    ),
                )
            }
        }
        return RemoteFeatureFlagCatalogDataSource(
            url = CATALOG_URL,
            cacheFileUri = Uri.parse(CACHE_FILE_URI),
            logger = TestLogger(),
            configStore = configStore,
            fileSystemManager = FakeFileSystemManager(),
            json = json,
            httpClient = HttpClient(engine.mockEngine) {
                install(ContentNegotiation) {
                    json(json)
                }
            },
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
    }

    private companion object {
        const val CATALOG_URL = "https://example.com/catalog.json"
        const val CACHE_FILE_URI = "file:///cache/catalog.json"
        const val CATALOG_VERSION = "2026-10-01.1"
    }
}

/**
 * Serves the remote catalog, answering `HEAD` with cache metadata and `GET` with the catalog body,
 * and counts the downloads so tests can assert the catalog is not fetched more than needed.
 */
private class FakeRemoteCatalogEngine {
    var downloadCount: Int = 0
        private set

    val mockEngine = MockEngine { request ->
        if (request.method == HttpMethod.Get) {
            downloadCount++
        }
        respond(
            content = CATALOG_BODY,
            status = HttpStatusCode.OK,
            headers = headersOf(
                HttpHeaders.ContentType to listOf("application/json"),
                HttpHeaders.ETag to listOf("\"catalog-etag\""),
            ),
        )
    }

    private companion object {
        // language=json
        const val CATALOG_BODY = """
            {
              "version": "2026-10-01.1",
              "flags": [
                { "key": "archive_marks_as_read", "default": true }
              ],
              "overrides": {
                "thunderbird": { "debug": {}, "release": {} },
                "k9": { "debug": {}, "release": {} }
              }
            }
        """
    }
}

private class FakeFeatureFlagConfigStore(
    remoteCatalogEnabled: Boolean,
) : FeatureFlagConfigStore {
    private val state = MutableStateFlow(
        FeatureFlagConfigData(remoteCatalogConfig = RemoteCatalogConfig(enabled = remoteCatalogEnabled)),
    )

    override val config: Flow<FeatureFlagConfigData> = state

    fun setRemoteCatalogEnabled(enabled: Boolean) {
        state.update { it.copy(remoteCatalogConfig = it.remoteCatalogConfig.copy(enabled = enabled)) }
    }

    override suspend fun update(transform: (FeatureFlagConfigData?) -> FeatureFlagConfigData) {
        state.update { current -> transform(current) }
    }

    override suspend fun clear() {
        state.value = FeatureFlagConfigData.DEFAULT
    }
}

/** Keeps written files in memory, so the downloaded catalog can be cached and read back. */
private class FakeFileSystemManager : FileSystemManager {
    private val files = mutableMapOf<Uri, Buffer>()

    override fun openSink(uri: Uri, mode: WriteMode): RawSink = Buffer().also { files[uri] = it }

    override fun openSource(uri: Uri): RawSource? = files[uri]?.copy()

    override fun delete(uri: Uri) {
        files.remove(uri)
    }

    override fun createDirectories(uri: Uri) = Unit
}

private class FakeRemoteAppVariantOverrides(wrapper: AppVariantOverridesRawType) : BaseAppVariantOverrides(wrapper)
