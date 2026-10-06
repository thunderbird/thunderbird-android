package net.thunderbird.core.featureflag.provider.evaluator

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.thunderbird.core.featureflag.FeatureFlagKey
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey.ARCHIVE_MARKS_AS_READ
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey.DISPLAY_IN_APP_NOTIFICATIONS
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey.MESSAGE_VIEW_ACTION_EXPORT_EML
import net.thunderbird.core.featureflag.model.EmptyAppVariantOverride
import net.thunderbird.core.featureflag.model.FeatureFlagCatalog
import net.thunderbird.core.featureflag.model.FlagOverrides
import net.thunderbird.core.featureflag.model.FlagRegistry
import net.thunderbird.core.featureflag.model.FlagRegistryOverride
import net.thunderbird.core.featureflag.provider.BaseCatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.CatalogFeatureFlagProvider
import net.thunderbird.core.featureflag.provider.CatalogFeatureFlagProvider.State
import net.thunderbird.core.featureflag.provider.ProviderMetadata
import net.thunderbird.core.logging.testing.TestLogger

class MultiFeatureFlagProviderEvaluatorTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `provide should return Enabled when the first provider returns Enabled`() {
        // Arrange
        val recorder = InvocationRecorder()
        val testSubject = createTestSubject(
            recorder.provider(name = FIRST, result = FeatureFlagResult.Enabled),
            recorder.provider(name = SECOND, result = FeatureFlagResult.Disabled),
        )

        // Act
        val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(result).isEqualTo(FeatureFlagResult.Enabled)
    }

    @Test
    fun `provide should return Disabled when the first provider returns Disabled`() {
        // Arrange
        val recorder = InvocationRecorder()
        val testSubject = createTestSubject(
            recorder.provider(name = FIRST, result = FeatureFlagResult.Disabled),
            recorder.provider(name = SECOND, result = FeatureFlagResult.Enabled),
        )

        // Act
        val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(result).isEqualTo(FeatureFlagResult.Disabled)
    }

    @Test
    fun `provide should return Enabled when the first provider is Unavailable and the second returns Enabled`() {
        // Arrange
        val recorder = InvocationRecorder()
        val testSubject = createTestSubject(
            recorder.provider(name = FIRST, result = FeatureFlagResult.Unavailable),
            recorder.provider(name = SECOND, result = FeatureFlagResult.Enabled),
        )

        // Act
        val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(result).isEqualTo(FeatureFlagResult.Enabled)
        assertThat(recorder.invocations).containsExactly(FIRST, SECOND)
    }

    @Test
    fun `provide should return Disabled when the first provider is Unavailable and the second returns Disabled`() {
        // Arrange
        val recorder = InvocationRecorder()
        val testSubject = createTestSubject(
            recorder.provider(name = FIRST, result = FeatureFlagResult.Unavailable),
            recorder.provider(name = SECOND, result = FeatureFlagResult.Disabled),
        )

        // Act
        val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(result).isEqualTo(FeatureFlagResult.Disabled)
        assertThat(recorder.invocations).containsExactly(FIRST, SECOND)
    }

    @Test
    fun `provide should return Unavailable when every provider returns Unavailable`() {
        // Arrange
        val recorder = InvocationRecorder()
        val testSubject = createTestSubject(
            recorder.provider(name = FIRST, result = FeatureFlagResult.Unavailable),
            recorder.provider(name = SECOND, result = FeatureFlagResult.Unavailable),
            recorder.provider(name = THIRD, result = FeatureFlagResult.Unavailable),
        )

        // Act
        val result = testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(result).isEqualTo(FeatureFlagResult.Unavailable)
    }

    @Test
    fun `provide should query the providers in their configured order`() {
        // Arrange
        val recorder = InvocationRecorder()
        val testSubject = createTestSubject(
            recorder.provider(name = FIRST, result = FeatureFlagResult.Unavailable),
            recorder.provider(name = SECOND, result = FeatureFlagResult.Unavailable),
            recorder.provider(name = THIRD, result = FeatureFlagResult.Unavailable),
        )

        // Act
        testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(recorder.invocations).containsExactly(FIRST, SECOND, THIRD)
    }

    @Test
    fun `provide should not query the providers after a concrete result`() {
        // Arrange
        val recorder = InvocationRecorder()
        val notQueriedProvider = recorder.provider(name = THIRD, result = FeatureFlagResult.Enabled)
        val testSubject = createTestSubject(
            recorder.provider(name = FIRST, result = FeatureFlagResult.Unavailable),
            recorder.provider(name = SECOND, result = FeatureFlagResult.Disabled),
            notQueriedProvider,
        )

        // Act
        testSubject.provide(MESSAGE_VIEW_ACTION_EXPORT_EML)

        // Assert
        assertThat(recorder.invocations).containsExactly(FIRST, SECOND)
        assertThat(notQueriedProvider.provideCount).isEqualTo(0)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `enabledFlags should exclude a flag disabled by a higher priority provider`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val remote = FakeResolvedCatalogFeatureFlagProvider(name = FIRST, scope = backgroundScope)
            val bundled = FakeResolvedCatalogFeatureFlagProvider(name = SECOND, scope = backgroundScope)
            val testSubject = createTestSubject(remote, bundled)

            testSubject.enabledFlags.test {
                // Act
                remote.resolveFlags(mapOf(MESSAGE_VIEW_ACTION_EXPORT_EML.key to false))
                bundled.resolveFlags(
                    mapOf(
                        MESSAGE_VIEW_ACTION_EXPORT_EML.key to true,
                        ARCHIVE_MARKS_AS_READ.key to true,
                    ),
                )

                // Assert
                assertThat(expectMostRecentItem()).containsExactly(ARCHIVE_MARKS_AS_READ)
            }
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `enabledFlags should include a flag enabled by a higher priority provider`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val remote = FakeResolvedCatalogFeatureFlagProvider(name = FIRST, scope = backgroundScope)
            val bundled = FakeResolvedCatalogFeatureFlagProvider(name = SECOND, scope = backgroundScope)
            val testSubject = createTestSubject(remote, bundled)

            testSubject.enabledFlags.test {
                // Act
                remote.resolveFlags(mapOf(MESSAGE_VIEW_ACTION_EXPORT_EML.key to true))
                bundled.resolveFlags(
                    mapOf(
                        MESSAGE_VIEW_ACTION_EXPORT_EML.key to false,
                        ARCHIVE_MARKS_AS_READ.key to true,
                    ),
                )

                // Assert
                assertThat(expectMostRecentItem())
                    .containsExactlyInAnyOrder(MESSAGE_VIEW_ACTION_EXPORT_EML, ARCHIVE_MARKS_AS_READ)
            }
        }

    @Test
    fun `enabledFlags should fall back to the lower priority provider when the higher one is cleared`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val remote = FakeResolvedCatalogFeatureFlagProvider(name = FIRST, scope = backgroundScope)
            val bundled = FakeResolvedCatalogFeatureFlagProvider(name = SECOND, scope = backgroundScope)
            val testSubject = createTestSubject(remote, bundled)

            testSubject.enabledFlags.test {
                remote.resolveFlags(
                    mapOf(
                        MESSAGE_VIEW_ACTION_EXPORT_EML.key to false,
                        DISPLAY_IN_APP_NOTIFICATIONS.key to true,
                    ),
                )
                bundled.resolveFlags(mapOf(MESSAGE_VIEW_ACTION_EXPORT_EML.key to true))
                assertThat(expectMostRecentItem()).containsExactly(DISPLAY_IN_APP_NOTIFICATIONS)

                // Act
                remote.resolveFlags(emptyMap())

                // Assert
                assertThat(awaitItem()).containsExactly(MESSAGE_VIEW_ACTION_EXPORT_EML)
            }
        }

    @Test
    fun `enabledFlags should ignore keys unknown to the app`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val remote = FakeResolvedCatalogFeatureFlagProvider(name = FIRST, scope = backgroundScope)
        val testSubject = createTestSubject(remote)

        testSubject.enabledFlags.test {
            // Act
            remote.resolveFlags(mapOf(UNKNOWN_FLAG_KEY to true, ARCHIVE_MARKS_AS_READ.key to true))

            // Assert
            assertThat(expectMostRecentItem()).containsExactly(ARCHIVE_MARKS_AS_READ)
        }
    }

    @Test
    fun `enabledFlags should be empty when no provider enables a flag`() = runTest(UnconfinedTestDispatcher()) {
        // Arrange
        val remote = FakeResolvedCatalogFeatureFlagProvider(name = FIRST, scope = backgroundScope)
        val bundled = FakeResolvedCatalogFeatureFlagProvider(name = SECOND, scope = backgroundScope)
        val testSubject = createTestSubject(remote, bundled)

        testSubject.enabledFlags.test {
            // Act
            remote.resolveFlags(mapOf(ARCHIVE_MARKS_AS_READ.key to false))
            bundled.resolveFlags(mapOf(MESSAGE_VIEW_ACTION_EXPORT_EML.key to false))

            // Assert
            assertThat(expectMostRecentItem()).isEmpty()
        }
    }

    private fun createTestSubject(
        vararg providers: CatalogFeatureFlagProvider,
    ): MultiFeatureFlagProviderEvaluator = DefaultMultiFeatureFlagProviderEvaluator(
        providers = providers.toList(),
        logger = TestLogger(),
    )

    private companion object {
        const val FIRST = "first"
        const val SECOND = "second"
        const val THIRD = "third"
        const val UNKNOWN_FLAG_KEY = "unknown_flag_key"
    }
}

/**
 * Records the order in which the providers it creates are queried, so tests can assert both the
 * first-match strategy and that later providers are left untouched.
 */
private class InvocationRecorder {
    val invocations: MutableList<String> = mutableListOf()

    fun provider(name: String, result: FeatureFlagResult): FakeCatalogFeatureFlagProvider =
        FakeCatalogFeatureFlagProvider(name = name, result = result, invocations = invocations)
}

private class FakeCatalogFeatureFlagProvider(
    private val name: String,
    private val result: FeatureFlagResult,
    private val invocations: MutableList<String>,
) : CatalogFeatureFlagProvider {
    var provideCount: Int = 0
        private set

    override val state: StateFlow<State> = MutableStateFlow(State.Resolved)

    override val metadata: ProviderMetadata = FakeProviderMetadata(name)

    override fun provide(key: FeatureFlagKey): FeatureFlagResult {
        provideCount++
        invocations += name
        return result
    }

    override fun toString(): String = "fake feature-flag provider '$name'"
}

private data class FakeProviderMetadata(override val name: String) : ProviderMetadata

/**
 * A [BaseCatalogFeatureFlagProvider] whose resolved flags are set directly by the test, so the
 * evaluator's [MultiFeatureFlagProviderEvaluator.enabledFlags] merge can be observed.
 */
private class FakeResolvedCatalogFeatureFlagProvider(
    name: String,
    scope: CoroutineScope,
) : BaseCatalogFeatureFlagProvider(providerName = name, logger = TestLogger(), scope = scope) {
    fun resolveFlags(flags: FlagOverrides) {
        resolve(
            context = null,
            catalog = FeatureFlagCatalog(
                version = "test-version",
                flags = flags.map { (key, default) -> FlagRegistry(key = key, default = default) },
                overrides = FlagRegistryOverride(k9 = EmptyAppVariantOverride, thunderbird = EmptyAppVariantOverride),
            ),
        )
    }

    override fun toString(): String = "fake resolved feature-flag provider '${metadata.name}'"
}
