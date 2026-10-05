package net.thunderbird.core.featureflag.provider

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.featureflag.data.FeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigData
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigStore
import net.thunderbird.core.featureflag.data.configstore.RemoteCatalogConfig
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey.ARCHIVE_MARKS_AS_READ
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey.MESSAGE_VIEW_ACTION_EXPORT_EML
import net.thunderbird.core.featureflag.model.EmptyAppVariantOverride
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.featureflag.model.FlagRegistry
import net.thunderbird.core.featureflag.model.FlagRegistryOverride
import net.thunderbird.core.featureflag.provider.context.FeatureFlagContext
import net.thunderbird.core.featureflag.provider.context.ImmutableFeatureFlagContext
import net.thunderbird.core.logging.testing.TestLogger

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteCatalogFeatureFlagProviderTest {

    @Test
    fun `initialize should resolve the flags from the remote catalog when it is enabled`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val testSubject = createTestSubject(
                configStore = FakeRemoteCatalogConfigStore(remoteCatalogEnabled = true),
                dataSource = FakeRemoteCatalogDataSource(catalog = CATALOG),
            )

            // Act
            testSubject.initialize(initialContext = context())

            // Assert
            assertThat(testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)).isEqualTo(FeatureFlagResult.Enabled)
            assertThat(testSubject.provide(ARCHIVE_MARKS_AS_READ)).isEqualTo(FeatureFlagResult.Disabled)
        }

    @Test
    fun `initialize should mark the provider as resolved`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val testSubject = createTestSubject(
            configStore = FakeRemoteCatalogConfigStore(remoteCatalogEnabled = true),
            dataSource = FakeRemoteCatalogDataSource(catalog = CATALOG),
        )

        // Act
        testSubject.initialize(initialContext = context())

        // Assert
        assertThat(testSubject.state.value).isEqualTo(CatalogFeatureFlagProvider.State.Resolved)
    }

    @Test
    fun `initialize should not load the remote catalog when it is disabled`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val dataSource = FakeRemoteCatalogDataSource(catalog = CATALOG)
        val testSubject = createTestSubject(
            configStore = FakeRemoteCatalogConfigStore(remoteCatalogEnabled = false),
            dataSource = dataSource,
        )

        // Act
        testSubject.initialize(initialContext = context())

        // Assert
        assertThat(dataSource.loadCount).isEqualTo(0)
        assertThat(testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)).isEqualTo(FeatureFlagResult.Unavailable)
    }

    @Test
    fun `provide should return Unavailable when the remote catalog cannot be loaded`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val testSubject = createTestSubject(
                configStore = FakeRemoteCatalogConfigStore(remoteCatalogEnabled = true),
                dataSource = FakeRemoteCatalogDataSource(catalog = null),
            )
            testSubject.initialize(initialContext = context())

            // Act
            val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

            // Assert
            assertThat(result).isEqualTo(FeatureFlagResult.Unavailable)
        }

    @Test
    fun `provide should resolve the remote catalog when it is enabled after initialize`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val configStore = FakeRemoteCatalogConfigStore(remoteCatalogEnabled = false)
            val dataSource = FakeRemoteCatalogDataSource(catalog = CATALOG)
            val testSubject = createTestSubject(configStore = configStore, dataSource = dataSource)
            testSubject.initialize(initialContext = context())

            // Act
            configStore.setRemoteCatalogEnabled(enabled = true)

            // Assert
            assertThat(dataSource.loadCount).isEqualTo(1)
            assertThat(testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)).isEqualTo(FeatureFlagResult.Enabled)
        }

    @Test
    fun `provide should return Unavailable when the remote catalog is disabled after initialize`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val configStore = FakeRemoteCatalogConfigStore(remoteCatalogEnabled = true)
            val testSubject = createTestSubject(
                configStore = configStore,
                dataSource = FakeRemoteCatalogDataSource(catalog = CATALOG),
            )
            testSubject.initialize(initialContext = context())

            // Act
            configStore.setRemoteCatalogEnabled(enabled = false)

            // Assert
            assertThat(testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)).isEqualTo(FeatureFlagResult.Unavailable)
            assertThat(testSubject.provide(ARCHIVE_MARKS_AS_READ)).isEqualTo(FeatureFlagResult.Unavailable)
        }

    @Test
    fun `provide should resolve the remote catalog again when it is re-enabled`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val configStore = FakeRemoteCatalogConfigStore(remoteCatalogEnabled = true)
            val testSubject = createTestSubject(
                configStore = configStore,
                dataSource = FakeRemoteCatalogDataSource(catalog = CATALOG),
            )
            testSubject.initialize(initialContext = context())
            configStore.setRemoteCatalogEnabled(enabled = false)

            // Act
            configStore.setRemoteCatalogEnabled(enabled = true)

            // Assert
            assertThat(testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)).isEqualTo(FeatureFlagResult.Enabled)
        }

    @Test
    fun `enabling the remote catalog before initialize should not load the catalog`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val configStore = FakeRemoteCatalogConfigStore(remoteCatalogEnabled = false)
            val dataSource = FakeRemoteCatalogDataSource(catalog = CATALOG)
            val testSubject = createTestSubject(configStore = configStore, dataSource = dataSource)

            // Act
            configStore.setRemoteCatalogEnabled(enabled = true)

            // Assert
            assertThat(dataSource.loadCount).isEqualTo(0)
            assertThat(testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)).isEqualTo(FeatureFlagResult.Unavailable)
        }

    private fun TestScope.createTestSubject(
        configStore: FeatureFlagConfigStore,
        dataSource: FeatureFlagCatalogDataSource,
    ): RemoteCatalogFeatureFlagProvider = RemoteCatalogFeatureFlagProvider(
        configStore = configStore,
        dataSource = dataSource,
        logger = TestLogger(),
        scope = backgroundScope,
    )

    private companion object {
        const val TARGETING_KEY = "targeting-key"

        val CATALOG = FeatureFlagCatalog(
            version = "2026-10-01.1",
            flags = listOf(
                FlagRegistry(key = MESSAGE_VIEW_ACTION_EXPORT_EML.key, default = true),
                FlagRegistry(key = ARCHIVE_MARKS_AS_READ.key, default = false),
            ),
            overrides = FlagRegistryOverride(k9 = EmptyAppVariantOverride, thunderbird = EmptyAppVariantOverride),
        )

        fun context(): FeatureFlagContext = ImmutableFeatureFlagContext(targetingKey = TARGETING_KEY)
    }
}

/** Serves a fixed catalog and counts the loads, so tests can assert when the remote catalog is fetched. */
private class FakeRemoteCatalogDataSource(
    private val catalog: FeatureFlagCatalog?,
) : FeatureFlagCatalogDataSource {
    var loadCount: Int = 0
        private set

    override fun observe(): Flow<FeatureFlagCatalog?> = emptyFlow()

    override suspend fun load(): FeatureFlagCatalog? {
        loadCount++
        return catalog
    }
}

private class FakeRemoteCatalogConfigStore(
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
