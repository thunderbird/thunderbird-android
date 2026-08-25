package net.thunderbird.core.featureflag.provider

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.featureflag.TestingFeatureFlagKey
import net.thunderbird.core.featureflag.data.FeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey.ARCHIVE_MARKS_AS_READ
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey.DISPLAY_IN_APP_NOTIFICATIONS
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey.MESSAGE_VIEW_ACTION_EXPORT_EML
import net.thunderbird.core.featureflag.model.AppVariantOverridesRawType
import net.thunderbird.core.featureflag.model.BaseAppVariantOverrides
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.featureflag.model.FlagOverrides
import net.thunderbird.core.featureflag.model.FlagRegistry
import net.thunderbird.core.featureflag.model.FlagRegistryOverride
import net.thunderbird.core.featureflag.provider.context.FeatureFlagContext
import net.thunderbird.core.featureflag.provider.context.FeatureFlagContext.Value
import net.thunderbird.core.featureflag.provider.context.ImmutableFeatureFlagContext
import net.thunderbird.core.logging.testing.TestLogger

@OptIn(ExperimentalCoroutinesApi::class)
class BundledCatalogFeatureFlagProviderTest {

    @Test
    fun `provide should return Enabled when the catalog default is true`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val testSubject = createTestSubject(
            catalog = catalog(flags = listOf(MESSAGE_VIEW_ACTION_EXPORT_EML.toFlagRegistry(default = true))),
        )
        testSubject.initialize(initialContext = context())

        // Act
        val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(result).isEqualTo(FeatureFlagResult.Enabled)
    }

    @Test
    fun `provide should return Disabled when the catalog default is false`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val testSubject = createTestSubject(
            catalog = catalog(flags = listOf(MESSAGE_VIEW_ACTION_EXPORT_EML.toFlagRegistry(default = false))),
        )
        testSubject.initialize(initialContext = context())

        // Act
        val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(result).isEqualTo(FeatureFlagResult.Disabled)
    }

    @Test
    fun `provide should return Enabled when the build type override enables a disabled default`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val testSubject = createTestSubject(
                catalog = catalog(
                    flags = listOf(MESSAGE_VIEW_ACTION_EXPORT_EML.toFlagRegistry(default = false)),
                    thunderbirdOverrides = mapOf(DEBUG to mapOf(MESSAGE_VIEW_ACTION_EXPORT_EML.key to true)),
                ),
            )
            testSubject.initialize(initialContext = context(app = THUNDERBIRD, buildType = DEBUG))

            // Act
            val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

            // Assert
            assertThat(result).isEqualTo(FeatureFlagResult.Enabled)
        }

    @Test
    fun `provide should return Disabled when the build type override disables an enabled default`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val testSubject = createTestSubject(
                catalog = catalog(
                    flags = listOf(MESSAGE_VIEW_ACTION_EXPORT_EML.toFlagRegistry(default = true)),
                    k9Overrides = mapOf(RELEASE to mapOf(MESSAGE_VIEW_ACTION_EXPORT_EML.key to false)),
                ),
            )
            testSubject.initialize(initialContext = context(app = K9, buildType = RELEASE))

            // Act
            val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

            // Assert
            assertThat(result).isEqualTo(FeatureFlagResult.Disabled)
        }

    @Test
    fun `provide should return the default when the catalog has no override for the current build type`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val testSubject = createTestSubject(
                catalog = catalog(
                    flags = listOf(MESSAGE_VIEW_ACTION_EXPORT_EML.toFlagRegistry(default = false)),
                    thunderbirdOverrides = mapOf(DEBUG to mapOf(MESSAGE_VIEW_ACTION_EXPORT_EML.key to true)),
                ),
            )
            testSubject.initialize(initialContext = context(app = THUNDERBIRD, buildType = BETA))

            // Act
            val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

            // Assert
            assertThat(result).isEqualTo(FeatureFlagResult.Disabled)
        }

    @Test
    fun `provide should return the default when the build type override map is empty`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val testSubject = createTestSubject(
                catalog = catalog(
                    flags = listOf(MESSAGE_VIEW_ACTION_EXPORT_EML.toFlagRegistry(default = true)),
                    thunderbirdOverrides = mapOf(RELEASE to emptyMap()),
                ),
            )
            testSubject.initialize(initialContext = context(app = THUNDERBIRD, buildType = RELEASE))

            // Act
            val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

            // Assert
            assertThat(result).isEqualTo(FeatureFlagResult.Enabled)
        }

    @Test
    fun `provide should return the default when the context has no app and build type attributes`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val testSubject = createTestSubject(
                catalog = catalog(
                    flags = listOf(MESSAGE_VIEW_ACTION_EXPORT_EML.toFlagRegistry(default = false)),
                    thunderbirdOverrides = mapOf(DEBUG to mapOf(MESSAGE_VIEW_ACTION_EXPORT_EML.key to true)),
                ),
            )
            testSubject.initialize(initialContext = ImmutableFeatureFlagContext(targetingKey = TARGETING_KEY))

            // Act
            val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

            // Assert
            assertThat(result).isEqualTo(FeatureFlagResult.Disabled)
        }

    @Test
    fun `provide should return Unavailable when the key is not in the catalog`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val testSubject = createTestSubject(
                catalog = catalog(flags = listOf(MESSAGE_VIEW_ACTION_EXPORT_EML.toFlagRegistry(default = true))),
            )
            testSubject.initialize(initialContext = context())

            // Act
            val result = testSubject.provide(TestingFeatureFlagKey("unknown_feature_flag"))

            // Assert
            assertThat(result).isEqualTo(FeatureFlagResult.Unavailable)
        }

    @Test
    fun `defaults should return every catalog flag with the build type overrides applied`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val testSubject = createTestSubject(
                catalog = catalog(
                    flags = listOf(
                        ARCHIVE_MARKS_AS_READ.toFlagRegistry(default = true),
                        MESSAGE_VIEW_ACTION_EXPORT_EML.toFlagRegistry(default = false),
                        DISPLAY_IN_APP_NOTIFICATIONS.toFlagRegistry(default = true),
                    ),
                    thunderbirdOverrides = mapOf(
                        DEBUG to mapOf(
                            MESSAGE_VIEW_ACTION_EXPORT_EML.key to true,
                            DISPLAY_IN_APP_NOTIFICATIONS.key to false,
                        ),
                    ),
                ),
            )
            testSubject.initialize(initialContext = context(app = THUNDERBIRD, buildType = DEBUG))

            // Act
            val result = testSubject.defaults()

            // Assert
            assertThat(result).containsOnly(
                ARCHIVE_MARKS_AS_READ.key to true,
                MESSAGE_VIEW_ACTION_EXPORT_EML.key to true,
                DISPLAY_IN_APP_NOTIFICATIONS.key to false,
            )
        }

    @Test
    fun `defaults should be empty when the catalog was never loaded`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val testSubject = createTestSubject(
            catalog = catalog(flags = listOf(MESSAGE_VIEW_ACTION_EXPORT_EML.toFlagRegistry(default = true))),
        )

        // Act
        val result = testSubject.defaults()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `initialize should load the catalog only once`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val dataSource = FakeFeatureFlagCatalogDataSource(
            catalog = catalog(flags = listOf(MESSAGE_VIEW_ACTION_EXPORT_EML.toFlagRegistry(default = true))),
        )
        val testSubject = createTestSubject(dataSource = dataSource)
        testSubject.initialize(initialContext = context())

        // Act
        repeat(times = 5) { testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML) }
        testSubject.defaults()

        // Assert
        assertThat(dataSource.loadCount).isEqualTo(1)
    }

    private fun TestScope.createTestSubject(
        catalog: FeatureFlagCatalog,
    ): BundledCatalogFeatureFlagProvider = createTestSubject(FakeFeatureFlagCatalogDataSource(catalog))

    private fun TestScope.createTestSubject(
        dataSource: FeatureFlagCatalogDataSource,
    ): BundledCatalogFeatureFlagProvider = BundledCatalogFeatureFlagProvider(
        dataSource = dataSource,
        logger = TestLogger(),
        scope = backgroundScope,
    )

    private companion object {
        const val CATALOG_VERSION = "2026-07-30.1"
        const val TARGETING_KEY = "targeting-key"
        const val THUNDERBIRD = "thunderbird"
        const val K9 = "k9"
        const val DEBUG = "debug"
        const val BETA = "beta"
        const val RELEASE = "release"

        fun FeatureFlagKey.toFlagRegistry(default: Boolean): FlagRegistry = FlagRegistry(key = key, default = default)

        fun catalog(
            flags: List<FlagRegistry>,
            thunderbirdOverrides: Map<String, FlagOverrides> = emptyMap(),
            k9Overrides: Map<String, FlagOverrides> = emptyMap(),
        ): FeatureFlagCatalog = FeatureFlagCatalog(
            version = CATALOG_VERSION,
            flags = flags,
            overrides = FlagRegistryOverride(
                k9 = FakeAppVariantOverrides(k9Overrides),
                thunderbird = FakeAppVariantOverrides(thunderbirdOverrides),
            ),
        )

        fun context(
            app: String = THUNDERBIRD,
            buildType: String = DEBUG,
        ): FeatureFlagContext = ImmutableFeatureFlagContext(
            targetingKey = TARGETING_KEY,
            attributes = mapOf(
                "app" to Value.String(app),
                "build_type" to Value.String(buildType),
            ),
        )
    }
}

/**
 * Counts how often the catalog is actually collected, so tests can assert the provider caches the
 * resolved flags instead of re-reading the catalog on every [BundledCatalogFeatureFlagProvider.provide].
 */
private class FakeFeatureFlagCatalogDataSource(
    private val catalog: FeatureFlagCatalog,
) : FeatureFlagCatalogDataSource {
    var loadCount: Int = 0
        private set

    override fun load(): Flow<FeatureFlagCatalog> = flow {
        loadCount++
        emit(catalog)
    }
}

private class FakeAppVariantOverrides(wrapper: AppVariantOverridesRawType) : BaseAppVariantOverrides(wrapper)
