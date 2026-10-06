package net.thunderbird.core.featureflag.domain.usecase

import app.cash.turbine.test
import assertk.all
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.test.Test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.featureflag.data.FeatureFlagCatalogDataSource
import net.thunderbird.core.featureflag.data.configstore.FeatureFlagConfigData
import net.thunderbird.core.featureflag.data.configstore.RemoteCatalogConfig
import net.thunderbird.core.featureflag.domain.RemoteFeatureFlagDomainContract.FetchEnabledFeatureFlags
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey.ARCHIVE_MARKS_AS_READ
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey.MESSAGE_VIEW_ACTION_EXPORT_EML
import net.thunderbird.core.featureflag.model.EmptyAppVariantOverride
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.featureflag.model.FlagRegistryOverride
import net.thunderbird.core.featureflag.provider.CatalogFeatureFlagProvider.State
import net.thunderbird.core.featureflag.provider.CatalogProviderMetadata
import net.thunderbird.core.featureflag.provider.ProviderMetadata
import net.thunderbird.core.featureflag.provider.context.FeatureFlagContext
import net.thunderbird.core.featureflag.provider.evaluator.MultiFeatureFlagProviderEvaluator
import net.thunderbird.core.logging.testing.TestLogger

class DefaultFetchEnabledFeatureFlagsTest {

    @Test
    fun `invoke should emit the enabled flags and the remote catalog availability`() = runTest {
        // Arrange
        val configStore = FakeFeatureFlagConfigStore(initialConfig = config(remoteCatalogEnabled = true))
        val evaluator = FakeMultiFeatureFlagProviderEvaluator(
            enabledFlags = listOf(MESSAGE_VIEW_ACTION_EXPORT_EML, ARCHIVE_MARKS_AS_READ),
        )
        val testSubject = createTestSubject(configStore, evaluator)

        // Act
        testSubject().test {
            // Assert
            assertThat(awaitItem()).isEqualTo(
                success(
                    enabled = true,
                    available = true,
                    isRuntimeOverride = false,
                    flags = listOf(MESSAGE_VIEW_ACTION_EXPORT_EML, ARCHIVE_MARKS_AS_READ),
                ),
            )
        }
    }

    @Test
    fun `invoke should report the remote catalog as disabled when it is disabled in the config`() = runTest {
        // Arrange
        val configStore = FakeFeatureFlagConfigStore(initialConfig = config(remoteCatalogEnabled = false))
        val evaluator = FakeMultiFeatureFlagProviderEvaluator(enabledFlags = emptyList())
        val testSubject = createTestSubject(configStore, evaluator)

        // Act
        testSubject().test {
            // Assert
            assertThat(awaitItem()).isEqualTo(
                success(enabled = false, available = true, isRuntimeOverride = false, flags = emptyList()),
            )
        }
    }

    @Test
    fun `invoke should report a runtime override when the config has overrides`() = runTest {
        // Arrange
        val configStore = FakeFeatureFlagConfigStore(
            initialConfig = config(
                remoteCatalogEnabled = true,
                overrides = mapOf(MESSAGE_VIEW_ACTION_EXPORT_EML.key to true),
            ),
        )
        val evaluator = FakeMultiFeatureFlagProviderEvaluator(enabledFlags = listOf(MESSAGE_VIEW_ACTION_EXPORT_EML))
        val testSubject = createTestSubject(configStore, evaluator)

        // Act
        testSubject().test {
            // Assert
            assertThat(awaitItem()).isEqualTo(
                success(
                    enabled = true,
                    available = true,
                    isRuntimeOverride = true,
                    flags = listOf(MESSAGE_VIEW_ACTION_EXPORT_EML),
                ),
            )
        }
    }

    @Test
    fun `invoke should emit again when the enabled flags change`() = runTest {
        // Arrange
        val configStore = FakeFeatureFlagConfigStore(initialConfig = config(remoteCatalogEnabled = true))
        val evaluator = FakeMultiFeatureFlagProviderEvaluator(enabledFlags = emptyList())
        val testSubject = createTestSubject(configStore, evaluator)

        testSubject().test {
            assertThat(awaitItem()).isEqualTo(
                success(enabled = true, available = true, isRuntimeOverride = false, flags = emptyList()),
            )

            // Act
            evaluator.enabledFlags.value = listOf(ARCHIVE_MARKS_AS_READ)

            // Assert
            assertThat(awaitItem()).isEqualTo(
                success(
                    enabled = true,
                    available = true,
                    isRuntimeOverride = false,
                    flags = listOf(ARCHIVE_MARKS_AS_READ),
                ),
            )
        }
    }

    @Test
    fun `invoke should emit again when the config changes`() = runTest {
        // Arrange
        val configStore = FakeFeatureFlagConfigStore(initialConfig = config(remoteCatalogEnabled = true))
        val evaluator = FakeMultiFeatureFlagProviderEvaluator(enabledFlags = listOf(ARCHIVE_MARKS_AS_READ))
        val testSubject = createTestSubject(configStore, evaluator)

        testSubject().test {
            assertThat(awaitItem()).isEqualTo(
                success(
                    enabled = true,
                    available = true,
                    isRuntimeOverride = false,
                    flags = listOf(ARCHIVE_MARKS_AS_READ),
                ),
            )

            // Act
            configStore.setConfig(
                config(
                    remoteCatalogEnabled = false,
                    overrides = mapOf(ARCHIVE_MARKS_AS_READ.key to true),
                ),
            )

            // Assert
            assertThat(awaitItem()).isEqualTo(
                success(
                    enabled = false,
                    available = true,
                    isRuntimeOverride = true,
                    flags = listOf(ARCHIVE_MARKS_AS_READ),
                ),
            )
        }
    }

    @Test
    fun `invoke should report the remote catalog as disabled and unavailable when no catalog is loaded`() = runTest {
        // Arrange
        val configStore = FakeFeatureFlagConfigStore(initialConfig = config(remoteCatalogEnabled = true))
        val evaluator = FakeMultiFeatureFlagProviderEvaluator(enabledFlags = emptyList())
        val testSubject = createTestSubject(
            configStore = configStore,
            evaluator = evaluator,
            remoteDataSource = FakeRemoteCatalogDataSource(catalog = null),
        )

        // Act
        testSubject().test {
            // Assert
            assertThat(awaitItem()).isEqualTo(
                success(enabled = false, available = false, isRuntimeOverride = false, flags = emptyList()),
            )
        }
    }

    @Test
    fun `invoke should emit again when the remote catalog becomes available`() = runTest {
        // Arrange
        val configStore = FakeFeatureFlagConfigStore(initialConfig = config(remoteCatalogEnabled = true))
        val evaluator = FakeMultiFeatureFlagProviderEvaluator(enabledFlags = emptyList())
        val remoteDataSource = FakeRemoteCatalogDataSource(catalog = null)
        val testSubject = createTestSubject(configStore, evaluator, remoteDataSource)

        testSubject().test {
            assertThat(awaitItem()).isEqualTo(
                success(enabled = false, available = false, isRuntimeOverride = false, flags = emptyList()),
            )

            // Act
            remoteDataSource.catalog.value = CATALOG

            // Assert
            assertThat(awaitItem()).isEqualTo(
                success(enabled = true, available = true, isRuntimeOverride = false, flags = emptyList()),
            )
        }
    }

    @Test
    fun `invoke should emit ConfigReadFailed and complete when the config cannot be read`() = runTest {
        // Arrange
        val error = IOException("corrupted config")
        val configStore = FakeFeatureFlagConfigStore(readFailure = error)
        val evaluator = FakeMultiFeatureFlagProviderEvaluator(enabledFlags = listOf(ARCHIVE_MARKS_AS_READ))
        val testSubject = createTestSubject(configStore, evaluator)

        // Act
        testSubject().test {
            // Assert
            assertThat(awaitItem())
                .isInstanceOf<Outcome.Failure<FetchEnabledFeatureFlags.Failure>>()
                .prop(Outcome.Failure<FetchEnabledFeatureFlags.Failure>::error)
                .isInstanceOf<FetchEnabledFeatureFlags.Failure.ConfigReadFailed>()
                .prop(FetchEnabledFeatureFlags.Failure.ConfigReadFailed::cause)
                .all {
                    isInstanceOf<IOException>()
                    hasMessage(error.message)
                }
            awaitComplete()
        }
    }

    @Test
    fun `invoke should propagate errors that are not storage failures`() = runTest {
        // Arrange
        val configStore = FakeFeatureFlagConfigStore(readFailure = IllegalStateException("unexpected"))
        val evaluator = FakeMultiFeatureFlagProviderEvaluator(enabledFlags = emptyList())
        val testSubject = createTestSubject(configStore, evaluator)

        // Act
        testSubject().test {
            // Assert
            assertThat(awaitError()).isInstanceOf<IllegalStateException>()
        }
    }

    private fun createTestSubject(
        configStore: FakeFeatureFlagConfigStore,
        evaluator: MultiFeatureFlagProviderEvaluator,
        remoteDataSource: FeatureFlagCatalogDataSource = FakeRemoteCatalogDataSource(catalog = CATALOG),
    ): FetchEnabledFeatureFlags = DefaultFetchEnabledFeatureFlags(
        logger = TestLogger(),
        configStore = configStore,
        featureFlagProvider = evaluator,
        remoteDataSource = remoteDataSource,
    )

    private fun config(
        remoteCatalogEnabled: Boolean,
        overrides: Map<String, Boolean> = emptyMap(),
    ) = FeatureFlagConfigData(
        remoteCatalogConfig = RemoteCatalogConfig(enabled = remoteCatalogEnabled),
        overrides = overrides,
    )

    private fun success(
        enabled: Boolean,
        available: Boolean,
        isRuntimeOverride: Boolean,
        flags: List<FeatureFlagKey>,
    ) = Outcome.success(
        FetchEnabledFeatureFlags.Success(
            enabled = enabled,
            available = available,
            isRuntimeOverride = isRuntimeOverride,
            flags = flags,
        ),
    )

    private companion object {
        val CATALOG = FeatureFlagCatalog(
            version = "2026-10-01.1",
            flags = emptyList(),
            overrides = FlagRegistryOverride(k9 = EmptyAppVariantOverride, thunderbird = EmptyAppVariantOverride),
        )
    }
}

private class FakeRemoteCatalogDataSource(
    catalog: FeatureFlagCatalog?,
) : FeatureFlagCatalogDataSource {
    val catalog = MutableStateFlow(catalog)

    override fun observe(): Flow<FeatureFlagCatalog?> = catalog

    override suspend fun load(): FeatureFlagCatalog? = catalog.value
}

private class FakeMultiFeatureFlagProviderEvaluator(
    enabledFlags: List<FeatureFlagKey>,
) : MultiFeatureFlagProviderEvaluator {
    override val enabledFlags: MutableStateFlow<List<FeatureFlagKey>> = MutableStateFlow(enabledFlags)

    override val state: StateFlow<State> = MutableStateFlow(State.Resolved)

    override val metadata: ProviderMetadata = CatalogProviderMetadata(name = "fake_multi_provider")

    override suspend fun initialize(initialContext: FeatureFlagContext) = Unit

    override fun provide(key: FeatureFlagKey): FeatureFlagResult = FeatureFlagResult.Unavailable

    override fun toString(): String = "fake multi feature-flag provider"
}
